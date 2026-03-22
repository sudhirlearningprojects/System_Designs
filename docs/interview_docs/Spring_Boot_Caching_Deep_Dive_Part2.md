# Spring Boot Caching Deep Dive - Part 2

## Table of Contents
- [Caching Patterns](#caching-patterns)
- [Multi-Level Caching](#multi-level-caching)
- [Cache Invalidation Strategies](#cache-invalidation-strategies)
- [Cache Warming](#cache-warming)
- [Distributed Caching Considerations](#distributed-caching-considerations)

---

## Caching Patterns

### 1. Cache-Aside (Lazy Loading)

**Most Common Pattern** - Application manages cache explicitly.

```java
@Service
public class CacheAsideService {
    
    private final ProductRepository repository;
    private final CacheManager cacheManager;
    
    public Product getProduct(Long id) {
        // 1. Check cache
        Cache cache = cacheManager.getCache("products");
        Product cached = cache.get(id, Product.class);
        
        if (cached != null) {
            return cached;  // Cache hit
        }
        
        // 2. Cache miss - fetch from database
        Product product = repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        // 3. Store in cache
        cache.put(id, product);
        
        return product;
    }
    
    public Product updateProduct(Product product) {
        // 1. Update database
        Product updated = repository.save(product);
        
        // 2. Invalidate cache
        Cache cache = cacheManager.getCache("products");
        cache.evict(product.getId());
        
        return updated;
    }
}
```

**Using Spring Annotations:**
```java
@Service
public class CacheAsideAnnotationService {
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return repository.findById(id).orElseThrow();
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return repository.save(product);
    }
}
```

**Pros:**
- ✅ Simple to implement
- ✅ Cache only what's requested
- ✅ Resilient to cache failures

**Cons:**
- ❌ Cache miss penalty (initial request slow)
- ❌ Potential cache stampede
- ❌ Stale data possible

### 2. Read-Through Cache

**Cache acts as primary interface** - Cache loads data automatically.

```java
@Configuration
public class ReadThroughCacheConfig {
    
    @Bean
    public CacheManager cacheManager(ProductRepository repository) {
        return new CaffeineCacheManager("products") {
            @Override
            protected Cache createCaffeineCache(String name) {
                return new CaffeineCache(name, 
                    Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build(key -> loadProduct((Long) key, repository))
                );
            }
        };
    }
    
    private Product loadProduct(Long id, ProductRepository repository) {
        return repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
```

**Usage:**
```java
@Service
public class ReadThroughService {
    
    private final CacheManager cacheManager;
    
    public Product getProduct(Long id) {
        Cache cache = cacheManager.getCache("products");
        return cache.get(id, Product.class);  // Auto-loads if missing
    }
}
```

**Pros:**
- ✅ Automatic cache loading
- ✅ Simplified application code
- ✅ Consistent data access pattern

**Cons:**
- ❌ Tight coupling with cache
- ❌ Complex configuration
- ❌ Cache failure affects reads

### 3. Write-Through Cache

**Writes go through cache to database** - Cache updated synchronously.

```java
@Service
public class WriteThroughService {
    
    private final ProductRepository repository;
    private final CacheManager cacheManager;
    
    @Transactional
    public Product updateProduct(Product product) {
        // 1. Update database
        Product updated = repository.save(product);
        
        // 2. Update cache synchronously
        Cache cache = cacheManager.getCache("products");
        cache.put(updated.getId(), updated);
        
        return updated;
    }
}
```

**Using @CachePut:**
```java
@Service
public class WriteThroughAnnotationService {
    
    @CachePut(value = "products", key = "#product.id")
    @Transactional
    public Product updateProduct(Product product) {
        return repository.save(product);
    }
}
```

**Pros:**
- ✅ Cache always consistent
- ✅ No stale data
- ✅ Read performance maintained

**Cons:**
- ❌ Write latency increased
- ❌ Unnecessary cache writes
- ❌ Cache failure affects writes

### 4. Write-Behind (Write-Back) Cache

**Writes to cache first, database later** - Asynchronous database update.

```java
@Service
public class WriteBehindService {
    
    private final ProductRepository repository;
    private final RedisTemplate<String, Product> redisTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public Product updateProduct(Product product) {
        // 1. Update cache immediately
        String key = "product:" + product.getId();
        redisTemplate.opsForValue().set(key, product);
        
        // 2. Async database update
        executor.submit(() -> {
            try {
                repository.save(product);
            } catch (Exception e) {
                // Handle failure - retry or log
                log.error("Failed to persist product: {}", product.getId(), e);
            }
        });
        
        return product;
    }
}
```

**With Spring Events:**
```java
@Service
public class WriteBehindEventService {
    
    private final RedisTemplate<String, Product> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    public Product updateProduct(Product product) {
        // Update cache
        redisTemplate.opsForValue().set("product:" + product.getId(), product);
        
        // Publish event for async DB update
        eventPublisher.publishEvent(new ProductUpdateEvent(product));
        
        return product;
    }
}

@Component
public class ProductUpdateListener {
    
    private final ProductRepository repository;
    
    @Async
    @EventListener
    @Transactional
    public void handleProductUpdate(ProductUpdateEvent event) {
        repository.save(event.getProduct());
    }
}
```

**Pros:**
- ✅ Fastest write performance
- ✅ Reduced database load
- ✅ Better throughput

**Cons:**
- ❌ Data loss risk (cache failure)
- ❌ Complex consistency management
- ❌ Requires persistence mechanism

### 5. Refresh-Ahead Cache

**Proactively refresh before expiry** - Prevents cache miss.

```java
@Configuration
public class RefreshAheadCacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("products");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(8, TimeUnit.MINUTES)  // Refresh at 8 min
            .build(key -> loadProduct((Long) key))
        );
        return manager;
    }
    
    private Product loadProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }
}
```

**Manual Refresh:**
```java
@Service
public class RefreshAheadService {
    
    private final ProductRepository repository;
    private final CacheManager cacheManager;
    
    @Scheduled(fixedRate = 480000)  // Every 8 minutes
    public void refreshPopularProducts() {
        List<Long> popularIds = getPopularProductIds();
        Cache cache = cacheManager.getCache("products");
        
        popularIds.forEach(id -> {
            Product product = repository.findById(id).orElse(null);
            if (product != null) {
                cache.put(id, product);
            }
        });
    }
}
```

**Pros:**
- ✅ No cache miss for hot data
- ✅ Consistent performance
- ✅ Better user experience

**Cons:**
- ❌ Increased database load
- ❌ Wasted refreshes for cold data
- ❌ Complex implementation

---

## Multi-Level Caching

### L1 (Local) + L2 (Distributed) Cache

```java
@Configuration
public class MultiLevelCacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisFactory) {
        return new CompositeCacheManager(
            caffeineCacheManager(),  // L1: Local cache
            redisCacheManager(redisFactory)  // L2: Distributed cache
        );
    }
    
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("products", "users");
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)  // Smaller L1 cache
            .expireAfterWrite(5, TimeUnit.MINUTES)
        );
        return manager;
    }
    
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30));  // Longer L2 TTL
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

**Custom Multi-Level Implementation:**

```java
@Service
public class MultiLevelCacheService {
    
    private final Cache<Long, Product> l1Cache;  // Caffeine
    private final RedisTemplate<String, Product> l2Cache;  // Redis
    private final ProductRepository repository;
    
    public MultiLevelCacheService(RedisTemplate<String, Product> redisTemplate,
                                   ProductRepository repository) {
        this.l1Cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
        this.l2Cache = redisTemplate;
        this.repository = repository;
    }
    
    public Product getProduct(Long id) {
        // 1. Check L1 cache (local)
        Product product = l1Cache.getIfPresent(id);
        if (product != null) {
            log.debug("L1 cache hit: {}", id);
            return product;
        }
        
        // 2. Check L2 cache (Redis)
        String key = "product:" + id;
        product = l2Cache.opsForValue().get(key);
        if (product != null) {
            log.debug("L2 cache hit: {}", id);
            l1Cache.put(id, product);  // Populate L1
            return product;
        }
        
        // 3. Database query
        log.debug("Cache miss: {}", id);
        product = repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        // 4. Populate both caches
        l1Cache.put(id, product);
        l2Cache.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
        
        return product;
    }
    
    public Product updateProduct(Product product) {
        Product updated = repository.save(product);
        
        // Invalidate both caches
        l1Cache.invalidate(product.getId());
        l2Cache.delete("product:" + product.getId());
        
        return updated;
    }
}
```

**Performance Comparison:**

```
┌─────────────┬──────────┬───────────┬──────────────┐
│ Cache Level │ Latency  │ Capacity  │ Scope        │
├─────────────┼──────────┼───────────┼──────────────┤
│ L1 (Local)  │ <1ms     │ 100-1000  │ Single JVM   │
│ L2 (Redis)  │ 1-5ms    │ 10K-1M    │ Distributed  │
│ Database    │ 50-200ms │ Unlimited │ Persistent   │
└─────────────┴──────────┴───────────┴──────────────┘
```

---

## Cache Invalidation Strategies

### 1. Time-Based Expiration (TTL)

**Fixed TTL:**
```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return repository.findById(id).orElseThrow();
}

// Configuration
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10));  // 10 min TTL
    
    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

**Variable TTL:**
```java
@Service
public class VariableTTLService {
    
    private final RedisTemplate<String, Product> redisTemplate;
    
    public void cacheProduct(Product product) {
        String key = "product:" + product.getId();
        
        // Popular products: longer TTL
        long ttl = product.isPopular() ? 60 : 10;
        
        redisTemplate.opsForValue().set(key, product, ttl, TimeUnit.MINUTES);
    }
}
```

### 2. Event-Based Invalidation

```java
@Service
public class ProductService {
    
    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        Product updated = repository.save(product);
        
        // Publish invalidation event
        eventPublisher.publishEvent(new ProductUpdatedEvent(updated.getId()));
        
        return updated;
    }
}

@Component
public class CacheInvalidationListener {
    
    private final CacheManager cacheManager;
    
    @EventListener
    public void handleProductUpdate(ProductUpdatedEvent event) {
        Cache cache = cacheManager.getCache("products");
        cache.evict(event.getProductId());
        
        // Invalidate related caches
        cacheManager.getCache("productsByCategory").clear();
        cacheManager.getCache("popularProducts").clear();
    }
}
```

### 3. Version-Based Invalidation

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    @Version
    private Long version;  // Optimistic locking
    
    private String name;
    private BigDecimal price;
}

@Service
public class VersionedCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public Product getProduct(Long id) {
        String key = "product:" + id;
        
        // Get cached product with version
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (!cached.isEmpty()) {
            Long cachedVersion = (Long) cached.get("version");
            Long dbVersion = getProductVersion(id);
            
            if (cachedVersion.equals(dbVersion)) {
                return mapToProduct(cached);  // Version match
            }
        }
        
        // Version mismatch or cache miss
        Product product = repository.findById(id).orElseThrow();
        cacheProductWithVersion(product);
        return product;
    }
}
```

### 4. Tag-Based Invalidation

```java
@Service
public class TagBasedCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void cacheProduct(Product product) {
        String key = "product:" + product.getId();
        redisTemplate.opsForValue().set(key, product);
        
