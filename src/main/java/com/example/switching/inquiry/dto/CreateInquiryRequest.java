package com.example.switching.inquiry.dto;

import java.math.BigDecimal;

public class CreateInquiryRequest {

    private String clientInquiryId;
    private String sourceBank;
    private String destinationBank;
    private String creditorAccount;
    private BigDecimal amount;
    private String currency;
    private String reference;

    public String getClientInquiryId() {
        return clientInquiryId;
    }

    public void setClientInquiryId(String clientInquiryId) {
        this.clientInquiryId = clientInquiryId;
    }

    public String getSourceBank() {
        return sourceBank;
    }

    public void setSourceBank(String sourceBank) {
        this.sourceBank = sourceBank;
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
}
