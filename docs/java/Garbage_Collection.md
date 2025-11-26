# Java Garbage Collection - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Memory Structure](#memory-structure)
3. [GC Algorithms](#gc-algorithms)
4. [Theory & Concepts](#theory--concepts)
5. [Tuning Parameters](#tuning-parameters)
6. [Best Practices](#best-practices)
7. [Monitoring & Tools](#monitoring--tools)

---

## Introduction

### What is Garbage Collection?

Garbage Collection (GC) is an **automatic memory management** process in Java that:
- Identifies objects no longer reachable by the application
- Reclaims memory occupied by unreachable objects
- Prevents memory leaks and fragmentation

### Why Garbage Collection?

**Benefits:**
- ✅ Automatic memory management (no manual free/delete)
- ✅ Prevents memory leaks
- ✅ Eliminates dangling pointers
- ✅ Reduces programming errors
- ✅ Improves developer productivity

**Trade-offs:**
- ❌ Stop-the-world pauses
- ❌ CPU overhead
- ❌ Non-deterministic timing
- ❌ Potential latency spikes

---

## Memory Structure

### JVM Heap Layout

```
┌─────────────────────────────────────────────────────────┐
│                      JVM HEAP                            │
├──────────────────────────────┬──────────────────────────┤
│     Young Generation         │    Old Generation        │
│  (Minor GC - Frequent)       │  (Major GC - Infrequent) │
├─────────┬──────────┬─────────┤                          │
│  Eden   │ Survivor │Survivor │      Tenured Space       │
│  Space  │  Space   │ Space   │                          │
│  (S0)   │   (S1)   │  (S2)   │                          │
└─────────┴──────────┴─────────┴──────────────────────────┘
         ↓                              ↓
    New objects              Long-lived objects
    allocated here           promoted here
```

### Memory Regions

#### 1. Young Generation (1/3 of heap)

**Eden Space (80%)**
- All new objects allocated here
- Fast allocation using bump-the-pointer
- Fills up quickly

**Survivor Spaces S0 & S1 (10% each)**
- Objects that survive one GC cycle
- Only one survivor space active at a time
- Objects copied between S0 ↔ S1

**Minor GC Process:**
```
1. Eden full → Trigger Minor GC
2. Mark live objects in Eden + Active Survivor
3. Copy live objects to Inactive Survivor
4. Clear Eden + Active Survivor
5. Swap Survivor spaces
6. Promote old objects to Old Gen (age > threshold)
```

#### 2. Old Generation (2/3 of heap)

**Tenured Space**
- Objects promoted from Young Gen
- Objects that survived multiple GC cycles (default: 15)
- Large objects allocated directly here
- Less frequent, more expensive collections

**Major GC (Full GC):**
- Triggered when Old Gen fills up
- Stop-the-world pause
- Longer duration than Minor GC

#### 3. Metaspace (Java 8+)

**Replaces PermGen**
- Class metadata, method data
- Native memory (not part of heap)
- Auto-expands (no fixed size)
- Garbage collected when classes unloaded

---

## GC Algorithms

### 1. Serial GC
**Flag:** `-XX:+UseSerialGC`

**Characteristics:**
- Single-threaded collector
- Stop-the-world for both Minor and Major GC
- Uses Mark-Sweep-Compact for Old Gen

**Use Case:**
- Small applications (<100MB heap)
- Single-core machines
- Client-side applications

**Pros:**
- Simple implementation
- Low memory overhead
- Predictable behavior

**Cons:**
- Long pause times
- Not suitable for multi-core systems

**Example:**
```bash
java -Xms512m -Xmx512m -XX:+UseSerialGC -jar app.jar
```

---

### 2. Parallel GC (Throughput Collector)
**Flag:** `-XX:+UseParallelGC`

**Characteristics:**
- Multi-threaded for Young Gen
- Multi-threaded for Old Gen (Parallel Old)
- Stop-the-world for both generations
- Default in Java 8

**Use Case:**
- Batch processing
- Scientific computing
- High throughput requirements
- Multi-core systems

**Pros:**
- High throughput
- Efficient CPU utilization
- Good for batch jobs

**Cons:**
- Longer pause times
- Not suitable for latency-sensitive apps

**Tuning:**
```bash
java -Xms4g -Xmx4g \
  -XX:+UseParallelGC \
  -XX:ParallelGCThreads=8 \
  -XX:MaxGCPauseMillis=100 \
  -jar app.jar
```

---

### 3. CMS - Concurrent Mark Sweep
**Flag:** `-XX:+UseConcMarkSweepGC` (Deprecated in Java 9, Removed in Java 14)

**Characteristics:**
- Low-latency collector
- Runs concurrently with application threads
- Does NOT compact (fragmentation issue)
- Uses more CPU

**Phases:**
1. **Initial Mark (STW)**: Mark GC roots
2. **Concurrent Mark**: Traverse object graph
3. **Concurrent Preclean**: Handle references changed during marking
4. **Remark (STW)**: Final marking
5. **Concurrent Sweep**: Reclaim dead objects
6. **Concurrent Reset**: Prepare for next cycle

**Use Case:**
- Web applications
- Low-latency requirements
- Large heaps (4-8GB)

**Pros:**
- Short pause times
- Concurrent collection
- Good for interactive apps

**Cons:**
- CPU overhead (concurrent threads)
- Fragmentation (no compaction)
- Fallback to Full GC if fragmented
- Deprecated

**Tuning:**
```bash
java -Xms4g -Xmx4g \
  -XX:+UseConcMarkSweepGC \
  -XX:CMSInitiatingOccupancyFraction=70 \
  -XX:+UseCMSInitiatingOccupancyOnly \
  -jar app.jar
```

---

### 4. G1 GC - Garbage First
**Flag:** `-XX:+UseG1GC` (Default since Java 9)

**Characteristics:**
- Region-based heap layout (~2048 regions)
- Predictable pause times
- Concurrent marking + parallel evacuation
- Compacts during collection

**Heap Layout:**
```
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ E  │ S  │ O  │ O  │ H  │ E  │ S  │
└────┴────┴────┴────┴────┴────┴────┴────┘
 Eden  Eden Surv Old  Old  Huge Eden Surv

E = Eden, S = Survivor, O = Old, H = Humongous
```

**Collection Phases:**
1. **Young-only phase**: Minor GCs
2. **Concurrent marking**: Mark live objects
3. **Space-reclamation phase**: Mixed GCs (Young + Old)

**Use Case:**
- Large heaps (>4GB)
- Balanced throughput and latency
- General-purpose applications
- Default choice for most apps

**Pros:**
- Predictable pause times
- Handles large heaps well
- Compacts during collection
- Good balance

**Cons:**
- More complex than Parallel GC
- Higher memory overhead
- Not lowest latency

**Tuning:**
```bash
java -Xms8g -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=16m \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -jar app.jar
```

---

### 5. ZGC - Z Garbage Collector
**Flag:** `-XX:+UseZGC` (Production-ready since Java 15)

**Characteristics:**
- Ultra-low latency (<10ms pauses)
- Scalable (8MB - 16TB heaps)
- Concurrent compaction
- Colored pointers (64-bit only)

**Key Features:**
- Pause times independent of heap size
- Pause times independent of live-set size
- Load barriers for concurrent operations

**Use Case:**
- Ultra-low latency requirements (<10ms)
- Very large heaps (>100GB)
- Trading systems
- Real-time applications

**Pros:**
- Sub-10ms pause times
- Scales to huge heaps
- Concurrent compaction
- Predictable latency

**Cons:**
- Higher memory overhead (10-15%)
- Requires 64-bit JVM
- More CPU usage

**Tuning:**
```bash
java -Xms16g -Xmx16g \
  -XX:+UseZGC \
  -XX:ZCollectionInterval=5 \
  -XX:ZAllocationSpikeTolerance=2 \
  -jar app.jar
```

---

### 6. Shenandoah GC
**Flag:** `-XX:+UseShenandoahGC` (OpenJDK only)

**Characteristics:**
- Low-latency collector
- Concurrent compaction
- Region-based like G1
- Brooks pointers for forwarding

**Use Case:**
- Low-latency requirements
- Large heaps
- Alternative to ZGC

**Pros:**
- Consistent low pauses
- Concurrent compaction
- Works on 32-bit and 64-bit

**Cons:**
- Higher CPU usage
- Not in Oracle JDK
- More complex

**Tuning:**
```bash
java -Xms8g -Xmx8g \
  -XX:+UseShenandoahGC \
  -XX:ShenandoahGCHeuristics=adaptive \
  -jar app.jar
```

---

## Theory & Concepts

### 1. Object Reachability

An object is **reachable** if accessible through:

```
GC Roots:
├── Local variables in active stack frames
├── Static fields of loaded classes
├── JNI references
├── Active threads
└── Synchronized monitors
```

**Reachability States:**
- **Strongly Reachable**: Direct reference from GC root
- **Softly Reachable**: Only through SoftReference (cleared before OOM)
- **Weakly Reachable**: Only through WeakReference (cleared eagerly)
- **Phantom Reachable**: Only through PhantomReference (post-finalization)
- **Unreachable**: No references, eligible for GC

**Example:**
```java
// Strongly reachable
Object obj = new Object();

// Weakly reachable
WeakReference<Object> weakRef = new WeakReference<>(obj);
obj = null; // Now only weakly reachable

// Unreachable
weakRef.clear(); // Now unreachable
```

---

### 2. Generational Hypothesis

**Weak Generational Hypothesis:**
> Most objects die young

**Statistics:**
- 90-98% of objects become unreachable shortly after allocation
- Surviving objects tend to live much longer

**Implications:**
- Separate Young and Old generations
- Frequent, fast Minor GCs
- Infrequent, slower Major GCs
- Optimize for short-lived objects

---

### 3. Stop-The-World (STW)

**Definition:** All application threads pause during GC

**Why STW?**
- Ensures consistent heap state
- Prevents objects from moving during collection
- Simplifies GC implementation

**STW Phases:**
```
Application Running → STW Pause → GC Work → Resume Application
```

**Minimizing STW:**
- Concurrent collectors (CMS, G1, ZGC)
- Smaller heap regions
- Incremental collection
- Parallel GC threads

---

### 4. Mark and Sweep

**Mark Phase:**
1. Start from GC roots
2. Traverse object graph
3. Mark all reachable objects

**Sweep Phase:**
1. Scan entire heap
2. Reclaim unmarked objects
3. Add memory to free list

**Compact Phase (optional):**
1. Move live objects together
2. Eliminate fragmentation
3. Update references

**Tri-color Marking:**
- **White**: Not visited (dead)
- **Gray**: Visited, children not scanned
- **Black**: Visited, children scanned (live)

---

### 5. Card Table

**Problem:** Old Gen → Young Gen references

**Solution:** Card Table
- Divides Old Gen into 512-byte cards
- Marks cards with Old→Young references
- Scan only marked cards during Minor GC

```
Old Generation:
┌────┬────┬────┬────┬────┬────┐
│ C0 │ C1 │ C2 │ C3 │ C4 │ C5 │
└────┴────┴────┴────┴────┴────┘
  ✓    ✗    ✓    ✗    ✗    ✓
  
Card Table: [1, 0, 1, 0, 0, 1]
Only scan cards C0, C2, C5
```

---

### 6. Write Barriers

**Purpose:** Track reference updates during concurrent GC

**Types:**
- **Pre-write barrier**: Before reference update
- **Post-write barrier**: After reference update

**Example (G1 GC):**
```java
// Application code
obj.field = newValue;

// With write barrier
if (obj.field != newValue) {
    preWriteBarrier(obj, obj.field);
    obj.field = newValue;
    postWriteBarrier(obj, newValue);
}
```

---

### 7. Safe Points

**Definition:** Points where JVM can safely pause threads for GC

**Safe Point Locations:**
- Method returns
- Loop back-edges
- Before method calls

**Safepoint Synchronization:**
```
Thread 1: Running → Poll → Safepoint → Wait
Thread 2: Running → Poll → Safepoint → Wait
Thread 3: Running → Poll → Safepoint → Wait
All threads at safepoint → GC starts
```

---

## Tuning Parameters

### Heap Size

```bash
# Initial and maximum heap size
-Xms4g -Xmx4g

# Young generation size
-Xmn1g                          # Fixed size
-XX:NewRatio=2                  # Old:Young = 2:1
-XX:NewSize=512m                # Initial Young size
-XX:MaxNewSize=1g               # Max Young size

# Survivor space ratio
-XX:SurvivorRatio=8             # Eden:Survivor = 8:1
```

### GC Selection

```bash
# Serial GC
-XX:+UseSerialGC

# Parallel GC
-XX:+UseParallelGC
-XX:ParallelGCThreads=8

# G1 GC
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# ZGC
-XX:+UseZGC
-XX:ZCollectionInterval=5

# Shenandoah
-XX:+UseShenandoahGC
```

### Performance Tuning

```bash
# GC threads
-XX:ParallelGCThreads=8         # Parallel GC threads
-XX:ConcGCThreads=2             # Concurrent GC threads

# Promotion
-XX:MaxTenuringThreshold=15     # Age before promotion
-XX:TargetSurvivorRatio=90      # Survivor space target

# Large objects
-XX:PretenureSizeThreshold=1m   # Direct to Old Gen

# GC overhead
-XX:GCTimeRatio=99              # 1% GC time target
```

### GC Logging

```bash
# Java 9+
-Xlog:gc*:file=gc.log:time,uptime,level,tags

# Java 8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

---

## Best Practices

### 1. Object Creation

**Minimize Allocations:**
```java
// Bad: Creates new object every iteration
for (int i = 0; i < 1000000; i++) {
    String s = new String("value");
    process(s);
}

// Good: Reuse object
String s = "value";
for (int i = 0; i < 1000000; i++) {
    process(s);
}
```

### 2. Object Pooling

**For Expensive Objects:**
```java
// Connection pool
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
HikariDataSource ds = new HikariDataSource(config);

// Thread pool
ExecutorService executor = Executors.newFixedThreadPool(10);
```

### 3. Avoid Finalizers

**Bad:**
```java
class Resource {
    @Override
    protected void finalize() throws Throwable {
        // Unpredictable timing
        cleanup();
    }
}
```

**Good:**
```java
class Resource implements AutoCloseable {
    @Override
    public void close() {
        cleanup();
    }
}

try (Resource r = new Resource()) {
    // Use resource
} // Guaranteed cleanup
```

### 4. Use Appropriate References

```java
// Strong reference (default)
Object obj = new Object();

// Soft reference (memory-sensitive cache)
SoftReference<byte[]> cache = new SoftReference<>(largeData);

// Weak reference (canonical mapping)
WeakHashMap<Key, Value> map = new WeakHashMap<>();

// Phantom reference (post-finalization cleanup)
PhantomReference<Object> ref = new PhantomReference<>(obj, queue);
```

### 5. Right-size Heap

```bash
# Too small: Frequent GCs, OOM
-Xms512m -Xmx512m

# Too large: Long GC pauses
-Xms64g -Xmx64g

# Right-sized: Balance
-Xms4g -Xmx4g
```

### 6. Monitor and Tune

**Iterative Process:**
1. Start with defaults
2. Monitor GC logs
3. Identify bottlenecks
4. Tune parameters
5. Measure impact
6. Repeat

---

## Monitoring & Tools

### 1. JVM Flags

```bash
# Print GC details
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCApplicationStoppedTime

# GC log rotation
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M
```

### 2. Programmatic Monitoring

```java
import java.lang.management.*;

public class GCMonitor {
    public static void main(String[] args) {
        // Memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        System.out.println("Heap Used: " + heapUsage.getUsed() / (1024*1024) + " MB");
        System.out.println("Heap Max: " + heapUsage.getMax() / (1024*1024) + " MB");
        
        // GC statistics
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("GC Name: " + gcBean.getName());
            System.out.println("GC Count: " + gcBean.getCollectionCount());
            System.out.println("GC Time: " + gcBean.getCollectionTime() + " ms");
        }
    }
}
```

### 3. JConsole

```bash
# Start application with JMX
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar app.jar

# Connect JConsole
jconsole localhost:9010
```

### 4. JVisualVM

```bash
# Start JVisualVM
jvisualvm

# Features:
# - Real-time heap monitoring
# - GC activity visualization
# - Thread dumps
# - Heap dumps
# - Profiling
```

### 5. GC Log Analysis

**GCViewer:**
```bash
java -jar gcviewer.jar gc.log
```

**GCEasy:**
- Upload gc.log to https://gceasy.io
- Get detailed analysis and recommendations

### 6. Production Monitoring

**Micrometer + Prometheus:**
```java
@Configuration
public class MetricsConfig {
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}

// Metrics exposed at /actuator/prometheus
// - jvm.memory.used
// - jvm.memory.max
// - jvm.gc.pause
// - jvm.gc.memory.allocated
```

---

## GC Selection Guide

| Workload | Heap Size | Recommended GC | Reason |
|----------|-----------|----------------|---------|
| Batch processing | Any | Parallel GC | High throughput |
| Web applications | <4GB | G1 GC | Balanced |
| Web applications | >4GB | G1 GC or ZGC | Predictable pauses |
| Low-latency trading | >8GB | ZGC | <10ms pauses |
| Microservices | <2GB | G1 GC | Good default |
| Big data processing | >32GB | ZGC | Scalability |
| Desktop applications | <512MB | Serial GC | Low overhead |

---

## Common GC Issues

### 1. OutOfMemoryError

**Causes:**
- Heap too small
- Memory leak
- Too many live objects

**Solutions:**
```bash
# Increase heap
-Xmx8g

# Heap dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# Analyze with Eclipse MAT
```

### 2. Long GC Pauses

**Causes:**
- Large heap
- Wrong GC algorithm
- Fragmentation

**Solutions:**
```bash
# Use low-latency GC
-XX:+UseZGC

# Reduce heap size
-Xmx4g

# Tune pause time
-XX:MaxGCPauseMillis=100
```

### 3. High GC Overhead

**Causes:**
- Frequent GCs
- Heap too small
- Too many short-lived objects

**Solutions:**
```bash
# Increase Young Gen
-Xmn2g

# Increase heap
-Xmx8g

# Optimize code (reduce allocations)
```

---

## Summary

### Key Takeaways

1. **Start with defaults**: Modern GCs (G1, ZGC) work well out-of-the-box
2. **Monitor first**: Understand your GC behavior before tuning
3. **Right-size heap**: Not too small (frequent GCs), not too large (long pauses)
4. **Choose appropriate GC**: Match GC to workload requirements
5. **Minimize allocations**: Best GC is no GC
6. **Use appropriate references**: Soft/Weak for caches
7. **Avoid finalizers**: Use try-with-resources
8. **Test under load**: Production-like conditions

### Quick Reference

```bash
# General-purpose (default)
java -Xms4g -Xmx4g -XX:+UseG1GC -jar app.jar

# High throughput
java -Xms4g -Xmx4g -XX:+UseParallelGC -jar app.jar

# Low latency
java -Xms8g -Xmx8g -XX:+UseZGC -jar app.jar

# With monitoring
java -Xms4g -Xmx4g -XX:+UseG1GC \
  -Xlog:gc*:file=gc.log:time,uptime,level,tags \
  -XX:+HeapDumpOnOutOfMemoryError \
  -jar app.jar
```

---

## References

- [Oracle JVM GC Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [G1 GC Documentation](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc)
- [Shenandoah GC](https://wiki.openjdk.org/display/shenandoah)
- [Java Performance: The Definitive Guide](https://www.oreilly.com/library/view/java-performance-the/9781449363512/)

---

**Last Updated**: 2024
**Java Versions**: 8, 11, 17, 21
