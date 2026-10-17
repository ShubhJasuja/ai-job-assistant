package com.jobassistant.entity;

import com.jobassistant.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit record of every status change.
 * Never updated, never deleted — only inserted.
 */
@Entity
@Table(name = "application_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus; // null on creation

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}