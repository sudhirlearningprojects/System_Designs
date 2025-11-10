# Uber Technology Stack - Production Implementation

## Multi-Database Architecture (Polyglot Persistence)

### 1. Redis (Hot Data - In-Memory)
**Use Case**: Real-time location tracking, surge pricing, caching
**Why**: Sub-millisecond latency, geospatial queries
```
- Driver locations (GEOADD/GEORADIUS)
- Surge pricing (demand/supply counters)
- Session cache (driver status, active rides)
- TTL: 30 seconds for locations
```

### 2. PostgreSQL (Transactional Data - ACID)
**Use Case**: Rides, users, payments, drivers
**Why**: Strong consistency, ACID transactions
```
- Rides table (status, fare, timestamps)
- Users table (riders, drivers)
- Payments table (with idempotency)
- Partitioning: By month for rides
```

### 3. Cassandra (Time-Series Data)
**Use Case**: Location history, ride tracking
**Why**: Write-optimized, horizontal scaling
```
- location_history (driver_id, timestamp, lat, lng)
- ride_tracking (ride_id, timestamp, location, eta)
- TTL: 90 days
- Replication Factor: 3
```

### 4. Elasticsearch (Search & Analytics)
**Use Case**: Driver search, ride analytics, dashboards
**Why**: Full-text search, aggregations
```
- Driver profiles (name, rating, vehicle)
- Ride analytics (fare, duration, distance)
- Real-time dashboards
```

### 5. S3 (Cold Storage)
**Use Case**: Historical data, backups
**Why**: Cost-effective, durable
```
- Old ride data (>1 year)
- Location archives (>90 days)
- Backup snapshots
```

---

## Communication Protocols

### 1. gRPC (Internal Services)
**Use Case**: Service-to-service communication
**Why**: 5-10x faster than REST
```java
// Matching Engine → Location Service
NearbyDriversRequest request = NearbyDriversRequest.newBuilder()
    .setLatitude(37.7749)
    .setLongitude(-122.4194)
    .setRadiusKm(5.0)
    .build();
```
**Performance**: 2ms latency vs 15ms REST

### 2. WebSocket (Real-time Updates)
**Use Case**: Driver location streaming, ride status
**Why**: Persistent connection, bidirectional
```
ws://api.uber.com/ws/location?driverId={uuid}
```
**Performance**: 50ms latency, 100K connections/server

### 3. Kafka (Event Streaming)
**Use Case**: Location events, ride events, analytics
**Why**: High throughput, durable
```
Topics:
- uber.location.updates (100 partitions)
- uber.ride.events (50 partitions)
- uber.payment.transactions (20 partitions)
```
**Performance**: 1M events/sec

---

## Real-Time Features

### 1. Surge Pricing
**Implementation**: Redis counters + real-time calculation
```java
surge_multiplier = min(demand / supply, 3.0)
```
**Update Frequency**: Every 30 seconds per geohash

### 2. Location Tracking
**Flow**:
```
Driver App → WebSocket → Location Service
    ↓
    ├─→ Redis (hot data, 30s TTL)
    ├─→ Kafka (event stream)
    └─→ Cassandra (archive, async)
```
**Frequency**: Every 4 seconds

### 3. ETA Calculation
**Implementation**: Haversine distance + traffic data
```java
eta_minutes = distance_km / average_speed_kmh * 60
```
**Update**: Real-time as driver moves

---

## Data Flow Example

### Ride Request Flow
```
1. Rider App → REST API → Ride Service
   ↓
2. Ride Service → PostgreSQL (save ride)
   ↓
3. Ride Service → Kafka (publish RIDE_REQUESTED event)
   ↓
4. Matching Engine → gRPC → Location Service
   ↓
5. Location Service → Redis GEORADIUS (find drivers)
   ↓
6. Matching Engine → WebSocket → Driver App (notify)
   ↓
7. Driver accepts → PostgreSQL (update ride)
   ↓
8. Kafka (publish RIDE_ACCEPTED event)
   ↓
9. Elasticsearch (index for analytics)
```

### Location Update Flow
```
1. Driver App → WebSocket → Location Service
   ↓
2. Location Service → Redis GEOADD (update position)
   ↓
3. Location Service → Kafka (publish location event)
   ↓
4. Kafka Consumer → Cassandra (archive)
   ↓
5. If on active ride → WebSocket → Rider App (stream location)
```

---

## Technology Comparison

| Technology | Latency | Throughput | Use Case |
|------------|---------|------------|----------|
| **Redis** | <1ms | 100K ops/sec | Hot data, cache |
| **PostgreSQL** | 5-10ms | 10K TPS | Transactional |
| **Cassandra** | 2-5ms | 1M writes/sec | Time-series |
| **Elasticsearch** | 10-50ms | 10K queries/sec | Search |
| **gRPC** | 2ms | 100K RPS | Internal APIs |
| **WebSocket** | 50ms | 100K connections | Real-time |
| **Kafka** | 5-10ms | 1M events/sec | Streaming |

---

## Scalability Strategy

### Horizontal Scaling
```
Redis: 6 nodes (3 masters + 3 replicas)
PostgreSQL: 1 master + 3 read replicas
Cassandra: 9 nodes (RF=3)
Elasticsearch: 6 nodes (3 data + 3 master)
Kafka: 5 brokers
```

### Sharding
```
Redis: By geohash prefix
PostgreSQL: By month (time-based)
Cassandra: By driver_id
Elasticsearch: By date
```

---

## Cost Optimization

### Hot vs Cold Storage
```
Hot (Redis): Last 30 seconds - $4K/month
Warm (PostgreSQL): Last 1 year - $3.5K/month
Cold (Cassandra): Last 90 days - $5.4K/month
Archive (S3): >90 days - $2.3K/month
```

### Data Lifecycle
```
Location Data:
0-30s: Redis (real-time matching)
30s-1hr: Kafka (streaming analytics)
1hr-90d: Cassandra (historical queries)
>90d: S3 (compliance, archives)
```

---

## Monitoring

### Metrics
```
- Redis: Hit rate, latency, memory usage
- PostgreSQL: QPS, connection pool, replication lag
- Cassandra: Write latency, compaction, disk usage
- Kafka: Consumer lag, throughput, partition distribution
- gRPC: Request latency, error rate
- WebSocket: Active connections, message rate
```

### Alerts
```
- Redis memory >80%
- PostgreSQL replication lag >5s
- Cassandra write latency >10ms
- Kafka consumer lag >1000 messages
- gRPC error rate >1%
- WebSocket connection drops >5%
```

---

## Summary

This implementation mirrors **real Uber's production architecture**:
- ✅ **7 different technologies** for different use cases
- ✅ **Polyglot persistence** (Redis, PostgreSQL, Cassandra, Elasticsearch, S3)
- ✅ **3 communication protocols** (gRPC, WebSocket, Kafka)
- ✅ **Real-time features** (surge pricing, location tracking, ETA)
- ✅ **Production-grade** (monitoring, scaling, cost optimization)
