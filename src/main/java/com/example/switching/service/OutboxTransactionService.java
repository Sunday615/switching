package com.example.switching.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.dto.BankDispatchResult;
import com.example.switching.dto.DispatchTransferCommand;
import com.example.switching.entity.AuditLogEntity;
import com.example.switching.entity.OutboxEventEntity;
import com.example.switching.entity.TransferEntity;
import com.example.switching.entity.TransferStatusHistoryEntity;
import com.example.switching.enums.OutboxStatus;
import com.example.switching.enums.TransferStatus;
import com.example.switching.repository.AuditLogRepository;
import com.example.switching.repository.OutboxEventRepository;
import com.example.switching.repository.TransferRepository;
import com.example.switching.repository.TransferStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxTransactionService {

    private final OutboxEventRepository outboxEventRepository;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public DispatchTransferCommand markEventProcessing(Long outboxEventId) {
        OutboxEventEntity outbox = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + outboxEventId));

        if (outbox.getStatus() != OutboxStatus.NEW) {
            return null;
        }

        TransferEntity transfer = transferRepository.findByTransferRef(outbox.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Transfer not found: " + outbox.getAggregateId()));

        outbox.setStatus(OutboxStatus.PROCESSING);
        outboxEventRepository.save(outbox);

        TransferStatus previousStatus = transfer.getStatus();
        if (previousStatus == TransferStatus.RECEIVED) {
            transfer.setStatus(TransferStatus.PROCESSING);
            transferRepository.save(transfer);

            saveHistory(
                    transfer.getTransferRef(),
                    previousStatus.name(),
                    TransferStatus.PROCESSING.name(),
                    "OUTBOX_DISPATCH_STARTED",
                    "Outbox worker started dispatch",
                    "WORKER"
            );

            saveAudit(
                    "TRANSFER_STATUS_UPDATED",
                    "TRANSFER",
                    transfer.getTransferRef(),
                    "WORKER",
                    transfer.getChannelId(),
                    "{\"status\":\"PROCESSING\"}"
            );
        }

        return new DispatchTransferCommand(
                outbox.getId(),
                transfer.getTransferRef(),
                transfer.getSourceBankCode(),
                transfer.getSourceAccountNo(),
                transfer.getDestinationBankCode(),
                transfer.getDestinationAccountNo(),
                transfer.getDestinationAccountName(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getConnectorName()
        );
    }

    @Transactional
    public void completeDispatch(Long outboxEventId, BankDispatchResult result) {
        OutboxEventEntity outbox = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + outboxEventId));

        TransferEntity transfer = transferRepository.findByTransferRef(outbox.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Transfer not found: " + outbox.getAggregateId()));

        TransferStatus previousStatus = transfer.getStatus();
        TransferStatus nextStatus = mapTransferStatus(result.getStatus());

        transfer.setStatus(nextStatus);
        transfer.setExternalReference(result.getExternalReference());

        if (nextStatus == TransferStatus.FAILED) {
            transfer.setErrorCode(result.getResponseCode());
            transfer.setErrorMessage(result.getResponseMessage());
        } else {
            transfer.setErrorCode(null);
            transfer.setErrorMessage(null);
        }

        transferRepository.save(transfer);

        outbox.setStatus(OutboxStatus.PUBLISHED);
        outbox.setPublishedAt(LocalDateTime.now());
        outbox.setLastError(null);
        outboxEventRepository.save(outbox);

        saveHistory(
                transfer.getTransferRef(),
                previousStatus.name(),
                nextStatus.name(),
                result.getResponseCode(),
                result.getResponseMessage(),
                "MOCK_CONNECTOR"
        );

        saveAudit(
                "TRANSFER_DISPATCH_RESULT",
                "TRANSFER",
                transfer.getTransferRef(),
                "MOCK_CONNECTOR",
                transfer.getChannelId(),
                toJson(result)
        );
    }

    @Transactional
    public void failDispatch(Long outboxEventId, String errorMessage) {
        OutboxEventEntity outbox = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + outboxEventId));

        TransferEntity transfer = transferRepository.findByTransferRef(outbox.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Transfer not found: " + outbox.getAggregateId()));

        TransferStatus previousStatus = transfer.getStatus();

        outbox.setStatus(OutboxStatus.FAILED);
        outbox.setRetryCount(outbox.getRetryCount() + 1);
        outbox.setLastError(truncate(errorMessage, 255));
        outboxEventRepository.save(outbox);

        transfer.setStatus(TransferStatus.FAILED);
        transfer.setErrorCode("DISPATCH_ERROR");
        transfer.setErrorMessage(truncate(errorMessage, 255));
        transferRepository.save(transfer);

        saveHistory(
                transfer.getTransferRef(),
                previousStatus.name(),
                TransferStatus.FAILED.name(),
                "DISPATCH_ERROR",
                truncate(errorMessage, 255),
                "WORKER"
        );

        saveAudit(
                "TRANSFER_DISPATCH_FAILED",
                "TRANSFER",
                transfer.getTransferRef(),
                "WORKER",
                transfer.getChannelId(),
                "{\"error\":\"" + escapeJson(truncate(errorMessage, 255)) + "\"}"
        );
    }

    private TransferStatus mapTransferStatus(String connectorStatus) {
        return switch (connectorStatus) {
            case "SUCCESS" -> TransferStatus.SUCCESS;
            case "PENDING" -> TransferStatus.PENDING;
            case "FAILED" -> TransferStatus.FAILED;
            default -> throw new IllegalArgumentException("Unsupported connector status: " + connectorStatus);
        };
    }

    private void saveHistory(
            String transferRef,
            String fromStatus,
            String toStatus,
            String reasonCode,
            String reasonMessage,
            String changedBy
    ) {
        TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
        history.setTransferRef(transferRef);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReasonCode(reasonCode);
        history.setReasonMessage(reasonMessage);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    private void saveAudit(
            String eventType,
            String referenceType,
            String referenceId,
            String actor,
            String channelId,
            String payload
    ) {
        AuditLogEntity audit = new AuditLogEntity();
        audit.setEventType(eventType);
        audit.setReferenceType(referenceType);
        audit.setReferenceId(referenceId);
        audit.setActor(actor);
        audit.setChannelId(channelId);
        audit.setPayload(payload);
        auditLogRepository.save(audit);
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}