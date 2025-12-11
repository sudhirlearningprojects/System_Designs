# Database Write Throughput Maxed Out: Solutions

## Problem Statement
Your DB write throughput has maxed out, but you still can't horizontally scale easily. What next?

## Scenario
```
Current State:
- Single PostgreSQL instance: 10K writes/sec (maxed out)
- CPU: 100%
- Disk I/O: Saturated
- Connection pool: Exhausted
- Can't shard easily (complex joins, transactions)

Need: 50K writes/sec without breaking existing architecture
```

---

## Why Can't You Scale Horizontally?

### Common Blockers

1. **Complex Joins Across Tables**
```sql
-- Query spans multiple tables
SELECT o.*, u.name, p.title, i.quantity
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN order_items i ON o.id = i.order_id
JOIN products p ON i.product_id = p.id
WHERE o.created_at > NOW() - INTERVAL '7 days';

-- Sharding breaks this query (data on different shards)
```

2. **Distributed Transactions**
```java
@Transactional
public void transferMoney(Long fromAccount, Long toAccount, BigDecimal amount) {
    // Both accounts must be updated atomically
    accountRepository.debit(fromAccount, amount);  // Shard 1
    accountRepository.credit(toAccount, amount);   // Shard 2
    // Can't guarantee ACID across shards
}
```

3. **Foreign Key Constraints**
```sql
-- Foreign keys don't work across shards
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT REFERENCES users(id), -- users table on different shard
    product_id BIGINT REFERENCES products(id) -- products table on different shard
);
```

4. **Sequential IDs**
```sql
-- Auto-increment IDs don't work across shards
CREATE TABLE orders (
    id SERIAL PRIMARY KEY -- Each shard generates conflicting IDs
);
```

---

## Solution 1: Vertical Scaling (Buy Time)

### Upgrade Hardware

```yaml
Current: db.r6g.xlarge
- 4 vCPUs
- 32 GB RAM
- 10K IOPS
- Cost: $400/month
- Writes: 10K/sec

Upgrade: db.r6g.8xlarge
- 32 vCPUs
- 256 GB RAM
- 80K IOPS
- Cost: $3,200/month
- Writes: 40K/sec (4x improvement)
```

### Optimize Instance Configuration

```sql
-- PostgreSQL tuning for write-heavy workload
-- postgresql.conf

-- Memory settings
shared_buffers = 64GB                    -- 25% of RAM
effective_cache_size = 192GB             -- 75% of RAM
work_mem = 256MB
maintenance_work_mem = 2GB

-- Write performance
wal_buffers = 16MB
checkpoint_timeout = 15min
max_wal_size = 4GB
checkpoint_completion_target = 0.9

-- Async commit (trade durability for speed)
synchronous_commit = off                 -- 2-3x faster writes
wal_writer_delay = 200ms

-- Connection pooling
max_connections = 500
```

