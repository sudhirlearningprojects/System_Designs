# WebClient - Reactive HTTP Client

## Overview

WebClient is Spring's reactive, non-blocking HTTP client that replaces RestTemplate. It supports both synchronous and asynchronous operations.

## Basic Setup

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

## Basic Operations

### GET Request

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final WebClient webClient;
    
    // Single object
    public Mono<User> getUserById(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
    
    // Multiple objects
    public Flux<User> getAllUsers() {
        return webClient.get()
            .uri("/users")
            .retrieve()
            .bodyToFlux(User.class);
    }
}
```

### POST Request

```java
public Mono<User> createUser(User user) {
    return webClient.post()
        .uri("/users")
        .bodyValue(user)
        .retrieve()
        .bodyToMono(User.class);
}

// With Mono body
public Mono<User> createUserAsync(Mono<User> userMono) {
    return webClient.post()
        .uri("/users")
        .body(userMono, User.class)
        .retrieve()
        .bodyToMono(User.class);
}
```

### PUT Request

```java
public Mono<User> updateUser(Long id, User user) {
    return webClient.put()
        .uri("/users/{id}", id)
        .bodyValue(user)
        .retrieve()
        .bodyToMono(User.class);
}
```

### DELETE Request

```java
public Mono<Void> deleteUser(Long id) {
    return webClient.delete()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(Void.class);
}
```

## URI Building

### Path Variables

```java
public Mono<Order> getOrder(Long userId, Long orderId) {
    return webClient.get()
        .uri("/users/{userId}/orders/{orderId}", userId, orderId)
        .retrieve()
        .bodyToMono(Order.class);
}
```

### Query Parameters

```java
public Flux<Product> searchProducts(String keyword, int page, int size) {
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/products/search")
            .queryParam("keyword", keyword)
            .queryParam("page", page)
            .queryParam("size", size)
            .build())
        .retrieve()
        .bodyToFlux(Product.class);
}
```

### Complex URI

```java
public Flux<Product> filterProducts(ProductFilter filter) {
    return webClient.get()
        .uri(uriBuilder -> {
            UriBuilder builder = uriBuilder.path("/products");
            if (filter.getCategory() != null) {
                builder.queryParam("category", filter.getCategory());
            }
            if (filter.getMinPrice() != null) {
                builder.queryParam("minPrice", filter.getMinPrice());
            }
            if (filter.getMaxPrice() != null) {
                builder.queryParam("maxPrice", filter.getMaxPrice());
            }
            return builder.build();
        })
        .retrieve()
        .bodyToFlux(Product.class);
}
```

## Headers

### Static Headers

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .header("X-API-Key", "my-api-key")
        .header("X-Request-Id", UUID.randomUUID().toString())
        .retrieve()
        .bodyToMono(User.class);
}
```

### Dynamic Headers

```java
public Mono<User> getAuthenticatedUser(String token) {
    return webClient.get()
        .uri("/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .bodyToMono(User.class);
}
```

### Headers from Function

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .headers(headers -> {
            headers.setBearerAuth(getToken());
            headers.set("X-Request-Id", UUID.randomUUID().toString());
        })
        .retrieve()
        .bodyToMono(User.class);
}
```

## Response Handling

### Status Code Handling

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .onStatus(HttpStatus::is4xxClientError, response ->
            response.bodyToMono(String.class)
                .flatMap(body -> Mono.error(new ClientException(body))))
        .onStatus(HttpStatus::is5xxServerError, response ->
            Mono.error(new ServerException("Server error")))
        .bodyToMono(User.class);
}
```

### Response Entity

```java
public Mono<ResponseEntity<User>> getUserWithHeaders(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .toEntity(User.class);
}

// Access headers
public Mono<User> getUserAndLogHeaders(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .toEntity(User.class)
        .doOnNext(response -> {
            log.info("Status: {}", response.getStatusCode());
            log.info("Headers: {}", response.getHeaders());
        })
        .map(ResponseEntity::getBody);
}
```

### Exchange for Full Control

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .exchangeToMono(response -> {
            if (response.statusCode().equals(HttpStatus.OK)) {
                return response.bodyToMono(User.class);
            } else if (response.statusCode().is4xxClientError()) {
                return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new ClientException(body)));
            } else {
                return Mono.error(new ServerException("Server error"));
            }
        });
}
```

## Error Handling

### Basic Error Handling

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .onErrorResume(WebClientResponseException.NotFound.class, e ->
            Mono.error(new UserNotFoundException(id)))
        .onErrorResume(WebClientException.class, e ->
            Mono.error(new ServiceUnavailableException("Service unavailable")));
}
```

### Custom Error Decoder

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .onStatus(HttpStatus::isError, response ->
            response.bodyToMono(ErrorResponse.class)
                .flatMap(error -> Mono.error(new ApiException(error))))
        .bodyToMono(User.class);
}
```

## Timeout Configuration

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(5))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}

// Per-request timeout
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .timeout(Duration.ofSeconds(3));
}
```

