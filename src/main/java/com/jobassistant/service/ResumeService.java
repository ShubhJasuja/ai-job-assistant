package com.jobassistant.service;

import com.jobassistant.dto.ResumeRequest;
import com.jobassistant.dto.ResumeResponse;
import com.jobassistant.entity.Resume;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ResumeRepository;
import com.jobassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepo;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ResumeService(ResumeRepository resumeRepo, CurrentUserService currentUserService,
                         UserRepository userRepository) {
        this.resumeRepo = resumeRepo;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ResumeResponse create(ResumeRequest req) {
        Long userId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new RuntimeException("Title is required");
        }
        Resume r = Resume.builder()
                .user(user)
                .title(req.getTitle())
                .content(req.getContent())
                .isBase(true)
                .build();
        Resume saved = resumeRepo.save(r);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> listForUser() {
        Long userId = currentUserService.getCurrentUserId();
        return resumeRepo.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeResponse getById(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        Resume r = resumeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (r.getUser() == null || !r.getUser().getId().equals(userId)) {
            throw new RuntimeException("Resume does not belong to current user");
        }
        return toResponse(r);
    }

    @Transactional
    public ResumeResponse update(Long id, ResumeRequest req) {
        Long userId = currentUserService.getCurrentUserId();
        Resume r = resumeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (r.getUser() == null || !r.getUser().getId().equals(userId)) {
            throw new RuntimeException("Resume does not belong to current user");
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            r.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            r.setContent(req.getContent());
        }
        Resume saved = resumeRepo.save(r);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        Resume r = resumeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found"));
        if (r.getUser() == null || !r.getUser().getId().equals(userId)) {
            throw new RuntimeException("Resume does not belong to current user");
        }
        resumeRepo.delete(r);
    }

    private ResumeResponse toResponse(Resume r) {
        return new ResumeResponse(r.getId(), r.getTitle(), r.getContent(), r.getIsBase());
    }
}