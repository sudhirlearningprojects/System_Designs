# Bulkhead Pattern - Deep Dive

## Table of Contents
1. [Introduction](#introduction)
2. [Theory and Concepts](#theory-and-concepts)
3. [Types of Bulkheads](#types-of-bulkheads)
4. [Implementation Patterns](#implementation-patterns)
5. [Practical Examples](#practical-examples)
6. [Best Practices](#best-practices)
7. [Common Pitfalls](#common-pitfalls)

---

## Introduction

The **Bulkhead Pattern** is a design pattern that isolates resources to prevent cascading failures. Named after ship bulkheads that prevent water from flooding the entire vessel, this pattern partitions resources so that failure in one area doesn't bring down the entire system.

### Why Bulkhead Pattern?

In distributed systems:
- One slow service can exhaust all threads
- Resource contention affects all operations
- Cascading failures spread across services
- No isolation between critical and non-critical operations

**Bulkhead Pattern solves this by:**
- Isolating resources into separate pools
- Limiting blast radius of failures
- Protecting critical operations
- Preventing resource exhaustion

---

## Theory and Concepts

### The Ship Analogy

```
┌─────────────────────────────────┐
│         Ship (System)           │
├──────┬──────┬──────┬──────┬─────┤
│ Pool │ Pool │ Pool │ Pool │Pool │
│  A   │  B   │  C   │  D   │ E  │
│      │      │ 💧💧 │      │     │
└──────┴──────┴──────┴──────┴─────┘
         ↑
    If Pool C floods,
    others remain safe
```

### Core Principles

1. **Resource Isolation**: Separate resource pools for different operations
2. **Failure Containment**: Failures don't spread across pools
3. **Priority Management**: Critical operations get dedicated resources
4. **Graceful Degradation**: System continues with reduced capacity

### Key Metrics

- **Pool Size**: Number of resources per pool
- **Queue Capacity**: Waiting requests per pool
- **Timeout**: Maximum wait time for resource
- **Rejection Policy**: What to do when pool is full

---

## Types of Bulkheads

### 1. Thread Pool Bulkhead

Separate thread pools for different operations.

```
┌─────────────────────────────────┐
│      Application Threads        │
├──────────┬──────────┬───────────┤
│ Payment  │  Search  │  Reports  │
│ Pool     │  Pool    │  Pool     │
│ (10)     │  (20)    │  (5)      │
└──────────┴──────────┴───────────┘
```

### 2. Semaphore Bulkhead

Limit concurrent executions using semaphores.

```
┌─────────────────────────────────┐
│      Semaphore Limits           │
├──────────┬──────────┬───────────┤
│ Payment  │  Search  │  Reports  │
│ Max: 10  │  Max: 50 │  Max: 5   │
└──────────┴──────────┴───────────┘
```

### 3. Connection Pool Bulkhead

Separate database connection pools.

```
┌─────────────────────────────────┐
│    Database Connections         │
├──────────┬──────────┬───────────┤
│ OLTP     │  OLAP    │  Batch    │
│ Pool     │  Pool    │  Pool     │
│ (50)     │  (20)    │  (10)     │
└──────────┴──────────┴───────────┘
```

---

## Implementation Patterns

### Pattern 1: Basic Thread Pool Bulkhead

```java
@Configuration
public class BulkheadConfig {
    
    @Bean
    public ExecutorService paymentThreadPool() {
        return new ThreadPoolExecutor(
            5,                              // corePoolSize
            10,                             // maximumPoolSize
            60L,                            // keepAliveTime
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(25),   // workQueue
            new ThreadPoolExecutor.CallerRunsPolicy() // rejectionPolicy
        );
    }
    
    @Bean
    public ExecutorService searchThreadPool() {
        return new ThreadPoolExecutor(
            10,
            20,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
    
    @Bean
    public ExecutorService reportThreadPool() {
        return new ThreadPoolExecutor(
            2,
            5,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10),
            new ThreadPoolExecutor.DiscardOldestPolicy()
        );
    }
}

@Service
public class PaymentService {
    
    @Autowired
    @Qualifier("paymentThreadPool")
    private ExecutorService paymentThreadPool;
    
    public CompletableFuture<Payment> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Process payment
            return executePayment(request);
        }, paymentThreadPool);
    }
}
```

### Pattern 2: Resilience4j Bulkhead

```java
@Configuration
public class Resilience4jBulkheadConfig {
    
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        // Thread Pool Bulkhead
        ThreadPoolBulkheadConfig threadPoolConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(10)
            .coreThreadPoolSize(5)
            .queueCapacity(25)
            .keepAliveDuration(Duration.ofMillis(1000))
            .build();
        
        // Semaphore Bulkhead
        BulkheadConfig semaphoreConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(25)
            .maxWaitDuration(Duration.ofMillis(500))
            .build();
        
        return BulkheadRegistry.of(Map.of(
            "payment", threadPoolConfig,
            "search", semaphoreConfig
        ));
    }
}

@Service
public class OrderService {
    
    private final Bulkhead bulkhead;
    
    public OrderService(BulkheadRegistry registry) {
        this.bulkhead = registry.bulkhead("orderService");
    }
    
    public Order createOrder(OrderRequest request) {
        return Bulkhead.decorateSupplier(bulkhead, () -> 
            executeOrderCreation(request)
        ).get();
    }
}
```

### Pattern 3: Annotation-Based Bulkhead

```java
@Service
public class ProductService {
    
    @Bulkhead(name = "productService", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Product> getProduct(String productId) {
        return CompletableFuture.supplyAsync(() -> 
            productRepository.findById(productId)
        );
    }
    
    @Bulkhead(name = "searchService", type = Bulkhead.Type.SEMAPHORE)
    public List<Product> searchProducts(String query) {
        return productRepository.search(query);
    }
}

// Configuration
@Configuration
public class BulkheadConfiguration {
    
    @Bean
    public ThreadPoolBulkheadConfig productBulkheadConfig() {
        return ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(10)
            .coreThreadPoolSize(5)
            .queueCapacity(20)
            .build();
    }
    
    @Bean
    public BulkheadConfig searchBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(50)
            .maxWaitDuration(Duration.ofMillis(100))
            .build();
    }
}
```

### Pattern 4: Database Connection Pool Bulkhead

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.oltp")
    public DataSource oltpDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("OLTP-Pool");
        return new HikariDataSource(config);
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.olap")
    public DataSource olapDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(60000);
        config.setPoolName("OLAP-Pool");
        return new HikariDataSource(config);
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.batch")
    public DataSource batchDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(120000);
        config.setPoolName("Batch-Pool");
        return new HikariDataSource(config);
    }
}

@Service
public class TransactionService {
    
    @Autowired
    @Qualifier("oltpDataSource")
    private DataSource oltpDataSource;
    
    @Autowired
    @Qualifier("olapDataSource")
    private DataSource olapDataSource;
    
    public Transaction createTransaction(TransactionRequest request) {
        try (Connection conn = oltpDataSource.getConnection()) {
            // Fast transactional operations
            return executeTransaction(conn, request);
        }
    }
    
    public Report generateReport(ReportRequest request) {
        try (Connection conn = olapDataSource.getConnection()) {
            // Long-running analytical queries
            return executeReport(conn, request);
        }
    }
}
```

### Pattern 5: HTTP Client Bulkhead

```java
@Configuration
public class HttpClientBulkheadConfig {
    
    @Bean
    public RestTemplate paymentRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(10);
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        
        return new RestTemplate(factory);
    }
    
    @Bean
    public RestTemplate searchRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
        
        factory.setHttpClient(httpClient);
        
        return new RestTemplate(factory);
    }
}
```

---

## Practical Examples

### Example 1: E-Commerce Service Isolation

```java
@Service
public class EcommerceService {
    
    private final ExecutorService checkoutPool;
    private final ExecutorService browsingPool;
    private final ExecutorService recommendationPool;
    
    public EcommerceService() {
        // Critical: Checkout operations
        this.checkoutPool = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // High volume: Browsing operations
        this.browsingPool = new ThreadPoolExecutor(
            20, 50, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200),
            new ThreadPoolExecutor.AbortPolicy()
        );
        
        // Non-critical: Recommendations
        this.recommendationPool = new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.DiscardPolicy()
        );
    }
    
    public CompletableFuture<Order> checkout(CheckoutRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Critical operation - dedicated pool
            return processCheckout(request);
        }, checkoutPool);
    }
    
    public CompletableFuture<List<Product>> browseProducts(String category) {
        return CompletableFuture.supplyAsync(() -> {
            // High volume operation - larger pool
            return fetchProducts(category);
        }, browsingPool);
    }
    
    public CompletableFuture<List<Product>> getRecommendations(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            // Non-critical - can be dropped if overloaded
            return fetchRecommendations(userId);
        }, recommendationPool)
        .exceptionally(ex -> Collections.emptyList());
    }
}
```

### Example 2: Multi-Tenant Resource Isolation

```java
@Service
public class MultiTenantService {
    
