# Kafka Consumer Lag - Handling High-Speed Producers

## Overview

**Problem**: Kafka producer produces messages faster than consumer can process → Consumer lag increases → Messages pile up → Potential data loss or delayed processing.

**Consumer Lag** = (Latest Offset - Current Offset) = Number of unprocessed messages

---

## Understanding Consumer Lag

### Visual Representation

```
Producer: ████████████████████████████ (Offset: 10000)
Consumer: ████████░░░░░░░░░░░░░░░░░░░░ (Offset: 3000)
Lag:      ░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (7000 messages behind)
```

**Causes**:
- Producer rate > Consumer processing rate
- Slow consumer processing (DB calls, API calls)
- Insufficient consumer instances
- Network issues
- Consumer downtime

---

## Solutions Comparison

| Solution | Complexity | Effectiveness | Cost | Use Case |
|----------|-----------|---------------|------|----------|
| **Scale Consumers** | Low | High | Medium | Most common |
| **Increase Partitions** | Medium | High | Low | Parallelism |
| **Batch Processing** | Low | Medium | Low | Throughput |
| **Async Processing** | Medium | High | Low | I/O-bound tasks |
| **Optimize Consumer** | Medium | High | Low | Always do this |
| **Rate Limiting Producer** | Low | Medium | Low | Controlled flow |
| **Dead Letter Queue** | Medium | Medium | Low | Error handling |
| **Stream Processing** | High | Very High | High | Real-time |

---

## Solution 1: Scale Consumers (Horizontal Scaling)

### Add More Consumer Instances

```java
// Consumer Configuration
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Optimize for throughput
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Fetch more records
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024); // 1MB
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Wait 500ms
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
}

// Consumer Service
@Service
public class OrderConsumerService {
    
    @KafkaListener(
        topics = "orders",
        groupId = "order-consumer-group",
        concurrency = "10" // 10 concurrent consumers
    )
    public void consume(Order order) {
        processOrder(order);
    }
}
```

**Scaling Formula**:
```
Max Consumers = Number of Partitions
```

**Example**:
- Topic has 20 partitions → Max 20 consumers in same group
- Each consumer processes 1 partition

---

## Solution 2: Increase Partitions

### Add More Partitions for Parallelism

```bash
# Check current partitions
kafka-topics.sh --describe --topic orders --bootstrap-server localhost:9092

# Increase partitions (cannot decrease)
kafka-topics.sh --alter --topic orders --partitions 50 --bootstrap-server localhost:9092
```

**Before** (10 partitions, 10 consumers):
```
Producer: 10,000 msg/sec
Consumer: 1,000 msg/sec per consumer = 10,000 msg/sec total
Lag: 0 (balanced)
```

**After** (50 partitions, 50 consumers):
```
Producer: 10,000 msg/sec
Consumer: 1,000 msg/sec per consumer = 50,000 msg/sec total
Lag: 0 (over-provisioned, can handle spikes)
```

---

## Solution 3: Batch Processing

### Process Multiple Records Together

```java
@Service
public class BatchOrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "batch-consumer")
    public void consumeBatch(List<Order> orders) {
        // Process in batch (more efficient)
        processBatch(orders);
    }
    
    private void processBatch(List<Order> orders) {
        // Batch database insert
        orderRepository.saveAll(orders);
        
        // Batch API call
        externalService.notifyBatch(orders);
    }
}

// Configuration
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> batchFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setBatchListener(true); // Enable batch mode
    factory.getContainerProperties().setPollTimeout(3000);
    return factory;
}
```

**Performance Improvement**:
```
Single Processing: 1000 msg/sec (1ms per message)
Batch Processing: 5000 msg/sec (0.2ms per message)
```

---

## Solution 4: Async Processing with Thread Pool

### Non-blocking Consumer Processing

