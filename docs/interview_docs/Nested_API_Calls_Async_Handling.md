# Handling Nested API Calls Asynchronously

## Overview

When a parent API needs to call nested/child APIs that take time to respond, blocking the parent flow degrades performance. This guide covers strategies to continue parent API execution without waiting for slow nested calls.

**Problem**: Parent API waits for nested API → Increased latency → Poor user experience

**Solution**: Execute nested calls asynchronously → Parent continues → Better performance

---

## Strategies Comparison

| Strategy | Use Case | Latency | Complexity | Reliability |
|----------|----------|---------|------------|-------------|
| **CompletableFuture** | Independent nested calls | Low | Low | Medium |
| **@Async + Future** | Spring Boot async methods | Low | Low | Medium |
| **WebClient (Reactive)** | Non-blocking HTTP calls | Very Low | Medium | High |
| **Message Queue** | Fire-and-forget operations | Very Low | High | Very High |
| **Callback/Webhook** | Long-running operations | Very Low | Medium | High |
| **Thread Pool** | CPU-intensive tasks | Low | Medium | Medium |

---

## Strategy 1: CompletableFuture (Most Common)

### Basic Async Execution

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // 1. Create order (synchronous - must complete)
        Order order = orderService.createOrder(request);
        
        // 2. Send notification (async - don't wait)
        CompletableFuture.runAsync(() -> {
            notificationService.sendOrderConfirmation(order.getId());
        });
        
        // 3. Update inventory (async - don't wait)
        CompletableFuture.runAsync(() -> {
            inventoryService.updateStock(order.getItems());
        });
        
        // 4. Return immediately
        return ResponseEntity.ok(new OrderResponse(order.getId(), "Order created"));
    }
}
```

**Output**:
```
Request received at: 10:00:00.000
Order created at: 10:00:00.100
Response sent at: 10:00:00.150
Notification sent at: 10:00:02.500 (async, 2.5s later)
Inventory updated at: 10:00:01.800 (async, 1.8s later)
```

---

### Multiple Async Calls with Custom Thread Pool

```java
@Configuration
public class AsyncConfig {
    
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class OrderService {
    
    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;
    
    public OrderResponse processOrder(OrderRequest request) {
        Order order = createOrder(request);
        
        // Execute multiple nested calls in parallel
        CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(
            () -> sendEmail(order), executor
        );
        
        CompletableFuture<Void> smsFuture = CompletableFuture.runAsync(
            () -> sendSMS(order), executor
        );
        
        CompletableFuture<Void> inventoryFuture = CompletableFuture.runAsync(
            () -> updateInventory(order), executor
        );
        
        // Don't wait - return immediately
        return new OrderResponse(order.getId(), "Processing");
    }
    
    // Optional: Wait for all if needed
    public void waitForCompletion(CompletableFuture<?>... futures) {
        CompletableFuture.allOf(futures).join();
    }
}
```

---

### With Error Handling

```java
@Service
public class PaymentService {
    
