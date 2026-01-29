# Custom HashMap Implementation - Deep Dive

## Overview
A HashMap is a data structure that stores key-value pairs and provides O(1) average time complexity for insert, delete, and lookup operations. It uses hashing to map keys to array indices.

**Key Characteristics**:
- Stores key-value pairs
- Uses hash function to compute array index
- Handles collisions using separate chaining or open addressing
- Dynamic resizing when load factor exceeds threshold
- Average O(1) time complexity for basic operations

---

## Internal Working

### Hash Function

A hash function converts a key into an array index:

```
hash(key) = key.hashCode() % array.length
```

**Example**:
```
Array size = 16
Key = "apple"
hashCode("apple") = 93029210
Index = 93029210 % 16 = 10

Store "apple" → "fruit" at index 10
```

### Collision Handling

When two keys hash to the same index, we have a collision.

#### Separate Chaining (Linked List)

```
Index 0: null
Index 1: [key1=val1] → [key2=val2] → null
Index 2: [key3=val3] → null
Index 3: null
```

**Collision Example**:
```
hash("apple") = 10
hash("banana") = 10  ← Collision!

Index 10: [apple=fruit] → [banana=yellow] → null
```

### Load Factor & Resizing

**Load Factor** = Number of entries / Array capacity

```
Load Factor = 12 / 16 = 0.75
```

When load factor exceeds threshold (typically 0.75):
1. Create new array (2x size)
2. Rehash all existing entries
3. Insert into new array

**Example**:
```
Old array (size 4):
[0]: [a=1]
[1]: [b=2] → [c=3]
[2]: null
[3]: [d=4]

Resize to 8:
[0]: [a=1]
[1]: null
[2]: [b=2]
[3]: [c=3]
[4]: null
[5]: null
[6]: null
[7]: [d=4]
```

---

## Implementation 1: Custom HashMap (No Inbuilt HashMap)

### Complete Implementation

```java
package org.sudhir512kj.datastructures;

/**
 * Custom HashMap implementation without using inbuilt HashMap
 * Uses separate chaining for collision resolution
 */
public class CustomHashMap<K, V> {
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    
    private Node<K, V>[] buckets;
    private int size;
    
    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    @SuppressWarnings("unchecked")
    public CustomHashMap() {
        buckets = new Node[DEFAULT_CAPACITY];
        size = 0;
    }
    
    @SuppressWarnings("unchecked")
    public CustomHashMap(int capacity) {
        buckets = new Node[capacity];
        size = 0;
    }
    
    private int hash(K key) {
        return key == null ? 0 : Math.abs(key.hashCode() % buckets.length);
    }
    
    public void put(K key, V value) {
        if (size >= buckets.length * LOAD_FACTOR) {
            resize();
        }
        
        int index = hash(key);
        Node<K, V> head = buckets[index];
        
        // Update if key exists
        Node<K, V> current = head;
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        
        // Add new node at head
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = head;
        buckets[index] = newNode;
        size++;
    }
    
    public V get(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }
    
    public V remove(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;
        
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null;
    }
    
    public boolean containsKey(K key) {
        return get(key) != null;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    @SuppressWarnings("unchecked")
    private void resize() {
        Node<K, V>[] oldBuckets = buckets;
        buckets = new Node[oldBuckets.length * 2];
        size = 0;
        
        for (Node<K, V> head : oldBuckets) {
            Node<K, V> current = head;
            while (current != null) {
                put(current.key, current.value);
                current = current.next;
            }
        }
    }
    
    public void display() {
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] != null) {
                System.out.print("Bucket " + i + ": ");
                Node<K, V> current = buckets[i];
                while (current != null) {
                    System.out.print("[" + current.key + "=" + current.value + "] → ");
                    current = current.next;
                }
                System.out.println("null");
            }
        }
    }
}
```

### Usage Example

```java
public class CustomHashMapTest {
    public static void main(String[] args) {
        CustomHashMap<String, Integer> map = new CustomHashMap<>();
        
        // Put operations
        map.put("apple", 100);
        map.put("banana", 200);
        map.put("cherry", 300);
        map.put("date", 400);
        
        // Get operations
        System.out.println("apple: " + map.get("apple"));    // 100
        System.out.println("banana: " + map.get("banana"));  // 200
        
        // Update existing key
        map.put("apple", 150);
        System.out.println("apple: " + map.get("apple"));    // 150
        
        // Remove operation
        map.remove("banana");
        System.out.println("banana: " + map.get("banana"));  // null
        
        // Size and containsKey
        System.out.println("Size: " + map.size());           // 3
        System.out.println("Contains cherry: " + map.containsKey("cherry")); // true
        
        // Display internal structure
        map.display();
    }
}
```

