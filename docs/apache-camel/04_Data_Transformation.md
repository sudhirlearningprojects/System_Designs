# Module 4: Data Transformation

## 1. Marshal & Unmarshal

**Marshal** = Object → Wire format (serialize)  
**Unmarshal** = Wire format → Object (deserialize)

```java
// JSON
from("kafka:orders")
    .unmarshal().json(JsonLibrary.Jackson, Order.class)  // JSON string → Order POJO
    .bean("processor")
    .marshal().json(JsonLibrary.Jackson)                  // Order POJO → JSON string
    .to("kafka:processed-orders");

// XML
from("file:/data/xml")
    .unmarshal().jaxb("com.example.model")               // XML → JAXB object
    .bean("transformer")
    .marshal().jaxb("com.example.model")                 // JAXB object → XML
    .to("file:/data/output");

// CSV
from("file:/data/csv?include=.*\\.csv")
    .unmarshal().csv()                                    // CSV → List<List<String>>
    .split(body())
        .bean("csvProcessor")
    .end();

// Avro (with schema registry)
from("kafka:avro-topic")
    .unmarshal().avro(AvroLibrary.ApacheAvro, Order.getClassSchema())
    .to("direct:process");

// Protobuf
from("kafka:proto-topic")
    .unmarshal().protobuf(OrderProto.Order.class)
    .to("direct:process");
```

---

## 2. JSON Processing

### Jackson (Default)

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-jackson-starter</artifactId>
</dependency>
```

```java
// Unmarshal to specific class
from("kafka:orders")
    .unmarshal().json(JsonLibrary.Jackson, Order.class)
    .log("Order ID: ${body.orderId}");

// Unmarshal to Map (when class unknown)
from("kafka:events")
    .unmarshal().json(JsonLibrary.Jackson, Map.class)
    .log("Event type: ${body[eventType]}");

// Unmarshal to List
from("kafka:bulk")
    .unmarshal().json(JsonLibrary.Jackson, List.class)
    .split(body())
        .to("direct:process-one");

// Custom ObjectMapper
@Bean("customMapper")
public ObjectMapper customMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
}

from("kafka:orders")
    .unmarshal().json(JsonLibrary.Jackson, Order.class, "customMapper");
```

### JsonPath (Extract fields without POJO)

```java
from("kafka:events")
    .setHeader("eventType", jsonpath("$.eventType"))
    .setHeader("userId", jsonpath("$.data.userId"))
    .filter(jsonpath("$.data.amount > 1000"))
    .to("direct:high-value");

// Transform using JsonPath
from("kafka:raw-data")
    .transform(jsonpath("$.results[*].name"))  // Extract array of names
    .to("direct:process-names");
```

---

## 3. XML Processing

### JAXB

```java
@XmlRootElement(name = "order")
@XmlAccessorType(XmlAccessType.FIELD)
public class Order {
    @XmlElement private String orderId;
    @XmlElement private BigDecimal amount;
    @XmlElement private String customer;
}

from("file:/data/xml")
    .unmarshal().jaxb("com.example.model")
    .bean("orderProcessor");
```

### XPath

```java
from("file:/data/xml")
    .filter(xpath("/order/amount > 500"))
    .setHeader("customer", xpath("/order/customer/text()"))
    .split(xpath("/orders/order"))
        .to("direct:process-order")
    .end();
```

### XSLT Transformation

```java
from("file:/data/input-xml")
    .to("xslt:transform.xsl")  // Apply XSLT stylesheet from classpath
    .to("file:/data/output-xml");
```

---

## 4. CSV Processing

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-csv-starter</artifactId>
</dependency>
```

```java
// CSV with headers
from("file:/data/csv")
    .unmarshal(new CsvDataFormat()
        .setUseMaps(true)           // Each row = Map<String, String>
        .setHeader(new String[]{"id", "name", "email", "amount"})
        .setSkipHeaderRecord(true))
    .split(body())
        .log("Row: ${body[name]} - ${body[amount]}")
        .to("direct:process-row")
    .end();

// CSV to POJO (using header mapping)
from("file:/data/csv")
    .unmarshal(new CsvDataFormat()
        .setRecordConverter(new CsvRecordConverter<Order>() {
            public Order convertRecord(CSVRecord record) {
                return new Order(record.get("id"), record.get("name"), 
                    new BigDecimal(record.get("amount")));
            }
        }))
    .split(body())
        .to("direct:process-order");

// Write CSV
from("direct:generate-report")
    .marshal(new CsvDataFormat()
        .setHeader(new String[]{"id", "name", "total"}))
    .to("file:/data/reports?fileName=report-${date:now:yyyyMMdd}.csv");
```

