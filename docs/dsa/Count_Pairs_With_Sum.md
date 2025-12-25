# Count Pairs with Given Sum

## Problem Statement

Find the number of pairs from a given integer array whose sum equals a given target number.

**Input:** 
- Array of integers
- Target sum

**Output:** Count of pairs

**Examples:**
```
Input: arr = [1, 5, 7, -1, 5], target = 6
Output: 3
Explanation: Pairs are (1, 5), (7, -1), (1, 5)

Input: arr = [1, 1, 1, 1], target = 2
Output: 6
Explanation: All pairs of 1s sum to 2

Input: arr = [1, 2, 3, 4], target = 5
Output: 2
Explanation: Pairs are (1, 4), (2, 3)

Input: arr = [1, 2, 3], target = 10
Output: 0
Explanation: No pairs sum to 10
```

---

## Solution Approaches

### Approach 1: HashMap (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static int countPairs(int[] arr, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    int count = 0;
    
    for (int num : arr) {
        int complement = target - num;
        
        if (map.containsKey(complement)) {
            count += map.get(complement);
        }
        
        map.put(num, map.getOrDefault(num, 0) + 1);
    }
    
    return count;
}
```

---

### Approach 2: Two Pointers (Sorted Array)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(1)

```java
public static int countPairsTwoPointers(int[] arr, int target) {
    Arrays.sort(arr);
    int left = 0, right = arr.length - 1;
    int count = 0;
    
    while (left < right) {
        int sum = arr[left] + arr[right];
        
        if (sum == target) {
            // Count duplicates
            if (arr[left] == arr[right]) {
                int n = right - left + 1;
                count += n * (n - 1) / 2;
                break;
            }
            
            int leftCount = 1, rightCount = 1;
            
            while (left + 1 < right && arr[left] == arr[left + 1]) {
                leftCount++;
                left++;
            }
            
            while (right - 1 > left && arr[right] == arr[right - 1]) {
                rightCount++;
                right--;
            }
            
            count += leftCount * rightCount;
            left++;
            right--;
        } else if (sum < target) {
            left++;
        } else {
            right--;
        }
    }
    
    return count;
}
```

---

### Approach 3: Brute Force

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int countPairsBrute(int[] arr, int target) {
    int count = 0;
    
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (arr[i] + arr[j] == target) {
                count++;
            }
        }
    }
    
    return count;
}
```

---

### Approach 4: HashSet (Find if Pair Exists)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static boolean hasPair(int[] arr, int target) {
    Set<Integer> seen = new HashSet<>();
    
    for (int num : arr) {
        if (seen.contains(target - num)) {
            return true;
        }
        seen.add(num);
    }
    
    return false;
}
```

---

## Algorithm Walkthrough

### Example: arr = [1, 5, 7, -1, 5], target = 6

**HashMap Approach:**

```
Initial: map = {}, count = 0

Step 1: num = 1
  complement = 6 - 1 = 5
  5 not in map
  map = {1: 1}, count = 0

Step 2: num = 5
  complement = 6 - 5 = 1
  1 in map with frequency 1
  count = 0 + 1 = 1
  map = {1: 1, 5: 1}

Step 3: num = 7
  complement = 6 - 7 = -1
  -1 not in map
  map = {1: 1, 5: 1, 7: 1}, count = 1

Step 4: num = -1
  complement = 6 - (-1) = 7
  7 in map with frequency 1
  count = 1 + 1 = 2
  map = {1: 1, 5: 1, 7: 1, -1: 1}

Step 5: num = 5
  complement = 6 - 5 = 1
  1 in map with frequency 1
  count = 2 + 1 = 3
  map = {1: 1, 5: 2, 7: 1, -1: 1}

Result: count = 3
Pairs: (1, 5), (7, -1), (1, 5)
```

### Example: arr = [1, 1, 1, 1], target = 2

```
Step 1: num = 1
  complement = 1
  1 not in map
  map = {1: 1}, count = 0

