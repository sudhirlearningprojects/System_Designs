# Spring Boot Caching Deep Dive - Part 3

## Table of Contents
- [Performance Monitoring](#performance-monitoring)
- [Testing Strategies](#testing-strategies)
- [Common Pitfalls](#common-pitfalls)
- [Production Best Practices](#production-best-practices)
- [Interview Questions](#interview-questions)

---

## Performance Monitoring

### 1. Cache Metrics with Micrometer

```java
@Configuration
public class CacheMetricsConfig {
    
    @Bean
    public CacheManager cacheManager(MeterRegistry registry) {
        CaffeineCacheManager manager = new CaffeineCacheManager("products", "users");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()  // Enable statistics
        );
        
        // Register metrics
        manager.getCacheNames().forEach(cacheName -> {
            Cache cache = manager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                CaffeineCacheMetrics.monitor(registry, nativeCache, cacheName);
            }
        });
        
        return manager;
    }
}
```

### 2. Custom Cache Metrics

```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry registry;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheLoadTime;
    
    public CacheMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        this.cacheHits = Counter.builder("cache.hits")
            .tag("cache", "products")
            .register(registry);
        this.cacheMisses = Counter.builder("cache.misses")
            .tag("cache", "products")
            .register(registry);
        this.cacheLoadTime = Timer.builder("cache.load.time")
            .tag("cache", "products")
            .register(registry);
    }
    
    public void recordHit() {
        cacheHits.increment();
    }
    
    public void recordMiss() {
        cacheMisses.increment();
    }
    
    public void recordLoadTime(long nanos) {
        cacheLoadTime.record(nanos, TimeUnit.NANOSECONDS);
    }
}
```

### 3. Cache Statistics Endpoint

```java
@RestController
@RequestMapping("/actuator/cache")
public class CacheStatsController {
    
    private final CacheManager cacheManager;
    
    @GetMapping("/stats")
    public Map<String, CacheStats> getCacheStats() {
        Map<String, CacheStats> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                
                CacheStats cacheStats = nativeCache.stats();
                stats.put(cacheName, cacheStats);
            }
        });
        
        return stats;
    }
    
    @GetMapping("/stats/{cacheName}")
    public CacheStatsDTO getCacheStatsByName(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                ((CaffeineCache) cache).getNativeCache();
            
            CacheStats stats = nativeCache.stats();
            return CacheStatsDTO.builder()
                .hitCount(stats.hitCount())
                .missCount(stats.missCount())
                .hitRate(stats.hitRate())
                .evictionCount(stats.evictionCount())
                .loadSuccessCount(stats.loadSuccessCount())
                .loadFailureCount(stats.loadFailureCount())
                .averageLoadPenalty(stats.averageLoadPenalty())
                .build();
        }
        throw new CacheNotFoundException(cacheName);
    }
}
```

### 4. Redis Cache Monitoring

```java
@Component
public class RedisCacheMonitor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry registry;
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void collectRedisMetrics() {
        Properties info = redisTemplate.getConnectionFactory()
            .getConnection()
            .info();
        
        // Memory usage
        String usedMemory = info.getProperty("used_memory");
        registry.gauge("redis.memory.used", Double.parseDouble(usedMemory));
        
        // Connected clients
        String connectedClients = info.getProperty("connected_clients");
        registry.gauge("redis.clients.connected", Double.parseDouble(connectedClients));
        
        // Hit rate
        String keyspaceHits = info.getProperty("keyspace_hits");
        String keyspaceMisses = info.getProperty("keyspace_misses");
        double hitRate = calculateHitRate(keyspaceHits, keyspaceMisses);
        registry.gauge("redis.hit.rate", hitRate);
    }
    
    private double calculateHitRate(String hits, String misses) {
        long h = Long.parseLong(hits);
        long m = Long.parseLong(misses);
        return h + m == 0 ? 0 : (double) h / (h + m);
    }
}
```

---

## Testing Strategies

### 1. Unit Testing with Mock Cache

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository repository;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private Cache cache;
    
    @InjectMocks
    private ProductService service;
    
    @Test
    void testGetProduct_CacheHit() {
        Long productId = 1L;
        Product product = new Product(productId, "Test Product");
        
        when(cacheManager.getCache("products")).thenReturn(cache);
        when(cache.get(productId, Product.class)).thenReturn(product);
        
        Product result = service.getProduct(productId);
        
        assertEquals(product, result);
        verify(repository, never()).findById(any());
    }
    
    @Test
    void testGetProduct_CacheMiss() {
        Long productId = 1L;
        Product product = new Product(productId, "Test Product");
        
        when(cacheManager.getCache("products")).thenReturn(cache);
        when(cache.get(productId, Product.class)).thenReturn(null);
        when(repository.findById(productId)).thenReturn(Optional.of(product));
        
        Product result = service.getProduct(productId);
        
        assertEquals(product, result);
        verify(repository).findById(productId);
        verify(cache).put(productId, product);
    }
}
```

### 2. Integration Testing with Embedded Redis

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6370"
})
class CacheIntegrationTest {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CacheManager cacheManager;
    
    private static RedisServer redisServer;
    
    @BeforeAll
    static void startRedis() {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }
    
    @AfterAll
    static void stopRedis() {
        redisServer.stop();
    }
    
    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear()
        );
    }
    
    @Test
    void testCaching() {
        Product product = new Product(1L, "Test Product");
        productRepository.save(product);
        
        // First call - cache miss
        long start1 = System.currentTimeMillis();
        Product result1 = productService.getProduct(1L);
        long time1 = System.currentTimeMillis() - start1;
        
        // Second call - cache hit
        long start2 = System.currentTimeMillis();
        Product result2 = productService.getProduct(1L);
        long time2 = System.currentTimeMillis() - start2;
        
        assertEquals(result1, result2);
        assertTrue(time2 < time1, "Cache hit should be faster");
    }
}
```

### 3. Testing Cache Eviction

```java
@SpringBootTest
class CacheEvictionTest {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    void testCacheEvictionOnUpdate() {
        Product product = new Product(1L, "Original");
        productService.createProduct(product);
        
        // Cache the product
        Product cached = productService.getProduct(1L);
        assertEquals("Original", cached.getName());
        
        // Update product
        product.setName("Updated");
        productService.updateProduct(product);
        
        // Verify cache was evicted
        Cache cache = cacheManager.getCache("products");
        assertNull(cache.get(1L));
        
        // Fetch again - should get updated value
        Product updated = productService.getProduct(1L);
        assertEquals("Updated", updated.getName());
    }
}
```

### 4. Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
class RedisTestcontainersTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private ProductService productService;
    
    @Test
    void testRedisCaching() {
        Product product = new Product(1L, "Test");
        productService.createProduct(product);
        
        Product result = productService.getProduct(1L);
        assertNotNull(result);
    }
}
```

---

## Common Pitfalls

### 1. Caching Null Values

**Problem:**
```java
@Cacheable("products")
public Product getProduct(Long id) {
    return repository.findById(id).orElse(null);  // Caches null!
}
```

**Solution:**
```java
// Option 1: Don't cache null
@Cacheable(value = "products", unless = "#result == null")
public Product getProduct(Long id) {
    return repository.findById(id).orElse(null);
}

// Option 2: Throw exception
@Cacheable("products")
public Product getProduct(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
}

// Option 3: Configure cache manager
RedisCacheConfiguration.defaultCacheConfig()
    .disableCachingNullValues();
```

### 2. Wrong Cache Key

**Problem:**
```java
@Cacheable(value = "products")  // Key is all method params
public Product getProduct(Long id, String locale) {
    return repository.findById(id).orElseThrow();
}
// Cache key: products::1::en, products::1::fr (separate entries!)
```

**Solution:**
```java
@Cacheable(value = "products", key = "#id")  // Only use id
public Product getProduct(Long id, String locale) {
    return repository.findById(id).orElseThrow();
}
```

### 3. Caching in Same Class

**Problem:**
```java
@Service
public class ProductService {
    
    @Cacheable("products")
    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }
    
    public List<Product> getProducts(List<Long> ids) {
        return ids.stream()
            .map(this::getProduct)  // Cache doesn't work!
            .collect(Collectors.toList());
    }
}
```

**Reason:** Spring AOP proxies don't intercept internal method calls.

**Solution:**
```java
// Option 1: Self-injection
@Service
public class ProductService {
    
    @Autowired
    private ProductService self;
    
    public List<Product> getProducts(List<Long> ids) {
        return ids.stream()
            .map(id -> self.getProduct(id))  // Works!
            .collect(Collectors.toList());
    }
}

// Option 2: Separate service
@Service
public class ProductCacheService {
    @Cacheable("products")
    public Product getProduct(Long id) { ... }
}

@Service
public class ProductService {
    @Autowired
    private ProductCacheService cacheService;
    
    public List<Product> getProducts(List<Long> ids) {
        return ids.stream()
            .map(cacheService::getProduct)
            .collect(Collectors.toList());
    }
}
```

### 4. Serialization Issues

**Problem:**
```java
public class Product {
    private Long id;
    private transient String tempData;  // Lost on serialization!
}
```

**Solution:**
```java
// Option 1: Remove transient
public class Product implements Serializable {
    private Long id;
    private String tempData;
}

// Option 2: Custom serializer
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private Long id;
    @JsonIgnore
    private String tempData;
}
```

### 5. Large Objects in Cache

**Problem:**
```java
@Cacheable("reports")
public byte[] generateReport(Long id) {
    return generateLargeReport();  // 10MB report!
}
```

**Solution:**
```java
// Cache metadata, not content
@Cacheable("reportMetadata")
public ReportMetadata getReportMetadata(Long id) {
    return new ReportMetadata(id, s3Url, size);
}

// Store large content in S3/disk
public byte[] getReportContent(Long id) {
    ReportMetadata metadata = getReportMetadata(id);
    return s3Client.getObject(metadata.getS3Url());
}
```

### 6. Cache Stampede

**Problem:**
```java
@Cacheable("products")
public Product getProduct(Long id) {
    Thread.sleep(5000);  // Slow query
    return repository.findById(id).orElseThrow();
}
// 100 concurrent requests = 100 DB queries!
```

**Solution:**
```java
// Use Caffeine with loading cache
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(key -> loadProduct((Long) key))  // Only one thread loads
    );
    return manager;
}
```

---

## Production Best Practices

### 1. Cache Configuration Checklist

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
      cache-null-values: false  # Don't cache nulls
      use-key-prefix: true
      key-prefix: "${spring.application.name}:"
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: true  # Use SSL in production
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
      shutdown-timeout: 100ms
```

