# Google Docs - Scale Calculations

## Table of Contents
1. [Traffic Estimates](#traffic-estimates)
2. [Storage Calculations](#storage-calculations)
3. [Bandwidth Requirements](#bandwidth-requirements)
4. [Database Sizing](#database-sizing)
5. [Cache Sizing](#cache-sizing)
6. [Server Capacity](#server-capacity)
7. [Cost Analysis](#cost-analysis)

---

## Traffic Estimates

### User Base
- **Total Users:** 1 Billion
- **Daily Active Users (DAU):** 100 Million (10%)
- **Monthly Active Users (MAU):** 300 Million (30%)
- **Peak Concurrent Users:** 10 Million (10% of DAU)

### Document Statistics
- **Total Documents:** 5 Billion
- **Documents per User:** 5 average
- **Active Documents (being edited):** 5 Million at peak
- **New Documents per Day:** 50 Million

### Request Patterns

#### Read Requests
- **Document Opens:** 100M users × 10 docs/day = 1B requests/day
- **Requests per Second (RPS):** 1B / 86,400 = 11,574 RPS
- **Peak RPS (3x average):** 34,722 RPS

#### Write Requests (Edit Operations)
- **Active Editors:** 10M concurrent users
- **Operations per User per Minute:** 20 (typing, deleting, formatting)
- **Operations per Second:** 10M × 20 / 60 = 3.33M ops/sec
- **Peak Operations:** 5M ops/sec

#### Version Saves
- **Auto-save Frequency:** Every 3 seconds
- **Active Documents:** 5M
- **Version Saves per Second:** 5M / 3 = 1.67M saves/sec

#### Comments & Suggestions
- **Comments per Day:** 50M
- **Comments per Second:** 50M / 86,400 = 579 RPS
- **Suggestions per Day:** 20M
- **Suggestions per Second:** 20M / 86,400 = 231 RPS

### Total Request Load
- **Read RPS:** 34,722
- **Write RPS:** 5,000,000
- **Version Save RPS:** 1,670,000
- **Comment/Suggestion RPS:** 810
- **Total Peak RPS:** ~6.7M requests/second

---

## Storage Calculations

### Document Storage

#### Average Document Size
- **Plain Text:** 50 KB average
- **With Formatting:** 100 KB average
- **With Images (20% of docs):** 500 KB average
- **Weighted Average:** (0.8 × 100 KB) + (0.2 × 500 KB) = 180 KB

#### Total Document Storage
- **Documents:** 5 Billion
- **Storage:** 5B × 180 KB = 900 TB = 0.9 PB

#### Version History Storage
- **Versions per Document:** 20 average
- **Version Size:** 100 KB average (compressed)
- **Total Versions:** 5B × 20 = 100 Billion versions
- **Version Storage:** 100B × 100 KB = 10 PB

### Metadata Storage

#### Document Metadata
```
Document Record:
- ID: 36 bytes (UUID)
- Title: 100 bytes
- Owner ID: 36 bytes
- Status: 10 bytes
- Timestamps: 16 bytes
- Watermark: 200 bytes
- Tags: 100 bytes
- Version: 4 bytes
Total: ~500 bytes per document
```

**Total Metadata:** 5B × 500 bytes = 2.5 TB

#### Comments & Suggestions
- **Comments per Document:** 5 average
- **Comment Size:** 500 bytes
- **Total Comments:** 5B × 5 = 25B comments
- **Comment Storage:** 25B × 500 bytes = 12.5 TB

- **Suggestions per Document:** 2 average
- **Suggestion Size:** 300 bytes
- **Total Suggestions:** 5B × 2 = 10B suggestions
- **Suggestion Storage:** 10B × 300 bytes = 3 TB

### Permissions Storage
- **Shared Documents:** 30% of total = 1.5B
- **Permissions per Shared Doc:** 3 average
- **Permission Record Size:** 200 bytes
- **Total Permissions:** 1.5B × 3 = 4.5B
- **Permission Storage:** 4.5B × 200 bytes = 900 GB

### Total Storage Requirements

| Component | Storage |
|-----------|---------|
| Documents | 0.9 PB |
| Versions | 10 PB |
| Metadata | 2.5 TB |
| Comments | 12.5 TB |
| Suggestions | 3 TB |
| Permissions | 900 GB |
| **Total** | **~11 PB** |

**With Replication (3x):** 33 PB
**With Backups (2x):** 66 PB

---

## Bandwidth Requirements

### Inbound Traffic

#### Document Edits
- **Operations per Second:** 5M
- **Operation Size:** 200 bytes average
- **Bandwidth:** 5M × 200 bytes = 1 GB/sec = 8 Gbps

#### Document Uploads
- **New Documents per Second:** 50M / 86,400 = 579
- **Average Size:** 180 KB
- **Bandwidth:** 579 × 180 KB = 104 MB/sec = 832 Mbps

#### Comments & Suggestions
- **Requests per Second:** 810
- **Average Size:** 500 bytes
- **Bandwidth:** 810 × 500 bytes = 405 KB/sec = 3.2 Mbps

**Total Inbound:** ~9 Gbps

### Outbound Traffic

#### Document Reads
- **Requests per Second:** 34,722
- **Average Size:** 180 KB
- **Bandwidth:** 34,722 × 180 KB = 6.25 GB/sec = 50 Gbps

#### Real-time Operation Broadcasting
- **Operations per Second:** 5M
- **Average Recipients per Operation:** 2 (collaborative editing)
- **Operation Size:** 200 bytes
- **Bandwidth:** 5M × 2 × 200 bytes = 2 GB/sec = 16 Gbps

#### Version History
- **Version Requests per Second:** 1,000
- **Average Size:** 100 KB
- **Bandwidth:** 1,000 × 100 KB = 100 MB/sec = 800 Mbps

**Total Outbound:** ~67 Gbps

### Total Bandwidth
- **Inbound:** 9 Gbps
- **Outbound:** 67 Gbps
- **Total:** 76 Gbps
- **Peak (3x):** 228 Gbps

### Monthly Data Transfer
- **Average Bandwidth:** 76 Gbps
- **Seconds per Month:** 2,592,000
- **Monthly Transfer:** 76 Gbps × 2,592,000 sec = 197 PB/month

---

## Database Sizing

### PostgreSQL (Metadata)

#### Data Size
- **Documents:** 5B × 500 bytes = 2.5 TB
- **Permissions:** 4.5B × 200 bytes = 900 GB
- **Comments:** 25B × 500 bytes = 12.5 TB
- **Suggestions:** 10B × 300 bytes = 3 TB
- **Total Data:** 18.9 TB

#### Indexes
- **Index Overhead:** 30% of data size
- **Index Size:** 18.9 TB × 0.3 = 5.67 TB

#### Total PostgreSQL Storage
- **Data + Indexes:** 24.57 TB
- **With Replication (3x):** 73.71 TB

#### Connection Pool
- **Application Servers:** 100
- **Connections per Server:** 50
- **Total Connections:** 5,000
- **PgBouncer Pool:** 1,000 actual connections

#### IOPS Requirements
- **Read IOPS:** 50,000
- **Write IOPS:** 20,000
- **Total IOPS:** 70,000

### Cassandra (Version History)

#### Data Size
- **Versions:** 100B × 100 KB = 10 PB
- **Replication Factor:** 3
- **Total Storage:** 30 PB

#### Cluster Configuration
- **Node Storage:** 2 TB per node
- **Nodes Required:** 30 PB / 2 TB = 15,000 nodes
- **With Overhead (20%):** 18,000 nodes

#### Partitioning
- **Partition Key:** document_id
- **Partitions:** 5 Billion (one per document)
- **Average Partition Size:** 10 PB / 5B = 2 MB

#### Read/Write Throughput
- **Version Saves per Second:** 1.67M
- **Version Reads per Second:** 1,000
- **Consistency Level:** QUORUM (2 of 3 replicas)

---

## Cache Sizing

### Redis Cluster

#### Hot Document Cache
- **Hot Documents:** 1% of total = 50M documents
- **Document Size:** 180 KB average
- **Cache Size:** 50M × 180 KB = 9 TB

#### Active Session Cache
- **Concurrent Users:** 10M
- **Session Data per User:** 1 KB
- **Session Cache:** 10M × 1 KB = 10 GB

#### Pending Operations Queue
- **Active Documents:** 5M
- **Operations per Document:** 100 (buffered)
- **Operation Size:** 200 bytes
- **Queue Size:** 5M × 100 × 200 bytes = 100 GB

#### Total Redis Memory
- **Hot Documents:** 9 TB
- **Sessions:** 10 GB
- **Operations:** 100 GB
- **Total:** 9.11 TB

#### Redis Cluster Configuration
- **Memory per Node:** 64 GB
- **Nodes Required:** 9.11 TB / 64 GB = 142 nodes
- **With Replication (2x):** 284 nodes
- **Cluster Setup:** 142 masters + 142 replicas

#### Cache Hit Ratio
- **Target Hit Ratio:** 95%
- **Cache Misses:** 5% of 34,722 RPS = 1,736 RPS
- **Database Load Reduction:** 95%

---

## Server Capacity

### Application Servers

#### Request Handling Capacity
- **Requests per Server:** 1,000 RPS
- **Peak RPS:** 6.7M
- **Servers Required:** 6.7M / 1,000 = 6,700 servers

#### Server Specifications
- **Instance Type:** c5.2xlarge (8 vCPU, 16 GB RAM)
- **Cost per Instance:** $0.34/hour
- **Total Servers:** 6,700
- **With Redundancy (20%):** 8,040 servers

### WebSocket Servers

#### Connection Capacity
- **Connections per Server:** 10,000
- **Concurrent Users:** 10M
- **Servers Required:** 10M / 10,000 = 1,000 servers

#### Server Specifications
- **Instance Type:** c5.xlarge (4 vCPU, 8 GB RAM)
- **Cost per Instance:** $0.17/hour
- **Total Servers:** 1,000
- **With Redundancy (20%):** 1,200 servers

### Load Balancers

#### Application Load Balancers
- **Capacity per ALB:** 100,000 RPS
- **Peak RPS:** 6.7M
- **ALBs Required:** 67
- **Cost per ALB:** $0.0225/hour + $0.008/LCU

#### Network Load Balancers (WebSocket)
- **Capacity per NLB:** 1M connections
- **Concurrent Connections:** 10M
- **NLBs Required:** 10
- **Cost per NLB:** $0.0225/hour + $0.006/NLCU

---

## Cost Analysis

### Compute Costs (Monthly)

#### Application Servers
- **Instances:** 8,040 × c5.2xlarge
- **Cost:** 8,040 × $0.34 × 730 hours = $1,995,336

#### WebSocket Servers
- **Instances:** 1,200 × c5.xlarge
- **Cost:** 1,200 × $0.17 × 730 hours = $148,920

#### Total Compute:** $2,144,256/month

### Storage Costs (Monthly)

#### S3 (Documents)
- **Storage:** 11 PB
- **Cost:** 11,000 TB × $0.023/GB = $253,000

#### PostgreSQL (RDS)
- **Storage:** 74 TB
- **Instance:** db.r5.24xlarge × 10 instances
- **Cost:** 10 × $13.248 × 730 = $96,710
- **Storage Cost:** 74,000 GB × $0.115 = $8,510
- **Total RDS:** $105,220

#### Cassandra (EC2 + EBS)
- **Instances:** 18,000 × i3.2xlarge
- **Cost:** 18,000 × $0.624 × 730 = $8,201,280

#### Redis (ElastiCache)
- **Nodes:** 284 × r5.4xlarge
- **Cost:** 284 × $1.344 × 730 = $279,475

#### Total Storage:** $8,839,975/month

### Network Costs (Monthly)

#### Data Transfer Out
- **Monthly Transfer:** 197 PB
- **Cost:** 197,000 TB × $0.09/GB = $17,730,000

#### CloudFront (CDN)
- **Transfer:** 50 PB
- **Cost:** 50,000 TB × $0.085/GB = $4,250,000

#### Total Network:** $21,980,000/month

### Total Monthly Cost

| Component | Cost |
|-----------|------|
| Compute | $2.14M |
| Storage | $8.84M |
| Network | $21.98M |
| **Total** | **$32.96M** |

### Cost per User
- **Monthly Active Users:** 300M
- **Cost per MAU:** $32.96M / 300M = $0.11
- **Annual Cost per MAU:** $1.32

### Cost per Document
- **New Documents per Month:** 1.5B
- **Cost per Document:** $32.96M / 1.5B = $0.022

---

## Performance Benchmarks

### Latency Targets

| Operation | p50 | p95 | p99 |
|-----------|-----|-----|-----|
| Document Read | 50ms | 100ms | 200ms |
| Document Write | 100ms | 200ms | 300ms |
| Real-time Sync | 100ms | 300ms | 500ms |
| Version Save | 200ms | 500ms | 1s |
| Comment Add | 50ms | 100ms | 150ms |

### Throughput Targets

| Operation | Target |
|-----------|--------|
| Document Reads | 35K RPS |
| Edit Operations | 5M ops/sec |
| Version Saves | 1.7M saves/sec |
| WebSocket Messages | 10M msg/sec |

### Availability Targets

- **Uptime SLA:** 99.99%
- **Downtime per Year:** 52.56 minutes
- **Downtime per Month:** 4.38 minutes
- **Recovery Time Objective (RTO):** 5 minutes
- **Recovery Point Objective (RPO):** 1 minute

---

## Scalability Limits

### Current Architecture Limits

| Component | Current Capacity | Max Capacity | Scaling Method |
|-----------|-----------------|--------------|----------------|
| Application Servers | 6.7M RPS | 20M RPS | Horizontal |
| WebSocket Servers | 10M connections | 50M connections | Horizontal |
| PostgreSQL | 70K IOPS | 200K IOPS | Vertical + Sharding |
| Cassandra | 1.7M writes/sec | 10M writes/sec | Horizontal |
| Redis | 9 TB | 50 TB | Horizontal |

### Bottlenecks

1. **Database Write Throughput**
   - Current: 1.7M version saves/sec
   - Solution: Cassandra horizontal scaling

2. **Network Bandwidth**
   - Current: 228 Gbps peak
   - Solution: Multi-region deployment + CDN

3. **Real-time Broadcasting**
   - Current: 10M concurrent connections
   - Solution: WebSocket server scaling + Redis Pub/Sub

---

## Optimization Strategies

### 1. Caching
- **Hit Ratio:** 95% → Reduces DB load by 20x
- **Cost Savings:** $500K/month in database costs

### 2. Compression
- **Document Compression:** 50% size reduction
- **Storage Savings:** 5.5 PB → $126K/month

### 3. CDN Usage
- **Offload:** 70% of read traffic
- **Bandwidth Savings:** 140 PB/month → $12.6M/month

### 4. Database Sharding
- **Shards:** 100 (by document_id hash)
- **Per-shard Load:** 67 RPS
- **Scalability:** Linear scaling to 1B+ documents

### 5. Async Processing
- **Version Saves:** Batched every 3 seconds
- **Throughput Increase:** 3x
- **Latency Reduction:** 50%

---

## Growth Projections

### Year 1
- **Users:** 1B → 1.5B (50% growth)
- **Documents:** 5B → 8B (60% growth)
- **Cost:** $33M/month → $50M/month

### Year 2
- **Users:** 1.5B → 2B (33% growth)
- **Documents:** 8B → 12B (50% growth)
- **Cost:** $50M/month → $70M/month

### Year 3
- **Users:** 2B → 2.5B (25% growth)
- **Documents:** 12B → 16B (33% growth)
- **Cost:** $70M/month → $90M/month

---

## Conclusion

The Google Docs system is designed to handle:
- **1 Billion users** with 100M daily active users
- **5 Billion documents** with 10 PB of version history
- **6.7M requests/second** at peak load
- **10M concurrent WebSocket connections**
- **228 Gbps peak bandwidth**

The architecture scales horizontally across all components and can grow to support 10x current load with proper infrastructure provisioning. The estimated monthly cost of $33M translates to $0.11 per monthly active user, which is sustainable for a freemium model with premium subscriptions.

Key cost drivers:
1. **Network bandwidth (67%)**: Mitigated by CDN and compression
2. **Storage (27%)**: Optimized with compression and tiered storage
3. **Compute (6%)**: Efficient with auto-scaling

The system achieves 99.99% availability with multi-region deployment and comprehensive disaster recovery procedures.
