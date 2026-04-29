package com.example.switching.connector;

import org.springframework.stereotype.Component;

import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.BankIsoDispatchResponse;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.example.switching.outbox.dto.DispatchTransferCommand;

/**
 * Generic MQ connector — PLACEHOLDER (not yet implemented).
 * <p>
 * This stub is registered so the {@link com.example.switching.connector.registry.ConnectorRegistry}
 * can resolve it for connector_type = MQ without throwing at startup.
 * <p>
 * Future implementation should:
 * <ul>
 *   <li>Publish the encrypted PACS.008 payload to a message queue (RabbitMQ, Kafka, IBM MQ, etc.)</li>
 *   <li>Correlate and consume the PACS.002 response from the reply queue</li>
 *   <li>Respect {@code connector_configs.timeout_ms} for reply correlation</li>
 *   <li>Handle MQ connection errors as retryable technical failures</li>
 * </ul>
 *
 * TODO: Implement MQ dispatch in a future phase.
 */
@Component
public class GenericMqConnector implements BankConnector {

    @Override
    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        throw new UnsupportedOperationException(
                "MQ connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.getConnectorName() : "null"));
    }

    @Override
    public BankDispatchResult dispatchIsoMessage(DispatchIsoMessageCommand command) {
        throw new UnsupportedOperationException(
                "MQ connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.connectorName() : "null"));
    }

    @Override
    public BankIsoDispatchResponse dispatchIsoMessageWithPacs002(DispatchIsoMessageCommand command) {
        throw new UnsupportedOperationException(
                "MQ connector is not yet implemented. "
                        + "connectorName=" + (command != null ? command.connectorName() : "null"));
    }
}
