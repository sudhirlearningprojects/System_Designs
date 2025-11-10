package org.sudhir512kj.notification.model;

public enum DLQReason {
    MAX_RETRIES_EXCEEDED,
    INVALID_DATA,
    PROVIDER_ERROR,
    UNRETRYABLE_ERROR
}
