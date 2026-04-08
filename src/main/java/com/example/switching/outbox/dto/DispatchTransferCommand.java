package com.example.switching.outbox.dto;

public class DispatchTransferCommand {

    private String transferRef;
    private String sourceBank;
    private String destinationBank;
    private String payload;

    public DispatchTransferCommand() {
    }

    public DispatchTransferCommand(String transferRef, String sourceBank, String destinationBank, String payload) {
        this.transferRef = transferRef;
        this.sourceBank = sourceBank;
        this.destinationBank = destinationBank;
        this.payload = payload;
    }

    public String getTransferRef() {
        return transferRef;
    }

    public void setTransferRef(String transferRef) {
        this.transferRef = transferRef;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}