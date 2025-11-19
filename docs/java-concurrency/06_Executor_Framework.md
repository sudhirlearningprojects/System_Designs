# Executor Framework

## Table of Contents
- [Introduction to Executor Framework](#introduction-to-executor-framework)
- [Thread Pools](#thread-pools)
- [ExecutorService](#executorservice)
- [Future and Callable](#future-and-callable)
- [CompletableFuture](#completablefuture)
- [ScheduledExecutorService](#scheduledexecutorservice)
- [Custom Thread Pools](#custom-thread-pools)
- [Best Practices](#best-practices)

## Introduction to Executor Framework

### Theoretical Foundation

The **Executor Framework** is based on several key design patterns and principles:

1. **Command Pattern**: Tasks are encapsulated as objects that can be executed
2. **Producer-Consumer Pattern**: Task submission is decoupled from execution
3. **Thread Pool Pattern**: Reuse threads to avoid creation/destruction overhead
4. **Future Pattern**: Asynchronous computation with result retrieval

### Core Concepts

1. **Task**: Unit of work (Runnable or Callable)
2. **Executor**: Interface for executing tasks
3. **ExecutorService**: Extended interface with lifecycle management
4. **Thread Pool**: Collection of worker threads
5. **Work Queue**: Queue holding tasks waiting for execution
6. **Future**: Handle to asynchronous computation result

### Benefits Over Raw Threads

```java
import java.util.concurrent.*;

public class ExecutorBenefits {
    
    // Problems with raw threads
    public static void rawThreadProblems() {
        // 1. Thread creation overhead
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                // Short task
                System.out.println("Task executed");
            }).start(); // Creates 1000 threads!
        }
        
        // 2. No control over thread lifecycle
        // 3. No way to get results from tasks
        // 4. Difficult to handle exceptions
        // 5. No built-in task queuing
    }
    
    // Solutions with Executor Framework
    public static void executorSolutions() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // 1. Thread reuse - only 10 threads for 1000 tasks
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                System.out.println("Task executed by: " + Thread.currentThread().getName());
            });
        }
        
        // 2. Controlled lifecycle
        executor.shutdown();
        
        // 3. Can get results using Future
        // 4. Exception handling built-in
        // 5. Built-in task queuing
    }
}
```

## Thread Pools

### Thread Pool Theory

**Thread pools** implement the **Worker Thread Pattern**:
1. **Fixed number of worker threads** wait for tasks
2. **Work queue** holds pending tasks
3. **Task distribution** happens automatically
4. **Thread reuse** eliminates creation/destruction overhead

### Thread Pool Sizing Theory

**Optimal thread pool size** depends on:
1. **CPU-bound tasks**: N_threads = N_CPU + 1
2. **I/O-bound tasks**: N_threads = N_CPU × (1 + W/C)
   - W = wait time
   - C = compute time
3. **Little's Law**: N = λ × W (throughput × response time)

```java
import java.util.concurrent.*;

public class ThreadPoolTypes {
    
    public static void fixedThreadPool() {
        // Fixed number of threads
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        System.out.println("=== Fixed Thread Pool (4 threads) ===");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
    
    public static void cachedThreadPool() {
        // Creates threads as needed, reuses idle threads
        ExecutorService executor = Executors.newCachedThreadPool();
        
        System.out.println("=== Cached Thread Pool ===");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
    
    public static void singleThreadExecutor() {
        // Single worker thread - guarantees sequential execution
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        System.out.println("=== Single Thread Executor ===");
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed sequentially");
            });
        }
        
        executor.shutdown();
    }
    
    public static void workStealingPool() {
        // Fork-Join based pool with work stealing
        ExecutorService executor = Executors.newWorkStealingPool();
        
        System.out.println("=== Work Stealing Pool ===");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + 
                    Thread.currentThread().getName());
                // Simulate variable work
                try {
                    Thread.sleep(taskId * 100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException {
        fixedThreadPool();
        Thread.sleep(2000);
        
        cachedThreadPool();
        Thread.sleep(2000);
        
        singleThreadExecutor();
        Thread.sleep(2000);
        
        workStealingPool();
        Thread.sleep(5000);
    }
}
```

### Thread Pool Internal Structure

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolInternals {
    
    public static void demonstrateQueueing() throws InterruptedException {
        // Small thread pool to demonstrate queuing
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // core pool size
            2,                              // maximum pool size
            60L, TimeUnit.SECONDS,          // keep alive time
            new LinkedBlockingQueue<>(3)    // work queue with capacity 3
        );
        
        AtomicInteger taskCounter = new AtomicInteger(0);
        
        // Submit more tasks than pool + queue can handle
        for (int i = 0; i < 8; i++) {
            try {
                final int taskId = i;
                executor.submit(() -> {
                    int taskNum = taskCounter.incrementAndGet();
                    System.out.println("Executing task " + taskId + 
                        " (execution order: " + taskNum + ") by " + 
                        Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                System.out.println("Submitted task " + i + 
                    ", Active: " + executor.getActiveCount() + 
                    ", Queue: " + executor.getQueue().size());
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + i + " rejected: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateQueueing();
    }
}
```

## ExecutorService

### Lifecycle Management

```java
import java.util.concurrent.*;
import java.util.List;

public class ExecutorLifecycle {
    
    public static void gracefulShutdown() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit some long-running tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " started");
                    Thread.sleep(3000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    System.out.println("Task " + taskId + " interrupted");
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        System.out.println("Initiating shutdown...");
        executor.shutdown(); // No new tasks accepted
        
        // Wait for existing tasks to complete
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("Tasks didn't complete in time, forcing shutdown...");
            List<Runnable> pendingTasks = executor.shutdownNow();
            System.out.println("Pending tasks: " + pendingTasks.size());
            
            // Wait a bit more for tasks to respond to interruption
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.out.println("Some tasks didn't respond to interruption");
            }
        }
        
        System.out.println("Executor shutdown complete");
    }
    
    public static void immediateShutdown() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " started");
                    Thread.sleep(5000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    System.out.println("Task " + taskId + " interrupted");
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Immediate shutdown
        List<Runnable> pendingTasks = executor.shutdownNow();
        System.out.println("Forced shutdown, pending tasks: " + pendingTasks.size());
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Graceful Shutdown ===");
        gracefulShutdown();
        
        Thread.sleep(1000);
        
        System.out.println("\n=== Immediate Shutdown ===");
        immediateShutdown();
    }
}
```

### Task Submission Methods

```java
import java.util.concurrent.*;
import java.util.List;
import java.util.Arrays;

public class TaskSubmission {
    
    public static void submitMethods() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // 1. execute() - fire and forget
        executor.execute(() -> System.out.println("Execute: Fire and forget"));
        
        // 2. submit(Runnable) - returns Future<?>
        Future<?> future1 = executor.submit(() -> System.out.println("Submit Runnable"));
        future1.get(); // Wait for completion
        
        // 3. submit(Runnable, result) - returns Future<T>
        String result = "Task completed";
        Future<String> future2 = executor.submit(() -> 
            System.out.println("Submit with result"), result);
        System.out.println("Result: " + future2.get());
        
        // 4. submit(Callable) - returns Future<T>
        Future<Integer> future3 = executor.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
        System.out.println("Callable result: " + future3.get());
        
        executor.shutdown();
    }
    
    public static void bulkOperations() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Create multiple tasks
        List<Callable<String>> tasks = Arrays.asList(
            () -> { Thread.sleep(1000); return "Task 1"; },
            () -> { Thread.sleep(2000); return "Task 2"; },
            () -> { Thread.sleep(1500); return "Task 3"; }
        );
        
        // invokeAll - wait for all tasks to complete
        System.out.println("=== invokeAll ===");
        List<Future<String>> futures = executor.invokeAll(tasks);
        for (Future<String> future : futures) {
            System.out.println("Result: " + future.get());
        }
        
        // invokeAny - return result of first completed task
        System.out.println("=== invokeAny ===");
        String firstResult = executor.invokeAny(tasks);
        System.out.println("First result: " + firstResult);
        
        executor.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== Submit Methods ===");
        submitMethods();
        
        System.out.println("\n=== Bulk Operations ===");
        bulkOperations();
    }
}
```

## Future and Callable

### Future Pattern Theory

The **Future Pattern** represents:
1. **Asynchronous computation**: Task executes in background
2. **Placeholder for result**: Future holds eventual result
3. **Non-blocking submission**: Client continues after submitting task
4. **Blocking retrieval**: get() blocks until result is available

```java
import java.util.concurrent.*;
import java.util.Random;

public class FutureExample {
    
    public static void basicFuture() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Submit a Callable task
        Future<String> future = executor.submit(() -> {
            Thread.sleep(2000);
            return "Hello from Future!";
        });
        
        System.out.println("Task submitted, doing other work...");
        
        // Do other work while task executes
        for (int i = 0; i < 5; i++) {
            System.out.println("Doing other work: " + i);
            Thread.sleep(300);
        }
        
        // Get the result (blocks if not ready)
        String result = future.get();
        System.out.println("Future result: " + result);
        
        executor.shutdown();
    }
    
    public static void futureWithTimeout() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(5000); // Long running task
            return 42;
        });
        
        try {
            // Wait maximum 2 seconds for result
            Integer result = future.get(2, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (TimeoutException e) {
            System.out.println("Task timed out, cancelling...");
            future.cancel(true); // Interrupt the task
        } catch (ExecutionException e) {
            System.out.println("Task failed: " + e.getCause());
        }
        
        executor.shutdown();
    }
    
    public static void futureState() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> {
            Thread.sleep(2000);
            return "Completed";
        });
        
        // Check future state
        System.out.println("Is done: " + future.isDone());
        System.out.println("Is cancelled: " + future.isCancelled());
        
        // Wait a bit
        Thread.sleep(1000);
        System.out.println("After 1 second - Is done: " + future.isDone());
        
        // Get result
        String result = future.get();
        System.out.println("Result: " + result);
        System.out.println("Final - Is done: " + future.isDone());
        
        executor.shutdown();
    }
    
    public static void multipleFutures() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Random random = new Random();
        
        // Submit multiple tasks
        Future<Integer>[] futures = new Future[5];
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            futures[i] = executor.submit(() -> {
                int sleepTime = 1000 + random.nextInt(2000);
                Thread.sleep(sleepTime);
                return taskId * 10;
            });
        }
        
        // Collect results as they become available
        for (int i = 0; i < 5; i++) {
            Integer result = futures[i].get();
            System.out.println("Task " + i + " result: " + result);
        }
        
        executor.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== Basic Future ===");
        basicFuture();
        
        System.out.println("\n=== Future with Timeout ===");
        futureWithTimeout();
        
        System.out.println("\n=== Future State ===");
        futureState();
        
        System.out.println("\n=== Multiple Futures ===");
        multipleFutures();
    }
}
```

### Exception Handling with Future

```java
import java.util.concurrent.*;

