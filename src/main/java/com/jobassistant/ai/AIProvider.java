package com.jobassistant.ai;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.Resume;

/**
 * Abstract base class for AI providers.
 * All AI capabilities (job analysis, resume tailoring, cover letter generation)
 * go through this interface, which is configurable via application.yml.
 */
public interface AIProvider {

    String getProviderName();

    /**
     * Analyze a job posting and explain how it matches the candidate's profile.
     * Returns structured analysis: required skills, preferred skills, responsibilities,
     * potential gaps, and recommendations.
     */
    JobAnalysisResult analyzeJob(Job job, CandidateProfile profile);

    /**
     * Generate a tailored version of the candidate's resume for a specific job.
     * Returns a diff/new version of the resume (as plain text or markdown).
     */
    ResumeTailoringResult tailorResume(Resume resume, Job job);

    /**
     * Generate a draft cover letter based on the candidate's profile and the job description.
     * The result is user‑approved before being used.
     */
    CoverLetterResult generateCoverLetter(CandidateProfile profile, Job job);

    /**
     * Whether this provider is enabled (configurable via AI_ENABLED flag).
     */
    boolean isEnabled();
}