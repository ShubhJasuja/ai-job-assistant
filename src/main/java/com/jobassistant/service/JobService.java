package com.jobassistant.service;

import com.jobassistant.dto.JobCreateRequest;
import com.jobassistant.dto.JobResponse;
import com.jobassistant.entity.Job;
import com.jobassistant.ingestion.ManualJobIngestion;
import com.jobassistant.repository.JobRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for job ingestion and management.
 * Jobs are shared/global data — not owned by a single user.
 * Duplicate detection: same (source, sourceJobId) is rejected.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ManualJobIngestion manualIngestion;

    public JobService(JobRepository jobRepository, ManualJobIngestion manualIngestion) {
        this.jobRepository = jobRepository;
        this.manualIngestion = manualIngestion;
    }

    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        Optional<Job> normalized = manualIngestion.ingest(
                request.getTitle(), request.getCompany(), request.getLocation(),
                request.getRemoteStatus(), request.getEmploymentType(),
                request.getDescription(), request.getApplicationUrl(),
                request.getSourceJobId(), request.getPostedDate(), request.getDeadline());
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Invalid job data");
        }
        Job job = normalized.get();
        // Duplicate detection: same (source, sourceJobId) must not exist
        if (jobRepository.findBySourceAndSourceJobId(job.getSource(), job.getSourceJobId()).isPresent()) {
            throw new IllegalStateException("Duplicate job: same source and sourceJobId already exists");
        }
        Job saved = jobRepository.save(job);
        return JobResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Optional<JobResponse> getJobById(Long id) {
        return jobRepository.findById(id).map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(String keyword, String company, String location,
                                        Boolean remote, String employmentType, Boolean active) {
        List<Job> results = jobRepository.findAll().stream()
                .filter(j -> active == null || j.getIsActive().equals(active))
                .filter(j -> keyword == null || keyword.isBlank()
                        || j.getTitle().toLowerCase().contains(keyword.toLowerCase())
                        || j.getCompany().toLowerCase().contains(keyword.toLowerCase()))
                .filter(j -> company == null || company.isBlank() || j.getCompany().equalsIgnoreCase(company))
                .filter(j -> location == null || location.isBlank() || j.getLocation().equalsIgnoreCase(location))
                .filter(j -> remote == null || j.getRemoteStatus() == null || j.getRemoteStatus().name().equalsIgnoreCase(remote ? "REMOTE" : "ON_SITE"))
                .filter(j -> employmentType == null || employmentType.isBlank() || j.getEmploymentType() == null || j.getEmploymentType().equalsIgnoreCase(employmentType))
                .toList();
        return results.stream().map(JobResponse::from).toList();
    }

    @Transactional
    public JobResponse updateJob(Long id, JobCreateRequest request) {
        Job existing = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (request.getTitle() != null && !request.getTitle().isBlank()) existing.setTitle(request.getTitle().trim());
        if (request.getCompany() != null && !request.getCompany().isBlank()) existing.setCompany(request.getCompany().trim());
        if (request.getLocation() != null) existing.setLocation(request.getLocation().trim());
        if (request.getRemoteStatus() != null) existing.setRemoteStatus(request.getRemoteStatus());
        if (request.getEmploymentType() != null) existing.setEmploymentType(request.getEmploymentType());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getApplicationUrl() != null) existing.setApplicationUrl(request.getApplicationUrl());
        if (request.getDeadline() != null) existing.setDeadline(request.getDeadline());
        Job saved = jobRepository.save(existing);
        return JobResponse.from(saved);
    }

    @Transactional
    public void deactivateJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setIsActive(false);
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(String source, String sourceJobId) {
        return jobRepository.findBySourceAndSourceJobId(source, sourceJobId).isPresent();
    }
}