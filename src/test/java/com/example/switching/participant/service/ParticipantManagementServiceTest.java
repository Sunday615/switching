package com.example.switching.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.switching.participant.dto.CreateParticipantRequest;
import com.example.switching.participant.dto.ParticipantResponse;
import com.example.switching.participant.dto.UpdateParticipantRequest;
import com.example.switching.participant.entity.ParticipantEntity;
import com.example.switching.participant.enums.ParticipantStatus;
import com.example.switching.participant.enums.ParticipantType;
import com.example.switching.participant.exception.ParticipantAlreadyExistsException;
import com.example.switching.participant.exception.ParticipantNotFoundException;
import com.example.switching.participant.repository.ParticipantRepository;

@ExtendWith(MockitoExtension.class)
class ParticipantManagementServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantManagementService service;

    @Test
    void createNormalizesFieldsAndAppliesDefaults() {
        CreateParticipantRequest request = new CreateParticipantRequest();
        request.setBankCode(" bank_d ");
        request.setBankName(" Demo Bank D ");
        request.setCountry(" th ");
        request.setCurrency(" thb ");

        when(participantRepository.findByBankCode("BANK_D")).thenReturn(Optional.empty());

        ParticipantResponse response = service.create(request);

        ArgumentCaptor<ParticipantEntity> captor = ArgumentCaptor.forClass(ParticipantEntity.class);
        verify(participantRepository).save(captor.capture());
        ParticipantEntity saved = captor.getValue();

        assertThat(saved.getBankCode()).isEqualTo("BANK_D");
        assertThat(saved.getBankName()).isEqualTo("Demo Bank D");
        assertThat(saved.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
        assertThat(saved.getParticipantType()).isEqualTo(ParticipantType.BANK);
        assertThat(saved.getCountry()).isEqualTo("TH");
        assertThat(saved.getCurrency()).isEqualTo("THB");
        assertThat(response.getBankCode()).isEqualTo("BANK_D");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createThrowsWhenParticipantAlreadyExists() {
        CreateParticipantRequest request = new CreateParticipantRequest();
        request.setBankCode("bank_a");
        request.setBankName("Duplicate");
        request.setCountry("TH");
        request.setCurrency("THB");

        when(participantRepository.findByBankCode("BANK_A")).thenReturn(Optional.of(participant("BANK_A")));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ParticipantAlreadyExistsException.class)
                .hasMessage("Participant already exists: BANK_A");

        verify(participantRepository, never()).save(any());
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndNormalizesValues() {
        ParticipantEntity existing = participant("BANK_D");
        existing.setBankName("Old Name");
        existing.setStatus(ParticipantStatus.ACTIVE);
        existing.setCountry("TH");
        existing.setCurrency("THB");

        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setBankName(" Demo Bank D Updated ");
        request.setStatus(" inactive ");
        request.setCurrency(" usd ");

        when(participantRepository.findByBankCode("BANK_D")).thenReturn(Optional.of(existing));

        ParticipantResponse response = service.update(" bank_d ", request);

        assertThat(existing.getBankName()).isEqualTo("Demo Bank D Updated");
        assertThat(existing.getStatus()).isEqualTo(ParticipantStatus.INACTIVE);
        assertThat(existing.getCountry()).isEqualTo("TH");
        assertThat(existing.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isEqualTo("INACTIVE");
        verify(participantRepository).save(existing);
    }

    @Test
    void updateThrowsWhenParticipantDoesNotExist() {
        when(participantRepository.findByBankCode("BANK_Z")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("bank_z", new UpdateParticipantRequest()))
                .isInstanceOf(ParticipantNotFoundException.class)
                .hasMessage("Participant not found: BANK_Z");

        verify(participantRepository, never()).save(any());
    }

    private ParticipantEntity participant(String bankCode) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.setBankCode(bankCode);
        entity.setBankName(bankCode + " Name");
        entity.setStatus(ParticipantStatus.ACTIVE);
        entity.setParticipantType(ParticipantType.BANK);
        entity.setCountry("TH");
        entity.setCurrency("THB");
        return entity;
    }
}
