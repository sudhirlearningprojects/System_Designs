# Kafka Consumer API - Confluent Certification Study Guide

## Consumer Configuration

### Essential Properties

```java
Properties props = new Properties();

// Required
props.put("bootstrap.servers", "localhost:9092");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("group.id", "my-consumer-group");

// Important
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "5000");
props.put("auto.offset.reset", "earliest");  // earliest, latest, none
props.put("max.poll.records", "500");
props.put("max.poll.interval.ms", "300000"); // 5 minutes
props.put("session.timeout.ms", "45000");    // 45 seconds
props.put("heartbeat.interval.ms", "3000");  // 3 seconds
props.put("fetch.min.bytes", "1");
props.put("fetch.max.wait.ms", "500");
```

### Auto Offset Reset

**earliest**: Start from beginning of partition
**latest**: Start from end (only new messages)
**none**: Throw exception if no offset found

```java
props.put("auto.offset.reset", "earliest");
```

## Consumer Groups

### Concept

**Consumer Group**: Logical grouping of consumers sharing workload

**Rules**:
- Each partition assigned to ONE consumer in group
- Consumer can handle MULTIPLE partitions
- Different groups can read same data independently

```
Topic: orders (4 partitions)
Group: processors

Consumer 1: P0, P1
Consumer 2: P2, P3

Group: analytics (independent)
Consumer 1: P0, P1, P2, P3
```

### Group Coordination

```java
// Coordinator manages group membership
// Assigns partitions to consumers
// Handles rebalancing

props.put("group.id", "my-group");
```

## Subscribing to Topics

### Subscribe by Name

```java
consumer.subscribe(Arrays.asList("topic1", "topic2"));
```

### Subscribe with Pattern

```java
// Subscribe to all topics matching pattern
consumer.subscribe(Pattern.compile("orders-.*"));
```

### Assign Specific Partitions

```java
// Manual assignment (no group management)
TopicPartition partition0 = new TopicPartition("topic", 0);
TopicPartition partition1 = new TopicPartition("topic", 1);

consumer.assign(Arrays.asList(partition0, partition1));

// Cannot use subscribe() and assign() together
```

## Consuming Messages

### Basic Poll Loop

```java
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("topic"));

try {
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        
        for (ConsumerRecord<String, String> record : records) {
            System.out.printf("offset=%d, key=%s, value=%s%n",
                record.offset(), record.key(), record.value());
        }
    }
} finally {
    consumer.close();
}
```

### Processing Records

```java
ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

// Iterate by partition
for (TopicPartition partition : records.partitions()) {
    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
    
    for (ConsumerRecord<String, String> record : partitionRecords) {
        // Process record
    }
    
    // Commit offset for this partition
    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
    consumer.commitSync(Collections.singletonMap(
        partition, new OffsetAndMetadata(lastOffset + 1)
    ));
}
```

## Offset Management

### Auto Commit

```java
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "5000");

// Offsets committed automatically every 5 seconds
// Risk: May commit before processing completes
```

### Manual Commit - Synchronous

```java
props.put("enable.auto.commit", "false");

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    for (ConsumerRecord<String, String> record : records) {
        // Process record
    }
    
    // Commit after processing all records
    consumer.commitSync();  // Blocks until commit completes
}
```

### Manual Commit - Asynchronous

```java
consumer.commitAsync((offsets, exception) -> {
    if (exception != null) {
        logger.error("Commit failed", exception);
    }
});
```

### Commit Specific Offsets

```java
Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
offsets.put(
    new TopicPartition("topic", 0),
    new OffsetAndMetadata(100)
);

consumer.commitSync(offsets);
```

### Seeking to Specific Offset

```java
TopicPartition partition = new TopicPartition("topic", 0);

// Seek to specific offset
consumer.seek(partition, 100);

// Seek to beginning
consumer.seekToBeginning(Arrays.asList(partition));

// Seek to end
consumer.seekToEnd(Arrays.asList(partition));
```

## Rebalancing

### Rebalance Triggers

