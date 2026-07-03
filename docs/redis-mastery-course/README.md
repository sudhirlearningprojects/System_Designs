# 🔴 Redis Mastery Course - Complete Java & Spring Boot Project

Learn **every** Redis concept, usage pattern, and limitation by building a production-grade **Real-Time E-Commerce Platform** using Java 21, Spring Boot 3, and the Spring ecosystem.

---

## 🎯 What You'll Build

A complete e-commerce platform that uses Redis for:
- Session management & authentication
- Product catalog caching (multi-layer)
- Shopping cart (persistent & distributed)
- Real-time inventory with atomic operations
- Leaderboards & trending products
- Rate limiting & API throttling
- Pub/Sub for real-time notifications
- Redis Streams for event processing
- Full-text search with RediSearch
- Distributed locks for checkout
- Geospatial queries (nearby stores)
- Time-series data (analytics)
- Job queues with Redis

---

## 📚 Course Modules

| # | Module | Redis Concepts |
|---|--------|---------------|
| 1 | [Foundations & Setup](01_Foundations_Setup.md) | Architecture, Redis installation, Spring Data Redis config |
| 2 | [Data Types Deep Dive](02_Data_Types.md) | Strings, Hashes, Lists, Sets, Sorted Sets, Streams |
| 3 | [Spring Data Redis](03_Spring_Data_Redis.md) | RedisTemplate, Repositories, Serialization |
| 4 | [Caching with Spring Cache](04_Caching.md) | @Cacheable, @CacheEvict, TTL, multi-layer cache |
| 5 | [Session Management](05_Session_Management.md) | Spring Session Redis, token storage, SSO |
| 6 | [Pub/Sub & Messaging](06_PubSub_Messaging.md) | Publish/Subscribe, message listeners, patterns |
| 7 | [Redis Streams](07_Redis_Streams.md) | Event sourcing, consumer groups, acknowledgment |
| 8 | [Distributed Locks](08_Distributed_Locks.md) | Redisson, Redlock, optimistic/pessimistic locking |
| 9 | [Rate Limiting](09_Rate_Limiting.md) | Sliding window, token bucket, Lua scripts |
| 10 | [Transactions & Lua](10_Transactions_Lua.md) | MULTI/EXEC, WATCH, Lua scripting, atomicity |
| 11 | [Geospatial Queries](11_Geospatial.md) | GEOADD, GEORADIUS, nearby search |
| 12 | [Search & Indexing](12_Search_Indexing.md) | RediSearch, full-text, autocomplete |
| 13 | [Persistence & HA](13_Persistence_HA.md) | RDB, AOF, Sentinel, Cluster, replication |
| 14 | [Performance & Limits](14_Performance_Limits.md) | Memory management, eviction, big keys, pitfalls |
| 15 | [Monitoring & Production](15_Monitoring_Production.md) | INFO, SLOWLOG, RedisInsight, Spring Actuator |
| 16 | [Complete Project](16_Complete_Project.md) | Full source, Docker Compose, testing |

---

## 🏗️ Project Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   E-COMMERCE PLATFORM                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Auth Service │  │ Product      │  │ Order Service            │  │
│  │ (Sessions)   │  │ Service      │  │ (Distributed Lock)       │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                  │                     │                   │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────────┴───────────────┐  │
│  │ Cart Service │  │ Search       │  │ Notification Service     │  │
│  │ (Hash)       │  │ Service      │  │ (Pub/Sub + Streams)      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                  │                     │                   │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────────┴───────────────┐  │
│  │ Analytics    │  │ Geo Service  │  │ Rate Limiter             │  │
│  │ (Sorted Set) │  │ (Geospatial) │  │ (Lua Scripts)            │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                        REDIS CLUSTER                           │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────────┐ │  │
│  │  │ Master  │  │ Master  │  │ Master  │  │ Sentinel/       │ │  │
│  │  │ Shard 1 │  │ Shard 2 │  │ Shard 3 │  │ Cluster Mgr     │ │  │
│  │  │ + Replica│  │ + Replica│  │ + Replica│  │                 │ │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────────────┘ │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │               PostgreSQL (Source of Truth)                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.2+ |
| Redis Client | Lettuce (default) + Redisson |
| Spring Data | Spring Data Redis |
| Caching | Spring Cache + Caffeine (L1) |
| Sessions | Spring Session Redis |
| Search | RediSearch module |
| Locking | Redisson (Redlock) |
| Database | PostgreSQL 16 |
| Messaging | Redis Pub/Sub + Streams |
| Containerization | Docker + Docker Compose |
| Testing | Testcontainers + Embedded Redis |
| Monitoring | RedisInsight + Prometheus + Grafana |

