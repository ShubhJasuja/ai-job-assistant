package com.jobassistant.dto;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.JobMatch;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDiscoveryResponse {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String remoteStatus;
    private String employmentType;
    private String description;
    private String applicationUrl;
    private String source;
    private LocalDate postedDate;
    private LocalDateTime ingestedAt;
    private Boolean isActive;
    private List<String> skills;
    private Integer matchScore;
    private String explanation;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String userState;

    public static JobDiscoveryResponse from(Job job, JobMatch match, String userState) {
        JobDiscoveryResponse response = new JobDiscoveryResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setCompany(job.getCompany());
        response.setLocation(job.getLocation());
        response.setRemoteStatus(job.getRemoteStatus() != null ? job.getRemoteStatus().name() : null);
        response.setEmploymentType(job.getEmploymentType());
        response.setDescription(job.getDescription());
        response.setApplicationUrl(job.getApplicationUrl());
        response.setSource(job.getSource());
        response.setPostedDate(job.getPostedDate());
        response.setIngestedAt(job.getIngestedAt());
        response.setIsActive(job.getIsActive());
        response.setSkills(job.getJobSkills().stream().map(skill -> skill.getSkill_name()).distinct().toList());
        response.setMatchScore(match != null ? match.getScore() : null);
        response.setExplanation(match != null ? match.getExplanation() : null);
        response.setMatchedSkills(match != null ? splitSkills(match.getMatchedSkills()) : List.of());
        response.setMissingSkills(match != null ? splitSkills(match.getMissingSkills()) : List.of());
        response.setUserState(userState);
        return response;
    }

    private static List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) return List.of();
        return java.util.Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isEmpty())
                .distinct()
                .toList();
    }
}
