# Redis - Complete Deep Dive Guide

## Table of Contents
1. [Introduction to Redis](#introduction-to-redis)
2. [Redis Architecture](#redis-architecture)
3. [Data Structures](#data-structures)
4. [Internal Working](#internal-working)
5. [Redis with Spring Boot](#redis-with-spring-boot)
6. [Advanced Features](#advanced-features)
7. [Performance & Optimization](#performance--optimization)
8. [Clustering & High Availability](#clustering--high-availability)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction to Redis

Redis (Remote Dictionary Server) is an in-memory data structure store used as a database, cache, and message broker. It supports various data structures and provides high performance with sub-millisecond latency.

### Key Characteristics
- **In-Memory**: All data stored in RAM for ultra-fast access
- **Persistent**: Optional disk persistence (RDB snapshots, AOF logs)
- **Single-Threaded**: Event-driven, non-blocking I/O
- **Atomic Operations**: All operations are atomic
- **Rich Data Types**: Strings, Lists, Sets, Hashes, Sorted Sets, etc.

### Use Cases
- **Caching**: Session storage, page caching, API response caching
- **Real-time Analytics**: Counters, leaderboards, rate limiting
- **Message Queuing**: Pub/Sub, task queues
- **Session Management**: User sessions, shopping carts
- **Geospatial**: Location-based services
- **Time Series**: Metrics, monitoring data

## Redis Architecture

### Single Instance Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Redis Server                        │
│  ┌─────────────────────────────────────────────────────┤
│  │              Event Loop (Single Thread)            │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │  │   Network   │  │   Command   │  │   Memory    │ │
│  │  │   Handler   │  │  Processor  │  │   Manager   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │
│  └─────────────────────────────────────────────────────┤
│  │                 Data Structures                     │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │  │ Strings │ │  Lists  │ │  Sets   │ │ Hashes  │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│  └─────────────────────────────────────────────────────┤
│  │                  Persistence                        │
│  │  ┌─────────────┐              ┌─────────────┐       │
│  │  │ RDB Snapshots│              │  AOF Logs   │       │
│  │  └─────────────┘              └─────────────┘       │
│  └─────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────┘
```

### Memory Layout

```
Redis Memory Layout:
┌─────────────────────────────────────────────────────────┐
│ Used Memory                                             │
├─────────────────────────────────────────────────────────┤
│ Data (Keys + Values)           │ 80-90%                 │
│ Overhead (Expiry, Type info)   │ 5-10%                  │
│ Fragmentation                  │ 5-10%                  │
│ Buffers (Input/Output)         │ 1-5%                   │
└─────────────────────────────────────────────────────────┘
```

## Data Structures

### 1. Strings (SDS - Simple Dynamic Strings)

```c
// Internal structure
struct sdshdr {
    int len;        // String length
    int free;       // Free space
    char buf[];     // Character array
};
```

```java
// Operations
redisTemplate.opsForValue().set("user:1:name", "John");
redisTemplate.opsForValue().get("user:1:name");
redisTemplate.opsForValue().increment("counter");
redisTemplate.opsForValue().setIfAbsent("lock:resource", "locked");
```

### 2. Lists (Doubly Linked List + Ziplist)

```
List Structure:
┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐
│ A   │<-->│ B   │<-->│ C   │<-->│ D   │
└─────┘    └─────┘    └─────┘    └─────┘
  ^                                  ^
 head                               tail
```

```java
// Operations
redisTemplate.opsForList().leftPush("queue", "task1");
redisTemplate.opsForList().rightPop("queue");
redisTemplate.opsForList().range("queue", 0, -1);
```

### 3. Sets (Hash Table + IntSet)

```java
// Operations
redisTemplate.opsForSet().add("users:online", "user1", "user2");
redisTemplate.opsForSet().isMember("users:online", "user1");
redisTemplate.opsForSet().intersect("set1", "set2");
```

### 4. Hashes (Hash Table + Ziplist)

```
Hash Structure:
Key: user:1
┌─────────────────────────────────────┐
│ name    → "John"                    │
│ email   → "john@example.com"        │
│ age     → "30"                      │
│ city    → "New York"                │
└─────────────────────────────────────┘
```

```java
// Operations
redisTemplate.opsForHash().put("user:1", "name", "John");
redisTemplate.opsForHash().get("user:1", "name");
redisTemplate.opsForHash().entries("user:1");
```

### 5. Sorted Sets (Skip List + Hash Table)

```
Skip List Structure:
Level 3: [1] ────────────────────────> [9]
Level 2: [1] ──────> [5] ──────────> [9]
Level 1: [1] ──> [3] ──> [5] ──> [7] ──> [9]
Level 0: [1] ──> [3] ──> [5] ──> [7] ──> [9]
```

```java
// Operations
redisTemplate.opsForZSet().add("leaderboard", "player1", 100);
redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);
redisTemplate.opsForZSet().rank("leaderboard", "player1");
```

## Internal Working

### Event Loop (Single-Threaded)

```c
// Simplified event loop
while (server_running) {
    // 1. Handle network events (accept connections, read commands)
    handle_network_events();
    
    // 2. Process commands
    process_commands();
    
    // 3. Handle timers and background tasks
    handle_timers();
    
    // 4. Write responses to clients
    write_responses();
}
```

### Memory Management

#### Object Structure
```c
typedef struct redisObject {
    unsigned type:4;        // Data type (string, list, etc.)
    unsigned encoding:4;    // Encoding type
    unsigned lru:24;        // LRU time
    int refcount;          // Reference count
    void *ptr;             // Pointer to actual data
} robj;
```

#### Memory Optimization Techniques

1. **Small Integers**: Cached integers (-1 to 10000)
2. **Shared Objects**: Common strings like "OK", "PONG"
3. **Ziplist**: Compact encoding for small collections
4. **IntSet**: Compact encoding for integer sets

### Persistence Mechanisms

#### 1. RDB (Redis Database Backup)
```
RDB Process:
┌─────────────┐    fork()    ┌─────────────┐
│   Parent    │ ──────────> │    Child    │
│   Process   │             │   Process   │
│             │             │             │
│ Continue    │             │ Write RDB   │
│ serving     │             │ to disk     │
│ clients     │             │             │
└─────────────┘             └─────────────┘
```

#### 2. AOF (Append Only File)
```
AOF Process:
Command → Buffer → fsync() → Disk
SET key val → "SET key val\r\n" → Write → Persist
```

### Expiration and Eviction

#### Expiration Strategies
1. **Passive**: Check on key access
2. **Active**: Periodic sampling and deletion

#### Eviction Policies
```java
// Configuration
maxmemory 2gb
maxmemory-policy allkeys-lru

// Policies:
// noeviction, allkeys-lru, volatile-lru, allkeys-random, 
// volatile-random, volatile-ttl, allkeys-lfu, volatile-lfu
```

## Redis with Spring Boot

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### Configuration

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        config.setPassword("password");
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ZERO)
            .build();
            
        return new LettuceConnectionFactory(config, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // Serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
                
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .build();
    }
}
```

### Basic Operations

```java
@Service
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // String operations
    public void setString(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }
    
    public String getString(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
    
    // Hash operations
    public void setHash(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }
    
    public Object getHash(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }
    
    // List operations
    public void pushToList(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }
    
    public Object popFromList(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }
    
    // Set operations
    public void addToSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }
    
    public Set<Object> getSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }
    
    // Sorted Set operations
    public void addToSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }
    
    public Set<Object> getSortedSetRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }
}
```

### Caching with Annotations

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        // Expensive database operation
        return userRepository.findById(id);
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsers() {
        // Clear all user cache entries
    }
}
```

### Session Management

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
    // Redis-backed HTTP sessions
}

@RestController
public class SessionController {
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, 
                                  HttpServletRequest httpRequest) {
        // Authenticate user
        User user = authService.authenticate(request.getUsername(), request.getPassword());
        
        // Store in session (automatically stored in Redis)
        HttpSession session = httpRequest.getSession();
        session.setAttribute("user", user);
        
        return ResponseEntity.ok(new LoginResponse("Success"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
```

### Distributed Locking

```java
@Component
public class RedisDistributedLock {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) else return 0 end";
    
    public boolean tryLock(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(LOCK_PREFIX + key, value, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
    
    public boolean unlock(String key, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(LOCK_PREFIX + key), value);
        return Long.valueOf(1).equals(result);
    }
}

@Service
public class PaymentService {
    
    @Autowired
    private RedisDistributedLock distributedLock;
    
    public void processPayment(String userId, BigDecimal amount) {
        String lockKey = "payment:" + userId;
        String lockValue = UUID.randomUUID().toString();
        
        if (distributedLock.tryLock(lockKey, lockValue, 30)) {
            try {
                // Process payment logic
                processPaymentInternal(userId, amount);
            } finally {
                distributedLock.unlock(lockKey, lockValue);
            }
        } else {
            throw new RuntimeException("Unable to acquire lock for payment processing");
        }
    }
}
```

### Pub/Sub Messaging

```java
@Configuration
public class RedisPubSubConfig {
    
    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory());
        container.addMessageListener(new PaymentEventListener(), new PatternTopic("payment.*"));
        return container;
    }
}

@Component
public class PaymentEventListener implements MessageListener {
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        
        log.info("Received message on channel {}: {}", channel, body);
        
        // Process the payment event
        processPaymentEvent(body);
    }
}

@Service
public class PaymentEventPublisher {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void publishPaymentEvent(PaymentEvent event) {
        redisTemplate.convertAndSend("payment.processed", event);
    }
}
```

## Advanced Features

### Lua Scripting

```java
@Service
public class RedisLuaService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Rate limiting with Lua script
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('GET', key) " +
        "if current == false then " +
        "  redis.call('SET', key, 1) " +
        "  redis.call('EXPIRE', key, window) " +
        "  return 1 " +
        "elseif tonumber(current) < limit then " +
        "  return redis.call('INCR', key) " +
        "else " +
        "  return -1 " +
        "end";
    
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RATE_LIMIT_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key), limit, windowSeconds);
        
        return result != null && result > 0;
    }
}
```

### Geospatial Operations

```java
@Service
public class LocationService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void addLocation(String key, String member, double longitude, double latitude) {
        redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
    }
    
    public List<Point> getLocations(String key, String... members) {
        return redisTemplate.opsForGeo().position(key, members);
    }
    
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> findNearby(
            String key, double longitude, double latitude, double radius) {
        
        Circle circle = new Circle(new Point(longitude, latitude), 
            new Distance(radius, Metrics.KILOMETERS));
        
        return redisTemplate.opsForGeo().radius(key, circle);
    }
}
```

### HyperLogLog (Cardinality Estimation)

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void trackUniqueVisitor(String date, String userId) {
        String key = "unique_visitors:" + date;
        redisTemplate.opsForHyperLogLog().add(key, userId);
    }
    
    public Long getUniqueVisitorCount(String date) {
        String key = "unique_visitors:" + date;
        return redisTemplate.opsForHyperLogLog().size(key);
    }
    
    public Long getUniqueVisitorCountForPeriod(String... dates) {
        String[] keys = Arrays.stream(dates)
            .map(date -> "unique_visitors:" + date)
            .toArray(String[]::new);
        
        return redisTemplate.opsForHyperLogLog().union("temp_union", keys);
    }
}
```

## Performance & Optimization

### Connection Pooling

```java
@Configuration
public class RedisPoolConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = 
            new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .poolingClientConfiguration(LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .build())
            .build();
            
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(), clientConfig);
    }
}
```

### Pipeline Operations

```java
@Service
public class RedisPipelineService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void batchOperations(Map<String, Object> data) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    connection.set(entry.getKey().getBytes(), 
                        serialize(entry.getValue()));
                }
                return null;
            }
        });
    }
}
```

### Memory Optimization

```java
// Configuration for memory optimization
@Configuration
public class RedisMemoryConfig {
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(60))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}

