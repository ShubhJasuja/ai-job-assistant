package com.jobassistant.dto;

import com.jobassistant.enums.RemoteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Request for manually adding a job via the API.
 */
@Data
public class JobCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Company is required")
    private String company;

    private String location;

    private RemoteStatus remoteStatus;

    private String employmentType;

    private String description;

    private String applicationUrl;

    private String sourceJobId;

    private LocalDate postedDate;

    private LocalDate deadline;

    private List<String> skills;
}