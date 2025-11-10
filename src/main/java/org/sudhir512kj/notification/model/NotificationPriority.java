package org.sudhir512kj.notification.model;

public enum NotificationPriority {
    CRITICAL,  // OTP, security alerts - <100ms
    HIGH,      // Payment confirmations - <1s
    MEDIUM,    // Order updates - <5s
    LOW        // Marketing emails - best effort
}
