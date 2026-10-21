package com.jobassistant.controller;

import com.jobassistant.dto.JobDiscoveryResponse;
import com.jobassistant.dto.JobSearchPage;
import com.jobassistant.dto.JobStateRequest;
import com.jobassistant.service.JobDiscoveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
public class JobDiscoveryController {
    private final JobDiscoveryService discoveryService;

    public JobDiscoveryController(JobDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public ResponseEntity<List<JobDiscoveryResponse>> discover(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "ALL") String state) {
        return ResponseEntity.ok(discoveryService.discover(
                keyword, company, location, remote, employmentType, skill, state));
    }

    @GetMapping("/page")
    public ResponseEntity<JobSearchPage<JobDiscoveryResponse>> discoverPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "24") int limit) {
        validatePageWindow(offset, limit);
        return ResponseEntity.ok(discoveryService.discoverPage(
                keyword, company, location, remote, employmentType, skill, state, offset, limit));
    }

    private void validatePageWindow(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Offset must be non-negative and limit must be between 1 and 100");
        }
    }

    @PutMapping("/{jobId}/state")
    public ResponseEntity<JobDiscoveryResponse> updateState(
            @PathVariable Long jobId,
            @Valid @RequestBody JobStateRequest request) {
        return ResponseEntity.ok(discoveryService.updateState(jobId, request.getState()));
    }
}