// Use appropriate data structures
@Service
public class OptimizedRedisService {
    
    // Use hashes for objects instead of serialized strings
    public void saveUser(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("age", user.getAge());
        
        redisTemplate.opsForHash().putAll("user:" + user.getId(), userMap);
    }
    
    // Use sets for unique collections
    public void addToUserGroup(String groupId, String userId) {
        redisTemplate.opsForSet().add("group:" + groupId, userId);
    }
}
```

## Clustering & High Availability

### Redis Sentinel Configuration

```java
@Configuration
public class RedisSentinelConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("127.0.0.1", 26379)
            .sentinel("127.0.0.1", 26380)
            .sentinel("127.0.0.1", 26381);
            
        return new LettuceConnectionFactory(sentinelConfig);
    }
}
```

### Redis Cluster Configuration

```java
@Configuration
public class RedisClusterConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        clusterConfig.clusterNode("127.0.0.1", 7000);
        clusterConfig.clusterNode("127.0.0.1", 7001);
        clusterConfig.clusterNode("127.0.0.1", 7002);
        clusterConfig.clusterNode("127.0.0.1", 7003);
        clusterConfig.clusterNode("127.0.0.1", 7004);
        clusterConfig.clusterNode("127.0.0.1", 7005);
        
        return new LettuceConnectionFactory(clusterConfig);
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Redis and what are its main advantages?**

