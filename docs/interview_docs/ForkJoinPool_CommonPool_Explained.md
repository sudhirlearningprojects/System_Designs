# ForkJoinPool.commonPool() - Complete Guide

## What is ForkJoinPool.commonPool()?

**ForkJoinPool.commonPool()** is a shared, static thread pool in Java used for parallel task execution. It's the default executor for:
- `CompletableFuture.supplyAsync()`
- `CompletableFuture.runAsync()`
- Parallel Streams (`stream().parallel()`)
- `Arrays.parallelSort()`

---

## Key Characteristics

| Property | Value |
|----------|-------|
| **Type** | Static, shared thread pool |
| **Default Size** | `Runtime.getRuntime().availableProcessors() - 1` |
| **Thread Type** | Daemon threads |
| **Work Stealing** | Yes (idle threads steal tasks from busy threads) |
| **Lifecycle** | JVM-managed (cannot be shutdown) |
| **Shared** | All applications in JVM share same pool |

---

## Thread Pool Size

### Default Calculation

```java
int parallelism = Runtime.getRuntime().availableProcessors() - 1;

// Examples:
// 4 cores  → 3 threads
// 8 cores  → 7 threads
// 16 cores → 15 threads
```

### Check Pool Size

```java
import java.util.concurrent.ForkJoinPool;

public class CommonPoolInfo {
    
    public static void main(String[] args) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        
        System.out.println("Parallelism: " + commonPool.getParallelism());
        System.out.println("Pool Size: " + commonPool.getPoolSize());
        System.out.println("Active Threads: " + commonPool.getActiveThreadCount());
        System.out.println("Running Threads: " + commonPool.getRunningThreadCount());
        System.out.println("Queued Tasks: " + commonPool.getQueuedTaskCount());
    }
}
```

**Output** (on 8-core machine):
```
Parallelism: 7
Pool Size: 7
Active Threads: 0
Running Threads: 0
Queued Tasks: 0
```

---

## How It Works - Work Stealing

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│           ForkJoinPool.commonPool()                     │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ Thread 1 │  │ Thread 2 │  │ Thread 3 │            │
│  │          │  │          │  │          │            │
│  │ [Task A] │  │ [Task B] │  │ [Task C] │            │
│  │ [Task D] │  │ [Task E] │  │          │ ← Steals   │
│  │ [Task F] │  │          │  │          │   Task F   │
│  └──────────┘  └──────────┘  └──────────┘            │
│       ↑             ↑             ↑                    │
│       └─────────────┴─────────────┘                    │
│         Work Stealing Algorithm                        │
└─────────────────────────────────────────────────────────┘
```

**Work Stealing**: Idle threads steal tasks from busy threads' queues.

---

## Usage Examples

### 1. CompletableFuture (Default)

```java
import java.util.concurrent.CompletableFuture;

public class CommonPoolExample {
    
    public static void main(String[] args) {
        // Uses ForkJoinPool.commonPool() by default
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "Result";
        });
        
        future.thenAccept(result -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            System.out.println("Result: " + result);
        });
        
        future.join();
    }
}
```

**Output**:
```
Thread: ForkJoinPool.commonPool-worker-1
Thread: ForkJoinPool.commonPool-worker-1
Result: Result
```

---

### 2. Parallel Streams

```java
import java.util.Arrays;
import java.util.List;

public class ParallelStreamExample {
    
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        
        // Uses ForkJoinPool.commonPool()
        numbers.parallelStream()
            .forEach(num -> {
                System.out.println("Thread: " + Thread.currentThread().getName() + 
                                 " - Processing: " + num);
            });
    }
}
```

**Output**:
```
Thread: ForkJoinPool.commonPool-worker-1 - Processing: 3
Thread: ForkJoinPool.commonPool-worker-2 - Processing: 5
Thread: main - Processing: 1
Thread: ForkJoinPool.commonPool-worker-3 - Processing: 7
Thread: ForkJoinPool.commonPool-worker-1 - Processing: 4
...
```

---

### 3. Direct Usage

```java
import java.util.concurrent.*;

public class DirectCommonPoolUsage {
    
    public static void main(String[] args) throws Exception {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        
        // Submit task
        Future<String> future = commonPool.submit(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "Task completed";
        });
        
