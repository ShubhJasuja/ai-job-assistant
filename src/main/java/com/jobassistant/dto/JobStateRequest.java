package com.jobassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobStateRequest {
    @NotNull
    private JobState state;

    public enum JobState {
        INTERESTED,
        NOT_INTERESTED,
        APPLIED,
        NONE
    }
}
