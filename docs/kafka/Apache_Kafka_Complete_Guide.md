# Apache Kafka - Complete Deep Dive Guide

## Table of Contents
1. [Introduction to Apache Kafka](#introduction-to-apache-kafka)
2. [Kafka Architecture](#kafka-architecture)
3. [Core Concepts](#core-concepts)
4. [Internal Working](#internal-working)
5. [Kafka with Spring Boot](#kafka-with-spring-boot)
6. [Advanced Topics](#advanced-topics)
7. [Performance Tuning](#performance-tuning)
8. [Monitoring and Operations](#monitoring-and-operations)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction to Apache Kafka

Apache Kafka is a distributed streaming platform designed for high-throughput, fault-tolerant, real-time data streaming. Originally developed by LinkedIn, it's now an Apache Software Foundation project.

### Key Characteristics
- **High Throughput**: Millions of messages per second
- **Low Latency**: Sub-millisecond latency
- **Fault Tolerant**: Replication and distributed architecture
- **Scalable**: Horizontal scaling across multiple servers
- **Durable**: Persistent storage with configurable retention

### Use Cases
- **Real-time Analytics**: Stream processing for immediate insights
- **Event Sourcing**: Capturing state changes as events
- **Log Aggregation**: Centralized logging from multiple services
- **Message Queuing**: Decoupling microservices
- **Data Integration**: ETL pipelines and data synchronization

## Kafka Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Broker 1  │  │   Broker 2  │  │   Broker 3  │        │
│  │             │  │             │  │             │        │
│  │ Topic A     │  │ Topic A     │  │ Topic A     │        │
│  │ Partition 0 │  │ Partition 1 │  │ Partition 2 │        │
│  │             │  │             │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
           ▲                                    ▲
           │                                    │
    ┌─────────────┐                    ┌─────────────┐
    │  Producers  │                    │  Consumers  │
    │             │                    │             │
    │ App 1       │                    │ App A       │
    │ App 2       │                    │ App B       │
    │ App 3       │                    │ App C       │
    └─────────────┘                    └─────────────┘
```

### Components Overview

#### 1. **Broker**
- Kafka server that stores and serves data
- Each broker identified by unique ID
- Can handle thousands of partitions

#### 2. **Topic**
- Logical channel for messages
- Divided into partitions for scalability
- Messages are immutable once written

#### 3. **Partition**
- Ordered sequence of messages
- Each message has unique offset
- Enables parallel processing

#### 4. **Producer**
- Publishes messages to topics
- Chooses partition (round-robin, key-based, custom)
- Can batch messages for efficiency

#### 5. **Consumer**
- Reads messages from topics
- Maintains offset position
- Can be part of consumer group

#### 6. **ZooKeeper** (Legacy) / **KRaft** (New)
- Cluster coordination and metadata management
- Leader election for partitions
- Configuration management

## Core Concepts

### Topics and Partitions

```java
// Topic: user-events
// Partitions: 3

Partition 0: [msg0] [msg3] [msg6] [msg9]  ...
Partition 1: [msg1] [msg4] [msg7] [msg10] ...
Partition 2: [msg2] [msg5] [msg8] [msg11] ...
             ↑
           Offset
```

### Message Structure

```json
{
  "offset": 12345,
  "timestamp": 1640995200000,
  "key": "user-123",
  "value": "{\"userId\":123,\"action\":\"login\"}",
  "headers": {
    "source": "web-app",
    "version": "1.0"
  }
}
```

### Replication

```
Topic: orders (Replication Factor: 3)

Partition 0:
├── Leader:   Broker 1 [msg0, msg3, msg6]
├── Replica:  Broker 2 [msg0, msg3, msg6]
└── Replica:  Broker 3 [msg0, msg3, msg6]

Partition 1:
├── Leader:   Broker 2 [msg1, msg4, msg7]
├── Replica:  Broker 3 [msg1, msg4, msg7]
└── Replica:  Broker 1 [msg1, msg4, msg7]
```

## Internal Working

### Message Storage

#### Log Segments
```
/kafka-logs/topic-partition/
├── 00000000000000000000.log    # Active segment
├── 00000000000000001000.log    # Older segment
├── 00000000000000002000.log    # Older segment
├── 00000000000000000000.index  # Offset index
├── 00000000000000001000.index  # Offset index
└── 00000000000000000000.timeindex # Time index
```

#### Message Format (On Disk)
```
┌─────────────────────────────────────────────────────────┐
│ Message Set                                             │
├─────────────────────────────────────────────────────────┤
│ Offset (8 bytes) │ Message Size (4 bytes) │ Message    │
├─────────────────────────────────────────────────────────┤
│ CRC (4 bytes) │ Magic (1 byte) │ Attributes (1 byte)   │
├─────────────────────────────────────────────────────────┤
│ Key Length (4 bytes) │ Key │ Value Length │ Value      │
└─────────────────────────────────────────────────────────┘
```

### Producer Internals

#### Producer Flow
```
Producer → Serializer → Partitioner → RecordAccumulator → Sender → Broker
```

#### Batching and Compression
```java
// Producer batches messages for efficiency
Properties props = new Properties();
props.put("batch.size", 16384);           // 16KB batch size
props.put("linger.ms", 5);                // Wait 5ms for more messages
props.put("compression.type", "snappy");   // Compress batches
```

### Consumer Internals

#### Consumer Group Coordination
```
Consumer Group: payment-processors

┌─────────────────────────────────────────────────────────┐
│ Topic: payments (6 partitions)                         │
├─────────────────────────────────────────────────────────┤
│ Consumer 1: Partitions [0, 1]                          │
│ Consumer 2: Partitions [2, 3]                          │
│ Consumer 3: Partitions [4, 5]                          │
└─────────────────────────────────────────────────────────┘
```

#### Offset Management
```java
// Offset commit strategies
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "1000");

// Manual commit
consumer.commitSync();  // Synchronous
consumer.commitAsync(); // Asynchronous
```

### Replication Protocol

#### ISR (In-Sync Replicas)
```
Partition Leader: Broker 1
ISR: [1, 2, 3]  # All replicas in sync

# If Broker 3 falls behind:
ISR: [1, 2]     # Broker 3 removed from ISR

# High Water Mark (HWM): Last offset replicated to all ISR
# Log End Offset (LEO): Last offset in leader's log
```

#### Leader Election
```java
// When leader fails:
1. Controller detects leader failure
2. Selects new leader from ISR
3. Updates metadata in ZooKeeper/KRaft
4. Notifies all brokers
5. Clients discover new leader
```

## Kafka with Spring Boot

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Configuration

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Performance tuning
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Consumer tuning
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
```

### Producer Implementation

```java
@Service
public class PaymentEventProducer {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishPaymentEvent(PaymentEvent event) {
        try {
            // Send with callback
            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("payment-events", event.getPaymentId(), event);
            
            future.addCallback(
                result -> log.info("Payment event sent: {}", result.getRecordMetadata()),
                failure -> log.error("Failed to send payment event", failure)
            );
        } catch (Exception e) {
            log.error("Error publishing payment event", e);
        }
    }
    
    // Transactional producer
    @Transactional
    public void publishPaymentEventTransactional(PaymentEvent event) {
        kafkaTemplate.send("payment-events", event.getPaymentId(), event);
        kafkaTemplate.send("audit-events", event.getPaymentId(), 
            new AuditEvent("PAYMENT_PUBLISHED", event.getPaymentId()));
    }
}
```

### Consumer Implementation

```java
@Component
public class PaymentEventConsumer {
    
    @KafkaListener(topics = "payment-events", groupId = "payment-processor")
    public void handlePaymentEvent(
            @Payload PaymentEvent event,
            @Header Map<String, Object> headers,
            Acknowledgment ack) {
        
        try {
            log.info("Processing payment event: {}", event);
            
            // Process the payment
            processPayment(event);
            
            // Manual acknowledgment
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing payment event", e);
            // Don't acknowledge - message will be retried
        }
    }
    
    // Batch consumer
    @KafkaListener(topics = "payment-events", groupId = "batch-processor")
    public void handlePaymentEventsBatch(List<PaymentEvent> events, Acknowledgment ack) {
        try {
            log.info("Processing {} payment events", events.size());
            
            // Batch processing
            processBatch(events);
            
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment events batch", e);
        }
    }
    
    // Error handling
    @KafkaListener(topics = "payment-events.DLT", groupId = "error-handler")
    public void handleFailedPaymentEvents(PaymentEvent event) {
        log.error("Processing failed payment event: {}", event);
        // Handle failed messages
        sendToManualReview(event);
    }
}
```

### Advanced Spring Kafka Features

#### Custom Serializers/Deserializers

```java
public class PaymentEventSerializer implements Serializer<PaymentEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public byte[] serialize(String topic, PaymentEvent data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing PaymentEvent", e);
        }
    }
}

public class PaymentEventDeserializer implements Deserializer<PaymentEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public PaymentEvent deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, PaymentEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing PaymentEvent", e);
        }
    }
}
```

#### Error Handling and Retry

```java
@Configuration
public class KafkaErrorHandlingConfig {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        // Retry configuration
        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback(recoveryCallback());
        
        // Error handler
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate()), 
            new FixedBackOff(1000L, 3)
        ));
        
        return factory;
    }
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
    
    @Bean
    public RecoveryCallback<Void> recoveryCallback() {
        return context -> {
            ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) context.getAttribute("record");
            log.error("Recovery callback for record: {}", record);
            // Send to DLQ or handle manually
            return null;
        };
    }
}
```

## Advanced Topics

### Kafka Streams

```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {
    
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "payment-stream-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        return new KafkaStreamsConfiguration(props);
    }
    
    @Bean
    public KStream<String, PaymentEvent> paymentStream(StreamsBuilder streamsBuilder) {
        KStream<String, PaymentEvent> stream = streamsBuilder
            .stream("payment-events", Consumed.with(Serdes.String(), paymentEventSerde()));
        
        // Filter high-value payments
        KStream<String, PaymentEvent> highValuePayments = stream
            .filter((key, payment) -> payment.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0);
        
        // Send to fraud detection
        highValuePayments.to("fraud-detection-events");
        
        // Aggregate payments by user
        KTable<String, BigDecimal> userTotals = stream
            .groupBy((key, payment) -> payment.getUserId())
            .aggregate(
                () -> BigDecimal.ZERO,
                (userId, payment, total) -> total.add(payment.getAmount()),
                Materialized.with(Serdes.String(), bigDecimalSerde())
            );
        
        return stream;
    }
}
```

### Kafka Connect

```json
{
  "name": "postgres-source-connector",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "connection.url": "jdbc:postgresql://localhost:5432/payments",
    "connection.user": "kafka",
    "connection.password": "kafka",
    "table.whitelist": "payments,transactions",
    "mode": "incrementing",
    "incrementing.column.name": "id",
    "topic.prefix": "postgres-",
    "poll.interval.ms": 1000
  }
}
```

### Schema Registry Integration

```java
@Configuration
public class SchemaRegistryConfig {
    
    @Bean
    public ProducerFactory<String, PaymentEvent> avroProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        
        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

## Performance Tuning

### Producer Tuning

```java
// Throughput optimization
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);      // 64KB
props.put(ProducerConfig.LINGER_MS_CONFIG, 20);          // Wait 20ms
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4"); // Fast compression
props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864); // 64MB buffer

// Latency optimization
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 0);          // No batching
props.put(ProducerConfig.LINGER_MS_CONFIG, 0);           // Send immediately
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none"); // No compression

// Reliability optimization
props.put(ProducerConfig.ACKS_CONFIG, "all");            // Wait for all replicas
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
```

### Consumer Tuning

```java
// Throughput optimization
props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);     // 50KB min fetch
props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);     // Wait 500ms
props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);     // 1000 records per poll

// Memory optimization
props.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 65536);      // 64KB
props.put(ConsumerConfig.SEND_BUFFER_CONFIG, 131072);        // 128KB
```

### Broker Tuning

```properties
# Server configuration
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Log configuration
num.partitions=3
default.replication.factor=3
min.insync.replicas=2
log.retention.hours=168
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000