        System.out.println(future.get());
    }
}
```

---

## Configuring Common Pool Size

### Method 1: System Property

```bash
# Set parallelism to 4
java -Djava.util.concurrent.ForkJoinPool.common.parallelism=4 MyApp

# Set to 1 (sequential)
java -Djava.util.concurrent.ForkJoinPool.common.parallelism=1 MyApp
```

---

### Method 2: Programmatically (Before First Use)

```java
public class ConfigureCommonPool {
    
    static {
        // Must be set before commonPool is first accessed
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
    }
    
    public static void main(String[] args) {
        System.out.println("Parallelism: " + 
            ForkJoinPool.commonPool().getParallelism());
    }
}
```

**Output**:
```
Parallelism: 4
```

---

## Custom ForkJoinPool (Alternative)

### When to Use Custom Pool

- Need different parallelism level
- Want to isolate tasks
- Need to control thread lifecycle
- Avoid sharing with other components

```java
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class CustomForkJoinPool {
    
    public static void main(String[] args) throws Exception {
        // Create custom pool with 4 threads
        ForkJoinPool customPool = new ForkJoinPool(4);
        
        // Use custom pool for parallel stream
        customPool.submit(() -> {
            IntStream.range(1, 10)
                .parallel()
                .forEach(i -> {
                    System.out.println("Thread: " + Thread.currentThread().getName() + 
                                     " - Value: " + i);
                });
        }).get();
        
        customPool.shutdown();
    }
}
```

**Output**:
```
Thread: ForkJoinPool-1-worker-1 - Value: 3
Thread: ForkJoinPool-1-worker-2 - Value: 5
Thread: ForkJoinPool-1-worker-3 - Value: 7
...
```

---

### Custom Pool for CompletableFuture

```java
import java.util.concurrent.*;

public class CustomPoolCompletableFuture {
    
    public static void main(String[] args) {
        ForkJoinPool customPool = new ForkJoinPool(4);
        
        // Use custom pool instead of commonPool
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "Result";
        }, customPool);
        
        System.out.println(future.join());
        
        customPool.shutdown();
    }
}
```

---

## Common Pool vs Custom Pool

### Comparison

| Aspect | Common Pool | Custom Pool |
|--------|-------------|-------------|
| **Creation** | Static, pre-created | Manual creation |
| **Lifecycle** | JVM-managed | Application-managed |
| **Shutdown** | Cannot shutdown | Must shutdown |
| **Sharing** | Shared across JVM | Isolated |
| **Size** | CPU cores - 1 | Configurable |
| **Use Case** | General purpose | Specific workloads |

---

### Example: Blocking Operations

```java
import java.util.concurrent.*;

public class BlockingOperationsExample {
    
    public static void main(String[] args) throws Exception {
        // BAD: Blocking in commonPool
        CompletableFuture<String> bad = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000); // Blocks commonPool thread
            } catch (InterruptedException e) {}
            return "Bad";
        });
        
        // GOOD: Use custom pool for blocking operations
        ExecutorService customExecutor = Executors.newFixedThreadPool(10);
        
        CompletableFuture<String> good = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000); // Blocks custom pool thread
            } catch (InterruptedException e) {}
            return "Good";
        }, customExecutor);
        
        System.out.println(good.join());
        customExecutor.shutdown();
    }
}
```

---

## Monitoring Common Pool

### Real-Time Monitoring

```java
import java.util.concurrent.*;

public class MonitorCommonPool {
    
    public static void main(String[] args) throws Exception {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        
        // Submit multiple tasks
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {}
            });
        }
        
        // Monitor pool
        for (int i = 0; i < 5; i++) {
            System.out.println("\n--- Monitoring ---");
            System.out.println("Pool Size: " + commonPool.getPoolSize());
            System.out.println("Active Threads: " + commonPool.getActiveThreadCount());
            System.out.println("Running Threads: " + commonPool.getRunningThreadCount());
            System.out.println("Queued Tasks: " + commonPool.getQueuedTaskCount());
            System.out.println("Steal Count: " + commonPool.getStealCount());
            
            Thread.sleep(500);
        }
    }
}
```

**Output**:
```
--- Monitoring ---
Pool Size: 7
Active Threads: 7
Running Threads: 7
Queued Tasks: 13
Steal Count: 5

