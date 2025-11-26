# Binary Search

## Overview
Binary Search is an efficient algorithm for finding a target value within a sorted array by repeatedly dividing the search interval in half.

## Algorithm

1. **Initialize**: Set left = 0, right = array.length - 1
2. **Compare**: Calculate mid = left + (right - left) / 2
3. **Check**:
   - If arr[mid] == target → Found, return mid
   - If arr[mid] < target → Search right half (left = mid + 1)
   - If arr[mid] > target → Search left half (right = mid - 1)
4. **Repeat**: Continue until left > right
5. **Not Found**: Return -1

## Implementation

### Iterative Approach

```java
public class BinarySearch {
    
    public static int binarySearch(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                return mid;
            }
            
            if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return -1; // Not found
    }
    
    public static void main(String[] args) {
        int[] arr = {2, 5, 8, 12, 16, 23, 38, 45, 56, 67, 78};
        int target = 23;
        
        int result = binarySearch(arr, target);
        if (result != -1) {
            System.out.println("Element found at index: " + result);
        } else {
            System.out.println("Element not found");
        }
    }
}
```

### Recursive Approach

```java
public class BinarySearchRecursive {
    
    public static int binarySearch(int[] arr, int target, int left, int right) {
        if (left > right) {
            return -1;
        }
        
        int mid = left + (right - left) / 2;
        
        if (arr[mid] == target) {
            return mid;
        }
        
        if (arr[mid] < target) {
            return binarySearch(arr, target, mid + 1, right);
        } else {
            return binarySearch(arr, target, left, mid - 1);
        }
    }
    
    public static void main(String[] args) {
        int[] arr = {2, 5, 8, 12, 16, 23, 38, 45, 56, 67, 78};
        int target = 23;
        
        int result = binarySearch(arr, target, 0, arr.length - 1);
        System.out.println(result != -1 ? "Found at: " + result : "Not found");
    }
}
```

## Dry Run Example

**Input**: `arr = [2, 5, 8, 12, 16, 23, 38, 45, 56, 67, 78]`, `target = 23`

```
Initial: left=0, right=10

Iteration 1:
  mid = 0 + (10-0)/2 = 5
  arr[5] = 23
  23 == 23 → FOUND at index 5 ✓

Result: 5
```

### Another Example (Target Not Found)

**Input**: `arr = [2, 5, 8, 12, 16, 23, 38, 45, 56, 67, 78]`, `target = 20`

```
Initial: left=0, right=10

Iteration 1:
  mid = 0 + (10-0)/2 = 5
  arr[5] = 23
  20 < 23 → Search left half
  right = 5 - 1 = 4

Iteration 2:
  left=0, right=4
  mid = 0 + (4-0)/2 = 2
  arr[2] = 8
  20 > 8 → Search right half
  left = 2 + 1 = 3

Iteration 3:
  left=3, right=4
  mid = 3 + (4-3)/2 = 3
  arr[3] = 12
  20 > 12 → Search right half
  left = 3 + 1 = 4

Iteration 4:
  left=4, right=4
  mid = 4 + (4-4)/2 = 4
  arr[4] = 16
  20 > 16 → Search right half
  left = 4 + 1 = 5

Iteration 5:
  left=5, right=4
  left > right → NOT FOUND

Result: -1
```

## Edge Test Cases

```java
// Test Case 1: Empty array
int[] arr1 = {};
int result1 = binarySearch(arr1, 5);
// Expected: -1

// Test Case 2: Single element (found)
int[] arr2 = {5};
int result2 = binarySearch(arr2, 5);
// Expected: 0

// Test Case 3: Single element (not found)
int[] arr3 = {5};
int result3 = binarySearch(arr3, 3);
// Expected: -1

// Test Case 4: Target at first position
int[] arr4 = {1, 2, 3, 4, 5};
int result4 = binarySearch(arr4, 1);
// Expected: 0

// Test Case 5: Target at last position
int[] arr5 = {1, 2, 3, 4, 5};
int result5 = binarySearch(arr5, 5);
// Expected: 4

// Test Case 6: Target in middle
int[] arr6 = {1, 2, 3, 4, 5};
int result6 = binarySearch(arr6, 3);
// Expected: 2

// Test Case 7: Target smaller than all elements
int[] arr7 = {10, 20, 30, 40, 50};
int result7 = binarySearch(arr7, 5);
// Expected: -1

// Test Case 8: Target larger than all elements
int[] arr8 = {10, 20, 30, 40, 50};
int result8 = binarySearch(arr8, 100);
// Expected: -1

// Test Case 9: Duplicates (returns any valid index)
int[] arr9 = {1, 2, 3, 3, 3, 4, 5};
int result9 = binarySearch(arr9, 3);
// Expected: 2, 3, or 4 (any index with value 3)

// Test Case 10: Large array
int[] arr10 = new int[1000000];
for (int i = 0; i < arr10.length; i++) arr10[i] = i * 2;
int result10 = binarySearch(arr10, 1000);
// Expected: 500
```

