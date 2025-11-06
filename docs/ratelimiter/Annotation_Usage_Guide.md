# Rate Limiter Annotation Usage Guide

## Overview

The annotation-based rate limiter provides a declarative way to apply rate limiting to methods and classes using simple annotations. This approach eliminates the need for manual interceptor configuration and provides a clean, maintainable solution.

### What Are Annotations?

Annotations in Java are like **labels** or **tags** that you attach to your code to give it special behavior. Think of them like sticky notes with instructions:

```java
// Without annotation: Just a regular method
public String getData() {
    return "data";
}

// With annotation: Now this method has rate limiting!
@RateLimit(requests = 100, window = 3600)
public String getData() {
    return "data";
}
```

### Why Use Annotations for Rate Limiting?

#### ✅ **Advantages**
```java
// Old way: Manual configuration
if (rateLimiter.checkLimit(request)) {
    return getData();
} else {
    throw new RateLimitException();
}

// New way: Just add annotation
@RateLimit(requests = 100, window = 3600)
public String getData() {
    return "data"; // Rate limiting happens automatically!
}
```

#### 📊 **Comparison**
```
Manual Approach:
❌ Repetitive code in every method
❌ Easy to forget rate limiting
❌ Hard to maintain and update
❌ Scattered rate limit logic

Annotation Approach:
✅ One line per method
✅ Impossible to forget (compiler enforced)
✅ Easy to change limits
✅ Centralized rate limit logic
```

## Understanding the Basics

### How Annotations Work Behind the Scenes

```java
@RateLimit(requests = 5, window = 60)
public String myMethod() {
    return "Hello World";
}
```

**What happens when this method is called:**

1. **Interception**: Spring AOP intercepts the method call
2. **Check**: Rate limiter checks if client has exceeded 5 requests in 60 seconds
3. **Decision**: 
   - If under limit → Method executes normally
   - If over limit → Throws RateLimitExceededException (HTTP 429)
4. **Update**: Counter is updated in Redis
5. **Response**: Client gets result or error

```
Client Request → AOP Interceptor → Rate Limit Check → Method Execution → Response
                      ↓
                 Redis Counter
```

## Basic Usage

### Method-Level Rate Limiting

#### Simple Example
```java
@RestController
public class ApiController {
    
    @GetMapping("/api/data")
    @RateLimit(requests = 100, window = 3600) // 100 requests per hour
    public ResponseEntity<String> getData() {
        return ResponseEntity.ok("Data retrieved");
    }
}
```

#### What This Means
```
Annotation Breakdown:
@RateLimit(
    requests = 100,    // Allow 100 requests
    window = 3600      // In 3600 seconds (1 hour)
)

Behavior:
- Request 1-100: ✅ Allowed
- Request 101+:  ❌ Blocked (HTTP 429)
- After 1 hour:  🔄 Counter resets
```

#### Real-World Scenario
```
Time: 2:00 PM - User makes 50 requests → ✅ All allowed
Time: 2:30 PM - User makes 50 requests → ✅ All allowed (total: 100)
Time: 2:45 PM - User makes 1 request  → ❌ Blocked!
Time: 3:00 PM - Counter resets        → 🔄 Fresh start
```

### Class-Level Rate Limiting

#### Example
```java
@RestController
@RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.IP)
public class ApiController {
    
    // All methods inherit the class-level rate limit
    @GetMapping("/method1")
    public ResponseEntity<String> method1() {
        return ResponseEntity.ok("Method 1");
    }
    
    // This method overrides the class-level rate limit
    @GetMapping("/method2")
    @RateLimit(requests = 10, window = 60)
    public ResponseEntity<String> method2() {
        return ResponseEntity.ok("Method 2 with custom limit");
    }
}
```

#### How Inheritance Works
```
Class Level: @RateLimit(requests = 1000, window = 3600)
│
├── method1() → Uses class limit (1000/hour)
├── method2() → Has own @RateLimit (10/minute) - OVERRIDES class
└── method3() → Uses class limit (1000/hour)
```

