# Rate Limiting & Throttling Problems

Design problems focused on traffic control, API rate limiting, and request throttling.

## 📚 Problems List

### Time-Window Based
1. **Logger Rate Limiter (LC 359)** ⭐
   - HashMap with timestamp
   - Simple 10-second window
   - **Difficulty**: Easy
   - **Key Concepts**: Fixed window, timestamp tracking

2. **Design Hit Counter (LC 362)** ⭐⭐⭐
   - **[COMPLETED - See Algorithm Design](../02-algorithm-design/design-hit-counter.md)**
   - Sliding window with queue
   - Circular array optimization
   - **Difficulty**: Medium

3. **Design Rate Limiter (System Design)** ⭐⭐⭐
   - Token bucket algorithm
   - Sliding window log
   - Fixed window counter
   - Leaky bucket
   - **Difficulty**: Hard

## 🎯 Rate Limiting Algorithms

### 1. Fixed Window Counter
```java
class FixedWindowRateLimiter {
    private Map<String, Integer> counters = new HashMap<>();
    private Map<String, Long> windows = new HashMap<>();
    private final int limit;
    private final long windowMs;
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        long windowStart = windows.getOrDefault(userId, 0L);
        
        if (now - windowStart >= windowMs) {
            // New window
            windows.put(userId, now);
            counters.put(userId, 1);
            return true;
        }
        
        int count = counters.getOrDefault(userId, 0);
        if (count < limit) {
            counters.put(userId, count + 1);
            return true;
        }
        
        return false;
    }
}
```

**Pros**: Simple, memory efficient
**Cons**: Burst at window boundaries

### 2. Sliding Window Log
```java
class SlidingWindowRateLimiter {
    private Map<String, Queue<Long>> logs = new HashMap<>();
    private final int limit;
    private final long windowMs;
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        Queue<Long> log = logs.computeIfAbsent(userId, k -> new LinkedList<>());
        
        // Remove old entries
        while (!log.isEmpty() && now - log.peek() >= windowMs) {
            log.poll();
        }
        
        if (log.size() < limit) {
            log.offer(now);
            return true;
        }
        
        return false;
    }
}
```

**Pros**: Accurate, no boundary issues
**Cons**: Memory intensive (stores all timestamps)

### 3. Token Bucket
```java
class TokenBucketRateLimiter {
    private Map<String, Bucket> buckets = new HashMap<>();
    private final int capacity;
    private final double refillRate; // tokens per second
    
    class Bucket {
        double tokens;
        long lastRefill;
        
        Bucket() {
            tokens = capacity;
            lastRefill = System.currentTimeMillis();
        }
    }
    
    public boolean allowRequest(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, k -> new Bucket());
        
        long now = System.currentTimeMillis();
        double elapsed = (now - bucket.lastRefill) / 1000.0;
        
        // Refill tokens
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillRate);
        bucket.lastRefill = now;
        
        if (bucket.tokens >= 1) {
            bucket.tokens -= 1;
            return true;
        }
        
        return false;
    }
}
```

**Pros**: Smooth traffic, allows bursts
**Cons**: Complex implementation

### 4. Leaky Bucket
```java
class LeakyBucketRateLimiter {
    private Map<String, Queue<Long>> buckets = new HashMap<>();
    private final int capacity;
    private final long leakIntervalMs;
    
    public boolean allowRequest(String userId) {
        Queue<Long> bucket = buckets.computeIfAbsent(userId, k -> new LinkedList<>());
        long now = System.currentTimeMillis();
        
        // Leak old requests
        while (!bucket.isEmpty() && now - bucket.peek() >= leakIntervalMs) {
            bucket.poll();
        }
        
        if (bucket.size() < capacity) {
            bucket.offer(now);
            return true;
        }
        
        return false;
    }
}
```

**Pros**: Constant output rate
**Cons**: Can reject requests even when idle

## 📊 Algorithm Comparison

