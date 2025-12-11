# Hot Shard Problem: Detection & Solutions

## Problem Statement
What happens if one DB shard gets "hot" (too many requests while others are idle)?

## What is a Hot Shard?

A **hot shard** occurs when:
- One shard receives disproportionately more traffic than others
- Causes: Celebrity users, viral content, poor sharding key choice
- Impact: High latency, timeouts, cascading failures

### Example Scenarios

```
Scenario 1: Social Media (Instagram)
- Shard by user_id
- Celebrity (100M followers) on Shard 5
- Shard 5: 10K QPS ❌ (overloaded)
- Other shards: 100 QPS ✅ (idle)

Scenario 2: E-commerce (Amazon)
- Shard by product_id
- iPhone launch → Product on Shard 3
- Shard 3: 50K QPS ❌ (hot)
- Other shards: 500 QPS ✅ (normal)

Scenario 3: Ride-hailing (Uber)
- Shard by geo-location
- New Year's Eve in Times Square → Shard 1
- Shard 1: 100K location updates/sec ❌
- Other shards: 1K updates/sec ✅
```

---

## Impact of Hot Shards

### 1. Performance Degradation
```
Normal Shard:  [====]      100ms latency
Hot Shard:     [==========] 5000ms latency ❌
```

### 2. Resource Exhaustion
- CPU: 100% utilization
- Memory: OOM errors
- Connections: Pool exhausted
- Disk I/O: Saturated

### 3. Cascading Failures
```
Hot Shard → Timeouts → Retries → More Load → Complete Failure
```

---

## Solution 1: Detect Hot Shards (Monitoring)

### Real-time Monitoring

```java
@Component
public class ShardHealthMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private AlertService alertService;
    
    private static final int SHARD_COUNT = 100;
    private static final double HOT_SHARD_THRESHOLD = 3.0; // 3x average
    
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void detectHotShards() {
        Map<Integer, Long> shardRequestCounts = new HashMap<>();
        
        // Collect request counts per shard
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            Counter counter = meterRegistry.counter("db.requests", "shard", String.valueOf(shardId));
            shardRequestCounts.put(shardId, (long) counter.count());
        }
        
        // Calculate average
        double avgRequests = shardRequestCounts.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        // Detect hot shards
        shardRequestCounts.forEach((shardId, count) -> {
            double ratio = count / avgRequests;
            
            if (ratio > HOT_SHARD_THRESHOLD) {
                log.warn("Hot shard detected: Shard {} has {}x average traffic", shardId, ratio);
                
                meterRegistry.gauge("db.shard.hotness", Tags.of("shard", String.valueOf(shardId)), ratio);
                
                alertService.sendAlert(
                    "Hot Shard Alert",
                    String.format("Shard %d is receiving %dx average traffic", shardId, (int) ratio)
                );
                
                // Trigger mitigation
                mitigateHotShard(shardId);
            }
        });
    }
    
    // Monitor shard-level metrics
    public void recordShardRequest(int shardId, long latency) {
        meterRegistry.counter("db.requests", "shard", String.valueOf(shardId)).increment();
        meterRegistry.timer("db.latency", "shard", String.valueOf(shardId))
            .record(latency, TimeUnit.MILLISECONDS);
    }
}
```

### Metrics to Track

```java
@Component
public class ShardMetrics {
    
    // Per-shard metrics
    - db.requests (counter) - Request count per shard
    - db.latency (timer) - Query latency per shard
    - db.connections (gauge) - Active connections per shard
    - db.cpu (gauge) - CPU utilization per shard
    - db.memory (gauge) - Memory usage per shard
    - db.queue_depth (gauge) - Connection pool queue depth
}
```

---

## Solution 2: Caching Hot Data

### Multi-Layer Cache for Hot Shards

