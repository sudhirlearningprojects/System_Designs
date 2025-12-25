# Minimum Size Subarray Sum

## Problem Statement

Find the minimal length of a contiguous subarray whose sum is greater than or equal to a given target value. Return 0 if no such subarray exists.

**Input:** 
- Array of positive integers
- Target sum

**Output:** Minimum length of subarray with sum ≥ target

**Examples:**
```
Input: arr = [2, 3, 1, 2, 4, 3], target = 7
Output: 2
Explanation: [4, 3] has sum 7, length 2 (minimum)

Input: arr = [1, 4, 4], target = 4
Output: 1
Explanation: [4] has sum 4, length 1

Input: arr = [1, 1, 1, 1, 1], target = 11
Output: 0
Explanation: No subarray has sum ≥ 11

Input: arr = [1, 2, 3, 4, 5], target = 15
Output: 5
Explanation: Entire array [1,2,3,4,5] has sum 15
```

---

## Solution Approaches

### Approach 1: Sliding Window (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int minSubArrayLen(int target, int[] arr) {
    int n = arr.length;
    int minLen = Integer.MAX_VALUE;
    int sum = 0;
    int left = 0;
    
    for (int right = 0; right < n; right++) {
        sum += arr[right];
        
        while (sum >= target) {
            minLen = Math.min(minLen, right - left + 1);
            sum -= arr[left];
            left++;
        }
    }
    
    return minLen == Integer.MAX_VALUE ? 0 : minLen;
}
```

---

### Approach 2: Binary Search + Prefix Sum

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int minSubArrayLenBinarySearch(int target, int[] arr) {
    int n = arr.length;
    int[] prefix = new int[n + 1];
    
    for (int i = 0; i < n; i++) {
        prefix[i + 1] = prefix[i] + arr[i];
    }
    
    int minLen = Integer.MAX_VALUE;
    
    for (int i = 0; i < n; i++) {
        int needed = target + prefix[i];
        int pos = binarySearch(prefix, needed);
        
        if (pos <= n) {
            minLen = Math.min(minLen, pos - i);
        }
    }
    
    return minLen == Integer.MAX_VALUE ? 0 : minLen;
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

### Approach 3: Brute Force

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int minSubArrayLenBrute(int target, int[] arr) {
    int n = arr.length;
    int minLen = Integer.MAX_VALUE;
    
    for (int i = 0; i < n; i++) {
        int sum = 0;
        for (int j = i; j < n; j++) {
            sum += arr[j];
            if (sum >= target) {
                minLen = Math.min(minLen, j - i + 1);
                break;
            }
        }
    }
    
    return minLen == Integer.MAX_VALUE ? 0 : minLen;
}
```

---

## Algorithm Walkthrough

### Example: arr = [2, 3, 1, 2, 4, 3], target = 7

**Sliding Window:**