1. Consumer joins group
2. Consumer leaves group (graceful shutdown)
3. Consumer crashes (session timeout)
4. Topic partition count changes

### Rebalance Listener

```java
consumer.subscribe(Arrays.asList("topic"), new ConsumerRebalanceListener() {
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // Called before rebalance
        // Commit offsets for partitions being revoked
        consumer.commitSync();
        System.out.println("Partitions revoked: " + partitions);
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Called after rebalance
        // Initialize state for new partitions
        System.out.println("Partitions assigned: " + partitions);
    }
});
```

### Partition Assignment Strategies

**Range** (default):
```java
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RangeAssignor");

// Assigns consecutive partitions per topic
// Topic1: C1=[P0,P1], C2=[P2]
// Topic2: C1=[P0,P1], C2=[P2]
```

**RoundRobin**:
```java
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.RoundRobinAssignor");

// Distributes partitions evenly across all topics
// C1=[T1-P0, T2-P1], C2=[T1-P1, T2-P0]
```

**Sticky**:
```java
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.StickyAssignor");

// Minimizes partition movement during rebalance
// Preserves existing assignments when possible
```

**CooperativeSticky**:
```java
props.put("partition.assignment.strategy", "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");

// Incremental rebalancing (no stop-the-world)
// Only affected partitions are reassigned
```

## Deserialization

### Built-in Deserializers

```java
StringDeserializer
IntegerDeserializer
LongDeserializer
ByteArrayDeserializer
BytesDeserializer
```

### Custom Deserializer

```java
public class UserDeserializer implements Deserializer<User> {
    @Override
    public User deserialize(String topic, byte[] data) {
        if (data == null) return null;
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            String name = dis.readUTF();
            int age = dis.readInt();
            
            return new User(name, age);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing User", e);
        }
    }
}
```

### Avro Deserialization

```java
props.put("key.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
props.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
props.put("schema.registry.url", "http://localhost:8081");
props.put("specific.avro.reader", "true");

// Consumer automatically deserializes using schema from registry
```

## Consumer Lifecycle

### Heartbeats and Session Management

```java
// Consumer sends heartbeats to coordinator
props.put("session.timeout.ms", "45000");    // Max time without heartbeat
props.put("heartbeat.interval.ms", "3000");  // Heartbeat frequency

// Rule: heartbeat.interval.ms < session.timeout.ms / 3
```

### Poll Timeout

```java
props.put("max.poll.interval.ms", "300000"); // 5 minutes

// If poll() not called within this time, consumer considered dead
// Rebalance triggered
```

### Graceful Shutdown

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    consumer.wakeup();
}));

try {
    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        // Process records
    }
} catch (WakeupException e) {
    // Expected on shutdown
} finally {
    consumer.close(); // Commits offsets and leaves group
}
```

## Advanced Patterns

### Exactly-Once Consumption

```java
// Read-Process-Write pattern with transactions
props.put("isolation.level", "read_committed");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

producer.initTransactions();

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    
    producer.beginTransaction();
    
    for (ConsumerRecord<String, String> record : records) {
        // Process and produce
        ProducerRecord<String, String> outputRecord = process(record);
        producer.send(outputRecord);
    }
    
    // Send offsets to transaction
    Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (TopicPartition partition : records.partitions()) {
        List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
        long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
        offsets.put(partition, new OffsetAndMetadata(lastOffset + 1));
    }
    
    producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
    producer.commitTransaction();
}
```

### Pausing and Resuming

```java
// Pause consumption from specific partitions
Set<TopicPartition> partitions = consumer.assignment();
consumer.pause(partitions);

// Resume consumption
consumer.resume(partitions);

// Check paused partitions
Set<TopicPartition> paused = consumer.paused();
```

### Fetching Metadata

```java
// Get partition info
List<PartitionInfo> partitions = consumer.partitionsFor("topic");

// Get current assignment
Set<TopicPartition> assignment = consumer.assignment();

// Get subscription
Set<String> subscription = consumer.subscription();

// Get committed offset
OffsetAndMetadata offset = consumer.committed(new TopicPartition("topic", 0));

