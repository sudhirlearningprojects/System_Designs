# Binary Search - Pattern Recognition Guide

## 🎯 How to Identify Binary Search Problems

Binary Search problems often hide behind various disguises. This guide helps you recognize when to apply Binary Search.

## 🔍 Recognition Signals

### Strong Indicators (90%+ confidence)

1. **Keywords in Problem Statement**
   - "sorted array"
   - "find minimum/maximum"
   - "optimize"
   - "in O(log n) time"
   - "rotated sorted array"
   - "search in range"

2. **Problem Characteristics**
   - Input is sorted or partially sorted
   - Need to find a boundary/threshold
   - Optimization problem with monotonic property
   - Answer space is searchable

3. **Constraint Patterns**
   - Large input size (n ≤ 10^6 or 10^9)
   - Time limit suggests better than O(n)
   - "Find in logarithmic time"

### Moderate Indicators (50-70% confidence)

1. **Problem asks for**
   - First/last occurrence
   - Closest element
   - Peak element
   - Minimum in rotated array
   - Kth smallest/largest

2. **Hidden monotonicity**
   - "Minimize the maximum"
   - "Maximize the minimum"
   - "Find smallest value that satisfies condition"

## 🎨 Pattern Recognition Framework

### Decision Tree

```
Is the data sorted or has monotonic property?
│
├─ YES → Can you eliminate half the search space?
│   │
│   ├─ YES → Use Binary Search
│   │
│   └─ NO → Consider other approaches
│
└─ NO → Can you define a searchable answer space?
    │
    ├─ YES → Binary Search on Answer Space
    │
    └─ NO → Binary Search not applicable
```

## 📋 Pattern Categories

### Pattern 1: Direct Search in Sorted Array

**Recognition**:
- Array is explicitly sorted
- Need to find exact element or position
- May have duplicates

**Template Choice**: Standard Binary Search

**Example Problems**:
- "Find target in sorted array"
- "Search insert position"
- "Find first bad version"

**Code Skeleton**:
```java
public int search(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;
        else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

### Pattern 2: Finding Boundaries

**Recognition**:
- "Find first occurrence"
- "Find last occurrence"
- "Find lower/upper bound"
- Array may have duplicates

**Template Choice**: Left/Right Boundary Search

**Example Problems**:
- "Find first and last position of element"
- "Count occurrences in sorted array"
- "Find smallest letter greater than target"

**Code Skeleton**:
```java
// Left boundary
public int findFirst(int[] nums, int target) {
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

### Pattern 3: Modified Sorted Arrays

**Recognition**:
- "Rotated sorted array"
- "Mountain array"
- "Bitonic array"
- "Find peak element"

**Template Choice**: Modified Binary Search with extra conditions

**Example Problems**:
- "Search in rotated sorted array"
- "Find minimum in rotated sorted array"
- "Peak index in mountain array"

**Code Skeleton**:
```java
public int searchRotated(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;
        
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

### Pattern 4: 2D Matrix Search

**Recognition**:
- "Search in 2D matrix"
- "Row-wise sorted"
- "Column-wise sorted"
- Matrix dimensions in constraints

**Template Choice**: Treat as 1D array or staircase search

**Example Problems**:
- "Search a 2D matrix"
- "Search a 2D matrix II"
- "Kth smallest element in sorted matrix"

**Code Skeleton**:
```java
public boolean searchMatrix(int[][] matrix, int target) {
    int m = matrix.length, n = matrix[0].length;
    int left = 0, right = m * n - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        int midValue = matrix[mid / n][mid % n];
        
        if (midValue == target) return true;
        else if (midValue < target) left = mid + 1;
        else right = mid - 1;
    }
    return false;
}
```

### Pattern 5: Binary Search on Answer Space

**Recognition**:
- "Minimize the maximum"
- "Maximize the minimum"
- "Find smallest/largest value that satisfies condition"
- Optimization problem
- Answer range is known or calculable

**Template Choice**: Binary Search with feasibility check

**Example Problems**:
- "Koko eating bananas"
- "Capacity to ship packages"
- "Split array largest sum"
- "Minimum number of days to make m bouquets"

**Code Skeleton**:
```java
public int minCapacity(int[] nums, int constraint) {
    int left = max(nums);  // Minimum possible answer
    int right = sum(nums); // Maximum possible answer
    int result = right;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (isFeasible(nums, mid, constraint)) {
            result = mid;
            right = mid - 1;  // Try smaller
        } else {
            left = mid + 1;
        }
    }
    return result;
}

