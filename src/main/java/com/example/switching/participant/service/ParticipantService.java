package com.example.switching.participant.service;

import com.example.switching.participant.entity.ParticipantBankEntity;
import com.example.switching.participant.repository.ParticipantBankRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ParticipantService {

    private final ParticipantBankRepository participantBankRepository;

    public ParticipantService(ParticipantBankRepository participantBankRepository) {
        this.participantBankRepository = participantBankRepository;
    }

    public List<ParticipantBankEntity> findAll() {
        try {
            return participantBankRepository.findAll();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public Optional<ParticipantBankEntity> findById(Long id) {
        try {
            return participantBankRepository.findById(id);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<ParticipantBankEntity> findByParticipantCode(String participantCode) {
        // TODO: เปลี่ยนเป็น custom query เมื่อรู้ field จริงใน entity/repository
        return Optional.empty();
    }

    public boolean isActive(String participantCode) {
        // TODO: เช็กจาก status จริงของ participant
        return true;
    }
}