### 2. Monitoring and Alerting

```java
@Component
public class CacheHealthIndicator implements HealthIndicator {
    
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            // Check Redis connectivity
            redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            // Check cache hit rate
            double hitRate = calculateHitRate();
            
            if (hitRate < 0.5) {
                return Health.down()
                    .withDetail("hitRate", hitRate)
                    .withDetail("message", "Cache hit rate below threshold")
                    .build();
            }
            
            return Health.up()
                .withDetail("hitRate", hitRate)
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

### 3. Cache Key Design

```java
public class CacheKeyGenerator {
    
    // Good: Hierarchical keys
    public static String productKey(Long id) {
        return String.format("product:%d", id);
    }
    
    public static String userKey(Long id) {
        return String.format("user:%d", id);
    }
    
    // Good: Include version
    public static String productKeyWithVersion(Long id, int version) {
        return String.format("product:v%d:%d", version, id);
    }
    
    // Good: Include tenant
    public static String tenantProductKey(String tenantId, Long productId) {
        return String.format("tenant:%s:product:%d", tenantId, productId);
    }
}
```

### 4. TTL Strategy

```java
@Configuration
public class TTLStrategy {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        
        // Hot data: 1 hour
        configs.put("products", cacheConfig(Duration.ofHours(1)));
        
