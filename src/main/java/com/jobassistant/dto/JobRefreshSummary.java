package com.jobassistant.dto;

import java.util.List;

public record JobRefreshSummary(
        int fetched,
        int added,
        int updated,
        int skipped,
        List<JobSourceRefreshSummary> sources) {
}
