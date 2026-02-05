# Kafka Missed Events/Messages - Deep Dive Guide

## Table of Contents
1. [Overview](#overview)
2. [Kafka Architecture Theory](#kafka-architecture-theory)
3. [Consumer Offset Management](#consumer-offset-management)
4. [Missed Events Scenarios](#missed-events-scenarios)
5. [Recovery Mechanisms](#recovery-mechanisms)
6. [Dead Letter Queue (DLQ) Deep Dive](#dead-letter-queue-deep-dive)
7. [Best Practices](#best-practices)

---

## Overview

### What are "Missed Events"?

Missed events occur when:
1. **Consumer is down** - Process crashed, network failure
2. **Consumer is slow** - Processing takes too long
3. **Consumer skips messages** - Offset committed incorrectly
4. **Consumer starts fresh** - New consumer joins group

### Key Concept: Kafka Never Loses Messages

```
┌─────────────────────────────────────────────────────────┐
│              Kafka Broker (Persistent Storage)          │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Topic: orders                                     │  │
│  │ Partition 0: [msg1, msg2, msg3, msg4, msg5]      │  │
│  │              offset: 0    1    2    3    4        │  │
│  │                                                   │  │
│  │ Messages stored for retention.ms (default 7 days)│  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│              Consumer Group: order-processors           │
│  Last committed offset: 2                               │
│  Consumer crashed after processing offset 2             │
│  Messages 3, 4, 5 are NOT lost - still in Kafka        │
└─────────────────────────────────────────────────────────┘
```

**Important**: Kafka stores messages on disk. If a consumer misses messages, they're still available for replay.

---

## Kafka Architecture Theory

### How Kafka Stores Messages

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Broker 1                                                  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │ Topic: orders (Partition 0 - Leader)                │  │  │
│  │  │ ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐  │  │  │
│  │  │ │ M0  │ M1  │ M2  │ M3  │ M4  │ M5  │ M6  │ M7  │  │  │  │
│  │  │ └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘  │  │  │
│  │  │ Stored on disk: /var/kafka-logs/orders-0/           │  │  │
│  │  │ Retention: 7 days (168 hours)                       │  │  │
│  │  │ Segment files: 00000000000000000000.log             │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Broker 2 (Replica)                                        │  │
│  │  Topic: orders (Partition 0 - Follower)                  │  │
│  │  Replicates data from Broker 1                           │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Message Lifecycle

```
1. Producer sends message
   ↓
2. Broker writes to disk (append-only log)
   ↓
3. Broker replicates to followers
   ↓
4. Broker acknowledges producer (acks=all)
   ↓
5. Message available for consumers
   ↓
6. Consumer reads message
   ↓
7. Consumer processes message
   ↓
8. Consumer commits offset
   ↓
9. Message remains in Kafka until retention expires
```

### Why Messages Are Never Lost

**1. Disk Persistence**
```
Kafka writes messages to disk immediately:
- Append-only log structure (fast sequential writes)
- OS page cache for performance
- Fsync to disk for durability

File structure:
/var/kafka-logs/
  orders-0/
    00000000000000000000.log  (segment file)
    00000000000000000000.index (offset index)
    00000000000000000000.timeindex (timestamp index)
```

**2. Replication**
```
Topic: orders
Replication Factor: 3

Broker 1: Leader (Partition 0)
Broker 2: Follower (Partition 0 - replica)
Broker 3: Follower (Partition 0 - replica)

If Broker 1 fails:
→ Broker 2 or 3 becomes new leader
→ No data loss
```

**3. Consumer Offset Tracking**
```
Kafka tracks TWO offsets:

1. Log End Offset (LEO): Last message in partition
   Example: 10000

2. Consumer Committed Offset: Last processed message
   Example: 5000

Lag = LEO - Committed Offset = 10000 - 5000 = 5000 messages behind
```

### Consumer Group Coordination

```
┌─────────────────────────────────────────────────────────────┐
│              Kafka Broker (Group Coordinator)               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Consumer Group: order-processors                      │  │
│  │                                                       │  │
│  │ Members:                                              │  │
│  │  - consumer-1 (alive) → Partition 0, 1               │  │
│  │  - consumer-2 (alive) → Partition 2, 3               │  │
│  │  - consumer-3 (DEAD)  → Partition 4, 5 (orphaned)    │  │
│  │                                                       │  │
│  │ Committed Offsets:                                    │  │
│  │  - (orders, 0) → 1000                                 │  │
│  │  - (orders, 1) → 1500                                 │  │
│  │  - (orders, 2) → 2000                                 │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                    ↓
         Detects consumer-3 failure
                    ↓
         Triggers REBALANCE
                    ↓
┌─────────────────────────────────────────────────────────────┐
│  New Assignment:                                            │
│  - consumer-1 → Partition 0, 1, 4                           │
│  - consumer-2 → Partition 2, 3, 5                           │
│                                                             │
│  Partition 4, 5 now assigned to consumer-1 and consumer-2  │
│  They start reading from last committed offsets             │
└─────────────────────────────────────────────────────────────┘
```

### Heartbeat Mechanism

```
Consumer sends heartbeat every 3 seconds (heartbeat.interval.ms)

Timeline:
T0:  Consumer sends heartbeat ✓
T3:  Consumer sends heartbeat ✓
T6:  Consumer sends heartbeat ✓
T9:  Consumer sends heartbeat ✓
...
T45: No heartbeat received for 45 seconds (session.timeout.ms)
     → Broker marks consumer as DEAD
     → Triggers rebalance

Heartbeat is sent by background thread:
- Separate from poll() thread
- Continues even if processing is slow
- Stops only if consumer crashes or network fails
```

### Poll Loop Timeout

```
Consumer must call poll() within max.poll.interval.ms (default 5 minutes)

Timeline:
T0:   consumer.poll() → Returns 500 messages
T1:   Processing messages...
T2:   Processing messages...
T3:   Processing messages...
T4:   Processing messages...
T5:   Still processing... (5 minutes elapsed)
T6:   max.poll.interval.ms exceeded!
      → Broker assumes consumer is stuck
      → Triggers rebalance
      → Consumer kicked out of group

Note: Heartbeat still sent, but poll() timeout is separate check
```

---

## Consumer Offset Management

### What is an Offset?

```
Topic: orders, Partition: 0
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ M1 │ M2 │ M3 │ M4 │ M5 │ M6 │ M7 │ M8 │
└────┴────┴────┴────┴────┴────┴────┴────┘
  0    1    2    3    4    5    6    7   ← Offsets

Consumer reads from offset 0 to 7 sequentially
```

### Offset Storage

Kafka stores consumer offsets in a special internal topic: `__consumer_offsets`

```java
// When consumer commits offset
consumer.commitSync(); // Stores offset in __consumer_offsets topic

// Format: (group.id, topic, partition) -> offset
// Example: (order-group, orders, 0) -> 5
```

### Auto Commit vs Manual Commit

#### 1. Auto Commit (Default - Risky)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "5000"); // Commit every 5 seconds

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processOrder(record); // If this fails, offset may already be committed!
    }
    // Auto-commit happens in background every 5 seconds
}
```

**Problem**: Message loss if consumer crashes after auto-commit but before processing.

```
Timeline:
T0: Consumer polls messages [offset 0-9]
T1: Auto-commit happens (offset 10 committed)
T2: Processing message at offset 5
T3: Consumer CRASHES
T4: Consumer restarts, reads from offset 10
Result: Messages 5-9 are LOST (never processed)
```

#### 2. Manual Commit (Recommended)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "false");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        try {
            processOrder(record);
            consumer.commitSync(); // Commit after successful processing
        } catch (Exception e) {
            // Don't commit - message will be reprocessed
            log.error("Failed to process: " + record.offset(), e);
        }
    }
}
```

**Benefit**: No message loss. If processing fails, offset is not committed.

#### 3. Batch Commit (Performance Optimized)

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processOrder(record);
    }
    
    // Commit after processing entire batch
    consumer.commitSync();
}
```

---

## Missed Events Scenarios

### Scenario 1: Consumer Crashes Before Commit

```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consume(ConsumerRecord<String, Order> record) {
        
        Order order = record.value();
        
        // Step 1: Process order
        processOrder(order); // Success
        
        // Step 2: Save to database
        orderRepository.save(order); // Success
        
        // Step 3: Consumer CRASHES here (before commit)
        System.exit(1);
        
        // Offset NOT committed
    }
}
```

**What Happens**:
```
1. Consumer crashes
2. Offset NOT committed (still at previous offset)
3. Consumer restarts
4. Kafka redelivers message from last committed offset
5. Message processed AGAIN (duplicate processing)
```

**Solution**: Idempotent processing

```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consume(ConsumerRecord<String, Order> record) {
        
        Order order = record.value();
        
        // Check if already processed (idempotency)
        if (orderRepository.existsByOrderId(order.getId())) {
            log.info("Order already processed: " + order.getId());
            return; // Skip duplicate
        }
        
        processOrder(order);
        orderRepository.save(order);
    }
}
```

---

### Scenario 2: Consumer Down for Extended Period

```
Timeline:
T0: Consumer processes offset 100
T1: Consumer CRASHES
T2-T10: Consumer is DOWN for 2 hours
T11: During downtime, 10,000 new messages arrive (offset 101-10100)
T12: Consumer RESTARTS
```

**What Happens**:
```java
// Consumer restarts and reads from last committed offset
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    // Kafka delivers ALL missed messages (101-10100)
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    // Consumer processes 10,000 backlog messages
    for (ConsumerRecord<String, String> record : records) {
        processOrder(record);
    }
}
```

**Kafka Behavior**:
- Messages are NOT lost
- Consumer catches up by processing backlog
- `max.poll.records` controls batch size (default 500)

**Configuration**:
```properties
# Control how many messages to fetch per poll
max.poll.records=500

