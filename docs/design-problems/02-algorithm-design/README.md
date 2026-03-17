# Algorithm Design Problems

Advanced algorithm design problems requiring creative data structure combinations and optimization techniques.

## 📚 Problems List

### Randomization Problems
1. **[Insert Delete GetRandom O(1) (LC 380)](./insert-delete-getrandom.md)** ⭐⭐⭐
   - HashMap + ArrayList
   - Swap-with-last trick for O(1) removal
   - **Difficulty**: Medium
   - **Key Concepts**: Random access, O(1) operations

2. **Insert Delete GetRandom O(1) - Duplicates (LC 381)** ⭐⭐⭐
   - HashMap<Integer, Set<Integer>>
   - Handle duplicate values
   - **Difficulty**: Hard

3. **Random Pick Index (LC 398)** ⭐⭐
   - Reservoir sampling
   - Stream processing
   - **Difficulty**: Medium

4. **Random Pick with Weight (LC 528)** ⭐⭐
   - Prefix sum + binary search
   - Weighted random selection
   - **Difficulty**: Medium

### String/Trie Problems
5. **Design Add and Search Words Data Structure (LC 211)** ⭐⭐
   - Trie with wildcard search
   - DFS for pattern matching
   - **Difficulty**: Medium

6. **Design Search Autocomplete System (LC 642)** ⭐⭐⭐
   - Trie + priority queue
   - Top K frequent sentences
   - **Difficulty**: Hard

7. **Implement Trie (Prefix Tree) (LC 208)** ⭐⭐
   - Basic trie implementation
   - Insert, search, startsWith
   - **Difficulty**: Medium

### Time-based Problems
8. **[Design Hit Counter (LC 362)](./design-hit-counter.md)** ⭐⭐⭐
   - Sliding window with queue
   - Circular array optimization
   - **Difficulty**: Medium
   - **Key Concepts**: Time window, rate limiting

9. **Logger Rate Limiter (LC 359)** ⭐
   - HashMap with timestamp
   - Simple rate limiting
   - **Difficulty**: Easy

10. **Design Log Storage System (LC 635)** ⭐⭐
    - TreeMap for range queries
    - Timestamp-based retrieval
    - **Difficulty**: Medium

11. **Time Based Key-Value Store (LC 981)** ⭐⭐
    - HashMap + TreeMap
    - Binary search for timestamps
    - **Difficulty**: Medium

### File System Problems
12. **Design In-Memory File System (LC 588)** ⭐⭐⭐
    - Trie-like tree structure
    - Path parsing and navigation
    - **Difficulty**: Hard

13. **Design File System (LC 1166)** ⭐⭐
    - HashMap-based paths
    - Create and get operations
    - **Difficulty**: Medium

### Game Design
14. **Design Tic-Tac-Toe (LC 348)** ⭐⭐
    - Row/col/diagonal counters
    - O(1) move validation
    - **Difficulty**: Medium

15. **Design Snake Game (LC 353)** ⭐⭐
    - Deque for snake body
    - Collision detection
    - **Difficulty**: Medium

### Resource Management
16. **Design Phone Directory (LC 379)** ⭐
    - Available numbers tracking
    - HashSet + Queue
    - **Difficulty**: Medium

17. **Design Memory Allocator (LC 2502)** ⭐⭐
    - Interval management
    - First-fit allocation
    - **Difficulty**: Medium

## 🎯 Problem Patterns

### Pattern 1: HashMap + ArrayList (Random Access)
**Problems**: Insert Delete GetRandom O(1)
**Template**:
```java
class RandomizedSet {
    Map<Integer, Integer> valToIndex;
    List<Integer> values;
    
    // Swap with last for O(1) removal
    public boolean remove(int val) {
        int index = valToIndex.get(val);
        int lastVal = values.get(values.size() - 1);
        values.set(index, lastVal);
        valToIndex.put(lastVal, index);
        values.remove(values.size() - 1);
        valToIndex.remove(val);
    }
}
```

### Pattern 2: Trie for String Operations
**Problems**: Add/Search Words, Autocomplete, Prefix Tree
**Template**:
```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
    String word; // For autocomplete
    int frequency; // For ranking
}
```

### Pattern 3: Sliding Time Window
**Problems**: Hit Counter, Rate Limiter, Moving Average
**Template**:
```java
class TimeWindow {
    Queue<Integer> timestamps;
    
    public int getCount(int currentTime) {
        while (!timestamps.isEmpty() && 
               currentTime - timestamps.peek() >= WINDOW_SIZE) {
            timestamps.poll();
        }
        return timestamps.size();
    }
}
```

