# Java Collections Framework - Complete Guide

*Comprehensive guide covering all Java Collections with internal workings, examples, and concurrent collections*

## Table of Contents
1. [Collections Framework Overview](#overview)
2. [List Interface](#list)
3. [Set Interface](#set)
4. [Queue Interface](#queue)
5. [Map Interface](#map)
6. [Concurrent Collections](#concurrent)
7. [Collection Utilities](#utilities)
8. [Performance Comparison](#performance)
9. [Best Practices](#best-practices)

---

## 1. Collections Framework Overview {#overview}

### Hierarchy Structure

```
Collection (Interface)
├── List (Interface)
│   ├── ArrayList
│   ├── LinkedList
│   ├── Vector
│   └── Stack
├── Set (Interface)
│   ├── HashSet
│   ├── LinkedHashSet
│   └── SortedSet (Interface)
│       └── TreeSet
└── Queue (Interface)
    ├── PriorityQueue
    ├── ArrayDeque
    └── Deque (Interface)
        └── LinkedList

Map (Interface) - Separate hierarchy
├── HashMap
├── LinkedHashMap
├── Hashtable
├── Properties
└── SortedMap (Interface)
    └── TreeMap
```

### Core Interfaces

**Collection**: Root interface for all collections except Map
- `add(E e)`, `remove(Object o)`, `size()`, `isEmpty()`, `contains(Object o)`

**Iterable**: Enables for-each loop
- `iterator()`, `forEach(Consumer<? super T> action)`

---

## 2. List Interface {#list}

### ArrayList

**Internal Structure**: Dynamic array (Object[])
**Default Capacity**: 10
**Growth Factor**: 1.5x (newCapacity = oldCapacity + (oldCapacity >> 1))

```java
public class ArrayList<E> extends AbstractList<E> implements List<E> {
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elementData;
    private int size;
    
    // Growth mechanism
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5x growth
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}
```

**Key Methods & Time Complexity**:
```java
List<String> list = new ArrayList<>();

// Add operations
list.add("A");              // O(1) amortized, O(n) worst case
list.add(0, "B");          // O(n) - shifts elements
list.addAll(Arrays.asList("C", "D")); // O(m) where m is added elements

// Access operations
String item = list.get(1);  // O(1) - direct array access
list.set(1, "E");          // O(1) - direct array access

// Search operations
int index = list.indexOf("A");     // O(n) - linear search
boolean exists = list.contains("A"); // O(n) - linear search

// Remove operations
list.remove(0);            // O(n) - shifts elements
list.remove("A");          // O(n) - find + shift
list.clear();              // O(1) - just reset size
```

**Internal Working Example**:
```java
// Initial state: elementData = [null, null, null, ...] (capacity 10)
ArrayList<String> list = new ArrayList<>();

list.add("A");  // elementData = ["A", null, null, ...], size = 1
list.add("B");  // elementData = ["A", "B", null, ...], size = 2

// When capacity exceeded (after 10 elements)
// grow() creates new array with capacity 15, copies elements
```

### LinkedList

**Internal Structure**: Doubly-linked list
**Node Structure**:
```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
    
    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

**Key Methods & Time Complexity**:
```java
LinkedList<String> list = new LinkedList<>();

// Add operations
list.add("A");              // O(1) - add to tail
list.addFirst("B");         // O(1) - add to head
list.addLast("C");          // O(1) - add to tail
list.add(1, "D");           // O(n) - traverse to index

// Access operations
String first = list.getFirst();  // O(1)
String last = list.getLast();    // O(1)
String item = list.get(2);       // O(n) - traverse to index

// Remove operations
list.removeFirst();         // O(1)
list.removeLast();          // O(1)
list.remove(1);             // O(n) - traverse to index
```

**Use Cases**:
- Frequent insertions/deletions at beginning/end
- Queue/Deque implementations
- When you don't know final size

### Vector

**Legacy class** (since Java 1.0)
**Thread-safe** (synchronized methods)
**Growth Factor**: 2x (doubles capacity)

```java
Vector<String> vector = new Vector<>();
vector.add("A");           // Synchronized
vector.get(0);             // Synchronized
```

**Differences from ArrayList**:
- Synchronized (thread-safe but slower)
- 2x growth vs 1.5x
- Legacy methods: `addElement()`, `elementAt()`

### Stack

**Extends Vector**
**LIFO (Last In, First Out)**

```java
Stack<String> stack = new Stack<>();
stack.push("A");           // Add to top
stack.push("B");
String top = stack.pop();  // Remove from top: "B"
String peek = stack.peek(); // View top without removing: "A"
boolean empty = stack.empty();
```

**Modern Alternative**: Use `ArrayDeque` instead
```java
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
String top = stack.pop();
```

---

## 3. Set Interface {#set}

### HashSet

**Internal Structure**: HashMap (keys only, dummy value)
**Hash Function**: `hashCode()` and `equals()`
**Load Factor**: 0.75 (resize when 75% full)
**Initial Capacity**: 16

```java
public class HashSet<E> extends AbstractSet<E> {
    private transient HashMap<E,Object> map;
    private static final Object PRESENT = new Object();
    
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }
}
```

**Key Methods & Time Complexity**:
```java
Set<String> set = new HashSet<>();

// Add operations
set.add("A");              // O(1) average, O(n) worst case
set.addAll(Arrays.asList("B", "C")); // O(m) where m is added elements

// Search operations
boolean exists = set.contains("A");  // O(1) average, O(n) worst case

// Remove operations
set.remove("A");           // O(1) average, O(n) worst case
set.clear();               // O(n)

// Size operations
int size = set.size();     // O(1)
boolean empty = set.isEmpty(); // O(1)
```

**Hash Collision Handling**:
```java
// Before Java 8: Linked list for collisions
// Java 8+: Tree (Red-Black) when bucket size > 8

// Good hash function distribution
class Person {
    String name;
    int age;
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age);  // Good distribution
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Person person = (Person) obj;
        return age == person.age && Objects.equals(name, person.name);
    }
}
```

### LinkedHashSet

**Extends HashSet**
**Maintains insertion order**
**Internal Structure**: HashMap + Doubly-linked list

```java
Set<String> set = new LinkedHashSet<>();
set.add("C");
set.add("A");
set.add("B");
// Iteration order: C, A, B (insertion order)

for (String item : set) {
    System.out.println(item); // C, A, B
}
```

**Use Cases**:
- When you need Set behavior + predictable iteration order
- Cache implementations (LRU-like behavior)

### TreeSet

**Internal Structure**: Red-Black Tree (self-balancing BST)
**Sorted order**: Natural ordering or custom Comparator
**All operations**: O(log n)

```java
Set<String> set = new TreeSet<>();
set.add("C");
set.add("A");
set.add("B");
// Iteration order: A, B, C (sorted order)

// Custom comparator
Set<String> reverseSet = new TreeSet<>(Collections.reverseOrder());
reverseSet.addAll(Arrays.asList("C", "A", "B"));
// Iteration order: C, B, A
```

**NavigableSet Methods**:
```java
NavigableSet<Integer> set = new TreeSet<>();
set.addAll(Arrays.asList(1, 3, 5, 7, 9));

Integer lower = set.lower(5);      // 3 (greatest < 5)
Integer floor = set.floor(5);      // 5 (greatest <= 5)
Integer ceiling = set.ceiling(6);  // 7 (smallest >= 6)
Integer higher = set.higher(5);    // 7 (smallest > 5)

// Range views
SortedSet<Integer> headSet = set.headSet(5);    // [1, 3]
SortedSet<Integer> tailSet = set.tailSet(5);    // [5, 7, 9]
SortedSet<Integer> subSet = set.subSet(3, 8);   // [3, 5, 7]
```

---

## 4. Queue Interface {#queue}

### PriorityQueue

**Internal Structure**: Binary heap (array-based)
**Default**: Min-heap (smallest element first)
**Not thread-safe**

```java
// Min-heap (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(2);
minHeap.offer(8);
minHeap.offer(1);

while (!minHeap.isEmpty()) {
    System.out.println(minHeap.poll()); // 1, 2, 5, 8
}

// Max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// Custom comparator
PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> a.length() - b.length());
```

**Key Methods & Time Complexity**:
```java
PriorityQueue<Integer> pq = new PriorityQueue<>();

// Add operations
pq.offer(5);               // O(log n) - heapify up
pq.add(3);                 // O(log n) - same as offer

// Access operations
Integer head = pq.peek();  // O(1) - view min/max
Integer polled = pq.poll(); // O(log n) - remove and heapify down

// Search operations
boolean exists = pq.contains(5); // O(n) - linear search

// Size operations
int size = pq.size();      // O(1)
```

**Internal Heap Operations**:
```java
// Heap array representation for [1, 2, 5, 8]
// Index:  0  1  2  3
// Array: [1, 2, 5, 8]
//
// Parent of index i: (i-1)/2
// Left child of i: 2*i + 1
// Right child of i: 2*i + 2
```

### ArrayDeque

**Internal Structure**: Resizable circular array
**Double-ended queue**: Add/remove from both ends
**Preferred over Stack and LinkedList for stack/queue operations**

```java
Deque<String> deque = new ArrayDeque<>();

// Queue operations (FIFO)
deque.offer("A");          // Add to tail
deque.offer("B");
String head = deque.poll(); // Remove from head: "A"

// Stack operations (LIFO)
deque.push("C");           // Add to head
deque.push("D");
String top = deque.pop();  // Remove from head: "D"

// Deque-specific operations
deque.addFirst("E");       // Add to head
deque.addLast("F");        // Add to tail
String first = deque.removeFirst(); // Remove from head
String last = deque.removeLast();   // Remove from tail
```

**Key Methods & Time Complexity**:
```java
ArrayDeque<String> deque = new ArrayDeque<>();

// All add/remove operations at ends: O(1)
deque.addFirst("A");       // O(1)
deque.addLast("B");        // O(1)
deque.removeFirst();       // O(1)
deque.removeLast();        // O(1)

// Access operations
String first = deque.peekFirst();  // O(1)
String last = deque.peekLast();    // O(1)

// Search operations
boolean exists = deque.contains("A"); // O(n)
```

---

## 5. Map Interface {#map}

### HashMap

**Internal Structure**: Array of buckets (Node[])
**Hash Function**: `hashCode()` and `equals()`
**Load Factor**: 0.75
**Initial Capacity**: 16
**Collision Resolution**: Chaining (linked list → tree when size > 8)

```java
public class HashMap<K,V> extends AbstractMap<K,V> {
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;
    }
    
    transient Node<K,V>[] table;
    transient int size;
    int threshold; // capacity * loadFactor
}
```

**Key Methods & Time Complexity**:
```java
Map<String, Integer> map = new HashMap<>();

// Put operations
map.put("A", 1);           // O(1) average, O(n) worst case
map.putAll(Map.of("B", 2, "C", 3)); // O(m) where m is added entries
Integer old = map.put("A", 10);     // Returns old value: 1

// Get operations
Integer value = map.get("A");       // O(1) average, O(n) worst case
Integer defaultVal = map.getOrDefault("D", 0); // O(1) average

// Remove operations
Integer removed = map.remove("A");  // O(1) average, O(n) worst case
boolean removed2 = map.remove("B", 2); // Remove only if value matches

// Utility operations
boolean hasKey = map.containsKey("A");    // O(1) average
boolean hasValue = map.containsValue(1);  // O(n) - scans all values
int size = map.size();                    // O(1)
```

**Hash Collision Example**:
```java
// Poor hash function causing collisions
class BadKey {
    String value;
    
    @Override
    public int hashCode() {
        return 1; // All objects hash to same bucket!
    }
}

// Good hash function
class GoodKey {
    String value;
    
    @Override
    public int hashCode() {
        return Objects.hash(value); // Good distribution
    }
}
```

**Java 8+ Tree Optimization**:
```java
// When bucket has > 8 elements, converts to Red-Black tree
// Improves worst-case from O(n) to O(log n)
// Converts back to linked list when < 6 elements
```

### LinkedHashMap

**Extends HashMap**
**Maintains insertion order** (or access order)
**Internal Structure**: HashMap + Doubly-linked list

```java
// Insertion order (default)
Map<String, Integer> map = new LinkedHashMap<>();
map.put("C", 3);
map.put("A", 1);
map.put("B", 2);
// Iteration order: C, A, B

// Access order (LRU cache behavior)
Map<String, Integer> lruMap = new LinkedHashMap<>(16, 0.75f, true);
lruMap.put("A", 1);
lruMap.put("B", 2);
lruMap.put("C", 3);
lruMap.get("A"); // A becomes most recently used
// Iteration order: B, C, A
```

**LRU Cache Implementation**:
```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCache(int capacity) {
        super(capacity + 1, 1.0f, true); // access order
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("A", 1);
cache.put("B", 2);
cache.put("C", 3);
cache.put("D", 4); // "A" is evicted
```

### TreeMap

**Internal Structure**: Red-Black Tree
**Sorted order**: Natural ordering or custom Comparator
**All operations**: O(log n)

```java
Map<String, Integer> map = new TreeMap<>();
map.put("C", 3);
map.put("A", 1);
map.put("B", 2);
// Iteration order: A, B, C (sorted by keys)

// Custom comparator
Map<String, Integer> reverseMap = new TreeMap<>(Collections.reverseOrder());
reverseMap.putAll(map);
// Iteration order: C, B, A
```

**NavigableMap Methods**:
```java
NavigableMap<Integer, String> map = new TreeMap<>();
map.put(1, "One");
map.put(3, "Three");
map.put(5, "Five");
map.put(7, "Seven");

// Navigation methods
Map.Entry<Integer, String> lower = map.lowerEntry(5);     // (3, "Three")
Map.Entry<Integer, String> floor = map.floorEntry(5);     // (5, "Five")
Map.Entry<Integer, String> ceiling = map.ceilingEntry(6); // (7, "Seven")
Map.Entry<Integer, String> higher = map.higherEntry(5);   // (7, "Seven")

// Range views
SortedMap<Integer, String> headMap = map.headMap(5);      // {1=One, 3=Three}
SortedMap<Integer, String> tailMap = map.tailMap(5);      // {5=Five, 7=Seven}
SortedMap<Integer, String> subMap = map.subMap(2, 6);     // {3=Three, 5=Five}
```

### Hashtable

**Legacy class** (since Java 1.0)
**Thread-safe** (synchronized methods)
**No null keys or values**

```java
Hashtable<String, Integer> table = new Hashtable<>();
table.put("A", 1);         // Synchronized
// table.put(null, 1);     // NullPointerException
// table.put("A", null);   // NullPointerException
```

**Differences from HashMap**:
- Synchronized (thread-safe but slower)
- No null keys/values
- Legacy methods: `elements()`, `keys()`

---

## 6. Concurrent Collections {#concurrent}

### ConcurrentHashMap

**Thread-safe HashMap replacement**
**Segment-based locking** (Java 7) → **CAS operations** (Java 8+)
**Better performance than synchronized HashMap**

```java
ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();

// Thread-safe operations
map.put("A", 1);           // Thread-safe
map.putIfAbsent("B", 2);   // Atomic operation
map.replace("A", 1, 10);   // Atomic conditional replace
map.compute("C", (k, v) -> v == null ? 1 : v + 1); // Atomic computation

// Bulk operations (Java 8+)
map.forEach((k, v) -> System.out.println(k + "=" + v));
map.replaceAll((k, v) -> v * 2);
Integer sum = map.reduceValues(1, Integer::sum);
```

**Key Features**:
- **No locking for reads** (most of the time)
- **CAS operations** for updates
- **Segment-based** concurrency
- **Fail-safe iterators** (don't throw ConcurrentModificationException)

**Performance Comparison**:
```java
// Synchronized HashMap (poor performance)
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// ConcurrentHashMap (better performance)
ConcurrentMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();

// For high-concurrency scenarios, ConcurrentHashMap is 2-5x faster
```

### CopyOnWriteArrayList

**Thread-safe List**
**Copy-on-write semantics**: Creates new array for modifications
**Best for read-heavy scenarios**

```java
List<String> list = new CopyOnWriteArrayList<>();

// Write operations (expensive - copies entire array)
list.add("A");             // Creates new array
list.add("B");             // Creates new array
list.set(0, "C");          // Creates new array

// Read operations (fast - no locking)
String item = list.get(0); // No locking, reads from snapshot
for (String s : list) {    // Iterator uses snapshot
    System.out.println(s); // Won't see concurrent modifications
}
```

**Use Cases**:
- **Read-heavy workloads** (90%+ reads)
- **Small collections** (copying cost is acceptable)
- **Event listeners** (rarely modified, frequently iterated)

### CopyOnWriteArraySet

**Thread-safe Set**
**Backed by CopyOnWriteArrayList**
**Same characteristics as CopyOnWriteArrayList**

```java
Set<String> set = new CopyOnWriteArraySet<>();
set.add("A");              // O(n) - checks for duplicates + copy
boolean exists = set.contains("A"); // O(n) - linear search
```

### BlockingQueue Implementations

#### ArrayBlockingQueue

**Bounded blocking queue**
**Fixed capacity**
**FIFO ordering**

```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// Producer thread
queue.put("item");         // Blocks if queue is full
queue.offer("item", 1, TimeUnit.SECONDS); // Timeout version

// Consumer thread
String item = queue.take(); // Blocks if queue is empty
String item2 = queue.poll(1, TimeUnit.SECONDS); // Timeout version
```

#### LinkedBlockingQueue

**Optionally bounded**
**Linked list implementation**
**Better throughput than ArrayBlockingQueue**

```java
BlockingQueue<String> queue = new LinkedBlockingQueue<>(); // Unbounded
BlockingQueue<String> bounded = new LinkedBlockingQueue<>(100); // Bounded

// Same blocking operations as ArrayBlockingQueue
queue.put("item");
String item = queue.take();
```

#### PriorityBlockingQueue

**Unbounded blocking priority queue**
**Elements ordered by priority**
**Thread-safe PriorityQueue**

```java
BlockingQueue<Integer> queue = new PriorityBlockingQueue<>();
queue.put(5);
queue.put(1);
queue.put(3);

Integer min = queue.take(); // Always returns smallest element: 1
```

#### SynchronousQueue

**Zero capacity**
**Direct handoff between threads**
**Each put must wait for take**

```java
BlockingQueue<String> queue = new SynchronousQueue<>();

// Producer thread
queue.put("item"); // Blocks until consumer takes it

// Consumer thread
String item = queue.take(); // Blocks until producer puts item
```

### ConcurrentLinkedQueue

**Non-blocking queue**
**CAS-based operations**
**Unbounded**

```java
Queue<String> queue = new ConcurrentLinkedQueue<>();

// Non-blocking operations
queue.offer("A");          // Always succeeds (unbounded)
String head = queue.poll(); // Returns null if empty (non-blocking)
String peek = queue.peek(); // Returns null if empty (non-blocking)
```

### ConcurrentSkipListMap/Set

**Thread-safe sorted collections**
**Skip list data structure**
**O(log n) operations**

```java
ConcurrentNavigableMap<String, Integer> map = new ConcurrentSkipListMap<>();
map.put("C", 3);
map.put("A", 1);
map.put("B", 2);
// Maintains sorted order: A, B, C

ConcurrentNavigableSet<String> set = new ConcurrentSkipListSet<>();
set.add("C");
set.add("A");
set.add("B");
// Maintains sorted order: A, B, C
```

---

## 7. Collection Utilities {#utilities}

### Collections Class

**Static utility methods for collections**

```java
List<String> list = Arrays.asList("C", "A", "B");

// Sorting
Collections.sort(list);                    // [A, B, C]
Collections.sort(list, Collections.reverseOrder()); // [C, B, A]

// Searching (requires sorted list)
int index = Collections.binarySearch(list, "B"); // O(log n)

// Min/Max
String min = Collections.min(list);        // A
String max = Collections.max(list);        // C

// Shuffling
Collections.shuffle(list);                 // Random order

// Reversing
Collections.reverse(list);                 // Reverse current order

// Filling
Collections.fill(list, "X");               // [X, X, X]

// Frequency
int count = Collections.frequency(list, "X"); // 3

// Synchronization wrappers
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// Unmodifiable wrappers
List<String> immutableList = Collections.unmodifiableList(list);
Map<String, Integer> immutableMap = Collections.unmodifiableMap(map);

// Empty collections
List<String> emptyList = Collections.emptyList();
Set<String> emptySet = Collections.emptySet();
Map<String, Integer> emptyMap = Collections.emptyMap();

// Singleton collections
List<String> singletonList = Collections.singletonList("A");
Set<String> singletonSet = Collections.singleton("A");
Map<String, Integer> singletonMap = Collections.singletonMap("A", 1);
```

### Arrays Class

**Static utility methods for arrays**

```java
String[] array = {"C", "A", "B"};

// Sorting
Arrays.sort(array);                        // [A, B, C]

// Searching (requires sorted array)
int index = Arrays.binarySearch(array, "B"); // O(log n)

// Converting to List
List<String> list = Arrays.asList(array);  // Fixed-size list

// Copying
String[] copy = Arrays.copyOf(array, array.length);
String[] partial = Arrays.copyOfRange(array, 1, 3); // [B, C]

// Filling
Arrays.fill(array, "X");                   // [X, X, X]

// Equality
boolean equal = Arrays.equals(array1, array2);
boolean deepEqual = Arrays.deepEquals(array2D1, array2D2);

// String representation
String str = Arrays.toString(array);       // [A, B, C]
String deepStr = Arrays.deepToString(array2D); // [[1, 2], [3, 4]]

// Parallel operations (Java 8+)
int[] numbers = {3, 1, 4, 1, 5};
Arrays.parallelSort(numbers);              // Parallel sorting
Arrays.parallelSetAll(numbers, i -> i * 2); // Parallel computation
```

---

## 8. Performance Comparison {#performance}

### Time Complexity Summary

| Collection | Add | Remove | Get | Contains | Space |
|------------|-----|--------|-----|----------|-------|
| ArrayList | O(1)* | O(n) | O(1) | O(n) | O(n) |
| LinkedList | O(1) | O(n) | O(n) | O(n) | O(n) |
| HashSet | O(1)* | O(1)* | N/A | O(1)* | O(n) |
| TreeSet | O(log n) | O(log n) | N/A | O(log n) | O(n) |
| HashMap | O(1)* | O(1)* | O(1)* | O(1)* | O(n) |
| TreeMap | O(log n) | O(log n) | O(log n) | O(log n) | O(n) |
| PriorityQueue | O(log n) | O(log n) | O(1) | O(n) | O(n) |
| ArrayDeque | O(1) | O(1) | O(1) | O(n) | O(n) |

*Amortized time complexity

### Memory Usage Comparison

```java
// Memory overhead per element (approximate)
ArrayList<Integer>:     4 bytes (just reference) + array overhead
LinkedList<Integer>:    24 bytes (object + 2 pointers + reference)
HashMap<String,Integer>: 32 bytes (entry object + references + hash)
TreeMap<String,Integer>: 40 bytes (node object + references + color)
```

### Performance Benchmarks

```java
// Adding 1 million elements
ArrayList:      ~50ms
LinkedList:     ~200ms (due to object creation)
HashSet:        ~100ms
TreeSet:        ~300ms (due to tree balancing)

// Random access (1 million operations)
ArrayList:      ~10ms
LinkedList:     ~30 seconds (O(n) access)

// Iteration (1 million elements)
ArrayList:      ~5ms
LinkedList:     ~10ms
HashMap:        ~15ms
TreeMap:        ~25ms
```

---

## 9. Best Practices {#best-practices}

### Choosing the Right Collection

**List Selection**:
```java
// Use ArrayList when:
// - Random access needed
// - More reads than writes
// - Memory efficiency important
List<String> list = new ArrayList<>();

// Use LinkedList when:
// - Frequent insertions/deletions at beginning/middle
// - Implementing queue/deque
// - Memory is not a concern
List<String> list = new LinkedList<>();

// Use Vector only when:
// - Legacy code compatibility required
// - Simple thread safety needed (prefer concurrent collections)
```

**Set Selection**:
```java
// Use HashSet when:
// - Fast lookups needed
// - No ordering required
Set<String> set = new HashSet<>();

// Use LinkedHashSet when:
// - Fast lookups + insertion order needed
Set<String> set = new LinkedHashSet<>();

// Use TreeSet when:
// - Sorted order needed
// - Range operations needed
Set<String> set = new TreeSet<>();
```

**Map Selection**:
```java
// Use HashMap when:
// - Fast lookups needed
// - No ordering required
Map<String, Integer> map = new HashMap<>();

// Use LinkedHashMap when:
// - Fast lookups + insertion/access order needed
// - LRU cache implementation
Map<String, Integer> map = new LinkedHashMap<>();

// Use TreeMap when:
// - Sorted order needed
// - Range operations needed
Map<String, Integer> map = new TreeMap<>();
```

### Thread Safety Guidelines

```java
// For single-threaded applications
List<String> list = new ArrayList<>();
Map<String, Integer> map = new HashMap<>();

// For multi-threaded applications (high concurrency)
List<String> list = new CopyOnWriteArrayList<>(); // Read-heavy
Map<String, Integer> map = new ConcurrentHashMap<>();

// For multi-threaded applications (simple synchronization)
List<String> list = Collections.synchronizedList(new ArrayList<>());
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

// Always synchronize iteration for synchronized collections
synchronized(list) {
    for (String item : list) {
        // Process item
    }
}
```

### Memory and Performance Optimization

```java
// Specify initial capacity to avoid resizing
List<String> list = new ArrayList<>(1000);
Map<String, Integer> map = new HashMap<>(1000);
Set<String> set = new HashSet<>(1000);

// Use appropriate load factor for HashMap
Map<String, Integer> map = new HashMap<>(1000, 0.75f);

// Trim to size after bulk operations
ArrayList<String> list = new ArrayList<>();
// ... add many elements
list.trimToSize(); // Reduce capacity to current size

// Use primitive collections for better performance (external libraries)
// TIntArrayList instead of ArrayList<Integer>
// TIntObjectHashMap instead of HashMap<Integer, Object>
```

### Common Pitfalls and Solutions

```java
// 1. ConcurrentModificationException
List<String> list = Arrays.asList("A", "B", "C");
// Wrong:
for (String item : list) {
    if (item.equals("B")) {
        list.remove(item); // ConcurrentModificationException
    }
}

// Correct:
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("B")) {
        it.remove(); // Safe removal
    }
}

// Or use removeIf (Java 8+)
list.removeIf(item -> item.equals("B"));

// 2. Null handling in collections
Map<String, Integer> map = new HashMap<>();
map.put("A", null);
Integer value = map.get("A");        // null
Integer value2 = map.get("B");       // null
// Can't distinguish between null value and missing key

// Solution: Use getOrDefault or containsKey
Integer value = map.getOrDefault("B", -1); // -1 if missing
if (map.containsKey("B")) {
    Integer value = map.get("B"); // Now we know it exists
}

// 3. Modifying collection during iteration
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
// Wrong:
for (int i = 0; i < list.size(); i++) {
    if (list.get(i).equals("B")) {
        list.remove(i); // Index shifts, might skip elements
    }
}

// Correct:
for (int i = list.size() - 1; i >= 0; i--) {
    if (list.get(i).equals("B")) {
        list.remove(i); // Safe when iterating backwards
    }
}
```

### Modern Java Features (Java 8+)

```java
// Stream API with collections
List<String> list = Arrays.asList("apple", "banana", "cherry");
List<String> filtered = list.stream()
    .filter(s -> s.startsWith("a"))
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// Collection factory methods (Java 9+)
List<String> list = List.of("A", "B", "C");           // Immutable
Set<String> set = Set.of("A", "B", "C");              // Immutable
Map<String, Integer> map = Map.of("A", 1, "B", 2);    // Immutable

// Collection convenience methods
List<String> list = new ArrayList<>();
list.addAll(List.of("A", "B", "C"));

// forEach with lambda
list.forEach(System.out::println);
map.forEach((k, v) -> System.out.println(k + "=" + v));

// removeIf
list.removeIf(s -> s.startsWith("A"));

// replaceAll
list.replaceAll(String::toUpperCase);
```

---

## Summary

Java Collections Framework provides:

1. **Comprehensive data structures** for different use cases
2. **Thread-safe alternatives** for concurrent applications
3. **Rich utility methods** for common operations
4. **Performance optimizations** for different scenarios
5. **Modern features** with streams and lambda expressions

**Key Takeaways**:
- Choose collections based on access patterns and performance requirements
- Use concurrent collections for multi-threaded applications
- Understand time complexity trade-offs
- Leverage modern Java features for cleaner code
- Always consider memory usage and initialization parameters

This guide covers all major collections with their internal workings, use cases, and best practices for building efficient Java applications.