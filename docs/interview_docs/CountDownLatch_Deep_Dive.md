# CountDownLatch

A synchronization aid that allows one or more threads to wait until a set of operations in other threads completes.

## How it Works

Internal counter initialized at creation. Once it hits **zero**, all waiting threads are released. **One-time use — cannot be reset.**

```
CountDownLatch(N)
      │
      ├── Thread 1 → countDown() → N-1
      ├── Thread 2 → countDown() → N-2
      └── Thread N → countDown() →  0  ──► awaiting threads released
```

## Core API

| Method | Description |
|--------|-------------|
| `countDown()` | Decrements counter by 1 |
| `await()` | Blocks until counter = 0 |
| `await(timeout, unit)` | Blocks with timeout |
| `getCount()` | Returns current count |

## Basic Example

```java
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        try {
            doWork();
        } finally {
            latch.countDown(); // always in finally to avoid deadlock
        }
    }).start();
}

latch.await(10, TimeUnit.SECONDS); // use timeout in production
System.out.println("All workers done!");
```

## Use Cases

**1. Wait for services to initialize**
```java
CountDownLatch ready = new CountDownLatch(3);
startDatabase(ready);   // countDown() when ready
startCache(ready);
startKafka(ready);
ready.await();          // block until all 3 are up
```

**2. Fan-out queries (e.g., distributed DB shards)**
```java
CountDownLatch latch = new CountDownLatch(shards.size());
List<Result> results = new CopyOnWriteArrayList<>();

shards.forEach(shard -> executor.submit(() -> {
    try { results.add(shard.query(sql)); }
    finally { latch.countDown(); }
}));

latch.await(5, TimeUnit.SECONDS);
aggregate(results);
```

**3. Simultaneous thread start (load testing)**
```java
CountDownLatch startGun = new CountDownLatch(1);
for (int i = 0; i < 100; i++) {
    new Thread(() -> { startGun.await(); runTask(); }).start();
}
startGun.countDown(); // release all 100 threads at once
```

## CountDownLatch vs CyclicBarrier vs Phaser

| | CountDownLatch | CyclicBarrier | Phaser |
|---|---|---|---|
| Reusable | ❌ | ✅ | ✅ |
| Who waits | One waits for many | All wait for each other | Flexible |
| Dynamic parties | ❌ | ❌ | ✅ |
| Use case | Init / fan-out | Iterative phases | Complex workflows |

## Gotchas

- Always call `countDown()` in a `finally` block — exceptions can cause deadlocks
- Prefer `await(timeout, unit)` over `await()` in production
- Use `CyclicBarrier` if you need the barrier to reset after each cycle
- Use `Phaser` for dynamic number of parties or multi-phase workflows
