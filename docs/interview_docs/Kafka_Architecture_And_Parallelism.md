# Apache Kafka Architecture and Parallelism - Deep Dive

## Overview

Apache Kafka is a distributed streaming platform designed for high-throughput, fault-tolerant, real-time data pipelines and streaming applications.

**Key Features**:
- High throughput (millions of messages/sec)
- Low latency (<10ms)
- Fault tolerance and durability
- Horizontal scalability
- Distributed architecture

---

## Kafka Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────┐
│                    Kafka Cluster                        │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Broker 1 │  │ Broker 2 │  │ Broker 3 │             │
│  │          │  │          │  │          │             │
│  │ Topic A  │  │ Topic A  │  │ Topic B  │             │
│  │ Part 0   │  │ Part 1   │  │ Part 0   │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                          │
└─────────────────────────────────────────────────────────┘
         ↑                                    ↓
    ┌─────────┐                          ┌──────────┐
    │Producer │                          │Consumer  │
    └─────────┘                          └──────────┘
         ↑                                    ↓
    ┌─────────────────────────────────────────────┐
    │           ZooKeeper (Coordination)          │
    └─────────────────────────────────────────────┘
```

---

## 1. Broker

**What**: Kafka server that stores and serves data

**Responsibilities**:
- Store messages in partitions
- Handle producer writes
- Serve consumer reads
- Replicate data to other brokers
- Manage partition leadership

**Example Configuration**:
```properties
# server.properties
broker.id=1
listeners=PLAINTEXT://localhost:9092
log.dirs=/var/kafka-logs
num.partitions=3
default.replication.factor=2
```

---

## 2. Topic

**What**: Logical channel for messages (like a database table)

**Characteristics**:
- Named feed of messages
- Split into partitions
- Immutable log
- Configurable retention

**Example**:
```
Topic: "orders"
Partitions: 3
Replication Factor: 2

orders-0: [msg1, msg2, msg3, msg4]
orders-1: [msg5, msg6, msg7, msg8]
orders-2: [msg9, msg10, msg11, msg12]
```

**Create Topic**:
```bash
kafka-topics.sh --create \
  --topic orders \
  --partitions 3 \
  --replication-factor 2 \
  --bootstrap-server localhost:9092
```

---

## 3. Partition

**What**: Ordered, immutable sequence of messages within a topic

**Key Concepts**:
- Each partition is an ordered log
- Messages have sequential offset
- Partitions enable parallelism
- Each partition has one leader and multiple replicas

**Partition Structure**:
```
Partition 0:
Offset: 0    1    2    3    4    5
Data:  [M1] [M2] [M3] [M4] [M5] [M6]
        ↑                        ↑
    Oldest                   Newest
```

**Why Partitions?**
- **Scalability**: Distribute data across brokers
- **Parallelism**: Multiple consumers read simultaneously
- **Throughput**: Parallel writes and reads

---

## 4. Producer

**What**: Client that publishes messages to topics

**Key Features**:
- Chooses which partition to send message
- Batching for efficiency
- Compression support
- Acknowledgment levels

**Producer Code**:
```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("acks", "all");
props.put("retries", 3);

KafkaProducer<String, String> producer = new KafkaProducer<>(props);

// Send message
ProducerRecord<String, String> record = 
    new ProducerRecord<>("orders", "order-123", "Order data");

producer.send(record, (metadata, exception) -> {
    if (exception == null) {
        System.out.println("Sent to partition: " + metadata.partition() + 
            ", offset: " + metadata.offset());
    }
});

producer.close();
```

---

## 5. Consumer

**What**: Client that reads messages from topics

**Key Features**:
- Subscribes to topics
- Tracks offset (position in partition)
- Part of consumer group
- Automatic or manual offset commit

**Consumer Code**:
```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "order-processors");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("enable.auto.commit", "true");
props.put("auto.offset.reset", "earliest");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        System.out.printf("Partition: %d, Offset: %d, Key: %s, Value: %s%n",
            record.partition(), record.offset(), record.key(), record.value());
    }
}
```

---

## 6. Consumer Group

**What**: Group of consumers that work together to consume a topic

**Key Concepts**:
- Each partition assigned to one consumer in group
- Enables parallel processing
- Automatic rebalancing
- Fault tolerance

**Example**:
```
Topic: orders (3 partitions)
Consumer Group: order-processors (3 consumers)

