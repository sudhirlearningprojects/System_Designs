# JVM Configuration & Tuning for Enterprise Applications

## 1. JVM Memory Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        JVM Process                       │
│                                                          │
│  ┌──────────────── Heap ────────────────┐                │
│  │  ┌─────────────┐  ┌───────────────┐ │                │
│  │  │  Young Gen  │  │   Old Gen     │ │                │
│  │  │ ┌───┬─────┐ │  │  (Tenured)   │ │                │
│  │  │ │Eden│S0/S1│ │  │              │ │                │
│  │  │ └───┴─────┘ │  │              │ │                │
│  │  └─────────────┘  └───────────────┘ │                │
│  └──────────────────────────────────────┘                │
│                                                          │
│  ┌──────────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Metaspace   │  │  Stack   │  │  Direct Memory   │  │
│  │ (class meta) │  │(per thrd)│  │  (NIO buffers)   │  │
│  └──────────────┘  └──────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Memory Regions

| Region | Flag | Default | Stores |
|--------|------|---------|--------|
| Heap | `-Xms` / `-Xmx` | 256m / 25% RAM | Objects |
| Young Gen | `-Xmn` or `-XX:NewRatio` | 1/3 of heap | New objects |
| Eden | `-XX:SurvivorRatio` | 8/10 of Young | Newly allocated |
| Survivor (S0/S1) | `-XX:SurvivorRatio` | 1/10 each | Survived 1+ GC |
| Old Gen | Remainder of heap | 2/3 of heap | Long-lived objects |
| Metaspace | `-XX:MaxMetaspaceSize` | Unlimited | Class metadata |
| Stack | `-Xss` | 512k–1m | Thread frames |
| Direct | `-XX:MaxDirectMemorySize` | = `-Xmx` | NIO off-heap |

### Deep Dive: How Object Allocation Works

When your code does `new Object()`, the JVM follows this exact path:

```
1. TLAB (Thread-Local Allocation Buffer) fast path
   └─ Each thread owns a private chunk of Eden (~1% of Eden)
   └─ Allocation = pointer bump, no synchronization needed
   └─ ~1ns per allocation — essentially free

2. Eden slow path (TLAB exhausted)
   └─ JVM requests a new TLAB from Eden
   └─ If Eden is full → triggers Minor GC

3. Large object path
   └─ Object > TLAB size → allocated directly in Eden (with lock)
   └─ Object > 50% of G1 region → Humongous allocation → Old Gen directly

4. Stack allocation (escape analysis)
   └─ If JIT proves object doesn't escape the method → allocated on stack
   └─ Zero GC pressure — freed when method returns
```

```bash
# See TLAB statistics
-XX:+PrintTLAB                    # log TLAB usage (debug)
-XX:TLABSize=512k                 # fix TLAB size (default: adaptive)
-XX:+ResizeTLAB                   # let JVM auto-tune TLAB size (default on)
```

### Object Lifecycle & Promotion

```
Allocation → Eden
     ↓ (Minor GC — object survives)
Survivor S0 (age=1)
     ↓ (another Minor GC — survives again)
Survivor S1 (age=2)
     ↓ (after MaxTenuringThreshold GCs)
Old Gen (Tenured)
```

```bash
-XX:MaxTenuringThreshold=15       # promote to Old after 15 GCs (default)
                                  # lower this if objects are long-lived
                                  # to avoid copying overhead in survivors
-XX:+PrintTenuringDistribution    # see age histogram — tune threshold
```

**Key insight**: If survivors are consistently > 50% full, objects get promoted early regardless of age threshold. This is called "premature promotion" and leads to Old Gen pressure.

### NewRatio vs Xmn

```bash
# Option 1: ratio-based (recommended — scales with heap changes)
-XX:NewRatio=2        # Old:Young = 2:1 → Young = 1/3 of heap

# Option 2: fixed size (fragile — breaks if you change -Xmx)
-Xmn1g               # Young Gen = 1GB fixed

# SurvivorRatio: Eden:Survivor = ratio:1
-XX:SurvivorRatio=8  # Eden=8, S0=1, S1=1 → Eden = 80% of Young Gen
```

### Non-Heap Memory: What Lives Outside the Heap

Many developers only think about heap, but non-heap memory is equally important:

| Area | What's In It | Risk |
|------|-------------|------|
| Metaspace | Class bytecode, method metadata, constant pool | Classloader leaks → OOM |
| Code Cache | JIT-compiled native code | Full cache → deoptimization, CPU spike |
| Stack | Local variables, method call frames per thread | StackOverflowError |
| Direct Memory | ByteBuffer.allocateDirect(), Netty, Kafka | OOM: Direct buffer memory |
| Mapped Files | Memory-mapped files via FileChannel | OS-level, not JVM-tracked |

```
Total process memory ≈ Heap + Metaspace + CodeCache + (threads × Xss) + DirectMemory + OS overhead
```

---

## 2. Garbage Collectors

### GC Comparison

| GC | Flag | Pause | Throughput | Best For |
|----|------|-------|------------|----------|
| Serial | `-XX:+UseSerialGC` | High | Low | Single-core, small heap |
| Parallel | `-XX:+UseParallelGC` | Medium | High | Batch jobs, throughput |
| G1 | `-XX:+UseG1GC` | Low-Medium | Medium | General purpose (default Java 9+) |
| ZGC | `-XX:+UseZGC` | <1ms | Medium | Large heaps, low latency |
| Shenandoah | `-XX:+UseShenandoahGC` | <1ms | Medium | Low latency, any heap size |

### How GC Works Internally: The Tri-Color Marking Algorithm

All modern GCs use a variant of tri-color marking to find live objects:

```
White  = not yet visited (candidate for collection)
Gray   = discovered but children not yet scanned
Black  = fully scanned (definitely live)

Algorithm:
1. Start: all objects White, GC roots → Gray
2. Pick a Gray object, scan its references → mark children Gray
3. Mark the Gray object Black
4. Repeat until no Gray objects remain
5. All remaining White objects = garbage → collect

GC Roots (always live):
  - Static fields
  - Local variables on thread stacks
  - JNI references
  - Class objects loaded by bootstrap classloader
```

### Write Barriers & Read Barriers

Concurrent GCs need barriers to stay correct while your app mutates the heap:

```
Write Barrier (G1, Shenandoah):
  Triggered on every reference store: obj.field = ref
  Records the old value in a "remembered set" or SATB buffer
  Ensures concurrent marking doesn't miss objects modified during GC

Read Barrier (ZGC):
  Triggered on every reference load: ref = obj.field
  Checks colored pointer bits — if object was relocated, fixes the pointer
  This is why ZGC can relocate objects concurrently without STW
  Cost: ~1-3ns per reference load (vs write barrier ~0.5ns)
```

