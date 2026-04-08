package com.example.switching.transfer.service;

import org.springframework.stereotype.Service;

import com.example.switching.common.util.TransferRefGenerator;
import com.example.switching.transfer.dto.CreateTransferRequest;
import com.example.switching.transfer.dto.CreateTransferResponse;

@Service
public class CreateTransferService {

    private final TransferRefGenerator transferRefGenerator;

    public CreateTransferService(TransferRefGenerator transferRefGenerator) {
        this.transferRefGenerator = transferRefGenerator;
    }

    public CreateTransferResponse create(CreateTransferRequest request) {
        String transferRef = transferRefGenerator.generate();
        return new CreateTransferResponse(
                transferRef,
                "RECEIVED",
                "Transfer request accepted"
        );
    }
}