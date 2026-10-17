package com.jobassistant.service;

import com.jobassistant.dto.JobCreateRequest;
import com.jobassistant.dto.JobResponse;
import com.jobassistant.entity.Job;
import com.jobassistant.ingestion.ManualJobIngestion;
import com.jobassistant.enums.RemoteStatus;
import com.jobassistant.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests job creation, dedup, retrieval, listing, update, deactivation.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class JobServiceTests {

    @Autowired
    private JobRepository jobRepository;

    private JobService service;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        service = new JobService(jobRepository, new ManualJobIngestion());
    }

    @Test
    void createAndRetrieveJob() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Backend Developer");
        req.setCompany("Acme");
        req.setLocation("Remote");
        req.setRemoteStatus(RemoteStatus.REMOTE);
        req.setEmploymentType("FULL_TIME");

        JobResponse created = service.createJob(req);
        assertNotNull(created.getId());
        assertEquals("Backend Developer", created.getTitle());
        assertEquals("Acme", created.getCompany());
        assertTrue(created.getIsActive());
    }

    @Test
    void duplicateJobRejected() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Dev");
        req.setCompany("Acme");
        req.setSourceJobId("ext-123");

        service.createJob(req);

        JobCreateRequest req2 = new JobCreateRequest();
        req2.setTitle("Dev Duplicate");
        req2.setCompany("Acme");
        req2.setSourceJobId("ext-123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.createJob(req2));
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void listJobsFiltered() {
        JobCreateRequest r1 = new JobCreateRequest();
        r1.setTitle("Java Developer");
        r1.setCompany("Acme");
        r1.setLocation("Bangalore");
        r1.setEmploymentType("FULL_TIME");
        service.createJob(r1);

        JobCreateRequest r2 = new JobCreateRequest();
        r2.setTitle("Python Intern");
        r2.setCompany("Beta");
        r2.setLocation("Remote");
        r2.setEmploymentType("INTERNSHIP");
        service.createJob(r2);

        List<JobResponse> all = service.listJobs(null, null, null, null, null, null);
        assertEquals(2, all.size());

        List<JobResponse> byCompany = service.listJobs(null, "Acme", null, null, null, null);
        assertEquals(1, byCompany.size());
        assertEquals("Java Developer", byCompany.get(0).getTitle());

        List<JobResponse> byKeyword = service.listJobs("python", null, null, null, null, null);
        assertEquals(1, byKeyword.size());
        assertEquals("Python Intern", byKeyword.get(0).getTitle());

        List<JobResponse> byType = service.listJobs(null, null, null, null, "INTERNSHIP", null);
        assertEquals(1, byType.size());
    }

    @Test
    void deactivateJob() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Dev");
        req.setCompany("Acme");
        JobResponse created = service.createJob(req);

        service.deactivateJob(created.getId());
        JobResponse updated = service.getJobById(created.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @Test
    void updateJob() {
        JobCreateRequest req = new JobCreateRequest();
        req.setTitle("Junior Dev");
        req.setCompany("Acme");
        JobResponse created = service.createJob(req);

        JobCreateRequest update = new JobCreateRequest();
        update.setTitle("Senior Dev");
        JobResponse updated = service.updateJob(created.getId(), update);
        assertEquals("Senior Dev", updated.getTitle());
    }
}