package com.jobassistant.service;

import com.jobassistant.dto.InterviewPreparationRequest;
import com.jobassistant.dto.InterviewPreparationResponse;
import com.jobassistant.entity.InterviewPreparation;
import com.jobassistant.entity.Resume;
import com.jobassistant.entity.User;
import com.jobassistant.repository.InterviewPreparationRepository;
import com.jobassistant.repository.ResumeRepository;
import com.jobassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewPreparationService {

    private final InterviewPreparationRepository repo;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepo;
    private final ResumeRepository resumeRepo;

    public InterviewPreparationService(InterviewPreparationRepository repo, CurrentUserService currentUserService,
                                       UserRepository userRepo, ResumeRepository resumeRepo) {
        this.repo = repo; this.currentUserService = currentUserService; this.userRepo = userRepo; this.resumeRepo = resumeRepo;
    }

    @Transactional
    public InterviewPreparationResponse create(InterviewPreparationRequest req) {
        Long userId = currentUserService.getCurrentUserId();
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        InterviewPreparation p = InterviewPreparation.builder()
                .user(user)
                .suggestedQuestions(req.getSuggestedQuestions())
                .tips(req.getTips())
                .build();
        if (req.getResumeId() != null) {
            Resume r = resumeRepo.findById(req.getResumeId()).orElseThrow(() -> new RuntimeException("Resume not found"));
            p.setResume(r);
        }
        InterviewPreparation saved = repo.save(p);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewPreparationResponse> listForUser() {
        Long userId = currentUserService.getCurrentUserId();
        return repo.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InterviewPreparationResponse getById(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        InterviewPreparation p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Interview preparation not found"));
        return toResponse(p);
    }

    @Transactional
    public InterviewPreparationResponse update(Long id, InterviewPreparationRequest req) {
        Long userId = currentUserService.getCurrentUserId();
        InterviewPreparation p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Interview preparation not found"));
        if (req.getSuggestedQuestions() != null) p.setSuggestedQuestions(req.getSuggestedQuestions());
        if (req.getTips() != null) p.setTips(req.getTips());
        return toResponse(repo.save(p));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        InterviewPreparation p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Interview preparation not found"));
        repo.delete(p);
    }

    private InterviewPreparationResponse toResponse(InterviewPreparation p) {
        return new InterviewPreparationResponse(p.getId(),
                p.getResume() != null ? p.getResume().getId() : null,
                p.getJob() != null ? p.getJob().getId() : null,
                p.getSuggestedQuestions(), p.getTips());
    }
}
