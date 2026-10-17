package com.jobassistant.repository;

import com.jobassistant.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CandidateProfile entity.
 */
@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    /**
     * Finds the candidate profile belonging to a specific user.
     * Each user has exactly one profile.
     */
    Optional<CandidateProfile> findByUserId(Long userId);
}