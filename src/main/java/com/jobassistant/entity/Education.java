package com.jobassistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Represents an educational qualification in the candidate's profile.
 * One profile can have multiple education entries (e.g., B.Tech, M.Sc).
 */
@Entity
@Table(name = "educations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CandidateProfile profile;

    private String degree;

    private String university;

    private String fieldOfStudy;

    private LocalDate graduationDate;

    private Double gpa; // optional

    private String description; // optional: honours, awards, etc.
}