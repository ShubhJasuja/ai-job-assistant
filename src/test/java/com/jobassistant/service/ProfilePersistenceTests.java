package com.jobassistant.service;

import com.jobassistant.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the User entity, repository, and basic profile-related persistence.
 * Uses the PostgreSQL test datasource (not an embedded database).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class ProfilePersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void userRepositoryExists() {
        assertNotNull(userRepository);
    }
}