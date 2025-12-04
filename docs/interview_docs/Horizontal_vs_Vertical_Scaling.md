# Horizontal vs Vertical Scaling - Complete Guide

## Quick Summary

| Aspect | Vertical Scaling (Scale Up) | Horizontal Scaling (Scale Out) |
|--------|----------------------------|-------------------------------|
| **Definition** | Add more power to existing server | Add more servers |
| **Hardware** | Bigger CPU, RAM, Disk | More machines |
| **Cost** | Expensive (exponential) | Cost-effective (linear) |
| **Limit** | Hardware limit | Virtually unlimited |
| **Downtime** | Yes (during upgrade) | No (add nodes live) |
| **Complexity** | Simple | Complex (distributed) |
| **Example** | 8GB → 32GB RAM | 1 server → 10 servers |

---

## What is Vertical Scaling?

**Vertical Scaling (Scale Up)**: Increase the capacity of a single server by adding more resources.

### Visual Representation

```
Before:                    After:
┌─────────────┐           ┌─────────────┐
│   Server    │           │   Server    │
│  CPU: 4     │    →      │  CPU: 16    │
│  RAM: 8GB   │           │  RAM: 64GB  │
│  Disk: 500GB│           │  Disk: 2TB  │
└─────────────┘           └─────────────┘
```

### Example

**Initial Setup**:
```
Application Server:
- CPU: 4 cores
- RAM: 8 GB
- Disk: 500 GB
- Handles: 1000 requests/sec
```

**After Vertical Scaling**:
```
Application Server:
- CPU: 16 cores
- RAM: 64 GB
- Disk: 2 TB
- Handles: 4000 requests/sec
```

### Advantages

✅ **Simple**: No code changes needed  
✅ **No complexity**: Single server to manage  
✅ **Consistency**: No distributed system issues  
✅ **Quick**: Just upgrade hardware

### Disadvantages

❌ **Hardware limits**: Can't scale infinitely  
❌ **Expensive**: Cost increases exponentially  
❌ **Downtime**: Need to stop server for upgrade  
❌ **Single point of failure**: If server dies, everything stops

---

## What is Horizontal Scaling?

**Horizontal Scaling (Scale Out)**: Add more servers to distribute the load.

### Visual Representation

```
Before:                    After:
┌─────────────┐           ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Server    │           │  Server 1   │  │  Server 2   │  │  Server 3   │
│  CPU: 4     │    →      │  CPU: 4     │  │  CPU: 4     │  │  CPU: 4     │
│  RAM: 8GB   │           │  RAM: 8GB   │  │  RAM: 8GB   │  │  RAM: 8GB   │
└─────────────┘           └─────────────┘  └─────────────┘  └─────────────┘
                                 ↓               ↓               ↓
                          Load Balancer distributes traffic
```

### Example

**Initial Setup**:
```
1 Server:
- Handles: 1000 requests/sec
```

**After Horizontal Scaling**:
```
3 Servers:
- Server 1: 333 requests/sec
- Server 2: 333 requests/sec
- Server 3: 334 requests/sec
- Total: 1000 requests/sec
```

### Advantages

✅ **Unlimited scaling**: Add as many servers as needed  
✅ **Cost-effective**: Use commodity hardware  
✅ **No downtime**: Add servers without stopping  
✅ **Fault tolerance**: If one server fails, others continue  
✅ **Geographic distribution**: Servers in multiple regions

### Disadvantages

❌ **Complex**: Need load balancer, data synchronization  
❌ **Code changes**: Application must support distributed architecture  
❌ **Data consistency**: Harder to maintain  
❌ **Network overhead**: Communication between servers

---

## Cost Comparison

### Vertical Scaling Cost (Exponential)

```
Server Specs          Cost/Month
4 cores, 8GB RAM      $100
8 cores, 16GB RAM     $250
16 cores, 32GB RAM    $600
32 cores, 64GB RAM    $1,500
64 cores, 128GB RAM   $4,000
```

**Problem**: Cost increases exponentially!

---

### Horizontal Scaling Cost (Linear)

```
Number of Servers     Cost/Month
1 server (4 cores)    $100
2 servers             $200
3 servers             $300
4 servers             $400
10 servers            $1,000
```

**Benefit**: Cost increases linearly!

---

## Redis Scaling

### Redis Vertical Scaling

**Approach**: Increase RAM and CPU of single Redis instance.

**Configuration**:
```bash
# Initial Setup
redis-server --maxmemory 2gb

# After Vertical Scaling
redis-server --maxmemory 16gb
```

