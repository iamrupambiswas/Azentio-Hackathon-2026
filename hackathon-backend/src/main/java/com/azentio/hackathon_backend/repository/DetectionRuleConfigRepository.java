package com.azentio.hackathon_backend.repository;

import com.azentio.hackathon_backend.entity.DetectionRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DetectionRuleConfigRepository
        extends JpaRepository<DetectionRuleConfig, String> {

    Optional<DetectionRuleConfig> findByRuleCode(String ruleCode);
}