# Control max time between polls (prevent consumer timeout)
max.poll.interval.ms=300000  # 5 minutes
```

---

### Scenario 3: New Consumer Joins Group

```java
// First consumer
@KafkaListener(topics = "orders", groupId = "order-group")
public void consumer1(ConsumerRecord<String, Order> record) {
    // Processing messages from offset 0
}

// New consumer joins same group
@KafkaListener(topics = "orders", groupId = "order-group")
public void consumer2(ConsumerRecord<String, Order> record) {
    // Where does this consumer start reading?
}
```

**Behavior depends on `auto.offset.reset`**:

```properties
# Option 1: Start from earliest (beginning of topic)
auto.offset.reset=earliest

# Option 2: Start from latest (only new messages)
auto.offset.reset=latest

# Option 3: Throw error if no offset found
auto.offset.reset=none
```

**Example**:
```
Topic has messages at offsets: 0-1000
Consumer group has committed offset: 500

Scenario A: Existing consumer restarts
→ Reads from offset 500 (last committed)

Scenario B: New consumer with auto.offset.reset=earliest
→ Reads from offset 0 (all messages)

Scenario C: New consumer with auto.offset.reset=latest
→ Reads from offset 1001 (only new messages)
```

---

### Scenario 4: Consumer Processes Too Slowly

```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consume(ConsumerRecord<String, Order> record) {
    
    // Slow processing (10 seconds per message)
    Thread.sleep(10000);
    processOrder(record.value());
    
    // If max.poll.interval.ms = 5 minutes
    // And batch has 500 messages
    // Total time = 500 * 10s = 5000s = 83 minutes
    // Exceeds max.poll.interval.ms!
}
```

**What Happens**:
```
1. Consumer polls 500 messages
2. Processing takes 83 minutes
3. Exceeds max.poll.interval.ms (5 minutes)
4. Kafka assumes consumer is dead
5. Triggers REBALANCE
6. Consumer kicked out of group
7. Partitions reassigned to other consumers
```

**Error**:
```
org.apache.kafka.clients.consumer.CommitFailedException: 
Commit cannot be completed since the group has already rebalanced
```

**Solutions**:

**Option 1**: Increase timeout
```properties
max.poll.interval.ms=600000  # 10 minutes
```

**Option 2**: Reduce batch size
```properties
max.poll.records=50  # Process fewer messages per poll
```

**Option 3**: Async processing
```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consume(ConsumerRecord<String, Order> record) {
    
    // Submit to thread pool for async processing
    executorService.submit(() -> {
        processOrder(record.value());
    });
    
    // Return quickly to poll again
}
```

---ption 1**: Increase timeout
```properties
max.poll.interval.ms=600000  # 10 minutes
```

**Option 2**: Reduce batch size
```properties
max.poll.records=50  # Process fewer messages per poll
```

**Option 3**: Async processing
```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consume(ConsumerRecord<String, Order> record) {
    
    // Submit to thread pool for async processing
    executorService.submit(() -> {
        processOrder(record.value());
    });
    
    // Return quickly to poll again
}
```

---

## Recovery Mechanisms

### 1. Seek to Specific Offset

```java
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

