package com.example.switching.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.switching.common.dto.ApiErrorResponse;
import com.example.switching.common.error.ErrorCatalog;
import com.example.switching.common.filter.RequestIdFilter;
import com.example.switching.idempotency.exception.IdempotencyConflictException;
import com.example.switching.inquiry.exception.InquiryNotFoundException;
import com.example.switching.iso.exception.IsoMessageCryptoException;
import com.example.switching.iso.exception.IsoMessageInvalidStateException;
import com.example.switching.iso.exception.IsoMessageNotFoundException;
import com.example.switching.transfer.exception.InquiryAlreadyUsedException;
import com.example.switching.transfer.exception.InquiryValidationException;
import com.example.switching.transfer.exception.TransferNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, Object> details = new LinkedHashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
                ex.getBindingResult().getGlobalErrors()
                                .forEach(error -> details.put(error.getObjectName(), error.getDefaultMessage()));

                return buildResponse(
                                ErrorCatalog.REQ_001,
                                "Request validation failed",
                                request,
                                details);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.REQ_001,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.REQ_002,
                                "Malformed JSON request",
                                request,
                                null);
        }

        @ExceptionHandler(InquiryValidationException.class)
        public ResponseEntity<ApiErrorResponse> handleInquiryValidation(
                        InquiryValidationException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.INQ_002,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(InquiryNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleInquiryNotFound(
                        InquiryNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.INQ_001,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(InquiryAlreadyUsedException.class)
        public ResponseEntity<ApiErrorResponse> handleInquiryAlreadyUsed(
                        InquiryAlreadyUsedException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.INQ_003,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(TransferNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleTransferNotFound(
                        TransferNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.TRF_001,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(IdempotencyConflictException.class)
        public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(
                        IdempotencyConflictException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.TRF_002,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.INF_DB_002,
                                "Database constraint violation",
                                request,
                                null);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
                        HttpRequestMethodNotSupportedException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.REQ_003,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.SYS_001,
                                "Internal server error",
                                request,
                                null);
        }

 

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.REQ_001,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(IsoMessageNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleIsoMessageNotFound(
                        IsoMessageNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.ISO_001,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(IsoMessageInvalidStateException.class)
        public ResponseEntity<ApiErrorResponse> handleIsoMessageInvalidState(
                        IsoMessageInvalidStateException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.ISO_002,
                                ex.getMessage(),
                                request,
                                null);
        }

        @ExceptionHandler(IsoMessageCryptoException.class)
        public ResponseEntity<ApiErrorResponse> handleIsoMessageCrypto(
                        IsoMessageCryptoException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                ErrorCatalog.ISO_003,
                                ex.getMessage(),
                                request,
                                null);
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        ErrorCatalog catalog,
                        String message,
                        HttpServletRequest request,
                        Map<String, Object> details) {

                ApiErrorResponse body = new ApiErrorResponse();
                body.setTimestamp(java.time.LocalDateTime.now());
                body.setStatus(catalog.getHttpStatus().value());
                body.setError(catalog.getError());
                body.setErrorCode(catalog.getErrorCode());
                body.setCategory(catalog.getCategory().name());
                body.setLayer(catalog.getLayer().name());
                body.setPhase(catalog.getPhase().name());
                body.setRetryable(catalog.isRetryable());
                body.setMessage(message != null ? message : catalog.getDefaultMessage());
                body.setPath(request.getRequestURI());
                body.setRequestId((String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
                body.setDetails(details);

                return ResponseEntity.status(catalog.getHttpStatus()).body(body);
        }
}