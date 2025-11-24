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
- **Bandwidth**: 529 Gbps peak bandwidth
- **Live Streams**: 1M+ concurrent viewers, 800K concurrent streamers
- **Latency**: <100ms video feed load, <5s live stream latency
- **Availability**: 99.9% uptime
- **Chat**: 75M messages/day during live streams

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

#### Live Streaming Pipeline (Enhanced)
```
Broadcaster (OBS/RTMP) → API Gateway (Auth) → Media Server Cluster
                                                        ↓
                                    ┌───────────────────┴───────────────────┐
                                    ▼                                       ▼
                            Ingestion Service                      Recording Service
                            (RTMP/WebRTC)                         (S3 Storage)
                                    ↓
                            Transcoding Service
                            (FFmpeg Workers)
                            - 360p (500 Kbps)
                            - 480p (1 Mbps)
                            - 720p (2 Mbps)
                            - 1080p (4 Mbps)
                                    ↓
                            Segmenting Service
                            (HLS/DASH)
                            - 3s segments
                            - Adaptive bitrate
                                    ↓
                            CDN Push (Origin)
                                    ↓
                            Edge Locations (200+)
                                    ↓
                            Viewers (HLS Player)
                            
Parallel: Chat Service (WebSocket) ←→ Viewers
          Analytics Service ←→ Metrics Collection
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

### 3.3 Live Streaming Deep Dive

#### 3.3.1 Media Server Architecture

**Ingestion Layer**
```java
@Service
public class MediaIngestionService {
    
    // RTMP stream authentication
    public boolean authenticateStream(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow(() -> new UnauthorizedException("Invalid stream key"));
        
        if (stream.getStatus() != StreamStatus.SCHEDULED) {
            throw new IllegalStateException("Stream not in SCHEDULED state");
        }
        
        // Validate user subscription tier
        User user = userRepository.findById(stream.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
        
        if (!user.canStartLiveStream()) {
            throw new ForbiddenException("User not authorized for live streaming");
        }
        
        return true;
    }
    
    // Handle stream start callback from Nginx-RTMP
    @Transactional
    public void onStreamStart(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow();
        
        stream.setStatus(StreamStatus.LIVE);
        stream.setStartedAt(LocalDateTime.now());
        liveStreamRepository.save(stream);
        
        // Add to active streams
        redisTemplate.opsForSet().add("live:active", stream.getStreamId());
        
        // Notify followers
        notificationService.notifyFollowers(stream.getUserId(), 
            "started a live stream: " + stream.getTitle());
        
        // Start analytics collection
        analyticsService.startStreamMetrics(stream.getStreamId());
    }
}
```

**Transcoding Service**
```java
@Service
public class TranscodingService {
    private final ExecutorService transcodingPool = 
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    public void transcodeStream(String streamKey, String inputRtmpUrl) {
        // Transcode to multiple bitrates using FFmpeg
        List<TranscodingTask> tasks = Arrays.asList(
            new TranscodingTask("360p", 640, 360, 500),
            new TranscodingTask("480p", 854, 480, 1000),
            new TranscodingTask("720p", 1280, 720, 2000),
            new TranscodingTask("1080p", 1920, 1080, 4000)
        );
        
        tasks.forEach(task -> transcodingPool.submit(() -> {
            String command = String.format(
                "ffmpeg -i %s -vf scale=%d:%d -b:v %dk -c:v libx264 " +
                "-preset veryfast -g 60 -sc_threshold 0 " +
                "-c:a aac -b:a 128k -f flv rtmp://localhost/hls/%s_%s",
                inputRtmpUrl, task.width, task.height, task.bitrate,
                streamKey, task.quality
            );
            
            executeFFmpeg(command);
        }));
    }
    
    @Data
    @AllArgsConstructor
    static class TranscodingTask {
        String quality;
        int width;
        int height;
        int bitrate; // Kbps
    }
}
```

**HLS Segmenting Service**
```java
@Service
public class HLSSegmentingService {
    
