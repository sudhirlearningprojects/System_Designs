# ExecutorService and Executor Framework - Deep Dive

## Overview

The **Executor Framework** (introduced in Java 5) provides a high-level API for managing thread execution, replacing manual thread creation and management.

**Key Benefits**:
- Thread pool management
- Task submission and execution
- Lifecycle management
- Better resource utilization
- Simplified concurrent programming

---

## Executor Framework Hierarchy

```
Executor (interface)
    ↓
ExecutorService (interface)
    ↓
AbstractExecutorService (abstract class)
    ↓
ThreadPoolExecutor (concrete class)
    ↓
ScheduledThreadPoolExecutor (concrete class)
```

---

## 1. Executor Interface (Base)

### Simple Task Execution

```java
public interface Executor {
    void execute(Runnable command);
}

// Usage
Executor executor = Executors.newFixedThreadPool(5);
executor.execute(() -> {
    System.out.println("Task executed by: " + Thread.currentThread().getName());
});
```

**Limitation**: No way to track task completion or get results

---

## 2. ExecutorService Interface (Enhanced)

### Core Methods

```java
public interface ExecutorService extends Executor {
    // Submit tasks
    <T> Future<T> submit(Callable<T> task);
    Future<?> submit(Runnable task);
    <T> Future<T> submit(Runnable task, T result);
    
    // Bulk operations
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks);
    <T> T invokeAny(Collection<? extends Callable<T>> tasks);
    
    // Lifecycle management
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit);
}
```

---

## 3. Types of Thread Pools

### Comparison Table

| Type | Threads | Queue | Use Case | Risk |
|------|---------|-------|----------|------|
| **FixedThreadPool** | Fixed (n) | Unbounded | Known workload | Queue overflow |
| **CachedThreadPool** | 0 to ∞ | SynchronousQueue | Short-lived tasks | Thread explosion |
| **SingleThreadExecutor** | 1 | Unbounded | Sequential execution | Queue overflow |
| **ScheduledThreadPool** | Fixed (n) | DelayedWorkQueue | Scheduled tasks | Queue overflow |
| **WorkStealingPool** | CPU cores | Deque per thread | CPU-intensive | Complex debugging |

---

### 3.1 FixedThreadPool

**Fixed number of threads, unbounded queue**

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

for (int i = 1; i <= 10; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " by " + Thread.currentThread().getName());
        Thread.sleep(2000);
        return "Result " + taskId;
    });
}

executor.shutdown();
```

**Output**:
```
Task 1 by pool-1-thread-1
Task 2 by pool-1-thread-2
Task 3 by pool-1-thread-3
Task 4 by pool-1-thread-4
Task 5 by pool-1-thread-5
Task 6 by pool-1-thread-1 (reused)
...
```

**Use Case**: Web server handling fixed concurrent requests

---

### 3.2 CachedThreadPool

**Creates threads on demand, reuses idle threads**

```java
ExecutorService executor = Executors.newCachedThreadPool();

for (int i = 1; i <= 100; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " by " + Thread.currentThread().getName());
        Thread.sleep(100);
    });
}

executor.shutdown();
```

**Characteristics**:
- Thread idle timeout: 60 seconds
- No core threads
- Max threads: Integer.MAX_VALUE
- Queue: SynchronousQueue (no storage)

**Use Case**: Many short-lived async tasks

**Warning**: Can create thousands of threads → OutOfMemoryError

---

### 3.3 SingleThreadExecutor

**Single worker thread, sequential execution**

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

executor.submit(() -> System.out.println("Task 1"));
executor.submit(() -> System.out.println("Task 2"));
executor.submit(() -> System.out.println("Task 3"));

executor.shutdown();
```

**Output** (always sequential):
```
Task 1
Task 2
Task 3
```

**Use Case**: Event processing, logging, sequential operations

---

### 3.4 ScheduledThreadPoolExecutor

**Schedule tasks with delay or periodic execution**

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

