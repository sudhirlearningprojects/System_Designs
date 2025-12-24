# Subarray Exceeds Sum

## Problem Statement

Given an array of positive integers and a target sum, find the length of the smallest contiguous subarray whose sum exceeds the target. If no such subarray exists, return -1.

**Input:** 
- Array of positive integers
- Target sum (integer)

**Output:** 
- Length of smallest subarray with sum > target
- -1 if no such subarray exists

**Examples:**
```
arr = [1, 2, 3, 4], target = 6
  Subarray [3, 4] has sum 7 > 6, length = 2
  Result: 2

arr = [1, 2, 3, 4], target = 12
  Maximum possible sum = 10 (entire array)
  No subarray exceeds 12
  Result: -1

arr = [1, 2, 3, 4], target = 3
  Subarray [4] has sum 4 > 3, length = 1
  Result: 1
```

---

## Solution Approaches

### Approach 1: Sliding Window (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int subArrayExceedsSum(int arr[], int target) {
    int n = arr.length;
    int minLen = Integer.MAX_VALUE;
    int sum = 0;
    int left = 0;
    
    for (int right = 0; right < n; right++) {
        sum += arr[right];
        
        while (sum > target) {
            minLen = Math.min(minLen, right - left + 1);
            sum -= arr[left];
            left++;
        }
    }
    
    return minLen == Integer.MAX_VALUE ? -1 : minLen;
}
```

---

### Approach 2: Brute Force

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int subArrayExceedsSumBrute(int arr[], int target) {
    int n = arr.length;
    int minLen = Integer.MAX_VALUE;
    
    for (int i = 0; i < n; i++) {
        int sum = 0;
        for (int j = i; j < n; j++) {
            sum += arr[j];
            if (sum > target) {
                minLen = Math.min(minLen, j - i + 1);
                break;
            }
        }
    }
    
    return minLen == Integer.MAX_VALUE ? -1 : minLen;
}
```

---

### Approach 3: Binary Search + Prefix Sum

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int subArrayExceedsSumBinarySearch(int arr[], int target) {
    int n = arr.length;
    int[] prefix = new int[n + 1];
    
    for (int i = 0; i < n; i++) {
        prefix[i + 1] = prefix[i] + arr[i];
    }
    
    int minLen = Integer.MAX_VALUE;
    
    for (int i = 0; i < n; i++) {
        int needed = target + prefix[i] + 1;
        int pos = binarySearch(prefix, needed);
        
        if (pos <= n) {
            minLen = Math.min(minLen, pos - i);
        }
    }
    
    return minLen == Integer.MAX_VALUE ? -1 : minLen;
}

private static int binarySearch(int[] prefix, int target) {
    int left = 0, right = prefix.length;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (prefix[mid] < target) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    return left;
}
```

---

## Algorithm Walkthrough

### Example: arr = [1, 2, 3, 4], target = 6

**Sliding Window Approach:**

```
Initial: left=0, right=0, sum=0, minLen=∞

Step 1: right=0
  sum = 0 + 1 = 1
  1 > 6? No
  Window: [1]

Step 2: right=1
  sum = 1 + 2 = 3
  3 > 6? No
  Window: [1, 2]

Step 3: right=2
  sum = 3 + 3 = 6
  6 > 6? No
  Window: [1, 2, 3]

Step 4: right=3
  sum = 6 + 4 = 10
  10 > 6? Yes ✓
    minLen = min(∞, 3-0+1) = 4
    sum = 10 - 1 = 9, left=1
    
    9 > 6? Yes ✓
      minLen = min(4, 3-1+1) = 3
      sum = 9 - 2 = 7, left=2
      
      7 > 6? Yes ✓
        minLen = min(3, 3-2+1) = 2
        sum = 7 - 3 = 4, left=3
        
        4 > 6? No
  Window: [4]

Result: minLen = 2
Subarray: [3, 4] with sum 7
```

### Example: arr = [1, 2, 3, 4], target = 12

```
Process entire array:
  Maximum sum = 1+2+3+4 = 10
  10 > 12? No
  
No window ever exceeds target

Result: -1
```

---

## Complete Implementation

```java
import java.io.*;
import java.util.*;

public class Solution {
    
    // Approach 1: Sliding Window (Optimal)
    public static int subArrayExceedsSum(int arr[], int target) {
        int n = arr.length;
        int minLen = Integer.MAX_VALUE;
        int sum = 0;
        int left = 0;
        
        for (int right = 0; right < n; right++) {
            sum += arr[right];
            
            while (sum > target) {
                minLen = Math.min(minLen, right - left + 1);
                sum -= arr[left];
                left++;
            }
        }
        
        return minLen == Integer.MAX_VALUE ? -1 : minLen;
    }
    
