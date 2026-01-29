# Java Virtual Threads - Deep Dive (Java 21 to 25)

## Overview

**Virtual Threads** (Project Loom) are lightweight threads introduced in Java 21 that enable high-throughput concurrent applications with simple thread-per-request programming model.

**Key Benefits**:
- Millions of threads (vs thousands of platform threads)
- Low memory footprint (~1KB vs ~1MB)
- Simple blocking code (no async/await complexity)
- Better resource utilization
- Backward compatible with existing code

---

## Platform Threads vs Virtual Threads

### Comparison Table

| Feature | Platform Threads | Virtual Threads |
|---------|-----------------|-----------------|
| **Memory** | ~1MB per thread | ~1KB per thread |
| **Max Threads** | ~10,000 | Millions |
| **Scheduling** | OS scheduler | JVM scheduler |
| **Creation Cost** | Expensive | Cheap |
| **Context Switch** | Expensive | Cheap |
| **Blocking** | Blocks OS thread | Unmounts from carrier |
| **Use Case** | CPU-intensive | I/O-intensive |

---

## How Virtual Threads Work

### Architecture

```
┌─────────────────────────────────────────┐
│         Virtual Threads (Millions)       │
│  VT1  VT2  VT3  VT4  VT5  VT6  VT7 ...  │
└─────────────────────────────────────────┘
              ↓ Mounted on
┌─────────────────────────────────────────┐
│      Carrier Threads (Platform)          │
│        CT1    CT2    CT3    CT4          │
└─────────────────────────────────────────┘
              ↓ Scheduled by
┌─────────────────────────────────────────┐
│           OS Threads (Kernel)            │
└─────────────────────────────────────────┘
```

### Key Concepts

1. **Carrier Thread**: Platform thread that executes virtual thread code
2. **Mounting**: Virtual thread runs on carrier thread
3. **Unmounting**: Virtual thread releases carrier thread (during blocking I/O)
4. **Work-Stealing**: ForkJoinPool schedules virtual threads

---

## Creating Virtual Threads

### Method 1: Thread.startVirtualThread()

```java
public class VirtualThreadExample {
    
    public static void main(String[] args) {
        // Create and start virtual thread
        Thread vThread = Thread.startVirtualThread(() -> {
            System.out.println("Running in: " + Thread.currentThread());
            System.out.println("Is virtual: " + Thread.currentThread().isVirtual());
        });
        
        vThread.join();
    }
}
```

**Output**:
```
Running in: VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1
Is virtual: true
```

---

### Method 2: Thread.ofVirtual()

```java
public class VirtualThreadBuilder {
    
    public static void main(String[] args) throws InterruptedException {
        Thread vThread = Thread.ofVirtual()
            .name("my-virtual-thread")
            .start(() -> {
                System.out.println("Virtual thread: " + Thread.currentThread().getName());
            });
        
        vThread.join();
    }
}
```

**Output**:
```
Virtual thread: my-virtual-thread
```

---

### Method 3: Executors.newVirtualThreadPerTaskExecutor()

```java
public class VirtualThreadExecutor {
    
    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit 10,000 tasks
            for (int i = 0; i < 10000; i++) {
                int taskId = i;
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " on " + Thread.currentThread());
                    Thread.sleep(1000);
                    return taskId;
                });
            }
            
        } // Auto-shutdown with try-with-resources
    }
}
```

---

## Real-World Examples

### Example 1: HTTP Server (Before vs After)

#### Before (Platform Threads - Limited Scalability)

```java
@RestController
public class OrderController {
    
    // Thread pool with 200 threads
    private final ExecutorService executor = Executors.newFixedThreadPool(200);
    
    @GetMapping("/orders/{id}")
    public CompletableFuture<Order> getOrder(@PathVariable String id) {
        // Async to avoid blocking
        return CompletableFuture.supplyAsync(() -> {
            // Blocking I/O
            Order order = orderService.findById(id);
            User user = userService.findById(order.getUserId());
            Payment payment = paymentService.findById(order.getPaymentId());
            
            order.setUser(user);
            order.setPayment(payment);
            return order;
        }, executor);
    }
}
```

