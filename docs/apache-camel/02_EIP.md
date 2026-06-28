# Module 2: Enterprise Integration Patterns (EIPs)

Apache Camel implements all 65+ patterns from the book "Enterprise Integration Patterns" by Hohpe & Woolf. Here are the most important ones with practical examples.

---

## 1. Message Routing Patterns

### 1.1 Content-Based Router

Route messages to different destinations based on content.

```java
from("kafka:incoming-orders")
    .routeId("order-router")
    .unmarshal().json(JsonLibrary.Jackson, Order.class)
    .choice()
        .when(simple("${body.type} == 'DIGITAL'"))
            .to("direct:digital-fulfillment")
        .when(simple("${body.type} == 'PHYSICAL' && ${body.amount} > 500"))
            .to("direct:priority-shipping")
        .when(simple("${body.type} == 'PHYSICAL'"))
            .to("direct:standard-shipping")
        .otherwise()
            .log(LoggingLevel.WARN, "Unknown order type: ${body.type}")
            .to("kafka:unknown-orders")
    .end();
```

### 1.2 Message Filter

Discard messages that don't match criteria.

```java
from("kafka:events")
    .filter(simple("${body.eventType} == 'ORDER_COMPLETED'"))
        .to("direct:process-completed-orders");

// With Predicate
from("kafka:events")
    .filter(method("eventFilter", "isHighValue"))
        .to("direct:high-value-processing");
```

### 1.3 Recipient List

Route to multiple dynamic destinations.

```java
// Dynamic recipients from header
from("direct:notify")
    .recipientList(header("notificationChannels"))
    .delimiter(",");
// Header value: "seda:email,seda:sms,seda:push"

// Dynamic recipients from body
from("direct:distribute")
    .recipientList(method("routingBean", "getDestinations"))
    .parallelProcessing()
    .stopOnException();
```

### 1.4 Routing Slip

Process through a dynamic list of steps.

```java
from("direct:process")
    .routingSlip(header("steps"));
// Header: "direct:validate,direct:enrich,direct:transform,direct:save"

// Each step processes and passes to next
from("direct:validate")
    .bean(validator);

from("direct:enrich")
    .enrich("http://enrichment-service/api/lookup", new MergeStrategy());
```

### 1.5 Dynamic Router

Decide the next step at runtime after each processing step.

```java
from("direct:dynamic")
    .dynamicRouter(method("dynamicRoutingBean", "route"));

@Component("dynamicRoutingBean")
public class DynamicRoutingBean {
    public String route(@Body Order order, @ExchangeProperty("step") Integer step) {
        if (step == null) step = 0;
        
        return switch (step) {
            case 0 -> "direct:validate";
            case 1 -> order.needsApproval() ? "direct:approval" : "direct:save";
            case 2 -> "direct:notify";
            default -> null; // null = stop routing
        };
    }
}
```

### 1.6 Multicast

Send the same message to multiple endpoints simultaneously.

```java
from("kafka:orders")
    .multicast()
        .parallelProcessing()       // Process in parallel
        .stopOnException()          // Stop all if one fails
        .to("direct:save-to-db", "direct:send-notification", "direct:update-analytics")
    .end();

// With aggregation strategy (combine results)
from("direct:get-price")
    .multicast(new LowestPriceStrategy())
        .to("http://supplier-a/price", "http://supplier-b/price", "http://supplier-c/price")
    .end();
```

### 1.7 Wire Tap

Send a copy of the message for monitoring without affecting the main flow.

```java
from("kafka:payments")
    .wireTap("kafka:payment-audit")      // Async copy to audit topic
    .to("direct:process-payment");       // Main flow continues unaffected

// With modified copy
from("kafka:payments")
    .wireTap("kafka:payment-audit")
        .newExchangeBody(simple("Audit: ${body.paymentId} at ${date:now}"))
    .end()
    .to("direct:process-payment");
```

---

## 2. Message Transformation Patterns

### 2.1 Content Enricher

Enrich a message with data from an external source.

```java
// Simple enrichment
from("kafka:orders")
    .enrich("http://user-service/api/users/${header.userId}", new OrderEnrichmentStrategy())
    .to("direct:process");

// Poll-based enrichment (lookup from DB)
from("kafka:orders")
    .pollEnrich("sql:SELECT * FROM customers WHERE id = :#${body.customerId}?dataSource=#ds")
    .to("direct:process");

public class OrderEnrichmentStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange original, Exchange enrichment) {
        Order order = original.getIn().getBody(Order.class);
        User user = enrichment.getIn().getBody(User.class);
        order.setCustomerName(user.getName());
        return original;
    }
}
```

