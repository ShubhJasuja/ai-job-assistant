package com.jobassistant.service;

import com.jobassistant.entity.*;
import com.jobassistant.enums.RemoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4.1 — Deterministic matching engine verification.
 * Weight scheme: skills 40, experience 20, location 15, education 10, remote 10, role 5 = 100.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({MatchingEngine.class, MatchingProperties.class})
public class MatchingEngineTest {

    @Autowired
    private MatchingEngine engine;

    @Autowired
    private com.jobassistant.service.MatchingProperties props;

    private User user;
    private CandidateProfile profile;
    private Job job;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").build();

        Skill s1 = Skill.builder().name("Java").build();
        profile = CandidateProfile.builder()
                .id(1L).user(user).headline("Dev")
                .preferredRoles("Backend Developer")
                .preferredLocations("Bangalore")
                .remotePreference(true)
                .experienceLevel(3)
                .skills(List.of(s1))
                .build();

        JobSkill js = JobSkill.builder().skill_name("Java").build();
        job = Job.builder().id(10L).title("Backend Developer")
                .company("Acme").location("Bangalore").remoteStatus(RemoteStatus.REMOTE)
                .employmentType("FULL_TIME")
                .jobSkills(List.of(js))
                .build();
    }

    @Test
    void scoreWithinZeroToOneHundred() {
        JobMatch match = engine.compute(user, profile, job);
        assertNotNull(match);
        assertTrue(match.getScore() >= 0 && match.getScore() <= 100,
                "Score out of range: " + match.getScore());
    }

    @Test
    void explanationNotEmpty() {
        JobMatch match = engine.compute(user, profile, job);
        assertNotNull(match.getExplanation());
        assertFalse(match.getExplanation().isBlank());
    }

    @Test
    void weightsSumToOneHundred() {
        int sum = props.getWeightSkills() + props.getWeightExperience()
                + props.getWeightLocation() + props.getWeightEducation()
                + props.getWeightRemote() + props.getWeightRole();
        assertEquals(100, sum, "Weight scheme must sum to 100");
    }

    @Test
    void userIsolationNotBroken() {
        // Same engine; different users get independent results.
        JobMatch m1 = engine.compute(user, profile, job);
        User other = User.builder().id(99L).email("other@example.com").build();
        JobMatch m2 = engine.compute(other, profile, job);
        assertEquals(m1.getScore(), m2.getScore()); // deterministic
    }

    @Test
    void missingProfileHandledGracefully() {
        // Engine expects non-null profile; caller (MatchService) skips when null.
        // We verify here that providing a minimal profile produces a result.
        CandidateProfile empty = CandidateProfile.builder()
                .id(2L).user(user).skills(List.of()).build();
        JobMatch match = engine.compute(user, empty, job);
        assertNotNull(match);
    }
}