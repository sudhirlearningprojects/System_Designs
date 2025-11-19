# Concurrent Collections

## Table of Contents
- [Introduction to Concurrent Collections](#introduction-to-concurrent-collections)
- [ConcurrentHashMap](#concurrenthashmap)
- [CopyOnWriteArrayList](#copyonwritearraylist)
- [ConcurrentLinkedQueue](#concurrentlinkedqueue)
- [ConcurrentSkipListMap](#concurrentskiplistmap)
- [BlockingDeque](#blockingdeque)
- [Performance Comparison](#performance-comparison)
- [Best Practices](#best-practices)

## Introduction to Concurrent Collections

### Theoretical Foundation

**Concurrent data structures** are designed to be safely accessed by multiple threads without external synchronization. The theory behind them involves:

1. **Lock-Free Programming**: Using atomic operations and compare-and-swap (CAS) instead of locks
2. **Memory Consistency Models**: Ensuring proper ordering of memory operations
3. **ABA Problem**: When a value changes from A to B and back to A, appearing unchanged
4. **Hazard Pointers**: Technique to safely reclaim memory in lock-free structures
5. **Epoch-Based Reclamation**: Alternative memory management for concurrent structures

### Design Principles

1. **Non-blocking**: Operations don't block other threads
2. **Wait-free**: Every operation completes in finite steps
3. **Lock-free**: At least one thread makes progress
4. **Obstruction-free**: A thread makes progress when running alone

### Traditional vs Concurrent Collections

```java
import java.util.*;
import java.util.concurrent.*;

public class CollectionComparison {
    
    // Traditional synchronized collections
    public static void traditionalCollections() {
        // Synchronized wrapper - coarse-grained locking
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Problems with synchronized collections:
        // 1. Iteration requires external synchronization
        synchronized(syncList) {
            for (String item : syncList) {
                System.out.println(item);
            }
        }
        
        // 2. Compound operations are not atomic
        // This is NOT thread-safe even with synchronized map
        if (!syncMap.containsKey("key")) {
            syncMap.put("key", 1); // Race condition here!
        }
    }
    
    // Modern concurrent collections
    public static void concurrentCollections() {
        // Fine-grained locking and lock-free algorithms
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        
        // Safe iteration without external synchronization
        for (String item : cowList) {
            System.out.println(item);
        }
        
        // Atomic compound operations
        concurrentMap.putIfAbsent("key", 1); // Thread-safe
        concurrentMap.computeIfAbsent("key", k -> 1); // Thread-safe
    }
}
```

## ConcurrentHashMap

### Theoretical Background

**ConcurrentHashMap** uses **segment-based locking** (Java 7) and **CAS operations with synchronized blocks** (Java 8+):

1. **Segmentation Theory**: Divide hash table into segments, each with its own lock
2. **Lock Striping**: Reduce contention by using multiple locks
3. **Optimistic Locking**: Assume no conflicts, retry if conflict detected
4. **Memory Ordering**: Proper use of volatile and final fields for visibility

### Internal Structure (Java 8+)

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHashMapExample {
    
    public static void basicOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Thread-safe operations
        map.put("key1", 1);
        map.putIfAbsent("key2", 2);
        map.replace("key1", 1, 10); // Atomic compare-and-replace
        
        // Atomic increment
        map.compute("counter", (key, val) -> val == null ? 1 : val + 1);
        map.computeIfAbsent("newKey", k -> k.length());
        map.computeIfPresent("key1", (k, v) -> v * 2);
        
        // Merge operation
        map.merge("sum", 5, Integer::sum);
        
        System.out.println("Map contents: " + map);
    }
    
    public static void concurrentModification() throws InterruptedException {
        ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
        
        // Initialize counters
        for (int i = 0; i < 10; i++) {
            counters.put("counter" + i, new AtomicInteger(0));
        }
        
        // Multiple threads incrementing counters
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    String key = "counter" + (j % 10);
                    counters.get(key).incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify results
        int total = counters.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        System.out.println("Total increments: " + total); // Should be 5000
    }
    
    // Parallel operations (Java 8+)
    public static void parallelOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Populate map
        for (int i = 0; i < 1000; i++) {
            map.put("key" + i, i);
        }
        
        // Parallel search
        String result = map.search(100, (key, value) -> 
            value > 500 ? key : null);
        System.out.println("Found key with value > 500: " + result);
        
        // Parallel reduce
        Integer sum = map.reduce(100, 
            (key, value) -> value,
            0,
            Integer::sum);
        System.out.println("Sum of all values: " + sum);
        
        // Parallel forEach
        map.forEach(100, (key, value) -> {
            if (value % 100 == 0) {
                System.out.println(key + " = " + value);
            }
        });
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Basic Operations ===");
        basicOperations();
        
        System.out.println("\n=== Concurrent Modification ===");
        concurrentModification();
        
        System.out.println("\n=== Parallel Operations ===");
        parallelOperations();
    }
}
```

### Advanced ConcurrentHashMap Features

```java
import java.util.concurrent.ConcurrentHashMap;

public class AdvancedConcurrentHashMap {
    
    // Custom equivalence and hashing
    public static void customEquivalence() {
        // Case-insensitive string keys
        ConcurrentHashMap<String, String> caseInsensitiveMap = 
            new ConcurrentHashMap<String, String>() {
                @Override
                public String put(String key, String value) {
                    return super.put(key.toLowerCase(), value);
                }
                
                @Override
                public String get(Object key) {
                    return super.get(key.toString().toLowerCase());
                }
            };
        
        caseInsensitiveMap.put("Hello", "World");
        System.out.println(caseInsensitiveMap.get("HELLO")); // "World"
    }
    
    // Bulk operations
    public static void bulkOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Populate
        for (int i = 0; i < 100; i++) {
            map.put("key" + i, i);
        }
        
        // Transform all values
        map.replaceAll((key, value) -> value * 2);
        
        // Conditional updates
        map.forEach((key, value) -> {
            if (value > 100) {
                map.put(key, value / 2);
            }
        });
        
        System.out.println("Map size: " + map.size());
    }
}
```

## CopyOnWriteArrayList

### Theoretical Background

**Copy-on-Write** is a optimization strategy where:
1. **Read operations** are lock-free and fast
2. **Write operations** create a new copy of the entire array
3. **Memory consistency** is maintained through volatile array reference
4. **Snapshot isolation** for iterators

### Use Cases and Trade-offs

```java
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;

public class CopyOnWriteExample {
    
    public static void readHeavyScenario() throws InterruptedException {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        
        // Initial data
        for (int i = 0; i < 10; i++) {
            list.add("Item-" + i);
        }
        
        // Many readers
        Thread[] readers = new Thread[10];
        for (int i = 0; i < 10; i++) {
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    // Fast, lock-free reads
                    for (String item : list) {
                        // Process item
                        item.length(); // Simulate work
                    }
                }
            });
            readers[i].start();
        }
        
        // Few writers
        Thread writer = new Thread(() -> {
            for (int i = 10; i < 15; i++) {
                list.add("Item-" + i); // Expensive copy operation
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        writer.start();
        
        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();
        
        System.out.println("Final list size: " + list.size());
    }
    
    public static void snapshotIteration() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        
        // Iterator sees snapshot at creation time
        Iterator<String> iterator = list.iterator();
        
        // Modify list after iterator creation
        list.add("D");
        list.remove("A");
        
        System.out.println("Iterator sees:");
        while (iterator.hasNext()) {
            System.out.println(iterator.next()); // Still sees A, B, C
        }
        
        System.out.println("Current list: " + list); // [B, C, D]
    }
    
    // Performance comparison
    public static void performanceComparison() {
        CopyOnWriteArrayList<Integer> cowList = new CopyOnWriteArrayList<>();
        java.util.Collections.synchronizedList(new java.util.ArrayList<Integer>()) syncList;
        
        // Write performance test
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            cowList.add(i);
        }
        long cowWriteTime = System.nanoTime() - start;
        
        // Read performance test
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            for (Integer value : cowList) {
                // Read operation
            }
        }
        long cowReadTime = System.nanoTime() - start;
        
        System.out.println("COW Write time: " + cowWriteTime / 1_000_000 + "ms");
        System.out.println("COW Read time: " + cowReadTime / 1_000_000 + "ms");
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Read-Heavy Scenario ===");
        readHeavyScenario();
        
        System.out.println("\n=== Snapshot Iteration ===");
        snapshotIteration();
        
        System.out.println("\n=== Performance Comparison ===");
        performanceComparison();
    }
}
```

## ConcurrentLinkedQueue

### Theoretical Background

**Lock-free queue** implementation using:
1. **Michael & Scott Algorithm**: Non-blocking queue algorithm
2. **CAS operations**: Compare-and-swap for atomic updates
3. **Memory ordering**: Proper use of volatile fields
4. **ABA problem mitigation**: Using node references instead of values

```java
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentQueueExample {
    
    public static void basicOperations() {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        
        // Thread-safe operations
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");
        
        System.out.println("Queue size: " + queue.size()); // Note: size() is not constant-time
        System.out.println("Head: " + queue.peek());
        System.out.println("Poll: " + queue.poll());
        System.out.println("Remaining: " + queue);
    }
    
    public static void producerConsumerWithQueue() throws InterruptedException {
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        
        // Producer
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                queue.offer(i);
                if (i % 10 == 0) {
                    System.out.println("Produced: " + i);
                }
            }
        });
        
        // Consumer
        Thread consumer = new Thread(() -> {
            int consumed = 0;
            while (consumed < 100) {
                Integer item = queue.poll();
                if (item != null) {
                    consumed++;
                    if (consumed % 10 == 0) {
                        System.out.println("Consumed: " + item);
                    }
                } else {
                    Thread.yield(); // No item available, yield
                }
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        System.out.println("Final queue size: " + queue.size());
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Basic Operations ===");
        basicOperations();
        
        System.out.println("\n=== Producer-Consumer ===");
        producerConsumerWithQueue();
    }
}
```

## ConcurrentSkipListMap

### Theoretical Background

**Skip List** is a probabilistic data structure that:
1. **Maintains sorted order** with O(log n) operations
2. **Uses multiple levels** of linked lists
3. **Probabilistic balancing** instead of strict balancing
4. **Lock-free implementation** using CAS operations

```java
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.NavigableMap;

public class SkipListExample {
    
    public static void basicOperations() {
        ConcurrentSkipListMap<Integer, String> skipMap = new ConcurrentSkipListMap<>();
        
        // Insert in random order
        skipMap.put(5, "Five");
        skipMap.put(1, "One");
        skipMap.put(3, "Three");
        skipMap.put(7, "Seven");
        skipMap.put(2, "Two");
        
        System.out.println("Sorted map: " + skipMap);
        
        // Range operations
        NavigableMap<Integer, String> subMap = skipMap.subMap(2, true, 6, false);
        System.out.println("SubMap [2,6): " + subMap);
        
        // Navigation
        System.out.println("Lower key than 4: " + skipMap.lowerKey(4));
        System.out.println("Higher key than 4: " + skipMap.higherKey(4));
        System.out.println("Floor key of 4: " + skipMap.floorKey(4));
        System.out.println("Ceiling key of 4: " + skipMap.ceilingKey(4));
    }
    
    public static void concurrentNavigation() throws InterruptedException {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
        
        // Populate map
        for (int i = 0; i < 100; i += 2) {
            map.put(i, "Value-" + i);
        }
        
        // Concurrent readers doing range queries
        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    int start = threadId * 30;
                    int end = start + 20;
                    NavigableMap<Integer, String> subMap = map.subMap(start, end);
                    System.out.println("Thread " + threadId + " found " + 
                        subMap.size() + " items in range [" + start + "," + end + ")");
                }
            });
            readers[i].start();
        }
        
        // Concurrent writer
        Thread writer = new Thread(() -> {
            for (int i = 1; i < 100; i += 2) {
                map.put(i, "Odd-" + i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        writer.start();
        
        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();
        
        System.out.println("Final map size: " + map.size());
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Basic Operations ===");
        basicOperations();
        
        System.out.println("\n=== Concurrent Navigation ===");
        concurrentNavigation();
    }
}
```

## BlockingDeque

### Theoretical Background

**Double-ended queue** with blocking operations:
1. **FIFO and LIFO** operations on both ends
2. **Blocking semantics** for empty/full conditions
3. **Work-stealing algorithms** often use deques
4. **Producer-consumer variations** with multiple access points

```java
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class BlockingDequeExample {
    
    public static void workStealingPattern() throws InterruptedException {
        BlockingDeque<String> workQueue = new LinkedBlockingDeque<>();
        
        // Worker that adds work to its own end
        Thread worker1 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String task = "Worker1-Task-" + i;
                    workQueue.putLast(task); // Add to own end
                    System.out.println("Worker1 added: " + task);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Worker that steals work from other end
        Thread worker2 = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String task = workQueue.takeFirst(); // Steal from other end
                    System.out.println("Worker2 stole: " + task);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        worker1.start();
        Thread.sleep(50); // Let worker1 add some tasks
        worker2.start();
        
        worker1.join();
        worker2.join();
        
        System.out.println("Remaining tasks: " + workQueue.size());
    }
    
    public static void dualEndedProducerConsumer() throws InterruptedException {
        BlockingDeque<Integer> deque = new LinkedBlockingDeque<>(10);
        
        // High priority producer (adds to front)
        Thread highPriorityProducer = new Thread(() -> {
            try {
                for (int i = 100; i < 105; i++) {
                    deque.putFirst(i); // High priority items go to front
                    System.out.println("High priority added: " + i);
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Normal producer (adds to back)
        Thread normalProducer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    deque.putLast(i); // Normal items go to back
                    System.out.println("Normal added: " + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer (takes from front - gets high priority first)
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 15; i++) {
                    Integer item = deque.takeFirst();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        normalProducer.start();
        Thread.sleep(200);
        highPriorityProducer.start();
        consumer.start();
        
        normalProducer.join();
        highPriorityProducer.join();
        consumer.join();
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Work Stealing Pattern ===");
        workStealingPattern();
        
        System.out.println("\n=== Dual-Ended Producer-Consumer ===");
        dualEndedProducerConsumer();
    }
}
```

## Performance Comparison

```java
import java.util.*;
import java.util.concurrent.*;

public class CollectionPerformanceTest {
    
    private static final int NUM_THREADS = 4;
    private static final int OPERATIONS_PER_THREAD = 100000;
    
    public static void compareMapPerformance() throws InterruptedException {
        // Test different map implementations
        testMapImplementation("HashMap (synchronized)", 
            Collections.synchronizedMap(new HashMap<>()));
        testMapImplementation("ConcurrentHashMap", 
            new ConcurrentHashMap<>());
        testMapImplementation("Hashtable", 
            new Hashtable<>());
    }
    
    private static void testMapImplementation(String name, Map<Integer, Integer> map) 
            throws InterruptedException {
        long startTime = System.nanoTime();
        
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    int key = threadId * OPERATIONS_PER_THREAD + j;
                    map.put(key, key * 2);
                    map.get(key);
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        System.out.println(name + ": " + duration + "ms, Size: " + map.size());
    }
    
    public static void compareListPerformance() throws InterruptedException {
        testListImplementation("ArrayList (synchronized)", 
            Collections.synchronizedList(new ArrayList<>()));
        testListImplementation("CopyOnWriteArrayList", 
            new CopyOnWriteArrayList<>());
        testListImplementation("Vector", 
            new Vector<>());
    }
    
    private static void testListImplementation(String name, List<Integer> list) 
            throws InterruptedException {
        // Pre-populate for read tests
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        
        long startTime = System.nanoTime();
        
        Thread[] threads = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(() -> {
                // Mostly reads with few writes (realistic scenario)
                for (int j = 0; j < OPERATIONS_PER_THREAD / 10; j++) {
                    // 90% reads
                    for (int k = 0; k < 9; k++) {
                        if (!list.isEmpty()) {
                            list.get(k % list.size());
                        }
                    }
                    // 10% writes
                    list.add(j);
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;
        
        System.out.println(name + ": " + duration + "ms, Size: " + list.size());
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Map Performance Comparison ===");
        compareMapPerformance();
        
        System.out.println("\n=== List Performance Comparison ===");
        compareListPerformance();
    }
}
```

## Best Practices

### 1. Choose the Right Collection

```java
public class CollectionSelection {
    
    // Use ConcurrentHashMap for high-concurrency scenarios
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    
    // Use CopyOnWriteArrayList for read-heavy scenarios
    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();
    
    // Use BlockingQueue for producer-consumer
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    
    // Use ConcurrentSkipListMap for sorted concurrent access
    private final NavigableMap<Timestamp, Event> events = new ConcurrentSkipListMap<>();
}
```

### 2. Avoid Common Pitfalls

```java
public class CommonPitfalls {
    
    // WRONG: Compound operations on concurrent collections
    public void wrongWay(ConcurrentHashMap<String, Integer> map) {
        if (!map.containsKey("counter")) {
            map.put("counter", 0); // Race condition!
        }
        map.put("counter", map.get("counter") + 1); // Race condition!
    }
    
    // CORRECT: Use atomic operations
    public void correctWay(ConcurrentHashMap<String, Integer> map) {
        map.putIfAbsent("counter", 0);
        map.compute("counter", (k, v) -> v + 1);
        // Or even better:
        map.merge("counter", 1, Integer::sum);
    }
    
    // WRONG: Iterating synchronized collections without synchronization
    public void wrongIteration(List<String> syncList) {
        for (String item : syncList) { // ConcurrentModificationException possible
            System.out.println(item);
        }
    }
    
    // CORRECT: Synchronize iteration or use concurrent collections
    public void correctIteration(List<String> syncList, CopyOnWriteArrayList<String> cowList) {
        synchronized(syncList) {
            for (String item : syncList) {
                System.out.println(item);
            }
        }
        
        // Or use concurrent collection (no synchronization needed)
        for (String item : cowList) {
            System.out.println(item);
        }
    }
}
```

### 3. Memory and Performance Considerations

```java
public class PerformanceConsiderations {
    
    // Size operations can be expensive
    public void avoidFrequentSizeChecks(ConcurrentLinkedQueue<String> queue) {
        // WRONG: size() is O(n) for ConcurrentLinkedQueue
        while (queue.size() > 0) {
            queue.poll();
        }
        
        // CORRECT: Use isEmpty() or poll until null
        String item;
        while ((item = queue.poll()) != null) {
            // Process item
        }
    }
    
    // Memory considerations for CopyOnWriteArrayList
    public void memoryAwareness() {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        
        // Each write creates a new array - memory intensive
        for (int i = 0; i < 10000; i++) {
            list.add("Item " + i); // Creates 10000 array copies!
        }
        
        // Better: Batch additions
        List<String> batch = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            batch.add("Item " + i);
        }
        list.addAll(batch); // Only one array copy
    }
}
```

## Summary

- **Concurrent collections** provide thread-safe access without external synchronization
- **ConcurrentHashMap** uses segment-based locking and CAS operations for high performance
- **CopyOnWriteArrayList** is ideal for read-heavy scenarios with infrequent writes
- **ConcurrentLinkedQueue** provides lock-free FIFO operations
- **ConcurrentSkipListMap** maintains sorted order with concurrent access
- **BlockingDeque** supports work-stealing and priority-based patterns
- **Choose collections** based on access patterns and performance requirements
- **Avoid compound operations** on concurrent collections without proper atomicity

## Next Steps

Continue to [Executor Framework](06_Executor_Framework.md) to learn about thread pools and task execution.