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

    @Override
    public String getSourceName() { return "rss_permitted"; }

    @Override
    public List<Job> fetch() {
        // Live verification blocked: no permitted source URL configured,
        // no external access rights established, rate-limit policy unknown.
        // Returns empty instead of fabricating listings.
        return new ArrayList<>();
    }
}