**Pros**: Quick, no code changes
**Cons**: Expensive, limited ceiling (can't scale infinitely)

---

## Solution 2: Write-Optimized Database (Switch DB)

### Option A: Cassandra (Write-Optimized)

```java
@Service
public class CassandraWriteService {
    
    @Autowired
    private CassandraTemplate cassandraTemplate;
    
    // Cassandra handles 100K+ writes/sec easily
    public void writeEvent(Event event) {
        String cql = "INSERT INTO events (id, user_id, event_type, data, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        cassandraTemplate.getCqlOperations().execute(
            cql,
            event.getId(),
            event.getUserId(),
            event.getEventType(),
            event.getData(),
            Instant.now()
        );
    }
}
```

**Cassandra Schema**:
```sql
CREATE TABLE events (
    user_id BIGINT,
    timestamp TIMESTAMP,
    event_id UUID,
    event_type TEXT,
    data TEXT,
    PRIMARY KEY (user_id, timestamp, event_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Optimized for writes:
-- - No joins
-- - No foreign keys
-- - Append-only
-- - Distributed by partition key (user_id)
```

**Performance**:
- Writes: 100K-1M/sec per cluster
- Linear scalability (add nodes)
- No single point of failure

**Trade-offs**:
- No joins (denormalize data)
- Eventual consistency
- Limited query flexibility

---

### Option B: ScyllaDB (Cassandra-compatible, 10x faster)

```yaml
# Same API as Cassandra, but C++ implementation
Performance:
- Cassandra: 100K writes/sec
- ScyllaDB: 1M writes/sec (same hardware)
- Lower latency (p99: 5ms vs 50ms)
```

---

## Solution 3: Batch Writes (Reduce Write Amplification)

### Problem: Too Many Small Writes

```java
// ❌ BAD: 10,000 individual writes
for (int i = 0; i < 10000; i++) {
    jdbcTemplate.update("INSERT INTO events VALUES (?, ?)", id, data);
}
// Result: 10,000 round trips, 10,000 disk writes
```

### Solution: Batch Writes

```java
@Service
public class BatchWriteService {
    
    private final BlockingQueue<WriteRequest> writeBuffer = new LinkedBlockingQueue<>(100000);
    
    @PostConstruct
    public void startBatchProcessor() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        // Flush every 100ms or 1000 records
        executor.scheduleAtFixedRate(() -> {
            List<WriteRequest> batch = new ArrayList<>();
            writeBuffer.drainTo(batch, 1000);
            
            if (!batch.isEmpty()) {
                batchWrite(batch);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    // Queue write (non-blocking)
    public CompletableFuture<Void> writeAsync(WriteRequest request) {
        writeBuffer.offer(request);
        return CompletableFuture.completedFuture(null);
    }
    
    // Batch write (1000 records in single transaction)
    private void batchWrite(List<WriteRequest> batch) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO events (id, user_id, data, timestamp) VALUES (?, ?, ?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    WriteRequest req = batch.get(i);
                    ps.setString(1, req.getId());
                    ps.setLong(2, req.getUserId());
                    ps.setString(3, req.getData());
                    ps.setTimestamp(4, Timestamp.from(Instant.now()));
                }
                
                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            }
        );
    }
}
```

**Performance Improvement**:
- Before: 10K writes/sec (individual)
- After: 50K writes/sec (batched) - **5x improvement**

---

## Solution 4: Write-Behind Caching (Async Writes)

### Architecture

```
Write Path:
User → API → Redis (immediate) → Kafka → Batch Writer → PostgreSQL (async)
              ↓ (200 OK)
           Response
```

### Implementation

```java
@Service
public class WriteBehindCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private KafkaTemplate<String, WriteEvent> kafkaTemplate;
    
    // Write to cache immediately, DB asynchronously
    public void write(String key, Object value) {
        // Step 1: Write to Redis (fast, <1ms)
        redisTemplate.opsForValue().set(key, value);
        
        // Step 2: Publish to Kafka (async)
        WriteEvent event = new WriteEvent(key, value, Instant.now());
        kafkaTemplate.send("write-events", key, event);
        
        // Return immediately (don't wait for DB)
    }
    
    // Kafka consumer writes to DB in batches
    @KafkaListener(topics = "write-events", groupId = "db-writer")
    public void consumeWriteEvent(List<WriteEvent> events) {
        // Batch write to database
        jdbcTemplate.batchUpdate(
            "INSERT INTO data (key, value, timestamp) VALUES (?, ?, ?) " +
            "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
            events
        );
    }
}
```

**Benefits**:
- User sees <1ms write latency (Redis)
- Database writes batched (10x throughput)
- Kafka provides durability

**Trade-offs**:
- Eventual consistency (Redis → DB lag)
- Complexity (3 systems: Redis, Kafka, PostgreSQL)

---

## Solution 5: CQRS (Separate Read/Write Databases)

### Architecture

```
Write Path:
User → API → Write DB (PostgreSQL) → Event Stream (Kafka) → Read DB (MongoDB/Elasticsearch)

Read Path:
User → API → Read DB (optimized for queries)
```

### Implementation

```java
@Service
public class CQRSService {
    
    @Autowired
    private JdbcTemplate writeDb; // PostgreSQL (normalized)
    
    @Autowired
    private MongoTemplate readDb; // MongoDB (denormalized)
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    // Write to normalized DB
    @Transactional
    public Order createOrder(OrderRequest request) {
        // Write to PostgreSQL (ACID)
        Order order = writeDb.queryForObject(
            "INSERT INTO orders (user_id, total_amount, status) VALUES (?, ?, ?) RETURNING *",
            orderRowMapper,
            request.getUserId(), request.getTotalAmount(), "PENDING"
        );
        
        // Publish event
        kafkaTemplate.send("order-events", new OrderCreatedEvent(order));
        
        return order;
    }
    
    // Sync to read-optimized DB
    @KafkaListener(topics = "order-events")
    public void syncToReadDb(OrderCreatedEvent event) {
        // Denormalized view for fast reads
        OrderView view = OrderView.builder()
            .orderId(event.getOrder().getId())
            .userId(event.getOrder().getUserId())
            .userName(userService.getUserName(event.getOrder().getUserId())) // Denormalized
            .totalAmount(event.getOrder().getTotalAmount())
            .items(orderItemService.getItems(event.getOrder().getId())) // Denormalized
            .build();
        
        readDb.save(view);
    }
    
    // Read from denormalized DB
    public OrderView getOrder(String orderId) {
        return readDb.findById(orderId, OrderView.class);
    }
}
```

**Benefits**:
- Write DB optimized for writes (normalized, ACID)
- Read DB optimized for reads (denormalized, fast queries)
- Independent scaling

---

## Solution 6: Partition Tables (PostgreSQL Native)

### Table Partitioning

```sql
-- Partition by date (time-series data)
CREATE TABLE events (
    id BIGINT,
    user_id BIGINT,
    event_type VARCHAR(50),
    data JSONB,
    created_at TIMESTAMP NOT NULL
) PARTITION BY RANGE (created_at);

-- Create partitions
CREATE TABLE events_2024_01 PARTITION OF events
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE events_2024_02 PARTITION OF events
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Indexes on each partition
CREATE INDEX idx_events_2024_01_user ON events_2024_01(user_id);
CREATE INDEX idx_events_2024_02_user ON events_2024_02(user_id);
```

**Benefits**:
- Parallel writes to different partitions
- Faster queries (partition pruning)
- Easy archival (drop old partitions)

**Performance**:
- Before: 10K writes/sec (single table)
- After: 30K writes/sec (partitioned) - **3x improvement**

---

## Solution 7: Reduce Write Amplification

### Problem: Too Many Indexes

```sql
-- ❌ BAD: 10 indexes on table
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    status VARCHAR(20),
    created_at TIMESTAMP
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_product ON orders(product_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);
CREATE INDEX idx_orders_product_created ON orders(product_id, created_at);
-- ... 4 more indexes

-- Each write updates 1 table + 10 indexes = 11 writes!
```

### Solution: Minimize Indexes

```sql
-- ✅ GOOD: Only essential indexes
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    status VARCHAR(20),
    created_at TIMESTAMP
);

-- Composite index covers multiple queries
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC) WHERE status != 'COMPLETED';

-- Each write updates 1 table + 2 indexes = 3 writes (3.6x faster)
```

### Partial Indexes

```sql
-- Index only active orders (not completed)
CREATE INDEX idx_active_orders ON orders(user_id, created_at)
WHERE status IN ('PENDING', 'PROCESSING');

-- 80% of orders are completed → 80% fewer index writes
```

---

## Solution 8: Async Replication (Trade Durability)

### Synchronous vs Asynchronous Commit

```sql
-- postgresql.conf

-- ❌ Synchronous (slow, durable)
synchronous_commit = on
-- Waits for WAL to be written to disk
-- Latency: 10ms per write
-- Throughput: 10K writes/sec

-- ✅ Asynchronous (fast, less durable)
synchronous_commit = off
-- Returns immediately, WAL written async
-- Latency: 1ms per write
-- Throughput: 50K writes/sec (5x faster)

-- Risk: Lose last 1-2 seconds of data on crash
```

### Group Commit

```sql
-- Batch multiple transactions into single fsync
commit_delay = 10000  -- 10ms
commit_siblings = 5   -- Wait for 5 concurrent transactions

-- Result: 5 transactions = 1 disk write (5x throughput)
```

---

## Solution 9: Denormalize Data (Reduce Joins)

### Problem: Expensive Joins

```sql
-- ❌ Normalized (slow writes, slow reads)
SELECT o.*, u.name, u.email, p.title, p.price
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN products p ON o.product_id = p.id;

-- 3 table lookups, 2 joins
```

### Solution: Denormalize

```sql
-- ✅ Denormalized (fast writes, fast reads)
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    user_name VARCHAR(255),      -- Denormalized
    user_email VARCHAR(255),     -- Denormalized
    product_id BIGINT,
    product_title VARCHAR(500),  -- Denormalized
    product_price DECIMAL(10,2), -- Denormalized
    created_at TIMESTAMP
);

-- Single table lookup, no joins
SELECT * FROM orders WHERE id = ?;
```

**Trade-off**: Data duplication, update complexity

---

## Solution 10: Use Append-Only Pattern

### Problem: Updates are Expensive

```sql
-- ❌ UPDATE requires:
-- 1. Find row (index lookup)
-- 2. Lock row
-- 3. Update in-place
-- 4. Update indexes
-- 5. Write WAL

UPDATE orders SET status = 'COMPLETED' WHERE id = ?;
```

### Solution: Append-Only (Event Sourcing)

```sql
-- ✅ INSERT is faster (append to end)
CREATE TABLE order_events (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    event_type VARCHAR(50),
    event_data JSONB,
    created_at TIMESTAMP
);

-- Just append, no updates
INSERT INTO order_events (order_id, event_type, event_data, created_at)
VALUES (?, 'STATUS_CHANGED', '{"status": "COMPLETED"}', NOW());

-- Reconstruct current state from events
SELECT * FROM order_events WHERE order_id = ? ORDER BY created_at;
```

**Performance**: 3-5x faster than updates

---

## Solution 11: Connection Pooling Optimization

### Problem: Connection Overhead

```java
// ❌ BAD: Create connection per request
for (int i = 0; i < 10000; i++) {
    Connection conn = DriverManager.getConnection(url);
    // Execute query
    conn.close();
}
// Result: 10,000 connection handshakes (slow)
```

### Solution: Optimized Connection Pool

```yaml
spring:
  datasource:
    hikari:
      # Increase pool size
      maximum-pool-size: 200        # Up from 10
      minimum-idle: 50
      
      # Reduce connection overhead
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 1800000
      
      # Prepared statement cache
      cachePrepStmts: true
      prepStmtCacheSize: 500
      prepStmtCacheSqlLimit: 2048
      
      # Batch operations
      rewriteBatchedStatements: true
```

**Performance**: 2-3x improvement

---

## Solution 12: Use Time-Series Database

### For Time-Series Data (Logs, Metrics, Events)

```java
@Service
public class TimeSeriesWriteService {
    
    @Autowired
    private InfluxDBClient influxDBClient;
    
    // InfluxDB handles 1M+ writes/sec
    public void writeMetric(Metric metric) {
        Point point = Point.measurement("events")
            .addTag("user_id", metric.getUserId())
            .addTag("event_type", metric.getEventType())
            .addField("value", metric.getValue())
            .time(Instant.now(), WritePrecision.NS);
        
        influxDBClient.getWriteApiBlocking().writePoint(point);
    }
}
```

**Time-Series DB Options**:
- InfluxDB: 1M writes/sec
- TimescaleDB: 500K writes/sec (PostgreSQL extension)
- Prometheus: 1M samples/sec

---

## Complete Strategy (Layered Approach)

```java
@Service
public class HighThroughputWriteService {
    
    // Layer 1: Immediate response (Redis)
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Layer 2: Durable queue (Kafka)
    @Autowired
    private KafkaTemplate<String, WriteEvent> kafkaTemplate;
    
    // Layer 3: Batch writer (PostgreSQL)
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public CompletableFuture<Void> write(WriteRequest request) {
        // Step 1: Write to Redis (1ms)
        redisTemplate.opsForValue().set(request.getKey(), request.getValue());
        
        // Step 2: Queue to Kafka (5ms)
        kafkaTemplate.send("write-events", new WriteEvent(request));
        
        // Step 3: Return immediately
        return CompletableFuture.completedFuture(null);
    }
    
    // Background: Batch write to DB
    @KafkaListener(topics = "write-events", groupId = "db-writer")
    public void batchWriteToDb(List<WriteEvent> events) {
        // Batch 1000 events
        jdbcTemplate.batchUpdate(
            "INSERT INTO data VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
            events
        );
    }
}
```

**Result**:
- User latency: <10ms (Redis + Kafka)
- DB throughput: 50K writes/sec (batched)
- Durability: Kafka ensures no data loss

---

## Decision Matrix

| Solution | Throughput Gain | Complexity | Durability | Cost |
|----------|----------------|------------|------------|------|
| Vertical Scaling | 4x | Low | High | High |
| Cassandra/ScyllaDB | 10-100x | High | Medium | Medium |
| Batch Writes | 5x | Low | High | Low |
| Write-Behind Cache | 10x | Medium | Medium | Medium |
| CQRS | 5x | High | High | Medium |
| Table Partitioning | 3x | Low | High | Low |
| Reduce Indexes | 3x | Low | High | Low |
| Async Commit | 5x | Low | Low | Low |
| Denormalization | 2x | Medium | High | Low |
| Append-Only | 3-5x | Medium | High | Low |

---

## Key Takeaways

### Quick Wins (< 1 day)
1. **Batch writes** (5x improvement)
2. **Reduce indexes** (3x improvement)
3. **Async commit** (5x improvement)
4. **Connection pooling** (2x improvement)

### Medium-term (1 week)
1. **Write-behind caching** (10x improvement)
2. **Table partitioning** (3x improvement)
3. **Vertical scaling** (4x improvement)

### Long-term (1 month)
1. **CQRS** (5x improvement)
2. **Switch to Cassandra/ScyllaDB** (100x improvement)
3. **Event sourcing** (append-only)

---

## Interview Answer Summary

**Question**: DB write throughput maxed out, can't shard easily. What next?

**Answer**:

**Quick Wins** (< 1 day):
1. **Batch writes** - 1000 records at once (5x faster)
2. **Reduce indexes** - Only essential indexes (3x faster)
3. **Async commit** - Trade durability for speed (5x faster)
4. **Connection pooling** - Increase pool size (2x faster)

**Medium-term** (1 week):
1. **Write-behind cache** - Redis → Kafka → DB (10x faster)
2. **Table partitioning** - Parallel writes (3x faster)
3. **Vertical scaling** - Bigger instance (4x faster)

**Long-term** (1 month):
1. **CQRS** - Separate read/write DBs (5x faster)
2. **Cassandra/ScyllaDB** - Write-optimized DB (100x faster)
3. **Event sourcing** - Append-only pattern (5x faster)

**Combined**: Can achieve 50K+ writes/sec without sharding by combining multiple strategies.
