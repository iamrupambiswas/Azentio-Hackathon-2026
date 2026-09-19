package com.azentio.hackathon_backend.repository;

import com.azentio.hackathon_backend.entity.IngestionErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionErrorLogRepository extends JpaRepository<IngestionErrorLog, String> {}