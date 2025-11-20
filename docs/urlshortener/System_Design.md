# TinyURL Clone - URL Shortener System Design

## System Overview

### What is a URL Shortener?
A URL shortener is a service that takes long URLs and converts them into shorter, more manageable links. When users click the short URL, they are redirected to the original long URL.

**Examples:**
- Long URL: `https://www.example.com/very/long/path/to/some/resource?param1=value1&param2=value2`
- Short URL: `https://tinyurl.com/abc123`

### Key Features
- **URL Shortening**: Convert long URLs to short ones
- **URL Redirection**: Redirect short URLs to original URLs
- **Custom Aliases**: Allow users to create custom short URLs
- **Analytics**: Track click counts, geographic data, referrers
- **Expiration**: Set expiration dates for URLs
- **Rate Limiting**: Prevent abuse and spam

### Core Challenges
1. **Unique Short URL Generation**: Ensure no collisions
2. **High Read/Write Ratio**: 100:1 read to write ratio
3. **Low Latency**: Sub-100ms redirect response
4. **High Availability**: 99.99% uptime
5. **Scalability**: Handle billions of URLs and redirects

## 1. Requirements

### Functional Requirements
- **Shorten URL**: Users can input a long URL and get a shortened version
- **Redirect**: When accessing short URL, redirect to original URL
- **Custom Aliases**: Users can specify custom short URLs (if available)
- **Expiration**: URLs can have expiration dates
- **Analytics**: Track clicks, geographic data, user agents
- **URL Management**: Users can view, edit, delete their URLs

### Non-Functional Requirements
- **Scale**: 100M URLs shortened per day, 10B redirects per day
- **Availability**: 99.99% uptime
- **Latency**: <100ms for redirects, <500ms for shortening
- **Durability**: URLs should not be lost
- **Read Heavy**: 100:1 read to write ratio

### Capacity Estimation
- **Write QPS**: 100M URLs/day = ~1,200 URLs/second
- **Read QPS**: 10B redirects/day = ~115,000 redirects/second
- **Storage**: 100M URLs/day × 365 days × 5 years × 500 bytes = ~91TB
- **Bandwidth**: 115K QPS × 500 bytes = ~58MB/s

## 2. High-Level Design (HLD)

### 2.1 System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │   Mobile App    │    │   API Client    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   CDN/Cache     │
                    │  (Static Assets)│
                    └─────────┬───────┘
                              │
                    ┌─────────────────┐
                    │  Load Balancer  │
                    │   (Rate Limit)  │
                    └─────────┬───────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────▼───────┐ ┌─────────▼───────┐ ┌─────────▼───────┐
│ URL Shortener   │ │ Redirect Service│ │Analytics Service│
│   Service       │ │                 │ │                 │
└─────────┬───────┘ └─────────┬───────┘ └─────────┬───────┘
          │                   │                   │
          │         ┌─────────▼───────┐           │
          │         │  Cache Layer    │           │
          │         │    (Redis)      │           │
          │         └─────────┬───────┘           │
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                    ┌─────────────────┐
                    │   Database      │
                    │  (PostgreSQL)   │
                    └─────────────────┘
```

### 2.2 Database Design

#### URL Table
```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_url VARCHAR(7) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    click_count BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_short_url ON urls(short_url);
CREATE INDEX idx_user_id ON urls(user_id);
CREATE INDEX idx_expires_at ON urls(expires_at);
```

#### Analytics Table
```sql
CREATE TABLE url_analytics (
    id BIGSERIAL PRIMARY KEY,
    short_url VARCHAR(7) NOT NULL,
    clicked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT,
    country VARCHAR(2),
    city VARCHAR(100),
    referrer TEXT
);

