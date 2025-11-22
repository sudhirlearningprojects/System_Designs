# Getting Started with WebClient

## Setup

### Maven Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Optional: Netty for HTTP/2 -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
    </dependency>
</dependencies>
```

### Basic Configuration

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, "MyApp/1.0")
            .build();
    }
}
```

## Creating WebClient

### Method 1: Simple Creation

```java
WebClient webClient = WebClient.create();
```

### Method 2: With Base URL

```java
WebClient webClient = WebClient.create("https://api.example.com");
```

### Method 3: Using Builder

```java
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultCookie("session", "abc123")
    .build();
```

## Basic HTTP Operations

### GET Request

```java
// Simple GET
Mono<String> response = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(String.class);

// GET with path variable
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class);

// GET with query parameters
Mono<List<User>> users = webClient.get()
    .uri(uriBuilder -> uriBuilder
        .path("/users")
        .queryParam("page", 0)
        .queryParam("size", 10)
        .build())
    .retrieve()
    .bodyToFlux(User.class)
    .collectList();
```

### POST Request

```java
// POST with body
User newUser = new User("john", "john@example.com");

Mono<User> created = webClient.post()
    .uri("/users")
    .bodyValue(newUser)
    .retrieve()
    .bodyToMono(User.class);

// POST with Mono body
Mono<User> userMono = Mono.just(newUser);

Mono<User> created = webClient.post()
    .uri("/users")
    .body(userMono, User.class)
    .retrieve()
    .bodyToMono(User.class);
```

### PUT Request

```java
User updatedUser = new User("john_updated", "john.new@example.com");

Mono<User> updated = webClient.put()
    .uri("/users/{id}", 1)
    .bodyValue(updatedUser)
    .retrieve()
    .bodyToMono(User.class);
```

### DELETE Request

```java
Mono<Void> deleted = webClient.delete()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(Void.class);
```

### PATCH Request

```java
Map<String, Object> updates = Map.of("email", "new@example.com");

Mono<User> patched = webClient.patch()
    .uri("/users/{id}", 1)
    .bodyValue(updates)
    .retrieve()
    .bodyToMono(User.class);
```

## Working with Responses

### Extract Body

```java
// Single object
Mono<User> user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class);

// List of objects
Flux<User> users = webClient.get()
    .uri("/users")
    .retrieve()
    .bodyToFlux(User.class);

// String response
Mono<String> response = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(String.class);
```

### Extract Headers

```java
Mono<ResponseEntity<User>> response = webClient.get()
    .uri("/users/1")
    .retrieve()
    .toEntity(User.class);

response.subscribe(entity -> {
    HttpHeaders headers = entity.getHeaders();
    User user = entity.getBody();
    HttpStatus status = entity.getStatusCode();
});
```

## Consuming Responses

### Subscribe

```java
webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .subscribe(
        user -> System.out.println("User: " + user),
        error -> System.err.println("Error: " + error),
        () -> System.out.println("Completed")
    );
```

### Block (Synchronous)

```java
// Block and wait for result
User user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .block();

// Block with timeout
User user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .block(Duration.ofSeconds(5));
```

### Async Processing

```java
webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .doOnSuccess(user -> log.info("Received: {}", user))
    .doOnError(error -> log.error("Error: {}", error))
    .subscribe();
```

## Service Integration

### Basic Service

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final WebClient webClient;
    
    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    public Flux<User> getAllUsers() {
        return webClient.get()
            .uri("/users")
            .retrieve()
            .bodyToFlux(User.class);
    }
    
    public Mono<User> createUser(User user) {
        return webClient.post()
            .uri("/users")
            .bodyValue(user)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return webClient.put()
            .uri("/users/{id}", id)
            .bodyValue(user)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    public Mono<Void> deleteUser(Long id) {
        return webClient.delete()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(Void.class);
    }
}
```

### Controller Integration

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
    
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}
```

## Error Handling Basics