```
Initial: left=0, right=0, sum=0, minLen=∞

Step 1: right=0, arr[0]=2
  sum = 0 + 2 = 2
  2 >= 7? No
  Window: [2]

Step 2: right=1, arr[1]=3
  sum = 2 + 3 = 5
  5 >= 7? No
  Window: [2, 3]

Step 3: right=2, arr[2]=1
  sum = 5 + 1 = 6
  6 >= 7? No
  Window: [2, 3, 1]

Step 4: right=3, arr[3]=2
  sum = 6 + 2 = 8
  8 >= 7? Yes ✓
    minLen = min(∞, 3-0+1) = 4
    sum = 8 - 2 = 6, left=1
    6 >= 7? No
  Window: [3, 1, 2]

Step 5: right=4, arr[4]=4
  sum = 6 + 4 = 10
  10 >= 7? Yes ✓
    minLen = min(4, 4-1+1) = 4
    sum = 10 - 3 = 7, left=2
    
    7 >= 7? Yes ✓
      minLen = min(4, 4-2+1) = 3
      sum = 7 - 1 = 6, left=3
      
      6 >= 7? No
  Window: [2, 4]

Step 6: right=5, arr[5]=3
  sum = 6 + 3 = 9
  9 >= 7? Yes ✓
    minLen = min(3, 5-3+1) = 3
    sum = 9 - 2 = 7, left=4
    
    7 >= 7? Yes ✓
      minLen = min(3, 5-4+1) = 2 ✓
      sum = 7 - 4 = 3, left=5
      
      3 >= 7? No
  Window: [3]

Result: minLen = 2
Subarray: [4, 3]
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Sliding Window (Optimal)
    public static int minSubArrayLen(int target, int[] arr) {
        int n = arr.length;
        int minLen = Integer.MAX_VALUE;
        int sum = 0;
        int left = 0;
        
        for (int right = 0; right < n; right++) {
            sum += arr[right];
            
            while (sum >= target) {
                minLen = Math.min(minLen, right - left + 1);
                sum -= arr[left];
                left++;
            }
        }
        
        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }
    
    // Return the actual subarray
    public static int[] minSubArray(int target, int[] arr) {
        int n = arr.length;
        int minLen = Integer.MAX_VALUE;
        int minStart = 0;
        int sum = 0;
        int left = 0;
        
        for (int right = 0; right < n; right++) {
            sum += arr[right];
            
            while (sum >= target) {
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minStart = left;
                }
                sum -= arr[left];
                left++;
            }
        }
        
        if (minLen == Integer.MAX_VALUE) {
            return new int[0];
        }
        
        return Arrays.copyOfRange(arr, minStart, minStart + minLen);
    }
    
    // Approach 2: Brute Force
    public static int minSubArrayLenBrute(int target, int[] arr) {
        int n = arr.length;
        int minLen = Integer.MAX_VALUE;
        
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = i; j < n; j++) {
                sum += arr[j];
                if (sum >= target) {
                    minLen = Math.min(minLen, j - i + 1);
                    break;
                }
            }
        }
        
        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }
    
    public static boolean doTestsPass() {
        // Test 1
        int[] test1 = {2, 3, 1, 2, 4, 3};
        if (minSubArrayLen(7, test1) != 2) return false;
        
        // Test 2
        int[] test2 = {1, 4, 4};
        if (minSubArrayLen(4, test2) != 1) return false;
        
        // Test 3
        int[] test3 = {1, 1, 1, 1, 1};
        if (minSubArrayLen(11, test3) != 0) return false;
        
        // Test 4
        int[] test4 = {1, 2, 3, 4, 5};
        if (minSubArrayLen(15, test4) != 5) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {2, 3, 1, 2, 4, 3};
        int target = 7;
        
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Target: " + target);
        System.out.println("Min length: " + minSubArrayLen(target, arr));
        System.out.println("Min subarray: " + Arrays.toString(minSubArray(target, arr)));
    }
}
```

---

## Test Cases

```java
@Test
public void testMinSubArrayLen() {
    // Test 1: Normal case
    assertEquals(2, minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));
    
    // Test 2: Single element sufficient
    assertEquals(1, minSubArrayLen(4, new int[]{1, 4, 4}));
    
    // Test 3: No solution
    assertEquals(0, minSubArrayLen(11, new int[]{1, 1, 1, 1, 1}));
    
    // Test 4: Entire array needed
    assertEquals(5, minSubArrayLen(15, new int[]{1, 2, 3, 4, 5}));
    
    // Test 5: Target is 0
    assertEquals(1, minSubArrayLen(0, new int[]{1, 2, 3}));
    
    // Test 6: Empty array
    assertEquals(0, minSubArrayLen(5, new int[]{}));
    
    // Test 7: All elements same
    assertEquals(3, minSubArrayLen(10, new int[]{4, 4, 4, 4}));
    
    // Test 8: First element sufficient
    assertEquals(1, minSubArrayLen(5, new int[]{10, 1, 2, 3}));
    
    // Test 9: Last element sufficient
    assertEquals(1, minSubArrayLen(5, new int[]{1, 2, 3, 10}));
}
```

---

## Visual Representation

### Sliding Window Movement

```
arr = [2, 3, 1, 2, 4, 3], target = 7

Window 1: [2, 3, 1, 2] sum=8 ≥ 7, len=4
Window 2: [3, 1, 2, 4] sum=10 ≥ 7, len=4
Window 3: [1, 2, 4] sum=7 ≥ 7, len=3
Window 4: [2, 4, 3] sum=9 ≥ 7, len=3
Window 5: [4, 3] sum=7 ≥ 7, len=2 ← Minimum

Result: 2
```

### Window Expansion and Contraction

```
Expand (right++):
[2] → [2,3] → [2,3,1] → [2,3,1,2]
sum: 2    5       6         8 ≥ 7 ✓

Contract (left++):
[2,3,1,2] → [3,1,2] → [1,2] → [2]
sum: 8        6        3      2

Continue expanding...
```

---

## Edge Cases

1. **Empty array:** [] → 0
2. **Single element sufficient:** [10], target=5 → 1
3. **Single element insufficient:** [1], target=5 → 0
4. **No solution:** [1,1,1], target=10 → 0
5. **Entire array needed:** [1,2,3], target=6 → 3
6. **Target is 0:** Any element works → 1
7. **All same elements:** [4,4,4,4], target=10 → 3
8. **First element works:** [10,1,2], target=5 → 1

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Sliding Window | O(n) | O(1) | **Optimal** |
| Binary Search | O(n log n) | O(n) | Prefix sum |
| Brute Force | O(n²) | O(1) | Not practical |

