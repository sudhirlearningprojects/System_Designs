# Amazon Interview - Arrays & Strings (60 Problems)

## 📊 Overview
Arrays and Strings represent **20% of all Amazon interview questions**. Master these patterns to clear most coding rounds.

**Frequency**: Very High (Asked in 70%+ of interviews)  
**Total Problems**: 60  
**Difficulty**: 20 Easy, 35 Medium, 5 Hard

---

## 🔥 Top 20 Must-Know Problems

### 1. Two Sum (LC 1) ⭐⭐⭐⭐⭐
**Difficulty**: Easy | **Frequency**: Very High

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
**Time**: O(n) | **Space**: O(n)

---

### 2. Best Time to Buy and Sell Stock I (LC 121) ⭐⭐⭐⭐⭐
**Difficulty**: Easy | **Frequency**: Very High

```java
public int maxProfit(int[] prices) {
    int minPrice = Integer.MAX_VALUE;
    int maxProfit = 0;
    
    for (int price : prices) {
        minPrice = Math.min(minPrice, price);
        maxProfit = Math.max(maxProfit, price - minPrice);
    }
    
    return maxProfit;
}
```
**Time**: O(n) | **Space**: O(1)

---

### 3. Maximum Subarray (LC 53) ⭐⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: Very High

```java
public int maxSubArray(int[] nums) {
    int maxSum = nums[0];
    int currentSum = nums[0];
    
    for (int i = 1; i < nums.length; i++) {
        currentSum = Math.max(nums[i], currentSum + nums[i]);
        maxSum = Math.max(maxSum, currentSum);
    }
    
    return maxSum;
}
```
**Time**: O(n) | **Space**: O(1)

---

### 4. Product of Array Except Self (LC 238) ⭐⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: Very High

```java
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    
    result[0] = 1;
    for (int i = 1; i < n; i++) {
        result[i] = result[i - 1] * nums[i - 1];
    }
    
    int suffix = 1;
    for (int i = n - 1; i >= 0; i--) {
        result[i] *= suffix;
        suffix *= nums[i];
    }
    
    return result;
}
```
**Time**: O(n) | **Space**: O(1)

---

### 5. Container With Most Water (LC 11) ⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: High

```java
public int maxArea(int[] height) {
    int left = 0, right = height.length - 1;
    int maxArea = 0;
    
    while (left < right) {
        int area = Math.min(height[left], height[right]) * (right - left);
        maxArea = Math.max(maxArea, area);
        
        if (height[left] < height[right]) {
            left++;
        } else {
            right--;
        }
    }
    
    return maxArea;
}
```
**Time**: O(n) | **Space**: O(1)

---

### 6. 3Sum (LC 15) ⭐⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: Very High

```java
public List<List<Integer>> threeSum(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);
    
    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;
        
        int left = i + 1, right = nums.length - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            
            if (sum == 0) {
                result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++;
                right--;
            } else if (sum < 0) {
                left++;
            } else {
                right--;
            }
        }
    }
    
    return result;
}
```
**Time**: O(n²) | **Space**: O(1)

---

### 7. Subarray Sum Equals K (LC 560) ⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: High

```java
public int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> map = new HashMap<>();
    map.put(0, 1);
    int sum = 0, count = 0;
    
    for (int num : nums) {
        sum += num;
        if (map.containsKey(sum - k)) {
            count += map.get(sum - k);
        }
        map.put(sum, map.getOrDefault(sum, 0) + 1);
    }
    
    return count;
}
```
**Time**: O(n) | **Space**: O(n)

---

### 8. Merge Intervals (LC 56) ⭐⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: Very High

```java
public int[][] merge(int[][] intervals) {
    if (intervals.length <= 1) return intervals;
    
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    List<int[]> result = new ArrayList<>();
    int[] current = intervals[0];
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] <= current[1]) {
            current[1] = Math.max(current[1], intervals[i][1]);
        } else {
            result.add(current);
            current = intervals[i];
        }
    }
    result.add(current);
    
    return result.toArray(new int[result.size()][]);
}
```
**Time**: O(n log n) | **Space**: O(n)

---

### 9. Longest Substring Without Repeating Characters (LC 3) ⭐⭐⭐⭐⭐
**Difficulty**: Medium | **Frequency**: Very High

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
**Time**: O(n) | **Space**: O(min(n, m))

---

