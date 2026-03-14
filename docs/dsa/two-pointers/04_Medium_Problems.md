# Two Pointers - Medium Problems (50%)

## 📚 10 Medium Problems with Complete Solutions

---

## Problem 1: 3Sum

**Difficulty**: Medium  
**Pattern**: Opposite Direction + Iteration  
**LeetCode**: #15

### Problem Statement

Given an integer array `nums`, return all triplets `[nums[i], nums[j], nums[k]]` such that `i != j`, `i != k`, and `j != k`, and `nums[i] + nums[j] + nums[k] == 0`.

The solution set must not contain duplicate triplets.

### Examples

```
Input: nums = [-1,0,1,2,-1,-4]
Output: [[-1,-1,2],[-1,0,1]]

Input: nums = [0,1,1]
Output: []

Input: nums = [0,0,0]
Output: [[0,0,0]]
```

### Solution

```java
public class ThreeSum {
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums); // Sort first
        
        for (int i = 0; i < nums.length - 2; i++) {
            // Skip duplicates for first number
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            
            int left = i + 1;
            int right = nums.length - 1;
            int target = -nums[i];
            
            while (left < right) {
                int sum = nums[left] + nums[right];
                
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    
                    // Skip duplicates for second number
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    // Skip duplicates for third number
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `nums = [-1, 0, 1, 2, -1, -4]`

```
After sorting: [-4, -1, -1, 0, 1, 2]

Iteration 1: i = 0, nums[i] = -4, target = 4
  left = 1, right = 5
  [-4, -1, -1, 0, 1, 2]
    ↑   ↑           ↑
    i  left       right
  
  sum = -1 + 2 = 1 < 4, left++
  sum = -1 + 2 = 1 < 4, left++
  sum = 0 + 2 = 2 < 4, left++
  sum = 1 + 2 = 3 < 4, left++
  left >= right, no solution

Iteration 2: i = 1, nums[i] = -1, target = 1
  left = 2, right = 5
  [-4, -1, -1, 0, 1, 2]
       ↑   ↑        ↑
       i  left    right
  
  sum = -1 + 2 = 1 == 1 ✓
  Found: [-1, -1, 2]
  Skip duplicates, left++, right--
  
  left = 3, right = 4
  sum = 0 + 1 = 1 == 1 ✓
  Found: [-1, 0, 1]

Iteration 3: i = 2, nums[i] = -1
  Skip (duplicate of previous)

Iteration 4: i = 3, nums[i] = 0, target = 0
  left = 4, right = 5
  sum = 1 + 2 = 3 > 0, right--
  left >= right, no solution

Result: [[-1, -1, 2], [-1, 0, 1]]
```

### Complexity Analysis

- **Time Complexity**: O(n²) - O(n log n) for sorting + O(n²) for two pointers
- **Space Complexity**: O(1) - Excluding result array

### Test Cases

```java
@Test
public void testThreeSum() {
    ThreeSum solution = new ThreeSum();
    
    List<List<Integer>> result1 = solution.threeSum(new int[]{-1, 0, 1, 2, -1, -4});
    assertEquals(2, result1.size());
    assertTrue(result1.contains(Arrays.asList(-1, -1, 2)));
    assertTrue(result1.contains(Arrays.asList(-1, 0, 1)));
    
    List<List<Integer>> result2 = solution.threeSum(new int[]{0, 1, 1});
    assertEquals(0, result2.size());
    
    List<List<Integer>> result3 = solution.threeSum(new int[]{0, 0, 0});
    assertEquals(1, result3.size());
    assertTrue(result3.contains(Arrays.asList(0, 0, 0)));
}
```

---

## Problem 2: Container With Most Water

**Difficulty**: Medium  
**Pattern**: Opposite Direction  
**LeetCode**: #11

### Problem Statement

You are given an integer array `height` of length `n`. There are `n` vertical lines drawn such that the two endpoints of the `i-th` line are `(i, 0)` and `(i, height[i])`.

Find two lines that together with the x-axis form a container that contains the most water.

Return the maximum amount of water a container can store.

### Examples

```
Input: height = [1,8,6,2,5,4,8,3,7]
Output: 49
Explanation: Lines at index 1 and 8 form container with area = 7 * 7 = 49

