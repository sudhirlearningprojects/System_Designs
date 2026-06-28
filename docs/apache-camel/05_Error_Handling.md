# Module 5: Error Handling & Resilience

## 1. Error Handler Types

Camel provides 4 error handler strategies:

| Type | Description | Use When |
|------|-------------|----------|
| `DefaultErrorHandler` | Logs and propagates exception | Development, simple routes |
| `DeadLetterChannel` | Routes failed messages to DLQ | Production — must not lose data |
| `TransactionErrorHandler` | Rolls back transaction on failure | JMS/DB transactional routes |
| `NoErrorHandler` | Does nothing (custom handling) | You handle everything manually |

---

## 2. Dead Letter Channel (DLC)

```java
@Component
public class OrderRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // Global error handler for all routes in this builder
        errorHandler(
            deadLetterChannel("kafka:dead-letter-queue")
                .maximumRedeliveries(3)            // Retry 3 times
                .redeliveryDelay(1000)             // Wait 1s between retries
                .backOffMultiplier(2.0)            // 1s, 2s, 4s (exponential)
                .maximumRedeliveryDelay(30000)     // Cap at 30s
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                .logRetryAttempted(true)
                .logExhausted(true)
                .logStackTrace(true)
                .useOriginalMessage()              // DLQ gets original, not modified
                .onRedelivery(this::onRetry)       // Custom logic before each retry
        );

        from("kafka:orders")
            .routeId("order-processor")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .bean("orderService", "process")
            .to("kafka:processed-orders");
    }

    private void onRetry(Exchange exchange) {
        int retryCount = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
        String routeId = exchange.getFromRouteId();
        log.warn("Retry #{} for route: {}", retryCount, routeId);
    }
}
```

---

## 3. Exception-Specific Handling

```java
@Override
public void configure() {
    // Handle specific exceptions differently
    onException(ValidationException.class)
        .handled(true)                             // Don't propagate
        .maximumRedeliveries(0)                    // No retries for validation errors
        .log(LoggingLevel.WARN, "Validation failed: ${exception.message}")
        .to("kafka:validation-errors");

    onException(TimeoutException.class, ConnectException.class)
        .maximumRedeliveries(5)                    // Retry network errors
        .redeliveryDelay(2000)
        .backOffMultiplier(2)
        .to("kafka:network-errors");

    onException(Exception.class)                   // Catch-all
        .handled(true)
        .maximumRedeliveries(3)
        .to("kafka:dead-letter-queue");

    from("kafka:orders")
        .bean("orderValidator")                    // May throw ValidationException
        .to("http://payment-service/charge")       // May throw TimeoutException
        .to("kafka:completed");
}
```

### handled() vs continued()

```java
// handled(true) → Exception is "consumed", route stops, exchange marked as success
onException(ValidationException.class)
    .handled(true)
    .to("kafka:validation-errors");
    // Route STOPS here for this exchange

// continued(true) → Exception is ignored, route CONTINUES from next step
onException(EnrichmentException.class)
    .continued(true)                               // Skip enrichment failure, continue
    .log("Enrichment failed, continuing without enrichment");
    // Route CONTINUES to next step after the failed one
```

---

## 4. Try-Catch-Finally (Route level)

```java
from("kafka:orders")
    .doTry()
        .bean("orderService", "process")
        .to("http://payment-service/charge")
    .doCatch(PaymentException.class)
        .log("Payment failed: ${exception.message}")
        .bean("orderService", "markPaymentFailed")
        .to("kafka:payment-failures")
    .doCatch(Exception.class)
        .log("Unexpected error: ${exception.message}")
        .to("kafka:errors")
    .doFinally()
        .log("Order processing complete for ${header.orderId}")
        .bean("metricsService", "recordProcessingTime")
    .end();
```

---

## 5. Circuit Breaker (Resilience4j)

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-resilience4j-starter</artifactId>
</dependency>
```

```java
from("direct:call-external-service")
    .circuitBreaker()
        .resilience4jConfiguration()
            .failureRateThreshold(50)           // Open at 50% failure rate
            .waitDurationInOpenState(30)         // Wait 30s before half-open
            .slidingWindowSize(10)              // Evaluate last 10 calls
            .minimumNumberOfCalls(5)            // Need at least 5 calls
            .slowCallRateThreshold(80)          // 80% slow = open
            .slowCallDurationThreshold(3000)    // "Slow" = > 3s
        .end()
        .to("http://payment-service/api/charge")
        .log("Payment successful")
    .onFallback()
        .log(LoggingLevel.WARN, "Circuit open! Using fallback")
        .setBody(constant("{\"status\":\"PENDING\",\"message\":\"Payment queued\"}"))
        .to("kafka:payment-retry-queue")
    .end();
```

### Circuit Breaker States

```
CLOSED (normal) ──[failure rate > threshold]──→ OPEN (reject all)
                                                    │
                                              [wait duration]
                                                    │
                                                    ▼
                                              HALF-OPEN (allow few)
                                                    │
                              ┌──────────────────────┼────────────────┐
                              │                                       │
                    [calls succeed]                          [calls fail]
                              │                                       │
                              ▼                                       ▼
                           CLOSED                                   OPEN
```

---

## 6. Retry with Custom Strategies

```java
// Retryable route with custom backoff
from("kafka:orders")
    .onException(TransientException.class)
        .maximumRedeliveries(5)
        .redeliveryDelay(500)
        .backOffMultiplier(2.0)         // 500ms, 1s, 2s, 4s, 8s
        .collisionAvoidanceFactor(0.15) // Add ±15% jitter
        .retryWhile(method("retryPolicy", "shouldRetry"))
    .end()
    .to("http://unreliable-service/api");