#### Practical Example
```
Scenario: API with general limits but strict upload limits

Class: 1000 requests/hour for all endpoints
Upload method: 5 uploads/hour (overrides class limit)

Result:
- /api/search: 1000 requests/hour
- /api/profile: 1000 requests/hour  
- /api/upload: 5 requests/hour (special limit)
```

## Understanding Rate Limiting Algorithms

### Why Different Algorithms?

Imagine you're managing entry to a popular restaurant:

```
Fixed Window: "We allow 100 customers per hour, counter resets at :00"
Sliding Window: "We allow 100 customers in any 60-minute period"
Token Bucket: "You get 100 tokens, use them anytime, we refill slowly"
Leaky Bucket: "Customers enter fast, but we serve them at steady pace"
```

### Visual Comparison
```
Traffic Pattern: ████████░░░░████████░░░░████████

Fixed Window:    [████████]   [████████]   [████████]
                 Allow all    Allow all    Allow all
                 (can spike)  (can spike)  (can spike)

Sliding Window:  ████████░░░░████████░░░░████████
                 Smooth rate limiting, no spikes

Token Bucket:    ████████░░░░████████░░░░████████
                 Allows bursts when tokens available

Leaky Bucket:    ████████████████████████████████
                 Smooths output, constant rate
```

## Rate Limiting Algorithms

### 1. Sliding Window (Default)

#### When to Use
```
✅ Best for: Most general use cases
✅ Good when: You want accurate rate limiting
❌ Avoid when: Memory is very limited
```

#### How It Works
```
Imagine a 60-second sliding window:

Time:    0s   10s   20s   30s   40s   50s   60s   70s
Requests: 5    3     7     2     8     4     1     6

At 60s: Count last 60s = 5+3+7+2+8+4 = 29 requests
At 70s: Count last 60s = 3+7+2+8+4+1 = 25 requests (5 dropped)
```

#### Code Example
```java
@RateLimit(
    requests = 100,     // Allow 100 requests
    window = 3600,      // In any 1-hour period
    algorithm = RateLimit.Algorithm.SLIDING_WINDOW
)
public String getData() {
    return "data";
}
```

#### Real-World Example
```
API Usage Scenario:
- User makes 50 requests at 2:00 PM
- User makes 50 requests at 2:30 PM  
- User tries 1 more request at 2:45 PM → BLOCKED
- At 3:00 PM, the 2:00 PM requests "slide out" → 50 requests available
```

### 2. Token Bucket

#### When to Use
```
✅ Best for: APIs that need to handle traffic bursts
✅ Good when: Users have irregular usage patterns
❌ Avoid when: You need strict, constant rate limiting
```

#### How It Works (Simple Analogy)
```
Think of a bucket with tokens (coins):

1. 🪙 Bucket starts with 100 tokens
2. 💰 Each API request costs 1 token
3. ⏰ Every second, 1 new token is added (refill rate)
4. 🚫 If no tokens left, request is rejected
5. 📦 Bucket can hold max 100 tokens (burst capacity)
```

#### Visual Example
```
Time:     0s    1s    2s    3s    4s    5s
Tokens:   100   90    80    81    82    83
Action:   -10   -10   +1-10  +1    +1
Status:   ✅    ✅    ✅     ✅     ✅

Burst scenario (user inactive for 60s, then sends 50 requests):
Tokens: 100 (full bucket)
Requests: 50 at once
Result: All 50 pass immediately! (burst allowed)
Remaining: 50 tokens
```

#### Code Example
```java
@RateLimit(
    requests = 50,           // Base rate: 50 requests
    window = 60,             // Per minute
    algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
    burstCapacity = 75,      // Can burst up to 75
    refillRate = 0.833       // ~50 tokens per minute
)
public String uploadFile() {
    return "upload successful";
}
```

#### Real-World Example
```
File Upload API:
- Normal usage: 10 uploads/hour
- Burst capacity: 25 uploads (for batch operations)
- Refill rate: 1 token every 6 minutes

Scenario:
- User uploads 25 files at once → ✅ All allowed (burst)
- User tries to upload 1 more → ❌ Blocked (no tokens)
- After 6 minutes → 1 token refilled, 1 upload allowed
```

