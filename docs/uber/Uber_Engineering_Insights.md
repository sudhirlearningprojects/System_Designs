# Uber Engineering Insights - Real-World Architecture

> **Source**: Uber Engineering Blog (https://www.uber.com/blog/engineering/)

This document captures real-world architectural patterns, technologies, and best practices used by Uber at scale.

---

## Table of Contents
1. [Microservices Architecture](#microservices-architecture)
2. [Geo-Spatial Systems](#geo-spatial-systems)
3. [Real-Time Data Processing](#real-time-data-processing)
4. [Reliability & Fault Tolerance](#reliability--fault-tolerance)
5. [Machine Learning at Scale](#machine-learning-at-scale)
6. [Mobile Architecture](#mobile-architecture)
7. [Observability & Monitoring](#observability--monitoring)

---

## Microservices Architecture

### Domain-Oriented Microservices Architecture (DOMA)

Uber evolved from monolith → SOA → microservices → **DOMA** (2020+)

**Key Principles**:
- **Domain-driven design**: Services organized by business domains (Marketplace, Fulfillment, Payments)
- **Clear ownership**: Each domain has dedicated teams
- **API contracts**: gRPC with Protocol Buffers for type safety
- **Service mesh**: Envoy for traffic management, observability

**Architecture Layers**:
```
┌─────────────────────────────────────────────────────────┐
│              Presentation Layer (Mobile/Web)             │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                    API Gateway Layer                     │
│              (Rate Limiting, Auth, Routing)              │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                   Domain Services Layer                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │Marketplace│  │Fulfillment│ │ Payments │  │ Pricing │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                  Platform Services Layer                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │  Identity │  │Notification│ │ Analytics│  │ Storage │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Benefits**:
- **Reduced coupling**: 70% reduction in cross-domain dependencies
- **Faster deployments**: Independent service releases
- **Better scalability**: Scale domains independently

---

## Geo-Spatial Systems

### H3: Uber's Hexagonal Hierarchical Spatial Index

Uber uses **H3** (open-sourced by Uber) instead of traditional Geohash:

**Why H3 over Geohash?**
- **Uniform cell sizes**: Hexagons have consistent neighbor distances
- **No edge distortion**: Better for proximity queries
- **Hierarchical**: 16 resolutions from 4M km² to 0.9 m²
- **Fast**: O(1) neighbor lookup

**H3 Resolutions Used**:
```
Resolution 5: ~252 km² - City-level sharding
Resolution 7: ~5.16 km² - Neighborhood matching
Resolution 9: ~0.105 km² - Fine-grained driver search
Resolution 11: ~0.0016 km² - Precise location tracking
```

**Implementation**:
```java
// H3 Integration for Uber
public class H3GeoService {
    private final H3Core h3;
    
    public String getCellId(double lat, double lng, int resolution) {
        return h3.geoToH3Address(lat, lng, resolution);
    }
    
    public List<String> getNeighbors(String cellId) {
        return h3.kRing(cellId, 1); // Get 6 adjacent hexagons
    }
    
    public List<Driver> findNearbyDrivers(Location location) {
        // Resolution 9 for 100m radius
        String centerCell = getCellId(location.getLat(), location.getLng(), 9);
        List<String> searchCells = getNeighbors(centerCell);
        searchCells.add(centerCell);
        
        return searchCells.stream()
            .flatMap(cell -> redisTemplate.opsForSet()
                .members("drivers:h3:" + cell).stream())
            .map(this::getDriver)
            .limit(20)
            .collect(Collectors.toList());
    }
}
```

**Performance**:
- **Search space reduction**: 1000x smaller than global search
- **Query latency**: <5ms for 20 nearest drivers
- **Memory efficiency**: 8-byte cell ID vs 16-byte lat/lng

### DISCO: Uber's Dispatch System

**DISCO** (Dispatch Optimization) is Uber's core matching engine:

**Matching Algorithm**:
```
1. Spatial Filtering (H3):
   - Get rider's H3 cell (resolution 9)
   - Query 7 cells (center + 6 neighbors)
   - Filter online drivers in cells
   
2. Temporal Filtering:
   - Remove drivers on active trips
   - Remove drivers in cooldown (recently declined)
   - Check driver's last location update (<30 seconds)
   
3. Scoring Function:
   score = w1 * (1/distance) + w2 * acceptance_rate + 
           w3 * driver_rating + w4 * (1/eta)
   
   Weights (ML-optimized):
   w1 = 0.5 (distance most important)
   w2 = 0.2 (reliability)
   w3 = 0.2 (quality)
   w4 = 0.1 (speed)
   
4. Batching (for efficiency):
   - Batch 100 ride requests
   - Run matching every 2 seconds
   - Optimize global assignment (Hungarian algorithm)
   
5. Fallback Strategy:
   - If no match in 30 seconds, expand radius to 10km
   - If still no match, notify rider "No drivers available"
```

**Performance Metrics**:
- **Match rate**: 95% of rides matched within 30 seconds
- **Average ETA**: 4 minutes globally
- **Throughput**: 100K matches/second during peak

---

## Real-Time Data Processing

### Uber's Streaming Architecture

**Tech Stack**:
- **Apache Kafka**: Event backbone (1M+ events/sec)
- **Apache Flink**: Stream processing (real-time aggregations)
- **Apache Pinot**: Real-time OLAP (analytics queries)

**Data Flow**:
```
Driver App → Kafka → Flink → Pinot → Dashboard
    ↓
Location Updates (75K/sec)
    ↓
Kafka Topic: location.updates (100 partitions)
    ↓
Flink Job: Calculate ETA, detect anomalies
    ↓
Pinot: Store aggregated metrics (1-min windows)
    ↓
Grafana: Real-time dashboards
```

**Kafka Topics**:
```
uber.location.updates
  - Partitions: 100 (by driver_id hash)
  - Retention: 7 days
  - Throughput: 75K msgs/sec
  - Size: 15 MB/sec

uber.ride.events
  - Partitions: 50 (by ride_id hash)
  - Retention: 90 days
  - Throughput: 5K msgs/sec
  - Size: 1 MB/sec

uber.payment.transactions
  - Partitions: 20 (by user_id hash)
  - Retention: 7 years (compliance)
  - Throughput: 2K msgs/sec
  - Size: 500 KB/sec
```

**Flink Processing**:
```java
// Real-time ETA calculation
DataStream<LocationUpdate> locations = env
    .addSource(new FlinkKafkaConsumer<>("location.updates", schema, props))
    .keyBy(LocationUpdate::getDriverId)
    .window(TumblingEventTimeWindows.of(Time.seconds(10)))
    .process(new ETACalculator());

// Anomaly detection (driver stuck, unusual route)
locations
    .keyBy(LocationUpdate::getDriverId)
    .flatMap(new AnomalyDetector())
    .addSink(new AlertingSink());
```

### Apache Pinot for Real-Time Analytics

**Why Pinot?**
- **Sub-second queries**: 99th percentile <100ms
- **High concurrency**: 10K queries/sec
- **Real-time ingestion**: From Kafka with <1 min latency

**Use Cases**:
```sql
-- Driver earnings dashboard (real-time)
SELECT driver_id, SUM(fare) as total_earnings
FROM rides
WHERE completed_at > now() - INTERVAL '1' DAY
GROUP BY driver_id
ORDER BY total_earnings DESC
LIMIT 100;

-- Surge pricing calculation (1-min aggregation)
SELECT h3_cell, COUNT(*) as demand, 
       (SELECT COUNT(*) FROM drivers WHERE h3_cell = rides.h3_cell) as supply
FROM rides
WHERE requested_at > now() - INTERVAL '5' MINUTE
GROUP BY h3_cell
HAVING demand / supply > 2.0;
```

---

## Reliability & Fault Tolerance

### Uber's Reliability Principles

**1. Circuit Breaker Pattern**

Uber uses **Hystrix** (Netflix) for circuit breakers:

```java
@HystrixCommand(
    fallbackMethod = "fallbackPayment",
    commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
        @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "20"),
        @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
    }
)
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentGateway.charge(request);
}

public PaymentResponse fallbackPayment(PaymentRequest request) {
    // Fallback: Queue for async processing
    paymentQueue.enqueue(request);
    return PaymentResponse.pending();
}
```

**2. Retry with Exponential Backoff**

```java
public class ResilientHttpClient {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    
    public Response callWithRetry(Request request) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return httpClient.execute(request);
            } catch (TransientException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) throw e;
                
                long backoff = INITIAL_BACKOFF_MS * (1L << attempt); // 100, 200, 400ms
                Thread.sleep(backoff + random.nextInt(100)); // Add jitter
            }
        }
    }
}
```

**3. Graceful Degradation**

```java
public RideEstimate estimateFare(Location pickup, Location dropoff) {
    try {
        // Try ML-based pricing (complex, accurate)
        return mlPricingService.predict(pickup, dropoff);
    } catch (Exception e) {
        log.warn("ML pricing failed, falling back to rule-based", e);
        // Fallback to simple rule-based pricing
        return rulePricingService.calculate(pickup, dropoff);
    }
}
```

**4. Bulkhead Pattern**

Isolate thread pools for different services:

```java
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor paymentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("payment-");
        return executor;
    }
    
    @Bean
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-");
        return executor;
    }
}
```

### Multi-Region Failover

**Active-Active Architecture**:
```
US-East (Primary)     US-West (Active)     EU-West (Active)
     │                      │                     │
     ├─ PostgreSQL Master   ├─ Read Replica      ├─ Read Replica
     ├─ Redis Cluster       ├─ Redis Cluster     ├─ Redis Cluster
     └─ Kafka Cluster       └─ Kafka Mirror      └─ Kafka Mirror