// Manually seek to specific offset
TopicPartition partition = new TopicPartition("orders", 0);
consumer.seek(partition, 100); // Start reading from offset 100

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    // Process messages starting from offset 100
}
```

### 2. Seek to Beginning (Replay All)

```java
consumer.subscribe(Arrays.asList("orders"));
consumer.poll(Duration.ofMillis(0)); // Trigger partition assignment

// Seek to beginning of all assigned partitions
consumer.seekToBeginning(consumer.assignment());

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    // Reprocess ALL messages from beginning
}
```

### 3. Seek to Timestamp

```java
// Replay messages from specific time
long timestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago

Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
for (TopicPartition partition : consumer.assignment()) {
    timestampsToSearch.put(partition, timestamp);
}

Map<TopicPartition, OffsetAndTimestamp> offsets = 
    consumer.offsetsForTimes(timestampsToSearch);

for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : offsets.entrySet()) {
    consumer.seek(entry.getKey(), entry.getValue().offset());
}
```

### 4. Dead Letter Queue (DLQ)

```java
@Service
public class OrderConsumer {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    private static final int MAX_RETRIES = 3;
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consume(ConsumerRecord<String, Order> record) {
        
        try {
            processOrder(record.value());
        } catch (Exception e) {
            
            int retryCount = getRetryCount(record);
            
            if (retryCount < MAX_RETRIES) {
                // Retry: Send to retry topic
                kafkaTemplate.send("orders-retry", record.value());
            } else {
                // Max retries exceeded: Send to DLQ
                kafkaTemplate.send("orders-dlq", record.value());
                log.error("Message sent to DLQ: " + record.value());
            }
        }
    }
}
```

### 5. Pause/Resume Consumer

```java
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        try {
            processOrder(record);
        } catch (DatabaseException e) {
            // Database is down - pause consumption
            consumer.pause(consumer.assignment());
            
            // Wait for database to recover
            Thread.sleep(60000);
            
            // Resume consumption
            consumer.resume(consumer.assignment());
        }
    }
}
```

---

## Dead Letter Queue (DLQ) Deep Dive

### What is a Dead Letter Queue?

A **Dead Letter Queue (DLQ)** is a separate Kafka topic where messages that fail processing are sent after exhausting all retry attempts.

```
┌─────────────────────────────────────────────────────────────┐
│                    Normal Flow                              │
└─────────────────────────────────────────────────────────────┘

