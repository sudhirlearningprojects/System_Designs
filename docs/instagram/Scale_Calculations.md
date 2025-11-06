# Instagram Clone - Scale Calculations

## 1. User and Usage Assumptions

### User Base
- **Total Users**: 2 billion registered users
- **Daily Active Users (DAU)**: 100 million (5% of total)
- **Monthly Active Users (MAU)**: 500 million (25% of total)
- **Peak Concurrent Users**: 10 million (10% of DAU)

### User Behavior Patterns
- **Average session duration**: 30 minutes
- **Sessions per day per user**: 3 sessions
- **Posts viewed per session**: 50 posts
- **Posts created per user per day**: 0.5 posts (50M posts/day from 100M DAU)
- **Stories created per user per day**: 1 story
- **Messages sent per user per day**: 20 messages
- **Searches per user per day**: 5 searches

## 2. Storage Calculations

### User Data Storage
```
Users: 2B users × 1KB metadata per user = 2TB
User profiles: 2B users × 5KB extended profile = 10TB
User sessions: 10M concurrent × 1KB session data = 10GB
Total User Data: ~12TB
```

### Post Data Storage
```
Posts per day: 100M DAU × 0.5 posts = 50M posts/day
Posts per year: 50M × 365 = 18.25B posts/year
Post metadata: 18.25B × 2KB = 36.5TB/year
Post content over 5 years: 18.25B × 5 years × 2KB = 182.5TB

Media Storage (Images):
- Average image size: 2MB (after compression)
- Images per day: 50M posts × 1.5 images/post = 75M images/day
- Daily image storage: 75M × 2MB = 150TB/day
- Annual image storage: 150TB × 365 = 54.75PB/year

Media Storage (Videos):
- Video posts: 10% of total posts = 5M videos/day
- Average video size: 50MB (after compression)
- Daily video storage: 5M × 50MB = 250TB/day
- Annual video storage: 250TB × 365 = 91.25PB/year

Total Media Storage (5 years): (54.75 + 91.25) × 5 = 730PB
```

### Stories Storage
```
Stories per day: 100M DAU × 1 story = 100M stories/day
Story retention: 24 hours
Active stories storage: 100M × 5MB avg = 500TB
Story archive (optional): 100M × 365 × 5MB = 182.5PB/year
```

### Comments and Interactions
```
Comments per day: 50M posts × 10 comments/post = 500M comments/day
Comment storage: 500M × 200 bytes = 100GB/day
Annual comment storage: 100GB × 365 = 36.5TB/year

Likes per day: 50M posts × 50 likes/post = 2.5B likes/day
Like storage: 2.5B × 16 bytes = 40GB/day
Annual like storage: 40GB × 365 = 14.6TB/year
```

### Messages Storage
```
Messages per day: 100M DAU × 20 messages = 2B messages/day
Message storage: 2B × 1KB avg = 2TB/day
Annual message storage: 2TB × 365 = 730TB/year
Message retention: 5 years = 3.65PB
```

### Total Storage Requirements (5 Years)
```
User Data: 12TB
Post Metadata: 182.5TB
Media Storage: 730PB
Comments: 182.5TB
Likes: 73TB
Messages: 3.65PB
Stories Archive: 912.5PB (if archived)

Total (excluding story archive): ~734PB
Total (including story archive): ~1,647PB
```

## 3. Bandwidth Calculations

### Read Traffic
```
Feed requests: 100M DAU × 10 feed refreshes × 20 posts = 20B post views/day
Post data transfer: 20B × 2MB avg = 40PB/day = 463GB/s

Image views: 20B post views × 1.5 images = 30B image views/day
Image bandwidth: 30B × 2MB = 60PB/day = 694GB/s

Video views: 20B post views × 10% video rate = 2B video views/day
Video bandwidth: 2B × 50MB = 100PB/day = 1,157GB/s

Stories views: 100M DAU × 50 stories viewed = 5B story views/day
Story bandwidth: 5B × 5MB = 25PB/day = 289GB/s

Total Read Bandwidth: 225PB/day = 2,603GB/s
Peak Read Bandwidth (3x): 7,809GB/s
```

