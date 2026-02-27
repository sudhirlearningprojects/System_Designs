package org.sudhir512kj.alertmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.alertmanager.model.NotificationChannel;

import java.util.List;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, String> {
    List<NotificationChannel> findByIdInAndEnabledTrue(List<String> ids);
}
