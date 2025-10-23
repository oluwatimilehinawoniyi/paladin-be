package com.paladin.notification.repository;

import com.paladin.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {
    // Fetch all notifications for a user, newest first
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Fetch unread notifications for a user, newest first
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    // Count how many unread notifications a user has
    long countByUserIdAndIsReadFalse(UUID userId);

    // Get the 10 most recent notifications for a user
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    // Find notifications by user and type
    @Query("""
                SELECT n FROM Notification n
                WHERE n.user.id = :userId
                AND n.type = :type
                ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserAndType(
            @Param("userId") UUID userId,
            @Param("type") String type
    );

    // Find notifications created after a specific date
    @Query("""
                SELECT n FROM Notification n
                WHERE n.user.id = :userId
                AND n.createdAt > :after
                ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdAndCreatedAtAfter(
            @Param("userId") UUID userId,
            @Param("after") Instant after
    );

    // check if user already has unread notification for same entity
    boolean existsByUserIdAndRelatedEntityIdAndIsReadFalse(UUID userId, UUID relatedEntityId);

    @Query("""
            SELECT DISTINCT n.user.id FROM Notification n
            WHERE n.relatedEntityId = :featureRequestId AND n.isRead = false
            """)
    List<UUID> findUserIdsWithUnreadNotificationForEntity(@Param("featureRequestId") UUID featureRequestId);
}
