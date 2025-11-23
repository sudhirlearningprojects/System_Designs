# TikTok Clone - Short Video & Live Streaming Platform

## Overview

A production-ready TikTok clone supporting short-form video sharing (15-60 seconds) and live streaming with real-time interactions. Built with Spring Boot, the platform handles billions of users with AI-powered personalized recommendations.

## Key Features

### 🎥 Short Video Platform
- **Video Upload**: Upload 15-60 second videos with captions and hashtags
- **For You Feed**: AI-powered personalized video recommendations
- **Following Feed**: Videos from users you follow
- **Video Interactions**: Like, comment, share, save
- **Duet & Stitch**: Collaborative video creation
- **Video Effects**: Filters, stickers, music overlay (future)

### 📡 Live Streaming
- **RTMP Ingestion**: Stream from OBS, Streamlabs, or mobile apps
- **HLS Delivery**: Adaptive bitrate streaming for all devices
- **Real-time Chat**: WebSocket-based live comments
- **Interactive Features**: Likes, gifts, viewer count
- **Multi-bitrate**: 360p, 480p, 720p, 1080p transcoding
- **Low Latency**: <3 second stream delay

### 🤖 Recommendation Engine
- **Hybrid Algorithm**: Collaborative + content-based + trending
- **Personalization**: Based on watch history, likes, shares
- **Diversity**: Avoid filter bubble with explore content
- **Freshness**: Prioritize recent uploads

### 🔒 Security & Moderation
- **JWT Authentication**: Secure API access
- **Content Moderation**: AI-based NSFW detection
- **Rate Limiting**: Prevent abuse and spam
- **DDoS Protection**: CloudFlare integration

## Architecture Highlights

### Microservices
- **Video Service**: Upload, transcode, metadata management
- **Live Service**: RTMP ingestion, HLS packaging, viewer tracking
- **Feed Service**: Recommendation algorithm, feed generation
- **Social Service**: Likes, comments, follows, shares
- **User Service**: Authentication, profiles, preferences

### Technology Stack
- **Backend**: Java 17, Spring Boot 3.2
- **Databases**: PostgreSQL (users), Cassandra (videos), Redis (cache)
- **Messaging**: Apache Kafka (event streaming)
- **Storage**: Amazon S3 (videos), CloudFront CDN
- **Live Streaming**: Nginx-RTMP, FFmpeg transcoding
- **Real-time**: WebSocket (live chat)

### Scalability
- **Horizontal Scaling**: Stateless microservices with load balancers
- **Database Sharding**: Cassandra partitioned by user_id
- **Multi-layer Caching**: Application cache + Redis + CDN
- **Async Processing**: Kafka-based video transcoding pipeline

## Scale Metrics

| Metric | Value |
|--------|-------|
| Monthly Active Users | 1.5 Billion |
| Daily Active Users | 500 Million |
| Videos Uploaded/Day | 1 Billion |
| Total Storage | 500 PB |
| Peak Bandwidth | 100 Gbps |
| Concurrent Live Streams | 10 Million |
| Feed Load Time | <100ms (p95) |
| Live Stream Latency | <3 seconds |
| Availability | 99.99% |

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 6+
- Apache Kafka 3.0+
- FFmpeg (for video processing)

### Installation

1. **Clone repository**
```bash
git clone https://github.com/sudhir512kj/system-designs.git
cd system-designs
```

2. **Start infrastructure**
```bash
docker-compose up -d postgres redis kafka
```

3. **Configure environment**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/tiktok
export DB_USERNAME=postgres
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export S3_BUCKET=tiktok-videos
export CDN_URL=https://cdn.tiktok.com
export JWT_SECRET=your_secret_key
```

4. **Build and run**
```bash
mvn clean install
./run-systems.sh tiktok  # Port 8095
```

5. **Verify**
```bash
curl http://localhost:8095/actuator/health
```

## API Examples

### Upload Video
```bash
curl -X POST http://localhost:8095/api/v1/videos/upload \
  -H "Authorization: Bearer <jwt_token>" \
  -F "file=@video.mp4" \
  -F "caption=Amazing dance moves! #fyp" \
  -F "isPublic=true"
```

### Get For You Feed
```bash
curl http://localhost:8095/api/v1/videos/feed/foryou?userId=123&page=0&size=20 \
  -H "Authorization: Bearer <jwt_token>"
```

### Create Live Stream
```bash
curl -X POST http://localhost:8095/api/v1/live/create?userId=123 \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Live Q&A Session",
    "description": "Ask me anything!"
  }'
