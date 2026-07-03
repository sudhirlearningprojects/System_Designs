# Module 6: Pub/Sub & Messaging

## 🎯 Learning Objectives

- Implement Redis Pub/Sub for real-time notifications
- Configure message listeners in Spring
- Use pattern subscriptions
- Understand keyspace notifications
- Know Pub/Sub limitations vs Streams

---

## 6.1 Publisher

```java
// src/main/java/com/example/redis/notification/publisher/EventPublisher.java

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishOrderCreated(String orderId, String userId) {
        OrderEvent event = new OrderEvent(orderId, userId, "CREATED", Instant.now());
        redisTemplate.convertAndSend("events:orders", event);
        log.info("Published order event: {}", orderId);
    }

    public void publishPriceUpdate(Long productId, BigDecimal newPrice) {
        PriceEvent event = new PriceEvent(productId, newPrice, Instant.now());
        redisTemplate.convertAndSend("events:prices", event);
    }

    public void publishToUser(String userId, String message) {
        // User-specific channel
        redisTemplate.convertAndSend("notifications:user:" + userId, message);
    }

    // Publish to pattern (all product channels)
    public void publishInventoryAlert(String category, String message) {
        redisTemplate.convertAndSend("alerts:inventory:" + category, message);
    }
}
```

---

## 6.2 Subscriber Configuration

```java
// src/main/java/com/example/redis/config/PubSubConfig.java

@Configuration
public class PubSubConfig {

    @Bean
    public RedisMessageListenerContainer messageListenerContainer(
            RedisConnectionFactory factory,
            OrderEventListener orderListener,
            PriceEventListener priceListener,
            InventoryAlertListener inventoryListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // Subscribe to specific channels
        container.addMessageListener(
            orderListener,
            new ChannelTopic("events:orders")
        );

        container.addMessageListener(
            priceListener,
            new ChannelTopic("events:prices")
        );

        // Pattern subscription (all inventory alerts)
        container.addMessageListener(
            inventoryListener,
            new PatternTopic("alerts:inventory:*")
        );

        return container;
    }

    @Bean
    public MessageListenerAdapter orderListenerAdapter(OrderEventListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }
}
```

---

## 6.3 Message Listeners

```java
// src/main/java/com/example/redis/notification/subscriber/OrderEventListener.java

@Component
@Slf4j
public class OrderEventListener implements MessageListener {

    private final ObjectMapper objectMapper;

    public OrderEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            String channel = new String(message.getChannel());

            OrderEvent event = objectMapper.readValue(body, OrderEvent.class);

            log.info("Received order event on channel {}: orderId={}, status={}",
                channel, event.getOrderId(), event.getStatus());

            // Process the event
            handleOrderEvent(event);

        } catch (Exception e) {
            log.error("Failed to process message: {}", e.getMessage());
        }
    }

    private void handleOrderEvent(OrderEvent event) {
        switch (event.getStatus()) {
            case "CREATED" -> sendOrderConfirmation(event);
            case "SHIPPED" -> sendShippingNotification(event);
            case "DELIVERED" -> sendDeliveryNotification(event);
        }
    }
}
```

---

## 6.4 Keyspace Notifications

Redis can notify when keys expire, get deleted, or are modified.

```java
// Enable keyspace notifications in Redis config
// redis.conf: notify-keyspace-events Ex (for expired events)

// Or via command:
// CONFIG SET notify-keyspace-events Ex

@Configuration
public class KeyspaceNotificationConfig {

    @Bean
    public RedisMessageListenerContainer keyspaceContainer(
            RedisConnectionFactory factory,
            SessionExpiryListener sessionListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // Listen for key expiration events
        container.addMessageListener(
            sessionListener,
            new PatternTopic("__keyevent@0__:expired")
        );

        return container;
    }
}

@Component
@Slf4j
public class SessionExpiryListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());

        if (expiredKey.startsWith("cart:user:")) {
            String userId = expiredKey.replace("cart:user:", "");
            log.info("Cart expired for user: {}. Sending reminder email.", userId);
            // Send abandoned cart email
        }
    }
}
```

---

## 6.5 Pub/Sub Limitations

| Limitation | Detail | Alternative |
|-----------|--------|------------|
| Fire-and-forget | No delivery guarantee | Use Streams |
| No persistence | Messages lost if no subscriber online | Use Streams |
| No replay | Can't re-read old messages | Use Streams |
| No acknowledgment | Don't know if subscriber processed it | Use Streams |
| Subscriber must be connected | Offline subscribers miss everything | Use Streams |
| No consumer groups | Can't split work across workers | Use Streams |
| Memory pressure | Fast publisher + slow subscriber = buffer growth | Monitor |

**When to use Pub/Sub:**
- Real-time notifications where loss is acceptable
- Cache invalidation signals across instances
- Live dashboard updates
- Chat messages (with separate persistence)

**When to use Streams instead:**
- Guaranteed delivery needed
- Consumer groups (competing consumers)
- Message replay/history
- Exactly-once processing

---

## 6.6 Cache Invalidation with Pub/Sub

```java
// Notify all app instances to evict a cache entry

@Service
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private final StringRedisTemplate redis;
    private static final String CHANNEL = "cache:invalidate";

    public void invalidate(String cacheName, String key) {
        String message = cacheName + ":" + key;
        redis.convertAndSend(CHANNEL, message);
    }
}

@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    private final CacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String[] parts = new String(message.getBody()).split(":", 2);
        String cacheName = parts[0];
        String key = parts[1];

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
```

---

## ⏭️ Next Module

Proceed to **[Module 7: Redis Streams](07_Redis_Streams.md)** for reliable event processing with consumer groups.
