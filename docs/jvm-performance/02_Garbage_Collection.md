# Garbage Collection Deep Dive

## 1. GC Fundamentals

### 1.1 What Triggers Garbage Collection?

```
┌─────────────────────────────────────────────────────┐
│              GC Triggering Conditions                 │
├─────────────────────────────────────────────────────┤
│ • Eden space full         → Minor GC (Young GC)     │
│ • Old Gen filling up      → Major GC / Mixed GC     │
│ • Metaspace full          → Full GC                 │
│ • System.gc() called      → Full GC (if honored)    │
│ • Allocation failure      → GC + possible OOM       │
│ • Concurrent GC threshold → Concurrent cycle start  │
└─────────────────────────────────────────────────────┘
```

### 1.2 Object Lifecycle & Promotion

```
                        ┌─────────────────────────┐
                        │    new Object()          │
                        └──────────┬──────────────┘
                                   ▼
                        ┌──────────────────────────┐
                        │         EDEN             │
                        │   (fast bump pointer)    │
                        └──────────┬──────────────┘
                                   │ Minor GC (survives)
                                   ▼
                        ┌──────────────────────────┐
                        │      SURVIVOR (S0/S1)    │
                        │   age++ each survival    │
                        └──────────┬──────────────┘
                                   │ age >= MaxTenuringThreshold (default 15)
                                   ▼
                        ┌──────────────────────────┐
                        │      OLD GENERATION      │
                        │   (tenured / promoted)   │
                        └──────────────────────────┘
```

### 1.3 GC Roots (Starting Points for Reachability)

- Active thread stack frames (local variables, parameters)
- Static fields of loaded classes
- JNI references
- Synchronization monitors (locked objects)
- JVM internal references (class loaders, exception objects)

## 2. GC Algorithms

### 2.1 Mark-and-Sweep

```
Phase 1: MARK (trace from GC roots)        Phase 2: SWEEP (reclaim unmarked)
┌─────────────────────────┐                ┌─────────────────────────┐
│ ■ ■ □ ■ □ □ ■ □ ■ □    │                │ ■ ■ _ ■ _ _ ■ _ ■ _    │
│ ■=reachable □=garbage   │    ──────►     │ ■=kept   _=freed        │
└─────────────────────────┘                └─────────────────────────┘
                                           Problem: Memory fragmentation!
```

### 2.2 Mark-and-Compact

```
Phase 1: MARK     Phase 2: COMPACT (slide live objects)
┌─────────────┐   ┌─────────────┐
│ ■ □ ■ □ ■ □│   │ ■ ■ ■ _ _ _│   ← No fragmentation
└─────────────┘   └─────────────┘   ← But expensive (object relocation)
```

### 2.3 Copying Collector (Used in Young Gen)

```
FROM Space:              TO Space (after copy):
┌─────────────┐          ┌─────────────┐
│ ■ □ ■ □ ■ □│  ──────► │ ■ ■ ■ _ _ _│
└─────────────┘          └─────────────┘
• O(live objects) — fast when most are garbage (young gen!)
• Wastes 50% space (needs two semispaces)
```

### 2.4 Generational Hypothesis

> "Most objects die young."

Empirical observation across all applications:
- 90-95% of objects die in Young Gen (never reach Old Gen)
- This is why minor GC is fast: only copies the few survivors

## 3. Garbage Collectors

### 3.1 Serial GC (`-XX:+UseSerialGC`)

```
Application:  ████████████████████|STOP|████████████████████
GC Thread:                        |████|
                                   ↑ Single-threaded STW pause
```

- Single GC thread, stop-the-world
- Best for: single-core, small heaps (<256MB), client apps
- Flag: `-XX:+UseSerialGC`

### 3.2 Parallel GC (`-XX:+UseParallelGC`)

```
Application:  ████████████████████|STOP|████████████████████
GC Threads:                       |████|  (N parallel threads)
                                  |████|
                                  |████|
```

- Multiple parallel GC threads during STW
- Best for: batch processing, throughput-oriented workloads
- Default in Java 8
- Flags:
```bash
-XX:+UseParallelGC
-XX:ParallelGCThreads=8           # Number of GC threads
-XX:GCTimeRatio=99                # Target: 1% time in GC
-XX:MaxGCPauseMillis=200          # Soft pause target
```

### 3.3 G1 GC (`-XX:+UseG1GC`) — Default since Java 9

#### Architecture: Region-Based Heap

```
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ E  │ S  │    │ O  │ O  │ H  │    │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ E  │    │ O  │ O  │ O  │    │ H  │ E  │
├────┼────┼────┼────┼────┼────┼────┼────┤
│    │ O  │ O  │ E  │    │ O  │    │ S  │
└────┴────┴────┴────┴────┴────┴────┴────┘

E = Eden    S = Survivor    O = Old    H = Humongous
(empty) = Free region
Region size: 1MB - 32MB (power of 2, auto-calculated)
```

