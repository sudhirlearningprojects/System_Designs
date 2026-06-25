# Troubleshooting & Diagnostics

## 1. Troubleshooting Decision Tree

```
                    ┌─────────────────────────────┐
                    │  What's the problem?         │
                    └──────────────┬──────────────┘
           ┌──────────────────────┼───────────────────────┐
           ▼                      ▼                       ▼
   ┌───────────────┐    ┌────────────────┐    ┌──────────────────┐
   │ High Latency  │    │  High CPU      │    │ High Memory/OOM  │
   │ / Slow        │    │                │    │                  │
   └───────┬───────┘    └───────┬────────┘    └────────┬─────────┘
           │                    │                      │
     ┌─────┴─────┐       ┌─────┴─────┐          ┌─────┴─────┐
     ▼           ▼       ▼           ▼          ▼           ▼
  GC Pauses?  I/O Wait? Hot Code?  GC Thrash?  Heap Leak? Native Leak?
  → GC logs   → JFR    → Profile  → GC logs   → HeapDump  → NMT
  → tune GC   → async  → optimize → tune GC   → MAT       → pmap
              → traces                          → track     → jemalloc
```

## 2. OutOfMemoryError Diagnosis

### 2.1 Java Heap Space

```bash
# Error: java.lang.OutOfMemoryError: Java heap space

# Immediate actions:
# 1. Heap dump should auto-capture (if configured)
ls -la /var/log/java/heapdump*.hprof

# 2. If no auto-dump, capture from another instance
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# 3. Quick histogram (which objects consume the most)
jmap -histo:live <pid> | head -30
```

**Analysis workflow:**
```
Step 1: Is it a leak or insufficient heap?
  → Check: Is live data (after Full GC) growing over time?
  → jstat -gcutil <pid> → watch O% after each Full GC
  → Growing = leak, Stable but high = need more heap

Step 2: If leak, find the retaining path:
  → Open heap dump in MAT
  → Leak Suspects Report → identifies top retained objects
  → Dominator Tree → sort by retained size
  → Path to GC Roots (exclude weak/phantom refs)

Step 3: Common culprits:
  → HashMap/ConcurrentHashMap growing unbounded
  → ArrayList/LinkedList accumulating entries
  → byte[] from unclosed streams/buffers
  → String[] from log message accumulation
  → Session objects never expiring
```

### 2.2 Metaspace

```bash
# Error: java.lang.OutOfMemoryError: Metaspace

# Diagnosis:
jstat -gcmetacapacity <pid>
# MC    MU     CCSC  CCSU
# 256   254    32    31     ← Metaspace nearly full!

# Count loaded classes
jcmd <pid> GC.class_stats | wc -l

# Check ClassLoader instances
jcmd <pid> VM.classloaders

# Common causes:
# 1. Hot-deploy in app servers (Tomcat redeploy without restart)
# 2. Dynamic proxy generation (Spring AOP, Hibernate)
# 3. Groovy/scripting engine compiling classes
# 4. CGLIB/ByteBuddy generating classes

# Fixes:
-XX:MaxMetaspaceSize=512m          # Prevent unbounded growth
# Fix the classloader leak at source
# For app servers: restart instead of redeploy
# For Hibernate: limit proxy caching
```

### 2.3 GC Overhead Limit Exceeded

```bash
# Error: java.lang.OutOfMemoryError: GC overhead limit exceeded
# Meaning: JVM spent >98% of time in GC, recovered <2% heap

# This means: the application has almost no usable heap left
# Usually precedes "Java heap space" OOM

# Diagnosis:
# 1. Check GC log for Full GC reclaiming very little
grep "Pause Full" gc.log | tail -5
# [Pause Full (Allocation Failure) 3890M->3850M(4096M) 8.234s]
#                                   ↑ Only freed 40MB out of 4GB!

# 2. Either increase heap or fix memory leak
```

### 2.4 Unable to Create Native Thread