### 10. Minimum Window Substring (LC 76) ⭐⭐⭐⭐
**Difficulty**: Hard | **Frequency**: High

```java
public String minWindow(String s, String t) {
    if (s.length() < t.length()) return "";
    
    Map<Character, Integer> need = new HashMap<>();
    for (char c : t.toCharArray()) {
        need.put(c, need.getOrDefault(c, 0) + 1);
    }
    
    int left = 0, right = 0, formed = 0, required = need.size();
    Map<Character, Integer> window = new HashMap<>();
    int[] result = {-1, 0, 0}; // length, left, right
    
    while (right < s.length()) {
        char c = s.charAt(right);
        window.put(c, window.getOrDefault(c, 0) + 1);
        
        if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) {
            formed++;
        }
        
        while (left <= right && formed == required) {
            c = s.charAt(left);
            
            if (result[0] == -1 || right - left + 1 < result[0]) {
                result[0] = right - left + 1;
                result[1] = left;
                result[2] = right;
            }
            
            window.put(c, window.get(c) - 1);
            if (need.containsKey(c) && window.get(c) < need.get(c)) {
                formed--;
            }
            left++;
        }
        right++;
    }
    
    return result[0] == -1 ? "" : s.substring(result[1], result[2] + 1);
}
```
**Time**: O(|S| + |T|) | **Space**: O(|S| + |T|)

---

## 📋 Complete Problem List (60 Problems)

### Easy (20 problems)
1. Two Sum (1)
2. Best Time to Buy and Sell Stock I (121)
3. Valid Palindrome (125)
4. Valid Anagram (242)
5. Longest Common Prefix (14)
6. Valid Parentheses (20)
7. Implement strStr() (28)
8. Roman to Integer (13)
9. Move Zeroes (283)
10. Missing Number (268)
11. Majority Element (169)
12. Plus One (66)
13. Remove Duplicates from Sorted Array (26)
14. Merge Sorted Array (88)
15. Pascal's Triangle (118)
16. Contains Duplicate (217)
17. Single Number (136)
18. Intersection of Two Arrays II (350)
19. Reverse String (344)
20. Reverse Integer (7)

### Medium (35 problems)
21. Best Time to Buy and Sell Stock II (122)
22. Maximum Subarray (53)
23. Product of Array Except Self (238)
24. Maximum Product Subarray (152)
25. Container With Most Water (11)
26. 3Sum (15)
27. 3Sum Closest (16)
28. Subarray Sum Equals K (560)
29. Longest Consecutive Sequence (128)
30. Merge Intervals (56)
31. Insert Interval (57)
32. Meeting Rooms II (253)
33. Kth Largest Element in Array (215)
34. Top K Frequent Elements (347)
35. Sort Colors (75)
36. Rotate Array (189)
37. Find Minimum in Rotated Sorted Array (153)
38. Search in Rotated Sorted Array (33)
39. Longest Substring Without Repeating Characters (3)
40. Longest Repeating Character Replacement (424)
41. Group Anagrams (49)
42. Longest Palindromic Substring (5)
43. Palindromic Substrings (647)
44. String to Integer (atoi) (8)
45. Integer to Roman (12)
46. Reverse Words in a String (151)
47. Find All Anagrams in a String (438)
48. Permutation in String (567)
49. Encode and Decode Strings (271)
50. Reorganize String (767)
51. Count and Say (38)
52. Compare Version Numbers (165)
53. Next Permutation (31)
54. Majority Element II (229)
55. Set Matrix Zeroes (73)

### Hard (5 problems)
56. Minimum Window Substring (76)
57. First Missing Positive (41)
58. Trapping Rain Water (42)
59. Sliding Window Maximum (239)
60. Spiral Matrix (54)

---

## 🎯 Key Patterns

### 1. Two Pointers
- Two Sum, 3Sum, Container With Most Water
- Trapping Rain Water

### 2. Sliding Window
- Longest Substring Without Repeating
- Minimum Window Substring
- Find All Anagrams

### 3. Hash Map/Set
- Two Sum, Group Anagrams
- Subarray Sum Equals K

### 4. Prefix/Suffix
- Product of Array Except Self

### 5. Kadane's Algorithm
- Maximum Subarray
- Maximum Product Subarray

### 6. Sorting + Greedy
- Merge Intervals
- Meeting Rooms II

---

**Next**: [Linked Lists Problems](02_Linked_Lists.md)
