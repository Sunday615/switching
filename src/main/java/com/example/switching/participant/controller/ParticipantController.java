package com.example.switching.participant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.participant.dto.ParticipantListResponse;
import com.example.switching.participant.dto.ParticipantResponse;
import com.example.switching.participant.service.ParticipantService;

@RestController
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/api/participants")
    public ParticipantListResponse list(
            @RequestParam(required = false) String status
    ) {
        return participantService.list(status);
    }

    @GetMapping("/api/participants/{bankCode}")
    public ParticipantResponse getByBankCode(
            @PathVariable String bankCode
    ) {
        return participantService.getByBankCode(bankCode);
    }
}