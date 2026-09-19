package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String accountType; // CHECKING, SAVINGS, BUSINESS

    @Column(nullable = false)
    private String currency; // USD, INR, EUR, etc.

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private String riskRating; // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private LocalDateTime openingDate;

    @PrePersist
    public void prePersist() {
        if (this.openingDate == null) {
            this.openingDate = LocalDateTime.now();
        }
    }
}