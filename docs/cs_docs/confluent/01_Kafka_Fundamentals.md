# Kafka Fundamentals - Confluent Certification Study Guide

## Core Concepts

### What is Apache Kafka?

**Distributed Event Streaming Platform** for:
- High-throughput message publishing/subscribing
- Fault-tolerant storage of event streams
- Real-time stream processing

### Key Components

**1. Producer**
- Publishes messages to topics
- Chooses partition (round-robin, key-based, custom)
- Handles retries and acknowledgments

**2. Consumer**
- Subscribes to topics
- Reads messages from partitions
- Tracks offset (position in partition)

**3. Broker**
- Kafka server that stores data
- Handles read/write requests
- Manages replication

**4. Topic**
- Category/feed name for messages
- Divided into partitions
- Immutable, append-only log

**5. Partition**
- Ordered, immutable sequence of records
- Each message has unique offset
- Unit of parallelism

**6. ZooKeeper** (being replaced by KRaft)
- Manages cluster metadata
- Leader election
- Configuration management

### Topic Architecture

```
Topic: orders
├── Partition 0: [msg0, msg1, msg2, msg3, ...]
├── Partition 1: [msg0, msg1, msg2, ...]
└── Partition 2: [msg0, msg1, msg2, msg3, msg4, ...]

Each partition is replicated across brokers
```

### Message Structure

```java
ProducerRecord<K, V> {
    String topic;           // Required
    Integer partition;      // Optional (null = auto-assign)
    Long timestamp;         // Optional (null = current time)
    K key;                  // Optional (null = no key)
    V value;                // Required
    Iterable<Header> headers; // Optional metadata
}
```

### Offsets

**Definition**: Sequential ID assigned to each message in partition

**Types**:
- **Current Offset**: Last read position
- **Committed Offset**: Last processed position (saved)
- **Log-End Offset**: Latest message in partition

**Offset Management**:
```java
// Auto-commit (default)
enable.auto.commit=true
auto.commit.interval.ms=5000

// Manual commit
consumer.commitSync();  // Blocking
consumer.commitAsync(); // Non-blocking
```

### Replication

**Purpose**: Fault tolerance and high availability

**Concepts**:
- **Replication Factor**: Number of copies (typically 3)
- **Leader**: Handles all reads/writes for partition
- **Follower (Replica)**: Copies data from leader
- **ISR (In-Sync Replicas)**: Replicas caught up with leader

```
Partition 0 (RF=3)
├── Leader: Broker 1 (handles all I/O)
├── Follower: Broker 2 (syncs from leader)
└── Follower: Broker 3 (syncs from leader)
```

### Guarantees

**Ordering**:
- Messages within same partition are ordered
- No ordering guarantee across partitions

**Durability**:
- Messages persisted to disk
- Configurable retention (time/size based)

**Delivery Semantics**:
- **At-most-once**: May lose messages (acks=0)
- **At-least-once**: May duplicate (acks=1 or all)
- **Exactly-once**: No loss, no duplicates (idempotence + transactions)

## Exam Focus Areas

### Topic Configuration

```properties
# Retention
retention.ms=604800000              # 7 days
retention.bytes=1073741824          # 1 GB per partition

# Cleanup policy
cleanup.policy=delete               # Delete old segments
cleanup.policy=compact              # Keep latest per key

# Segment
segment.ms=604800000                # Roll new segment after 7 days
segment.bytes=1073741824            # Roll at 1 GB

# Replication
min.insync.replicas=2               # Min ISR for writes
unclean.leader.election.enable=false # Don't elect out-of-sync replica
```

### Partitioning Strategy

**Default (Round-Robin)**:
```java
producer.send(new ProducerRecord<>("topic", value));
```

**Key-Based**:
```java
// Same key → same partition
producer.send(new ProducerRecord<>("topic", key, value));
// Hash(key) % num_partitions
```

**Custom Partitioner**:
```java
public class CustomPartitioner implements Partitioner {
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        // Custom logic
        return partitionNumber;
    }
}
```

### Consumer Groups

**Concept**: Group of consumers sharing workload

**Rules**:
- Each partition assigned to ONE consumer in group
- Consumer can read from MULTIPLE partitions
- Rebalancing occurs when consumers join/leave

```
Topic: orders (3 partitions)
Consumer Group: order-processors

Consumer 1: Partition 0, 1
Consumer 2: Partition 2

If Consumer 3 joins:
Consumer 1: Partition 0
Consumer 2: Partition 1
Consumer 3: Partition 2
```

### Rebalancing

**Triggers**:
- Consumer joins/leaves group
- Consumer crashes (heartbeat timeout)
- Partition count changes

**Strategies**:
- **Range**: Assign consecutive partitions
- **RoundRobin**: Distribute evenly
- **Sticky**: Minimize movement
- **CooperativeSticky**: Incremental rebalancing

**Configuration**:
```properties
session.timeout.ms=45000           # Max time without heartbeat
heartbeat.interval.ms=3000         # Heartbeat frequency
max.poll.interval.ms=300000        # Max time between polls
```

## Practice Questions

**Q1**: If a topic has 5 partitions and replication factor 3, how many total partition replicas exist?
**A**: 15 (5 partitions × 3 replicas each)

**Q2**: What happens if min.insync.replicas=2 and only 1 replica is in-sync?
**A**: Producer with acks=all will receive NotEnoughReplicasException

**Q3**: Can two consumers in same group read from same partition simultaneously?
**A**: No, each partition assigned to only one consumer per group

**Q4**: What determines which partition a message goes to when key is null?
**A**: Round-robin or sticky partitioning (depending on configuration)

**Q5**: What is the difference between committed offset and current offset?
**A**: Current offset is last read position; committed offset is last processed position saved to Kafka