    // Approach 2: Brute Force
    public static int subArrayExceedsSumBrute(int arr[], int target) {
        int n = arr.length;
        int minLen = Integer.MAX_VALUE;
        
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = i; j < n; j++) {
                sum += arr[j];
                if (sum > target) {
                    minLen = Math.min(minLen, j - i + 1);
                    break;
                }
            }
        }
        
        return minLen == Integer.MAX_VALUE ? -1 : minLen;
    }
    
    // Approach 3: Binary Search + Prefix Sum
    public static int subArrayExceedsSumBinarySearch(int arr[], int target) {
        int n = arr.length;
        int[] prefix = new int[n + 1];
        
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + arr[i];
        }
        
        int minLen = Integer.MAX_VALUE;
        
        for (int i = 0; i < n; i++) {
            int needed = target + prefix[i] + 1;
            int pos = binarySearch(prefix, needed);
            
            if (pos <= n) {
                minLen = Math.min(minLen, pos - i);
            }
        }
        
        return minLen == Integer.MAX_VALUE ? -1 : minLen;
    }
    
    private static int binarySearch(int[] prefix, int target) {
        int left = 0, right = prefix.length;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (prefix[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        
        return left;
    }
    
    public static void main(String[] args) {
        boolean result = true;
        int[] arr = {1, 2, 3, 4};
        
        result = result && subArrayExceedsSum(arr, 6) == 2;
        result = result && subArrayExceedsSum(arr, 12) == -1;
        result = result && subArrayExceedsSum(arr, 3) == 1;
        result = result && subArrayExceedsSum(arr, 0) == 1;
        
        if (result) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("There are test failures\n");
        }
        
        // Demo
        System.out.println("arr=[1,2,3,4], target=6 => " + subArrayExceedsSum(arr, 6));
        System.out.println("arr=[1,2,3,4], target=12 => " + subArrayExceedsSum(arr, 12));
    }
}
```

---

## Test Cases

```java
@Test
public void testSubArrayExceedsSum() {
    // Test case 1: Given example
    assertEquals(2, subArrayExceedsSum(new int[]{1, 2, 3, 4}, 6));
    
    // Test case 2: No solution
    assertEquals(-1, subArrayExceedsSum(new int[]{1, 2, 3, 4}, 12));
    
    // Test case 3: Single element
    assertEquals(1, subArrayExceedsSum(new int[]{1, 2, 3, 4}, 3));
    
    // Test case 4: Target is 0
    assertEquals(1, subArrayExceedsSum(new int[]{1, 2, 3, 4}, 0));
    
    // Test case 5: Large subarray needed
    assertEquals(4, subArrayExceedsSum(new int[]{1, 1, 1, 1}, 3));
    
    // Test case 6: All elements same
    assertEquals(3, subArrayExceedsSum(new int[]{2, 2, 2, 2}, 5));
    
    // Test case 7: Single element array
    assertEquals(1, subArrayExceedsSum(new int[]{5}, 3));
    assertEquals(-1, subArrayExceedsSum(new int[]{5}, 10));
    
    // Test case 8: Two elements
    assertEquals(1, subArrayExceedsSum(new int[]{5, 3}, 4));
    assertEquals(2, subArrayExceedsSum(new int[]{3, 4}, 6));
    
    // Test case 9: Large numbers
    assertEquals(2, subArrayExceedsSum(new int[]{100, 200, 300}, 250));
}
```

---

## Visual Representation

### Sliding Window Movement

```
arr = [1, 2, 3, 4], target = 6

Step 1: [1] sum=1
        ↑
        L,R

Step 2: [1, 2] sum=3
        ↑   ↑
        L   R

Step 3: [1, 2, 3] sum=6
        ↑       ↑
        L       R

Step 4: [1, 2, 3, 4] sum=10 > 6 ✓
        ↑           ↑
        L           R
        
        Shrink: [2, 3, 4] sum=9 > 6 ✓
                 ↑       ↑
                 L       R
        
        Shrink: [3, 4] sum=7 > 6 ✓ (length=2)
                    ↑   ↑
                    L   R
        
        Shrink: [4] sum=4 ≤ 6
                       ↑
                       L,R

