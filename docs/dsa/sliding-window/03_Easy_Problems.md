# Sliding Window - Easy Problems (40%)

## 📚 8 Easy Problems with Complete Solutions

---

## Problem 1: Maximum Sum Subarray of Size K

**Difficulty**: Easy  
**Pattern**: Fixed Window  
**Similar**: LeetCode #643 (Maximum Average Subarray I)

### Problem Statement

Given an array of integers and a number K, find the maximum sum of any contiguous subarray of size K.

**Constraints**:
- 1 ≤ arr.length ≤ 10⁵
- -10⁴ ≤ arr[i] ≤ 10⁴
- 1 ≤ K ≤ arr.length

### Examples

```
Input: arr = [2, 1, 5, 1, 3, 2], K = 3
Output: 9
Explanation: Subarray [5, 1, 3] has maximum sum 9

Input: arr = [2, 3, 4, 1, 5], K = 2
Output: 7
Explanation: Subarray [3, 4] has maximum sum 7
```

### Solution

```java
public class MaxSumSubarrayOfSizeK {
    public int maxSum(int[] arr, int k) {
        if (arr.length < k) return -1;
        
        // Calculate sum of first window
        int windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += arr[i];
        }
        
        int maxSum = windowSum;
        
        // Slide the window
        for (int i = k; i < arr.length; i++) {
            windowSum = windowSum - arr[i - k] + arr[i];
            maxSum = Math.max(maxSum, windowSum);
        }
        
        return maxSum;
    }
}
```

### Dry Run

**Input**: `arr = [2, 1, 5, 1, 3, 2]`, `K = 3`

```
Step 1: Build first window [2, 1, 5]
[2, 1, 5] 1, 3, 2
 |-----|
windowSum = 2 + 1 + 5 = 8
maxSum = 8

Step 2: Slide to [1, 5, 1]
2, [1, 5, 1] 3, 2
    |-----|
Remove arr[0] = 2, Add arr[3] = 1
windowSum = 8 - 2 + 1 = 7
maxSum = 8 (no change)

Step 3: Slide to [5, 1, 3]
2, 1, [5, 1, 3] 2
       |-----|
Remove arr[1] = 1, Add arr[4] = 3
windowSum = 7 - 1 + 3 = 9
maxSum = 9 ✓

Step 4: Slide to [1, 3, 2]
2, 1, 5, [1, 3, 2]
          |-----|
Remove arr[2] = 5, Add arr[5] = 2
windowSum = 9 - 5 + 2 = 6
maxSum = 9 (no change)

Final Answer: 9
```

### Complexity Analysis

- **Time Complexity**: O(n) - Single pass through array
- **Space Complexity**: O(1) - Only using variables

### Test Cases

```java
@Test
public void testMaxSum() {
    MaxSumSubarrayOfSizeK solution = new MaxSumSubarrayOfSizeK();
    
    assertEquals(9, solution.maxSum(new int[]{2, 1, 5, 1, 3, 2}, 3));
    assertEquals(7, solution.maxSum(new int[]{2, 3, 4, 1, 5}, 2));
    assertEquals(12, solution.maxSum(new int[]{1, 4, 2, 10, 23, 3, 1, 0, 20}, 4));
    assertEquals(5, solution.maxSum(new int[]{5}, 1));
    assertEquals(-1, solution.maxSum(new int[]{1, 2}, 3)); // K > length
}
```

---

## Problem 2: Average of Subarrays of Size K

**Difficulty**: Easy  
**Pattern**: Fixed Window  
**LeetCode**: #643

### Problem Statement

Given an array, find the average of all contiguous subarrays of size K.

### Examples

```
Input: arr = [1, 3, 2, 6, -1, 4, 1, 8, 2], K = 5
Output: [2.2, 2.8, 2.4, 3.6, 2.8]
```

### Solution

