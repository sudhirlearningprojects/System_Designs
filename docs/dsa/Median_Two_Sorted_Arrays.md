# Median of Two Sorted Arrays

## Problem Statement

Find the median of two sorted arrays.

**Median:** Middle value in sorted order. If even count, average of two middle values.

**Input:** Two sorted arrays  
**Output:** Median (double)

**Examples:**
```
[1, 3] and [2] → 2.0
  Combined: [1, 2, 3], median = 2

[1, 2] and [3, 4] → 2.5
  Combined: [1, 2, 3, 4], median = (2 + 3) / 2 = 2.5

[1, 3, 5] and [2, 4, 6] → 3.5
  Combined: [1, 2, 3, 4, 5, 6], median = (3 + 4) / 2 = 3.5
```

---

## Solution Approaches

### Approach 1: Merge and Find (Simple)

**Time Complexity:** O(m + n)  
**Space Complexity:** O(m + n)

```java
public static double findMedian(int[] nums1, int[] nums2) {
    int[] merged = merge(nums1, nums2);
    int n = merged.length;
    
    if (n % 2 == 0) {
        return (merged[n/2 - 1] + merged[n/2]) / 2.0;
    } else {
        return merged[n/2];
    }
}

private static int[] merge(int[] nums1, int[] nums2) {
    int[] result = new int[nums1.length + nums2.length];
    int i = 0, j = 0, k = 0;
    
    while (i < nums1.length && j < nums2.length) {
        if (nums1[i] <= nums2[j]) {
            result[k++] = nums1[i++];
        } else {
            result[k++] = nums2[j++];
        }
    }
    
    while (i < nums1.length) result[k++] = nums1[i++];
    while (j < nums2.length) result[k++] = nums2[j++];
    
    return result;
}
```

---

### Approach 2: Binary Search (Optimal)

**Time Complexity:** O(log(min(m, n)))  
**Space Complexity:** O(1)

```java
public static double findMedian(int[] nums1, int[] nums2) {
    // Ensure nums1 is smaller
    if (nums1.length > nums2.length) {
        return findMedian(nums2, nums1);
    }
    
    int m = nums1.length;
    int n = nums2.length;
    int left = 0, right = m;
    
    while (left <= right) {
        int partition1 = (left + right) / 2;
        int partition2 = (m + n + 1) / 2 - partition1;
        
        int maxLeft1 = (partition1 == 0) ? Integer.MIN_VALUE : nums1[partition1 - 1];
        int minRight1 = (partition1 == m) ? Integer.MAX_VALUE : nums1[partition1];
        
        int maxLeft2 = (partition2 == 0) ? Integer.MIN_VALUE : nums2[partition2 - 1];
        int minRight2 = (partition2 == n) ? Integer.MAX_VALUE : nums2[partition2];
        
        if (maxLeft1 <= minRight2 && maxLeft2 <= minRight1) {
            // Found correct partition
            if ((m + n) % 2 == 0) {
                return (Math.max(maxLeft1, maxLeft2) + Math.min(minRight1, minRight2)) / 2.0;
            } else {
                return Math.max(maxLeft1, maxLeft2);
            }
        } else if (maxLeft1 > minRight2) {
            right = partition1 - 1;
        } else {
            left = partition1 + 1;
        }
    }
    
    throw new IllegalArgumentException("Input arrays are not sorted");
}
```

---

### Approach 3: Partial Merge (Space Optimized)

**Time Complexity:** O(m + n)  
**Space Complexity:** O(1)

```java
public static double findMedian(int[] nums1, int[] nums2) {
    int total = nums1.length + nums2.length;
    int mid = total / 2;
    
    int i = 0, j = 0;
    int prev = 0, curr = 0;
    
    for (int k = 0; k <= mid; k++) {
        prev = curr;
        
        if (i < nums1.length && (j >= nums2.length || nums1[i] <= nums2[j])) {
            curr = nums1[i++];
        } else {
            curr = nums2[j++];
        }
    }
    
    if (total % 2 == 0) {
        return (prev + curr) / 2.0;
    } else {
        return curr;
    }
}
```

---

## Algorithm Walkthrough

### Example 1: [1, 3] and [2]

**Merge Approach:**
```
nums1 = [1, 3]
nums2 = [2]

Merge:
  Compare 1 and 2 → take 1 → [1]
  Compare 3 and 2 → take 2 → [1, 2]
  Take remaining 3 → [1, 2, 3]

Median:
  Length = 3 (odd)
  Median = merged[3/2] = merged[1] = 2

Result: 2.0
```

