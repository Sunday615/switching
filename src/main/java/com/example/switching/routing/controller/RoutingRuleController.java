package com.example.switching.routing.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.routing.dto.RoutingResolveResponse;
import com.example.switching.routing.dto.RoutingRuleListResponse;
import com.example.switching.routing.service.RoutingService;
@RestController
public class RoutingRuleController {

    private final RoutingService routingService;

    public RoutingRuleController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/api/routing-rules")
    public RoutingRuleListResponse list() {
        return routingService.list();
    }

    @GetMapping("/api/routing-rules/resolve")
    public RoutingResolveResponse resolve(
            @RequestParam String sourceBank,
            @RequestParam String destinationBank,
            @RequestParam String messageType
    ) {
        return routingService.resolve(
                sourceBank,
                destinationBank,
                messageType
        );
    }

    @PostMapping("/api/routing-rules/cache/clear")
public Map<String, Object> clearCache() {
    routingService.clearCache();

    return Map.of(
            "message", "Routing cache cleared",
            "cacheSize", routingService.cacheSize()
    );
}
}