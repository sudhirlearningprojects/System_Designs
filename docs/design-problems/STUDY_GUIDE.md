# LeetCode Design Problems - Complete Study Guide

## 📖 Overview

This guide contains in-depth solutions with multiple approaches for 50+ design problems from LeetCode's Master Design Interview List. Each problem includes:
- 2-4 different solution approaches
- Time/space complexity analysis
- Trade-offs and comparisons
- Production-ready Java code
- Comprehensive test cases
- Follow-up questions and extensions

## 🎯 Quick Start

### For Interview Preparation (1-2 weeks)
**Week 1: Core Problems (Must Know)**
1. [LRU Cache (LC 146)](./01-data-structure-design/lru-cache.md) ⭐⭐⭐
2. [LFU Cache (LC 460)](./01-data-structure-design/lfu-cache.md) ⭐⭐⭐
3. [Insert Delete GetRandom O(1) (LC 380)](./02-algorithm-design/insert-delete-getrandom.md) ⭐⭐⭐
4. [Design Hit Counter (LC 362)](./02-algorithm-design/design-hit-counter.md) ⭐⭐⭐
5. [Min Stack (LC 155)](./01-data-structure-design/min-stack.md) ⭐⭐
6. [Design Twitter (LC 355)](./03-system-design/design-twitter.md) ⭐⭐⭐

**Week 2: Advanced Problems**
1. Design Search Autocomplete System (LC 642)
2. Design In-Memory File System (LC 588)
3. Design Skiplist (LC 1206)
4. Design Add and Search Words (LC 211)

### For System Design Interviews
Focus on these problems that translate to real systems:
- Design Twitter → Social media feeds
- Design Hit Counter → Rate limiting, analytics
- LRU/LFU Cache → Caching strategies
- Design File System → Storage systems

## 🗂️ Problem Categories

### 1. Data Structure Design (12 problems)
**Focus**: Fundamental data structures with specific constraints

| Problem | Difficulty | Key Technique | Time Complexity |
|---------|-----------|---------------|-----------------|
| LRU Cache | Medium | HashMap + DLL | O(1) |
| LFU Cache | Hard | HashMap + FreqMap + DLL | O(1) |
| Min Stack | Medium | Two Stacks | O(1) |
| Design HashMap | Easy | Array + Chaining | O(1) avg |
| Circular Queue | Medium | Circular Array | O(1) |

[View All Data Structure Problems →](./01-data-structure-design/)

### 2. Algorithm Design (17 problems)
**Focus**: Creative combinations of data structures

| Problem | Difficulty | Key Technique | Time Complexity |
|---------|-----------|---------------|-----------------|
| Insert Delete GetRandom | Medium | HashMap + ArrayList | O(1) |
| Design Hit Counter | Medium | Sliding Window | O(1) |
| Add/Search Words | Medium | Trie + DFS | O(L) |
| Design Autocomplete | Hard | Trie + PriorityQueue | O(L + K log K) |

[View All Algorithm Problems →](./02-algorithm-design/)

### 3. System Design (10 problems)
**Focus**: Simplified versions of real systems

| Problem | Difficulty | Key Technique | Scalability |
|---------|-----------|---------------|-------------|
| Design Twitter | Medium | HashMap + PriorityQueue | Millions of users |
| Design File Sharing | Hard | HashMap + TreeSet | Distributed |
| Design Movie Rental | Hard | Multiple Indices | High throughput |

[View All System Design Problems →](./03-system-design/)

### 4. Advanced Data Structures (8 problems)
**Focus**: Complex data structures

| Problem | Difficulty | Key Technique | Use Case |
|---------|-----------|---------------|----------|
| Design Skiplist | Hard | Probabilistic Levels | Redis ZSET |
| Design Leaderboard | Medium | TreeMap | Gaming |
| Design Text Editor | Hard | Two Stacks | Real-time editing |

[View All Advanced Problems →](./04-advanced-data-structures/)

### 5. Rate Limiting (3 problems)
**Focus**: Traffic control and throttling

