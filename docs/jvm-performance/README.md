# JVM Performance & Fine-Tuning Guide

A comprehensive deep-dive into JVM internals, garbage collection, memory management, and production-grade performance tuning.

## 📚 Table of Contents

| # | Topic | Description |
|---|-------|-------------|
| 1 | [JVM Architecture & Memory Model](01_JVM_Architecture.md) | JVM internals, memory areas, class loading, JIT compilation |
| 2 | [Garbage Collection Deep Dive](02_Garbage_Collection.md) | GC algorithms, collectors (G1, ZGC, Shenandoah), tuning strategies |
| 3 | [Memory Management & Tuning](03_Memory_Management.md) | Heap sizing, metaspace, off-heap, memory leaks detection |
| 4 | [JIT Compilation & Code Optimization](04_JIT_Compilation.md) | C1/C2 compilers, tiered compilation, inlining, escape analysis |
| 5 | [Thread Performance & Concurrency](05_Thread_Performance.md) | Thread pools, virtual threads, lock contention, false sharing |
| 6 | [Profiling & Monitoring Tools](06_Profiling_Tools.md) | JFR, async-profiler, VisualVM, JMX, flame graphs |
| 7 | [Production Tuning Recipes](07_Production_Tuning.md) | Real-world tuning scenarios, checklists, flags reference |
| 8 | [Troubleshooting & Diagnostics](08_Troubleshooting.md) | OOM, high CPU, deadlocks, GC storms, heap dump analysis |

## 🎯 Who Is This For?

- Backend engineers optimizing Java microservices
- DevOps/SRE teams tuning JVM in production
- Developers preparing for system design or performance interviews
- Anyone running JVM workloads at scale (Kafka, Elasticsearch, Spark, etc.)

## 🔑 Key JVM Flags Quick Reference

```bash
# Memory
-Xms4g -Xmx4g                    # Heap size (min = max to avoid resize)
-XX:MetaspaceSize=256m            # Initial metaspace
-XX:MaxMetaspaceSize=512m         # Max metaspace

# GC Selection
-XX:+UseG1GC                      # G1 (default Java 9+)
-XX:+UseZGC                       # ZGC (low latency, Java 15+)
-XX:+UseShenandoahGC              # Shenandoah (low latency)

# GC Tuning
-XX:MaxGCPauseMillis=200          # G1 target pause
-XX:G1HeapRegionSize=16m          # G1 region size
-XX:InitiatingHeapOccupancyPercent=45

# Diagnostics
-XX:+HeapDumpOnOutOfMemoryError   # Auto heap dump on OOM
-XX:HeapDumpPath=/tmp/heapdump.hprof
-Xlog:gc*:file=gc.log:time,level,tags

# JIT
-XX:+TieredCompilation            # Enable tiered (default)
-XX:ReservedCodeCacheSize=256m    # Code cache size
```

## 📊 Decision Matrix: Which GC to Use?

| Requirement | Recommended GC | Java Version |
|------------|---------------|--------------|
| General purpose / Balanced | G1GC | 9+ (default) |
| Ultra-low latency (<1ms) | ZGC | 15+ |
| Low latency + RedHat | Shenandoah | 12+ |
| Maximum throughput (batch) | Parallel GC | Any |
| Small heap (<256MB) | Serial GC | Any |
| Containers with tight memory | ZGC or G1 | 15+ |

## 🚀 Quick Start: Profile Your Application

```bash
# 1. Enable GC logging
java -Xlog:gc*:file=gc.log:time,level,tags -jar app.jar

# 2. Capture flight recording (zero overhead in production)
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr -jar app.jar

# 3. Async profiler (CPU + allocation profiling)
./asprof -d 30 -f profile.html <pid>

# 4. Quick heap analysis
jmap -histo:live <pid> | head -30

# 5. Thread dump for deadlock/contention
jstack -l <pid> > thread_dump.txt
```
