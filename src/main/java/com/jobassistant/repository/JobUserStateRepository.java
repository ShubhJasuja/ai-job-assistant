package com.jobassistant.repository;

import com.jobassistant.entity.JobUserState;
import com.jobassistant.enums.JobUserStateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobUserStateRepository extends JpaRepository<JobUserState, Long> {
    Optional<JobUserState> findByUserIdAndJobId(Long userId, Long jobId);
    List<JobUserState> findByUserId(Long userId);
    List<JobUserState> findByUserIdAndState(Long userId, JobUserStateType state);
    void deleteByUserIdAndJobId(Long userId, Long jobId);
}
