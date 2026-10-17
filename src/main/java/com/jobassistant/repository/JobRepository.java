package com.jobassistant.repository;

import com.jobassistant.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Job entity.
 * Dedup key: (source, sourceJobId).
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findBySourceAndSourceJobId(String source, String sourceJobId);

    java.util.List<Job> findByIsActive(Boolean isActive);
}