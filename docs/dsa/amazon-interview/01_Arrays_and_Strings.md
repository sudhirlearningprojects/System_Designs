# Amazon Interview - Arrays & Strings Problems

## Problem 1: Two Sum (LeetCode 1) ⭐⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: Very High (Asked in 60%+ Amazon interviews)  
**Pattern**: Hash Map

### Problem Statement
Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`. You may assume that each input would have exactly one solution, and you may not use the same element twice.

### Examples
```
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: nums[0] + nums[1] = 2 + 7 = 9

Input: nums = [3,2,4], target = 6
Output: [1,2]

Input: nums = [3,3], target = 6
Output: [0,1]
```

### Solution 1: Brute Force
```java
public int[] twoSum(int[] nums, int target) {
    for (int i = 0; i < nums.length; i++) {
        for (int j = i + 1; j < nums.length; j++) {
            if (nums[i] + nums[j] == target) {
                return new int[]{i, j};
            }
        }
    }
    return new int[]{};
}
```
**Time**: O(n²), **Space**: O(1)

### Solution 2: Hash Map (Optimal)
```java
public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        
        map.put(nums[i], i);
    }
    
    return new int[]{};
}
```
**Time**: O(n), **Space**: O(n)

### Dry Run
```
nums = [2, 7, 11, 15], target = 9

i=0: complement = 9-2 = 7, map={}, add 2→0, map={2:0}
i=1: complement = 9-7 = 2, map contains 2! return [0, 1]
```

### Follow-up Questions
1. What if there are multiple solutions? Return all pairs
2. What if array is sorted? Use two pointers
3. What if we need three numbers? Use 3Sum approach

---

## Problem 2: Longest Substring Without Repeating Characters (LeetCode 3) ⭐⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Very High (Asked in 55%+ Amazon interviews)  
**Pattern**: Sliding Window + Hash Set

### Problem Statement
Given a string `s`, find the length of the longest substring without repeating characters.

### Examples
```
Input: s = "abcabcbb"
Output: 3
Explanation: "abc" is the longest substring

Input: s = "bbbbb"
Output: 1
Explanation: "b" is the longest substring

Input: s = "pwwkew"
Output: 3
Explanation: "wke" is the longest substring
```

### Solution: Sliding Window
```java
public int lengthOfLongestSubstring(String s) {
    Set<Character> set = new HashSet<>();
    int left = 0, maxLength = 0;
    
    for (int right = 0; right < s.length(); right++) {
        // Shrink window until no duplicates
        while (set.contains(s.charAt(right))) {
            set.remove(s.charAt(left));
            left++;
        }
        
        set.add(s.charAt(right));
        maxLength = Math.max(maxLength, right - left + 1);
    }
    
    return maxLength;
}
```
**Time**: O(n), **Space**: O(min(n, m)) where m is charset size

### Optimized Solution: Hash Map with Index
```java
public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> map = new HashMap<>();
    int left = 0, maxLength = 0;
    
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        
        if (map.containsKey(c)) {
            left = Math.max(left, map.get(c) + 1);
        }
        
        map.put(c, right);
        maxLength = Math.max(maxLength, right - left + 1);
    }
    
    return maxLength;
}
```
**Time**: O(n), **Space**: O(min(n, m))

### Dry Run
```
s = "abcabcbb"