## Time Complexity

### Best Case: O(1)
- Target is at the middle position in first comparison
- Example: `arr = [1, 2, 3, 4, 5]`, `target = 3`

### Average Case: O(log n)
- Search space halves with each iteration
- Takes log₂(n) comparisons on average

### Worst Case: O(log n)
- Target is at first/last position or not present
- Maximum log₂(n) + 1 comparisons

### Mathematical Proof:
```
After 1st iteration: n/2 elements remain
After 2nd iteration: n/4 elements remain
After 3rd iteration: n/8 elements remain
...
After kth iteration: n/2^k elements remain

When n/2^k = 1:
n = 2^k
k = log₂(n)

Therefore, T(n) = O(log n)
```

### Example:
- Array size = 1,000,000 elements
- Maximum comparisons = log₂(1,000,000) ≈ 20 comparisons

## Space Complexity

### Iterative: O(1)
- Only uses constant extra space (left, right, mid variables)
- No recursion stack

### Recursive: O(log n)
- Recursion depth = log n
- Each call stores variables on call stack

## Real-World Use Cases

### 1. **Database Indexing**
```java
// Finding records in sorted database index
int findUserById(int[] userIds, int targetId) {
    return binarySearch(userIds, targetId);
}
```

### 2. **Dictionary/Spell Checker**
```java
// Check if word exists in sorted dictionary
boolean isValidWord(String[] dictionary, String word) {
    return binarySearchString(dictionary, word) != -1;
}
```

### 3. **Version Control Systems**
```java
// Find first bad commit in sorted commit history
int findFirstBadCommit(int[] commits) {
    // Binary search to find transition point
}
```

### 4. **Finding Square Root**
```java
// Find integer square root using binary search
int sqrt(int x) {
    int left = 0, right = x;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        long square = (long) mid * mid;
        if (square == x) return mid;
        if (square < x) left = mid + 1;
        else right = mid - 1;
    }
    return right;
}
```

### 5. **Search in Rotated Sorted Array**
```java
// [4,5,6,7,0,1,2] - rotated at pivot
int searchRotated(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        
        // Left half is sorted
        if (arr[left] <= arr[mid]) {
            if (target >= arr[left] && target < arr[mid])
                right = mid - 1;
            else
                left = mid + 1;
        } else { // Right half is sorted
            if (target > arr[mid] && target <= arr[right])
                left = mid + 1;
            else
                right = mid - 1;
        }
    }
    return -1;
}
```

### 6. **Finding Peak Element**
```java
// Find any peak element (greater than neighbors)
int findPeak(int[] arr) {
    int left = 0, right = arr.length - 1;
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] < arr[mid + 1])
            left = mid + 1;
        else
            right = mid;
    }
    return left;
}
```

### 7. **First and Last Position in Sorted Array**
```java
// Find range of target value
int[] searchRange(int[] arr, int target) {
    int first = findFirst(arr, target);
    int last = findLast(arr, target);
    return new int[]{first, last};
}

int findFirst(int[] arr, int target) {
    int left = 0, right = arr.length - 1, result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) {
            result = mid;
            right = mid - 1; // Continue searching left
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    return result;
}
```

### 8. **Capacity To Ship Packages Within D Days**
```java
// Minimize ship capacity to deliver all packages in D days
int shipWithinDays(int[] weights, int days) {
    int left = Arrays.stream(weights).max().getAsInt();
    int right = Arrays.stream(weights).sum();
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (canShip(weights, days, mid)) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }
    return left;
}
```

