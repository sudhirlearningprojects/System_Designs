# Profiling & Monitoring Tools

## 1. Profiling Strategy

### 1.1 When to Profile What

```
┌────────────────────────────────────────────────────────────────┐
│              Symptom → Tool → Action                            │
├────────────────────────────────────────────────────────────────┤
│ High CPU usage         → async-profiler (CPU)   → Optimize code│
│ High latency/pauses    → GC logs + JFR          → Tune GC      │
│ Memory growing         → heap dump + MAT         → Fix leak     │
│ Thread blocked         → thread dump + JFR       → Fix lock     │
│ Slow response time     → distributed tracing     → Fix bottlenck│
│ High allocation rate   → async-profiler (alloc)  → Reduce alloc │
│ Startup slow           → JFR + CDS              → AOT/warmup   │
└────────────────────────────────────────────────────────────────┘
```

### 1.2 Profiling Overhead Comparison

| Tool | CPU Overhead | Memory Overhead | Production Safe? |
|------|-------------|-----------------|------------------|
| JFR (default settings) | <1% | ~20MB | ✅ Always-on |
| JFR (all events) | 2-5% | ~50MB | ✅ Short bursts |
| async-profiler | 1-2% | ~10MB | ✅ Short bursts |
| VisualVM (sampling) | 3-5% | ~50MB | ⚠️ Dev/staging |
| VisualVM (instrumentation) | 20-50% | ~200MB | ❌ Dev only |
| JMX metrics | <1% | ~5MB | ✅ Always-on |
| YourKit | 5-10% | ~100MB | ⚠️ Dev/staging |

## 2. Java Flight Recorder (JFR)

### 2.1 Starting JFR

```bash
# Method 1: At startup (always-on, minimal overhead)
java -XX:StartFlightRecording=\
  settings=default,\
  maxsize=250m,\
  maxage=12h,\
  disk=true,\
  filename=app.jfr,\
  dumponexit=true \
  -jar app.jar

# Method 2: Attach to running process
jcmd <pid> JFR.start name=profile duration=60s filename=profile.jfr

# Method 3: Start continuous, dump on demand
jcmd <pid> JFR.start name=continuous maxsize=500m maxage=1h
# Later, when investigating:
jcmd <pid> JFR.dump name=continuous filename=incident.jfr

# Method 4: With custom settings
jcmd <pid> JFR.start name=detailed settings=profile duration=120s filename=detailed.jfr

# Stop/check recording
jcmd <pid> JFR.check
jcmd <pid> JFR.stop name=profile
```

### 2.2 JFR Events of Interest

```
┌──────────────────────────────────────────────────────────────┐
│                    Key JFR Events                              │
├──────────────────────────────────────────────────────────────┤
│ CPU & Code:                                                   │
│   jdk.ExecutionSample         → CPU profiling (sampled stacks)│
│   jdk.CompilerInlining       → Inlining decisions            │
│   jdk.Compilation            → JIT compilation events         │
│   jdk.Deoptimization         → Deoptimization events          │
│                                                               │
│ GC & Memory:                                                  │
│   jdk.GarbageCollection      → GC pause times                │
│   jdk.G1GarbageCollection    → G1 specific details           │
│   jdk.ObjectAllocationSample → Allocation hotspots           │
│   jdk.OldObjectSample        → Potential memory leaks        │
│   jdk.GCHeapSummary          → Heap after GC                 │
│                                                               │
│ Threads & Locks:                                              │
│   jdk.JavaMonitorWait        → Lock wait times               │
│   jdk.JavaMonitorEnter       → Lock contention               │
│   jdk.ThreadSleep            → Thread.sleep calls            │
│   jdk.ThreadPark             → LockSupport.park calls        │
│   jdk.VirtualThreadPinned    → Pinned virtual threads        │
│                                                               │
│ I/O:                                                          │
│   jdk.SocketRead             → Network read latency          │
│   jdk.SocketWrite            → Network write latency         │
│   jdk.FileRead               → File read latency            │
│   jdk.FileWrite              → File write latency           │
│                                                               │
│ Exceptions:                                                   │
│   jdk.JavaExceptionThrow     → Exception creation            │
│   jdk.JavaErrorThrow         → Error creation                │
└──────────────────────────────────────────────────────────────┘
```

### 2.3 Custom JFR Events

