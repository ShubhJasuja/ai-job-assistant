package com.jobassistant.controller;

import com.jobassistant.dto.UpdateStatusRequest;
import com.jobassistant.entity.Application;
import com.jobassistant.entity.ApplicationEvent;
import com.jobassistant.service.ApplicationService;
import com.jobassistant.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for managing job applications.
 *
 * GET  /api/v1/applications            → list all (paginated)
 * POST /api/v1/applications             → create new application (status = DISCOVERED)
 * GET  /api/v1/applications/{id}        → get application details
 * PUT  /api/v1/applications/{id}/status → move to a new status
 * GET  /api/v1/applications/{id}/events → get audit trail
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUserService currentUserService;

    public ApplicationController(ApplicationService applicationService, CurrentUserService currentUserService) {
        this.applicationService = applicationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<Application> create(@RequestParam Long jobId) {
        // User is NEVER taken from client parameters.
        // It is derived from the authenticated JWT via SecurityContext.
        Long userId = currentUserService.getCurrentUserId();
        Application app = applicationService.create(jobId, userId);
        return ResponseEntity.ok(app);
    }

    @PutMapping("/{appId}/status")
    public ResponseEntity<Application> updateStatus(
            @PathVariable Long appId,
            @RequestBody UpdateStatusRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        Application app = applicationService.updateStatus(appId, request, userId);
        return ResponseEntity.ok(app);
    }

    @GetMapping("/{appId}/events")
    public ResponseEntity<List<ApplicationEvent>> getEvents(@PathVariable Long appId) {
        return ResponseEntity.ok(applicationService.getEventHistory(appId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        // User is derived from SecurityContext, never from client parameter
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getStatusCountsAsString(userId));
    }
}