    public PaymentResponse processPayment(PaymentRequest request) {
        // Main payment processing
        Payment payment = createPayment(request);
        
        // Async fraud check (don't wait)
        CompletableFuture.supplyAsync(() -> {
            return fraudService.checkFraud(payment);
        }).exceptionally(ex -> {
            log.error("Fraud check failed: {}", ex.getMessage());
            return null; // Continue even if fraud check fails
        }).thenAccept(fraudResult -> {
            if (fraudResult != null && fraudResult.isFraudulent()) {
                payment.setStatus("FLAGGED");
                paymentRepository.save(payment);
            }
        });
        
        // Async analytics (don't wait)
        CompletableFuture.runAsync(() -> {
            analyticsService.trackPayment(payment);
        }).exceptionally(ex -> {
            log.error("Analytics failed: {}", ex.getMessage());
            return null;
        });
        
        return new PaymentResponse(payment.getId(), "SUCCESS");
    }
}
```

---

## Strategy 2: Spring @Async Annotation

### Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("spring-async-");
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async error in {}: {}", method.getName(), ex.getMessage());
        };
    }
}
```

### Usage

```java
@Service
public class NotificationService {
    
    @Async
    public void sendEmailAsync(String email, String message) {
        // This runs in separate thread
        log.info("Sending email to: {}", email);
        emailClient.send(email, message);
        log.info("Email sent to: {}", email);
    }
    
    @Async
    public CompletableFuture<String> sendSMSAsync(String phone, String message) {
        log.info("Sending SMS to: {}", phone);
        String result = smsClient.send(phone, message);
        return CompletableFuture.completedFuture(result);
    }
}

@RestController
public class UserController {
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        User user = userService.createUser(request);
        
        // Async - returns immediately
        notificationService.sendEmailAsync(user.getEmail(), "Welcome!");
        notificationService.sendSMSAsync(user.getPhone(), "Welcome!");
        
        return ResponseEntity.ok(new UserResponse(user.getId()));
    }
}
```

---

## Strategy 3: WebClient (Reactive - Non-blocking)

### Configuration

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
```

### Fire-and-Forget Pattern

```java
@Service
public class OrderService {
    
    @Autowired
    private WebClient webClient;
    
    public OrderResponse createOrder(OrderRequest request) {
        Order order = saveOrder(request);
        
        // Fire-and-forget: Call external API without waiting
        webClient.post()
            .uri("/notifications/email")
            .bodyValue(Map.of("orderId", order.getId(), "email", order.getEmail()))
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe(
                result -> log.info("Email sent successfully"),
                error -> log.error("Email failed: {}", error.getMessage())
            );
        
        // Call inventory service without waiting
        webClient.post()
            .uri("/inventory/update")
            .bodyValue(order.getItems())
            .retrieve()
            .bodyToMono(Void.class)
            .subscribe();
        
        return new OrderResponse(order.getId(), "Created");
    }
}
```

### Parallel Async Calls

```java
@Service
public class UserService {
    
    @Autowired
    private WebClient webClient;
    
    public UserResponse getUserDetails(String userId) {
        // Main user data (synchronous)
        User user = userRepository.findById(userId);
        
        // Parallel async calls
        Mono<Profile> profileMono = webClient.get()
            .uri("/profiles/{id}", userId)
            .retrieve()
            .bodyToMono(Profile.class);
        
        Mono<List<Order>> ordersMono = webClient.get()
            .uri("/orders?userId={id}", userId)
            .retrieve()
            .bodyToFlux(Order.class)
            .collectList();
        
        Mono<Preferences> prefMono = webClient.get()
            .uri("/preferences/{id}", userId)
            .retrieve()
            .bodyToMono(Preferences.class);
        
        // Execute all in parallel, don't wait
        Mono.zip(profileMono, ordersMono, prefMono)
            .subscribe(tuple -> {
                // Process results when all complete
                log.info("All data fetched for user: {}", userId);
            });
        
        // Return immediately with basic user data
        return new UserResponse(user.getId(), user.getName());
    }
}
```

---

## Strategy 4: Message Queue (Kafka/RabbitMQ)

### Kafka Producer

```java
@Service
public class OrderService {
    
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public OrderResponse createOrder(OrderRequest request) {
        Order order = saveOrder(request);
        
        // Publish event - fire and forget
        OrderEvent event = new OrderEvent(order.getId(), order.getCustomerId());
        kafkaTemplate.send("order-created", event);
        
        // Return immediately
        return new OrderResponse(order.getId(), "Created");
    }
}
```

### Kafka Consumer (Separate Service)

```java
@Service
public class NotificationConsumer {
    
