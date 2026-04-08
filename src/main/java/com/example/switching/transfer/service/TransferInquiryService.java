package com.example.switching.transfer.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.switching.transfer.dto.TransferInquiryResponse;
import com.example.switching.transfer.dto.TransferStatusHistoryItemResponse;

@Service
public class TransferInquiryService {

    public TransferInquiryResponse inquire(String transferRef) {
        TransferInquiryResponse response = new TransferInquiryResponse();
        response.setTransferRef(transferRef);
        response.setStatus("IN_PROGRESS");
        response.setSourceBank("BANK_A");
        response.setDestinationBank("BANK_B");
        response.setHistory(List.of(
                new TransferStatusHistoryItemResponse("RECEIVED", null, null)
        ));
        return response;
    }
}