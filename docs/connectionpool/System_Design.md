# Connection Pooling System - System Design

## 1. Problem Statement

Creating a new database/network connection for every request is expensive:
- TCP handshake: ~1.5 RTT (round-trip times)
- TLS handshake: ~2 additional RTTs
- Authentication: ~1 RTT
- Total: 5-50ms per connection setup

At 10K requests/sec, this means 10K connections created/destroyed per second — overwhelming the database and wasting resources.

**Solution**: Maintain a pool of pre-established, reusable connections.

---

## 2. Requirements

### Functional Requirements
1. Acquire a connection from the pool (with timeout)
2. Release a connection back to the pool
3. Create new connections on demand (up to max pool size)
4. Validate connections before handing out (detect stale/broken)
5. Evict idle connections after configurable timeout
6. Support connection lifecycle hooks (on-create, on-acquire, on-release, on-destroy)
7. Graceful shutdown (drain all connections)

### Non-Functional Requirements
1. **Thread-safe**: Support high concurrency without data races
2. **Low latency**: Acquire connection in <1ms (from pool)
3. **Leak detection**: Detect connections not returned within timeout
4. **Health monitoring**: Periodic validation of idle connections
5. **Metrics**: Pool utilization, wait times, connection counts
6. **Fairness**: FIFO ordering for waiting threads
7. **Bounded**: Never exceed max pool size

---

## 3. High-Level Design

```
┌─────────────────────────────────────────────────────────────────┐
│                        APPLICATION                                │
│                                                                   │
│   Thread 1 ──┐                                                   │
│   Thread 2 ──┤                                                   │
│   Thread 3 ──┼──► ConnectionPool.acquire() ──► PooledConnection  │
│   Thread N ──┘         │                              │          │
│                        │                              │          │
│              ┌─────────▼──────────┐          ┌────────▼───────┐  │
│              │   WAIT QUEUE       │          │  ACTIVE SET    │  │
│              │   (bounded, FIFO)  │          │  (in-use)      │  │
│              └────────────────────┘          └────────┬───────┘  │
│                                                       │          │
│              ┌────────────────────┐                    │          │
│              │   IDLE QUEUE       │◄───── release() ──┘          │
│              │   (available)      │                               │
│              └─────────┬──────────┘                               │
│                        │                                          │
│              ┌─────────▼──────────┐                               │
│              │  CONNECTION        │                               │
│              │  FACTORY           │──► Creates real connections   │
│              └────────────────────┘                               │
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Health Checker   │  │ Idle Evictor     │  │ Leak Detector │  │
│  │ (periodic)       │  │ (scheduled)      │  │ (timeout)     │  │
│  └──────────────────┘  └──────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Core Components

### 4.1 PooledConnection (Wrapper)

Wraps the real connection and tracks metadata.

```
PooledConnection
├── realConnection: Connection       // Actual DB/network connection
├── state: IDLE | IN_USE | EVICTED | CLOSED
├── createdAt: Instant               // When connection was created
├── lastUsedAt: Instant              // Last time it was acquired
├── lastValidatedAt: Instant         // Last health check
├── borrowCount: long                // Times this connection was used
├── currentBorrowDuration: Duration  // How long current user has held it
└── threadName: String               // Which thread is using it (leak detection)
```

### 4.2 Connection Pool

The main pool managing connection lifecycle.

```
ConnectionPool
├── idleConnections: BlockingDeque<PooledConnection>   // Available connections
├── activeConnections: ConcurrentHashMap<Connection, PooledConnection>  // In-use
├── totalConnections: AtomicInteger                    // Total count
├── waitingThreads: AtomicInteger                     // Threads waiting
├── config: PoolConfig                                // Configuration
├── factory: ConnectionFactory                        // Creates connections
├── healthChecker: ScheduledExecutorService           // Validates idle
├── idleEvictor: ScheduledExecutorService             // Removes stale
└── leakDetector: ScheduledExecutorService            // Finds leaks
```

### 4.3 Pool Configuration

```
PoolConfig
├── minIdle: int = 5                    // Minimum idle connections maintained
├── maxIdle: int = 10                   // Maximum idle connections
├── maxTotal: int = 20                  // Maximum total connections (idle + active)
├── maxWaitMillis: long = 30000         // Max time to wait for connection (30s)
├── minEvictableIdleTime: Duration = 10min  // Idle time before eligible for eviction
├── timeBetweenEvictionRuns: Duration = 5min // How often evictor runs
├── validationInterval: Duration = 30s  // How often to validate idle connections
├── validationQuery: String = "SELECT 1" // Query to validate connection
├── testOnBorrow: boolean = true        // Validate before handing out
├── testOnReturn: boolean = false       // Validate on return
├── testWhileIdle: boolean = true       // Validate idle connections
├── connectionTimeout: Duration = 5s    // Timeout for creating new connection
├── leakDetectionThreshold: Duration = 60s // Alert if held longer than this
├── maxLifetime: Duration = 30min       // Max age of a connection
└── keepaliveTime: Duration = 2min      // Send keepalive to prevent server timeout
```

---

## 5. Detailed Design

### 5.1 Connection Acquisition Flow

```
acquire(timeout)
       │
       ▼
