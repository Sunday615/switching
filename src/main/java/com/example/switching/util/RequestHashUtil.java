package com.example.switching.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.example.switching.dto.CreateTransferRequest;

public final class RequestHashUtil {

    private RequestHashUtil() {
    }

    public static String hash(CreateTransferRequest request) {
        String raw = String.join("|",
                request.getClientTransferId(),
                request.getSourceBankCode(),
                request.getSourceAccountNo(),
                request.getDestinationBankCode(),
                request.getDestinationAccountNo(),
                request.getDestinationAccountName(),
                request.getAmount().toPlainString(),
                request.getCurrency()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot hash request", e);
        }
    }
}