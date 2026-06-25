# JVM Architecture & Memory Model

## 1. JVM Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Java Application                          │
├─────────────────────────────────────────────────────────────────┤
│                      Java Class Libraries                        │
├─────────────────────────────────────────────────────────────────┤
│                   JVM (HotSpot / OpenJ9 / GraalVM)              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐ │
│  │ Class Loader │  │  Execution   │  │    Runtime Data Areas  │ │
│  │  Subsystem   │  │   Engine     │  │                        │ │
│  │              │  │  ┌────────┐  │  │  ┌──────┐ ┌────────┐  │ │
│  │ Bootstrap    │  │  │  JIT   │  │  │  │ Heap │ │Metaspace│  │ │
│  │ Extension    │  │  │Compiler│  │  │  └──────┘ └────────┘  │ │
│  │ Application  │  │  └────────┘  │  │  ┌──────┐ ┌────────┐  │ │
│  │              │  │  ┌────────┐  │  │  │Stack │ │   PC   │  │ │
│  │              │  │  │Interp- │  │  │  │      │ │Register│  │ │
│  │              │  │  │reter   │  │  │  └──────┘ └────────┘  │ │
│  └──────────────┘  │  └────────┘  │  │  ┌─────────────────┐  │ │
│                    │  ┌────────┐  │  │  │ Native Method   │  │ │
│                    │  │  GC    │  │  │  │ Stack           │  │ │
│                    │  └────────┘  │  │  └─────────────────┘  │ │
│                    └──────────────┘  └───────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                    Native Interface (JNI)                        │
├─────────────────────────────────────────────────────────────────┤
│                   Operating System / Hardware                    │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Runtime Data Areas (Memory Model)

### 2.1 Heap Memory

The heap is where all object instances live. It's the primary area managed by the garbage collector.

```
┌─────────────────────────────────────────────────────────────────┐
│                         HEAP MEMORY                               │
├────────────────────────────┬────────────────────────────────────┤
│       Young Generation     │         Old Generation              │
│  ┌───────┐  ┌───────────┐ │                                    │
│  │ Eden  │  │ Survivor  │ │     Tenured Space                  │
│  │       │  │ S0  │ S1  │ │     (Long-lived objects)           │
│  │(new   │  │     │     │ │                                    │
│  │objects)│  │     │     │ │                                    │
│  └───────┘  └───────────┘ │                                    │
├────────────────────────────┴────────────────────────────────────┤
│ Young:Old ratio typically 1:2 (configurable via -XX:NewRatio)    │
└─────────────────────────────────────────────────────────────────┘
```

**Key Facts:**
- `-Xms` / `-Xmx`: Initial and max heap size
- Default Young:Old ratio is 1:2 (Young = 1/3 of heap)
- Eden:Survivor ratio defaults to 8:1:1 (`-XX:SurvivorRatio=8`)
- Objects allocate in Eden first → promote to Survivor → eventually Old Gen

**Sizing Guidelines:**
```bash
# Production: set Xms = Xmx to avoid resize pauses
-Xms4g -Xmx4g

# Young gen sizing (if not using G1)
-XX:NewSize=1g -XX:MaxNewSize=1g

# Or use ratio
-XX:NewRatio=2  # Old = 2x Young, so Young = 1/3 heap
```

### 2.2 Metaspace (Replaces PermGen since Java 8)

Stores class metadata, method bytecodes, constant pool, and annotations.

```
┌─────────────────────────────────────────────┐
│              METASPACE (Native Memory)        │
│                                              │
│  ┌──────────────┐  ┌─────────────────────┐  │
│  │ Class        │  │ Compressed Class    │  │
│  │ Metadata     │  │ Space               │  │
│  │              │  │ (if UseCompressed   │  │
│  │ • Klass      │  │  ClassPointers)     │  │
│  │ • Methods    │  │                     │  │
│  │ • Constant   │  │                     │  │
│  │   Pool       │  │                     │  │
│  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────┘
```

**Key Flags:**
```bash
-XX:MetaspaceSize=256m           # Initial threshold for GC
-XX:MaxMetaspaceSize=512m        # Hard limit (default: unlimited)
-XX:CompressedClassSpaceSize=256m # If compressed oops enabled
```

**Common Issues:**
- Metaspace leak = unbounded class loading (common with hot-deploy, reflection, dynamic proxies)
- Monitor with: `jstat -gcmetacapacity <pid>`

### 2.3 Thread Stacks

Each thread gets its own stack storing frames (local variables, operand stack, return address).

```bash
-Xss512k          # Thread stack size (default 1MB on 64-bit)
-XX:ThreadStackSize=512  # Same as above, in KB
```