    public void generateHLSPlaylist(String streamKey, List<String> qualities) {
        // Generate master playlist
        StringBuilder masterPlaylist = new StringBuilder();
        masterPlaylist.append("#EXTM3U\n");
        masterPlaylist.append("#EXT-X-VERSION:3\n");
        
        for (String quality : qualities) {
            int bandwidth = getBandwidth(quality);
            String resolution = getResolution(quality);
            
            masterPlaylist.append(String.format(
                "#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s\n",
                bandwidth, resolution
            ));
            masterPlaylist.append(String.format("%s_%s/index.m3u8\n", 
                streamKey, quality));
        }
        
        // Upload to S3/CDN origin
        String playlistUrl = uploadToOrigin(streamKey, masterPlaylist.toString());
        
        // Update stream HLS URL
        updateStreamHlsUrl(streamKey, playlistUrl);
    }
    
    private int getBandwidth(String quality) {
        return switch (quality) {
            case "360p" -> 500_000;
            case "480p" -> 1_000_000;
            case "720p" -> 2_000_000;
            case "1080p" -> 4_000_000;
            default -> 1_000_000;
        };
    }
}
```

#### 3.3.2 Real-Time Chat Service

**WebSocket Chat Handler**
```java
@Component
public class LiveChatWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, Set<WebSocketSession>> streamSessions = new ConcurrentHashMap<>();
    private final RateLimiter chatRateLimiter = RateLimiter.create(10.0); // 10 msg/sec
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long streamId = extractStreamId(session);
        Long userId = extractUserId(session);
        
        // Add to stream session pool
        streamSessions.computeIfAbsent(streamId, k -> ConcurrentHashMap.newKeySet())
            .add(session);
        
        // Send recent chat history
        List<ChatMessage> history = chatService.getRecentMessages(streamId, 50);
        sendToSession(session, new ChatHistoryMessage(history));
        
        // Broadcast join notification
        broadcastToStream(streamId, new UserJoinedMessage(userId));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long streamId = extractStreamId(session);
        Long userId = extractUserId(session);
        
        // Rate limiting
        if (!chatRateLimiter.tryAcquire()) {
            sendToSession(session, new ErrorMessage("Rate limit exceeded"));
            return;
        }
        
        ChatMessageDTO dto = parseMessage(message.getPayload());
        
        // Content moderation
        if (moderationService.containsInappropriateContent(dto.getContent())) {
            sendToSession(session, new ErrorMessage("Message blocked by moderation"));
            return;
        }
        
        // Save to database
        ChatMessage chatMessage = chatService.saveMessage(streamId, userId, dto);
        
        // Broadcast to all viewers
        broadcastToStream(streamId, chatMessage);
        
        // Update analytics
        analyticsService.incrementChatCount(streamId);
    }
    
    private void broadcastToStream(Long streamId, Object message) {
        Set<WebSocketSession> sessions = streamSessions.get(streamId);
        if (sessions == null) return;
        
        String json = objectMapper.writeValueAsString(message);
        sessions.parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("Failed to send message", e);
                }
            });
    }
}
```

**Chat Message Queue (Kafka)**
```java
@Service
public class ChatMessageProcessor {
    
    @KafkaListener(topics = "live-chat-messages", concurrency = "10")
    public void processMessage(ChatMessageEvent event) {
        // Persist to Cassandra
        chatRepository.save(event.toChatMessage());
        
        // Update Redis cache (recent 100 messages)
        String cacheKey = "chat:recent:" + event.getStreamId();
        redisTemplate.opsForList().leftPush(cacheKey, event);
        redisTemplate.opsForList().trim(cacheKey, 0, 99);
        redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
        
        // Check for spam patterns
        if (spamDetectionService.isSpam(event)) {
            moderationService.flagUser(event.getUserId(), "Spam detected");
        }
    }
}
```

#### 3.3.3 Content Moderation

**Multi-Layer Moderation**
```java
@Service
public class ContentModerationService {
    private final Set<String> bannedWords = loadBannedWords();
    private final Pattern urlPattern = Pattern.compile("https?://\\S+");
    