```java
// Define custom event for business metrics
@Label("Order Processing")
@Description("Tracks order processing time")
@Category({"Application", "Orders"})
@StackTrace(false)
public class OrderEvent extends jdk.jfr.Event {
    @Label("Order ID")
    String orderId;
    
    @Label("Processing Time")
    @Timespan(Timespan.MILLISECONDS)
    long processingTimeMs;
    
    @Label("Item Count")
    int itemCount;
    
    @Label("Success")
    boolean success;
}

// Usage
public void processOrder(Order order) {
    OrderEvent event = new OrderEvent();
    event.begin();
    try {
        // Process order...
        event.success = true;
    } catch (Exception e) {
        event.success = false;
    } finally {
        event.orderId = order.getId();
        event.itemCount = order.getItems().size();
        event.processingTimeMs = Duration.between(start, Instant.now()).toMillis();
        event.end();
        event.commit();  // Only writes if event is enabled
    }
}
```

### 2.4 Analyzing JFR with JDK Mission Control (JMC)

```bash
# Open JFR file in JMC GUI
jmc -open profile.jfr

# CLI analysis with jfr tool (Java 17+)
jfr summary profile.jfr
jfr print --events jdk.GarbageCollection profile.jfr
jfr print --events jdk.ExecutionSample --stack-depth 10 profile.jfr

# Filter specific time range
jfr print --events jdk.GCHeapSummary \
  --from "2024-01-15 10:30:00" \
  --to "2024-01-15 10:35:00" \
  profile.jfr
```

## 3. async-profiler

### 3.1 Overview

The best low-overhead profiler for production JVMs. Captures true CPU samples (not safepoint-biased).

```bash
# Install
wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/asprof-3.0-linux-x64.tar.gz
tar xzf asprof-3.0-linux-x64.tar.gz

# Basic CPU profiling (30 seconds, HTML flame graph)
./asprof -d 30 -f cpu_profile.html <pid>

# Allocation profiling (find who allocates the most)
./asprof -d 30 -e alloc -f alloc_profile.html <pid>

# Lock contention profiling
./asprof -d 30 -e lock -f lock_profile.html <pid>

# Wall-clock profiling (includes I/O wait time)
./asprof -d 30 -e wall -t -f wall_profile.html <pid>
# -t = separate per-thread flame graphs

# Profile specific threads
./asprof -d 30 --filter "http-nio-*" -f http_profile.html <pid>
```

### 3.2 Reading Flame Graphs

```
┌────────────────────────────────────────────────────────────────┐
│                  How to Read Flame Graphs                       │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │              processRequest                                │ │  ← Wide = more time
│  ├──────────────────────┬─────────────────────────────────┤ │
│  │    parseJSON         │       queryDatabase              │ │  ← Called by above
│  ├──────────┬───────────┼──────────────┬──────────────────┤ │
│  │readBytes │ validate  │ getConn      │  executeSQL      │ │
│  └──────────┴───────────┴──────────────┴──────────────────┘ │
│                                                                │
│  Width = time spent in that function (including children)     │
│  Color = category (Java=green, native=orange, kernel=red)     │
│  Look for: wide bars at bottom (CPU hogs)                     │
│            flat top = leaf function doing real work            │
│            plateau = unexpected time in single method          │
└────────────────────────────────────────────────────────────────┘
```

### 3.3 Allocation Profiling

```bash
# Profile heap allocations
./asprof -d 60 -e alloc -f alloc.html <pid>

# Profile only allocations > 1KB
./asprof -d 60 -e alloc --alloc 1k -f large_alloc.html <pid>

# Output sample:
# Top allocators:
#   35% - com.example.Serializer.toJson      → Jackson serialization
#   22% - java.util.Arrays.copyOf            → ArrayList resize
#   15% - java.lang.String.concat            → String concatenation
#    8% - io.netty.buffer.PooledByteBuf      → Network buffers
```

### 3.4 Continuous Profiling in Production

```bash
# Start async-profiler as Java agent (always-on, rotate files)
java -agentpath:/opt/asprof/lib/libasyncProfiler.so=\
  start,event=cpu,interval=10ms,\
  file=/var/log/profiles/cpu-%t.jfr,\
  jfr,\
  loop=60s \
  -jar app.jar

# This creates a new JFR file every 60 seconds
# Can be shipped to a profiling backend (Pyroscope, Grafana)
```

## 4. jcmd (JVM Diagnostic Command)

### 4.1 Essential Commands

