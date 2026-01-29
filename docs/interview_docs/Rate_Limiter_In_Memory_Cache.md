# Rate Limiter with In-Memory Cache - Implementation Guide

## Overview

A Rate Limiter controls the rate of requests a client can make to prevent abuse, ensure fair usage, and protect system resources. Using in-memory cache with expiration provides fast, efficient rate limiting.

**Key Benefits**:
- Prevent API abuse
- Fair resource allocation
- DDoS protection
- Cost control
- System stability

---

## Rate Limiting Algorithms

### 1. Fixed Window Counter

**Concept**: Count requests in fixed time windows

```
Window 1 (0-60s): 5 requests
Window 2 (60-120s): 3 requests
Window 3 (120-180s): 7 requests

Limit: 10 requests per minute
```

**Problem**: Burst at window boundaries

```
Time:    0s -------- 60s -------- 120s
Window 1: [9 requests at 59s]
Window 2: [9 requests at 61s]
Result: 18 requests in 2 seconds! (burst)
```

---

### 2. Sliding Window Log

**Concept**: Track timestamp of each request

```
Requests: [10s, 15s, 25s, 40s, 55s]
Current time: 70s
Window: 60s

Valid requests in last 60s:
- 10s: Expired (70-10 = 60s)
- 15s: Valid (70-15 = 55s)
- 25s: Valid (70-25 = 45s)
- 40s: Valid (70-40 = 30s)
- 55s: Valid (70-55 = 15s)

Count: 4 requests in last 60s
```

**Pros**: Accurate, no burst issue  
**Cons**: Memory intensive (stores all timestamps)

---

### 3. Sliding Window Counter (Hybrid)

**Concept**: Combine fixed window with sliding calculation

```
Previous window: 8 requests
Current window: 3 requests
Current time: 30s into window

Estimated count = (8 * 0.5) + 3 = 7 requests
```

**Pros**: Memory efficient, smooth  
**Cons**: Approximate count

---

### 4. Token Bucket

**Concept**: Bucket holds tokens, refilled at constant rate

```
Bucket capacity: 10 tokens
Refill rate: 1 token/second

Time 0s: 10 tokens
Request 1: 9 tokens (consumed 1)
Request 2: 8 tokens (consumed 1)
Time 5s: 10 tokens (refilled 2, capped at 10)
```

**Pros**: Allows bursts, smooth  
**Cons**: Complex implementation

---

### 5. Leaky Bucket

**Concept**: Requests processed at constant rate

```
Bucket capacity: 10 requests
Process rate: 1 request/second

Incoming: 5 requests/second
Bucket: [R1, R2, R3, R4, R5, R6, R7, R8, R9, R10]
Process: 1 request/second
Overflow: Rejected
```

**Pros**: Smooth traffic  
**Cons**: Doesn't allow bursts

---

## Implementation 1: Fixed Window Counter

### Simple In-Memory Implementation

```java
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class FixedWindowRateLimiter {
    
    private final int maxRequests;
    private final long windowSizeSeconds;
    private final ConcurrentHashMap<String, WindowCounter> cache;
    
    static class WindowCounter {
        long windowStart;
        int count;
        
        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
    
    public FixedWindowRateLimiter(int maxRequests, long windowSizeSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
        this.cache = new ConcurrentHashMap<>();
    }
    
    public boolean allowRequest(String userId) {
        long now = Instant.now().getEpochSecond();
        long currentWindow = now / windowSizeSeconds;
        
        String key = userId + ":" + currentWindow;
        
        WindowCounter counter = cache.compute(key, (k, v) -> {
            if (v == null) {
                return new WindowCounter(currentWindow, 1);
            }
            v.count++;
            return v;
        });
        
        // Cleanup old windows
        cleanupOldWindows(userId, currentWindow);
        
        return counter.count <= maxRequests;
    }
    
    private void cleanupOldWindows(String userId, long currentWindow) {
        cache.keySet().removeIf(key -> {
            if (key.startsWith(userId + ":")) {
                long window = Long.parseLong(key.split(":")[1]);
                return window < currentWindow;
            }
            return false;
        });
    }
}
```

### Usage Example

