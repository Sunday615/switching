package com.example.switching.outbox.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.outbox.dto.OutboxManualRetryResponse;
import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.exception.OutboxEventNotFoundException;
import com.example.switching.outbox.exception.OutboxManualRetryNotAllowedException;
import com.example.switching.outbox.repository.OutboxEventRepository;

@Service
public class OutboxManualRetryService {

    private static final String ENTITY_TYPE = "TRANSFER";
    private static final String SOURCE_SYSTEM = "API";

    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogService auditLogService;

    public OutboxManualRetryService(OutboxEventRepository outboxEventRepository,
                                    AuditLogService auditLogService) {
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OutboxManualRetryResponse retry(Long outboxEventId) {
        OutboxEventEntity event = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new OutboxEventNotFoundException(
                        "Outbox event not found: " + outboxEventId
                ));

        OutboxStatus previousStatus = event.getStatus();

        if (previousStatus != OutboxStatus.FAILED) {
            throw new OutboxManualRetryNotAllowedException(
                    "Only FAILED outbox events can be manually retried. Current status: " + previousStatus
            );
        }

        int updated = outboxEventRepository.resetFailedEventToPending(
                event.getId(),
                OutboxStatus.FAILED.name(),
                OutboxStatus.PENDING.name()
        );

        if (updated != 1) {
            throw new OutboxManualRetryNotAllowedException(
                    "Outbox event could not be reset to PENDING: " + outboxEventId
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outboxEventId", event.getId());
        payload.put("transferRef", event.getTransferRef());
        payload.put("messageType", event.getMessageType());
        payload.put("previousStatus", OutboxStatus.FAILED.name());
        payload.put("newStatus", OutboxStatus.PENDING.name());
        payload.put("retryCount", event.getRetryCount());
        payload.put("manualAction", true);
        payload.put("errorCode", "OUT-004");
        payload.put("category", "CORE");
        payload.put("layer", "OUTBOX");
        payload.put("phase", "DISPATCH_TRANSFER");
        payload.put("retryable", true);

        auditLogService.log(
                "OUTBOX_MANUAL_RETRY_REQUESTED",
                ENTITY_TYPE,
                event.getTransferRef(),
                SOURCE_SYSTEM,
                payload
        );

        return new OutboxManualRetryResponse(
                event.getId(),
                event.getTransferRef(),
                OutboxStatus.FAILED.name(),
                OutboxStatus.PENDING.name(),
                event.getRetryCount(),
                "Outbox event reset to PENDING for manual retry"
        );
    }
}