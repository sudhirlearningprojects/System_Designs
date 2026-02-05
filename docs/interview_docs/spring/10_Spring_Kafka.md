# Spring Kafka - Event-Driven Architecture Deep Dive

[← Back to Index](README.md) | [← Previous: Spring Cloud](09_Spring_Cloud.md) | [Next: Spring AOP →](11_Spring_AOP.md)

## Table of Contents
- [Theory: Understanding Kafka](#theory-understanding-kafka)
- [Producer Configuration](#producer-configuration)
- [Consumer Configuration](#consumer-configuration)
- [Kafka Listener](#kafka-listener)
- [Error Handling](#error-handling)
- [Transactions](#transactions)
- [Advanced Topics](#advanced-topics)

---

## Theory: Understanding Kafka

### What is Apache Kafka?

**Kafka**: Distributed event streaming platform for high-throughput, fault-tolerant messaging

**Key Characteristics**:
- Distributed and scalable
- High throughput (millions of messages/sec)
- Low latency (<10ms)
- Fault-tolerant and durable
- Persistent storage

### Core Concepts

**1. Topic**
- Logical channel for messages
- Like a table in database
- Partitioned for parallelism
- Example: "orders", "payments", "notifications"

**2. Partition**
- Physical division of topic
- Ordered, immutable sequence
- Each message has offset
- Enables parallelism

**3. Offset**
- Unique ID for message in partition
- Sequential number (0, 1, 2, ...)
- Consumer tracks position

**4. Producer**
- Publishes messages to topics
- Chooses partition (key-based or round-robin)
- Receives acknowledgment

**5. Consumer**
- Subscribes to topics
- Reads messages from partitions
- Part of consumer group

**6. Consumer Group**
- Group of consumers
- Each partition consumed by one consumer
- Load balancing and fault tolerance

**7. Broker**
- Kafka server
- Stores data
- Handles requests

**8. Cluster**
- Multiple brokers
- Replication for fault tolerance
- Leader-follower model

### Kafka Architecture

```
Producer 1 ──┐
Producer 2 ──┼──> Topic: orders (3 partitions)
Producer 3 ──┘         │
                       ├─ Partition 0 (Leader: Broker 1, Replicas: Broker 2,3)
                       ├─ Partition 1 (Leader: Broker 2, Replicas: Broker 1,3)
                       └─ Partition 2 (Leader: Broker 3, Replicas: Broker 1,2)
                              │
                              ├──> Consumer 1 (Group A)
                              ├──> Consumer 2 (Group A)
                              └──> Consumer 3 (Group B)
```

### Message Flow

```
1. Producer sends message with key
2. Kafka determines partition (hash(key) % partitions)
3. Message appended to partition log
4. Replicas sync from leader
5. Producer receives ack
6. Consumer polls for messages
7. Consumer processes message
8. Consumer commits offset
```

### Partitioning Strategy

**Key-based**:
```java
// Same key → same partition
producer.send("orders", "user123", order); // Always partition X
```

**Round-robin** (no key):
```java
// Distributed evenly
producer.send("orders", null, order); // Random partition
```

**Custom partitioner**:
```java
public class CustomPartitioner implements Partitioner {
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        // Custom logic
        return Math.abs(key.hashCode()) % cluster.partitionCountForTopic(topic);
    }
}
```

### Consumer Groups

**Scenario 1: Single Consumer Group**
```
Topic: orders (3 partitions)
Group: order-processors
  Consumer 1 → Partition 0
  Consumer 2 → Partition 1
  Consumer 3 → Partition 2
```

**Scenario 2: Multiple Consumer Groups**
```
Topic: orders (3 partitions)

Group A (order-processors):
  Consumer A1 → Partition 0, 1
  Consumer A2 → Partition 2

Group B (analytics):
  Consumer B1 → Partition 0, 1, 2
```

### Replication

**Leader-Follower Model**:
```
Partition 0:
  Leader: Broker 1 (handles reads/writes)
  Follower: Broker 2 (syncs from leader)
  Follower: Broker 3 (syncs from leader)
```

**Replication Factor**: Number of copies
- RF=1: No replication (data loss if broker fails)
- RF=2: 1 leader + 1 follower
- RF=3: 1 leader + 2 followers (recommended)

### Delivery Semantics

**1. At-most-once** (may lose messages)
```java
// Producer: acks=0 (no wait)
// Consumer: auto-commit before processing
```

**2. At-least-once** (may duplicate)
```java
// Producer: acks=all (wait for replicas)
// Consumer: commit after processing
```

**3. Exactly-once** (no loss, no duplicates)
```java
// Producer: enable.idempotence=true
// Consumer: transactional processing
```

### Kafka vs Traditional Messaging

| Feature | Kafka | RabbitMQ | ActiveMQ |
|---------|-------|----------|----------|
| Throughput | Very High | Medium | Medium |
| Latency | Low | Very Low | Low |
| Persistence | Always | Optional | Optional |
| Ordering | Per partition | Per queue | Per queue |
| Replay | Yes | No | No |
| Scalability | Excellent | Good | Good |
| Use Case | Event streaming | Task queues | Enterprise messaging |

### When to Use Kafka

✅ **Use Kafka for**:
- Event sourcing
- Log aggregation
- Stream processing
- Real-time analytics
- Microservices communication
- CDC (Change Data Capture)

❌ **Don't use Kafka for**:
- Request-response patterns
- Small message volumes
- Complex routing
- Immediate consistency

---

## Producer Configuration

### Understanding Producer

**Producer**: Sends messages to Kafka topics

**Key Responsibilities**:
- Serialize messages
- Determine partition
- Send to broker
- Handle acknowledgments
- Retry on failure

### Basic Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, Order> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Connection
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        
        // Serialization
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Order> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Producer Configuration Properties

```java
@Bean
public ProducerFactory<String, Order> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    
    // === Connection ===
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093");
    config.put(ProducerConfig.CLIENT_ID_CONFIG, "order-producer");
    
    // === Serialization ===
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    
    // === Acknowledgment ===
    config.put(ProducerConfig.ACKS_CONFIG, "all"); // 0, 1, all
    // 0 = no wait (fast, may lose data)
    // 1 = wait for leader (balanced)
    // all = wait for all replicas (safe, slow)
    
    // === Retries ===
    config.put(ProducerConfig.RETRIES_CONFIG, 3);
    config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
    
    // === Idempotence (Exactly-once) ===
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    // Automatically sets: acks=all, retries=MAX_INT, max.in.flight=5
    
    // === Batching ===
    config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
    config.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Wait 10ms for batch
    config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
    
    // === Compression ===
    config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // none, gzip, snappy, lz4, zstd
    
    // === Partitioning ===
    config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class);
    
    return new DefaultKafkaProducerFactory<>(config);
}
```

### Sending Messages

#### Fire and Forget

```java
@Service
public class OrderProducer {
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public void sendOrder(Order order) {
        kafkaTemplate.send("orders", order.getId().toString(), order);
        // No wait for response
    }
}
```

#### Synchronous Send

```java
public void sendOrderSync(Order order) throws Exception {
    SendResult<String, Order> result = kafkaTemplate
        .send("orders", order.getId().toString(), order)
        .get(10, TimeUnit.SECONDS); // Wait for response
    
    RecordMetadata metadata = result.getRecordMetadata();
    System.out.println("Sent to partition: " + metadata.partition());
    System.out.println("Offset: " + metadata.offset());
}
```

#### Asynchronous Send with Callback

```java
public void sendOrderAsync(Order order) {
    ListenableFuture<SendResult<String, Order>> future = 
        kafkaTemplate.send("orders", order.getId().toString(), order);
    
    future.addCallback(
        result -> {
            RecordMetadata metadata = result.getRecordMetadata();
            System.out.println("Success! Partition: " + metadata.partition() + 
                             ", Offset: " + metadata.offset());
        },
        ex -> {
            System.err.println("Failed to send: " + ex.getMessage());
        }
    );
}
```

#### With ProducerRecord

```java
public void sendWithHeaders(Order order) {
    ProducerRecord<String, Order> record = new ProducerRecord<>(
        "orders",                    // topic
        0,                           // partition (optional)
        order.getId().toString(),    // key
        order                        // value
    );
    
    // Add headers
    record.headers().add("source", "order-service".getBytes());
    record.headers().add("version", "1.0".getBytes());
    record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
    
    kafkaTemplate.send(record);
}
```

### Custom Serializer

```java
public class OrderSerializer implements Serializer<Order> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public byte[] serialize(String topic, Order order) {
        try {
            return objectMapper.writeValueAsBytes(order);
        } catch (Exception e) {
            throw new SerializationException("Error serializing order", e);
        }
    }
}
```

### Partitioner Strategy

```java
public class CustomPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        int partitionCount = cluster.partitionCountForTopic(topic);
        
        if (key == null) {
            // Round-robin for null keys
            return ThreadLocalRandom.current().nextInt(partitionCount);
        }
        
        // Hash-based partitioning
        return Math.abs(key.hashCode()) % partitionCount;
    }
    
    @Override
    public void close() { }
    
    @Override
    public void configure(Map<String, ?> configs) { }
}
```

### Producer Interceptor

```java
public class ProducerInterceptor implements org.apache.kafka.clients.producer.ProducerInterceptor<String, Order> {
    
    @Override
    public ProducerRecord<String, Order> onSend(ProducerRecord<String, Order> record) {
        // Modify record before sending
        System.out.println("Sending: " + record.key());
        record.headers().add("sent-time", String.valueOf(System.currentTimeMillis()).getBytes());
        return record;
    }
    
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception == null) {
            System.out.println("Ack received: " + metadata.offset());
        } else {
            System.err.println("Send failed: " + exception.getMessage());
        }
    }
    
    @Override
    public void close() { }
    
    @Override
    public void configure(Map<String, ?> configs) { }
}
```

### Best Practices

✅ **DO**:
- Use idempotence for exactly-once
- Set appropriate acks level
- Enable compression for large messages
- Use batching for throughput
- Add meaningful headers

❌ **DON'T**:
- Use acks=0 for critical data
- Send large messages (>1MB)
- Block on send() in hot path
- Ignore send failures

---

## Consumer Configuration

### Understanding Consumer

**Consumer**: Reads messages from Kafka topics

**Key Responsibilities**:
- Subscribe to topics
- Poll for messages
- Deserialize messages
- Process messages
- Commit offsets

### Basic Consumer Configuration

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Connection
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        
        // Consumer Group
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-group");
        
        // Deserialization
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

### Consumer Configuration Properties

```java
@Bean
public ConsumerFactory<String, Order> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    
    // === Connection ===
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ConsumerConfig.CLIENT_ID_CONFIG, "order-consumer");
    
    // === Consumer Group ===
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-group");
    config.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "consumer-1"); // Static membership
    
    // === Deserialization ===
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.model");
    config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Order.class);
    
    // === Offset Management ===
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // earliest = start from beginning
    // latest = start from end (default)
    // none = throw exception if no offset
    
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
    config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000); // If auto-commit
    
    // === Polling ===
    config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Max records per poll
    config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
    config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Min data to fetch
    config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Max wait time
    
    // === Session Management ===
    config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000); // 10 seconds
    config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3 seconds
    
    // === Isolation Level ===
    config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // or read_uncommitted
    
    return new DefaultKafkaConsumerFactory<>(config);
}
```

### Container Factory Configuration

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    
    factory.setConsumerFactory(consumerFactory());
    
    // === Concurrency ===
    factory.setConcurrency(3); // 3 consumer threads
    
    // === Batch Listener ===
    factory.setBatchListener(true); // Process messages in batch
    
    // === Acknowledgment Mode ===
    factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
    // RECORD = commit after each record
    // BATCH = commit after batch
    // TIME = commit after time interval
    // COUNT = commit after N records
    // MANUAL = manual commit
    // MANUAL_IMMEDIATE = manual commit immediately
    
    // === Error Handler ===
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new FixedBackOff(1000L, 3L) // 3 retries with 1s delay
    ));
    
    // === Record Filter ===
    factory.setRecordFilterStrategy(record -> {
        // Filter out messages
        return record.value() == null;
    });
    
    // === After Rollback Processor ===
    factory.setAfterRollbackProcessor(new DefaultAfterRollbackProcessor<>());
    
    return factory;
}
```

### Custom Deserializer

```java
public class OrderDeserializer implements Deserializer<Order> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Order deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, Order.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing order", e);
        }
    }
}
```

### Consumer Interceptor

```java
public class ConsumerInterceptor implements org.apache.kafka.clients.consumer.ConsumerInterceptor<String, Order> {
    
    @Override
    public ConsumerRecords<String, Order> onConsume(ConsumerRecords<String, Order> records) {
        // Modify records before processing
        System.out.println("Received " + records.count() + " records");
        return records;
    }
    
    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        System.out.println("Committed offsets: " + offsets);
    }
    
    @Override
    public void close() { }
    
    @Override
    public void configure(Map<String, ?> configs) { }
}
```

### Best Practices

✅ **DO**:
- Use manual commit for critical data
- Set appropriate max.poll.records
- Handle deserialization errors
- Monitor consumer lag
- Use static membership for stable groups

❌ **DON'T**:
- Use auto-commit for transactional processing
- Set max.poll.interval too low
- Block in consumer thread
- Ignore rebalancing

---

## Kafka Listener

### Basic Listener

```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consumeOrder(Order order) {
        System.out.println("Received order: " + order.getId());
        processOrder(order);
    }
}
```

### Listener with Metadata

```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consumeWithMetadata(
        @Payload Order order,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
        @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    
    System.out.println("Topic: " + topic);
    System.out.println("Partition: " + partition);
    System.out.println("Offset: " + offset);
    System.out.println("Timestamp: " + timestamp);
    System.out.println("Key: " + key);
    
    processOrder(order);
}
```

### Custom Headers

```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consumeWithCustomHeaders(
        @Payload Order order,
        @Header("source") String source,
        @Header("version") String version,
        @Header(value = "correlation-id", required = false) String correlationId) {
    
    System.out.println("Source: " + source);
    System.out.println("Version: " + version);
    System.out.println("Correlation ID: " + correlationId);
    
    processOrder(order);
}
```

### ConsumerRecord

```java
@KafkaListener(topics = "orders", groupId = "order-group")
public void consumeRecord(ConsumerRecord<String, Order> record) {
    System.out.println("Key: " + record.key());
    System.out.println("Value: " + record.value());
    System.out.println("Partition: " + record.partition());
    System.out.println("Offset: " + record.offset());
    System.out.println("Timestamp: " + record.timestamp());
    
    // Access headers
    record.headers().forEach(header -> {
        System.out.println(header.key() + ": " + new String(header.value()));
    });
    
    processOrder(record.value());
}
```

### Batch Listener

```java
@KafkaListener(topics = "orders", groupId = "order-group", 
               containerFactory = "batchFactory")
