package com.jobassistant.dto;

import com.jobassistant.entity.Application;
import com.jobassistant.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private String applicationUrl;
    private ApplicationStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApplicationResponse from(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setJobId(application.getJob().getId());
        response.setJobTitle(application.getJob().getTitle());
        response.setCompany(application.getJob().getCompany());
        response.setLocation(application.getJob().getLocation());
        response.setApplicationUrl(application.getJob().getApplicationUrl());
        response.setStatus(application.getStatus());
        response.setNotes(application.getNotes());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }
}