public class FutureExceptionHandling {
    
    public static void handlingExceptions() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Task that throws exception
        Future<String> future = executor.submit(() -> {
            Thread.sleep(1000);
            if (Math.random() > 0.5) {
                throw new RuntimeException("Random failure!");
            }
            return "Success";
        });
        
        try {
            String result = future.get();
            System.out.println("Result: " + result);
        } catch (ExecutionException e) {
            System.out.println("Task failed with: " + e.getCause().getClass().getSimpleName());
            System.out.println("Error message: " + e.getCause().getMessage());
        }
        
        executor.shutdown();
    }
    
    public static void cancellation() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Future<String> future = executor.submit(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Task was cancelled");
                    }
                    Thread.sleep(500);
                    System.out.println("Working... " + i);
                }
                return "Completed";
            } catch (InterruptedException e) {
                System.out.println("Task interrupted");
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task cancelled", e);
            }
        });
        
        // Let it run for a bit
        Thread.sleep(2000);
        
        // Cancel the task
        boolean cancelled = future.cancel(true);
        System.out.println("Cancellation successful: " + cancelled);
        
        try {
            future.get();
        } catch (CancellationException e) {
            System.out.println("Task was cancelled");
        } catch (ExecutionException e) {
            System.out.println("Task failed: " + e.getCause());
        }
        
        executor.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Exception Handling ===");
        handlingExceptions();
        
        System.out.println("\n=== Cancellation ===");
        cancellation();
    }
}
```

## CompletableFuture

### CompletableFuture Theory

**CompletableFuture** extends Future with:
1. **Composability**: Chain operations together
2. **Asynchronous callbacks**: Non-blocking result handling
3. **Exception handling**: Built-in error handling
4. **Combining futures**: Merge multiple async operations

```java
import java.util.concurrent.*;
import java.util.function.Supplier;

