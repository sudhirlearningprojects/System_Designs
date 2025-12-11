# Cross-Shard Query Solutions

## Problem Statement
If your DB is split into multiple shards, how do you run cross-shard queries (like "top 10 users globally")?

## The Challenge

```
Scenario: 100 shards, need "top 10 users by follower count"

Shard 1: User A (10M followers), User B (5M followers), ...
Shard 2: User C (8M followers), User D (3M followers), ...
Shard 3: User E (12M followers), User F (2M followers), ...
...
Shard 100: User X (7M followers), User Y (1M followers), ...

Question: How to find global top 10 without querying all 100 shards?
```

---

## Solution 1: Scatter-Gather Pattern

### Basic Approach: Query All Shards + Merge

```java
@Service
public class CrossShardQueryService {
    
    @Autowired
    private Map<Integer, DataSource> shardDataSources;
    
    private static final int SHARD_COUNT = 100;
    
    // Get top 10 users globally
    public List<User> getTopUsers(int limit) {
        List<CompletableFuture<List<User>>> futures = new ArrayList<>();
        
        // Step 1: SCATTER - Query each shard in parallel
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            int finalShardId = shardId;
            
            CompletableFuture<List<User>> future = CompletableFuture.supplyAsync(() -> {
                JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(finalShardId));
                
                // Get top 10 from this shard
                return jdbc.query(
                    "SELECT * FROM users ORDER BY follower_count DESC LIMIT ?",
                    userRowMapper,
                    limit
                );
            });
            
            futures.add(future);
        }
        
        // Step 2: GATHER - Collect results from all shards
        List<User> allResults = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        // Step 3: MERGE - Sort and take top 10
        return allResults.stream()
            .sorted(Comparator.comparing(User::getFollowerCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

### Performance Analysis

```
Shards: 100
Query time per shard: 10ms
Parallel execution: 10ms (all shards queried simultaneously)
Merge time: 5ms (sort 1000 records)
Total: 15ms

vs

