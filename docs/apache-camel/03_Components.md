# Module 3: Components Deep Dive

## 1. HTTP Component

### Producer (Outbound calls)

```java
// Simple GET
from("direct:getUser")
    .toD("http://user-service:8080/api/users/${header.userId}?httpMethod=GET")
    .unmarshal().json(JsonLibrary.Jackson, User.class);

// POST with body
from("direct:createUser")
    .marshal().json()
    .setHeader(Exchange.HTTP_METHOD, constant("POST"))
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
    .to("http://user-service:8080/api/users")
    .unmarshal().json(JsonLibrary.Jackson, User.class);

// With timeout and connection pool
from("direct:callExternal")
    .to("http://external-api.com/data"
        + "?httpMethod=GET"
        + "&connectTimeout=5000"
        + "&socketTimeout=10000"
        + "&httpClientConfigurer=#myHttpConfig");

@Bean("myHttpConfig")
public HttpClientConfigurer myHttpConfig() {
    return builder -> {
        builder.setMaxConnTotal(200);
        builder.setMaxConnPerRoute(50);
    };
}
```

### Consumer (Inbound — Platform HTTP)

```java
// Using platform-http (embedded in Spring Boot)
from("platform-http:/api/orders?httpMethodRestrict=POST")
    .unmarshal().json(JsonLibrary.Jackson, Order.class)
    .bean("orderService", "create")
    .marshal().json()
    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));
```

---

## 2. Kafka Component

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-kafka-starter</artifactId>
</dependency>
```

### Consumer

```java
from("kafka:order-events"
        + "?brokers=localhost:9092"
        + "&groupId=order-processor"
        + "&autoOffsetReset=earliest"
        + "&maxPollRecords=100"
        + "&consumersCount=3"              // 3 consumer threads
        + "&autoCommitEnable=false"        // Manual commit
        + "&allowManualCommit=true"
        + "&breakOnFirstError=true")       // Stop batch on error
    .routeId("kafka-consumer")
    .log("Offset: ${header.kafka.OFFSET}, Partition: ${header.kafka.PARTITION}")
    .unmarshal().json(JsonLibrary.Jackson, OrderEvent.class)
    .process(this::processEvent)
    .process(this::manualCommit);         // Commit after successful processing

private void manualCommit(Exchange exchange) {
    KafkaManualCommit manual = exchange.getIn()
        .getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
    if (manual != null) {
        manual.commit();
    }
}
```

### Producer

```java
from("direct:publish-event")
    .setHeader(KafkaConstants.KEY, simple("${body.orderId}"))
    .setHeader(KafkaConstants.PARTITION_KEY, simple("${body.customerId}"))
    .marshal().json()
    .to("kafka:order-events"
        + "?brokers=localhost:9092"
        + "&requestRequiredAcks=all"       // Wait for all replicas
        + "&retries=3"
        + "&compressionCodec=lz4"
        + "&lingerMs=5");
```

### Key Kafka Headers Available

| Header | Description |
|--------|-------------|
| `kafka.OFFSET` | Message offset |
| `kafka.PARTITION` | Partition number |
| `kafka.TOPIC` | Topic name |
| `kafka.TIMESTAMP` | Message timestamp |
| `kafka.KEY` | Message key |
| `kafka.HEADERS` | Kafka headers map |

---

## 3. File & FTP Component

### File Consumer (Polling)

```java
from("file:/data/inbox"
        + "?include=.*\\.csv"              // Only CSV files
        + "&move=.processed/${date:now:yyyyMMdd}/${file:name}"  // Move after processing
        + "&moveFailed=.error/${file:name}" // Move failed files here
        + "&readLock=changed"              // Wait until file stops changing
        + "&readLockCheckInterval=2000"    // Check every 2s
        + "&maxMessagesPerPoll=10"         // Process max 10 files per poll
        + "&sortBy=file:modified"          // Oldest first
        + "&recursive=true")              // Include subdirectories
    .routeId("file-processor")
    .log("Processing: ${header.CamelFileName} (${header.CamelFileLength} bytes)")
    .to("direct:process-file");
