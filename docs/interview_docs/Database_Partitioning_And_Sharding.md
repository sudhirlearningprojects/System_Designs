# Database Partitioning and Sharding - Deep Dive

## Overview

**Partitioning** and **Sharding** are techniques to split large databases into smaller, manageable pieces to improve performance, scalability, and manageability.

**Key Benefits**:
- Improved query performance
- Horizontal scalability
- Better manageability
- Increased availability
- Parallel processing

---

## Partitioning vs Sharding

| Aspect | Partitioning | Sharding |
|--------|-------------|----------|
| **Scope** | Single database server | Multiple database servers |
| **Location** | Same machine | Different machines |
| **Purpose** | Performance optimization | Horizontal scaling |
| **Complexity** | Low | High |
| **Cost** | Low | High |
| **Scalability** | Limited by single server | Unlimited (add more servers) |

**Visual Comparison**:
```
Partitioning (Vertical Split - Same Server):
┌─────────────────────────┐
│      Database Server    │
│  ┌────────┬────────┐   │
│  │Part 1  │Part 2  │   │
│  │Users   │Users   │   │
│  │1-1M    │1M-2M   │   │
│  └────────┴────────┘   │
└─────────────────────────┘

Sharding (Horizontal Split - Multiple Servers):
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Shard 1  │  │ Shard 2  │  │ Shard 3  │
│ Users    │  │ Users    │  │ Users    │
│ 1-1M     │  │ 1M-2M    │  │ 2M-3M    │
└──────────┘  └──────────┘  └──────────┘
```

---

## Database Partitioning

### What is Partitioning?

Splitting a large table into smaller pieces (partitions) within the same database server.

### Types of Partitioning

#### 1. Horizontal Partitioning (Row-Based)

Split table by rows based on a partition key.

**Example**: Users table partitioned by registration year

```sql
-- Original table
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    registration_date DATE
);

-- Partitioned table
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    registration_date DATE
)
PARTITION BY RANGE (YEAR(registration_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**Data Distribution**:
```
p2020: Users registered in 2020
p2021: Users registered in 2021
p2022: Users registered in 2022
p2023: Users registered in 2023
p_future: Users registered after 2023
```

**Query Optimization**:
```sql
-- Query only scans p2022 partition
SELECT * FROM users 
WHERE registration_date BETWEEN '2022-01-01' AND '2022-12-31';

-- Without partitioning: Scans entire table (10M rows)
-- With partitioning: Scans only p2022 (2M rows)
```

---

#### 2. Vertical Partitioning (Column-Based)

Split table by columns.

**Example**: Users table split into frequently and rarely accessed columns

```sql
-- Original table
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    password_hash VARCHAR(255),
    bio TEXT,
    profile_picture BLOB,
    last_login TIMESTAMP
);

-- Vertical partitioning
CREATE TABLE users_core (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    password_hash VARCHAR(255),
    last_login TIMESTAMP
);

CREATE TABLE users_profile (
    id INT PRIMARY KEY,
    bio TEXT,
    profile_picture BLOB,
    FOREIGN KEY (id) REFERENCES users_core(id)
);
```

**Benefits**:
- Faster queries on frequently accessed columns
- Reduced I/O for common queries
- Better cache utilization

---

#### 3. Range Partitioning

Partition based on value ranges.

**Example**: Orders partitioned by order date

```sql
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    customer_id INT,
    order_date DATE,
    total_amount DECIMAL(10,2)
)
PARTITION BY RANGE (YEAR(order_date)) (
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024)
);
```

**Use Cases**:
- Time-series data (logs, events, orders)
- Historical data archival
- Data retention policies

---

#### 4. List Partitioning

Partition based on discrete values.

**Example**: Users partitioned by country

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    country VARCHAR(50)
)
PARTITION BY LIST (country) (
    PARTITION p_usa VALUES IN ('USA'),
    PARTITION p_uk VALUES IN ('UK'),
    PARTITION p_india VALUES IN ('India'),
    PARTITION p_others VALUES IN (DEFAULT)
);
```