Producer → [orders] → Consumer → Process ✓ → Commit Offset

┌─────────────────────────────────────────────────────────────┐
│                    Failure Flow with DLQ                    │
└─────────────────────────────────────────────────────────────┘

Producer → [orders] → Consumer → Process ✗ (fails)
                         ↓
                    Retry 1 ✗
                         ↓
                    Retry 2 ✗
                         ↓
                    Retry 3 ✗
                         ↓
                Send to [orders-dlq]
                         ↓
                Commit original offset
                         ↓
                Continue processing next message
```

### DLQ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Topic: orders (Main Topic)                              │   │
│  │ Partitions: 6                                           │   │
│  │ Messages: Normal order events                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Consumer Group: order-processors                        │   │
│  │ Consumers: 3                                            │   │
│  │ Processing: 10,000 msg/sec                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│                    Success ✓ | Failure ✗                       │
│                           ↓         ↓                           │
│                      Commit    Retry Logic                      │
│                                     ↓                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Topic: orders-retry (Retry Topic)                       │   │
│  │ Partitions: 3                                           │   │
│  │ Messages: Failed messages for retry                     │   │
│  │ Retention: 1 hour                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Consumer Group: order-retry-processors                  │   │
│  │ Consumers: 1 (slower processing)                        │   │
│  │ Processing: 100 msg/sec                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│                    Success ✓ | Still Fails ✗                   │
│                           ↓         ↓                           │
│                      Commit    Max Retries Exceeded             │
│                                     ↓                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Topic: orders-dlq (Dead Letter Queue)                   │   │
│  │ Partitions: 1                                           │   │
│  │ Messages: Permanently failed messages                   │   │
│  │ Retention: 30 days (for investigation)                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ DLQ Processor (Manual/Automated)                        │   │
│  │ - Alert operations team                                 │   │
│  │ - Log to monitoring system                              │   │
│  │ - Store in database for analysis                        │   │
│  │ - Manual reprocessing after fix                         │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### DLQ Implementation Patterns

#### Pattern 1: Simple DLQ (No Retry)

```java
@Service
public class OrderConsumer {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consume(ConsumerRecord<String, Order> record) {
        
