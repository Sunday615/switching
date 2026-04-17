package com.example.switching.transfer.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.common.util.RequestHashUtil;
import com.example.switching.common.util.TransferRefGenerator;
import com.example.switching.idempotency.service.IdempotencyService;
import com.example.switching.inquiry.entity.InquiryEntity;
import com.example.switching.inquiry.enums.InquiryStatus;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.service.OutboxTransactionService;
import com.example.switching.transfer.dto.CreateTransferRequest;
import com.example.switching.transfer.dto.CreateTransferResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.exception.InquiryValidationException;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class CreateTransferService {

    private final TransferRefGenerator transferRefGenerator;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final InquiryRepository inquiryRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxTransactionService outboxTransactionService;

    public CreateTransferService(TransferRefGenerator transferRefGenerator,
                                 TransferRepository transferRepository,
                                 TransferStatusHistoryRepository transferStatusHistoryRepository,
                                 InquiryRepository inquiryRepository,
                                 IdempotencyService idempotencyService,
                                 OutboxTransactionService outboxTransactionService) {
        this.transferRefGenerator = transferRefGenerator;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.inquiryRepository = inquiryRepository;
        this.idempotencyService = idempotencyService;
        this.outboxTransactionService = outboxTransactionService;
    }

    @Transactional
    public CreateTransferResponse create(CreateTransferRequest request) {
        String inquiryRef = requireInquiryRef(request.getInquiryRef());
        String requestHash = RequestHashUtil.sha256(request);

  Optional<TransferEntity> existingTransferOptional =
        idempotencyService.findExistingTransfer("API", request.getIdempotencyKey(), requestHash);

        if (existingTransferOptional.isPresent()) {
            TransferEntity existingTransfer = existingTransferOptional.get();
            return new CreateTransferResponse(
                    existingTransfer.getTransferRef(),
                    existingTransfer.getStatus() == null ? null : existingTransfer.getStatus().name(),
                    "Duplicate request returned existing transfer"
            );
        }

        InquiryEntity inquiry = inquiryRepository.findByInquiryRef(inquiryRef)
                .orElseThrow(() -> new InquiryValidationException("Inquiry not found: " + inquiryRef));

        validateEligibleInquiry(inquiry, request);

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
        transfer.setInquiryRef(inquiryRef);
        transfer.setSourceBank(request.getSourceBank());
        transfer.setDebtorAccount(request.getDebtorAccount());
        transfer.setDestinationBank(request.getDestinationBank());
        transfer.setCreditorAccount(request.getCreditorAccount());
        transfer.setDestinationAccountName(inquiry.getDestinationAccountName());
        transfer.setAmount(request.getAmount());
        transfer.setCurrency(request.getCurrency());
        transfer.setChannelId("API");
        transfer.setRouteCode(inquiry.getRouteCode());
        transfer.setConnectorName(inquiry.getConnectorName());
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

    idempotencyService.saveNew(
        "API",
        idempotencyKey,
        requestHash,
        transferRef,
        TransferStatus.RECEIVED.name()
);

        DispatchTransferCommand command = new DispatchTransferCommand(
                transferRef,
                request.getSourceBank(),
                request.getDebtorAccount(),
                request.getDestinationBank(),
                request.getCreditorAccount(),
                request.getAmount(),
                request.getCurrency(),
                inquiry.getConnectorName(),
                inquiry.getRouteCode()
        );

        outboxTransactionService.enqueueTransferDispatch(command);

        return new CreateTransferResponse(
                transferRef,
                TransferStatus.RECEIVED.name(),
                "Transfer request accepted and queued for dispatch"
        );
    }

    private String requireInquiryRef(String inquiryRef) {
        if (inquiryRef == null || inquiryRef.isBlank()) {
            throw new InquiryValidationException("inquiryRef is required before transfer");
        }
        return inquiryRef;
    }

    private void validateEligibleInquiry(InquiryEntity inquiry, CreateTransferRequest request) {
        if (inquiry.getStatus() != InquiryStatus.ELIGIBLE) {
            throw new InquiryValidationException("Inquiry is not eligible: " + inquiry.getInquiryRef());
        }

        if (!Boolean.TRUE.equals(inquiry.getBankAvailable())) {
            throw new InquiryValidationException("Destination bank is not available");
        }

        if (!Boolean.TRUE.equals(inquiry.getAccountFound())) {
            throw new InquiryValidationException("Destination account was not found");
        }

        if (!Boolean.TRUE.equals(inquiry.getEligibleForTransfer())) {
            throw new InquiryValidationException("Inquiry is not eligible for transfer");
        }

        if (!equalsIgnoreCase(inquiry.getSourceBank(), request.getSourceBank())) {
            throw new InquiryValidationException("Source bank does not match inquiry");
        }

        if (!equalsIgnoreCase(inquiry.getDestinationBank(), request.getDestinationBank())) {
            throw new InquiryValidationException("Destination bank does not match inquiry");
        }

        if (!equalsValue(inquiry.getCreditorAccount(), request.getCreditorAccount())) {
            throw new InquiryValidationException("Creditor account does not match inquiry");
        }

        if (!equalsValue(stringValue(inquiry.getAmount()), stringValue(request.getAmount()))) {
            throw new InquiryValidationException("Amount does not match inquiry");
        }

        if (!equalsIgnoreCase(inquiry.getCurrency(), request.getCurrency())) {
            throw new InquiryValidationException("Currency does not match inquiry");
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    private boolean equalsValue(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}