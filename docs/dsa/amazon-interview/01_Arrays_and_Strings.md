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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/two-sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/key-pair/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/stock-buy-and-sell/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/maximum-subarray/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/kadanes-algorithm/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/product-of-array-except-self/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/product-array-puzzle/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/container-with-most-water/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/container-with-most-water/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/3sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/triplet-sum-in-array/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/subarray-sum-equals-k/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/subarray-with-given-sum/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/merge-intervals/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/overlapping-intervals/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/longest-substring-without-repeating-characters/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/length-of-the-longest-substring/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/minimum-window-substring/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/smallest-window-in-a-string-containing-all-the-characters-of-another-string/)

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
1. Two Sum (1) - 🔗 [LC](https://leetcode.com/problems/two-sum/) | [GFG](https://www.geeksforgeeks.org/problems/key-pair/)
2. Best Time to Buy and Sell Stock I (121) - 🔗 [LC](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | [GFG](https://www.geeksforgeeks.org/problems/stock-buy-and-sell/)
3. Valid Palindrome (125) - 🔗 [LC](https://leetcode.com/problems/valid-palindrome/) | [GFG](https://www.geeksforgeeks.org/problems/palindrome-string/)
4. Valid Anagram (242) - 🔗 [LC](https://leetcode.com/problems/valid-anagram/) | [GFG](https://www.geeksforgeeks.org/problems/anagram/)
5. Longest Common Prefix (14) - 🔗 [LC](https://leetcode.com/problems/longest-common-prefix/) | [GFG](https://www.geeksforgeeks.org/problems/longest-common-prefix-in-an-array/)
6. Valid Parentheses (20) - 🔗 [LC](https://leetcode.com/problems/valid-parentheses/) | [GFG](https://www.geeksforgeeks.org/problems/parenthesis-checker/)
7. Implement strStr() (28) - 🔗 [LC](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) | [GFG](https://www.geeksforgeeks.org/problems/implement-strstr/)
8. Roman to Integer (13) - 🔗 [LC](https://leetcode.com/problems/roman-to-integer/) | [GFG](https://www.geeksforgeeks.org/problems/roman-number-to-integer/)
9. Move Zeroes (283) - 🔗 [LC](https://leetcode.com/problems/move-zeroes/) | [GFG](https://www.geeksforgeeks.org/problems/move-all-zeroes-to-end-of-array/)
10. Missing Number (268) - 🔗 [LC](https://leetcode.com/problems/missing-number/) | [GFG](https://www.geeksforgeeks.org/problems/missing-number-in-array/)
11. Majority Element (169) - 🔗 [LC](https://leetcode.com/problems/majority-element/) | [GFG](https://www.geeksforgeeks.org/problems/majority-element/)
12. Plus One (66) - 🔗 [LC](https://leetcode.com/problems/plus-one/) | [GFG](https://www.geeksforgeeks.org/problems/add-1-to-a-number-represented-as-linked-list/)
13. Remove Duplicates from Sorted Array (26) - 🔗 [LC](https://leetcode.com/problems/remove-duplicates-from-sorted-array/) | [GFG](https://www.geeksforgeeks.org/problems/remove-duplicate-elements-from-sorted-array/)
14. Merge Sorted Array (88) - 🔗 [LC](https://leetcode.com/problems/merge-sorted-array/) | [GFG](https://www.geeksforgeeks.org/problems/merge-two-sorted-arrays/)
15. Pascal's Triangle (118) - 🔗 [LC](https://leetcode.com/problems/pascals-triangle/) | [GFG](https://www.geeksforgeeks.org/problems/pascal-triangle/)
16. Contains Duplicate (217) - 🔗 [LC](https://leetcode.com/problems/contains-duplicate/) | [GFG](https://www.geeksforgeeks.org/problems/check-if-array-contains-duplicates/)
17. Single Number (136) - 🔗 [LC](https://leetcode.com/problems/single-number/) | [GFG](https://www.geeksforgeeks.org/problems/element-appearing-once/)
18. Intersection of Two Arrays II (350) - 🔗 [LC](https://leetcode.com/problems/intersection-of-two-arrays-ii/) | [GFG](https://www.geeksforgeeks.org/problems/intersection-of-two-arrays/)
19. Reverse String (344) - 🔗 [LC](https://leetcode.com/problems/reverse-string/) | [GFG](https://www.geeksforgeeks.org/problems/reverse-a-string/)
20. Reverse Integer (7) - 🔗 [LC](https://leetcode.com/problems/reverse-integer/) | [GFG](https://www.geeksforgeeks.org/problems/reverse-digit/)

### Medium (35 problems)
21. Best Time to Buy and Sell Stock II (122) - 🔗 [LC](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/) | [GFG](https://www.geeksforgeeks.org/problems/stock-buy-and-sell-ii/)
22. Maximum Subarray (53) - 🔗 [LC](https://leetcode.com/problems/maximum-subarray/) | [GFG](https://www.geeksforgeeks.org/problems/kadanes-algorithm/)
23. Product of Array Except Self (238) - 🔗 [LC](https://leetcode.com/problems/product-of-array-except-self/) | [GFG](https://www.geeksforgeeks.org/problems/product-array-puzzle/)
24. Maximum Product Subarray (152) - 🔗 [LC](https://leetcode.com/problems/maximum-product-subarray/) | [GFG](https://www.geeksforgeeks.org/problems/maximum-product-subarray/)
25. Container With Most Water (11) - 🔗 [LC](https://leetcode.com/problems/container-with-most-water/) | [GFG](https://www.geeksforgeeks.org/problems/container-with-most-water/)
26. 3Sum (15) - 🔗 [LC](https://leetcode.com/problems/3sum/) | [GFG](https://www.geeksforgeeks.org/problems/triplet-sum-in-array/)
27. 3Sum Closest (16) - 🔗 [LC](https://leetcode.com/problems/3sum-closest/) | [GFG](https://www.geeksforgeeks.org/problems/3-sum-closest/)
28. Subarray Sum Equals K (560) - 🔗 [LC](https://leetcode.com/problems/subarray-sum-equals-k/) | [GFG](https://www.geeksforgeeks.org/problems/subarray-with-given-sum/)
29. Longest Consecutive Sequence (128) - 🔗 [LC](https://leetcode.com/problems/longest-consecutive-sequence/) | [GFG](https://www.geeksforgeeks.org/problems/longest-consecutive-subsequence/)
30. Merge Intervals (56) - 🔗 [LC](https://leetcode.com/problems/merge-intervals/) | [GFG](https://www.geeksforgeeks.org/problems/overlapping-intervals/)
31. Insert Interval (57) - 🔗 [LC](https://leetcode.com/problems/insert-interval/) | [GFG](https://www.geeksforgeeks.org/problems/insert-interval/)
32. Meeting Rooms II (253) - 🔗 [LC](https://leetcode.com/problems/meeting-rooms-ii/) | [GFG](https://www.geeksforgeeks.org/problems/minimum-platforms/)
33. Kth Largest Element in Array (215) - 🔗 [LC](https://leetcode.com/problems/kth-largest-element-in-an-array/) | [GFG](https://www.geeksforgeeks.org/problems/kth-largest-element/)
34. Top K Frequent Elements (347) - 🔗 [LC](https://leetcode.com/problems/top-k-frequent-elements/) | [GFG](https://www.geeksforgeeks.org/problems/top-k-frequent-elements-in-array/)
35. Sort Colors (75) - 🔗 [LC](https://leetcode.com/problems/sort-colors/) | [GFG](https://www.geeksforgeeks.org/problems/sort-an-array-of-0s-1s-and-2s/)
36. Rotate Array (189) - 🔗 [LC](https://leetcode.com/problems/rotate-array/) | [GFG](https://www.geeksforgeeks.org/problems/rotate-array-by-n-elements/)
37. Find Minimum in Rotated Sorted Array (153) - 🔗 [LC](https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/) | [GFG](https://www.geeksforgeeks.org/problems/minimum-element-in-a-sorted-and-rotated-array/)
38. Search in Rotated Sorted Array (33) - 🔗 [LC](https://leetcode.com/problems/search-in-rotated-sorted-array/) | [GFG](https://www.geeksforgeeks.org/problems/search-in-a-rotated-array/)
39. Longest Substring Without Repeating Characters (3) - 🔗 [LC](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | [GFG](https://www.geeksforgeeks.org/problems/length-of-the-longest-substring/)
40. Longest Repeating Character Replacement (424) - 🔗 [LC](https://leetcode.com/problems/longest-repeating-character-replacement/) | [GFG](https://www.geeksforgeeks.org/problems/longest-repeating-character-replacement/)
41. Group Anagrams (49) - 🔗 [LC](https://leetcode.com/problems/group-anagrams/) | [GFG](https://www.geeksforgeeks.org/problems/print-anagrams-together/)
42. Longest Palindromic Substring (5) - 🔗 [LC](https://leetcode.com/problems/longest-palindromic-substring/) | [GFG](https://www.geeksforgeeks.org/problems/longest-palindrome-in-a-string/)
43. Palindromic Substrings (647) - 🔗 [LC](https://leetcode.com/problems/palindromic-substrings/) | [GFG](https://www.geeksforgeeks.org/problems/count-palindrome-sub-strings-of-a-string/)
44. String to Integer (atoi) (8) - 🔗 [LC](https://leetcode.com/problems/string-to-integer-atoi/) | [GFG](https://www.geeksforgeeks.org/problems/implement-atoi/)
45. Integer to Roman (12) - 🔗 [LC](https://leetcode.com/problems/integer-to-roman/) | [GFG](https://www.geeksforgeeks.org/problems/convert-to-roman-no/)
46. Reverse Words in a String (151) - 🔗 [LC](https://leetcode.com/problems/reverse-words-in-a-string/) | [GFG](https://www.geeksforgeeks.org/problems/reverse-words-in-a-given-string/)
47. Find All Anagrams in a String (438) - 🔗 [LC](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | [GFG](https://www.geeksforgeeks.org/problems/anagram-of-string/)
48. Permutation in String (567) - 🔗 [LC](https://leetcode.com/problems/permutation-in-string/) | [GFG](https://www.geeksforgeeks.org/problems/permutation-in-string/)
49. Encode and Decode Strings (271) - 🔗 [LC](https://leetcode.com/problems/encode-and-decode-strings/) | [GFG](https://www.geeksforgeeks.org/problems/encode-and-decode-strings/)
50. Reorganize String (767) - 🔗 [LC](https://leetcode.com/problems/reorganize-string/) | [GFG](https://www.geeksforgeeks.org/problems/rearrange-characters/)
51. Count and Say (38) - 🔗 [LC](https://leetcode.com/problems/count-and-say/) | [GFG](https://www.geeksforgeeks.org/problems/decode-the-pattern/)
52. Compare Version Numbers (165) - 🔗 [LC](https://leetcode.com/problems/compare-version-numbers/) | [GFG](https://www.geeksforgeeks.org/problems/compare-version-numbers/)
53. Next Permutation (31) - 🔗 [LC](https://leetcode.com/problems/next-permutation/) | [GFG](https://www.geeksforgeeks.org/problems/next-permutation/)
54. Majority Element II (229) - 🔗 [LC](https://leetcode.com/problems/majority-element-ii/) | [GFG](https://www.geeksforgeeks.org/problems/majority-element-ii/)
55. Set Matrix Zeroes (73) - 🔗 [LC](https://leetcode.com/problems/set-matrix-zeroes/) | [GFG](https://www.geeksforgeeks.org/problems/set-matrix-zeroes/)

### Hard (5 problems)
56. Minimum Window Substring (76) - 🔗 [LC](https://leetcode.com/problems/minimum-window-substring/) | [GFG](https://www.geeksforgeeks.org/problems/smallest-window-in-a-string-containing-all-the-characters-of-another-string/)
57. First Missing Positive (41) - 🔗 [LC](https://leetcode.com/problems/first-missing-positive/) | [GFG](https://www.geeksforgeeks.org/problems/smallest-positive-missing-number/)
58. Trapping Rain Water (42) - 🔗 [LC](https://leetcode.com/problems/trapping-rain-water/) | [GFG](https://www.geeksforgeeks.org/problems/trapping-rain-water/)
59. Sliding Window Maximum (239) - 🔗 [LC](https://leetcode.com/problems/sliding-window-maximum/) | [GFG](https://www.geeksforgeeks.org/problems/maximum-of-all-subarrays-of-size-k/)
60. Spiral Matrix (54) - 🔗 [LC](https://leetcode.com/problems/spiral-matrix/) | [GFG](https://www.geeksforgeeks.org/problems/spirally-traversing-a-matrix/)

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
