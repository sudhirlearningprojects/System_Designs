# Kafka Message Ordering & Partitioning

## 🎯 Key Concepts

**Kafka Ordering Guarantee**: Messages are ordered **within a partition**, not across partitions.

```
Topic: orders (3 partitions)
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Partition 0 │  │ Partition 1 │  │ Partition 2 │
│ [M1, M2, M3]│  │ [M4, M5, M6]│  │ [M7, M8, M9]│
└─────────────┘  └─────────────┘  └─────────────┘
   ✅ Ordered      ✅ Ordered      ✅ Ordered
   
❌ No ordering guarantee between partitions
```

---

## 1. Partitioning Strategies

### Strategy 1: Key-Based Partitioning (Most Common)

```java
// Same key → Same partition → Guaranteed ordering
ProducerRecord<String, String> record1 = new ProducerRecord<>(
    "orders", "user123", "order-1"
);
ProducerRecord<String, String> record2 = new ProducerRecord<>(
    "orders", "user123", "order-2"  // Same key, same partition
);

// Result: Both messages go to same partition, order preserved
```

**Use Cases:**
- User events (all events for user123 stay ordered)
- Account transactions (all transactions for account456 stay ordered)
- Device telemetry (all data from device789 stays ordered)

### Strategy 2: Single Partition (Global Ordering)

```java
// Force all messages to partition 0
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",
    0,           // Explicit partition
    "user123",
    "order-data"
);
```

**Pros**: Global ordering
**Cons**: No parallelism, limited throughput

### Strategy 3: Custom Partitioner

```java
public class OrderPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        String orderKey = (String) key;
        
        // VIP customers → Partition 0 (dedicated processing)
        if (orderKey.startsWith("VIP")) {
            return 0;
        }
        
        // Regular customers → Hash-based
        return Math.abs(orderKey.hashCode()) % cluster.partitionCountForTopic(topic);
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

## 2. Real-World Scenarios

### Scenario 1: E-commerce Order Processing

```java
public class OrderProducer {
    private final KafkaProducer<String, String> producer;
    
    public void sendOrder(Order order) {
        // Key: customerId ensures all orders for same customer are ordered
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "orders",
            order.getCustomerId(),  // Partition key
            order.toJson()
        );
        
        producer.send(record);
    }
}

// Consumer processes orders for each customer in sequence
public class OrderConsumer {
    public void processOrders() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            for (ConsumerRecord<String, String> record : records) {
                // Orders for same customer processed in order
                processOrder(record.key(), record.value());
            }
        }
    }
}
```

### Scenario 2: Financial Transactions

```java
public class TransactionProducer {
    
    public void sendTransaction(Transaction txn) {
        // Key: accountId ensures all transactions for account are ordered
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "transactions",
            txn.getAccountId(),     // Critical for balance calculations
            txn.toJson()
        );
        
        producer.send(record);
    }
}
```

### Scenario 3: IoT Device Data

```java
public class IoTDataProducer {
    
    public void sendSensorData(SensorReading reading) {
        // Key: deviceId ensures chronological order per device
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "sensor-data",
            reading.getDeviceId(),  // Partition by device
            reading.toJson()
        );
        
        producer.send(record);
    }
}
```

---

## 3. Handling Cross-Partition Ordering

### Problem: Global Ordering Across Partitions

```java
// Messages sent to different partitions
producer.send(new ProducerRecord<>("events", "user1", "event1")); // → Partition 0
producer.send(new ProducerRecord<>("events", "user2", "event2")); // → Partition 1
producer.send(new ProducerRecord<>("events", "user3", "event3")); // → Partition 2

// No guarantee that event1 is processed before event2 or event3
```

### Solution 1: Timestamp-Based Reordering

```java
public class OrderingConsumer {
    private final TreeMap<Long, ConsumerRecord<String, String>> buffer = new TreeMap<>();
    private final long maxDelay = 5000; // 5 seconds
    
    public void processRecords(ConsumerRecords<String, String> records) {
        // Buffer messages by timestamp
        for (ConsumerRecord<String, String> record : records) {
            buffer.put(record.timestamp(), record);
        }
        
        // Process messages in timestamp order
        long cutoff = System.currentTimeMillis() - maxDelay;
        Iterator<Map.Entry<Long, ConsumerRecord<String, String>>> it = buffer.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<Long, ConsumerRecord<String, String>> entry = it.next();
            if (entry.getKey() < cutoff) {
                processInOrder(entry.getValue());
                it.remove();
            } else {
                break; // Wait for more messages
            }
        }
    }
}
```

### Solution 2: Sequence Numbers

```java
public class SequencedMessage {
    private long sequenceNumber;
    private String payload;
    
    // Producer assigns sequence numbers
    public void sendSequencedMessage(String message) {
        SequencedMessage seqMsg = new SequencedMessage(
            getNextSequenceNumber(), 
            message
        );
        
        producer.send(new ProducerRecord<>("events", null, seqMsg.toJson()));
    }
}

public class SequenceConsumer {
    private long expectedSequence = 0;
    private final Map<Long, String> outOfOrderBuffer = new HashMap<>();
    