### 3. Fixed Window

#### When to Use
```
✅ Best for: Simple rate limiting with predictable resets
✅ Good when: Memory usage must be minimal
❌ Avoid when: You can't tolerate traffic spikes
```

#### How It Works
```
Imagine hourly buckets that reset at exact times:

Hour 1 (12:00-1:00): [Request 1][Request 2]...[Request 1000] ✅
Hour 2 (1:00-2:00):  [Empty bucket - starts fresh]           🔄
Hour 3 (2:00-3:00):  [Request 1][Request 2]...[Request 1000] ✅
```

#### The "Boundary Problem"
```
Problem: Users can game the system

12:59:30 - User sends 1000 requests → ✅ All allowed
1:00:00  - Counter resets              → 🔄 Fresh start  
1:00:30  - User sends 1000 requests → ✅ All allowed

Result: 2000 requests in 1 minute! (Double the intended limit)
```

#### Code Example
```java
@RateLimit(
    requests = 1000,        // 1000 requests
    window = 3600,          // Per hour (resets at :00)
    algorithm = RateLimit.Algorithm.FIXED_WINDOW
)
public String getAnalytics() {
    return "analytics data";
}
```

#### Real-World Example
```
Newsletter API (sends emails):
- Limit: 10,000 emails per hour
- Resets: Every hour at :00 minutes
- Use case: Batch email sending

Behavior:
- 2:00 PM: Send 10,000 emails → ✅ Allowed
- 2:30 PM: Try to send 1 email → ❌ Blocked
- 3:00 PM: Counter resets → Can send 10,000 again
```

### 4. Leaky Bucket

#### When to Use
```
✅ Best for: Protecting downstream services
✅ Good when: You need smooth, constant output
❌ Avoid when: Users expect immediate responses to bursts
```

#### How It Works (Water Analogy)
```
Imagine a bucket with a small hole at the bottom:

1. 💧 Requests pour in from the top (any rate)
2. 🕳️ Requests "leak out" at constant rate (e.g., 1/second)
3. 📦 If bucket overflows, new requests are dropped
4. 📊 Output is always smooth and predictable
```

#### Visual Example
```
Input Traffic:  ████████░░░░████████ (bursty)
Bucket:         [████████████████████] (queued)
Output Traffic: ████████████████████ (smooth)
```

#### Code Example
```java
@RateLimit(
    requests = 30,              // Bucket capacity
    window = 60,                // Per minute
    algorithm = RateLimit.Algorithm.LEAKY_BUCKET,
    refillRate = 0.5           // 30 requests per minute = 0.5/second
)
public String processPayment() {
    return "payment processed";
}
```

#### Real-World Example
```
Payment Processing API:
- Downstream service can only handle 1 payment/second
- Users might send bursts of payments
- Leaky bucket smooths the traffic

Scenario:
- User sends 10 payments at once
- Bucket queues them
- Processes 1 payment every second
- Protects payment processor from overload
```

## Understanding Rate Limiting Scopes

### What is a "Scope"?

Scope determines **WHO** the rate limit applies to. Think of it as different ways to group users:

```
Restaurant Analogy:
- Per Person: Each customer gets 1 meal/hour
- Per Table: Each table gets 4 meals/hour  
- Per IP: Each address gets 10 meals/hour
- Global: Restaurant serves max 100 meals/hour total
```

### Scope Comparison
```
Scenario: 1000 users behind same office IP

IP Scope (100 requests/hour):
- All 1000 users share 100 requests
- Each user gets ~0.1 requests/hour ☹️

USER Scope (100 requests/hour):
- Each user gets their own 100 requests
- Total: 100,000 requests/hour from this IP 😱

Best: Combine both with multi-layer limiting!
```

## Rate Limiting Scopes

### 1. IP-based (Default)

#### What It Means
```
Rate limiting based on client's IP address
Same IP = Same rate limit bucket
```

#### When to Use
```
✅ Good for:
- Public APIs (no login required)
- Preventing DDoS attacks
- Basic abuse protection
- Simple implementation

❌ Problems with:
- Office/School WiFi (many users, same IP)
- Mobile users (IP changes frequently)
- Users behind NAT/Proxy
```