public void consumeBatch(List<Order> orders) {
    System.out.println("Received " + orders.size() + " orders");
    orders.forEach(this::processOrder);
}

// Batch with metadata
@KafkaListener(topics = "orders", groupId = "order-group",
               containerFactory = "batchFactory")
public void consumeBatchWithMetadata(
        List<Order> orders,
        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
    
    for (int i = 0; i < orders.size(); i++) {
        System.out.println("Order " + i + ": Partition=" + partitions.get(i) + 
                         ", Offset=" + offsets.get(i));
        processOrder(orders.get(i));
    }
}
```

### Multiple Topics

```java
@KafkaListener(topics = {"orders", "payments", "shipments"}, 
               groupId = "multi-topic-group")
public void consumeMultipleTopics(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    
    switch (topic) {
        case "orders":
            processOrder(message);
            break;
        case "payments":
            processPayment(message);
            break;
        case "shipments":
            processShipment(message);
            break;
    }
}
```

### Topic Pattern

```java
@KafkaListener(topicPattern = "order-.*", groupId = "pattern-group")
public void consumePattern(Order order) {
    // Consumes from: order-created, order-updated, order-cancelled, etc.
    processOrder(order);
}
```

### Partition Assignment

```java
// Specific partitions
@KafkaListener(
    topicPartitions = @TopicPartition(
        topic = "orders",
        partitions = {"0", "1"}
    ),
    groupId = "partition-group"
)
public void consumePartitions(Order order) {
    processOrder(order);
}