    private final Map<String, ExecutorService> tenantPools = new ConcurrentHashMap<>();
    
    public ExecutorService getTenantPool(String tenantId) {
        return tenantPools.computeIfAbsent(tenantId, id -> {
            TenantConfig config = getTenantConfig(id);
            
            return new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(config.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        });
    }
    
    public CompletableFuture<Result> executeForTenant(String tenantId, Callable<Result> task) {
        ExecutorService pool = getTenantPool(tenantId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, pool);
    }
}
```

### Example 3: API Rate Limiting with Bulkhead

```java
@Service
public class RateLimitedService {
    
    private final Bulkhead freeTierBulkhead;
    private final Bulkhead premiumTierBulkhead;
    private final Bulkhead enterpriseTierBulkhead;
    
    public RateLimitedService(BulkheadRegistry registry) {
        // Free tier: Limited resources
        BulkheadConfig freeConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(10)
            .maxWaitDuration(Duration.ofMillis(100))
            .build();
        this.freeTierBulkhead = registry.bulkhead("free", freeConfig);
        
        // Premium tier: More resources
        BulkheadConfig premiumConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(50)
            .maxWaitDuration(Duration.ofMillis(500))
            .build();
        this.premiumTierBulkhead = registry.bulkhead("premium", premiumConfig);
        
        // Enterprise tier: Maximum resources
        BulkheadConfig enterpriseConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(200)
            .maxWaitDuration(Duration.ofSeconds(2))
            .build();
        this.enterpriseTierBulkhead = registry.bulkhead("enterprise", enterpriseConfig);
    }
    
