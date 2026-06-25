# Memory Management & Tuning

## 1. Total JVM Memory Footprint

```
┌─────────────────────────────────────────────────────────────┐
│                TOTAL JVM PROCESS MEMORY (RSS)                 │
│                                                              │
│  ┌──────────────────────────────────────────────┐           │
│  │              Java Heap (-Xmx)                 │           │
│  │         (Objects, Arrays, Strings)            │           │
│  └──────────────────────────────────────────────┘           │
│  ┌───────────────┐  ┌───────────────────────────┐           │
│  │   Metaspace   │  │    Thread Stacks           │           │
│  │  (Classes,    │  │  (N threads × -Xss)        │           │
│  │   Methods)    │  │                            │           │
│  └───────────────┘  └───────────────────────────┘           │
│  ┌───────────────┐  ┌───────────────────────────┐           │
│  │  Code Cache   │  │   Direct ByteBuffers       │           │
│  │  (JIT code)   │  │   (NIO, off-heap)          │           │
│  └───────────────┘  └───────────────────────────┘           │
│  ┌───────────────┐  ┌───────────────────────────┐           │
│  │  GC Overhead  │  │   Native Libraries         │           │
│  │  (card tables,│  │   (JNI, malloc)            │           │
│  │   bitmaps)    │  │                            │           │
│  └───────────────┘  └───────────────────────────┘           │
│  ┌──────────────────────────────────────────────┐           │
│  │         Internal JVM Structures               │           │
│  │  (Symbol table, String table, Compiler)       │           │
│  └──────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘

Rule of thumb: Total RSS ≈ Heap + Metaspace + Threads + 20-30% overhead
Example: 4GB heap + 256MB meta + 200 threads×1MB + overhead ≈ 5.5-6GB RSS
```

## 2. Heap Sizing Strategy

### 2.1 The Golden Rules

```bash
# Rule 1: Set Xms = Xmx (avoid resize pauses)
-Xms4g -Xmx4g

# Rule 2: Leave 30% heap free after Full GC for healthy operation
# If live data = 2GB after Full GC, heap should be ≥ 2GB / 0.7 ≈ 3GB

# Rule 3: For containers, use percentage-based sizing
-XX:MaxRAMPercentage=75.0   # Leave 25% for non-heap

# Rule 4: Pre-touch pages to avoid page faults at runtime
-XX:+AlwaysPreTouch          # Touch all pages at startup
```

### 2.2 Sizing Worksheet

```
Step 1: Determine live data size (heap after Full GC)
        → Run app under load, trigger Full GC, measure

Step 2: Heap size = Live Data × 3 to 4
        → If live data = 1.5GB, heap = 4.5 - 6GB

Step 3: Young Gen = 1 to 1.5 × Live Data
        → Young Gen = 1.5 - 2.25GB

Step 4: Old Gen = Heap - Young Gen
        → Old Gen = 3 - 3.75GB

Step 5: Validate:
        → Old Gen after GC should be < 60% of Old Gen capacity
        → Young GC frequency: every 5-30 seconds is healthy
```

### 2.3 Container Memory Planning

```bash
# Container with 8GB limit
# Memory breakdown:
#   Heap:       75% = 6GB   (-XX:MaxRAMPercentage=75.0)
#   Metaspace:  256MB
#   Threads:    200 × 512KB = 100MB
#   Code Cache: 240MB
#   GC:         ~200MB
#   Native:     ~200MB
#   Buffer:     ~500MB (safety margin for spikes)
#   Total:      ~7.5GB < 8GB ✓

# Dockerfile
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:MaxMetaspaceSize=256m \
               -Xss512k \
               -XX:+UseContainerSupport"
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

## 3. Memory Leak Detection

### 3.1 Symptoms of Memory Leak

```
Heap Usage Over Time:

