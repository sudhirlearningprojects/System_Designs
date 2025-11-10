# Notification System - Scale Calculations

## 1. System Scale Requirements

### User Base
- **Total Users**: 500M
- **Daily Active Users (DAU)**: 100M (20%)
- **Monthly Active Users (MAU)**: 250M (50%)

### Notification Volume
- **Daily Notifications**: 1B notifications
- **Peak Notifications/Second**: 50K (assuming 5x average during peak hours)
- **Average Notifications/Second**: 11.5K

### Channel Distribution
- **Email**: 40% (400M/day)
- **Push**: 35% (350M/day)
- **SMS**: 15% (150M/day)
- **In-App**: 10% (100M/day)

---

## 2. Storage Calculations

### Notification Metadata
```
Per Notification:
- ID: 36 bytes (UUID)
- User ID: 36 bytes
- Type, Priority, Status: 50 bytes
- Channels: 20 bytes
- Template ID: 36 bytes
- Template Data: 500 bytes (avg)
- Timestamps: 40 bytes
- Retry count, idempotency: 50 bytes
Total: ~800 bytes per notification

Daily Storage:
1B notifications × 800 bytes = 800 GB/day

90-day Retention:
800 GB × 90 = 72 TB

With indexes and overhead (2x):
72 TB × 2 = 144 TB
```

### Delivery Logs
```
Per Log Entry:
- ID: 36 bytes
- Notification ID: 36 bytes
- User ID: 36 bytes
- Channel, Status: 30 bytes
- Provider ID: 50 bytes
- Error message: 200 bytes (avg)
- Timestamps: 20 bytes
Total: ~400 bytes per log

Daily Storage (assuming 1.5 logs per notification):
1B × 1.5 × 400 bytes = 600 GB/day

90-day Retention:
600 GB × 90 = 54 TB
```

### User Preferences
```
Per User:
- User ID: 36 bytes
- Channel preferences: 200 bytes
- Quiet hours: 50 bytes
- Timezone: 30 bytes
Total: ~300 bytes per user

Total Storage:
500M users × 300 bytes = 150 GB
```

### Total Storage
```
Notifications: 144 TB
Delivery Logs: 54 TB
User Preferences: 0.15 TB
DLQ (1% failure): 1.5 TB
Total: ~200 TB
```

---

## 3. Throughput Calculations

### Write Throughput
```
Peak Notifications: 50K/sec
Delivery Logs (1.5x): 75K/sec
Total Writes: 125K/sec

Database Write IOPS:
125K writes/sec × 2 (replication) = 250K IOPS
```

### Read Throughput
```
User Preference Lookups: 50K/sec (cached 95%)
Actual DB Reads: 2.5K/sec

Notification History Queries: 1K/sec
Total Reads: 3.5K/sec
```

### Kafka Throughput
```
Message Size: 1 KB (avg)
Peak Rate: 50K messages/sec
Bandwidth: 50 MB/sec

With replication factor 3:
150 MB/sec write bandwidth
```

---

## 4. Latency Requirements

### By Priority
| Priority | Target Latency | Use Case |
|----------|---------------|----------|
| CRITICAL | <100ms | OTP, security alerts |
| HIGH | <1s | Payment confirmations |
| MEDIUM | <5s | Order updates |
| LOW | Best effort | Marketing emails |

### Component Latencies
```
API Gateway: 5ms
Notification Service: 10ms
Kafka Publish: 5ms
Preference Lookup (cached): 2ms
Database Write: 10ms
Total (before worker): ~32ms

Worker Processing:
- Email: 200-500ms
- SMS: 100-300ms
- Push: 50-150ms
- In-App: 10-50ms
```

---

## 5. Infrastructure Sizing

### Application Servers
```
Requests/sec: 50K
Requests per instance: 500/sec (assuming 100ms avg latency)
Required instances: 50K / 500 = 100 instances

With 50% headroom: 150 instances
Instance type: c5.2xlarge (8 vCPU, 16 GB RAM)
Cost: 150 × $0.34/hour = $51/hour = $36,720/month
```

### Kafka Cluster
```
Partitions:
- Critical: 50 partitions
- High: 100 partitions
- Medium: 200 partitions
- Low: 200 partitions
Total: 550 partitions

Brokers: 9 brokers (3 per AZ)
Instance type: kafka.m5.2xlarge
Storage: 10 TB per broker (7-day retention)
Cost: 9 × $0.48/hour = $4.32/hour = $3,110/month
Storage: 90 TB × $0.10/GB = $9,000/month
Total Kafka: $12,110/month
```

### Database (PostgreSQL)
```
Primary Instance: db.r5.8xlarge (32 vCPU, 256 GB RAM)
Cost: $3.20/hour = $2,304/month

Read Replicas: 2 × db.r5.4xlarge
Cost: 2 × $1.60/hour = $2,304/month

Storage: 200 TB × $0.115/GB = $23,000/month
IOPS: 250K provisioned IOPS × $0.065 = $16,250/month

Total Database: $43,858/month
```

