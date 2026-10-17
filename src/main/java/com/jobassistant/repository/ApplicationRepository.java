package com.jobassistant.repository;

import com.jobassistant.entity.Application;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.User;
import com.jobassistant.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Application entity.
 * Allows querying applications by user, job, and status.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

    Page<Application> findByUserId(Long userId, Pageable pageable);

    Page<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    List<Application> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId " +
            "ORDER BY a.updatedAt DESC")
    List<Application> findRecentByUserId(@Param("userId") Long userId);
}