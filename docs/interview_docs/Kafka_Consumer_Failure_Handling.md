# Kafka Consumer Failure Handling - Complete Guide

## Overview

When a Kafka consumer goes down or fails to process events, Kafka handles it through:
1. **Consumer Group Rebalancing**
2. **Offset Management**
3. **Session Timeout Detection**
4. **Partition Reassignment**

---

## 1. Consumer Failure Detection

### How Kafka Detects Consumer Failure

```
┌─────────────────────────────────────────────────────────┐
│              Kafka Broker (Group Coordinator)           │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Monitors consumer heartbeats                      │  │
│  │ session.timeout.ms = 45 seconds (default)         │  │
│  │ heartbeat.interval.ms = 3 seconds (default)       │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                    ↕ Heartbeat every 3s
┌─────────────────────────────────────────────────────────┐
│              Consumer (Alive)                           │
│  Sends heartbeat every 3 seconds                        │
│  Processes messages                                     │
└─────────────────────────────────────────────────────────┘

                    ✗ No heartbeat for 45s
┌─────────────────────────────────────────────────────────┐
│              Consumer (Dead/Crashed)                    │
│  No heartbeat sent                                      │
│  Broker triggers REBALANCE                              │
└─────────────────────────────────────────────────────────┘
```

### Key Configuration Properties

```properties
# Consumer fails if no heartbeat for 45 seconds
session.timeout.ms=45000

# Consumer sends heartbeat every 3 seconds
heartbeat.interval.ms=3000

# Max time between poll() calls (10 minutes default)
max.poll.interval.ms=600000

# Max records returned in single poll()
max.poll.records=500
```

---

## 2. Failure Scenarios

### Scenario 1: Consumer Crashes (Process Dies)

```java
public class ConsumerCrashExample {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));
        
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                    
                    // Simulate crash
                    if (record.value().equals("CRASH")) {
                        System.exit(1); // Process dies
                    }
                }
                
                consumer.commitSync();
            }
        } finally {
            consumer.close();
        }
    }
}
```

**What Happens**:
```
1. Consumer crashes (no heartbeat sent)
2. Broker waits for session.timeout.ms (45s)
3. Broker marks consumer as dead
4. Broker triggers REBALANCE
5. Partitions reassigned to other consumers in group
6. Other consumers start processing from last committed offset
```

---

### Scenario 2: Consumer Hangs (Slow Processing)

```java
public class SlowConsumerExample {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-group");
        props.put("max.poll.interval.ms", "300000"); // 5 minutes
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));
        
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            for (ConsumerRecord<String, String> record : records) {
                // Slow processing takes 10 minutes
                slowProcess(record); // Takes 10 minutes
                
                // Next poll() called after 10 minutes
                // Exceeds max.poll.interval.ms (5 minutes)
                // Consumer kicked out of group
            }
            
            consumer.commitSync();
        }
    }
    
    private static void slowProcess(ConsumerRecord<String, String> record) {
        try {
            Thread.sleep(600000); // 10 minutes
        } catch (InterruptedException e) {}
    }
}
```

**What Happens**:
```
1. Consumer polls messages
2. Processing takes > max.poll.interval.ms
3. Broker assumes consumer is dead
4. Broker triggers REBALANCE
5. Consumer kicked out of group
6. Partitions reassigned to other consumers
```

**Error**:
```
org.apache.kafka.clients.consumer.CommitFailedException: 
Commit cannot be completed since the group has already rebalanced
```

---

### Scenario 3: Network Partition

```java
public class NetworkPartitionExample {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-group");
        props.put("session.timeout.ms", "45000");
        props.put("heartbeat.interval.ms", "3000");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));
        
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                // Network partition occurs here
                // Consumer cannot send heartbeat
                // After 45 seconds, broker marks consumer as dead
                
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
                
                consumer.commitSync();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
```

**What Happens**:
```
1. Network partition occurs
2. Consumer cannot send heartbeat to broker
3. Broker waits for session.timeout.ms (45s)
4. Broker marks consumer as dead
5. Broker triggers REBALANCE
6. Partitions reassigned to other consumers
7. When network recovers, consumer rejoins group (new rebalance)
```

---

## 3. Rebalancing Process

### Rebalance Trigger Events

1. **Consumer joins group** (new consumer)
2. **Consumer leaves group** (graceful shutdown)
3. **Consumer fails** (no heartbeat)
4. **Topic partition count changes**
5. **Consumer subscription changes**

### Rebalance Flow

