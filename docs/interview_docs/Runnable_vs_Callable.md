# Runnable vs Callable - Deep Dive

## Overview

Both Runnable and Callable are functional interfaces used for multi-threading in Java, but they have key differences in return values, exception handling, and usage patterns.

---

## Quick Comparison

| Feature | Runnable | Callable |
|---------|----------|----------|
| **Package** | java.lang | java.util.concurrent |
| **Method** | void run() | V call() throws Exception |
| **Return Value** | No (void) | Yes (generic type V) |
| **Exception** | Cannot throw checked exceptions | Can throw checked exceptions |
| **Introduced** | Java 1.0 | Java 1.5 |
| **Use with** | Thread, ExecutorService | ExecutorService only |
| **Result** | N/A | Future<V> |

---

## Runnable Interface

### Definition

```java
@FunctionalInterface
public interface Runnable {
    void run();
}
```

### Basic Usage

```java
// Method 1: Anonymous class
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Task running in: " + Thread.currentThread().getName());
    }
};

// Method 2: Lambda expression
Runnable task = () -> {
    System.out.println("Task running in: " + Thread.currentThread().getName());
};

// Method 3: Method reference
Runnable task = System.out::println;
```

### With Thread

```java
public class RunnableExample {
    public static void main(String[] args) {
        Runnable task = () -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println(Thread.currentThread().getName() + ": " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        
        Thread thread = new Thread(task);
        thread.start();
    }
}
```

**Output**:
```
Thread-0: 1
Thread-0: 2
Thread-0: 3
Thread-0: 4
Thread-0: 5
```

### With ExecutorService

```java
public class RunnableExecutorExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int i = 1; i <= 5; i++) {
            int taskId = i;
            Runnable task = () -> {
                System.out.println("Task " + taskId + " executed by " + 
                    Thread.currentThread().getName());
            };
            executor.submit(task);
        }
        
        executor.shutdown();
    }
}
```

**Output**:
```
Task 1 executed by pool-1-thread-1
Task 2 executed by pool-1-thread-2
Task 3 executed by pool-1-thread-3
Task 4 executed by pool-1-thread-1
Task 5 executed by pool-1-thread-2
```

### Limitations

**1. No Return Value**
```java
Runnable task = () -> {
    int result = 10 + 20;
    // Cannot return result!
};
```

**2. Cannot Throw Checked Exceptions**
```java
Runnable task = () -> {
    // Compile error: Unhandled exception
    // Thread.sleep(1000);
    
    // Must handle inside
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
};
```

---

## Callable Interface

### Definition

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

### Basic Usage

```java
// Method 1: Anonymous class
Callable<Integer> task = new Callable<Integer>() {
    @Override
    public Integer call() throws Exception {
        return 42;
    }
};

// Method 2: Lambda expression
Callable<Integer> task = () -> {
    Thread.sleep(1000);
    return 42;
};

// Method 3: Method reference (if applicable)
Callable<String> task = () -> "Hello World";
```

### With ExecutorService and Future

```java
public class CallableExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Callable<Integer> task = () -> {
            System.out.println("Task running in: " + Thread.currentThread().getName());
            Thread.sleep(2000);
            return 42;
        };
        
        Future<Integer> future = executor.submit(task);
        
        System.out.println("Task submitted, doing other work...");
        
        // Blocking call - waits for result
        Integer result = future.get();
        System.out.println("Result: " + result);
        
        executor.shutdown();
    }
}
```

**Output**:
```
Task submitted, doing other work...
Task running in: pool-1-thread-1
Result: 42
```

### Multiple Callable Tasks

```java
public class MultipleCallableExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        List<Callable<Integer>> tasks = Arrays.asList(
            () -> { Thread.sleep(1000); return 10; },
            () -> { Thread.sleep(2000); return 20; },
            () -> { Thread.sleep(1500); return 30; }
        );
        
        List<Future<Integer>> futures = executor.invokeAll(tasks);
        
        int sum = 0;
        for (Future<Integer> future : futures) {
            sum += future.get();
        }
        
        System.out.println("Sum: " + sum); // 60
        
        executor.shutdown();
    }
}
```

### Exception Handling

```java
public class CallableExceptionExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Callable<Integer> task = () -> {
            if (Math.random() > 0.5) {
                throw new Exception("Random failure!");
            }
            return 42;
        };
        
        Future<Integer> future = executor.submit(task);
        
        try {
            Integer result = future.get();
            System.out.println("Result: " + result);
        } catch (ExecutionException e) {
            System.out.println("Task threw exception: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        executor.shutdown();
    }
}
```

