package com.jobassistant.service;

import com.jobassistant.entity.*;
import com.jobassistant.enums.RemoteStatus;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure deterministic matching engine.
 *
 * Scoring components and their weights are configured in:
 *   src/main/resources/matching.properties
 *
 * Algorithm:
 * 1. Skill overlap       → Jaccard similarity (intersection / union)
 * 2. Experience match    → candidateYears ∈ [jobMin, jobMax]
 * 3. Location match      → exact, alias, or country match
 * 4. Education match     → degreeLevel ≥ job minimum
 * 5. Remote preference   → compatible with job's remote status
 * 6. Role title match    → keyword overlap in titles
 *
 * Scores are weighted and summed to produce 0–100.
 */
@Component
public class MatchingEngine {

    // City alias map (Pune aliases, Mumbai aliases, etc.)
    private static final Map<String, Set<String>> CITY_ALIASES = Map.of(
            "pune", Set.of("pimpri", "chinchwad", "pune"),
            "mumbai", Set.of("bombay", "mumbai", "thane", "navi mumbai"),
            "bangalore", Set.of("bangalore", "bengaluru", "blr"),
            "delhi", Set.of("delhi", "new delhi", "noida", "gurgaon")
    );

    private final MatchingProperties props;

    public MatchingEngine(MatchingProperties props) {
        this.props = props;
    }

    /**
     * Computes a JobMatch for a single job against a profile.
     * Returns score 0–100 and a textual explanation.
     */
    public JobMatch compute(User user, CandidateProfile profile, Job job) {
        int score = 0;
        StringBuilder explanation = new StringBuilder();
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        // --- 1. Skills (40%) ---
        Set<String> candidateSet = safeList(profile.getSkills()).stream()
                .map(Skill::getName)
                .filter(Objects::nonNull)
                .map(MatchingEngine::canonicalSkill)
                .collect(Collectors.toSet());
        List<String> jobSkillNames = safeList(job.getJobSkills()).stream()
                .map(JobSkill::getSkill_name)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<String> matched = new LinkedHashSet<>();
        for (String jobSkill : jobSkillNames) {
            if (candidateSet.contains(canonicalSkill(jobSkill))) {
                matched.add(jobSkill);
            } else {
                missingSkills.add(jobSkill);
            }
        }

        double skillRatio = jobSkillNames.isEmpty()
                ? 1.0
                : (double) matched.size() / jobSkillNames.size();
        int skillScore = (int) Math.round(skillRatio * props.getWeightSkills());
        score += skillScore;

        matchedSkills.addAll(matched);
        explanation.append(String.format("Skills: %d of %d matched (%d/%d pts). ",
                matched.size(), jobSkillNames.size(), skillScore, props.getWeightSkills()));
        if (!matchedSkills.isEmpty()) {
            explanation.append("Matches: ").append(String.join(", ", matchedSkills)).append(". ");
        }
        if (!missingSkills.isEmpty()) {
            explanation.append("Skills to build: ").append(String.join(", ", missingSkills)).append(". ");
        }

        // --- 2. Experience (20%) ---
        int candidateYears = estimateCandidateYears(profile);
        int[] expRange = parseExperienceRange(job.getEmploymentType(), job.getTitle());
        int jobMin = expRange[0];
        int jobMax = expRange[1];

        int expScore = 0;
        if (candidateYears >= jobMin && candidateYears <= jobMax) {
            expScore = props.getWeightExperience();
            explanation.append("Experience: fits range (").append(expScore).append("/").append(props.getWeightExperience()).append(" pts). ");
        } else {
            explanation.append("Experience: out of range. ");
        }
        score += expScore;

        // --- 3. Location (15%) ---
        int locScore = computeLocationScore(
                preferredLocation(profile),
                job.getLocation()
        );
        score += locScore;
        explanation.append("Location: ").append(locScore).append("/").append(props.getWeightLocation()).append(" pts. ");

        // --- 4. Education (10%) ---
        int eduScore = computeEducationScore(profile.getEducations());
        score += eduScore;
        explanation.append("Education: ").append(eduScore).append("/").append(props.getWeightEducation()).append(" pts. ");

        // --- 5. Remote preference (10%) ---
        int remoteScore = computeRemoteScore(
                preferredRemote(profile),
                job.getRemoteStatus()
        );
        score += remoteScore;
        explanation.append("Remote: ").append(remoteScore).append("/").append(props.getWeightRemote()).append(" pts. ");

        // --- 6. Role title match (5%) ---
        int roleScore = computeRoleScore(
                preferredRole(profile),
                job.getTitle()
        );
        score += roleScore;
        explanation.append("Role: ").append(roleScore).append("/").append(props.getWeightRole()).append(" pts. ");

        // Clamp to 0–100
        score = Math.max(0, Math.min(100, score));

        return JobMatch.builder()
                .user(user)
                .job(job)
                .score(score)
                .explanation(explanation.toString().trim())
                .matchedSkills(String.join(",", matchedSkills))
                .missingSkills(String.join(",", missingSkills))
                .calculatedAt(java.time.LocalDateTime.now())
                .build();
    }

