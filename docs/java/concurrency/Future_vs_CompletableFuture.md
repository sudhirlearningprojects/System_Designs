# Future vs CompletableFuture

## Table of Contents
1. [Introduction](#introduction)
2. [Quick Comparison](#quick-comparison)
3. [Future Interface](#future-interface)
4. [CompletableFuture](#completablefuture)
5. [Key Differences](#key-differences)
6. [Real-World Examples](#real-world-examples)
7. [When to Use What](#when-to-use-what)

---

## Introduction

### What is Future?

**Future** represents the result of an asynchronous computation. Introduced in **Java 5**.

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Result";
});
```

### What is CompletableFuture?

**CompletableFuture** is an enhanced Future with functional programming capabilities. Introduced in **Java 8**.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return "Result";
});
```

---

## Quick Comparison

| Feature | Future | CompletableFuture |
|---------|--------|-------------------|
| **Introduced** | Java 5 | Java 8 |
| **Manual completion** | ❌ No | ✅ Yes |
| **Chaining** | ❌ No | ✅ Yes |
| **Combining** | ❌ No | ✅ Yes |
| **Exception handling** | ❌ Limited | ✅ Rich |
| **Non-blocking callbacks** | ❌ No | ✅ Yes |
| **Functional style** | ❌ No | ✅ Yes |
| **Timeout support** | ❌ No (Java 8) | ✅ Yes (Java 9+) |

---

## Future Interface

### Basic Usage

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

// Submit task
Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "Hello World";
});

// Get result (blocking)
String result = future.get();  // Waits until complete
System.out.println(result);

executor.shutdown();
```

### Methods

```java
// Check if done
boolean isDone = future.isDone();

// Check if cancelled
boolean isCancelled = future.isCancelled();

// Cancel task
boolean cancelled = future.cancel(true);

// Get with timeout
String result = future.get(5, TimeUnit.SECONDS);
```

### Problems with Future

#### 1. Blocking get()

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Result";
});

// Blocks current thread for 5 seconds!
String result = future.get();
```

#### 2. No Chaining

```java
Future<String> future1 = executor.submit(() -> "Hello");

// Cannot chain - need to get() and submit again
String result1 = future1.get();  // Blocking!
Future<String> future2 = executor.submit(() -> result1 + " World");
String result2 = future2.get();  // Blocking again!
```

#### 3. No Combining

```java
Future<String> future1 = executor.submit(() -> "Hello");
Future<String> future2 = executor.submit(() -> "World");

// Cannot combine - must get() both
String result1 = future1.get();  // Blocking
String result2 = future2.get();  // Blocking
String combined = result1 + " " + result2;
```

#### 4. Limited Exception Handling

```java
Future<String> future = executor.submit(() -> {
    throw new RuntimeException("Error");
});

try {
    String result = future.get();  // Throws ExecutionException
} catch (ExecutionException e) {
    // Must manually handle
    System.out.println("Error: " + e.getCause().getMessage());
}
```

#### 5. No Callbacks

```java
Future<String> future = executor.submit(() -> "Result");

// No way to register callback
// Must poll or block
while (!future.isDone()) {
    Thread.sleep(100);  // Polling - inefficient!
}
String result = future.get();
```

---

## CompletableFuture

### Basic Usage

```java
// Create and run async
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return "Hello World";
});

// Non-blocking callback
future.thenAccept(result -> System.out.println(result));

// Or blocking get
String result = future.get();
```

### Creating CompletableFuture

```java
// 1. supplyAsync (returns value)
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");

// 2. runAsync (no return value)
CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> 
    System.out.println("Running")
);

// 3. Manual completion
CompletableFuture<String> future3 = new CompletableFuture<>();
future3.complete("Manual result");

// 4. Already completed
CompletableFuture<String> future4 = CompletableFuture.completedFuture("Done");
```

### Chaining Operations

#### thenApply() - Transform result

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(String::toUpperCase);

System.out.println(future.get());  // HELLO WORLD
```

#### thenAccept() - Consume result

```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenAccept(result -> System.out.println(result));
```

#### thenRun() - Run after completion

```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenRun(() -> System.out.println("Done"));
```

#### thenCompose() - Flatten nested futures

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));

System.out.println(future.get());  // Hello World
```

### Combining Multiple Futures

#### thenCombine() - Combine two futures

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

CompletableFuture<String> combined = future1.thenCombine(future2, 
    (s1, s2) -> s1 + " " + s2
);

System.out.println(combined.get());  // Hello World
```

#### allOf() - Wait for all

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "C");

CompletableFuture<Void> all = CompletableFuture.allOf(future1, future2, future3);
all.join();  // Wait for all to complete

System.out.println(future1.get() + future2.get() + future3.get());  // ABC
```

#### anyOf() - Wait for any

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Slow";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(100);
    return "Fast";
});

CompletableFuture<Object> any = CompletableFuture.anyOf(future1, future2);
System.out.println(any.get());  // Fast (completes first)
```

### Exception Handling

#### exceptionally() - Handle exception

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("Error");
    return "Success";
})
.exceptionally(ex -> "Recovered: " + ex.getMessage());

System.out.println(future.get());  // Recovered: Error
```

#### handle() - Handle both result and exception

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) throw new RuntimeException("Error");
    return "Success";
})
.handle((result, ex) -> {
    if (ex != null) {
        return "Error: " + ex.getMessage();
    }
    return result;
});