```

**Failover Strategy**:
1. **Health checks**: Every 10 seconds
2. **Automatic failover**: <30 seconds
3. **Data replication**: Async (eventual consistency for non-critical data)
4. **DNS routing**: Route53 with latency-based routing

---

## Machine Learning at Scale

### Michelangelo: Uber's ML Platform

**ML Use Cases**:
1. **ETA Prediction**: XGBoost model (95% accuracy)
2. **Surge Pricing**: Real-time demand forecasting
3. **Fraud Detection**: Anomaly detection (99.9% precision)
4. **Driver Churn Prediction**: Prevent driver attrition

**ETA Prediction Model**:
```
Features (50+):
- Historical trip data (same route, time of day)
- Real-time traffic (Google Maps API)
- Weather conditions
- Driver behavior (speed, braking patterns)
- Road type (highway, city streets)

Model: XGBoost Regressor
Training: Daily on 100M trips
Inference: <10ms per prediction
Accuracy: 95% within ±2 minutes
```

**Model Serving**:
```java
@Service
public class ETAPredictionService {
    private final XGBoostModel model;
    
    public int predictETA(Location pickup, Location dropoff, Driver driver) {
        // Feature engineering
        Map<String, Double> features = extractFeatures(pickup, dropoff, driver);
        
        // Model inference (cached in memory)
        double etaMinutes = model.predict(features);
        
        // Post-processing (add buffer for safety)
        return (int) Math.ceil(etaMinutes * 1.1);
    }
    
