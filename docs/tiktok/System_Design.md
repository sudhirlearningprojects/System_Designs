# TikTok Clone - Short Video & Live Streaming Platform

## 1. System Overview

TikTok is a short-form video sharing platform with live streaming capabilities, supporting billions of users worldwide. The platform enables users to create, share, and discover short videos (15-60 seconds) with a highly personalized recommendation algorithm.

### Key Features
- **Short Video Sharing**: Upload, edit, and share 15-60 second videos
- **For You Feed**: AI-powered personalized video recommendations
- **Live Streaming**: Real-time video broadcasting with interactive features
- **Social Interactions**: Likes, comments, shares, follows
- **Video Effects**: Filters, stickers, AR effects, music overlay
- **Duet & Stitch**: Collaborative video creation
- **Trending Challenges**: Hashtag-based viral content

### Scale Requirements
- **Users**: 1.5B monthly active users, 500M daily active users
- **Videos**: 1B videos uploaded daily, 60M videos/hour
- **Storage**: 500PB total video storage
- **Bandwidth**: 100 Gbps peak bandwidth
- **Live Streams**: 10M concurrent live streams
- **Latency**: <100ms video feed load, <3s live stream latency
- **Availability**: 99.99% uptime

---

## 2. High-Level Design (HLD)

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  (Mobile Apps: iOS/Android, Web Browser, Smart TV)              │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CDN Layer (CloudFront)                      │
│  - Video delivery (HLS/DASH)                                     │
│  - Thumbnail caching                                             │
│  - Edge locations worldwide                                      │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway + Load Balancer                   │
│  - Rate limiting (10K req/sec per user)                          │
│  - Authentication (JWT)                                          │
│  - Request routing                                               │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Microservices Layer                         │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Video Service│  │ User Service │  │ Feed Service │          │
│  │ - Upload     │  │ - Profile    │  │ - For You    │          │
│  │ - Metadata   │  │ - Follow     │  │ - Following  │          │
│  │ - Transcode  │  │ - Auth       │  │ - Trending   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ Live Service │  │Social Service│  │ Search Svc   │          │
│  │ - RTMP       │  │ - Like       │  │ - Video      │          │
│  │ - HLS        │  │ - Comment    │  │ - User       │          │
│  │ - Chat       │  │ - Share      │  │ - Hashtag    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  PostgreSQL  │  │   Cassandra  │  │    Redis     │          │
│  │  - Users     │  │  - Videos    │  │  - Cache     │          │
│  │  - Metadata  │  │  - Comments  │  │  - Sessions  │          │
│  │              │  │  - Likes     │  │  - Counters  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │      S3      │  │ Elasticsearch│  │    Kafka     │          │
│  │  - Videos    │  │  - Search    │  │  - Events    │          │
│  │  - Thumbnails│  │  - Analytics │  │  - Logs      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Core Components

#### Video Upload Pipeline
```
User → API Gateway → Video Service → S3 (Raw) → Kafka (Event)
                                                      ↓
                                            Video Processing Worker
                                                      ↓
                                    ┌─────────────────┴─────────────────┐
                                    ▼                                     ▼
                            Transcoding Service                   Thumbnail Service
                            (FFmpeg/AWS MediaConvert)             (Extract frame)
                                    ↓                                     ↓
                            Multiple Resolutions                   Generate thumbnail
                            (360p, 480p, 720p, 1080p)                    ↓
                                    ↓                                     ▼
                            S3 (Processed) ←──────────────────────── S3 (Thumbnails)
                                    ↓
                            CloudFront CDN
                                    ↓
                            Update Video Metadata (Cassandra)
```

#### Live Streaming Pipeline
```
Broadcaster (RTMP) → Media Server (Nginx-RTMP/Wowza)
                              ↓
                    ┌─────────┴─────────┐
                    ▼                   ▼
            Transcoding            Recording
            (Multiple bitrates)    (S3 Storage)
                    ↓
            HLS/DASH Packaging
                    ↓
            CDN Distribution
                    ↓
            Viewers (HLS Player)
```

