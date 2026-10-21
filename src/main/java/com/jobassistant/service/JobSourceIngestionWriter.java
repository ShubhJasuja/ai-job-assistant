package com.jobassistant.service;

import com.jobassistant.dto.JobSourceRefreshSummary;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.JobSkill;
import com.jobassistant.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobSourceIngestionWriter {
    private static final Logger logger = LoggerFactory.getLogger(JobSourceIngestionWriter.class);

    private final JobRepository repository;

    public JobSourceIngestionWriter(JobRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public JobSourceRefreshSummary persist(String sourceName, List<Job> fetched) {
        int added = 0;
        int updated = 0;
        int skipped = 0;
        for (Job job : fetched) {
            if (job == null || job.getTitle() == null || job.getTitle().isBlank()
                    || job.getCompany() == null || job.getCompany().isBlank()
                    || job.getSourceJobId() == null || job.getSourceJobId().isBlank()) {
                logger.warn("Skipping incomplete job from source {}", sourceName);
                skipped++;
                continue;
            }
            String source = job.getSource() == null || job.getSource().isBlank()
                    ? sourceName : job.getSource();
            Job existing = repository.findBySourceAndSourceJobId(source, job.getSourceJobId()).orElse(null);
            if (existing == null) {
                job.setSource(source);
                repository.save(job);
                added++;
            } else {
                updateExisting(existing, job);
                repository.save(existing);
                updated++;
            }
        }
        logger.info("Job source {} refresh complete: fetched={}, added={}, updated={}, skipped={}",
                sourceName, fetched.size(), added, updated, skipped);
        return new JobSourceRefreshSummary(sourceName, fetched.size(), added, updated, skipped, true);
    }

    private void updateExisting(Job existing, Job incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setCompany(incoming.getCompany());
        existing.setLocation(incoming.getLocation());
        existing.setRemoteStatus(incoming.getRemoteStatus());
        existing.setEmploymentType(incoming.getEmploymentType());
        existing.setDescription(incoming.getDescription());
        existing.setApplicationUrl(incoming.getApplicationUrl());
        existing.setPostedDate(incoming.getPostedDate());
        existing.setSourceUpdatedAt(incoming.getSourceUpdatedAt());
        existing.setIsActive(true);
        existing.getJobSkills().clear();
        for (JobSkill skill : incoming.getJobSkills()) {
            existing.getJobSkills().add(JobSkill.builder()
                    .job(existing)
                    .skill_name(skill.getSkill_name())
                    .build());
        }
    }
}
