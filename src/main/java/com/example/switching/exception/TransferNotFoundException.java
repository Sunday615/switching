package com.example.switching.exception;

public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(String transferRef) {
        super("Transfer not found: " + transferRef);
    }
}