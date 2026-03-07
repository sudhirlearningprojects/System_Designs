# Java Data Structures - Internal Working

A deep dive into how Java's core data structures work under the hood, covering memory layout, time complexity, and real-world usage patterns.

---

## Table of Contents

1. [HashMap](#1-hashmap)
2. [LinkedHashMap](#2-linkedhashmap)
3. [TreeMap](#3-treemap)
4. [HashSet](#4-hashset)
5. [LinkedHashSet](#5-linkedhashset)
6. [TreeSet](#6-treeset)
7. [ArrayList](#7-arraylist)
8. [LinkedList](#8-linkedlist)
9. [ArrayDeque](#9-arraydeque)
10. [PriorityQueue](#10-priorityqueue)
11. [ConcurrentHashMap](#11-concurrenthashmap)
12. [Comparison Summary](#12-comparison-summary)

---

## 1. HashMap

### Internal Structure

HashMap uses an **array of buckets** (called `table`), where each bucket is a linked list (or red-black tree after Java 8).

```
table[] (array of Node)
  [0] → null
  [1] → Node{key="a", value=1, hash=97, next=null}
  [2] → Node{key="b", value=2, hash=98, next=Node{key="z", value=26, hash=98, next=null}}  ← collision
  ...
  [15] → null
```

### Key Fields

```java
// Default initial capacity = 16 (must be power of 2)
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

// Load factor: when to resize (resize at 16 * 0.75 = 12 entries)
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// Threshold to convert linked list → red-black tree
static final int TREEIFY_THRESHOLD = 8;

// Threshold to convert red-black tree → linked list
static final int UNTREEIFY_THRESHOLD = 6;

// Minimum table size for treeification
static final int MIN_TREEIFY_CAPACITY = 64;
```

### How `put(key, value)` Works

```
1. Compute hash(key)
   - hash = key.hashCode() ^ (key.hashCode() >>> 16)
   - XOR with upper 16 bits to spread entropy (reduces collisions)

2. Find bucket index
   - index = hash & (capacity - 1)   ← bitwise AND (faster than modulo)
   - Works because capacity is always power of 2

3. Insert into bucket
   - If bucket is empty → create new Node
   - If key already exists → update value
   - If collision → append to linked list (or tree)

4. Check if resize needed
   - if (++size > threshold) resize()
   - threshold = capacity * loadFactor
   - resize() doubles capacity and rehashes all entries
```

### Hash Function

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

Why XOR with upper 16 bits? For small tables, only the lower bits of hash are used in index calculation. XOR spreads the upper bits into lower bits, reducing collisions.

### Treeification (Java 8+)

When a single bucket has ≥ 8 nodes AND table size ≥ 64:
- Linked list → **Red-Black Tree**
- Lookup in that bucket: O(log n) instead of O(n)
- When entries are removed and bucket shrinks to ≤ 6 → converts back to linked list

### Resize / Rehash

```
Old capacity: 16  →  New capacity: 32
Old threshold: 12 →  New threshold: 24

For each existing node:
  newIndex = hash & (newCapacity - 1)
  
  Key insight: newIndex is either:
    - same as old index, OR
    - old index + old capacity
  
  This is because (hash & 31) differs from (hash & 15) only in bit 4.
  So nodes split into two groups without full rehash.
```

### Time Complexity

| Operation | Average | Worst Case (all in one bucket) |
|-----------|---------|-------------------------------|
| get       | O(1)    | O(n) → O(log n) with tree     |
| put       | O(1)    | O(n) → O(log n) with tree     |
| remove    | O(1)    | O(n) → O(log n) with tree     |
| resize    | O(n)    | O(n)                          |

### Null Handling

- **One null key** allowed → always stored at index 0
- Multiple null values allowed

### Common Pitfalls

```java
// BAD: mutable key - hashCode changes after modification
Map<List<Integer>, String> map = new HashMap<>();
List<Integer> key = new ArrayList<>(List.of(1, 2));
map.put(key, "value");
key.add(3);  // key's hashCode changed → can never find "value" again!

// GOOD: use immutable keys (String, Integer, record types)
Map<String, String> map = new HashMap<>();
```

---

## 2. LinkedHashMap

### Internal Structure

Extends HashMap but adds a **doubly-linked list** running through all entries to maintain insertion order (or access order).

```
HashMap table[] + doubly-linked list:

head ↔ Node{A} ↔ Node{B} ↔ Node{C} ↔ tail
         ↑           ↑           ↑
      bucket[2]   bucket[7]   bucket[2]  (also in hash table)
```

Each node has two extra pointers:
```java
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;  // doubly-linked list pointers
}
```

### Access Order Mode

```java
// accessOrder=true → moves accessed entry to end (LRU behavior)
LinkedHashMap<K,V> lruCache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > MAX_SIZE;  // evict oldest when full
    }
};
```

This is the classic **LRU Cache** implementation pattern.

### Time Complexity

Same as HashMap (O(1) average) but with O(1) ordered iteration.

---

## 3. TreeMap

### Internal Structure

TreeMap is backed by a **Red-Black Tree** — a self-balancing BST.

```
         [M, BLACK]
        /           \
   [F, RED]       [T, RED]
   /      \       /      \
[C,BLK] [H,BLK] [P,BLK] [X,BLK]
```

### Red-Black Tree Properties

1. Every node is RED or BLACK
2. Root is always BLACK
3. No two consecutive RED nodes (RED node's parent must be BLACK)
4. Every path from root to null has the same number of BLACK nodes
5. Null leaves are considered BLACK

These rules guarantee tree height ≤ 2 * log₂(n+1), ensuring O(log n) operations.

### How `put(key, value)` Works

```
1. Start at root, compare key using Comparator (or natural ordering)
2. Traverse left (key < current) or right (key > current)
3. Insert as RED leaf
4. Fix violations:
   - Case 1: Uncle is RED → recolor parent, uncle to BLACK; grandparent to RED
   - Case 2: Uncle is BLACK, node is inner child → rotate parent
   - Case 3: Uncle is BLACK, node is outer child → rotate grandparent + recolor
5. Ensure root is BLACK
```

### Key Operations Unique to TreeMap

```java
TreeMap<Integer, String> map = new TreeMap<>();

map.firstKey();              // smallest key
map.lastKey();               // largest key
map.floorKey(5);             // largest key ≤ 5
map.ceilingKey(5);           // smallest key ≥ 5
map.lowerKey(5);             // largest key < 5
map.higherKey(5);            // smallest key > 5
map.headMap(5);              // keys < 5 (view, not copy)
map.tailMap(5);              // keys ≥ 5 (view, not copy)
map.subMap(3, 7);            // keys in [3, 7) (view, not copy)
map.descendingMap();         // reverse order view
```

### Custom Comparator

```java
// Case-insensitive TreeMap
TreeMap<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

// Reverse order
TreeMap<Integer, String> reverseMap = new TreeMap<>(Comparator.reverseOrder());
```

### Time Complexity

| Operation       | Time     |
|-----------------|----------|
| get/put/remove  | O(log n) |
| firstKey/lastKey| O(log n) |
| floorKey/ceiling| O(log n) |
| iteration       | O(n)     |

### When to Use TreeMap vs HashMap

| Use TreeMap when...                          | Use HashMap when...              |
|----------------------------------------------|----------------------------------|
| Need sorted key order                        | Order doesn't matter             |
| Need range queries (subMap, headMap)         | Need O(1) average operations     |
| Need floor/ceiling/nearest key lookups       | Keys are not Comparable          |
| Implementing interval trees or ordered caches| Maximum throughput needed        |

---

## 4. HashSet

### Internal Structure

HashSet is literally just a **HashMap with a dummy value**:

```java
public class HashSet<E> {
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object();  // dummy value

    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }
}
```

Everything about HashMap's internals (hashing, bucketing, treeification, resizing) applies directly to HashSet.

### Time Complexity

| Operation | Average | Worst Case |
|-----------|---------|------------|
| add       | O(1)    | O(log n)   |
| contains  | O(1)    | O(log n)   |
| remove    | O(1)    | O(log n)   |

### hashCode + equals Contract

For HashSet (and HashMap keys) to work correctly:
- If `a.equals(b)` → `a.hashCode() == b.hashCode()` (**mandatory**)
- If `a.hashCode() == b.hashCode()` → `a.equals(b)` may or may not be true (collision is OK)

```java
// Correct: override both hashCode and equals together
public class Point {
    int x, y;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point p)) return false;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
```

---

## 5. LinkedHashSet

### Internal Structure

LinkedHashSet extends HashSet but uses **LinkedHashMap** internally (insertion-order maintained):

```java
public class LinkedHashSet<E> extends HashSet<E> {
    // Uses LinkedHashMap instead of HashMap
    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }
}
```

Maintains insertion order with O(1) add/contains/remove. Useful when you need both uniqueness and predictable iteration order.

---

## 6. TreeSet

### Internal Structure

TreeSet is a **TreeMap with a dummy value** — exactly like HashSet is to HashMap:

```java
public class TreeSet<E> {
    private transient NavigableMap<E, Object> m;
    private static final Object PRESENT = new Object();

    public TreeSet() {
        this(new TreeMap<>());
    }
}
```

All TreeMap internals (Red-Black Tree) apply directly.

### Key Operations

```java
TreeSet<Integer> set = new TreeSet<>();

set.first();          // smallest element
set.last();           // largest element
set.floor(5);         // largest element ≤ 5
set.ceiling(5);       // smallest element ≥ 5
set.headSet(5);       // elements < 5
set.tailSet(5);       // elements ≥ 5
set.subSet(3, 7);     // elements in [3, 7)
set.descendingSet();  // reverse order view
```

---

## 7. ArrayList

### Internal Structure

ArrayList wraps a plain **Object array** with dynamic resizing:

```java
transient Object[] elementData;  // backing array
private int size;                // logical size (not array length)
```

### Growth Strategy

```
Initial capacity: 10 (default)

When array is full:
  newCapacity = oldCapacity + (oldCapacity >> 1)
             = oldCapacity * 1.5

Example: 10 → 15 → 22 → 33 → 49 → ...
```

### How `add(element)` Works

```
1. ensureCapacity(size + 1)
   - If size == elementData.length → grow array
   - Arrays.copyOf(elementData, newCapacity)  ← O(n) copy

2. elementData[size++] = element  ← O(1)
```

### Time Complexity

| Operation          | Time |
|--------------------|------|
| get(index)         | O(1) |
| add(element)       | O(1) amortized |
| add(index, element)| O(n) |
| remove(index)      | O(n) |
| contains(element)  | O(n) |
| size()             | O(1) |

### Memory Layout

```
elementData: [A][B][C][D][null][null][null][null][null][null]
              0   1   2   3     4     5     6     7     8     9
size = 4, capacity = 10
```

Unused slots hold `null` — wasted memory. Use `trimToSize()` to reclaim.

---

## 8. LinkedList

### Internal Structure

Doubly-linked list with head and tail pointers:

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}

transient Node<E> first;  // head
transient Node<E> last;   // tail
transient int size;
```

```
null ← [A] ↔ [B] ↔ [C] ↔ [D] → null
        ↑                   ↑
      first               last
```

### Time Complexity

| Operation              | Time |
|------------------------|------|
| addFirst / addLast     | O(1) |
| removeFirst / removeLast| O(1)|
| get(index)             | O(n) |
| add(index, element)    | O(n) |
| contains(element)      | O(n) |

### LinkedList vs ArrayList

| Scenario                        | Winner      |
|---------------------------------|-------------|
| Random access by index          | ArrayList   |
| Frequent add/remove at ends     | LinkedList  |
| Frequent add/remove in middle   | LinkedList (if you have iterator position) |
| Memory efficiency               | ArrayList   |
| Cache locality (CPU performance)| ArrayList   |

> In practice, ArrayList almost always wins due to CPU cache locality. LinkedList has high memory overhead (two pointers per node) and poor cache performance.

---

## 9. ArrayDeque

### Internal Structure

ArrayDeque uses a **circular array** (ring buffer):

```java
transient Object[] elements;
transient int head;  // index of first element
transient int tail;  // index AFTER last element
```

```
Circular array (capacity=8):
  [null][C][D][E][F][null][A][B]
         2   3   4   5         6   7
                               ↑head    ↑tail (wraps around)
```

### Growth Strategy

When full: doubles capacity and copies elements to new array (linearized).

### Time Complexity

| Operation              | Time |
|------------------------|------|
| addFirst / addLast     | O(1) amortized |
| removeFirst / removeLast| O(1)|
| peekFirst / peekLast   | O(1) |
| get(index)             | O(1) |
| contains               | O(n) |

### Why ArrayDeque > LinkedList as a Queue/Stack

- No node allocation overhead
- Better cache locality
- ArrayDeque is the **recommended Stack and Queue** in Java (Deque interface)

```java
// Stack
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);   // addFirst
stack.pop();     // removeFirst

// Queue
Deque<Integer> queue = new ArrayDeque<>();
queue.offer(1);  // addLast
queue.poll();    // removeFirst
```

---

## 10. PriorityQueue

### Internal Structure

PriorityQueue uses a **binary min-heap** stored in an array:

```java
transient Object[] queue;  // heap array
private int size;
```

### Heap Array Layout

For node at index `i`:
- Left child: `2*i + 1`
- Right child: `2*i + 2`
- Parent: `(i - 1) / 2`

```
Min-heap:
        1
       / \
      3   2
     / \ / \
    7  4 5  6

Array: [1, 3, 2, 7, 4, 5, 6]
        0  1  2  3  4  5  6
```

### How `offer(element)` Works — Sift Up

```
1. Add element at end of array (index = size)
2. Sift up: compare with parent, swap if smaller
3. Repeat until heap property restored

Example: add(0) to heap [1,3,2,7,4,5,6]
  Place 0 at index 6 → [1,3,2,7,4,5,0]
  0 < parent(2) → swap → [1,3,0,7,4,5,2]
  0 < parent(1) → swap → [0,3,1,7,4,5,2]
  0 is root → done
```

### How `poll()` Works — Sift Down

```
1. Remove root (min element)
2. Move last element to root
3. Sift down: compare with smaller child, swap if larger
4. Repeat until heap property restored
```

### Time Complexity

| Operation | Time     |
|-----------|----------|
| offer     | O(log n) |
| poll      | O(log n) |
| peek      | O(1)     |
| remove(o) | O(n)     |
| contains  | O(n)     |
| heapify   | O(n)     |

### Custom Comparator

```java
// Max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());

// By field
PriorityQueue<Task> byPriority = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority)
);
```

---

## 11. ConcurrentHashMap

### Internal Structure (Java 8+)

Same array-of-buckets structure as HashMap, but with **fine-grained locking**:

- No single global lock (unlike `Hashtable` or `Collections.synchronizedMap`)
- Uses **CAS (Compare-And-Swap)** for lock-free reads and empty-bucket writes
- Uses **synchronized on individual bucket head node** for writes to non-empty buckets
- Effectively: up to N concurrent writers (one per bucket)

```
table[]:
  [0] → CAS for empty bucket insert
  [1] → synchronized(node) for non-empty bucket
  [2] → synchronized(node)
  ...
```

### Key Design Decisions

```java
// volatile array ensures visibility across threads
transient volatile Node<K,V>[] table;

// Node values are volatile for lock-free reads
static class Node<K,V> {
    final int hash;
    final K key;
    volatile V val;       // ← volatile
    volatile Node<K,V> next; // ← volatile
}
```

### size() Accuracy

`size()` returns an **approximate count** (uses LongAdder-like striped counters). Use `mappingCount()` for large maps.

### Null Keys/Values

ConcurrentHashMap does **NOT** allow null keys or null values (unlike HashMap). This prevents ambiguity in concurrent contexts — you can't distinguish "key not present" from "key maps to null".

### Time Complexity

Same as HashMap (O(1) average) but thread-safe without full synchronization.

---

## 12. Comparison Summary

### Map Implementations

| Feature              | HashMap       | LinkedHashMap  | TreeMap        | ConcurrentHashMap |
|----------------------|---------------|----------------|----------------|-------------------|
| Ordering             | None          | Insertion/Access| Sorted (BST)  | None              |
| Null keys            | 1 allowed     | 1 allowed      | Not allowed*   | Not allowed       |
| Thread-safe          | No            | No             | No             | Yes               |
| get/put              | O(1) avg      | O(1) avg       | O(log n)       | O(1) avg          |
| Iteration order      | Unpredictable | Predictable    | Sorted         | Unpredictable     |
| Memory overhead      | Low           | Medium         | High           | Medium            |
| Range queries        | No            | No             | Yes            | No                |

*TreeMap allows null key only with a custom Comparator that handles null.

### Set Implementations

| Feature              | HashSet       | LinkedHashSet  | TreeSet        |
|----------------------|---------------|----------------|----------------|
| Backed by            | HashMap       | LinkedHashMap  | TreeMap        |
| Ordering             | None          | Insertion order| Sorted         |
| add/contains/remove  | O(1) avg      | O(1) avg       | O(log n)       |
| Null element         | 1 allowed     | 1 allowed      | Not allowed    |

### List/Queue/Deque Implementations

| Feature              | ArrayList     | LinkedList     | ArrayDeque     | PriorityQueue  |
|----------------------|---------------|----------------|----------------|----------------|
| Backing structure    | Dynamic array | Doubly-linked  | Circular array | Binary heap    |
| Random access        | O(1)          | O(n)           | O(1)           | O(n)           |
| Add/remove at ends   | O(1) amortized| O(1)           | O(1) amortized | O(log n)       |
| Add/remove at middle | O(n)          | O(n)           | O(n)           | O(n)           |
| Memory efficiency    | High          | Low            | High           | High           |
| Ordering             | Insertion     | Insertion      | Insertion      | Priority       |

---

## Quick Decision Guide

```
Need key-value storage?
  ├── Need sorted keys or range queries? → TreeMap
  ├── Need insertion/access order?       → LinkedHashMap
  ├── Need thread safety?                → ConcurrentHashMap
  └── Just need fast lookup?             → HashMap

Need unique elements?
  ├── Need sorted order or range ops?    → TreeSet
  ├── Need insertion order?              → LinkedHashSet
  └── Just need fast membership test?   → HashSet

Need a list?
  ├── Frequent random access?            → ArrayList
  └── Frequent add/remove at ends?       → ArrayDeque (not LinkedList)

Need a queue/stack?
  ├── Priority-based ordering?           → PriorityQueue
  └── FIFO or LIFO?                      → ArrayDeque

Need thread-safe queue?
  ├── Blocking behavior needed?          → LinkedBlockingQueue / ArrayBlockingQueue
  └── Non-blocking?                      → ConcurrentLinkedQueue
```
