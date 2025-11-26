# Design Patterns for Distributed Systems

A comprehensive collection of design patterns essential for building resilient, scalable distributed systems and microservices architectures.

---

## 📚 Available Patterns

### 1. [Saga Pattern](Saga_Pattern.md)
**Purpose**: Manage distributed transactions across multiple microservices

**Key Concepts**:
- Choreography vs Orchestration
- Compensating transactions
- Eventual consistency
- Idempotency

**Use Cases**:
- E-commerce order processing
- Travel booking systems
- Money transfers
- Multi-step workflows

**When to Use**:
- Need distributed transactions without 2PC
- Microservices with separate databases
- Long-running business processes
- Eventual consistency is acceptable

---

### 2. [Circuit Breaker Pattern](Circuit_Breaker_Pattern.md)
**Purpose**: Prevent cascading failures by failing fast when a service is unavailable

**Key Concepts**:
- Three states: CLOSED, OPEN, HALF-OPEN
- Failure threshold and timeout
- Fallback mechanisms
- Automatic recovery

**Use Cases**:
- External API calls
- Database connections
- Microservices communication
- Third-party integrations

**When to Use**:
- Calling unreliable external services
- Need to prevent cascading failures
- Want to fail fast instead of waiting for timeouts
- Need graceful degradation

---

### 3. [API Gateway Pattern](API_Gateway_Pattern.md)
**Purpose**: Provide a single entry point for all client requests to microservices

**Key Concepts**:
- Request routing
- Authentication & authorization
- Rate limiting
- Response aggregation
- Protocol translation

**Use Cases**:
- Microservices architecture
- Mobile/web applications
- Backend for Frontend (BFF)
- Multi-tenant systems

**When to Use**:
- Multiple microservices need unified API
- Need centralized authentication
- Want to aggregate multiple service calls
- Need protocol translation (REST to gRPC)

---

### 4. [Bulkhead Pattern](Bulkhead_Pattern.md)
**Purpose**: Isolate resources to prevent cascading failures and resource exhaustion

**Key Concepts**:
- Thread pool isolation
- Semaphore-based limits
- Connection pool separation
- Resource partitioning

**Use Cases**:
- Multi-tenant applications
- Critical vs non-critical operations
- Database connection management
- HTTP client pools

**When to Use**:
- Need to isolate critical operations
- Prevent one slow service from affecting others
- Multi-tenant resource isolation
- Want to limit blast radius of failures

---

## 🎯 Pattern Comparison

| Pattern | Problem Solved | Complexity | Performance Impact |
|---------|---------------|------------|-------------------|
| **Saga** | Distributed transactions | High | Medium |
| **Circuit Breaker** | Cascading failures | Medium | Low |
| **API Gateway** | Service coordination | Medium | Low-Medium |
| **Bulkhead** | Resource exhaustion | Low-Medium | Low |

---

## 🔄 Pattern Combinations

### Resilient Microservices Stack

```java
@Service
public class ResilientService {
    
    // Bulkhead: Isolate resources
    @Bulkhead(name = "externalService", type = Bulkhead.Type.THREADPOOL)
    
    // Circuit Breaker: Fail fast when service is down
    @CircuitBreaker(name = "externalService", fallbackMethod = "fallback")
    
    // Retry: Handle transient failures
    @Retry(name = "externalService")
    
    // Timeout: Prevent hanging
    @TimeLimiter(name = "externalService")
    public CompletableFuture<Response> callExternalService(Request request) {
        return CompletableFuture.supplyAsync(() -> 
            externalServiceClient.call(request)
        );
    }
    
    private CompletableFuture<Response> fallback(Request request, Exception e) {
        return CompletableFuture.completedFuture(Response.cached());
    }
}
```

### API Gateway with Circuit Breaker

```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("orderCB")
                        .setFallbackUri("forward:/fallback/orders"))
                    .requestRateLimiter(rl -> rl
                        .setRateLimiter(redisRateLimiter()))
                )
                .uri("lb://order-service"))
            .build();
    }
}
```

### Saga with Circuit Breaker

```java
@Service
public class OrderSagaOrchestrator {
    
    @CircuitBreaker(name = "paymentService")
    private Payment processPayment(Order order) {
        return paymentService.charge(order.getAmount());
    }
    
    @CircuitBreaker(name = "inventoryService")
    private void reserveInventory(Order order) {
        inventoryService.reserve(order.getItems());
    }
    
    public void executeOrderSaga(OrderRequest request) {
        try {
            Order order = createOrder(request);
            Payment payment = processPayment(order);
            reserveInventory(order);
            scheduleShipping(order);
        } catch (Exception e) {
            compensate();
        }
    }
}
```

---

## 📊 Decision Tree

```
Need to handle distributed transactions?
├─ Yes → Use Saga Pattern
│  ├─ Simple flow? → Choreography
│  └─ Complex flow? → Orchestration
│
Need to prevent cascading failures?
├─ Yes → Use Circuit Breaker
│  └─ Also need resource isolation? → Add Bulkhead
│
Need single entry point for microservices?
├─ Yes → Use API Gateway
│  ├─ Different clients? → Use BFF pattern
│  └─ Need aggregation? → Implement response aggregation
│
Need to isolate resources?
└─ Yes → Use Bulkhead
   ├─ CPU-bound? → Thread pool bulkhead
   └─ I/O-bound? → Semaphore bulkhead
```

