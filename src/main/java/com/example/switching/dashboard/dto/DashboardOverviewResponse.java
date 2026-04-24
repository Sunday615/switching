package com.example.switching.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class DashboardOverviewResponse {

    private LocalDateTime generatedAt;

    private long inquiriesTotal;
    private List<StatusCountResponse> inquiryStatusCounts;

    private long transfersTotal;
    private List<StatusCountResponse> transferStatusCounts;

    private long outboxEventsTotal;
    private List<StatusCountResponse> outboxStatusCounts;

    public DashboardOverviewResponse() {
    }

    public DashboardOverviewResponse(LocalDateTime generatedAt,
                                     long inquiriesTotal,
                                     List<StatusCountResponse> inquiryStatusCounts,
                                     long transfersTotal,
                                     List<StatusCountResponse> transferStatusCounts,
                                     long outboxEventsTotal,
                                     List<StatusCountResponse> outboxStatusCounts) {
        this.generatedAt = generatedAt;
        this.inquiriesTotal = inquiriesTotal;
        this.inquiryStatusCounts = inquiryStatusCounts;
        this.transfersTotal = transfersTotal;
        this.transferStatusCounts = transferStatusCounts;
        this.outboxEventsTotal = outboxEventsTotal;
        this.outboxStatusCounts = outboxStatusCounts;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public long getInquiriesTotal() {
        return inquiriesTotal;
    }

    public void setInquiriesTotal(long inquiriesTotal) {
        this.inquiriesTotal = inquiriesTotal;
    }

    public List<StatusCountResponse> getInquiryStatusCounts() {
        return inquiryStatusCounts;
    }

    public void setInquiryStatusCounts(List<StatusCountResponse> inquiryStatusCounts) {
        this.inquiryStatusCounts = inquiryStatusCounts;
    }

    public long getTransfersTotal() {
        return transfersTotal;
    }

    public void setTransfersTotal(long transfersTotal) {
        this.transfersTotal = transfersTotal;
    }

    public List<StatusCountResponse> getTransferStatusCounts() {
        return transferStatusCounts;
    }

    public void setTransferStatusCounts(List<StatusCountResponse> transferStatusCounts) {
        this.transferStatusCounts = transferStatusCounts;
    }

    public long getOutboxEventsTotal() {
        return outboxEventsTotal;
    }

    public void setOutboxEventsTotal(long outboxEventsTotal) {
        this.outboxEventsTotal = outboxEventsTotal;
    }

    public List<StatusCountResponse> getOutboxStatusCounts() {
        return outboxStatusCounts;
    }

    public void setOutboxStatusCounts(List<StatusCountResponse> outboxStatusCounts) {
        this.outboxStatusCounts = outboxStatusCounts;
    }
}