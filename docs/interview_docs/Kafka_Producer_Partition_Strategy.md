# Kafka Producer - Writing to Specific Partition

## Overview

Kafka producers can control which partition a message goes to using:
1. **Explicit partition number**
2. **Message key** (hash-based routing)
3. **Custom partitioner**
4. **Round-robin** (default when no key)

---

## 1. Explicit Partition Number

### Java Example

```java
import org.apache.kafka.clients.producer.*;
import java.util.Properties;

public class ExplicitPartitionProducer {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        
        // Send to partition 2 explicitly
        ProducerRecord<String, String> record = new ProducerRecord<>(
            "my-topic",     // topic
            2,              // partition number (0-indexed)
            "key1",         // key (optional)
            "message1"      // value
        );
        
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("Sent to partition: " + metadata.partition());
            }
        });
        
        producer.close();
    }
}
```

**Output**:
```
Sent to partition: 2
```

---

## 2. Key-Based Partitioning (Hash)

### How It Works

```
partition = hash(key) % number_of_partitions
```

### Java Example

```java
public class KeyBasedPartitionProducer {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        
        // Messages with same key go to same partition
        ProducerRecord<String, String> record1 = new ProducerRecord<>(
            "my-topic",
            "user123",      // key - determines partition
            "message1"
        );
        
        ProducerRecord<String, String> record2 = new ProducerRecord<>(
            "my-topic",
            "user123",      // same key = same partition
            "message2"
        );
        
        producer.send(record1, (metadata, ex) -> {
            System.out.println("Record1 -> Partition: " + metadata.partition());
        });
        
        producer.send(record2, (metadata, ex) -> {
            System.out.println("Record2 -> Partition: " + metadata.partition());
        });
        
        producer.close();
    }
}
```

**Output**:
```
Record1 -> Partition: 1
Record2 -> Partition: 1  (same partition)
```

---

## 3. Custom Partitioner

### Implementation

```java
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import java.util.Map;

public class CustomPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Strategy 1: Route VIP users to partition 0
        if (key != null && key.toString().startsWith("VIP")) {
            return 0;
        }
        
        // Strategy 2: Route by user ID range
        if (key != null) {
            String userId = key.toString();
            if (userId.startsWith("user1")) return 0;
            if (userId.startsWith("user2")) return 1;
            if (userId.startsWith("user3")) return 2;
        }
        
        // Strategy 3: Hash-based for others
        return Math.abs(key.hashCode()) % numPartitions;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

### Using Custom Partitioner

```java
public class CustomPartitionerProducer {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("partitioner.class", "com.example.CustomPartitioner");
        
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        
        // VIP user -> Partition 0
        producer.send(new ProducerRecord<>("my-topic", "VIP-user1", "VIP message"));
        
        // Regular users -> Hash-based
        producer.send(new ProducerRecord<>("my-topic", "user123", "Regular message"));
        
        producer.close();
    }
}
```

---

## 4. Round-Robin (Default)

### When Used

- No key provided
- No partition specified
- Default partitioner

```java
public class RoundRobinProducer {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        
        // No key, no partition -> Round-robin
        for (int i = 0; i < 10; i++) {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                "my-topic",
                null,           // no key
                "message" + i
            );
            
            producer.send(record, (metadata, ex) -> {
                System.out.println("Message -> Partition: " + metadata.partition());
            });
        }
        
        producer.close();
    }
}
```

**Output**:
```
Message -> Partition: 0
Message -> Partition: 1
Message -> Partition: 2
Message -> Partition: 0
Message -> Partition: 1
...
```

---

## 5. Advanced Custom Partitioner Examples

### Geographic Partitioning

```java
public class GeographicPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        if (key != null) {
            String location = key.toString();
            
            // Route by geographic region
            if (location.startsWith("US")) return 0;
            if (location.startsWith("EU")) return 1;
            if (location.startsWith("ASIA")) return 2;
        }
        
        return Math.abs(key.hashCode()) % numPartitions;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

### Priority-Based Partitioning

```java
public class PriorityPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Parse priority from value (assuming JSON)
        String valueStr = new String(valueBytes);
        
        if (valueStr.contains("\"priority\":\"HIGH\"")) {
            return 0;  // High priority -> Partition 0
        } else if (valueStr.contains("\"priority\":\"MEDIUM\"")) {
            return 1;  // Medium priority -> Partition 1
        } else {
            return 2;  // Low priority -> Partition 2
        }
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

### Time-Based Partitioning

```java
import java.time.LocalDateTime;

public class TimeBasedPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Route by hour of day
        int hour = LocalDateTime.now().getHour();
        
        if (hour >= 0 && hour < 8) return 0;      // Night shift
        if (hour >= 8 && hour < 16) return 1;     // Day shift
        if (hour >= 16 && hour < 24) return 2;    // Evening shift
        
        return 0;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

