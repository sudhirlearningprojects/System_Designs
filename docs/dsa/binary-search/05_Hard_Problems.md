# Binary Search - Hard Problems

## Problem 1: Median of Two Sorted Arrays (LeetCode 4)

**Difficulty**: Hard  
**Pattern**: Binary Search with Partitioning

### Problem Statement
Given two sorted arrays `nums1` and `nums2` of size `m` and `n` respectively, return the median of the two sorted arrays.

The overall run time complexity should be O(log(min(m,n))).

### Examples
```
Input: nums1 = [1,3], nums2 = [2]
Output: 2.00000
Explanation: merged = [1,2,3], median = 2

Input: nums1 = [1,2], nums2 = [3,4]
Output: 2.50000
Explanation: merged = [1,2,3,4], median = (2+3)/2 = 2.5

Input: nums1 = [], nums2 = [1]
Output: 1.00000
```

### Approach

The key insight is to partition both arrays such that:
1. Left partition has same number of elements as right partition (or one more)
2. All elements in left partition ≤ all elements in right partition

```
nums1: [1, 3, 8, 9, 15]
nums2: [7, 11, 18, 19, 21, 25]

Partition:
nums1: [1, 3, 8] | [9, 15]
nums2: [7, 11]   | [18, 19, 21, 25]

Left partition: [1, 3, 8, 7, 11]
Right partition: [9, 15, 18, 19, 21, 25]

Median = (max(left) + min(right)) / 2 = (11 + 9) / 2 = 10
```

### Solution
```java
public double findMedianSortedArrays(int[] nums1, int[] nums2) {
    // Ensure nums1 is the smaller array
    if (nums1.length > nums2.length) {
        return findMedianSortedArrays(nums2, nums1);
    }
    
    int m = nums1.length;
    int n = nums2.length;
    int left = 0, right = m;
    
    while (left <= right) {
        // Partition nums1 at partitionX
        int partitionX = left + (right - left) / 2;
        // Partition nums2 such that left half has same elements
        int partitionY = (m + n + 1) / 2 - partitionX;
        
        // Get boundary elements
        int maxLeftX = (partitionX == 0) ? Integer.MIN_VALUE : nums1[partitionX - 1];
        int minRightX = (partitionX == m) ? Integer.MAX_VALUE : nums1[partitionX];
        
        int maxLeftY = (partitionY == 0) ? Integer.MIN_VALUE : nums2[partitionY - 1];
        int minRightY = (partitionY == n) ? Integer.MAX_VALUE : nums2[partitionY];
        
        // Check if we found the correct partition
        if (maxLeftX <= minRightY && maxLeftY <= minRightX) {
            // Found correct partition
            if ((m + n) % 2 == 0) {
                // Even total length
                return (Math.max(maxLeftX, maxLeftY) + Math.min(minRightX, minRightY)) / 2.0;
            } else {
                // Odd total length
                return Math.max(maxLeftX, maxLeftY);
            }
        } else if (maxLeftX > minRightY) {
            // Move partition left in nums1
            right = partitionX - 1;
        } else {
            // Move partition right in nums1
            left = partitionX + 1;
        }
    }
    
    throw new IllegalArgumentException("Input arrays are not sorted");
}
```

### Complexity Analysis
- **Time**: O(log(min(m, n))) - binary search on smaller array
- **Space**: O(1) - only constant variables

### Detailed Dry Run

```
nums1 = [1, 3, 8, 9, 15]  (m = 5)
nums2 = [7, 11, 18, 19, 21, 25]  (n = 6)
Total = 11 (odd), so median is at position 6

Iteration 1: left=0, right=5, partitionX=2
  partitionY = (5+6+1)/2 - 2 = 4
  
  nums1: [1, 3] | [8, 9, 15]
  nums2: [7, 11, 18, 19] | [21, 25]
  
  maxLeftX = nums1[1] = 3
  minRightX = nums1[2] = 8
  maxLeftY = nums2[3] = 19
  minRightY = nums2[4] = 21
  
  Check: maxLeftX(3) <= minRightY(21)? Yes
         maxLeftY(19) <= minRightX(8)? No
  
  maxLeftY > minRightX → left = 3

Iteration 2: left=3, right=5, partitionX=4
  partitionY = 6 - 4 = 2
  
  nums1: [1, 3, 8, 9] | [15]
  nums2: [7, 11] | [18, 19, 21, 25]
  
  maxLeftX = nums1[3] = 9
  minRightX = nums1[4] = 15
  maxLeftY = nums2[1] = 11
  minRightY = nums2[2] = 18
  
  Check: maxLeftX(9) <= minRightY(18)? Yes
         maxLeftY(11) <= minRightX(15)? Yes
  
  Found! Total is odd, return max(9, 11) = 11
```

