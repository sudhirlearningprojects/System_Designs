# Production Tuning Recipes

## 1. Baseline Configuration (Start Here)

### 1.1 Universal Production Template

```bash
# === MEMORY ===
-Xms4g -Xmx4g                           # Heap (set equal, avoid resize)
-XX:MaxMetaspaceSize=512m                # Cap metaspace
-XX:MaxDirectMemorySize=1g               # Cap direct buffers
-Xss512k                                 # Stack size (512k is enough)
-XX:+AlwaysPreTouch                      # Pre-fault heap pages at startup

# === GC (G1 — Default) ===
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200                 # Target pause time
-XX:G1HeapRegionSize=16m                 # Region size (heap/2048, round to power of 2)
-XX:InitiatingHeapOccupancyPercent=45    # Start concurrent mark
-XX:+ParallelRefProcEnabled              # Parallel reference processing

# === GC LOGGING ===
-Xlog:gc*:file=/var/log/java/gc.log:time,level,tags:filecount=5,filesize=100m

# === DIAGNOSTICS ===
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/java/heapdump.hprof
-XX:+ExitOnOutOfMemoryError              # Exit on OOM (container restarts)

# === JFR (Always-on) ===
-XX:StartFlightRecording=settings=default,maxsize=250m,maxage=12h,disk=true,dumponexit=true,filename=/var/log/java/flight.jfr

# === NETWORK ===
-Djava.net.preferIPv4Stack=true          # Avoid IPv6 issues
-Dsun.net.inetaddr.ttl=30               # DNS cache TTL (seconds)
```

### 1.2 Container-Specific Template (Docker/Kubernetes)

```bash
# Container 4GB limit
-XX:+UseContainerSupport                 # Respect cgroup limits (default ON)
-XX:MaxRAMPercentage=75.0                # 75% of container memory = heap
-XX:InitialRAMPercentage=75.0            # Start at max (avoid resize)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:MaxMetaspaceSize=256m
-Xss512k
-XX:+AlwaysPreTouch
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*:file=/var/log/gc.log:time,level,tags:filecount=3,filesize=50m
```

```yaml
# Kubernetes resource limits
resources:
  requests:
    memory: "4Gi"
    cpu: "2"
  limits:
    memory: "4Gi"   # Same as request to avoid OOM kills
    cpu: "4"        # Burst allowed
```

## 2. Recipe: Spring Boot Microservice (REST API)

### 2.1 Profile: Low-Latency API Service

```bash
# 8GB container, 200 concurrent requests, p99 < 100ms target
-XX:MaxRAMPercentage=70.0
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50                  # Aggressive pause target
-XX:G1NewSizePercent=30                  # Larger young gen for short-lived requests
-XX:G1MaxNewSizePercent=50
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=35    # Start concurrent early
-XX:+ParallelRefProcEnabled
-XX:MaxMetaspaceSize=256m
-Xss512k
-XX:+AlwaysPreTouch
```

### 2.2 Profile: High-Throughput Batch Service

```bash
# 16GB heap, batch processing, throughput > latency
-Xms16g -Xmx16g
-XX:+UseParallelGC                       # Max throughput
-XX:ParallelGCThreads=8
-XX:GCTimeRatio=19                       # Max 5% in GC
-XX:+AlwaysPreTouch
-XX:MaxMetaspaceSize=512m
-XX:+UseCompressedOops                   # Heap < 32GB
```

### 2.3 Profile: Ultra-Low Latency Service (Java 21+)

```bash
# Trading system, p99 < 5ms, no GC pauses
-Xms12g -Xmx12g
-XX:+UseZGC
-XX:+ZGenerational                       # Generational ZGC
-XX:SoftMaxHeapSize=10g                  # Target size (GC proactively)
-XX:+AlwaysPreTouch
-XX:+UseTransparentHugePages             # Linux THP
-XX:+UseLargePages                       # Huge pages (requires OS config)
-XX:MaxMetaspaceSize=256m
```

## 3. Recipe: Apache Kafka Broker

```bash
# Kafka broker: high throughput, large heap, network-heavy
-Xms6g -Xmx6g                           # Don't go too large — Kafka uses page cache
-XX:+UseG1GC
-XX:MaxGCPauseMillis=20                  # Kafka is latency-sensitive
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=35
-XX:+ExplicitGCInvokesConcurrent         # System.gc() uses concurrent GC
-XX:+ParallelRefProcEnabled
-XX:MaxMetaspaceSize=256m
-XX:+AlwaysPreTouch

# Kafka-specific: let OS cache do the heavy lifting
# Keep heap small, OS page cache handles data files
# Monitor: page cache hit ratio, not just JVM heap
```

