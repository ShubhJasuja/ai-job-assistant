package com.jobassistant.ai;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.Resume;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of AIProvider.
 * Used when AI_ENABLED=false or when no other provider is configured.
 * All methods return empty results — the system works without AI.
 */
@Component
@ConditionalOnProperty(name = "ai.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAIProvider implements AIProvider {

    @Override
    public String getProviderName() {
        return "Noop";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public JobAnalysisResult analyzeJob(Job job, CandidateProfile profile) {
        return new JobAnalysisResult();
    }

    @Override
    public ResumeTailoringResult tailorResume(Resume resume, Job job) {
        return new ResumeTailoringResult();
    }

    @Override
    public CoverLetterResult generateCoverLetter(CandidateProfile profile, Job job) {
        return new CoverLetterResult();
    }
}