    public boolean containsInappropriateContent(String content) {
        // Layer 1: Banned words filter
        String lowerContent = content.toLowerCase();
        for (String word : bannedWords) {
            if (lowerContent.contains(word)) {
                return true;
            }
        }
        
        // Layer 2: URL spam detection
        Matcher matcher = urlPattern.matcher(content);
        if (matcher.find()) {
            return true; // Block URLs in chat
        }
        
        // Layer 3: Repetitive character detection
        if (hasRepetitiveCharacters(content, 5)) {
            return true;
        }
        
        // Layer 4: ML-based toxicity detection (async)
        CompletableFuture.runAsync(() -> {
            double toxicityScore = mlModerationService.predictToxicity(content);
            if (toxicityScore > 0.8) {
                moderationQueue.add(new ModerationTask(content, toxicityScore));
            }
        });
        
        return false;
    }
    
    private boolean hasRepetitiveCharacters(String text, int threshold) {
        if (text.length() < threshold) return false;
        
        for (int i = 0; i <= text.length() - threshold; i++) {
            char c = text.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < threshold; j++) {
                if (text.charAt(i + j) != c) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) return true;
        }
        return false;
    }
}
```

#### 3.3.4 Analytics Service

**Real-Time Metrics Collection**
```java
@Service
public class LiveStreamAnalyticsService {
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void collectMetrics() {
        Set<Object> activeStreams = redisTemplate.opsForSet().members("live:active");
        
        activeStreams.forEach(streamIdObj -> {
            Long streamId = (Long) streamIdObj;
            
            // Collect metrics
            StreamMetrics metrics = new StreamMetrics();
            metrics.setStreamId(streamId);
            metrics.setTimestamp(LocalDateTime.now());
            metrics.setViewerCount(getViewerCount(streamId));
            metrics.setChatMessageRate(getChatMessageRate(streamId));
            metrics.setLikeRate(getLikeRate(streamId));
            metrics.setGiftCount(getGiftCount(streamId));
            
            // Save to time-series database
            metricsRepository.save(metrics);
            
            // Update stream peak viewer count
            updatePeakViewerCount(streamId, metrics.getViewerCount());
        });
    }
    
    public StreamAnalytics getStreamAnalytics(Long streamId) {
        List<StreamMetrics> metrics = metricsRepository
            .findByStreamIdOrderByTimestampAsc(streamId);
        
        return StreamAnalytics.builder()
            .totalViewers(calculateTotalViewers(metrics))
            .peakViewers(metrics.stream()
                .mapToLong(StreamMetrics::getViewerCount)
                .max().orElse(0))
            .averageViewers(metrics.stream()
                .mapToLong(StreamMetrics::getViewerCount)
                .average().orElse(0))
            .totalChatMessages(metrics.stream()
                .mapToLong(StreamMetrics::getChatMessageRate)
                .sum())
            .totalLikes(metrics.stream()
                .mapToLong(StreamMetrics::getLikeRate)
                .sum())
            .totalGifts(metrics.stream()
                .mapToLong(StreamMetrics::getGiftCount)
                .sum())
            .streamDuration(calculateDuration(metrics))
            .build();
    }
}
```

#### 3.3.5 Nginx-RTMP Configuration

```nginx
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        max_message 10M;
        
        application live {
            live on;
            record off;
            
            # Authentication
            on_publish http://api.tiktok.com/api/v1/live/auth;
            on_publish_done http://api.tiktok.com/api/v1/live/end;
            
            # HLS configuration
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;
            hls_continuous on;
            hls_cleanup on;
            hls_nested on;
            
            # DASH configuration
            dash on;
            dash_path /tmp/dash;
            dash_fragment 3s;
            dash_playlist_length 60s;
            
            # Recording (optional)
            record all;
            record_path /tmp/recordings;
            record_suffix -%Y%m%d-%H%M%S.flv;
            record_max_size 1024M;
            
            # Transcoding to multiple bitrates
            exec ffmpeg -i rtmp://localhost/$app/$name
                # 1080p
                -c:v libx264 -b:v 4000k -s 1920x1080 -preset veryfast 
                -profile:v high -level 4.1 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 128k -ar 44100 
                -f flv rtmp://localhost/hls/$name_1080p
                
                # 720p
                -c:v libx264 -b:v 2000k -s 1280x720 -preset veryfast 
                -profile:v high -level 4.0 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 128k -ar 44100 
                -f flv rtmp://localhost/hls/$name_720p
                
                # 480p
                -c:v libx264 -b:v 1000k -s 854x480 -preset veryfast 
                -profile:v main -level 3.1 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 96k -ar 44100 
                -f flv rtmp://localhost/hls/$name_480p
                
                # 360p
                -c:v libx264 -b:v 500k -s 640x360 -preset veryfast 
                -profile:v main -level 3.0 -g 60 -keyint_min 60 
                -sc_threshold 0 -c:a aac -b:a 64k -ar 44100 
                -f flv rtmp://localhost/hls/$name_360p;
        }
        
        application hls {
            live on;
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3s;
            hls_playlist_length 60s;
            hls_nested on;
            
            # Variant playlist
            hls_variant _1080p BANDWIDTH=4000000,RESOLUTION=1920x1080;
            hls_variant _720p BANDWIDTH=2000000,RESOLUTION=1280x720;
            hls_variant _480p BANDWIDTH=1000000,RESOLUTION=854x480;
            hls_variant _360p BANDWIDTH=500000,RESOLUTION=640x360;
        }
    }
}

