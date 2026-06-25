# Thread Performance & Concurrency

## 1. Thread Model Overview

### 1.1 Platform Threads vs Virtual Threads

```
┌─────────────────────────────────────────────────────────────────┐
│              Platform Threads (Traditional)                       │
│                                                                  │
│  Java Thread ──── 1:1 ──── OS Thread ──── Kernel Scheduled      │
│                                                                  │
│  Cost: ~1MB stack + OS resources per thread                      │
│  Limit: ~10,000 threads per JVM (practical)                      │
│  Blocking: Holds OS thread during I/O wait                       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              Virtual Threads (Java 21+)                           │
│                                                                  │
│  Virtual Thread ──── M:N ──── Carrier Thread ──── OS Thread      │
│                                                                  │
│  Cost: ~few KB per thread (heap-backed continuation)             │
│  Limit: Millions of concurrent virtual threads                   │
│  Blocking: Unmounts from carrier, carrier serves another VT      │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 When to Use What

| Workload | Platform Threads | Virtual Threads |
|----------|-----------------|-----------------|
| CPU-intensive computation | ✅ (cores = threads) | ❌ (no benefit) |
| I/O-bound (DB, HTTP, file) | ❌ (wastes OS threads) | ✅ (millions ok) |
| Mixed workload | ✅ with pools | ✅ for I/O parts |
| Synchronized blocks | ✅ | ⚠️ (pins carrier) |
| ThreadLocal heavy | ✅ | ⚠️ (overhead per VT) |

## 2. Thread Pool Tuning

### 2.1 Optimal Pool Sizing

```java
// CPU-bound tasks: threads = number of cores
int cpuThreads = Runtime.getRuntime().availableProcessors();

// I/O-bound tasks: threads = cores × (1 + wait_time / compute_time)
// Example: 8 cores, 200ms wait, 50ms compute
int ioThreads = 8 * (1 + 200 / 50); // = 40 threads

// Mixed: separate pools for CPU and I/O work
ExecutorService cpuPool = Executors.newFixedThreadPool(cpuThreads);
ExecutorService ioPool = Executors.newFixedThreadPool(ioThreads);
```

### 2.2 ThreadPoolExecutor Configuration

```java
// Production-grade thread pool
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                                    // corePoolSize
    50,                                    // maximumPoolSize
    60L, TimeUnit.SECONDS,                 // keepAliveTime for idle threads
    new LinkedBlockingQueue<>(1000),       // bounded queue (IMPORTANT!)
    new ThreadFactory() {                  // named threads for debugging
        private final AtomicInteger counter = new AtomicInteger();
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "worker-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // backpressure strategy
);

// CRITICAL: Always use bounded queue! Unbounded → OOM under load
```

### 2.3 Rejection Policies

| Policy | Behavior | Use Case |
|--------|----------|----------|
| AbortPolicy | Throws RejectedExecutionException | Fail fast, alert ops |
| CallerRunsPolicy | Caller thread executes the task | Natural backpressure |
| DiscardPolicy | Silently drops the task | Fire-and-forget tasks |
| DiscardOldestPolicy | Drops oldest in queue | Latest-value-wins |

### 2.4 ForkJoinPool (Work-Stealing)

```java
// Default common pool (used by parallel streams, CompletableFuture)
ForkJoinPool commonPool = ForkJoinPool.commonPool();
// Size = Runtime.getRuntime().availableProcessors() - 1

// Custom ForkJoinPool for isolation
ForkJoinPool customPool = new ForkJoinPool(
    16,                                    // parallelism
    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
    null,                                  // UncaughtExceptionHandler
    true                                   // asyncMode (FIFO, good for events)
);

// Override common pool size
-Djava.util.concurrent.ForkJoinPool.common.parallelism=16
```

## 3. Virtual Threads (Java 21+)

### 3.1 Basic Usage

```java
// Create virtual threads
Thread vt = Thread.ofVirtual().name("vt-worker").start(() -> {
    // I/O operation — virtual thread unmounts from carrier
    String result = httpClient.send(request, BodyHandlers.ofString()).body();
    processResult(result);
});

