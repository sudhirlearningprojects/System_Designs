# Sharding, Rebalancing, and Consistent Hashing - Deep Dive

## Table of Contents
1. [Sharding Fundamentals](#1-sharding-fundamentals)
2. [Consistent Hashing](#2-consistent-hashing)
3. [Rebalancing Strategies](#3-rebalancing-strategies)
4. [Production Implementation](#4-production-implementation)
5. [Real-World Examples](#5-real-world-examples)
6. [Interview Questions](#6-interview-questions)

---

## 1. Sharding Fundamentals

### What is Sharding?

**Sharding** is a database partitioning technique that splits large datasets horizontally across multiple servers (shards). Each shard contains a subset of the total data and operates independently.

### Why Shard?

**Problem**: Single database limitations
- **Storage**: Single server has finite disk space (e.g., 10TB limit)
- **Memory**: Limited RAM for caching (e.g., 256GB)
- **CPU**: Single server can handle ~10K QPS max
- **Network**: Single NIC bandwidth ~10 Gbps

**Solution**: Horizontal scaling via sharding
- **Storage**: 100 shards × 10TB = 1PB total capacity
- **Throughput**: 100 shards × 10K QPS = 1M QPS
- **Availability**: Failure of 1 shard affects only 1% of data

### Sharding Strategies

#### 1. Hash-Based Sharding (Most Common)

**Concept**: Use hash function to determine shard placement

```
shard_id = hash(sharding_key) % num_shards
```

**Example**: User data sharding
```java
// User ID: "user_12345"
int shardId = Math.abs("user_12345".hashCode()) % 16;  // 16 shards
// Result: shard_id = 7
```

**Pros**:
- ✅ Uniform data distribution
- ✅ Simple implementation
- ✅ Predictable shard lookup

**Cons**:
- ❌ Adding/removing shards requires massive data migration
- ❌ Resharding affects ~(N-1)/N of data (e.g., 15/16 = 93.75%)

**Use Cases**: Instagram (user posts), Twitter (tweets), Facebook (user profiles)

---

#### 2. Range-Based Sharding

**Concept**: Partition data by key ranges

```
Shard 1: user_id 1-1M
Shard 2: user_id 1M-2M
Shard 3: user_id 2M-3M
```

**Example**: Time-series data
```java
// Order ID: "order_20240115_12345"
String date = extractDate(orderId);  // "2024-01-15"
if (date >= "2024-01-01" && date < "2024-02-01") {
    return "shard_2024_01";
} else if (date >= "2024-02-01" && date < "2024-03-01") {
    return "shard_2024_02";
}
```

**Pros**:
- ✅ Range queries are efficient (single shard)
- ✅ Easy to add new shards for new ranges
- ✅ Time-based archival is simple

**Cons**:
- ❌ Hotspots if data is not uniformly distributed
- ❌ Recent data gets more traffic (temporal locality)

**Use Cases**: Time-series databases (Prometheus, InfluxDB), log aggregation systems

---

#### 3. Geo-Based Sharding

**Concept**: Partition by geographic location

```
Shard US-EAST: Users in Eastern US
Shard US-WEST: Users in Western US
Shard EU: Users in Europe
Shard ASIA: Users in Asia
```

**Example**: Uber ride data
```java
// Pickup location: (37.7749, -122.4194) - San Francisco
String region = getRegion(latitude, longitude);
if (region.equals("US-WEST")) {
    return "shard_us_west";
}
```

**Pros**:
- ✅ Low latency (data close to users)
- ✅ Regulatory compliance (GDPR, data residency)
- ✅ Natural isolation for regional failures

**Cons**:
- ❌ Uneven distribution (population density varies)
- ❌ Cross-region queries are expensive

**Use Cases**: Uber (rides), Airbnb (listings), Netflix (content delivery)

---

#### 4. Directory-Based Sharding (Lookup Table)

**Concept**: Maintain a lookup table mapping keys to shards

```
Lookup Table:
user_123 → shard_5
user_456 → shard_2
user_789 → shard_5
```

**Example**: Multi-tenant SaaS
```java
// Tenant ID: "acme_corp"
String shardId = lookupTable.get("acme_corp");  // "shard_premium_1"
```

**Pros**:
- ✅ Flexible shard assignment
- ✅ Easy to move specific tenants
- ✅ Can group related data together

**Cons**:
- ❌ Lookup table is a single point of failure
- ❌ Extra network hop for every query
- ❌ Lookup table can become bottleneck

**Use Cases**: Salesforce (tenants), Slack (workspaces), Shopify (stores)

---

### Sharding Key Selection

**Critical Decision**: Choosing the right sharding key

**Good Sharding Keys**:
- ✅ **High Cardinality**: Many unique values (user_id, order_id)
- ✅ **Uniform Distribution**: Evenly spread across shards
- ✅ **Query Pattern Alignment**: Most queries include the key
- ✅ **Immutable**: Key doesn't change over time

**Bad Sharding Keys**:
- ❌ **Low Cardinality**: Few unique values (country, gender)
- ❌ **Temporal**: Recent values get all traffic (timestamp)
- ❌ **Monotonic**: Sequential IDs cause hotspots

**Example Comparison**:

```java
// ❌ BAD: Country as sharding key
shard = hash(country) % 16;
// Problem: USA has 50% of users → 1 shard gets 50% traffic

// ✅ GOOD: User ID as sharding key
shard = hash(user_id) % 16;
// Result: Uniform distribution across all shards
```

---

### Cross-Shard Queries

**Problem**: Query needs data from multiple shards

**Solution 1: Scatter-Gather**

```java
public List<Order> findOrdersByDateRange(Date start, Date end) {
    List<CompletableFuture<List<Order>>> futures = new ArrayList<>();
    
    // Query all shards in parallel
    for (Shard shard : allShards) {
        futures.add(CompletableFuture.supplyAsync(() -> 
            shard.query("SELECT * FROM orders WHERE date BETWEEN ? AND ?", start, end)
        ));
    }
    
    // Merge results
    return futures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .sorted(Comparator.comparing(Order::getDate))
        .collect(Collectors.toList());
}
```

**Solution 2: Denormalization**

```java
// Store aggregated data separately
// User shard: user_id → user_data
// Order summary shard: user_id → order_count, total_spent
```

**Solution 3: Application-Level Joins**

```java
// Step 1: Get user IDs from shard 1
List<String> userIds = shard1.query("SELECT user_id FROM users WHERE country = 'US'");

// Step 2: Get orders for those users from shard 2
List<Order> orders = shard2.query("SELECT * FROM orders WHERE user_id IN (?)", userIds);
```

---


## 2. Consistent Hashing

### The Problem with Simple Hashing

**Scenario**: You have 3 servers and use simple hash

```java
server = hash(key) % 3;  // 3 servers
```

**What happens when you add a 4th server?**

```java
server = hash(key) % 4;  // 4 servers
```

**Result**: ~75% of keys get remapped to different servers!

```
Before (3 servers):
key_1 → hash=5 → 5%3=2 → server_2
key_2 → hash=7 → 7%3=1 → server_1

After (4 servers):
key_1 → hash=5 → 5%4=1 → server_1  ❌ MOVED!
key_2 → hash=7 → 7%4=3 → server_3  ❌ MOVED!
```

**Impact**: Cache invalidation, massive data migration, downtime

---

### Consistent Hashing Solution

**Concept**: Map both keys and servers onto a circular hash ring (0 to 2^32-1)

**Algorithm**:
1. Hash each server to multiple points on the ring (virtual nodes)
2. Hash each key to a point on the ring
3. Assign key to the first server found clockwise

**Visual Representation**:

```
         0°
         |
    S1   |   S2
         |
270° ----+---- 90°
         |
    S3   |   S0
         |
        180°

Ring positions:
S0: 45°, 135°, 225°, 315°  (4 virtual nodes)
S1: 30°, 120°, 210°, 300°
S2: 60°, 150°, 240°, 330°
S3: 15°, 105°, 195°, 285°
```

**Key Assignment**:
```
key_A → hash=50° → clockwise → S2 (at 60°)
key_B → hash=200° → clockwise → S3 (at 210°)
```

---

### Implementation

```java
public class ConsistentHash {
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;
    private final MessageDigest md;
    
    public ConsistentHash(int virtualNodes) throws NoSuchAlgorithmException {
        this.virtualNodes = virtualNodes;
        this.md = MessageDigest.getInstance("MD5");
    }
    
    // Add server with virtual nodes
    public void addServer(String server) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(server + "#" + i);
            ring.put(hash, server);
        }
    }
    
    // Remove server
    public void removeServer(String server) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(server + "#" + i);
            ring.remove(hash);
        }
    }
    
    // Get server for key
    public String getServer(String key) {
        if (ring.isEmpty()) return null;
        
        long hash = hash(key);
        
        // Find first server clockwise
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        
        // Wrap around if needed
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    private long hash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        
        // Use first 8 bytes as long
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }
        return hash;
    }
}
```

**Usage**:
```java
ConsistentHash ch = new ConsistentHash(150);  // 150 virtual nodes per server

// Add servers
ch.addServer("server_1");
ch.addServer("server_2");
ch.addServer("server_3");

// Route keys
String server = ch.getServer("user_12345");  // → "server_2"
```

---

### Virtual Nodes (VNodes)

**Problem**: With few nodes, distribution is uneven

```
3 servers, 1 position each:
Server 1: 33.3% of keys
Server 2: 33.3% of keys
Server 3: 33.3% of keys
(In theory, but actual distribution varies ±10%)
```

**Solution**: Each server gets multiple virtual positions

```
3 servers, 150 virtual nodes each:
Server 1: 33.2% of keys
Server 2: 33.4% of keys
Server 3: 33.4% of keys
(Much more uniform distribution)
```

**Optimal Virtual Node Count**:
- **Small clusters (3-10 servers)**: 150-200 vnodes per server
- **Medium clusters (10-100 servers)**: 100-150 vnodes per server
- **Large clusters (100+ servers)**: 50-100 vnodes per server

**Trade-off**:
- More vnodes = Better distribution + Slower lookups
- Fewer vnodes = Worse distribution + Faster lookups

---

### Benefits of Consistent Hashing

**1. Minimal Redistribution**

When adding/removing a server, only K/N keys move (K = total keys, N = servers)

```
Before: 3 servers, 1M keys
Add 4th server:
Simple hash: 750K keys move (75%)
Consistent hash: 250K keys move (25%)
```

**2. Horizontal Scalability**

```java
// Add server dynamically
ch.addServer("server_4");
// Only ~25% of keys need to move
```

**3. Fault Tolerance**

```java
// Server fails
ch.removeServer("server_2");
// Its keys redistribute to remaining servers
```

---

### Consistent Hashing with Replication

**Concept**: Store each key on N consecutive servers for redundancy

```java
public List<String> getServers(String key, int replicationFactor) {
    if (ring.isEmpty()) return Collections.emptyList();
    
    List<String> servers = new ArrayList<>();
    Set<String> uniqueServers = new HashSet<>();
    
    long hash = hash(key);
    
    // Find N unique servers clockwise
    Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
    
    while (uniqueServers.size() < replicationFactor) {
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        String server = entry.getValue();
        if (uniqueServers.add(server)) {
            servers.add(server);
        }
        
        entry = ring.higherEntry(entry.getKey());
    }
    
    return servers;
}
```

**Usage**:
```java
// Store key on 3 servers
List<String> servers = ch.getServers("user_12345", 3);
// Result: ["server_2", "server_3", "server_1"]

for (String server : servers) {
    writeToServer(server, "user_12345", userData);
}
```

---


## 3. Rebalancing Strategies

### When to Rebalance?

**Triggers**:
1. **Adding new shards**: Scale out for capacity/performance
2. **Removing shards**: Decommission old hardware
3. **Hotspot detection**: One shard gets disproportionate traffic
4. **Storage imbalance**: One shard is 80% full, others at 40%

**Metrics to Monitor**:
```
- Storage utilization per shard
- QPS per shard
- CPU/Memory usage per shard
- Query latency per shard
```

---

### Rebalancing Approaches

#### 1. Stop-the-World Rebalancing (Offline)

**Process**:
1. Put system in read-only mode
2. Migrate data to new shard configuration
3. Update routing logic
4. Resume normal operations

**Pros**: Simple, consistent
**Cons**: Downtime (unacceptable for production)

**Use Case**: Small systems, maintenance windows

---

#### 2. Live Migration (Online)

**Process** (Dual-Write Strategy):

```
Phase 1: Preparation
- Add new shards
- Start dual-writing (write to both old and new shards)

Phase 2: Backfill
- Copy existing data from old to new shards
- Verify data consistency

Phase 3: Cutover
- Switch reads to new shards
- Stop writing to old shards
- Decommission old shards
```

**Implementation**:

```java
public class LiveMigration {
    private boolean migrationInProgress = false;
    private Set<String> migratingKeys = new HashSet<>();
    
    public void write(String key, String value) {
        String primaryShard = getNewShard(key);
        
        if (migrationInProgress && needsDualWrite(key)) {
            // Dual write during migration
            String oldShard = getOldShard(key);
            writeToShard(oldShard, key, value);  // Old location
            writeToShard(primaryShard, key, value);  // New location
        } else {
            // Normal write
            writeToShard(primaryShard, key, value);
        }
    }
    
    public String read(String key) {
        if (migrationInProgress && !isMigrated(key)) {
            // Read from old shard during migration
            return readFromShard(getOldShard(key), key);
        } else {
            // Read from new shard
            return readFromShard(getNewShard(key), key);
        }
    }
    
    // Background job
    public void backfillData() {
        for (String key : getAllKeys()) {
            if (!isMigrated(key)) {
                String value = readFromShard(getOldShard(key), key);
                writeToShard(getNewShard(key), key, value);
                markAsMigrated(key);
            }
        }
    }
}
```

**Timeline Example**:
```
Day 1: Start dual-write (0% migrated)
Day 2-7: Backfill data (10% → 90% migrated)
Day 8: Verify consistency (100% migrated)
Day 9: Switch reads to new shards
Day 10: Decommission old shards
```

---

#### 3. Consistent Hashing Rebalancing

**Advantage**: Only affected keys move

```java
// Before: 4 servers
ConsistentHash ch = new ConsistentHash(150);
ch.addServer("s1");
ch.addServer("s2");
ch.addServer("s3");
ch.addServer("s4");

// Add 5th server
ch.addServer("s5");

// Result: Only ~20% of keys move (1/5)
// Keys that were assigned to s1-s4 in the range now covered by s5
```

**Migration Process**:

```java
public void rebalanceWithConsistentHash(String newServer) {
    // 1. Add new server to ring
    consistentHash.addServer(newServer);
    
    // 2. Identify keys that need to move
    List<String> keysToMove = new ArrayList<>();
    for (String key : getAllKeys()) {
        String oldServer = oldConsistentHash.getServer(key);
        String newServerForKey = consistentHash.getServer(key);
        
        if (!oldServer.equals(newServerForKey)) {
            keysToMove.add(key);
        }
    }
    
    // 3. Migrate keys
    for (String key : keysToMove) {
        String value = readFromServer(oldServer, key);
        writeToServer(newServerForKey, key, value);
        deleteFromServer(oldServer, key);
    }
}
```

---

### Rebalancing Best Practices

**1. Rate Limiting**

```java
// Limit migration speed to avoid overwhelming system
RateLimiter migrationRateLimiter = RateLimiter.create(1000);  // 1000 keys/sec

for (String key : keysToMigrate) {
    migrationRateLimiter.acquire();
    migrateKey(key);
}
```

**2. Checkpointing**

```java
// Save progress to resume after failures
public void migrateWithCheckpoint() {
    String lastMigratedKey = loadCheckpoint();
    
    for (String key : getKeysAfter(lastMigratedKey)) {
        migrateKey(key);
        
        if (++count % 10000 == 0) {
            saveCheckpoint(key);  // Save every 10K keys
        }
    }
}
```

**3. Verification**

```java
// Verify data consistency after migration
public boolean verifyMigration(String key) {
    String oldValue = readFromOldShard(key);
    String newValue = readFromNewShard(key);
    
    if (!oldValue.equals(newValue)) {
        log.error("Mismatch for key: {}", key);
        return false;
    }
    return true;
}
```

**4. Rollback Plan**

```java
// Keep old shards until verification complete
public void rollback() {
    // Switch routing back to old shards
    routingConfig.setActiveShards(oldShards);
    
    // Stop dual-write
    migrationInProgress = false;
    
    log.info("Rolled back to old shard configuration");
}
```

---

### Hotspot Mitigation

**Problem**: One shard gets disproportionate traffic

**Solution 1: Split Hot Shard**

```java
// Detect hotspot
if (shard.getQPS() > avgQPS * 2) {
    // Split shard into 2
    Shard shard1 = new Shard(startKey, midKey);
    Shard shard2 = new Shard(midKey, endKey);
    
    // Migrate data
    migrateData(hotShard, shard1, shard2);
}
```

**Solution 2: Add Read Replicas**

```java
// Route reads to replicas
public String read(String key) {
    String shardId = getShardId(key);
    
    if (isHotShard(shardId)) {
        // Load balance across replicas
        String replica = selectReplica(shardId);
        return readFromReplica(replica, key);
    } else {
        return readFromPrimary(shardId, key);
    }
}
```

**Solution 3: Cache Hot Data**

```java
// Cache frequently accessed data
@Cacheable(value = "hotData", key = "#key")
public String getData(String key) {
    return readFromShard(key);
}
```

---


## 4. Production Implementation

### Complete Sharding System

```java
@Service
public class ShardingService {
    private final ConsistentHash consistentHash;
    private final Map<String, DataSource> shardDataSources;
    private final RedisTemplate<String, String> redis;
    
    // Route query to correct shard
    public <T> T executeQuery(String shardingKey, ShardQuery<T> query) {
        String shardId = consistentHash.getServer(shardingKey);
        DataSource dataSource = shardDataSources.get(shardId);
        
        try (Connection conn = dataSource.getConnection()) {
            return query.execute(conn);
        }
    }
    
    // Cross-shard query (scatter-gather)
    public <T> List<T> executeGlobalQuery(ShardQuery<List<T>> query) {
        List<CompletableFuture<List<T>>> futures = new ArrayList<>();
        
        // Execute on all shards in parallel
        for (Map.Entry<String, DataSource> entry : shardDataSources.entrySet()) {
            CompletableFuture<List<T>> future = CompletableFuture.supplyAsync(() -> {
                try (Connection conn = entry.getValue().getConnection()) {
                    return query.execute(conn);
                }
            });
            futures.add(future);
        }
        
        // Gather results
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    // Distributed transaction (2PC)
    public void executeDistributedTransaction(
        Map<String, ShardQuery<Void>> shardQueries
    ) {
        // Phase 1: Prepare
        Map<String, Connection> connections = new HashMap<>();
        try {
            for (Map.Entry<String, ShardQuery<Void>> entry : shardQueries.entrySet()) {
                String shardId = entry.getKey();
                Connection conn = shardDataSources.get(shardId).getConnection();
                conn.setAutoCommit(false);
                
                entry.getValue().execute(conn);
                connections.put(shardId, conn);
            }
            
            // Phase 2: Commit
            for (Connection conn : connections.values()) {
                conn.commit();
            }
        } catch (Exception e) {
            // Rollback all
            for (Connection conn : connections.values()) {
                conn.rollback();
            }
            throw new RuntimeException("Distributed transaction failed", e);
        } finally {
            for (Connection conn : connections.values()) {
                conn.close();
            }
        }
    }
}

@FunctionalInterface
interface ShardQuery<T> {
    T execute(Connection conn) throws SQLException;
}
```

---

### Shard Registry

```java
@Service
public class ShardRegistry {
    private final Map<String, ShardMetadata> shards = new ConcurrentHashMap<>();
    
    @Data
    public static class ShardMetadata {
        private String shardId;
        private String host;
        private int port;
        private ShardStatus status;
        private long storageUsedBytes;
        private long storageCapacityBytes;
        private double qps;
        private Instant lastHealthCheck;
    }
    
    public enum ShardStatus {
        ACTIVE, READONLY, DRAINING, OFFLINE
    }
    
    // Health check
    @Scheduled(fixedRate = 5000)
    public void healthCheck() {
        for (ShardMetadata shard : shards.values()) {
            try {
                boolean healthy = pingShard(shard);
                if (!healthy && shard.getStatus() == ShardStatus.ACTIVE) {
                    shard.setStatus(ShardStatus.OFFLINE);
                    alertOncall("Shard " + shard.getShardId() + " is down!");
                }
            } catch (Exception e) {
                log.error("Health check failed for shard: {}", shard.getShardId(), e);
            }
        }
    }
    
    // Auto-scaling trigger
    public void checkRebalanceNeeded() {
        double avgQps = shards.values().stream()
            .mapToDouble(ShardMetadata::getQps)
            .average()
            .orElse(0);
        
        for (ShardMetadata shard : shards.values()) {
            // Hotspot detection
            if (shard.getQps() > avgQps * 2) {
                log.warn("Hotspot detected on shard: {}", shard.getShardId());
                triggerRebalance(shard);
            }
            
            // Storage threshold
            double utilization = (double) shard.getStorageUsedBytes() / shard.getStorageCapacityBytes();
            if (utilization > 0.8) {
                log.warn("Storage threshold exceeded on shard: {}", shard.getShardId());
                triggerScaleOut();
            }
        }
    }
}
```

---

### Shard-Aware Repository

```java
@Repository
public class UserRepository {
    private final ShardingService shardingService;
    
    public User findById(String userId) {
        return shardingService.executeQuery(userId, conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE user_id = ?"
            );
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapToUser(rs);
            }
            return null;
        });
    }
    
    public void save(User user) {
        shardingService.executeQuery(user.getUserId(), conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (user_id, name, email) VALUES (?, ?, ?)"
            );
            ps.setString(1, user.getUserId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.executeUpdate();
            return null;
        });
    }
    
    // Cross-shard query
    public List<User> findByCountry(String country) {
        return shardingService.executeGlobalQuery(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE country = ?"
            );
            ps.setString(1, country);
            ResultSet rs = ps.executeQuery();
            
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapToUser(rs));
            }
            return users;
        });
    }
}
```

---

### Configuration

```yaml
# application.yml
sharding:
  strategy: CONSISTENT_HASH
  virtual-nodes: 150
  shards:
    - id: shard_1
      host: db1.example.com
      port: 5432
      database: app_shard_1
      username: app_user
      password: ${SHARD_1_PASSWORD}
      
    - id: shard_2
      host: db2.example.com
      port: 5432
      database: app_shard_2
      username: app_user
      password: ${SHARD_2_PASSWORD}
      
    - id: shard_3
      host: db3.example.com
      port: 5432
      database: app_shard_3
      username: app_user
      password: ${SHARD_3_PASSWORD}
```

```java
@Configuration
public class ShardingConfig {
    
    @Bean
    public ConsistentHash consistentHash(
        @Value("${sharding.virtual-nodes}") int virtualNodes
    ) throws NoSuchAlgorithmException {
        return new ConsistentHash(virtualNodes);
    }
    
    @Bean
    public Map<String, DataSource> shardDataSources(
        @ConfigurationProperties(prefix = "sharding") ShardingProperties props
    ) {
        Map<String, DataSource> dataSources = new HashMap<>();
        
        for (ShardConfig shard : props.getShards()) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format(
                "jdbc:postgresql://%s:%d/%s",
                shard.getHost(), shard.getPort(), shard.getDatabase()
            ));
            config.setUsername(shard.getUsername());
            config.setPassword(shard.getPassword());
            config.setMaximumPoolSize(20);
            
            dataSources.put(shard.getId(), new HikariDataSource(config));
        }
        
        return dataSources;
    }
}
```

---


## 5. Real-World Examples

### Instagram: User Posts Sharding

**Sharding Strategy**: Hash-based on user_id

```sql
-- Logical schema
CREATE TABLE posts (
    post_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    image_url VARCHAR(500),
    caption TEXT,
    created_at TIMESTAMP
);

-- Physical shards (4096 shards)
shard_id = user_id % 4096
```

**Query Routing**:
```java
// Get user's posts (single shard)
long userId = 12345;
int shardId = (int) (userId % 4096);
List<Post> posts = queryShardById(shardId, 
    "SELECT * FROM posts WHERE user_id = ?", userId);

// Get post by ID (need user_id for routing)
long postId = 98765;
long userId = extractUserIdFromPostId(postId);  // Encoded in post_id
int shardId = (int) (userId % 4096);
Post post = queryShardById(shardId,
    "SELECT * FROM posts WHERE post_id = ?", postId);
```

**Scale**: 4096 shards, 2B users, 100B posts

---

### Discord: Message Sharding

**Sharding Strategy**: Range-based on channel_id + timestamp

```java
// Shard by channel and time bucket
String shardId = String.format("messages_%s_%s", 
    channelId, 
    getTimeBucket(timestamp));  // "2024-01"

// Example
channelId = "channel_123"
timestamp = "2024-01-15"
shardId = "messages_channel_123_2024_01"
```

**Benefits**:
- Recent messages in hot cache
- Old messages can be archived
- Range queries within channel are efficient

**Scale**: 100K shards, 1B messages/day

---

### Uber: Geo-Sharding

**Sharding Strategy**: Geohash-based

```java
// Geohash precision 4 = ~20km × 20km
String geohash = Geohash.encode(latitude, longitude, 4);  // "9q8y"
String shardId = "rides_" + geohash;

// Example: San Francisco
latitude = 37.7749
longitude = -122.4194
geohash = "9q8y"
shardId = "rides_9q8y"
```

**Query Pattern**:
```java
// Find nearby drivers (single shard)
String riderGeohash = "9q8y";
List<Driver> drivers = queryShardById("rides_9q8y",
    "SELECT * FROM drivers WHERE status = 'AVAILABLE'");

// Cross-region query (multiple shards)
List<String> neighborGeohashes = getNeighbors("9q8y");  // ["9q8y", "9q8v", "9q8w", ...]
List<Driver> allDrivers = neighborGeohashes.stream()
    .flatMap(gh -> queryShardById("rides_" + gh, "SELECT * FROM drivers").stream())
    .collect(Collectors.toList());
```

**Scale**: 10K geo-shards, 5M drivers, 100M rides/day

---

### Cassandra: Consistent Hashing in Production

**Architecture**:
```
Ring: 0 to 2^127-1
Nodes: 100 nodes
Virtual nodes: 256 per node
Replication factor: 3
```

**Token Assignment**:
```java
// Each node gets 256 tokens
Node 1: tokens [123, 456, 789, ...]
Node 2: tokens [234, 567, 890, ...]
...

// Key placement
key = "user_12345"
token = hash(key) = 50000
// Find 3 nodes clockwise from token 50000
replicas = [Node_5, Node_12, Node_23]
```

**Rebalancing**:
```bash
# Add new node
nodetool join

# Cassandra automatically:
# 1. Assigns 256 tokens to new node
# 2. Streams data from existing nodes
# 3. Updates routing table
# 4. No downtime!
```

---

### DynamoDB: Adaptive Capacity

**Problem**: Hot partition gets throttled

```
Partition 1: 1000 RCU/sec (throttled at 3000 RCU limit)
Partition 2: 100 RCU/sec
Partition 3: 50 RCU/sec
```

**Solution**: Automatic partition splitting

```java
// DynamoDB detects hot partition
if (partition.getRCU() > threshold) {
    // Split partition
    Partition p1 = new Partition(startKey, midKey);
    Partition p2 = new Partition(midKey, endKey);
    
    // Each gets half the capacity
    p1.setCapacity(originalCapacity / 2);
    p2.setCapacity(originalCapacity / 2);
    
    // Migrate data
    migrateData(originalPartition, p1, p2);
}
```

**Result**: Hot partition split into 2, doubling capacity

---

### Twitter: Snowflake ID Generation

**Problem**: Need globally unique IDs across shards

**Solution**: Snowflake ID (64-bit)

```
| 1 bit (unused) | 41 bits (timestamp) | 10 bits (machine ID) | 12 bits (sequence) |
```

```java
public class SnowflakeIdGenerator {
    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF;  // 12 bits
            if (sequence == 0) {
                // Wait for next millisecond
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << 22) | (machineId << 12) | sequence;
    }
}
```

**Benefits**:
- Time-ordered IDs
- No coordination between shards
- 4096 IDs per millisecond per machine

---

### YouTube: Video Metadata Sharding

**Sharding Strategy**: Hash-based on video_id

```java
// Video ID: "dQw4w9WgXcQ" (11 characters)
int shardId = Math.abs("dQw4w9WgXcQ".hashCode()) % 1024;

// Store video metadata
VideoMetadata metadata = new VideoMetadata(
    videoId, title, description, uploaderId, views, likes
);
saveToShard(shardId, metadata);
```

**Denormalization for Performance**:
```java
// Shard 1: video_id → video_metadata
// Shard 2: user_id → list of video_ids (user's uploads)
// Shard 3: channel_id → list of video_ids (channel videos)
```

**Scale**: 1024 shards, 800M videos, 500 hours uploaded/min

---

### LinkedIn: Connection Graph Sharding

**Sharding Strategy**: Bidirectional edges stored on both user shards

```java
// User A connects with User B
int shardA = hash(userA) % NUM_SHARDS;
int shardB = hash(userB) % NUM_SHARDS;

// Store edge on both shards
saveToShard(shardA, new Edge(userA, userB));
saveToShard(shardB, new Edge(userB, userA));
```

**Query Pattern**:
```java
// Get user's connections (single shard)
int shardId = hash(userId) % NUM_SHARDS;
List<String> connections = queryShardById(shardId,
    "SELECT target_user FROM connections WHERE source_user = ?", userId);

// 2nd degree connections (scatter-gather)
List<String> secondDegree = connections.stream()
    .flatMap(conn -> getConnections(conn).stream())
    .distinct()
    .collect(Collectors.toList());
```

**Scale**: 256 shards, 900M users, 30B connections

---


## 6. Interview Questions

### Conceptual Questions

**Q1: What is sharding and why do we need it?**

**Answer**: Sharding is horizontal partitioning of data across multiple servers. We need it because:
- Single server has limited storage (e.g., 10TB)
- Single server has limited throughput (e.g., 10K QPS)
- Horizontal scaling is cheaper than vertical scaling
- Provides fault isolation (one shard failure doesn't affect others)

**Example**: Instagram has 2B users and 100B posts. A single PostgreSQL server can't handle this. They use 4096 shards, each handling ~25M posts.

---

**Q2: What's the difference between sharding and partitioning?**

**Answer**:
- **Partitioning**: Splitting data within a single database (vertical or horizontal)
- **Sharding**: Horizontal partitioning across multiple physical servers

**Example**:
```sql
-- Partitioning (single database)
CREATE TABLE orders_2024_01 PARTITION OF orders FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Sharding (multiple databases)
Server 1: orders where user_id % 4 = 0
Server 2: orders where user_id % 4 = 1
Server 3: orders where user_id % 4 = 2
Server 4: orders where user_id % 4 = 3
```

---

**Q3: How does consistent hashing solve the rebalancing problem?**

**Answer**: 
- **Simple hash**: Adding/removing server remaps ~(N-1)/N keys (e.g., 75% for 4→3 servers)
- **Consistent hash**: Only K/N keys move (e.g., 25% for 4→3 servers)

**Mechanism**: Both keys and servers are mapped to a ring. Each key goes to the first server clockwise. When a server is added/removed, only keys in its range move.

**Example**:
```
Before: 3 servers, 1M keys
Add 4th server:
- Simple hash: 750K keys move (75%)
- Consistent hash: 250K keys move (25%)
```

---

**Q4: What are virtual nodes and why are they important?**

**Answer**: Virtual nodes (vnodes) are multiple positions on the hash ring for each physical server.

**Problem without vnodes**:
```
3 servers, 1 position each:
Server 1: 40% of keys (uneven!)
Server 2: 35% of keys
Server 3: 25% of keys
```

**Solution with vnodes**:
```
3 servers, 150 vnodes each:
Server 1: 33.2% of keys (uniform!)
Server 2: 33.4% of keys
Server 3: 33.4% of keys
```

**Optimal count**: 100-200 vnodes per server

---

**Q5: How do you handle cross-shard queries?**

**Answer**: Three approaches:

**1. Scatter-Gather** (query all shards, merge results)
```java
List<Order> orders = allShards.parallelStream()
    .flatMap(shard -> shard.query("SELECT * FROM orders WHERE date = ?", date).stream())
    .collect(Collectors.toList());
```

**2. Denormalization** (duplicate data for different access patterns)
```java
// Shard 1: user_id → user_data
// Shard 2: email → user_id (for email lookup)
```

**3. Application-Level Joins** (fetch from multiple shards sequentially)
```java
List<User> users = shard1.query("SELECT * FROM users WHERE country = 'US'");
List<Order> orders = shard2.query("SELECT * FROM orders WHERE user_id IN (?)", userIds);
```

---

### Design Questions

**Q6: Design a sharding strategy for Twitter's tweet storage**

**Answer**:

**Requirements**:
- 500M users, 500M tweets/day
- Query patterns: Get user's tweets, Get tweet by ID

**Sharding Strategy**: Hash-based on user_id

```java
shard_id = hash(user_id) % 4096  // 4096 shards
```

**Schema**:
```sql
CREATE TABLE tweets (
    tweet_id BIGINT PRIMARY KEY,
    user_id BIGINT,
    content TEXT,
    created_at TIMESTAMP
);

CREATE INDEX idx_user_tweets ON tweets(user_id, created_at DESC);
```

**Query Routing**:
```java
// Get user's tweets (single shard)
int shardId = hash(userId) % 4096;
List<Tweet> tweets = queryShardById(shardId, 
    "SELECT * FROM tweets WHERE user_id = ? ORDER BY created_at DESC LIMIT 20", userId);

// Get tweet by ID (encode user_id in tweet_id)
long userId = extractUserIdFromTweetId(tweetId);
int shardId = hash(userId) % 4096;
Tweet tweet = queryShardById(shardId, "SELECT * FROM tweets WHERE tweet_id = ?", tweetId);
```

**Scale**: 4096 shards × 2.5TB = 10PB total storage

---

**Q7: How would you migrate from 16 shards to 32 shards with zero downtime?**

**Answer**: Use dual-write strategy

**Phase 1: Preparation (Day 1)**
```java
// Add 32 new shards
// Start dual-writing
public void write(String key, String value) {
    int oldShard = hash(key) % 16;
    int newShard = hash(key) % 32;
    
    writeToShard(oldShard, key, value);  // Old
    writeToShard(newShard, key, value);  // New
}

// Read from old shards
public String read(String key) {
    int oldShard = hash(key) % 16;
    return readFromShard(oldShard, key);
}
```

**Phase 2: Backfill (Day 2-7)**
```java
// Background job to copy existing data
for (String key : getAllKeys()) {
    String value = readFromOldShard(key);
    writeToNewShard(key, value);
}
```

**Phase 3: Cutover (Day 8)**
```java
// Switch reads to new shards
public String read(String key) {
    int newShard = hash(key) % 32;
    return readFromShard(newShard, key);
}

// Stop dual-write
public void write(String key, String value) {
    int newShard = hash(key) % 32;
    writeToShard(newShard, key, value);
}
```

**Phase 4: Cleanup (Day 9-10)**
```
- Verify data consistency
- Decommission old 16 shards
```

---

**Q8: Design sharding for Uber's ride data**

**Answer**:

**Requirements**:
- 100M rides/day
- Query patterns: Find nearby drivers, Get ride history

**Sharding Strategy**: Geo-based + Time-based

```java
// Active rides: Geo-sharding (for nearby driver queries)
String geohash = Geohash.encode(latitude, longitude, 4);  // ~20km
String shardId = "active_rides_" + geohash;

// Historical rides: User-based sharding (for ride history)
int shardId = hash(userId) % 256;
```

**Schema**:
```sql
-- Active rides (geo-sharded)
CREATE TABLE active_rides (
    ride_id UUID PRIMARY KEY,
    driver_id UUID,
    rider_id UUID,
    pickup_lat DOUBLE,
    pickup_lng DOUBLE,
    status VARCHAR(20),
    created_at TIMESTAMP
);

CREATE INDEX idx_geo ON active_rides USING GIST (
    ll_to_earth(pickup_lat, pickup_lng)
);

-- Historical rides (user-sharded)
CREATE TABLE ride_history (
    ride_id UUID PRIMARY KEY,
    user_id UUID,
    driver_id UUID,
    fare DECIMAL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_user_rides ON ride_history(user_id, completed_at DESC);
```

**Query Routing**:
```java
// Find nearby drivers (single geo-shard)
String geohash = Geohash.encode(riderLat, riderLng, 4);
List<Driver> drivers = queryShardById("active_rides_" + geohash,
    "SELECT * FROM drivers WHERE status = 'AVAILABLE' AND distance < 5km");

// Get user's ride history (single user-shard)
int shardId = hash(userId) % 256;
List<Ride> history = queryShardById(shardId,
    "SELECT * FROM ride_history WHERE user_id = ? ORDER BY completed_at DESC", userId);
```

**Scale**: 10K geo-shards + 256 user-shards

---

**Q9: How do you handle hotspots in sharding?**

**Answer**: Multiple strategies

**1. Detect Hotspots**
```java
@Scheduled(fixedRate = 60000)
public void detectHotspots() {
    double avgQPS = shards.stream().mapToDouble(Shard::getQPS).average().orElse(0);
    
    for (Shard shard : shards) {
        if (shard.getQPS() > avgQPS * 2) {
            log.warn("Hotspot detected: {}", shard.getId());
            mitigateHotspot(shard);
        }
    }
}
```

**2. Add Read Replicas**
```java
// Route reads to replicas
public String read(String key) {
    int shardId = getShardId(key);
    
    if (isHotShard(shardId)) {
        String replica = selectReplica(shardId);  // Round-robin
        return readFromReplica(replica, key);
    }
    return readFromPrimary(shardId, key);
}
```

**3. Split Hot Shard**
```java
// Split shard_5 into shard_5a and shard_5b
if (shard5.getQPS() > threshold) {
    splitShard(shard5, shard5a, shard5b);
}
```

**4. Cache Hot Data**
```java
@Cacheable(value = "hotData", key = "#key")
public String getData(String key) {
    return readFromShard(key);
}
```

---

**Q10: Explain distributed transactions across shards**

**Answer**: Use Two-Phase Commit (2PC)

**Phase 1: Prepare**
```java
// Coordinator asks all shards to prepare
Map<String, Connection> connections = new HashMap<>();

for (String shardId : involvedShards) {
    Connection conn = getConnection(shardId);
    conn.setAutoCommit(false);
    
    // Execute query
    executeQuery(conn, query);
    
    // Ask to prepare
    boolean canCommit = conn.prepareCommit();
    if (!canCommit) {
        throw new Exception("Shard " + shardId + " cannot commit");
    }
    
    connections.put(shardId, conn);
}
```

**Phase 2: Commit**
```java
try {
    // All shards agreed, commit all
    for (Connection conn : connections.values()) {
        conn.commit();
    }
} catch (Exception e) {
    // Rollback all
    for (Connection conn : connections.values()) {
        conn.rollback();
    }
    throw e;
}
```

**Example**: Transfer money between users on different shards
```java
// User A on shard_1, User B on shard_2
executeDistributedTransaction(Map.of(
    "shard_1", conn -> updateBalance(conn, userA, -100),
    "shard_2", conn -> updateBalance(conn, userB, +100)
));
```

**Limitations**:
- Blocking protocol (locks held during 2PC)
- Coordinator is single point of failure
- High latency (2 network round-trips)

**Alternative**: Saga pattern for eventual consistency

---

### Troubleshooting Questions

**Q11: One shard is at 90% storage capacity, others at 40%. What do you do?**

**Answer**:

**Immediate Action**:
1. Add read replicas to hot shard
2. Enable compression on hot shard
3. Archive old data to cold storage

**Long-term Solution**:
```java
// Split hot shard
Shard hotShard = shards.get("shard_5");
Shard newShard1 = new Shard("shard_5a", startKey, midKey);
Shard newShard2 = new Shard("shard_5b", midKey, endKey);

// Migrate data
migrateData(hotShard, newShard1, newShard2);

// Update routing
updateShardRouting(hotShard, newShard1, newShard2);
```

**Prevention**:
- Monitor storage utilization
- Set alerts at 70% capacity
- Auto-scale when threshold reached

---

**Q12: After adding a new shard, queries are failing. How do you debug?**

**Answer**:

**Step 1: Check Routing Configuration**
```java
// Verify new shard is in routing table
ConsistentHash ch = getConsistentHash();
String server = ch.getServer("test_key");
log.info("test_key routes to: {}", server);
```

**Step 2: Check Data Migration Status**
```java
// Verify data was migrated
for (String key : sampleKeys) {
    String oldValue = readFromOldShard(key);
    String newValue = readFromNewShard(key);
    
    if (!oldValue.equals(newValue)) {
        log.error("Data mismatch for key: {}", key);
    }
}
```

**Step 3: Check Connection Pool**
```java
// Verify new shard has connection pool
DataSource ds = shardDataSources.get("new_shard");
if (ds == null) {
    log.error("No connection pool for new shard!");
}
```

**Step 4: Rollback if Needed**
```java
// Switch routing back to old configuration
routingConfig.setActiveShards(oldShards);
migrationInProgress = false;
```

---

## Summary

### Key Takeaways

**Sharding**:
- Horizontal partitioning for scalability
- Choose sharding key carefully (high cardinality, uniform distribution)
- Hash-based for uniform load, Range-based for range queries, Geo-based for locality

**Consistent Hashing**:
- Minimizes data movement during rebalancing (K/N instead of K)
- Virtual nodes ensure uniform distribution
- Used by Cassandra, DynamoDB, Memcached, Redis Cluster

**Rebalancing**:
- Live migration with dual-write for zero downtime
- Rate limit migration to avoid overwhelming system
- Verify data consistency before cutover
- Always have rollback plan

**Production Considerations**:
- Monitor shard health and load continuously
- Automate rebalancing based on metrics
- Use consistent hashing for dynamic scaling
- Plan for cross-shard queries (scatter-gather)
- Implement distributed transactions carefully (2PC overhead)

---

### Comparison Table

| Aspect | Simple Hash | Consistent Hash | Range-Based | Geo-Based |
|--------|-------------|-----------------|-------------|-----------|
| **Distribution** | Uniform | Uniform | Can be skewed | Can be skewed |
| **Rebalancing** | 75-90% keys move | 1/N keys move | Minimal | Minimal |
| **Lookup Speed** | O(1) | O(log N) | O(1) | O(1) |
| **Range Queries** | Scatter-gather | Scatter-gather | Single shard | Single region |
| **Hotspots** | Rare | Rare | Common | Common |
| **Use Case** | Static clusters | Dynamic clusters | Time-series | Location-based |

---

### When to Use What

**Hash-Based Sharding**:
- ✅ Uniform data distribution needed
- ✅ Point queries (lookup by key)
- ✅ Static number of shards
- ❌ Range queries
- **Examples**: Instagram (posts), Twitter (tweets)

**Consistent Hashing**:
- ✅ Dynamic cluster (frequent add/remove nodes)
- ✅ Minimal data movement during rebalancing
- ✅ Caching systems
- ❌ Range queries
- **Examples**: Cassandra, DynamoDB, Memcached

**Range-Based Sharding**:
- ✅ Range queries common
- ✅ Time-series data
- ✅ Easy to archive old data
- ❌ Hotspots on recent data
- **Examples**: Time-series DBs, log aggregation

**Geo-Based Sharding**:
- ✅ Low latency (data close to users)
- ✅ Regulatory compliance (data residency)
- ✅ Regional isolation
- ❌ Uneven distribution
- **Examples**: Uber (rides), Netflix (content)

---

### Production Checklist

**Before Sharding**:
- [ ] Identify sharding key (high cardinality, immutable)
- [ ] Analyze query patterns
- [ ] Choose sharding strategy
- [ ] Plan for cross-shard queries
- [ ] Design ID generation (Snowflake)

**During Migration**:
- [ ] Set up dual-write
- [ ] Backfill data with rate limiting
- [ ] Verify data consistency
- [ ] Monitor performance metrics
- [ ] Have rollback plan ready

**After Sharding**:
- [ ] Monitor shard health (QPS, storage, latency)
- [ ] Set up alerts for hotspots
- [ ] Automate rebalancing
- [ ] Document shard topology
- [ ] Plan for future scaling

---

This guide covers the theoretical foundations and practical implementations used by companies like Instagram, Uber, Discord, and AWS. The code examples are production-ready patterns used at scale.