Result: Minimum length = 2
```

---

## Edge Cases

1. **Empty array:** `[]` → -1
2. **Single element exceeds:** `[5], target=3` → 1
3. **Single element doesn't exceed:** `[5], target=10` → -1
4. **Target is 0:** Any element exceeds → 1
5. **All elements needed:** `[1,1,1,1], target=3` → 4
6. **No solution possible:** Sum of all < target → -1
7. **Negative target:** All elements exceed → 1
8. **Large array:** Performance test

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Brute Force | O(n²) | O(1) | Check all subarrays |
| Sliding Window | O(n) | O(1) | **Optimal** |
| Binary Search | O(n log n) | O(n) | Prefix sum + search |

**Why Sliding Window is O(n)?**
- Each element added once (right pointer)
- Each element removed at most once (left pointer)
- Total operations: 2n = O(n)

**Key Insight:**
- Array has positive integers only
- Sum increases as we add elements
- Sum decreases as we remove elements
- Perfect for sliding window

---

## Related Problems

1. **Minimum Size Subarray Sum** - Sum ≥ target (not >)
2. **Maximum Size Subarray Sum Equals k** - Exact sum
3. **Subarray Sum Equals K** - Count subarrays
4. **Longest Substring Without Repeating Characters** - Similar window
5. **Minimum Window Substring** - Pattern matching
6. **Sliding Window Maximum** - Window technique

---

## Interview Tips

### Clarification Questions
1. Are all array elements positive? (Yes - enables sliding window)
2. Can array be empty? (Return -1)
3. What if target is negative? (All elements exceed)
4. Do we want sum > target or sum ≥ target? (Strictly greater)
5. Can we modify the input array? (Not needed)

### Approach Explanation
1. "Use sliding window since all elements are positive"
2. "Expand window by moving right pointer"
3. "When sum exceeds target, try to shrink from left"
4. "Track minimum length during shrinking"
5. "O(n) time, O(1) space - optimal"

### Common Mistakes
- Using sum ≥ target instead of sum > target
- Not handling case when no solution exists
- Forgetting to update minLen before shrinking
- Off-by-one errors in length calculation
- Not considering single element subarrays

### Why Sliding Window Works
- Positive integers → monotonic sum growth
- If [i, j] exceeds target, [i, j+1] also exceeds
- Can safely shrink from left without missing solutions

---

## Real-World Applications

1. **Network Packets** - Find burst of traffic exceeding threshold
2. **Financial Analysis** - Detect spending spikes
3. **Resource Monitoring** - CPU/memory usage bursts
4. **Quality Control** - Defect rate exceeding limit
5. **Time Series Analysis** - Anomaly detection

---

## Key Takeaways

1. **Sliding Window Pattern:** Optimal for positive integer arrays
2. **Two Pointers:** Expand right, shrink left when condition met
3. **Time Complexity:** O(n) - each element visited at most twice
4. **Space Complexity:** O(1) - only tracking variables
5. **Key Condition:** All positive integers enables monotonic sum
6. **Edge Case:** Return -1 when no solution exists
7. **Similar Pattern:** Minimum window problems with constraints

---

## Additional Notes

### Why Not Binary Search on Answer?

Could binary search on length (1 to n):
```java
// Check if any subarray of length k exceeds target
boolean canExceed(int[] arr, int k, int target) {
    int sum = 0;
    for (int i = 0; i < k; i++) sum += arr[i];
    if (sum > target) return true;
    
    for (int i = k; i < arr.length; i++) {
        sum += arr[i] - arr[i - k];
        if (sum > target) return true;
    }
    return false;
}
```
Time: O(n log n), but sliding window is simpler and O(n).

### Variation: Sum ≥ Target

Change condition from `sum > target` to `sum >= target`:
```java
while (sum >= target) {
    minLen = Math.min(minLen, right - left + 1);
    sum -= arr[left];
    left++;
}
```

### Variation: Maximum Length

Find longest subarray with sum ≤ target:
```java
public static int maxSubArrayNotExceedSum(int arr[], int target) {
    int maxLen = 0;
    int sum = 0;
    int left = 0;
    
    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];
        
        while (sum > target) {
            sum -= arr[left];
            left++;
        }
        
        maxLen = Math.max(maxLen, right - left + 1);
    }
    
    return maxLen;
}
```

### When Sliding Window Doesn't Work

If array has negative numbers:
- Sum can decrease when adding elements
- Sum can increase when removing elements
- Need different approach (prefix sum + hash map)

Example: `[-1, 2, 3], target=1`
- Sliding window might miss `[2]` (sum=2 > 1)
