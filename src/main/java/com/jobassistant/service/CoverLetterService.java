package com.jobassistant.service;

import com.jobassistant.ai.CoverLetterResult;
import com.jobassistant.dto.CoverLetterRequest;
import com.jobassistant.dto.CoverLetterResponse;
import com.jobassistant.entity.CoverLetter;
import com.jobassistant.entity.User;
import com.jobassistant.repository.CoverLetterRepository;
import com.jobassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoverLetterService {

    private final CoverLetterRepository repo;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepo;
    private final com.jobassistant.ai.AIProvider aiProvider;

    public CoverLetterService(CoverLetterRepository repo, CurrentUserService currentUserService,
                              UserRepository userRepo, com.jobassistant.ai.AIProvider aiProvider) {
        this.repo = repo; this.currentUserService = currentUserService;
        this.userRepo = userRepo; this.aiProvider = aiProvider;
    }

    @Transactional
    public CoverLetterResponse create(CoverLetterRequest req) {
        Long userId = currentUserService.getCurrentUserId();
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        CoverLetterResult aiResult = aiProvider.generateCoverLetter(null, null);

        CoverLetter cl = CoverLetter.builder()
                .user(user)
                .draftText(aiResult.getDraftCoverLetter())
                .keyPoints(aiResult.getKeyPoints())
                .approved(false)
                .build();
        CoverLetter saved = repo.save(cl);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CoverLetterResponse> listForUser() {
        Long userId = currentUserService.getCurrentUserId();
        return repo.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CoverLetterResponse approve(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        CoverLetter cl = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Cover letter not found"));
        cl.setApproved(true);
        return toResponse(repo.save(cl));
    }

    private CoverLetterResponse toResponse(CoverLetter cl) {
        return new CoverLetterResponse(cl.getId(), cl.getDraftText(), cl.getKeyPoints(), cl.getApproved());
    }
}
