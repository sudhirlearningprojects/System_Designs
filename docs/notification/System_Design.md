# Distributed Notification System - System Design

## 1. Problem Statement

Design a highly scalable, reliable distributed notification system that can deliver messages across multiple channels (email, SMS, push, in-app) to millions of users with guaranteed delivery, fault tolerance, and respect for user preferences.

## 2. Functional Requirements

### Core Features
1. **Multi-Channel Support**: Email, SMS, Push (iOS/Android), In-App, WebSocket
2. **Notification Types**: Real-time, Scheduled, Recurring, Broadcast
3. **Personalization**: Template-based with dynamic content injection
4. **User Preferences**: Channel opt-in/opt-out, notification type preferences, quiet hours
5. **Priority Levels**: Critical (OTP), High (alerts), Medium (updates), Low (marketing)
6. **Delivery Tracking**: Status tracking, read receipts, analytics
7. **Batch Operations**: Bulk notifications with fan-out
8. **Rate Limiting**: Per-user, per-channel rate limits

### API Requirements
- Send single notification
- Send batch notifications
- Schedule notifications
- Cancel scheduled notifications
- Update user preferences
- Query delivery status
- Retrieve notification history

## 3. Non-Functional Requirements

| Requirement | Target | Strategy |
|------------|--------|----------|
| **Throughput** | 10M notifications/min | Horizontal scaling, Kafka partitioning |
| **Latency** | <100ms (critical), <5s (normal) | Priority queues, dedicated workers |
| **Availability** | 99.99% uptime | Multi-region, circuit breakers |
| **Reliability** | At-least-once delivery | Retry with exponential backoff, DLQ |
| **Scalability** | 500M users | Microservices, sharding |
| **Data Retention** | 90 days (logs), 1 year (analytics) | Time-series DB, archival |

## 4. High-Level Design

### 4.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway Layer                         │
│                    (Rate Limiting, Auth, LB)                     │
└────────────┬────────────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────────────┐
│                   Notification Service (Core)                    │
│  - Validation  - Preference Check  - Template Rendering          │
│  - Priority Assignment  - Deduplication                          │
└────────────┬────────────────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────────────────────┐
│                    Apache Kafka (Message Bus)                    │
│  Topics: notifications.critical, notifications.high,             │
│          notifications.medium, notifications.low                 │
└──┬────────┬────────┬────────┬────────┬──────────────────────────┘
   │        │        │        │        │
   ▼        ▼        ▼        ▼        ▼
┌──────┐ ┌─────┐ ┌──────┐ ┌──────┐ ┌────────┐
│Email │ │ SMS │ │ Push │ │In-App│ │WebSocket│
│Worker│ │Worker│ │Worker│ │Worker│ │ Worker │
└──┬───┘ └──┬──┘ └──┬───┘ └──┬───┘ └───┬────┘
   │        │       │        │         │
   ▼        ▼       ▼        ▼         ▼
┌──────────────────────────────────────────┐
│      Third-Party Provider Layer          │
│  SendGrid, Twilio, FCM, APNS, etc.       │
└──────────────────────────────────────────┘
```

### 4.2 Core Components

#### 4.2.1 Notification Service (Core)
- **Responsibilities**: 
  - Request validation and enrichment
  - User preference lookup and filtering
  - Template rendering with personalization
  - Priority assignment and routing
  - Idempotency check (deduplication)
  - Metrics collection

#### 4.2.2 Channel Workers
- **Email Worker**: SendGrid, AWS SES integration
- **SMS Worker**: Twilio, AWS SNS integration
- **Push Worker**: FCM (Android), APNS (iOS)
- **In-App Worker**: Database persistence
- **WebSocket Worker**: Real-time delivery

#### 4.2.3 Scheduler Service
- **Responsibilities**:
  - Cron-based scheduling
  - Time-zone aware delivery
  - Recurring notification management
  - Batch job orchestration

#### 4.2.4 Preference Service
- **Responsibilities**:
  - User preference CRUD
  - Channel opt-in/opt-out
  - Quiet hours management
  - Preference caching (Redis)

#### 4.2.5 Delivery Tracker
- **Responsibilities**:
  - Status updates (sent, delivered, failed, read)
  - Webhook handling from providers
  - Analytics aggregation
  - SLA monitoring

## 5. Low-Level Design

### 5.1 Data Models

```java
// Notification Entity
@Entity
public class Notification {
    @Id
    private String id;  // UUID
    private String userId;
    private NotificationType type;  // TRANSACTIONAL, PROMOTIONAL, ALERT
    private NotificationPriority priority;  // CRITICAL, HIGH, MEDIUM, LOW
    private List<Channel> channels;  // EMAIL, SMS, PUSH, IN_APP
    private String templateId;
    private Map<String, Object> templateData;
    private NotificationStatus status;
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private int retryCount;
    private String idempotencyKey;
    private Instant createdAt;
}