| Problem | Difficulty | Algorithm | Window |
|---------|-----------|-----------|--------|
| Logger Rate Limiter | Easy | HashMap | Fixed |
| Design Hit Counter | Medium | Sliding Window | 5 minutes |
| Design Rate Limiter | Hard | Token Bucket | Configurable |

[View All Rate Limiting Problems →](./05-rate-limiting/)

### 6. Distributed Systems (5 problems)
**Focus**: Distributed computing concepts

| Problem | Difficulty | Key Concept | CAP Trade-off |
|---------|-----------|-------------|---------------|
| Distributed ID Generator | Hard | Snowflake Algorithm | AP |
| Consistent Hashing | Hard | Hash Ring | Availability |
| Distributed Lock | Hard | Lease-based | CP |

[View All Distributed Problems →](./06-distributed-systems/)

## 🔑 Essential Patterns

### Pattern 1: HashMap + Auxiliary Structure
**When to use**: Need O(1) lookup + maintain order/aggregate

**Problems**: LRU Cache, LFU Cache, Min Stack, Insert Delete GetRandom

**Template**:
```java
class DataStructure {
    Map<Key, Value> map;           // O(1) lookup
    AuxiliaryStructure aux;        // Maintain order/aggregate
    
    public void operation() {
        // Update both structures
        map.put(key, value);
        aux.update(key);
    }
}
```

**Examples**:
- LRU: HashMap + Doubly Linked List
- LFU: HashMap + Frequency Map + DLL
- Min Stack: Stack + Min Stack
- Insert Delete GetRandom: HashMap + ArrayList

### Pattern 2: Trie for String Operations
**When to use**: Prefix matching, autocomplete, dictionary

**Problems**: Add/Search Words, Autocomplete, Implement Trie

**Template**:
```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd = false;
    String word;        // For autocomplete
    int frequency;      // For ranking
}

class Trie {
    TrieNode root = new TrieNode();
    
    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }
    
    public boolean search(String word) {
        TrieNode node = findNode(word);
        return node != null && node.isEnd;
    }
}
```

### Pattern 3: Sliding Time Window
**When to use**: Time-based aggregation, rate limiting

**Problems**: Hit Counter, Rate Limiter, Moving Average

**Template**:
```java
class TimeWindow {
    Queue<Integer> timestamps;
    int windowSize;
    
    public void add(int timestamp) {
        timestamps.offer(timestamp);
        cleanup(timestamp);
    }
    
    public int getCount(int timestamp) {
        cleanup(timestamp);
        return timestamps.size();
    }
    
    private void cleanup(int currentTime) {
        while (!timestamps.isEmpty() && 
               currentTime - timestamps.peek() >= windowSize) {
            timestamps.poll();
        }
    }
}
```

### Pattern 4: Circular Array
**When to use**: Fixed-size buffer, time-based bucketing

**Problems**: Hit Counter, Circular Queue, Moving Average

**Template**:
```java
class CircularArray {
    int[] data;
    int head = 0, tail = 0, size = 0;
    
    public void add(int val) {
        data[tail] = val;
        tail = (tail + 1) % data.length;
        if (size < data.length) size++;
        else head = (head + 1) % data.length;
    }
    
    public int get(int index) {
        return data[(head + index) % data.length];
    }
}
```

### Pattern 5: TreeMap for Range Queries
**When to use**: Sorted data, range queries, time-based retrieval

**Problems**: Log Storage, Time-based KV Store, Calendar

**Template**:
```java
class RangeQuery {
    TreeMap<Integer, String> map = new TreeMap<>();
    
    public void put(int key, String value) {
        map.put(key, value);
    }
    
    public List<String> getRange(int start, int end) {
        return new ArrayList<>(
            map.subMap(start, true, end, true).values()
        );
    }
    
    public String getFloor(int key) {
        Map.Entry<Integer, String> entry = map.floorEntry(key);
        return entry != null ? entry.getValue() : null;
    }
}
```

### Pattern 6: Two Pointers/Stacks
**When to use**: Undo/redo, browser history, text editor

**Problems**: Browser History, Text Editor, Design Calculator

