package com.jobassistant.service;

import com.jobassistant.dto.CandidateProfileRequest;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.Preferences;
import com.jobassistant.entity.User;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing a user's candidate profile.
 * Each user has exactly one profile (1:1 with User).
 */
@Service
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepo;
    private final UserRepository userRepo;

    public CandidateProfileService(CandidateProfileRepository profileRepo, UserRepository userRepo) {
        this.profileRepo = profileRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public CandidateProfile getProfileForUser(Long userId) {
        return profileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));
    }

    @Transactional
    public CandidateProfile updateProfileForUser(Long userId, CandidateProfileRequest req) {
        CandidateProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));

        profile.setHeadline(req.getHeadline());
        profile.setSummary(req.getSummary());
        profile.setPreferredRoles(req.getPreferredRoles());
        profile.setPreferredLocations(req.getPreferredLocations());
        profile.setRemotePreference(req.getRemotePreference());
        profile.setExperienceLevel(req.getExperienceLevel());

        return profileRepo.save(profile);
    }

    @Transactional
    public CandidateProfile createProfileForUser(Long userId, CandidateProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (profileRepo.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Profile already exists for user");
        }

        CandidateProfile profile = CandidateProfile.builder()
                .user(user)
                .headline(req.getHeadline())
                .summary(req.getSummary())
                .preferredRoles(req.getPreferredRoles())
                .preferredLocations(req.getPreferredLocations())
                .remotePreference(req.getRemotePreference())
                .experienceLevel(req.getExperienceLevel())
                .build();

        return profileRepo.save(profile);
    }
}