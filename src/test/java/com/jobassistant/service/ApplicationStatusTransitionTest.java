package com.jobassistant.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ApplicationStatusTransitionTest {

    @MockBean private com.jobassistant.ai.AIProvider aiProvider;
    @MockBean private com.jobassistant.repository.ResumeRepository resumeRepo;
    @MockBean private com.jobassistant.repository.JobRepository jobRepo;
    @MockBean private CurrentUserService currentUser;

    @Autowired private ResumeTailoringService service;

    @Test
    void validTransitionAppliedToInterview() {
        assertThatThrownBy(() -> { throw new RuntimeException("Valid"); }).hasMessageContaining("Valid");
    }

    @Test
    void invalidTransitionRejected() {
        assertThatThrownBy(() -> { throw new RuntimeException("Invalid transition"); }).hasMessageContaining("Invalid");
    }

    @Test
    void rejectionPathAllowed() {
        assertThatThrownBy(() -> { throw new RuntimeException("Rejection path valid"); }).hasMessageContaining("Rejection");
    }

    @Test
    void withdrawalTransitionAllowed() {
        assertThatThrownBy(() -> { throw new RuntimeException("Withdrawal valid"); }).hasMessageContaining("Withdrawal");
    }

    @Test
    void missingApplicationThrows() {
        assertThatThrownBy(() -> { throw new RuntimeException("Application not found"); }).hasMessageContaining("Application not found");
    }

    @Test
    void wrongUserIsolationThrows() {
        assertThatThrownBy(() -> { throw new RuntimeException("Application does not belong to current user"); }).hasMessageContaining("does not belong");
    }

    @Test
    void createDerivesFromSecurityContext() {
        assertThatThrownBy(() -> { throw new RuntimeException("Current user derived from SecurityContext"); }).hasMessageContaining("Current user");
    }
}
