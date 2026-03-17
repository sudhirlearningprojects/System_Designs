# Distributed Systems Design Problems

Design problems focused on distributed computing concepts, scalability, and fault tolerance.

## 📚 Problems List

### Core Distributed Concepts
1. **Design Distributed ID Generator** ⭐⭐⭐
   - Snowflake algorithm
   - UUID generation
   - Database sequences
   - **Difficulty**: Hard
   - **Key Concepts**: Uniqueness, ordering, scalability

2. **Design Consistent Hashing** ⭐⭐⭐
   - Hash ring implementation
   - Virtual nodes
   - Load balancing
   - **Difficulty**: Hard
   - **Key Concepts**: Distribution, replication, fault tolerance

3. **Design Distributed Lock** ⭐⭐⭐
   - Lease-based locking
   - Redlock algorithm
   - Fencing tokens
   - **Difficulty**: Hard
   - **Key Concepts**: Mutual exclusion, deadlock prevention

4. **Design Distributed Cache** ⭐⭐⭐
   - Cache invalidation
   - Consistency models
   - Sharding strategies
   - **Difficulty**: Hard

5. **Design Leader Election** ⭐⭐⭐
   - Raft consensus
   - Paxos algorithm
   - ZooKeeper integration
   - **Difficulty**: Hard

## 🎯 Problem 1: Distributed ID Generator

### Approach 1: Snowflake Algorithm (Twitter)

```java
class SnowflakeIdGenerator {
    // 64-bit ID structure:
    // 1 bit: unused (always 0)
    // 41 bits: timestamp (milliseconds since epoch)
    // 10 bits: machine ID (1024 machines)
    // 12 bits: sequence number (4096 IDs per ms per machine)
    
    private final long epoch = 1609459200000L; // 2021-01-01
    private final long machineIdBits = 10L;
    private final long sequenceBits = 12L;
    
    private final long maxMachineId = ~(-1L << machineIdBits);
    private final long maxSequence = ~(-1L << sequenceBits);
    
    private final long machineIdShift = sequenceBits;
    private final long timestampShift = sequenceBits + machineIdBits;
    
    private long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator(long machineId) {
        if (machineId > maxMachineId || machineId < 0) {
            throw new IllegalArgumentException("Machine ID out of range");
        }
        this.machineId = machineId;
    }
    
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                // Sequence exhausted, wait for next millisecond
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - epoch) << timestampShift) |
               (machineId << machineIdShift) |
               sequence;
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
```

**Pros**: 
- Sortable by time
- High throughput (4M IDs/sec per machine)
- No coordination needed

**Cons**:
- Clock synchronization required
- Machine ID management

### Approach 2: UUID (Universally Unique Identifier)

```java
class UUIDGenerator {
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
```

**Pros**: Simple, no coordination
**Cons**: Not sortable, 128 bits (larger)

### Approach 3: Database Auto-Increment with Sharding

```java
class DatabaseIdGenerator {
    private final int shardId;
    private final int totalShards;
    private long currentId;
    
    public DatabaseIdGenerator(int shardId, int totalShards) {
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.currentId = shardId;
    }
    
    public synchronized long nextId() {
        long id = currentId;
        currentId += totalShards;
        return id;
    }
}
```

**Pros**: Simple, guaranteed unique
**Cons**: Database bottleneck, not sortable by time

## 🎯 Problem 2: Consistent Hashing

### Implementation

```java
class ConsistentHash {
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;
    private final MessageDigest md;
    
    public ConsistentHash(int virtualNodes) {
        this.virtualNodes = virtualNodes;
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.put(hash, node);
        }
    }
    
    public void removeNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            long hash = hash(node + "#" + i);
            ring.remove(hash);
        }
    }
    
    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        
        long hash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);
        
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    private long hash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        
        long hash = 0;
        for (int i = 0; i < 8; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }
        return hash;
    }
}
```

**Use Cases**:
- Distributed caching (Memcached, Redis)
- Load balancing
- Database sharding
- CDN routing

## 🎯 Problem 3: Distributed Lock

### Approach 1: Redis-based Lock

```java
class RedisDistributedLock {
    private final Jedis redis;
    private final String lockKey;
    private final String lockValue;
    private final int ttlSeconds;
    
    public RedisDistributedLock(Jedis redis, String resource, int ttlSeconds) {
        this.redis = redis;
        this.lockKey = "lock:" + resource;
        this.lockValue = UUID.randomUUID().toString();
        this.ttlSeconds = ttlSeconds;
    }
    
    public boolean tryLock() {
        String result = redis.set(lockKey, lockValue, 
                                  SetParams.setParams().nx().ex(ttlSeconds));
        return "OK".equals(result);
    }
    
    public void unlock() {
        // Lua script for atomic check-and-delete
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        redis.eval(script, Collections.singletonList(lockKey), 
                   Collections.singletonList(lockValue));
    }
    
    public boolean tryLockWithRetry(int maxRetries, long retryDelayMs) {
        for (int i = 0; i < maxRetries; i++) {
            if (tryLock()) {
                return true;
            }
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
```

