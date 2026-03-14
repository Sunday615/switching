package com.example.switching.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DispatchTransferCommand {
    private Long outboxEventId;
    private String transferRef;
    private String sourceBankCode;
    private String sourceAccountNo;
    private String destinationBankCode;
    private String destinationAccountNo;
    private String destinationAccountName;
    private BigDecimal amount;
    private String currency;
    private String connectorName;
}