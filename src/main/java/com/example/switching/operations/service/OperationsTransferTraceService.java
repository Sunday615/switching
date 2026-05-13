package com.example.switching.operations.service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.switching.operations.dto.OperationsTransferTraceAuditItemResponse;
import com.example.switching.operations.dto.OperationsTransferTraceInquiryResponse;
import com.example.switching.operations.dto.OperationsTransferTraceIsoMessageItemResponse;
import com.example.switching.operations.dto.OperationsTransferTraceOutboxItemResponse;
import com.example.switching.operations.dto.OperationsTransferTraceResponse;
import com.example.switching.operations.dto.OperationsTransferTraceSummaryResponse;
import com.example.switching.operations.dto.OperationsTransferTraceTimelineItemResponse;
import com.example.switching.operations.dto.OperationsTransferTraceTransferResponse;

@Service
public class OperationsTransferTraceService {

    private final JdbcTemplate jdbcTemplate;

    public OperationsTransferTraceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<OperationsTransferTraceResponse> findTraceByTransferRef(String transferRef) {
        if (!StringUtils.hasText(transferRef)) {
            return Optional.empty();
        }

        String normalizedTransferRef = transferRef.trim();

        OperationsTransferTraceTransferResponse transfer = findTransfer(normalizedTransferRef);
        if (transfer == null) {
            return Optional.empty();
        }

        List<String> warnings = new ArrayList<>();

        OperationsTransferTraceInquiryResponse inquiry = null;
        try {
            inquiry = findInquiry(transfer.inquiryRef(), normalizedTransferRef);
        } catch (Exception ex) {
            warnings.add("INQUIRY_TRACE_UNAVAILABLE: " + ex.getClass().getSimpleName());
        }

        String inquiryRef = inquiry == null ? transfer.inquiryRef() : inquiry.inquiryRef();

        List<OperationsTransferTraceOutboxItemResponse> outboxEvents = List.of();
        try {
            outboxEvents = findOutboxEvents(normalizedTransferRef);
        } catch (Exception ex) {
            warnings.add("OUTBOX_TRACE_UNAVAILABLE: " + ex.getClass().getSimpleName());
        }

        List<OperationsTransferTraceIsoMessageItemResponse> isoMessages = List.of();
        try {
            isoMessages = findIsoMessages(normalizedTransferRef, inquiryRef);
        } catch (Exception ex) {
            warnings.add("ISO_MESSAGE_TRACE_UNAVAILABLE: " + ex.getClass().getSimpleName());
        }

        List<OperationsTransferTraceAuditItemResponse> auditEvents = List.of();
        try {
            auditEvents = findAuditEvents(normalizedTransferRef, inquiryRef);
        } catch (Exception ex) {
            warnings.add("AUDIT_TRACE_UNAVAILABLE: " + ex.getClass().getSimpleName());
        }

        List<OperationsTransferTraceTimelineItemResponse> timeline = buildTimeline(
                transfer,
                inquiry,
                outboxEvents,
                isoMessages,
                auditEvents
        );

        OperationsTransferTraceSummaryResponse summary = new OperationsTransferTraceSummaryResponse(
                outboxEvents.size(),
                isoMessages.size(),
                auditEvents.size(),
                timeline.size(),
                inquiry != null,
                outboxEvents.stream().anyMatch(item -> "FAILED".equalsIgnoreCase(item.status())),
                isoMessages.stream().anyMatch(item ->
                        StringUtils.hasText(item.errorCode())
                                || "INVALID".equalsIgnoreCase(item.validationStatus())
                                || "FAILED".equalsIgnoreCase(item.securityStatus())
                ),
                "SUCCESS".equalsIgnoreCase(transfer.status())
        );

        return Optional.of(new OperationsTransferTraceResponse(
                warnings.isEmpty() ? "TRACE_FOUND" : "TRACE_FOUND_WITH_WARNINGS",
                LocalDateTime.now(),
                normalizedTransferRef,
                inquiryRef,
                transfer.currentStatus(),
                warnings,
                summary,
                transfer,
                inquiry,
                outboxEvents,
                isoMessages,
                auditEvents,
                timeline
        ));
    }

