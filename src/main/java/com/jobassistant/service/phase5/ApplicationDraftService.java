package com.jobassistant.service.phase5;

import com.jobassistant.entity.ApplicationDraft;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ApplicationDraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ApplicationDraftService {

    private final ApplicationDraftRepository repo;

    public ApplicationDraftService(ApplicationDraftRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ApplicationDraft saveDraft(ApplicationDraft draft) {
        draft.setStatus("READY");
        draft.setUpdatedAt(LocalDateTime.now());
        return repo.save(draft);
    }

    // 10. Explicit approval tied to exact version/hash
    @Transactional
    public ApplicationDraft approve(Long id, Long userId, String versionHash) {
        Optional<ApplicationDraft> opt = repo.findByIdAndUserId(id, userId);
        if (opt.isEmpty()) throw new RuntimeException("Draft not found or not owned");
        ApplicationDraft d = opt.get();
        if (!versionHash.equals(d.getVersionHash())) {
            throw new RuntimeException("Approval invalidated: content changed");
        }
        d.setApproved(true);
        d.setApprovalTimestamp(LocalDateTime.now());
        d.setStatus("READY");
        return repo.save(d);
    }

    // 11. Invalidate when submission-relevant content changes
    @Transactional
    public ApplicationDraft invalidateIfChanged(Long id, Long userId, String newHash) {
        Optional<ApplicationDraft> opt = repo.findByIdAndUserId(id, userId);
        if (opt.isPresent()) {
            ApplicationDraft d = opt.get();
            if (!newHash.equals(d.getVersionHash())) {
                d.setApproved(false);
                d.setApprovalTimestamp(null);
                d.setStatus("NEEDS_REVIEW");
                return repo.save(d);
            }
        }
        return null;
    }
}