        try {
            processOrder(record.value());
        } catch (Exception e) {
            // Send directly to DLQ
            sendToDLQ(record, e);
        }
    }
    
    private void sendToDLQ(ConsumerRecord<String, Order> record, Exception e) {
        DLQMessage dlqMessage = DLQMessage.builder()
            .originalTopic(record.topic())
            .originalPartition(record.partition())
            .originalOffset(record.offset())
            .originalKey(record.key())
            .originalValue(record.value())
            .errorMessage(e.getMessage())
            .errorStackTrace(ExceptionUtils.getStackTrace(e))
            .failedTimestamp(System.currentTimeMillis())
            .build();
        
        kafkaTemplate.send("orders-dlq", dlqMessage);
        log.error("Message sent to DLQ: offset={}, error={}", 
                  record.offset(), e.getMessage());
    }
}
```

#### Pattern 2: DLQ with Retry Logic

```java
@Service
public class OrderConsumerWithRetry {
    
    @Autowired
    private KafkaTemplate<String, RetryableMessage> kafkaTemplate;
    
    private static final int MAX_RETRIES = 3;
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consume(ConsumerRecord<String, Order> record) {
        
        try {
            processOrder(record.value());
        } catch (Exception e) {
            handleFailure(record, e, 0);
        }
    }
    
    private void handleFailure(ConsumerRecord<String, Order> record, 
                              Exception e, int retryCount) {
        
        if (retryCount < MAX_RETRIES) {
            // Send to retry topic
            RetryableMessage retryMsg = RetryableMessage.builder()
                .originalMessage(record.value())
                .retryCount(retryCount + 1)
                .lastError(e.getMessage())
                .nextRetryTime(calculateBackoff(retryCount))
                .build();
            
            kafkaTemplate.send("orders-retry", retryMsg);
            log.warn("Retry {}/{}: offset={}", retryCount + 1, MAX_RETRIES, record.offset());
        } else {
            // Max retries exceeded - send to DLQ
            sendToDLQ(record, e, retryCount);
        }
    }
    