**Template**:
```java
class History {
    Stack<State> backward = new Stack<>();
    Stack<State> forward = new Stack<>();
    State current;
    
    public void visit(State state) {
        backward.push(current);
        current = state;
        forward.clear(); // Clear forward history
    }
    
    public State back() {
        if (!backward.isEmpty()) {
            forward.push(current);
            current = backward.pop();
        }
        return current;
    }
    
    public State forward() {
        if (!forward.isEmpty()) {
            backward.push(current);
            current = forward.pop();
        }
        return current;
    }
}
```

## 🎓 Key Techniques

### 1. Swap-with-Last for O(1) Removal
**Problem**: Remove element from ArrayList in O(1)

```java
// Instead of: list.remove(index); // O(n)
// Do:
int lastIndex = list.size() - 1;
list.set(index, list.get(lastIndex));
list.remove(lastIndex); // O(1)
```

**Used in**: Insert Delete GetRandom O(1)

### 2. Dummy Nodes for Edge Cases
**Problem**: Simplify linked list operations

```java
class LinkedList {
    Node head = new Node(0); // Dummy head
    Node tail = new Node(0); // Dummy tail
    
    public LinkedList() {
        head.next = tail;
        tail.prev = head;
    }
    
    // No need to check if head/tail is null
}
```

**Used in**: LRU Cache, LFU Cache

### 3. Lazy Cleanup
**Problem**: Avoid proactive cleanup overhead

```java
class Cache {
    Map<Key, Entry> cache;
    
    public Value get(Key key) {
        Entry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        cache.remove(key); // Lazy cleanup
        return null;
    }
}
```

**Used in**: Authentication Manager, Hit Counter

### 4. Reservoir Sampling
**Problem**: Random selection from stream

```java
int count = 0, result = 0;
Random random = new Random();

for (int num : stream) {
    count++;
    if (random.nextInt(count) == 0) {
        result = num;
    }
}
// Each element has 1/n probability
```

**Used in**: Random Pick Index, Linked List Random Node

### 5. Prefix Sum for Weighted Random
**Problem**: Random selection with weights

```java
int[] weights = {1, 3, 2}; // Total = 6
int[] prefixSum = {1, 4, 6};

int target = random.nextInt(6); // 0-5
int index = binarySearch(prefixSum, target);
// Probability: 1/6, 3/6, 2/6
```

**Used in**: Random Pick with Weight

### 6. Difference Encoding
**Problem**: Space-efficient storage

```java
// Instead of storing absolute values
int[] values = {100, 102, 105, 103};

// Store differences
int[] diffs = {100, 2, 3, -2};
// Saves space when values are close
```

**Used in**: Min Stack (advanced), Time Series

## 📊 Complexity Cheat Sheet

### Common Operations

| Data Structure | Insert | Delete | Search | Space |
|---------------|--------|--------|--------|-------|
| HashMap | O(1) | O(1) | O(1) | O(n) |
| TreeMap | O(log n) | O(log n) | O(log n) | O(n) |
| ArrayList | O(1) amortized | O(n) | O(n) | O(n) |
| LinkedList | O(1) | O(1)* | O(n) | O(n) |
| PriorityQueue | O(log n) | O(log n) | O(n) | O(n) |
| Trie | O(L) | O(L) | O(L) | O(N*L) |
| Skiplist | O(log n) avg | O(log n) avg | O(log n) avg | O(n log n) |

*With pointer to node

### Design Problem Complexities

| Problem | Get/Search | Put/Add | Delete | Space |
|---------|-----------|---------|--------|-------|
| LRU Cache | O(1) | O(1) | O(1) | O(n) |
| LFU Cache | O(1) | O(1) | O(1) | O(n) |
| Min Stack | O(1) | O(1) | O(1) | O(n) |
| Insert Delete GetRandom | O(1) | O(1) | O(1) | O(n) |
| Hit Counter (Queue) | O(n) | O(1) | - | O(n) |
| Hit Counter (Circular) | O(1) | O(1) | - | O(1) |
| Trie | O(L) | O(L) | O(L) | O(N*L) |
| Skiplist | O(log n) | O(log n) | O(log n) | O(n log n) |

