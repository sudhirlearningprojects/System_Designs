# Rate Limiter - Complete Coding Guide

## System Design Overview

**Problem**: Limit API requests per user (e.g., 100 requests/hour)

**Algorithms**:
1. Fixed Window
2. Sliding Window
3. Token Bucket
4. Leaky Bucket

## SOLID Principles

- **SRP**: Each algorithm in separate class
- **OCP**: Add new algorithms without modifying existing
- **LSP**: All algorithms implement same interface

## Design Patterns

1. **Strategy Pattern**: Different rate limiting algorithms
2. **Factory Pattern**: Create limiter based on type

## Complete Implementation

```java
import java.util.*;
import java.util.concurrent.*;

interface RateLimiter {
    boolean allowRequest(String userId);
}

class FixedWindowLimiter implements RateLimiter {
    private int maxRequests;
    private long windowMs;
    private Map<String, WindowData> windows = new ConcurrentHashMap<>();
    
    static class WindowData {
        long windowStart;
        int count;
    }
    
    FixedWindowLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        WindowData data = windows.computeIfAbsent(userId, k -> new WindowData());
        
        synchronized (data) {
            if (now - data.windowStart >= windowMs) {
                data.windowStart = now;
                data.count = 0;
            }
            
            if (data.count < maxRequests) {
                data.count++;
                return true;
            }
            return false;
        }
    }
}

class SlidingWindowLimiter implements RateLimiter {
    private int maxRequests;
    private long windowMs;
    private Map<String, Queue<Long>> requests = new ConcurrentHashMap<>();
    
    SlidingWindowLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        Queue<Long> userRequests = requests.computeIfAbsent(userId, k -> new LinkedList<>());
        
        synchronized (userRequests) {
            while (!userRequests.isEmpty() && now - userRequests.peek() >= windowMs) {
                userRequests.poll();
            }
            
            if (userRequests.size() < maxRequests) {
                userRequests.offer(now);
                return true;
            }
            return false;
        }
    }
}

class TokenBucketLimiter implements RateLimiter {
    private int capacity;
    private int refillRate;
    private Map<String, BucketData> buckets = new ConcurrentHashMap<>();
    
    static class BucketData {
        int tokens;
        long lastRefill;
    }
    
    TokenBucketLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }
    
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        BucketData bucket = buckets.computeIfAbsent(userId, k -> {
            BucketData b = new BucketData();
            b.tokens = capacity;
            b.lastRefill = now;
            return b;
        });
        
        synchronized (bucket) {
            long elapsed = now - bucket.lastRefill;
            int tokensToAdd = (int)(elapsed / 1000) * refillRate;
            bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
            bucket.lastRefill = now;
            
            if (bucket.tokens > 0) {
                bucket.tokens--;
                return true;
            }
            return false;
        }
    }
}

class RateLimiterFactory {
    public static RateLimiter create(String type, int limit, long window) {
        return switch (type) {
            case "FIXED" -> new FixedWindowLimiter(limit, window);
            case "SLIDING" -> new SlidingWindowLimiter(limit, window);
            case "TOKEN" -> new TokenBucketLimiter(limit, limit / 10);
            default -> throw new IllegalArgumentException("Unknown type");
        };
    }
}

public class RateLimiterDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter Demo ===\n");
        
        // Test Fixed Window (5 requests per 2 seconds)
        System.out.println("=== Fixed Window (5 req/2s) ===");
        RateLimiter fixed = RateLimiterFactory.create("FIXED", 5, 2000);
        testLimiter(fixed, "user1", 8);
        
        Thread.sleep(2100);
        System.out.println("After window reset:");
        testLimiter(fixed, "user1", 3);
        
        // Test Sliding Window
        System.out.println("\n=== Sliding Window (5 req/2s) ===");
        RateLimiter sliding = RateLimiterFactory.create("SLIDING", 5, 2000);
        testLimiter(sliding, "user2", 8);
        
        // Test Token Bucket
        System.out.println("\n=== Token Bucket (10 capacity, 5/s refill) ===");
        RateLimiter token = RateLimiterFactory.create("TOKEN", 10, 1000);
        testLimiter(token, "user3", 12);
    }
    
    static void testLimiter(RateLimiter limiter, String user, int requests) {
        for (int i = 1; i <= requests; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.println("Request " + i + ": " + (allowed ? "✓ ALLOWED" : "✗ BLOCKED"));
        }
    }
}
```

## Algorithm Comparison

| Algorithm | Pros | Cons |
|-----------|------|------|
| Fixed Window | Simple, memory efficient | Burst at window edges |
| Sliding Window | Accurate, smooth | More memory |
| Token Bucket | Handles bursts | Complex refill logic |
| Leaky Bucket | Smooth output | Drops requests |

## Interview Questions

**Q: Which algorithm for API rate limiting?**
A: Sliding Window for accuracy, Token Bucket for burst handling

**Q: Distributed rate limiting?**
A: Use Redis with INCR + EXPIRE or Lua scripts

**Q: Handle clock skew?**
A: Use logical timestamps or centralized time service

**Q: Scale to millions of users?**
A: Redis cluster, consistent hashing, local cache

Run: https://www.jdoodle.com/online-java-compiler
