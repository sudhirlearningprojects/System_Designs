# Module 9: REST DSL & OpenAPI

## 1. REST DSL Basics

Camel's REST DSL lets you define REST APIs declaratively, separate from the implementation.

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-platform-http-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-jackson-starter</artifactId>
</dependency>
```

```java
@Component
public class OrderRestRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // REST configuration
        restConfiguration()
            .component("platform-http")        // Use Spring Boot's embedded server
            .bindingMode(RestBindingMode.json)  // Auto JSON marshal/unmarshal
            .dataFormatProperty("prettyPrint", "true")
            .apiContextPath("/api-doc")        // OpenAPI spec URL
            .apiProperty("api.title", "Order Service API")
            .apiProperty("api.version", "1.0.0")
            .enableCORS(true);

        // REST endpoint definitions
        rest("/api/v1/orders")
            .description("Order Management API")

            .get()
                .description("List all orders")
                .outType(Order[].class)
                .to("direct:list-orders")

            .get("/{orderId}")
                .description("Get order by ID")
                .param().name("orderId").type(RestParamType.path).required(true).endParam()
                .outType(Order.class)
                .to("direct:get-order")

            .post()
                .description("Create new order")
                .type(CreateOrderRequest.class)
                .outType(Order.class)
                .to("direct:create-order")

            .put("/{orderId}")
                .description("Update order")
                .param().name("orderId").type(RestParamType.path).required(true).endParam()
                .type(UpdateOrderRequest.class)
                .outType(Order.class)
                .to("direct:update-order")

            .delete("/{orderId}")
                .description("Delete order")
                .param().name("orderId").type(RestParamType.path).required(true).endParam()
                .to("direct:delete-order");

        // Implementation routes
        from("direct:list-orders")
            .bean("orderService", "findAll");

        from("direct:get-order")
            .bean("orderService", "findById(${header.orderId})");

        from("direct:create-order")
            .bean("orderValidator", "validate")
            .bean("orderService", "create")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));

        from("direct:update-order")
            .bean("orderService", "update(${header.orderId}, ${body})");

        from("direct:delete-order")
            .bean("orderService", "delete(${header.orderId})")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(204))
            .setBody(constant(null));
    }
}
```

---

## 2. Request Validation

```java
// Using Bean Validation (Jakarta)
public class CreateOrderRequest {
    @NotNull @Size(min = 1, max = 100)
    private String customerName;
    
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    
    @NotEmpty
    private List<OrderItem> items;
}

// Enable validation in REST config
restConfiguration()
    .component("platform-http")
    .bindingMode(RestBindingMode.json)
    .clientRequestValidation(true);  // Auto-validate request bodies

// Custom validation in route
from("direct:create-order")
    .process(exchange -> {
        CreateOrderRequest request = exchange.getIn().getBody(CreateOrderRequest.class);
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            throw new ValidationException(errors);
        }
    })
    .bean("orderService", "create");
