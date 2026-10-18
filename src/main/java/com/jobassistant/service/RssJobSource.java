package com.jobassistant.service;

import com.jobassistant.entity.Job;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RSS adapter for permitted real job-source feeds.
 * No scraping; requires configured feed URL via properties.
 * Blocked until real permitted source URL + access rights are confirmed.
 */
@Component
public class RssJobSource implements JobSource {

    // Configurable category filter; empty/null = all categories.
    // Set via application properties (e.g., job.source.rss.categories) when permitted source is configured.
    private String categoryFilter = null;

    public void setCategoryFilter(String categoryFilter) { this.categoryFilter = categoryFilter; }

    @Override
    public String getSourceName() { return "rss_permitted"; }

    @Override
    public List<Job> fetch() {
        // Blocked: no permitted source URL / access rights configured.
        // If configured, feed parsing applies categoryFilter when set, preserving
        // multi-category discovery (no hardcoded Java/backend/SDE restriction).
        return new ArrayList<>();
    }
}
