package org.sudhir512kj.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.notification.model.Notification;
import org.sudhir512kj.notification.model.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, Instant time);
}
