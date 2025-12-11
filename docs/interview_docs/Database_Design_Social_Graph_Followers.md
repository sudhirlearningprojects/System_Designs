# Database Design for Social Graph - Followers Table (Instagram-like)

## Problem Statement
Your social media app (like Instagram) has users with 1 follower vs. celebrities with 100M followers. How do you store & scale the "followers" table?

## Scale Requirements
- **2B total users**
- **Regular users**: 100-1000 followers
- **Influencers**: 10K-1M followers
- **Celebrities**: 10M-100M followers
- **Average**: 200 followers per user
- **Total relationships**: 400B follower records

## Key Challenges

1. **Massive data skew** (1 follower vs 100M followers)
2. **Hot partition problem** (celebrity accounts)
3. **Fan-out on write** (posting to 100M followers)
4. **Storage explosion** (400B records)
5. **Query patterns** vary by user type

---

## Solution: Hybrid Storage Strategy

### Strategy Overview

```
User Type          Storage Strategy           Reason
─────────────────────────────────────────────────────────
Regular Users      PostgreSQL (normalized)    ACID, joins
(< 10K followers)  

Influencers        PostgreSQL + Redis Cache   Frequent reads
(10K - 1M)         

Celebrities        Graph DB + Cassandra       Massive scale
(> 1M followers)   + Separate fan-out service
```

---

## 1. Regular Users: PostgreSQL (Normalized)

### Schema Design

```sql
-- Followers table (bidirectional relationship)
CREATE TABLE followers (
    follower_id BIGINT NOT NULL,      -- User who follows
    followee_id BIGINT NOT NULL,      -- User being followed
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (follower_id, followee_id)
);

-- Indexes for both query patterns
CREATE INDEX idx_followers_followee ON followers(followee_id, follower_id);
CREATE INDEX idx_followers_created ON followers(followee_id, created_at DESC);

-- Follower count cache (denormalized)
CREATE TABLE user_stats (
    user_id BIGINT PRIMARY KEY,
    follower_count BIGINT DEFAULT 0,
    following_count BIGINT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Partition by user_id range (horizontal partitioning)
CREATE TABLE followers_0 PARTITION OF followers
    FOR VALUES FROM (0) TO (100000000);

CREATE TABLE followers_1 PARTITION OF followers
    FOR VALUES FROM (100000000) TO (200000000);
```

### Implementation

```java
@Repository
public class FollowerRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Follow a user
    @Transactional
    public void follow(Long followerId, Long followeeId) {
        // Insert relationship
        jdbcTemplate.update(
            "INSERT INTO followers (follower_id, followee_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            followerId, followeeId
        );
        
        // Update counts (denormalized)
        jdbcTemplate.update(
            "UPDATE user_stats SET follower_count = follower_count + 1 WHERE user_id = ?",
            followeeId
        );
        jdbcTemplate.update(
            "UPDATE user_stats SET following_count = following_count + 1 WHERE user_id = ?",
            followerId
        );
    }
    
    // Get followers (paginated)
    public List<Long> getFollowers(Long userId, int page, int size) {
        return jdbcTemplate.queryForList(
            "SELECT follower_id FROM followers WHERE followee_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
            Long.class,
            userId, size, page * size
        );
    }
    
    // Check if following
    public boolean isFollowing(Long followerId, Long followeeId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM followers WHERE follower_id = ? AND followee_id = ?",
            Integer.class,
            followerId, followeeId
        );
        return count != null && count > 0;
    }
}
```

---

## 2. Influencers: PostgreSQL + Redis Cache

### Redis Caching Strategy

