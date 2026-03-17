# LRU Cache (LeetCode 146)

## Problem Statement

Design a data structure that follows the constraints of a Least Recently Used (LRU) cache.

Implement the `LRUCache` class:
- `LRUCache(int capacity)` Initialize the LRU cache with positive size capacity.
- `int get(int key)` Return the value of the key if the key exists, otherwise return -1.
- `void put(int key, int value)` Update the value of the key if the key exists. Otherwise, add the key-value pair to the cache. If the number of keys exceeds the capacity from this operation, evict the least recently used key.

The functions `get` and `put` must each run in **O(1)** average time complexity.

**Constraints:**
- 1 <= capacity <= 3000
- 0 <= key <= 10^4
- 0 <= value <= 10^5
- At most 2 * 10^5 calls will be made to get and put

## Approach 1: HashMap + Doubly Linked List (Optimal)

### Intuition
- HashMap provides O(1) lookup
- Doubly Linked List maintains order (most recent at head, least recent at tail)
- When accessing a node, move it to head
- When capacity exceeded, remove tail node

### Implementation

```java
class LRUCache {
    class Node {
        int key, value;
        Node prev, next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final int capacity;
    private final Map<Integer, Node> cache;
    private final Node head, tail;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        Node node = cache.get(key);
        moveToHead(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            moveToHead(node);
        } else {
            Node node = new Node(key, value);
            cache.put(key, node);
            addToHead(node);
            
            if (cache.size() > capacity) {
                Node removed = removeTail();
                cache.remove(removed.key);
            }
        }
    }
    
    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }
    
    private Node removeTail() {
        Node node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

**Time Complexity**: O(1) for both get and put
**Space Complexity**: O(capacity)

### Pros
- Optimal O(1) time complexity
- Clean separation of concerns
- Easy to maintain and debug

### Cons
- More complex implementation
- Extra space for doubly linked list pointers

---

## Approach 2: LinkedHashMap (Java Built-in)

### Intuition
Java's LinkedHashMap maintains insertion order and provides access-order mode.

### Implementation

```java
class LRUCache {
    private final LinkedHashMap<Integer, Integer> cache;
    
    public LRUCache(int capacity) {
        this.cache = new LinkedHashMap<Integer, Integer>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > capacity;
            }
        };
    }
    
    public int get(int key) {
        return cache.getOrDefault(key, -1);
    }
    
    public void put(int key, int value) {
        cache.put(key, value);
    }
}
```

**Time Complexity**: O(1) for both get and put
**Space Complexity**: O(capacity)

### Pros
- Extremely concise (5 lines of code)
- Built-in Java solution
- Less error-prone

### Cons
- Not available in all languages
- Less control over implementation details
- Not suitable for interviews (too easy)

---

## Approach 3: HashMap + Queue (Suboptimal)

### Intuition
Use HashMap for O(1) lookup and Queue to track access order.

### Implementation

```java
class LRUCache {
    private final int capacity;
    private final Map<Integer, Integer> cache;
    private final Deque<Integer> order;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.order = new LinkedList<>();
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        
        // Move to end (most recent)
        order.remove(key); // O(n) - BAD!
        order.addLast(key);
        return cache.get(key);
    }
    
    public void put(int key, int value) {
        if (cache.containsKey(key)) {
            order.remove(key); // O(n) - BAD!
        } else if (cache.size() >= capacity) {
            int lru = order.removeFirst();
            cache.remove(lru);
        }
        
        cache.put(key, value);
        order.addLast(key);
    }
}
```

**Time Complexity**: O(n) for get and put (due to queue.remove())
**Space Complexity**: O(capacity)

### Pros
- Simpler to understand
- Uses standard data structures

### Cons
- **Does NOT meet O(1) requirement**
- Queue.remove() is O(n)
- Not acceptable in interviews

---

## Approach 4: Array-based Circular Buffer (Fixed Size)

### Intuition
For small, fixed capacity, use array with circular indexing.

### Implementation

```java
class LRUCache {
    class Entry {
        int key, value, timestamp;
        Entry(int k, int v, int t) { key = k; value = v; timestamp = t; }
    }
    
