# Java Garbage Collection - Deep Dive

## Overview

Garbage Collection (GC) is an automatic memory management process in Java that identifies and removes objects that are no longer referenced, freeing up memory for new objects.

**Key Benefits**:
- Automatic memory management
- Prevents memory leaks
- Eliminates manual memory deallocation
- Reduces programming errors (dangling pointers, double free)

---

## Memory Structure in JVM

### Heap Memory (GC Managed)

```
┌─────────────────────────────────────────────────┐
│                  Java Heap                      │
├─────────────────────────────────────────────────┤
│  Young Generation          │  Old Generation    │
│  ┌──────┬────────────────┐ │                   │
│  │ Eden │ Survivor 0 (S0)│ │  Tenured Space    │
│  │      │ Survivor 1 (S1)│ │                   │
│  └──────┴────────────────┘ │                   │
└─────────────────────────────────────────────────┘
```

**Young Generation** (1/3 of heap):
- **Eden Space**: New objects created here
- **Survivor Space (S0, S1)**: Objects that survive minor GC

**Old Generation** (2/3 of heap):
- **Tenured Space**: Long-lived objects promoted from Young Gen

### Non-Heap Memory (Not GC Managed)

```
┌─────────────────────────────────────────────────┐
│  Metaspace (Java 8+) / PermGen (Java 7)        │
│  - Class metadata                               │
│  - Method metadata                              │
│  - Static variables                             │
└─────────────────────────────────────────────────┘
```

---

## How Garbage Collection Works

### 1. Object Creation

```java
public class GCExample {
    public static void main(String[] args) {
        Person p1 = new Person("Alice"); // Created in Eden
        Person p2 = new Person("Bob");   // Created in Eden
        Person p3 = new Person("Charlie"); // Created in Eden
    }
}
```

**Memory State**:
```
Eden Space:
[Person("Alice")] [Person("Bob")] [Person("Charlie")]
```

---

### 2. Reachability Analysis

**Root Objects** (GC Roots):
- Local variables in active methods
- Static variables
- Active threads
- JNI references

**Reachable vs Unreachable**:
```java
public class ReachabilityExample {
    static Person staticPerson = new Person("Static"); // GC Root
    
    public void method() {
        Person local = new Person("Local"); // GC Root (while method active)
        Person temp = new Person("Temp");
        temp = null; // Now unreachable → Eligible for GC
    }
}
```

**Reachability Graph**:
```
GC Roots → Reachable Objects → Reachable Objects
           ↓
        Unreachable Objects (Garbage)
```

---

### 3. Mark and Sweep Algorithm

**Phase 1: Mark**
```
1. Start from GC roots
2. Mark all reachable objects
3. Unmarked objects = garbage
```

**Phase 2: Sweep**
```
1. Scan heap
2. Remove unmarked objects
3. Free memory
```

**Example**:
```
Before GC:
[A*] [B ] [C*] [D ] [E*]  (* = reachable)

After Mark:
[A✓] [B ] [C✓] [D ] [E✓]  (✓ = marked)

After Sweep:
[A✓] [  ] [C✓] [  ] [E✓]  (unmarked removed)
```

---

## Types of Garbage Collection

### 1. Minor GC (Young Generation)

**Trigger**: Eden space is full

**Process**:
```
Step 1: Eden full
Eden: [Obj1] [Obj2] [Obj3] [Obj4] [Obj5]
S0:   []
S1:   []

Step 2: Minor GC runs
- Mark reachable objects in Eden
- Copy survivors to S0
- Clear Eden

Eden: []
S0:   [Obj1] [Obj3] [Obj5]  (age=1)
S1:   []

Step 3: Next Minor GC
Eden: [Obj6] [Obj7] [Obj8]
S0:   [Obj1] [Obj3] [Obj5]  (age=1)

After GC:
Eden: []
S0:   []
S1:   [Obj1] [Obj3] [Obj6]  (age=2, 2, 1)

Step 4: After multiple GCs (age > threshold)
Promote to Old Generation
```

**Characteristics**:
- Fast (milliseconds)
- Frequent
- Stop-the-world (STW) event
- Uses copying algorithm

---

### 2. Major GC / Full GC (Old Generation)

**Trigger**: Old generation is full

**Process**:
```
1. Mark all reachable objects in Old Gen
2. Sweep unreachable objects
3. Compact memory (optional)
```

