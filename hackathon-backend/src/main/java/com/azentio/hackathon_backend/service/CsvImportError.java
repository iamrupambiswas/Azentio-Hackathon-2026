package com.azentio.hackathon_backend.service;

public record CsvImportError(
        long row,
        String error
) {
}