package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false)
    private String assignedAnalyst;

    @Column(nullable = false)
    private String disposition; // CLEARED, FILED_SAR, BLOCKED

    @Column(columnDefinition = "TEXT")
    private String analystNotes;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}