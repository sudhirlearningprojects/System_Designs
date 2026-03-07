# Java Memory Issues in Enterprise Applications — Deep Dive

## JVM Memory Architecture (Quick Recap)

```
JVM Memory
├── Heap
│   ├── Young Generation
│   │   ├── Eden Space         ← new objects allocated here
│   │   ├── Survivor S0
│   │   └── Survivor S1
│   └── Old Generation (Tenured) ← long-lived objects promoted here
├── Non-Heap
│   ├── Metaspace             ← class metadata (replaced PermGen in Java 8)
│   ├── Code Cache            ← JIT-compiled native code
│   └── Thread Stacks         ← one stack per thread
└── Off-Heap (Direct Memory)  ← NIO ByteBuffers, Netty, etc.
```

Understanding which region is exhausted is the first step in diagnosing any memory issue.

---

## Issue 1 — OutOfMemoryError: Java Heap Space

### What it is
The most common OOM. The heap is full and GC cannot reclaim enough space to satisfy an allocation request.

### Root Causes
- Memory leak — objects are referenced but never used again
- Unbounded caches — `HashMap` or `List` growing indefinitely
- Loading large datasets into memory (entire DB result set, large file)
- Session accumulation in web apps
- Event listeners never deregistered

### How to Fix

**Unbounded cache → use bounded cache with eviction:**
```java
// Bad
Map<String, Object> cache = new HashMap<>();  // grows forever

// Good — Caffeine with size + TTL eviction
Cache<String, Object> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(10))
    .build();
```

**Large DB result set → stream/paginate:**
```java
// Bad — loads all rows into memory
List<Order> all = orderRepo.findAll();

// Good — paginate
Pageable page = PageRequest.of(0, 500);
Page<Order> batch = orderRepo.findAll(page);

// Good — JPA stream (cursor-based)
@Query("SELECT o FROM Order o")
@QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "500"))
Stream<Order> streamAll();
```

**Deregister listeners:**
```java
// Always remove listeners when component is destroyed
eventBus.unregister(this);
applicationContext.removeBeanFactoryPostProcessor(processor);
```

### How to Monitor
- JVM flag: `-Xmx2g -Xms2g` (set max heap)
- GC logs: `-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=20m`
- Heap dump on OOM: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/`
- Tools: **VisualVM**, **Eclipse MAT** (Memory Analyzer Tool), **JProfiler**
- Metrics: `jvm.memory.used`, `jvm.gc.pause` via Micrometer → Prometheus → Grafana

---

## Issue 2 — Memory Leak

### What it is
Objects are no longer needed but are still reachable via a reference chain, preventing GC from collecting them. Heap usage grows monotonically over time.

### Common Leak Patterns in Enterprise Java

**Static collections holding references:**
```java
// Bad — static map holds User objects forever
private static final Map<String, User> SESSION_CACHE = new HashMap<>();
```

**Inner class holding outer class reference:**
```java
// Bad — anonymous Runnable captures 'this' (the enclosing service)
executor.submit(new Runnable() {
    public void run() { /* uses outer class fields */ }
});

// Good — static nested class or lambda with explicit capture
executor.submit(() -> processData(data));  // only captures what's needed
```

**ThreadLocal not cleaned up:**
```java
// Bad — in thread pool, thread is reused, ThreadLocal value persists
ThreadLocal<UserContext> ctx = new ThreadLocal<>();
ctx.set(new UserContext());
// ... forgot ctx.remove()

// Good
try {
    ctx.set(new UserContext());
    doWork();
} finally {
    ctx.remove();  // always clean up in thread pools
}
```

**Unclosed resources (pre-try-with-resources):**
```java
// Bad
Connection conn = dataSource.getConnection();
// exception thrown — conn never closed, connection pool exhausted

// Good
try (Connection conn = dataSource.getConnection()) {
    // auto-closed
}
```

**JPA/Hibernate first-level cache in long transactions:**
```java
// Bad — processing 1M records in one transaction, EntityManager cache grows
for (Record r : records) {
    entityManager.persist(r);  // all stay in L1 cache
}

