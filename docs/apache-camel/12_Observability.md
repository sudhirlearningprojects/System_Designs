# Module 12: Observability

## 1. Metrics (Micrometer)

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-micrometer-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Auto-Generated Metrics

Camel automatically exposes these metrics:

| Metric | Description |
|--------|-------------|
| `camel.exchanges.total` | Total exchanges processed |
| `camel.exchanges.succeeded` | Successful exchanges |
| `camel.exchanges.failed` | Failed exchanges |
| `camel.exchanges.inflight` | Currently processing |
| `camel.exchange.processing.duration` | Processing time |
| `camel.route.exchanges.total` | Per-route exchange count |
| `camel.route.processing.duration` | Per-route latency |

### Configuration

```yaml
camel:
  metrics:
    enable-route-event-notifier: true
    enable-exchange-event-notifier: true
    naming-strategy: default

management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:local}
```

### Custom Metrics in Routes

```java
from("kafka:orders")
    .routeId("order-processor")
    .to("micrometer:counter:orders.received?tags=source=${header.source}")
    .bean("orderService", "process")
    .to("micrometer:timer:orders.processing.time?action=stop")
    .choice()
        .when(simple("${body.status} == 'HIGH_VALUE'"))
            .to("micrometer:counter:orders.high_value")
    .end();

// Custom gauge
@Component
public class OrderMetrics {
    private final MeterRegistry registry;
    private final AtomicInteger pendingOrders = new AtomicInteger(0);

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("orders.pending", pendingOrders);
    }

    public void incrementPending() { pendingOrders.incrementAndGet(); }
    public void decrementPending() { pendingOrders.decrementAndGet(); }
}
```

---

## 2. Distributed Tracing (OpenTelemetry)

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-opentelemetry-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

```yaml
# application.yml
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  resource:
    attributes:
      service.name: order-integration-service
      deployment.environment: production

camel:
  opentelemetry:
    enabled: true
    exclude-patterns: "timer:*,direct:internal-*"  # Don't trace internal timers
```

### Trace Propagation

Camel automatically propagates trace context across:
- Kafka messages (via headers)
- HTTP calls (via W3C Trace Context headers)
- JMS messages (via properties)
- Direct/SEDA (in-memory)

```java
// Traces flow automatically:
// Service A → Kafka → Service B → HTTP → Service C
// All appear in the same distributed trace

from("kafka:orders")  // Trace context extracted from Kafka headers
    .routeId("order-processor")
    .bean("orderService", "process")
    .to("http://payment-service/api/charge")  // Trace context injected into HTTP headers
    .to("kafka:completed-orders");            // Trace context injected into Kafka headers
```

### Custom Spans

```java
from("kafka:orders")
    .process(exchange -> {
        Span span = Span.current();
        span.setAttribute("order.id", exchange.getIn().getHeader("orderId", String.class));
        span.setAttribute("order.amount", exchange.getIn().getBody(Order.class).getAmount().doubleValue());
    })
    .bean("orderService", "process");
```

---

## 3. Logging

### Structured Logging (JSON)

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>camel.routeId</includeMdcKeyName>
            <includeMdcKeyName>camel.exchangeId</includeMdcKeyName>
            <includeMdcKeyName>correlationId</includeMdcKeyName>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### MDC (Mapped Diagnostic Context) in Routes

```java
from("kafka:orders")
    .routeId("order-processor")
    .process(exchange -> {
        MDC.put("orderId", exchange.getIn().getHeader("orderId", String.class));
        MDC.put("customerId", exchange.getIn().getHeader("customerId", String.class));
    })
    .log("Processing order ${header.orderId}")   // MDC values appear in JSON log
    .bean("orderService", "process")
    .log("Order processed successfully");

// Camel auto-adds: camel.routeId, camel.exchangeId to MDC
```

### Log EIP with Levels

```java
from("kafka:orders")
    .log(LoggingLevel.INFO, "com.example.orders", "Received: ${header.orderId}")
    .bean("service")
    .log(LoggingLevel.DEBUG, "com.example.orders", "Body after processing: ${body}");
```