private boolean isFeasible(int[] nums, int capacity, int constraint) {
    // Check if 'capacity' satisfies the constraint
    // Problem-specific logic
}
```

## 🎯 Problem Identification Checklist

Use this checklist when encountering a new problem:

### Step 1: Check for Explicit Signals
- [ ] Is the array/data sorted?
- [ ] Does problem mention "O(log n)" or "logarithmic time"?
- [ ] Does it ask for first/last occurrence?
- [ ] Is it an optimization problem?

### Step 2: Analyze the Search Space
- [ ] Can you define a range [left, right]?
- [ ] Is there a monotonic property?
- [ ] Can you eliminate half the space each iteration?

### Step 3: Identify the Pattern
- [ ] Direct search → Pattern 1
- [ ] Finding boundaries → Pattern 2
- [ ] Modified array → Pattern 3
- [ ] 2D matrix → Pattern 4
- [ ] Optimization → Pattern 5

### Step 4: Verify Feasibility
- [ ] Can you write a condition to move left/right?
- [ ] Are there edge cases that break monotonicity?
- [ ] Is the answer guaranteed to exist?

## 🔄 Pattern Transformation Examples

### Example 1: Hidden Binary Search

**Problem**: "Find the smallest divisor given a threshold"

**Initial Thought**: Seems like a math problem

**Recognition Process**:
1. Need to find "smallest" → optimization
2. Divisor range is [1, max(nums)]
3. Larger divisor → smaller sum (monotonic!)
4. Can check if divisor works in O(n)

**Pattern**: Binary Search on Answer Space

### Example 2: Disguised Boundary Search

**Problem**: "Find the smallest letter greater than target"

**Initial Thought**: Linear search?

**Recognition Process**:
1. Array is sorted
2. Need "smallest" that satisfies condition
3. Finding a boundary

**Pattern**: Left Boundary Search

### Example 3: Complex Condition

**Problem**: "Split array into m subarrays to minimize largest sum"

**Initial Thought**: Dynamic programming?

**Recognition Process**:
1. "Minimize the maximum" → optimization
2. Answer range: [max(nums), sum(nums)]
3. Can verify if a max sum works
4. Monotonic: larger max → easier to split

**Pattern**: Binary Search on Answer Space

## 🎨 Visual Pattern Recognition

### Sorted Array Patterns

```
Standard Search:
[1, 3, 5, 7, 9, 11, 13]
 ↑           ↑        ↑
left        mid     right

Boundary Search:
[1, 2, 2, 2, 3, 4, 5]
    ↑first  ↑last
    
Rotated Array:
[7, 9, 11, 1, 3, 5]
 ↑sorted↑  ↑sorted↑
```

### Answer Space Patterns

```
Feasibility Array:
[F, F, F, T, T, T, T]
        ↑
    Find first T

Optimization:
Answer: [1, 2, 3, 4, 5, 6, 7, 8]
Check:  [N, N, N, Y, Y, Y, Y, Y]
                ↑
        Find minimum Y
```

## 💡 Common Mistakes in Recognition

### Mistake 1: Missing Hidden Monotonicity
```
Problem: "Koko eating bananas"
❌ Thinking: This is a simulation problem
✅ Reality: Speed vs time has monotonic relationship
```

### Mistake 2: Overcomplicating Simple Problems
```
Problem: "Find target in sorted array"
❌ Thinking: Need complex algorithm
✅ Reality: Standard binary search template
```

### Mistake 3: Forcing Binary Search
```
Problem: "Find majority element"
❌ Thinking: Array is sorted, use binary search
✅ Reality: Boyer-Moore voting is better (O(n) but simpler)
```

## 🎯 Practice Strategy

### Phase 1: Pattern Recognition (Week 1)
1. Read problem statement
2. Identify pattern BEFORE coding
3. Write down which template to use
4. Then implement

### Phase 2: Speed Recognition (Week 2)
1. Time yourself: 30 seconds to identify pattern
2. Practice with mixed problems
3. Focus on edge cases

### Phase 3: Interview Simulation (Week 3)
1. Explain your recognition process aloud
2. Justify why Binary Search applies
3. Discuss alternative approaches

## 📊 Pattern Frequency in Interviews

| Pattern | Frequency | Difficulty | Priority |
|---------|-----------|------------|----------|
| Standard Search | 20% | Easy | High |
| Finding Boundaries | 25% | Easy-Medium | High |
| Modified Arrays | 20% | Medium | High |
| 2D Matrix | 10% | Medium | Medium |
| Answer Space | 25% | Medium-Hard | Very High |

## 🔗 Quick Reference

### Pattern Selection Flowchart

```
Start
  ↓
Is array sorted?
  ├─ Yes → Direct search or boundaries
  └─ No → Check for rotated/peak
      ↓
Is it optimization?
  ├─ Yes → Answer space search
  └─ No → Binary search may not apply
```

### Template Quick Pick

```java
// Exact match
while (left <= right) { ... }

// Left boundary
while (left < right) { right = mid; }

// Right boundary  
while (left < right) { left = mid + 1; }

// Answer space
while (left <= right) { 
    if (feasible) result = mid;
}
```

---

**Next**: [Easy Problems](03_Easy_Problems.md)
