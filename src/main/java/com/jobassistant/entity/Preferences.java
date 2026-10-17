package com.jobassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preferences for the candidate — what they want in their next role.
 * One-to-one with CandidateProfile.
 */
@Entity
@Table(name = "preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private CandidateProfile profile;

    private String role;

    private String location;

    private Boolean remoteOk;

    private String experienceLevel; // "ENTRY", "MID", "SENIOR", etc.
}