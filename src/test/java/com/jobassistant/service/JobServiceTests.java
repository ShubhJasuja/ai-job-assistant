package com.jobassistant.service;

import com.jobassistant.dto.JobCreateRequest;
import com.jobassistant.dto.JobResponse;
import com.jobassistant.dto.JobSearchPage;
import com.jobassistant.dto.JobSearchSuggestion;
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
        service = new JobService(jobRepository, new ManualJobIngestion(), new JobSkillNormalizer());
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

    @Test
    void keywordSearchMatchesTechnologySkillsAndEquivalentSoftwareRoles() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Software Engineer");
        request.setCompany("Acme");
        request.setLocation("San Francisco");
        request.setSkills(List.of("Java"));
        service.createJob(request);

        List<JobResponse> technologyMatches =
                service.listJobs("java", null, null, null, null, true);
        List<JobResponse> roleMatches =
                service.listJobs("software developer", null, null, null, null, true);

        assertEquals(1, technologyMatches.size());
        assertEquals(1, roleMatches.size());
        assertEquals("Software Engineer", roleMatches.get(0).getTitle());
    }

    @Test
    void singleCharacterSearchUsesWordPrefixesInsteadOfMatchingEveryDescription() {
        JobCreateRequest javaJob = new JobCreateRequest();
        javaJob.setTitle("Platform Engineer");
        javaJob.setCompany("Acme");
        javaJob.setDescription("Build services for our growing platform.");
        javaJob.setSkills(List.of("Java"));
        service.createJob(javaJob);

        JobCreateRequest unrelatedJob = new JobCreateRequest();
        unrelatedJob.setTitle("Backend Engineer");
        unrelatedJob.setCompany("Beta");
        unrelatedJob.setDescription("Join an experienced team and build reliable services.");
        service.createJob(unrelatedJob);

        List<JobResponse> matches = service.listJobs("j", null, null, null, null, true);

        assertEquals(1, matches.size());
        assertEquals("Platform Engineer", matches.get(0).getTitle());
    }

    @Test
    void suggestionsIncludeTechnologyRoleAliasesAndPrefixMatchedLocations() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Software Engineer");
        request.setCompany("Acme");
        request.setLocation("San Francisco");
        request.setSkills(List.of("Java"));
        service.createJob(request);

        List<JobSearchSuggestion> keywordSuggestions =
                service.searchSuggestions("j", "keyword");
        List<JobSearchSuggestion> roleSuggestions =
                service.searchSuggestions("sof", "keyword");
        List<JobSearchSuggestion> locationSuggestions =
                service.searchSuggestions("s", "location");
        List<JobSearchSuggestion> inLocationWordSuggestions =
                service.searchSuggestions("fra", "location");

        assertTrue(keywordSuggestions.stream().anyMatch(suggestion ->
                suggestion.value().equals("Java") && suggestion.category().equals("TECHNOLOGY")));
        assertTrue(roleSuggestions.stream().anyMatch(suggestion ->
                suggestion.value().equals("Software Developer") && suggestion.category().equals("ROLE")));
        assertTrue(locationSuggestions.stream().anyMatch(suggestion ->
                suggestion.value().equals("San Francisco")));
        assertTrue(inLocationWordSuggestions.stream().anyMatch(suggestion ->
                suggestion.value().equals("San Francisco")));
    }

    @Test
    void searchPageReturnsBoundedResultsAndPagingMetadata() {
        for (String title : List.of("Backend Engineer", "Frontend Engineer", "Platform Engineer")) {
            JobCreateRequest request = new JobCreateRequest();
            request.setTitle(title);
            request.setCompany("Acme");
            service.createJob(request);
        }

        JobSearchPage<JobResponse> firstPage =
                service.searchPage(null, null, null, null, null, true, null, 0, 2);
        JobSearchPage<JobResponse> lastPage =
                service.searchPage(null, null, null, null, null, true, null, 2, 2);

        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.items().size());
        assertTrue(firstPage.hasNext());
        assertEquals(3, lastPage.totalElements());
        assertEquals(1, lastPage.items().size());
        assertFalse(lastPage.hasNext());
    }

    @Test
    void searchPageRejectsInvalidPageBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> service.searchPage(null, null, null, null, null, true, null, -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> service.searchPage(null, null, null, null, null, true, null, 0, 101));
    }
}