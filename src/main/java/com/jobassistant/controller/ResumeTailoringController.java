package com.jobassistant.controller;

import com.jobassistant.ai.ResumeTailoringResult;
import com.jobassistant.service.ResumeTailoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class ResumeTailoringController {

    private final ResumeTailoringService service;

    public ResumeTailoringController(ResumeTailoringService service) {
        this.service = service;
    }

    @PostMapping("/tailor/{resumeId}/{jobId}")
    public ResponseEntity<ResumeTailoringResult> tailor(@PathVariable Long resumeId,
                                                        @PathVariable Long jobId) {
        // User derived exclusively from SecurityContext via service; never from client param.
        ResumeTailoringResult result = service.tailor(resumeId, jobId);
        return ResponseEntity.ok(result);
    }
}