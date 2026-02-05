# Database Cache Strategies - Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Cache Patterns](#cache-patterns)
3. [Cache Invalidation](#cache-invalidation)
4. [Implementation Examples](#implementation-examples)
5. [Performance Analysis](#performance-analysis)
6. [Best Practices](#best-practices)

---

## Introduction

### What is Database Caching?

Caching stores frequently accessed data in fast storage (RAM) to reduce database load and improve response times.

```
Without Cache:
Request → Application → Database (10-100ms)

With Cache:
Request → Application → Cache (0.1-1ms) → Database (if miss)

Speed improvement: 10-100x faster!
```

### Cache Hierarchy

```
┌─────────────────────────────────────┐
│         Application Layer           │
├─────────────────────────────────────┤
│    L1: Local Cache (In-Memory)     │
│    - Caffeine, Guava Cache          │
│    - Response time: 0.1ms           │
├─────────────────────────────────────┤
│    L2: Distributed Cache (Redis)   │
│    - Redis, Memcached               │
│    - Response time: 1-5ms           │
├─────────────────────────────────────┤
│    L3: Database                     │
│    - PostgreSQL, MySQL              │
│    - Response time: 10-100ms        │
└─────────────────────────────────────┘
```

## Cache Patterns Comparison Matrix

### Quick Reference Table

```
┌─────────────────┬─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│ Pattern         │ Read Perf   │ Write Perf  │ Consistency │ Complexity  │ Best For    │
├─────────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ Cache-Aside     │ Good        │ Excellent   │ Eventual    │ Low         │ General     │
│                 │ (1-100ms)   │ (10-100ms)  │             │             │ purpose     │
├─────────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ Read-Through    │ Good        │ Good        │ Eventual    │ Medium      │ Framework   │
│                 │ (1-100ms)   │ (10-100ms)  │             │             │ integration │
├─────────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ Write-Through   │ Excellent   │ Poor        │ Strong      │ Medium      │ Financial   │
│                 │ (0.1-1ms)   │ (10-100ms)  │             │             │ systems     │
├─────────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ Write-Behind    │ Excellent   │ Excellent   │ Eventual    │ High        │ Analytics   │
│                 │ (0.1-1ms)   │ (1-2ms)     │             │             │ logging     │
├─────────────────┼─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
│ Refresh-Ahead   │ Excellent   │ Good        │ Eventual    │ High        │ Hot data    │
│                 │ (0.1-1ms)   │ (10-100ms)  │             │             │ predictable │
└─────────────────┴─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
```

### Decision Tree

```
Start: Choose Cache Pattern
│
├── Need strong consistency?
│   │
│   └── YES ─► Write-Through
│   │
│   └── NO ─► Continue
│
├── Write-heavy workload (>50% writes)?
│   │
│   └── YES ─► Can tolerate data loss?
│   │       │
│   │       ├── YES ─► Write-Behind
│   │       │
│   │       └── NO ─► Write-Through
│   │
│   └── NO ─► Continue
│
├── Predictable hot data?
│   │
│   └── YES ─► Latency-sensitive?
│   │       │
│   │       ├── YES ─► Refresh-Ahead
│   │       │
│   │       └── NO ─► Cache-Aside
│   │
│   └── NO ─► Continue
│
├── Using Spring Framework?
│   │
│   ├── YES ─► Read-Through (@Cacheable)
│   │
│   └── NO ─► Cache-Aside (default)
│
└── Default: Cache-Aside (simplest, most flexible)
```

### Hybrid Patterns (Real-World)

Most production systems combine multiple patterns:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        E-Commerce System                           │
├────────────────────────────────────────────────────────────────────────┤
│ Product Catalog:      Cache-Aside (read-heavy, can be stale)        │
│ Inventory Count:      Write-Through (strong consistency)            │
│ User Sessions:        Write-Behind (fast writes, eventual sync)     │
│ Popular Products:     Refresh-Ahead (predictable hot data)         │
│ Search Results:       Cache-Aside (unpredictable queries)          │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                      Social Media Platform                          │
├────────────────────────────────────────────────────────────────────────┤
│ User Profiles:        Read-Through (framework integration)          │
│ Like Counts:          Write-Behind (high write volume)              │
│ News Feed:            Cache-Aside (personalized, unpredictable)     │
│ Trending Posts:       Refresh-Ahead (predictable hot content)       │
│ Comments:             Cache-Aside (read after write)                │
└────────────────────────────────────────────────────────────────────────┘
```

### Performance Comparison (Real Numbers)

```
Benchmark: 1M requests, 80% reads, 20% writes

┌─────────────────┬────────────┬────────────┬────────────┬────────────┐
│ Pattern         │ Avg Read   │ Avg Write  │ DB Queries │ Throughput │
├─────────────────┼────────────┼────────────┼────────────┼────────────┤
│ Cache-Aside     │ 5ms        │ 15ms       │ 160K       │ 50K RPS    │
│ Read-Through    │ 6ms        │ 15ms       │ 160K       │ 45K RPS    │
│ Write-Through   │ 1ms        │ 50ms       │ 1M         │ 30K RPS    │
│ Write-Behind    │ 1ms        │ 2ms        │ 20K        │ 100K RPS   │
│ Refresh-Ahead   │ 1ms        │ 15ms       │ 150K       │ 80K RPS    │
└─────────────────┴────────────┴────────────┴────────────┴────────────┘

Key Insights:
- Write-Behind: Highest throughput, lowest DB load
- Write-Through: Lowest read latency, highest DB load
- Cache-Aside: Balanced, good default choice
- Refresh-Ahead: Best read latency for hot data
```

---

## Cache Patterns

### 1. Cache-Aside (Lazy Loading)

**Most common pattern** - Application manages cache explicitly.

#### Visual Flow Diagram
```
┌─────────────┐
│ Application │
└──────┬──────┘
       │
       ├─── READ ────────────────────────────────────┐
       │                                              │
       ▼                                              ▼
  ┌─────────┐                                   ┌──────────┐
  │  Cache  │                                   │ Database │
  └────┬────┘                                   └──────────┘
       │
       ├─ HIT? ──► Return Data (0.1-1ms)
       │
       └─ MISS? ─► Query DB (10-100ms) ─► Store in Cache ─► Return


       ├─── WRITE ──────────────────────────────────┐
       │                                             │
       ▼                                             ▼
  ┌─────────┐                                  ┌──────────┐
  │  Cache  │◄─── Invalidate ───────────────── │ Database │
  └─────────┘                                  └──────────┘
                                                     ▲
                                                     │
                                              Write First
```

#### Read Flow (Detailed)
```
1. Application checks cache for key
2. If HIT (data exists):
   → Return cached data immediately (0.1-1ms)
   → Cache hit ratio: 80-95% in production
3. If MISS (data not found):
   → Query database (10-100ms)
   → Store result in cache with TTL
   → Return data to application
   → Total latency: 11-101ms (first request)
```

#### Write Flow (Detailed)
```
1. Application writes to database first (durability)
2. Database confirms write success
3. Application invalidates cache entry
4. Next read will be cache MISS (lazy reload)
5. Alternative: Update cache immediately (write-through)
```

**Implementation**:
```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    private static final String CACHE_KEY_PREFIX = "user:";
    
    // Read with Cache-Aside
    public User getUser(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 1. Check cache
        User user = redisTemplate.opsForValue().get(cacheKey);
        
        if (user != null) {
            // Cache HIT
            return user;
        }
        
        // 2. Cache MISS - query database
        user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        // 3. Store in cache
        redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
        
        return user;
    }
    
    // Write with Cache Invalidation
    public User updateUser(Long id, User updatedUser) {
        // 1. Update database
        User user = userRepository.save(updatedUser);
        
        // 2. Invalidate cache
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.delete(cacheKey);
        
        return user;
    }
}
```

**Pros**:
- ✅ **Simple to implement** - Straightforward logic, easy to debug
- ✅ **Cache only requested data** - No wasted memory on unused data
- ✅ **Resilient to cache failures** - App continues working if cache is down
- ✅ **Flexible TTL** - Different expiration times per data type
- ✅ **No write penalty** - Writes are fast (only DB operation)

**Cons**:
- ❌ **Cache miss penalty** - 3 round trips (check cache → query DB → update cache)
- ❌ **Stale data possible** - Cache may have outdated data until TTL expires
- ❌ **Cache stampede risk** - Multiple requests hit DB simultaneously on miss
- ❌ **Cold start problem** - Empty cache after restart causes DB load spike
- ❌ **Code duplication** - Caching logic scattered across codebase

**When to Use**:
- ✅ Read-heavy workloads (90%+ reads)
- ✅ Data that doesn't change frequently
- ✅ Acceptable to serve slightly stale data
- ✅ Need simple, maintainable solution

**Real-World Examples**:
- **E-commerce**: Product catalog, inventory counts
- **Social Media**: User profiles, follower counts
- **News Sites**: Article content, comment threads
- **SaaS Apps**: User settings, feature flags

**Performance Metrics**:
```
Cache Hit:  0.1-1ms    (100x faster than DB)
Cache Miss: 11-101ms   (DB query + cache update)
Hit Ratio:  80-95%     (typical production)
Throughput: 100K+ RPS  (with Redis)
```

---

### 2. Read-Through Cache

Cache sits between application and database, automatically loads data on miss.

#### Visual Flow Diagram
```
┌─────────────┐
│ Application │
└──────┬──────┘
       │
       │ Always talks to Cache only
       │ (Cache handles DB interaction)
       ▼
  ┌───────────────────────┐
  │   Cache (Smart Layer)  │
  │                        │
  │  HIT? ─► Return Data   │
  │                        │
  │  MISS? ─► Load from DB │
  │         ─► Cache it     │
  │         ─► Return       │
  └───────────┬───────────┘
             │
             ▼
      ┌──────────┐
      │ Database │
      └──────────┘
```

#### Read Flow (Detailed)
```
1. Application requests data from cache
2. Cache checks if data exists:
   → HIT: Return immediately
   → MISS: Cache automatically:
      a) Queries database
      b) Stores result in cache
      c) Returns data to application