```java
@Service
public class InfluencerFollowerService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private FollowerRepository followerRepository;
    
    private static final String FOLLOWERS_KEY = "followers:";
    private static final String FOLLOWING_KEY = "following:";
    
    // Cache follower list in Redis Set
    public void cacheFollowers(Long userId) {
        List<Long> followers = followerRepository.getFollowers(userId, 0, 10000);
        
        String key = FOLLOWERS_KEY + userId;
        followers.forEach(followerId -> 
            redisTemplate.opsForSet().add(key, followerId.toString())
        );
        
        // Expire after 1 hour
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
    
    // Check if following (O(1) lookup)
    public boolean isFollowing(Long followerId, Long followeeId) {
        String key = FOLLOWERS_KEY + followeeId;
        
        // Try cache first
        Boolean isMember = redisTemplate.opsForSet().isMember(key, followerId.toString());
        if (isMember != null) {
            return isMember;
        }
        
        // Cache miss - query DB and cache
        boolean following = followerRepository.isFollowing(followerId, followeeId);
        if (following) {
            redisTemplate.opsForSet().add(key, followerId.toString());
        }
        return following;
    }
    
    // Get follower count (cached)
    public long getFollowerCount(Long userId) {
        String countKey = "follower_count:" + userId;
        
        String cached = redisTemplate.opsForValue().get(countKey);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        
        // Query DB
        Long count = jdbcTemplate.queryForObject(
            "SELECT follower_count FROM user_stats WHERE user_id = ?",
            Long.class,
            userId
        );
        
        // Cache for 10 minutes
        redisTemplate.opsForValue().set(countKey, count.toString(), 10, TimeUnit.MINUTES);
        return count;
    }
}
```

---

## 3. Celebrities: Separate Storage + Fan-out Service

### Problem with Celebrities

```
Celebrity posts → 100M followers → 100M writes to feed table ❌
```

### Solution: Hybrid Push-Pull Model

```java
@Service
public class CelebrityFollowerService {
    
    // Store celebrity followers in Cassandra (write-optimized)
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    // Mark user as celebrity
    public void markAsCelebrity(Long userId) {
        jdbcTemplate.update(
            "UPDATE users SET is_celebrity = true WHERE user_id = ?",
            userId
        );
    }
    
    // Store followers in Cassandra (partitioned by follower_id)
    public void followCelebrity(Long followerId, Long celebrityId) {
        String cql = "INSERT INTO celebrity_followers (celebrity_id, follower_id, created_at) VALUES (?, ?, ?)";
        cassandraTemplate.getCqlOperations().execute(cql, celebrityId, followerId, Instant.now());
        
        // Increment counter
        cassandraTemplate.getCqlOperations().execute(
            "UPDATE celebrity_stats SET follower_count = follower_count + 1 WHERE celebrity_id = ?",
            celebrityId
        );
    }
}
```

### Cassandra Schema

```sql
-- Celebrity followers (partitioned by celebrity_id)
CREATE TABLE celebrity_followers (
    celebrity_id BIGINT,
    follower_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY (celebrity_id, follower_id)
) WITH CLUSTERING ORDER BY (follower_id ASC);

-- Reverse lookup (partitioned by follower_id)
CREATE TABLE celebrity_following (
    follower_id BIGINT,
    celebrity_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY (follower_id, celebrity_id)
) WITH CLUSTERING ORDER BY (celebrity_id ASC);

-- Celebrity stats (counter table)
CREATE TABLE celebrity_stats (
    celebrity_id BIGINT PRIMARY KEY,
    follower_count COUNTER
);
```

---

## 4. Graph Database for Complex Queries

For "mutual friends", "suggested follows", use Neo4j:

```cypher
// Create relationship
MATCH (follower:User {id: $followerId})
MATCH (followee:User {id: $followeeId})
CREATE (follower)-[:FOLLOWS {created_at: timestamp()}]->(followee)

// Find mutual followers
MATCH (me:User {id: $myId})-[:FOLLOWS]->(mutual)<-[:FOLLOWS]-(other:User {id: $otherId})
RETURN mutual

// Suggest follows (friends of friends)
MATCH (me:User {id: $myId})-[:FOLLOWS]->()-[:FOLLOWS]->(suggested)
WHERE NOT (me)-[:FOLLOWS]->(suggested) AND me <> suggested
RETURN suggested, COUNT(*) as mutual_count
ORDER BY mutual_count DESC
LIMIT 10
```

---

## 5. Sharding Strategy

### Shard by User ID Range

```
User ID Range        Shard       Followers
─────────────────────────────────────────────
0 - 100M            Shard 0     40B records
100M - 200M         Shard 1     40B records
200M - 300M         Shard 2     40B records
...
```

### Implementation