        // Add to category tag
        String categoryTag = "category:" + product.getCategoryId();
        redisTemplate.opsForSet().add(categoryTag, key);
    }
    
    public void invalidateCategory(Long categoryId) {
        String categoryTag = "category:" + categoryId;
        
        // Get all product keys in this category
        Set<Object> productKeys = redisTemplate.opsForSet().members(categoryTag);
        
        // Delete all products in category
        if (productKeys != null && !productKeys.isEmpty()) {
            redisTemplate.delete(productKeys.stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        }
        
        // Delete tag
        redisTemplate.delete(categoryTag);
    }
}
```

### 5. Dependency-Based Invalidation

```java
@Service
public class DependencyCacheService {
    
    private final CacheManager cacheManager;
    
    @CacheEvict(value = "products", key = "#product.id")
    @CacheEvict(value = "productsByCategory", key = "#product.categoryId")
    @CacheEvict(value = "productsByBrand", key = "#product.brandId")
    public Product updateProduct(Product product) {
        return repository.save(product);
    }
    
    // Or using @Caching
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#product.id"),
        @CacheEvict(value = "productsByCategory", key = "#product.categoryId"),
        @CacheEvict(value = "productsByBrand", key = "#product.brandId"),
        @CacheEvict(value = "relatedProducts", allEntries = true)
    })
    public Product updateProductWithDependencies(Product product) {
        return repository.save(product);
    }
}
```

---

## Cache Warming

### 1. Application Startup Warming

```java
@Component
public class CacheWarmer implements ApplicationRunner {
    
