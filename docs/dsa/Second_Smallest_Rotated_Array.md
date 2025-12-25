# Second Smallest Element in Sorted Rotated Array

## Problem Statement

Find the second smallest element in a sorted rotated array. A sorted rotated array is a sorted array that has been rotated at some pivot point.

**Input:** Sorted rotated array of distinct integers  
**Output:** Second smallest element

**Examples:**
```
Input: [5, 6, 1, 2, 3, 4]
Output: 2
Explanation: Sorted: [1, 2, 3, 4, 5, 6], Second smallest = 2

Input: [3, 4, 5, 1, 2]
Output: 2
Explanation: Sorted: [1, 2, 3, 4, 5], Second smallest = 2

Input: [2, 3, 4, 5, 6, 7, 8, 1]
Output: 2
Explanation: Minimum = 1, Second smallest = 2

Input: [1, 2, 3, 4, 5] (no rotation)
Output: 2
Explanation: Already sorted, Second smallest = 2
```

---

## Solution Approaches

### Approach 1: Find Minimum Index (Optimal)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static int secondSmallest(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int minIdx = findMinIndex(arr);
    
    // Second smallest is next element (circular)
    return arr[(minIdx + 1) % arr.length];
}

private static int findMinIndex(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    return left;
}
```

---

### Approach 2: Direct Binary Search

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static int secondSmallestDirect(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int n = arr.length;
    int left = 0, right = n - 1;
    
    // Find rotation point (minimum element)
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    // left is index of minimum
    // Second smallest is at (left + 1) % n
    return arr[(left + 1) % n];
}
```

---

### Approach 3: Linear Scan

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int secondSmallestLinear(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int first = Integer.MAX_VALUE;
    int second = Integer.MAX_VALUE;
    
    for (int num : arr) {
        if (num < first) {
            second = first;
            first = num;
        } else if (num < second) {
            second = num;
        }
    }
    
    return second;
}
```

---

### Approach 4: Find Min + Next Element

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static int secondSmallestMinNext(int[] arr) {
    if (arr == null || arr.length < 2) return -1;
    
    int min = findMin(arr);
    
    // Find index of min
    int minIdx = -1;
    for (int i = 0; i < arr.length; i++) {
        if (arr[i] == min) {
            minIdx = i;
            break;
        }
    }
    
    // Second smallest is next element
    return arr[(minIdx + 1) % arr.length];
}

private static int findMin(int[] arr) {
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

## Algorithm Walkthrough

### Example: [5, 6, 1, 2, 3, 4]

**Binary Search for Minimum:**

```
Array: [5, 6, 1, 2, 3, 4]
Indices: 0  1  2  3  4  5

Initial: left=0, right=5

Step 1: mid = (0+5)/2 = 2
  arr[2]=1, arr[5]=4
  1 < 4 → right half is sorted
  right = 2

Step 2: left=0, right=2
  mid = (0+2)/2 = 1
  arr[1]=6, arr[2]=1
  6 > 1 → rotation in right half
  left = 2

Step 3: left=2, right=2
  Loop ends

Minimum index: 2
Minimum value: arr[2] = 1
Second smallest: arr[(2+1)%6] = arr[3] = 2
```

### Example: [3, 4, 5, 1, 2]

```
Array: [3, 4, 5, 1, 2]
Indices: 0  1  2  3  4

Initial: left=0, right=4

Step 1: mid = 2
  arr[2]=5, arr[4]=2
  5 > 2 → rotation in right half
  left = 3

Step 2: left=3, right=4
  mid = 3
  arr[3]=1, arr[4]=2
  1 < 2 → right half sorted
  right = 3

Step 3: left=3, right=3
  Loop ends

Minimum index: 3
Minimum value: arr[3] = 1
Second smallest: arr[(3+1)%5] = arr[4] = 2
```

### Example: [1, 2, 3, 4, 5] (No Rotation)

```
Array: [1, 2, 3, 4, 5]

