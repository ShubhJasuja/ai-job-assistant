package com.jobassistant.service;

import com.jobassistant.entity.Job;
import java.util.List;

/**
 * Job-source abstraction for permitted real or manually-curated ingestion.
 * Implementations must handle rate limits, partial failures, and normalization.
 */
public interface JobSource {
    String getSourceName();
    List<Job> fetch();
}
