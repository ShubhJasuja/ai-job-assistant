package com.jobassistant.controller;

import com.jobassistant.entity.JobMatch;
import com.jobassistant.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for job matching.
 * GET  /api/v1/matches        → get all matches for the current user, sorted by score desc
 * GET  /api/v1/matches/{id}   → get a single match with explanation
 * POST /api/v1/matches/recalc → force recalculation of all matches
 */
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ResponseEntity<List<JobMatch>> getMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // User derived from SecurityContext; never from client parameters.
        return ResponseEntity.ok(matchService.getMatchesForCurrentUser(page, size));
    }

    @PostMapping("/recalc")
    public ResponseEntity<String> recalculate() {
        matchService.recalculateAll();
        return ResponseEntity.ok("Match recalculation started");
    }

    @PostMapping("/recalc/me")
    public ResponseEntity<String> recalculateForMe() {
        matchService.recalculateForCurrentUser();
        return ResponseEntity.ok("Recalculation for current user completed");
    }
}