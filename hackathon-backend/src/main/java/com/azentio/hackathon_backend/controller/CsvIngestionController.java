package com.azentio.hackathon_backend.controller;

import com.azentio.hackathon_backend.service.CsvImportResult;
import com.azentio.hackathon_backend.service.CsvIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class CsvIngestionController {

    private final CsvIngestionService csvIngestionService;

    @PostMapping(
            value = "/csv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CsvImportResult> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        CsvImportResult result =
                csvIngestionService.process(file);

        return ResponseEntity.ok(result);
    }
}
