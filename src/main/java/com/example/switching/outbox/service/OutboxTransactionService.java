package com.example.switching.outbox.service;

import org.springframework.stereotype.Service;

import com.example.switching.connector.BankConnector;
import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.DispatchTransferCommand;

@Service
public class OutboxTransactionService {

    private final BankConnector bankConnector;

    public OutboxTransactionService(BankConnector bankConnector) {
        this.bankConnector = bankConnector;
    }

    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        return bankConnector.dispatch(command);
    }
}