    public Response processRequest(Request request, UserTier tier) {
        Bulkhead bulkhead = selectBulkhead(tier);
        
        return Bulkhead.decorateSupplier(bulkhead, () -> 
            executeRequest(request)
        ).get();
    }
    
    private Bulkhead selectBulkhead(UserTier tier) {
        switch (tier) {
            case FREE: return freeTierBulkhead;
            case PREMIUM: return premiumTierBulkhead;
            case ENTERPRISE: return enterpriseTierBulkhead;
            default: return freeTierBulkhead;
        }
    }
}
```

### Example 4: Microservices Communication Bulkhead

```java
@Service
public class ServiceCommunicationBulkhead {
    
    private final Map<String, Bulkhead> serviceBulkheads;
    private final WebClient.Builder webClientBuilder;
    
    public ServiceCommunicationBulkhead(BulkheadRegistry registry) {
        this.serviceBulkheads = Map.of(
            "user-service", registry.bulkhead("user-service"),
            "order-service", registry.bulkhead("order-service"),
            "payment-service", registry.bulkhead("payment-service"),
            "inventory-service", registry.bulkhead("inventory-service")
        );
        this.webClientBuilder = WebClient.builder();
    }
    
    public <T> Mono<T> callService(String serviceName, String path, Class<T> responseType) {
        Bulkhead bulkhead = serviceBulkheads.get(serviceName);
        
        return Mono.fromCallable(() -> 
            Bulkhead.decorateSupplier(bulkhead, () -> 
                webClientBuilder.build()
                    .get()
                    .uri("http://" + serviceName + path)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block()
            ).get()
        );
    }
}
```

---

## Best Practices

### 1. Size Pools Appropriately

```java
@Configuration
public class PoolSizingConfig {
    
