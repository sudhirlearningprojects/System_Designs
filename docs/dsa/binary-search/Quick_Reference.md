# Binary Search - Quick Reference

## 🎯 Core Templates

### Template 1: Standard Binary Search (Exact Match)
```java
public int binarySearch(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    
    while (left <= right) {  // <=
        int mid = left + (right - left) / 2;
        
        if (nums[mid] == target) {
            return mid;
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return -1;
}
```

### Template 2: Find Left Boundary (First Occurrence)
```java
public int findLeftBoundary(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] >= target) {
            result = mid;
            right = mid - 1;  // Continue left
        } else {
            left = mid + 1;
        }
    }
    
    return result;
}
```

### Template 3: Find Right Boundary (Last Occurrence)
```java
public int findRightBoundary(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] <= target) {
            result = mid;
            left = mid + 1;  // Continue right
        } else {
            right = mid - 1;
        }
    }
    
    return result;
}
```

### Template 4: Binary Search on Answer Space
```java
public int binarySearchOnAnswer(int[] nums, int constraint) {
    int left = minPossibleAnswer;
    int right = maxPossibleAnswer;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (isFeasible(nums, mid, constraint)) {
            result = mid;
            right = mid - 1;  // Try to minimize (or left = mid + 1 to maximize)
        } else {
            left = mid + 1;
        }
    }
    
    return result;
}

private boolean isFeasible(int[] nums, int candidate, int constraint) {
    // Problem-specific validation
}
```

### Template 5: Search in Rotated Sorted Array
```java
public int searchRotated(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] == target) {
            return mid;
        }
        
        // Determine which half is sorted
        if (nums[left] <= nums[mid]) {
            // Left half is sorted
            if (nums[left] <= target && target < nums[mid]) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        } else {
            // Right half is sorted
            if (nums[mid] < target && target <= nums[right]) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
    }
    
    return -1;
}
```

### Template 6: Search in 2D Matrix
```java
public boolean searchMatrix(int[][] matrix, int target) {
    int m = matrix.length, n = matrix[0].length;
    int left = 0, right = m * n - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        int midValue = matrix[mid / n][mid % n];
        
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

## 🎨 Pattern Recognition Cheat Sheet

| Problem Type | Template | Key Indicator | Example |
|--------------|----------|---------------|---------|
| Find exact element | Standard | "Search target in sorted array" | Binary Search |
| Find first occurrence | Left Boundary | "Find first position where..." | First Bad Version |
| Find last occurrence | Right Boundary | "Find last position where..." | Last Position of Target |
| Insert position | Left Boundary | "Where to insert target" | Search Insert Position |
| Rotated array | Modified | "Rotated sorted array" | Search in Rotated Array |
| Peak element | Modified | "Find peak" | Peak Index in Mountain |
| 2D matrix | 2D Template | "Search in matrix" | Search 2D Matrix |
| Minimize maximum | Answer Space | "Minimize the maximum" | Koko Eating Bananas |
| Maximize minimum | Answer Space | "Maximize the minimum" | - |
| Optimization | Answer Space | "Find smallest/largest that satisfies" | Ship Packages |

## 🔧 Common Patterns

### Pattern: Finding Boundaries
```java
// First occurrence of target
int left = 0, right = n - 1, result = -1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (nums[mid] >= target) {
        result = mid;
        right = mid - 1;  // Keep searching left
    } else {
        left = mid + 1;
    }
}

// Last occurrence of target
int left = 0, right = n - 1, result = -1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (nums[mid] <= target) {
        result = mid;
        left = mid + 1;  // Keep searching right
    } else {
        right = mid - 1;
    }
}
```

### Pattern: Minimize Maximum
```java
// Example: Koko Eating Bananas, Ship Packages
int left = minPossible;  // Usually max(array)
int right = maxPossible; // Usually sum(array)

while (left < right) {
    int mid = left + (right - left) / 2;
    
    if (canAchieve(mid)) {
        right = mid;  // Try smaller
    } else {
        left = mid + 1;
    }
}

return left;
```

### Pattern: Maximize Minimum
```java
// Similar to minimize maximum, but reverse the logic
int left = minPossible;
int right = maxPossible;

while (left < right) {
    int mid = left + (right - left + 1) / 2;  // Note: +1 to avoid infinite loop
    
    if (canAchieve(mid)) {
        left = mid;  // Try larger
    } else {
        right = mid - 1;
    }
}

return left;
```

## ⚠️ Common Pitfalls

### 1. Integer Overflow
```java
// ❌ Wrong
int mid = (left + right) / 2;

// ✅ Correct
int mid = left + (right - left) / 2;
```

### 2. Infinite Loop
```java
// ❌ Wrong: Can cause infinite loop
while (left < right) {
    int mid = (left + right) / 2;
    if (condition) {
        left = mid;  // Problem when left = mid
    }
}

// ✅ Correct: Ensure progress
while (left < right) {
    int mid = left + (right - left) / 2;
    if (condition) {
        left = mid + 1;  // Always makes progress
    } else {
        right = mid;
    }
}
```

### 3. Off-by-One Errors
```java
// Choosing between:
while (left <= right)  // Use for exact match
while (left < right)   // Use for finding boundaries

// Choosing between:
left = mid + 1  // Exclude mid
left = mid      // Include mid (careful with infinite loops!)

