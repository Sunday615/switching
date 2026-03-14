package com.example.switching.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}