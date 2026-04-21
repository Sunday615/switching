package com.example.switching.outbox.worker;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.connector.BankConnector;
import com.example.switching.idempotency.service.IdempotencyService;
import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OutboxDispatchWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final BankConnector bankConnector;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;

    public OutboxDispatchWorker(OutboxEventRepository outboxEventRepository,
                                TransferRepository transferRepository,
                                TransferStatusHistoryRepository transferStatusHistoryRepository,
                                BankConnector bankConnector,
                                ObjectMapper objectMapper,
                                AuditLogService auditLogService,
                                IdempotencyService idempotencyService) {
        this.outboxEventRepository = outboxEventRepository;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.bankConnector = bankConnector;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingEvents() {
        System.out.println(">>> Outbox worker triggered");

        List<OutboxEventEntity> events =
                outboxEventRepository.findTop20ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        System.out.println(">>> Pending events size = " + events.size());

        for (OutboxEventEntity event : events) {
            processSingleEvent(event);
        }
    }

    private void processSingleEvent(OutboxEventEntity event) {
        try {
            System.out.println(">>> Processing outbox event id = " + event.getId()
                    + ", transferRef = " + event.getTransferRef());

            event.setStatus(OutboxStatus.PROCESSING);
            outboxEventRepository.save(event);

            auditLogService.log(
                    "OUTBOX_DISPATCH_STARTED",
                    "TRANSFER",
                    event.getTransferRef(),
                    "WORKER",
                    Map.of(
                            "outboxEventId", event.getId(),
                            "transferRef", event.getTransferRef(),
                            "messageType", event.getMessageType()
                    )
            );

            DispatchTransferCommand command =
                    objectMapper.readValue(event.getPayload(), DispatchTransferCommand.class);

            TransferEntity transfer = transferRepository.findByTransferRef(command.getTransferRef())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transfer not found for transferRef: " + command.getTransferRef()
                    ));

            BankDispatchResult result = bankConnector.dispatch(command);

            if (result.success()) {
                handleSuccess(event, transfer, result);
            } else {
                handleFailure(event, transfer, result);
            }

        } catch (Exception ex) {
            System.out.println(">>> ERROR processing outbox event id = " + event.getId()
                    + ", transferRef = " + event.getTransferRef());
            ex.printStackTrace();

            event.setStatus(OutboxStatus.FAILED);
            event.setRetryCount(event.getRetryCount() + 1);
            outboxEventRepository.save(event);

            auditLogService.logError(
                    "OUTBOX_DISPATCH_FAILED",
                    "TRANSFER",
                    event.getTransferRef(),
                    "WORKER",
                    ex
            );
        }
    }

    private void handleSuccess(OutboxEventEntity event,
                               TransferEntity transfer,
                               BankDispatchResult result) {
        transfer.setStatus(TransferStatus.SUCCESS);
        transfer.setExternalReference(result.getExternalReference());
        transfer.setReference(result.getReference());
        transfer.setErrorCode(null);
        transfer.setErrorMessage(null);

        transferRepository.save(transfer);
        saveTransferHistory(
                transfer.getTransferRef(),
                TransferStatus.SUCCESS.name(),
                null
        );

        event.setStatus(OutboxStatus.SUCCESS);
        outboxEventRepository.save(event);

        idempotencyService.updateStatus(
                "API",
                transfer.getIdempotencyKey(),
                TransferStatus.SUCCESS.name()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transfer.getTransferRef());
        payload.put("status", TransferStatus.SUCCESS.name());
        payload.put("externalReference", result.getExternalReference());
        payload.put("reference", result.getReference());
        payload.put("outboxEventId", event.getId());

        auditLogService.log(
                "OUTBOX_DISPATCH_SUCCESS",
                "TRANSFER",
                transfer.getTransferRef(),
                "WORKER",
                payload
        );

        System.out.println(">>> Finished processing transferRef = "
                + transfer.getTransferRef()
                + ", final transfer status = " + transfer.getStatus()
                + ", outbox status = " + event.getStatus());
    }

    private void handleFailure(OutboxEventEntity event,
                               TransferEntity transfer,
                               BankDispatchResult result) {
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setErrorCode(result.getErrorCode());
        transfer.setErrorMessage(result.getErrorMessage());

        transferRepository.save(transfer);
        saveTransferHistory(
                transfer.getTransferRef(),
                TransferStatus.FAILED.name(),
                result.getErrorCode()
        );

        event.setStatus(OutboxStatus.FAILED);
        outboxEventRepository.save(event);

        idempotencyService.updateStatus(
                "API",
                transfer.getIdempotencyKey(),
                TransferStatus.FAILED.name()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transfer.getTransferRef());
        payload.put("status", TransferStatus.FAILED.name());
        payload.put("errorCode", result.getErrorCode());
        payload.put("errorMessage", result.getErrorMessage());
        payload.put("outboxEventId", event.getId());

        auditLogService.log(
                "OUTBOX_DISPATCH_FAILED",
                "TRANSFER",
                transfer.getTransferRef(),
                "WORKER",
                payload
        );

        System.out.println(">>> Dispatch failed for transferRef = "
                + transfer.getTransferRef()
                + ", errorCode = " + result.getErrorCode()
                + ", errorMessage = " + result.getErrorMessage());
    }

    private void saveTransferHistory(String transferRef, String status, String reasonCode) {
        TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
        history.setTransferRef(transferRef);
        history.setStatus(status);
        history.setReasonCode(reasonCode);
        history.setCreatedAt(LocalDateTime.now());
        transferStatusHistoryRepository.save(history);
    }
}