package com.jobassistant.controller;

import com.jobassistant.dto.JobCreateRequest;
import com.jobassistant.dto.JobResponse;
import com.jobassistant.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST APIs for job ingestion and management.
 * Jobs are shared/global data.
 * Create/update/deactivate require authentication.
 * List/read are public (no user isolation needed for shared data).
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobCreateRequest request) {
        JobResponse response = jobService.createJob(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(jobService.listJobs(keyword, company, location, remote, employmentType, active));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(@PathVariable Long id, @Valid @RequestBody JobCreateRequest request) {
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateJob(@PathVariable Long id) {
        jobService.deactivateJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/duplicate-check")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam String source, @RequestParam String sourceJobId) {
        return ResponseEntity.ok(jobService.isDuplicate(source, sourceJobId));
    }
}