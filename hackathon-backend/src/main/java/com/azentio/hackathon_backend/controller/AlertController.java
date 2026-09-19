package com.azentio.hackathon_backend.controller;

import com.azentio.hackathon_backend.dto.CaseUpdateDto;
import com.azentio.hackathon_backend.entity.Alert;
import com.azentio.hackathon_backend.entity.CaseEntity;
import com.azentio.hackathon_backend.repository.AlertRepository;
import com.azentio.hackathon_backend.repository.CaseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compliance")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;
    private final CaseRepository caseRepository;

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertRepository.findAll());
    }

    @PostMapping("/alerts/{alertId}/case")
    public ResponseEntity<CaseEntity> createOrUpdateCase(
            @PathVariable String alertId,
            @Valid @RequestBody CaseUpdateDto dto) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

        // Update alert status accordingly
        alert.status = "INVESTIGATING";
        alertRepository.save(alert);

        CaseEntity complianceCase = CaseEntity.builder()
                .alert(alert)
                .assignedAnalyst(dto.getAssignedAnalyst())
                .disposition(dto.getDisposition())
                .analystNotes(dto.getAnalystNotes())
                .build();

        CaseEntity savedCase = caseRepository.save(complianceCase);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCase);
    }
}
