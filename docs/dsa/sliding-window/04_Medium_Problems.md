# Sliding Window - Medium Problems (50%)

## 📚 10 Medium Problems with Complete Solutions

---

## Problem 1: Longest Substring Without Repeating Characters

**Difficulty**: Medium  
**Pattern**: Variable Window with Set  
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

Input: s = "pwwkew"
Output: 3
Explanation: "wke"
```

### Solution

```java
public class LongestSubstringWithoutRepeating {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> window = new HashSet<>();
        int left = 0;
        int maxLen = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            
            // Contract window while duplicate exists
            while (window.contains(c)) {
                window.remove(s.charAt(left));
                left++;
            }
            
            window.add(c);
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
  window = {}
  Add 'a' → window = {a}
  maxLen = 1

Step 2: right = 1, c = 'b'
  window = {a}
  Add 'b' → window = {a, b}
  maxLen = 2

Step 3: right = 2, c = 'c'
  window = {a, b}
  Add 'c' → window = {a, b, c}
  maxLen = 3

Step 4: right = 3, c = 'a'
  window = {a, b, c}
  'a' exists! Contract:
    Remove s[0] = 'a', left = 1
    window = {b, c}
  Add 'a' → window = {b, c, a}
  maxLen = 3

Step 5: right = 4, c = 'b'
  window = {b, c, a}
  'b' exists! Contract:
    Remove s[1] = 'b', left = 2
    window = {c, a}
  Add 'b' → window = {c, a, b}
  maxLen = 3

... continue

Final maxLen = 3
```

### Complexity Analysis

- **Time Complexity**: O(n) - Each character added and removed once
- **Space Complexity**: O(min(n, m)) where m is charset size

### Test Cases

```java
@Test
public void testLengthOfLongestSubstring() {
    LongestSubstringWithoutRepeating solution = new LongestSubstringWithoutRepeating();
    
    assertEquals(3, solution.lengthOfLongestSubstring("abcabcbb"));
    assertEquals(1, solution.lengthOfLongestSubstring("bbbbb"));
    assertEquals(3, solution.lengthOfLongestSubstring("pwwkew"));
    assertEquals(0, solution.lengthOfLongestSubstring(""));
    assertEquals(1, solution.lengthOfLongestSubstring(" "));
}
```

---

## Problem 2: Longest Repeating Character Replacement

**Difficulty**: Medium  
**Pattern**: Variable Window with Frequency Map  
**LeetCode**: #424

### Problem Statement

You are given a string `s` and an integer `k`. You can choose any character and change it to any other uppercase English character. You can perform this operation at most `k` times. Return the length of the longest substring containing the same letter you can get after performing the above operations.

### Examples

```
Input: s = "ABAB", k = 2
Output: 4
Explanation: Replace two 'A's with 'B's or vice versa

Input: s = "AABABBA", k = 1
Output: 4
Explanation: Replace one 'A' with 'B' to get "AABBBBA"
```

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
            
            // If window size - most frequent char > k, contract
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

### Dry Run

**Input**: `s = "AABABBA"`, `k = 1`

```
Step 1: right = 0, s[0] = 'A'
  count[A] = 1, maxCount = 1
  windowSize = 1, replacements needed = 1 - 1 = 0 ≤ 1 ✓
  maxLen = 1

Step 2: right = 1, s[1] = 'A'
  count[A] = 2, maxCount = 2
  windowSize = 2, replacements = 2 - 2 = 0 ≤ 1 ✓
  maxLen = 2

Step 3: right = 2, s[2] = 'B'
  count[B] = 1, maxCount = 2 (still A)
  windowSize = 3, replacements = 3 - 2 = 1 ≤ 1 ✓
  maxLen = 3

Step 4: right = 3, s[3] = 'A'
  count[A] = 3, maxCount = 3
  windowSize = 4, replacements = 4 - 3 = 1 ≤ 1 ✓
  maxLen = 4 ✓

Step 5: right = 4, s[4] = 'B'
  count[B] = 2, maxCount = 3
  windowSize = 5, replacements = 5 - 3 = 2 > 1 ✗
  Contract: remove s[0] = 'A', count[A] = 2, left = 1
  windowSize = 4, replacements = 4 - 3 = 1 ≤ 1 ✓
  maxLen = 4

... continue

Final maxLen = 4
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1) - Fixed size array (26)

