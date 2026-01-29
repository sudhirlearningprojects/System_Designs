# CompletableFuture & Kafka Integration Guide

## Table of Contents
1. [CompletableFuture Internals](#completablefuture-internals)
2. [Kafka Producer with CompletableFuture](#kafka-producer-with-completablefuture)
3. [Kafka Consumer with CompletableFuture](#kafka-consumer-with-completablefuture)
4. [Real-World Production Examples](#real-world-production-examples)

---

## CompletableFuture Internals

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    CompletableFuture                        │
├─────────────────────────────────────────────────────────────┤
│  State: INCOMPLETE → COMPLETING → NORMAL/EXCEPTIONAL        │
│                                                             │
│  ┌──────────────┐      ┌──────────────┐                   │
│  │   result     │      │  exception   │                   │
│  │  (Object)    │      │ (Throwable)  │                   │
│  └──────────────┘      └──────────────┘                   │
│                                                             │
│  ┌──────────────────────────────────────────────────┐     │
│  │         Completion Stack (Linked List)           │     │
│  │  ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐  │     │
│  │  │ Node │ -> │ Node │ -> │ Node │ -> │ Node │  │     │
│  │  └──────┘    └──────┘    └──────┘    └──────┘  │     │
│  └──────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Internal Components

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    
    // Core fields
    volatile Object result;        // Holds result or exception
    volatile Completion stack;     // Linked list of dependent actions
    
    // States
    static final int INCOMPLETE = 0;
    static final int COMPLETING = 1;
    static final int NORMAL = 2;
    static final int EXCEPTIONAL = 3;
    
    // Completion node (internal linked list)
    abstract static class Completion extends ForkJoinTask<Void> {
        volatile Completion next;  // Next completion in stack
        abstract CompletableFuture<?> tryFire(int mode);
    }
}
```

### How It Works

**1. Creation**
```java
CompletableFuture<String> future = new CompletableFuture<>();
// State: INCOMPLETE, result: null, stack: null
```

**2. Chaining Operations**
```java
future.thenApply(s -> s.toUpperCase())
      .thenAccept(System.out::println);

// Creates completion nodes and adds to stack
// Stack: [thenAccept] -> [thenApply] -> null
```

**3. Completion**
```java
future.complete("hello");

// 1. Sets result = "hello"
// 2. Changes state to NORMAL
// 3. Pops stack and executes each completion node
// 4. Each node fires its dependent futures
```

### Thread Execution Modes

```java
// 1. Sync (same thread)
future.thenApply(x -> x * 2)

// 2. Async (ForkJoinPool.commonPool)
future.thenApplyAsync(x -> x * 2)

// 3. Async with custom executor
future.thenApplyAsync(x -> x * 2, customExecutor)
```

---

## Kafka Producer with CompletableFuture

### How Kafka Producer Returns CompletableFuture

Kafka's `send()` method returns a `Future`, which we convert to `CompletableFuture`:

```java
// Kafka Producer send() signature
public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
```

### Internal Flow

```
Producer.send()
    ↓
Creates ProducerRecord
    ↓
Serializes key/value
    ↓
Partitioner selects partition
    ↓
Adds to RecordAccumulator (buffer)
    ↓
Sender thread picks batch
    ↓
Sends to Kafka broker
    ↓
Receives acknowledgment
    ↓
Invokes callback
    ↓
Completes Future
```

### Basic Integration

```java
import org.apache.kafka.clients.producer.*;
import java.util.concurrent.CompletableFuture;

public class KafkaProducerService {
    
    private final KafkaProducer<String, String> producer;
    
    // Convert Kafka Future to CompletableFuture
    public CompletableFuture<RecordMetadata> sendAsync(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(metadata);
            }
        });
        
        return future;
    }
}
```

### Advanced Producer Patterns

#### 1. Send with Retry and Timeout

```java
public CompletableFuture<RecordMetadata> sendWithRetry(String topic, String key, String value) {
    return CompletableFuture
        .supplyAsync(() -> {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    future.completeExceptionally(exception);
                } else {
                    future.complete(metadata);
                }
            });
            
            return future;
        })
        .thenCompose(f -> f)
        .orTimeout(5, TimeUnit.SECONDS)
        .exceptionally(ex -> {
            log.error("Send failed, retrying...", ex);
            return sendWithRetry(topic, key, value).join();
        });
}
```

#### 2. Batch Send with CompletableFuture

```java
public CompletableFuture<List<RecordMetadata>> sendBatch(List<ProducerRecord<String, String>> records) {
    List<CompletableFuture<RecordMetadata>> futures = records.stream()
        .map(record -> {
            CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    future.completeExceptionally(exception);
                } else {
                    future.complete(metadata);
                }
            });
            return future;
        })
        .collect(Collectors.toList());
    
    // Wait for all to complete
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList()));
}
```

#### 3. Send with Circuit Breaker

```java
public class ResilientKafkaProducer {
    