### Visual Explanation

```
Step 1: Partition at middle of nums1
nums1: [1, 3] | [8, 9, 15]
nums2: [7, 11, 18, 19] | [21, 25]
       ↑
       19 > 8 (violation!)

Step 2: Move partition right in nums1
nums1: [1, 3, 8, 9] | [15]
nums2: [7, 11] | [18, 19, 21, 25]
       ↑
       11 ≤ 15 ✓ and 9 ≤ 18 ✓

Left partition: [1, 3, 8, 9, 7, 11]
Right partition: [15, 18, 19, 21, 25]

Median = max(left) = 11
```

### Edge Cases

```java
// Test cases
findMedianSortedArrays([1,3], [2]) → 2.0
findMedianSortedArrays([1,2], [3,4]) → 2.5
findMedianSortedArrays([], [1]) → 1.0
findMedianSortedArrays([2], []) → 2.0
findMedianSortedArrays([1], [2,3,4,5,6]) → 3.5
```

### Key Insights

1. **Why binary search on smaller array?**
   - Reduces time complexity to O(log(min(m,n)))
   - Ensures partitionY is always valid

2. **Partition formula**: `partitionY = (m + n + 1) / 2 - partitionX`
   - Ensures left half has same or one more element than right half
   - Works for both odd and even total lengths

3. **Boundary conditions**:
   - Use `Integer.MIN_VALUE` when partition is at start
   - Use `Integer.MAX_VALUE` when partition is at end

4. **Median calculation**:
   - Odd total: `max(maxLeftX, maxLeftY)`
   - Even total: `(max(maxLeftX, maxLeftY) + min(minRightX, minRightY)) / 2`

---

## Problem 2: Find Minimum in Rotated Sorted Array II (LeetCode 154)

**Difficulty**: Hard  
**Pattern**: Modified Binary Search with Duplicates

### Problem Statement
Suppose an array of length `n` sorted in ascending order is rotated between `1` and `n` times. Given the sorted rotated array `nums` that may contain duplicates, return the minimum element of this array.

You must decrease the overall operation steps as much as possible.

### Examples
```
Input: nums = [1,3,5]
Output: 1

Input: nums = [2,2,2,0,1]
Output: 0

Input: nums = [10,1,10,10,10]
Output: 1
```

### Approach

The challenge with duplicates is that we can't always determine which half contains the minimum:

```
Case 1: [3, 3, 1, 3]
        nums[mid] = 3, nums[right] = 3
        Can't determine which half has minimum

Case 2: [1, 3, 3, 3]
        nums[mid] = 3, nums[right] = 3
        Minimum could be on left

Solution: When nums[mid] == nums[right], decrement right by 1
```

### Solution
```java
public int findMin(int[] nums) {
    int left = 0, right = nums.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] > nums[right]) {
            // Minimum is in right half
            left = mid + 1;
        } else if (nums[mid] < nums[right]) {
            // Minimum is in left half (including mid)
            right = mid;
        } else {
            // nums[mid] == nums[right]
            // Can't determine which half, reduce search space by 1
            right--;
        }
    }
    
    return nums[left];
}
```

### Complexity Analysis
- **Time**: O(log n) average case, O(n) worst case (all duplicates)
- **Space**: O(1)

### Detailed Dry Run

```
Example 1: nums = [2,2,2,0,1]

Iteration 1: left=0, right=4, mid=2
  nums[2]=2, nums[4]=1
  nums[mid] > nums[right] → left=3

Iteration 2: left=3, right=4, mid=3
  nums[3]=0, nums[4]=1
  nums[mid] < nums[right] → right=3

Exit: left == right = 3, return nums[3] = 0
```

