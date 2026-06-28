# Module 1: Apache Camel Fundamentals

## 1. Core Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       CamelContext                                │
│  (The runtime container — like Spring ApplicationContext)        │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                        Route                              │   │
│  │                                                           │   │
│  │  ┌─────────┐    ┌───────────┐    ┌──────────────────┐   │   │
│  │  │Endpoint │───▶│ Processor │───▶│   Endpoint        │   │   │
│  │  │ (from)  │    │ (EIP/Bean)│    │   (to)            │   │   │
│  │  └─────────┘    └───────────┘    └──────────────────┘   │   │
│  │      ▲                                                    │   │
│  │      │                                                    │   │
│  │  Component (factory for endpoints)                        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────┐  ┌──────────────┐  ┌─────────────────────┐       │
│  │ Registry │  │Type Converter│  │ Error Handler        │       │
│  └──────────┘  └──────────────┘  └─────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### Key Concepts

| Concept | Description | Analogy |
|---------|-------------|---------|
| **CamelContext** | Runtime container that manages routes, components, and lifecycle | Spring ApplicationContext |
| **Route** | A pipeline connecting a source to destination(s) with processing steps | A conveyor belt |
| **Endpoint** | An addressable resource (Kafka topic, REST URL, file directory) | A postal address |
| **Component** | Factory that creates endpoints (kafka, http, file, jms) | A protocol driver |
| **Exchange** | The message container flowing through a route | An envelope |
| **Message** | The actual payload (headers + body) | The letter inside |
| **Processor** | Code that transforms/routes the exchange | A worker on the belt |
| **Producer** | Sends messages to an endpoint | Sender |
| **Consumer** | Receives messages from an endpoint | Receiver |

---

## 2. The Exchange & Message Model

```
┌─────────────────── Exchange ──────────────────────┐
│                                                    │
│  exchangeId: "ID-abc123"                          │
│  pattern: InOut | InOnly                          │
│  properties: {CamelToEndpoint, CamelRouteId, ...} │
│                                                    │
│  ┌─────────── IN Message ───────────┐             │
│  │  headers: {                      │             │
│  │    Content-Type: application/json│             │
│  │    CamelFileName: orders.csv     │             │
│  │    kafka.OFFSET: 42              │             │
│  │  }                               │             │
│  │  body: { "orderId": 123, ... }   │             │
│  └──────────────────────────────────┘             │
│                                                    │
│  ┌─────────── OUT Message ──────────┐             │
│  │  (response for InOut exchanges)  │             │
│  │  headers: { ... }                │             │
│  │  body: { "status": "OK" }        │             │
│  └──────────────────────────────────┘             │
│                                                    │
│  exception: null (or caught exception)            │
└────────────────────────────────────────────────────┘
```

### Exchange Patterns

| Pattern | Use Case | Example |
|---------|----------|---------|
| **InOnly** | Fire-and-forget | Send to Kafka, write file |
| **InOut** | Request-reply | HTTP call, DB query |

```java
// InOnly (default for most messaging)
from("kafka:orders")
    .to("jms:queue:process-orders");

// InOut (request-reply)
from("direct:getUser")
    .to("http://user-service/api/users/${header.userId}");
```

---

## 3. Endpoint URIs

Every endpoint is addressed by a URI:

```
component:path?option1=value1&option2=value2
```

**Examples:**
```
kafka:my-topic?brokers=localhost:9092&groupId=myGroup
file:/data/inbox?noop=true&include=.*\.csv
http://api.example.com/users?httpMethod=GET
jms:queue:orders?concurrentConsumers=5
timer:heartbeat?period=5000
direct:processOrder
seda:async-queue?concurrentConsumers=10
sql:SELECT * FROM orders WHERE status = :#status?dataSource=#myDS
aws2-s3://my-bucket?region=us-east-1
```

---

## 4. RouteBuilder — Defining Routes

### Java DSL (Most Common)

```java
@Component
public class OrderRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        // Route 1: Consume from Kafka, process, save to DB
        from("kafka:new-orders?brokers=localhost:9092&groupId=order-processor")
            .routeId("process-orders")
            .log("Received order: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .process(this::validateOrder)
            .to("jpa:org.example.Order")
            .log("Order saved: ${body.orderId}");

        // Route 2: REST API that triggers processing
        from("direct:createOrder")
            .routeId("create-order-api")
            .marshal().json()
            .to("kafka:new-orders");

        // Route 3: File polling
        from("file:/data/orders?include=.*\\.csv&move=.done")
            .routeId("file-ingestion")
            .split(body().tokenize("\n")).streaming()
                .to("direct:processLine")
            .end();
    }

    private void validateOrder(Exchange exchange) {
        Order order = exchange.getIn().getBody(Order.class);
        if (order.getAmount() <= 0) {
            throw new ValidationException("Invalid order amount");
        }
    }
}
```

