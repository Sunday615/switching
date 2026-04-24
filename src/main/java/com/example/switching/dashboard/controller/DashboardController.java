package com.example.switching.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.dashboard.dto.DashboardOverviewResponse;
import com.example.switching.dashboard.service.DashboardOverviewService;

@RestController
public class DashboardController {

    private final DashboardOverviewService dashboardOverviewService;

    public DashboardController(DashboardOverviewService dashboardOverviewService) {
        this.dashboardOverviewService = dashboardOverviewService;
    }

    @GetMapping("/api/dashboard/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        DashboardOverviewResponse response = dashboardOverviewService.getOverview();
        return ResponseEntity.ok(response);
    }
}