Step 2: num = 1
  complement = 1
  1 in map with frequency 1
  count = 0 + 1 = 1
  map = {1: 2}

Step 3: num = 1
  complement = 1
  1 in map with frequency 2
  count = 1 + 2 = 3
  map = {1: 3}

Step 4: num = 1
  complement = 1
  1 in map with frequency 3
  count = 3 + 3 = 6
  map = {1: 4}

Result: count = 6
Pairs: (1₁,1₂), (1₁,1₃), (1₁,1₄), (1₂,1₃), (1₂,1₄), (1₃,1₄)
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: HashMap (Optimal)
    public static int countPairs(int[] arr, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        int count = 0;
        
        for (int num : arr) {
            int complement = target - num;
            
            if (map.containsKey(complement)) {
                count += map.get(complement);
            }
            
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        
        return count;
    }
    
    // Approach 2: Two Pointers
    public static int countPairsTwoPointers(int[] arr, int target) {
        Arrays.sort(arr);
        int left = 0, right = arr.length - 1;
        int count = 0;
        
        while (left < right) {
            int sum = arr[left] + arr[right];
            
            if (sum == target) {
                if (arr[left] == arr[right]) {
                    int n = right - left + 1;
                    count += n * (n - 1) / 2;
                    break;
                }
                
                int leftCount = 1, rightCount = 1;
                
                while (left + 1 < right && arr[left] == arr[left + 1]) {
                    leftCount++;
                    left++;
                }
                
                while (right - 1 > left && arr[right] == arr[right - 1]) {
                    rightCount++;
                    right--;
                }
                
                count += leftCount * rightCount;
                left++;
                right--;
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        
        return count;
    }
    
    // Approach 3: Brute Force
    public static int countPairsBrute(int[] arr, int target) {
        int count = 0;
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] + arr[j] == target) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    // Check if at least one pair exists
    public static boolean hasPair(int[] arr, int target) {
        Set<Integer> seen = new HashSet<>();
        
        for (int num : arr) {
            if (seen.contains(target - num)) {
                return true;
            }
            seen.add(num);
        }
        
        return false;
    }
    
    // Return all pairs
    public static List<int[]> findAllPairs(int[] arr, int target) {
        List<int[]> pairs = new ArrayList<>();
        Map<Integer, Integer> map = new HashMap<>();
        
        for (int num : arr) {
            int complement = target - num;
            
            if (map.containsKey(complement)) {
                for (int i = 0; i < map.get(complement); i++) {
                    pairs.add(new int[]{complement, num});
                }
            }
            
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        
        return pairs;
    }
    
    public static boolean doTestsPass() {
        // Test 1
        int[] test1 = {1, 5, 7, -1, 5};
        if (countPairs(test1, 6) != 3) return false;
        
        // Test 2
        int[] test2 = {1, 1, 1, 1};
        if (countPairs(test2, 2) != 6) return false;
        
        // Test 3
        int[] test3 = {1, 2, 3, 4};
        if (countPairs(test3, 5) != 2) return false;
        
        // Test 4
        int[] test4 = {1, 2, 3};
        if (countPairs(test4, 10) != 0) return false;
        
        // Test 5
        int[] test5 = {1};
        if (countPairs(test5, 2) != 0) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {1, 5, 7, -1, 5};
        int target = 6;
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Target: " + target);
        System.out.println("Number of pairs: " + countPairs(arr, target));
    }
}
```

---

## Test Cases

```java
@Test
public void testCountPairs() {
    // Test 1: Multiple pairs
    assertEquals(3, countPairs(new int[]{1, 5, 7, -1, 5}, 6));
    
    // Test 2: All same elements
    assertEquals(6, countPairs(new int[]{1, 1, 1, 1}, 2));
    
    // Test 3: Simple case
    assertEquals(2, countPairs(new int[]{1, 2, 3, 4}, 5));
    
    // Test 4: No pairs
    assertEquals(0, countPairs(new int[]{1, 2, 3}, 10));
    
    // Test 5: Single element
    assertEquals(0, countPairs(new int[]{1}, 2));
    
    // Test 6: Empty array
    assertEquals(0, countPairs(new int[]{}, 5));
    
    // Test 7: Negative numbers
    assertEquals(2, countPairs(new int[]{-1, -2, 1, 2}, 0));
    
    // Test 8: Zero target
    assertEquals(1, countPairs(new int[]{0, 0}, 0));
    
    // Test 9: Duplicates
    assertEquals(4, countPairs(new int[]{2, 2, 3, 3}, 5));
    
    // Test 10: Large array
    int[] large = new int[1000];
    Arrays.fill(large, 1);
    assertEquals(499500, countPairs(large, 2));
}
```

---

## Visual Representation

### HashMap State Changes

```
arr = [1, 5, 7, -1, 5], target = 6

Step 1: num=1
  map: {1:1}
  count: 0

Step 2: num=5
  complement=1 found!
  map: {1:1, 5:1}
  count: 1

Step 3: num=7
  map: {1:1, 5:1, 7:1}
  count: 1

Step 4: num=-1
  complement=7 found!
  map: {1:1, 5:1, 7:1, -1:1}
  count: 2

Step 5: num=5
  complement=1 found!
  map: {1:1, 5:2, 7:1, -1:1}
  count: 3

Pairs found:
  (1, 5) at step 2
  (7, -1) at step 4
  (1, 5) at step 5
```

### Two Pointers Movement

```
arr = [1, 2, 3, 4], target = 5
After sorting: [1, 2, 3, 4]

Initial: left=0, right=3
  arr[0] + arr[4] = 1 + 4 = 5 ✓
  count = 1
  left=1, right=2

Next: left=1, right=2
  arr[1] + arr[2] = 2 + 3 = 5 ✓
  count = 2
  left=2, right=1

left >= right, stop

Result: 2 pairs
```

---

## Edge Cases

1. **Empty array:** [] → 0
2. **Single element:** [1] → 0
3. **No pairs:** [1, 2, 3], target=10 → 0
4. **All same:** [1, 1, 1, 1], target=2 → 6
5. **Negative numbers:** [-1, -2, 1, 2], target=0 → 2
6. **Zero target:** [0, 0], target=0 → 1
7. **Duplicates:** [2, 2, 3, 3], target=5 → 4
8. **Large array:** All 1s, target=2 → n*(n-1)/2

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(n) | **Optimal** |
| Two Pointers | O(n log n) | O(1) | Requires sorting |
| Brute Force | O(n²) | O(1) | Not practical |
| HashSet (exists) | O(n) | O(n) | Only checks existence |

**Why HashMap is Optimal:**
- Single pass through array
- O(1) lookup for complement
- Handles duplicates correctly

---

## Related Problems

1. **Two Sum** - Return indices of pair
2. **3Sum** - Find triplets with sum
3. **4Sum** - Find quadruplets with sum
4. **Two Sum II** - Sorted array variant
5. **Two Sum III** - Design data structure
6. **Count Pairs with Difference K** - Different constraint

---

## Interview Tips

### Clarification Questions
1. Can array have duplicates? (Yes, count all pairs)
2. Can numbers be negative? (Yes)
3. Count unique pairs or all pairs? (All pairs)
4. Can we modify the array? (Depends on approach)
5. What if array is empty? (Return 0)

### Approach Explanation
1. "Use HashMap to store frequencies"
2. "For each number, check if complement exists"
3. "Add frequency of complement to count"
4. "Then add current number to map"
5. "O(n) time, O(n) space"

### Common Mistakes
- Not handling duplicates correctly
- Counting same pair twice
- Wrong order of operations (check before adding)
- Not considering negative numbers
- Integer overflow for large counts

### Why Check Before Adding?

```java
// Correct: Check complement before adding
if (map.containsKey(complement)) {
    count += map.get(complement);
}
map.put(num, map.getOrDefault(num, 0) + 1);

// Wrong: Add before checking
map.put(num, map.getOrDefault(num, 0) + 1);
if (map.containsKey(complement)) {
    count += map.get(complement); // Counts self-pairs!
}
```

---

## Real-World Applications

1. **E-commerce** - Finding product combinations
2. **Finance** - Portfolio pair analysis
3. **Social Networks** - Friend pair recommendations
4. **Gaming** - Team formation
5. **Data Analysis** - Correlation detection
6. **Inventory** - Stock pair matching

---

## Key Takeaways

1. **HashMap:** Optimal O(n) solution
2. **Complement:** target - num
3. **Order Matters:** Check before adding to map
4. **Duplicates:** Frequency map handles correctly
5. **Time Complexity:** O(n) with HashMap
6. **Space Complexity:** O(n) for HashMap
7. **Alternative:** Two pointers O(n log n) with O(1) space

---

## Additional Notes

### Counting Formula for Duplicates

```
If all n elements are same and sum to target:
  Number of pairs = n * (n - 1) / 2

Example: [1, 1, 1, 1], target = 2
  n = 4
  pairs = 4 * 3 / 2 = 6
  
  Pairs: (1₁,1₂), (1₁,1₃), (1₁,1₄), (1₂,1₃), (1₂,1₄), (1₃,1₄)
```

### Two Pointers with Duplicates

```java
// When arr[left] == arr[right]
if (arr[left] == arr[right]) {
    int n = right - left + 1;
    count += n * (n - 1) / 2;
    break;
}

// Example: [1, 1, 1, 1], target = 2
// All elements same, use formula
```

### Return Actual Pairs

```java
public static List<int[]> findAllPairs(int[] arr, int target) {
    List<int[]> pairs = new ArrayList<>();
    Map<Integer, Integer> map = new HashMap<>();
    
    for (int num : arr) {
        int complement = target - num;
        
        if (map.containsKey(complement)) {
            for (int i = 0; i < map.get(complement); i++) {
                pairs.add(new int[]{complement, num});
            }
        }
        
        map.put(num, map.getOrDefault(num, 0) + 1);
    }
    
    return pairs;
}
```

### Unique Pairs Only

```java
// If we want unique pairs (no duplicates)
public static int countUniquePairs(int[] arr, int target) {
    Set<Integer> seen = new HashSet<>();
    Set<String> pairs = new HashSet<>();
    
    for (int num : arr) {
        int complement = target - num;
        
        if (seen.contains(complement)) {
            int min = Math.min(num, complement);
            int max = Math.max(num, complement);
            pairs.add(min + "," + max);
        }
        
        seen.add(num);
    }
    
    return pairs.size();
}
```

### K-Sum Generalization

```java
// For k numbers that sum to target
public static int kSum(int[] arr, int k, int target) {
    // Use recursion or DP
    // Base case: k=2 (two sum)
    // Recursive: fix one element, solve (k-1)-sum
}
```

### Optimization: Early Exit

```java
// If array is sorted and all positive
if (arr[0] + arr[1] > target) {
    return 0; // No pairs possible
}
```

### Handling Integer Overflow

```java
// For very large arrays with all same elements
// n * (n - 1) / 2 might overflow

// Use long
long count = (long) n * (n - 1) / 2;

// Or check before calculation
if (n > 46340) { // sqrt(Integer.MAX_VALUE)
    // Use BigInteger or return error
}
```

### Comparison: HashMap vs Two Pointers

```
HashMap:
  + O(n) time
  + Handles unsorted array
  - O(n) space
  + Single pass

Two Pointers:
  + O(1) space
  - O(n log n) time (sorting)
  - Modifies array order
  + Good for sorted input
```

### Three Sum Extension

```java
// Find triplets that sum to target
public static int threeSumCount(int[] arr, int target) {
    Arrays.sort(arr);
    int count = 0;
    
    for (int i = 0; i < arr.length - 2; i++) {
        int newTarget = target - arr[i];
        // Use two pointers for remaining array
        count += twoSumCount(arr, i + 1, newTarget);
    }
    
    return count;
}
```