### G1GC — Default for Most Enterprise Apps

```
Heap divided into equal-sized regions (1–32MB each)
Each region dynamically assigned as Eden / Survivor / Old / Humongous

Minor GC: collects Eden + Survivor regions
Mixed GC:  collects Eden + Survivor + some Old regions
Full GC:   last resort, stop-the-world (avoid this!)
```

**G1GC Phases in Detail:**

```
Phase 1: Young GC (STW, parallel)
  - Evacuate live objects from Eden + Survivor to new Survivor/Old regions
  - Update remembered sets
  - Typical pause: 10–50ms

Phase 2: Concurrent Marking Cycle (concurrent, overlaps with app)
  Step 1 - Initial Mark (STW, piggybacks on Young GC): mark GC roots
  Step 2 - Root Region Scan (concurrent): scan Survivor regions
  Step 3 - Concurrent Mark (concurrent): trace object graph
  Step 4 - Remark (STW, short): finalize marking with SATB
  Step 5 - Cleanup (STW + concurrent): reclaim empty regions, sort by liveness

Phase 3: Mixed GC (STW, parallel)
  - Collect Young regions + selected Old regions (lowest liveness first)
  - Continues until Old Gen reclaimed enough space
```

Key flags:
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200          # target pause time (soft goal)
-XX:G1HeapRegionSize=16m          # region size (1–32MB, power of 2)
-XX:G1NewSizePercent=20           # min young gen %
-XX:G1MaxNewSizePercent=40        # max young gen %
-XX:G1MixedGCLiveThresholdPercent=85  # only collect regions < 85% live
-XX:InitiatingHeapOccupancyPercent=45 # start concurrent marking at 45% heap
-XX:G1ReservePercent=10           # keep 10% heap as emergency buffer
-XX:G1HeapWastePercent=5          # stop mixed GC when < 5% reclaimable
```

**Why Full GC happens in G1 (and how to prevent it):**
```
Cause 1: Concurrent marking can't keep up with allocation rate
  Fix: Lower -XX:InitiatingHeapOccupancyPercent (start marking earlier)
       Increase -XX:ConcGCThreads

Cause 2: Humongous object allocation fills Old Gen
  Fix: Increase -XX:G1HeapRegionSize

Cause 3: Evacuation failure (no free regions to evacuate to)
  Fix: Increase -XX:G1ReservePercent
       Increase heap size
```

### ZGC — Ultra-Low Latency (Java 15+ production-ready)

```
Concurrent: marking, relocation, reference processing all concurrent
Colored pointers: metadata stored in pointer bits (load barriers)
Pause times: <1ms regardless of heap size (tested up to 16TB)
```

**ZGC Phases:**
```
Phase 1: Pause Mark Start (STW, <1ms)
  - Mark GC roots only

Phase 2: Concurrent Mark (concurrent)
  - Trace entire object graph while app runs
  - Load barrier fixes stale references on the fly

Phase 3: Pause Mark End (STW, <1ms)
  - Finalize marking

Phase 4: Concurrent Prepare for Relocation (concurrent)
  - Select relocation set (regions to compact)

Phase 5: Pause Relocate Start (STW, <1ms)
  - Relocate objects in GC roots

Phase 6: Concurrent Relocate (concurrent)
  - Move objects, update forwarding table
  - App threads fix pointers lazily via load barrier
```

```bash
-XX:+UseZGC
-XX:SoftMaxHeapSize=28g           # soft limit, ZGC tries to stay below
-XX:ZCollectionInterval=5         # force GC every 5 seconds (avoid spikes)
-XX:ZUncommitDelay=300            # return memory to OS after 5 min idle
-XX:+ZGenerational                # Java 21+ generational ZGC (better throughput)
```

**ZGC Generational (Java 21+):** Before Java 21, ZGC was non-generational — it treated all objects equally. Generational ZGC adds Young/Old generations, reducing the amount of work per GC cycle by 3-5x for typical workloads.

### Shenandoah — Low Latency Alternative

**Key difference from ZGC**: Shenandoah uses a forwarding pointer in the object header (Brooks pointer) instead of colored pointers. This means it works on any JVM without special pointer encoding, but adds 8 bytes per object overhead.

```bash
-XX:+UseShenandoahGC
-XX:ShenandoahGCMode=iu           # incremental update (default, lower CPU)
-XX:ShenandoahGCHeuristics=adaptive  # auto-tune GC triggers
# Other heuristics: static, compact, aggressive (for testing)
```

**Shenandoah vs ZGC:**
```
Shenandoah:
  + Works on Java 8, 11, 17, 21 (backported by Red Hat)
  + Lower memory overhead for small heaps
  - Brooks pointer = 8 bytes per object overhead
  - Slightly higher CPU overhead

ZGC:
  + Better throughput with Generational ZGC (Java 21)
  + No per-object overhead
  + Tested at larger heap sizes (up to 16TB)
  - Java 11+ only (production-ready Java 15+)
```

---

## 3. Heap Sizing

### Rules of Thumb

```
Container/Pod memory = 2GB
  Heap (-Xmx)        = 75% = 1.5GB
  Metaspace          = ~256MB
  Stack (threads)    = threads × Xss  (200 threads × 512k = 100MB)
  Direct memory      = ~256MB
  JVM overhead       = ~100MB
```

### Always Set Xms = Xmx in Production

```bash
# BAD — JVM resizes heap, causes GC pressure and latency spikes
-Xms256m -Xmx4g

# GOOD — pre-allocate full heap, no resize overhead
-Xms4g -Xmx4g
```

**Why this matters:** When heap needs to grow, the JVM must request memory from the OS and potentially run a Full GC to compact. This causes unpredictable latency spikes at startup and under load. Pre-allocating eliminates this entirely.

**Exception:** Development environments — use `-Xms256m -Xmx4g` to avoid wasting RAM on idle dev instances.

### Live Set Ratio — The Most Important Sizing Rule

```
Live set = amount of heap occupied after a Full GC (only truly live objects)

Rule: Heap should be 3–4× the live set size

Why:
  - GC efficiency degrades as heap fills up
  - G1 needs free regions to evacuate into
  - ZGC needs headroom for concurrent relocation

Example:
  After Full GC, heap shows 1GB used → set -Xmx3g to -Xmx4g
  If you set -Xmx1.2g, GC runs constantly (heap always near full)
