package com.example.switching.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.TransferEntity;

public interface TransferRepository extends JpaRepository<TransferEntity, Long> {
    Optional<TransferEntity> findByTransferRef(String transferRef);
    Optional<TransferEntity> findByChannelIdAndClientTransferId(String channelId, String clientTransferId);
}