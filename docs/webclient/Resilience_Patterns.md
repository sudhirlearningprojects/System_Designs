# Resilience Patterns with WebClient

## Retry Pattern

### Basic Retry

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .retry(3);
```

### Exponential Backoff

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
        .maxBackoff(Duration.ofSeconds(10))
        .jitter(0.5));
```

### Conditional Retry

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
        .filter(throwable -> throwable instanceof WebClientException)
        .doBeforeRetry(signal -> 
            log.warn("Retrying... attempt: {}", signal.totalRetries() + 1)));
```

### Custom Retry Logic

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
        .filter(throwable -> !(throwable instanceof UserNotFoundException))
        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
            new ServiceUnavailableException("Max retries exceeded")));
```

## Timeout Pattern

### Response Timeout

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .timeout(Duration.ofSeconds(5));
```

### Timeout with Fallback

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .timeout(Duration.ofSeconds(5), Mono.just(getDefaultUser()));
```

### Timeout with Error Mapping

```java
Mono<User> user = webClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .bodyToMono(User.class)
    .timeout(Duration.ofSeconds(5))
    .onErrorMap(TimeoutException.class, e ->
        new ServiceUnavailableException("Request timeout"));
```

## Circuit Breaker Pattern

### Using Resilience4j

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>
```

### Configuration

```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
        
        return CircuitBreakerRegistry.of(config);
    }
}
```

### Service with Circuit Breaker

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

### Circuit Breaker with Fallback

```java
public Mono<User> getUser(Long id) {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
    
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .onErrorResume(CallNotPermittedException.class, e -> {
            log.warn("Circuit breaker open, using fallback");
            return Mono.just(getDefaultUser());
        });
}
```

## Bulkhead Pattern

### Configuration

```java
@Bean
public BulkheadRegistry bulkheadRegistry() {
    BulkheadConfig config = BulkheadConfig.custom()
        .maxConcurrentCalls(10)
        .maxWaitDuration(Duration.ofMillis(500))
        .build();
    
    return BulkheadRegistry.of(config);
}
```

### Service with Bulkhead

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final WebClient webClient;
    private final BulkheadRegistry bulkheadRegistry;
    
    public Mono<User> getUser(Long id) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("userService");
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .transformDeferred(BulkheadOperator.of(bulkhead));
    }
}
```

## Rate Limiter Pattern

### Configuration

```java
@Bean
public RateLimiterRegistry rateLimiterRegistry() {
    RateLimiterConfig config = RateLimiterConfig.custom()
        .limitForPeriod(10)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofMillis(500))
        .build();
    
    return RateLimiterRegistry.of(config);
}
```

### Service with Rate Limiter

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final WebClient webClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public Mono<User> getUser(Long id) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("userService");
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .transformDeferred(RateLimiterOperator.of(rateLimiter));
    }
}
```

## Fallback Pattern

### Simple Fallback

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .onErrorResume(e -> Mono.just(getDefaultUser()));
}
```

### Fallback Chain

```java
public Mono<User> getUser(Long id) {
    return primaryWebClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .onErrorResume(e -> 
            secondaryWebClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(User.class))
        .onErrorResume(e -> Mono.just(getDefaultUser()));
}
```

### Conditional Fallback

```java
public Mono<User> getUser(Long id) {
    return webClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class)
        .onErrorResume(WebClientResponseException.NotFound.class, e ->
            Mono.error(new UserNotFoundException(id)))
        .onErrorResume(WebClientException.class, e -> {
            log.warn("Service unavailable, using cache");
            return getCachedUser(id);
        });
}
```

## Combined Patterns

### Retry + Timeout + Circuit Breaker

```java
@Service
@RequiredArgsConstructor
public class ResilientUserService {
    
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<User> getUser(Long id) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> !(throwable instanceof TimeoutException)))
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(this::handleError);
    }
    
    private Mono<User> handleError(Throwable error) {
        if (error instanceof CallNotPermittedException) {
            log.warn("Circuit breaker open");
            return Mono.just(getDefaultUser());
        }
        if (error instanceof TimeoutException) {
            log.error("Request timeout");
            return Mono.error(new ServiceUnavailableException("Timeout"));
        }
        return Mono.error(error);
    }
}
```

### All Patterns Combined

```java
@Service
@RequiredArgsConstructor
public class FullyResilientService {
    
    private final WebClient webClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public Mono<User> getUser(Long id) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("userService");
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("userService");
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> !(throwable instanceof TimeoutException)))
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
            .onErrorResume(this::handleError);
    }
    
    private Mono<User> handleError(Throwable error) {
        log.error("Error fetching user", error);
        return Mono.just(getDefaultUser());
    }
}
```

## Caching Pattern

### Simple Cache

```java
@Service
@RequiredArgsConstructor
public class CachedUserService {
    
    private final WebClient webClient;
    private final Map<Long, User> cache = new ConcurrentHashMap<>();
    
    public Mono<User> getUser(Long id) {
        User cached = cache.get(id);
        if (cached != null) {
            return Mono.just(cached);
        }
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .doOnNext(user -> cache.put(id, user));
    }
}
```

### Redis Cache

```java
@Service
@RequiredArgsConstructor
public class RedisCachedUserService {
    
    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, User> redisTemplate;
    
    public Mono<User> getUser(Long id) {
        String key = "user:" + id;
        
        return redisTemplate.opsForValue().get(key)
            .switchIfEmpty(
                webClient.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .bodyToMono(User.class)
                    .flatMap(user -> 
                        redisTemplate.opsForValue()
                            .set(key, user, Duration.ofMinutes(10))
                            .thenReturn(user))
            );
    }
}
```

## Health Check Pattern

```java
@Service
@RequiredArgsConstructor
public class ServiceHealthChecker {
    
    private final WebClient webClient;
    
    public Mono<Boolean> isHealthy() {
        return webClient.get()
            .uri("/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> "UP".equals(response))
            .timeout(Duration.ofSeconds(2))
            .onErrorReturn(false);
    }
    
    public Flux<Boolean> monitorHealth() {
        return Flux.interval(Duration.ofSeconds(30))
            .flatMap(tick -> isHealthy())
            .doOnNext(healthy -> {
                if (!healthy) {
                    log.warn("Service is unhealthy");
                }
            });
    }
}
```

## Monitoring and Metrics

```java
@Service
@RequiredArgsConstructor
public class MonitoredUserService {
    
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;
    
    public Mono<User> getUser(Long id) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .doOnSuccess(user -> {
                sample.stop(Timer.builder("webclient.request")
                    .tag("method", "GET")
                    .tag("uri", "/users")
                    .tag("status", "success")
                    .register(meterRegistry));
            })
            .doOnError(error -> {
                sample.stop(Timer.builder("webclient.request")
                    .tag("method", "GET")
                    .tag("uri", "/users")
                    .tag("status", "error")
                    .register(meterRegistry));
            });
    }
}
```

## Best Practices

1. **Always set timeouts**: Prevent hanging requests
2. **Use exponential backoff**: For retry logic
3. **Implement circuit breakers**: Prevent cascading failures
4. **Add fallbacks**: Graceful degradation
5. **Cache when possible**: Reduce load on services
6. **Monitor metrics**: Track success/failure rates
7. **Log appropriately**: Debug issues effectively
8. **Test resilience**: Simulate failures in tests

## Next Steps

- [Error Handling](Error_Handling.md) - Comprehensive error strategies
- [Testing](Testing.md) - Testing resilience patterns
- [Examples](Examples.md) - Real-world implementations