Binary search finds minimum at index 0
Second smallest: arr[(0+1)%5] = arr[1] = 2
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Find minimum index (Optimal)
    public static int secondSmallest(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        int minIdx = findMinIndex(arr);
        return arr[(minIdx + 1) % arr.length];
    }
    
    private static int findMinIndex(int[] arr) {
        int left = 0, right = arr.length - 1;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] > arr[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        
        return left;
    }
    
    // Approach 2: Linear scan
    public static int secondSmallestLinear(int[] arr) {
        if (arr == null || arr.length < 2) return -1;
        
        int first = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        
        for (int num : arr) {
            if (num < first) {
                second = first;
                first = num;
            } else if (num < second) {
                second = num;
            }
        }
        
        return second;
    }
    
    // Helper: Find minimum value
    public static int findMin(int[] arr) {
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
    
    public static boolean doTestsPass() {
        // Test 1: Rotated array
        int[] test1 = {5, 6, 1, 2, 3, 4};
        if (secondSmallest(test1) != 2) return false;
        
        // Test 2: Another rotation
        int[] test2 = {3, 4, 5, 1, 2};
        if (secondSmallest(test2) != 2) return false;
        
        // Test 3: No rotation
        int[] test3 = {1, 2, 3, 4, 5};
        if (secondSmallest(test3) != 2) return false;
        
        // Test 4: Rotated at end
        int[] test4 = {2, 3, 4, 5, 6, 7, 8, 1};
        if (secondSmallest(test4) != 2) return false;
        
        // Test 5: Two elements
        int[] test5 = {2, 1};
        if (secondSmallest(test5) != 2) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {5, 6, 1, 2, 3, 4};
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Second smallest: " + secondSmallest(arr));
        System.out.println("Minimum: " + findMin(arr));
    }
}
```

---

## Test Cases

```java
@Test
public void testSecondSmallest() {
    // Test 1: Standard rotation
    assertEquals(2, secondSmallest(new int[]{5, 6, 1, 2, 3, 4}));
    
    // Test 2: Different rotation point
    assertEquals(2, secondSmallest(new int[]{3, 4, 5, 1, 2}));
    
    // Test 3: No rotation (already sorted)
    assertEquals(2, secondSmallest(new int[]{1, 2, 3, 4, 5}));
    
    // Test 4: Rotated at last position
    assertEquals(2, secondSmallest(new int[]{2, 3, 4, 5, 6, 7, 8, 1}));
    
    // Test 5: Two elements
    assertEquals(2, secondSmallest(new int[]{2, 1}));
    assertEquals(5, secondSmallest(new int[]{5, 3}));
    
    // Test 6: Rotated by 1
    assertEquals(2, secondSmallest(new int[]{2, 3, 4, 5, 1}));
    
    // Test 7: Large rotation
    assertEquals(2, secondSmallest(new int[]{6, 7, 8, 9, 10, 1, 2, 3, 4, 5}));
    
    // Test 8: Single element
    assertEquals(-1, secondSmallest(new int[]{5}));
    
    // Test 9: Empty array
    assertEquals(-1, secondSmallest(new int[]{}));
}
```

---

## Visual Representation

### Rotation Visualization

```
Original sorted: [1, 2, 3, 4, 5, 6]

Rotated at index 2:
[3, 4, 5, 6, 1, 2]
          ↑
      Rotation point (minimum)

Rotated at index 4:
[5, 6, 1, 2, 3, 4]
      ↑
  Rotation point (minimum)

Key insight: 
  Minimum is at rotation point
  Second smallest is next element (circular)
```

### Binary Search Process

```
Array: [5, 6, 1, 2, 3, 4]

Step 1:
[5, 6, 1, 2, 3, 4]
 L     M        R
arr[M]=1 < arr[R]=4 → right half sorted
Search left: R = M

Step 2:
[5, 6, 1, 2, 3, 4]
 L  M  R
arr[M]=6 > arr[R]=1 → left half rotated
Search right: L = M+1

Step 3:
[5, 6, 1, 2, 3, 4]
       L/R
Found minimum at index 2

Second smallest: arr[(2+1)%6] = arr[3] = 2
```

---

## Edge Cases

1. **No rotation:** [1, 2, 3, 4, 5] → 2
2. **Rotated by 1:** [2, 3, 4, 5, 1] → 2
3. **Rotated at end:** [2, 3, 4, 5, 6, 1] → 2
4. **Two elements:** [2, 1] → 2
5. **Single element:** [5] → -1
6. **Empty array:** [] → -1
7. **All rotations:** Works for any rotation point

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Binary Search | O(log n) | O(1) | **Optimal** |
| Linear Scan | O(n) | O(1) | Simple but slower |
| Sort | O(n log n) | O(1) | Unnecessary |

**Why Binary Search is Optimal:**
- Exploits sorted rotated property
- Logarithmic time to find minimum
- Constant time to get second smallest

**Key Insight:**
In a sorted rotated array with distinct elements:
- Minimum is at rotation point
- Second smallest is immediately after minimum (circular)

---

## Related Problems

1. **Find Minimum in Rotated Sorted Array** - Base problem
2. **Search in Rotated Sorted Array** - Search for target
3. **Find Rotation Count** - Count rotations
4. **Kth Smallest in Rotated Array** - Generalization
5. **Find Peak Element** - Similar binary search
6. **Rotated Array with Duplicates** - Harder variant

---

## Interview Tips

### Clarification Questions
1. Are elements distinct? (Usually yes for this problem)
2. Can array be empty? (Handle edge case)
3. Is array guaranteed to be rotated? (Could be sorted)
4. What if array has < 2 elements? (Return -1)
5. Can we modify the array? (Not needed)

### Approach Explanation
1. "Sorted rotated array has special property"
2. "Use binary search to find minimum (rotation point)"
3. "Second smallest is next element after minimum"
4. "Handle circular array with modulo"
5. "O(log n) time, O(1) space"

### Common Mistakes
- Not handling circular array (forgetting modulo)
- Wrong binary search condition
- Not handling no-rotation case
- Assuming rotation point is always in middle
- Not checking array size

### Why This Works
```
Sorted rotated array: [5, 6, 1, 2, 3, 4]
                           ↑
                       Minimum (rotation point)

