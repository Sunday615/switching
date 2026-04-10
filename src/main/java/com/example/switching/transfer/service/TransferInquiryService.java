package com.example.switching.transfer.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.transfer.dto.TransferInquiryResponse;
import com.example.switching.transfer.dto.TransferStatusHistoryItemResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.exception.TransferNotFoundException;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class TransferInquiryService {

    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final AuditLogService auditLogService;

    public TransferInquiryService(TransferRepository transferRepository,
                                  TransferStatusHistoryRepository transferStatusHistoryRepository,
                                  AuditLogService auditLogService) {
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.auditLogService = auditLogService;
    }

    public TransferInquiryResponse inquire(String transferRef) {
        auditLogService.log(
                "TRANSFER_INQUIRY_REQUESTED",
                "TRANSFER",
                transferRef,
                "API",
                Map.of("transferRef", transferRef)
        );

        try {
            TransferEntity transfer = transferRepository.findByTransferRef(transferRef)
                    .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + transferRef));

            List<TransferStatusHistoryEntity> historyEntities =
                    transferStatusHistoryRepository.findByTransferRefOrderByCreatedAtAsc(transferRef);

            List<TransferStatusHistoryItemResponse> history = historyEntities.stream()
                    .map(item -> new TransferStatusHistoryItemResponse(
                            item.getStatus(),
                            item.getReasonCode(),
                            item.getCreatedAt() == null ? null : item.getCreatedAt().toString()
                    ))
                    .toList();

            TransferInquiryResponse response = new TransferInquiryResponse();
            response.setTransferRef(transfer.getTransferRef());
            response.setStatus(transfer.getStatus() == null ? null : transfer.getStatus().name());
            response.setSourceBank(transfer.getSourceBank());
            response.setDestinationBank(transfer.getDestinationBank());
            response.setHistory(history);

            Map<String, Object> auditPayload = new LinkedHashMap<>();
            auditPayload.put("transferRef", response.getTransferRef());
            auditPayload.put("status", response.getStatus());
            auditPayload.put("sourceBank", response.getSourceBank());
            auditPayload.put("destinationBank", response.getDestinationBank());
            auditPayload.put("historySize", history.size());

            auditLogService.log(
                    "TRANSFER_INQUIRY_RESPONDED",
                    "TRANSFER",
                    transferRef,
                    "API",
                    auditPayload
            );

            return response;

        } catch (Exception ex) {
            auditLogService.logError(
                    "TRANSFER_INQUIRY_FAILED",
                    "TRANSFER",
                    transferRef,
                    "API",
                    ex
            );
            throw ex;
        }
    }
}