#### Recommendation Algorithm (For You Feed)
```
User Request → Feed Service
                    ↓
        ┌───────────┴───────────┐
        ▼                       ▼
    User Profile          Video Candidates
    - Watch history       - Trending videos
    - Likes/shares        - Following feed
    - Interactions        - Similar content
        ↓                       ↓
        └───────────┬───────────┘
                    ▼
            ML Ranking Model
            - Engagement prediction
            - Diversity score
            - Freshness score
                    ↓
            Personalized Feed
            (20 videos per page)
```

---

## 3. Low-Level Design (LLD)

### 3.1 Database Schema

#### PostgreSQL (User & Metadata)
```sql
-- Users table
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    bio TEXT,
    profile_picture_url VARCHAR(500),
    is_verified BOOLEAN DEFAULT FALSE,
    follower_count BIGINT DEFAULT 0,
    following_count BIGINT DEFAULT 0,
    video_count BIGINT DEFAULT 0,
    total_likes BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Live streams table
CREATE TABLE live_streams (
    stream_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stream_key VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL, -- SCHEDULED, LIVE, ENDED
    rtmp_url VARCHAR(500) NOT NULL,
    hls_url VARCHAR(500) NOT NULL,
    viewer_count BIGINT DEFAULT 0,
    peak_viewer_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    gift_count BIGINT DEFAULT 0,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_user_status (user_id, status),
    INDEX idx_status_started (status, started_at)
);
```

#### Cassandra (Videos & Social Graph)
```cql
-- Videos table (partitioned by user_id for efficient user video queries)
CREATE TABLE videos (
    video_id BIGINT,
    user_id BIGINT,
    video_url TEXT,
    thumbnail_url TEXT,
    caption TEXT,
    duration_seconds INT,
    view_count BIGINT,
    like_count BIGINT,
    comment_count BIGINT,
    share_count BIGINT,
    video_quality TEXT,
    file_size BIGINT,
    is_public BOOLEAN,
    allow_comments BOOLEAN,
    allow_duet BOOLEAN,
    allow_stitch BOOLEAN,
    created_at TIMESTAMP,
    PRIMARY KEY ((user_id), created_at, video_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Likes table
CREATE TABLE likes (
    user_id BIGINT,
    video_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY ((user_id), video_id)
);

-- Comments table
CREATE TABLE comments (
    comment_id BIGINT,
    video_id BIGINT,
    user_id BIGINT,
    content TEXT,
    parent_comment_id BIGINT,
    like_count BIGINT,
    reply_count BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY ((video_id), created_at, comment_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Follows table
CREATE TABLE follows (
    follower_id BIGINT,
    following_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY ((follower_id), following_id)
);
```

#### Redis Cache Structure
```
# User session
user:session:{user_id} → {jwt_token, expires_at}

# Video metadata cache
video:{video_id} → {video_json}

# Feed cache (5 min TTL)
feed:foryou:{user_id}:{page} → [video_ids]
feed:following:{user_id}:{page} → [video_ids]

# Live stream viewers (Set)
live:viewers:{stream_id} → {user_id1, user_id2, ...}

# Active live streams (Set)
live:active → {stream_id1, stream_id2, ...}

# Trending videos (Sorted Set by engagement score)
trending:videos → {video_id: score}

# Video engagement counters
video:views:{video_id} → counter
video:likes:{video_id} → counter
```

### 3.2 API Design

