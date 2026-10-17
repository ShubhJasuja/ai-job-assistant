package com.jobassistant.service;

import com.jobassistant.ai.AIProvider;
import com.jobassistant.ai.JobAnalysisResult;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.Job;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AIAnalysisService {

    private final AIProvider aiProvider;
    private final JobRepository jobRepo;
    private final CandidateProfileRepository profileRepo;
    private final CurrentUserService currentUserService;

    public AIAnalysisService(AIProvider aiProvider, JobRepository jobRepo,
                             CandidateProfileRepository profileRepo,
                             CurrentUserService currentUserService) {
        this.aiProvider = aiProvider;
        this.jobRepo = jobRepo;
        this.profileRepo = profileRepo;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public JobAnalysisResult analyzeCurrentUserJob(Long jobId) {
        if (!aiProvider.isEnabled()) {
            throw new IllegalStateException("AI disabled");
        }
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (job.getIsActive() == null || !job.getIsActive()) {
            throw new IllegalArgumentException("Job is inactive");
        }
        Long userId = currentUserService.getCurrentUserId();
        CandidateProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));
        return aiProvider.analyzeJob(job, profile);
    }
}