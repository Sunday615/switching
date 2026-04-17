package com.example.switching.outbox.worker;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.switching.connector.BankConnector;
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

    public OutboxDispatchWorker(OutboxEventRepository outboxEventRepository,
                                TransferRepository transferRepository,
                                TransferStatusHistoryRepository transferStatusHistoryRepository,
                                BankConnector bankConnector,
                                ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.bankConnector = bankConnector;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        System.out.println(">>> Outbox worker triggered");

        List<OutboxEventEntity> events =
                outboxEventRepository.findTop20ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        System.out.println(">>> Pending events size = " + events.size());

        for (OutboxEventEntity event : events) {
            try {
                System.out.println(">>> Processing outbox event id = " + event.getId()
                        + ", transferRef = " + event.getTransferRef());

                event.setStatus(OutboxStatus.PROCESSING);
                outboxEventRepository.save(event);

                DispatchTransferCommand command =
                        objectMapper.readValue(event.getPayload(), DispatchTransferCommand.class);

                System.out.println(">>> Dispatch command loaded for transferRef = "
                        + command.getTransferRef());

                TransferEntity transfer = transferRepository.findByTransferRef(command.getTransferRef())
                        .orElseThrow(() -> new IllegalStateException(
                                "Transfer not found for transferRef: " + command.getTransferRef()));

                BankDispatchResult result = bankConnector.dispatch(command);

                if (result.success()) {
                    System.out.println(">>> Dispatch success for transferRef = "
                            + command.getTransferRef());

                    transfer.setStatus(TransferStatus.SUCCESS);
                    transfer.setExternalReference(result.getExternalReference());
                    transfer.setReference(result.getReference());
                    transfer.setErrorCode(null);
                    transfer.setErrorMessage(null);

                    event.setStatus(OutboxStatus.SUCCESS);
                } else {
                    System.out.println(">>> Dispatch failed for transferRef = "
                            + command.getTransferRef()
                            + ", errorCode = " + result.getErrorCode()
                            + ", errorMessage = " + result.getErrorMessage());

                    transfer.setStatus(TransferStatus.FAILED);
                    transfer.setErrorCode(result.getErrorCode());
                    transfer.setErrorMessage(result.getErrorMessage());

                    event.setStatus(OutboxStatus.FAILED);
                }

                transferRepository.save(transfer);

                saveTransferHistory(
                        transfer.getTransferRef(),
                        transfer.getStatus().name(),
                        transfer.getErrorCode()
                );

                outboxEventRepository.save(event);

                System.out.println(">>> Finished processing transferRef = "
                        + transfer.getTransferRef()
                        + ", final transfer status = " + transfer.getStatus()
                        + ", outbox status = " + event.getStatus());

            } catch (Exception ex) {
                System.out.println(">>> ERROR processing outbox event id = " + event.getId()
                        + ", transferRef = " + event.getTransferRef());
                ex.printStackTrace();

                event.setStatus(OutboxStatus.FAILED);
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
            }
        }
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