```java
public class AverageOfSubarrays {
    public double[] findAverages(int[] arr, int k) {
        double[] result = new double[arr.length - k + 1];
        
        double windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += arr[i];
        }
        result[0] = windowSum / k;
        
        for (int i = k; i < arr.length; i++) {
            windowSum = windowSum - arr[i - k] + arr[i];
            result[i - k + 1] = windowSum / k;
        }
        
        return result;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(n) for result array

### Test Cases

```java
@Test
public void testFindAverages() {
    AverageOfSubarrays solution = new AverageOfSubarrays();
    
    double[] result = solution.findAverages(new int[]{1, 3, 2, 6, -1, 4, 1, 8, 2}, 5);
    assertArrayEquals(new double[]{2.2, 2.8, 2.4, 3.6, 2.8}, result, 0.01);
}
```

---

## Problem 3: Contains Duplicate II

**Difficulty**: Easy  
**Pattern**: Fixed Window with Set  
**LeetCode**: #219

### Problem Statement

Given an integer array `nums` and an integer `k`, return `true` if there are two distinct indices `i` and `j` such that `nums[i] == nums[j]` and `abs(i - j) <= k`.

### Examples

```
Input: nums = [1,2,3,1], k = 3
Output: true

Input: nums = [1,0,1,1], k = 1
Output: true

Input: nums = [1,2,3,1,2,3], k = 2
Output: false
```

### Solution

```java
public class ContainsDuplicateII {
    public boolean containsNearbyDuplicate(int[] nums, int k) {
        Set<Integer> window = new HashSet<>();
        
        for (int i = 0; i < nums.length; i++) {
            // If window size exceeds k, remove leftmost element
            if (i > k) {
                window.remove(nums[i - k - 1]);
            }
            
            // If current element already in window, found duplicate
            if (!window.add(nums[i])) {
                return true;
            }
        }
        
        return false;
    }
}
```

### Dry Run

**Input**: `nums = [1, 2, 3, 1]`, `k = 3`

```
Step 1: i = 0, nums[0] = 1
  window = {}
  Add 1 → window = {1}
  
Step 2: i = 1, nums[1] = 2
  window = {1}
  Add 2 → window = {1, 2}
  
Step 3: i = 2, nums[2] = 3
  window = {1, 2}
  Add 3 → window = {1, 2, 3}
  
Step 4: i = 3, nums[3] = 1
  window = {1, 2, 3}
  Try to add 1 → Already exists!
  Return true ✓
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(min(n, k))

### Test Cases

```java
@Test
public void testContainsNearbyDuplicate() {
    ContainsDuplicateII solution = new ContainsDuplicateII();
    
    assertTrue(solution.containsNearbyDuplicate(new int[]{1, 2, 3, 1}, 3));
    assertTrue(solution.containsNearbyDuplicate(new int[]{1, 0, 1, 1}, 1));
    assertFalse(solution.containsNearbyDuplicate(new int[]{1, 2, 3, 1, 2, 3}, 2));
}
```

---

## Problem 4: Maximum Average Subarray I

**Difficulty**: Easy  
**Pattern**: Fixed Window  
**LeetCode**: #643

### Problem Statement

You are given an integer array `nums` consisting of `n` elements, and an integer `k`. Find a contiguous subarray whose length is equal to `k` that has the maximum average value and return this value.

### Solution

```java
public class MaximumAverageSubarray {
    public double findMaxAverage(int[] nums, int k) {
        double sum = 0;
        for (int i = 0; i < k; i++) {
            sum += nums[i];
        }
        
        double maxSum = sum;
        
        for (int i = k; i < nums.length; i++) {
            sum = sum - nums[i - k] + nums[i];
            maxSum = Math.max(maxSum, sum);
        }
        
        return maxSum / k;
    }
}
```

### Test Cases

```java
@Test
public void testFindMaxAverage() {
    MaximumAverageSubarray solution = new MaximumAverageSubarray();
    
    assertEquals(12.75, solution.findMaxAverage(new int[]{1, 12, -5, -6, 50, 3}, 4), 0.01);
    assertEquals(5.0, solution.findMaxAverage(new int[]{5}, 1), 0.01);
}
```

