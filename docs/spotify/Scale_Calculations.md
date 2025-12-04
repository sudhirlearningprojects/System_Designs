# Spotify Scale Calculations

## User Base & Traffic

### Assumptions
- **Total Users**: 500 million
- **Daily Active Users (DAU)**: 100 million (20%)
- **Premium Users**: 200 million (40%)
- **Artists/Creators**: 10 million (2%)
- **Average listening time**: 2.5 hours/day
- **Peak traffic**: 3x average

### Traffic Patterns
```
Average concurrent users = 100M DAU / 24 hours = 4.17M users
Peak concurrent users = 4.17M × 3 = 12.5M users
```

## Storage Calculations

### Audio Storage

#### Per Track Storage
```
Low Quality (96 kbps):    96 kbps × 180s = 2.16 MB
Medium Quality (160 kbps): 160 kbps × 180s = 3.6 MB
High Quality (320 kbps):   320 kbps × 180s = 7.2 MB
Lossless (FLAC):          ~1411 kbps × 180s = 31.7 MB

Total per track = 2.16 + 3.6 + 7.2 + 31.7 = 44.66 MB
```

#### Total Music Catalog
```
Total tracks: 100 million
Total storage = 100M × 44.66 MB = 4,466 TB = 4.47 PB

With 3x replication = 4.47 PB × 3 = 13.4 PB
```

### Metadata Storage (PostgreSQL)

#### Per Track Metadata
```
Track record: ~2 KB
Album record: ~1 KB
Artist record: ~3 KB

Average per track = 2 KB
Total = 100M tracks × 2 KB = 200 GB
```

#### User Data
```
User record: ~5 KB
Total = 500M users × 5 KB = 2.5 TB
```

#### Playlists
```
Average playlists per user: 10
Total playlists = 500M × 10 = 5 billion
Playlist record: ~1 KB
Total = 5B × 1 KB = 5 TB

Playlist tracks (junction table):
Average tracks per playlist: 50
Total records = 5B × 50 = 250 billion
Record size: ~100 bytes
Total = 250B × 100 bytes = 25 TB
```

#### Total PostgreSQL Storage
```
Tracks: 200 GB
Users: 2.5 TB
Playlists: 5 TB
Playlist Tracks: 25 TB
Albums: 50 GB
User Library: 10 TB
Downloads: 5 TB
Total = ~47.75 TB

With indexes and overhead (2x) = 95.5 TB
```

### Listening History (Cassandra)

#### Daily Events
```
100M DAU × 2.5 hours × 20 tracks/hour = 5 billion events/day
```

#### Storage per Event
```
Event record: ~200 bytes
Daily storage = 5B × 200 bytes = 1 TB/day
Monthly storage = 1 TB × 30 = 30 TB/month
90-day retention = 30 TB × 3 = 90 TB
```

### Total Storage Summary
```
Audio files (S3): 13.4 PB
Metadata (PostgreSQL): 95.5 TB
Listening history (Cassandra): 90 TB
Redis cache: 500 GB
Elasticsearch: 1 TB

Total = 13.59 PB
```

## Bandwidth Calculations

### Streaming Bandwidth

#### Average Quality Distribution
```
Free users (60%): 96 kbps
Premium users (40%): 
  - 50% use 160 kbps
  - 30% use 320 kbps
  - 20% use lossless (1411 kbps)

Weighted average:
= 0.6 × 96 + 0.4 × (0.5 × 160 + 0.3 × 320 + 0.2 × 1411)
= 57.6 + 0.4 × (80 + 96 + 282.2)
= 57.6 + 183.28
= 240.88 kbps per stream
```

#### Peak Bandwidth
```
Peak concurrent streams = 12.5M users
Peak bandwidth = 12.5M × 240.88 kbps
              = 3,011 Gbps
              = 3.01 Tbps
```

#### Daily Bandwidth
```
Total listening hours = 100M users × 2.5 hours = 250M hours
Total bandwidth = 250M hours × 240.88 kbps
                = 250M × 3600s × 240.88 kbps
                = 216,792 TB/day
                = 217 PB/day
```

#### Monthly Bandwidth
```
Monthly = 217 PB × 30 = 6.5 EB/month
```

