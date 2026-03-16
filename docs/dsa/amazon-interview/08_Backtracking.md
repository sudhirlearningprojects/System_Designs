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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/permutations/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/permutations-of-a-given-string/)

### Problem Statement
Given an array `nums` of distinct integers, return all possible permutations. You can return the answer in any order.

A permutation is an arrangement of all the elements in a specific order. For example, [1,2,3] has 6 permutations.

**Constraints**:
- 1 <= nums.length <= 6
- -10 <= nums[i] <= 10
- All integers in nums are unique

**Examples**:

**Example 1**:
```
Input: nums = [1,2,3]
Output: [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
Explanation: There are 6 permutations of 3 distinct numbers.
```

**Example 2**:
```
Input: nums = [0,1]
Output: [[0,1],[1,0]]
Explanation: There are 2 permutations of 2 distinct numbers.
```

**Example 3**:
```
Input: nums = [1]
Output: [[1]]
Explanation: Only one permutation for a single element.
```

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
// Test Case 1: Standard case with 3 elements
Input: nums = [1,2,3]
Output: [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
Expected: 6 permutations (3! = 6)

// Test Case 2: Two elements
Input: nums = [0,1]
Output: [[0,1],[1,0]]
Expected: 2 permutations (2! = 2)

// Test Case 3: Single element
Input: nums = [1]
Output: [[1]]
Expected: 1 permutation (1! = 1)

// Test Case 4: Negative numbers
Input: nums = [-1,0,1]
Output: [[-1,0,1],[-1,1,0],[0,-1,1],[0,1,-1],[1,-1,0],[1,0,-1]]
Expected: 6 permutations

// Test Case 5: Maximum size
Input: nums = [1,2,3,4,5,6]
Output: 720 permutations (6! = 720)
Expected: All 720 unique permutations
```

### Use Cases
- Generating all possible orderings (task scheduling, route planning)
- Anagram generation
- Password brute-force simulation

---

## 2. Subsets (LC 78) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/subsets/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/subsets/)

### Problem Statement
Given an integer array `nums` of unique elements, return all possible subsets (the power set).

The solution set must not contain duplicate subsets. Return the solution in any order.

A subset is a selection of elements (possibly none or all) from the original array.

**Constraints**:
- 1 <= nums.length <= 10
- -10 <= nums[i] <= 10
- All numbers in nums are unique

**Examples**:

**Example 1**:
```
Input: nums = [1,2,3]
Output: [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
Explanation: The power set has 2^3 = 8 subsets.
```

**Example 2**:
```
Input: nums = [0]
Output: [[],[0]]
Explanation: The power set has 2^1 = 2 subsets.
```

**Example 3**:
```
Input: nums = [1,2]
Output: [[],[1],[2],[1,2]]
Explanation: The power set has 2^2 = 4 subsets.
```

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
// Test Case 1: Standard case with 3 elements
Input: nums = [1,2,3]
Output: [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]
Expected: 8 subsets (2^3 = 8)

// Test Case 2: Single element
Input: nums = [0]
Output: [[],[0]]
Expected: 2 subsets (2^1 = 2)

// Test Case 3: Two elements
Input: nums = [1,2]
Output: [[],[1],[2],[1,2]]
Expected: 4 subsets (2^2 = 4)

// Test Case 4: Negative numbers
Input: nums = [-1,0,1]
Output: [[],[-1],[0],[-1,0],[1],[-1,1],[0,1],[-1,0,1]]
Expected: 8 subsets

// Test Case 5: Maximum size
Input: nums = [1,2,3,4,5,6,7,8,9,10]
Output: 1024 subsets (2^10 = 1024)
Expected: All 1024 unique subsets
```

### Use Cases
- Feature selection in ML
- Finding all possible combinations of items
- Power set generation for combinatorics

---

## 3. Combination Sum (LC 39) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/combination-sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/combination-sum/)

### Problem Statement
Given an array of distinct integers `candidates` and a target integer `target`, return a list of all unique combinations of candidates where the chosen numbers sum to target. You may return the combinations in any order.

The same number may be chosen from candidates an unlimited number of times. Two combinations are unique if the frequency of at least one of the chosen numbers is different.

The test cases are generated such that the number of unique combinations that sum up to target is less than 150 combinations for the given input.

**Constraints**:
- 1 <= candidates.length <= 30
- 2 <= candidates[i] <= 40
- All elements of candidates are distinct
- 1 <= target <= 40

**Examples**:

**Example 1**:
```
Input: candidates = [2,3,6,7], target = 7
Output: [[2,2,3],[7]]
Explanation:
2 and 3 are candidates, and 2 + 2 + 3 = 7. Note that 2 can be used multiple times.
7 is a candidate, and 7 = 7.
These are the only two combinations.
```

**Example 2**:
```
Input: candidates = [2,3,5], target = 8
Output: [[2,2,2,2],[2,3,3],[3,5]]
Explanation:
2 + 2 + 2 + 2 = 8
2 + 3 + 3 = 8
3 + 5 = 8
```

**Example 3**:
```
Input: candidates = [2], target = 1
Output: []
Explanation: There are no combinations that sum to 1.
```

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
// Test Case 1: Multiple combinations
Input: candidates = [2,3,6,7], target = 7
Output: [[2,2,3],[7]]
Expected: 2 combinations

// Test Case 2: Repeated use of same number
Input: candidates = [2,3,5], target = 8
Output: [[2,2,2,2],[2,3,3],[3,5]]
Expected: 3 combinations

// Test Case 3: No valid combination
Input: candidates = [2], target = 1
Output: []
Expected: Empty list

// Test Case 4: Target equals candidate
Input: candidates = [1], target = 1
Output: [[1]]
Expected: 1 combination

// Test Case 5: Large target
Input: candidates = [2,3,5], target = 15
Output: [[2,2,2,2,2,2,3],[2,2,2,3,3,3],[2,3,5,5],[3,3,3,3,3],[5,5,5]]
Expected: Multiple combinations

// Test Case 6: All candidates needed
Input: candidates = [7,3,2], target = 18
Output: [[2,2,2,2,2,2,2,2,2],[2,2,2,2,2,2,3,3],[2,2,2,3,3,3,3],[2,3,3,3,7],[3,3,3,3,3,3],[2,2,7,7]]
Expected: Multiple valid combinations
```

### Use Cases
- Change-making problem
- Budget allocation with repeatable items
- Recipe combinations

---

## 4. Generate Parentheses (LC 22) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/generate-parentheses/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/generate-all-possible-parentheses/)

### Problem Statement
Given `n` pairs of parentheses, write a function to generate all combinations of well-formed parentheses.

A well-formed parentheses string means:
- Every opening bracket has a corresponding closing bracket
- Opening brackets must come before their corresponding closing brackets
- Brackets are properly nested

**Constraints**:
- 1 <= n <= 8

**Examples**:

**Example 1**:
```
Input: n = 3
Output: ["((()))","(()())","(())()","()(())","()()()"]
Explanation: All 5 valid combinations of 3 pairs of parentheses.
```

**Example 2**:
```
Input: n = 1
Output: ["()"]
Explanation: Only one valid combination for 1 pair.
```

**Example 3**:
```
Input: n = 2
Output: ["(())","()()"]
Explanation: Two valid combinations for 2 pairs.
```

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
// Test Case 1: Single pair
Input: n = 1
Output: ["()"]
Expected: 1 combination (Catalan number C(1) = 1)

// Test Case 2: Two pairs
Input: n = 2
Output: ["(())","()()"]
Expected: 2 combinations (Catalan number C(2) = 2)

// Test Case 3: Three pairs
Input: n = 3
Output: ["((()))","(()())","(())()","()(())","()()()"]
Expected: 5 combinations (Catalan number C(3) = 5)

// Test Case 4: Four pairs
Input: n = 4
Output: 14 combinations
Expected: ["(((())))","((()()))","((())())","((()))()","(()(()))",
          "(()()())","(()())()","(())(())","(())()()","()((()))",
          "()(()())","()(())()","()()(())","()()()()"]
Catalan number C(4) = 14

// Test Case 5: Maximum size
Input: n = 8
Output: 1430 combinations
Expected: Catalan number C(8) = 1430
```

### Use Cases
- Code formatter/validator
- Expression tree generation
- Compiler design (balanced bracket generation)

---

## 5. Word Search (LC 79) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/word-search/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/word-search/)

### Problem Statement
Given an `m x n` grid of characters `board` and a string `word`, return `true` if `word` exists in the grid.

The word can be constructed from letters of sequentially adjacent cells, where adjacent cells are horizontally or vertically neighboring. The same letter cell may not be used more than once.

**Constraints**:
- m == board.length
- n = board[i].length
- 1 <= m, n <= 6
- 1 <= word.length <= 15
- board and word consists of only lowercase and uppercase English letters

**Examples**:

**Example 1**:
```
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], 
       word = "ABCCED"
Output: true
Explanation: 
A → B → C → C → E → D (path exists)
```

**Example 2**:
```
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], 
       word = "SEE"
Output: true
Explanation:
S → E → E (path exists)
```

**Example 3**:
```
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], 
       word = "ABCB"
Output: false
Explanation:
Cannot reuse the same cell (B at position [0,1])
```

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
// Test Case 1: Word exists with path
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], word = "ABCCED"
Output: true
Path: (0,0)→(0,1)→(0,2)→(1,2)→(2,2)→(2,1)

// Test Case 2: Word exists, shorter path
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], word = "SEE"
Output: true
Path: (1,0)→(2,0)→(2,1) or other valid paths

