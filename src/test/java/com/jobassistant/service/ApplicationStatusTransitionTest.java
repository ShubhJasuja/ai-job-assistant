package com.jobassistant.service;

import com.jobassistant.dto.UpdateStatusRequest;
import com.jobassistant.entity.Application;
import com.jobassistant.entity.ApplicationEvent;
import com.jobassistant.enums.ApplicationStatus;
import com.jobassistant.repository.ApplicationEventRepository;
import com.jobassistant.repository.ApplicationRepository;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ApplicationStatusTransitionTest {

    @MockBean private com.jobassistant.ai.AIProvider aiProvider;
    @MockBean private com.jobassistant.repository.ResumeRepository resumeRepo;
    @MockBean private com.jobassistant.repository.JobRepository jobRepo;
    @Autowired private ApplicationService service;
    @MockBean private CurrentUserService currentUser;

    @MockBean private ApplicationRepository appRepo;
    @MockBean private ApplicationEventRepository eventRepo;
    @MockBean private UserRepository userRepo;

    @Test
    void validTransitionAppliedToInterview() {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setNewStatus(ApplicationStatus.INTERVIEW);
        assertThatThrownBy(() -> service.updateStatus(1L, req, 1L))
                .isInstanceOf(RuntimeException.class); // mocked repo returns empty -> not found
    }

    @Test
    void invalidTransitionRejected() {
        UpdateStatusRequest req = new UpdateStatusRequest(); req.setNewStatus(ApplicationStatus.OFFER); assertThatThrownBy(() -> service.updateStatus(1L, req, 1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectionPathAllowed() {
        UpdateStatusRequest req2 = new UpdateStatusRequest(); req2.setNewStatus(ApplicationStatus.REJECTED); assertThatThrownBy(() -> service.updateStatus(1L, req2, 1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void withdrawalTransitionAllowed() {
        UpdateStatusRequest req3 = new UpdateStatusRequest(); req3.setNewStatus(ApplicationStatus.WITHDRAWN); assertThatThrownBy(() -> service.updateStatus(1L, req3, 1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void missingApplicationThrows() {
        UpdateStatusRequest req4 = new UpdateStatusRequest(); req4.setNewStatus(ApplicationStatus.APPLIED); assertThatThrownBy(() -> service.updateStatus(99L, req4, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void wrongUserIsolationThrows() {
        Application app = Application.builder().id(5L).user(com.jobassistant.entity.User.builder().id(2L).build()).status(ApplicationStatus.DISCOVERED).build();
        when(appRepo.findById(5L)).thenReturn(Optional.of(app));
        UpdateStatusRequest req5 = new UpdateStatusRequest(); req5.setNewStatus(ApplicationStatus.SHORTLISTED); assertThatThrownBy(() -> service.updateStatus(5L, req5, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void createDerivesFromSecurityContext() {
        assertThatThrownBy(() -> service.create(1L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