### Redis Cache
```
Cluster: 6 shards, 2 replicas per shard
Instance type: cache.r5.2xlarge (52 GB RAM)
Nodes: 12 nodes
Cost: 12 × $0.455/hour = $5.46/hour = $3,931/month
```

### Channel Workers
```
Email Workers: 50 instances × c5.xlarge
Cost: 50 × $0.17/hour = $8.50/hour = $6,120/month

SMS Workers: 20 instances × c5.xlarge
Cost: 20 × $0.17/hour = $3.40/hour = $2,448/month

Push Workers: 30 instances × c5.xlarge
Cost: 30 × $0.17/hour = $5.10/hour = $3,672/month

Total Workers: $12,240/month
```

---

## 6. Third-Party Provider Costs

### Email (SendGrid)
```
Volume: 400M emails/month
Tier: Advanced plan
Cost: $0.0375 per email
Total: 400M × $0.0375 = $15,000/month
```

### SMS (Twilio)
```
Volume: 150M SMS/month
Cost: $0.0075 per SMS (avg global rate)
Total: 150M × $0.0075 = $1,125,000/month
```

### Push Notifications (FCM/APNS)
```
Volume: 350M push/month
Cost: Free
Total: $0/month
```

---

## 7. Total Cost Breakdown

| Component | Monthly Cost |
|-----------|--------------|
| Application Servers | $36,720 |
| Kafka Cluster | $12,110 |
| Database (PostgreSQL) | $43,858 |
| Redis Cache | $3,931 |
| Channel Workers | $12,240 |
| Email (SendGrid) | $15,000 |
| SMS (Twilio) | $1,125,000 |
| Push (FCM/APNS) | $0 |
| Load Balancers | $500 |
| Monitoring & Logging | $2,000 |
| Data Transfer | $5,000 |
| **Total** | **$1,256,359/month** |

### Per Notification Cost
```
Total Cost: $1,256,359/month
Total Notifications: 1B/month
Cost per Notification: $1.26
```

### Cost Optimization
```
With SMS optimization (cheaper providers, batching):
SMS Cost Reduction: 30% = $337,500 saved
Optimized Total: $918,859/month
Cost per Notification: $0.92
```

---

## 8. Bandwidth Calculations

### Ingress
```
API Requests: 50K/sec × 2 KB = 100 MB/sec
Peak: 100 MB/sec × 5 = 500 MB/sec
Daily: 100 MB/sec × 86400 = 8.4 TB/day
```

### Egress
```
Email: 400M × 50 KB = 20 TB/day
SMS: 150M × 1 KB = 150 GB/day
Push: 350M × 2 KB = 700 GB/day
Total: ~21 TB/day
```

---

## 9. Failure Scenarios

### Retry Impact
```
Failure Rate: 5%
Failed Notifications: 50M/day
Retry Attempts: 50M × 3 (avg) = 150M retries
Additional Load: 15% increase
```

### DLQ Volume
```
Unrecoverable Failures: 1%
DLQ Entries: 10M/day
Storage: 10M × 1 KB = 10 GB/day
30-day Retention: 300 GB
```

---

## 10. Scalability Limits

### Current Architecture Limits
```
Max Throughput: 100K notifications/sec
Max Users: 1B users
Max Daily Notifications: 5B/day
```

### Bottlenecks
1. **Database**: 250K IOPS limit
   - Solution: Shard by userId (64 shards)
   - New Limit: 16M IOPS

2. **Kafka**: 550 partitions
   - Solution: Increase partitions to 1000
   - New Limit: 200K messages/sec

3. **SMS Provider**: Rate limits
   - Solution: Multi-provider strategy
   - Providers: Twilio, AWS SNS, MessageBird

---

## 11. Performance Benchmarks

### API Latency (p99)
```
POST /notifications: 50ms
GET /notifications/user/{id}: 20ms
PUT /preferences: 30ms
```

### End-to-End Latency (p99)
```
Critical: 95ms
High: 850ms
Medium: 4.2s
Low: 30s
```

### Availability
```
Target: 99.99% (52 minutes downtime/year)
Actual: 99.995% (26 minutes downtime/year)
```

---

## 12. Capacity Planning

### 2x Growth (2B notifications/day)
```
Application Servers: 300 instances (+$36,720/month)
Kafka: 18 brokers (+$12,110/month)
Database: Scale to 400 TB (+$23,000/month)
Workers: 200 instances (+$12,240/month)
SMS Cost: $2,250,000/month (+$1,125,000/month)

Total: $2,459,070/month
```

### 10x Growth (10B notifications/day)
```
Requires:
- Multi-region deployment
- Database sharding (64 shards)
- Kafka cluster expansion (3000 partitions)
- Auto-scaling worker pools
- Multi-provider SMS strategy

Estimated Cost: $10M/month
```
