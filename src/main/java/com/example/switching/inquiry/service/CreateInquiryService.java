package com.example.switching.inquiry.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.inquiry.dto.CreateInquiryRequest;
import com.example.switching.inquiry.dto.CreateInquiryResponse;
import com.example.switching.inquiry.entity.InquiryEntity;
import com.example.switching.inquiry.entity.InquiryStatusHistoryEntity;
import com.example.switching.inquiry.enums.InquiryStatus;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.inquiry.repository.InquiryStatusHistoryRepository;

@Service
public class CreateInquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryStatusHistoryRepository inquiryStatusHistoryRepository;
    private final AuditLogService auditLogService;

    public CreateInquiryService(InquiryRepository inquiryRepository,
                                InquiryStatusHistoryRepository inquiryStatusHistoryRepository,
                                AuditLogService auditLogService) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryStatusHistoryRepository = inquiryStatusHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreateInquiryResponse create(CreateInquiryRequest request) {
        auditLogService.log(
                "INQUIRY_REQUEST_RECEIVED",
                "INQUIRY",
                null,
                "API",
                request
        );

        try {
            String inquiryRef = generateInquiryRef();
            LocalDateTime now = LocalDateTime.now();

            boolean bankAvailable = "BANK_B".equalsIgnoreCase(request.getDestinationBank());
            boolean accountFound = request.getCreditorAccount() != null
                    && request.getCreditorAccount().matches("\\d{10,20}");
            boolean eligibleForTransfer = bankAvailable && accountFound;
            String destinationAccountName = accountFound ? "MR DEMO RECEIVER" : null;
            InquiryStatus status = eligibleForTransfer ? InquiryStatus.ELIGIBLE : InquiryStatus.NOT_ELIGIBLE;

            InquiryEntity inquiry = new InquiryEntity();
            inquiry.setInquiryRef(inquiryRef);
            inquiry.setClientInquiryId(request.getClientInquiryId());
            inquiry.setSourceBank(request.getSourceBank());
            inquiry.setDestinationBank(request.getDestinationBank());
            inquiry.setCreditorAccount(request.getCreditorAccount());
            inquiry.setDestinationAccountName(destinationAccountName);
            inquiry.setAmount(request.getAmount());
            inquiry.setCurrency(request.getCurrency());
            inquiry.setChannelId("API");
            inquiry.setRouteCode(bankAvailable ? "ROUTE_BANK_B_PRIMARY" : null);
            inquiry.setConnectorName(bankAvailable ? "MOCK_CONNECTOR" : null);
            inquiry.setAccountFound(accountFound);
            inquiry.setBankAvailable(bankAvailable);
            inquiry.setEligibleForTransfer(eligibleForTransfer);
            inquiry.setStatus(status);
            inquiry.setErrorCode(eligibleForTransfer ? null : "INQUIRY_NOT_ELIGIBLE");
            inquiry.setErrorMessage(eligibleForTransfer ? null : "Destination bank or account is not eligible");
            inquiry.setReference(request.getReference());

            inquiryRepository.save(inquiry);

            saveHistory(inquiryRef, InquiryStatus.RECEIVED.name(), null, now);
            saveHistory(inquiryRef, status.name(), eligibleForTransfer ? null : "INQUIRY_NOT_ELIGIBLE", now);

            Map<String, Object> auditPayload = new LinkedHashMap<>();
            auditPayload.put("inquiryRef", inquiryRef);
            auditPayload.put("status", status.name());
            auditPayload.put("sourceBank", request.getSourceBank());
            auditPayload.put("destinationBank", request.getDestinationBank());
            auditPayload.put("creditorAccount", request.getCreditorAccount());
            auditPayload.put("amount", request.getAmount());
            auditPayload.put("currency", request.getCurrency());
            auditPayload.put("accountFound", accountFound);
            auditPayload.put("bankAvailable", bankAvailable);
            auditPayload.put("eligibleForTransfer", eligibleForTransfer);
            auditPayload.put("destinationAccountName", destinationAccountName);

            auditLogService.log(
                    "INQUIRY_RESPONDED",
                    "INQUIRY",
                    inquiryRef,
                    "API",
                    auditPayload
            );

            return new CreateInquiryResponse(
                    inquiryRef,
                    status.name(),
                    accountFound,
                    bankAvailable,
                    eligibleForTransfer,
                    destinationAccountName,
                    eligibleForTransfer ? "Inquiry completed" : "Inquiry completed with non-eligible result"
            );
        } catch (Exception ex) {
            auditLogService.logError(
                    "INQUIRY_FAILED",
                    "INQUIRY",
                    null,
                    "API",
                    ex
            );
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