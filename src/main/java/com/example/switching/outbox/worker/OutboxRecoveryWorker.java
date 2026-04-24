package com.example.switching.outbox.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.switching.outbox.service.OutboxRecoveryService;

@Component
public class OutboxRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryWorker.class);

    private static final int STUCK_AFTER_MINUTES = 2;

    private final OutboxRecoveryService outboxRecoveryService;

    public OutboxRecoveryWorker(OutboxRecoveryService outboxRecoveryService) {
        this.outboxRecoveryService = outboxRecoveryService;
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckProcessingEvents() {
        int recoveredCount = outboxRecoveryService.recoverStuckProcessingEvents(STUCK_AFTER_MINUTES);

        if (recoveredCount > 0) {
            log.warn("Recovered {} stuck PROCESSING outbox event(s)", recoveredCount);
        }
    }
}