    private OperationsTransferTraceTransferResponse findTransfer(String transferRef) {
        return jdbcTemplate.query(
                """
                SELECT
                    t.id,
                    t.transfer_ref,
                    t.client_transfer_id,
                    t.inquiry_ref,
                    t.source_bank_code,
                    t.source_account_no,
                    t.destination_bank_code,
                    t.destination_account_no,
                    t.destination_account_name,
                    t.amount,
                    t.currency,
                    t.status,
                    t.channel_id,
                    t.route_code,
                    t.connector_name,
                    t.external_reference,
                    t.reference,
                    t.error_code,
                    t.error_message,
                    t.created_at,
                    t.updated_at
                FROM transfers t
                WHERE t.transfer_ref = ?
                LIMIT 1
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    return mapTransfer(rs);
                },
                transferRef
        );
    }

    private OperationsTransferTraceTransferResponse mapTransfer(ResultSet rs)
            throws java.sql.SQLException {
        String transferRef = clean(rs.getString("transfer_ref"));
        String inquiryRef = clean(rs.getString("inquiry_ref"));
        String status = clean(rs.getString("status"));

        return new OperationsTransferTraceTransferResponse(
                rs.getLong("id"),
                transferRef,
                clean(rs.getString("client_transfer_id")),
                inquiryRef,
                clean(rs.getString("source_bank_code")),
                clean(rs.getString("source_account_no")),
                clean(rs.getString("destination_bank_code")),
                clean(rs.getString("destination_account_no")),
                clean(rs.getString("destination_account_name")),
                rs.getBigDecimal("amount"),
                clean(rs.getString("currency")),
                status,
                status,
                clean(rs.getString("channel_id")),
                clean(rs.getString("route_code")),
                clean(rs.getString("connector_name")),
                clean(rs.getString("external_reference")),
                clean(rs.getString("reference")),
                clean(rs.getString("error_code")),
                clean(rs.getString("error_message")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                "/api/transfers/" + transferRef,
                "/api/operations/transfers/" + transferRef,
                "/api/operations/transfers/" + transferRef + "/trace",
                StringUtils.hasText(inquiryRef) ? "/api/iso-inquiries/" + inquiryRef : null
        );
    }

    private OperationsTransferTraceInquiryResponse findInquiry(
            String inquiryRef,
            String transferRef
    ) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(inquiryRef)) {
            conditions.add("q.inquiry_ref = ?");
            params.add(inquiryRef.trim());
        }

        if (StringUtils.hasText(transferRef)) {
            conditions.add("q.used_by_transfer_ref = ?");
            params.add(transferRef.trim());
        }

        if (conditions.isEmpty()) {
            return null;
        }

        String sql = """
                SELECT
                    q.id,
                    q.inquiry_ref,
                    q.channel_id,
                    q.message_id,
                    q.instruction_id,
                    q.end_to_end_id,
                    q.source_bank_code,
                    q.destination_bank_code,
                    q.debtor_account_no,
                    q.creditor_account_no,
                    q.amount,
                    q.currency,
                    q.reference,
                    q.status,
                    q.account_found,
                    q.bank_available,
                    q.eligible_for_transfer,
                    q.failure_code,
                    q.failure_message,
                    q.expires_at,
                    q.used_by_transfer_ref,
                    q.created_at,
                    q.updated_at
                FROM iso_inquiries q
                WHERE """
                + String.join(" OR ", conditions)
                + """

                ORDER BY q.created_at DESC, q.id DESC
                LIMIT 1
                """;

        return jdbcTemplate.query(
                sql,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    return mapInquiry(rs);
                },
                params.toArray()
        );
    }

    private OperationsTransferTraceInquiryResponse mapInquiry(ResultSet rs)
            throws java.sql.SQLException {
        String inquiryRef = clean(rs.getString("inquiry_ref"));
        String usedByTransferRef = clean(rs.getString("used_by_transfer_ref"));
        LocalDateTime expiresAt = toLocalDateTime(rs.getTimestamp("expires_at"));
        boolean expired = expiresAt != null && expiresAt.isBefore(LocalDateTime.now());

        return new OperationsTransferTraceInquiryResponse(
                rs.getLong("id"),
                inquiryRef,
                clean(rs.getString("channel_id")),
                clean(rs.getString("message_id")),
                clean(rs.getString("instruction_id")),
                clean(rs.getString("end_to_end_id")),
                clean(rs.getString("source_bank_code")),
                clean(rs.getString("destination_bank_code")),
                clean(rs.getString("debtor_account_no")),
                clean(rs.getString("creditor_account_no")),
                rs.getBigDecimal("amount"),
                clean(rs.getString("currency")),
                clean(rs.getString("reference")),
                clean(rs.getString("status")),
                rs.getBoolean("account_found"),
                rs.getBoolean("bank_available"),
                rs.getBoolean("eligible_for_transfer"),
                clean(rs.getString("failure_code")),
                clean(rs.getString("failure_message")),
                expiresAt,
                expired,
                usedByTransferRef,
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                "/api/iso-inquiries/" + inquiryRef,
                "/api/operations/iso-inquiries/" + inquiryRef
        );
    }

    private List<OperationsTransferTraceOutboxItemResponse> findOutboxEvents(
            String transferRef
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    transfer_ref,
                    message_type,
                    status,
                    retry_count,
                    last_error,
                    created_at,
                    updated_at,
                    processed_at,
                    next_retry_at
                FROM outbox_events
                WHERE transfer_ref = ?
                ORDER BY created_at ASC, id ASC
                """,
                (rs, rowNum) -> new OperationsTransferTraceOutboxItemResponse(
                        rs.getLong("id"),
                        clean(rs.getString("transfer_ref")),
                        clean(rs.getString("message_type")),
                        clean(rs.getString("status")),
                        rs.getInt("retry_count"),
                        clean(rs.getString("last_error")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("processed_at")),
                        toLocalDateTime(rs.getTimestamp("next_retry_at"))
                ),
                transferRef
        );
    }