```

### File Producer (Write)

```java
from("direct:write-output")
    .setHeader(Exchange.FILE_NAME, simple("output-${date:now:yyyyMMddHHmmss}.json"))
    .to("file:/data/outbox?fileExist=Append&charset=UTF-8");
```

### SFTP

```java
from("sftp://user@sftp-server.com/uploads"
        + "?password=RAW(s3cr3t)"          // RAW() prevents URI encoding
        + "&include=.*\\.xml"
        + "&move=.done"
        + "&delay=30000"                   // Poll every 30s
        + "&stepwise=false"                // Faster for deep directories
        + "&binary=true")
    .to("direct:process-sftp-file");

// Upload file
from("direct:upload")
    .setHeader(Exchange.FILE_NAME, simple("${header.filename}"))
    .to("sftp://user@sftp-server.com/outgoing?password=RAW(s3cr3t)");
```

---

## 4. SQL & Database Component

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-sql-starter</artifactId>
</dependency>
```

### Query (Consumer — polling)

```java
// Poll for new orders every 5 seconds
from("sql:SELECT * FROM orders WHERE status = 'NEW' ORDER BY created_at LIMIT 100"
        + "?dataSource=#dataSource"
        + "&delay=5000"
        + "&onConsume=UPDATE orders SET status = 'PROCESSING' WHERE id = :#id"
        + "&outputType=SelectList")
    .routeId("poll-new-orders")
    .split(body())
        .to("direct:process-order")
    .end();
```

### Insert/Update (Producer)

```java
// Insert
from("direct:save-order")
    .to("sql:INSERT INTO orders (id, customer_id, amount, status) "
        + "VALUES (:#${body.id}, :#${body.customerId}, :#${body.amount}, 'CREATED')"
        + "?dataSource=#dataSource");

// Named parameters from body
from("direct:update-status")
    .to("sql:UPDATE orders SET status = :#status WHERE id = :#orderId"
        + "?dataSource=#dataSource");

// Batch insert
from("direct:batch-insert")
    .to("sql:INSERT INTO events (id, type, data) VALUES (:#id, :#type, :#data)"
        + "?dataSource=#dataSource&batch=true");
```

### Stored Procedures

```java
from("direct:call-proc")
    .to("sql:CALL process_order(:#orderId, :#amount)?dataSource=#dataSource");
```

---

## 5. JMS / ActiveMQ Component

```java
// Consume from queue
from("jms:queue:incoming-orders?concurrentConsumers=5&maxConcurrentConsumers=20")
    .routeId("jms-consumer")
    .transacted()                          // JMS transaction
    .to("direct:process-order");

// Send to queue
from("direct:send-to-queue")
    .setHeader("JMSPriority", constant(9))
    .setHeader("JMSExpiration", constant(60000))  // Expires in 60s
    .to("jms:queue:outgoing-orders?deliveryPersistent=true");

// Request-Reply (InOut)
from("direct:get-price")
    .to("jms:queue:price-service?replyTo=price-replies&requestTimeout=5000");

// Topic (pub/sub)
from("jms:topic:order-events")
    .to("direct:handle-event");
```

---

## 6. AWS Components

### S3

```java
// Download file from S3
from("aws2-s3://my-bucket"
        + "?region=us-east-1"
        + "&prefix=incoming/"
        + "&moveAfterRead=true"
        + "&destinationBucket=my-bucket-processed"
        + "&delay=10000")
    .routeId("s3-consumer")
    .log("S3 file: ${header.CamelAwsS3Key}")
    .to("direct:process-s3-file");

// Upload to S3
from("direct:upload-to-s3")
    .setHeader(AWS2S3Constants.KEY, simple("uploads/${date:now:yyyyMMdd}/${header.filename}"))
    .setHeader(AWS2S3Constants.CONTENT_TYPE, constant("application/json"))
    .to("aws2-s3://my-bucket?region=us-east-1");
```

### SQS

