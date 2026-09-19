package com.azentio.hackathon_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "detection_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_detection_rule_code",
                        columnNames = "rule_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(nullable = false)
    private boolean enabled;

    @Column(precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    private Integer windowMinutes;

    private Integer minimumTransactionCount;
}