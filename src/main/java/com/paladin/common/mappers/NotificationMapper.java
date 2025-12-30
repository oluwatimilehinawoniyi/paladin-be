package com.paladin.common.mappers;

import com.paladin.common.dto.NotificationDTO;
import com.paladin.common.enums.NotificationType;
import com.paladin.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "read", target = "isRead")
    NotificationDTO toDTO(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(NotificationDTO dto);

    @Named("mapTypeDisplayName")
    default String mapTypeDisplayName(NotificationType type) {
        return (type != null) ? type.getDisplayName() : null;
    }

    @Named("mapEnumType")
    default NotificationType mapEnumType(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case NotificationType type -> {
                return type;
            }
            case String str -> {
                try {
                    return NotificationType.valueOf(str);
                } catch (IllegalArgumentException ignored) {
                }
            }
            default -> {
            }
        }

        return null;
    }
}