**Why Sliding Window is O(n)?**
- Each element added once (right pointer)
- Each element removed at most once (left pointer)
- Total operations: 2n = O(n)

---

## Related Problems

1. **Subarray Sum Equals K** - Exact sum
2. **Maximum Size Subarray Sum Equals k** - Maximum length
3. **Longest Substring with At Most K Distinct** - Similar window
4. **Minimum Window Substring** - Pattern matching
5. **Subarray Product Less Than K** - Product constraint
6. **Shortest Subarray with Sum at Least K** - With negatives

---

## Interview Tips

### Clarification Questions
1. Are all elements positive? (Yes - enables sliding window)
2. Can array be empty? (Return 0)
3. What if no solution? (Return 0)
4. Return length or subarray? (Usually length)
5. Can we modify array? (Not needed)

### Approach Explanation
1. "Use sliding window with two pointers"
2. "Expand window by moving right pointer"
3. "When sum ≥ target, try to shrink from left"
4. "Track minimum length during shrinking"
5. "O(n) time, O(1) space"

### Common Mistakes
- Not handling no solution case
- Wrong window length calculation
- Not shrinking window optimally
- Forgetting to update minimum
- Off-by-one errors

### Why Sliding Window Works

```
Key insight: All elements are positive
- Adding element increases sum
- Removing element decreases sum
- Monotonic property enables sliding window

If [i, j] has sum ≥ target:
  [i, j+1] also has sum ≥ target
  Can try shrinking from left
```

---

## Real-World Applications

1. **Resource Allocation** - Minimum resources needed
2. **Network Packets** - Minimum buffer size
3. **Financial Analysis** - Minimum investment period
4. **Inventory Management** - Minimum stock level
5. **Time Series** - Minimum time window
6. **Load Balancing** - Minimum server capacity

---

## Key Takeaways

1. **Sliding Window:** Optimal O(n) solution
2. **Two Pointers:** Expand right, shrink left
3. **Positive Elements:** Enable monotonic sum property
4. **Track Minimum:** Update during shrinking phase
5. **Time Complexity:** O(n) - each element visited twice
6. **Space Complexity:** O(1) - only tracking variables
7. **No Solution:** Return 0 when impossible

---

## Additional Notes

### Why Not Binary Search on Length?

```java
// Could binary search on answer (length)
// For each length k, check if any subarray of length k has sum ≥ target

boolean canAchieve(int[] arr, int k, int target) {
    int sum = 0;
    for (int i = 0; i < k; i++) sum += arr[i];
    if (sum >= target) return true;
    
    for (int i = k; i < arr.length; i++) {
        sum += arr[i] - arr[i - k];
        if (sum >= target) return true;
    }
    return false;
}

// Time: O(n log n)
// Sliding window is better: O(n)
```

### With Negative Numbers

```java
// If array has negative numbers, sliding window doesn't work
// Use prefix sum + monotonic deque or segment tree
// Time: O(n log n)

// Example: [1, -1, 5, -2, 3], target = 3
// Sliding window fails because removing element might increase sum
```

### Maximum Length Variant

```java
// Find maximum length subarray with sum ≤ target
public static int maxSubArrayLen(int target, int[] arr) {
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

### Count Subarrays

```java
// Count subarrays with sum ≥ target
public static int countSubarrays(int target, int[] arr) {
    int count = 0;
    int sum = 0;
    int left = 0;
    
    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];
        
        while (sum >= target && left <= right) {
            // All subarrays from [left, right] to [right, right] are valid
            count += arr.length - right;
            sum -= arr[left];
            left++;
        }
    }
    
    return count;
}
```

### Optimization: Early Exit

```java
// If first element ≥ target, answer is 1
if (arr.length > 0 && arr[0] >= target) {
    return 1;
}

// If sum of all elements < target, no solution
int totalSum = Arrays.stream(arr).sum();
if (totalSum < target) {
    return 0;
}
```

### Comparison: Minimum vs Maximum

```
Minimum length with sum ≥ target:
  - Shrink when sum ≥ target
  - Track minimum length
  
Maximum length with sum ≤ target:
  - Shrink when sum > target
  - Track maximum length
  
Same pattern, different condition!
```

### Why Integer.MAX_VALUE?

```java
int minLen = Integer.MAX_VALUE;

// Ensures any valid length is smaller
// Easy to check if solution exists:
return minLen == Integer.MAX_VALUE ? 0 : minLen;

// Alternative: Use -1 or n+1 as sentinel
```

### Prefix Sum Approach Explained

```java
// prefix[i] = sum of arr[0..i-1]
// Sum of arr[i..j] = prefix[j+1] - prefix[i]

// For each i, find smallest j where:
// prefix[j+1] - prefix[i] ≥ target
// prefix[j+1] ≥ target + prefix[i]

// Binary search on prefix array
// Time: O(n log n)
```