**Answer:** Redis is an in-memory data structure store that can be used as a database, cache, and message broker. Main advantages:
- **Speed**: In-memory storage provides sub-millisecond latency
- **Rich Data Types**: Strings, Lists, Sets, Hashes, Sorted Sets, etc.
- **Atomic Operations**: All operations are atomic
- **Persistence**: Optional RDB snapshots and AOF logs
- **High Availability**: Replication, Sentinel, and Clustering support
- **Pub/Sub**: Built-in messaging capabilities

**Q2: Explain Redis data types and their use cases.**

**Answer:**
- **Strings**: Caching, counters, session tokens
- **Lists**: Message queues, activity feeds, recent items
- **Sets**: Unique items, tags, real-time analytics
- **Hashes**: Object storage, user profiles
- **Sorted Sets**: Leaderboards, time-series data, priority queues
- **Bitmaps**: Real-time analytics, user activity tracking
- **HyperLogLog**: Cardinality estimation, unique visitors
- **Geospatial**: Location-based services, nearby searches

**Q3: What is the difference between Redis and Memcached?**

**Answer:**
| Feature | Redis | Memcached |
|---------|-------|-----------|
| Data Types | Rich (String, List, Set, etc.) | Only Strings |
| Persistence | RDB + AOF | None |
| Threading | Single-threaded | Multi-threaded |
| Memory Usage | Higher overhead | Lower overhead |
| Advanced Features | Pub/Sub, Lua scripts, Clustering | Basic caching |
| Use Cases | Database, Cache, Message Broker | Simple caching |

