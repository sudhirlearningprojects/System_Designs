# Backtracking - Pattern Recognition Guide

## 🎯 How to Identify Backtracking Problems

This guide helps you recognize when to use Backtracking by analyzing problem characteristics.

## 🔍 Recognition Checklist

### ✅ Strong Indicators

1. **"All possible" keywords**
   - "Generate all"
   - "Find all combinations"
   - "All permutations"
   - "All subsets"
   
2. **Constraint satisfaction**
   - "Valid arrangements"
   - "Satisfy constraints"
   - "Place items with rules"
   
3. **Exploration keywords**
   - "Explore all paths"
   - "Try all possibilities"
   - "Exhaustive search"
   
4. **Puzzle/Game solving**
   - Sudoku, N-Queens
   - Maze solving
   - Word puzzles

## 📋 Decision Tree

```
Does the problem ask for "all" solutions?
│
├─ YES ──→ Can you build solution incrementally?
│          │
│          ├─ YES ──→ Can you prune invalid branches early?
│          │          │
│          │          ├─ YES ──→ USE BACKTRACKING ✓
│          │          │
│          │          └─ NO ──→ Might still use backtracking (brute force)
│          │
│          └─ NO ──→ Consider other approaches
│
└─ NO ──→ Is it constraint satisfaction?
          │
          ├─ YES ──→ USE BACKTRACKING ✓
          │
          └─ NO ──→ Not backtracking (try DP, Greedy, etc.)
```

## 🎨 Pattern Matching Examples

### Pattern 1: Subsets/Power Set

**Keywords**: subsets, power set, all combinations

**Example Problems**:
```
✓ "Generate all subsets of a set"
✓ "Find all possible combinations"
✓ "Power set of array"
```

**Recognition**:
- Each element: include or exclude
- No ordering constraint
- 2^n total subsets