#### Video APIs
```http
# Upload video
POST /api/v1/videos/upload
Content-Type: multipart/form-data
Authorization: Bearer {jwt_token}

Request:
- file: video file (max 100MB)
- caption: string
- isPublic: boolean
- allowComments: boolean
- allowDuet: boolean
- allowStitch: boolean

Response: 200 OK
{
  "videoId": 123456,
  "videoUrl": "https://cdn.tiktok.com/videos/abc123.mp4",
  "thumbnailUrl": "https://cdn.tiktok.com/thumbnails/abc123.jpg",
  "status": "PROCESSING"
}

# Get For You feed
GET /api/v1/videos/feed/foryou?userId=123&page=0&size=20
Response: 200 OK
{
  "videos": [
    {
      "videoId": 456,
      "userId": 789,
      "username": "john_doe",
      "profilePictureUrl": "https://...",
      "videoUrl": "https://cdn.tiktok.com/videos/xyz.mp4",
      "thumbnailUrl": "https://cdn.tiktok.com/thumbnails/xyz.jpg",
      "caption": "Amazing dance moves! #fyp",
      "durationSeconds": 30,
      "viewCount": 1500000,
      "likeCount": 250000,
      "commentCount": 5000,
      "shareCount": 10000,
      "isLiked": false,
      "isFollowing": true
    }
  ],
  "nextCursor": "page_1"
}

# Like video
POST /api/v1/videos/{videoId}/like?userId=123
Response: 200 OK

# Unlike video
DELETE /api/v1/videos/{videoId}/like?userId=123
Response: 200 OK

# Increment view count
POST /api/v1/videos/{videoId}/view
Response: 200 OK
```

#### Live Streaming APIs
```http
# Create live stream
POST /api/v1/live/create?userId=123
Content-Type: application/json

Request:
{
  "title": "Live Q&A Session",
  "description": "Ask me anything!"
}

Response: 200 OK
{
  "streamId": 789,
  "streamKey": "abc123-def456-ghi789",
  "rtmpUrl": "rtmp://live.tiktok.com/live/abc123-def456-ghi789",
  "hlsUrl": "https://cdn.tiktok.com/live/abc123-def456-ghi789/index.m3u8",
  "status": "SCHEDULED"
}

# Start live stream
POST /api/v1/live/start?streamKey=abc123-def456-ghi789
Response: 200 OK

# End live stream
POST /api/v1/live/end?streamKey=abc123-def456-ghi789
Response: 200 OK

# Get active live streams
GET /api/v1/live/active
Response: 200 OK
{
  "streams": [
    {
      "streamId": 789,
      "userId": 123,
      "username": "john_doe",
      "title": "Live Q&A Session",
      "viewerCount": 5000,
      "hlsUrl": "https://cdn.tiktok.com/live/abc123/index.m3u8"
    }
  ]
}

# Join live stream
POST /api/v1/live/{streamId}/join?userId=123
Response: 200 OK

# Leave live stream
POST /api/v1/live/{streamId}/leave?userId=123
Response: 200 OK
```

#### WebSocket (Live Stream Interactions)
```javascript
// Connect to live stream
const ws = new WebSocket('ws://api.tiktok.com/ws/live?streamId=789');

// Send comment
ws.send(JSON.stringify({
  type: 'COMMENT',
  userId: 123,
  username: 'john_doe',
  message: 'Great stream!'
}));

// Send like
ws.send(JSON.stringify({
  type: 'LIKE',
  userId: 123
}));

// Send gift
ws.send(JSON.stringify({
  type: 'GIFT',
  userId: 123,
  giftId: 5,
  giftName: 'Rose',
  giftValue: 100
}));

// Receive messages
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  // Handle COMMENT, LIKE, GIFT events
};
```

### 3.3 Video Processing Workflow

