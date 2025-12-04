# CompletableFuture Threading Behavior - Deep Dive

## Quick Answer

**CompletableFuture can run on EITHER main thread OR background threads**, depending on which method you use:

| Method | Thread | Example |
|--------|--------|---------|
| `supplyAsync()` | ✅ Background (ForkJoinPool) | `CompletableFuture.supplyAsync(() -> task())` |
| `runAsync()` | ✅ Background (ForkJoinPool) | `CompletableFuture.runAsync(() -> task())` |
| `completedFuture()` | ❌ Main thread (already complete) | `CompletableFuture.completedFuture(value)` |
| `thenApply()` | ⚠️ Same thread as previous stage | `future.thenApply(x -> x * 2)` |
| `thenApplyAsync()` | ✅ Background (ForkJoinPool) | `future.thenApplyAsync(x -> x * 2)` |

---

## Detailed Examples

### Example 1: supplyAsync() - Runs in Background Thread

```java
import java.util.concurrent.*;

public class AsyncThreadExample {
    public static void main(String[] args) throws Exception {
        System.out.println("Main thread: " + Thread.currentThread().getName());
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            // This runs in ForkJoinPool.commonPool() thread
            System.out.println("supplyAsync thread: " + Thread.currentThread().getName());
            return "Result from background thread";
        });
        
        String result = future.get(); // Main thread waits here
        System.out.println("Result: " + result);
        System.out.println("Back to main thread: " + Thread.currentThread().getName());
    }
}
```

**Output**:
```
Main thread: main
supplyAsync thread: ForkJoinPool.commonPool-worker-1
Result: Result from background thread
Back to main thread: main
```

**Explanation**: 
- `supplyAsync()` executes the task in **ForkJoinPool.commonPool()** (background thread)
- Main thread continues and waits at `get()`

---

### Example 2: completedFuture() - Already Complete (No Thread)

```java
public class CompletedFutureExample {
    public static void main(String[] args) {
        System.out.println("Main thread: " + Thread.currentThread().getName());
        
        // Already completed, no async execution
        CompletableFuture<String> future = CompletableFuture.completedFuture("Immediate value");
        
        System.out.println("Result: " + future.join());
        System.out.println("Still main thread: " + Thread.currentThread().getName());
    }
}
```

**Output**:
```
Main thread: main
Result: Immediate value
Still main thread: main
```

**Explanation**: 
- `completedFuture()` creates an already-completed future
- No background thread involved
- Everything runs on main thread

---

### Example 3: thenApply() vs thenApplyAsync()

```java
public class ThenApplyThreading {
    public static void main(String[] args) throws Exception {
        System.out.println("Main thread: " + Thread.currentThread().getName());
        
        // Test 1: thenApply() - runs on same thread as previous stage
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync thread: " + Thread.currentThread().getName());
            return 10;
        }).thenApply(x -> {
            // Runs on SAME thread as supplyAsync (ForkJoinPool thread)
            System.out.println("thenApply thread: " + Thread.currentThread().getName());
            return x * 2;
        });
        
        System.out.println("Result 1: " + future1.get());
        
        // Test 2: thenApplyAsync() - runs on different thread
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync thread: " + Thread.currentThread().getName());
            return 10;
        }).thenApplyAsync(x -> {
            // Runs on DIFFERENT ForkJoinPool thread
            System.out.println("thenApplyAsync thread: " + Thread.currentThread().getName());
            return x * 2;
        });
        
        System.out.println("Result 2: " + future2.get());
    }
}
```

**Output**:
```
Main thread: main
supplyAsync thread: ForkJoinPool.commonPool-worker-1
thenApply thread: ForkJoinPool.commonPool-worker-1
Result 1: 20
supplyAsync thread: ForkJoinPool.commonPool-worker-2
thenApplyAsync thread: ForkJoinPool.commonPool-worker-3
Result 2: 20
```

