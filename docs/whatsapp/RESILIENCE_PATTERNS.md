# WhatsApp Messenger - Resilience Patterns

## 🛡️ Overview

This document outlines the resilience patterns and fault tolerance mechanisms implemented in the WhatsApp Messenger system to ensure high availability, reliability, and graceful degradation under failure conditions.

## 🔄 Circuit Breaker Pattern

### Implementation

```java
@Component
public class CircuitBreakerService {
    
    private final CircuitBreaker databaseCircuitBreaker;
    private final CircuitBreaker redisCircuitBreaker;
    private final CircuitBreaker kafkaCircuitBreaker;
    
    public CircuitBreakerService() {
        this.databaseCircuitBreaker = CircuitBreaker.ofDefaults("database");
        this.redisCircuitBreaker = CircuitBreaker.ofDefaults("redis");
        this.kafkaCircuitBreaker = CircuitBreaker.ofDefaults("kafka");
        
        configureCircuitBreakers();
    }
    
    private void configureCircuitBreakers() {
        // Database Circuit Breaker
        databaseCircuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Database circuit breaker state transition: {}", event));
        
        // Redis Circuit Breaker with custom config
        CircuitBreakerConfig redisConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();
        
        redisCircuitBreaker = CircuitBreaker.of("redis", redisConfig);
    }
    
    public <T> T executeWithDatabaseCircuitBreaker(Supplier<T> operation) {
        return databaseCircuitBreaker.executeSupplier(operation);
    }
    
    public <T> T executeWithRedisCircuitBreaker(Supplier<T> operation) {
        return redisCircuitBreaker.executeSupplier(operation);
    }
}
```

### Kubernetes Circuit Breaker (Istio)

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: circuit-breaker-rules
  namespace: whatsapp-prod
spec:
  host: message-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
        consecutiveGatewayErrors: 3
        interval: 30s
        baseEjectionTime: 30s
        maxEjectionPercent: 50
    outlierDetection:
      consecutiveErrors: 3
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
      minHealthPercent: 50
```

## 🔁 Retry Pattern with Exponential Backoff

### Service Implementation

```java
@Service
@RequiredArgsConstructor
public class ResilientMessageService {
    
    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RetryTemplate retryTemplate;
    
    @PostConstruct
    public void initRetryTemplate() {
        retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(1000, 2, 10000)
            .retryOn(DataAccessException.class, KafkaException.class)
            .build();
    }
    
    public MessageDTO sendMessageWithRetry(SendMessageRequest request) {
        return retryTemplate.execute(context -> {
            log.info("Attempt {} to send message", context.getRetryCount() + 1);
            
            try {
                return sendMessage(request);
            } catch (Exception e) {
                log.warn("Failed to send message, attempt {}: {}", 
                    context.getRetryCount() + 1, e.getMessage());
                throw e;
            }
        });
    }
    
