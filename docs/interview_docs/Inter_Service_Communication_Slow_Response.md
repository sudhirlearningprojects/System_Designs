# Inter-Service Communication with Slow Response Times - Solutions

## Overview

When services take longer to respond, synchronous communication becomes problematic. This guide covers patterns and implementations to handle slow inter-service communication effectively.

**Common Problems**:
- Request timeouts
- Thread pool exhaustion
- Cascading failures
- Poor user experience
- Resource wastage

---

## Problem Scenario

```
Service A → Service B (takes 30 seconds)
           → Service C (takes 20 seconds)
           → Service D (takes 15 seconds)

Total time: 65 seconds (sequential)
User waits: 65 seconds ❌
```

---

## Solution 1: Asynchronous Communication (Recommended)

### Using CompletableFuture

```java
@RestController
@RequestMapping("/api")
public class AsyncController {
    
    @Autowired
    private ServiceBClient serviceBClient;
    
    @Autowired
    private ServiceCClient serviceCClient;
    
    @Autowired
    private ServiceDClient serviceDClient;
    
    @GetMapping("/data")
    public CompletableFuture<ResponseEntity<AggregatedResponse>> getData() {
        
        // Call all services in parallel
        CompletableFuture<String> futureB = CompletableFuture.supplyAsync(() -> 
            serviceBClient.getData());
        
        CompletableFuture<String> futureC = CompletableFuture.supplyAsync(() -> 
            serviceCClient.getData());
        
        CompletableFuture<String> futureD = CompletableFuture.supplyAsync(() -> 
            serviceDClient.getData());
        
        // Combine results
        return CompletableFuture.allOf(futureB, futureC, futureD)
            .thenApply(v -> {
                AggregatedResponse response = new AggregatedResponse();
                response.setDataB(futureB.join());
                response.setDataC(futureC.join());
                response.setDataD(futureD.join());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> 
                ResponseEntity.status(500).body(new AggregatedResponse()));
    }
}
```

**Result**:
```
Service A → Service B (30s) ┐
         → Service C (20s) ├─ Parallel execution
         → Service D (15s) ┘

Total time: 30 seconds (parallel)
User waits: 30 seconds ✅ (54% improvement)
```

---

## Solution 2: Async with Timeout

### Using CompletableFuture with Timeout

```java
@Service
public class AsyncServiceWithTimeout {
    
    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    
    public AsyncServiceWithTimeout() {
        this.restTemplate = new RestTemplate();
        this.executor = Executors.newFixedThreadPool(10);
    }
    
    public CompletableFuture<String> callServiceWithTimeout(
            String url, long timeoutSeconds) {
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
                throw new RuntimeException("Service call failed", e);
            }
        }, executor);
        
        // Add timeout
        return future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    return "TIMEOUT";
                }
                return "ERROR: " + ex.getMessage();
            });
    }
}
```

### Usage

```java
@RestController
public class TimeoutController {
    
    @Autowired
    private AsyncServiceWithTimeout asyncService;
    
    @GetMapping("/data")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getData() {
        
        CompletableFuture<String> futureB = 
            asyncService.callServiceWithTimeout("http://service-b/data", 5);
        
        CompletableFuture<String> futureC = 
            asyncService.callServiceWithTimeout("http://service-c/data", 5);
        
        CompletableFuture<String> futureD = 
            asyncService.callServiceWithTimeout("http://service-d/data", 5);
        
        return CompletableFuture.allOf(futureB, futureC, futureD)
            .thenApply(v -> {
                Map<String, String> result = new HashMap<>();
                result.put("serviceB", futureB.join());
                result.put("serviceC", futureC.join());
                result.put("serviceD", futureD.join());
                return ResponseEntity.ok(result);
            });
    }
}
```

**Result**:
```
Each service has 5-second timeout
If service takes > 5s → Returns "TIMEOUT"
Total max time: 5 seconds ✅
```

---

## Solution 3: WebClient (Reactive - Spring WebFlux)

### Non-Blocking Reactive Approach

