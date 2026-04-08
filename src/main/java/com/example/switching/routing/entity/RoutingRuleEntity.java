package com.example.switching.routing.entity;

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
@Table(name = "routing_rules")
public class RoutingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", nullable = false, unique = true, length = 40)
    private String routeCode;

    @Column(name = "destination_bank_code", nullable = false, length = 20)
    private String destinationBankCode;

    @Column(name = "connector_name", nullable = false, length = 80)
    private String connectorName;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag;
}