public class CompletableFutureBasics {
    
    public static void basicUsage() throws ExecutionException, InterruptedException {
        // Create completed future
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("Hello");
        System.out.println("Completed future: " + completedFuture.get());
        
        // Create future with supplier
        CompletableFuture<String> supplierFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "World";
        });
        
        System.out.println("Supplier future: " + supplierFuture.get());
        
        // Create future with runnable
        CompletableFuture<Void> runnableFuture = CompletableFuture.runAsync(() -> {
            System.out.println("Running async task");
        });
        
        runnableFuture.get(); // Wait for completion
    }
    
    public static void chainingOperations() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> "Hello")
            .thenApply(s -> s + " World")
            .thenApply(String::toUpperCase)
            .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + "!"));
        
        System.out.println("Chained result: " + future.get());
    }
    
    public static void asyncCallbacks() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Async Result";
        });
        
        // Non-blocking callbacks
        future.thenAccept(result -> 
            System.out.println("Received: " + result));
        
        future.thenRun(() -> 
            System.out.println("Task completed"));
        
        // Wait for completion
        future.join();
    }
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== Basic Usage ===");
        basicUsage();
        
        System.out.println("\n=== Chaining Operations ===");
        chainingOperations();
        
        System.out.println("\n=== Async Callbacks ===");
        asyncCallbacks();
    }
}
```

### Advanced CompletableFuture Operations

```java
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdvancedCompletableFuture {
    
    public static void combiningFutures() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Hello";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "World";
        });
        
        // Combine two futures
        CompletableFuture<String> combined = future1.thenCombine(future2, 
            (s1, s2) -> s1 + " " + s2);
        
        System.out.println("Combined: " + combined.get());
        
        // Wait for both to complete
        CompletableFuture<Void> both = CompletableFuture.allOf(future1, future2);
        both.thenRun(() -> System.out.println("Both completed"));
        
        // Wait for any to complete
        CompletableFuture<Object> any = CompletableFuture.anyOf(future1, future2);
        System.out.println("First completed: " + any.get());
    }
    
    public static void exceptionHandling() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("Random failure");
            }
            return "Success";
        }).exceptionally(throwable -> {
            System.out.println("Exception occurred: " + throwable.getMessage());
            return "Default value";
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.out.println("Task failed");
            } else {
                System.out.println("Task succeeded with: " + result);
            }
        });
        
        System.out.println("Final result: " + future.join());
    }
    
    public static void parallelProcessing() {
        List<String> urls = Arrays.asList(
            "http://example1.com",
            "http://example2.com", 
            "http://example3.com",
            "http://example4.com"
        );
        
        // Process URLs in parallel
        List<CompletableFuture<String>> futures = urls.stream()
            .map(url -> CompletableFuture.supplyAsync(() -> fetchData(url)))
            .collect(Collectors.toList());
        
        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        CompletableFuture<List<String>> allResults = allFutures.thenApply(v ->
            futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
        
        List<String> results = allResults.join();
        System.out.println("All results: " + results);
    }
    
    private static String fetchData(String url) {
        sleep(1000 + (int)(Math.random() * 1000));
        return "Data from " + url;
    }
    
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== Combining Futures ===");
        combiningFutures();
        
        System.out.println("\n=== Exception Handling ===");
        exceptionHandling();
        
        System.out.println("\n=== Parallel Processing ===");
        parallelProcessing();
    }
}
```

## ScheduledExecutorService

### Scheduling Theory

**ScheduledExecutorService** provides:
1. **Delayed execution**: Execute tasks after a delay
2. **Periodic execution**: Execute tasks repeatedly
3. **Fixed rate vs fixed delay**: Different timing semantics
4. **Cron-like scheduling**: Time-based task scheduling

```java
import java.util.concurrent.*;
import java.time.LocalTime;