---

## Detailed Comparison

### 1. Return Value

**Runnable - No Return**
```java
Runnable task = () -> {
    int result = compute();
    // result is lost!
};
```

**Callable - Returns Value**
```java
Callable<Integer> task = () -> {
    int result = compute();
    return result; // Can return!
};

Future<Integer> future = executor.submit(task);
Integer result = future.get(); // Retrieve result
```

### 2. Exception Handling

**Runnable - Must Handle Internally**
```java
Runnable task = () -> {
    try {
        riskyOperation();
    } catch (Exception e) {
        // Must handle here
        e.printStackTrace();
    }
};
```

**Callable - Can Throw**
```java
Callable<String> task = () -> {
    return riskyOperation(); // Can throw checked exception
};

try {
    String result = future.get();
} catch (ExecutionException e) {
    // Handle exception thrown by task
    Throwable cause = e.getCause();
}
```

### 3. Usage with Thread

**Runnable - Works**
```java
Runnable task = () -> System.out.println("Hello");
Thread thread = new Thread(task);
thread.start();
```

**Callable - Doesn't Work**
```java
Callable<String> task = () -> "Hello";
// Thread thread = new Thread(task); // Compile error!
// Must use ExecutorService
```

### 4. Future Support

**Runnable - No Future**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(() -> System.out.println("Task"));
// No way to get result or check completion
```

**Callable - Returns Future**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Integer> future = executor.submit(() -> 42);

// Check if done
if (future.isDone()) {
    Integer result = future.get();
}

// Cancel task
future.cancel(true);
```

---

## Real-World Examples

### Example 1: Database Query

**With Runnable (No Result)**
```java
Runnable task = () -> {
    List<User> users = database.query("SELECT * FROM users");
    // Cannot return users!
    // Must use shared variable or callback
};
```

**With Callable (Returns Result)**
```java
Callable<List<User>> task = () -> {
    return database.query("SELECT * FROM users");
};

Future<List<User>> future = executor.submit(task);
List<User> users = future.get(); // Get result
```

### Example 2: File Processing

**With Runnable**
```java
public class FileProcessor {
    private volatile String content; // Shared state
    
    public void processFile(String filePath) {
        Runnable task = () -> {
            try {
                content = Files.readString(Path.of(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        
        Thread thread = new Thread(task);
        thread.start();
        thread.join(); // Wait for completion
        
        System.out.println(content); // Access shared state
    }
}
```

**With Callable**
```java
public class FileProcessor {
    
    public String processFile(String filePath) throws Exception {
        Callable<String> task = () -> {
            return Files.readString(Path.of(filePath));
        };
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(task);
        
        String content = future.get(); // Get result directly
        executor.shutdown();
        
        return content;
    }
}
```

### Example 3: Parallel Computation

**With Callable**
```java
public class ParallelSum {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        // Split into 4 tasks
        List<Callable<Integer>> tasks = new ArrayList<>();
        int chunkSize = numbers.length / 4;
        
        for (int i = 0; i < 4; i++) {
            int start = i * chunkSize;
            int end = (i == 3) ? numbers.length : start + chunkSize;
            
            tasks.add(() -> {
                int sum = 0;
                for (int j = start; j < end; j++) {
                    sum += numbers[j];
                }
                return sum;
            });
        }
        
        List<Future<Integer>> futures = executor.invokeAll(tasks);
        
        int totalSum = 0;
        for (Future<Integer> future : futures) {
            totalSum += future.get();
        }
        
        System.out.println("Total Sum: " + totalSum); // 55
        
        executor.shutdown();
    }
}
```

### Example 4: API Calls with Timeout

**With Callable**
```java
public class APIClient {
    public String fetchData(String url) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        Callable<String> task = () -> {
            // Simulate API call
            Thread.sleep(5000);
            return "API Response";
        };
        
        Future<String> future = executor.submit(task);
        
        try {
            // Wait max 3 seconds
            return future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return "Request timed out";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            executor.shutdown();
        }
    }
}
```

---

## Converting Between Runnable and Callable

### Runnable to Callable

```java
// Using Executors utility
Runnable runnable = () -> System.out.println("Task");
Callable<Void> callable = Executors.callable(runnable);

// Manual conversion
Callable<Void> callable = () -> {
    runnable.run();
    return null;
};
```

### Callable to Runnable (Loses Return Value)

```java
Callable<Integer> callable = () -> 42;

Runnable runnable = () -> {
    try {
        Integer result = callable.call();
        // result is lost
    } catch (Exception e) {
        e.printStackTrace();
    }
};
```