```java
@Service
public class ReactiveServiceClient {
    
    private final WebClient webClient;
    
    public ReactiveServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    public Mono<String> callServiceB() {
        return webClient.get()
            .uri("http://service-b/data")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn("Service B unavailable");
    }
    
    public Mono<String> callServiceC() {
        return webClient.get()
            .uri("http://service-c/data")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn("Service C unavailable");
    }
    
    public Mono<String> callServiceD() {
        return webClient.get()
            .uri("http://service-d/data")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn("Service D unavailable");
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api")
public class ReactiveController {
    
    @Autowired
    private ReactiveServiceClient serviceClient;
    
    @GetMapping("/data")
    public Mono<ResponseEntity<Map<String, String>>> getData() {
        
        Mono<String> monoB = serviceClient.callServiceB();
        Mono<String> monoC = serviceClient.callServiceC();
        Mono<String> monoD = serviceClient.callServiceD();
        
        return Mono.zip(monoB, monoC, monoD)
            .map(tuple -> {
                Map<String, String> result = new HashMap<>();
                result.put("serviceB", tuple.getT1());
                result.put("serviceC", tuple.getT2());
                result.put("serviceD", tuple.getT3());
                return ResponseEntity.ok(result);
            });
    }
}
```

**Benefits**:
- Non-blocking I/O
- Better resource utilization
- Handles backpressure
- Scales to millions of requests

---

## Solution 4: Message Queue (Asynchronous Processing)

### Request-Response Pattern with Kafka

```java
// Producer: Service A sends request
@Service
public class AsyncRequestService {
    
    @Autowired
    private KafkaTemplate<String, Request> kafkaTemplate;
    
    @Autowired
    private ResponseRepository responseRepository;
    
    public String submitRequest(Request request) {
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);
        
        // Send to Kafka
        kafkaTemplate.send("service-requests", requestId, request);
        
        // Return request ID immediately
        return requestId;
    }
    
    public Response getResponse(String requestId) {
        return responseRepository.findByRequestId(requestId);
    }
}

// Consumer: Service B processes request
@Service
public class AsyncRequestProcessor {
    
    @KafkaListener(topics = "service-requests", groupId = "processors")
    public void processRequest(Request request) {
        // Process request (takes 30 seconds)
        Response response = heavyProcessing(request);
        
        // Save response
        responseRepository.save(response);
        
        // Optionally send to response topic
        kafkaTemplate.send("service-responses", request.getRequestId(), response);
    }
    
    private Response heavyProcessing(Request request) {
        // Simulate slow processing
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {}
        return new Response(request.getRequestId(), "Processed");
    }
}
```

### API Endpoints

```java
@RestController
@RequestMapping("/api")
public class AsyncApiController {
    
    @Autowired
    private AsyncRequestService requestService;
    
    // Submit request (returns immediately)
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitRequest(@RequestBody Request request) {
        String requestId = requestService.submitRequest(request);
        
        Map<String, String> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("status", "PROCESSING");
        result.put("statusUrl", "/api/status/" + requestId);
        
        return ResponseEntity.accepted().body(result);
    }
    
    // Check status
    @GetMapping("/status/{requestId}")
    public ResponseEntity<Response> getStatus(@PathVariable String requestId) {
        Response response = requestService.getResponse(requestId);
        
        if (response == null) {
            return ResponseEntity.status(202).build(); // Still processing
        }
        
        return ResponseEntity.ok(response);
    }
}
```

**Flow**:
```
1. Client → POST /api/submit → Returns requestId immediately
2. Client → GET /api/status/{requestId} → Polls for result
3. Service processes in background
4. Client gets result when ready
```

---

## Solution 5: Circuit Breaker Pattern

### Using Resilience4j

```java
@Service
public class ResilientServiceClient {
    
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    
    public ResilientServiceClient() {
        this.restTemplate = new RestTemplate();
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open if 50% fail
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker("serviceB");
    }
    
    public String callServiceB() {
        return circuitBreaker.executeSupplier(() -> {
            try {
                return restTemplate.getForObject(
                    "http://service-b/data", String.class);
            } catch (Exception e) {
                throw new RuntimeException("Service B failed", e);
            }
        });
    }
}
```

### With Fallback