    public void processMessage(SequencedMessage message) {
        if (message.getSequenceNumber() == expectedSequence) {
            // Process in-order message
            processInOrder(message);
            expectedSequence++;
            
            // Check buffer for next messages
            while (outOfOrderBuffer.containsKey(expectedSequence)) {
                processInOrder(outOfOrderBuffer.remove(expectedSequence));
                expectedSequence++;
            }
        } else {
            // Buffer out-of-order message
            outOfOrderBuffer.put(message.getSequenceNumber(), message.getPayload());
        }
    }
}
```

---

## 4. Partition Assignment Strategies

### Round Robin Assignment

```java
Properties props = new Properties();
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");

// Distributes partitions evenly across consumers
// Consumer1: [P0, P3, P6]
// Consumer2: [P1, P4, P7]  
// Consumer3: [P2, P5, P8]
```

### Range Assignment (Default)

```java
Properties props = new Properties();
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RangeAssignor");

// Assigns contiguous ranges to consumers
// Consumer1: [P0, P1, P2]
// Consumer2: [P3, P4, P5]
// Consumer3: [P6, P7, P8]
```

### Sticky Assignment

```java
Properties props = new Properties();
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.StickyAssignor");

// Minimizes partition movement during rebalancing
// Maintains existing assignments when possible
```

---

## 5. Best Practices

### 1. Choose Partition Key Carefully

```java
// ✅ Good: Ensures related messages stay together
producer.send(new ProducerRecord<>("orders", customerId, orderData));

// ❌ Bad: Random distribution, no ordering guarantee
producer.send(new ProducerRecord<>("orders", UUID.randomUUID().toString(), orderData));
```

### 2. Handle Hot Partitions

```java
public class LoadBalancedPartitioner implements Partitioner {
    private final AtomicLong counter = new AtomicLong(0);
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Detect hot keys
        if (isHotKey(key)) {
            // Distribute hot keys across multiple partitions
            return (int) (counter.getAndIncrement() % numPartitions);
        }
        
        // Regular hash-based partitioning
        return Math.abs(key.hashCode()) % numPartitions;
    }
}
```

### 3. Monitor Partition Skew

```java
public class PartitionMonitor {
    
    public void checkPartitionSkew(String topic) {
        AdminClient admin = AdminClient.create(props);
        
        // Get partition sizes
        Map<TopicPartition, Long> partitionSizes = getPartitionSizes(admin, topic);
        
        // Calculate skew
        long maxSize = partitionSizes.values().stream().mapToLong(Long::longValue).max().orElse(0);
        long minSize = partitionSizes.values().stream().mapToLong(Long::longValue).min().orElse(0);
        
        double skewRatio = (double) maxSize / minSize;
        
        if (skewRatio > 2.0) {
            System.out.println("WARNING: High partition skew detected: " + skewRatio);
        }
    }
}
```

### 4. Optimize Partition Count

```bash
# Calculate optimal partition count
# Partitions = max(target_throughput / partition_throughput, target_parallelism)

# Example: 
# Target throughput: 1000 msg/sec
# Per-partition throughput: 100 msg/sec
# Target parallelism: 10 consumers
# Optimal partitions: max(1000/100, 10) = 10 partitions

kafka-topics.sh --create --topic orders \
  --bootstrap-server localhost:9092 \
  --partitions 10 --replication-factor 3
```

---

## 6. Common Pitfalls

### Pitfall 1: Null Keys

```java
// ❌ Null key → Round-robin distribution → No ordering
producer.send(new ProducerRecord<>("orders", null, orderData));

// ✅ Use meaningful key for ordering
producer.send(new ProducerRecord<>("orders", customerId, orderData));
```

### Pitfall 2: Changing Partition Count

```java
// ❌ Adding partitions breaks key-based routing
// Messages with same key may go to different partitions

// Original: hash(key) % 3 = partition
// After adding partition: hash(key) % 4 = different partition

// ✅ Plan partition count carefully from the start
```

### Pitfall 3: Consumer Lag

```java
// Monitor consumer lag to ensure ordering isn't affected by slow processing
public void checkConsumerLag() {
    AdminClient admin = AdminClient.create(props);
    
    ListConsumerGroupOffsetsResult result = admin.listConsumerGroupOffsets("my-group");
    Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
    
    for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
        long lag = calculateLag(entry.getKey(), entry.getValue());
        if (lag > 1000) {
            System.out.println("High lag detected: " + lag);
        }
    }
}
```

---

## Quick Reference

### Ordering Guarantees
- ✅ **Within partition**: Strict ordering
- ❌ **Across partitions**: No ordering guarantee
- ✅ **Same key**: Goes to same partition (ordered)
- ❌ **Null key**: Round-robin (no ordering)

### Partitioning Decision Tree
```
Need global ordering?
├─ Yes → Single partition (limited throughput)
└─ No → Multiple partitions
   ├─ Need entity-level ordering? → Key-based partitioning
   └─ No ordering needed? → Round-robin/Random
```

### Key Configurations
```java
// Producer
props.put("partitioner.class", "custom.partitioner.class");
props.put("max.in.flight.requests.per.connection", 1); // For ordering

// Consumer  
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.StickyAssignor");
props.put("max.poll.records", 100); // Smaller batches for better ordering
```