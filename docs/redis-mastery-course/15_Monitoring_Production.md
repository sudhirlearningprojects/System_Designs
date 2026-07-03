# Module 15: Monitoring & Production

## 🎯 Key Topics

- Redis INFO command
- SLOWLOG for debugging
- Spring Boot Actuator + Redis metrics
- RedisInsight dashboard
- Production checklist

---

## 15.1 Essential Monitoring Commands

```bash
# Overall stats
redis-cli INFO

# Key sections
redis-cli INFO memory        # used_memory, fragmentation
redis-cli INFO stats         # ops/sec, hits/misses
redis-cli INFO replication   # master/replica status
redis-cli INFO clients       # connected clients

# Cache hit ratio
# hit_rate = keyspace_hits / (keyspace_hits + keyspace_misses)

# Slow queries (>10ms by default)
redis-cli SLOWLOG GET 10
redis-cli CONFIG SET slowlog-log-slower-than 5000  # 5ms threshold

# Monitor all commands in real-time (DEBUG ONLY - never in production)
redis-cli MONITOR

# Memory doctor
redis-cli MEMORY DOCTOR
```

---

## 15.2 Spring Actuator Integration

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    tags:
      application: redis-ecommerce
```

```java
// Custom Redis health indicator
@Component
public class RedisDetailedHealthIndicator extends AbstractHealthIndicator {

    private final StringRedisTemplate redis;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            Properties info = redis.getConnectionFactory()
                .getConnection().serverCommands().info();

            builder.up()
                .withDetail("version", info.getProperty("redis_version"))
                .withDetail("used_memory_human", info.getProperty("used_memory_human"))
                .withDetail("connected_clients", info.getProperty("connected_clients"))
                .withDetail("ops_per_sec", info.getProperty("instantaneous_ops_per_sec"));
        } catch (Exception e) {
            builder.down(e);
        }
    }
}
```

Key metrics to track:
- `redis.commands.duration` — latency per command type
- `cache.gets` / `cache.puts` — cache hit/miss ratio
- `redis.pool.active` — connection pool usage
- Memory usage vs maxmemory
- Connected clients vs max clients

---

## 15.3 Production Checklist

- [ ] `maxmemory` set (don't let Redis eat all RAM)
- [ ] Eviction policy configured (`allkeys-lfu` for cache)
- [ ] Persistence enabled (RDB + AOF)
- [ ] Sentinel or Cluster for HA
- [ ] `rename-command KEYS ""` (disable dangerous commands)
- [ ] `rename-command FLUSHALL ""` 
- [ ] TLS enabled for network encryption
- [ ] AUTH password set (`requirepass`)
- [ ] ACLs configured (Redis 6+)
- [ ] `timeout 300` (close idle connections)
- [ ] Monitoring/alerting on memory, ops/sec, latency
- [ ] Connection pool sized correctly
- [ ] TTL on all cache keys
- [ ] No KEYS command usage in application code
- [ ] Backup strategy for RDB files
- [ ] Network latency < 1ms between app and Redis

---

## ⏭️ Next: [Module 16: Complete Project](16_Complete_Project.md)
