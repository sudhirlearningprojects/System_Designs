# Spring Integration - Enterprise Integration Patterns

[← Back to Index](README.md) | [← Previous: Spring Batch Part 2](13_Spring_Batch_Part2.md)

## Table of Contents
- [Message Channels](#message-channels)
- [File Integration](#file-integration)
- [HTTP Integration](#http-integration)
- [Message Transformation](#message-transformation)
- [Message Routing](#message-routing)

---

## Message Channels

```java
@Configuration
public class ChannelConfig {
    
    @Bean
    public MessageChannel directChannel() {
        return new DirectChannel();
    }
    
    @Bean
    public MessageChannel queueChannel() {
        return new QueueChannel(100);
    }
    
    @Bean
    public MessageChannel pubSubChannel() {
        return new PublishSubscribeChannel();
    }
}
```

---

## File Integration

```java
@Configuration
@EnableIntegration
public class FileIntegrationConfig {
    
    @Bean
    public IntegrationFlow fileReadingFlow() {
        return IntegrationFlows
            .from(Files.inboundAdapter(new File("input"))
                .patternFilter("*.csv")
                .autoCreateDirectory(true),
                e -> e.poller(Pollers.fixedDelay(5000)))
            .transform(File.class, file -> {
                try {
                    return Files.readString(file.toPath());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            })
            .handle(String.class, (payload, headers) -> {
                System.out.println("Processing file: " + payload);
                return payload;
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow fileWritingFlow() {
        return IntegrationFlows
            .from("outputChannel")
            .handle(Files.outboundAdapter(new File("output"))
                .fileNameGenerator(message -> "result-" + System.currentTimeMillis() + ".txt")
                .autoCreateDirectory(true))
            .get();
    }
}
```

---

## HTTP Integration

```java
@Configuration
public class HttpIntegrationConfig {
    
    @Bean
    public IntegrationFlow httpInboundFlow() {
        return IntegrationFlows
            .from(Http.inboundGateway("/api/process")
                .requestMapping(m -> m.methods(HttpMethod.POST))
                .requestPayloadType(Order.class))
            .handle(Order.class, (payload, headers) -> {
                return processOrder(payload);
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow httpOutboundFlow() {
        return IntegrationFlows
            .from("externalApiChannel")
            .handle(Http.outboundGateway("https://api.example.com/orders")
                .httpMethod(HttpMethod.POST)
                .expectedResponseType(String.class))
            .get();
    }
}
```

---

## Message Transformation

```java
@Configuration
public class TransformationConfig {
    
    @Bean
    public IntegrationFlow transformationFlow() {
        return IntegrationFlows
            .from("inputChannel")
            .transform(String.class, String::toUpperCase)
            .enrichHeaders(h -> h
                .header("timestamp", System.currentTimeMillis())
                .header("source", "integration-service"))
            .transform(Transformers.toJson())
            .channel("outputChannel")
            .get();
    }
}
```

---

## Message Routing

```java
@Configuration
public class RoutingConfig {
    
    @Bean
    public IntegrationFlow routingFlow() {
        return IntegrationFlows
            .from("inputChannel")
            .route(Order.class, order -> {
                if (order.getAmount() > 1000) {
                    return "highValueChannel";
                } else if (order.getAmount() > 100) {
                    return "mediumValueChannel";
                } else {
                    return "lowValueChannel";
                }
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow filteringFlow() {
        return IntegrationFlows
            .from("inputChannel")
            .filter(Order.class, order -> order.getStatus().equals("PENDING"))
            .handle(message -> {
                System.out.println("Processing pending order");
            })
            .get();
    }
}
```

---

## Service Activator

```java
@Component
public class OrderService {
    
    @ServiceActivator(inputChannel = "orderChannel", outputChannel = "resultChannel")
    public OrderResult processOrder(Order order) {
        OrderResult result = new OrderResult();
        result.setOrderId(order.getId());
        result.setStatus("PROCESSED");
        return result;
    }
}
```

---

## Gateway

```java
@MessagingGateway
public interface OrderGateway {
    
    @Gateway(requestChannel = "orderChannel")
    OrderResult processOrder(Order order);
    
    @Gateway(requestChannel = "orderChannel")
    Future<OrderResult> processOrderAsync(Order order);
}

// Usage
@Service
public class OrderController {
    @Autowired
    private OrderGateway orderGateway;
    
    public void submitOrder(Order order) {
        OrderResult result = orderGateway.processOrder(order);
        System.out.println("Order processed: " + result.getStatus());
    }
}
```

---

[← Previous: Spring Batch Part 2](13_Spring_Batch_Part2.md) | [Back to Index](README.md)