Partition 0 → Consumer 1
Partition 1 → Consumer 2
Partition 2 → Consumer 3
```

---

## 7. ZooKeeper (Being Replaced by KRaft)

**What**: Coordination service for Kafka cluster

**Responsibilities**:
- Broker discovery
- Leader election
- Configuration management
- Consumer group coordination (legacy)

**Note**: Kafka 3.0+ uses KRaft (Kafka Raft) instead of ZooKeeper

---

## Kafka Parallelism

### How Kafka Achieves Parallelism

#### 1. Partition-Level Parallelism

**Concept**: Each partition can be processed independently

```
Topic: orders (6 partitions)

Producer Side (Parallel Writes):
Producer 1 → Partition 0, 1
Producer 2 → Partition 2, 3
Producer 3 → Partition 4, 5

Consumer Side (Parallel Reads):
Consumer 1 → Partition 0, 1
Consumer 2 → Partition 2, 3
Consumer 3 → Partition 4, 5
```

**Key Rule**: Number of consumers ≤ Number of partitions

---

#### 2. Producer Parallelism

**Multiple Producers**:
```java
// Producer 1
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    int orderId = i;
    executor.submit(() -> {
        ProducerRecord<String, String> record = 
            new ProducerRecord<>("orders", "order-" + orderId, "data");
        producer.send(record);
    });
}
```

**Partition Assignment**:
```java
// Option 1: Round-robin (no key)
producer.send(new ProducerRecord<>("orders", "data"));

// Option 2: Hash-based (with key)
producer.send(new ProducerRecord<>("orders", "user-123", "data"));
// partition = hash(key) % num_partitions

// Option 3: Custom partitioner
producer.send(new ProducerRecord<>("orders", 2, "key", "data"));
// Explicitly send to partition 2
```

---

#### 3. Consumer Parallelism

**Consumer Group Parallelism**:

```
Scenario 1: 3 Partitions, 3 Consumers (Optimal)
┌──────────┐  ┌──────────┐  ┌──────────┐
│Consumer 1│  │Consumer 2│  │Consumer 3│
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │
     ↓             ↓             ↓
  Part 0        Part 1        Part 2

Scenario 2: 3 Partitions, 2 Consumers
┌──────────┐  ┌──────────┐
│Consumer 1│  │Consumer 2│
└────┬─────┘  └────┬─────┘
     │             │
     ↓             ↓
  Part 0        Part 1
  Part 2

Scenario 3: 3 Partitions, 4 Consumers (Idle Consumer)
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│Consumer 1│  │Consumer 2│  │Consumer 3│  │Consumer 4│
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │
     ↓             ↓             ↓             ↓
  Part 0        Part 1        Part 2        (Idle)
```

**Consumer Group Code**:
```java
// Consumer 1
Properties props1 = new Properties();
props1.put("group.id", "order-processors");
KafkaConsumer<String, String> consumer1 = new KafkaConsumer<>(props1);
consumer1.subscribe(Arrays.asList("orders"));

// Consumer 2 (same group)
Properties props2 = new Properties();
props2.put("group.id", "order-processors");
KafkaConsumer<String, String> consumer2 = new KafkaConsumer<>(props2);
consumer2.subscribe(Arrays.asList("orders"));

// Consumer 3 (same group)
Properties props3 = new Properties();
props3.put("group.id", "order-processors");
KafkaConsumer<String, String> consumer3 = new KafkaConsumer<>(props3);
consumer3.subscribe(Arrays.asList("orders"));

// Each consumer processes different partitions in parallel
```

---

#### 4. Multi-Threading Within Consumer

**Single Consumer, Multiple Threads**:
```java
public class MultiThreadedConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final ExecutorService executor;
    
    public MultiThreadedConsumer() {
        Properties props = new Properties();
        props.put("group.id", "order-processors");
        this.consumer = new KafkaConsumer<>(props);
        this.executor = Executors.newFixedThreadPool(10);
    }
    
    public void consume() {
        consumer.subscribe(Arrays.asList("orders"));
        
        while (true) {
            ConsumerRecords<String, String> records = 
                consumer.poll(Duration.ofMillis(100));
            
            // Process records in parallel
            for (ConsumerRecord<String, String> record : records) {
                executor.submit(() -> processRecord(record));
            }
            
            // Commit offsets after processing
            consumer.commitSync();
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record) {
        // Process message
        System.out.println("Processing: " + record.value());
    }
}
```

**Warning**: Be careful with offset commits when using multi-threading!

---

#### 5. Broker-Level Parallelism

**Multiple Brokers**:
```
Cluster: 3 Brokers
Topic: orders (6 partitions, replication factor 2)

Broker 1: Partition 0 (Leader), Partition 1 (Follower)
Broker 2: Partition 1 (Leader), Partition 2 (Leader)
Broker 3: Partition 0 (Follower), Partition 2 (Follower)

