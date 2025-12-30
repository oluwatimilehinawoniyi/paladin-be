package com.paladin.common.dto;

import com.paladin.common.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        UUID userId,
        NotificationType type,
        String typeDisplayName,
        String title,
        String message,
        UUID relatedEntityId,
        String relatedEntityType,
        boolean isRead,
        Instant createdAt
) {
}
