# Performance Optimization in Spring WebFlux

## Overview

Spring WebFlux provides excellent performance for I/O-bound applications. This guide covers optimization strategies and best practices.

## Thread Model

### Event Loop

WebFlux uses an event loop model with a small number of threads:

```yaml
spring:
  reactor:
    netty:
      ioWorkerCount: 4  # Default: CPU cores
      ioSelectCount: 1  # Default: 1
```

### Schedulers

```java
// Parallel scheduler (CPU-bound)
Flux.range(1, 100)
    .parallel()
    .runOn(Schedulers.parallel())
    .map(this::cpuIntensiveOperation)
    .sequential();

// Bounded elastic scheduler (blocking I/O)
Mono.fromCallable(() -> blockingDatabaseCall())
    .subscribeOn(Schedulers.boundedElastic());

// Single scheduler (sequential tasks)
Flux.interval(Duration.ofSeconds(1))
    .subscribeOn(Schedulers.single());
```

## Connection Pooling

### R2DBC Connection Pool

```java
@Configuration
public class R2dbcConfig {
    
    @Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql")
            .option(HOST, "localhost")
            .option(PORT, 5432)
            .option(USER, "postgres")
            .option(PASSWORD, "password")
            .option(DATABASE, "mydb")
            .build();
        
        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
            .initialSize(10)              // Initial connections
            .maxSize(50)                  // Max connections
            .maxIdleTime(Duration.ofMinutes(30))
            .maxAcquireTime(Duration.ofSeconds(3))
            .maxCreateConnectionTime(Duration.ofSeconds(5))
            .validationQuery("SELECT 1")
            .build();
        
        return new ConnectionPool(poolConfig);
    }
}
```

### WebClient Connection Pool

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
        .responseTimeout(Duration.ofSeconds(5))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Caching Strategies

### Application-Level Caching

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final Map<Long, User> cache = new ConcurrentHashMap<>();
    
    public Mono<User> getUserById(Long id) {
        User cached = cache.get(id);
        if (cached != null) {
            return Mono.just(cached);
        }
        
        return userRepository.findById(id)
            .doOnNext(user -> cache.put(id, user));
    }
}
```

### Redis Caching

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, User> redisTemplate;
    
    public Mono<User> getUserById(Long id) {
        String key = "user:" + id;
        
        return redisTemplate.opsForValue().get(key)
            .switchIfEmpty(
                userRepository.findById(id)
                    .flatMap(user -> 
                        redisTemplate.opsForValue()
                            .set(key, user, Duration.ofMinutes(10))
                            .thenReturn(user)
                    )
            );
    }
    
    public Mono<User> updateUser(Long id, User user) {
        return userRepository.save(user)
            .flatMap(updated -> {
                String key = "user:" + id;
                return redisTemplate.delete(key)
                    .thenReturn(updated);
            });
    }
}
```

### Caffeine Cache

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return cacheManager;
    }
}

@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public Mono<Void> deleteUser(Long id) {
        return userRepository.deleteById(id);
    }
}
```

## Batching Operations

### Database Batching

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public Flux<User> createUsers(List<User> users) {
        return userRepository.saveAll(users)
            .buffer(100)  // Batch size
            .flatMap(Flux::fromIterable);
    }
    
    public Mono<Void> deleteUsers(List<Long> ids) {
        return Flux.fromIterable(ids)
            .buffer(100)
            .flatMap(batch -> userRepository.deleteAllById(batch))
            .then();
    }
}
```

### Request Batching

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final WebClient webClient;
    
    public Flux<Product> getProducts(List<Long> ids) {
        return Flux.fromIterable(ids)
            .buffer(50)  // Batch requests
            .flatMap(batch -> 
                webClient.post()
                    .uri("/products/batch")
                    .bodyValue(batch)
                    .retrieve()
                    .bodyToFlux(Product.class)
            );
    }
}
```

## Parallel Processing

### Parallel Flux

```java
public Flux<Result> processInParallel(List<Item> items) {
    return Flux.fromIterable(items)
        .parallel(4)  // Number of rails
        .runOn(Schedulers.parallel())
        .map(this::processItem)
        .sequential();
}
```

### Concurrent Requests

```java
public Mono<UserDetails> getUserDetails(Long userId) {
    Mono<User> user = userService.getUserById(userId);
    Mono<List<Order>> orders = orderService.getOrdersByUserId(userId).collectList();
    Mono<Address> address = addressService.getAddressByUserId(userId);
    
    // Execute in parallel
    return Mono.zip(user, orders, address)
        .map(tuple -> new UserDetails(tuple.getT1(), tuple.getT2(), tuple.getT3()));
}
```

## Backpressure Handling

### Buffer Strategy

```java
public Flux<Data> streamData() {
    return dataSource.stream()
        .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_LATEST);
}
```

### Drop Strategy

```java
public Flux<Data> streamData() {
    return dataSource.stream()
        .onBackpressureDrop(dropped -> 
            log.warn("Dropped item: {}", dropped));
}
```

### Latest Strategy

```java
public Flux<Data> streamData() {
    return dataSource.stream()
        .onBackpressureLatest();
}
```

## Memory Optimization

### Avoid Collecting Large Streams

```java
// Bad: Collects all items in memory
public Mono<List<User>> getAllUsers() {
    return userRepository.findAll().collectList();
}

