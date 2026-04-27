package com.example.switching.iso.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.switching.iso.dto.IsoMessageDetailResponse;
import com.example.switching.iso.dto.IsoMessageItemResponse;
import com.example.switching.iso.dto.IsoMessageListResponse;
import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoMessageDirection;
import com.example.switching.iso.enums.IsoMessageType;
import com.example.switching.iso.exception.IsoMessageNotFoundException;
import com.example.switching.iso.repository.IsoMessageRepository;

@Service
public class IsoMessageQueryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final IsoMessageRepository isoMessageRepository;

    public IsoMessageQueryService(IsoMessageRepository isoMessageRepository) {
        this.isoMessageRepository = isoMessageRepository;
    }

    @Transactional(readOnly = true)
    public IsoMessageListResponse search(String messageType,
                                         String direction,
                                         String correlationRef,
                                         String inquiryRef,
                                         String transferRef,
                                         String endToEndId,
                                         Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        IsoMessageType resolvedMessageType = resolveMessageType(messageType);
        IsoMessageDirection resolvedDirection = resolveDirection(direction);

        String resolvedCorrelationRef = normalize(correlationRef);
        String resolvedInquiryRef = normalize(inquiryRef);
        String resolvedTransferRef = normalize(transferRef);
        String resolvedEndToEndId = normalize(endToEndId);

        List<IsoMessageEntity> messages = isoMessageRepository.searchIsoMessages(
                resolvedMessageType,
                resolvedDirection,
                resolvedCorrelationRef,
                resolvedInquiryRef,
                resolvedTransferRef,
                resolvedEndToEndId,
                PageRequest.of(0, resolvedLimit)
        );

        List<IsoMessageItemResponse> items = messages.stream()
                .map(this::toItemResponse)
                .toList();

        return new IsoMessageListResponse(
                items.size(),
                resolvedLimit,
                resolvedMessageType == null ? null : resolvedMessageType.name(),
                resolvedDirection == null ? null : resolvedDirection.name(),
                resolvedCorrelationRef,
                resolvedInquiryRef,
                resolvedTransferRef,
                resolvedEndToEndId,
                items
        );
    }

    @Transactional(readOnly = true)
    public IsoMessageDetailResponse getById(Long id) {
        IsoMessageEntity message = isoMessageRepository.findById(id)
                .orElseThrow(() -> new IsoMessageNotFoundException("ISO message not found: " + id));

        return toDetailResponse(message);
    }

    private IsoMessageItemResponse toItemResponse(IsoMessageEntity message) {
        return new IsoMessageItemResponse(
                message.getId(),
                message.getCorrelationRef(),
                message.getInquiryRef(),
                message.getTransferRef(),
                message.getEndToEndId(),
                message.getMessageId(),
                message.getMessageType() == null ? null : message.getMessageType().name(),
                message.getDirection() == null ? null : message.getDirection().name(),
                message.getSecurityStatus() == null ? null : message.getSecurityStatus().name(),
                message.getValidationStatus() == null ? null : message.getValidationStatus().name(),
                message.getErrorCode(),
                message.getErrorMessage(),
                message.getCreatedAt()
        );
    }

    private IsoMessageDetailResponse toDetailResponse(IsoMessageEntity message) {
        IsoMessageDetailResponse response = new IsoMessageDetailResponse();

        response.setId(message.getId());
        response.setCorrelationRef(message.getCorrelationRef());
        response.setInquiryRef(message.getInquiryRef());
        response.setTransferRef(message.getTransferRef());
        response.setEndToEndId(message.getEndToEndId());
        response.setMessageId(message.getMessageId());
        response.setMessageType(message.getMessageType() == null ? null : message.getMessageType().name());
        response.setDirection(message.getDirection() == null ? null : message.getDirection().name());
        response.setPlainPayload(message.getPlainPayload());
        response.setEncryptedPayload(message.getEncryptedPayload());
        response.setSecurityStatus(message.getSecurityStatus() == null ? null : message.getSecurityStatus().name());
        response.setValidationStatus(message.getValidationStatus() == null ? null : message.getValidationStatus().name());
        response.setErrorCode(message.getErrorCode());
        response.setErrorMessage(message.getErrorMessage());
        response.setCreatedAt(message.getCreatedAt());

        return response;
    }

    private IsoMessageType resolveMessageType(String messageType) {
        String normalized = normalize(messageType);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        try {
            return IsoMessageType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ISO message type: " + messageType);
        }
    }

    private IsoMessageDirection resolveDirection(String direction) {
        String normalized = normalize(direction);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        try {
            return IsoMessageDirection.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ISO message direction: " + direction);
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}