package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String triggeredRule;

    @Column(nullable = false)
    private Integer riskScore;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Column(nullable = false)
    public String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(
            name = "alert_transaction_evidence",
            joinColumns = @JoinColumn(name = "alert_id")
    )
    @Column(name = "transaction_id", nullable = false)
    @Builder.Default
    private List<String> transactionIds = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}