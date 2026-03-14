package com.example.switching.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.dto.CreateTransferRequest;
import com.example.switching.dto.CreateTransferResponse;
import com.example.switching.service.CreateTransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final CreateTransferService createTransferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTransferResponse createTransfer(
            @RequestHeader("X-Channel-Id") String channelId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        return createTransferService.createTransfer(channelId, idempotencyKey, request);
    }
}