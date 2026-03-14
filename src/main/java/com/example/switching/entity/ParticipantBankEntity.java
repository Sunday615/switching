package com.example.switching.entity;

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
@Table(name = "participant_banks")
public class ParticipantBankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code", nullable = false, unique = true, length = 20)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "short_name", length = 40)
    private String shortName;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag;

    @Column(name = "maintenance_flag", nullable = false)
    private Boolean maintenanceFlag;
}