#### Transcoding Pipeline
```java
@Service
public class VideoProcessingService {
    
    @KafkaListener(topics = "video-processing")
    public void processVideo(VideoProcessingEvent event) {
        String videoUrl = event.getVideoUrl();
        
        // 1. Download video from S3
        File videoFile = downloadFromS3(videoUrl);
        
        // 2. Transcode to multiple resolutions
        List<String> transcodedUrls = transcodeVideo(videoFile, 
            Arrays.asList("360p", "480p", "720p", "1080p"));
        
        // 3. Generate thumbnail
        String thumbnailUrl = generateThumbnail(videoFile);
        
        // 4. Extract audio for music matching
        String audioUrl = extractAudio(videoFile);
        
        // 5. Upload processed files to S3
        uploadToS3(transcodedUrls);
        uploadToS3(thumbnailUrl);
        
        // 6. Update video metadata
        updateVideoMetadata(event.getVideoId(), transcodedUrls, thumbnailUrl);
        
        // 7. Trigger recommendation indexing
        kafkaTemplate.send("video-indexed", event.getVideoId());
    }
    
    private List<String> transcodeVideo(File video, List<String> resolutions) {
        // Use FFmpeg or AWS MediaConvert
        // ffmpeg -i input.mp4 -vf scale=1280:720 -c:v libx264 -crf 23 output_720p.mp4
        return transcodedUrls;
    }
}
```

#### Live Streaming Setup (Nginx-RTMP)
```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        
        application live {
            live on;
            record off;
            
            # HLS packaging
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;
            
            # Transcoding to multiple bitrates
            exec ffmpeg -i rtmp://localhost/$app/$name
                -c:v libx264 -b:v 2500k -s 1280x720 -f flv rtmp://localhost/hls/$name_720p
                -c:v libx264 -b:v 1000k -s 854x480 -f flv rtmp://localhost/hls/$name_480p
                -c:v libx264 -b:v 500k -s 640x360 -f flv rtmp://localhost/hls/$name_360p;
            
            # Authentication callback
            on_publish http://api.tiktok.com/api/v1/live/auth;
            
            # Notify when stream starts/ends
            on_publish_done http://api.tiktok.com/api/v1/live/end;
        }
    }
}
```

### 3.4 Recommendation Algorithm

#### For You Feed Generation
```java
@Service
public class RecommendationService {
    
    public List<Video> generateForYouFeed(Long userId, int page, int size) {
        // 1. Get user profile and preferences
        UserProfile profile = getUserProfile(userId);
        
        // 2. Fetch candidate videos from multiple sources
        List<Video> candidates = new ArrayList<>();
        
        // Source 1: Trending videos (20%)
        candidates.addAll(getTrendingVideos(size * 0.2));
        
        // Source 2: Following feed (30%)
        candidates.addAll(getFollowingVideos(userId, size * 0.3));
        
        // Source 3: Similar to liked videos (30%)
        candidates.addAll(getSimilarVideos(profile.getLikedVideos(), size * 0.3));
        
        // Source 4: Explore new content (20%)
        candidates.addAll(getExploreVideos(size * 0.2));
        
        // 3. Rank videos using ML model
        List<ScoredVideo> scored = rankVideos(candidates, profile);
        
        // 4. Apply diversity and freshness filters
        List<Video> diversified = applyDiversityFilter(scored);
        
        // 5. Return paginated results
        return diversified.subList(page * size, (page + 1) * size);
    }
    
    private List<ScoredVideo> rankVideos(List<Video> candidates, UserProfile profile) {
        return candidates.stream()
            .map(video -> {
                double score = 0.0;
                
                // Engagement prediction (40%)
                score += predictEngagement(video, profile) * 0.4;
                
                // Content similarity (30%)
                score += calculateSimilarity(video, profile.getInterests()) * 0.3;
                
                // Freshness (20%)
                score += calculateFreshness(video) * 0.2;
                
                // Creator popularity (10%)
                score += calculateCreatorScore(video.getUserId()) * 0.1;
                
                return new ScoredVideo(video, score);
            })
            .sorted(Comparator.comparingDouble(ScoredVideo::getScore).reversed())
            .collect(Collectors.toList());
    }
    
    private double predictEngagement(Video video, UserProfile profile) {
        // Use ML model (TensorFlow/PyTorch) to predict:
        // P(like | user, video) + P(comment | user, video) + P(share | user, video)
        
        // Features:
        // - User: watch history, like history, demographics
        // - Video: category, hashtags, music, duration, engagement rate
        // - Context: time of day, device type, location
        
        return mlModel.predict(video, profile);
    }
}
```