```

### Humongous Objects (G1GC)

Objects > 50% of region size go directly to Old Gen, bypassing Young Gen.
```bash
# If allocating large arrays/buffers frequently:
-XX:G1HeapRegionSize=32m   # increase region size to reduce humongous allocations
```

**How to detect humongous allocations:**
```bash
# In GC log, look for:
# [GC pause (G1 Humongous Allocation)...]
# This means a large object triggered GC directly

# Or use async-profiler to find allocation sites:
./profiler.sh -e alloc -d 30 -f alloc.html <pid>
# Look for large byte[] or char[] allocations
```

### Heap Sizing for Different Workloads

```
Web API (request-response):
  - Many short-lived objects (request objects, DTOs)
  - Large Young Gen helps: -XX:NewRatio=1 (50% Young)
  - Tune survivor spaces to avoid premature promotion

Caching application:
  - Large Old Gen needed for cached objects
  - -XX:NewRatio=4 (20% Young, 80% Old)
  - Consider off-heap caching (Redis, Caffeine with soft refs)

Stream processing:
  - Constant allocation of event objects
  - Large Eden, small survivors
  - ZGC or Shenandoah to avoid pauses during processing
```

---

## 4. GC Tuning Flags

### GC Logging (Essential for Diagnosis)

```bash
# Java 9+
-Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=10,filesize=50m

# Java 8
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCTimeStamps
-Xloggc:/var/log/app/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=50m
```

**What to look for in GC logs:**

```
[0.523s][info][gc] GC(3) Pause Young (Normal) (G1 Evacuation Pause) 512M->128M(2048M) 12.345ms
                                                                      ^before ^after ^heap    ^pause

Key metrics to track:
  1. Pause duration — should be < MaxGCPauseMillis 95% of the time
  2. Heap before/after — large delta = lots of garbage (good)
                         small delta = mostly live objects (bad — heap pressure)
  3. GC frequency — too frequent = heap too small
  4. Full GC count — should be 0 in steady state
  5. Allocation rate — (heap_before_GC - heap_after_prev_GC) / time_between_GCs
```

**Unified logging selectors (Java 9+):**
```bash
-Xlog:gc                          # basic GC events only
-Xlog:gc*                         # all GC subsystems (verbose)
-Xlog:gc+heap=debug               # heap sizing details
-Xlog:gc+age=trace                # tenuring age distribution
-Xlog:safepoint                   # safepoint pause details (not just GC)
-Xlog:gc*,safepoint:file=gc.log:time,uptime,pid:filecount=5,filesize=20m
```

### Safepoints — The Hidden Latency Source

Many developers blame GC for all pauses, but safepoints cause pauses too:

```
A safepoint is a point where all threads are paused so the JVM can:
  - Run GC
  - Deoptimize JIT code
  - Take a thread dump
  - Revoke biased locks
  - Class redefinition (hot-swap)

Time-to-safepoint (TTSP): time for all threads to reach a safe state
  - Threads check safepoint flag at: method returns, loop back-edges, JNI calls
  - A thread in a long loop may delay safepoint for milliseconds!

Diagnose with:
-Xlog:safepoint:file=safepoint.log:time,uptime
# Look for: "Reaching safepoint" time > a few ms → long-running loops
```

### Pause Time Tuning

```bash
-XX:MaxGCPauseMillis=100          # G1: target 100ms pauses
-XX:GCPauseIntervalMillis=1000    # G1: pause at most once per second
-XX:ParallelGCThreads=8           # STW GC threads (default = CPU cores)
-XX:ConcGCThreads=2               # Concurrent GC threads (25% of parallel)
```

**ParallelGCThreads vs ConcGCThreads:**
```
ParallelGCThreads: used during STW phases (all threads stop, GC runs in parallel)
  Default = min(8, CPU_cores) for <= 8 cores
          = 8 + (CPU_cores - 8) * 5/8 for > 8 cores
  On a 32-core machine: 8 + (32-8)*5/8 = 8 + 15 = 23 threads

ConcGCThreads: used during concurrent phases (app runs alongside GC)
  Default = max(1, ParallelGCThreads / 4)
  Too high: steals CPU from app threads
  Too low: GC can't keep up with allocation rate → Full GC
```

### String Deduplication (G1 only)

```bash
-XX:+UseStringDeduplication       # deduplicate identical String objects in Old Gen
-XX:StringDeduplicationAgeThreshold=3  # deduplicate after 3 GC cycles
```

**How it works:** G1 maintains a hash table of char[] arrays. When a String is promoted to Old Gen, its char[] is hashed and compared. If an identical char[] already exists, the duplicate is replaced with a reference to the existing one, and the duplicate char[] becomes garbage.

**When it helps:** Applications with many duplicate strings — HTTP headers, JSON field names, database column values. Can reduce heap by 10–30% in such workloads.

**When it doesn't help:** Unique strings (UUIDs, user-generated content). The hash table overhead outweighs the savings.

```bash
# Measure effectiveness:
-XX:+PrintStringDeduplicationStatistics
# Output shows: new/deduped/skipped counts and bytes saved
```

---

## 5. JIT Compiler Tuning

### Tiered Compilation (default Java 8+)

```
Level 0: Interpreter
Level 1: C1 (simple JIT, fast compile)
Level 2: C1 (limited profiling)
Level 3: C1 (full profiling)
Level 4: C2 (optimizing JIT, slow compile, fast execution)
```

**How the JIT decides what to compile:**
```
Interpreter runs the method and counts:
  - Invocation count (method calls)
  - Back-edge count (loop iterations)

When invocation_count + back_edge_count > CompileThreshold:
  → Method queued for C1 compilation (Level 1-3)

When C1-compiled method accumulates enough profiling data:
  → Method queued for C2 compilation (Level 4)

C2 uses profile data to make aggressive optimizations:
  - Inlining (most impactful — eliminates call overhead + enables further opts)
  - Loop unrolling
  - Escape analysis → stack allocation, lock elision
  - Intrinsics (replace Java code with hand-optimized CPU instructions)
  - Speculative optimizations (deoptimize if assumption violated)
```

### Key JIT Optimizations Explained

**Inlining** — the most important optimization:
```java
// Before inlining:
int result = add(a, b);   // method call overhead + prevents further opts

// After inlining (JIT replaces call with body):
int result = a + b;       // no call, enables constant folding, escape analysis
```
```bash
-XX:MaxInlineSize=35          # max bytecode size to inline (default 35 bytes)
-XX:FreqInlineSize=325        # max size for frequently called methods
-XX:+PrintInlining            # log inlining decisions (debug only)
# If a hot method isn't being inlined, it's likely too large
# Refactor large methods into smaller ones to enable inlining
```

**Escape Analysis** — eliminates heap allocations:
```java
// JIT can prove 'point' doesn't escape this method
void process() {
    Point p = new Point(1, 2);  // normally heap-allocated
    int sum = p.x + p.y;        // JIT: allocate on stack or eliminate entirely
}
// Result: zero GC pressure for this allocation
```
```bash
-XX:+DoEscapeAnalysis         # enabled by default
-XX:+EliminateAllocations     # stack-allocate non-escaping objects
-XX:+EliminateLocks           # remove locks on non-escaping objects
```

**Deoptimization** — when JIT assumptions break:
```
JIT compiles with assumption: "field X is always type Foo"
Later: a subclass Bar is loaded → assumption violated
JIT deoptimizes: reverts to interpreter for that method
Eventually: recompiles with updated type profile

