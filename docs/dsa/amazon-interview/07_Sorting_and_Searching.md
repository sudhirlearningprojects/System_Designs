# Amazon Interview - Sorting & Searching Problems

## Problem 29: Kth Largest Element in Array (LeetCode 215) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: QuickSelect / Heap

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/kth-largest-element-in-an-array/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/kth-largest-element/)

### Solution 1: Min Heap
```java
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }
    
    return minHeap.peek();
}
```
**Time**: O(n log k), **Space**: O(k)

### Solution 2: QuickSelect (Optimal)
```java
public int findKthLargest(int[] nums, int k) {
    return quickSelect(nums, 0, nums.length - 1, nums.length - k);
}

private int quickSelect(int[] nums, int left, int right, int k) {
    if (left == right) return nums[left];
    
    int pivotIndex = partition(nums, left, right);
    
    if (k == pivotIndex) {
        return nums[k];
    } else if (k < pivotIndex) {
        return quickSelect(nums, left, pivotIndex - 1, k);
    } else {
        return quickSelect(nums, pivotIndex + 1, right, k);
    }
}

private int partition(int[] nums, int left, int right) {
    int pivot = nums[right];
    int i = left;
    
    for (int j = left; j < right; j++) {
        if (nums[j] < pivot) {
            swap(nums, i, j);
            i++;
        }
    }
    
    swap(nums, i, right);
    return i;
}

private void swap(int[] nums, int i, int j) {
    int temp = nums[i];
    nums[i] = nums[j];
    nums[j] = temp;
}
```
**Time**: O(n) average, O(n²) worst, **Space**: O(1)

---

## Problem 30: Search in Rotated Sorted Array (LeetCode 33) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: Binary Search

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/search-in-rotated-sorted-array/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/search-in-a-rotated-array/)

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
**Time**: O(log n), **Space**: O(1)

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

## Summary - All 30 Problems

| # | Problem | Difficulty | Frequency | Pattern | Time | Space |
|---|---------|------------|-----------|---------|------|-------|
| 1 | Two Sum | Easy | ⭐⭐⭐⭐⭐ | Hash Map | O(n) | O(n) |
| 2 | Longest Substring | Medium | ⭐⭐⭐⭐⭐ | Sliding Window | O(n) | O(min(n,m)) |
| 3 | Trapping Rain Water | Hard | ⭐⭐⭐⭐ | Two Pointers | O(n) | O(1) |
| 4 | Product Except Self | Medium | ⭐⭐⭐⭐ | Prefix/Suffix | O(n) | O(1) |
| 5 | Valid Parentheses | Easy | ⭐⭐⭐⭐ | Stack | O(n) | O(n) |
| 6 | Group Anagrams | Medium | ⭐⭐⭐⭐ | Hash Map | O(n*k) | O(n*k) |
| 7 | Merge Intervals | Medium | ⭐⭐⭐⭐ | Sorting | O(n log n) | O(n) |
| 8 | Rotate Image | Medium | ⭐⭐⭐ | Matrix | O(n²) | O(1) |
| 9 | Level Order Traversal | Medium | ⭐⭐⭐⭐⭐ | BFS | O(n) | O(n) |
| 10 | Lowest Common Ancestor | Medium | ⭐⭐⭐⭐ | DFS | O(n) | O(h) |
| 11 | Number of Islands | Medium | ⭐⭐⭐⭐⭐ | DFS/BFS | O(m*n) | O(m*n) |
| 12 | Word Ladder | Hard | ⭐⭐⭐⭐ | BFS | O(n*m²*26) | O(n) |
| 13 | Course Schedule | Medium | ⭐⭐⭐⭐ | Topological Sort | O(V+E) | O(V+E) |
| 14 | Serialize Tree | Hard | ⭐⭐⭐⭐ | DFS | O(n) | O(n) |
| 15 | Max Path Sum | Hard | ⭐⭐⭐⭐ | DFS | O(n) | O(h) |
| 16 | Climbing Stairs | Easy | ⭐⭐⭐⭐ | DP | O(n) | O(1) |
| 17 | Coin Change | Medium | ⭐⭐⭐⭐ | DP | O(n*amount) | O(amount) |
| 18 | Word Break | Medium | ⭐⭐⭐⭐ | DP | O(n³) | O(n) |
| 19 | Longest Palindrome | Medium | ⭐⭐⭐⭐ | DP | O(n²) | O(1) |
| 20 | Buy Sell Stock | Easy | ⭐⭐⭐⭐ | DP/Greedy | O(n) | O(1) |
| 21 | Reverse Linked List | Easy | ⭐⭐⭐⭐ | Two Pointers | O(n) | O(1) |
| 22 | Merge Two Lists | Easy | ⭐⭐⭐⭐ | Two Pointers | O(n+m) | O(1) |
| 23 | Copy Random List | Medium | ⭐⭐⭐⭐ | Hash Map | O(n) | O(n) |
| 24 | Queue using Stacks | Easy | ⭐⭐⭐⭐ | Stack | O(1) | O(n) |
| 25 | Min Stack | Medium | ⭐⭐⭐⭐ | Stack | O(1) | O(n) |
| 26 | Eval RPN | Medium | ⭐⭐⭐ | Stack | O(n) | O(n) |
| 27 | LRU Cache | Medium | ⭐⭐⭐⭐⭐ | Hash+DLL | O(1) | O(capacity) |
| 28 | Design HashMap | Easy | ⭐⭐⭐ | Array+List | O(1) avg | O(n) |
| 29 | Kth Largest | Medium | ⭐⭐⭐⭐ | QuickSelect | O(n) | O(1) |
| 30 | Rotated Array | Medium | ⭐⭐⭐⭐ | Binary Search | O(log n) | O(1) |

---

**Next**: [Quick Reference](Quick_Reference.md)