```java
// Consume SQS messages
from("aws2-sqs://my-queue"
        + "?region=us-east-1"
        + "&maxMessagesPerPoll=10"
        + "&waitTimeSeconds=20"            // Long polling
        + "&concurrentConsumers=3"
        + "&deleteAfterRead=true")
    .to("direct:process-sqs-message");

// Send to SQS
from("direct:send-sqs")
    .to("aws2-sqs://my-queue?region=us-east-1&messageDeduplicationIdStrategy=useContentBasedDeduplication");
```

### SNS

```java
from("direct:publish-notification")
    .setHeader(SnsConstants.SUBJECT, constant("Order Update"))
    .to("aws2-sns://order-notifications?region=us-east-1");
```

### DynamoDB

```java
// Put item
from("direct:save-to-dynamo")
    .setHeader(Ddb2Constants.OPERATION, constant(Ddb2Operations.PutItem))
    .to("aws2-ddb://orders-table?region=us-east-1");
```

---

## 7. Timer & Scheduler

### Timer (Simple periodic)

```java
from("timer:heartbeat?period=10000&delay=5000")  // Every 10s, start after 5s
    .routeId("heartbeat")
    .setBody(constant("PING"))
    .to("http://health-check-service/ping");
```

### Cron Scheduler

```java
// Using camel-quartz
from("cron:daily-report?schedule=0+0+8+*+*+?")  // Every day at 8 AM
    .routeId("daily-report")
    .to("sql:SELECT * FROM orders WHERE date = CURRENT_DATE?dataSource=#ds")
    .marshal().json()
    .to("direct:send-report-email");

// Spring cron syntax
from("scheduler:cleanup?scheduler.cron=0 */5 * * * *")  // Every 5 minutes
    .to("sql:DELETE FROM sessions WHERE expired_at < NOW()?dataSource=#ds");
```

---

## 8. Bean Component

```java
// Call specific method
from("kafka:orders")
    .bean("orderService", "processOrder");

// Auto-detect method by parameter type
from("kafka:orders")
    .bean(OrderService.class);  // Matches method that accepts Message body type

// With processor
from("kafka:orders")
    .process(exchange -> {
        String body = exchange.getIn().getBody(String.class);
        exchange.getIn().setBody(body.toUpperCase());
    });
```

---

## 9. MongoDB Component

```java
// Find
from("direct:find-user")
    .setHeader(MongoDbConstants.CRITERIA, constant(
        Filters.eq("email", "user@example.com")))
    .to("mongodb:mongoClient?database=mydb&collection=users&operation=findAll");

// Insert
from("direct:save-user")
    .to("mongodb:mongoClient?database=mydb&collection=users&operation=insert");

// Change stream (real-time)
from("mongodb:mongoClient?database=mydb&collection=orders"
        + "&consumerType=changeStreams"
        + "&streamFilter={\"operationType\":\"insert\"}")
    .log("New order inserted: ${body}");
```

---

## 10. Elasticsearch Component

```java
// Index document
from("direct:index-document")
    .setHeader(ElasticsearchConstants.OPERATION, constant("Index"))
    .setHeader(ElasticsearchConstants.INDEX_NAME, constant("orders"))
    .to("elasticsearch-rest://myCluster?hostAddresses=localhost:9200");

// Search
from("direct:search")
    .setHeader(ElasticsearchConstants.OPERATION, constant("Search"))
    .setBody(constant("{\"query\":{\"match\":{\"status\":\"active\"}}}"))
    .to("elasticsearch-rest://myCluster?hostAddresses=localhost:9200&indexName=orders");
```

---

## 11. Component Configuration (Spring Boot)

```yaml
# application.yml
camel:
  component:
    kafka:
      brokers: localhost:9092
      security-protocol: SASL_SSL
      sasl-mechanism: PLAIN
      
    sql:
      data-source: "#dataSource"
      
    aws2-s3:
      region: us-east-1
      
    jms:
      connection-factory: "#connectionFactory"
      concurrent-consumers: 5
      max-concurrent-consumers: 20
```

This eliminates repeating connection details in every endpoint URI:

```java
// Before (verbose)
from("kafka:orders?brokers=localhost:9092&securityProtocol=SASL_SSL")

// After (with component-level config)
from("kafka:orders")  // Uses camel.component.kafka.* properties
```
