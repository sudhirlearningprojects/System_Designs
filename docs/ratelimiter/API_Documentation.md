# Rate Limiter - Annotation-Based Implementation

## Base URL
```
http://localhost:8088/api
```

## Overview

This documentation covers the annotation-based rate limiting implementation. Rate limits are applied declaratively using `@RateLimit` annotations on methods and classes.

## Rate Limit Headers
All responses include rate limit information:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
X-RateLimit-Algorithm: SLIDING_WINDOW
```



## Annotation-Based Examples

### Basic Usage
```java
@GetMapping("/api/data")
@RateLimit(requests = 100, window = 3600)
public ResponseEntity<String> getData() {
    return ResponseEntity.ok("Data retrieved");
}
```

### Multiple Rate Limits
```java
@GetMapping("/api/premium")
@RateLimits({
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)
})
public ResponseEntity<String> premiumData() {
    return ResponseEntity.ok("Premium data");
}
```

### Custom Key with SpEL
```java
@PostMapping("/upload/{userId}")
@RateLimit(
    requests = 10,
    window = 300,
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':upload'"
)
public ResponseEntity<String> upload(@PathVariable String userId) {
    return ResponseEntity.ok("Upload successful");
}
```

## How to Use in Your Application

### Step 1: Add Annotations to Your Controllers

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER)
    public List<Product> getAllProducts() {
        return productService.findAll();
    }
    
    @PostMapping
    @RateLimit(requests = 10, window = 3600, scope = RateLimit.Scope.USER)
    public Product createProduct(@RequestBody Product product) {
        return productService.save(product);
    }
}
```

### Step 2: Test Your Rate Limits

```bash
# Test basic rate limiting
for i in {1..15}; do
    curl -w "Status: %{http_code}\n" http://localhost:8088/api/products
done

# Test user-scoped rate limiting
curl -H "X-User-ID: user123" http://localhost:8088/api/products
```

## Testing Different Scopes

### USER Scope Testing
Rate limiting based on user ID from `X-User-ID` header.
```bash
curl -H "X-User-ID: user123" http://localhost:8088/api/your-endpoint
```

### IP Scope Testing
Rate limiting based on client IP address.
```bash
curl http://localhost:8088/api/your-endpoint
```

### API_KEY Scope Testing
Rate limiting based on API key from `X-API-Key` header.
```bash
curl -H "X-API-Key: api_key_123" http://localhost:8088/api/your-endpoint
```

### Custom Scope Testing
Rate limiting with custom SpEL expressions.
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"data":"test"}' \
  http://localhost:8088/api/your-endpoint/user123
```

## Error Responses

### 429 Too Many Requests
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 60,
  "ruleKey": "/api/test/.*"
}
```

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "Missing required parameters"
}
```

### 404 Not Found
```json
{
  "error": "Rule not found",
  "message": "Rate limit rule with ID 999 not found"
}
```

## Rate Limit Algorithms

### SLIDING_WINDOW
```java
@RateLimit(
    requests = 100,
    window = 3600,
    algorithm = RateLimit.Algorithm.SLIDING_WINDOW
)
```

### TOKEN_BUCKET
```java
@RateLimit(
    requests = 100,
    window = 3600,
    algorithm = RateLimit.Algorithm.TOKEN_BUCKET,
    burstCapacity = 150,
    refillRate = 0.028
)
```

### FIXED_WINDOW
```java
@RateLimit(
    requests = 1000,
    window = 3600,
    algorithm = RateLimit.Algorithm.FIXED_WINDOW
)
```

### LEAKY_BUCKET
```java
@RateLimit(
    requests = 60,
    window = 60,
    algorithm = RateLimit.Algorithm.LEAKY_BUCKET,
    refillRate = 1.0
)
```

## Configuration Examples

### Basic Rate Limiting
```java
@GetMapping("/api/data")
@RateLimit(requests = 10, window = 60)
public String getData() {
    return "data";
}
```

### Multi-Layer Protection
```java
@GetMapping("/api/premium")
@RateLimits({
    @RateLimit(requests = 1000, window = 3600, scope = RateLimit.Scope.USER),
    @RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.IP)
})
public String getPremiumData() {
    return "premium data";
}
```

### Custom Key with SpEL
```java
@PostMapping("/upload/{userId}")
@RateLimit(
    requests = 5,
    window = 300,
    scope = RateLimit.Scope.CUSTOM,
    key = "#userId + ':upload'"
)
public String upload(@PathVariable String userId) {
    return "upload successful";
}
```