### Test Cases

```java
@Test
public void testCharacterReplacement() {
    LongestRepeatingCharacterReplacement solution = new LongestRepeatingCharacterReplacement();
    
    assertEquals(4, solution.characterReplacement("ABAB", 2));
    assertEquals(4, solution.characterReplacement("AABABBA", 1));
    assertEquals(5, solution.characterReplacement("AAAA", 2));
}
```

---

## Problem 3: Permutation in String

**Difficulty**: Medium  
**Pattern**: Fixed Window with Frequency Map  
**LeetCode**: #567

### Problem Statement

Given two strings `s1` and `s2`, return `true` if `s2` contains a permutation of `s1`, or `false` otherwise.

### Examples

```
Input: s1 = "ab", s2 = "eidbaooo"
Output: true
Explanation: s2 contains "ba" which is a permutation of "ab"

Input: s1 = "ab", s2 = "eidboaoo"
Output: false
```

### Solution

```java
public class PermutationInString {
    public boolean checkInclusion(String s1, String s2) {
        if (s1.length() > s2.length()) return false;
        
        int[] s1Count = new int[26];
        int[] s2Count = new int[26];
        
        for (char c : s1.toCharArray()) {
            s1Count[c - 'a']++;
        }
        
        int windowSize = s1.length();
        
        for (int i = 0; i < s2.length(); i++) {
            s2Count[s2.charAt(i) - 'a']++;
            
            if (i >= windowSize) {
                s2Count[s2.charAt(i - windowSize) - 'a']--;
            }
            
            if (Arrays.equals(s1Count, s2Count)) {
                return true;
            }
        }
        
        return false;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n) where n is length of s2
- **Space Complexity**: O(1) - Fixed size arrays

### Test Cases

```java
@Test
public void testCheckInclusion() {
    PermutationInString solution = new PermutationInString();
    
    assertTrue(solution.checkInclusion("ab", "eidbaooo"));
    assertFalse(solution.checkInclusion("ab", "eidboaoo"));
    assertTrue(solution.checkInclusion("abc", "bbbca"));
}
```

---

## Problem 4: Fruit Into Baskets

**Difficulty**: Medium  
**Pattern**: Variable Window with Hash Map  
**LeetCode**: #904

### Problem Statement

You are visiting a farm with trees in a row. Each tree has a type represented by an integer. You have two baskets, and each basket can only hold one type of fruit. Starting from any tree, you must pick exactly one fruit from every tree while moving to the right. Return the maximum number of fruits you can collect.

### Examples

```
Input: fruits = [1,2,1]
Output: 3
Explanation: Pick all fruits

Input: fruits = [0,1,2,2]
Output: 3
Explanation: Pick [1,2,2]

