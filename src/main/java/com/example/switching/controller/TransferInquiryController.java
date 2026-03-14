package com.example.switching.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.dto.TransferInquiryResponse;
import com.example.switching.service.TransferInquiryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferInquiryController {

    private final TransferInquiryService transferInquiryService;

    @GetMapping("/{transferRef}")
    public TransferInquiryResponse getTransferByRef(@PathVariable String transferRef) {
        return transferInquiryService.getTransferByRef(transferRef);
    }
}