// Get beginning/end offsets
Map<TopicPartition, Long> beginningOffsets = 
    consumer.beginningOffsets(assignment);
Map<TopicPartition, Long> endOffsets = 
    consumer.endOffsets(assignment);
```

## Performance Tuning

### Fetch Configuration

```java
// Minimum bytes to fetch
props.put("fetch.min.bytes", "1");

// Max wait time if fetch.min.bytes not met
props.put("fetch.max.wait.ms", "500");

// Max bytes per partition
props.put("max.partition.fetch.bytes", "1048576"); // 1 MB

// Trade-off: Higher values = fewer requests, higher latency
```

### Poll Configuration

```java
// Max records returned per poll
props.put("max.poll.records", "500");

// Lower value = more frequent commits, less data loss on failure
// Higher value = better throughput
```

### Memory Configuration

```java
// Receive buffer size
props.put("receive.buffer.bytes", "65536"); // 64 KB

// Send buffer size
props.put("send.buffer.bytes", "131072");   // 128 KB
```

## Error Handling

### Deserialization Errors

```java
// Skip bad records
props.put("key.deserializer", StringDeserializer.class.getName());
props.put("value.deserializer", StringDeserializer.class.getName());

// Custom error handler
consumer.subscribe(Arrays.asList("topic"));

while (true) {
    try {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        
        for (ConsumerRecord<String, String> record : records) {
            try {
                processRecord(record);
            } catch (Exception e) {
                logger.error("Error processing record at offset " + record.offset(), e);
                // Continue processing other records
            }
        }
        
        consumer.commitSync();
    } catch (SerializationException e) {
        logger.error("Deserialization error", e);
        // Seek past bad record
    }
}
```

### Handling Rebalance Failures

```java
consumer.subscribe(Arrays.asList("topic"), new ConsumerRebalanceListener() {
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        try {
            // Commit offsets
            consumer.commitSync();
        } catch (CommitFailedException e) {
            logger.error("Commit failed during rebalance", e);
        }
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Initialize state
    }
});
```

## Best Practices

### 1. Always Close Consumer

```java
try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
    // Consume messages
} // Auto-closes
```

### 2. Handle Rebalancing

```java
consumer.subscribe(topics, new ConsumerRebalanceListener() {
    // Implement both methods
});
```

### 3. Commit Offsets Appropriately

```java
// For at-least-once: Commit after processing
// For at-most-once: Commit before processing
// For exactly-once: Use transactions
```

### 4. Configure Timeouts Properly

```java
// heartbeat.interval.ms < session.timeout.ms / 3
props.put("session.timeout.ms", "45000");
props.put("heartbeat.interval.ms", "3000");
```

### 5. Monitor Consumer Lag

```java
// Track difference between log-end-offset and committed offset
// High lag indicates slow processing
```

## Exam Practice Questions

**Q1**: What happens if max.poll.interval.ms is exceeded?
**A**: Consumer is considered dead, rebalance triggered, partitions reassigned

**Q2**: Can you use both subscribe() and assign() on same consumer?
**A**: No, they are mutually exclusive

**Q3**: What is the default partition assignment strategy?
**A**: RangeAssignor

**Q4**: With auto.offset.reset=none, what happens if no offset exists?
**A**: NoOffsetForPartitionException is thrown

**Q5**: What is the difference between commitSync() and commitAsync()?
**A**: commitSync() blocks until commit completes; commitAsync() is non-blocking

**Q6**: How do you consume only committed messages in transactional scenarios?
**A**: Set isolation.level=read_committed

**Q7**: What is the purpose of ConsumerRebalanceListener?
**A**: Handle actions before/after partition reassignment (commit offsets, initialize state)

**Q8**: Can multiple consumers in same group read from same partition?
**A**: No, each partition assigned to only one consumer per group

**Q9**: What is the relationship between heartbeat.interval.ms and session.timeout.ms?
**A**: heartbeat.interval.ms should be < session.timeout.ms / 3

**Q10**: How do you seek to the beginning of a partition?
**A**: consumer.seekToBeginning(Collections.singletonList(partition))
