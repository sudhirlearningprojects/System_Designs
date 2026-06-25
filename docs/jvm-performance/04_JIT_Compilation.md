# JIT Compilation & Code Optimization

## 1. JIT Compilation Overview

### 1.1 Interpretation → Compilation Pipeline

```
┌──────────────────────────────────────────────────────────────────┐
│                    Code Execution Lifecycle                        │
│                                                                   │
│  .java → javac → .class (bytecode) → JVM Execution              │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    JVM Runtime                                │ │
│  │                                                              │ │
│  │  Method first called:                                        │ │
│  │  ┌─────────────┐                                           │ │
│  │  │ Interpreter │  ← Slow but starts immediately             │ │
│  │  └──────┬──────┘                                           │ │
│  │         │ (method invoked N times / loop iterates N times)  │ │
│  │         │ (profile data collected)                          │ │
│  │         ▼                                                    │ │
│  │  ┌─────────────┐                                           │ │
│  │  │ C1 Compiler │  ← Quick compile, basic optimizations      │ │
│  │  │  (Client)   │     3-5x faster than interpreter           │ │
│  │  └──────┬──────┘                                           │ │
│  │         │ (more invocations, richer profile data)           │ │
│  │         ▼                                                    │ │
│  │  ┌─────────────┐                                           │ │
│  │  │ C2 Compiler │  ← Heavy optimization, peak performance    │ │
│  │  │  (Server)   │     10-30x faster than interpreter         │ │
│  │  └─────────────┘                                           │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 1.2 Tiered Compilation Levels

| Level | Name | Description |
|-------|------|-------------|
| 0 | Interpreter | Bytecode interpretation, collects basic profiles |
| 1 | C1 (simple) | Full speed C1, no profiling |
| 2 | C1 (limited profile) | C1 with invocation counters |
| 3 | C1 (full profile) | C1 with full profiling data for C2 |
| 4 | C2 (optimized) | Maximum optimization using profile data |

Normal flow: 0 → 3 → 4 (interpreter → C1 with profiling → C2)

```bash
# Tiered compilation (default since Java 8)
-XX:+TieredCompilation                    # On by default
-XX:TieredStopAtLevel=1                  # Stop at C1 (fast startup)
-XX:TieredStopAtLevel=4                  # Normal (peak performance)
-XX:-TieredCompilation                    # Disable: interpreter→C2 only
```

### 1.3 Compilation Thresholds

```bash
# Default thresholds for compilation triggers
-XX:CompileThreshold=10000               # Method invocation count (non-tiered)
-XX:Tier3InvocationThreshold=200         # Tier 0 → Tier 3 threshold
-XX:Tier4InvocationThreshold=5000        # Tier 3 → Tier 4 threshold

# Back-edge counter: loops trigger OSR (On-Stack Replacement)
-XX:OnStackReplacePercentage=140         # OSR threshold multiplier
```

## 2. Key JIT Optimizations

### 2.1 Method Inlining

The single most impactful optimization. Replaces method call with method body.

```java
// Before inlining:
public int compute(int x) {
    return square(x) + cube(x);
}
private int square(int n) { return n * n; }
private int cube(int n) { return n * n * n; }

// After inlining:
public int compute(int x) {
    return (x * x) + (x * x * x);  // No call overhead, enables more opts
}
```

```bash
# Inlining parameters
-XX:MaxInlineSize=35                 # Max bytecode size for always-inline
-XX:FreqInlineSize=325               # Max size for hot methods
-XX:InlineSmallCode=2000             # Max compiled size to inline
-XX:MaxInlineLevel=15                # Max call chain depth for inlining

# Diagnostic: see what gets inlined
-XX:+PrintInlining                   # Log inlining decisions
-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
```

**Why It Matters:**
- Eliminates call/return overhead
- Enables subsequent optimizations (constant folding, dead code elimination)
- Megamorphic calls (>2 receiver types) block inlining

### 2.2 Escape Analysis

Determines if an object "escapes" the allocating method/thread.

```java
// Case 1: NoEscape — object never leaves the method
public int sum() {
    Point p = new Point(3, 4);    // ← JIT eliminates this allocation!
    return p.x + p.y;            // Scalar replacement: just uses 3 + 4
}

