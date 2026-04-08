package com.example.switching.connector;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.DispatchTransferCommand;

@Component
public class MockBankConnector implements BankConnector {

    @Override
    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        String ref = "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return BankDispatchResult.success(ref,
                "Mock dispatch success to " + command.getDestinationBank());
    }
}