**Output**:
```
apple: 100
banana: 200
apple: 150
banana: null
Size: 3
Contains cherry: true
Bucket 5: [date=400] → null
Bucket 10: [cherry=300] → null
Bucket 12: [apple=150] → null
```

---

## Implementation 2: HashMap with Bucket Limit (FIFO Eviction)

### Problem Statement

Implement a HashMap where each bucket can hold a maximum of 3 entries. When a bucket exceeds this limit, evict the oldest entry (FIFO - First In First Out).

### Architecture

```
Bucket 0: [key1=val1] → [key2=val2] → [key3=val3]
          ↑ oldest                    ↑ newest

Add key4 (hashes to bucket 0):
Evict key1 (oldest)

Bucket 0: [key2=val2] → [key3=val3] → [key4=val4]
          ↑ oldest                    ↑ newest
```

### Complete Implementation

```java
package org.sudhir512kj.datastructures;

/**
 * HashMap with bucket size limit and FIFO eviction policy
 * Each bucket can hold maximum 3 entries
 * When limit exceeded, oldest entry is evicted
 */
public class LimitedBucketHashMap<K, V> {
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final int MAX_BUCKET_SIZE = 3;
    
    private Node<K, V>[] buckets;
    private int size;
    
    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    @SuppressWarnings("unchecked")
    public LimitedBucketHashMap() {
        buckets = new Node[DEFAULT_CAPACITY];
        size = 0;
    }
    
    private int hash(K key) {
        return key == null ? 0 : Math.abs(key.hashCode() % buckets.length);
    }
    
    public void put(K key, V value) {
        int index = hash(key);
        Node<K, V> head = buckets[index];
        
        // Check if key already exists (update value)
        Node<K, V> current = head;
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        
        // Count bucket size
        int bucketSize = getBucketSize(index);
        
        // If bucket is full, evict oldest (first node)
        if (bucketSize >= MAX_BUCKET_SIZE) {
            evictOldest(index);
        }
        
        // Add new node at end (newest)
        Node<K, V> newNode = new Node<>(key, value);
        if (head == null) {
            buckets[index] = newNode;
        } else {
            Node<K, V> tail = getTail(head);
            tail.next = newNode;
        }
        size++;
    }
    
    public V get(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }
    
    public V remove(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;
        
        while (current != null) {
            if ((current.key == null && key == null) || 
                (current.key != null && current.key.equals(key))) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null;
    }
    
    private int getBucketSize(int index) {
        int count = 0;
        Node<K, V> current = buckets[index];
        while (current != null) {
            count++;
            current = current.next;
        }
        return count;
    }
    
    private Node<K, V> getTail(Node<K, V> head) {
        Node<K, V> current = head;
        while (current.next != null) {
            current = current.next;
        }
        return current;
    }
    
    private void evictOldest(int index) {
        Node<K, V> head = buckets[index];
        if (head != null) {
            System.out.println("Evicting oldest entry: " + head.key + "=" + head.value);
            buckets[index] = head.next;
            size--;
        }
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public void display() {
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] != null) {
                System.out.print("Bucket " + i + " (size=" + getBucketSize(i) + "): ");
                Node<K, V> current = buckets[i];
                while (current != null) {
                    System.out.print("[" + current.key + "=" + current.value + "] → ");
                    current = current.next;
                }
                System.out.println("null");
            }
        }
        System.out.println("Total size: " + size);
    }
}
```

### Usage Example with FIFO Eviction