// Partition range
@KafkaListener(
    topicPartitions = @TopicPartition(
        topic = "orders",
        partitionOffsets = {
            @PartitionOffset(partition = "0", initialOffset = "0"),
            @PartitionOffset(partition = "1", initialOffset = "100")
        }
    ),
    groupId = "offset-group"
)
public void consumeFromOffset(Order order) {
    processOrder(order);
}
```

### Manual Acknowledgment

```java
@KafkaListener(topics = "orders", groupId = "manual-ack-group")
public void consumeManualAck(Order order, Acknowledgment ack) {
    try {
        processOrder(order);
        ack.acknowledge(); // Commit offset
    } catch (Exception e) {
        // Don't acknowledge - will be reprocessed
        System.err.println("Failed to process: " + e.getMessage());
    }
}

// Batch manual acknowledgment
@KafkaListener(topics = "orders", groupId = "batch-ack-group",
               containerFactory = "batchFactory")
public void consumeBatchManualAck(List<Order> orders, Acknowledgment ack) {
    try {
        orders.forEach(this::processOrder);
        ack.acknowledge(); // Commit all offsets
    } catch (Exception e) {
        // Rollback - all messages will be reprocessed
    }
}
```

### Conditional Listener

```java
@KafkaListener(
    topics = "orders",
    groupId = "conditional-group",
    filter = "orderFilter"
)
public void consumeFiltered(Order order) {
    processOrder(order);
}

