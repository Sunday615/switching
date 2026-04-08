package com.example.switching.routing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.switching.routing.entity.RoutingRuleEntity;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRuleEntity, Long> {
}