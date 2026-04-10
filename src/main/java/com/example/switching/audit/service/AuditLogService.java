package com.example.switching.audit.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.switching.audit.entity.AuditLogEntity;
import com.example.switching.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public AuditLogEntity log(String eventType,
                              String referenceType,
                              String referenceId,
                              String actor,
                              Object payload) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setEventType(eventType);
        entity.setReferenceType(referenceType);
        entity.setReferenceId(referenceId);
        entity.setActor(actor);
        entity.setPayload(toJson(payload));
        entity.setCreatedAt(LocalDateTime.now());

        return auditLogRepository.save(entity);
    }

    public AuditLogEntity log(String eventType,
                              String referenceType,
                              String referenceId,
                              Object payload) {
        return log(eventType, referenceType, referenceId, "SYSTEM", payload);
    }

    public AuditLogEntity logError(String eventType,
                                   String referenceType,
                                   String referenceId,
                                   String actor,
                                   Exception exception) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setEventType(eventType);
        entity.setReferenceType(referenceType);
        entity.setReferenceId(referenceId);
        entity.setActor(actor);
        entity.setPayload(buildErrorPayload(exception));
        entity.setCreatedAt(LocalDateTime.now());

        return auditLogRepository.save(entity);
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"payload_serialization_failed\",\"message\":\""
                    + safe(ex.getMessage())
                    + "\"}";
        }
    }

    private String buildErrorPayload(Exception exception) {
        String exceptionClass = exception == null ? "UnknownException" : exception.getClass().getSimpleName();
        String message = exception == null ? null : exception.getMessage();

        return "{\"error\":true,\"exception\":\""
                + safe(exceptionClass)
                + "\",\"message\":\""
                + safe(message)
                + "\"}";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
