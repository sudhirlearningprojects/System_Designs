# Find Minimum Positive Integer in Array - Complete Guide

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Solution Approaches](#solution-approaches)
3. [Related Problems](#related-problems)
4. [Edge Cases](#edge-cases)

---

## Problem Statement

Given an array of integers (positive, negative, and zero), find the minimum positive integer.

**Examples**:
```
Input: [-5, 3, -2, 8, 1, 9]
Output: 1

Input: [-1, -2, -3]
Output: -1 (no positive integer)

Input: [5, 2, 8, 1, 3]
Output: 1
```

---

## Solution Approaches

### Approach 1: Linear Scan (Simple)

**Time**: O(n) | **Space**: O(1)

```java
public int findMinPositive(int[] arr) {
    int min = Integer.MAX_VALUE;
    boolean found = false;
    
    for (int num : arr) {
        if (num > 0 && num < min) {
            min = num;
            found = true;
        }
    }
    
    return found ? min : -1;
}
```

**Explanation**:
- Iterate through array once
- Track minimum positive value
- Return -1 if no positive found

---

### Approach 2: Stream API

**Time**: O(n) | **Space**: O(1)

```java
public int findMinPositiveStream(int[] arr) {
    return Arrays.stream(arr)
                 .filter(n -> n > 0)
                 .min()
                 .orElse(-1);
}
```

**Explanation**:
- Filter positive integers
- Find minimum using Stream API
- Return -1 if empty

---

### Approach 3: Priority Queue

**Time**: O(n log n) | **Space**: O(n)

```java
public int findMinPositivePQ(int[] arr) {
    PriorityQueue<Integer> pq = new PriorityQueue<>();
    
    for (int num : arr) {
        if (num > 0) {
            pq.offer(num);
        }
    }
    
    return pq.isEmpty() ? -1 : pq.peek();
}
```

**Explanation**:
- Add positive integers to min-heap
- Peek at top for minimum
- Less efficient but useful for streaming data

---

## Related Problems

### Problem 1: First Missing Positive (LeetCode 41)

Find the smallest missing positive integer (1, 2, 3, ...).

**Time**: O(n) | **Space**: O(1)

```java
public int firstMissingPositive(int[] arr) {
    int n = arr.length;
    
    // Step 1: Place each positive integer at correct index (value-1)
    for (int i = 0; i < n; i++) {
        while (arr[i] > 0 && arr[i] <= n && arr[i] != arr[arr[i] - 1]) {
            // Swap arr[i] to its correct position
            int temp = arr[arr[i] - 1];
            arr[arr[i] - 1] = arr[i];
            arr[i] = temp;
        }
    }
    
    // Step 2: Find first missing positive
    for (int i = 0; i < n; i++) {
        if (arr[i] != i + 1) {
            return i + 1;
        }
    }
    
    return n + 1;
}
```

**Example**:
```
Input: [3, 4, -1, 1]
After placement: [1, -1, 3, 4]
Output: 2 (missing)

Input: [1, 2, 0]
After placement: [1, 2, 0]
Output: 3 (missing)
```

**Step-by-Step**:
```
Array: [3, 4, -1, 1]

Step 1: Place at correct positions
i=0: arr[0]=3 → place at index 2
     [3, 4, -1, 1] → [-1, 4, 3, 1]
     arr[0]=-1 → skip

i=1: arr[1]=4 → place at index 3
     [-1, 4, 3, 1] → [-1, 1, 3, 4]
     arr[1]=1 → place at index 0
     [-1, 1, 3, 4] → [1, -1, 3, 4]

i=2: arr[2]=3 → already at correct position
i=3: arr[3]=4 → already at correct position

Step 2: Find first missing
i=0: arr[0]=1 ✓
i=1: arr[1]=-1 ≠ 2 → return 2
```

---

### Problem 2: Kth Smallest Positive

Find the kth smallest positive integer.

**Time**: O(n log k) | **Space**: O(k)

```java
public int kthSmallestPositive(int[] arr, int k) {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
    
    for (int num : arr) {
        if (num > 0) {
            maxHeap.offer(num);
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }
    }
    
    return maxHeap.isEmpty() ? -1 : maxHeap.peek();
}
```

**Example**:
```
Input: [7, 10, 4, 3, 20, 15], k=3
Positive: [7, 10, 4, 3, 20, 15]
Sorted: [3, 4, 7, 10, 15, 20]
Output: 7 (3rd smallest)
```

---

### Problem 3: Count Positive Integers

Count how many positive integers exist.

**Time**: O(n) | **Space**: O(1)

```java
public int countPositive(int[] arr) {
    int count = 0;
    for (int num : arr) {
        if (num > 0) count++;
    }
    return count;
}

// Stream API
public int countPositiveStream(int[] arr) {
    return (int) Arrays.stream(arr).filter(n -> n > 0).count();
}
```

---

### Problem 4: Sum of Positive Integers

Calculate sum of all positive integers.

**Time**: O(n) | **Space**: O(1)

```java
public int sumPositive(int[] arr) {
    int sum = 0;
    for (int num : arr) {
        if (num > 0) sum += num;
    }
    return sum;
}

// Stream API
public int sumPositiveStream(int[] arr) {
    return Arrays.stream(arr).filter(n -> n > 0).sum();
}
```

---

### Problem 5: Find All Missing Positives in Range

Find all missing positive integers in range [1, n].

**Time**: O(n) | **Space**: O(1)

```java
public List<Integer> findMissingPositives(int[] arr) {
    int n = arr.length;
    List<Integer> result = new ArrayList<>();
    
    // Mark presence using negative values
    for (int i = 0; i < n; i++) {
        int val = Math.abs(arr[i]);
        if (val > 0 && val <= n) {
            arr[val - 1] = -Math.abs(arr[val - 1]);
        }
    }
    
    // Collect missing numbers
    for (int i = 0; i < n; i++) {
        if (arr[i] > 0) {
            result.add(i + 1);
        }
    }
    
    return result;
}
```

**Example**:
```
Input: [4, 3, 2, 7, 8, 2, 3, 1]
Output: [5, 6]

Explanation:
Range [1, 8], missing: 5 and 6
```

---

## Edge Cases

### Test Cases

```java
public class MinPositiveTests {
    
    public static void main(String[] args) {
        testAllCases();
    }
    
    static void testAllCases() {
        // Test 1: Normal case
        assert findMinPositive(new int[]{-5, 3, -2, 8, 1, 9}) == 1;
        
        // Test 2: No positive integers
        assert findMinPositive(new int[]{-1, -2, -3}) == -1;
        
        // Test 3: All positive
        assert findMinPositive(new int[]{5, 2, 8, 1, 3}) == 1;
        
        // Test 4: Single element positive
        assert findMinPositive(new int[]{5}) == 5;
        
        // Test 5: Single element negative
        assert findMinPositive(new int[]{-5}) == -1;
        
        // Test 6: With zeros
        assert findMinPositive(new int[]{0, -1, 2, 0, 3}) == 2;
        
        // Test 7: Large numbers
        assert findMinPositive(new int[]{1000, 500, 2000, 1}) == 1;
        
        // Test 8: Duplicates
        assert findMinPositive(new int[]{3, 3, 1, 1, 2}) == 1;
        
        // Test 9: Empty array
        assert findMinPositive(new int[]{}) == -1;
        
        // Test 10: Integer.MAX_VALUE
        assert findMinPositive(new int[]{Integer.MAX_VALUE, 1}) == 1;
        
        System.out.println("All tests passed!");
    }
    
    static int findMinPositive(int[] arr) {
        int min = Integer.MAX_VALUE;
        boolean found = false;
        
        for (int num : arr) {
            if (num > 0 && num < min) {
                min = num;
                found = true;
            }
        }
        
        return found ? min : -1;
    }
}
```

---

## Performance Comparison

| Approach | Time | Space | Best For |
|----------|------|-------|----------|
| Linear Scan | O(n) | O(1) | General use |
| Stream API | O(n) | O(1) | Readable code |
| Priority Queue | O(n log n) | O(n) | Streaming data |
| First Missing | O(n) | O(1) | Missing number |

---

## Complete Implementation

```java
public class MinimumPositiveFinder {
    
    // Approach 1: Linear Scan
    public int findMinPositive(int[] arr) {
        int min = Integer.MAX_VALUE;
        boolean found = false;
        
        for (int num : arr) {
            if (num > 0 && num < min) {
                min = num;
                found = true;
            }
        }
        
        return found ? min : -1;
    }
    
    // Approach 2: Stream API
    public int findMinPositiveStream(int[] arr) {
        return Arrays.stream(arr)
                     .filter(n -> n > 0)
                     .min()
                     .orElse(-1);
    }
    
    // Approach 3: First Missing Positive
    public int firstMissingPositive(int[] arr) {
        int n = arr.length;
        
        for (int i = 0; i < n; i++) {
            while (arr[i] > 0 && arr[i] <= n && arr[i] != arr[arr[i] - 1]) {
                int temp = arr[arr[i] - 1];
                arr[arr[i] - 1] = arr[i];
                arr[i] = temp;
            }
        }
        
        for (int i = 0; i < n; i++) {
            if (arr[i] != i + 1) {
                return i + 1;
            }
        }
        
        return n + 1;
    }
    
    // Approach 4: Kth Smallest Positive
    public int kthSmallestPositive(int[] arr, int k) {
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
        
        for (int num : arr) {
            if (num > 0) {
                maxHeap.offer(num);
                if (maxHeap.size() > k) {
                    maxHeap.poll();
                }
            }
        }
        
        return maxHeap.isEmpty() ? -1 : maxHeap.peek();
    }
    
    // Test
    public static void main(String[] args) {
        MinimumPositiveFinder finder = new MinimumPositiveFinder();
        
        int[] arr1 = {-5, 3, -2, 8, 1, 9};
        int[] arr2 = {-1, -2, -3};
        int[] arr3 = {5, 2, 8, 1, 3};
        
        System.out.println("=== Find Minimum Positive ===");
        System.out.println(finder.findMinPositive(arr1));        // 1
        System.out.println(finder.findMinPositive(arr2));        // -1
        System.out.println(finder.findMinPositive(arr3));        // 1
        
        System.out.println("\n=== Stream API ===");
        System.out.println(finder.findMinPositiveStream(arr1));  // 1
        
        System.out.println("\n=== First Missing Positive ===");
        System.out.println(finder.firstMissingPositive(new int[]{3, 4, -1, 1}));  // 2
        System.out.println(finder.firstMissingPositive(new int[]{1, 2, 0}));      // 3
        
        System.out.println("\n=== Kth Smallest Positive ===");
        System.out.println(finder.kthSmallestPositive(new int[]{7, 10, 4, 3, 20, 15}, 3));  // 7
    }
}
```

---

## Summary

### Quick Reference

**Find Minimum Positive**:
```java
int min = Integer.MAX_VALUE;
for (int num : arr) {
    if (num > 0 && num < min) min = num;
}
return min == Integer.MAX_VALUE ? -1 : min;
```

**First Missing Positive**:
```java
// Place at correct index, then find first mismatch
for (int i = 0; i < n; i++) {
    while (arr[i] > 0 && arr[i] <= n && arr[i] != arr[arr[i] - 1]) {
        swap(arr, i, arr[i] - 1);
    }
}
```

### Key Takeaways

1. **Linear scan** is most efficient: O(n) time, O(1) space
2. **Stream API** is most readable
3. **First missing positive** uses clever in-place marking
4. Always handle edge cases: empty array, no positives, duplicates
5. Consider using **PriorityQueue** for kth smallest problems
