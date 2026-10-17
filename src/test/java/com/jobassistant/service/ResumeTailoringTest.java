package com.jobassistant.service;

import com.jobassistant.ai.AIProvider;
import com.jobassistant.ai.ResumeTailoringResult;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.Resume;
import com.jobassistant.entity.User;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ResumeTailoringTest {

    @MockBean private AIProvider aiProvider;
    @MockBean private ResumeRepository resumeRepo;
    @MockBean private JobRepository jobRepo;
    @MockBean private CurrentUserService currentUser;

    @Autowired private ResumeTailoringService service;

    @Test
    void disabledThrows() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.tailor(1L, 2L)).hasMessageContaining("AI disabled");
    }

    @Test
    void missingResumeThrows() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(currentUser.getCurrentUserId()).thenReturn(99L);
        org.mockito.Mockito.when(resumeRepo.findById(1L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.tailor(1L, 2L)).hasMessageContaining("Resume not found");
    }

    @Test
    void wrongUserIsolationThrows() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(currentUser.getCurrentUserId()).thenReturn(99L);
        Resume r = new Resume();
        User u = new User(); u.setId(1L); r.setUser(u); r.setContent("test");
        org.mockito.Mockito.when(resumeRepo.findById(1L)).thenReturn(java.util.Optional.of(r));
        assertThatThrownBy(() -> service.tailor(1L, 2L)).hasMessageContaining("does not belong");
    }

    @Test
    void successTailoring() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(currentUser.getCurrentUserId()).thenReturn(99L);
        Resume r = new Resume(); User u = new User(); u.setId(99L); r.setUser(u); r.setContent("exp");
        Job j = new Job(); j.setId(2L); j.setIsActive(true); j.setTitle("Dev");
        org.mockito.Mockito.when(resumeRepo.findById(1L)).thenReturn(java.util.Optional.of(r));
        org.mockito.Mockito.when(jobRepo.findById(2L)).thenReturn(java.util.Optional.of(j));
        org.mockito.Mockito.when(aiProvider.tailorResume(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(ResumeTailoringResult.builder().originalResume("o").tailoredResume("t").diffExplanation("d").approved(false).build());
        ResumeTailoringResult res = service.tailor(1L, 2L);
        org.assertj.core.api.Assertions.assertThat(res.getTailoredResume()).isEqualTo("t");
    }

    @Test
    void missingJobThrows() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(currentUser.getCurrentUserId()).thenReturn(99L);
        Resume r = new Resume(); User u = new User(); u.setId(99L); r.setUser(u); r.setContent("c");
        org.mockito.Mockito.when(resumeRepo.findById(1L)).thenReturn(java.util.Optional.of(r));
        org.mockito.Mockito.when(jobRepo.findById(2L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.tailor(1L, 2L)).hasMessageContaining("Job not found");
    }

    @Test
    void inactiveJobThrows() {
        org.mockito.Mockito.when(aiProvider.isEnabled()).thenReturn(true);
        org.mockito.Mockito.when(currentUser.getCurrentUserId()).thenReturn(99L);
        Resume r = new Resume(); User u = new User(); u.setId(99L); r.setUser(u); r.setContent("c");
        Job j = new Job(); j.setId(2L); j.setIsActive(false);
        org.mockito.Mockito.when(resumeRepo.findById(1L)).thenReturn(java.util.Optional.of(r));
        org.mockito.Mockito.when(jobRepo.findById(2L)).thenReturn(java.util.Optional.of(j));
        assertThatThrownBy(() -> service.tailor(1L, 2L)).hasMessageContaining("inactive");
    }
}
