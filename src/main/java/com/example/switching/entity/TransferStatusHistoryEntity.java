package com.example.switching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transfer_status_history")
public class TransferStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_ref", nullable = false, length = 40)
    private String transferRef;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "reason_message", length = 255)
    private String reasonMessage;

    @Column(name = "changed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false, length = 60)
    private String changedBy;
}