# Performance
replica.fetch.max.bytes=1048576
message.max.bytes=1000000
replica.fetch.wait.max.ms=500
```

## Monitoring and Operations

### Key Metrics

```java
@Component
public class KafkaMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Producer metrics
    @EventListener
    public void handleProducerMetrics(ProducerMetricEvent event) {
        meterRegistry.gauge("kafka.producer.record.send.rate", event.getRecordSendRate());
        meterRegistry.gauge("kafka.producer.batch.size.avg", event.getBatchSizeAvg());
        meterRegistry.gauge("kafka.producer.request.latency.avg", event.getRequestLatencyAvg());
    }
    
    // Consumer metrics
    @EventListener
    public void handleConsumerMetrics(ConsumerMetricEvent event) {
        meterRegistry.gauge("kafka.consumer.records.consumed.rate", event.getRecordsConsumedRate());
        meterRegistry.gauge("kafka.consumer.fetch.latency.avg", event.getFetchLatencyAvg());
        meterRegistry.gauge("kafka.consumer.lag", event.getConsumerLag());
    }
}
```

### Health Checks

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        try {
            // Test producer connectivity
            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("health-check", "ping");
            
            SendResult<String, Object> result = future.get(5, TimeUnit.SECONDS);
            
            return Health.up()
                .withDetail("kafka.producer", "UP")
                .withDetail("partition", result.getRecordMetadata().partition())
                .withDetail("offset", result.getRecordMetadata().offset())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("kafka.producer", "DOWN")
                .withException(e)
                .build();
        }
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Apache Kafka and what are its main use cases?**

**Answer:** Apache Kafka is a distributed streaming platform designed for high-throughput, fault-tolerant, real-time data streaming. Main use cases include:
- Real-time analytics and stream processing
- Event sourcing and CQRS patterns
- Log aggregation from multiple services
- Message queuing between microservices
- Data integration and ETL pipelines
- Activity tracking and monitoring

**Q2: Explain the difference between a topic and a partition in Kafka.**

**Answer:** 
- **Topic**: A logical channel or category for messages. It's like a database table or folder.
- **Partition**: A topic is divided into partitions for scalability and parallelism. Each partition is an ordered, immutable sequence of messages. Partitions enable:
  - Parallel processing by multiple consumers
  - Horizontal scaling across brokers
  - Fault tolerance through replication

**Q3: What is the role of ZooKeeper in Kafka?**

**Answer:** ZooKeeper (being replaced by KRaft) manages:
- Cluster membership and broker discovery
- Topic and partition metadata
- Leader election for partitions
- Configuration management
- Access control lists (ACLs)
- Consumer group coordination (legacy)

### Intermediate Level

**Q4: Explain Kafka's replication mechanism and ISR.**

**Answer:** 
Kafka replication ensures fault tolerance:
- Each partition has one leader and multiple followers
- **ISR (In-Sync Replicas)**: Replicas that are caught up with the leader
- Only ISR members can become leaders
- Messages are considered committed when replicated to all ISR members
- If a replica falls behind, it's removed from ISR
- `min.insync.replicas` ensures minimum replicas before accepting writes

```java
// Example: Topic with replication factor 3, min.insync.replicas = 2
// Leader: Broker 1, Followers: Broker 2, 3
// ISR: [1, 2, 3] - all in sync
// If Broker 3 fails: ISR: [1, 2] - still accepts writes
// If Broker 2 also fails: ISR: [1] - rejects writes (< min.insync.replicas)
```

**Q5: How does Kafka ensure exactly-once semantics?**

**Answer:** Kafka achieves exactly-once through:
1. **Idempotent Producer**: Prevents duplicate messages
   ```java
   props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
   ```
2. **Transactional Producer**: Atomic writes across partitions
   ```java
   producer.initTransactions();
   producer.beginTransaction();
   producer.send(record1);
   producer.send(record2);
   producer.commitTransaction();
   ```
3. **Consumer Isolation**: Read only committed messages
   ```java
   props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
   ```

**Q6: Explain consumer groups and partition assignment strategies.**

**Answer:** 
Consumer groups enable scalable message consumption:
- Each partition assigned to exactly one consumer in a group
- Multiple groups can consume the same topic independently
- Rebalancing occurs when consumers join/leave

**Assignment Strategies:**
1. **Range**: Assigns consecutive partitions (default)
2. **Round Robin**: Distributes partitions evenly
3. **Sticky**: Minimizes partition movement during rebalancing
4. **Cooperative Sticky**: Incremental rebalancing

```java
props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, 
    "org.apache.kafka.clients.consumer.StickyAssignor");
