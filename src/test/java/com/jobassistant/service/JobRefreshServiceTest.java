package com.jobassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class JobRefreshServiceTest {

    @MockBean private com.jobassistant.ai.AIProvider aiProvider;
    @MockBean private com.jobassistant.repository.ResumeRepository resumeRepo;
    @MockBean private com.jobassistant.repository.ApplicationRepository appRepo;

    @Autowired private JobRefreshService service;

    @Test
    void refreshReturnsZeroWhenSourceBlocked() {
        int added = service.refresh();
        assertThat(added).isEqualTo(0); // real source blocked; no fabricated listings
    }
}
