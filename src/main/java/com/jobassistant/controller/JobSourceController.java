package com.jobassistant.controller;

import com.jobassistant.dto.JobRefreshSummary;
import com.jobassistant.service.JobRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-sources")
public class JobSourceController {
    private final JobRefreshService refreshService;

    public JobSourceController(JobRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<JobRefreshSummary> refresh() {
        return ResponseEntity.ok(refreshService.refresh());
    }
}