http {
    server {
        listen 8080;
        
        # HLS streaming endpoint
        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            root /tmp;
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
        
        # DASH streaming endpoint
        location /dash {
            types {
                application/dash+xml mpd;
                video/mp4 mp4;
            }
            root /tmp;
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
    }
}
```

### 3.4 Video Processing Workflow

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

## 4. Trade-Off Analysis

### 4.1 RTMP vs WebRTC for Ingestion

**RTMP (Chosen)**
- ✅ Well-supported by OBS, Streamlabs, and other broadcasting tools
- ✅ Predictable ingestion workflow with mature ecosystem
- ✅ Lower server resource consumption
- ✅ Better for one-to-many broadcasting
- ❌ Higher latency (2-5 seconds)
- ❌ Requires Flash player (deprecated, but RTMP protocol still used)

**WebRTC (Alternative)**
- ✅ Ultra-low latency (<1 second)
- ✅ Native browser support without plugins
- ✅ Peer-to-peer capability
- ❌ Complex SFU/MCU setup for scaling
- ❌ Higher server resource consumption
- ❌ Limited support in broadcasting tools
- ❌ Not suitable for large-scale one-to-many

**Decision**: Use RTMP for ingestion, HLS/DASH for delivery. Consider WebRTC for ultra-low latency use cases (e.g., live auctions, gaming tournaments).

### 4.2 CDN vs Custom Edge Network

**Commercial CDN (Chosen)**
- ✅ Global distribution out-of-the-box (200+ edge locations)
- ✅ DDoS protection and security features
- ✅ TLS termination and certificate management
- ✅ Pay-as-you-go pricing model
- ❌ Higher cost at scale ($0.085/GB)
- ❌ Less control over caching logic

**Custom Edge Network (Alternative)**
- ✅ Tighter control over caching and routing
- ✅ Lower cost at massive scale
- ✅ Custom optimization for video streaming
- ❌ Heavy infrastructure investment
- ❌ Complex monitoring and maintenance
- ❌ Requires global data center presence

**Decision**: Use commercial CDN (CloudFront/Akamai) with fallback to origin. Explore hybrid approach once scale justifies investment.

### 4.3 WebSockets vs Polling for Chat

**WebSockets (Chosen)**
- ✅ Low-latency bi-directional communication
- ✅ Efficient for real-time updates
- ✅ Single persistent connection
- ❌ Scaling challenges with millions of connections
- ❌ Requires sticky sessions or pub/sub

**Polling (Alternative)**
- ✅ Simpler implementation
- ✅ Works with any HTTP infrastructure
- ❌ Higher latency (1-5 seconds)
- ❌ Unnecessary bandwidth consumption
- ❌ Increased server load

**Decision**: Use WebSockets with Redis pub/sub for horizontal scaling. Implement sticky sessions at load balancer level.

### 4.4 SQL vs NoSQL for Chat Messages

**Cassandra (Chosen)**
- ✅ High write throughput (75M messages/day)
- ✅ Horizontal scalability
- ✅ Time-series data model fits chat perfectly
- ❌ Eventual consistency
- ❌ Limited query flexibility

**PostgreSQL (Alternative)**
- ✅ ACID guarantees
- ✅ Complex queries and joins
- ❌ Write bottleneck at scale
- ❌ Vertical scaling limits

**Decision**: Use Cassandra for chat messages, PostgreSQL for user/stream metadata, Redis for ephemeral data (live viewer counts).

### 4.5 Horizontal vs Vertical Scaling

**Horizontal Scaling (Chosen)**
- ✅ Load distribution across multiple servers
- ✅ Failure isolation (one node failure doesn't affect others)
- ✅ Cost-effective with commodity hardware
- ❌ Requires stateless services
- ❌ Complex coordination (distributed sessions)

**Vertical Scaling (Alternative)**
- ✅ Simpler architecture
- ✅ No distributed state management
- ❌ Hardware limits (max CPU/RAM)
- ❌ Single point of failure
- ❌ Expensive high-end servers

**Decision**: Design for horizontal scaling with auto-scaling groups. Use stateless microservices with external session storage (Redis).

### 4.6 Automated vs Manual Moderation

**Hybrid Approach (Chosen)**
- ✅ AI filters provide first-pass moderation (99% coverage)
- ✅ Human moderators handle edge cases
- ✅ User flagging system for community moderation
- ✅ Continuous ML model improvement

**Implementation**:
1. Real-time: Regex filters, banned word lists (block immediately)
2. Async: ML toxicity detection (flag for review)
3. Manual: Human moderators review flagged content
4. Feedback loop: Moderator decisions retrain ML model

## 5. Key Design Decisions

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

## 6. Failure Modes & Mitigations

### 6.1 Media Server Overload

**Symptoms**:
- Dropped frames during live streams
- Stream disconnects and buffering
- Increased transcoding latency

**Root Cause**:
- Spike in concurrent ingest streams without enough transcode resources
- CPU/memory exhaustion on media servers
- Network bandwidth saturation

**Mitigations**:
```java
@Service
public class MediaServerLoadBalancer {
    
    @Scheduled(fixedRate = 10000)
    public void monitorServerLoad() {
        List<MediaServer> servers = mediaServerRepository.findAll();
        
        for (MediaServer server : servers) {
            ServerMetrics metrics = metricsService.getMetrics(server.getId());
            
            // Check CPU usage
            if (metrics.getCpuUsage() > 80) {
                // Scale up: Launch new media server
                autoScalingService.scaleUp("media-server-group");
                
                // Throttle new stream ingestion
                rateLimiter.setRate(server.getId(), 0.5); // 50% capacity
                
                alertService.alert("Media server " + server.getId() + 
                    " CPU usage: " + metrics.getCpuUsage() + "%");
            }
            
            // Check active stream count
            if (metrics.getActiveStreams() > server.getMaxStreams()) {
                // Reject new streams
                server.setAcceptingStreams(false);
            }
        }
    }
    
    // Prioritize streamers based on account tier
    public MediaServer assignServer(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        List<MediaServer> availableServers = mediaServerRepository
            .findByAcceptingStreamsTrue();
        
        if (availableServers.isEmpty()) {
            if (user.isPremium()) {
                // Premium users get priority - force scale up
                return autoScalingService.launchNewServer();
            } else {
                throw new ServiceUnavailableException(
                    "No media servers available. Please try again later.");
            }
        }
        
        // Load balance based on current load
        return availableServers.stream()
            .min(Comparator.comparingInt(MediaServer::getActiveStreams))
            .orElseThrow();
    }
}
```

### 6.2 CDN Outage or Degradation

**Symptoms**:
- Users experience buffering or complete video failure
- High latency in video playback
- Increased origin server load

**Mitigations**:
```java
@Service
public class MultiCDNFailoverService {
    private final List<CDNProvider> cdnProviders = Arrays.asList(
        new CloudFrontProvider(),
        new AkamaiProvider(),
        new CloudflareProvider()
    );
    
    public String getVideoUrl(String videoId) {
        // Try primary CDN
        CDNProvider primary = cdnProviders.get(0);
        if (primary.isHealthy()) {
            return primary.getVideoUrl(videoId);
        }
        
        // Failover to secondary CDN
        log.warn("Primary CDN unhealthy, failing over to secondary");
        CDNProvider secondary = cdnProviders.get(1);
        if (secondary.isHealthy()) {
            return secondary.getVideoUrl(videoId);
        }
        
        // Last resort: Origin server (with rate limiting)
        log.error("All CDNs unhealthy, serving from origin");
        return originServer.getVideoUrl(videoId);
    }
    
    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        cdnProviders.forEach(cdn -> {
            boolean healthy = cdn.performHealthCheck();
            if (!healthy) {
                alertService.alert("CDN " + cdn.getName() + " is unhealthy");
                
                // Update DNS to route traffic away
                route53Service.updateHealthCheck(cdn.getName(), false);
            }
        });
    }
}
```

### 6.3 Chat Service Bottlenecks

**Symptoms**:
- Delayed messages (>1 second latency)
- Chat server crashes during popular events
- WebSocket connection failures

**Mitigations**:
```java
@Service
public class ChatServiceScaling {
    