System.out.println(future.get());  // Error: Error
```

#### whenComplete() - Peek at result/exception

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
    .whenComplete((result, ex) -> {
        if (ex != null) {
            System.out.println("Failed: " + ex.getMessage());
        } else {
            System.out.println("Success: " + result);
        }
    });
```

### Timeout (Java 9+)

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    sleep(5000);
    return "Result";
})
.orTimeout(2, TimeUnit.SECONDS)  // Timeout after 2 seconds
.exceptionally(ex -> "Timeout!");

System.out.println(future.get());  // Timeout!
```

---

## Key Differences

### 1. Manual Completion

**Future:**
```java
// Cannot manually complete
Future<String> future = executor.submit(() -> "Result");
// No way to complete it manually
```

**CompletableFuture:**
```java
CompletableFuture<String> future = new CompletableFuture<>();

// Complete manually
future.complete("Manual result");

// Complete exceptionally
future.completeExceptionally(new RuntimeException("Error"));
```

---

### 2. Chaining

**Future:**
```java
// No chaining - must block
Future<String> future1 = executor.submit(() -> "Hello");
String result1 = future1.get();  // Blocking!

Future<String> future2 = executor.submit(() -> result1 + " World");
String result2 = future2.get();  // Blocking again!
```

**CompletableFuture:**
```java
// Fluent chaining - non-blocking
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(String::toUpperCase);

future.thenAccept(System.out::println);  // Non-blocking callback
```

---

### 3. Combining

**Future:**
```java
// Cannot combine - must get() both
Future<String> future1 = executor.submit(() -> "Hello");
Future<String> future2 = executor.submit(() -> "World");

String result1 = future1.get();  // Blocking
String result2 = future2.get();  // Blocking
String combined = result1 + " " + result2;
```

**CompletableFuture:**
```java
// Easy combining
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

CompletableFuture<String> combined = future1.thenCombine(future2, 
    (s1, s2) -> s1 + " " + s2
);
```

---

### 4. Exception Handling

**Future:**
```java
Future<String> future = executor.submit(() -> {
    throw new RuntimeException("Error");
});

try {
    String result = future.get();
} catch (ExecutionException e) {
    // Manual exception handling
    System.out.println("Error: " + e.getCause().getMessage());
}
```

**CompletableFuture:**
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Error");
})
.exceptionally(ex -> "Recovered")
.thenApply(String::toUpperCase);

System.out.println(future.get());  // RECOVERED
```

