# Annotated Controllers in Spring WebFlux

## Overview

Annotated controllers use familiar Spring MVC annotations (@RestController, @GetMapping, etc.) but return reactive types (Mono, Flux).

## Basic Controller Structure

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    // Return Mono for single item
    @GetMapping("/{id}")
    public Mono<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id);
    }
    
    // Return Flux for multiple items
    @GetMapping
    public Flux<Product> getAllProducts() {
        return productService.findAll();
    }
}
```

## Request Mapping

### Path Variables

```java
@GetMapping("/users/{userId}/orders/{orderId}")
public Mono<Order> getOrder(
    @PathVariable Long userId,
    @PathVariable Long orderId
) {
    return orderService.findByUserAndId(userId, orderId);
}
```

### Query Parameters

```java
@GetMapping("/search")
public Flux<Product> searchProducts(
    @RequestParam String keyword,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    return productService.search(keyword, page, size);
}
```

### Request Body

```java
@PostMapping
public Mono<Product> createProduct(@Valid @RequestBody Product product) {
    return productService.save(product);
}

// Multiple items
@PostMapping("/batch")
public Flux<Product> createProducts(@RequestBody Flux<Product> products) {
    return productService.saveAll(products);
}
```

### Headers

```java
@GetMapping("/profile")
public Mono<User> getProfile(@RequestHeader("Authorization") String token) {
    return userService.findByToken(token);
}
```

## Response Handling

### Status Codes

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Mono<Product> createProduct(@RequestBody Product product) {
    return productService.save(product);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public Mono<Void> deleteProduct(@PathVariable Long id) {
    return productService.delete(id);
}
```

### ResponseEntity

```java
@GetMapping("/{id}")
public Mono<ResponseEntity<Product>> getProduct(@PathVariable Long id) {
    return productService.findById(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
}

@PostMapping
public Mono<ResponseEntity<Product>> createProduct(@RequestBody Product product) {
    return productService.save(product)
        .map(saved -> ResponseEntity
            .created(URI.create("/api/products/" + saved.getId()))
            .body(saved));
}
```

## Streaming Responses

### Server-Sent Events (SSE)

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<Product> streamProducts() {
    return productService.findAll()
        .delayElements(Duration.ofSeconds(1));
}

// With heartbeat
@GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<Notification>> streamNotifications() {
    return notificationService.getStream()
        .map(notification -> ServerSentEvent.<Notification>builder()
            .id(notification.getId())
            .event("notification")
            .data(notification)
            .build());
}
```

### JSON Stream

```java
@GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
public Flux<Product> streamProductsAsJson() {
    return productService.findAll();
}
```

## Request Body Handling

### Single Object

```java
@PostMapping
public Mono<Product> createProduct(@Valid @RequestBody Mono<Product> productMono) {
    return productMono.flatMap(productService::save);
}
```

### Stream of Objects

```java
@PostMapping("/batch")
public Flux<Product> createProducts(@RequestBody Flux<Product> products) {
    return products
        .flatMap(productService::save)
        .onErrorContinue((error, product) -> 
            log.error("Failed to save product: {}", product, error));
}
```

## Validation

```java
@PostMapping
public Mono<Product> createProduct(@Valid @RequestBody Product product) {
    return productService.save(product);
}

// Custom validation
@PostMapping("/custom")
public Mono<Product> createProductWithValidation(@RequestBody Product product) {
    return Mono.just(product)
        .flatMap(this::validateProduct)
        .flatMap(productService::save);
}

private Mono<Product> validateProduct(Product product) {
    if (product.getPrice() < 0) {
        return Mono.error(new ValidationException("Price must be positive"));
    }
    return Mono.just(product);
}
```

## Error Handling

### Controller-Level

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public Mono<Product> getProduct(@PathVariable Long id) {
        return productService.findById(id)
            .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }
    
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### Global Error Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ProductNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(ProductNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleValidation(
        WebExchangeBindException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        return Mono.just(ResponseEntity.badRequest().body(errors));
    }
}
```

## Advanced Patterns

### Pagination

```java
@GetMapping
public Mono<ResponseEntity<PageResponse<Product>>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size
) {
    return productService.findAll(page, size)
        .collectList()
        .zipWith(productService.count())
        .map(tuple -> {
            List<Product> products = tuple.getT1();
            Long total = tuple.getT2();
            PageResponse<Product> response = new PageResponse<>(
                products, page, size, total
            );
            return ResponseEntity.ok(response);
        });
}
```

### Filtering and Sorting

```java
@GetMapping("/filter")
public Flux<Product> filterProducts(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Double minPrice,
    @RequestParam(required = false) Double maxPrice,
    @RequestParam(defaultValue = "name") String sortBy
) {
    return productService.findAll()
        .filter(p -> category == null || p.getCategory().equals(category))
        .filter(p -> minPrice == null || p.getPrice() >= minPrice)
        .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
        .sort(Comparator.comparing(Product::getName));
}
```

### Combining Multiple Sources

```java
@GetMapping("/{id}/details")
public Mono<ProductDetails> getProductDetails(@PathVariable Long id) {
    Mono<Product> product = productService.findById(id);
    Mono<List<Review>> reviews = reviewService.findByProductId(id).collectList();
    Mono<Inventory> inventory = inventoryService.findByProductId(id);
    
    return Mono.zip(product, reviews, inventory)
        .map(tuple -> new ProductDetails(
            tuple.getT1(),
            tuple.getT2(),
            tuple.getT3()
        ));
}
```

### Timeout Handling

```java
@GetMapping("/{id}")
public Mono<Product> getProduct(@PathVariable Long id) {
    return productService.findById(id)
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(TimeoutException.class, e -> 
            Mono.error(new ServiceUnavailableException("Request timeout")));
}
```

### Retry Logic

```java
@GetMapping("/{id}")
public Mono<Product> getProduct(@PathVariable Long id) {
    return productService.findById(id)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof TransientException));
}
```

## Content Negotiation

```java
@GetMapping(value = "/{id}", produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE
})
public Mono<Product> getProduct(@PathVariable Long id) {
    return productService.findById(id);
}
```

## File Upload

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Mono<String> uploadFile(@RequestPart("file") FilePart filePart) {
    return filePart.transferTo(Path.of("/uploads/" + filePart.filename()))
        .then(Mono.just("File uploaded: " + filePart.filename()));
}

// Multiple files
@PostMapping("/upload-multiple")
public Flux<String> uploadFiles(@RequestPart("files") Flux<FilePart> files) {
    return files.flatMap(file -> 
        file.transferTo(Path.of("/uploads/" + file.filename()))
            .then(Mono.just(file.filename()))
    );
}
```

## File Download

```java
@GetMapping("/{id}/download")
public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable Long id) {
    return fileService.findById(id)
        .map(file -> ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + file.getName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new FileSystemResource(file.getPath())));
}
```

## Best Practices

1. **Always return Mono/Flux**: Never block in controllers
2. **Use switchIfEmpty()**: Handle empty results gracefully
3. **Proper error handling**: Use onErrorResume() or global handlers
4. **Validation**: Use @Valid for automatic validation
5. **Timeouts**: Set reasonable timeouts for external calls
6. **Backpressure**: Let Reactor handle it automatically
7. **Testing**: Use WebTestClient for integration tests

## Next Steps

- [Functional Endpoints](Functional_Endpoints.md) - Alternative programming model
- [Error Handling](Error_Handling.md) - Comprehensive error strategies
- [Testing](Testing.md) - Testing annotated controllers
