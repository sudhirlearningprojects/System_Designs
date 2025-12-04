# Priority Queue - Deep Dive

## Overview
A Priority Queue is an abstract data type where each element has a priority, and elements are served based on their priority rather than insertion order. Elements with higher priority are dequeued before elements with lower priority.

**Key Characteristics**:
- Elements are ordered by priority, not insertion order
- Highest (or lowest) priority element is always at the front
- Implemented using Binary Heap (most common)
- Not a true queue (FIFO doesn't apply)

## Internal Working

### Binary Heap Implementation

Priority Queue is typically implemented using a **Binary Heap** - a complete binary tree that satisfies the heap property.

#### Min Heap vs Max Heap

**Min Heap**: Parent ≤ Children (smallest element at root)
```
        1
       / \
      3   2
     / \ / \
    7  5 4  6
```

**Max Heap**: Parent ≥ Children (largest element at root)
```
        10
       /  \
      8    9
     / \  / \
    5  6 7  4
```

### Array Representation

Heaps are stored in arrays for efficient memory usage:

```
Array: [1, 3, 2, 7, 5, 4, 6]
Index:  0  1  2  3  4  5  6

Tree representation:
        1 (index 0)
       / \
      3   2 (index 1, 2)
     / \ / \
    7  5 4  6 (index 3, 4, 5, 6)
```

**Index Relationships**:
- Parent of node at index `i`: `(i - 1) / 2`
- Left child of node at index `i`: `2 * i + 1`
- Right child of node at index `i`: `2 * i + 2`

### Core Operations

#### 1. Insert (Heapify Up)

**Process**:
1. Add element at the end of array
2. Compare with parent
3. If violates heap property, swap with parent
4. Repeat until heap property is satisfied

**Example**: Insert 1 into Min Heap `[2, 4, 3, 7, 5, 6]`

```
Step 1: Add at end
[2, 4, 3, 7, 5, 6, 1]
        2
       / \
      4   3
     / \ / \
    7  5 6  1

Step 2: Compare 1 with parent 3
1 < 3, swap
[2, 4, 1, 7, 5, 6, 3]
        2
       / \
      4   1
     / \ / \
    7  5 6  3

Step 3: Compare 1 with parent 2
1 < 2, swap
[1, 4, 2, 7, 5, 6, 3]
        1
       / \
      4   2
     / \ / \
    7  5 6  3

Done! Heap property satisfied.
```

#### 2. Remove/Poll (Heapify Down)

**Process**:
1. Remove root element
2. Move last element to root
3. Compare with children
4. Swap with smaller child (min heap) or larger child (max heap)
5. Repeat until heap property is satisfied

**Example**: Remove from Min Heap `[1, 4, 2, 7, 5, 6, 3]`

```
Step 1: Remove root (1), move last element (3) to root
[3, 4, 2, 7, 5, 6]
        3
       / \
      4   2
     / \ /
    7  5 6

Step 2: Compare 3 with children (4, 2)
Smaller child is 2, and 3 > 2, swap
[2, 4, 3, 7, 5, 6]
        2
       / \
      4   3
     / \ /
    7  5 6

Step 3: Compare 3 with children (6)
3 < 6, heap property satisfied

Done!
```

#### 3. Peek

Simply return the root element without removing it.

```java
public int peek() {
    return heap[0];
}
```

## Java PriorityQueue Implementation

### Basic Usage

```java
import java.util.*;

public class PriorityQueueBasics {
    
    public static void main(String[] args) {
        // Min Heap (default)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        // Add elements
        minHeap.offer(5);
        minHeap.offer(2);
        minHeap.offer(8);
        minHeap.offer(1);
        
        System.out.println("Min Heap:");
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.poll() + " "); // 1 2 5 8
        }
        
        System.out.println("\n\nMax Heap:");
        // Max Heap (reverse order)
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.offer(5);
        maxHeap.offer(2);
        maxHeap.offer(8);
        maxHeap.offer(1);
        
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.poll() + " "); // 8 5 2 1
        }
    }
}
```

### Custom Comparator

```java
// Min Heap by absolute value
PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> Math.abs(a) - Math.abs(b));

// Max Heap by string length
PriorityQueue<String> pq = new PriorityQueue<>((a, b) -> b.length() - a.length());

// Min Heap by custom object property
class Task {
    String name;
    int priority;
}
PriorityQueue<Task> pq = new PriorityQueue<>((a, b) -> a.priority - b.priority);
```

### Custom Class with Comparable

```java
class Student implements Comparable<Student> {
    String name;
    int marks;
    
    public Student(String name, int marks) {
        this.name = name;
        this.marks = marks;
    }
    
    @Override
    public int compareTo(Student other) {
        return this.marks - other.marks; // Min heap by marks
    }
}

PriorityQueue<Student> pq = new PriorityQueue<>();
pq.offer(new Student("Alice", 85));
pq.offer(new Student("Bob", 92));
pq.offer(new Student("Charlie", 78));

System.out.println(pq.poll().name); // Charlie (lowest marks)
```

## Time Complexity

| Operation | Time Complexity | Explanation |
|-----------|----------------|-------------|
| **offer()/add()** | O(log n) | Heapify up through tree height |
| **poll()/remove()** | O(log n) | Heapify down through tree height |
| **peek()** | O(1) | Direct access to root |
| **contains()** | O(n) | Linear search through array |
| **remove(Object)** | O(n) | Find element + heapify |
| **size()** | O(1) | Stored as variable |
| **isEmpty()** | O(1) | Check size |

### Why O(log n)?

Binary heap is a complete binary tree:
- Height of tree = log₂(n)
- Heapify operations traverse from root to leaf (or vice versa)
- Maximum swaps = height = log n

**Example**:
- 1,000 elements → height = 10 → max 10 swaps
- 1,000,000 elements → height = 20 → max 20 swaps

## Space Complexity

**O(n)** - Stores n elements in internal array

## Common Interview Problems

### 1. Kth Largest Element in Array

```java
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll(); // Remove smallest
        }
    }
    
    return minHeap.peek();
}

// Example: nums = [3,2,1,5,6,4], k = 2
// Result: 5 (2nd largest)
```

**Time**: O(n log k), **Space**: O(k)

---

### 2. Kth Smallest Element in Array

```java
public int findKthSmallest(int[] nums, int k) {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    
    for (int num : nums) {
        maxHeap.offer(num);
        if (maxHeap.size() > k) {
            maxHeap.poll(); // Remove largest
        }
    }
    
    return maxHeap.peek();
}

// Example: nums = [7,10,4,3,20,15], k = 3
// Result: 7 (3rd smallest)
```

**Time**: O(n log k), **Space**: O(k)

---

### 3. Merge K Sorted Lists

```java
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> minHeap = new PriorityQueue<>((a, b) -> a.val - b.val);
    
    // Add first node of each list
    for (ListNode list : lists) {
        if (list != null) {
            minHeap.offer(list);
        }
    }
    
    ListNode dummy = new ListNode(0);
    ListNode current = dummy;
    
    while (!minHeap.isEmpty()) {
        ListNode node = minHeap.poll();
        current.next = node;
        current = current.next;
        
        if (node.next != null) {
            minHeap.offer(node.next);
        }
    }
    
    return dummy.next;
}
```

**Time**: O(n log k) where n = total nodes, k = number of lists  
**Space**: O(k)

---

### 4. Top K Frequent Elements

```java
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freqMap = new HashMap<>();
    for (int num : nums) {
        freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
    }
    
    PriorityQueue<Map.Entry<Integer, Integer>> minHeap = 
        new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
    
    for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
        minHeap.offer(entry);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }
    
    int[] result = new int[k];
    for (int i = 0; i < k; i++) {
        result[i] = minHeap.poll().getKey();
    }
    
    return result;
}
```

**Time**: O(n log k), **Space**: O(n)

---

### 5. Find Median from Data Stream

```java
class MedianFinder {
    PriorityQueue<Integer> maxHeap; // Left half (smaller elements)
    PriorityQueue<Integer> minHeap; // Right half (larger elements)
    
    public MedianFinder() {
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        minHeap = new PriorityQueue<>();
    }
    
    public void addNum(int num) {
        maxHeap.offer(num);
        minHeap.offer(maxHeap.poll());
        
        if (maxHeap.size() < minHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
    }
    
    public double findMedian() {
        if (maxHeap.size() > minHeap.size()) {
            return maxHeap.peek();
        }
        return (maxHeap.peek() + minHeap.peek()) / 2.0;
    }
}

// Example:
// addNum(1) → median = 1
// addNum(2) → median = 1.5
// addNum(3) → median = 2
```

**Time**: addNum O(log n), findMedian O(1)  
**Space**: O(n)

---

### 6. K Closest Points to Origin

```java
public int[][] kClosest(int[][] points, int k) {
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> 
        (b[0]*b[0] + b[1]*b[1]) - (a[0]*a[0] + a[1]*a[1])
    );
    
    for (int[] point : points) {
        maxHeap.offer(point);
        if (maxHeap.size() > k) {
            maxHeap.poll();
        }
    }
    
    int[][] result = new int[k][2];
    for (int i = 0; i < k; i++) {
        result[i] = maxHeap.poll();
    }
    
    return result;
}

// Example: points = [[1,3],[-2,2],[5,8],[0,1]], k = 2
// Result: [[0,1],[-2,2]]
```

**Time**: O(n log k), **Space**: O(k)

---

### 7. Task Scheduler

```java
public int leastInterval(char[] tasks, int n) {
    Map<Character, Integer> freq = new HashMap<>();
    for (char task : tasks) {
        freq.put(task, freq.getOrDefault(task, 0) + 1);
    }
    
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    maxHeap.addAll(freq.values());
    
    int cycles = 0;
    while (!maxHeap.isEmpty()) {
        List<Integer> temp = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            if (!maxHeap.isEmpty()) {
                temp.add(maxHeap.poll());
            }
        }
        
        for (int count : temp) {
            if (--count > 0) {
                maxHeap.offer(count);
            }
        }
        
        cycles += maxHeap.isEmpty() ? temp.size() : n + 1;
    }
    
    return cycles;
}

// Example: tasks = ['A','A','A','B','B','B'], n = 2
// Result: 8 (A -> B -> idle -> A -> B -> idle -> A -> B)
```

**Time**: O(n), **Space**: O(1) - at most 26 unique tasks

---

### 8. Merge K Sorted Arrays

```java
public List<Integer> mergeKArrays(int[][] arrays) {
    PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    
    // Add first element of each array: [value, arrayIndex, elementIndex]
    for (int i = 0; i < arrays.length; i++) {
        if (arrays[i].length > 0) {
            minHeap.offer(new int[]{arrays[i][0], i, 0});
        }
    }
    
    List<Integer> result = new ArrayList<>();
    
    while (!minHeap.isEmpty()) {
        int[] current = minHeap.poll();
        result.add(current[0]);
        
        int arrayIdx = current[1];
        int elemIdx = current[2];
        
        if (elemIdx + 1 < arrays[arrayIdx].length) {
            minHeap.offer(new int[]{
                arrays[arrayIdx][elemIdx + 1], 
                arrayIdx, 
                elemIdx + 1
            });
        }
    }
    
    return result;
}
```

**Time**: O(n log k), **Space**: O(k)

---

### 9. Reorganize String

```java
public String reorganizeString(String s) {
    Map<Character, Integer> freq = new HashMap<>();
    for (char c : s.toCharArray()) {
        freq.put(c, freq.getOrDefault(c, 0) + 1);
    }
    
    PriorityQueue<Map.Entry<Character, Integer>> maxHeap = 
        new PriorityQueue<>((a, b) -> b.getValue() - a.getValue());
    maxHeap.addAll(freq.entrySet());
    
    StringBuilder result = new StringBuilder();
    Map.Entry<Character, Integer> prev = null;
    
    while (!maxHeap.isEmpty() || prev != null) {
        if (prev != null && maxHeap.isEmpty()) {
            return ""; // Impossible
        }
        
        Map.Entry<Character, Integer> current = maxHeap.poll();
        result.append(current.getKey());
        current.setValue(current.getValue() - 1);
        
        if (prev != null && prev.getValue() > 0) {
            maxHeap.offer(prev);
        }
        
        prev = current.getValue() > 0 ? current : null;
    }
    
    return result.toString();
}

// Example: s = "aab"
// Result: "aba"
```

**Time**: O(n log k), **Space**: O(k) where k = unique characters

---

### 10. Sliding Window Median

```java
public double[] medianSlidingWindow(int[] nums, int k) {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    double[] result = new double[nums.length - k + 1];
    
    for (int i = 0; i < nums.length; i++) {
        // Add element
        if (maxHeap.isEmpty() || nums[i] <= maxHeap.peek()) {
            maxHeap.offer(nums[i]);
        } else {
            minHeap.offer(nums[i]);
        }
        
        // Balance heaps
        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        } else if (minHeap.size() > maxHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
        
        // Remove element outside window
        if (i >= k) {
            int toRemove = nums[i - k];
            if (toRemove <= maxHeap.peek()) {
                maxHeap.remove(toRemove);
            } else {
                minHeap.remove(toRemove);
            }
            
            // Rebalance
            if (maxHeap.size() > minHeap.size() + 1) {
                minHeap.offer(maxHeap.poll());
            } else if (minHeap.size() > maxHeap.size()) {
                maxHeap.offer(minHeap.poll());
            }
        }
        
        // Calculate median
        if (i >= k - 1) {
            if (k % 2 == 0) {
                result[i - k + 1] = ((double)maxHeap.peek() + minHeap.peek()) / 2.0;
            } else {
                result[i - k + 1] = maxHeap.peek();
            }
        }
    }
    
    return result;
}
```

**Time**: O(n * k) due to remove operation, **Space**: O(k)

---

### 11. Ugly Number II

```java
public int nthUglyNumber(int n) {
    PriorityQueue<Long> minHeap = new PriorityQueue<>();
    Set<Long> seen = new HashSet<>();
    
    minHeap.offer(1L);
    seen.add(1L);
    
    long ugly = 1;
    int[] primes = {2, 3, 5};
    
    for (int i = 0; i < n; i++) {
        ugly = minHeap.poll();
        
        for (int prime : primes) {
            long next = ugly * prime;
            if (seen.add(next)) {
                minHeap.offer(next);
            }
        }
    }
    
    return (int) ugly;
}

// Example: n = 10
// Result: 12 (1,2,3,4,5,6,8,9,10,12)
```

**Time**: O(n log n), **Space**: O(n)

---

### 12. Meeting Rooms II (Minimum Conference Rooms)

```java
public int minMeetingRooms(int[][] intervals) {
    if (intervals.length == 0) return 0;
    
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    
    minHeap.offer(intervals[0][1]); // Add first meeting end time
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] >= minHeap.peek()) {
            minHeap.poll(); // Room is free
        }
        minHeap.offer(intervals[i][1]);
    }
    
    return minHeap.size();
}

// Example: intervals = [[0,30],[5,10],[15,20]]
// Result: 2 (need 2 rooms)
```

**Time**: O(n log n), **Space**: O(n)

---

## Real-World Use Cases

### 1. **Operating System - Process Scheduling**
```java
// CPU schedules processes based on priority
PriorityQueue<Process> scheduler = new PriorityQueue<>((a, b) -> a.priority - b.priority);
```

### 2. **Dijkstra's Shortest Path Algorithm**
```java
// Find shortest path in weighted graph
PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.distance - b.distance);
```

### 3. **Huffman Coding (Data Compression)**
```java
// Build optimal prefix-free code
PriorityQueue<HuffmanNode> pq = new PriorityQueue<>((a, b) -> a.freq - b.freq);
```

### 4. **Load Balancing**
```java
// Assign tasks to least loaded server
PriorityQueue<Server> servers = new PriorityQueue<>((a, b) -> a.load - b.load);
```

### 5. **Event-Driven Simulation**
```java
// Process events in chronological order
PriorityQueue<Event> events = new PriorityQueue<>((a, b) -> a.time - b.time);
```

### 6. **A* Pathfinding Algorithm**
```java
// Find optimal path in games/maps
PriorityQueue<Node> openSet = new PriorityQueue<>((a, b) -> a.fScore - b.fScore);
```

### 7. **Stock Price Monitoring**
```java
// Track top N stocks by price change
PriorityQueue<Stock> topGainers = new PriorityQueue<>((a, b) -> 
    Double.compare(b.changePercent, a.changePercent)
);
```

---

## Common Patterns

### Pattern 1: Top K Elements
Use min heap of size k, remove smallest when size > k

### Pattern 2: K Smallest Elements
Use max heap of size k, remove largest when size > k

### Pattern 3: Two Heaps (Median)
Max heap for left half, min heap for right half

### Pattern 4: Merge K Sorted
Min heap to track smallest element from each source

### Pattern 5: Scheduling/Intervals
Min heap to track end times or deadlines

---

## Advantages

1. **Efficient Priority Access**: O(1) to get highest priority
2. **Dynamic**: Can add/remove elements efficiently
3. **Flexible Ordering**: Custom comparators for any priority logic
4. **Space Efficient**: Compact array representation
5. **Predictable Performance**: O(log n) for most operations

---

## Disadvantages

1. **No Random Access**: O(n) to find arbitrary element
2. **Not Sorted**: Only root is guaranteed to be min/max
3. **Remove Specific Element**: O(n) operation
4. **No Iteration Order**: Iterator doesn't guarantee order

---

## Interview Tips

1. **Identify Priority**: Look for "kth largest/smallest", "top k", "median"
2. **Choose Heap Type**: Min heap for k largest, max heap for k smallest
3. **Size Matters**: Keep heap size = k for space optimization
4. **Two Heaps**: Consider for median or balanced partitioning
5. **Custom Comparator**: Don't forget to define comparison logic
6. **Edge Cases**: Empty heap, k > n, duplicate priorities

---

## Key Takeaways

1. **Priority Queue = Binary Heap** (usually)
2. **O(log n) insert/remove**, O(1) peek
3. **Min heap by default** in Java
4. **Use for "top k" problems** - most efficient approach
5. **Two heaps pattern** for median problems
6. **Custom comparator** for complex priority logic
7. **Not for random access** - use TreeSet if needed

---

## Comparison with Other Data Structures

| Data Structure | Insert | Delete | Find Min/Max | Search |
|---------------|--------|--------|--------------|--------|
| Priority Queue | O(log n) | O(log n) | O(1) | O(n) |
| Sorted Array | O(n) | O(n) | O(1) | O(log n) |
| BST | O(log n) | O(log n) | O(log n) | O(log n) |
| Hash Table | O(1) | O(1) | O(n) | O(1) |
| Unsorted Array | O(1) | O(n) | O(n) | O(n) |

**Priority Queue wins when**: Need frequent min/max access with dynamic insertions/deletions
