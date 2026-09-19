package com.azentio.hackathon_backend.detection;

import com.azentio.hackathon_backend.entity.Transaction;

public interface DetectionRule {

    String getRuleCode();

    void evaluate(Transaction transaction);
}