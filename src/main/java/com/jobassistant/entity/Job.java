package com.jobassistant.entity;

import com.jobassistant.enums.RemoteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A normalized job posting, independent of which source it came from.
 *
 * Deduplication: (source, sourceJobId) is unique — the same job cannot be
 * ingested twice from the same source.
 */
@Entity
@Table(name = "jobs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"source", "source_job_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    private RemoteStatus remoteStatus;

    private String employmentType; // FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "application_url")
    private String applicationUrl;

    /** Source identifier, e.g. "mock", "rss_hackernews". Part of the dedup key. */
    @Column(name = "source", nullable = false)
    private String source;

    /** The job's own ID within its source. Part of the dedup key. */
    @Column(name = "source_job_id", nullable = false)
    private String sourceJobId;

    @Column(name = "posted_date")
    private LocalDate postedDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobSkill> jobSkills = new ArrayList<>();

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        ingestedAt = LocalDateTime.now();
        isActive = Boolean.TRUE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}