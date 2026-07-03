# Module 14: Performance & Limits

## 🎯 Key Topics

- Memory management and eviction policies
- Big key anti-patterns
- Pipeline vs individual commands benchmarks
- KEYS vs SCAN
- Common pitfalls

---

## 14.1 Eviction Policies

```
# redis.conf
maxmemory 256mb
maxmemory-policy allkeys-lru
```

| Policy | Behavior | Use When |
|--------|----------|----------|
| noeviction | Return error on write when full | Data must never be lost |
| allkeys-lru | Evict least recently used | General cache |
| allkeys-lfu | Evict least frequently used | Hot/cold data split |
| volatile-lru | LRU only on keys with TTL | Mix of cache + permanent data |
| volatile-ttl | Evict shortest TTL first | Prioritize longer-lived data |
| allkeys-random | Random eviction | Uniform access patterns |

**Recommendation**: `allkeys-lfu` for caches, `noeviction` for session/state stores.

---

## 14.2 Big Key Problems

| Problem | Impact | Solution |
|---------|--------|----------|
| Hash with 1M fields | Slow HGETALL, blocks event loop | Split into smaller hashes |
| List with 10M items | LRANGE blocks | Paginate, use multiple lists |
| 10MB string value | Network/memory spike | Compress or split |
| Sorted Set with 5M members | Slow ZRANGEBYSCORE | Partition by time/category |

Detection:
```bash
# Find big keys
redis-cli --bigkeys

# Memory usage of specific key
redis-cli MEMORY USAGE "my:big:key"
```

---

## 14.3 KEYS vs SCAN

```java
// ❌ NEVER use KEYS in production (blocks entire Redis)
// redis.keys("product:*");  // O(N) - scans ALL keys

// ✅ Use SCAN (cursor-based, non-blocking)
public Set<String> findKeysByPattern(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions()
        .match(pattern).count(100).build();

    try (Cursor<String> cursor = redis.scan(options)) {
        cursor.forEachRemaining(keys::add);
    }
    return keys;
}
```

---

## 14.4 Performance Tips

| Tip | Impact |
|-----|--------|
| Use Pipeline for batch ops | 10-100x faster than individual calls |
| Keep values < 1KB | Avoids network bottleneck |
| Use short key names in high-throughput | Saves memory (millions of keys) |
| Set TTL on everything | Prevents memory leak |
| Use UNLINK instead of DEL for big keys | Non-blocking delete |
| Avoid Lua scripts > 100ms | Blocks entire Redis |
| Use connection pooling | Avoids TCP handshake overhead |
| Prefer MGET over multiple GETs | Single round trip |

---

## 14.5 Memory Estimation

| Data Type | Memory per entry (approx) |
|-----------|--------------------------|
| String (small) | ~90 bytes overhead + value size |
| Hash (ziplist, <128 fields) | ~60 bytes + field sizes |
| Hash (hashtable) | ~100 bytes per field |
| Set (intset, <512 ints) | 16 bytes per member |
| Set (hashtable) | ~80 bytes per member |
| Sorted Set (ziplist) | ~30 bytes per member |
| Sorted Set (skiplist) | ~120 bytes per member |
| Stream entry | ~100 bytes + field sizes |

---

## ⏭️ Next: [Module 15: Monitoring & Production](15_Monitoring_Production.md)