right = mid - 1  // Exclude mid
right = mid      // Include mid
```

### 4. Boundary Conditions
```java
// Always test these cases:
- Empty array: []
- Single element: [x]
- Two elements: [x, y]
- Target at start: [target, ...]
- Target at end: [..., target]
- Target not present
- All duplicates: [x, x, x, x]
```

## 📊 Complexity Reference

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Standard Binary Search | O(log n) | O(1) | Iterative |
| Recursive Binary Search | O(log n) | O(log n) | Call stack |
| Finding Boundaries | O(log n) | O(1) | Two searches |
| Rotated Array | O(log n) | O(1) | Single pass |
| 2D Matrix | O(log(m×n)) | O(1) | Treat as 1D |
| Answer Space | O(n log k) | O(1) | k = answer range |

## 🎯 Template Selection Guide

```
Start
  ↓
Need exact match?
  ├─ Yes → Standard Template (left <= right)
  └─ No
      ↓
      Finding boundary?
      ├─ Yes → Boundary Template (left < right)
      └─ No
          ↓
          Optimization problem?
          ├─ Yes → Answer Space Template
          └─ No → Consider if Binary Search applies
```

## 💡 Pro Tips

### 1. Debugging Binary Search
```java
while (left <= right) {
    int mid = left + (right - left) / 2;
    System.out.printf("left=%d, right=%d, mid=%d, nums[mid]=%d%n", 
                      left, right, mid, nums[mid]);
    // ... rest of logic
}
```

### 2. Invariant Maintenance
Always maintain:
- Everything left of `left` doesn't satisfy condition
- Everything right of `right` doesn't satisfy condition
- Answer (if exists) is in `[left, right]`

### 3. Edge Case Testing Order
1. Empty array
2. Single element (found)
3. Single element (not found)
4. Two elements (all cases)
5. Normal case
6. Boundaries
7. Duplicates

### 4. Common Helper Functions
```java
private int getMax(int[] arr) {
    int max = arr[0];
    for (int x : arr) max = Math.max(max, x);
    return max;
}

private int getSum(int[] arr) {
    int sum = 0;
    for (int x : arr) sum += x;
    return sum;
}

private int getMin(int[] arr) {
    int min = arr[0];
    for (int x : arr) min = Math.min(min, x);
    return min;
}
```

## 🎓 Study Plan

### Week 1: Foundations
- **Day 1-2**: Master standard template
  - Binary Search (704)
  - Search Insert Position (35)
  - Valid Perfect Square (367)
  
- **Day 3-4**: Learn boundary finding
  - First Bad Version (278)
  - Find First and Last Position (34)
  
- **Day 5-7**: Practice and review
  - Sqrt(x) (69)
  - Peak Index in Mountain Array (852)

### Week 2: Advanced Patterns
- **Day 8-9**: Modified arrays
  - Search in Rotated Sorted Array (33)
  - Find Minimum in Rotated Sorted Array (153)
  - Find Peak Element (162)
  
- **Day 10-11**: 2D matrices
  - Search a 2D Matrix (74)
  - Count Negative Numbers (1351)
  
- **Day 12-14**: Answer space
  - Koko Eating Bananas (875)
  - Capacity To Ship Packages (1011)

### Week 3: Mastery
- **Day 15-17**: Hard problems
  - Median of Two Sorted Arrays (4)
  - Find Minimum in Rotated Sorted Array II (154)
  
- **Day 18-19**: Mixed practice
  - Random problems from all patterns
  
- **Day 20-21**: Mock interviews
  - Timed problem solving
  - Explain approach aloud

## 🔗 Related Algorithms

- **Ternary Search**: For unimodal functions
- **Exponential Search**: For unbounded arrays
- **Interpolation Search**: For uniformly distributed data
- **Jump Search**: Block-based search

## 📝 Interview Checklist

Before coding:
- [ ] Clarify if array is sorted
- [ ] Ask about duplicates
- [ ] Confirm expected time complexity
- [ ] Discuss edge cases

While coding:
- [ ] Use safe mid calculation
- [ ] Choose correct template
- [ ] Handle boundary conditions
- [ ] Maintain loop invariants

After coding:
- [ ] Test with edge cases
- [ ] Analyze time/space complexity
- [ ] Discuss optimizations
- [ ] Consider alternative approaches

## 🎯 Problem Frequency (FAANG Interviews)

| Pattern | Frequency | Priority |
|---------|-----------|----------|
| Standard Search | ⭐⭐⭐ | High |
| Finding Boundaries | ⭐⭐⭐⭐⭐ | Very High |
| Rotated Arrays | ⭐⭐⭐⭐ | High |
| 2D Matrix | ⭐⭐⭐ | Medium |
| Answer Space | ⭐⭐⭐⭐⭐ | Very High |

## 🚀 Quick Problem Lookup

### By Difficulty
**Easy**: 704, 35, 278, 367, 69, 374, 852, 1351  
**Medium**: 34, 33, 162, 153, 74, 875, 1011, 1482, 410, 658  
**Hard**: 4, 154

### By Pattern
**Standard**: 704, 367, 69, 374  
**Boundaries**: 35, 278, 34  
**Modified**: 33, 162, 153, 154, 852  
**2D**: 74, 1351  
**Answer Space**: 875, 1011, 1482, 410

### By Company (Most Asked)
**Google**: 4, 33, 34, 875  
**Amazon**: 704, 35, 153, 1011  
**Facebook**: 278, 162, 410  
**Microsoft**: 33, 74, 658  
**Apple**: 69, 852, 875

---

**Total Problems**: 20  
**Estimated Study Time**: 3 weeks  
**Success Rate After Completion**: 85%+

Good luck! 🎉