Normal:              Memory Leak:
   ▲                    ▲
   │  /\/\/\/\          │        /
   │ /        \         │      / ← Baseline keeps rising
   │/          \/\      │    /
   │              \     │  /     ← GC reclaims less each time
   └────────────────    └────────────── → OOM eventually
        Time                 Time
```

**Key Indicators:**
- Old Gen usage after Full GC keeps increasing
- GC frequency increasing over time
- Eventually: `java.lang.OutOfMemoryError: Java heap space`

### 3.2 Heap Dump Analysis

```bash
# Capture heap dump
jmap -dump:live,format=b,file=heap.hprof <pid>

# Auto-dump on OOM (ALWAYS enable in production)
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/java/heapdump-%t.hprof

# Dump via jcmd (preferred, no-attach)
jcmd <pid> GC.heap_dump /tmp/heap.hprof
```

### 3.3 Analyzing with Eclipse MAT

```
Key MAT Views:
1. Dominator Tree    → Who retains the most memory?
2. Leak Suspects     → Automated leak detection
3. Histogram         → Object count by class
4. Path to GC Roots  → Why is this object alive?

Common Leak Patterns Found:
┌─────────────────────────────────────────────────────┐
│ Pattern                    │ Fix                     │
├─────────────────────────────────────────────────────┤
│ Growing HashMap/List       │ Use bounded cache/LRU   │
│ Unclosed resources         │ try-with-resources      │
│ Static collections         │ WeakHashMap or cleanup  │
│ Listener not deregistered  │ Explicit removeListener │
│ ThreadLocal not removed    │ threadLocal.remove()    │
│ ClassLoader leak           │ Fix hot-deploy cycle    │
│ String.intern() abuse      │ Limit intern usage      │
└─────────────────────────────────────────────────────┘
```

### 3.4 Programmatic Leak Detection

```java
// Pattern: Detect growing collections
public class LeakDetector {
    private static final Map<String, Integer> lastSizes = new ConcurrentHashMap<>();
    
    public static void track(String name, Collection<?> collection) {
        int size = collection.size();
        Integer lastSize = lastSizes.put(name, size);
        if (lastSize != null && size > lastSize * 1.5 && size > 10000) {
            log.warn("Potential leak in '{}': size grew from {} to {}", 
                     name, lastSize, size);
        }
    }
}

// Common leak: ThreadLocal not cleaned up
public class RequestContext {
    private static final ThreadLocal<Map<String, Object>> context = 
        ThreadLocal.withInitial(HashMap::new);
    
    public static void set(String key, Object value) {
        context.get().put(key, value);
    }
    
    // CRITICAL: Always call this in finally/filter cleanup
    public static void clear() {
        context.remove(); // Not just .get().clear()!
    }
}
```

## 4. Off-Heap Memory Management

### 4.1 Direct ByteBuffers

```java
// Direct buffer: allocated outside Java heap via malloc
ByteBuffer direct = ByteBuffer.allocateDirect(1024 * 1024); // 1MB off-heap

// Regular buffer: allocated on Java heap
ByteBuffer heap = ByteBuffer.allocate(1024 * 1024); // 1MB on-heap
```

**When to use Direct buffers:**
- I/O-heavy operations (network, file)
- Zero-copy transfers
- Large buffers that would pressure GC

**Monitoring:**
```bash
-XX:MaxDirectMemorySize=1g   # Limit (default = -Xmx)

