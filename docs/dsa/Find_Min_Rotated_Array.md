# Find Minimum in Rotated Sorted Array

## Problem Statement

Find the smallest number in a rotated sorted array.

**Input:** Rotated sorted array  
**Output:** Minimum element

**Example:**
```
Input:  [3, 4, 5, 6, 1, 2]
Output: 1

Input:  [4, 5, 6, 7, 0, 1, 2]
Output: 0

Input:  [1, 2, 3, 4, 5]
Output: 1 (no rotation)
```

---

## Solution Approaches

### Approach 1: Linear Search (Baseline)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public int findMin(int[] arr) {
    int min = arr[0];
    for (int num : arr) {
        min = Math.min(min, num);
    }
    return min;
}
```

**Problem:** Doesn't leverage the sorted property

---

### Approach 2: Binary Search (Optimal)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public int findMin(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    return arr[left];
}
```

---

## Algorithm Explanation

### Key Insight

In a rotated sorted array:
- **Left half sorted:** `arr[mid] <= arr[right]` → minimum is in left half (including mid)
- **Right half sorted:** `arr[mid] > arr[right]` → minimum is in right half (excluding mid)

### Visual Representation

```
Array: [3, 4, 5, 6, 1, 2]
        L     M     R

arr[mid]=5 > arr[right]=2
→ Minimum is in right half
→ left = mid + 1

Array: [3, 4, 5, 6, 1, 2]
              L  M  R

arr[mid]=6 > arr[right]=2
→ Minimum is in right half
→ left = mid + 1

Array: [3, 4, 5, 6, 1, 2]
                 L
                 R

left == right → Found minimum = 1
```

---

## Detailed Walkthrough

**Input:** [3, 4, 5, 6, 1, 2]

```
Iteration 1:
  left=0, right=5, mid=2
  arr[2]=5, arr[5]=2
  5 > 2 → minimum in right half
  left = 3

Iteration 2:
  left=3, right=5, mid=4
  arr[4]=1, arr[5]=2
  1 <= 2 → minimum in left half (including mid)
  right = 4

Iteration 3:
  left=3, right=4, mid=3
  arr[3]=6, arr[4]=1
  6 > 1 → minimum in right half
  left = 4

Iteration 4:
  left=4, right=4
  left == right → Exit loop

Result: arr[4] = 1 ✓
```

---

## Complete Implementation

```java
public class FindMinRotatedArray {
    
    // Optimal: Binary Search
    public int findMin(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }
        
        int left = 0, right = arr.length - 1;
        
        while (left < right) {
            // Already sorted (no rotation or found minimum)
            if (arr[left] < arr[right]) {
                return arr[left];
            }
            
            int mid = left + (right - left) / 2;
            
            if (arr[mid] > arr[right]) {
                // Minimum is in right half
                left = mid + 1;
            } else {
                // Minimum is in left half (including mid)
                right = mid;
            }
        }
        
        return arr[left];
    }
    
    // Alternative: Find rotation point
    public int findMinWithRotationPoint(int[] arr) {
        int left = 0, right = arr.length - 1;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            // Check if mid+1 is the minimum
            if (mid < arr.length - 1 && arr[mid] > arr[mid + 1]) {
                return arr[mid + 1];
            }
            
            // Check if mid is the minimum
            if (mid > 0 && arr[mid] < arr[mid - 1]) {
                return arr[mid];
            }
            
            // Decide which half to search
            if (arr[mid] > arr[right]) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return arr[left];
    }
    
    // Baseline: Linear search
    public int findMinLinear(int[] arr) {
        int min = arr[0];
        for (int num : arr) {
            min = Math.min(min, num);
        }
        return min;
    }
}
```

---

## Test Cases

```java
@Test
public void testFindMin() {
    FindMinRotatedArray solver = new FindMinRotatedArray();
    
    // Basic rotation
    assertEquals(1, solver.findMin(new int[]{3, 4, 5, 6, 1, 2}));
    assertEquals(0, solver.findMin(new int[]{4, 5, 6, 7, 0, 1, 2}));
    
    // No rotation (already sorted)
    assertEquals(1, solver.findMin(new int[]{1, 2, 3, 4, 5}));
    
    // Rotated by 1
    assertEquals(1, solver.findMin(new int[]{2, 3, 4, 5, 1}));
    
    // Rotated to last position
    assertEquals(1, solver.findMin(new int[]{2, 1}));
    
    // Single element
    assertEquals(5, solver.findMin(new int[]{5}));
    
    // Two elements
    assertEquals(1, solver.findMin(new int[]{2, 1}));
    assertEquals(1, solver.findMin(new int[]{1, 2}));
    
    // All same elements (edge case)
    assertEquals(5, solver.findMin(new int[]{5, 5, 5, 5}));
    
    // Large rotation
    assertEquals(1, solver.findMin(new int[]{7, 8, 9, 1, 2, 3, 4, 5, 6}));
}
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[1, 2, 3]` | `1` | No rotation |
| `[3, 1, 2]` | `1` | Rotated by 1 |
| `[2, 1]` | `1` | Two elements |
| `[5]` | `5` | Single element |
| `[5, 5, 5]` | `5` | All duplicates |

