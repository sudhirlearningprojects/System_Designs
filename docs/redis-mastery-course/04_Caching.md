# Module 4: Caching with Spring Cache

## 🎯 Learning Objectives

- Use @Cacheable, @CacheEvict, @CachePut annotations
- Configure TTL per cache
- Implement multi-layer caching (L1 Caffeine + L2 Redis)
- Handle cache stampede and thundering herd
- Understand cache-aside, write-through, write-behind patterns

---

## 4.1 Cache Configuration

```java
// src/main/java/com/example/redis/config/CacheConfig.java

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        // Default cache config
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues();

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "products", defaultConfig.entryTtl(Duration.ofHours(1)),
            "categories", defaultConfig.entryTtl(Duration.ofHours(6)),
            "productSearch", defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "userProfile", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "inventory", defaultConfig.entryTtl(Duration.ofSeconds(30))
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()  // Cache operations join Spring transactions
            .build();
    }
}
```

---

## 4.2 Cache Annotations

```java
// src/main/java/com/example/redis/product/service/ProductService.java

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepo;  // JPA (PostgreSQL)

    /**
     * @Cacheable - Check cache first, if miss → call method → store result
     * Key: "products::123"
     */
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        log.info("Cache MISS - loading from DB: {}", id);
        return productRepo.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * @Cacheable with complex key using SpEL
     */
    @Cacheable(value = "productSearch", key = "#category + ':' + #page")
    public Page<Product> searchProducts(String category, int page) {
        log.info("Cache MISS - searching DB: category={}, page={}", category, page);
        return productRepo.findByCategory(category, PageRequest.of(page, 20));
    }

    /**
     * @Cacheable with condition - only cache if price > 100
     */
    @Cacheable(value = "products", key = "#id",
               condition = "#id > 0",
               unless = "#result.price < 100")
    public Product getProductConditional(Long id) {
        return productRepo.findById(id).orElseThrow();
    }

    /**
     * @CachePut - Always call method AND update cache
     * Use for updates (not reads)
     */
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        log.info("Updating product and refreshing cache: {}", product.getId());
        return productRepo.save(product);
    }

    /**
     * @CacheEvict - Remove from cache
     */
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    /**
     * @CacheEvict - Clear entire cache
     */
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        log.info("Product cache cleared");
    }

    /**
     * @Caching - Multiple cache operations
     */
    @Caching(
        put = @CachePut(value = "products", key = "#product.id"),
        evict = {
            @CacheEvict(value = "productSearch", allEntries = true),
            @CacheEvict(value = "categories", key = "#product.category")
        }
    )
    public Product saveProduct(Product product) {
        return productRepo.save(product);
    }
}
```

---

## 4.3 Multi-Layer Cache (L1 + L2)

```
Request → L1 (Caffeine - in-memory, 100ms) → L2 (Redis - network, 2-5ms) → DB (50-200ms)
```

```java
// src/main/java/com/example/redis/config/MultiLayerCacheConfig.java

@Configuration
@EnableCaching
public class MultiLayerCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // L2: Redis
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60)))
            .build();

        // L1: Caffeine (in-process)
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))  // Short TTL for L1
            .recordStats());

        // Composite: L1 first, then L2
        return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
    }
}

// Custom composite that checks L1 first, then L2
@Component
public class TwoLevelCacheManager implements CacheManager {

    private final CaffeineCacheManager l1;
    private final RedisCacheManager l2;

    @Override
    public Cache getCache(String name) {
        return new TwoLevelCache(
            l1.getCache(name),
            l2.getCache(name)
        );
    }
}

// TwoLevelCache implementation
public class TwoLevelCache implements Cache {

    private final Cache l1;  // Caffeine
    private final Cache l2;  // Redis

    @Override
    public ValueWrapper get(Object key) {
        // Check L1 first
        ValueWrapper value = l1.get(key);
        if (value != null) return value;

        // Check L2
        value = l2.get(key);
        if (value != null) {
            // Populate L1 from L2
            l1.put(key, value.get());
        }
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }
}
```