```bash
# List all Java processes
jcmd -l

# VM information
jcmd <pid> VM.version
jcmd <pid> VM.flags              # All active JVM flags
jcmd <pid> VM.system_properties  # System properties
jcmd <pid> VM.info               # Comprehensive VM info
jcmd <pid> VM.uptime

# Memory
jcmd <pid> GC.heap_info          # Heap layout and usage
jcmd <pid> GC.heap_dump /tmp/heap.hprof  # Heap dump
jcmd <pid> GC.class_histogram    # Object histogram (no dump needed)
jcmd <pid> GC.run                # Force Full GC

# Native Memory
jcmd <pid> VM.native_memory summary        # NMT summary
jcmd <pid> VM.native_memory detail         # NMT detail
jcmd <pid> VM.native_memory baseline       # Set baseline
jcmd <pid> VM.native_memory summary.diff   # Diff from baseline

# Threads
jcmd <pid> Thread.print          # Thread dump

# JFR
jcmd <pid> JFR.start name=rec duration=60s filename=rec.jfr
jcmd <pid> JFR.dump name=rec filename=dump.jfr
jcmd <pid> JFR.stop name=rec

# Compiler
jcmd <pid> Compiler.codecache    # Code cache usage
jcmd <pid> Compiler.queue        # Compilation queue

# ClassLoading
jcmd <pid> VM.classloaders       # ClassLoader hierarchy
jcmd <pid> VM.classes            # All loaded classes
```

### 4.2 Practical Diagnostic Scripts

```bash
#!/bin/bash
# quick-diag.sh - Quick JVM diagnostics
PID=$1

echo "=== JVM Info ==="
jcmd $PID VM.version
echo ""

echo "=== Heap Usage ==="
jcmd $PID GC.heap_info
echo ""

echo "=== Top 20 Objects by Size ==="
jcmd $PID GC.class_histogram | head -25
echo ""

echo "=== Thread Count ==="
jcmd $PID Thread.print | grep -c "\"" 
echo ""

echo "=== Code Cache ==="
jcmd $PID Compiler.codecache
echo ""

echo "=== GC Stats ==="
jstat -gcutil $PID
```

## 5. jstat (JVM Statistics)

```bash
# GC utilization (most commonly used)
jstat -gcutil <pid> 1000 10
# Output: S0% S1% E% O% M% CCS% YGC YGCT FGC FGCT CGC CGCT GCT
# S0/S1: Survivor    E: Eden    O: Old    M: Metaspace
# YGC: Young GC count   FGC: Full GC count   GCT: Total GC time

# GC capacity
jstat -gccapacity <pid> 1000
# Shows min/max/current capacity of each generation

# GC cause
jstat -gccause <pid> 1000
# Shows last GC cause (Allocation Failure, System.gc(), etc.)

# Class loading
jstat -class <pid> 1000
# Loaded/unloaded class count and time

# JIT compilation
jstat -compiler <pid> 1000
# Compiled methods, failed compilations, time spent compiling

# Practical: Watch for problems
watch -n 1 "jstat -gcutil $(pgrep -f 'app.jar') | tail -1"
```

## 6. Heap Dump Analysis

### 6.1 Capturing Heap Dumps

```bash
# Live heap dump (triggers Full GC first, removes unreachable objects)
jmap -dump:live,format=b,file=heap_live.hprof <pid>

# Full heap dump (includes unreachable objects)
jmap -dump:format=b,file=heap_full.hprof <pid>

# Preferred: jcmd (doesn't require -XX:+AttachListener)
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# Auto-dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/java/

# Quick histogram (no dump file, instant)
jmap -histo:live <pid> | head -30
# or
jcmd <pid> GC.class_histogram | head -30
```

### 6.2 Eclipse MAT Analysis Workflow

```
1. Open heap dump in Eclipse MAT

2. Leak Suspects Report (automated):
   → Shows top memory consumers with path to GC root
   → "Problem Suspect 1: 1.2GB retained by HashMap in CacheManager"

3. Dominator Tree:
   → Shows retained size per object
   → Find: Who keeps the most memory alive?

4. Histogram:
   → Object count by class
   → Sort by retained heap
   → "1,234,567 instances of byte[], total 890MB"

5. OQL (Object Query Language):
   → SELECT * FROM java.util.HashMap WHERE size > 10000
   → SELECT toString(referent) FROM java.lang.ref.WeakReference

6. Path to GC Roots:
   → Right-click suspected object
   → "Path to GC Roots → exclude weak/soft/phantom refs"
   → Shows WHY the object is alive
```

### 6.3 MAT One-Liners (CLI)

```bash
# Parse heap dump and generate reports (headless)
./ParseHeapDump.sh heap.hprof \
  org.eclipse.mat.api:suspects \
  org.eclipse.mat.api:overview \
  org.eclipse.mat.api:top_components

# Output: ZIP file with HTML reports
```

## 7. JMX Monitoring

### 7.1 Enable Remote JMX

```bash
# Basic JMX (no auth, dev only)
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9090
-Dcom.sun.management.jmxremote.rmi.port=9090
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
-Djava.rmi.server.hostname=0.0.0.0

# Production: with auth and SSL
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9090
-Dcom.sun.management.jmxremote.authenticate=true
-Dcom.sun.management.jmxremote.password.file=/etc/jmx/password
-Dcom.sun.management.jmxremote.access.file=/etc/jmx/access
-Dcom.sun.management.jmxremote.ssl=true
```

