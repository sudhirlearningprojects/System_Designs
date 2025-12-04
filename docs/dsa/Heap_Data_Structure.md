# Heap Data Structure - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Min Heap](#min-heap)
3. [Max Heap](#max-heap)
4. [Key Differences](#key-differences)
5. [Heap Operations](#heap-operations)
6. [Implementation](#implementation)
7. [Time & Space Complexity](#time--space-complexity)
8. [Use Cases](#use-cases)
9. [DSA Problems](#dsa-problems)

---

## Introduction

A **Heap** is a specialized tree-based data structure that satisfies the heap property. It's a complete binary tree where nodes follow a specific ordering rule.

### Properties
- **Complete Binary Tree**: All levels are filled except possibly the last, which is filled from left to right
- **Heap Property**: Parent-child relationship follows specific ordering
- **Array Representation**: Efficiently stored in arrays without pointers

### Array Indexing (0-based)
```
For node at index i:
- Parent: (i - 1) / 2
- Left Child: 2 * i + 1
- Right Child: 2 * i + 2
```

---

## Min Heap

### Definition
A Min Heap is a complete binary tree where **every parent node is smaller than or equal to its children**.

### Structure
```
        1
       / \
      3   2
     / \ / \
    7  5 4  6
```

**Array Representation**: `[1, 3, 2, 7, 5, 4, 6]`

### Heap Property
```
parent ≤ left_child
parent ≤ right_child
```

### Characteristics
- **Root**: Contains the minimum element
- **Access Min**: O(1) - always at root
- **Ordering**: Only parent-child relationship matters, not left-right siblings

---

## Max Heap

### Definition
A Max Heap is a complete binary tree where **every parent node is greater than or equal to its children**.

### Structure
```
        10
       /  \
      8    9
     / \  / \
    4  5 6  7
```

**Array Representation**: `[10, 8, 9, 4, 5, 6, 7]`

### Heap Property
```
parent ≥ left_child
parent ≥ right_child
```

### Characteristics
- **Root**: Contains the maximum element
- **Access Max**: O(1) - always at root
- **Ordering**: Only parent-child relationship matters

---

## Key Differences

| Feature | Min Heap | Max Heap |
|---------|----------|----------|
| **Root Element** | Minimum | Maximum |
| **Parent-Child Rule** | Parent ≤ Children | Parent ≥ Children |
| **Extract Operation** | Returns minimum | Returns maximum |
| **Use Case** | Find smallest elements | Find largest elements |
| **Priority Queue** | Lowest priority first | Highest priority first |
| **Example** | `[1, 3, 2, 7, 5]` | `[10, 8, 9, 4, 5]` |

---

## Heap Operations

### 1. Insert (Add Element)

**Process**:
1. Add element at the end (maintain complete tree)
2. **Heapify Up** (Bubble Up): Compare with parent and swap if needed
3. Repeat until heap property is satisfied

**Min Heap Insert Example**:
```
Insert 1 into [3, 5, 7, 9, 11]

Step 1: Add at end
        3
       / \
      5   7
     / \ /
    9 11 1  ← Added here

Step 2: Compare with parent (7)
1 < 7, swap
        3
       / \
      5   1
     / \ /
    9 11 7

Step 3: Compare with parent (3)
1 < 3, swap
        1
       / \
      5   3
     / \ /
    9 11 7

Result: [1, 5, 3, 9, 11, 7]
```

**Time Complexity**: O(log n)

---

### 2. Extract Min/Max (Remove Root)

**Process**:
1. Replace root with last element
2. Remove last element
3. **Heapify Down** (Bubble Down): Compare with children and swap with smaller/larger child
4. Repeat until heap property is satisfied

**Min Heap Extract Example**:
```
Extract from [1, 3, 2, 7, 5, 4, 6]

Step 1: Replace root with last element
        6
       / \
      3   2
     / \ /
    7  5 4

Step 2: Compare with children (3, 2)
Swap with smaller child (2)
        2
       / \
      3   6
     / \ /
    7  5 4

Step 3: Compare with children (4)
Swap with 4
        2
       / \
      3   4
     / \ /
    7  5 6

Result: [2, 3, 4, 7, 5, 6]
Extracted: 1
```

**Time Complexity**: O(log n)

---

### 3. Peek (Get Root)

**Process**: Return root element without removing

**Min Heap**: Returns minimum
**Max Heap**: Returns maximum

**Time Complexity**: O(1)

---

### 4. Heapify (Build Heap from Array)

**Process**:
1. Start from last non-leaf node: `(n/2) - 1`
2. Heapify down each node
3. Move backwards to root

**Example - Build Min Heap from [9, 5, 6, 2, 3]**:
```
Step 1: Start with array
    9
   / \
  5   6
 / \
2   3

Step 2: Heapify from index 1 (node 5)
Compare 5 with children (2, 3)
Swap with 2
    9
   / \
  2   6
 / \
5   3

Step 3: Heapify from index 0 (node 9)
Compare 9 with children (2, 6)
Swap with 2
    2
   / \
  9   6
 / \
5   3

Step 4: Heapify down 9
Compare 9 with children (5, 3)
Swap with 3
    2
   / \
  3   6
 / \
5   9

Result: [2, 3, 6, 5, 9]
```

**Time Complexity**: O(n)

---

### 5. Delete Arbitrary Element

**Process**:
1. Find element (O(n) scan)
2. Replace with last element
3. Remove last element
4. Heapify up or down as needed

**Time Complexity**: O(n) for search + O(log n) for heapify = O(n)

---

### 6. Increase/Decrease Key

**Decrease Key (Min Heap)**:
1. Decrease value
2. Heapify up (might violate parent property)

**Increase Key (Min Heap)**:
1. Increase value
2. Heapify down (might violate child property)

**Time Complexity**: O(log n)

---

## Implementation

### Min Heap Implementation (Java)

```java
public class MinHeap {
    private int[] heap;
    private int size;
    private int capacity;
    
    public MinHeap(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.heap = new int[capacity];
    }
    
    private int parent(int i) { return (i - 1) / 2; }
    private int leftChild(int i) { return 2 * i + 1; }
    private int rightChild(int i) { return 2 * i + 2; }
    
    private void swap(int i, int j) {
        int temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
    
    // Insert element
    public void insert(int value) {
        if (size == capacity) throw new IllegalStateException("Heap is full");
        
        heap[size] = value;
        int current = size;
        size++;
        
        // Heapify up
        while (current > 0 && heap[current] < heap[parent(current)]) {
            swap(current, parent(current));
            current = parent(current);
        }
    }
    
    // Extract minimum
    public int extractMin() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        
        int min = heap[0];
        heap[0] = heap[size - 1];
        size--;
        
        heapifyDown(0);
        return min;
    }
    
    // Heapify down
    private void heapifyDown(int i) {
        int smallest = i;
        int left = leftChild(i);
        int right = rightChild(i);
        
        if (left < size && heap[left] < heap[smallest])
            smallest = left;
        
        if (right < size && heap[right] < heap[smallest])
            smallest = right;
        
        if (smallest != i) {
            swap(i, smallest);
            heapifyDown(smallest);
        }
    }
    
    // Peek minimum
    public int peek() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return heap[0];
    }
    
    // Build heap from array
    public static MinHeap buildHeap(int[] arr) {
        MinHeap heap = new MinHeap(arr.length);
        heap.size = arr.length;
        System.arraycopy(arr, 0, heap.heap, 0, arr.length);
        
        // Start from last non-leaf node
        for (int i = (heap.size / 2) - 1; i >= 0; i--) {
            heap.heapifyDown(i);
        }
        return heap;
    }
}
```

### Max Heap Implementation (Java)

```java
public class MaxHeap {
    private int[] heap;
    private int size;
    private int capacity;
    
    public MaxHeap(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.heap = new int[capacity];
    }
    
    private int parent(int i) { return (i - 1) / 2; }
    private int leftChild(int i) { return 2 * i + 1; }
    private int rightChild(int i) { return 2 * i + 2; }
    
    private void swap(int i, int j) {
        int temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
    
    public void insert(int value) {
        if (size == capacity) throw new IllegalStateException("Heap is full");
        
        heap[size] = value;
        int current = size;
        size++;
        
        // Heapify up
        while (current > 0 && heap[current] > heap[parent(current)]) {
            swap(current, parent(current));
            current = parent(current);
        }
    }
    
    public int extractMax() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        
        int max = heap[0];
        heap[0] = heap[size - 1];
        size--;
        
        heapifyDown(0);
        return max;
    }
    
    private void heapifyDown(int i) {
        int largest = i;
        int left = leftChild(i);
        int right = rightChild(i);
        
        if (left < size && heap[left] > heap[largest])
            largest = left;
        
        if (right < size && heap[right] > heap[largest])
            largest = right;
        
        if (largest != i) {
            swap(i, largest);
            heapifyDown(largest);
        }
    }
    
    public int peek() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
        return heap[0];
    }
}
```

---

## Time & Space Complexity

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| **Insert** | O(log n) | O(1) |
| **Extract Min/Max** | O(log n) | O(1) |
| **Peek** | O(1) | O(1) |
| **Build Heap** | O(n) | O(1) |
| **Delete** | O(n) | O(1) |
| **Search** | O(n) | O(1) |
| **Heapify Up** | O(log n) | O(1) |
| **Heapify Down** | O(log n) | O(1) |

**Overall Space**: O(n) for storing n elements

---

## Use Cases

### Min Heap Use Cases

1. **Dijkstra's Shortest Path Algorithm**
   - Extract minimum distance vertex efficiently

2. **Prim's Minimum Spanning Tree**
   - Find minimum weight edge

3. **Huffman Coding**
   - Build optimal prefix codes

4. **Task Scheduling**
   - Process tasks with earliest deadline first

5. **Median Maintenance**
   - Combined with max heap for running median

6. **K Largest Elements**
   - Maintain min heap of size K

### Max Heap Use Cases

1. **Heap Sort**
   - Sort in ascending order

2. **Priority Queue (High Priority First)**
   - Process highest priority tasks

3. **K Smallest Elements**
   - Maintain max heap of size K

4. **Maximum CPU Scheduling**
   - Schedule process with highest burst time

5. **Top K Frequent Elements**
   - Track most frequent items

---

## DSA Problems

### Problem 1: Kth Largest Element in Array

**Problem**: Find the kth largest element in an unsorted array.

**Approach**: Use Min Heap of size K

```java
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }
    
    return minHeap.peek();
}
```

**Time**: O(n log k), **Space**: O(k)

**Example**:
```
Input: [3, 2, 1, 5, 6, 4], k = 2
Output: 5

Process:
[3] → [2,3] → [2,3] (remove 1) → [3,5] → [5,6] → [5,6] (remove 4)
Result: 5
```

---

### Problem 2: Merge K Sorted Lists

**Problem**: Merge k sorted linked lists into one sorted list.

**Approach**: Use Min Heap to track smallest elements

```java
public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> minHeap = new PriorityQueue<>((a, b) -> a.val - b.val);
    
    // Add first node of each list
    for (ListNode list : lists) {
        if (list != null) minHeap.offer(list);
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

**Time**: O(n log k), **Space**: O(k)

---

### Problem 3: Top K Frequent Elements

**Problem**: Find k most frequent elements in array.

**Approach**: Use Max Heap with frequency count

```java
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int num : nums) {
        freq.put(num, freq.getOrDefault(num, 0) + 1);
    }
    
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> freq.get(b) - freq.get(a));
    maxHeap.addAll(freq.keySet());
    
    int[] result = new int[k];
    for (int i = 0; i < k; i++) {
        result[i] = maxHeap.poll();
    }
    
    return result;
}
```

**Time**: O(n log n), **Space**: O(n)

---

### Problem 4: Find Median from Data Stream

**Problem**: Design a data structure that supports adding numbers and finding median.

**Approach**: Use Max Heap (left half) + Min Heap (right half)

```java
class MedianFinder {
    PriorityQueue<Integer> maxHeap; // Left half (smaller elements)
    PriorityQueue<Integer> minHeap; // Right half (larger elements)
    
    public MedianFinder() {
        maxHeap = new PriorityQueue<>((a, b) -> b - a);
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
```

**Time**: addNum O(log n), findMedian O(1)

**Example**:
```
addNum(1) → maxHeap: [1], minHeap: []
addNum(2) → maxHeap: [1], minHeap: [2]
findMedian() → 1.5

addNum(3) → maxHeap: [2, 1], minHeap: [3]
findMedian() → 2
```

---

### Problem 5: Kth Smallest Element in Sorted Matrix

**Problem**: Find kth smallest element in n x n matrix where rows and columns are sorted.

**Approach**: Use Min Heap

```java
public int kthSmallest(int[][] matrix, int k) {
    int n = matrix.length;
    PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    
    // Add first element of each row
    for (int i = 0; i < Math.min(n, k); i++) {
        minHeap.offer(new int[]{matrix[i][0], i, 0});
    }
    
    int result = 0;
    for (int i = 0; i < k; i++) {
        int[] curr = minHeap.poll();
        result = curr[0];
        int row = curr[1];
        int col = curr[2];
        
        if (col + 1 < n) {
            minHeap.offer(new int[]{matrix[row][col + 1], row, col + 1});
        }
    }
    
    return result;
}
```

**Time**: O(k log n), **Space**: O(n)

---

### Problem 6: Task Scheduler

**Problem**: Schedule tasks with cooling period between same tasks.

**Approach**: Use Max Heap for frequency

```java
public int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char task : tasks) {
        freq[task - 'A']++;
    }
    
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
    for (int f : freq) {
        if (f > 0) maxHeap.offer(f);
    }
    
    int time = 0;
    while (!maxHeap.isEmpty()) {
        List<Integer> temp = new ArrayList<>();
        
        for (int i = 0; i <= n; i++) {
            if (!maxHeap.isEmpty()) {
                int f = maxHeap.poll();
                if (f > 1) temp.add(f - 1);
            }
            time++;
            if (maxHeap.isEmpty() && temp.isEmpty()) break;
        }
        
        maxHeap.addAll(temp);
    }
    
    return time;
}
```

---

### Problem 7: Reorganize String

**Problem**: Rearrange string so no two adjacent characters are same.

**Approach**: Use Max Heap by frequency

```java
public String reorganizeString(String s) {
    Map<Character, Integer> freq = new HashMap<>();
    for (char c : s.toCharArray()) {
        freq.put(c, freq.getOrDefault(c, 0) + 1);
    }
    
    PriorityQueue<Character> maxHeap = new PriorityQueue<>((a, b) -> freq.get(b) - freq.get(a));
    maxHeap.addAll(freq.keySet());
    
    StringBuilder result = new StringBuilder();
    Character prev = null;
    
    while (!maxHeap.isEmpty()) {
        char curr = maxHeap.poll();
        result.append(curr);
        
        if (prev != null && freq.get(prev) > 0) {
            maxHeap.offer(prev);
        }
        
        freq.put(curr, freq.get(curr) - 1);
        prev = curr;
    }
    
    return result.length() == s.length() ? result.toString() : "";
}
```

---

### Problem 8: Sliding Window Median

**Problem**: Find median in each sliding window of size k.

**Approach**: Two heaps (max + min)

```java
public double[] medianSlidingWindow(int[] nums, int k) {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> Integer.compare(b, a));
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    double[] result = new double[nums.length - k + 1];
    
    for (int i = 0; i < nums.length; i++) {
        // Add element
        maxHeap.offer(nums[i]);
        minHeap.offer(maxHeap.poll());
        if (maxHeap.size() < minHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
        
        // Remove element outside window
        if (i >= k) {
            if (nums[i - k] <= maxHeap.peek()) {
                maxHeap.remove(nums[i - k]);
            } else {
                minHeap.remove(nums[i - k]);
            }
        }
        
        // Calculate median
        if (i >= k - 1) {
            if (k % 2 == 0) {
                result[i - k + 1] = ((double) maxHeap.peek() + minHeap.peek()) / 2.0;
            } else {
                result[i - k + 1] = maxHeap.peek();
            }
        }
    }
    
    return result;
}
```

---

### Problem 9: Meeting Rooms II

**Problem**: Find minimum number of meeting rooms required.

**Approach**: Use Min Heap for end times

```java
public int minMeetingRooms(int[][] intervals) {
    if (intervals.length == 0) return 0;
    
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    
    minHeap.offer(intervals[0][1]);
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] >= minHeap.peek()) {
            minHeap.poll();
        }
        minHeap.offer(intervals[i][1]);
    }
    
    return minHeap.size();
}
```

**Time**: O(n log n), **Space**: O(n)

---

### Problem 10: Ugly Number II

**Problem**: Find nth ugly number (numbers whose prime factors are only 2, 3, 5).

**Approach**: Use Min Heap

```java
public int nthUglyNumber(int n) {
    PriorityQueue<Long> minHeap = new PriorityQueue<>();
    Set<Long> seen = new HashSet<>();
    
    minHeap.offer(1L);
    seen.add(1L);
    
    long ugly = 1;
    for (int i = 0; i < n; i++) {
        ugly = minHeap.poll();
        
        for (int factor : new int[]{2, 3, 5}) {
            long next = ugly * factor;
            if (seen.add(next)) {
                minHeap.offer(next);
            }
        }
    }
    
    return (int) ugly;
}
```

---

## Summary

### When to Use Min Heap
- Find minimum element repeatedly
- K largest elements
- Merge sorted sequences
- Dijkstra's algorithm
- Huffman coding

### When to Use Max Heap
- Find maximum element repeatedly
- K smallest elements
- Heap sort
- Priority scheduling (high priority first)
- Top K frequent elements

### Key Takeaways
1. Heaps provide O(1) access to min/max
2. Insert and extract are O(log n)
3. Building heap from array is O(n)
4. Perfect for priority queue implementations
5. Use two heaps for median problems
6. Combine with hash maps for frequency problems

---

**Practice Resources**:
- LeetCode: Heap tag problems
- HackerRank: Heap section
- GeeksforGeeks: Heap data structure

**Related Topics**:
- Priority Queue
- Binary Trees
- Sorting Algorithms
- Graph Algorithms (Dijkstra, Prim)
