package com.jobassistant.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of resume tailoring from AI provider.
 * Returns a new version of the resume (formatted as markdown or plain text)
 * highlighting relevant experience for the job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeTailoringResult {
    private String originalResume; // The candidate's original resume
    private String tailoredResume; // Modified version optimized for the job
    private String diffExplanation; // Human-readable explanation of changes
    private boolean approved; // User approval required before use
}