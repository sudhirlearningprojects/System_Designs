# Binary Search - Medium Problems

## Problem 1: Find First and Last Position (LeetCode 34)

**Difficulty**: Medium  
**Pattern**: Finding Boundaries

### Problem Statement
Given an array of integers `nums` sorted in non-decreasing order, find the starting and ending position of a given `target` value. If `target` is not found, return `[-1, -1]`.

You must write an algorithm with O(log n) runtime complexity.

### Examples
```
Input: nums = [5,7,7,8,8,10], target = 8
Output: [3,4]

Input: nums = [5,7,7,8,8,10], target = 6
Output: [-1,-1]

Input: nums = [], target = 0
Output: [-1,-1]
```

### Solution
```java
public int[] searchRange(int[] nums, int target) {
    int[] result = {-1, -1};
    
    // Find left boundary
    result[0] = findBoundary(nums, target, true);
    
    // If target not found, no need to find right boundary
    if (result[0] == -1) {
        return result;
    }
    
    // Find right boundary
    result[1] = findBoundary(nums, target, false);
    
    return result;
}

private int findBoundary(int[] nums, int target, boolean isLeft) {
    int left = 0, right = nums.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] == target) {
            result = mid;
            if (isLeft) {
                right = mid - 1;  // Continue searching left
            } else {
                left = mid + 1;   // Continue searching right
            }
        } else if (nums[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return result;
}
```

### Complexity Analysis
- **Time**: O(log n) - two binary searches
- **Space**: O(1)

### Dry Run
```
nums = [5,7,7,8,8,10], target = 8

Finding left boundary:
  Iteration 1: left=0, right=5, mid=2, nums[2]=7 < 8 → left=3
  Iteration 2: left=3, right=5, mid=4, nums[4]=8 == 8 → result=4, right=3
  Iteration 3: left=3, right=3, mid=3, nums[3]=8 == 8 → result=3, right=2
  Exit: result = 3

Finding right boundary:
  Iteration 1: left=0, right=5, mid=2, nums[2]=7 < 8 → left=3
  Iteration 2: left=3, right=5, mid=4, nums[4]=8 == 8 → result=4, left=5
  Iteration 3: left=5, right=5, mid=5, nums[5]=10 > 8 → right=4
  Exit: result = 4

Output: [3, 4]
```

---

## Problem 2: Search in Rotated Sorted Array (LeetCode 33)

**Difficulty**: Medium  
**Pattern**: Modified Binary Search

### Problem Statement
There is an integer array `nums` sorted in ascending order (with distinct values). Prior to being passed to your function, `nums` is possibly rotated at an unknown pivot index.

Given the array `nums` after the possible rotation and an integer `target`, return the index of `target` if it is in `nums`, or `-1` if it is not in `nums`.

### Examples
```
Input: nums = [4,5,6,7,0,1,2], target = 0
Output: 4

Input: nums = [4,5,6,7,0,1,2], target = 3
Output: -1

Input: nums = [1], target = 0
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

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

### Dry Run
```
nums = [4,5,6,7,0,1,2], target = 0

Iteration 1: left=0, right=6, mid=3
  nums[3]=7, nums[0]=4 ≤ nums[3] → left half sorted
  target=0 not in [4,7] → left=4

Iteration 2: left=4, right=6, mid=5
  nums[5]=1, nums[4]=0 > nums[5] → right half sorted
  target=0 not in [1,2] → right=4

Iteration 3: left=4, right=4, mid=4
  nums[4]=0 == target → return 4
```

---

## Problem 3: Find Peak Element (LeetCode 162)

**Difficulty**: Medium  
**Pattern**: Modified Binary Search

### Problem Statement
A peak element is an element that is strictly greater than its neighbors. Given a 0-indexed integer array `nums`, find a peak element, and return its index. If the array contains multiple peaks, return the index to any of the peaks.

You may imagine that `nums[-1] = nums[n] = -∞`. You must write an algorithm that runs in O(log n) time.

### Examples
```
Input: nums = [1,2,3,1]
Output: 2