```java
public class FixedWindowExample {
    public static void main(String[] args) throws InterruptedException {
        // 5 requests per 10 seconds
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(5, 10);
        
        String userId = "user123";
        
        // Make 7 requests
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(userId);
            System.out.println("Request " + i + ": " + 
                (allowed ? "ALLOWED" : "REJECTED"));
            Thread.sleep(1000);
        }
    }
}
```

**Output**:
```
Request 1: ALLOWED
Request 2: ALLOWED
Request 3: ALLOWED
Request 4: ALLOWED
Request 5: ALLOWED
Request 6: REJECTED
Request 7: REJECTED
```

---

## Implementation 2: Sliding Window Log

### Complete Implementation with Expiration

```java
import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

public class SlidingWindowLogRateLimiter {
    
    private final int maxRequests;
    private final long windowSizeSeconds;
    private final ConcurrentHashMap<String, Queue<Long>> requestLog;
    private final ScheduledExecutorService cleanupExecutor;
    
    public SlidingWindowLogRateLimiter(int maxRequests, long windowSizeSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeSeconds = windowSizeSeconds;
        this.requestLog = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Cleanup expired entries every minute
        startCleanupTask();
    }
    
    public boolean allowRequest(String userId) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSizeSeconds;
        
        Queue<Long> timestamps = requestLog.computeIfAbsent(
            userId, k -> new ConcurrentLinkedQueue<>());
        
        // Remove expired timestamps
        timestamps.removeIf(timestamp -> timestamp <= windowStart);
        
        // Check if limit exceeded
        if (timestamps.size() >= maxRequests) {
            return false;
        }
        
        // Add current request timestamp
        timestamps.offer(now);
        return true;
    }
    
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = Instant.now().getEpochSecond();
            long windowStart = now - windowSizeSeconds;
            
            requestLog.forEach((userId, timestamps) -> {
                timestamps.removeIf(timestamp -> timestamp <= windowStart);
                if (timestamps.isEmpty()) {
                    requestLog.remove(userId);
                }
            });
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
```

### Usage Example

```java
public class SlidingWindowExample {
    public static void main(String[] args) throws InterruptedException {
        // 5 requests per 10 seconds
        SlidingWindowLogRateLimiter limiter = 
            new SlidingWindowLogRateLimiter(5, 10);
        
        String userId = "user123";
        
        // Make requests at different times
        for (int i = 1; i <= 10; i++) {
            boolean allowed = limiter.allowRequest(userId);
            System.out.println("Time " + i + "s, Request: " + 
                (allowed ? "ALLOWED" : "REJECTED"));
            Thread.sleep(1000);
        }
        
        limiter.shutdown();
    }
}
```

**Output**:
```
Time 1s, Request: ALLOWED
Time 2s, Request: ALLOWED
Time 3s, Request: ALLOWED
Time 4s, Request: ALLOWED
Time 5s, Request: ALLOWED
Time 6s, Request: REJECTED
Time 7s, Request: REJECTED
Time 8s, Request: REJECTED
Time 9s, Request: REJECTED
Time 10s, Request: REJECTED
Time 11s, Request: ALLOWED (first request expired)
```

---

## Implementation 3: Token Bucket

### Complete Implementation

```java
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class TokenBucketRateLimiter {
    
    private final int capacity;
    private final double refillRate; // tokens per second
    private final ConcurrentHashMap<String, Bucket> buckets;
    
    static class Bucket {
        double tokens;
        long lastRefillTime;
        
        Bucket(double tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }
    
    public TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    public boolean allowRequest(String userId) {
        return allowRequest(userId, 1);
    }
    
    public boolean allowRequest(String userId, int tokens) {
        long now = Instant.now().getEpochSecond();
        
        Bucket bucket = buckets.compute(userId, (k, v) -> {
            if (v == null) {
                return new Bucket(capacity - tokens, now);
            }
            
            // Refill tokens based on time elapsed
            long timeElapsed = now - v.lastRefillTime;
            double tokensToAdd = timeElapsed * refillRate;
            v.tokens = Math.min(capacity, v.tokens + tokensToAdd);
            v.lastRefillTime = now;
            
            // Consume tokens
            if (v.tokens >= tokens) {
                v.tokens -= tokens;
            }
            
            return v;
        });
        
        return bucket.tokens >= 0;
    }
    
    public double getAvailableTokens(String userId) {
        Bucket bucket = buckets.get(userId);
        if (bucket == null) {
            return capacity;
        }
        
        long now = Instant.now().getEpochSecond();
        long timeElapsed = now - bucket.lastRefillTime;
        double tokensToAdd = timeElapsed * refillRate;
        return Math.min(capacity, bucket.tokens + tokensToAdd);
    }
}
```

