# Backtracking - Easy Problems (40%)

## 📚 8 Easy Problems with Complete Solutions

---

## Problem 1: Subsets

**Difficulty**: Easy/Medium  
**Pattern**: Subsets  
**LeetCode**: #78

### Problem Statement

Given an integer array `nums` of unique elements, return all possible subsets (the power set). The solution set must not contain duplicate subsets.

### Examples

```
Input: nums = [1,2,3]
Output: [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]

Input: nums = [0]
Output: [[],[0]]
```

### Solution

```java
public class Subsets {
    public List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int[] nums, int start, List<Integer> current, 
                          List<List<Integer>> result) {
        result.add(new ArrayList<>(current));
        
        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);
            backtrack(nums, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(2^n) - 2^n subsets
- **Space Complexity**: O(n) - Recursion depth

### Test Cases

```java
@Test
public void testSubsets() {
    Subsets solution = new Subsets();
    
    List<List<Integer>> result = solution.subsets(new int[]{1,2,3});
    assertEquals(8, result.size());
    assertTrue(result.contains(Arrays.asList()));
    assertTrue(result.contains(Arrays.asList(1,2,3)));
}
```

---

## Problem 2: Permutations

**Difficulty**: Medium  
**Pattern**: Permutations  
**LeetCode**: #46

### Problem Statement

Given an array `nums` of distinct integers, return all possible permutations.

### Solution

```java
public class Permutations {
    public List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(nums, new ArrayList<>(), new boolean[nums.length], result);
        return result;
    }
    
    private void backtrack(int[] nums, List<Integer> current, boolean[] used,
                          List<List<Integer>> result) {
        if (current.size() == nums.length) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            
            current.add(nums[i]);
            used[i] = true;
            backtrack(nums, current, used, result);
            current.remove(current.size() - 1);
            used[i] = false;
        }
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n!)
- **Space Complexity**: O(n)

---

## Problem 3: Combinations

**Difficulty**: Medium  
**Pattern**: Combinations  
**LeetCode**: #77

### Problem Statement

Given two integers `n` and `k`, return all possible combinations of `k` numbers chosen from the range [1, n].

### Solution

```java
public class Combinations {
    public List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(n, k, 1, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int n, int k, int start, List<Integer> current,
                          List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i <= n; i++) {
            current.add(i);
            backtrack(n, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

---

## Problem 4: Letter Case Permutation

**Difficulty**: Easy  
**Pattern**: Backtracking  
**LeetCode**: #784

### Problem Statement

Given a string `s`, you can transform every letter individually to be lowercase or uppercase to create another string. Return a list of all possible strings.

### Solution

```java
public class LetterCasePermutation {
    public List<String> letterCasePermutation(String s) {
        List<String> result = new ArrayList<>();
        backtrack(s.toCharArray(), 0, result);
        return result;
    }
    
    private void backtrack(char[] chars, int index, List<String> result) {
        if (index == chars.length) {
            result.add(new String(chars));
            return;
        }
        
        backtrack(chars, index + 1, result);
        
        if (Character.isLetter(chars[index])) {
            chars[index] = Character.isLowerCase(chars[index]) ? 
                          Character.toUpperCase(chars[index]) : 
                          Character.toLowerCase(chars[index]);
            backtrack(chars, index + 1, result);
        }
    }
}
```

---

## Problem 5: Generate Parentheses

**Difficulty**: Medium  
**Pattern**: Backtracking  
**LeetCode**: #22

### Problem Statement

Given `n` pairs of parentheses, write a function to generate all combinations of well-formed parentheses.

### Solution

```java
public class GenerateParentheses {
    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        backtrack(n, 0, 0, new StringBuilder(), result);
        return result;
    }
    
    private void backtrack(int n, int open, int close, StringBuilder current,
                          List<String> result) {
        if (current.length() == 2 * n) {
            result.add(current.toString());
            return;
        }
        
        if (open < n) {
            current.append('(');
            backtrack(n, open + 1, close, current, result);
            current.deleteCharAt(current.length() - 1);
        }
        
        if (close < open) {
            current.append(')');
            backtrack(n, open, close + 1, current, result);
            current.deleteCharAt(current.length() - 1);
        }
    }
}
```

---

## Problem 6: Binary Watch

**Difficulty**: Easy  
**Pattern**: Backtracking  
**LeetCode**: #401

### Problem Statement

A binary watch has 4 LEDs on the top (hours) and 6 LEDs on the bottom (minutes). Given an integer `turnedOn` representing the number of LEDs that are currently on, return all possible times the watch could represent.

### Solution

```java
public class BinaryWatch {
    public List<String> readBinaryWatch(int turnedOn) {
        List<String> result = new ArrayList<>();
        
        for (int h = 0; h < 12; h++) {
            for (int m = 0; m < 60; m++) {
                if (Integer.bitCount(h) + Integer.bitCount(m) == turnedOn) {
                    result.add(String.format("%d:%02d", h, m));
                }
            }
        }
        
        return result;
    }
}
```

---

## Problem 7: Combination Sum III

**Difficulty**: Medium  
**Pattern**: Combinations  
**LeetCode**: #216

### Problem Statement

Find all valid combinations of `k` numbers that sum up to `n` such that:
- Only numbers 1 through 9 are used
- Each number is used at most once

### Solution

```java
public class CombinationSumIII {
    public List<List<Integer>> combinationSum3(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(k, n, 1, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int k, int remain, int start, List<Integer> current,
                          List<List<Integer>> result) {
        if (current.size() == k && remain == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        if (current.size() >= k || remain <= 0) {
            return;
        }
        
        for (int i = start; i <= 9; i++) {
            if (i > remain) break;
            
            current.add(i);
            backtrack(k, remain - i, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

---

## Problem 8: Palindrome Partitioning

**Difficulty**: Medium  
**Pattern**: Partition  
**LeetCode**: #131

### Problem Statement

Given a string `s`, partition `s` such that every substring of the partition is a palindrome. Return all possible palindrome partitioning of `s`.

### Solution

```java
public class PalindromePartitioning {
    public List<List<String>> partition(String s) {
        List<List<String>> result = new ArrayList<>();
        backtrack(s, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(String s, int start, List<String> current,
                          List<List<String>> result) {
        if (start == s.length()) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int end = start + 1; end <= s.length(); end++) {
            String substring = s.substring(start, end);
            if (isPalindrome(substring)) {
                current.add(substring);
                backtrack(s, end, current, result);
                current.remove(current.size() - 1);
            }
        }
    }
    
    private boolean isPalindrome(String s) {
        int left = 0, right = s.length() - 1;
        while (left < right) {
            if (s.charAt(left++) != s.charAt(right--)) {
                return false;
            }
        }
        return true;
    }
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Subsets | Subsets | O(2^n) | O(n) | Include/exclude |
| Permutations | Permutations | O(n!) | O(n) | Track used |
| Combinations | Combinations | O(C(n,k)) | O(k) | Choose k |
| Letter Case | Backtracking | O(2^n) | O(n) | Toggle case |
| Parentheses | Backtracking | O(4^n/√n) | O(n) | Valid pairs |
| Binary Watch | Enumeration | O(1) | O(1) | Bit count |
| Combination Sum III | Combinations | O(C(9,k)) | O(k) | Sum constraint |
| Palindrome Partition | Partition | O(n×2^n) | O(n) | Check palindrome |

---

**Next**: [Medium Problems](04_Medium_Problems.md)
