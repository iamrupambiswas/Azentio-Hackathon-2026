package com.azentio.hackathon_backend.service;

import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import com.azentio.hackathon_backend.entity.Account;
import com.azentio.hackathon_backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionValidationService {

    private final AccountRepository accountRepository;

    public Account validate(TransactionIngestDto dto) {

        Account account = accountRepository
                .findById(dto.getSourceAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found: "
                                        + dto.getSourceAccountId()
                        ));

        validateCurrency(dto.getCurrency());

        validateChannel(dto.getChannel());

        validateJurisdiction(dto.getJurisdiction());

        return account;
    }

    private void validateCurrency(String currency) {

        if (currency == null ||
                !currency.matches("[A-Z]{3}")) {

            throw new IllegalArgumentException(
                    "Invalid currency: " + currency
            );
        }
    }

    private void validateChannel(String channel) {

        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException(
                    "Channel is required"
            );
        }
    }

    private void validateJurisdiction(String jurisdiction) {

        if (jurisdiction == null ||
                !jurisdiction.matches("[A-Z]{2}")) {

            throw new IllegalArgumentException(
                    "Invalid jurisdiction: " + jurisdiction
            );
        }
    }
}