Input: fruits = [1,2,3,2,2]
Output: 4
Explanation: Pick [2,3,2,2]
```

### Solution

```java
public class FruitIntoBaskets {
    public int totalFruit(int[] fruits) {
        Map<Integer, Integer> basket = new HashMap<>();
        int left = 0;
        int maxFruits = 0;
        
        for (int right = 0; right < fruits.length; right++) {
            basket.put(fruits[right], basket.getOrDefault(fruits[right], 0) + 1);
            
            // Contract while more than 2 types
            while (basket.size() > 2) {
                int leftFruit = fruits[left];
                basket.put(leftFruit, basket.get(leftFruit) - 1);
                if (basket.get(leftFruit) == 0) {
                    basket.remove(leftFruit);
                }
                left++;
            }
            
            maxFruits = Math.max(maxFruits, right - left + 1);
        }
        
        return maxFruits;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1) - At most 3 entries in map

### Test Cases

```java
@Test
public void testTotalFruit() {
    FruitIntoBaskets solution = new FruitIntoBaskets();
    
    assertEquals(3, solution.totalFruit(new int[]{1, 2, 1}));
    assertEquals(3, solution.totalFruit(new int[]{0, 1, 2, 2}));
    assertEquals(4, solution.totalFruit(new int[]{1, 2, 3, 2, 2}));
}
```

---

## Problem 5: Longest Substring with At Most K Distinct Characters

**Difficulty**: Medium  
**Pattern**: Variable Window with Hash Map  
**Similar**: LeetCode #340 (Premium)

### Problem Statement

Given a string `s` and an integer `k`, return the length of the longest substring that contains at most `k` distinct characters.

### Examples

```
Input: s = "eceba", k = 2
Output: 3
Explanation: "ece"

Input: s = "aa", k = 1
Output: 2
```

### Solution

```java
public class LongestSubstringKDistinct {
    public int lengthOfLongestSubstringKDistinct(String s, int k) {
        if (k == 0) return 0;
        
        Map<Character, Integer> map = new HashMap<>();
        int left = 0;
        int maxLen = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            map.put(c, map.getOrDefault(c, 0) + 1);
            
            while (map.size() > k) {
                char leftChar = s.charAt(left);
                map.put(leftChar, map.get(leftChar) - 1);
                if (map.get(leftChar) == 0) {
                    map.remove(leftChar);
                }
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
public void testLengthOfLongestSubstringKDistinct() {
    LongestSubstringKDistinct solution = new LongestSubstringKDistinct();
    
    assertEquals(3, solution.lengthOfLongestSubstringKDistinct("eceba", 2));
    assertEquals(2, solution.lengthOfLongestSubstringKDistinct("aa", 1));
    assertEquals(5, solution.lengthOfLongestSubstringKDistinct("aaabb", 2));
}
```

---

## Problem 6: Max Consecutive Ones III

**Difficulty**: Medium  
**Pattern**: Variable Window  
**LeetCode**: #1004

### Problem Statement

Given a binary array `nums` and an integer `k`, return the maximum number of consecutive 1's if you can flip at most `k` 0's.

### Examples

```
Input: nums = [1,1,1,0,0,0,1,1,1,1,0], k = 2
Output: 6
Explanation: Flip 0's at indices 4 and 5

Input: nums = [0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1], k = 3
Output: 10
```

### Solution

```java
public class MaxConsecutiveOnesIII {
    public int longestOnes(int[] nums, int k) {
        int left = 0;
        int zeros = 0;
        int maxLen = 0;
        
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                zeros++;
            }
            
            while (zeros > k) {
                if (nums[left] == 0) {
                    zeros--;
                }
                left++;
            }
            
            maxLen = Math.max(maxLen, right - left + 1);
        }
        
        return maxLen;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testLongestOnes() {
    MaxConsecutiveOnesIII solution = new MaxConsecutiveOnesIII();
    
    assertEquals(6, solution.longestOnes(new int[]{1,1,1,0,0,0,1,1,1,1,0}, 2));
    assertEquals(10, solution.longestOnes(new int[]{0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1}, 3));
}
```

---

## Problem 7: Subarray Product Less Than K

**Difficulty**: Medium  
**Pattern**: Variable Window  
**LeetCode**: #713

### Problem Statement

Given an array of integers `nums` and an integer `k`, return the number of contiguous subarrays where the product of all elements is strictly less than `k`.

### Examples

```
Input: nums = [10,5,2,6], k = 100
Output: 8
Explanation: [10], [5], [2], [6], [10,5], [5,2], [2,6], [5,2,6]

Input: nums = [1,2,3], k = 0
Output: 0
```

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
            
            // All subarrays ending at right with start >= left
            count += right - left + 1;
        }
        
        return count;
    }
}
```

### Dry Run

**Input**: `nums = [10, 5, 2, 6]`, `k = 100`

```
Step 1: right = 0, nums[0] = 10
  product = 10, left = 0
  10 < 100 ✓
  count += 1 (subarray: [10])
  count = 1

Step 2: right = 1, nums[1] = 5
  product = 50, left = 0
  50 < 100 ✓
  count += 2 (subarrays: [5], [10,5])
  count = 3

Step 3: right = 2, nums[2] = 2
  product = 100, left = 0
  100 >= 100, contract:
    product /= 10, product = 10, left = 1
  10 < 100 ✓
  count += 2 (subarrays: [2], [5,2])
  count = 5

Step 4: right = 3, nums[3] = 6
  product = 60, left = 1
  60 < 100 ✓
  count += 3 (subarrays: [6], [2,6], [5,2,6])
  count = 8

Final count = 8
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

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

## Problem 8: Minimum Window Substring

**Difficulty**: Medium/Hard  
**Pattern**: Variable Window with Hash Map  
**LeetCode**: #76

