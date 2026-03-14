# Binary Search - Easy Problems

## Problem 1: Binary Search (LeetCode 704)

**Difficulty**: Easy  
**Pattern**: Standard Binary Search

### Problem Statement
Given a sorted array of integers `nums` and an integer `target`, write a function to search `target` in `nums`. If `target` exists, return its index. Otherwise, return `-1`.

**Constraints**:
- All integers in `nums` are unique
- `nums` is sorted in ascending order
- -10^4 ≤ nums[i], target ≤ 10^4

### Examples
```
Input: nums = [-1,0,3,5,9,12], target = 9
Output: 4

Input: nums = [-1,0,3,5,9,12], target = 2
Output: -1
```

### Solution
```java
public int search(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    
    while (left <= right) {
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

### Complexity Analysis
- **Time**: O(log n) - halves search space each iteration
- **Space**: O(1) - only uses constant variables

### Dry Run
```
nums = [-1, 0, 3, 5, 9, 12], target = 9

Iteration 1: left=0, right=5, mid=2
  nums[2] = 3 < 9 → left = 3

Iteration 2: left=3, right=5, mid=4
  nums[4] = 9 == 9 → return 4
```

---

## Problem 2: Search Insert Position (LeetCode 35)

**Difficulty**: Easy  
**Pattern**: Finding Left Boundary

### Problem Statement
Given a sorted array and a target value, return the index if the target is found. If not, return the index where it would be if it were inserted in order.

### Examples
```
Input: nums = [1,3,5,6], target = 5
Output: 2

Input: nums = [1,3,5,6], target = 2
Output: 1

