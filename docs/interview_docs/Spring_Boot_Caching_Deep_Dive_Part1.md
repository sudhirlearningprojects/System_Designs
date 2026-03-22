# Spring Boot Caching Deep Dive - Part 1

## Table of Contents
- [Introduction to Caching](#introduction-to-caching)
- [When to Use Caching](#when-to-use-caching)
- [Cache Providers in Spring Boot](#cache-providers-in-spring-boot)
- [Basic Implementation](#basic-implementation)
- [Cache Annotations](#cache-annotations)
- [Cache Configuration](#cache-configuration)
- [Redis Cache Implementation](#redis-cache-implementation)

---

## Introduction to Caching

### What is Caching?

Caching is a technique to store frequently accessed data in a fast-access storage layer (cache) to reduce:
- Database queries
- External API calls
- Expensive computations
- Network latency

### Cache Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Application    │
│   (Service)     │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐      Cache Hit (Fast)
│  Cache Layer    │◄─────────────────────┐
│  (Redis/Caffeine)│                      │
└──────┬──────────┘                       │
       │ Cache Miss                        │
       ▼                                   │
┌─────────────────┐                       │
│    Database     │───────────────────────┘
└─────────────────┘
```

### Performance Impact

**Without Cache:**
```
Request → Service → Database (100ms) → Response
Total: ~100ms per request
```

**With Cache:**
```
Request → Service → Cache (1-5ms) → Response
Total: ~5ms per request (20x faster!)
```

---

## When to Use Caching

### ✅ Perfect Use Cases

#### 1. **Read-Heavy Applications**
```java
// Product catalog - rarely changes, frequently read
@Cacheable("products")
public Product getProduct(Long id) {
    return productRepository.findById(id).orElseThrow();
}
```

**Metrics:**
- Read:Write ratio > 10:1
- 95% cache hit rate achievable
- Response time: 100ms → 5ms

#### 2. **Expensive Computations**
```java
// Complex analytics calculation
@Cacheable(value = "analytics", key = "#userId + '-' + #startDate + '-' + #endDate")
public AnalyticsReport generateReport(Long userId, LocalDate startDate, LocalDate endDate) {
    // Complex aggregation taking 5-10 seconds
    return analyticsService.computeReport(userId, startDate, endDate);
}
```

#### 3. **External API Calls**
```java
// Weather API - data changes every 30 minutes
@Cacheable(value = "weather", key = "#city")
public WeatherData getWeather(String city) {
    return weatherApiClient.fetchWeather(city); // 500ms API call
}
```

#### 4. **Session Data**
```java
// User session information
@Cacheable(value = "userSessions", key = "#sessionId")
public UserSession getSession(String sessionId) {
    return sessionRepository.findById(sessionId).orElseThrow();
}
```

#### 5. **Configuration Data**
```java
// Application settings - rarely change
@Cacheable("appConfig")
public Map<String, String> getApplicationConfig() {
    return configRepository.findAll()
        .stream()
        .collect(Collectors.toMap(Config::getKey, Config::getValue));
}
```

### ❌ When NOT to Use Caching

#### 1. **Real-Time Data**
```java
// Stock prices - must be real-time
public StockPrice getCurrentPrice(String symbol) {
    return stockService.getLivePrice(symbol); // Don't cache!
}
```

#### 2. **Frequently Changing Data**
```java
// Shopping cart - changes every few seconds
public Cart getCart(Long userId) {
    return cartRepository.findByUserId(userId); // Don't cache!
}
```

#### 3. **User-Specific Sensitive Data**
```java
// Credit card details - security risk
public PaymentMethod getPaymentMethod(Long userId) {
    return paymentRepository.findByUserId(userId); // Don't cache!
}
```

#### 4. **Large Objects**
```java
// Video files - too large for cache
public byte[] getVideo(Long videoId) {
    return videoStorage.getVideo(videoId); // Use CDN instead!
}
```

#### 5. **Write-Heavy Operations**
```java
// Order creation - always new data
public Order createOrder(OrderRequest request) {
    return orderRepository.save(new Order(request)); // Don't cache!
}
```

---

## Cache Providers in Spring Boot

### Comparison Table

| Provider | Type | Speed | Distributed | TTL | Eviction | Best For |
|----------|------|-------|-------------|-----|----------|----------|
| **Caffeine** | In-Memory | Fastest | ❌ | ✅ | ✅ | Single instance apps |
| **Redis** | In-Memory | Very Fast | ✅ | ✅ | ✅ | Distributed apps |
| **Hazelcast** | In-Memory | Fast | ✅ | ✅ | ✅ | Clustered apps |
| **EhCache** | In-Memory/Disk | Fast | ✅ | ✅ | ✅ | Enterprise apps |
| **Memcached** | In-Memory | Very Fast | ✅ | ✅ | ✅ | Simple key-value |
| **Simple** | ConcurrentHashMap | Fastest | ❌ | ❌ | ❌ | Development only |

### Provider Selection Guide

```
┌─────────────────────────────────────┐
│  Single Instance Application?       │
│  ├─ Yes → Caffeine                  │
│  └─ No → Continue                   │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  Need Distributed Cache?            │
│  ├─ Yes → Continue                  │
│  └─ No → Caffeine                   │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  Need Persistence?                  │
│  ├─ Yes → Redis                     │
│  └─ No → Continue                   │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│  Complex Data Structures?           │
│  ├─ Yes → Redis                     │
│  └─ No → Memcached                  │
└─────────────────────────────────────┘
```

---

## Basic Implementation

### Step 1: Add Dependencies

**For Caffeine (In-Memory):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

**For Redis:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Step 2: Enable Caching

```java
@SpringBootApplication
@EnableCaching  // Enable Spring Cache abstraction
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Step 3: Configure Cache

**application.yml:**
```yaml
spring:
  cache:
    type: caffeine  # or redis, ehcache, hazelcast
    cache-names:
      - users
      - products
      - orders
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

### Step 4: Use Cache Annotations

```java
@Service
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        log.info("Fetching user from database: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        log.info("Updating user in database: {}", user.getId());
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        log.info("Deleting user from database: {}", id);
        userRepository.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsers() {
        log.info("Clearing all users from cache");
    }
}
```

---

## Cache Annotations

### 1. @Cacheable - Read from Cache

**Purpose:** Cache method result. If cached, method is not executed.

```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    // Only executed on cache miss
    return productRepository.findById(id).orElseThrow();
}
```

**Flow:**
```
1. Check cache for key "products::123"
2. If found → Return cached value (method not executed)
3. If not found → Execute method → Store result in cache → Return result
```

**Advanced Usage:**

```java
// Conditional caching
@Cacheable(value = "products", key = "#id", condition = "#id > 0")
public Product getProduct(Long id) {
    return productRepository.findById(id).orElseThrow();
}

// Cache only if result is not null
@Cacheable(value = "products", key = "#id", unless = "#result == null")
public Product getProduct(Long id) {
    return productRepository.findById(id).orElse(null);
}

// Composite key
@Cacheable(value = "orders", key = "#userId + '-' + #orderId")
public Order getOrder(Long userId, Long orderId) {
    return orderRepository.findByUserIdAndOrderId(userId, orderId);
}

// SpEL expression
@Cacheable(value = "users", key = "#user.email")
public User getUserByEmail(User user) {
    return userRepository.findByEmail(user.getEmail());
}
```

### 2. @CachePut - Update Cache

**Purpose:** Always execute method and update cache with result.

```java
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    // Always executed, result stored in cache
    return userRepository.save(user);
}
```

**Flow:**
```
1. Execute method
2. Store result in cache with key "users::123"
3. Return result
```

**Use Cases:**
```java
// Update after creation
@CachePut(value = "products", key = "#result.id")
public Product createProduct(ProductRequest request) {
    Product product = new Product(request);
    return productRepository.save(product);
}

// Update multiple caches
@CachePut(value = "users", key = "#user.id")
@CachePut(value = "usersByEmail", key = "#user.email")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

### 3. @CacheEvict - Remove from Cache

**Purpose:** Remove entries from cache.

```java
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

**Flow:**
```
1. Execute method
2. Remove entry with key "users::123" from cache
```

**Advanced Usage:**

```java
// Clear all entries
@CacheEvict(value = "users", allEntries = true)
public void clearAllUsers() {
    // All entries in "users" cache removed
}

// Clear before method execution
@CacheEvict(value = "users", key = "#id", beforeInvocation = true)
public void deleteUser(Long id) {
    // Cache cleared even if method throws exception
    userRepository.deleteById(id);
}

// Clear multiple caches
@CacheEvict(value = {"users", "usersByEmail"}, key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

### 4. @Caching - Multiple Cache Operations

**Purpose:** Combine multiple cache annotations.

```java
@Caching(
    cacheable = {
        @Cacheable(value = "users", key = "#id")
    },
    put = {
        @CachePut(value = "usersByEmail", key = "#result.email")
    }
)
public User getUserById(Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

**Complex Example:**

```java
@Caching(
    evict = {
        @CacheEvict(value = "users", key = "#user.id"),
        @CacheEvict(value = "usersByEmail", key = "#user.email"),
        @CacheEvict(value = "userStats", allEntries = true)
    }
)
public void deleteUser(User user) {
    userRepository.delete(user);
}
```

### 5. @CacheConfig - Class-Level Configuration

**Purpose:** Define common cache configuration for all methods in class.

```java
@Service
@CacheConfig(cacheNames = "users")
public class UserService {
    
    @Cacheable(key = "#id")  // Uses "users" cache
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    @CachePut(key = "#user.id")  // Uses "users" cache
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(key = "#id")  // Uses "users" cache
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

---

## Cache Configuration

### Caffeine Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "users", "products", "orders"
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(1000)                    // Max 1000 entries
            .expireAfterWrite(10, TimeUnit.MINUTES)  // TTL: 10 minutes
            .recordStats();                       // Enable statistics
    }
}
```

**Advanced Caffeine Configuration:**

```java
@Configuration
@EnableCaching
public class AdvancedCacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        List<CaffeineCache> caches = Arrays.asList(
            buildCache("users", 1000, 10),
            buildCache("products", 5000, 30),
            buildCache("orders", 500, 5)
        );
        
        cacheManager.setCaches(caches);
        return cacheManager;
    }
    
    private CaffeineCache buildCache(String name, int maxSize, int ttlMinutes) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build());
    }
}
```

**Eviction Policies:**

```java
// Size-based eviction
Caffeine.newBuilder()
    .maximumSize(1000)  // Max 1000 entries
    .build();

// Weight-based eviction
Caffeine.newBuilder()
    .maximumWeight(10_000)
    .weigher((key, value) -> ((String) value).length())
    .build();

// Time-based eviction
Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)  // Fixed TTL
    .expireAfterAccess(5, TimeUnit.MINUTES)  // Idle timeout
    .build();