CREATE INDEX idx_short_url_analytics ON url_analytics(short_url);
CREATE INDEX idx_clicked_at ON url_analytics(clicked_at);
```

### 2.3 URL Encoding Algorithms

#### Base62 Encoding
```
Characters: [a-z, A-Z, 0-9] = 62 characters
Short URL length: 7 characters
Total combinations: 62^7 = ~3.5 trillion URLs
```

#### Counter-Based Approach
```java
public class Base62Encoder {
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    public static String encode(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int)(id % 62)));
            id /= 62;
        }
        return sb.reverse().toString();
    }
    
    public static long decode(String shortUrl) {
        long result = 0;
        for (char c : shortUrl.toCharArray()) {
            result = result * 62 + BASE62.indexOf(c);
        }
        return result;
    }
}
```

#### Hash-Based Approach
```java
public class HashBasedEncoder {
    public static String generateShortUrl(String longUrl) {
        // Use MD5 hash and take first 7 characters
        String hash = DigestUtils.md5Hex(longUrl + System.currentTimeMillis());
        return hash.substring(0, 7);
    }
}
```

### 2.4 Caching Strategy

#### Cache Hierarchy
```
L1: Application Cache (Caffeine) - 10K most popular URLs
L2: Redis Cluster - 1M URLs with TTL
L3: Database - All URLs
```

#### Cache Implementation
```java
@Service
public class CacheService {
    
    @Cacheable(value = "urls", key = "#shortUrl")
    public String getLongUrl(String shortUrl) {
        // L1: Check application cache (handled by @Cacheable)
        
        // L2: Check Redis
        String longUrl = redisTemplate.opsForValue().get("url:" + shortUrl);
        if (longUrl != null) {
            return longUrl;
        }
        
        // L3: Check database
        URL url = urlRepository.findByShortUrl(shortUrl);
        if (url != null && url.isActive() && !url.isExpired()) {
            // Cache in Redis for 1 hour
            redisTemplate.opsForValue().set("url:" + shortUrl, 
                                           url.getLongUrl(), 
                                           Duration.ofHours(1));
            return url.getLongUrl();
        }
        
        return null;
    }
}
```
## 3. Low-Level Design (LLD)

### 3.1 Core Components

#### URL Shortener Service
```java
@Service
@Transactional
public class UrlShortenerService {
    
    private final UrlRepository urlRepository;
    private final CacheService cacheService;
    private final CounterService counterService;
    
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        // Validate URL
        if (!isValidUrl(request.getLongUrl())) {
            throw new InvalidUrlException("Invalid URL format");
        }
        
        // Check if custom alias is provided
        String shortUrl;
        if (request.getCustomAlias() != null) {
            if (urlRepository.existsByShortUrl(request.getCustomAlias())) {
                throw new AliasAlreadyExistsException("Custom alias already exists");
            }
            shortUrl = request.getCustomAlias();
        } else {
            // Generate unique short URL
            shortUrl = generateUniqueShortUrl();
        }
        
        // Create URL entity
        URL url = URL.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .userId(request.getUserId())
                .expiresAt(request.getExpiresAt())
                .build();
        
        // Save to database
        urlRepository.save(url);
        
        // Cache the mapping
        cacheService.cacheUrl(shortUrl, request.getLongUrl());
        
        return ShortenUrlResponse.builder()
                .shortUrl(BASE_URL + shortUrl)
                .longUrl(request.getLongUrl())
                .expiresAt(url.getExpiresAt())
                .build();
    }
    
    private String generateUniqueShortUrl() {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            long counter = counterService.getNextCounter();
            String shortUrl = Base62Encoder.encode(counter);
            
            if (!urlRepository.existsByShortUrl(shortUrl)) {
                return shortUrl;
            }
        }
        throw new RuntimeException("Unable to generate unique short URL");
    }
}
```

#### Redirect Service
```java
@Service
public class RedirectService {
    
    private final CacheService cacheService;
    private final AnalyticsService analyticsService;
    
