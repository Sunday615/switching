package com.example.switching.idempotency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.switching.idempotency.entity.IdempotencyRecordEntity;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
}