Parallel Processing:
- Broker 1 handles writes to Partition 0
- Broker 2 handles writes to Partition 1, 2
- All brokers serve reads
```

---

## Partition Assignment Strategies

### 1. Range Assignor (Default)

```
Topic: orders (6 partitions)
Consumers: 3

Consumer 1: Partitions 0, 1
Consumer 2: Partitions 2, 3
Consumer 3: Partitions 4, 5
```

### 2. Round Robin Assignor

```
Topic: orders (6 partitions)
Consumers: 3

Consumer 1: Partitions 0, 3
Consumer 2: Partitions 1, 4
Consumer 3: Partitions 2, 5
```

### 3. Sticky Assignor

Minimizes partition movement during rebalancing.

### 4. Cooperative Sticky Assignor

Allows incremental rebalancing without stopping all consumers.

**Configuration**:
```java
props.put("partition.assignment.strategy", 
    "org.apache.kafka.clients.consumer.RangeAssignor");
```

---

## Replication and Fault Tolerance

### Leader-Follower Replication

```
Topic: orders, Partition 0, Replication Factor: 3

Broker 1 (Leader):   [M1, M2, M3, M4, M5]
Broker 2 (Follower): [M1, M2, M3, M4, M5]
Broker 3 (Follower): [M1, M2, M3, M4, M5]

Producers write to Leader
Consumers read from Leader (by default)
Followers replicate from Leader
```

**ISR (In-Sync Replicas)**:
```
ISR = {Broker 1, Broker 2, Broker 3}

If Broker 3 lags behind:
ISR = {Broker 1, Broker 2}

If Broker 1 (Leader) fails:
New Leader elected from ISR (Broker 2 or Broker 3)
```

---

## Message Ordering Guarantees

### 1. Within Partition

**Guaranteed**: Messages in same partition are ordered

```
Partition 0:
Offset 0: Message A
Offset 1: Message B
Offset 2: Message C

Consumer reads: A → B → C (always in order)
```

### 2. Across Partitions

**Not Guaranteed**: No ordering across partitions

```
Partition 0: [A, B, C]
Partition 1: [D, E, F]

Consumer may read: A, D, B, E, C, F (any order)
```

### 3. Key-Based Ordering

**Solution**: Use same key for related messages

```java
// All orders for user-123 go to same partition
producer.send(new ProducerRecord<>("orders", "user-123", "order-1"));
producer.send(new ProducerRecord<>("orders", "user-123", "order-2"));
producer.send(new ProducerRecord<>("orders", "user-123", "order-3"));

// Guaranteed order: order-1 → order-2 → order-3
```

---

## Performance Optimization

### 1. Increase Partitions

```bash
# More partitions = more parallelism
kafka-topics.sh --alter \
  --topic orders \
  --partitions 10 \
  --bootstrap-server localhost:9092
```

**Rule of Thumb**:
```
Partitions = max(
    target_throughput / producer_throughput,
    target_throughput / consumer_throughput
)

Example:
Target: 1 GB/s
Producer: 100 MB/s
Consumer: 50 MB/s

Partitions = max(1000/100, 1000/50) = max(10, 20) = 20
```

### 2. Batch Size

```java
// Producer batching
props.put("batch.size", 16384); // 16 KB
props.put("linger.ms", 10); // Wait 10ms to batch

// Larger batches = higher throughput, higher latency
```

### 3. Compression

```java
props.put("compression.type", "snappy"); // or gzip, lz4, zstd

// Reduces network bandwidth
// Increases CPU usage
```

### 4. Consumer Fetch Size

```java
props.put("fetch.min.bytes", 1024); // 1 KB
props.put("fetch.max.wait.ms", 500); // Wait 500ms

// Larger fetches = fewer requests, higher throughput
```

---

## Real-World Example: Order Processing System

### Architecture

```
┌──────────────┐
│   Orders     │ (Topic: 10 partitions)
└──────┬───────┘
       │
       ├─→ Consumer Group 1: Order Validation (10 consumers)
       │   └─→ Topic: validated-orders (10 partitions)
       │
       ├─→ Consumer Group 2: Payment Processing (10 consumers)
       │   └─→ Topic: payment-completed (10 partitions)
       │
       └─→ Consumer Group 3: Inventory Update (10 consumers)
           └─→ Topic: inventory-updated (10 partitions)
```

### Implementation

```java
// Producer: Order Service
public class OrderProducer {
    private final KafkaProducer<String, Order> producer;
    
    public void createOrder(Order order) {
        ProducerRecord<String, Order> record = 
            new ProducerRecord<>("orders", order.getUserId(), order);
        
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Order sent to partition: " + 
                    metadata.partition());
            }
        });
    }
}