### YAML DSL (Camel 4.x — great for Kubernetes/GitOps)

```yaml
# routes.yaml
- route:
    id: process-orders
    from:
      uri: kafka:new-orders
      parameters:
        brokers: localhost:9092
        groupId: order-processor
    steps:
      - log: "Received order: ${body}"
      - unmarshal:
          json:
            library: Jackson
            unmarshalType: org.example.Order
      - bean:
          ref: orderValidator
          method: validate
      - to: jpa:org.example.Order
      - log: "Order saved: ${body.orderId}"
```

### XML DSL (Legacy, still supported)

```xml
<routes xmlns="http://camel.apache.org/schema/spring">
    <route id="process-orders">
        <from uri="kafka:new-orders?brokers=localhost:9092"/>
        <log message="Received: ${body}"/>
        <unmarshal><json library="Jackson"/></unmarshal>
        <to uri="jpa:org.example.Order"/>
    </route>
</routes>
```

---

## 5. Components — The Connectors

Camel has **300+ components** out of the box. Key ones:

### Messaging

| Component | URI Prefix | Description |
|-----------|-----------|-------------|
| Kafka | `kafka:topic` | Apache Kafka producer/consumer |
| JMS | `jms:queue:name` | Any JMS broker (ActiveMQ, RabbitMQ) |
| AMQP | `amqp:queue:name` | AMQP 1.0 protocol |
| AWS SQS | `aws2-sqs:queue` | Amazon SQS |
| Google Pub/Sub | `google-pubsub:project:topic` | GCP Pub/Sub |

### HTTP/REST

| Component | URI Prefix | Description |
|-----------|-----------|-------------|
| HTTP | `http://host/path` | HTTP client (producer only) |
| Netty HTTP | `netty-http:http://0.0.0.0:8080` | Async HTTP server |
| Platform HTTP | `platform-http:/path` | Embedded HTTP (Spring Boot/Quarkus) |
| REST | `rest:get:/path` | REST DSL |

### Data

| Component | URI Prefix | Description |
|-----------|-----------|-------------|
| File | `file:/path` | File system read/write |
| FTP/SFTP | `sftp://host/path` | FTP file transfer |
| SQL | `sql:SELECT...` | JDBC queries |
| JPA | `jpa:EntityClass` | JPA persistence |
| MongoDB | `mongodb:connectionBean` | MongoDB operations |
| Elasticsearch | `elasticsearch-rest://cluster` | ES indexing/search |

### Cloud

| Component | URI Prefix | Description |
|-----------|-----------|-------------|
| AWS S3 | `aws2-s3://bucket` | S3 operations |
| AWS Lambda | `aws2-lambda:function` | Invoke Lambda |
| AWS DynamoDB | `aws2-ddb:table` | DynamoDB ops |
| Azure Blob | `azure-storage-blob://account/container` | Blob storage |
| GCS | `google-storage://bucket` | Google Cloud Storage |

### Internal

| Component | URI Prefix | Description |
|-----------|-----------|-------------|
| Direct | `direct:name` | Synchronous in-memory call |
| SEDA | `seda:name` | Async in-memory queue |
| Timer | `timer:name` | Periodic trigger |
| Scheduler | `scheduler:name` | Cron-based trigger |
| Bean | `bean:beanName` | Call a Spring bean |
| Log | `log:category` | Logging |

---

## 6. Direct vs SEDA vs VM

```java
// DIRECT: Synchronous, same thread, same CamelContext
// Use for: breaking routes into reusable sub-routes
from("kafka:orders")
    .to("direct:validate")      // Blocks until validate route completes
    .to("direct:enrich")
    .to("direct:save");

from("direct:validate")
    .bean(validator, "validate");

// SEDA: Asynchronous, different thread pool, same CamelContext
// Use for: parallel processing, decoupling, buffering
from("kafka:orders")
    .to("seda:process?concurrentConsumers=5");  // Non-blocking, queued

from("seda:process?concurrentConsumers=5")
    .bean(processor, "process");  // 5 threads consume from internal queue

// VM: Same as SEDA but across CamelContexts (multiple JVMs/deployments)
from("vm:shared-queue")
    .to("bean:handler");
```

