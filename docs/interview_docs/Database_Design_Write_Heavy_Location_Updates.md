# Database Design for Write-Heavy Location Updates (Uber-like)

## Problem Statement
You run a ride-hailing app (like Uber). Drivers update their location every few seconds → very write-heavy workload. How do you design the database?

## Scale Requirements
- **1M active drivers** sending location updates
- **Update frequency**: Every 3-5 seconds
- **Write load**: 200K-333K writes/sec
- **Read load**: Nearby driver queries (10K/sec)
- **Data retention**: Recent locations (last 24 hours)

---

## Key Challenges

1. **Extremely high write throughput** (200K+ writes/sec)
2. **Low latency requirements** (<50ms for writes)
3. **Geo-spatial queries** (find drivers within radius)
4. **Time-series data** (location history)
5. **Hot partition problem** (popular areas)

---

## Solution Architecture

### 1. **In-Memory Store (Redis) - Primary Solution**

Redis is the BEST choice for real-time location tracking:

```
Driver App → API Gateway → Redis Geospatial → PostgreSQL (Archive)
                              ↓
                         Nearby Driver Queries
```

#### Why Redis?
- **200K+ writes/sec** capability
- **Sub-millisecond latency** (<1ms)
- **Built-in geospatial** commands (GEOADD, GEORADIUS)
- **Automatic expiry** (TTL for stale data)
- **In-memory** = blazing fast

#### Implementation

```java
@Service
public class DriverLocationService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String LOCATION_KEY = "driver:locations";
    private static final int LOCATION_TTL = 300; // 5 minutes
    
    // Update driver location (200K+ writes/sec)
    public void updateLocation(String driverId, double latitude, double longitude) {
        // Add to geospatial index
        redisTemplate.opsForGeo().add(
            LOCATION_KEY,
            new Point(longitude, latitude),
            driverId
        );
        
        // Set expiry to auto-remove stale locations
        redisTemplate.expire(LOCATION_KEY, LOCATION_TTL, TimeUnit.SECONDS);
        
        // Store additional metadata
        Map<String, String> metadata = Map.of(
            "lat", String.valueOf(latitude),
            "lng", String.valueOf(longitude),
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "status", "AVAILABLE"
        );
        redisTemplate.opsForHash().putAll("driver:" + driverId, metadata);
        redisTemplate.expire("driver:" + driverId, LOCATION_TTL, TimeUnit.SECONDS);
    }
    
    // Find nearby drivers (10K reads/sec)
    public List<DriverLocation> findNearbyDrivers(
        double latitude, 
        double longitude, 
        double radiusKm
    ) {
        GeoResults<GeoLocation<String>> results = redisTemplate.opsForGeo()
            .radius(
                LOCATION_KEY,
                new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS)),
                GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending()
                    .limit(20)
            );
        
        return results.getContent().stream()
            .map(result -> {
                String driverId = result.getContent().getName();
                Point point = result.getContent().getPoint();
                double distance = result.getDistance().getValue();
                
                return new DriverLocation(driverId, point.getX(), point.getY(), distance);
            })
            .collect(Collectors.toList());
    }
}
```

#### Redis Commands
```bash
# Add driver location
GEOADD driver:locations -122.4194 37.7749 driver123

# Find drivers within 5km
GEORADIUS driver:locations -122.4194 37.7749 5 km WITHDIST WITHCOORD ASC COUNT 20

# Get driver metadata
HGETALL driver:driver123

# Auto-expire stale locations
EXPIRE driver:locations 300
```

---

### 2. **Geo-Sharding by Region**

Partition data by geographic regions to avoid hot partitions:

```
Global Traffic
      ↓
┌─────┴─────┬─────────┬─────────┬─────────┐
↓           ↓         ↓         ↓         ↓
US-West   US-East   Europe    Asia    LatAm
Redis     Redis     Redis     Redis   Redis
Cluster   Cluster   Cluster   Cluster Cluster
```

#### Implementation

```java
@Service
public class GeoShardingService {
    
    private Map<String, RedisTemplate> regionShards = Map.of(
        "US-WEST", usWestRedis,
        "US-EAST", usEastRedis,
        "EUROPE", europeRedis,
        "ASIA", asiaRedis
    );
    
    public String getRegion(double latitude, double longitude) {
        // Simple geo-hashing or use H3 library
        if (longitude >= -125 && longitude <= -100 && latitude >= 30 && latitude <= 50) {
            return "US-WEST";
        } else if (longitude >= -100 && longitude <= -70 && latitude >= 25 && latitude <= 50) {
            return "US-EAST";
        } else if (longitude >= -10 && longitude <= 40 && latitude >= 35 && latitude <= 70) {
            return "EUROPE";
        } else if (longitude >= 60 && longitude <= 150 && latitude >= -10 && latitude <= 55) {
            return "ASIA";
        }
        return "DEFAULT";
    }
    
    public void updateLocation(String driverId, double lat, double lng) {
        String region = getRegion(lat, lng);
        RedisTemplate redis = regionShards.get(region);
        
        redis.opsForGeo().add("driver:locations:" + region, new Point(lng, lat), driverId);
    }
}
```

