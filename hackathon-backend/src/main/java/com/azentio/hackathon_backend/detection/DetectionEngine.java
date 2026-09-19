package com.azentio.hackathon_backend.detection;

import com.azentio.hackathon_backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DetectionEngine {

    private final List<DetectionRule> detectionRules;

    public void evaluate(Transaction transaction) {

        for (DetectionRule rule : detectionRules) {
            rule.evaluate(transaction);
        }
    }
}