### Pattern 4: Circular Array
**Problems**: Hit Counter, Moving Average
**Template**:
```java
class CircularArray {
    int[] times = new int[SIZE];
    int[] counts = new int[SIZE];
    
    public void add(int timestamp) {
        int index = timestamp % SIZE;
        if (times[index] != timestamp) {
            times[index] = timestamp;
            counts[index] = 1;
        } else {
            counts[index]++;
        }
    }
}
```

### Pattern 5: TreeMap for Range Queries
**Problems**: Log Storage, Time-based KV Store
**Template**:
```java
class RangeQuery {
    TreeMap<Integer, String> map;
    
    public List<String> getRange(int start, int end) {
        return new ArrayList<>(map.subMap(start, true, end, true).values());
    }
}
```

## 📊 Complexity Quick Reference

| Problem | Insert | Delete | Query | Space |
|---------|--------|--------|-------|-------|
| Insert Delete GetRandom | O(1) | O(1) | O(1) | O(n) |
| Add/Search Words | O(L) | - | O(L) or O(26^L) | O(N*L) |
| Hit Counter | O(1) | - | O(1) or O(n) | O(n) |
| In-Memory File System | O(L) | - | O(L) | O(N*L) |
| Tic-Tac-Toe | O(1) | - | O(1) | O(1) |

L = length of string/path, N = number of items

## 🔑 Key Techniques

### 1. Swap-with-Last Trick
For O(1) removal from ArrayList:
```java
// Instead of: list.remove(index); // O(n)
// Do:
list.set(index, list.get(list.size() - 1));
list.remove(list.size() - 1); // O(1)
```

### 2. Reservoir Sampling
For random selection from stream:
```java
int count = 0, result = 0;
for (int num : stream) {
    count++;
    if (random.nextInt(count) == 0) {
        result = num;
    }
}
```

### 3. Prefix Sum for Weighted Random
```java
int[] prefixSum = buildPrefixSum(weights);
int target = random.nextInt(prefixSum[n-1]);
return binarySearch(prefixSum, target);
```

### 4. Trie with Wildcard Search
```java
boolean search(String word, TrieNode node, int index) {
    if (word.charAt(index) == '.') {
        for (TrieNode child : node.children.values()) {
            if (search(word, child, index + 1)) return true;
        }
    }
}
```

## 🚀 Study Path

### Beginner
1. Logger Rate Limiter
2. Design Phone Directory
3. Design Tic-Tac-Toe

### Intermediate
1. **Insert Delete GetRandom O(1)** ⭐ Most Important
2. **Design Hit Counter** ⭐ Very Common
3. Implement Trie
4. Design Add and Search Words
5. Time Based Key-Value Store

### Advanced
1. **Design Search Autocomplete System** ⭐ Hard but Common
2. Design In-Memory File System
3. Insert Delete GetRandom - Duplicates
4. Design Snake Game

## 💡 Interview Tips

### Common Follow-ups

1. **"How to handle concurrency?"**
   - Use ConcurrentHashMap
   - Add ReadWriteLock
   - Consider lock-free algorithms

2. **"How to scale to distributed system?"**
   - Shard by key hash
   - Use Redis for shared state
   - Consider consistency vs availability

3. **"How to optimize for memory?"**
   - Use primitive arrays instead of objects
   - Implement custom memory pool
   - Consider compression

4. **"How to add persistence?"**
   - Write-ahead log (WAL)
   - Periodic snapshots
   - Event sourcing

### Red Flags to Avoid

❌ Using ArrayList.remove(index) without swap
❌ Using HashSet.toArray() for random access
❌ Not handling edge cases (empty, single element)
❌ Forgetting to clean up expired data
❌ Not discussing time/space trade-offs

## 🔗 Related Topics

- [Data Structure Design](../01-data-structure-design/)
- [System Design](../03-system-design/)
- [Rate Limiting](../05-rate-limiting/)

## 📚 Additional Resources

### Must-Read Articles
- [Reservoir Sampling Explained](https://en.wikipedia.org/wiki/Reservoir_sampling)
- [Trie Data Structure](https://en.wikipedia.org/wiki/Trie)
- [Sliding Window Technique](https://www.geeksforgeeks.org/window-sliding-technique/)

### Practice Problems by Company
- **Google**: Autocomplete, File System, Hit Counter
- **Amazon**: LRU Cache, Insert Delete GetRandom
- **Facebook**: Design Hit Counter, Trie problems
- **Microsoft**: Time-based KV Store, Log Storage

---

**Total Problems**: 17
**Estimated Study Time**: 3-4 days
**Difficulty Distribution**: Easy (2), Medium (12), Hard (3)