// Case 2: ArgEscape — passed to method but doesn't escape thread
public void process() {
    List<String> list = new ArrayList<>();
    list.add("hello");
    helper(list);                  // list escapes to helper, but not thread
    // → Lock elision: synchronized blocks on 'list' can be removed
}

// Case 3: GlobalEscape — stored in static field or passed to other thread
public void leaks() {
    Object o = new Object();
    sharedField = o;              // ← Cannot optimize, object escapes
}
```

**Optimizations Enabled by Escape Analysis:**

| Escape Type | Optimization | Effect |
|------------|--------------|--------|
| NoEscape | Scalar Replacement | Allocation eliminated entirely |
| NoEscape | Stack Allocation | Object on stack (no GC needed) |
| ArgEscape | Lock Elision | Remove unnecessary synchronization |
| NoEscape | Allocation Elimination | Object removed from heap |

```bash
-XX:+DoEscapeAnalysis            # Default ON
-XX:+EliminateAllocations        # Scalar replacement (default ON)
-XX:+EliminateLocks              # Lock elision (default ON)
```

### 2.3 Loop Optimizations

```java
// Loop Unrolling: reduce branch overhead
// Before:
for (int i = 0; i < 1000; i++) {
    sum += array[i];
}

// After JIT unrolling (conceptual):
for (int i = 0; i < 1000; i += 4) {
    sum += array[i] + array[i+1] + array[i+2] + array[i+3];
}

// Loop-Invariant Code Motion: hoist constant expressions
// Before:
for (int i = 0; i < n; i++) {
    result[i] = array[i] * (x * y);  // x*y computed every iteration
}

// After LICM:
int temp = x * y;                     // Hoisted out of loop
for (int i = 0; i < n; i++) {
    result[i] = array[i] * temp;
}
```

```bash
# Loop optimization flags
-XX:LoopUnrollLimit=60           # Max iterations to unroll
-XX:+UseLoopPredicate            # Loop predicate optimization
```

### 2.4 Devirtualization (Speculative Optimization)

```java
// Virtual dispatch normally requires vtable lookup
interface Shape { int area(); }
class Circle implements Shape { int area() { return (int)(Math.PI * r * r); } }
class Square implements Shape { int area() { return side * side; } }

// If profiling shows 99% of calls are Circle:
void process(Shape s) {
    s.area();  // Virtual call → vtable lookup (slow)
}

// JIT speculative devirtualization:
void process(Shape s) {
    if (s instanceof Circle c) {  // Type guard (fast check)
        c.area();                  // Direct call → can be inlined!
    } else {
        s.area();                  // Uncommon trap → deoptimize
    }
}
```

**Key insight:** Monomorphic (1 type) and bimorphic (2 types) call sites get devirtualized. Megamorphic (3+) don't.

### 2.5 Intrinsics

The JIT replaces certain method calls with hand-written assembly:

```java
// These are replaced with CPU-specific instructions:
System.arraycopy()           // → REP MOVSB / SIMD copy
Math.sqrt()                  // → FSQRT / VSQRTSD
Integer.bitCount()           // → POPCNT instruction
String.equals()              // → SIMD comparison
Arrays.equals()              // → Vectorized comparison
Object.hashCode()            // → Optimized identity hash
Thread.currentThread()       // → Read from TLS register
Unsafe.compareAndSwap*()     // → CMPXCHG instruction
```

```bash
# List all intrinsics
-XX:+PrintIntrinsics
```

## 3. Code Cache

### 3.1 Structure

```
┌──────────────────────────────────────────────────────────────┐
│                     CODE CACHE                                │
│                                                              │
│  ┌──────────────────┐  Segmented (Java 9+):                 │
│  │ Non-method code  │  JVM internal stubs, adapters          │
│  │ (8MB default)    │                                        │
│  ├──────────────────┤                                        │
│  │ Profiled code    │  C1-compiled with profiling (Tier 3)   │
│  │ (122MB default)  │  Short-lived, replaced by C2 code     │
│  ├──────────────────┤                                        │
│  │ Non-profiled code│  C2-compiled (Tier 4) + C1 simple     │
│  │ (122MB default)  │  Long-lived, peak performance          │
│  └──────────────────┘                                        │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Code Cache Full — Performance Cliff

