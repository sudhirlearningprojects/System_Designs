# Design Skiplist (LeetCode 1206)

## Problem Statement

Design a Skiplist without using any built-in libraries.

A skiplist is a data structure that takes O(log n) time to add, erase and search. Comparing with treap and red-black tree which has the same function and performance, the code length of Skiplist can be comparatively short and the idea behind Skiplists is just simple linked lists.

Implement the `Skiplist` class:
- `Skiplist()` Initializes the object of the skiplist.
- `bool search(int target)` Returns true if the integer target exists in the Skiplist or false otherwise.
- `void add(int num)` Inserts the value num into the SkipList.
- `bool erase(int num)` Removes the value num from the Skiplist and returns true. If num does not exist, return false.

**Constraints:**
- 0 <= num, target <= 2 * 10^4
- At most 5 * 10^4 calls will be made to search, add, and erase

## What is a Skiplist?

A skiplist is a probabilistic data structure that allows O(log n) search, insertion, and deletion by maintaining multiple levels of linked lists.

```
Level 3:  1 -----------------> 9
Level 2:  1 ------> 4 ------> 9
Level 1:  1 -> 3 -> 4 -> 7 -> 9
Level 0:  1 -> 3 -> 4 -> 7 -> 9 -> 12
```

**Key Properties**:
- Bottom level (Level 0) contains all elements
- Each higher level is a "express lane" with fewer elements
- Element at level i appears at level i+1 with probability 0.5
- Expected height: O(log n)

## Approach 1: Standard Skiplist Implementation

### Intuition
- Each node has multiple forward pointers (one per level)
- Search: Start from top level, move right until next > target, then drop down
- Insert: Find position, randomly determine height, insert at all levels
- Delete: Find node, remove from all levels

### Implementation

```java
class Skiplist {
    class Node {
        int val;
        Node[] forward; // forward[i] points to next node at level i
        
        Node(int val, int level) {
            this.val = val;
            this.forward = new Node[level + 1];
        }
    }
    
    private static final int MAX_LEVEL = 16;
    private static final double P = 0.5;
    private Node head;
    private int level; // current max level
    private Random random;
    
    public Skiplist() {
        head = new Node(-1, MAX_LEVEL);
        level = 0;
        random = new Random();
    }
    
    public boolean search(int target) {
        Node curr = head;
        
        // Start from top level, move right and down
        for (int i = level; i >= 0; i--) {
            while (curr.forward[i] != null && curr.forward[i].val < target) {
                curr = curr.forward[i];
            }
        }
        
        // Move to next node at level 0
        curr = curr.forward[0];
        return curr != null && curr.val == target;
    }
    
    public void add(int num) {
        Node[] update = new Node[MAX_LEVEL + 1];
        Node curr = head;
        
        // Find position and track update pointers
        for (int i = level; i >= 0; i--) {
            while (curr.forward[i] != null && curr.forward[i].val < num) {
                curr = curr.forward[i];
            }
            update[i] = curr;
        }
        
        // Randomly determine level for new node
        int newLevel = randomLevel();
        
        // Update max level if needed
        if (newLevel > level) {
            for (int i = level + 1; i <= newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }
        
        // Create new node and insert
        Node newNode = new Node(num, newLevel);
        for (int i = 0; i <= newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
    }
    
    public boolean erase(int num) {
        Node[] update = new Node[MAX_LEVEL + 1];
        Node curr = head;
        
        // Find node to delete
        for (int i = level; i >= 0; i--) {
            while (curr.forward[i] != null && curr.forward[i].val < num) {
                curr = curr.forward[i];
            }
            update[i] = curr;
        }
        
        curr = curr.forward[0];
        
        // Node not found
        if (curr == null || curr.val != num) {
            return false;
        }
        
        // Remove node from all levels
        for (int i = 0; i <= level; i++) {
            if (update[i].forward[i] != curr) {
                break;
            }
            update[i].forward[i] = curr.forward[i];
        }
        
        // Update max level
        while (level > 0 && head.forward[level] == null) {
            level--;
        }
        
        return true;
    }
    
    private int randomLevel() {
        int lvl = 0;
        while (lvl < MAX_LEVEL && random.nextDouble() < P) {
            lvl++;
        }
        return lvl;
    }
}
```

**Time Complexity**: O(log n) average for all operations
**Space Complexity**: O(n log n) expected

### Pros
- O(log n) operations (expected)
- Simpler than balanced trees
- Lock-free variants possible
- Good cache locality

### Cons
- Probabilistic guarantees (not deterministic)
- More space than simple linked list
- Worst case O(n) if unlucky with random levels

