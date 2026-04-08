package com.example.switching.routing.service;

import com.example.switching.routing.entity.RoutingRuleEntity;
import com.example.switching.routing.repository.RoutingRuleRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class RoutingService {

    private final RoutingRuleRepository routingRuleRepository;

    public RoutingService(RoutingRuleRepository routingRuleRepository) {
        this.routingRuleRepository = routingRuleRepository;
    }

    public List<RoutingRuleEntity> findAll() {
        try {
            return routingRuleRepository.findAll();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public Optional<RoutingRuleEntity> findById(Long id) {
        try {
            return routingRuleRepository.findById(id);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<RoutingRuleEntity> resolveRoute(String fromParticipant, String toParticipant, String messageType) {
        // TODO: เปลี่ยนเป็น query จริง เช่น findActiveRoute(from, to, type)
        return Optional.empty();
    }

    public boolean hasRoute(String fromParticipant, String toParticipant, String messageType) {
        return resolveRoute(fromParticipant, toParticipant, messageType).isPresent();
    }
}