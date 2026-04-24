package com.example.switching.transfer.dto;

import java.math.BigDecimal;
import java.util.List;

public class TransferTraceResponse {

    private String transferRef;
    private String inquiryRef;
    private String currentStatus;

    private String sourceBank;
    private String debtorAccount;
    private String destinationBank;
    private String creditorAccount;
    private String destinationAccountName;

    private BigDecimal amount;
    private String currency;
    private String reference;
    private String externalReference;
    private String errorCode;
    private String errorMessage;

    private String inquiryStatus;
    private Boolean inquiryAccountFound;
    private Boolean inquiryBankAvailable;
    private Boolean inquiryEligibleForTransfer;

    private List<TransferTraceHistoryItemResponse> transferHistory;
    private List<TransferTraceOutboxItemResponse> outboxEvents;
    private List<TransferTraceAuditItemResponse> auditEvents;

    public TransferTraceResponse() {
    }

    public String getTransferRef() {
        return transferRef;
    }

    public void setTransferRef(String transferRef) {
        this.transferRef = transferRef;
    }

    public String getInquiryRef() {
        return inquiryRef;
    }

    public void setInquiryRef(String inquiryRef) {
        this.inquiryRef = inquiryRef;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getSourceBank() {
        return sourceBank;
    }

    public void setSourceBank(String sourceBank) {
        this.sourceBank = sourceBank;
    }

    public String getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(String debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public String getDestinationBank() {
        return destinationBank;
    }

    public void setDestinationBank(String destinationBank) {
        this.destinationBank = destinationBank;
    }

    public String getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(String creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public String getDestinationAccountName() {
        return destinationAccountName;
    }

    public void setDestinationAccountName(String destinationAccountName) {
        this.destinationAccountName = destinationAccountName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getInquiryStatus() {
        return inquiryStatus;
    }

    public void setInquiryStatus(String inquiryStatus) {
        this.inquiryStatus = inquiryStatus;
    }

    public Boolean getInquiryAccountFound() {
        return inquiryAccountFound;
    }

    public void setInquiryAccountFound(Boolean inquiryAccountFound) {
        this.inquiryAccountFound = inquiryAccountFound;
    }

    public Boolean getInquiryBankAvailable() {
        return inquiryBankAvailable;
    }

    public void setInquiryBankAvailable(Boolean inquiryBankAvailable) {
        this.inquiryBankAvailable = inquiryBankAvailable;
    }

    public Boolean getInquiryEligibleForTransfer() {
        return inquiryEligibleForTransfer;
    }

    public void setInquiryEligibleForTransfer(Boolean inquiryEligibleForTransfer) {
        this.inquiryEligibleForTransfer = inquiryEligibleForTransfer;
    }

    public List<TransferTraceHistoryItemResponse> getTransferHistory() {
        return transferHistory;
    }

    public void setTransferHistory(List<TransferTraceHistoryItemResponse> transferHistory) {
        this.transferHistory = transferHistory;
    }

    public List<TransferTraceOutboxItemResponse> getOutboxEvents() {
        return outboxEvents;
    }

    public void setOutboxEvents(List<TransferTraceOutboxItemResponse> outboxEvents) {
        this.outboxEvents = outboxEvents;
    }

    public List<TransferTraceAuditItemResponse> getAuditEvents() {
        return auditEvents;
    }

    public void setAuditEvents(List<TransferTraceAuditItemResponse> auditEvents) {
        this.auditEvents = auditEvents;
    }
}