**Characteristics**:
- Slow (seconds)
- Infrequent
- Stop-the-world event
- Uses mark-sweep-compact algorithm

---

### 3. Full GC (Entire Heap)

**Trigger**: 
- Old generation full
- Metaspace full
- System.gc() called
- Heap fragmentation

**Impact**:
```
Application paused: 2-10 seconds
All threads stopped
Complete heap cleanup
```

---

## Garbage Collection Algorithms

### 1. Serial GC

**Use Case**: Single-threaded, small applications

```bash
java -XX:+UseSerialGC -jar app.jar
```

**How it works**:
```
Single GC thread
Stop-the-world for both Minor and Major GC
Simple mark-sweep-compact
```

**Pros**: Simple, low memory overhead  
**Cons**: Long pause times

---

### 2. Parallel GC (Throughput Collector)

**Use Case**: Multi-core systems, batch processing

```bash
java -XX:+UseParallelGC -jar app.jar
```

**How it works**:
```
Multiple GC threads (parallel)
Stop-the-world for both Minor and Major GC
Optimized for throughput
```

**Example**:
```
4 CPU cores → 4 GC threads
Minor GC: 100ms → 25ms (4x faster)
```

**Pros**: High throughput, fast GC  
**Cons**: Long pause times

---

### 3. CMS (Concurrent Mark Sweep)

**Use Case**: Low-latency applications

```bash
java -XX:+UseConcMarkSweepGC -jar app.jar
```

**How it works**:
```
Phase 1: Initial Mark (STW) - 10ms
Phase 2: Concurrent Mark (no STW) - 1000ms
Phase 3: Remark (STW) - 50ms
Phase 4: Concurrent Sweep (no STW) - 500ms
```

**Pros**: Low pause times  
**Cons**: CPU overhead, fragmentation, deprecated in Java 14

---

### 4. G1 GC (Garbage First) - Default in Java 9+

**Use Case**: Large heaps (>4GB), balanced latency/throughput

```bash
java -XX:+UseG1GC -jar app.jar
```

**How it works**:
```
Heap divided into regions (1-32MB each)

┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ E  │ S  │ O  │ O  │ E  │ H  │ O  │
└────┴────┴────┴────┴────┴────┴────┴────┘
E = Eden, S = Survivor, O = Old, H = Humongous

Young GC: Collect Eden + Survivor regions
Mixed GC: Collect Young + some Old regions
```

**Phases**:
```
1. Young GC (STW) - 10-50ms
2. Concurrent Marking
3. Mixed GC (STW) - 50-200ms
```

**Pros**: Predictable pause times, handles large heaps  
**Cons**: More CPU overhead

---

### 5. ZGC (Z Garbage Collector) - Java 11+

**Use Case**: Ultra-low latency (<10ms), very large heaps (TB)

```bash
java -XX:+UseZGC -jar app.jar
```

**How it works**:
```
Concurrent GC (most work done concurrently)
Pause times: <10ms regardless of heap size
Uses colored pointers and load barriers
```

**Pros**: Ultra-low latency, scalable  
**Cons**: Higher memory overhead

---

### 6. Shenandoah GC - Java 12+

**Use Case**: Low latency, large heaps

```bash
java -XX:+UseShenandoahGC -jar app.jar
```

**How it works**:
```
Concurrent evacuation
Pause times: <10ms
Similar to ZGC but different implementation
```

---

## GC Tuning Parameters

### Heap Size

```bash
# Initial heap size
-Xms2g

# Maximum heap size
-Xmx4g

# Young generation size
-Xmn1g

# Example
java -Xms2g -Xmx4g -Xmn1g -jar app.jar
```

### GC Selection

```bash
# Serial GC
-XX:+UseSerialGC

# Parallel GC
-XX:+UseParallelGC

# CMS GC
-XX:+UseConcMarkSweepGC

# G1 GC (default Java 9+)
-XX:+UseG1GC

# ZGC
-XX:+UseZGC

# Shenandoah GC
-XX:+UseShenandoahGC
```

### G1 GC Tuning

```bash
# Target pause time (default 200ms)
-XX:MaxGCPauseMillis=100

# GC threads
-XX:ParallelGCThreads=8

# Concurrent GC threads
-XX:ConcGCThreads=2

# Region size (1-32MB)
-XX:G1HeapRegionSize=16m

# Example
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:ParallelGCThreads=8 \
     -Xms4g -Xmx4g \
     -jar app.jar
```

### GC Logging

