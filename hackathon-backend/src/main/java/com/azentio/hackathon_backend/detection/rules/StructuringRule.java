package com.azentio.hackathon_backend.detection.rules;

import com.azentio.hackathon_backend.detection.DetectionRule;
import com.azentio.hackathon_backend.entity.Alert;
import com.azentio.hackathon_backend.entity.DetectionRuleConfig;
import com.azentio.hackathon_backend.entity.Transaction;
import com.azentio.hackathon_backend.repository.AlertRepository;
import com.azentio.hackathon_backend.repository.DetectionRuleConfigRepository;
import com.azentio.hackathon_backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StructuringRule implements DetectionRule {

    private static final String RULE_CODE = "STRUCTURING";

    private final TransactionRepository transactionRepository;
    private final DetectionRuleConfigRepository ruleConfigRepository;
    private final AlertRepository alertRepository;

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public void evaluate(Transaction transaction) {

        DetectionRuleConfig config =
                ruleConfigRepository
                        .findByRuleCode(RULE_CODE)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Structuring rule configuration not found"
                                ));

        if (!config.isEnabled()) {
            return;
        }

        LocalDateTime end = transaction.getTimestamp();

        LocalDateTime start =
                end.minusMinutes(config.getWindowMinutes());

        String customerId =
                transaction
                        .getSourceAccount()
                        .getCustomer()
                        .getId();

        List<Transaction> transactions =
                transactionRepository
                        .findBySourceAccountCustomerIdAndTimestampBetween(
                                customerId,
                                start,
                                end
                        );

        List<Transaction> suspiciousTransactions =
                transactions.stream()
                        .filter(tx ->
                                tx.getNormalizedAmountInBaseCurrency()
                                        .compareTo(config.getThresholdAmount()) < 0
                        )
                        .toList();

        if (suspiciousTransactions.size()
                < config.getMinimumTransactionCount()) {
            return;
        }

        createAlert(
                transaction,
                suspiciousTransactions,
                config
        );
    }

    private void createAlert(
            Transaction triggeringTransaction,
            List<Transaction> suspiciousTransactions,
            DetectionRuleConfig config) {

        String customerId =
                triggeringTransaction
                        .getSourceAccount()
                        .getCustomer()
                        .getId();

        if (hasExistingAlert(
                customerId,
                suspiciousTransactions)) {
            return;
        }

        BigDecimal totalAmount =
                suspiciousTransactions.stream()
                        .map(Transaction::getNormalizedAmountInBaseCurrency)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = suspiciousTransactions.size();

        int riskScore = calculateRiskScore(
                count,
                totalAmount,
                config.getThresholdAmount()
        );

        String explanation =
                buildExplanation(
                        count,
                        totalAmount,
                        config
                );

        Alert alert = Alert.builder()
                .customer(
                        triggeringTransaction
                                .getSourceAccount()
                                .getCustomer()
                )
                .triggeredRule(RULE_CODE)
                .riskScore(riskScore)
                .explanation(explanation)
                .status("OPEN")
                .transactionIds(
                        suspiciousTransactions.stream()
                                .map(Transaction::getId)
                                .toList()
                )
                .build();

        alertRepository.save(alert);
    }

    private int calculateRiskScore(
            int transactionCount,
            BigDecimal totalAmount,
            BigDecimal threshold) {

        int score = 60;

        if (transactionCount >= 5) {
            score += 10;
        }

        if (transactionCount >= 8) {
            score += 10;
        }

        if (totalAmount.compareTo(
                threshold.multiply(BigDecimal.valueOf(3))
        ) > 0) {
            score += 10;
        }

        if (totalAmount.compareTo(
                threshold.multiply(BigDecimal.valueOf(5))
        ) > 0) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private String buildExplanation(
            int transactionCount,
            BigDecimal totalAmount,
            DetectionRuleConfig config) {

        return String.format(
                "Detected %d transactions below the reporting threshold "
                        + "of %s within %d minutes. "
                        + "Combined normalized transaction value is %s. "
                        + "This pattern may indicate transaction structuring.",
                transactionCount,
                config.getThresholdAmount(),
                config.getWindowMinutes(),
                totalAmount
        );
    }

    private boolean hasExistingAlert(
            String customerId,
            List<Transaction> transactions) {

        // Temporary implementation.
        return false;
    }
}