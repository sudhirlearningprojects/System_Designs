# Spring WebClient - Reactive HTTP Client

## Overview

WebClient is Spring's modern, reactive, non-blocking HTTP client that replaces the legacy RestTemplate. Built on Project Reactor and Netty, it provides superior performance for I/O-bound operations.

## Why WebClient?

### WebClient vs RestTemplate

| Feature | RestTemplate | WebClient |
|---------|-------------|-----------|
| Blocking | Yes | No |
| Reactive | No | Yes |
| Async Support | Limited | Native |
| Backpressure | No | Yes |
| Performance | Lower | Higher |
| Thread Usage | High | Low |
| Streaming | No | Yes |
| Status | Maintenance Mode | Active Development |

### Key Benefits

- **Non-blocking I/O**: Handle thousands of concurrent connections with few threads
- **Reactive Streams**: Native support for Mono and Flux
- **Backpressure**: Automatic flow control
- **Functional API**: Fluent, composable interface
- **Resilience**: Built-in retry, timeout, circuit breaker support
- **Streaming**: Server-Sent Events (SSE) and streaming responses
- **Modern**: Active development with latest features

## Documentation Structure

1. [Getting Started](Getting_Started.md) - Setup and basic usage
2. [Configuration](Configuration.md) - Advanced configuration and customization
3. [Request Building](Request_Building.md) - Building HTTP requests
4. [Response Handling](Response_Handling.md) - Processing responses
5. [Error Handling](Error_Handling.md) - Exception management and recovery
6. [Resilience Patterns](Resilience_Patterns.md) - Retry, timeout, circuit breaker
7. [Authentication](Authentication.md) - OAuth2, JWT, Basic Auth
8. [Testing](Testing.md) - MockWebServer and testing strategies
9. [Performance](Performance.md) - Optimization and best practices
10. [Real-World Examples](Examples.md) - Production patterns

## Quick Start

```java
// Create WebClient
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .build();

// GET request
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class);

// POST request
Mono<User> created = webClient.post()
    .uri("/users")
    .bodyValue(new User("john", "john@example.com"))
    .retrieve()
    .bodyToMono(User.class);
```

## Core Concepts

### Request Flow

```
WebClient.create()
    → method() [get/post/put/delete]
    → uri()
    → headers()
    → body()
    → retrieve() / exchange()
    → bodyToMono() / bodyToFlux()
```

### Mono vs Flux

```java
// Mono: Single value (0 or 1)
Mono<User> user = webClient.get()
    .uri("/users/1")
    .retrieve()
    .bodyToMono(User.class);

// Flux: Multiple values (0 to N)
Flux<User> users = webClient.get()
    .uri("/users")
    .retrieve()
    .bodyToFlux(User.class);
```

## Common Use Cases

### 1. REST API Client

```java
@Service
public class UserApiClient {
    private final WebClient webClient;
    
    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
}
```

### 2. Microservices Communication

```java
@Service
public class OrderService {
    private final WebClient userClient;
    private final WebClient inventoryClient;
    
    public Mono<OrderDetails> createOrder(Order order) {
        Mono<User> user = userClient.get()
            .uri("/users/{id}", order.getUserId())
            .retrieve()
            .bodyToMono(User.class);
        
        Mono<Inventory> inventory = inventoryClient.get()
            .uri("/inventory/{id}", order.getProductId())
            .retrieve()
            .bodyToMono(Inventory.class);
        
        return Mono.zip(user, inventory)
            .map(tuple -> new OrderDetails(order, tuple.getT1(), tuple.getT2()));
    }
}
```

### 3. External API Integration

```java
@Service
public class PaymentGatewayClient {
    private final WebClient webClient;
    
    public Mono<PaymentResponse> processPayment(PaymentRequest request) {
        return webClient.post()
            .uri("/charge")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}
```

## When to Use WebClient

✅ **Use WebClient when:**
- Building reactive applications
- High concurrency requirements
- Microservices communication
- Streaming data (SSE, WebSocket)
- Need non-blocking I/O
- Modern Spring Boot applications

❌ **Avoid WebClient when:**
- Legacy applications with blocking code
- Simple synchronous operations
- Team unfamiliar with reactive programming
- Using blocking JDBC drivers

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- For testing -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <scope>test</scope>
</dependency>
```

## Next Steps

Start with [Getting Started](Getting_Started.md) to build your first WebClient integration.
