# Amazon Interview - Backtracking Problems (15 Problems)

## Overview
Backtracking = DFS + Pruning. Build candidates incrementally and abandon ("backtrack") when a candidate cannot lead to a valid solution.

**Pattern Template**:
```java
void backtrack(state, choices) {
    if (isGoal(state)) { result.add(copy(state)); return; }
    for (choice : choices) {
        if (isValid(choice)) {
            makeChoice(choice);
            backtrack(state, remainingChoices);
            undoChoice(choice);  // backtrack
        }
    }
}
```

---

## 1. Permutations (LC 46) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given an array of distinct integers, return all possible permutations.

**Example**: `nums = [1,2,3]` → `[[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]`

### Solution
```java
public List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(result, new ArrayList<>(), nums, new boolean[nums.length]);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, boolean[] used) {
    if (current.size() == nums.length) {
        result.add(new ArrayList<>(current));
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        used[i] = true;
        current.add(nums[i]);
        backtrack(result, current, nums, used);
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```
**Time**: O(n * n!) | **Space**: O(n)

### Dry Run
```
nums = [1, 2, 3]
backtrack([], used=[F,F,F])
  pick 1 → backtrack([1], used=[T,F,F])
    pick 2 → backtrack([1,2], used=[T,T,F])
      pick 3 → backtrack([1,2,3]) → ADD [1,2,3]
    pick 3 → backtrack([1,3], used=[T,F,T])
      pick 2 → backtrack([1,3,2]) → ADD [1,3,2]
  pick 2 → ... → ADD [2,1,3], [2,3,1]
  pick 3 → ... → ADD [3,1,2], [3,2,1]
```

### Test Cases
```java
permute([1,2,3]) → 6 permutations
permute([0,1])   → [[0,1],[1,0]]
permute([1])     → [[1]]
```

### Use Cases
- Generating all possible orderings (task scheduling, route planning)
- Anagram generation
- Password brute-force simulation

---

## 2. Subsets (LC 78) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given an integer array of unique elements, return all possible subsets (power set).

**Example**: `nums = [1,2,3]` → `[[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]`

### Solution
```java
public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(result, new ArrayList<>(), nums, 0);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, int start) {
    result.add(new ArrayList<>(current));
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);
        backtrack(result, current, nums, i + 1);
        current.remove(current.size() - 1);
    }
}
```
**Time**: O(n * 2^n) | **Space**: O(n)

### Dry Run
```
nums = [1,2,3], start=0
ADD []
  pick 1 → ADD [1]
    pick 2 → ADD [1,2]
      pick 3 → ADD [1,2,3]
    pick 3 → ADD [1,3]
  pick 2 → ADD [2]
    pick 3 → ADD [2,3]
  pick 3 → ADD [3]
```

### Test Cases
```java
subsets([1,2,3]) → 8 subsets (2^3)
subsets([0])     → [[], [0]]
subsets([])      → [[]]
```

### Use Cases
- Feature selection in ML
- Finding all possible combinations of items
- Power set generation for combinatorics

---

## 3. Combination Sum (LC 39) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given an array of distinct integers and a target, return all unique combinations that sum to target. Same number can be used unlimited times.

**Example**: `candidates=[2,3,6,7], target=7` → `[[2,2,3],[7]]`

### Solution
```java
public List<List<Integer>> combinationSum(int[] candidates, int target) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(candidates);
    backtrack(result, new ArrayList<>(), candidates, target, 0);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] candidates, int remaining, int start) {
    if (remaining == 0) { result.add(new ArrayList<>(current)); return; }
    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > remaining) break; // pruning
        current.add(candidates[i]);
        backtrack(result, current, candidates, remaining - candidates[i], i); // i not i+1 (reuse allowed)
        current.remove(current.size() - 1);
    }
}
```
**Time**: O(n^(T/M)) where T=target, M=min candidate | **Space**: O(T/M)