    private final Map<Integer, Entry> cache;
    private final int capacity;
    private int timestamp = 0;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        Entry entry = cache.get(key);
        entry.timestamp = ++timestamp;
        return entry.value;
    }
    
    public void put(int key, int value) {
        if (cache.containsKey(key)) {
            Entry entry = cache.get(key);
            entry.value = value;
            entry.timestamp = ++timestamp;
        } else {
            if (cache.size() >= capacity) {
                // Find LRU entry
                int lruKey = -1;
                int minTime = Integer.MAX_VALUE;
                for (Map.Entry<Integer, Entry> e : cache.entrySet()) {
                    if (e.getValue().timestamp < minTime) {
                        minTime = e.getValue().timestamp;
                        lruKey = e.getKey();
                    }
                }
                cache.remove(lruKey);
            }
            cache.put(key, new Entry(key, value, ++timestamp));
        }
    }
}
```

**Time Complexity**: O(1) for get, O(n) for put when evicting
**Space Complexity**: O(capacity)

### Pros
- Simple timestamp-based approach
- Easy to understand

### Cons
- O(n) eviction time
- Not suitable for large capacity
- Timestamp overflow risk

---

## Comparison Table

| Approach | Get Time | Put Time | Space | Interview Suitable | Difficulty |
|----------|----------|----------|-------|-------------------|------------|
| HashMap + DLL | O(1) | O(1) | O(n) | ✅ Best | Medium |
| LinkedHashMap | O(1) | O(1) | O(n) | ❌ Too easy | Easy |
| HashMap + Queue | O(n) | O(n) | O(n) | ❌ Wrong | Easy |
| Timestamp | O(1) | O(n) | O(n) | ❌ Slow | Easy |

---

## Test Cases

```java
public class LRUCacheTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        assert cache.get(1) == 1;       // returns 1
        cache.put(3, 3);                // evicts key 2
        assert cache.get(2) == -1;      // returns -1 (not found)
        cache.put(4, 4);                // evicts key 1
        assert cache.get(1) == -1;      // returns -1 (not found)
        assert cache.get(3) == 3;       // returns 3
        assert cache.get(4) == 4;       // returns 4
        
        // Test Case 2: Update existing key
        LRUCache cache2 = new LRUCache(2);
        cache2.put(1, 1);
        cache2.put(2, 2);
        cache2.put(1, 10);              // update key 1
        assert cache2.get(1) == 10;     // returns 10
        cache2.put(3, 3);               // evicts key 2
        assert cache2.get(2) == -1;     // returns -1
        
        // Test Case 3: Single capacity
        LRUCache cache3 = new LRUCache(1);
        cache3.put(1, 1);
        assert cache3.get(1) == 1;
        cache3.put(2, 2);
        assert cache3.get(1) == -1;
        assert cache3.get(2) == 2;
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

1. **Q: How would you implement LRU cache in a distributed system?**
   - Use Redis with sorted sets (ZADD with timestamp)
   - Implement distributed locking for consistency
   - Consider eventual consistency trade-offs

2. **Q: How to handle concurrent access?**
   - Add synchronized blocks or ReentrantReadWriteLock
   - Use ConcurrentHashMap
   - Consider lock-free algorithms with CAS operations

3. **Q: What if we need to support TTL (Time To Live)?**
   - Add expiration timestamp to each entry
   - Use scheduled cleanup thread
   - Check expiration on get() operations

4. **Q: How to persist LRU cache to disk?**
   - Serialize cache state periodically
   - Use write-ahead log (WAL)
   - Implement snapshot mechanism

5. **Q: How to implement LRU with O(1) time and O(1) space?**
   - Not possible - need O(n) space to store n items
   - Can optimize constant factors but not asymptotic complexity

---

## Common Mistakes

1. ❌ Using ArrayList.remove() - O(n) operation
2. ❌ Forgetting to update access order on get()
3. ❌ Not handling capacity = 1 edge case
4. ❌ Memory leaks from not removing old nodes
5. ❌ Not updating value when key already exists

---

## Related Problems

- [LFU Cache (LC 460)](../lfu-cache.md) - Harder variant
- [Design In-Memory File System (LC 588)](../in-memory-file-system.md)
- [Time Based Key-Value Store (LC 981)](../time-based-key-value.md)

---

## Real-world Applications

1. **Browser Cache**: Store recently visited pages
2. **Database Query Cache**: Cache frequent queries
3. **CDN**: Cache popular content at edge servers
4. **CPU Cache**: L1/L2/L3 cache management
5. **Operating Systems**: Page replacement algorithms