#### Real Example
```
Public Weather API:

IP 192.168.1.1 (John's home):     100 requests/hour ✅
IP 192.168.1.1 (Same IP):         Shares the 100 requests
IP 10.0.0.5 (Mary's office):      100 requests/hour ✅
IP 203.45.67.89 (Bob's mobile):   100 requests/hour ✅
```

#### Code Example
```java
@GetMapping("/weather")
@RateLimit(
    requests = 100, 
    window = 3600, 
    scope = RateLimit.Scope.IP  // Default scope
)
public String getWeather() {
    return "Sunny, 25°C";
}
```

### 2. User-based

#### What It Means
```
Rate limiting per authenticated user
Same user = Same rate limit bucket (regardless of IP)
```

#### When to Use
```
✅ Good for:
- Authenticated APIs
- Fair usage per user
- Subscription-based limits
- Mobile apps (changing IPs)

❌ Requires:
- User authentication
- X-User-ID header in requests
```

#### Real Example
```
Social Media API:

User: john@example.com
- From home IP: 50 requests
- From office IP: 30 requests  
- From mobile: 20 requests
- Total used: 100/1000 (same user, different IPs)

User: mary@example.com
- Gets her own 1000 requests/hour
```

#### Code Example
```java
@GetMapping("/profile")
@RateLimit(
    requests = 1000, 
    window = 3600, 
    scope = RateLimit.Scope.USER
)
public String getUserProfile() {
    return "user profile data";
}

// Client must send: X-User-ID: john@example.com
```

#### How User ID is Extracted
```java
// From HTTP header
X-User-ID: john@example.com

// Or from JWT token (if configured)
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### 3. API Key-based

#### What It Means
```
Rate limiting per API key/application
Same API key = Same rate limit bucket
```

#### When to Use
```
✅ Good for:
- Partner integrations
- Third-party developers
- Different service tiers
- B2B APIs

✅ Benefits:
- Easy usage tracking
- Simple billing integration
- Clear accountability
```

#### Real Example
```
Payment Processing API:

API Key: stripe_partner_123
- Tier: Partner (5000 requests/hour)
- Usage: 2500/5000

API Key: dev_sandbox_456  
- Tier: Developer (1000 requests/hour)
- Usage: 950/1000

API Key: enterprise_789
- Tier: Enterprise (50000 requests/hour)
- Usage: 15000/50000
```

#### Code Example
```java
@PostMapping("/charge")
@RateLimit(
    requests = 5000, 
    window = 3600, 
    scope = RateLimit.Scope.API_KEY
)
public String processPayment() {
    return "payment processed";
}

// Client must send: X-API-Key: stripe_partner_123
```

#### Tiered Limits Example
```java
// Different endpoints, different limits per tier
@GetMapping("/basic-data")
@RateLimit(requests = 1000, scope = RateLimit.Scope.API_KEY)
public String getBasicData() { return "basic"; }

@GetMapping("/premium-data")
@RateLimit(requests = 100, scope = RateLimit.Scope.API_KEY)  // Stricter
public String getPremiumData() { return "premium"; }
```

### 4. Tenant-based

#### What It Means
```
Rate limiting per tenant/organization
Same tenant = Shared rate limit bucket for all users in that tenant
```

#### When to Use
```
✅ Good for:
- Multi-tenant SaaS applications
- Organization-level limits
- Enterprise customers
- Resource allocation per company
```

#### Real Example
```
CRM SaaS Application:

Tenant: acme-corp
- Plan: Enterprise (10,000 requests/hour)
- Users: 100 employees
- Shared pool: All employees share 10,000 requests

Tenant: startup-inc
- Plan: Basic (1,000 requests/hour)  
- Users: 5 employees
- Shared pool: All employees share 1,000 requests
```

#### Code Example
```java
@GetMapping("/crm/contacts")
@RateLimit(
    requests = 10000, 
    window = 3600, 
    scope = RateLimit.Scope.TENANT
)
public String getContacts() {
    return "contact list";
}