**Example**:
```
Before:
┌─────────────────┐
│  Redis Server   │
│  RAM: 2 GB      │
│  Keys: 1M       │
└─────────────────┘

After:
┌─────────────────┐
│  Redis Server   │
│  RAM: 16 GB     │
│  Keys: 8M       │
└─────────────────┘
```

**Advantages**:
- ✅ Simple setup
- ✅ No data sharding
- ✅ All data in one place

**Disadvantages**:
- ❌ Limited by single machine RAM
- ❌ Single point of failure
- ❌ Expensive for large datasets

**When to Use**:
- Dataset < 100 GB
- Simple caching
- Development/testing

---

### Redis Horizontal Scaling

Redis supports horizontal scaling through **Redis Cluster** and **Sharding**.

#### 1. Redis Cluster (Built-in Sharding)

**Architecture**:
```
                    Client
                      ↓
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │ Master 1│   │ Master 2│   │ Master 3│
   │ Slots:  │   │ Slots:  │   │ Slots:  │
   │ 0-5460  │   │5461-10922│  │10923-16383│
   └─────────┘   └─────────┘   └─────────┘
        ↓             ↓             ↓
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │ Replica 1│   │ Replica 2│   │ Replica 3│
   └─────────┘   └─────────┘   └─────────┘
```

**How It Works**:
1. Data divided into 16,384 hash slots
2. Each master node handles a range of slots
3. Keys hashed to determine which slot (and thus which node)
4. Replicas provide high availability

**Setup**:
```bash
# Create 6 Redis instances (3 masters + 3 replicas)
redis-server --port 7000 --cluster-enabled yes
redis-server --port 7001 --cluster-enabled yes
redis-server --port 7002 --cluster-enabled yes
redis-server --port 7003 --cluster-enabled yes
redis-server --port 7004 --cluster-enabled yes
redis-server --port 7005 --cluster-enabled yes

# Create cluster
redis-cli --cluster create \
  127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
  127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
  --cluster-replicas 1
```

**Data Distribution**:
```bash
# Key "user:1000" hashed to slot 5798
# Slot 5798 belongs to Master 2
SET user:1000 "John Doe"  # Stored on Master 2

# Key "user:2000" hashed to slot 12345
# Slot 12345 belongs to Master 3
SET user:2000 "Jane Smith"  # Stored on Master 3
```

**Hash Slot Calculation**:
```
slot = CRC16(key) mod 16384

Example:
key = "user:1000"
CRC16("user:1000") = 5798
slot = 5798 mod 16384 = 5798
```

**Client Code (Java)**:
```java
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.HostAndPort;

Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("127.0.0.1", 7000));
nodes.add(new HostAndPort("127.0.0.1", 7001));
nodes.add(new HostAndPort("127.0.0.1", 7002));

JedisCluster jedisCluster = new JedisCluster(nodes);

// Client automatically routes to correct node
jedisCluster.set("user:1000", "John Doe");
String value = jedisCluster.get("user:1000");
```

**Advantages**:
- ✅ Automatic sharding
- ✅ High availability (replicas)
- ✅ Automatic failover
- ✅ Linear scalability

**Disadvantages**:
- ❌ No multi-key operations across nodes
- ❌ More complex setup
- ❌ Network overhead

---

#### 2. Redis Sentinel (High Availability)

**Architecture**:
```
        Sentinel 1    Sentinel 2    Sentinel 3
            ↓             ↓             ↓
        ┌───────────────────────────────┐
        │      Monitor & Failover       │
        └───────────────────────────────┘
                      ↓
            ┌─────────────────┐
            │  Redis Master   │
            └─────────────────┘
                      ↓
        ┌─────────────┴─────────────┐
        ↓                           ↓
   ┌─────────┐               ┌─────────┐
   │ Replica 1│               │ Replica 2│
   └─────────┘               └─────────┘
```

**Purpose**: Automatic failover, not sharding.

**Setup**:
```bash
# sentinel.conf
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000

# Start sentinel
redis-sentinel sentinel.conf
```

---

#### 3. Client-Side Sharding

**Architecture**:
```
        Application
             ↓
    ┌────────┼────────┐
    ↓        ↓        ↓
Redis 1   Redis 2   Redis 3
(Keys     (Keys     (Keys
A-H)      I-P)      Q-Z)
```

**Code Example**:
```java
public class RedisSharding {
    private List<Jedis> redisNodes;
    
    public RedisSharding() {
        redisNodes = Arrays.asList(
            new Jedis("localhost", 6379),
            new Jedis("localhost", 6380),
            new Jedis("localhost", 6381)
        );
    }
    
    private Jedis getNode(String key) {
        int hash = key.hashCode();
        int index = Math.abs(hash) % redisNodes.size();
        return redisNodes.get(index);
    }
    
    public void set(String key, String value) {
        Jedis redis = getNode(key);
        redis.set(key, value);
    }
    
    public String get(String key) {
        Jedis redis = getNode(key);
        return redis.get(key);
    }
}
```