### Dry Run
```
candidates=[2,3,6,7], target=7
backtrack([], remaining=7, start=0)
  pick 2 → backtrack([2], remaining=5, start=0)
    pick 2 → backtrack([2,2], remaining=3, start=0)
      pick 2 → backtrack([2,2,2], remaining=1, start=0)
        pick 2 → remaining=-1, skip
        pick 3 → remaining=-2, break
      pick 3 → backtrack([2,2,3], remaining=0) → ADD [2,2,3]
    pick 3 → backtrack([2,3], remaining=2, start=1)
      pick 3 → remaining=-1, break
  pick 3 → backtrack([3], remaining=4, start=1)
    pick 3 → backtrack([3,3], remaining=1) → no valid
  pick 6 → backtrack([6], remaining=1) → no valid
  pick 7 → backtrack([7], remaining=0) → ADD [7]
```

### Test Cases
```java
combinationSum([2,3,6,7], 7) → [[2,2,3],[7]]
combinationSum([2,3,5], 8)   → [[2,2,2,2],[2,3,3],[3,5]]
combinationSum([2], 1)       → []
```

### Use Cases
- Change-making problem
- Budget allocation with repeatable items
- Recipe combinations

---

## 4. Generate Parentheses (LC 22) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given n pairs of parentheses, generate all combinations of well-formed parentheses.

**Example**: `n=3` → `["((()))","(()())","(())()","()(())","()()()"]`

### Solution
```java
public List<String> generateParenthesis(int n) {
    List<String> result = new ArrayList<>();
    backtrack(result, new StringBuilder(), 0, 0, n);
    return result;
}

private void backtrack(List<String> result, StringBuilder current, int open, int close, int n) {
    if (current.length() == 2 * n) { result.add(current.toString()); return; }
    if (open < n) {
        current.append('(');
        backtrack(result, current, open + 1, close, n);
        current.deleteCharAt(current.length() - 1);
    }
    if (close < open) {
        current.append(')');
        backtrack(result, current, open, close + 1, n);
        current.deleteCharAt(current.length() - 1);
    }
}
```
**Time**: O(4^n / sqrt(n)) | **Space**: O(n)

### Dry Run
```
n=2
backtrack("", open=0, close=0)
  add '(' → backtrack("(", open=1, close=0)
    add '(' → backtrack("((", open=2, close=0)
      add ')' → backtrack("(()", open=2, close=1)
        add ')' → backtrack("(())", len=4) → ADD "(())"
    add ')' → backtrack("()", open=1, close=1)
      add '(' → backtrack("()(", open=2, close=1)
        add ')' → backtrack("()()", len=4) → ADD "()()"
```

### Test Cases
```java
generateParenthesis(1) → ["()"]
generateParenthesis(2) → ["(())", "()()"]
generateParenthesis(3) → 5 combinations
```

### Use Cases
- Code formatter/validator
- Expression tree generation
- Compiler design (balanced bracket generation)

---

## 5. Word Search (LC 79) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Problem
Given an m×n grid of characters and a string word, return true if word exists in the grid (adjacent cells, no reuse).

**Example**: `board=[["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word="ABCCED"` → `true`

### Solution
```java
public boolean exist(char[][] board, String word) {
    int m = board.length, n = board[0].length;
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (dfs(board, word, i, j, 0)) return true;
    return false;
}

private boolean dfs(char[][] board, String word, int i, int j, int k) {
    if (k == word.length()) return true;
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length || board[i][j] != word.charAt(k))
        return false;
    char temp = board[i][j];
    board[i][j] = '#'; // mark visited
    boolean found = dfs(board, word, i+1, j, k+1) || dfs(board, word, i-1, j, k+1)
                 || dfs(board, word, i, j+1, k+1) || dfs(board, word, i, j-1, k+1);
    board[i][j] = temp; // restore
    return found;
}
```
**Time**: O(m * n * 4^L) where L=word length | **Space**: O(L)

### Test Cases
```java
exist([["A","B"],["C","D"]], "ABDC") → true
exist([["A","B"],["C","D"]], "ABCD") → false
exist([["A"]], "A") → true
```

