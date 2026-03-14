# Binary Search - Theory & Concepts

## 📖 What is Binary Search?

Binary Search is a **divide-and-conquer** algorithm that efficiently searches for a target value in a **sorted** collection by repeatedly dividing the search space in half.

### Core Idea
Instead of checking every element (linear search O(n)), we eliminate half of the remaining elements with each comparison, achieving O(log n) time complexity.

### Real-World Analogy
**Dictionary Search**: When looking for a word in a dictionary:
1. Open to the middle page
2. If your word comes before, search the left half
3. If your word comes after, search the right half
4. Repeat until found

## 🎯 Fundamental Concepts

### 1. Monotonicity Requirement

Binary Search requires a **monotonic** property - the search space must be ordered in some way:
- **Sorted Array**: [1, 3, 5, 7, 9, 11]
- **Rotated Sorted**: [7, 9, 11, 1, 3, 5]
- **Peak Array**: [1, 3, 5, 7, 5, 3, 1]
- **Answer Space**: [feasible, feasible, ..., infeasible, infeasible]

### 2. Search Space Reduction

Each iteration reduces the search space by half:
```
Iteration 1: n elements
Iteration 2: n/2 elements
Iteration 3: n/4 elements
...
Iteration k: 1 element (when n/2^k = 1)
```

Therefore: **k = log₂(n)** → O(log n) time complexity

### 3. Three Key Variables

```java
int left = 0;           // Start of search space
int right = n - 1;      // End of search space
int mid = left + (right - left) / 2;  // Middle point
```

**Why `left + (right - left) / 2`?**
- Prevents integer overflow when `left + right > Integer.MAX_VALUE`
- Equivalent to `(left + right) / 2` but safer

## 🔧 Core Patterns & Templates

### Pattern 1: Standard Binary Search (Exact Match)

**Use Case**: Find exact target in sorted array

```java
public int binarySearch(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    
    while (left <= right) {  // Note: <=
        int mid = left + (right - left) / 2;
        
        if (nums[mid] == target) {
            return mid;  // Found
        } else if (nums[mid] < target) {
            left = mid + 1;  // Search right half
        } else {
            right = mid - 1;  // Search left half
        }
    }
    
    return -1;  // Not found
}
```

**Key Points**:
- Condition: `left <= right` (includes equality)
- Update: `left = mid + 1` or `right = mid - 1` (exclude mid)
- Returns: Index or -1

### Pattern 2: Finding Left Boundary (First Occurrence)

**Use Case**: Find first position where condition is true

```java
public int findLeftBoundary(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] >= target) {  // Found candidate
            result = mid;
            right = mid - 1;  // Continue searching left
        } else {
            left = mid + 1;
        }
    }
    
    return result;
}
```

**Alternative Template** (more elegant):
```java
public int findLeftBoundary(int[] nums, int target) {
    int left = 0, right = nums.length;
    
    while (left < right) {  // Note: <
        int mid = left + (right - left) / 2;
        
        if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid;  // Don't exclude mid
        }
    }
    
    return left;  // left == right
}
```

### Pattern 3: Finding Right Boundary (Last Occurrence)

**Use Case**: Find last position where condition is true

```java
public int findRightBoundary(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] <= target) {  // Found candidate
            result = mid;
            left = mid + 1;  // Continue searching right
        } else {
            right = mid - 1;
        }
    }
    
    return result;
}
```

### Pattern 4: Binary Search on Answer Space

**Use Case**: Optimization problems (minimize maximum, maximize minimum)

```java
public int binarySearchOnAnswer(int[] nums, int target) {
    int left = minPossibleAnswer;
    int right = maxPossibleAnswer;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (isFeasible(nums, mid, target)) {
            result = mid;  // Found feasible answer
            right = mid - 1;  // Try to minimize (or left = mid + 1 to maximize)
        } else {
            left = mid + 1;
        }
    }
    
    return result;
}

private boolean isFeasible(int[] nums, int capacity, int target) {
    // Check if 'capacity' satisfies the constraints
    // This is problem-specific
}
```

**Examples**:
- Koko Eating Bananas: Minimize eating speed
- Capacity To Ship Packages: Minimize ship capacity
- Split Array Largest Sum: Minimize largest subarray sum

### Pattern 5: Binary Search in 2D Matrix

**Use Case**: Search in row-wise and column-wise sorted matrix

```java
public boolean searchMatrix(int[][] matrix, int target) {
    if (matrix.length == 0) return false;
    
    int m = matrix.length, n = matrix[0].length;
    int left = 0, right = m * n - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        int midValue = matrix[mid / n][mid % n];  // Convert 1D to 2D
        
        if (midValue == target) {
            return true;
        } else if (midValue < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return false;
}
```

**Key Insight**: Treat 2D matrix as 1D array
- Index conversion: `row = mid / n`, `col = mid % n`

## 🎨 Visual Examples

### Example 1: Standard Binary Search