### Wire Logging (Debug HTTP requests/responses)

```yaml
# Log all HTTP request/response bodies (DEBUG only!)
logging:
  level:
    org.apache.camel.component.http: DEBUG
    
# Or per-route
camel:
  springboot:
    tracing: true  # Log each step in every route
```

---

## 4. Health Checks

```yaml
camel:
  health:
    enabled: true
    routes-enabled: true         # Route-level health
    consumers-enabled: true      # Consumer connectivity health
    producers-enabled: true      # Producer connectivity health
    
management:
  endpoint:
    health:
      show-details: always
      group:
        readiness:
          include: camel,db,kafka
        liveness:
          include: ping,camel
```

### Custom Health Check

```java
@Component
public class KafkaLagHealthCheck extends AbstractHealthCheck {

    private final AdminClient adminClient;

    public KafkaLagHealthCheck(AdminClient adminClient) {
        super("kafka-consumer-lag");
        this.adminClient = adminClient;
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        try {
            long totalLag = calculateConsumerLag();
            builder.detail("consumer-lag", totalLag);
            
            if (totalLag > 100000) {
                builder.down();
                builder.detail("reason", "Consumer lag too high: " + totalLag);
            } else {
                builder.up();
            }
        } catch (Exception e) {
            builder.down().error(e);
        }
    }
}
```

---

## 5. Alerting on Camel Metrics

```yaml
# prometheus-alerts.yml
groups:
  - name: camel-integration
    rules:
      - alert: HighExchangeFailureRate
        expr: |
          rate(camel_exchanges_failed_total[5m]) / rate(camel_exchanges_total[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Exchange failure rate > 5% on {{ $labels.routeId }}"

      - alert: HighProcessingLatency
        expr: |
          histogram_quantile(0.99, rate(camel_exchange_processing_duration_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "p99 latency > 5s on route {{ $labels.routeId }}"

      - alert: ExchangesInflight
        expr: camel_exchanges_inflight > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Over 1000 inflight exchanges"

      - alert: RouteDown
        expr: camel_route_status != 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Route {{ $labels.routeId }} is not running"
```

---

## 6. Grafana Dashboard (Key Panels)

```
┌─────────────────────────────────────────────────────────────┐
│  Camel Integration Service Dashboard                         │
├────────────────┬────────────────┬───────────────────────────┤
│ Exchanges/sec  │ Error Rate     │ Inflight Exchanges        │
│  [Line Graph]  │  [Line Graph]  │  [Gauge: 45/500]          │
├────────────────┴────────────────┴───────────────────────────┤
│                                                              │
│  Processing Latency (p50, p95, p99) per Route               │
│  [Multi-line chart]                                         │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  Route Status Table                                          │
│  ┌──────────────────┬────────┬──────────┬─────────────────┐│
│  │ Route ID         │ Status │ Rate/sec │ Error Rate      ││
│  │ order-processor  │ ✅ UP  │ 150      │ 0.2%            ││
│  │ payment-handler  │ ✅ UP  │ 80       │ 1.5%            ││
│  │ notification     │ ⚠️ SLOW│ 45       │ 0.1%            ││
│  └──────────────────┴────────┴──────────┴─────────────────┘│
├──────────────────────────────────────────────────────────────┤
│  Kafka Consumer Lag per Partition                            │
│  [Bar chart]                                                │
└──────────────────────────────────────────────────────────────┘
```

### PromQL Queries

```promql
# Throughput per route
rate(camel_exchanges_total{routeId="order-processor"}[5m])

# Error rate percentage
100 * rate(camel_exchanges_failed_total[5m]) / rate(camel_exchanges_total[5m])

# p99 latency
histogram_quantile(0.99, rate(camel_exchange_processing_duration_seconds_bucket[5m]))

# Inflight
camel_exchanges_inflight

# Success ratio
rate(camel_exchanges_succeeded_total[5m]) / rate(camel_exchanges_total[5m])
```
