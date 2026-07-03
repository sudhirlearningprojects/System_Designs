# Module 7: Redis Streams

## 🎯 Learning Objectives

- Understand Redis Streams as persistent event log
- Implement producer/consumer with Spring Data Redis
- Use consumer groups for competing consumers
- Handle acknowledgments, pending entries, and dead letters
- Compare Streams vs Kafka vs Pub/Sub

---

## 7.1 Streams Concept

```
Stream: stream:orders
┌────────────────────────────────────────────────────────────┐
│ Entry 1705312800000-0   │ Entry 1705312801000-0   │ ...   │
│ orderId: "ord-1"        │ orderId: "ord-2"        │       │
│ userId: "u-100"         │ userId: "u-200"         │       │
│ amount: "5999"          │ amount: "1299"          │       │
│ status: "CREATED"       │ status: "CREATED"       │       │
└────────────────────────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
  Consumer Group:          Consumer Group:
  "payment-processors"     "notification-senders"
  ├── consumer-1           ├── consumer-1
  └── consumer-2           └── consumer-2
```

---

## 7.2 Producer (Writing to Stream)

```java
// src/main/java/com/example/redis/notification/stream/OrderStreamProducer.java

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStreamProducer {

    private final StringRedisTemplate redisTemplate;
    private static final String STREAM_KEY = "stream:orders";

    public String publishOrderEvent(String orderId, String userId,
                                     String status, BigDecimal amount) {
        Map<String, String> fields = Map.of(
            "orderId", orderId,
            "userId", userId,
            "status", status,
            "amount", amount.toString(),
            "timestamp", Instant.now().toString()
        );

        // XADD stream:orders * field1 value1 field2 value2
        RecordId recordId = redisTemplate.opsForStream()
            .add(StreamRecords.newRecord()
                .in(STREAM_KEY)
                .ofMap(fields));

        log.info("Published to stream: id={}, orderId={}", recordId, orderId);
        return recordId.getValue();
    }

    // Capped stream (auto-trim to max length)
    public String publishWithMaxLen(Map<String, String> data, long maxLen) {
        StringRecord record = StreamRecords.string(data).withStreamKey(STREAM_KEY);

        RecordId id = redisTemplate.opsForStream().add(record);

        // Trim to max length (approximate with ~)
        redisTemplate.opsForStream().trim(STREAM_KEY, maxLen, true);

        return id.getValue();
    }
}
```

---

## 7.3 Consumer Group Setup

```java
// src/main/java/com/example/redis/config/StreamConfig.java

@Configuration
@Slf4j
public class StreamConfig {

    private static final String STREAM_KEY = "stream:orders";
    private static final String GROUP_PAYMENT = "payment-processors";
    private static final String GROUP_NOTIFICATION = "notification-senders";

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            streamListenerContainer(
                RedisConnectionFactory factory,
                PaymentStreamConsumer paymentConsumer,
                NotificationStreamConsumer notificationConsumer) {

        // Create consumer groups (idempotent)
        createConsumerGroups(factory);

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .builder()
            .pollTimeout(Duration.ofSeconds(2))
            .batchSize(10)
            .targetType(MapRecord.class)
            .build();

        var container = StreamMessageListenerContainer.create(factory, options);

        // Payment consumer group
        container.receive(
            Consumer.from(GROUP_PAYMENT, "payment-worker-1"),
            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
            paymentConsumer
        );

        // Notification consumer group
        container.receive(
            Consumer.from(GROUP_NOTIFICATION, "notif-worker-1"),
            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
            notificationConsumer
        );

        container.start();
        return container;
    }

    private void createConsumerGroups(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        try {
            template.opsForStream().createGroup(STREAM_KEY, GROUP_PAYMENT);
            log.info("Created consumer group: {}", GROUP_PAYMENT);
        } catch (Exception e) {
            log.debug("Group already exists: {}", GROUP_PAYMENT);
        }
        try {
            template.opsForStream().createGroup(STREAM_KEY, GROUP_NOTIFICATION);
            log.info("Created consumer group: {}", GROUP_NOTIFICATION);
        } catch (Exception e) {
            log.debug("Group already exists: {}", GROUP_NOTIFICATION);
        }
    }
}
```