```bash
# Error: java.lang.OutOfMemoryError: unable to create native thread

# Diagnosis:
# Check thread count
jcmd <pid> Thread.print | grep -c "\"" 
# Or: ls /proc/<pid>/task | wc -l

# Check OS limits
ulimit -u         # Max user processes
cat /proc/sys/kernel/threads-max
cat /proc/<pid>/limits | grep "Max processes"

# Fixes:
# 1. Reduce thread count (bounded pools, fewer connections)
# 2. Reduce stack size: -Xss256k
# 3. Increase OS limits:
ulimit -u 65535
echo 100000 > /proc/sys/kernel/threads-max

# 4. Calculate: Available = (Total RAM - Heap - Metaspace) / Xss
# 8GB RAM, 4GB heap, 256MB meta, 512k stack = (8G-4G-256M) / 512K ≈ 7500 threads
```

### 2.5 Direct Buffer Memory

```bash
# Error: java.lang.OutOfMemoryError: Direct buffer memory

# Cause: Too many direct ByteBuffers allocated (Netty, NIO)

# Check direct buffer usage:
jcmd <pid> VM.native_memory summary | grep -A2 "Internal"

# Fixes:
-XX:MaxDirectMemorySize=2g          # Increase limit

# If Netty: tune buffer pool
# -Dio.netty.maxDirectMemory=2147483648
# -Dio.netty.allocator.maxOrder=9   # Reduce chunk size

# Ensure direct buffers are released:
# - Use try-with-resources for channels
# - Call buffer.release() in Netty handlers
# - Invoke System.gc() (direct buffers freed via Cleaner in GC)
```

## 3. High CPU Troubleshooting

### 3.1 Identify the Hot Thread

```bash
# Step 1: Find Java process consuming CPU
top -p <java-pid> -H   # Show threads, sorted by CPU
# Or:
ps -eLo pid,tid,pcpu,comm | grep java | sort -k3 -rn | head -10

# Step 2: Convert thread ID to hex (for thread dump matching)
printf "0x%x\n" <tid>    # e.g., tid=12345 → 0x3039

# Step 3: Take thread dump
jstack <pid> > threads.txt

# Step 4: Find the hot thread in dump
grep -A 30 "nid=0x3039" threads.txt
# Shows the stack trace of the CPU-consuming thread
```

### 3.2 Automated Script

```bash
#!/bin/bash
# hot-threads.sh - Find CPU-hungry Java threads
PID=$1
echo "Top 5 CPU threads for PID $PID:"
echo "================================"

# Get top threads
ps -eLo pid,tid,%cpu --sort=-%cpu | grep "^ *$PID" | head -5 | while read p t cpu; do
    HEX=$(printf "0x%x" $t)
    echo ""
    echo "Thread $t (nid=$HEX) - CPU: $cpu%"
    echo "Stack trace:"
    jstack $PID | grep -A 20 "nid=$HEX " | head -25
done
```

### 3.3 CPU Profiling

```bash
# Quick: async-profiler CPU profile
./asprof -d 30 -f cpu.html <pid>
# Open cpu.html → flame graph shows where CPU time is spent

# JFR CPU profiling
jcmd <pid> JFR.start name=cpu duration=30s settings=profile filename=cpu.jfr
# Analyze in JMC: Method Profiling → Hot Methods

# Common CPU hogs:
# - Regex compilation in loops: precompile Pattern
# - XML/JSON parsing: use streaming parser
# - Reflection: cache Method/Field references
# - Logging: check level before expensive string formatting
# - Hash computation: cache hash codes
# - Spinning on CAS in high contention: use LongAdder
```

### 3.4 GC Causing High CPU

```bash
# Symptoms: CPU spikes correlate with GC pauses
# Check: Is GC eating the CPU?

jstat -gcutil <pid> 1000
# If YGC/FGC count increasing rapidly → GC is the CPU hog

# Verify in GC log:
# Many concurrent marking threads running = expected CPU usage
# Many Full GC back-to-back = trouble (heap exhaustion)

# Fix: See GC tuning recipes (Chapter 7)
```

## 4. Deadlock Detection

### 4.1 Detecting Deadlocks

