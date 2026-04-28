package com.example.switching.connector;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.switching.outbox.dto.BankDispatchResult;
import com.example.switching.outbox.dto.DispatchIsoMessageCommand;
import com.example.switching.outbox.dto.DispatchTransferCommand;

@Component
public class MockBankConnector implements BankConnector {
    

 @Override
public BankDispatchResult dispatch(DispatchTransferCommand command) {
    return new BankDispatchResult(
            true,
            "00",
            "MOCK_TRANSFER_ACCEPTED",
            null,
            null
    );
}

    @Override
    public BankDispatchResult dispatchIsoMessage(DispatchIsoMessageCommand command) {
        if (command == null) {
            return new BankDispatchResult(
                    false,
                    "BANK-400",
                    "DispatchIsoMessageCommand is null",
                    null,
                    null
            );
        }

        if (!StringUtils.hasText(command.transferRef())) {
            return new BankDispatchResult(
                    false,
                    "BANK-400",
                    "transferRef is required",
                    null,
                    command.destinationBank()
            );
        }

        if (command.isoMessageId() == null) {
            return new BankDispatchResult(
                    false,
                    "BANK-400",
                    "isoMessageId is required",
                    command.transferRef(),
                    command.destinationBank()
            );
        }

        if (!StringUtils.hasText(command.encryptedPayload())) {
            return new BankDispatchResult(
                    false,
                    "BANK-400",
                    "encryptedPayload is required",
                    command.transferRef(),
                    command.destinationBank()
            );
        }

        if (!"BANK_B".equalsIgnoreCase(command.destinationBank())) {
            return new BankDispatchResult(
                    false,
                    "BANK-404",
                    "Destination bank not supported by mock connector",
                    command.transferRef(),
                    command.destinationBank()
            );
        }

        return new BankDispatchResult(
                true,
                "00",
                "MOCK_BANK_B_ACCEPTED_ENCRYPTED_ISO_MESSAGE",
                command.transferRef(),
                command.destinationBank()
        );
    }
}