```java
@Service
public class ServiceClientWithFallback {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "serviceB", fallbackMethod = "fallbackServiceB")
    @Retry(name = "serviceB", fallbackMethod = "fallbackServiceB")
    @TimeLimiter(name = "serviceB")
    public CompletableFuture<String> callServiceB() {
        return CompletableFuture.supplyAsync(() -> 
            restTemplate.getForObject("http://service-b/data", String.class));
    }
    
    public CompletableFuture<String> fallbackServiceB(Exception e) {
        return CompletableFuture.completedFuture("Service B unavailable - using cached data");
    }
}
```

**Configuration**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      serviceB:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
  
  retry:
    instances:
      serviceB:
        max-attempts: 3
        wait-duration: 1s
  
  timelimiter:
    instances:
      serviceB:
        timeout-duration: 5s
```

---

## Solution 6: Caching

### Cache Slow Service Responses

```java
@Service
public class CachedServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Cacheable(value = "serviceB", key = "#userId", unless = "#result == null")
    public String getDataFromServiceB(String userId) {
        // This call takes 30 seconds
        return restTemplate.getForObject(
            "http://service-b/data?userId=" + userId, String.class);
    }
    
    @CacheEvict(value = "serviceB", key = "#userId")
    public void invalidateCache(String userId) {
        // Manually invalidate cache
    }
}
```

**Configuration**:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("serviceB");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }
}
```

**Result**:
```
First call: 30 seconds (cache miss)
Subsequent calls: <1ms (cache hit) ✅
Cache expires after 10 minutes
```

---

## Solution 7: Webhook/Callback Pattern

### Service A Provides Callback URL

```java
@RestController
@RequestMapping("/api")
public class CallbackController {
    
    @Autowired
    private ServiceBClient serviceBClient;
    
    @Autowired
    private ResponseRepository responseRepository;
    
    // Initiate request with callback
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> initiateRequest(@RequestBody Request request) {
        String requestId = UUID.randomUUID().toString();
        String callbackUrl = "http://service-a/api/callback/" + requestId;
        
        // Send request to Service B with callback URL
        serviceBClient.processAsync(request, callbackUrl);
        
        Map<String, String> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("status", "PROCESSING");
        
        return ResponseEntity.accepted().body(result);
    }
    
    // Callback endpoint
    @PostMapping("/callback/{requestId}")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String requestId,
            @RequestBody Response response) {
        
        // Save response
        response.setRequestId(requestId);
        responseRepository.save(response);
        
        // Optionally notify client (WebSocket, SSE, etc.)
        notifyClient(requestId, response);
        
        return ResponseEntity.ok().build();
    }
    
    // Get result
    @GetMapping("/result/{requestId}")
    public ResponseEntity<Response> getResult(@PathVariable String requestId) {
        Response response = responseRepository.findByRequestId(requestId);
        
        if (response == null) {
            return ResponseEntity.status(202).build();
        }
        
        return ResponseEntity.ok(response);
    }
}
```

### Service B Implementation

```java
@Service
public class ServiceBProcessor {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Async
    public void processAsync(Request request, String callbackUrl) {
        // Process request (takes 30 seconds)
        Response response = heavyProcessing(request);
        
        // Call back to Service A
        restTemplate.postForObject(callbackUrl, response, Void.class);
    }
    
    private Response heavyProcessing(Request request) {
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {}
        return new Response("Processed");
    }
}
```

---

## Solution 8: Server-Sent Events (SSE)

### Real-Time Updates to Client

```java
@RestController
@RequestMapping("/api")
public class SSEController {
    
    @Autowired
    private AsyncRequestService requestService;
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitRequest(@RequestBody Request request) {
        String requestId = UUID.randomUUID().toString();
        
        // Submit async request
        requestService.submitRequest(requestId, request, (response) -> {
            // Callback when processing complete
            SseEmitter emitter = emitters.get(requestId);
            if (emitter != null) {
                try {
                    emitter.send(response);
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        });
        
        Map<String, String> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("streamUrl", "/api/stream/" + requestId);
        
        return ResponseEntity.accepted().body(result);
    }
    
    @GetMapping("/stream/{requestId}")
    public SseEmitter streamResult(@PathVariable String requestId) {
        SseEmitter emitter = new SseEmitter(60000L); // 60 second timeout
        emitters.put(requestId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(requestId));
        emitter.onTimeout(() -> emitters.remove(requestId));
        
        return emitter;
    }
}
```

