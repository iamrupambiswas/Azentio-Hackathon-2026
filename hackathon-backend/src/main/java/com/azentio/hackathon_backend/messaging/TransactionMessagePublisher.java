package com.azentio.hackathon_backend.messaging;

import com.azentio.hackathon_backend.config.ActiveMqConfig;
import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TransactionMessagePublisher {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public void publish(TransactionIngestDto dto, String source) {

        TransactionIngestionMessage message =
                TransactionIngestionMessage.builder()
                        .sourceAccountId(dto.getSourceAccountId())
                        .amount(dto.getAmount())
                        .currency(dto.getCurrency())
                        .counterpartyName(dto.getCounterpartyName())
                        .counterpartyAccount(dto.getCounterpartyAccount())
                        .channel(dto.getChannel())
                        .jurisdiction(dto.getJurisdiction())
                        .timestamp(dto.getTimestamp())
                        .source(source)
                        .build();

        try {
            String json = objectMapper.writeValueAsString(message);

            jmsTemplate.convertAndSend(
                    ActiveMqConfig.TRANSACTION_QUEUE,
                    json
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to serialize transaction message",
                    e
            );
        }
    }
}