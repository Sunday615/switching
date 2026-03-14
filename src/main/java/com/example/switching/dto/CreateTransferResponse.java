package com.example.switching.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTransferResponse {
    private String transferRef;
    private String clientTransferId;
    private String status;
    private String message;
}