# Asynchronous API Calls with Concurrent Programming in Java 21

## Problem Statement
Call multiple asynchronous APIs concurrently, collect their responses, merge the data, and return the final result. Demonstrate different approaches using Java 21 concurrent programming features.

## Scenario
Fetch user data from multiple microservices:
- User Service: Basic user info
- Order Service: User's orders
- Payment Service: Payment history
- Notification Service: User preferences

Merge all data into a single response.

---

## Solution 1: CompletableFuture (Most Common)

```java
import java.util.*;
import java.util.concurrent.*;

public class AsyncAPICompletableFuture {
    
    // Simulate async API calls
    static CompletableFuture<UserInfo> fetchUserInfo(String userId) {
        // CompletableFuture.supplyAsync() - Runs task asynchronously in ForkJoinPool.commonPool()
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000); // Simulate 1 second API delay (network latency)
            // Return user info after delay
            return new UserInfo(userId, "John Doe", "john@example.com");
        });
    }
    
    static CompletableFuture<List<Order>> fetchOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return Arrays.asList(
                new Order("ORD1", 100.0),
                new Order("ORD2", 200.0)
            );
        });
    }
    
    static CompletableFuture<PaymentHistory> fetchPaymentHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return new PaymentHistory(userId, 5, 1500.0);
        });
    }
    
    static CompletableFuture<NotificationPrefs> fetchNotificationPrefs(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return new NotificationPrefs(true, false, true);
        });
    }
    
    // Merge all data
    public static CompletableFuture<UserProfile> getUserProfile(String userId) {
        // Step 1: Start all API calls concurrently (non-blocking)
        CompletableFuture<UserInfo> userInfoFuture = fetchUserInfo(userId);
        CompletableFuture<List<Order>> ordersFuture = fetchOrders(userId);
        CompletableFuture<PaymentHistory> paymentFuture = fetchPaymentHistory(userId);
        CompletableFuture<NotificationPrefs> notifFuture = fetchNotificationPrefs(userId);
        
        // Step 2: Wait for ALL futures to complete using allOf()
        // allOf() returns CompletableFuture<Void> that completes when all complete
        return CompletableFuture.allOf(userInfoFuture, ordersFuture, paymentFuture, notifFuture)
            // Step 3: Once all complete, merge results using thenApply()
            .thenApply(v -> {
                // join() blocks and gets the result (safe here as all are complete)
                UserInfo userInfo = userInfoFuture.join();
                List<Order> orders = ordersFuture.join();
                PaymentHistory payment = paymentFuture.join();
                NotificationPrefs notif = notifFuture.join();
                
                // Step 4: Create and return merged UserProfile
                return new UserProfile(userInfo, orders, payment, notif);
            });
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        
        // get() blocks until CompletableFuture completes and returns result
        // Throws ExecutionException if any task failed
        UserProfile profile = getUserProfile("user123").get();
        
        long end = System.currentTimeMillis();
        System.out.println("Profile: " + profile);
        // Time = max(1000, 1500, 800, 500) = 1500ms (parallel execution)
        // Sequential would be: 1000 + 1500 + 800 + 500 = 3800ms
        System.out.println("Time taken: " + (end - start) + "ms"); // ~1500ms (parallel)
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}

// Data classes
record UserInfo(String userId, String name, String email) {}
record Order(String orderId, double amount) {}
record PaymentHistory(String userId, int totalTransactions, double totalAmount) {}
record NotificationPrefs(boolean email, boolean sms, boolean push) {}
record UserProfile(UserInfo userInfo, List<Order> orders, 
                   PaymentHistory payment, NotificationPrefs notifications) {}
```

**Time**: ~1500ms (longest API call) instead of 3800ms (sequential)  
**Advantages**: Non-blocking, composable, exception handling

---

## Solution 2: CompletableFuture with Custom Executor

