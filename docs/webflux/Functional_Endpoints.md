# Functional Endpoints in Spring WebFlux

## Overview

Functional endpoints provide a lightweight, functional programming model as an alternative to annotated controllers. They use RouterFunction and HandlerFunction.

## Core Concepts

- **HandlerFunction**: Processes request and returns response
- **RouterFunction**: Routes requests to handlers
- **ServerRequest**: Represents HTTP request
- **ServerResponse**: Represents HTTP response

## Basic Example

```java
@Configuration
public class ProductRouter {
    
    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return RouterFunctions
            .route(GET("/api/products"), handler::getAllProducts)
            .andRoute(GET("/api/products/{id}"), handler::getProduct)
            .andRoute(POST("/api/products"), handler::createProduct)
            .andRoute(PUT("/api/products/{id}"), handler::updateProduct)
            .andRoute(DELETE("/api/products/{id}"), handler::deleteProduct);
    }
}

@Component
@RequiredArgsConstructor
public class ProductHandler {
    
    private final ProductService productService;
    
    public Mono<ServerResponse> getAllProducts(ServerRequest request) {
        return ServerResponse.ok()
            .body(productService.findAll(), Product.class);
    }
    
    public Mono<ServerResponse> getProduct(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return productService.findById(id)
            .flatMap(product -> ServerResponse.ok().bodyValue(product))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    public Mono<ServerResponse> createProduct(ServerRequest request) {
        return request.bodyToMono(Product.class)
            .flatMap(productService::save)
            .flatMap(product -> ServerResponse
                .created(URI.create("/api/products/" + product.getId()))
                .bodyValue(product));
    }
    
    public Mono<ServerResponse> updateProduct(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return request.bodyToMono(Product.class)
            .flatMap(product -> productService.update(id, product))
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    public Mono<ServerResponse> deleteProduct(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return productService.delete(id)
            .then(ServerResponse.noContent().build());
    }
}
```

## Router Configuration

### Nested Routes

```java
@Bean
public RouterFunction<ServerResponse> routes(ProductHandler handler, UserHandler userHandler) {
    return nest(path("/api"),
        nest(path("/products"),
            route(GET(""), handler::getAllProducts)
                .andRoute(GET("/{id}"), handler::getProduct)
                .andRoute(POST(""), handler::createProduct)
                .andRoute(PUT("/{id}"), handler::updateProduct)
                .andRoute(DELETE("/{id}"), handler::deleteProduct)
        ).andNest(path("/users"),
            route(GET(""), userHandler::getAllUsers)
                .andRoute(GET("/{id}"), userHandler::getUser)
                .andRoute(POST(""), userHandler::createUser)
        )
    );
}
```

### Request Predicates

```java
@Bean
public RouterFunction<ServerResponse> routes(ProductHandler handler) {
    return route()
        .GET("/api/products", accept(MediaType.APPLICATION_JSON), handler::getAllProducts)
        .GET("/api/products/{id}", handler::getProduct)
        .POST("/api/products", contentType(MediaType.APPLICATION_JSON), handler::createProduct)
        .PUT("/api/products/{id}", handler::updateProduct)
        .DELETE("/api/products/{id}", handler::deleteProduct)
        .build();
}
```

### Custom Predicates

```java
@Bean
public RouterFunction<ServerResponse> routes(ProductHandler handler) {
    RequestPredicate hasAuthHeader = request -> 
        request.headers().header("Authorization").size() > 0;
    
    RequestPredicate isAdmin = request ->
        request.headers().firstHeader("Role").equals("ADMIN");
    
    return route()
        .GET("/api/products", handler::getAllProducts)
        .GET("/api/products/{id}", handler::getProduct)
        .POST("/api/products", hasAuthHeader.and(isAdmin), handler::createProduct)
        .build();
}
```

## Handler Functions

### Query Parameters

```java
public Mono<ServerResponse> searchProducts(ServerRequest request) {
    String keyword = request.queryParam("keyword").orElse("");
    int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
    int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
    
    return ServerResponse.ok()
        .body(productService.search(keyword, page, size), Product.class);
}
```

### Headers

```java
public Mono<ServerResponse> getProfile(ServerRequest request) {
    String token = request.headers().firstHeader("Authorization");
    
    return userService.findByToken(token)
        .flatMap(user -> ServerResponse.ok().bodyValue(user))
        .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
}
```

