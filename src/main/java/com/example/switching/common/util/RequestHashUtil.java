package com.example.switching.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.example.switching.transfer.dto.CreateTransferRequest;

@Component
public class RequestHashUtil {

    public String hash(CreateTransferRequest request) {
        String raw = String.join("|",
                safe(request.getSourceBank()),
                safe(request.getDestinationBank()),
                safe(request.getDebtorAccount()),
                safe(request.getCreditorAccount()),
                request.getAmount() == null ? "" : request.getAmount().toPlainString(),
                safe(request.getCurrency()),
                safe(request.getReference()),
                safe(request.getIdempotencyKey())
        );
        return sha256(raw);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashBytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash request", ex);
        }
    }
}