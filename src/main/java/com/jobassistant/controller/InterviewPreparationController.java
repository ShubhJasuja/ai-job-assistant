package com.jobassistant.controller;

import com.jobassistant.dto.InterviewPreparationRequest;
import com.jobassistant.dto.InterviewPreparationResponse;
import com.jobassistant.service.InterviewPreparationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interview-preparation")
public class InterviewPreparationController {

    private final InterviewPreparationService service;

    public InterviewPreparationController(InterviewPreparationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InterviewPreparationResponse> create(@RequestBody InterviewPreparationRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public ResponseEntity<List<InterviewPreparationResponse>> list() {
        return ResponseEntity.ok(service.listForUser());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewPreparationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterviewPreparationResponse> update(@PathVariable Long id, @RequestBody InterviewPreparationRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