**Performance Impact:**
- 10,000 threads × 1MB = 10GB of native memory just for stacks
- Reduce to 512k or 256k for high-thread-count applications
- Virtual threads (Java 21) use heap-backed continuations instead

### 2.4 Direct Memory (Off-Heap)

Used by NIO ByteBuffers for zero-copy I/O operations.

```bash
-XX:MaxDirectMemorySize=1g  # Limit direct memory (default = -Xmx)
```

**Use Cases:**
- Netty, gRPC, Kafka clients
- Memory-mapped files
- Large caches (Ehcache off-heap, Chronicle Map)

### 2.5 Code Cache

Stores JIT-compiled native code.

```bash
-XX:ReservedCodeCacheSize=256m     # Max code cache (default 240MB)
-XX:InitialCodeCacheSize=64m       # Initial size
```

**Warning:** If code cache fills up, JIT stops compiling → sudden performance degradation.

## 3. Class Loading Subsystem

### 3.1 Class Loader Hierarchy

```
┌─────────────────────────────────┐
│    Bootstrap ClassLoader        │  (rt.jar, java.base module)
│    (Native C++ implementation)  │
└───────────────┬─────────────────┘
                │ parent
┌───────────────▼─────────────────┐
│   Platform/Extension ClassLoader │  (ext/*.jar, jdk modules)
└───────────────┬─────────────────┘
                │ parent
┌───────────────▼─────────────────┐
│   Application/System ClassLoader │  (classpath, -cp)
└───────────────┬─────────────────┘
                │ parent
┌───────────────▼─────────────────┐
│   Custom ClassLoaders            │  (WAR isolators, OSGi, etc.)
└─────────────────────────────────┘
```

### 3.2 Class Loading Phases

1. **Loading** – Read .class bytecode from filesystem/network/jar
2. **Linking**
   - **Verification** – Bytecode verifier checks structural correctness
   - **Preparation** – Allocate memory for static fields, set defaults
   - **Resolution** – Symbolic references → direct references
3. **Initialization** – Execute `<clinit>` (static initializers)

### 3.3 Performance Impact of Class Loading

```bash
# Verbose class loading (debug only)
-verbose:class
-Xlog:class+load=info

# Class Data Sharing (CDS) for faster startup
java -Xshare:dump                          # Create shared archive
java -Xshare:on -jar app.jar              # Use shared archive

# Application CDS (Java 13+)
java -XX:ArchiveClassesAtExit=app-cds.jsa -jar app.jar  # Record
java -XX:SharedArchiveFile=app-cds.jsa -jar app.jar     # Use
```

**CDS Impact:** 20-40% startup time reduction for microservices.

## 4. Object Memory Layout

### 4.1 Object Header (HotSpot 64-bit)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Object Header (12-16 bytes)                    │
├─────────────────────────────────┬───────────────────────────────┤
│         Mark Word (8 bytes)     │  Klass Pointer (4 bytes*)     │
│                                 │  (* with compressed oops)     │
│ ┌─────────────────────────────┐ │                               │
│ │ HashCode │ Age │ Lock State │ │  Points to class metadata     │
│ │ (25 bit) │(4b) │  (2 bit)  │ │  in Metaspace                │
│ └─────────────────────────────┘ │                               │
├─────────────────────────────────┴───────────────────────────────┤
│              Instance Data (fields)                               │
├─────────────────────────────────────────────────────────────────┤
│              Padding (alignment to 8 bytes)                       │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Compressed Oops

```bash
# Enabled by default for heaps < 32GB
-XX:+UseCompressedOops            # Object pointers: 8→4 bytes
-XX:+UseCompressedClassPointers   # Klass pointers: 8→4 bytes
```

**Impact:** ~30-40% heap reduction. Automatically disabled if heap > 32GB.

### 4.3 Field Reordering & Memory Waste

The JVM reorders fields to minimize padding. Use JOL (Java Object Layout) to analyze:

```java
// Add dependency: org.openjdk.jol:jol-core:0.17
import org.openjdk.jol.info.ClassLayout;

public class MemoryLayoutDemo {
    public static void main(String[] args) {
        System.out.println(ClassLayout.parseInstance(new MyObject()).toPrintable());
    }
}

class MyObject {
    boolean flag;    // 1 byte
    long timestamp;  // 8 bytes  
    int count;       // 4 bytes
    byte type;       // 1 byte
}
```

Output shows field order and padding:
```
MyObject object internals:
OFF  SZ      TYPE DESCRIPTION
  0   8           (object header: mark)
  8   4           (object header: class)
 12   4       int MyObject.count
 16   8      long MyObject.timestamp
 24   1   boolean MyObject.flag
 25   1      byte MyObject.type
 26   6           (object alignment gap)
Instance size: 32 bytes
```

## 5. Java Memory Model (JMM) for Performance

