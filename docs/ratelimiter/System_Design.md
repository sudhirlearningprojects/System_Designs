# Distributed API Rate Limiter - System Design

## 1. Overview

A highly available, scalable, and configurable distributed API rate limiter that provides multi-layered protection with different algorithms and scopes. Available in both **API-based** and **Annotation-based** implementations.

### What is Rate Limiting?

Rate limiting is like a **traffic control system** for your APIs:

```
Without Rate Limiting:
🚗🚗🚗🚗🚗🚗🚗🚗🚗🚗 → [API Server] → 💥 Crash!

With Rate Limiting:
🚗🚗🚗 → [Rate Limiter] → 🚗🚗🚗 → [API Server] → ✅ Healthy!
      ↓
   🚫🚗🚗🚗 (Blocked excess traffic)
```

### Why Do We Need It?

#### 1. **System Protection**
```
Scenario: E-commerce flash sale
- Expected: 1,000 users
- Reality: 100,000 users hit the site
- Without rate limiting: Server crashes, nobody can buy
- With rate limiting: System stays up, 1,000 users can buy successfully
```

#### 2. **Fair Resource Sharing**
```
Scenario: API with 1000 requests/second capacity
- User A (bot): Tries to make 900 requests/second
- User B (human): Tries to make 10 requests/second
- User C (human): Tries to make 10 requests/second

Without rate limiting: A gets 900, B gets 50, C gets 50
With rate limiting: A gets 333, B gets 333, C gets 333 (fair!)
```

#### 3. **Cost Control**
```
Cloud API costs: $0.001 per request
- Without limits: Runaway bot makes 10M requests = $10,000 bill 😱
- With limits: Maximum 100K requests = $100 bill 😊
```

### Key Features
- **Annotation-Based**: Declarative rate limiting with @RateLimit annotations
- **Multi-Algorithm Support**: Sliding Window, Token Bucket, Fixed Window, Leaky Bucket
- **Multi-Scope Protection**: User, IP, API Key, Tenant, Global level rate limiting
- **High Availability**: Redis-based distributed state with failover
- **SpEL Support**: Custom key expressions with Spring Expression Language
- **Multi-Layer Defense**: Combine multiple rate limits for comprehensive protection
- **Security**: DDoS protection and abuse prevention
- **Developer-Friendly**: Clean annotations, no configuration files needed

## 2. High-Level Design

### 2.1 The Big Picture

Think of our rate limiter as a **smart bouncer** at a popular nightclub:

```
👥 Customers (API Clients)
    ↓
🚪 Bouncer (Rate Limiter) - "You can enter, you wait, you're banned"
    ↓
🎉 Nightclub (Your API Server) - Stays at perfect capacity
```

### 2.2 Architecture Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   API Gateway   │    │  Rate Limiter   │
│                 │────│                 │────│   Service       │
│   (HAProxy/     │    │  (Kong/Zuul)    │    │                 │
│    Nginx)       │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────┐             │
                       │   Redis Cluster │◄────────────┘
                       │   (Distributed  │
                       │    State)       │
                       └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   PostgreSQL    │
                       │   (Rules &      │
                       │   Analytics)    │
                       └─────────────────┘
```

#### Component Roles Explained

**Load Balancer (Traffic Director)**
```
Role: Distributes incoming requests across multiple rate limiter instances
Analogy: Traffic cop directing cars to different lanes
Why needed: Single rate limiter can't handle millions of requests
```

**Rate Limiter Service (The Bouncer)**
```
Role: Decides whether to allow or block each request
Analogy: Nightclub bouncer checking IDs and counting people
Decision process:
1. Who is this? (IP, User, API Key)
2. How many requests have they made?
3. Are they within their limit?
4. Allow or block?
```

**Redis Cluster (The Memory)**
```
Role: Stores request counters and rate limit state
Analogy: Bouncer's notebook tracking who entered when
Why Redis: 
- Super fast (< 1ms response time)
- Shared across all rate limiter instances
- Handles millions of operations per second
```

**PostgreSQL (The Record Keeper)**
```
Role: Stores rate limit rules and analytics data
Analogy: Club's permanent records and policies
Stores:
- Rate limit rules ("VIPs get 1000 entries/hour")
- Historical data ("User X was blocked 50 times last week")
```

### 2.3 Rate Limiting Flow (Step by Step)

```
📱 Client Request → 🛡️ Interceptor → 📋 Rule Matcher → ⚙️ Algorithm → 🔍 Redis Check → ✅/❌ Decision
    │                                                                                      │
    └─────────────────────────── 📊 Analytics Logger ◄──────────────────────────────────┘
