package com.jobassistant.dto;

import com.jobassistant.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating an application's status.
 * Sent by React when user clicks a new state.
 */
public class UpdateStatusRequest {

    @NotNull(message = "Target status is required")
    private ApplicationStatus newStatus;

    private String notes; // optional: why did you move to this status?

    public ApplicationStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ApplicationStatus newStatus) { this.newStatus = newStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}