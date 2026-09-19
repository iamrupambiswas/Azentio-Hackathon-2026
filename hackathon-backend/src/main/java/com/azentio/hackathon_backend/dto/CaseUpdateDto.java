package com.azentio.hackathon_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaseUpdateDto {
    @NotBlank(message = "Analyst name is required")
    private String assignedAnalyst;

    @NotBlank(message = "Disposition is required")
    private String disposition; // CLEARED, FILED_SAR, BLOCKED

    private String analystNotes;
}