```

#### Detailed Flow Explanation

**Step 1: Request Arrives**
```
Client sends: GET /api/weather
Headers: X-User-ID: john123, X-API-Key: abc456
IP: 192.168.1.100
```

**Step 2: Interceptor Catches Request**
```
Interceptor thinks: "Wait! Before processing this request, let me check rate limits"
Extracts: User ID, API Key, IP address, endpoint
```

**Step 3: Rule Matcher Finds Applicable Rules**
```
Checks rules in priority order:
1. /api/weather endpoint: 100 requests/hour per user ✓ (matches!)
2. /api/* wildcard: 1000 requests/hour per IP
3. Global rule: 1M requests/hour total

Selected rule: 100 requests/hour per user for /api/weather
```

**Step 4: Algorithm Processes Request**
```
Sliding Window Algorithm:
1. Look at last 60 minutes of requests for user john123
2. Count: 45 requests found
3. Limit: 100 requests allowed
4. Decision: 45 < 100, so ALLOW
```

**Step 5: Redis Check/Update**
```
Redis operations:
1. GET rate_limit:user:john123:/api/weather → "45"
2. INCR rate_limit:user:john123:/api/weather → "46"
3. EXPIRE rate_limit:user:john123:/api/weather 3600
```

**Step 6: Decision Made**
```
If ALLOWED:
- Add headers: X-RateLimit-Remaining: 54
- Continue to actual API
- Return response to client

If BLOCKED:
- Return HTTP 429 Too Many Requests
- Add headers: Retry-After: 3600
- Log the violation
```

**Step 7: Analytics Logging**
```
Log to database:
- User: john123
- Endpoint: /api/weather  
- Timestamp: 2024-01-15 14:30:00
- Decision: ALLOWED
- Remaining: 54
```

## 3. Understanding Rate Limiting Algorithms

### 3.1 Why Different Algorithms?

Imagine different ways to control entry to a popular restaurant:

```
🏪 Restaurant Capacity: 100 people

Fixed Window: "We allow 100 customers per hour. At 2:00 PM, counter resets."
Sliding Window: "We allow 100 customers in any 60-minute period."
Token Bucket: "You get 100 tokens. Use them anytime. We refill 1 token/minute."
Leaky Bucket: "Customers enter fast, but we seat them at steady 1 person/minute."
```

### 3.2 Algorithm Comparison

| Algorithm | Memory Usage | Accuracy | Burst Handling | Complexity |
|-----------|--------------|----------|----------------|-----------|
| Fixed Window | Low | Poor | Poor | Simple |
| Sliding Window | High | Excellent | Good | Medium |
| Token Bucket | Medium | Good | Excellent | Medium |
| Leaky Bucket | Medium | Good | Poor | Complex |

### 3.3 When to Use Each Algorithm

```
Fixed Window:
✅ Use when: Memory is limited, simple rate limiting needed
❌ Avoid when: Accuracy is critical, traffic is bursty

Sliding Window:
✅ Use when: Accuracy is important, fair rate limiting needed
❌ Avoid when: Memory usage must be minimal

Token Bucket:
✅ Use when: Traffic is bursty, need to allow temporary spikes
❌ Avoid when: Strict constant rate limiting required

Leaky Bucket:
✅ Use when: Need to smooth output, protect downstream services
❌ Avoid when: Users expect immediate response to bursts
```

## 4. Low-Level Design

### 4.1 Annotation-Based Implementation

#### Core Annotation Usage
```java
@RestController
public class ApiController {
    
    @GetMapping("/api/data")
    @RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.USER)
    public ResponseEntity<String> getData() {
        return ResponseEntity.ok("Data retrieved");
    }
    
    @PostMapping("/api/upload/{userId}")
    @RateLimit(
        requests = 10,
        window = 300,
        scope = RateLimit.Scope.CUSTOM,
        key = "#userId + ':upload'",
        algorithm = RateLimit.Algorithm.TOKEN_BUCKET
    )
    public ResponseEntity<String> upload(@PathVariable String userId) {
        return ResponseEntity.ok("Upload successful");
    }
}
```

#### Multi-Layer Protection
```java
@GetMapping("/api/premium")
@RateLimits({
    @RateLimit(requests = 10000, window = 3600, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)
})
public String getPremiumData() {
    return "premium data";
}
```

### 4.2 Core Components

#### Rate Limit Algorithms

**1. Sliding Window (Redis-based)**
- Uses Redis Sorted Sets with timestamps
- Removes expired entries automatically
- Provides accurate rate limiting

**2. Token Bucket (Redis-based)**
- Stores bucket state in Redis
- Supports burst capacity and refill rate
- Handles traffic spikes gracefully

### 3.2 Multi-Layered Protection

#### Layer 1: Global Rate Limiting
- Protects against DDoS attacks
- Applied to all requests regardless of user

#### Layer 2: IP-based Rate Limiting
- Prevents abuse from specific IP addresses
- Configurable per API endpoint

#### Layer 3: User-based Rate Limiting
- Per-user quotas and limits
- Different tiers (free, premium, enterprise)

#### Layer 4: API Key Rate Limiting
- Per-application rate limits
- Supports different subscription plans

### 3.3 Rule Matching Engine

```java
public RateLimitRule findApplicableRule(String apiEndpoint) {
    // 1. Get all enabled rules ordered by priority
    // 2. Find first matching rule using regex
    // 3. Cache result for performance
    // 4. Return rule or null for no limit
}
```

### 3.4 Distributed State Management

#### Redis Data Structures

**Sliding Window:**
```
Key: rate_limit:sliding:{clientKey}
Type: Sorted Set
Value: timestamp -> request_id
TTL: window_size_seconds
```

**Token Bucket:**
```
Key: rate_limit:token:{clientKey}
Type: String
Value: "tokens:last_refill_timestamp"
TTL: window_size_seconds
```

## 4. Scalability Design

### 4.1 Horizontal Scaling
- Stateless rate limiter instances
- Redis cluster for distributed state
- Load balancer with consistent hashing

### 4.2 Performance Optimizations
- Rule caching with Redis
- Batch processing for analytics
- Async logging to prevent blocking
- Connection pooling for Redis

### 4.3 High Availability
- Redis Sentinel for automatic failover
- Multiple rate limiter instances
- Circuit breaker for Redis failures
- Graceful degradation (allow requests if Redis down)

## 5. Security Features

### 5.1 DDoS Protection
- Global rate limits with aggressive thresholds
- IP-based blocking for repeated violations
- Exponential backoff for retry attempts

### 5.2 Abuse Prevention
- Pattern detection for suspicious behavior
- Automatic rule creation for detected attacks
- Integration with security monitoring systems

### 5.3 Data Protection
- Encrypted Redis connections
- Audit logging for all rate limit decisions
- PII anonymization in logs

## 6. Configuration Examples

### 6.1 API-Specific Rules

```json
{
  "ruleKey": "/api/upload.*",
  "algorithm": "TOKEN_BUCKET",
  "requestsPerWindow": 10,
  "windowSizeSeconds": 60,
  "burstCapacity": 15,
  "refillRate": 0.167,
  "scope": "USER",
  "priority": 100
}
```

### 6.2 Tiered Rate Limiting

```json
[
  {
    "ruleKey": "/api/premium/.*",
    "scope": "USER",
    "requestsPerWindow": 1000,
    "windowSizeSeconds": 3600,
    "priority": 90
  },
  {
    "ruleKey": "/api/.*",
    "scope": "USER", 
    "requestsPerWindow": 100,
    "windowSizeSeconds": 3600,
    "priority": 10
  }
]
```

## 7. Monitoring and Analytics

### 7.1 Key Metrics
- Requests per second by endpoint
- Rate limit violations by client
- Algorithm performance metrics
- Redis latency and availability

### 7.2 Alerting
- High violation rates
- Redis cluster health
- Unusual traffic patterns
- Performance degradation

## 8. API Documentation

### 8.1 Rate Limit Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
X-RateLimit-Algorithm: SLIDING_WINDOW
```

### 8.2 Error Response (429)
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 60,
  "ruleKey": "/api/upload.*"
}
```

## 9. Deployment Architecture

### 9.1 Production Setup
- 3+ Rate Limiter instances behind load balancer
- Redis Cluster (3 masters, 3 replicas)
- PostgreSQL with read replicas
- Monitoring with Prometheus/Grafana

### 9.2 Disaster Recovery
- Multi-region Redis replication
- Database backups and point-in-time recovery
- Automated failover procedures
- Circuit breaker fallback to local limits

## 10. Performance Characteristics

### 10.1 Latency
- P50: < 1ms (Redis hit)
- P95: < 5ms (Redis + DB)
- P99: < 10ms (worst case)

### 10.2 Throughput
- 100K+ requests/second per instance
- Linear scaling with additional instances
- Redis cluster supports millions of operations/second

### 10.3 Storage
- ~100 bytes per active rate limit key
- Automatic cleanup of expired data
- Configurable retention policies