        // Warm data: 30 minutes
        configs.put("categories", cacheConfig(Duration.ofMinutes(30)));
        
        // Cold data: 10 minutes
        configs.put("reports", cacheConfig(Duration.ofMinutes(10)));
        
        // Session data: 24 hours
        configs.put("sessions", cacheConfig(Duration.ofHours(24)));
        
        return RedisCacheManager.builder(factory)
            .withInitialCacheConfigurations(configs)
            .build();
    }
    
    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }
}
```

### 5. Graceful Degradation

```java
@Service
public class ResilientProductService {
    
    private final ProductRepository repository;
    private final RedisTemplate<String, Product> redisTemplate;
    
    @CircuitBreaker(name = "redis", fallbackMethod = "getProductFallback")
    public Product getProduct(Long id) {
        String key = "product:" + id;
        Product product = redisTemplate.opsForValue().get(key);
        
        if (product == null) {
            product = repository.findById(id).orElseThrow();
            redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
        }
        
        return product;
    }
    
    public Product getProductFallback(Long id, Exception e) {
        log.warn("Redis unavailable, fetching from database", e);
        return repository.findById(id).orElseThrow();
    }
}
```

---

## Interview Questions

### Q1: What is caching and why use it?

**Answer:**
Caching stores frequently accessed data in fast-access storage to reduce:
- Database load (fewer queries)
- Response time (ms vs seconds)
- External API calls
- Expensive computations

**Example:** Product catalog with 1M products, 10K requests/sec
- Without cache: 10K DB queries/sec
- With cache (90% hit rate): 1K DB queries/sec

### Q2: Difference between @Cacheable, @CachePut, and @CacheEvict?

**Answer:**
- `@Cacheable`: Read from cache, execute method only on miss
- `@CachePut`: Always execute method, update cache with result
- `@CacheEvict`: Remove entry from cache

```java
@Cacheable("users")  // Cache result
User getUser(Long id) { }

@CachePut("users")  // Update cache
User updateUser(User user) { }

