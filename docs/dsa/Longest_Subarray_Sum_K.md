# Longest Subarray with Sum K

## Problem Statement

Given an array of integers, find the length of the longest contiguous subarray whose sum equals a given value k.

**Input:** 
- Array of integers (can have negatives)
- Target sum k

**Output:** Length of longest subarray with sum = k

**Examples:**
```
Input: arr = [10, 5, 2, 7, 1, 9], k = 15
Output: 4
Explanation: [5, 2, 7, 1] has sum 15, length 4

Input: arr = [1, 2, 3], k = 3
Output: 2
Explanation: [1, 2] has sum 3, length 2

Input: arr = [-1, 1, 0], k = 0
Output: 3
Explanation: [-1, 1, 0] has sum 0, length 3

Input: arr = [1, 2, 3, 4], k = 10
Output: 4
Explanation: [1, 2, 3, 4] has sum 10, length 4

Input: arr = [1, 2, 3], k = 10
Output: 0
Explanation: No subarray has sum 10
```

---

## Solution Approaches

### Approach 1: Prefix Sum + HashMap (Optimal for All Cases)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static int longestSubarraySum(int[] arr, int k) {
    Map<Integer, Integer> prefixMap = new HashMap<>();
    int sum = 0;
    int maxLen = 0;
    
    for (int i = 0; i < arr.length; i++) {
        sum += arr[i];
        
        // If sum equals k from start
        if (sum == k) {
            maxLen = i + 1;
        }
        
        // If (sum - k) exists, we found a subarray
        if (prefixMap.containsKey(sum - k)) {
            maxLen = Math.max(maxLen, i - prefixMap.get(sum - k));
        }
        
        // Store first occurrence of this sum
        if (!prefixMap.containsKey(sum)) {
            prefixMap.put(sum, i);
        }
    }
    
    return maxLen;
}
```

---

### Approach 2: Sliding Window (Only for Positive Numbers)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int longestSubarraySumPositive(int[] arr, int k) {
    int sum = 0;
    int maxLen = 0;
    int left = 0;
    
    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];
        
        // Shrink window if sum exceeds k
        while (sum > k && left <= right) {
            sum -= arr[left];
            left++;
        }
        
        // Check if current window has sum k
        if (sum == k) {
            maxLen = Math.max(maxLen, right - left + 1);
        }
    }
    
    return maxLen;
}
```

---

### Approach 3: Brute Force

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int longestSubarraySumBrute(int[] arr, int k) {
    int maxLen = 0;
    
    for (int i = 0; i < arr.length; i++) {
        int sum = 0;
        for (int j = i; j < arr.length; j++) {
            sum += arr[j];
            
            if (sum == k) {
                maxLen = Math.max(maxLen, j - i + 1);
            }
        }
    }
    
    return maxLen;
}
```

---

## Algorithm Walkthrough

### Example: arr = [10, 5, 2, 7, 1, 9], k = 15

**Prefix Sum + HashMap:**

```
Initial: sum=0, maxLen=0, prefixMap={}

i=0, arr[0]=10:
  sum = 0 + 10 = 10
  sum == 15? No
  (sum - k) = 10 - 15 = -5 in map? No
  prefixMap = {10: 0}

i=1, arr[1]=5:
  sum = 10 + 5 = 15
  sum == 15? Yes → maxLen = 1 + 1 = 2
  (sum - k) = 15 - 15 = 0 in map? No
  prefixMap = {10: 0, 15: 1}

i=2, arr[2]=2:
  sum = 15 + 2 = 17
  sum == 15? No
  (sum - k) = 17 - 15 = 2 in map? No
  prefixMap = {10: 0, 15: 1, 17: 2}

i=3, arr[3]=7:
  sum = 17 + 7 = 24
  sum == 15? No
  (sum - k) = 24 - 15 = 9 in map? No
  prefixMap = {10: 0, 15: 1, 17: 2, 24: 3}