#### G1 GC Phases

```
┌──────────────────────────────────────────────────────────────┐
│                    G1 GC Cycle                                │
│                                                              │
│  ┌──────────────┐                                           │
│  │ Young GC     │  STW: Evacuate Eden+Survivor → Survivor   │
│  │ (Minor)      │  Frequency: Every few seconds              │
│  └──────┬───────┘                                           │
│         │ (when IHOP threshold reached)                      │
│         ▼                                                    │
│  ┌──────────────────────────────────────────┐               │
│  │ Concurrent Marking Cycle                  │               │
│  │                                           │               │
│  │ 1. Initial Mark (STW, piggyback on YGC)  │               │
│  │ 2. Root Region Scan (concurrent)          │               │
│  │ 3. Concurrent Mark (concurrent)           │               │
│  │ 4. Remark (STW, SATB processing)         │               │
│  │ 5. Cleanup (STW + concurrent)            │               │
│  └──────┬───────────────────────────────────┘               │
│         │                                                    │
│         ▼                                                    │
│  ┌──────────────┐                                           │
│  │ Mixed GC     │  STW: Evacuate Young + selected Old       │
│  │              │  regions (by garbage-first priority)       │
│  └──────────────┘                                           │
└──────────────────────────────────────────────────────────────┘
```

#### G1 Key Tuning Parameters

```bash
# Essential G1 flags
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200              # Target pause time (default 200ms)
-XX:G1HeapRegionSize=16m              # Region size (1-32MB, power of 2)
-XX:InitiatingHeapOccupancyPercent=45 # When to start concurrent mark
-XX:G1MixedGCCountTarget=8           # Spread mixed GC over N cycles
-XX:G1HeapWastePercent=5              # Stop mixed if reclaimable < 5%

# Advanced tuning
-XX:G1NewSizePercent=5                # Min young gen (% of heap)
-XX:G1MaxNewSizePercent=60            # Max young gen (% of heap)
-XX:G1MixedGCLiveThresholdPercent=85  # Skip regions with >85% live
-XX:G1ReservePercent=10               # Reserve for promotion guarantee
```

#### G1 Humongous Objects

Objects > 50% of region size are "humongous" and allocated directly in Old Gen.

```bash
# If you see many humongous allocations, increase region size
-XX:G1HeapRegionSize=32m  # Objects up to 16MB won't be humongous
```

**Diagnostic:**
```bash
# Count humongous allocations in GC log
grep "humongous" gc.log | wc -l
```

### 3.4 ZGC (`-XX:+UseZGC`) — Ultra-Low Latency

#### Key Properties
- **Sub-millisecond pauses** (typically <1ms) regardless of heap size
- Heap sizes from 8MB to 16TB
- Concurrent: mark, relocate, remap all happen concurrently
- Uses colored pointers (metadata stored in pointer bits)

```
┌──────────────────────────────────────────────────────────────┐
│                     ZGC Architecture                          │
│                                                              │
│  Colored Pointer (64-bit):                                   │
│  ┌────────┬────┬────┬────┬────┬──────────────────────────┐  │
│  │ unused │ fin│remap│ m1 │ m0 │     object address       │  │
│  │ (18b)  │(1b)│(1b) │(1b)│(1b)│     (42 bits = 4TB)    │  │
│  └────────┴────┴────┴────┴────┴──────────────────────────┘  │
│                                                              │
│  Phases (ALL concurrent except ~<1ms pause for root scan):   │
│  1. Pause Mark Start (~200µs)                                │
│  2. Concurrent Mark + Remap                                  │
│  3. Pause Mark End (~200µs)                                  │
│  4. Concurrent Prepare Relocate                              │
│  5. Pause Relocate Start (~200µs)                           │
│  6. Concurrent Relocate                                      │
└──────────────────────────────────────────────────────────────┘
```

#### ZGC Flags

```bash
-XX:+UseZGC
-XX:+ZGenerational                    # Generational ZGC (Java 21+, much better)
-XX:SoftMaxHeapSize=4g               # Target heap size (GC more aggressively)
-XX:ZCollectionInterval=5            # Force GC every 5 seconds (proactive)
-XX:ZAllocationSpikeTolerance=2.0    # How fast to react to alloc spikes
```

#### When to Use ZGC

✅ Latency-sensitive: trading systems, real-time APIs, gaming servers
✅ Large heaps (>32GB) where G1 pauses become too long
✅ Java 21+ (Generational ZGC is production-ready)

❌ Don't use for: batch processing (Parallel GC has better throughput)
❌ Don't use for: very small heaps (<512MB, overhead too high)

