package com.jobassistant.dto;

public record JobSourceRefreshSummary(
        String source,
        int fetched,
        int added,
        int updated,
        int skipped,
        boolean successful) {
}
