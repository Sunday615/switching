package com.example.switching.connector.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.connector.dto.ConnectorConfigListResponse;
import com.example.switching.connector.dto.ConnectorConfigResponse;
import com.example.switching.connector.service.ConnectorConfigService;

@RestController
public class ConnectorConfigController {

    private final ConnectorConfigService connectorConfigService;

    public ConnectorConfigController(ConnectorConfigService connectorConfigService) {
        this.connectorConfigService = connectorConfigService;
    }

    @GetMapping("/api/connector-configs")
    public ConnectorConfigListResponse list() {
        return connectorConfigService.list();
    }

    @GetMapping("/api/connector-configs/{connectorName}")
    public ConnectorConfigResponse getByConnectorName(
            @PathVariable String connectorName
    ) {
        return connectorConfigService.getByConnectorName(connectorName);
    }
}