    // Shard chat rooms across multiple servers
    public String getChatServerUrl(Long streamId) {
        int shard = (int) (streamId % chatServerCount);
        return "ws://chat-" + shard + ".tiktok.com/ws";
    }
    
    // Rate limit per user
    @RateLimit(requests = 10, window = 60, scope = RateLimit.Scope.USER)
    public void sendMessage(Long userId, Long streamId, String message) {
        // Send to Kafka for async processing
        kafkaTemplate.send("chat-messages", new ChatMessageEvent(
            streamId, userId, message, LocalDateTime.now()
        ));
    }
    
    // Pre-warm nodes for anticipated large events
    public void prewarmForEvent(Long streamId, int expectedViewers) {
        int requiredServers = (int) Math.ceil(expectedViewers / 10000.0);
        
        for (int i = 0; i < requiredServers; i++) {
            autoScalingService.launchChatServer(streamId);
        }
        
        log.info("Pre-warmed {} chat servers for stream {}", 
            requiredServers, streamId);
    }
}
```

### 6.4 Database Hotspots

**Symptoms**:
- High latency during heavy write operations
- Database connection pool exhaustion
- Slow queries during chat floods

**Mitigations**:
```java
@Service
public class DatabaseOptimizationService {
    
    // Partition chat messages by stream_id and time
    @Entity
    @Table(name = "chat_messages", 
           partitionKeys = {@PartitionKey("stream_id"), @PartitionKey("date")},
           clusteringKeys = {@ClusteringKey("timestamp")})
    public class ChatMessage {
        private Long streamId;
        private LocalDate date;
        private LocalDateTime timestamp;
        private Long userId;
        private String message;
    }
    