// Test Case 3: Word doesn't exist (reuse required)
Input: board = [["A","B","C","E"],
                ["S","F","C","S"],
                ["A","D","E","E"]], word = "ABCB"
Output: false
Reason: Would need to reuse B at (0,1)

// Test Case 4: Simple 2x2 grid
Input: board = [["A","B"],["C","D"]], word = "ABDC"
Output: true
Path: (0,0)→(0,1)→(1,1)→(1,0)

// Test Case 5: Word doesn't exist
Input: board = [["A","B"],["C","D"]], word = "ABCD"
Output: false
Reason: No valid path exists

// Test Case 6: Single cell
Input: board = [["A"]], word = "A"
Output: true

// Test Case 7: Word longer than grid
Input: board = [["A","B"]], word = "ABCDEF"
Output: false
Reason: Not enough cells
```

### Use Cases
- Boggle game solver
- Text pattern matching in 2D grids
- Maze path finding

---

## 6. Palindrome Partitioning (LC 131) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/palindrome-partitioning/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/palindromic-patitioning/)

### Problem Statement
Given a string `s`, partition `s` such that every substring of the partition is a palindrome. Return all possible palindrome partitioning of `s`.

A palindrome is a string that reads the same backward as forward.

**Constraints**:
- 1 <= s.length <= 16
- s contains only lowercase English letters

**Examples**:

**Example 1**:
```
Input: s = "aab"
Output: [["a","a","b"],["aa","b"]]
Explanation:
- "a", "a", "b" are all palindromes
- "aa", "b" are all palindromes
```

**Example 2**:
```
Input: s = "a"
Output: [["a"]]
Explanation: Single character is always a palindrome.
```

**Example 3**:
```
Input: s = "aabb"
Output: [["a","a","b","b"],["a","a","bb"],["aa","b","b"],["aa","bb"]]
```

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
// Test Case 1: Multiple partitions
Input: s = "aab"
Output: [["a","a","b"],["aa","b"]]
Expected: 2 valid partitions

// Test Case 2: Single character
Input: s = "a"
Output: [["a"]]
Expected: 1 partition

// Test Case 3: All same characters
Input: s = "aaa"
Output: [["a","a","a"],["a","aa"],["aa","a"],["aaa"]]
Expected: 4 partitions

// Test Case 4: No palindrome substrings except single chars
Input: s = "abc"
Output: [["a","b","c"]]
Expected: 1 partition (only single characters)

// Test Case 5: Full string is palindrome
Input: s = "aba"
Output: [["a","b","a"],["aba"]]
Expected: 2 partitions

// Test Case 6: Complex case
Input: s = "aabb"
Output: [["a","a","b","b"],["a","a","bb"],["aa","b","b"],["aa","bb"]]
Expected: 4 partitions
```