---

## 🛠️ Technology Stack

### Resilience4j (Recommended)
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>2.0.2</version>
</dependency>
```

**Features**:
- Circuit Breaker
- Bulkhead (Thread Pool & Semaphore)
- Rate Limiter
- Retry
- Time Limiter
- Cache

### Spring Cloud Gateway
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

**Features**:
- Request routing
- Load balancing
- Circuit breaker integration
- Rate limiting
- Request/response transformation

### Axon Framework (Saga)
```xml
<dependency>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-spring-boot-starter</artifactId>
    <version>4.8.0</version>
</dependency>
```

**Features**:
- Saga orchestration
- Event sourcing
- CQRS support
- Distributed transactions

---

## 📈 Monitoring and Observability

### Metrics to Track

**Circuit Breaker**:
- State transitions (CLOSED → OPEN → HALF-OPEN)
- Failure rate
- Call duration
- Fallback invocations

**Bulkhead**:
- Available concurrent calls
- Queue size
- Rejected calls
- Thread pool utilization

**API Gateway**:
- Request rate
- Response time
- Error rate
- Cache hit ratio

**Saga**:
- Saga execution time
- Compensation rate
- Failed sagas
- Saga state distribution

### Example Monitoring Setup

```java
@Component
public class PatternMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onCircuitBreakerEvent(CircuitBreakerOnStateTransitionEvent event) {
        meterRegistry.counter("circuit.breaker.state.transition",
            "name", event.getCircuitBreakerName(),
            "from", event.getStateTransition().getFromState().name(),
            "to", event.getStateTransition().getToState().name()
        ).increment();
    }
    
    @EventListener
    public void onBulkheadEvent(BulkheadOnCallRejectedEvent event) {
        meterRegistry.counter("bulkhead.calls.rejected",
            "name", event.getBulkheadName()
        ).increment();
    }
}
```

---

## 🧪 Testing Patterns

### Circuit Breaker Testing

```java
@Test
public void testCircuitBreakerOpens() {
    // Simulate failures
    for (int i = 0; i < 5; i++) {
        assertThrows(ServiceException.class, () -> 
            service.call(invalidRequest())
        );
    }
    
    // Verify circuit is open
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    
    // Verify fail fast
    assertThrows(CircuitBreakerOpenException.class, () ->
        service.call(validRequest())
    );
}
```

### Bulkhead Testing

```java
@Test
public void testBulkheadIsolation() {
    // Fill up bulkhead
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        futures.add(service.slowOperation());
    }
    
    // Next call should be rejected
    assertThrows(BulkheadFullException.class, () ->
        service.slowOperation()
    );
}
```

### Saga Testing

```java
@Test
public void testSagaCompensation() {
    // Mock payment failure
    when(paymentService.charge(any())).thenThrow(PaymentException.class);
    
    // Execute saga
    assertThrows(SagaException.class, () ->
        sagaOrchestrator.executeOrderSaga(request)
    );
    
    // Verify compensation
    verify(orderService).cancelOrder(any());
    verify(inventoryService, never()).reserve(any());
}
```

---

## 📚 Further Reading

### Books
- **"Release It!"** by Michael Nygard - Resilience patterns
- **"Building Microservices"** by Sam Newman - Microservices architecture
- **"Microservices Patterns"** by Chris Richardson - Saga and other patterns

### Online Resources
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Martin Fowler - Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Chris Richardson - Saga Pattern](https://microservices.io/patterns/data/saga.html)

### Real-World Examples
- **Netflix**: Hystrix (Circuit Breaker), Zuul (API Gateway)
- **Amazon**: API Gateway, Bulkhead for service isolation
- **Uber**: Saga for trip booking, Circuit breaker for service calls
- **Twitter**: Bulkhead for timeline vs tweet operations

---

## 🎓 Best Practices Summary

1. **Start Simple**: Don't over-engineer. Add patterns as needed.

2. **Monitor Everything**: You can't improve what you don't measure.

3. **Test Failure Scenarios**: Chaos engineering is your friend.

4. **Combine Patterns**: Use multiple patterns together for resilience.

5. **Document Decisions**: Explain why you chose specific patterns.

6. **Set Appropriate Thresholds**: One size doesn't fit all services.

7. **Implement Fallbacks**: Always have a Plan B.

8. **Review and Adjust**: Patterns need tuning based on real traffic.

---

## 🤝 Contributing

To add a new pattern:
1. Create a new markdown file: `Pattern_Name.md`
2. Follow the existing structure
3. Include theory, implementation, and practical examples
4. Add real-world use cases
5. Update this README

---

## 📝 License

This documentation is part of the System Designs Collection project.

---

**Remember**: These patterns are tools, not rules. Use them wisely based on your specific requirements and constraints.