// Virtual thread executor (preferred for I/O-bound servers)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Submit millions of tasks — each gets its own virtual thread
    List<Future<String>> futures = urls.stream()
        .map(url -> executor.submit(() -> fetch(url)))
        .toList();
    
    for (Future<String> f : futures) {
        results.add(f.get());
    }
}
```

### 3.2 Spring Boot with Virtual Threads

```yaml
# application.yml (Spring Boot 3.2+)
spring:
  threads:
    virtual:
      enabled: true  # All request-handling uses virtual threads
```

### 3.3 Virtual Thread Pitfalls

```java
// Pitfall 1: synchronized blocks PIN the carrier thread
synchronized (lock) {
    // Virtual thread is PINNED to carrier — defeats the purpose!
    database.query("SELECT ...");
}

// Fix: Use ReentrantLock instead
private final ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    database.query("SELECT ...");  // Virtual thread can unmount during I/O
} finally {
    lock.unlock();
}

// Pitfall 2: Heavy ThreadLocal usage
// Each virtual thread gets its own copy — millions of copies = memory issue
private static final ThreadLocal<ExpensiveObject> TL = 
    ThreadLocal.withInitial(ExpensiveObject::new);
// Fix: Use ScopedValue (Java 21 preview) for virtual threads

// Pitfall 3: CPU-bound work on virtual threads = no benefit
// Virtual threads only help when blocking on I/O
```

### 3.4 Monitoring Virtual Threads

```bash
# JFR events for virtual threads
-XX:StartFlightRecording=settings=default,filename=vt.jfr

# Thread dump shows virtual threads
jcmd <pid> Thread.dump_to_file -format=json threads.json

# Key metrics to watch:
# - Carrier thread pool utilization (should not be 100%)
# - Pinned virtual thread count (should be 0 or near 0)
# - Virtual thread count vs active carrier threads
```

## 4. Lock Contention & Optimization

### 4.1 Types of Locking in JVM

```
┌─────────────────────────────────────────────────────────────┐
│                   Lock Escalation                             │
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │   Biased    │ → │ Thin Lock   │ → │  Fat Lock   │      │
│  │   Lock      │   │ (CAS spin)  │   │ (OS mutex)  │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                              │
│  No contention     Light contention   Heavy contention      │
│  Zero-cost lock    CAS + short spin   Park thread (expensive)│
└─────────────────────────────────────────────────────────────┘
```

```bash
# Biased locking (removed in Java 18, disable explicitly in 15-17)
-XX:-UseBiasedLocking    # Disable (saves revocation pauses)

# Spin before parking (adaptive)
-XX:+UseSpinning                    # Adaptive spinning (default ON)
```

### 4.2 Detecting Lock Contention

```bash
# Thread dump analysis
jstack -l <pid> > threads.txt
# Look for: "BLOCKED" state, "waiting to lock", "locked"

# JFR lock contention events
# Event: jdk.JavaMonitorWait, jdk.JavaMonitorEnter
# Shows which locks are contended and for how long

# async-profiler lock profiling
./asprof -e lock -d 30 -f locks.html <pid>
```

### 4.3 Lock Contention Patterns & Fixes

```java
// Pattern 1: Coarse-grained lock → Fine-grained
// BAD:
class CoarseCache {
    private final Map<String, Object> cache = new HashMap<>();
    public synchronized Object get(String key) { return cache.get(key); }
    public synchronized void put(String key, Object val) { cache.put(key, val); }
}

// GOOD: ConcurrentHashMap (striped locks internally)
class FineCache {
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    public Object get(String key) { return cache.get(key); }
    public void put(String key, Object val) { cache.put(key, val); }
}

// Pattern 2: Lock splitting (separate locks for independent data)
// BAD:
class Account {
    private double balance;
    private List<Transaction> history;
    private final Object lock = new Object();
    
    public void transfer(double amount) {
        synchronized (lock) { balance += amount; history.add(new Transaction(amount)); }
    }
    public List<Transaction> getHistory() {
        synchronized (lock) { return new ArrayList<>(history); }  // Blocks transfers!
    }
}

