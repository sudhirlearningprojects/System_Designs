# Multi-Database Setup Guide

## Overview
Instagram uses multiple databases optimized for different use cases. This guide explains the setup.

## Database Architecture

### 1. PostgreSQL (Primary Database)
**Use**: Users, Posts, Comments, Likes, Follows
- Already configured in `application-instagram.yml`
- ACID compliance for critical data
- Complex joins and relationships

### 2. Redis (Cache & Real-time)
**Use**: Caching, Sessions, Counters, Real-time data
- Already configured in `RedisConfig.java`
- Sub-millisecond latency
- Reduces DB load by 80%

### 3. Cassandra (Timeline Storage)
**Use**: User timelines, Activity feeds
- **Status**: Configured but requires dependency
- Optimized for time-series data
- Horizontal scalability

**Setup**:
```xml
<!-- Add to pom.xml if needed -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-cassandra</artifactId>
</dependency>
```

**Docker**:
```bash
docker run -d --name cassandra -p 9042:9042 cassandra:latest
```

### 4. Elasticsearch (Search)
**Use**: User search, Hashtag search, Content discovery
- **Status**: Configured but requires dependency
- Full-text search with relevance
- Fast autocomplete

**Setup**:
```xml
<!-- Add to pom.xml if needed -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**Docker**:
```bash
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.11.0
```

### 5. S3/MinIO (Media Storage)
**Use**: Images, Videos, Profile pictures
- Already implemented in `MediaService.java`
- Local filesystem fallback
- Unlimited scalability

**Docker (MinIO)**:
```bash
docker run -d --name minio -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=password" \
  minio/minio server /data --console-address ":9001"
```

### 6. Apache Kafka (Event Streaming)
**Use**: Async processing, Notifications, Analytics
- Already configured in `KafkaConfig.java`
- High throughput event streaming
- Decouples services

## Current Implementation

### ✅ Fully Implemented
- **PostgreSQL**: All core entities (User, Post, Comment, etc.)
- **Redis**: Caching, timeline cache, session management
- **Kafka**: Event streaming for notifications
- **Local Storage**: Media files with thumbnail generation

### 📦 Configured (Optional Dependencies)
- **Cassandra**: Timeline storage (requires spring-data-cassandra)
- **Elasticsearch**: Search (requires spring-data-elasticsearch)

### 🎯 Recommendation

**For Development/Demo**:
Use current setup (PostgreSQL + Redis + Kafka) - fully functional

**For Production Scale**:
Add Cassandra + Elasticsearch for:
- Billions of timeline entries
- Advanced search capabilities
- Better horizontal scalability

## Data Flow

### Post Creation
1. **PostgreSQL**: Save post metadata
2. **Local Storage/S3**: Upload media
3. **Kafka**: Publish post-created event
4. **Cassandra** (optional): Fanout to timelines
5. **Elasticsearch** (optional): Index for search
6. **Redis**: Cache invalidation

### Feed Generation
1. **Redis**: Check timeline cache
2. **Cassandra** (optional): Query user timeline
3. **PostgreSQL**: Fetch metadata (fallback)
4. **Redis**: Cache result

### Search
1. **Elasticsearch** (optional): Full-text search
2. **PostgreSQL**: Fallback to SQL LIKE queries
3. **Redis**: Cache popular searches

## Performance Comparison

| Operation | PostgreSQL | Cassandra | Redis |
|-----------|-----------|-----------|-------|
| Timeline Read | 50-100ms | 5-10ms | <1ms |
| Write Fanout | N/A | 1-2ms/user | <1ms |
| Search | 100-500ms | N/A | N/A |
| Scalability | Vertical | Horizontal | Horizontal |

## Migration Path

**Phase 1** (Current): PostgreSQL + Redis + Kafka
- Handles 1M users, 10M posts

**Phase 2**: Add Cassandra
- Handles 100M users, 1B posts
- Better timeline performance

**Phase 3**: Add Elasticsearch
- Advanced search features
- Better user discovery

**Phase 4**: Add S3/CDN
- Global media delivery
- Reduced latency

## Monitoring

```yaml
# Metrics to track
- PostgreSQL: Connection pool, query latency
- Redis: Hit rate, memory usage
- Cassandra: Read/write latency, compaction
- Elasticsearch: Query time, index size
- Kafka: Lag, throughput
```

## Conclusion

Current implementation is production-ready for moderate scale. Optional databases (Cassandra, Elasticsearch) can be added when needed for massive scale (100M+ users).
