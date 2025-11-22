package org.sudhir512kj.instagram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.instagram.dto.NotificationResponse;
import org.sudhir512kj.instagram.model.Notification;
import org.sudhir512kj.instagram.model.User;
import org.sudhir512kj.instagram.repository.NotificationRepository;
import org.sudhir512kj.instagram.repository.UserRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void createNotification(Long userId, Long actorId, Notification.NotificationType type, String entityId) {
        if (userId.equals(actorId)) return;
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setEntityId(entityId);
        notificationRepository.save(notification);
        
        kafkaTemplate.send("notifications", userId.toString(), "New notification");
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        var actorIds = notifications.stream().map(Notification::getActorId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(actorIds).stream()
            .collect(Collectors.toMap(User::getUserId, u -> u));
        
        return notifications.map(n -> buildNotificationResponse(n, userMap.get(n.getActorId())));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse buildNotificationResponse(Notification n, User actor) {
        String message = switch (n.getType()) {
            case LIKE -> actor.getUsername() + " liked your post";
            case COMMENT -> actor.getUsername() + " commented on your post";
            case FOLLOW -> actor.getUsername() + " started following you";
            case MENTION -> actor.getUsername() + " mentioned you";
            case STORY_VIEW -> actor.getUsername() + " viewed your story";
        };
        
        return NotificationResponse.builder()
            .notificationId(n.getNotificationId())
            .actorId(actor.getUserId())
            .actorUsername(actor.getUsername())
            .actorProfilePictureUrl(actor.getProfilePictureUrl())
            .type(n.getType())
            .entityId(n.getEntityId())
            .message(message)
            .isRead(n.getIsRead())
            .createdAt(n.getCreatedAt())
            .build();
    }
}
