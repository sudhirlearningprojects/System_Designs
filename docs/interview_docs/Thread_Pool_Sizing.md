# Thread Pool Sizing Guide for Java ExecutorService

## Overview

Choosing the right thread pool size is critical for application performance. Too few threads = underutilization, too many threads = context switching overhead and memory waste.

---

## Quick Decision Matrix

| Task Type | Formula | Example (8 cores) | Use Case |
|-----------|---------|-------------------|----------|
| **CPU-Bound** | `cores` or `cores + 1` | 8-9 threads | Image processing, encryption, calculations |
| **Mixed** | `cores * 2` | 16 threads | Web requests with some computation |
| **I/O-Bound** | `cores * (1 + wait/compute)` | 80+ threads | Database queries, file operations |
| **Heavy I/O** | `cores * 50-100` | 400-800 threads | External API calls, network operations |

---

## 1. CPU-Bound Tasks

**Definition**: Tasks that spend most time computing (encryption, image processing, mathematical calculations)

```java
// Optimal size = number of CPU cores
int threadPoolSize = Runtime.getRuntime().availableProcessors();

ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
```

**Why?** More threads than cores causes context switching without performance gain.

**Example Use Cases**:
- Video encoding/transcoding
- Data compression
- Cryptographic operations
- Machine learning inference

---

## 2. I/O-Bound Tasks

**Definition**: Tasks that spend most time waiting (network calls, database queries, file I/O)

```java
// Start with 2x cores
int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;

// Or use wait/compute ratio
int cores = Runtime.getRuntime().availableProcessors();
double waitTime = 90.0;  // 90ms waiting
double computeTime = 10.0; // 10ms computing
int threadPoolSize = (int) (cores * (1 + waitTime/computeTime));
// Result: cores * 10 = 80 threads for 8-core machine

ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
```

**Example Use Cases**:
- REST API calls
- Database queries
- File uploads/downloads
- Message queue operations

---

## 3. Little's Law Formula (Most Accurate)

**Formula**: `Thread Pool Size = (Target Throughput × Response Time) / CPU Time per Request`

```java
// Example calculation:
// Target: 1000 requests/sec
// Response time: 100ms (0.1s)
// CPU time per request: 10ms (0.01s)

double targetThroughput = 1000;  // requests/sec
double responseTime = 0.1;        // seconds
double cpuTime = 0.01;            // seconds

int threadPoolSize = (int) ((targetThroughput * responseTime) / cpuTime);
// Result: (1000 × 0.1) / 0.01 = 10,000 threads
```

**When to use**: When you have specific throughput and latency requirements.

---

## 4. Production-Ready Configuration

### Basic Fixed Thread Pool
```java
@Configuration
public class ThreadPoolConfig {
    
    @Value("${thread.pool.size:#{T(Runtime).getRuntime().availableProcessors() * 2}}")
    private int threadPoolSize;
    
    @Bean(name = "taskExecutor")
    public ExecutorService taskExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
```

### Advanced ThreadPoolExecutor with Fine Control
```java
@Configuration
public class AdvancedThreadPoolConfig {
    
    @Bean(name = "customExecutor")
    public ThreadPoolExecutor customExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        
        return new ThreadPoolExecutor(
            cores,                              // corePoolSize (minimum threads)
            cores * 2,                          // maximumPoolSize
            60L,                                // keepAliveTime
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),    // workQueue capacity
            new ThreadFactoryBuilder()
                .setNameFormat("custom-pool-%d")
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()  // rejectionPolicy
        );
    }
}
```

### Separate Pools for Different Task Types
```java
@Configuration
public class MultiPoolConfig {
    
    // CPU-bound tasks (image processing, encryption)
    @Bean(name = "cpuBoundExecutor")
    public ExecutorService cpuBoundExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(cores);
    }
    
    // I/O-bound tasks (database, external APIs)
    @Bean(name = "ioBoundExecutor")
    public ExecutorService ioBoundExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(cores * 10);
    }
    
    // Scheduled tasks
    @Bean(name = "scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(4);
    }
}
```

---

## 5. Real-World Examples from System Designs