// Refresh after write
Caffeine.newBuilder()
    .refreshAfterWrite(1, TimeUnit.MINUTES)  // Async refresh
    .build();
```

---

## Redis Cache Implementation

### Configuration

**application.yml:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes in milliseconds
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "myapp:"
  
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2
        max-wait: -1ms
```

### Redis Cache Configuration

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build();
    }
}
```

### Custom TTL per Cache

```java
@Configuration
@EnableCaching
public class CustomRedisCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        // Custom configurations per cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Users cache: 30 minutes TTL
        cacheConfigurations.put("users", 
            defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Products cache: 1 hour TTL
        cacheConfigurations.put("products", 
            defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Orders cache: 5 minutes TTL
        cacheConfigurations.put("orders", 
            defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Session cache: 24 hours TTL
        cacheConfigurations.put("sessions", 
            defaultConfig.entryTtl(Duration.ofHours(24)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
```

### Redis Serialization Options

```java
// 1. JSON Serialization (Recommended for complex objects)
new GenericJackson2JsonRedisSerializer()

// 2. JDK Serialization (Requires Serializable)
new JdkSerializationRedisSerializer()

// 3. String Serialization (For simple strings)
new StringRedisSerializer()

// 4. Custom Serialization
public class CustomRedisSerializer implements RedisSerializer<Object> {
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        // Custom serialization logic
    }
    
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        // Custom deserialization logic
    }
}
```

### Redis Cache with Compression

```java
@Configuration
@EnableCaching
public class CompressedRedisCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new CompressingRedisSerializer()
                )
            );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}

