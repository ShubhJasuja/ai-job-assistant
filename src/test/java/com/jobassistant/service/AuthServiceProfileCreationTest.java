package com.jobassistant.service;

import com.jobassistant.entity.CandidateProfile;
import com.jobassistant.entity.User;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.UserRepository;
import com.jobassistant.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit test — no Spring context, no @DataJpaTest.
 * Verifies that register() creates both User and CandidateProfile
 * and that duplicate checks work; failure does not leave partials.
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceProfileCreationTest {

    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndProfile() {
        User savedUser = User.builder().id(3L).email("reg@test.local").passwordHash("h").build();
        when(userRepository.existsByEmail("reg@test.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(passwordEncoder.encode(any())).thenReturn("h");
        when(jwtService.generateAccessToken(savedUser)).thenReturn("a");
        when(jwtService.generateRefreshToken(savedUser)).thenReturn("r");
        when(profileRepository.save(any(CandidateProfile.class))).thenAnswer(i -> {
            CandidateProfile p = i.getArgument(0);
            p.setId(1L); // simulate DB assign
            return p;
        });

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("reg@test.local");
        req.setPassword("Pass1!");
        var resp = authService.register(req);

        assertNotNull(resp.getAccessToken());
        verify(userRepository).save(any(User.class));
        verify(profileRepository).save(argThat(p -> p.getUser() == savedUser));
    }

    @Test
    void duplicateRegistrationFails() {
        when(userRepository.existsByEmail("dup@test.local")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> {
            var req = new com.jobassistant.dto.AuthRequest();
            req.setEmail("dup@test.local");
            req.setPassword("Pass1!");
            authService.register(req);
        });
        verify(userRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void profileAssociatedWithCorrectUser() {
        User user = User.builder().id(7L).email("assoc@test.local").passwordHash("h").build();
        when(userRepository.existsByEmail("assoc@test.local")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("h");
        when(jwtService.generateAccessToken(user)).thenReturn("a");
        when(jwtService.generateRefreshToken(user)).thenReturn("r");

        var req = new com.jobassistant.dto.AuthRequest();
        req.setEmail("assoc@test.local");
        req.setPassword("Pass1!");
        authService.register(req);

        verify(profileRepository).save(argThat(p -> p.getUser() != null && p.getUser().getId().equals(7L)));
    }
}