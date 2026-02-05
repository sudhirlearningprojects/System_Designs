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

#### Theory & Internal Mechanics

**Implementation**:
```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(
        nThreads,                      // corePoolSize = nThreads
        nThreads,                      // maximumPoolSize = nThreads
        0L,                            // keepAliveTime = 0 (threads never timeout)
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>()  // Unbounded queue
    );
}
```

**Key Characteristics**:
1. **Fixed Thread Count**: Creates exactly `n` threads at startup and maintains them throughout lifecycle
2. **Thread Reuse**: Threads are never destroyed (keepAliveTime = 0), always available for reuse
3. **Unbounded Queue**: Uses `LinkedBlockingQueue` with no capacity limit
4. **Blocking Behavior**: When all threads are busy, new tasks wait in queue indefinitely
5. **No Thread Creation Overhead**: After initial creation, no new threads are spawned

**Execution Flow**:
```
Task Submission → Check Active Threads
                     ↓
              [Threads < n?]
                     ↓
            Yes ─→ Create New Thread
                     ↓
            No ─→ Add to Queue
                     ↓
              Wait for Available Thread
                     ↓
              Thread Picks Task from Queue
                     ↓
              Execute Task
                     ↓
              Thread Returns to Pool (never dies)
```

**Memory Implications**:
- **Thread Stack**: Each thread consumes ~1MB stack memory
- **Queue Growth**: Unbounded queue can grow indefinitely → OutOfMemoryError risk
- **Example**: 10 threads = 10MB + queue memory

**Performance Characteristics**:
- **Throughput**: Constant after warmup (no thread creation overhead)
- **Latency**: Predictable for tasks within thread capacity
- **Queue Latency**: Tasks wait in queue when threads are saturated

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
Task 6 by pool-1-thread-1 (reused after 2s)
Task 7 by pool-1-thread-2 (reused after 2s)
...
```

**Use Cases**:
- Web servers with predictable concurrent request load
- Database connection pools
- Fixed resource constraints (e.g., 10 concurrent API calls)

**Pitfalls**:
- **Queue Overflow**: Unbounded queue can consume all heap memory
- **Thread Starvation**: Long-running tasks block other tasks
- **No Elasticity**: Cannot scale beyond fixed thread count

---

### 3.2 CachedThreadPool

**Creates threads on demand, reuses idle threads**

#### Theory & Internal Mechanics

**Implementation**:
```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(
        0,                             // corePoolSize = 0 (no core threads)
        Integer.MAX_VALUE,             // maximumPoolSize = ~2 billion
        60L,                           // keepAliveTime = 60 seconds
        TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>()  // Zero-capacity queue
    );
}
```

**Key Characteristics**:
1. **Zero Core Threads**: No threads exist initially, all created on-demand
2. **Unbounded Thread Creation**: Can create up to 2,147,483,647 threads (practically unlimited)
3. **SynchronousQueue**: Direct handoff, no buffering (queue size = 0)
4. **60-Second Timeout**: Idle threads terminate after 60 seconds of inactivity
5. **Elastic Scaling**: Grows and shrinks based on workload

**Execution Flow**:
```
Task Submission → Try Direct Handoff to Idle Thread
                     ↓
              [Idle Thread Available?]
                     ↓
            Yes ─→ Handoff Task Immediately
                     ↓
            No ─→ Create New Thread
                     ↓
              Execute Task
                     ↓
              [Idle for 60s?]
                     ↓
            Yes ─→ Terminate Thread
                     ↓
            No ─→ Wait for Next Task
```

**SynchronousQueue Behavior**:
- **No Storage**: Cannot hold tasks, must immediately transfer to thread
- **Blocking**: Producer blocks until consumer (thread) takes the task
- **Direct Handoff**: Zero-copy transfer from submitter to worker thread

**Memory & Performance**:
- **Thread Explosion Risk**: 10,000 concurrent tasks = 10,000 threads = 10GB+ memory
- **Context Switching**: Too many threads → CPU thrashing
- **Optimal for**: Burst workloads with short task duration

**Thread Lifecycle**:
```
Time 0s:   Submit 100 tasks → Create 100 threads
Time 1s:   Tasks complete → 100 idle threads
Time 61s:  All threads terminated (60s timeout)
Time 62s:  Submit 1 task → Create 1 new thread
```

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

**Output** (100 threads created):
```
Task 1 by pool-1-thread-1
Task 2 by pool-1-thread-2
...
Task 100 by pool-1-thread-100
```

**Use Cases**:
- Async I/O operations (network calls, file operations)
- Short-lived tasks with unpredictable arrival rate
- Microservices with bursty traffic

**Pitfalls**:
- **OutOfMemoryError**: Too many threads exhaust heap/stack memory
- **CPU Thrashing**: Context switching overhead with 1000+ threads
- **No Backpressure**: No queue means no buffering during spikes

---

### 3.3 SingleThreadExecutor

**Single worker thread, sequential execution**

#### Theory & Internal Mechanics

**Implementation**:
```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService(
        new ThreadPoolExecutor(
            1,                         // corePoolSize = 1
            1,                         // maximumPoolSize = 1
            0L,                        // keepAliveTime = 0
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>()  // Unbounded queue
        )
    );
}
```

**Key Characteristics**:
1. **Single Thread Guarantee**: Only one thread ever exists, never replaced
2. **Sequential Execution**: Tasks execute in submission order (FIFO)
3. **Unbounded Queue**: Tasks wait in queue if thread is busy
4. **Thread Resurrection**: If thread dies (exception), new thread is created
5. **Non-Reconfigurable**: Wrapped in `DelegatedExecutorService` to prevent reconfiguration

**Execution Flow**:
```
Task 1 Submitted → Queue: [Task1]
                     ↓
              Thread Executes Task1
                     ↓
