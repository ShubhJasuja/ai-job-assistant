package com.jobassistant.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "job-sources.schedule-enabled", havingValue = "true", matchIfMissing = true)
public class JobSourceRefreshSchedule {
    private final JobRefreshService refreshService;

    public JobSourceRefreshSchedule(JobRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(
            initialDelayString = "${job-sources.initial-delay-ms:60000}",
            fixedDelayString = "${job-sources.refresh-interval-ms:21600000}")
    public void refreshConfiguredSources() {
        refreshService.refresh();
    }
}
