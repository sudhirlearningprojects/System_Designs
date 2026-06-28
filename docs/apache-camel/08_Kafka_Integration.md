# Module 8: Camel with Kafka (Deep Dive)

## 1. Kafka Consumer Patterns

### Basic Consumer

```java
from("kafka:order-events"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=order-service"
        + "&autoOffsetReset=earliest"
        + "&maxPollRecords=500"
        + "&pollTimeoutMs=1000"
        + "&consumersCount=3")           // 3 threads consuming
    .routeId("kafka-order-consumer")
    .unmarshal().json(JsonLibrary.Jackson, OrderEvent.class)
    .to("direct:process-order");
```

### Manual Offset Commit (Exactly-Once Processing)

```java
from("kafka:payments"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=payment-processor"
        + "&autoCommitEnable=false"         // Disable auto-commit
        + "&allowManualCommit=true"         // Enable manual commit
        + "&breakOnFirstError=true"         // Stop batch on error
        + "&maxPollRecords=100")
    .routeId("exactly-once-consumer")
    .unmarshal().json(JsonLibrary.Jackson, Payment.class)
    .doTry()
        .bean("paymentService", "process")
        .to("sql:INSERT INTO payments (id, amount, status) VALUES (:#${body.id}, :#${body.amount}, 'PROCESSED')")
        .process(exchange -> {
            // Commit ONLY after successful processing + DB write
            KafkaManualCommit commit = exchange.getIn()
                .getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
            if (commit != null) {
                commit.commit();  // Sync commit
            }
        })
    .doCatch(Exception.class)
        .log(LoggingLevel.ERROR, "Failed to process payment: ${exception.message}")
        // Offset NOT committed → message will be redelivered
        .to("kafka:payment-dlq")
    .end();
```

### Batch Consumer (Process multiple records at once)

```java
from("kafka:bulk-events"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=batch-processor"
        + "&maxPollRecords=1000"
        + "&batching=true"                  // Enable batch mode
        + "&maxPollIntervalMs=300000")
    .routeId("batch-consumer")
    .split(body())                          // body is List<Exchange> in batch mode
        .parallelProcessing()
        .to("direct:process-single")
    .end()
    .log("Batch of ${header.CamelSplitSize} processed");
```

### Consumer with Seek (Replay from specific offset)

```java
from("kafka:audit-events"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=audit-replay"
        + "&seekTo=beginning"              // Start from beginning
        + "&autoOffsetReset=earliest")
    .routeId("replay-consumer")
    .to("direct:rebuild-audit-view");

// Seek to specific timestamp
from("kafka:events"
        + "?brokers={{kafka.brokers}}"
        + "&seekTo=timestamp"
        + "&seekToTimestamp=1704067200000")  // From Jan 1, 2024
    .to("direct:reprocess");
```

---

## 2. Kafka Producer Patterns

### Basic Producer

```java
from("direct:publish-order")
    .marshal().json()
    .to("kafka:order-events"
        + "?brokers={{kafka.brokers}}"
        + "&requestRequiredAcks=all"        // Wait for all replicas
        + "&retries=3"
        + "&compressionCodec=lz4"
        + "&lingerMs=5"                     // Batch for 5ms
        + "&batchSize=65536");              // 64KB batch
```

### Producer with Key & Partition

```java
from("direct:publish-with-key")
    .setHeader(KafkaConstants.KEY, simple("${body.customerId}"))     // Partition by customer
    .setHeader(KafkaConstants.PARTITION_KEY, simple("${body.region}")) // Or by region
    .marshal().json()
    .to("kafka:customer-events");

// Explicit partition
from("direct:to-specific-partition")
    .setHeader(KafkaConstants.PARTITION, constant(0))  // Always partition 0
    .to("kafka:ordered-events");
```

### Producer with Headers

```java
from("direct:publish-with-headers")
    .process(exchange -> {
        exchange.getIn().setHeader(KafkaConstants.KEY, "order-123");
        
        // Kafka record headers (different from Camel headers)
        List<RecordHeader> kafkaHeaders = List.of(
            new RecordHeader("source", "order-service".getBytes()),
            new RecordHeader("version", "v2".getBytes()),
            new RecordHeader("correlation-id", UUID.randomUUID().toString().getBytes())
        );
        exchange.getIn().setHeader(KafkaConstants.OVERRIDE_HEADERS, kafkaHeaders);
    })
    .marshal().json()
    .to("kafka:enriched-events");
```