    @KafkaListener(topics = "order-created", groupId = "notification-service")
    public void handleOrderCreated(OrderEvent event) {
        // Process async - send notifications
        emailService.sendOrderConfirmation(event.getOrderId());
        smsService.sendOrderSMS(event.getCustomerId());
    }
}
```

---

## Strategy 5: Callback/Webhook Pattern

### Parent API with Callback

```java
@RestController
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    @PostMapping("/reports/generate")
    public ResponseEntity<ReportResponse> generateReport(@RequestBody ReportRequest request) {
        // Start async report generation
        String reportId = UUID.randomUUID().toString();
        
        CompletableFuture.runAsync(() -> {
            // Long-running report generation
            byte[] reportData = reportService.generateReport(request);
            
            // Call webhook when done
            webClient.post()
                .uri(request.getCallbackUrl())
                .bodyValue(Map.of("reportId", reportId, "status", "COMPLETED"))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
        });
        
        // Return immediately with report ID
        return ResponseEntity.accepted()
            .body(new ReportResponse(reportId, "PROCESSING"));
    }
}
```

### Client Webhook Endpoint

```java
@RestController
public class WebhookController {
    
    @PostMapping("/webhooks/report-completed")
    public ResponseEntity<Void> handleReportCompleted(@RequestBody ReportCallback callback) {
        log.info("Report {} completed", callback.getReportId());
        // Download and process report
        return ResponseEntity.ok().build();
    }
}
```

---

## Strategy 6: Timeout with Fallback

### Execute with Timeout

```java
@Service
public class ProductService {
    
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findById(productId);
        
        // Try to get reviews with 2-second timeout
        CompletableFuture<List<Review>> reviewsFuture = CompletableFuture.supplyAsync(() -> {
            return reviewService.getReviews(productId); // Slow API
        });
        
        List<Review> reviews;
        try {
            reviews = reviewsFuture.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Reviews API timeout, using empty list");
            reviews = Collections.emptyList();
        } catch (Exception e) {
            log.error("Reviews API error: {}", e.getMessage());
            reviews = Collections.emptyList();
        }
        
        return new ProductResponse(product, reviews);
    }
}
```

---

## Real-World Example: E-commerce Order Flow

```java
@Service
public class OrderOrchestrationService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private Executor asyncExecutor;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public OrderResponse processOrder(OrderRequest request) {
        // Step 1: Validate and create order (MUST complete)
        Order order = validateAndCreateOrder(request);
        
        // Step 2: Process payment (MUST complete)
        PaymentResult payment = paymentService.processPayment(order);
        if (!payment.isSuccess()) {
            throw new PaymentFailedException("Payment failed");
        }
        
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        
        // Step 3: Async operations (fire-and-forget)
        
        // 3a. Send confirmation email (async)
        CompletableFuture.runAsync(() -> {
            emailService.sendOrderConfirmation(order);
        }, asyncExecutor);
        
        // 3b. Send SMS (async)
        CompletableFuture.runAsync(() -> {
            smsService.sendOrderSMS(order);
        }, asyncExecutor);
        
        // 3c. Update inventory (via Kafka)
        kafkaTemplate.send("inventory-update", new InventoryEvent(order.getItems()));
        
        // 3d. Trigger analytics (via Kafka)
        kafkaTemplate.send("order-analytics", new OrderAnalyticsEvent(order));
        
        // 3e. Call shipping service (async with callback)
        CompletableFuture.supplyAsync(() -> {
            return shippingService.createShipment(order);
        }, asyncExecutor).thenAccept(shipment -> {
            order.setShipmentId(shipment.getId());
            orderRepository.save(order);
        });
        
