# Module 14: Real-World Projects

## Project 1: File ETL Pipeline

**Scenario**: Ingest CSV files from SFTP, transform, load into database + Kafka.

```java
@Component
public class FileEtlRoute extends RouteBuilder {

    @Override
    public void configure() {
        errorHandler(deadLetterChannel("kafka:etl-dlq")
            .maximumRedeliveries(3).redeliveryDelay(5000).useOriginalMessage());

        // Route 1: Poll SFTP for new files
        from("sftp://{{sftp.host}}/incoming"
                + "?username={{sftp.user}}&password=RAW({{sftp.password}})"
                + "&include=.*\\.csv&move=.processed/${date:now:yyyyMMdd}/${file:name}"
                + "&delay=30000&readLock=changed")
            .routeId("sftp-poller")
            .log("New file: ${header.CamelFileName} (${header.CamelFileLength} bytes)")
            .to("direct:process-csv");

        // Route 2: Parse CSV, validate, transform
        from("direct:process-csv")
            .routeId("csv-processor")
            .split(body().tokenize("\n")).streaming()
                .filter(simple("${exchangeProperty.CamelSplitIndex} > 0"))  // Skip header
                .bean("csvParser", "parseLine")
                .bean("validator", "validate")
                .choice()
                    .when(simple("${body.valid}"))
                        .to("seda:persist?concurrentConsumers=5")
                    .otherwise()
                        .to("kafka:validation-errors")
                .end()
            .end()
            .log("File ${header.CamelFileName} processed: ${exchangeProperty.CamelSplitSize} rows");

        // Route 3: Persist to DB + publish event
        from("seda:persist?concurrentConsumers=5")
            .routeId("db-persister")
            .to("sql:INSERT INTO records (id, name, amount, created_at) "
                + "VALUES (:#${body.id}, :#${body.name}, :#${body.amount}, NOW())")
            .marshal().json()
            .to("kafka:new-records");
    }
}
```

---

## Project 2: Event-Driven Order Processing

**Scenario**: Microservice orchestration with saga pattern.

```java
@Component
public class OrderSagaRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Saga compensations
        onException(PaymentException.class)
            .handled(true)
            .to("direct:compensate-inventory")
            .to("kafka:order-failed");

        // Main orchestration
        from("kafka:order-created?groupId=order-saga")
            .routeId("order-saga-orchestrator")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .setHeader("orderId", simple("${body.id}"))
            .log("Starting saga for order ${header.orderId}")

            // Step 1: Reserve inventory
            .to("direct:reserve-inventory")
            
            // Step 2: Process payment
            .to("direct:process-payment")
            
            // Step 3: Confirm order
            .to("direct:confirm-order")
            
            .marshal().json()
            .to("kafka:order-completed")
            .log("Saga completed for order ${header.orderId}");

        // Reserve Inventory
        from("direct:reserve-inventory")
            .routeId("reserve-inventory")
            .circuitBreaker()
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setBody(simple("{\"orderId\":\"${header.orderId}\",\"items\":${body.items}}"))
                .to("http://inventory-service/api/reserve")
            .onFallback()
                .throwException(new InventoryException("Inventory service unavailable"))
            .end()
            .log("Inventory reserved for ${header.orderId}");

        // Process Payment
        from("direct:process-payment")
            .routeId("process-payment")
            .circuitBreaker()
                .resilience4jConfiguration()
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(30)
                .end()
                .setBody(simple("{\"orderId\":\"${header.orderId}\",\"amount\":${body.totalAmount}}"))
                .to("http://payment-service/api/charge")
            .onFallback()
                .throwException(new PaymentException("Payment failed"))
            .end()
            .log("Payment processed for ${header.orderId}");

        // Compensation: Release inventory
        from("direct:compensate-inventory")
            .routeId("compensate-inventory")
            .log(LoggingLevel.WARN, "Compensating: releasing inventory for ${header.orderId}")
            .setBody(simple("{\"orderId\":\"${header.orderId}\"}"))
            .to("http://inventory-service/api/release")
            .to("kafka:inventory-compensated");

        // Confirm Order
        from("direct:confirm-order")
            .routeId("confirm-order")
            .to("sql:UPDATE orders SET status = 'CONFIRMED' WHERE id = :#${header.orderId}")
            .bean("notificationService", "sendConfirmation");
    }
}
```