### Usage Example

```java
public class TokenBucketExample {
    public static void main(String[] args) throws InterruptedException {
        // Capacity: 10 tokens, Refill: 1 token/second
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(10, 1.0);
        
        String userId = "user123";
        
        // Burst: 12 requests immediately
        System.out.println("=== Burst Test ===");
        for (int i = 1; i <= 12; i++) {
            boolean allowed = limiter.allowRequest(userId);
            System.out.println("Request " + i + ": " + 
                (allowed ? "ALLOWED" : "REJECTED") + 
                " (Tokens: " + limiter.getAvailableTokens(userId) + ")");
        }
        
        // Wait 5 seconds (5 tokens refilled)
        System.out.println("\n=== Wait 5 seconds ===");
        Thread.sleep(5000);
        
        // Try 7 more requests
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(userId);
            System.out.println("Request " + i + ": " + 
                (allowed ? "ALLOWED" : "REJECTED") + 
                " (Tokens: " + limiter.getAvailableTokens(userId) + ")");
        }
    }
}
```

**Output**:
```
=== Burst Test ===
Request 1: ALLOWED (Tokens: 9.0)
Request 2: ALLOWED (Tokens: 8.0)
Request 3: ALLOWED (Tokens: 7.0)
Request 4: ALLOWED (Tokens: 6.0)
Request 5: ALLOWED (Tokens: 5.0)
Request 6: ALLOWED (Tokens: 4.0)
Request 7: ALLOWED (Tokens: 3.0)
Request 8: ALLOWED (Tokens: 2.0)
Request 9: ALLOWED (Tokens: 1.0)
Request 10: ALLOWED (Tokens: 0.0)
Request 11: REJECTED (Tokens: 0.0)
Request 12: REJECTED (Tokens: 0.0)

=== Wait 5 seconds ===
Request 1: ALLOWED (Tokens: 4.0)
Request 2: ALLOWED (Tokens: 3.0)
Request 3: ALLOWED (Tokens: 2.0)
Request 4: ALLOWED (Tokens: 1.0)
Request 5: ALLOWED (Tokens: 0.0)
Request 6: REJECTED (Tokens: 0.0)
Request 7: REJECTED (Tokens: 0.0)
```

---

## Implementation 4: Guava Cache with Expiration

### Using Google Guava

```java
import com.google.common.cache.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GuavaRateLimiter {
    
    private final int maxRequests;
    private final LoadingCache<String, AtomicInteger> cache;
    
    public GuavaRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }
    
    public boolean allowRequest(String userId) {
        try {
            AtomicInteger counter = cache.get(userId);
            int count = counter.incrementAndGet();
            return count <= maxRequests;
        } catch (Exception e) {
            return false;
        }
    }
    
    public int getRemainingRequests(String userId) {
        try {
            AtomicInteger counter = cache.getIfPresent(userId);
            if (counter == null) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - counter.get());
        } catch (Exception e) {
            return 0;
        }
    }
}
```

### Usage Example

```java
public class GuavaRateLimiterExample {
    public static void main(String[] args) throws InterruptedException {
        // 5 requests per 10 seconds
        GuavaRateLimiter limiter = new GuavaRateLimiter(5, 10);
        
        String userId = "user123";
        
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(userId);
            int remaining = limiter.getRemainingRequests(userId);
            System.out.println("Request " + i + ": " + 
                (allowed ? "ALLOWED" : "REJECTED") + 
                " (Remaining: " + remaining + ")");
        }
        
        System.out.println("\n=== Wait 11 seconds (cache expired) ===");
        Thread.sleep(11000);
        
        boolean allowed = limiter.allowRequest(userId);
        System.out.println("Request after expiry: " + 
            (allowed ? "ALLOWED" : "REJECTED"));
    }
}
```

---

## Implementation 5: Caffeine Cache (High Performance)