3. Application is unaware of cache miss
4. Transparent to application code
```

#### Write Flow (Detailed)
```
1. Application writes to database directly
2. Application invalidates cache entry
3. Next read triggers cache reload
4. Alternative: Use write-through for consistency
```

**Implementation**:
```java
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // Read-Through with Spring Cache
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    @CacheEvict(value = "users", key = "#id")
    public User updateUser(Long id, User updatedUser) {
        return userRepository.save(updatedUser);
    }
}
```

**Pros**:
- ✅ **Transparent to application** - No caching logic in business code
- ✅ **Consistent caching logic** - Centralized cache management
- ✅ **Less code duplication** - Cache logic in one place
- ✅ **Easier testing** - Mock cache layer independently
- ✅ **Automatic cache population** - No manual cache.set() calls

**Cons**:
- ❌ **Tight coupling with cache** - Application depends on cache availability
- ❌ **Cache failure affects reads** - No fallback to DB if cache is down
- ❌ **Complex setup** - Requires cache provider with read-through support
- ❌ **Limited control** - Less flexibility in caching strategy
- ❌ **Debugging harder** - Cache operations are hidden

**When to Use**:
- ✅ Need consistent caching across application
- ✅ Want to abstract caching from business logic
- ✅ Using framework with built-in support (Spring Cache)
- ✅ Cache is highly available (clustered Redis)

**Real-World Examples**:
- **Spring Boot Apps**: @Cacheable annotations
- **Hibernate**: Second-level cache
- **CDN**: CloudFront, Cloudflare (read-through for origin)
- **API Gateways**: Kong, AWS API Gateway caching

**Performance Metrics**:
```
Cache Hit:  0.5-2ms    (slightly slower than cache-aside)
Cache Miss: 12-102ms   (cache loads from DB)
Hit Ratio:  85-95%     (similar to cache-aside)
Throughput: 80K+ RPS   (depends on cache provider)
```

**Comparison with Cache-Aside**:
```
┌────────────────┬─────────────────┬─────────────────┐
│ Aspect         │ Cache-Aside     │ Read-Through    │
├────────────────┼─────────────────┼─────────────────┤
│ Complexity     │ Low             │ Medium          │
│ Control        │ High            │ Low             │
│ Code in App    │ Yes             │ No (annotation) │
│ Cache Failure  │ Graceful        │ Breaks reads    │
│ Flexibility    │ High            │ Limited         │
└────────────────┴─────────────────┴─────────────────┘
```

---

### 3. Write-Through Cache

Data written to cache and database simultaneously.

#### Visual Flow Diagram
```
┌─────────────┐
│ Application │
└──────┬──────┘
       │
       │ WRITE
       ▼
  ┌───────────────────────┐
  │   Cache (Write-Through) │
  │                        │
  │  1. Write to cache     │
  │  2. Write to DB (sync) │
  │  3. Return success     │
  └───────────┬───────────┘
             │
             │ Synchronous write
             ▼
      ┌──────────┐
      │ Database │
      └──────────┘