    @Bean
    public ExecutorService cpuBoundPool() {
        // CPU-bound: cores + 1
        int poolSize = Runtime.getRuntime().availableProcessors() + 1;
        return Executors.newFixedThreadPool(poolSize);
    }
    
    @Bean
    public ExecutorService ioBoundPool() {
        // I/O-bound: cores * 2 (or more)
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        return new ThreadPoolExecutor(
            poolSize,
            poolSize * 2,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100)
        );
    }
}
```

### 2. Monitor Bulkhead Metrics

```java
@Component
public class BulkheadMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onBulkheadEvent(BulkheadEvent event) {
        String bulkheadName = event.getBulkheadName();
        
        if (event instanceof BulkheadOnCallPermittedEvent) {
            meterRegistry.counter("bulkhead.calls.permitted", 
                "name", bulkheadName).increment();
        } else if (event instanceof BulkheadOnCallRejectedEvent) {
            meterRegistry.counter("bulkhead.calls.rejected", 
                "name", bulkheadName).increment();
        }
    }
    
    @Scheduled(fixedRate = 5000)
    public void recordBulkheadMetrics() {
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            Bulkhead.Metrics metrics = bulkhead.getMetrics();
            
            meterRegistry.gauge("bulkhead.available.concurrent.calls",
                Tags.of("name", bulkhead.getName()),
                metrics.getAvailableConcurrentCalls());
            
            meterRegistry.gauge("bulkhead.max.allowed.concurrent.calls",
                Tags.of("name", bulkhead.getName()),
                metrics.getMaxAllowedConcurrentCalls());
        });
    }
}
```

### 3. Implement Graceful Degradation

```java
@Service
public class GracefulDegradationService {
    
    @Bulkhead(name = "primaryService", fallbackMethod = "fallbackMethod")
    public Response processRequest(Request request) {
        return primaryService.process(request);
    }
    
    private Response fallbackMethod(Request request, BulkheadFullException ex) {
        log.warn("Bulkhead full, using degraded service");
        
        // Return cached data
        Response cached = cache.get(request.getId());
        if (cached != null) {
            cached.setFromCache(true);
            return cached;
        }
        
        // Return minimal response
        return Response.builder()
            .status("DEGRADED")
            .message("Service temporarily overloaded")
            .build();
    }
}
```

### 4. Use Appropriate Rejection Policies

```java
public class RejectionPolicyExamples {
    
    // Abort: Throw exception
    public ExecutorService abortPolicy() {
        return new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.AbortPolicy() // Throws RejectedExecutionException
        );
    }
    
    // Caller Runs: Execute in caller's thread
    public ExecutorService callerRunsPolicy() {
        return new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.CallerRunsPolicy() // Slows down caller
        );
    }
    
    // Discard: Silently drop task
    public ExecutorService discardPolicy() {
        return new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.DiscardPolicy() // Drops task
        );
    }
    
    // Discard Oldest: Drop oldest task in queue
    public ExecutorService discardOldestPolicy() {
        return new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.DiscardOldestPolicy() // Drops oldest
        );
    }
}
```

### 5. Combine with Circuit Breaker

```java
@Service
public class ResilientService {
    