    private Map<String, Double> extractFeatures(Location pickup, Location dropoff, Driver driver) {
        return Map.of(
            "distance_km", pickup.distanceTo(dropoff),
            "hour_of_day", LocalDateTime.now().getHour(),
            "day_of_week", LocalDateTime.now().getDayOfWeek().getValue(),
            "driver_rating", driver.getRating().doubleValue(),
            "traffic_multiplier", trafficService.getMultiplier(pickup)
        );
    }
}
```

### A/B Testing Framework

**Experimentation Platform**:
```java
@Service
public class ExperimentService {
    public boolean isInExperiment(String userId, String experimentName) {
        // Consistent hashing for stable assignment
        int hash = Hashing.murmur3_128()
            .hashString(userId + experimentName, StandardCharsets.UTF_8)
            .asInt();
        
        int bucket = Math.abs(hash % 100);
        
        Experiment exp = getExperiment(experimentName);
        return bucket < exp.getTrafficPercentage();
    }
}

// Usage
if (experimentService.isInExperiment(userId, "new_pricing_algorithm")) {
    return newPricingService.calculate(pickup, dropoff);
} else {
    return oldPricingService.calculate(pickup, dropoff);
}
```

---

## Mobile Architecture

### RIBs: Uber's Mobile Architecture

**RIBs** (Router, Interactor, Builder) - Cross-platform architecture:

**Key Principles**:
- **Business logic in Interactors**: Testable, platform-agnostic
- **View-agnostic**: Easy to change UI without touching logic
- **Dependency injection**: Dagger (Android), Needle (iOS)

**Example**:
```kotlin
// Interactor (Business Logic)
class RideRequestInteractor(
    private val rideService: RideService,
    private val locationService: LocationService
) : Interactor() {
    
    fun requestRide(vehicleType: VehicleType) {
        val pickup = locationService.getCurrentLocation()
        val request = RideRequest(pickup, vehicleType)
        
        rideService.requestRide(request)
            .subscribe(
                { ride -> router.attachRideTracking(ride) },
                { error -> view.showError(error) }
            )
    }
}