i=4, arr[4]=1:
  sum = 24 + 1 = 25
  sum == 15? No
  (sum - k) = 25 - 15 = 10 in map? Yes, at index 0
    maxLen = max(2, 4 - 0) = 4
  prefixMap = {10: 0, 15: 1, 17: 2, 24: 3, 25: 4}

i=5, arr[5]=9:
  sum = 25 + 9 = 34
  sum == 15? No
  (sum - k) = 34 - 15 = 19 in map? No
  prefixMap = {10: 0, 15: 1, 17: 2, 24: 3, 25: 4, 34: 5}

Result: maxLen = 4
Subarray: [5, 2, 7, 1] (indices 1-4)
```

### Visual Explanation

```
arr = [10, 5, 2, 7, 1, 9], k = 15

Prefix sums:
  [10, 15, 17, 24, 25, 34]

Looking for: prefix[j] - prefix[i-1] = k
  Or: prefix[j] - k = prefix[i-1]

At i=4: prefix[4] = 25
  25 - 15 = 10
  10 found at index 0
  Subarray from index 1 to 4: [5, 2, 7, 1]
  Length: 4 - 0 = 4
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Prefix Sum + HashMap (Works for all cases)
    public static int longestSubarraySum(int[] arr, int k) {
        Map<Integer, Integer> prefixMap = new HashMap<>();
        int sum = 0;
        int maxLen = 0;
        
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
            
            if (sum == k) {
                maxLen = i + 1;
            }
            
            if (prefixMap.containsKey(sum - k)) {
                maxLen = Math.max(maxLen, i - prefixMap.get(sum - k));
            }
            
            if (!prefixMap.containsKey(sum)) {
                prefixMap.put(sum, i);
            }
        }
        
        return maxLen;
    }
    
    // Approach 2: Sliding Window (Only for positive numbers)
    public static int longestSubarraySumPositive(int[] arr, int k) {
        int sum = 0;
        int maxLen = 0;
        int left = 0;
        
        for (int right = 0; right < arr.length; right++) {
            sum += arr[right];
            
            while (sum > k && left <= right) {
                sum -= arr[left];
                left++;
            }
            
            if (sum == k) {
                maxLen = Math.max(maxLen, right - left + 1);
            }
        }
        
        return maxLen;
    }
    
    // Return the actual subarray
    public static int[] findLongestSubarray(int[] arr, int k) {
        Map<Integer, Integer> prefixMap = new HashMap<>();
        int sum = 0;
        int maxLen = 0;
        int startIdx = -1;
        
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
            
            if (sum == k) {
                maxLen = i + 1;
                startIdx = 0;
            }
            
            if (prefixMap.containsKey(sum - k)) {
                int len = i - prefixMap.get(sum - k);
                if (len > maxLen) {
                    maxLen = len;
                    startIdx = prefixMap.get(sum - k) + 1;
                }
            }
            
            if (!prefixMap.containsKey(sum)) {
                prefixMap.put(sum, i);
            }
        }
        
        if (startIdx == -1) {
            return new int[0];
        }
        
        return Arrays.copyOfRange(arr, startIdx, startIdx + maxLen);
    }
    
    public static boolean doTestsPass() {
        // Test 1
        int[] test1 = {10, 5, 2, 7, 1, 9};
        if (longestSubarraySum(test1, 15) != 4) return false;
        
        // Test 2
        int[] test2 = {1, 2, 3};
        if (longestSubarraySum(test2, 3) != 2) return false;
        
        // Test 3: With negatives
        int[] test3 = {-1, 1, 0};
        if (longestSubarraySum(test3, 0) != 3) return false;
        
        // Test 4: No solution
        int[] test4 = {1, 2, 3};
        if (longestSubarraySum(test4, 10) != 0) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {10, 5, 2, 7, 1, 9};
        int k = 15;
        
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Target sum: " + k);
        System.out.println("Longest subarray length: " + longestSubarraySum(arr, k));
        System.out.println("Longest subarray: " + Arrays.toString(findLongestSubarray(arr, k)));
    }
}
```

---

## Test Cases

```java
@Test
public void testLongestSubarraySum() {
    // Test 1: Normal case
    assertEquals(4, longestSubarraySum(new int[]{10, 5, 2, 7, 1, 9}, 15));
    
    // Test 2: Multiple subarrays
    assertEquals(2, longestSubarraySum(new int[]{1, 2, 3}, 3));
    
    // Test 3: With negatives
    assertEquals(3, longestSubarraySum(new int[]{-1, 1, 0}, 0));
    assertEquals(5, longestSubarraySum(new int[]{1, -1, 5, -2, 3}, 3));
    
    // Test 4: No solution
    assertEquals(0, longestSubarraySum(new int[]{1, 2, 3}, 10));
    
    // Test 5: Entire array
    assertEquals(4, longestSubarraySum(new int[]{1, 2, 3, 4}, 10));
    
    // Test 6: Single element
    assertEquals(1, longestSubarraySum(new int[]{5}, 5));
    assertEquals(0, longestSubarraySum(new int[]{5}, 3));
    
    // Test 7: Empty array
    assertEquals(0, longestSubarraySum(new int[]{}, 5));
    
    // Test 8: All zeros
    assertEquals(3, longestSubarraySum(new int[]{0, 0, 0}, 0));
    
    // Test 9: Negative sum
    assertEquals(2, longestSubarraySum(new int[]{-2, -1, 2, 1}, -3));
}
```

---

## Visual Representation

### Prefix Sum Concept

```
arr = [10, 5, 2, 7, 1, 9], k = 15