### CDN Bandwidth
```
CDN cache hit ratio: 80%
CDN bandwidth = 3.01 Tbps × 0.8 = 2.41 Tbps
Origin bandwidth = 3.01 Tbps × 0.2 = 602 Gbps
```

## Database Performance

### PostgreSQL

#### Read Operations
```
Track metadata reads: 5B/day (one per stream)
Playlist reads: 500M/day
User library reads: 200M/day
Total reads = 5.7B/day = 66K reads/sec

Peak reads = 66K × 3 = 198K reads/sec
```

#### Write Operations
```
Track uploads: 100K/day (artists)
Playlist updates: 50M/day
Library updates: 100M/day
Play count updates: 5B/day (batched)
Total writes = 5.15B/day = 59.6K writes/sec

Peak writes = 59.6K × 3 = 179K writes/sec
```

#### Sharding Strategy
```
Shard by user_id (consistent hashing)
Number of shards = 100

Per shard:
- Reads: 1,980 reads/sec
- Writes: 1,790 writes/sec
- Storage: 955 GB
```

### Cassandra

#### Write Operations
```
Listening events: 5B/day = 57.9K writes/sec
Peak writes = 57.9K × 3 = 174K writes/sec
```

#### Read Operations
```
History queries: 100M/day = 1,157 reads/sec
Peak reads = 1,157 × 3 = 3,471 reads/sec
```

#### Cluster Size
```
Replication factor: 3
Nodes needed (write capacity): 174K / 10K per node = 18 nodes
Nodes needed (storage): 90 TB / 2 TB per node = 45 nodes

Total nodes = 45 (storage is bottleneck)
With 3x replication = 45 nodes
```

### Redis Cache

#### Cache Size
```
Hot tracks (top 100K): 100K × 2 KB = 200 MB
User sessions (10M active): 10M × 5 KB = 50 GB
Playlist cache (1M hot): 1M × 50 KB = 50 GB
Total = ~100 GB per instance

With 5 replicas = 500 GB total
```

#### Cache Operations
```
Cache reads: 10M/sec (90% hit rate)
Cache writes: 100K/sec
```

### Elasticsearch

#### Index Size
```
100M tracks × 10 KB per document = 1 TB
With replicas (2x) = 2 TB
```

#### Search Operations
```
Search queries: 50M/day = 579 queries/sec
Peak = 579 × 3 = 1,737 queries/sec
```

## Cost Estimation (AWS)

### Compute (EC2)

#### Application Servers
```
Instance type: c5.4xlarge (16 vCPU, 32 GB RAM)
Number of instances: 200 (for 12.5M concurrent users)
Cost per instance: $0.68/hour
Total = 200 × $0.68 × 730 hours = $99,280/month
```

#### Transcoding Workers
```
Instance type: c5.9xlarge (36 vCPU, 72 GB RAM)
Number of instances: 50
Cost per instance: $1.53/hour
Total = 50 × $1.53 × 730 = $55,845/month
```

### Storage

#### S3 (Audio Files)
```
Storage: 13.4 PB
Cost: $0.023/GB for first 50 TB, $0.022/GB for next 450 TB, $0.021/GB after
Average cost: ~$0.021/GB
Total = 13,400,000 GB × $0.021 = $281,400/month
```

#### RDS PostgreSQL
```
Instance: db.r5.24xlarge (96 vCPU, 768 GB RAM)
Number of instances: 10 (sharded)
Cost per instance: $13.92/hour
Total = 10 × $13.92 × 730 = $101,616/month

Storage: 100 TB × $0.115/GB = $11,500/month
```

#### Cassandra (Managed)
```
Instance: i3.4xlarge (16 vCPU, 122 GB RAM, 2×1.9 TB NVMe)
Number of nodes: 45
Cost per node: $3.12/hour
Total = 45 × $3.12 × 730 = $102,492/month
```

#### ElastiCache Redis
```
Instance: cache.r5.4xlarge (16 vCPU, 104 GB RAM)
Number of instances: 5
Cost per instance: $1.344/hour
Total = 5 × $1.344 × 730 = $4,906/month
```

