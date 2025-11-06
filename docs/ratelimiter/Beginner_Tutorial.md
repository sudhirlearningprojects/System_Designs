# Annotation-Based Rate Limiter - Complete Beginner Tutorial

## 🎯 What You'll Learn

By the end of this tutorial, you'll understand:
- What rate limiting is and why it's important
- How to add rate limiting to your APIs with simple annotations
- Different strategies for different use cases
- How to test and monitor rate limits

## 📚 Prerequisites

- Basic Java knowledge
- Understanding of REST APIs
- Spring Boot basics (helpful but not required)

## 🚀 Getting Started

### Step 1: Understanding the Problem

Imagine you built a popular API that provides weather data. Without rate limiting:

```
Normal Day:
- 1000 users make 10 requests each = 10,000 requests ✅ (manageable)

Black Friday:
- 100,000 users make 100 requests each = 10,000,000 requests 💥 (server crash!)

Malicious Attack:
- 1 user makes 1,000,000 requests = 1,000,000 requests 💥 (server crash!)
```

**Rate limiting solves this by saying: "Each user can only make X requests per Y time period"**

### Step 2: Your First Rate Limit

Let's start with the simplest possible example:

```java
@RestController
public class WeatherController {
    
    @GetMapping("/weather")
    @RateLimit(requests = 10, window = 60)  // 10 requests per 60 seconds
    public String getWeather() {
        return "Sunny, 25°C";
    }
}
```

**What this does:**
- Each user can call `/weather` 10 times per minute
- On the 11th request within a minute → HTTP 429 (Too Many Requests)
- After 60 seconds → Counter resets, user can make 10 more requests

### Step 3: Testing Your Rate Limit

Let's test this with a simple script:

```bash
# Make 12 requests quickly to your endpoint
for i in {1..12}; do
    echo "Request $i:"
    curl -w "Status: %{http_code}\n" http://localhost:8088/weather
    echo "---"
done
```

**Expected output:**
```
Request 1: Status: 200 ✅
Request 2: Status: 200 ✅
...
Request 10: Status: 200 ✅
Request 11: Status: 429 ❌ (Rate limited!)
Request 12: Status: 429 ❌ (Rate limited!)
```

## 🎛️ Different Rate Limiting Strategies

### Strategy 1: Protect Against Abuse (IP-based)

**Use case:** Public API that anyone can access

```java
@GetMapping("/public-data")
@RateLimit(
    requests = 100,                    // 100 requests
    window = 3600,                     // per hour
    scope = RateLimit.Scope.IP         // per IP address
)
public String getPublicData() {
    return "public data";
}
```

**What this means:**
- Each IP address gets 100 requests per hour
- Good for preventing DDoS attacks
- Problem: Office WiFi users share the same IP

### Strategy 2: Fair Usage (User-based)

**Use case:** Authenticated API with registered users

```java
@GetMapping("/user-data")
@RateLimit(
    requests = 1000,                   // 1000 requests  
    window = 3600,                     // per hour
    scope = RateLimit.Scope.USER       // per logged-in user
)
public String getUserData() {
    return "user-specific data";
}
```

**What this means:**
- Each logged-in user gets their own 1000 requests per hour
- Fair for all users regardless of IP
- Requires user authentication (X-User-ID header)

### Strategy 3: Subscription Tiers (API Key-based)

**Use case:** API with different pricing plans

```java
@GetMapping("/premium-data")
@RateLimit(
    requests = 10000,                  // 10,000 requests
    window = 3600,                     // per hour  
    scope = RateLimit.Scope.API_KEY    // per API key
)
public String getPremiumData() {
    return "premium data";
}
```

**What this means:**
- Each API key gets 10,000 requests per hour
- Different API keys can have different limits
- Good for B2B integrations

## 🔄 Different Algorithms Explained Simply

### Algorithm 1: Fixed Window (Simple)

```java
@RateLimit(
    requests = 100,
    window = 3600,
    algorithm = RateLimit.Algorithm.FIXED_WINDOW
)
```

**How it works:**
```
Hour 1 (12:00-1:00): [●●●●●●●●●●] 100 requests ✅
Hour 2 (1:00-2:00):  [          ] 0 requests (resets) ✅
Hour 3 (2:00-3:00):  [●●●●●●●●●●] 100 requests ✅
```

**Pros:** Simple, memory efficient
**Cons:** Can allow double traffic at hour boundaries

### Algorithm 2: Sliding Window (Accurate)

```java
@RateLimit(
    requests = 100,
    window = 3600,
    algorithm = RateLimit.Algorithm.SLIDING_WINDOW
)
```

**How it works:**
```
Current time: 12:30
Window: Last 60 minutes (11:30 - 12:30)
Counts all requests in this moving window
```

**Pros:** More accurate, smooth limiting
**Cons:** Uses more memory

### Algorithm 3: Token Bucket (Bursty Traffic)

```java
@RateLimit(
    requests = 100,
    window = 3600,
    algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
    burstCapacity = 150,
    refillRate = 0.028  // ~100 tokens per hour
)
```

**How it works:**
```
Think of a bucket with 150 tokens:
- Each request costs 1 token
- Tokens refill slowly (100 per hour)
- Can handle bursts when bucket is full
```

**Pros:** Handles traffic spikes well
**Cons:** More complex to configure

## 🛡️ Multi-Layer Protection

For production APIs, use multiple layers:

```java
@GetMapping("/important-api")
@RateLimits({
    @RateLimit(requests = 1000000, window = 3600, scope = RateLimit.Scope.GLOBAL),  // System protection
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),       // Fair usage
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)             // Abuse prevention
})
public String getImportantData() {
    return "important data";
}
```

