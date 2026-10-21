package com.jobassistant.service;

import com.jobassistant.dto.JobCreateRequest;
import com.jobassistant.dto.JobResponse;
import com.jobassistant.dto.JobSearchPage;
import com.jobassistant.dto.JobSearchSuggestion;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.JobSkill;
import com.jobassistant.ingestion.ManualJobIngestion;
import com.jobassistant.repository.JobRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Service for job ingestion and management.
 * Jobs are shared/global data — not owned by a single user.
 * Duplicate detection: same (source, sourceJobId) is rejected.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ManualJobIngestion manualIngestion;
    private final JobSkillNormalizer skillNormalizer;

    public JobService(JobRepository jobRepository, ManualJobIngestion manualIngestion,
                      JobSkillNormalizer skillNormalizer) {
        this.jobRepository = jobRepository;
        this.manualIngestion = manualIngestion;
        this.skillNormalizer = skillNormalizer;
    }

    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        Optional<Job> normalized = manualIngestion.ingest(
                request.getTitle(), request.getCompany(), request.getLocation(),
                request.getRemoteStatus(), request.getEmploymentType(),
                request.getDescription(), request.getApplicationUrl(),
                request.getSourceJobId(), request.getPostedDate(), request.getDeadline());
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Invalid job data");
        }
        Job job = normalized.get();
        skillNormalizer.apply(job, request.getSkills(), job.getTitle(), job.getDescription());
        // Duplicate detection: same (source, sourceJobId) must not exist
        if (jobRepository.findBySourceAndSourceJobId(job.getSource(), job.getSourceJobId()).isPresent()) {
            throw new IllegalStateException("Duplicate job: same source and sourceJobId already exists");
        }
        Job saved = jobRepository.save(job);
        return JobResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Optional<JobResponse> getJobById(Long id) {
        return jobRepository.findById(id).map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(String keyword, String company, String location,
                                        Boolean remote, String employmentType, Boolean active) {
        return listJobs(keyword, company, location, remote, employmentType, active, null);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(String keyword, String company, String location,
                                       Boolean remote, String employmentType, Boolean active, String skill) {
        return searchJobs(keyword, company, location, remote, employmentType, active, skill)
                .stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobSearchPage<JobResponse> searchPage(
            String keyword, String company, String location, Boolean remote,
            String employmentType, Boolean active, String skill, int offset, int limit) {
        validatePageWindow(offset, limit);
        List<Job> results = searchJobs(keyword, company, location, remote, employmentType, active, skill);
        int fromIndex = Math.min(offset, results.size());
        int toIndex = (int) Math.min((long) fromIndex + limit, results.size());
        List<JobResponse> items = results.subList(fromIndex, toIndex).stream()
                .map(JobResponse::from)
                .toList();
        return new JobSearchPage<>(items, results.size(), toIndex < results.size());
    }

    private void validatePageWindow(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Offset must be non-negative and limit must be between 1 and 100");
        }
    }

    @Transactional(readOnly = true)
    public List<Job> searchJobs(String keyword, String company, String location,
                                Boolean remote, String employmentType, Boolean active, String skill) {
        String normalizedKeyword = normalizeFilter(keyword);
        String normalizedCompany = normalizeFilter(company);
        String normalizedLocation = normalizeFilter(location);
        String normalizedEmploymentType = normalizeFilter(employmentType);
        String normalizedSkill = normalizeFilter(skill);
        List<Job> results = jobRepository.findAll().stream()
                .filter(j -> active == null || Boolean.valueOf(active).equals(j.getIsActive()))
                .filter(j -> normalizedKeyword == null || matchesKeyword(j, normalizedKeyword))
                .filter(j -> normalizedCompany == null || contains(j.getCompany(), normalizedCompany))
                .filter(j -> normalizedLocation == null || contains(j.getLocation(), normalizedLocation))
                .filter(j -> remote == null || (j.getRemoteStatus() != null
                        && (remote
                        ? j.getRemoteStatus() == com.jobassistant.enums.RemoteStatus.REMOTE
                            || j.getRemoteStatus() == com.jobassistant.enums.RemoteStatus.HYBRID
                        : j.getRemoteStatus() == com.jobassistant.enums.RemoteStatus.ON_SITE)))
                .filter(j -> normalizedEmploymentType == null || contains(j.getEmploymentType(), normalizedEmploymentType))
                .filter(j -> normalizedSkill == null || j.getJobSkills().stream()
                        .anyMatch(jobSkill -> contains(jobSkill.getSkill_name(), normalizedSkill)))
                .toList();
        return results;
    }

    @Transactional(readOnly = true)
    public List<JobSearchSuggestion> searchSuggestions(String query, String field) {
        String prefix = normalizeFilter(query);
        if (prefix == null) return List.of();

        String normalizedField = field == null ? "keyword" : field.trim().toLowerCase(Locale.ROOT);
        if (!normalizedField.equals("keyword") && !normalizedField.equals("location")) {
            throw new IllegalArgumentException("Suggestion field must be keyword or location");
        }

        Map<String, SuggestionCount> suggestions = new LinkedHashMap<>();
        for (Job job : jobRepository.findAll()) {
            if (!Boolean.TRUE.equals(job.getIsActive())) continue;
            if (normalizedField.equals("location")) {
                addSuggestion(suggestions, job.getLocation(), "LOCATION", prefix);
                continue;
            }

            addSuggestion(suggestions, job.getTitle(), "ROLE", prefix);
            addSuggestion(suggestions, job.getCompany(), "COMPANY", prefix);
            for (JobSkill jobSkill : job.getJobSkills()) {
                addSuggestion(suggestions, jobSkill.getSkill_name(), "TECHNOLOGY", prefix);
            }
            addSoftwareRoleAlias(suggestions, job.getTitle(), prefix);
        }

        return suggestions.values().stream()
                .sorted(java.util.Comparator.comparingLong(SuggestionCount::matches).reversed()
                        .thenComparing(SuggestionCount::value, String.CASE_INSENSITIVE_ORDER))
                .limit(8)
                .map(suggestion -> new JobSearchSuggestion(
                        suggestion.value(), suggestion.category(), suggestion.matches()))
                .toList();
    }

    private boolean matchesKeyword(Job job, String keyword) {
        if (keyword.length() == 1) {
            return startsWithWord(nullToEmpty(job.getTitle()), keyword)
                    || startsWithWord(nullToEmpty(job.getCompany()), keyword)
                    || startsWithWord(nullToEmpty(job.getLocation()), keyword)
                    || job.getJobSkills().stream()
                    .map(JobSkill::getSkill_name)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(skill -> startsWithWord(skill, keyword));
        }
        String searchable = String.join(" ",
                nullToEmpty(job.getTitle()),
                nullToEmpty(job.getCompany()),
                nullToEmpty(job.getLocation()),
                nullToEmpty(job.getDescription()),
                job.getJobSkills().stream().map(JobSkill::getSkill_name).filter(java.util.Objects::nonNull)
                        .reduce("", (left, right) -> left + " " + right))
                .toLowerCase(Locale.ROOT);
        if (searchable.contains(keyword)) return true;

        String[] terms = keyword.split("\\s+");
        for (String term : terms) {
            if (term.isBlank()) continue;
            boolean termMatches = searchable.contains(term);
            if (!termMatches && isDeveloperEngineerSynonym(term)) {
                termMatches = searchable.contains("developer") || searchable.contains("engineer")
                        || searchable.contains("development");
            }
            if (!termMatches) return false;
        }
        return true;
    }

    private boolean isDeveloperEngineerSynonym(String term) {
        return term.equals("developer") || term.equals("developers") || term.equals("engineer")
                || term.equals("engineers") || term.equals("development") || term.equals("dev");
    }

    private void addSuggestion(Map<String, SuggestionCount> suggestions,
                               String value, String category, String prefix) {
        if (value == null || value.isBlank() || !startsWithWord(value, prefix)) return;
        String key = category + ":" + value.toLowerCase(Locale.ROOT);
        SuggestionCount existing = suggestions.get(key);
        if (existing == null) suggestions.put(key, new SuggestionCount(value.trim(), category, 1));
        else existing.increment();
    }

    private void addSoftwareRoleAlias(Map<String, SuggestionCount> suggestions, String title, String prefix) {
        if (title == null) return;
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        if (!normalizedTitle.contains("software")) return;
        if (normalizedTitle.contains("engineer")) {
            addSuggestion(suggestions, "Software Developer", "ROLE", prefix);
        } else if (normalizedTitle.contains("developer") || normalizedTitle.contains("development")) {
            addSuggestion(suggestions, "Software Engineer", "ROLE", prefix);
        }
    }

    private boolean startsWithWord(String value, String prefix) {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        if (normalizedValue.startsWith(prefix)) return true;
        return java.util.regex.Pattern.compile("[^\\p{Alnum}]+")
                .splitAsStream(normalizedValue)
                .anyMatch(word -> word.startsWith(prefix));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class SuggestionCount {
        private final String value;
        private final String category;
        private long matches;

        private SuggestionCount(String value, String category, long matches) {
            this.value = value;
            this.category = category;
            this.matches = matches;
        }

        String value() { return value; }
        String category() { return category; }
        long matches() { return matches; }
        void increment() { matches++; }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean contains(String value, String fragment) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(fragment);
    }

    @Transactional
    public JobResponse updateJob(Long id, JobCreateRequest request) {
        Job existing = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (request.getTitle() != null && !request.getTitle().isBlank()) existing.setTitle(request.getTitle().trim());
        if (request.getCompany() != null && !request.getCompany().isBlank()) existing.setCompany(request.getCompany().trim());
        if (request.getLocation() != null) existing.setLocation(request.getLocation().trim());
        if (request.getRemoteStatus() != null) existing.setRemoteStatus(request.getRemoteStatus());
        if (request.getEmploymentType() != null) existing.setEmploymentType(request.getEmploymentType());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getApplicationUrl() != null) existing.setApplicationUrl(request.getApplicationUrl());
        if (request.getDeadline() != null) existing.setDeadline(request.getDeadline());
        if (request.getSkills() != null) {
            skillNormalizer.apply(existing, request.getSkills(), existing.getTitle(), existing.getDescription());
        }
        Job saved = jobRepository.save(existing);
        return JobResponse.from(saved);
    }

    @Transactional
    public void deactivateJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setIsActive(false);
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(String source, String sourceJobId) {
        return jobRepository.findBySourceAndSourceJobId(source, sourceJobId).isPresent();
    }
}