```java
public class LimitedBucketHashMapTest {
    public static void main(String[] args) {
        LimitedBucketHashMap<Integer, String> map = new LimitedBucketHashMap<>();
        
        // Force collisions by using keys that hash to same bucket
        // Assuming hash function: key % 16
        
        System.out.println("=== Adding entries to same bucket ===");
        map.put(1, "First");    // Bucket 1
        map.put(17, "Second");  // Bucket 1 (17 % 16 = 1)
        map.put(33, "Third");   // Bucket 1 (33 % 16 = 1)
        
        map.display();
        System.out.println();
        
        System.out.println("=== Adding 4th entry (triggers eviction) ===");
        map.put(49, "Fourth");  // Bucket 1 (49 % 16 = 1) - evicts "First"
        
        map.display();
        System.out.println();
        
        System.out.println("=== Verifying eviction ===");
        System.out.println("Get key 1: " + map.get(1));    // null (evicted)
        System.out.println("Get key 17: " + map.get(17));  // Second
        System.out.println("Get key 33: " + map.get(33));  // Third
        System.out.println("Get key 49: " + map.get(49));  // Fourth
        System.out.println();
        
        System.out.println("=== Adding 5th entry (triggers another eviction) ===");
        map.put(65, "Fifth");   // Bucket 1 (65 % 16 = 1) - evicts "Second"
        
        map.display();
        System.out.println();
        
        System.out.println("=== Final verification ===");
        System.out.println("Get key 17: " + map.get(17));  // null (evicted)
        System.out.println("Get key 33: " + map.get(33));  // Third
        System.out.println("Get key 49: " + map.get(49));  // Fourth
        System.out.println("Get key 65: " + map.get(65));  // Fifth
    }
}
```

**Output**:
```
=== Adding entries to same bucket ===
Bucket 1 (size=3): [1=First] → [17=Second] → [33=Third] → null
Total size: 3

=== Adding 4th entry (triggers eviction) ===
Evicting oldest entry: 1=First
Bucket 1 (size=3): [17=Second] → [33=Third] → [49=Fourth] → null
Total size: 3

=== Verifying eviction ===
Get key 1: null
Get key 17: Second
Get key 33: Third
Get key 49: Fourth

=== Adding 5th entry (triggers another eviction) ===
Evicting oldest entry: 17=Second
Bucket 1 (size=3): [33=Third] → [49=Fourth] → [65=Fifth] → null
Total size: 3

=== Final verification ===
Get key 17: null
Get key 33: Third
Get key 49: Fourth
Get key 65: Fifth
```

---

## Time Complexity Analysis

### Custom HashMap (No Limit)

| Operation | Average Case | Worst Case | Explanation |
|-----------|-------------|------------|-------------|
| **put()** | O(1) | O(n) | O(1) with good hash, O(n) if all collide |
| **get()** | O(1) | O(n) | O(1) with good hash, O(n) if all collide |
| **remove()** | O(1) | O(n) | O(1) with good hash, O(n) if all collide |
| **resize()** | O(n) | O(n) | Rehash all n entries |

### Limited Bucket HashMap

| Operation | Average Case | Worst Case | Explanation |
|-----------|-------------|------------|-------------|
| **put()** | O(1) | O(3) = O(1) | Max 3 entries per bucket |
| **get()** | O(1) | O(3) = O(1) | Max 3 entries per bucket |
| **remove()** | O(1) | O(3) = O(1) | Max 3 entries per bucket |
| **evictOldest()** | O(1) | O(1) | Remove head node |

**Key Insight**: Limited bucket size guarantees O(1) worst case!

---

## Space Complexity

### Custom HashMap
- **Space**: O(n) where n = number of entries
- **Array size**: Grows dynamically (16 → 32 → 64 → ...)

### Limited Bucket HashMap
- **Space**: O(min(n, capacity × 3))
- **Maximum entries**: capacity × 3 (3 per bucket)
- **Example**: 16 buckets × 3 = max 48 entries

---

## Comparison: Standard vs Limited Bucket

| Feature | Standard HashMap | Limited Bucket HashMap |
|---------|-----------------|----------------------|
| **Bucket Size** | Unlimited | Max 3 entries |
| **Eviction** | None | FIFO (oldest first) |
| **Worst Case** | O(n) | O(1) |
| **Memory** | Unbounded | Bounded |
| **Use Case** | General purpose | Cache, LRU-like |

---

## Real-World Use Cases

### 1. **Cache with Size Limit**
```java
// Web browser cache with limited entries per domain
LimitedBucketHashMap<String, String> cache = new LimitedBucketHashMap<>();
cache.put("google.com/page1", "content1");
cache.put("google.com/page2", "content2");
cache.put("google.com/page3", "content3");
cache.put("google.com/page4", "content4"); // Evicts page1
```