**Use Cases**:
- Geographic distribution
- Category-based data
- Status-based data

---

#### 5. Hash Partitioning

Partition based on hash function.

**Example**: Users partitioned by user_id hash

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
)
PARTITION BY HASH(id)
PARTITIONS 4;
```

**Data Distribution**:
```
Partition 0: hash(id) % 4 = 0
Partition 1: hash(id) % 4 = 1
Partition 2: hash(id) % 4 = 2
Partition 3: hash(id) % 4 = 3
```

**Benefits**:
- Even data distribution
- No hotspots
- Good for uniform access patterns

---

#### 6. Composite Partitioning

Combination of multiple partitioning strategies.

**Example**: Range + Hash partitioning

```sql
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    customer_id INT,
    order_date DATE,
    total_amount DECIMAL(10,2)
)
PARTITION BY RANGE (YEAR(order_date))
SUBPARTITION BY HASH(customer_id)
SUBPARTITIONS 4 (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024)
);
```

**Result**:
```
p2022_hash0, p2022_hash1, p2022_hash2, p2022_hash3
p2023_hash0, p2023_hash1, p2023_hash2, p2023_hash3
```

---

## Database Sharding

### What is Sharding?

Distributing data across multiple database servers (shards), where each shard is an independent database.

### Architecture

```
                Application
                     ↓
              Shard Router/Proxy
                     ↓
        ┌────────────┼────────────┐
        ↓            ↓            ↓
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │ Shard 1 │  │ Shard 2 │  │ Shard 3 │
   │ Users   │  │ Users   │  │ Users   │
   │ 1-1M    │  │ 1M-2M   │  │ 2M-3M   │
   └─────────┘  └─────────┘  └─────────┘
```

---

### Sharding Strategies

#### 1. Range-Based Sharding

Distribute data based on value ranges.

**Example**: Users sharded by user_id

```
Shard 1: user_id 1 - 1,000,000
Shard 2: user_id 1,000,001 - 2,000,000
Shard 3: user_id 2,000,001 - 3,000,000
```

**Implementation**:
```java
public class RangeSharding {
    public DataSource getShard(long userId) {
        if (userId <= 1_000_000) return shard1;
        if (userId <= 2_000_000) return shard2;
        return shard3;
    }
}
```

**Pros**:
- Simple to implement
- Easy to add new shards
- Range queries efficient

**Cons**:
- Uneven distribution (hotspots)
- Difficult to rebalance

---

#### 2. Hash-Based Sharding

Distribute data based on hash function.

**Example**: Users sharded by hash(user_id)

```
Shard = hash(user_id) % num_shards

user_id = 12345
hash(12345) = 67890
67890 % 3 = 0 → Shard 0
```

**Implementation**:
```java
public class HashSharding {
    private List<DataSource> shards;
    
    public DataSource getShard(long userId) {
        int shardIndex = (int) (userId % shards.size());
        return shards.get(shardIndex);
    }
}
```

**Pros**:
- Even distribution
- No hotspots
- Simple logic

**Cons**:
- Difficult to add/remove shards (requires rehashing)
- Range queries require querying all shards

---

#### 3. Consistent Hashing

Improved hash-based sharding that minimizes data movement when adding/removing shards.

**How it works**:
```
Hash Ring (0 to 2^32-1):

    0
    ↓
┌───────────────┐
│   Shard 1     │
│   (0-1000)    │
├───────────────┤
│   Shard 2     │
│   (1001-2000) │
├───────────────┤
│   Shard 3     │
│   (2001-2^32) │
└───────────────┘

hash(user_id) = 1500 → Shard 2
```

**Implementation**:
```java
public class ConsistentHashing {
    private TreeMap<Integer, DataSource> ring = new TreeMap<>();
    
    public void addShard(DataSource shard) {
        for (int i = 0; i < 150; i++) { // Virtual nodes
            int hash = hash(shard.toString() + i);
            ring.put(hash, shard);
        }
    }
    
