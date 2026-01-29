# Redis Deep Dive - Spring Boot Integration & Performance

## Overview

**Redis** (Remote Dictionary Server) is an in-memory data structure store used as database, cache, message broker, and streaming engine.

**Key Features**:
- In-memory storage (sub-millisecond latency)
- Supports multiple data structures
- Persistence options (RDB, AOF)
- Replication and clustering
- Pub/Sub messaging
- Atomic operations

---

## Redis Data Structures

### Comparison Table

| Data Structure | Use Case | Time Complexity | Example |
|---------------|----------|-----------------|---------|
| **String** | Cache, counters, sessions | O(1) | User session |
| **Hash** | Objects, user profiles | O(1) | User details |
| **List** | Queues, timelines | O(1) head/tail | Activity feed |
| **Set** | Unique items, tags | O(1) | User followers |
| **Sorted Set** | Leaderboards, rankings | O(log N) | Game scores |
| **Bitmap** | Analytics, flags | O(1) | Daily active users |
| **HyperLogLog** | Cardinality estimation | O(1) | Unique visitors |
| **Geospatial** | Location-based | O(log N) | Nearby drivers |
| **Stream** | Event sourcing, logs | O(1) | Audit logs |

---

## 1. String Operations

### Basic Commands

```bash
# Set/Get
SET user:1000:name "John Doe"
GET user:1000:name

# Set with expiry (TTL)
SETEX session:abc123 3600 "user_data"  # Expires in 1 hour

# Increment/Decrement
SET page:views 0
INCR page:views        # 1
INCRBY page:views 10   # 11
DECR page:views        # 10

# Multiple operations
MSET user:1:name "John" user:2:name "Jane"
MGET user:1:name user:2:name
```

### Spring Boot Example

```java
@Service
public class CacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public void cacheUser(String userId, String userData) {
        redisTemplate.opsForValue().set("user:" + userId, userData, 1, TimeUnit.HOURS);
    }
    
    public String getUser(String userId) {
        return redisTemplate.opsForValue().get("user:" + userId);
    }
    
    public Long incrementPageViews(String pageId) {
        return redisTemplate.opsForValue().increment("page:" + pageId + ":views");
    }
}
```

---

## 2. Hash Operations (HCache Structure)

### Hash Structure in Redis

```
Key: user:1000
┌─────────────────────────────────┐
│ Field      │ Value              │
├────────────┼────────────────────┤
│ name       │ John Doe           │
│ email      │ john@example.com   │
│ age        │ 30                 │
│ city       │ New York           │
│ created_at │ 2024-01-15         │
└─────────────────────────────────┘
```

### Hash Commands

```bash
# Set hash fields
HSET user:1000 name "John Doe"
HSET user:1000 email "john@example.com"
HSET user:1000 age 30

# Get hash fields
HGET user:1000 name
HGETALL user:1000

# Multiple fields
HMSET user:1000 name "John" email "john@example.com" age 30
HMGET user:1000 name email

# Check field exists
HEXISTS user:1000 name

# Delete field
HDEL user:1000 age

# Get all keys/values
HKEYS user:1000
HVALS user:1000

# Increment field
HINCRBY user:1000 login_count 1
```

---

## Spring Boot Redis Configuration

### 1. Dependencies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Data Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Lettuce (default) or Jedis -->
    <dependency>
        <groupId>io.lettuce</groupId>
        <artifactId>lettuce-core</artifactId>
    </dependency>
    
    <!-- Spring Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- JSON serialization -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

### 2. Application Configuration

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 2000ms
    
    # Lettuce pool configuration
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
    
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour in milliseconds
      cache-null-values: false
      key-prefix: "myapp:"
      use-key-prefix: true
