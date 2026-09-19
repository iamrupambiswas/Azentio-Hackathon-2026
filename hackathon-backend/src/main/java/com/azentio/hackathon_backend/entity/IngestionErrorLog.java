package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_error_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngestionErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String entityType; // CUSTOMER, ACCOUNT, TRANSACTION

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawDataRecord;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}