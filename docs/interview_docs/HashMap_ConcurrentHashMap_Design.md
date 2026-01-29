# HashMap to ConcurrentHashMap - Design & Implementation Guide

## Table of Contents
1. [HashMap Design](#hashmap-design)
2. [HashMap Implementation](#hashmap-implementation)
3. [ConcurrentHashMap Design](#concurrenthashmap-design)
4. [Conversion Strategy](#conversion-strategy)
5. [Performance Comparison](#performance-comparison)

---

## HashMap Design

### Internal Structure

```
HashMap Structure:
┌─────────────────────────────────────────────────────┐
│              HashMap (Default Capacity: 16)          │
├─────────────────────────────────────────────────────┤
│  Node<K,V>[] table                                  │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐│
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │10 │...││
│  └─┬─┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘│
│    │                                                 │
│    ↓ (Linked List / Red-Black Tree)                │
│  Node → Node → Node                                 │
│  ├─ hash                                            │
│  ├─ key                                             │
│  ├─ value                                           │
│  └─ next                                            │
└─────────────────────────────────────────────────────┘
```

### Key Components

```java
public class HashMap<K,V> {
    // Default capacity
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    // Maximum capacity
    static final int MAXIMUM_CAPACITY = 1 << 30;
    
    // Load factor
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    // Threshold for treeifying
    static final int TREEIFY_THRESHOLD = 8;
    
    // Internal storage
    transient Node<K,V>[] table;
    transient int size;
    int threshold;
    final float loadFactor;
    
    // Node structure
    static class Node<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;
    }
}
```

---

## HashMap Implementation

### Custom HashMap Implementation

```java
public class MyHashMap<K, V> {
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    
    private Node<K, V>[] table;
    private int size;
    private int threshold;
    
    static class Node<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;
        
        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
    
    @SuppressWarnings("unchecked")
    public MyHashMap() {
        this.table = (Node<K, V>[]) new Node[DEFAULT_CAPACITY];
        this.threshold = (int) (DEFAULT_CAPACITY * LOAD_FACTOR);
        this.size = 0;
    }
    
    @SuppressWarnings("unchecked")
    public MyHashMap(int capacity) {
        int cap = tableSizeFor(capacity);
        this.table = (Node<K, V>[]) new Node[cap];
        this.threshold = (int) (cap * LOAD_FACTOR);
        this.size = 0;
    }
    
    // Hash function
    private int hash(K key) {
        if (key == null) return 0;
        int h = key.hashCode();
        return h ^ (h >>> 16);  // XOR with right-shifted bits
    }
    
    // Get index from hash
    private int indexFor(int hash, int length) {
        return hash & (length - 1);  // Equivalent to hash % length
    }
    
    // Round up to power of 2
    private int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
    }
    
    // PUT operation
    public V put(K key, V value) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        // Check if key exists
        Node<K, V> node = table[index];
        while (node != null) {
            if (node.hash == hash && 
                (node.key == key || (key != null && key.equals(node.key)))) {
                V oldValue = node.value;
                node.value = value;
                return oldValue;
            }
            node = node.next;
        }
        
        // Add new node
        addNode(hash, key, value, index);
        return null;
    }
    
    private void addNode(int hash, K key, V value, int index) {
        Node<K, V> newNode = new Node<>(hash, key, value, table[index]);
        table[index] = newNode;
        size++;
        
        if (size >= threshold) {
            resize();
        }
    }
    
    // GET operation
    public V get(K key) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        Node<K, V> node = table[index];
        while (node != null) {
            if (node.hash == hash && 
                (node.key == key || (key != null && key.equals(node.key)))) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }
    
    // REMOVE operation
    public V remove(K key) {
        int hash = hash(key);
        int index = indexFor(hash, table.length);
        
        Node<K, V> node = table[index];
        Node<K, V> prev = null;
        
        while (node != null) {
            if (node.hash == hash && 
                (node.key == key || (key != null && key.equals(node.key)))) {
                if (prev == null) {
                    table[index] = node.next;
                } else {
                    prev.next = node.next;
                }
                size--;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
    }
    
    // RESIZE operation
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = table.length * 2;
        Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCapacity];
        
        // Rehash all nodes
        for (Node<K, V> node : table) {
            while (node != null) {
                Node<K, V> next = node.next;
                int newIndex = indexFor(node.hash, newCapacity);
                node.next = newTable[newIndex];
                newTable[newIndex] = node;
                node = next;
            }
        }
        
        table = newTable;
        threshold = (int) (newCapacity * LOAD_FACTOR);
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public boolean containsKey(K key) {
        return get(key) != null;
    }
}
```

---

## ConcurrentHashMap Design

### Internal Structure

```
ConcurrentHashMap Structure:
┌─────────────────────────────────────────────────────┐
│         ConcurrentHashMap (Segmented)               │
├─────────────────────────────────────────────────────┤
│  Segment 0  │  Segment 1  │  Segment 2  │ ...      │
│  ┌────────┐ │  ┌────────┐ │  ┌────────┐ │          │
│  │ Lock   │ │  │ Lock   │ │  │ Lock   │ │          │
│  ├────────┤ │  ├────────┤ │  ├────────┤ │          │
│  │Bucket[]│ │  │Bucket[]│ │  │Bucket[]│ │          │
│  └────────┘ │  └────────┘ │  └────────┘ │          │
└─────────────────────────────────────────────────────┘

Java 8+ ConcurrentHashMap (CAS + synchronized):
┌─────────────────────────────────────────────────────┐
│  Node[] table (volatile)                            │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┐                │
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │                │
│  └─┬─┴───┴───┴───┴───┴───┴───┴───┘                │
│    │                                                 │
│    ↓ CAS for first node, synchronized for chain    │
│  Node → Node → Node                                 │
└─────────────────────────────────────────────────────┘
```

### Key Features

```java
public class ConcurrentHashMap<K,V> {
    // Uses CAS (Compare-And-Swap) for lock-free operations
    // Uses synchronized blocks for bucket-level locking
    // No segment locking in Java 8+
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    
    transient volatile Node<K,V>[] table;
    
    static class Node<K,V> {
        final int hash;
        final K key;
        volatile V val;  // volatile for visibility
        volatile Node<K,V> next;
    }
}
```

---

## Conversion Strategy

### Method 1: Constructor Conversion

```java
// HashMap to ConcurrentHashMap
HashMap<String, Integer> hashMap = new HashMap<>();
hashMap.put("A", 1);
hashMap.put("B", 2);

// Convert using constructor
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>(hashMap);
```

### Method 2: putAll() Method

```java
HashMap<String, Integer> hashMap = new HashMap<>();
hashMap.put("A", 1);
hashMap.put("B", 2);

ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.putAll(hashMap);
```

### Method 3: Stream API

```java
HashMap<String, Integer> hashMap = new HashMap<>();
hashMap.put("A", 1);
hashMap.put("B", 2);

ConcurrentHashMap<String, Integer> concurrentMap = hashMap.entrySet()
    .stream()
    .collect(Collectors.toConcurrentMap(
        Map.Entry::getKey,
        Map.Entry::getValue
    ));
```

### Method 4: Manual Iteration

```java
HashMap<String, Integer> hashMap = new HashMap<>();
hashMap.put("A", 1);
hashMap.put("B", 2);

ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
    concurrentMap.put(entry.getKey(), entry.getValue());
}
```

---

## Complete Implementation Example

```java
import java.util.*;
import java.util.concurrent.*;

public class HashMapConversionDemo {
    
    public static void main(String[] args) {
        // 1. Create HashMap with default capacity
        HashMap<String, Integer> hashMap = createHashMap();
        
        // 2. Display HashMap
        System.out.println("=== HashMap ===");
        displayMap(hashMap);
        
        // 3. Convert to ConcurrentHashMap
        ConcurrentHashMap<String, Integer> concurrentMap = convertToConcurrent(hashMap);
        
        // 4. Display ConcurrentHashMap
        System.out.println("\n=== ConcurrentHashMap ===");
        displayMap(concurrentMap);
        
        // 5. Test thread safety
        testThreadSafety(hashMap, concurrentMap);
    }
    
    // Create HashMap with default capacity (16)
    private static HashMap<String, Integer> createHashMap() {
        HashMap<String, Integer> map = new HashMap<>();  // Default capacity: 16
        map.put("Apple", 100);
        map.put("Banana", 200);
        map.put("Cherry", 300);
        map.put("Date", 400);
        map.put("Elderberry", 500);
        return map;
    }
    
    // Convert HashMap to ConcurrentHashMap
    private static ConcurrentHashMap<String, Integer> convertToConcurrent(
            HashMap<String, Integer> hashMap) {
        return new ConcurrentHashMap<>(hashMap);
    }
    
    // Display map contents
    private static void displayMap(Map<String, Integer> map) {
        map.forEach((key, value) -> 
            System.out.println(key + " = " + value));
        System.out.println("Size: " + map.size());
    }
    
    // Test thread safety
    private static void testThreadSafety(
            HashMap<String, Integer> hashMap,
            ConcurrentHashMap<String, Integer> concurrentMap) {
        
        System.out.println("\n=== Thread Safety Test ===");
        
        // Test HashMap (NOT thread-safe)
        System.out.println("\nTesting HashMap (may cause issues):");
        testMapConcurrency(new HashMap<>(hashMap), "HashMap");
        
        // Test ConcurrentHashMap (thread-safe)
        System.out.println("\nTesting ConcurrentHashMap (thread-safe):");
        testMapConcurrency(new ConcurrentHashMap<>(concurrentMap), "ConcurrentHashMap");
    }
    
    private static void testMapConcurrency(Map<String, Integer> map, String mapType) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Submit 100 concurrent write operations
        for (int i = 0; i < 100; i++) {
            final int value = i;
            executor.submit(() -> {
                map.put("Key" + value, value);
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
            System.out.println(mapType + " final size: " + map.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

---

## Performance Comparison

### Benchmark Test

```java
import java.util.*;
import java.util.concurrent.*;

public class PerformanceBenchmark {
    
    private static final int OPERATIONS = 1_000_000;
    private static final int THREADS = 10;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Performance Benchmark ===\n");
        
        // Single-threaded performance
        benchmarkSingleThreaded();
        
        // Multi-threaded performance
        benchmarkMultiThreaded();
    }
    
    private static void benchmarkSingleThreaded() {
        System.out.println("Single-threaded Performance:");
        
        // HashMap
        long hashMapTime = measureTime(() -> {
            HashMap<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < OPERATIONS; i++) {
                map.put(i, i);
            }
        });
        System.out.println("HashMap: " + hashMapTime + " ms");
        
        // ConcurrentHashMap
        long concurrentMapTime = measureTime(() -> {
            ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
            for (int i = 0; i < OPERATIONS; i++) {
                map.put(i, i);
            }
        });
        System.out.println("ConcurrentHashMap: " + concurrentMapTime + " ms");
        System.out.println();
    }
    
    private static void benchmarkMultiThreaded() throws InterruptedException {
        System.out.println("Multi-threaded Performance (" + THREADS + " threads):");
        
        // HashMap with synchronization
        long syncHashMapTime = measureConcurrent(() -> {
            Map<Integer, Integer> map = Collections.synchronizedMap(new HashMap<>());
            return map;
        });
        System.out.println("Synchronized HashMap: " + syncHashMapTime + " ms");
        
        // ConcurrentHashMap
        long concurrentMapTime = measureConcurrent(() -> {
            return new ConcurrentHashMap<Integer, Integer>();
        });
        System.out.println("ConcurrentHashMap: " + concurrentMapTime + " ms");
    }
    
    private static long measureTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
    
    private static long measureConcurrent(java.util.function.Supplier<Map<Integer, Integer>> mapSupplier) 
            throws InterruptedException {
        Map<Integer, Integer> map = mapSupplier.get();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                int start_idx = threadId * (OPERATIONS / THREADS);
                int end_idx = start_idx + (OPERATIONS / THREADS);
                for (int j = start_idx; j < end_idx; j++) {
                    map.put(j, j);
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        return System.currentTimeMillis() - start;
    }
}
```

### Results Analysis

```
Single-threaded Performance:
HashMap:            ~50 ms   (Fastest - no synchronization)
ConcurrentHashMap:  ~80 ms   (Slower due to CAS overhead)

Multi-threaded Performance (10 threads):
Synchronized HashMap: ~500 ms  (Slow - global lock)
ConcurrentHashMap:    ~150 ms  (Fast - fine-grained locking)
```

---

## Key Differences

### HashMap vs ConcurrentHashMap

| Feature | HashMap | ConcurrentHashMap |
|---------|---------|-------------------|
| **Thread Safety** | Not thread-safe | Thread-safe |
| **Null Keys** | Allows 1 null key | Does NOT allow null |
| **Null Values** | Allows null values | Does NOT allow null |
| **Performance (Single)** | Faster | Slightly slower |
| **Performance (Multi)** | Poor (with sync) | Excellent |
| **Locking** | No locking | CAS + synchronized |
| **Fail-Fast** | Yes | No (weakly consistent) |
| **Use Case** | Single-threaded | Multi-threaded |

---

## Best Practices

### 1. Choose Based on Concurrency Needs

```java
// Single-threaded application
HashMap<String, Integer> map = new HashMap<>();

// Multi-threaded application
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
```

### 2. Initial Capacity Planning

```java
// If you know size, set initial capacity
int expectedSize = 1000;
int capacity = (int) (expectedSize / 0.75) + 1;

HashMap<String, Integer> hashMap = new HashMap<>(capacity);
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>(capacity);
```

### 3. Avoid Null Keys/Values in ConcurrentHashMap

```java
// ❌ Bad - throws NullPointerException
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put(null, 1);      // NPE
map.put("key", null);  // NPE

// ✅ Good - use Optional or default values
map.put("key", Optional.ofNullable(value).orElse(0));
```

### 4. Use Atomic Operations

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// ❌ Bad - not atomic
if (!map.containsKey("key")) {
    map.put("key", 1);
}

// ✅ Good - atomic operation
map.putIfAbsent("key", 1);

// ✅ Good - compute atomically
map.compute("key", (k, v) -> v == null ? 1 : v + 1);
```

---

## Summary

### Conversion Methods
1. **Constructor**: `new ConcurrentHashMap<>(hashMap)`
2. **putAll()**: `concurrentMap.putAll(hashMap)`
3. **Stream API**: `collect(Collectors.toConcurrentMap())`
4. **Manual**: Iterate and put

### When to Use
- **HashMap**: Single-threaded, performance-critical
- **ConcurrentHashMap**: Multi-threaded, thread-safety required

### Key Takeaways
- HashMap: Fast but not thread-safe
- ConcurrentHashMap: Thread-safe with fine-grained locking
- No null keys/values in ConcurrentHashMap
- Use atomic operations for thread safety
