# Module 2: Data Types Deep Dive

## 🎯 Learning Objectives

- Master all Redis data types with Java/Spring
- Understand time complexity of each operation
- Know when to use which data type
- Implement real use cases for each type

---

## 2.1 Strings (Most Basic)

**Use cases**: Counters, session tokens, cached JSON, flags, locks

```java
// src/main/java/com/example/redis/product/service/ProductCounterService.java

@Service
@RequiredArgsConstructor
public class ProductCounterService {

    private final StringRedisTemplate redis;

    // Simple set/get
    public void cacheProductName(Long productId, String name) {
        redis.opsForValue().set(
            "product:name:" + productId,
            name,
            Duration.ofHours(1)  // TTL
        );
    }

    // Atomic increment (views counter)
    public Long incrementViews(Long productId) {
        return redis.opsForValue().increment(
            "product:views:" + productId
        );
    }

    // Set only if not exists (distributed lock primitive)
    public boolean acquireLock(String lockKey, String value, Duration ttl) {
        return Boolean.TRUE.equals(
            redis.opsForValue().setIfAbsent(lockKey, value, ttl)
        );
    }

    // GETSET - set new value, return old (atomic swap)
    public String swapValue(String key, String newValue) {
        return redis.opsForValue().getAndSet(key, newValue);
    }

    // MGET - batch get (single round trip!)
    public List<String> getMultipleProducts(List<Long> ids) {
        List<String> keys = ids.stream()
            .map(id -> "product:name:" + id)
            .toList();
        return redis.opsForValue().multiGet(keys);
    }

    // SETNX + EXPIRE pattern (conditional caching)
    public boolean cacheIfMissing(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(
            redis.opsForValue().setIfAbsent(key, value, ttl)
        );
    }
}
```

**Time Complexity**: SET O(1), GET O(1), INCR O(1), MGET O(N)

**Limitations**:
- Max value size: 512 MB (but keep values small for performance)
- No partial update — must SET entire value again
- Not suitable for complex objects (use Hashes instead)

---

## 2.2 Hashes (Object Storage)

**Use cases**: User profiles, shopping cart, product details, session data

```java
// src/main/java/com/example/redis/cart/service/CartService.java

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Add item to cart (HSET)
    public void addToCart(String userId, String productId, int quantity) {
        String key = "cart:user:" + userId;
        redisTemplate.opsForHash().put(key, productId, quantity);
        redisTemplate.expire(key, Duration.ofDays(7)); // Cart expires in 7 days
    }

    // Get single item quantity (HGET)
    public Integer getItemQuantity(String userId, String productId) {
        Object qty = redisTemplate.opsForHash().get(
            "cart:user:" + userId, productId
        );
        return qty != null ? (Integer) qty : 0;
    }

    // Get entire cart (HGETALL)
    public Map<Object, Object> getFullCart(String userId) {
        return redisTemplate.opsForHash().entries("cart:user:" + userId);
    }

    // Remove item (HDEL)
    public void removeFromCart(String userId, String productId) {
        redisTemplate.opsForHash().delete("cart:user:" + userId, productId);
    }

    // Increment quantity (HINCRBY - atomic!)
    public Long incrementQuantity(String userId, String productId, int delta) {
        return redisTemplate.opsForHash().increment(
            "cart:user:" + userId, productId, delta
        );
    }

    // Get cart size (HLEN)
    public Long getCartItemCount(String userId) {
        return redisTemplate.opsForHash().size("cart:user:" + userId);
    }

    // Check if item exists (HEXISTS)
    public boolean isInCart(String userId, String productId) {
        return redisTemplate.opsForHash().hasKey(
            "cart:user:" + userId, productId
        );
    }
}
```

**Time Complexity**: HSET O(1), HGET O(1), HGETALL O(N), HINCRBY O(1)

**Limitations**:
- No nested hashes (flat key-value only)
- HGETALL on large hashes is expensive — use HSCAN for big ones
- No per-field TTL (entire hash expires together)

---

## 2.3 Lists (Ordered Collections)

**Use cases**: Recent activity, job queues, message history, timelines

