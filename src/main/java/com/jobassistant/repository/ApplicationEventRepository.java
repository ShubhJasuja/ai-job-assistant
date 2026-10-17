package com.jobassistant.repository;

import com.jobassistant.entity.ApplicationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {

    Page<ApplicationEvent> findByApplicationId(Long applicationId, Pageable pageable);

    @Query("SELECT e FROM ApplicationEvent e WHERE e.application.user.id = :userId " +
            "ORDER BY e.createdAt DESC")
    List<ApplicationEvent> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}