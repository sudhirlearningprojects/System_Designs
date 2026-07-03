# Module 8: Distributed Locks

## 🎯 Learning Objectives

- Implement distributed locks with Redisson
- Understand Redlock algorithm
- Use locks for checkout, inventory deduction
- Handle lock failures and timeouts

---

## 8.1 Why Distributed Locks?

```
Problem: Two users buy the last item simultaneously

Thread A: READ stock=1 → CHECK stock>=1 → DEDUCT stock=0 ✅
Thread B: READ stock=1 → CHECK stock>=1 → DEDUCT stock=-1 ❌ OVERSOLD!

Solution: Lock the resource during check+deduct
```

---

## 8.2 Redisson Configuration

```java
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setConnectionMinimumIdleSize(4)
            .setConnectionPoolSize(16)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);
        return Redisson.create(config);
    }
}
```

---

## 8.3 Lock for Checkout

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final RedissonClient redisson;
    private final InventoryRepository inventoryRepo;
    private final OrderRepository orderRepo;

    public Order checkout(String userId, Long productId, int quantity) {
        String lockKey = "lock:checkout:" + productId;
        RLock lock = redisson.getLock(lockKey);

        try {
            // Wait 5s to acquire, auto-release after 10s
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new RuntimeException("Product is being purchased, try again");
            }

            // Critical section - only one thread executes this
            int stock = inventoryRepo.getStock(productId);
            if (stock < quantity) {
                throw new InsufficientStockException(productId, stock, quantity);
            }

            inventoryRepo.deductStock(productId, quantity);
            Order order = orderRepo.createOrder(userId, productId, quantity);

            log.info("Checkout successful: order={}", order.getId());
            return order;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 8.4 Fair Lock & ReadWrite Lock

```java
@Service
@RequiredArgsConstructor
public class AdvancedLockService {

    private final RedissonClient redisson;

    // Fair lock - FIFO ordering (prevents starvation)
    public void withFairLock(String resource, Runnable action) {
        RLock lock = redisson.getFairLock("lock:fair:" + resource);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    // ReadWrite lock - multiple readers, single writer
    public <T> T readProduct(Long productId, Supplier<T> reader) {
        RReadWriteLock rwLock = redisson.getReadWriteLock("lock:rw:product:" + productId);
        RLock readLock = rwLock.readLock();
        readLock.lock(5, TimeUnit.SECONDS);
        try {
            return reader.get();
        } finally {
            readLock.unlock();
        }
    }

    public void updateProduct(Long productId, Runnable writer) {
        RReadWriteLock rwLock = redisson.getReadWriteLock("lock:rw:product:" + productId);
        RLock writeLock = rwLock.writeLock();
        writeLock.lock(10, TimeUnit.SECONDS);
        try {
            writer.run();
        } finally {
            writeLock.unlock();
        }
    }
}
```

---

## 8.5 Limitations

| Issue | Detail | Mitigation |
|-------|--------|------------|
| Clock drift | Redlock assumes synchronized clocks | Use fencing tokens |
| Network partition | Lock may appear held but Redis unreachable | Set short lease times |
| GC pauses | Java GC can pause past lock expiry | Use longer lease + watchdog |
| Single Redis failure | Lock lost if master dies before replication | Use Redlock (3+ nodes) |
| Performance | Lock contention = serial execution | Lock fine-grained resources |

**Redisson Watchdog**: Auto-extends lock if thread is still running (default 30s renewal).

---

## ⏭️ Next: [Module 9: Rate Limiting](09_Rate_Limiting.md)
