package com.example.switching.service;

import org.springframework.stereotype.Service;

import com.example.switching.dto.CreateTransferRequest;
import com.example.switching.dto.CreateTransferResponse;
import com.example.switching.entity.AuditLogEntity;
import com.example.switching.entity.IdempotencyRecordEntity;
import com.example.switching.entity.OutboxEventEntity;
import com.example.switching.entity.ParticipantBankEntity;
import com.example.switching.entity.RoutingRuleEntity;
import com.example.switching.entity.TransferEntity;
import com.example.switching.entity.TransferStatusHistoryEntity;
import com.example.switching.enums.OutboxStatus;
import com.example.switching.enums.TransferStatus;
import com.example.switching.repository.AuditLogRepository;
import com.example.switching.repository.IdempotencyRecordRepository;
import com.example.switching.repository.OutboxEventRepository;
import com.example.switching.repository.ParticipantBankRepository;
import com.example.switching.repository.RoutingRuleRepository;
import com.example.switching.repository.TransferRepository;
import com.example.switching.repository.TransferStatusHistoryRepository;
import com.example.switching.util.RequestHashUtil;
import com.example.switching.util.TransferRefGenerator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CreateTransferService {

    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ParticipantBankRepository participantBankRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public CreateTransferResponse createTransfer(
            String channelId,
            String idempotencyKey,
            CreateTransferRequest request) {

        String requestHash = RequestHashUtil.hash(request);

        IdempotencyRecordEntity existingIdempotency = idempotencyRecordRepository
                .findByChannelIdAndIdempotencyKey(channelId, idempotencyKey)
                .orElse(null);

        if (existingIdempotency != null) {
            if (!existingIdempotency.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException("Idempotency key already used with different request");
            }

            if (existingIdempotency.getTransferRef() != null) {
                TransferEntity existingTransfer = transferRepository
                        .findByTransferRef(existingIdempotency.getTransferRef())
                        .orElseThrow(() -> new IllegalStateException("Transfer not found for idempotency record"));

                return new CreateTransferResponse(
                        existingTransfer.getTransferRef(),
                        existingTransfer.getClientTransferId(),
                        existingTransfer.getStatus().name(),
                        "Duplicate request returned existing transfer");
            }
        }

        ParticipantBankEntity bank = participantBankRepository.findByBankCode(request.getDestinationBankCode())
                .orElseThrow(() -> new IllegalArgumentException("Destination bank not found"));

        if (!Boolean.TRUE.equals(bank.getActiveFlag()) || Boolean.TRUE.equals(bank.getMaintenanceFlag())) {
            throw new IllegalArgumentException("Destination bank is unavailable");
        }

        RoutingRuleEntity routingRule = routingRuleRepository
                .findFirstByDestinationBankCodeAndActiveFlagTrueOrderByPriorityAsc(request.getDestinationBankCode())
                .orElseThrow(() -> new IllegalArgumentException("Routing rule not found"));

        transferRepository.findByChannelIdAndClientTransferId(channelId, request.getClientTransferId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("clientTransferId already exists for this channel");
                });

        String transferRef = TransferRefGenerator.generate();

        TransferEntity transfer = new TransferEntity();
        transfer.setTransferRef(transferRef);
        transfer.setClientTransferId(request.getClientTransferId());
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setSourceBankCode(request.getSourceBankCode());
        transfer.setSourceAccountNo(request.getSourceAccountNo());
        transfer.setDestinationBankCode(request.getDestinationBankCode());
        transfer.setDestinationAccountNo(request.getDestinationAccountNo());
        transfer.setDestinationAccountName(request.getDestinationAccountName());
        transfer.setAmount(request.getAmount());
        transfer.setCurrency(request.getCurrency());
        transfer.setChannelId(channelId);
        transfer.setRouteCode(routingRule.getRouteCode());
        transfer.setConnectorName(routingRule.getConnectorName());
        transfer.setStatus(TransferStatus.RECEIVED);

        transferRepository.save(transfer);

        TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
        history.setTransferRef(transferRef);
        history.setFromStatus(null);
        history.setToStatus(TransferStatus.RECEIVED.name());
        history.setReasonCode("CREATED");
        history.setReasonMessage("Transfer created successfully");
        history.setChangedBy("SYSTEM");
        historyRepository.save(history);

        IdempotencyRecordEntity idempotencyRecord = existingIdempotency != null
                ? existingIdempotency
                : new IdempotencyRecordEntity();

        idempotencyRecord.setChannelId(channelId);
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setTransferRef(transferRef);
        idempotencyRecord.setStatus("COMPLETED");
        idempotencyRecordRepository.save(idempotencyRecord);

        OutboxEventEntity outbox = new OutboxEventEntity();
        outbox.setAggregateType("TRANSFER");
        outbox.setAggregateId(transferRef);
        outbox.setEventType("TRANSFER_CREATED");
        outbox.setPayload(toJson(request));
        outbox.setStatus(OutboxStatus.NEW);
        outbox.setRetryCount(0);
        outboxEventRepository.save(outbox);

        AuditLogEntity audit = new AuditLogEntity();
        audit.setEventType("TRANSFER_CREATED");
        audit.setReferenceType("TRANSFER");
        audit.setReferenceId(transferRef);
        audit.setActor("API");
        audit.setChannelId(channelId);
        audit.setPayload(toJson(request));
        auditLogRepository.save(audit);

        return new CreateTransferResponse(
                transferRef,
                request.getClientTransferId(),
                transfer.getStatus().name(),
                "Transfer accepted for processing");
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot serialize payload", e);
        }
    }
}