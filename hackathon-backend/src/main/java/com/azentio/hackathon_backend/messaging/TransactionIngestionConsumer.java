package com.azentio.hackathon_backend.messaging;

import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import com.azentio.hackathon_backend.entity.IngestionErrorLog;
import com.azentio.hackathon_backend.repository.IngestionErrorLogRepository;
import com.azentio.hackathon_backend.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class TransactionIngestionConsumer {

    private final IngestionService ingestionService;
    private final IngestionErrorLogRepository ingestionErrorLogRepository;
    private final ObjectMapper objectMapper;

    @JmsListener(
            destination = "transaction.ingestion"
    )
    public void consume(String json) {

        try {

            TransactionIngestionMessage message =
                    objectMapper.readValue(
                            json,
                            TransactionIngestionMessage.class
                    );

            TransactionIngestDto dto =
                    TransactionIngestDto.builder()
                            .sourceAccountId(message.getSourceAccountId())
                            .amount(message.getAmount())
                            .currency(message.getCurrency())
                            .counterpartyName(message.getCounterpartyName())
                            .counterpartyAccount(message.getCounterpartyAccount())
                            .channel(message.getChannel())
                            .jurisdiction(message.getJurisdiction())
                            .timestamp(message.getTimestamp())
                            .build();

            ingestionService.ingestTransaction(dto);

        } catch (Exception e) {

            logIngestionError(
                    json,
                    e.getMessage()
            );
        }
    }

    private void logIngestionError(
            String rawData,
            String errorMessage) {

        IngestionErrorLog errorLog =
                IngestionErrorLog.builder()
                        .entityType("TRANSACTION")
                        .rawDataRecord(rawData)
                        .errorMessage(errorMessage)
                        .build();

        ingestionErrorLogRepository.save(errorLog);
    }
}