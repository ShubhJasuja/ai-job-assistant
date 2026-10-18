package com.jobassistant.service;

import com.jobassistant.entity.Job;
import com.jobassistant.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(JobRefreshService.class);

    private final JobRepository repo;
    private final JobSource source;

    public JobRefreshService(JobRepository repo, JobSource source) {
        this.repo = repo; this.source = source;
    }

    @Transactional
    public int refresh() {
        List<Job> fetched = source.fetch();
        int added = 0;
        for (Job j : fetched) {
            if (j.getTitle() == null || j.getTitle().isBlank()) {
                logger.warn("Skipping job with missing title");
                continue;
            }
            boolean exists = repo.findBySourceAndSourceJobId(j.getSource() != null ? j.getSource() : source.getSourceName(), j.getSourceJobId()).isPresent();
            if (!exists) {
                repo.save(j);
                added++;
            } else {
                logger.info("Duplicate prevented: source={} id={}", j.getSource(), j.getSourceJobId());
            }
        }
        logger.info("Refresh complete: fetched={}, new={}, duplicate_skipped={}", fetched.size(), added, fetched.size() - added);
        return added;
    }
}