```java
@Service
public class HotShardCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CaffeineCache localCache;
    
    @Autowired
    private ShardingService shardingService;
    
    // Detect and cache hot keys
    private Set<String> hotKeys = ConcurrentHashMap.newKeySet();
    
    public <T> T getWithHotShardProtection(String key, Class<T> type, Supplier<T> dbFetcher) {
        // L1: Local cache (for extremely hot keys)
        if (hotKeys.contains(key)) {
            T cached = localCache.get(key, type);
            if (cached != null) {
                return cached;
            }
        }
        
        // L2: Redis cache
        T redisCached = (T) redisTemplate.opsForValue().get(key);
        if (redisCached != null) {
            // Promote to local cache if accessed frequently
            incrementAccessCount(key);
            return redisCached;
        }
        
        // L3: Database
        T value = dbFetcher.get();
        
        // Cache in Redis
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
        
        return value;
    }
    
    private void incrementAccessCount(String key) {
        String countKey = "access_count:" + key;
        Long count = redisTemplate.opsForValue().increment(countKey);
        
        // Mark as hot if accessed > 1000 times in 1 minute
        if (count != null && count > 1000) {
            hotKeys.add(key);
            log.info("Marked key as hot: {}", key);
            
            // Cache locally for 5 minutes
            Object value = redisTemplate.opsForValue().get(key);
            localCache.put(key, value, 5, TimeUnit.MINUTES);
        }
    }
}
```

### Celebrity User Cache

```java
@Service
public class CelebrityUserService {
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    private static final Set<Long> CELEBRITY_IDS = ConcurrentHashMap.newKeySet();
    
    // Mark users as celebrities (100K+ followers)
    @Scheduled(cron = "0 0 * * * ?") // Hourly
    public void identifyCelebrities() {
        List<Long> celebrities = jdbcTemplate.queryForList(
            "SELECT user_id FROM user_stats WHERE follower_count > 100000",
            Long.class
        );
        
        CELEBRITY_IDS.clear();
        CELEBRITY_IDS.addAll(celebrities);
        
        log.info("Identified {} celebrity users", celebrities.size());
    }
    
    public User getUser(Long userId) {
        // Celebrity users → aggressive caching
        if (CELEBRITY_IDS.contains(userId)) {
            String cacheKey = "celebrity:user:" + userId;
            User cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                return cached;
            }
            
            User user = userRepository.findById(userId);
            
            // Cache for 1 hour (longer than regular users)
            redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
            
            return user;
        }
        
        // Regular users → normal flow
        return userRepository.findById(userId);
    }
}
```

---

## Solution 3: Read Replicas for Hot Shards

### Dynamic Read Replica Scaling

```java
@Service
public class HotShardReplicaService {
    
    @Autowired
    private Map<Integer, List<DataSource>> shardReplicas;
    
    @Autowired
    private LoadBalancer loadBalancer;
    
    // Add read replicas to hot shards
    public void scaleHotShard(int shardId) {
        log.info("Scaling hot shard {} with additional read replicas", shardId);
        
        // Provision new read replica (AWS RDS API)
        String replicaEndpoint = awsRdsService.createReadReplica(
            "shard-" + shardId + "-replica-" + System.currentTimeMillis()
        );
        
        // Add to connection pool
        DataSource replicaDataSource = createDataSource(replicaEndpoint);
        shardReplicas.get(shardId).add(replicaDataSource);
        
        log.info("Added read replica for shard {}: {}", shardId, replicaEndpoint);
    }
    
    // Route reads to replicas
    @Transactional(readOnly = true)
    public <T> T readFromReplica(int shardId, Function<JdbcTemplate, T> query) {
        List<DataSource> replicas = shardReplicas.get(shardId);
        
        // Load balance across replicas
        DataSource replica = loadBalancer.selectReplica(replicas);
        
        JdbcTemplate jdbc = new JdbcTemplate(replica);
        return query.apply(jdbc);
    }
}
```

---

## Solution 4: Better Sharding Strategy

### Problem: Poor Sharding Key

```java
// ❌ BAD: Shard by user_id (celebrities cause hot shards)
int shardId = userId % SHARD_COUNT;

// ❌ BAD: Shard by product_id (viral products cause hot shards)
int shardId = productId.hashCode() % SHARD_COUNT;

// ❌ BAD: Shard by geo-location (popular areas cause hot shards)
int shardId = getGeoHash(lat, lng) % SHARD_COUNT;
```

