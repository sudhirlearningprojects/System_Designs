# Spring WebFlux - Reactive Programming Guide

## Overview

Spring WebFlux is a reactive web framework built on Project Reactor, designed for non-blocking, event-driven applications that can scale with a small number of threads.

## Key Concepts

### Reactive Streams
- **Publisher**: Emits data (Mono, Flux)
- **Subscriber**: Consumes data
- **Subscription**: Connection between Publisher and Subscriber
- **Backpressure**: Flow control mechanism

### Mono vs Flux
```java
// Mono: 0 or 1 element
Mono<User> user = userRepository.findById(id);

// Flux: 0 to N elements
Flux<User> users = userRepository.findAll();
```

## WebFlux vs Spring MVC

| Feature | Spring MVC | Spring WebFlux |
|---------|-----------|----------------|
| Programming Model | Blocking | Non-blocking |
| Thread Model | Thread per request | Event loop |
| Concurrency | High thread count | Low thread count |
| Scalability | Vertical | Horizontal |
| Use Case | Traditional CRUD | High concurrency, streaming |

## Documentation Structure

1. [Getting Started](Getting_Started.md) - Setup and first application
2. [Annotated Controllers](Annotated_Controllers.md) - @RestController approach
3. [Functional Endpoints](Functional_Endpoints.md) - RouterFunction approach
4. [WebClient](WebClient.md) - Reactive HTTP client
5. [Reactive Data Access](Reactive_Data_Access.md) - R2DBC, MongoDB
6. [Error Handling](Error_Handling.md) - Exception management
7. [Testing](Testing.md) - WebTestClient and testing strategies
8. [Performance](Performance.md) - Optimization and backpressure
9. [Real-World Examples](Examples.md) - Production patterns

## Quick Start

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return userRepository.findById(id);
    }
    
    @GetMapping
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return userRepository.save(user);
    }
}
```

## When to Use WebFlux

✅ **Use WebFlux when:**
- High concurrency requirements (10K+ concurrent connections)
- Streaming data (SSE, WebSocket)
- Microservices with reactive dependencies
- I/O-bound operations dominate
- Need efficient resource utilization

❌ **Avoid WebFlux when:**
- Simple CRUD applications
- Blocking dependencies (JDBC, legacy libraries)
- Team unfamiliar with reactive programming
- CPU-bound operations dominate

## Core Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>

<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Next Steps

Start with [Getting Started](Getting_Started.md) to build your first reactive application.