**Decision guide:**
```
Need synchronous call? → direct:
Need async with buffering? → seda:
Need cross-CamelContext? → vm:
Need cross-JVM? → kafka: or jms:
```

---

## 7. CamelContext Lifecycle

```
     CREATED → INITIALIZING → INITIALIZED → STARTING → STARTED
                                                           │
                                                           │ (running)
                                                           │
     STOPPED ← STOPPING ←─────────────────────────────────┘
```

```java
// Programmatic control (rarely needed with Spring Boot)
CamelContext context = new DefaultCamelContext();
context.addRoutes(new MyRouteBuilder());
context.start();  // Starts all routes
// ... application runs ...
context.stop();   // Graceful shutdown
```

### Graceful Shutdown

```yaml
# application.yml
camel:
  springboot:
    shutdown-timeout: 30  # Wait up to 30s for inflight exchanges
    
  main:
    shutdown-timeout: 30
```

```java
// Programmatic shutdown config
from("kafka:orders")
    .routeId("order-processor")
    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)  // Finish all
    .process(this::processOrder);
```

---

## 8. Registry & Bean Integration

Camel integrates with Spring's bean registry:

```java
@Component("orderValidator")
public class OrderValidator {
    public void validate(@Body Order order, @Header("source") String source) {
        if (order.getAmount() <= 0) {
            throw new ValidationException("Invalid amount");
        }
    }
}

// Use in route
from("kafka:orders")
    .bean("orderValidator", "validate");  // Calls the Spring bean

// Or using method reference
from("kafka:orders")
    .bean(OrderValidator.class);  // Auto-discovers method by parameter types
```

### Parameter Binding Annotations

```java
public class MyProcessor {
    
    public String process(
        @Body String body,                    // Message body
        @Header("orderId") String orderId,    // Single header
        @Headers Map<String, Object> headers, // All headers
        @ExchangeProperty("key") String prop, // Exchange property
        @Simple("${header.amount}") int amount // Simple expression
    ) {
        return "Processed: " + orderId;
    }
}
```

---

## 9. Simple Expression Language

Camel's built-in expression language for accessing exchange data:

```java
// In route definitions
from("kafka:orders")
    .log("Order ${header.orderId} with amount ${body.amount}")
    .setHeader("priority", simple("${body.amount > 1000 ? 'HIGH' : 'LOW'}"))
    .filter(simple("${body.status} == 'NEW'"))
    .choice()
        .when(simple("${header.priority} == 'HIGH'"))
            .to("direct:priority-queue")
        .otherwise()
            .to("direct:normal-queue");
```

**Common expressions:**

| Expression | Description |
|-----------|-------------|
| `${body}` | Message body |
| `${body.fieldName}` | Body field (if POJO/Map) |
| `${header.name}` | Header value |
| `${exchangeProperty.key}` | Exchange property |
| `${exchange.exchangeId}` | Exchange ID |
| `${routeId}` | Current route ID |
| `${camelId}` | CamelContext name |
| `${date:now:yyyy-MM-dd}` | Current date formatted |
| `${bean:myBean.method}` | Call bean method |
| `${env:VAR_NAME}` | Environment variable |
| `${sys:java.version}` | System property |

---

## 10. Your First Complete Application

```java
@SpringBootApplication
public class CamelApplication {
    public static void main(String[] args) {
        SpringApplication.run(CamelApplication.class, args);
    }
}

@Component
public class FileToKafkaRoute extends RouteBuilder {
    
    @Override
    public void configure() {
        // Error handling for all routes in this builder
        errorHandler(deadLetterChannel("kafka:dead-letters")
            .maximumRedeliveries(3)
            .redeliveryDelay(1000));

        // Route: Read CSV files → Transform → Send to Kafka
        from("file:/data/inbox?include=.*\\.csv&move=.processed&moveFailed=.error")
            .routeId("csv-to-kafka")
            .log("Processing file: ${header.CamelFileName}")
            .split(body().tokenize("\n")).streaming()
                .filter(simple("${body} != ''"))  // Skip empty lines
                .unmarshal().csv()
                .process(this::csvToJson)
                .marshal().json()
                .to("kafka:processed-records?brokers=localhost:9092")
            .end()
            .log("File processed: ${header.CamelFileName}");
    }

    private void csvToJson(Exchange exchange) {
        List<String> row = exchange.getIn().getBody(List.class);
        Map<String, String> record = Map.of(
            "id", row.get(0),
            "name", row.get(1),
            "amount", row.get(2)
        );
        exchange.getIn().setBody(record);
    }
}
```