**Limitations**:
- Max 200 concurrent requests
- Complex async code
- Thread pool management overhead

---

#### After (Virtual Threads - Millions of Requests)

```java
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        // Simple blocking code - scales to millions!
        Order order = orderService.findById(id);
        User user = userService.findById(order.getUserId());
        Payment payment = paymentService.findById(order.getPaymentId());
        
        order.setUser(user);
        order.setPayment(payment);
        return order;
    }
}

// Spring Boot 3.2+ Configuration
@Configuration
public class VirtualThreadConfig {
    
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}
```

**Benefits**:
- Handles millions of concurrent requests
- Simple blocking code
- No thread pool management

---

### Example 2: Parallel API Calls

```java
public class ParallelApiCalls {
    
    public UserProfile getUserProfile(String userId) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Launch parallel calls
            Future<User> userFuture = executor.submit(() -> 
                userService.getUser(userId));
            
            Future<List<Order>> ordersFuture = executor.submit(() -> 
                orderService.getOrders(userId));
            
            Future<List<Review>> reviewsFuture = executor.submit(() -> 
                reviewService.getReviews(userId));
            
            Future<Preferences> prefFuture = executor.submit(() -> 
                preferencesService.getPreferences(userId));
            
            // Wait for all (blocks virtual thread, not carrier thread)
            User user = userFuture.get();
            List<Order> orders = ordersFuture.get();
            List<Review> reviews = reviewsFuture.get();
            Preferences prefs = prefFuture.get();
            
            return new UserProfile(user, orders, reviews, prefs);
        }
    }
}
```

**Performance**:
```
Platform Threads: 200ms (sequential) or complex async code
Virtual Threads: 50ms (parallel) with simple blocking code
```

---

### Example 3: Database Connection Pool

```java
@Service
public class OrderService {
    
    @Autowired
    private DataSource dataSource;
    
    public List<Order> processOrders(List<String> orderIds) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<Order>> futures = orderIds.stream()
                .map(orderId -> executor.submit(() -> {
                    // Each virtual thread gets DB connection
                    try (Connection conn = dataSource.getConnection()) {
                        return fetchOrder(conn, orderId);
                    }
                }))
                .toList();
            
            return futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        }
    }
}
```

---

### Example 4: Kafka Consumer with Virtual Threads

```java
@Service
public class VirtualThreadKafkaConsumer {
    
    @KafkaListener(topics = "orders", groupId = "vthread-consumer")
    public void consume(List<Order> orders) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Process each order in separate virtual thread
            List<Future<Void>> futures = orders.stream()
                .map(order -> executor.submit(() -> {
                    processOrder(order);
                    return null;
                }))
                .toList();
            
            // Wait for all
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (Exception e) {
                    log.error("Processing failed", e);
                }
            });
        }
    }
    
    private void processOrder(Order order) {
        // Blocking I/O operations
        saveToDatabase(order);
        callExternalAPI(order);
        sendNotification(order);
    }
}
```

---

## Issues in Java 21 and Fixes in Java 24/25

### Issue 1: Pinned Virtual Threads (Java 21)

**Problem**: Virtual threads get "pinned" to carrier threads during:
1. `synchronized` blocks
2. Native method calls (JNI)

```java
// Java 21 - PROBLEM: Pinning
public class PinningIssue {
    
    private final Object lock = new Object();
    
    public void processRequest() {
        synchronized (lock) {  // ⚠️ Pins virtual thread to carrier
            // Blocking I/O - carrier thread blocked!
            String data = httpClient.get("https://api.example.com");
            processData(data);
        }
    }
}
```

**Impact**:
- Virtual thread cannot unmount
- Carrier thread blocked
- Reduces scalability

---

**Fix in Java 21**: Use ReentrantLock instead

