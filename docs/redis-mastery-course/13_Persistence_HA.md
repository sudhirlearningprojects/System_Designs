# Module 13: Persistence & High Availability

## 🎯 Learning Objectives

- RDB vs AOF persistence trade-offs
- Redis Sentinel for automatic failover
- Redis Cluster for horizontal scaling
- Replication architecture
- Spring Boot configuration for each mode

---

## 13.1 Persistence Options

### RDB (Snapshots)

```
# redis.conf
save 900 1      # Snapshot if 1 key changed in 900 seconds
save 300 10     # Snapshot if 10 keys changed in 300 seconds
save 60 10000   # Snapshot if 10000 keys changed in 60 seconds

dbfilename dump.rdb
dir /data
```

| Pros | Cons |
|------|------|
| Compact binary file | Data loss between snapshots |
| Fast restart (load entire file) | Fork() can be slow on large datasets |
| Good for backups | Not suitable for zero-data-loss |

### AOF (Append-Only File)

```
# redis.conf
appendonly yes
appendfsync everysec   # fsync every second (recommended)
# appendfsync always   # fsync every write (slowest, safest)
# appendfsync no       # OS decides (fastest, risky)

auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

| Pros | Cons |
|------|------|
| At most 1 second data loss | Larger files than RDB |
| Append-only (no corruption) | Slower restart (replay log) |
| Human-readable | Write amplification |

### Recommended: Both

```
# Use both for best of both worlds
save 900 1
appendonly yes
appendfsync everysec
```

---

## 13.2 Replication

```
Master (read+write)
    ├── Replica 1 (read-only)
    └── Replica 2 (read-only)
```

```yaml
# Spring Boot: read from replicas, write to master
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes: sentinel1:26379,sentinel2:26379,sentinel3:26379
      lettuce:
        read-from: REPLICA_PREFERRED  # Read from replica when possible
```

---

## 13.3 Sentinel (Auto-Failover)

```yaml
# docker-compose-sentinel.yml
services:
  redis-master:
    image: redis:7.2-alpine
    ports: ["6379:6379"]

  redis-replica:
    image: redis:7.2-alpine
    command: redis-server --replicaof redis-master 6379

  sentinel1:
    image: redis:7.2-alpine
    command: >
      redis-sentinel /etc/sentinel.conf
    volumes:
      - ./sentinel.conf:/etc/sentinel.conf
```

```
# sentinel.conf
sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
```

Spring Boot config:
```java
@Configuration
public class SentinelConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinel = new RedisSentinelConfiguration()
            .master("mymaster")
            .sentinel("sentinel1", 26379)
            .sentinel("sentinel2", 26379)
            .sentinel("sentinel3", 26379);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build();

        return new LettuceConnectionFactory(sentinel, clientConfig);
    }
}
```

---

## 13.4 Redis Cluster (Horizontal Scaling)

```
┌─────────────────────────────────────────────┐
│              Redis Cluster                    │
│  Slot 0-5460     Slot 5461-10922  Slot 10923-16383  │
│  ┌──────────┐   ┌──────────┐    ┌──────────┐│
│  │ Master 1 │   │ Master 2 │    │ Master 3 ││
│  │ Replica  │   │ Replica  │    │ Replica  ││
│  └──────────┘   └──────────┘    └──────────┘│
└─────────────────────────────────────────────┘
```

Spring Boot config:
```yaml
spring:
  data:
    redis:
      cluster:
        nodes: node1:6379,node2:6379,node3:6379,node4:6379,node5:6379,node6:6379
        max-redirects: 3
      lettuce:
        cluster:
          refresh:
            adaptive: true
            period: 30s
```

---

## 13.5 Cluster Limitations

| Limitation | Detail | Workaround |
|-----------|--------|-----------|
| Multi-key ops | MGET/MSET only on same slot | Use hash tags: `{user:1}:cart`, `{user:1}:session` |
| Lua scripts | All keys must be same slot | Hash tag in key names |
| Transactions | MULTI/EXEC same slot only | Design keys accordingly |
| Pub/Sub | Messages broadcast to ALL nodes | Use Streams for efficiency |
| Max nodes | ~1000 recommended | Shard at application level beyond |
| Rebalancing | Moving slots = brief unavailability | Plan during low traffic |

---

## 13.6 Decision Matrix

| Scenario | Setup |
|----------|-------|
| Dev/small app | Single Redis |
| Production, need HA | Sentinel (3 sentinels + master + replica) |
| Need >100GB or >100K ops/sec | Cluster (6+ nodes) |
| Read-heavy | Master + multiple read replicas |
| Multi-region | Cluster per region + application routing |

---

## ⏭️ Next: [Module 14: Performance & Limits](14_Performance_Limits.md)