### Intermediate Level

**Q4: Explain Redis persistence mechanisms.**

**Answer:**

**RDB (Redis Database Backup):**
- Point-in-time snapshots
- Compact binary format
- Good for backups and disaster recovery
- May lose data between snapshots

```redis
# Configuration
save 900 1      # Save if at least 1 key changed in 900 seconds
save 300 10     # Save if at least 10 keys changed in 300 seconds
save 60 10000   # Save if at least 10000 keys changed in 60 seconds
```

**AOF (Append Only File):**
- Logs every write operation
- Better durability (configurable fsync)
- Larger file size
- Slower restart times

```redis
# Configuration
appendonly yes
appendfsync everysec  # always, everysec, no
```

**Q5: How does Redis handle memory management and what happens when memory is full?**

**Answer:**

**Memory Management:**
- All data stored in RAM
- Memory fragmentation handled by jemalloc
- Expiration handled by passive + active strategies

**Eviction Policies when maxmemory reached:**
```redis
maxmemory 2gb
maxmemory-policy allkeys-lru

# Policies:
# noeviction - Return errors when memory limit reached
# allkeys-lru - Evict least recently used keys
# volatile-lru - Evict LRU keys with expire set
# allkeys-random - Evict random keys
# volatile-random - Evict random keys with expire set
# volatile-ttl - Evict keys with shortest TTL
# allkeys-lfu - Evict least frequently used keys
# volatile-lfu - Evict LFU keys with expire set
```