```java
@Service
public class ShardedFollowerService {
    
    private static final int SHARD_COUNT = 10;
    
    @Autowired
    private Map<Integer, DataSource> shardDataSources;
    
    private int getShardId(Long userId) {
        return (int) (userId % SHARD_COUNT);
    }
    
    public void follow(Long followerId, Long followeeId) {
        // Write to both shards (bidirectional)
        int followerShard = getShardId(followerId);
        int followeeShard = getShardId(followeeId);
        
        // Insert into follower's shard (following list)
        JdbcTemplate followerJdbc = new JdbcTemplate(shardDataSources.get(followerShard));
        followerJdbc.update(
            "INSERT INTO following (user_id, following_id) VALUES (?, ?)",
            followerId, followeeId
        );
        
        // Insert into followee's shard (followers list)
        JdbcTemplate followeeJdbc = new JdbcTemplate(shardDataSources.get(followeeShard));
        followeeJdbc.update(
            "INSERT INTO followers (user_id, follower_id) VALUES (?, ?)",
            followeeId, followerId
        );
    }
    
    public List<Long> getFollowers(Long userId, int page, int size) {
        int shardId = getShardId(userId);
        JdbcTemplate jdbc = new JdbcTemplate(shardDataSources.get(shardId));
        
        return jdbc.queryForList(
            "SELECT follower_id FROM followers WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
            Long.class,
            userId, size, page * size
        );
    }
}
```

---

## 6. Denormalization for Performance

### Materialized Follower Counts

```sql
-- Trigger to update counts
CREATE OR REPLACE FUNCTION update_follower_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE user_stats SET follower_count = follower_count + 1 WHERE user_id = NEW.followee_id;
        UPDATE user_stats SET following_count = following_count + 1 WHERE user_id = NEW.follower_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE user_stats SET follower_count = follower_count - 1 WHERE user_id = OLD.followee_id;
        UPDATE user_stats SET following_count = following_count - 1 WHERE user_id = OLD.follower_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER follower_count_trigger
AFTER INSERT OR DELETE ON followers
FOR EACH ROW EXECUTE FUNCTION update_follower_count();
```

---

## 7. Feed Generation Strategy

### Regular Users: Push Model (Fan-out on Write)

```java
@Service
public class FeedService {
    
    @Async
    public void fanOutPost(Long userId, Long postId) {
        // Get followers (< 10K)
        List<Long> followers = followerRepository.getFollowers(userId, 0, 10000);
        
        // Write to each follower's feed
        followers.forEach(followerId -> {
            jdbcTemplate.update(
                "INSERT INTO user_feed (user_id, post_id, created_at) VALUES (?, ?, NOW())",
                followerId, postId
            );
        });
    }
}
```

### Celebrities: Pull Model (Fan-out on Read)

```java
@Service
public class CelebrityFeedService {
    
    public List<Post> getFeed(Long userId) {
        // Get celebrities user follows
        List<Long> celebrities = jdbcTemplate.queryForList(
            "SELECT celebrity_id FROM celebrity_following WHERE follower_id = ?",
            Long.class,
            userId
        );
        
        // Fetch recent posts from celebrities (on-demand)
        List<Post> posts = new ArrayList<>();
        celebrities.forEach(celebrityId -> {
            List<Post> celebrityPosts = jdbcTemplate.query(
                "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC LIMIT 10",
                postRowMapper,
                celebrityId
            );
            posts.addAll(celebrityPosts);
        });
        
        // Sort by timestamp
        posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
        return posts.subList(0, Math.min(50, posts.size()));
    }
}
```

---

## 8. Complete Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│  Regular Users   │                  │   Celebrities    │
│  (< 10K)         │                  │   (> 1M)         │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│   PostgreSQL     │                  │   Cassandra      │
│   (Sharded)      │                  │   (Distributed)  │
│                  │                  │                  │
│ - followers      │                  │ - celebrity_     │
│ - user_stats     │                  │   followers      │
│ - Partitioned    │                  │ - Wide rows      │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
┌──────────────────┐                  ┌──────────────────┐
│   Redis Cache    │                  │   Redis Cache    │
│                  │                  │                  │
│ - Follower sets  │                  │ - Count only     │
│ - Count cache    │                  │ - No full list   │
└──────────────────┘                  └──────────────────┘
        ↓                                       ↓
        └───────────────────┬───────────────────┘
                            ↓
                  ┌──────────────────┐
                  │   Neo4j Graph    │
                  │                  │
                  │ - Mutual friends │
                  │ - Suggestions    │
                  └──────────────────┘
