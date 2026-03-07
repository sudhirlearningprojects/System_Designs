# Netflix System Design - Interview Guide (CoderPad)

## Table of Contents
1. [Interview Structure](#interview-structure)
2. [Requirements Gathering](#requirements-gathering)
3. [High-Level Design (HLD)](#high-level-design-hld)
4. [Low-Level Design (LLD)](#low-level-design-lld)
5. [Code Implementation](#code-implementation)
6. [Scale Calculations](#scale-calculations)
7. [Trade-offs & Discussion](#trade-offs--discussion)

---

## Interview Structure

**Total Time: 45-60 minutes**

| Phase | Time | Focus |
|-------|------|-------|
| Requirements | 5-7 min | Clarify scope, constraints, scale |
| HLD | 10-15 min | Architecture, components, data flow |
| LLD | 10-15 min | Class design, APIs, database schema |
| Code | 15-20 min | Core functionality implementation |
| Discussion | 5-10 min | Trade-offs, scaling, bottlenecks |

---

## Requirements Gathering

### Functional Requirements
1. **User Management**: Registration, login, profiles
2. **Content Catalog**: Browse movies/shows by genre, search
3. **Video Streaming**: Adaptive bitrate streaming (ABR)
4. **Recommendations**: Personalized content suggestions
5. **Watchlist**: Add/remove content to watch later
6. **Watch History**: Track viewing progress, resume playback
7. **Ratings & Reviews**: Rate content, write reviews

### Non-Functional Requirements
1. **Scale**: 200M users, 100M DAU, 1M concurrent streams
2. **Availability**: 99.99% uptime
3. **Latency**: <2s video start time, <100ms API response
4. **Storage**: 100K videos, 10PB total storage
5. **Bandwidth**: 50 Gbps peak traffic
6. **Global**: Multi-region deployment

### Out of Scope (Clarify with Interviewer)
- Payment processing
- Content upload/encoding pipeline
- Live streaming
- Subtitles/captions
- Parental controls

---

## High-Level Design (HLD)

### System Architecture

```
┌─────────────┐
│   Client    │ (Web/Mobile/TV)
│  (React/iOS)│
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────────────────────────────────────────────┐
│                    CDN (CloudFront)                      │
│              (Video Delivery - 90% traffic)              │
└─────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│                   API Gateway (Kong)                      │
│         (Rate Limiting, Auth, Routing)                    │
└───────────────────────┬──────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   User       │ │   Content    │ │  Streaming   │
│   Service    │ │   Service    │ │   Service    │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  PostgreSQL  │ │ Elasticsearch│ │    Redis     │
│  (User Data) │ │  (Search)    │ │  (Sessions)  │
└──────────────┘ └──────────────┘ └──────────────┘

        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│Recommendation│ │   Analytics  │ │   Encoding   │
│   Service    │ │   Service    │ │   Service    │
│  (ML Model)  │ │   (Kafka)    │ │  (FFmpeg)    │
└──────────────┘ └──────────────┘ └──────────────┘
       │
       ▼
┌──────────────┐
│   Cassandra  │
│ (Watch Data) │
└──────────────┘

┌──────────────────────────────────────────────────────────┐
│                    Object Storage (S3)                    │
│              (Video Files - Multiple Bitrates)            │
└──────────────────────────────────────────────────────────┘
```

### Key Components

1. **CDN (CloudFront)**: Delivers 90% of video traffic, reduces origin load
2. **API Gateway**: Authentication, rate limiting, request routing
3. **User Service**: Registration, login, profile management
4. **Content Service**: Catalog, metadata, search
5. **Streaming Service**: Video URL generation, ABR manifest
6. **Recommendation Service**: ML-based personalized suggestions
7. **Analytics Service**: Real-time event processing (Kafka)
8. **Encoding Service**: Transcode videos to multiple bitrates

### Data Flow

**Video Streaming Flow:**
```
1. User clicks "Play" → API Gateway
2. Streaming Service validates subscription
3. Generate signed CDN URL (time-limited)
4. Return HLS/DASH manifest with multiple bitrates
5. Client requests video chunks from CDN
6. CDN serves from edge (cache hit) or S3 (cache miss)
7. Client adapts bitrate based on network conditions
```

**Recommendation Flow:**
```
1. User opens homepage → Content Service
2. Fetch user watch history from Cassandra
3. Call Recommendation Service with user profile
4. ML model returns personalized content IDs
5. Fetch metadata from PostgreSQL
6. Return ranked list to client
```

---

## Low-Level Design (LLD)

### Core Classes

```java
// Domain Models
class User {
    String userId;
    String email;
    String passwordHash;
    SubscriptionPlan plan;
    LocalDateTime createdAt;
}

class Content {
    String contentId;
    String title;
    ContentType type; // MOVIE, SERIES
    List<String> genres;
    int releaseYear;
    double rating;
    int durationMinutes;
    List<VideoFile> videoFiles;
}

class VideoFile {
    String fileId;
    String contentId;
    Resolution resolution; // 360p, 720p, 1080p, 4K
    int bitrate; // kbps
    String s3Key;
    long fileSizeBytes;
}

class WatchHistory {
    String userId;
    String contentId;
    int progressSeconds;
    LocalDateTime lastWatchedAt;
    boolean completed;
}

class Recommendation {
    String userId;
    List<String> contentIds;
    String algorithm; // COLLABORATIVE, CONTENT_BASED
    double score;
}
```

### API Design

```java
// User Service APIs
POST   /api/v1/users/register
POST   /api/v1/users/login
GET    /api/v1/users/{userId}/profile
PUT    /api/v1/users/{userId}/profile

// Content Service APIs
GET    /api/v1/content/search?q={query}
GET    /api/v1/content/{contentId}
GET    /api/v1/content/browse?genre={genre}&page={page}
GET    /api/v1/content/trending

// Streaming Service APIs
POST   /api/v1/streaming/play
{
  "userId": "user123",
  "contentId": "movie456",
  "quality": "AUTO"
}
Response: {
  "manifestUrl": "https://cdn.netflix.com/movie456/master.m3u8",
  "expiresAt": "2024-01-15T10:30:00Z"
}

// Watch History APIs
POST   /api/v1/watch-history
GET    /api/v1/watch-history/{userId}
PUT    /api/v1/watch-history/{userId}/{contentId}/progress

// Recommendation APIs
GET    /api/v1/recommendations/{userId}
```

### Database Schema

**PostgreSQL (User & Content Metadata)**
```sql
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    subscription_plan VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE content (
    content_id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    type VARCHAR(20), -- MOVIE, SERIES
    genres TEXT[], -- Array of genres
    release_year INT,
    rating DECIMAL(3,1),
    duration_minutes INT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_content_genres ON content USING GIN(genres);

CREATE TABLE video_files (
    file_id VARCHAR(36) PRIMARY KEY,
    content_id VARCHAR(36) REFERENCES content(content_id),
    resolution VARCHAR(10), -- 360p, 720p, 1080p, 4K
    bitrate INT, -- kbps
    s3_key VARCHAR(500),
    file_size_bytes BIGINT
);
CREATE INDEX idx_video_content ON video_files(content_id);
```

**Cassandra (Watch History - High Write Volume)**
```cql
CREATE TABLE watch_history (
    user_id TEXT,
    content_id TEXT,
    progress_seconds INT,
    last_watched_at TIMESTAMP,
    completed BOOLEAN,
    PRIMARY KEY (user_id, last_watched_at, content_id)
) WITH CLUSTERING ORDER BY (last_watched_at DESC);

CREATE TABLE user_watchlist (
    user_id TEXT,
    content_id TEXT,
    added_at TIMESTAMP,
    PRIMARY KEY (user_id, content_id)
);
```

**Redis (Session & Cache)**
```
# User session
SET session:{sessionId} {userId} EX 86400

# Content cache
SET content:{contentId} {json} EX 3600

# Trending cache
SET trending:movies {json} EX 300
```

---

## Code Implementation

### 1. Content Service (Core)

```java
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {
    @Autowired private ContentService contentService;
    
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDTO> getContent(@PathVariable String contentId) {
        return ResponseEntity.ok(contentService.getContent(contentId));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ContentDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(contentService.search(q));
    }
}

@Service
public class ContentService {
    @Autowired private ContentRepository contentRepo;
    @Autowired private RedisTemplate<String, String> redis;
    @Autowired private ObjectMapper mapper;
    
    public ContentDTO getContent(String contentId) {
        // L1: Redis cache
        String cached = redis.opsForValue().get("content:" + contentId);
        if (cached != null) {
            return mapper.readValue(cached, ContentDTO.class);
        }
        
        // L2: Database
        Content content = contentRepo.findById(contentId)
            .orElseThrow(() -> new NotFoundException("Content not found"));
        
        ContentDTO dto = toDTO(content);
        
        // Cache for 1 hour
        redis.opsForValue().set("content:" + contentId, 
            mapper.writeValueAsString(dto), 3600, TimeUnit.SECONDS);
        
        return dto;
    }
    
    public List<ContentDTO> search(String query) {
        // Use Elasticsearch for full-text search
        return contentRepo.searchByTitle(query).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

### 2. Streaming Service (Video URL Generation)

```java
@RestController
@RequestMapping("/api/v1/streaming")
public class StreamingController {
    @Autowired private StreamingService streamingService;
    
    @PostMapping("/play")
    public ResponseEntity<StreamingResponse> play(@RequestBody PlayRequest request) {
        return ResponseEntity.ok(streamingService.generateStreamingUrl(request));
    }
}

@Service
public class StreamingService {
    @Autowired private UserService userService;
    @Autowired private ContentRepository contentRepo;
    
    public StreamingResponse generateStreamingUrl(PlayRequest request) {
        // 1. Validate user subscription
        User user = userService.getUser(request.getUserId());
        if (!user.hasActiveSubscription()) {
            throw new UnauthorizedException("No active subscription");
        }
        
        // 2. Validate content exists
        Content content = contentRepo.findById(request.getContentId())
            .orElseThrow(() -> new NotFoundException("Content not found"));
        
        // 3. Generate signed CDN URL (expires in 4 hours)
        String manifestUrl = generateSignedUrl(content.getContentId());
        
        // 4. Log streaming event for analytics
        logStreamingEvent(request.getUserId(), request.getContentId());
        
        return StreamingResponse.builder()
            .manifestUrl(manifestUrl)
            .expiresAt(LocalDateTime.now().plusHours(4))
            .build();
    }
    
    private String generateSignedUrl(String contentId) {
        // Generate CloudFront signed URL
        String baseUrl = "https://cdn.netflix.com/" + contentId + "/master.m3u8";
        long expiryTime = System.currentTimeMillis() / 1000 + 14400; // 4 hours
        String signature = generateSignature(baseUrl, expiryTime);
        return baseUrl + "?Expires=" + expiryTime + "&Signature=" + signature;
    }
    
    private String generateSignature(String url, long expiry) {
        // Use CloudFront private key to sign URL
        // Implementation depends on AWS SDK
        return "signed_hash_here";
    }
}
```

### 3. Watch History Service

```java
@RestController
@RequestMapping("/api/v1/watch-history")
public class WatchHistoryController {
    @Autowired private WatchHistoryService watchHistoryService;
    
    @PostMapping
    public ResponseEntity<Void> recordWatch(@RequestBody WatchHistoryRequest request) {
        watchHistoryService.recordWatch(request);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<WatchHistoryDTO>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(watchHistoryService.getHistory(userId));
    }
}

@Service
public class WatchHistoryService {
    @Autowired private CassandraTemplate cassandra;
    
    public void recordWatch(WatchHistoryRequest request) {
        WatchHistory history = WatchHistory.builder()
            .userId(request.getUserId())
            .contentId(request.getContentId())
            .progressSeconds(request.getProgressSeconds())
            .lastWatchedAt(LocalDateTime.now())
            .completed(request.isCompleted())
            .build();
        
        cassandra.insert(history);
    }
    
    public List<WatchHistoryDTO> getHistory(String userId) {
        String query = "SELECT * FROM watch_history WHERE user_id = ? LIMIT 50";
        return cassandra.select(query, WatchHistory.class, userId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

### 4. Recommendation Service (Simplified)

```java
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    @Autowired private RecommendationService recommendationService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<ContentDTO>> getRecommendations(@PathVariable String userId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }
}

@Service
public class RecommendationService {
    @Autowired private WatchHistoryService watchHistoryService;
    @Autowired private ContentService contentService;
    
    public List<ContentDTO> getRecommendations(String userId) {
        // 1. Get user watch history
        List<WatchHistoryDTO> history = watchHistoryService.getHistory(userId);
        
        // 2. Extract genres from watched content
        Set<String> preferredGenres = history.stream()
            .flatMap(h -> contentService.getContent(h.getContentId()).getGenres().stream())
            .collect(Collectors.toSet());
        
        // 3. Find similar content (simplified collaborative filtering)
        List<ContentDTO> recommendations = contentService.findByGenres(preferredGenres);
        
        // 4. Filter out already watched
        Set<String> watchedIds = history.stream()
            .map(WatchHistoryDTO::getContentId)
            .collect(Collectors.toSet());
        
        return recommendations.stream()
            .filter(c -> !watchedIds.contains(c.getContentId()))
            .limit(20)
            .collect(Collectors.toList());
    }
}
```

### 5. Adaptive Bitrate Streaming (Client-Side Logic)

```javascript
// HLS.js for adaptive streaming
const video = document.getElementById('video');
const hls = new Hls({
    maxBufferLength: 30,
    maxMaxBufferLength: 60,
    startLevel: -1 // Auto quality
});

// Load manifest
hls.loadSource(manifestUrl);
hls.attachMedia(video);

// Handle quality changes
hls.on(Hls.Events.LEVEL_SWITCHED, (event, data) => {
    console.log('Quality changed to:', data.level);
});

// Track watch progress every 10 seconds
setInterval(() => {
    fetch('/api/v1/watch-history', {
        method: 'POST',
        body: JSON.stringify({
            userId: currentUserId,
            contentId: currentContentId,
            progressSeconds: Math.floor(video.currentTime),
            completed: video.currentTime >= video.duration - 10
        })
    });
}, 10000);
```

---

## Scale Calculations

### Traffic Estimates

**Users:**
- Total: 200M users
- DAU: 100M (50%)
- Concurrent streams: 1M peak

**Video Streaming:**
- Average watch time: 2 hours/day
- Videos per user: 3 videos/day
- Total daily streams: 300M

**Storage:**
- Videos: 100K titles
- Bitrates per video: 5 (360p, 480p, 720p, 1080p, 4K)
- Average video size: 2GB (1080p, 2 hours)
- Total storage: 100K × 5 × 2GB = 1PB (compressed)

**Bandwidth:**
- Concurrent streams: 1M
- Average bitrate: 5 Mbps (1080p)
- Peak bandwidth: 1M × 5 Mbps = 5 Tbps = 625 GB/s

### Database Sizing

**PostgreSQL (Content Metadata):**
- Content records: 100K × 2KB = 200MB
- User records: 200M × 1KB = 200GB
- Total: ~200GB (easily fits in single instance)

**Cassandra (Watch History):**
- Events per day: 100M users × 3 videos × 12 progress updates = 3.6B writes/day
- Write throughput: 3.6B / 86400 = 41,667 writes/sec
- Storage per event: 200 bytes
- Daily storage: 3.6B × 200B = 720GB/day
- 90-day retention: 64.8TB

**Redis (Cache):**
- Active sessions: 1M × 1KB = 1GB
- Content cache: 10K hot videos × 5KB = 50MB
- Total: ~2GB (single instance sufficient)

### API Throughput

**Content Service:**
- Requests: 100M DAU × 10 API calls = 1B requests/day
- QPS: 1B / 86400 = 11,574 QPS
- With 10 instances: 1,157 QPS per instance

**Streaming Service:**
- Play requests: 300M/day
- QPS: 3,472 QPS
- With 5 instances: 694 QPS per instance

---

## Trade-offs & Discussion

### 1. CDN vs Origin Servers
**Decision: Use CDN for 90% of traffic**
- Pros: Reduced latency, lower origin load, global reach
- Cons: Cost ($0.085/GB), cache invalidation complexity
- Trade-off: Cost vs performance (CDN wins for video)

### 2. PostgreSQL vs NoSQL for Content
**Decision: PostgreSQL for content metadata**
- Pros: ACID, complex queries, relationships
- Cons: Vertical scaling limits
- Trade-off: At 100K videos, PostgreSQL is sufficient

### 3. Cassandra for Watch History
**Decision: Cassandra over PostgreSQL**
- Pros: High write throughput (41K writes/sec), horizontal scaling
- Cons: Eventual consistency, no joins
- Trade-off: Consistency vs scalability (scalability wins)

### 4. Push vs Pull for Recommendations
**Decision: Pull-based (on-demand)**
- Pros: Fresh recommendations, no stale data
- Cons: Higher latency (100-200ms)
- Trade-off: Freshness vs latency (freshness wins)

### 5. HLS vs DASH for Streaming
**Decision: HLS (HTTP Live Streaming)**
- Pros: Better Apple device support, simpler
- Cons: Slightly higher latency than DASH
- Trade-off: Compatibility vs latency (compatibility wins)

### Bottlenecks & Solutions

| Bottleneck | Solution |
|------------|----------|
| Database writes (watch history) | Cassandra with 10+ nodes |
| Video encoding time | Distributed encoding with 100+ workers |
| CDN cache misses | Pre-warm cache for trending content |
| Recommendation latency | Pre-compute recommendations (batch job) |
| Search performance | Elasticsearch with 5 shards |

### Follow-up Questions to Expect

1. **How do you handle video encoding?**
   - Use distributed encoding service (FFmpeg workers)
   - Transcode to 5 bitrates in parallel
   - Store in S3, invalidate CDN cache

2. **How do you prevent account sharing?**
   - Limit concurrent streams (4 for premium)
   - Track device fingerprints
   - Geo-location anomaly detection

3. **How do you handle CDN failures?**
   - Multi-CDN strategy (CloudFront + Akamai)
   - Automatic failover to origin
   - Health checks every 30 seconds

4. **How do you personalize recommendations?**
   - Collaborative filtering (user-user similarity)
   - Content-based filtering (genre, actors)
   - Hybrid approach with ML model (TensorFlow)

5. **How do you handle peak traffic (new season release)?**
   - Pre-warm CDN cache
   - Rate limiting on API gateway
   - Auto-scaling (10x capacity)
   - Queue-based request handling

---

## Interview Tips

### Do's
✅ Start with requirements clarification
✅ Draw diagrams (boxes and arrows)
✅ Explain trade-offs for each decision
✅ Mention specific technologies (PostgreSQL, Cassandra, Redis)
✅ Calculate numbers (QPS, storage, bandwidth)
✅ Write clean, compilable code
✅ Handle edge cases (null checks, exceptions)

### Don'ts
❌ Jump into code without design
❌ Over-engineer (don't add unnecessary complexity)
❌ Ignore scale (don't design for 100 users)
❌ Use vague terms ("database", "cache" - be specific)
❌ Write pseudocode (write actual Java/Python)
❌ Forget error handling

### Code Quality Checklist
- [ ] Proper class/method names
- [ ] Input validation
- [ ] Exception handling
- [ ] Comments for complex logic
- [ ] SOLID principles
- [ ] No hardcoded values

---

## Summary

**Key Points to Emphasize:**
1. **CDN is critical** - 90% of traffic, reduces latency from 500ms to 50ms
2. **Cassandra for writes** - 41K writes/sec for watch history
3. **Redis for sessions** - Sub-millisecond latency for auth
4. **Elasticsearch for search** - Full-text search with 100ms latency
5. **Adaptive bitrate** - HLS with 5 quality levels (360p-4K)
6. **Horizontal scaling** - All services are stateless, scale to 100+ instances

**Architecture Highlights:**
- Microservices (User, Content, Streaming, Recommendation)
- Multi-layer caching (Redis, CDN, Browser)
- Event-driven (Kafka for analytics)
- Polyglot persistence (PostgreSQL, Cassandra, Redis, S3)

**Scale Achievements:**
- 1M concurrent streams
- 5 Tbps peak bandwidth
- 41K writes/sec (watch history)
- <2s video start time
- 99.99% availability

Good luck with your interview! 🚀