### Transactional Producer

```java
from("direct:transactional-publish")
    .to("kafka:events"
        + "?brokers={{kafka.brokers}}"
        + "&requestRequiredAcks=all"
        + "&transactionalId=order-producer-1"  // Enable transactions
        + "&enableIdempotence=true");          // Required for transactions
```

---

## 3. Idempotent Consumer with Kafka

### Using Kafka Topic as Idempotent Repository

```java
@Bean
public KafkaIdempotentRepository kafkaIdempotentRepo() {
    return new KafkaIdempotentRepository("idempotent-store", "localhost:9092");
}

from("kafka:payments")
    .idempotentConsumer(header("paymentId"))
        .idempotentRepository("kafkaIdempotentRepo")
        .skipDuplicate(true)
        .removeOnFailure(true)
    .bean("paymentService", "process")
    .to("kafka:payment-confirmations");
```

### Using Redis (Better for high throughput)

```java
@Bean
public RedisStringIdempotentRepository redisIdempotentRepo(
        RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.afterPropertiesSet();
    
    RedisStringIdempotentRepository repo = new RedisStringIdempotentRepository(template, "idemp:");
    repo.setExpiry(86400L);  // 24h TTL
    return repo;
}
```

---

## 4. Error Handling with Kafka

### Dead Letter Topic Pattern

```java
@Override
public void configure() {
    // Route errors to DLT with original headers preserved
    errorHandler(deadLetterChannel("kafka:order-events-dlq"
            + "?brokers={{kafka.brokers}}")
        .maximumRedeliveries(3)
        .redeliveryDelay(5000)
        .backOffMultiplier(2)
        .useOriginalMessage()
        .onExceptionOccurred(exchange -> {
            // Add error metadata before sending to DLQ
            exchange.getIn().setHeader("error-message", 
                exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage());
            exchange.getIn().setHeader("error-timestamp", Instant.now().toString());
            exchange.getIn().setHeader("error-route", exchange.getFromRouteId());
            exchange.getIn().setHeader("original-topic", 
                exchange.getIn().getHeader(KafkaConstants.TOPIC));
        }));

    from("kafka:order-events?brokers={{kafka.brokers}}&groupId=processor")
        .routeId("order-event-processor")
        .unmarshal().json(JsonLibrary.Jackson, OrderEvent.class)
        .bean("orderService", "handle");
}
```

### DLQ Reprocessor (Retry failed messages)

```java
from("kafka:order-events-dlq?brokers={{kafka.brokers}}&groupId=dlq-reprocessor")
    .routeId("dlq-reprocessor")
    .filter(simple("${header.error-timestamp} != null"))
    .process(exchange -> {
        // Only retry if error is < 1 hour old
        Instant errorTime = Instant.parse(exchange.getIn().getHeader("error-timestamp", String.class));
        if (Duration.between(errorTime, Instant.now()).toHours() > 1) {
            exchange.setRouteStop(true);  // Too old, skip
            return;
        }
    })
    .removeHeaders("error-*")  // Remove error headers
    .to("kafka:order-events");  // Re-publish to original topic
```

---

## 5. Consumer Group Rebalancing

```java
from("kafka:orders"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=order-service"
        + "&consumersCount=3"               // 3 consumer threads
        + "&autoCommitEnable=false"
        + "&allowManualCommit=true"
        + "&sessionTimeoutMs=30000"         // Session timeout
        + "&heartbeatIntervalMs=10000"      // Heartbeat frequency
        + "&maxPollIntervalMs=600000"       // Max time between polls (10min)
        + "&partitionAssignor=org.apache.kafka.clients.consumer.CooperativeStickyAssignor")
    .routeId("cooperative-consumer")
    .process(this::processWithTimeout)
    .process(this::commit);
```

**Key settings for stability:**
- `sessionTimeoutMs`: How long broker waits before declaring consumer dead
- `heartbeatIntervalMs`: Should be < 1/3 of session timeout
- `maxPollIntervalMs`: Max time between polls (increase for slow processing)
- `partitionAssignor`: Use `CooperativeStickyAssignor` for minimal rebalancing impact