**Binary Search Approach:**
```
nums1 = [1, 3], m = 2
nums2 = [2], n = 1
Total = 3

partition1 = 1
partition2 = (3 + 1) / 2 - 1 = 1

Left side: [1] from nums1, [2] from nums2
Right side: [3] from nums1, [] from nums2

maxLeft1 = 1, minRight1 = 3
maxLeft2 = 2, minRight2 = ∞

Check: 1 ≤ ∞ ✓ and 2 ≤ 3 ✓

Odd total: max(1, 2) = 2

Result: 2.0
```

### Example 2: [1, 2] and [3, 4]

```
Merged: [1, 2, 3, 4]
Length = 4 (even)
Median = (merged[1] + merged[2]) / 2
       = (2 + 3) / 2
       = 2.5

Result: 2.5
```

---

## Complete Implementation

```java
public class MedianTwoSortedArrays {
    
    // Approach 1: Merge arrays (Simple)
    public static double findMedianMerge(int[] nums1, int[] nums2) {
        int[] merged = merge(nums1, nums2);
        int n = merged.length;
        
        if (n % 2 == 0) {
            return (merged[n/2 - 1] + merged[n/2]) / 2.0;
        } else {
            return merged[n/2];
        }
    }
    
    private static int[] merge(int[] nums1, int[] nums2) {
        int[] result = new int[nums1.length + nums2.length];
        int i = 0, j = 0, k = 0;
        
        while (i < nums1.length && j < nums2.length) {
            if (nums1[i] <= nums2[j]) {
                result[k++] = nums1[i++];
            } else {
                result[k++] = nums2[j++];
            }
        }
        
        while (i < nums1.length) result[k++] = nums1[i++];
        while (j < nums2.length) result[k++] = nums2[j++];
        
        return result;
    }
    
    // Approach 2: Binary Search (Optimal)
    public static double findMedianBinarySearch(int[] nums1, int[] nums2) {
        if (nums1.length > nums2.length) {
            return findMedianBinarySearch(nums2, nums1);
        }
        
        int m = nums1.length;
        int n = nums2.length;
        int left = 0, right = m;
        
        while (left <= right) {
            int partition1 = (left + right) / 2;
            int partition2 = (m + n + 1) / 2 - partition1;
            
            int maxLeft1 = (partition1 == 0) ? Integer.MIN_VALUE : nums1[partition1 - 1];
            int minRight1 = (partition1 == m) ? Integer.MAX_VALUE : nums1[partition1];
            
            int maxLeft2 = (partition2 == 0) ? Integer.MIN_VALUE : nums2[partition2 - 1];
            int minRight2 = (partition2 == n) ? Integer.MAX_VALUE : nums2[partition2];
            
            if (maxLeft1 <= minRight2 && maxLeft2 <= minRight1) {
                if ((m + n) % 2 == 0) {
                    return (Math.max(maxLeft1, maxLeft2) + Math.min(minRight1, minRight2)) / 2.0;
                } else {
                    return Math.max(maxLeft1, maxLeft2);
                }
            } else if (maxLeft1 > minRight2) {
                right = partition1 - 1;
            } else {
                left = partition1 + 1;
            }
        }
        
        throw new IllegalArgumentException("Input arrays are not sorted");
    }
    
    // Approach 3: Partial merge (Space optimized)
    public static double findMedianPartial(int[] nums1, int[] nums2) {
        int total = nums1.length + nums2.length;
        int mid = total / 2;
        
        int i = 0, j = 0;
        int prev = 0, curr = 0;
        
        for (int k = 0; k <= mid; k++) {
            prev = curr;
            
            if (i < nums1.length && (j >= nums2.length || nums1[i] <= nums2[j])) {
                curr = nums1[i++];
            } else {
                curr = nums2[j++];
            }
        }
        
        if (total % 2 == 0) {
            return (prev + curr) / 2.0;
        } else {
            return curr;
        }
    }
    
    public static void main(String[] args) {
        // Test case 1
        int[] nums1 = {1, 3};
        int[] nums2 = {2};
        System.out.println(findMedianBinarySearch(nums1, nums2)); // 2.0
        
        // Test case 2
        int[] nums3 = {1, 2};
        int[] nums4 = {3, 4};
        System.out.println(findMedianBinarySearch(nums3, nums4)); // 2.5
    }
}
```

---

## Test Cases