---

## Why Binary Search Works

### Property of Rotated Sorted Array

```
Original: [1, 2, 3, 4, 5, 6, 7]
Rotated:  [4, 5, 6, 7, 1, 2, 3]
           ↑sorted↑  ↑sorted↑
```

**Key Observation:**
- At least one half is always sorted
- The unsorted half contains the rotation point (minimum)

**Decision Rule:**
```
if arr[mid] > arr[right]:
    # Right half is unsorted → minimum is there
    left = mid + 1
else:
    # Left half might contain minimum
    right = mid
```

---

## Common Mistakes

1. **Wrong Comparison:**
   ```java
   // WRONG - comparing with left
   if (arr[mid] > arr[left])
   
   // CORRECT - comparing with right
   if (arr[mid] > arr[right])
   ```

2. **Missing Mid in Search Space:**
   ```java
   // WRONG - might skip minimum
   right = mid - 1
   
   // CORRECT - mid could be minimum
   right = mid
   ```

3. **Integer Overflow:**
   ```java
   // WRONG - can overflow
   int mid = (left + right) / 2;
   
   // CORRECT
   int mid = left + (right - left) / 2;
   ```

4. **Wrong Loop Condition:**
   ```java
   // WRONG - infinite loop possible
   while (left <= right)
   
   // CORRECT
   while (left < right)
   ```

---

## Handling Duplicates (Follow-up)

**Problem:** Array with duplicates like `[2, 2, 2, 0, 1, 2]`

```java
public int findMinWithDuplicates(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else if (arr[mid] < arr[right]) {
            right = mid;
        } else {
            // arr[mid] == arr[right], can't determine which half
            right--; // Reduce search space by 1
        }
    }
    
    return arr[left];
}
```

**Time Complexity:** O(n) worst case when all elements are same

---

## Variations

### Find Rotation Count

```java
public int findRotationCount(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        if (arr[left] < arr[right]) {
            return left; // Already sorted
        }
        
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    return left; // Index of minimum = rotation count
}
```

### Find Maximum in Rotated Array

```java
public int findMax(int[] arr) {
    int minIndex = findRotationCount(arr);
    
    if (minIndex == 0) {
        return arr[arr.length - 1]; // No rotation
    }
    
    return arr[minIndex - 1]; // Element before minimum
}
```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Linear Search | O(n) | O(1) | Doesn't use sorted property |
| Binary Search | O(log n) | O(1) | Optimal solution |
| With Duplicates | O(n) worst | O(1) | Degrades to linear |

---

## Related Problems

- **LeetCode 153:** Find Minimum in Rotated Sorted Array
- **LeetCode 154:** Find Minimum in Rotated Sorted Array II (with duplicates)
- **LeetCode 33:** Search in Rotated Sorted Array
- **LeetCode 81:** Search in Rotated Sorted Array II

---

## Interview Tips

1. **Clarify Constraints:**
   - Are there duplicates?
   - Can array be empty?
   - Is it guaranteed to be rotated?

2. **Start with Observation:**
   - "The array has two sorted halves"
   - "We can use binary search"

3. **Explain Decision Logic:**
   - Compare mid with right endpoint
   - Decide which half contains minimum

4. **Walk Through Example:**
   - Use [3,4,5,6,1,2]
   - Show each iteration

5. **Discuss Edge Cases:**
   - No rotation
   - Single element
   - Two elements

---

## Visual Decision Tree

```
         arr[mid] vs arr[right]
                |
        ________|________
       |                 |
   mid > right      mid <= right
       |                 |
   Minimum in        Minimum in
   right half        left half
       |                 |
   left = mid+1      right = mid
```

---

## Real-World Applications

- **Circular Buffers:** Finding start position
- **Time Series Data:** Detecting anomalies in cyclic data
- **Load Balancing:** Finding optimal server in round-robin
- **Cache Management:** Circular queue operations

---

## Key Takeaways

✅ Binary search reduces O(n) to O(log n)  
✅ Compare mid with right endpoint, not left  
✅ Keep mid in search space when going left  
✅ Handle no-rotation case early for optimization  
✅ Duplicates degrade to O(n) worst case  
✅ Rotation count = index of minimum element