```java
import java.util.concurrent.*;

public class AsyncAPICustomExecutor {
    
    // Custom thread pool with fixed 10 threads
    // Better than default ForkJoinPool for controlling resource usage
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    static CompletableFuture<UserInfo> fetchUserInfo(String userId) {
        // Pass custom executor as second parameter to supplyAsync()
        // Task will run in our thread pool instead of ForkJoinPool.commonPool()
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return new UserInfo(userId, "John Doe", "john@example.com");
        }, executor); // ← Custom executor
    }
    
    static CompletableFuture<List<Order>> fetchOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
        }, executor);
    }
    
    static CompletableFuture<PaymentHistory> fetchPaymentHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return new PaymentHistory(userId, 5, 1500.0);
        }, executor);
    }
    
    static CompletableFuture<NotificationPrefs> fetchNotificationPrefs(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return new NotificationPrefs(true, false, true);
        }, executor);
    }
    
    public static CompletableFuture<UserProfile> getUserProfile(String userId) {
        return CompletableFuture.allOf(
            fetchUserInfo(userId),
            fetchOrders(userId),
            fetchPaymentHistory(userId),
            fetchNotificationPrefs(userId)
        ).thenApply(v -> new UserProfile(
            fetchUserInfo(userId).join(),
            fetchOrders(userId).join(),
            fetchPaymentHistory(userId).join(),
            fetchNotificationPrefs(userId).join()
        ));
    }
    
    public static void main(String[] args) throws Exception {
        UserProfile profile = getUserProfile("user123").get();
        System.out.println(profile);
        // IMPORTANT: Always shutdown custom executor to release resources
        executor.shutdown();
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Control over thread pool size, better resource management

---

## Solution 3: Java 21 Virtual Threads (Project Loom)

```java
import java.util.concurrent.*;

public class AsyncAPIVirtualThreads {
    
    // Note: Methods are synchronous (no CompletableFuture)
    // Virtual threads make blocking code efficient
    static UserInfo fetchUserInfo(String userId) {
        sleep(1000); // Blocking call is OK with virtual threads
        return new UserInfo(userId, "John Doe", "john@example.com");
    }
    
    static List<Order> fetchOrders(String userId) {
        sleep(1500);
        return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
    }
    
    static PaymentHistory fetchPaymentHistory(String userId) {
        sleep(800);
        return new PaymentHistory(userId, 5, 1500.0);
    }
    
    static NotificationPrefs fetchNotificationPrefs(String userId) {
        sleep(500);
        return new NotificationPrefs(true, false, true);
    }
    
    public static UserProfile getUserProfile(String userId) throws Exception {
        // try-with-resources automatically closes executor
        // newVirtualThreadPerTaskExecutor() creates lightweight virtual threads
        // Can create millions of virtual threads (vs thousands of platform threads)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit tasks - each runs in separate virtual thread
            Future<UserInfo> userInfoFuture = executor.submit(() -> fetchUserInfo(userId));
            Future<List<Order>> ordersFuture = executor.submit(() -> fetchOrders(userId));
            Future<PaymentHistory> paymentFuture = executor.submit(() -> fetchPaymentHistory(userId));
            Future<NotificationPrefs> notifFuture = executor.submit(() -> fetchNotificationPrefs(userId));
            
            // get() blocks until result is ready
            // All tasks run concurrently in virtual threads
            return new UserProfile(
                userInfoFuture.get(),  // Waits for user info
                ordersFuture.get(),    // Waits for orders
                paymentFuture.get(),   // Waits for payment
                notifFuture.get()      // Waits for notifications
            );
        } // Executor auto-closes here
    }
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        UserProfile profile = getUserProfile("user123");
        long end = System.currentTimeMillis();
        
        System.out.println(profile);
        System.out.println("Time: " + (end - start) + "ms");
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Lightweight threads, millions of concurrent tasks, simpler code

---

## Solution 4: StructuredTaskScope (Java 21 Preview)