### 3.5 Shenandoah GC (`-XX:+UseShenandoahGC`)

Similar goals to ZGC but different implementation (Brooks forwarding pointers).

```bash
-XX:+UseShenandoahGC
-XX:ShenandoahGCHeuristics=adaptive  # or: compact, aggressive, static
-XX:ShenandoahMinFreeThreshold=10    # Start GC when <10% free
```

**ZGC vs Shenandoah:**
| Feature | ZGC | Shenandoah |
|---|---|---|
| Pause time | <1ms | <10ms (usually <1ms) |
| Max heap | 16TB | Limited by OS |
| Pointer technique | Colored pointers | Brooks pointers |
| Read barrier cost | Load barrier | Load + store barrier |
| Availability | Oracle/OpenJDK | OpenJDK (RedHat) |
| Generational | Yes (Java 21) | No (planned) |

## 4. GC Logging & Analysis

### 4.1 Enable GC Logging (Java 9+ Unified Logging)

```bash
# Comprehensive GC log
-Xlog:gc*:file=gc.log:time,level,tags:filecount=5,filesize=100m

# Minimal GC log (pause times only)
-Xlog:gc:file=gc.log:time

# Include heap details and phases
-Xlog:gc+heap=info,gc+phases=info:file=gc.log:time

# Allocation stalls
-Xlog:gc+alloc=debug:file=gc.log:time

# Full verbose (debugging only)
-Xlog:gc*=trace:file=gc-trace.log:time,level,tags
```

### 4.2 Reading GC Logs

**G1 Young GC example:**
```
[2024-01-15T10:30:45.123+0000] GC(142) Pause Young (Normal) (G1 Evacuation Pause)
[2024-01-15T10:30:45.123+0000] GC(142)   Eden regions: 48->0(48)
[2024-01-15T10:30:45.123+0000] GC(142)   Survivor regions: 6->6(6)
[2024-01-15T10:30:45.123+0000] GC(142)   Old regions: 124->127
[2024-01-15T10:30:45.123+0000] GC(142)   Humongous regions: 2->2
[2024-01-15T10:30:45.123+0000] GC(142) Pause Young (Normal) 1204M->892M(4096M) 12.345ms
                                                             ↑before  ↑after  ↑heap  ↑pause
```

**Key Metrics to Monitor:**
- Pause duration (should meet SLA)
- Frequency (too frequent = heap too small or allocation too fast)
- Promotion rate (high = objects living too long in young gen)
- Reclaimed bytes (low = potential memory leak)

### 4.3 GC Log Analysis Tools