---

## 4. Key Design Decisions

### 4.1 Video Storage Strategy

**Decision**: Use S3 for video storage with CloudFront CDN

**Rationale**:
- **Scalability**: S3 can store unlimited videos
- **Cost-effective**: $0.023/GB/month for S3 Standard
- **Global delivery**: CloudFront edge locations reduce latency
- **Durability**: 99.999999999% durability

**Implementation**:
```
Video Upload → S3 (us-east-1) → CloudFront → Edge Locations
                                                    ↓
                                            User (nearest edge)
```

### 4.2 Database Partitioning

**Decision**: Use Cassandra for videos/comments, PostgreSQL for users

**Rationale**:
- **Cassandra**: Write-heavy workload (1B videos/day), horizontal scalability
- **PostgreSQL**: ACID transactions for user accounts, complex queries
- **Partition key**: `user_id` for videos (efficient user profile queries)

### 4.3 Live Streaming Protocol

**Decision**: RTMP for ingestion, HLS for delivery

**Rationale**:
- **RTMP**: Industry standard for live streaming ingestion, low latency
- **HLS**: Adaptive bitrate streaming, works on all devices, CDN-friendly
- **Latency**: 3-5 seconds (acceptable for social live streaming)

**Alternative considered**: WebRTC (sub-second latency but higher infrastructure cost)

### 4.4 Recommendation Algorithm

**Decision**: Hybrid approach (collaborative + content-based + trending)

**Rationale**:
- **Cold start problem**: Use trending videos for new users
- **Personalization**: Collaborative filtering based on similar users
- **Diversity**: Content-based filtering to avoid filter bubble
- **Real-time**: Update recommendations every 5 minutes

---

## 5. Scalability & Performance

### 5.1 Horizontal Scaling

**Video Service**:
- Stateless microservices behind load balancer
- Auto-scaling based on CPU (target: 70%)
- 100 instances during peak hours

**Database Sharding**:
```
Cassandra Cluster:
- 50 nodes (replication factor: 3)
- Partition by user_id (consistent hashing)
- 10TB per node

PostgreSQL:
- Master-slave replication (1 master, 5 read replicas)
- Connection pooling (HikariCP, max 100 connections)
```

### 5.2 Caching Strategy

**Multi-layer caching**:
```
L1: Application cache (Caffeine, 10K videos, 5 min TTL)
L2: Redis cluster (100M videos, 1 hour TTL)
L3: CDN (CloudFront, all videos, 24 hour TTL)
```

**Cache hit ratio**: 95% for video metadata, 99% for video files

### 5.3 Performance Optimization

**Video Upload**:
- Chunked upload (10MB chunks) for large files
- Parallel transcoding (4 resolutions simultaneously)
- Async processing (Kafka + worker pool)

**Feed Generation**:
- Pre-compute feeds for active users (Redis cache)
- Lazy loading (load 20 videos, fetch next batch on scroll)
- Pagination cursor (avoid offset-based pagination)

---

## 6. Fault Tolerance & Reliability

### 6.1 High Availability

**Multi-region deployment**:
```
Primary: us-east-1 (50% traffic)
Secondary: eu-west-1 (30% traffic)
Tertiary: ap-southeast-1 (20% traffic)

Failover: Route53 health checks + automatic DNS failover
```

### 6.2 Data Backup

**Video files**:
- S3 versioning enabled
- Cross-region replication (us-east-1 → us-west-2)
- Glacier archival after 90 days

**Database**:
- PostgreSQL: Daily full backup + WAL archiving
- Cassandra: Snapshot every 6 hours
- Point-in-time recovery (7 days retention)

### 6.3 Circuit Breaker

