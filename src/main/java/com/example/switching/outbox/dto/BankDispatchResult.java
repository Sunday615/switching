package com.example.switching.outbox.dto;

public class BankDispatchResult {

    private boolean success;
    private String bankReference;
    private String message;

    public BankDispatchResult() {
    }

    public BankDispatchResult(boolean success, String bankReference, String message) {
        this.success = success;
        this.bankReference = bankReference;
        this.message = message;
    }

    public static BankDispatchResult success(String bankReference, String message) {
        return new BankDispatchResult(true, bankReference, message);
    }

    public static BankDispatchResult failure(String message) {
        return new BankDispatchResult(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}