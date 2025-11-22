# Instagram Database Architecture

## Multi-Database Strategy

### 1. **PostgreSQL** - Relational Data
**Use Case**: User profiles, relationships, metadata

**Tables**:
- `users` - User accounts, profiles, settings
- `follows` - Follow relationships
- `post_likes` - Like relationships
- `comment_likes` - Comment like relationships

**Why PostgreSQL**:
- ACID compliance for critical user data
- Complex joins for relationships
- Strong consistency for financial/auth data
- Mature ecosystem

---

### 2. **Cassandra** - Timeline/Feed Storage
**Use Case**: User timelines, activity feeds

**Tables**:
- `user_timeline` - Partitioned by user_id, clustered by created_at
  - Optimized for time-series reads
  - Fast pagination with clustering keys
  - Write-optimized for fanout

**Schema**:
```cql
CREATE TABLE user_timeline (
    user_id bigint,
    created_at timestamp,
    post_id text,
    author_id bigint,
    author_username text,
    content text,
    media_urls list<text>,
    like_count int,
    comment_count int,
    PRIMARY KEY (user_id, created_at, post_id)
) WITH CLUSTERING ORDER BY (created_at DESC);
```

**Why Cassandra**:
- Horizontal scalability for billions of timeline entries
- Optimized for time-series data
- Fast writes for fanout operations
- No single point of failure
- Linear scalability

**Fanout Strategy**:
- Celebrity users (>1M followers): Pull model
- Regular users: Push model (fanout on write)

---

### 3. **Redis** - Caching & Real-time Data
**Use Case**: Hot data, sessions, counters, real-time features

**Data Structures**:
- `timeline:{userId}` - Cached timeline (5 min TTL)
- `post:{postId}` - Post cache (10 min TTL)
- `user:{userId}` - User profile cache (30 min TTL)
- `trending:hashtags` - Sorted set for trending hashtags
- `online:users` - Set of online user IDs
- `story:views:{storyId}` - Story view tracking

**Why Redis**:
- Sub-millisecond latency
- Reduce database load by 80%
- Real-time counters and leaderboards
- Session management
- Pub/Sub for real-time features

---

### 4. **Elasticsearch** - Search & Discovery
**Use Case**: User search, hashtag search, content discovery

**Indices**:
- `users` - Username, full name, bio (full-text search)
- `posts` - Content, hashtags, location
- `hashtags` - Trending hashtags with counts

**Why Elasticsearch**:
- Full-text search with relevance scoring
- Fuzzy matching for typos
- Aggregations for trending content
- Fast autocomplete
- Scalable search infrastructure

---

### 5. **S3/Object Storage** - Media Files
**Use Case**: Images, videos, profile pictures

**Structure**:
```
/media/
  /images/{userId}/{postId}/original.jpg
  /images/{userId}/{postId}/thumbnail.jpg
  /videos/{userId}/{postId}/video.mp4
  /profiles/{userId}/avatar.jpg
```

**Why S3**:
- Unlimited scalability
- 99.999999999% durability
- CDN integration (CloudFront)
- Cost-effective for large files
- Automatic replication

---

### 6. **Apache Kafka** - Event Streaming
**Use Case**: Async processing, notifications, analytics

**Topics**:
- `post-created` - New post events
- `post-liked` - Like events
- `notifications` - Notification events
- `user-activity` - Analytics events

**Why Kafka**:
- High throughput (millions of events/sec)
- Durable message storage
- Decouples services
- Real-time stream processing
- Replay capability

---

## Data Flow Examples

### Post Creation Flow
1. **PostgreSQL**: Save post metadata
2. **S3**: Upload media files
3. **Kafka**: Publish post-created event
4. **Cassandra**: Fanout to follower timelines (async)
5. **Elasticsearch**: Index post for search
6. **Redis**: Invalidate user cache

### Feed Generation Flow
1. **Redis**: Check cache for timeline
2. **Cassandra**: Query user_timeline if cache miss
3. **PostgreSQL**: Fetch additional metadata (likes, comments)
4. **Redis**: Cache result for 5 minutes

### Search Flow
1. **Elasticsearch**: Full-text search on users/posts
2. **Redis**: Cache popular search results
3. **PostgreSQL**: Fetch complete user profiles

---

## Scalability Numbers

| Database | Use Case | Scale |
|----------|----------|-------|
| PostgreSQL | Users, Relationships | 2B users, 100M follows/day |
| Cassandra | Timelines | 500M posts/day, 50B timeline entries |
| Redis | Cache, Real-time | 10M ops/sec, 1TB memory |
| Elasticsearch | Search | 100M documents, 10K searches/sec |
| S3 | Media | 734PB storage, 100K uploads/sec |
| Kafka | Events | 1M events/sec, 7-day retention |

---

## Consistency Models

- **PostgreSQL**: Strong consistency (ACID)
- **Cassandra**: Eventual consistency (tunable)
- **Redis**: Strong consistency (single-threaded)
- **Elasticsearch**: Near real-time (1s refresh)
- **S3**: Eventual consistency (read-after-write for new objects)

---

## Backup & Disaster Recovery

- **PostgreSQL**: Daily full backup + WAL archiving
- **Cassandra**: Snapshot + incremental backups
- **Redis**: RDB snapshots + AOF
- **Elasticsearch**: Snapshot to S3
- **S3**: Cross-region replication
- **Kafka**: Multi-datacenter replication

---

## Cost Optimization

1. **Hot/Warm/Cold Storage**:
   - Hot: Redis (recent 24h)
   - Warm: Cassandra (recent 30 days)
   - Cold: S3 Glacier (>30 days)

2. **Compression**:
   - Cassandra: LZ4 compression
   - S3: Gzip for text, optimized codecs for media

3. **TTL Policies**:
   - Stories: 24-hour TTL
   - Notifications: 30-day TTL
   - Old posts: Archive to cold storage

---

## Monitoring & Observability

- **Metrics**: Prometheus + Grafana
- **Logs**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: Jaeger for distributed tracing
- **Alerts**: PagerDuty for critical issues

---

**Reference**: Based on Instagram's actual architecture and industry best practices.