#### Elasticsearch
```
Instance: r5.4xlarge.elasticsearch (16 vCPU, 128 GB RAM)
Number of instances: 10
Cost per instance: $1.952/hour
Total = 10 × $1.952 × 730 = $14,250/month
```

### Bandwidth

#### CloudFront CDN
```
Data transfer out: 6.5 EB/month
Cost tiers:
- First 10 TB: $0.085/GB
- Next 40 TB: $0.080/GB
- Next 100 TB: $0.060/GB
- Next 350 TB: $0.040/GB
- Over 500 TB: $0.030/GB

Average cost: ~$0.030/GB
Total = 6,500,000,000 GB × $0.030 = $195,000,000/month
```

#### Data Transfer (Origin to CDN)
```
Origin bandwidth: 217 PB/month × 0.2 = 43.4 PB
Cost: $0.02/GB
Total = 43,400,000 GB × $0.02 = $868,000/month
```

### Total Monthly Cost
```
Compute: $155,125
Storage: $515,764
Bandwidth: $195,868,000
Total = $196,538,889/month

Per user per month = $196.5M / 500M = $0.39
Per DAU per month = $196.5M / 100M = $1.97
```

### Revenue Model

#### Subscription Pricing
```
Free: $0 (ad-supported)
Individual: $10.99/month
Duo: $14.99/month
Family: $16.99/month
Student: $5.99/month
```

#### Revenue Calculation
```
Premium users: 200M
Average subscription: $9/month (weighted average)
Monthly revenue = 200M × $9 = $1,800M

Free users (ad revenue): 300M × $0.50 = $150M

Total monthly revenue = $1,950M
Profit margin = ($1,950M - $196.5M) / $1,950M = 89.9%
```

## Performance Targets

### Latency Requirements
```
Search: <200ms (p99)
Stream start: <500ms (p99)
API calls: <100ms (p99)
Playlist operations: <200ms (p99)
```

### Availability
```
Target: 99.99% uptime
Allowed downtime: 4.38 minutes/month
```

### Throughput
```
Concurrent streams: 12.5M
API requests: 500K/sec
Search queries: 2K/sec
Database writes: 180K/sec
Database reads: 200K/sec
```

## Optimization Strategies

### 1. Audio Compression
```
Use Opus codec instead of MP3:
- 30% better compression at same quality
- Storage savings: 13.4 PB × 0.3 = 4 PB saved
- Cost savings: 4,000,000 GB × $0.021 = $84,000/month
```

### 2. CDN Optimization
```
Increase cache hit ratio from 80% to 90%:
- Origin bandwidth reduction: 602 Gbps → 301 Gbps
- Cost savings: $868K × 0.5 = $434K/month
```

### 3. Adaptive Bitrate Streaming
```
Dynamically adjust quality based on network:
- Average quality reduction: 240 kbps → 200 kbps
- Bandwidth savings: 17%
- Cost savings: $195.8M × 0.17 = $33.3M/month
```

### 4. Deduplication
```
Many tracks are duplicates (different albums, compilations)
Deduplication ratio: 15%
Storage savings: 13.4 PB × 0.15 = 2 PB
Cost savings: 2,000,000 GB × $0.021 = $42K/month
```

### 5. Intelligent Caching
```
Cache popular tracks in edge locations:
- Top 1% tracks account for 80% of streams
- Edge cache size: 100K tracks × 7.2 MB = 720 GB per location
- 200 edge locations = 144 TB total
- Reduces origin bandwidth by 60%
```

## Scalability Considerations

### Horizontal Scaling
```
Application servers: Auto-scale based on CPU (target 70%)
Database: Shard by user_id, add shards as needed
Cassandra: Add nodes for storage/throughput
Redis: Add replicas for read scaling
```

### Vertical Scaling Limits
```
PostgreSQL: Max 96 vCPU per instance
Cassandra: Max 2 TB per node (NVMe)
Redis: Max 6.1 TB per instance
```

### Geographic Distribution
```
Regions: 10 (US-East, US-West, EU, Asia, etc.)
CDN PoPs: 200+ worldwide
Database replicas: 3 per region
Latency target: <50ms to nearest PoP
```