```bash
# jstack automatically detects deadlocks
jstack -l <pid>
# Look for: "Found one Java-level deadlock:"

# Programmatic detection
jcmd <pid> Thread.print
# Or via JMX:
ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
long[] deadlocked = tmx.findDeadlockedThreads();
```

### 4.2 Example Deadlock Output

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f2a8c003d80 (object 0x00000000e0f1a5b0, a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f2a8c003e40 (object 0x00000000e0f1a5a0, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
"Thread-1":
    at com.example.Service.transferA(Service.java:25)
    - waiting to lock <0x00000000e0f1a5b0>
    - locked <0x00000000e0f1a5a0>
"Thread-2":
    at com.example.Service.transferB(Service.java:35)
    - waiting to lock <0x00000000e0f1a5a0>
    - locked <0x00000000e0f1a5b0>
```

### 4.3 Deadlock Prevention Strategies

```java
// Strategy 1: Consistent lock ordering
// Always acquire locks in same global order (e.g., by ID)
public void transfer(Account from, Account to, double amount) {
    Account first = from.getId() < to.getId() ? from : to;
    Account second = from.getId() < to.getId() ? to : from;
    
    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// Strategy 2: tryLock with timeout
public boolean transfer(Account from, Account to, double amount) {
    boolean fromLocked = false, toLocked = false;
    try {
        fromLocked = from.getLock().tryLock(1, TimeUnit.SECONDS);
        toLocked = to.getLock().tryLock(1, TimeUnit.SECONDS);
        if (fromLocked && toLocked) {
            from.debit(amount);
            to.credit(amount);
            return true;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (fromLocked) from.getLock().unlock();
        if (toLocked) to.getLock().unlock();
    }
    return false; // Retry later
}

// Strategy 3: Use concurrent data structures instead of locks
// ConcurrentHashMap, AtomicReference, etc.
```

## 5. GC Storm (Continuous GC)

### 5.1 Identifying GC Storm

```bash
# Symptoms:
# - Application nearly unresponsive
# - GC running back-to-back with minimal recovery
# - CPU at 100% on GC threads

# In GC log:
# [Pause Full (Allocation Failure) 3900M->3850M(4096M) 12.5s]
# [Pause Full (Allocation Failure) 3880M->3860M(4096M) 14.2s]
# [Pause Full (Allocation Failure) 3890M->3870M(4096M) 15.8s]
# → Each Full GC recovers less than 100MB

# jstat shows:
# O% stays >95% after every GC
```

### 5.2 Emergency Response

```bash
# If the application is in GC storm:

# 1. Capture heap dump immediately (if possible)
jcmd <pid> GC.heap_dump /tmp/emergency_heap.hprof

# 2. If dump fails (OOM), try jmap with -F (force)
jmap -F -dump:format=b,file=/tmp/emergency_heap.hprof <pid>

# 3. Restart the application (graceful)
kill -SIGTERM <pid>
# Container will restart automatically

# 4. If unresponsive to SIGTERM
kill -9 <pid>

# 5. Post-mortem: analyze heap dump for root cause
```

### 5.3 Preventing GC Storms

```bash
# 1. Right-size heap (live data should be <50% of heap)
# 2. Monitor Old Gen after GC — alert at 75%
# 3. Enable ExitOnOutOfMemoryError (fast restart better than thrashing)
-XX:+ExitOnOutOfMemoryError

# 4. Set GC overhead action
-XX:GCHeapFreeLimit=5              # Fail if <5% freed (triggers OOM)
-XX:GCTimeLimit=98                 # Fail if >98% time in GC

# 5. Circuit breaker pattern in application
# Shed load when heap > 85%
```

## 6. Thread Leak Diagnosis

### 6.1 Detecting Thread Leak

```bash
# Thread count keeps growing
watch -n 5 "jcmd <pid> Thread.print | grep -c '\"'"

# Or via JMX
# jvm_threads_live keeps increasing without plateau

# Quick check
jcmd <pid> Thread.print | grep "\"" | sort | uniq -c | sort -rn | head -20
# Shows thread name patterns and counts
```

### 6.2 Common Thread Leak Sources

```java
// 1. Unbounded thread pool (Executors.newCachedThreadPool)
// BAD:
ExecutorService pool = Executors.newCachedThreadPool();
// Under load, creates unlimited threads!
// FIX:
ExecutorService pool = new ThreadPoolExecutor(10, 50, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000));

// 2. Timer/ScheduledExecutor not shut down
// BAD:
public void handleRequest() {
    Timer timer = new Timer();  // New timer = new thread per request!
    timer.schedule(task, 5000);
}
// FIX: Use shared scheduler
private final ScheduledExecutorService scheduler = 
    Executors.newScheduledThreadPool(2);

// 3. HTTP connections creating threads (e.g., WebSocket not closed)
// FIX: Always close connections, implement connection timeout

// 4. Daemon threads created in library code
// FIX: Set reasonable maxPoolSize, use shared executors
```

## 7. High Allocation Rate Troubleshooting

### 7.1 Measuring Allocation Rate

```bash
# From jstat: bytes allocated between measurements
jstat -gc <pid> 1000

# Calculate: (EC × EU_change + S0C×S0U_change) / time_interval
# Or from GC log: delta of "Eden: X->Y" between Young GCs / time

# Target: < 1 GB/s allocation rate for most applications
# Concerning: > 2 GB/s
# Critical: > 5 GB/s (causes frequent GC)
```

### 7.2 Finding Allocation Hotspots

```bash
# async-profiler allocation profiling
./asprof -d 60 -e alloc -f alloc_profile.html <pid>

# JFR allocation sampling
jcmd <pid> JFR.start name=alloc duration=60s
# In JMC: Memory → Allocations tab

# Top allocation patterns to fix:
```

```java
// Pattern 1: String concatenation in loops
// BAD: Creates N StringBuilder + N String objects
for (Order order : orders) {
    String line = order.getId() + "," + order.getAmount() + "\n"; // Allocates!
    writer.write(line);
}
// FIX:
StringBuilder sb = new StringBuilder(64);
for (Order order : orders) {
    sb.setLength(0);
    sb.append(order.getId()).append(',').append(order.getAmount()).append('\n');
    writer.write(sb.toString()); // Or writer.append(sb)
}

// Pattern 2: Autoboxing
// BAD: Integer.valueOf() on every iteration
Map<Integer, Integer> counts = new HashMap<>();
for (int id : ids) {
    counts.merge(id, 1, Integer::sum); // Autoboxes id and 1
}
// FIX: Use primitive map (Eclipse Collections, Koloboke)
IntIntHashMap counts = new IntIntHashMap();

// Pattern 3: Lambda / closure capturing variables
// BAD: Each iteration creates a new lambda instance
list.forEach(item -> processor.process(item, context)); // Captures 'context'
// FIX: Use method reference if possible, or move out of hot loop

// Pattern 4: Stream API in hot paths
// BAD: Creates Stream, Spliterator, intermediate objects
list.stream().filter(x -> x > 0).map(x -> x * 2).collect(Collectors.toList());
// FIX: Plain loop in allocation-sensitive code
List<Integer> result = new ArrayList<>(list.size());
for (int x : list) {
    if (x > 0) result.add(x * 2);
}

// Pattern 5: Date/Time formatting
// BAD: Creates DateTimeFormatter each time
String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
// FIX: Static formatter
private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
String formatted = LocalDateTime.now().format(FMT);
```

## 8. Latency Spike Investigation

### 8.1 Systematic Approach

```bash
# Step 1: Correlate with GC
# Check if latency spikes coincide with GC pauses
grep "Pause" gc.log  # Get GC pause timestamps
# Compare with application latency log timestamps

# Step 2: If NOT GC, check thread state during spike
# Take 3 thread dumps during the spike (5s apart)
for i in 1 2 3; do jcmd <pid> Thread.print > dump_$i.txt; sleep 5; done

# Step 3: Look for blocked threads
grep -c "BLOCKED" dump_*.txt        # Lock contention
grep -c "TIMED_WAITING" dump_*.txt  # Waiting on I/O or sleep

# Step 4: Check OS-level causes
vmstat 1 10       # Look for: high 'wa' (I/O wait), high 'si' (swap in)
iostat -x 1 10    # Disk I/O saturation (%util > 90%)
sar -n DEV 1 10   # Network saturation

# Step 5: Check for safepoint pauses (non-GC STW)
-Xlog:safepoint*:file=safepoint.log:time,level
# Safepoints: JIT deopt, biased lock revocation, thread dump
```

### 8.2 Common Non-GC Latency Spikes

| Cause | Evidence | Fix |
|-------|----------|-----|
| DNS resolution | Threads in `InetAddress.getByName` | Cache DNS, reduce TTL |
| Connection pool exhaustion | Threads waiting on `getConnection` | Increase pool, add timeout |
| Lock contention | Many threads BLOCKED on same monitor | Fine-grained locking |
| OS page swap | High `si` in vmstat | Disable swap, increase RAM |
| Disk I/O | High `await` in iostat | SSD, reduce logging |
| Network timeout | Threads in `SocketInputStream.read` | Reduce timeouts |
| Transparent Huge Pages | Latency spikes at page allocation | Disable THP |
| JIT compilation | Short spikes during warmup | Pre-warmup, tiered |

### 8.3 Safepoint Pauses

```bash
# Non-GC stop-the-world events
-Xlog:safepoint*:file=safepoint.log:time,level,tags

# Common safepoint triggers:
# - Biased lock revocation (disable biased locking)
# - Thread dump (jstack)
# - Class redefinition (hot-deploy)
# - Code deoptimization
# - JNI critical region exit

# Reduce safepoint impact:
-XX:+UseCountedLoopSafepoints      # Safepoint in counted loops
-XX:GuaranteedSafepointInterval=0  # Disable periodic safepoints (risky!)
-XX:-UseBiasedLocking              # Avoid bias revocation pauses
```

## 9. Container-Specific Issues

### 9.1 OOM Killed by Kubernetes

```bash
# Symptom: Pod restarted with reason "OOMKilled"
# But Java says heap was fine!

# Root cause: Total RSS exceeded container memory limit
# RSS = Heap + Metaspace + Threads + CodeCache + DirectBuffers + Native

# Diagnosis:
# Check what the JVM thinks is available
-XX:+UseContainerSupport   # Should be ON
jcmd <pid> VM.native_memory summary  # Shows all memory areas

# Fix: Ensure total fits within container
# Container: 4GB
# -XX:MaxRAMPercentage=70.0  → Heap = 2.8GB
# Metaspace: 256MB
# 200 threads × 512KB = 100MB
# CodeCache: 240MB
# Direct: 256MB
# GC + internal: 200MB
# Total ≈ 3.8GB < 4GB ✓
```

### 9.2 CPU Throttling

```bash
# Symptom: Latency spikes every ~100ms, CPU usage shows < limit
# Cause: CFS scheduler throttles after quota exhausted

# Check throttling:
cat /sys/fs/cgroup/cpu/cpu.stat
# nr_throttled: 12345    ← Throttling happening!
# throttled_time: 5000000000  ← 5 seconds throttled

# JVM impact: GC threads throttled → longer pauses
# Fix:
# 1. Increase CPU limit (or remove limit, keep request)
# 2. Reduce GC thread count to fit within limit
-XX:ParallelGCThreads=4
-XX:ConcGCThreads=2

# 3. Use cpu.cfs_period_us wisely
# Default: 100ms period, quota = cores × 100ms
# With 2 cores: 200ms quota per 100ms period
# GC using 8 threads exceeds this instantly → throttled
```

### 9.3 Docker / cgroup v2 Compatibility

```bash
# Java 17+ fully supports cgroup v2
# Java 11-16: may need explicit flags
-XX:+UseContainerSupport

# Verify JVM sees correct limits:
jcmd <pid> VM.info | grep -i "container\|cgroup\|memory"

# If JVM sees wrong memory:
# Force specific memory recognition
-XX:MaxRAMPercentage=75.0       # Percentage-based (recommended)
# Or explicit:
-Xmx3g                         # If container is 4GB
```

## 10. Emergency Playbook

### 10.1 Application Unresponsive

```bash
# Step 1: Is it alive?
kill -0 <pid>           # Check process exists

# Step 2: Is it in GC storm?
jstat -gcutil <pid>     # If O% > 95% → GC storm
# If yes: capture heap dump + restart

# Step 3: Is it deadlocked?
jstack <pid>            # Check for deadlock message
# If yes: identify locks, plan fix, restart

# Step 4: Is it blocked on I/O?
jstack <pid> | grep -c "BLOCKED\|TIMED_WAITING"
# If many: check downstream dependencies (DB, external APIs)

# Step 5: Is it a native hang?
jstack -F <pid>         # Force mode (attaches via ptrace)
# If this hangs too: check with strace or gdb
strace -p <pid> -f -e trace=all 2>&1 | head -50
```

### 10.2 Sudden Memory Spike

```bash
# Step 1: Quick heap check
jcmd <pid> GC.heap_info

# Step 2: Object histogram (instant, no dump needed)
jcmd <pid> GC.class_histogram | head -20
# Look for unusually large counts or byte[]

# Step 3: Trigger GC to see how much is reclaimable
jcmd <pid> GC.run
jcmd <pid> GC.heap_info   # Compare before/after

# Step 4: If not reclaimable, capture heap dump
jcmd <pid> GC.heap_dump /tmp/heap.hprof

# Step 5: For native memory growth
jcmd <pid> VM.native_memory summary.diff  # (if NMT baseline was set)
```

### 10.3 Slow Startup

```bash
# Profile startup
java -Xlog:class+load:file=classload.log:time -jar app.jar
# Count classes: wc -l classload.log

# Check JIT compilation queue
-XX:+PrintCompilation

# Quick fixes:
-XX:TieredStopAtLevel=1          # C1 only (50% faster startup)
-XX:+UseParallelGC               # Parallel GC has less startup overhead
-Xverify:none                    # Skip bytecode verification (risky!)

# Best fix: Application CDS
java -XX:ArchiveClassesAtExit=app.jsa -jar app.jar  # Record
java -XX:SharedArchiveFile=app.jsa -jar app.jar     # Replay (30-50% faster)
```

## 11. Diagnostic Command Cheat Sheet

```bash
# ═══════════════════════════════════════════════════
# ESSENTIAL DIAGNOSTIC COMMANDS
# ═══════════════════════════════════════════════════

# Process info
jcmd -l                           # List Java processes
jcmd <pid> VM.version             # JVM version
jcmd <pid> VM.flags               # Active flags
jcmd <pid> VM.uptime              # Uptime

# Memory
jcmd <pid> GC.heap_info           # Heap layout
jcmd <pid> GC.class_histogram     # Object histogram
jcmd <pid> GC.heap_dump /tmp/h.hprof  # Heap dump
jcmd <pid> VM.native_memory summary    # Native memory

# Threads  
jcmd <pid> Thread.print           # Thread dump
jstack -l <pid>                   # Thread dump (with locks)

# GC
jstat -gcutil <pid> 1000 10       # GC utilization every 1s
jstat -gccause <pid> 1000         # GC with cause
jcmd <pid> GC.run                 # Force GC

# JIT
jcmd <pid> Compiler.codecache     # Code cache usage
jcmd <pid> Compiler.queue         # Compilation queue

# JFR
jcmd <pid> JFR.start name=diag duration=60s filename=/tmp/diag.jfr
jcmd <pid> JFR.dump name=diag filename=/tmp/diag.jfr
jcmd <pid> JFR.stop name=diag

# OS-level
top -Hp <pid>                     # Thread CPU usage
pmap -x <pid> | sort -k3 -rn | head -20  # Memory mappings
strace -fp <pid> -e trace=all 2>&1 | head -100  # System calls
cat /proc/<pid>/status            # Process status (VmRSS, Threads)
cat /proc/<pid>/limits            # Resource limits
```
