# Backtracking - Complete Guide

## 📚 Documentation Structure

This comprehensive guide covers Backtracking techniques with theory, examples, and 20+ problems.

### Documents

1. **[01_Theory_and_Concepts.md](01_Theory_and_Concepts.md)** - Deep dive into Backtracking theory
2. **[02_Pattern_Recognition.md](02_Pattern_Recognition.md)** - How to identify Backtracking problems
3. **[03_Easy_Problems.md](03_Easy_Problems.md)** - 8 Easy problems with solutions (40%)
4. **[04_Medium_Problems.md](04_Medium_Problems.md)** - 10 Medium problems with solutions (50%)
5. **[05_Hard_Problems.md](05_Hard_Problems.md)** - 2 Hard problems with solutions (10%)
6. **[Quick_Reference.md](Quick_Reference.md)** - Cheat sheet for quick review

## 🎯 Quick Overview

**Backtracking** is an algorithmic technique that builds solutions incrementally and abandons a solution ("backtracks") as soon as it determines the solution cannot be completed.

### Key Benefits
- **Exhaustive Search**: Explores all possible solutions
- **Pruning**: Eliminates invalid paths early
- **Systematic**: Organized exploration of solution space
- **Optimal for Constraints**: Perfect for constraint satisfaction problems

### Core Principles
1. **Choose** - Make a choice and move forward
2. **Explore** - Recursively explore with that choice
3. **Unchoose** - Backtrack and undo the choice
4. **Prune** - Skip invalid branches early

## 📊 Problem Distribution

| Difficulty | Count | Percentage |
|------------|-------|------------|
| Easy       | 8     | 40%        |
| Medium     | 10    | 50%        |
| Hard       | 2     | 10%        |
| **Total**  | **20**| **100%**   |

## 🚀 Getting Started

Start with **01_Theory_and_Concepts.md** to understand the fundamentals, then move to **02_Pattern_Recognition.md** to learn how to identify these problems.

Practice problems in order: Easy → Medium → Hard

## 📝 Problem List

### Easy Problems (8)
1. Subsets
2. Permutations
3. Combinations
4. Letter Case Permutation
5. Generate Parentheses
6. Binary Watch
7. Combination Sum III
8. Palindrome Partitioning

### Medium Problems (10)
1. Combination Sum
2. Combination Sum II
3. Permutations II
4. Subsets II
5. Word Search
6. N-Queens
7. Sudoku Solver
8. Letter Combinations of Phone Number
9. Restore IP Addresses
10. Partition to K Equal Sum Subsets

### Hard Problems (2)
1. N-Queens II (Count Solutions)
2. Word Search II

## 💡 Tips for Success

1. **Identify the choice** - What decision to make at each step?
2. **Define constraints** - When to prune invalid paths?
3. **Track state** - What information to maintain?
4. **Backtrack properly** - Undo changes when returning
5. **Optimize with pruning** - Skip impossible branches early

## 🎨 Visual Example: Generate Subsets

### Problem: Generate all subsets of [1, 2, 3]

```
Decision Tree:
                    []
                /        \
            [1]            []
          /     \        /    \
      [1,2]    [1]    [2]     []
      /   \    /  \   /  \    /  \
  [1,2,3][1,2][1,3][1][2,3][2][3][]

At each node, we decide: Include current element or not?

Backtracking Process:
1. Choose: Add element to current subset
2. Explore: Recurse with remaining elements
3. Unchoose: Remove element from subset
4. Repeat for next element
```

## 🔑 Key Approaches

### 1. Standard Backtracking Template

```java
void backtrack(List<Integer> current, int start) {
    // Base case: found a solution
    if (isComplete(current)) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    // Try all choices
    for (int i = start; i < candidates.length; i++) {
        // Prune invalid choices
        if (!isValid(current, candidates[i])) {
            continue;
        }
        
        // Choose
        current.add(candidates[i]);
        
        // Explore
        backtrack(current, i + 1);
        
        // Unchoose (backtrack)
        current.remove(current.size() - 1);
    }
}
```

### 2. Backtracking with Visited Array

```java
void backtrack(List<Integer> current, boolean[] visited) {
    if (current.size() == n) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = 0; i < n; i++) {
        if (visited[i]) continue;
        
        // Choose
        current.add(nums[i]);
        visited[i] = true;
        
        // Explore
        backtrack(current, visited);
        
        // Unchoose
        current.remove(current.size() - 1);
        visited[i] = false;
    }
}
```

### 3. Backtracking with Grid/Board

```java
boolean backtrack(char[][] board, int row, int col) {
    // Base case: reached end
    if (row == board.length) {
        return true;
    }
    
    // Move to next cell
    int nextRow = (col == board[0].length - 1) ? row + 1 : row;
    int nextCol = (col == board[0].length - 1) ? 0 : col + 1;
    
    // Try all choices
    for (char c = '1'; c <= '9'; c++) {
        if (isValid(board, row, col, c)) {
            // Choose
            board[row][col] = c;
            
            // Explore
            if (backtrack(board, nextRow, nextCol)) {
                return true;
            }
            
            // Unchoose
            board[row][col] = '.';
        }
    }
    
    return false;
}
```

## 📈 Common Backtracking Patterns

| Pattern | Example | Key Characteristic |
|---------|---------|-------------------|
| Subsets | Power Set | Include/exclude each element |
| Permutations | Arrange elements | Use all elements, different orders |
| Combinations | Choose k from n | Choose subset of size k |
| Partition | Split into groups | Divide into valid partitions |
| Grid Search | Word Search | Explore 4 directions |
| Constraint Satisfaction | N-Queens, Sudoku | Place items with constraints |

## 🎯 When to Use Backtracking

### Strong Indicators
- Generate all possible solutions
- Find all combinations/permutations
- Constraint satisfaction problems
- Puzzle solving (Sudoku, N-Queens)
- Path finding with constraints
- Decision tree exploration

### Problem Keywords
- "All possible"
- "Generate all"
- "Find all combinations"
- "Permutations"
- "Subsets"
- "Partition"
- "Valid arrangements"

## 📊 Complexity Patterns

| Problem Type | Time Complexity | Space Complexity | Notes |
|--------------|----------------|------------------|-------|
| Subsets | O(2^n) | O(n) | 2^n possible subsets |
| Permutations | O(n!) | O(n) | n! permutations |
| Combinations | O(C(n,k)) | O(k) | Binomial coefficient |
| N-Queens | O(n!) | O(n) | Pruning reduces actual time |
| Sudoku | O(9^m) | O(m) | m = empty cells |

## 🔄 Backtracking vs Other Techniques

### Backtracking vs DFS
- **Backtracking**: Builds solution incrementally, undoes choices
- **DFS**: Explores graph/tree, doesn't necessarily undo

### Backtracking vs Dynamic Programming
- **Backtracking**: Finds all solutions, explores all paths
- **DP**: Finds optimal solution, avoids recomputation

### Backtracking vs Greedy
- **Backtracking**: Tries all possibilities, guaranteed to find solution
- **Greedy**: Makes local optimal choice, may not find global optimal

---

**Total Problems**: 20 | **Estimated Study Time**: 20-25 hours