```
┌─────────────────────────────────────────────────────────┐
│  Initial State: 3 Consumers, 6 Partitions               │
│  Consumer1: [P0, P1]                                    │
│  Consumer2: [P2, P3]                                    │
│  Consumer3: [P4, P5]                                    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Consumer2 Crashes (no heartbeat for 45s)               │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Broker Detects Failure → Triggers REBALANCE            │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  All Consumers Stop Processing                          │
│  Partitions Reassigned                                  │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  New State: 2 Consumers, 6 Partitions                   │
│  Consumer1: [P0, P1, P2]                                │
│  Consumer3: [P4, P5, P3]                                │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Consumers Resume Processing from Last Committed Offset │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Offset Management

### Auto Commit (Default)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "5000"); // Commit every 5 seconds

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("my-topic"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
        // Offset auto-committed every 5 seconds
    }
}
```

**Problem**: If consumer crashes after processing but before auto-commit:
```
1. Consumer processes messages (offset 100-110)
2. Consumer crashes before auto-commit
3. Last committed offset: 99
4. New consumer starts from offset 100
5. Messages 100-110 processed AGAIN (duplicate processing)
```

---

### Manual Commit (Recommended)

```java
Properties props = new Properties();
props.put("enable.auto.commit", "false");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("my-topic"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
    }
    
    // Commit after processing all records
    consumer.commitSync();
}
```

**Benefit**: Ensures messages are committed only after successful processing.

---

### Commit Strategies

#### Strategy 1: Commit After Each Batch

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
    }
    
    consumer.commitSync(); // Commit after batch
}
```

**Pros**: Simple, minimal duplicate processing
**Cons**: If crash during batch, entire batch reprocessed

---

#### Strategy 2: Commit After Each Record

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
        
        // Commit after each record
        consumer.commitSync(Collections.singletonMap(
            new TopicPartition(record.topic(), record.partition()),
            new OffsetAndMetadata(record.offset() + 1)
        ));
    }
}
```

**Pros**: Minimal duplicate processing
**Cons**: High overhead, slow

---

#### Strategy 3: Async Commit

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        processRecord(record);
    }
    
    // Non-blocking commit
    consumer.commitAsync((offsets, exception) -> {
        if (exception != null) {
            System.err.println("Commit failed: " + exception.getMessage());
        }
    });
}
```

**Pros**: Non-blocking, high throughput
**Cons**: No guarantee of commit success

---

#### Strategy 4: Hybrid (Async + Sync on Close)

```java
try {
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        
        for (ConsumerRecord<String, String> record : records) {
            processRecord(record);
        }
        
        consumer.commitAsync(); // Fast, non-blocking
    }
} finally {
    consumer.commitSync(); // Ensure final commit on shutdown
    consumer.close();
}
```

**Pros**: High throughput + guaranteed final commit
**Cons**: Complex

---

## 5. Handling Unprocessable Messages

### Strategy 1: Skip and Log

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        try {
            processRecord(record);
        } catch (Exception e) {
            System.err.println("Failed to process: " + record.value());
            // Skip and continue
        }
    }
    
    consumer.commitSync();
}
```

---

### Strategy 2: Dead Letter Queue (DLQ)

```java
KafkaProducer<String, String> dlqProducer = new KafkaProducer<>(dlqProps);

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        try {
            processRecord(record);
        } catch (Exception e) {
            // Send to DLQ
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                "my-topic-dlq",
                record.key(),
                record.value()
            );
            dlqProducer.send(dlqRecord);
            
            System.err.println("Sent to DLQ: " + record.value());
        }
    }
    
    consumer.commitSync();
}
```

---

### Strategy 3: Retry with Backoff

```java
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        int retries = 0;
        int maxRetries = 3;
        boolean success = false;
        
        while (retries < maxRetries && !success) {
            try {
                processRecord(record);
                success = true;
            } catch (Exception e) {
                retries++;
                System.err.println("Retry " + retries + " for: " + record.value());
                
                if (retries < maxRetries) {
                    Thread.sleep(1000 * retries); // Exponential backoff
                } else {
                    // Send to DLQ after max retries
                    sendToDLQ(record);
                }
            }
        }
    }
    
    consumer.commitSync();
}
```

---

### Strategy 4: Pause and Resume