// Good — flush and clear periodically
for (int i = 0; i < records.size(); i++) {
    entityManager.persist(records.get(i));
    if (i % 500 == 0) {
        entityManager.flush();
        entityManager.clear();
    }
}
```

### How to Monitor
- Heap dump analysis with **Eclipse MAT**: look for "Leak Suspects" report
- **Retained heap** of a class growing over time = leak candidate
- Heap histogram: `jmap -histo:live <pid> | head -30`
- GC overhead increasing while heap never fully reclaims = leak signal
- Micrometer: track `jvm.memory.used` over time — steady upward trend = leak

---

## Issue 3 — OutOfMemoryError: Metaspace

### What it is
Metaspace (Java 8+) stores class metadata. It grows when classes are loaded and is only reclaimed when the classloader is GC'd. In enterprise apps with dynamic class generation, it can exhaust.

### Root Causes
- Frameworks generating proxies at runtime (Spring AOP, Hibernate, CGLIB, ByteBuddy)
- Hot redeployment in app servers (Tomcat, JBoss) — old classloaders not GC'd
- Groovy/scripting engines compiling scripts repeatedly
- Reflection-heavy frameworks creating synthetic classes

### How to Fix
```bash
# Set Metaspace limit (default is unlimited — dangerous in containers)
-XX:MaxMetaspaceSize=256m

# Enable GC of class metadata
-XX:+CMSClassUnloadingEnabled   # for CMS GC
# G1/ZGC handle this automatically
```

**For hot redeployment leaks:** use application server's proper undeploy mechanism, not just redeploy. Ensure no static references to application classloader from server-level code.

### How to Monitor
- `jstat -gcmetacapacity <pid>`
- Micrometer: `jvm.memory.used{area="nonheap",id="Metaspace"}`
- Alert when Metaspace > 80% of `MaxMetaspaceSize`

---

## Issue 4 — OutOfMemoryError: Unable to Create New Native Thread

### What it is
The JVM cannot create a new OS thread. This is not a heap issue — it's an OS-level resource exhaustion.

### Root Causes
- Thread leak — threads created but never terminated
- Unbounded thread pool (`Executors.newCachedThreadPool()` under high load)
- Each thread consumes ~512KB–1MB of native stack memory
- OS limit on threads per process (`ulimit -u`)

### How to Fix
```java
// Bad — unbounded, creates new thread per task under load
ExecutorService exec = Executors.newCachedThreadPool();

// Good — bounded thread pool
ExecutorService exec = new ThreadPoolExecutor(
    10, 50,                          // core, max threads
    60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000), // bounded queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // backpressure
);

// Java 21+ — virtual threads for I/O-bound work (no native thread per task)
ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
```

**Check for thread leaks:**
```bash
# Thread count
jstack <pid> | grep "java.lang.Thread.State" | wc -l

# Full thread dump
jstack <pid> > thread_dump.txt
```

### How to Monitor
- Micrometer: `jvm.threads.live`, `jvm.threads.daemon`, `jvm.threads.peak`
- Alert when live threads > expected max (e.g., > 500 for a typical service)
- Thread dump analysis: look for many threads in `WAITING` or `TIMED_WAITING` on same stack = thread pool starvation

---

## Issue 5 — GC Overhead Limit Exceeded

### What it is
JVM throws `OutOfMemoryError: GC overhead limit exceeded` when more than 98% of time is spent in GC but less than 2% of heap is reclaimed. The app is effectively frozen doing GC.

### Root Causes
- Heap too small for the workload
- Memory leak causing heap to fill up
- Excessive object creation rate (allocation pressure)
- Inefficient data structures (many small short-lived objects)

### How to Fix
```bash
# Increase heap
-Xmx4g

# Switch to low-pause GC (G1 is default since Java 9, ZGC for large heaps)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# ZGC for heaps > 4GB, sub-millisecond pauses (Java 15+ production-ready)
-XX:+UseZGC

# Shenandoah (RedHat, available in OpenJDK)
-XX:+UseShenandoahGC
```

**Reduce allocation pressure:**
```java
// Bad — creates new StringBuilder per iteration
for (String s : list) result += s;

// Good — reuse
StringBuilder sb = new StringBuilder();
for (String s : list) sb.append(s);

