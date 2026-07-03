# Module 3: Spring Data Redis

## 🎯 Learning Objectives

- Master RedisTemplate operations
- Use Spring Data Redis Repositories
- Handle serialization (JSON, JDK, custom)
- Pipeline and batch operations
- Reactive Redis with WebFlux

---

## 3.1 RedisTemplate Operations

```java
// Key Operations
redisTemplate.hasKey("key");                    // EXISTS
redisTemplate.delete("key");                    // DEL
redisTemplate.delete(List.of("k1", "k2"));     // DEL multiple
redisTemplate.expire("key", Duration.ofMinutes(10)); // EXPIRE
redisTemplate.getExpire("key");                 // TTL
redisTemplate.type("key");                      // TYPE
redisTemplate.rename("old", "new");             // RENAME

// SCAN (safe alternative to KEYS)
ScanOptions options = ScanOptions.scanOptions()
    .match("product:*")
    .count(100)
    .build();
try (Cursor<String> cursor = redisTemplate.scan(options)) {
    while (cursor.hasNext()) {
        String key = cursor.next();
        // process key
    }
}
```

---

## 3.2 Repository Pattern with @RedisHash

```java
// src/main/java/com/example/redis/product/model/Product.java

@Data
@RedisHash(value = "product", timeToLive = 3600) // 1 hour TTL
public class Product {

    @Id
    private String id;

    @Indexed  // Creates secondary index for findBy queries
    private String category;

    @Indexed
    private String brand;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private List<String> tags;
    private LocalDateTime createdAt;

    // TTL can be dynamic per instance
    @TimeToLive
    private Long expiration;  // Overrides class-level timeToLive
}
```

```java
// src/main/java/com/example/redis/product/repository/ProductRedisRepository.java

public interface ProductRedisRepository extends CrudRepository<Product, String> {

    // Uses @Indexed fields for secondary index lookup
    List<Product> findByCategory(String category);
    List<Product> findByBrand(String brand);
    List<Product> findByCategoryAndBrand(String category, String brand);

    // Page support
    Page<Product> findByCategory(String category, Pageable pageable);
}
```

```java
// Usage in service
@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductRedisRepository redisRepo;

    public Product save(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        return redisRepo.save(product);
    }

    public Optional<Product> findById(String id) {
        return redisRepo.findById(id);
    }

    public List<Product> findByCategory(String category) {
        return redisRepo.findByCategory(category);
    }

    public void delete(String id) {
        redisRepo.deleteById(id);
    }

    public long count() {
        return redisRepo.count();
    }
}
```

**How it works internally:**
```
HSET product:123 _class "com.example...Product"
HSET product:123 id "123"
HSET product:123 name "iPhone 15"
HSET product:123 category "electronics"
HSET product:123 price "79999"

# Secondary index:
SADD product:category:electronics "123"
SADD product:brand:Apple "123"

# TTL:
EXPIRE product:123 3600
```

**Limitations of @RedisHash:**
- No complex queries (no LIKE, range, sorting)
- Secondary indexes use Sets (memory overhead)
- No transactions across multiple entities
- Not suitable for millions of records

---

## 3.3 Serialization Strategies

```java
@Configuration
public class SerializationConfig {

    // Option 1: JSON (human-readable, debuggable)
    @Bean("jsonRedisTemplate")
    public RedisTemplate<String, Object> jsonRedisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // Option 2: JDK Serialization (fast, but not human-readable)
    @Bean("jdkRedisTemplate")
    public RedisTemplate<String, Object> jdkRedisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        return template;
    }

    // Option 3: String only (manual conversion)
    // Use StringRedisTemplate for full control
}
```

| Serializer | Pros | Cons | Use When |
|-----------|------|------|----------|
| JSON | Readable, debuggable, cross-language | Slower, larger size, type info needed | Default choice |
| JDK | Fast, native Java | Not readable, Java-only, fragile | Performance critical, same JVM |
| String | Smallest, fastest | Manual parsing | Simple values, counters |
| Protobuf | Very compact, cross-language | Setup overhead | High-throughput systems |

---

## 3.4 Pipelining (Batch Operations)

```java
@Service
@RequiredArgsConstructor
public class BatchOperationService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Pipeline: Send multiple commands in one round trip.
     * 10x-100x faster than individual commands for bulk ops.
     */
    public Map<Long, Object> getMultipleProducts(List<Long> productIds) {
        // Without pipeline: N round trips
        // With pipeline: 1 round trip for N commands

        List<Object> results = redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {
                StringRedisSerializer serializer = new StringRedisSerializer();
                for (Long id : productIds) {
                    byte[] key = serializer.serialize("product:cache:" + id);
                    connection.stringCommands().get(key);
                }
                return null; // Must return null in pipeline
            }
        );

        Map<Long, Object> map = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            if (results.get(i) != null) {
                map.put(productIds.get(i), results.get(i));
            }
        }
        return map;
    }

    /**
     * Bulk write with pipeline.
     */
    public void bulkCacheProducts(Map<Long, String> products) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisSerializer serializer = new StringRedisSerializer();
            for (Map.Entry<Long, String> entry : products.entrySet()) {
                byte[] key = serializer.serialize("product:cache:" + entry.getKey());
                byte[] value = serializer.serialize(entry.getValue());
                connection.stringCommands().setEx(key, 3600, value);
            }
            return null;
        });
    }
}
```

**When to use Pipeline:**
- Batch reads (loading page with multiple items)
- Bulk writes (cache warming)
- Any scenario with 10+ independent commands

**Limitations:**
- No conditional logic between commands (use Lua for that)
- Responses come in order but after ALL commands execute
- Large pipelines consume server memory

---

## 3.5 Reactive Redis (WebFlux)

```java
// For Spring WebFlux applications

@Configuration
public class ReactiveRedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
            new GenericJackson2JsonRedisSerializer();

        RedisSerializationContext<String, Object> context =
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

// Reactive service
@Service
@RequiredArgsConstructor
public class ReactiveProductService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedis;

    public Mono<Boolean> cacheProduct(String id, Product product) {
        return reactiveRedis.opsForValue()
            .set("product:" + id, product, Duration.ofHours(1));
    }

    public Mono<Object> getProduct(String id) {
        return reactiveRedis.opsForValue().get("product:" + id);
    }

    public Flux<Object> getMultiple(List<String> ids) {
        List<String> keys = ids.stream()
            .map(id -> "product:" + id)
            .toList();
        return reactiveRedis.opsForValue()
            .multiGet(keys)
            .flatMapMany(Flux::fromIterable);
    }
}
```

---

## 3.6 Error Handling

```java
@Service
@Slf4j
public class ResilientRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis operations should NEVER crash your application.
     * Cache miss = go to database, not throw error.
     */
    public Optional<Object> safeGet(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, key={}: {}", key, e.getMessage());
            return Optional.empty(); // Graceful degradation
        } catch (Exception e) {
            log.error("Redis error for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void safeSet(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache key={}: {}", key, e.getMessage());
            // Don't throw - app continues without cache
        }
    }
}
```

---

## 3.7 Key Takeaways

| Pattern | When to Use |
|---------|-------------|
| RedisTemplate | Direct operations, full control |
| @RedisHash + Repository | CRUD on objects, simple queries |
| Pipeline | Batch operations (10+ commands) |
| Reactive | WebFlux/non-blocking apps |
| StringRedisTemplate | Counters, simple values, Lua scripts |

---

## ⏭️ Next Module

Proceed to **[Module 4: Caching with Spring Cache](04_Caching.md)** for annotation-based caching and multi-layer strategies.
