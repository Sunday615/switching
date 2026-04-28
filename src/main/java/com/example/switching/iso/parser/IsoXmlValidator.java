package com.example.switching.iso.parser;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import com.example.switching.iso.dto.IsoXmlValidationResult;
import com.example.switching.iso.enums.IsoMessageType;

@Component
public class IsoXmlValidator {

    public IsoXmlValidationResult validate(String xml, IsoMessageType expectedMessageType) {
        if (!StringUtils.hasText(xml)) {
            return IsoXmlValidationResult.invalid(
                    "ISO-VAL-001",
                    "ISO XML payload is empty",
                    expectedMessageType == null ? null : expectedMessageType.name()
            );
        }

        try {
            Document document = parseDocument(xml);

            String namespace = document.getDocumentElement() == null
                    ? null
                    : document.getDocumentElement().getNamespaceURI();

            IsoMessageType detectedType = detectMessageType(namespace, document);

            if (detectedType == null) {
                return IsoXmlValidationResult.invalid(
                        "ISO-VAL-002",
                        "Unsupported or unknown ISO 20022 message type",
                        null
                );
            }

            if (expectedMessageType != null && detectedType != expectedMessageType) {
                return IsoXmlValidationResult.invalid(
                        "ISO-VAL-003",
                        "Expected messageType="
                                + expectedMessageType.name()
                                + " but detected="
                                + detectedType.name(),
                        detectedType.name()
                );
            }

            if (detectedType == IsoMessageType.PACS_008) {
                return validatePacs008(document);
            }

            if (detectedType == IsoMessageType.PACS_002) {
                return validatePacs002(document);
            }

            if (detectedType == IsoMessageType.PACS_028) {
                return validatePacs028(document);
            }

            if (detectedType == IsoMessageType.PACS_004) {
                return validatePacs004(document);
            }

            return IsoXmlValidationResult.invalid(
                    "ISO-VAL-004",
                    "Validator not implemented for messageType=" + detectedType.name(),
                    detectedType.name()
            );

        } catch (Exception ex) {
            return IsoXmlValidationResult.invalid(
                    "ISO-VAL-999",
                    "Invalid ISO XML: " + ex.getMessage(),
                    expectedMessageType == null ? null : expectedMessageType.name()
            );
        }
    }

    private IsoXmlValidationResult validatePacs008(Document document) {
        String msgId = text(document, "MsgId");
        String endToEndId = text(document, "EndToEndId");
        String txId = text(document, "TxId");
        String amount = text(document, "IntrBkSttlmAmt");

        if (!StringUtils.hasText(msgId)) {
            return invalidRequired("MsgId", "PACS_008");
        }

        if (!StringUtils.hasText(endToEndId)) {
            return invalidRequired("EndToEndId", "PACS_008");
        }

        if (!StringUtils.hasText(txId)) {
            return invalidRequired("TxId", "PACS_008");
        }

        if (!StringUtils.hasText(amount)) {
            return invalidRequired("IntrBkSttlmAmt", "PACS_008");
        }

        return IsoXmlValidationResult.valid(
                "PACS_008",
                msgId,
                endToEndId,
                null
        );
    }

    private IsoXmlValidationResult validatePacs002(Document document) {
        String msgId = text(document, "MsgId");
        String originalMsgId = text(document, "OrgnlMsgId");
        String originalEndToEndId = text(document, "OrgnlEndToEndId");
        String originalTxId = text(document, "OrgnlTxId");
        String txStatus = text(document, "TxSts");

        if (!StringUtils.hasText(msgId)) {
            return invalidRequired("MsgId", "PACS_002");
        }

        if (!StringUtils.hasText(originalMsgId)) {
            return invalidRequired("OrgnlMsgId", "PACS_002");
        }

        if (!StringUtils.hasText(originalEndToEndId)) {
            return invalidRequired("OrgnlEndToEndId", "PACS_002");
        }

        if (!StringUtils.hasText(originalTxId)) {
            return invalidRequired("OrgnlTxId", "PACS_002");
        }

        if (!StringUtils.hasText(txStatus)) {
            return invalidRequired("TxSts", "PACS_002");
        }

        if (!isSupportedPacsStatus(txStatus)) {
            return IsoXmlValidationResult.invalid(
                    "ISO-VAL-020",
                    "Unsupported PACS.002 TxSts=" + txStatus,
                    "PACS_002"
            );
        }

        return IsoXmlValidationResult.valid(
                "PACS_002",
                msgId,
                originalEndToEndId,
                txStatus
        );
    }

