# LFU Cache (LeetCode 460)

## Problem Statement

Design and implement a data structure for a Least Frequently Used (LFU) cache.

Implement the `LFUCache` class:
- `LFUCache(int capacity)` Initializes the object with the capacity of the data structure.
- `int get(int key)` Gets the value of the key if the key exists in the cache. Otherwise, returns -1.
- `void put(int key, int value)` Update the value of the key if present, or inserts the key if not already present. When the cache reaches its capacity, it should invalidate and remove the least frequently used key before inserting a new item. For this problem, when there is a tie (i.e., two or more keys with the same frequency), the least recently used key would be invalidated.

**Time Complexity Requirement**: O(1) for both get and put

**Constraints:**
- 1 <= capacity <= 10^4
- 0 <= key <= 10^5
- 0 <= value <= 10^9
- At most 2 * 10^5 calls will be made to get and put

## Approach 1: HashMap + Frequency Map + DLL (Optimal)

### Intuition
- HashMap: key -> Node (O(1) lookup)
- Frequency Map: frequency -> Doubly Linked List of nodes with that frequency
- Track minimum frequency for O(1) eviction
- When frequency increases, move node to next frequency list

### Implementation

```java
class LFUCache {
    class Node {
        int key, value, freq;
        Node prev, next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
            this.freq = 1;
        }
    }
    
    class DLList {
        Node head, tail;
        int size;
        
        DLList() {
            head = new Node(0, 0);
            tail = new Node(0, 0);
            head.next = tail;
            tail.prev = head;
        }
        
        void add(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
            size++;
        }
        
        void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            size--;
        }
        
        Node removeLast() {
            if (size == 0) return null;
            Node node = tail.prev;
            remove(node);
            return node;
        }
    }
    
    private final int capacity;
    private int minFreq;
    private final Map<Integer, Node> cache;
    private final Map<Integer, DLList> freqMap;
    
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
        this.cache = new HashMap<>();
        this.freqMap = new HashMap<>();
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        
        Node node = cache.get(key);
        updateFreq(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (capacity == 0) return;
        
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            updateFreq(node);
        } else {
            if (cache.size() >= capacity) {
                DLList minFreqList = freqMap.get(minFreq);
                Node toRemove = minFreqList.removeLast();
                cache.remove(toRemove.key);
            }
            
            Node node = new Node(key, value);
            cache.put(key, node);
            freqMap.computeIfAbsent(1, k -> new DLList()).add(node);
            minFreq = 1;
        }
    }
    
    private void updateFreq(Node node) {
        int freq = node.freq;
        DLList list = freqMap.get(freq);
        list.remove(node);
        
        if (list.size == 0 && freq == minFreq) {
            minFreq++;
        }
        
        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new DLList()).add(node);
    }
}
```

**Time Complexity**: O(1) for both get and put
**Space Complexity**: O(capacity)

### Pros
- Optimal O(1) time complexity
- Handles LFU + LRU tie-breaking correctly
- Scalable for large capacity

### Cons
- Complex implementation
- Multiple data structures to maintain
- Higher constant factors

---

## Approach 2: HashMap + TreeMap + LinkedHashSet

### Intuition
- TreeMap maintains frequencies in sorted order
- LinkedHashSet maintains insertion order for LRU tie-breaking
- TreeMap.firstKey() gives minimum frequency

### Implementation

```java
class LFUCache {
    class Node {
        int key, value, freq;
        Node(int k, int v) { key = k; value = v; freq = 1; }
    }
    
    private final int capacity;
    private final Map<Integer, Node> cache;
    private final TreeMap<Integer, LinkedHashSet<Integer>> freqMap;
    
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.freqMap = new TreeMap<>();
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        
        Node node = cache.get(key);
        updateFreq(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (capacity == 0) return;
        
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            updateFreq(node);
        } else {
            if (cache.size() >= capacity) {
                int minFreq = freqMap.firstKey();
                LinkedHashSet<Integer> keys = freqMap.get(minFreq);
                int toRemove = keys.iterator().next();
                keys.remove(toRemove);
                if (keys.isEmpty()) freqMap.remove(minFreq);
                cache.remove(toRemove);
            }
            
            Node node = new Node(key, value);
            cache.put(key, node);
            freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        }
    }
    
    private void updateFreq(Node node) {
        int freq = node.freq;
        freqMap.get(freq).remove(node.key);
        if (freqMap.get(freq).isEmpty()) {
            freqMap.remove(freq);
        }
        
        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node.key);
    }
}
```

