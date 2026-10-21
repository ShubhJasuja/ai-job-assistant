package com.jobassistant.service;

import com.jobassistant.dto.JobRefreshSummary;
import com.jobassistant.dto.JobSourceRefreshSummary;
import com.jobassistant.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobRefreshService {
    private static final Logger logger = LoggerFactory.getLogger(JobRefreshService.class);

    private final List<JobSource> sources;
    private final JobSourceIngestionWriter writer;

    public JobRefreshService(List<JobSource> sources, JobSourceIngestionWriter writer) {
        this.sources = sources;
        this.writer = writer;
    }

    public JobRefreshSummary refresh() {
        int fetchedCount = 0;
        int addedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<JobSourceRefreshSummary> sourceSummaries = new ArrayList<>();
        for (JobSource source : sources) {
            List<Job> fetched;
            try {
                fetched = source.fetch();
            } catch (RuntimeException exception) {
                logger.error("Job source {} failed ({}); continuing with remaining sources",
                        source.getSourceName(), failureType(exception));
                sourceSummaries.add(new JobSourceRefreshSummary(
                        source.getSourceName(), 0, 0, 0, 0, false));
                continue;
            }
            if (fetched == null) {
                logger.error("Job source {} returned no result (null); continuing with remaining sources",
                        source.getSourceName());
                sourceSummaries.add(new JobSourceRefreshSummary(
                        source.getSourceName(), 0, 0, 0, 0, false));
                continue;
            }
            fetchedCount += fetched.size();
            try {
                JobSourceRefreshSummary sourceSummary = writer.persist(source.getSourceName(), fetched);
                sourceSummaries.add(sourceSummary);
                addedCount += sourceSummary.added();
                updatedCount += sourceSummary.updated();
                skippedCount += sourceSummary.skipped();
            } catch (RuntimeException exception) {
                logger.error("Could not persist listings from source {} ({})",
                        source.getSourceName(), failureType(exception));
                sourceSummaries.add(new JobSourceRefreshSummary(
                        source.getSourceName(), fetched.size(), 0, 0, fetched.size(), false));
                skippedCount += fetched.size();
            }
        }
        return new JobRefreshSummary(fetchedCount, addedCount, updatedCount, skippedCount, sourceSummaries);
    }

    private String failureType(RuntimeException exception) {
        if (exception instanceof org.springframework.web.client.RestClientResponseException responseException) {
            return "HTTP " + responseException.getStatusCode().value();
        }
        return exception.getClass().getSimpleName();
    }
}