```java
// src/main/java/com/example/redis/analytics/service/ActivityService.java

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Add to recent activity (LPUSH - newest first)
    public void recordActivity(String userId, String activity) {
        String key = "activity:user:" + userId;
        redisTemplate.opsForList().leftPush(key, activity);
        // Keep only last 100 activities
        redisTemplate.opsForList().trim(key, 0, 99);
    }

    // Get recent activities (LRANGE)
    public List<Object> getRecentActivity(String userId, int count) {
        return redisTemplate.opsForList().range(
            "activity:user:" + userId, 0, count - 1
        );
    }

    // Simple job queue - producer (RPUSH)
    public void enqueueJob(String queueName, Object job) {
        redisTemplate.opsForList().rightPush("queue:" + queueName, job);
    }

    // Simple job queue - consumer (BLPOP - blocking)
    public Object dequeueJob(String queueName, Duration timeout) {
        return redisTemplate.opsForList().leftPop(
            "queue:" + queueName, timeout
        );
    }

    // Get queue length (LLEN)
    public Long getQueueSize(String queueName) {
        return redisTemplate.opsForList().size("queue:" + queueName);
    }
}
```

**Time Complexity**: LPUSH O(1), RPOP O(1), LRANGE O(S+N), LLEN O(1)

**Limitations**:
- No efficient access by index in the middle (O(N))
- LRANGE on large lists is slow
- No built-in deduplication
- Not ideal for priority queues (use Sorted Sets)

---

## 2.4 Sets (Unique Collections)

**Use cases**: Tags, unique visitors, friends lists, set operations

```java
// src/main/java/com/example/redis/product/service/TagService.java

@Service
@RequiredArgsConstructor
public class TagService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Add tags to product (SADD)
    public void addTags(Long productId, String... tags) {
        redisTemplate.opsForSet().add("product:tags:" + productId, (Object[]) tags);
    }

    // Get all tags (SMEMBERS)
    public Set<Object> getTags(Long productId) {
        return redisTemplate.opsForSet().members("product:tags:" + productId);
    }

    // Track unique visitors today (SADD)
    public void trackVisitor(String userId) {
        String key = "visitors:" + LocalDate.now();
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, Duration.ofDays(2));
    }

    // Count unique visitors (SCARD)
    public Long getUniqueVisitorCount() {
        return redisTemplate.opsForSet().size("visitors:" + LocalDate.now());
    }

    // Products with common tags (SINTER)
    public Set<Object> commonTags(Long product1, Long product2) {
        return redisTemplate.opsForSet().intersect(
            "product:tags:" + product1,
            "product:tags:" + product2
        );
    }

    // All tags across products (SUNION)
    public Set<Object> allTags(List<Long> productIds) {
        List<String> keys = productIds.stream()
            .map(id -> "product:tags:" + id)
            .toList();
        return redisTemplate.opsForSet().union(keys.get(0), keys.subList(1, keys.size()));
    }

    // Random product from set (SRANDMEMBER)
    public Object getRandomProduct(String category) {
        return redisTemplate.opsForSet().randomMember("category:" + category);
    }
}
```

**Time Complexity**: SADD O(1), SISMEMBER O(1), SMEMBERS O(N), SINTER O(N*M)

**Limitations**:
- No ordering (use Sorted Sets if order matters)
- SMEMBERS on large sets is expensive — use SSCAN
- Set operations (SINTER, SUNION) can be slow on large sets

---

## 2.5 Sorted Sets (Ranked Data)

**Use cases**: Leaderboards, trending, priority queues, time-based scoring

```java
// src/main/java/com/example/redis/analytics/leaderboard/LeaderboardService.java

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TRENDING_KEY = "leaderboard:trending";

    // Record product view/purchase (ZINCRBY)
    public Double recordInteraction(Long productId, double score) {
        return redisTemplate.opsForZSet().incrementScore(
            TRENDING_KEY, productId.toString(), score
        );
    }

    // Get top N trending products (ZREVRANGE)
    public Set<Object> getTopTrending(int count) {
        return redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, count - 1);
    }

    // Get top N with scores (ZREVRANGEBYSCORE)
    public Set<ZSetOperations.TypedTuple<Object>> getTopWithScores(int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(
            TRENDING_KEY, 0, count - 1
        );
    }

    // Get product rank (ZREVRANK - 0-indexed)
    public Long getProductRank(Long productId) {
        return redisTemplate.opsForZSet().reverseRank(
            TRENDING_KEY, productId.toString()
        );
    }

    // Get product score (ZSCORE)
    public Double getProductScore(Long productId) {
        return redisTemplate.opsForZSet().score(
            TRENDING_KEY, productId.toString()
        );
    }

    // Remove old entries below score threshold (ZREMRANGEBYSCORE)
    public void cleanupLowScoring(double minScore) {
        redisTemplate.opsForZSet().removeRangeByScore(
            TRENDING_KEY, 0, minScore
        );
    }

    // Count products in score range (ZCOUNT)
    public Long countInRange(double min, double max) {
        return redisTemplate.opsForZSet().count(TRENDING_KEY, min, max);
    }

    // Priority queue: schedule tasks by timestamp
    public void scheduleTask(String taskId, Instant executeAt) {
        redisTemplate.opsForZSet().add(
            "queue:scheduled",
            taskId,
            executeAt.toEpochMilli()
        );
    }

    // Get due tasks (score <= now)
    public Set<Object> getDueTasks() {
        double now = Instant.now().toEpochMilli();
        return redisTemplate.opsForZSet().rangeByScore(
            "queue:scheduled", 0, now
        );
    }
}
```

