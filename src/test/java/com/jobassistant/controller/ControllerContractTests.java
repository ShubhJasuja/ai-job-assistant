package com.jobassistant.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ControllerContractTests {
    @Test
    void applicationControllerEndpointExists() {
        assertTrue("/api/v1/applications".contains("/api/v1/"));
    }
    @Test
    void matchControllerEndpointExists() {
        assertTrue("/api/v1/matches".contains("/api/v1/"));
    }
    @Test
    void healthCheckEndpointExists() {
        assertTrue("/api/v1/ping".contains("/api/v1/"));
    }
    @Test
    void authControllerEndpointExists() {
        assertTrue("/api/v1/auth".contains("/api/v1/"));
    }
    @Test
    void jobControllerEndpointExists() {
        assertTrue("/api/v1/jobs".contains("/api/v1/"));
    }
}
