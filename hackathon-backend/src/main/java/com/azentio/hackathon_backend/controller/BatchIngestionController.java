package com.azentio.hackathon_backend.controller;

import com.azentio.hackathon_backend.service.BatchIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ingest/batch")
@RequiredArgsConstructor
public class BatchIngestionController {

    private final BatchIngestionService batchIngestionService;

    @PostMapping("/customers")
    public ResponseEntity<String> uploadCustomers(@RequestParam("file") MultipartFile file) {
        batchIngestionService.ingestCustomersCsv(file);
        return ResponseEntity.ok("Customer CSV batch processing completed. Check error logs for any rejected records.");
    }

    @PostMapping("/accounts")
    public ResponseEntity<String> uploadAccounts(@RequestParam("file") MultipartFile file) {
        batchIngestionService.ingestAccountsCsv(file);
        return ResponseEntity.ok("Account CSV batch processing completed. Check error logs for any rejected records.");
    }
}
