package com.azentio.hackathon_backend.repository;

import com.azentio.hackathon_backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, String> {}