**Q6: Explain Redis replication and how it works.**

**Answer:**

**Master-Slave Replication:**
```
Master ──────> Slave 1
   │
   └────────> Slave 2
```

**Replication Process:**
1. Slave connects to master
2. Master starts background save (RDB)
3. Master buffers new commands
4. Master sends RDB to slave
5. Slave loads RDB and applies buffered commands
6. Continuous replication of new commands

```redis
# Slave configuration
replicaof 192.168.1.100 6379
replica-read-only yes
```

### Advanced Level

**Q7: Design a distributed rate limiter using Redis.**

**Answer:**

**Sliding Window Rate Limiter:**
```java
@Service
public class SlidingWindowRateLimiter {
    
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local limit = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local clearBefore = now - window " +
        
        "redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore) " +
        "local current = redis.call('ZCARD', key) " +
        
        "if current < limit then " +
        "  redis.call('ZADD', key, now, now) " +
        "  redis.call('EXPIRE', key, window) " +
        "  return {1, limit - current - 1} " +
        "else " +
        "  return {0, 0} " +
        "end";
    
    public RateLimitResult isAllowed(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        
        List<Long> result = redisTemplate.execute(
            new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, List.class),
            Collections.singletonList(key),
            windowSeconds * 1000, limit, now
        );
        
        return new RateLimitResult(result.get(0) == 1, result.get(1));
    }
}
```

**Q8: How would you implement a distributed cache with Redis for a high-traffic application?**

**Answer:**

**Multi-Level Caching Strategy:**
```java
@Service
public class DistributedCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final LoadingCache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(this::loadFromRedis);
    
    public Object get(String key) {
        try {
            // L1: Local cache
            return localCache.get(key);
        } catch (Exception e) {
            // L2: Redis cache
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return value;
            }
            
            // L3: Database
            return loadFromDatabase(key);
        }
    }
    
    public void put(String key, Object value, Duration ttl) {
        // Write to Redis
        redisTemplate.opsForValue().set(key, value, ttl);
        
        // Invalidate local cache
        localCache.invalidate(key);
        
        // Notify other instances via pub/sub
        redisTemplate.convertAndSend("cache.invalidate", key);
    }
    
    @EventListener
    public void handleCacheInvalidation(String key) {
        localCache.invalidate(key);
    }
}
```

**Q9: Explain Redis Cluster and how data is distributed.**

**Answer:**

**Hash Slot Distribution:**
- 16384 hash slots total
- Each key mapped to slot using CRC16(key) % 16384
- Slots distributed across cluster nodes
- Each node handles subset of slots

```
Cluster Layout:
Node A: Slots 0-5460
Node B: Slots 5461-10922  
Node C: Slots 10923-16383

Key Distribution:
"user:1" → CRC16("user:1") % 16384 = 9842 → Node B
"user:2" → CRC16("user:2") % 16384 = 2345 → Node A
```

**Cluster Operations:**
```java
@Service
public class RedisClusterService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Multi-key operations require same slot
    public void multiKeyOperation() {
        // Use hash tags to ensure same slot
        String user = "user:123";
        String profile = "{user:123}:profile";
        String settings = "{user:123}:settings";
        
        // All keys will be in same slot due to {user:123} hash tag
        redisTemplate.multi();
        redisTemplate.opsForValue().set(profile, profileData);
        redisTemplate.opsForValue().set(settings, settingsData);
        redisTemplate.exec();
    }
}
```