    private long calculateBackoff(int retryCount) {
        // Exponential backoff: 1s, 2s, 4s, 8s...
        return System.currentTimeMillis() + (long) Math.pow(2, retryCount) * 1000;
    }
}
```

#### Pattern 3: Retry Consumer (Separate Service)

```java
@Service
public class RetryConsumer {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @KafkaListener(topics = "orders-retry", groupId = "retry-group")
    public void consumeRetry(ConsumerRecord<String, RetryableMessage> record) {
        
        RetryableMessage retryMsg = record.value();
        
        // Check if it's time to retry (respect backoff)
        if (System.currentTimeMillis() < retryMsg.getNextRetryTime()) {
            // Not yet time - send back to retry topic
            kafkaTemplate.send("orders-retry", retryMsg);
            return;
        }
        
        try {
            // Attempt to process again
            orderService.processOrder(retryMsg.getOriginalMessage());
            log.info("Retry successful after {} attempts", retryMsg.getRetryCount());
            
        } catch (Exception e) {
            
            if (retryMsg.getRetryCount() < 3) {
                // Increment retry count and send back to retry topic
                retryMsg.setRetryCount(retryMsg.getRetryCount() + 1);
                retryMsg.setLastError(e.getMessage());
                retryMsg.setNextRetryTime(calculateBackoff(retryMsg.getRetryCount()));
                
                kafkaTemplate.send("orders-retry", retryMsg);
            } else {
                // Max retries exceeded - send to DLQ
                sendToDLQ(retryMsg, e);
            }
        }
    }
}
```

### Who Handles DLQ Messages?

#### Option 1: Monitoring & Alerting Service

```java
@Service
public class DLQMonitoringService {
    
    @Autowired
    private AlertingService alertingService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private DLQRepository dlqRepository;
    
    @KafkaListener(topics = "orders-dlq", groupId = "dlq-monitor")
    public void monitorDLQ(ConsumerRecord<String, DLQMessage> record) {
        
        DLQMessage dlqMsg = record.value();
        
        // 1. Store in database for analysis
        DLQEntry entry = new DLQEntry();
        entry.setTopic(dlqMsg.getOriginalTopic());
        entry.setOffset(dlqMsg.getOriginalOffset());
        entry.setMessage(dlqMsg.getOriginalValue());
        entry.setError(dlqMsg.getErrorMessage());
        entry.setStackTrace(dlqMsg.getErrorStackTrace());
        entry.setTimestamp(dlqMsg.getFailedTimestamp());
        entry.setStatus("PENDING_REVIEW");
        
        dlqRepository.save(entry);
        
        // 2. Send alert to operations team
        alertingService.sendAlert(
            "DLQ Alert",
            "Message failed after all retries: " + dlqMsg.getErrorMessage(),
            AlertSeverity.HIGH
        );
        
        // 3. Update metrics
        metricsService.incrementCounter("dlq.messages.count");
        metricsService.recordGauge("dlq.messages.total", dlqRepository.countPending());
        
        // 4. Log for debugging
        log.error("DLQ Message: topic={}, offset={}, error={}",
                  dlqMsg.getOriginalTopic(),
                  dlqMsg.getOriginalOffset(),
                  dlqMsg.getErrorMessage());
    }
}
```

#### Option 2: Automated Reprocessing Service

```java
@Service
public class DLQReprocessingService {
    
    @Autowired
    private DLQRepository dlqRepository;
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    // Scheduled job runs every hour
    @Scheduled(cron = "0 0 * * * *")
    public void reprocessDLQMessages() {
        
        // Find messages that can be retried
        List<DLQEntry> retryableMessages = dlqRepository
            .findByStatusAndErrorType("PENDING_REVIEW", "TRANSIENT_ERROR");
        
        for (DLQEntry entry : retryableMessages) {
            
            try {
                // Attempt to reprocess
                Order order = deserialize(entry.getMessage());
                processOrder(order);
                
                // Mark as resolved
                entry.setStatus("RESOLVED");
                entry.setResolvedTimestamp(System.currentTimeMillis());
                dlqRepository.save(entry);
                
                log.info("DLQ message reprocessed successfully: {}", entry.getId());
                
            } catch (Exception e) {
                // Still failing - update retry count
                entry.setRetryCount(entry.getRetryCount() + 1);
                
                if (entry.getRetryCount() > 10) {
                    // Give up - mark as permanent failure
                    entry.setStatus("PERMANENT_FAILURE");
                    alertingService.sendAlert("Permanent DLQ Failure", entry.getId());
                }
                
                dlqRepository.save(entry);
            }
        }
    }
}
```

#### Option 3: Manual Review Dashboard

```java
@RestController
@RequestMapping("/api/dlq")
public class DLQManagementController {
    