```bash
# Java 8
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-Xloggc:gc.log

# Java 9+
-Xlog:gc*:file=gc.log:time,uptime,level,tags

# Example
java -Xlog:gc*:file=gc.log:time,uptime,level,tags \
     -XX:+UseG1GC \
     -jar app.jar
```

---

## GC Monitoring

### 1. JVisualVM

```bash
jvisualvm
```

**Features**:
- Real-time heap usage
- GC activity
- Thread monitoring
- CPU profiling

---

### 2. JConsole

```bash
jconsole
```

**Features**:
- Memory usage
- Thread count
- GC statistics

---

### 3. GC Logs Analysis

**Sample GC Log**:
```
[0.123s][info][gc] GC(0) Pause Young (Normal) 25M->5M(128M) 10.234ms
[1.456s][info][gc] GC(1) Pause Young (Normal) 30M->8M(128M) 12.567ms
[5.789s][info][gc] GC(2) Pause Full (Allocation Failure) 120M->50M(128M) 2345.678ms
```

**Analysis**:
- GC(0), GC(1): Minor GC (fast, 10-12ms)
- GC(2): Full GC (slow, 2345ms) ← Problem!

---

### 4. Programmatic Monitoring

```java
import java.lang.management.*;

public class GCMonitor {
    public static void main(String[] args) {
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("GC Name: " + gcBean.getName());
            System.out.println("Collection Count: " + gcBean.getCollectionCount());
            System.out.println("Collection Time: " + gcBean.getCollectionTime() + "ms");
        }
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        System.out.println("Heap Used: " + heapUsage.getUsed() / 1024 / 1024 + "MB");
        System.out.println("Heap Max: " + heapUsage.getMax() / 1024 / 1024 + "MB");
    }
}
```

---

## Common GC Problems

### 1. Memory Leak

**Symptom**: Heap usage keeps growing, frequent Full GC

```java
// Memory leak example
public class MemoryLeak {
    private static List<byte[]> list = new ArrayList<>();
    
    public void leak() {
        while (true) {
            list.add(new byte[1024 * 1024]); // 1MB
            // Never removed → Memory leak
        }
    }
}
```

**Solution**: Remove unused references

```java
public void fixed() {
    List<byte[]> list = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        list.add(new byte[1024 * 1024]);
    }
    list.clear(); // Clear when done
}
```

---

### 2. OutOfMemoryError

**Types**:

**a) Java heap space**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution**: Increase heap size or fix memory leak
```bash
java -Xmx4g -jar app.jar
```

**b) GC overhead limit exceeded**
```
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Meaning**: >98% time in GC, <2% heap recovered

**Solution**: Increase heap or optimize code

**c) Metaspace**
```
java.lang.OutOfMemoryError: Metaspace
```

**Solution**: Increase metaspace
```bash
java -XX:MaxMetaspaceSize=512m -jar app.jar
```

---

### 3. Long GC Pauses

**Symptom**: Application freezes for seconds

**Causes**:
- Large heap size
- Full GC triggered
- Wrong GC algorithm

**Solutions**:
```bash
# Use G1 GC with pause time goal
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -Xms4g -Xmx4g \
     -jar app.jar

# Or use ZGC for ultra-low latency
java -XX:+UseZGC \
     -Xms4g -Xmx4g \
     -jar app.jar
```

---

### 4. High GC Frequency

**Symptom**: Minor GC every few seconds

**Cause**: Young generation too small

**Solution**: Increase young generation size
```bash
java -Xmn2g -Xms8g -Xmx8g -jar app.jar
```

---

## Best Practices

### 1. Choose Right GC Algorithm

```
Small heap (<2GB): Serial GC or Parallel GC
Medium heap (2-4GB): G1 GC
Large heap (>4GB): G1 GC or ZGC
Ultra-low latency: ZGC or Shenandoah
```

### 2. Set Appropriate Heap Size

```bash
# Rule of thumb: Xms = Xmx (avoid resizing)
java -Xms4g -Xmx4g -jar app.jar

# Young gen = 1/3 to 1/2 of heap
java -Xms6g -Xmx6g -Xmn2g -jar app.jar
```

### 3. Monitor GC Activity

```bash
# Enable GC logging
java -Xlog:gc*:file=gc.log:time,uptime,level,tags \
     -jar app.jar

# Analyze logs regularly
```

### 4. Avoid Explicit GC Calls

```java
// Bad
System.gc(); // Triggers Full GC

