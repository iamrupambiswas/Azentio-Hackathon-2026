package com.azentio.hackathon_backend.controller;

import com.azentio.hackathon_backend.dto.TransactionIngestDto;
import com.azentio.hackathon_backend.messaging.TransactionMessagePublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final TransactionMessagePublisher publisher;

    @PostMapping("/transaction")
    public ResponseEntity<Void> ingestTransaction(
            @Valid @RequestBody TransactionIngestDto dto) {

        publisher.publish(dto, "REST");

        return ResponseEntity.accepted().build();
    }
}