package com.jobassistant.service;

import com.jobassistant.entity.Job;
import com.jobassistant.entity.JobSkill;
import com.jobassistant.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSourceIngestionWriterTest {
    @Mock private JobRepository repository;

    private JobSourceIngestionWriter writer;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        writer = new JobSourceIngestionWriter(repository);
    }

    @Test
    void rerunningRefreshUpdatesExistingRecordWithoutChangingItsIdentity() {
        Job incoming = Job.builder()
                .title("Backend Engineer")
                .company("Example")
                .location("Remote")
                .sourceJobId("board:123")
                .jobSkills(new ArrayList<>(List.of(JobSkill.builder().skill_name("Java").build())))
                .build();
        Job existing = Job.builder()
                .id(42L)
                .title("Backend Engineer")
                .company("Example")
                .location("Office")
                .source("greenhouse")
                .sourceJobId("board:123")
                .jobSkills(new ArrayList<>(List.of(JobSkill.builder().skill_name("Python").build())))
                .build();
        when(repository.findBySourceAndSourceJobId("greenhouse", "board:123"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(repository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var first = writer.persist("greenhouse", List.of(incoming));
        var second = writer.persist("greenhouse", List.of(incoming));

        assertThat(first.added()).isEqualTo(1);
        assertThat(second.added()).isZero();
        assertThat(second.updated()).isEqualTo(1);
        assertThat(existing.getId()).isEqualTo(42L);
        assertThat(existing.getLocation()).isEqualTo("Remote");
        assertThat(existing.getJobSkills()).extracting(JobSkill::getSkill_name).containsExactly("Java");
        verify(repository, times(2)).save(any(Job.class));
    }

    @Test
    void skipsMalformedListingsAndAcceptsAnEmptySource() {
        var empty = writer.persist("greenhouse", List.of());
        var malformed = writer.persist("greenhouse", List.of(Job.builder()
                .title(" ")
                .company("Example")
                .sourceJobId("bad")
                .build()));

        assertThat(empty.successful()).isTrue();
        assertThat(empty.fetched()).isZero();
        assertThat(malformed.skipped()).isEqualTo(1);
        verifyNoInteractions(repository);
    }
}