public class ScheduledExecutorExample {
    
    public static void delayedExecution() throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        System.out.println("Current time: " + LocalTime.now());
        
        // Execute after delay
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            System.out.println("Delayed task executed at: " + LocalTime.now());
        }, 3, TimeUnit.SECONDS);
        
        // Execute Callable after delay
        ScheduledFuture<String> callableFuture = scheduler.schedule(() -> {
            return "Delayed result at: " + LocalTime.now();
        }, 2, TimeUnit.SECONDS);
        
        try {
            System.out.println(callableFuture.get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);
    }
    
    public static void periodicExecution() throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        System.out.println("Starting periodic tasks at: " + LocalTime.now());
        
        // Fixed rate - executes every 2 seconds regardless of execution time
        ScheduledFuture<?> fixedRateTask = scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Fixed rate task at: " + LocalTime.now());
            try {
                Thread.sleep(1000); // Task takes 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        // Fixed delay - waits 2 seconds after each execution completes
        ScheduledFuture<?> fixedDelayTask = scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("Fixed delay task at: " + LocalTime.now());
            try {
                Thread.sleep(1000); // Task takes 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        // Let tasks run for 10 seconds
        Thread.sleep(10000);
        
        // Cancel tasks
        fixedRateTask.cancel(false);
        fixedDelayTask.cancel(false);
        
        scheduler.shutdown();
    }
    
    public static void taskMonitoring() throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Monitoring task running...");
        }, 0, 1, TimeUnit.SECONDS);
        
        // Monitor the task
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1500);
            System.out.println("Task cancelled: " + future.isCancelled());
            System.out.println("Task done: " + future.isDone());
            System.out.println("Delay until next execution: " + 
                future.getDelay(TimeUnit.MILLISECONDS) + "ms");
        }
        
        future.cancel(false);
        scheduler.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Delayed Execution ===");
        delayedExecution();
        
        System.out.println("\n=== Periodic Execution ===");
        periodicExecution();
        
        System.out.println("\n=== Task Monitoring ===");
        taskMonitoring();
    }
}
```

## Custom Thread Pools

### ThreadPoolExecutor Theory

**ThreadPoolExecutor** parameters:
1. **corePoolSize**: Minimum number of threads
2. **maximumPoolSize**: Maximum number of threads
3. **keepAliveTime**: How long excess threads stay alive
4. **workQueue**: Queue for holding tasks
5. **threadFactory**: Creates new threads
6. **rejectionHandler**: Handles rejected tasks

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPools {
    
    public static void customThreadPoolExecutor() throws InterruptedException {
        // Custom thread factory
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "CustomPool-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
        
        // Custom rejection handler
        RejectedExecutionHandler rejectionHandler = (r, executor) -> {
            System.out.println("Task rejected: " + r.toString());
            System.out.println("Executor: " + executor.toString());
        };
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                                    // core pool size
            4,                                    // maximum pool size
            60L, TimeUnit.SECONDS,                // keep alive time
            new ArrayBlockingQueue<>(2),          // bounded queue
            threadFactory,                        // custom thread factory
            rejectionHandler                      // custom rejection handler
        );
        
        // Submit tasks to test behavior
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " executing on " + 
                        Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                System.out.println("Submitted task " + taskId + 
                    " - Pool size: " + executor.getPoolSize() + 
                    ", Active: " + executor.getActiveCount() + 
                    ", Queue: " + executor.getQueue().size());
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + taskId + " rejected");
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
    
    public static void differentQueueTypes() throws InterruptedException {
        System.out.println("=== LinkedBlockingQueue (unbounded) ===");
        testWithQueue(new LinkedBlockingQueue<>());
        
        System.out.println("\n=== ArrayBlockingQueue (bounded) ===");
        testWithQueue(new ArrayBlockingQueue<>(2));
        
        System.out.println("\n=== SynchronousQueue (direct handoff) ===");
        testWithQueue(new SynchronousQueue<>());
        
        System.out.println("\n=== PriorityBlockingQueue ===");
        testWithPriorityQueue();
    }
    
    private static void testWithQueue(BlockingQueue<Runnable> queue) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 3, 60L, TimeUnit.SECONDS, queue);
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " executing");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + taskId + " rejected");
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    private static void testWithPriorityQueue() throws InterruptedException {
        // Priority queue requires Comparable tasks
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 60L, TimeUnit.SECONDS, 
            new PriorityBlockingQueue<>());
        
        // Submit tasks with different priorities
        for (int i = 5; i >= 1; i--) {
            final int priority = i;
            executor.submit(new PriorityTask(priority, () -> {
                System.out.println("Priority " + priority + " task executing");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    static class PriorityTask implements Runnable, Comparable<PriorityTask> {
        private final int priority;
        private final Runnable task;
        
        public PriorityTask(int priority, Runnable task) {
            this.priority = priority;
            this.task = task;
        }
        
        @Override
        public void run() {
            task.run();
        }
        
        @Override
        public int compareTo(PriorityTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Custom ThreadPoolExecutor ===");
        customThreadPoolExecutor();
        
        System.out.println("\n=== Different Queue Types ===");
        differentQueueTypes();
    }
}
```