    // Use read replicas for high-read endpoints
    @Transactional(readOnly = true)
    public List<Video> getForYouFeed(Long userId) {
        // Route to read replica
        return videoRepository.findForYouFeed(userId);
    }
    
    // Cache hot data in Redis
    @Cacheable(value = "live:viewers", key = "#streamId")
    public Long getViewerCount(Long streamId) {
        return redisTemplate.opsForSet().size("live:viewers:" + streamId);
    }
    
    // Batch writes to reduce database load
    @Scheduled(fixedRate = 5000)
    public void flushViewCountUpdates() {
        Map<Long, Long> updates = viewCountBuffer.getAndClear();
        
        jdbcTemplate.batchUpdate(
            "UPDATE live_streams SET viewer_count = ? WHERE stream_id = ?",
            updates.entrySet().stream()
                .map(e -> new Object[]{e.getValue(), e.getKey()})
                .collect(Collectors.toList())
        );
    }
}
```

### 6.5 Single Point of Failure (SPOF)

**Examples**:
- Single API gateway going down
- Single database instance failure
- Single Redis instance failure

**Mitigations**:
```yaml
# Multi-AZ deployment with redundancy
apiGateway:
  instances: 3
  loadBalancer: 
    type: Application Load Balancer
    healthCheck: /health
    crossZone: true

