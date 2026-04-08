package com.example.switching.transfer.dto;

import java.util.ArrayList;
import java.util.List;

public class TransferInquiryResponse {

    private String transferRef;
    private String status;
    private String sourceBank;
    private String destinationBank;
    private List<TransferStatusHistoryItemResponse> history = new ArrayList<>();

    public TransferInquiryResponse() {
    }

    public String getTransferRef() {
        return transferRef;
    }

    public void setTransferRef(String transferRef) {
        this.transferRef = transferRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<TransferStatusHistoryItemResponse> getHistory() {
        return history;
    }

    public void setHistory(List<TransferStatusHistoryItemResponse> history) {
        this.history = history;
    }
}