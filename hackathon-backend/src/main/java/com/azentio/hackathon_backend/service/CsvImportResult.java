package com.azentio.hackathon_backend.service;

import java.util.List;

public record CsvImportResult(
        int totalRecords,
        int successfulRecords,
        int failedRecords,
        List<CsvImportError> errors
) {
}