---

## 📋 Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Basic Spring Boot knowledge
- SQL basics (PostgreSQL)
- Redis CLI familiarity (helpful, not required)

---

## 🚀 Quick Start

```bash
# Clone
git clone <repo>
cd redis-ecommerce-platform

# Start infrastructure
docker-compose up -d redis postgres redis-insight

# Run the application
./mvnw spring-boot:run

# Access
# App: http://localhost:8080
# RedisInsight: http://localhost:8001
# Swagger: http://localhost:8080/swagger-ui.html
```

---

## 📁 Project Structure

```
redis-ecommerce-platform/
├── src/main/java/com/example/redis/
│   ├── RedisEcommerceApplication.java
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── CacheConfig.java
│   │   ├── SessionConfig.java
│   │   ├── RedissonConfig.java
│   │   └── SecurityConfig.java
│   ├── auth/
│   │   ├── controller/
│   │   ├── service/
│   │   └── model/
│   ├── product/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── model/
│   ├── cart/
│   │   ├── controller/
│   │   ├── service/
│   │   └── model/
│   ├── order/
│   │   ├── controller/
│   │   ├── service/
│   │   └── model/
│   ├── search/
│   │   ├── controller/
│   │   └── service/
│   ├── notification/
│   │   ├── publisher/
│   │   ├── subscriber/
│   │   └── stream/
│   ├── analytics/
│   │   ├── controller/
│   │   ├── service/
│   │   └── leaderboard/
│   ├── geo/
│   │   ├── controller/
│   │   └── service/
│   ├── ratelimit/
│   │   ├── interceptor/
│   │   ├── service/
│   │   └── lua/
│   └── common/
│       ├── lock/
│       ├── util/
│       └── exception/
├── src/main/resources/
│   ├── application.yml
│   ├── application-redis-cluster.yml
│   ├── lua/
│   │   ├── rate_limit.lua
│   │   ├── inventory_deduct.lua
│   │   └── leaderboard_update.lua
│   └── schema.sql
├── src/test/java/
│   ├── integration/
│   └── unit/
├── docker-compose.yml
├── docker-compose-cluster.yml
├── pom.xml
└── README.md
```

---

## 📊 Redis Concepts Coverage Map

| Redis Concept | Module | Project Feature |
|---------------|--------|-----------------|
| SET/GET (Strings) | 2 | Session tokens, config flags |
| HSET/HGET (Hashes) | 2 | Shopping cart, user profiles |
| LPUSH/RPOP (Lists) | 2 | Recent activity, job queues |
| SADD/SMEMBERS (Sets) | 2 | Tags, unique visitors |
| ZADD/ZRANGE (Sorted Sets) | 2 | Leaderboards, trending, scheduling |
| XADD/XREAD (Streams) | 7 | Order events, audit trail |
| GEOADD/GEORADIUS | 11 | Store locator, delivery radius |
| PUBLISH/SUBSCRIBE | 6 | Real-time notifications |
| MULTI/EXEC | 10 | Atomic cart updates |
| Lua scripting | 10 | Rate limiting, inventory deduction |
| TTL/EXPIRE | 4 | Cache expiry, session timeout |
| SCAN/KEYS | 14 | Admin operations, key cleanup |
| Pipeline | 14 | Bulk operations performance |
| Pub/Sub patterns | 6 | Channel-based routing |
| Consumer groups | 7 | Competing consumers |
| Keyspace notifications | 6 | Expiry events, triggers |
| Memory policies | 14 | LRU, LFU, allkeys, volatile |
| RDB/AOF | 13 | Persistence trade-offs |
| Sentinel | 13 | Auto-failover |
| Cluster | 13 | Horizontal scaling |
| Redlock | 8 | Distributed mutex |
| HyperLogLog | 2 | Unique visitor count |
| Bitmaps | 2 | Feature flags, daily active users |

---

## ⏭️ Start

Begin with **[Module 1: Foundations & Setup](01_Foundations_Setup.md)** to set up the project and understand Redis architecture.