### Example 1: Instagram Feed Generation (I/O-Bound)
```java
// Heavy database queries + Redis cache calls
@Service
public class FeedService {
    
    private final ExecutorService executor;
    
    public FeedService() {
        int cores = Runtime.getRuntime().availableProcessors();
        // I/O-bound: 90% waiting, 10% computing
        this.executor = Executors.newFixedThreadPool(cores * 10);
    }
    
    public List<Post> generateFeed(String userId) {
        List<Future<List<Post>>> futures = new ArrayList<>();
        
        // Parallel fetch from multiple sources
        futures.add(executor.submit(() -> fetchFollowingPosts(userId)));
        futures.add(executor.submit(() -> fetchRecommendedPosts(userId)));
        futures.add(executor.submit(() -> fetchTrendingPosts()));
        
        // Aggregate results
        return futures.stream()
            .flatMap(f -> {
                try { return f.get().stream(); }
                catch (Exception e) { return Stream.empty(); }
            })
            .collect(Collectors.toList());
    }
}
```

### Example 2: Uber Driver Matching (CPU-Bound)
```java
// Distance calculations + scoring algorithms
@Service
public class MatchingService {
    
    private final ExecutorService executor;
    
    public MatchingService() {
        int cores = Runtime.getRuntime().availableProcessors();
        // CPU-bound: heavy calculations
        this.executor = Executors.newFixedThreadPool(cores);
    }
    
    public Driver findBestDriver(Location pickup, List<Driver> nearbyDrivers) {
        List<Future<DriverScore>> futures = nearbyDrivers.stream()
            .map(driver -> executor.submit(() -> calculateScore(driver, pickup)))
            .collect(Collectors.toList());
        
        return futures.stream()
            .map(f -> {
                try { return f.get(); }
                catch (Exception e) { return null; }
            })
            .filter(Objects::nonNull)
            .max(Comparator.comparing(DriverScore::getScore))
            .map(DriverScore::getDriver)
            .orElse(null);
    }
}
```

### Example 3: Notification System (Heavy I/O)
```java
// External API calls (SendGrid, Twilio, FCM)
@Service
public class NotificationService {
    
    private final ExecutorService executor;
    
    public NotificationService() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Heavy I/O: external API calls with high latency
        this.executor = Executors.newFixedThreadPool(cores * 20);
    }
    
    public void sendNotifications(List<Notification> notifications) {
        notifications.forEach(notification -> 
            executor.submit(() -> {
                try {
                    switch (notification.getChannel()) {
                        case EMAIL -> sendEmail(notification);
                        case SMS -> sendSMS(notification);
                        case PUSH -> sendPush(notification);
                    }
                } catch (Exception e) {
                    handleFailure(notification, e);
                }
            })
        );
    }
}
```

### Example 4: TikTok Video Processing (Mixed)
```java
// Video transcoding (CPU) + S3 upload (I/O)
@Service
public class VideoProcessingService {
    
    private final ExecutorService cpuExecutor;
    private final ExecutorService ioExecutor;
    
    public VideoProcessingService() {
        int cores = Runtime.getRuntime().availableProcessors();
        this.cpuExecutor = Executors.newFixedThreadPool(cores);      // CPU-bound
        this.ioExecutor = Executors.newFixedThreadPool(cores * 5);   // I/O-bound
    }
    
    public void processVideo(Video video) {
        // Step 1: Transcode (CPU-bound)
        CompletableFuture<List<VideoVariant>> transcodeFuture = 
            CompletableFuture.supplyAsync(() -> transcodeVideo(video), cpuExecutor);
        
        // Step 2: Upload to S3 (I/O-bound)
        transcodeFuture.thenAcceptAsync(variants -> {
            variants.forEach(variant -> uploadToS3(variant));
        }, ioExecutor);
    }
}
```

---

## 6. Monitoring and Tuning

### Add Metrics
```java
@Configuration
public class MonitoredThreadPoolConfig {
    
    @Bean
    public ThreadPoolExecutor monitoredExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10, 50, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000)
        );
        
        // Schedule monitoring
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        monitor.scheduleAtFixedRate(() -> {
            log.info("Pool size: {}, Active: {}, Queue: {}, Completed: {}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
            );
        }, 0, 30, TimeUnit.SECONDS);
        
        return executor;
    }
}
```

