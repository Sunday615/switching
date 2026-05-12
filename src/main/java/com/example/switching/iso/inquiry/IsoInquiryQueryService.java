package com.example.switching.iso.inquiry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IsoInquiryQueryService {

    private final JdbcTemplate jdbcTemplate;

    public IsoInquiryQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<IsoInquiryQueryResponse> findByInquiryRef(String inquiryRef) {
        if (!StringUtils.hasText(inquiryRef)) {
            return Optional.empty();
        }

        IsoInquiryQueryResponse response = jdbcTemplate.query(
                """
                SELECT id,
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
                FROM iso_inquiries
                WHERE inquiry_ref = ?
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    return mapRow(rs);
                },
                inquiryRef.trim()
        );

        return Optional.ofNullable(response);
    }

    private IsoInquiryQueryResponse mapRow(ResultSet rs) throws SQLException {
        IsoInquiryQueryResponse response = new IsoInquiryQueryResponse();

        response.setId(rs.getLong("id"));
        response.setInquiryRef(clean(rs.getString("inquiry_ref")));
        response.setChannelId(clean(rs.getString("channel_id")));
        response.setMessageId(clean(rs.getString("message_id")));
        response.setInstructionId(clean(rs.getString("instruction_id")));
        response.setEndToEndId(clean(rs.getString("end_to_end_id")));

        response.setSourceBank(clean(rs.getString("source_bank_code")));
        response.setDestinationBank(clean(rs.getString("destination_bank_code")));

        /*
         * Current local ACMT.023 profile verifies the destination / creditor account.
         * Do not expose the verified ACMT.023 account as debtorAccount.
         *
         * This also hides older dirty rows where debtor_account_no may contain raw XML whitespace.
         */
        response.setDebtorAccount(null);
        response.setCreditorAccount(clean(rs.getString("creditor_account_no")));

        response.setAmount(rs.getBigDecimal("amount"));
        response.setCurrency(clean(rs.getString("currency")));
        response.setReference(clean(rs.getString("reference")));

        response.setStatus(clean(rs.getString("status")));
        response.setAccountFound(rs.getBoolean("account_found"));
        response.setBankAvailable(rs.getBoolean("bank_available"));
        response.setEligibleForTransfer(rs.getBoolean("eligible_for_transfer"));

        response.setFailureCode(clean(rs.getString("failure_code")));
        response.setFailureMessage(clean(rs.getString("failure_message")));

        response.setExpiresAt(toLocalDateTime(rs.getTimestamp("expires_at")));
        response.setUsedByTransferRef(clean(rs.getString("used_by_transfer_ref")));

        response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        response.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));

        return response;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }
}