```java
// Java 21 - SOLUTION: Use ReentrantLock
public class NoPinning {
    
    private final ReentrantLock lock = new ReentrantLock();
    
    public void processRequest() {
        lock.lock();  // ✅ No pinning
        try {
            String data = httpClient.get("https://api.example.com");
            processData(data);
        } finally {
            lock.unlock();
        }
    }
}
```

---

**Fix in Java 24/25**: Synchronized Blocks No Longer Pin

```java
// Java 24+ - synchronized is now safe!
public class Java24Fixed {
    
    private final Object lock = new Object();
    
    public void processRequest() {
        synchronized (lock) {  // ✅ No longer pins in Java 24+
            String data = httpClient.get("https://api.example.com");
            processData(data);
        }
    }
}
```

**Java 24 Improvement**: JVM detects blocking in synchronized blocks and unmounts virtual thread.

---

### Issue 2: Thread-Local Variables Overhead (Java 21)

**Problem**: ThreadLocal creates copy for each virtual thread → Memory overhead with millions of threads

```java
// Java 21 - PROBLEM: Memory overhead
public class ThreadLocalIssue {
    
    private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();
    
    public void handleRequest(String userId) {
        userContext.set(new UserContext(userId));  // ⚠️ Copy for each virtual thread
        try {
            processRequest();
        } finally {
            userContext.remove();
        }
    }
}
```

**Impact**:
- 1 million virtual threads × 1KB ThreadLocal = 1GB memory
- Garbage collection pressure

---

**Fix in Java 21+**: Use Scoped Values (Preview)

```java
// Java 21+ - SOLUTION: Scoped Values
public class ScopedValueSolution {
    
    private static final ScopedValue<UserContext> userContext = ScopedValue.newInstance();
    
    public void handleRequest(String userId) {
        ScopedValue.where(userContext, new UserContext(userId))
            .run(() -> {
                processRequest();  // ✅ Immutable, shared across virtual threads
            });
    }
    
    private void processRequest() {
        UserContext context = userContext.get();
        // Use context
    }
}
```

**Benefits**:
- Immutable and shared
- No per-thread copy
- Better performance

---

### Issue 3: Structured Concurrency (Java 21 Preview)

**Problem**: Difficult to manage lifecycle of multiple virtual threads

```java
// Java 21 - PROBLEM: Manual thread management
public UserProfile getUserProfile(String userId) {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    Future<User> userFuture = executor.submit(() -> getUser(userId));
    Future<List<Order>> ordersFuture = executor.submit(() -> getOrders(userId));
    
    try {
        User user = userFuture.get();
        List<Order> orders = ordersFuture.get();
        return new UserProfile(user, orders);
    } catch (Exception e) {
        // What about cleanup?
        userFuture.cancel(true);
        ordersFuture.cancel(true);
        throw e;
    } finally {
        executor.shutdown();
    }
}
```

---

**Fix in Java 21+**: Structured Concurrency (Preview)

```java
// Java 21+ - SOLUTION: Structured Concurrency
public UserProfile getUserProfile(String userId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        
        Subtask<User> userTask = scope.fork(() -> getUser(userId));
        Subtask<List<Order>> ordersTask = scope.fork(() -> getOrders(userId));
        
        scope.join();           // Wait for all
        scope.throwIfFailed();  // Propagate errors
        
        return new UserProfile(userTask.get(), ordersTask.get());
    }
    // Auto-cleanup: All subtasks cancelled if parent fails
}
```

**Benefits**:
- Automatic cleanup
- Error propagation
- Cancellation of all subtasks if one fails

---

### Issue 4: Debugging and Observability (Java 21)

**Problem**: Hard to debug millions of virtual threads

```java
// Java 21 - PROBLEM: Poor observability
Thread vThread = Thread.startVirtualThread(() -> {
    // Which carrier thread? Stack trace?
    processRequest();
});
```

---

**Fix in Java 24/25**: Enhanced JFR and Debugging

```java
// Java 24+ - SOLUTION: Better observability
Thread vThread = Thread.ofVirtual()
    .name("order-processor-", 0)  // Auto-incrementing names
    .start(() -> {
        processRequest();
    });

// JFR Events for virtual threads
// - VirtualThreadStart
// - VirtualThreadEnd
// - VirtualThreadPinned
// - VirtualThreadSubmitFailed
```

