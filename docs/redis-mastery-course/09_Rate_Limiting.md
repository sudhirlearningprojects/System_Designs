# Module 9: Rate Limiting

## 🎯 Learning Objectives

- Implement sliding window rate limiter with Lua scripts
- Token bucket algorithm in Redis
- Per-user, per-IP, and global rate limits
- Spring interceptor integration

---

## 9.1 Sliding Window (Lua Script)

```lua
-- src/main/resources/lua/rate_limit_sliding_window.lua
-- KEYS[1] = rate limit key
-- ARGV[1] = window size (seconds)
-- ARGV[2] = max requests
-- ARGV[3] = current timestamp (ms)

local key = KEYS[1]
local window = tonumber(ARGV[1]) * 1000
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local window_start = now - window

-- Remove expired entries
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

-- Count current requests in window
local count = redis.call('ZCARD', key)

if count < limit then
    -- Allowed: add this request
    redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
    redis.call('EXPIRE', key, ARGV[1])
    return 1  -- ALLOWED
else
    return 0  -- REJECTED
end
```

---

## 9.2 Rate Limiter Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> slidingWindowScript;

    @PostConstruct
    public void init() {
        // Load Lua script
    }

    public boolean isAllowed(String identifier, int maxRequests, int windowSeconds) {
        String key = "rate:" + identifier;
        long now = System.currentTimeMillis();

        Long result = redis.execute(
            slidingWindowScript,
            List.of(key),
            String.valueOf(windowSeconds),
            String.valueOf(maxRequests),
            String.valueOf(now)
        );

        boolean allowed = result != null && result == 1L;
        if (!allowed) {
            log.warn("Rate limit exceeded for: {}", identifier);
        }
        return allowed;
    }

    public RateLimitInfo getRateLimitInfo(String identifier, int maxRequests, int windowSeconds) {
        String key = "rate:" + identifier;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // Count current requests
        Long count = redis.opsForZSet().count(key, windowStart, now);
        long remaining = Math.max(0, maxRequests - (count != null ? count : 0));

        return new RateLimitInfo(maxRequests, remaining, windowSeconds);
    }
}
```

---

## 9.3 Spring Interceptor

```java
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = request.getRemoteAddr();
        String key = clientIp + ":" + request.getRequestURI();

        if (!rateLimiter.isAllowed(key, 100, 60)) {  // 100 req/min
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Too Many Requests\"}");
            return false;
        }

        // Add rate limit headers
        RateLimitInfo info = rateLimiter.getRateLimitInfo(key, 100, 60);
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining()));

        return true;
    }
}
```

---

## 9.4 Token Bucket (Alternative)

```java
public boolean tokenBucket(String key, int capacity, int refillRate, int refillInterval) {
    // Using Redis Hash: tokens, last_refill_time
    String tokensKey = "bucket:" + key + ":tokens";
    String timeKey = "bucket:" + key + ":time";

    String script = """
        local tokens = tonumber(redis.call('GET', KEYS[1]) or ARGV[1])
        local last_time = tonumber(redis.call('GET', KEYS[2]) or ARGV[4])
        local now = tonumber(ARGV[4])
        local elapsed = now - last_time
        local refill = math.floor(elapsed / tonumber(ARGV[3])) * tonumber(ARGV[2])
        tokens = math.min(tonumber(ARGV[1]), tokens + refill)
        if tokens >= 1 then
            tokens = tokens - 1
            redis.call('SET', KEYS[1], tokens)
            redis.call('SET', KEYS[2], now)
            return 1
        end
        return 0
    """;

    // Execute atomically via Lua
    return redis.execute(/* script with params */);
}
```

---

## 9.5 Limitations

- **Clock skew**: Distributed systems may have slight time differences
- **Lua atomicity**: Good for single Redis, not across cluster shards for same key
- **Memory**: Each unique identifier creates a sorted set
- **Precision**: Millisecond precision, not nanosecond

---

## ⏭️ Next: [Module 10: Transactions & Lua](10_Transactions_Lua.md)
