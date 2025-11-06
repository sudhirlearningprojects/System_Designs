# Rate Limiter - Scale Calculations

## 1. Traffic Assumptions

### 1.1 Request Volume
- **Peak Traffic**: 1M requests/second
- **Average Traffic**: 300K requests/second
- **Daily Requests**: 25.9B requests/day
- **Active APIs**: 10,000 different endpoints
- **Active Users**: 100M daily active users

### 1.2 Rate Limit Rules
- **Total Rules**: 1,000 active rules
- **Rule Complexity**: Average 5 conditions per rule
- **Rule Updates**: 100 updates/day
- **Cache Hit Rate**: 95% for rule lookups

## 2. Storage Requirements

### 2.1 PostgreSQL (Metadata)

#### Rate Limit Rules
```
Columns: id(8) + ruleKey(100) + algorithm(20) + requests(4) + 
         window(4) + scope(20) + priority(4) + enabled(1) + 
         timestamps(16) = ~177 bytes per rule

Total Rules: 1,000 rules × 177 bytes = 177 KB
With indexes: 177 KB × 3 = 531 KB
```

#### Rate Limit Attempts (Analytics)
```
Columns: id(8) + clientId(50) + ruleKey(100) + ip(15) + 
         userAgent(200) + apiKey(50) + allowed(1) + 
         remaining(4) + resetTime(8) + timestamp(8) = ~444 bytes per attempt

Daily Attempts: 25.9B × 444 bytes = 11.5 TB/day
Weekly Retention: 11.5 TB × 7 = 80.5 TB
With indexes: 80.5 TB × 2 = 161 TB
```

#### Total PostgreSQL Storage
- **Metadata**: ~1 MB
- **Analytics (7 days)**: ~161 TB
- **Total**: ~161 TB

### 2.2 Redis (Active State)

#### Sliding Window Data
```
Per Client Key: ~50 bytes (key) + 8 bytes × requests_in_window
Average Window: 60 seconds
Average Requests: 10 requests/window per client

Storage per client: 50 + (8 × 10) = 130 bytes
Active clients: 10M concurrent
Total: 10M × 130 bytes = 1.3 GB
```

#### Token Bucket Data
```
Per Client Key: ~50 bytes (key) + 16 bytes (tokens:timestamp)
Storage per client: 66 bytes
Active clients: 5M concurrent
Total: 5M × 66 bytes = 330 MB
```

#### Rule Cache
```
Rules: 1,000 × 200 bytes = 200 KB
TTL: 5 minutes
```

#### Total Redis Storage
- **Sliding Window**: 1.3 GB
- **Token Bucket**: 330 MB
- **Rule Cache**: 200 KB
- **Total**: ~1.63 GB per Redis instance

## 3. Compute Requirements

### 3.1 Rate Limiter Service

#### CPU Usage per Request
```
Rule Lookup: 0.1ms CPU
Redis Operation: 0.2ms CPU
Logging: 0.05ms CPU
Total: 0.35ms CPU per request
```

#### Instance Sizing
```
Target: 1M requests/second
CPU per request: 0.35ms
Total CPU needed: 1M × 0.35ms = 350 CPU seconds/second = 350 cores

With 80% utilization: 350 ÷ 0.8 = 438 cores
Instance type: 16-core instances
Instances needed: 438 ÷ 16 = 28 instances
```

#### Memory per Instance
```
JVM Heap: 4 GB
Application: 2 GB
OS: 1 GB
Buffer: 1 GB
Total: 8 GB per instance
```

### 3.2 Redis Cluster

#### Memory Requirements
```
Data: 1.63 GB
Replication: 1.63 GB × 2 = 3.26 GB (with replica)
Overhead: 3.26 GB × 0.3 = 0.98 GB
Total: 4.24 GB per shard
```

#### Throughput Requirements
```
Peak Operations: 1M requests/second
Operations per request: 2 (read + write)
Total ops: 2M ops/second

Redis capacity: 100K ops/second per core
Cores needed: 2M ÷ 100K = 20 cores
Instances: 3 masters + 3 replicas = 6 instances
Cores per instance: 20 ÷ 3 = 7 cores (masters only)
```

### 3.3 PostgreSQL

#### Write Load (Analytics)
```
Attempts: 25.9B/day = 300K writes/second
Batch size: 1000 records
Batches: 300 batches/second
```

#### Instance Sizing
```
CPU: 16 cores (for write-heavy workload)
Memory: 64 GB (large buffer pool)
Storage: 200 TB SSD (with growth buffer)
IOPS: 50K IOPS for write performance
```

