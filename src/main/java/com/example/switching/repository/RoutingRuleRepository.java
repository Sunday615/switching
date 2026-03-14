package com.example.switching.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.RoutingRuleEntity;

public interface RoutingRuleRepository extends JpaRepository<RoutingRuleEntity, Long> {
    Optional<RoutingRuleEntity> findFirstByDestinationBankCodeAndActiveFlagTrueOrderByPriorityAsc(String destinationBankCode);
}