---

## 7.4 Consumer Implementation

```java
// src/main/java/com/example/redis/notification/stream/PaymentStreamConsumer.java

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;
    private final PaymentService paymentService;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        String streamKey = record.getStream();
        RecordId id = record.getId();
        Map<String, String> data = record.getValue();

        log.info("Processing payment for order: {}, streamId: {}",
            data.get("orderId"), id);

        try {
            // Process payment
            paymentService.processPayment(
                data.get("orderId"),
                new BigDecimal(data.get("amount"))
            );

            // Acknowledge successful processing (XACK)
            redisTemplate.opsForStream().acknowledge(
                streamKey, "payment-processors", id
            );

            log.info("Payment processed and ACK'd: {}", data.get("orderId"));

        } catch (Exception e) {
            log.error("Payment failed for order {}: {}",
                data.get("orderId"), e.getMessage());
            // Don't ACK - message stays in pending list for retry
        }
    }
}
```

---

## 7.5 Pending Entry Recovery (Dead Letter Handling)

```java
// src/main/java/com/example/redis/notification/stream/PendingEntryRecovery.java

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingEntryRecovery {

    private final StringRedisTemplate redis;

    /**
     * Recover messages that were consumed but never ACK'd.
     * Run this periodically (e.g., every 5 minutes).
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void recoverPendingEntries() {
        String stream = "stream:orders";
        String group = "payment-processors";

        // Get pending entries older than 60 seconds
        PendingMessages pending = redis.opsForStream().pending(
            stream, group, Range.unbounded(), 100
        );

        for (PendingMessage msg : pending) {
            if (msg.getElapsedTimeSinceLastDelivery().toSeconds() > 60
                    && msg.getTotalDeliveryCount() < 3) {

                // Claim and redeliver
                log.warn("Reclaiming pending message: {}, deliveries: {}",
                    msg.getIdAsString(), msg.getTotalDeliveryCount());

                redis.opsForStream().claim(
                    stream, group, "recovery-worker",
                    Duration.ofSeconds(60),
                    RecordId.of(msg.getIdAsString())
                );

            } else if (msg.getTotalDeliveryCount() >= 3) {
                // Move to dead letter queue
                log.error("Moving to DLQ after 3 attempts: {}", msg.getIdAsString());
                moveToDeadLetter(stream, group, msg.getIdAsString());
            }
        }
    }

    private void moveToDeadLetter(String stream, String group, String recordId) {
        // Read the message
        List<MapRecord<String, Object, Object>> records = redis.opsForStream()
            .range(stream, Range.closed(recordId, recordId));

        if (!records.isEmpty()) {
            // Write to DLQ stream
            Map<String, String> data = new HashMap<>();
            records.get(0).getValue().forEach((k, v) -> data.put(k.toString(), v.toString()));
            data.put("_original_id", recordId);
            data.put("_failed_at", Instant.now().toString());

            redis.opsForStream().add(
                StreamRecords.string(data).withStreamKey("stream:orders:dlq")
            );
        }

        // ACK original (remove from pending)
        redis.opsForStream().acknowledge(stream, group, recordId);
    }
}
```

---

## 7.6 Streams vs Pub/Sub vs Kafka

| Feature | Pub/Sub | Streams | Kafka |
|---------|---------|---------|-------|
| Persistence | ❌ | ✅ | ✅ |
| Consumer Groups | ❌ | ✅ | ✅ |
| Message Replay | ❌ | ✅ | ✅ |
| Acknowledgment | ❌ | ✅ | ✅ |
| At-least-once | ❌ | ✅ | ✅ |
| Exactly-once | ❌ | ❌ | ✅ |
| Throughput | High | Medium | Very High |
| Ordering | Per channel | Per stream | Per partition |
| Setup complexity | Low | Low | High |
| Use case | Notifications | Event processing | Enterprise streaming |

**Use Streams when:**
- You need persistence + consumer groups but Kafka is overkill
- Events need replay capability
- Less than 100K messages/sec
- Already using Redis

---

## ⏭️ Next Module

Proceed to **[Module 8: Distributed Locks](08_Distributed_Locks.md)** for Redisson and Redlock patterns.
