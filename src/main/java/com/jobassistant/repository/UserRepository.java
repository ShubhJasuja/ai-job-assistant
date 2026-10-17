package com.jobassistant.repository;

import com.jobassistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 * Spring Data JPA auto-generates basic CRUD operations (save, findById, delete, etc.).
 * We only write queries for custom business needs (e.g. findByEmail).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Returns Optional<User> so we can distinguish "not found" from "found".
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email.
     * More efficient than loading the full entity just to check existence.
     */
    boolean existsByEmail(String email);
}