package com.example.switching.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.entity.ParticipantBankEntity;

public interface ParticipantBankRepository extends JpaRepository<ParticipantBankEntity, Long> {
    Optional<ParticipantBankEntity> findByBankCode(String bankCode);
}