```
Example 2: nums = [10,1,10,10,10]

Iteration 1: left=0, right=4, mid=2
  nums[2]=10, nums[4]=10
  nums[mid] == nums[right] → right=3

Iteration 2: left=0, right=3, mid=1
  nums[1]=1, nums[3]=10
  nums[mid] < nums[right] → right=1

Iteration 3: left=0, right=1, mid=0
  nums[0]=10, nums[1]=1
  nums[mid] > nums[right] → left=1

Exit: left == right = 1, return nums[1] = 1
```

```
Example 3: nums = [3,3,1,3] (worst case with duplicates)

Iteration 1: left=0, right=3, mid=1
  nums[1]=3, nums[3]=3
  nums[mid] == nums[right] → right=2

Iteration 2: left=0, right=2, mid=1
  nums[1]=3, nums[2]=1
  nums[mid] > nums[right] → left=2

Exit: left == right = 2, return nums[2] = 1
```

### Visual Explanation

```
Array: [2, 2, 2, 0, 1]
        ↑     ↑     ↑
       left  mid  right

Step 1: nums[mid]=2 > nums[right]=1
        Minimum must be in right half
        [2, 2, 2, 0, 1]
                  ↑  ↑
                left right

Step 2: nums[mid]=0 < nums[right]=1
        Minimum is mid or left of mid
        [2, 2, 2, 0, 1]
                  ↑
                found
```

### Comparison with Non-Duplicate Version

| Aspect | Without Duplicates | With Duplicates |
|--------|-------------------|-----------------|
| Time Complexity | O(log n) guaranteed | O(log n) average, O(n) worst |
| Key Difference | Always can determine half | May need to reduce by 1 |
| Edge Case | None | All elements same |

### Edge Cases

```java
// Test cases
findMin([1,3,5]) → 1  // No rotation
findMin([2,2,2,0,1]) → 0  // Duplicates with rotation
findMin([10,1,10,10,10]) → 1  // Minimum at start
findMin([3,3,3,3,3]) → 3  // All same
findMin([1]) → 1  // Single element
findMin([2,2,2,2,2,2,2,2,1,2]) → 1  // Many duplicates
```

### Key Insights

1. **Why decrement right instead of left?**
   - We compare `nums[mid]` with `nums[right]`
   - When equal, we know `nums[right]` is not unique
   - Safe to exclude `nums[right]` from search

2. **Worst case scenario**:
   ```
   [3, 3, 3, 3, 3, 3, 1, 3]
   Must check almost every element → O(n)
   ```

3. **Optimization**:
   - Could check if array is already sorted: `nums[left] < nums[right]`
   - Early return if no rotation detected

4. **Alternative approach**:
   ```java
   // Could also compare with left
   if (nums[mid] == nums[left]) {
       left++;
   }
   ```

### Common Mistakes

1. **Infinite loop**: Forgetting to handle `nums[mid] == nums[right]`
2. **Wrong comparison**: Comparing with `left` instead of `right`
3. **Off-by-one**: Using `left <= right` instead of `left < right`

---

## Summary

| Problem | Pattern | Key Challenge | Time Complexity |
|---------|---------|---------------|-----------------|
| Median of Two Sorted Arrays | Partitioning | Find correct partition point | O(log(min(m,n))) |
| Find Min in Rotated Array II | Modified with Duplicates | Handle duplicate elements | O(log n) avg, O(n) worst |

### Problem-Solving Strategies

**Median of Two Sorted Arrays**:
1. Always binary search on smaller array
2. Calculate partition in second array based on first
3. Check if partition is valid (cross-comparison)
4. Adjust partition based on violation

**Find Min in Rotated Array II**:
1. Compare mid with right boundary
2. If equal, reduce search space by 1
3. Otherwise, use standard rotated array logic
4. Handle worst case of all duplicates

### Interview Tips

1. **Median Problem**:
   - Draw partition diagrams
   - Explain why we search on smaller array
   - Discuss edge cases (empty arrays, single elements)

2. **Rotated Array with Duplicates**:
   - Explain why duplicates make it harder
   - Discuss worst case time complexity
   - Mention optimization for sorted arrays

---

**Next**: [Quick Reference](Quick_Reference.md)