// Consumer 1: Order Validation
@Service
public class OrderValidationConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-validators")
    public void validateOrder(Order order) {
        // Validate order
        if (isValid(order)) {
            // Send to next topic
            kafkaTemplate.send("validated-orders", order.getUserId(), order);
        }
    }
}

// Consumer 2: Payment Processing
@Service
public class PaymentConsumer {
    
    @KafkaListener(topics = "validated-orders", groupId = "payment-processors")
    public void processPayment(Order order) {
        // Process payment
        Payment payment = paymentService.charge(order);
        
        // Send to next topic
        kafkaTemplate.send("payment-completed", order.getUserId(), payment);
    }
}

// Consumer 3: Inventory Update
@Service
public class InventoryConsumer {
    
    @KafkaListener(topics = "payment-completed", groupId = "inventory-updaters")
    public void updateInventory(Payment payment) {
        // Update inventory
        inventoryService.deduct(payment.getItems());
        
        // Send confirmation
        kafkaTemplate.send("inventory-updated", payment.getOrderId(), "SUCCESS");
    }
}
```

**Parallelism**:
- 10 partitions per topic
- 10 consumers per consumer group
- Each consumer processes 1 partition
- Total: 30 consumers processing in parallel

---

## Monitoring and Metrics

### Key Metrics

```java
// Producer metrics
producer.metrics().forEach((name, metric) -> {
    System.out.println(name + ": " + metric.metricValue());
});

// Important metrics:
// - record-send-rate
// - record-error-rate
// - request-latency-avg
// - batch-size-avg

// Consumer metrics
consumer.metrics().forEach((name, metric) -> {
    System.out.println(name + ": " + metric.metricValue());
});

// Important metrics:
// - records-consumed-rate
// - fetch-latency-avg
// - records-lag-max
// - commit-latency-avg
```

---

## Best Practices

### 1. Choose Right Number of Partitions

```
Start with: num_brokers * 2
Adjust based on throughput requirements
```

### 2. Use Consumer Groups for Parallelism

```java
// Scale consumers = scale partitions
// 10 partitions → 10 consumers (optimal)
```

### 3. Key Messages for Ordering

```java
// Related messages use same key
producer.send(new ProducerRecord<>("orders", userId, order));
```

### 4. Handle Rebalancing Gracefully

```java
consumer.subscribe(topics, new ConsumerRebalanceListener() {
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // Commit offsets before rebalancing
        consumer.commitSync();
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Initialize state for new partitions
    }
});
```

### 5. Monitor Consumer Lag

```bash
kafka-consumer-groups.sh --describe \
  --group order-processors \
  --bootstrap-server localhost:9092

# Output shows lag per partition
```

---

## Interview Questions & Answers

### Q1: How does Kafka achieve high throughput?

**Answer**:
- Partitioning for parallelism
- Sequential disk I/O
- Zero-copy optimization
- Batching and compression
- Page cache utilization

### Q2: What's the maximum parallelism in Kafka?

**Answer**: Number of partitions. If topic has 10 partitions, maximum 10 consumers can process in parallel within a consumer group.

### Q3: How to ensure message ordering?

**Answer**: 
- Use same key for related messages (go to same partition)
- Single partition for strict ordering (limits parallelism)
- Process messages in order within consumer

### Q4: What happens if consumer is slower than producer?

**Answer**:
- Consumer lag increases
- Messages accumulate in partitions
- Solutions: Add more consumers, optimize processing, increase partitions

### Q5: How does Kafka handle consumer failure?

**Answer**:
- Consumer group rebalances
- Failed consumer's partitions reassigned to healthy consumers
- Processing continues from last committed offset

---

## Key Takeaways

1. **Partitions enable parallelism** - more partitions = more parallelism
2. **Consumer groups** allow multiple consumers to process in parallel
3. **One partition = one consumer** within a consumer group
4. **Ordering guaranteed** within partition, not across partitions
5. **Use keys** for related messages to maintain order
6. **Replication** provides fault tolerance
7. **ISR** ensures data durability
8. **Monitor consumer lag** to detect processing issues
9. **Scale consumers** by adding more partitions
10. **Kafka is fast** due to sequential I/O and zero-copy

---

## Practice Problems

1. Design Kafka architecture for e-commerce order processing
2. Calculate optimal number of partitions for given throughput
3. Implement consumer with manual offset management
4. Handle consumer rebalancing gracefully
5. Design multi-stage data pipeline with Kafka
6. Optimize Kafka for low latency vs high throughput
7. Implement exactly-once semantics
8. Design Kafka cluster for multi-region deployment
9. Troubleshoot consumer lag issues
10. Implement custom partitioner for specific use case