## 4. Recipe: Elasticsearch / OpenSearch

```bash
# Elasticsearch: large heap for field data cache, lucene uses page cache
-Xms16g -Xmx16g                         # Never exceed 50% of RAM (page cache!)
-XX:+UseG1GC                             # Default since ES 7.x
-XX:G1HeapRegionSize=32m                 # Large regions for large heap
-XX:InitiatingHeapOccupancyPercent=30    # Start marking early
-XX:MaxGCPauseMillis=200
-XX:+AlwaysPreTouch
-XX:-UseBiasedLocking                    # ES recommends disabling

# CRITICAL: Never exceed 50% of total RAM
# Lucene needs the rest for OS page cache (segment files)
# 64GB RAM → 31GB heap (stay under 32GB for compressed oops)
```

## 5. Recipe: Apache Spark Executor

```bash
# Spark executor: large heap, high allocation rate, many short-lived objects
-Xms8g -Xmx8g
-XX:+UseG1GC
-XX:G1HeapRegionSize=32m
-XX:InitiatingHeapOccupancyPercent=30
-XX:MaxGCPauseMillis=500                 # Batch tolerates longer pauses
-XX:G1NewSizePercent=30                  # Large young gen for allocations
-XX:G1MaxNewSizePercent=60
-XX:+AlwaysPreTouch

# For Spark: off-heap memory is important
# spark.memory.offHeap.enabled=true
# spark.memory.offHeap.size=4g
```

## 6. Recipe: gRPC / High-Connection Server

```bash
# gRPC server: many connections, direct buffers, moderate heap
-Xms4g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:MaxDirectMemorySize=2g               # Netty uses direct buffers heavily
-XX:MaxMetaspaceSize=256m
-Xss256k                                 # Small stacks (many threads)
-XX:+AlwaysPreTouch

# Monitor: direct buffer usage (jcmd VM.native_memory)
# Netty pool: io.netty.allocator.maxOrder=11 (default)
```

## 7. Recipe: Serverless / AWS Lambda

```bash
# Lambda: fast startup, limited memory, short-lived
-XX:TieredStopAtLevel=1                  # Skip C2 compilation (fast startup)
-XX:+UseSerialGC                         # Low overhead for small heaps
-Xms128m -Xmx512m                       # Match Lambda memory config
-XX:MaxMetaspaceSize=128m
-XX:-TieredCompilation                   # Or disable tiered entirely

# Better: Use GraalVM Native Image for Lambda
# → 10ms cold start instead of 1-5 seconds
# → 50MB memory instead of 200MB+
```

## 8. Tuning Process: Step-by-Step

### 8.1 Phase 1: Measure Baseline

```bash
# 1. Deploy with baseline config (Section 1.1)
# 2. Generate production-like load (50-75% of peak)
# 3. Collect metrics for 30+ minutes:

# GC behavior
jstat -gcutil <pid> 1000 | tee gc_baseline.txt

# Heap after GC (live data size)
jcmd <pid> GC.run           # Force full GC
jcmd <pid> GC.heap_info     # Check old gen usage = live data

# Allocation rate
# From GC log: bytes allocated between consecutive Young GCs / time

# Response time percentiles (from APM or logs)
```

### 8.2 Phase 2: Identify Bottleneck

```
Bottleneck Identification:
┌──────────────────────────────────────────────────────────────┐
│ Symptom                  │ Likely Bottleneck     │ Tool       │
├──────────────────────────────────────────────────────────────┤
│ p99 spikes periodically  │ GC pauses            │ GC logs    │
│ Steady latency increase  │ Memory leak / GC freq │ Heap dump  │
│ CPU 100% on some threads │ Code hotspot          │ Profiler   │
│ Low CPU, slow responses  │ Lock contention       │ Thread dump│
│ Low CPU, slow responses  │ I/O wait             │ JFR I/O    │
│ Heap growing post-GC     │ Memory leak          │ MAT        │
│ Metaspace growing        │ ClassLoader leak     │ jstat      │
│ RSS > Xmx + 2GB         │ Native memory leak   │ NMT        │
└──────────────────────────────────────────────────────────────┘
```

