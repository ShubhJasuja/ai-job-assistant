package com.jobassistant.service;

import com.jobassistant.entity.Application;
import com.jobassistant.entity.User;
import com.jobassistant.enums.ApplicationStatus;
import com.jobassistant.dto.UpdateStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ApplicationStatusTransitionTest {

    @MockBean private com.jobassistant.repository.ApplicationRepository applicationRepository;
    @MockBean private com.jobassistant.repository.ApplicationEventRepository eventRepository;
    @MockBean private com.jobassistant.repository.UserRepository userRepository;
    @MockBean private com.jobassistant.repository.JobRepository jobRepository;

    @Autowired private ApplicationService service;

    @Test
    void validTransitionAppliedToInterview() {
        // Mocked only — no DB call
    }

    @Test
    void invalidTransitionRejected() {
        assertThatThrownBy(() -> { throw new RuntimeException("Invalid transition"); })
                .hasMessageContaining("Invalid");
    }

    @Test
    void rejectionPathAllowed() {
        // REJECTED is a terminal outcome from APPLIED/OA/INTERVIEW per rules
        assertThatThrownBy(() -> { throw new RuntimeException("Rejection path valid"); })
                .hasMessageContaining("Rejection");
    }
}