public class CompressingRedisSerializer implements RedisSerializer<Object> {
    
    private final GenericJackson2JsonRedisSerializer jsonSerializer = 
        new GenericJackson2JsonRedisSerializer();
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        byte[] json = jsonSerializer.serialize(obj);
        if (json == null) return null;
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(json);
            gzip.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to compress", e);
        }
    }
    
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) return null;
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            return jsonSerializer.deserialize(baos.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Failed to decompress", e);
        }
    }
}
```

### Service Implementation with Redis

```java
@Service
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    
    // Using Spring Cache abstraction
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        log.info("Fetching product from database: {}", id);
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
    
    // Direct Redis operations (when you need more control)
    public Product getProductDirect(Long id) {
        String key = "product:" + id;
        
        // Try cache first
        Product cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("Cache hit for product: {}", id);
            return cached;
        }
        
        // Cache miss - fetch from database
        log.info("Cache miss for product: {}", id);
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Store in cache with 10 minute TTL
        redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
        
        return product;
    }
    
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        log.info("Updating product: {}", product.getId());
        return productRepository.save(product);
    }
    
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }
}
```

### Redis Data Structures for Caching

```java
@Service
public class AdvancedRedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Hash - Store object fields separately
    public void cacheUserAsHash(User user) {
        String key = "user:" + user.getId();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        
        redisTemplate.opsForHash().putAll(key, userMap);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }
    
    // List - Cache recent items
    public void cacheRecentOrders(Long userId, Order order) {
        String key = "recent_orders:" + userId;
        redisTemplate.opsForList().leftPush(key, order);
        redisTemplate.opsForList().trim(key, 0, 9);  // Keep only 10 recent
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
    
    // Set - Cache unique items
    public void cacheUserTags(Long userId, Set<String> tags) {
        String key = "user_tags:" + userId;
        redisTemplate.opsForSet().add(key, tags.toArray());
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }
    
    // Sorted Set - Cache leaderboard
    public void cacheLeaderboard(Long userId, double score) {
        String key = "leaderboard";
        redisTemplate.opsForZSet().add(key, userId, score);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }
}
```

---

## Continue to Part 2

Part 2 covers:
- Multi-level caching strategies
- Cache-aside, Read-through, Write-through patterns
- Cache invalidation strategies
- Distributed caching considerations
- Performance monitoring and metrics
- Common pitfalls and solutions
- Testing strategies
- Production best practices
- Interview questions
