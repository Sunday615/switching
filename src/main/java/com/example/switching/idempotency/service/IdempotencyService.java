package com.example.switching.idempotency.service;

import com.example.switching.idempotency.entity.IdempotencyRecordEntity;
import com.example.switching.idempotency.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public boolean isDuplicate(String idempotencyKey, String requestHash) {
        // TODO:
        // 1) query by idempotency key
        // 2) compare request hash
        // 3) return true เมื่อเป็น request ซ้ำ
        return false;
    }

    public Optional<IdempotencyRecordEntity> findById(Long id) {
        try {
            return idempotencyRecordRepository.findById(id);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public IdempotencyRecordEntity save(IdempotencyRecordEntity entity) {
        return idempotencyRecordRepository.save(entity);
    }
}