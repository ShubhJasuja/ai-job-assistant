package com.jobassistant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_drafts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String destinationUrl;

    @Column(columnDefinition = "TEXT")
    private String proposedFields; // JSON-style mapping

    private String unresolvedFields;

    private Boolean approved = false;

    @Column(name = "version_hash", length = 64)
    private String versionHash; // content-based version identifier for approval binding

    private LocalDateTime approvalTimestamp;

    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