Input: nums = [1,2,1,3,5,6,4]
Output: 5
```

### Solution
```java
public int findPeakElement(int[] nums) {
    int left = 0, right = nums.length - 1;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (nums[mid] > nums[mid + 1]) {
            // Peak is on the left (including mid)
            right = mid;
        } else {
            // Peak is on the right
            left = mid + 1;
        }
    }
    
    return left;
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

---

## Problem 4: Find Minimum in Rotated Sorted Array (LeetCode 153)

**Difficulty**: Medium  
**Pattern**: Modified Binary Search

### Problem Statement
Suppose an array of length `n` sorted in ascending order is rotated between `1` and `n` times. Given the sorted rotated array `nums` of unique elements, return the minimum element of this array.

### Examples
```
Input: nums = [3,4,5,1,2]
Output: 1

Input: nums = [4,5,6,7,0,1,2]
Output: 0

Input: nums = [11,13,15,17]
Output: 11
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
        } else {
            // Minimum is in left half (including mid)
            right = mid;
        }
    }
    
    return nums[left];
}
```

### Complexity Analysis
- **Time**: O(log n)
- **Space**: O(1)

---

## Problem 5: Search a 2D Matrix (LeetCode 74)

**Difficulty**: Medium  
**Pattern**: 2D Binary Search

### Problem Statement
You are given an `m x n` integer matrix with the following properties:
- Each row is sorted in non-decreasing order
- The first integer of each row is greater than the last integer of the previous row

Given an integer `target`, return `true` if `target` is in `matrix` or `false` otherwise.

### Examples
```
Input: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 3
Output: true

Input: matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 13
Output: false
```

### Solution
```java
public boolean searchMatrix(int[][] matrix, int target) {
    if (matrix.length == 0 || matrix[0].length == 0) {
        return false;
    }
    
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

### Complexity Analysis
- **Time**: O(log(m*n))
- **Space**: O(1)

---

## Problem 6: Koko Eating Bananas (LeetCode 875)

**Difficulty**: Medium  
**Pattern**: Binary Search on Answer Space

### Problem Statement
Koko loves to eat bananas. There are `n` piles of bananas, the `i-th` pile has `piles[i]` bananas. The guards have gone and will come back in `h` hours.

Koko can decide her bananas-per-hour eating speed of `k`. Each hour, she chooses some pile of bananas and eats `k` bananas from that pile. If the pile has less than `k` bananas, she eats all of them instead and will not eat any more bananas during this hour.

Return the minimum integer `k` such that she can eat all the bananas within `h` hours.

### Examples
```
Input: piles = [3,6,7,11], h = 8
Output: 4

Input: piles = [30,11,23,4,20], h = 5
Output: 30

Input: piles = [30,11,23,4,20], h = 6
Output: 23
```

### Solution
```java
public int minEatingSpeed(int[] piles, int h) {
    int left = 1;
    int right = getMax(piles);
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (canFinish(piles, mid, h)) {
            right = mid;  // Try smaller speed
        } else {
            left = mid + 1;
        }
    }
    
    return left;
}

private boolean canFinish(int[] piles, int speed, int h) {
    int hours = 0;
    for (int pile : piles) {
        hours += (pile + speed - 1) / speed;  // Ceiling division
        if (hours > h) return false;
    }
    return true;
}

private int getMax(int[] piles) {
    int max = 0;
    for (int pile : piles) {
        max = Math.max(max, pile);
    }
    return max;
}
```

### Complexity Analysis
- **Time**: O(n log m) where m is max pile size
- **Space**: O(1)

### Dry Run
```
piles = [3,6,7,11], h = 8

Answer space: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
Feasibility:  [N, N, N, Y, Y, Y, Y, Y, Y, Y,  Y]

Iteration 1: left=1, right=11, mid=6
  canFinish(6, 8)? → 1+1+2+2=6 ≤ 8 → Yes → right=6