```

---

### 3. Redis Configuration Class

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // JSON serialization
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        serializer.setObjectMapper(mapper);
        
        // Key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value serializer
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## HCache (Hash Cache) Implementation

### 1. User Profile Cache with Hash

```java
@Service
public class UserCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_CACHE_PREFIX = "user:";
    
    // Save user as hash
    public void cacheUser(User user) {
        String key = USER_CACHE_PREFIX + user.getId();
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("age", user.getAge());
        userMap.put("city", user.getCity());
        
        redisTemplate.opsForHash().putAll(key, userMap);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
    
    // Get user from hash
    public User getUser(String userId) {
        String key = USER_CACHE_PREFIX + userId;
        
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        
        User user = new User();
        user.setId((String) entries.get("id"));
        user.setName((String) entries.get("name"));
        user.setEmail((String) entries.get("email"));
        user.setAge((Integer) entries.get("age"));
        user.setCity((String) entries.get("city"));
        
        return user;
    }
    
    // Update specific field
    public void updateUserField(String userId, String field, Object value) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    // Get specific field
    public Object getUserField(String userId, String field) {
        String key = USER_CACHE_PREFIX + userId;
        return redisTemplate.opsForHash().get(key, field);
    }
    
    // Delete user
    public void deleteUser(String userId) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
```

---

### 2. Product Inventory Cache with Hash

```java
@Service
public class ProductCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PRODUCT_CACHE = "products";
    
    // Cache all products in single hash
    public void cacheProduct(Product product) {
        redisTemplate.opsForHash().put(
            PRODUCT_CACHE,
            product.getId(),
            product
        );
    }
    
    // Get product
    public Product getProduct(String productId) {
        return (Product) redisTemplate.opsForHash().get(PRODUCT_CACHE, productId);
    }
    
    // Get all products
    public List<Product> getAllProducts() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(PRODUCT_CACHE);
        return entries.values().stream()
            .map(obj -> (Product) obj)
            .collect(Collectors.toList());
    }
    
    // Update stock
    public void updateStock(String productId, int quantity) {
        Product product = getProduct(productId);
        if (product != null) {
            product.setStock(quantity);
            cacheProduct(product);
        }
    }
    
    // Increment stock atomically
    public Long incrementStock(String productId, int delta) {
        return redisTemplate.opsForHash().increment(
            PRODUCT_CACHE,
            productId + ":stock",
            delta
        );
    }
}
```

---

## Spring Cache Annotations

### 1. @Cacheable - Cache Method Result

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Cacheable(value = "users", key = "#userId")
    public User findById(String userId) {
        System.out.println("Fetching from database: " + userId);
        return userRepository.findById(userId).orElse(null);
    }
    
    @Cacheable(value = "users", key = "#email", unless = "#result == null")
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
```

**First call**: Fetches from DB, caches result  
**Subsequent calls**: Returns from cache (no DB hit)

---

### 2. @CachePut - Update Cache

```java
@Service
public class UserService {
    
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        System.out.println("Updating user: " + user.getId());
        return userRepository.save(user);
    }
}
```

**Behavior**: Always executes method, updates cache with result

---

### 3. @CacheEvict - Remove from Cache

```java
@Service
public class UserService {
    
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }
}
```

---

### 4. @Caching - Multiple Cache Operations

```java
@Service
public class UserService {
    
    @Caching(
        put = {
            @CachePut(value = "users", key = "#user.id"),
            @CachePut(value = "usersByEmail", key = "#user.email")
        },
        evict = {
            @CacheEvict(value = "allUsers", allEntries = true)
        }
    )
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

---

## Performance Improvements

### Before Redis (Database Only)

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    public Order getOrder(String orderId) {
        // Every call hits database
        return orderRepository.findById(orderId).orElse(null);
    }
}
```

**Performance**:
```
Database Query: 50-100ms per request
1000 requests: 50,000-100,000ms (50-100 seconds)
```

---

### After Redis (With Caching)

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Cacheable(value = "orders", key = "#orderId")
    public Order getOrder(String orderId) {
        // First call: DB hit
        // Subsequent calls: Redis hit
        return orderRepository.findById(orderId).orElse(null);
    }
}
```

**Performance**:
```
First request: 50-100ms (DB)
Cached requests: 1-5ms (Redis)
1000 requests: 100ms + (999 × 2ms) = 2,098ms (~2 seconds)

Improvement: 25-50x faster
```

---

## Real-World Performance Benchmarks

### Benchmark 1: User Profile Lookup

```java
@Service
public class UserProfileBenchmark {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Without cache
    public User getUserFromDB(String userId) {
        long start = System.currentTimeMillis();
        User user = userRepository.findById(userId).orElse(null);
        long end = System.currentTimeMillis();
        System.out.println("DB lookup: " + (end - start) + "ms");
        return user;
    }
    
    // With Redis cache
    public User getUserFromCache(String userId) {
        long start = System.currentTimeMillis();
        
        String key = "user:" + userId;
        User user = (User) redisTemplate.opsForValue().get(key);
        
        if (user == null) {
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
            }
        }
        