READ Flow:
┌─────────────┐
│ Application │
└──────┬──────┘
       │ READ
       ▼
  ┌─────────┐
  │  Cache  │ ──► Always HIT (data is fresh)
  └─────────┘
```

#### Write Flow (Detailed)
```
1. Application writes data
2. Cache receives write request
3. Cache writes to itself (fast)
4. Cache synchronously writes to database (slow)
5. Both operations must succeed
6. Return success to application
7. Total latency: Cache write + DB write (10-100ms)
```

#### Read Flow (Detailed)
```
1. Application reads from cache
2. Cache always has latest data (no stale reads)
3. Cache HIT guaranteed (if data exists)
4. No database query needed
5. Ultra-fast reads (0.1-1ms)
```

#### Consistency Guarantee
```
Cache and Database are ALWAYS in sync:

✅ Write succeeds → Both updated
❌ Write fails → Neither updated (rollback)
❌ Partial failure → Transaction rollback

Result: Strong consistency, no stale data
```

**Implementation**:
```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    // Write-Through
    @CachePut(value = "users", key = "#user.id")
    public User createUser(User user) {
        // 1. Write to database
        User savedUser = userRepository.save(user);
        
        // 2. Write to cache (done by @CachePut)
        return savedUser;
    }
    
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

**Pros**:
- ✅ **Cache always consistent** - No stale data, cache = DB
- ✅ **No cache miss penalty** - Reads are always fast
- ✅ **Data durability** - Written to persistent storage immediately
- ✅ **Simple read logic** - No fallback to DB needed
- ✅ **Strong consistency** - ACID guarantees maintained

**Cons**:
- ❌ **Write latency** - 2 operations (cache + DB) slow down writes
- ❌ **Wasted cache space** - Caches data that may never be read
- ❌ **Cache failure affects writes** - Write fails if cache is down
- ❌ **Higher write cost** - Every write hits both systems
- ❌ **No write optimization** - Can't batch or defer writes

**When to Use**:
- ✅ **Strong consistency required** - Financial transactions, inventory
- ✅ **Read-heavy after write** - Data is immediately read after creation
- ✅ **Small dataset** - All data fits in cache
- ✅ **High read/write ratio** - 10:1 or higher

**When NOT to Use**:
- ❌ **Write-heavy workloads** - Writes become bottleneck
- ❌ **Large datasets** - Cache can't hold everything
- ❌ **Batch operations** - Bulk writes are slow
- ❌ **Temporary data** - Data that expires quickly

**Real-World Examples**:
- **Banking**: Account balances, transaction history
- **E-commerce**: Shopping cart, order status
- **Gaming**: Player scores, leaderboards
- **Booking Systems**: Seat reservations, ticket inventory

**Performance Metrics**:
```
Write Latency: 10-100ms  (cache + DB)
Read Latency:  0.1-1ms   (always cache hit)
Consistency:   Strong    (cache = DB)
Throughput:    10K WPS   (limited by DB)
Cache Hit:     99%+      (almost always)
```

**Failure Scenarios**:
```
Scenario 1: Cache write succeeds, DB write fails
→ Rollback cache write
→ Return error to application
→ Consistency maintained

Scenario 2: Cache write fails
→ Don't write to DB
→ Return error immediately
→ No inconsistency

Scenario 3: Both succeed
→ Cache and DB in sync
→ Return success
```

---

### 4. Write-Behind (Write-Back) Cache

Data written to cache immediately, database updated asynchronously.

#### Visual Flow Diagram
```
WRITE Flow (Fast Path):
┌─────────────┐
│ Application │
└──────┬──────┘
       │ WRITE
       ▼
  ┌───────────────────────┐
  │   Cache (Write-Back)   │
  │                        │
  │  1. Write to cache     │
  │  2. Return SUCCESS     │ ──► Fast! (1-2ms)
  │  3. Queue for DB write │
  └───────────┬───────────┘
             │
             │ Async (later)
             ▼
      ┌──────────────────┐
      │ Background Worker │
      │                   │
      │ - Batch writes    │
      │ - Retry on fail   │
      │ - Coalesce writes │
      └─────────┬─────────┘
                │
                ▼
         ┌──────────┐
         │ Database │
         └──────────┘
```

#### Write Flow (Detailed)
```
1. Application writes data
2. Cache stores data immediately (1-2ms)
3. Cache marks entry as "dirty"
4. Return success to application (fast!)
5. Background worker picks up dirty entries
6. Worker batches multiple writes
7. Worker writes to database asynchronously
8. On success, mark entry as "clean"
9. On failure, retry with exponential backoff
```

#### Read Flow (Detailed)
```
1. Application reads from cache
2. Cache returns data (may be dirty)
3. Ultra-fast reads (0.1-1ms)
4. Data may not be in DB yet (eventual consistency)
```

#### Write Coalescing (Optimization)
```
Without Coalescing:
Update user.score = 100  → DB write
Update user.score = 150  → DB write
Update user.score = 200  → DB write
Total: 3 DB writes

With Coalescing:
Update user.score = 100  → Cache only
Update user.score = 150  → Cache only
Update user.score = 200  → Cache only
Batch write: user.score = 200
Total: 1 DB write (3x reduction!)
```

#### Failure Handling
```
┌────────────────────────────────────────────────┐
│ Failure Scenario                                  │
├────────────────────────────────────────────────┤
│ 1. Cache write succeeds, DB write fails later    │
│    → Retry with exponential backoff            │
│    → Move to Dead Letter Queue after N retries │
│    → Alert operations team                      │
├────────────────────────────────────────────────┤
│ 2. Cache crashes before DB write                 │
│    → Data loss! (dirty entries lost)            │
│    → Mitigation: Persist dirty entries to disk  │
│    → Use Redis AOF or RDB snapshots             │
├────────────────────────────────────────────────┤
│ 3. Database is down for extended period          │
│    → Queue grows (memory pressure)              │
│    → Implement queue size limits                │
│    → Reject new writes if queue is full         │
└────────────────────────────────────────────────┘
```

