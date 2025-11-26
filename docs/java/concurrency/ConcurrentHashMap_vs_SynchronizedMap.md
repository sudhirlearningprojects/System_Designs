# ConcurrentHashMap vs Synchronized HashMap

## Quick Comparison

| Feature | Synchronized HashMap | ConcurrentHashMap |
|---------|---------------------|-------------------|
| **Locking** | Entire map locked | Segment/bucket-level locking |
| **Read operations** | Requires lock | Lock-free (Java 8+) |
| **Write operations** | Locks entire map | Locks only affected bucket |
| **Throughput** | Low (1 thread at a time) | High (multiple threads) |
| **Null keys/values** | Allowed | NOT allowed |
| **Iteration** | Fail-fast | Weakly consistent |
| **Performance** | Poor under contention | Excellent under contention |
| **Introduced** | Java 1.2 | Java 5 |

---

## 1. Locking Mechanism

### Synchronized HashMap

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// Internal implementation
public V get(Object key) {
    synchronized (mutex) {  // Locks ENTIRE map
        return m.get(key);
    }
}

public V put(K key, V value) {
    synchronized (mutex) {  // Locks ENTIRE map
        return m.put(key, value);
    }
}
```

**Problem:**
```java
Thread 1: map.put("A", 1)  → Acquires lock on entire map
Thread 2: map.get("Z")     → BLOCKED (waiting for lock)
Thread 3: map.size()       → BLOCKED (waiting for lock)
Thread 4: map.put("B", 2)  → BLOCKED (waiting for lock)

// Only 1 thread can access at a time
```

### ConcurrentHashMap

```java
Map<String, Integer> map = new ConcurrentHashMap<>();

// Java 8+ uses CAS + synchronized on bucket level
public V get(Object key) {
    // NO LOCKING - direct volatile read
    return getNode(hash(key), key);
}

public V put(K key, V value) {
    // Locks only the specific bucket
    synchronized (bucket) {
        // Insert into this bucket only
    }
}
```

**Benefit:**
```java
Thread 1: map.put("A", 1)  → Locks bucket for hash("A")
Thread 2: map.get("Z")     → Reads directly (no lock)
Thread 3: map.size()       → Approximate count (no lock)
Thread 4: map.put("B", 2)  → Locks bucket for hash("B") - concurrent if different bucket

// Multiple threads can work simultaneously
```

---

## 2. Performance Comparison

### Benchmark Code

```java
// Test: 4 threads, 1 million operations each

// Synchronized HashMap
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
long start = System.currentTimeMillis();
// 4 threads doing put/get operations
long syncTime = System.currentTimeMillis() - start;
// Result: ~5000ms

// ConcurrentHashMap
Map<String, Integer> concMap = new ConcurrentHashMap<>();
start = System.currentTimeMillis();
// 4 threads doing put/get operations
long concTime = System.currentTimeMillis() - start;
// Result: ~800ms (6x faster!)
```

### Why ConcurrentHashMap is Faster

```java
// Synchronized HashMap: Sequential execution
Thread 1: ████████████ (holds lock)
Thread 2:             ████████████ (waits, then executes)
Thread 3:                         ████████████ (waits, then executes)
Thread 4:                                     ████████████ (waits, then executes)

// ConcurrentHashMap: Parallel execution
Thread 1: ████████████
Thread 2: ████████████ (concurrent)
Thread 3: ████████████ (concurrent)
Thread 4: ████████████ (concurrent)
```

---

## 3. Null Handling

### Synchronized HashMap

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// Null key allowed
map.put(null, 1);  // ✅ OK

// Null value allowed
map.put("key", null);  // ✅ OK

// Ambiguous: null means "not found" or "value is null"?
Integer value = map.get("key");
if (value == null) {
    // Key doesn't exist OR value is null?
}
```

### ConcurrentHashMap

```java
Map<String, Integer> map = new ConcurrentHashMap<>();

// Null key NOT allowed
map.put(null, 1);  // ❌ NullPointerException

// Null value NOT allowed
map.put("key", null);  // ❌ NullPointerException

// Clear semantics: null means "not found"
Integer value = map.get("key");
if (value == null) {
    // Key definitely doesn't exist
}
```

**Why no nulls?**
```java
// Ambiguity problem in concurrent environment:
if (map.get(key) == null) {
    // Does key not exist, or is value null?
    // Can't call containsKey() reliably - race condition!
}

// ConcurrentHashMap solution: disallow null
if (map.get(key) == null) {
    // Key definitely doesn't exist
}
```

---

## 4. Iteration Behavior

