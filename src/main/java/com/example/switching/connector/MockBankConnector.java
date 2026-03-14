package com.example.switching.connector;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.switching.dto.BankDispatchResult;
import com.example.switching.dto.DispatchTransferCommand;

@Component
public class MockBankConnector implements BankConnector {

    private static final BigDecimal SUCCESS_THRESHOLD = new BigDecimal("100000.00");
    private static final BigDecimal PENDING_THRESHOLD = new BigDecimal("500000.00");

    @Override
    public boolean supports(String connectorName) {
        return "MOCK_CONNECTOR".equalsIgnoreCase(connectorName);
    }

    @Override
    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        String externalReference = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        if (command.getAmount().compareTo(SUCCESS_THRESHOLD) < 0) {
            return new BankDispatchResult(
                    "SUCCESS",
                    externalReference,
                    "00",
                    "Approved by mock connector"
            );
        }

        if (command.getAmount().compareTo(PENDING_THRESHOLD) <= 0) {
            return new BankDispatchResult(
                    "PENDING",
                    externalReference,
                    "09",
                    "Processing by mock connector"
            );
        }

        return new BankDispatchResult(
                "FAILED",
                externalReference,
                "96",
                "Rejected by mock connector"
        );
    }
}