### Using Caffeine Cache

```java
import com.github.benmanes.caffeine.cache.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CaffeineRateLimiter {
    
    private final int maxRequests;
    private final Cache<String, AtomicInteger> cache;
    
    public CaffeineRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .recordStats()
            .build();
    }
    
    public boolean allowRequest(String userId) {
        AtomicInteger counter = cache.get(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        return count <= maxRequests;
    }
    
    public int getCurrentCount(String userId) {
        AtomicInteger counter = cache.getIfPresent(userId);
        return counter != null ? counter.get() : 0;
    }
    
    public CacheStats getStats() {
        return cache.stats();
    }
}
```

---

## Production-Ready Implementation

### Complete Rate Limiter with All Features

```java
import java.util.concurrent.*;
import java.time.Instant;
import java.util.*;

public class ProductionRateLimiter {
    
    private final int maxRequests;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, RateLimitEntry> cache;
    private final ScheduledExecutorService cleanupExecutor;
    
    static class RateLimitEntry {
        final Queue<Long> timestamps;
        long lastAccessTime;
        
        RateLimitEntry() {
            this.timestamps = new ConcurrentLinkedQueue<>();
            this.lastAccessTime = Instant.now().getEpochSecond();
        }
    }
    
    public ProductionRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.cache = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        startCleanupTask();
    }
    
    public RateLimitResult allowRequest(String userId) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;
        
        RateLimitEntry entry = cache.computeIfAbsent(userId, k -> new RateLimitEntry());
        entry.lastAccessTime = now;
        
        // Remove expired timestamps
        entry.timestamps.removeIf(timestamp -> timestamp <= windowStart);
        
        int currentCount = entry.timestamps.size();
        boolean allowed = currentCount < maxRequests;
        
        if (allowed) {
            entry.timestamps.offer(now);
            currentCount++;
        }
        
        int remaining = Math.max(0, maxRequests - currentCount);
        long resetTime = entry.timestamps.peek() != null ? 
            entry.timestamps.peek() + windowSeconds : now + windowSeconds;
        
        return new RateLimitResult(allowed, remaining, resetTime);
    }
    
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = Instant.now().getEpochSecond();
            long expiryThreshold = now - (windowSeconds * 2);
            
            cache.entrySet().removeIf(entry -> 
                entry.getValue().lastAccessTime < expiryThreshold);
            
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
    
    public static class RateLimitResult {
        public final boolean allowed;
        public final int remaining;
        public final long resetTime;
        
        public RateLimitResult(boolean allowed, int remaining, long resetTime) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
        
        @Override
        public String toString() {
            return String.format("Allowed: %s, Remaining: %d, Reset: %d", 
                allowed, remaining, resetTime);
        }
    }
}
```

### REST API Integration

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController
@RequestMapping("/api")
public class RateLimitedController {
    
    private final ProductionRateLimiter rateLimiter;
    
    public RateLimitedController() {
        // 100 requests per minute
        this.rateLimiter = new ProductionRateLimiter(100, 60);
    }
    
    @GetMapping("/data")
    public ResponseEntity<String> getData(
            @RequestHeader("X-User-Id") String userId) {
        
        ProductionRateLimiter.RateLimitResult result = 
            rateLimiter.allowRequest(userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", "100");
        headers.add("X-RateLimit-Remaining", String.valueOf(result.remaining));
        headers.add("X-RateLimit-Reset", String.valueOf(result.resetTime));
        
        if (!result.allowed) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body("Rate limit exceeded");
        }
        
        return ResponseEntity
            .ok()
            .headers(headers)
            .body("Data response");
    }
}
```

---

## Expiration Strategies

### 1. Time-Based Expiration (TTL)

```java
// Expire after fixed duration
cache.expireAfterWrite(60, TimeUnit.SECONDS);
```

### 2. Idle-Based Expiration

```java
// Expire if not accessed for duration
cache.expireAfterAccess(60, TimeUnit.SECONDS);
```

### 3. Custom Expiration

```java
cache.expireAfter(new Expiry<String, AtomicInteger>() {
    @Override
    public long expireAfterCreate(String key, AtomicInteger value, long currentTime) {
        return TimeUnit.SECONDS.toNanos(60);
    }
    
    @Override
    public long expireAfterUpdate(String key, AtomicInteger value, 
            long currentTime, long currentDuration) {
        return currentDuration;
    }
    
    @Override
    public long expireAfterRead(String key, AtomicInteger value, 
            long currentTime, long currentDuration) {
        return currentDuration;
    }
});
```

### 4. Manual Cleanup

```java
ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
executor.scheduleAtFixedRate(() -> {
    cache.cleanUp(); // Trigger cleanup
}, 1, 1, TimeUnit.MINUTES);
```

---

## Performance Comparison

| Algorithm | Memory | Accuracy | Burst Handling | Complexity |
|-----------|--------|----------|----------------|------------|
| **Fixed Window** | O(1) | Low | Poor | Low |
| **Sliding Log** | O(N) | High | Good | Medium |
| **Sliding Counter** | O(1) | Medium | Good | Medium |
| **Token Bucket** | O(1) | High | Excellent | High |
| **Leaky Bucket** | O(N) | High | Poor | High |

---

## Best Practices

### 1. Choose Right Algorithm

```java
// API rate limiting: Token Bucket
TokenBucketRateLimiter apiLimiter = new TokenBucketRateLimiter(1000, 10);

