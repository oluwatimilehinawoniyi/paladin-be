package com.paladin.common.enums;

public enum ApplicationStatus {
    SENT,
    FAILED_TO_SEND,  // Email sending failed (circuit breaker open or retries exhausted)
    INTERVIEW,
    REJECTED,
    ACCEPTED,
    FOLLOW_UP
}
