package com.jobassistant.ingestion;

import com.jobassistant.entity.Job;
import com.jobassistant.enums.RemoteStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Simple manual/internal ingestion mechanism.
 * Allows an authenticated user (admin/recruiter) to add a job directly.
 *
 * The source field is "manual" and sourceJobId is user-supplied.
 * Duplicate detection is performed by the orchestrator using (source, sourceJobId).
 */
@Component
public class ManualJobIngestion implements JobProvider {

    @Override
    public String getSource() {
        return "manual";
    }

    @Override
    public Optional<Job> fetchNext() {
        return Optional.empty();
    }

    /**
     * Normalizes a manual job submission into a Job entity for persistence.
     */
    public Optional<Job> ingest(String title, String company, String location,
                                RemoteStatus remoteStatus, String employmentType,
                                String description, String applicationUrl,
                                String sourceJobId, LocalDate postedDate,
                                LocalDate deadline) {
        if (title == null || title.isBlank() || company == null || company.isBlank()) {
            return Optional.empty();
        }
        Job job = Job.builder()
                .title(title.trim())
                .company(company.trim())
                .location(location != null ? location.trim() : "")
                .remoteStatus(remoteStatus)
                .employmentType(employmentType)
                .description(description)
                .applicationUrl(applicationUrl)
                .source(getSource())
                .sourceJobId(sourceJobId != null ? sourceJobId.trim() : title.trim().toLowerCase() + "-" + company.trim().toLowerCase())
                .postedDate(postedDate)
                .deadline(deadline)
                .build();
        return Optional.of(job);
    }
}