| Tool | Type | Best For |
|------|------|----------|
| [GCEasy](https://gceasy.io) | Web-based | Quick analysis, free |
| [GCViewer](https://github.com/chewiebug/GCViewer) | Desktop app | Offline analysis |
| [Censum](https://www.jclarity.com/censum) | Desktop app | Detailed recommendations |
| `gc_log_visualizer` | CLI script | CI/CD pipeline integration |

## 5. GC Tuning Strategies

### 5.1 Strategy: Minimize Pause Times (Latency-Critical)

```bash
# Option A: G1 with aggressive pause target
-XX:+UseG1GC
-XX:MaxGCPauseMillis=50
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=35  # Start concurrent earlier
-XX:G1NewSizePercent=20                # Larger young gen
-XX:G1MaxNewSizePercent=40

# Option B: ZGC (if Java 21+)
-XX:+UseZGC
-XX:+ZGenerational
-XX:SoftMaxHeapSize=6g
-Xmx8g
```

### 5.2 Strategy: Maximize Throughput (Batch Processing)

```bash
-XX:+UseParallelGC
-XX:ParallelGCThreads=8
-XX:GCTimeRatio=19          # Spend max 5% in GC (1/(1+19))
-Xms8g -Xmx8g              # Large heap, no resize
-XX:+AlwaysPreTouch         # Pre-fault pages at startup
```

### 5.3 Strategy: Container-Optimized

```bash
# Containers: JVM must respect cgroup limits
-XX:+UseContainerSupport              # Default ON since Java 10
-XX:MaxRAMPercentage=75.0             # Use 75% of container memory
-XX:InitialRAMPercentage=75.0         # Start at same size
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100

# For tight containers (512MB-1GB)
-XX:+UseZGC                           # or G1
-XX:MaxRAMPercentage=70.0             # Leave room for metaspace + native
-XX:MaxMetaspaceSize=128m
```

### 5.4 Strategy: Reduce GC Frequency (Allocation-Heavy)

```java
// Code-level: Reduce allocations
// BAD: Allocates new String per iteration
for (int i = 0; i < 1_000_000; i++) {
    String s = "prefix_" + i;  // allocates StringBuilder + String
    process(s);
}

// BETTER: Reuse StringBuilder
StringBuilder sb = new StringBuilder(32);
for (int i = 0; i < 1_000_000; i++) {
    sb.setLength(0);
    sb.append("prefix_").append(i);
    process(sb);
}
```

```bash
# JVM flags to handle high allocation rate
-XX:G1NewSizePercent=30          # Larger Eden to absorb bursts
-XX:G1MaxNewSizePercent=60
-XX:G1HeapRegionSize=32m         # Larger regions for big allocations
```

## 6. GC Performance Anti-Patterns

### 6.1 Premature Promotion

**Symptom:** High promotion rate, frequent mixed GCs, Old Gen filling fast.

**Cause:** Objects surviving just long enough to get promoted but dying shortly after.

**Fix:**
```bash
# Increase survivor space or tenuring threshold
-XX:MaxTenuringThreshold=15       # Keep in young gen longer
-XX:SurvivorRatio=4               # Larger survivors (Eden:S = 4:1:1)
-XX:+AlwaysTenure                 # DON'T do this (forces immediate promotion)
```

### 6.2 Full GC Due to Metaspace

**Symptom:** Full GC triggered by Metaspace expansion.

```
[GC (Metadata GC Threshold) ... Full GC (Metadata GC Threshold)]
```

**Fix:**
```bash
-XX:MetaspaceSize=256m            # Set initial threshold high
-XX:MaxMetaspaceSize=512m         # Cap to prevent runaway
```

### 6.3 Allocation Stall

**Symptom:** Threads blocked waiting for GC to free memory.

```
[GC (Allocation Failure) ...]
```

**Fix:** Larger heap, or faster concurrent GC initiation:
```bash
-XX:InitiatingHeapOccupancyPercent=35  # Start concurrent GC earlier
-XX:G1ReservePercent=15                # More headroom for promotions
```

### 6.4 Long Reference Processing

**Symptom:** GC pauses dominated by reference processing (WeakRef, SoftRef, PhantomRef, Finalizers).

**Fix:**
```bash
-XX:+ParallelRefProcEnabled       # Parallel reference processing
# Reduce soft references (cleared more aggressively)
-XX:SoftRefLRUPolicyMSPerMB=0    # Clear soft refs immediately on GC
```

## 7. Monitoring GC in Production

### 7.1 Key Metrics

```bash
# jstat: GC statistics (every 1 second, 10 samples)
jstat -gc <pid> 1000 10
jstat -gcutil <pid> 1000 10     # Percentages
jstat -gccause <pid> 1000 10   # With cause

# Example output (jstat -gcutil):
#  S0     S1     E      O      M      CCS    YGC   YGCT    FGC  FGCT     GCT
#  0.00  84.21  45.67  52.34  96.12  93.45   142   1.234    2   0.567   1.801
#
# S0/S1 = Survivor usage%    E = Eden%     O = Old%    M = Metaspace%
# YGC = Young GC count       YGCT = Young GC total time (seconds)
# FGC = Full GC count        FGCT = Full GC total time
```

### 7.2 Prometheus/Grafana JVM Metrics

```java
// Micrometer + Prometheus (Spring Boot)
// Auto-exposed via /actuator/prometheus

// Key metrics to alert on:
// jvm_gc_pause_seconds{cause="G1 Evacuation Pause"} > 0.5
// jvm_gc_memory_promoted_bytes_total rate > 50MB/s
// jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
```

### 7.3 Alerting Thresholds

| Metric | Warning | Critical |
|--------|---------|----------|
| GC pause p99 | >200ms | >500ms |
| GC overhead (% time in GC) | >5% | >15% |
| Heap utilization | >75% | >90% |
| Full GC frequency | >1/hour | >1/10min |
| Allocation rate | >2GB/s | >5GB/s |
| Promotion rate | >100MB/s | >500MB/s |

## 8. GC Selection Flowchart

```
                    ┌────────────────────────┐
                    │ What's your priority?   │
                    └───────────┬────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                  ▼
    ┌─────────────────┐  ┌──────────────┐  ┌──────────────┐
    │ Low Latency     │  │  Throughput   │  │  Balanced    │
    │ (<10ms pauses)  │  │  (batch jobs) │  │  (general)   │
    └────────┬────────┘  └──────┬───────┘  └──────┬───────┘
             │                  │                  │
       ┌─────┴─────┐           ▼                  ▼
       ▼           ▼     ┌──────────┐       ┌──────────┐
  ┌─────────┐ ┌────────┐│ Parallel │       │   G1GC   │
  │   ZGC   │ │Shenand-││   GC     │       │(default) │
  │(Java21+)│ │ oah    │└──────────┘       └──────────┘
  └─────────┘ └────────┘
```
