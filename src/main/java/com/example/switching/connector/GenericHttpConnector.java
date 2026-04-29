package com.example.switching.connector;

import org.springframework.stereotype.Component;

import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.BankIsoDispatchResponse;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.example.switching.outbox.dto.DispatchTransferCommand;

/**
 * Generic HTTP connector — PLACEHOLDER (not yet implemented).
 * <p>
 * This stub is registered so the {@link com.example.switching.connector.registry.ConnectorRegistry}
 * can resolve it for connector_type = HTTP without throwing at startup.
 * <p>
 * Future implementation should:
 * <ul>
 *   <li>POST the encrypted PACS.008 payload to {@code connector_configs.endpoint_url}</li>
 *   <li>Respect {@code connector_configs.timeout_ms}</li>
 *   <li>Parse the PACS.002 XML response from the HTTP body</li>
 *   <li>Handle network errors and timeouts as retryable failures</li>
 * </ul>
 *
 * TODO: Implement HTTP dispatch in a future phase.
 */
@Component
public class GenericHttpConnector implements BankConnector {

    @Override
    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        throw new UnsupportedOperationException(
                "HTTP connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.getConnectorName() : "null"));
    }

    @Override
    public BankDispatchResult dispatchIsoMessage(DispatchIsoMessageCommand command) {
        throw new UnsupportedOperationException(
                "HTTP connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.connectorName() : "null"));
    }

    @Override
    public BankIsoDispatchResponse dispatchIsoMessageWithPacs002(DispatchIsoMessageCommand command) {
        throw new UnsupportedOperationException(
                "HTTP connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.connectorName() : "null"));
    }
}
