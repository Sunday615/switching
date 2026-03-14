package com.example.switching.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferInquiryResponse {
    private String transferRef;
    private String clientTransferId;
    private String sourceBankCode;
    private String sourceAccountNo;
    private String destinationBankCode;
    private String destinationAccountNo;
    private String destinationAccountName;
    private BigDecimal amount;
    private String currency;
    private String channelId;
    private String routeCode;
    private String connectorName;
    private String externalReference;
    private String status;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TransferStatusHistoryItemResponse> statusHistory;
}