```java
@Service
public class AsyncOrderConsumer {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(50);
    
    @KafkaListener(
        topics = "orders",
        groupId = "async-consumer",
        concurrency = "10"
    )
    public void consume(Order order, Acknowledgment ack) {
        // Submit to thread pool (non-blocking)
        executor.submit(() -> {
            try {
                processOrder(order);
                ack.acknowledge(); // Manual commit after processing
            } catch (Exception e) {
                log.error("Processing failed: {}", e.getMessage());
            }
        });
    }
    
    private void processOrder(Order order) {
        // Slow I/O operations
        saveToDatabase(order);
        callExternalAPI(order);
        sendNotification(order);
    }
}

// Configuration for manual acknowledgment
@Bean
public ConsumerFactory<String, Order> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
    config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
    return new DefaultKafkaConsumerFactory<>(config);
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setAckMode(AckMode.MANUAL); // Manual ack
    return factory;
}
```

**Performance**:
```
Sync Processing: 1000 msg/sec (blocked by I/O)
Async Processing: 5000 msg/sec (parallel I/O)
```

---

## Solution 5: Optimize Consumer Code

### Performance Optimization Techniques

```java
@Service
public class OptimizedConsumer {
    
    // 1. Connection pooling
    @Autowired
    private HikariDataSource dataSource; // Connection pool
    
    // 2. Caching
    @Cacheable("products")
    public Product getProduct(String id) {
        return productRepository.findById(id);
    }
    
    // 3. Batch database operations
    @KafkaListener(topics = "orders", groupId = "optimized-consumer")
    public void consumeBatch(List<Order> orders) {
        // Batch insert instead of individual inserts
        jdbcTemplate.batchUpdate(
            "INSERT INTO orders (id, customer_id, amount) VALUES (?, ?, ?)",
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Order order = orders.get(i);
                    ps.setString(1, order.getId());
                    ps.setString(2, order.getCustomerId());
                    ps.setBigDecimal(3, order.getAmount());
                }
                
                @Override
                public int getBatchSize() {
                    return orders.size();
                }
            }
        );
    }
    
    // 4. Avoid unnecessary processing
    @KafkaListener(topics = "orders", groupId = "filtered-consumer")
    public void consume(Order order) {
        // Filter early
        if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return; // Skip invalid orders
        }
        
        // Process only necessary fields
        processOrder(order);
    }
    
    // 5. Use WebClient for non-blocking HTTP calls
    @Autowired
    private WebClient webClient;
    
    public void notifyExternal(Order order) {
        webClient.post()
            .uri("/orders")
            .bodyValue(order)
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(); // Non-blocking
    }
}
```

---

## Solution 6: Rate Limiting Producer

### Control Producer Speed

```java
@Service
public class RateLimitedProducer {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    // Rate limiter: 1000 messages per second
    private final RateLimiter rateLimiter = RateLimiter.create(1000.0);
    
    public void sendOrder(Order order) {
        // Wait if rate limit exceeded
        rateLimiter.acquire();
        
        kafkaTemplate.send("orders", order.getId(), order);
    }
}

// Alternative: Token Bucket with Semaphore
@Service
public class ThrottledProducer {
    
    private final Semaphore semaphore = new Semaphore(100); // Max 100 in-flight
    
    public void sendOrder(Order order) throws InterruptedException {
        semaphore.acquire(); // Wait if limit reached
        
        kafkaTemplate.send("orders", order.getId(), order)
            .addCallback(
                result -> semaphore.release(),
                ex -> semaphore.release()
            );
    }
}
```

---

## Solution 7: Dead Letter Queue (DLQ)

### Handle Failed Messages Separately

