# Module 1: Foundations & Setup

## 🎯 Learning Objectives

- Understand Redis architecture (single-threaded, in-memory, event loop)
- Set up Redis with Docker
- Configure Spring Boot with Spring Data Redis
- Understand Lettuce vs Jedis client differences
- Connect RedisInsight for visualization

---

## 1.1 Redis Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     REDIS SERVER                          │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │            Event Loop (Single Thread)              │   │
│  │                                                  │   │
│  │  Command Queue → Parse → Execute → Respond       │   │
│  │                                                  │   │
│  │  Why single-threaded?                            │   │
│  │  • No locks needed = blazing fast                │   │
│  │  • No context switching overhead                 │   │
│  │  • Predictable latency                           │   │
│  │  • Commands are atomic by default                │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐   │
│  │   Memory     │  │  Persistence│  │  Networking  │   │
│  │   (RAM)      │  │  (RDB/AOF)  │  │  (TCP/TLS)   │   │
│  │              │  │  Background │  │  I/O Threads │   │
│  │  All data    │  │  threads    │  │  (Redis 6+)  │   │
│  │  lives here  │  │             │  │              │   │
│  └──────────────┘  └─────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Key Properties

| Property | Detail |
|----------|--------|
| Storage | In-memory (RAM) |
| Threading | Single-threaded command execution |
| Speed | 100K-1M ops/sec (single instance) |
| Persistence | Optional (RDB snapshots / AOF log) |
| Data structures | Native (not just key-value) |
| Max memory | Limited by RAM (typically 25-100GB) |
| Max key size | 512 MB |
| Max value size | 512 MB |
| Max keys | 2^32 (~4.2 billion) |

### Limitations to Keep in Mind

1. **Memory-bound** — All data must fit in RAM
2. **Single-threaded execution** — One slow command blocks everything
3. **No built-in SQL** — Must model queries differently
4. **No native joins** — Denormalize or use application-level joins
5. **Eventual persistence** — Can lose data between snapshots
6. **Key expiration is lazy** — Expired keys may briefly consume memory

---

## 1.2 Docker Setup

```yaml
# docker-compose.yml

version: '3.8'

services:
  redis:
    image: redis:7.2-alpine
    container_name: redis-master
    ports:
      - "6379:6379"
    command: >
      redis-server
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
      --appendonly yes
      --appendfsync everysec
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 3

  redis-insight:
    image: redislabs/redisinsight:latest
    container_name: redis-insight
    ports:
      - "8001:8001"
    depends_on:
      - redis

  postgres:
    image: postgres:16-alpine
    container_name: postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ecommerce
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    volumes:
      - pg-data:/var/lib/postgresql/data

volumes:
  redis-data:
  pg-data:
```

```bash
# Start everything
docker-compose up -d

# Verify Redis
docker exec -it redis-master redis-cli ping
# Output: PONG

# Check Redis info
docker exec -it redis-master redis-cli INFO server
```

---

## 1.3 Spring Boot Project Setup

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>redis-ecommerce</artifactId>
    <version>1.0.0</version>
    <name>Redis E-Commerce Platform</name>

    <properties>
        <java.version>21</java.version>
        <redisson.version>3.27.0</redisson.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Session Redis -->
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>

        <!-- Redisson (Distributed Locks) -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
        </dependency>

        <!-- Caffeine (L1 Cache) -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Jackson for JSON serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

---

## 1.4 Redis Configuration

```yaml
# src/main/resources/application.yml

spring:
  application:
    name: redis-ecommerce

  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 2000ms
        shutdown-timeout: 200ms

  # PostgreSQL
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce
    username: app
    password: secret

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

  # Session
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: ecom:sessions

  # Cache
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes default
      cache-null-values: false
```

```java
// src/main/java/com/example/redis/config/RedisConfig.java

package com.example.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer - always String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer - JSON for readability and debugging
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(mapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

---

## 1.5 Lettuce vs Jedis

| Feature | Lettuce (Default) | Jedis |
|---------|------------------|-------|
| Threading | Non-blocking, async | Blocking, sync |
| Connection pool | Single shared connection | Pool required |
| Reactive support | ✅ Full | ❌ No |
| Cluster support | ✅ Native | ✅ Native |
| Thread safety | ✅ Thread-safe | ❌ Not thread-safe |
| Pipeline | ✅ | ✅ |
| Dependency | Netty-based | Lightweight |
| **Recommendation** | **Use for Spring Boot** | Legacy apps |

Spring Boot uses Lettuce by default. Stick with it unless you have specific reasons for Jedis.

---

## 1.6 Verify Connection

```java
// src/main/java/com/example/redis/config/RedisHealthCheck.java

package com.example.redis.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthCheck {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void verifyRedisConnection() {
        try {
            String result = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            log.info("✅ Redis connected successfully: {}", result);

            // Store a test value
            redisTemplate.opsForValue().set("app:startup", "OK");
            log.info("✅ Redis read/write verified");
        } catch (Exception e) {
            log.error("❌ Redis connection failed: {}", e.getMessage());
            throw new RuntimeException("Cannot start without Redis", e);
        }
    }
}
```

---

## 1.7 Key Naming Conventions

Establish consistent key naming from the start:

```
Pattern: {service}:{entity}:{id}:{field}

Examples:
  product:cache:12345           → Cached product
  cart:user:u-100               → User's shopping cart
  session:abc123                → Session data
  rate:api:192.168.1.1          → Rate limit counter
  lock:order:ord-500            → Distributed lock
  geo:stores                    → Geospatial index
  stream:orders                 → Order event stream
  leaderboard:trending          → Product trending scores
  search:idx:products           → Search index
  queue:email                   → Email job queue
```

```java
// src/main/java/com/example/redis/common/RedisKeyBuilder.java

package com.example.redis.common;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {}

    // Product keys
    public static String productCache(Long id) {
        return "product:cache:" + id;
    }

    public static String productStock(Long id) {
        return "product:stock:" + id;
    }

    // Cart keys
    public static String userCart(String userId) {
        return "cart:user:" + userId;
    }

    // Session keys
    public static String session(String sessionId) {
        return "session:" + sessionId;
    }

    // Rate limiting
    public static String rateLimit(String identifier) {
        return "rate:api:" + identifier;
    }

    // Distributed locks
    public static String orderLock(String orderId) {
        return "lock:order:" + orderId;
    }

    // Leaderboards
    public static String trendingProducts() {
        return "leaderboard:trending";
    }

    // Geospatial
    public static String storeLocations() {
        return "geo:stores";
    }

    // Streams
    public static String orderStream() {
        return "stream:orders";
    }
}
```

---

## 1.8 Running the Project

```bash
# Start Redis + Postgres
docker-compose up -d

# Run Spring Boot app
./mvnw spring-boot:run

# Verify
curl http://localhost:8080/actuator/health

# Open RedisInsight
open http://localhost:8001
# Add Redis database: host=redis, port=6379
```

---

## ⏭️ Next Module

Proceed to **[Module 2: Data Types Deep Dive](02_Data_Types.md)** to master every Redis data type with hands-on examples.
