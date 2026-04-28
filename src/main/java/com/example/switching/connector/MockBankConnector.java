package com.example.switching.connector;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.switching.iso.mapper.Pacs002XmlBuilder;
import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.BankIsoDispatchResponse;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.example.switching.outbox.dto.DispatchTransferCommand;

@Component
public class MockBankConnector implements BankConnector {

    private final Pacs002XmlBuilder pacs002XmlBuilder;

    public MockBankConnector(Pacs002XmlBuilder pacs002XmlBuilder) {
        this.pacs002XmlBuilder = pacs002XmlBuilder;
    }

    @Override
    public BankDispatchResult dispatch(DispatchTransferCommand command) {
        return new BankDispatchResult(
                true,
                "00",
                "MOCK_TRANSFER_ACCEPTED",
                null,
                null
        );
    }

    @Override
    public BankDispatchResult dispatchIsoMessage(DispatchIsoMessageCommand command) {
        BankIsoDispatchResponse response = dispatchIsoMessageWithPacs002(command);

        return new BankDispatchResult(
                response.success(),
                response.responseCode(),
                response.responseMessage(),
                response.externalReference(),
                response.isoStatusCode()
        );
    }

    @Override
    public BankIsoDispatchResponse dispatchIsoMessageWithPacs002(DispatchIsoMessageCommand command) {
        if (command == null) {
            return rejectedWithoutPacs002(
                    "BANK-400",
                    "DispatchIsoMessageCommand is null",
                    null
            );
        }

        if (!StringUtils.hasText(command.transferRef())) {
            return rejectedWithoutPacs002(
                    "BANK-400",
                    "transferRef is required",
                    null
            );
        }

        if (command.isoMessageId() == null) {
            return rejectedWithoutPacs002(
                    "BANK-400",
                    "isoMessageId is required",
                    command.transferRef()
            );
        }

        if (!StringUtils.hasText(command.encryptedPayload())) {
            return rejectedWithoutPacs002(
                    "BANK-400",
                    "encryptedPayload is required",
                    command.transferRef()
            );
        }

        if (!"BANK_B".equalsIgnoreCase(command.destinationBank())) {
            String pacs002Xml = pacs002XmlBuilder.buildRejectedResponse(
                    command.messageId(),
                    command.endToEndId(),
                    command.transferRef(),
                    "AG01",
                    "Destination bank not supported by mock connector"
            );

            return new BankIsoDispatchResponse(
                    false,
                    "BANK-404",
                    "Destination bank not supported by mock connector",
                    externalReference(),
                    pacs002Xml,
                    "RJCT"
            );
        }

        String pacs002Xml = pacs002XmlBuilder.buildAcceptedResponse(
                command.messageId(),
                command.endToEndId(),
                command.transferRef()
        );

        return new BankIsoDispatchResponse(
                true,
                "00",
                "MOCK_BANK_B_ACCEPTED_AND_RETURNED_PACS002",
                externalReference(),
                pacs002Xml,
                "ACSC"
        );
    }

    private BankIsoDispatchResponse rejectedWithoutPacs002(
            String responseCode,
            String responseMessage,
            String transferRef
    ) {
        return new BankIsoDispatchResponse(
                false,
                responseCode,
                responseMessage,
                transferRef == null ? null : externalReference(),
                null,
                "RJCT"
        );
    }

    private String externalReference() {
        return "BANK-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}