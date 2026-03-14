package com.example.switching.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.switching.dto.TransferInquiryResponse;
import com.example.switching.dto.TransferStatusHistoryItemResponse;
import com.example.switching.entity.TransferEntity;
import com.example.switching.entity.TransferStatusHistoryEntity;
import com.example.switching.exception.TransferNotFoundException;
import com.example.switching.repository.TransferRepository;
import com.example.switching.repository.TransferStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferInquiryService {

    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository historyRepository;

    public TransferInquiryResponse getTransferByRef(String transferRef) {
        TransferEntity transfer = transferRepository.findByTransferRef(transferRef)
                .orElseThrow(() -> new TransferNotFoundException(transferRef));

        List<TransferStatusHistoryEntity> histories =
                historyRepository.findByTransferRefOrderByChangedAtAsc(transferRef);

        List<TransferStatusHistoryItemResponse> historyItems = histories.stream()
                .map(history -> new TransferStatusHistoryItemResponse(
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getReasonCode(),
                        history.getReasonMessage(),
                        history.getChangedBy(),
                        history.getChangedAt()
                ))
                .toList();

        return TransferInquiryResponse.builder()
                .transferRef(transfer.getTransferRef())
                .clientTransferId(transfer.getClientTransferId())
                .sourceBankCode(transfer.getSourceBankCode())
                .sourceAccountNo(transfer.getSourceAccountNo())
                .destinationBankCode(transfer.getDestinationBankCode())
                .destinationAccountNo(transfer.getDestinationAccountNo())
                .destinationAccountName(transfer.getDestinationAccountName())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .channelId(transfer.getChannelId())
                .routeCode(transfer.getRouteCode())
                .connectorName(transfer.getConnectorName())
                .externalReference(transfer.getExternalReference())
                .status(transfer.getStatus().name())
                .errorCode(transfer.getErrorCode())
                .errorMessage(transfer.getErrorMessage())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .statusHistory(historyItems)
                .build();
    }
}