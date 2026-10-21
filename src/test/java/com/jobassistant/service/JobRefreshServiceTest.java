package com.jobassistant.service;

import com.jobassistant.entity.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRefreshServiceTest {

    @Mock private JobSource source;
    @Mock private JobSourceIngestionWriter writer;
    private JobRefreshService service;

    @BeforeEach
    void setUp() {
        service = new JobRefreshService(List.of(source), writer);
        when(source.getSourceName()).thenReturn("greenhouse");
    }

    @Test
    void refreshAddsThenUpdatesTheSameSourceListing() {
        Job incoming = Job.builder().title("Backend Engineer").company("Example")
                .sourceJobId("stripe:123").build();
        when(source.fetch()).thenReturn(List.of(incoming));
        when(writer.persist("greenhouse", List.of(incoming)))
                .thenReturn(new com.jobassistant.dto.JobSourceRefreshSummary("greenhouse", 1, 1, 0, 0, true),
                        new com.jobassistant.dto.JobSourceRefreshSummary("greenhouse", 1, 0, 1, 0, true));

        var firstRefresh = service.refresh();
        var secondRefresh = service.refresh();

        assertThat(firstRefresh.added()).isEqualTo(1);
        assertThat(secondRefresh.added()).isZero();
        assertThat(secondRefresh.updated()).isEqualTo(1);
    }

    @Test
    void emptySourceIsSuccessfulAndAddsNothing() {
        when(source.fetch()).thenReturn(List.of());
        when(writer.persist("greenhouse", List.of()))
                .thenReturn(new com.jobassistant.dto.JobSourceRefreshSummary("greenhouse", 0, 0, 0, 0, true));

        var summary = service.refresh();

        assertThat(summary.fetched()).isZero();
        assertThat(summary.added()).isZero();
        assertThat(summary.sources()).singleElement().satisfies(result -> {
            assertThat(result.successful()).isTrue();
            assertThat(result.fetched()).isZero();
        });
    }

    @Test
    void unavailableSourceIsReportedAndDoesNotStopOtherSources() {
        when(source.fetch()).thenThrow(new IllegalStateException("unavailable"));

        var summary = service.refresh();

        assertThat(summary.sources()).singleElement().satisfies(result -> {
            assertThat(result.successful()).isFalse();
            assertThat(result.source()).isEqualTo("greenhouse");
        });
        assertThat(summary.added()).isZero();
    }
}