---

## 5. Type Converters

Camel auto-converts types using a built-in type converter registry.

```java
// Automatic conversions
from("file:/data/input")
    .convertBodyTo(String.class)        // InputStream → String
    .convertBodyTo(byte[].class)        // String → byte[]
    .convertBodyTo(Integer.class)       // String → Integer
    .convertBodyTo(InputStream.class);  // byte[] → InputStream

// Custom type converter
@Converter(generateBulkLoader = true)
public class OrderConverter {

    @Converter
    public static Order toOrder(Map<String, Object> map) {
        Order order = new Order();
        order.setId((String) map.get("id"));
        order.setAmount(new BigDecimal(map.get("amount").toString()));
        return order;
    }

    @Converter
    public static Map<String, Object> toMap(Order order) {
        return Map.of(
            "id", order.getId(),
            "amount", order.getAmount().toString()
        );
    }
}
```

Register in `META-INF/services/org/apache/camel/TypeConverterLoader`:
```
com.example.OrderConverterBulkConverterLoader
```

---

## 6. Data Format with Schema Registry (Avro + Kafka)

```java
// Using Confluent Schema Registry
from("kafka:avro-orders"
        + "?brokers=localhost:9092"
        + "&valueDeserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer"
        + "&additionalProperties.schema.registry.url=http://localhost:8081"
        + "&additionalProperties.specific.avro.reader=true")
    .log("Avro order: ${body}")
    .to("direct:process");
```

---

## 7. Content-Type Based Transformation

```java
from("platform-http:/api/transform")
    .choice()
        .when(header(Exchange.CONTENT_TYPE).isEqualTo("application/json"))
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
        .when(header(Exchange.CONTENT_TYPE).isEqualTo("application/xml"))
            .unmarshal().jaxb("com.example.model")
        .when(header(Exchange.CONTENT_TYPE).isEqualTo("text/csv"))
            .unmarshal().csv()
    .end()
    .bean("orderProcessor")
    .marshal().json()
    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"));
```

---

## 8. Data Transformation with Processors & Beans

### Using Processor

```java
from("kafka:orders")
    .unmarshal().json(JsonLibrary.Jackson, Map.class)
    .process(exchange -> {
        Map<String, Object> input = exchange.getIn().getBody(Map.class);
        
        // Transform
        OrderDTO output = new OrderDTO();
        output.setId((String) input.get("order_id"));
        output.setTotal(new BigDecimal(input.get("amount").toString()));
        output.setCreatedAt(Instant.parse((String) input.get("timestamp")));
        
        exchange.getIn().setBody(output);
    })
    .marshal().json()
    .to("kafka:transformed-orders");
```

### Using Bean with Annotations

```java
@Component("orderTransformer")
public class OrderTransformer {
    
    public OrderDTO transform(@Body RawOrder raw, @Header("source") String source) {
        return OrderDTO.builder()
            .id(raw.getOrderId())
            .total(raw.getItems().stream()
                .map(Item::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .source(source)
            .processedAt(Instant.now())
            .build();
    }
}

from("kafka:orders")
    .unmarshal().json(JsonLibrary.Jackson, RawOrder.class)
    .bean("orderTransformer", "transform")
    .marshal().json()
    .to("kafka:processed-orders");
```

---

## 9. Templating (Velocity, Freemarker, Mustache)

```java
// Generate email body from template
from("direct:send-email")
    .to("velocity:templates/order-confirmation.vm")  // Classpath template
    .to("smtp://mail-server?to=${header.customerEmail}");

// templates/order-confirmation.vm
// Hello ${body.customerName},
// Your order ${body.orderId} for $${body.amount} has been confirmed.
// Expected delivery: ${body.deliveryDate}
```

---

## 10. DataSonnet / JSONata (Complex JSON transformations)

For complex JSON-to-JSON transformations without writing Java:

```java
// Using DataSonnet
from("kafka:input")
    .to("datasonnet:transform.ds?inputMimeType=application/json&outputMimeType=application/json")
    .to("kafka:output");
```

```jsonata
// transform.ds (DataSonnet file)
{
    "orderId": payload.order_id,
    "customerName": payload.customer.first_name & " " & payload.customer.last_name,
    "totalAmount": $sum(payload.items.price),
    "itemCount": $count(payload.items),
    "isHighValue": $sum(payload.items.price) > 1000
}
```