```

### Advanced Level

**Q7: How would you handle a slow consumer in Kafka?**

**Answer:** Multiple strategies:

1. **Increase Consumer Instances**
   ```java
   // Add more consumers to the group
   factory.setConcurrency(10); // 10 consumer threads
   ```

2. **Optimize Consumer Configuration**
   ```java
   props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
   props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
   props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
   ```

3. **Async Processing**
   ```java
   @KafkaListener(topics = "events")
   public void handleEvent(Event event) {
       CompletableFuture.runAsync(() -> processEvent(event));
   }
   ```

4. **Batch Processing**
   ```java
   @KafkaListener(topics = "events")
   public void handleEvents(List<Event> events) {
       processBatch(events); // Process multiple events together
   }
   ```

**Q8: Design a fault-tolerant payment processing system using Kafka.**

**Answer:**

```java
@Service
public class PaymentProcessor {
    
    // Idempotent processing with database
    @Transactional
    @KafkaListener(topics = "payment-requests")
    public void processPayment(PaymentRequest request, Acknowledgment ack) {
        try {
            // Check if already processed (idempotency)
            if (paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
                ack.acknowledge();
                return;
            }
            
            // Process payment
            Payment payment = paymentService.processPayment(request);
            
            // Publish success event
            kafkaTemplate.send("payment-success", payment);
            
            ack.acknowledge();
            
        } catch (InsufficientFundsException e) {
            // Publish failure event
            kafkaTemplate.send("payment-failed", 
                new PaymentFailure(request.getId(), "INSUFFICIENT_FUNDS"));
            ack.acknowledge(); // Don't retry
            
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            // Don't acknowledge - will be retried
        }
    }
}
```

**Architecture:**
```
Payment Requests → Kafka → Payment Processor → Database
                                    ↓
                            Success/Failure Events → Notification Service
                                    ↓
                            Dead Letter Queue → Manual Review
