package com.jobassistant.repository;

import com.jobassistant.entity.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
    List<CoverLetter> findByUserId(Long userId);
    Optional<CoverLetter> findByIdAndUserId(Long id, Long userId);
}
