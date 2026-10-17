package com.jobassistant.service;

import com.jobassistant.entity.User;
import com.jobassistant.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Resolves the currently authenticated user from the Spring SecurityContext.
 *
 * IMPORTANT: The user is NEVER taken from client-sent parameters.
 * It is always derived from the JWT token, which is validated server-side.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the currently authenticated user's ID.
     * Throws if no user is authenticated.
     */
    public Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            // JWT authentication: principal is the email
            return userRepository.findByEmail((String) principal)
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }
        if (principal instanceof UserDetails) {
            return userRepository.findByEmail(((UserDetails) principal).getUsername())
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }
        throw new RuntimeException("No authenticated user");
    }

    /**
     * Returns the currently authenticated User entity.
     */
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return userRepository.findByEmail((String) principal)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }
        if (principal instanceof UserDetails) {
            return userRepository.findByEmail(((UserDetails) principal).getUsername())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }
        throw new RuntimeException("No authenticated user");
    }
}