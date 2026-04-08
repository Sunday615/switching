package com.example.switching.transfer.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CreateTransferService(TransferRefGenerator transferRefGenerator,
                                 TransferRepository transferRepository,
                                 TransferStatusHistoryRepository transferStatusHistoryRepository) {
        this.transferRefGenerator = transferRefGenerator;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
    }

    @Transactional
    public CreateTransferResponse create(CreateTransferRequest request) {
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

        return new CreateTransferResponse(
                transferRef,
                TransferStatus.RECEIVED.name(),
                "Transfer request accepted"
        );
    }
}