```

### Start Streaming (OBS)
```
Server: rtmp://localhost:1935/live
Stream Key: <stream_key_from_create_response>
```

### Watch Live Stream
```html
<video id="player" controls>
  <source src="https://cdn.tiktok.com/live/<stream_key>/index.m3u8" type="application/x-mpegURL">
</video>

<script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
<script>
  const video = document.getElementById('player');
  const hls = new Hls();
  hls.loadSource('https://cdn.tiktok.com/live/<stream_key>/index.m3u8');
  hls.attachMedia(video);
</script>
```

## System Design Deep Dive

### Video Upload Pipeline
```
1. Client uploads video → API Gateway
2. Store raw video in S3
3. Publish event to Kafka (video-processing topic)
4. Worker picks up event
5. Transcode to multiple resolutions (360p, 480p, 720p, 1080p)
6. Generate thumbnail (extract frame at 2s)
7. Upload processed files to S3
8. Update video metadata in Cassandra
9. Invalidate cache
10. Notify client (webhook or polling)
```

### Live Streaming Pipeline
```
1. Broadcaster sends RTMP stream → Nginx-RTMP server
2. Nginx authenticates stream key
3. FFmpeg transcodes to multiple bitrates
4. Package as HLS (3-second segments)
5. Upload segments to S3
6. CloudFront serves HLS playlist
7. Viewers play via HLS.js
8. WebSocket for real-time chat/likes
```

### Recommendation Algorithm
```
1. Fetch user profile (watch history, likes, demographics)
2. Generate candidate videos:
   - 20% trending videos (high engagement rate)
   - 30% following feed (users you follow)
   - 30% similar content (collaborative filtering)
   - 20% explore (new creators, diverse content)
3. Rank candidates using ML model:
   - Engagement prediction (40%)
   - Content similarity (30%)
   - Freshness (20%)
   - Creator popularity (10%)
4. Apply diversity filter (avoid repetitive content)
5. Return top 20 videos
6. Cache in Redis (5 min TTL)
```

## Performance Optimization

### Caching Strategy
```
L1: Application Cache (Caffeine)
    - 10K most popular videos
    - 5 min TTL
    - 95% hit ratio

L2: Redis Cluster
    - 100M video metadata
    - 1 hour TTL
    - 90% hit ratio

L3: CloudFront CDN
    - All video files
    - 24 hour TTL
    - 99% hit ratio
```

### Database Optimization
```
Cassandra:
- Partition key: user_id (efficient user video queries)
- Clustering key: created_at DESC (chronological order)
- Replication factor: 3
- Consistency level: QUORUM

PostgreSQL:
- Master-slave replication (1 master, 5 read replicas)
- Connection pooling (HikariCP, max 100 connections)
- Read queries → replicas, Write queries → master
```

## Monitoring

### Key Metrics
- **Video Upload Success Rate**: >99.5%
- **Feed Load Time**: <100ms (p95)
- **Live Stream Latency**: <3s
- **CDN Cache Hit Ratio**: >95%
- **Database Query Latency**: <10ms (p99)
- **Kafka Lag**: <1000 messages

### Alerting
- **P1**: Service down, database unavailable
- **P2**: High error rate (>1%), slow response time
- **P3**: Cache hit ratio drop, disk space warning

## Cost Analysis

### Monthly Infrastructure Cost
| Component | Cost |
|-----------|------|
| EC2 (100 instances) | $24,000 |
| RDS PostgreSQL | $3,500 |
| Cassandra (50 nodes) | $30,000 |
| Redis Cluster | $4,000 |
| S3 Storage (500PB) | $11,500,000 |
| CloudFront (100TB/day) | $255,000 |
| Kafka (20 brokers) | $8,000 |
| **Total** | **$11,824,500** |

### Cost Optimization
- S3 Intelligent-Tiering: Save 30%
- Reserved Instances: Save 40%
- Video compression: Save 20% bandwidth

## Documentation

- [System Design](System_Design.md) - Complete HLD/LLD
- [API Documentation](API_Documentation.md) - REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis

## Future Enhancements

1. **AI Video Editing**: Auto-generate highlights, captions
2. **AR Effects**: Real-time face filters, 3D objects
3. **E-commerce**: In-video shopping links
4. **Multi-language**: Auto-translate captions
5. **Live Shopping**: Sell products during streams
6. **Creator Monetization**: Ad revenue, virtual gifts

## Contributing

Contributions welcome! Please read [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

## License

MIT License - see [LICENSE](../../LICENSE) for details.

---

**Built with ❤️ by Sudhir Meena**

For questions, open an issue or visit [sudhirmeenaswe.netlify.app](https://sudhirmeenaswe.netlify.app/)