### 9. **Search in 2D Matrix**
```java
// Search in row-wise and column-wise sorted matrix
boolean searchMatrix(int[][] matrix, int target) {
    int m = matrix.length, n = matrix[0].length;
    int left = 0, right = m * n - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        int midValue = matrix[mid / n][mid % n];
        
        if (midValue == target) return true;
        if (midValue < target) left = mid + 1;
        else right = mid - 1;
    }
    return false;
}
```

### 10. **Finding Minimum in Rotated Sorted Array**
```java
// [4,5,6,7,0,1,2] → minimum is 0
int findMin(int[] arr) {
    int left = 0, right = arr.length - 1;
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] > arr[right])
            left = mid + 1;
        else
            right = mid;
    }
    return arr[left];
}
```

### 11. **Kth Smallest Element in Sorted Matrix**
```java
// Find kth smallest in n×n matrix where each row/col is sorted
int kthSmallest(int[][] matrix, int k) {
    int n = matrix.length;
    int left = matrix[0][0], right = matrix[n-1][n-1];
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        int count = countLessEqual(matrix, mid);
        if (count < k)
            left = mid + 1;
        else
            right = mid;
    }
    return left;
}
```

### 12. **Time-Based Key-Value Store**
```java
// Get value at timestamp using binary search
class TimeMap {
    Map<String, List<Pair<Integer, String>>> map;
    
    String get(String key, int timestamp) {
        List<Pair<Integer, String>> list = map.get(key);
        // Binary search for largest timestamp <= given timestamp
    }
}
```

## Advantages

1. **Efficient**: O(log n) vs O(n) for linear search
2. **Predictable**: Consistent performance
3. **Simple**: Easy to implement and understand
4. **Scalable**: Works well with large datasets

## Disadvantages

1. **Requires Sorted Data**: Array must be sorted first
2. **Array Only**: Doesn't work well with linked lists
3. **Static Data**: Inefficient if data changes frequently

## When to Use

✅ **Use Binary Search When:**
- Data is sorted or can be sorted
- Need fast lookups (O(log n))
- Working with large datasets
- Random access is available (arrays)
- Data is relatively static

❌ **Don't Use Binary Search When:**
- Data is unsorted and sorting is expensive
- Working with linked lists
- Data changes frequently (insertions/deletions)
- Small datasets (linear search is simpler)

## Comparison with Other Search Algorithms

| Algorithm       | Time (Avg) | Time (Worst) | Space | Requires Sorted |
|----------------|-----------|-------------|-------|-----------------|
| Binary Search  | O(log n)  | O(log n)    | O(1)  | Yes             |
| Linear Search  | O(n)      | O(n)        | O(1)  | No              |
| Jump Search    | O(√n)     | O(√n)       | O(1)  | Yes             |
| Interpolation  | O(log log n)| O(n)      | O(1)  | Yes (uniform)   |
| Hash Table     | O(1)      | O(n)        | O(n)  | No              |

## Common Pitfalls

### 1. Integer Overflow
```java
// ❌ Wrong: Can overflow for large values
int mid = (left + right) / 2;

// ✓ Correct: Prevents overflow
int mid = left + (right - left) / 2;
```

### 2. Infinite Loop
```java
// ❌ Wrong: Can cause infinite loop
while (left < right) {
    int mid = (left + right) / 2;
    if (arr[mid] < target) left = mid; // Should be mid + 1
}

// ✓ Correct
while (left < right) {
    int mid = left + (right - left) / 2;
    if (arr[mid] < target) left = mid + 1;
    else right = mid;
}
```

### 3. Off-by-One Errors
```java
// Be careful with <= vs < in while condition
// Be careful with mid + 1 vs mid - 1
```

## Tips for Interviews

1. **Clarify Requirements**: Ask if array is sorted, contains duplicates
2. **Handle Edge Cases**: Empty array, single element, target not found
3. **Explain Approach**: Walk through the algorithm before coding
4. **Test with Examples**: Use small examples to verify logic
5. **Consider Variations**: Be ready for rotated arrays, 2D matrices, etc.