@CacheEvict("users")  // Remove from cache
void deleteUser(Long id) { }
```

### Q3: What cache providers does Spring Boot support?

**Answer:**
- **Caffeine**: In-memory, fastest, single instance
- **Redis**: Distributed, persistent, most popular
- **Hazelcast**: Distributed, in-memory data grid
- **EhCache**: In-memory/disk, enterprise features
- **Memcached**: Distributed, simple key-value
- **Simple**: ConcurrentHashMap, dev only

**Selection:**
- Single instance → Caffeine
- Distributed → Redis
- Complex queries → Hazelcast

### Q4: How to handle cache stampede?

**Answer:**
Cache stampede occurs when many requests hit database simultaneously on cache expiry.

**Solutions:**
1. **Refresh-ahead**: Refresh before expiry
2. **Locking**: Only one thread loads data
3. **Probabilistic early expiration**: Random early refresh

```java
Caffeine.newBuilder()
    .refreshAfterWrite(8, TimeUnit.MINUTES)  // Refresh at 8 min
    .expireAfterWrite(10, TimeUnit.MINUTES)  // Expire at 10 min
```

### Q5: What are cache invalidation strategies?

**Answer:**
1. **TTL**: Time-based expiration
2. **Event-based**: Invalidate on updates
3. **Version-based**: Check version before use
4. **Tag-based**: Group related entries
5. **LRU/LFU**: Evict least recently/frequently used

**Best Practice:** Combine TTL + event-based

### Q6: How to test caching?

**Answer:**
1. **Unit tests**: Mock CacheManager
2. **Integration tests**: Embedded Redis
3. **Testcontainers**: Real Redis in Docker
4. **Performance tests**: Measure hit rate

```java
@Test
void testCaching() {
    service.getProduct(1L);  // Cache miss
    service.getProduct(1L);  // Cache hit
    verify(repository, times(1)).findById(1L);
}
```

### Q7: When NOT to use caching?

**Answer:**
- Real-time data (stock prices)
- Frequently changing data (shopping cart)
- User-specific sensitive data (credit cards)
- Large objects (videos)
- Write-heavy operations

### Q8: How to implement multi-level caching?

**Answer:**
L1 (local) + L2 (distributed):

```java
public Product getProduct(Long id) {
    // L1: Caffeine (1ms)
    Product product = l1Cache.getIfPresent(id);
    if (product != null) return product;
    
    // L2: Redis (5ms)
    product = redisTemplate.opsForValue().get("product:" + id);
    if (product != null) {
        l1Cache.put(id, product);
        return product;
    }
    
    // Database (100ms)
    product = repository.findById(id).orElseThrow();
    l1Cache.put(id, product);
    redisTemplate.opsForValue().set("product:" + id, product);
    return product;
}
```

### Q9: How to monitor cache performance?

**Answer:**
Key metrics:
- **Hit rate**: hits / (hits + misses)
- **Miss rate**: misses / (hits + misses)
- **Eviction rate**: evictions / time
- **Load time**: Average time to load on miss
- **Memory usage**: Cache size

**Target:** 80%+ hit rate for read-heavy apps

### Q10: What is cache-aside vs read-through?

**Answer:**
**Cache-Aside (Lazy Loading):**
- Application manages cache
- Load on demand
- Most common pattern

**Read-Through:**
- Cache loads data automatically
- Transparent to application
- Requires cache provider support

```java
// Cache-Aside
Product p = cache.get(id);
if (p == null) {
    p = db.find(id);
    cache.put(id, p);
}

// Read-Through
Product p = cache.get(id);  // Auto-loads if missing
```

---

## Summary

### Key Takeaways

1. **Use caching for**: Read-heavy, expensive computations, external APIs
2. **Don't cache**: Real-time data, frequently changing, sensitive data
3. **Choose provider**: Caffeine (single), Redis (distributed)
4. **Invalidation**: Combine TTL + event-based
5. **Monitor**: Hit rate, latency, memory usage
6. **Test**: Unit, integration, performance tests
7. **Production**: Graceful degradation, monitoring, alerting

### Performance Impact

```
Without Cache: 100ms response time
With Cache (90% hit rate): 14ms average
Improvement: 7x faster
```

### Checklist

- [ ] Cache provider selected
- [ ] TTL configured appropriately
- [ ] Cache keys well-designed
- [ ] Invalidation strategy implemented
- [ ] Monitoring and metrics enabled
- [ ] Tests written
- [ ] Graceful degradation implemented
- [ ] Documentation updated

---

**Related Documents:**
- [Redis_Deep_Dive_Spring_Boot.md](Redis_Deep_Dive_Spring_Boot.md)
- [Database_Cache_Strategies_Deep_Dive.md](Database_Cache_Strategies_Deep_Dive.md)
- [Spring_Transactional_Deep_Dive.md](Spring_Transactional_Deep_Dive.md)
