package com.jobassistant.service;

import org.springframework.stereotype.Service;

/**
 * Phase 5 framework — browser automation for permitted application targets only.
 * REAL IMPLEMENTATION BLOCKED until permitted target sites + authorization +
 * human approval workflow are specified (per instructions: do not bypass
 * anti-bot, CAPTCHAs, or access restrictions).
 *
 * Supported page types (documented, not verified live):
 * - Company career portal forms (permitted with authorization)
 * - Authorized applicant-tracking-system (ATS) pages (with explicit agreement)
 * - No unsupported scraping of commercial job boards.
 */
@Service
public class ApplicationAutomationService {

    // Mandatory approval mechanism: approval must be tied to a specific
    // draft (Application preparation record) and invalidated on edit.
    // Implementation deferred until permitted targets and review workflow
    // are specified.

    public boolean canAutomate(String targetUrl) {
        return false; // blocked until permitted source confirmed
    }
}