// Router (Navigation)
class RideRequestRouter : Router() {
    fun attachRideTracking(ride: Ride) {
        val rideTrackingRIB = rideTrackingBuilder.build(ride)
        attachChild(rideTrackingRIB)
    }
}
```

### Offline-First Architecture

**Strategy**:
1. **Local cache**: SQLite for critical data
2. **Sync on reconnect**: Queue mutations, replay on network
3. **Optimistic UI**: Show success immediately, rollback on failure

```kotlin
class OfflineRideService(
    private val localDb: RideDatabase,
    private val remoteApi: RideApi,
    private val syncQueue: SyncQueue
) {
    
    suspend fun requestRide(request: RideRequest): Ride {
        // Save to local DB immediately
        val localRide = localDb.insertRide(request)
        
        // Queue for sync
        syncQueue.enqueue(SyncOperation.CREATE_RIDE, request)
        
        // Try remote call
        try {
            val remoteRide = remoteApi.requestRide(request)
            localDb.updateRide(remoteRide) // Update with server ID
            return remoteRide
        } catch (e: NetworkException) {
            // Return local ride, will sync later
            return localRide
        }
    }
}
```

---

## Observability & Monitoring

### Uber's Observability Stack

**Tech Stack**:
- **Metrics**: Prometheus + M3 (Uber's time-series DB)
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: Jaeger (distributed tracing)
- **Alerting**: PagerDuty

**Key Metrics**:
```
# Golden Signals
- Latency: p50, p95, p99 response times
- Traffic: Requests per second
- Errors: Error rate (%)
- Saturation: CPU, memory, disk usage

# Business Metrics
- Rides per minute
- Match rate (%)
- Average ETA accuracy
- Payment success rate
- Driver utilization (%)
```

**Distributed Tracing**:
```java
@Service
public class RideService {
    @Traced(operationName = "request_ride")
    public Ride requestRide(RideRequest request) {
        Span span = tracer.activeSpan();
        span.setTag("rider_id", request.getRiderId());
        span.setTag("vehicle_type", request.getVehicleType());
        
        try {
            Driver driver = matchingService.findDriver(request);
            span.setTag("driver_id", driver.getId());
            span.setTag("match_time_ms", System.currentTimeMillis() - startTime);
            
            return createRide(request, driver);
        } catch (Exception e) {
            span.setTag("error", true);
            span.log(Map.of("event", "error", "message", e.getMessage()));
            throw e;
        }
    }
}
```

**Alerting Rules**:
```yaml
# High latency alert
- alert: HighMatchingLatency
  expr: histogram_quantile(0.99, matching_duration_seconds) > 2
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Matching latency p99 > 2s"

# Low match rate alert
- alert: LowMatchRate
  expr: (matched_rides / total_ride_requests) < 0.90
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Match rate dropped below 90%"
```

---

## Performance Optimizations

### Connection Pooling

```java
@Configuration
public class DatabaseConfig {
    @Bean
    public HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/uber");
        config.setMaximumPoolSize(50); // Max connections
        config.setMinimumIdle(10);     // Min idle connections
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000);      // 10 minutes
        config.setMaxLifetime(1800000);     // 30 minutes
        return new HikariDataSource(config);
    }
}
```

### Caching Strategy

**Multi-Layer Cache**:
```
L1: Application Cache (Caffeine) - 1 min TTL
    ↓ (miss)
