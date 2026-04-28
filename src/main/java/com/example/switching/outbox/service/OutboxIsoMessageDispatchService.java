package com.example.switching.outbox.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.switching.audit.service.AuditLogService;
import com.example.switching.connector.BankConnector;
import com.example.switching.iso.dto.Pacs002ParseResult;
import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoSecurityStatus;
import com.example.switching.iso.exception.IsoMessageInvalidStateException;
import com.example.switching.iso.exception.IsoMessageNotFoundException;
import com.example.switching.iso.parser.Pacs002Parser;
import com.example.switching.iso.repository.IsoMessageRepository;
import com.example.switching.iso.service.InboundPacs002MessageService;
import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.BankIsoDispatchResponse;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxIsoMessageDispatchService {

    private static final String ENTITY_TYPE = "TRANSFER";
    private static final String SOURCE_SYSTEM = "WORKER";

    private final ObjectMapper objectMapper;
    private final IsoMessageRepository isoMessageRepository;
    private final BankConnector bankConnector;
    private final Pacs002Parser pacs002Parser;
    private final InboundPacs002MessageService inboundPacs002MessageService;
    private final AuditLogService auditLogService;

    public OutboxIsoMessageDispatchService(
            ObjectMapper objectMapper,
            IsoMessageRepository isoMessageRepository,
            BankConnector bankConnector,
            Pacs002Parser pacs002Parser,
            InboundPacs002MessageService inboundPacs002MessageService,
            AuditLogService auditLogService
    ) {
        this.objectMapper = objectMapper;
        this.isoMessageRepository = isoMessageRepository;
        this.bankConnector = bankConnector;
        this.pacs002Parser = pacs002Parser;
        this.inboundPacs002MessageService = inboundPacs002MessageService;
        this.auditLogService = auditLogService;
    }

    public BankDispatchResult dispatchEncryptedIsoMessage(String outboxPayload) {
        try {
            JsonNode payload = objectMapper.readTree(outboxPayload);

            String transferRef = requiredText(payload, "transferRef");
            Long isoMessageId = requiredLong(payload, "isoMessageId");
            String sourceBank = optionalText(payload, "sourceBank");
            String destinationBank = requiredText(payload, "destinationBank");

            IsoMessageEntity outboundPacs008 = isoMessageRepository.findById(isoMessageId)
                    .orElseThrow(() -> new IsoMessageNotFoundException(String.valueOf(isoMessageId)));

            validateOutboundPacs008(outboundPacs008, transferRef);

            logIsoDispatchStarted(
                    transferRef,
                    outboundPacs008,
                    destinationBank
            );

            DispatchIsoMessageCommand command = new DispatchIsoMessageCommand(
                    transferRef,
                    outboundPacs008.getId(),
                    outboundPacs008.getMessageId(),
                    outboundPacs008.getEndToEndId(),
                    String.valueOf(outboundPacs008.getMessageType()),
                    sourceBank,
                    destinationBank,
                    outboundPacs008.getEncryptedPayload()
            );

            BankIsoDispatchResponse bankResponse =
                    bankConnector.dispatchIsoMessageWithPacs002(command);

            if (bankResponse == null) {
                return new BankDispatchResult(
                        false,
                        null,
                        null,
                        "PACS002-NULL",
                        "BankConnector returned null PACS.002 response"
                );
            }

            logPacs002ResponseReceived(
                    transferRef,
                    outboundPacs008,
                    bankResponse
            );

      
            if (StringUtils.hasText(bankResponse.pacs002Xml())) {
                Pacs002ParseResult pacs002 =
                        pacs002Parser.parse(bankResponse.pacs002Xml());

                IsoMessageEntity inboundPacs002 =
                        inboundPacs002MessageService.saveInboundPacs002(
                                outboundPacs008,
                                pacs002,
                                bankResponse.pacs002Xml()
                        );

                logPacs002InboundSaved(
                        transferRef,
                        inboundPacs002,
                        pacs002,
                        bankResponse.externalReference()
                );

                logPacs002Parsed(
                        transferRef,
                        inboundPacs002,
                        pacs002
                );

                if (pacs002.accepted()) {
                    return new BankDispatchResult(
                            true,
                            bankResponse.externalReference(),
                            "PACS.002 accepted with TxSts=" + pacs002.transactionStatus(),
                            null,
                            null
                    );
                }

                if (pacs002.rejected()) {
                    return new BankDispatchResult(
                            false,
                            bankResponse.externalReference(),
                            null,
                            "PACS002-RJCT",
                            "PACS.002 rejected. reasonCode="
                                    + pacs002.reasonCode()
                                    + ", reasonMessage="
                                    + pacs002.reasonMessage()
                    );
                }

                return new BankDispatchResult(
                        false,
                        bankResponse.externalReference(),
                        null,
                        "PACS002-UNKNOWN",
                        "Unsupported PACS.002 TxSts=" + pacs002.transactionStatus()
                );
            }

            /*
             * ถ้าไม่มี PACS.002 XML กลับมา
             * - ถ้า bankResponse.success = true ถือว่า response ไม่สมบูรณ์
             * - ถ้า bankResponse.success = false ถือว่าเป็น downstream failure ปกติ
             */
            if (bankResponse.success()) {
                return new BankDispatchResult(
                        false,
                        bankResponse.externalReference(),
                        null,
                        "PACS002-001",
                        "Bank response success but PACS.002 XML is empty"
                );
            }

            return new BankDispatchResult(
                    false,
                    bankResponse.externalReference(),
                    null,
                    bankResponse.responseCode(),
                    bankResponse.responseMessage()
            );

        } catch (IsoMessageNotFoundException | IsoMessageInvalidStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to dispatch encrypted ISO message from outbox payload",
                    ex
            );
        }
    }

    private void validateOutboundPacs008(IsoMessageEntity isoMessage, String transferRef) {
        if (!StringUtils.hasText(isoMessage.getTransferRef())) {
            throw new IsoMessageInvalidStateException(
                    "ISO message transferRef is empty. isoMessageId=" + isoMessage.getId()
            );
        }

        if (!isoMessage.getTransferRef().equals(transferRef)) {
            throw new IsoMessageInvalidStateException(
                    "ISO message transferRef does not match outbox transferRef. isoMessageId="
                            + isoMessage.getId()
                            + ", isoTransferRef="
                            + isoMessage.getTransferRef()
                            + ", outboxTransferRef="
                            + transferRef
            );
        }

        if (isoMessage.getSecurityStatus() != IsoSecurityStatus.ENCRYPTED) {
            throw new IsoMessageInvalidStateException(
                    "ISO message must be ENCRYPTED before dispatch. isoMessageId="
                            + isoMessage.getId()
                            + ", securityStatus="
                            + isoMessage.getSecurityStatus()
            );
        }

        if (!StringUtils.hasText(isoMessage.getEncryptedPayload())) {
            throw new IsoMessageInvalidStateException(
                    "ISO encryptedPayload is empty. isoMessageId=" + isoMessage.getId()
            );
        }
    }

    private void logIsoDispatchStarted(
            String transferRef,
            IsoMessageEntity outboundPacs008,
            String destinationBank
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transferRef);
        payload.put("outboundIsoMessageId", outboundPacs008.getId());
        payload.put("messageType", String.valueOf(outboundPacs008.getMessageType()));
        payload.put("direction", String.valueOf(outboundPacs008.getDirection()));
        payload.put("messageId", outboundPacs008.getMessageId());
        payload.put("endToEndId", outboundPacs008.getEndToEndId());
        payload.put("securityStatus", String.valueOf(outboundPacs008.getSecurityStatus()));
        payload.put("destinationBank", destinationBank);
        payload.put("encryptedPayloadPresent", StringUtils.hasText(outboundPacs008.getEncryptedPayload()));

        auditLogService.log(
                "OUTBOX_ISO_DISPATCH_STARTED",
                ENTITY_TYPE,
                transferRef,
                SOURCE_SYSTEM,
                payload
        );
    }

    private void logPacs002ResponseReceived(
            String transferRef,
            IsoMessageEntity outboundPacs008,
            BankIsoDispatchResponse bankResponse
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transferRef);
        payload.put("outboundIsoMessageId", outboundPacs008.getId());
        payload.put("outboundMessageId", outboundPacs008.getMessageId());
        payload.put("responseSuccess", bankResponse.success());
        payload.put("responseCode", bankResponse.responseCode());
        payload.put("responseMessage", bankResponse.responseMessage());
        payload.put("externalReference", bankResponse.externalReference());
        payload.put("isoStatusCode", bankResponse.isoStatusCode());
        payload.put("pacs002XmlPresent", StringUtils.hasText(bankResponse.pacs002Xml()));

        auditLogService.log(
                "PACS002_RESPONSE_RECEIVED",
                ENTITY_TYPE,
                transferRef,
                SOURCE_SYSTEM,
                payload
        );
    }

    private void logPacs002InboundSaved(
            String transferRef,
            IsoMessageEntity inboundPacs002,
            Pacs002ParseResult pacs002,
            String externalReference
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transferRef);
        payload.put("inboundIsoMessageId", inboundPacs002.getId());
        payload.put("messageType", String.valueOf(inboundPacs002.getMessageType()));
        payload.put("direction", String.valueOf(inboundPacs002.getDirection()));
        payload.put("messageId", inboundPacs002.getMessageId());
        payload.put("endToEndId", inboundPacs002.getEndToEndId());
        payload.put("securityStatus", String.valueOf(inboundPacs002.getSecurityStatus()));
        payload.put("txStatus", pacs002.transactionStatus());
        payload.put("originalMessageId", pacs002.originalMessageId());
        payload.put("originalEndToEndId", pacs002.originalEndToEndId());
        payload.put("originalTransactionId", pacs002.originalTransactionId());
        payload.put("externalReference", externalReference);

        auditLogService.log(
                "PACS002_INBOUND_SAVED",
                ENTITY_TYPE,
                transferRef,
                SOURCE_SYSTEM,
                payload
        );
    }

    private void logPacs002Parsed(
            String transferRef,
            IsoMessageEntity inboundPacs002,
            Pacs002ParseResult pacs002
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferRef", transferRef);
        payload.put("inboundIsoMessageId", inboundPacs002.getId());
        payload.put("messageId", pacs002.messageId());
        payload.put("originalMessageId", pacs002.originalMessageId());
        payload.put("originalEndToEndId", pacs002.originalEndToEndId());
        payload.put("originalTransactionId", pacs002.originalTransactionId());
        payload.put("txStatus", pacs002.transactionStatus());
        payload.put("accepted", pacs002.accepted());
        payload.put("rejected", pacs002.rejected());
        payload.put("reasonCode", pacs002.reasonCode());
        payload.put("reasonMessage", pacs002.reasonMessage());

        auditLogService.log(
                "PACS002_PARSED",
                ENTITY_TYPE,
                transferRef,
                SOURCE_SYSTEM,
                payload
        );
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

        String text = value.asText();

        if (!StringUtils.hasText(text)) {
            return null;
        }

        return text;
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