Single DB: 100ms (scan entire table)
```

**Pros**: Simple, accurate
**Cons**: Queries all shards (expensive for 1000+ shards)

---

## Solution 2: Global Aggregation Table

### Pre-compute Global Stats

```sql
-- Separate table for global aggregations (not sharded)
CREATE TABLE global_user_stats (
    user_id BIGINT PRIMARY KEY,
    follower_count BIGINT NOT NULL,
    post_count BIGINT NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

CREATE INDEX idx_global_stats_followers ON global_user_stats(follower_count DESC);
```

### Sync from Shards

```java
@Service
public class GlobalStatsService {
    
    @Autowired
    private JdbcTemplate globalStatsDb;
    
    @Autowired
    private KafkaTemplate<String, UserStatsEvent> kafkaTemplate;
    
    // Update global stats when user stats change
    @KafkaListener(topics = "user-stats-events")
    public void updateGlobalStats(UserStatsEvent event) {
        globalStatsDb.update(
            "INSERT INTO global_user_stats (user_id, follower_count, post_count, last_updated) " +
            "VALUES (?, ?, ?, NOW()) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "follower_count = EXCLUDED.follower_count, " +
            "post_count = EXCLUDED.post_count, " +
            "last_updated = NOW()",
            event.getUserId(),
            event.getFollowerCount(),
            event.getPostCount()
        );
    }
    
    // Query global stats (fast, single DB)
    public List<User> getTopUsers(int limit) {
        return globalStatsDb.query(
            "SELECT * FROM global_user_stats ORDER BY follower_count DESC LIMIT ?",
            userStatsRowMapper,
            limit
        );
    }
}
```

### Architecture

```
Shard 1 → Kafka → Global Stats DB
Shard 2 → Kafka → Global Stats DB
Shard 3 → Kafka → Global Stats DB
...
Shard 100 → Kafka → Global Stats DB

Query: SELECT * FROM global_user_stats ORDER BY follower_count DESC LIMIT 10
Time: 5ms (single DB query)
```

**Pros**: Fast queries (5ms), no scatter-gather
**Cons**: Eventual consistency, extra storage

---

## Solution 3: Distributed Cache (Redis)

### Cache Global Rankings

```java
@Service
public class GlobalRankingCacheService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String TOP_USERS_KEY = "global:top_users";
    
    // Update ranking when user stats change
    public void updateUserRanking(Long userId, long followerCount) {
        // Redis Sorted Set (automatic sorting)
        redisTemplate.opsForZSet().add(TOP_USERS_KEY, userId.toString(), followerCount);
    }
    
    // Get top 10 users (O(log N) time)
    public List<Long> getTopUsers(int limit) {
        Set<String> topUserIds = redisTemplate.opsForZSet()
            .reverseRange(TOP_USERS_KEY, 0, limit - 1);
        
        return topUserIds.stream()
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
    
    // Get user rank
    public Long getUserRank(Long userId) {
        return redisTemplate.opsForZSet().reverseRank(TOP_USERS_KEY, userId.toString());
    }
    
    // Rebuild cache periodically
    @Scheduled(cron = "0 0 * * * ?") // Hourly
    public void rebuildCache() {
        // Scatter-gather from all shards
        List<User> allUsers = crossShardQueryService.getAllUsers();
        
        // Rebuild sorted set
        redisTemplate.delete(TOP_USERS_KEY);
        allUsers.forEach(user -> 
            redisTemplate.opsForZSet().add(TOP_USERS_KEY, user.getId().toString(), user.getFollowerCount())
        );
    }
}
```

**Performance**:
- Query time: <1ms (Redis in-memory)
- Update time: <1ms (single ZADD)
- Rebuild time: 1 minute (hourly)

**Pros**: Extremely fast, supports ranking queries
**Cons**: Memory usage, eventual consistency

---

## Solution 4: Elasticsearch for Aggregations

### Index All Users in Elasticsearch

```java
@Service
public class ElasticsearchAggregationService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    // Index user when created/updated
    @KafkaListener(topics = "user-events")
    public void indexUser(UserEvent event) {
        UserDocument doc = UserDocument.builder()
            .userId(event.getUserId())
            .username(event.getUsername())
            .followerCount(event.getFollowerCount())
            .postCount(event.getPostCount())
            .build();
        
        elasticsearchOperations.save(doc);
    }
    
    // Get top users (fast aggregation)
    public List<UserDocument> getTopUsers(int limit) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .withSort(SortBuilders.fieldSort("follower_count").order(SortOrder.DESC))
            .withPageable(PageRequest.of(0, limit))
            .build();
        
        SearchHits<UserDocument> hits = elasticsearchOperations.search(searchQuery, UserDocument.class);
        
        return hits.stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
    
    // Complex aggregations
    public Map<String, Long> getUsersByCountry() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .addAggregation(AggregationBuilders.terms("by_country").field("country"))
            .build();
        
        SearchHits<UserDocument> hits = elasticsearchOperations.search(searchQuery, UserDocument.class);
        
        // Extract aggregation results
        Aggregations aggregations = hits.getAggregations();
        Terms countryAgg = aggregations.get("by_country");
        
        return countryAgg.getBuckets().stream()
            .collect(Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            ));
    }
}
```

**Pros**: Fast aggregations, complex queries, full-text search
**Cons**: Extra infrastructure, eventual consistency

---

## Solution 5: Materialized Views (Pre-computed)

### PostgreSQL Materialized Views

```sql
-- Create materialized view (refreshed periodically)
CREATE MATERIALIZED VIEW global_top_users AS
SELECT 
    user_id,
    username,
    follower_count,
    post_count
FROM (
    SELECT * FROM shard_1.users
    UNION ALL
    SELECT * FROM shard_2.users
    UNION ALL
    SELECT * FROM shard_3.users
    -- ... all shards
) AS all_users
ORDER BY follower_count DESC
LIMIT 1000;

-- Create index
CREATE INDEX idx_global_top_users_followers ON global_top_users(follower_count DESC);