// One-time delay
scheduler.schedule(() -> {
    System.out.println("Executed after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Fixed rate (every 2 seconds)
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Periodic task: " + System.currentTimeMillis());
}, 0, 2, TimeUnit.SECONDS);

// Fixed delay (2 seconds after previous completion)
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Task with delay");
    Thread.sleep(1000);
}, 0, 2, TimeUnit.SECONDS);
```

**Difference**:
- **scheduleAtFixedRate**: Fixed interval (ignores execution time)
- **scheduleWithFixedDelay**: Fixed delay after completion

**Use Case**: Cron jobs, health checks, cache refresh

---

### 3.5 WorkStealingPool (Java 8+)

**ForkJoinPool-based, work-stealing algorithm**

```java
ExecutorService executor = Executors.newWorkStealingPool();

List<Callable<Integer>> tasks = new ArrayList<>();
for (int i = 1; i <= 100; i++) {
    int num = i;
    tasks.add(() -> num * num);
}

List<Future<Integer>> results = executor.invokeAll(tasks);
executor.shutdown();
```

**Characteristics**:
- Parallelism = CPU cores
- Each thread has own deque
- Idle threads steal work from busy threads

**Use Case**: CPU-intensive parallel computations

---

## 4. ThreadPoolExecutor (Custom Configuration)

### Constructor Parameters

```java
public ThreadPoolExecutor(
    int corePoolSize,           // Minimum threads
    int maximumPoolSize,        // Maximum threads
    long keepAliveTime,         // Idle thread timeout
    TimeUnit unit,              // Time unit
    BlockingQueue<Runnable> workQueue,  // Task queue
    ThreadFactory threadFactory,        // Thread creation
    RejectedExecutionHandler handler    // Rejection policy
)
```

### Custom Thread Pool

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    10,                         // maximumPoolSize
    60L,                        // keepAliveTime
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),  // Bounded queue
    new ThreadFactory() {
        private AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "custom-thread-" + counter.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // Rejection policy
);

// Submit tasks
for (int i = 1; i <= 200; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId);
        Thread.sleep(1000);
    });
}

executor.shutdown();
```

---

## 5. Task Submission Methods

### 5.1 execute() - Fire and Forget

```java
executor.execute(() -> {
    System.out.println("Task executed");
});
// No return value, no exception handling
```

---

### 5.2 submit() - Get Future

```java
// Callable with return value
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(1000);
    return 42;
});

Integer result = future.get(); // Blocks until complete
System.out.println("Result: " + result); // 42

// Runnable with no return
Future<?> future2 = executor.submit(() -> {
    System.out.println("Task executed");
});
future2.get(); // Returns null
```

---

### 5.3 invokeAll() - Execute All

```java
List<Callable<String>> tasks = Arrays.asList(
    () -> { Thread.sleep(1000); return "Task 1"; },
    () -> { Thread.sleep(2000); return "Task 2"; },
    () -> { Thread.sleep(1500); return "Task 3"; }
);

List<Future<String>> futures = executor.invokeAll(tasks);

for (Future<String> future : futures) {
    System.out.println(future.get()); // Blocks for each
}
```

**Output** (after 2 seconds - longest task):
```
Task 1
Task 2
Task 3
```

---

### 5.4 invokeAny() - First Successful

```java
List<Callable<String>> tasks = Arrays.asList(
    () -> { Thread.sleep(3000); return "Task 1"; },
    () -> { Thread.sleep(1000); return "Task 2"; },
    () -> { Thread.sleep(2000); return "Task 3"; }
);

String result = executor.invokeAny(tasks);
System.out.println(result); // "Task 2" (fastest)
```

**Behavior**: Returns first successful result, cancels others

---

## 6. Future Interface

### Core Methods

```java
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit) throws TimeoutException;
}
```

### Usage Example

```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(5000);
    return 100;
});

// Check status
System.out.println("Done: " + future.isDone()); // false

// Get with timeout
try {
    Integer result = future.get(2, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Task timeout");
    future.cancel(true); // Cancel task
}

// Check cancellation
System.out.println("Cancelled: " + future.isCancelled());
```

---

## 7. Rejection Policies

### When Rejection Happens

Queue full + All threads busy = RejectedExecutionException

### Built-in Policies

```java
// 1. AbortPolicy (default) - Throw exception
new ThreadPoolExecutor.AbortPolicy()

// 2. CallerRunsPolicy - Run in caller thread
new ThreadPoolExecutor.CallerRunsPolicy()

// 3. DiscardPolicy - Silently discard
new ThreadPoolExecutor.DiscardPolicy()

// 4. DiscardOldestPolicy - Discard oldest, retry
new ThreadPoolExecutor.DiscardOldestPolicy()
```