// Client must send: X-Tenant-ID: acme-corp
```

#### Multi-User Scenario
```
Tenant: tech-company (1000 requests/hour limit)

User john@tech-company.com makes 300 requests
User mary@tech-company.com makes 400 requests  
User bob@tech-company.com makes 300 requests

Total: 1000/1000 used
Next request from anyone at tech-company → BLOCKED
```

### 5. Global

#### What It Means
```
Rate limiting across ALL clients combined
Everyone shares the same rate limit bucket
```

#### When to Use
```
✅ Good for:
- System capacity protection
- Database connection limits
- Third-party service quotas
- Emergency throttling

⚠️ Use carefully:
- Can block all users if limit reached
- Usually combined with other scopes
```

#### Real Example
```
Email Sending API (using external service with 100K emails/hour limit):

Global limit: 100,000 emails/hour

All customers combined:
- Customer A: 30,000 emails
- Customer B: 40,000 emails
- Customer C: 30,000 emails
- Total: 100,000/100,000 (limit reached)

Next email from ANY customer → BLOCKED until next hour
```

#### Code Example
```java
@PostMapping("/send-email")
@RateLimit(
    requests = 100000,     // 100K emails total
    window = 3600,         // Per hour
    scope = RateLimit.Scope.GLOBAL
)
public String sendEmail() {
    return "email sent";
}
```

#### Emergency Use Case
```java
// During system issues, globally throttle all traffic
@GetMapping("/api/data")
@RateLimit(
    requests = 1000,       // Reduced from normal 100K
    window = 3600,
    scope = RateLimit.Scope.GLOBAL,
    message = "System under maintenance - reduced capacity"
)
public String getData() {
    return "data";
}
```

### 6. Custom Key with SpEL

#### What It Means
```
Create your own rate limiting key using Spring Expression Language (SpEL)
Custom logic = Custom rate limit buckets
```

#### When to Use
```
✅ Good for:
- Complex business logic
- Combining multiple factors
- Resource-specific limits
- Advanced use cases
```

#### SpEL Variables Available
```java
#userId    // From X-User-ID header
#apiKey    // From X-API-Key header  
#ip        // Client IP address
#method    // Method name
#args      // Method arguments array
#request   // Full HTTP request object
```

#### Simple Examples
```java
// Per user + operation type
@PostMapping("/upload/{userId}")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':upload'"    // Result: "john123:upload"
)

// Per API key + endpoint
@GetMapping("/premium/data")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#apiKey + ':premium'"   // Result: "key123:premium"
)

// Per IP + user combination
@PostMapping("/login")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#ip + ':' + #userId"    // Result: "192.168.1.1:john"
)
```

#### Advanced Examples
```java
// Conditional logic
@GetMapping("/data")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId != null ? #userId + ':premium' : #ip + ':basic'"
    // If logged in: "john:premium", if not: "192.168.1.1:basic"
)

// Using method arguments
@PostMapping("/process/{type}/{priority}")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#args[0] + ':' + #args[1]"  // "video:high" or "image:low"
)
public String process(@PathVariable String type, @PathVariable String priority) {
    return "processed";
}

// Complex business logic
@PostMapping("/transfer")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':transfer:' + (#args[0].amount > 1000 ? 'large' : 'small')"
    // "john:transfer:large" or "john:transfer:small"
)
public String transfer(@RequestBody TransferRequest request) {
    return "transfer completed";
}
```

#### Real-World Scenario
```java
// Video processing with different limits per quality
@PostMapping("/process-video/{quality}")
@RateLimit(
    requests = 10,
    window = 3600,
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':video:' + #args[0]"
)
public String processVideo(@PathVariable String quality) {
    return "video processed";
}

// Results in separate buckets:
// "john:video:4k" - 10 requests/hour
// "john:video:1080p" - 10 requests/hour  
// "john:video:720p" - 10 requests/hour
```

## Advanced Features Explained

### Why Use Advanced Features?

```
Basic: One size fits all
@RateLimit(requests = 100, window = 3600)

