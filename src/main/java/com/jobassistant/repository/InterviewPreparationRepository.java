package com.jobassistant.repository;

import com.jobassistant.entity.InterviewPreparation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewPreparationRepository extends JpaRepository<InterviewPreparation, Long> {
    List<InterviewPreparation> findByUserId(Long userId);
    Optional<InterviewPreparation> findByIdAndUserId(Long id, Long userId);
}