```

**Q9: How would you implement event sourcing with Kafka?**

**Answer:**

```java
@Entity
public class Account {
    private String id;
    private BigDecimal balance;
    private Long version;
    
    // Apply events to rebuild state
    public void apply(AccountEvent event) {
        switch (event.getType()) {
            case ACCOUNT_CREATED:
                this.balance = BigDecimal.ZERO;
                break;
            case MONEY_DEPOSITED:
                this.balance = this.balance.add(event.getAmount());
                break;
            case MONEY_WITHDRAWN:
                this.balance = this.balance.subtract(event.getAmount());
                break;
        }
        this.version = event.getVersion();
    }
}

@Service
public class AccountEventStore {
    
    @Autowired
    private KafkaTemplate<String, AccountEvent> kafkaTemplate;
    
    public void saveEvent(AccountEvent event) {
        // Store event in Kafka (event log)
        kafkaTemplate.send("account-events", event.getAccountId(), event);
    }
    
    public Account rebuildAccount(String accountId) {
        Account account = new Account(accountId);
        
        // Replay all events for this account
        List<AccountEvent> events = getEventsForAccount(accountId);
        events.forEach(account::apply);
        
        return account;
    }
}
```

**Q10: Explain Kafka's log compaction and when to use it.**

**Answer:**
Log compaction retains only the latest value for each key:

```properties
# Topic configuration
log.cleanup.policy=compact
log.segment.ms=604800000  # 7 days
log.min.cleanable.dirty.ratio=0.5
```

**Use Cases:**
- User profiles (latest state per user)
- Configuration changes
- Database change streams (CDC)

**Example:**
```
Before compaction:
Key A: v1 → v2 → v3 → v4
Key B: v1 → v2
Key C: v1

