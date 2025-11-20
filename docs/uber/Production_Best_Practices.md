# Uber Production Best Practices

Based on Uber Engineering Blog insights and real-world production systems.

## 1. Microservices Architecture

### Service Decomposition
```
Domain Services:
- Marketplace Service: Matching, dispatch
- Fulfillment Service: Trip execution, tracking
- Payments Service: Billing, settlements
- Pricing Service: Fare calculation, surge

Platform Services:
- Identity Service: Authentication, authorization
- Notification Service: Push, SMS, email
- Analytics Service: Metrics, logging
- Storage Service: S3, CDN
```

### Communication Patterns
- **Synchronous**: gRPC for internal services (5-10x faster than REST)
- **Asynchronous**: Kafka for events (1M+ events/sec)
- **Real-time**: WebSocket for client updates (100K+ connections)

## 2. Geo-Spatial Optimization

### H3 Hexagonal Indexing
```java
// Resolution levels
Resolution 5: ~252 km² - City sharding
Resolution 7: ~5.16 km² - Neighborhood matching
Resolution 9: ~0.105 km² - Driver search
Resolution 11: ~0.0016 km² - Precise tracking

// Benefits
- 10x faster proximity queries
- Uniform cell sizes
- O(1) neighbor lookup
```

### Redis Geo-Spatial
```redis
GEOADD drivers:online {lng} {lat} {driver_id}
GEORADIUS drivers:online {lng} {lat} 5km WITHDIST COUNT 20
```

## 3. Real-Time Data Processing

### Kafka Event Streaming
```
Topics:
- location.updates: 75K msgs/sec, 7-day retention
- ride.events: 5K msgs/sec, 90-day retention
- payment.transactions: 2K msgs/sec, 7-year retention

Partitioning:
- By driver_id for location updates
- By ride_id for ride events
- By user_id for payments
```

### Apache Flink Processing
```java
// Real-time ETA calculation
locations
  .keyBy(LocationUpdate::getDriverId)
  .window(TumblingEventTimeWindows.of(Time.seconds(10)))
  .process(new ETACalculator());
```

## 4. Caching Strategy

### Multi-Layer Cache
```
L1: Caffeine (Application) - 1 min TTL, 10K entries
L2: Redis (Distributed) - 5 min TTL, 1M entries
L3: PostgreSQL (Database) - Persistent
```

### Cache Patterns
```java
// Cache-aside pattern
public Driver getDriver(UUID id) {
    // L1: Application cache
    Driver driver = localCache.get(id);
    if (driver != null) return driver;
    
    // L2: Redis cache
    driver = redisTemplate.opsForValue().get("driver:" + id);
    if (driver != null) {
        localCache.put(id, driver);
        return driver;
    }
    
    // L3: Database
    driver = driverRepository.findById(id).orElseThrow();
    redisTemplate.opsForValue().set("driver:" + id, driver, 5, TimeUnit.MINUTES);
    localCache.put(id, driver);
    return driver;
}
```

## 5. Reliability Patterns

### Circuit Breaker
```java
@HystrixCommand(
    fallbackMethod = "fallbackPayment",
    commandProperties = {
        @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "20"),
        @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
    }
)
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentGateway.charge(request);
}
```

### Retry with Exponential Backoff
```java
int attempt = 0;
long backoff = 100; // ms
while (attempt < MAX_RETRIES) {
    try {
        return httpClient.execute(request);
    } catch (TransientException e) {
        Thread.sleep(backoff * (1L << attempt) + random.nextInt(100));
        attempt++;
    }
}
```

### Bulkhead Pattern
```java
// Isolate thread pools
@Bean
public ThreadPoolTaskExecutor paymentExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(100);
    return executor;
}
```

## 6. Database Optimization

### Indexing Strategy
```sql
-- Composite index for common queries
CREATE INDEX idx_rides_rider_status ON rides(rider_id, status, requested_at DESC);

-- Partial index for active rides
CREATE INDEX idx_active_rides ON rides(driver_id) WHERE status IN ('REQUESTED', 'ACCEPTED', 'STARTED');

-- GiST index for geo queries
CREATE INDEX idx_drivers_location ON drivers USING GIST(current_location);
```