// Good
// Let JVM decide when to GC
```

### 5. Minimize Object Creation

```java
// Bad: Creates many temporary objects
public String concatenate(String[] strings) {
    String result = "";
    for (String s : strings) {
        result += s; // Creates new String each time
    }
    return result;
}

// Good: Reuses StringBuilder
public String concatenate(String[] strings) {
    StringBuilder sb = new StringBuilder();
    for (String s : strings) {
        sb.append(s);
    }
    return sb.toString();
}
```

### 6. Use Object Pools for Expensive Objects

```java
// Object pool for database connections
public class ConnectionPool {
    private Queue<Connection> pool = new ConcurrentLinkedQueue<>();
    
    public Connection getConnection() {
        Connection conn = pool.poll();
        if (conn == null) {
            conn = createNewConnection();
        }
        return conn;
    }
    
    public void returnConnection(Connection conn) {
        pool.offer(conn);
    }
}
```

### 7. Use Weak References for Caches

```java
// Cache with weak references (GC can collect)
Map<String, WeakReference<Data>> cache = new WeakHashMap<>();

cache.put("key", new WeakReference<>(data));

// If memory low, GC can collect data
```

---

## GC Algorithm Comparison

| Algorithm | Pause Time | Throughput | Heap Size | Use Case |
|-----------|-----------|------------|-----------|----------|
| **Serial** | High | Low | Small | Single-core, dev |
| **Parallel** | High | High | Medium | Batch processing |
| **CMS** | Low | Medium | Medium | Low-latency apps |
| **G1** | Medium | High | Large | General purpose |
| **ZGC** | Ultra-low | High | Very large | Ultra-low latency |
| **Shenandoah** | Ultra-low | High | Very large | Ultra-low latency |

---

## Interview Questions & Answers

### Q1: What is Garbage Collection?

**Answer**: Automatic memory management process that identifies and removes unreachable objects from heap memory, freeing space for new objects.

### Q2: How does JVM identify garbage?

**Answer**: Using reachability analysis from GC roots (local variables, static variables, active threads). Objects not reachable from roots are garbage.

### Q3: What's the difference between Minor GC and Major GC?

**Answer**:
- **Minor GC**: Cleans Young Generation, fast (ms), frequent
- **Major GC**: Cleans Old Generation, slow (seconds), infrequent

### Q4: What is Stop-the-World event?

**Answer**: GC pauses all application threads to perform garbage collection. Duration varies by GC algorithm (Serial: seconds, ZGC: <10ms).

### Q5: Which GC algorithm is best?

**Answer**: Depends on requirements:
- **G1 GC**: General purpose, default in Java 9+
- **ZGC**: Ultra-low latency (<10ms)
- **Parallel GC**: High throughput, batch processing

### Q6: How to reduce GC pause time?

**Answer**:
- Use G1 GC or ZGC
- Set MaxGCPauseMillis target
- Increase heap size
- Optimize code to create fewer objects

### Q7: What causes OutOfMemoryError?

**Answer**:
- Memory leak (objects not released)
- Heap size too small
- Too many objects created
- Metaspace full (too many classes)

### Q8: How to detect memory leaks?

**Answer**:
- Monitor heap usage over time
- Analyze heap dumps (jmap, VisualVM)
- Check for growing collections
- Use profilers (YourKit, JProfiler)

---

## Key Takeaways

1. **GC is automatic** memory management in Java
2. **Heap divided** into Young Gen (Eden, Survivor) and Old Gen
3. **Minor GC** cleans Young Gen (fast), **Major GC** cleans Old Gen (slow)
4. **G1 GC** is default in Java 9+, good for most use cases
5. **ZGC** for ultra-low latency (<10ms pause)
6. **Monitor GC** activity with logs and tools
7. **Tune heap size** based on application needs
8. **Avoid explicit** System.gc() calls
9. **Minimize object creation** to reduce GC pressure
10. **Choose GC algorithm** based on latency vs throughput requirements

---

## Practice Problems

1. Analyze GC logs to identify performance issues
2. Tune G1 GC for 100ms pause time target
3. Detect and fix memory leak in application
4. Compare Parallel GC vs G1 GC performance
5. Optimize code to reduce object creation
6. Configure ZGC for ultra-low latency
7. Monitor GC activity using JVisualVM
8. Calculate optimal heap size for application
9. Implement object pool to reduce GC pressure
10. Debug OutOfMemoryError in production
