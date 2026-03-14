package com.example.switching.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.TransferStatusHistoryEntity;

public interface TransferStatusHistoryRepository extends JpaRepository<TransferStatusHistoryEntity, Long> {
    List<TransferStatusHistoryEntity> findByTransferRefOrderByChangedAtAsc(String transferRef);
}