**Java 24/25 Improvements**:
- JFR events for virtual thread lifecycle
- Better stack traces
- Thread dumps include virtual threads
- IntelliJ IDEA debugger support

---

### Issue 5: File I/O Blocking (Java 21)

**Problem**: File I/O operations pin virtual threads

```java
// Java 21 - PROBLEM: File I/O pins
public String readFile(String path) {
    return Files.readString(Path.of(path));  // ⚠️ Pins virtual thread
}
```

---

**Fix in Java 24/25**: Non-blocking File I/O

```java
// Java 24+ - SOLUTION: Non-blocking file operations
public String readFile(String path) {
    return Files.readString(Path.of(path));  // ✅ No longer pins
}
```

**Java 24 Improvement**: File I/O operations reimplemented to support virtual thread unmounting.

---

## Performance Comparison

### Benchmark: 10,000 Concurrent HTTP Requests

```java
public class PerformanceBenchmark {
    
    // Platform Threads
    public void platformThreads() {
        ExecutorService executor = Executors.newFixedThreadPool(200);
        
        long start = System.currentTimeMillis();
        
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            futures.add(executor.submit(() -> {
                return httpClient.get("https://api.example.com");
            }));
        }
        
        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {}
        });
        
        long end = System.currentTimeMillis();
        System.out.println("Platform threads: " + (end - start) + "ms");
        
        executor.shutdown();
    }
    
    // Virtual Threads
    public void virtualThreads() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            long start = System.currentTimeMillis();
            
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                futures.add(executor.submit(() -> {
                    return httpClient.get("https://api.example.com");
                }));
            }
            
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (Exception e) {}
            });
            
            long end = System.currentTimeMillis();
            System.out.println("Virtual threads: " + (end - start) + "ms");
        }
    }
}
```

**Results**:
```
Platform Threads (200 pool): 50,000ms (50 batches × 1000ms)
Virtual Threads (unlimited): 1,000ms (all parallel)

Improvement: 50x faster
```

---

## Spring Boot Integration

### Spring Boot 3.2+ with Virtual Threads

```java
// application.properties
spring.threads.virtual.enabled=true

// Or programmatic configuration
@Configuration
public class VirtualThreadConfig {
    
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
    
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}

// Controller - automatically uses virtual threads
@RestController
public class OrderController {
    
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        // Runs on virtual thread automatically
        return orderService.findById(id);
    }
}
```

---

## Best Practices

### ✅ Do's

```java
// 1. Use for I/O-bound tasks
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> httpClient.get("https://api.example.com"));
}

// 2. Use ReentrantLock instead of synchronized (Java 21)
private final ReentrantLock lock = new ReentrantLock();

// 3. Use Scoped Values instead of ThreadLocal
private static final ScopedValue<UserContext> context = ScopedValue.newInstance();

// 4. Use try-with-resources for auto-cleanup
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Tasks
}

// 5. Use Structured Concurrency for related tasks
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    // Subtasks
}
```

---

### ❌ Don'ts

```java
// 1. Don't use for CPU-intensive tasks
executor.submit(() -> {
    for (int i = 0; i < 1_000_000_000; i++) {
        // CPU-bound - use platform threads
    }
});

// 2. Don't use synchronized blocks (Java 21)
synchronized (lock) {  // Pins virtual thread
    // Use ReentrantLock instead
}

// 3. Don't use ThreadLocal excessively
private static final ThreadLocal<Data> data = new ThreadLocal<>();  // Memory overhead

// 4. Don't create thread pools for virtual threads
Executors.newFixedThreadPool(100);  // Wrong! Use newVirtualThreadPerTaskExecutor()

// 5. Don't block carrier threads
ForkJoinPool.commonPool().submit(() -> {
    Thread.sleep(10000);  // Blocks carrier thread
});
```

---

## Migration Guide

### From Platform Threads to Virtual Threads

