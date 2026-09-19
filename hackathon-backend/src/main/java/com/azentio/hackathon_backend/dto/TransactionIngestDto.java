package com.azentio.hackathon_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionIngestDto {
    @NotNull(message = "Source account ID is required")
    private String sourceAccountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Counterparty name is required")
    private String counterpartyName;

    @NotBlank(message = "Counterparty account is required")
    private String counterpartyAccount;

    @NotBlank(message = "Channel is required")
    private String channel; // WIRE, SWIFT, UPI, etc.

    @NotBlank(message = "Jurisdiction is required")
    private String jurisdiction; // Country code e.g. US, IN, KY

    private LocalDateTime timestamp;
}