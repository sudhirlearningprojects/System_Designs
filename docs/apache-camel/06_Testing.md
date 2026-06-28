# Module 6: Testing Apache Camel Routes

## 1. Testing Dependencies

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-test-spring-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-test-junit5</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 2. Unit Testing with MockEndpoint

```java
@CamelSpringBootTest
@SpringBootTest
@MockEndpoints("kafka:*")  // Mock all Kafka endpoints
class OrderRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @EndpointInject("mock:kafka:processed-orders")
    private MockEndpoint processedOrders;

    @EndpointInject("mock:kafka:dead-letter-queue")
    private MockEndpoint deadLetterQueue;

    @Test
    void shouldProcessValidOrder() throws Exception {
        // Arrange
        processedOrders.expectedMessageCount(1);
        processedOrders.expectedBodiesReceived("{\"orderId\":\"123\",\"status\":\"PROCESSED\"}");

        // Act
        String orderJson = "{\"orderId\":\"123\",\"amount\":99.99,\"type\":\"DIGITAL\"}";
        producerTemplate.sendBody("direct:processOrder", orderJson);

        // Assert
        processedOrders.assertIsSatisfied();
    }

    @Test
    void shouldSendInvalidOrderToDLQ() throws Exception {
        // Arrange
        deadLetterQueue.expectedMessageCount(1);
        processedOrders.expectedMessageCount(0);

        // Act
        String invalidOrder = "{\"orderId\":null,\"amount\":-1}";
        producerTemplate.sendBody("direct:processOrder", invalidOrder);

        // Assert
        deadLetterQueue.assertIsSatisfied();
        processedOrders.assertIsSatisfied();
    }

    @Test
    void shouldRouteHighValueOrdersToPriorityQueue() throws Exception {
        MockEndpoint priorityQueue = camelContext.getEndpoint("mock:direct:priority", MockEndpoint.class);
        priorityQueue.expectedMessageCount(1);

        producerTemplate.sendBody("direct:router", 
            "{\"orderId\":\"456\",\"amount\":5000,\"type\":\"PHYSICAL\"}");

        priorityQueue.assertIsSatisfied();
    }
}
```

---

## 3. AdviceWith — Modify Routes for Testing

Replace real endpoints with mocks without changing route code.

```java
@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith  // Don't auto-start routes (we modify them first)
class PaymentRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate template;

    @Test
    void shouldProcessPaymentAndNotifySlack() throws Exception {
        // Replace real HTTP endpoint with mock
        AdviceWith.adviceWith(camelContext, "payment-route", route -> {
            // Intercept outgoing HTTP call to payment gateway
            route.interceptSendToEndpoint("http://payment-gateway.com/*")
                .skipSendToOriginalEndpoint()
                .setBody(constant("{\"status\":\"SUCCESS\",\"txnId\":\"TXN-001\"}"));

            // Mock the Slack notification
            route.mockEndpointsAndSkip("https://hooks.slack.com/*");

            // Add assertion endpoint at the end
            route.weaveAddLast().to("mock:result");
        });

        camelContext.start();

        MockEndpoint result = camelContext.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        template.sendBody("kafka:payments", "{\"amount\":100,\"currency\":\"USD\"}");

        result.assertIsSatisfied();
    }

    @Test
    void shouldHandlePaymentGatewayTimeout() throws Exception {
        AdviceWith.adviceWith(camelContext, "payment-route", route -> {
            // Simulate timeout
            route.interceptSendToEndpoint("http://payment-gateway.com/*")
                .skipSendToOriginalEndpoint()
                .throwException(new SocketTimeoutException("Connection timed out"));
        });

        camelContext.start();

        MockEndpoint dlq = camelContext.getEndpoint("mock:kafka:payment-dlq", MockEndpoint.class);
        dlq.expectedMessageCount(1);

        template.sendBody("kafka:payments", "{\"amount\":100}");

        dlq.assertIsSatisfied();
    }
}
```

---

## 4. Testing with Testcontainers (Integration Tests)