## 6. Spring Kafka Examples

### Explicit Partition

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendToPartition(String message, int partition) {
        kafkaTemplate.send("my-topic", partition, "key", message);
    }
    
    public void sendWithKey(String key, String message) {
        kafkaTemplate.send("my-topic", key, message);
    }
}
```

---

### Custom Partitioner Configuration

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("partitioner.class", "com.example.CustomPartitioner");
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

---

## 7. Partition Selection Decision Tree

```
┌─────────────────────────────────────┐
│  Producer sends message             │
└──────────────┬──────────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │ Partition specified? │
    └──────┬───────────────┘
           │
     ┌─────┴─────┐
     │           │
    YES         NO
     │           │
     ▼           ▼
  Use that   ┌──────────────┐
  partition  │ Key present? │
             └──┬───────────┘
                │
          ┌─────┴─────┐
          │           │
         YES         NO
          │           │
          ▼           ▼
    ┌──────────┐  ┌──────────┐
    │ Custom   │  │ Round    │
    │Partitioner│  │ Robin    │
    │  or      │  │          │
    │ Hash     │  │          │
    └──────────┘  └──────────┘
```

---

## 8. Best Practices

### 1. Maintain Order with Keys

```java
// All messages for same user go to same partition (ordered)
producer.send(new ProducerRecord<>("orders", "user123", "order1"));
producer.send(new ProducerRecord<>("orders", "user123", "order2"));
producer.send(new ProducerRecord<>("orders", "user123", "order3"));
// All in same partition, processed in order
```

---

### 2. Balance Load

```java
// Avoid hot partitions
public class BalancedPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Use murmur hash for better distribution
        return Math.abs(MurmurHash.hash32(keyBytes)) % numPartitions;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

---

### 3. Handle Partition Changes

```java
// Topic has 3 partitions initially
// Later scaled to 5 partitions
// Same key may go to different partition after scaling

// Solution: Use consistent hashing or sticky partitioning
public class ConsistentHashPartitioner implements Partitioner {
    
    private ConsistentHash<Integer> consistentHash;
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Initialize consistent hash ring
    }
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Consistent hashing minimizes remapping on partition changes
        return consistentHash.get(key.toString());
    }
    
    @Override
    public void close() {}
}
```

---

## 9. Common Use Cases

### Use Case 1: User-Based Partitioning

```java
// All events for same user in same partition (ordering)
ProducerRecord<String, String> record = new ProducerRecord<>(
    "user-events",
    userId,        // key = userId
    eventData
);
```

---

### Use Case 2: Tenant-Based Partitioning

```java
// Multi-tenant application - isolate tenants
ProducerRecord<String, String> record = new ProducerRecord<>(
    "tenant-data",
    tenantId,      // key = tenantId
    data
);
```

---

### Use Case 3: Priority Queue

```java
// High priority -> Partition 0 (dedicated consumer)
// Low priority -> Partition 1, 2, 3 (shared consumers)
int partition = priority.equals("HIGH") ? 0 : (hash(key) % 3) + 1;

ProducerRecord<String, String> record = new ProducerRecord<>(
    "tasks",
    partition,
    key,
    taskData
);
```

---

### Use Case 4: Geographic Routing

```java
// Route to region-specific partitions
String region = extractRegion(data);
int partition = getPartitionForRegion(region);

ProducerRecord<String, String> record = new ProducerRecord<>(
    "global-events",
    partition,
    key,
    data
);
```

---

## 10. Verification

### Check Partition Assignment

```bash
# Kafka console consumer with partition info
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --from-beginning \
  --property print.partition=true \
  --property print.key=true

# Output:
# Partition:0  Key:user123  Value:message1
# Partition:1  Key:user456  Value:message2
# Partition:0  Key:user123  Value:message3
```

---

### Programmatic Verification

```java
producer.send(record, (metadata, exception) -> {
    if (exception == null) {
        System.out.println("Topic: " + metadata.topic());
        System.out.println("Partition: " + metadata.partition());
        System.out.println("Offset: " + metadata.offset());
        System.out.println("Timestamp: " + metadata.timestamp());
    } else {
        exception.printStackTrace();
    }
});
```

---

## Summary

| Method | When to Use | Pros | Cons |
|--------|-------------|------|------|
| **Explicit Partition** | Testing, specific routing | Full control | Manual management |
| **Key-Based** | Ordering per key | Automatic, ordered | Hot partitions possible |
| **Custom Partitioner** | Complex routing logic | Flexible | More code |
| **Round-Robin** | No ordering needed | Balanced load | No ordering |

**Recommendation**: Use **key-based partitioning** for most use cases (ordering + automatic distribution).
