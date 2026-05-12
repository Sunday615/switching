package com.example.switching.iso.inquiry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IsoInquiryQueryService {

    private final JdbcTemplate jdbcTemplate;

    public IsoInquiryQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IsoInquiryQueryResponse findByInquiryRef(String inquiryRef) {
        if (!StringUtils.hasText(inquiryRef)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inquiryRef is required");
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

        if (response == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "ISO inquiry not found: " + inquiryRef
            );
        }

        return response;
    }

    private IsoInquiryQueryResponse mapRow(ResultSet rs) throws SQLException {
        IsoInquiryQueryResponse response = new IsoInquiryQueryResponse();

        response.setId(rs.getLong("id"));
        response.setInquiryRef(rs.getString("inquiry_ref"));
        response.setChannelId(rs.getString("channel_id"));
        response.setMessageId(rs.getString("message_id"));
        response.setInstructionId(rs.getString("instruction_id"));
        response.setEndToEndId(rs.getString("end_to_end_id"));

        response.setSourceBank(rs.getString("source_bank_code"));
        response.setDestinationBank(rs.getString("destination_bank_code"));

        response.setDebtorAccount(rs.getString("debtor_account_no"));
        response.setCreditorAccount(rs.getString("creditor_account_no"));

        response.setAmount(rs.getBigDecimal("amount"));
        response.setCurrency(rs.getString("currency"));
        response.setReference(rs.getString("reference"));

        response.setStatus(rs.getString("status"));
        response.setAccountFound(rs.getBoolean("account_found"));
        response.setBankAvailable(rs.getBoolean("bank_available"));
        response.setEligibleForTransfer(rs.getBoolean("eligible_for_transfer"));

        response.setFailureCode(rs.getString("failure_code"));
        response.setFailureMessage(rs.getString("failure_message"));

        response.setExpiresAt(toLocalDateTime(rs.getTimestamp("expires_at")));
        response.setUsedByTransferRef(rs.getString("used_by_transfer_ref"));

        response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        response.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));

        return response;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }
}