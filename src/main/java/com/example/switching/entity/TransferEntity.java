package com.example.switching.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.switching.enums.TransferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_ref", nullable = false, unique = true, length = 40)
    private String transferRef;

    @Column(name = "client_transfer_id", nullable = false, length = 80)
    private String clientTransferId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "source_bank_code", nullable = false, length = 20)
    private String sourceBankCode;

    @Column(name = "source_account_no", nullable = false, length = 40)
    private String sourceAccountNo;

    @Column(name = "destination_bank_code", nullable = false, length = 20)
    private String destinationBankCode;

    @Column(name = "destination_account_no", nullable = false, length = 40)
    private String destinationAccountNo;

    @Column(name = "destination_account_name", length = 120)
    private String destinationAccountName;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "channel_id", nullable = false, length = 40)
    private String channelId;

    @Column(name = "route_code", length = 40)
    private String routeCode;

    @Column(name = "connector_name", length = 80)
    private String connectorName;

    @Column(name = "external_reference", length = 80)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}