**Time Complexity**: O(log n) for TreeMap operations
**Space Complexity**: O(capacity)

### Pros
- Cleaner code than Approach 1
- Automatic frequency sorting
- LinkedHashSet handles LRU naturally

### Cons
- **Not O(1)** - TreeMap operations are O(log n)
- Not acceptable for strict O(1) requirement
- Higher memory overhead

---

## Approach 3: HashMap + PriorityQueue (Incorrect for O(1))

### Intuition
Use PriorityQueue to track (frequency, timestamp) pairs.

### Implementation

```java
class LFUCache {
    class Node {
        int key, value, freq;
        long timestamp;
        
        Node(int k, int v, long t) {
            key = k; value = v; freq = 1; timestamp = t;
        }
    }
    
    private final int capacity;
    private final Map<Integer, Node> cache;
    private final PriorityQueue<Node> pq;
    private long time = 0;
    
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.pq = new PriorityQueue<>((a, b) -> 
            a.freq != b.freq ? a.freq - b.freq : Long.compare(a.timestamp, b.timestamp)
        );
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        
        Node node = cache.get(key);
        pq.remove(node); // O(n) - BAD!
        node.freq++;
        node.timestamp = ++time;
        pq.offer(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (capacity == 0) return;
        
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            pq.remove(node); // O(n) - BAD!
            node.value = value;
            node.freq++;
            node.timestamp = ++time;
            pq.offer(node);
        } else {
            if (cache.size() >= capacity) {
                Node toRemove = pq.poll();
                cache.remove(toRemove.key);
            }
            
            Node node = new Node(key, value, ++time);
            cache.put(key, node);
            pq.offer(node);
        }
    }
}
```

**Time Complexity**: O(n) due to PriorityQueue.remove()
**Space Complexity**: O(capacity)

### Pros
- Simple to understand
- Natural frequency ordering

### Cons
- **Does NOT meet O(1) requirement**
- PriorityQueue.remove() is O(n)
- Not acceptable in interviews

---

## Approach 4: Two HashMaps (Simplified but Suboptimal)

### Intuition
Track frequency and last access time separately.

### Implementation

```java
class LFUCache {
    class Node {
        int value, freq;
        long lastAccess;
        Node(int v) { value = v; freq = 1; lastAccess = System.nanoTime(); }
    }
    
    private final int capacity;
    private final Map<Integer, Node> cache;
    
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) return -1;
        
        Node node = cache.get(key);
        node.freq++;
        node.lastAccess = System.nanoTime();
        return node.value;
    }
    
    public void put(int key, int value) {
        if (capacity == 0) return;
        
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            node.freq++;
            node.lastAccess = System.nanoTime();
        } else {
            if (cache.size() >= capacity) {
                // Find LFU, then LRU
                int toRemove = -1;
                int minFreq = Integer.MAX_VALUE;
                long minTime = Long.MAX_VALUE;
                
                for (Map.Entry<Integer, Node> e : cache.entrySet()) {
                    Node n = e.getValue();
                    if (n.freq < minFreq || (n.freq == minFreq && n.lastAccess < minTime)) {
                        minFreq = n.freq;
                        minTime = n.lastAccess;
                        toRemove = e.getKey();
                    }
                }
                cache.remove(toRemove);
            }
            cache.put(key, new Node(value));
        }
    }
}
```

**Time Complexity**: O(1) for get, O(n) for put when evicting
**Space Complexity**: O(capacity)

### Pros
- Simple implementation
- Easy to understand

### Cons
- O(n) eviction time
- Not suitable for interviews
- Timestamp overflow risk

---

## Comparison Table

| Approach | Get Time | Put Time | Space | Interview Suitable | Difficulty |
|----------|----------|----------|-------|-------------------|------------|
| HashMap + FreqMap + DLL | O(1) | O(1) | O(n) | ✅ Best | Hard |
| TreeMap + LinkedHashSet | O(log n) | O(log n) | O(n) | ⚠️ Close | Medium |
| PriorityQueue | O(n) | O(n) | O(n) | ❌ Wrong | Medium |
| Two HashMaps | O(1) | O(n) | O(n) | ❌ Slow | Easy |