---

## MongoDB Horizontal Scaling (Sharding)

MongoDB uses **sharding** for horizontal scaling.

### MongoDB Sharding Architecture

```
                    Application
                         ↓
                   ┌──────────┐
                   │  mongos  │  (Query Router)
                   │  mongos  │
                   └──────────┘
                         ↓
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
   ┌─────────┐      ┌─────────┐      ┌─────────┐
   │ Shard 1 │      │ Shard 2 │      │ Shard 3 │
   │ Replica │      │ Replica │      │ Replica │
   │  Set    │      │  Set    │      │  Set    │
   └─────────┘      └─────────┘      └─────────┘
        ↑                ↑                ↑
        └────────────────┴────────────────┘
                         ↓
                ┌─────────────────┐
                │  Config Servers │  (Metadata)
                │  (Replica Set)  │
                └─────────────────┘
```

### Components

1. **Shard**: Holds subset of data (replica set)
2. **mongos**: Query router (directs queries to correct shard)
3. **Config Servers**: Store metadata and cluster configuration

---

### How MongoDB Sharding Works

#### 1. Shard Key Selection

**Shard Key**: Field used to distribute data across shards.

**Example**:
```javascript
// Shard collection by userId
sh.shardCollection("mydb.users", { userId: 1 })
```

**Data Distribution**:
```
Shard 1: userId 1-1000
Shard 2: userId 1001-2000
Shard 3: userId 2001-3000
```

---

#### 2. Sharding Strategies

**A. Range-Based Sharding**

```javascript
// Shard by userId (range)
sh.shardCollection("mydb.users", { userId: 1 })

// Data distribution:
// Shard 1: userId 1-1000
// Shard 2: userId 1001-2000
// Shard 3: userId 2001-3000
```

**Pros**: Simple, good for range queries  
**Cons**: Can cause hotspots (uneven distribution)

---

**B. Hash-Based Sharding**

```javascript
// Shard by hashed userId
sh.shardCollection("mydb.users", { userId: "hashed" })

// Data distribution:
// Shard 1: hash(userId) % 3 == 0
// Shard 2: hash(userId) % 3 == 1
// Shard 3: hash(userId) % 3 == 2
```

**Pros**: Even distribution  
**Cons**: Range queries require querying all shards

---

**C. Zone/Tag-Based Sharding**

```javascript
// Geographic sharding
sh.addShardTag("shard1", "US")
sh.addShardTag("shard2", "EU")
sh.addShardTag("shard3", "ASIA")

sh.addTagRange(
  "mydb.users",
  { country: "US" },
  { country: "US\uffff" },
  "US"
)
```

**Pros**: Data locality, compliance  
**Cons**: Complex setup

---

### MongoDB Sharding Setup

**Step 1: Start Config Servers**
```bash
# Start 3 config servers (replica set)
mongod --configsvr --replSet configRS --port 27019 --dbpath /data/config1
mongod --configsvr --replSet configRS --port 27020 --dbpath /data/config2
mongod --configsvr --replSet configRS --port 27021 --dbpath /data/config3

# Initialize config replica set
mongo --port 27019
rs.initiate({
  _id: "configRS",
  configsvr: true,
  members: [
    { _id: 0, host: "localhost:27019" },
    { _id: 1, host: "localhost:27020" },
    { _id: 2, host: "localhost:27021" }
  ]
})
```

**Step 2: Start Shards (Replica Sets)**
```bash
# Shard 1
mongod --shardsvr --replSet shard1RS --port 27017 --dbpath /data/shard1
mongod --shardsvr --replSet shard1RS --port 27018 --dbpath /data/shard1-replica

# Shard 2
mongod --shardsvr --replSet shard2RS --port 27027 --dbpath /data/shard2
mongod --shardsvr --replSet shard2RS --port 27028 --dbpath /data/shard2-replica

# Initialize shard replica sets
mongo --port 27017
rs.initiate({
  _id: "shard1RS",
  members: [
    { _id: 0, host: "localhost:27017" },
    { _id: 1, host: "localhost:27018" }
  ]
})
```

**Step 3: Start mongos (Query Router)**
```bash
mongos --configdb configRS/localhost:27019,localhost:27020,localhost:27021 --port 27016
```

**Step 4: Add Shards**
```javascript
mongo --port 27016
sh.addShard("shard1RS/localhost:27017,localhost:27018")
sh.addShard("shard2RS/localhost:27027,localhost:27028")
```

