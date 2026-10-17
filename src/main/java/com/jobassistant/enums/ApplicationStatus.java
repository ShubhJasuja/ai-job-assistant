package com.jobassistant.enums;

/**
 * The finite states an application can exist in.
 */
public enum ApplicationStatus {
    DISCOVERED,
    SHORTLISTED,
    REVIEWING,
    READY_TO_APPLY,
    APPLIED,
    OA,             // Online Assessment
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN
}