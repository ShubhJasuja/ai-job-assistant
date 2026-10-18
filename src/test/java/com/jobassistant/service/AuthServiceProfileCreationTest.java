package com.jobassistant.service;

import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.User;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.UserRepository;
import com.jobassistant.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that registration creates both the User and its CandidateProfile
 * in a single transaction, and that no partial account can survive a failure.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class AuthServiceProfileCreationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository profileRepository;

    @Autowired
    private AuthService authService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtService jwtService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private AuthenticationManager authenticationManager;

    @org.springframework.boot.test.mock.mockito.MockBean
    private PasswordEncoder passwordEncoder;

    private AuthService newService(PasswordEncoder encoder) {
        return new AuthService(userRepository, profileRepository, encoder,
                jwtService, authenticationManager);
    }

    @Test
    void registerCreatesUserAndProfile() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("reg@test.local");
        req.setPassword("Pass123!");
        var resp = authService.register(req);

        User user = userRepository.findByEmail("reg@test.local").orElseThrow();
        CandidateProfile profile = profileRepository.findByUserId(user.getId()).orElseThrow();

        assertEquals(user.getId(), profile.getUser().getId());
        assertNotEquals(0L, user.getId());
        assertNotEquals(0L, profile.getId());
        assertEquals("access", resp.getAccessToken());
        assertEquals("refresh", resp.getRefreshToken());
    }

    @Test
    void profileRetrievalWorksAfterRegistration() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("profile@test.local");
        req.setPassword("Pass123!");
        authService.register(req);

        User user = userRepository.findByEmail("profile@test.local").orElseThrow();
        assertDoesNotThrow(() -> profileRepository.findByUserId(user.getId()).orElseThrow());
    }

    @Test
    void duplicateEmailRegistrationFails() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("dup@test.local");
        req.setPassword("Pass123!");
        authService.register(req);

        assertThrows(RuntimeException.class, () -> authService.register(req));

        assertEquals(1, userRepository.count());
        assertEquals(1, profileRepository.count());
    }

    @Test
    void failureDuringRegistrationLeavesNoPartialAccount() {
        PasswordEncoder throwingEncoder = new PasswordEncoder() {
            @Override public String encode(CharSequence raw) { throw new IllegalStateException("encoder failed"); }
            @Override public boolean matches(CharSequence raw, String encoded) { return false; }
        };
        AuthService failingService = newService(throwingEncoder);

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("partial@test.local");
        req.setPassword("Pass123!");
        assertThrows(IllegalStateException.class, () -> failingService.register(req));

        assertEquals(0, userRepository.count());
        assertEquals(0, profileRepository.count());
        assertFalse(userRepository.findByEmail("partial@test.local").isPresent());
    }
}