---

## Future Interface Methods

```java
Future<Integer> future = executor.submit(callable);

// Check if task completed
boolean done = future.isDone();

// Check if task was cancelled
boolean cancelled = future.isCancelled();

// Get result (blocking)
Integer result = future.get();

// Get result with timeout
Integer result = future.get(5, TimeUnit.SECONDS);

// Cancel task
boolean cancelled = future.cancel(true); // true = interrupt if running
```

---

## When to Use What?

### Use Runnable When:
- ✅ No return value needed
- ✅ Fire-and-forget tasks
- ✅ Simple background operations
- ✅ Need to use with Thread class
- ✅ Logging, notifications, cleanup tasks

**Examples**:
```java
// Logging
Runnable logger = () -> log.info("Task completed");

// Cleanup
Runnable cleanup = () -> tempFiles.deleteAll();

// Notification
Runnable notify = () -> emailService.send("Task done");
```

### Use Callable When:
- ✅ Need return value
- ✅ Need to throw checked exceptions
- ✅ Need to check task completion status
- ✅ Need to cancel tasks
- ✅ Parallel computations with results

**Examples**:
```java
// Database query
Callable<List<User>> query = () -> db.findAll();

// API call
Callable<String> apiCall = () -> httpClient.get(url);

// Computation
Callable<Integer> compute = () -> heavyComputation();
```

---

## Interview Questions & Answers

### Q1: Can Callable be used with Thread class?

**Answer**: No. Thread constructor only accepts Runnable. Callable must be used with ExecutorService.

```java
// This works
Thread t = new Thread(runnable);

// This doesn't work
Thread t = new Thread(callable); // Compile error
```

### Q2: How to get result from Runnable?

**Answer**: Use shared variable or callback, but Callable is better:

```java
// Approach 1: Shared variable (not recommended)
class ResultHolder {
    volatile Integer result;
}

ResultHolder holder = new ResultHolder();
Runnable task = () -> holder.result = 42;

// Approach 2: Use Callable instead (recommended)
Callable<Integer> task = () -> 42;
Future<Integer> future = executor.submit(task);
Integer result = future.get();
```

### Q3: What happens if Callable throws exception?

**Answer**: Exception is wrapped in ExecutionException and thrown when calling future.get():

```java
Callable<Integer> task = () -> {
    throw new IOException("File not found");
};

Future<Integer> future = executor.submit(task);

try {
    future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause(); // IOException
}
```

### Q4: Can we convert Runnable to Callable?

**Answer**: Yes, using Executors.callable():

```java
Runnable runnable = () -> System.out.println("Task");
Callable<Void> callable = Executors.callable(runnable);
```

### Q5: Which is better for performance?

**Answer**: No performance difference. Choice depends on requirements:
- Need return value? → Callable
- Fire-and-forget? → Runnable

---

## Best Practices

1. **Prefer Callable for tasks that produce results**
```java
// Good
Callable<String> task = () -> fetchData();
Future<String> future = executor.submit(task);
String data = future.get();

// Avoid
String[] result = new String[1];
Runnable task = () -> result[0] = fetchData();
```

2. **Use timeout with Future.get()**
```java
try {
    result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);
}
```

3. **Handle ExecutionException properly**
```java
try {
    result = future.get();
} catch (ExecutionException e) {
    Throwable cause = e.getCause();
    if (cause instanceof IOException) {
        // Handle IO exception
    }
}
```

4. **Always shutdown ExecutorService**
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
try {
    // Submit tasks
} finally {
    executor.shutdown();
}
```

---

## Key Takeaways

1. **Runnable**: void run() - no return, no checked exceptions
2. **Callable**: V call() throws Exception - returns value, can throw
3. **Callable requires ExecutorService**, Runnable works with Thread
4. **Use Future to get Callable results** and check status
5. **Prefer Callable when you need results** or exception propagation
6. **Use Runnable for fire-and-forget** background tasks
7. **ExecutorService.submit()** works with both Runnable and Callable

---

## Practice Problems

1. Convert a Runnable task to Callable that returns execution time
2. Implement parallel file processing using Callable
3. Create a task that can be cancelled mid-execution
4. Build a system that executes multiple API calls and aggregates results
5. Implement timeout mechanism for long-running Callable tasks
6. Design a task scheduler using Callable with retry logic
7. Create a parallel computation framework using Callable
8. Implement exception handling strategy for multiple Callable tasks
9. Build a result caching mechanism for Callable tasks
10. Design a priority-based task execution system