### 5.1 Happens-Before Relationships

The JMM defines visibility guarantees. Without proper synchronization, threads may see stale data due to:
- CPU caches (L1/L2/L3)
- Store buffers
- Compiler reordering

```java
// BROKEN: no happens-before between writer and reader
class Broken {
    private boolean ready = false;
    private int value = 0;

    void writer() {
        value = 42;       // might be reordered after ready=true
        ready = true;
    }

    void reader() {
        if (ready) {
            System.out.println(value); // might print 0!
        }
    }
}

// FIXED: volatile establishes happens-before
class Fixed {
    private volatile boolean ready = false;
    private int value = 0;

    void writer() {
        value = 42;       // guaranteed visible before ready=true
        ready = true;     // volatile write = release fence
    }

    void reader() {
        if (ready) {      // volatile read = acquire fence
            System.out.println(value); // always prints 42
        }
    }
}
```

### 5.2 False Sharing

When two threads write to different fields on the same cache line (64 bytes), they invalidate each other's cache.

```java
// PROBLEM: counter1 and counter2 likely share a cache line
class FalseSharing {
    volatile long counter1;
    volatile long counter2; // 8 bytes apart = same cache line
}

// SOLUTION: Pad to separate cache lines
class NoPadding {
    volatile long counter1;
    long p1, p2, p3, p4, p5, p6, p7; // 56 bytes padding
    volatile long counter2;
}

// BETTER: Use @Contended (Java 8+, requires -XX:-RestrictContended)
import sun.misc.Contended;

class BetterSolution {
    @Contended volatile long counter1;
    @Contended volatile long counter2;
}
```

**Benchmark Impact:** False sharing can cause 10-100x slowdown on multi-threaded counters.

## 6. JVM Startup Optimization

### 6.1 Startup Time Components

```
┌──────────────────────────────────────────────────────┐
│ JVM Init → Class Loading → Bytecode Verify → JIT → Ready │
│   50ms        200ms           100ms         500ms+        │
└──────────────────────────────────────────────────────┘
```

### 6.2 Optimization Techniques

```bash
# 1. Application CDS (biggest impact)
java -XX:ArchiveClassesAtExit=app.jsa -jar app.jar  # Training run
java -XX:SharedArchiveFile=app.jsa -jar app.jar     # Fast start

# 2. Tiered compilation with stop at tier 1 (fast startup, slower peak)
-XX:TieredStopAtLevel=1

# 3. Disable verification for trusted code (risky in production)
-Xverify:none  # or -XX:-BytecodeVerificationLocal

# 4. GraalVM Native Image (AOT compilation)
native-image -jar app.jar  # Compile ahead of time: ~10ms startup

# 5. Spring Boot optimizations
-Dspring.jmx.enabled=false
-Dspring.config.location=classpath:application.properties
```

### 6.3 Startup vs Throughput Trade-offs

| Optimization | Startup Impact | Throughput Impact |
|---|---|---|
| CDS/AppCDS | -30-50% startup | No impact |
| TieredStopAtLevel=1 | -40% startup | -20-30% peak perf |
| GraalVM Native | -95% startup | Variable (often slower) |
| Large heap pretouch | +5% startup | +5% steady state |

## 7. NUMA-Aware Memory Allocation

For multi-socket servers:

```bash
-XX:+UseNUMA                    # NUMA-aware allocation
-XX:+UseNUMAInterleaving        # Interleave across NUMA nodes
```

**When to use:** Servers with 2+ CPU sockets. Can improve throughput 10-30% by allocating objects near the CPU that created them.

## 8. Practical Commands

```bash
# View all JVM flags and their current values
java -XX:+PrintFlagsFinal -version 2>&1 | grep -i "heap\|gc\|meta"

# View flags of running process
jcmd <pid> VM.flags

# View system properties
jcmd <pid> VM.system_properties

# View native memory usage
java -XX:NativeMemoryTracking=summary -jar app.jar
jcmd <pid> VM.native_memory summary

# View class loading stats
jstat -class <pid> 1000  # every 1 second

# Total memory breakdown
jcmd <pid> VM.native_memory summary scale=MB
```

**Sample Native Memory Tracking output:**
```
Total: reserved=5765MB, committed=4521MB
-                 Java Heap (reserved=4096MB, committed=4096MB)
-                     Class (reserved=1056MB, committed=42MB)
-                    Thread (reserved=256MB, committed=256MB)
-                      Code (reserved=250MB, committed=67MB)
-                        GC (reserved=78MB, committed=50MB)
-                  Internal (reserved=12MB, committed=12MB)
-                    Symbol (reserved=8MB, committed=8MB)
-    Native Memory Tracking (reserved=5MB, committed=5MB)
-                     Other (reserved=4MB, committed=4MB)
```
