package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_timestamp", columnList = "timestamp"),
        @Index(name = "idx_transaction_account", columnList = "source_account_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal normalizedAmountInBaseCurrency; // e.g., in INR

    @Column(nullable = false)
    private String counterpartyName;

    @Column(nullable = false)
    private String counterpartyAccount;

    @Column(nullable = false)
    private String channel; // WIRE, SWIFT, UPI, ATM, ACH

    @Column(nullable = false)
    private String jurisdiction; // Country code e.g. US, IN, KY (Cayman)

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}