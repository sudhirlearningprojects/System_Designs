# Notification System - Theory and Algorithms

## Table of Contents
1. [Exponential Backoff Algorithm](#1-exponential-backoff-algorithm)
2. [Circuit Breaker Pattern](#2-circuit-breaker-pattern)
3. [Rate Limiting Algorithms](#3-rate-limiting-algorithms)
4. [Fan-Out Strategies](#4-fan-out-strategies)
5. [Idempotency Mechanisms](#5-idempotency-mechanisms)
6. [Priority Queue Management](#6-priority-queue-management)
7. [Database Sharding](#7-database-sharding)
8. [Caching Strategies](#8-caching-strategies)
9. [Dead Letter Queue](#9-dead-letter-queue)
10. [Distributed Tracing](#10-distributed-tracing)

---

## 1. Exponential Backoff Algorithm

### Theory

Exponential backoff is a retry strategy where the wait time between retries increases exponentially. This prevents overwhelming a failing service and gives it time to recover.

### Mathematical Formula

```
delay = initial_delay × (multiplier ^ attempt_number) + jitter
```

Where:
- `initial_delay` = 1 second
- `multiplier` = 2.0
- `attempt_number` = 0, 1, 2, 3, 4
- `jitter` = random(0, delay/10)

### Retry Schedule

| Attempt | Base Delay | Jitter Range | Total Delay Range |
|---------|-----------|--------------|-------------------|
| 1 | 1s | 0-100ms | 1.0s - 1.1s |
| 2 | 2s | 0-200ms | 2.0s - 2.2s |
| 3 | 4s | 0-400ms | 4.0s - 4.4s |
| 4 | 8s | 0-800ms | 8.0s - 8.8s |
| 5 | 16s | 0-1.6s | 16.0s - 17.6s |

### Why Jitter?

**Problem**: Without jitter, all failed requests retry at exactly the same time, causing a "thundering herd" problem.

**Solution**: Add random jitter (0-10% of delay) to spread out retry attempts.

### Implementation

```java
public long calculateDelay(int attemptNumber) {
    // Base exponential delay
    long delayMs = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber));
    
    // Add jitter (0-10% of delay)
    long jitter = ThreadLocalRandom.current().nextLong(0, delayMs / 10);
    
    return delayMs + jitter;
}
```

### When to Use

✅ **Use when:**
- Network errors (timeouts, connection refused)
- Server errors (5xx responses)
- Rate limit errors (429 responses)
- Transient failures

❌ **Don't use when:**
- Client errors (4xx responses)
- Invalid data
- Authentication failures
- Business logic errors

### Benefits

1. **Reduces Load**: Gives failing service time to recover
2. **Prevents Thundering Herd**: Jitter spreads out retries
3. **Automatic Recovery**: System self-heals from transient failures
4. **Cost Effective**: Fewer wasted retry attempts

---

## 2. Circuit Breaker Pattern

### Theory

Circuit breaker prevents cascading failures by stopping requests to a failing service. It has three states: CLOSED, OPEN, and HALF_OPEN.

### State Machine

```
CLOSED (Normal) → OPEN (Failing) → HALF_OPEN (Testing) → CLOSED (Recovered)
                                  ↓
                                OPEN (Still Failing)
```

### States Explained

#### CLOSED State (Normal Operation)
- All requests pass through
- Failures are counted in a sliding window
- If failure rate > threshold → transition to OPEN

#### OPEN State (Fast Fail)
- All requests are immediately rejected
- No calls to the failing service
- After wait duration → transition to HALF_OPEN

#### HALF_OPEN State (Testing Recovery)
- Allow limited test requests (e.g., 5 requests)
- If all succeed → transition to CLOSED
- If any fails → transition back to OPEN

### Configuration

```java
CircuitBreakerConfig.custom()
    .failureRateThreshold(50)              // Open if 50% fail
    .waitDurationInOpenState(30s)          // Wait 30s before testing
    .slidingWindowSize(100)                // Track last 100 calls
    .minimumNumberOfCalls(10)              // Need 10 calls before calculating rate
    .permittedNumberOfCallsInHalfOpenState(5)  // Allow 5 test calls
    .build()
```

### Example Scenario

```
Time    State       Requests    Failures    Action
0s      CLOSED      100         10          Normal (10% failure)
10s     CLOSED      100         60          Failure rate = 60% → OPEN
10s     OPEN        50          0           All rejected (fast fail)
40s     HALF_OPEN   5           0           Testing recovery
40s     CLOSED      100         5           Recovered (5% failure)
```

### Benefits

1. **Prevents Cascade Failures**: Stops calling failing services
2. **Fast Fail**: Immediate response instead of waiting for timeout
3. **Automatic Recovery**: Tests service health periodically
4. **Resource Protection**: Saves threads, connections, memory

### Real-World Example

```
SendGrid API is down
↓
Circuit breaker opens after 50% failures
↓
All email requests fail fast (no waiting)
↓
After 30 seconds, circuit breaker tests with 5 requests
↓
If SendGrid recovered, circuit closes
↓
Normal operation resumes
```

---

## 3. Rate Limiting Algorithms

### 3.1 Token Bucket Algorithm

#### Theory

Tokens are added to a bucket at a fixed rate. Each request consumes one token. If no tokens available, request is rejected.

#### Parameters
- **Capacity**: Maximum tokens in bucket (burst size)
- **Refill Rate**: Tokens added per second
- **Token Cost**: Tokens consumed per request

#### Example

```
Capacity: 100 tokens
Refill Rate: 10 tokens/second
Token Cost: 1 token/request

Time    Tokens    Request    Result
0s      100       Yes        Success (99 tokens left)
1s      100       Yes        Success (99 tokens left, refilled 10)
10s     100       Yes        Success (burst of 100 allowed)
10s     0         Yes        Rejected (no tokens)
11s     10        Yes        Success (refilled 10 tokens)
```

#### Implementation

```java
public boolean tryAcquire() {
    long now = System.currentTimeMillis();
    long elapsed = now - lastRefillTime;
    
    // Refill tokens based on elapsed time
    long tokensToAdd = (elapsed / 1000) * refillRate;
    tokens = Math.min(capacity, tokens + tokensToAdd);
    lastRefillTime = now;
    
    // Try to consume token
    if (tokens > 0) {
        tokens--;
        return true;
    }
    return false;
}
```

#### Use Cases
- API rate limiting
- Burst traffic handling
- Smooth traffic distribution

### 3.2 Sliding Window Algorithm

#### Theory

Tracks requests in a sliding time window. More accurate than fixed window but more memory intensive.

#### Example

```
Limit: 100 requests per minute
Window: 60 seconds

Time    Requests in Window    New Request    Result
0s      0                     Yes            Success (1 in window)
30s     50                    Yes            Success (51 in window)
60s     100                   Yes            Rejected (100 in window)
61s     99                    Yes            Success (oldest request expired)
```

#### Implementation

```java
public boolean allowRequest(String userId) {
    long now = System.currentTimeMillis();
    String key = "rate:" + userId;
    
    // Remove old requests outside window
    redis.zremrangeByScore(key, 0, now - windowMs);
    
    // Count requests in window
    long count = redis.zcard(key);
    
    if (count < limit) {
        redis.zadd(key, now, UUID.randomUUID().toString());
        redis.expire(key, windowMs / 1000);
        return true;
    }
    return false;
}
```

---

## 4. Fan-Out Strategies

### Theory

Fan-out distributes a single notification to multiple recipients efficiently.

### 4.1 Push-Based Fan-Out

#### How It Works

When a notification is created, immediately push to all recipients.

```
Notification Created
↓
For each recipient:
    Create individual notification
    Push to recipient's queue
```

#### Pros
- Real-time delivery
- Simple implementation
- Predictable latency

#### Cons
- High write load
- Slow for large recipient lists
- Wasted work if recipient offline

#### Use Case
- Small recipient lists (<1000)
- Real-time critical notifications
- High-value users

### 4.2 Pull-Based Fan-Out

#### How It Works

Store notification once. Recipients pull when they check.

```
Notification Created
↓
Store in shared location
↓
Recipients query when online
```

#### Pros
- Low write load
- Scales to millions
- No wasted work

#### Cons
- Higher read load
- Delayed delivery
- Complex querying

#### Use Case
- Large recipient lists (>1M)
- Non-time-sensitive notifications
- Social media feeds

### 4.3 Hybrid Fan-Out (Our Approach)

#### Strategy

- **Push**: For active users and critical notifications
- **Pull**: For inactive users and low-priority notifications

```java
public void fanOut(Notification notification, List<User> recipients) {
    List<User> activeUsers = recipients.stream()
        .filter(u -> u.isActive())
        .collect(Collectors.toList());
    
    List<User> inactiveUsers = recipients.stream()
        .filter(u -> !u.isActive())
        .collect(Collectors.toList());
    
    // Push to active users
    activeUsers.forEach(user -> pushNotification(user, notification));
    
    // Store for inactive users to pull later
    storeForPull(inactiveUsers, notification);
}
```

### 4.4 Batch Processing for Scale

#### Streaming Approach

```java
public void broadcastNotification(NotificationRequest request) {
    int batchSize = 1000;
    
    // Stream users in batches to avoid memory issues
    userRepository.streamAllUsers(batchSize)
        .forEach(userBatch -> {
            // Process batch in parallel
            userBatch.parallelStream().forEach(user -> {
                if (shouldSendToUser(user, request)) {
                    Notification notification = createNotification(user, request);
                    
                    // Distribute across Kafka partitions
                    int partition = Math.abs(user.getId().hashCode()) % partitionCount;
                    kafkaTemplate.send(topic, partition, notification.getId(), notification);
                }
            });
        });
}
```

#### Benefits
1. **Memory Efficient**: Process 1000 users at a time
2. **Parallel Processing**: Use all CPU cores
3. **Even Distribution**: Hash-based partitioning
4. **Fault Tolerant**: Kafka handles failures

---

## 5. Idempotency Mechanisms

### Theory

Idempotency ensures that processing the same request multiple times has the same effect as processing it once.

### Why Needed?

**Problem Scenarios:**
1. Network timeout → Client retries
2. Kafka rebalance → Message reprocessed
3. User double-clicks submit button
4. Webhook delivered twice

### Implementation Strategies

#### 5.1 Idempotency Key (Our Approach)

```java
public NotificationResponse sendNotification(NotificationRequest request) {
    String idempotencyKey = request.getIdempotencyKey();
    
    // Check if already processed
    if (redis.exists("idempotency:" + idempotencyKey)) {
        return NotificationResponse.builder()
            .success(false)
            .message("Duplicate request")
            .build();
    }
    
    // Mark as processing (atomic operation)
    boolean set = redis.setNX("idempotency:" + idempotencyKey, "1", 24, TimeUnit.HOURS);
    
    if (!set) {
        return NotificationResponse.builder()
            .success(false)
            .message("Duplicate request")
            .build();
    }
    
    // Process notification
    processNotification(request);
    
    return NotificationResponse.builder()
        .success(true)
        .build();
}
```

#### 5.2 Database Unique Constraint

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE,
    ...
);
```

If duplicate request arrives, database throws unique constraint violation.

#### 5.3 Distributed Lock

```java
public void processNotification(Notification notification) {
    String lockKey = "lock:notification:" + notification.getId();
    
    // Try to acquire lock
    boolean acquired = redis.setNX(lockKey, "1", 60, TimeUnit.SECONDS);
    
    if (!acquired) {
        log.warn("Notification {} already being processed", notification.getId());
        return;
    }
    
    try {
        // Process notification
        sendToProvider(notification);
    } finally {
        // Release lock
        redis.delete(lockKey);
    }
}
```

### Best Practices

1. **Client-Generated Keys**: Client provides idempotency key
2. **TTL**: Expire keys after 24 hours to save memory
3. **Atomic Operations**: Use Redis SETNX for atomicity
4. **Idempotent Operations**: Design operations to be naturally idempotent

---

## 6. Priority Queue Management

### Theory

Different notifications have different urgency levels. Priority queues ensure critical notifications are processed first.

### Priority Levels

| Priority | SLA | Use Case | Example |
|----------|-----|----------|---------|
| CRITICAL | <100ms | Security, OTP | "Login from new device" |
| HIGH | <1s | Transactions | "Payment received" |
| MEDIUM | <5s | Updates | "Order shipped" |
| LOW | Best effort | Marketing | "Weekly newsletter" |

### Kafka Topic Strategy

#### Separate Topics per Priority

```
notifications.critical  (50 partitions, 3 replicas)
notifications.high      (100 partitions, 3 replicas)
notifications.medium    (200 partitions, 2 replicas)
notifications.low       (200 partitions, 2 replicas)
```

#### Benefits
1. **Isolation**: Critical notifications not blocked by low priority
2. **Resource Allocation**: More workers for critical topics
3. **Retention**: Different retention policies per priority
4. **Monitoring**: Separate metrics per priority

### Worker Pool Allocation

```
Critical Workers: 50 instances (dedicated)
High Workers: 30 instances
Medium Workers: 20 instances
Low Workers: 10 instances (shared, can be scaled down)
```

### Priority Inversion Prevention

**Problem**: Low priority notification blocks high priority.

**Solution**: 
1. Separate topics (no shared queue)
2. Dedicated worker pools
3. Timeout mechanisms
4. Circuit breakers

---

## 7. Database Sharding

### Theory

Sharding distributes data across multiple databases to handle scale.

### Sharding Strategy: Hash-Based

```java
public int getShardId(String userId) {
    int hash = userId.hashCode();
    return Math.abs(hash) % SHARD_COUNT;
}
```

### Shard Distribution

```
Total Users: 500M
Shard Count: 64
Users per Shard: ~7.8M

Shard 0: users with hash % 64 == 0
Shard 1: users with hash % 64 == 1
...
Shard 63: users with hash % 64 == 63
```

### Routing Logic

```java
@Service
public class ShardedNotificationRepository {
    private final Map<Integer, DataSource> shards;
    
    public void save(Notification notification) {
        int shardId = getShardId(notification.getUserId());
        DataSource dataSource = shards.get(shardId);
        
        // Use shard-specific connection
        try (Connection conn = dataSource.getConnection()) {
            // Insert into shard
        }
    }
    
    public List<Notification> findByUserId(String userId) {
        int shardId = getShardId(userId);
        DataSource dataSource = shards.get(shardId);
        
        // Query specific shard
        try (Connection conn = dataSource.getConnection()) {
            // Query shard
        }
    }
}
```

### Cross-Shard Queries

**Problem**: How to query all notifications?

**Solutions**:
1. **Scatter-Gather**: Query all shards in parallel, merge results
2. **Global Index**: Maintain separate index for cross-shard queries
3. **Denormalization**: Duplicate data in global table

### Rebalancing

**When to Rebalance**: When adding/removing shards

**Strategy**: Consistent Hashing
- Minimizes data movement
- Only affects adjacent shards
- Gradual migration

---

## 8. Caching Strategies

### 8.1 Cache-Aside Pattern (Our Approach)

```java
public UserPreference getUserPreference(String userId) {
    // Try cache first
    UserPreference cached = redis.get("pref:" + userId);
    if (cached != null) {
        return cached;
    }
    
    // Cache miss - query database
    UserPreference pref = database.findById(userId);
    
    // Store in cache
    redis.set("pref:" + userId, pref, 1, TimeUnit.HOURS);
    
    return pref;
}
```

### 8.2 Write-Through Cache

```java
public void updatePreference(String userId, UserPreference pref) {
    // Update database
    database.save(pref);
    
    // Update cache
    redis.set("pref:" + userId, pref, 1, TimeUnit.HOURS);
}
```

### 8.3 Cache Invalidation

```java
public void updatePreference(String userId, UserPreference pref) {
    // Update database
    database.save(pref);
    
    // Invalidate cache (let next read populate)
    redis.delete("pref:" + userId);
}
```

### Cache Eviction Policies

1. **LRU (Least Recently Used)**: Evict oldest accessed items
2. **LFU (Least Frequently Used)**: Evict least accessed items
3. **TTL (Time To Live)**: Evict after expiration

### Cache Stampede Prevention

**Problem**: Cache expires → Many requests hit database simultaneously

**Solution**: Lock-based approach

```java
public UserPreference getUserPreference(String userId) {
    String cacheKey = "pref:" + userId;
    String lockKey = "lock:" + cacheKey;
    
    // Try cache
    UserPreference cached = redis.get(cacheKey);
    if (cached != null) return cached;
    
    // Try to acquire lock
    boolean acquired = redis.setNX(lockKey, "1", 10, TimeUnit.SECONDS);
    
    if (acquired) {
        try {
            // Query database
            UserPreference pref = database.findById(userId);
            redis.set(cacheKey, pref, 1, TimeUnit.HOURS);
            return pref;
        } finally {
            redis.delete(lockKey);
        }
    } else {
        // Wait for lock holder to populate cache
        Thread.sleep(100);
        return getUserPreference(userId); // Retry
    }
}
```

---

## 9. Dead Letter Queue (DLQ)

### Theory

DLQ stores messages that cannot be processed after multiple retry attempts.

### When to Move to DLQ

1. **Max Retries Exceeded**: Failed 5 times
2. **Invalid Data**: Malformed request
3. **Unretryable Error**: 4xx client errors
4. **Provider Rejection**: Permanent failure

### DLQ Entry Structure

```java
public class DLQEntry {
    String id;
    String notificationId;
    String payload;              // Original notification
    String errorMessage;         // Last error
    String stackTrace;           // For debugging
    int totalAttempts;           // Number of retries
    DLQReason reason;            // Why it failed
    Instant firstAttemptAt;      // When first tried
    Instant lastAttemptAt;       // When last tried
    boolean reprocessed;         // Has it been fixed?
}
```

### DLQ Processing

```java
@Scheduled(fixedDelay = 300000)  // Every 5 minutes
public void processDLQ() {
    List<DLQEntry> entries = dlqRepository.findReprocessable();
    
    for (DLQEntry entry : entries) {
        try {
            // Check if issue is resolved
            if (canReprocess(entry)) {
                Notification notification = deserialize(entry.getPayload());
                notificationService.send(notification);
                
                // Mark as reprocessed
                entry.setReprocessed(true);
                dlqRepository.save(entry);
            } else {
                // Alert ops team for manual intervention
                alertService.sendAlert("DLQ entry needs manual review: " + entry.getId());
            }
        } catch (Exception e) {
            log.error("Failed to reprocess DLQ entry", e);
        }
    }
}
```

### DLQ Monitoring

**Metrics to Track:**
1. DLQ entry rate
2. DLQ size
3. Reprocess success rate
4. Time in DLQ

**Alerts:**
- DLQ size > 1000 entries
- DLQ entry rate > 100/minute
- Entries older than 24 hours

---

## 10. Distributed Tracing

### Theory

Distributed tracing tracks a request across multiple services to understand latency and failures.

### Trace Structure

```
Trace ID: abc123
├── Span 1: API Gateway (5ms)
├── Span 2: Notification Service (10ms)
│   ├── Span 3: Preference Service (2ms)
│   │   └── Span 4: Redis Cache (1ms)
│   └── Span 5: Database Write (8ms)
├── Span 6: Kafka Publish (5ms)
└── Span 7: Email Worker (200ms)
    ├── Span 8: Circuit Breaker Check (1ms)
    └── Span 9: SendGrid API (199ms)

Total Latency: 230ms
```

### Implementation

```java
@Service
public class NotificationService {
    
    @NewSpan("send-notification")
    public void sendNotification(
        @SpanTag("userId") String userId,
        NotificationRequest request
    ) {
        Span span = tracer.currentSpan();
        span.tag("notification.type", request.getType().name());
        span.tag("notification.priority", request.getPriority().name());
        span.tag("notification.channels", request.getChannels().toString());
        
        try {
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

### Benefits

1. **Performance Analysis**: Identify slow components
2. **Debugging**: Trace request flow across services
3. **Dependency Mapping**: Understand service interactions
4. **Error Attribution**: Find root cause of failures

---

## Summary

This notification system combines multiple algorithms and patterns:

1. **Exponential Backoff**: Graceful retry with jitter
2. **Circuit Breaker**: Prevent cascade failures
3. **Rate Limiting**: Protect providers from overload
4. **Fan-Out**: Efficient broadcast to millions
5. **Idempotency**: Prevent duplicate processing
6. **Priority Queues**: SLA-based delivery
7. **Sharding**: Scale to 500M users
8. **Caching**: 95% cache hit rate
9. **DLQ**: Handle unrecoverable failures
10. **Tracing**: End-to-end observability

Each algorithm is chosen to solve specific challenges at scale while maintaining reliability and performance.
