package com.azentio.hackathon_backend.messaging;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIngestionMessage implements Serializable {

    private String sourceAccountId;

    private BigDecimal amount;

    private String currency;

    private String counterpartyName;

    private String counterpartyAccount;

    private String channel;

    private String jurisdiction;

    private LocalDateTime timestamp;

    private String source;
}