Input: height = [1,1]
Output: 1
```

### Solution

```java
public class ContainerWithMostWater {
    public int maxArea(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;
        
        while (left < right) {
            // Calculate current area
            int width = right - left;
            int minHeight = Math.min(height[left], height[right]);
            int area = width * minHeight;
            maxArea = Math.max(maxArea, area);
            
            // Move pointer with smaller height
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        
        return maxArea;
    }
}
```

### Dry Run

**Input**: `height = [1, 8, 6, 2, 5, 4, 8, 3, 7]`

```
Step 1: left = 0, right = 8
  height[0] = 1, height[8] = 7
  width = 8, minHeight = 1
  area = 8 * 1 = 8
  maxArea = 8
  height[0] < height[8], left++

Step 2: left = 1, right = 8
  height[1] = 8, height[8] = 7
  width = 7, minHeight = 7
  area = 7 * 7 = 49
  maxArea = 49
  height[1] > height[8], right--

Step 3: left = 1, right = 7
  height[1] = 8, height[7] = 3
  width = 6, minHeight = 3
  area = 6 * 3 = 18
  maxArea = 49 (no change)
  height[1] > height[7], right--

... continue until left >= right

Final maxArea = 49
```

### Complexity Analysis

- **Time Complexity**: O(n) - Single pass
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testMaxArea() {
    ContainerWithMostWater solution = new ContainerWithMostWater();
    
    assertEquals(49, solution.maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}));
    assertEquals(1, solution.maxArea(new int[]{1, 1}));
    assertEquals(16, solution.maxArea(new int[]{4, 3, 2, 1, 4}));
}
```

---

## Problem 3: Sort Colors (Dutch National Flag)

**Difficulty**: Medium  
**Pattern**: Three Pointers  
**LeetCode**: #75

### Problem Statement

Given an array `nums` with `n` objects colored red, white, or blue, sort them in-place so that objects of the same color are adjacent, with colors in order red, white, and blue.

We use integers 0, 1, and 2 to represent red, white, and blue respectively.

### Solution

```java
public class SortColors {
    public void sortColors(int[] nums) {
        int low = 0;      // Boundary for 0s
        int mid = 0;      // Current element
        int high = nums.length - 1; // Boundary for 2s
        
        while (mid <= high) {
            if (nums[mid] == 0) {
                swap(nums, low, mid);
                low++;
                mid++;
            } else if (nums[mid] == 1) {
                mid++;
            } else { // nums[mid] == 2
                swap(nums, mid, high);
                high--;
            }
        }
    }
    
    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
}
```

### Dry Run

**Input**: `nums = [2, 0, 2, 1, 1, 0]`

```
Initial: low = 0, mid = 0, high = 5
[2, 0, 2, 1, 1, 0]
 ↑              ↑
low,mid       high

Step 1: nums[mid] = 2
  Swap nums[0] and nums[5]: [0, 0, 2, 1, 1, 2]
  high = 4
  [0, 0, 2, 1, 1, 2]
   ↑           ↑
  low,mid    high

Step 2: nums[mid] = 0
  Swap nums[0] and nums[0]: [0, 0, 2, 1, 1, 2]
  low = 1, mid = 1
  [0, 0, 2, 1, 1, 2]
      ↑        ↑
    low,mid  high

Step 3: nums[mid] = 0
  Swap nums[1] and nums[1]: [0, 0, 2, 1, 1, 2]
  low = 2, mid = 2
  [0, 0, 2, 1, 1, 2]
         ↑     ↑
       low,mid high

Step 4: nums[mid] = 2
  Swap nums[2] and nums[4]: [0, 0, 1, 1, 2, 2]
  high = 3
  [0, 0, 1, 1, 2, 2]
         ↑  ↑
       low,mid high

Step 5: nums[mid] = 1
  mid = 3
  [0, 0, 1, 1, 2, 2]
         ↑  ↑
        low mid,high

Step 6: nums[mid] = 1
  mid = 4
  mid > high, stop

Result: [0, 0, 1, 1, 2, 2]
```

### Complexity Analysis

- **Time Complexity**: O(n) - Single pass
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testSortColors() {
    SortColors solution = new SortColors();
    
    int[] nums1 = {2, 0, 2, 1, 1, 0};
    solution.sortColors(nums1);
    assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2}, nums1);
    
    int[] nums2 = {2, 0, 1};
    solution.sortColors(nums2);
    assertArrayEquals(new int[]{0, 1, 2}, nums2);
}
```

---

## Problem 4: Trapping Rain Water

**Difficulty**: Medium  
**Pattern**: Opposite Direction  
**LeetCode**: #42

### Problem Statement

Given `n` non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.

### Examples

```
Input: height = [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6

Input: height = [4,2,0,3,2,5]
Output: 9
```

### Solution

```java
public class TrappingRainWater {
    public int trap(int[] height) {
        if (height.length == 0) return 0;
        
        int left = 0;
        int right = height.length - 1;
        int leftMax = 0;
        int rightMax = 0;
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
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testTrap() {
    TrappingRainWater solution = new TrappingRainWater();
    
    assertEquals(6, solution.trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}));
    assertEquals(9, solution.trap(new int[]{4, 2, 0, 3, 2, 5}));
}
```

---

## Problem 5: Longest Substring Without Repeating Characters

**Difficulty**: Medium  
**Pattern**: Sliding Window  
**LeetCode**: #3

### Problem Statement

Given a string `s`, find the length of the longest substring without repeating characters.

### Examples

```
Input: s = "abcabcbb"
Output: 3
Explanation: "abc"

Input: s = "bbbbb"
Output: 1
Explanation: "b"

Input: s = "pwwkew"
Output: 3
Explanation: "wke"
```

### Solution

```java
public class LongestSubstringWithoutRepeating {
    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> map = new HashMap<>();
        int left = 0;
        int maxLen = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            
            // If character exists in window, move left
            if (map.containsKey(c)) {
                left = Math.max(left, map.get(c) + 1);
            }
            
            map.put(c, right);
            maxLen = Math.max(maxLen, right - left + 1);
        }
        
        return maxLen;
    }
}
```

### Dry Run

**Input**: `s = "abcabcbb"`

```
Step 1: right = 0, c = 'a'
  map = {a: 0}, left = 0
  maxLen = 1