---

## Project 3: Real-Time Data Sync (CDC)

**Scenario**: Sync database changes to Elasticsearch in real-time.

```java
@Component
public class CdcSyncRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Consume Debezium CDC events from Kafka
        from("kafka:dbserver1.public.products"
                + "?groupId=es-sync&autoOffsetReset=earliest&autoCommitEnable=false&allowManualCommit=true")
            .routeId("cdc-es-sync")
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .choice()
                .when(jsonpath("$.op").isEqualTo("c"))   // CREATE
                    .to("direct:es-index")
                .when(jsonpath("$.op").isEqualTo("u"))   // UPDATE
                    .to("direct:es-update")
                .when(jsonpath("$.op").isEqualTo("d"))   // DELETE
                    .to("direct:es-delete")
            .end()
            .process(this::commitOffset);

        // Index new document
        from("direct:es-index")
            .routeId("es-index")
            .process(exchange -> {
                Map<String, Object> event = exchange.getIn().getBody(Map.class);
                Map<String, Object> after = (Map<String, Object>) event.get("after");
                exchange.getIn().setHeader("documentId", after.get("id"));
                exchange.getIn().setBody(after);
            })
            .marshal().json()
            .setHeader("ElasticsearchOperation", constant("Index"))
            .setHeader("ElasticsearchIndexName", constant("products"))
            .toD("elasticsearch-rest://cluster?hostAddresses={{es.hosts}}&indexName=products");

        // Delete document
        from("direct:es-delete")
            .routeId("es-delete")
            .process(exchange -> {
                Map<String, Object> event = exchange.getIn().getBody(Map.class);
                Map<String, Object> before = (Map<String, Object>) event.get("before");
                exchange.getIn().setHeader("documentId", before.get("id"));
            })
            .setHeader("ElasticsearchOperation", constant("Delete"))
            .toD("elasticsearch-rest://cluster?hostAddresses={{es.hosts}}&indexName=products");
    }
}
```

---

## Project 4: API Gateway / Aggregator

**Scenario**: Aggregate responses from multiple backend services.

```java
@Component
public class ApiGatewayRoute extends RouteBuilder {

    @Override
    public void configure() {
        restConfiguration()
            .component("platform-http")
            .bindingMode(RestBindingMode.json);

        // BFF endpoint: Get complete user profile (aggregated)
        rest("/api/v1/users")
            .get("/{userId}/profile")
                .to("direct:aggregate-profile");

        from("direct:aggregate-profile")
            .routeId("profile-aggregator")
            .setHeader("userId", header("userId"))
            .multicast(new ProfileAggregationStrategy())
                .parallelProcessing()
                .timeout(3000)  // 3s timeout for all calls
                .to("direct:get-user-info",
                    "direct:get-user-orders",
                    "direct:get-user-preferences")
            .end()
            .marshal().json();

        from("direct:get-user-info")
            .routeId("fetch-user-info")
            .circuitBreaker()
                .toD("http://user-service/api/users/${header.userId}")
            .onFallback()
                .setBody(constant("{\"name\":\"Unknown\",\"fallback\":true}"))
            .end();

        from("direct:get-user-orders")
            .routeId("fetch-user-orders")
            .circuitBreaker()
                .toD("http://order-service/api/users/${header.userId}/orders?limit=5")
            .onFallback()
                .setBody(constant("{\"orders\":[],\"fallback\":true}"))
            .end();

        from("direct:get-user-preferences")
            .routeId("fetch-user-preferences")
            .circuitBreaker()
                .toD("http://preference-service/api/users/${header.userId}/prefs")
            .onFallback()
                .setBody(constant("{\"preferences\":{},\"fallback\":true}"))
            .end();
    }
}

public class ProfileAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("data", new ArrayList<>());
            newExchange.getIn().setBody(profile);
            ((List) ((Map) newExchange.getIn().getBody()).get("data"))
                .add(newExchange.getIn().getBody(String.class));
            return newExchange;
        }
        Map<String, Object> profile = oldExchange.getIn().getBody(Map.class);
        ((List) profile.get("data")).add(newExchange.getIn().getBody(String.class));
        return oldExchange;
    }
}
```

