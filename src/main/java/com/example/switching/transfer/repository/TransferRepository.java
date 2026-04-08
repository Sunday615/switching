package com.example.switching.transfer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.switching.transfer.entity.TransferEntity;

@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, Long> {

    Optional<TransferEntity> findByTransferRef(String transferRef);
}