    @Autowired
    private DLQRepository dlqRepository;
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    // Get all DLQ messages
    @GetMapping
    public Page<DLQEntry> getDLQMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        return dlqRepository.findByStatus("PENDING_REVIEW", 
                                         PageRequest.of(page, size));
    }
    
    // Get DLQ message details
    @GetMapping("/{id}")
    public DLQEntry getDLQMessage(@PathVariable Long id) {
        return dlqRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("DLQ entry not found"));
    }
    
    // Manually reprocess a DLQ message
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<?> reprocessMessage(@PathVariable Long id) {
        
        DLQEntry entry = dlqRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("DLQ entry not found"));
        
        try {
            // Send back to original topic
            Order order = deserialize(entry.getMessage());
            kafkaTemplate.send(entry.getTopic(), order);
            
            entry.setStatus("REPROCESSED");
            entry.setReprocessedTimestamp(System.currentTimeMillis());
            dlqRepository.save(entry);
            
            return ResponseEntity.ok("Message reprocessed successfully");
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Failed to reprocess: " + e.getMessage());
        }
    }
    
    // Mark as resolved (skip reprocessing)
    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveMessage(
            @PathVariable Long id,
            @RequestBody ResolveRequest request) {
        
        DLQEntry entry = dlqRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("DLQ entry not found"));
        
        entry.setStatus("RESOLVED");
        entry.setResolutionNotes(request.getNotes());
        entry.setResolvedBy(request.getResolvedBy());
        entry.setResolvedTimestamp(System.currentTimeMillis());
        
        dlqRepository.save(entry);
        
        return ResponseEntity.ok("Message marked as resolved");
    }
    
    // Bulk reprocess
    @PostMapping("/bulk-reprocess")
    public ResponseEntity<?> bulkReprocess(@RequestBody List<Long> ids) {
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Long id : ids) {
            try {
                reprocessMessage(id);
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", successCount,
            "failed", failureCount
        ));
    }
}
```

### DLQ Message Structure

```java
@Data
@Builder
public class DLQMessage {
    // Original message metadata
    private String originalTopic;
    private int originalPartition;
    private long originalOffset;
    private String originalKey;
    private Object originalValue;
    
    // Error information
    private String errorMessage;
    private String errorStackTrace;
    private String errorType; // TRANSIENT, PERMANENT, UNKNOWN
    
    // Retry information
    private int retryCount;
    private long firstFailedTimestamp;
    private long lastFailedTimestamp;
    
    // Processing metadata
    private String consumerGroupId;
    private String consumerId;
    private String applicationVersion;
    
    // Additional context
    private Map<String, String> headers;
    private String environment; // dev, staging, prod
}
```

### DLQ Best Practices

**1. Separate DLQ per Topic**
```
orders → orders-dlq
payments → payments-dlq
notifications → notifications-dlq
```

**2. DLQ Retention Policy**
```properties
# Keep DLQ messages for 30 days for investigation
retention.ms=2592000000

# Compact DLQ to save space (keep latest per key)
cleanup.policy=compact
```

**3. Error Classification**
```java
public enum ErrorType {
    TRANSIENT,      // Network timeout, DB connection - can retry
    PERMANENT,      // Invalid data, business rule violation - cannot retry
    UNKNOWN         // Unexpected error - needs investigation
}
```

**4. Monitoring & Alerting**
```java
// Alert if DLQ size exceeds threshold
if (dlqMessageCount > 1000) {
    alertingService.sendAlert("High DLQ volume", AlertSeverity.CRITICAL);
}