// GOOD:
class Account {
    private final ReentrantLock balanceLock = new ReentrantLock();
    private final ReadWriteLock historyLock = new ReentrantReadWriteLock();
    private double balance;
    private List<Transaction> history = new ArrayList<>();
    
    public void transfer(double amount) {
        balanceLock.lock();
        try { balance += amount; } finally { balanceLock.unlock(); }
        historyLock.writeLock().lock();
        try { history.add(new Transaction(amount)); } finally { historyLock.writeLock().unlock(); }
    }
    public List<Transaction> getHistory() {
        historyLock.readLock().lock();
        try { return new ArrayList<>(history); } finally { historyLock.readLock().unlock(); }
    }
}

// Pattern 3: Lock-free with CAS
class LockFreeCounter {
    private final AtomicLong counter = new AtomicLong(0);
    
    public void increment() {
        counter.incrementAndGet();  // CAS loop internally, no lock
    }
    
    public long get() {
        return counter.get();
    }
}

// Pattern 4: LongAdder for high-contention counters
// Better than AtomicLong when many threads update same counter
class HighThroughputCounter {
    private final LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment();  // Striped cells, much less CAS contention
    }
    
    public long get() {
        return counter.sum();  // Slightly stale, but fast
    }
}
```

## 5. False Sharing

### 5.1 Understanding Cache Lines

```
CPU Core 0                         CPU Core 1
┌──────────────┐                   ┌──────────────┐
│   L1 Cache   │                   │   L1 Cache   │
│ ┌──────────┐ │                   │ ┌──────────┐ │
│ │Cache Line│ │ ← 64 bytes →     │ │Cache Line│ │
│ │[counter1]│ │   SAME line!     │ │[counter2]│ │
│ │[counter2]│ │                   │ │[counter1]│ │
│ └──────────┘ │                   │ └──────────┘ │
└──────────────┘                   └──────────────┘
        │                                  │
        └──── Cache coherence protocol ────┘
              (MESI: invalidates other core's cache line
               even though they wrote to DIFFERENT fields!)
```

### 5.2 Detecting and Fixing False Sharing

```java
// Detect: perf tool (Linux)
// $ perf c2c record -g -- java -jar app.jar
// $ perf c2c report

// Fix 1: @Contended annotation
import jdk.internal.vm.annotation.Contended;

class Counters {
    @Contended volatile long counter1;  // Gets its own cache line
    @Contended volatile long counter2;  // Gets its own cache line
}
// Requires: --add-opens java.base/jdk.internal.vm.annotation=ALL-UNNAMED
// Or: -XX:-RestrictContended

// Fix 2: Manual padding
class PaddedCounters {
    volatile long counter1;
    long p1, p2, p3, p4, p5, p6, p7;  // 56 bytes padding
    volatile long counter2;
    long p8, p9, p10, p11, p12, p13, p14; // 56 bytes padding
}

// Fix 3: Array-based with stride
// Ensure each thread's slot is on different cache line
long[] counters = new long[numThreads * 8]; // 8 longs = 64 bytes per slot
// Thread i uses counters[i * 8]
```

### 5.3 Benchmark: Impact of False Sharing

```java
@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
public class FalseSharingBenchmark {
    // With false sharing
    volatile long value1;
    volatile long value2;  // Same cache line as value1!
    
    // Without false sharing
    @Contended volatile long paddedValue1;
    @Contended volatile long paddedValue2;

    @Benchmark @Group("shared") @GroupThreads(1)
    public long writer1() { return ++value1; }
    
    @Benchmark @Group("shared") @GroupThreads(1)
    public long writer2() { return ++value2; }
    
    @Benchmark @Group("padded") @GroupThreads(1)
    public long paddedWriter1() { return ++paddedValue1; }
    