Step 2: right = 1, c = 'b'
  map = {a: 0, b: 1}, left = 0
  maxLen = 2

Step 3: right = 2, c = 'c'
  map = {a: 0, b: 1, c: 2}, left = 0
  maxLen = 3

Step 4: right = 3, c = 'a'
  'a' exists at index 0
  left = max(0, 0 + 1) = 1
  map = {a: 3, b: 1, c: 2}
  maxLen = 3 (no change)

Step 5: right = 4, c = 'b'
  'b' exists at index 1
  left = max(1, 1 + 1) = 2
  map = {a: 3, b: 4, c: 2}
  maxLen = 3

... continue

Final maxLen = 3
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(min(n, m)) where m is charset size

### Test Cases

```java
@Test
public void testLengthOfLongestSubstring() {
    LongestSubstringWithoutRepeating solution = new LongestSubstringWithoutRepeating();
    
    assertEquals(3, solution.lengthOfLongestSubstring("abcabcbb"));
    assertEquals(1, solution.lengthOfLongestSubstring("bbbbb"));
    assertEquals(3, solution.lengthOfLongestSubstring("pwwkew"));
}
```

---

## Problem 6: Minimum Window Substring

**Difficulty**: Medium  
**Pattern**: Sliding Window  
**LeetCode**: #76