### Use Cases
- Boggle game solver
- Text pattern matching in 2D grids
- Maze path finding

---

## 6. Palindrome Partitioning (LC 131) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Problem
Given a string s, partition it such that every substring is a palindrome. Return all possible partitions.

**Example**: `s="aab"` → `[["a","a","b"],["aa","b"]]`

### Solution
```java
public List<List<String>> partition(String s) {
    List<List<String>> result = new ArrayList<>();
    backtrack(result, new ArrayList<>(), s, 0);
    return result;
}

private void backtrack(List<List<String>> result, List<String> current, String s, int start) {
    if (start == s.length()) { result.add(new ArrayList<>(current)); return; }
    for (int end = start + 1; end <= s.length(); end++) {
        String sub = s.substring(start, end);
        if (isPalindrome(sub)) {
            current.add(sub);
            backtrack(result, current, s, end);
            current.remove(current.size() - 1);
        }
    }
}

private boolean isPalindrome(String s) {
    int l = 0, r = s.length() - 1;
    while (l < r) if (s.charAt(l++) != s.charAt(r--)) return false;
    return true;
}
```
**Time**: O(n * 2^n) | **Space**: O(n)

### Test Cases
```java
partition("aab") → [["a","a","b"],["aa","b"]]
partition("a")   → [["a"]]
partition("ab")  → [["a","b"]]
```

---

## 7. N-Queens (LC 51) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Problem
Place n queens on an n×n chessboard such that no two queens attack each other.

**Example**: `n=4` → `[[".Q..","...Q","Q...","..Q."],["..Q.","Q...","...Q",".Q.."]]`

### Solution
```java
public List<List<String>> solveNQueens(int n) {
    List<List<String>> result = new ArrayList<>();
    char[][] board = new char[n][n];
    for (char[] row : board) Arrays.fill(row, '.');
    backtrack(result, board, 0, n);
    return result;
}

private void backtrack(List<List<String>> result, char[][] board, int row, int n) {
    if (row == n) { result.add(buildBoard(board)); return; }
    for (int col = 0; col < n; col++) {
        if (isValid(board, row, col, n)) {
            board[row][col] = 'Q';
            backtrack(result, board, row + 1, n);
            board[row][col] = '.';
        }
    }
}

private boolean isValid(char[][] board, int row, int col, int n) {
    for (int i = 0; i < row; i++) if (board[i][col] == 'Q') return false;
    for (int i = row-1, j = col-1; i >= 0 && j >= 0; i--, j--) if (board[i][j] == 'Q') return false;
    for (int i = row-1, j = col+1; i >= 0 && j < n; i--, j++) if (board[i][j] == 'Q') return false;
    return true;
}

private List<String> buildBoard(char[][] board) {
    List<String> res = new ArrayList<>();
    for (char[] row : board) res.add(new String(row));
    return res;
}
```
**Time**: O(n!) | **Space**: O(n²)

### Test Cases
```java
solveNQueens(1) → [["Q"]]
solveNQueens(4) → 2 solutions
solveNQueens(8) → 92 solutions
```

---

## 8. Letter Combinations of Phone Number (LC 17) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Problem
Given a string of digits 2-9, return all possible letter combinations.

**Example**: `digits="23"` → `["ad","ae","af","bd","be","bf","cd","ce","cf"]`

### Solution
```java
public List<String> letterCombinations(String digits) {
    if (digits.isEmpty()) return new ArrayList<>();
    String[] phone = {"", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"};
    List<String> result = new ArrayList<>();
    backtrack(result, new StringBuilder(), digits, 0, phone);
    return result;
}

private void backtrack(List<String> result, StringBuilder current, String digits, int index, String[] phone) {
    if (index == digits.length()) { result.add(current.toString()); return; }
    for (char c : phone[digits.charAt(index) - '0'].toCharArray()) {
        current.append(c);
        backtrack(result, current, digits, index + 1, phone);
        current.deleteCharAt(current.length() - 1);
    }
}
```
**Time**: O(4^n * n) | **Space**: O(n)