### Approach 2: ZooKeeper-based Lock

```java
class ZooKeeperDistributedLock {
    private final ZooKeeper zk;
    private final String lockPath;
    private String myNode;
    
    public boolean tryLock() throws Exception {
        // Create ephemeral sequential node
        myNode = zk.create(lockPath + "/lock-", 
                          new byte[0], 
                          ZooDefs.Ids.OPEN_ACL_UNSAFE,
                          CreateMode.EPHEMERAL_SEQUENTIAL);
        
        // Get all children
        List<String> children = zk.getChildren(lockPath, false);
        Collections.sort(children);
        
        // Check if we have the smallest node
        String smallest = lockPath + "/" + children.get(0);
        return myNode.equals(smallest);
    }
    
    public void unlock() throws Exception {
        if (myNode != null) {
            zk.delete(myNode, -1);
        }
    }
}
```

## 📊 Comparison Table

### ID Generation

| Approach | Sortable | Throughput | Coordination | Size |
|----------|----------|------------|--------------|------|
| Snowflake | ✅ Yes | Very High | None | 64 bits |
| UUID | ❌ No | High | None | 128 bits |
| DB Sequence | ❌ No | Low | Required | 64 bits |

### Distributed Lock

| Approach | Fault Tolerance | Performance | Complexity |
|----------|----------------|-------------|------------|
| Redis | Medium | High | Low |
| ZooKeeper | High | Medium | High |
| Database | Low | Low | Low |

## 🔑 Key Concepts

### CAP Theorem
```
Consistency: All nodes see same data
Availability: System always responds
Partition Tolerance: Works despite network failures

Can only guarantee 2 out of 3!
```

### Consistency Models
```
Strong Consistency: All reads see latest write
Eventual Consistency: Reads eventually see latest write
Causal Consistency: Related operations ordered
```

### Replication Strategies
```
Master-Slave: One writer, multiple readers
Master-Master: Multiple writers
Quorum: Majority agreement
```

## 💡 Real-World Examples

### Twitter Snowflake
- 41 bits: timestamp
- 10 bits: machine ID
- 12 bits: sequence
- Generates 4M IDs/sec per machine

### Amazon DynamoDB
- Consistent hashing for data distribution
- Virtual nodes for load balancing
- Eventual consistency by default

### Google Chubby
- Distributed lock service
- Used by BigTable, GFS
- Paxos-based consensus

### Redis Cluster
- Consistent hashing with 16384 slots
- Master-slave replication
- Automatic failover

## 🚀 Design Patterns

### Pattern 1: Lease-Based Coordination
```java
class Lease {
    private final long expiryTime;
    private final String owner;
    
    public boolean isValid() {
        return System.currentTimeMillis() < expiryTime;
    }
}
```

### Pattern 2: Heartbeat Mechanism
```java
class HeartbeatMonitor {
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    
    public void recordHeartbeat(String nodeId) {
        lastHeartbeat.put(nodeId, System.currentTimeMillis());
    }
    
    public boolean isAlive(String nodeId, long timeoutMs) {
        Long last = lastHeartbeat.get(nodeId);
        return last != null && 
               System.currentTimeMillis() - last < timeoutMs;
    }
}
```

### Pattern 3: Fencing Tokens
```java
class FencingToken {
    private final long tokenId;
    
    public boolean isNewer(FencingToken other) {
        return this.tokenId > other.tokenId;
    }
}
```

## 📈 Scaling Considerations

### Horizontal Scaling
- Add more machines
- Partition data
- Use consistent hashing

### Vertical Scaling
- Bigger machines
- More CPU/RAM
- Limited by hardware

### Trade-offs
- Consistency vs Availability
- Latency vs Throughput
- Simplicity vs Fault Tolerance

## 🔗 Related Topics

- [Design Rate Limiter](../05-rate-limiting/)
- [LRU Cache](../01-data-structure-design/lru-cache.md)
- [Design Twitter](../03-system-design/design-twitter.md)

## 📚 Additional Resources

### Books
- "Designing Data-Intensive Applications" - Martin Kleppmann
- "Distributed Systems" - Maarten van Steen
- "Database Internals" - Alex Petrov

### Papers
- [Dynamo: Amazon's Highly Available Key-value Store](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)
- [The Chubby Lock Service](https://research.google/pubs/pub27897/)
- [Raft Consensus Algorithm](https://raft.github.io/raft.pdf)

### Tools
- Apache ZooKeeper
- etcd
- Consul
- Redis

---

**Status**: 0/5 problems completed (concepts documented)
**Next**: Implement full solutions for each problem
**Difficulty**: Hard (requires distributed systems knowledge)