**Q10: How would you handle Redis failover and ensure high availability?**

**Answer:**

**Redis Sentinel Setup:**
```yaml
# Sentinel configuration
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
```

**Application Configuration:**
```java
@Configuration
public class RedisHAConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("sentinel1", 26379)
            .sentinel("sentinel2", 26379)
            .sentinel("sentinel3", 26379);
            
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED) // Read from replicas when possible
            .build();
            
        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // Enable connection pooling
        template.setEnableTransactionSupport(true);
        
        return template;
    }
}
```

### Expert Level

**Q11: Design a real-time leaderboard system using Redis that can handle millions of users.**

**Answer:**

```java
@Service
public class RealtimeLeaderboard {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Update user score
    public void updateScore(String leaderboardId, String userId, double score) {
        String key = "leaderboard:" + leaderboardId;
        
        // Use Lua script for atomic operations
        String script = 
            "local key = KEYS[1] " +
            "local userId = ARGV[1] " +
            "local score = tonumber(ARGV[2]) " +
            "local currentScore = redis.call('ZSCORE', key, userId) " +
            
            "if currentScore then " +
            "  redis.call('ZADD', key, currentScore + score, userId) " +
            "else " +
            "  redis.call('ZADD', key, score, userId) " +
            "end " +
            
            "return redis.call('ZREVRANK', key, userId)";
        
        Long rank = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            userId, score
        );
        
        // Update user's rank in separate key for quick lookup
        redisTemplate.opsForValue().set("rank:" + leaderboardId + ":" + userId, rank);
    }
    
    // Get top N users
    public List<LeaderboardEntry> getTopUsers(String leaderboardId, int count) {
        String key = "leaderboard:" + leaderboardId;
        
        Set<ZSetOperations.TypedTuple<Object>> results = 
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, count - 1);
        
        return results.stream()
            .map(tuple -> new LeaderboardEntry(
                (String) tuple.getValue(),
                tuple.getScore().longValue()
            ))
            .collect(Collectors.toList());
    }
    
    // Get user rank and score
    public UserRankInfo getUserRank(String leaderboardId, String userId) {
        String key = "leaderboard:" + leaderboardId;
        
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId);
        Double score = redisTemplate.opsForZSet().score(key, userId);
        
        return new UserRankInfo(userId, rank, score);
    }
    
    // Get users around specific rank
    public List<LeaderboardEntry> getUsersAroundRank(String leaderboardId, long rank, int range) {
        String key = "leaderboard:" + leaderboardId;
        
        long start = Math.max(0, rank - range);
        long end = rank + range;
        
        Set<ZSetOperations.TypedTuple<Object>> results = 
            redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        
        return results.stream()
            .map(tuple -> new LeaderboardEntry(
                (String) tuple.getValue(),
                tuple.getScore().longValue()
            ))
            .collect(Collectors.toList());
    }
}
```

**Q12: Implement a distributed session store with Redis that handles session clustering and failover.**

**Answer:**

```java
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800,
    redisNamespace = "spring:session"
)
public class RedisSessionConfig {
    
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        serializer.setHttpOnly(true);
        serializer.setSecure(true);
        serializer.setUseSecureCookie(true);
        return serializer;
    }
    
    @Bean
    public RedisOperationsSessionRepository sessionRepository() {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        RedisOperationsSessionRepository repository = 
            new RedisOperationsSessionRepository(redisTemplate);
        
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));
        repository.setRedisKeyNamespace("myapp:session");
        
        return repository;
    }
}

@Service
public class SessionService {
    
    @Autowired
    private RedisOperationsSessionRepository sessionRepository;
    
    public void createSession(String userId, Map<String, Object> attributes) {
        RedisOperationsSessionRepository.RedisSession session = 
            sessionRepository.createSession();
        
        session.setAttribute("userId", userId);
        attributes.forEach(session::setAttribute);
        
        sessionRepository.save(session);
    }
    
    public Map<String, Object> getSessionData(String sessionId) {
        RedisOperationsSessionRepository.RedisSession session = 
            sessionRepository.findById(sessionId);
        
        if (session != null) {
            return session.getAttributeNames().stream()
                .collect(Collectors.toMap(
                    name -> name,
                    session::getAttribute
                ));
        }
        
        return Collections.emptyMap();
    }
    
    public void invalidateSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
```

