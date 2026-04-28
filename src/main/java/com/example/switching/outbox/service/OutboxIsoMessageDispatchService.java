package com.example.switching.outbox.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.switching.connector.BankConnector;
import com.example.switching.iso.dto.Pacs002ParseResult;
import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoSecurityStatus;
import com.example.switching.iso.exception.IsoMessageInvalidStateException;
import com.example.switching.iso.exception.IsoMessageNotFoundException;
import com.example.switching.iso.parser.Pacs002Parser;
import com.example.switching.iso.repository.IsoMessageRepository;
import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.BankIsoDispatchResponse;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxIsoMessageDispatchService {

    private final ObjectMapper objectMapper;
    private final IsoMessageRepository isoMessageRepository;
    private final BankConnector bankConnector;
    private final Pacs002Parser pacs002Parser;

    public OutboxIsoMessageDispatchService(
            ObjectMapper objectMapper,
            IsoMessageRepository isoMessageRepository,
            BankConnector bankConnector,
            Pacs002Parser pacs002Parser) {
        this.objectMapper = objectMapper;
        this.isoMessageRepository = isoMessageRepository;
        this.bankConnector = bankConnector;
        this.pacs002Parser = pacs002Parser;
    }

    public BankDispatchResult dispatchEncryptedIsoMessage(String outboxPayload) {
        try {
            JsonNode payload = objectMapper.readTree(outboxPayload);

            String transferRef = requiredText(payload, "transferRef");
            Long isoMessageId = requiredLong(payload, "isoMessageId");
            String sourceBank = optionalText(payload, "sourceBank");
            String destinationBank = requiredText(payload, "destinationBank");

            IsoMessageEntity isoMessage = isoMessageRepository.findById(isoMessageId)
                    .orElseThrow(() -> new IsoMessageNotFoundException(String.valueOf(isoMessageId)));

            validateIsoMessage(isoMessage, transferRef);

            DispatchIsoMessageCommand command = new DispatchIsoMessageCommand(
                    transferRef,
                    isoMessage.getId(),
                    isoMessage.getMessageId(),
                    isoMessage.getEndToEndId(),
                    String.valueOf(isoMessage.getMessageType()),
                    sourceBank,
                    destinationBank,
                    isoMessage.getEncryptedPayload());

            BankIsoDispatchResponse bankResponse = bankConnector.dispatchIsoMessageWithPacs002(command);

            if (!bankResponse.success()) {
                return new BankDispatchResult(
                        false,
                        bankResponse.responseCode(),
                        bankResponse.responseMessage(),
                        bankResponse.externalReference(),
                        bankResponse.isoStatusCode());
            }

            if (!StringUtils.hasText(bankResponse.pacs002Xml())) {
                return new BankDispatchResult(
                        false,
                        "PACS002-001",
                        "Bank response success but PACS.002 XML is empty",
                        bankResponse.externalReference(),
                        bankResponse.isoStatusCode());
            }

            Pacs002ParseResult pacs002 = pacs002Parser.parse(bankResponse.pacs002Xml());

            if (pacs002.accepted()) {
                return new BankDispatchResult(
                        true,
                        bankResponse.externalReference(),
                        "PACS.002 accepted with TxSts=" + pacs002.transactionStatus(),
                        null,
                        null);
            }

            if (pacs002.rejected()) {
                return new BankDispatchResult(
                        false,
                        "PACS002-RJCT",
                        "PACS.002 rejected. reasonCode="
                                + pacs002.reasonCode()
                                + ", reasonMessage="
                                + pacs002.reasonMessage(),
                        bankResponse.externalReference(),
                        "PACS002_" + pacs002.transactionStatus());
            }

            return new BankDispatchResult(
                    false,
                    "PACS002-UNKNOWN",
                    "Unsupported PACS.002 TxSts=" + pacs002.transactionStatus(),
                    bankResponse.externalReference(),
                    "PACS002_" + pacs002.transactionStatus());

        } catch (IsoMessageNotFoundException | IsoMessageInvalidStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to dispatch encrypted ISO message from outbox payload", ex);
        }
    }

    private void validateIsoMessage(IsoMessageEntity isoMessage, String transferRef) {
        if (!StringUtils.hasText(isoMessage.getTransferRef())) {
            throw new IsoMessageInvalidStateException(
                    "ISO message transferRef is empty. isoMessageId=" + isoMessage.getId());
        }

        if (!isoMessage.getTransferRef().equals(transferRef)) {
            throw new IsoMessageInvalidStateException(
                    "ISO message transferRef does not match outbox transferRef. isoMessageId="
                            + isoMessage.getId()
                            + ", isoTransferRef="
                            + isoMessage.getTransferRef()
                            + ", outboxTransferRef="
                            + transferRef);
        }

        if (isoMessage.getSecurityStatus() != IsoSecurityStatus.ENCRYPTED) {
            throw new IsoMessageInvalidStateException(
                    "ISO message must be ENCRYPTED before dispatch. isoMessageId="
                            + isoMessage.getId()
                            + ", securityStatus="
                            + isoMessage.getSecurityStatus());
        }

        if (!StringUtils.hasText(isoMessage.getEncryptedPayload())) {
            throw new IsoMessageInvalidStateException(
                    "ISO encryptedPayload is empty. isoMessageId=" + isoMessage.getId());
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || !StringUtils.hasText(value.asText())) {
            throw new IllegalArgumentException("Missing required field in outbox payload: " + fieldName);
        }

        return value.asText();
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private Long requiredLong(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required field in outbox payload: " + fieldName);
        }

        if (!value.canConvertToLong()) {
            throw new IllegalArgumentException("Invalid long field in outbox payload: " + fieldName);
        }

        return value.asLong();
    }
}