```bash
# Symptom in log:
# "CodeCache is full. Compiler has been disabled."
# → JIT stops, new methods run interpreted = HUGE perf drop

# Prevention:
-XX:ReservedCodeCacheSize=512m     # Increase for large applications

# Monitor:
jcmd <pid> Compiler.codecache

# Output:
# CodeHeap 'non-profiled nmethods': size=120256Kb used=45678Kb max_used=67890Kb free=74578Kb
# CodeHeap 'profiled nmethods': size=120256Kb used=89012Kb max_used=110234Kb free=31244Kb
#  ↑ If free is low, increase ReservedCodeCacheSize

# Aggressive code cache sweeper
-XX:+UseCodeCacheFlushing         # Flush cold methods (default ON)
```

### 3.3 Sizing Recommendations

| Application Type | ReservedCodeCacheSize |
|---|---|
| Microservice (Spring Boot) | 128-256MB |
| Monolith / Application Server | 256-512MB |
| IDE / Large Desktop App | 512MB-1GB |
| Compiler / Build Tool | 256MB |

## 4. JIT Diagnostic & Logging

### 4.1 Print Compilation

```bash
# See what's being compiled
-XX:+PrintCompilation

# Output format:
# timestamp compilation_id attributes method_name size deopt
#    76    1       b        java.lang.String::hashCode (55 bytes)
#   142   34  %    b        org.example.HotLoop::compute @ 12 (120 bytes)
#                  ↑ % = OSR compilation  b = blocking

# Attributes:
# % = On-Stack Replacement (loop optimization)
# s = synchronized method
# ! = has exception handler
# b = blocking compilation
# n = native method wrapper
```

### 4.2 Detailed Compilation Log

```bash
-XX:+UnlockDiagnosticVMOptions
-XX:+LogCompilation
-XX:LogFile=compilation.log

# Analyze with JITWatch (visualization tool)
# https://github.com/AdoptOpenJDK/jitwatch
```

### 4.3 Print Assembly (Disassembly)

```bash
# Requires hsdis (HotSpot disassembler) library
-XX:+UnlockDiagnosticVMOptions
-XX:+PrintAssembly
-XX:PrintAssemblyOptions=intel    # Intel syntax (more readable)

# Filter to specific method
-XX:CompileCommand=print,*MyClass.hotMethod
```

### 4.4 Compile Commands

```bash
# Force compile specific method
-XX:CompileCommand=compileonly,*MyClass.hotMethod

# Exclude method from compilation
-XX:CompileCommand=exclude,*MyClass.problematicMethod

# Don't inline specific method
-XX:CompileCommand=dontinline,*MyClass.largeMethod

# Log inlining for specific method
-XX:CompileCommand=print,*MyClass.hotMethod

# From file
-XX:CompileCommandFile=hotspot_compiler
# File content:
# compileonly *MyClass.hotMethod
# exclude *MyClass.debugMethod
```

## 5. Deoptimization

### 5.1 What Causes Deoptimization

The JIT makes speculative optimizations based on profiling. When assumptions are violated:

```java
// JIT assumed Shape is always Circle (monomorphic)
void process(Shape s) {
    s.area();  // Compiled as direct call to Circle.area()
}

// Then suddenly a Square shows up:
process(new Square());  // DEOPTIMIZATION! Falls back to interpreter

// Types of uncommon traps:
// - class_check: unexpected receiver type
// - null_check: null pointer where non-null assumed
// - range_check: array bounds violation
// - unloaded: class not yet loaded
// - unstable_if: branch direction changed
```

### 5.2 Monitoring Deoptimization

```bash
# Log deoptimization events
-XX:+TraceDeoptimization

# In GC/compilation logs:
# "Uncommon trap" or "made not entrant" = deoptimization happened

# Excessive deopt = performance instability
# Check with:
jcmd <pid> Compiler.queue   # Compilation queue
```

### 5.3 Avoiding Unnecessary Deoptimizations