Advanced: Tailored protection
- VIP users get higher limits
- Different limits per operation type
- Multiple protection layers
- Custom error messages
```

## Advanced Features

### Multiple Rate Limits (Multi-Layer Protection)

#### Why Use Multiple Limits?
```
Single Layer Problem:
- Only IP limit (100/hour): Office users share limit unfairly
- Only User limit (1000/hour): One user can DDoS with multiple IPs
- Only Global limit (10K/hour): Few users can consume all capacity

Multi-Layer Solution:
- Layer 1 (Global): Protect system capacity
- Layer 2 (User): Fair usage per person  
- Layer 3 (IP): Prevent IP-based attacks
```

#### How It Works
```
Request Processing:
1. Check Global limit (10,000/hour) → ✅ Pass
2. Check User limit (1,000/hour)   → ✅ Pass  
3. Check IP limit (100/hour)       → ❌ FAIL → Block request

All layers must pass for request to succeed!
```

#### Code Example
```java
@GetMapping("/api/premium")
@RateLimits({
    @RateLimit(requests = 10000, window = 3600, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)
})
public ResponseEntity<String> premiumEndpoint() {
    return ResponseEntity.ok("Premium data");
}
```

#### Real-World Example
```
E-commerce API Protection:

Scenario: Black Friday sale, high traffic expected

Layers:
1. Global: 1M requests/hour (system capacity)
2. User: 1K requests/hour (fair usage)
3. IP: 100 requests/hour (prevent bots)

Attack Scenario:
- Attacker uses 1000 IPs
- Without multi-layer: 1000 × 1000 = 1M requests (system down!)
- With multi-layer: 1000 × 100 = 100K requests (system safe!)
```

### Custom Error Messages

#### Why Custom Messages?
```
Default Message:
"Rate limit exceeded"
❌ Not helpful for users
❌ Doesn't explain what to do
❌ Generic and confusing

Custom Message:
"Upload limit reached. You can upload 5 more files in 3 minutes."
✅ Clear explanation
✅ Tells user what to do
✅ Provides timeline
```

#### Examples by Use Case
```java
// File upload limit
@RateLimit(
    requests = 5,
    window = 300,
    message = "Upload limit reached. You can upload 5 more files in 5 minutes."
)

// API call limit
@RateLimit(
    requests = 1000,
    window = 3600,
    message = "Hourly API limit exceeded. Limit resets at the top of each hour."
)

// Payment processing
@RateLimit(
    requests = 10,
    window = 3600,
    message = "Payment processing limit reached for security. Please try again in 1 hour."
)

// Login attempts
@RateLimit(
    requests = 5,
    window = 900,
    message = "Too many login attempts. Please wait 15 minutes before trying again."
)
```

#### User-Friendly Messages
```java
// Bad: Technical jargon
message = "Rate limit exceeded: 429 error"

// Good: Plain English
message = "You're sending requests too quickly. Please slow down."

// Better: Specific and helpful
message = "You've reached your daily limit of 1000 API calls. Limit resets at midnight UTC."

// Best: Actionable with alternatives
message = "Free tier limit reached (100 calls/day). Upgrade to Pro for 10,000 calls/day."
```

### Priority-based Rules

#### What is Priority?
```
Priority determines the ORDER in which rate limits are checked
Higher number = Higher priority = Checked first
```

#### Why Does Order Matter?
```
Scenario: User has exceeded IP limit but not User limit

Order 1 (IP first, User second):
1. Check IP limit → ❌ FAIL → Block immediately
2. User limit never checked

Order 2 (User first, IP second):  
1. Check User limit → ✅ PASS
2. Check IP limit → ❌ FAIL → Block

Same result, but different error messages possible!
```

#### Code Example
```java
@RateLimits({
    @RateLimit(
        requests = 100, 
        window = 60, 
        scope = RateLimit.Scope.IP, 
        priority = 2,  // Checked FIRST (higher priority)
        message = "Too many requests from your IP. Please slow down."
    ),
    @RateLimit(
        requests = 1000, 
        window = 3600, 
        scope = RateLimit.Scope.USER, 
        priority = 1,  // Checked SECOND (lower priority)
        message = "You've exceeded your hourly limit."
    )
})
```

#### Strategic Priority Usage
```java
// Security-first approach: Check strictest limits first
@RateLimits({
    @RateLimit(requests = 5, window = 60, scope = IP, priority = 3),      // Anti-abuse
    @RateLimit(requests = 100, window = 3600, scope = USER, priority = 2), // Fair usage
    @RateLimit(requests = 10000, window = 3600, scope = GLOBAL, priority = 1) // Capacity
})

