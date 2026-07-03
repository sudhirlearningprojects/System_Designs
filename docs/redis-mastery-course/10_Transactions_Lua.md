# Module 10: Transactions & Lua Scripting

## 🎯 Learning Objectives

- Redis MULTI/EXEC transactions
- Optimistic locking with WATCH
- Lua scripting for complex atomic operations
- When to use transactions vs Lua vs locks

---

## 10.1 MULTI/EXEC Transactions

```java
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final StringRedisTemplate redis;

    /**
     * Transfer credits between users atomically.
     * Either both operations succeed or neither does.
     */
    public void transferCredits(String from, String to, int amount) {
        redis.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi(); // START TRANSACTION

                operations.opsForValue().decrement("credits:" + from, amount);
                operations.opsForValue().increment("credits:" + to, amount);

                return operations.exec(); // COMMIT (atomic)
            }
        });
    }
}
```

**Limitation**: MULTI/EXEC doesn't support conditional logic. You can't read a value inside a transaction and branch based on it.

---

## 10.2 Optimistic Locking with WATCH

```java
/**
 * WATCH implements optimistic locking:
 * - Watch a key
 * - Read its value
 * - Start MULTI
 * - If key changed before EXEC → transaction FAILS (returns null)
 * - Retry
 */
public boolean deductStockOptimistic(Long productId, int quantity) {
    String key = "product:stock:" + productId;

    // Retry loop for optimistic locking
    for (int attempt = 0; attempt < 5; attempt++) {
        List<Object> result = redis.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.watch(key);

                String currentStr = (String) operations.opsForValue().get(key);
                int current = Integer.parseInt(currentStr);

                if (current < quantity) {
                    operations.unwatch();
                    return null; // Not enough stock
                }

                operations.multi();
                operations.opsForValue().set(key, String.valueOf(current - quantity));
                return operations.exec(); // Returns null if WATCH detected change
            }
        });

        if (result != null) {
            return true; // Success
        }
        // Key was modified by another thread, retry
    }
    return false; // Failed after retries
}
```

---

## 10.3 Lua Scripting (Recommended Approach)

Lua scripts execute atomically on the server — no race conditions possible.

```java
@Service
@RequiredArgsConstructor
public class LuaScriptService {

    private final StringRedisTemplate redis;

    /**
     * Atomic inventory deduction with validation.
     * This is BETTER than WATCH for most use cases.
     */
    public boolean deductInventory(Long productId, int quantity) {
        String script = """
            local key = KEYS[1]
            local qty = tonumber(ARGV[1])
            local current = tonumber(redis.call('GET', key) or '0')
            if current >= qty then
                redis.call('DECRBY', key, qty)
                return 1
            end
            return 0
        """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        Long result = redis.execute(
            redisScript,
            List.of("product:stock:" + productId),
            String.valueOf(quantity)
        );

        return result != null && result == 1L;
    }

    /**
     * Atomic: check stock + deduct + record sale (multiple operations)
     */
    public String atomicPurchase(Long productId, String userId, int quantity) {
        String script = """
            local stockKey = KEYS[1]
            local salesKey = KEYS[2]
            local qty = tonumber(ARGV[1])
            local userId = ARGV[2]
            local timestamp = ARGV[3]
            
            local stock = tonumber(redis.call('GET', stockKey) or '0')
            if stock < qty then
                return 'INSUFFICIENT_STOCK'
            end
            
            redis.call('DECRBY', stockKey, qty)
            redis.call('HINCRBY', salesKey, 'total_sold', qty)
            redis.call('HINCRBY', salesKey, 'total_revenue', qty * 100)
            redis.call('ZADD', 'recent_purchases', timestamp, userId)
            
            return 'SUCCESS'
        """;

        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);

        return redis.execute(
            redisScript,
            List.of("product:stock:" + productId, "product:sales:" + productId),
            String.valueOf(quantity),
            userId,
            String.valueOf(System.currentTimeMillis())
        );
    }

    /**
     * Load Lua from file (for complex scripts)
     */
    @Bean
    public RedisScript<Long> rateLimitScript() {
        return RedisScript.of(
            new ClassPathResource("lua/rate_limit.lua"),
            Long.class
        );
    }
}
```

---

## 10.4 When to Use What

| Approach | Use When | Limitation |
|----------|----------|-----------|
| MULTI/EXEC | Simple batch writes (no reads needed) | No conditional logic inside |
| WATCH + MULTI | Optimistic locking, low contention | Fails under high contention |
| Lua scripts | Complex atomic logic, read+write | 5s timeout, blocks all Redis |
| Redisson locks | Long operations, external calls | Performance overhead |

---

## 10.5 Lua Script Limitations

- **5 second default timeout** — long scripts block the entire Redis server
- **No external calls** — can't call HTTP, DB, etc. from Lua
- **Limited libraries** — only cjson, cmsgpack, math, string, table
- **Cluster restriction** — all KEYS must hash to the same slot (use `{hashtag}`)
- **Debugging** — redis.log() only, no breakpoints

---

## ⏭️ Next: [Module 11: Geospatial Queries](11_Geospatial.md)
