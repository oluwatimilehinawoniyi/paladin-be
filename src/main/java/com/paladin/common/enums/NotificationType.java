package com.paladin.common.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    STATUS_UPDATE(
            "Status Update",
            "Sent when a feature request's status changes (e.g., from 'Pending' to 'In Progress')."
    ),

    ADMIN_RESPONSE(
            "Admin Response",
            "Sent when an admin adds a comment or response to a feature request."
    ),

    SUBSCRIBED_UPDATE(
            "Subscribed Update",
            "Sent to users who upvoted or subscribed to a feature request when it gets updated."
    ),

    FEATURE_ANNOUNCEMENT(
            "Feature Announcement",
            "Sent when a new feature goes live and is announced to interested users."
    ),

    SYSTEM_ANNOUNCEMENT(
            "System Announcement",
            "General notification sent to all users for system-wide messages or maintenance updates."
    );

    private final String displayName;
    private final String description;

    NotificationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
