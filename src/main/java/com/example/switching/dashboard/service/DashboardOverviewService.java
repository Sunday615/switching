package com.example.switching.dashboard.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.dashboard.dto.DashboardOverviewResponse;
import com.example.switching.dashboard.dto.StatusCountResponse;
import com.example.switching.inquiry.enums.InquiryStatus;
import com.example.switching.inquiry.repository.InquiryRepository;
import com.example.switching.outbox.enums.OutboxStatus;
import com.example.switching.outbox.repository.OutboxEventRepository;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.repository.TransferRepository;

@Service
public class DashboardOverviewService {

    private final InquiryRepository inquiryRepository;
    private final TransferRepository transferRepository;
    private final OutboxEventRepository outboxEventRepository;

    public DashboardOverviewService(InquiryRepository inquiryRepository,
                                    TransferRepository transferRepository,
                                    OutboxEventRepository outboxEventRepository) {
        this.inquiryRepository = inquiryRepository;
        this.transferRepository = transferRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        List<StatusCountResponse> inquiryStatusCounts = Arrays.stream(InquiryStatus.values())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        inquiryRepository.countByStatus(status)
                ))
                .toList();

        List<StatusCountResponse> transferStatusCounts = Arrays.stream(TransferStatus.values())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        transferRepository.countByStatus(status)
                ))
                .toList();

        List<StatusCountResponse> outboxStatusCounts = Arrays.stream(OutboxStatus.values())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        outboxEventRepository.countByStatus(status)
                ))
                .toList();

        return new DashboardOverviewResponse(
                LocalDateTime.now(),
                inquiryRepository.count(),
                inquiryStatusCounts,
                transferRepository.count(),
                transferStatusCounts,
                outboxEventRepository.count(),
                outboxStatusCounts
        );
    }
}