package com.jobassistant.controller;

import com.jobassistant.dto.CoverLetterRequest;
import com.jobassistant.dto.CoverLetterResponse;
import com.jobassistant.service.CoverLetterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cover-letters")
public class CoverLetterController {

    private final CoverLetterService service;

    public CoverLetterController(CoverLetterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CoverLetterResponse> create(@RequestBody CoverLetterRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<CoverLetterResponse>> list() {
        return ResponseEntity.ok(service.listForUser());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<CoverLetterResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }
}