### Test Cases
```java
letterCombinations("23") → ["ad","ae","af","bd","be","bf","cd","ce","cf"]
letterCombinations("")   → []
letterCombinations("2")  → ["a","b","c"]
```

### Use Cases
- T9 keyboard autocomplete
- Phone number word generation (e.g., 1-800-FLOWERS)

---

## 9. Subsets II (LC 90) - With Duplicates ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Problem
Given an integer array that may contain duplicates, return all possible subsets without duplicates.

**Example**: `nums=[1,2,2]` → `[[],[1],[1,2],[1,2,2],[2],[2,2]]`

### Solution
```java
public List<List<Integer>> subsetsWithDup(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);
    backtrack(result, new ArrayList<>(), nums, 0);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, int start) {
    result.add(new ArrayList<>(current));
    for (int i = start; i < nums.length; i++) {
        if (i > start && nums[i] == nums[i - 1]) continue; // skip duplicates
        current.add(nums[i]);
        backtrack(result, current, nums, i + 1);
        current.remove(current.size() - 1);
    }
}
```
**Time**: O(n * 2^n) | **Space**: O(n)

---

## 10. Permutations II (LC 47) - With Duplicates ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public List<List<Integer>> permuteUnique(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);
    backtrack(result, new ArrayList<>(), nums, new boolean[nums.length]);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] nums, boolean[] used) {
    if (current.size() == nums.length) { result.add(new ArrayList<>(current)); return; }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        if (i > 0 && nums[i] == nums[i-1] && !used[i-1]) continue; // skip duplicates
        used[i] = true;
        current.add(nums[i]);
        backtrack(result, current, nums, used);
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

---

## 11. Combination Sum II (LC 40) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Problem
Each number may only be used once. Find all unique combinations that sum to target.

### Solution
```java
public List<List<Integer>> combinationSum2(int[] candidates, int target) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(candidates);
    backtrack(result, new ArrayList<>(), candidates, target, 0);
    return result;
}

private void backtrack(List<List<Integer>> result, List<Integer> current, int[] candidates, int remaining, int start) {
    if (remaining == 0) { result.add(new ArrayList<>(current)); return; }
    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > remaining) break;
        if (i > start && candidates[i] == candidates[i-1]) continue; // skip duplicates
        current.add(candidates[i]);
        backtrack(result, current, candidates, remaining - candidates[i], i + 1);
        current.remove(current.size() - 1);
    }
}
```

---

## 12. Restore IP Addresses (LC 93) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Problem
Given a string of digits, return all valid IP addresses.

**Example**: `s="25525511135"` → `["255.255.11.135","255.255.111.35"]`

### Solution
```java
public List<String> restoreIpAddresses(String s) {
    List<String> result = new ArrayList<>();
    backtrack(result, new ArrayList<>(), s, 0);
    return result;
}

private void backtrack(List<String> result, List<String> parts, String s, int start) {
    if (parts.size() == 4 && start == s.length()) { result.add(String.join(".", parts)); return; }
    if (parts.size() == 4 || start == s.length()) return;
    for (int len = 1; len <= 3; len++) {
        if (start + len > s.length()) break;
        String part = s.substring(start, start + len);
        if (part.length() > 1 && part.charAt(0) == '0') break; // no leading zeros
        if (Integer.parseInt(part) > 255) break;
        parts.add(part);
        backtrack(result, parts, s, start + len);
        parts.remove(parts.size() - 1);
    }
}
```

---

## 13. Sudoku Solver (LC 37) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Solution
```java
public void solveSudoku(char[][] board) {
    solve(board);
}

private boolean solve(char[][] board) {
    for (int i = 0; i < 9; i++) {
        for (int j = 0; j < 9; j++) {
            if (board[i][j] == '.') {
                for (char c = '1'; c <= '9'; c++) {
                    if (isValid(board, i, j, c)) {
                        board[i][j] = c;
                        if (solve(board)) return true;
                        board[i][j] = '.';
                    }
                }
                return false; // no valid digit found
            }
        }
    }
    return true; // all cells filled
}

private boolean isValid(char[][] board, int row, int col, char c) {
    for (int i = 0; i < 9; i++) {
        if (board[row][i] == c) return false;
        if (board[i][col] == c) return false;
        if (board[3*(row/3) + i/3][3*(col/3) + i%3] == c) return false;
    }
    return true;
}
```
**Time**: O(9^m) where m = empty cells | **Space**: O(m)

---

## 14. Expression Add Operators (LC 282) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Problem
Given a string of digits and a target, add operators (+, -, *) to make the expression equal target.

**Example**: `num="123", target=6` → `["1+2+3","1*2*3"]`

### Solution
```java
public List<String> addOperators(String num, int target) {
    List<String> result = new ArrayList<>();
    backtrack(result, num, target, new StringBuilder(), 0, 0, 0);
    return result;
}

private void backtrack(List<String> result, String num, int target, StringBuilder expr, int pos, long eval, long mult) {
    if (pos == num.length()) {
        if (eval == target) result.add(expr.toString());
        return;
    }
    for (int len = 1; len <= num.length() - pos; len++) {
        String curr = num.substring(pos, pos + len);
        if (curr.length() > 1 && curr.charAt(0) == '0') break; // no leading zeros
        long val = Long.parseLong(curr);
        int exprLen = expr.length();
        if (pos == 0) {
            backtrack(result, num, target, expr.append(curr), len, val, val);
        } else {
            backtrack(result, num, target, expr.append('+').append(curr), pos + len, eval + val, val);
            expr.setLength(exprLen);
            backtrack(result, num, target, expr.append('-').append(curr), pos + len, eval - val, -val);
            expr.setLength(exprLen);
            backtrack(result, num, target, expr.append('*').append(curr), pos + len, eval - mult + mult * val, mult * val);
        }
        expr.setLength(exprLen);
    }
}
```

---

## 15. Word Search II (LC 212) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: High

### Problem
Given a board and a list of words, find all words in the board.

### Solution (Trie + Backtracking)
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    String word = null;
}