### 2.2 Splitter

Split a message into individual pieces for separate processing.

```java
// Split JSON array
from("kafka:bulk-orders")
    .unmarshal().json(JsonLibrary.Jackson)
    .split(jsonpath("$.orders[*]"))
        .streaming()                    // Low memory (don't load all in memory)
        .parallelProcessing()           // Process splits in parallel
        .to("direct:process-single-order")
    .end()
    .log("All orders processed");

// Split with aggregation (collect results)
from("direct:process-batch")
    .split(body(), new ListAggregationStrategy())
        .to("direct:process-one")
    .end()
    .log("Results: ${body}");  // Aggregated list

// Split file line by line
from("file:/data/large-files")
    .split(body().tokenize("\n"))
        .streaming()  // Critical for large files
        .to("seda:process-line?concurrentConsumers=10")
    .end();
```

### 2.3 Aggregator

Combine multiple messages into a single message.

```java
from("kafka:order-items")
    .aggregate(header("orderId"), new OrderAggregationStrategy())
        .completionSize(10)             // Complete after 10 messages
        .completionTimeout(5000)        // Or after 5 seconds
        .completionPredicate(header("isLast").isEqualTo(true))  // Or when flag set
    .to("direct:process-complete-order");

public class OrderAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            // First message
            List<OrderItem> items = new ArrayList<>();
            items.add(newExchange.getIn().getBody(OrderItem.class));
            newExchange.getIn().setBody(items);
            return newExchange;
        }
        // Subsequent messages
        List<OrderItem> items = oldExchange.getIn().getBody(List.class);
        items.add(newExchange.getIn().getBody(OrderItem.class));
        return oldExchange;
    }
}
```

### 2.4 Claim Check

Store message content temporarily and retrieve later (reduce payload size in transit).

```java
from("direct:large-payload")
    .claimCheck(ClaimCheckOperation.Push)   // Store body, replace with claim ticket
    .to("direct:lightweight-processing")     // Process without heavy payload
    .claimCheck(ClaimCheckOperation.Pop)     // Restore original body
    .to("direct:final-processing");
```

---

## 3. Messaging Patterns

### 3.1 Dead Letter Channel

Route failed messages to an error destination.

```java
// Global error handler
errorHandler(
    deadLetterChannel("kafka:dead-letter-queue")
        .maximumRedeliveries(3)
        .redeliveryDelay(2000)
        .backOffMultiplier(2)          // Exponential backoff
        .retryAttemptedLogLevel(LoggingLevel.WARN)
        .useOriginalMessage()          // Send original (not modified) to DLQ
);

from("kafka:orders")
    .to("direct:process");  // If fails after 3 retries → goes to DLQ
```

### 3.2 Idempotent Consumer

Prevent duplicate message processing.

```java
// Using Kafka headers as dedup key
from("kafka:payments")
    .idempotentConsumer(header("paymentId"))
        .idempotentRepository(
            MemoryIdempotentRepository.memoryIdempotentRepository(1000))
    .to("direct:process-payment");

// Using Redis for distributed idempotency
from("kafka:payments")
    .idempotentConsumer(header("paymentId"))
        .idempotentRepository(redisIdempotentRepository())
        .skipDuplicate(true)          // Skip silently
        .removeOnFailure(true)        // Allow retry if processing fails
    .to("direct:process-payment");

@Bean
public RedisStringIdempotentRepository redisIdempotentRepository() {
    return new RedisStringIdempotentRepository(redisTemplate(), "idempotent:");
}
```

### 3.3 Throttler

Limit the rate of messages.

```java
from("kafka:high-volume-events")
    .throttle(100)                    // Max 100 messages per second
        .timePeriodMillis(1000)
        .asyncDelayed()               // Non-blocking throttle
    .to("direct:process");

// Dynamic throttling
from("kafka:events")
    .throttle(method("rateLimitBean", "getCurrentLimit"))
    .to("direct:process");
```

### 3.4 Load Balancer

Distribute messages across multiple endpoints.