```java
// Anti-pattern: Megamorphic call sites (3+ types)
List<Shape> shapes = List.of(new Circle(), new Square(), new Triangle());
for (Shape s : shapes) {
    s.area();  // Megamorphic → no devirtualization → slow virtual dispatch
}

// Better: Separate monomorphic loops (if possible)
for (Circle c : circles) { c.area(); }   // Monomorphic → inlined
for (Square s : squares) { s.area(); }   // Monomorphic → inlined

// Anti-pattern: Unstable branch
boolean debug = System.getenv("DEBUG") != null;
for (int i = 0; i < 1_000_000; i++) {
    if (debug) { log(i); }  // JIT assumes always false, then compiles away
    compute(i);             // If DEBUG set later → deopt!
}
```

## 6. Warmup Strategies

### 6.1 The Warmup Problem

```
        Performance
            ▲
            │         ┌──────────── Peak (C2 compiled)
            │        /
            │       /  ← Warmup period (1-5 minutes)
            │      /
            │─────/    ← C1 compiled
            │    /
            │───/      ← Interpreted
            └──────────────────────── Time
            
Problem: First N requests after deploy are slow
```

### 6.2 Warmup Approaches

```java
// Approach 1: Eager warmup via dummy requests at startup
@Component
public class JitWarmup implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting JIT warmup...");
        // Call hot paths with realistic data
        for (int i = 0; i < 10_000; i++) {
            processOrder(createDummyOrder());
            calculatePrice(createDummyItem());
            serializeResponse(createDummyResponse());
        }
        log.info("JIT warmup complete");
    }
}
```

```bash
# Approach 2: Class Data Sharing + AOT hints
# Java 19+: Training run records profile
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -jar app.jar
# Production: use pre-recorded profile
java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -XX:AOTCache=app.aot -jar app.jar
java -XX:AOTCache=app.aot -jar app.jar

# Approach 3: Increase compilation threads for faster warmup
-XX:CICompilerCount=4              # More compiler threads (default: cores/4)
```

### 6.3 ReadyNow (Azul Zing)

```bash
# Commercial: Azul Platform Prime with ReadyNow
# Records compilation profile, replays on next start
# Achieves peak performance within seconds
-XX:ProfileLogIn=profile.log       # Load from previous run
-XX:ProfileLogOut=profile.log      # Save for next run
```

## 7. GraalVM & AOT Compilation

### 7.1 GraalVM JIT vs Native Image

```
┌─────────────────────────────────────────────────────────────┐
│                    GraalVM Compilation Modes                   │
│                                                              │
│  ┌─────────────────────────┐  ┌────────────────────────────┐│
│  │   JIT Mode (GraalVM CE) │  │   Native Image (AOT)       ││
│  │                         │  │                             ││
│  │ • Replaces C2 compiler  │  │ • Compile at build time    ││
│  │ • Better peak perf      │  │ • ~10ms startup            ││
│  │ • Same startup as       │  │ • Fixed memory footprint   ││
│  │   HotSpot               │  │ • No warmup needed         ││
│  │ • Aggressive speculative│  │ • Closed-world assumption  ││
│  │   optimizations         │  │ • No reflection by default ││
│  └─────────────────────────┘  └────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Native Image Trade-offs

| Aspect | JIT (HotSpot/Graal) | AOT (Native Image) |
|--------|---------------------|---------------------|
| Startup | 1-10 seconds | 10-50 milliseconds |
| Peak throughput | Higher (speculative opts) | Lower (no profile-guided) |
| Memory footprint | Higher | Much lower |
| Warmup required | Yes (1-5 min) | No |
| Reflection support | Full | Requires configuration |
| Dynamic class loading | Full | Not supported |
| Best for | Long-running services | Serverless, CLI, short-lived |

## 8. Performance Anti-Patterns

### 8.1 Preventing Inlining

```java
// BAD: Large methods won't be inlined
public void doEverything() {
    // 500 lines of code → too large for inlining → callers can't optimize
}

// GOOD: Small focused methods get inlined
public void processOrder(Order order) {
    validate(order);      // Small → inlined
    calculateTotal(order); // Small → inlined
    persist(order);       // Small → inlined
}
```

### 8.2 Megamorphic Call Sites

```java
// BAD: Interface with many implementations at same call site
public void processAll(List<EventHandler> handlers, Event event) {
    for (EventHandler h : handlers) {
        h.handle(event);  // If >2 types → megamorphic → no inlining
    }
}

