package com.jobassistant.service.phase5;

import com.jobassistant.entity.ApplicationDraft;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ApplicationDraftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@WithMockUser(username = "testuser")
public class DraftApprovalVersionTest {

    @Autowired
    private ApplicationDraftRepository repo;

    @Autowired
    private ApplicationDraftService service;

    @Test
    public void approvalTiedToExactVersion() {
        User u = new User(); u.setId(1L);
        ApplicationDraft d = ApplicationDraft.builder()
            .user(u).destinationUrl("https://example.com/app").status("READY")
            .versionHash("v1").approved(false).build();
        d = repo.save(d);
        ApplicationDraft approved = service.approve(d.getId(), 1L, "v1");
        assertThat(approved.getApproved()).isTrue();
    }

    @Test
    public void approvalInvalidatedWhenHashChanges() {
        User u = new User(); u.setId(1L);
        ApplicationDraft d = ApplicationDraft.builder()
            .user(u).destinationUrl("https://example.com/app").status("READY")
            .versionHash("v1").approved(true).approvalTimestamp(LocalDateTime.now()).build();
        d = repo.save(d);
        ApplicationDraft changed = service.invalidateIfChanged(d.getId(), 1L, "v2");
        assertThat(changed).isNotNull();
        assertThat(changed.getApproved()).isFalse();
    }

    @Test
    public void crossUserAccessBlocked() {
        assertThatThrownBy(() -> service.approve(999L, 2L, "v"))
            .isInstanceOf(RuntimeException.class);
    }
}