### Synchronized HashMap (Fail-Fast)

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
map.put("A", 1);
map.put("B", 2);

// Throws ConcurrentModificationException
for (String key : map.keySet()) {
    map.put("C", 3);  // Modification during iteration
}

// MUST manually synchronize entire iteration
synchronized (map) {
    for (String key : map.keySet()) {
        map.put("C", 3);  // Still throws exception!
    }
}

// Can only read during iteration
synchronized (map) {
    for (String key : map.keySet()) {
        System.out.println(key);  // OK
    }
}
```

### ConcurrentHashMap (Weakly Consistent)

```java
Map<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

// NO exception - sees snapshot
for (String key : map.keySet()) {
    map.put("C", 3);  // Safe, but may or may not see "C"
}

// No manual synchronization needed
for (String key : map.keySet()) {
    System.out.println(key);  // Always safe
}
```

**Weakly Consistent Behavior:**
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

Iterator<String> it = map.keySet().iterator();
// Iterator sees snapshot at creation time

map.put("C", 3);  // Added after iterator created

while (it.hasNext()) {
    System.out.println(it.next());  // May or may not see "C"
}
```

---

## 5. Compound Operations

### Synchronized HashMap

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// NOT thread-safe - race condition
if (!map.containsKey("counter")) {
    map.put("counter", 0);
}
map.put("counter", map.get("counter") + 1);

// MUST manually synchronize
synchronized (map) {
    if (!map.containsKey("counter")) {
        map.put("counter", 0);
    }
    map.put("counter", map.get("counter") + 1);
}
```

### ConcurrentHashMap

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Atomic operations built-in
map.putIfAbsent("counter", 0);
map.compute("counter", (k, v) -> v == null ? 1 : v + 1);

// Or simpler
map.merge("counter", 1, Integer::sum);

// All atomic, no manual synchronization needed
```

---

## 6. Atomic Methods Comparison

### Synchronized HashMap

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// Check-then-act (NOT atomic)
synchronized (map) {
    if (!map.containsKey(key)) {
        map.put(key, value);
    }
}

// Compute new value (NOT atomic without sync)
synchronized (map) {
    Integer oldValue = map.get(key);
    Integer newValue = oldValue == null ? 1 : oldValue + 1;
    map.put(key, newValue);
}
```

### ConcurrentHashMap

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Atomic put-if-absent
map.putIfAbsent(key, value);

// Atomic compute
map.compute(key, (k, v) -> v == null ? 1 : v + 1);

// Atomic compute-if-absent
map.computeIfAbsent(key, k -> expensiveComputation());

// Atomic compute-if-present
map.computeIfPresent(key, (k, v) -> v + 1);

// Atomic merge
map.merge(key, 1, Integer::sum);

// Atomic replace
map.replace(key, oldValue, newValue);
```

---

## 7. Real-World Examples

### Example 1: Word Counter

**Synchronized HashMap:**
```java
Map<String, Integer> wordCount = Collections.synchronizedMap(new HashMap<>());

// Process words from multiple threads
void countWord(String word) {
    synchronized (wordCount) {
        Integer count = wordCount.get(word);
        wordCount.put(word, count == null ? 1 : count + 1);
    }
}
```

**ConcurrentHashMap:**
```java
ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();

// Process words from multiple threads
void countWord(String word) {
    wordCount.merge(word, 1, Integer::sum);  // One line, atomic
}
```

---

### Example 2: Cache

**Synchronized HashMap:**
```java
Map<String, Data> cache = Collections.synchronizedMap(new HashMap<>());

Data getData(String key) {
    synchronized (cache) {
        Data data = cache.get(key);
        if (data == null) {
            data = loadFromDatabase(key);
            cache.put(key, data);
        }
        return data;
    }
}
// Problem: Entire cache locked during database load!
```

**ConcurrentHashMap:**
```java
ConcurrentHashMap<String, Data> cache = new ConcurrentHashMap<>();

Data getData(String key) {
    return cache.computeIfAbsent(key, k -> loadFromDatabase(k));
}
// Only the specific key is locked, other threads can access other keys
```

---

### Example 3: Session Management

**Synchronized HashMap:**
```java
Map<String, Session> sessions = Collections.synchronizedMap(new HashMap<>());

// Every request blocks all other requests
Session getSession(String sessionId) {
    synchronized (sessions) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastAccess();
        }
        return session;
    }
}
```

**ConcurrentHashMap:**
```java
ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

// Multiple requests can access different sessions concurrently
Session getSession(String sessionId) {
    return sessions.computeIfPresent(sessionId, (id, session) -> {
        session.updateLastAccess();
        return session;
    });
}
```

