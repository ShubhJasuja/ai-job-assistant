package com.jobassistant.service;

import com.jobassistant.dto.JobDiscoveryResponse;
import com.jobassistant.dto.JobSearchPage;
import com.jobassistant.dto.JobStateRequest;
import com.jobassistant.entity.*;
import com.jobassistant.enums.JobUserStateType;
import com.jobassistant.enums.ApplicationStatus;
import com.jobassistant.repository.ApplicationRepository;
import com.jobassistant.repository.CandidateProfileRepository;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.JobUserStateRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobDiscoveryService {
    private final JobService jobService;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobUserStateRepository stateRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final CurrentUserService currentUserService;
    private final MatchingEngine matchingEngine;

    public JobDiscoveryService(JobService jobService,
                               JobRepository jobRepository,
                               CandidateProfileRepository profileRepository,
                               JobUserStateRepository stateRepository,
                               ApplicationRepository applicationRepository,
                               ApplicationService applicationService,
                               CurrentUserService currentUserService,
                               MatchingEngine matchingEngine) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.stateRepository = stateRepository;
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.currentUserService = currentUserService;
        this.matchingEngine = matchingEngine;
    }

    @Transactional
    public List<JobDiscoveryResponse> discover(String keyword, String company, String location,
                                               Boolean remote, String employmentType, String skill,
                                               String state) {
        User user = currentUserService.getCurrentUser();
        CandidateProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        Map<Long, JobUserStateType> states = stateRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(item -> item.getJob().getId(), JobUserState::getState));
        Map<Long, ApplicationStatus> applicationStatuses = applicationRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(application -> application.getJob().getId(), Application::getStatus));

        List<JobDiscoveryResponse> responses = jobService.searchJobs(
                        keyword, company, location, remote, employmentType, true, skill).stream()
                .map(job -> {
                    JobMatch match = profile == null ? null : matchingEngine.compute(user, profile, job);
                    String userState = Optional.ofNullable(applicationStatuses.get(job.getId()))
                            .map(status -> isSubmittedStatus(status) ? "APPLIED" : "TRACKED")
                            .orElseGet(() -> Optional.ofNullable(states.get(job.getId()))
                                    .map(Enum::name)
                                    .orElse("NONE"));
                    return JobDiscoveryResponse.from(job, match, userState);
                })
                .filter(item -> matchesState(item.getUserState(), state))
                .toList();

        if (profile != null) {
            return responses.stream()
                    .sorted(Comparator.comparing(JobDiscoveryResponse::getMatchScore,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return responses;
    }

    @Transactional
    public JobSearchPage<JobDiscoveryResponse> discoverPage(
            String keyword, String company, String location, Boolean remote, String employmentType,
            String skill, String state, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Offset must be non-negative and limit must be between 1 and 100");
        }
        List<JobDiscoveryResponse> results = discover(
                keyword, company, location, remote, employmentType, skill, state);
        int fromIndex = Math.min(offset, results.size());
        int toIndex = (int) Math.min((long) fromIndex + limit, results.size());
        return new JobSearchPage<>(
                results.subList(fromIndex, toIndex), results.size(), toIndex < results.size());
    }

    @Transactional
    public JobDiscoveryResponse updateState(Long jobId, JobStateRequest.JobState requestedState) {
        User user = currentUserService.getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .filter(found -> Boolean.TRUE.equals(found.getIsActive()))
                .orElseThrow(() -> new NoSuchElementException("Active job not found"));
        Optional<Application> application = applicationRepository.findByUserIdAndJobId(user.getId(), jobId);
        if (application.isPresent() && requestedState != JobStateRequest.JobState.APPLIED) {
            throw new IllegalStateException("An applied job cannot be changed to a discovery state");
        }

        switch (requestedState) {
            case APPLIED -> {
                if (application.isEmpty()) {
                    applicationService.createAsApplied(jobId, user.getId());
                } else if (!isSubmittedStatus(application.get().getStatus())) {
                    throw new IllegalStateException("Update the existing application through its status workflow");
                }
                stateRepository.deleteByUserIdAndJobId(user.getId(), jobId);
            }
            case INTERESTED, NOT_INTERESTED -> {
                JobUserStateType newState = JobUserStateType.valueOf(requestedState.name());
                JobUserState savedState = stateRepository.findByUserIdAndJobId(user.getId(), jobId)
                        .orElseGet(() -> JobUserState.builder().user(user).job(job).build());
                savedState.setState(newState);
                stateRepository.save(savedState);
            }
            case NONE -> stateRepository.deleteByUserIdAndJobId(user.getId(), jobId);
        }

        CandidateProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        JobMatch match = profile == null ? null : matchingEngine.compute(user, profile, job);
        String state = applicationRepository.findByUserIdAndJobId(user.getId(), jobId)
                .map(app -> isSubmittedStatus(app.getStatus()) ? "APPLIED" : "TRACKED")
                .orElseGet(() -> stateRepository.findByUserIdAndJobId(user.getId(), jobId)
                        .map(item -> item.getState().name())
                        .orElse("NONE"));
        return JobDiscoveryResponse.from(job, match, state);
    }

    private boolean isSubmittedStatus(ApplicationStatus status) {
        return status == ApplicationStatus.APPLIED
                || status == ApplicationStatus.OA
                || status == ApplicationStatus.INTERVIEW
                || status == ApplicationStatus.OFFER
                || status == ApplicationStatus.REJECTED;
    }

    private boolean matchesState(String currentState, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter)) {
            return !"NOT_INTERESTED".equals(currentState);
        }
        if ("SAVED".equalsIgnoreCase(filter)) {
            return "INTERESTED".equals(currentState);
        }
        return currentState.equalsIgnoreCase(filter.trim());
    }
}
