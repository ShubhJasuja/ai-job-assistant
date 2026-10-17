package com.jobassistant.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of cover letter generation from AI provider.
 * Returns a draft cover letter tailored to the job and candidate profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverLetterResult {
    private String draftCoverLetter; // The generated cover letter
    private String keyPoints; // Summary of what the letter emphasizes
    private boolean approved; // User must approve before use
}