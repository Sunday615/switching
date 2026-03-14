package com.example.switching.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankDispatchResult {
    private String status;
    private String externalReference;
    private String responseCode;
    private String responseMessage;
}