// Alert if DLQ messages are old
if (oldestDLQMessageAge > 24 * 60 * 60 * 1000) {
    alertingService.sendAlert("Stale DLQ messages", AlertSeverity.HIGH);
}
```

---

## Best Practices

### 1. At-Least-Once Delivery (Recommended)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "false");
props.put("isolation.level", "read_committed");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        // Process message
        processOrder(record);
        
        // Commit only after successful processing
        consumer.commitSync();
    }
}
```

**Guarantees**:
- No message loss
- Possible duplicates (if crash after processing but before commit)
- Requires idempotent processing

### 2. Exactly-Once Semantics (EOS)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "false");
props.put("isolation.level", "read_committed");
props.put("transactional.id", "order-processor-1");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

producer.initTransactions();

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    producer.beginTransaction();
    
    try {
        for (ConsumerRecord<String, String> record : records) {
            // Process and produce in transaction
            String result = processOrder(record);
            producer.send(new ProducerRecord<>("processed-orders", result));
        }
        
        // Commit transaction (includes consumer offset)
        producer.sendOffsetsToTransaction(
            getOffsets(records), 
            consumer.groupMetadata()
        );
        producer.commitTransaction();
        
    } catch (Exception e) {
        producer.abortTransaction();
    }
}
```

### 3. Idempotent Processing

```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    @Transactional
    public void consume(ConsumerRecord<String, Order> record) {
        
        Order order = record.value();
        
        // Use unique message ID for deduplication
        String messageId = record.topic() + "-" + 
                          record.partition() + "-" + 
                          record.offset();
        
        // Check if already processed
        if (processedMessageRepository.existsById(messageId)) {
            log.info("Duplicate message, skipping: " + messageId);
            return;
        }
        
        // Process order
        processOrder(order);
        
        // Mark as processed
        processedMessageRepository.save(new ProcessedMessage(messageId));
    }
}
```

### 4. Monitoring Lag

```java
@Component
public class ConsumerLagMonitor {
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorLag() {
        
        AdminClient adminClient = AdminClient.create(props);
        
        Map<TopicPartition, OffsetAndMetadata> committedOffsets = 
            adminClient.listConsumerGroupOffsets("order-group").partitionsToOffsetAndMetadata().get();
        
        Map<TopicPartition, Long> endOffsets = 
            consumer.endOffsets(committedOffsets.keySet());
        
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
            TopicPartition partition = entry.getKey();
            long committedOffset = entry.getValue().offset();
            long endOffset = endOffsets.get(partition);
            
            long lag = endOffset - committedOffset;
            
            if (lag > 10000) {
                log.warn("High lag detected: " + lag + " messages behind");
                alerting.sendAlert("Consumer lag: " + lag);
            }
        }
    }
}
```

### 5. Configuration Summary

```properties
# Consumer Group
group.id=order-group

# Offset Management
enable.auto.commit=false
auto.offset.reset=earliest

# Performance
max.poll.records=500
max.poll.interval.ms=300000
fetch.min.bytes=1
fetch.max.wait.ms=500

# Session Management
session.timeout.ms=45000
heartbeat.interval.ms=3000

# Reliability
isolation.level=read_committed
```

---

## Summary

| Scenario | Kafka Behavior | Solution |
|----------|---------------|----------|
| **Consumer crashes** | Messages NOT lost, stored in Kafka | Consumer restarts from last committed offset |
| **Consumer down for hours** | Messages accumulate in Kafka | Consumer processes backlog on restart |
| **Processing fails** | Offset not committed | Message redelivered (at-least-once) |
| **Duplicate processing** | Same message processed twice | Implement idempotent processing |
| **Consumer too slow** | Rebalance triggered | Increase `max.poll.interval.ms` or reduce `max.poll.records` |
| **New consumer** | Depends on `auto.offset.reset` | Set to `earliest` to process all messages |
| **Need to replay** | Use `seek()` methods | Seek to specific offset or timestamp |

**Key Takeaway**: Kafka NEVER loses messages. They're stored on disk for `retention.ms` (default 7 days). Consumers can always replay missed messages.