public List<String> findWords(char[][] board, String[] words) {
    TrieNode root = buildTrie(words);
    List<String> result = new ArrayList<>();
    for (int i = 0; i < board.length; i++)
        for (int j = 0; j < board[0].length; j++)
            dfs(board, i, j, root, result);
    return result;
}

private void dfs(char[][] board, int i, int j, TrieNode node, List<String> result) {
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length || board[i][j] == '#') return;
    char c = board[i][j];
    TrieNode next = node.children[c - 'a'];
    if (next == null) return;
    if (next.word != null) { result.add(next.word); next.word = null; } // avoid duplicates
    board[i][j] = '#';
    dfs(board, i+1, j, next, result); dfs(board, i-1, j, next, result);
    dfs(board, i, j+1, next, result); dfs(board, i, j-1, next, result);
    board[i][j] = c;
}

private TrieNode buildTrie(String[] words) {
    TrieNode root = new TrieNode();
    for (String word : words) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) node.children[c - 'a'] = new TrieNode();
            node = node.children[c - 'a'];
        }
        node.word = word;
    }
    return root;
}
```
**Time**: O(m * n * 4 * 3^(L-1)) | **Space**: O(total chars in words)

---

## Pattern Summary

| Problem | Key Insight | Time |
|---------|-------------|------|
| Permutations | `used[]` array to track | O(n * n!) |
| Subsets | Start index prevents reuse | O(n * 2^n) |
| Combination Sum | Reuse: pass `i` not `i+1` | O(n^(T/M)) |
| Generate Parentheses | open < n, close < open | O(4^n/√n) |
| Word Search | Mark visited with `#` | O(m*n*4^L) |
| N-Queens | Check col + diagonals | O(n!) |

**Next**: [Heap & Greedy Problems](09_Heap_and_Greedy.md)
