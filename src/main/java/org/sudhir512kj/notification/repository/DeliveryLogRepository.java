package org.sudhir512kj.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.notification.model.DeliveryLog;
import java.util.List;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, String> {
    List<DeliveryLog> findByNotificationIdOrderByTimestampDesc(String notificationId);
    List<DeliveryLog> findByUserId(String userId);
}
