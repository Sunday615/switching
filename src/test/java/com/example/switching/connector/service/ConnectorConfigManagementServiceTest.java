package com.example.switching.connector.service;

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

import com.example.switching.connector.dto.ConnectorConfigResponse;
import com.example.switching.connector.dto.CreateConnectorConfigRequest;
import com.example.switching.connector.dto.UpdateConnectorConfigRequest;
import com.example.switching.connector.entity.ConnectorConfigEntity;
import com.example.switching.connector.enums.ConnectorType;
import com.example.switching.connector.exception.ConnectorConfigAlreadyExistsException;
import com.example.switching.connector.exception.ConnectorConfigNotFoundException;
import com.example.switching.connector.repository.ConnectorConfigRepository;
import com.example.switching.participant.entity.ParticipantEntity;
import com.example.switching.participant.enums.ParticipantStatus;
import com.example.switching.participant.enums.ParticipantType;
import com.example.switching.participant.service.ParticipantService;

@ExtendWith(MockitoExtension.class)
class ConnectorConfigManagementServiceTest {

    @Mock
    private ConnectorConfigRepository connectorConfigRepository;

    @Mock
    private ParticipantService participantService;

    @InjectMocks
    private ConnectorConfigManagementService service;

    @Test
    void createValidatesBankNormalizesFieldsAndAppliesDefaults() {
        CreateConnectorConfigRequest request = new CreateConnectorConfigRequest();
        request.setConnectorName(" mock_bank_d_connector ");
        request.setBankCode(" bank_d ");
        request.setConnectorType(" mock ");

        when(connectorConfigRepository.findByConnectorName("MOCK_BANK_D_CONNECTOR"))
                .thenReturn(Optional.empty());
        when(participantService.findByBankCode("BANK_D")).thenReturn(participant("BANK_D"));

        ConnectorConfigResponse response = service.create(request);

        ArgumentCaptor<ConnectorConfigEntity> captor = ArgumentCaptor.forClass(ConnectorConfigEntity.class);
        verify(connectorConfigRepository).save(captor.capture());
        ConnectorConfigEntity saved = captor.getValue();

        assertThat(saved.getConnectorName()).isEqualTo("MOCK_BANK_D_CONNECTOR");
        assertThat(saved.getBankCode()).isEqualTo("BANK_D");
        assertThat(saved.getConnectorType()).isEqualTo(ConnectorType.MOCK);
        assertThat(saved.getTimeoutMs()).isEqualTo(5000);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getForceReject()).isFalse();
        assertThat(response.getConnectorName()).isEqualTo("MOCK_BANK_D_CONNECTOR");
        assertThat(response.getConnectorType()).isEqualTo("MOCK");
    }

    @Test
    void createThrowsWhenConnectorAlreadyExists() {
        CreateConnectorConfigRequest request = new CreateConnectorConfigRequest();
        request.setConnectorName("mock_bank_a_connector");
        request.setBankCode("BANK_A");
        request.setConnectorType("MOCK");

        when(connectorConfigRepository.findByConnectorName("MOCK_BANK_A_CONNECTOR"))
                .thenReturn(Optional.of(new ConnectorConfigEntity()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConnectorConfigAlreadyExistsException.class)
                .hasMessage("Connector config already exists: MOCK_BANK_A_CONNECTOR");

        verify(participantService, never()).findByBankCode(any());
        verify(connectorConfigRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidConnectorTypeBeforeBankValidation() {
        CreateConnectorConfigRequest request = new CreateConnectorConfigRequest();
        request.setConnectorName("TEST_CONNECTOR");
        request.setBankCode("BANK_A");
        request.setConnectorType("FTP");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid connectorType: FTP");

        verify(participantService, never()).findByBankCode(any());
        verify(connectorConfigRepository, never()).save(any());
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndTrimsBlankableValues() {
        ConnectorConfigEntity existing = connector("MOCK_BANK_D_CONNECTOR");
        existing.setEndpointUrl("https://old.example.test");
        existing.setTimeoutMs(5000);
        existing.setEnabled(true);
        existing.setForceReject(false);
        existing.setRejectReasonCode("AC01");
        existing.setRejectReasonMessage("Old reason");

        UpdateConnectorConfigRequest request = new UpdateConnectorConfigRequest();
        request.setEndpointUrl("   ");
        request.setTimeoutMs(9000);
        request.setEnabled(false);
        request.setForceReject(true);
        request.setRejectReasonCode(" AC04 ");
        request.setRejectReasonMessage(" Closed account ");

        when(connectorConfigRepository.findByConnectorName("MOCK_BANK_D_CONNECTOR"))
                .thenReturn(Optional.of(existing));

        ConnectorConfigResponse response = service.update(" mock_bank_d_connector ", request);

        assertThat(existing.getEndpointUrl()).isNull();
        assertThat(existing.getTimeoutMs()).isEqualTo(9000);
        assertThat(existing.getEnabled()).isFalse();
        assertThat(existing.getForceReject()).isTrue();
        assertThat(existing.getRejectReasonCode()).isEqualTo("AC04");
        assertThat(existing.getRejectReasonMessage()).isEqualTo("Closed account");
        assertThat(response.getForceReject()).isTrue();
        verify(connectorConfigRepository).save(existing);
    }

    @Test
    void updateThrowsWhenConnectorDoesNotExist() {
        when(connectorConfigRepository.findByConnectorName("MISSING_CONNECTOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("missing_connector", new UpdateConnectorConfigRequest()))
                .isInstanceOf(ConnectorConfigNotFoundException.class)
                .hasMessage("Connector config not found: MISSING_CONNECTOR");

        verify(connectorConfigRepository, never()).save(any());
    }

    private ConnectorConfigEntity connector(String connectorName) {
        ConnectorConfigEntity entity = new ConnectorConfigEntity();
        entity.setConnectorName(connectorName);
        entity.setBankCode("BANK_D");
        entity.setConnectorType(ConnectorType.MOCK);
        return entity;
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