---

## Project 5: Notification Fan-Out

**Scenario**: Send notifications via multiple channels based on user preferences.

```java
@Component
public class NotificationRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("kafka:notification-requests?groupId=notifier")
            .routeId("notification-dispatcher")
            .unmarshal().json(JsonLibrary.Jackson, NotificationRequest.class)
            .bean("userPreferenceService", "getChannels")  // Returns List<String> channels
            .recipientList(simple("direct:${body.channels}"))
                .delimiter(",")
                .parallelProcessing()
                .stopOnException(false);  // Don't stop if one channel fails

        // Email channel
        from("direct:email")
            .routeId("email-sender")
            .bean("templateEngine", "renderEmail")
            .to("smtp://{{smtp.host}}:{{smtp.port}}"
                + "?username={{smtp.user}}&password=RAW({{smtp.password}})"
                + "&to=${header.recipientEmail}"
                + "&subject=${header.subject}")
            .to("kafka:notifications-sent");

        // SMS channel
        from("direct:sms")
            .routeId("sms-sender")
            .bean("templateEngine", "renderSms")
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .to("http://twilio-proxy/api/sms")
            .to("kafka:notifications-sent");

        // Push notification channel
        from("direct:push")
            .routeId("push-sender")
            .bean("templateEngine", "renderPush")
            .to("http://fcm-proxy/api/send")
            .to("kafka:notifications-sent");

        // Slack channel
        from("direct:slack")
            .routeId("slack-sender")
            .bean("templateEngine", "renderSlack")
            .to("https://hooks.slack.com/services/{{slack.webhook}}");
    }
}
```

---

## Project 6: Scheduled Report Generator

```java
@Component
public class ReportRoute extends RouteBuilder {

    @Override
    public void configure() {
        // Daily report at 8 AM
        from("cron:daily-report?schedule=0+0+8+*+*+?")
            .routeId("daily-report-generator")
            .log("Generating daily report...")
            .to("sql:SELECT * FROM orders WHERE DATE(created_at) = CURRENT_DATE - 1")
            .bean("reportBuilder", "buildDailySummary")
            .multicast()
                .to("direct:save-report-s3", "direct:email-report", "direct:slack-report")
            .end();

        from("direct:save-report-s3")
            .setHeader(AWS2S3Constants.KEY, 
                simple("reports/${date:now:yyyy/MM/dd}/daily-report.json"))
            .marshal().json()
            .to("aws2-s3://{{s3.reports-bucket}}?region={{aws.region}}");

        from("direct:email-report")
            .bean("reportBuilder", "formatEmail")
            .to("smtp://{{smtp.host}}?to={{report.recipients}}&subject=Daily Report ${date:now:yyyy-MM-dd}");

        from("direct:slack-report")
            .bean("reportBuilder", "formatSlack")
            .to("https://hooks.slack.com/services/{{slack.reports-webhook}}");
    }
}
```

---

## Summary: When to Use Camel

| Use Case | Camel Fit | Alternative |
|----------|-----------|-------------|
| System integration (REST↔Kafka↔DB) | ✅ Perfect | Custom code |
| ETL/Data pipelines | ✅ Great | Apache NiFi, Flink |
| Microservice orchestration | ✅ Good | Spring Cloud, Temporal |
| Simple REST API | ❌ Overkill | Spring MVC |
| Real-time stream processing | ⚠️ Okay (use Flink for complex) | Kafka Streams, Flink |
| File ingestion/processing | ✅ Perfect | Custom code |
| Legacy system integration | ✅ Perfect (SOAP, FTP, JMS) | MuleSoft |
| Event-driven architecture | ✅ Great | Spring Cloud Stream |
| API gateway (simple) | ✅ Good | Spring Cloud Gateway |
| Scheduled jobs | ✅ Good | Spring Scheduler, Quartz |