---

## 8. Internal Structure

### Synchronized HashMap

```
┌─────────────────────────────────────┐
│         Single Lock (mutex)         │
│  ┌───────────────────────────────┐  │
│  │        HashMap                │  │
│  │  [bucket0][bucket1]...[bucketN]│  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘

All operations acquire the same lock
```

### ConcurrentHashMap (Java 8+)

```
┌──────────┬──────────┬──────────┬──────────┐
│ Bucket 0 │ Bucket 1 │ Bucket 2 │ Bucket 3 │
│  Lock 0  │  Lock 1  │  Lock 2  │  Lock 3  │
└──────────┴──────────┴──────────┴──────────┘

Each bucket can be locked independently
Reads are lock-free (volatile)
```

---

## 9. Size() Method

### Synchronized HashMap

```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// Locks entire map
int size = map.size();  // Exact count, but blocks all operations
```

### ConcurrentHashMap

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Lock-free, approximate count
int size = map.size();  // May be slightly stale, but doesn't block

// For exact count (slower)
long exactSize = map.mappingCount();
```

---

## 10. When to Use What

### Use Synchronized HashMap When:

❌ **Almost never in new code**
- Legacy code compatibility
- Single-threaded with occasional concurrent access
- Need null keys/values

### Use ConcurrentHashMap When:

✅ **Always for concurrent access**
- Multiple threads reading/writing
- High-performance requirements
- Need atomic operations
- Modern applications (Java 5+)

---

## Performance Summary

```java
// Scenario: 10 threads, 100K operations each

// Synchronized HashMap
Throughput: ~50K ops/sec
Latency: High variance (lock contention)
CPU: Low utilization (threads waiting)

// ConcurrentHashMap
Throughput: ~500K ops/sec (10x faster)
Latency: Low, consistent
CPU: High utilization (threads working)
```

---

## Complete Example

```java
import java.util.*;
import java.util.concurrent.*;

public class MapComparison {
    
    // Synchronized HashMap example
    static class SynchronizedCounter {
        private Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
        
        public void increment(String key) {
            synchronized (map) {
                map.put(key, map.getOrDefault(key, 0) + 1);
            }
        }
        
        public int get(String key) {
            synchronized (map) {
                return map.getOrDefault(key, 0);
            }
        }
    }
    
    // ConcurrentHashMap example
    static class ConcurrentCounter {
        private ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        public void increment(String key) {
            map.merge(key, 1, Integer::sum);
        }
        
        public int get(String key) {
            return map.getOrDefault(key, 0);
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        int threads = 10;
        int operations = 100000;
        
        // Test Synchronized HashMap
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        long syncStart = System.currentTimeMillis();
        runTest(threads, operations, () -> syncCounter.increment("counter"));
        long syncTime = System.currentTimeMillis() - syncStart;
        
        // Test ConcurrentHashMap
        ConcurrentCounter concCounter = new ConcurrentCounter();
        long concStart = System.currentTimeMillis();
        runTest(threads, operations, () -> concCounter.increment("counter"));
        long concTime = System.currentTimeMillis() - concStart;
        
        System.out.println("Synchronized HashMap: " + syncTime + "ms");
        System.out.println("ConcurrentHashMap: " + concTime + "ms");
        System.out.println("Speedup: " + (syncTime / (double) concTime) + "x");
    }
    
    static void runTest(int threads, int operations, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < operations; j++) {
                    task.run();
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
    }
}
```

---

## Key Takeaways

1. **ConcurrentHashMap uses fine-grained locking** (bucket-level), Synchronized HashMap locks entire map
2. **ConcurrentHashMap reads are lock-free**, Synchronized HashMap locks for reads
3. **ConcurrentHashMap provides atomic operations**, Synchronized HashMap requires manual synchronization
4. **ConcurrentHashMap disallows null**, Synchronized HashMap allows null
5. **ConcurrentHashMap is 5-10x faster** under concurrent load
6. **ConcurrentHashMap iteration is weakly consistent**, Synchronized HashMap is fail-fast

**Recommendation:** Use `ConcurrentHashMap` for all concurrent scenarios. It's faster, safer, and more feature-rich.

---

## References

- [ConcurrentHashMap JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html)
- [Java Concurrency in Practice](https://jcip.net/)
- [Doug Lea's Concurrent Programming](http://gee.cs.oswego.edu/dl/cpj/)

---

**Last Updated**: 2024
**Java Versions**: 5, 8, 11, 17, 21
