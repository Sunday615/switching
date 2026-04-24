package com.example.switching.transfer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.audit.entity.AuditLogEntity;
import com.example.switching.audit.repository.AuditLogRepository;
import com.example.switching.inquiry.entity.InquiryEntity;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.example.switching.transfer.dto.TransferTraceAuditItemResponse;
import com.example.switching.transfer.dto.TransferTraceHistoryItemResponse;
import com.example.switching.transfer.dto.TransferTraceOutboxItemResponse;
import com.example.switching.transfer.dto.TransferTraceResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.exception.TransferNotFoundException;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class TransferTraceService {

    private static final String ENTITY_TYPE_TRANSFER = "TRANSFER";

    private final TransferRepository transferRepository;
    private final InquiryRepository inquiryRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuditLogRepository auditLogRepository;

    public TransferTraceService(TransferRepository transferRepository,
                                InquiryRepository inquiryRepository,
                                TransferStatusHistoryRepository transferStatusHistoryRepository,
                                OutboxEventRepository outboxEventRepository,
                                AuditLogRepository auditLogRepository) {
        this.transferRepository = transferRepository;
        this.inquiryRepository = inquiryRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public TransferTraceResponse getTrace(String transferRef) {
        String resolvedTransferRef = requireTransferRef(transferRef);

        TransferEntity transfer = transferRepository.findByTransferRef(resolvedTransferRef)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + resolvedTransferRef));

        InquiryEntity inquiry = null;
        if (transfer.getInquiryRef() != null && !transfer.getInquiryRef().isBlank()) {
            inquiry = inquiryRepository.findByInquiryRef(transfer.getInquiryRef()).orElse(null);
        }

        List<TransferStatusHistoryEntity> histories =
                transferStatusHistoryRepository.findAllByTransferRefOrderByIdAsc(resolvedTransferRef);

        List<OutboxEventEntity> outboxEvents =
                outboxEventRepository.findAllByTransferRefOrderByIdAsc(resolvedTransferRef);

       List<AuditLogEntity> auditLogs =
        auditLogRepository.findTop100ByOrderByIdDesc();

        TransferTraceResponse response = new TransferTraceResponse();

        response.setTransferRef(transfer.getTransferRef());
        response.setInquiryRef(transfer.getInquiryRef());
        response.setCurrentStatus(transfer.getStatus() == null ? null : transfer.getStatus().name());

        response.setSourceBank(transfer.getSourceBank());
        response.setDebtorAccount(transfer.getDebtorAccount());
        response.setDestinationBank(transfer.getDestinationBank());
        response.setCreditorAccount(transfer.getCreditorAccount());
        response.setDestinationAccountName(transfer.getDestinationAccountName());

        response.setAmount(transfer.getAmount());
        response.setCurrency(transfer.getCurrency());
        response.setReference(transfer.getReference());
        response.setExternalReference(transfer.getExternalReference());
        response.setErrorCode(transfer.getErrorCode());
        response.setErrorMessage(transfer.getErrorMessage());

        if (inquiry != null) {
            response.setInquiryStatus(inquiry.getStatus() == null ? null : inquiry.getStatus().name());
            response.setInquiryAccountFound(inquiry.getAccountFound());
            response.setInquiryBankAvailable(inquiry.getBankAvailable());
            response.setInquiryEligibleForTransfer(inquiry.getEligibleForTransfer());
        }

        response.setTransferHistory(histories.stream()
                .map(this::toHistoryItem)
                .toList());

        response.setOutboxEvents(outboxEvents.stream()
                .map(this::toOutboxItem)
                .toList());

        response.setAuditEvents(auditLogs.stream()
                .map(this::toAuditItem)
                .toList());

        return response;
    }

    private TransferTraceHistoryItemResponse toHistoryItem(TransferStatusHistoryEntity history) {
        return new TransferTraceHistoryItemResponse(
                history.getStatus(),
                history.getReasonCode(),
                history.getCreatedAt()
        );
    }

    private TransferTraceOutboxItemResponse toOutboxItem(OutboxEventEntity event) {
        return new TransferTraceOutboxItemResponse(
                event.getId(),
                event.getMessageType(),
                event.getStatus() == null ? null : event.getStatus().name(),
                event.getRetryCount(),
                event.getCreatedAt()
        );
    }

  private TransferTraceAuditItemResponse toAuditItem(AuditLogEntity auditLog) {
    return new TransferTraceAuditItemResponse(
            auditLog.getId(),
            auditLog.getEventType(),
            null,
            null,
            null,
            auditLog.getCreatedAt()
    );
}

    private String requireTransferRef(String transferRef) {
        if (transferRef == null || transferRef.isBlank()) {
            throw new TransferNotFoundException("Transfer not found");
        }
        return transferRef.trim();
    }
}