database:
  postgres:
    primary: us-east-1a
    replicas:
      - us-east-1b
      - us-east-1c
    autoFailover: true
    
  cassandra:
    replicationFactor: 3
    consistencyLevel: QUORUM
    
redis:
  cluster:
    nodes: 6
    replicas: 2
    sentinels: 3
    autoFailover: true
```

### 6.6 Content Moderation Failures

**Symptoms**:
- Inappropriate content slipping through filters
- False positives causing user dissatisfaction
- Delayed moderation response

**Mitigations**:
```java
@Service
public class ModerationService {
    
    // Multi-layer moderation pipeline
    public ModerationResult moderateContent(String content) {
        // Layer 1: Regex filters (instant)
        if (regexFilter.matches(content)) {
            return ModerationResult.blocked("Banned word detected");
        }
        
        // Layer 2: ML toxicity detection (100ms)
        double toxicityScore = mlModel.predict(content);
        if (toxicityScore > 0.9) {
            return ModerationResult.blocked("High toxicity score");
        } else if (toxicityScore > 0.7) {
            // Flag for manual review
            moderationQueue.add(new ModerationTask(content, toxicityScore));
            return ModerationResult.flagged("Flagged for review");
        }
        
        // Layer 3: User reports
        int reportCount = reportService.getReportCount(content);
        if (reportCount > 10) {
            return ModerationResult.blocked("Multiple user reports");
        }
        
        return ModerationResult.approved();
    }
    
    // Allow user appeals
    public void appealModeration(Long userId, Long contentId, String reason) {
        Appeal appeal = new Appeal(userId, contentId, reason);
        appealQueue.add(appeal);
        
        // Notify moderators
        notificationService.notifyModerators(appeal);
    }
    
    // Continuous model retraining
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void retrainModel() {
        List<ModerationDecision> decisions = moderationRepository
            .findRecentDecisions(LocalDateTime.now().minusDays(7));
        
        mlModelService.retrain(decisions);
        
        log.info("Retrained moderation model with {} decisions", 
            decisions.size());
    }
}
```

### 6.7 Security Breaches

**Attack Vectors**:
- JWT token hijacking
- RTMP stream key leaks
- Unauthorized database access
- DDoS attacks

**Mitigations**:
```java
@Service
public class SecurityService {
    
    // Rotate JWT secrets regularly
    @Scheduled(cron = "0 0 0 1 * ?") // Monthly
    public void rotateJwtSecret() {
        String newSecret = generateSecureSecret();
        jwtConfig.setSecret(newSecret);
        
        // Invalidate all existing tokens
        redisTemplate.delete("user:session:*");
        
        log.info("Rotated JWT secret");
    }
    