```java
// Round-robin
from("direct:call-service")
    .loadBalance().roundRobin()
        .to("http://service-1:8080/api", "http://service-2:8080/api", "http://service-3:8080/api")
    .end();

// Failover (try next if current fails)
from("direct:call-service")
    .loadBalance().failover(3, false, true)  // maxAttempts, inheritErrorHandler, roundRobin
        .to("http://primary:8080/api", "http://secondary:8080/api")
    .end();

// Weighted round-robin
from("direct:call-service")
    .loadBalance().weighted(true, "70,20,10")  // 70% to first, 20% to second
        .to("http://service-1/api", "http://service-2/api", "http://service-3/api")
    .end();
```

---

## 4. Saga Pattern (Distributed Transactions)

```java
from("direct:book-trip")
    .saga()
        .propagation(SagaPropagation.REQUIRED)
        .compensation("direct:cancelTrip")  // Called on failure
        .completion("direct:confirmTrip")   // Called on success
        .option("tripId", header("tripId"))
    .to("direct:book-flight")
    .to("direct:book-hotel")
    .to("direct:book-car");

from("direct:cancelTrip")
    .log("Compensating: cancelling trip ${header.tripId}")
    .to("direct:cancel-flight")
    .to("direct:cancel-hotel")
    .to("direct:cancel-car");
```

---

## 5. Process Manager (Orchestration)

```java
from("kafka:order-events")
    .routeId("order-orchestrator")
    .unmarshal().json(JsonLibrary.Jackson, OrderEvent.class)
    .choice()
        .when(simple("${body.status} == 'CREATED'"))
            .to("direct:validate-inventory")
        .when(simple("${body.status} == 'INVENTORY_RESERVED'"))
            .to("direct:process-payment")
        .when(simple("${body.status} == 'PAYMENT_CONFIRMED'"))
            .to("direct:ship-order")
        .when(simple("${body.status} == 'SHIPPED'"))
            .to("direct:send-notification")
    .end();
```

---

## 6. Scatter-Gather

Send to multiple services, aggregate responses.

```java
from("direct:get-best-price")
    .multicast(new BestPriceAggregator())
        .parallelProcessing()
        .timeout(3000)                    // 3s timeout per service
        .to("http://supplier-a/price", "http://supplier-b/price", "http://supplier-c/price")
    .end()
    .log("Best price: ${body}");

public class BestPriceAggregator implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) return newExchange;
        
        double oldPrice = oldExchange.getIn().getBody(PriceResponse.class).getPrice();
        double newPrice = newExchange.getIn().getBody(PriceResponse.class).getPrice();
        
        return newPrice < oldPrice ? newExchange : oldExchange;
    }
}
```

---

## 7. Pipes and Filters

Chain multiple processors sequentially.

```java
from("kafka:raw-data")
    .routeId("etl-pipeline")
    .to("direct:validate")       // Filter 1: Validate
    .to("direct:transform")      // Filter 2: Transform
    .to("direct:enrich")         // Filter 3: Enrich
    .to("direct:deduplicate")    // Filter 4: Deduplicate
    .to("direct:persist");       // Filter 5: Save

// Each "filter" is a separate route (reusable, testable)
from("direct:validate")
    .filter(method("validator", "isValid"))
    .bean("sanitizer");

from("direct:transform")
    .convertBodyTo(String.class)
    .unmarshal().json()
    .bean("transformer", "mapFields");
```

---

## 8. Pattern Quick Reference

| Pattern | Camel DSL | When to Use |
|---------|-----------|-------------|
| Content-Based Router | `.choice().when().end()` | Route by message content |
| Filter | `.filter()` | Drop unwanted messages |
| Splitter | `.split()` | Break batch into individuals |
| Aggregator | `.aggregate()` | Combine individuals into batch |
| Multicast | `.multicast()` | Send to multiple destinations |
| Recipient List | `.recipientList()` | Dynamic multiple destinations |
| Wire Tap | `.wireTap()` | Async copy for audit/monitoring |
| Dead Letter Channel | `errorHandler(deadLetterChannel())` | Handle failures |
| Idempotent Consumer | `.idempotentConsumer()` | Prevent duplicates |
| Throttler | `.throttle()` | Rate limiting |
| Load Balancer | `.loadBalance()` | Distribute across instances |
| Enricher | `.enrich()` | Add data from external source |
| Claim Check | `.claimCheck()` | Temporarily store large payloads |
| Saga | `.saga()` | Distributed transactions |
| Routing Slip | `.routingSlip()` | Dynamic processing pipeline |