---

## Problem 5: Minimum Size Subarray Sum

**Difficulty**: Easy/Medium  
**Pattern**: Variable Window  
**LeetCode**: #209

### Problem Statement

Given an array of positive integers `nums` and a positive integer `target`, return the minimal length of a contiguous subarray whose sum is greater than or equal to `target`. If no such subarray exists, return 0.

### Examples

```
Input: target = 7, nums = [2,3,1,2,4,3]
Output: 2
Explanation: [4,3] has minimal length

Input: target = 4, nums = [1,4,4]
Output: 1

Input: target = 11, nums = [1,1,1,1,1,1,1,1]
Output: 0
```

### Solution

```java
public class MinimumSizeSubarraySum {
    public int minSubArrayLen(int target, int[] nums) {
        int left = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE;
        
        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];
            
            while (sum >= target) {
                minLen = Math.min(minLen, right - left + 1);
                sum -= nums[left];
                left++;
            }
        }
        
        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }
}
```

### Dry Run

**Input**: `target = 7`, `nums = [2, 3, 1, 2, 4, 3]`

```
Step 1: right = 0, nums[0] = 2
  sum = 2, left = 0
  2 < 7, continue
  
Step 2: right = 1, nums[1] = 3
  sum = 5, left = 0
  5 < 7, continue
  
Step 3: right = 2, nums[2] = 1
  sum = 6, left = 0
  6 < 7, continue
  
Step 4: right = 3, nums[3] = 2
  sum = 8, left = 0
  8 >= 7 ✓
  minLen = 4 (window [2,3,1,2])
  
  Contract: sum -= nums[0] = 2, sum = 6, left = 1
  6 < 7, stop contracting
  
Step 5: right = 4, nums[4] = 4
  sum = 10, left = 1
  10 >= 7 ✓
  minLen = 4 (window [3,1,2,4])
  
  Contract: sum -= nums[1] = 3, sum = 7, left = 2
  7 >= 7 ✓
  minLen = 3 (window [1,2,4])
  
  Contract: sum -= nums[2] = 1, sum = 6, left = 3
  6 < 7, stop
  
Step 6: right = 5, nums[5] = 3
  sum = 9, left = 3
  9 >= 7 ✓
  minLen = 3 (window [2,4,3])
  
  Contract: sum -= nums[3] = 2, sum = 7, left = 4
  7 >= 7 ✓
  minLen = 2 (window [4,3]) ✓
  
  Contract: sum -= nums[4] = 4, sum = 3, left = 5
  3 < 7, stop

Final Answer: 2
```

### Complexity Analysis

- **Time Complexity**: O(n) - Each element added and removed once
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testMinSubArrayLen() {
    MinimumSizeSubarraySum solution = new MinimumSizeSubarraySum();
    
    assertEquals(2, solution.minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}));
    assertEquals(1, solution.minSubArrayLen(4, new int[]{1, 4, 4}));
    assertEquals(0, solution.minSubArrayLen(11, new int[]{1, 1, 1, 1, 1, 1, 1, 1}));
}
```

---

## Problem 6: Find All Anagrams in String

**Difficulty**: Easy/Medium  
**Pattern**: Fixed Window with Hash Map  
**LeetCode**: #438

### Problem Statement

Given two strings `s` and `p`, return an array of all the start indices of `p`'s anagrams in `s`.

### Examples

```
Input: s = "cbaebabacd", p = "abc"
Output: [0,6]
Explanation: 
  "cba" at index 0 is an anagram of "abc"
  "bac" at index 6 is an anagram of "abc"

Input: s = "abab", p = "ab"
Output: [0,1,2]
```

### Solution

```java
public class FindAllAnagrams {
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
            // Add current character to window
            sCount[s.charAt(i) - 'a']++;
            
            // Remove character outside window
            if (i >= windowSize) {
                sCount[s.charAt(i - windowSize) - 'a']--;
            }
            
            // Check if current window is anagram
            if (Arrays.equals(sCount, pCount)) {
                result.add(i - windowSize + 1);
            }
        }
        
        return result;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n) where n is length of s
