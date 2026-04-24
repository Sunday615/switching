package com.example.switching.outbox.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.common.error.ErrorCatalog;
import com.example.switching.common.error.ErrorClassifier;
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

@Service
public class OutboxProcessorService {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessorService.class);

    private static final String ENTITY_TYPE = "TRANSFER";
    private static final String SOURCE_SYSTEM = "WORKER";
    private static final String IDEMPOTENCY_CHANNEL = "API";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_RETRY = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final BankConnector bankConnector;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final ErrorClassifier errorClassifier;
    private final TransactionTemplate transactionTemplate;

    public OutboxProcessorService(OutboxEventRepository outboxEventRepository,
                                  TransferRepository transferRepository,
                                  TransferStatusHistoryRepository transferStatusHistoryRepository,
                                  BankConnector bankConnector,
                                  ObjectMapper objectMapper,
                                  AuditLogService auditLogService,
                                  IdempotencyService idempotencyService,
                                  ErrorClassifier errorClassifier,
                                  PlatformTransactionManager transactionManager) {
        this.outboxEventRepository = outboxEventRepository;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.bankConnector = bankConnector;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.idempotencyService = idempotencyService;
        this.errorClassifier = errorClassifier;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void processSingleEvent(Long outboxEventId) {
        OutboxEventEntity claimedEvent = transactionTemplate.execute(status -> claimEvent(outboxEventId));

        if (claimedEvent == null) {
            log.debug("Skip outboxEventId={} because it was already claimed or is no longer pending", outboxEventId);
            return;
        }

        DispatchTransferCommand command = null;

        try {
            command = objectMapper.readValue(claimedEvent.getPayload(), DispatchTransferCommand.class);

            BankDispatchResult result = bankConnector.dispatch(command);
            if (result == null) {
                throw new IllegalStateException("BankConnector returned null result");
            }

            final DispatchTransferCommand finalCommand = command;
            final BankDispatchResult finalResult = result;

            if (result.success()) {
                transactionTemplate.executeWithoutResult(status ->
                        finalizeSuccess(claimedEvent.getId(), finalCommand.getTransferRef(), finalResult)
                );
            } else {
                transactionTemplate.executeWithoutResult(status ->
                        finalizeBusinessFailure(claimedEvent.getId(), finalCommand.getTransferRef(), finalResult)
                );
            }

        } catch (Exception ex) {
            final String transferRef = resolveTransferRef(claimedEvent, command);

            transactionTemplate.executeWithoutResult(status ->
                    finalizeTechnicalFailure(claimedEvent.getId(), transferRef, ex)
            );
        }
    }

    private OutboxEventEntity claimEvent(Long outboxEventId) {
        int updated = outboxEventRepository.claimPendingEvent(
                outboxEventId,
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING
        );

        if (updated == 0) {
            return null;
        }

        OutboxEventEntity event = getOutboxEventOrThrow(outboxEventId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outboxEventId", event.getId());
        payload.put("transferRef", event.getTransferRef());
        payload.put("messageType", event.getMessageType());

        auditLogService.log(
                "OUTBOX_DISPATCH_STARTED",
                ENTITY_TYPE,
                event.getTransferRef(),
                SOURCE_SYSTEM,
                payload
        );

        log.info("Claimed outboxEventId={} transferRef={} as PROCESSING",
                event.getId(), event.getTransferRef());

        return event;
    }

    private void finalizeSuccess(Long outboxEventId,
                                 String transferRef,
                                 BankDispatchResult result) {
        OutboxEventEntity event = getOutboxEventOrThrow(outboxEventId);
        TransferEntity transfer = getTransferOrThrow(transferRef);

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

        if (StringUtils.hasText(transfer.getIdempotencyKey())) {
            idempotencyService.updateStatus(
                    IDEMPOTENCY_CHANNEL,
                    transfer.getIdempotencyKey(),
                    TransferStatus.SUCCESS.name()
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outboxEventId", event.getId());
        payload.put("transferRef", transfer.getTransferRef());
        payload.put("status", TransferStatus.SUCCESS.name());
        payload.put("externalReference", result.getExternalReference());
        payload.put("reference", result.getReference());

        auditLogService.log(
                "OUTBOX_DISPATCH_SUCCESS",
                ENTITY_TYPE,
                transfer.getTransferRef(),
                SOURCE_SYSTEM,
                payload
        );

        log.info("Outbox dispatch success: outboxEventId={} transferRef={}",
                event.getId(), transfer.getTransferRef());
    }

    private void finalizeBusinessFailure(Long outboxEventId,
                                         String transferRef,
                                         BankDispatchResult result) {
        OutboxEventEntity event = getOutboxEventOrThrow(outboxEventId);
        TransferEntity transfer = getTransferOrThrow(transferRef);

        ErrorCatalog catalog = ErrorCatalog.EXT_001;

        transfer.setStatus(TransferStatus.FAILED);
        transfer.setErrorCode(catalog.getErrorCode());
        transfer.setErrorMessage(trimMessage(result.getErrorMessage()));
        transferRepository.save(transfer);

        saveTransferHistory(
                transfer.getTransferRef(),
                TransferStatus.FAILED.name(),
                catalog.getErrorCode()
        );

        event.setStatus(OutboxStatus.FAILED);
        outboxEventRepository.save(event);

        if (StringUtils.hasText(transfer.getIdempotencyKey())) {
            idempotencyService.updateStatus(
                    IDEMPOTENCY_CHANNEL,
                    transfer.getIdempotencyKey(),
                    TransferStatus.FAILED.name()
            );
        }

        Map<String, Object> payload = buildErrorPayload(
                catalog,
                event.getId(),
                transfer.getTransferRef(),
                result.getErrorMessage()
        );
        payload.put("downstreamErrorCode", result.getErrorCode());

        auditLogService.log(
                "OUTBOX_DISPATCH_FAILED",
                ENTITY_TYPE,
                transfer.getTransferRef(),
                SOURCE_SYSTEM,
                payload
        );

        log.warn("Outbox dispatch downstream failure: outboxEventId={} transferRef={} errorCode={}",
                event.getId(), transfer.getTransferRef(), catalog.getErrorCode());
    }

    private void finalizeTechnicalFailure(Long outboxEventId,
                                          String transferRef,
                                          Exception ex) {
        ErrorCatalog catalog = errorClassifier.classify(ex);
        OutboxEventEntity event = getOutboxEventOrThrow(outboxEventId);

        int nextRetryCount = safeRetryCount(event.getRetryCount()) + 1;
        boolean shouldRetry = catalog.isRetryable() && nextRetryCount < MAX_RETRY;

        TransferEntity transfer = null;
        if (StringUtils.hasText(transferRef)) {
            transfer = transferRepository.findByTransferRef(transferRef).orElse(null);
        }

        if (transfer != null) {
            transfer.setErrorCode(catalog.getErrorCode());
            transfer.setErrorMessage(trimMessage(ex.getMessage()));

            if (!shouldRetry) {
                transfer.setStatus(TransferStatus.FAILED);
                transferRepository.save(transfer);

                saveTransferHistory(
                        transfer.getTransferRef(),
                        TransferStatus.FAILED.name(),
                        catalog.getErrorCode()
                );

                if (StringUtils.hasText(transfer.getIdempotencyKey())) {
                    idempotencyService.updateStatus(
                            IDEMPOTENCY_CHANNEL,
                            transfer.getIdempotencyKey(),
                            TransferStatus.FAILED.name()
                    );
                }
            } else {
                transferRepository.save(transfer);
            }
        }

        event.setRetryCount(nextRetryCount);
        event.setStatus(shouldRetry ? OutboxStatus.PENDING : OutboxStatus.FAILED);
        outboxEventRepository.save(event);

        Map<String, Object> payload = buildErrorPayload(
                catalog,
                event.getId(),
                StringUtils.hasText(transferRef) ? transferRef : event.getTransferRef(),
                ex.getMessage()
        );
        payload.put("attemptNo", nextRetryCount);
        payload.put("maxRetry", MAX_RETRY);
        payload.put("willRetry", shouldRetry);

        auditLogService.log(
                shouldRetry ? "OUTBOX_DISPATCH_RETRY_SCHEDULED" : "OUTBOX_DISPATCH_FAILED",
                ENTITY_TYPE,
                StringUtils.hasText(transferRef) ? transferRef : event.getTransferRef(),
                SOURCE_SYSTEM,
                payload
        );

        if (shouldRetry) {
            log.warn("Outbox dispatch retry scheduled: outboxEventId={} transferRef={} errorCode={} attempt={}/{}",
                    event.getId(), transferRef, catalog.getErrorCode(), nextRetryCount, MAX_RETRY);
        } else {
            log.error("Outbox dispatch terminal failure: outboxEventId={} transferRef={} errorCode={}",
                    event.getId(), transferRef, catalog.getErrorCode(), ex);
        }
    }

    private Map<String, Object> buildErrorPayload(ErrorCatalog catalog,
                                                  Long outboxEventId,
                                                  String transferRef,
                                                  String errorMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outboxEventId", outboxEventId);
        payload.put("transferRef", transferRef);
        payload.put("errorCode", catalog.getErrorCode());
        payload.put("category", catalog.getCategory().name());
        payload.put("layer", catalog.getLayer().name());
        payload.put("phase", catalog.getPhase().name());
        payload.put("retryable", catalog.isRetryable());
        payload.put("message", catalog.getDefaultMessage());
        payload.put("errorMessage", trimMessage(errorMessage));
        return payload;
    }

    private OutboxEventEntity getOutboxEventOrThrow(Long outboxEventId) {
        return outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found for id: " + outboxEventId
                ));
    }

    private TransferEntity getTransferOrThrow(String transferRef) {
        return transferRepository.findByTransferRef(transferRef)
                .orElseThrow(() -> new IllegalStateException(
                        "Transfer not found for transferRef: " + transferRef
                ));
    }

    private void saveTransferHistory(String transferRef, String status, String reasonCode) {
        TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
        history.setTransferRef(transferRef);
        history.setStatus(status);
        history.setReasonCode(reasonCode);
        history.setCreatedAt(LocalDateTime.now());
        transferStatusHistoryRepository.save(history);
    }

    private String resolveTransferRef(OutboxEventEntity event, DispatchTransferCommand command) {
        if (command != null && StringUtils.hasText(command.getTransferRef())) {
            return command.getTransferRef();
        }
        return event.getTransferRef();
    }

    private int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }

    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }

        String trimmed = message.trim();
        if (trimmed.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return trimmed;
        }

        return trimmed.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}