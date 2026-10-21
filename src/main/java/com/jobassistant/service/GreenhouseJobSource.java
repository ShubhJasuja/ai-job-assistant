package com.jobassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobassistant.entity.Job;
import com.jobassistant.enums.RemoteStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class GreenhouseJobSource implements JobSource {
    private static final Logger logger = LoggerFactory.getLogger(GreenhouseJobSource.class);
    private static final Pattern BOARD_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private final JobSourceProperties properties;
    private final JobSkillNormalizer skills;
    private final RestClient client = JobSourceHttpClient.create();
    private final ObjectMapper mapper;

    public GreenhouseJobSource(JobSourceProperties properties, JobSkillNormalizer skills, ObjectMapper mapper) {
        this.properties = properties;
        this.skills = skills;
        this.mapper = mapper;
    }

    @Override
    public String getSourceName() { return "greenhouse"; }

    @Override
    public List<Job> fetch() {
        List<Job> jobs = new ArrayList<>();
        int successfulBoards = 0;
        int failedBoards = 0;
        for (String configuredBoard : properties.getGreenhouseBoards()) {
            String[] boardConfig = configuredBoard.split("=", 2);
            String boardId = boardConfig[0].trim();
            String company = boardConfig.length == 2 ? boardConfig[1].trim() : boardId;
            if (!BOARD_ID.matcher(boardId).matches() || company.isBlank()) {
                logger.warn("Skipping invalid Greenhouse board configuration");
                continue;
            }
            try {
                String body = JobSourceHttpClient.get(client,
                        java.net.URI.create("https://boards-api.greenhouse.io/v1/boards/" + boardId + "/jobs?content=true"));
                JsonNode root = mapper.readTree(body);
                JsonNode listings = root.path("jobs");
                if (!listings.isArray()) {
                    failedBoards++;
                    logger.warn("Greenhouse returned an invalid postings list for board {}", boardId);
                    continue;
                }
                successfulBoards++;
                for (int index = 0; index < listings.size(); index++) {
                    try {
                        Job job = toJob(listings.get(index), boardId, company);
                        if (job != null) jobs.add(job);
                    } catch (RuntimeException exception) {
                        logger.warn("Skipping malformed Greenhouse listing for board {} at index {} ({})",
                                boardId, index, exception.getClass().getSimpleName());
                    }
                }
            } catch (Exception exception) {
                failedBoards++;
                logger.warn("Greenhouse board fetch failed for board {} ({})",
                        boardId, failureType(exception));
            }
        }
        if (!properties.getGreenhouseBoards().isEmpty() && successfulBoards == 0 && failedBoards > 0) {
            throw new IllegalStateException("No configured Greenhouse board could be fetched");
        }
        return jobs;
    }

    private Job toJob(JsonNode listing, String boardId, String company) {
        String title = text(listing, "title");
        String id = text(listing, "id");
        if (title.isBlank() || id.isBlank()) return null;
        String location = listing.path("location").path("name").asText("");
        String description = JobText.cleanHtml(text(listing, "content"));
        String applicationUrl = text(listing, "absolute_url");
        LocalDate posted = parseDate(text(listing, "first_published"));
        if (posted == null) posted = parseDate(text(listing, "updated_at"));
        java.time.LocalDateTime sourceUpdatedAt = parseTimestamp(text(listing, "updated_at"));
        Job job = Job.builder()
                .title(title)
                .company(company)
                .location(location)
                .remoteStatus(JobText.remoteStatus(location + " " + description))
                .employmentType(JobText.employmentType(title, description))
                .description(description)
                .applicationUrl(applicationUrl)
                .source(getSourceName())
                .sourceJobId(boardId + ":" + id)
                .postedDate(posted)
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
        skills.apply(job, List.of(), title, description);
        return job;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) return null;
        try {
            return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDate();
        } catch (RuntimeException ignored) {
            try {
                return LocalDate.parse(value.substring(0, 10));
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }

    private java.time.LocalDateTime parseTimestamp(String value) {
        if (value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toInstant()
                    .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
        } catch (RuntimeException exception) {
            try {
                return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
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
