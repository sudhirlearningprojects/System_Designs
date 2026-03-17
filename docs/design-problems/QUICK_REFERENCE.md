# Design Problems - Quick Reference Card

## 🎯 Top 6 Must-Know Problems

| # | Problem | Pattern | Time | Key Insight |
|---|---------|---------|------|-------------|
| 1 | **LRU Cache** | HashMap + DLL | O(1) | Swap with last for O(1) remove |
| 2 | **LFU Cache** | HashMap + FreqMap | O(1) | Track minFreq separately |
| 3 | **Insert Delete GetRandom** | HashMap + ArrayList | O(1) | Swap with last trick |
| 4 | **Design Hit Counter** | Sliding Window | O(1) | Circular array for fixed window |
| 5 | **Min Stack** | Two Stacks | O(1) | Auxiliary stack for min |
| 6 | **Design Twitter** | HashMap + PQ | O(K log K) | K-way merge for feed |

## 🔑 Essential Patterns (Memorize These!)

### Pattern 1: HashMap + ArrayList (Random Access)
```java
Map<Integer, Integer> valToIndex;
List<Integer> values;

// Remove in O(1)
int index = valToIndex.get(val);
int last = values.get(values.size() - 1);
values.set(index, last);
valToIndex.put(last, index);
values.remove(values.size() - 1);
```
**Use for**: Insert Delete GetRandom

### Pattern 2: HashMap + Doubly Linked List (Order)
```java
Map<Integer, Node> cache;
Node head, tail; // Dummy nodes

// Move to head
void moveToHead(Node node) {
    remove(node);
    addToHead(node);
}
```
**Use for**: LRU Cache, LFU Cache

### Pattern 3: Two Stacks (Auxiliary Data)
```java
Stack<Integer> stack;
Stack<Integer> minStack;

void push(int val) {
    stack.push(val);
    if (minStack.isEmpty() || val <= minStack.peek()) {
        minStack.push(val);
    }
}
```
**Use for**: Min Stack, Max Stack

### Pattern 4: Sliding Window (Time-based)
```java
Queue<Integer> timestamps;

int getCount(int time) {
    while (!timestamps.isEmpty() && 
           time - timestamps.peek() >= WINDOW) {
        timestamps.poll();
    }
    return timestamps.size();
}
```
**Use for**: Hit Counter, Rate Limiter

### Pattern 5: Circular Array (Fixed Size)
```java
int[] times = new int[300];
int[] hits = new int[300];

void hit(int timestamp) {
    int idx = timestamp % 300;
    if (times[idx] != timestamp) {
        times[idx] = timestamp;
        hits[idx] = 1;
    } else {
        hits[idx]++;
    }
}
```
**Use for**: Hit Counter, Moving Average

### Pattern 6: Trie (String Operations)
```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd;
}

void insert(String word) {
    TrieNode node = root;
    for (char c : word.toCharArray()) {
        node.children.putIfAbsent(c, new TrieNode());
        node = node.children.get(c);
    }
    node.isEnd = true;
}
```
**Use for**: Autocomplete, Add/Search Words

## ⚡ Key Techniques (One-Liners)

| Technique | Code | Use Case |
|-----------|------|----------|
| Swap with Last | `list.set(i, list.get(size-1)); list.remove(size-1);` | O(1) ArrayList remove |
| Dummy Nodes | `head = new Node(0); tail = new Node(0);` | Simplify edge cases |
| Lazy Cleanup | `if (isExpired()) cache.remove(key);` | Cleanup on access |
| Modulo Wrap | `index = (index + 1) % capacity;` | Circular buffer |
| Min Tracking | `min = Math.min(val, minStack.peek());` | Auxiliary min |

## 📊 Complexity Cheat Sheet

| Data Structure | Get | Put | Delete | Space |
|---------------|-----|-----|--------|-------|
| HashMap | O(1) | O(1) | O(1) | O(n) |
| TreeMap | O(log n) | O(log n) | O(log n) | O(n) |
| ArrayList | O(1) | O(1)* | O(n) | O(n) |
| LinkedList | O(n) | O(1) | O(1)** | O(n) |
| PriorityQueue | O(n) | O(log n) | O(log n) | O(n) |
| Trie | O(L) | O(L) | O(L) | O(N*L) |

*Amortized, **With pointer

## 🎤 Interview Script

### Step 1: Clarify (2 min)
```
"Let me clarify the requirements:
- What operations are most frequent?
- What's the expected scale?
- Do we need thread safety?
- Any persistence requirements?"
```

### Step 2: Propose (3 min)
```
"I'll use [Data Structure 1] for [reason] and 
[Data Structure 2] for [reason].
This gives us O(X) for [operation].
The trade-off is [space/complexity].
Does this sound good?"
```

### Step 3: Implement (15 min)
```java
class Solution {
    // 1. Declare data structures
    private Map<K, V> map;
    private List<V> list;
    
    // 2. Constructor
    public Solution() {
        map = new HashMap<>();
        list = new ArrayList<>();
    }
    
    // 3. Core operations
    public void operation() {
        // Implementation
    }
}
```

### Step 4: Test (3 min)
```
"Let me test with:
- Empty case
- Single element
- Multiple elements
- Edge case: [specific to problem]"
```

### Step 5: Follow-up (2 min)
```
"For thread safety, I'd add [locks/ConcurrentHashMap].
For distributed system, I'd use [Redis/sharding].
For persistence, I'd implement [WAL/snapshots]."
```

## ❌ Common Mistakes (Avoid These!)

| Mistake | Why Bad | Fix |
|---------|---------|-----|
| `list.remove(index)` | O(n) | Swap with last |
| `set.toArray()` for random | O(n) | Use ArrayList |
| `val < minStack.peek()` | Misses duplicates | Use `<=` |
| No dummy nodes | Complex edge cases | Add dummies |
| Proactive cleanup | Overhead | Lazy cleanup |

## 🏢 Company Focus

| Company | Focus Problems |
|---------|---------------|
| **Google** | Autocomplete, File System, Hit Counter |
| **Amazon** | LRU Cache, Insert Delete GetRandom |
| **Facebook** | Design Twitter, Hit Counter |
| **Microsoft** | Text Editor, Time-based KV Store |
| **Apple** | Browser History, Circular Queue |

## 📝 Quick Checklist

Before interview:
- [ ] Can implement LRU Cache in 15 min
- [ ] Know swap-with-last trick
- [ ] Understand HashMap + DLL pattern
- [ ] Can explain time/space trade-offs
- [ ] Practiced follow-up questions

During interview:
- [ ] Clarified requirements
- [ ] Proposed approach before coding
- [ ] Handled edge cases
- [ ] Tested with examples
- [ ] Discussed extensions

## 🎯 Time Allocation (45 min interview)

- **Clarify**: 2-3 min
- **Propose**: 3-5 min
- **Implement**: 15-20 min
- **Test**: 3-5 min
- **Follow-up**: 5-10 min
- **Buffer**: 5 min

## 💡 Pro Tips

1. **Start simple**: Brute force → Optimize
2. **Think aloud**: Explain your reasoning
3. **Draw diagrams**: Visualize data structures
4. **Ask questions**: Show you're thinking
5. **Trade-offs**: Always discuss pros/cons
6. **Edge cases**: Empty, single, capacity
7. **Follow-ups**: Prepare for concurrency/scale

## 🔗 Quick Links

- [Full Study Guide](./STUDY_GUIDE.md)
- [All Problems](./README.md)
- [Data Structure Design](./01-data-structure-design/)
- [Algorithm Design](./02-algorithm-design/)

---

**Print this card and keep it handy during practice!**