-- Refresh periodically (every 15 minutes)
REFRESH MATERIALIZED VIEW CONCURRENTLY global_top_users;
```

### Scheduled Refresh

```java
@Service
public class MaterializedViewService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    public void refreshMaterializedView() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY global_top_users");
    }
    
    // Query materialized view (fast)
    public List<User> getTopUsers(int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM global_top_users LIMIT ?",
            userRowMapper,
            limit
        );
    }
}
```

**Pros**: Fast queries, no extra infrastructure
**Cons**: Stale data (15-minute lag), refresh overhead

---

## Solution 6: Approximate Queries (Sampling)

### Query Subset of Shards

```java
@Service
public class ApproximateQueryService {
    
    private static final int SHARD_COUNT = 100;
    private static final int SAMPLE_SIZE = 10; // Query 10% of shards
    
    // Get approximate top users (query 10 random shards)
    public List<User> getApproximateTopUsers(int limit) {
        // Randomly select shards to query
        List<Integer> sampledShards = ThreadLocalRandom.current()
            .ints(0, SHARD_COUNT)
            .distinct()
            .limit(SAMPLE_SIZE)
            .boxed()
            .collect(Collectors.toList());
        
        List<CompletableFuture<List<User>>> futures = sampledShards.stream()
            .map(shardId -> CompletableFuture.supplyAsync(() -> {
                JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(shardId));
                return jdbc.query(
                    "SELECT * FROM users ORDER BY follower_count DESC LIMIT ?",
                    userRowMapper,
                    limit * 2 // Get more to improve accuracy
                );
            }))
            .collect(Collectors.toList());
        
