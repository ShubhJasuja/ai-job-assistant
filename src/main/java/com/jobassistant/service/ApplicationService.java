package com.jobassistant.service;

import com.jobassistant.dto.UpdateStatusRequest;
import com.jobassistant.entity.Application;
import com.jobassistant.entity.ApplicationEvent;
import com.jobassistant.entity.Job;
import com.jobassistant.entity.User;
import com.jobassistant.enums.ApplicationStatus;
import com.jobassistant.repository.ApplicationEventRepository;
import com.jobassistant.repository.ApplicationRepository;
import com.jobassistant.repository.JobRepository;
import com.jobassistant.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.EnumMap;

/**
 * Orchestrates the application state machine.
 */
@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    // Define valid forward transitions
    private static final Map<ApplicationStatus, List<ApplicationStatus>> VALID_TRANSITIONS = Map.of(
            ApplicationStatus.DISCOVERED, List.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.SHORTLISTED, List.of(ApplicationStatus.REVIEWING, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.REVIEWING, List.of(ApplicationStatus.READY_TO_APPLY, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.READY_TO_APPLY, List.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.APPLIED, List.of(ApplicationStatus.OA, ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OA, List.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.INTERVIEW, List.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER, List.of(ApplicationStatus.WITHDRAWN)
    );

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationEventRepository eventRepository,
                              UserRepository userRepository,
                              JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public Application create(Long userId, Long jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Prevent duplicates
        if (applicationRepository.findByUserIdAndJobId(userId, jobId).isPresent()) {
            throw new RuntimeException("Application already exists for this user and job");
        }

        Application app = Application.builder()
                .user(user)
                .job(job)
                .status(ApplicationStatus.DISCOVERED)
                .build();

        Application saved = applicationRepository.save(app);

        createEvent(saved, null, ApplicationStatus.DISCOVERED, null);

        return saved;
    }

    @Transactional
    public Application updateStatus(Long applicationId, UpdateStatusRequest request, Long currentUserId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (app.getUser() == null || !app.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Application does not belong to current user");
        }

        ApplicationStatus currentStatus = app.getStatus();
        ApplicationStatus newStatus = request.getNewStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new RuntimeException(
                    "Invalid transition from " + currentStatus + " to " + newStatus);
        }

        // Record the transition in the audit trail
        createEvent(app, currentStatus, newStatus, request.getNotes());

        app.setStatus(newStatus);
        app.setNotes(request.getNotes());
        return applicationRepository.save(app);
    }

    private boolean isValidTransition(ApplicationStatus from, ApplicationStatus to) {
        // Terminal states cannot transition out
        if (from == ApplicationStatus.OFFER || from == ApplicationStatus.REJECTED) {
            return false;
        }
        // WITHDRAWN is terminal
        if (from == ApplicationStatus.WITHDRAWN) {
            return false;
        }

        return VALID_TRANSITIONS.getOrDefault(from, Collections.emptyList()).contains(to);
    }

    private ApplicationEvent createEvent(Application app,
                                         ApplicationStatus from,
                                         ApplicationStatus to,
                                         String notes) {
        ApplicationEvent event = ApplicationEvent.builder()
                .application(app)
                .fromStatus(from)
                .toStatus(to)
                .notes(notes)
                .build();
        return eventRepository.save(event);
    }

    public List<ApplicationEvent> getEventHistory(Long applicationId) {
        return eventRepository.findByApplicationId(applicationId,
                org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();
    }

    public Map<ApplicationStatus, Long> getStatusCounts(Long userId) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, applicationRepository.countByUserIdAndStatus(userId, status));
        }
        return counts;
    }

    public Map<String, Long> getStatusCountsAsString(Long userId) {
        Map<ApplicationStatus, Long> counts = getStatusCounts(userId);
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<ApplicationStatus, Long> entry : counts.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
}