    private List<OperationsTransferTraceIsoMessageItemResponse> findIsoMessages(
            String transferRef,
            String inquiryRef
    ) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(transferRef)) {
            conditions.add("transfer_ref = ?");
            params.add(transferRef.trim());
        }

        if (StringUtils.hasText(inquiryRef)) {
            conditions.add("inquiry_ref = ?");
            params.add(inquiryRef.trim());
        }

        if (conditions.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    id,
                    correlation_ref,
                    inquiry_ref,
                    transfer_ref,
                    end_to_end_id,
                    message_id,
                    message_type,
                    direction,
                    security_status,
                    validation_status,
                    error_code,
                    error_message,
                    created_at
                FROM iso_messages
                WHERE """
                + String.join(" OR ", conditions)
                + """

                ORDER BY created_at ASC, id ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Long id = rs.getLong("id");

                    return new OperationsTransferTraceIsoMessageItemResponse(
                            id,
                            clean(rs.getString("correlation_ref")),
                            clean(rs.getString("inquiry_ref")),
                            clean(rs.getString("transfer_ref")),
                            clean(rs.getString("end_to_end_id")),
                            clean(rs.getString("message_id")),
                            clean(rs.getString("message_type")),
                            clean(rs.getString("direction")),
                            clean(rs.getString("security_status")),
                            clean(rs.getString("validation_status")),
                            clean(rs.getString("error_code")),
                            clean(rs.getString("error_message")),
                            toLocalDateTime(rs.getTimestamp("created_at")),
                            "/api/operations/iso-messages/" + id
                    );
                },
                params.toArray()
        );
    }

    private List<OperationsTransferTraceAuditItemResponse> findAuditEvents(
            String transferRef,
            String inquiryRef
    ) {
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(transferRef)) {
            conditions.add("reference_id = ?");
            params.add(transferRef.trim());

            conditions.add("payload LIKE ?");
            params.add("%" + transferRef.trim() + "%");
        }

        if (StringUtils.hasText(inquiryRef)) {
            conditions.add("reference_id = ?");
            params.add(inquiryRef.trim());

            conditions.add("payload LIKE ?");
            params.add("%" + inquiryRef.trim() + "%");
        }

        if (conditions.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT
                    id,
                    event_type,
                    reference_type,
                    reference_id,
                    actor,
                    payload,
                    created_at
                FROM audit_logs
                WHERE """
                + String.join(" OR ", conditions)
                + """

                ORDER BY created_at ASC, id ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OperationsTransferTraceAuditItemResponse(
                        rs.getLong("id"),
                        clean(rs.getString("event_type")),
                        clean(rs.getString("reference_type")),
                        clean(rs.getString("reference_id")),
                        clean(rs.getString("actor")),
                        clean(rs.getString("payload")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                params.toArray()
        );
    }

    private List<OperationsTransferTraceTimelineItemResponse> buildTimeline(
            OperationsTransferTraceTransferResponse transfer,
            OperationsTransferTraceInquiryResponse inquiry,
            List<OperationsTransferTraceOutboxItemResponse> outboxEvents,
            List<OperationsTransferTraceIsoMessageItemResponse> isoMessages,
            List<OperationsTransferTraceAuditItemResponse> auditEvents
    ) {
        List<OperationsTransferTraceTimelineItemResponse> timeline = new ArrayList<>();

        if (inquiry != null) {
            timeline.add(new OperationsTransferTraceTimelineItemResponse(
                    inquiry.createdAt(),
                    "ISO_INQUIRY",
                    "ISO_INQUIRY_CREATED",
                    inquiry.status(),
                    "ACMT_023",
                    "INBOUND",
                    inquiry.inquiryRef(),
                    "ISO inquiry created",
                    "Inquiry status=" + inquiry.status()
                            + ", creditorAccount=" + inquiry.creditorAccount()
            ));

            if (StringUtils.hasText(inquiry.usedByTransferRef())) {
                timeline.add(new OperationsTransferTraceTimelineItemResponse(
                        inquiry.updatedAt(),
                        "ISO_INQUIRY",
                        "ISO_INQUIRY_USED_BY_TRANSFER",
                        inquiry.status(),
                        "PACS_008",
                        "INBOUND",
                        inquiry.usedByTransferRef(),
                        "Inquiry used by transfer",
                        "usedByTransferRef=" + inquiry.usedByTransferRef()
                ));
            }
        }

        timeline.add(new OperationsTransferTraceTimelineItemResponse(
                transfer.createdAt(),
                "TRANSFER",
                "TRANSFER_CREATED",
                transfer.status(),
                null,
                null,
                transfer.transferRef(),
                "Transfer created",
                "sourceBank=" + transfer.sourceBank()
                        + ", destinationBank=" + transfer.destinationBank()
                        + ", amount=" + transfer.amount()
                        + " " + transfer.currency()
        ));

        if (transfer.updatedAt() != null) {
            timeline.add(new OperationsTransferTraceTimelineItemResponse(
                    transfer.updatedAt(),
                    "TRANSFER",
                    "TRANSFER_UPDATED",
                    transfer.status(),
                    null,
                    null,
                    transfer.transferRef(),
                    "Transfer updated",
                    StringUtils.hasText(transfer.errorCode())
                            ? "errorCode=" + transfer.errorCode()
                                    + ", errorMessage=" + transfer.errorMessage()
                            : "currentStatus=" + transfer.status()
            ));
        }

        for (OperationsTransferTraceOutboxItemResponse outbox : outboxEvents) {
            timeline.add(new OperationsTransferTraceTimelineItemResponse(
                    outbox.createdAt(),
                    "OUTBOX",
                    "OUTBOX_EVENT",
                    outbox.status(),
                    outbox.messageType(),
                    null,
                    outbox.id() == null ? null : String.valueOf(outbox.id()),
                    "Outbox event " + outbox.status(),
                    "retryCount=" + outbox.retryCount()
                            + (StringUtils.hasText(outbox.lastError())
                            ? ", lastError=" + outbox.lastError()
                            : "")
            ));

            if (outbox.processedAt() != null) {
                timeline.add(new OperationsTransferTraceTimelineItemResponse(
                        outbox.processedAt(),
                        "OUTBOX",
                        "OUTBOX_PROCESSED",
                        outbox.status(),
                        outbox.messageType(),
                        null,
                        outbox.id() == null ? null : String.valueOf(outbox.id()),
                        "Outbox processed",
                        "status=" + outbox.status()
                ));
            }
        }

        for (OperationsTransferTraceIsoMessageItemResponse iso : isoMessages) {
            timeline.add(new OperationsTransferTraceTimelineItemResponse(
                    iso.createdAt(),
                    "ISO_MESSAGE",
                    "ISO_MESSAGE_" + nullSafeUpper(iso.direction()),
                    iso.validationStatus(),
                    iso.messageType(),
                    iso.direction(),
                    iso.messageId(),
                    iso.messageType() + " " + iso.direction(),
                    "securityStatus=" + iso.securityStatus()
                            + ", validationStatus=" + iso.validationStatus()
                            + (StringUtils.hasText(iso.errorCode())
                            ? ", errorCode=" + iso.errorCode()
                            : "")
            ));
        }

        for (OperationsTransferTraceAuditItemResponse audit : auditEvents) {
            timeline.add(new OperationsTransferTraceTimelineItemResponse(
                    audit.createdAt(),
                    "AUDIT",
                    audit.eventType(),
                    null,
                    null,
                    null,
                    audit.id() == null ? null : String.valueOf(audit.id()),
                    audit.eventType(),
                    "referenceType=" + audit.referenceType()
                            + ", referenceId=" + audit.referenceId()
                            + ", actor=" + audit.actor()
            ));
        }

        timeline.sort(Comparator.comparing(
                OperationsTransferTraceTimelineItemResponse::timestamp,
                Comparator.nullsLast(LocalDateTime::compareTo)
        ));

        return timeline;
    }

    private String nullSafeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            return "UNKNOWN";
        }

        return value.trim().toUpperCase();
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }
}