- **Space Complexity**: O(1) - Fixed size arrays (26)

### Test Cases

```java
@Test
public void testFindAnagrams() {
    FindAllAnagrams solution = new FindAllAnagrams();
    
    assertEquals(Arrays.asList(0, 6), solution.findAnagrams("cbaebabacd", "abc"));
    assertEquals(Arrays.asList(0, 1, 2), solution.findAnagrams("abab", "ab"));
}
```

---

## Problem 7: Longest Nice Substring

**Difficulty**: Easy  
**Pattern**: Brute Force with Sliding Window Concept  
**LeetCode**: #1763

### Problem Statement

A string is nice if, for every letter of the alphabet that the string contains, it appears both in uppercase and lowercase. Return the longest substring that is nice. If there are multiple, return the substring of the earliest occurrence.

### Examples

```
Input: s = "YazaAay"
Output: "aAa"

Input: s = "Bb"
Output: "Bb"

Input: s = "c"
Output: ""
```

### Solution

```java
public class LongestNiceSubstring {
    public String longestNiceSubstring(String s) {
        if (s.length() < 2) return "";
        
        String result = "";
        
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + 1; j <= s.length(); j++) {
                String sub = s.substring(i, j);
                if (isNice(sub) && sub.length() > result.length()) {
                    result = sub;
                }
            }
        }
        
        return result;
    }
    
    private boolean isNice(String s) {
        Set<Character> set = new HashSet<>();
        for (char c : s.toCharArray()) {
            set.add(c);
        }
        
        for (char c : s.toCharArray()) {
            if (Character.isLowerCase(c)) {
                if (!set.contains(Character.toUpperCase(c))) {
                    return false;
                }
            } else {
                if (!set.contains(Character.toLowerCase(c))) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
```

### Test Cases

```java
@Test
public void testLongestNiceSubstring() {
    LongestNiceSubstring solution = new LongestNiceSubstring();
    
    assertEquals("aAa", solution.longestNiceSubstring("YazaAay"));
    assertEquals("Bb", solution.longestNiceSubstring("Bb"));
    assertEquals("", solution.longestNiceSubstring("c"));
}
```

---

## Problem 8: Defanging an IP Address

**Difficulty**: Easy  
**Pattern**: String Processing (Warm-up)  
**LeetCode**: #1108

### Problem Statement

Given a valid IP address, return a defanged version where every period "." is replaced with "[.]".

### Solution

```java
public class DefangIPAddress {
    public String defangIPaddr(String address) {
        return address.replace(".", "[.]");
    }
    
    // Alternative: Using StringBuilder
    public String defangIPaddrManual(String address) {
        StringBuilder sb = new StringBuilder();
        for (char c : address.toCharArray()) {
            if (c == '.') {
                sb.append("[.]");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
```

### Test Cases

```java
@Test
public void testDefangIPaddr() {
    DefangIPAddress solution = new DefangIPAddress();
    
    assertEquals("1[.]1[.]1[.]1", solution.defangIPaddr("1.1.1.1"));
    assertEquals("255[.]100[.]50[.]0", solution.defangIPaddr("255.100.50.0"));
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Max Sum Size K | Fixed Window | O(n) | O(1) | Slide and update sum |
| Average Subarrays | Fixed Window | O(n) | O(n) | Calculate averages |
| Contains Duplicate II | Fixed Window + Set | O(n) | O(k) | Track window elements |
| Max Average Subarray | Fixed Window | O(n) | O(1) | Find maximum average |
| Min Size Subarray Sum | Variable Window | O(n) | O(1) | Expand and contract |
| Find All Anagrams | Fixed + Hash Map | O(n) | O(1) | Character frequency |
| Longest Nice Substring | Brute Force | O(n³) | O(n) | Check all substrings |
| Defang IP Address | String Processing | O(n) | O(n) | Simple replacement |

---

**Next**: [Medium Problems](04_Medium_Problems.md)