After compaction:
Key A: v4  (only latest)
Key B: v2  (only latest)  
Key C: v1  (only latest)
```

### Expert Level

**Q11: How would you design a multi-region Kafka deployment?**

**Answer:**

```yaml
# Region 1 (Primary)
kafka-cluster-us-east:
  brokers: [broker1, broker2, broker3]
  replication.factor: 3
  min.insync.replicas: 2

# Region 2 (Secondary)  
kafka-cluster-us-west:
  brokers: [broker4, broker5, broker6]
  replication.factor: 3
  min.insync.replicas: 2

# Cross-region replication
mirror-maker-2:
  source.cluster: us-east
  target.cluster: us-west
  topics: payment-events, user-events
  replication.factor: 3
```

**Strategies:**
1. **Active-Passive**: One region handles writes, other for DR
2. **Active-Active**: Both regions handle writes (complex conflict resolution)
3. **Regional Isolation**: Each region handles local traffic

**Q12: Implement a custom partitioner for optimal load distribution.**

**Answer:**

```java
public class CustomPartitioner implements Partitioner {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        
        if (key == null) {
            // Round-robin for null keys
            return counter.getAndIncrement() % numPartitions;
        }
        
        // Custom logic based on key
        if (key instanceof String) {
            String stringKey = (String) key;
            
            // Route high-priority users to specific partitions
            if (stringKey.startsWith("VIP_")) {
                return 0; // Dedicated partition for VIP users
            }
            
            // Hash-based partitioning for regular users
            return Math.abs(stringKey.hashCode()) % (numPartitions - 1) + 1;
        }
        
