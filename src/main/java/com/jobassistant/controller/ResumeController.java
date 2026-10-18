package com.jobassistant.controller;

import com.jobassistant.dto.ResumeRequest;
import com.jobassistant.dto.ResumeResponse;
import com.jobassistant.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> create(@RequestBody ResumeRequest req) {
        return ResponseEntity.ok(resumeService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list() {
        return ResponseEntity.ok(resumeService.listForUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(resumeService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> update(@PathVariable Long id, @RequestBody ResumeRequest req) {
        return ResponseEntity.ok(resumeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resumeService.delete(id);
        return ResponseEntity.ok().build();
    }
}