**Why multiple layers?**
- **Global:** Protects your entire system from overload
- **User:** Ensures fair usage among users
- **IP:** Prevents abuse from specific IP addresses

## 🎯 Real-World Examples

### Example 1: File Upload API

```java
@PostMapping("/upload")
@RateLimit(
    requests = 5,                      // Only 5 uploads
    window = 300,                      // per 5 minutes
    algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
    burstCapacity = 10,                // Can burst to 10 if user was inactive
    message = "Upload limit reached. You can upload 5 more files in 5 minutes."
)
public String uploadFile(@RequestBody MultipartFile file) {
    // Upload logic here
    return "File uploaded successfully";
}
```

### Example 2: Login API (Security)

```java
@PostMapping("/login")
@RateLimit(
    requests = 5,                      // Only 5 login attempts
    window = 900,                      // per 15 minutes
    scope = RateLimit.Scope.IP,        // per IP (prevent brute force)
    message = "Too many login attempts. Please wait 15 minutes."
)
public String login(@RequestBody LoginRequest request) {
    // Login logic here
    return "Login successful";
}
```

### Example 3: Payment API (Critical)

```java
@PostMapping("/payment")
@RateLimits({
    @RateLimit(requests = 10, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.GLOBAL)
})
public String processPayment(@RequestBody PaymentRequest request) {
    // Payment processing logic
    return "Payment processed";
}
```

## 🧪 Testing Your Rate Limits

### Test Script 1: Basic Functionality

```bash
#!/bin/bash
echo "Testing basic rate limit (5 requests per minute)..."

for i in {1..7}; do
    echo "Request $i:"
    response=$(curl -s -w "%{http_code}" http://localhost:8088/api/your-endpoint)
    status_code="${response: -3}"
    
    if [ "$status_code" = "200" ]; then
        echo "✅ Success"
    elif [ "$status_code" = "429" ]; then
        echo "❌ Rate Limited (Expected after 5 requests)"
    else
        echo "❓ Unexpected status: $status_code"
    fi
    
    sleep 1
done
```

### Test Script 2: Different Users

```bash
#!/bin/bash
echo "Testing user-based rate limiting..."

# User 1
echo "User 1 requests:"
for i in {1..3}; do
    curl -H "X-User-ID: user1" -w "Status: %{http_code}\n" http://localhost:8088/api/your-endpoint
done

# User 2  
echo "User 2 requests:"
for i in {1..3}; do
    curl -H "X-User-ID: user2" -w "Status: %{http_code}\n" http://localhost:8088/api/your-endpoint
done
```

## 🚨 Common Mistakes and How to Avoid Them

### Mistake 1: Too Restrictive Limits

```java
// ❌ Bad: Too restrictive for normal usage
@RateLimit(requests = 1, window = 3600)  // Only 1 request per hour!

// ✅ Good: Reasonable for normal usage
@RateLimit(requests = 100, window = 3600)  // 100 requests per hour
```

### Mistake 2: No Error Messages

```java
// ❌ Bad: Generic error message
@RateLimit(requests = 10, window = 60)

// ✅ Good: Helpful error message
@RateLimit(
    requests = 10, 
    window = 60,
    message = "You can make 10 requests per minute. Please slow down."
)
```

### Mistake 3: Wrong Scope Choice

```java
// ❌ Bad: IP-based for mobile app (IPs change frequently)
@RateLimit(scope = RateLimit.Scope.IP)

// ✅ Good: User-based for mobile app
@RateLimit(scope = RateLimit.Scope.USER)
```

### Mistake 4: No Multi-Layer Protection

```java
// ❌ Bad: Only one layer of protection
@RateLimit(requests = 1000, scope = RateLimit.Scope.USER)

// ✅ Good: Multiple layers
@RateLimits({
    @RateLimit(requests = 10000, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, scope = RateLimit.Scope.IP)
})
```

## 📊 Monitoring Your Rate Limits

### Key Metrics to Track

1. **Rate Limit Hit Rate**
   ```
   Good: < 5% of requests are rate limited
   Warning: 5-10% of requests are rate limited  
   Bad: > 10% of requests are rate limited
   ```

2. **Top Rate Limited Users/IPs**
   ```
   Monitor who's hitting limits most often
   May indicate abuse or legitimate high usage
   ```

3. **Algorithm Performance**
   ```
   Track latency added by rate limiting
   Should be < 1ms for most algorithms
   ```

### Simple Monitoring Setup

```java
@Component
public class RateLimitMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleRateLimitEvent(RateLimitEvent event) {
        meterRegistry.counter("rate_limit.hits", 
            "scope", event.getScope(),
            "allowed", String.valueOf(event.isAllowed())
        ).increment();
    }
}
```

## 🎓 Next Steps

Now that you understand the basics:

1. **Start Simple:** Begin with basic IP-based rate limiting
2. **Add Layers:** Gradually add user-based and global limits
3. **Monitor:** Set up basic monitoring to see how limits are working
4. **Adjust:** Fine-tune limits based on real usage patterns
5. **Advanced:** Explore custom SpEL expressions for complex scenarios

## 🔗 Quick Reference

### Most Common Patterns

```java
// Public API protection
@RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.IP)

// User fair usage  
@RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER)

// File upload limits
@RateLimit(requests = 10, window = 300, algorithm = RateLimit.Algorithm.TOKEN_BUCKET)

// Login security
@RateLimit(requests = 5, window = 900, scope = RateLimit.Scope.IP)

// Multi-layer protection
@RateLimits({
    @RateLimit(requests = 10000, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, scope = RateLimit.Scope.IP)
})
```

Remember: Rate limiting is about finding the right balance between protecting your system and providing a good user experience. Start conservative and adjust based on real usage patterns!