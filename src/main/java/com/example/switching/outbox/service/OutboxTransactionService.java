package com.example.switching.outbox.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxTransactionService {

    private static final String MESSAGE_TYPE = "TRANSFER_DISPATCH";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxTransactionService(OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueueTransferDispatch(DispatchTransferCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            LocalDateTime now = LocalDateTime.now();

            OutboxEventEntity event = new OutboxEventEntity();
            event.setTransferRef(command.getTransferRef());
            event.setMessageType(MESSAGE_TYPE);
            event.setPayload(payload);
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setCreatedAt(now);

            outboxEventRepository.save(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize dispatch command", ex);
        }
    }
}