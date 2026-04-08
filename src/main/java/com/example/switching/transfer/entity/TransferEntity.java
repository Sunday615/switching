package com.example.switching.transfer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.switching.transfer.enums.TransferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfers")
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_ref", nullable = false, unique = true)
    private String transferRef;

    @Column(name = "client_transfer_id", nullable = false)
    private String clientTransferId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "source_bank_code", nullable = false)
    private String sourceBank;

    @Column(name = "source_account_no", nullable = false)
    private String debtorAccount;

    @Column(name = "destination_bank_code", nullable = false)
    private String destinationBank;

    @Column(name = "destination_account_no", nullable = false)
    private String creditorAccount;

    @Column(name = "destination_account_name")
    private String destinationAccountName;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "route_code")
    private String routeCode;

    @Column(name = "connector_name")
    private String connectorName;

    @Column(name = "external_reference")
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public TransferEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getTransferRef() {
        return transferRef;
    }

    public void setTransferRef(String transferRef) {
        this.transferRef = transferRef;
    }

    public String getClientTransferId() {
        return clientTransferId;
    }

    public void setClientTransferId(String clientTransferId) {
        this.clientTransferId = clientTransferId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSourceBank() {
        return sourceBank;
    }

    public void setSourceBank(String sourceBank) {
        this.sourceBank = sourceBank;
    }

    public String getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(String debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public String getDestinationBank() {
        return destinationBank;
    }

    public void setDestinationBank(String destinationBank) {
        this.destinationBank = destinationBank;
    }

    public String getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(String creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public String getDestinationAccountName() {
        return destinationAccountName;
    }

    public void setDestinationAccountName(String destinationAccountName) {
        this.destinationAccountName = destinationAccountName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}