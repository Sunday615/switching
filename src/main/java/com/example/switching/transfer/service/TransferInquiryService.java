package com.example.switching.transfer.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.switching.transfer.dto.TransferInquiryResponse;
import com.example.switching.transfer.dto.TransferStatusHistoryItemResponse;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.exception.TransferNotFoundException;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class TransferInquiryService {

    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;

    public TransferInquiryService(TransferRepository transferRepository,
                                  TransferStatusHistoryRepository transferStatusHistoryRepository) {
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
    }

    public TransferInquiryResponse inquire(String transferRef) {
        TransferEntity transfer = transferRepository.findByTransferRef(transferRef)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found: " + transferRef));

        List<TransferStatusHistoryEntity> historyEntities =
                transferStatusHistoryRepository.findByTransferRefOrderByCreatedAtAsc(transferRef);

        List<TransferStatusHistoryItemResponse> history = historyEntities.stream()
                .map(item -> new TransferStatusHistoryItemResponse(
                        item.getStatus(),
                        item.getReasonCode(),
                        item.getCreatedAt() == null ? null : item.getCreatedAt().toString()
                ))
                .toList();

        TransferInquiryResponse response = new TransferInquiryResponse();
        response.setTransferRef(transfer.getTransferRef());
        response.setStatus(transfer.getStatus() == null ? null : transfer.getStatus().name());
        response.setSourceBank(transfer.getSourceBank());
        response.setDestinationBank(transfer.getDestinationBank());
        response.setHistory(history);

        return response;
    }
}