```java
@SpringBootTest
@Testcontainers
@DirtiesContext
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("camel.component.kafka.brokers", kafka::getBootstrapServers);
    }

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private ConsumerTemplate consumerTemplate;

    @Test
    void shouldRouteOrderFromKafkaToProcessedTopic() throws Exception {
        // Send to input topic
        producerTemplate.sendBody("kafka:raw-orders", "{\"orderId\":\"123\",\"amount\":50}");

        // Consume from output topic (with timeout)
        String result = consumerTemplate.receiveBody(
            "kafka:processed-orders?autoOffsetReset=earliest&groupId=test", 
            10000, String.class);

        assertThat(result).contains("\"orderId\":\"123\"");
        assertThat(result).contains("\"status\":\"PROCESSED\"");
    }
}
```

### Database Integration Test

```java
@SpringBootTest
@Testcontainers
class DatabaseRouteTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withInitScript("init-test.sql");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldSaveOrderToDatabase() {
        template.sendBody("direct:save-order", 
            new Order("ORD-1", BigDecimal.valueOf(99.99), "CREATED"));

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE id = 'ORD-1'", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
```

---

## 5. Testing Patterns

### Test Route with Split + Aggregation

```java
@Test
void shouldSplitAndAggregateCorrectly() throws Exception {
    MockEndpoint result = getMockEndpoint("mock:aggregated");
    result.expectedMessageCount(1);
    result.expectedBodiesReceived(List.of("A", "B", "C"));

    // Send a batch message that will be split
    template.sendBodyAndHeader("direct:batch-processor",
        List.of("A", "B", "C"), "batchId", "batch-001");

    result.assertIsSatisfied();
}
```

### Test Error Handling

```java
@Test
void shouldRetryAndEventuallySucceed() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    AdviceWith.adviceWith(camelContext, "retry-route", route -> {
        route.interceptSendToEndpoint("http://flaky-service/*")
            .skipSendToOriginalEndpoint()
            .process(exchange -> {
                if (callCount.incrementAndGet() < 3) {
                    throw new IOException("Service unavailable");
                }
                exchange.getIn().setBody("{\"status\":\"OK\"}");
            });
    });
    camelContext.start();

    MockEndpoint success = getMockEndpoint("mock:success");
    success.expectedMessageCount(1);

    template.sendBody("direct:call-flaky", "request");

    success.assertIsSatisfied();
    assertThat(callCount.get()).isEqualTo(3); // Failed twice, succeeded on third
}
```

### Test Idempotent Consumer

```java
@Test
void shouldRejectDuplicateMessages() throws Exception {
    MockEndpoint processed = getMockEndpoint("mock:processed");
    processed.expectedMessageCount(1); // Only 1, not 3

    // Send same message 3 times
    for (int i = 0; i < 3; i++) {
        template.sendBodyAndHeader("direct:idempotent-input", 
            "payment data", "paymentId", "PAY-001");
    }

    processed.assertIsSatisfied();
}
```

---

## 6. Testing Best Practices

| Practice | How |
|----------|-----|
| Isolate from external systems | Use `@MockEndpoints` or `AdviceWith` |
| Test error paths | Simulate exceptions with interceptors |
| Test time-dependent logic | Use `NotifyBuilder` with timeouts |
| Test route structure | `camelContext.getRouteDefinition("id")` |
| Avoid flaky tests | Use `assertIsSatisfied(timeout)` |
| Test in parallel | Use `@DirtiesContext` to reset context |
| Integration tests | Use Testcontainers for real Kafka/DB |

### NotifyBuilder (Wait for conditions)

```java
@Test
void shouldCompleteWithinTimeout() throws Exception {
    NotifyBuilder notify = new NotifyBuilder(camelContext)
        .from("kafka:orders")
        .whenCompleted(5)          // Wait for 5 exchanges to complete
        .create();

    // Send 5 messages
    for (int i = 0; i < 5; i++) {
        template.sendBody("kafka:orders", "{\"id\":" + i + "}");
    }

    boolean done = notify.matches(10, TimeUnit.SECONDS);
    assertTrue(done, "All 5 orders should be processed within 10s");
}
```