**Implementation**:
```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AsyncTaskExecutor taskExecutor;
    
    // Write-Behind
    public User updateUser(Long id, User user) {
        String cacheKey = "user:" + id;
        
        // 1. Write to cache immediately
        redisTemplate.opsForValue().set(cacheKey, user);
        
        // 2. Async write to database
        taskExecutor.execute(() -> {
            try {
                userRepository.save(user);
            } catch (Exception e) {
                // Handle failure (retry, DLQ, etc.)
                log.error("Failed to persist user to database", e);
            }
        });
        
        return user;
    }
    
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

**Pros**:
- ✅ **Fast writes** - No DB wait, 1-2ms response time (50-100x faster)
- ✅ **Reduced database load** - Batch writes, fewer DB operations
- ✅ **Batch writes possible** - Coalesce multiple updates into one
- ✅ **Write optimization** - Combine, deduplicate, compress writes
- ✅ **High throughput** - Handle 100K+ writes/sec
- ✅ **Absorbs write spikes** - Queue buffers traffic bursts

**Cons**:
- ❌ **Data loss risk** - Cache failure before DB write loses data
- ❌ **Complex consistency** - Eventual consistency, not immediate
- ❌ **Requires background workers** - Additional infrastructure
- ❌ **Debugging harder** - Async operations complicate troubleshooting
- ❌ **Memory pressure** - Queue can grow large during DB outages
- ❌ **Ordering issues** - Writes may arrive out of order

**When to Use**:
- ✅ **Write-heavy workloads** - 50%+ writes, high write throughput
- ✅ **Acceptable data loss** - Can tolerate losing recent writes
- ✅ **Bursty traffic** - Need to absorb traffic spikes
- ✅ **Analytics/Logging** - Non-critical data, eventual consistency OK

**When NOT to Use**:
- ❌ **Financial transactions** - Cannot risk data loss
- ❌ **Strong consistency required** - Need immediate DB persistence
- ❌ **Audit trails** - Must guarantee write durability
- ❌ **Small write volume** - Complexity not justified

**Real-World Examples**:
- **Social Media**: Like counts, view counts, follower counts
- **Gaming**: Player stats, achievements, leaderboards
- **Analytics**: Page views, click tracking, user events
- **IoT**: Sensor data, telemetry, metrics
- **Logging**: Application logs, audit logs (non-critical)

**Performance Metrics**:
```
Write Latency:  1-2ms      (cache only, 50x faster)
Read Latency:   0.1-1ms    (always cache hit)
Consistency:    Eventual   (seconds to minutes lag)
Throughput:     100K+ WPS  (limited by cache)
DB Write Rate:  1K-10K WPS (batched)
Write Reduction: 10-100x   (via coalescing)
```

**Data Loss Risk Mitigation**:
```
1. Redis Persistence:
   - AOF (Append-Only File): fsync every second
   - RDB Snapshots: Every 5 minutes
   - Worst case: Lose 1-5 minutes of data

2. Dual Write:
   - Write to cache + message queue (Kafka)
   - Worker reads from queue and writes to DB
   - No data loss even if cache crashes

3. Write-Ahead Log (WAL):
   - Log writes to disk before caching
   - Replay log on recovery
   - Guarantees durability
```

---

### 5. Refresh-Ahead Cache

Proactively refresh cache before expiration.

#### Visual Flow Diagram
```
Normal Operation:
┌─────────────┐
│ Application │
└──────┬──────┘
       │ READ
       ▼
  ┌─────────┐
  │  Cache  │ ──► Always HIT (proactively refreshed)
  └─────────┘


Background Refresh Process:
┌────────────────────────────────────────────────┐
│         Refresh Scheduler (Background)         │
│                                                 │
│  1. Monitor cache TTL                          │
│  2. If TTL < threshold (e.g., 10% remaining)   │
│  3. Fetch fresh data from DB                   │
│  4. Update cache with new TTL                  │
│  5. User never experiences cache miss          │
└────────────────┬────────────────────────────────┘
               │
               ▼
        ┌──────────┐
        │ Database │
        └──────────┘
```

#### Refresh Strategy (Detailed)
```
Example: Cache TTL = 1 hour, Refresh threshold = 10%

Timeline:
00:00 - Data cached (TTL = 3600s)
00:54 - TTL = 360s (10% remaining)
00:54 - Refresh triggered (background)
00:54 - Fresh data loaded from DB
00:54 - Cache updated (TTL reset to 3600s)
01:00 - User reads data (HIT, no miss!)

Result: User never experiences cache miss
```

#### Predictive Refresh (Advanced)
```
Use ML/Analytics to predict hot data:

1. Track access patterns:
   - user:123 accessed 1000 times/hour
   - user:456 accessed 10 times/hour

2. Prioritize refresh:
   - High-traffic keys: Refresh at 20% TTL
   - Medium-traffic keys: Refresh at 10% TTL
   - Low-traffic keys: Let expire naturally

3. Adaptive TTL:
   - Hot data: TTL = 1 hour
   - Warm data: TTL = 10 minutes
   - Cold data: TTL = 1 minute
```

#### Access Pattern Detection
```java
@Service
public class CacheRefreshService {
    
    // Track access frequency
    private Map<String, AtomicLong> accessCounts = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void analyzeAndRefresh() {
        // Get all cache keys
        Set<String> keys = redisTemplate.keys("user:*");
        
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            Long accessCount = accessCounts.getOrDefault(key, new AtomicLong(0)).get();
            
            // Hot data: accessed > 100 times/min
            if (accessCount > 100 && ttl < 600) {
                refreshCache(key);
            }
            // Warm data: accessed > 10 times/min
            else if (accessCount > 10 && ttl < 60) {
                refreshCache(key);
            }
        }
        