// BETTER: Use visitor pattern or batch by type
public void processAll(List<EventHandler> handlers, Event event) {
    // Group by type, process each group (monomorphic within loop)
    handlers.stream()
        .collect(Collectors.groupingBy(Object::getClass))
        .values()
        .forEach(group -> group.forEach(h -> h.handle(event)));
}
```

### 8.3 Defeating Escape Analysis

```java
// BAD: Object escapes through field assignment
class Processor {
    private Point lastPoint;  // Field reference prevents escape analysis
    
    public int compute(int x, int y) {
        Point p = new Point(x, y);
        lastPoint = p;          // ← Escapes! Can't eliminate allocation
        return p.x + p.y;
    }
}

// GOOD: Keep objects local
class Processor {
    public int compute(int x, int y) {
        Point p = new Point(x, y);  // ← Doesn't escape → eliminated!
        return p.x + p.y;
    }
}
```

## 9. Benchmarking with JMH

### 9.1 Why JMH is Necessary

```java
// WRONG: naive benchmarking (JIT eliminates dead code, warmup ignored)
long start = System.nanoTime();
for (int i = 0; i < 1_000_000; i++) {
    Math.sqrt(i);  // JIT eliminates this — result unused!
}
long time = System.nanoTime() - start;  // Measures nothing!

// CORRECT: Use JMH
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Thread)
public class MathBenchmark {
    private double value = 42.0;

    @Benchmark
    public double sqrtBenchmark() {
        return Math.sqrt(value);  // Returned → can't be eliminated
    }
    
    @Benchmark
    public void sqrtBlackhole(Blackhole bh) {
        bh.consume(Math.sqrt(value));  // Blackhole prevents elimination
    }
}
```

### 9.2 Common JMH Pitfalls

```java
// Pitfall 1: Constant folding
@Benchmark
public int badBenchmark() {
    return 2 + 2;  // JIT folds to 4 at compile time!
}

// Fix: Use @State fields
@State(Scope.Thread)
public class MyState {
    int x = 2, y = 2;
}
@Benchmark
public int goodBenchmark(MyState state) {
    return state.x + state.y;  // Can't fold — values could change
}

// Pitfall 2: Loop optimization
@Benchmark
public int badLoop() {
    int sum = 0;
    for (int i = 0; i < 1000; i++) {
        sum += i;  // JIT replaces with formula: n*(n-1)/2
    }
    return sum;
}
```

### 9.3 Running JMH

```bash
# Add to pom.xml
# <dependency>
#   <groupId>org.openjdk.jmh</groupId>
#   <artifactId>jmh-core</artifactId>
#   <version>1.37</version>
# </dependency>

# Run benchmarks
mvn clean install
java -jar target/benchmarks.jar

# With JVM flags
java -jar target/benchmarks.jar -jvmArgs "-XX:+UseZGC -Xmx4g"

# Profile specific benchmark with async-profiler
java -jar target/benchmarks.jar -prof async:output=flamegraph
```

## 10. Quick Reference: JIT Flags

```bash
# Compilation Control
-XX:+TieredCompilation               # Tiered compilation (default)
-XX:TieredStopAtLevel=4              # Max compilation level
-XX:CompileThreshold=10000           # Invocations before compile
-XX:CICompilerCount=4                # Compiler threads

# Inlining
-XX:MaxInlineSize=35                 # Always inline if < N bytes
-XX:FreqInlineSize=325               # Inline hot methods up to N bytes
-XX:InlineSmallCode=2000             # Inline if compiled < N bytes
-XX:MaxInlineLevel=15                # Max depth of inlining

# Escape Analysis
-XX:+DoEscapeAnalysis                # Enable (default)
-XX:+EliminateAllocations            # Scalar replacement
-XX:+EliminateLocks                  # Lock elision

# Code Cache
-XX:ReservedCodeCacheSize=256m       # Total code cache
-XX:+UseCodeCacheFlushing            # Sweep cold code

# Diagnostics (use with -XX:+UnlockDiagnosticVMOptions)
-XX:+PrintCompilation                # Log compilations
-XX:+PrintInlining                   # Log inlining decisions
-XX:+PrintAssembly                   # Print generated assembly
-XX:+LogCompilation                  # XML compilation log
-XX:+TraceDeoptimization             # Log deoptimization events
```