| Algorithm | Accuracy | Memory | Burst Handling | Complexity |
|-----------|----------|--------|----------------|------------|
| Fixed Window | Low | O(1) | Poor | Easy |
| Sliding Window Log | High | O(n) | Good | Medium |
| Sliding Window Counter | Medium | O(1) | Good | Medium |
| Token Bucket | High | O(1) | Excellent | Hard |
| Leaky Bucket | High | O(n) | Poor | Medium |

## 🎯 When to Use Which?

### Fixed Window Counter
- **Use**: Simple rate limiting, low traffic
- **Example**: Limit 100 requests per minute
- **Companies**: Small APIs, internal services

### Sliding Window Log
- **Use**: Accurate rate limiting, compliance
- **Example**: Financial APIs, payment systems
- **Companies**: Stripe, PayPal

### Token Bucket
- **Use**: Allow bursts, smooth traffic
- **Example**: AWS API Gateway, CDN
- **Companies**: AWS, Cloudflare, Google Cloud

### Leaky Bucket
- **Use**: Constant output rate, queue processing
- **Example**: Message queues, background jobs
- **Companies**: RabbitMQ, Kafka consumers

## 🔑 Key Concepts

### 1. Rate vs Quota
```
Rate: Requests per time window (100 req/min)
Quota: Total requests allowed (1000 req/day)
```

### 2. Distributed Rate Limiting
```java
// Using Redis
class DistributedRateLimiter {
    private Jedis redis;
    
    public boolean allowRequest(String userId) {
        String key = "rate_limit:" + userId;
        long count = redis.incr(key);
        
        if (count == 1) {
            redis.expire(key, 60); // 60 seconds
        }
        
        return count <= 100;
    }
}
```

### 3. Multi-Tier Rate Limiting
```java
// Per user + per IP + global
public boolean allowRequest(String userId, String ip) {
    return allowUser(userId) && 
           allowIP(ip) && 
           allowGlobal();
}
```

## 💡 Real-World Examples

### Twitter API
```
Standard: 900 requests / 15 min
Search: 180 requests / 15 min
Streaming: 1 connection
```

### GitHub API
```
Authenticated: 5000 requests / hour
Unauthenticated: 60 requests / hour
Search: 30 requests / minute
```

### Stripe API
```
Standard: 100 requests / second
Burst: 1000 requests / second (short duration)
```

## 🚀 Implementation Patterns

### Pattern 1: Decorator Pattern
```java
@RateLimit(requests = 100, window = 60)
public Response handleRequest() {
    // Business logic
}
```

### Pattern 2: Middleware
```java
app.use(rateLimiter({
    windowMs: 60 * 1000,
    max: 100
}));
```

### Pattern 3: API Gateway
```
AWS API Gateway → Rate Limiting → Lambda
Cloudflare → Rate Limiting → Origin Server
```

## 📈 Monitoring & Metrics

### Key Metrics
```
1. Request rate (req/sec)
2. Rejection rate (%)
3. P99 latency
4. Burst size
5. Token refill rate
```

### Alerting
```
Alert if:
- Rejection rate > 10%
- Burst > 2x normal
- Latency > 100ms
```

## 🔗 Related Topics

- [Design Hit Counter](../02-algorithm-design/design-hit-counter.md)
- [Logger Rate Limiter (LC 359)](./logger-rate-limiter.md) - Coming soon
- [Design Rate Limiter (System)](./design-rate-limiter-system.md) - Coming soon

## 📚 Additional Resources

### Articles
- [Rate Limiting Strategies](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- [Leaky Bucket Algorithm](https://en.wikipedia.org/wiki/Leaky_bucket)

### Real Implementations
- [Redis Rate Limiter](https://redis.io/commands/incr#pattern-rate-limiter)
- [Nginx Rate Limiting](https://www.nginx.com/blog/rate-limiting-nginx/)
- [Kong Rate Limiting](https://docs.konghq.com/hub/kong-inc/rate-limiting/)

---

**Status**: 1/3 problems completed (Design Hit Counter)
**Next**: Logger Rate Limiter, Design Rate Limiter System