        // Reset counters
        accessCounts.clear();
    }
}
```

**Implementation**:
```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void refreshPopularUsers() {
        // Get popular user IDs (from analytics)
        List<Long> popularUserIds = getPopularUserIds();
        
        for (Long userId : popularUserIds) {
            String cacheKey = "user:" + userId;
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            
            // Refresh if TTL < 10 minutes
            if (ttl != null && ttl < 600) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
                }
            }
        }
    }
}
```

**Pros**:
- ✅ **No cache miss for hot data** - Proactive refresh eliminates misses
- ✅ **Predictable performance** - Consistent sub-millisecond latency
- ✅ **Reduced database load** - Controlled refresh rate vs random misses
- ✅ **Better user experience** - No sudden latency spikes
- ✅ **Optimized for read-heavy** - Perfect for frequently accessed data

**Cons**:
- ❌ **Complex implementation** - Requires background workers and monitoring
- ❌ **Wasted refreshes** - May refresh data that won't be accessed
- ❌ **Requires prediction logic** - Need to identify hot data
- ❌ **Additional infrastructure** - Schedulers, workers, analytics
- ❌ **Tuning complexity** - Finding optimal refresh thresholds

**When to Use**:
- ✅ **Predictable access patterns** - Same data accessed repeatedly
- ✅ **High read volume** - 1000+ reads/sec per key
- ✅ **Latency-sensitive** - Cannot tolerate cache miss penalty
- ✅ **Expensive DB queries** - Complex joins, aggregations

**When NOT to Use**:
- ❌ **Unpredictable access** - Random data access patterns
- ❌ **Low read volume** - < 10 reads/sec per key
- ❌ **Rapidly changing data** - Data changes faster than refresh
- ❌ **Large dataset** - Cannot predict which data will be hot

**Real-World Examples**:
- **E-commerce**: Product pages during sales (predictable spikes)
- **News Sites**: Trending articles, breaking news
- **Social Media**: Celebrity profiles, viral posts
- **Gaming**: Leaderboards, popular game stats
- **Finance**: Stock prices, currency rates

**Performance Metrics**:
```
Cache Hit Rate:  99.9%+     (almost never miss)
Read Latency:    0.1-1ms    (always cache hit)
DB Load:         Controlled (scheduled refreshes)
Refresh Overhead: 1-5%      (background process)
User Experience: Excellent  (no latency spikes)
```

**Comparison: Refresh-Ahead vs Cache-Aside**
```
┌─────────────────────┬──────────────────┬──────────────────┐
│ Metric              │ Cache-Aside      │ Refresh-Ahead    │
├─────────────────────┼──────────────────┼──────────────────┤
│ Cache Miss Rate     │ 5-20%            │ < 0.1%           │
│ P99 Latency         │ 100ms (on miss)  │ 1ms (always)     │
│ DB Load             │ Spiky (on miss)  │ Smooth (refresh) │
│ Complexity          │ Low              │ High             │
│ Infrastructure      │ Simple           │ Complex          │
│ Best For            │ General use      │ Hot data         │
└─────────────────────┴──────────────────┴──────────────────┘
```

**Refresh Strategies**:
```
1. Time-Based Refresh:
   - Refresh every N seconds
   - Simple but may waste resources
   - Example: Stock prices every 5 seconds

2. TTL-Based Refresh:
   - Refresh when TTL < threshold
   - More efficient than time-based
   - Example: Refresh at 10% TTL remaining

3. Access-Based Refresh:
   - Refresh based on access frequency
   - Most efficient, no wasted refreshes
   - Example: Refresh if accessed > 100 times/min

4. Hybrid Approach:
   - Combine multiple strategies
   - Time-based for critical data
   - Access-based for user data
   - Example: Stock prices (time) + user profiles (access)
```

---

## Cache Invalidation

### Strategies

#### 1. TTL (Time-To-Live)

```java
// Set expiration time
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

