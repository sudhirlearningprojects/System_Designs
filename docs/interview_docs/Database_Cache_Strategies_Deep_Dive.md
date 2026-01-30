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

---

## Cache Patterns

### 1. Cache-Aside (Lazy Loading)

**Most common pattern** - Application manages cache explicitly.

```
Read Flow:
1. Check cache
2. If HIT → return data
3. If MISS → query database
4. Store in cache
5. Return data

Write Flow:
1. Write to database
2. Invalidate cache
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
- ✅ Simple to implement
- ✅ Cache only requested data
- ✅ Resilient to cache failures

**Cons**:
- ❌ Cache miss penalty (3 round trips)
- ❌ Stale data possible
- ❌ Cache stampede risk

---

### 2. Read-Through Cache

Cache sits between application and database, automatically loads data on miss.

```
Read Flow:
1. Application requests from cache
2. Cache checks if data exists
3. If MISS → cache loads from database
4. Cache returns data

Write Flow:
1. Write to database
2. Invalidate cache
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
- ✅ Transparent to application
- ✅ Consistent caching logic
- ✅ Less code duplication

**Cons**:
- ❌ Tight coupling with cache
- ❌ Cache failure affects reads

---

### 3. Write-Through Cache

Data written to cache and database simultaneously.

```
Write Flow:
1. Write to cache
2. Cache writes to database (synchronously)
3. Return success

Read Flow:
1. Read from cache
2. Always HIT (if data exists)
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
- ✅ Cache always consistent
- ✅ No cache miss penalty
- ✅ Data durability

**Cons**:
- ❌ Write latency (2 operations)
- ❌ Wasted cache space (unused data)
- ❌ Cache failure affects writes

---

### 4. Write-Behind (Write-Back) Cache

Data written to cache immediately, database updated asynchronously.

```
Write Flow:
1. Write to cache (fast)
2. Return success immediately
3. Async worker writes to database

Read Flow:
1. Read from cache
2. Always HIT (if data exists)
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
- ✅ Fast writes (no DB wait)
- ✅ Reduced database load
- ✅ Batch writes possible

**Cons**:
- ❌ Data loss risk (cache failure)
- ❌ Complex consistency
- ❌ Requires background workers

---

### 5. Refresh-Ahead Cache

Proactively refresh cache before expiration.

```
Flow:
1. Cache monitors TTL
2. Before expiration, refresh from database
3. Update cache with fresh data
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
- ✅ No cache miss for hot data
- ✅ Predictable performance
- ✅ Reduced database load

**Cons**:
- ❌ Complex implementation
- ❌ Wasted refreshes
- ❌ Requires prediction logic

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