```java
@Service
public class ConsumerWithDLQ {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    @KafkaListener(topics = "orders", groupId = "dlq-consumer")
    public void consume(Order order, Acknowledgment ack) {
        try {
            processOrder(order);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Processing failed, sending to DLQ: {}", e.getMessage());
            
            // Send to Dead Letter Queue
            kafkaTemplate.send("orders-dlq", order.getId(), order);
            ack.acknowledge(); // Acknowledge to move forward
        }
    }
}

// Separate consumer for DLQ (slower processing, retries)
@Service
public class DLQConsumer {
    
    @KafkaListener(topics = "orders-dlq", groupId = "dlq-processor")
    public void processDLQ(Order order) {
        // Retry with exponential backoff
        retryWithBackoff(order, 3);
    }
    
    private void retryWithBackoff(Order order, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                processOrder(order);
                return;
            } catch (Exception e) {
                long delay = (long) Math.pow(2, i) * 1000; // 1s, 2s, 4s
                Thread.sleep(delay);
            }
        }
        // Send to manual review queue
        kafkaTemplate.send("orders-failed", order.getId(), order);
    }
}
```

---

## Solution 8: Consumer Configuration Tuning

### Optimize Kafka Consumer Settings

```java
@Configuration
public class OptimizedKafkaConfig {
    
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Basic config
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "optimized-consumer");
        
        // Throughput optimization
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Fetch 500 records
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024); // 1MB min
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Wait 500ms
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10 * 1024 * 1024); // 10MB
        
        // Session management
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30s
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10s
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5min
        
        // Offset management
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
}
```

**Configuration Impact**:
```
Default Config: 1000 msg/sec
Optimized Config: 5000 msg/sec (5x improvement)
```

---

## Solution 9: Stream Processing (Kafka Streams)

### Real-time Stream Processing

```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {
    
    @Bean
    public KStream<String, Order> processOrders(StreamsBuilder builder) {
        KStream<String, Order> orders = builder.stream("orders");
        
        // Parallel processing with multiple threads
        orders
            .filter((key, order) -> order.getAmount().compareTo(BigDecimal.ZERO) > 0)
            .mapValues(order -> {
                // Transform
                order.setProcessedAt(LocalDateTime.now());
                return order;
            })
            .to("orders-processed");
        
        return orders;
    }
    
    @Bean
    public Properties streamsConfig() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-stream-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 10); // 10 threads
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "at_least_once");
        return props;
    }
}
```

---

## Monitoring Consumer Lag

### Lag Monitoring Service

```java
@Service
public class ConsumerLagMonitor {
    
    @Autowired
    private KafkaAdmin kafkaAdmin;
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorLag() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            
            // Get consumer group offsets
            Map<TopicPartition, OffsetAndMetadata> offsets = adminClient
                .listConsumerGroupOffsets("order-consumer-group")
                .partitionsToOffsetAndMetadata()
                .get();
            
            // Get end offsets (latest)
            Map<TopicPartition, Long> endOffsets = adminClient
                .listOffsets(offsets.keySet().stream()
                    .collect(Collectors.toMap(
                        tp -> tp,
                        tp -> OffsetSpec.latest()
                    )))
                .all()
                .get()
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().offset()
                ));
            
            // Calculate lag
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long currentOffset = entry.getValue().offset();
                long endOffset = endOffsets.get(tp);
                long lag = endOffset - currentOffset;
                
                log.info("Partition {}: Lag = {}", tp.partition(), lag);
                
                // Alert if lag > threshold
                if (lag > 10000) {
                    sendAlert("High consumer lag detected: " + lag);
                }
            }
        } catch (Exception e) {
            log.error("Failed to monitor lag", e);
        }
    }
}
```

---

## Real-World Example: E-commerce Order Processing

### Complete Solution with Multiple Strategies