---

## 7. N-Queens (LC 51) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/n-queens/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/n-queen-problem/)

### Problem Statement
The n-queens puzzle is the problem of placing `n` queens on an `n x n` chessboard such that no two queens attack each other.

Given an integer `n`, return all distinct solutions to the n-queens puzzle. You may return the answer in any order.

Each solution contains a distinct board configuration of the n-queens' placement, where 'Q' and '.' both indicate a queen and an empty space, respectively.

Queens can attack:
- Horizontally (same row)
- Vertically (same column)  
- Diagonally (both diagonals)

**Constraints**:
- 1 <= n <= 9

**Examples**:

**Example 1**:
```
Input: n = 4
Output: [[".Q..","...Q","Q...","..Q."],
         ["..Q.","Q...","...Q",".Q.."]]
Explanation: There are 2 distinct solutions to the 4-queens puzzle.

Solution 1:        Solution 2:
. Q . .            . . Q .
Q . . .            Q . . .
. . . Q            . . . Q
. . Q .            . Q . .
```

**Example 2**:
```
Input: n = 1
Output: [["Q"]]
Explanation: Only one queen on 1x1 board.
```

**Example 3**:
```
Input: n = 2
Output: []
Explanation: No solution exists for 2x2 board.
```

**Example 4**:
```
Input: n = 3
Output: []
Explanation: No solution exists for 3x3 board.
```

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
// Test Case 1: Minimum valid case
Input: n = 1
Output: [["Q"]]
Expected: 1 solution