    public RedirectResponse redirect(String shortUrl, HttpServletRequest request) {
        // Get long URL from cache/database
        String longUrl = cacheService.getLongUrl(shortUrl);
        
        if (longUrl == null) {
            throw new UrlNotFoundException("Short URL not found");
        }
        
        // Async analytics tracking
        analyticsService.trackClick(shortUrl, request);
        
        return RedirectResponse.builder()
                .longUrl(longUrl)
                .statusCode(HttpStatus.MOVED_PERMANENTLY)
                .build();
    }
}
```

#### Analytics Service
```java
@Service
public class AnalyticsService {
    
    private final UrlAnalyticsRepository analyticsRepository;
    private final GeoLocationService geoLocationService;
    
    @Async
    public void trackClick(String shortUrl, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        
        // Get geographic data
        GeoLocation location = geoLocationService.getLocation(ipAddress);
        
        // Create analytics record
        UrlAnalytics analytics = UrlAnalytics.builder()
                .shortUrl(shortUrl)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .country(location.getCountry())
                .city(location.getCity())
                .referrer(referrer)
                .build();
        
        // Save to database
        analyticsRepository.save(analytics);
        
        // Update click count in cache
        redisTemplate.opsForValue().increment("clicks:" + shortUrl);
    }
}
```

### 3.2 REST API Design

#### Shorten URL
```http
POST /api/v1/urls/shorten
Content-Type: application/json

{
    "longUrl": "https://www.example.com/very/long/url",
    "customAlias": "my-link",  // optional
    "expiresAt": "2024-12-31T23:59:59Z"  // optional
}

Response:
{
    "shortUrl": "https://tinyurl.com/my-link",
    "longUrl": "https://www.example.com/very/long/url",
    "expiresAt": "2024-12-31T23:59:59Z",
    "createdAt": "2024-01-15T10:30:00Z"
}
```

#### Redirect
```http
GET /{shortUrl}

Response:
HTTP/1.1 301 Moved Permanently
Location: https://www.example.com/very/long/url
```

#### Get Analytics
```http
GET /api/v1/urls/{shortUrl}/analytics

Response:
{
    "shortUrl": "abc123",
    "totalClicks": 1500,
    "clicksByCountry": {
        "US": 800,
        "IN": 400,
        "UK": 300
    },
    "clicksByDate": {
        "2024-01-15": 100,
        "2024-01-16": 150
    }
}
```

### 3.3 Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class UrlController {
    
    private final UrlShortenerService urlShortenerService;
    private final RedirectService redirectService;
    private final AnalyticsService analyticsService;
    
    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request) {
        
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{shortUrl}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @PathVariable String shortUrl) {
        
        AnalyticsResponse response = analyticsService.getAnalytics(shortUrl);
        return ResponseEntity.ok(response);
    }
}

@RestController
public class RedirectController {
    
    private final RedirectService redirectService;
    
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortUrl,
            HttpServletRequest request) {
        
        RedirectResponse response = redirectService.redirect(shortUrl, request);
        
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", response.getLongUrl())
                .build();
    }
}
```

## 4. Scalability & Performance

### 4.1 Database Sharding

#### Sharding Strategy
```java
@Component
public class ShardingStrategy {
    
    private static final int SHARD_COUNT = 1000;
    
    public int getShardId(String shortUrl) {
        return Math.abs(shortUrl.hashCode()) % SHARD_COUNT;
    }
    
    public String getShardedTableName(String shortUrl) {
        int shardId = getShardId(shortUrl);
        return "urls_" + shardId;
    }
}
```

### 4.2 Read Replicas
```yaml
# Database configuration
spring:
  datasource:
    master:
      url: jdbc:postgresql://master-db:5432/urlshortener
    slaves:
      - url: jdbc:postgresql://slave1-db:5432/urlshortener
      - url: jdbc:postgresql://slave2-db:5432/urlshortener
```

### 4.3 Rate Limiting
```java
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        
        String clientId = getClientId(request);
        String key = "rate_limit:" + clientId;
        
        // Sliding window rate limiting
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - TimeUnit.HOURS.toMillis(1);
        
        // Remove old entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Count current requests
        Long requestCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
        
        if (requestCount >= 1000) { // 1000 requests per hour
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        
        // Add current request
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), currentTime);
        redisTemplate.expire(key, Duration.ofHours(1));
        
        return true;
    }
}
```