Property: All elements after minimum are in sorted order
Therefore: Second smallest = arr[minIdx + 1]

Special case: If minIdx is last index
  Second smallest = arr[0] (wrap around)
  Use modulo: arr[(minIdx + 1) % n]
```

---

## Real-World Applications

1. **Circular Buffers** - Finding elements in rotated buffers
2. **Time Series Data** - Rotated time windows
3. **Scheduling** - Circular scheduling systems
4. **Cache Systems** - Rotated cache entries
5. **Load Balancing** - Round-robin with rotation

---

## Key Takeaways

1. **Binary Search:** O(log n) to find minimum in rotated array
2. **Rotation Point:** Minimum element is at rotation point
3. **Second Smallest:** Next element after minimum (circular)
4. **Modulo Arithmetic:** Handle circular array with (idx + 1) % n
5. **No Rotation:** Algorithm works even if array not rotated
6. **Distinct Elements:** Assumption simplifies the problem
7. **Optimal Solution:** O(log n) time, O(1) space

---

## Additional Notes

### Finding Minimum in Rotated Array

```java
// Key insight: Compare mid with right
if (arr[mid] > arr[right]) {
    // Minimum is in right half
    left = mid + 1;
} else {
    // Minimum is in left half (including mid)
    right = mid;
}

Why compare with right, not left?
  [3, 4, 5, 1, 2]
       M     R
  arr[M]=5 > arr[R]=2 → minimum in right
  
  [5, 1, 2, 3, 4]
   L  M     R
  arr[M]=2 < arr[R]=4 → minimum in left (could be mid)
```

### Handling Duplicates

```java
// With duplicates, worst case becomes O(n)
private static int findMinWithDuplicates(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] > arr[right]) {
            left = mid + 1;
        } else if (arr[mid] < arr[right]) {
            right = mid;
        } else {
            // arr[mid] == arr[right], can't determine
            right--; // Linear scan in worst case
        }
    }
    
    return left;
}
```

### Why Not Just Sort?

```
Sorting: O(n log n)
  - Destroys rotation information
  - Slower than binary search
  - Modifies array

Binary Search: O(log n)
  - Exploits existing structure
  - Faster
  - No modification
```

### Generalization to K-th Smallest

```java
public static int kthSmallest(int[] arr, int k) {
    if (arr == null || arr.length < k) return -1;
    
    int minIdx = findMinIndex(arr);
    int n = arr.length;
    
    // k-th smallest is at (minIdx + k - 1) % n
    return arr[(minIdx + k - 1) % n];
}

// Example: [5, 6, 1, 2, 3, 4], k=3
// minIdx = 2
// 3rd smallest = arr[(2 + 3 - 1) % 6] = arr[4] = 3
```

### Alternative: Find Rotation Count

```java
// Rotation count = index of minimum
public static int findRotationCount(int[] arr) {
    return findMinIndex(arr);
}

// Example: [5, 6, 1, 2, 3, 4]
// Rotated 2 times (minimum at index 2)
```

### Comparison with Unsorted Array

```
Unsorted Array:
  - Must track two variables
  - O(n) time required
  - No structure to exploit

Sorted Rotated Array:
  - Can use binary search
  - O(log n) time
  - Exploit sorted property
  - Second smallest = next after minimum
```

### Edge Case: Minimum at Last Position

```
Array: [2, 3, 4, 5, 1]
Minimum at index 4

Second smallest: arr[(4 + 1) % 5] = arr[0] = 2 ✓

Without modulo: arr[5] → Index out of bounds ✗
```

### Visual: All Rotation Cases

```
No rotation:     [1, 2, 3, 4, 5] → min at 0, 2nd = arr[1] = 2
Rotate by 1:     [2, 3, 4, 5, 1] → min at 4, 2nd = arr[0] = 2
Rotate by 2:     [3, 4, 5, 1, 2] → min at 3, 2nd = arr[4] = 2
Rotate by 3:     [4, 5, 1, 2, 3] → min at 2, 2nd = arr[3] = 2
Rotate by 4:     [5, 1, 2, 3, 4] → min at 1, 2nd = arr[2] = 2

Pattern: Second smallest = arr[(minIdx + 1) % n]
```