### Solution: Composite Sharding Key

```java
@Service
public class CompositeShardingService {
    
    private static final int SHARD_COUNT = 100;
    
    // ✅ GOOD: Shard by (user_id + timestamp)
    public int getShardForPost(Long userId, Instant timestamp) {
        // Combine user_id and day-of-year
        int dayOfYear = timestamp.atZone(ZoneId.systemDefault()).getDayOfYear();
        String compositeKey = userId + ":" + dayOfYear;
        
        return Math.abs(compositeKey.hashCode()) % SHARD_COUNT;
    }
    
    // ✅ GOOD: Shard by (product_id + category)
    public int getShardForProduct(String productId, String category) {
        String compositeKey = category + ":" + productId;
        return Math.abs(compositeKey.hashCode()) % SHARD_COUNT;
    }
    
    // ✅ GOOD: Shard by (geo_hash + user_id) for location updates
    public int getShardForLocation(String geoHash, Long userId) {
        // Distribute celebrity users across multiple shards
        String compositeKey = geoHash + ":" + (userId % 10);
        return Math.abs(compositeKey.hashCode()) % SHARD_COUNT;
    }
}
```

### Consistent Hashing with Virtual Nodes

```java
@Service
public class ConsistentHashingService {
    
    private final TreeMap<Integer, Integer> ring = new TreeMap<>();
    private static final int VIRTUAL_NODES = 150; // Per physical shard
    
    @PostConstruct
    public void initializeRing() {
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            // Add virtual nodes for each physical shard
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                String virtualKey = "shard-" + shardId + "-vnode-" + i;
                int hash = virtualKey.hashCode();
                ring.put(hash, shardId);
            }
        }
    }
    
    public int getShard(String key) {
        int hash = key.hashCode();
        
        // Find next shard in ring
        Map.Entry<Integer, Integer> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    // Add new shard dynamically
    public void addShard(int newShardId) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            String virtualKey = "shard-" + newShardId + "-vnode-" + i;
            int hash = virtualKey.hashCode();
            ring.put(hash, newShardId);
        }
        
        log.info("Added new shard {} with {} virtual nodes", newShardId, VIRTUAL_NODES);
    }
}
```

---

## Solution 5: Shard Splitting (Re-sharding)

### Split Hot Shard into Multiple Shards

```java
@Service
public class ShardSplittingService {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    // Split hot shard into 2 new shards
    public void splitShard(int hotShardId) {
        log.info("Splitting hot shard {} into 2 new shards", hotShardId);
        
        int newShard1 = SHARD_COUNT;
        int newShard2 = SHARD_COUNT + 1;
        
        // Step 1: Create new shards
        DataSource shard1 = dataSourceManager.createNewShard(newShard1);
        DataSource shard2 = dataSourceManager.createNewShard(newShard2);
        
        // Step 2: Copy data from hot shard
        JdbcTemplate hotShard = new JdbcTemplate(dataSourceManager.getShard(hotShardId));
        JdbcTemplate newShard1Jdbc = new JdbcTemplate(shard1);
        JdbcTemplate newShard2Jdbc = new JdbcTemplate(shard2);
        
        // Step 3: Migrate data (even/odd split)
        List<String> keys = hotShard.queryForList("SELECT key FROM data", String.class);
        
        keys.forEach(key -> {
            Map<String, Object> row = hotShard.queryForMap("SELECT * FROM data WHERE key = ?", key);
            
            // Split by hash
            if (key.hashCode() % 2 == 0) {
                newShard1Jdbc.update("INSERT INTO data VALUES (...)", row);
            } else {
                newShard2Jdbc.update("INSERT INTO data VALUES (...)", row);
            }
        });
        
        // Step 4: Update routing table
        shardRoutingService.updateRouting(hotShardId, Arrays.asList(newShard1, newShard2));
        
        // Step 5: Decommission old shard (after verification)
        dataSourceManager.decommissionShard(hotShardId);
        
        log.info("Shard splitting completed. Old shard {} split into {} and {}", 
            hotShardId, newShard1, newShard2);
    }
}
```

---