# Check usage via JMX
jcmd <pid> VM.native_memory summary
# Or programmatically:
# java.nio.Bits.reservedMemory (internal, use reflection or JMX)
```

### 4.2 Memory-Mapped Files

```java
// Memory-mapped file for large data access
try (FileChannel channel = FileChannel.open(Path.of("data.bin"), READ)) {
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    // Direct access to file content without copying to heap
    // OS manages page faults transparently
    int value = buffer.getInt(offset);
}
```

**Performance characteristics:**
- Reads: OS page cache → zero copy into JVM
- No heap pressure (data stays off-heap)
- Random access: excellent for index files, databases
- Caution: unmapping is non-deterministic (may hold file handles)

### 4.3 Unsafe and Foreign Memory (Java 21+)

```java
// Modern: Foreign Function & Memory API (Java 21+)
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate(1024 * 1024); // 1MB off-heap
    segment.set(ValueLayout.JAVA_INT, 0, 42);
    int value = segment.get(ValueLayout.JAVA_INT, 0);
} // Automatically freed when arena closes
```

## 5. String Memory Optimization

### 5.1 Compact Strings (Java 9+)

```
Before Java 9:  String → char[] (UTF-16, always 2 bytes/char)
Java 9+:        String → byte[] + coder flag
                 - Latin1 content → 1 byte/char (50% savings!)
                 - Non-Latin1    → 2 bytes/char (UTF-16)
```

Enabled by default. Disable only if causing issues:
```bash
-XX:-CompactStrings  # Disable (rare)
```

### 5.2 String Deduplication (G1 only)

```bash
# G1 automatically deduplicates String values during GC
-XX:+UseStringDeduplication           # Enable (disabled by default)
-XX:StringDeduplicationAgeThreshold=3 # Strings surviving N GCs

# Impact: Can save 10-25% heap in apps with many duplicate strings
# Overhead: ~2% GC time increase
```

### 5.3 String.intern() Considerations

```java
// intern() puts string in JVM's string pool (native memory)
String s1 = new String("hello").intern();  // Moves to string pool
String s2 = "hello";                       // Already in pool
assert s1 == s2;  // Same reference

// Caution: String pool is a fixed-size hashtable
-XX:StringTableSize=1000003  # Increase if many intern() calls (use prime number)
```

```bash
# Monitor string table
jcmd <pid> VM.stringtable
# Output: StringTable statistics:
# Number of buckets: 60013
# Number of entries: 42567
# Average bucket size: 0.709
```

## 6. Object Allocation Optimization

### 6.1 TLAB (Thread-Local Allocation Buffer)

```
┌─────────────────────────────────────────────┐
│                 EDEN SPACE                    │
│                                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐       │
│  │ Thread1 │ │ Thread2 │ │ Thread3 │       │
│  │  TLAB   │ │  TLAB   │ │  TLAB   │       │
│  │ [███░░] │ │ [████░] │ │ [██░░░] │       │
│  └─────────┘ └─────────┘ └─────────┘       │
│  █=allocated  ░=free                         │
│                                              │
│  Each thread allocates from its own TLAB     │
│  → No contention, just bump the pointer!     │
└─────────────────────────────────────────────┘
```

```bash
# TLAB flags
-XX:+UseTLAB                 # Enabled by default
-XX:TLABSize=512k            # Initial TLAB size (auto-resized)
-XX:MinTLABSize=2k           # Minimum TLAB size
-XX:+ResizeTLAB              # Auto-resize based on allocation rate

# Diagnostic
-XX:+PrintTLAB               # Log TLAB stats (very verbose)
```

### 6.2 Escape Analysis & Scalar Replacement

The JIT compiler can eliminate allocations entirely:

```java
// Before optimization:
public int sumPoints() {
    Point p = new Point(3, 4);  // Allocation
    return p.x + p.y;
}

// After Escape Analysis + Scalar Replacement:
// JIT detects: Point doesn't escape this method
// Replaces with: return 3 + 4;  (no allocation!)
```

```bash
-XX:+DoEscapeAnalysis        # Enabled by default
-XX:+EliminateAllocations    # Scalar replacement (default on)
-XX:+EliminateLocks          # Lock elision for non-escaping objects
```

### 6.3 Object Pooling (When Appropriate)

```java
// Pool expensive objects (DB connections, threads, buffers)
// DON'T pool cheap objects (small POJOs) — GC handles them fine