    private final KafkaProducer<String, String> producer;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final int threshold = 5;
    private volatile boolean circuitOpen = false;
    
    public CompletableFuture<RecordMetadata> sendWithCircuitBreaker(String topic, String key, String value) {
        if (circuitOpen) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Circuit breaker is OPEN"));
        }
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                int failures = failureCount.incrementAndGet();
                if (failures >= threshold) {
                    circuitOpen = true;
                    scheduleCircuitReset();
                }
                future.completeExceptionally(exception);
            } else {
                failureCount.set(0);
                future.complete(metadata);
            }
        });
        
        return future;
    }
    
    private void scheduleCircuitReset() {
        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
            .execute(() -> {
                circuitOpen = false;
                failureCount.set(0);
            });
    }
}
```

---

## Kafka Consumer with CompletableFuture

### Consumer Processing Patterns

#### 1. Async Message Processing

```java
public class AsyncKafkaConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void startConsuming() {
        consumer.subscribe(Collections.singletonList("my-topic"));
        
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (ConsumerRecord<String, String> record : records) {
                CompletableFuture<Void> future = CompletableFuture
                    .supplyAsync(() -> processRecord(record), executor)
                    .exceptionally(ex -> {
                        log.error("Processing failed for offset: " + record.offset(), ex);
                        return null;
                    });
                
                futures.add(future);
            }
            
            // Wait for all messages in batch to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Commit offsets after all processing completes
            consumer.commitSync();
        }
    }
    
    private String processRecord(ConsumerRecord<String, String> record) {
        // Business logic
        return "processed";
    }
}
```

#### 2. Parallel Processing with Ordering Guarantee

```java
public class OrderedParallelConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final Map<Integer, ExecutorService> partitionExecutors = new ConcurrentHashMap<>();
    
    public void startConsuming() {
        consumer.subscribe(Collections.singletonList("my-topic"));
        
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            // Group by partition
            Map<Integer, List<ConsumerRecord<String, String>>> byPartition = new HashMap<>();
            records.forEach(record -> 
                byPartition.computeIfAbsent(record.partition(), k -> new ArrayList<>()).add(record)
            );
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // Process each partition sequentially, but partitions in parallel
            byPartition.forEach((partition, partitionRecords) -> {
                ExecutorService executor = partitionExecutors.computeIfAbsent(
                    partition, k -> Executors.newSingleThreadExecutor()
                );
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    partitionRecords.forEach(this::processRecord);
                }, executor);
                
                futures.add(future);
            });
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            consumer.commitSync();
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record) {
        // Business logic
    }
}
```

#### 3. Consumer with Retry and DLQ

```java
public class ResilientKafkaConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> dlqProducer;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final int maxRetries = 3;
    
    public void startConsuming() {
        consumer.subscribe(Collections.singletonList("my-topic"));
        
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (ConsumerRecord<String, String> record : records) {
                CompletableFuture<Void> future = processWithRetry(record, 0);
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            consumer.commitSync();
        }
    }
    
    private CompletableFuture<Void> processWithRetry(ConsumerRecord<String, String> record, int attempt) {
        return CompletableFuture
            .supplyAsync(() -> processRecord(record), executor)
            .exceptionally(ex -> {
                if (attempt < maxRetries) {
                    log.warn("Retry attempt {} for offset {}", attempt + 1, record.offset());
                    return processWithRetry(record, attempt + 1).join();
                } else {
                    log.error("Max retries exceeded, sending to DLQ", ex);
                    sendToDLQ(record, ex);
                    return null;
                }
            })
            .thenApply(v -> null);
    }
    
    private String processRecord(ConsumerRecord<String, String> record) {
        // Business logic that may throw exception
        return "processed";
    }
    
    private void sendToDLQ(ConsumerRecord<String, String> record, Throwable ex) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
            "my-topic-dlq",
            record.key(),
            record.value()
        );
        dlqRecord.headers().add("error", ex.getMessage().getBytes());
        dlqProducer.send(dlqRecord);
    }
}
```

---

## Real-World Production Examples

### Example 1: E-Commerce Order Processing

```java
@Service
public class OrderProcessingService {
    