#### Benefits
- **Distribute load** across regions
- **Reduce latency** (data closer to users)
- **Avoid hot partitions** in busy cities
- **Independent scaling** per region

---

### 3. **Time-Series Database for History (Optional)**

For location history and analytics, use time-series DB:

```
Redis (Real-time) → Kafka → TimescaleDB/InfluxDB (History)
                      ↓
                  Analytics
```

#### Schema (TimescaleDB)

```sql
CREATE TABLE driver_location_history (
    driver_id VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    accuracy DOUBLE PRECISION
);

-- Convert to hypertable (time-series optimization)
SELECT create_hypertable('driver_location_history', 'timestamp');

-- Create index for driver queries
CREATE INDEX idx_driver_time ON driver_location_history (driver_id, timestamp DESC);

-- Auto-delete old data (retention policy)
SELECT add_retention_policy('driver_location_history', INTERVAL '7 days');
```

#### Async Write to History

```java
@Service
public class LocationHistoryService {
    
    @Autowired
    private KafkaTemplate<String, LocationEvent> kafkaTemplate;
    
    @Async
    public void archiveLocation(String driverId, double lat, double lng) {
        LocationEvent event = new LocationEvent(driverId, lat, lng, Instant.now());
        kafkaTemplate.send("location-events", driverId, event);
    }
}

@Service
public class LocationEventConsumer {
    
    @KafkaListener(topics = "location-events", groupId = "location-archiver")
    public void consumeLocationEvent(LocationEvent event) {
        // Batch insert to TimescaleDB
        jdbcTemplate.update(
            "INSERT INTO driver_location_history (driver_id, latitude, longitude, timestamp) VALUES (?, ?, ?, ?)",
            event.getDriverId(), event.getLatitude(), event.getLongitude(), event.getTimestamp()
        );
    }
}
```

---

### 4. **Write Optimization Strategies**

#### A. Batch Writes (Reduce Network Calls)

```java
@Service
public class BatchLocationService {
    
    private final BlockingQueue<LocationUpdate> buffer = new LinkedBlockingQueue<>(10000);
    
    @PostConstruct
    public void startBatchProcessor() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::flushBatch, 0, 1, TimeUnit.SECONDS);
    }
    
    public void updateLocation(String driverId, double lat, double lng) {
        buffer.offer(new LocationUpdate(driverId, lat, lng));
    }
    
    private void flushBatch() {
        List<LocationUpdate> batch = new ArrayList<>();
        buffer.drainTo(batch, 1000); // Batch size: 1000
        
        if (!batch.isEmpty()) {
            // Single Redis pipeline for all updates
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                batch.forEach(update -> {
                    connection.geoAdd(
                        "driver:locations".getBytes(),
                        new Point(update.getLng(), update.getLat()),
                        update.getDriverId().getBytes()
                    );
                });
                return null;
            });
        }
    }
}
```

#### B. Write-Behind Caching

```
Driver Update → Redis (immediate) → Async → PostgreSQL (eventual)
                  ↓ (instant response)
              200 OK
```

#### C. Sampling (Reduce Write Volume)

```java
// Only write if location changed significantly
public boolean shouldUpdate(Location old, Location newLoc) {
    double distance = calculateDistance(old, newLoc);
    return distance > 50; // 50 meters threshold
}

// Adaptive sampling based on speed
public int getUpdateInterval(double speed) {
    if (speed < 10) return 10; // 10 seconds (stationary)
    if (speed < 30) return 5;  // 5 seconds (slow)
    return 3; // 3 seconds (fast)
}
```

---

### 5. **Database Choice Comparison**

| Database | Write Throughput | Latency | Geo-Queries | Best For |
|----------|-----------------|---------|-------------|----------|
| **Redis** | 200K+ writes/sec | <1ms | ✅ GEORADIUS | Real-time tracking |
| **MongoDB** | 50K writes/sec | 5-10ms | ✅ $geoNear | Flexible schema |
| **PostgreSQL + PostGIS** | 10K writes/sec | 10-50ms | ✅ ST_DWithin | ACID compliance |
| **Cassandra** | 100K+ writes/sec | 5-10ms | ❌ (manual) | High availability |
| **TimescaleDB** | 50K writes/sec | 10-20ms | ✅ PostGIS | Time-series history |