### Problem Statement

Given two strings `s` and `t`, return the minimum window substring of `s` such that every character in `t` (including duplicates) is included in the window. If no such substring exists, return empty string.

### Solution

```java
public class MinimumWindowSubstring {
    public String minWindow(String s, String t) {
        if (s.length() < t.length()) return "";
        
        Map<Character, Integer> need = new HashMap<>();
        Map<Character, Integer> window = new HashMap<>();
        
        for (char c : t.toCharArray()) {
            need.put(c, need.getOrDefault(c, 0) + 1);
        }
        
        int left = 0, right = 0;
        int valid = 0;
        int start = 0, minLen = Integer.MAX_VALUE;
        
        while (right < s.length()) {
            char c = s.charAt(right);
            right++;
            
            if (need.containsKey(c)) {
                window.put(c, window.getOrDefault(c, 0) + 1);
                if (window.get(c).equals(need.get(c))) {
                    valid++;
                }
            }
            
            while (valid == need.size()) {
                if (right - left < minLen) {
                    start = left;
                    minLen = right - left;
                }
                
                char d = s.charAt(left);
                left++;
                
                if (need.containsKey(d)) {
                    if (window.get(d).equals(need.get(d))) {
                        valid--;
                    }
                    window.put(d, window.get(d) - 1);
                }
            }
        }
        
        return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + minLen);
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(|s| + |t|)
- **Space Complexity**: O(|s| + |t|)

### Test Cases

```java
@Test
public void testMinWindow() {
    MinimumWindowSubstring solution = new MinimumWindowSubstring();
    
    assertEquals("BANC", solution.minWindow("ADOBECODEBANC", "ABC"));
    assertEquals("a", solution.minWindow("a", "a"));
    assertEquals("", solution.minWindow("a", "aa"));
}
```

---

## Problem 7: Find All Anagrams in String

**Difficulty**: Medium  
**Pattern**: Sliding Window  
**LeetCode**: #438

### Problem Statement

Given two strings `s` and `p`, return an array of all start indices of `p`'s anagrams in `s`.

### Solution

```java
public class FindAnagrams {
    public List<Integer> findAnagrams(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length()) return result;
        
        int[] pCount = new int[26];
        int[] sCount = new int[26];
        
        for (char c : p.toCharArray()) {
            pCount[c - 'a']++;
        }
        
        int windowSize = p.length();
        
        for (int i = 0; i < s.length(); i++) {
            sCount[s.charAt(i) - 'a']++;
            
            if (i >= windowSize) {
                sCount[s.charAt(i - windowSize) - 'a']--;
            }
            
            if (Arrays.equals(sCount, pCount)) {
                result.add(i - windowSize + 1);
            }
        }
        