### Write Traffic
```
Post uploads: 50M posts/day × 2MB = 100TB/day = 1.16GB/s
Story uploads: 100M stories/day × 5MB = 500TB/day = 5.79GB/s
Message uploads: 2B messages/day × 1KB = 2TB/day = 0.023GB/s

Total Write Bandwidth: 602TB/day = 6.97GB/s
Peak Write Bandwidth (5x): 34.85GB/s
```

## 4. QPS (Queries Per Second) Calculations

### Read Operations
```
Feed Generation:
- Feed requests: 100M DAU × 10 refreshes / 86,400s = 11,574 QPS
- Peak feed QPS: 11,574 × 3 = 34,722 QPS

Post Interactions:
- Post views: 20B views/day / 86,400s = 231,481 QPS
- Like/unlike: 2.5B interactions/day / 86,400s = 28,935 QPS
- Comment reads: 500M comments/day / 86,400s = 5,787 QPS

Search Operations:
- User searches: 100M DAU × 5 searches / 86,400s = 5,787 QPS
- Peak search QPS: 5,787 × 2 = 11,574 QPS

Message Operations:
- Message reads: 2B messages/day × 2 reads / 86,400s = 46,296 QPS

Total Read QPS: ~328,000 QPS
Peak Read QPS: ~500,000 QPS
```

### Write Operations
```
Post Creation:
- New posts: 50M posts/day / 86,400s = 579 QPS
- Peak post creation: 579 × 5 = 2,895 QPS

User Interactions:
- Likes: 2.5B likes/day / 86,400s = 28,935 QPS
- Comments: 500M comments/day / 86,400s = 5,787 QPS
- Follows: 100M DAU × 2 follows/day / 86,400s = 2,315 QPS

Messages:
- Message sends: 2B messages/day / 86,400s = 23,148 QPS

Stories:
- Story creation: 100M stories/day / 86,400s = 1,157 QPS

Total Write QPS: ~62,000 QPS
Peak Write QPS: ~100,000 QPS
```

## 5. Database Sizing

### PostgreSQL (User Service)
```
Users table: 2B rows × 1KB = 2TB
User sessions: 10M active sessions × 1KB = 10GB
User relationships cache: 500M relationships × 100 bytes = 50GB
Indexes: ~50% of data size = 1TB

Total PostgreSQL: ~3TB per shard
Recommended shards: 100 shards = 300TB total
```

### Cassandra (Post Service)
```
Posts table: 91.25B posts (5 years) × 2KB = 182.5TB
Comments table: 912.5B comments (5 years) × 200 bytes = 182.5TB
Likes table: 4.56T likes (5 years) × 16 bytes = 73TB
Replication factor: 3x = 1.31PB

Total Cassandra cluster: ~1.31PB
```

### Redis (Caching)
```
User sessions: 10M sessions × 1KB = 10GB
Feed cache: 100M users × 1MB feed = 100TB
Hot post cache: 1M hot posts × 2KB = 2GB
Search cache: 10GB
Social graph cache: 50GB

Total Redis cluster: ~100TB
```

### Neo4j (Social Graph)
```
User nodes: 2B users × 100 bytes = 200GB
Follow relationships: 100B relationships × 50 bytes = 5TB
Indexes and metadata: 1TB

Total Neo4j cluster: ~6TB
```

### MongoDB (Messages)
```
Messages: 3.65PB (5 years retention)
Conversations: 500M conversations × 1KB = 500GB
Indexes: 20% of data = 730TB

Total MongoDB cluster: ~4.38PB
```

### Elasticsearch (Search)
```
User index: 2B users × 500 bytes = 1TB
Post index: 91.25B posts × 1KB = 91.25TB
Hashtag index: 10M hashtags × 1KB = 10GB
Replication: 2x = 184.5TB

Total Elasticsearch cluster: ~185TB
```

## 6. Infrastructure Requirements

### Application Servers
```
Peak concurrent users: 10M
Concurrent connections per server: 10,000
Required servers: 10M / 10,000 = 1,000 servers

CPU per server: 16 cores
Memory per server: 64GB
Total: 16,000 CPU cores, 64TB RAM
```

