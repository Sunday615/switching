package com.example.switching.iso.inbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class IsoPacs008InboundService {

    private final Pacs008InboundParser parser;
    private final Pacs002XmlResponseBuilder responseBuilder;

    public IsoPacs008InboundService(
            Pacs008InboundParser parser,
            Pacs002XmlResponseBuilder responseBuilder
    ) {
        this.parser = parser;
        this.responseBuilder = responseBuilder;
    }

    public String handleInboundPacs008(String xmlBody, String bankCodeHeader) {
        if (isBlank(xmlBody)) {
            return responseBuilder.rejectedWithoutOriginalMessage(
                    "FF01",
                    "PACS.008 XML body is required"
            );
        }

        Pacs008InboundRequest request;

        try {
            request = parser.parse(xmlBody);
        } catch (Exception e) {
            return responseBuilder.rejectedWithoutOriginalMessage(
                    "FF01",
                    "Invalid PACS.008 XML"
            );
        }

        List<String> errors = validate(request, bankCodeHeader);

        if (!errors.isEmpty()) {
            return responseBuilder.rejected(
                    request,
                    "FF01",
                    String.join("; ", errors)
            );
        }

        return responseBuilder.accepted(request);
    }

    private List<String> validate(Pacs008InboundRequest request, String bankCodeHeader) {
        List<String> errors = new ArrayList<>();

        require(errors, request.getMessageId(), "GrpHdr/MsgId is required");
        require(errors, request.getCreationDateTime(), "GrpHdr/CreDtTm is required");
        require(errors, request.getNumberOfTransactions(), "GrpHdr/NbOfTxs is required");

        if (!"1".equals(request.getNumberOfTransactions())) {
            errors.add("Only single-transaction PACS.008 is supported in ISO-IN-1A");
        }

        require(errors, request.getInstructionId(), "PmtId/InstrId is required");
        require(errors, request.getEndToEndId(), "PmtId/EndToEndId is required");

        if (request.getAmount() == null) {
            errors.add("IntrBkSttlmAmt is required and must be numeric");
        } else if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("IntrBkSttlmAmt must be greater than zero");
        }

        require(errors, request.getCurrency(), "IntrBkSttlmAmt/@Ccy is required");
        require(errors, request.getDebtorAgentBic(), "DbtrAgt/FinInstnId/BICFI is required");
        require(errors, request.getCreditorAgentBic(), "CdtrAgt/FinInstnId/BICFI is required");
        require(errors, request.getDebtorAccount(), "DbtrAcct/Id/Othr/Id is required");
        require(errors, request.getCreditorAccount(), "CdtrAcct/Id/Othr/Id is required");

        if (isBlank(bankCodeHeader)) {
            errors.add("X-Bank-Code header is required");
        } else if (!isBlank(request.getDebtorAgentBic())
                && !bankCodeHeader.trim().equalsIgnoreCase(request.getDebtorAgentBic().trim())) {
            errors.add("X-Bank-Code must match DbtrAgt/FinInstnId/BICFI");
        }

        return errors;
    }

    private void require(List<String> errors, String value, String message) {
        if (isBlank(value)) {
            errors.add(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}