package com.jobassistant.ai;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.Resume;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of job analysis from the AI provider.
 * Includes the job description, required skills, preferred skills,
 * skill gaps, and a human-readable explanation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobAnalysisResult {
    private String summary; // One-sentence summary of the job
    private List<String> requiredSkills; // Skills explicitly required by the job
    private List<String> preferredSkills; // Skills the candidate should have
    private List<String> skillGaps; // Skills the candidate lacks
    private String analysisExplanation; // Detailed explanation
    private String riskLevel; // "Low", "Medium", "High"
}