right=0: c='a', set={a}, left=0, maxLen=1
right=1: c='b', set={a,b}, left=0, maxLen=2
right=2: c='c', set={a,b,c}, left=0, maxLen=3
right=3: c='a', duplicate! remove 'a', left=1, set={b,c}, add 'a', set={b,c,a}, maxLen=3
right=4: c='b', duplicate! remove 'b', left=2, set={c,a}, add 'b', set={c,a,b}, maxLen=3
...
Result: 3
```

---

## Problem 3: Trapping Rain Water (LeetCode 42) ⭐⭐⭐⭐

**Difficulty**: Hard  
**Frequency**: High (Asked in 40%+ Amazon interviews)  
**Pattern**: Two Pointers

### Problem Statement
Given `n` non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.

### Examples
```
Input: height = [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6

Input: height = [4,2,0,3,2,5]
Output: 9
```

### Solution 1: Two Pointers (Optimal)
```java
public int trap(int[] height) {
    if (height == null || height.length == 0) return 0;
    
    int left = 0, right = height.length - 1;
    int leftMax = 0, rightMax = 0;
    int water = 0;
    
    while (left < right) {
        if (height[left] < height[right]) {
            if (height[left] >= leftMax) {
                leftMax = height[left];
            } else {
                water += leftMax - height[left];
            }
            left++;
        } else {
            if (height[right] >= rightMax) {
                rightMax = height[right];
            } else {
                water += rightMax - height[right];
            }
            right--;
        }
    }
    
    return water;
}
```
**Time**: O(n), **Space**: O(1)

### Solution 2: Dynamic Programming
```java
public int trap(int[] height) {
    int n = height.length;
    if (n == 0) return 0;
    
    int[] leftMax = new int[n];
    int[] rightMax = new int[n];
    
    leftMax[0] = height[0];
    for (int i = 1; i < n; i++) {
        leftMax[i] = Math.max(leftMax[i - 1], height[i]);
    }
    
    rightMax[n - 1] = height[n - 1];
    for (int i = n - 2; i >= 0; i--) {
        rightMax[i] = Math.max(rightMax[i + 1], height[i]);
    }
    
    int water = 0;
    for (int i = 0; i < n; i++) {
        water += Math.min(leftMax[i], rightMax[i]) - height[i];
    }
    
    return water;
}
```
**Time**: O(n), **Space**: O(n)

### Dry Run
```
height = [0,1,0,2,1,0,1,3,2,1,2,1]

Two Pointers Approach:
left=0, right=11, leftMax=0, rightMax=0, water=0

Step 1: height[0]=0 < height[11]=1
  leftMax = 0, left=1

Step 2: height[1]=1 < height[11]=1
  leftMax = 1, left=2

Step 3: height[2]=0 < height[11]=1
  water += 1-0 = 1, left=3

...continue until left meets right

Total water = 6
```

---

## Problem 4: Product of Array Except Self (LeetCode 238) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High (Asked in 45%+ Amazon interviews)  
**Pattern**: Prefix/Suffix Product

### Problem Statement
Given an integer array `nums`, return an array `answer` such that `answer[i]` is equal to the product of all the elements of `nums` except `nums[i]`.

You must write an algorithm that runs in O(n) time and without using the division operation.

### Examples
```
Input: nums = [1,2,3,4]
Output: [24,12,8,6]

Input: nums = [-1,1,0,-3,3]
Output: [0,0,9,0,0]
```

### Solution: Prefix and Suffix Products
```java
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    
    // Calculate prefix products
    result[0] = 1;
    for (int i = 1; i < n; i++) {
        result[i] = result[i - 1] * nums[i - 1];
    }
    
    // Calculate suffix products and multiply
    int suffix = 1;
    for (int i = n - 1; i >= 0; i--) {
        result[i] *= suffix;
        suffix *= nums[i];
    }
    
    return result;
}
```
**Time**: O(n), **Space**: O(1) (output array doesn't count)

### Dry Run
```
nums = [1, 2, 3, 4]

Step 1: Calculate prefix products
result = [1, 1, 2, 6]
  result[0] = 1
  result[1] = 1 * 1 = 1
  result[2] = 1 * 2 = 2
  result[3] = 2 * 3 = 6

Step 2: Calculate suffix and multiply
suffix = 1
  i=3: result[3] = 6 * 1 = 6, suffix = 1 * 4 = 4
  i=2: result[2] = 2 * 4 = 8, suffix = 4 * 3 = 12
  i=1: result[1] = 1 * 12 = 12, suffix = 12 * 2 = 24
  i=0: result[0] = 1 * 24 = 24, suffix = 24 * 1 = 24

result = [24, 12, 8, 6]
```

---

## Problem 5: Valid Parentheses (LeetCode 20) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High (Asked in 40%+ Amazon interviews)  
**Pattern**: Stack

### Problem Statement
Given a string `s` containing just the characters `'('`, `')'`, `'{'`, `'}'`, `'['` and `']'`, determine if the input string is valid.

### Examples
```
Input: s = "()"
Output: true

Input: s = "()[]{}"
Output: true

Input: s = "(]"
Output: false

Input: s = "([)]"
Output: false
```

### Solution: Stack
```java
public boolean isValid(String s) {
    Stack<Character> stack = new Stack<>();
    Map<Character, Character> map = new HashMap<>();
    map.put(')', '(');
    map.put('}', '{');
    map.put(']', '[');
    
    for (char c : s.toCharArray()) {
        if (map.containsKey(c)) {
            // Closing bracket
            if (stack.isEmpty() || stack.pop() != map.get(c)) {
                return false;
            }
        } else {
            // Opening bracket
            stack.push(c);
        }
    }
    
    return stack.isEmpty();
}
```
**Time**: O(n), **Space**: O(n)

### Dry Run
```
s = "([)]"

c='(': stack=['(']
c='[': stack=['(', '[']
c=')': pop '[', expected '(' → false
```

---

## Problem 6: Group Anagrams (LeetCode 49) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High (Asked in 35%+ Amazon interviews)  
**Pattern**: Hash Map

### Problem Statement
Given an array of strings `strs`, group the anagrams together.

### Examples
```
Input: strs = ["eat","tea","tan","ate","nat","bat"]
Output: [["bat"],["nat","tan"],["ate","eat","tea"]]

Input: strs = [""]
Output: [[""]]

