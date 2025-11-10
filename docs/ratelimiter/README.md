# API Rate Limiter - Complete Documentation

## 📚 Documentation Overview

This comprehensive documentation covers everything you need to know about implementing and using the distributed API rate limiter, from basic concepts to advanced production deployment.

## 🎯 Start Here

### For Beginners
1. **[Beginner Tutorial](Beginner_Tutorial.md)** - Start here if you're new to rate limiting
2. **[Theory and Concepts](Theory_and_Concepts.md)** - Understand the fundamentals
3. **[Annotation Usage Guide](Annotation_Usage_Guide.md)** - Learn the annotation-based approach

### For Experienced Developers
1. **[System Design](System_Design.md)** - Complete architecture and design decisions
2. **[Flow Diagram](Flow_Diagram.md)** - Visual flow diagrams and system architecture
3. **[API Documentation](API_Documentation.md)** - Annotation examples and testing
4. **[Scale Calculations](Scale_Calculations.md)** - Performance analysis and cost planning

## 🚀 Quick Start

### 1. Add Rate Limiting to Any Method

```java
@RestController
public class ApiController {
    
    @GetMapping("/api/data")
    @RateLimit(requests = 100, window = 3600)  // 100 requests per hour
    public String getData() {
        return "your data";
    }
}
```

### 2. Multi-Layer Protection

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

### 3. Custom Business Logic

```java
@PostMapping("/upload/{userId}")
@RateLimit(
    requests = 10,
    window = 300,
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':upload'",
    message = "Upload limit exceeded for this user"
)
public String upload(@PathVariable String userId) {
    return "upload successful";
}
```

## 📖 Documentation Structure

### Core Concepts
- **[Theory and Concepts](Theory_and_Concepts.md)** - Fundamental concepts, algorithms, and strategies
- **[Beginner Tutorial](Beginner_Tutorial.md)** - Step-by-step learning path with examples

### Implementation Guides
- **[Annotation Usage Guide](Annotation_Usage_Guide.md)** - Complete guide to using annotations
- **[Flow Diagram](Flow_Diagram.md)** - Visual flow diagrams and system architecture
- **[API Documentation](API_Documentation.md)** - Annotation examples and testing

### Architecture & Operations
- **[System Design](System_Design.md)** - High-level and low-level design
- **[Scale Calculations](Scale_Calculations.md)** - Performance, capacity, and cost analysis

## 🎯 Choose Your Learning Path

### Path 1: I'm New to Rate Limiting
```
1. Beginner Tutorial (30 min)
   ↓
2. Theory and Concepts (45 min)
   ↓
3. Annotation Usage Guide (60 min)
   ↓
4. Start implementing!
```

### Path 2: I Know Rate Limiting, Show Me the Code
```
1. Annotation Usage Guide (30 min)
   ↓
2. Start implementing!
```

### Path 3: I Need to Design a System
```
1. System Design (45 min)
   ↓
2. Scale Calculations (30 min)
   ↓
3. Theory and Concepts (for algorithm details)
   ↓
4. Plan your architecture!
```

## 🔧 Implementation Approach

### Annotation-Based Rate Limiting
- **Pros**: Simple, declarative, type-safe, maintainable
- **Use when**: Building applications with rate limiting requirements
- **Example**: `@RateLimit(requests = 100, window = 3600)`
- **Features**: Multi-algorithm support, multi-scope protection, SpEL expressions

## 🏗️ Architecture Highlights

### Multi-Algorithm Support
```java
// Sliding Window - Most accurate
@RateLimit(algorithm = RateLimit.Algorithm.SLIDING_WINDOW)

// Token Bucket - Handles bursts
@RateLimit(algorithm = RateLimit.Algorithm.TOKEN_BUCKET, burstCapacity = 150)

// Fixed Window - Memory efficient
@RateLimit(algorithm = RateLimit.Algorithm.FIXED_WINDOW)

// Leaky Bucket - Smooth output
@RateLimit(algorithm = RateLimit.Algorithm.LEAKY_BUCKET)
```

### Multi-Scope Protection
```java
// Per user (fair usage)
@RateLimit(scope = RateLimit.Scope.USER)

// Per IP (abuse prevention)
@RateLimit(scope = RateLimit.Scope.IP)

// Per API key (subscription tiers)
@RateLimit(scope = RateLimit.Scope.API_KEY)

// Custom logic with SpEL
@RateLimit(scope = RateLimit.Scope.CUSTOM, key = "#userId + ':' + #operation")
```

### High Availability
- **Redis Clustering**: Distributed state management
- **Automatic Failover**: Graceful degradation when Redis is down
- **Horizontal Scaling**: Add more rate limiter instances as needed
- **Circuit Breaker**: Protect against cascading failures

## 📊 Performance Characteristics

| Metric | Value | Notes |
|--------|-------|-------|
| **Throughput** | 1M+ requests/second | Per rate limiter cluster |
| **Latency** | < 1ms P50, < 5ms P95 | Including Redis operations |
| **Availability** | 99.99% | With proper Redis clustering |
| **Memory** | ~100 bytes per active key | Varies by algorithm |
| **Scalability** | Linear | Add instances as needed |

## 🛡️ Security Features

### DDoS Protection
```java
@GetMapping("/api/protected")
@RateLimits({
    @RateLimit(requests = 1000000, scope = RateLimit.Scope.GLOBAL),
    @RateLimit(requests = 1000, scope = RateLimit.Scope.IP),
    @RateLimit(requests = 10000, scope = RateLimit.Scope.USER)
})
public String getProtectedData() {
    return "protected data";
}
```

### Abuse Prevention
- **IP-based blocking**: Prevent attacks from specific IPs
- **Pattern detection**: Identify suspicious behavior
- **Exponential backoff**: Increase penalties for repeated violations
- **Whitelist support**: Allow trusted clients to bypass limits

## 🔍 Monitoring & Analytics

### Key Metrics
- Request rate and rate limit violations
- Algorithm performance and Redis latency
- Top rate-limited clients and endpoints
- System capacity utilization

### Built-in Analytics
- Real-time violation tracking
- Historical usage patterns
- Client behavior analysis
- Performance monitoring

## 🚀 Getting Started

1. **Read the [Beginner Tutorial](Beginner_Tutorial.md)** to understand the basics
2. **Follow the [Annotation Usage Guide](Annotation_Usage_Guide.md)** to implement rate limiting
3. **Check the [API Documentation](API_Documentation.md)** for testing examples
4. **Review [Scale Calculations](Scale_Calculations.md)** for production planning

## 🤝 Contributing

Found an issue or want to improve the documentation?
1. Check existing issues
2. Create a detailed bug report or feature request
3. Submit a pull request with improvements

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

---

**Need Help?** Start with the [Beginner Tutorial](Beginner_Tutorial.md) or jump to the [Annotation Usage Guide](Annotation_Usage_Guide.md) for hands-on examples!