```java
@Service
public class VideoService {
    
    @CircuitBreaker(name = "videoProcessing", fallbackMethod = "fallbackProcessing")
    public void processVideo(Video video) {
        // Call video processing service
        videoProcessingClient.process(video);
    }
    
    public void fallbackProcessing(Video video, Exception e) {
        // Queue for retry
        kafkaTemplate.send("video-processing-retry", video);
        
        // Update status
        video.setStatus("PROCESSING_FAILED");
        videoRepository.save(video);
    }
}
```

---

## 7. Security

### 7.1 Authentication & Authorization

**JWT-based authentication**:
```
User login → Generate JWT (expires in 7 days)
              ↓
        Store in Redis (session management)
              ↓
        Client includes JWT in Authorization header
              ↓
        API Gateway validates JWT
```

### 7.2 Content Moderation

**Multi-layer moderation**:
1. **Upload time**: Hash-based duplicate detection
2. **Async processing**: AI-based content classification (NSFW, violence)
3. **User reports**: Manual review queue
4. **Automated takedown**: Remove videos with >90% confidence of violation

### 7.3 DDoS Protection

- **Rate limiting**: 100 requests/minute per user
- **CloudFlare**: DDoS mitigation at edge
- **WAF rules**: Block malicious patterns

---

## 8. Monitoring & Observability

### 8.1 Metrics

**Key metrics**:
- Video upload success rate: >99.5%
- Feed load time: <100ms (p95)
- Live stream latency: <3s
- CDN cache hit ratio: >95%
- Database query latency: <10ms (p99)

### 8.2 Logging

**Centralized logging** (ELK Stack):
```
Application logs → Filebeat → Logstash → Elasticsearch → Kibana
```

**Log levels**:
- ERROR: Failed video processing, database errors
- WARN: High latency, cache misses
- INFO: User actions (upload, like, comment)

### 8.3 Alerting

**PagerDuty alerts**:
- P1: Service down, database unavailable
- P2: High error rate (>1%), slow response time
- P3: Cache hit ratio drop, disk space warning

---

## 9. Cost Analysis

### 9.1 Infrastructure Costs (Monthly)

| Component | Specification | Cost |
|-----------|--------------|------|
| EC2 (Video Service) | 100 x m5.2xlarge | $24,000 |
| RDS PostgreSQL | db.r5.4xlarge (Multi-AZ) | $3,500 |
| Cassandra (EC2) | 50 x i3.2xlarge | $30,000 |
| Redis Cluster | 10 x cache.r5.2xlarge | $4,000 |
| S3 Storage | 500PB @ $0.023/GB | $11,500,000 |
| CloudFront | 100TB/day @ $0.085/GB | $255,000 |
| Kafka (MSK) | 20 brokers | $8,000 |
| **Total** | | **$11,824,500** |

### 9.2 Cost Optimization

- **S3 Intelligent-Tiering**: Save 30% on storage costs
- **Reserved Instances**: Save 40% on EC2 costs
- **CDN optimization**: Compress videos (save 20% bandwidth)

---

## 10. Future Enhancements

1. **AI-powered video editing**: Auto-generate highlights, captions
2. **AR effects**: Real-time face filters, 3D objects
3. **E-commerce integration**: In-video shopping links
4. **Multi-language support**: Auto-translate captions
5. **Live shopping**: Sell products during live streams
6. **Creator monetization**: Ad revenue sharing, virtual gifts

---

## 11. References

- [TikTok Engineering Blog](https://newsroom.tiktok.com/en-us/engineering)
- [ByteDance Tech Stack](https://www.bytedance.com/en/technology)
- [Live Streaming Platform Design](https://bugfree.ai/system-design/live-streaming-platform)
- [Video CDN Best Practices](https://aws.amazon.com/cloudfront/streaming/)
- [Recommendation Systems at Scale](https://netflixtechblog.com/netflix-recommendations-beyond-the-5-stars-part-1-55838468f429)
