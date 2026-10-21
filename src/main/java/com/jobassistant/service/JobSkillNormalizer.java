package com.jobassistant.service;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.JobSkill;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class JobSkillNormalizer {
    private static final List<String> KNOWN_SKILLS = List.of(
            "TypeScript", "JavaScript", "PostgreSQL", "Kubernetes", "Spring Boot", "Machine Learning",
            "Data Analysis", "Node.js", "React.js", "C++", "C#", "Python", "Java", "React", "Angular",
            "Vue", "SQL", "MySQL", "MongoDB", "Redis", "Docker", "AWS", "Azure", "GCP", "Terraform",
            "GraphQL", "REST", "HTML", "CSS", "Figma", "Excel", "Tableau", "Power BI", "Pandas",
            "PyTorch", "TensorFlow", "Git", "Linux", "Agile", "Communication", "Leadership"
    );

    public List<String> normalize(Collection<String> supplied, String title, String description) {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        if (supplied != null) {
            for (String skill : supplied) {
                if (skill != null && !skill.isBlank()) {
                    String clean = skill.trim().replaceAll("\\s+", " ");
                    normalized.putIfAbsent(clean.toLowerCase(Locale.ROOT), clean);
                }
            }
        }

        String searchable = ((title == null ? "" : title) + " " + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);
        for (String skill : KNOWN_SKILLS) {
            if (Pattern.compile("(?<![\\p{Alnum}])" + Pattern.quote(skill.toLowerCase(Locale.ROOT))
                    + "(?![\\p{Alnum}])").matcher(searchable).find()) {
                normalized.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
            }
        }
        return List.copyOf(normalized.values());
    }

    public void apply(Job job, Collection<String> skills, String title, String description) {
        List<JobSkill> normalized = normalize(skills, title, description).stream()
                .map(name -> JobSkill.builder().job(job).skill_name(name).build())
                .toList();
        job.getJobSkills().clear();
        job.getJobSkills().addAll(normalized);
    }
}