--- Monitoring ---
Pool Size: 7
Active Threads: 7
Running Threads: 7
Queued Tasks: 6
Steal Count: 12
...
```

---

## Best Practices

### ✅ DO

```java
// 1. Use for CPU-intensive, short-lived tasks
CompletableFuture.supplyAsync(() -> {
    return expensiveCalculation();
});

// 2. Use for parallel streams
list.parallelStream()
    .map(this::transform)
    .collect(Collectors.toList());

// 3. Configure size for specific workloads
System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
```

---

### ❌ DON'T

```java
// 1. DON'T use for blocking I/O
CompletableFuture.supplyAsync(() -> {
    return httpClient.get(url); // BAD: Blocks thread
});

// 2. DON'T use for long-running tasks
CompletableFuture.supplyAsync(() -> {
    Thread.sleep(60000); // BAD: Holds thread for 1 minute
    return result;
});

// 3. DON'T shutdown commonPool
ForkJoinPool.commonPool().shutdown(); // BAD: Throws exception
```

---

### Correct Approach for Blocking Operations

```java
// Use separate thread pool for blocking operations
ExecutorService ioExecutor = Executors.newFixedThreadPool(20);

CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return httpClient.get(url); // Blocking I/O
}, ioExecutor);

// Cleanup
ioExecutor.shutdown();
```

---

## Common Issues

### Issue 1: Pool Starvation

```java
// Problem: All threads blocked
for (int i = 0; i < 100; i++) {
    CompletableFuture.runAsync(() -> {
        Thread.sleep(10000); // All 7 threads blocked
    });
}

// Solution: Use custom pool
ExecutorService executor = Executors.newFixedThreadPool(100);
for (int i = 0; i < 100; i++) {
    CompletableFuture.runAsync(() -> {
        Thread.sleep(10000);
    }, executor);
}
```

---

### Issue 2: Nested Parallel Streams

```java
// Problem: Deadlock risk
list.parallelStream()
    .map(item -> {
        return anotherList.parallelStream() // Nested parallel stream
            .filter(x -> x.equals(item))
            .findFirst();
    })
    .collect(Collectors.toList());

// Solution: Use sequential inner stream
list.parallelStream()
    .map(item -> {
        return anotherList.stream() // Sequential
            .filter(x -> x.equals(item))
            .findFirst();
    })
    .collect(Collectors.toList());
```

---

### Issue 3: Shared State

```java
// Problem: Race condition
List<Integer> results = new ArrayList<>(); // Not thread-safe

IntStream.range(0, 1000)
    .parallel()
    .forEach(i -> results.add(i)); // Race condition

// Solution: Use thread-safe collection
List<Integer> results = Collections.synchronizedList(new ArrayList<>());

IntStream.range(0, 1000)
    .parallel()
    .forEach(i -> results.add(i)); // Thread-safe
```

---

## Performance Comparison

### Sequential vs Parallel

```java
import java.util.stream.IntStream;

public class PerformanceComparison {
    
    public static void main(String[] args) {
        int size = 10_000_000;
        
        // Sequential
        long start = System.currentTimeMillis();
        long sum1 = IntStream.range(0, size)
            .map(i -> i * 2)
            .sum();
        System.out.println("Sequential: " + (System.currentTimeMillis() - start) + "ms");
        
        // Parallel (uses commonPool)
        start = System.currentTimeMillis();
        long sum2 = IntStream.range(0, size)
            .parallel()
            .map(i -> i * 2)
            .sum();
        System.out.println("Parallel: " + (System.currentTimeMillis() - start) + "ms");
    }
}
```

**Output** (8-core machine):
```
Sequential: 45ms
Parallel: 12ms
```

---

## Summary

| Aspect | Details |
|--------|---------|
| **What** | Shared, static thread pool for parallel tasks |
| **Size** | CPU cores - 1 (default) |
| **Threads** | Daemon threads with work-stealing |
| **Used By** | CompletableFuture, Parallel Streams, Arrays.parallelSort() |
| **Lifecycle** | JVM-managed, cannot shutdown |
| **Best For** | CPU-intensive, short-lived tasks |
| **Avoid For** | Blocking I/O, long-running tasks |
| **Configuration** | `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N` |

**Key Takeaway**: Use commonPool for CPU-bound tasks; use custom thread pool for I/O-bound or blocking operations.