// Performance-first: Check cheapest operations first
@RateLimits({
    @RateLimit(requests = 10000, scope = GLOBAL, priority = 3),    // Fast check
    @RateLimit(requests = 1000, scope = USER, priority = 2),       // Medium check
    @RateLimit(requests = 100, scope = CUSTOM, priority = 1)       // Expensive check
})
```

### Conditional Rate Limiting

#### What is Conditional Rate Limiting?
```
Ability to turn rate limits on/off without code changes
Useful for:
- Emergency situations
- A/B testing
- Feature flags
- Maintenance mode
```

#### Basic On/Off Control
```java
@RateLimit(
    requests = 100,
    window = 3600,
    enabled = true  // Can be changed via configuration
)
```

#### Configuration-Driven Control
```yaml
# application.yml
rate-limiting:
  upload-endpoint:
    enabled: true
    requests: 10
    window: 300
```

```java
@RateLimit(
    requests = "${rate-limiting.upload-endpoint.requests:10}",
    window = "${rate-limiting.upload-endpoint.window:300}",
    enabled = "${rate-limiting.upload-endpoint.enabled:true}"
)
```

#### Emergency Scenarios
```java
// Normal operation
@RateLimit(requests = 1000, window = 3600, enabled = true)

// During system issues - tighten limits
@RateLimit(requests = 100, window = 3600, enabled = true)

// During maintenance - disable completely
@RateLimit(requests = 1000, window = 3600, enabled = false)
```

#### A/B Testing Example
```java
// Test different rate limits for different user groups
@GetMapping("/api/data")
@RateLimits({
    @RateLimit(
        requests = 1000, 
        window = 3600,
        enabled = "${feature.premium-limits:false}",
        scope = RateLimit.Scope.CUSTOM,
        key = "#userId + ':premium'"
    ),
    @RateLimit(
        requests = 100, 
        window = 3600,
        enabled = "${feature.standard-limits:true}",
        scope = RateLimit.Scope.USER
    )
})
```

## SpEL (Spring Expression Language) Deep Dive

### What is SpEL?
```
SpEL = Spring Expression Language
Think of it as "mini programming language" inside annotations
Allows dynamic, conditional logic in rate limit keys
```

### Why Use SpEL?
```
Static Key:
@RateLimit(scope = USER)  // Always uses user ID

Dynamic Key with SpEL:
@RateLimit(
    scope = CUSTOM,
    key = "#userId != null ? #userId : #ip"  // Use user ID if logged in, else IP
)
```

## SpEL Expression Examples

### Available Variables
- `#request` - HttpServletRequest object
- `#method` - Method name
- `#args` - Method arguments array
- `#userId` - Extracted from X-User-ID header
- `#apiKey` - Extracted from X-API-Key header
- `#ip` - Client IP address

### Common Patterns
```java
// User + endpoint combination
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':' + #method"
)

// API key + IP combination
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#apiKey + ':' + #ip"
)

// Method argument-based
@PostMapping("/process/{type}")
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#args[0] + ':process'" // Uses path variable
)
public ResponseEntity<String> process(@PathVariable String type) {
    return ResponseEntity.ok("Processed");
}

// Complex expression
@RateLimit(
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId != null ? #userId + ':premium' : #ip + ':basic'"
)
```

## Configuration Examples