        return Math.abs(key.hashCode()) % numPartitions;
    }
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Configuration if needed
    }
    
    @Override
    public void close() {
        // Cleanup if needed
    }
}
```

## Best Practices

### Producer Best Practices

```java
// 1. Use appropriate serialization
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

// 2. Enable idempotence for exactly-once
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

// 3. Optimize for throughput vs latency
// High throughput
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
props.put(ProducerConfig.LINGER_MS_CONFIG, 20);
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

// Low latency
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 0);
props.put(ProducerConfig.LINGER_MS_CONFIG, 0);

// 4. Handle failures gracefully
kafkaTemplate.send(topic, key, value)
    .addCallback(
        result -> log.info("Message sent successfully"),
        failure -> {
            log.error("Failed to send message", failure);
            // Implement fallback logic
        }
    );
```

### Consumer Best Practices

```java
// 1. Use manual acknowledgment for reliability
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

@KafkaListener(topics = "events")
public void handleEvent(Event event, Acknowledgment ack) {
    try {
        processEvent(event);
        ack.acknowledge(); // Only ack after successful processing
    } catch (Exception e) {
        log.error("Processing failed", e);
        // Don't acknowledge - message will be retried
    }
}

// 2. Implement proper error handling
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    dltStrategy = DltStrategy.FAIL_ON_ERROR
)
@KafkaListener(topics = "events")
public void handleEvent(Event event) {
    processEvent(event);
}

// 3. Monitor consumer lag
@Component
public class ConsumerLagMonitor {
    
    @Scheduled(fixedRate = 30000)
    public void monitorLag() {
        // Check consumer lag and alert if too high
        Map<TopicPartition, Long> lag = getConsumerLag();
        lag.forEach((tp, lagValue) -> {
            if (lagValue > 10000) {
                alertService.sendAlert("High consumer lag: " + tp + " = " + lagValue);
            }
        });
    }
}
```

### Topic Design Best Practices

```java
// 1. Choose appropriate partition count
// Rule of thumb: (Target Throughput / Partition Throughput)
// Consider: Consumer parallelism, Broker count, Replication overhead

// 2. Use meaningful topic names
// Good: user.profile.updates, payment.transactions, order.events
// Bad: topic1, data, events

// 3. Set appropriate retention
Properties topicConfig = new Properties();
topicConfig.put("retention.ms", "604800000"); // 7 days
topicConfig.put("retention.bytes", "1073741824"); // 1GB
topicConfig.put("cleanup.policy", "delete"); // or "compact"

// 4. Configure replication properly
topicConfig.put("replication.factor", "3");
topicConfig.put("min.insync.replicas", "2");
```

### Security Best Practices

```java
// 1. Enable SSL/TLS
props.put("security.protocol", "SSL");
props.put("ssl.truststore.location", "/path/to/truststore.jks");
props.put("ssl.truststore.password", "password");
props.put("ssl.keystore.location", "/path/to/keystore.jks");
props.put("ssl.keystore.password", "password");

// 2. Use SASL for authentication
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism", "PLAIN");
props.put("sasl.jaas.config", 
    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
    "username=\"user\" password=\"password\";");

// 3. Implement ACLs
// kafka-acls.sh --authorizer-properties zookeeper.connect=localhost:2181 \
//   --add --allow-principal User:alice --operation Read --topic payments
```

This comprehensive guide covers Kafka's internal workings, Spring Boot integration, advanced concepts, and practical interview questions. The examples demonstrate real-world usage patterns and best practices for building robust, scalable systems with Kafka.