package com.example.switching.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCatalog {

        REQ_001(
                        HttpStatus.BAD_REQUEST,
                        "BAD_REQUEST",
                        "REQ-001",
                        ErrorCategory.REQUEST,
                        ErrorLayer.API,
                        ErrorPhase.VALIDATE_REQUEST,
                        false,
                        "Request validation failed"),
        REQ_002(
                        HttpStatus.BAD_REQUEST,
                        "BAD_REQUEST",
                        "REQ-002",
                        ErrorCategory.REQUEST,
                        ErrorLayer.API,
                        ErrorPhase.RECEIVE_REQUEST,
                        false,
                        "Malformed JSON request"),
        REQ_003(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "METHOD_NOT_ALLOWED",
                        "REQ-003",
                        ErrorCategory.REQUEST,
                        ErrorLayer.API,
                        ErrorPhase.RECEIVE_REQUEST,
                        false,
                        "HTTP method not allowed"),

        INQ_001(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "INQ-001",
                        ErrorCategory.BUSINESS,
                        ErrorLayer.INQUIRY,
                        ErrorPhase.LOOKUP_INQUIRY,
                        false,
                        "Inquiry not found"),
        INQ_002(
                        HttpStatus.BAD_REQUEST,
                        "BAD_REQUEST",
                        "INQ-002",
                        ErrorCategory.BUSINESS,
                        ErrorLayer.TRANSFER,
                        ErrorPhase.VALIDATE_REQUEST,
                        false,
                        "Inquiry validation failed"),
        INQ_003(
                        HttpStatus.CONFLICT,
                        "CONFLICT",
                        "INQ-003",
                        ErrorCategory.BUSINESS,
                        ErrorLayer.TRANSFER,
                        ErrorPhase.CREATE_TRANSFER,
                        false,
                        "Inquiry already used by transfer"),

        TRF_001(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "TRF-001",
                        ErrorCategory.BUSINESS,
                        ErrorLayer.TRANSFER,
                        ErrorPhase.READ_RESOURCE,
                        false,
                        "Transfer not found"),
        TRF_002(
                        HttpStatus.CONFLICT,
                        "CONFLICT",
                        "TRF-002",
                        ErrorCategory.BUSINESS,
                        ErrorLayer.TRANSFER,
                        ErrorPhase.CREATE_TRANSFER,
                        false,
                        "Idempotency conflict"),

        OUT_001(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "OUT-001",
                        ErrorCategory.CORE,
                        ErrorLayer.OUTBOX,
                        ErrorPhase.PARSE_PAYLOAD,
                        false,
                        "Outbox payload parse failed"),
        OUT_002(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "OUT-002",
                        ErrorCategory.CORE,
                        ErrorLayer.WORKER,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "Outbox worker processing failed"),

        NET_001(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "NET-001",
                        ErrorCategory.NETWORK,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "Downstream connection failed"),
        NET_002(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "NET-002",
                        ErrorCategory.NETWORK,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "Downstream timeout"),
        NET_003(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "NET-003",
                        ErrorCategory.NETWORK,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "DNS resolution failed"),
        NET_004(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "NET-004",
                        ErrorCategory.NETWORK,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "TLS or SSL handshake failed"),

        EXT_001(
                        HttpStatus.BAD_GATEWAY,
                        "BAD_GATEWAY",
                        "EXT-001",
                        ErrorCategory.DOWNSTREAM,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        false,
                        "Downstream bank rejected transfer"),
        EXT_002(
                        HttpStatus.BAD_GATEWAY,
                        "BAD_GATEWAY",
                        "EXT-002",
                        ErrorCategory.DOWNSTREAM,
                        ErrorLayer.CONNECTOR,
                        ErrorPhase.DISPATCH_TRANSFER,
                        false,
                        "Downstream bank returned invalid response"),

        INF_DB_001(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "INF-DB-001",
                        ErrorCategory.INFRASTRUCTURE,
                        ErrorLayer.DATABASE,
                        ErrorPhase.WRITE_DATABASE,
                        true,
                        "Database write failed"),
        INF_DB_002(
                        HttpStatus.CONFLICT,
                        "CONFLICT",
                        "INF-DB-002",
                        ErrorCategory.INFRASTRUCTURE,
                        ErrorLayer.DATABASE,
                        ErrorPhase.WRITE_DATABASE,
                        false,
                        "Database unique constraint violation"),
        INF_SER_001(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "INF-SER-001",
                        ErrorCategory.INFRASTRUCTURE,
                        ErrorLayer.SYSTEM,
                        ErrorPhase.PARSE_PAYLOAD,
                        false,
                        "Serialization or deserialization failed"),
        OUT_003(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "OUT-003",
                        ErrorCategory.CORE,
                        ErrorLayer.OUTBOX,
                        ErrorPhase.DISPATCH_TRANSFER,
                        true,
                        "Outbox stuck processing recovered"),
        OUT_004(
                        HttpStatus.CONFLICT,
                        "CONFLICT",
                        "OUT-004",
                        ErrorCategory.CORE,
                        ErrorLayer.OUTBOX,
                        ErrorPhase.DISPATCH_TRANSFER,
                        false,
                        "Outbox event cannot be manually retried"),
        ISO_001(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "ISO-001",
                        ErrorCategory.CORE,
                        ErrorLayer.ISO,
                        ErrorPhase.READ_RESOURCE,
                        false,
                        "ISO message not found"),
        ISO_002(
                        HttpStatus.CONFLICT,
                        "CONFLICT",
                        "ISO-002",
                        ErrorCategory.CORE,
                        ErrorLayer.ISO,
                        ErrorPhase.VALIDATE_REQUEST,
                        false,
                        "ISO message invalid state"),
        ISO_003(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "ISO-003",
                        ErrorCategory.INFRASTRUCTURE,
                        ErrorLayer.ISO,
                        ErrorPhase.UNKNOWN,
                        false,
                        "ISO message crypto operation failed"),

        SYS_001(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "SYS-001",
                        ErrorCategory.UNKNOWN,
                        ErrorLayer.SYSTEM,
                        ErrorPhase.UNKNOWN,
                        false,
                        "Internal server error");

        private final HttpStatus httpStatus;
        private final String error;
        private final String errorCode;
        private final ErrorCategory category;
        private final ErrorLayer layer;
        private final ErrorPhase phase;
        private final boolean retryable;
        private final String defaultMessage;

        ErrorCatalog(HttpStatus httpStatus,
                        String error,
                        String errorCode,
                        ErrorCategory category,
                        ErrorLayer layer,
                        ErrorPhase phase,
                        boolean retryable,
                        String defaultMessage) {
                this.httpStatus = httpStatus;
                this.error = error;
                this.errorCode = errorCode;
                this.category = category;
                this.layer = layer;
                this.phase = phase;
                this.retryable = retryable;
                this.defaultMessage = defaultMessage;
        }

        public HttpStatus getHttpStatus() {
                return httpStatus;
        }

        public String getError() {
                return error;
        }

        public String getErrorCode() {
                return errorCode;
        }

        public ErrorCategory getCategory() {
                return category;
        }

        public ErrorLayer getLayer() {
                return layer;
        }

        public ErrorPhase getPhase() {
                return phase;
        }

        public boolean isRetryable() {
                return retryable;
        }

        public String getDefaultMessage() {
                return defaultMessage;
        }
}