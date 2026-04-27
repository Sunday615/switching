package com.example.switching.iso.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.switching.iso.dto.IsoMessageDetailResponse;
import com.example.switching.iso.dto.IsoMessageListResponse;
import com.example.switching.iso.service.IsoMessageQueryService;

@RestController
public class IsoMessageController {

    private final IsoMessageQueryService isoMessageQueryService;

    public IsoMessageController(IsoMessageQueryService isoMessageQueryService) {
        this.isoMessageQueryService = isoMessageQueryService;
    }

    @GetMapping("/api/iso-messages")
    public ResponseEntity<IsoMessageListResponse> searchIsoMessages(
            @RequestParam(value = "messageType", required = false) String messageType,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "correlationRef", required = false) String correlationRef,
            @RequestParam(value = "inquiryRef", required = false) String inquiryRef,
            @RequestParam(value = "transferRef", required = false) String transferRef,
            @RequestParam(value = "endToEndId", required = false) String endToEndId,
            @RequestParam(value = "limit", required = false) Integer limit) {

        IsoMessageListResponse response = isoMessageQueryService.search(
                messageType,
                direction,
                correlationRef,
                inquiryRef,
                transferRef,
                endToEndId,
                limit
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/iso-messages/{id}")
    public ResponseEntity<IsoMessageDetailResponse> getIsoMessageById(
            @PathVariable("id") Long id) {

        IsoMessageDetailResponse response = isoMessageQueryService.getById(id);
        return ResponseEntity.ok(response);
    }
}