### Request Body

```java
// Single object
public Mono<ServerResponse> createProduct(ServerRequest request) {
    return request.bodyToMono(Product.class)
        .flatMap(productService::save)
        .flatMap(product -> ServerResponse.ok().bodyValue(product));
}

// Stream of objects
public Mono<ServerResponse> createProducts(ServerRequest request) {
    Flux<Product> products = request.bodyToFlux(Product.class);
    return ServerResponse.ok()
        .body(productService.saveAll(products), Product.class);
}
```

### Path Variables

```java
public Mono<ServerResponse> getProduct(ServerRequest request) {
    Long id = Long.valueOf(request.pathVariable("id"));
    String category = request.pathVariable("category");
    
    return productService.findByCategoryAndId(category, id)
        .flatMap(product -> ServerResponse.ok().bodyValue(product))
        .switchIfEmpty(ServerResponse.notFound().build());
}
```

## Response Building

### JSON Response

```java
public Mono<ServerResponse> getProduct(ServerRequest request) {
    Long id = Long.valueOf(request.pathVariable("id"));
    return productService.findById(id)
        .flatMap(product -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(product))
        .switchIfEmpty(ServerResponse.notFound().build());
}
```

### Custom Headers

```java
public Mono<ServerResponse> createProduct(ServerRequest request) {
    return request.bodyToMono(Product.class)
        .flatMap(productService::save)
        .flatMap(product -> ServerResponse
            .created(URI.create("/api/products/" + product.getId()))
            .header("X-Product-Id", product.getId().toString())
            .bodyValue(product));
}
```

### Streaming Response (SSE)

```java
public Mono<ServerResponse> streamProducts(ServerRequest request) {
    Flux<Product> products = productService.findAll()
        .delayElements(Duration.ofSeconds(1));
    
    return ServerResponse.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(products, Product.class);
}

// With ServerSentEvent
public Mono<ServerResponse> streamNotifications(ServerRequest request) {
    Flux<ServerSentEvent<Notification>> events = notificationService.getStream()
        .map(notification -> ServerSentEvent.<Notification>builder()
            .id(notification.getId())
            .event("notification")
            .data(notification)
            .build());
    
    return ServerResponse.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(events, ServerSentEvent.class);
}
```

### File Download

```java
public Mono<ServerResponse> downloadFile(ServerRequest request) {
    Long id = Long.valueOf(request.pathVariable("id"));
    
    return fileService.findById(id)
        .flatMap(file -> ServerResponse.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + file.getName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(new FileSystemResource(file.getPath())));
}
```

## Error Handling

### Handler-Level

```java
public Mono<ServerResponse> getProduct(ServerRequest request) {
    Long id = Long.valueOf(request.pathVariable("id"));
    
    return productService.findById(id)
        .flatMap(product -> ServerResponse.ok().bodyValue(product))
        .switchIfEmpty(ServerResponse.notFound().build())
        .onErrorResume(IllegalArgumentException.class, e ->
            ServerResponse.badRequest().bodyValue(e.getMessage()))
        .onErrorResume(e ->
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue("Internal server error"));
}
```

### Global Error Handler

```java
@Component
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    
    public GlobalErrorWebExceptionHandler(
        ErrorAttributes errorAttributes,
        ResourceProperties resourceProperties,
        ApplicationContext applicationContext,
        ServerCodecConfigurer configurer
    ) {
        super(errorAttributes, resourceProperties, applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }
    
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }
    
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        
        if (error instanceof ProductNotFoundException) {
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                .bodyValue(Map.of("error", error.getMessage()));
        }
        
        if (error instanceof ValidationException) {
            return ServerResponse.badRequest()
                .bodyValue(Map.of("error", error.getMessage()));
        }
        
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .bodyValue(Map.of("error", "Internal server error"));
    }
}
```

## Validation

```java
@Component
@RequiredArgsConstructor
public class ProductHandler {
    
    private final ProductService productService;
    private final Validator validator;
    
    public Mono<ServerResponse> createProduct(ServerRequest request) {
        return request.bodyToMono(Product.class)
            .flatMap(this::validate)
            .flatMap(productService::save)
            .flatMap(product -> ServerResponse.ok().bodyValue(product))
            .onErrorResume(ValidationException.class, e ->
                ServerResponse.badRequest().bodyValue(e.getErrors()));
    }
    
    private Mono<Product> validate(Product product) {
        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        if (!violations.isEmpty()) {
            Map<String, String> errors = violations.stream()
                .collect(Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    ConstraintViolation::getMessage
                ));
            return Mono.error(new ValidationException(errors));
        }
        return Mono.just(product);
    }
}
```

