package com.jobassistant.controller;

import com.jobassistant.dto.CandidateProfileRequest;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.service.CandidateProfileService;
import com.jobassistant.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final CandidateProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfileController(CandidateProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<CandidateProfile> getProfile() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(profileService.getProfileForUser(userId));
    }

    @PutMapping
    public ResponseEntity<CandidateProfile> updateProfile(@RequestBody CandidateProfileRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(profileService.updateProfileForUser(userId, request));
    }
}