// Good: Stream processing
public Flux<User> streamUsers() {
    return userRepository.findAll();
}
```

### Use Window/Buffer Wisely

```java
// Process in windows
public Flux<List<User>> processUsersInBatches() {
    return userRepository.findAll()
        .window(100)
        .flatMap(window -> window.collectList())
        .flatMap(this::processBatch);
}
```

## Database Optimization

### Indexing

```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

### Query Optimization

```java
// Bad: N+1 query problem
public Flux<Order> getOrdersWithUsers() {
    return orderRepository.findAll()
        .flatMap(order -> 
            userRepository.findById(order.getUserId())
                .map(user -> {
                    order.setUser(user);
                    return order;
                })
        );
}

// Good: Batch loading
public Flux<Order> getOrdersWithUsers() {
    return orderRepository.findAll()
        .collectList()
        .flatMapMany(orders -> {
            Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());
            
            return userRepository.findAllById(userIds)
                .collectMap(User::getId)
                .flatMapMany(userMap -> Flux.fromIterable(orders)
                    .map(order -> {
                        order.setUser(userMap.get(order.getUserId()));
                        return order;
                    })
                );
        });
}
```

### Pagination

```java
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    
    @Query("SELECT * FROM users ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<User> findAllPaginated(@Param("limit") int limit, @Param("offset") int offset);
}

@Service
public class UserService {
    
    public Flux<User> getUsers(int page, int size) {
        int offset = page * size;
        return userRepository.findAllPaginated(size, offset);
    }
}
```

## Monitoring and Metrics

### Micrometer Integration

```java
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;
    
    public Mono<User> getUserById(Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return userRepository.findById(id)
            .doOnSuccess(user -> {
                sample.stop(Timer.builder("user.fetch")
                    .tag("status", "success")
                    .register(meterRegistry));
            })
            .doOnError(error -> {
                sample.stop(Timer.builder("user.fetch")
                    .tag("status", "error")
                    .register(meterRegistry));
            });
    }
}
```

### Custom Metrics

```java
@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    @PostConstruct
    public void init() {
        requestCounter = Counter.builder("api.requests")
            .tag("type", "user")
            .register(meterRegistry);
        
        requestTimer = Timer.builder("api.request.duration")
            .tag("type", "user")
            .register(meterRegistry);
    }
    
    public void recordRequest() {
        requestCounter.increment();
    }
    
    public void recordDuration(Duration duration) {
        requestTimer.record(duration);
    }
}
```

## Load Testing

### Gatling Script

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UserLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
  
  val scn = scenario("User API Load Test")
    .exec(http("Get User")
      .get("/api/users/1")
      .check(status.is(200)))
    .pause(1)
    .exec(http("Create User")
      .post("/api/users")
      .body(StringBody("""{"username":"test","email":"test@example.com"}"""))
      .check(status.is(201)))
  
  setUp(
    scn.inject(
      rampUsers(1000) during (60 seconds)
    )
  ).protocols(httpProtocol)
}
```

## Best Practices

1. **Use appropriate schedulers**: boundedElastic for blocking, parallel for CPU-bound
2. **Configure connection pools**: Set appropriate sizes based on load
3. **Implement caching**: Cache frequently accessed data
4. **Batch operations**: Reduce database round trips
5. **Handle backpressure**: Use appropriate strategies
6. **Avoid blocking**: Never block in reactive chains
7. **Monitor performance**: Track metrics and logs
8. **Optimize queries**: Use indexes and efficient queries
9. **Stream large datasets**: Don't collect everything in memory
10. **Test under load**: Use load testing tools

## Performance Benchmarks

### WebFlux vs Spring MVC

| Metric | Spring MVC | Spring WebFlux |
|--------|-----------|----------------|
| Threads | 200 | 8 |
| Memory | 2GB | 512MB |
| Throughput | 10K req/s | 50K req/s |
| Latency (p99) | 500ms | 100ms |
| Concurrent Connections | 1K | 10K |

### Optimization Impact

| Optimization | Improvement |
|-------------|-------------|
| Connection Pooling | 3x throughput |
| Redis Caching | 10x faster reads |
| Database Indexing | 5x faster queries |
| Batching | 2x throughput |
| Parallel Processing | 4x faster (4 cores) |

## Troubleshooting

### High Memory Usage

```java
// Check for memory leaks
public Flux<User> streamUsers() {
    return userRepository.findAll()
        .doOnNext(user -> log.debug("Processing: {}", user))
        .doOnComplete(() -> log.info("Stream completed"))
        .doOnError(e -> log.error("Stream error", e));
}
```

### Slow Queries

```java
// Add query timing
public Mono<User> getUserById(Long id) {
    long start = System.currentTimeMillis();
    
    return userRepository.findById(id)
        .doOnSuccess(user -> {
            long duration = System.currentTimeMillis() - start;
            if (duration > 100) {
                log.warn("Slow query: {}ms", duration);
            }
        });
}
```

### Thread Starvation

```java
// Move blocking operations to boundedElastic
public Mono<Result> processWithBlocking() {
    return Mono.fromCallable(() -> {
        // Blocking operation
        return blockingService.process();
    })
    .subscribeOn(Schedulers.boundedElastic());
}
```

## Next Steps

- [Examples](Examples.md) - Real-world performance examples
- [Getting Started](Getting_Started.md) - Build your first app