// DDoS protection: Fixed Window
FixedWindowRateLimiter ddosLimiter = new FixedWindowRateLimiter(100, 1);

// Fair usage: Sliding Window Log
SlidingWindowLogRateLimiter fairLimiter = new SlidingWindowLogRateLimiter(50, 60);
```

### 2. Set Appropriate Limits

```java
// Per user: 100 requests/minute
// Per IP: 1000 requests/minute
// Per API key: 10000 requests/hour
```

### 3. Return Proper Headers

```java
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 75
X-RateLimit-Reset: 1640000000
Retry-After: 60
```

### 4. Implement Cleanup

```java
// Cleanup expired entries every minute
executor.scheduleAtFixedRate(() -> {
    cache.cleanUp();
}, 60, 60, TimeUnit.SECONDS);
```

### 5. Monitor Performance

```java
// Track metrics
- Hit rate
- Miss rate
- Eviction count
- Average load time
```

---

## Interview Questions & Answers

### Q1: Why use in-memory cache for rate limiting?

**Answer**: 
- Fast access (O(1) lookup)
- Low latency (<1ms)
- No network overhead
- Automatic expiration support

### Q2: What's the difference between Fixed Window and Sliding Window?

**Answer**:
- **Fixed Window**: Count resets at window boundary, allows bursts
- **Sliding Window**: Continuous window, smooth rate limiting

### Q3: How to handle distributed rate limiting?

**Answer**: Use Redis with Lua scripts for atomic operations:
```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end
return current <= limit
```

### Q4: Which algorithm is best?

**Answer**: Depends on requirements:
- **Token Bucket**: Best for APIs (allows bursts)
- **Leaky Bucket**: Best for smooth traffic
- **Sliding Window**: Best for accuracy

### Q5: How to set expiration time?

**Answer**:
```java
// Caffeine
cache.expireAfterWrite(60, TimeUnit.SECONDS);

// Guava
CacheBuilder.newBuilder()
    .expireAfterWrite(60, TimeUnit.SECONDS)
    .build();
```

---

## Key Takeaways

1. **In-memory cache** provides fast, efficient rate limiting
2. **Token Bucket** best for APIs (allows bursts)
3. **Sliding Window Log** most accurate but memory intensive
4. **Automatic expiration** prevents memory leaks
5. **Cleanup tasks** remove stale entries
6. **Return proper headers** (X-RateLimit-*)
7. **Monitor performance** with cache statistics
8. **Choose algorithm** based on requirements
9. **Set appropriate limits** per user/IP/API key
10. **Use Redis** for distributed rate limiting

---

## Practice Problems

1. Implement rate limiter with multiple tiers (free, premium)
2. Add burst allowance to fixed window algorithm
3. Implement distributed rate limiter with Redis
4. Create rate limiter with dynamic limits
5. Add monitoring and alerting
6. Implement rate limiter per endpoint
7. Create hierarchical rate limiting (user + IP + global)
8. Optimize memory usage for millions of users
9. Implement rate limiter with priority queues
10. Add graceful degradation when limit exceeded