    private final ProductService productService;
    private final ProductRepository repository;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming...");
        
        // Load popular products
        List<Product> popularProducts = repository.findTop100ByOrderByViewCountDesc();
        popularProducts.forEach(product -> {
            productService.getProduct(product.getId());  // Triggers @Cacheable
        });
        
        log.info("Cache warming completed: {} products", popularProducts.size());
    }
}
```

### 2. Scheduled Cache Warming

```java
@Component
public class ScheduledCacheWarmer {
    
    private final CacheManager cacheManager;
    private final ProductRepository repository;
    
    @Scheduled(cron = "0 0 */6 * * *")  // Every 6 hours
    public void warmCache() {
        log.info("Scheduled cache warming started");
        
        Cache cache = cacheManager.getCache("products");
        List<Product> products = repository.findPopularProducts();
        
        products.forEach(product -> cache.put(product.getId(), product));
        
        log.info("Warmed {} products", products.size());
    }
}
```

### 3. Predictive Cache Warming

```java
@Service
public class PredictiveCacheWarmer {
    
    private final CacheManager cacheManager;
    private final AnalyticsService analyticsService;
    
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    public void warmPredictedProducts() {
        // Get trending products from analytics
        List<Long> trendingIds = analyticsService.getTrendingProductIds();
        
        Cache cache = cacheManager.getCache("products");
        
        trendingIds.forEach(id -> {
            if (cache.get(id) == null) {
                Product product = repository.findById(id).orElse(null);
                if (product != null) {
                    cache.put(id, product);
                }
            }
        });
    }
}
```

---

## Distributed Caching Considerations

### 1. Cache Stampede Prevention

**Problem:** Multiple requests hit database when cache expires.

```java
@Service
public class StampedePreventionService {
    