    public DataSource getShard(long userId) {
        int hash = hash(String.valueOf(userId));
        Map.Entry<Integer, DataSource> entry = ring.ceilingEntry(hash);
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }
}
```

**Pros**:
- Minimal data movement when adding/removing shards
- Even distribution with virtual nodes
- Scalable

**Cons**:
- More complex implementation
- Range queries still require all shards

---

#### 4. Geographic Sharding

Distribute data based on geographic location.

**Example**: Users sharded by country

```
Shard 1 (US-East): Users from USA, Canada
Shard 2 (EU-West): Users from UK, Germany, France
Shard 3 (Asia-Pacific): Users from India, China, Japan
```

**Implementation**:
```java
public class GeoSharding {
    public DataSource getShard(String country) {
        switch (country) {
            case "USA":
            case "Canada":
                return usEastShard;
            case "UK":
            case "Germany":
            case "France":
                return euWestShard;
            default:
                return asiaPacificShard;
        }
    }
}
```

**Pros**:
- Low latency (data close to users)
- Compliance with data residency laws
- Natural data isolation

**Cons**:
- Uneven distribution
- Cross-shard queries expensive

---

#### 5. Directory-Based Sharding

Use a lookup table to map keys to shards.

**Example**: Shard mapping table

```sql
CREATE TABLE shard_mapping (
    user_id INT PRIMARY KEY,
    shard_id INT
);

-- Lookup
SELECT shard_id FROM shard_mapping WHERE user_id = 12345;
-- Result: shard_id = 2
```

**Implementation**:
```java
public class DirectorySharding {
    private Map<Long, Integer> shardMapping = new HashMap<>();
    private List<DataSource> shards;
    
    public DataSource getShard(long userId) {
        Integer shardId = shardMapping.get(userId);
        return shards.get(shardId);
    }
}
```

**Pros**:
- Flexible (can move data between shards)
- No rehashing needed
- Easy to rebalance

**Cons**:
- Extra lookup overhead
- Single point of failure (mapping table)
- Mapping table can become large

---

## Sharding Implementation Example

### Complete Sharding System

```java
// Shard configuration
public class ShardConfig {
    private String shardId;
    private String host;
    private int port;
    private String database;
}

// Shard router
public class ShardRouter {
    private List<DataSource> shards;
    private ShardingStrategy strategy;
    
    public ShardRouter(List<ShardConfig> configs, ShardingStrategy strategy) {
        this.shards = configs.stream()
            .map(this::createDataSource)
            .collect(Collectors.toList());
        this.strategy = strategy;
    }
    
    public DataSource getShard(long key) {
        return strategy.getShard(key, shards);
    }
    
    private DataSource createDataSource(ShardConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + config.getHost() + 
            ":" + config.getPort() + "/" + config.getDatabase());
        return new HikariDataSource(hikariConfig);
    }
}

// Sharding strategy interface
public interface ShardingStrategy {
    DataSource getShard(long key, List<DataSource> shards);
}

// Hash-based strategy
public class HashShardingStrategy implements ShardingStrategy {
    @Override
    public DataSource getShard(long key, List<DataSource> shards) {
        int index = (int) (key % shards.size());
        return shards.get(index);
    }
}

// Usage
public class UserService {
    private ShardRouter shardRouter;
    