## Solution 6: Rate Limiting per Shard

### Protect Hot Shards with Rate Limiting

```java
@Service
public class ShardRateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int MAX_REQUESTS_PER_SHARD = 10000; // Per minute
    
    public boolean allowRequest(int shardId) {
        String key = "shard_rate_limit:" + shardId;
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == 1) {
            // Set expiry on first request
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        if (currentCount > MAX_REQUESTS_PER_SHARD) {
            log.warn("Rate limit exceeded for shard {}: {} requests", shardId, currentCount);
            return false;
        }
        
        return true;
    }
    
    @Aspect
    @Component
    public class ShardRateLimitAspect {
        
        @Around("@annotation(ShardRateLimited)")
        public Object enforceRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
            // Extract shard ID from method arguments
            int shardId = extractShardId(joinPoint.getArgs());
            
            if (!shardRateLimiter.allowRequest(shardId)) {
                throw new RateLimitExceededException("Shard " + shardId + " rate limit exceeded");
            }
            
            return joinPoint.proceed();
        }
    }
}

// Usage
@ShardRateLimited
public User getUser(Long userId) {
    int shardId = getShardId(userId);
    return queryUserFromShard(shardId, userId);
}
```

---

## Solution 7: Circuit Breaker for Hot Shards

### Prevent Cascading Failures

```java
@Service
public class ShardCircuitBreaker {
    
    private final Map<Integer, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeCircuitBreakers() {
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open if 50% failures
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(100)
                .build();
            
            circuitBreakers.put(shardId, CircuitBreaker.of("shard-" + shardId, config));
        }
    }
    
    public <T> T executeWithCircuitBreaker(int shardId, Supplier<T> operation) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(shardId);
        
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (CallNotPermittedException e) {
            log.error("Circuit breaker OPEN for shard {}. Returning cached/default value", shardId);
            
            // Fallback: Return cached data or default value
            return getFallbackValue(shardId);
        }
    }
    
    private <T> T getFallbackValue(int shardId) {
        // Try cache first
        String cacheKey = "shard_fallback:" + shardId;
        T cached = (T) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        // Return default/stale data
        return null;
    }
}
```

---

## Solution 8: Separate Storage for Hot Entities

### Dedicated Storage for Celebrities

```java
@Service
public class CelebrityStorageService {
    
    @Autowired
    @Qualifier("celebrityDataSource")
    private DataSource celebrityDataSource;
    
    @Autowired
    @Qualifier("regularDataSource")
    private DataSource regularDataSource;
    
    private static final long CELEBRITY_THRESHOLD = 100_000; // 100K followers
    
    public User getUser(Long userId) {
        // Check if celebrity
        if (isCelebrity(userId)) {
            // Query from dedicated celebrity database
            JdbcTemplate celebrityJdbc = new JdbcTemplate(celebrityDataSource);
            return celebrityJdbc.queryForObject(
                "SELECT * FROM celebrity_users WHERE user_id = ?",
                userRowMapper,
                userId
            );
        }
        
        // Regular users → sharded database
        int shardId = getShardId(userId);
        return queryFromShard(shardId, userId);
    }
    
    private boolean isCelebrity(Long userId) {
        Long followerCount = redisTemplate.opsForValue().get("follower_count:" + userId);
        return followerCount != null && followerCount > CELEBRITY_THRESHOLD;
    }
}
```

---

## Solution 9: Async Processing for Writes

### Queue Writes to Hot Shards