        return result;
    }
}
```

### Test Cases

```java
@Test
public void testFindAnagrams() {
    FindAnagrams solution = new FindAnagrams();
    
    assertEquals(Arrays.asList(0, 6), solution.findAnagrams("cbaebabacd", "abc"));
    assertEquals(Arrays.asList(0, 1, 2), solution.findAnagrams("abab", "ab"));
}
```

---

## Problem 8: Subarray Product Less Than K

**Difficulty**: Medium  
**Pattern**: Sliding Window  
**LeetCode**: #713

### Problem Statement

Given an array of integers `nums` and an integer `k`, return the number of contiguous subarrays where the product of all elements is strictly less than `k`.

### Solution

```java
public class SubarrayProductLessThanK {
    public int numSubarrayProductLessThanK(int[] nums, int k) {
        if (k <= 1) return 0;
        
        int left = 0;
        int product = 1;
        int count = 0;
        
        for (int right = 0; right < nums.length; right++) {
            product *= nums[right];
            
            while (product >= k) {
                product /= nums[left];
                left++;
            }
            
            count += right - left + 1;
        }
        
        return count;
    }
}
```

### Test Cases

```java
@Test
public void testNumSubarrayProductLessThanK() {
    SubarrayProductLessThanK solution = new SubarrayProductLessThanK();
    
    assertEquals(8, solution.numSubarrayProductLessThanK(new int[]{10, 5, 2, 6}, 100));
    assertEquals(0, solution.numSubarrayProductLessThanK(new int[]{1, 2, 3}, 0));
}
```

---

## Problem 9: Longest Repeating Character Replacement

**Difficulty**: Medium  
**Pattern**: Sliding Window  
**LeetCode**: #424

### Problem Statement

You are given a string `s` and an integer `k`. You can choose any character and change it to any other uppercase English character. You can perform this operation at most `k` times.

Return the length of the longest substring containing the same letter you can get after performing the above operations.

### Solution

```java
public class LongestRepeatingCharacterReplacement {
    public int characterReplacement(String s, int k) {
        int[] count = new int[26];
        int left = 0;
        int maxCount = 0;
        int maxLen = 0;
        
        for (int right = 0; right < s.length(); right++) {
            count[s.charAt(right) - 'A']++;
            maxCount = Math.max(maxCount, count[s.charAt(right) - 'A']);
            
            while (right - left + 1 - maxCount > k) {
                count[s.charAt(left) - 'A']--;
                left++;
            }
            
            maxLen = Math.max(maxLen, right - left + 1);
        }
        
        return maxLen;
    }
}
```

### Test Cases

```java
@Test
public void testCharacterReplacement() {
    LongestRepeatingCharacterReplacement solution = new LongestRepeatingCharacterReplacement();
    
    assertEquals(4, solution.characterReplacement("ABAB", 2));
    assertEquals(4, solution.characterReplacement("AABABBA", 1));
}
```

---

## Problem 10: 4Sum

**Difficulty**: Medium  
**Pattern**: Opposite Direction + Nested Iteration  
**LeetCode**: #18

### Problem Statement

Given an array `nums` of `n` integers, return an array of all unique quadruplets `[nums[a], nums[b], nums[c], nums[d]]` such that `nums[a] + nums[b] + nums[c] + nums[d] == target`.

### Solution

```java
public class FourSum {
    public List<List<Integer>> fourSum(int[] nums, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);
        
        for (int i = 0; i < nums.length - 3; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            
            for (int j = i + 1; j < nums.length - 2; j++) {
                if (j > i + 1 && nums[j] == nums[j - 1]) continue;
                
                int left = j + 1;
                int right = nums.length - 1;
                
                while (left < right) {
                    long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];
                    
                    if (sum == target) {
                        result.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
                        
                        while (left < right && nums[left] == nums[left + 1]) left++;
                        while (left < right && nums[right] == nums[right - 1]) right--;
                        
                        left++;
                        right--;
                    } else if (sum < target) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
        }
        
        return result;
    }
}
```

### Test Cases

```java
@Test
public void testFourSum() {
    FourSum solution = new FourSum();
    
    List<List<Integer>> result = solution.fourSum(new int[]{1, 0, -1, 0, -2, 2}, 0);
    assertEquals(3, result.size());
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| 3Sum | Opposite + Loop | O(n²) | O(1) | Fix one, find two |
| Container Water | Opposite | O(n) | O(1) | Greedy approach |
| Sort Colors | Three Pointers | O(n) | O(1) | Partitioning |
| Trapping Rain Water | Opposite | O(n) | O(1) | Track max heights |
| Longest Substring | Sliding Window | O(n) | O(n) | Hash map tracking |
| Minimum Window | Sliding Window | O(n) | O(n) | Character frequency |
| Find Anagrams | Sliding Window | O(n) | O(1) | Fixed window |
| Subarray Product | Sliding Window | O(n) | O(1) | Product tracking |
| Character Replacement | Sliding Window | O(n) | O(1) | Max frequency |
| 4Sum | Opposite + Nested | O(n³) | O(1) | Extension of 3Sum |

---

**Next**: [Hard Problems](05_Hard_Problems.md)