@Bean
public RecordFilterStrategy<String, Order> orderFilter() {
    return record -> {
        // Filter out cancelled orders
        return "CANCELLED".equals(record.value().getStatus());
    };
}
```

### Reply Template

```java
@KafkaListener(topics = "order-requests", groupId = "request-group")
@SendTo("order-responses")
public OrderResponse processRequest(OrderRequest request) {
    // Process request
    OrderResponse response = new OrderResponse();
    response.setOrderId(request.getOrderId());
    response.setStatus("PROCESSED");
    return response; // Automatically sent to order-responses topic
}
```

### Concurrent Listeners

```java
@KafkaListener(
    topics = "orders",
    groupId = "concurrent-group",
    concurrency = "3" // 3 consumer threads
)
public void consumeConcurrent(Order order) {
    System.out.println("Thread: " + Thread.currentThread().getName());
    processOrder(order);
}
```

### Listener with SpEL

```java
@KafkaListener(
    topics = "#{@environment.getProperty('kafka.topic.orders')}",
    groupId = "#{@environment.getProperty('kafka.group.id')}"
)
public void consumeWithSpEL(Order order) {
    processOrder(order);
}
```

### Rebalance Listener

```java
@Component
public class RebalanceListener implements ConsumerAwareRebalanceListener {
    
    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer, 
                                    Collection<TopicPartition> partitions) {
        System.out.println("Partitions assigned: " + partitions);
    }
    
    @Override
    public void onPartitionsRevoked(Consumer<?, ?> consumer, 
                                   Collection<TopicPartition> partitions) {
        System.out.println("Partitions revoked: " + partitions);
        // Commit offsets before rebalance
        consumer.commitSync();
    }
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> factory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setConsumerRebalanceListener(rebalanceListener());
    return factory;
}
```

### Best Practices

✅ **DO**:
- Use manual acknowledgment for critical processing
- Handle exceptions properly
- Use batch processing for throughput
- Monitor consumer lag
- Set appropriate concurrency

❌ **DON'T**:
- Block in listener method
- Ignore deserialization errors
- Use auto-commit for transactional processing
- Process messages synchronously if high throughput needed

---

## Error Handling

### Default Error Handler

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> factory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    
    // Retry 3 times with 1 second delay
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new FixedBackOff(1000L, 3L)
    ));
    
    return factory;
}
```

