package com.jobassistant.service;

import com.jobassistant.entity.*;
import com.jobassistant.entity.User;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.JobMatchRepository;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the matching process.
 * Calls MatchingEngine to compute scores, saves results as JobMatch records.
 */
@Service
public class MatchService {

    private final UserRepository userRepo;
    private final JobRepository jobRepo;
    private final JobMatchRepository matchRepo;
    private final CandidateProfileRepository profileRepo;
    private final MatchingEngine engine;

    public MatchService(UserRepository userRepo, JobRepository jobRepo,
                        JobMatchRepository matchRepo, CandidateProfileRepository profileRepo,
                        MatchingEngine engine) {
        this.userRepo = userRepo;
        this.jobRepo = jobRepo;
        this.matchRepo = matchRepo;
        this.profileRepo = profileRepo;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public List<JobMatch> getMatchesForCurrentUser(int page, int size) {
        // Extract user from SecurityContext (JWT-authenticated user)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId;
        if (principal instanceof String) {
            // JWT authentication: principal is the email String
            // Look up user by email to get the actual Long userId
            userId = userRepo.findByEmail((String) principal)
                    .map(User::getId)
                    .orElse(null);
        } else if (principal instanceof UserDetails) {
            userId = userRepo.findByEmail(((UserDetails) principal).getUsername())
                    .map(User::getId)
                    .orElse(null);
        } else {
            userId = null;
        }
        if (userId == null) {
            // Fallback: return empty list when no user authenticated
            return List.of();
        }
        return matchRepo.findByUserIdOrderByScoreDesc(userId, org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    @Transactional
    public void recalculateAll() {
        List<User> users = userRepo.findAll();
        List<Job> jobs = jobRepo.findAll();

        for (User user : users) {
            CandidateProfile profile = profileRepo.findByUserId(user.getId())
                    .orElse(null);
            if (profile == null) continue;

            for (Job job : jobs) {
                JobMatch match = engine.compute(user, profile, job);
                matchRepo.save(match);
            }
        }
    }
}