// Example: Reusable byte buffer pool
public class BufferPool {
    private final Queue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();
    private final int bufferSize;
    private final int maxPoolSize;
    private final AtomicInteger poolSize = new AtomicInteger(0);

    public BufferPool(int bufferSize, int maxPoolSize) {
        this.bufferSize = bufferSize;
        this.maxPoolSize = maxPoolSize;
    }

    public ByteBuffer acquire() {
        ByteBuffer buffer = pool.poll();
        if (buffer != null) {
            buffer.clear();
            return buffer;
        }
        return ByteBuffer.allocateDirect(bufferSize);
    }

    public void release(ByteBuffer buffer) {
        if (poolSize.get() < maxPoolSize) {
            poolSize.incrementAndGet();
            pool.offer(buffer);
        }
        // Otherwise, let GC collect it
    }
}
```

## 7. Metaspace Tuning

### 7.1 What Lives in Metaspace

- Class metadata (Klass structures)
- Method metadata and bytecode
- Constant pool
- Annotations
- Method counters (for JIT decisions)

### 7.2 Metaspace Growth Issues

```bash
# Symptom: Metaspace keeps growing → common in:
# - Application servers with hot-deploy (Tomcat, WildFly)
# - Heavy use of reflection/proxies (Spring AOP, Hibernate)
# - Groovy/Scala scripts compiling at runtime
# - Dynamic class generation (CGLIB, ByteBuddy)

# Fix: Set hard limits
-XX:MetaspaceSize=256m       # GC trigger threshold
-XX:MaxMetaspaceSize=512m    # Hard limit (crash rather than OOM the system)

# Diagnostic
jcmd <pid> GC.class_stats     # Class statistics
jcmd <pid> VM.classloaders    # ClassLoader hierarchy
jstat -gcmetacapacity <pid>   # Metaspace capacity
```

### 7.3 ClassLoader Leak Detection

```java
// Common pattern causing ClassLoader leak:
// A single object from the child classloader is referenced by parent
// → Entire ClassLoader + all its classes can't be GC'd

// Detection:
// In heap dump, look for:
// - Multiple instances of same class from different classloaders
// - ClassLoader instances with high retained size

// Prevention:
// - Use WeakReference for ClassLoader references
// - Clean up ThreadLocals in web apps (Servlet Filter)
// - Avoid static references to dynamically loaded classes
```

## 8. Native Memory Tracking (NMT)

### 8.1 Enable NMT

```bash
# Enable at startup (5-10% overhead in detail mode)
-XX:NativeMemoryTracking=summary   # Low overhead
-XX:NativeMemoryTracking=detail    # High overhead, shows callsites

# Query at runtime
jcmd <pid> VM.native_memory summary
jcmd <pid> VM.native_memory detail

# Baseline and diff (find leaks)
jcmd <pid> VM.native_memory baseline
# ... wait ...
jcmd <pid> VM.native_memory summary.diff
```

### 8.2 Reading NMT Output

```
Native Memory Tracking:
Total: reserved=6234MB, committed=4812MB
                                      ↑ virtual  ↑ physical (RSS)

-            Java Heap (reserved=4096MB, committed=4096MB)
                       (mmap: reserved=4096MB, committed=4096MB)