// User Preference Entity
@Entity
public class UserPreference {
    @Id
    private String userId;
    private Map<NotificationType, Set<Channel>> enabledChannels;
    private Map<Channel, Boolean> globalChannelSettings;
    private QuietHours quietHours;
    private String timezone;
    private Instant updatedAt;
}

// Delivery Log Entity
@Entity
public class DeliveryLog {
    @Id
    private String id;
    private String notificationId;
    private String userId;
    private Channel channel;
    private DeliveryStatus status;  // PENDING, SENT, DELIVERED, FAILED, READ
    private String providerId;  // External provider message ID
    private String errorMessage;
    private int attemptNumber;
    private Instant timestamp;
}

// Dead Letter Queue Entry
@Entity
public class DLQEntry {
    @Id
    private String id;
    private String notificationId;
    private String payload;
    private String errorMessage;
    private String stackTrace;
    private int totalAttempts;
    private Instant firstAttemptAt;
    private Instant lastAttemptAt;
    private DLQReason reason;  // MAX_RETRIES, INVALID_DATA, PROVIDER_ERROR
}
```

### 5.2 Retry Mechanism with Exponential Backoff

```java
@Service
public class RetryService {
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_DELAY_MS = 1000;  // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    public void scheduleRetry(Notification notification, int attemptNumber) {
        if (attemptNumber >= MAX_RETRIES) {
            moveToDLQ(notification, "Max retries exceeded");
            return;
        }
        
        long delayMs = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber));
        
        // Add jitter to prevent thundering herd
        long jitter = ThreadLocalRandom.current().nextLong(0, delayMs / 10);
        delayMs += jitter;
        
        // Schedule retry using Kafka with delay
        kafkaTemplate.send(
            "notifications.retry",
            notification.getId(),
            notification,
            delayMs
        );
    }
    
    private void moveToDLQ(Notification notification, String reason) {
        DLQEntry entry = new DLQEntry();
        entry.setNotificationId(notification.getId());
        entry.setReason(reason);
        entry.setTotalAttempts(notification.getRetryCount());
        dlqRepository.save(entry);
        
        // Alert monitoring system
        alertService.sendAlert("Notification moved to DLQ: " + notification.getId());
    }
}
```

### 5.3 Fan-Out Strategy

```java
@Service
public class FanOutService {
    
    // For broadcast notifications (e.g., system announcements)
    public void broadcastNotification(NotificationRequest request) {
        // Use Kafka partitioning for parallel processing
        int partitionCount = 100;
        
        // Stream users in batches to avoid memory issues
        userRepository.streamAllUsers(1000)
            .forEach(userBatch -> {
                userBatch.parallelStream().forEach(user -> {
                    if (shouldSendToUser(user, request)) {
                        Notification notification = createNotification(user, request);
                        
                        // Partition by userId hash for even distribution
                        int partition = Math.abs(user.getId().hashCode()) % partitionCount;
                        
                        kafkaTemplate.send(
                            getTopicByPriority(notification.getPriority()),
                            partition,
                            notification.getId(),
                            notification
                        );
                    }
                });
            });
    }
    
    // For segment-based notifications (e.g., users in a city)
    public void sendToSegment(NotificationRequest request, UserSegment segment) {
        // Use database streaming with cursor-based pagination
        String cursor = null;
        do {
            Page<User> users = userRepository.findBySegment(segment, cursor, 1000);
            
            users.getContent().parallelStream().forEach(user -> {
                sendNotification(user, request);
            });
            
            cursor = users.getNextCursor();
        } while (cursor != null);
    }
}
```

### 5.4 Channel Worker Implementation

```java
@Service
public class EmailWorker {
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    
    @KafkaListener(topics = "notifications.email", concurrency = "10")
    public void processEmailNotification(Notification notification) {
        try {
            // Check rate limit
            if (!rateLimiter.tryAcquire(notification.getUserId())) {
                scheduleRetry(notification, "Rate limit exceeded");
                return;
            }
            
            // Circuit breaker for provider resilience
            circuitBreaker.run(() -> {
                EmailRequest email = buildEmail(notification);
                EmailResponse response = emailProvider.send(email);
                
                updateDeliveryStatus(notification, DeliveryStatus.SENT, response.getMessageId());
            }, throwable -> {
                handleFailure(notification, throwable);
                return null;
            });
            
        } catch (Exception e) {
            log.error("Failed to process email notification", e);
            retryService.scheduleRetry(notification, notification.getRetryCount() + 1);
        }
    }
    
