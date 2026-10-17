package com.jobassistant.ingestion;

import com.jobassistant.entity.Job;
import java.util.Optional;

/**
 * Abstraction for a job source.
 * Implementations normalize external feed data into the canonical Job model.
 * Duplicate detection is handled at the ingestion orchestration level.
 */
public interface JobProvider {

    /** Stable identifier for this source, e.g. "mock", "rss_hackernews". */
    String getSource();

    /**
     * Fetch normalized job records from this provider.
     */
    Optional<Job> fetchNext();
}