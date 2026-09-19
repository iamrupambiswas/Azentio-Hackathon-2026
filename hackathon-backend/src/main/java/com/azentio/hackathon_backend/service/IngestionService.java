package com.azentio.hackathon_backend.service;

import com.azentio.hackathon_backend.detection.DetectionEngine;
import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import com.azentio.hackathon_backend.entity.Account;
import com.azentio.hackathon_backend.entity.Transaction;
import com.azentio.hackathon_backend.repository.AccountRepository;
import com.azentio.hackathon_backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionValidationService validationService;
    private final DetectionEngine detectionEngine;

    @Transactional
    public Transaction ingestTransaction(TransactionIngestDto dto) {

        Account account = validationService.validate(dto);

        BigDecimal normalizedAmount = dto.getAmount();

        Transaction tx = Transaction.builder()
                .sourceAccount(account)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .normalizedAmountInBaseCurrency(normalizedAmount)
                .counterpartyName(dto.getCounterpartyName())
                .counterpartyAccount(dto.getCounterpartyAccount())
                .channel(dto.getChannel())
                .jurisdiction(dto.getJurisdiction())
                .timestamp(
                        dto.getTimestamp() != null
                                ? dto.getTimestamp()
                                : LocalDateTime.now()
                )
                .build();

        Transaction savedTransaction =
                transactionRepository.save(tx);

        detectionEngine.evaluate(savedTransaction);

        return savedTransaction;
    }
}