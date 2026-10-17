package com.jobassistant.repository;

import com.jobassistant.entity.JobMatch;
import com.jobassistant.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    Optional<JobMatch> findByUserIdAndJobId(Long userId, Long jobId);

    Page<JobMatch> findByUserIdOrderByScoreDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM JobMatch jm WHERE jm.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);

    @Modifying
    @Query("DELETE FROM JobMatch jm WHERE jm.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}