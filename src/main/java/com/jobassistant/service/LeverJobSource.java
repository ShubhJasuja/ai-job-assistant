package com.jobassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LeverJobSource implements JobSource {
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 100;
    private static final Logger logger = LoggerFactory.getLogger(LeverJobSource.class);
    private static final Pattern SITE_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private final JobSourceProperties properties;
    private final JobSkillNormalizer skills;
    private final RestClient client = JobSourceHttpClient.create();
    private final ObjectMapper mapper;

    public LeverJobSource(JobSourceProperties properties, JobSkillNormalizer skills, ObjectMapper mapper) {
        this.properties = properties;
        this.skills = skills;
        this.mapper = mapper;
    }

    @Override
    public String getSourceName() { return "lever"; }

    @Override
    public List<Job> fetch() {
        List<Job> jobs = new ArrayList<>();
        int successfulSites = 0;
        int failedSites = 0;
        for (String configuredSite : properties.getLeverSites()) {
            String[] siteConfig = configuredSite.split("=", 2);
            String siteId = siteConfig[0].trim();
            String company = siteConfig.length == 2 ? siteConfig[1].trim() : siteId;
            if (!SITE_ID.matcher(siteId).matches() || company.isBlank()) {
                logger.warn("Skipping invalid Lever site configuration");
                continue;
            }
            int skip = 0;
            boolean successfulSite = false;
            boolean failureCounted = false;
            try {
                for (int page = 0; page < MAX_PAGES; page++) {
                    java.net.URI uri = UriComponentsBuilder
                            .fromUriString("https://api.lever.co/v0/postings/{site}")
                            .queryParam("skip", skip)
                            .queryParam("limit", PAGE_SIZE)
                            .queryParam("mode", "json")
                            .buildAndExpand(siteId)
                            .toUri();
                    String body = JobSourceHttpClient.get(client, uri);
                    JsonNode postings = mapper.readTree(body);
                    if (postings == null || !postings.isArray()) {
                        logger.warn("Lever returned an invalid postings page for site {}", siteId);
                        break;
                    }
                    successfulSite = true;
                    for (int index = 0; index < postings.size(); index++) {
                        try {
                            Job job = toJob(postings.get(index), siteId, company);
                            if (job != null) jobs.add(job);
                        } catch (RuntimeException exception) {
                            logger.warn("Skipping malformed Lever listing for site {} at index {} ({})",
                                    siteId, skip + index, exception.getClass().getSimpleName());
                        }
                    }
                    skip += postings.size();
                    if (postings.size() < PAGE_SIZE) break;
                }
                if (skip >= PAGE_SIZE * MAX_PAGES) {
                    logger.warn("Lever pagination limit reached for site {}; remaining listings were not fetched", siteId);
                }
            } catch (Exception exception) {
                failedSites++;
                failureCounted = true;
                logger.warn("Lever site fetch failed for site {} ({})",
                        siteId, failureType(exception));
            }
            if (successfulSite) successfulSites++;
            else if (!failureCounted) failedSites++;
        }
        if (!properties.getLeverSites().isEmpty() && successfulSites == 0 && failedSites > 0) {
            throw new IllegalStateException("No configured Lever site could be fetched");
        }
        return jobs;
    }

    private Job toJob(JsonNode posting, String siteId, String company) {
        String id = text(posting, "id");
        String title = text(posting, "text");
        if (id.isBlank() || title.isBlank()) return null;
        JsonNode categories = posting.path("categories");
        String location = text(categories, "location");
        String description = JobText.cleanHtml(text(posting, "descriptionPlain"));
        if (description.isBlank()) description = JobText.cleanHtml(text(posting, "description"));
        if (description.isBlank()) description = JobText.cleanHtml(text(posting, "additional"));
        String employment = text(categories, "commitment");
        long createdAt = posting.path("createdAt").asLong(0);
        LocalDateTime sourceUpdatedAt = parseTimestamp(text(posting, "updatedAt"));
        Job job = Job.builder()
                .title(title)
                .company(company)
                .location(location)
                .remoteStatus(JobText.remoteStatus(location + " " + description))
                .employmentType(employment.isBlank() ? JobText.employmentType(title, description) : employment)
                .description(description)
                .applicationUrl(text(posting, "hostedUrl"))
                .source(getSourceName())
                .sourceJobId(siteId + ":" + id)
                .postedDate(createdAt > 0 ? Instant.ofEpochMilli(createdAt).atZone(java.time.ZoneOffset.UTC).toLocalDate() : null)
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
        skills.apply(job, List.of(), title, description);
        return job;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value.isBlank()) return null;
        try {
            return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
        } catch (RuntimeException exception) {
            try {
                return OffsetDateTime.parse(value).toInstant()
                        .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    private String failureType(Exception exception) {
        if (exception instanceof org.springframework.web.client.RestClientResponseException responseException) {
            return "HTTP " + responseException.getStatusCode().value();
        }
        return exception.getClass().getSimpleName();
    }
}