// Use object pooling for expensive objects
ObjectPool<ExpensiveObject> pool = new GenericObjectPool<>(factory);
```

### How to Monitor
- GC logs: `-Xlog:gc*` — look for Full GC frequency and duration
- Micrometer: `jvm.gc.pause` (duration), `jvm.gc.memory.promoted`
- Alert: Full GC > once per minute = problem
- **GCViewer** or **GCEasy** for GC log analysis

---

## Issue 6 — Direct Memory / Off-Heap OOM

### What it is
`OutOfMemoryError: Direct buffer memory` — NIO direct ByteBuffers, Netty, and off-heap caches allocate memory outside the heap. This is not controlled by `-Xmx`.

### Root Causes
- Netty/gRPC/WebFlux not releasing ByteBuf after use
- `ByteBuffer.allocateDirect()` without explicit cleanup
- Off-heap caches (Ehcache off-heap, MapDB) growing unbounded

### How to Fix
```bash
# Set direct memory limit
-XX:MaxDirectMemorySize=512m
```

```java
// Netty — always release ByteBuf
ByteBuf buf = ctx.alloc().buffer();
try {
    // use buf
} finally {
    buf.release();  // decrements reference count
}

// NIO — direct buffers are GC'd but not promptly; force cleanup
((DirectBuffer) byteBuffer).cleaner().clean();
```

### How to Monitor
- Micrometer: `jvm.buffer.memory.used{id="direct"}`
- `jcmd <pid> VM.native_memory` — shows direct memory usage
- Alert when direct memory > 80% of `MaxDirectMemorySize`

---

## Issue 7 — Stack Overflow

### What it is
`StackOverflowError` — each thread has a fixed stack. Deep or infinite recursion exhausts it.

### Root Causes
- Infinite recursion (missing base case)
- Deeply nested method calls (e.g., recursive JSON/XML parsing of deeply nested structures)
- Circular object graph serialization

### How to Fix
```java
// Bad — recursive without base case or too deep
int factorial(int n) { return n * factorial(n - 1); }

// Good — iterative
int factorial(int n) {
    int result = 1;
    for (int i = 2; i <= n; i++) result *= i;
    return result;
}

// Good — tail-recursive with trampoline pattern for very deep recursion
// Or use explicit Stack<> to simulate recursion iteratively
```

```bash
# Increase thread stack size (use carefully — increases memory per thread)
-Xss2m  # default is 512k-1m
```

---

## Issue 8 — Connection Pool Exhaustion (Logical Memory Leak)

### What it is
Not a JVM memory issue, but behaves like one — all connections in the pool are held, new requests block or fail. Common in HikariCP, JDBC pools.

### Root Causes
- Long-running transactions holding connections
- Connections not returned (exception path skips `close()`)
- N+1 query problem causing excessive connection hold time
- Misconfigured pool size

### How to Fix
```java
// Always use try-with-resources
try (Connection conn = dataSource.getConnection()) { ... }

// HikariCP config
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.connection-timeout=3000   # fail fast
spring.datasource.hikari.leak-detection-threshold=5000  # log if held > 5s
```

### How to Monitor
- Micrometer: `hikaricp.connections.active`, `hikaricp.connections.pending`
- Alert when `pending > 0` for sustained period
- HikariCP leak detection logs: `Connection leak detection triggered`

---

## Monitoring Stack for Enterprise Java

### JVM Metrics via Micrometer + Prometheus + Grafana

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Key metrics to alert on:

| Metric | Alert Threshold | Issue |
|---|---|---|
| `jvm.memory.used{area="heap"}` | > 85% of max | Heap pressure |
| `jvm.memory.used{area="nonheap",id="Metaspace"}` | > 80% of max | Metaspace leak |
| `jvm.gc.pause` | p99 > 1s | GC pressure |
| `jvm.threads.live` | > expected max | Thread leak |
| `jvm.buffer.memory.used{id="direct"}` | > 80% of max | Direct memory |
| `hikaricp.connections.pending` | > 0 sustained | Pool exhaustion |
| `jvm.gc.memory.promoted` | Rapid growth | Old gen pressure |

### JVM Flags for Production

```bash
# Heap
-Xms2g -Xmx2g                          # same min/max avoids resize pauses

# GC
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m

# OOM diagnostics
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/dumps/heap.hprof
-XX:+ExitOnOutOfMemoryError             # restart instead of limping

# Metaspace
-XX:MaxMetaspaceSize=256m

# Direct memory
-XX:MaxDirectMemorySize=512m

# Thread stack
-Xss512k
```

### Heap Dump Analysis Workflow

```bash
# 1. Trigger heap dump manually
jmap -dump:format=b,file=heap.hprof <pid>
# or via jcmd
jcmd <pid> GC.heap_dump /dumps/heap.hprof

# 2. Analyze with Eclipse MAT
# - Run "Leak Suspects" report
# - Check "Dominator Tree" — largest retained objects
# - Check "Histogram" — class with most instances

