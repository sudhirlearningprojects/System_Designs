# Find Median from Data Stream

## Problem Statement

Design a data structure that supports adding integers from a data stream and finding the median of all elements read so far in an efficient way.

**Operations:**
- `addNum(int num)`: Add a number to the data stream
- `findMedian()`: Return the median of all elements so far

**Median Definition:**
- Odd count: Middle element
- Even count: Average of two middle elements

**Example:**
```
Stream: 5, 15, 1, 3

After 5:      [5]           → median = 5
After 15:     [5, 15]       → median = (5+15)/2 = 10
After 1:      [1, 5, 15]    → median = 5
After 3:      [1, 3, 5, 15] → median = (3+5)/2 = 4
```

---

## Solution Approaches

### Approach 1: Two Heaps (Optimal)

**Time Complexity:** 
- addNum: O(log n)
- findMedian: O(1)

**Space Complexity:** O(n)

```java
class MedianFinder {
    private PriorityQueue<Integer> maxHeap; // Left half (max heap)
    private PriorityQueue<Integer> minHeap; // Right half (min heap)
    
    public MedianFinder() {
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        minHeap = new PriorityQueue<>();
    }
    
    public void addNum(int num) {
        // Add to max heap first
        maxHeap.offer(num);
        
        // Balance: move largest from maxHeap to minHeap
        minHeap.offer(maxHeap.poll());
        
        // Maintain size: maxHeap size >= minHeap size
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

---

### Approach 2: Sorted List (Simple but Slower)

**Time Complexity:** 
- addNum: O(n)
- findMedian: O(1)

**Space Complexity:** O(n)

```java
class MedianFinderSorted {
    private List<Integer> list;
    
    public MedianFinderSorted() {
        list = new ArrayList<>();
    }
    
    public void addNum(int num) {
        int pos = Collections.binarySearch(list, num);
        if (pos < 0) pos = -(pos + 1);
        list.add(pos, num);
    }
    
    public double findMedian() {
        int n = list.size();
        if (n % 2 == 1) {
            return list.get(n / 2);
        }
        return (list.get(n / 2 - 1) + list.get(n / 2)) / 2.0;
    }
}
```

---

### Approach 3: Brute Force (Sort Every Time)

**Time Complexity:** 
- addNum: O(1)
- findMedian: O(n log n)

**Space Complexity:** O(n)

```java
class MedianFinderBrute {
    private List<Integer> list;
    
    public MedianFinderBrute() {
        list = new ArrayList<>();
    }
    
    public void addNum(int num) {
        list.add(num);
    }
    
    public double findMedian() {
        Collections.sort(list);
        int n = list.size();
        
        if (n % 2 == 1) {
            return list.get(n / 2);
        }
        return (list.get(n / 2 - 1) + list.get(n / 2)) / 2.0;
    }
}
```

---

## Algorithm Walkthrough

### Two Heaps Approach: Stream [5, 15, 1, 3]

**Invariants:**
1. maxHeap contains smaller half (max at top)
2. minHeap contains larger half (min at top)
3. maxHeap.size() = minHeap.size() OR maxHeap.size() = minHeap.size() + 1

```
Initial:
  maxHeap: []
  minHeap: []

Step 1: Add 5
  maxHeap.offer(5)           → maxHeap: [5]
  minHeap.offer(maxHeap.poll()) → maxHeap: [], minHeap: [5]
  Balance: maxHeap.size() < minHeap.size()
    maxHeap.offer(minHeap.poll()) → maxHeap: [5], minHeap: []
  
  Median: maxHeap.peek() = 5

Step 2: Add 15
  maxHeap.offer(15)          → maxHeap: [15, 5]
  minHeap.offer(maxHeap.poll()) → maxHeap: [5], minHeap: [15]
  Balance: sizes equal, no action
  
  Median: (5 + 15) / 2 = 10

Step 3: Add 1
  maxHeap.offer(1)           → maxHeap: [5, 1]
  minHeap.offer(maxHeap.poll()) → maxHeap: [1], minHeap: [5, 15]
  Balance: maxHeap.size() < minHeap.size()
    maxHeap.offer(minHeap.poll()) → maxHeap: [5, 1], minHeap: [15]
  
  Median: maxHeap.peek() = 5

Step 4: Add 3
  maxHeap.offer(3)           → maxHeap: [5, 3, 1]
  minHeap.offer(maxHeap.poll()) → maxHeap: [3, 1], minHeap: [5, 15]
  Balance: sizes equal, no action
  
  Median: (3 + 5) / 2 = 4

Final State:
  maxHeap: [3, 1]  (smaller half)
  minHeap: [5, 15] (larger half)
  Sorted view: [1, 3, 5, 15]