┌─────────────────────┐
│ Try get from idle   │──── Got one? ──► Validate ──► Return to caller
│ queue (non-blocking)│                      │
└─────────┬───────────┘                      │ Invalid?
          │ Empty                             ▼
          ▼                            Destroy & retry
┌─────────────────────┐
│ Can create new?     │──── Yes ──► Create new connection
│ (total < maxTotal)  │                    │
└─────────┬───────────┘                    ▼
          │ No                       Add to active set
          ▼                          Return to caller
┌─────────────────────┐
│ Wait on queue       │──── Timeout? ──► Throw TimeoutException
│ (with timeout)      │
│                     │──── Signaled? ──► Got connection ──► Validate ──► Return
└─────────────────────┘
```

### 5.2 Connection Release Flow

```
release(connection)
       │
       ▼
┌─────────────────────┐
│ Remove from active  │
│ set                 │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Connection valid?   │──── No ──► Destroy connection
│ (not broken, not    │            Replenish if below minIdle
│  past maxLifetime)  │
└─────────┬───────────┘
          │ Yes
          ▼
┌─────────────────────┐
│ Idle queue full?    │──── Yes ──► Destroy connection
│ (idle >= maxIdle)   │
└─────────┬───────────┘
          │ No
          ▼
┌─────────────────────┐
│ Waiters present?    │──── Yes ──► Hand directly to waiter (bypass idle queue)
└─────────┬───────────┘
          │ No
          ▼
┌─────────────────────┐
│ Add to idle queue   │
│ Update lastUsedAt   │
└─────────────────────┘
```

### 5.3 Health Check (Idle Validation)

```
Every validationInterval:
  for each connection in idleQueue:
    if (now - lastValidatedAt > validationInterval):
      if (!validate(connection)):
        destroy(connection)
      else:
        connection.lastValidatedAt = now

  // Replenish if below minimum
  while (totalConnections < minIdle):
    createAndAddToIdle()
```

### 5.4 Idle Eviction

```
Every timeBetweenEvictionRuns:
  for each connection in idleQueue:
    if (idleConnections.size() > minIdle):
      if (now - lastUsedAt > minEvictableIdleTime):
        destroy(connection)
    
    if (now - createdAt > maxLifetime):
      destroy(connection)  // Prevent stale server-side state
```

### 5.5 Leak Detection

```
Every 30 seconds:
  for each connection in activeSet:
    borrowDuration = now - acquiredAt
    if (borrowDuration > leakDetectionThreshold):
      log.warn("Potential connection leak! Held by {} for {}",
               connection.threadName, borrowDuration)
      // Optionally: force-close and remove
```

### 5.6 Keepalive

```
Every keepaliveTime:
  for each connection in idleQueue:
    if (now - lastUsedAt > keepaliveTime):
      send lightweight query (SELECT 1)
      // Prevents server-side idle timeout (e.g., MySQL wait_timeout)
```

---

## 6. Thread Safety Design

### Synchronization Strategy

```
┌─────────────────────────────────────────────────────────┐
│                LOCK-FREE WHERE POSSIBLE                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  idleQueue: ConcurrentLinkedDeque + Semaphore            │
│    - Semaphore tracks available permits                   │
│    - tryAcquire(timeout) for bounded waiting             │
│                                                          │
│  activeSet: ConcurrentHashMap                            │
│    - Lock-free reads and writes                          │
│                                                          │
│  totalConnections: AtomicInteger                         │
│    - CAS for increment/decrement                         │
│                                                          │
│  Connection creation: synchronized(createLock)           │
│    - Prevent thundering herd on empty pool               │
│    - Double-check: if (total < max) then create          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Semaphore-Based Approach (HikariCP-style)