Prefix sums:
Index:  0   1   2   3   4   5
Value: 10  15  17  24  25  34

Subarray [5, 2, 7, 1] (indices 1-4):
  Sum = prefix[4] - prefix[0]
      = 25 - 10
      = 15 ✓

General formula:
  sum[i..j] = prefix[j] - prefix[i-1]
  
If sum[i..j] = k:
  prefix[j] - prefix[i-1] = k
  prefix[i-1] = prefix[j] - k
```

### HashMap Lookup

```
At index 4:
  Current prefix sum = 25
  Looking for: 25 - 15 = 10
  
  HashMap: {10: 0, 15: 1, 17: 2, 24: 3}
  Found 10 at index 0!
  
  Subarray from (0+1) to 4 = indices 1 to 4
  Length = 4 - 0 = 4
```

---

## Edge Cases

1. **Empty array:** [] → 0
2. **Single element match:** [5], k=5 → 1
3. **Single element no match:** [5], k=3 → 0
4. **No solution:** [1, 2, 3], k=10 → 0
5. **Entire array:** [1, 2, 3, 4], k=10 → 4
6. **With negatives:** [-1, 1, 0], k=0 → 3
7. **All zeros:** [0, 0, 0], k=0 → 3
8. **Multiple solutions:** Return longest

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Prefix Sum + HashMap | O(n) | O(n) | **Optimal for all cases** |
| Sliding Window | O(n) | O(1) | Only for positive numbers |
| Brute Force | O(n²) | O(1) | Not practical |

**Why HashMap is Needed:**
- With negatives, sum can increase or decrease
- Cannot use sliding window
- HashMap tracks all prefix sums

---

## Related Problems

1. **Subarray Sum Equals K** - Count subarrays
2. **Maximum Size Subarray Sum Equals k** - Same problem
3. **Minimum Size Subarray Sum** - Sum ≥ k
4. **Continuous Subarray Sum** - Sum multiple of k
5. **Subarray Sum Divisible by K** - Divisibility
6. **Maximum Subarray** - Maximum sum (Kadane's)

---

## Interview Tips

### Clarification Questions
1. Can array have negatives? (Yes - use HashMap)
2. Can array be empty? (Return 0)
3. What if no solution? (Return 0)
4. Return length or subarray? (Usually length)
5. Multiple solutions? (Return longest)

### Approach Explanation
1. "Use prefix sum with HashMap"
2. "Track cumulative sum at each index"
3. "If (sum - k) exists in map, found subarray"
4. "Store first occurrence of each sum"
5. "O(n) time, O(n) space"

### Common Mistakes
- Not storing first occurrence only
- Wrong length calculation
- Not handling sum == k case
- Forgetting negative numbers
- Using sliding window with negatives

### Why Store First Occurrence?

```java
// Store first occurrence to get longest subarray
if (!prefixMap.containsKey(sum)) {
    prefixMap.put(sum, i);
}