@Component("retryPolicy")
public class RetryPolicy {
    public boolean shouldRetry(@Header(Exchange.REDELIVERY_COUNTER) int count,
                               @ExchangeException Exception ex) {
        // Don't retry if it's a 4xx error (client error)
        if (ex instanceof HttpOperationFailedException httpEx) {
            return httpEx.getStatusCode() >= 500; // Only retry 5xx
        }
        return count < 5;
    }
}
```

---

## 7. Idempotent Consumer (Duplicate Prevention)

```java
// In-memory (for single instance)
from("kafka:payments")
    .idempotentConsumer(header("paymentId"),
        MemoryIdempotentRepository.memoryIdempotentRepository(10000))
    .log("Processing unique payment: ${header.paymentId}")
    .to("direct:process-payment");

// Redis-based (for distributed)
@Bean
public IdempotentRepository redisRepo(RedisTemplate<String, String> redis) {
    return new RedisStringIdempotentRepository(redis, "idempotent-payments");
}

from("kafka:payments")
    .idempotentConsumer(header("paymentId"), "redisRepo")
        .skipDuplicate(true)           // Silently skip duplicates
        .removeOnFailure(true)         // Allow retry if processing fails
    .to("direct:process-payment");

// JDBC-based (persistent, queryable)
@Bean
public JdbcMessageIdRepository jdbcRepo(DataSource ds) {
    return new JdbcMessageIdRepository(ds, "camel_idempotent");
}
```

---

## 8. Timeout Handling

```java
// Route-level timeout
from("direct:call-service")
    .to("http://slow-service/api?socketTimeout=5000&connectTimeout=2000");

// Using EIP timeout
from("direct:aggregate-with-timeout")
    .aggregate(header("batchId"), new ListStrategy())
        .completionTimeout(10000)      // Force completion after 10s
        .completionSize(100)
    .to("direct:process-batch");

// Async timeout with fallback
from("direct:get-enrichment")
    .circuitBreaker()
        .resilience4jConfiguration()
            .timeoutEnabled(true)
            .timeoutDuration(3000)     // 3s timeout
        .end()
        .to("http://enrichment-service/api/lookup")
    .onFallback()
        .setBody(constant("{}"))       // Empty enrichment on timeout
    .end();
```

---

## 9. Transactional Error Handling

```java
@Bean
public PlatformTransactionManager transactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
}

@Bean("PROPAGATION_REQUIRED")
public SpringTransactionPolicy required(PlatformTransactionManager txManager) {
    SpringTransactionPolicy policy = new SpringTransactionPolicy();
    policy.setTransactionManager(txManager);
    policy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
    return policy;
}

@Override
public void configure() {
    // Transactional route — rollback on any exception
    from("jms:queue:orders")
        .transacted("PROPAGATION_REQUIRED")
        .to("sql:INSERT INTO orders (id, amount) VALUES (:#${body.id}, :#${body.amount})")
        .to("sql:UPDATE inventory SET quantity = quantity - :#${body.qty} WHERE product_id = :#${body.productId}")
        .to("jms:queue:order-confirmations");
    // If any step fails → all rolled back (DB + JMS)
}
```

---

## 10. Error Handling Best Practices

```java
@Override
public void configure() {
    // 1. Global DLQ for unhandled errors
    errorHandler(deadLetterChannel("kafka:dlq")
        .maximumRedeliveries(3)
        .redeliveryDelay(1000)
        .backOffMultiplier(2)
        .useOriginalMessage()
        .logExhausted(true));

    // 2. Specific handlers for known exceptions
    onException(ValidationException.class)
        .handled(true)
        .maximumRedeliveries(0)                    // No retry for bad data
        .to("kafka:validation-errors");

    onException(RateLimitException.class)
        .maximumRedeliveries(10)                   // Many retries for rate limits
        .redeliveryDelay(5000)                     // Long wait
        .backOffMultiplier(1.5);

    onException(TimeoutException.class, IOException.class)
        .maximumRedeliveries(5)
        .redeliveryDelay(2000)
        .backOffMultiplier(2)
        .circuitBreaker()                          // Open circuit if persistent
            .resilience4jConfiguration()
                .failureRateThreshold(50)
            .end()
        .end();

    // 3. Route with comprehensive error handling
    from("kafka:orders")
        .routeId("order-pipeline")
        .doTry()
            .bean("validator")
            .bean("enricher")
        .doCatch(EnrichmentException.class)
            .continued(true)                       // Continue without enrichment
        .end()
        .circuitBreaker()
            .to("http://payment-service/charge")
        .onFallback()
            .to("seda:payment-retry")
        .end()
        .to("kafka:completed");
}
```

### Decision Matrix

| Error Type | Strategy | Retry? | DLQ? |
|-----------|----------|--------|------|
| Validation error (bad data) | Handle immediately | No | Yes (for analysis) |
| Network timeout | Retry with backoff | Yes (3-5x) | Yes (after exhausted) |
| Rate limit (429) | Retry with long delay | Yes (10x) | No (eventually succeeds) |
| Auth failure (401/403) | Fail immediately | No | Yes |
| Server error (500) | Retry + circuit breaker | Yes | Yes |
| Downstream permanently down | Circuit breaker + fallback | No | Queue for later |
| Data format error | Log and skip | No | Yes |
