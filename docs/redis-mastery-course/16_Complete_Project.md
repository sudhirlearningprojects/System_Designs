# Module 16: Complete Project

## 🎯 Bringing It All Together

This module provides the final integration: Docker Compose, testing with Testcontainers, and a summary of all Redis concepts applied.

---

## 16.1 Final Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  redis:
    image: redis/redis-stack:latest
    ports:
      - "6379:6379"
      - "8001:8001"
    environment:
      - REDIS_ARGS=--maxmemory 256mb --maxmemory-policy allkeys-lfu --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ecommerce
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    volumes:
      - pg-data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ecommerce
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_started

volumes:
  redis-data:
  pg-data:
```

---

## 16.2 Integration Tests with Testcontainers

```java
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private CartService cartService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Test
    void testCartOperations() {
        cartService.addToCart("user-1", "prod-100", 2);
        cartService.addToCart("user-1", "prod-200", 1);

        Map<Object, Object> cart = cartService.getFullCart("user-1");
        assertThat(cart).hasSize(2);
        assertThat(cart.get("prod-100")).isEqualTo(2);
    }

    @Test
    void testLeaderboard() {
        leaderboardService.recordInteraction(1L, 10.0);
        leaderboardService.recordInteraction(2L, 25.0);
        leaderboardService.recordInteraction(3L, 15.0);

        Set<Object> top = leaderboardService.getTopTrending(2);
        assertThat(top).hasSize(2);
        // Product 2 should be first (highest score)
    }

    @Test
    void testRateLimit() {
        RateLimiterService limiter = // ...

        // 5 requests allowed
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.isAllowed("test-user", 5, 60)).isTrue();
        }
        // 6th should be rejected
        assertThat(limiter.isAllowed("test-user", 5, 60)).isFalse();
    }
}
```

---

## 16.3 Feature → Redis Concept Map

| E-Commerce Feature | Redis Concept | Module |
|-------------------|---------------|--------|
| Product cache | String + @Cacheable | 4 |
| Shopping cart | Hash (HSET/HGET) | 2, 3 |
| User session | Spring Session | 5 |
| Order notifications | Pub/Sub | 6 |
| Order processing | Streams + Consumer Groups | 7 |
| Checkout lock | Redisson distributed lock | 8 |
| API throttling | Sorted Set + Lua | 9 |
| Inventory deduction | Lua script (atomic) | 10 |
| Store locator | Geospatial (GEOADD) | 11 |
| Product search | RediSearch | 12 |
| Trending products | Sorted Set (ZINCRBY) | 2 |
| Unique visitors | HyperLogLog | 2 |
| Feature flags | Bitmap | 2 |
| Recent activity | List (LPUSH/LTRIM) | 2 |
| Flash sale countdown | String + TTL + Keyspace notif | 6 |

---

## 16.4 Complete Limitations Summary

| Category | Limitation | Impact |
|----------|-----------|--------|
| **Memory** | All data in RAM | Expensive for large datasets |
| **Persistence** | Async = possible data loss | Not for sole source of truth |
| **Single-thread** | One slow command blocks all | Avoid O(N) on large collections |
| **Cluster** | Multi-key ops same slot only | Requires key design discipline |
| **Data modeling** | No joins, no relations | Denormalize everything |
| **TTL** | Per-key only, not per-field | Entire Hash expires together |
| **Value size** | 512MB max (practical: <1MB) | Split large objects |
| **Pub/Sub** | Fire-and-forget, no persistence | Use Streams for reliability |
| **Lua** | 5s timeout, blocks server | Keep scripts short |
| **Transactions** | No rollback on partial failure | Use Lua for safety |
| **Search** | Requires module, extra memory | Consider Elasticsearch for complex search |

---

## 16.5 When NOT to Use Redis

| Scenario | Better Alternative |
|----------|-------------------|
| Primary database (source of truth) | PostgreSQL, MySQL |
| Complex queries with JOINs | SQL database |
| Data > available RAM | PostgreSQL, Cassandra |
| Full-text search (complex) | Elasticsearch |
| Large file/blob storage | S3, MinIO |
| Strict ACID transactions | PostgreSQL |
| Complex event processing | Kafka |
| Time-series at scale | TimescaleDB, InfluxDB |

---

## 16.6 Running the Complete Project

```bash
# Start everything
docker-compose up -d

# Verify
curl http://localhost:8080/actuator/health

# Open RedisInsight
open http://localhost:8001

# Run tests
./mvnw test

# Load sample data
curl -X POST http://localhost:8080/api/admin/seed-data
```

---

## 🎉 Course Complete!

You now understand:
- All Redis data types and their Java/Spring APIs
- Caching patterns (cache-aside, write-through, stampede prevention)
- Session management across distributed instances
- Real-time messaging (Pub/Sub + Streams)
- Distributed locking (Redisson, Redlock)
- Rate limiting with Lua scripts
- Geospatial queries
- Full-text search with RediSearch
- Persistence, replication, and cluster modes
- Performance optimization and monitoring
- Every major limitation and when to choose alternatives
