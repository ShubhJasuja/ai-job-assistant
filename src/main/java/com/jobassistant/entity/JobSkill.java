package com.jobassistant.entity;

import com.jobassistant.entity.Job;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String skill_name; // normalized skill name (e.g., "Java", "Spring Boot", "PostgreSQL")
}