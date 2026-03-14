package com.example.switching.worker;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.switching.connector.BankConnector;
import com.example.switching.dto.BankDispatchResult;
import com.example.switching.dto.DispatchTransferCommand;
import com.example.switching.entity.OutboxEventEntity;
import com.example.switching.enums.OutboxStatus;
import com.example.switching.repository.OutboxEventRepository;
import com.example.switching.service.OutboxTransactionService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxDispatchWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxTransactionService outboxTransactionService;
    private final List<BankConnector> bankConnectors;

    @Scheduled(fixedDelayString = "${switching.outbox.dispatch-delay-ms:5000}")
    public void dispatchNewOutboxEvents() {
        List<OutboxEventEntity> newEvents =
                outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);

        for (OutboxEventEntity event : newEvents) {
            try {
                DispatchTransferCommand command =
                        outboxTransactionService.markEventProcessing(event.getId());

                if (command == null) {
                    continue;
                }

                BankConnector connector = bankConnectors.stream()
                        .filter(candidate -> candidate.supports(command.getConnectorName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No connector found for: " + command.getConnectorName()
                        ));

                BankDispatchResult result = connector.dispatch(command);
                outboxTransactionService.completeDispatch(command.getOutboxEventId(), result);
            } catch (Exception ex) {
                outboxTransactionService.failDispatch(event.getId(), ex.getMessage());
            }
        }
    }
}