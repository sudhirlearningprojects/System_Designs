# Future and CompletableFuture - Complete Deep Dive

## Table of Contents
1. [Future - Theory and Basics](#future-theory)
2. [Future Methods](#future-methods)
3. [Future Limitations](#future-limitations)
4. [CompletableFuture - Theory](#completablefuture-theory)
5. [CompletableFuture Creation Methods](#creation-methods)
6. [CompletableFuture Transformation Methods](#transformation-methods)
7. [CompletableFuture Combination Methods](#combination-methods)
8. [CompletableFuture Exception Handling](#exception-handling)
9. [CompletableFuture Completion Methods](#completion-methods)
10. [Real-World Use Cases](#use-cases)

---

## 1. Future - Theory and Basics

### What is Future?

**Future** represents the result of an asynchronous computation. It's a placeholder for a value that will be available in the future.

```
┌─────────────────────────────────────────────────┐
│  Main Thread                                    │
│  ┌──────────────────────────────────────────┐  │
│  │ Submit Task → Future<String>             │  │
│  │ Continue other work...                   │  │
│  │ future.get() → Blocks until result ready │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Worker Thread                                  │
│  ┌──────────────────────────────────────────┐  │
│  │ Execute task                             │  │
│  │ Return result                            │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Basic Example

```java
import java.util.concurrent.*;

public class FutureBasics {
    
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Submit task, get Future
        Future<String> future = executor.submit(() -> {
            Thread.sleep(2000);
            return "Task completed";
        });
        
        System.out.println("Task submitted, doing other work...");
        
        // Block until result is ready
        String result = future.get();
        System.out.println("Result: " + result);
        
        executor.shutdown();
    }
}
```

**Output**:
```
Task submitted, doing other work...
(2 seconds delay)
Result: Task completed
```

---

## 2. Future Methods

### Method 1: get()

**Blocks** until the result is available.

```java
Future<String> future = executor.submit(() -> "Result");

// Blocks indefinitely
String result = future.get();
```

---

### Method 2: get(timeout, unit)

**Blocks** with timeout.

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Result";
});

try {
    // Wait max 2 seconds
    String result = future.get(2, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Task timed out");
}
```

---

### Method 3: isDone()

**Non-blocking** check if task is complete.

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "Result";
});

while (!future.isDone()) {
    System.out.println("Task still running...");
    Thread.sleep(500);
}

String result = future.get(); // Won't block
```

---

### Method 4: cancel(mayInterruptIfRunning)

**Cancel** the task.

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Result";
});

// Cancel after 1 second
Thread.sleep(1000);
boolean cancelled = future.cancel(true);

if (cancelled) {
    System.out.println("Task cancelled");
}

// Throws CancellationException
try {
    future.get();
} catch (CancellationException e) {
    System.out.println("Task was cancelled");
}
```

---

### Method 5: isCancelled()

**Check** if task was cancelled.

```java
Future<String> future = executor.submit(() -> "Result");
future.cancel(true);

if (future.isCancelled()) {
    System.out.println("Task is cancelled");
}
```

---

### Complete Future Methods Example

```java
import java.util.concurrent.*;

public class FutureMethodsDemo {
    
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Example 1: get()
        Future<Integer> future1 = executor.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
        System.out.println("Result: " + future1.get());
        
        // Example 2: get(timeout)
        Future<String> future2 = executor.submit(() -> {
            Thread.sleep(5000);
            return "Slow task";
        });
        try {
            future2.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Timeout!");
        }
        
        // Example 3: isDone()
        Future<String> future3 = executor.submit(() -> {
            Thread.sleep(1000);
            return "Done";
        });
        System.out.println("Is done? " + future3.isDone());
        Thread.sleep(1500);
        System.out.println("Is done? " + future3.isDone());
        
        // Example 4: cancel()
        Future<String> future4 = executor.submit(() -> {
            Thread.sleep(5000);
            return "Never completes";
        });
        future4.cancel(true);
        System.out.println("Is cancelled? " + future4.isCancelled());
        
        executor.shutdown();
    }
}
```

---

## 3. Future Limitations

### Problem 1: Cannot Chain Operations

```java
// Want to: fetch user → fetch orders → process orders
// With Future: Must block at each step

Future<User> userFuture = executor.submit(() -> fetchUser());
User user = userFuture.get(); // BLOCKS

Future<List<Order>> ordersFuture = executor.submit(() -> fetchOrders(user));
List<Order> orders = ordersFuture.get(); // BLOCKS

Future<Report> reportFuture = executor.submit(() -> processOrders(orders));
Report report = reportFuture.get(); // BLOCKS
```

---

### Problem 2: Cannot Combine Multiple Futures

```java
// Want to: Run 3 tasks in parallel, combine results
// With Future: Must block on each

Future<String> future1 = executor.submit(() -> "Result1");
Future<String> future2 = executor.submit(() -> "Result2");
Future<String> future3 = executor.submit(() -> "Result3");

String r1 = future1.get(); // BLOCKS
String r2 = future2.get(); // BLOCKS
String r3 = future3.get(); // BLOCKS

String combined = r1 + r2 + r3;
```

---

### Problem 3: No Exception Handling

```java
Future<String> future = executor.submit(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
});

try {
    String result = future.get(); // Exception wrapped in ExecutionException
} catch (ExecutionException e) {
    // Must unwrap and handle
    Throwable cause = e.getCause();
}
```

---

### Problem 4: No Callbacks

```java
// Want to: Execute callback when task completes
// With Future: Must poll or block

Future<String> future = executor.submit(() -> "Result");

// Option 1: Block
String result = future.get();
processResult(result);

// Option 2: Poll (inefficient)
while (!future.isDone()) {
    Thread.sleep(100);
}
processResult(future.get());
```

---

### Problem 5: Cannot Manually Complete

```java
// Want to: Manually set result
// With Future: Not possible

Future<String> future = executor.submit(() -> "Result");
// Cannot do: future.complete("Manual result");
```

---

## 4. CompletableFuture - Theory

### What is CompletableFuture?

**CompletableFuture** is an enhanced Future that supports:
- **Non-blocking** operations
- **Chaining** transformations
- **Combining** multiple futures
- **Exception handling**
- **Manual completion**
- **Callbacks**

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│              CompletableFuture<T>                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Implements: Future<T>, CompletionStage<T>        │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  Features:                                              │
│  • Non-blocking transformations (thenApply)             │
│  • Chaining (thenCompose)                               │
│  • Combining (thenCombine, allOf, anyOf)                │
│  • Exception handling (exceptionally, handle)           │
│  • Callbacks (thenAccept, thenRun)                      │
│  • Manual completion (complete, completeExceptionally)  │
└─────────────────────────────────────────────────────────┘
```

### Basic Example

```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureBasics {
    
    public static void main(String[] args) {
        // Create and complete
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return "Hello";
        });
        
        // Chain transformations (non-blocking)
        future
            .thenApply(s -> s + " World")
            .thenApply(String::toUpperCase)
            .thenAccept(System.out::println);
        
        // Wait for completion
        future.join();
    }
}
```

**Output**:
```
HELLO WORLD
```

---

## 5. CompletableFuture Creation Methods

### Method 1: completedFuture()

Create already completed future.

```java
CompletableFuture<String> future = CompletableFuture.completedFuture("Result");
System.out.println(future.get()); // Immediately available
```

---

### Method 2: supplyAsync()

Run task asynchronously, return result.

```java
// Uses ForkJoinPool.commonPool()
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Async result";
});

// With custom executor
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    return "Custom executor result";
}, executor);
```

---

### Method 3: runAsync()

Run task asynchronously, no return value.

```java
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Task running");
});