---

## Approach 2: Simplified Skiplist (Fixed Levels)

### Intuition
- Use fixed number of levels (e.g., 4)
- Simpler implementation
- Slightly worse performance

### Implementation

```java
class Skiplist {
    class Node {
        int val;
        Node right, down;
        
        Node(int val) {
            this.val = val;
        }
    }
    
    private Node head;
    private Random random;
    
    public Skiplist() {
        head = new Node(-1);
        random = new Random();
    }
    
    public boolean search(int target) {
        Node curr = head;
        
        while (curr != null) {
            while (curr.right != null && curr.right.val < target) {
                curr = curr.right;
            }
            
            if (curr.right != null && curr.right.val == target) {
                return true;
            }
            
            curr = curr.down;
        }
        
        return false;
    }
    
    public void add(int num) {
        Stack<Node> stack = new Stack<>();
        Node curr = head;
        
        // Find insertion point
        while (curr != null) {
            while (curr.right != null && curr.right.val < num) {
                curr = curr.right;
            }
            stack.push(curr);
            curr = curr.down;
        }
        
        // Insert at bottom level
        Node downNode = null;
        boolean insertUp = true;
        
        while (insertUp && !stack.isEmpty()) {
            curr = stack.pop();
            Node newNode = new Node(num);
            newNode.right = curr.right;
            curr.right = newNode;
            newNode.down = downNode;
            downNode = newNode;
            
            insertUp = random.nextDouble() < 0.5;
        }
        
        // Add new level if needed
        if (insertUp) {
            Node newHead = new Node(-1);
            newHead.down = head;
            Node newNode = new Node(num);
            newHead.right = newNode;
            newNode.down = downNode;
            head = newHead;
        }
    }
    
    public boolean erase(int num) {
        Node curr = head;
        boolean found = false;
        
        while (curr != null) {
            while (curr.right != null && curr.right.val < num) {
                curr = curr.right;
            }
            
            if (curr.right != null && curr.right.val == num) {
                found = true;
                curr.right = curr.right.right;
            }
            
            curr = curr.down;
        }
        
        return found;
    }
}
```

**Time Complexity**: O(log n) average
**Space Complexity**: O(n log n) expected

### Pros
- Simpler node structure (right + down pointers)
- Easier to visualize
- Good for teaching

### Cons
- Still probabilistic
- Slightly more complex than array-based

---

## Approach 3: Array-based Skiplist (Not True Skiplist)

### Intuition
- Use sorted array for simplicity
- Binary search for O(log n) search
- Not a true skiplist but meets requirements

### Implementation

```java
class Skiplist {
    private List<Integer> list;
    
    public Skiplist() {
        list = new ArrayList<>();
    }
    
    public boolean search(int target) {
        int index = Collections.binarySearch(list, target);
        return index >= 0;
    }
    
    public void add(int num) {
        int index = Collections.binarySearch(list, num);
        if (index < 0) {
            index = -(index + 1);
        }
        list.add(index, num);
    }
    
    public boolean erase(int num) {
        int index = Collections.binarySearch(list, num);
        if (index >= 0) {
            list.remove(index);
            return true;
        }
        return false;
    }
}
```

**Time Complexity**: 
- search: O(log n)
- add: O(n) due to array shift
- erase: O(n) due to array shift

**Space Complexity**: O(n)

### Pros
- Extremely simple
- O(log n) search
- No randomization needed

### Cons
- **Not a true skiplist**
- O(n) insertion and deletion
- Not acceptable in interviews for skiplist question

---

## Comparison Table

| Approach | Search | Add | Erase | Space | True Skiplist? |
|----------|--------|-----|-------|-------|----------------|
| Standard Skiplist | O(log n) | O(log n) | O(log n) | O(n log n) | ✅ Yes |
| Simplified (right/down) | O(log n) | O(log n) | O(log n) | O(n log n) | ✅ Yes |
| Array-based | O(log n) | O(n) | O(n) | O(n) | ❌ No |

---

## Skiplist Visualization

### Example: Insert 7

```
Before:
Level 2:  1 ------> 9
Level 1:  1 -> 4 -> 9
Level 0:  1 -> 4 -> 9

Step 1: Search for position (between 4 and 9)
Level 2:  1 ------> 9
          ^
Level 1:  1 -> 4 -> 9
               ^
Level 0:  1 -> 4 -> 9
               ^

Step 2: Randomly determine level (say level 1)

Step 3: Insert at levels 0 and 1
Level 2:  1 ------> 9
Level 1:  1 -> 4 -> 7 -> 9
Level 0:  1 -> 4 -> 7 -> 9
```