# 3. Quick histogram (no dump needed)
jmap -histo:live <pid> | head -30
```

### Thread Dump Analysis Workflow

```bash
# Capture 3 dumps 10 seconds apart
jstack <pid> > dump1.txt && sleep 10 && jstack <pid> > dump2.txt

# Look for:
# - Many threads BLOCKED on same lock = deadlock or contention
# - Many threads WAITING on same queue = pool starvation
# - Same stack trace repeated = thread leak pattern

# Automated: use fastthread.io or samurai for visual analysis
```

### APM Tools for Enterprise

| Tool | Best For |
|---|---|
| **Elastic APM** | Full-stack tracing + JVM metrics |
| **Datadog APM** | Enterprise, auto-instrumentation |
| **New Relic** | JVM profiling + alerting |
| **Dynatrace** | AI-based anomaly detection |
| **Pyroscope** | Continuous profiling (open source) |
| **Async-profiler** | Low-overhead CPU + allocation profiling |
| **JProfiler / YourKit** | Deep local profiling |

### Async-profiler (Best Free Tool for Production Profiling)

```bash
# CPU profiling
./profiler.sh -d 30 -f cpu.html <pid>

# Allocation profiling — find what's creating the most objects
./profiler.sh -d 30 -e alloc -f alloc.html <pid>