    private void handleFailure(Notification notification, Throwable error) {
        if (isRetryable(error)) {
            retryService.scheduleRetry(notification, notification.getRetryCount() + 1);
        } else {
            moveToDLQ(notification, error.getMessage());
        }
    }
    
    private boolean isRetryable(Throwable error) {
        // Network errors, timeouts, 5xx errors are retryable
        // 4xx errors (invalid email, etc.) are not retryable
        return error instanceof TimeoutException ||
               error instanceof IOException ||
               (error instanceof HttpException && ((HttpException) error).getStatusCode() >= 500);
    }
}
```

### 5.5 Preference Management

```java
@Service
public class PreferenceService {
    
    @Cacheable(value = "user-preferences", key = "#userId")
    public UserPreference getUserPreference(String userId) {
        return preferenceRepository.findById(userId)
            .orElseGet(() -> createDefaultPreference(userId));
    }
    
    public boolean shouldSendNotification(String userId, NotificationType type, Channel channel) {
        UserPreference pref = getUserPreference(userId);
        
        // Check global channel setting
        if (!pref.getGlobalChannelSettings().getOrDefault(channel, true)) {
            return false;
        }
        
        // Check type-specific channel setting
        Set<Channel> enabledChannels = pref.getEnabledChannels().get(type);
        if (enabledChannels == null || !enabledChannels.contains(channel)) {
            return false;
        }
        
        // Check quiet hours
        if (isInQuietHours(pref)) {
            // Only allow critical notifications during quiet hours
            return type == NotificationType.CRITICAL;
        }
        
        return true;
    }
    
    private boolean isInQuietHours(UserPreference pref) {
        if (pref.getQuietHours() == null) return false;
        
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(pref.getTimezone()));
        LocalTime currentTime = now.toLocalTime();
        
        return currentTime.isAfter(pref.getQuietHours().getStart()) &&
               currentTime.isBefore(pref.getQuietHours().getEnd());
    }
}
```

### 5.6 Idempotency and Deduplication

```java
@Service
public class IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final long IDEMPOTENCY_TTL_HOURS = 24;
    
    public boolean isDuplicate(String idempotencyKey) {
        String key = "idempotency:" + idempotencyKey;
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
        return result == null || !result;
    }
    
    public void markAsProcessed(String notificationId) {
        String key = "processed:" + notificationId;
        redisTemplate.opsForValue()
            .set(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
    }
}
```

## 6. Scalability Design

### 6.1 Horizontal Scaling Strategy

| Component | Scaling Strategy | Metric |
|-----------|------------------|--------|
| **Notification Service** | Auto-scale based on CPU/Memory | >70% CPU |
| **Kafka Consumers** | Partition-based parallelism | Lag > 10K messages |
| **Channel Workers** | Independent scaling per channel | Queue depth |
| **Database** | Read replicas + Sharding by userId | Connection pool saturation |
| **Redis** | Cluster mode with sharding | Memory usage > 80% |

### 6.2 Database Sharding

```
Shard Key: userId (consistent hashing)
Shard Count: 64 shards

Shard Selection: shard_id = hash(userId) % 64

Tables to Shard:
- notifications (by userId)
- delivery_logs (by userId)
- user_preferences (by userId)

Global Tables (no sharding):
- templates
- notification_types
```

### 6.3 Kafka Topic Design

```
Topic: notifications.critical
  Partitions: 50
  Replication Factor: 3
  Retention: 7 days
  
Topic: notifications.high
  Partitions: 100
  Replication Factor: 3
  Retention: 3 days
  
Topic: notifications.medium
  Partitions: 200
  Replication Factor: 2
  Retention: 1 day
  
Topic: notifications.low
  Partitions: 200
  Replication Factor: 2
  Retention: 12 hours

Topic: notifications.dlq
  Partitions: 10
  Replication Factor: 3
  Retention: 30 days