```

---

## 3. Error Responses

```java
@Override
public void configure() {
    // Map exceptions to HTTP status codes
    onException(OrderNotFoundException.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(simple("{\"error\":\"Order not found\",\"id\":\"${header.orderId}\"}"));

    onException(ValidationException.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .setBody(simple("{\"error\":\"Validation failed\",\"details\":\"${exception.message}\"}"));

    onException(Exception.class)
        .handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setBody(constant("{\"error\":\"Internal server error\"}"))
        .log(LoggingLevel.ERROR, "${exception.stacktrace}");
}
```

---

## 4. OpenAPI Specification

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-openapi-java-starter</artifactId>
</dependency>
```

```java
restConfiguration()
    .apiContextPath("/api-doc")
    .apiProperty("api.title", "Order Service")
    .apiProperty("api.version", "2.0.0")
    .apiProperty("api.description", "Order management microservice")
    .apiProperty("api.contact.name", "Platform Team")
    .apiProperty("api.license.name", "Apache 2.0")
    .apiProperty("cors", "true");
```

Access the spec at:
- `GET /api-doc` → OpenAPI JSON
- `GET /api-doc/swagger-ui` → Swagger UI (if configured)

---

## 5. Query Parameters & Headers

```java
rest("/api/v1/orders")
    .get()
        .param().name("status").type(RestParamType.query)
            .description("Filter by status").required(false).endParam()
        .param().name("page").type(RestParamType.query)
            .defaultValue("0").dataType("integer").endParam()
        .param().name("size").type(RestParamType.query)
            .defaultValue("20").dataType("integer").endParam()
        .param().name("Authorization").type(RestParamType.header)
            .required(true).endParam()
        .to("direct:list-orders");

from("direct:list-orders")
    .process(exchange -> {
        String status = exchange.getIn().getHeader("status", String.class);
        int page = exchange.getIn().getHeader("page", 0, Integer.class);
        int size = exchange.getIn().getHeader("size", 20, Integer.class);
        String auth = exchange.getIn().getHeader("Authorization", String.class);
        // Use parameters...
    })
    .bean("orderService", "findFiltered");
```

---

## 6. File Upload & Download

```java
rest("/api/v1/files")
    .post("/upload")
        .consumes("multipart/form-data")
        .bindingMode(RestBindingMode.off)   // Don't try to JSON parse file
        .to("direct:handle-upload")

    .get("/download/{fileId}")
        .produces("application/octet-stream")
        .to("direct:handle-download");

from("direct:handle-upload")
    .process(exchange -> {
        AttachmentMessage am = exchange.getIn(AttachmentMessage.class);
        DataHandler dataHandler = am.getAttachment("file");
        InputStream is = dataHandler.getInputStream();
        String filename = dataHandler.getName();
        // Save file...
        exchange.getIn().setBody(Map.of("filename", filename, "status", "uploaded"));
    });

from("direct:handle-download")
    .bean("fileService", "getFile(${header.fileId})")
    .setHeader(Exchange.CONTENT_TYPE, constant("application/octet-stream"))
    .setHeader("Content-Disposition", simple("attachment; filename=${header.filename}"));
```

---

## 7. CORS Configuration

```java
restConfiguration()
    .enableCORS(true)
    .corsAllowCredentials(true)
    .corsHeaderProperty("Access-Control-Allow-Origin", "*")
    .corsHeaderProperty("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
    .corsHeaderProperty("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization")
    .corsHeaderProperty("Access-Control-Max-Age", "3600");
```

---

## 8. API Versioning

```java
// URL-based versioning
rest("/api/v1/orders")
    .get().to("direct:list-orders-v1");

rest("/api/v2/orders")
    .get().to("direct:list-orders-v2");

// Header-based versioning
rest("/api/orders")
    .get()
        .to("direct:route-by-version");

from("direct:route-by-version")
    .choice()
        .when(header("API-Version").isEqualTo("2"))
            .to("direct:list-orders-v2")
        .otherwise()
            .to("direct:list-orders-v1");
```

---

## 9. Rate Limiting on REST Endpoints

```java
rest("/api/v1/orders")
    .get()
        .to("direct:list-orders-throttled");

from("direct:list-orders-throttled")
    .throttle(100).timePeriodMillis(60000)    // 100 requests per minute
        .rejectExecution(true)                // Return 429 instead of queuing
    .bean("orderService", "findAll");

// Custom 429 response
onException(ThrottlerRejectedExecutionException.class)
    .handled(true)
    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(429))
    .setHeader("Retry-After", constant("60"))
    .setBody(constant("{\"error\":\"Rate limit exceeded. Try again in 60s.\"}"));
```

---

## 10. YAML REST DSL

```yaml
# rest-routes.yaml
- rest:
    path: /api/v1/products
    
    get:
      - path: /
        to: direct:list-products
      - path: /{id}
        to: direct:get-product
        
    post:
      - path: /
        type: com.example.CreateProductRequest
        to: direct:create-product

- route:
    id: list-products
    from:
      uri: direct:list-products
    steps:
      - bean:
          ref: productService
          method: findAll
          
- route:
    id: get-product
    from:
      uri: direct:get-product
    steps:
      - bean:
          ref: productService
          method: findById(${header.id})
```