## 🚀 Interview Strategy

### Before the Interview
1. **Master the top 6 problems** (marked with ⭐⭐⭐)
2. **Understand patterns**, not just solutions
3. **Practice explaining trade-offs**
4. **Review follow-up questions**

### During the Interview

#### Step 1: Clarify Requirements (2-3 min)
```
Questions to ask:
- What operations are most frequent?
- What are the performance requirements?
- How much data? (scale)
- Concurrent access needed?
- Persistence required?
```

#### Step 2: Propose High-Level Approach (3-5 min)
```
1. State the data structures you'll use
2. Explain why (trade-offs)
3. Mention time/space complexity
4. Ask if this approach sounds good
```

#### Step 3: Implement (15-20 min)
```
1. Start with class structure
2. Implement core operations first
3. Handle edge cases
4. Add comments for complex logic
```

#### Step 4: Test (5 min)
```
1. Walk through example
2. Test edge cases:
   - Empty
   - Single element
   - Capacity limits
   - Duplicates
```

#### Step 5: Discuss Extensions (5 min)
```
Common follow-ups:
- Thread safety
- Distributed system
- Persistence
- Different time windows
- Monitoring/metrics
```

### Red Flags to Avoid

❌ **Don't**:
- Jump into coding without clarifying
- Use suboptimal data structures (e.g., ArrayList.remove())
- Ignore edge cases
- Forget to discuss trade-offs
- Say "I don't know" without trying

✅ **Do**:
- Think out loud
- Start with brute force, then optimize
- Explain your reasoning
- Ask clarifying questions
- Discuss multiple approaches

## 📚 Company-Specific Focus

### Google
**Focus**: Algorithm design, scalability
- Design Autocomplete System
- Design In-Memory File System
- Design Hit Counter
- LRU/LFU Cache

### Amazon
**Focus**: Practical systems, caching
- LRU Cache (very common)
- Insert Delete GetRandom
- Design Log Storage
- Design Leaderboard

### Facebook/Meta
**Focus**: Social features, feeds
- Design Twitter
- Design Hit Counter
- LRU Cache
- Design News Feed

### Microsoft
**Focus**: Data structures, Windows APIs
- LRU Cache
- Design Text Editor
- Time-based Key-Value Store
- Design File System

### Apple
**Focus**: iOS/macOS features
- LRU Cache
- Design Browser History
- Design Circular Queue
- Design Authentication Manager

## 🎯 Practice Schedule

### Week 1: Foundations
- **Day 1-2**: LRU Cache (all approaches)
- **Day 3**: Min Stack + variations
- **Day 4**: Insert Delete GetRandom
- **Day 5**: Design HashMap/HashSet
- **Day 6-7**: Review + practice

### Week 2: Advanced
- **Day 1-2**: LFU Cache
- **Day 3**: Design Hit Counter
- **Day 4**: Design Twitter
- **Day 5**: Trie problems (Add/Search Words)
- **Day 6-7**: Mock interviews

### Week 3: Mastery
- **Day 1**: Design Autocomplete
- **Day 2**: Design File System
- **Day 3**: Design Skiplist
- **Day 4-5**: System design problems
- **Day 6-7**: Review all + mock interviews

## 📖 Additional Resources

### Books
- "Designing Data-Intensive Applications" by Martin Kleppmann
- "System Design Interview" by Alex Xu
- "Cracking the Coding Interview" by Gayle McDowell

### Online Resources
- [LeetCode Discuss](https://leetcode.com/discuss/)
- [System Design Primer](https://github.com/donnemartin/system-design-primer)
- [ByteByteGo](https://bytebytego.com/)

### Practice Platforms
- LeetCode Premium (for locked problems)
- Pramp (mock interviews)
- Interviewing.io (real interviews)

---

**Total Problems**: 50+
**Estimated Study Time**: 3-4 weeks
**Success Rate**: Practice all ⭐⭐⭐ problems for 80%+ interview success
