# Backtracking - Medium Problems (50%)

## 📚 10 Medium Problems with Complete Solutions

---

## Problem 1: Combination Sum

**Difficulty**: Medium  
**Pattern**: Backtracking with Reuse  
**LeetCode**: #39

### Problem Statement

Given an array of distinct integers `candidates` and a target integer `target`, return all unique combinations where the chosen numbers sum to `target`. The same number may be chosen unlimited times.

### Solution

```java
public class CombinationSum {
    public List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates);
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int[] candidates, int remain, int start,
                          List<Integer> current, List<List<Integer>> result) {
        if (remain == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] > remain) break;
            
            current.add(candidates[i]);
            backtrack(candidates, remain - candidates[i], i, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(2^target)
- **Space Complexity**: O(target)

---

## Problem 2: Combination Sum II

**Difficulty**: Medium  
**Pattern**: Backtracking with Duplicates  
**LeetCode**: #40

### Problem Statement

Given a collection of candidate numbers and a target, find all unique combinations where each number may be used only once.

### Solution

```java
public class CombinationSumII {
    public List<List<Integer>> combinationSum2(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates);
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int[] candidates, int remain, int start,
                          List<Integer> current, List<List<Integer>> result) {
        if (remain == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < candidates.length; i++) {
            if (i > start && candidates[i] == candidates[i-1]) continue;
            if (candidates[i] > remain) break;
            
            current.add(candidates[i]);
            backtrack(candidates, remain - candidates[i], i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

---

## Problem 3: Permutations II

**Difficulty**: Medium  
**Pattern**: Permutations with Duplicates  
**LeetCode**: #47

### Problem Statement

Given a collection of numbers that might contain duplicates, return all possible unique permutations.

### Solution

```java
public class PermutationsII {
    public List<List<Integer>> permuteUnique(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);
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
            if (i > 0 && nums[i] == nums[i-1] && !used[i-1]) continue;
            
            current.add(nums[i]);
            used[i] = true;
            backtrack(nums, current, used, result);
            current.remove(current.size() - 1);
            used[i] = false;
        }
    }
}
```

---

## Problem 4: Subsets II

**Difficulty**: Medium  
**Pattern**: Subsets with Duplicates  
**LeetCode**: #90

### Problem Statement

Given an integer array that may contain duplicates, return all possible subsets (the power set). The solution must not contain duplicate subsets.

### Solution

```java
public class SubsetsII {
    public List<List<Integer>> subsetsWithDup(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int[] nums, int start, List<Integer> current,
                          List<List<Integer>> result) {
        result.add(new ArrayList<>(current));
        
        for (int i = start; i < nums.length; i++) {
            if (i > start && nums[i] == nums[i-1]) continue;
            
            current.add(nums[i]);
            backtrack(nums, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
```

---

## Problem 5: Word Search

**Difficulty**: Medium  
**Pattern**: Grid Backtracking  
**LeetCode**: #79

### Problem Statement

Given an `m x n` grid of characters and a string `word`, return `true` if `word` exists in the grid.

### Solution

```java
public class WordSearch {
    public boolean exist(char[][] board, String word) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (backtrack(board, word, i, j, 0)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean backtrack(char[][] board, String word, int row, int col, int index) {
        if (index == word.length()) return true;
        
        if (row < 0 || row >= board.length || col < 0 || col >= board[0].length ||
            board[row][col] != word.charAt(index)) {
            return false;
        }
        
        char temp = board[row][col];
        board[row][col] = '#';
        
        boolean found = backtrack(board, word, row+1, col, index+1) ||
                       backtrack(board, word, row-1, col, index+1) ||
                       backtrack(board, word, row, col+1, index+1) ||
                       backtrack(board, word, row, col-1, index+1);
        
        board[row][col] = temp;
        return found;
    }
}
```

---

## Problem 6: N-Queens

**Difficulty**: Hard  
**Pattern**: Constraint Satisfaction  
**LeetCode**: #51

### Problem Statement

Place `n` queens on an `n x n` chessboard such that no two queens attack each other. Return all distinct solutions.

### Solution

```java
public class NQueens {
    public List<List<String>> solveNQueens(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];
        for (char[] row : board) Arrays.fill(row, '.');
        
        backtrack(board, 0, result);
        return result;
    }
    
    private void backtrack(char[][] board, int row, List<List<String>> result) {
        if (row == board.length) {
            result.add(construct(board));
            return;
        }
        
        for (int col = 0; col < board.length; col++) {
            if (isValid(board, row, col)) {
                board[row][col] = 'Q';
                backtrack(board, row + 1, result);
                board[row][col] = '.';
            }
        }
    }
    
    private boolean isValid(char[][] board, int row, int col) {
        for (int i = 0; i < row; i++) {
            if (board[i][col] == 'Q') return false;
        }
        
        for (int i = row-1, j = col-1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j] == 'Q') return false;
        }
        
        for (int i = row-1, j = col+1; i >= 0 && j < board.length; i--, j++) {
            if (board[i][j] == 'Q') return false;
        }
        
        return true;
    }
    
    private List<String> construct(char[][] board) {
        List<String> result = new ArrayList<>();
        for (char[] row : board) {
            result.add(new String(row));
        }
        return result;
    }
}
```

---

## Problem 7: Sudoku Solver

**Difficulty**: Hard  
**Pattern**: Constraint Satisfaction  
**LeetCode**: #37

### Problem Statement

Write a program to solve a Sudoku puzzle by filling the empty cells.

### Solution

```java
public class SudokuSolver {
    public void solveSudoku(char[][] board) {
        backtrack(board, 0, 0);
    }
    
    private boolean backtrack(char[][] board, int row, int col) {
        if (row == 9) return true;
        if (col == 9) return backtrack(board, row + 1, 0);
        if (board[row][col] != '.') return backtrack(board, row, col + 1);
        
        for (char c = '1'; c <= '9'; c++) {
            if (isValid(board, row, col, c)) {
                board[row][col] = c;
                
                if (backtrack(board, row, col + 1)) {
                    return true;
                }
                
                board[row][col] = '.';
            }
        }
        
        return false;
    }
    
    private boolean isValid(char[][] board, int row, int col, char c) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == c) return false;
            if (board[i][col] == c) return false;
            if (board[3*(row/3) + i/3][3*(col/3) + i%3] == c) return false;
        }
        return true;
    }
}
```

---

## Problem 8: Letter Combinations of Phone Number

**Difficulty**: Medium  
**Pattern**: Backtracking  
**LeetCode**: #17

### Problem Statement

Given a string containing digits from 2-9, return all possible letter combinations that the number could represent.

### Solution

```java
public class LetterCombinations {
    private static final String[] KEYS = {
        "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };
    
    public List<String> letterCombinations(String digits) {
        List<String> result = new ArrayList<>();
        if (digits.isEmpty()) return result;
        
        backtrack(digits, 0, new StringBuilder(), result);
        return result;
    }
    
    private void backtrack(String digits, int index, StringBuilder current,
                          List<String> result) {
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }
        
        String letters = KEYS[digits.charAt(index) - '0'];
        for (char c : letters.toCharArray()) {
            current.append(c);
            backtrack(digits, index + 1, current, result);
            current.deleteCharAt(current.length() - 1);
        }
    }
}
```

---

## Problem 9: Restore IP Addresses

**Difficulty**: Medium  
**Pattern**: Backtracking  
**LeetCode**: #93

### Problem Statement

Given a string containing only digits, return all possible valid IP addresses that can be formed by inserting dots.

### Solution

```java
public class RestoreIPAddresses {
    public List<String> restoreIpAddresses(String s) {
        List<String> result = new ArrayList<>();
        backtrack(s, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(String s, int start, List<String> segments,
                          List<String> result) {
        if (segments.size() == 4) {
            if (start == s.length()) {
                result.add(String.join(".", segments));
            }
            return;
        }
        
        for (int len = 1; len <= 3 && start + len <= s.length(); len++) {
            String segment = s.substring(start, start + len);
            
            if (isValid(segment)) {
                segments.add(segment);
                backtrack(s, start + len, segments, result);
                segments.remove(segments.size() - 1);
            }
        }
    }
    
    private boolean isValid(String segment) {
        if (segment.length() > 1 && segment.charAt(0) == '0') return false;
        int val = Integer.parseInt(segment);
        return val >= 0 && val <= 255;
    }
}
```

---

## Problem 10: Partition to K Equal Sum Subsets

**Difficulty**: Medium  
**Pattern**: Partition  
**LeetCode**: #698

### Problem Statement

Given an integer array `nums` and an integer `k`, return `true` if it is possible to divide this array into `k` non-empty subsets whose sums are all equal.

### Solution

```java
public class PartitionKSubsets {
    public boolean canPartitionKSubsets(int[] nums, int k) {
        int sum = 0;
        for (int num : nums) sum += num;
        
        if (sum % k != 0) return false;
        
        Arrays.sort(nums);
        int target = sum / k;
        int[] buckets = new int[k];
        
        return backtrack(nums, nums.length - 1, buckets, target);
    }
    
    private boolean backtrack(int[] nums, int index, int[] buckets, int target) {
        if (index < 0) {
            for (int bucket : buckets) {
                if (bucket != target) return false;
            }
            return true;
        }
        
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] + nums[index] <= target) {
                buckets[i] += nums[index];
                
                if (backtrack(nums, index - 1, buckets, target)) {
                    return true;
                }
                
                buckets[i] -= nums[index];
            }
            
            if (buckets[i] == 0) break;
        }
        
        return false;
    }
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Combination Sum | Backtracking | O(2^t) | O(t) | Reuse elements |
| Combination Sum II | Backtracking | O(2^n) | O(n) | Skip duplicates |
| Permutations II | Permutations | O(n!) | O(n) | Handle duplicates |
| Subsets II | Subsets | O(2^n) | O(n) | Skip duplicates |
| Word Search | Grid | O(m×n×4^L) | O(L) | Mark visited |
| N-Queens | Constraint | O(n!) | O(n) | Check validity |
| Sudoku Solver | Constraint | O(9^m) | O(m) | Fill empty cells |
| Letter Combinations | Backtracking | O(4^n) | O(n) | Phone mapping |
| Restore IP | Backtracking | O(1) | O(1) | Validate segments |
| Partition K Subsets | Partition | O(k^n) | O(n) | Equal sum groups |

---

**Next**: [Hard Problems](05_Hard_Problems.md)