Input: strs = ["a"]
Output: [["a"]]
```

### Solution: Sorted String as Key
```java
public List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    
    for (String str : strs) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        String key = new String(chars);
        
        map.putIfAbsent(key, new ArrayList<>());
        map.get(key).add(str);
    }
    
    return new ArrayList<>(map.values());
}
```
**Time**: O(n * k log k) where k is max string length  
**Space**: O(n * k)

### Optimized Solution: Character Count as Key
```java
public List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    
    for (String str : strs) {
        int[] count = new int[26];
        for (char c : str.toCharArray()) {
            count[c - 'a']++;
        }
        
        String key = Arrays.toString(count);
        map.putIfAbsent(key, new ArrayList<>());
        map.get(key).add(str);
    }
    
    return new ArrayList<>(map.values());
}
```
**Time**: O(n * k), **Space**: O(n * k)

---

## Problem 7: Merge Intervals (LeetCode 56) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High (Asked in 45%+ Amazon interviews)  
**Pattern**: Sorting + Greedy

### Problem Statement
Given an array of `intervals` where `intervals[i] = [starti, endi]`, merge all overlapping intervals.

### Examples
```
Input: intervals = [[1,3],[2,6],[8,10],[15,18]]
Output: [[1,6],[8,10],[15,18]]

Input: intervals = [[1,4],[4,5]]
Output: [[1,5]]
```

### Solution
```java
public int[][] merge(int[][] intervals) {
    if (intervals.length <= 1) return intervals;
    
    // Sort by start time
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    
    List<int[]> result = new ArrayList<>();
    int[] current = intervals[0];
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] <= current[1]) {
            // Overlapping, merge
            current[1] = Math.max(current[1], intervals[i][1]);
        } else {
            // Non-overlapping, add current and move to next
            result.add(current);
            current = intervals[i];
        }
    }
    
    result.add(current);
    return result.toArray(new int[result.size()][]);
}
```
**Time**: O(n log n), **Space**: O(n)

### Dry Run
```
intervals = [[1,3],[2,6],[8,10],[15,18]]

After sorting: [[1,3],[2,6],[8,10],[15,18]]

current = [1,3]
i=1: [2,6], 2 <= 3 → merge → current = [1,6]
i=2: [8,10], 8 > 6 → add [1,6], current = [8,10]
i=3: [15,18], 15 > 10 → add [8,10], current = [15,18]
Add [15,18]

Result: [[1,6],[8,10],[15,18]]
```

---

## Problem 8: Rotate Image (LeetCode 48) ⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Medium (Asked in 30%+ Amazon interviews)  
**Pattern**: Matrix Manipulation

### Problem Statement
You are given an `n x n` 2D matrix representing an image, rotate the image by 90 degrees (clockwise). You have to rotate the image in-place.

### Examples
```
Input: matrix = [[1,2,3],[4,5,6],[7,8,9]]
Output: [[7,4,1],[8,5,2],[9,6,3]]

Input: matrix = [[5,1,9,11],[2,4,8,10],[13,3,6,7],[15,14,12,16]]
Output: [[15,13,2,5],[14,3,4,1],[12,6,8,9],[16,7,10,11]]
```

### Solution: Transpose + Reverse
```java
public void rotate(int[][] matrix) {
    int n = matrix.length;
    
    // Step 1: Transpose the matrix
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            int temp = matrix[i][j];
            matrix[i][j] = matrix[j][i];
            matrix[j][i] = temp;
        }
    }
    
    // Step 2: Reverse each row
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n / 2; j++) {
            int temp = matrix[i][j];
            matrix[i][j] = matrix[i][n - 1 - j];
            matrix[i][n - 1 - j] = temp;
        }
    }
}
```
**Time**: O(n²), **Space**: O(1)

### Dry Run
```
matrix = [[1,2,3],
          [4,5,6],
          [7,8,9]]

Step 1: Transpose
[[1,4,7],
 [2,5,8],
 [3,6,9]]

Step 2: Reverse each row
[[7,4,1],
 [8,5,2],
 [9,6,3]]
```

---

## Summary

| Problem | Difficulty | Frequency | Pattern | Time | Space |
|---------|------------|-----------|---------|------|-------|
| Two Sum | Easy | ⭐⭐⭐⭐⭐ | Hash Map | O(n) | O(n) |
| Longest Substring | Medium | ⭐⭐⭐⭐⭐ | Sliding Window | O(n) | O(min(n,m)) |
| Trapping Rain Water | Hard | ⭐⭐⭐⭐ | Two Pointers | O(n) | O(1) |
| Product Except Self | Medium | ⭐⭐⭐⭐ | Prefix/Suffix | O(n) | O(1) |
| Valid Parentheses | Easy | ⭐⭐⭐⭐ | Stack | O(n) | O(n) |
| Group Anagrams | Medium | ⭐⭐⭐⭐ | Hash Map | O(n*k) | O(n*k) |
| Merge Intervals | Medium | ⭐⭐⭐⭐ | Sorting | O(n log n) | O(n) |
| Rotate Image | Medium | ⭐⭐⭐ | Matrix | O(n²) | O(1) |

**Next**: [Trees & Graphs Problems](02_Trees_and_Graphs.md)