```java
@Service
public class AsyncWriteService {
    
    @Autowired
    private KafkaTemplate<String, WriteRequest> kafkaTemplate;
    
    private final Map<Integer, BlockingQueue<WriteRequest>> writeQueues = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeWriteQueues() {
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            writeQueues.put(shardId, new LinkedBlockingQueue<>(10000));
            startBatchWriter(shardId);
        }
    }
    
    // Queue write instead of direct execution
    public CompletableFuture<Void> writeAsync(int shardId, WriteRequest request) {
        BlockingQueue<WriteRequest> queue = writeQueues.get(shardId);
        
        if (!queue.offer(request)) {
            // Queue full → send to Kafka for durability
            kafkaTemplate.send("write-requests", String.valueOf(shardId), request);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private void startBatchWriter(int shardId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        executor.scheduleAtFixedRate(() -> {
            BlockingQueue<WriteRequest> queue = writeQueues.get(shardId);
            List<WriteRequest> batch = new ArrayList<>();
            queue.drainTo(batch, 1000); // Batch size: 1000
            
            if (!batch.isEmpty()) {
                batchWrite(shardId, batch);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    private void batchWrite(int shardId, List<WriteRequest> batch) {
        JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(shardId));
        
        jdbc.batchUpdate(
            "INSERT INTO data (key, value) VALUES (?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    WriteRequest req = batch.get(i);
                    ps.setString(1, req.getKey());
                    ps.setString(2, req.getValue());
                }
                
                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            }
        );
    }
}
```

---

## Complete Mitigation Strategy

```java
@Service
public class HotShardMitigationService {
    
    @Autowired
    private ShardHealthMonitor healthMonitor;
    
    @Autowired
    private HotShardCacheService cacheService;
    
    @Autowired
    private HotShardReplicaService replicaService;
    
    @Autowired
    private ShardRateLimiter rateLimiter;
    
    @Autowired
    private ShardCircuitBreaker circuitBreaker;
    
    public void mitigateHotShard(int shardId) {
        log.info("Initiating hot shard mitigation for shard {}", shardId);
        
        // Step 1: Immediate - Enable aggressive caching
        cacheService.enableAggressiveCaching(shardId);
        
        // Step 2: Immediate - Enable rate limiting
        rateLimiter.enableRateLimiting(shardId);
        
        // Step 3: Short-term (5 min) - Add read replicas
        replicaService.scaleHotShard(shardId);
        
        // Step 4: Medium-term (1 hour) - Enable circuit breaker
        circuitBreaker.enableCircuitBreaker(shardId);
        
        // Step 5: Long-term (manual) - Consider shard splitting
        if (healthMonitor.isChronicallyHot(shardId)) {
            alertService.sendAlert(
                "Chronic Hot Shard",
                "Shard " + shardId + " requires manual intervention (shard splitting)"
            );
        }
    }
}
```

---

## Key Takeaways

### Detection
1. **Monitor per-shard metrics** (QPS, latency, CPU, memory)
2. **Alert on 3x average** traffic
3. **Track hot keys** within shards

### Immediate Mitigation (< 1 min)
1. **Aggressive caching** (Redis + local cache)
2. **Rate limiting** per shard
3. **Circuit breaker** to prevent cascading failures

### Short-term (5-30 min)
1. **Add read replicas** to hot shards
2. **Route reads** to replicas
3. **Async writes** with batching

### Long-term (hours-days)
1. **Better sharding strategy** (composite keys, consistent hashing)
2. **Shard splitting** (re-shard hot data)
3. **Separate storage** for hot entities (celebrities)
4. **Capacity planning** based on traffic patterns

### Prevention
1. **Choose good sharding keys** (avoid celebrity problem)
2. **Use consistent hashing** with virtual nodes
3. **Monitor and predict** hot shards before they occur
4. **Design for uneven distribution** from day one

---

## Interview Answer Summary

**Question**: What happens if one shard gets hot?

**Answer**:

**Problem**: One shard receives 10x traffic → high latency, timeouts, failures

**Detection**:
- Monitor per-shard QPS, latency, CPU
- Alert when shard > 3x average traffic

**Immediate Fixes** (< 1 min):
1. Aggressive caching (Redis + local)
2. Rate limiting per shard
3. Circuit breaker for fallback

**Short-term** (5-30 min):
1. Add read replicas to hot shard
2. Route reads to replicas
3. Async writes with batching

**Long-term** (hours-days):
1. Better sharding key (composite, consistent hashing)
2. Shard splitting (re-shard hot data)
3. Separate storage for hot entities

**Prevention**:
- Avoid user_id/product_id as sole sharding key
- Use composite keys (user_id + timestamp)
- Consistent hashing with virtual nodes
- Design for 10x traffic imbalance