```

---

## 9. Storage Comparison

| User Type | Followers | Storage | Query Time | Write Time |
|-----------|-----------|---------|------------|------------|
| Regular | 100-1K | PostgreSQL | 10ms | 5ms |
| Influencer | 10K-1M | PostgreSQL + Redis | 2ms (cached) | 10ms |
| Celebrity | 10M-100M | Cassandra | 50ms (paginated) | 5ms |

---

## 10. Optimization Techniques

### A. Bloom Filter for "Is Following" Check

```java
@Service
public class BloomFilterFollowerService {
    
    private BloomFilter<Long> followingBloomFilter;
    
    @PostConstruct
    public void initBloomFilter() {
        // Expected 1M following relationships per user
        followingBloomFilter = BloomFilter.create(
            Funnels.longFunnel(),
            1_000_000,
            0.01 // 1% false positive rate
        );
    }
    
    public boolean mightBeFollowing(Long followerId, Long followeeId) {
        // Quick negative check (100% accurate for "not following")
        if (!followingBloomFilter.mightContain(followeeId)) {
            return false;
        }
        
        // Possible positive - verify with DB
        return followerRepository.isFollowing(followerId, followeeId);
    }
}
```

### B. Approximate Counts for Celebrities

```java
// Use HyperLogLog for approximate follower counts
public long getApproximateFollowerCount(Long celebrityId) {
    String key = "hll:followers:" + celebrityId;
    return redisTemplate.opsForHyperLogLog().size(key);
}

public void addFollower(Long celebrityId, Long followerId) {
    String key = "hll:followers:" + celebrityId;
    redisTemplate.opsForHyperLogLog().add(key, followerId);
}
```

### C. Pagination with Cursor

```java
// Avoid OFFSET for large datasets
public List<Long> getFollowersCursor(Long userId, Long lastFollowerId, int size) {
    return jdbcTemplate.queryForList(
        "SELECT follower_id FROM followers WHERE followee_id = ? AND follower_id > ? ORDER BY follower_id LIMIT ?",
        Long.class,
        userId, lastFollowerId, size
    );
}
```

---

## 11. Monitoring & Metrics

```java
@Component
public class FollowerMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    public void recordFollow(String userType, long latency) {
        registry.timer("follower.follow", "type", userType)
            .record(latency, TimeUnit.MILLISECONDS);
    }
    
    public void recordFollowerQuery(String userType, int followerCount, long latency) {
        registry.timer("follower.query", "type", userType)
            .record(latency, TimeUnit.MILLISECONDS);
        
        registry.gauge("follower.count", Tags.of("type", userType), followerCount);
    }
}
```

---

## 12. Cost Analysis

### Storage Costs (400B relationships)

| Storage | Size per Record | Total Size | Cost/Month |
|---------|----------------|------------|------------|
| PostgreSQL | 24 bytes | 9.6TB | $2,000 |
| Cassandra | 32 bytes | 12.8TB | $1,500 |
| Redis Cache | 16 bytes (IDs only) | 100GB (hot data) | $500 |
| Neo4j | 40 bytes | 16TB | $5,000 |

**Total: ~$4,000-9,000/month** depending on strategy

---

## Key Takeaways

1. **Hybrid storage** based on user type (regular vs celebrity)
2. **PostgreSQL** for regular users (< 10K followers)
3. **Cassandra** for celebrities (> 1M followers)
4. **Redis cache** for hot data and counts
5. **Denormalize counts** to avoid expensive COUNT(*) queries
6. **Shard by user_id** to distribute load
7. **Bloom filters** for quick negative checks
8. **Cursor pagination** instead of OFFSET
9. **Push model** for regular users, **pull model** for celebrities
10. **Graph DB** for complex relationship queries

---

## Interview Answer Summary

**Question**: How do you store followers for users with 1 follower vs 100M followers?

**Answer**:
1. **Hybrid approach** - different storage for different user types
2. **Regular users** (< 10K): PostgreSQL with sharding
3. **Celebrities** (> 1M): Cassandra for write scalability
4. **Cache aggressively** - Redis for follower counts and hot data
5. **Denormalize counts** - avoid expensive aggregations
6. **Fan-out strategy** - push for regular, pull for celebrities
7. **Bloom filters** - optimize "is following" checks
8. **Partition/shard** - distribute 400B records across nodes

This handles the massive data skew while maintaining performance for both query patterns.
