package com.jobassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The result of the matching engine — how well a job fits a user.
 * Created automatically when the engine runs (not by user action).
 */
@Entity
@Table(name = "job_matches", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "job_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Score out of 100
    private int score;

    @Column(columnDefinition = "TEXT")
    private String explanation; // human-readable explanation of why this score

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills; // comma-separated

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills; // comma-separated

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    void prePersist() {
        calculatedAt = LocalDateTime.now();
    }
}