```java
Set<TopicPartition> pausedPartitions = new HashSet<>();

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        try {
            processRecord(record);
        } catch (Exception e) {
            // Pause partition on failure
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            consumer.pause(Collections.singleton(partition));
            pausedPartitions.add(partition);
            
            System.err.println("Paused partition: " + partition);
            
            // Schedule retry after delay
            scheduleRetry(partition, 60000); // Retry after 1 minute
        }
    }
    
    consumer.commitSync();
}

private void scheduleRetry(TopicPartition partition, long delayMs) {
    scheduler.schedule(() -> {
        consumer.resume(Collections.singleton(partition));
        pausedPartitions.remove(partition);
        System.out.println("Resumed partition: " + partition);
    }, delayMs, TimeUnit.MILLISECONDS);
}
```

---

## 6. Consumer Rebalance Listener

### Handling Rebalance Events

```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import java.util.*;

public class RebalanceListenerExample {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "my-group");
        props.put("enable.auto.commit", "false");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        
        consumer.subscribe(
            Arrays.asList("my-topic"),
            new ConsumerRebalanceListener() {
                
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    System.out.println("Partitions revoked: " + partitions);
                    
                    // Commit offsets before losing partitions
                    consumer.commitSync();
                    
                    // Cleanup resources
                    cleanupResources(partitions);
                }
                
                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    System.out.println("Partitions assigned: " + partitions);
                    
                    // Initialize resources for new partitions
                    initializeResources(partitions);
                    
                    // Optionally seek to specific offset
                    for (TopicPartition partition : partitions) {
                        long offset = getOffsetFromDatabase(partition);
                        consumer.seek(partition, offset);
                    }
                }
            }
        );
        
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            for (ConsumerRecord<String, String> record : records) {
                processRecord(record);
            }
            
            consumer.commitSync();
        }
    }
    
    private static void cleanupResources(Collection<TopicPartition> partitions) {
        // Close database connections, file handles, etc.
    }
    
    private static void initializeResources(Collection<TopicPartition> partitions) {
        // Open database connections, file handles, etc.
    }
    
    private static long getOffsetFromDatabase(TopicPartition partition) {
        // Retrieve offset from external storage
        return 0;
    }
}
```

---

## 7. Best Practices

### 1. Set Appropriate Timeouts

```properties
# Consumer considered dead after 45 seconds
session.timeout.ms=45000

# Send heartbeat every 3 seconds
heartbeat.interval.ms=3000

# Max time between poll() calls (adjust based on processing time)
max.poll.interval.ms=600000

# Reduce batch size if processing is slow
max.poll.records=100
```

---

### 2. Graceful Shutdown

```java
public class GracefulShutdown {
    
    private static volatile boolean running = true;
    
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully...");
            running = false;
        }));
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("my-topic"));
        
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
                
                consumer.commitSync();
            }
        } finally {
            consumer.close(); // Triggers rebalance immediately
        }
    }
}
```

---

### 3. Idempotent Processing

```java
public class IdempotentConsumer {
    
    private Set<String> processedIds = new HashSet<>();
    
    public void processRecord(ConsumerRecord<String, String> record) {
        String messageId = record.key();
        
        // Check if already processed
        if (processedIds.contains(messageId)) {
            System.out.println("Duplicate message, skipping: " + messageId);
            return;
        }
        
        // Process message
        doProcess(record);
        
        // Mark as processed
        processedIds.add(messageId);
        
        // Persist to database for durability
        saveProcessedId(messageId);
    }
}
```

---

### 4. Monitor Consumer Lag

```java
public class ConsumerLagMonitor {
    
    public void monitorLag(KafkaConsumer<String, String> consumer) {
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
        
        for (TopicPartition partition : consumer.assignment()) {
            long currentPosition = consumer.position(partition);
            long endOffset = endOffsets.get(partition);
            long lag = endOffset - currentPosition;
            
            System.out.println("Partition: " + partition + ", Lag: " + lag);
            
            if (lag > 10000) {
                System.err.println("HIGH LAG WARNING: " + partition);
            }
        }
    }
}
```

---

## Summary

| Failure Type | Detection Time | Recovery Action |
|--------------|----------------|-----------------|
| **Process Crash** | session.timeout.ms (45s) | Rebalance, reassign partitions |
| **Slow Processing** | max.poll.interval.ms (10min) | Kick consumer, rebalance |
| **Network Partition** | session.timeout.ms (45s) | Rebalance, reassign partitions |
| **Unprocessable Message** | Immediate | Skip, DLQ, or retry |

**Key Takeaways**:
1. Kafka detects failures via heartbeat mechanism
2. Failed consumer's partitions reassigned to healthy consumers
3. Processing resumes from last committed offset
4. Use manual commit for exactly-once semantics
5. Implement DLQ for unprocessable messages
6. Use rebalance listener for cleanup/initialization
7. Make processing idempotent to handle duplicates