## Retry Logic

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
            .filter(throwable -> throwable instanceof WebClientException)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                new ServiceUnavailableException("Max retries exceeded")));
}
```

## Circuit Breaker

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<User> getUser(Long id) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(CallNotPermittedException.class, e ->
                Mono.error(new ServiceUnavailableException("Circuit breaker open")));
    }
}
```

## Authentication

### Basic Auth

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .filter(ExchangeFilterFunctions.basicAuthentication("username", "password"))
        .build();
}
```

### Bearer Token

```java
public Mono<User> getUser(Long id, String token) {
    return webClient.get()
        .uri("/users/{id}", id)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .bodyToMono(User.class);
}
```

### OAuth2

```java
@Bean
public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth2.setDefaultClientRegistrationId("my-client");
    
    return WebClient.builder()
        .filter(oauth2)
        .build();
}
```

## Request Filters

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .filter(loggingFilter())
        .filter(authenticationFilter())
        .build();
}

private ExchangeFilterFunction loggingFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        log.info("Request: {} {}", request.method(), request.url());
        return Mono.just(request);
    });
}

private ExchangeFilterFunction authenticationFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        ClientRequest filtered = ClientRequest.from(request)
            .header("X-API-Key", getApiKey())
            .build();
        return Mono.just(filtered);
    });
}
```

## Streaming Responses

### Server-Sent Events

```java
public Flux<Event> streamEvents() {
    return webClient.get()
        .uri("/events/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(Event.class);
}
```

### JSON Stream

```java
public Flux<Product> streamProducts() {
    return webClient.get()
        .uri("/products/stream")
        .accept(MediaType.APPLICATION_NDJSON)
        .retrieve()
        .bodyToFlux(Product.class);
}
```

## File Upload

```java
public Mono<String> uploadFile(File file) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", new FileSystemResource(file));
    
    return webClient.post()
        .uri("/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(builder.build())
        .retrieve()
        .bodyToMono(String.class);
}
```

## File Download

```java
public Mono<Void> downloadFile(Long id, Path destination) {
    return webClient.get()
        .uri("/files/{id}/download", id)
        .retrieve()
        .bodyToFlux(DataBuffer.class)
        .flatMap(dataBuffer -> {
            try {
                AsynchronousFileChannel channel = AsynchronousFileChannel.open(
                    destination,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
                );
                return DataBufferUtils.write(Flux.just(dataBuffer), channel)
                    .then(Mono.fromRunnable(() -> {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            log.error("Error closing file", e);
                        }
                    }));
            } catch (IOException e) {
                return Mono.error(e);
            }
        })
        .then();
}
```

## Connection Pool Configuration

```java
@Bean
public WebClient webClient() {
    ConnectionProvider provider = ConnectionProvider.builder("custom")
        .maxConnections(500)
        .maxIdleTime(Duration.ofSeconds(20))
        .maxLifeTime(Duration.ofSeconds(60))
        .pendingAcquireTimeout(Duration.ofSeconds(60))
        .evictInBackground(Duration.ofSeconds(120))
        .build();
    
    HttpClient httpClient = HttpClient.create(provider)
        .responseTimeout(Duration.ofSeconds(5));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Advanced Patterns

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
    
    Mono<Address> address = webClient.get()
        .uri("/users/{id}/address", userId)
        .retrieve()
        .bodyToMono(Address.class);
    
    return Mono.zip(user, orders, address)
        .map(tuple -> new UserDetails(tuple.getT1(), tuple.getT2(), tuple.getT3()));
}
```

### Sequential Requests

```java
public Mono<OrderDetails> createOrderWithPayment(Order order) {
    return webClient.post()
        .uri("/orders")
        .bodyValue(order)
        .retrieve()
        .bodyToMono(Order.class)
        .flatMap(createdOrder -> 
            webClient.post()
                .uri("/payments")
                .bodyValue(new Payment(createdOrder.getId(), order.getAmount()))
                .retrieve()
                .bodyToMono(Payment.class)
                .map(payment -> new OrderDetails(createdOrder, payment))
        );
}
```

### Conditional Requests

```java
public Mono<Product> getProductWithReviews(Long id, boolean includeReviews) {
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

## Testing WebClient

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
        .expectNextMatches(user -> user.getId() == 1 && user.getName().equals("John"))
        .verifyComplete();
    
    mockServer.shutdown();
}
```

## Best Practices

1. **Reuse WebClient instances**: Create once, use many times
2. **Configure timeouts**: Always set reasonable timeouts
3. **Handle errors properly**: Use onStatus() or onErrorResume()
4. **Use connection pooling**: Configure for your load
5. **Implement retry logic**: With exponential backoff
6. **Add circuit breakers**: Prevent cascading failures
7. **Log requests/responses**: Use filters for debugging
8. **Test with mocks**: Use MockWebServer for testing

## Next Steps

- [Reactive Data Access](Reactive_Data_Access.md) - R2DBC and reactive repositories
- [Error Handling](Error_Handling.md) - Comprehensive error strategies
- [Testing](Testing.md) - Testing WebClient calls