```java
// Simplified acquire logic
public Connection acquire(long timeout, TimeUnit unit) throws TimeoutException {
    // 1. Try to get permit (represents available connection slot)
    if (!semaphore.tryAcquire(timeout, unit)) {
        throw new TimeoutException("Connection pool exhausted");
    }
    
    // 2. Try to get idle connection
    PooledConnection conn = idleQueue.pollFirst();
    if (conn != null && isValid(conn)) {
        markActive(conn);
        return conn.getProxy();
    }
    
    // 3. Create new if under limit
    if (totalConnections.get() < config.maxTotal) {
        conn = createConnection();
        markActive(conn);
        return conn.getProxy();
    }
    
    // 4. Wait for returned connection
    conn = idleQueue.pollFirst(remainingTimeout, unit);
    if (conn == null) {
        semaphore.release();
        throw new TimeoutException("Timed out waiting for connection");
    }
    markActive(conn);
    return conn.getProxy();
}
```

---

## 7. Connection Proxy Pattern

Return a proxy instead of the real connection — intercepts close() to return to pool.

```
┌─────────────────────────────────────────┐
│         ConnectionProxy                  │
│                                          │
│  close() ──► pool.release(this)         │
│  isClosed() ──► check proxy state       │
│  prepareStatement() ──► delegate        │
│  createStatement() ──► delegate         │
│  ...all methods ──► delegate to real    │
│                                          │
│  Wraps: realConnection                   │
└─────────────────────────────────────────┘
```

This ensures:
- Application code calls `connection.close()` as normal
- Connection is returned to pool instead of actually closed
- Prevents accidental use after return (proxy throws exception)

---

## 8. Metrics & Monitoring

### Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `pool.connections.active` | Gauge | Currently borrowed connections |
| `pool.connections.idle` | Gauge | Available idle connections |
| `pool.connections.total` | Gauge | Total connections (active + idle) |
| `pool.connections.max` | Gauge | Maximum pool size |
| `pool.connections.pending` | Gauge | Threads waiting for connection |
| `pool.acquire.duration` | Histogram | Time to acquire connection |
| `pool.usage.duration` | Histogram | How long connections are held |
| `pool.connections.created` | Counter | Total connections created |
| `pool.connections.destroyed` | Counter | Total connections destroyed |
| `pool.connections.timeout` | Counter | Acquisition timeouts |
| `pool.connections.leaked` | Counter | Detected leaks |
| `pool.validation.failures` | Counter | Failed health checks |

### Alerting Rules

```
# Pool exhaustion warning
pool.connections.active / pool.connections.max > 0.8 for 5m → WARNING

# Pool exhausted
pool.connections.pending > 0 for 1m → CRITICAL

# Connection leaks
pool.connections.leaked > 0 → WARNING

# High acquisition latency
pool.acquire.duration.p99 > 100ms → WARNING

# Frequent connection creation (pool thrashing)
rate(pool.connections.created[5m]) > 10 → WARNING
```

---

## 9. Comparison with Production Pools

| Feature | HikariCP | Apache DBCP2 | c3p0 | Our Design |
|---------|----------|-------------|------|------------|
| Acquire strategy | Semaphore + CAS | Synchronized | Synchronized | Semaphore + CAS |
| Idle validation | Background | On borrow/return | Background | Both |
| Leak detection | ✅ | ✅ | ❌ | ✅ |
| Connection proxy | ✅ (FastList) | ✅ | ✅ | ✅ |
| Metrics | Micrometer | JMX | JMX | Micrometer |
| Eviction | Scheduled | Scheduled | Scheduled | Scheduled |
| Fairness | FIFO | FIFO/LIFO | FIFO | FIFO |
| Performance | Fastest | Good | Slow | Fast |

### Why HikariCP is Fast

1. **ConcurrentBag**: Custom lock-free collection (thread-local + steal)
2. **FastList**: ArrayList without range checks
3. **No synchronized blocks**: Uses CAS operations
4. **Connection proxy via Javassist**: Faster than JDK Proxy
5. **Minimal bytecode**: Tiny codebase (~130KB)

---

## 10. Pool Sizing Formula

### Optimal Pool Size

```
Pool Size = (Core Count * 2) + Effective Spindle Count

For SSD:
Pool Size = Core Count * 2 + 1

Example: 4-core server with SSD
Pool Size = 4 * 2 + 1 = 9
```