---

### 5. Callbacks

**Future:**
```java
Future<String> future = executor.submit(() -> "Result");

// No callbacks - must poll or block
while (!future.isDone()) {
    Thread.sleep(100);
}
String result = future.get();
```

**CompletableFuture:**
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Result");

// Non-blocking callback
future.thenAccept(result -> System.out.println("Got: " + result));
```

---

## Real-World Examples

### Example 1: API Calls

**With Future:**
```java
ExecutorService executor = Executors.newFixedThreadPool(3);

Future<String> userFuture = executor.submit(() -> fetchUser());
Future<String> ordersFuture = executor.submit(() -> fetchOrders());
Future<String> profileFuture = executor.submit(() -> fetchProfile());

// Must block on each
String user = userFuture.get();      // Blocking
String orders = ordersFuture.get();  // Blocking
String profile = profileFuture.get(); // Blocking

String result = user + orders + profile;
executor.shutdown();
```

**With CompletableFuture:**
```java
CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> fetchUser());
CompletableFuture<String> ordersFuture = CompletableFuture.supplyAsync(() -> fetchOrders());
CompletableFuture<String> profileFuture = CompletableFuture.supplyAsync(() -> fetchProfile());

// Combine all
CompletableFuture<String> combined = userFuture
    .thenCombine(ordersFuture, (u, o) -> u + o)
    .thenCombine(profileFuture, (uo, p) -> uo + p);

combined.thenAccept(System.out::println);  // Non-blocking
```

---

### Example 2: Sequential Processing

**With Future:**
```java
Future<String> future1 = executor.submit(() -> step1());
String result1 = future1.get();  // Blocking

Future<String> future2 = executor.submit(() -> step2(result1));
String result2 = future2.get();  // Blocking

Future<String> future3 = executor.submit(() -> step3(result2));
String result3 = future3.get();  // Blocking
```

**With CompletableFuture:**
```java
CompletableFuture<String> result = CompletableFuture.supplyAsync(() -> step1())
    .thenApply(r1 -> step2(r1))
    .thenApply(r2 -> step3(r2));

result.thenAccept(System.out::println);  // Non-blocking
```

---

### Example 3: Error Recovery

**With Future:**
```java
Future<String> future = executor.submit(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
});

String result;
try {
    result = future.get();
} catch (ExecutionException e) {
    result = "Default";  // Manual fallback
}
```

**With CompletableFuture:**
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Error");
    }
    return "Success";
})
.exceptionally(ex -> "Default");  // Automatic fallback

future.thenAccept(System.out::println);
```

---

### Example 4: Timeout Handling

**With Future (Java 8):**
```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(5000);
    return "Result";
});

try {
    String result = future.get(2, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);
    System.out.println("Timeout!");
}
```

**With CompletableFuture (Java 9+):**
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    sleep(5000);
    return "Result";
})
.orTimeout(2, TimeUnit.SECONDS)
.exceptionally(ex -> "Timeout!");

future.thenAccept(System.out::println);
```

---

### Example 5: Multiple API Calls with Fallback

**With Future:**
```java
Future<String> primary = executor.submit(() -> fetchFromPrimary());
Future<String> secondary = executor.submit(() -> fetchFromSecondary());

String result;
try {
    result = primary.get(1, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    try {
        result = secondary.get();
    } catch (Exception ex) {
        result = "Default";
    }
}
```

**With CompletableFuture:**
```java
CompletableFuture<String> result = CompletableFuture.supplyAsync(() -> fetchFromPrimary())
    .orTimeout(1, TimeUnit.SECONDS)
    .exceptionally(ex -> fetchFromSecondary())
    .exceptionally(ex -> "Default");

result.thenAccept(System.out::println);
```

---

### Example 6: Parallel Processing with Aggregation

**With Future:**
```java
List<Future<Integer>> futures = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    int taskId = i;
    futures.add(executor.submit(() -> process(taskId)));
}