// Example: [1, 0, 1], k = 1
// At i=0: sum=1, store {1: 0}
// At i=1: sum=1, don't update (keep first)
// At i=2: sum=2, looking for 2-1=1
//   Found at index 0, length = 2-0 = 2 ✓

// If we stored last occurrence:
//   Would find at index 1, length = 2-1 = 1 ✗
```

---

## Real-World Applications

1. **Financial Analysis** - Find periods with target profit
2. **Time Series** - Find windows with specific sum
3. **Resource Management** - Allocate resources optimally
4. **Data Analysis** - Find patterns in data
5. **Signal Processing** - Detect specific patterns
6. **Load Balancing** - Distribute load evenly

---

## Key Takeaways

1. **Prefix Sum:** Key technique for subarray problems
2. **HashMap:** Stores prefix sums for O(1) lookup
3. **First Occurrence:** Store first to get longest subarray
4. **Time Complexity:** O(n) with HashMap
5. **Space Complexity:** O(n) for HashMap
6. **Negatives:** HashMap approach handles all cases
7. **Sliding Window:** Only works for positive numbers

---

## Additional Notes

### Why Sliding Window Fails with Negatives

```
arr = [1, -1, 5, -2, 3], k = 3

Sliding window approach:
  [1] sum=1 < 3
  [1, -1] sum=0 < 3
  [1, -1, 5] sum=5 > 3, shrink
  [-1, 5] sum=4 > 3, shrink
  [5] sum=5 > 3, shrink
  [] sum=0 < 3
  
Missed: [5, -2] has sum 3!

Problem: Removing element can increase sum (if negative)
Solution: Use HashMap approach
```

### Prefix Sum Formula

```
prefix[i] = arr[0] + arr[1] + ... + arr[i]

sum[i..j] = arr[i] + arr[i+1] + ... + arr[j]
          = prefix[j] - prefix[i-1]

If sum[i..j] = k:
  prefix[j] - prefix[i-1] = k
  prefix[i-1] = prefix[j] - k

At index j, look for (prefix[j] - k) in HashMap
```

### Count vs Length

```java
// Count subarrays with sum k
public static int countSubarrays(int[] arr, int k) {
    Map<Integer, Integer> prefixMap = new HashMap<>();
    prefixMap.put(0, 1); // Empty prefix
    int sum = 0;
    int count = 0;
    
    for (int num : arr) {
        sum += num;
        count += prefixMap.getOrDefault(sum - k, 0);
        prefixMap.put(sum, prefixMap.getOrDefault(sum, 0) + 1);
    }
    
    return count;
}

// For count: Store frequency
// For length: Store first index
```

### Optimization: Early Exit

```java
// If k is very large and all elements positive
int maxPossible = Arrays.stream(arr).sum();
if (k > maxPossible) {
    return 0;
}
```

### Extension: At Most K

```java
// Longest subarray with sum ≤ k (positive numbers)
public static int longestSubarrayAtMostK(int[] arr, int k) {
    int sum = 0;
    int maxLen = 0;
    int left = 0;
    
    for (int right = 0; right < arr.length; right++) {
        sum += arr[right];
        
        while (sum > k) {
            sum -= arr[left];
            left++;
        }
        
        maxLen = Math.max(maxLen, right - left + 1);
    }
    
    return maxLen;
}
```

### Why HashMap Over Array?

```
Could use array if sum range is small:
  int[] prefixMap = new int[MAX_SUM];

But:
  - Sum can be negative
  - Sum range can be large
  - HashMap more flexible

HashMap is better choice!
```

### Comparison: Exact vs Minimum

```
Exact sum = k:
  - Use HashMap
  - Check (sum - k)
  
Minimum sum ≥ k:
  - Use sliding window
  - Shrink when sum ≥ k
  
Different problems, different approaches!
```