### Custom Error Handler

```java
@Bean
public CommonErrorHandler errorHandler() {
    return new DefaultErrorHandler((record, exception) -> {
        // Handle error
        System.err.println("Failed to process: " + record.value());
        System.err.println("Error: " + exception.getMessage());
        
        // Send to DLQ
        kafkaTemplate.send("orders-dlq", record.value());
    }, new FixedBackOff(1000L, 3L));
}
```

### Dead Letter Queue (DLQ)

```java
@Bean
public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
    return new DeadLetterPublishingRecoverer(kafkaTemplate(),
        (record, ex) -> {
            // Determine DLQ topic
            return new TopicPartition(record.topic() + "-dlq", record.partition());
        });
}

@Bean
public CommonErrorHandler errorHandler() {
    return new DefaultErrorHandler(
        deadLetterPublishingRecoverer(),
        new FixedBackOff(1000L, 3L)
    );
}
```

### Retry with Exponential Backoff

```java
@Bean
public CommonErrorHandler errorHandler() {
    ExponentialBackOff backOff = new ExponentialBackOff();
    backOff.setInitialInterval(1000L);  // 1 second
    backOff.setMultiplier(2.0);         // Double each time
    backOff.setMaxInterval(10000L);     // Max 10 seconds
    backOff.setMaxElapsedTime(60000L);  // Max 1 minute total
    
    return new DefaultErrorHandler(backOff);
}
```

