# Kafka Interview Questions & Scenarios

## 📋 Table of Contents
1. [Core Concepts](#core-concepts)
2. [Producer Questions](#producer-questions)
3. [Consumer Questions](#consumer-questions)
4. [Partition & Replication](#partition--replication)
5. [Performance & Tuning](#performance--tuning)
6. [Scenario-Based Questions](#scenario-based-questions)
7. [Advanced Topics](#advanced-topics)

---

## Core Concepts

### Q1: What is Apache Kafka and how does it work?
**Answer:**
Kafka is a distributed streaming platform that acts as a message broker. It stores streams of records in categories called topics.

**Key Components:**
- **Producer**: Publishes messages to topics
- **Consumer**: Subscribes to topics and processes messages
- **Broker**: Kafka server that stores data
- **Topic**: Category of messages
- **Partition**: Ordered, immutable sequence of records

```java
// Basic producer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("my-topic", "key", "value"));
```

### Q2: Explain Kafka's architecture
**Answer:**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Broker 1  │    │   Broker 2  │    │   Broker 3  │
│             │    │             │    │             │
│ Topic A     │    │ Topic A     │    │ Topic A     │
│ Partition 0 │    │ Partition 1 │    │ Partition 2 │
│ (Leader)    │    │ (Follower)  │    │ (Leader)    │
└─────────────┘    └─────────────┘    └─────────────┘
       ▲                   ▲                   ▲
       │                   │                   │
┌─────────────────────────────────────────────────────┐
│              ZooKeeper Cluster                      │
│        (Metadata & Coordination)                    │
└─────────────────────────────────────────────────────┘
```

### Q3: What is the role of ZooKeeper in Kafka?
**Answer:**
- **Broker Discovery**: Maintains list of active brokers
- **Leader Election**: Elects partition leaders
- **Configuration Management**: Stores topic configurations
- **Consumer Group Coordination**: Manages consumer group membership

**Note**: Kafka 2.8+ can run without ZooKeeper using KRaft mode.

---

## Producer Questions

### Q4: How does a Kafka producer determine which partition to send a message to?
**Answer:**
1. **Explicit Partition**: If specified, use that partition
2. **Key-based**: `hash(key) % num_partitions`
3. **Round-robin**: If no key, distribute evenly
4. **Custom Partitioner**: User-defined logic

```java
// Key-based partitioning
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",           // topic
    "user123",          // key (determines partition)
    "order-data"        // value
);

// Explicit partition
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",           // topic
    2,                  // partition number
    "user123",          // key
    "order-data"        // value
);
```

### Q5: What are producer acknowledgments (acks)?
**Answer:**
- **acks=0**: Fire and forget (no acknowledgment)
- **acks=1**: Leader acknowledgment only
- **acks=all/-1**: All in-sync replicas must acknowledge

```java
props.put("acks", "all");           // Highest durability
props.put("retries", 3);            // Retry failed sends
props.put("enable.idempotence", true); // Exactly-once semantics
```

### Q6: How do you handle producer failures?
**Answer:**
```java
producer.send(record, new Callback() {
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            // Handle failure
            logger.error("Failed to send message", exception);
            // Implement retry logic or dead letter queue
        } else {
            logger.info("Message sent to partition {} at offset {}", 
                       metadata.partition(), metadata.offset());
        }
    }
});
```

---

## Consumer Questions

### Q7: Explain consumer groups and partition assignment
**Answer:**
- **Consumer Group**: Logical grouping of consumers
- **Partition Assignment**: Each partition consumed by only one consumer in a group
- **Rebalancing**: Reassignment when consumers join/leave

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "order-processing-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("orders"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processOrder(record.value());
    }
}
```

### Q8: What are the different offset commit strategies?
**Answer:**
1. **Auto-commit**: `enable.auto.commit=true`
2. **Manual commit**: `consumer.commitSync()` or `consumer.commitAsync()`
3. **Per-message commit**: Commit after processing each message

```java
// Manual commit
props.put("enable.auto.commit", "false");

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        processMessage(record);
    }
    consumer.commitSync(); // Commit after processing batch
}
```

### Q9: How do you handle consumer lag?
**Answer:**
- **Scale consumers**: Add more consumers to group
- **Optimize processing**: Improve message processing speed
- **Batch processing**: Process multiple messages together
- **Monitoring**: Use tools like Kafka Manager or Burrow

```bash
# Check consumer lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group my-group --describe
```

---

## Partition & Replication

### Q10: How does Kafka ensure data durability?
**Answer:**
- **Replication Factor**: Multiple copies of each partition
- **In-Sync Replicas (ISR)**: Replicas that are caught up with leader
- **Min In-Sync Replicas**: Minimum replicas required for writes

```bash
# Create topic with replication factor 3
kafka-topics.sh --create --topic orders \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 3
```

### Q11: What happens when a broker fails?
**Answer:**
1. **Leader Election**: New leader elected from ISR
2. **Client Redirection**: Clients redirect to new leader
3. **Replica Sync**: New replicas sync with leader

```java
// Producer configuration for high availability
props.put("retries", Integer.MAX_VALUE);
props.put("max.in.flight.requests.per.connection", 1);
props.put("enable.idempotence", true);
```

---

## Performance & Tuning

### Q12: How do you optimize Kafka producer performance?
**Answer:**
```java
// Batching
props.put("batch.size", 16384);          // Batch size in bytes
props.put("linger.ms", 5);               // Wait time for batching

// Compression
props.put("compression.type", "snappy"); // or "gzip", "lz4", "zstd"

// Buffer memory
props.put("buffer.memory", 33554432);    // 32MB buffer

// Throughput vs Latency
props.put("acks", "1");                  // Balance durability/performance
```

### Q13: How do you optimize Kafka consumer performance?
**Answer:**
```java
// Fetch size
props.put("fetch.min.bytes", 1024);      // Minimum fetch size
props.put("fetch.max.wait.ms", 500);     // Max wait time

// Processing optimization
props.put("max.poll.records", 1000);     // Records per poll
props.put("session.timeout.ms", 30000);  // Session timeout

// Parallel processing
ExecutorService executor = Executors.newFixedThreadPool(10);
for (ConsumerRecord<String, String> record : records) {
    executor.submit(() -> processRecord(record));
}
```

---

## Scenario-Based Questions

### Q14: Design a system to process 1 million orders per second
**Answer:**
```java
// Producer configuration
Properties producerProps = new Properties();
producerProps.put("bootstrap.servers", "broker1:9092,broker2:9092,broker3:9092");
producerProps.put("acks", "1");                    // Balance durability/throughput
producerProps.put("batch.size", 65536);           // 64KB batches
producerProps.put("linger.ms", 10);               // 10ms batching window
producerProps.put("compression.type", "lz4");      // Fast compression
producerProps.put("buffer.memory", 134217728);     // 128MB buffer

// Topic configuration
// 100 partitions for parallelism
// Replication factor 3 for durability
kafka-topics.sh --create --topic orders \
  --partitions 100 --replication-factor 3

// Consumer scaling
// Deploy 100 consumer instances (1 per partition)
// Use consumer groups for automatic load balancing
```

### Q15: How would you implement exactly-once processing?
**Answer:**
```java
// Producer: Enable idempotence
props.put("enable.idempotence", true);
props.put("acks", "all");
props.put("retries", Integer.MAX_VALUE);
props.put("max.in.flight.requests.per.connection", 1);

// Consumer: Manual offset management with transactions
@Transactional
public void processMessage(ConsumerRecord<String, String> record) {
    // 1. Process message
    Order order = parseOrder(record.value());
    orderService.saveOrder(order);
    
    // 2. Store offset in same transaction
    offsetRepository.saveOffset(record.topic(), record.partition(), record.offset());
}
```

### Q16: Handle a scenario where one consumer is much slower than others
**Answer:**
```java
// Solution 1: Separate consumer groups by processing speed
// Fast consumers
props.put("group.id", "fast-processors");

// Slow consumers  
props.put("group.id", "slow-processors");

// Solution 2: Async processing with thread pools
public class AsyncConsumer {
    private final ExecutorService fastPool = Executors.newFixedThreadPool(20);
    private final ExecutorService slowPool = Executors.newFixedThreadPool(5);
    
    public void processRecord(ConsumerRecord<String, String> record) {
        if (isFastProcessing(record)) {
            fastPool.submit(() -> processFast(record));
        } else {
            slowPool.submit(() -> processSlow(record));
        }
    }
}
```

### Q17: Design a dead letter queue pattern
**Answer:**
```java
public class MessageProcessor {
    private final KafkaProducer<String, String> dlqProducer;
    private final String dlqTopic = "orders-dlq";
    private final int maxRetries = 3;
    
    public void processMessage(ConsumerRecord<String, String> record) {
        try {
            // Process message
            processOrder(record.value());
        } catch (Exception e) {
            int retryCount = getRetryCount(record);
            
            if (retryCount < maxRetries) {
                // Retry with exponential backoff
                scheduleRetry(record, retryCount + 1);
            } else {
                // Send to DLQ
                sendToDLQ(record, e);
            }
        }
    }
    
    private void sendToDLQ(ConsumerRecord<String, String> record, Exception error) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
            dlqTopic,
            record.key(),
            createDLQMessage(record, error)
        );
        dlqProducer.send(dlqRecord);
    }
}
```

### Q18: Implement message ordering across multiple partitions
**Answer:**
```java
// Problem: Global ordering across partitions is not possible
// Solution 1: Single partition (limits throughput)
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",
    0,              // Force single partition
    orderId,
    orderData
);

// Solution 2: Partition by entity ID for entity-level ordering
ProducerRecord<String, String> record = new ProducerRecord<>(
    "orders",
    customerId,     // Same customer always goes to same partition
    orderData
);

// Solution 3: Use message timestamps and reorder in consumer
public class OrderingConsumer {
    private final TreeMap<Long, ConsumerRecord<String, String>> buffer = new TreeMap<>();
    private final long maxDelay = 5000; // 5 seconds
    
    public void processRecords(ConsumerRecords<String, String> records) {
        // Add to buffer
        for (ConsumerRecord<String, String> record : records) {
            buffer.put(record.timestamp(), record);
        }
        
        // Process in order
        long cutoff = System.currentTimeMillis() - maxDelay;
        Iterator<Map.Entry<Long, ConsumerRecord<String, String>>> it = buffer.entrySet().iterator();
        
        while (it.hasNext() && it.next().getKey() < cutoff) {
            processInOrder(it.next().getValue());
            it.remove();
        }
    }
}
```

---

## Advanced Topics

### Q19: How does Kafka Streams work?
**Answer:**
```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-processing");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

StreamsBuilder builder = new StreamsBuilder();

// Stream processing topology
KStream<String, String> orders = builder.stream("orders");

orders
    .filter((key, value) -> isValidOrder(value))
    .mapValues(value -> enrichOrder(value))
    .groupByKey()
    .aggregate(
        () -> new OrderSummary(),
        (key, value, aggregate) -> aggregate.add(value),
        Materialized.as("order-summaries")
    )
    .toStream()
    .to("order-summaries");

KafkaStreams streams = new KafkaStreams(builder.build(), props);
streams.start();
```

### Q20: Explain Kafka Connect
**Answer:**
Kafka Connect is a framework for connecting Kafka with external systems.

```json
{
  "name": "jdbc-source-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:postgresql://localhost:5432/orders",
    "connection.user": "kafka",
    "connection.password": "password",
    "table.whitelist": "orders",
    "mode": "incrementing",
    "incrementing.column.name": "id",
    "topic.prefix": "db-"
  }
}
```

### Q21: How do you monitor Kafka?
**Answer:**
```java
// JMX Metrics
public class KafkaMetrics {
    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    
    public double getProducerThroughput() throws Exception {
        ObjectName objectName = new ObjectName(
            "kafka.producer:type=producer-metrics,client-id=*"
        );
        return (Double) server.getAttribute(objectName, "record-send-rate");
    }
    
    public long getConsumerLag() throws Exception {
        ObjectName objectName = new ObjectName(
            "kafka.consumer:type=consumer-fetch-manager-metrics,client-id=*"
        );
        return (Long) server.getAttribute(objectName, "records-lag-max");
    }
}
```

**Key Metrics to Monitor:**
- Producer: `record-send-rate`, `record-error-rate`, `batch-size-avg`
- Consumer: `records-consumed-rate`, `records-lag-max`, `commit-rate`
- Broker: `MessagesInPerSec`, `BytesInPerSec`, `UnderReplicatedPartitions`

### Q22: Kafka Security Implementation
**Answer:**
```java
// SSL Configuration
Properties props = new Properties();
props.put("security.protocol", "SSL");
props.put("ssl.truststore.location", "/path/to/truststore.jks");
props.put("ssl.truststore.password", "password");
props.put("ssl.keystore.location", "/path/to/keystore.jks");
props.put("ssl.keystore.password", "password");

// SASL Authentication
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism", "PLAIN");
props.put("sasl.jaas.config", 
    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
    "username=\"user\" password=\"password\";");

// ACL Authorization
kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
  --add --allow-principal User:alice \
  --operation Read --topic orders
```

---

## Quick Reference

### Common Configurations
```java
// Producer Configs
props.put("acks", "all");                    // Durability
props.put("retries", Integer.MAX_VALUE);     // Reliability
props.put("batch.size", 16384);              // Throughput
props.put("linger.ms", 5);                   // Latency vs throughput
props.put("compression.type", "snappy");      // Network efficiency

// Consumer Configs
props.put("enable.auto.commit", "false");     // Manual offset control
props.put("max.poll.records", 500);          // Batch size
props.put("session.timeout.ms", 30000);      // Failure detection
props.put("heartbeat.interval.ms", 3000);    // Keep-alive
```

### Performance Tuning Checklist
- ✅ Use appropriate batch sizes
- ✅ Enable compression
- ✅ Tune buffer sizes
- ✅ Optimize partition count
- ✅ Monitor consumer lag
- ✅ Use async processing
- ✅ Implement proper error handling
- ✅ Configure appropriate timeouts