package com.example.switching.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.OutboxEventEntity;
import com.example.switching.enums.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    List<OutboxEventEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}