### Custom Rejection Policy

```java
RejectedExecutionHandler customHandler = (runnable, executor) -> {
    System.out.println("Task rejected: " + runnable.toString());
    // Log to database, send alert, etc.
};

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 4, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(2),
    customHandler
);
```

---

## 8. Lifecycle Management

### Shutdown Methods

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

// Submit tasks
executor.submit(() -> Thread.sleep(5000));
executor.submit(() -> Thread.sleep(3000));

// 1. Graceful shutdown (wait for tasks to complete)
executor.shutdown();
System.out.println("Shutdown initiated");

// 2. Wait for termination
boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
System.out.println("Terminated: " + terminated);

// 3. Force shutdown (interrupt running tasks)
if (!terminated) {
    List<Runnable> pending = executor.shutdownNow();
    System.out.println("Pending tasks: " + pending.size());
}
```

### Proper Shutdown Pattern

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

try {
    // Submit tasks
    executor.submit(() -> processTask());
} finally {
    executor.shutdown();
    try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

## 9. Real-World Examples

### Example 1: Parallel File Processing

```java
@Service
public class FileProcessingService {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void processFiles(List<File> files) {
        List<Future<ProcessResult>> futures = new ArrayList<>();
        
        for (File file : files) {
            Future<ProcessResult> future = executor.submit(() -> {
                return processFile(file);
            });
            futures.add(future);
        }
        
        // Collect results
        for (Future<ProcessResult> future : futures) {
            try {
                ProcessResult result = future.get();
                System.out.println("Processed: " + result.getFileName());
            } catch (Exception e) {
                log.error("Processing failed", e);
            }
        }
    }
    
    @PreDestroy
    public void cleanup() {
        executor.shutdown();
    }
}
```

---

### Example 2: Batch API Calls

```java
@Service
public class UserService {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(20);
    