### Handle 4xx/5xx Errors

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .onStatus(HttpStatus::is4xxClientError, response ->
        Mono.error(new ClientException("Client error")))
    .onStatus(HttpStatus::is5xxServerError, response ->
        Mono.error(new ServerException("Server error")))
    .bodyToMono(User.class);
```

### Default Error Handling

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .onErrorResume(WebClientResponseException.class, ex -> {
        log.error("Error: {}", ex.getMessage());
        return Mono.empty();
    });
```

## Common Patterns

### Parallel Requests

```java
public Mono<UserDetails> getUserDetails(Long userId) {
    Mono<User> user = webClient.get()
        .uri("/users/{id}", userId)
        .retrieve()
        .bodyToMono(User.class);
    
    Mono<List<Order>> orders = webClient.get()
        .uri("/users/{id}/orders", userId)
        .retrieve()
        .bodyToFlux(Order.class)
        .collectList();
    
    return Mono.zip(user, orders)
        .map(tuple -> new UserDetails(tuple.getT1(), tuple.getT2()));
}
```

### Sequential Requests

```java
public Mono<OrderConfirmation> createOrder(Order order) {
    return webClient.post()
        .uri("/orders")
        .bodyValue(order)
        .retrieve()
        .bodyToMono(Order.class)
        .flatMap(createdOrder -> 
            webClient.post()
                .uri("/payments")
                .bodyValue(new Payment(createdOrder.getId()))
                .retrieve()
                .bodyToMono(Payment.class)
                .map(payment -> new OrderConfirmation(createdOrder, payment))
        );
}
```

### Conditional Requests

```java
public Mono<Product> getProduct(Long id, boolean includeReviews) {
    Mono<Product> product = webClient.get()
        .uri("/products/{id}", id)
        .retrieve()
        .bodyToMono(Product.class);
    
    if (includeReviews) {
        Mono<List<Review>> reviews = webClient.get()
            .uri("/products/{id}/reviews", id)
            .retrieve()
            .bodyToFlux(Review.class)
            .collectList();
        
        return Mono.zip(product, reviews)
            .map(tuple -> {
                Product p = tuple.getT1();
                p.setReviews(tuple.getT2());
                return p;
            });
    }
    
    return product;
}
```

## Testing

### Basic Test

```java
@Test
void testGetUser() {
    MockWebServer mockServer = new MockWebServer();
    mockServer.enqueue(new MockResponse()
        .setBody("{\"id\":1,\"name\":\"John\"}")
        .addHeader("Content-Type", "application/json"));
    
    WebClient webClient = WebClient.create(mockServer.url("/").toString());
    
    Mono<User> result = webClient.get()
        .uri("/users/1")
        .retrieve()
        .bodyToMono(User.class);
    
    StepVerifier.create(result)
        .expectNextMatches(user -> user.getId() == 1)
        .verifyComplete();
    
    mockServer.shutdown();
}
```

## Best Practices

1. **Reuse WebClient instances**: Create once, use many times
2. **Use base URL**: Configure base URL in builder
3. **Handle errors**: Always add error handling
4. **Set timeouts**: Prevent hanging requests
5. **Use reactive types**: Return Mono/Flux, don't block
6. **Configure connection pool**: Optimize for your load
7. **Add logging**: Use filters for debugging
8. **Test with mocks**: Use MockWebServer

## Common Mistakes

❌ **Blocking in reactive chain**
```java
// Bad
User user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class)
    .block(); // Blocks thread!
```

✅ **Return reactive type**
```java
// Good
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class);
}
```

❌ **Creating WebClient per request**
```java
// Bad
public Mono<User> getUser(Long id) {
    WebClient webClient = WebClient.create("https://api.example.com");
    return webClient.get()...
}
```

✅ **Reuse WebClient instance**
```java
// Good
@Service
public class UserService {
    private final WebClient webClient;
    
    public UserService(WebClient webClient) {
        this.webClient = webClient;
    }
}
```

## Next Steps

- [Configuration](Configuration.md) - Advanced configuration
- [Request Building](Request_Building.md) - Complex request patterns
- [Error Handling](Error_Handling.md) - Comprehensive error strategies