## Best Practices

### 1. Proper Resource Management

```java
import java.util.concurrent.*;

public class ResourceManagement {
    
    // WRONG: Not shutting down executor
    public void wrongWay() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.submit(() -> System.out.println("Task"));
        // Missing shutdown - threads keep running!
    }
    
    // CORRECT: Proper shutdown
    public void correctWay() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            executor.submit(() -> System.out.println("Task"));
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // BEST: Try-with-resources (Java 19+)
    public void bestWay() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> System.out.println("Task"));
        } // Automatic shutdown
    }
}
```

### 2. Choosing the Right Thread Pool

```java
public class ThreadPoolSelection {
    
    // CPU-intensive tasks
    public void cpuIntensiveTasks() {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores + 1);
        // Use for computational tasks
    }
    
    // I/O-intensive tasks
    public void ioIntensiveTasks() {
        // More threads for I/O waiting
        ExecutorService executor = Executors.newCachedThreadPool();
        // Or custom pool with higher thread count
    }
    
    // Mixed workload
    public void mixedWorkload() {
        // Separate pools for different task types
        ExecutorService cpuPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
        ExecutorService ioPool = Executors.newCachedThreadPool();
    }
    
    // Scheduled tasks
    public void scheduledTasks() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        // Use for periodic or delayed tasks
    }
}
```

### 3. Exception Handling

```java
public class ExecutorExceptionHandling {
    
    public void properExceptionHandling() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Handle exceptions in Future
        Future<String> future = executor.submit(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("Task failed");
            }
            return "Success";
        });
        
        try {
            String result = future.get();
            System.out.println("Result: " + result);
        } catch (ExecutionException e) {
            System.out.println("Task failed: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Handle exceptions in Runnable with custom handler
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in " + thread.getName() + 
                ": " + exception.getMessage());
        });
        
        executor.shutdown();
    }
}
```

## Summary

- **Executor Framework** provides high-level abstraction for task execution
- **Thread pools** reuse threads and manage task queues efficiently
- **Future** represents asynchronous computation results
- **CompletableFuture** enables composable asynchronous programming
- **ScheduledExecutorService** handles delayed and periodic tasks
- **Custom thread pools** allow fine-tuned control over execution behavior
- **Proper resource management** is crucial for preventing thread leaks
- **Choose appropriate pool types** based on workload characteristics

## Next Steps

Continue to [Atomic Operations](07_Atomic_Operations.md) to learn about lock-free programming and atomic classes.