**Key Difference**:
- `thenApply()` - Reuses the same thread (efficient)
- `thenApplyAsync()` - Uses a new thread (more overhead)

---

### Example 4: Custom Executor

```java
import java.util.concurrent.*;

public class CustomExecutorExample {
    public static void main(String[] args) throws Exception {
        // Create custom thread pool
        ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setName("CustomThread-" + t.getId());
            return t;
        });
        
        System.out.println("Main thread: " + Thread.currentThread().getName());
        
        // Use custom executor
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Task thread: " + Thread.currentThread().getName());
            return "Result from custom thread";
        }, executor); // Pass custom executor
        
        System.out.println("Result: " + future.get());
        
        executor.shutdown();
    }
}
```

**Output**:
```
Main thread: main
Task thread: CustomThread-15
Result: Result from custom thread
```

**Explanation**: 
- Pass custom executor as second parameter to `supplyAsync()`
- Task runs in your custom thread pool instead of ForkJoinPool

---

## Complete Threading Behavior Table

| Method | Thread Behavior | Use Case |
|--------|----------------|----------|
| `supplyAsync(task)` | ForkJoinPool.commonPool() | CPU-bound async tasks |
| `supplyAsync(task, executor)` | Custom executor thread | Control thread pool |
| `runAsync(task)` | ForkJoinPool.commonPool() | Async task with no return |
| `completedFuture(value)` | No thread (immediate) | Already have result |
| `thenApply(fn)` | Same as previous stage | Transform result (efficient) |
| `thenApplyAsync(fn)` | New ForkJoinPool thread | Force async transformation |
| `thenAccept(fn)` | Same as previous stage | Consume result |
| `thenAcceptAsync(fn)` | New ForkJoinPool thread | Force async consumption |
| `thenRun(fn)` | Same as previous stage | Run after completion |
| `thenRunAsync(fn)` | New ForkJoinPool thread | Force async run |
| `thenCompose(fn)` | Same as previous stage | Chain dependent futures |
| `thenComposeAsync(fn)` | New ForkJoinPool thread | Force async chaining |
| `join()` / `get()` | Blocks calling thread | Wait for result |

---

## Real-World Example: HTTP API Calls

```java
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

public class HTTPThreadingExample {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Main thread: " + Thread.currentThread().getName());
        
        // HTTP call runs in HttpClient's internal thread pool
        CompletableFuture<String> future = httpClient
            .sendAsync(
                HttpRequest.newBuilder()
                    .uri(URI.create("https://jsonplaceholder.typicode.com/users/1"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            .thenApply(response -> {
                // This runs in HttpClient's thread
                System.out.println("=== Response processing thread: " + Thread.currentThread().getName());
                return response.body();
            })
            .thenApplyAsync(body -> {
                // This runs in ForkJoinPool
                System.out.println("=== JSON parsing thread: " + Thread.currentThread().getName());
                return body.substring(0, 50) + "...";
            });
        
        String result = future.get(); // Main thread waits
        System.out.println("=== Result: " + result);
        System.out.println("=== Back to main thread: " + Thread.currentThread().getName());
    }
}
```

**Output**:
```
=== Main thread: main
=== Response processing thread: HttpClient-1-Worker-0
=== JSON parsing thread: ForkJoinPool.commonPool-worker-1
=== Result: {"id":1,"name":"Leanne Graham","username":"Bret"...
=== Back to main thread: main
```

**Key Points**:
1. Main thread starts the request
2. HttpClient uses its own thread pool for network I/O
3. `thenApply()` runs on HttpClient's thread
4. `thenApplyAsync()` switches to ForkJoinPool
5. `get()` blocks main thread until complete

---

## Common Pitfall: Blocking Main Thread

### ❌ Bad: Blocks Main Thread

```java
public static void main(String[] args) throws Exception {
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        sleep(2000); // Simulates long task
        return "Result";
    });
    
    // Main thread BLOCKS here for 2 seconds
    String result = future.get();
    System.out.println(result);
}
```