---

## 6. Schema Registry Integration

```java
// Using Confluent Schema Registry with Avro
from("kafka:avro-orders"
        + "?brokers={{kafka.brokers}}"
        + "&groupId=avro-consumer"
        + "&valueDeserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer"
        + "&additionalProperties.schema.registry.url=http://schema-registry:8081"
        + "&additionalProperties.specific.avro.reader=true")
    .routeId("avro-consumer")
    .log("Received Avro order: ${body.orderId}")
    .to("direct:process");

// Producer with schema registry
from("direct:publish-avro")
    .process(exchange -> {
        Order order = exchange.getIn().getBody(Order.class);
        exchange.getIn().setBody(order.toAvro());  // Convert to Avro GenericRecord
    })
    .to("kafka:avro-orders"
        + "?brokers={{kafka.brokers}}"
        + "&valueSerializer=io.confluent.kafka.serializers.KafkaAvroSerializer"
        + "&additionalProperties.schema.registry.url=http://schema-registry:8081");
```

---

## 7. Kafka Streams-Like Processing in Camel

```java
// Windowed aggregation (similar to Kafka Streams)
from("kafka:page-views?brokers={{kafka.brokers}}&groupId=analytics")
    .routeId("page-view-counter")
    .unmarshal().json(JsonLibrary.Jackson, PageView.class)
    .aggregate(simple("${body.pageId}"), new CountAggregationStrategy())
        .completionInterval(60000)         // Emit count every 60 seconds
        .completionSize(1000)              // Or after 1000 events
    .marshal().json()
    .to("kafka:page-view-counts");

// Join two topics (enrichment pattern)
from("kafka:orders?brokers={{kafka.brokers}}&groupId=joiner")
    .routeId("order-customer-join")
    .unmarshal().json(JsonLibrary.Jackson, Order.class)
    .enrich("direct:lookup-customer", new OrderCustomerMerge())
    .marshal().json()
    .to("kafka:enriched-orders");

from("direct:lookup-customer")
    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
    .toD("http://customer-service/api/customers/${body.customerId}");
```

---

## 8. Multi-Topic Consumer

```java
// Consume from multiple topics
from("kafka:orders,payments,refunds"     // Comma-separated topics
        + "?brokers={{kafka.brokers}}"
        + "&groupId=event-processor")
    .routeId("multi-topic-consumer")
    .choice()
        .when(header(KafkaConstants.TOPIC).isEqualTo("orders"))
            .to("direct:handle-order")
        .when(header(KafkaConstants.TOPIC).isEqualTo("payments"))
            .to("direct:handle-payment")
        .when(header(KafkaConstants.TOPIC).isEqualTo("refunds"))
            .to("direct:handle-refund")
    .end();

// Pattern-based subscription
from("kafka:order.*"                     // Regex pattern
        + "?brokers={{kafka.brokers}}"
        + "&groupId=all-orders"
        + "&topicIsPattern=true")
    .to("direct:handle-any-order-event");
```

---

## 9. Performance Tuning

```yaml
# application.yml — Optimized Kafka settings
camel:
  component:
    kafka:
      brokers: kafka-1:9092,kafka-2:9092,kafka-3:9092
      
      # Consumer tuning
      max-poll-records: 500
      fetch-min-bytes: 50000              # Wait for 50KB before fetching
      fetch-max-wait-ms: 500             # Or 500ms max wait
      
      # Producer tuning
      compression-codec: lz4
      linger-ms: 10                       # Batch for 10ms
      batch-size: 131072                  # 128KB batch
      buffer-memory-size: 67108864        # 64MB buffer
      request-required-acks: all
      retries: 5
      
      # Connection
      reconnect-backoff-ms: 1000
      reconnect-backoff-max-ms: 10000
```

| Setting | Low Latency | High Throughput |
|---------|------------|----------------|
| `lingerMs` | 0 | 10-50 |
| `batchSize` | 16384 | 131072+ |
| `compressionCodec` | none | lz4 |
| `maxPollRecords` | 10-50 | 500-1000 |
| `fetchMinBytes` | 1 | 50000+ |
| `consumersCount` | = partitions | = partitions |
| `acks` | 1 | all |
