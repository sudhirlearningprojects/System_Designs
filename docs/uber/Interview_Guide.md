# Uber System Design - Interview Guide

## Key Talking Points for Production-Grade Answer

### 1. Communication Protocols (Critical Differentiator)

**Question**: "How do services communicate in your Uber system?"

**Answer**:
> "We use **three different protocols** optimized for specific use cases:
> 
> 1. **gRPC for internal microservices** - Matching Engine ↔ Geo-Location Service
>    - 5-10x lower latency than REST (2ms vs 15ms)
>    - Binary Protocol Buffers are 4x smaller than JSON
>    - Type-safe contracts prevent runtime errors
>    - Bidirectional streaming for real-time updates
> 
> 2. **WebSocket for client communication** - Driver/Rider apps
>    - Persistent connections eliminate HTTP handshake overhead
>    - Bidirectional streaming allows server push
>    - 250x less overhead than HTTP polling
>    - Handles 100K concurrent connections per server
> 
> 3. **Kafka for event streaming** - Analytics, audit logs
>    - 1M events/sec throughput
>    - Durable storage with replication
>    - Decouples services for async processing
>    - Enables real-time analytics and ML pipelines"

**Why This Matters**: Most candidates only mention REST APIs. Discussing gRPC shows production-level thinking.

---

### 2. Geo-Location Deep Dive

**Question**: "How do you find the nearest 10 drivers within 5km with 100K active drivers?"

**Answer**:
> "We use a **multi-layered approach**:
> 
> 1. **Geohash Sharding** (Level 3, ~1km² cells)
>    - Reduces search space from 100K to ~50 drivers per cell
>    - Enables horizontal sharding by geography
>    - Supports hierarchical queries (expand to neighboring cells)
> 
> 2. **Redis Geospatial Index**
>    - GEOADD to store driver locations: O(log N)
>    - GEORADIUS to query nearby drivers: O(N+log M)
>    - <10ms latency for 1000 drivers per cell
>    - Memory: ~100 bytes per driver × 100K = 10MB
> 
> 3. **Matching Algorithm**
>    ```
>    score = 0.6 × (1/distance) + 0.3 × rating + 0.1 × experience
>    ```
>    - Multi-factor scoring for best driver selection
>    - Fallback: Expand radius from 5km to 10km if insufficient drivers
>    - Timeout: 30 seconds for driver response
> 
> **Performance**: <100ms p99 latency for matching"

---

### 3. Scalability Strategy

**Question**: "How does your system scale to 10M concurrent users?"

**Answer**:
> "**Horizontal scaling at every layer**:
> 
> 1. **Stateless Services** (Auto-scale on CPU/Memory)
>    - Ride Service: 20-100 instances
>    - Location Service: 50-200 instances (WebSocket)
>    - Matching Engine: 20-50 instances (CPU intensive)
> 
> 2. **Database Sharding**
>    - PostgreSQL: Partition rides by month (time-based)
>    - Redis: Shard by geohash prefix (6 nodes, 3 masters + 3 replicas)
>    - Cassandra: Partition by driver_id (9 nodes, RF=3)
> 
> 3. **Caching Strategy**
>    - L1 (Caffeine): Pricing rules (5 min TTL)
>    - L2 (Redis): Driver locations (30 sec TTL)
>    - L3 (CDN): Static assets
> 
> 4. **Load Balancing**
>    - API Gateway: AWS ALB with path-based routing
>    - WebSocket: Sticky sessions with consistent hashing
>    - gRPC: Client-side load balancing with health checks
> 
> **Result**: 75K location updates/sec, 520 ride requests/sec"

---

### 4. Real-time Location Tracking

**Question**: "How do you handle 75K location updates per second?"

**Answer**:
> "**Multi-stage pipeline**:
> 
> 1. **Ingestion** (WebSocket)
>    - Driver sends location every 4 seconds
>    - 100K drivers × (1/4 sec) = 25K updates/sec
>    - WebSocket maintains persistent connection
> 
> 2. **Hot Storage** (Redis)
>    - GEOADD updates driver position: 1ms
>    - HSET stores metadata (status, timestamp): 1ms
>    - TTL: 30 seconds (auto-expire stale data)
> 
> 3. **Event Streaming** (Kafka)
>    - Publish to `uber.location.updates` topic
>    - 100 partitions for parallel processing
>    - Batch size: 100 updates, linger: 10ms
> 
> 4. **Cold Storage** (Cassandra)
>    - Async consumer writes to time-series table
>    - Partition by driver_id, cluster by timestamp
>    - TTL: 90 days, compression: LZ4
> 
> 5. **Real-time Broadcast** (WebSocket)
>    - If driver on active ride, push to rider app
>    - Update frequency: 2 seconds
> 
> **Total Latency**: 55ms p99"

---

### 5. Fault Tolerance

**Question**: "What happens if Redis goes down?"