Task 2 Submitted → Queue: [Task2]
                     ↓
Task 3 Submitted → Queue: [Task2, Task3]
                     ↓
              Task1 Completes
                     ↓
              Thread Executes Task2
                     ↓
              Task2 Completes
                     ↓
              Thread Executes Task3
```

**Ordering Guarantees**:
- **Happens-Before**: Task N+1 sees all effects of Task N
- **Memory Visibility**: No need for synchronization between tasks
- **Atomic Operations**: Each task is atomic unit of execution

**Thread Safety**:
```java
// No synchronization needed - single thread guarantees visibility
class Counter {
    private int count = 0;  // No volatile needed
    
    void increment() {
        count++;  // Thread-safe in SingleThreadExecutor
    }
}
```

**Fault Tolerance**:
```java
executor.submit(() -> {
    System.out.println("Task 1");
    throw new RuntimeException("Task 1 failed");
});

executor.submit(() -> {
    System.out.println("Task 2");  // Still executes!
});

// Output:
// Task 1
// Task 2  (new thread created after Task 1 exception)
```

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

**Use Cases**:
- Event loop processing (UI events, message queue)
- Audit logging (sequential writes)
- Database migrations (ordered schema changes)
- State machines (sequential state transitions)

**Pitfalls**:
- **No Parallelism**: Cannot utilize multiple CPU cores
- **Head-of-Line Blocking**: Slow task blocks all subsequent tasks
- **Queue Overflow**: Unbounded queue can exhaust memory

---

### 3.4 ScheduledThreadPoolExecutor

**Schedule tasks with delay or periodic execution**

#### Theory & Internal Mechanics

**Implementation**:
```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}

// ScheduledThreadPoolExecutor extends ThreadPoolExecutor
public ScheduledThreadPoolExecutor(int corePoolSize) {
    super(
        corePoolSize,                  // corePoolSize
        Integer.MAX_VALUE,             // maximumPoolSize
        0L,                            // keepAliveTime
        TimeUnit.NANOSECONDS,
        new DelayedWorkQueue()         // Priority queue sorted by delay
    );
}
```

**Key Characteristics**:
1. **DelayedWorkQueue**: Priority queue ordered by execution time (earliest first)
2. **Nanosecond Precision**: Uses `System.nanoTime()` for accurate scheduling
3. **Fixed Core Threads**: Core threads never timeout
4. **Unbounded Max Threads**: Can create additional threads if needed
5. **Task Wrapping**: Tasks wrapped in `ScheduledFutureTask` with delay metadata

**DelayedWorkQueue Internals**:
```
Heap Structure (Min-Heap by execution time):

         [Task A: 10:00:05]
              /           \
    [Task B: 10:00:10]  [Task C: 10:00:15]
         /
[Task D: 10:00:20]

Poll() → Returns Task A (earliest)
```

**Scheduling Modes**:

**1. One-Time Delay** (`schedule`):
```java
scheduler.schedule(() -> {
    System.out.println("Executed after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Timeline:
// T=0s:  Task scheduled
// T=5s:  Task executes once
// T=6s:  Task complete, never runs again
```

**2. Fixed Rate** (`scheduleAtFixedRate`):
```java
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Periodic: " + System.currentTimeMillis());
    Thread.sleep(3000);  // Task takes 3 seconds
}, 0, 2, TimeUnit.SECONDS);

// Timeline:
// T=0s:  Execution 1 starts
// T=2s:  Execution 2 scheduled (but waits for Execution 1)
// T=3s:  Execution 1 completes, Execution 2 starts immediately
// T=4s:  Execution 3 scheduled
// T=6s:  Execution 2 completes, Execution 3 starts immediately

// Note: If task takes longer than period, executions run back-to-back
```

**3. Fixed Delay** (`scheduleWithFixedDelay`):
```java
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Task with delay");
    Thread.sleep(3000);  // Task takes 3 seconds
}, 0, 2, TimeUnit.SECONDS);

// Timeline:
// T=0s:  Execution 1 starts
// T=3s:  Execution 1 completes
// T=5s:  Execution 2 starts (3s + 2s delay)
// T=8s:  Execution 2 completes
// T=10s: Execution 3 starts (8s + 2s delay)

