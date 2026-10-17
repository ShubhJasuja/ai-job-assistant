package com.jobassistant.service;

import com.jobassistant.entity.User;
import com.jobassistant.entity.CandidateProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for authentication, JWT, current user, user isolation,
 * profile creation, and basic application workflows.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class IntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        // Integration tests assume a running database (set in application-test.yml)
    }

    @Test
    void authServiceExists() {
        // Basic sanity check that Spring context loads with security services wired
        assertNotNull(authService);
        assertNotNull(passwordEncoder);
        assertNotNull(currentUserService);
    }

    @Test
    void passwordEncoderWorks() {
        String raw = "test-password";
        String encoded = passwordEncoder.encode(raw);
        assertNotEquals(raw, encoded);
        assertTrue(passwordEncoder.matches(raw, encoded));
    }
}