future.join(); // Wait for completion
```

---

### Method 4: new CompletableFuture()

Create incomplete future, complete manually.

```java
CompletableFuture<String> future = new CompletableFuture<>();

// Complete manually later
new Thread(() -> {
    try {
        Thread.sleep(1000);
        future.complete("Manual result");
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
}).start();

System.out.println(future.get()); // Waits for manual completion
```

---

### Method 5: failedFuture()

Create already failed future (Java 9+).

```java
CompletableFuture<String> future = CompletableFuture.failedFuture(
    new RuntimeException("Error")
);

try {
    future.get();
} catch (ExecutionException e) {
    System.out.println("Error: " + e.getCause().getMessage());
}
```

---

## 6. CompletableFuture Transformation Methods

### Method 1: thenApply()

Transform result (like map).

```java
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 5);

CompletableFuture<Integer> result = future.thenApply(num -> num * 2);

System.out.println(result.get()); // 10
```

**Chain multiple transformations**:

```java
CompletableFuture.supplyAsync(() -> "hello")
    .thenApply(String::toUpperCase)
    .thenApply(s -> s + " WORLD")
    .thenApply(String::length)
    .thenAccept(System.out::println); // 11
```

---

### Method 2: thenApplyAsync()

Transform result asynchronously.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello");

// Runs in different thread
future.thenApplyAsync(s -> {
    System.out.println("Thread: " + Thread.currentThread().getName());
    return s.toUpperCase();
});
```

---

### Method 3: thenAccept()

Consume result, no return value.

