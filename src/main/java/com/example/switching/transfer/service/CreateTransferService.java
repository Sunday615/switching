package com.example.switching.transfer.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.common.util.RequestHashUtil;
import com.example.switching.common.util.TransferRefGenerator;
import com.example.switching.idempotency.service.IdempotencyService;
import com.example.switching.inquiry.entity.InquiryEntity;
import com.example.switching.inquiry.enums.InquiryStatus;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoMessageType;
import com.example.switching.iso.service.IsoMessageService;
import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.service.OutboxTransactionService;
import com.example.switching.participant.service.ParticipantService;
import com.example.switching.routing.dto.RoutingResolveResponse;
import com.example.switching.routing.service.RoutingService;
import com.example.switching.transfer.dto.CreateTransferRequest;
import com.example.switching.transfer.dto.CreateTransferResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.exception.InquiryAlreadyUsedException;
import com.example.switching.transfer.exception.InquiryValidationException;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class CreateTransferService {

    private static final String CHANNEL_ID = "API";

    private final TransferRefGenerator transferRefGenerator;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final InquiryRepository inquiryRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxTransactionService outboxTransactionService;
    private final AuditLogService auditLogService;
    private final IsoMessageService isoMessageService;
    private final ParticipantService participantService;
    private final RoutingService routingService;

    public CreateTransferService(
            TransferRefGenerator transferRefGenerator,
            TransferRepository transferRepository,
            TransferStatusHistoryRepository transferStatusHistoryRepository,
            InquiryRepository inquiryRepository,
            IdempotencyService idempotencyService,
            OutboxTransactionService outboxTransactionService,
            AuditLogService auditLogService,
            IsoMessageService isoMessageService,
            ParticipantService participantService,
            RoutingService routingService) {
        this.transferRefGenerator = transferRefGenerator;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.inquiryRepository = inquiryRepository;
        this.idempotencyService = idempotencyService;
        this.outboxTransactionService = outboxTransactionService;
        this.auditLogService = auditLogService;
        this.isoMessageService = isoMessageService;
        this.participantService = participantService;
        this.routingService = routingService;
    }

    @Transactional
    public CreateTransferResponse create(CreateTransferRequest request) {
        String inquiryRef = null;
        String transferRef = null;

        auditLogService.log(
                "TRANSFER_REQUEST_RECEIVED",
                "TRANSFER",
                null,
                CHANNEL_ID,
                request);

        try {
            String resolvedInquiryRef = requireInquiryRef(request.getInquiryRef());
            inquiryRef = resolvedInquiryRef;

            String requestHash = RequestHashUtil.sha256(request);
            String incomingIdempotencyKey = normalize(request.getIdempotencyKey());

            Optional<TransferEntity> existingTransferOptional = Optional.empty();
            if (StringUtils.hasText(incomingIdempotencyKey)) {
                existingTransferOptional = idempotencyService.findExistingTransfer(
                        CHANNEL_ID,
                        incomingIdempotencyKey,
                        requestHash);
            }

            if (existingTransferOptional.isPresent()) {
                TransferEntity existingTransfer = existingTransferOptional.get();

                Map<String, Object> duplicatePayload = new LinkedHashMap<>();
                duplicatePayload.put("transferRef", existingTransfer.getTransferRef());
                duplicatePayload.put("status",
                        existingTransfer.getStatus() == null ? null : existingTransfer.getStatus().name());
                duplicatePayload.put("idempotencyKey", incomingIdempotencyKey);
                duplicatePayload.put("inquiryRef", resolvedInquiryRef);

                auditLogService.log(
                        "TRANSFER_IDEMPOTENCY_HIT",
                        "TRANSFER",
                        existingTransfer.getTransferRef(),
                        CHANNEL_ID,
                        duplicatePayload);

                return new CreateTransferResponse(
                        existingTransfer.getTransferRef(),
                        existingTransfer.getStatus() == null ? null : existingTransfer.getStatus().name(),
                        "Duplicate request returned existing transfer");
            }

            InquiryEntity inquiry = inquiryRepository.findByInquiryRef(resolvedInquiryRef)
                    .orElseThrow(() -> new InquiryValidationException("Inquiry not found: " + resolvedInquiryRef));

            validateEligibleInquiry(inquiry, request);

            Optional<TransferEntity> existingTransferByInquiry = transferRepository
                    .findByInquiryRef(resolvedInquiryRef);

            if (existingTransferByInquiry.isPresent()) {
                throw new InquiryAlreadyUsedException(
                        "Inquiry already used by transfer: "
                                + existingTransferByInquiry.get().getTransferRef());
            }

            String normalizedSourceBank = participantService.normalize(request.getSourceBank());
            String normalizedDestinationBank = participantService.normalize(request.getDestinationBank());

            participantService.requireActive(normalizedSourceBank);
            participantService.requireActive(normalizedDestinationBank);

            RoutingResolveResponse routing = routingService.resolve(
                    normalizedSourceBank,
                    normalizedDestinationBank,
                    IsoMessageType.PACS_008.name());

            String routeCode = requireRoutingValue(routing.getRouteCode(), "routeCode");
            String connectorName = requireRoutingValue(routing.getConnectorName(), "connectorName");

            Map<String, Object> validatedPayload = new LinkedHashMap<>();
            validatedPayload.put("inquiryRef", inquiryRef);
            validatedPayload.put("sourceBank", normalizedSourceBank);
            validatedPayload.put("destinationBank", normalizedDestinationBank);
            validatedPayload.put("creditorAccount", request.getCreditorAccount());
            validatedPayload.put("amount", request.getAmount());
            validatedPayload.put("currency", request.getCurrency());
            validatedPayload.put("messageType", IsoMessageType.PACS_008.name());
            validatedPayload.put("routeCode", routeCode);
            validatedPayload.put("connectorName", connectorName);

            auditLogService.log(
                    "TRANSFER_VALIDATED_AGAINST_INQUIRY",
                    "TRANSFER",
                    null,
                    CHANNEL_ID,
                    validatedPayload);

            auditLogService.log(
                    "TRANSFER_ROUTE_RESOLVED",
                    "TRANSFER",
                    null,
                    CHANNEL_ID,
                    validatedPayload);

            transferRef = transferRefGenerator.generate();
            LocalDateTime now = LocalDateTime.now();

            String clientTransferId = StringUtils.hasText(incomingIdempotencyKey)
                    ? incomingIdempotencyKey
                    : transferRef;

            String idempotencyKey = StringUtils.hasText(incomingIdempotencyKey)
                    ? incomingIdempotencyKey
                    : transferRef;

            TransferEntity transfer = new TransferEntity();
            transfer.setTransferRef(transferRef);
            transfer.setClientTransferId(clientTransferId);
            transfer.setIdempotencyKey(idempotencyKey);
            transfer.setInquiryRef(inquiryRef);
            transfer.setSourceBank(normalizedSourceBank);
            transfer.setDebtorAccount(request.getDebtorAccount());
            transfer.setDestinationBank(normalizedDestinationBank);
            transfer.setCreditorAccount(request.getCreditorAccount());
            transfer.setDestinationAccountName(inquiry.getDestinationAccountName());
            transfer.setAmount(request.getAmount());
            transfer.setCurrency(request.getCurrency());
            transfer.setChannelId(CHANNEL_ID);
            transfer.setRouteCode(routeCode);
            transfer.setConnectorName(connectorName);
            transfer.setExternalReference(null);
            transfer.setStatus(TransferStatus.RECEIVED);
            transfer.setErrorCode(null);
            transfer.setErrorMessage(null);
            transfer.setReference(request.getReference());

            try {
                transferRepository.saveAndFlush(transfer);
            } catch (DataIntegrityViolationException ex) {
                throw new InquiryAlreadyUsedException("Inquiry already used by another transfer");
            }

            TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();
            history.setTransferRef(transferRef);
            history.setStatus(TransferStatus.RECEIVED.name());
            history.setReasonCode(null);
            history.setCreatedAt(now);
            transferStatusHistoryRepository.save(history);

            idempotencyService.saveNew(
                    CHANNEL_ID,
                    idempotencyKey,
                    requestHash,
                    transferRef,
                    TransferStatus.RECEIVED.name());

            Map<String, Object> createdPayload = new LinkedHashMap<>();
            createdPayload.put("transferRef", transferRef);
            createdPayload.put("inquiryRef", inquiryRef);
            createdPayload.put("status", TransferStatus.RECEIVED.name());
            createdPayload.put("sourceBank", normalizedSourceBank);
            createdPayload.put("destinationBank", normalizedDestinationBank);
            createdPayload.put("creditorAccount", request.getCreditorAccount());
            createdPayload.put("amount", request.getAmount());
            createdPayload.put("currency", request.getCurrency());
            createdPayload.put("idempotencyKey", idempotencyKey);
            createdPayload.put("messageType", IsoMessageType.PACS_008.name());
            createdPayload.put("routeCode", routeCode);
            createdPayload.put("connectorName", connectorName);

            auditLogService.log(
                    "TRANSFER_CREATED",
                    "TRANSFER",
                    transferRef,
                    CHANNEL_ID,
                    createdPayload);

            IsoMessageEntity pacs008Message = isoMessageService.createEncryptedOutboundPacs008(transfer);

            Map<String, Object> isoPayload = new LinkedHashMap<>();
            isoPayload.put("isoMessageId", pacs008Message.getId());
            isoPayload.put("messageType", pacs008Message.getMessageType().name());
            isoPayload.put("direction", pacs008Message.getDirection().name());
            isoPayload.put("messageId", pacs008Message.getMessageId());
            isoPayload.put("endToEndId", pacs008Message.getEndToEndId());
            isoPayload.put("securityStatus", pacs008Message.getSecurityStatus().name());
            isoPayload.put("plainPayloadPresent", pacs008Message.getPlainPayload() != null);
            isoPayload.put("encryptedPayloadPresent", pacs008Message.getEncryptedPayload() != null);
            isoPayload.put("transferRef", transferRef);
            isoPayload.put("inquiryRef", inquiryRef);
            isoPayload.put("sourceBank", normalizedSourceBank);
            isoPayload.put("destinationBank", normalizedDestinationBank);
            isoPayload.put("routeCode", routeCode);
            isoPayload.put("connectorName", connectorName);

            auditLogService.log(
                    "ISO_PACS008_ENCRYPTED_CREATED",
                    "TRANSFER",
                    transferRef,
                    CHANNEL_ID,
                    isoPayload);

            DispatchTransferCommand command = new DispatchTransferCommand(
                    transferRef,
                    pacs008Message.getId(),
                    normalizedSourceBank,
                    request.getDebtorAccount(),
                    normalizedDestinationBank,
                    request.getCreditorAccount(),
                    request.getAmount(),
                    request.getCurrency(),
                    connectorName,
                    routeCode);

            outboxTransactionService.enqueueTransferDispatch(command);

            Map<String, Object> queuedPayload = new LinkedHashMap<>();
            queuedPayload.put("transferRef", transferRef);
            queuedPayload.put("inquiryRef", inquiryRef);
            queuedPayload.put("outboxMessageType", "TRANSFER_DISPATCH");
            queuedPayload.put("isoMessageId", pacs008Message.getId());
            queuedPayload.put("isoMessageType", pacs008Message.getMessageType().name());
            queuedPayload.put("isoSecurityStatus", pacs008Message.getSecurityStatus().name());
            queuedPayload.put("isoEncryptedPayloadPresent", pacs008Message.getEncryptedPayload() != null);
            queuedPayload.put("sourceBank", normalizedSourceBank);
            queuedPayload.put("destinationBank", normalizedDestinationBank);
            queuedPayload.put("messageType", IsoMessageType.PACS_008.name());
            queuedPayload.put("routeCode", routeCode);
            queuedPayload.put("connectorName", connectorName);

            auditLogService.log(
                    "TRANSFER_QUEUED_FOR_DISPATCH",
                    "TRANSFER",
                    transferRef,
                    CHANNEL_ID,
                    queuedPayload);

            return new CreateTransferResponse(
                    transferRef,
                    TransferStatus.RECEIVED.name(),
                    "Transfer request accepted and queued for dispatch");

        } catch (Exception ex) {
            auditLogService.logError(
                    "TRANSFER_FAILED",
                    "TRANSFER",
                    transferRef,
                    CHANNEL_ID,
                    ex);
            throw ex;
        }
    }

    private String requireInquiryRef(String inquiryRef) {
        if (!StringUtils.hasText(inquiryRef)) {
            throw new InquiryValidationException("inquiryRef is required before transfer");
        }
        return inquiryRef.trim();
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

    private String requireRoutingValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Routing resolved but missing " + fieldName);
        }

        return value.trim();
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

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}