    public User getUser(long userId) {
        DataSource shard = shardRouter.getShard(userId);
        try (Connection conn = shard.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM users WHERE id = ?");
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapToUser(rs);
            }
        }
        return null;
    }
    
    public void createUser(User user) {
        DataSource shard = shardRouter.getShard(user.getId());
        try (Connection conn = shard.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (id, name, email) VALUES (?, ?, ?)");
            stmt.setLong(1, user.getId());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getEmail());
            stmt.executeUpdate();
        }
    }
}
```

---

## Challenges and Solutions

### 1. Cross-Shard Queries

**Problem**: Query spans multiple shards

```sql
-- Get all users with email ending in @gmail.com
SELECT * FROM users WHERE email LIKE '%@gmail.com';
```

**Solution 1: Scatter-Gather**
```java
public List<User> searchUsers(String emailPattern) {
    List<User> results = new ArrayList<>();
    
    // Query all shards in parallel
    List<CompletableFuture<List<User>>> futures = shards.stream()
        .map(shard -> CompletableFuture.supplyAsync(() -> 
            queryShardForUsers(shard, emailPattern)))
        .collect(Collectors.toList());
    
    // Gather results
    for (CompletableFuture<List<User>> future : futures) {
        results.addAll(future.join());
    }
    
    return results;
}
```

**Solution 2: Denormalization**
```
Maintain a separate search index (Elasticsearch)
Query index instead of shards
```

---

### 2. Distributed Transactions

**Problem**: Transaction spans multiple shards

```java
// Transfer money between users on different shards
transferMoney(user1, user2, amount);
```

**Solution 1: Two-Phase Commit (2PC)**
```java
public void transferMoney(long fromUserId, long toUserId, double amount) {
    DataSource fromShard = shardRouter.getShard(fromUserId);
    DataSource toShard = shardRouter.getShard(toUserId);
    
    // Phase 1: Prepare
    Connection conn1 = fromShard.getConnection();
    Connection conn2 = toShard.getConnection();
    
    try {
        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);
        
        // Deduct from user1
        deductBalance(conn1, fromUserId, amount);
        
        // Add to user2
        addBalance(conn2, toUserId, amount);
        
        // Phase 2: Commit
        conn1.commit();
        conn2.commit();
    } catch (Exception e) {
        conn1.rollback();
        conn2.rollback();
        throw e;
    }
}
```

**Solution 2: Saga Pattern**
```java
public void transferMoney(long fromUserId, long toUserId, double amount) {
    try {
        // Step 1: Deduct from user1
        deductBalance(fromUserId, amount);
        
        // Step 2: Add to user2
        addBalance(toUserId, amount);
    } catch (Exception e) {
        // Compensating transaction
        addBalance(fromUserId, amount); // Rollback
        throw e;
    }
}
```

---

### 3. Joins Across Shards

**Problem**: Join tables on different shards

```sql
-- Users and Orders on different shards
SELECT u.name, o.order_id 
FROM users u 
JOIN orders o ON u.id = o.user_id;
```

**Solution 1: Application-Level Join**
```java
public List<UserOrder> getUserOrders() {
    // Get all users
    List<User> users = getAllUsers();
    
    // Get orders for each user
    Map<Long, List<Order>> ordersByUser = new HashMap<>();
    for (User user : users) {
        List<Order> orders = getOrdersForUser(user.getId());
        ordersByUser.put(user.getId(), orders);
    }
    
    // Join in application
    List<UserOrder> results = new ArrayList<>();
    for (User user : users) {
        List<Order> orders = ordersByUser.get(user.getId());
        for (Order order : orders) {
            results.add(new UserOrder(user, order));
        }
    }
    return results;
}
```

**Solution 2: Denormalization**
```sql
-- Store user info with order
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    user_name VARCHAR(100), -- Denormalized
    user_email VARCHAR(100), -- Denormalized
    order_date DATE
);
```

---

### 4. Rebalancing Shards

**Problem**: Add new shard, need to redistribute data

**Solution: Consistent Hashing with Virtual Nodes**
```java
public void addShard(DataSource newShard) {
    // Add virtual nodes for new shard
    for (int i = 0; i < 150; i++) {
        int hash = hash(newShard.toString() + i);
        ring.put(hash, newShard);
    }
    
    // Migrate data from adjacent shards
    migrateData(newShard);
}

private void migrateData(DataSource newShard) {
    // Only ~1/N data needs to be moved (N = number of shards)
    // Identify keys that now belong to new shard
    // Move data from old shard to new shard
}
```

---

### 5. Shard Key Selection

**Problem**: Choosing the right shard key

**Good Shard Keys**:
- High cardinality (many unique values)
- Even distribution
- Frequently used in queries
- Immutable (doesn't change)

**Examples**:
```
✅ Good: user_id, order_id, email_hash
❌ Bad: country (low cardinality), status (few values)
```

---

## Partitioning vs Sharding: When to Use

### Use Partitioning When:

✅ **Single server can handle load**
```
Data size: < 1TB
QPS: < 10,000
```

✅ **Query performance optimization**
```
Time-series data (partition by date)
Archive old data
```

✅ **Simpler management**
```
No distributed transactions
No cross-shard queries
```

---

### Use Sharding When:

✅ **Single server cannot handle load**
```
Data size: > 1TB
QPS: > 10,000
```

✅ **Horizontal scalability needed**
```
Add more servers to scale
Handle growing data
```

✅ **High availability required**
```
Shard failure doesn't affect other shards
Geographic distribution
```

---

## Best Practices

### 1. Choose Right Shard Key

```java
// Good: High cardinality, immutable
shard_key = user_id

