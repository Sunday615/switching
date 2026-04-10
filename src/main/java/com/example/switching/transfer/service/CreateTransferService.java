package com.example.switching.transfer.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.common.util.TransferRefGenerator;
import com.example.switching.transfer.dto.CreateTransferRequest;
import com.example.switching.transfer.dto.CreateTransferResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class CreateTransferService {

    private final TransferRefGenerator transferRefGenerator;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final AuditLogService auditLogService;

    public CreateTransferService(TransferRefGenerator transferRefGenerator,
                                 TransferRepository transferRepository,
                                 TransferStatusHistoryRepository transferStatusHistoryRepository,
                                 AuditLogService auditLogService) {
        this.transferRefGenerator = transferRefGenerator;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreateTransferResponse create(CreateTransferRequest request) {
        auditLogService.log(
                "TRANSFER_REQUEST_RECEIVED",
                "TRANSFER",
                null,
                "API",
                request
        );

        try {
            String transferRef = transferRefGenerator.generate();
            LocalDateTime now = LocalDateTime.now();

            String clientTransferId = request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                    ? request.getIdempotencyKey()
                    : transferRef;

            String idempotencyKey = request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                    ? request.getIdempotencyKey()
                    : transferRef;

            TransferEntity transfer = new TransferEntity();
            transfer.setTransferRef(transferRef);
            transfer.setClientTransferId(clientTransferId);
            transfer.setIdempotencyKey(idempotencyKey);
            transfer.setSourceBank(request.getSourceBank());
            transfer.setDebtorAccount(request.getDebtorAccount());
            transfer.setDestinationBank(request.getDestinationBank());
            transfer.setCreditorAccount(request.getCreditorAccount());
            transfer.setDestinationAccountName(null);
            transfer.setAmount(request.getAmount());
            transfer.setCurrency(request.getCurrency());
            transfer.setChannelId("API");
            transfer.setRouteCode(null);
            transfer.setConnectorName(null);
            transfer.setExternalReference(null);
            transfer.setStatus(TransferStatus.RECEIVED);
            transfer.setErrorCode(null);
            transfer.setErrorMessage(null);
            transfer.setReference(request.getReference());

            transferRepository.save(transfer);

            TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
            history.setTransferRef(transferRef);
            history.setStatus(TransferStatus.RECEIVED.name());
            history.setReasonCode(null);
            history.setCreatedAt(now);

            transferStatusHistoryRepository.save(history);

            Map<String, Object> auditPayload = new LinkedHashMap<>();
            auditPayload.put("transferRef", transferRef);
            auditPayload.put("status", TransferStatus.RECEIVED.name());
            auditPayload.put("sourceBank", request.getSourceBank());
            auditPayload.put("destinationBank", request.getDestinationBank());
            auditPayload.put("amount", request.getAmount());
            auditPayload.put("currency", request.getCurrency());
            auditPayload.put("reference", request.getReference());

            auditLogService.log(
                    "TRANSFER_CREATED",
                    "TRANSFER",
                    transferRef,
                    "API",
                    auditPayload
            );

            return new CreateTransferResponse(
                    transferRef,
                    TransferStatus.RECEIVED.name(),
                    "Transfer request accepted"
            );

        } catch (Exception ex) {
            auditLogService.logError(
                    "TRANSFER_CREATE_FAILED",
                    "TRANSFER",
                    null,
                    "API",
                    ex
            );
            throw ex;
        }
    }
}