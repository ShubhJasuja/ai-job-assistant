package com.jobassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating/updating a candidate profile.
 * Every user has exactly one profile (enforced by User 1:1 mapping).
 */
public class CandidateProfileRequest {

    @NotBlank(message = "Headline is required")
    @Size(max = 255, message = "Headline too long")
    private String headline;

    @Size(max = 5000, message = "Summary too long")
    private String summary;

    private String preferredRoles;
    private String preferredLocations;
    private Boolean remotePreference;
    private Integer experienceLevel;

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPreferredRoles() { return preferredRoles; }
    public void setPreferredRoles(String preferredRoles) { this.preferredRoles = preferredRoles; }
    public String getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(String preferredLocations) { this.preferredLocations = preferredLocations; }
    public Boolean getRemotePreference() { return remotePreference; }
    public void setRemotePreference(Boolean remotePreference) { this.remotePreference = remotePreference; }
    public Integer getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(Integer experienceLevel) { this.experienceLevel = experienceLevel; }
}