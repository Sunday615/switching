package com.example.switching.outbox.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.switching.outbox.dto.DispatchTransferCommand;
import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxTransactionService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxTransactionService(OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public OutboxEventEntity enqueueTransferDispatch(DispatchTransferCommand command) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setTransferRef(command.getTransferRef());
        event.setMessageType("TRANSFER_DISPATCH");
        event.setPayload(toJson(command));
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());

        return outboxEventRepository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox payload", ex);
        }
    }
}