package com.example.switching.outbox.worker;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.example.switching.outbox.service.OutboxProcessorService;

@Component
public class OutboxDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProcessorService outboxProcessorService;

    public OutboxDispatchWorker(OutboxEventRepository outboxEventRepository,
                                OutboxProcessorService outboxProcessorService) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxProcessorService = outboxProcessorService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        List<OutboxEventEntity> events =
                outboxEventRepository.findTop20ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox worker found {} pending event(s)", events.size());

        for (OutboxEventEntity event : events) {
            outboxProcessorService.processSingleEvent(event.getId());
        }
    }
}