    private final KafkaProducer<String, Order> orderProducer;
    private final KafkaConsumer<String, Order> orderConsumer;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    
    // Producer: Submit order asynchronously
    public CompletableFuture<OrderResult> submitOrder(Order order) {
        return sendToKafka("orders", order.getId(), order)
            .thenCompose(metadata -> processOrder(order))
            .thenCompose(result -> notifyCustomer(order, result))
            .exceptionally(ex -> {
                log.error("Order processing failed", ex);
                return OrderResult.failed(ex.getMessage());
            });
    }
    
    private CompletableFuture<RecordMetadata> sendToKafka(String topic, String key, Order order) {
        ProducerRecord<String, Order> record = new ProducerRecord<>(topic, key, order);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        orderProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(metadata);
            }
        });
        
        return future;
    }
    
    private CompletableFuture<OrderResult> processOrder(Order order) {
        // Chain multiple async operations
        return CompletableFuture
            .supplyAsync(() -> inventoryService.reserveItems(order))
            .thenCompose(reserved -> paymentService.processPayment(order))
            .thenApply(payment -> OrderResult.success(order.getId(), payment));
    }
    
    private CompletableFuture<OrderResult> notifyCustomer(Order order, OrderResult result) {
        return notificationService.sendEmail(order.getCustomerEmail(), result)
            .thenApply(v -> result);
    }
    
    // Consumer: Process orders from Kafka
    @KafkaListener(topics = "orders")
    public void consumeOrders(ConsumerRecord<String, Order> record) {
        Order order = record.value();
        
        CompletableFuture
            .supplyAsync(() -> processOrder(order))
            .thenCompose(result -> updateDatabase(order, result))
            .thenRun(() -> log.info("Order {} processed successfully", order.getId()))
            .exceptionally(ex -> {
                log.error("Failed to process order {}", order.getId(), ex);
                return null;
            });
    }
    
    private CompletableFuture<Void> updateDatabase(Order order, OrderResult result) {
        return CompletableFuture.runAsync(() -> {
            // Update order status in database
        });
    }
}
```

### Example 2: Real-Time Analytics Pipeline

```java
@Service
public class AnalyticsPipeline {
    
    private final KafkaProducer<String, Event> eventProducer;
    private final KafkaConsumer<String, Event> eventConsumer;
    private final ExecutorService executor = Executors.newFixedThreadPool(20);
    
    // Producer: Emit events
    public CompletableFuture<Void> emitEvent(Event event) {
        return CompletableFuture
            .supplyAsync(() -> enrichEvent(event), executor)
            .thenCompose(enriched -> sendToKafka("events", enriched))
            .thenAccept(metadata -> 
                log.info("Event sent to partition {} offset {}", 
                    metadata.partition(), metadata.offset())
            );
    }
    
    private Event enrichEvent(Event event) {
        // Add metadata, timestamps, etc.
        return event;
    }
    
    private CompletableFuture<RecordMetadata> sendToKafka(String topic, Event event) {
        ProducerRecord<String, Event> record = new ProducerRecord<>(topic, event.getId(), event);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        eventProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(metadata);
            }
        });
        
        return future;
    }
    
    // Consumer: Process events in parallel
    public void startConsuming() {
        eventConsumer.subscribe(Collections.singletonList("events"));
        
        while (true) {
            ConsumerRecords<String, Event> records = eventConsumer.poll(Duration.ofMillis(100));
            
            // Process events in parallel
            List<CompletableFuture<Void>> futures = StreamSupport
                .stream(records.spliterator(), false)
                .map(record -> CompletableFuture
                    .supplyAsync(() -> processEvent(record.value()), executor)
                    .thenCompose(this::aggregateMetrics)
                    .thenCompose(this::updateDashboard)
                    .exceptionally(ex -> {
                        log.error("Event processing failed", ex);
                        return null;
                    })
                )
                .collect(Collectors.toList());
            
            // Wait for all to complete before committing
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            eventConsumer.commitSync();
        }
    }
    
    private Event processEvent(Event event) {
        // Transform, filter, validate
        return event;
    }
    
    private CompletableFuture<Metrics> aggregateMetrics(Event event) {
        return CompletableFuture.supplyAsync(() -> {
            // Calculate metrics
            return new Metrics();
        }, executor);
    }
    
    private CompletableFuture<Void> updateDashboard(Metrics metrics) {
        return CompletableFuture.runAsync(() -> {
            // Update real-time dashboard
        }, executor);
    }
}
```

### Example 3: Microservices Communication

```java
@Service
public class UserService {
    
