package com.example.switching.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTransferRequest {

    @NotBlank
    private String clientTransferId;

    @NotBlank
    private String sourceBankCode;

    @NotBlank
    private String sourceAccountNo;

    @NotBlank
    private String destinationBankCode;

    @NotBlank
    private String destinationAccountNo;

    @NotBlank
    private String destinationAccountName;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    private String remark;
}