L2: Redis Cache - 5 min TTL
    ↓ (miss)
L3: Database
```

```java
@Service
public class DriverService {
    private final LoadingCache<UUID, Driver> localCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build(this::loadDriverFromRedis);
    
    public Driver getDriver(UUID driverId) {
        return localCache.get(driverId);
    }
    
    private Driver loadDriverFromRedis(UUID driverId) {
        String key = "driver:" + driverId;
        Driver driver = redisTemplate.opsForValue().get(key);
        
        if (driver == null) {
            driver = driverRepository.findById(driverId).orElseThrow();
            redisTemplate.opsForValue().set(key, driver, 5, TimeUnit.MINUTES);
        }
        
        return driver;
    }
}
```

### Database Optimization

**Indexing Strategy**:
```sql
-- Composite index for ride queries
CREATE INDEX idx_rides_rider_status ON rides(rider_id, status, requested_at DESC);

-- Partial index for active rides only
CREATE INDEX idx_active_rides ON rides(driver_id, status) 
WHERE status IN ('REQUESTED', 'ACCEPTED', 'STARTED');

-- GiST index for geo queries
CREATE INDEX idx_drivers_location ON drivers USING GIST(current_location);
```

**Query Optimization**:
```sql
-- Bad: Full table scan
SELECT * FROM rides WHERE rider_id = '123' ORDER BY requested_at DESC LIMIT 10;

-- Good: Index-only scan
SELECT ride_id, status, requested_at 
FROM rides 
WHERE rider_id = '123' AND status != 'CANCELLED'
ORDER BY requested_at DESC 
LIMIT 10;
```

---

## Security Best Practices

### API Security

**1. Rate Limiting**:
```java
@RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.USER)
@GetMapping("/api/v1/rides/{rideId}")
public Ride getRide(@PathVariable UUID rideId) {
    return rideService.getRide(rideId);
}
```

**2. Input Validation**:
```java
@PostMapping("/api/v1/rides/request")
public Ride requestRide(@Valid @RequestBody RideRequest request) {
    // Validate coordinates
    if (!isValidCoordinate(request.getPickupLocation())) {
        throw new InvalidRequestException("Invalid pickup location");
    }
    
    // Sanitize inputs
    request.setPickupAddress(sanitize(request.getPickupAddress()));
    
    return rideService.requestRide(request);
}
```

**3. Authentication**:
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/riders/**").hasRole("RIDER")
                .requestMatchers("/api/v1/drivers/**").hasRole("DRIVER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

---

## Cost Optimization

### Resource Efficiency

**1. Auto-Scaling**:
```yaml
# Kubernetes HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ride-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ride-service
  minReplicas: 10
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

**2. Spot Instances**:
- Use AWS Spot Instances for non-critical workloads (70% cost savings)
- Batch processing, analytics, ML training

**3. Data Lifecycle**:
```
Hot data (0-7 days): PostgreSQL + Redis
Warm data (7-90 days): S3 Standard
Cold data (90+ days): S3 Glacier
```

---

## Key Takeaways

1. **H3 over Geohash**: 10x better performance for geo queries
2. **gRPC for internal services**: 5-10x faster than REST
3. **Multi-layer caching**: 90% cache hit rate reduces DB load
4. **Circuit breakers**: Prevent cascading failures
5. **Real-time streaming**: Kafka + Flink for sub-second analytics
6. **ML-powered features**: ETA, pricing, fraud detection
7. **Observability**: Metrics, logs, traces for every request
8. **Multi-region**: Active-active for 99.99% availability

---

**References**:
- Uber Engineering Blog: https://www.uber.com/blog/engineering/
- H3 Documentation: https://h3geo.org/
- Michelangelo Platform: https://www.uber.com/blog/michelangelo-machine-learning-platform/
- RIBs Architecture: https://github.com/uber/RIBs

**Last Updated**: 2024