**Winner: Redis** for real-time location tracking

---

### 6. **Complete Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                     Driver Mobile Apps                       │
│              (1M drivers × 1 update/3sec)                    │
└─────────────────────────────────────────────────────────────┘
                            ↓ (200K writes/sec)
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
│              (Load Balancer + Rate Limiting)                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  Location Service│                  │  Matching Service│
│   (Stateless)    │                  │   (Stateless)    │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
        ↓                                       ↓ (reads)
┌─────────────────────────────────────────────────────────────┐
│              Redis Cluster (Geo-Sharded)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ US-West  │  │ US-East  │  │  Europe  │  │   Asia   │   │
│  │  Shard   │  │  Shard   │  │  Shard   │  │  Shard   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│         (GEOADD, GEORADIUS, TTL expiry)                     │
└─────────────────────────────────────────────────────────────┘
        ↓ (async archival)
┌─────────────────────────────────────────────────────────────┐
│                      Kafka Stream                            │
└─────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────┐
│              TimescaleDB (Location History)                  │
│           (7-day retention, analytics queries)               │
└─────────────────────────────────────────────────────────────┘
```

---

## Performance Optimization

### 1. Connection Pooling
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 100
        max-idle: 50
        min-idle: 20
```

### 2. Redis Cluster Configuration
```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-us-west-1:6379
        - redis-us-west-2:6379
        - redis-us-east-1:6379
      max-redirects: 3
```

### 3. Async Processing
```java
@Async
public CompletableFuture<Void> updateLocationAsync(String driverId, double lat, double lng) {
    updateLocation(driverId, lat, lng);
    return CompletableFuture.completedFuture(null);
}
```

---

## Monitoring & Alerts

### Key Metrics
```java
@Component
public class LocationMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    public void recordLocationUpdate(String region, long latency) {
        registry.timer("location.update", "region", region)
            .record(latency, TimeUnit.MILLISECONDS);
    }
    
    public void recordWriteThroughput(String region) {
        registry.counter("location.writes", "region", region).increment();
    }
    
    public void recordRedisLatency(long latency) {
        registry.timer("redis.latency").record(latency, TimeUnit.MILLISECONDS);
    }
}
```

### Alerts
- Write latency p99 > 50ms
- Redis memory usage > 80%
- Write throughput drop > 20%
- Geo-query latency > 100ms
- Stale location count > 1000

---

## Cost Analysis

### Redis Cluster (AWS ElastiCache)
- **Instance**: r6g.2xlarge (52GB RAM)
- **Nodes**: 6 nodes (3 shards × 2 replicas)
- **Cost**: ~$2,500/month
- **Capacity**: 300K writes/sec

### TimescaleDB (AWS RDS)
- **Instance**: db.r6g.xlarge
- **Storage**: 1TB SSD
- **Cost**: ~$800/month
- **Retention**: 7 days

### Total: ~$3,300/month for 1M drivers

---

## Key Takeaways

1. **Use Redis for real-time location tracking** (200K+ writes/sec, <1ms latency)
2. **Geo-shard by region** to avoid hot partitions
3. **Use TTL expiry** to auto-remove stale locations
4. **Batch writes** to reduce network overhead
5. **Archive to time-series DB** for history/analytics
6. **Sample intelligently** to reduce write volume
7. **Monitor write latency** and throughput continuously

---

## Alternative: Cassandra for Extreme Scale

If Redis doesn't meet scale requirements (1B+ drivers):

```sql
CREATE TABLE driver_locations (
    driver_id TEXT,
    timestamp TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    geohash TEXT,
    PRIMARY KEY ((geohash), timestamp, driver_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Query by geohash prefix
SELECT * FROM driver_locations 
WHERE geohash LIKE '9q8yy%' 
AND timestamp > now() - 5m;
```

**Benefits:**
- **1M+ writes/sec** capability
- **Linear scalability** (add nodes)
- **Multi-datacenter** replication
- **No single point of failure**

**Drawbacks:**
- No native geo-queries (manual geohashing)
- Higher latency than Redis (5-10ms)
- More complex operations

---

## Conclusion

For Uber-like location tracking with **200K+ writes/sec**:

✅ **Primary**: Redis Cluster with geo-sharding  
✅ **Secondary**: TimescaleDB for history  
✅ **Optimization**: Batching, sampling, TTL expiry  
✅ **Latency**: <1ms for writes, <10ms for nearby queries  
✅ **Cost**: ~$3,300/month for 1M drivers
