# Backtracking - Hard Problems (10%)

## 📚 2 Hard Problems with Complete Solutions

---

## Problem 1: N-Queens II (Count Solutions)

**Difficulty**: Hard  
**Pattern**: Constraint Satisfaction with Optimization  
**LeetCode**: #52

### Problem Statement

Return the number of distinct solutions to the n-queens puzzle.

### Intuition

Instead of storing all solutions, we just count them. We can optimize by using bit manipulation to track columns, diagonals, and anti-diagonals.

### Solution (Optimized with Bit Manipulation)

```java
public class NQueensII {
    private int count = 0;
    
    public int totalNQueens(int n) {
        backtrack(0, 0, 0, 0, n);
        return count;
    }
    
    private void backtrack(int row, int cols, int diag, int antiDiag, int n) {
        if (row == n) {
            count++;
            return;
        }
        
        // Available positions: positions not attacked
        int availablePositions = ((1 << n) - 1) & ~(cols | diag | antiDiag);
        
        while (availablePositions != 0) {
            // Get rightmost available position
            int position = availablePositions & -availablePositions;
            availablePositions -= position;
            
            backtrack(
                row + 1,
                cols | position,
                (diag | position) << 1,
                (antiDiag | position) >> 1,
                n
            );
        }
    }
}
```

### Solution (Standard Backtracking)

```java
public class NQueensIIStandard {
    private int count = 0;
    
    public int totalNQueens(int n) {
        boolean[] cols = new boolean[n];
        boolean[] diag = new boolean[2 * n - 1];
        boolean[] antiDiag = new boolean[2 * n - 1];
        
        backtrack(0, n, cols, diag, antiDiag);
        return count;
    }
    
    private void backtrack(int row, int n, boolean[] cols, 
                          boolean[] diag, boolean[] antiDiag) {
        if (row == n) {
            count++;
            return;
        }
        
        for (int col = 0; col < n; col++) {
            int diagIndex = row + col;
            int antiDiagIndex = row - col + n - 1;
            
            if (cols[col] || diag[diagIndex] || antiDiag[antiDiagIndex]) {
                continue;
            }
            
            cols[col] = true;
            diag[diagIndex] = true;
            antiDiag[antiDiagIndex] = true;
            
            backtrack(row + 1, n, cols, diag, antiDiag);
            
            cols[col] = false;
            diag[diagIndex] = false;
            antiDiag[antiDiagIndex] = false;
        }
    }
}
```

### Dry Run

**Input**: `n = 4`

```
Backtracking Tree (partial):

Row 0:
  Try col 0: Place queen at (0,0)
    Row 1:
      col 0: blocked (same column)
      col 1: blocked (diagonal)
      Try col 2: Place queen at (1,2)
        Row 2:
          col 0: blocked (anti-diagonal)
          col 1: Place queen at (2,1)
            Row 3:
              col 0: blocked
              col 1: blocked
              col 2: blocked
              col 3: Place queen at (3,3)
                Row 4: Found solution! count = 1
              Backtrack
          col 2: blocked
          col 3: blocked
        Backtrack
      col 3: ...
    Backtrack
  Try col 1: Place queen at (0,1)
    ...

Final count = 2 (for n=4)

Solutions:
. Q . .    . . Q .
. . . Q    Q . . .
Q . . .    . . . Q
. . Q .    . Q . .
```

### Bit Manipulation Explanation

```
For n=4:

cols:     Tracks which columns are occupied
diag:     Tracks which diagonals are occupied (row + col)
antiDiag: Tracks which anti-diagonals are occupied (row - col + n - 1)

Example:
If queen at (0, 1):
  cols = 0010 (column 1 occupied)
  diag = 0010 (diagonal 0+1=1 occupied)
  antiDiag = 0010 (anti-diagonal 0-1+3=2 occupied)

Available positions = all positions & ~(cols | diag | antiDiag)
```

### Complexity Analysis

**Standard Backtracking**:
- **Time Complexity**: O(n!) - With pruning, much better than O(n^n)
- **Space Complexity**: O(n) - Recursion stack + tracking arrays

**Bit Manipulation**:
- **Time Complexity**: O(n!) - Same as standard
- **Space Complexity**: O(n) - Only recursion stack
- **Advantage**: Faster constant factors, more elegant

### Test Cases

```java
@Test
public void testTotalNQueens() {
    NQueensII solution = new NQueensII();
    
    assertEquals(1, solution.totalNQueens(1));
    assertEquals(0, solution.totalNQueens(2));
    assertEquals(0, solution.totalNQueens(3));
    assertEquals(2, solution.totalNQueens(4));
    assertEquals(10, solution.totalNQueens(5));
    assertEquals(4, solution.totalNQueens(6));
    assertEquals(40, solution.totalNQueens(7));
    assertEquals(92, solution.totalNQueens(8));
}
```

### Edge Cases

1. **n = 1**: Only one solution (single queen)
2. **n = 2, 3**: No solutions possible
3. **n = 4**: Two solutions
4. **n = 8**: Classic 8-queens problem (92 solutions)

### Common Mistakes

1. **Not tracking diagonals correctly**: Use row+col and row-col+n-1
2. **Forgetting to backtrack**: Must undo all changes
3. **Wrong diagonal indexing**: Anti-diagonal needs offset

---

## Problem 2: Word Search II

**Difficulty**: Hard  
**Pattern**: Grid Backtracking with Trie  
**LeetCode**: #212

### Problem Statement

Given an `m x n` board of characters and a list of strings `words`, return all words on the board. Each word must be constructed from letters of sequentially adjacent cells.

### Intuition

**Naive Approach**: For each word, do Word Search I → O(words × m × n × 4^L)