Symptoms of excessive deoptimization:
  - CPU spike after class loading
  - "made not entrant" / "made zombie" in PrintCompilation output
  - Code cache churn
```

```bash
-XX:+TieredCompilation                  # enabled by default
-XX:CompileThreshold=10000              # invocations before C2 compile
-XX:+OptimizeStringConcat               # optimize string concatenation
-XX:+UseCompressedOops                  # compress 64-bit pointers (heap < 32GB)
-XX:+UseCompressedClassPointers         # compress class pointers
-XX:ReservedCodeCacheSize=512m          # JIT compiled code cache (increase if deopt warnings)
-XX:+PrintCompilation                   # log JIT compilation (debug only)
```

### Code Cache — Often Overlooked

```
The code cache stores JIT-compiled native code.
If it fills up:
  - JVM stops compiling new methods
  - Falls back to interpreter for new code
  - CPU usage spikes, throughput drops
  - Log message: "CodeCache is full. Compiler has been disabled."

Default size: 240MB (Java 8), larger in Java 9+
```

```bash
-XX:ReservedCodeCacheSize=512m    # total code cache
-XX:InitialCodeCacheSize=64m      # initial allocation
-XX:+UseCodeCacheFlushing         # evict old compiled code when cache full
                                  # (enabled by default, but can cause jitter)

# Monitor code cache:
jcmd <pid> Compiler.codecache
# or via JMX: java.lang:type=Compilation
```

### Warmup Strategy for Latency-Sensitive Services

```
Problem: JVM starts cold — first requests hit interpreter (10-100x slower)
         p99 latency is terrible for first few minutes

Solutions:

1. Tiered compilation warmup (built-in):
   -XX:+TieredCompilation  ← already on by default
   Methods reach C2 after ~10K invocations

2. JVM Warmup via synthetic traffic:
   - Send representative requests before going live
   - Use readiness probe delay in Kubernetes

3. CDS (Class Data Sharing):
   # Step 1: create archive
   java -Xshare:dump -XX:SharedArchiveFile=app.jsa -jar app.jar
   # Step 2: use archive on startup
   java -Xshare:on -XX:SharedArchiveFile=app.jsa -jar app.jar
   # Benefit: 20-40% faster startup, shared read-only memory across JVMs

4. AppCDS (Application CDS, Java 10+):
   java -XX:+UseAppCDS -XX:SharedArchiveFile=app.jsa \
        -XX:SharedClassListFile=classes.lst -jar app.jar
   # Includes application classes in the shared archive
```

### Ahead-of-Time (AOT) — GraalVM Native Image

```bash
# Compile to native binary — eliminates JVM warmup entirely
native-image -jar app.jar \
  --no-fallback \
  -H:+ReportExceptionStackTraces \
  --initialize-at-build-time
# Result: <10ms startup, 50% less memory, no JIT warmup
```

**Trade-offs of Native Image:**
```
Pros:
  + Instant startup (<10ms vs seconds for JVM)
  + Lower memory footprint (no JIT compiler overhead)
  + Smaller container images
  + Predictable latency (no JIT compilation pauses)

Cons:
  - No dynamic class loading at runtime
  - Reflection requires configuration (reflect-config.json)
  - Peak throughput lower than JIT (no runtime profiling)
  - Longer build times (minutes vs seconds)
  - Debugging is harder

Best for: Serverless functions, CLI tools, short-lived containers
Avoid for: Long-running services where JIT peak throughput matters
```

---

## 6. Thread & Stack Tuning

```bash
-Xss512k                          # stack size per thread (reduce for many threads)
                                  # default 1MB on 64-bit Linux

# For apps with 1000+ threads:
-Xss256k                          # saves 750MB vs default for 1000 threads

# Virtual Threads (Java 21) — eliminates thread stack concern
# Virtual threads use heap, not native stack
```

### What's on the Stack

```
Each thread has its own stack. Each method call pushes a stack frame containing:
  - Local variables (primitives stored directly, object references)
  - Operand stack (intermediate computation values)
  - Return address
  - Reference to constant pool

Stack frame size depends on:
  - Number of local variables
  - Depth of expression evaluation
  - NOT the size of objects (those are on heap — only references on stack)

StackOverflowError causes:
  - Infinite recursion (most common)
  - Very deep call chains (e.g., deeply nested JSON parsing)
  - Large number of local variables in recursive methods
```

### Thread Pool Sizing

```
CPU-bound tasks:   threads = CPU cores
IO-bound tasks:    threads = CPU cores × (1 + wait_time / compute_time)

Example: 8 cores, 90% IO wait
  threads = 8 × (1 + 0.9/0.1) = 8 × 10 = 80 threads
```

**Little's Law for thread pool sizing:**
```
L = λ × W

L = number of concurrent requests in the system
λ = arrival rate (requests/sec)
W = average response time (seconds)

Example:
  500 req/sec, average response time 200ms
  L = 500 × 0.2 = 100 concurrent requests
  → Need at least 100 threads to handle this load

Add 20-30% headroom: 120-130 threads
```

### Virtual Threads (Java 21) — Project Loom

```
Traditional platform threads:
  - 1:1 mapping to OS threads
  - ~1MB stack each
  - Context switch = OS kernel call (~1-10μs)
  - Practical limit: ~10K threads per JVM

Virtual threads:
  - M:N mapping — many virtual threads on few carrier (OS) threads
  - Stack on heap (grows/shrinks dynamically, starts at ~1KB)
  - Context switch = JVM-level (~100ns)
  - Practical limit: millions per JVM
```

```java
// Create virtual thread
Thread.ofVirtual().start(() -> handleRequest(request));

// Virtual thread executor (drop-in replacement)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Spring Boot 3.2+ — enable virtual threads globally:
// spring.threads.virtual.enabled=true
```

**When virtual threads help vs don't help:**
```
HELP: IO-bound workloads (database calls, HTTP calls, file IO)
  - Thread blocks on IO → virtual thread unmounts from carrier thread
  - Carrier thread picks up another virtual thread
  - Effectively free concurrency for IO waits

