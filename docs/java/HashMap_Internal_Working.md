# HashMap - Internal Working & Hash Collision Handling

## Table of Contents
1. [Introduction](#introduction)
2. [Internal Structure](#internal-structure)
3. [Hash Function](#hash-function)
4. [Core Operations](#core-operations)
5. [Hash Collision](#hash-collision)
6. [Collision Handling](#collision-handling)
7. [Resizing & Rehashing](#resizing--rehashing)
8. [Examples](#examples)
9. [Performance Analysis](#performance-analysis)

---

## Introduction

### What is HashMap?

HashMap is a **hash table-based implementation** of the Map interface that stores key-value pairs.

**Key Characteristics:**
- Stores data as key-value pairs
- Allows one null key and multiple null values
- Not synchronized (not thread-safe)
- No ordering guarantee
- Average O(1) time complexity for get/put

**Basic Usage:**
```java
Map<String, Integer> map = new HashMap<>();
map.put("John", 25);
map.put("Jane", 30);
Integer age = map.get("John");  // 25
```

---

## Internal Structure

### Core Components

```java
public class HashMap<K,V> {
    // Array of buckets
    transient Node<K,V>[] table;
    
    // Number of key-value pairs
    transient int size;
    
    // Resize threshold (capacity * loadFactor)
    int threshold;
    
    // Load factor for resizing
    final float loadFactor;
    
    // Modification counter (for fail-fast)
    transient int modCount;
}
```

### Node Structure

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;      // Cached hash value
    final K key;         // Key
    V value;             // Value
    Node<K,V> next;      // Next node in chain (for collisions)
    
    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```

### Visual Representation

```
HashMap (capacity = 16, size = 5)
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│  0  │  1  │  2  │  3  │  4  │  5  │  6  │  7  │  8  │  9  │ 10  │ 11  │ 12  │ 13  │ 14  │ 15  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
   │                       │           │                               │
   │                       │           │                               │
   ▼                       ▼           ▼                               ▼
[A,1]→null          [B,2]→[F,6]   [C,3]→null                    [D,4]→[E,5]→null
                         (collision)                                  (collision)
```

---

## Hash Function

### Step 1: Get hashCode()

```java
String key = "John";
int hashCode = key.hashCode();  // Returns integer
```

### Step 2: HashMap's Hash Function

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**Why XOR with right shift?**
- Spreads high bits to low bits
- Reduces collisions
- Better distribution

**Example:**
```java
// Original hashCode
hashCode:  1111 1111 1111 1111 0000 0000 0000 0000

// Right shift by 16
>>> 16:    0000 0000 0000 0000 1111 1111 1111 1111

// XOR result
XOR:       1111 1111 1111 1111 1111 1111 1111 1111
```

### Step 3: Calculate Bucket Index

```java
int index = (n - 1) & hash;  // n = table.length
```

**Why (n - 1) & hash instead of hash % n?**
- Bitwise AND is faster than modulo
- Works because capacity is always power of 2

**Example:**
```java
// Capacity = 16, n - 1 = 15 = 0000 1111 (binary)
hash:      1010 1100 1101 0011
n - 1:     0000 0000 0000 1111
AND:       0000 0000 0000 0011  → index = 3
```

---

## Core Operations

### 1. PUT Operation

```java
map.put("John", 25);
```

**Step-by-Step Process:**

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    
    // Step 1: Initialize table if empty
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    // Step 2: Calculate index
    i = (n - 1) & hash;
    
    // Step 3: Check if bucket is empty
    if ((p = tab[i]) == null) {
        // No collision - create new node
        tab[i] = newNode(hash, key, value, null);
    } else {
        // Collision detected
        Node<K,V> e; K k;
        
        // Case 1: Key exists at head
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
            e = p;
        }
        // Case 2: Tree node (Java 8+)
        else if (p instanceof TreeNode) {
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        }
        // Case 3: Linked list
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    // Add to end of list
                    p.next = newNode(hash, key, value, null);
                    
                    // Convert to tree if threshold reached
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);
                    break;
                }
                // Key found in list
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        
        // Update existing key
        if (e != null) {
            V oldValue = e.value;
            e.value = value;
            return oldValue;
        }
    }
    
    // Step 4: Increment size and check resize
    if (++size > threshold)
        resize();
    
    return null;
}
```

**Visual Example:**

```
// put("John", 25) - hash → index 5
Before:
bucket[5]: null

After:
bucket[5]: [John, 25] → null

// put("Jane", 30) - hash → index 5 (COLLISION!)
Before:
bucket[5]: [John, 25] → null

After:
bucket[5]: [John, 25] → [Jane, 30] → null
```

---

### 2. GET Operation

```java
Integer age = map.get("John");
```

**Step-by-Step Process:**

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    
    // Step 1: Check table exists and bucket not empty
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        
        // Step 2: Check first node
        if (first.hash == hash &&
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        
        // Step 3: Check remaining nodes
        if ((e = first.next) != null) {
            // Tree structure
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            
            // Linked list - traverse
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

**Visual Example:**

```
// get("Jane") - hash → index 5
bucket[5]: [John, 25] → [Jane, 30] → null

Step 1: Go to bucket[5]
Step 2: Check first node: "John" != "Jane"
Step 3: Check next node: "Jane" == "Jane" ✓
Step 4: Return 30
```

---

### 3. REMOVE Operation

```java
map.remove("John");
```

**Process:**
1. Calculate hash and index
2. Find node in bucket
3. Remove node from chain
4. Decrement size

```java
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false, true)) == null ?
        null : e.value;
}
```

---

## Hash Collision

### What is Hash Collision?

**Collision occurs when two different keys produce the same bucket index.**

```java
// Example collision
String key1 = "Aa";
String key2 = "BB";

key1.hashCode();  // 2112
key2.hashCode();  // 2112  (SAME!)

// Both map to same bucket
int index1 = (16 - 1) & hash(key1);  // 0
int index2 = (16 - 1) & hash(key2);  // 0  (COLLISION!)
```

### Why Collisions Happen?

1. **Limited bucket space**: 16 buckets (default) vs infinite possible keys
2. **Hash function limitations**: Perfect hash function impossible
3. **Poor hash distribution**: Bad hashCode() implementation

### Types of Collisions

#### 1. Hash Code Collision
```java
// Different keys, same hashCode
"Aa".hashCode() == "BB".hashCode()  // true (2112)
```

#### 2. Bucket Index Collision
```java
// Different hashCodes, same bucket
hash1 = 17;  // index = 17 & 15 = 1
hash2 = 33;  // index = 33 & 15 = 1  (same bucket)
```

---

## Collision Handling

### Method 1: Separate Chaining (HashMap uses this)

**Concept:** Store colliding elements in a linked list at the same bucket.

```
bucket[5]: [key1, val1] → [key2, val2] → [key3, val3] → null
           (all have same bucket index)
```

### Java 7: Linked List Only

```java
// All collisions stored in linked list
bucket[5]: [A,1] → [B,2] → [C,3] → [D,4] → [E,5] → null

// Worst case: O(n) lookup
```

**Problem:** If many collisions, performance degrades to O(n).

---

### Java 8+: Linked List + Red-Black Tree

**Optimization:** Convert linked list to balanced tree when chain length ≥ 8.

```java
static final int TREEIFY_THRESHOLD = 8;      // List → Tree
static final int UNTREEIFY_THRESHOLD = 6;    // Tree → List
static final int MIN_TREEIFY_CAPACITY = 64;  // Min capacity for treeify
```

**Before Treeify (Linked List):**
```
bucket[5]: [A,1] → [B,2] → [C,3] → [D,4] → [E,5] → [F,6] → [G,7] → [H,8] → null
           O(n) lookup - worst case
```

**After Treeify (Red-Black Tree):**
```
bucket[5]:              [D,4]
                       /      \
                   [B,2]      [F,6]
                   /   \      /   \
               [A,1] [C,3] [E,5] [G,7]
                                    \
                                   [H,8]
           O(log n) lookup - improved!
```

**TreeNode Structure:**
```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<K,V> parent;
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;
    boolean red;  // Red-black tree color
}
```

---

### Collision Handling Example

```java
Map<String, Integer> map = new HashMap<>();

// All these keys collide to same bucket (for demonstration)
map.put("Aa", 1);   // hash → bucket 0
map.put("BB", 2);   // hash → bucket 0 (collision!)
map.put("CC", 3);   // hash → bucket 0 (collision!)

// Internal structure:
// bucket[0]: [Aa,1] → [BB,2] → [CC,3] → null

// Get operation
Integer value = map.get("BB");
// 1. Calculate hash("BB") → bucket 0
// 2. Traverse list: "Aa" != "BB", "BB" == "BB" ✓
// 3. Return 2
```

---

## Resizing & Rehashing

### When Resize Happens

**Trigger:** `size > threshold`

```java
threshold = capacity × loadFactor
// Default: 16 × 0.75 = 12
```

**Example:**
```java
HashMap<String, Integer> map = new HashMap<>();
// Initial: capacity = 16, threshold = 12

// Add 12 elements - OK
for (int i = 0; i < 12; i++) {
    map.put("key" + i, i);
}

// Add 13th element - triggers resize!
map.put("key12", 12);
// New: capacity = 32, threshold = 24
```

---

### Resize Process

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    
    int newCap, newThr = 0;
    
    if (oldCap > 0) {
        // Double the capacity
        newCap = oldCap << 1;  // oldCap * 2
        newThr = oldThr << 1;  // oldThr * 2
    } else {
        // Initial capacity
        newCap = DEFAULT_INITIAL_CAPACITY;  // 16
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);  // 12
    }
    
    threshold = newThr;
    Node<K,V>[] newTab = new Node[newCap];
    table = newTab;
    
    // Rehash all existing entries
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                
                if (e.next == null) {
                    // Single node - recalculate index
                    newTab[e.hash & (newCap - 1)] = e;
                } else if (e instanceof TreeNode) {
                    // Split tree
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                } else {
                    // Split linked list
                    // Elements stay at same index or move to (index + oldCap)
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            // Stay at same index
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        } else {
                            // Move to (index + oldCap)
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

---

### Resize Example

```
Before Resize (capacity = 4, size = 4):
┌─────┬─────┬─────┬─────┐
│  0  │  1  │  2  │  3  │
└─────┴─────┴─────┴─────┘
   │     │     │     │
  [A]   [B]   [C]   [D]

After Resize (capacity = 8):
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│  0  │  1  │  2  │  3  │  4  │  5  │  6  │  7  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
   │           │           │           │
  [A]         [B]         [C]         [D]

Elements redistributed based on new capacity
```

---

## Examples

### Example 1: Basic Operations

```java
import java.util.*;

public class HashMapExample {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        
        // PUT operations
        map.put("John", 25);
        map.put("Jane", 30);
        map.put("Bob", 35);
        
        // GET operation
        System.out.println(map.get("Jane"));  // 30
        
        // UPDATE operation
        map.put("John", 26);  // Updates existing key
        System.out.println(map.get("John"));  // 26
        
        // REMOVE operation
        map.remove("Bob");
        System.out.println(map.get("Bob"));  // null
        
        // CHECK operations
        System.out.println(map.containsKey("Jane"));    // true
        System.out.println(map.containsValue(30));      // true
        
        // SIZE
        System.out.println(map.size());  // 2
    }
}
```

---

### Example 2: Collision Demonstration

```java
public class CollisionDemo {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        
        // These strings have same hashCode (collision)
        String key1 = "Aa";
        String key2 = "BB";
        
        System.out.println(key1.hashCode());  // 2112
        System.out.println(key2.hashCode());  // 2112 (SAME!)
        
        // Both stored in same bucket, but different nodes
        map.put(key1, 1);
        map.put(key2, 2);
        
        // HashMap handles collision internally
        System.out.println(map.get(key1));  // 1
        System.out.println(map.get(key2));  // 2
        
        // Both keys exist despite collision
        System.out.println(map.size());  // 2
    }
}
```

---

### Example 3: Custom Object as Key

```java
class Person {
    String name;
    int age;
    
    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // MUST override hashCode and equals
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Person person = (Person) obj;
        return age == person.age && Objects.equals(name, person.name);
    }
}

public class CustomKeyExample {
    public static void main(String[] args) {
        Map<Person, String> map = new HashMap<>();
        
        Person p1 = new Person("John", 25);
        Person p2 = new Person("John", 25);  // Same content as p1
        
        map.put(p1, "Engineer");
        
        // p2 equals p1, so retrieves same value
        System.out.println(map.get(p2));  // Engineer
        
        // Only one entry (p1 and p2 are equal)
        System.out.println(map.size());  // 1
    }
}
```

---

### Example 4: Iteration

```java
public class IterationExample {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        
        // Method 1: entrySet()
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        
        // Method 2: keySet()
        for (String key : map.keySet()) {
            System.out.println(key + " = " + map.get(key));
        }
        
        // Method 3: values()
        for (Integer value : map.values()) {
            System.out.println(value);
        }
        
        // Method 4: forEach (Java 8+)
        map.forEach((key, value) -> 
            System.out.println(key + " = " + value)
        );
    }
}
```

---

### Example 5: Resize Observation

```java
public class ResizeDemo {
    public static void main(String[] args) {
        // Initial capacity = 4, loadFactor = 0.75
        Map<String, Integer> map = new HashMap<>(4, 0.75f);
        
        System.out.println("Adding elements...");
        
        // Threshold = 4 * 0.75 = 3
        map.put("A", 1);  // size = 1
        map.put("B", 2);  // size = 2
        map.put("C", 3);  // size = 3
        
        System.out.println("Size: " + map.size());  // 3
        
        // Next put triggers resize (3 >= 3)
        map.put("D", 4);  // Resize! New capacity = 8
        
        System.out.println("Size after resize: " + map.size());  // 4
    }
}
```

---

## Performance Analysis

### Time Complexity

| Operation | Average | Worst Case (Java 7) | Worst Case (Java 8+) |
|-----------|---------|---------------------|----------------------|
| get()     | O(1)    | O(n)                | O(log n)             |
| put()     | O(1)    | O(n)                | O(log n)             |
| remove()  | O(1)    | O(n)                | O(log n)             |
| containsKey() | O(1) | O(n)               | O(log n)             |

**Assumptions:**
- Good hash function
- Proper load factor
- Uniform distribution

---

### Space Complexity

**O(n)** where n = number of entries

**Memory overhead:**
- Array of buckets
- Node objects (hash, key, value, next)
- Empty buckets waste space

---

### Load Factor Impact

```java
// Low load factor (0.5)
Map<String, Integer> map1 = new HashMap<>(16, 0.5f);
// Pros: Fewer collisions, faster lookups
// Cons: More memory waste, frequent resizing

// High load factor (0.9)
Map<String, Integer> map2 = new HashMap<>(16, 0.9f);
// Pros: Less memory waste, fewer resizes
// Cons: More collisions, slower lookups

// Default (0.75) - optimal balance
Map<String, Integer> map3 = new HashMap<>();
```

---

## Important Constants

```java
// Default initial capacity (must be power of 2)
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;  // 16

// Maximum capacity
static final int MAXIMUM_CAPACITY = 1 << 30;  // 2^30

// Default load factor
static final float DEFAULT_LOAD_FACTOR = 0.75f;

// Treeify threshold (list → tree)
static final int TREEIFY_THRESHOLD = 8;

// Untreeify threshold (tree → list)
static final int UNTREEIFY_THRESHOLD = 6;

// Minimum capacity for treeify
static final int MIN_TREEIFY_CAPACITY = 64;
```

---

## Best Practices

### 1. Specify Initial Capacity

```java
// Bad: Multiple resizes
Map<String, Integer> map1 = new HashMap<>();
for (int i = 0; i < 1000; i++) {
    map1.put("key" + i, i);  // Multiple resizes!
}

// Good: Pre-size to avoid resizes
Map<String, Integer> map2 = new HashMap<>(1500);  // 1000 / 0.75 ≈ 1334
for (int i = 0; i < 1000; i++) {
    map2.put("key" + i, i);  // No resizes!
}
```

### 2. Override hashCode() and equals()

```java
class Key {
    String id;
    
    // MUST override both
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Key key = (Key) obj;
        return Objects.equals(id, key.id);
    }
}
```

### 3. Use Immutable Keys

```java
// Good: Immutable key
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);

// Bad: Mutable key
class MutableKey {
    int value;
    // hashCode changes if value changes!
}
```

### 4. Avoid Null Keys in Production

```java
// Allowed but not recommended
map.put(null, 1);

// Better: Use Optional or default key
map.put("DEFAULT", 1);
```

---

## Common Pitfalls

### 1. Modifying Key After Insertion

```java
class MutableKey {
    int value;
    
    @Override
    public int hashCode() {
        return value;
    }
}

MutableKey key = new MutableKey();
key.value = 10;
map.put(key, "data");

// Modify key - BREAKS HashMap!
key.value = 20;  // hashCode changes!

// Can't find entry anymore
System.out.println(map.get(key));  // null (wrong bucket!)
```

### 2. Poor hashCode() Implementation

```java
// Bad: All objects map to same bucket
@Override
public int hashCode() {
    return 1;  // All collisions!
}

// Good: Distribute evenly
@Override
public int hashCode() {
    return Objects.hash(field1, field2, field3);
}
```

### 3. ConcurrentModificationException

```java
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);

// Throws exception
for (String key : map.keySet()) {
    map.remove(key);  // Modifying during iteration!
}

// Solution: Use iterator
Iterator<String> it = map.keySet().iterator();
while (it.hasNext()) {
    it.next();
    it.remove();  // Safe removal
}
```

---

## Summary

### Key Points

1. **HashMap uses array + linked list/tree** for storage
2. **Hash function** converts key to bucket index
3. **Collisions handled** by separate chaining (linked list or red-black tree)
4. **Java 8+ optimization**: list → tree when chain length ≥ 8
5. **Resizing** doubles capacity when size > threshold (capacity × 0.75)
6. **Average O(1)** for get/put, worst case O(log n) in Java 8+
7. **Not thread-safe** - use ConcurrentHashMap for concurrency
8. **Allows one null key** and multiple null values

### When to Use HashMap

✅ **Use HashMap when:**
- Need fast key-value lookups
- Order doesn't matter
- Single-threaded or externally synchronized
- Keys are immutable

❌ **Don't use HashMap when:**
- Need thread-safety (use ConcurrentHashMap)
- Need ordering (use LinkedHashMap or TreeMap)
- Need sorted keys (use TreeMap)

---

## References

- [HashMap JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html)
- [Java Collections Framework](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/)
- [Effective Java by Joshua Bloch](https://www.oreilly.com/library/view/effective-java/9780134686097/)

---

**Last Updated**: 2024
**Java Versions**: 7, 8, 11, 17, 21