### Connection Pooling
```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(50);
config.setMinimumIdle(10);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

### Partitioning
```sql
-- Time-based partitioning for rides
CREATE TABLE rides_2024_01 PARTITION OF rides
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

## 7. Observability

### Metrics (Prometheus)
```java
@Timed(value = "matching.duration", percentiles = {0.5, 0.95, 0.99})
public Driver findBestDriver(RideRequest request) {
    // Implementation
}

Counter.builder("rides.requested")
    .tag("vehicle_type", vehicleType)
    .register(meterRegistry)
    .increment();
```

### Distributed Tracing (Jaeger)
```java
@Traced(operationName = "request_ride")
public Ride requestRide(RideRequest request) {
    Span span = tracer.activeSpan();
    span.setTag("rider_id", request.getRiderId());
    span.setTag("vehicle_type", request.getVehicleType());
    // Implementation
}
```

### Logging (Structured)
```java
log.info("Ride requested: riderId={}, vehicleType={}, pickup={}, dropoff={}", 
    request.getRiderId(), 
    request.getVehicleType(),
    request.getPickupLocation(),
    request.getDropoffLocation()
);
```

## 8. Security

### Authentication
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/riders/**").hasRole("RIDER")
                .requestMatchers("/api/v1/drivers/**").hasRole("DRIVER")
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

### Rate Limiting
```java
@RateLimit(requests = 100, window = 60, scope = RateLimit.Scope.USER)
@GetMapping("/api/v1/rides/{rideId}")
public Ride getRide(@PathVariable UUID rideId) {
    return rideService.getRide(rideId);
}
```

### Input Validation
```java
@PostMapping("/api/v1/rides/request")
public Ride requestRide(@Valid @RequestBody RideRequest request) {
    if (!isValidCoordinate(request.getPickupLocation())) {
        throw new InvalidRequestException("Invalid pickup location");
    }
    return rideService.requestRide(request);
}
```

## 9. Performance Optimization

### Async Processing
```java
@Async("notificationExecutor")
public CompletableFuture<Void> sendNotification(UUID userId, String message) {
    notificationService.send(userId, message);
    return CompletableFuture.completedFuture(null);
}
```

### Batch Processing
```java
// Batch location updates
List<LocationUpdate> batch = new ArrayList<>();
for (LocationUpdate update : updates) {
    batch.add(update);
    if (batch.size() >= 100) {
        processBatch(batch);
        batch.clear();
    }
}
```

### Query Optimization
```sql
-- Use EXPLAIN ANALYZE
EXPLAIN ANALYZE
SELECT * FROM rides WHERE rider_id = '123' ORDER BY requested_at DESC LIMIT 10;

-- Optimize with covering index
CREATE INDEX idx_rides_covering ON rides(rider_id, requested_at DESC) 
INCLUDE (ride_id, status, estimated_fare);
```

## 10. Cost Optimization

### Auto-Scaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ride-service
spec:
  minReplicas: 10
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        averageUtilization: 70
```

### Data Lifecycle
```
Hot (0-7 days): PostgreSQL + Redis
Warm (7-90 days): S3 Standard
Cold (90+ days): S3 Glacier
```

### Spot Instances
- Use for batch processing (70% cost savings)
- Use for ML training workloads
- Not for critical real-time services

## Key Metrics

### Golden Signals
- **Latency**: p50, p95, p99 response times
- **Traffic**: Requests per second
- **Errors**: Error rate percentage
- **Saturation**: CPU, memory, disk usage

### Business Metrics
- Rides per minute
- Match rate (target: >95%)
- Average ETA accuracy
- Payment success rate (target: >99%)
- Driver utilization percentage

### SLOs
- Matching latency: p99 < 1s
- Location update latency: p99 < 100ms
- Payment processing: p99 < 3s
- API availability: 99.99%

---

**Last Updated**: 2024
**Source**: Uber Engineering Blog