// Bad: Low cardinality
shard_key = country (only ~200 values)

// Bad: Mutable
shard_key = email (users can change email)
```

### 2. Plan for Growth

```java
// Start with more shards than needed
// Easier to merge than split
initial_shards = expected_shards * 2
```

### 3. Monitor Shard Health

```java
// Track metrics per shard
- Query latency
- Storage usage
- Connection pool usage
- Error rate
```

### 4. Use Connection Pooling

```java
// One pool per shard
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
DataSource shard = new HikariDataSource(config);
```

### 5. Implement Retry Logic

```java
public User getUser(long userId) {
    int retries = 3;
    while (retries > 0) {
        try {
            return queryUser(userId);
        } catch (SQLException e) {
            retries--;
            if (retries == 0) throw e;
            Thread.sleep(100);
        }
    }
}
```

---

## Real-World Examples

### 1. Instagram (Hash-Based Sharding)

```
Shard key: user_id
Strategy: Consistent hashing
Shards: 1000+ PostgreSQL instances
```

### 2. Uber (Geographic Sharding)

```
Shard key: city_id
Strategy: Geographic
Shards: One per major city
```

### 3. Twitter (Range-Based Sharding)

```
Shard key: tweet_id (Snowflake ID)
Strategy: Range-based
Shards: Time-based ranges
```

### 4. Pinterest (Hash-Based Sharding)

```
Shard key: pin_id
Strategy: Hash-based
Shards: MySQL instances
```

---

## Interview Questions & Answers

### Q1: What's the difference between partitioning and sharding?

**Answer**: 
- **Partitioning**: Split data within single server
- **Sharding**: Split data across multiple servers

### Q2: When would you use range-based vs hash-based sharding?

**Answer**:
- **Range-based**: Time-series data, sequential IDs, range queries
- **Hash-based**: Even distribution, no hotspots, point queries

### Q3: How to handle cross-shard queries?

**Answer**:
- Scatter-gather (query all shards, merge results)
- Denormalization (duplicate data)
- Separate search index (Elasticsearch)

### Q4: What makes a good shard key?

**Answer**:
- High cardinality (many unique values)
- Even distribution (no hotspots)
- Immutable (doesn't change)
- Frequently used in queries

### Q5: How to add a new shard without downtime?

**Answer**:
- Use consistent hashing (minimal data movement)
- Migrate data gradually
- Use dual-write during migration
- Switch traffic after migration complete

---

## Key Takeaways

1. **Partitioning** = single server, **Sharding** = multiple servers
2. **Partitioning** for performance, **Sharding** for scalability
3. **Choose right shard key**: high cardinality, immutable, even distribution
4. **Hash-based sharding** for even distribution
5. **Consistent hashing** for easy shard addition/removal
6. **Cross-shard queries** are expensive (avoid if possible)
7. **Denormalization** helps avoid cross-shard joins
8. **Monitor shard health** and rebalance as needed
9. **Plan for growth** from the start
10. **Use connection pooling** per shard

---

## Practice Problems

1. Design sharding strategy for social media platform
2. Implement consistent hashing with virtual nodes
3. Handle cross-shard transactions with Saga pattern
4. Migrate data from 3 shards to 5 shards
5. Choose shard key for e-commerce platform
6. Implement scatter-gather for cross-shard queries
7. Design geographic sharding for global application
8. Handle shard failure and failover
9. Optimize query performance with partitioning
10. Calculate optimal number of shards for given load
