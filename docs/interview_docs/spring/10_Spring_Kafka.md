# Spring Kafka - Event-Driven Architecture

[← Back to Index](README.md) | [← Previous: Spring Cloud](09_Spring_Cloud.md) | [Next: Spring AOP →](11_Spring_AOP.md)

## Table of Contents
- [Producer Configuration](#producer-configuration)
- [Consumer Configuration](#consumer-configuration)
- [Kafka Listener](#kafka-listener)

---

## Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, Order> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Order> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

@Service
public class OrderProducer {
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public void sendOrder(Order order) {
        kafkaTemplate.send("orders", order.getId().toString(), order);
    }
}
```

---

## Consumer Configuration

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-group");
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

---

## Kafka Listener

```java
@Service
public class OrderConsumer {
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consumeOrder(Order order) {
        System.out.println("Received order: " + order.getId());
        processOrder(order);
    }
    
    @KafkaListener(topics = "orders", groupId = "order-group")
    public void consumeWithMetadata(
            @Payload Order order,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        System.out.println("Partition: " + partition + ", Offset: " + offset);
        processOrder(order);
    }
}
```

---

[← Previous: Spring Cloud](09_Spring_Cloud.md) | [Next: Spring AOP →](11_Spring_AOP.md)
