# Module 7: Spring Boot Integration

## 1. Project Setup

### Maven Dependencies

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<properties>
    <camel.version>4.4.0</camel.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-spring-boot-bom</artifactId>
            <version>${camel.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Core -->
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Components (add what you need) -->
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-kafka-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-jackson-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-http-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-sql-starter</artifactId>
    </dependency>
    
    <!-- Resilience -->
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-resilience4j-starter</artifactId>
    </dependency>
    
    <!-- Observability -->
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-micrometer-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-opentelemetry-starter</artifactId>
    </dependency>
    
    <!-- Health -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

---

## 2. Configuration (application.yml)

```yaml
# application.yml
spring:
  application:
    name: order-integration-service

camel:
  springboot:
    # Main settings
    name: OrderIntegration
    main-run-controller: true          # Keep app running (non-web)
    shutdown-timeout: 30               # Graceful shutdown seconds
    
    # Route auto-discovery
    routes-include-pattern: classpath:routes/*.yaml  # Load YAML routes too
    
  # Component-level defaults
  component:
    kafka:
      brokers: ${KAFKA_BROKERS:localhost:9092}
      security-protocol: ${KAFKA_SECURITY:PLAINTEXT}
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      
    http:
      connect-timeout: 5000
      socket-timeout: 10000
      
    sql:
      data-source: "#dataSource"

  # Health checks
  health:
    enabled: true
    routes-enabled: true
    consumers-enabled: true

  # Metrics
  metrics:
    enable-route-event-notifier: true
    enable-exchange-event-notifier: true

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,camelroutes,metrics,prometheus
  endpoint:
    health:
      show-details: always
      
  health:
    camel:
      enabled: true
```

---

## 3. Route Organization

### Structure

```
src/main/java/com/example/
├── Application.java
├── routes/
│   ├── OrderRoutes.java          # Order processing routes
│   ├── PaymentRoutes.java        # Payment integration routes
│   └── NotificationRoutes.java   # Notification routes
├── processors/
│   ├── OrderValidator.java
│   └── OrderEnricher.java
├── aggregation/
│   └── OrderAggregationStrategy.java
├── config/
│   └── CamelConfig.java
└── model/
    └── Order.java
```

### Multiple RouteBuilders

```java
// Each RouteBuilder is a @Component auto-discovered by Camel
@Component
public class OrderRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("kafka:new-orders").routeId("order-ingestion")
            .to("direct:validate")
            .to("direct:process");
    }
}

@Component
public class PaymentRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:charge-payment").routeId("payment-processing")
            .circuitBreaker()
                .to("http://payment-service/api/charge")
            .onFallback()
                .to("seda:payment-retry")
            .end();
    }
}
```

---

## 4. Spring Beans in Routes

```java
@Service
public class OrderService {

    private final OrderRepository repository;
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    // Camel will call this method, auto-binding parameters
    public Order processOrder(@Body Order order, 
                              @Header("source") String source) {
        order.setStatus("PROCESSING");
        order.setSource(source);
        return repository.save(order);
    }

    public void validate(@Body Order order) {
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Invalid order amount");
        }
    }
}

// In route
@Component
public class OrderRoutes extends RouteBuilder {
    @Override
    public void configure() {
        from("kafka:orders")
            .bean("orderService", "validate")      // By bean name
            .bean(OrderService.class, "processOrder")  // By class
            .marshal().json()
            .to("kafka:processed-orders");
    }
}
```

---

## 5. Property Placeholders & Profiles

```yaml
# application.yml (common)
app:
  kafka:
    topic:
      input: raw-orders
      output: processed-orders
      dlq: dead-letters
  retry:
    max-attempts: 3
    delay: 2000
    
---
# application-dev.yml
camel:
  component:
    kafka:
      brokers: localhost:9092
      
---
# application-prod.yml
camel:
  component:
    kafka:
      brokers: kafka-1:9092,kafka-2:9092,kafka-3:9092
      security-protocol: SASL_SSL
```

```java
@Component
public class OrderRoutes extends RouteBuilder {
    
    @Value("${app.kafka.topic.input}")
    private String inputTopic;
    
    @Value("${app.retry.max-attempts}")
    private int maxRetries;

    @Override
    public void configure() {
        errorHandler(deadLetterChannel("kafka:{{app.kafka.topic.dlq}}")
            .maximumRedeliveries(maxRetries)
            .redeliveryDelay({{app.retry.delay}}));

        from("kafka:" + inputTopic)
            .to("kafka:{{app.kafka.topic.output}}");  // {{}} = Camel property placeholder
    }
}
```

---

## 6. Actuator & Health Checks

### Custom Health Check

```java
@Component
public class KafkaHealthCheck extends AbstractCamelMicroProfileHealthCheck {

    public KafkaHealthCheck() {
        super("kafka-connectivity");
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        try {
            // Check Kafka connectivity
            AdminClient admin = AdminClient.create(Map.of(
                "bootstrap.servers", "localhost:9092"));
            admin.listTopics().names().get(5, TimeUnit.SECONDS);
            builder.up();
        } catch (Exception e) {
            builder.down().error(e);
        }
    }
}
```

### Actuator Endpoints

```
GET /actuator/health          → Overall health (includes Camel routes)
GET /actuator/health/camel    → Camel-specific health
GET /actuator/camelroutes     → List all routes with status
GET /actuator/metrics/camel.exchanges.total → Exchange count
GET /actuator/prometheus      → Prometheus metrics export
```

**Sample health response:**
```json
{
  "status": "UP",
  "components": {
    "camel": {
      "status": "UP",
      "details": {
        "name": "OrderIntegration",
        "version": "4.4.0",
        "uptimeMillis": 3600000,
        "routes.total": 5,
        "routes.started": 5
      }
    },
    "camelHealth": {
      "status": "UP",
      "details": {
        "route:order-ingestion": "UP",
        "route:payment-processing": "UP",
        "consumer:kafka-orders": "UP"
      }
    }
  }
}
```

---

## 7. Route Control via JMX/Actuator

```java
// Programmatic route control
@RestController
@RequestMapping("/admin/routes")
public class RouteAdminController {

    @Autowired
    private CamelContext camelContext;

    @PostMapping("/{routeId}/stop")
    public String stopRoute(@PathVariable String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
        return "Route " + routeId + " stopped";
    }

    @PostMapping("/{routeId}/start")
    public String startRoute(@PathVariable String routeId) throws Exception {
        camelContext.getRouteController().startRoute(routeId);
        return "Route " + routeId + " started";
    }

    @GetMapping
    public List<Map<String, String>> listRoutes() {
        return camelContext.getRoutes().stream()
            .map(r -> Map.of(
                "id", r.getRouteId(),
                "status", camelContext.getRouteController().getRouteStatus(r.getRouteId()).name(),
                "uptime", r.getUptime()
            ))
            .toList();
    }
}
```

---

## 8. Graceful Shutdown

```yaml
camel:
  springboot:
    shutdown-timeout: 60                    # Max wait time
    
server:
  shutdown: graceful                        # Spring Boot graceful shutdown

spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s
```

```java
// Route-level shutdown strategy
from("kafka:orders")
    .routeId("order-processor")
    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)  // Finish inflight
    .shutdownRoute(ShutdownRoute.Defer)                        // Shut down last
    .to("direct:process");
```

Shutdown order:
1. Stop accepting new messages (consumers stop polling)
2. Wait for in-flight exchanges to complete (up to timeout)
3. Close producers and connections
4. Destroy CamelContext

---

## 9. Multi-Module Project Structure

```
parent-pom/
├── pom.xml (parent)
├── common/
│   ├── pom.xml
│   └── src/main/java/
│       └── model/, dto/, util/
├── order-service/
│   ├── pom.xml
│   └── src/main/java/
│       └── routes/, processors/, config/
├── payment-service/
│   ├── pom.xml
│   └── src/main/java/
│       └── routes/, processors/
└── shared-routes/
    ├── pom.xml
    └── src/main/java/
        └── common routes reused across services
```

This allows sharing models/DTOs while keeping routes in separate deployable services.
