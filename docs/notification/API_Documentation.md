# Notification System - API Documentation

## Base URL
```
http://localhost:8089/api/v1
```

## Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer <token>
```

---

## Endpoints

### 1. Send Notification

Send a notification to a user across specified channels.

**Endpoint:** `POST /notifications`

**Request Body:**
```json
{
  "userId": "user123",
  "type": "TRANSACTIONAL",
  "priority": "HIGH",
  "channels": ["EMAIL", "PUSH"],
  "templateId": "order-confirmation",
  "templateData": {
    "orderId": "ORD-123",
    "amount": 99.99,
    "customerName": "John Doe"
  },
  "scheduledAt": "2024-01-15T10:00:00Z",
  "idempotencyKey": "order-123-notification"
}
```

**Response:**
```json
{
  "success": true,
  "notificationId": "notif-uuid-123",
  "message": "Notification queued successfully"
}
```

**Status Codes:**
- `200 OK` - Notification queued successfully
- `400 Bad Request` - Invalid request data
- `409 Conflict` - Duplicate idempotency key
- `429 Too Many Requests` - Rate limit exceeded

---

### 2. Get User Notifications

Retrieve notification history for a user.

**Endpoint:** `GET /notifications/user/{userId}`

**Response:**
```json
[
  {
    "id": "notif-123",
    "userId": "user123",
    "type": "TRANSACTIONAL",
    "priority": "HIGH",
    "channels": ["EMAIL", "PUSH"],
    "status": "DELIVERED",
    "createdAt": "2024-01-15T10:00:00Z",
    "sentAt": "2024-01-15T10:00:01Z",
    "deliveredAt": "2024-01-15T10:00:05Z"
  }
]
```

---

### 3. Get User Preferences

Retrieve notification preferences for a user.

**Endpoint:** `GET /notifications/user/{userId}/preferences`

**Response:**
```json
{
  "userId": "user123",
  "enabledChannels": {
    "TRANSACTIONAL": ["EMAIL", "SMS", "PUSH"],
    "PROMOTIONAL": ["EMAIL"],
    "ALERT": ["EMAIL", "SMS", "PUSH"],
    "SYSTEM": ["EMAIL"]
  },
  "globalChannelSettings": {
    "EMAIL": true,
    "SMS": true,
    "PUSH": true,
    "IN_APP": true,
    "WEBSOCKET": true
  },
  "quietHours": {
    "start": "22:00:00",
    "end": "08:00:00"
  },
  "timezone": "America/New_York",
  "updatedAt": "2024-01-15T10:00:00Z"
}
```

---

### 4. Update User Preferences

Update notification preferences for a user.

**Endpoint:** `PUT /notifications/user/{userId}/preferences`

**Request Body:**
```json
{
  "enabledChannels": {
    "TRANSACTIONAL": ["EMAIL", "SMS"],
    "PROMOTIONAL": [],
    "ALERT": ["EMAIL", "SMS", "PUSH"]
  },
  "globalChannelSettings": {
    "EMAIL": true,
    "SMS": true,
    "PUSH": false
  },
  "quietHours": {
    "start": "23:00:00",
    "end": "07:00:00"
  },
  "timezone": "America/Los_Angeles"
}
```

**Response:**
```json
{
  "userId": "user123",
  "enabledChannels": { ... },
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

## Enums

### NotificationType
- `TRANSACTIONAL` - Order confirmations, receipts
- `PROMOTIONAL` - Marketing, offers
- `ALERT` - Security alerts, warnings
- `SYSTEM` - System maintenance, updates

### NotificationPriority
- `CRITICAL` - OTP, security alerts (<100ms)
- `HIGH` - Payment confirmations (<1s)
- `MEDIUM` - Order updates (<5s)
- `LOW` - Marketing emails (best effort)

### Channel
- `EMAIL` - Email notifications
- `SMS` - SMS notifications
- `PUSH` - Push notifications (iOS/Android)
- `IN_APP` - In-app notifications
- `WEBSOCKET` - Real-time WebSocket notifications

### NotificationStatus
- `PENDING` - Queued for processing
- `SCHEDULED` - Scheduled for future delivery
- `PROCESSING` - Currently being processed
- `SENT` - Sent to provider
- `DELIVERED` - Confirmed delivery
- `FAILED` - Delivery failed
- `CANCELLED` - Cancelled by user

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "details": [
    "userId is required",
    "channels cannot be empty"
  ]
}
```

### 429 Too Many Requests
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests",
  "retryAfter": 60
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "requestId": "req-123"
}
```

---

## Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| POST /notifications | 1000 requests | 60 seconds |
| GET /notifications/user/{userId} | 100 requests | 60 seconds |
| PUT /notifications/user/{userId}/preferences | 10 requests | 60 seconds |

---

## Webhooks

### Delivery Status Webhook

Receive delivery status updates from third-party providers.

**Endpoint:** `POST /webhooks/delivery-status`

**Payload:**
```json
{
  "notificationId": "notif-123",
  "channel": "EMAIL",
  "status": "DELIVERED",
  "providerId": "sendgrid-msg-123",
  "timestamp": "2024-01-15T10:00:05Z"
}
```
