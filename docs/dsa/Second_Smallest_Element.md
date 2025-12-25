# Second Smallest Element in Unsorted Array

## Problem Statement

Find the second smallest element in an unsorted array.

**Input:** Unsorted array of integers  
**Output:** Second smallest element

**Examples:**
```
Input: [12, 13, 1, 10, 34, 1]
Output: 10
Explanation: Smallest = 1, Second smallest = 10

Input: [5, 3, 8, 1, 9]
Output: 3
Explanation: Smallest = 1, Second smallest = 3

Input: [7, 7, 7]
Output: -1 or Integer.MAX_VALUE
Explanation: All elements same, no second smallest
```

---

## Solution Approaches

### Approach 1: Single Pass (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int secondSmallest(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int first = Integer.MAX_VALUE;
    int second = Integer.MAX_VALUE;
    
    for (int num : arr) {
        if (num < first) {
            second = first;
            first = num;
        } else if (num < second && num != first) {
            second = num;
        }
    }
    
    return second == Integer.MAX_VALUE ? -1 : second;
}
```

---

### Approach 2: Sorting

**Time Complexity:** O(n log n)  
**Space Complexity:** O(1) or O(n) depending on sort

```java
public static int secondSmallestSort(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    Arrays.sort(arr);
    
    // Find first element different from smallest
    for (int i = 1; i < arr.length; i++) {
        if (arr[i] != arr[0]) {
            return arr[i];
        }
    }
    
    return -1; // All elements same
}
```

---

### Approach 3: Using TreeSet (Handles Duplicates)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int secondSmallestTreeSet(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    TreeSet<Integer> set = new TreeSet<>();
    for (int num : arr) {
        set.add(num);
    }
    
    if (set.size() < 2) return -1;
    
    set.pollFirst(); // Remove smallest
    return set.first(); // Return second smallest
}
```

---

### Approach 4: Min Heap

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int secondSmallestHeap(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    Set<Integer> seen = new HashSet<>();
    
    for (int num : arr) {
        if (seen.add(num)) {
            minHeap.offer(num);
        }
    }
    
    if (minHeap.size() < 2) return -1;
    
    minHeap.poll(); // Remove smallest
    return minHeap.poll(); // Return second smallest
}
```

---

## Algorithm Walkthrough

### Example: [12, 13, 1, 10, 34, 1]

**Single Pass Approach:**

```
Initial: first = ∞, second = ∞

Step 1: num = 12
  12 < ∞ → second = ∞, first = 12
  State: first = 12, second = ∞

Step 2: num = 13
  13 < ∞ and 13 ≠ 12 → second = 13
  State: first = 12, second = 13

Step 3: num = 1
  1 < 12 → second = 12, first = 1
  State: first = 1, second = 12

Step 4: num = 10
  10 < 12 and 10 ≠ 1 → second = 10
  State: first = 1, second = 10

Step 5: num = 34
  34 ≥ 10 → no change
  State: first = 1, second = 10

Step 6: num = 1
  1 = first → no change (skip duplicate)
  State: first = 1, second = 10

Result: second = 10
```

### Example: [5, 3, 8, 1, 9]

```
Initial: first = ∞, second = ∞

num = 5:  first = 5, second = ∞
num = 3:  first = 3, second = 5
num = 8:  first = 3, second = 5 (8 ≥ 5)
num = 1:  first = 1, second = 3
num = 9:  first = 1, second = 3 (9 ≥ 3)