---

## Test Cases

```java
public class LFUCacheTest {
    public static void main(String[] args) {
        // Test Case 1: Basic LFU behavior
        LFUCache cache = new LFUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        assert cache.get(1) == 1;       // freq: {1:2, 2:1}
        cache.put(3, 3);                // evicts key 2 (lowest freq)
        assert cache.get(2) == -1;
        assert cache.get(3) == 3;       // freq: {1:2, 3:2}
        cache.put(4, 4);                // evicts key 1 (LRU among freq=2)
        assert cache.get(1) == -1;
        assert cache.get(3) == 3;
        assert cache.get(4) == 4;
        
        // Test Case 2: LRU tie-breaking
        LFUCache cache2 = new LFUCache(2);
        cache2.put(1, 1);
        cache2.put(2, 2);
        cache2.get(1);                  // freq: {1:2, 2:1}
        cache2.get(1);                  // freq: {1:3, 2:1}
        cache2.get(2);                  // freq: {1:3, 2:2}
        cache2.put(3, 3);               // evicts key 1 (LRU among freq=2)
        assert cache2.get(2) == 2;
        
        // Test Case 3: Update existing key
        LFUCache cache3 = new LFUCache(2);
        cache3.put(1, 1);
        cache3.put(2, 2);
        cache3.put(1, 10);              // update, freq increases
        cache3.put(3, 3);               // evicts key 2
        assert cache3.get(2) == -1;
        assert cache3.get(1) == 10;
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

1. **Q: LFU vs LRU - when to use which?**
   - LFU: When access patterns are stable (e.g., popular items stay popular)
   - LRU: When recent items are more likely to be accessed again
   - LFU better for: Video streaming, CDN
   - LRU better for: Browser cache, database query cache

2. **Q: How to handle concurrent access?**
   ```java
   private final ReadWriteLock lock = new ReentrantReadWriteLock();
   
   public int get(int key) {
       lock.readLock().lock();
       try {
           // ... get logic
       } finally {
           lock.readLock().unlock();
       }
   }
   ```

3. **Q: What if we need to support batch operations?**
   - Implement `putAll(Map<Integer, Integer> entries)`
   - Use write lock for entire batch
   - Optimize by sorting by frequency first

4. **Q: How to implement LFU with decay (aging)?**
   - Periodically decrease all frequencies
   - Use time-based decay: `effectiveFreq = freq * e^(-λt)`
   - Prevents old popular items from staying forever

5. **Q: How to monitor cache performance?**
   ```java
   private long hits = 0, misses = 0;
   
   public double getHitRate() {
       return (double) hits / (hits + misses);
   }
   ```

---

## Common Mistakes

1. ❌ Not handling LRU tie-breaking correctly
2. ❌ Forgetting to update minFreq when removing last node
3. ❌ Using PriorityQueue (O(n) remove operation)
4. ❌ Not updating frequency on put() for existing key
5. ❌ Memory leaks from not cleaning up empty frequency lists

---

## Key Insights

1. **Why is LFU harder than LRU?**
   - Need to track both frequency AND recency
   - Frequency can have many different values
   - Eviction requires finding minimum frequency

2. **Critical optimization**: Track minFreq separately
   - Avoids scanning all frequencies on eviction
   - Updates only when removing last node of minFreq

3. **Data structure choice matters**:
   - DLL for O(1) add/remove
   - HashMap for O(1) lookup
   - Frequency map for O(1) frequency access

---

## Related Problems

- [LRU Cache (LC 146)](./lru-cache.md) - Simpler variant
- [Design In-Memory File System (LC 588)](../02-algorithm-design/in-memory-file-system.md)
- [All O(1) Data Structure (LC 432)](./all-o1-data-structure.md)

---

## Real-world Applications

1. **CDN**: Cache popular content (videos, images)
2. **Database**: Query result caching
3. **Web Servers**: Static resource caching
4. **DNS**: Domain name resolution caching
5. **CPU**: Instruction cache management