```java
// Before (Platform Threads)
@Configuration
public class OldConfig {
    
    @Bean
    public ExecutorService executor() {
        return Executors.newFixedThreadPool(200);
    }
}

@Service
public class OrderService {
    
    @Autowired
    private ExecutorService executor;
    
    public CompletableFuture<Order> processOrder(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            return orderRepository.findById(orderId);
        }, executor);
    }
}

// After (Virtual Threads)
@Configuration
public class NewConfig {
    
    @Bean
    public ExecutorService executor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

@Service
public class OrderService {
    
    @Autowired
    private ExecutorService executor;
    
    public Order processOrder(String orderId) {
        // Simple blocking code
        return orderRepository.findById(orderId);
    }
}
```

---

## Java Version Evolution Summary

### Java 21 (LTS - September 2023)

**Features**:
- ✅ Virtual Threads (Stable)
- ✅ Structured Concurrency (Preview)
- ✅ Scoped Values (Preview)

**Issues**:
- ⚠️ Pinning with synchronized blocks
- ⚠️ File I/O pins virtual threads
- ⚠️ Limited debugging support

---

### Java 22 (March 2024)

**Improvements**:
- ✅ Structured Concurrency (2nd Preview)
- ✅ Scoped Values (2nd Preview)
- ✅ Better JFR events

**Issues**:
- ⚠️ Still pinning with synchronized
- ⚠️ File I/O still pins

---

### Java 23 (September 2024)

**Improvements**:
- ✅ Structured Concurrency (3rd Preview)
- ✅ Scoped Values (3rd Preview)
- ✅ Enhanced thread dumps

**Issues**:
- ⚠️ Synchronized pinning partially fixed

---

### Java 24 (March 2025 - Expected)

**Major Fixes**:
- ✅ Synchronized blocks no longer pin
- ✅ File I/O no longer pins
- ✅ Structured Concurrency (Stable)
- ✅ Scoped Values (Stable)
- ✅ Full JFR support
- ✅ Better IDE debugging

---

### Java 25 (September 2025 - Expected)

**Expected Features**:
- ✅ Further performance optimizations
- ✅ Enhanced monitoring tools
- ✅ Better integration with frameworks

---

## Interview Questions

### Q1: What are virtual threads?

**Answer**: Lightweight threads managed by JVM (not OS) that enable millions of concurrent threads with low memory footprint (~1KB vs ~1MB).

---

### Q2: When to use virtual threads vs platform threads?

**Answer**: 
- **Virtual threads**: I/O-bound tasks (HTTP, DB, file I/O)
- **Platform threads**: CPU-intensive tasks (calculations, algorithms)

---

### Q3: What is thread pinning?

**Answer**: Virtual thread cannot unmount from carrier thread during synchronized blocks (Java 21) or native calls, blocking the carrier thread.

---

### Q4: How did Java 24 fix pinning?

**Answer**: JVM now detects blocking in synchronized blocks and unmounts virtual thread, allowing carrier thread to execute other virtual threads.

---

### Q5: What is Structured Concurrency?

**Answer**: API for managing lifecycle of multiple related tasks, ensuring automatic cleanup and error propagation.

---

## Key Takeaways

1. **Virtual threads** enable millions of concurrent threads
2. **Use for I/O-bound** tasks, not CPU-intensive
3. **Java 21**: Avoid synchronized, use ReentrantLock
4. **Java 24+**: Synchronized is safe, no pinning
5. **Use Scoped Values** instead of ThreadLocal
6. **Structured Concurrency** for task management
7. **Spring Boot 3.2+** has built-in support
8. **Simple blocking code** scales better than async
9. **Memory footprint**: 1KB vs 1MB per thread
10. **Migration is easy**: Replace thread pool with newVirtualThreadPerTaskExecutor()

---

## Practice Problems

1. Convert REST API from platform threads to virtual threads
2. Implement parallel API calls with Structured Concurrency
3. Benchmark 100K concurrent requests: platform vs virtual
4. Migrate Kafka consumer to use virtual threads
5. Implement Scoped Values for request context
