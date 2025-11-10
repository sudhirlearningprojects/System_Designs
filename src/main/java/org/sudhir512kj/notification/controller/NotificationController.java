package org.sudhir512kj.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.notification.dto.NotificationRequest;
import org.sudhir512kj.notification.dto.NotificationResponse;
import org.sudhir512kj.notification.model.Notification;
import org.sudhir512kj.notification.model.UserPreference;
import org.sudhir512kj.notification.service.NotificationService;
import org.sudhir512kj.notification.service.PreferenceService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final PreferenceService preferenceService;
    
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
        @Valid @RequestBody NotificationRequest request
    ) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(
        @PathVariable String userId
    ) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/user/{userId}/preferences")
    public ResponseEntity<UserPreference> getUserPreferences(
        @PathVariable String userId
    ) {
        UserPreference preference = preferenceService.getUserPreference(userId);
        return ResponseEntity.ok(preference);
    }
    
    @PutMapping("/user/{userId}/preferences")
    public ResponseEntity<UserPreference> updateUserPreferences(
        @PathVariable String userId,
        @RequestBody UserPreference preference
    ) {
        UserPreference updated = preferenceService.updatePreference(userId, preference);
        return ResponseEntity.ok(updated);
    }
}