```

---

## Complete Implementation

```java
import java.util.*;

class MedianFinder {
    private PriorityQueue<Integer> maxHeap; // Smaller half
    private PriorityQueue<Integer> minHeap; // Larger half
    
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

// Alternative: More explicit balancing
class MedianFinderExplicit {
    private PriorityQueue<Integer> maxHeap;
    private PriorityQueue<Integer> minHeap;
    
    public MedianFinderExplicit() {
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        minHeap = new PriorityQueue<>();
    }
    
    public void addNum(int num) {
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            maxHeap.offer(num);
        } else {
            minHeap.offer(num);
        }
        
        // Balance heaps
        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        } else if (minHeap.size() > maxHeap.size()) {
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

// Test class
public class Solution {
    
    public static boolean doTestsPass() {
        MedianFinder mf = new MedianFinder();
        
        // Test stream: 5, 15, 1, 3
        mf.addNum(5);
        if (Math.abs(mf.findMedian() - 5.0) > 0.001) return false;
        
        mf.addNum(15);
        if (Math.abs(mf.findMedian() - 10.0) > 0.001) return false;
        
        mf.addNum(1);
        if (Math.abs(mf.findMedian() - 5.0) > 0.001) return false;
        
        mf.addNum(3);
        if (Math.abs(mf.findMedian() - 4.0) > 0.001) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        MedianFinder mf = new MedianFinder();
        int[] stream = {5, 15, 1, 3};
        
        for (int num : stream) {
            mf.addNum(num);
            System.out.println("After adding " + num + ", median = " + mf.findMedian());
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testMedianFinder() {
    MedianFinder mf = new MedianFinder();
    
    // Test 1: Single element
    mf.addNum(5);
    assertEquals(5.0, mf.findMedian(), 0.001);
    
    // Test 2: Two elements
    mf.addNum(15);
    assertEquals(10.0, mf.findMedian(), 0.001);
    
    // Test 3: Three elements
    mf.addNum(1);
    assertEquals(5.0, mf.findMedian(), 0.001);
    
    // Test 4: Four elements
    mf.addNum(3);
    assertEquals(4.0, mf.findMedian(), 0.001);
    
    // Test 5: Negative numbers
    MedianFinder mf2 = new MedianFinder();
    mf2.addNum(-1);
    mf2.addNum(-2);
    mf2.addNum(-3);
    assertEquals(-2.0, mf2.findMedian(), 0.001);
    
    // Test 6: Duplicates
    MedianFinder mf3 = new MedianFinder();
    mf3.addNum(1);
    mf3.addNum(1);
    mf3.addNum(1);
    assertEquals(1.0, mf3.findMedian(), 0.001);
    
    // Test 7: Large numbers
    MedianFinder mf4 = new MedianFinder();
    mf4.addNum(Integer.MAX_VALUE);
    mf4.addNum(Integer.MAX_VALUE - 1);
    assertTrue(mf4.findMedian() > 0);
}
```

---

## Visual Representation

### Two Heaps Structure

```
Stream: [5, 15, 1, 3]

After adding all elements:

maxHeap (Max Heap)    minHeap (Min Heap)
    [3, 1]                [5, 15]
      3                      5
     /                      /
    1                     15

Smaller half          Larger half
(max at top)          (min at top)

Sorted view: [1, 3, 5, 15]
             ↑   ↑
          Median = (3 + 5) / 2 = 4
```

### Heap Balancing Rules

```
Case 1: Odd total count
  maxHeap.size() = minHeap.size() + 1
  Median = maxHeap.peek()
  
  Example: [1, 3, 5]
    maxHeap: [3, 1]
    minHeap: [5]
    Median = 3

Case 2: Even total count
  maxHeap.size() = minHeap.size()
  Median = (maxHeap.peek() + minHeap.peek()) / 2
  
  Example: [1, 3, 5, 15]
    maxHeap: [3, 1]
    minHeap: [5, 15]
    Median = (3 + 5) / 2 = 4
```

---

## Edge Cases

1. **Single element:** Return that element
2. **Two elements:** Return average
3. **Negative numbers:** Works correctly
4. **Duplicates:** Allowed (problem says no duplicates, but solution handles it)
5. **Large numbers:** Watch for integer overflow in average
6. **All same values:** Median is that value
7. **Sorted stream:** Still O(log n) per add

---

## Complexity Analysis

| Approach | addNum | findMedian | Space | Notes |
|----------|--------|------------|-------|-------|
| Two Heaps | O(log n) | O(1) | O(n) | **Optimal** |
| Sorted List | O(n) | O(1) | O(n) | Binary search + insert |
| Brute Force | O(1) | O(n log n) | O(n) | Sort every query |
| BST | O(log n) avg | O(log n) | O(n) | Balanced BST needed |

**Why Two Heaps is Optimal:**
- Heap operations: O(log n)
- Median access: O(1)
- Best balance for streaming data

---

## Related Problems

1. **Sliding Window Median** - Median in fixed-size window
2. **Find K-th Largest Element** - Similar heap technique
3. **Top K Frequent Elements** - Heap-based solution
4. **Kth Largest Element in Stream** - Single heap
5. **IPO** - Two heaps for optimization
6. **Meeting Rooms II** - Heap for scheduling

---

## Interview Tips

### Clarification Questions
1. Can there be duplicates? (Usually yes)
2. Are numbers always integers? (Usually yes)
3. What's the range of numbers? (Check for overflow)
4. How many numbers in stream? (Affects space consideration)
5. Need to support removal? (Changes approach)

### Approach Explanation
1. "Use two heaps to maintain sorted halves"
2. "maxHeap stores smaller half, minHeap stores larger half"
3. "Keep heaps balanced: sizes differ by at most 1"
4. "Median is either top of maxHeap or average of both tops"
5. "O(log n) insert, O(1) median query"

### Common Mistakes
- Wrong heap types (using min heap for both)
- Not balancing heaps properly
- Integer overflow when calculating average
- Forgetting to handle odd/even count cases
- Not maintaining heap size invariant

### Why Two Heaps?
```
Alternative: Sorted array
  Insert: O(n) - need to shift elements
  Query: O(1)

Alternative: Unsorted array
  Insert: O(1)
  Query: O(n log n) - need to sort

Two Heaps:
  Insert: O(log n) - heap operation
  Query: O(1) - just peek
  Best of both worlds!
```

---

## Real-World Applications

1. **Statistics** - Running median calculation
2. **Network Monitoring** - Median latency tracking
3. **Financial Systems** - Real-time price analysis
4. **Sensor Data** - Median filtering for noise reduction
5. **Load Balancing** - Median request time
6. **Quality Control** - Median defect rate

---

## Key Takeaways

1. **Two Heaps Pattern:** Maintain two halves of sorted data
2. **Heap Types:** Max heap for smaller half, min heap for larger half
3. **Balancing:** Keep sizes equal or differ by 1
4. **Time Complexity:** O(log n) insert, O(1) query
5. **Space Complexity:** O(n) for storing all elements
6. **Median Access:** Constant time using heap tops
7. **Optimal Solution:** Best for streaming data with frequent queries

---

## Additional Notes

### Why This Balancing Strategy?

```java
// Always add to maxHeap first, then rebalance
maxHeap.offer(num);
minHeap.offer(maxHeap.poll());

if (maxHeap.size() < minHeap.size()) {
    maxHeap.offer(minHeap.poll());
}
```

**Ensures:**
1. All elements in maxHeap ≤ all elements in minHeap
2. Size difference ≤ 1
3. Median always accessible from heap tops

### Alternative Balancing

```java
// Add to appropriate heap based on value
if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
    maxHeap.offer(num);
} else {
    minHeap.offer(num);
}

// Then balance sizes
if (maxHeap.size() > minHeap.size() + 1) {
    minHeap.offer(maxHeap.poll());
} else if (minHeap.size() > maxHeap.size()) {
    maxHeap.offer(minHeap.poll());
}
```

Both approaches work, first is more concise.

### Handling Integer Overflow

```java
// Wrong: Can overflow
return (maxHeap.peek() + minHeap.peek()) / 2;

// Correct: Use double
return (maxHeap.peek() + minHeap.peek()) / 2.0;

// Or: Avoid overflow
int a = maxHeap.peek();
int b = minHeap.peek();
return a + (b - a) / 2.0;
```

### Extension: Sliding Window Median

For median in sliding window of size k:

```java
class SlidingWindowMedian {
    private TreeMap<Integer, Integer> left;  // Smaller half
    private TreeMap<Integer, Integer> right; // Larger half
    
    // Add element
    // Remove element
    // Rebalance
    // Get median
}
```

Requires multiset (TreeMap with counts) to handle duplicates and removals.

### Why Not Single Heap?

Single heap can only efficiently give min or max, not median:

```
Min Heap: O(1) for minimum, O(n) for median
Max Heap: O(1) for maximum, O(n) for median
Two Heaps: O(1) for median ✓
```

### Comparison with Balanced BST

```
Balanced BST (AVL/Red-Black):
  Insert: O(log n)
  Find median: O(log n) - need to find middle node
  More complex to implement

Two Heaps:
  Insert: O(log n)
  Find median: O(1) ✓
  Simpler to implement
```

Two heaps is preferred for this specific problem.