### Problem Statement

Given two strings `s` and `t`, return the minimum window substring of `s` such that every character in `t` (including duplicates) is included in the window. If no such substring exists, return empty string.

### Examples

```
Input: s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"

Input: s = "a", t = "a"
Output: "a"

Input: s = "a", t = "aa"
Output: ""
```

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

## Problem 9: Grumpy Bookstore Owner

**Difficulty**: Medium  
**Pattern**: Fixed Window  
**LeetCode**: #1052

### Problem Statement

There is a bookstore owner with `n` minutes. For each minute, some customers enter the store. You are given an integer array `customers` and a binary array `grumpy` where `grumpy[i]` is 1 if the owner is grumpy during minute `i`, and 0 otherwise.

The owner can suppress grumpiness for `minutes` consecutive minutes. Return the maximum number of customers that can be satisfied.

### Solution

```java
public class GrumpyBookstoreOwner {
    public int maxSatisfied(int[] customers, int[] grumpy, int minutes) {
        int satisfied = 0;
        
        // Calculate already satisfied customers
        for (int i = 0; i < customers.length; i++) {
            if (grumpy[i] == 0) {
                satisfied += customers[i];
            }
        }
        
        // Find best window to use technique
        int maxExtra = 0;
        int windowExtra = 0;
        
        for (int i = 0; i < customers.length; i++) {
            if (grumpy[i] == 1) {
                windowExtra += customers[i];
            }
            
            if (i >= minutes && grumpy[i - minutes] == 1) {
                windowExtra -= customers[i - minutes];
            }
            
            maxExtra = Math.max(maxExtra, windowExtra);
        }
        
        return satisfied + maxExtra;
    }
}
```

### Test Cases

```java
@Test
public void testMaxSatisfied() {
    GrumpyBookstoreOwner solution = new GrumpyBookstoreOwner();
    
    assertEquals(16, solution.maxSatisfied(
        new int[]{1,0,1,2,1,1,7,5}, 
        new int[]{0,1,0,1,0,1,0,1}, 
        3));
}
```

---

## Problem 10: Get Equal Substrings Within Budget

**Difficulty**: Medium  
**Pattern**: Variable Window  
**LeetCode**: #1208

### Problem Statement

You are given two strings `s` and `t` of the same length and an integer `maxCost`. You want to change `s` to `t`. The cost of changing the `i-th` character is `|s[i] - t[i]|`. Return the maximum length of a substring that can be changed within `maxCost`.

### Solution

```java
public class GetEqualSubstringsWithinBudget {
    public int equalSubstring(String s, String t, int maxCost) {
        int left = 0;
        int cost = 0;
        int maxLen = 0;
        
        for (int right = 0; right < s.length(); right++) {
            cost += Math.abs(s.charAt(right) - t.charAt(right));
            
            while (cost > maxCost) {
                cost -= Math.abs(s.charAt(left) - t.charAt(left));
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
public void testEqualSubstring() {
    GetEqualSubstringsWithinBudget solution = new GetEqualSubstringsWithinBudget();
    
    assertEquals(3, solution.equalSubstring("abcd", "bcdf", 3));
    assertEquals(1, solution.equalSubstring("abcd", "cdef", 3));
    assertEquals(1, solution.equalSubstring("abcd", "acde", 0));
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Longest Substring No Repeat | Variable + Set | O(n) | O(n) | Track unique chars |
| Character Replacement | Variable + Array | O(n) | O(1) | Max frequency tracking |
| Permutation in String | Fixed + Array | O(n) | O(1) | Frequency matching |
| Fruit Into Baskets | Variable + Map | O(n) | O(1) | At most 2 types |
| K Distinct Characters | Variable + Map | O(n) | O(k) | Track distinct count |
| Max Consecutive Ones III | Variable | O(n) | O(1) | Count zeros |
| Subarray Product | Variable | O(n) | O(1) | Count subarrays |
| Minimum Window | Variable + Map | O(n) | O(n) | Match all characters |
| Grumpy Bookstore | Fixed | O(n) | O(1) | Maximize extra |
| Equal Substrings Budget | Variable | O(n) | O(1) | Cost tracking |

---

**Next**: [Hard Problems](05_Hard_Problems.md)