```java
@Test
public void testFindMedian() {
    // Odd total length
    assertEquals(2.0, findMedian(new int[]{1, 3}, new int[]{2}), 0.001);
    
    // Even total length
    assertEquals(2.5, findMedian(new int[]{1, 2}, new int[]{3, 4}), 0.001);
    
    // One empty array
    assertEquals(2.0, findMedian(new int[]{}, new int[]{2}), 0.001);
    assertEquals(1.5, findMedian(new int[]{1, 2}, new int[]{}), 0.001);
    
    // Different sizes
    assertEquals(3.5, findMedian(new int[]{1, 3, 5}, new int[]{2, 4, 6}), 0.001);
    
    // Single elements
    assertEquals(1.5, findMedian(new int[]{1}, new int[]{2}), 0.001);
    
    // Duplicates
    assertEquals(2.0, findMedian(new int[]{1, 2}, new int[]{2, 3}), 0.001);
}
```

---

## Visual Representation

### Binary Search Partition

```
nums1 = [1, 3, 5, 7]
nums2 = [2, 4, 6, 8, 10]

Total = 9 (odd), need 5 elements on left

Partition:
nums1: [1, 3] | [5, 7]
nums2: [2, 4, 6] | [8, 10]

Left side: [1, 3, 2, 4, 6] (5 elements)
Right side: [5, 7, 8, 10] (4 elements)

Check:
  maxLeft1 = 3, minRight1 = 5
  maxLeft2 = 6, minRight2 = 8
  
  3 ≤ 8 ✓ and 6 ≤ 5 ✗
  
Adjust partition...

Final: median = max(left side) = 6
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[], [1]` | `1.0` | One empty |
| `[1], [2]` | `1.5` | Two singles |
| `[1,1], [1,1]` | `1.0` | All same |
| `[1,2,3], []` | `2.0` | One empty |
| `[1], [2,3,4]` | `2.5` | Different sizes |

---

## Common Mistakes

1. **Not Handling Empty Arrays:**
   ```java
   // Check for empty arrays first
   if (nums1.length == 0) return findMedianSingle(nums2);
   if (nums2.length == 0) return findMedianSingle(nums1);
   ```

2. **Integer Division:**
   ```java
   // WRONG - integer division
   return (a + b) / 2;
   
   // CORRECT - floating point
   return (a + b) / 2.0;
   ```

3. **Wrong Partition Calculation:**
   ```java
   // CORRECT formula
   partition2 = (m + n + 1) / 2 - partition1;
   ```

4. **Not Ensuring nums1 is Smaller:**
   ```java
   // Binary search on smaller array
   if (nums1.length > nums2.length) {
       return findMedian(nums2, nums1);
   }
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Merge | O(m + n) | O(m + n) | Simple, intuitive |
| Partial merge | O(m + n) | O(1) | Space optimized |
| Binary search | O(log(min(m,n))) | O(1) | Optimal |

---

## Binary Search Intuition

```
Goal: Partition arrays so left half has (m+n+1)/2 elements

nums1: [... maxLeft1] | [minRight1 ...]
nums2: [... maxLeft2] | [minRight2 ...]

Valid partition when:
  maxLeft1 ≤ minRight2
  maxLeft2 ≤ minRight1

Median:
  Odd total: max(maxLeft1, maxLeft2)
  Even total: (max(maxLeft1, maxLeft2) + min(minRight1, minRight2)) / 2
```

---

## Related Problems

- **LeetCode 4:** Median of Two Sorted Arrays
- **LeetCode 295:** Find Median from Data Stream
- **Merge K Sorted Arrays**
- **Kth element in sorted arrays**

---

## Interview Tips

1. **Clarify Requirements:**
   - Arrays always sorted?
   - Handle empty arrays?
   - Return type (double)?
   - Duplicates allowed?

2. **Start with Simple:**
   - Merge approach first
   - Then optimize to binary search

3. **Explain Binary Search:**
   - Partition concept
   - Why log(min(m,n))

4. **Walk Through Example:**
   - Use [1,3] and [2]
   - Show partition logic

5. **Discuss Trade-offs:**
   - Time vs space
   - Simplicity vs optimization

---

## Real-World Applications

- **Statistics:** Finding median in distributed data
- **Database Queries:** Median aggregation
- **Data Analysis:** Percentile calculations
- **Load Balancing:** Finding middle value
- **Signal Processing:** Median filtering
- **Performance Monitoring:** Response time analysis

---

## Key Takeaways

✅ Merge approach: O(m+n) time, simple to implement  
✅ Binary search: O(log(min(m,n))) time, optimal  
✅ Partition arrays to have equal left/right halves  
✅ Handle odd/even total length differently  
✅ Ensure binary search on smaller array  
✅ Use Integer.MIN_VALUE/MAX_VALUE for boundaries  
✅ Median = average of two middle values if even count  
✅ Critical: maxLeft1 ≤ minRight2 && maxLeft2 ≤ minRight1