    public List<UserDetails> fetchUserDetails(List<String> userIds) {
        List<Callable<UserDetails>> tasks = userIds.stream()
            .map(userId -> (Callable<UserDetails>) () -> {
                return externalApi.getUserDetails(userId);
            })
            .collect(Collectors.toList());
        
        try {
            List<Future<UserDetails>> futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
            
            return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }
}
```

---

### Example 3: Scheduled Cache Refresh

```java
@Service
public class CacheRefreshService {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @PostConstruct
    public void startScheduledTasks() {
        // Refresh cache every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshCache();
            } catch (Exception e) {
                log.error("Cache refresh failed", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        // Health check every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            performHealthCheck();
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void stopScheduledTasks() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

### Example 4: Order Processing Pipeline

```java
@Service
public class OrderProcessingService {
    
    private final ExecutorService executor = new ThreadPoolExecutor(
        10, 50, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    
    public CompletableFuture<OrderResult> processOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Validate
            validateOrder(order);
            
            // Step 2: Process payment
            PaymentResult payment = processPayment(order);
            
            // Step 3: Update inventory
            updateInventory(order);
            
            // Step 4: Create shipment
            Shipment shipment = createShipment(order);
            
            return new OrderResult(order.getId(), payment, shipment);
        }, executor);
    }
    
    public void processOrdersBatch(List<Order> orders) {
        List<CompletableFuture<OrderResult>> futures = orders.stream()
            .map(this::processOrder)
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                System.out.println("All orders processed");
            });
    }
}
```

---

## 10. Monitoring and Metrics

### ThreadPoolExecutor Monitoring

```java
@Component
public class ExecutorMonitor {
    
    private final ThreadPoolExecutor executor;
    
    public ExecutorMonitor() {
        this.executor = new ThreadPoolExecutor(
            10, 50, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000)
        );
    }
    
    @Scheduled(fixedRate = 10000)
    public void logMetrics() {
        log.info("=== Executor Metrics ===");
        log.info("Active threads: {}", executor.getActiveCount());
        log.info("Pool size: {}", executor.getPoolSize());
        log.info("Core pool size: {}", executor.getCorePoolSize());
        log.info("Max pool size: {}", executor.getMaximumPoolSize());
        log.info("Queue size: {}", executor.getQueue().size());
        log.info("Completed tasks: {}", executor.getCompletedTaskCount());
        log.info("Total tasks: {}", executor.getTaskCount());
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }
}
```

---

## 11. Best Practices

### ✅ Do's

```java
// 1. Always shutdown executor
try {
    executor.submit(task);
} finally {
    executor.shutdown();
}

// 2. Use bounded queues
new ThreadPoolExecutor(10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100)); // Bounded

// 3. Handle exceptions
Future<Integer> future = executor.submit(() -> {
    try {
        return riskyOperation();
    } catch (Exception e) {
        log.error("Task failed", e);
        throw e;
    }
});

// 4. Set meaningful thread names
ThreadFactory factory = new ThreadFactoryBuilder()
    .setNameFormat("order-processor-%d")
    .build();

// 5. Use appropriate pool type
ExecutorService executor = Executors.newFixedThreadPool(10); // Known workload
```

### ❌ Don'ts

```java
// 1. Don't use unbounded queues in production
new LinkedBlockingQueue<>(); // Can cause OutOfMemoryError

// 2. Don't ignore Future exceptions
Future<Integer> future = executor.submit(task);
// Missing: future.get() to check exceptions

// 3. Don't create unlimited threads
Executors.newCachedThreadPool(); // Can create thousands of threads

// 4. Don't forget to shutdown
ExecutorService executor = Executors.newFixedThreadPool(10);
// Missing: executor.shutdown()

// 5. Don't block in tasks
executor.submit(() -> {
    Thread.sleep(Long.MAX_VALUE); // Blocks thread forever
});
```

---

## 12. Common Pitfalls

### Pitfall 1: Thread Leaks

```java
// ❌ Bad: Never shutdown
public class Service {
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void process() {
        executor.submit(() -> doWork());
    }
    // Threads never released!
}

// ✅ Good: Proper lifecycle
@Service
public class Service {
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    @PreDestroy
    public void cleanup() {
        executor.shutdown();
    }
}
```

### Pitfall 2: Ignoring Exceptions

```java
// ❌ Bad: Exception lost
Future<Integer> future = executor.submit(() -> {
    throw new RuntimeException("Error");
});
// Exception not visible until future.get()

// ✅ Good: Handle exceptions
Future<Integer> future = executor.submit(() -> {
    try {
        return riskyOperation();
    } catch (Exception e) {
        log.error("Task failed", e);
        throw e;
    }
});

try {
    future.get();
} catch (ExecutionException e) {
    log.error("Execution failed", e.getCause());
}
```

### Pitfall 3: Unbounded Queue

```java
// ❌ Bad: Unbounded queue
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>() // Unbounded!
);

// ✅ Good: Bounded queue with rejection policy
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## Interview Questions

### Q1: Difference between execute() and submit()?

**Answer**:
- **execute()**: Runnable only, no return value, exceptions lost
- **submit()**: Callable/Runnable, returns Future, exceptions captured

### Q2: When to use FixedThreadPool vs CachedThreadPool?

**Answer**:
- **FixedThreadPool**: Known workload, long-running tasks, bounded resources
- **CachedThreadPool**: Unknown workload, short-lived tasks, bursty traffic

### Q3: What happens if queue is full?

**Answer**: RejectedExecutionException thrown (depends on rejection policy)

### Q4: How to handle task exceptions?

**Answer**:
```java
Future<Integer> future = executor.submit(task);
try {
    Integer result = future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause(); // Original exception
}
```

### Q5: Difference between shutdown() and shutdownNow()?

**Answer**:
- **shutdown()**: Graceful, waits for tasks to complete
- **shutdownNow()**: Forceful, interrupts running tasks, returns pending tasks

---

## Key Takeaways

1. **Use ExecutorService** instead of manual thread creation
2. **Choose appropriate pool type** based on workload
3. **Always shutdown** executors to prevent thread leaks
4. **Use bounded queues** to prevent OutOfMemoryError
5. **Handle exceptions** from Future.get()
6. **Set rejection policy** for queue overflow scenarios
7. **Monitor thread pool metrics** in production
8. **Use meaningful thread names** for debugging
9. **Prefer submit() over execute()** for better error handling
10. **Test with realistic load** to tune pool parameters

---

## Practice Problems

1. Create thread pool that processes 1000 tasks with max 10 concurrent threads
2. Implement scheduled task that runs every 5 minutes with error handling
3. Build parallel file processor with progress tracking
4. Create custom rejection policy that logs rejected tasks
5. Implement graceful shutdown with timeout handling