### 2. **Rate Limiting**
```java
// Track last 3 requests per IP address
LimitedBucketHashMap<String, Long> rateLimiter = new LimitedBucketHashMap<>();
rateLimiter.put("192.168.1.1", System.currentTimeMillis());
```

### 3. **Session Management**
```java
// Store max 3 active sessions per user
LimitedBucketHashMap<String, Session> sessions = new LimitedBucketHashMap<>();
```

### 4. **DNS Cache**
```java
// Cache recent DNS lookups (max 3 per domain)
LimitedBucketHashMap<String, InetAddress> dnsCache = new LimitedBucketHashMap<>();
```

---

## Interview Questions & Answers

### Q1: Why use separate chaining instead of open addressing?

**Answer**:
- **Simpler implementation**: Just linked lists
- **No clustering**: Each bucket independent
- **Easier deletion**: Remove node from list
- **Handles high load factors**: Can exceed 1.0

### Q2: What happens if all keys hash to the same bucket?

**Answer**:
- **Standard HashMap**: Degrades to O(n) linked list
- **Limited Bucket HashMap**: Maintains O(1) by evicting oldest

### Q3: How to handle null keys?

**Answer**:
```java
private int hash(K key) {
    return key == null ? 0 : Math.abs(key.hashCode() % buckets.length);
}
```
Null keys always go to bucket 0.

### Q4: Why load factor 0.75?

**Answer**:
- **Balance**: Space vs time tradeoff
- **< 0.75**: More collisions, slower lookups
- **> 0.75**: Wasted space, frequent resizing
- **0.75**: Optimal empirically proven

### Q5: How to make it thread-safe?

**Answer**:
```java
public synchronized void put(K key, V value) {
    // ... implementation
}

public synchronized V get(K key) {
    // ... implementation
}
```
Or use `ConcurrentHashMap` approach with segment locking.

---

## Advanced Variations

### 1. **LRU Cache (Least Recently Used)**

Instead of FIFO, evict least recently accessed:

```java
// Combine HashMap + Doubly Linked List
class LRUCache<K, V> {
    private Map<K, Node<K, V>> map;
    private DoublyLinkedList<K, V> list;
    private int capacity;
    
    public void put(K key, V value) {
        if (map.containsKey(key)) {
            list.moveToFront(map.get(key));
        } else {
            if (map.size() >= capacity) {
                Node<K, V> evicted = list.removeLast();
                map.remove(evicted.key);
            }
            Node<K, V> node = new Node<>(key, value);
            list.addToFront(node);
            map.put(key, node);
        }
    }
}
```

### 2. **LFU Cache (Least Frequently Used)**

Evict least frequently accessed entry:

```java
class LFUCache<K, V> {
    private Map<K, Node<K, V>> map;
    private Map<Integer, LinkedHashSet<K>> freqMap;
    private int minFreq;
    
    public void put(K key, V value) {
        // Track access frequency
        // Evict entry with lowest frequency
    }
}
```

### 3. **TTL HashMap (Time To Live)**

Entries expire after timeout:

```java
class TTLHashMap<K, V> {
    static class Entry<K, V> {
        K key;
        V value;
        long expiryTime;
    }
    
    public void put(K key, V value, long ttlMillis) {
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        // Store with expiry time
    }
    
    public V get(K key) {
        Entry<K, V> entry = map.get(key);
        if (entry != null && System.currentTimeMillis() < entry.expiryTime) {
            return entry.value;
        }
        return null; // Expired
    }
}
```

---

## Key Takeaways

1. **HashMap uses hashing** for O(1) average case operations
2. **Separate chaining** handles collisions with linked lists
3. **Load factor 0.75** triggers resizing for performance
4. **Limited bucket size** guarantees O(1) worst case
5. **FIFO eviction** removes oldest entry when bucket full
6. **Real-world applications**: Caching, rate limiting, session management
7. **Thread safety** requires synchronization or concurrent design

---

## Practice Problems

1. Implement HashMap with open addressing (linear probing)
2. Add iterator support to custom HashMap
3. Implement LRU cache using HashMap + Doubly Linked List
4. Design thread-safe HashMap without synchronized keyword
5. Implement consistent hashing for distributed systems
6. Create HashMap that supports range queries
7. Build HashMap with automatic expiration (TTL)
8. Implement HashMap with custom hash function
9. Design HashMap that tracks access patterns
10. Create HashMap with compression for large values