**Optimized with Trie**: Build trie of all words, then do single DFS → O(m × n × 4^L)

### Solution

```java
public class WordSearchII {
    class TrieNode {
        TrieNode[] children = new TrieNode[26];
        String word = null;
    }
    
    public List<String> findWords(char[][] board, String[] words) {
        List<String> result = new ArrayList<>();
        
        // Build Trie
        TrieNode root = buildTrie(words);
        
        // DFS from each cell
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                backtrack(board, i, j, root, result);
            }
        }
        
        return result;
    }
    
    private void backtrack(char[][] board, int row, int col, 
                          TrieNode node, List<String> result) {
        char c = board[row][col];
        
        if (c == '#' || node.children[c - 'a'] == null) {
            return;
        }
        
        node = node.children[c - 'a'];
        
        // Found a word
        if (node.word != null) {
            result.add(node.word);
            node.word = null; // Avoid duplicates
        }
        
        // Mark as visited
        board[row][col] = '#';
        
        // Explore 4 directions
        if (row > 0) backtrack(board, row - 1, col, node, result);
        if (row < board.length - 1) backtrack(board, row + 1, col, node, result);
        if (col > 0) backtrack(board, row, col - 1, node, result);
        if (col < board[0].length - 1) backtrack(board, row, col + 1, node, result);
        
        // Backtrack
        board[row][col] = c;
    }
    
    private TrieNode buildTrie(String[] words) {
        TrieNode root = new TrieNode();
        
        for (String word : words) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }
            node.word = word;
        }
        
        return root;
    }
}
```

### Dry Run

**Input**: 
```
board = [
  ['o','a','a','n'],
  ['e','t','a','e'],
  ['i','h','k','r'],
  ['i','f','l','v']
]
words = ["oath","pea","eat","rain"]
```

```
Step 1: Build Trie
Root
├─ o → a → t → h (word: "oath")
├─ p → e → a (word: "pea")
├─ e → a → t (word: "eat")
└─ r → a → i → n (word: "rain")

Step 2: DFS from each cell

Start at (0,0) = 'o':
  Follow trie: o → a → t → h
  Path: (0,0) → (0,1) → (1,1) → (2,1)
  Found "oath"! Add to result

Start at (1,1) = 't':
  No 't' at root of trie, skip

Start at (1,0) = 'e':
  Follow trie: e → a → t
  Path: (1,0) → (0,1) → (1,1)
  Found "eat"! Add to result

... continue for all cells

Result: ["oath", "eat"]
```

### Why Trie is Better

**Without Trie** (Word Search I for each word):
```
For each word:
  For each cell:
    DFS to find word
Time: O(words × m × n × 4^L)
```

**With Trie**:
```
Build trie: O(words × L)
Single DFS: O(m × n × 4^L)
Total: O(words × L + m × n × 4^L)

Much better when words >> m×n
```

### Complexity Analysis

- **Time Complexity**: O(m × n × 4^L) where L is max word length
  - Build trie: O(words × L)
  - DFS: O(m × n × 4^L)
  
- **Space Complexity**: O(words × L) for trie

### Test Cases

```java
@Test
public void testFindWords() {
    WordSearchII solution = new WordSearchII();
    
    char[][] board = {
        {'o','a','a','n'},
        {'e','t','a','e'},
        {'i','h','k','r'},
        {'i','f','l','v'}
    };
    
    String[] words = {"oath","pea","eat","rain"};
    List<String> result = solution.findWords(board, words);
    
    assertTrue(result.contains("oath"));
    assertTrue(result.contains("eat"));
    assertFalse(result.contains("pea"));
    assertFalse(result.contains("rain"));
}
```

### Edge Cases

1. **Empty board**: Return empty list
2. **No words found**: Return empty list
3. **All words found**: Return all
4. **Duplicate words in list**: Trie handles naturally
5. **Single cell board**: Check if any word is single character

### Optimization Techniques

1. **Prune Trie**: Remove found words from trie
2. **Early Termination**: If trie node has no children, stop
3. **Avoid Duplicates**: Set word to null after finding

### Common Mistakes

1. **Not using Trie**: Too slow for multiple words
2. **Not marking visited**: Infinite loops
3. **Not backtracking**: Board state corrupted
4. **Returning duplicates**: Set word to null after finding

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| N-Queens II | Constraint + Optimization | O(n!) | O(n) | Bit manipulation for tracking |
| Word Search II | Grid + Trie | O(m×n×4^L) | O(W×L) | Trie for multiple words |

---

## 🎓 Key Takeaways

### Hard Problem Characteristics

1. **Optimization Required**: Bit manipulation, Trie
2. **Multiple Constraints**: Track multiple conditions
3. **Large Search Space**: Need aggressive pruning
4. **Data Structure Choice**: Right structure makes huge difference

### Problem-Solving Strategies

1. **Start with Standard**: Get working solution first
2. **Identify Bottleneck**: What's slow?
3. **Optimize**: Bit manipulation, better data structures
4. **Test Thoroughly**: Many edge cases

### N-Queens Optimization

- **Standard**: Use boolean arrays
- **Optimized**: Use bit manipulation
- **Key Insight**: Bits represent occupied positions

### Word Search Optimization

- **Naive**: Search for each word separately
- **Optimized**: Use Trie to search all words together
- **Key Insight**: Share common prefixes

---

**Congratulations!** You've completed all 20 Backtracking problems! 🎉

**Next Steps**:
1. Review problems you found difficult
2. Practice optimization techniques
3. Solve variations with different constraints
4. Time yourself on random problems

**Related Topics**:
- Dynamic Programming with Backtracking
- Branch and Bound
- Constraint Satisfaction Problems
- Game Theory
- Combinatorial Optimization
