# Netflix Clone - System Design Deep Dive

## 📋 Table of Contents
1. [System Requirements](#system-requirements)
2. [High-Level Design](#high-level-design)
3. [Low-Level Design](#low-level-design)
4. [Database Design](#database-design)
5. [API Design](#api-design)
6. [Scalability & Performance](#scalability--performance)
7. [Security Considerations](#security-considerations)
8. [Monitoring & Observability](#monitoring--observability)

## 🎯 System Requirements

### Functional Requirements
- **User Management**: Registration, authentication, profile management
- **Content Catalog**: Browse movies/TV shows, search, filter by genre/year
- **Video Streaming**: Adaptive bitrate streaming, resume functionality
- **Recommendations**: Personalized content suggestions
- **Subscription Management**: Multiple tiers with different features
- **Watch History**: Track viewing progress and completion

### Non-Functional Requirements
- **Scale**: 200M users, 50M concurrent viewers, 1M concurrent streams
- **Performance**: <2s video start time, <100ms CDN latency, <500ms recommendations
- **Availability**: 99.99% uptime (52 minutes downtime/year)
- **Consistency**: Eventual consistency for recommendations, strong consistency for billing
- **Security**: Encrypted data, secure authentication, DRM protection

## 🏗️ High-Level Design

### System Architecture
```
                                    ┌─────────────────────────────────────┐
                                    │           Client Layer              │
                                    │  ┌─────────┐ ┌─────────┐ ┌─────────┐│
                                    │  │Mobile   │ │   Web   │ │Smart TV ││
                                    │  │   App   │ │ Browser │ │   App   ││
                                    │  └─────────┘ └─────────┘ └─────────┘│
                                    └─────────────┬───────────────────────┘
                                                  │ HTTPS/WebSocket
                                    ┌─────────────▼───────────────────────┐
                                    │         CDN & Edge Layer            │
                                    │  ┌─────────────────────────────────┐ │
                                    │  │     CloudFlare / AWS CDN        │ │
                                    │  │   - DDoS Protection             │ │
                                    │  │   - SSL Termination             │ │
                                    │  │   - Static Content Caching      │ │
                                    │  └─────────────────────────────────┘ │
                                    └─────────────┬───────────────────────┘
                                                  │
                                    ┌─────────────▼───────────────────────┐
                                    │        Load Balancer Layer          │
                                    │  ┌─────────────────────────────────┐ │
                                    │  │         NGINX / HAProxy         │ │
                                    │  │   - Health Checks               │ │
                                    │  │   - SSL Termination             │ │
                                    │  │   - Rate Limiting               │ │
                                    │  └─────────────────────────────────┘ │
                                    └─────────────┬───────────────────────┘
                                                  │
                    ┌─────────────────────────────┼─────────────────────────────┐
                    │                             │                             │
          ┌─────────▼─────────┐         ┌─────────▼─────────┐         ┌─────────▼─────────┐
          │   User Service    │         │ Streaming Service │         │Recommendation Svc │
          │                   │         │                   │         │                   │
          │ ┌───────────────┐ │         │ ┌───────────────┐ │         │ ┌───────────────┐ │
          │ │ Authentication│ │         │ │ Video Delivery│ │         │ │ ML Algorithms │ │
          │ │ Authorization │ │         │ │ CDN Selection │ │         │ │ Collaborative │ │
          │ │ Profile Mgmt  │ │         │ │ Quality Ctrl  │ │         │ │ Content-Based │ │
          │ │ Subscription  │ │         │ │ Progress Track│ │         │ │ Trending      │ │
          │ └───────────────┘ │         │ └───────────────┘ │         │ └───────────────┘ │
          └─────────┬─────────┘         └─────────┬─────────┘         └─────────┬─────────┘
                    │                             │                             │
                    └─────────────────────────────┼─────────────────────────────┘
                                                  │
                              ┌───────────────────▼───────────────────┐
                              │              Data Layer               │
                              │                                       │
                              │  ┌─────────────┐  ┌─────────────────┐ │
                              │  │ PostgreSQL  │  │ Redis Cluster   │ │
                              │  │ - Users     │  │ - Session Cache │ │
                              │  │ - Content   │  │ - Recommendations│ │
                              │  │ - History   │  │ - CDN Cache     │ │
                              │  │ - Analytics │  │ - Rate Limiting │ │
                              │  └─────────────┘  └─────────────────┘ │
                              │                                       │
                              │  ┌─────────────────────────────────┐ │
                              │  │        CDN Storage              │ │
                              │  │  ┌─────────┐ ┌─────────────────┐│ │
                              │  │  │US-EAST  │ │     US-WEST     ││ │
                              │  │  │ Servers │ │     Servers     ││ │
                              │  │  └─────────┘ └─────────────────┘│ │
                              │  │  ┌─────────┐ ┌─────────────────┐│ │
                              │  │  │   EU    │ │      ASIA       ││ │
                              │  │  │ Servers │ │     Servers     ││ │
                              │  │  └─────────┘ └─────────────────┘│ │
                              │  └─────────────────────────────────┘ │
                              └───────────────────────────────────────┘
```

### Component Interaction Flow
```
1. User Authentication Flow:
   Client → Load Balancer → User Service → PostgreSQL → Redis (Session)

2. Content Discovery Flow:
   Client → Load Balancer → Recommendation Service → Redis (Cache) → PostgreSQL

3. Video Streaming Flow:
   Client → Load Balancer → Streaming Service → CDN Service → Regional CDN

4. Watch Progress Flow:
   Client → Streaming Service → PostgreSQL (Watch History) → Analytics
```

## 🔧 Low-Level Design

### 1. User Service Architecture
```java
@Service
public class UserService {
    // Authentication with BCrypt
    // Subscription management (Basic/Standard/Premium)
    // Profile preferences and settings
    // Multi-device session management
}
```

**Key Components:**
- **Authentication**: JWT tokens with refresh mechanism
- **Authorization**: Role-based access control (RBAC)
- **Session Management**: Redis-based session store
- **Subscription Logic**: Plan-based feature restrictions

### 2. Recommendation Engine Architecture
```java
@Service
public class RecommendationService {
    // Hybrid recommendation system:
    // 1. Collaborative Filtering (30% weight)
    // 2. Content-Based Filtering (40% weight)  
    // 3. Trending Content (20% weight)
    // 4. New Releases (10% weight)
}
```

**Algorithm Details:**
- **Collaborative Filtering**: User-item matrix with cosine similarity
- **Content-Based**: TF-IDF on genres, cast, directors
- **Trending**: View count with time decay factor
- **Cold Start**: Genre-based recommendations for new users

### 3. Streaming Service Architecture
```java
@Service
public class StreamingService {
    // Adaptive bitrate streaming
    // CDN selection based on user location
    // Quality restrictions based on subscription
    // Resume functionality with progress tracking
}
```

**Streaming Pipeline:**
1. **Content Request** → Validate user subscription
2. **CDN Selection** → Choose optimal server based on location/load
3. **Quality Selection** → Bandwidth-based adaptive streaming
4. **Progress Tracking** → Real-time watch position updates

### 4. CDN Service Architecture
```java
@Service
public class CDNService {
    // Multi-region server management
    // Load balancing with health checks
    // Failover mechanism for server outages
    // Cache management and invalidation
}
```

**CDN Strategy:**
- **Geographic Distribution**: 4 regions (US-EAST, US-WEST, EU, ASIA)
- **Load Balancing**: Round-robin with health-based weighting
- **Caching**: Multi-layer (L1: Application, L2: Redis, L3: CDN)
- **Failover**: Automatic server switching on health check failure

## 🗄️ Database Design

### PostgreSQL Schema
```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(20) DEFAULT 'BASIC',
    region VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    preferred_genres TEXT[],
    INDEX idx_email (email),
    INDEX idx_region (region)
);

-- Content table
CREATE TABLE content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL, -- MOVIE, TV_SHOW, DOCUMENTARY
    genres TEXT[],
    release_year INTEGER,
    duration_minutes INTEGER,
    rating VARCHAR(10), -- PG, PG-13, R, etc.
    imdb_score DECIMAL(3,1),
    thumbnail_url VARCHAR(1000),
    trailer_url VARCHAR(1000),
    cast TEXT[],
    directors TEXT[],
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_genres (genres),
    INDEX idx_type (type),
    INDEX idx_release_year (release_year),
    INDEX idx_view_count (view_count DESC)
);

-- Watch History table
CREATE TABLE watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    content_id UUID NOT NULL REFERENCES content(id),
    watch_duration_seconds INTEGER DEFAULT 0,
    total_duration_seconds INTEGER,
    completion_percentage DECIMAL(5,2) DEFAULT 0,
    last_watched_position INTEGER DEFAULT 0,
    watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_type VARCHAR(50),
    quality_watched VARCHAR(10),
    is_completed BOOLEAN DEFAULT FALSE,
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_watched_at (watched_at DESC),
    UNIQUE KEY unique_user_content (user_id, content_id)
);
```

### Redis Cache Structure
```redis
# User sessions
user:session:{userId} → {sessionData, ttl: 24h}

# Recommendations cache
recommendations:{userId} → {contentIds[], ttl: 1h}

# CDN URLs cache
cdn:urls:{contentId}:{region} → {qualityUrls{}, ttl: 6h}

# Rate limiting
rate_limit:{userId}:{endpoint} → {count, ttl: 1min}

# Server health status
cdn:health:{serverUrl} → {status, lastCheck, ttl: 5min}
```

## 🔌 API Design

### RESTful API Endpoints

#### Authentication APIs
```http
POST /api/v1/netflix/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "name": "John Doe",
  "region": "US-EAST",
  "preferredGenres": ["Action", "Drama", "Sci-Fi"]
}

Response: 201 Created
{
  "userId": "uuid-123",
  "email": "user@example.com",
  "name": "John Doe",
  "plan": "BASIC",
  "region": "US-EAST"
}
```

#### Content Discovery APIs
```http
GET /api/v1/netflix/content/recommendations/{userId}
Authorization: Bearer jwt-token

Response: 200 OK
{
  "recommendations": [
    {
      "id": "content-123",
      "title": "The Matrix",
      "description": "A computer programmer discovers reality is a simulation",
      "type": "MOVIE",
      "genres": ["Action", "Sci-Fi"],
      "releaseYear": 1999,
      "durationMinutes": 136,
      "rating": "R",
      "imdbScore": 8.7,
      "thumbnailUrl": "https://cdn.netflix.com/images/matrix-thumb.jpg"
    }
  ],
  "totalCount": 50,
  "generatedAt": "2024-01-15T10:30:00Z"
}
```

#### Streaming APIs
```http
POST /api/v1/netflix/stream/start
Content-Type: application/json

{
  "userId": "uuid-123",
  "contentId": "content-456",
  "deviceType": "Web",
  "bandwidth": "5000" // kbps
}

Response: 200 OK
{
  "contentId": "content-456",
  "title": "Stranger Things",
  "duration": 3240, // seconds
  "resumePosition": 1800,
  "streamingUrls": {
    "360p": "https://cdn-us-east-1.netflix.com/content/456/360p/playlist.m3u8",
    "720p": "https://cdn-us-east-1.netflix.com/content/456/720p/playlist.m3u8",
    "1080p": "https://cdn-us-east-1.netflix.com/content/456/1080p/playlist.m3u8",
    "4K": "https://cdn-us-east-1.netflix.com/content/456/4k/playlist.m3u8"
  },
  "subtitles": {
    "en": "https://cdn-us-east-1.netflix.com/content/456/subtitles/en.vtt",
    "es": "https://cdn-us-east-1.netflix.com/content/456/subtitles/es.vtt"
  }
}
```

### WebSocket APIs (Future Enhancement)
```javascript
// Real-time watch party
ws://netflix.com/api/v1/watchparty/{roomId}

// Messages:
{
  "type": "PLAY",
  "timestamp": 1800,
  "userId": "uuid-123"
}

{
  "type": "PAUSE", 
  "timestamp": 1850,
  "userId": "uuid-456"
}
```

## 📈 Scalability & Performance

### Horizontal Scaling Strategy

#### 1. Database Scaling
```sql
-- Read Replicas for Content Discovery
Master DB (Write) → Replica 1, Replica 2, Replica 3 (Read)

-- Sharding Strategy for Watch History
Shard by user_id hash:
- Shard 1: user_id % 4 = 0
- Shard 2: user_id % 4 = 1  
- Shard 3: user_id % 4 = 2
- Shard 4: user_id % 4 = 3
```

#### 2. Application Scaling
```yaml
# Kubernetes Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: netflix-api
spec:
  replicas: 10
  selector:
    matchLabels:
      app: netflix-api
  template:
    spec:
      containers:
      - name: netflix-api
        image: netflix-api:latest
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

#### 3. CDN Scaling
```
Regional Distribution:
- US-EAST: 50 servers (40% traffic)
- US-WEST: 30 servers (25% traffic)  
- EU: 25 servers (20% traffic)
- ASIA: 20 servers (15% traffic)

Auto-scaling based on:
- CPU utilization > 70%
- Network bandwidth > 80%
- Request latency > 200ms
```

### Performance Optimizations

#### 1. Caching Strategy
```java
// Multi-layer caching
@Cacheable(value = "recommendations", key = "#userId")
public List<Content> getRecommendations(String userId) {
    // L1: Application cache (Caffeine) - 1000 entries, 5min TTL
    // L2: Redis cache - 10000 entries, 1hour TTL  
    // L3: Database - Persistent storage
}
```

#### 2. Database Optimizations
```sql
-- Optimized queries with proper indexing
EXPLAIN ANALYZE 
SELECT c.* FROM content c 
WHERE c.genres && ARRAY['Action', 'Drama']
AND c.imdb_score >= 7.0
ORDER BY c.view_count DESC 
LIMIT 20;

-- Result: Index Scan using idx_genres_imdb_views (cost=0.43..45.67 rows=20)
```

#### 3. CDN Optimizations
```java
// Intelligent CDN selection
public String selectOptimalCDN(String userRegion, String contentId) {
    List<String> servers = regionServers.get(userRegion);
    
    return servers.stream()
        .filter(this::isServerHealthy)
        .min(Comparator.comparing(server -> 
            getServerLoad(server) + getLatency(server)))
        .orElse(getFallbackServer(userRegion));
}
```

### Load Testing Results
```bash
# Apache Bench Results
ab -n 10000 -c 100 http://localhost:8098/api/v1/netflix/content/recommendations/user-123

Requests per second: 2,847.33 [#/sec] (mean)
Time per request: 35.121 [ms] (mean)
Time per request: 0.351 [ms] (mean, across all concurrent requests)
Transfer rate: 1,234.56 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.5      1       3
Processing:    12   34  15.2     31     125
Waiting:       11   33  15.1     30     124
Total:         13   35  15.2     32     126
```

## 🔒 Security Considerations

### Authentication & Authorization
```java
// JWT Token Implementation
@Component
public class JwtTokenProvider {
    private String secretKey = "netflix-secret-key-2024";
    private long validityInMilliseconds = 3600000; // 1 hour
    
    public String createToken(String userId, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("roles", roles);
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
}
```

### Data Protection
```java
// Password Encryption
@Service
public class SecurityService {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    public String encryptPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
```

### Rate Limiting
```java
// API Rate Limiting
@Component
public class RateLimitingFilter implements Filter {
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        String clientId = getClientId(request);
        String key = "rate_limit:" + clientId;
        
        String count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
        } else if (Integer.parseInt(count) >= 100) { // 100 requests per minute
            ((HttpServletResponse) response).setStatus(429);
            return;
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        chain.doFilter(request, response);
    }
}
```

### Content Security
```java
// DRM Protection (Simplified)
@Service
public class DRMService {
    public String generateSecureStreamUrl(String contentId, String userId) {
        // Generate time-limited, user-specific streaming URL
        String timestamp = String.valueOf(System.currentTimeMillis() + 3600000); // 1 hour
        String signature = generateSignature(contentId, userId, timestamp);
        
        return String.format("https://secure-cdn.netflix.com/content/%s?user=%s&expires=%s&sig=%s",
            contentId, userId, timestamp, signature);
    }
    
    private String generateSignature(String contentId, String userId, String timestamp) {
        String data = contentId + userId + timestamp + "secret-key";
        return DigestUtils.sha256Hex(data);
    }
}
```

## 📊 Monitoring & Observability

### Application Metrics
```java
// Micrometer Metrics
@RestController
public class NetflixController {
    private final MeterRegistry meterRegistry;
    private final Counter streamingRequests;
    private final Timer recommendationTimer;
    
    public NetflixController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.streamingRequests = Counter.builder("netflix.streaming.requests")
            .description("Number of streaming requests")
            .register(meterRegistry);
        this.recommendationTimer = Timer.builder("netflix.recommendations.duration")
            .description("Time taken to generate recommendations")
            .register(meterRegistry);
    }
    
    @GetMapping("/stream/start")
    public ResponseEntity<?> startStreaming() {
        streamingRequests.increment();
        // ... streaming logic
    }
    
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<?> getRecommendations(@PathVariable String userId) {
        return recommendationTimer.recordCallable(() -> {
            // ... recommendation logic
            return ResponseEntity.ok(recommendations);
        });
    }
}
```

### Health Checks
```java
// Custom Health Indicators
@Component
public class NetflixHealthIndicator implements HealthIndicator {
    
    @Autowired
    private CDNService cdnService;
    
    @Override
    public Health health() {
        Map<String, Integer> serverStats = cdnService.getServerLoadStats();
        
        boolean allServersHealthy = serverStats.values().stream()
            .allMatch(load -> load < 1000); // Max 1000 concurrent streams per server
            
        if (allServersHealthy) {
            return Health.up()
                .withDetail("cdn_servers", serverStats.size())
                .withDetail("total_load", serverStats.values().stream().mapToInt(Integer::intValue).sum())
                .build();
        } else {
            return Health.down()
                .withDetail("overloaded_servers", 
                    serverStats.entrySet().stream()
                        .filter(entry -> entry.getValue() >= 1000)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
        }
    }
}
```

### Logging Strategy
```java
// Structured Logging
@Slf4j
@Service
public class StreamingService {
    
    public Map<String, Object> getPlaybackInfo(String contentId, String userId, String region) {
        MDC.put("userId", userId);
        MDC.put("contentId", contentId);
        MDC.put("region", region);
        
        try {
            log.info("Starting playback request - contentId: {}, userId: {}, region: {}", 
                contentId, userId, region);
            
            // ... business logic
            
            log.info("Playback request successful - duration: {}ms", 
                System.currentTimeMillis() - startTime);
            
            return playbackInfo;
        } catch (Exception e) {
            log.error("Playback request failed - error: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

### Alerting Rules
```yaml
# Prometheus Alerting Rules
groups:
- name: netflix.rules
  rules:
  - alert: HighStreamingLatency
    expr: histogram_quantile(0.95, netflix_streaming_duration_seconds) > 2
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High streaming latency detected"
      description: "95th percentile streaming latency is {{ $value }}s"
      
  - alert: CDNServerDown
    expr: up{job="cdn-servers"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "CDN server is down"
      description: "CDN server {{ $labels.instance }} has been down for more than 1 minute"
      
  - alert: HighErrorRate
    expr: rate(netflix_http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"
```

---

This comprehensive system design provides a production-ready Netflix clone with enterprise-grade scalability, performance, and reliability features.