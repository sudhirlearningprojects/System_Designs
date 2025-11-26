# Circuit Breaker Pattern - Deep Dive

## Table of Contents
1. [Introduction](#introduction)
2. [Theory and Concepts](#theory-and-concepts)
3. [Circuit States](#circuit-states)
4. [Implementation Patterns](#implementation-patterns)
5. [Practical Examples](#practical-examples)
6. [Best Practices](#best-practices)
7. [Common Pitfalls](#common-pitfalls)

---

## Introduction

The **Circuit Breaker Pattern** is a design pattern that prevents an application from repeatedly trying to execute an operation that's likely to fail. It acts like an electrical circuit breaker - when failures reach a threshold, the circuit "opens" and subsequent calls fail immediately without attempting the operation.

### Why Circuit Breaker?

In distributed systems:
- Services can become slow or unavailable
- Cascading failures can bring down entire systems
- Retry storms can overwhelm failing services
- Resources get exhausted waiting for timeouts

**Circuit Breaker solves this by:**
- Failing fast when a service is down
- Preventing cascading failures
- Allowing services time to recover
- Providing fallback mechanisms

---

## Theory and Concepts

### The Electrical Analogy

Just like an electrical circuit breaker:
- **Closed**: Current flows normally (requests pass through)
- **Open**: Circuit is broken (requests fail immediately)
- **Half-Open**: Testing if circuit can close (limited requests allowed)

### Core Principles

1. **Fail Fast**: Don't wait for timeouts when service is known to be down
2. **Automatic Recovery**: Periodically test if service has recovered
3. **Fallback**: Provide alternative responses when circuit is open
4. **Monitoring**: Track failures and circuit state changes

### Key Metrics

- **Failure Threshold**: Number/percentage of failures before opening
- **Timeout Duration**: How long to wait before attempting recovery
- **Success Threshold**: Successful calls needed to close circuit
- **Request Volume Threshold**: Minimum requests before evaluating failures

---

## Circuit States

### 1. CLOSED State (Normal Operation)

```
┌─────────────────┐
│  CLOSED STATE   │
│  Requests pass  │
│  through        │
└─────────────────┘
        │
        │ Failures < Threshold
        ↓
   [Continue]
        │
        │ Failures ≥ Threshold
        ↓
   [OPEN STATE]
```

**Behavior:**
- All requests pass through to the service
- Failures are counted
- When failure threshold is reached → transition to OPEN

**Example:**
```java
// 5 failures out of 10 requests = 50% failure rate
// If threshold is 50%, circuit opens
```

### 2. OPEN State (Failing Fast)

```
┌─────────────────┐
│   OPEN STATE    │
│  Requests fail  │
│  immediately    │
└─────────────────┘
        │
        │ Wait timeout period
        ↓
  [HALF-OPEN STATE]
```

**Behavior:**
- All requests fail immediately (no call to service)
- Return fallback response or error
- After timeout period → transition to HALF-OPEN

**Example:**
```java
// Circuit is open for 60 seconds
// All requests during this time fail fast
// After 60 seconds, allow test request
```

### 3. HALF-OPEN State (Testing Recovery)

```
┌─────────────────┐
│ HALF-OPEN STATE │
│  Limited test   │
│  requests       │
└─────────────────┘
        │
        ├─ Success ≥ Threshold → [CLOSED]
        │
        └─ Any Failure → [OPEN]
```

**Behavior:**
- Allow limited number of test requests
- If successful → transition to CLOSED
- If any failure → transition back to OPEN

**Example:**
```java
// Allow 3 test requests
// If all 3 succeed → close circuit
// If any fails → open circuit again
```

---

## Implementation Patterns

### Pattern 1: Basic Circuit Breaker

```java
public class CircuitBreaker {
    
    private enum State { CLOSED, OPEN, HALF_OPEN }
    
    private State state = State.CLOSED;
    private int failureCount = 0;
    private int successCount = 0;
    private final int failureThreshold;
    private final int successThreshold;
    private final Duration timeout;
    private Instant lastFailureTime;
    
    public CircuitBreaker(int failureThreshold, int successThreshold, Duration timeout) {
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
    }
    
    public <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException {
        if (state == State.OPEN) {
            if (Instant.now().isAfter(lastFailureTime.plus(timeout))) {
                state = State.HALF_OPEN;
                successCount = 0;
            } else {
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
            }
        }
        
        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    private synchronized void onSuccess() {
        failureCount = 0;
        
        if (state == State.HALF_OPEN) {
            successCount++;
            if (successCount >= successThreshold) {
                state = State.CLOSED;
            }
        }
    }
    
    private synchronized void onFailure() {
        failureCount++;
        lastFailureTime = Instant.now();
        
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
        } else if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
    
    public State getState() {
        return state;
    }
}
```

### Pattern 2: Resilience4j Circuit Breaker

```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                    // 50% failure rate
            .waitDurationInOpenState(Duration.ofSeconds(60))  // Wait 60s before half-open
            .slidingWindowSize(10)                       // Last 10 calls
            .minimumNumberOfCalls(5)                     // Min 5 calls before evaluation
            .permittedNumberOfCallsInHalfOpenState(3)    // 3 test calls in half-open
            .slowCallRateThreshold(50)                   // 50% slow calls
            .slowCallDurationThreshold(Duration.ofSeconds(2))  // >2s is slow
            .recordExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(BusinessException.class)
            .build();
        
        return CircuitBreakerRegistry.of(config);
    }
}

@Service
public class PaymentService {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Payment processPayment(PaymentRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-service");
        
        return circuitBreaker.executeSupplier(() -> {
            return restTemplate.postForObject(
                "http://payment-api/process",
                request,
                Payment.class
            );
        });
    }
}
```

### Pattern 3: Circuit Breaker with Fallback

```java
@Service
public class ProductService {
    
    private final CircuitBreaker circuitBreaker;
    private final WebClient webClient;
    private final ProductCache cache;
    
    public Mono<Product> getProduct(String productId) {
        return Mono.fromSupplier(() -> 
            circuitBreaker.executeSupplier(() -> 
                fetchProductFromAPI(productId)
            )
        )
        .onErrorResume(CircuitBreakerOpenException.class, e -> 
            getProductFromCache(productId)
        )
        .onErrorResume(e -> 
            Mono.just(getDefaultProduct())
        );
    }
    
    private Product fetchProductFromAPI(String productId) {
        return webClient.get()
            .uri("/products/{id}", productId)
            .retrieve()
            .bodyToMono(Product.class)
            .block();
    }
    
    private Mono<Product> getProductFromCache(String productId) {
        Product cached = cache.get(productId);
        if (cached != null) {
            cached.setFromCache(true);
            return Mono.just(cached);
        }
        return Mono.error(new ProductNotFoundException());
    }
    
    private Product getDefaultProduct() {
        return Product.builder()
            .id("default")
            .name("Product Unavailable")
            .available(false)
            .build();
    }
}
```

### Pattern 4: Annotation-Based Circuit Breaker

```java
@Service
public class OrderService {
    
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackGetOrder")
    public Order getOrder(String orderId) {
        return restTemplate.getForObject(
            "http://order-api/orders/" + orderId,
            Order.class
        );
    }
    
    // Fallback method - same signature + Throwable parameter
    private Order fallbackGetOrder(String orderId, Throwable t) {
        log.warn("Circuit breaker fallback for order: {}", orderId, t);
        
        // Try cache
        Order cached = orderCache.get(orderId);
        if (cached != null) {
            return cached;
        }
        
        // Return default
        return Order.builder()
            .id(orderId)
            .status(OrderStatus.UNKNOWN)
            .message("Order service temporarily unavailable")
            .build();
    }
    
    @CircuitBreaker(name = "orderService")
    @Retry(name = "orderService", fallbackMethod = "fallbackCreateOrder")
    public Order createOrder(OrderRequest request) {
        return restTemplate.postForObject(
            "http://order-api/orders",
            request,
            Order.class
        );
    }
    
    private Order fallbackCreateOrder(OrderRequest request, Throwable t) {
        // Queue for later processing
        orderQueue.add(request);
        
        return Order.builder()
            .status(OrderStatus.PENDING)
            .message("Order queued for processing")
            .build();
    }
}
```

### Pattern 5: Distributed Circuit Breaker with Redis

```java
@Service
public class DistributedCircuitBreaker {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String FAILURE_COUNT_KEY = "cb:failures:";
    private static final String STATE_KEY = "cb:state:";
    private static final String LAST_FAILURE_KEY = "cb:lastfailure:";
    
    public <T> T execute(String serviceName, Supplier<T> operation) {
        String state = getState(serviceName);
        
        if ("OPEN".equals(state)) {
            if (shouldAttemptReset(serviceName)) {
                setState(serviceName, "HALF_OPEN");
            } else {
                throw new CircuitBreakerOpenException("Circuit is OPEN for " + serviceName);
            }
        }
        
        try {
            T result = operation.get();
            onSuccess(serviceName);
            return result;
        } catch (Exception e) {
            onFailure(serviceName);
            throw e;
        }
    }
    
    private void onSuccess(String serviceName) {
        redisTemplate.delete(FAILURE_COUNT_KEY + serviceName);
        
        String state = getState(serviceName);
        if ("HALF_OPEN".equals(state)) {
            setState(serviceName, "CLOSED");
        }
    }
    
    private void onFailure(String serviceName) {
        Long failures = redisTemplate.opsForValue().increment(FAILURE_COUNT_KEY + serviceName);
        redisTemplate.opsForValue().set(LAST_FAILURE_KEY + serviceName, 
            String.valueOf(System.currentTimeMillis()));
        
        if (failures >= 5) {
            setState(serviceName, "OPEN");
        }
    }
    
    private String getState(String serviceName) {
        String state = redisTemplate.opsForValue().get(STATE_KEY + serviceName);
        return state != null ? state : "CLOSED";
    }
    
    private void setState(String serviceName, String state) {
        redisTemplate.opsForValue().set(STATE_KEY + serviceName, state, 
            Duration.ofMinutes(5));
    }
    
    private boolean shouldAttemptReset(String serviceName) {
        String lastFailureStr = redisTemplate.opsForValue().get(LAST_FAILURE_KEY + serviceName);
        if (lastFailureStr == null) return true;
        
        long lastFailure = Long.parseLong(lastFailureStr);
        long timeout = 60000; // 60 seconds
        return System.currentTimeMillis() - lastFailure > timeout;
    }
}
```

---

## Practical Examples

### Example 1: Payment Gateway Integration

```java
@Service
public class PaymentGatewayService {
    
    private final CircuitBreaker stripeCircuitBreaker;
    private final CircuitBreaker paypalCircuitBreaker;
    
    public PaymentGatewayService(CircuitBreakerRegistry registry) {
        this.stripeCircuitBreaker = registry.circuitBreaker("stripe");
        this.paypalCircuitBreaker = registry.circuitBreaker("paypal");
    }
    
    public Payment processPayment(PaymentRequest request) {
        // Try primary gateway (Stripe)
        try {
            return stripeCircuitBreaker.executeSupplier(() -> 
                processWithStripe(request)
            );
        } catch (CircuitBreakerOpenException e) {
            log.warn("Stripe circuit breaker is open, trying PayPal");
            
            // Fallback to secondary gateway (PayPal)
            try {
                return paypalCircuitBreaker.executeSupplier(() -> 
                    processWithPayPal(request)
                );
            } catch (CircuitBreakerOpenException e2) {
                log.error("Both payment gateways are unavailable");
                throw new PaymentUnavailableException("All payment gateways are down");
            }
        }
    }
    
    private Payment processWithStripe(PaymentRequest request) {
        // Call Stripe API
        return stripeClient.charge(request);
    }
    
    private Payment processWithPayPal(PaymentRequest request) {
        // Call PayPal API
        return paypalClient.charge(request);
    }
}
```

### Example 2: Database Connection Pool

```java
@Service
public class DatabaseCircuitBreaker {
    
    private final CircuitBreaker circuitBreaker;
    private final DataSource primaryDataSource;
    private final DataSource replicaDataSource;
    
    public <T> T executeQuery(Function<Connection, T> query) {
        return circuitBreaker.executeSupplier(() -> {
            try (Connection conn = primaryDataSource.getConnection()) {
                return query.apply(conn);
            } catch (SQLException e) {
                throw new DatabaseException("Primary database error", e);
            }
        });
    }
    
    public <T> T executeQueryWithFallback(Function<Connection, T> query) {
        try {
            return executeQuery(query);
        } catch (CircuitBreakerOpenException e) {
            log.warn("Primary database circuit open, using replica");
            
            try (Connection conn = replicaDataSource.getConnection()) {
                return query.apply(conn);
            } catch (SQLException ex) {
                throw new DatabaseException("Both databases unavailable", ex);
            }
        }
    }
}
```

### Example 3: External API with Retry and Circuit Breaker

```java
@Service
public class WeatherService {
    
    @Retry(name = "weatherApi", fallbackMethod = "fallbackGetWeather")
    @CircuitBreaker(name = "weatherApi", fallbackMethod = "fallbackGetWeather")
    public WeatherData getWeather(String city) {
        return webClient.get()
            .uri("/weather?city={city}", city)
            .retrieve()
            .bodyToMono(WeatherData.class)
            .timeout(Duration.ofSeconds(5))
            .block();
    }
    
    private WeatherData fallbackGetWeather(String city, Throwable t) {
        log.warn("Weather API unavailable for city: {}", city, t);
        
        // Try cache
        WeatherData cached = weatherCache.get(city);
        if (cached != null && cached.isRecentEnough()) {
            cached.setFromCache(true);
            return cached;
        }
        
        // Return default
        return WeatherData.builder()
            .city(city)
            .temperature(null)
            .condition("Unknown")
            .message("Weather data temporarily unavailable")
            .build();
    }
}

// Configuration
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreakerConfig weatherApiCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
    }
    
    @Bean
    public RetryConfig weatherApiRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .retryExceptions(IOException.class, TimeoutException.class)
            .build();
    }
}
```

### Example 4: Microservices Communication

```java
@Service
public class OrderOrchestrationService {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    public OrderResult createOrder(OrderRequest request) {
        OrderResult result = new OrderResult();
        
        // Call Inventory Service
        CircuitBreaker inventoryCircuitBreaker = circuitBreakerRegistry.circuitBreaker("inventory");
        try {
            InventoryResponse inventory = inventoryCircuitBreaker.executeSupplier(() ->
                inventoryService.checkAvailability(request.getItems())
            );
            result.setInventoryAvailable(inventory.isAvailable());
        } catch (CircuitBreakerOpenException e) {
            log.warn("Inventory service circuit is open");
            result.setInventoryAvailable(false);
            result.addWarning("Inventory check skipped - service unavailable");
        }
        
        // Call Payment Service
        CircuitBreaker paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payment");
        try {
            Payment payment = paymentCircuitBreaker.executeSupplier(() ->
                paymentService.processPayment(request.getPaymentInfo())
            );
            result.setPayment(payment);
        } catch (CircuitBreakerOpenException e) {
            log.error("Payment service circuit is open");
            throw new OrderException("Payment service unavailable");
        }
        
        // Call Shipping Service (optional)
        CircuitBreaker shippingCircuitBreaker = circuitBreakerRegistry.circuitBreaker("shipping");
        try {
            Shipment shipment = shippingCircuitBreaker.executeSupplier(() ->
                shippingService.scheduleShipment(request.getShippingInfo())
            );
            result.setShipment(shipment);
        } catch (CircuitBreakerOpenException e) {
            log.warn("Shipping service circuit is open - will schedule later");
            result.setShipment(null);
            result.addWarning("Shipment will be scheduled later");
        }
        
        return result;
    }
}
```

---

## Best Practices

### 1. Configure Appropriate Thresholds

```java
// Different services need different configurations
@Bean
public CircuitBreakerRegistry circuitBreakerRegistry() {
    // Critical service - fail fast
    CircuitBreakerConfig criticalConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(30)  // Open at 30% failures
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .build();
    
    // Non-critical service - more tolerant
    CircuitBreakerConfig nonCriticalConfig = CircuitBreakerConfig.custom()
        .failureRateThreshold(70)  // Open at 70% failures
        .waitDurationInOpenState(Duration.ofSeconds(60))
        .build();
    
    return CircuitBreakerRegistry.of(Map.of(
        "payment", criticalConfig,
        "recommendation", nonCriticalConfig
    ));
}
```

### 2. Implement Proper Fallbacks

```java
@Service
public class UserService {
    
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUser")
    public User getUser(String userId) {
        return userApiClient.getUser(userId);
    }
    
    private User fallbackGetUser(String userId, Throwable t) {
        // 1. Try cache
        User cached = userCache.get(userId);
        if (cached != null) {
            return cached;
        }
        
        // 2. Try database
        Optional<User> dbUser = userRepository.findById(userId);
        if (dbUser.isPresent()) {
            return dbUser.get();
        }
        
        // 3. Return minimal user
        return User.builder()
            .id(userId)
            .name("User " + userId)
            .limited(true)
            .build();
    }
}
```

### 3. Monitor Circuit Breaker Events

```java
@Component
public class CircuitBreakerEventListener {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerOnStateTransitionEvent event) {
        String circuitBreakerName = event.getCircuitBreakerName();
        CircuitBreaker.State fromState = event.getStateTransition().getFromState();
        CircuitBreaker.State toState = event.getStateTransition().getToState();
        
        log.info("Circuit Breaker {} transitioned from {} to {}", 
            circuitBreakerName, fromState, toState);
        
        // Metrics
        meterRegistry.counter("circuit.breaker.state.transition",
            "name", circuitBreakerName,
            "from", fromState.name(),
            "to", toState.name()
        ).increment();
        
        // Alert on OPEN
        if (toState == CircuitBreaker.State.OPEN) {
            alertService.sendAlert("Circuit breaker " + circuitBreakerName + " is OPEN");
        }
    }
    
    @EventListener
    public void onCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        log.error("Circuit Breaker {} recorded error: {}", 
            event.getCircuitBreakerName(), 
            event.getThrowable().getMessage());
    }
}
```

### 4. Use Bulkhead with Circuit Breaker

```java
@Configuration
public class ResilienceConfiguration {
    
    @Bean
    public ThreadPoolBulkheadRegistry bulkheadRegistry() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(10)
            .coreThreadPoolSize(5)
            .queueCapacity(20)
            .build();
        
        return ThreadPoolBulkheadRegistry.of(config);
    }
}

@Service
public class ResilientService {
    
    @Bulkhead(name = "externalService", type = Bulkhead.Type.THREADPOOL)
    @CircuitBreaker(name = "externalService")
    public CompletableFuture<Response> callExternalService(Request request) {
        return CompletableFuture.supplyAsync(() -> 
            externalServiceClient.call(request)
        );
    }
}
```

### 5. Test Circuit Breaker Behavior

```java
@SpringBootTest
public class CircuitBreakerTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Test
    public void testCircuitBreakerOpens() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
        
        // Simulate failures
        for (int i = 0; i < 5; i++) {
            assertThrows(PaymentException.class, () -> 
                paymentService.processPayment(invalidRequest())
            );
        }
        
        // Circuit should be open
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        
        // Next call should fail fast
        assertThrows(CircuitBreakerOpenException.class, () ->
            paymentService.processPayment(validRequest())
        );
    }
    
    @Test
    public void testCircuitBreakerRecovery() throws InterruptedException {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
        
        // Open circuit
        openCircuit(cb);
        
        // Wait for timeout
        Thread.sleep(61000);
        
        // Should be half-open
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        
        // Successful calls should close circuit
        for (int i = 0; i < 3; i++) {
            paymentService.processPayment(validRequest());
        }
        
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
```

---

## Common Pitfalls

### 1. ❌ Too Aggressive Thresholds

**Problem:**
```java
// BAD: Opens too quickly
CircuitBreakerConfig.custom()
    .failureRateThreshold(10)  // Opens at 10% failures
    .minimumNumberOfCalls(2)   // Only 2 calls needed
    .build();
```

**Solution:**
```java
// GOOD: Reasonable thresholds
CircuitBreakerConfig.custom()
    .failureRateThreshold(50)  // Opens at 50% failures
    .minimumNumberOfCalls(10)  // Need 10 calls before evaluation
    .slidingWindowSize(20)     // Look at last 20 calls
    .build();
```

### 2. ❌ No Fallback Strategy

**Problem:**
```java
// BAD: No fallback
@CircuitBreaker(name = "service")
public Data getData() {
    return externalService.getData();
}
// Throws exception when circuit is open
```

**Solution:**
```java
// GOOD: Proper fallback
@CircuitBreaker(name = "service", fallbackMethod = "fallbackGetData")
public Data getData() {
    return externalService.getData();
}

private Data fallbackGetData(Throwable t) {
    return cache.getData().orElse(Data.empty());
}
```

### 3. ❌ Ignoring Circuit State

**Problem:**
```java
// BAD: Not checking circuit state
public void processOrders() {
    orders.forEach(order -> {
        paymentService.process(order); // May fail fast repeatedly
    });
}
```

**Solution:**
```java
// GOOD: Check circuit state
public void processOrders() {
    CircuitBreaker cb = registry.circuitBreaker("payment");
    
    if (cb.getState() == CircuitBreaker.State.OPEN) {
        log.warn("Payment circuit is open, queueing orders");
        orders.forEach(orderQueue::add);
        return;
    }
    
    orders.forEach(order -> {
        try {
            paymentService.process(order);
        } catch (CircuitBreakerOpenException e) {
            orderQueue.add(order);
        }
    });
}
```

### 4. ❌ Not Monitoring Circuit Events

**Problem:**
```java
// BAD: No monitoring
// Circuit opens and closes without anyone knowing
```

**Solution:**
```java
// GOOD: Monitor and alert
@Component
public class CircuitBreakerMonitor {
    
    @EventListener
    public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
            alertService.criticalAlert(
                "Circuit breaker " + event.getCircuitBreakerName() + " is OPEN"
            );
        }
    }
}
```

### 5. ❌ Sharing Circuit Breaker Across Different Operations

**Problem:**
```java
// BAD: Same circuit breaker for different operations
@CircuitBreaker(name = "api")
public User getUser(String id) { ... }

@CircuitBreaker(name = "api")
public Order getOrder(String id) { ... }
// If getUser fails, getOrder is also blocked!
```

**Solution:**
```java
// GOOD: Separate circuit breakers
@CircuitBreaker(name = "userApi")
public User getUser(String id) { ... }

@CircuitBreaker(name = "orderApi")
public Order getOrder(String id) { ... }
```

---

## Real-World Examples

### Netflix Hystrix
- Pioneered circuit breaker pattern in microservices
- Used across all Netflix services
- Provides real-time monitoring dashboard
- Now in maintenance mode (use Resilience4j instead)

### AWS API Gateway
- Built-in circuit breaker for backend integrations
- Automatically fails fast when backends are unhealthy
- Configurable thresholds and timeouts

### Kubernetes
- Readiness and liveness probes act as circuit breakers
- Removes unhealthy pods from load balancer
- Prevents cascading failures

---

## Comparison with Other Patterns

| Pattern | Purpose | When to Use |
|---------|---------|-------------|
| **Circuit Breaker** | Prevent cascading failures | External service calls |
| **Retry** | Handle transient failures | Network glitches |
| **Timeout** | Prevent hanging | Slow operations |
| **Bulkhead** | Isolate resources | Resource exhaustion |
| **Rate Limiter** | Control request rate | Prevent overload |

---

## Conclusion

Circuit Breaker is essential for building resilient distributed systems. Key takeaways:

- **Fail fast** when services are down
- **Provide fallbacks** for better user experience
- **Monitor circuit state** and alert on transitions
- **Configure appropriately** for each service
- **Combine with other patterns** (retry, timeout, bulkhead)
- **Test failure scenarios** thoroughly

Remember: Circuit breakers protect your system from cascading failures and give failing services time to recover.