        // Merge results
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(User::getFollowerCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

**Performance**:
- Query 10 shards instead of 100 (10x faster)
- Accuracy: ~90% (good enough for "trending" queries)

**Pros**: Fast, low cost
**Cons**: Approximate results

---

## Solution 7: Coordinator Service (Query Router)

### Dedicated Service for Cross-Shard Queries

```java
@Service
public class QueryCoordinatorService {
    
    @Autowired
    private ShardRegistry shardRegistry;
    
    @Autowired
    private QueryOptimizer queryOptimizer;
    
    // Intelligent query routing
    public <T> List<T> executeGlobalQuery(GlobalQuery query) {
        // Step 1: Analyze query
        QueryPlan plan = queryOptimizer.optimize(query);
        
        // Step 2: Determine which shards to query
        List<Integer> relevantShards = plan.getRelevantShards();
        
        // Step 3: Execute in parallel
        List<CompletableFuture<List<T>>> futures = relevantShards.stream()
            .map(shardId -> CompletableFuture.supplyAsync(() -> 
                executeOnShard(shardId, query)
            ))
            .collect(Collectors.toList());
        
        // Step 4: Merge results
        return mergeResults(futures, query);
    }
    
    private <T> List<T> executeOnShard(int shardId, GlobalQuery query) {
        DataSource shard = shardRegistry.getShard(shardId);
        JdbcTemplate jdbc = new JdbcTemplate(shard);
        
        return jdbc.query(query.getSql(), query.getRowMapper(), query.getParams());
    }
    
    private <T> List<T> mergeResults(List<CompletableFuture<List<T>>> futures, GlobalQuery query) {
        List<T> allResults = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        // Apply global sorting/filtering
        return query.applyGlobalOperations(allResults);
    }
}
```

---

## Solution 8: Hybrid Approach (Hot + Cold Data)

### Separate Hot Data from Cold Data

```java
@Service
public class HybridQueryService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private CrossShardQueryService crossShardService;
    
    // Get top users (hybrid approach)
    public List<User> getTopUsers(int limit) {
        // Step 1: Check cache for hot data (top 1000 users)
        Set<String> cachedTopUsers = redisTemplate.opsForZSet()
            .reverseRange("global:top_users", 0, 999);
        
        if (cachedTopUsers != null && cachedTopUsers.size() >= limit) {
            // Cache hit - return from cache
            return cachedTopUsers.stream()
                .limit(limit)
                .map(Long::parseLong)
                .map(this::getUserById)
                .collect(Collectors.toList());
        }
        
        // Step 2: Cache miss - scatter-gather from shards
        List<User> topUsers = crossShardService.getTopUsers(1000);
        
        // Step 3: Update cache
        redisTemplate.delete("global:top_users");
        topUsers.forEach(user -> 
            redisTemplate.opsForZSet().add("global:top_users", user.getId().toString(), user.getFollowerCount())
        );
        redisTemplate.expire("global:top_users", 1, TimeUnit.HOURS);
        
        return topUsers.stream().limit(limit).collect(Collectors.toList());
    }
}
```

---

## Solution 9: Bloom Filter for Existence Checks

### Optimize "Does user exist?" queries

```java
@Service
public class BloomFilterService {
    
    private Map<Integer, BloomFilter<Long>> shardBloomFilters = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeBloomFilters() {
        for (int shardId = 0; shardId < SHARD_COUNT; shardId++) {
            BloomFilter<Long> filter = BloomFilter.create(
                Funnels.longFunnel(),
                10_000_000, // Expected users per shard
                0.01 // 1% false positive rate
            );
            
            // Load all user IDs from shard
            List<Long> userIds = loadUserIdsFromShard(shardId);
            userIds.forEach(filter::put);
            
            shardBloomFilters.put(shardId, filter);
        }
    }
    
    // Find which shard contains user (without querying all shards)
    public Integer findShardForUser(Long userId) {
        for (Map.Entry<Integer, BloomFilter<Long>> entry : shardBloomFilters.entrySet()) {
            if (entry.getValue().mightContain(userId)) {
                // Possible match - verify with actual query
                if (userExistsInShard(entry.getKey(), userId)) {
                    return entry.getKey();
                }
            }
        }
        return null; // User not found
    }
}
```

---

## Solution 10: Federated Query Engine (Presto/Trino)

### Use Distributed SQL Engine

```sql
-- Presto/Trino configuration
-- Define connectors for each shard

-- catalog/shard1.properties
connector.name=postgresql
connection-url=jdbc:postgresql://shard1:5432/db
connection-user=admin

-- catalog/shard2.properties
connector.name=postgresql
connection-url=jdbc:postgresql://shard2:5432/db
connection-user=admin

-- Query across all shards (Presto handles scatter-gather)
SELECT user_id, username, follower_count
FROM (
    SELECT * FROM shard1.users
    UNION ALL
    SELECT * FROM shard2.users
    UNION ALL
    SELECT * FROM shard3.users
    -- ... all shards
)
ORDER BY follower_count DESC
LIMIT 10;
```

**Pros**: Handles complex queries, optimized execution
**Cons**: Extra infrastructure, learning curve

---

## Performance Comparison

| Solution | Query Time | Accuracy | Complexity | Cost |
|----------|-----------|----------|------------|------|
| Scatter-Gather | 15ms | 100% | Low | Low |
| Global Aggregation Table | 5ms | 99% (eventual) | Medium | Medium |
| Redis Cache | <1ms | 99% (eventual) | Low | Low |
| Elasticsearch | 10ms | 99% (eventual) | Medium | Medium |
| Materialized View | 5ms | 95% (15min lag) | Low | Low |
| Sampling | 5ms | 90% | Low | Low |
| Presto/Trino | 20ms | 100% | High | High |

---

## Decision Matrix

### When to Use Each Solution

**Scatter-Gather**:
- ✅ Need 100% accuracy
- ✅ < 100 shards
- ✅ Infrequent queries
- ❌ 1000+ shards

**Global Aggregation Table**:
- ✅ Frequent queries
- ✅ Can tolerate eventual consistency
- ✅ Simple aggregations (count, sum, top N)
- ❌ Complex joins

**Redis Cache**:
- ✅ Need <1ms latency
- ✅ Ranking queries
- ✅ Hot data (top 1000)
- ❌ Large datasets

**Elasticsearch**:
- ✅ Complex aggregations
- ✅ Full-text search
- ✅ Analytics queries
- ❌ Strong consistency required

**Materialized View**:
- ✅ PostgreSQL-only stack
- ✅ Can tolerate 15-min lag
- ✅ Simple queries
- ❌ Real-time updates

**Sampling**:
- ✅ Approximate results acceptable
- ✅ "Trending" queries
- ✅ 1000+ shards
- ❌ Need exact results

---

## Best Practice: Layered Approach

```java
@Service
public class OptimizedCrossShardQueryService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private JdbcTemplate globalStatsDb;
    
    @Autowired
    private CrossShardQueryService crossShardService;
    
    public List<User> getTopUsers(int limit) {
        // Layer 1: Redis cache (hot data, <1ms)
        Set<String> cached = redisTemplate.opsForZSet()
            .reverseRange("global:top_users", 0, limit - 1);
        
        if (cached != null && cached.size() >= limit) {
            return cached.stream()
                .map(Long::parseLong)
                .map(this::getUserById)
                .collect(Collectors.toList());
        }
        
        // Layer 2: Global aggregation table (warm data, 5ms)
        try {
            List<User> users = globalStatsDb.query(
                "SELECT * FROM global_user_stats ORDER BY follower_count DESC LIMIT ?",
                userRowMapper,
                limit
            );
            
            if (!users.isEmpty()) {
                // Update cache
                updateCache(users);
                return users;
            }
        } catch (Exception e) {
            log.warn("Global stats DB unavailable, falling back to scatter-gather");
        }
        
        // Layer 3: Scatter-gather (cold data, 15ms)
        List<User> users = crossShardService.getTopUsers(limit);
        updateCache(users);
        return users;
    }
    
    private void updateCache(List<User> users) {
        redisTemplate.delete("global:top_users");
        users.forEach(user -> 
            redisTemplate.opsForZSet().add("global:top_users", user.getId().toString(), user.getFollowerCount())
        );
        redisTemplate.expire("global:top_users", 1, TimeUnit.HOURS);
    }
}
```

---

## Key Takeaways

### For Real-time Queries (<10ms)
1. **Redis Sorted Set** - Rankings, leaderboards
2. **Global Aggregation Table** - Simple aggregations
3. **Elasticsearch** - Complex queries

### For Batch Queries (>10ms)
1. **Scatter-Gather** - Accurate results, <100 shards
2. **Materialized Views** - Periodic refresh
3. **Presto/Trino** - Complex analytics

### For Approximate Queries
1. **Sampling** - Query 10% of shards
2. **Bloom Filters** - Existence checks
3. **HyperLogLog** - Cardinality estimates

### General Strategy
1. **Cache aggressively** (Redis)
2. **Pre-compute** when possible (global tables)
3. **Scatter-gather** as last resort
4. **Use approximate** for non-critical queries

---

## Interview Answer Summary

**Question**: How to run cross-shard queries like "top 10 users globally"?

**Answer**:

**Immediate Solution** (Scatter-Gather):
1. Query all shards in parallel (10ms each)
2. Merge results (sort + take top 10)
3. Total time: 15ms for 100 shards

**Optimized Solutions**:

1. **Redis Cache** (<1ms):
   - Sorted Set for rankings
   - Update on user stats change
   - 99% accuracy (eventual consistency)

2. **Global Aggregation Table** (5ms):
   - Separate DB with pre-aggregated data
   - Sync via Kafka events
   - Single query, no scatter-gather

3. **Elasticsearch** (10ms):
   - Index all users
   - Fast aggregations
   - Complex queries supported

4. **Sampling** (5ms):
   - Query 10% of shards
   - 90% accuracy
   - Good for "trending" queries

**Best Practice**: Layered approach
- L1: Redis (hot data, <1ms)
- L2: Global table (warm data, 5ms)
- L3: Scatter-gather (cold data, 15ms)

**Result**: <1ms for 95% of queries, 15ms for cache misses
