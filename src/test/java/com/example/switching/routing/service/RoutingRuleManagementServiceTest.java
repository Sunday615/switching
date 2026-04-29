package com.example.switching.routing.service;

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

import com.example.switching.iso.enums.IsoMessageType;
import com.example.switching.participant.entity.ParticipantEntity;
import com.example.switching.participant.enums.ParticipantStatus;
import com.example.switching.participant.enums.ParticipantType;
import com.example.switching.participant.service.ParticipantService;
import com.example.switching.routing.dto.CreateRoutingRuleRequest;
import com.example.switching.routing.dto.RoutingRuleResponse;
import com.example.switching.routing.dto.UpdateRoutingRuleRequest;
import com.example.switching.routing.entity.RoutingRuleEntity;
import com.example.switching.routing.exception.RoutingRuleAlreadyExistsException;
import com.example.switching.routing.exception.RoutingRuleNotFoundException;
import com.example.switching.routing.repository.RoutingRuleRepository;

@ExtendWith(MockitoExtension.class)
class RoutingRuleManagementServiceTest {

    @Mock
    private RoutingRuleRepository routingRuleRepository;

    @Mock
    private ParticipantService participantService;

    @Mock
    private RoutingService routingService;

    @InjectMocks
    private RoutingRuleManagementService service;

    @Test
    void createValidatesParticipantsNormalizesFieldsAndClearsCache() {
        CreateRoutingRuleRequest request = new CreateRoutingRuleRequest();
        request.setRouteCode(" route_bank_a_to_bank_d_pacs008_primary ");
        request.setSourceBank(" bank_a ");
        request.setDestinationBank(" bank_d ");
        request.setMessageType(" pacs_008 ");
        request.setConnectorName(" mock_bank_d_connector ");

        when(routingRuleRepository.findByRouteCode("ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY"))
                .thenReturn(Optional.empty());
        when(participantService.findByBankCode("BANK_A")).thenReturn(participant("BANK_A"));
        when(participantService.findByBankCode("BANK_D")).thenReturn(participant("BANK_D"));

        RoutingRuleResponse response = service.create(request);

        ArgumentCaptor<RoutingRuleEntity> captor = ArgumentCaptor.forClass(RoutingRuleEntity.class);
        verify(routingRuleRepository).save(captor.capture());
        RoutingRuleEntity saved = captor.getValue();

        assertThat(saved.getRouteCode()).isEqualTo("ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY");
        assertThat(saved.getSourceBank()).isEqualTo("BANK_A");
        assertThat(saved.getDestinationBank()).isEqualTo("BANK_D");
        assertThat(saved.getMessageType()).isEqualTo(IsoMessageType.PACS_008);
        assertThat(saved.getConnectorName()).isEqualTo("MOCK_BANK_D_CONNECTOR");
        assertThat(saved.getPriority()).isEqualTo(1);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(response.getRouteCode()).isEqualTo("ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY");
        verify(routingService).clearCache();
    }

    @Test
    void createThrowsWhenRouteCodeAlreadyExists() {
        CreateRoutingRuleRequest request = new CreateRoutingRuleRequest();
        request.setRouteCode("route_bank_a_to_bank_b_pacs008_primary");
        request.setSourceBank("BANK_A");
        request.setDestinationBank("BANK_B");
        request.setMessageType("PACS_008");
        request.setConnectorName("MOCK_BANK_B_CONNECTOR");

        when(routingRuleRepository.findByRouteCode("ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY"))
                .thenReturn(Optional.of(new RoutingRuleEntity()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RoutingRuleAlreadyExistsException.class)
                .hasMessage("Routing rule already exists: ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY");

        verify(participantService, never()).findByBankCode(any());
        verify(routingRuleRepository, never()).save(any());
        verify(routingService, never()).clearCache();
    }

    @Test
    void createRejectsInvalidMessageTypeBeforeParticipantValidation() {
        CreateRoutingRuleRequest request = new CreateRoutingRuleRequest();
        request.setRouteCode("ROUTE_TEST");
        request.setSourceBank("BANK_A");
        request.setDestinationBank("BANK_B");
        request.setMessageType("INVALID_TYPE");
        request.setConnectorName("MOCK_BANK_B_CONNECTOR");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid messageType: INVALID_TYPE");

        verify(participantService, never()).findByBankCode(any());
        verify(routingRuleRepository, never()).save(any());
    }

    @Test
    void updateChangesOptionalFieldsAndClearsCache() {
        RoutingRuleEntity existing = new RoutingRuleEntity();
        existing.setRouteCode("ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY");
        existing.setSourceBank("BANK_A");
        existing.setDestinationBank("BANK_D");
        existing.setMessageType(IsoMessageType.PACS_008);
        existing.setConnectorName("MOCK_OLD_CONNECTOR");
        existing.setPriority(9);
        existing.setEnabled(true);

        UpdateRoutingRuleRequest request = new UpdateRoutingRuleRequest();
        request.setConnectorName(" mock_bank_d_connector ");
        request.setPriority(2);
        request.setEnabled(false);

        when(routingRuleRepository.findByRouteCode("ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY"))
                .thenReturn(Optional.of(existing));

        RoutingRuleResponse response = service.update(" route_bank_a_to_bank_d_pacs008_primary ", request);

        assertThat(existing.getConnectorName()).isEqualTo("MOCK_BANK_D_CONNECTOR");
        assertThat(existing.getPriority()).isEqualTo(2);
        assertThat(existing.getEnabled()).isFalse();
        assertThat(response.getEnabled()).isFalse();
        verify(routingRuleRepository).save(existing);
        verify(routingService).clearCache();
    }

    @Test
    void updateThrowsWhenRouteDoesNotExist() {
        when(routingRuleRepository.findByRouteCode("ROUTE_MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("route_missing", new UpdateRoutingRuleRequest()))
                .isInstanceOf(RoutingRuleNotFoundException.class)
                .hasMessage("Routing rule not found. sourceBank=ROUTE_MISSING, destinationBank=-, messageType=-");

        verify(routingRuleRepository, never()).save(any());
        verify(routingService, never()).clearCache();
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