### Why Smaller is Better

```
Scenario: 10,000 concurrent users, 4-core database server

❌ Pool size = 10,000 (one per user)
   - 10,000 threads context-switching on 4 cores
   - Massive memory overhead
   - Database overwhelmed

✅ Pool size = 9
   - 4 cores can only execute 4-8 queries simultaneously
   - Remaining connections just wait for I/O
   - Database runs efficiently
   - Application threads wait on pool (fast, in-memory)
```

### Connection Pool vs Thread Pool Relationship

```
Application Thread Pool: 200 threads
Connection Pool: 10 connections

200 threads share 10 connections
- At any moment, max 10 threads are doing DB work
- Other 190 threads are doing CPU work or waiting
- This is optimal if DB queries take ~5% of request time
```

---

## 11. Advanced Patterns

### 11.1 Multi-Pool (Read/Write Splitting)

```
┌─────────────────────────────────────────┐
│           Connection Manager             │
├──────────────────┬──────────────────────┤
│   Write Pool     │     Read Pool        │
│   (Primary)      │     (Replicas)       │
│   maxSize: 5     │     maxSize: 20      │
│                  │                       │
│   ┌──────────┐  │  ┌──────────────────┐│
│   │ Primary  │  │  │ Replica 1        ││
│   │ DB       │  │  │ Replica 2        ││
│   └──────────┘  │  │ Replica 3        ││
│                  │  └──────────────────┘│
└──────────────────┴──────────────────────┘
```

### 11.2 Connection Affinity (Prepared Statement Cache)

```
Thread A always gets Connection 1 (if available)
  → Prepared statements cached on that connection are reused
  → Avoids re-parsing SQL on database side

Implementation: ThreadLocal<PooledConnection> + fallback to pool
```

### 11.3 Warm-Up Strategy

```
On pool initialization:
  1. Create minIdle connections in parallel
  2. Execute validation query on each (warms TCP, TLS, auth)
  3. For databases: execute common queries to warm plan cache
  4. Mark pool as ready only after warm-up completes
```

### 11.4 Circuit Breaker Integration

```
If connection creation fails N times in M seconds:
  → Open circuit breaker
  → Fail fast (don't wait for timeout)
  → Periodically try to create connection (half-open)
  → Resume normal operation when connection succeeds
```

---

## 12. Scale Calculations

### Assumptions
- Application: 50K requests/sec
- Average DB query time: 5ms
- Pool size: 20 connections

### Throughput

```
Max queries/sec per connection = 1000ms / 5ms = 200 queries/sec
Total pool throughput = 20 connections × 200 queries/sec = 4,000 queries/sec

For 50K requests/sec with 10% hitting DB:
Required = 5,000 queries/sec
Pool size needed = 5,000 / 200 = 25 connections
```

### Memory Overhead

```
Per connection:
- TCP socket buffer: ~128KB (send + receive)
- TLS state: ~50KB
- Connection metadata: ~1KB
- Prepared statement cache: ~100KB
Total per connection: ~280KB

Pool of 25 connections: 25 × 280KB = 7MB (negligible)
```

### Latency Impact

```
Without pool:
  Connection setup: 5ms (TCP) + 10ms (TLS) + 2ms (auth) = 17ms
  Query: 5ms
  Total: 22ms

With pool:
  Acquire from pool: <0.1ms
  Query: 5ms
  Return to pool: <0.01ms
  Total: ~5.1ms

Improvement: 77% latency reduction
```

---

## 13. Configuration Recommendations

### Web Application (Spring Boot)

```yaml
# HikariCP configuration
spring:
  datasource:
    hikari:
      pool-name: PaymentPool
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000      # 30s max wait
      idle-timeout: 600000           # 10min idle before eviction
      max-lifetime: 1800000          # 30min max connection age
      leak-detection-threshold: 60000 # 60s leak warning
      validation-timeout: 5000       # 5s validation timeout
      keepalive-time: 120000         # 2min keepalive
```

### High-Throughput Service

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 5000       # Fail fast
      idle-timeout: 300000           # 5min
      max-lifetime: 900000           # 15min (shorter for load balancing)
```

### Microservice (Low Traffic)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2
      maximum-pool-size: 5
      connection-timeout: 10000
      idle-timeout: 120000           # 2min (save resources)
      max-lifetime: 1800000
```