**Answer**:
> "**Multi-layer resilience**:
> 
> 1. **Redis Cluster** (6 nodes: 3 masters + 3 replicas)
>    - Automatic failover in <5 seconds
>    - Sentinel monitors health and promotes replica
>    - Client library handles reconnection
> 
> 2. **Circuit Breaker Pattern**
>    - If Redis unavailable, fallback to PostgreSQL
>    - Degraded mode: Query drivers from DB (slower but functional)
>    - Auto-recovery when Redis comes back
> 
> 3. **Multi-Region Deployment**
>    - Active-Active in US-East, US-West, EU-West, AP-South
>    - Route users to nearest healthy region
>    - Cross-region replication for critical data
> 
> 4. **Graceful Degradation**
>    - Increase matching radius (5km → 10km)
>    - Relax matching criteria (accept lower-rated drivers)
>    - Queue requests if all regions overloaded
> 
> **RTO**: 15 minutes, **RPO**: 5 minutes"

---

### 6. Data Consistency

**Question**: "How do you ensure payment consistency?"

**Answer**:
> "**Strong consistency for payments, eventual for location**:
> 
> 1. **Payment Transactions** (PostgreSQL ACID)
>    - Pessimistic locking: `SELECT FOR UPDATE`
>    - Two-phase commit for distributed transactions
>    - Idempotency key prevents duplicate charges
>    - Saga pattern for rollback on failure
> 
> 2. **Ride Status** (PostgreSQL)
>    - State machine: REQUESTED → ACCEPTED → STARTED → COMPLETED
>    - Optimistic locking with version field
>    - Retry with exponential backoff on conflict
> 
> 3. **Location Data** (Redis + Cassandra)
>    - Eventual consistency (acceptable for location)
>    - Last-write-wins conflict resolution
>    - Cassandra: Tunable consistency (QUORUM for reads)
> 
> **Trade-off**: Strong consistency for money, eventual for location (performance vs correctness)"

---

### 7. Cost Optimization

**Question**: "What's the monthly cost to run this system?"

**Answer**:
> "**~$170K/month for 10M concurrent users**:
> 
> | Component | Cost | Optimization |
> |-----------|------|--------------|
> | EC2 (200 instances) | $24K | Spot instances for non-critical |
> | RDS PostgreSQL | $3.5K | Read replicas for scaling |
> | ElastiCache Redis | $4.2K | Reserved instances (40% savings) |
> | Cassandra (9 nodes) | $5.4K | i3 instances with local SSD |
> | Data Transfer | $45K | CloudFront CDN caching |
> | S3 Storage | $2.3K | Lifecycle policies (S3 → Glacier) |
> 
> **Revenue**: 15M rides/day × $2 commission = $900M/month
> **Profit Margin**: 99.98%
> 
> **Optimizations**:
> - Compress Kafka messages (LZ4): 50% storage savings
> - Cache pricing rules: 90% Redis hit rate
> - Archive old rides to S3: 80% cost reduction"

---

### 8. Monitoring & Observability

**Question**: "How do you monitor system health?"

**Answer**:
> "**Three-pillar observability**:
> 
> 1. **Metrics** (Prometheus + Grafana)
>    - Request latency (p50, p95, p99)
>    - Ride matching success rate (target: >95%)
>    - Driver availability rate (target: >80%)
>    - Payment success rate (target: >99.9%)
> 
> 2. **Logging** (ELK Stack)
>    - Centralized logging with correlation IDs
>    - Structured JSON logs for easy parsing
>    - Log levels: ERROR (immediate alert), WARN (investigate)
> 
> 3. **Tracing** (Jaeger)
>    - Distributed tracing across microservices
>    - Trace ride request: API → Matching → Geo → Redis
>    - Identify bottlenecks and slow queries
> 
> **Alerts**:
> - High latency (>2s for matching) → Page on-call
> - Low driver availability (<10%) → Increase incentives
> - Payment failures (>5%) → Investigate PSP
> - Service downtime → Auto-failover to backup region"

---

## Common Follow-up Questions

### Q: "Why not use REST instead of gRPC?"

**A**: "REST is simpler but gRPC provides:
- 5-10x lower latency (critical for matching)
- 4x smaller payload (reduces bandwidth costs)
- Type safety (prevents runtime errors)
- Bidirectional streaming (real-time updates)

For external APIs (mobile apps), we use REST for simplicity. For internal high-frequency calls, gRPC is essential."

### Q: "How do you handle surge pricing?"

**A**: "Real-time demand/supply calculation:
```
surge_multiplier = min(demand / supply, 3.0)
demand = active ride requests in geohash
supply = available drivers in geohash
```
Updated every 30 seconds, cached in Redis, published to Kafka for analytics."

### Q: "What if a driver goes offline mid-ride?"

**A**: "Graceful handling:
1. WebSocket disconnect detected (heartbeat timeout)
2. Mark driver as OFFLINE in Redis
3. If on active ride, send alert to rider
4. Offer to find replacement driver
5. Log incident for investigation
6. Automatic refund if ride cancelled"

---

## Summary: Production-Grade Differentiators

✅ **gRPC for internal services** (not just REST)
✅ **WebSocket for real-time** (not HTTP polling)
✅ **Kafka for event streaming** (not just database)
✅ **Geohash sharding** (not brute-force search)
✅ **Multi-layer caching** (L1/L2/L3)
✅ **Circuit breaker pattern** (fault tolerance)
✅ **Multi-region deployment** (high availability)
✅ **Cost optimization** (spot instances, compression)

**Key Message**: "This architecture mirrors Uber's actual production system, using gRPC for low-latency internal communication, WebSocket for real-time client updates, and Kafka for event streaming - handling 10M concurrent users with 99.99% availability."
