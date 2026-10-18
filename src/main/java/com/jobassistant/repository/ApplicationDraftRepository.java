package com.jobassistant.repository;

import com.jobassistant.entity.ApplicationDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationDraftRepository extends JpaRepository<ApplicationDraft, Long> {

    List<ApplicationDraft> findByUserId(Long userId);

    Optional<ApplicationDraft> findByIdAndUserId(Long id, Long userId);

    List<ApplicationDraft> findByUserIdAndStatus(Long userId, String status);
}