```java
CompletableFuture.supplyAsync(() -> "Result")
    .thenAccept(result -> {
        System.out.println("Got: " + result);
    });
```

---

### Method 4: thenRun()

Run action after completion, no access to result.

```java
CompletableFuture.supplyAsync(() -> "Result")
    .thenRun(() -> {
        System.out.println("Task completed");
    });
```

---

### Method 5: thenCompose()

Chain dependent futures (like flatMap).

```java
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> fetchUser());

CompletableFuture<List<Order>> ordersFuture = userFuture.thenCompose(user -> 
    CompletableFuture.supplyAsync(() -> fetchOrders(user))
);

// Chaining multiple dependent calls
CompletableFuture<Report> reportFuture = CompletableFuture
    .supplyAsync(() -> fetchUser())
    .thenCompose(user -> CompletableFuture.supplyAsync(() -> fetchOrders(user)))
    .thenCompose(orders -> CompletableFuture.supplyAsync(() -> generateReport(orders)));
```

---

### thenApply vs thenCompose

```java
// thenApply: Returns T
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 5)
    .thenApply(num -> num * 2); // Returns Integer

// thenCompose: Returns CompletableFuture<T>
CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 5)
    .thenCompose(num -> CompletableFuture.supplyAsync(() -> num * 2)); // Returns CF<Integer>
```

---

## 7. CompletableFuture Combination Methods

### Method 1: thenCombine()

Combine two independent futures.

```java
CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> a + b);

System.out.println(combined.get()); // 30
```

**Real-world example**:

```java
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> fetchUser());
CompletableFuture<Account> accountFuture = CompletableFuture.supplyAsync(() -> fetchAccount());

CompletableFuture<UserProfile> profile = userFuture.thenCombine(
    accountFuture,
    (user, account) -> new UserProfile(user, account)
);
```

---

### Method 2: thenAcceptBoth()

Combine two futures, consume results.

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

future1.thenAcceptBoth(future2, (s1, s2) -> {
    System.out.println(s1 + " " + s2);
});
```

---

### Method 3: allOf()

Wait for all futures to complete.

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Task1");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "Task2");
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "Task3");

CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

allFutures.join(); // Wait for all

// Get results
String r1 = future1.get();
String r2 = future2.get();
String r3 = future3.get();
```

**Collect all results**:

```java
List<CompletableFuture<String>> futures = Arrays.asList(
    CompletableFuture.supplyAsync(() -> "Task1"),
    CompletableFuture.supplyAsync(() -> "Task2"),
    CompletableFuture.supplyAsync(() -> "Task3")
);

CompletableFuture<List<String>> allResults = CompletableFuture
    .allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList())
    );

System.out.println(allResults.get()); // [Task1, Task2, Task3]
```

---

### Method 4: anyOf()

Wait for any future to complete.

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(2000);
    return "Slow";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(500);
    return "Fast";
});

CompletableFuture<Object> fastest = CompletableFuture.anyOf(future1, future2);

System.out.println(fastest.get()); // "Fast"
```

---

### Method 5: applyToEither()

Use result from whichever completes first.

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(1000);
    return "Service1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(500);
    return "Service2";
});

CompletableFuture<String> result = future1.applyToEither(future2, s -> s.toUpperCase());

System.out.println(result.get()); // "SERVICE2"
```

---

## 8. CompletableFuture Exception Handling

### Method 1: exceptionally()

Handle exception, provide fallback.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
});

CompletableFuture<String> handled = future.exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return "Fallback value";
});

System.out.println(handled.get());
```

---

### Method 2: handle()

Handle both success and failure.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
});

CompletableFuture<String> handled = future.handle((result, ex) -> {
    if (ex != null) {
        return "Error: " + ex.getMessage();
    }
    return "Result: " + result;
});

System.out.println(handled.get());
```

---

### Method 3: whenComplete()

Perform action on completion (success or failure).

```java
CompletableFuture.supplyAsync(() -> {
    return "Result";
})
.whenComplete((result, ex) -> {
    if (ex != null) {
        System.out.println("Failed: " + ex.getMessage());
    } else {
        System.out.println("Success: " + result);
    }
});
```

---

### Exception Handling Chain

```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Step 1 failed");
})
.thenApply(s -> s.toUpperCase())
.exceptionally(ex -> {
    System.out.println("Caught: " + ex.getMessage());
    return "Recovered";
})
.thenApply(s -> s + " - Processed")
.thenAccept(System.out::println);
```

**Output**:
```
Caught: java.lang.RuntimeException: Step 1 failed
Recovered - Processed
```

---

## 9. CompletableFuture Completion Methods

### Method 1: complete()

Manually complete with value.