// Or with @Cacheable
@Cacheable(value = "users", key = "#id")
@CacheConfig(cacheNames = "users", ttl = 3600) // 1 hour
public User getUser(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

#### 2. Event-Based Invalidation

```java
@Service
public class UserService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public User updateUser(Long id, User user) {
        User updated = userRepository.save(user);
        
        // Publish event
        eventPublisher.publishEvent(new UserUpdatedEvent(id));
        
        return updated;
    }
}

@Component
public class CacheInvalidationListener {
    
    @Autowired
    private CacheManager cacheManager;
    
    @EventListener
    public void handleUserUpdated(UserUpdatedEvent event) {
        Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(event.getUserId());
        }
    }
}
```

#### 3. Tag-Based Invalidation

```java
@Service
public class ProductService {
    
    public void updateProduct(Long productId, Product product) {
        productRepository.save(product);
        
        // Invalidate all caches with this tag
        String tag = "product:" + productId;
        invalidateCachesByTag(tag);
    }
    
    private void invalidateCachesByTag(String tag) {
        Set<String> keys = redisTemplate.keys("*:" + tag + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

#### 4. Version-Based Invalidation

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @Version
    private Long version;
    
    private String name;
}

@Service
public class UserService {
    
    public User getUser(Long id) {
        String cacheKey = "user:" + id;
        
        // Get cached user with version
        CachedUser cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            // Check version
            User dbUser = userRepository.findById(id).orElse(null);
            if (dbUser != null && dbUser.getVersion().equals(cached.getVersion())) {
                return cached.getUser();
            }
        }
        
        // Fetch and cache
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            redisTemplate.opsForValue().set(
                cacheKey, 
                new CachedUser(user, user.getVersion()),
                1, TimeUnit.HOURS
            );
        }
        
        return user;
    }
}
```

---

## Implementation Examples

### Example 1: Multi-Level Cache

```java
@Service
public class ProductService {
    
    // L1: Local cache (Caffeine)
    private final Cache<Long, Product> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // L2: Distributed cache (Redis)
    @Autowired
    private RedisTemplate<String, Product> redisTemplate;
    
    // L3: Database
    @Autowired
    private ProductRepository productRepository;
    
    public Product getProduct(Long id) {
        // L1: Check local cache
        Product product = localCache.getIfPresent(id);
        if (product != null) {
            return product;
        }
        
        // L2: Check Redis
        String redisKey = "product:" + id;
        product = redisTemplate.opsForValue().get(redisKey);
        if (product != null) {
            localCache.put(id, product);
            return product;
        }
        
        // L3: Query database
        product = productRepository.findById(id).orElse(null);
        if (product != null) {
            // Update both caches
            localCache.put(id, product);
            redisTemplate.opsForValue().set(redisKey, product, 1, TimeUnit.HOURS);
        }
        
        return product;
    }
}
```

### Example 2: Cache Stampede Prevention

```java
@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, User> redisTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    private final ConcurrentHashMap<Long, CompletableFuture<User>> loadingCache = 
        new ConcurrentHashMap<>();
    
    public CompletableFuture<User> getUser(Long id) {
        String cacheKey = "user:" + id;
        
        // Check cache
        User cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Prevent stampede with single loading future
        return loadingCache.computeIfAbsent(id, key -> 
            CompletableFuture.supplyAsync(() -> {
                // Double-check cache
                User user = redisTemplate.opsForValue().get(cacheKey);
                if (user != null) {
                    return user;
                }
                
                // Load from database
                user = userRepository.findById(id).orElse(null);
                if (user != null) {
                    redisTemplate.opsForValue().set(cacheKey, user, 1, TimeUnit.HOURS);
                }
                
                return user;
            }).whenComplete((result, ex) -> {
                loadingCache.remove(id);
            })
        );
    }
}
```

### Example 3: Query Result Caching

```java
@Service
public class OrderService {
    
    @Autowired
    private RedisTemplate<String, List<Order>> redisTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    public List<Order> getUserOrders(Long userId, OrderStatus status) {
        // Create cache key from query parameters
        String cacheKey = String.format("orders:user:%d:status:%s", userId, status);
        
        // Check cache
        List<Order> orders = redisTemplate.opsForValue().get(cacheKey);
        if (orders != null) {
            return orders;
        }
        
        // Query database
        orders = orderRepository.findByUserIdAndStatus(userId, status);
        
        // Cache result
        redisTemplate.opsForValue().set(cacheKey, orders, 10, TimeUnit.MINUTES);
        
        return orders;
    }
    
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // Invalidate related caches
        String pattern = "orders:user:" + order.getUserId() + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

---

## Performance Analysis

### Cache Hit Ratio

```
Hit Ratio = Cache Hits / Total Requests

Example:
- Total requests: 10,000
- Cache hits: 9,000
- Cache misses: 1,000
- Hit ratio: 90%

Response time improvement:
- Without cache: 10,000 * 50ms = 500s
- With cache (90% hit): (9,000 * 1ms) + (1,000 * 50ms) = 59s
- Improvement: 8.5x faster!
```

### Memory vs Performance Trade-off

```
Cache Size | Hit Ratio | Memory | Response Time
-----------|-----------|--------|---------------
1MB        | 50%       | Low    | 25ms avg
10MB       | 75%       | Medium | 13ms avg
100MB      | 90%       | High   | 6ms avg
1GB        | 95%       | V.High | 3ms avg
```

### Benchmark Results

```
Test: 100,000 requests

No Cache:
- Avg response: 50ms
- Total time: 5000s
- Database load: 100%

Cache-Aside (90% hit):
- Avg response: 5.9ms
- Total time: 590s
- Database load: 10%
- Improvement: 8.5x

Read-Through (90% hit):
- Avg response: 6.1ms
- Total time: 610s
- Database load: 10%
- Improvement: 8.2x

Write-Through:
- Write latency: +5ms
- Read latency: 1ms
- Consistency: 100%
```

---

## Best Practices

### 1. Cache Key Design

```java
// ✅ Good: Structured, versioned keys
String key = "v1:user:" + userId;
String key = "v1:product:" + productId + ":details";
String key = "v1:orders:user:" + userId + ":status:" + status;

// ❌ Bad: Unstructured keys
String key = userId.toString();
String key = "user" + userId;
```

### 2. TTL Strategy

```java
// Different TTL for different data types
public enum CacheTTL {
    USER_PROFILE(1, TimeUnit.HOURS),      // Rarely changes
    PRODUCT_PRICE(5, TimeUnit.MINUTES),   // Changes frequently
    SESSION_DATA(30, TimeUnit.MINUTES),   // User session
    STATIC_CONTENT(24, TimeUnit.HOURS);   // Almost never changes
    
    private final long duration;
    private final TimeUnit unit;
}
```

### 3. Cache Warming

```java
@Component
public class CacheWarmer {
    
    @Autowired
    private ProductService productService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        // Load popular products into cache
        List<Long> popularProductIds = getPopularProductIds();
        
        popularProductIds.parallelStream()
            .forEach(productService::getProduct);
        
        log.info("Cache warmed with {} products", popularProductIds.size());
    }
}
```

### 4. Monitoring

```java
@Component
public class CacheMetrics {
    
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    
    public void recordHit() {
        hits.incrementAndGet();
    }
    
    public void recordMiss() {
        misses.incrementAndGet();
    }
    
    public double getHitRatio() {
        long totalHits = hits.get();
        long totalMisses = misses.get();
        long total = totalHits + totalMisses;
        
        return total == 0 ? 0 : (double) totalHits / total;
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void logMetrics() {
        log.info("Cache hit ratio: {}%", getHitRatio() * 100);
    }
}
```

### 5. Error Handling

```java
@Service
public class ResilientCacheService {
    