        long end = System.currentTimeMillis();
        System.out.println("Cache lookup: " + (end - start) + "ms");
        return user;
    }
}
```

**Results**:
```
DB lookup: 85ms
Cache lookup (miss): 87ms (DB + cache write)
Cache lookup (hit): 2ms

Cache hit ratio: 95%
Average latency: (5% × 87ms) + (95% × 2ms) = 6.25ms

Improvement: 13.6x faster
```

---

### Benchmark 2: Product Catalog

```java
@Service
public class ProductCatalogService {
    
    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Cacheable(value = "products", key = "#category")
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
```

**Results**:
```
Without Cache:
- Query 1000 products: 250ms
- 100 requests/sec: 25,000ms (25 seconds)

With Cache:
- First request: 250ms (DB + cache)
- Cached requests: 3ms
- 100 requests/sec: 250ms + (99 × 3ms) = 547ms

Improvement: 45x faster
```

---

## Advanced Redis Patterns

### 1. Cache-Aside Pattern

```java
@Service
public class CacheAsideService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    public User getUser(String userId) {
        String key = "user:" + userId;
        
        // 1. Try cache first
        User user = (User) redisTemplate.opsForValue().get(key);
        
        if (user != null) {
            return user; // Cache hit
        }
        
        // 2. Cache miss - fetch from DB
        user = userRepository.findById(userId).orElse(null);
        
        if (user != null) {
            // 3. Write to cache
            redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
        }
        
        return user;
    }
    
    public void updateUser(User user) {
        // 1. Update database
        userRepository.save(user);
        
        // 2. Invalidate cache
        String key = "user:" + user.getId();
        redisTemplate.delete(key);
    }
}
```

---

### 2. Write-Through Pattern

```java
@Service
public class WriteThroughService {
    
    public void updateUser(User user) {
        // 1. Update cache first
        String key = "user:" + user.getId();
        redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
        
        // 2. Update database
        userRepository.save(user);
    }
}
```

---

### 3. Read-Through Pattern

```java
@Service
public class ReadThroughService {
    
    @Cacheable(value = "users", key = "#userId")
    public User getUser(String userId) {
        // Spring automatically handles:
        // 1. Check cache
        // 2. If miss, execute method
        // 3. Cache result
        return userRepository.findById(userId).orElse(null);
    }
}
```

---

## Session Management with Redis

### Spring Session Configuration

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class SessionConfig {
    
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }
}

@RestController
public class SessionController {
    
    @GetMapping("/session/set")
    public String setSession(HttpSession session) {
        session.setAttribute("user", "John Doe");
        return "Session set";
    }
    
    @GetMapping("/session/get")
    public String getSession(HttpSession session) {
        return (String) session.getAttribute("user");
    }
}
```

**Benefits**:
- Distributed sessions across multiple servers
- Session persistence
- Fast session lookup (1-2ms)

---

## Rate Limiting with Redis

```java
@Service
public class RateLimiterService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public boolean isAllowed(String userId, int maxRequests, int windowSeconds) {
        String key = "rate_limit:" + userId;
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        
        return currentCount <= maxRequests;
    }
}

@RestController
public class ApiController {
    
    @Autowired
    private RateLimiterService rateLimiter;
    
    @GetMapping("/api/data")
    public ResponseEntity<?> getData(@RequestHeader("User-Id") String userId) {
        if (!rateLimiter.isAllowed(userId, 100, 60)) {
            return ResponseEntity.status(429).body("Rate limit exceeded");
        }
        
        return ResponseEntity.ok("Data");
    }
}
```

---

## Distributed Lock with Redis

```java
@Service
public class DistributedLockService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public boolean acquireLock(String lockKey, String lockValue, long expireTime) {
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
    
    public void releaseLock(String lockKey, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }
}

@Service
public class OrderService {
    
    @Autowired
    private DistributedLockService lockService;
    
    public void processOrder(String orderId) {
        String lockKey = "order_lock:" + orderId;
        String lockValue = UUID.randomUUID().toString();
        
        if (lockService.acquireLock(lockKey, lockValue, 30)) {
            try {
                // Process order (only one instance)
                processOrderLogic(orderId);
            } finally {
                lockService.releaseLock(lockKey, lockValue);
            }
        } else {
            throw new RuntimeException("Order is being processed");
        }
    }
}
```