```java
import java.util.concurrent.*;

public class AsyncAPIStructuredConcurrency {
    
    public static UserProfile getUserProfile(String userId) throws Exception {
        // StructuredTaskScope provides structured concurrency
        // ShutdownOnFailure: If any task fails, cancel all others immediately
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // fork() starts task in new virtual thread, returns Subtask handle
            Subtask<UserInfo> userInfoTask = scope.fork(() -> fetchUserInfo(userId));
            Subtask<List<Order>> ordersTask = scope.fork(() -> fetchOrders(userId));
            Subtask<PaymentHistory> paymentTask = scope.fork(() -> fetchPaymentHistory(userId));
            Subtask<NotificationPrefs> notifTask = scope.fork(() -> fetchNotificationPrefs(userId));
            
            // join() waits for all forked tasks to complete or fail
            scope.join();
            
            // throwIfFailed() throws if any task failed (fail-fast behavior)
            scope.throwIfFailed();
            
            // get() retrieves results (safe after join() + throwIfFailed())
            return new UserProfile(
                userInfoTask.get(),
                ordersTask.get(),
                paymentTask.get(),
                notifTask.get()
            );
        } // Scope auto-closes, cancels any remaining tasks
    }
    
    static UserInfo fetchUserInfo(String userId) {
        sleep(1000);
        return new UserInfo(userId, "John Doe", "john@example.com");
    }
    
    static List<Order> fetchOrders(String userId) {
        sleep(1500);
        return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
    }
    
    static PaymentHistory fetchPaymentHistory(String userId) {
        sleep(800);
        return new PaymentHistory(userId, 5, 1500.0);
    }
    
    static NotificationPrefs fetchNotificationPrefs(String userId) {
        sleep(500);
        return new NotificationPrefs(true, false, true);
    }
    
    public static void main(String[] args) throws Exception {
        UserProfile profile = getUserProfile("user123");
        System.out.println(profile);
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Structured concurrency, automatic cleanup, fail-fast behavior

---

## Solution 5: Parallel Streams

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class AsyncAPIParallelStreams {
    
    public static UserProfile getUserProfile(String userId) {
        // Create list of Callable tasks (functions that return values)
        List<Callable<Object>> tasks = Arrays.asList(
            () -> fetchUserInfo(userId),
            () -> fetchOrders(userId),
            () -> fetchPaymentHistory(userId),
            () -> fetchNotificationPrefs(userId)
        );
        
        // parallelStream() processes tasks concurrently using ForkJoinPool
        List<Object> results = tasks.parallelStream()
            .map(task -> {
                try {
                    // call() executes the Callable and returns result
                    return task.call();
                } catch (Exception e) {
                    // Wrap checked exception in RuntimeException
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList()); // Collect all results
        
        // Cast results back to specific types (order preserved)
        return new UserProfile(
            (UserInfo) results.get(0),
            (List<Order>) results.get(1),
            (PaymentHistory) results.get(2),
            (NotificationPrefs) results.get(3)
        );
    }
    
    static UserInfo fetchUserInfo(String userId) {
        sleep(1000);
        return new UserInfo(userId, "John Doe", "john@example.com");
    }
    
    static List<Order> fetchOrders(String userId) {
        sleep(1500);
        return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
    }
    
    static PaymentHistory fetchPaymentHistory(String userId) {
        sleep(800);
        return new PaymentHistory(userId, 5, 1500.0);
    }
    
    static NotificationPrefs fetchNotificationPrefs(String userId) {
        sleep(500);
        return new NotificationPrefs(true, false, true);
    }
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        UserProfile profile = getUserProfile("user123");
        long end = System.currentTimeMillis();
        
        System.out.println(profile);
        System.out.println("Time: " + (end - start) + "ms");
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Simple syntax, good for small number of tasks

---

## Solution 6: ExecutorService with invokeAll

```java
import java.util.*;
import java.util.concurrent.*;

public class AsyncAPIExecutorService {
    