    @Benchmark @Group("padded") @GroupThreads(1)
    public long paddedWriter2() { return ++paddedValue2; }
}
// Result: padded version is 5-20x faster on multi-core!
```

## 6. Context Switching Overhead

### 6.1 Cost of Context Switch

```
Voluntary Context Switch (thread yields/waits):     ~1-5 µs
Involuntary Context Switch (preempted by scheduler): ~5-15 µs
+ Cache warm-up after switch:                        ~10-100 µs

Rule: If compute per task < 10µs, context switch dominates
```

### 6.2 Reducing Context Switches

```bash
# Monitor context switches
vmstat 1          # 'cs' column = context switches/sec
pidstat -w -p <pid> 1  # Per-process context switches

# Strategies:
# 1. Reduce thread count to match CPU cores (for CPU work)
# 2. Use non-blocking I/O (Netty, reactor) instead of thread-per-request
# 3. Batch operations to amortize switch cost
# 4. Pin threads to cores (thread affinity) for ultra-low latency
```

```java
// Thread affinity (using net.openhft:affinity library)
import net.openhft.affinity.AffinityLock;

try (AffinityLock lock = AffinityLock.acquireLock()) {
    // This thread is pinned to a specific CPU core
    // No context switches to other cores, warm L1/L2 cache
    runLatencySensitiveLoop();
}
```

## 7. Concurrent Data Structures Performance

### 7.1 Comparison

| Structure | Read | Write | Best For |
|-----------|------|-------|----------|
| HashMap + synchronized | Slow | Slow | Never use in production |
| ConcurrentHashMap | Fast | Fast | General concurrent map |
| CopyOnWriteArrayList | Very Fast | Slow | Read-heavy, rare writes |
| ConcurrentLinkedQueue | Fast | Fast | MPMC queue |
| ArrayBlockingQueue | Medium | Medium | Bounded producer-consumer |
| LinkedBlockingQueue | Medium | Medium | Unbounded (careful!) |
| Disruptor (LMAX) | Ultra-fast | Ultra-fast | Ultra-low-latency |

### 7.2 ConcurrentHashMap Tuning

```java
// Constructor parameters matter for performance
ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>(
    1024,    // initialCapacity: avoid resize under load
    0.75f,   // loadFactor
    16       // concurrencyLevel: hint for internal striping
);

// Atomic compute operations (single lock, no race condition)
map.computeIfAbsent("key", k -> expensiveCreate(k));
map.merge("counter", 1L, Long::sum);  // Atomic increment

// Parallel bulk operations (uses ForkJoinPool)
map.forEach(10_000, (key, value) -> process(key, value));
// parallelismThreshold=10_000: go parallel if map size > 10K
```

### 7.3 Disruptor Pattern (Ultra-Low Latency)

```java
// LMAX Disruptor: lock-free ring buffer, ~100ns per operation
// Used in: financial trading, real-time analytics

// Concept:
// - Fixed-size ring buffer (pre-allocated)
// - Sequence-based coordination (no locks)
// - Single-writer principle (one producer per sequence)
// - Mechanical sympathy (cache-line aware)

Disruptor<OrderEvent> disruptor = new Disruptor<>(
    OrderEvent::new,           // Event factory
    1024 * 1024,               // Ring buffer size (power of 2)
    DaemonThreadFactory.INSTANCE,
    ProducerType.SINGLE,       // Single producer (fastest)
    new BusySpinWaitStrategy() // Spin wait (lowest latency, uses CPU)
);

disruptor.handleEventsWith(new OrderHandler());
disruptor.start();

// Publish event (allocation-free!)
RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
long sequence = ringBuffer.next();
try {
    OrderEvent event = ringBuffer.get(sequence);
    event.setOrderId(orderId);
    event.setPrice(price);
} finally {
    ringBuffer.publish(sequence);
}
```

## 8. Thread Dump Analysis

### 8.1 Capturing Thread Dumps

```bash
# Method 1: jstack
jstack -l <pid> > thread_dump.txt

# Method 2: jcmd (preferred)
jcmd <pid> Thread.print > thread_dump.txt

# Method 3: kill signal (Unix)
kill -3 <pid>  # Outputs to stdout/stderr

# Method 4: Programmatic
ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);

