package com.example.switching.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferStatusHistoryItemResponse {
    private String fromStatus;
    private String toStatus;
    private String reasonCode;
    private String reasonMessage;
    private String changedBy;
    private LocalDateTime changedAt;
}