### Load Balancers
```
Peak bandwidth: 7,809GB/s read + 35GB/s write = 7,844GB/s
Load balancer capacity: 100GB/s per unit
Required load balancers: 79 units (with redundancy: 100 units)
```

### CDN Requirements
```
Media bandwidth: 2,140GB/s (images + videos + stories)
CDN edge locations: 200+ global locations
Cache hit ratio: 95%
Origin bandwidth: 107GB/s
```

## 7. Cost Estimation (AWS)

### Compute (EC2)
```
Application servers: 1,000 × c5.4xlarge × $0.68/hour × 24 × 365 = $5.96M/year
Database servers: 200 × r5.8xlarge × $2.02/hour × 24 × 365 = $3.54M/year
Total compute: ~$9.5M/year
```

### Storage (S3)
```
Media storage: 734PB × $0.023/GB/month × 12 = $202.6M/year
Backup storage: 100PB × $0.004/GB/month × 12 = $4.8M/year
Total storage: ~$207.4M/year
```

### Data Transfer
```
CDN (CloudFront): 2,140GB/s × 86,400s × 365 × $0.085/GB = $5.9B/year
Inter-AZ transfer: 500GB/s × 86,400s × 365 × $0.01/GB = $157M/year
Total data transfer: ~$6.06B/year
```

### Database Services
```
RDS PostgreSQL: 100 instances × db.r5.24xlarge × $13.44/hour × 24 × 365 = $11.8M/year
ElastiCache Redis: 50 clusters × cache.r6g.12xlarge × $3.02/hour × 24 × 365 = $1.3M/year
Total managed databases: ~$13.1M/year
```

### Total Annual Cost: ~$6.29B/year

## 8. Performance Targets

### Latency Requirements
```
Feed generation: < 200ms (P95)
Post interactions: < 100ms (P95)
Search queries: < 150ms (P95)
Message delivery: < 50ms (P95)
Media upload: < 2s for images, < 10s for videos
```

### Throughput Requirements
```
Read QPS: 500K QPS (peak)
Write QPS: 100K QPS (peak)
Media upload: 10K uploads/second (peak)
Concurrent WebSocket connections: 10M
```

### Availability Targets
```
Overall system: 99.99% (52.6 minutes downtime/year)
Core services: 99.95% (4.38 hours downtime/year)
Media delivery: 99.9% (8.77 hours downtime/year)
```

## 9. Scaling Strategies

### Horizontal Scaling
```
Application tier: Auto-scaling groups (100-2000 instances)
Database tier: Sharding and read replicas
Cache tier: Redis cluster with consistent hashing
CDN: Global edge locations with regional failover
```

### Vertical Scaling Limits
```
Single server capacity: 64 CPU cores, 512GB RAM
Database server capacity: 96 CPU cores, 768GB RAM
Cache server capacity: 48 CPU cores, 384GB RAM
```

### Geographic Distribution
```
Primary regions: US-East, US-West, EU-West, Asia-Pacific
Secondary regions: 8 additional regions for disaster recovery
Edge locations: 200+ CDN points of presence
```

## 10. Bottleneck Analysis

### Potential Bottlenecks
1. **Database Write Capacity**: Cassandra cluster at 100K writes/second
2. **CDN Bandwidth**: 2.14TB/s media delivery during peak hours
3. **Feed Generation**: Real-time feed updates for 10M concurrent users
4. **Search Index Updates**: Elasticsearch indexing 50M posts/day
5. **WebSocket Connections**: 10M concurrent real-time connections

### Mitigation Strategies
1. **Database**: Increase Cassandra cluster size, optimize write patterns
2. **CDN**: Multi-CDN strategy, edge caching optimization
3. **Feed**: Hybrid push/pull model, pre-computed feeds for active users
4. **Search**: Async indexing, search result caching
5. **WebSocket**: Connection pooling, message queuing for offline users

This comprehensive scale calculation provides the foundation for building and operating an Instagram-scale social media platform with proper capacity planning and cost optimization.