---

## 4.4 Cache Stampede Prevention

When a popular key expires, hundreds of requests simultaneously miss cache and hit DB.

```java
@Service
public class StampedeProtectedCacheService {

    private final RedisTemplate<String, Object> redis;
    private final StringRedisTemplate stringRedis;

    /**
     * Approach 1: Probabilistic early expiration
     * Refresh cache BEFORE it actually expires
     */
    public Object getWithEarlyRefresh(String key, Supplier<Object> dbLoader) {
        String valueKey = key;
        String ttlKey = key + ":ttl";

        Object cached = redis.opsForValue().get(valueKey);
        String expiresAt = stringRedis.opsForValue().get(ttlKey);

        if (cached != null && expiresAt != null) {
            long expiresTs = Long.parseLong(expiresAt);
            long now = System.currentTimeMillis();

            // If within 10% of expiry, probabilistically refresh
            long totalTtl = 3600_000; // 1 hour
            double remaining = (expiresTs - now) / (double) totalTtl;

            if (remaining < 0.1 && Math.random() < 0.5) {
                // Async refresh (don't block current request)
                CompletableFuture.runAsync(() -> refreshCache(key, dbLoader));
            }
            return cached;
        }

        // Cache miss - load from DB
        return refreshCache(key, dbLoader);
    }

    /**
     * Approach 2: Distributed lock on cache miss
     * Only ONE request hits DB, others wait
     */
    public Object getWithLock(String key, Supplier<Object> dbLoader) {
        Object cached = redis.opsForValue().get(key);
        if (cached != null) return cached;

        String lockKey = "lock:cache:" + key;
        boolean acquired = Boolean.TRUE.equals(
            stringRedis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10))
        );

        if (acquired) {
            try {
                // Double-check after acquiring lock
                cached = redis.opsForValue().get(key);
                if (cached != null) return cached;

                Object value = dbLoader.get();
                redis.opsForValue().set(key, value, Duration.ofHours(1));
                return value;
            } finally {
                stringRedis.delete(lockKey);
            }
        } else {
            // Wait and retry (another thread is loading)
            Thread.sleep(100);
            return redis.opsForValue().get(key); // Should be populated by now
        }
    }

    private Object refreshCache(String key, Supplier<Object> dbLoader) {
        Object value = dbLoader.get();
        redis.opsForValue().set(key, value, Duration.ofHours(1));
        stringRedis.opsForValue().set(
            key + ":ttl",
            String.valueOf(System.currentTimeMillis() + 3600_000),
            Duration.ofHours(2)
        );
        return value;
    }
}
```

---

## 4.5 Caching Patterns Comparison

| Pattern | How it Works | Use When |
|---------|-------------|----------|
| **Cache-Aside** | App checks cache → miss → load DB → write cache | Default, most flexible |
| **Write-Through** | Write to cache AND DB simultaneously | Strong consistency needed |
| **Write-Behind** | Write to cache → async write to DB | High write throughput |
| **Read-Through** | Cache auto-loads from DB on miss | Simple, provider supports it |
| **Refresh-Ahead** | Cache proactively refreshes before expiry | Hot keys, predictable access |

Spring's @Cacheable implements **Cache-Aside** pattern.

---

## 4.6 Common Pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Caching null | NPE or unnecessary DB calls | `unless = "#result == null"` |
| Wrong key | Cache conflicts | Include all method params in key |
| No TTL | Stale data forever | Always set TTL |
| Cache large collections | Memory bloat | Paginate, cache smaller chunks |
| @Cacheable on same-class calls | Proxy bypass, no caching | Extract to separate @Service |
| Transaction rollback | Cached data that was rolled back | Use `transactionAware()` |

---

## ⏭️ Next Module

Proceed to **[Module 5: Session Management](05_Session_Management.md)** for Spring Session with Redis.