### Key Metrics to Monitor
- **Active threads**: Should be close to pool size under load
- **Queue size**: Growing queue = need more threads
- **Rejected tasks**: Pool is saturated
- **CPU utilization**: <50% = increase threads, >90% = decrease threads
- **Response time**: Increasing = bottleneck

---

## 7. Common Pitfalls

### ❌ Don't: Use Cached Thread Pool for Unbounded Tasks
```java
// BAD: Can create thousands of threads
ExecutorService executor = Executors.newCachedThreadPool();
```

### ✅ Do: Use Fixed or Custom Thread Pool
```java
// GOOD: Bounded thread pool
ExecutorService executor = Executors.newFixedThreadPool(50);
```

### ❌ Don't: Set Pool Size = Expected Concurrent Users
```java
// BAD: 10,000 threads for 10,000 users
ExecutorService executor = Executors.newFixedThreadPool(10000);
```

### ✅ Do: Use Formula Based on Task Type
```java
// GOOD: Based on cores and task characteristics
int cores = Runtime.getRuntime().availableProcessors();
ExecutorService executor = Executors.newFixedThreadPool(cores * 2);
```

### ❌ Don't: Ignore Memory Constraints
```java
// BAD: Each thread uses ~1MB stack space
// 1000 threads = 1GB just for stacks
ExecutorService executor = Executors.newFixedThreadPool(1000);
```

### ✅ Do: Consider Total Memory Available
```java
// GOOD: Account for heap + stack memory
long maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
int maxThreads = (int) (maxMemoryMB / 2);  // Conservative estimate
```

---

## 8. Decision Flowchart

```
Start
  |
  ├─> Task Type?
      |
      ├─> CPU-Bound (computation heavy)
      |   └─> Use: cores or cores + 1
      |
      ├─> I/O-Bound (network, DB, files)
      |   └─> Use: cores * (1 + wait/compute ratio)
      |
      ├─> Mixed (both CPU and I/O)
      |   └─> Use: cores * 2 (start here, tune)
      |
      └─> Have SLA requirements?
          └─> Use: Little's Law formula
```

---

## 9. Testing and Validation

### Load Testing Script
```java
@Test
public void testThreadPoolPerformance() {
    int[] poolSizes = {8, 16, 32, 64, 128};
    
    for (int poolSize : poolSizes) {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        
        long startTime = System.currentTimeMillis();
        List<Future<?>> futures = new ArrayList<>();
        
        // Submit 1000 tasks
        for (int i = 0; i < 1000; i++) {
            futures.add(executor.submit(() -> simulateTask()));
        }
        
        // Wait for completion
        futures.forEach(f -> {
            try { f.get(); } catch (Exception e) {}
        });
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Pool size: %d, Duration: %dms%n", poolSize, duration);
        
        executor.shutdown();
    }
}
```

---

## 10. Quick Reference

### Starting Points (8-core machine)

| Scenario | Pool Size | Reasoning |
|----------|-----------|-----------|
| Image processing | 8 | CPU-bound |
| REST API calls | 80 | I/O-bound (10:1 wait/compute) |
| Database queries | 40 | I/O-bound (5:1 wait/compute) |
| File operations | 16 | Mixed workload |
| External APIs | 160 | Heavy I/O (20:1 wait/compute) |
| Web scraping | 100 | Network-bound |
| Batch processing | 16 | Mixed CPU/I/O |

### Rule of Thumb
1. **Start conservative**: `cores * 2`
2. **Load test**: Simulate production traffic
3. **Monitor**: CPU, memory, response times
4. **Tune**: Adjust based on metrics
5. **Document**: Record final configuration and reasoning

---

## 11. Additional Resources

- [Java Concurrency in Practice](https://jcip.net/) - Brian Goetz
- [Little's Law](https://en.wikipedia.org/wiki/Little%27s_law) - Queueing theory
- [ThreadPoolExecutor JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html)

---

**Last Updated**: 2024
**Author**: System Designs Collection