# Wall-clock profiling — includes I/O wait
./profiler.sh -d 30 -e wall -f wall.html <pid>
```

---

## Summary — Issue → Cause → Fix → Monitor

| OOM Type | Primary Cause | Fix | Key Metric |
|---|---|---|---|
| Heap Space | Leak / unbounded data | Bounded cache, pagination, fix leak | `jvm.memory.used` heap |
| Metaspace | Class loader leak | Limit Metaspace, fix undeploy | `jvm.memory.used` nonheap |
| Native Thread | Thread leak / unbounded pool | Bounded pool, virtual threads | `jvm.threads.live` |
| GC Overhead | Heap too small / leak | Increase heap, tune GC, fix leak | `jvm.gc.pause` |
| Direct Buffer | Netty/NIO not releasing | Release ByteBuf, limit direct mem | `jvm.buffer.memory.used` |
| Stack Overflow | Deep recursion | Iterative approach, increase `-Xss` | Stack trace in logs |
| Connection Pool | Long tx / not closed | try-with-resources, leak detection | `hikaricp.connections.pending` |

---

## Interview Questions

### Beginner

**Q1. What is the difference between heap and stack memory in Java?**
Heap stores objects and is shared across threads. Stack stores method frames (local variables, return addresses) and is per-thread. Stack is LIFO and much smaller than heap.

**Q2. What causes `OutOfMemoryError: Java heap space`?**
Heap is full and GC cannot reclaim enough space. Caused by memory leaks, unbounded collections, or loading too much data into memory at once.

**Q3. What is a memory leak in Java? Can Java have memory leaks despite GC?**
Yes. A memory leak in Java means objects are still referenced (reachable) but never used again. GC only collects unreachable objects. Static collections, unclosed resources, and forgotten listeners are common causes.

**Q4. What is the difference between PermGen and Metaspace?**
PermGen (pre-Java 8) was a fixed-size heap region for class metadata — easy to exhaust. Metaspace (Java 8+) uses native memory and grows dynamically. It's still bounded by `-XX:MaxMetaspaceSize` but defaults to unlimited.

**Q5. What does `-Xmx` and `-Xms` do?**
`-Xmx` sets maximum heap size. `-Xms` sets initial heap size. Setting them equal avoids heap resize pauses in production.

---

### Intermediate

**Q6. How do you detect a memory leak in a running Java application?**
1. Monitor heap usage over time — steady upward trend despite GC = leak
2. Take heap dump: `jmap -dump:format=b,file=heap.hprof <pid>`
3. Analyze with Eclipse MAT — "Leak Suspects" report shows objects with large retained heap
4. Check dominator tree for unexpectedly large object graphs

**Q7. What is a ThreadLocal memory leak and how do you prevent it?**
In thread pools, threads are reused. If `ThreadLocal.set()` is called without `ThreadLocal.remove()`, the value persists on the thread indefinitely. Fix: always call `remove()` in a `finally` block.

**Q8. What is GC overhead limit exceeded?**
JVM spends > 98% of time in GC but reclaims < 2% of heap. The app is effectively frozen. Caused by heap being too small or a memory leak filling the heap. Fix: increase heap, fix leak, or tune GC.

**Q9. What is the difference between minor GC and major/full GC?**
- Minor GC: collects Young Generation (Eden + Survivors). Fast, frequent.
- Major GC: collects Old Generation. Slower, less frequent.
- Full GC: collects entire heap + Metaspace. Causes stop-the-world pause. Should be rare.

**Q10. How does G1GC differ from the old CMS/Parallel GC?**
G1 divides heap into equal-sized regions (not fixed Young/Old areas). It predicts and targets pause times (`-XX:MaxGCPauseMillis`). It performs concurrent marking and incremental compaction, avoiding long full GC pauses. Default since Java 9.

**Q11. What is direct memory and how can it cause OOM?**
Direct memory is off-heap memory allocated via `ByteBuffer.allocateDirect()` or Netty's `ByteBuf`. It's not controlled by `-Xmx`. If not released, it causes `OutOfMemoryError: Direct buffer memory`. Controlled by `-XX:MaxDirectMemorySize`.

**Q12. How do you find a thread leak?**
Take thread dumps (`jstack <pid>`) at intervals. If thread count grows and threads share the same stack trace (e.g., waiting on a queue), it's a thread leak. Check `jvm.threads.live` metric trend.

---

### Advanced

**Q13. Explain the JVM memory regions and what gets stored where.**
- Eden/Survivors: new object allocations (TLAB — Thread Local Allocation Buffer for fast allocation)
- Old Gen: objects that survived multiple minor GCs (promotion threshold configurable)
- Metaspace: class metadata, method bytecode, constant pool
- Code Cache: JIT-compiled native code
- Thread Stacks: method frames, local primitives, object references (not objects themselves)
- Direct Memory: off-heap NIO/Netty buffers

**Q14. What is TLAB and why does it matter for performance?**
Thread Local Allocation Buffer — each thread gets a private chunk of Eden to allocate into without synchronization. This makes object allocation nearly free (just a pointer bump). When TLAB is exhausted, the thread requests a new one from Eden, which requires synchronization.

**Q15. How would you diagnose and fix a Metaspace leak in a Spring Boot app?**
1. Monitor `jvm.memory.used{id="Metaspace"}` — growing after each request = class generation leak
2. Heap dump → MAT → histogram filtered by `ClassLoader` — look for many classloaders
3. Common cause: CGLIB proxy generation per request (misconfigured Spring AOP scope), Groovy script compilation without caching
4. Fix: ensure proxies are singletons, cache compiled scripts, set `-XX:MaxMetaspaceSize` as a safety net

**Q16. What is object promotion failure and how does it cause Full GC?**
When a Minor GC tries to promote objects from Young Gen to Old Gen but Old Gen doesn't have enough contiguous space, it triggers a Full GC. Caused by Old Gen fragmentation (CMS) or Old Gen being nearly full. G1 handles this better with region-based allocation.

**Q17. How do you profile memory allocation in production without significant overhead?**
Use **async-profiler** with `-e alloc` — it uses AsyncGetCallTrace and perf events, adding < 1% overhead. It shows allocation hot spots (which code paths allocate the most). Alternative: JFR (Java Flight Recorder) with `jcmd <pid> JFR.start` — built into JDK, very low overhead.

**Q18. What is the difference between retained heap and shallow heap in MAT?**
- Shallow heap: memory occupied by the object itself (fields only)
- Retained heap: memory that would be freed if this object were GC'd (the object + everything it exclusively references)
Retained heap is what matters for leak analysis — a small object with large retained heap is a leak root.

**Q19. How does ZGC achieve sub-millisecond pauses?**
ZGC uses colored pointers (load barriers) to track object state. It performs marking, relocation, and remapping concurrently with the application. Stop-the-world pauses are only for root scanning (< 1ms regardless of heap size). Heap size doesn't affect pause time — only the number of GC roots does.

**Q20. How would you handle a production OOM with no heap dump available?**
1. Check GC logs for pattern before OOM (rapid Full GC = heap exhaustion vs sudden = allocation spike)
2. `jmap -histo:live <pid>` on a similar live instance — shows class histogram without full dump
3. Enable `-XX:+HeapDumpOnOutOfMemoryError` going forward
4. Check application logs for the request/operation that triggered it
5. Use JFR continuous recording — it captures memory events even before OOM
6. Compare heap histograms over time to find growing classes
