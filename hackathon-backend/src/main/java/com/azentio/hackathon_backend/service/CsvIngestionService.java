package com.azentio.hackathon_backend.service;

import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import com.azentio.hackathon_backend.messaging.TransactionMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvIngestionService {

    private final TransactionMessagePublisher publisher;

    public CsvImportResult process(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV file is empty"
            );
        }

        List<CsvImportError> errors = new ArrayList<>();

        int totalRecords = 0;
        int validRecords = 0;

        try (
                Reader reader = new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {

            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : parser) {

                totalRecords++;

                try {

                    TransactionIngestDto dto =
                            parseRecord(record);

                    publisher.publish(dto, "CSV");

                    validRecords++;

                } catch (Exception ex) {

                    errors.add(
                            new CsvImportError(
                                    totalRecords,
                                    ex.getMessage()
                            )
                    );
                }
            }

        } catch (IOException ex) {

            throw new RuntimeException(
                    "Failed to read CSV file",
                    ex
            );
        }

        return new CsvImportResult(
                totalRecords,
                validRecords,
                errors.size(),
                errors
        );
    }

    private TransactionIngestDto parseRecord(
            CSVRecord record) {

        return TransactionIngestDto.builder()
                .sourceAccountId(
                        required(record, "sourceAccountId")
                )
                .amount(
                        new BigDecimal(
                                required(record, "amount")
                        )
                )
                .currency(
                        required(record, "currency")
                )
                .counterpartyName(
                        required(record, "counterpartyName")
                )
                .counterpartyAccount(
                        required(record, "counterpartyAccount")
                )
                .channel(
                        required(record, "channel")
                )
                .jurisdiction(
                        required(record, "jurisdiction")
                )
                .timestamp(
                        LocalDateTime.parse(
                                required(record, "timestamp")
                        )
                )
                .build();
    }

    private String required(
            CSVRecord record,
            String column) {

        String value = record.get(column);

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(
                    column + " is required"
            );
        }

        return value.trim();
    }
}