// Test Case 2: No solution
Input: n = 2
Output: []
Expected: 0 solutions (impossible)

// Test Case 3: No solution
Input: n = 3
Output: []
Expected: 0 solutions (impossible)

// Test Case 4: Classic 4-queens
Input: n = 4
Output: [[".Q..","...Q","Q...","..Q."],
         ["..Q.","Q...","...Q",".Q.."]]
Expected: 2 solutions

// Test Case 5: 8-queens (classic chess problem)
Input: n = 8
Output: 92 distinct solutions
Expected: 92 solutions

// Test Case 6: Maximum size
Input: n = 9
Output: 352 distinct solutions
Expected: 352 solutions
```

---

## 8. Letter Combinations of Phone Number (LC 17) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/letter-combinations-of-a-phone-number/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/possible-words-from-phone-digits/)

### Problem Statement
Given a string containing digits from `2-9` inclusive, return all possible letter combinations that the number could represent. Return the answer in any order.

A mapping of digits to letters (just like on the telephone buttons) is given below. Note that 1 does not map to any letters.

```
2 -> "abc"
3 -> "def"
4 -> "ghi"
5 -> "jkl"
6 -> "mno"
7 -> "pqrs"
8 -> "tuv"
9 -> "wxyz"
```

**Constraints**:
- 0 <= digits.length <= 4
- digits[i] is a digit in the range ['2', '9']

**Examples**:

**Example 1**:
```
Input: digits = "23"
Output: ["ad","ae","af","bd","be","bf","cd","ce","cf"]
Explanation:
2 maps to "abc"
3 maps to "def"
All combinations: a+d, a+e, a+f, b+d, b+e, b+f, c+d, c+e, c+f
```

**Example 2**:
```
Input: digits = ""
Output: []
Explanation: Empty input returns empty list.
```

**Example 3**:
```
Input: digits = "2"
Output: ["a","b","c"]
Explanation: Single digit returns all its letters.
```

**Example 4**:
```
Input: digits = "234"
Output: ["adg","adh","adi","aeg","aeh","aei","afg","afh","afi",
         "bdg","bdh","bdi","beg","beh","bei","bfg","bfh","bfi",
         "cdg","cdh","cdi","ceg","ceh","cei","cfg","cfh","cfi"]
Explanation: 3 * 3 * 3 = 27 combinations
```

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
// Test Case 1: Two digits
Input: digits = "23"
Output: ["ad","ae","af","bd","be","bf","cd","ce","cf"]
Expected: 9 combinations (3 * 3)

// Test Case 2: Empty string
Input: digits = ""
Output: []
Expected: Empty list

// Test Case 3: Single digit
Input: digits = "2"
Output: ["a","b","c"]
Expected: 3 combinations

// Test Case 4: Digit with 4 letters (7 or 9)
Input: digits = "7"
Output: ["p","q","r","s"]
Expected: 4 combinations

// Test Case 5: Three digits
Input: digits = "234"
Output: 27 combinations
Expected: 3 * 3 * 3 = 27 combinations

// Test Case 6: Maximum length with 7s and 9s
Input: digits = "7777"
Output: 256 combinations
Expected: 4^4 = 256 combinations

// Test Case 7: Mixed digits
Input: digits = "79"
Output: ["pw","px","py","pz","qw","qx","qy","qz",
         "rw","rx","ry","rz","sw","sx","sy","sz"]
Expected: 16 combinations (4 * 4)
```

### Use Cases
- T9 keyboard autocomplete
- Phone number word generation (e.g., 1-800-FLOWERS)

---

## 9. Subsets II (LC 90) - With Duplicates ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/subsets-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/subsets-with-duplicates/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/permutations-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/permutations-of-a-given-string-with-duplicates/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/combination-sum-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/combination-sum-ii/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/restore-ip-addresses/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/generate-ip-addresses/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/sudoku-solver/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/solve-the-sudoku/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/expression-add-operators/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/expression-add-operators/)

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

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/word-search-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/word-search-ii/)

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