Iteration 2: left=1, right=6, mid=3
  canFinish(3, 8)? → 1+2+3+4=10 > 8 → No → left=4

Iteration 3: left=4, right=6, mid=5
  canFinish(5, 8)? → 1+2+2+3=8 ≤ 8 → Yes → right=5

Iteration 4: left=4, right=5, mid=4
  canFinish(4, 8)? → 1+2+2+3=8 ≤ 8 → Yes → right=4

Exit: left == right = 4
```

---

## Problem 7: Capacity To Ship Packages (LeetCode 1011)

**Difficulty**: Medium  
**Pattern**: Binary Search on Answer Space

### Problem Statement
A conveyor belt has packages that must be shipped from one port to another within `days` days. The `i-th` package has a weight of `weights[i]`. Each day, we load the ship with packages on the conveyor belt (in the order given by `weights`). We may not load more weight than the maximum weight capacity of the ship.

Return the least weight capacity of the ship that will result in all the packages being shipped within `days` days.

### Examples
```
Input: weights = [1,2,3,4,5,6,7,8,9,10], days = 5
Output: 15
Explanation: Ship with capacity 15:
Day 1: 1,2,3,4,5
Day 2: 6,7
Day 3: 8
Day 4: 9
Day 5: 10

Input: weights = [3,2,2,4,1,4], days = 3
Output: 6
```

### Solution
```java
public int shipWithinDays(int[] weights, int days) {
    int left = getMax(weights);  // Min capacity = heaviest package
    int right = getSum(weights); // Max capacity = all packages
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (canShip(weights, mid, days)) {
            right = mid;  // Try smaller capacity
        } else {
            left = mid + 1;
        }
    }
    
    return left;
}

private boolean canShip(int[] weights, int capacity, int days) {
    int daysNeeded = 1;
    int currentLoad = 0;
    
    for (int weight : weights) {
        if (currentLoad + weight > capacity) {
            daysNeeded++;
            currentLoad = 0;
        }
        currentLoad += weight;
    }
    
    return daysNeeded <= days;
}

private int getMax(int[] weights) {
    int max = 0;
    for (int w : weights) max = Math.max(max, w);
    return max;
}

private int getSum(int[] weights) {
    int sum = 0;
    for (int w : weights) sum += w;
    return sum;
}
```

### Complexity Analysis
- **Time**: O(n log(sum - max))
- **Space**: O(1)

---

## Problem 8: Minimum Days to Make m Bouquets (LeetCode 1482)

**Difficulty**: Medium  
**Pattern**: Binary Search on Answer Space

### Problem Statement
You are given an integer array `bloomDay`, an integer `m` and an integer `k`. You want to make `m` bouquets. To make a bouquet, you need to use `k` adjacent flowers from the garden.

Return the minimum number of days you need to wait to be able to make `m` bouquets. If it is impossible, return `-1`.

### Examples
```
Input: bloomDay = [1,10,3,10,2], m = 3, k = 1
Output: 3
Explanation: After 3 days: [x, _, x, _, x] → 3 bouquets

Input: bloomDay = [1,10,3,10,2], m = 3, k = 2
Output: -1
Explanation: Need 6 flowers but only have 5

Input: bloomDay = [7,7,7,7,12,7,7], m = 2, k = 3
Output: 12
```

### Solution
```java
public int minDays(int[] bloomDay, int m, int k) {
    if ((long) m * k > bloomDay.length) {
        return -1;  // Impossible
    }
    
    int left = getMin(bloomDay);
    int right = getMax(bloomDay);
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (canMakeBouquets(bloomDay, mid, m, k)) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }
    
    return left;
}

private boolean canMakeBouquets(int[] bloomDay, int day, int m, int k) {
    int bouquets = 0;
    int flowers = 0;
    
    for (int bloom : bloomDay) {
        if (bloom <= day) {
            flowers++;
            if (flowers == k) {
                bouquets++;
                flowers = 0;
            }
        } else {
            flowers = 0;
        }
    }
    
    return bouquets >= m;
}

