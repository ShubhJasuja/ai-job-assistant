package com.jobassistant.service.phase5;

import com.jobassistant.entity.ApplicationDraft;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ApplicationDraftRepository;
import com.jobassistant.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "testuser")
public class DraftApprovalVersionTest {

    @Autowired
    private ApplicationDraftRepository repo;

    @Autowired
    private ApplicationDraftService service;

    @Autowired
    private UserRepository userRepository;

    private User persistedUser() {
        User u = User.builder()
            .email("phase5-test-" + UUID.randomUUID() + "@example.com")
            .passwordHash("hash")
            .name("Phase5 Tester")
            .build();
        return userRepository.save(u); // user must exist for application_drafts FK
    }

    private ApplicationDraft persistedDraft(User u) {
        ApplicationDraft d = ApplicationDraft.builder()
            .user(u).destinationUrl("https://example.com/app").status("READY")
            .versionHash("v1").approved(false).build();
        return repo.save(d);
    }

    @Test
    public void approvalTiedToExactVersion() {
        User u = persistedUser();
        ApplicationDraft d = persistedDraft(u);
        ApplicationDraft approved = service.approve(d.getId(), u.getId(), "v1");
        assertThat(approved.getApproved()).isTrue();
    }

    @Test
    public void approvalInvalidatedWhenHashChanges() {
        User u = persistedUser();
        ApplicationDraft d = repo.save(ApplicationDraft.builder()
            .user(u).destinationUrl("https://example.com/app").status("READY")
            .versionHash("v1").approved(true).approvalTimestamp(LocalDateTime.now()).build());
        ApplicationDraft changed = service.invalidateIfChanged(d.getId(), u.getId(), "v2");
        assertThat(changed).isNotNull();
        assertThat(changed.getApproved()).isFalse();
    }

    @Test
    public void crossUserAccessBlocked() {
        User u = persistedUser();
        ApplicationDraft d = persistedDraft(u);
        assertThatThrownBy(() -> service.approve(d.getId(), 999L, "v"))
            .isInstanceOf(RuntimeException.class);
    }
}