## Filters

### Router Filter

```java
@Bean
public RouterFunction<ServerResponse> routes(ProductHandler handler) {
    return route()
        .GET("/api/products", handler::getAllProducts)
        .GET("/api/products/{id}", handler::getProduct)
        .filter(loggingFilter())
        .filter(authenticationFilter())
        .build();
}

private HandlerFilterFunction<ServerResponse, ServerResponse> loggingFilter() {
    return (request, next) -> {
        log.info("Request: {} {}", request.method(), request.path());
        return next.handle(request)
            .doOnSuccess(response -> 
                log.info("Response: {}", response.statusCode()));
    };
}

private HandlerFilterFunction<ServerResponse, ServerResponse> authenticationFilter() {
    return (request, next) -> {
        String token = request.headers().firstHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
        return next.handle(request);
    };
}
```

## Advanced Patterns

### Combining Multiple Sources

```java
public Mono<ServerResponse> getProductDetails(ServerRequest request) {
    Long id = Long.valueOf(request.pathVariable("id"));
    
    Mono<Product> product = productService.findById(id);
    Mono<List<Review>> reviews = reviewService.findByProductId(id).collectList();
    Mono<Inventory> inventory = inventoryService.findByProductId(id);
    
    return Mono.zip(product, reviews, inventory)
        .map(tuple -> new ProductDetails(tuple.getT1(), tuple.getT2(), tuple.getT3()))
        .flatMap(details -> ServerResponse.ok().bodyValue(details))
        .switchIfEmpty(ServerResponse.notFound().build());
}
```

### Pagination

```java
public Mono<ServerResponse> getProducts(ServerRequest request) {
    int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
    int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
    
    return productService.findAll(page, size)
        .collectList()
        .zipWith(productService.count())
        .flatMap(tuple -> {
            List<Product> products = tuple.getT1();
            Long total = tuple.getT2();
            PageResponse<Product> response = new PageResponse<>(products, page, size, total);
            return ServerResponse.ok().bodyValue(response);
        });
}
```

### File Upload

```java
public Mono<ServerResponse> uploadFile(ServerRequest request) {
    return request.multipartData()
        .flatMap(parts -> {
            Part filePart = parts.getFirst("file");
            if (filePart instanceof FilePart) {
                FilePart file = (FilePart) filePart;
                return file.transferTo(Path.of("/uploads/" + file.filename()))
                    .then(ServerResponse.ok().bodyValue("File uploaded: " + file.filename()));
            }
            return ServerResponse.badRequest().bodyValue("No file provided");
        });
}
```

## Comparison: Functional vs Annotated

| Aspect | Functional | Annotated |
|--------|-----------|-----------|
| Style | Functional programming | Declarative annotations |
| Verbosity | More verbose | More concise |
| Type Safety | Compile-time routing | Runtime routing |
| Flexibility | More flexible | Less flexible |
| Testing | Easier to unit test | Requires integration tests |
| Learning Curve | Steeper | Gentler |

## When to Use Functional Endpoints

✅ **Use Functional when:**
- Need fine-grained control over routing
- Building microservices with minimal overhead
- Prefer functional programming style
- Need compile-time route validation
- Building API gateways or proxies

❌ **Use Annotated when:**
- Team familiar with Spring MVC
- Building traditional REST APIs
- Need rapid development
- Prefer declarative style

## Best Practices

1. **Separate routing from handling**: Keep RouterFunction and HandlerFunction separate
2. **Use nested routes**: Organize routes hierarchically
3. **Validate input**: Always validate request bodies
4. **Handle errors gracefully**: Use onErrorResume() or global handlers
5. **Use filters**: Apply cross-cutting concerns with filters
6. **Keep handlers focused**: One handler per operation
7. **Test thoroughly**: Unit test handlers, integration test routes

## Next Steps

- [WebClient](WebClient.md) - Making reactive HTTP calls
- [Error Handling](Error_Handling.md) - Comprehensive error strategies
- [Testing](Testing.md) - Testing functional endpoints