    public static UserProfile getUserProfile(String userId) throws Exception {
        // Create thread pool with 4 threads (one per task)
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            // Create list of tasks
            List<Callable<Object>> tasks = Arrays.asList(
                () -> fetchUserInfo(userId),
                () -> fetchOrders(userId),
                () -> fetchPaymentHistory(userId),
                () -> fetchNotificationPrefs(userId)
            );
            
            // invokeAll() submits all tasks and waits for ALL to complete
            // Returns List<Future> in same order as input tasks
            List<Future<Object>> futures = executor.invokeAll(tasks);
            
            // get() retrieves result from each Future (already complete)
            return new UserProfile(
                (UserInfo) futures.get(0).get(),
                (List<Order>) futures.get(1).get(),
                (PaymentHistory) futures.get(2).get(),
                (NotificationPrefs) futures.get(3).get()
            );
        } finally {
            // Always shutdown executor in finally block
            executor.shutdown();
        }
    }
    
    static UserInfo fetchUserInfo(String userId) {
        sleep(1000);
        return new UserInfo(userId, "John Doe", "john@example.com");
    }
    
    static List<Order> fetchOrders(String userId) {
        sleep(1500);
        return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
    }
    
    static PaymentHistory fetchPaymentHistory(String userId) {
        sleep(800);
        return new PaymentHistory(userId, 5, 1500.0);
    }
    
    static NotificationPrefs fetchNotificationPrefs(String userId) {
        sleep(500);
        return new NotificationPrefs(true, false, true);
    }
    
    public static void main(String[] args) throws Exception {
        UserProfile profile = getUserProfile("user123");
        System.out.println(profile);
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Traditional approach, explicit control

---

## Solution 7: CompletableFuture with Exception Handling

```java
import java.util.concurrent.*;

public class AsyncAPIWithExceptionHandling {
    
    public static CompletableFuture<UserProfile> getUserProfile(String userId) {
        // exceptionally() provides fallback value if API call fails
        // Enables graceful degradation - app continues even if one API fails
        CompletableFuture<UserInfo> userInfoFuture = fetchUserInfo(userId)
            .exceptionally(ex -> {
                // Log error
                System.err.println("User info failed: " + ex.getMessage());
                // Return default/fallback value instead of failing entire request
                return new UserInfo(userId, "Unknown", "unknown@example.com");
            });
        
        CompletableFuture<List<Order>> ordersFuture = fetchOrders(userId)
            .exceptionally(ex -> {
                System.err.println("Orders failed: " + ex.getMessage());
                // Return empty list if orders API fails
                return Collections.emptyList();
            });
        
        CompletableFuture<PaymentHistory> paymentFuture = fetchPaymentHistory(userId)
            .exceptionally(ex -> {
                System.err.println("Payment failed: " + ex.getMessage());
                return new PaymentHistory(userId, 0, 0.0);
            });
        
        CompletableFuture<NotificationPrefs> notifFuture = fetchNotificationPrefs(userId)
            .exceptionally(ex -> {
                System.err.println("Notifications failed: " + ex.getMessage());
                return new NotificationPrefs(false, false, false);
            });
        
        return CompletableFuture.allOf(userInfoFuture, ordersFuture, paymentFuture, notifFuture)
            .thenApply(v -> new UserProfile(
                userInfoFuture.join(),
                ordersFuture.join(),
                paymentFuture.join(),
                notifFuture.join()
            ));
    }
    
    static CompletableFuture<UserInfo> fetchUserInfo(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            // Simulate random API failure (20% chance)
            if (Math.random() > 0.8) throw new RuntimeException("API Error");
            return new UserInfo(userId, "John Doe", "john@example.com");
        });
    }
    
    static CompletableFuture<List<Order>> fetchOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return Arrays.asList(new Order("ORD1", 100.0));
        });
    }
    
    static CompletableFuture<PaymentHistory> fetchPaymentHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return new PaymentHistory(userId, 5, 1500.0);
        });
    }
    
    static CompletableFuture<NotificationPrefs> fetchNotificationPrefs(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return new NotificationPrefs(true, false, true);
        });
    }
    
    public static void main(String[] args) throws Exception {
        UserProfile profile = getUserProfile("user123").get();
        System.out.println(profile);
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Graceful degradation, fallback values

---

## Solution 8: Reactive Programming with CompletableFuture

```java
import java.util.concurrent.*;
import java.util.stream.*;

public class AsyncAPIReactive {
    
    public static CompletableFuture<UserProfile> getUserProfile(String userId) {
        // Reactive/functional composition style
        // thenCompose() chains dependent async operations
        return fetchUserInfo(userId)
            .thenCompose(userInfo ->  // Wait for userInfo, then...
                // Start other 3 API calls in parallel
                CompletableFuture.allOf(
                    fetchOrders(userId),
                    fetchPaymentHistory(userId),
                    fetchNotificationPrefs(userId)
                ).thenApply(v -> new UserProfile(
                    userInfo,  // Use userInfo from previous step
                    fetchOrders(userId).join(),
                    fetchPaymentHistory(userId).join(),
                    fetchNotificationPrefs(userId).join()
                ))
            );
    }
    
    // Alternative: Chain with thenCombine
    public static CompletableFuture<UserProfile> getUserProfileChained(String userId) {
        // thenCombine() combines results of two independent futures
        return fetchUserInfo(userId)
            // Combine userInfo + orders
            .thenCombine(fetchOrders(userId), (userInfo, orders) -> 
                new Object[]{userInfo, orders})
            // Combine previous result + payment
            .thenCombine(fetchPaymentHistory(userId), (arr, payment) -> 
                new Object[]{arr[0], arr[1], payment})
            // Combine previous result + notifications
            .thenCombine(fetchNotificationPrefs(userId), (arr, notif) -> 
                new UserProfile(
                    (UserInfo) arr[0],
                    (List<Order>) arr[1],
                    (PaymentHistory) arr[2],
                    notif
                ));
    }
    
    static CompletableFuture<UserInfo> fetchUserInfo(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return new UserInfo(userId, "John Doe", "john@example.com");
        });
    }
    
    static CompletableFuture<List<Order>> fetchOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return Arrays.asList(new Order("ORD1", 100.0));
        });
    }
    
    static CompletableFuture<PaymentHistory> fetchPaymentHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return new PaymentHistory(userId, 5, 1500.0);
        });
    }
    
    static CompletableFuture<NotificationPrefs> fetchNotificationPrefs(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return new NotificationPrefs(true, false, true);
        });
    }
    
    public static void main(String[] args) throws Exception {
        UserProfile profile = getUserProfile("user123").get();
        System.out.println(profile);
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

**Advantages**: Functional composition, reactive style

---

## Performance Comparison

| Approach | Time (ms) | Complexity | Java Version | Best For |
|----------|-----------|------------|--------------|----------|
| Sequential | 3800 | Simple | Any | Small tasks |
| CompletableFuture | 1500 | Medium | 8+ | **Production** ⭐ |
| Virtual Threads | 1500 | Simple | 21+ | High concurrency |
| StructuredTaskScope | 1500 | Simple | 21+ | Structured code |
| Parallel Streams | 1500 | Simple | 8+ | Quick prototypes |
| ExecutorService | 1500 | Medium | 5+ | Traditional apps |

---

## Complete Working Example with All Approaches

```java
import java.util.*;
import java.util.concurrent.*;

public class AsyncAPIComplete {
    
    // Approach 1: CompletableFuture (Recommended)
    public static CompletableFuture<UserProfile> getProfileCompletable(String userId) {
        // Start all 4 API calls concurrently (non-blocking)
        CompletableFuture<UserInfo> userInfo = CompletableFuture.supplyAsync(() -> fetchUserInfo(userId));
        CompletableFuture<List<Order>> orders = CompletableFuture.supplyAsync(() -> fetchOrders(userId));
        CompletableFuture<PaymentHistory> payment = CompletableFuture.supplyAsync(() -> fetchPayment(userId));
        CompletableFuture<NotificationPrefs> notif = CompletableFuture.supplyAsync(() -> fetchNotif(userId));
        
        // Wait for all to complete, then merge results
        return CompletableFuture.allOf(userInfo, orders, payment, notif)
            .thenApply(v -> new UserProfile(
                userInfo.join(), orders.join(), payment.join(), notif.join()
            ));
    }
    
    // Approach 2: Virtual Threads (Java 21)
    public static UserProfile getProfileVirtualThreads(String userId) throws Exception {
        // Virtual threads: lightweight, can create millions
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit 4 tasks, each runs in separate virtual thread
            Future<UserInfo> userInfo = executor.submit(() -> fetchUserInfo(userId));
            Future<List<Order>> orders = executor.submit(() -> fetchOrders(userId));
            Future<PaymentHistory> payment = executor.submit(() -> fetchPayment(userId));
            Future<NotificationPrefs> notif = executor.submit(() -> fetchNotif(userId));
            
            // get() blocks until result ready, all run concurrently
            return new UserProfile(userInfo.get(), orders.get(), payment.get(), notif.get());
        } // Auto-close executor
    }
    
    // Approach 3: StructuredTaskScope (Java 21 Preview)
    public static UserProfile getProfileStructured(String userId) throws Exception {
        // Structured concurrency: parent-child task relationship
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // fork() starts each task in virtual thread
            var userInfo = scope.fork(() -> fetchUserInfo(userId));
            var orders = scope.fork(() -> fetchOrders(userId));
            var payment = scope.fork(() -> fetchPayment(userId));
            var notif = scope.fork(() -> fetchNotif(userId));
            
            // join() waits for all, throwIfFailed() ensures all succeeded
            scope.join().throwIfFailed();
            
            // Safe to get() results after join()
            return new UserProfile(userInfo.get(), orders.get(), payment.get(), notif.get());
        } // Auto-cancel any remaining tasks
    }
    
    // Simulated API calls
    static UserInfo fetchUserInfo(String userId) {
        sleep(1000);
        return new UserInfo(userId, "John Doe", "john@example.com");
    }
    
    static List<Order> fetchOrders(String userId) {
        sleep(1500);
        return Arrays.asList(new Order("ORD1", 100.0), new Order("ORD2", 200.0));
    }
    
    static PaymentHistory fetchPayment(String userId) {
        sleep(800);
        return new PaymentHistory(userId, 5, 1500.0);
    }
    
    static NotificationPrefs fetchNotif(String userId) {
        sleep(500);
        return new NotificationPrefs(true, false, true);
    }
    
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
    
    public static void main(String[] args) throws Exception {
        String userId = "user123";
        
        System.out.println("=== CompletableFuture ===");
        long start1 = System.currentTimeMillis();
        // get() blocks until all async operations complete
        UserProfile profile1 = getProfileCompletable(userId).get();
        // Time = max(1000, 1500, 800, 500) = 1500ms (parallel)
        System.out.println("Time: " + (System.currentTimeMillis() - start1) + "ms");
        System.out.println(profile1);
        
        System.out.println("\n=== Virtual Threads ===");
        long start2 = System.currentTimeMillis();
        UserProfile profile2 = getProfileVirtualThreads(userId);
        System.out.println("Time: " + (System.currentTimeMillis() - start2) + "ms");
        System.out.println(profile2);
        
        System.out.println("\n=== Structured Concurrency ===");
        long start3 = System.currentTimeMillis();
        UserProfile profile3 = getProfileStructured(userId);
        System.out.println("Time: " + (System.currentTimeMillis() - start3) + "ms");
        System.out.println(profile3);
    }
}

// Data classes
record UserInfo(String userId, String name, String email) {}
record Order(String orderId, double amount) {}
record PaymentHistory(String userId, int totalTransactions, double totalAmount) {}
record NotificationPrefs(boolean email, boolean sms, boolean push) {}
record UserProfile(UserInfo userInfo, List<Order> orders, 
                   PaymentHistory payment, NotificationPrefs notifications) {}
```

**Output**:
```
=== CompletableFuture ===
Time: 1502ms
UserProfile[userInfo=UserInfo[userId=user123, name=John Doe, email=john@example.com], ...]

=== Virtual Threads ===
Time: 1501ms
UserProfile[userInfo=UserInfo[userId=user123, name=John Doe, email=john@example.com], ...]

=== Structured Concurrency ===
Time: 1500ms
UserProfile[userInfo=UserInfo[userId=user123, name=John Doe, email=john@example.com], ...]
```

---

## Key Takeaways

1. **CompletableFuture**: Most versatile, production-ready
2. **Virtual Threads**: Simplest code, best for I/O-bound tasks
3. **StructuredTaskScope**: Clean error handling, automatic cleanup
4. **Parallel Streams**: Quick for simple cases
5. **Custom Executor**: Fine-grained control over thread pool

---

## When to Use Each Approach

### Use CompletableFuture when:
- Need complex composition (thenCompose, thenCombine)
- Require fine-grained exception handling
- Working with Java 8+

### Use Virtual Threads when:
- High number of concurrent tasks (millions)
- I/O-bound operations
- Simple blocking code style preferred

### Use StructuredTaskScope when:
- Need structured concurrency guarantees
- Want automatic cleanup
- Fail-fast behavior required

### Use Parallel Streams when:
- Small number of independent tasks
- Quick prototyping
- Simple transformations

---

## Best Practices

1. **Use timeouts**: Prevent hanging on slow APIs
2. **Handle exceptions**: Provide fallback values
3. **Limit concurrency**: Use bounded thread pools
4. **Monitor performance**: Track API response times
5. **Cache results**: Reduce redundant API calls
6. **Use circuit breakers**: Prevent cascading failures
7. **Log appropriately**: Track concurrent operations
