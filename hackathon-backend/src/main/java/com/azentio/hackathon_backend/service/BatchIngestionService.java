package com.azentio.hackathon_backend.service;

import com.azentio.hackathon_backend.entity.Account;
import com.azentio.hackathon_backend.entity.Customer;
import com.azentio.hackathon_backend.entity.IngestionErrorLog;
import com.azentio.hackathon_backend.repository.AccountRepository;
import com.azentio.hackathon_backend.repository.CustomerRepository;
import com.azentio.hackathon_backend.repository.IngestionErrorLogRepository;
import com.azentio.hackathon_backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchIngestionService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IngestionErrorLogRepository errorLogRepository;

    @Transactional
    public void ingestCustomersCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] tokens = line.split(",");
                if (tokens.length < 5) {
                    logError("CUSTOMER", line, "Insufficient columns provided");
                    continue;
                }
                try {
                    Customer customer = Customer.builder()
                            .id(tokens[0].trim().isEmpty() ? UUID.randomUUID().toString() : tokens[0].trim())
                            .fullName(tokens[1].trim())
                            .email(tokens[2].trim())
                            .nationalIdMasked(tokens[3].trim())
                            .dateOfBirth(LocalDate.parse(tokens[4].trim()))
                            .riskRating(tokens.length > 5 ? tokens[5].trim() : "LOW")
                            .build();
                    customerRepository.save(customer);
                } catch (Exception e) {
                    logError("CUSTOMER", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse customer CSV: " + e.getMessage());
        }
    }

    @Transactional
    public void ingestAccountsCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] tokens = line.split(",");
                if (tokens.length < 5) {
                    logError("ACCOUNT", line, "Insufficient columns provided");
                    continue;
                }
                try {
                    String customerId = tokens[1].trim();
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new IllegalArgumentException("Referential integrity violation: Customer ID " + customerId + " not found"));

                    Account account = Account.builder()
                            .accountNumber(tokens[2].trim())
                            .customer(customer)
                            .accountType(tokens[3].trim())
                            .currency(tokens[4].trim())
                            .balance(tokens.length > 5 && !tokens[5].trim().isEmpty() ? new BigDecimal(tokens[5].trim()) : BigDecimal.ZERO)
                            .riskRating(tokens.length > 6 ? tokens[6].trim() : "LOW")
                            .openingDate(LocalDateTime.now())
                            .build();
                    accountRepository.save(account);
                } catch (Exception e) {
                    logError("ACCOUNT", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse accounts CSV: " + e.getMessage());
        }
    }

    private void logError(String entityType, String record, String message) {
        log.warn("Ingestion Error [{}]: {} | Reason: {}", entityType, record, message);
        IngestionErrorLog errorLog = IngestionErrorLog.builder()
                .entityType(entityType)
                .rawDataRecord(record)
                .errorMessage(message)
                .build();
        errorLogRepository.save(errorLog);
    }
}