    @Bulkhead(name = "externalService", type = Bulkhead.Type.THREADPOOL)
    @CircuitBreaker(name = "externalService")
    @Retry(name = "externalService")
    public CompletableFuture<Response> callExternalService(Request request) {
        return CompletableFuture.supplyAsync(() -> 
            externalServiceClient.call(request)
        );
    }
}
```

---

## Common Pitfalls

### 1. ❌ Sharing Thread Pools

**Problem:**
```java
// BAD: All operations share same pool
@Bean
public ExecutorService sharedPool() {
    return Executors.newFixedThreadPool(10);
}

// Critical and non-critical operations compete for threads
paymentService.process(payment); // Critical
recommendationService.fetch(userId); // Non-critical
```

**Solution:**
```java
// GOOD: Separate pools
@Bean
public ExecutorService criticalPool() {
    return Executors.newFixedThreadPool(10);
}

@Bean
public ExecutorService nonCriticalPool() {
    return Executors.newFixedThreadPool(5);
}
```

### 2. ❌ Unbounded Queues

**Problem:**
```java
// BAD: Unbounded queue can cause OOM
new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>() // Unbounded!
);
```

**Solution:**
```java
// GOOD: Bounded queue
new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100) // Bounded
);
```

### 3. ❌ Wrong Pool Size

**Problem:**
```java
// BAD: Too small for I/O operations
ExecutorService ioPool = Executors.newFixedThreadPool(2);
```

**Solution:**
```java
// GOOD: Appropriate size for I/O
int cores = Runtime.getRuntime().availableProcessors();
ExecutorService ioPool = Executors.newFixedThreadPool(cores * 2);
```

### 4. ❌ Not Monitoring Bulkheads

**Problem:**
```java
// BAD: No visibility into bulkhead state
// Bulkhead fills up and no one knows
```

**Solution:**
```java
// GOOD: Monitor and alert
@Scheduled(fixedRate = 60000)
public void checkBulkheadHealth() {
    bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
        Metrics metrics = bulkhead.getMetrics();
        int available = metrics.getAvailableConcurrentCalls();
        int max = metrics.getMaxAllowedConcurrentCalls();
        
        if (available < max * 0.2) { // Less than 20% available
            alertService.warn("Bulkhead " + bulkhead.getName() + " is 80% full");
        }
    });
}
```

### 5. ❌ Forgetting to Shutdown Pools

**Problem:**
```java
// BAD: Pool never shutdown
ExecutorService pool = Executors.newFixedThreadPool(10);
// Application exits but threads keep running
```

**Solution:**
```java
// GOOD: Proper shutdown
@PreDestroy
public void shutdown() {
    pool.shutdown();
    try {
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            pool.shutdownNow();
        }
    } catch (InterruptedException e) {
        pool.shutdownNow();
    }
}
```

---

## Real-World Examples

### Netflix
- Separate thread pools for each dependency
- Hystrix uses bulkhead pattern by default
- Prevents one slow service from affecting others

### Amazon
- Separate connection pools for different services
- Critical services get dedicated resources
- Non-critical services share smaller pools

### Twitter
- Isolates timeline generation from tweet posting
- Separate pools for read and write operations
- Protects core functionality during load spikes

---

## Comparison with Other Patterns

| Pattern | Purpose | Resource Type |
|---------|---------|---------------|
| **Bulkhead** | Isolate resources | Threads, connections |
| **Circuit Breaker** | Prevent cascading failures | N/A |
| **Rate Limiter** | Control request rate | Requests |
| **Timeout** | Prevent hanging | Time |

---

## Conclusion

Bulkhead Pattern is essential for building resilient systems. Key takeaways:

- **Isolate resources** into separate pools
- **Protect critical operations** with dedicated resources
- **Size pools appropriately** based on workload
- **Monitor bulkhead metrics** and alert on saturation
- **Combine with other patterns** (circuit breaker, retry)
- **Use bounded queues** to prevent memory issues
- **Implement graceful degradation** when pools are full

Remember: Bulkheads prevent one failure from sinking the entire ship!