    private final KafkaProducer<String, UserEvent> producer;
    
    public CompletableFuture<User> createUser(User user) {
        return saveToDatabase(user)
            .thenCompose(savedUser -> publishUserCreatedEvent(savedUser))
            .thenApply(metadata -> user)
            .exceptionally(ex -> {
                log.error("User creation failed", ex);
                rollbackUser(user);
                throw new RuntimeException("Failed to create user", ex);
            });
    }
    
    private CompletableFuture<User> saveToDatabase(User user) {
        return CompletableFuture.supplyAsync(() -> {
            // Save to DB
            return user;
        });
    }
    
    private CompletableFuture<RecordMetadata> publishUserCreatedEvent(User user) {
        UserEvent event = new UserEvent("USER_CREATED", user);
        ProducerRecord<String, UserEvent> record = new ProducerRecord<>("user-events", user.getId(), event);
        
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(metadata);
            }
        });
        
        return future;
    }
    
    private void rollbackUser(User user) {
        // Compensating transaction
    }
}

@Service
public class EmailService {
    
    @KafkaListener(topics = "user-events")
    public void handleUserEvents(ConsumerRecord<String, UserEvent> record) {
        UserEvent event = record.value();
        
        if ("USER_CREATED".equals(event.getType())) {
            CompletableFuture
                .supplyAsync(() -> sendWelcomeEmail(event.getUser()))
                .thenRun(() -> log.info("Welcome email sent to {}", event.getUser().getEmail()))
                .exceptionally(ex -> {
                    log.error("Failed to send welcome email", ex);
                    return null;
                });
        }
    }
    
    private boolean sendWelcomeEmail(User user) {
        // Send email
        return true;
    }
}
```

---

## Best Practices

### 1. Thread Pool Management

```java
// Use dedicated thread pools for different operations
ExecutorService kafkaExecutor = Executors.newFixedThreadPool(10, 
    new ThreadFactoryBuilder().setNameFormat("kafka-worker-%d").build());

ExecutorService businessExecutor = Executors.newFixedThreadPool(20,
    new ThreadFactoryBuilder().setNameFormat("business-worker-%d").build());
```

### 2. Timeout Handling

```java
CompletableFuture<RecordMetadata> future = sendToKafka(record)
    .orTimeout(5, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            log.error("Kafka send timeout");
        }
        return null;
    });
```

### 3. Error Handling

```java
future
    .exceptionally(ex -> {
        if (ex instanceof SerializationException) {
            // Handle serialization error
        } else if (ex instanceof TimeoutException) {
            // Handle timeout
        } else if (ex instanceof InterruptException) {
            // Handle interruption
        }
        return null;
    });
```

### 4. Resource Cleanup

```java
@PreDestroy
public void cleanup() {
    producer.flush();
    producer.close(Duration.ofSeconds(5));
    consumer.close(Duration.ofSeconds(5));
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
}
```

---

## Performance Considerations

### 1. Batch Processing
- Process multiple messages in parallel
- Commit offsets after batch completion
- Use `CompletableFuture.allOf()` for synchronization

### 2. Backpressure Handling
- Limit concurrent CompletableFutures
- Use bounded thread pools
- Monitor queue sizes

### 3. Memory Management
- Avoid holding large objects in CompletableFuture chains
- Clear references after completion
- Use weak references for caching

---

## Summary

**CompletableFuture with Kafka Producer:**
- Convert Kafka's callback-based API to CompletableFuture
- Chain async operations (validation → send → log)
- Handle errors gracefully with `exceptionally()`

**CompletableFuture with Kafka Consumer:**
- Process messages asynchronously in parallel
- Maintain ordering per partition
- Commit offsets after all processing completes

**Key Benefits:**
- Non-blocking I/O
- Composable async operations
- Better error handling
- Improved throughput
- Cleaner code with functional style