### Tiered Rate Limiting
```java
@RestController
public class TieredApiController {
    
    @GetMapping("/api/free")
    @RateLimit(requests = 100, window = 3600, scope = RateLimit.Scope.USER)
    public ResponseEntity<String> freeEndpoint() {
        return ResponseEntity.ok("Free tier data");
    }
    
    @GetMapping("/api/premium")
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER)
    public ResponseEntity<String> premiumEndpoint() {
        return ResponseEntity.ok("Premium tier data");
    }
    
    @GetMapping("/api/enterprise")
    @RateLimit(requests = 10000, window = 3600, scope = RateLimit.Scope.USER)
    public ResponseEntity<String> enterpriseEndpoint() {
        return ResponseEntity.ok("Enterprise tier data");
    }
}
```

### Resource-Specific Limits
```java
@RestController
public class ResourceController {
    
    @PostMapping("/api/upload")
    @RateLimit(
        requests = 10,
        window = 300,
        algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
        burstCapacity = 15,
        message = "Upload rate limit exceeded"
    )
    public ResponseEntity<String> upload() {
        return ResponseEntity.ok("File uploaded");
    }
    
    @GetMapping("/api/search")
    @RateLimit(
        requests = 1000,
        window = 3600,
        algorithm = RateLimit.Algorithm.SLIDING_WINDOW
    )
    public ResponseEntity<String> search() {
        return ResponseEntity.ok("Search results");
    }
    
    @PostMapping("/api/analytics")
    @RateLimit(
        requests = 100,
        window = 60,
        algorithm = RateLimit.Algorithm.LEAKY_BUCKET,
        refillRate = 1.667 // 100 per minute
    )
    public ResponseEntity<String> analytics() {
        return ResponseEntity.ok("Analytics processed");
    }
}
```

## Error Handling

### Default Error Response (429 Too Many Requests)
```json
{
  "error": "Rate limit exceeded",
  "message": "Rate limit exceeded",
  "retryAfter": 60,
  "ruleKey": "method_signature",
  "timestamp": 1640995200000
}
```

### Custom Exception Handling
```java
@RestControllerAdvice
public class CustomRateLimitHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleCustomRateLimit(
            RateLimitExceededException ex) {
        
        Map<String, Object> response = Map.of(
            "status", "RATE_LIMITED",
            "message", ex.getMessage(),
            "retryAfter", ex.getRetryAfter(),
            "suggestion", "Please reduce request frequency"
        );
        
        return ResponseEntity.status(429).body(response);
    }
}
```

## Best Practices

### 1. Choose Appropriate Algorithms
- **Sliding Window**: Most accurate, good for APIs with consistent traffic
- **Token Bucket**: Good for bursty traffic, allows temporary spikes
- **Fixed Window**: Simple, good for basic rate limiting
- **Leaky Bucket**: Smooth traffic shaping, good for downstream protection

### 2. Scope Selection
- **IP**: Good for public APIs, prevents abuse from specific IPs
- **USER**: Good for authenticated APIs, fair usage per user
- **API_KEY**: Good for partner APIs, different limits per client
- **GLOBAL**: Good for system protection, overall capacity limits

### 3. Rate Limit Values
```java
// Conservative for expensive operations
@RateLimit(requests = 10, window = 300) // 10 per 5 minutes

// Moderate for normal operations
@RateLimit(requests = 100, window = 3600) // 100 per hour

// Liberal for lightweight operations
@RateLimit(requests = 1000, window = 3600) // 1000 per hour
```

### 4. Multi-Layer Protection
```java
@RateLimits({
    @RateLimit(requests = 10000, window = 3600, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)
})
```

### 5. Monitoring and Alerting
- Monitor rate limit violations
- Set up alerts for unusual patterns
- Track algorithm performance
- Monitor Redis health and latency

## Testing

### Unit Testing
```java
@Test
public void testRateLimitAnnotation() {
    // Test rate limit behavior
    for (int i = 0; i < 10; i++) {
        ResponseEntity<String> response = controller.limitedEndpoint();
        assertEquals(200, response.getStatusCodeValue());
    }
    
    // 11th request should be rate limited
    assertThrows(RateLimitExceededException.class, () -> {
        controller.limitedEndpoint();
    });
}
```

### Integration Testing
```bash
# Test script example
for i in {1..10}; do
    curl -w "%{http_code}\n" http://localhost:8088/api/annotation-test/basic
done
```