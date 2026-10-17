package com.jobassistant.dto;

import com.jobassistant.entity.Job;
import lombok.Data;

/**
 * Read-only projection for job responses.
 */
@Data
public class JobResponse {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String remoteStatus;
    private String employmentType;
    private String description;
    private String applicationUrl;
    private String source;
    private String sourceJobId;
    private java.time.LocalDate postedDate;
    private java.time.LocalDate deadline;
    private java.time.LocalDateTime ingestedAt;
    private Boolean isActive;
    private java.time.LocalDateTime updatedAt;

    public static JobResponse from(Job job) {
        JobResponse dto = new JobResponse();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setCompany(job.getCompany());
        dto.setLocation(job.getLocation());
        dto.setRemoteStatus(job.getRemoteStatus() != null ? job.getRemoteStatus().name() : null);
        dto.setEmploymentType(job.getEmploymentType());
        dto.setDescription(job.getDescription());
        dto.setApplicationUrl(job.getApplicationUrl());
        dto.setSource(job.getSource());
        dto.setSourceJobId(job.getSourceJobId());
        dto.setPostedDate(job.getPostedDate());
        dto.setDeadline(job.getDeadline());
        dto.setIngestedAt(job.getIngestedAt());
        dto.setIsActive(job.getIsActive());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }
}