    public User getUser(Long id) {
        try {
            // Try cache first
            User user = redisTemplate.opsForValue().get("user:" + id);
            if (user != null) {
                return user;
            }
        } catch (Exception e) {
            log.error("Cache error, falling back to database", e);
        }
        
        // Fallback to database
        return userRepository.findById(id).orElse(null);
    }
}
```

---

## Summary

### Pattern Comparison

| Pattern | Use Case | Consistency | Performance | Complexity |
|---------|----------|-------------|-------------|------------|
| **Cache-Aside** | General purpose | Eventual | High | Low |
| **Read-Through** | Read-heavy | Eventual | High | Medium |
| **Write-Through** | Strong consistency | Strong | Medium | Medium |
| **Write-Behind** | Write-heavy | Eventual | Very High | High |
| **Refresh-Ahead** | Predictable access | Eventual | Very High | High |

### Decision Tree

```
Choose Cache Pattern:
├─ Need strong consistency?
│  └─ YES → Write-Through
│  └─ NO → Continue
│
├─ Write-heavy workload?
│  └─ YES → Write-Behind
│  └─ NO → Continue
│
├─ Predictable hot data?
│  └─ YES → Refresh-Ahead
│  └─ NO → Continue
│
├─ Simple implementation?
│  └─ YES → Cache-Aside
│  └─ NO → Read-Through
```

### Key Takeaways

1. **Cache-Aside** is most common and flexible
2. **Write-Through** for strong consistency
3. **Write-Behind** for high write throughput
4. **Multi-level caching** for best performance
5. **Monitor hit ratio** (target: >80%)
6. **Set appropriate TTL** based on data volatility
7. **Handle cache failures** gracefully
8. **Prevent cache stampede** with locking

Proper caching can improve performance by 10-100x! 🚀


---

## Advanced Cache Pattern Scenarios

### Scenario 1: E-Commerce Flash Sale

**Challenge**: 10K concurrent users buying limited inventory (100 items)

**Solution**: Multi-Pattern Approach
```
┌─────────────────────────────────────────────────────────────┐
│                    Flash Sale Architecture                  │
├─────────────────────────────────────────────────────────────┤
│ Product Details:     Refresh-Ahead (pre-warm before sale)  │
│ Inventory Count:     Write-Through + Redis (strong consistency) │
│ User Sessions:       Write-Behind (fast session updates)   │
│ Order Queue:         Write-Behind (async order processing)  │
│ Payment Status:      Write-Through (ACID guarantees)       │
└─────────────────────────────────────────────────────────────┘

Timeline:
T-10min: Refresh-Ahead warms product cache
T-0:     Sale starts, 10K users hit product page (cache HIT)
T+1s:    Users add to cart (Write-Behind, fast)
T+5s:    Checkout begins (Write-Through for inventory)
T+10s:   Inventory depleted, remaining users see "Sold Out"

Result:
- 99.9% cache hit rate
- Sub-second product page load
- Zero overselling (strong consistency)
- 100 successful orders in 10 seconds
```

### Scenario 2: Social Media Viral Post

**Challenge**: Post goes viral, 1M views in 1 hour

**Solution**: Adaptive Caching
```
Phase 1 (0-5 min): Cache-Aside
- Post is new, unpredictable traffic
- Normal cache behavior

Phase 2 (5-15 min): Detect viral pattern
- Access count > 1000/min
- Switch to Refresh-Ahead
- Proactively refresh every 30 seconds

Phase 3 (15-60 min): Peak traffic
- Refresh-Ahead maintains cache
- Zero cache misses
- DB load: 2 queries/min (refresh only)

Phase 4 (60+ min): Cool down
- Traffic decreases
- Switch back to Cache-Aside
- Let cache expire naturally

Performance:
- Without adaptive: 50K DB queries (5% miss rate)
- With adaptive: 120 DB queries (refresh only)
- DB load reduction: 99.76%
```

### Scenario 3: Multi-Region Deployment

**Challenge**: Global app with users in US, EU, Asia

**Solution**: Regional Cache with Write-Through
```
┌─────────────────────────────────────────────────────────────┐
│                   Global Architecture                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  US Region          EU Region          Asia Region         │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐        │
│  │ Redis    │      │ Redis    │      │ Redis    │        │
│  │ (Local)  │      │ (Local)  │      │ (Local)  │        │
│  └────┬─────┘      └────┬─────┘      └────┬─────┘        │
│       │                 │                 │               │
│       └─────────────────┼─────────────────┘               │
│                         │                                 │
│                  ┌──────▼──────┐                          │
│                  │  PostgreSQL │                          │
│                  │  (Primary)  │                          │
│                  └─────────────┘                          │
└─────────────────────────────────────────────────────────────┘

Strategy:
1. Reads: Cache-Aside from local Redis (1-5ms)
2. Writes: Write-Through to primary DB (50-200ms)
3. Cache Invalidation: Pub/Sub to all regions
4. Consistency: Strong (all regions invalidate)

Trade-offs:
- Fast reads (local cache)
- Slower writes (cross-region)
- Strong consistency (invalidate all)
- Higher complexity (multi-region sync)
```

### Scenario 4: Real-Time Gaming Leaderboard

**Challenge**: 1M players, scores update every second

**Solution**: Write-Behind with Sorted Sets
```java
@Service
public class LeaderboardService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Write-Behind: Update cache immediately
    public void updateScore(String playerId, long score) {
        // 1. Update Redis Sorted Set (1ms)
        redisTemplate.opsForZSet().add("leaderboard", playerId, score);
        
        // 2. Mark as dirty for async DB write
        redisTemplate.opsForSet().add("dirty:leaderboard", playerId);
        
        // 3. Return immediately (fast!)
    }
    
    // Background worker: Batch write to DB
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void flushToDatabase() {
        Set<String> dirtyPlayers = redisTemplate.opsForSet().members("dirty:leaderboard");
        
        if (dirtyPlayers.isEmpty()) return;
        
        // Batch write to DB
        List<PlayerScore> scores = new ArrayList<>();
        for (String playerId : dirtyPlayers) {
            Double score = redisTemplate.opsForZSet().score("leaderboard", playerId);
            scores.add(new PlayerScore(playerId, score));
        }
        
        // Single batch insert (efficient!)
        playerScoreRepository.saveAll(scores);
        
        // Clear dirty set
        redisTemplate.delete("dirty:leaderboard");
    }
    
    // Read from cache (always fast)
    public List<PlayerScore> getTopPlayers(int limit) {
        Set<ZSetOperations.TypedTuple<String>> top = 
            redisTemplate.opsForZSet().reverseRangeWithScores("leaderboard", 0, limit - 1);
        
        return top.stream()
            .map(t -> new PlayerScore(t.getValue(), t.getScore()))
            .collect(Collectors.toList());
    }
}
```

**Performance**:
```
Without Write-Behind:
- 1M score updates/sec
- 1M DB writes/sec
- Database overload!

With Write-Behind:
- 1M score updates/sec (cache)
- 200K DB writes/sec (batched, coalesced)
- 5x reduction in DB load
- Sub-millisecond response time
```

### Scenario 5: Financial Transaction System

**Challenge**: Bank transfers, zero data loss, strong consistency

**Solution**: Write-Through with 2PC (Two-Phase Commit)
```java
@Service
public class TransactionService {
    
    @Autowired
    private RedisTemplate<String, Account> redisTemplate;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Transactional
    public void transfer(String fromId, String toId, BigDecimal amount) {
        // Phase 1: Prepare (lock resources)
        String fromKey = "account:" + fromId;
        String toKey = "account:" + toId;
        
        // Lock accounts in cache and DB
        Account from = accountRepository.findByIdForUpdate(fromId);
        Account to = accountRepository.findByIdForUpdate(toId);
        
        // Validate
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // Phase 2: Commit (update both)
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        
        // Write-Through: Update cache AND database
        accountRepository.save(from);
        accountRepository.save(to);
        
        redisTemplate.opsForValue().set(fromKey, from);
        redisTemplate.opsForValue().set(toKey, to);
        
        // Both succeed or both rollback (ACID)
    }
}
```

**Guarantees**:
```
✅ Atomicity: Both accounts updated or neither
✅ Consistency: Cache = Database always
✅ Isolation: Locks prevent concurrent modifications
✅ Durability: Written to persistent storage