### 8.3 Phase 3: Apply Targeted Fix

```bash
# Example: p99 latency spikes every 30 seconds
# Root cause: Young GC pauses of 150ms

# Option A: Reduce pause time target
-XX:MaxGCPauseMillis=50       # G1 will adjust young gen size

# Option B: Switch to ZGC (Java 21)
-XX:+UseZGC -XX:+ZGenerational

# Option C: Increase young gen (less frequent GC, but longer pause)
-XX:G1NewSizePercent=40

# Always: Measure again after change!
```

### 8.4 Phase 4: Validate & Monitor

```bash
# After each change:
# 1. Run same load test
# 2. Compare metrics: p50, p95, p99 latency, throughput, GC time
# 3. Check for regressions: did fixing latency hurt throughput?
# 4. Monitor for 24h+ in production (weekly patterns matter)
```

## 9. Common Tuning Scenarios

### 9.1 Scenario: Frequent Young GC (Every 1-2 seconds)

```bash
# Diagnosis: High allocation rate, small young gen
# Evidence: jstat shows YGC every 1-2 seconds

# Fix 1: Increase young gen
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=60

# Fix 2: Reduce allocation rate (code change)
# Profile allocations: ./asprof -e alloc -d 30 -f alloc.html <pid>
# Common culprits:
#   - String concatenation in loops → StringBuilder
#   - Autoboxing (int → Integer) → primitive collections
#   - Excessive logging → check log level before formatting
#   - JSON serialization → reuse ObjectMapper, buffer pools
```

### 9.2 Scenario: Long Full GC Pauses

```bash
# Diagnosis: Full GC taking 5-10 seconds
# Evidence: GC log shows "Pause Full" events

# Why Full GC in G1?
# 1. Concurrent marking can't keep up with allocation
# 2. Humongous allocation failure
# 3. Metaspace expansion
# 4. System.gc() call

# Fix for #1:
-XX:InitiatingHeapOccupancyPercent=30    # Start concurrent earlier
-XX:G1ReservePercent=15                   # More headroom
-XX:ConcGCThreads=4                       # More concurrent GC threads

# Fix for #2 (humongous objects):
-XX:G1HeapRegionSize=32m                  # Larger regions

# Fix for #3:
-XX:MetaspaceSize=256m                    # Higher initial threshold

# Fix for #4:
-XX:+DisableExplicitGC                    # Ignore System.gc()
# OR
-XX:+ExplicitGCInvokesConcurrent          # Make it concurrent
```

### 9.3 Scenario: Memory Leak (Heap Growing Over Days)

```bash
# Diagnosis: Old gen after GC keeps growing
# Evidence: jstat -gcutil shows O% increasing over hours

# Step 1: Capture heap dumps at intervals
jcmd <pid> GC.heap_dump /tmp/heap_t0.hprof
# Wait 1 hour
jcmd <pid> GC.heap_dump /tmp/heap_t1.hprof

# Step 2: Compare in MAT
# Open both dumps → "Compare" → shows growing objects

# Step 3: Common fixes
# - Unbounded cache → use Caffeine with maxSize
# - Event listeners not removed → weak listeners
# - ThreadLocal not removed → add finally { tl.remove(); }
# - Connection/stream not closed → try-with-resources
# - Guava cache without eviction → add expireAfterWrite
```

### 9.4 Scenario: High GC Overhead (>15% Time in GC)

```bash
# Diagnosis: Application spending too much time in GC
# Evidence: jvm_gc_pause_seconds_sum rate shows >15%

# Root causes:
# 1. Heap too small for live data
# 2. Allocation rate too high
# 3. Too many surviving objects (premature promotion)

# Fix for #1: Increase heap
-Xmx8g    # Was 4g

# Fix for #2: Profile and reduce allocations
./asprof -e alloc -d 60 -f alloc.html <pid>

# Fix for #3: Tune tenuring
-XX:MaxTenuringThreshold=15
-XX:SurvivorRatio=4               # Larger survivors
```

### 9.5 Scenario: High RSS Beyond Heap (Native Memory Growth)