**Step 5: Enable Sharding**
```javascript
// Enable sharding on database
sh.enableSharding("mydb")

// Shard collection
sh.shardCollection("mydb.users", { userId: "hashed" })
```

---

### Query Routing

**Targeted Query** (includes shard key):
```javascript
// Query includes shard key (userId)
db.users.find({ userId: 12345 })

// mongos routes to specific shard
// Only 1 shard queried
```

**Broadcast Query** (no shard key):
```javascript
// Query doesn't include shard key
db.users.find({ email: "john@example.com" })

// mongos broadcasts to ALL shards
// All shards queried, results merged
```

---

### MongoDB Sharding Example

**Application Code**:
```java
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class MongoShardingExample {
    public static void main(String[] args) {
        // Connect to mongos (not directly to shards)
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27016");
        MongoDatabase database = mongoClient.getDatabase("mydb");
        MongoCollection<Document> collection = database.getCollection("users");
        
        // Insert data - mongos routes to correct shard
        Document user = new Document("userId", 12345)
                .append("name", "John Doe")
                .append("email", "john@example.com");
        collection.insertOne(user);
        
        // Query data - mongos routes to correct shard
        Document result = collection.find(new Document("userId", 12345)).first();
        System.out.println(result.toJson());
    }
}
```

**Behind the Scenes**:
```
1. Application → mongos (port 27016)
2. mongos calculates: hash(12345) % 2 = 1
3. mongos routes to Shard 2
4. Shard 2 returns data
5. mongos returns to application
```

---

### MongoDB Auto-Balancing

MongoDB automatically balances data across shards.

**Balancer Process**:
```
Initial State:
Shard 1: 1000 chunks
Shard 2: 500 chunks
Shard 3: 500 chunks

Balancer detects imbalance
↓
Moves chunks from Shard 1 to Shard 2/3
↓
Balanced State:
Shard 1: 667 chunks
Shard 2: 667 chunks
Shard 3: 666 chunks
```

**Configuration**:
```javascript
// Check balancer status
sh.getBalancerState()

// Stop balancer
sh.stopBalancer()

// Start balancer
sh.startBalancer()

// Set balancing window (off-peak hours)
db.settings.update(
  { _id: "balancer" },
  { $set: { activeWindow: { start: "23:00", stop: "06:00" } } },
  { upsert: true }
)
```

---

## Comparison: Redis vs MongoDB Scaling

| Aspect | Redis Cluster | MongoDB Sharding |
|--------|--------------|------------------|
| **Sharding Unit** | Hash slots (16,384) | Chunks (64MB default) |
| **Routing** | Client-side | mongos (server-side) |
| **Rebalancing** | Manual | Automatic |
| **Shard Key** | Key itself | Configurable field |
| **Multi-key Ops** | Limited | Supported |
| **Complexity** | Moderate | High |

---

## Best Practices

### Vertical Scaling

✅ **Use for**:
- Development/testing
- Small applications
- Quick fixes
- Single-tenant systems

❌ **Avoid for**:
- High-traffic applications
- Need for high availability
- Cost-sensitive projects

---

### Horizontal Scaling

✅ **Use for**:
- Production systems
- High-traffic applications
- Need for fault tolerance
- Geographic distribution

❌ **Avoid for**:
- Simple applications
- Tight budgets (initial setup)
- Limited DevOps expertise

---

## Key Takeaways

1. **Vertical Scaling**: Add more power to one server (simple but limited)
2. **Horizontal Scaling**: Add more servers (complex but unlimited)
3. **Redis Vertical**: Increase RAM (simple, limited to single machine)
4. **Redis Horizontal**: Redis Cluster with hash slots (16,384 slots)
5. **MongoDB Sharding**: Automatic with mongos router and config servers
6. **Shard Key**: Critical for MongoDB performance (choose wisely)
7. **Modern Approach**: Start vertical, scale horizontal when needed
8. **Cost**: Vertical (exponential), Horizontal (linear)

---

## Quick Decision Guide

```
Need to scale?
    ↓
Is it temporary?
├─ Yes → Vertical Scaling
└─ No → Continue
    ↓
Budget < $1000/month?
├─ Yes → Vertical Scaling
└─ No → Continue
    ↓
Need high availability?
├─ Yes → Horizontal Scaling
└─ No → Vertical Scaling
    ↓
Traffic > 10K requests/sec?
├─ Yes → Horizontal Scaling
└─ No → Vertical Scaling
```

**Bottom Line**: Start with vertical scaling for simplicity, move to horizontal scaling for unlimited growth and high availability!