```
Array: [1, 3, 5, 7, 9, 11, 13, 15]
Target: 7

Step 1: left=0, right=7, mid=3
        [1, 3, 5, 7, 9, 11, 13, 15]
                    ↑
        nums[3] = 7 == target → Found!
```

### Example 2: Finding Left Boundary

```
Array: [1, 2, 2, 2, 3, 4, 5]
Target: 2 (find first occurrence)

Step 1: left=0, right=6, mid=3
        [1, 2, 2, 2, 3, 4, 5]
                    ↑
        nums[3] = 2 >= target → result=3, right=2

Step 2: left=0, right=2, mid=1
        [1, 2, 2, 2, 3, 4, 5]
            ↑
        nums[1] = 2 >= target → result=1, right=0

Step 3: left=0, right=0, mid=0
        [1, 2, 2, 2, 3, 4, 5]
         ↑
        nums[0] = 1 < target → left=1

Exit: left > right, return result=1
```

### Example 3: Binary Search on Answer Space

```
Problem: Koko Eating Bananas
Piles: [3, 6, 7, 11], H = 8 hours
Find minimum eating speed k

Answer Space: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
              [F, F, F, T, T, T, T, T, T, T,  T]
              F = infeasible, T = feasible

Binary Search on [1, 11]:
Step 1: mid=6 → Can finish in 8 hours? Yes → result=6, right=5
Step 2: mid=3 → Can finish in 8 hours? No → left=4
Step 3: mid=4 → Can finish in 8 hours? Yes → result=4, right=3
Exit: left > right, return result=4
```

## ⚠️ Common Pitfalls & Edge Cases

### 1. Integer Overflow
```java
// ❌ Wrong: Can overflow
int mid = (left + right) / 2;

// ✅ Correct: Safe from overflow
int mid = left + (right - left) / 2;
```

### 2. Infinite Loop
```java
// ❌ Wrong: Infinite loop when left=0, right=1
while (left < right) {
    int mid = (left + right) / 2;
    if (condition) {
        left = mid;  // Problem: left never increases
    } else {
        right = mid - 1;
    }
}

// ✅ Correct: Ensure progress
while (left < right) {
    int mid = left + (right - left) / 2;
    if (condition) {
        left = mid + 1;  // Always make progress
    } else {
        right = mid;
    }
}
```

### 3. Off-by-One Errors
```java
// Choosing between left <= right vs left < right
// Choosing between mid+1 vs mid, mid-1 vs mid

// Rule of thumb:
// - Use left <= right when you want to check every element
// - Use left < right when finding boundaries
```

### 4. Edge Cases to Test
- Empty array: `[]`
- Single element: `[5]`
- Two elements: `[3, 5]`
- Target at boundaries: first or last element
- Target not in array
- All elements same: `[2, 2, 2, 2]`
- Duplicates: `[1, 2, 2, 2, 3]`

## 📊 Complexity Analysis

### Time Complexity

| Operation | Complexity | Explanation |
|-----------|------------|-------------|
| Standard Search | O(log n) | Halves search space each iteration |
| Finding Boundaries | O(log n) | Same as standard search |
| 2D Matrix Search | O(log(m×n)) | Treats matrix as 1D array |
| Rotated Array | O(log n) | At most 2 binary searches |
| Answer Space | O(n log k) | Binary search (log k) × validation (O(n)) |

### Space Complexity

| Implementation | Complexity | Explanation |
|----------------|------------|-------------|
| Iterative | O(1) | Only uses constant variables |
| Recursive | O(log n) | Call stack depth |

## 🎯 When NOT to Use Binary Search

Binary Search is NOT suitable when:
- ❌ Array is unsorted (unless you can sort it first)
- ❌ No monotonic property exists
- ❌ Random access is expensive (e.g., linked lists)
- ❌ Small dataset (linear search may be faster due to overhead)
- ❌ Frequent insertions/deletions (maintaining sorted order is costly)

## 💡 Pro Tips

### 1. Template Selection Strategy
```
Exact match → Use left <= right
Finding boundaries → Use left < right
Optimization → Use left <= right with result variable
```

### 2. Debugging Technique
```java
// Add logging to understand behavior
while (left <= right) {
    int mid = left + (right - left) / 2;
    System.out.println("left=" + left + ", right=" + right + ", mid=" + mid);
    // ... rest of logic
}
```

### 3. Invariant Maintenance
Always maintain the invariant:
- Everything to the left of `left` doesn't satisfy condition
- Everything to the right of `right` doesn't satisfy condition
- Answer (if exists) is in `[left, right]`

### 4. Testing Strategy
Test with these cases in order:
1. Empty array
2. Single element (target found)
3. Single element (target not found)
4. Two elements (all combinations)
5. Normal case
6. Target at boundaries
7. Duplicates

## 🔗 Related Algorithms

- **Ternary Search**: For unimodal functions (single peak/valley)
- **Exponential Search**: For unbounded/infinite arrays
- **Interpolation Search**: For uniformly distributed data
- **Jump Search**: Alternative for sorted arrays

---

**Next**: [Pattern Recognition Guide](02_Pattern_Recognition.md)