```bash
# Diagnosis: Container OOM killed, but heap was fine
# Evidence: RSS = 8GB, but Xmx = 4GB

# Step 1: Enable Native Memory Tracking
-XX:NativeMemoryTracking=summary

# Step 2: Check native memory
jcmd <pid> VM.native_memory summary

# Common culprits:
# - Thread stacks: Too many threads → reduce Xss or thread count
# - Direct buffers: Netty/NIO → cap MaxDirectMemorySize
# - Metaspace: Too many classes → cap MaxMetaspaceSize
# - Code cache: Large JIT output → cap ReservedCodeCacheSize
# - JNI/malloc: Native library leak → valgrind or ASan
```

## 10. JVM Flags Quick Reference

### 10.1 Memory Flags

| Flag | Default | Description |
|------|---------|-------------|
| `-Xms` | - | Initial heap size |
| `-Xmx` | 1/4 RAM | Maximum heap size |
| `-Xss` | 1MB | Thread stack size |
| `-XX:MaxMetaspaceSize` | Unlimited | Metaspace limit |
| `-XX:MetaspaceSize` | ~21MB | Initial metaspace GC threshold |
| `-XX:MaxDirectMemorySize` | `-Xmx` | Direct ByteBuffer limit |
| `-XX:ReservedCodeCacheSize` | 240MB | JIT code cache |
| `-XX:+AlwaysPreTouch` | false | Pre-fault heap pages |
| `-XX:MaxRAMPercentage` | 25% | Heap as % of RAM (containers) |
| `-XX:+UseCompressedOops` | auto | Compressed object pointers |

### 10.2 GC Flags

| Flag | Default | Description |
|------|---------|-------------|
| `-XX:+UseG1GC` | Java 9+ | G1 collector |
| `-XX:+UseZGC` | - | ZGC collector |
| `-XX:+ZGenerational` | Java 21+ | Generational ZGC |
| `-XX:+UseParallelGC` | Java 8 | Parallel collector |
| `-XX:MaxGCPauseMillis` | 200 | G1 target pause |
| `-XX:G1HeapRegionSize` | auto | G1 region size (1-32MB) |
| `-XX:InitiatingHeapOccupancyPercent` | 45 | Concurrent mark trigger |
| `-XX:ParallelGCThreads` | cores | STW GC thread count |
| `-XX:ConcGCThreads` | cores/4 | Concurrent GC threads |
| `-XX:+ParallelRefProcEnabled` | false | Parallel ref processing |
| `-XX:+DisableExplicitGC` | false | Ignore System.gc() |
| `-XX:+UseStringDeduplication` | false | G1 string dedup |

### 10.3 JIT Flags

| Flag | Default | Description |
|------|---------|-------------|
| `-XX:+TieredCompilation` | true | Tiered compilation |
| `-XX:TieredStopAtLevel` | 4 | Max compilation level |
| `-XX:CompileThreshold` | 10000 | Invocations before compile |
| `-XX:MaxInlineSize` | 35 | Always-inline threshold |
| `-XX:FreqInlineSize` | 325 | Hot method inline limit |
| `-XX:+DoEscapeAnalysis` | true | Enable escape analysis |
| `-XX:CICompilerCount` | varies | Compiler threads |

### 10.4 Diagnostics Flags

| Flag | Default | Description |
|------|---------|-------------|
| `-XX:+HeapDumpOnOutOfMemoryError` | false | Auto dump on OOM |
| `-XX:HeapDumpPath` | cwd | Heap dump location |
| `-XX:+ExitOnOutOfMemoryError` | false | Exit JVM on OOM |
| `-XX:NativeMemoryTracking` | off | Track native memory |
| `-Xlog:gc*` | - | GC logging (Java 9+) |
| `-XX:+PrintCompilation` | false | Log JIT compilations |
| `-XX:+PrintFlagsFinal` | false | Print all flag values |

## 11. Tuning Don'ts (Anti-Patterns)

```
❌ Don't set -Xms much smaller than -Xmx (causes resize pauses)
❌ Don't use -XX:+UseParallelGC for latency-sensitive services
❌ Don't set MaxGCPauseMillis=1 (G1 will make tiny young gens)
❌ Don't disable TieredCompilation unless you know why
❌ Don't set Xmx > 32GB without understanding compressed oops loss
❌ Don't tune without measuring (profile first!)
❌ Don't copy tuning flags from StackOverflow without context
❌ Don't set heap to 100% of container memory (leaves no room for native)
❌ Don't set -XX:+DisableExplicitGC if using NIO (direct buffer GC)
❌ Don't increase thread pool size to "fix" lock contention
❌ Don't use -XX:+AggressiveOpts (deprecated, unpredictable)
```
