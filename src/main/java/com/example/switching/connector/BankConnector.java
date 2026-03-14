package com.example.switching.connector;

import com.example.switching.dto.BankDispatchResult;
import com.example.switching.dto.DispatchTransferCommand;

public interface BankConnector {
    boolean supports(String connectorName);
    BankDispatchResult dispatch(DispatchTransferCommand command);
}