Result: second = 3
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Single pass (Optimal)
    public static int secondSmallest(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        int first = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        
        for (int num : arr) {
            if (num < first) {
                second = first;
                first = num;
            } else if (num < second && num != first) {
                second = num;
            }
        }
        
        return second == Integer.MAX_VALUE ? -1 : second;
    }
    
    // Approach 2: Sorting
    public static int secondSmallestSort(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        Arrays.sort(arr);
        
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] != arr[0]) {
                return arr[i];
            }
        }
        
        return -1;
    }
    
    // Approach 3: TreeSet
    public static int secondSmallestTreeSet(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        TreeSet<Integer> set = new TreeSet<>();
        for (int num : arr) {
            set.add(num);
        }
        
        if (set.size() < 2) return -1;
        
        set.pollFirst();
        return set.first();
    }
    
    // Approach 4: Min Heap
    public static int secondSmallestHeap(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        Set<Integer> seen = new HashSet<>();
        
        for (int num : arr) {
            if (seen.add(num)) {
                minHeap.offer(num);
            }
        }
        
        if (minHeap.size() < 2) return -1;
        
        minHeap.poll();
        return minHeap.poll();
    }
    
    public static boolean doTestsPass() {
        // Test 1: Normal case
        int[] test1 = {12, 13, 1, 10, 34, 1};
        if (secondSmallest(test1) != 10) return false;
        
        // Test 2: Simple case
        int[] test2 = {5, 3, 8, 1, 9};
        if (secondSmallest(test2) != 3) return false;
        
        // Test 3: Two elements
        int[] test3 = {5, 3};
        if (secondSmallest(test3) != 5) return false;
        
        // Test 4: All same
        int[] test4 = {7, 7, 7};
        if (secondSmallest(test4) != -1) return false;
        
        // Test 5: Single element
        int[] test5 = {5};
        if (secondSmallest(test5) != -1) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {12, 13, 1, 10, 34, 1};
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Second smallest: " + secondSmallest(arr));
    }
}
```

---

## Test Cases

```java
@Test
public void testSecondSmallest() {
    // Test 1: Normal case with duplicates
    assertEquals(10, secondSmallest(new int[]{12, 13, 1, 10, 34, 1}));
    
    // Test 2: Simple case
    assertEquals(3, secondSmallest(new int[]{5, 3, 8, 1, 9}));
    
    // Test 3: Two elements
    assertEquals(5, secondSmallest(new int[]{3, 5}));
    assertEquals(3, secondSmallest(new int[]{5, 3}));
    
    // Test 4: All same elements
    assertEquals(-1, secondSmallest(new int[]{7, 7, 7}));
    
    // Test 5: Single element
    assertEquals(-1, secondSmallest(new int[]{5}));
    
    // Test 6: Empty array
    assertEquals(-1, secondSmallest(new int[]{}));
    
    // Test 7: Null array
    assertEquals(-1, secondSmallest(null));
    
    // Test 8: Negative numbers
    assertEquals(-5, secondSmallest(new int[]{-10, -5, 0, 5}));
    
    // Test 9: Two distinct values with duplicates
    assertEquals(5, secondSmallest(new int[]{3, 3, 5, 5}));
    
    // Test 10: Already sorted
    assertEquals(2, secondSmallest(new int[]{1, 2, 3, 4, 5}));
}
```

---

## Visual Representation

### Single Pass Tracking

```
Array: [12, 13, 1, 10, 34, 1]

Step-by-step:
       first  second
Init:    ∞      ∞
12:      12     ∞
13:      12     13
1:       1      12
10:      1      10  ← Final
34:      1      10
1:       1      10

Result: 10
```

### Comparison of Approaches

```
Array: [5, 3, 8, 1, 9]

Single Pass:
  Track first=1, second=3
  O(n) time, O(1) space ✓

Sorting:
  Sort: [1, 3, 5, 8, 9]
  Return arr[1] = 3
  O(n log n) time

TreeSet:
  Set: {1, 3, 5, 8, 9}
  Remove 1, return 3
  O(n log n) time
```

---

## Edge Cases

1. **Empty array:** Return -1
2. **Null array:** Return -1
3. **Single element:** Return -1
4. **Two elements:** Return larger one
5. **All same elements:** Return -1
6. **Two distinct values:** Return larger one
7. **Negative numbers:** Works correctly
8. **Integer.MIN_VALUE in array:** Handle carefully

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Single Pass | O(n) | O(1) | **Optimal** |
| Sorting | O(n log n) | O(1) | Simple but slower |
| TreeSet | O(n log n) | O(n) | Auto-handles duplicates |
| Min Heap | O(n log n) | O(n) | Overkill for this problem |

**Why Single Pass is Optimal:**
- Visit each element once
- Constant extra space
- No sorting needed

---

## Related Problems

1. **Kth Smallest Element** - Generalization to k-th
2. **Second Largest Element** - Mirror problem
3. **Find Minimum** - Simpler version
4. **Top K Elements** - Multiple smallest/largest
5. **Median of Array** - Related selection problem
6. **QuickSelect** - O(n) average for k-th element

---

## Interview Tips

### Clarification Questions
1. Can array be empty? (Return -1)
2. Can array have duplicates? (Yes, skip them)
3. What if all elements are same? (Return -1)
4. What if array has < 2 elements? (Return -1)
5. Can array have negative numbers? (Yes)
6. What to return if no second smallest? (-1 or throw exception)

### Approach Explanation
1. "Track two variables: first and second smallest"
2. "Single pass through array"
3. "Update both when finding new minimum"
4. "Update only second when finding value between them"
5. "Skip duplicates of first smallest"

### Common Mistakes
- Not handling duplicates (num != first check)
- Wrong update order (must update second before first)
- Not checking array size
- Not handling all same elements case
- Integer overflow with Integer.MAX_VALUE

### Optimization Insights
```java
// Wrong: Updates in wrong order
if (num < first) {
    first = num;
    second = first; // ❌ Both become same!
}

