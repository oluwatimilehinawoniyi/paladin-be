package com.paladin.common.enums;

import lombok.Getter;

@Getter
public enum FeatureRequestCategory {
    AI_FEATURES("AI & Analysis", "AI-powered features and intelligent analysis"),
    EMAIL_AUTOMATION("Email & Communication", "Email sending and automation features"),
    UI_UX("User Interface", "UI improvements and user experience enhancements"),
    JOB_TRACKING("Job Tracking", "Application tracking and management tools"),
    CV_MANAGEMENT("CV Management", "CV upload, editing, and storage features"),
    INTEGRATIONS("Integrations", "Third-party service integrations"),
    ANALYTICS("Analytics & Reports", "Data analytics and reporting features"),
    PERFORMANCE("Performance", "Speed and performance improvements"),
    MOBILE("Mobile App", "Mobile-specific features and improvements"),
    OTHER("Other", "Other feature requests");

    private final String displayName;
    private final String description;

    FeatureRequestCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

}