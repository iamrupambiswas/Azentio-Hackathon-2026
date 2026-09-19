package com.azentio.hackathon_backend.repository;

import com.azentio.hackathon_backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, String> {

    List<Transaction> findBySourceAccountCustomerIdAndTimestampBetween(
            String customerId,
            LocalDateTime start,
            LocalDateTime end
    );
}