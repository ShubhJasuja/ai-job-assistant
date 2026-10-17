package com.jobassistant.service;

import com.jobassistant.ai.AIProvider;
import com.jobassistant.ai.ResumeTailoringResult;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.Resume;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeTailoringService {

    private final AIProvider aiProvider;
    private final ResumeRepository resumeRepo;
    private final JobRepository jobRepo;
    private final CurrentUserService currentUserService;

    public ResumeTailoringService(AIProvider aiProvider, ResumeRepository resumeRepo,
                                  JobRepository jobRepo, CurrentUserService currentUserService) {
        this.aiProvider = aiProvider;
        this.resumeRepo = resumeRepo;
        this.jobRepo = jobRepo;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ResumeTailoringResult tailor(Long resumeId, Long jobId) {
        if (!aiProvider.isEnabled()) {
            throw new IllegalStateException("AI disabled");
        }
        Long userId = currentUserService.getCurrentUserId();
        Resume resume = resumeRepo.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (resume.getUser() == null || !resume.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Resume does not belong to current user");
        }
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (job.getIsActive() == null || !job.getIsActive()) {
            throw new IllegalArgumentException("Job is inactive");
        }
        return aiProvider.tailorResume(resume, job);
    }
}