    private IsoXmlValidationResult validatePacs028(Document document) {
        String msgId = text(document, "MsgId");
        String originalMsgId = text(document, "OrgnlMsgId");
        String originalEndToEndId = text(document, "OrgnlEndToEndId");

        if (!StringUtils.hasText(msgId)) {
            return invalidRequired("MsgId", "PACS_028");
        }

        if (!StringUtils.hasText(originalMsgId)) {
            return invalidRequired("OrgnlMsgId", "PACS_028");
        }

        /*
         * PACS.028 ใช้สำหรับ request status ของ payment instruction เดิม
         * ใน demo นี้ validate แบบ basic ก่อน
         */
        return IsoXmlValidationResult.valid(
                "PACS_028",
                msgId,
                originalEndToEndId,
                null
        );
    }

    private IsoXmlValidationResult validatePacs004(Document document) {
        String msgId = text(document, "MsgId");
        String originalMsgId = text(document, "OrgnlMsgId");
        String originalEndToEndId = text(document, "OrgnlEndToEndId");
        String originalTxId = text(document, "OrgnlTxId");

        if (!StringUtils.hasText(msgId)) {
            return invalidRequired("MsgId", "PACS_004");
        }

        if (!StringUtils.hasText(originalMsgId)) {
            return invalidRequired("OrgnlMsgId", "PACS_004");
        }

        if (!StringUtils.hasText(originalEndToEndId) && !StringUtils.hasText(originalTxId)) {
            return IsoXmlValidationResult.invalid(
                    "ISO-VAL-011",
                    "Either OrgnlEndToEndId or OrgnlTxId is required for PACS.004",
                    "PACS_004"
            );
        }

    
        return IsoXmlValidationResult.valid(
                "PACS_004",
                msgId,
                originalEndToEndId,
                null
        );
    }

    private IsoXmlValidationResult invalidRequired(String fieldName, String messageType) {
        return IsoXmlValidationResult.invalid(
                "ISO-VAL-010",
                "Required field is missing: " + fieldName,
                messageType
        );
    }

    private boolean isSupportedPacsStatus(String status) {
        return "ACSC".equalsIgnoreCase(status)
                || "ACCP".equalsIgnoreCase(status)
                || "ACTC".equalsIgnoreCase(status)
                || "RJCT".equalsIgnoreCase(status)
                || "PDNG".equalsIgnoreCase(status);
    }

    private IsoMessageType detectMessageType(String namespace, Document document) {
        if (namespace != null && namespace.contains("pacs.008")) {
            return IsoMessageType.PACS_008;
        }

        if (namespace != null && namespace.contains("pacs.002")) {
            return IsoMessageType.PACS_002;
        }

        if (namespace != null && namespace.contains("pacs.028")) {
            return IsoMessageType.PACS_028;
        }

        if (namespace != null && namespace.contains("pacs.004")) {
            return IsoMessageType.PACS_004;
        }

        if (document.getElementsByTagNameNS("*", "FIToFICstmrCdtTrf").getLength() > 0) {
            return IsoMessageType.PACS_008;
        }

        if (document.getElementsByTagNameNS("*", "FIToFIPmtStsRpt").getLength() > 0) {
            return IsoMessageType.PACS_002;
        }

        if (document.getElementsByTagNameNS("*", "FIToFIPmtStsReq").getLength() > 0) {
            return IsoMessageType.PACS_028;
        }

        if (document.getElementsByTagNameNS("*", "PmtRtr").getLength() > 0) {
            return IsoMessageType.PACS_004;
        }

        return null;
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String text(Document document, String tagName) {
        var nodes = document.getElementsByTagNameNS("*", tagName);

        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }

        var node = nodes.item(0);

        if (node == null || node.getTextContent() == null) {
            return null;
        }

        String value = node.getTextContent().trim();

        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value;
    }
}