package com.example.switching.outbox.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.service.OutboxTransactionService;

@Component
public class OutboxDispatchWorker {

    private final OutboxTransactionService outboxTransactionService;

    public OutboxDispatchWorker(OutboxTransactionService outboxTransactionService) {
        this.outboxTransactionService = outboxTransactionService;
    }

    @Scheduled(fixedDelay = 30000)
    public void processPendingEvents() {
        // skeleton only
    }

    public BankDispatchResult dispatchNow(DispatchTransferCommand command) {
        return outboxTransactionService.dispatch(command);
    }
}