## 5. Security & Reliability

### 5.1 URL Validation
```java
@Component
public class UrlValidator {
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$"
    );
    
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "malicious-site.com",
        "spam-domain.net"
    );
    
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Check URL format
        if (!URL_PATTERN.matcher(url).matches()) {
            return false;
        }
        
        // Check against blocked domains
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return !BLOCKED_DOMAINS.contains(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
```

### 5.2 Circuit Breaker
```java
@Component
public class DatabaseCircuitBreaker {
    
    private final CircuitBreaker circuitBreaker;
    
    public DatabaseCircuitBreaker() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("database");
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Circuit breaker state transition: {}", event));
    }
    
    public Optional<URL> findByShortUrl(String shortUrl) {
        return circuitBreaker.executeSupplier(() -> 
            urlRepository.findByShortUrl(shortUrl));
    }
}
```

## 6. Monitoring & Observability

### 6.1 Metrics
```java
@Component
public class UrlMetrics {
    
    private final Counter shortenCounter;
    private final Counter redirectCounter;
    private final Timer redirectTimer;
    
    public UrlMetrics(MeterRegistry meterRegistry) {
        this.shortenCounter = Counter.builder("url.shorten.total")
            .description("Total URLs shortened")
            .register(meterRegistry);
            
        this.redirectCounter = Counter.builder("url.redirect.total")
            .description("Total redirects")
            .register(meterRegistry);
            
        this.redirectTimer = Timer.builder("url.redirect.duration")
            .description("Redirect response time")
            .register(meterRegistry);
    }
    
    public void incrementShortenCounter() {
        shortenCounter.increment();
    }
    
    public void recordRedirect(Duration duration) {
        redirectCounter.increment();
        redirectTimer.record(duration);
    }
}
```

### 6.2 Health Checks
```java
@Component
public class UrlShortenerHealthIndicator implements HealthIndicator {
    
    private final UrlRepository urlRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        try {
            // Check database connectivity
            urlRepository.count();
            
            // Check Redis connectivity
            redisTemplate.opsForValue().get("health-check");
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("redis", "UP")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 7. Deployment Architecture

### 7.1 Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: url-shortener
spec:
  replicas: 10
  selector:
    matchLabels:
      app: url-shortener
  template:
    metadata:
      labels:
        app: url-shortener
    spec:
      containers:
      - name: url-shortener
        image: url-shortener:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### 7.2 Load Balancer Configuration
```yaml
apiVersion: v1
kind: Service
metadata:
  name: url-shortener-service
spec:
  selector:
    app: url-shortener
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## 8. Cost Analysis

### 8.1 Infrastructure Costs (Monthly)
- **Application Servers**: 10 × $100 = $1,000
- **Database**: $500 (Master + 2 Replicas)
- **Redis Cluster**: $300
- **Load Balancer**: $50
- **CDN**: $100
- **Total**: ~$1,950/month

### 8.2 Cost per Request
- **Total Monthly Requests**: 300B (10B/day × 30 days)
- **Cost per Million Requests**: $6.50
- **Revenue Model**: Premium features, analytics, custom domains

## 9. Future Enhancements

### 9.1 Advanced Features
- **QR Code Generation**: Generate QR codes for short URLs
- **Bulk URL Shortening**: API for shortening multiple URLs
- **A/B Testing**: Split traffic between different destinations
- **Geographic Routing**: Route users to different URLs based on location
- **API Rate Plans**: Tiered pricing for API usage

### 9.2 Machine Learning
- **Fraud Detection**: Identify malicious URLs using ML
- **Click Prediction**: Predict URL popularity
- **Personalized Recommendations**: Suggest relevant URLs

This comprehensive URL shortener system design provides a scalable, reliable, and feature-rich solution capable of handling billions of URLs and redirects while maintaining sub-100ms response times.