    // ---- Private helpers ----

    private static String canonicalSkill(String skill) {
        String normalized = skill.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "postgres", "postgresql" -> "sql";
            case "react.js" -> "react";
            case "nodejs" -> "node.js";
            case "spring boot", "hibernate" -> "spring";
            default -> normalized;
        };
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int estimateCandidateYears(CandidateProfile profile) {
        int maxExp = 0;
        for (Experience exp : safeList(profile.getExperiences())) {
            if (exp.getStartDate() == null) continue;
            var end = exp.getEndDate() != null ? exp.getEndDate() : java.time.LocalDate.now();
            int years = Period.between(exp.getStartDate(), end).getYears();
            maxExp = Math.max(maxExp, years);
        }
        if (maxExp == 0 && profile.getExperienceLevel() != null) {
            int[] representatives = {0, 2, 4, 7, 12};
            int level = profile.getExperienceLevel();
            if (level >= 0 && level < representatives.length) return representatives[level];
        }
        return maxExp;
    }

    private String preferredLocation(CandidateProfile profile) {
        if (profile.getPreferences() != null && profile.getPreferences().getLocation() != null) {
            return profile.getPreferences().getLocation();
        }
        String locations = profile.getPreferredLocations();
        return locations == null ? null : locations.split(",")[0].trim();
    }

    private Boolean preferredRemote(CandidateProfile profile) {
        if (profile.getPreferences() != null && profile.getPreferences().getRemoteOk() != null) {
            return profile.getPreferences().getRemoteOk();
        }
        return profile.getRemotePreference();
    }

    private String preferredRole(CandidateProfile profile) {
        if (profile.getPreferences() != null && profile.getPreferences().getRole() != null) {
            return profile.getPreferences().getRole();
        }
        String roles = profile.getPreferredRoles();
        return roles == null ? null : roles.split(",")[0].trim();
    }

    private int[] parseExperienceRange(String employmentType, String title) {
        // Heuristic: look for "intern" or "fresher" or years in title
        String lower = (title + " " + employmentType).toLowerCase();
        if (lower.contains("intern") || lower.contains("fresher") || lower.contains("0-1")) {
            return new int[]{0, 2}; // 0-1 years or 0-2 years
        }
        if (lower.contains("junior") || lower.contains("1-3")) {
            return new int[]{1, 3};
        }
        if (lower.contains("senior") || lower.contains("3-5")) {
            return new int[]{3, 5};
        }
        if (lower.contains("lead") || lower.contains("principal")) {
            return new int[]{5, 10};
        }
        return new int[]{0, 99}; // no constraint
    }

    private int computeLocationScore(String preferredLocation, String jobLocation) {
        if (preferredLocation == null || preferredLocation.trim().isEmpty() || "any".equalsIgnoreCase(preferredLocation)) {
            return props.getWeightLocation(); // no preference → full score
        }
        if (jobLocation == null) return 0;

        String pref = preferredLocation.toLowerCase(Locale.ROOT).trim();
        String jobLoc = jobLocation.toLowerCase(Locale.ROOT).trim();

        if (pref.contains(jobLoc) || jobLoc.contains(pref)) {
            return props.getWeightLocation();
        }

        // Check aliases
        for (Map.Entry<String, Set<String>> entry : CITY_ALIASES.entrySet()) {
            boolean preferredMatches = entry.getValue().stream().anyMatch(pref::contains);
            boolean jobMatches = entry.getValue().stream().anyMatch(jobLoc::contains);
            if (preferredMatches && jobMatches) {
                return props.getWeightLocation();
            }
        }
        return 0;
    }

    private int computeEducationScore(List<Education> educations) {
        if (educations.isEmpty()) return 0;
        // Simple heuristic: any degree → pass
        Education highest = educations.get(0);
        for (Education edu : educations) {
            if (edu.getDegree() != null && edu.getDegree().equals("PhD")) highest = edu;
        }
        return props.getWeightEducation(); // full points if any degree present
    }

    private int computeRemoteScore(Boolean remoteOk, RemoteStatus jobRemote) {
        if (remoteOk == null) return props.getWeightRemote(); // no preference
        if (jobRemote == null) return props.getWeightRemote(); // source did not disclose remote status
        boolean jobIsRemote = jobRemote == RemoteStatus.REMOTE || jobRemote == RemoteStatus.HYBRID;
        if (remoteOk && jobIsRemote) return props.getWeightRemote();
        if (!remoteOk && jobRemote == RemoteStatus.ON_SITE) return props.getWeightRemote();
        return 0;
    }

    private int computeRoleScore(String preferredRole, String jobTitle) {
        if (preferredRole == null || preferredRole.trim().isEmpty()) return 0;
        String role = preferredRole.toLowerCase(Locale.ROOT);
        String title = jobTitle == null ? "" : jobTitle.toLowerCase(Locale.ROOT);
        // Check if any keyword from the role appears in the title
        String[] keywords = role.split("[, \\+]+");
        for (String kw : keywords) {
            if (!kw.isEmpty() && title.contains(kw)) {
                return props.getWeightRole();
            }
        }
        return 0;
    }
}