```java
CompletableFuture<String> future = new CompletableFuture<>();

new Thread(() -> {
    try {
        Thread.sleep(1000);
        future.complete("Done");
    } catch (Exception e) {}
}).start();

System.out.println(future.get()); // Waits, then prints "Done"
```

---

### Method 2: completeExceptionally()

Manually complete with exception.

```java
CompletableFuture<String> future = new CompletableFuture<>();

new Thread(() -> {
    try {
        Thread.sleep(1000);
        future.completeExceptionally(new RuntimeException("Failed"));
    } catch (Exception e) {}
}).start();

try {
    future.get();
} catch (ExecutionException e) {
    System.out.println("Error: " + e.getCause().getMessage());
}
```

---

### Method 3: completeOnTimeout()

Complete with default value on timeout (Java 9+).

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(5000);
    return "Result";
})
.completeOnTimeout("Timeout value", 2, TimeUnit.SECONDS);

System.out.println(future.get()); // "Timeout value"
```

---

### Method 4: orTimeout()

Fail with TimeoutException on timeout (Java 9+).

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(5000);
    return "Result";
})
.orTimeout(2, TimeUnit.SECONDS);

try {
    future.get();
} catch (ExecutionException e) {
    System.out.println("Timeout!");
}
```

---

### Method 5: obtrudeValue() / obtrudeException()

Forcefully set value/exception.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    Thread.sleep(5000);
    return "Original";
});

// Force different value
future.obtrudeValue("Forced value");

System.out.println(future.get()); // "Forced value"
```

---

## 10. Real-World Use Cases

### Use Case 1: Parallel API Calls

```java
public class ParallelAPICalls {
    
    public CompletableFuture<Dashboard> getDashboard(String userId) {
        CompletableFuture<User> userFuture = 
            CompletableFuture.supplyAsync(() -> fetchUser(userId));
        
        CompletableFuture<List<Order>> ordersFuture = 
            CompletableFuture.supplyAsync(() -> fetchOrders(userId));
        
        CompletableFuture<Account> accountFuture = 
            CompletableFuture.supplyAsync(() -> fetchAccount(userId));
        
        return userFuture.thenCombine(ordersFuture, (user, orders) -> 
            new Pair<>(user, orders)
        )
        .thenCombine(accountFuture, (pair, account) -> 
            new Dashboard(pair.getKey(), pair.getValue(), account)
        );
    }
}
```

---

### Use Case 2: Retry Logic

```java
public class RetryExample {
    
    public CompletableFuture<String> fetchWithRetry(String url, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> httpGet(url))
            .exceptionally(ex -> {
                if (maxRetries > 0) {
                    return fetchWithRetry(url, maxRetries - 1).join();
                }
                throw new RuntimeException("Max retries exceeded", ex);
            });
    }
}
```

---

### Use Case 3: Timeout Handling

```java
public class TimeoutExample {
    
    public CompletableFuture<String> fetchWithTimeout(String url) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
            httpGet(url)
        );
        
        CompletableFuture<String> timeout = new CompletableFuture<>();
        scheduler.schedule(() -> 
            timeout.completeExceptionally(new TimeoutException()),
            5, TimeUnit.SECONDS
        );
        
        return future.applyToEither(timeout, Function.identity());
    }
}
```

---

### Use Case 4: Fallback Chain

```java
public class FallbackExample {
    
    public CompletableFuture<String> fetchWithFallback(String userId) {
        return CompletableFuture.supplyAsync(() -> fetchFromPrimaryDB(userId))
            .exceptionally(ex -> fetchFromSecondaryDB(userId))
            .exceptionally(ex -> fetchFromCache(userId))
            .exceptionally(ex -> "Default value");
    }
}
```

---

### Use Case 5: Fan-Out/Fan-In Pattern

```java
public class FanOutFanIn {
    
    public CompletableFuture<Report> generateReport(List<String> userIds) {
        List<CompletableFuture<UserData>> futures = userIds.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> fetchUserData(id)))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
            )
            .thenApply(this::aggregateReport);
    }
}
```

---

## Summary - Method Categories

### Creation
- `completedFuture()`, `supplyAsync()`, `runAsync()`, `new CompletableFuture()`

### Transformation
- `thenApply()`, `thenApplyAsync()`, `thenCompose()`

### Consumption
- `thenAccept()`, `thenRun()`

### Combination
- `thenCombine()`, `allOf()`, `anyOf()`, `applyToEither()`

### Exception Handling
- `exceptionally()`, `handle()`, `whenComplete()`

### Completion
- `complete()`, `completeExceptionally()`, `completeOnTimeout()`, `orTimeout()`

### Blocking
- `get()`, `get(timeout)`, `join()`, `getNow()`