### 7.2 Key MBeans

```
java.lang:type=Memory
  → HeapMemoryUsage (used, max, committed)
  → NonHeapMemoryUsage

java.lang:type=GarbageCollector,name=G1 Young Generation
  → CollectionCount, CollectionTime
  → LastGcInfo (duration, memoryUsageBeforeGc, memoryUsageAfterGc)

java.lang:type=Threading
  → ThreadCount, PeakThreadCount, DaemonThreadCount
  → findDeadlockedThreads()

java.lang:type=OperatingSystem
  → ProcessCpuLoad, SystemCpuLoad
  → FreePhysicalMemorySize, TotalPhysicalMemorySize

java.lang:type=Runtime
  → Uptime, InputArguments, ClassPath
```

### 7.3 Prometheus JMX Exporter

```yaml
# jmx_exporter config.yml
rules:
  - pattern: 'java.lang<type=Memory><>HeapMemoryUsage'
    name: jvm_memory_heap_bytes
    type: GAUGE
    attrNameSnakeCase: true
  - pattern: 'java.lang<type=GarbageCollector,name=(.+)><>CollectionCount'
    name: jvm_gc_collection_count
    labels:
      gc: $1
    type: COUNTER
  - pattern: 'java.lang<type=Threading><>ThreadCount'
    name: jvm_threads_current
    type: GAUGE
```

```bash
# Run as Java agent
java -javaagent:jmx_prometheus_javaagent.jar=8081:config.yml -jar app.jar
# Metrics at http://localhost:8081/metrics
```

## 8. Micrometer + Spring Boot Actuator

### 8.1 Essential JVM Metrics

```java
// Spring Boot auto-configures these JVM metrics:
// jvm.memory.used{area=heap|nonheap, id=...}
// jvm.memory.max{area=heap|nonheap, id=...}
// jvm.gc.pause{action=..., cause=...}
// jvm.gc.memory.promoted
// jvm.gc.memory.allocated
// jvm.threads.live
// jvm.threads.daemon
// jvm.threads.peak
// jvm.buffer.memory.used{id=direct|mapped}
// jvm.classes.loaded
// process.cpu.usage

// Custom business metrics
@Component
@RequiredArgsConstructor
public class OrderMetrics {
    private final MeterRegistry registry;
    
    private Timer orderProcessingTimer;
    private Counter orderCounter;
    
    @PostConstruct
    void init() {
        orderProcessingTimer = Timer.builder("order.processing.time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
            
        orderCounter = Counter.builder("order.total")
            .tag("status", "success")
            .register(registry);
    }
    
    public void recordOrder(Duration processingTime) {
        orderProcessingTimer.record(processingTime);
        orderCounter.increment();
    }
}
```

### 8.2 Grafana Dashboard Queries (PromQL)

```promql
# Heap utilization percentage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# GC pause time p99 (5 minute window)
histogram_quantile(0.99, rate(jvm_gc_pause_seconds_bucket[5m]))

# Allocation rate (bytes/sec)
rate(jvm_gc_memory_allocated_bytes_total[1m])

# Promotion rate (bytes/sec)
rate(jvm_gc_memory_promoted_bytes_total[1m])

# Thread count change
delta(jvm_threads_live_threads[1h])

# CPU usage
process_cpu_usage * 100

# GC overhead (% time spent in GC)
rate(jvm_gc_pause_seconds_sum[5m]) / 300 * 100
```

## 9. VisualVM

```bash
# Launch (included with JDK or download separately)
jvisualvm

# Or with VisualVM standalone
visualvm --jdkhome /path/to/jdk

# Capabilities:
# - Real-time heap/CPU/threads monitoring
# - Heap dump capture and analysis
# - Thread dump capture
# - CPU/memory sampling
# - CPU/memory instrumentation (dev only)
# - Plugin ecosystem (BTrace, MBeans, etc.)
```

## 10. Production Monitoring Checklist

```
□ JFR always-on with 12h ring buffer (jcmd dump on incidents)
□ GC logging to rotated files (5 × 100MB)
□ HeapDumpOnOutOfMemoryError enabled
□ Prometheus/Micrometer JVM metrics exposed
□ Grafana dashboard with:
  □ Heap usage over time
  □ GC pause duration (p50, p95, p99)
  □ GC frequency
  □ Thread count
  □ CPU usage
  □ Allocation/promotion rates
□ Alerts configured:
  □ Heap > 85% for 5 minutes
  □ GC pause p99 > 500ms
  □ GC overhead > 10%
  □ Full GC count > 0 (for G1/ZGC)
  □ Thread count increasing unbounded
□ async-profiler available for on-demand profiling
□ Native Memory Tracking enabled in staging
```