```java
@Service
public class OrderProcessingService {
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(50);
    
    // Strategy 1: Batch processing
    @KafkaListener(
        topics = "orders",
        groupId = "order-processor",
        concurrency = "20", // 20 consumers
        containerFactory = "batchFactory"
    )
    public void processBatch(List<Order> orders, Acknowledgment ack) {
        try {
            // Strategy 2: Async processing
            CompletableFuture<Void> dbFuture = CompletableFuture.runAsync(() -> {
                orderRepository.saveAll(orders); // Batch insert
            }, executor);
            
            CompletableFuture<Void> notificationFuture = CompletableFuture.runAsync(() -> {
                sendBatchNotifications(orders);
            }, executor);
            
            // Wait for both
            CompletableFuture.allOf(dbFuture, notificationFuture).get(5, TimeUnit.SECONDS);
            
            ack.acknowledge();
            
        } catch (TimeoutException e) {
            log.error("Processing timeout, sending to DLQ");
            // Strategy 3: DLQ for failures
            orders.forEach(order -> kafkaTemplate.send("orders-dlq", order));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Processing failed", e);
            // Don't acknowledge - will retry
        }
    }
    
    private void sendBatchNotifications(List<Order> orders) {
        // Batch API call
        notificationService.sendBatch(orders);
    }
}

// Configuration
@Configuration
public class KafkaConfig {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> batchFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        
        return factory;
    }
}
```

**Performance Results**:
```
Before Optimization:
- Producer: 10,000 msg/sec
- Consumer: 1,000 msg/sec
- Lag: 9,000 msg/sec (growing)

After Optimization:
- Producer: 10,000 msg/sec
- Consumer: 15,000 msg/sec (20 instances × 500 batch × async)
- Lag: 0 (catching up)
```

---

## Best Practices

### ✅ Do's

```java
// 1. Scale consumers horizontally
concurrency = "20"

// 2. Use batch processing
factory.setBatchListener(true);

// 3. Optimize fetch settings
config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

// 4. Use async processing for I/O
CompletableFuture.runAsync(() -> processOrder(order), executor);

// 5. Monitor lag continuously
@Scheduled(fixedRate = 60000)
public void monitorLag() { ... }

// 6. Use DLQ for failures
kafkaTemplate.send("orders-dlq", failedOrder);

// 7. Manual commit for reliability
factory.getContainerProperties().setAckMode(AckMode.MANUAL);
```

---

### ❌ Don'ts

```java
// 1. Don't block consumer thread
Thread.sleep(10000); // Blocks partition

// 2. Don't use auto-commit with slow processing
config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true); // Risky

// 3. Don't ignore lag alerts
if (lag > 10000) {
    // Do nothing // Bad!
}

// 4. Don't process synchronously
processOrder(order); // Blocks
callExternalAPI(order); // Blocks

// 5. Don't have more consumers than partitions
// 50 consumers for 10 partitions = 40 idle consumers
```

---

## Interview Questions

### Q1: What causes consumer lag in Kafka?

**Answer**: Producer rate > Consumer processing rate, slow consumer logic, insufficient consumers, network issues, consumer downtime.

---

### Q2: How to reduce consumer lag?

**Answer**: Scale consumers, increase partitions, batch processing, async processing, optimize consumer code, rate limit producer.

---

### Q3: What is the maximum number of consumers in a consumer group?

**Answer**: Maximum = Number of partitions. Extra consumers will be idle.

---

### Q4: How to monitor consumer lag?

**Answer**: Use AdminClient API, Kafka Manager, Burrow, or monitoring tools like Prometheus + Grafana.

---

### Q5: What is Dead Letter Queue (DLQ)?

**Answer**: Separate topic for failed messages that can't be processed, allowing main consumer to continue without blocking.

---

## Key Takeaways

1. **Scale consumers** = Number of partitions for max parallelism
2. **Batch processing** improves throughput 5x
3. **Async processing** prevents I/O blocking
4. **Optimize fetch settings** for better throughput
5. **Monitor lag** continuously with alerts
6. **Use DLQ** for failed messages
7. **Manual commit** for reliability
8. **Rate limit producer** if needed
9. **Increase partitions** for more parallelism
10. **Profile consumer code** to find bottlenecks

---

## Practice Problems

1. Design consumer that processes 100K messages/sec
2. Implement lag monitoring with alerting
3. Create DLQ pattern with retry logic
4. Optimize consumer from 1K to 10K msg/sec
5. Build rate-limited producer with backpressure