private int getMin(int[] arr) {
    int min = Integer.MAX_VALUE;
    for (int x : arr) min = Math.min(min, x);
    return min;
}

private int getMax(int[] arr) {
    int max = 0;
    for (int x : arr) max = Math.max(max, x);
    return max;
}
```

### Complexity Analysis
- **Time**: O(n log(max - min))
- **Space**: O(1)

---

## Problem 9: Split Array Largest Sum (LeetCode 410)

**Difficulty**: Medium  
**Pattern**: Binary Search on Answer Space

### Problem Statement
Given an integer array `nums` and an integer `k`, split `nums` into `k` non-empty subarrays such that the largest sum of any subarray is minimized.

Return the minimized largest sum of the split.

### Examples
```
Input: nums = [7,2,5,10,8], k = 2
Output: 18
Explanation: Split into [7,2,5] and [10,8], largest sum = 18

Input: nums = [1,2,3,4,5], k = 2
Output: 9
```

### Solution
```java
public int splitArray(int[] nums, int k) {
    int left = getMax(nums);  // Min possible max sum
    int right = getSum(nums); // Max possible max sum
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        if (canSplit(nums, mid, k)) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }
    
    return left;
}

private boolean canSplit(int[] nums, int maxSum, int k) {
    int splits = 1;
    int currentSum = 0;
    
    for (int num : nums) {
        if (currentSum + num > maxSum) {
            splits++;
            currentSum = num;
            if (splits > k) return false;
        } else {
            currentSum += num;
        }
    }
    
    return true;
}

private int getMax(int[] nums) {
    int max = 0;
    for (int n : nums) max = Math.max(max, n);
    return max;
}

private int getSum(int[] nums) {
    int sum = 0;
    for (int n : nums) sum += n;
    return sum;
}
```

### Complexity Analysis
- **Time**: O(n log(sum - max))
- **Space**: O(1)

---

## Problem 10: Find K Closest Elements (LeetCode 658)

**Difficulty**: Medium  
**Pattern**: Binary Search + Two Pointers

### Problem Statement
Given a sorted integer array `arr`, two integers `k` and `x`, return the `k` closest integers to `x` in the array. The result should also be sorted in ascending order.

### Examples
```
Input: arr = [1,2,3,4,5], k = 4, x = 3
Output: [1,2,3,4]

Input: arr = [1,2,3,4,5], k = 4, x = -1
Output: [1,2,3,4]
```

### Solution
```java
public List<Integer> findClosestElements(int[] arr, int k, int x) {
    int left = 0, right = arr.length - k;
    
    while (left < right) {
        int mid = left + (right - left) / 2;
        
        // Compare distances from x
        if (x - arr[mid] > arr[mid + k] - x) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    
    List<Integer> result = new ArrayList<>();
    for (int i = left; i < left + k; i++) {
        result.add(arr[i]);
    }
    
    return result;
}
```

### Complexity Analysis
- **Time**: O(log(n-k) + k)
- **Space**: O(1) excluding output

---

## Summary

| Problem | Pattern | Key Technique | Time Complexity |
|---------|---------|---------------|-----------------|
| Find First and Last | Boundaries | Two binary searches | O(log n) |
| Rotated Array Search | Modified | Identify sorted half | O(log n) |
| Find Peak | Modified | Compare with neighbor | O(log n) |
| Find Minimum Rotated | Modified | Compare with right | O(log n) |
| Search 2D Matrix | 2D Search | Treat as 1D array | O(log(m*n)) |
| Koko Bananas | Answer Space | Minimize eating speed | O(n log m) |
| Ship Packages | Answer Space | Minimize capacity | O(n log(sum)) |
| Make Bouquets | Answer Space | Minimize days | O(n log(max)) |
| Split Array | Answer Space | Minimize maximum sum | O(n log(sum)) |
| K Closest Elements | Binary Search | Find window start | O(log n + k) |

**Next**: [Hard Problems](05_Hard_Problems.md)