**Template Match**:
```java
void backtrack(int start, List<Integer> current) {
    result.add(new ArrayList<>(current));
    
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);
        backtrack(i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

### Pattern 2: Permutations

**Keywords**: permutations, arrangements, all orderings

**Example Problems**:
```
✓ "Generate all permutations"
✓ "All possible arrangements"
✓ "Different orderings"
```

**Recognition**:
- Use all elements
- Different orders matter
- n! total permutations

**Template Match**:
```java
void backtrack(List<Integer> current, boolean[] used) {
    if (current.size() == n) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = 0; i < n; i++) {
        if (used[i]) continue;
        
        current.add(nums[i]);
        used[i] = true;
        backtrack(current, used);
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

### Pattern 3: Combinations

**Keywords**: choose k from n, combinations, select

**Example Problems**:
```
✓ "Choose k elements from n"
✓ "All combinations of size k"
✓ "Select k items"
```

**Recognition**:
- Fixed size k
- Order doesn't matter
- C(n,k) total combinations

**Template Match**:
```java
void backtrack(int start, List<Integer> current) {
    if (current.size() == k) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = start; i <= n; i++) {
        current.add(i);
        backtrack(i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

### Pattern 4: Partition

**Keywords**: partition, split, divide into groups

**Example Problems**:
```
✓ "Partition into k equal sum subsets"
✓ "Split into valid groups"
✓ "Divide array into parts"
```

**Recognition**:
- Divide into groups
- Each element in one group
- Groups satisfy constraints

**Template Match**:
```java
void backtrack(int index, List<List<Integer>> groups) {
    if (index == n) {
        if (isValid(groups)) {
            result.add(deepCopy(groups));
        }
        return;
    }
    
    for (List<Integer> group : groups) {
        if (canAdd(group, nums[index])) {
            group.add(nums[index]);
            backtrack(index + 1, groups);
            group.remove(group.size() - 1);
        }
    }
}
```

### Pattern 5: Grid/Board Search

**Keywords**: word search, path in grid, maze

**Example Problems**:
```
✓ "Find word in grid"
✓ "Path exists in maze"
✓ "Explore all paths in grid"
```

**Recognition**:
- 2D grid/board
- Explore 4 or 8 directions
- Mark visited cells

**Template Match**:
```java
boolean backtrack(int row, int col, int index) {
    if (index == word.length()) return true;
    
    if (outOfBounds(row, col) || visited[row][col]) {
        return false;
    }
    
    visited[row][col] = true;
    
    boolean found = backtrack(row+1, col, index+1) ||
                   backtrack(row-1, col, index+1) ||
                   backtrack(row, col+1, index+1) ||
                   backtrack(row, col-1, index+1);
    
    visited[row][col] = false;
    return found;
}
```

### Pattern 6: Constraint Satisfaction

**Keywords**: N-Queens, Sudoku, valid placement

**Example Problems**:
```
✓ "N-Queens problem"
✓ "Solve Sudoku"
✓ "Place items with constraints"
```

**Recognition**:
- Place items on board
- Constraints to satisfy
- Check validity at each step

**Template Match**:
```java
boolean backtrack(int row) {
    if (row == n) return true;
    
    for (int col = 0; col < n; col++) {
        if (isValid(row, col)) {
            place(row, col);
            
            if (backtrack(row + 1)) {
                return true;
            }
            
            remove(row, col);
        }
    }
    
    return false;
}
```

## 🔑 Key Questions to Ask

### Question 1: Need all solutions or just one?

**All solutions** → Backtracking  
**Just one** → Greedy, DP, or BFS  
**Optimal solution** → DP or Greedy  

### Question 2: Can build solution incrementally?

**YES** → Backtracking works  
**NO** → Consider other approaches  

### Question 3: Can prune invalid branches?

**YES** → Backtracking is efficient  
**NO** → Might be pure brute force  

### Question 4: What are the constraints?

**Clear constraints** → Easy to prune  
**No constraints** → Generate all possibilities  

## 📊 Problem Type Matrix

| Problem Type | Pattern | Complexity | Example |
|--------------|---------|------------|---------|
| Subsets | Include/Exclude | O(2^n) | Power Set |
| Permutations | Arrange All | O(n!) | All Arrangements |
| Combinations | Choose K | O(C(n,k)) | Choose k from n |
| Partition | Split Groups | O(k^n) | K Equal Subsets |
| Grid Search | Explore Directions | O(4^L) | Word Search |
| Constraint | Place with Rules | O(n!) | N-Queens |

## 🎯 Common Problem Phrases

### Backtracking Triggers

- "generate all"
- "find all combinations"
- "all permutations"
- "all subsets"
- "all possible"
- "valid arrangements"
- "satisfy constraints"
- "explore all paths"

### Specific Patterns

**Subsets**: "power set", "all subsets", "combinations"  
**Permutations**: "all arrangements", "different orderings"  
**Combinations**: "choose k", "select k items"  
**Partition**: "split into groups", "divide into parts"  
**Grid**: "word search", "path in grid", "maze"  
**Constraint**: "N-Queens", "Sudoku", "valid placement"  

## 🚫 When NOT to Use Backtracking

### 1. Only Need One Solution
```
Problem: "Find any valid Sudoku solution"
Better: Use constraint propagation + backtracking
Not: Generate all solutions
```

### 2. Optimal Substructure Exists
```
Problem: "Longest increasing subsequence"
Better: Dynamic Programming
Not: Backtracking (too slow)
```

### 3. Greedy Works
```
Problem: "Activity selection"
Better: Greedy algorithm
Not: Backtracking (overkill)
```

### 4. No Pruning Possible
```
Problem: "Generate all binary strings of length n"
Better: Iterative generation
Not: Backtracking (no advantage)
```

## 💡 Conversion Examples

### Example 1: Subsets

**Problem**: Generate all subsets of [1,2,3]

**Brute Force** (Generate then filter):
```java
// Generate all 2^n possibilities
for (int mask = 0; mask < (1 << n); mask++) {
    List<Integer> subset = new ArrayList<>();
    for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) {
            subset.add(nums[i]);
        }
    }
    result.add(subset);
}
```

**Backtracking** (Build incrementally):
```java
void backtrack(int start, List<Integer> current) {
    result.add(new ArrayList<>(current));
    
    for (int i = start; i < n; i++) {
        current.add(nums[i]);
        backtrack(i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

## 🎓 Practice Strategy

### Level 1: Master Templates
1. Subsets template
2. Permutations template
3. Combinations template
4. Practice on simple problems

### Level 2: Add Constraints
1. Handle duplicates
2. Add pruning conditions
3. Optimize with sorting

### Level 3: Complex Problems
1. Grid search problems
2. Constraint satisfaction
3. Partition problems

### Level 4: Optimize
1. Early termination
2. Better pruning
3. Memoization

## 📝 Quick Recognition Card

```
┌─────────────────────────────────────────────────────────┐
│ BACKTRACKING PATTERN RECOGNITION                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ SUBSETS:                                                │
│   ✓ "All subsets", "Power set"                         │
│   ✓ Include/exclude each element                       │
│   → O(2^n) complexity                                   │
│                                                         │
│ PERMUTATIONS:                                           │
│   ✓ "All arrangements", "Different orderings"          │
│   ✓ Use all elements, track used                       │
│   → O(n!) complexity                                    │
│                                                         │
│ COMBINATIONS:                                           │
│   ✓ "Choose k from n"                                  │
│   ✓ Fixed size, order doesn't matter                   │
│   → O(C(n,k)) complexity                               │
│                                                         │
│ PARTITION:                                              │
│   ✓ "Split into groups", "Divide into parts"           │
│   ✓ Each element in one group                          │
│   → O(k^n) complexity                                   │
│                                                         │
│ GRID SEARCH:                                            │
│   ✓ "Word search", "Path in grid"                      │
│   ✓ Explore 4 directions, mark visited                 │
│   → O(4^L) complexity                                   │
│                                                         │
│ CONSTRAINT:                                             │
│   ✓ "N-Queens", "Sudoku"                               │
│   ✓ Place with constraints, prune early                │
│   → O(n!) with pruning                                  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 🔍 Problem Analysis Framework

### Step 1: Identify Type
- All solutions? Constraint satisfaction?
- What are we generating/finding?

### Step 2: Choose Pattern
- Subsets, Permutations, Combinations?
- Grid search, Constraint satisfaction?

### Step 3: Define State
- What to track in current solution?
- What to pass in recursion?

### Step 4: Identify Choices
- What decisions at each step?
- How many options?

### Step 5: Define Constraints
- When is choice invalid?
- How to prune?

### Step 6: Implement
- Choose, Explore, Unchoose
- Handle base case
- Add pruning

---

**Next**: [Easy Problems](03_Easy_Problems.md)