```

## 7. Failure Handling

### 7.1 Failure Scenarios

| Failure Type | Detection | Recovery Strategy | SLA Impact |
|--------------|-----------|-------------------|------------|
| **Provider Outage** | Circuit breaker | Fallback provider | Minimal |
| **Database Failure** | Health check | Read replica failover | <1 min |
| **Kafka Broker Down** | Consumer lag | Automatic rebalancing | <30 sec |
| **Worker Crash** | Heartbeat timeout | Restart + reprocess | <2 min |
| **Network Partition** | Timeout | Retry with backoff | <5 min |

### 7.2 Circuit Breaker Configuration

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker emailCircuitBreaker() {
        return CircuitBreaker.of("email-provider", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)  // Open if 50% fail
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(5)
            .build());
    }
}
```

### 7.3 Dead Letter Queue Processing

```java
@Service
public class DLQProcessor {
    
    @Scheduled(fixedDelay = 300000)  // Every 5 minutes
    public void processDLQ() {
        List<DLQEntry> entries = dlqRepository.findReprocessable();
        
        for (DLQEntry entry : entries) {
            try {
                // Attempt manual reprocessing or alert ops team
                if (canReprocess(entry)) {
                    Notification notification = deserialize(entry.getPayload());
                    notificationService.send(notification);
                    dlqRepository.delete(entry);
                } else {
                    alertService.sendAlert("Manual intervention required: " + entry.getId());
                }
            } catch (Exception e) {
                log.error("Failed to reprocess DLQ entry", e);
            }
        }
    }
}
```

## 8. Monitoring and Observability

### 8.1 Key Metrics

```java
@Component
public class NotificationMetrics {
    private final MeterRegistry registry;
    
    // Throughput metrics
    public void recordNotificationSent(Channel channel, NotificationPriority priority) {
        registry.counter("notifications.sent",
            "channel", channel.name(),
            "priority", priority.name()
        ).increment();
    }
    
    // Latency metrics
    public void recordDeliveryLatency(Channel channel, long latencyMs) {
        registry.timer("notifications.delivery.latency",
            "channel", channel.name()
        ).record(latencyMs, TimeUnit.MILLISECONDS);
    }
    
    // Failure metrics
    public void recordFailure(Channel channel, String reason) {
        registry.counter("notifications.failed",
            "channel", channel.name(),
            "reason", reason
        ).increment();
    }
    
    // DLQ metrics
    public void recordDLQEntry(String reason) {
        registry.counter("notifications.dlq",
            "reason", reason
        ).increment();
    }
}
```

### 8.2 Distributed Tracing

```java
@Service
public class NotificationService {
    
    @NewSpan("send-notification")
    public void sendNotification(@SpanTag("userId") String userId, NotificationRequest request) {
        Span span = tracer.currentSpan();
        span.tag("notification.type", request.getType().name());
        span.tag("notification.channels", request.getChannels().toString());
        
        try {
            // Process notification
            processNotification(userId, request);
            span.tag("status", "success");
        } catch (Exception e) {
            span.tag("status", "error");
            span.tag("error.message", e.getMessage());
            throw e;
        }
    }
}
```

### 8.3 Alerting Rules

```yaml
alerts:
  - name: HighFailureRate
    condition: notifications.failed.rate > 5%
    duration: 5m
    severity: critical
    
  - name: DLQBacklog
    condition: notifications.dlq.count > 1000
    duration: 10m
    severity: high
    
  - name: HighLatency
    condition: notifications.delivery.latency.p99 > 10s
    duration: 5m
    severity: warning
    
  - name: ConsumerLag
    condition: kafka.consumer.lag > 100000
    duration: 5m
    severity: critical
```

## 9. Security Considerations

### 9.1 Data Protection
- **Encryption at Rest**: AES-256 for sensitive data (phone numbers, emails)
- **Encryption in Transit**: TLS 1.3 for all communications
- **PII Masking**: Mask sensitive data in logs
- **Access Control**: RBAC for admin operations

### 9.2 API Security
```java
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    @PostMapping
    @PreAuthorize("hasRole('NOTIFICATION_SENDER')")
    @RateLimit(requests = 1000, window = 60, scope = RateLimit.Scope.USER)
    public ResponseEntity<NotificationResponse> sendNotification(
        @Valid @RequestBody NotificationRequest request,
        @AuthenticationPrincipal User user
    ) {
        // Validate user can send to specified recipients
        if (!authService.canSendTo(user, request.getRecipients())) {
            throw new ForbiddenException("Insufficient permissions");
        }
        
        NotificationResponse response = notificationService.send(request);
        return ResponseEntity.ok(response);
    }
}
```

## 10. Cost Optimization

