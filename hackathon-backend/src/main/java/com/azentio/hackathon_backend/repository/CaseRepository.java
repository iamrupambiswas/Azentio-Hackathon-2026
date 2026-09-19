package com.azentio.hackathon_backend.repository;

import com.azentio.hackathon_backend.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<CaseEntity, String> {}