// Correct: Update second first
if (num < first) {
    second = first; // ✓ Save old first
    first = num;
}
```

---

## Real-World Applications

1. **Sports Rankings** - Second place finisher
2. **Auction Systems** - Second highest bid
3. **Performance Metrics** - Second best performer
4. **Quality Control** - Second lowest defect rate
5. **Resource Allocation** - Backup option selection
6. **Recommendation Systems** - Alternative suggestions

---

## Key Takeaways

1. **Single Pass:** O(n) time, O(1) space is optimal
2. **Two Variables:** Track first and second smallest
3. **Update Order:** Update second before first when finding new minimum
4. **Skip Duplicates:** Check num != first
5. **Edge Cases:** Handle empty, single element, all same
6. **Initialization:** Use Integer.MAX_VALUE for comparison
7. **Return Value:** -1 or exception when no second smallest exists

---

## Additional Notes

### Handling Integer.MIN_VALUE

```java
// Problem: If array contains Integer.MIN_VALUE
int[] arr = {Integer.MIN_VALUE, 5, 3};

// Solution 1: Use Long
long first = Long.MAX_VALUE;
long second = Long.MAX_VALUE;

// Solution 2: Use array indices
int firstIdx = -1, secondIdx = -1;
```

### Generalization to K-th Smallest

```java
public static int kthSmallest(int[] arr, int k) {
    if (arr == null || arr.length < k) return -1;
    
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    
    for (int num : arr) {
        maxHeap.offer(num);
        if (maxHeap.size() > k) {
            maxHeap.poll();
        }
    }
    
    return maxHeap.peek();
}
```

### QuickSelect for K-th Element

```java
// O(n) average, O(n²) worst case
public static int quickSelect(int[] arr, int k) {
    return quickSelect(arr, 0, arr.length - 1, k - 1);
}

private static int quickSelect(int[] arr, int left, int right, int k) {
    if (left == right) return arr[left];
    
    int pivotIndex = partition(arr, left, right);
    
    if (k == pivotIndex) {
        return arr[k];
    } else if (k < pivotIndex) {
        return quickSelect(arr, left, pivotIndex - 1, k);
    } else {
        return quickSelect(arr, pivotIndex + 1, right, k);
    }
}
```

### Second Smallest vs Second Largest

```java
// Second Smallest
int first = Integer.MAX_VALUE;
int second = Integer.MAX_VALUE;

for (int num : arr) {
    if (num < first) {
        second = first;
        first = num;
    } else if (num < second && num != first) {
        second = num;
    }
}

// Second Largest (mirror logic)
int first = Integer.MIN_VALUE;
int second = Integer.MIN_VALUE;

for (int num : arr) {
    if (num > first) {
        second = first;
        first = num;
    } else if (num > second && num != first) {
        second = num;
    }
}
```

### Why Not Use Sorting?

```
Sorting: O(n log n)
  - Overkill for finding just 2nd smallest
  - Modifies array (unless copy)
  - More time complexity

Single Pass: O(n)
  - Optimal for this specific problem
  - No modification needed
  - Minimal space
```

### Handling Duplicates Explicitly

```java
// If duplicates should be considered distinct positions
public static int secondSmallestWithDuplicates(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int first = Integer.MAX_VALUE;
    int second = Integer.MAX_VALUE;
    
    for (int num : arr) {
        if (num < first) {
            second = first;
            first = num;
        } else if (num < second) { // Allow num == first
            second = num;
        }
    }
    
    return second == Integer.MAX_VALUE ? -1 : second;
}

// Example: [1, 1, 2] → returns 1 (duplicate)
```

### Stream Processing

```java
class SecondSmallestFinder {
    private int first = Integer.MAX_VALUE;
    private int second = Integer.MAX_VALUE;
    
    public void add(int num) {
        if (num < first) {
            second = first;
            first = num;
        } else if (num < second && num != first) {
            second = num;
        }
    }
    
    public int getSecondSmallest() {
        return second == Integer.MAX_VALUE ? -1 : second;
    }
}
```