int sum = 0;
for (Future<Integer> future : futures) {
    sum += future.get();  // Blocking on each
}
```

**With CompletableFuture:**
```java
List<CompletableFuture<Integer>> futures = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    int taskId = i;
    futures.add(CompletableFuture.supplyAsync(() -> process(taskId)));
}

CompletableFuture<Integer> sum = CompletableFuture.allOf(
    futures.toArray(new CompletableFuture[0])
).thenApply(v -> 
    futures.stream()
           .map(CompletableFuture::join)
           .mapToInt(Integer::intValue)
           .sum()
);

sum.thenAccept(System.out::println);
```

---

## When to Use What

### Use Future When:

✅ Simple async task with blocking get()  
✅ Legacy code (Java 5-7)  
✅ No need for chaining or combining  
✅ Executor framework already in use  

**Example:**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(() -> "Simple task");
String result = future.get();  // Simple blocking get
```

---

### Use CompletableFuture When:

✅ Need non-blocking callbacks  
✅ Chaining multiple async operations  
✅ Combining multiple futures  
✅ Complex error handling  
✅ Functional programming style  
✅ Modern Java applications (8+)  

**Example:**
```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(this::transform)
    .thenApply(this::validate)
    .exceptionally(this::handleError)
    .thenAccept(this::save);
```

---

## Performance Comparison

```java
// Benchmark: 1000 tasks

// Future (blocking)
long start = System.currentTimeMillis();
List<Future<String>> futures = new ArrayList<>();
for (int i = 0; i < 1000; i++) {
    futures.add(executor.submit(() -> task()));
}
for (Future<String> f : futures) {
    f.get();  // Blocks on each
}
long futureTime = System.currentTimeMillis() - start;

// CompletableFuture (non-blocking)
start = System.currentTimeMillis();
List<CompletableFuture<String>> cfutures = new ArrayList<>();
for (int i = 0; i < 1000; i++) {
    cfutures.add(CompletableFuture.supplyAsync(() -> task()));
}
CompletableFuture.allOf(cfutures.toArray(new CompletableFuture[0])).join();
long cfTime = System.currentTimeMillis() - start;

// CompletableFuture is typically faster due to better thread utilization
```

---

## Migration Guide

### From Future to CompletableFuture

```java
// Before: Future
ExecutorService executor = Executors.newFixedThreadPool(10);
Future<String> future = executor.submit(() -> "Result");
String result = future.get();

// After: CompletableFuture
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Result");
future.thenAccept(result -> System.out.println(result));

// Or with custom executor
ExecutorService executor = Executors.newFixedThreadPool(10);
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Result", executor);
```

---

## Summary

### Key Takeaways

1. **Future is simple but limited** - blocking, no chaining, no callbacks
2. **CompletableFuture is powerful** - non-blocking, chainable, composable
3. **CompletableFuture is backward compatible** - implements Future interface
4. **Use CompletableFuture for modern applications** - better API, more features
5. **Future still useful for simple cases** - when you just need blocking get()

### Quick Reference

| Task | Future | CompletableFuture |
|------|--------|-------------------|
| Create | `executor.submit()` | `supplyAsync()` |
| Get result | `get()` | `get()` or `join()` |
| Callback | ❌ | `thenAccept()` |
| Transform | ❌ | `thenApply()` |
| Chain | ❌ | `thenCompose()` |
| Combine | ❌ | `thenCombine()` |
| Error handling | `try-catch` | `exceptionally()` |
| Timeout | `get(timeout)` | `orTimeout()` |

---

## References

- [Future JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html)
- [CompletableFuture JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- [Java Concurrency in Practice](https://jcip.net/)

---

**Last Updated**: 2024  
**Java Versions**: 5 (Future), 8+ (CompletableFuture)
