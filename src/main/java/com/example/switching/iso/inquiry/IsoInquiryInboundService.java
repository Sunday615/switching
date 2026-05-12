package com.example.switching.iso.inquiry;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IsoInquiryInboundService {

    private static final String ISO_CHANNEL_ID = "ISO20022_XML";
    private static final int INQUIRY_TTL_MINUTES = 15;

    private final Acmt023XmlParser parser;
    private final Acmt024XmlResponseBuilder responseBuilder;
    private final JdbcTemplate jdbcTemplate;

    public IsoInquiryInboundService(
            Acmt023XmlParser parser,
            Acmt024XmlResponseBuilder responseBuilder,
            JdbcTemplate jdbcTemplate
    ) {
        this.parser = parser;
        this.responseBuilder = responseBuilder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String handle(String xBankCode, String xmlBody) {
        Acmt023InquiryRequest request;

        try {
            request = parser.parse(xmlBody);
        } catch (Exception e) {
            Acmt023InquiryRequest fallback = new Acmt023InquiryRequest();
            fallback.setMessageId("UNKNOWN");
            return responseBuilder.rejected(fallback, "FF01", e.getMessage());
        }

        if (!StringUtils.hasText(xBankCode) || !xBankCode.trim().equals(request.getSourceBank())) {
            return responseBuilder.rejected(
                    request,
                    "FF01",
                    "X-Bank-Code must match inquiry source bank"
            );
        }

        if (!isParticipantActive(request.getSourceBank())) {
            return rejectAndSave(request, "BANK_INACTIVE", "Source participant is inactive or missing");
        }

        if (!isParticipantActive(request.getDestinationBank())) {
            return rejectAndSave(request, "BANK_INACTIVE", "Destination participant is inactive or missing");
        }

        boolean accountFound = StringUtils.hasText(request.getCreditorAccount());
        boolean bankAvailable = true;
        boolean eligible = accountFound && bankAvailable;

        if (!eligible) {
            return rejectAndSave(request, "AC01", "Creditor account is missing or invalid");
        }

        String existingInquiryRef = findExistingInquiryRef(request.getMessageId());
        if (StringUtils.hasText(existingInquiryRef)) {
            return responseBuilder.accepted(request, existingInquiryRef);
        }

        String inquiryRef = generateInquiryRef();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO iso_inquiries (
                    inquiry_ref,
                    channel_id,
                    message_id,
                    instruction_id,
                    end_to_end_id,
                    source_bank_code,
                    destination_bank_code,
                    debtor_account_no,
                    creditor_account_no,
                    amount,
                    currency,
                    reference,
                    status,
                    account_found,
                    bank_available,
                    eligible_for_transfer,
                    failure_code,
                    failure_message,
                    expires_at,
                    used_by_transfer_ref,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                inquiryRef,
                ISO_CHANNEL_ID,
                request.getMessageId(),
                request.getInstructionId(),
                request.getEndToEndId(),
                request.getSourceBank(),
                request.getDestinationBank(),
                request.getDebtorAccount(),
                request.getCreditorAccount(),
                request.getAmount(),
                request.getCurrency(),
                request.getReference(),
                "ELIGIBLE",
                true,
                true,
                true,
                null,
                null,
                now.plusMinutes(INQUIRY_TTL_MINUTES),
                null,
                now,
                now
        );

        return responseBuilder.accepted(request, inquiryRef);
    }

    private String rejectAndSave(Acmt023InquiryRequest request, String code, String message) {
        String inquiryRef = generateInquiryRef();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                INSERT INTO iso_inquiries (
                    inquiry_ref,
                    channel_id,
                    message_id,
                    instruction_id,
                    end_to_end_id,
                    source_bank_code,
                    destination_bank_code,
                    debtor_account_no,
                    creditor_account_no,
                    amount,
                    currency,
                    reference,
                    status,
                    account_found,
                    bank_available,
                    eligible_for_transfer,
                    failure_code,
                    failure_message,
                    expires_at,
                    used_by_transfer_ref,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                inquiryRef,
                ISO_CHANNEL_ID,
                request.getMessageId(),
                request.getInstructionId(),
                request.getEndToEndId(),
                request.getSourceBank(),
                request.getDestinationBank(),
                request.getDebtorAccount(),
                request.getCreditorAccount(),
                request.getAmount(),
                request.getCurrency(),
                request.getReference(),
                "REJECTED",
                false,
                false,
                false,
                code,
                message,
                now.plusMinutes(INQUIRY_TTL_MINUTES),
                null,
                now,
                now
        );

        return responseBuilder.rejected(request, code, message);
    }

    private boolean isParticipantActive(String bankCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM participants
                WHERE bank_code = ?
                  AND status = 'ACTIVE'
                """,
                Integer.class,
                bankCode
        );

        return count != null && count > 0;
    }

    private String findExistingInquiryRef(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return null;
        }

        return jdbcTemplate.query(
                """
                SELECT inquiry_ref
                FROM iso_inquiries
                WHERE channel_id = ?
                  AND message_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return rs.getString("inquiry_ref");
                },
                ISO_CHANNEL_ID,
                messageId.trim()
        );
    }

    private String generateInquiryRef() {
        return "INQ-" + LocalDateTime.now()
                .toString()
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .substring(0, 14)
                + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}