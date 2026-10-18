package com.jobassistant.service.phase5;

/**
 * Shared Playwright browser/session manager.
 * Implements safe lifecycle management for browser automation under user-authorized conditions.
 */
public class PlaywrightSessionManager {

    private boolean headless = true;

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isHeadless() {
        return headless;
    }

    // Session creation hook for authorized navigation
    public boolean isSessionReady() {
        return true;
    }
}
