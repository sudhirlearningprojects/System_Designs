# Spring WebFlux - Reactive Programming

[← Back to Index](README.md) | [← Previous: Spring Data JPA Part 4](06_Spring_Data_JPA_Part4.md) | [Next: Spring Security →](08_Spring_Security.md)

## Table of Contents
- [Theory: Understanding Reactive Programming](#theory-understanding-reactive-programming)
- [Reactive vs Blocking](#reactive-vs-blocking)
- [Mono vs Flux](#mono-vs-flux)
- [WebClient](#webclient)
- [Reactive Repository](#reactive-repository)

---

## Theory: Understanding Reactive Programming

### What is Reactive Programming?

Reactive programming is a **declarative programming paradigm** concerned with **data streams** and **propagation of change**.

### The Problem with Blocking I/O

**Traditional Blocking Model**:
```
Thread 1: Request → [Wait for DB] → Response
Thread 2: Request → [Wait for DB] → Response
Thread 3: Request → [Wait for DB] → Response

Problem: Threads are blocked waiting for I/O
Result: Limited scalability (thread-per-request)
```

**Reactive Non-Blocking Model**:
```
Thread 1: Request A → DB call → Handle Request B → DB call → Handle Request C
          ↓                      ↓                      ↓
       Response A              Response B              Response C

Benefit: One thread handles multiple requests
Result: High scalability with fewer threads
```

### Core Concepts

**1. Asynchronous & Non-Blocking**
- Operations don't wait for completion
- Thread is free to handle other requests
- Callbacks/Promises handle results

**2. Backpressure**
- Consumer controls data flow rate
- Prevents overwhelming slow consumers
- Publisher respects consumer's capacity

**3. Event-Driven**
- React to events (data arrival, errors, completion)
- Push-based (not pull-based)
- Declarative composition

### Reactive Streams Specification

**Four Interfaces**:
1. **Publisher**: Produces data
2. **Subscriber**: Consumes data
3. **Subscription**: Link between Publisher and Subscriber
4. **Processor**: Both Publisher and Subscriber

### Project Reactor (Spring's Implementation)

**Two Core Types**:

**Mono<T>**: 0 or 1 element
- Like CompletableFuture
- Single value or empty
- Example: Database query by ID

**Flux<T>**: 0 to N elements
- Like Stream but async
- Multiple values over time
- Example: Database query returning list

### When to Use Reactive?

✅ **Use Reactive When**:
- High concurrency requirements (10K+ concurrent users)
- I/O-bound operations (database, external APIs)
- Streaming data (real-time updates, SSE)
- Microservices with many service calls
- Limited resources (cloud, containers)

❌ **Avoid Reactive When**:
- CPU-bound operations
- Simple CRUD applications
- Team unfamiliar with reactive
- Blocking libraries (JDBC, legacy code)
- Debugging complexity not justified

### Reactive vs Traditional Performance

| Metric | Traditional (Blocking) | Reactive (Non-Blocking) |
|--------|----------------------|------------------------|
| Threads | 200 threads | 10 threads |
| Memory | High (1MB per thread) | Low (shared threads) |
| Concurrent Users | 200 | 10,000+ |
| Latency | Higher (context switching) | Lower |
| Throughput | Limited by threads | Limited by CPU/Network |

### Spring MVC vs Spring WebFlux

| Aspect | Spring MVC | Spring WebFlux |
|--------|-----------|---------------|
| Model | Blocking | Non-blocking |
| Server | Tomcat, Jetty | Netty, Undertow |
| API | Servlet API | Reactive Streams |
| Return Type | Object, List | Mono, Flux |
| Database | JDBC (blocking) | R2DBC (reactive) |
| Scalability | Thread-per-request | Event loop |

---

## Reactive vs Blocking

### Blocking (Traditional)
```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id); // Blocks thread
    }
}
```

### Non-Blocking (Reactive)
```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return userService.getUser(id); // Non-blocking
    }
    
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userService.getAllUsers(); // Stream
    }
}
```

---

## Mono vs Flux

```java
// Mono - 0 or 1 element
Mono<User> user = Mono.just(new User("John"));
Mono<User> empty = Mono.empty();

// Flux - 0 to N elements
Flux<User> users = Flux.just(
    new User("John"),
    new User("Jane"),
    new User("Bob")
);

// Operations
Mono<String> username = user.map(u -> u.getUsername());
Flux<String> usernames = users.map(User::getUsername);
Flux<User> filtered = users.filter(u -> u.getAge() > 18);

// Combining
Mono<User> combined = Mono.zip(user1, user2, (u1, u2) -> merge(u1, u2));
```

---

## WebClient

```java
@Service
public class ExternalApiService {
    private final WebClient webClient;
    
    public ExternalApiService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.example.com").build();
    }
    
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
}
```

---

## Reactive Repository

```java
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Flux<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public Mono<User> createUser(User user) {
        return userRepository.save(user);
    }
    
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

---

[← Previous: Spring Data JPA Part 4](06_Spring_Data_JPA_Part4.md) | [Next: Spring Security →](08_Spring_Security.md)