# Multiple dumps for contention analysis (3 dumps, 5 sec apart)
for i in 1 2 3; do jstack <pid> > dump_$i.txt; sleep 5; done
```

### 8.2 Thread States

```
┌───────────────────────────────────────────────────────────────┐
│                    Thread State Machine                         │
│                                                                │
│  NEW ──start()──► RUNNABLE ◄──notify()/timeout──┐            │
│                       │                          │            │
│                       │ synchronized (contended)  │            │
│                       ▼                          │            │
│                    BLOCKED ──(lock acquired)──► RUNNABLE      │
│                       │                          ↑            │
│                       │ wait()/join()             │            │
│                       ▼                          │            │
│                    WAITING ───────────────────────┘            │
│                    TIMED_WAITING                               │
│                       │                                       │
│                       │ finished                               │
│                       ▼                                       │
│                    TERMINATED                                  │
└───────────────────────────────────────────────────────────────┘
```

### 8.3 Common Patterns in Thread Dumps

```
# Deadlock (two threads waiting for each other's lock)
"Thread-1":
  waiting to lock 0x000000076ab39d60 (a java.lang.Object)
  which is held by "Thread-2"
"Thread-2":
  waiting to lock 0x000000076ab39d48 (a java.lang.Object)  
  which is held by "Thread-1"
→ Fix: Consistent lock ordering, or use tryLock with timeout

# Thread pool exhaustion (all threads blocked on I/O)
"http-nio-8080-exec-1" TIMED_WAITING (parking)
"http-nio-8080-exec-2" TIMED_WAITING (parking)
... (all 200 threads waiting)
→ Fix: Increase pool size, add timeouts, use async I/O

# Lock contention (many threads BLOCKED on same monitor)
"worker-1" BLOCKED waiting for 0x00000000f8a09d60
"worker-2" BLOCKED waiting for 0x00000000f8a09d60
"worker-3" BLOCKED waiting for 0x00000000f8a09d60
→ Fix: Reduce critical section, use concurrent data structures
```

## 9. Thread Performance Flags

```bash
# Stack size
-Xss512k                          # Thread stack size (reduce for many threads)

# Virtual threads (Java 21)
# No specific flags needed, just use the API

# Thread priorities (mostly advisory on Linux)
# Linux NPTL: Java priorities map to nice values
# Real-time threads require: -XX:+UseThreadPriorities

# Biased locking (removed Java 18+)
-XX:-UseBiasedLocking             # Disable in Java 15-17

# Park/unpark optimization
-XX:+UseSpinning                  # Spin before parking (default)
-XX:PreBlockSpin=10               # Spin iterations before blocking

# Monitoring
-XX:+PrintConcurrentLocks         # Print java.util.concurrent locks in jstack
```

## 10. Performance Patterns Summary

```
┌────────────────────────────────────────────────────────────────┐
│                Thread Performance Decision Tree                  │
│                                                                 │
│  Is your workload...                                           │
│                                                                 │
│  CPU-bound?                                                     │
│  → Fixed thread pool = number of cores                         │
│  → Consider ForkJoinPool for recursive/parallel work           │
│  → Pin threads to cores for ultra-low latency                  │
│                                                                 │
│  I/O-bound?                                                     │
│  → Java 21+: Use virtual threads (simplest)                   │
│  → Pre-21: Larger thread pool = cores × (1 + wait/compute)    │
│  → High scale: Reactive (Netty/WebFlux) but complex           │
│                                                                 │
│  High contention?                                               │
│  → Use java.util.concurrent over synchronized                  │
│  → ConcurrentHashMap over Collections.synchronizedMap          │
│  → LongAdder over AtomicLong for counters                     │
│  → ReadWriteLock for read-heavy workloads                      │
│  → Striped locks (Guava) for per-key locking                  │
│                                                                 │
│  Ultra-low latency?                                             │
│  → Disruptor pattern (lock-free ring buffer)                   │
│  → Thread affinity (pin to CPU core)                           │
│  → Avoid allocations in hot path                               │
│  → @Contended for frequently written fields                    │
└────────────────────────────────────────────────────────────────┘
```