DON'T HELP: CPU-bound workloads
  - Virtual thread can't unmount during CPU computation
  - Same throughput as platform threads
  - Slight overhead from scheduling

WATCH OUT FOR: Pinning
  - Virtual thread is "pinned" to carrier thread during:
    * synchronized blocks/methods
    * native method calls
  - Pinned virtual thread blocks its carrier thread
  - Fix: replace synchronized with ReentrantLock
```

```bash
# Detect pinning:
-Djdk.tracePinnedThreads=full     # log when virtual threads get pinned
```

---

## 7. Metaspace Tuning

```bash
-XX:MetaspaceSize=256m            # initial metaspace (triggers GC when reached)
-XX:MaxMetaspaceSize=512m         # cap metaspace (prevent unbounded growth)
-XX:MinMetaspaceFreeRatio=20      # expand if < 20% free
-XX:MaxMetaspaceFreeRatio=80      # shrink if > 80% free
```

### What's Actually in Metaspace

```
Metaspace stores per-class JVM metadata (NOT the class objects themselves):
  - Klass structure (JVM's internal class representation)
  - Method bytecode
  - Method metadata (name, signature, access flags)
  - Constant pool
  - Annotations
  - vtable / itable (virtual method dispatch tables)

Class objects (java.lang.Class instances) → still on heap
String literals from constant pool → heap (String pool)
Static fields → heap (since Java 8)
```

**Metaspace vs PermGen (pre-Java 8):**
```
PermGen (Java 7 and earlier):
  - Fixed size, set with -XX:MaxPermSize
  - Caused infamous "java.lang.OutOfMemoryError: PermGen space"
  - Stored class metadata + interned strings + static fields

Metaspace (Java 8+):
  - Native memory (not heap) — grows automatically
  - No fixed limit by default (can consume all native memory!)
  - Always set -XX:MaxMetaspaceSize to prevent runaway growth
  - Interned strings moved to heap String pool
  - Static fields moved to heap
```

### Classloader Hierarchy and Leaks

```
Bootstrap ClassLoader (C++)
  └── Extension/Platform ClassLoader
        └── Application ClassLoader
              └── Custom ClassLoaders (web apps, OSGi, plugins)

Each ClassLoader has its own namespace.
Classes are unloaded ONLY when their ClassLoader is GC'd.
A ClassLoader is GC'd only when nothing references it.

Classloader leak pattern (common in app servers):
  1. Deploy web app → new ClassLoader created, loads 500 classes
  2. Undeploy web app → ClassLoader should be GC'd
  3. BUT: a static field in a library holds a reference to a class
     from the web app's ClassLoader → ClassLoader can't be GC'd
  4. Redeploy → another 500 classes loaded into new ClassLoader
  5. Repeat → Metaspace fills up → OOM: Metaspace
```

**Warning signs**: `java.lang.OutOfMemoryError: Metaspace`
- Classloader leak (common in app servers with hot-deploy)
- Too many dynamically generated classes (Groovy, CGLIB proxies)

**Diagnosing Metaspace issues:**
```bash
# Count loaded classes and classloaders:
jcmd <pid> VM.classloaders
jmap -clstats <pid>              # classloader statistics

# Monitor class loading:
-XX:+TraceClassLoading           # log every class load
-XX:+TraceClassUnloading         # log every class unload
# If classes load but never unload → classloader leak

# Heap dump analysis:
# In Eclipse MAT: Window → Heap Dump Details → Class Histogram
# Sort by "Retained Heap" — look for ClassLoader instances with large retained size
```

**Dynamic class generation — common sources:**
```
Framework          | What it generates
-------------------|------------------------------------------
Spring (CGLIB)     | Proxy subclasses for @Transactional, @Cacheable
Hibernate          | Proxy classes for lazy-loaded entities
Groovy/Kotlin      | Script classes, closures
Jackson            | Deserializer classes (cached, usually fine)
ByteBuddy/ASM      | Instrumentation agents, mocking frameworks
Reflection proxies | java.lang.reflect.Proxy for interfaces
```

---

## 8. Direct Memory & NIO

```bash
-XX:MaxDirectMemorySize=2g        # cap off-heap NIO buffers
                                  # default = -Xmx value
```

Used by: Netty, Kafka clients, NIO channels, mapped files.

**Warning signs**: `java.lang.OutOfMemoryError: Direct buffer memory`

### How Direct Memory Works

```
Normal ByteBuffer (heap):
  ByteBuffer buf = ByteBuffer.allocate(1024);
  - Allocated on Java heap
  - GC manages lifecycle
  - IO operations: Java heap → OS kernel buffer (extra copy)

Direct ByteBuffer (off-heap):
  ByteBuffer buf = ByteBuffer.allocateDirect(1024);
  - Allocated in native memory (outside Java heap)
  - JVM creates a Cleaner (PhantomReference) to free it when GC'd
  - IO operations: direct to OS kernel buffer (zero-copy)
  - Faster for IO, but GC doesn't control when it's freed
```

**The Direct Memory GC Problem:**
```
Direct ByteBuffers are freed when their Java wrapper object is GC'd.
But the wrapper object is tiny (on heap) — GC may not run frequently enough.

Scenario:
  - App allocates 100MB direct buffers in a loop
  - Wrapper objects are small → heap GC doesn't trigger
  - Direct memory fills up → OOM: Direct buffer memory
  - Heap is only 10% full!

Fix:
  1. Set -XX:MaxDirectMemorySize explicitly (triggers GC when limit hit)
  2. Call ((DirectBuffer) buf).cleaner().clean() explicitly
  3. Use Netty's PooledByteBufAllocator (reuses buffers, avoids allocation)
  4. Monitor with: ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
```

### Memory-Mapped Files

```java
// Map a file directly into process address space
FileChannel channel = FileChannel.open(path);
MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
// Reading buffer reads directly from OS page cache — no Java heap involved
// OS handles paging in/out from disk automatically
```

```
Benefits:
  - Zero-copy reads (OS page cache → process address space, no Java heap copy)
  - Shared between processes (multiple JVMs can map same file)
  - OS handles caching and prefetching

Risks:
  - Not tracked by -XX:MaxDirectMemorySize
  - Can't be explicitly freed (unmap is not exposed in Java API)
  - Large mappings can exhaust virtual address space
  - MappedByteBuffer holds file lock on Windows
```

### Monitoring Off-Heap Memory

```bash
# Native memory tracking (NMT) — comprehensive off-heap breakdown
-XX:NativeMemoryTracking=summary   # low overhead (~1%)
-XX:NativeMemoryTracking=detail    # full tracking (~5% overhead)

# Print report:
jcmd <pid> VM.native_memory summary
# Output:
# Total: reserved=4GB, committed=2GB
# - Java Heap: reserved=2GB, committed=2GB
# - Class: reserved=256MB, committed=50MB   ← Metaspace
# - Thread: reserved=200MB, committed=200MB ← thread stacks
# - Code: reserved=512MB, committed=100MB   ← JIT code cache
# - GC: reserved=100MB, committed=100MB
# - Internal: reserved=50MB, committed=50MB ← direct buffers, etc.
```

---

## 9. Enterprise Configuration Templates

### High-Throughput Batch Processing

```bash
java \
  -Xms8g -Xmx8g \
  -XX:+UseParallelGC \
  -XX:ParallelGCThreads=16 \
  -XX:GCTimeRatio=19 \           # 95% app time, 5% GC time
  -XX:MaxGCPauseMillis=500 \
  -XX:+UseCompressedOops \
  -jar batch-app.jar
```

**Why Parallel GC for batch:**
```
Parallel GC (also called Throughput GC) maximizes application throughput
by using all available CPU cores for GC, accepting longer but infrequent pauses.

GCTimeRatio=19 means: GC time / app time = 1/19 → GC uses ~5% of total time
The JVM adjusts heap size dynamically to meet this ratio.

Batch jobs don't care about pause times (no user waiting),
they care about finishing as fast as possible → Parallel GC wins.
```

### Low-Latency API Service (G1GC)

```bash
java \
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:G1HeapRegionSize=16m \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:G1MixedGCLiveThresholdPercent=85 \
  -XX:+UseStringDeduplication \
  -XX:+UseCompressedOops \
  -Xss512k \
  -XX:MetaspaceSize=256m \
  -XX:MaxMetaspaceSize=512m \
  -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m \
  -jar api-service.jar
```

**Tuning rationale:**
```
MaxGCPauseMillis=50:
  Aggressive target. G1 will reduce Young Gen size to meet this.
  Trade-off: more frequent GCs, lower throughput.

InitiatingHeapOccupancyPercent=35:
  Start concurrent marking earlier (default 45%).
  Gives more time to finish marking before heap fills up.
  Reduces risk of Full GC under sudden load spikes.

G1HeapRegionSize=16m:
  Humongous threshold = 8MB (50% of 16MB).
  Objects < 8MB go through normal Young Gen path.
  Tune based on your largest common allocations.
```

### Ultra-Low Latency Service (ZGC, Java 21)

```bash
java \
  -Xms16g -Xmx16g \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:SoftMaxHeapSize=14g \
  -XX:ZCollectionInterval=10 \
  -XX:+UseCompressedOops \
  -XX:MaxDirectMemorySize=2g \
  -XX:MetaspaceSize=256m \
  -XX:MaxMetaspaceSize=512m \
  -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m \
  -jar latency-sensitive-app.jar
```

**Tuning rationale:**
```
SoftMaxHeapSize=14g (heap is 16g):
  ZGC tries to keep heap usage below 14GB.
  The 2GB headroom is for concurrent relocation — ZGC needs free space
  to move objects while the app is running.
  Without headroom, ZGC may stall waiting for free regions.

ZCollectionInterval=10:
  Force a GC cycle every 10 seconds even if not needed.
  Prevents heap from growing large between GCs, keeping pauses short.
  Without this, ZGC might wait until heap is nearly full → longer GC cycle.

ZGenerational (Java 21):
  Enables generational ZGC. Young objects collected more frequently.
  Reduces CPU overhead by 30-50% vs non-generational ZGC.
  Always use this on Java 21+.
```

### Kubernetes / Container-Aware (Java 11+)

```bash
java \
  -XX:+UseContainerSupport \        # auto-detect container CPU/memory limits
  -XX:MaxRAMPercentage=75.0 \       # use 75% of container memory for heap
  -XX:InitialRAMPercentage=50.0 \   # start at 50%
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+ExitOnOutOfMemoryError \     # crash fast, let k8s restart
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/dumps/heap.hprof \
  -jar app.jar
```

**Container-specific pitfalls and fixes:**

```
Problem 1: JVM sees host CPU count, not container CPU limit
  Container: 2 CPU limit, Host: 32 cores
  JVM creates 23 GC threads (based on 32 cores) → CPU throttling
  Fix: -XX:+UseContainerSupport (Java 8u191+, Java 11+) auto-detects limits
       Or explicitly: -XX:ActiveProcessorCount=2

Problem 2: JVM sees host memory, not container memory limit
  Container: 2GB limit, Host: 64GB RAM
  JVM sets -Xmx = 25% of 64GB = 16GB → container OOMKilled immediately
  Fix: -XX:+UseContainerSupport + -XX:MaxRAMPercentage=75.0

Problem 3: Heap dump fills container ephemeral storage
  Fix: Mount /dumps as a persistent volume or use a sidecar to ship dumps

Problem 4: OOM crash leaves pod in CrashLoopBackOff without useful info
  Fix: -XX:+HeapDumpOnOutOfMemoryError + -XX:+ExitOnOutOfMemoryError
       Configure k8s to restart on exit code != 0 (default behavior)

Problem 5: Slow startup in containers (class loading from JAR)
  Fix: Use layered JARs (Spring Boot layered JAR) + CDS archive
       Exploded JAR layout is faster than reading from ZIP
```

### Spring Boot Production Template (Java 21)

```bash
java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heap.hprof \
  -XX:MetaspaceSize=128m \
  -XX:MaxMetaspaceSize=256m \
  -XX:ReservedCodeCacheSize=256m \
  -Xss256k \
  -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=3,filesize=10m \
  -Dspring.profiles.active=prod \
  -jar app.jar
```

---

## 10. Diagnosing GC Problems

### GC Overhead Too High
```
Symptom: CPU high, throughput low, frequent GC
Cause:   Heap too small, too many short-lived large objects
Fix:
  - Increase -Xmx
  - Increase -XX:G1HeapRegionSize (reduce humongous allocations)
  - Profile allocations with async-profiler
```

### Long STW Pauses
```
Symptom: p99 latency spikes, timeout errors
Cause:   Large Old Gen, too many live objects, Full GC
Fix:
  - Switch to ZGC or Shenandoah
  - Reduce -XX:InitiatingHeapOccupancyPercent (start GC earlier)
  - Reduce object promotion rate (tune survivor spaces)
```

### Metaspace OOM
```
Symptom: java.lang.OutOfMemoryError: Metaspace
Cause:   Classloader leak, excessive dynamic class generation
Fix:
  - Set -XX:MaxMetaspaceSize
  - Profile classloaders with jmap -clstats <pid>
  - Fix hot-deploy classloader leaks
```

### OutOfMemoryError: Java Heap Space
```
Symptom: OOM crash
Cause:   Memory leak, heap too small, large data in memory
Fix:
  - Enable -XX:+HeapDumpOnOutOfMemoryError
  - Analyze heap dump with Eclipse MAT or VisualVM
  - Look for dominator tree — largest retained objects
```

### Systematic GC Diagnosis Workflow

```
Step 1: Collect GC logs
  Enable -Xlog:gc*:file=gc.log:time,uptime
  Run under production load for at least 30 minutes

Step 2: Analyze with GCViewer or GCEasy (online)
  Key metrics:
    - Throughput % (should be > 95%)
    - Max pause time (should be < SLA)
    - Full GC count (should be 0)
    - Allocation rate (MB/s)
    - Promotion rate (MB/s) — high = premature promotion

Step 3: If allocation rate is high → profile allocations
  ./profiler.sh -e alloc -d 60 -f alloc.html <pid>
  Look for: unexpected large allocations, byte[] from serialization

Step 4: If Old Gen grows steadily → memory leak
  Take heap dump: jcmd <pid> GC.heap_dump /tmp/heap.hprof
  Analyze in Eclipse MAT:
    - Dominator tree: what's retaining the most memory?
    - Leak suspects report: objects accumulating over time

Step 5: If pauses are long despite low heap usage → safepoint issue
  Enable -Xlog:safepoint
  Look for "Reaching safepoint" > 5ms → long-running loops without safepoint checks
```

### Reading a Heap Dump with Eclipse MAT

```
Key views in Eclipse MAT:

1. Histogram: lists all classes by instance count and shallow heap
   - Shallow heap = memory of the object itself (not what it references)
   - Look for unexpected high counts (e.g., 10M String objects)

2. Dominator Tree: shows what's retaining the most memory
   - Retained heap = memory freed if this object were GC'd
   - Top entries = root causes of memory usage

3. Leak Suspects: automated analysis
   - MAT identifies objects that grew over time
   - Shows reference chain from GC root to the leaking objects

4. OQL (Object Query Language):
   SELECT * FROM java.util.HashMap WHERE size > 10000
   -- Find large maps that might be caches without eviction
```

### Common Memory Leak Patterns

```
Pattern 1: Static collection that grows unbounded
  static Map<String, Object> cache = new HashMap<>();
  // Never evicted → grows forever
  Fix: Use Caffeine/Guava cache with size/time eviction

Pattern 2: ThreadLocal not removed
  ThreadLocal<byte[]> buffer = new ThreadLocal<>();
  // Thread pool reuses threads → ThreadLocal values never GC'd
  Fix: Always call threadLocal.remove() in finally block

Pattern 3: Listener/callback not deregistered
  eventBus.register(this);  // holds reference to 'this'
  // Object can't be GC'd as long as eventBus is alive
  Fix: Always deregister in close()/destroy() methods

Pattern 4: Inner class holding outer class reference
  class Outer {
      class Inner implements Runnable { ... }
      // Inner holds implicit reference to Outer
      // If Inner is submitted to a long-lived executor → Outer leaks
  }
  Fix: Use static nested class or lambda carefully

Pattern 5: Interned strings accumulating
  String.intern() adds to String pool (heap, permanent until class unload)
  Fix: Avoid intern() for user-generated strings
```

---

## 11. JVM Diagnostic Tools

| Tool | Use Case | Command |
|------|----------|---------|
| `jstat` | Live GC stats | `jstat -gcutil <pid> 1000` |
| `jmap` | Heap dump / histogram | `jmap -dump:format=b,file=heap.hprof <pid>` |
| `jstack` | Thread dump | `jstack <pid>` |
| `jcmd` | All-in-one | `jcmd <pid> GC.heap_info` |
| `async-profiler` | CPU + allocation profiling | `./profiler.sh -e alloc -d 30 -f out.html <pid>` |
| `VisualVM` | GUI monitoring | Connect to JMX port |
| `GCViewer` | Analyze GC logs | Load gc.log file |
| `Eclipse MAT` | Heap dump analysis | Open heap.hprof |

### Key jstat Output

```bash
jstat -gcutil <pid> 1000
# S0    S1    E     O     M     CCS   YGC  YGCT  FGC  FGCT  GCT
# 0.00  45.2  72.3  35.1  95.2  92.1  142  3.421   2  0.812  4.233
#
# E  = Eden utilization %
# O  = Old Gen utilization %
# YGC = Young GC count  YGCT = Young GC time
# FGC = Full GC count   FGCT = Full GC time  ← should be near 0
```

**All jstat options:**
```bash
jstat -gc <pid> 1000        # raw bytes for all heap spaces
jstat -gcutil <pid> 1000    # percentages (easier to read)
jstat -gccause <pid> 1000   # includes last GC cause
jstat -gcnew <pid> 1000     # Young Gen details only
jstat -gcold <pid> 1000     # Old Gen details only
jstat -compiler <pid>       # JIT compilation stats
jstat -class <pid>          # class loading stats
```

### jcmd — The Swiss Army Knife

```bash
# List all running JVMs
jcmd

# List available commands for a JVM
jcmd <pid> help

# GC operations
jcmd <pid> GC.run                          # trigger GC
jcmd <pid> GC.heap_info                    # heap summary
jcmd <pid> GC.heap_dump /tmp/heap.hprof    # heap dump (safer than jmap)

# Thread operations
jcmd <pid> Thread.print                    # thread dump
jcmd <pid> Thread.print -l                 # with lock info

# JVM info
jcmd <pid> VM.flags                        # all JVM flags (including defaults)
jcmd <pid> VM.system_properties            # system properties
jcmd <pid> VM.native_memory summary        # native memory breakdown
jcmd <pid> VM.classloaders                 # classloader hierarchy

# Compiler
jcmd <pid> Compiler.codecache             # code cache usage
jcmd <pid> Compiler.queue                 # JIT compilation queue

# Diagnostics
jcmd <pid> VM.uptime                       # JVM uptime
jcmd <pid> VM.version                      # JVM version
```

### async-profiler — Production-Safe Profiling

```bash
# CPU profiling (find hot methods)
./profiler.sh -e cpu -d 30 -f cpu.html <pid>

# Allocation profiling (find what's creating GC pressure)
./profiler.sh -e alloc -d 30 -f alloc.html <pid>

# Wall-clock profiling (includes IO wait — good for latency analysis)
./profiler.sh -e wall -d 30 -f wall.html <pid>

# Lock contention profiling
./profiler.sh -e lock -d 30 -f lock.html <pid>

# Combined CPU + allocation flame graph
./profiler.sh start -e cpu,alloc <pid>
./profiler.sh stop -f combined.html <pid>
```

**Reading flame graphs:**
```
X-axis: alphabetical order (NOT time)
Y-axis: call stack depth (bottom = thread entry, top = where CPU is spent)
Width: proportion of samples (wider = more CPU time)

Look for:
  - Wide bars near the top → hot methods (optimization targets)
  - Unexpected methods in the stack → unnecessary work
  - GC-related frames → allocation pressure
```

### Thread Dump Analysis

```bash
jstack <pid> > thread-dump.txt
# or
jcmd <pid> Thread.print > thread-dump.txt
```

**What to look for:**
```
BLOCKED threads:
  "http-nio-8080-exec-1" BLOCKED on java.util.concurrent.locks.ReentrantLock
  → Lock contention — find who holds the lock and why

WAITING threads:
  "pool-1-thread-1" WAITING on java.util.concurrent.LinkedBlockingQueue
  → Normal for idle thread pool workers

TIMED_WAITING threads:
  "http-nio-8080-exec-5" TIMED_WAITING (sleeping)
  → Thread sleeping — check if intentional

Deadlock detection:
  jstack automatically detects and reports deadlocks at the bottom of output
  "Found one Java-level deadlock:"

Thread count analysis:
  grep "java.lang.Thread.State" thread-dump.txt | sort | uniq -c
  → Shows distribution of thread states
```

### JMX Monitoring Setup

```bash
# Enable JMX for remote monitoring (VisualVM, JConsole, Prometheus JMX exporter)
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9999 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Djava.rmi.server.hostname=<host-ip>

# For Prometheus scraping (use JMX exporter agent):
-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent.jar=8080:/opt/jmx_exporter/config.yaml
```

**Key JMX MBeans to monitor:**
```
java.lang:type=Memory
  → HeapMemoryUsage.used / max  (heap utilization %)
  → NonHeapMemoryUsage.used     (Metaspace + CodeCache)

java.lang:type=GarbageCollector,name=G1 Young Generation
  → CollectionCount  (YGC count)
  → CollectionTime   (total YGC time ms)

java.lang:type=GarbageCollector,name=G1 Old Generation
  → CollectionCount  (Full GC count — alert if > 0)

java.lang:type=Threading
  → ThreadCount      (current live threads)
  → PeakThreadCount  (max since JVM start)
  → DeadlockedThreads (alert if non-null)

java.lang:type=OperatingSystem
  → ProcessCpuLoad   (JVM CPU usage 0.0-1.0)
  → SystemCpuLoad    (total system CPU)
```

---

## 12. Quick Reference Cheat Sheet

```
Memory
  -Xms = -Xmx                    always equal in production
  -XX:MaxRAMPercentage=75.0       use in containers
  -XX:MaxMetaspaceSize=512m       always cap metaspace
  -Xss512k                        reduce for high thread count

GC Selection
  Batch / throughput  → -XX:+UseParallelGC
  General purpose     → -XX:+UseG1GC  (default)
  Low latency         → -XX:+UseZGC -XX:+ZGenerational  (Java 21)

G1 Tuning
  -XX:MaxGCPauseMillis=100
  -XX:InitiatingHeapOccupancyPercent=35
  -XX:G1HeapRegionSize=16m

Observability
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/dumps/
  -XX:+ExitOnOutOfMemoryError     (containers — fail fast)
  -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m

JIT
  -XX:+UseCompressedOops          always (heap < 32GB)
  -XX:ReservedCodeCacheSize=512m  increase if JIT deopt warnings
```

### Decision Tree: Which GC Should I Use?

```
Is this a batch/offline job?
  YES → UseParallelGC (maximize throughput, pauses don't matter)
  NO  ↓

Is Java 21+ available?
  YES → UseZGC + ZGenerational (best overall: low latency + good throughput)
  NO  ↓

Is heap > 8GB or p99 latency SLA < 50ms?
  YES → UseZGC (Java 15+) or UseShenandoahGC (Java 8+)
  NO  → UseG1GC (safe default, well-understood behavior)
```

### GC Pause Budget Calculator

```
Given: SLA p99 latency = 100ms, GC pause target = 10% of SLA

MaxGCPauseMillis = 100ms × 10% = 10ms

For G1GC with 10ms target:
  - Young Gen will be sized to collect in < 10ms
  - Fewer objects per GC → more frequent GCs
  - Ensure allocation rate doesn't exceed GC throughput

Rule: MaxGCPauseMillis should be ≤ 10% of your p99 latency SLA
```

### Flag Compatibility Matrix

| Flag | Serial | Parallel | G1 | ZGC | Shenandoah |
|------|--------|----------|----|-----|------------|
| `-XX:MaxGCPauseMillis` | ✗ | ✓ | ✓ | ✗ | ✗ |
| `-XX:GCTimeRatio` | ✓ | ✓ | ✗ | ✗ | ✗ |
| `-XX:G1HeapRegionSize` | ✗ | ✗ | ✓ | ✗ | ✗ |
| `-XX:+UseStringDeduplication` | ✗ | ✗ | ✓ | ✗ | ✗ |
| `-XX:SoftMaxHeapSize` | ✗ | ✗ | ✗ | ✓ | ✓ |
| `-XX:+ZGenerational` | ✗ | ✗ | ✗ | ✓ | ✗ |
| `-XX:+UseCompressedOops` | ✓ | ✓ | ✓ | ✓* | ✓ |

*ZGC uses colored pointers — CompressedOops works differently but is supported

### Common Flags That Are Defaults (Don't Need to Set)

```bash
# These are ON by default — only set if you need to change them:
-XX:+TieredCompilation            # default Java 8+
-XX:+UseCompressedOops            # default when heap < 32GB
-XX:+UseCompressedClassPointers   # default when CompressedOops is on
-XX:+DoEscapeAnalysis             # default Java 6+
-XX:+OptimizeStringConcat         # default
-XX:+ResizeTLAB                   # default
-XX:+UseContainerSupport          # default Java 8u191+, Java 11+
```

### Environment-Specific Recommendations

```
Development:
  -Xms256m -Xmx2g -XX:+UseG1GC
  (don't pre-allocate full heap — wastes RAM on dev machine)

Staging:
  Match production flags exactly
  Add: -XX:+PrintCompilation -Xlog:gc*
  (catch JIT and GC issues before prod)

Production:
  -Xms = -Xmx (pre-allocate)
  -XX:+ExitOnOutOfMemoryError
  -XX:+HeapDumpOnOutOfMemoryError
  Always have GC logging enabled

Kubernetes:
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  Never hardcode -Xmx (breaks when pod memory limit changes)
```