    // Rotate stream keys after each stream
    @Transactional
    public void endStream(String streamKey) {
        LiveStream stream = liveStreamRepository.findByStreamKey(streamKey)
            .orElseThrow();
        
        stream.setStatus(StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        
        // Generate new stream key for next stream
        stream.setStreamKey(UUID.randomUUID().toString());
        
        liveStreamRepository.save(stream);
    }
    
    // Encrypt sensitive data at rest
    @PrePersist
    public void encryptSensitiveData(User user) {
        if (user.getEmail() != null) {
            user.setEmail(encryptionService.encrypt(user.getEmail()));
        }
    }
    
    // TLS everywhere
    @Configuration
    public class TLSConfig {
        @Bean
        public TomcatServletWebServerFactory servletContainer() {
            TomcatServletWebServerFactory tomcat = 
                new TomcatServletWebServerFactory();
            tomcat.addAdditionalTomcatConnectors(createHttpsConnector());
            return tomcat;
        }
    }
    
    // WAF rules for DDoS protection
    @Component
    public class DDoSProtectionFilter implements Filter {
        private final RateLimiter globalRateLimiter = 
            RateLimiter.create(10000.0); // 10K req/sec
        
        @Override
        public void doFilter(ServletRequest request, 
                           ServletResponse response, 
                           FilterChain chain) {
            if (!globalRateLimiter.tryAcquire()) {
                ((HttpServletResponse) response)
                    .setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
            
            chain.doFilter(request, response);
        }
    }
}
```

## 7. Fault Tolerance & Reliability

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

## 9. Scale Calculations

### 9.1 Traffic & Storage Estimates

#### Bandwidth (Peak)

**Live Streaming Viewers**:
- 1M concurrent viewers
- Distribution:
  - 560K viewers @ 2 Mbps (720p) = 1,120 Gbps
  - 240K viewers @ 4 Mbps (1080p) = 960 Gbps
  - 120K viewers @ 1 Mbps (480p) = 120 Gbps
  - 80K viewers @ 500 Kbps (360p) = 40 Gbps
- **Total Live Bandwidth**: ~2,240 Gbps

**Short Video Playback**:
- 100M DAU, 50% watching videos during peak hour
- 50M concurrent viewers
- Average bitrate: 1.5 Mbps
- **Total Video Bandwidth**: 75,000 Gbps

**Total Peak Bandwidth**: ~77,240 Gbps (77.24 Tbps)

#### Storage

**Daily Stream Storage**:
- 800K concurrent streamers
- Average stream duration: 30 minutes
- Average bitrate: 2 Mbps (after transcoding)
- Storage per stream: 2 Mbps × 1800s = 3,600 Mb = 450 MB
- Daily streams: 800K × 450 MB = 360 TB/day
- With 4 quality levels: 360 TB × 4 = 1,440 TB/day
- **Monthly retention (30 days)**: 1,440 TB × 30 = 43,200 TB = 43.2 PB

**Short Videos**:
- 1B videos/day × 30 seconds × 1.5 Mbps = 5.625 TB/day
- With 4 quality levels: 5.625 TB × 4 = 22.5 TB/day
- **Total video storage**: 500 PB (accumulated over years)

**Chat Messages**:
- 75M messages/day during live streams
- Average message size: 200 bytes
- Daily storage: 75M × 200 bytes = 15 GB/day
- **Monthly storage**: 15 GB × 30 = 450 GB
- With indexes and metadata: ~1.1 TB/month

**Metadata**:
- Users: 1.5B × 2 KB = 3 TB
- Videos: 500B videos × 5 KB = 2.5 PB
- Streams: 24M streams/month × 2 KB = 48 GB/month
- **Total Metadata**: ~2.5 PB

**Total Storage**: 500 PB (videos) + 43.2 PB (streams) + 2.5 PB (metadata) = **545.7 PB**

### 9.2 Database Capacity Planning

**PostgreSQL (User & Stream Metadata)**:
- Users: 1.5B rows × 2 KB = 3 TB
- Live streams: 24M rows/month × 2 KB = 48 GB/month
- With indexes (3x): 9 TB + 144 GB/month
- **Total**: ~10 TB

**Cassandra (Videos, Comments, Likes)**:
- Videos: 500B rows × 5 KB = 2.5 PB
- Comments: 100B rows × 500 bytes = 50 TB
- Likes: 1T rows × 100 bytes = 100 TB
- With replication factor 3: (2.5 PB + 150 TB) × 3 = **7.95 PB**

**Redis (Cache & Real-time Data)**:
- User sessions: 100M × 1 KB = 100 GB
- Video metadata cache: 10M × 5 KB = 50 GB
- Live viewer sets: 1M streams × 1K viewers × 8 bytes = 8 GB
- Feed cache: 50M × 20 videos × 5 KB = 5 TB
- **Total**: ~5.2 TB

### 9.3 QPS Estimates

**Read Operations**:
- Video feed requests: 100M DAU × 100 requests/day / 86400s = 115K QPS
- Video playback: 50M concurrent × 1 request/10s = 5M QPS
- Live stream viewers: 1M concurrent × 1 request/5s = 200K QPS
- **Total Read QPS**: ~5.3M QPS

**Write Operations**:
- Video uploads: 1B/day / 86400s = 11.5K QPS
- Likes: 100M DAU × 50 likes/day / 86400s = 57.8K QPS
- Comments: 100M DAU × 10 comments/day / 86400s = 11.5K QPS
- Chat messages: 75M/day / 86400s = 868 QPS
- **Total Write QPS**: ~81.7K QPS

## 10. Cost Analysis

### 10.1 Infrastructure Costs (Monthly)

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