---

## Test Cases

```java
public class SkiplistTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        Skiplist skiplist = new Skiplist();
        skiplist.add(1);
        skiplist.add(2);
        skiplist.add(3);
        assert skiplist.search(0) == false;
        skiplist.add(4);
        assert skiplist.search(1) == true;
        assert skiplist.erase(0) == false;
        assert skiplist.erase(1) == true;
        assert skiplist.search(1) == false;
        
        // Test Case 2: Duplicates
        Skiplist skiplist2 = new Skiplist();
        skiplist2.add(1);
        skiplist2.add(1);
        skiplist2.add(2);
        assert skiplist2.search(1) == true;
        assert skiplist2.erase(1) == true;
        assert skiplist2.search(1) == true; // Still one 1 left
        assert skiplist2.erase(1) == true;
        assert skiplist2.search(1) == false;
        
        // Test Case 3: Large dataset
        Skiplist skiplist3 = new Skiplist();
        for (int i = 0; i < 1000; i++) {
            skiplist3.add(i);
        }
        for (int i = 0; i < 1000; i++) {
            assert skiplist3.search(i) == true;
        }
        for (int i = 0; i < 500; i++) {
            assert skiplist3.erase(i) == true;
        }
        for (int i = 0; i < 500; i++) {
            assert skiplist3.search(i) == false;
        }
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

### 1. How to make skiplist thread-safe?

```java
class ConcurrentSkiplist {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public boolean search(int target) {
        lock.readLock().lock();
        try {
            // ... search logic
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void add(int num) {
        lock.writeLock().lock();
        try {
            // ... add logic
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

Or use Java's built-in `ConcurrentSkipListSet`.

### 2. How to support range queries?

```java
public List<Integer> range(int start, int end) {
    List<Integer> result = new ArrayList<>();
    Node curr = head;
    
    // Find start position
    for (int i = level; i >= 0; i--) {
        while (curr.forward[i] != null && curr.forward[i].val < start) {
            curr = curr.forward[i];
        }
    }
    
    curr = curr.forward[0];
    
    // Collect all values in range
    while (curr != null && curr.val <= end) {
        result.add(curr.val);
        curr = curr.forward[0];
    }
    
    return result;
}
```

### 3. Why use skiplist instead of balanced tree?

**Advantages**:
- Simpler implementation (no rotations)
- Lock-free variants easier to implement
- Better cache locality
- Probabilistic balancing (no worst-case rebalancing)

**Disadvantages**:
- Probabilistic guarantees (not deterministic)
- More space overhead
- Slightly slower in practice for small datasets

### 4. How to optimize space?

```java
// Use array instead of individual Node objects
class CompactSkiplist {
    int[] values;
    int[][] forward; // forward[level][index]
    int size;
    
    // Reduces object overhead
}
```

### 5. How does randomLevel() work?

```java
private int randomLevel() {
    int level = 0;
    // Flip coin: 50% chance to go up a level
    while (level < MAX_LEVEL && random.nextDouble() < 0.5) {
        level++;
    }
    return level;
}

// Probability distribution:
// Level 0: 50%
// Level 1: 25%
// Level 2: 12.5%
// Level 3: 6.25%
// ...
```

---

## Common Mistakes

1. ❌ Forgetting to update all levels during insertion/deletion
2. ❌ Not handling duplicates correctly
3. ❌ Incorrect update array tracking
4. ❌ Not updating max level after deletion
5. ❌ Off-by-one errors in level indexing

---

## Key Insights

### Why O(log n)?

Expected number of levels: O(log n)
Each level has ~n/2^i nodes
Search path: O(log n) levels × O(1) per level = O(log n)

### Space Analysis

Each node appears at level i with probability 1/2^i
Expected space per node: 1 + 1/2 + 1/4 + ... = 2
Total space: O(2n) = O(n)

But we store pointers, so actual space: O(n log n)

---

## Related Problems

- [Design HashMap (LC 706)](../01-data-structure-design/design-hashmap.md)
- [LRU Cache (LC 146)](../01-data-structure-design/lru-cache.md)
- [Design In-Memory File System (LC 588)](./in-memory-file-system.md)

---

## Real-world Applications

1. **Redis**: Uses skiplist for sorted sets (ZSET)
2. **LevelDB/RocksDB**: Memtable implementation
3. **Concurrent Data Structures**: Lock-free skiplists
4. **Databases**: Index structures
5. **Distributed Systems**: Consistent hashing rings