**Time Complexity**: ZADD O(log N), ZRANGE O(log N + M), ZRANK O(log N)

**Limitations**:
- Higher memory than Sets (stores score per member)
- Member must be unique (score can duplicate)
- No nested/grouped sorted sets

---

## 2.6 HyperLogLog (Approximate Counting)

**Use cases**: Unique visitor counting (millions), cardinality estimation

```java
// Approximate unique count - uses only 12KB memory regardless of count!

@Service
@RequiredArgsConstructor
public class UniqueCounterService {

    private final StringRedisTemplate redis;

    // Track unique page view (PFADD)
    public void trackPageView(String pageId, String visitorId) {
        redis.opsForHyperLogLog().add("hll:page:" + pageId, visitorId);
    }

    // Get approximate unique count (PFCOUNT) - 0.81% error rate
    public Long getUniqueViewers(String pageId) {
        return redis.opsForHyperLogLog().size("hll:page:" + pageId);
    }

    // Merge multiple pages (PFMERGE)
    public Long getTotalUniqueAcrossPages(List<String> pageIds) {
        String[] keys = pageIds.stream()
            .map(id -> "hll:page:" + id)
            .toArray(String[]::new);
        String destKey = "hll:merged:temp";
        redis.opsForHyperLogLog().union(destKey, keys);
        Long count = redis.opsForHyperLogLog().size(destKey);
        redis.delete(destKey);
        return count;
    }
}
```

**Limitation**: ~0.81% standard error, cannot retrieve individual elements

---

## 2.7 Bitmaps (Bit-Level Operations)

**Use cases**: Feature flags, daily active users, attendance, boolean states

```java
@Service
@RequiredArgsConstructor
public class BitmapService {

    private final StringRedisTemplate redis;

    // Track daily active user (SETBIT)
    public void markActive(int userId, LocalDate date) {
        String key = "active:" + date;
        redis.opsForValue().setBit(key, userId, true);
    }

    // Check if user was active (GETBIT)
    public boolean wasActive(int userId, LocalDate date) {
        return Boolean.TRUE.equals(
            redis.opsForValue().getBit("active:" + date, userId)
        );
    }

    // Count active users on a day (BITCOUNT)
    // Uses RedisCallback for BITCOUNT (not directly in Spring)
    public Long countActiveUsers(LocalDate date) {
        String key = "active:" + date;
        return redis.execute((connection) -> {
            return connection.stringCommands().bitCount(key.getBytes());
        });
    }

    // Feature flag per user
    public void setFeatureFlag(String feature, int userId, boolean enabled) {
        redis.opsForValue().setBit("feature:" + feature, userId, enabled);
    }

    public boolean hasFeature(String feature, int userId) {
        return Boolean.TRUE.equals(
            redis.opsForValue().getBit("feature:" + feature, userId)
        );
    }
}
```

**Limitation**: User IDs must be integers (used as bit offset). Max offset: 2^32 - 1.

---

## 2.8 Data Type Selection Guide

| Need | Data Type | Why |
|------|-----------|-----|
| Simple key-value | String | Fastest, simplest |
| Object with fields | Hash | Partial reads/updates |
| Ordered queue | List | FIFO/LIFO, blocking pop |
| Unique collection | Set | Dedup, set operations |
| Ranked/scored data | Sorted Set | Range queries, ranking |
| Approximate counting | HyperLogLog | 12KB fixed memory |
| Boolean per user | Bitmap | 1 bit per user |
| Event log | Stream | Consumer groups, persistence |
| Location data | Geospatial | Radius queries |

---

## ⏭️ Next Module

Proceed to **[Module 3: Spring Data Redis](03_Spring_Data_Redis.md)** for RedisTemplate patterns, repositories, and serialization.