## Best Practices

### Performance Best Practices

```java
// 1. Use appropriate data structures
@Service
public class OptimizedRedisOperations {
    
    // Use hashes for objects instead of serialized JSON
    public void saveUserOptimized(User user) {
        Map<String, String> userHash = Map.of(
            "name", user.getName(),
            "email", user.getEmail(),
            "age", String.valueOf(user.getAge())
        );
        redisTemplate.opsForHash().putAll("user:" + user.getId(), userHash);
    }
    
    // Use pipelines for batch operations
    public void batchUpdate(Map<String, Object> data) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            data.forEach((key, value) -> 
                connection.set(key.getBytes(), serialize(value)));
            return null;
        });
    }
    
    // Use Lua scripts for atomic operations
    public boolean transferPoints(String fromUser, String toUser, int points) {
        String script = 
            "local from = KEYS[1] " +
            "local to = KEYS[2] " +
            "local points = tonumber(ARGV[1]) " +
            "local fromPoints = tonumber(redis.call('GET', from) or 0) " +
            
            "if fromPoints >= points then " +
            "  redis.call('DECRBY', from, points) " +
            "  redis.call('INCRBY', to, points) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList("points:" + fromUser, "points:" + toUser),
            points
        );
        
        return result != null && result == 1;
    }
}
```

### Security Best Practices

```java
@Configuration
public class RedisSecurityConfig {
    
    @Bean
    public LettuceConnectionFactory secureRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("redis.example.com");
        config.setPort(6380); // Non-default port
        config.setPassword("strong-password");
        config.setDatabase(0);
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .useSsl()
            .and()
            .commandTimeout(Duration.ofSeconds(2))
            .build();
            
        return new LettuceConnectionFactory(config, clientConfig);
    }
}

// Key naming conventions
@Service
public class SecureRedisService {
    
    private static final String KEY_PREFIX = "myapp:";
    
    public void setSecureKey(String userInput, Object value) {
        // Sanitize user input
        String sanitizedKey = sanitizeKey(userInput);
        String fullKey = KEY_PREFIX + sanitizedKey;
        
        redisTemplate.opsForValue().set(fullKey, value, Duration.ofHours(1));
    }
    
    private String sanitizeKey(String input) {
        return input.replaceAll("[^a-zA-Z0-9:_-]", "");
    }
}
```

### Monitoring and Alerting

```java
@Component
public class RedisHealthMonitor {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Scheduled(fixedRate = 30000)
    public void monitorRedisHealth() {
        try {
            // Test connectivity
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            
            if (!"PONG".equals(pong)) {
                alertService.sendAlert("Redis connectivity issue");
            }
            
            // Monitor memory usage
            Properties info = redisTemplate.getConnectionFactory()
                .getConnection().info("memory");
            
            String usedMemory = info.getProperty("used_memory");
            String maxMemory = info.getProperty("maxmemory");
            
            if (usedMemory != null && maxMemory != null) {
                double usage = Double.parseDouble(usedMemory) / Double.parseDouble(maxMemory);
                if (usage > 0.8) {
                    alertService.sendAlert("Redis memory usage high: " + (usage * 100) + "%");
                }
            }
            
        } catch (Exception e) {
            alertService.sendAlert("Redis health check failed: " + e.getMessage());
        }
    }
}
```

This comprehensive Redis guide covers internal architecture, Spring Boot integration, advanced features, and real-world interview scenarios with practical implementations for building scalable, high-performance applications.