### 10.1 Cost Breakdown (Monthly for 1B notifications)

| Component | Cost | Optimization |
|-----------|------|--------------|
| **Kafka** | $5,000 | Use tiered storage |
| **Database** | $8,000 | Archive old data |
| **Redis** | $2,000 | Eviction policies |
| **Email (SendGrid)** | $15,000 | Batch sending |
| **SMS (Twilio)** | $50,000 | Use cheaper providers |
| **Push (FCM/APNS)** | Free | N/A |
| **Compute (ECS)** | $10,000 | Spot instances |
| **Total** | **$90,000** | **$0.09 per notification** |

### 10.2 Optimization Strategies
1. **Batch Processing**: Group notifications to same user
2. **Smart Routing**: Use cheapest available channel
3. **Deduplication**: Prevent duplicate sends
4. **Compression**: Compress Kafka messages
5. **Auto-scaling**: Scale down during off-peak hours

## 11. API Design

### 11.1 Send Notification
```http
POST /api/v1/notifications
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": "user123",
  "type": "TRANSACTIONAL",
  "priority": "HIGH",
  "channels": ["EMAIL", "PUSH"],
  "templateId": "order-confirmation",
  "templateData": {
    "orderId": "ORD-123",
    "amount": 99.99
  },
  "scheduledAt": "2024-01-15T10:00:00Z",
  "idempotencyKey": "order-123-notification"
}
```

### 11.2 Batch Send
```http
POST /api/v1/notifications/batch
Content-Type: application/json

{
  "notifications": [
    { "userId": "user1", ... },
    { "userId": "user2", ... }
  ]
}
```

### 11.3 Update Preferences
```http
PUT /api/v1/users/{userId}/preferences
Content-Type: application/json

{
  "enabledChannels": {
    "TRANSACTIONAL": ["EMAIL", "SMS", "PUSH"],
    "PROMOTIONAL": ["EMAIL"]
  },
  "quietHours": {
    "start": "22:00",
    "end": "08:00"
  },
  "timezone": "America/New_York"
}
```

## 12. Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     AWS Cloud (Multi-Region)                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │   Region 1   │         │   Region 2   │                  │
│  │  (Primary)   │◄───────►│  (Failover)  │                  │
│  └──────────────┘         └──────────────┘                  │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  Route 53 (Global DNS + Health Checks)          │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  CloudFront (API Gateway Caching)               │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  ALB (Application Load Balancer)                │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  ECS Fargate (Notification Service)             │        │
│  │  Auto-scaling: 10-100 instances                 │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  MSK (Managed Kafka)                            │        │
│  │  3 brokers per AZ, 9 total                      │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  RDS PostgreSQL (Multi-AZ)                      │        │
│  │  Primary + 2 Read Replicas                      │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │  ElastiCache Redis (Cluster Mode)               │        │
│  │  6 shards, 2 replicas per shard                 │        │
│  └─────────────────────────────────────────────────┘        │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## 13. Testing Strategy

### 13.1 Load Testing
```javascript
// k6 load test script
import http from 'k6/http';

export let options = {
  stages: [
    { duration: '5m', target: 1000 },   // Ramp up
    { duration: '10m', target: 10000 }, // Peak load
    { duration: '5m', target: 0 },      // Ramp down
  ],
};

export default function() {
  http.post('http://api/notifications', JSON.stringify({
    userId: `user${__VU}`,
    type: 'TRANSACTIONAL',
    channels: ['EMAIL'],
    templateId: 'test-template'
  }));
}
```

### 13.2 Chaos Engineering
- Randomly kill worker pods
- Simulate provider outages
- Inject network latency
- Corrupt Kafka messages
- Fill up disk space

## 14. Future Enhancements

1. **AI-Powered Optimization**
   - Best time to send predictions
   - Channel preference learning
   - Content personalization

2. **Advanced Analytics**
   - A/B testing framework
   - Conversion tracking
   - User engagement scoring

3. **Multi-Tenancy**
   - Tenant isolation
   - Custom rate limits per tenant
   - White-label support

4. **Rich Media Support**
   - Image/video attachments
   - Interactive notifications
   - AMP for email

---

**Design Principles Applied:**
- ✅ Reliability: Retry + DLQ + Circuit Breaker
- ✅ Scalability: Kafka partitioning + Horizontal scaling
- ✅ Availability: Multi-region + Failover
- ✅ Performance: Priority queues + Caching
- ✅ Observability: Metrics + Tracing + Alerts
- ✅ Security: Encryption + RBAC + Rate limiting