Performance:
- Latency: 50-100ms (acceptable for financial)
- Consistency: Strong (no stale data)
- Data Loss: Zero (write-through)
```

---

## Cache Pattern Anti-Patterns

### Anti-Pattern 1: Cache Everything

**Problem**: Caching all data wastes memory and reduces hit rate

```
❌ Bad:
@Cacheable("users")
public User getUser(Long id) { ... }

@Cacheable("orders")
public Order getOrder(Long id) { ... }

@Cacheable("products")
public Product getProduct(Long id) { ... }

// Result: Cache full of cold data, low hit rate

✅ Good:
// Only cache hot data
@Cacheable(value = "users", condition = "#result.isActive()")
public User getUser(Long id) { ... }

// Or use access-based caching
if (accessCount > 100) {
    cache.put(key, value);
}
```

### Anti-Pattern 2: Ignoring Cache Stampede

**Problem**: Cache expires, 1000 requests hit DB simultaneously

```
❌ Bad:
public User getUser(Long id) {
    User user = cache.get(id);
    if (user == null) {
        // 1000 threads execute this simultaneously!
        user = db.findById(id);
        cache.put(id, user);
    }
    return user;
}

✅ Good: Use locking
public User getUser(Long id) {
    User user = cache.get(id);
    if (user == null) {
        synchronized (("user:" + id).intern()) {
            // Double-check
            user = cache.get(id);
            if (user == null) {
                user = db.findById(id);
                cache.put(id, user);
            }
        }
    }
    return user;
}

✅ Better: Use probabilistic early expiration
public User getUser(Long id) {
    CacheEntry entry = cache.get(id);
    
    // Refresh if TTL < 10% and random chance
    if (entry.getTtl() < entry.getMaxTtl() * 0.1 && Math.random() < 0.1) {
        // One lucky thread refreshes
        asyncRefresh(id);
    }
    
    return entry.getValue();
}
```

### Anti-Pattern 3: Inconsistent Cache Keys

**Problem**: Same data cached under different keys

```
❌ Bad:
cache.put("user_" + id, user);        // Inconsistent
cache.put("user:" + id, user);        // Different format
cache.put("users/" + id, user);       // Another format

✅ Good: Consistent key format
public class CacheKeyGenerator {
    private static final String USER_PREFIX = "user:";
    private static final String ORDER_PREFIX = "order:";
    
    public static String userKey(Long id) {
        return USER_PREFIX + id;
    }
    
    public static String orderKey(Long id) {
        return ORDER_PREFIX + id;
    }
}
```

### Anti-Pattern 4: No Cache Monitoring

**Problem**: Can't detect cache issues or optimize

```
❌ Bad: No metrics

✅ Good: Comprehensive monitoring
@Service
public class CacheMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    public void recordCacheHit(String cacheName) {
        meterRegistry.counter("cache.hits", "cache", cacheName).increment();
    }
    
    public void recordCacheMiss(String cacheName) {
        meterRegistry.counter("cache.misses", "cache", cacheName).increment();
    }
    
    public void recordCacheEviction(String cacheName) {
        meterRegistry.counter("cache.evictions", "cache", cacheName).increment();
    }
    
    // Calculate hit rate
    public double getHitRate(String cacheName) {
        double hits = meterRegistry.counter("cache.hits", "cache", cacheName).count();
        double misses = meterRegistry.counter("cache.misses", "cache", cacheName).count();
        return hits / (hits + misses);
    }
}
```

---

## Summary: Choosing the Right Pattern

### Quick Decision Guide

```
┌─────────────────────────────────────────────────────────────┐
│                    Pattern Selection                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Use Cache-Aside when:                                     │
│  ✅ General-purpose caching                                 │
│  ✅ Read-heavy workload (80%+ reads)                        │
│  ✅ Unpredictable access patterns                           │
│  ✅ Need simple, maintainable solution                      │
│                                                             │
│  Use Read-Through when:                                    │
│  ✅ Using Spring Framework                                  │
│  ✅ Want transparent caching                                │
│  ✅ Consistent caching logic across app                     │
│                                                             │
│  Use Write-Through when:                                   │
│  ✅ Strong consistency required                             │
│  ✅ Financial transactions                                  │
│  ✅ Cannot tolerate stale data                              │
│  ✅ Read-after-write pattern                                │
│                                                             │
│  Use Write-Behind when:                                    │
│  ✅ Write-heavy workload (50%+ writes)                      │
│  ✅ Can tolerate data loss                                  │
│  ✅ Analytics, logging, metrics                             │
│  ✅ Need high write throughput                              │
│                                                             │
│  Use Refresh-Ahead when:                                   │
│  ✅ Predictable hot data                                    │
│  ✅ Latency-sensitive reads                                 │
│  ✅ Expensive DB queries                                    │
│  ✅ High read volume on same keys                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Pattern Combinations (Production)

Most real-world systems use multiple patterns:

1. **E-Commerce**:
   - Product catalog: Cache-Aside
   - Inventory: Write-Through
   - User sessions: Write-Behind
   - Popular products: Refresh-Ahead

2. **Social Media**:
   - User profiles: Read-Through
   - Like counts: Write-Behind
   - News feed: Cache-Aside
   - Trending: Refresh-Ahead

3. **Banking**:
   - Account balance: Write-Through
   - Transaction history: Cache-Aside
   - Audit logs: Write-Behind
   - Exchange rates: Refresh-Ahead

4. **Gaming**:
   - Player stats: Write-Behind
   - Leaderboards: Write-Behind + Sorted Sets
   - Game state: Write-Through
   - Popular games: Refresh-Ahead

### Key Takeaways

1. **No silver bullet**: Different patterns for different use cases
2. **Start simple**: Cache-Aside is a good default
3. **Measure everything**: Monitor hit rate, latency, DB load
4. **Iterate**: Start with one pattern, optimize based on metrics
5. **Combine patterns**: Use multiple patterns in same system
6. **Consider trade-offs**: Consistency vs Performance vs Complexity

---

## Further Reading

- [Redis Documentation](https://redis.io/docs/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Cache Stampede Problem](https://en.wikipedia.org/wiki/Cache_stampede)
- [Two-Phase Commit](https://en.wikipedia.org/wiki/Two-phase_commit_protocol)