---

## Monitoring and Metrics

### Redis Metrics Service

```java
@Service
public class RedisMetricsService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Scheduled(fixedRate = 60000)
    public void logMetrics() {
        Properties info = redisTemplate.getConnectionFactory()
            .getConnection()
            .info();
        
        log.info("=== Redis Metrics ===");
        log.info("Connected clients: {}", info.getProperty("connected_clients"));
        log.info("Used memory: {}", info.getProperty("used_memory_human"));
        log.info("Total commands: {}", info.getProperty("total_commands_processed"));
        log.info("Keyspace hits: {}", info.getProperty("keyspace_hits"));
        log.info("Keyspace misses: {}", info.getProperty("keyspace_misses"));
        
        // Calculate hit ratio
        long hits = Long.parseLong(info.getProperty("keyspace_hits"));
        long misses = Long.parseLong(info.getProperty("keyspace_misses"));
        double hitRatio = (double) hits / (hits + misses) * 100;
        log.info("Cache hit ratio: {:.2f}%", hitRatio);
    }
}
```

---

## Best Practices

### ✅ Do's

```java
// 1. Set expiration on all keys
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

// 2. Use connection pooling
spring.redis.lettuce.pool.max-active=20

// 3. Handle cache misses gracefully
User user = (User) redisTemplate.opsForValue().get(key);
if (user == null) {
    user = userRepository.findById(userId).orElse(null);
}

// 4. Use appropriate data structures
// Hash for objects, Set for unique items, Sorted Set for rankings

// 5. Monitor cache hit ratio
double hitRatio = hits / (hits + misses);
```

---

### ❌ Don'ts

```java
// 1. Don't store large objects
// Bad: 10MB object in cache
redisTemplate.opsForValue().set(key, largeObject);

// 2. Don't forget expiration
redisTemplate.opsForValue().set(key, value); // No TTL - memory leak!

// 3. Don't use Redis for everything
// Bad: Storing 1GB file in Redis

// 4. Don't ignore connection pool limits
// Bad: Creating new connection for each request

// 5. Don't cache frequently changing data
// Bad: Caching stock prices that change every second
```

---

## Performance Summary

### Latency Comparison

| Operation | Database | Redis | Improvement |
|-----------|----------|-------|-------------|
| **Single Read** | 50-100ms | 1-5ms | 10-100x |
| **Batch Read (100)** | 5,000ms | 50ms | 100x |
| **Write** | 50ms | 2ms | 25x |
| **Complex Query** | 500ms | 5ms | 100x |

### Throughput Comparison

| Metric | Database | Redis | Improvement |
|--------|----------|-------|-------------|
| **Reads/sec** | 1,000 | 100,000 | 100x |
| **Writes/sec** | 500 | 50,000 | 100x |
| **Concurrent Users** | 1,000 | 100,000 | 100x |

---

## Interview Questions

### Q1: What is Redis and when to use it?

**Answer**: In-memory data store for caching, session management, real-time analytics. Use for frequently accessed data with low latency requirements.

---

### Q2: Difference between Redis String and Hash?

**Answer**: 
- **String**: Single value, good for simple cache
- **Hash**: Multiple fields, good for objects, memory efficient

---

### Q3: How does Redis achieve high performance?

**Answer**: In-memory storage, single-threaded (no locks), efficient data structures, pipelining, multiplexing.

---

### Q4: What is cache-aside pattern?

**Answer**: Application checks cache first, on miss fetches from DB and updates cache. Most common pattern.

---

### Q5: How to handle cache invalidation?

**Answer**: TTL expiration, manual eviction on updates, LRU eviction policy, cache warming.

---

## Key Takeaways

1. **Redis is 10-100x faster** than database queries
2. **Use Hash for objects** - memory efficient
3. **Always set TTL** to prevent memory leaks
4. **Cache hit ratio > 80%** is good
5. **Connection pooling** is essential
6. **Monitor metrics** continuously
7. **Use appropriate data structures** for use case
8. **Handle cache misses** gracefully
9. **Distributed sessions** with Spring Session
10. **Rate limiting** and distributed locks with Redis

---

## Practice Problems

1. Implement user profile cache with Hash
2. Build rate limiter with sliding window
3. Create distributed lock for order processing
4. Implement leaderboard with Sorted Set
5. Build session management with Redis
