package com.example.switching.iso.inbound;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Component
public class Pacs008InboundParser {

    public Pacs008InboundRequest parse(String xml) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();

            Document document = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            document.getDocumentElement().normalize();

            Element groupHeader = firstElement(document.getDocumentElement(), "GrpHdr");
            Element transaction = firstElement(document.getDocumentElement(), "CdtTrfTxInf");

            String messageId = text(firstElement(groupHeader, "MsgId"));
            String creationDateTime = text(firstElement(groupHeader, "CreDtTm"));
            String numberOfTransactions = text(firstElement(groupHeader, "NbOfTxs"));

            Element paymentId = firstElement(transaction, "PmtId");
            String instructionId = text(firstElement(paymentId, "InstrId"));
            String endToEndId = text(firstElement(paymentId, "EndToEndId"));

            Element amountElement = firstElement(transaction, "IntrBkSttlmAmt");
            BigDecimal amount = parseBigDecimal(text(amountElement));
            String currency = amountElement == null ? null : trimToNull(amountElement.getAttribute("Ccy"));

            Element debtorAgent = firstElement(transaction, "DbtrAgt");
            Element creditorAgent = firstElement(transaction, "CdtrAgt");

            String debtorAgentBic = text(firstElement(debtorAgent, "BICFI"));
            String creditorAgentBic = text(firstElement(creditorAgent, "BICFI"));

            Element debtorAccountElement = firstElement(transaction, "DbtrAcct");
            Element creditorAccountElement = firstElement(transaction, "CdtrAcct");

            String debtorAccount = accountId(debtorAccountElement);
            String creditorAccount = accountId(creditorAccountElement);

            Element remittanceInformationElement = firstElement(transaction, "RmtInf");
            String remittanceInformation = text(firstElement(remittanceInformationElement, "Ustrd"));

            return new Pacs008InboundRequest(
                    messageId,
                    creationDateTime,
                    numberOfTransactions,
                    instructionId,
                    endToEndId,
                    amount,
                    currency,
                    debtorAgentBic,
                    creditorAgentBic,
                    debtorAccount,
                    creditorAccount,
                    remittanceInformation
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PACS.008 XML: " + e.getMessage(), e);
        }
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return factory;
    }

    private Element firstElement(Node root, String localName) {
        if (root == null) {
            return null;
        }

        if (root instanceof Element element && localName.equals(element.getLocalName())) {
            return element;
        }

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node child = root.getChildNodes().item(i);
            Element found = firstElement(child, localName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private String accountId(Element accountElement) {
        if (accountElement == null) {
            return null;
        }

        Element other = firstElement(accountElement, "Othr");
        if (other != null) {
            return text(firstElement(other, "Id"));
        }

        return text(firstElement(accountElement, "IBAN"));
    }

    private String text(Element element) {
        if (element == null) {
            return null;
        }
        return trimToNull(element.getTextContent());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}