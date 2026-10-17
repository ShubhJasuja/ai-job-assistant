package com.jobassistant.controller;

import com.jobassistant.ai.JobAnalysisResult;
import com.jobassistant.service.AIAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AIAnalysisController {

    private final AIAnalysisService service;

    public AIAnalysisController(AIAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analyze/{jobId}")
    public ResponseEntity<JobAnalysisResult> analyze(@PathVariable Long jobId) {
        // User derived exclusively from SecurityContext; never from client param.
        JobAnalysisResult result = service.analyzeCurrentUserJob(jobId);
        return ResponseEntity.ok(result);
    }
}