// Note: Delay always measured from completion time
```

**Comparison Table**:

| Aspect | scheduleAtFixedRate | scheduleWithFixedDelay |
|--------|---------------------|------------------------|
| **Interval** | From start time | From completion time |
| **Drift** | Can drift if task > period | No drift |
| **Overlap** | Tasks can queue up | Never overlaps |
| **Use Case** | Fixed frequency (heartbeat) | Rate limiting |

**Exception Handling**:
```java
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Task running");
    if (Math.random() > 0.5) {
        throw new RuntimeException("Random failure");
    }
}, 0, 1, TimeUnit.SECONDS);

// Behavior: Exception STOPS future executions!
// Solution: Wrap in try-catch
scheduler.scheduleAtFixedRate(() -> {
    try {
        // Task logic
    } catch (Exception e) {
        log.error("Task failed", e);
    }
}, 0, 1, TimeUnit.SECONDS);
```

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

**Use Cases**:
- **scheduleAtFixedRate**: Heartbeats, metrics collection, health checks
- **scheduleWithFixedDelay**: Rate-limited API calls, cache refresh, cleanup tasks
- **schedule**: One-time delayed operations, timeout handlers

**Pitfalls**:
- **Silent Failures**: Uncaught exceptions stop periodic tasks
- **Queue Overflow**: DelayedWorkQueue can grow unbounded
- **Time Drift**: Fixed rate can accumulate delays if tasks are slow

---

### 3.5 WorkStealingPool (Java 8+)

**ForkJoinPool-based, work-stealing algorithm**

#### Theory & Internal Mechanics

**Implementation**:
```java
public static ExecutorService newWorkStealingPool() {
    return new ForkJoinPool(
        Runtime.getRuntime().availableProcessors(),  // Parallelism = CPU cores
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        true  // asyncMode = true (FIFO for async tasks)
    );
}
```

**Key Characteristics**:
1. **Work-Stealing Algorithm**: Idle threads steal tasks from busy threads' queues
2. **Deque Per Thread**: Each worker thread has its own double-ended queue
3. **Fork/Join Framework**: Optimized for recursive divide-and-conquer algorithms
4. **CPU-Bound Optimization**: Parallelism matches CPU core count
5. **LIFO for Owner, FIFO for Stealers**: Owner pops from head, stealers take from tail

**Work-Stealing Architecture**:
```
Thread 1 Deque:  [T1] [T2] [T3] [T4] [T5]
                  ↑                    ↑
                Owner                Stealer
                (LIFO)              (FIFO)

Thread 2 Deque:  [T6] [T7]
                  ↑
                Owner

Thread 3 Deque:  [] (idle)
                  ↓
            Steals T5 from Thread 1
```

**Execution Flow**:
```
1. Task Submission → Random thread's deque
2. Thread executes own tasks (LIFO - cache locality)
3. Thread finishes → Check own deque
4. Own deque empty → Steal from random thread (FIFO - reduce contention)
5. All deques empty → Thread parks (waits)
6. New task arrives → Wake up parked thread
```

**Work-Stealing Benefits**:
- **Load Balancing**: Automatic distribution of work
- **Cache Locality**: Owner uses LIFO (recent tasks in cache)
- **Low Contention**: Stealer uses FIFO (opposite end of deque)
- **No Central Queue**: Eliminates bottleneck

**Fork/Join Pattern**:
```java
class SumTask extends RecursiveTask<Long> {
    private final long[] array;
    private final int start, end;
    private static final int THRESHOLD = 1000;
    
    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // Base case: compute directly
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // Recursive case: split and fork
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);
            
            left.fork();  // Async execution
            long rightResult = right.compute();  // Sync execution
            long leftResult = left.join();  // Wait for result
            
            return leftResult + rightResult;
        }
    }
}

// Usage
ForkJoinPool pool = new ForkJoinPool();
long result = pool.invoke(new SumTask(array, 0, array.length));
```

**Parallelism vs Thread Count**:
```java
// Parallelism = 4 (4 CPU cores)
ForkJoinPool pool = new ForkJoinPool(4);

// Actual thread count can be higher:
// - 4 worker threads (parallelism)
// - Additional threads for blocking tasks
// - Compensation threads when workers block
```

**Async Mode**:
```java
// asyncMode = true (WorkStealingPool default)
// - FIFO for all tasks
// - Better for event-driven async tasks

// asyncMode = false (ForkJoinPool default)
// - LIFO for owner, FIFO for stealers
// - Better for recursive divide-and-conquer
```

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

**Performance Characteristics**:
- **Throughput**: Excellent for CPU-bound tasks (near-linear scaling)
- **Latency**: Low for parallel computations
- **Overhead**: Higher than ThreadPoolExecutor (work-stealing complexity)

**Use Cases**:
- Parallel stream operations (`parallelStream()`)
- Recursive algorithms (merge sort, quicksort)
- Matrix operations, image processing
- MapReduce-style computations

**Pitfalls**:
- **Not for I/O**: Blocking I/O wastes worker threads
- **Compensation Threads**: Blocking creates extra threads → overhead
- **Complex Debugging**: Work-stealing makes stack traces harder to follow
- **Overkill for Simple Tasks**: Higher overhead than FixedThreadPool

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