        // Step 4: Return immediately
        return OrderResponse.builder()
            .orderId(order.getId())
            .status("CONFIRMED")
            .message("Order confirmed. Notifications will be sent shortly.")
            .build();
    }
}
```

**Timeline**:
```
T+0ms:    Request received
T+50ms:   Order validated and created
T+200ms:  Payment processed
T+250ms:  Order confirmed
T+260ms:  Response sent to client ✓
T+1500ms: Email sent (async)
T+1800ms: SMS sent (async)
T+2000ms: Inventory updated (Kafka consumer)
T+2500ms: Shipment created (async)
T+3000ms: Analytics processed (Kafka consumer)
```

---

## Best Practices

### 1. Choose Right Strategy

```java
// ✅ Good: Fire-and-forget for notifications
CompletableFuture.runAsync(() -> sendEmail(order));

// ❌ Bad: Blocking for non-critical operations
sendEmail(order); // Blocks parent API
```

### 2. Handle Errors Gracefully

```java
// ✅ Good: Error handling
CompletableFuture.runAsync(() -> {
    sendEmail(order);
}).exceptionally(ex -> {
    log.error("Email failed: {}", ex.getMessage());
    return null; // Don't fail parent flow
});

// ❌ Bad: No error handling
CompletableFuture.runAsync(() -> sendEmail(order)); // Errors lost
```

### 3. Use Appropriate Thread Pool

```java
// ✅ Good: Custom thread pool
Executor executor = Executors.newFixedThreadPool(10);
CompletableFuture.runAsync(() -> task(), executor);

// ❌ Bad: Common ForkJoinPool (limited threads)
CompletableFuture.runAsync(() -> task()); // Uses ForkJoinPool.commonPool()
```

### 4. Set Timeouts

```java
// ✅ Good: With timeout
future.get(5, TimeUnit.SECONDS);

// ❌ Bad: No timeout
future.get(); // May wait forever
```

### 5. Monitor Async Tasks

```java
@Component
public class AsyncMonitor {
    
    private final AtomicLong pendingTasks = new AtomicLong(0);
    
    public void executeAsync(Runnable task) {
        pendingTasks.incrementAndGet();
        CompletableFuture.runAsync(task).whenComplete((result, ex) -> {
            pendingTasks.decrementAndGet();
        });
    }
    
    @Scheduled(fixedRate = 60000)
    public void logMetrics() {
        log.info("Pending async tasks: {}", pendingTasks.get());
    }
}
```

---

## Interview Questions

### Q1: When should you use CompletableFuture vs Message Queue?

**Answer**:
- **CompletableFuture**: Same application, low latency, simple async tasks
- **Message Queue**: Distributed systems, high reliability, decoupled services, event-driven

### Q2: How to handle failures in async nested calls?

**Answer**:
```java
CompletableFuture.runAsync(() -> {
    sendEmail(order);
}).exceptionally(ex -> {
    // Log error
    log.error("Email failed: {}", ex.getMessage());
    // Store in retry queue
    retryQueue.add(new EmailTask(order));
    return null;
});
```

### Q3: What's the difference between @Async and CompletableFuture?

**Answer**:
- **@Async**: Spring-managed, declarative, method-level
- **CompletableFuture**: Programmatic, more control, composable

### Q4: How to prevent thread pool exhaustion?

**Answer**:
- Set bounded thread pool size
- Use queue capacity limits
- Implement rejection policy
- Monitor thread pool metrics
- Use circuit breaker for external calls

---

## Key Takeaways

1. **Don't block parent API** for non-critical nested calls
2. **Use CompletableFuture** for simple async operations
3. **Use Message Queue** for distributed, reliable async processing
4. **Always handle errors** in async calls
5. **Use custom thread pools** to avoid resource contention
6. **Set timeouts** to prevent indefinite waiting
7. **Monitor async tasks** for observability
8. **Choose fire-and-forget** for notifications, analytics, logging
9. **Use callbacks/webhooks** for long-running operations
10. **Test async behavior** thoroughly

---

## Practice Problems

1. Implement order processing with async email, SMS, and inventory update
2. Create product API that fetches reviews async with 1-second timeout
3. Build user registration with async email verification via Kafka
4. Implement report generation with webhook callback
5. Create payment API with async fraud check and analytics