Input: nums = [1,3,5,6], target = 7
Output: 4
```

### Solution
```java
public int searchInsert(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] == target) {
            return mid;
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return left;  // Insert position
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

### Dry Run
```
nums = [1, 3, 5, 6], target = 2

Iteration 1: left=0, right=3, mid=1
  nums[1] = 3 > 2 → right = 0

Iteration 2: left=0, right=0, mid=0
  nums[0] = 1 < 2 → left = 1

Exit: left > right, return left = 1
```

---

## Problem 3: First Bad Version (LeetCode 278)

**Difficulty**: Easy  
**Pattern**: Finding Left Boundary

### Problem Statement
You are a product manager and currently leading a team to develop a new product. Unfortunately, the latest version of your product fails the quality check. Since each version is developed based on the previous version, all the versions after a bad version are also bad.

Suppose you have `n` versions `[1, 2, ..., n]` and you want to find out the first bad one, which causes all the following ones to be bad.

You are given an API `bool isBadVersion(version)` which returns whether `version` is bad.

### Examples
```
Input: n = 5, bad = 4
Output: 4
Explanation: 
isBadVersion(3) -> false
isBadVersion(4) -> true
isBadVersion(5) -> true
```

### Solution
```java
public int firstBadVersion(int n) {
    int left = 1, right = n;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (isBadVersion(mid)) {
            right = mid;  // First bad might be mid or before
        } else {
            left = mid + 1;  // First bad is after mid
        }
    }
    
    return left;
}
```

### Complexity Analysis
- **Time**: O(log n) - binary search
- **Space**: O(1)

### Dry Run
```
n = 5, bad = 4
[1, 2, 3, 4, 5]
[G, G, G, B, B]  (G=Good, B=Bad)

Iteration 1: left=1, right=5, mid=3
  isBadVersion(3) = false → left = 4

Iteration 2: left=4, right=5, mid=4
  isBadVersion(4) = true → right = 4

Exit: left == right = 4
```

---

## Problem 4: Valid Perfect Square (LeetCode 367)

**Difficulty**: Easy  
**Pattern**: Standard Binary Search

### Problem Statement
Given a positive integer `num`, return `true` if `num` is a perfect square or `false` otherwise.

A perfect square is an integer that is the square of an integer. Do not use built-in library functions.

### Examples
```
Input: num = 16
Output: true

Input: num = 14
Output: false
```

### Solution
```java
public boolean isPerfectSquare(int num) {
    if (num < 2) return true;
    
    long left = 2, right = num / 2;
    
    while (left <= right) {
        long mid = left + (right - left) / 2;
        long square = mid * mid;
        
        if (square == num) {
            return true;
        } else if (square < num) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return false;
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

### Dry Run
```
num = 16

Iteration 1: left=2, right=8, mid=5
  5*5 = 25 > 16 → right = 4

Iteration 2: left=2, right=4, mid=3
  3*3 = 9 < 16 → left = 4

Iteration 3: left=4, right=4, mid=4
  4*4 = 16 == 16 → return true
```

---

## Problem 5: Sqrt(x) (LeetCode 69)

**Difficulty**: Easy  
**Pattern**: Finding Right Boundary

### Problem Statement
Given a non-negative integer `x`, return the square root of `x` rounded down to the nearest integer.

### Examples
```
Input: x = 4
Output: 2

Input: x = 8
Output: 2
Explanation: sqrt(8) = 2.828..., rounded down = 2
```

### Solution
```java
public int mySqrt(int x) {
    if (x < 2) return x;
    
    long left = 1, right = x / 2;
    
    while (left <= right) {
        long mid = left + (right - left) / 2;
        long square = mid * mid;
        
        if (square == x) {
            return (int) mid;
        } else if (square < x) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return (int) right;  // Largest integer whose square <= x
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

### Dry Run
```
x = 8

Iteration 1: left=1, right=4, mid=2
  2*2 = 4 < 8 → left = 3

Iteration 2: left=3, right=4, mid=3
  3*3 = 9 > 8 → right = 2

Exit: left > right, return right = 2
```

---

## Problem 6: Guess Number Higher or Lower (LeetCode 374)

**Difficulty**: Easy  
**Pattern**: Standard Binary Search

### Problem Statement
We are playing the Guess Game. The game is as follows:

I pick a number from `1` to `n`. You have to guess which number I picked.

Every time you guess wrong, I will tell you whether the number I picked is higher or lower than your guess.

You call a pre-defined API `int guess(int num)`, which returns three possible results:
- `-1`: Your guess is higher than the number I picked
- `1`: Your guess is lower than the number I picked
- `0`: Your guess is equal to the number I picked

### Solution
```java
public int guessNumber(int n) {
    int left = 1, right = n;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        int result = guess(mid);
        
        if (result == 0) {
            return mid;
        } else if (result == 1) {
            left = mid + 1;  // Target is higher
        } else {
            right = mid - 1;  // Target is lower
        }
    }
    
    return -1;
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

---

## Problem 7: Peak Index in Mountain Array (LeetCode 852)

**Difficulty**: Easy  
**Pattern**: Modified Binary Search

### Problem Statement
An array `arr` is a mountain if:
- `arr.length >= 3`
- There exists some `i` with `0 < i < arr.length - 1` such that:
  - `arr[0] < arr[1] < ... < arr[i - 1] < arr[i]`
  - `arr[i] > arr[i + 1] > ... > arr[arr.length - 1]`

Given a mountain array, return the index of the peak.

### Examples
```
Input: arr = [0,1,0]
Output: 1

Input: arr = [0,2,1,0]
Output: 1

Input: arr = [0,10,5,2]
Output: 1
```

### Solution
```java
public int peakIndexInMountainArray(int[] arr) {
    int left = 0, right = arr.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] < arr[mid + 1]) {
            left = mid + 1;  // Peak is on the right
        } else {
            right = mid;  // Peak is mid or on the left
        }
    }
    
    return left;
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

### Dry Run
```
arr = [0, 2, 5, 3, 1]

Iteration 1: left=0, right=4, mid=2
  arr[2]=5 > arr[3]=3 → right = 2

Iteration 2: left=0, right=2, mid=1
  arr[1]=2 < arr[2]=5 → left = 2

Exit: left == right = 2
```

---

## Problem 8: Count Negative Numbers in Sorted Matrix (LeetCode 1351)

**Difficulty**: Easy  
**Pattern**: Binary Search in 2D

### Problem Statement
Given a `m x n` matrix `grid` which is sorted in non-increasing order both row-wise and column-wise, return the number of negative numbers in `grid`.

### Examples
```
Input: grid = [[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]]
Output: 8

Input: grid = [[3,2],[1,0]]
Output: 0
```

### Solution
```java
public int countNegatives(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int count = 0;
    
    for (int i = 0; i < m; i++) {
        int left = 0, right = n - 1;
        int firstNegative = n;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (grid[i][mid] < 0) {
                firstNegative = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        count += (n - firstNegative);
    }
    
    return count;
}
```

### Complexity Analysis
- **Time**: O(m log n) - binary search on each row
- **Space**: O(1)

### Dry Run
```
grid = [[4,3,2,-1],
        [3,2,1,-1],
        [1,1,-1,-2],
        [-1,-1,-2,-3]]

Row 0: [4,3,2,-1] → first negative at index 3 → count += 1
Row 1: [3,2,1,-1] → first negative at index 3 → count += 1
Row 2: [1,1,-1,-2] → first negative at index 2 → count += 2
Row 3: [-1,-1,-2,-3] → first negative at index 0 → count += 4

Total: 8
```

---

## Summary

| Problem | Pattern | Key Concept | Difficulty |
|---------|---------|-------------|------------|
| Binary Search | Standard | Basic template | ⭐ |
| Search Insert Position | Left Boundary | Insert position = left pointer | ⭐ |
| First Bad Version | Left Boundary | Find first occurrence | ⭐ |
| Valid Perfect Square | Standard | Square root check | ⭐ |
| Sqrt(x) | Right Boundary | Floor of square root | ⭐ |
| Guess Number | Standard | Interactive binary search | ⭐ |
| Peak Index | Modified | Mountain array property | ⭐ |
| Count Negatives | 2D Binary Search | Row-wise search | ⭐ |

**Next**: [Medium Problems](04_Medium_Problems.md)
