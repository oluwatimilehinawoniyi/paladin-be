package com.paladin.notification.service;

import com.paladin.common.dto.NotificationDTO;
import com.paladin.common.enums.NotificationType;
import com.paladin.common.exceptions.NotFoundException;
import com.paladin.common.exceptions.UnauthorizedAccessException;
import com.paladin.common.mappers.NotificationMapper;
import com.paladin.featureRequest.FeatureRequestVote;
import com.paladin.featureRequest.repository.FeatureRequestRepository;
import com.paladin.featureRequest.repository.FeatureRequestVoteRepository;
import com.paladin.notification.entity.Notification;
import com.paladin.notification.repository.NotificationRepository;
import com.paladin.user.User;
import com.paladin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.paladin.common.EntityTypes.FEATURE_REQUEST;
import static com.paladin.common.EntityTypes.SYSTEM;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FeatureRequestVoteRepository featureRequestVoteRepository;
    private final FeatureRequestRepository featureRequestRepository;

    /**
     * Notify a single user
     */
    public void notifyUser(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID relatedEntityId,
            String relatedEntityType) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("No user found to be notified!"));

        if (relatedEntityId != null) {
            boolean alreadyNotified = notificationRepository
                    .existsByUserIdAndRelatedEntityIdAndIsReadFalse(
                            userId, relatedEntityId);

            if (alreadyNotified) {
                log.info("User {} already has unread notification for entity {}, skipping duplicate",
                        userId, relatedEntityId);
                return;
            }
        }

        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .createdAt(Instant.now())
                .isRead(false)
                .build();

        Notification newNotification = notificationRepository.save(notification);
        log.info("Notification sent to {}", userId);
    }

    /**
     * Notify all subscribers (upvoters) of a feature request
     */
    public void notifySubscribers(
            UUID featureRequestId,
            String title,
            String message) {
        UUID requestCreatorId = featureRequestRepository
                .findById(featureRequestId)
                .orElseThrow(() -> new NotFoundException("Creator of Request not found"))
                .getUser()
                .getId();

        List<FeatureRequestVote> votes = featureRequestVoteRepository
                .findByFeatureRequestId(featureRequestId);

        if (votes.isEmpty()) {
            log.info("No subscribers to notify for feature request {}", featureRequestId);
            return;
        }

        List<UUID> subscriberIds = votes
                .stream()
                .map(vote -> vote.getUser().getId())
                .filter(userId -> !userId.equals(requestCreatorId))
                .distinct()
                .toList();

        if (subscriberIds.isEmpty()) {
            log.info("No subscribers to notify (creator was only voter)");
            return;
        }

        List<UUID> alreadyNotifiedUsers = notificationRepository
                .findUserIdsWithUnreadNotificationForEntity(featureRequestId);


        List<UUID> usersToNotify = subscriberIds
                .stream()
                .filter(userId -> !alreadyNotifiedUsers.contains(userId))
                .toList();

        if (usersToNotify.isEmpty()) {
            log.info("All subscribers already have unread notifications");
            return;
        }

        if (usersToNotify.size() > 100) {
            notifyInBatches(
                    usersToNotify,
                    NotificationType.SUBSCRIBED_UPDATE,
                    title,
                    message,
                    featureRequestId,
                    FEATURE_REQUEST);
        } else {
            for (UUID userId : usersToNotify) {
                notifyUser(
                        userId,
                        NotificationType.SUBSCRIBED_UPDATE,
                        title,
                        message,
                        featureRequestId,
                        FEATURE_REQUEST
                );
            }
        }

        log.info("Notification created for {} users who upvoted/subscribed to feature request with id {} ", usersToNotify.size(), featureRequestId);
    }


    /**
     * Broadcast notification to ALL users
     */
    public void notifyAllUsers(
            String title,
            String message) {
        List<UUID> userIds = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        if (userIds.isEmpty()) {
            log.info("No users to notify");
            return;
        }

        if (userIds.size() > 100) {
            notifyInBatches(
                    userIds,
                    NotificationType.FEATURE_ANNOUNCEMENT,
                    title,
                    message,
                    null,
                    SYSTEM
            );
        } else {
            for (UUID userId : userIds) {
                notifyUser(
                        userId,
                        NotificationType.SUBSCRIBED_UPDATE,
                        title,
                        message,
                        null,
                        SYSTEM
                );
            }
        }

        log.info("Broadcast notification sent to {} users", userIds.size());
    }

    /**
     * Get user's notifications with pagination
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(
            UUID userId, int page, int size
    ) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("No user found")
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(notificationMapper::toDTO);
    }

    /**
     * Mark specific notification as read
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only mark your own notifications as read");
        }
        notification.setRead(true);
        notificationRepository.save(notification);

        log.info("Notification {} marked as read by user {}", notificationId, userId);
    }

    /**
     * Mark all user's notifications as read
     */
    public void markAllAsRead(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        if (notifications.isEmpty()) {
            log.info("No unread notifications for user {}", userId);
            return;
        }

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);

        log.info("Marked {} notifications as read for user {}", notifications.size(), userId);
    }

    /**
     * Get user's unread notifications
     */
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return notifications.stream().map(notificationMapper::toDTO).toList();
    }

    /**
     * Get count of unread notifications
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Helper method for batch notification creation
     */
    private void notifyInBatches(
            List<UUID> usersToNotify,
            NotificationType type,
            String title,
            String message,
            UUID featureRequestId,
            String relatedEntityType) {
        int batchSize = 100;
        List<Notification> notificationBatch = new ArrayList<>();

        for (int i = 0; i < usersToNotify.size(); i++) {
            UUID userId = usersToNotify.get(i);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));


            Notification notification = Notification.builder()
                    .title(title)
                    .message(message)
                    .user(user)
                    .type(type)
                    .relatedEntityId(featureRequestId)
                    .relatedEntityType(relatedEntityType)
                    .createdAt(Instant.now())
                    .isRead(false)
                    .build();

            notificationBatch.add(notification);

            if (notificationBatch.size() == batchSize || i == usersToNotify.size() - 1) {
                notificationRepository.saveAll(notificationBatch);
                log.info("Saved batch of {} notifications", notificationBatch.size());
                notificationBatch.clear();
            }
        }
    }
}