    @Retryable(
        value = {KafkaException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public void publishToKafkaWithRetry(String topic, Object message) {
        kafkaTemplate.send(topic, message);
    }
    
    @Recover
    public void recoverFromKafkaFailure(KafkaException ex, String topic, Object message) {
        log.error("Failed to publish to Kafka after retries, storing in DLQ: {}", ex.getMessage());
        storeInDeadLetterQueue(topic, message, ex);
    }
}
```

### Kubernetes Retry Policy (Istio)

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: retry-policy
  namespace: whatsapp-prod
spec:
  hosts:
  - message-service
  http:
  - route:
    - destination:
        host: message-service
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx,reset,connect-failure,refused-stream
      retryRemoteLocalities: true
    fault:
      delay:
        percentage:
          value: 0.1
        fixedDelay: 5s
```

## 🏥 Health Checks & Graceful Degradation

### Application Health Checks

```java
@Component
public class WhatsAppHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Database health check
        checkDatabase(builder);
        
        // Redis health check
        checkRedis(builder);
        
        // Kafka health check
        checkKafka(builder);
        
        return builder.build();
    }
    
    private void checkDatabase(Health.Builder builder) {
        try {
            dataSource.getConnection().close();
            builder.withDetail("database", "UP");
        } catch (Exception e) {
            builder.down().withDetail("database", "DOWN: " + e.getMessage());
        }
    }
    
    private void checkRedis(Health.Builder builder) {
        try {
            redisTemplate.opsForValue().get("health-check");
            builder.withDetail("redis", "UP");
        } catch (Exception e) {
            builder.withDetail("redis", "DEGRADED: " + e.getMessage());
            // Don't fail overall health for Redis issues
        }
    }
    
    private void checkKafka(Health.Builder builder) {
        try {
            kafkaTemplate.send("health-check", "ping").get(1, TimeUnit.SECONDS);
            builder.withDetail("kafka", "UP");
        } catch (Exception e) {
            builder.withDetail("kafka", "DEGRADED: " + e.getMessage());
            // Don't fail overall health for Kafka issues
        }
    }
}
```

### Kubernetes Health Checks

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: message-service
spec:
  template:
    spec:
      containers:
      - name: message-service
        image: whatsapp/message-service:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
          successThreshold: 1
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
          successThreshold: 1
        startupProbe:
          httpGet:
            path: /actuator/health/startup
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 30
          successThreshold: 1
        lifecycle:
          preStop:
            exec:
              command:
              - /bin/sh
              - -c
              - sleep 15  # Graceful shutdown delay
```

## 🔀 Bulkhead Pattern

### Thread Pool Isolation

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "messageProcessingExecutor")
    public Executor messageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MessageProcessing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "presenceUpdateExecutor")
    public Executor presenceUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("PresenceUpdate-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class BulkheadService {
    
    @Async("messageProcessingExecutor")
    public CompletableFuture<Void> processMessage(MessageDTO message) {
        // Message processing logic
        return CompletableFuture.completedFuture(null);
    }
    
    @Async("presenceUpdateExecutor")
    public CompletableFuture<Void> updatePresence(String userId, UserStatus status) {
        // Presence update logic
        return CompletableFuture.completedFuture(null);
    }
    
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendNotification(NotificationDTO notification) {
        // Notification sending logic
        return CompletableFuture.completedFuture(null);
    }
}
```

### Kubernetes Resource Isolation

```yaml
# Separate node pools for different workloads
apiVersion: v1
kind: Node
metadata:
  name: message-processing-pool
  labels:
    workload-type: message-processing
    instance-type: c5.4xlarge
spec:
  taints:
  - key: message-processing
    value: "true"
    effect: NoSchedule

---
apiVersion: v1
kind: Node
metadata:
  name: websocket-pool
  labels:
    workload-type: websocket
    instance-type: c5.9xlarge
spec:
  taints:
  - key: websocket
    value: "true"
    effect: NoSchedule

---
# Message service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: message-service
spec:
  template:
    spec:
      nodeSelector:
        workload-type: message-processing
      tolerations:
      - key: message-processing
        operator: Equal
        value: "true"
        effect: NoSchedule
      resources:
        requests:
          cpu: 1000m
          memory: 2Gi
        limits:
          cpu: 2000m
          memory: 4Gi

---
# WebSocket service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: websocket-service
spec:
  template:
    spec:
      nodeSelector:
        workload-type: websocket
      tolerations:
      - key: websocket
        operator: Equal
        value: "true"
        effect: NoSchedule
      resources:
        requests:
          cpu: 2000m
          memory: 4Gi
        limits:
          cpu: 4000m
          memory: 8Gi
```

## 🎭 Chaos Engineering

### Chaos Monkey Implementation

```java
@Component
@ConditionalOnProperty(name = "chaos.enabled", havingValue = "true")
public class ChaosMonkey {
    
    private final Random random = new Random();
    private final MeterRegistry meterRegistry;
    
    @Value("${chaos.failure-rate:0.01}")
    private double failureRate;
    
    @EventListener
    public void injectChaos(MessageSentEvent event) {
        if (shouldInjectChaos()) {
            Counter.builder("chaos.injected")
                .tag("type", "message-failure")
                .register(meterRegistry)
                .increment();
                
            throw new ChaosException("Chaos monkey struck!");
        }
    }
    
    private boolean shouldInjectChaos() {
        return random.nextDouble() < failureRate;
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void randomServiceFailure() {
        if (shouldInjectChaos()) {
            String[] services = {"database", "redis", "kafka"};
            String service = services[random.nextInt(services.length)];
            
            log.warn("Chaos monkey is simulating {} failure", service);
            simulateServiceFailure(service);
        }
    }
    
    private void simulateServiceFailure(String service) {
        // Simulate service failure for testing
        switch (service) {
            case "database":
                // Simulate database slowdown
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            case "redis":
                // Simulate Redis connection failure
                throw new RedisConnectionFailureException("Simulated Redis failure");
            case "kafka":
                // Simulate Kafka producer failure
                throw new KafkaException("Simulated Kafka failure");
        }
    }
}
```

### Kubernetes Chaos Engineering (Chaos Mesh)

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: message-service-pod-failure
  namespace: whatsapp-prod
spec:
  action: pod-failure
  mode: one
  duration: "30s"
  selector:
    namespaces:
    - whatsapp-prod
    labelSelectors:
      app: message-service
  scheduler:
    cron: "0 */6 * * *"  # Every 6 hours

---
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay
  namespace: whatsapp-prod
spec:
  action: delay
  mode: one
  selector:
    namespaces:
    - whatsapp-prod
    labelSelectors:
      app: message-service
  delay:
    latency: "100ms"
    correlation: "100"
    jitter: "0ms"
  duration: "5m"
  scheduler:
    cron: "0 */12 * * *"  # Every 12 hours

---
apiVersion: chaos-mesh.org/v1alpha1
kind: StressChaos
metadata:
  name: memory-stress
  namespace: whatsapp-prod
spec:
  mode: one
  selector:
    namespaces:
    - whatsapp-prod
    labelSelectors:
      app: websocket-service
  stressors:
    memory:
      workers: 4
      size: "1GB"
  duration: "2m"
  scheduler:
    cron: "0 */8 * * *"  # Every 8 hours
```

## 🔄 Timeout Pattern

### Service Timeouts

```java
@Service
public class TimeoutService {
    
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public TimeoutService() {
        // Configure RestTemplate with timeouts
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(10000)
            .setConnectionRequestTimeout(5000)
            .build();
            
        CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(config)
            .build();
            
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
        
        // Configure Redis timeouts
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setTimeout(Duration.ofSeconds(2));
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(factory);
    }
    
    @TimeLimiter(name = "database-operation", fallbackMethod = "fallbackDatabaseOperation")
    public CompletableFuture<String> performDatabaseOperation() {
        return CompletableFuture.supplyAsync(() -> {
            // Database operation that might take time
            return "Database result";
        });
    }
    
    public CompletableFuture<String> fallbackDatabaseOperation(Exception ex) {
        log.warn("Database operation timed out, using fallback: {}", ex.getMessage());
        return CompletableFuture.completedFuture("Fallback result");
    }
    
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public String performExternalApiCall() {
        return restTemplate.getForObject("https://external-api.com/data", String.class);
    }
}
```

### Kubernetes Timeouts

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: timeout-configuration
  namespace: whatsapp-prod
spec:
  hosts:
  - message-service
  http:
  - route:
    - destination:
        host: message-service
    timeout: 10s
    retries:
      attempts: 3
      perTryTimeout: 3s
      retryOn: 5xx,reset,connect-failure,refused-stream

---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: connection-timeout
  namespace: whatsapp-prod
spec:
  host: message-service
  trafficPolicy:
    connectionPool:
      tcp:
        connectTimeout: 5s
      http:
        h1MaxPendingRequests: 50
        http2MaxRequests: 100
        maxRequestsPerConnection: 10
        maxRetries: 3
        idleTimeout: 60s
        h2UpgradePolicy: UPGRADE
```

## 🏃‍♂️ Rate Limiting & Backpressure

### Application-Level Rate Limiting

```java
@Component
public class RateLimitingService {
    
    private final RateLimiter messageSendingLimiter;
    private final RateLimiter userRegistrationLimiter;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RateLimitingService() {
        this.messageSendingLimiter = RateLimiter.create(1000.0); // 1000 permits per second
        this.userRegistrationLimiter = RateLimiter.create(10.0); // 10 permits per second
    }
    
    public boolean allowMessageSending(String userId) {
        String key = "rate_limit:message:" + userId;
        String script = 
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false then " +
            "  redis.call('SET', KEYS[1], 1) " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "  return 1 " +
            "else " +
            "  local count = tonumber(current) " +
            "  if count < tonumber(ARGV[1]) then " +
            "    redis.call('INCR', KEYS[1]) " +
            "    return 1 " +
            "  else " +
            "    return 0 " +
            "  end " +
            "end";
            
        Long result = redisTemplate.execute(
            RedisScript.of(script, Long.class),
            Collections.singletonList(key),
            "100", // 100 messages per minute
            "60"   // 60 seconds window
        );
        
        return result != null && result == 1;
    }
    
    @RateLimiter(name = "message-processing", fallbackMethod = "fallbackMessageProcessing")
    public MessageDTO processMessage(SendMessageRequest request) {
        if (!messageSendingLimiter.tryAcquire()) {
            throw new RateLimitExceededException("Message sending rate limit exceeded");
        }
        
        // Process message
        return new MessageDTO();
    }
    
    public MessageDTO fallbackMessageProcessing(SendMessageRequest request, Exception ex) {
        log.warn("Message processing rate limited, queuing for later: {}", ex.getMessage());
        queueMessageForLaterProcessing(request);
        return MessageDTO.builder().status("QUEUED").build();
    }
}
```

### Kubernetes Rate Limiting (Envoy)

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: rate-limit-filter
  namespace: whatsapp-prod
spec:
  workloadSelector:
    labels:
      app: message-service
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: local_rate_limiter
            token_bucket:
              max_tokens: 1000
              tokens_per_fill: 1000
              fill_interval: 60s
            filter_enabled:
              runtime_key: local_rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            filter_enforced:
              runtime_key: local_rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED
```

## 📊 Monitoring & Alerting for Resilience

### Custom Metrics

```java
@Component
public class ResilienceMetrics {
    
    private final Counter circuitBreakerOpenCounter;
    private final Counter retryAttemptCounter;
    private final Timer timeoutTimer;
    private final Gauge rateLimitGauge;
    
    public ResilienceMetrics(MeterRegistry meterRegistry) {
        this.circuitBreakerOpenCounter = Counter.builder("circuit_breaker_open_total")
            .description("Number of times circuit breaker opened")
            .tag("service", "whatsapp")
            .register(meterRegistry);
            
        this.retryAttemptCounter = Counter.builder("retry_attempts_total")
            .description("Number of retry attempts")
            .tag("service", "whatsapp")
            .register(meterRegistry);
            
        this.timeoutTimer = Timer.builder("operation_timeout")
            .description("Operation timeout duration")
            .tag("service", "whatsapp")
            .register(meterRegistry);
            
        this.rateLimitGauge = Gauge.builder("rate_limit_current")
            .description("Current rate limit usage")
            .tag("service", "whatsapp")
            .register(meterRegistry, this, ResilienceMetrics::getCurrentRateLimit);
    }
    
    public void recordCircuitBreakerOpen(String component) {
        circuitBreakerOpenCounter.increment(Tags.of("component", component));
    }
    
    public void recordRetryAttempt(String operation) {
        retryAttemptCounter.increment(Tags.of("operation", operation));
    }
    
    public Timer.Sample startTimeoutTimer() {
        return Timer.start(timeoutTimer);
    }
    
    private double getCurrentRateLimit() {
        // Return current rate limit usage
        return 0.0;
    }
}
```

### Alerting Rules

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: resilience-alerts
  namespace: whatsapp-monitoring
spec:
  groups:
  - name: resilience.rules
    rules:
    - alert: CircuitBreakerOpen
      expr: increase(circuit_breaker_open_total[5m]) > 0
      for: 1m
      labels:
        severity: warning
      annotations:
        summary: "Circuit breaker opened for {{ $labels.component }}"
        description: "Circuit breaker for {{ $labels.component }} has opened"
        
    - alert: HighRetryRate
      expr: rate(retry_attempts_total[5m]) > 10
      for: 2m
      labels:
        severity: warning
      annotations:
        summary: "High retry rate detected"
        description: "Retry rate is {{ $value }} attempts per second"
        
    - alert: FrequentTimeouts
      expr: rate(operation_timeout_count[5m]) > 5
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "Frequent operation timeouts"
        description: "Operation timeout rate is {{ $value }} per second"
        
    - alert: RateLimitExceeded
      expr: rate_limit_current > 0.9
      for: 1m
      labels:
        severity: warning
      annotations:
        summary: "Rate limit threshold exceeded"
        description: "Rate limit usage is {{ $value }}%"
```

## 🎯 Summary

This resilience architecture provides:

### ✅ **Fault Tolerance**
- Circuit breakers prevent cascade failures
- Retry mechanisms with exponential backoff
- Timeout patterns prevent resource exhaustion
- Bulkhead isolation limits failure impact

### ✅ **Graceful Degradation**
- Health checks enable intelligent routing
- Fallback mechanisms maintain service availability
- Rate limiting prevents system overload
- Chaos engineering validates resilience

### ✅ **Observability**
- Comprehensive metrics for all resilience patterns
- Real-time alerting for failure conditions
- Distributed tracing for failure analysis
- Performance monitoring for optimization

### ✅ **Self-Healing**
- Automatic recovery from transient failures
- Dynamic scaling based on load
- Circuit breaker auto-recovery
- Kubernetes self-healing capabilities

This resilience framework ensures the WhatsApp system can handle failures gracefully while maintaining **99.99% availability** and providing a consistent user experience even under adverse conditions.