**Client Usage**:
```javascript
// JavaScript client
const eventSource = new EventSource('/api/stream/' + requestId);

eventSource.onmessage = function(event) {
    const response = JSON.parse(event.data);
    console.log('Received:', response);
    eventSource.close();
};
```

---

## Solution 9: Batch Processing

### Aggregate Multiple Requests

```java
@Service
public class BatchProcessor {
    
    private final List<Request> batch = new ArrayList<>();
    private final ScheduledExecutorService scheduler = 
        Executors.newSingleThreadScheduledExecutor();
    
    public BatchProcessor() {
        // Process batch every 5 seconds
        scheduler.scheduleAtFixedRate(this::processBatch, 5, 5, TimeUnit.SECONDS);
    }
    
    public synchronized CompletableFuture<Response> addRequest(Request request) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        request.setFuture(future);
        batch.add(request);
        return future;
    }
    
    private synchronized void processBatch() {
        if (batch.isEmpty()) return;
        
        List<Request> currentBatch = new ArrayList<>(batch);
        batch.clear();
        
        // Process all requests in single call
        List<Response> responses = serviceBClient.processBatch(currentBatch);
        
        // Complete futures
        for (int i = 0; i < currentBatch.size(); i++) {
            currentBatch.get(i).getFuture().complete(responses.get(i));
        }
    }
}
```

**Result**:
```
Individual calls: 100 requests × 30s = 3000s
Batch call: 1 batch × 30s = 30s ✅ (99% improvement)
```

---

## Comparison Table

| Solution | Response Time | Complexity | Use Case |
|----------|--------------|------------|----------|
| **CompletableFuture** | Medium | Low | Parallel calls |
| **WebClient** | Low | Medium | Reactive apps |
| **Message Queue** | Async | High | Decoupled systems |
| **Circuit Breaker** | Medium | Medium | Fault tolerance |
| **Caching** | Very Low | Low | Repeated requests |
| **Webhook** | Async | Medium | Long-running tasks |
| **SSE** | Real-time | Medium | Live updates |
| **Batch** | Low | Medium | Multiple similar requests |

---

## Best Practices

### 1. Set Appropriate Timeouts

```java
RestTemplate restTemplate = new RestTemplate();
HttpComponentsClientHttpRequestFactory factory = 
    new HttpComponentsClientHttpRequestFactory();
factory.setConnectTimeout(5000); // 5 seconds
factory.setReadTimeout(10000); // 10 seconds
restTemplate.setRequestFactory(factory);
```

### 2. Use Connection Pooling

```java
PoolingHttpClientConnectionManager connectionManager = 
    new PoolingHttpClientConnectionManager();
connectionManager.setMaxTotal(100);
connectionManager.setDefaultMaxPerRoute(20);

CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(connectionManager)
    .build();
```

### 3. Implement Retry Logic

```java
@Retry(name = "serviceB", fallbackMethod = "fallback")
public String callService() {
    return restTemplate.getForObject("http://service-b/data", String.class);
}
```

### 4. Monitor Performance

```java
@Timed(value = "service.call", description = "Time taken to call service")
public String callService() {
    return restTemplate.getForObject("http://service-b/data", String.class);
}
```

---

## Key Takeaways

1. **Use async communication** for slow services
2. **Parallel execution** reduces total time
3. **Set timeouts** to prevent hanging
4. **Circuit breaker** prevents cascading failures
5. **Caching** eliminates repeated slow calls
6. **Message queues** for decoupled async processing
7. **Webhooks/callbacks** for long-running tasks
8. **Batch processing** for multiple similar requests
9. **Monitor and measure** service performance
10. **Choose pattern** based on requirements

---

## Practice Problems

1. Implement async API with CompletableFuture
2. Add circuit breaker to service calls
3. Implement request-response with Kafka
4. Create caching layer for slow service
5. Build webhook-based async processing
6. Implement SSE for real-time updates
7. Design batch processing system
8. Add retry logic with exponential backoff
9. Monitor service call performance
10. Handle timeout and fallback scenarios
