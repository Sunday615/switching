package com.example.switching.inquiry.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.inquiry.dto.CreateInquiryRequest;
import com.example.switching.inquiry.dto.CreateInquiryResponse;
import com.example.switching.inquiry.entity.InquiryEntity;
import com.example.switching.inquiry.entity.InquiryStatusHistoryEntity;
import com.example.switching.inquiry.enums.InquiryStatus;
import com.example.switching.inquiry.exception.InquiryNotFoundException;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.inquiry.repository.InquiryStatusHistoryRepository;
import com.example.switching.participant.entity.ParticipantEntity;
import com.example.switching.participant.exception.ParticipantNotFoundException;
import com.example.switching.participant.service.ParticipantService;
import com.example.switching.transfer.exception.InquiryValidationException;

@Service
public class CreateInquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryStatusHistoryRepository inquiryStatusHistoryRepository;
    private final AuditLogService auditLogService;
    private final ParticipantService participantService;

    public CreateInquiryService(InquiryRepository inquiryRepository,
                                InquiryStatusHistoryRepository inquiryStatusHistoryRepository,
                                AuditLogService auditLogService,
                                ParticipantService participantService) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryStatusHistoryRepository = inquiryStatusHistoryRepository;
        this.auditLogService = auditLogService;
        this.participantService = participantService;
    }

    @Transactional
    public CreateInquiryResponse create(CreateInquiryRequest request) {
        auditLogService.log(
                "INQUIRY_REQUEST_RECEIVED",
                "INQUIRY",
                null,
                "API",
                request);

        try {
            // ---- Validate required fields ----
            String rawSourceBank = request.getSourceBank();
            String rawDestinationBank = request.getDestinationBank();

            if (!StringUtils.hasText(rawSourceBank)) {
                throw new InquiryValidationException("sourceBank is required");
            }
            if (!StringUtils.hasText(rawDestinationBank)) {
                throw new InquiryValidationException("destinationBank is required");
            }
            if (!StringUtils.hasText(request.getCreditorAccount())) {
                throw new InquiryValidationException("creditorAccount is required");
            }

            String normalizedSourceBank = participantService.normalize(rawSourceBank);
            String normalizedDestinationBank = participantService.normalize(rawDestinationBank);

            // ---- Validate source bank (hard error — caller must send a valid bank) ----
            try {
                ParticipantEntity sourceParticipant = participantService.findByBankCode(normalizedSourceBank);
                if (!sourceParticipant.active()) {
                    throw new InquiryValidationException(
                            "Source bank is not ACTIVE: " + normalizedSourceBank);
                }
            } catch (ParticipantNotFoundException ex) {
                throw new InquiryValidationException("Source bank not found: " + normalizedSourceBank);
            }

            // ---- Validate destination bank (soft error — return NOT_ELIGIBLE gracefully) ----
            boolean bankAvailable = false;
            String notEligibleReason = null;

            try {
                ParticipantEntity destinationParticipant =
                        participantService.findByBankCode(normalizedDestinationBank);
                bankAvailable = destinationParticipant.active();
                if (!bankAvailable) {
                    notEligibleReason = "Destination bank is not ACTIVE: " + normalizedDestinationBank;
                }
            } catch (ParticipantNotFoundException ex) {
                notEligibleReason = "Destination bank not found: " + normalizedDestinationBank;
            }

            // ---- Account validation (mock: accept any non-empty creditorAccount) ----
            // In production this would call the destination bank's account lookup API.
            boolean accountFound = StringUtils.hasText(request.getCreditorAccount());
            if (accountFound && notEligibleReason == null) {
                // bank is available and account is non-empty — eligible
            } else if (!accountFound) {
                notEligibleReason = "creditorAccount is empty";
            }

            boolean eligibleForTransfer = bankAvailable && accountFound;
            String destinationAccountName = eligibleForTransfer ? "MOCK RECEIVER ACCOUNT" : null;
            InquiryStatus status = eligibleForTransfer ? InquiryStatus.ELIGIBLE : InquiryStatus.NOT_ELIGIBLE;

            String inquiryRef = generateInquiryRef();
            LocalDateTime now = LocalDateTime.now();

            InquiryEntity inquiry = new InquiryEntity();
            inquiry.setInquiryRef(inquiryRef);
            inquiry.setClientInquiryId(request.getClientInquiryId());
            inquiry.setSourceBank(normalizedSourceBank);
            inquiry.setDestinationBank(normalizedDestinationBank);
            inquiry.setCreditorAccount(request.getCreditorAccount());
            inquiry.setDestinationAccountName(destinationAccountName);
            inquiry.setAmount(request.getAmount());
            inquiry.setCurrency(request.getCurrency());
            inquiry.setChannelId("API");
            inquiry.setRouteCode(null);       // routing resolved at transfer time
            inquiry.setConnectorName(null);   // resolved at transfer time
            inquiry.setAccountFound(accountFound);
            inquiry.setBankAvailable(bankAvailable);
            inquiry.setEligibleForTransfer(eligibleForTransfer);
            inquiry.setStatus(status);
            inquiry.setErrorCode(eligibleForTransfer ? null : "INQUIRY_NOT_ELIGIBLE");
            inquiry.setErrorMessage(notEligibleReason);
            inquiry.setReference(request.getReference());

            inquiryRepository.save(inquiry);

            saveHistory(inquiryRef, InquiryStatus.RECEIVED.name(), null, now);
            saveHistory(inquiryRef, status.name(),
                    eligibleForTransfer ? null : "INQUIRY_NOT_ELIGIBLE", now);

            Map<String, Object> auditPayload = new LinkedHashMap<>();
            auditPayload.put("inquiryRef", inquiryRef);
            auditPayload.put("status", status.name());
            auditPayload.put("sourceBank", normalizedSourceBank);
            auditPayload.put("destinationBank", normalizedDestinationBank);
            auditPayload.put("creditorAccount", request.getCreditorAccount());
            auditPayload.put("amount", request.getAmount());
            auditPayload.put("currency", request.getCurrency());
            auditPayload.put("accountFound", accountFound);
            auditPayload.put("bankAvailable", bankAvailable);
            auditPayload.put("eligibleForTransfer", eligibleForTransfer);
            auditPayload.put("destinationAccountName", destinationAccountName);
            auditPayload.put("notEligibleReason", notEligibleReason);

            auditLogService.log(
                    "INQUIRY_RESPONDED",
                    "INQUIRY",
                    inquiryRef,
                    "API",
                    auditPayload);

            return new CreateInquiryResponse(
                    inquiryRef,
                    status.name(),
                    accountFound,
                    bankAvailable,
                    eligibleForTransfer,
                    destinationAccountName,
                    eligibleForTransfer
                            ? "Inquiry completed — eligible for transfer"
                            : "Inquiry completed — not eligible: " + notEligibleReason);

        } catch (InquiryValidationException ex) {
            auditLogService.logError("INQUIRY_VALIDATION_FAILED", "INQUIRY", null, "API", ex);
            throw ex;
        } catch (Exception ex) {
            auditLogService.logError("INQUIRY_FAILED", "INQUIRY", null, "API", ex);
            throw ex;
        }
    }

    private void saveHistory(String inquiryRef, String status, String reasonCode, LocalDateTime createdAt) {
        InquiryStatusHistoryEntity history = new InquiryStatusHistoryEntity();
        history.setInquiryRef(inquiryRef);
        history.setStatus(status);
        history.setReasonCode(reasonCode);
        history.setCreatedAt(createdAt);
        inquiryStatusHistoryRepository.save(history);
    }

    private String generateInquiryRef() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "INQ-" + timestamp + "-" + suffix;
    }
}