## 4. Network Requirements

### 4.1 Bandwidth Calculations

#### Inbound Traffic
```
Request size: 1 KB average
Peak requests: 1M/second
Inbound: 1M × 1 KB = 1 GB/second = 8 Gbps
```

#### Outbound Traffic
```
Response size: 0.5 KB average (headers + small body)
Peak responses: 1M/second
Outbound: 1M × 0.5 KB = 0.5 GB/second = 4 Gbps
```

#### Redis Traffic
```
Operations: 2M/second
Data per operation: 100 bytes average
Redis traffic: 2M × 100 bytes = 200 MB/second = 1.6 Gbps
```

#### Total Network
- **Public**: 12 Gbps (8 Gbps in + 4 Gbps out)
- **Internal**: 1.6 Gbps (Redis cluster)

## 5. Cost Analysis (AWS)

### 5.1 Compute Costs

#### Rate Limiter Instances
```
Instance type: c5.4xlarge (16 vCPU, 32 GB RAM)
Count: 28 instances
Cost: $0.68/hour × 28 × 24 × 30 = $13,651/month
```

#### Redis Cluster
```
Instance type: r5.2xlarge (8 vCPU, 64 GB RAM)
Count: 6 instances (3 masters + 3 replicas)
Cost: $0.504/hour × 6 × 24 × 30 = $2,177/month
```

#### PostgreSQL
```
Instance type: r5.4xlarge (16 vCPU, 128 GB RAM)
Count: 1 primary + 2 read replicas = 3 instances
Cost: $1.008/hour × 3 × 24 × 30 = $2,177/month
```

### 5.2 Storage Costs

#### PostgreSQL Storage
```
Storage: 200 TB × $0.115/GB/month = $23,000/month
IOPS: 50K × $0.065/IOPS/month = $3,250/month
```

#### Redis Storage (included in instance cost)

### 5.3 Network Costs

#### Data Transfer
```
Outbound: 0.5 GB/second × 86,400 seconds/day × 30 days = 1.3 PB/month
Cost: First 10 TB free, then $0.09/GB
Cost: (1,300 TB - 10 TB) × $0.09 = $116,100/month
```

### 5.4 Total Monthly Cost
```
Compute: $18,005/month
Storage: $26,250/month
Network: $116,100/month
Total: $160,355/month
```

## 6. Performance Characteristics

### 6.1 Latency Targets
- **P50**: < 1ms (Redis cache hit)
- **P95**: < 5ms (Redis + rule lookup)
- **P99**: < 10ms (worst case with DB query)
- **P99.9**: < 50ms (failover scenarios)

### 6.2 Throughput Limits
- **Per Instance**: 35K requests/second
- **Total Cluster**: 1M requests/second
- **Redis Cluster**: 2M operations/second
- **PostgreSQL**: 300K writes/second

### 6.3 Availability Targets
- **Service Availability**: 99.99% (52 minutes downtime/year)
- **Redis Availability**: 99.95% (4.4 hours downtime/year)
- **PostgreSQL Availability**: 99.9% (8.8 hours downtime/year)

## 7. Scaling Strategies

### 7.1 Horizontal Scaling
- **Rate Limiter**: Add instances behind load balancer
- **Redis**: Shard data across multiple clusters
- **PostgreSQL**: Read replicas for analytics queries

### 7.2 Vertical Scaling
- **Rate Limiter**: Increase instance size for CPU-bound workloads
- **Redis**: Increase memory for larger datasets
- **PostgreSQL**: Increase storage and IOPS for write performance

### 7.3 Geographic Scaling
- **Multi-Region**: Deploy in 3+ regions for global coverage
- **Edge Caching**: Cache rules at CDN edge locations
- **Regional Failover**: Automatic failover between regions

## 8. Optimization Opportunities

### 8.1 Cost Optimization
- **Reserved Instances**: 40% savings on compute costs
- **Spot Instances**: 70% savings for non-critical workloads
- **Data Lifecycle**: Archive old analytics data to S3 Glacier

### 8.2 Performance Optimization
- **Connection Pooling**: Reduce Redis connection overhead
- **Batch Processing**: Batch analytics writes to reduce load
- **Compression**: Compress large payloads to reduce network usage

### 8.3 Operational Optimization
- **Auto Scaling**: Scale instances based on traffic patterns
- **Monitoring**: Comprehensive metrics and alerting
- **Automation**: Infrastructure as Code for consistent deployments