### ✅ Good: Non-Blocking

```java
public static void main(String[] args) throws Exception {
    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        sleep(2000);
        return "Result";
    });
    
    // Main thread continues, doesn't block
    System.out.println("Main thread continues...");
    
    // Do other work
    doOtherWork();
    
    // Only block when you need the result
    String result = future.get();
    System.out.println(result);
}
```

---

## ForkJoinPool.commonPool() Details

### What is ForkJoinPool.commonPool()?

- **Default thread pool** used by CompletableFuture
- **Size**: `Runtime.getRuntime().availableProcessors() - 1`
- **Shared**: Used by all `supplyAsync()` calls without custom executor
- **Daemon threads**: JVM exits even if tasks are running

### Check Pool Size

```java
public class ForkJoinPoolInfo {
    public static void main(String[] args) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        
        System.out.println("Parallelism: " + pool.getParallelism());
        System.out.println("Pool size: " + pool.getPoolSize());
        System.out.println("Active threads: " + pool.getActiveThreadCount());
        System.out.println("Running threads: " + pool.getRunningThreadCount());
    }
}
```

**Output** (on 8-core machine):
```
Parallelism: 7
Pool size: 7
Active threads: 0
Running threads: 0
```

---

## When to Use Custom Executor

### Use ForkJoinPool.commonPool() when:
- ✅ CPU-bound tasks
- ✅ Short-lived tasks
- ✅ Don't need thread control

### Use Custom Executor when:
- ✅ I/O-bound tasks (HTTP calls, database queries)
- ✅ Long-running tasks
- ✅ Need specific thread pool size
- ✅ Need named threads for debugging
- ✅ Need different thread priorities

### Example: I/O-Bound Tasks

```java
// ❌ Bad: Uses ForkJoinPool for I/O
CompletableFuture.supplyAsync(() -> {
    return callDatabase(); // Blocks ForkJoinPool thread
});

// ✅ Good: Use custom executor for I/O
ExecutorService ioExecutor = Executors.newFixedThreadPool(20);
CompletableFuture.supplyAsync(() -> {
    return callDatabase(); // Blocks custom thread, not ForkJoinPool
}, ioExecutor);
```

---

## Visualization

```
Main Thread:
  |
  |-- CompletableFuture.supplyAsync(() -> task())
  |                                    |
  |                                    v
  |                          ForkJoinPool Thread:
  |                                    |-- Execute task
  |                                    |-- Return result
  |                                    v
  |-- future.get() <-- BLOCKS HERE until result ready
  |
  v
Continue main thread
```

---

## Key Takeaways

1. **supplyAsync()** - Runs in **background thread** (ForkJoinPool)
2. **completedFuture()** - No thread, already complete
3. **thenApply()** - Reuses **same thread** as previous stage
4. **thenApplyAsync()** - Uses **new thread** from ForkJoinPool
5. **get()/join()** - **Blocks calling thread** (usually main)
6. **Custom executor** - Control which thread pool to use
7. **ForkJoinPool.commonPool()** - Default pool, size = CPU cores - 1
8. **Use custom executor for I/O** - Don't block ForkJoinPool

---

## Quick Reference

```java
// Background thread (ForkJoinPool)
CompletableFuture.supplyAsync(() -> task());

// Custom thread pool
CompletableFuture.supplyAsync(() -> task(), customExecutor);

// Same thread as previous stage
future.thenApply(x -> transform(x));

// New background thread
future.thenApplyAsync(x -> transform(x));

// Main thread (already complete)
CompletableFuture.completedFuture(value);

// Blocks calling thread
future.get();  // or future.join();
```

**Bottom Line**: CompletableFuture **does NOT run on main thread** when using `supplyAsync()` - it uses background threads from ForkJoinPool (or custom executor).