    private final LoadingCache<Long, Product> cache;
    private final ProductRepository repository;
    
    public StampedePreventionService(ProductRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .refreshAfterWrite(8, TimeUnit.MINUTES)  // Refresh before expiry
            .build(id -> repository.findById(id).orElseThrow());
    }
    
    public Product getProduct(Long id) {
        return cache.get(id);  // Only one thread loads on miss
    }
}
```

**Using Redis with Locking:**

```java
@Service
public class RedisStampedePreventionService {
    
    private final RedisTemplate<String, Product> redisTemplate;
    private final RedissonClient redissonClient;
    private final ProductRepository repository;
    
    public Product getProduct(Long id) {
        String key = "product:" + id;
        
        // Try cache
        Product product = redisTemplate.opsForValue().get(key);
        if (product != null) {
            return product;
        }
        
        // Acquire lock to prevent stampede
        String lockKey = "lock:product:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Wait max 5 seconds for lock
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    // Double-check cache
                    product = redisTemplate.opsForValue().get(key);
                    if (product != null) {
                        return product;
                    }
                    
                    // Load from database
                    product = repository.findById(id).orElseThrow();
                    redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
                    
                    return product;
                } finally {
                    lock.unlock();
                }
            } else {
                // Couldn't get lock - fallback to database
                return repository.findById(id).orElseThrow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for lock", e);
        }
    }
}
```

### 2. Cache Consistency in Distributed Systems

```java
@Service
public class DistributedCacheConsistencyService {
    
    private final RedisTemplate<String, Product> redisTemplate;
    private final KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;
    
    @Transactional
    public Product updateProduct(Product product) {
        // 1. Update database
        Product updated = repository.save(product);
        
        // 2. Invalidate local cache
        String key = "product:" + product.getId();
        redisTemplate.delete(key);
        
        // 3. Broadcast invalidation to all instances
        CacheInvalidationEvent event = new CacheInvalidationEvent(
            "products", 
            product.getId()
        );
        kafkaTemplate.send("cache-invalidation", event);
        
        return updated;
    }
}

@Component
public class CacheInvalidationConsumer {
    
    private final CacheManager cacheManager;
    
    @KafkaListener(topics = "cache-invalidation")
    public void handleInvalidation(CacheInvalidationEvent event) {
        Cache cache = cacheManager.getCache(event.getCacheName());
        if (cache != null) {
            cache.evict(event.getKey());
        }
    }
}
```

### 3. Handling Network Partitions

```java
@Service
public class ResilientCacheService {
    
    private final RedisTemplate<String, Product> redisTemplate;
    private final ProductRepository repository;
    private final Cache<Long, Product> fallbackCache;
    
    public ResilientCacheService(ProductRepository repository) {
        this.repository = repository;
        this.fallbackCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }
    
    public Product getProduct(Long id) {
        try {
            // Try Redis first
            String key = "product:" + id;
            Product product = redisTemplate.opsForValue().get(key);
            
            if (product != null) {
                fallbackCache.put(id, product);  // Update fallback
                return product;
            }
            
            // Load from database
            product = repository.findById(id).orElseThrow();
            redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
            fallbackCache.put(id, product);
            
            return product;
            
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, using fallback cache");
            
            // Use local fallback cache
            Product product = fallbackCache.getIfPresent(id);
            if (product != null) {
                return product;
            }
            
            // Last resort: database
            product = repository.findById(id).orElseThrow();
            fallbackCache.put(id, product);
            return product;
        }
    }
}
```

---

## Continue to Part 3

Part 3 covers:
- Performance monitoring and metrics
- Testing strategies
- Common pitfalls and solutions
- Production best practices
- Interview questions and answers