-                Class (reserved=1056MB, committed=89MB)
                       (classes #14523)
                       (  instance classes #13812, array classes #711)
                       (malloc=2MB #35123) (mmap: reserved=1054MB, committed=87MB)

-               Thread (reserved=512MB, committed=512MB)
                       (thread #502)                     ← 502 threads!
                       (stack: reserved=500MB, committed=500MB)

-                 Code (reserved=250MB, committed=78MB)
                       (malloc=12MB #21234)
                       (mmap: reserved=238MB, committed=66MB)

-                   GC (reserved=198MB, committed=145MB)
                       (malloc=23MB #8192)
                       (mmap: reserved=175MB, committed=122MB)

-              Compiler (reserved=2MB, committed=2MB)

-             Internal (reserved=14MB, committed=14MB)

-               Symbol (reserved=16MB, committed=16MB)
                       (malloc=12MB #178923)
                       (arena=4MB #1)

-    Native Memory Tracking (reserved=5MB, committed=5MB)

-        Shared class space (reserved=12MB, committed=12MB)

-                     Arena Chunk (reserved=1MB, committed=1MB)
```

### 8.3 Diagnosing Native Memory Issues

```bash
# High "Thread" memory? Too many threads
# Fix: reduce thread count or stack size
-Xss512k

# High "Class" memory? Too many loaded classes  
# Check: jcmd <pid> GC.class_stats | wc -l

# High "Code" memory? JIT compiled too much code
# Check: code cache is filling up
-XX:ReservedCodeCacheSize=128m  # Reduce if not needed

# High "Internal"? Direct buffers or JNI allocations
# Check: direct buffer monitoring
```

## 9. Memory Pressure Scenarios

### 9.1 OutOfMemoryError Types

| Error | Cause | Fix |
|-------|-------|-----|
| `Java heap space` | Heap full, can't allocate | Increase -Xmx, fix leak |
| `Metaspace` | Too many classes loaded | Increase MaxMetaspaceSize, fix CL leak |
| `GC overhead limit exceeded` | >98% time in GC, <2% reclaimed | Fix leak or increase heap |
| `Direct buffer memory` | Off-heap buffers exhausted | Increase MaxDirectMemorySize |
| `unable to create native thread` | OS thread/memory limit | Reduce threads, increase ulimit |
| `Compressed class space` | Compressed class space full | Increase CompressedClassSpaceSize |
| `Map failed` | mmap failed (native memory) | Check OS limits, RSS vs cgroup |

### 9.2 Handling OOM Gracefully

```bash
# Auto-dump + auto-restart
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/java/heap-%t.hprof
-XX:OnOutOfMemoryError="kill -9 %p"    # Force kill for container restart
-XX:+ExitOnOutOfMemoryError            # Or just exit (Java 8u92+)
```

```java
// Detect memory pressure programmatically
public class MemoryWatcher {
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    public static double getHeapUsagePercent() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return (double) heap.getUsed() / heap.getMax() * 100;
    }
    
    // Register for low memory notification
    public static void registerLowMemoryAlert(double threshold) {
        MemoryPoolMXBean oldGen = ManagementFactory.getMemoryPoolMXBeans().stream()
            .filter(p -> p.getType() == MemoryType.HEAP && p.getName().contains("Old"))
            .findFirst().orElseThrow();
        
        long thresholdBytes = (long) (oldGen.getUsage().getMax() * threshold);
        oldGen.setUsageThreshold(thresholdBytes);
        
        NotificationEmitter emitter = (NotificationEmitter) memoryBean;
        emitter.addNotificationListener((notification, handback) -> {
            if (notification.getType().equals(
                    MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                // Shed load, reject requests, trigger GC, alert ops
                System.err.println("CRITICAL: Heap usage exceeded " + threshold * 100 + "%");
            }
        }, null, null);
    }
}
```

## 10. Memory Tuning Checklist

```
□ Set Xms = Xmx (avoid resize)
□ Enable HeapDumpOnOutOfMemoryError
□ Set MaxMetaspaceSize (prevent unbounded growth)
□ Calculate total memory: Heap + Meta + Threads + 20% overhead
□ For containers: MaxRAMPercentage=70-75%
□ Enable NativeMemoryTracking=summary in staging
□ Monitor: heap after GC, promotion rate, allocation rate
□ Validate: Old Gen after Full GC < 60% capacity
□ Review: String deduplication if many duplicate strings
□ Check: Direct memory if using Netty/NIO heavily
□ Reduce Xss if thread count > 500
□ Profile allocation hotspots with JFR/async-profiler
```
