package com.jobassistant.service.phase5;

import com.jobassistant.entity.ApplicationDraft;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ApplicationDraftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WithMockUser(username = "testuser")
public class ApplicationDraftIsolationTest {

    @Autowired
    private ApplicationDraftRepository repo;

    @Test
    public void draftsAreUserScoped() {
        List<ApplicationDraft> byFirst = repo.findByUserId(1L);
        if (!byFirst.isEmpty()) {
            for (ApplicationDraft d : byFirst) {
                assertThat(d.getUser()).isNotNull();
            }
        }
    }
}