### Exception Classification

```java
@Bean
public CommonErrorHandler errorHandler() {
    DefaultErrorHandler handler = new DefaultErrorHandler(
        new FixedBackOff(1000L, 3L)
    );
    
    // Don't retry these exceptions
    handler.addNotRetryableExceptions(
        ValidationException.class,
        DeserializationException.class
    );
    
    // Always retry these
    handler.addRetryableExceptions(
        NetworkException.class,
        TimeoutException.class
    );
    
    return handler;
}
```

---

## Transactions

### Producer Transactions

```java
@Bean
public ProducerFactory<String, Order> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    
    // Enable transactions
    config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-tx-");
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    
    return new DefaultKafkaProducerFactory<>(config);
}

@Service
public class TransactionalProducer {
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    @Transactional("kafkaTransactionManager")
    public void sendTransactional(Order order) {
        kafkaTemplate.send("orders", order);
        kafkaTemplate.send("audit", order);
        // Both or neither
    }
}
```

### Consumer Transactions

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> factory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    
    // Enable transactions
    factory.getContainerProperties().setTransactionManager(kafkaTransactionManager());
    
    return factory;
}

@KafkaListener(topics = "orders", groupId = "tx-group")
public void consumeTransactional(Order order) {
    // Process in transaction
    processOrder(order);
    // Offset committed only if no exception
}
```

### Exactly-Once Semantics

```java
// Producer
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-id");

// Consumer
config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

---

## Advanced Topics

### Seek Operations

```java
@KafkaListener(topics = "orders", groupId = "seek-group")
public void consume(Order order, Consumer<String, Order> consumer) {
    // Seek to beginning
    consumer.seekToBeginning(consumer.assignment());
    
    // Seek to end
    consumer.seekToEnd(consumer.assignment());
    
    // Seek to specific offset
    TopicPartition partition = new TopicPartition("orders", 0);
    consumer.seek(partition, 100L);
}
```

### Pause/Resume

```java
@Autowired
private KafkaListenerEndpointRegistry registry;

public void pauseConsumer() {
    registry.getListenerContainer("consumerId").pause();
}

public void resumeConsumer() {
    registry.getListenerContainer("consumerId").resume();
}
```

### Admin Operations

```java
@Bean
public KafkaAdmin kafkaAdmin() {
    Map<String, Object> config = new HashMap<>();
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    return new KafkaAdmin(config);
}

@Bean
public NewTopic ordersTopic() {
    return TopicBuilder.name("orders")
        .partitions(3)
        .replicas(2)
        .config(TopicConfig.RETENTION_MS_CONFIG, "86400000") // 1 day
        .build();
}
```

### Metrics

```java
@Component
public class KafkaMetrics {
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public Map<MetricName, ? extends Metric> getProducerMetrics() {
        return kafkaTemplate.metrics();
    }
}
```

---

[← Previous: Spring Cloud](09_Spring_Cloud.md) | [Next: Spring AOP →](11_Spring_AOP.md)
