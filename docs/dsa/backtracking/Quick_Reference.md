# Backtracking - Quick Reference Cheat Sheet

## 🎯 Core Template

```java
void backtrack(State current) {
    // Base case: solution complete
    if (isComplete(current)) {
        processSolution(current);
        return;
    }
    
    // Try all choices
    for (Choice choice : getAllChoices(current)) {
        // Prune invalid choices
        if (!isValid(current, choice)) {
            continue;
        }
        
        // Choose
        makeChoice(current, choice);
        
        // Explore
        backtrack(current);
        
        // Unchoose (backtrack)
        undoChoice(current, choice);
    }
}
```

## 📋 Pattern Templates

### Pattern 1: Subsets
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
**Use for**: Power set, all subsets

### Pattern 2: Permutations
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
**Use for**: All arrangements, orderings

### Pattern 3: Combinations
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
**Use for**: Choose k from n

### Pattern 4: Grid Search
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
**Use for**: Word search, maze, path finding

### Pattern 5: Constraint Satisfaction
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
**Use for**: N-Queens, Sudoku

## 🔑 When to Use Backtracking

| Characteristic | Indicator |
|----------------|-----------|
| **All Solutions** | "Generate all", "Find all" |
| **Constraints** | "Valid arrangements", "Satisfy rules" |
| **Incremental** | Build solution step by step |
| **Pruning** | Can eliminate invalid branches |
| **Exploration** | Try all possibilities |

## ⚡ Time & Space Complexity

| Pattern | Time | Space | Notes |
|---------|------|-------|-------|
| Subsets | O(2^n) | O(n) | 2 choices per element |
| Permutations | O(n!) | O(n) | n! arrangements |
| Combinations | O(C(n,k)) | O(k) | Binomial coefficient |
| Grid Search | O(4^L) | O(L) | 4 directions, L = word length |
| N-Queens | O(n!) | O(n) | With pruning |
| Sudoku | O(9^m) | O(m) | m = empty cells |

## 🚫 Common Mistakes

1. **Not making a copy**
   ```java
   // ❌ Wrong
   result.add(current);
   
   // ✅ Correct
   result.add(new ArrayList<>(current));
   ```

2. **Forgetting to backtrack**
   ```java
   // ❌ Wrong
   current.add(choice);
   backtrack(current);
   // Missing: current.remove(current.size() - 1);
   
   // ✅ Correct
   current.add(choice);
   backtrack(current);
   current.remove(current.size() - 1);
   ```

3. **Not handling duplicates**
   ```java
   // ❌ Wrong
   for (int i = start; i < n; i++) {
       current.add(nums[i]);
       backtrack(i + 1, current);
       current.remove(current.size() - 1);
   }
   
   // ✅ Correct
   Arrays.sort(nums);
   for (int i = start; i < n; i++) {
       if (i > start && nums[i] == nums[i-1]) continue;
       current.add(nums[i]);
       backtrack(i + 1, current);
       current.remove(current.size() - 1);
   }
   ```

4. **Wrong base case**
   ```java
   // ❌ Wrong - checks after adding
   for (int i = start; i < n; i++) {
       current.add(i);
       if (current.size() == k) {
           result.add(new ArrayList<>(current));
       }
       backtrack(i + 1, current);
   }
   
   // ✅ Correct - checks before loop
   if (current.size() == k) {
       result.add(new ArrayList<>(current));
       return;
   }
   for (int i = start; i < n; i++) {
       current.add(i);
       backtrack(i + 1, current);
       current.remove(current.size() - 1);
   }
   ```

## 📊 All 20 Problems Summary

### Easy (8 problems)
1. Subsets - O(2^n)
2. Permutations - O(n!)
3. Combinations - O(C(n,k))
4. Letter Case Permutation - O(2^n)
5. Generate Parentheses - O(4^n/√n)
6. Binary Watch - O(1)
7. Combination Sum III - O(C(9,k))
8. Palindrome Partitioning - O(n×2^n)

### Medium (10 problems)
1. Combination Sum - O(2^t)
2. Combination Sum II - O(2^n)
3. Permutations II - O(n!)
4. Subsets II - O(2^n)
5. Word Search - O(m×n×4^L)
6. N-Queens - O(n!)
7. Sudoku Solver - O(9^m)
8. Letter Combinations - O(4^n)
9. Restore IP - O(1)
10. Partition K Subsets - O(k^n)

### Hard (2 problems)
1. N-Queens II - O(n!)
2. Word Search II - O(m×n×4^L)

## 🎓 Study Plan

### Week 1: Fundamentals
- Day 1-2: Theory and templates
- Day 3-4: Easy problems 1-4
- Day 5-6: Easy problems 5-8
- Day 7: Review

### Week 2: Advanced
- Day 1-2: Medium problems 1-5
- Day 3-4: Medium problems 6-10
- Day 5-6: Hard problems
- Day 7: Mixed practice

## 💡 Interview Tips

1. **Identify Pattern**
   - All solutions? → Backtracking
   - Constraints? → Backtracking
   - Incremental build? → Backtracking

2. **Choose Template**
   - Subsets, Permutations, Combinations?
   - Grid search, Constraint satisfaction?

3. **Define State**
   - What to track?
   - What to pass in recursion?

4. **Implement Carefully**
   - Choose, Explore, Unchoose
   - Always make copy when adding to result
   - Handle duplicates if needed

5. **Optimize with Pruning**
   - Skip invalid choices early
   - Sort for better pruning
   - Use constraints to eliminate branches

## 🔗 Related Patterns

- **DFS**: Backtracking is DFS with state restoration
- **Dynamic Programming**: Can combine with memoization
- **Branch and Bound**: Optimization variant
- **Constraint Propagation**: For Sudoku-like problems

## 📝 Quick Decision Guide

```
┌─────────────────────────────────────────────────────────┐
│ BACKTRACKING DECISION GUIDE                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ STEP 1: Need all solutions?                            │
│   ✓ Yes → Continue                                     │
│   ✗ No → Consider DP, Greedy, BFS                      │
│                                                         │
│ STEP 2: Can build incrementally?                       │
│   ✓ Yes → Continue                                     │
│   ✗ No → Consider other approaches                     │
│                                                         │
│ STEP 3: Can prune invalid branches?                    │
│   ✓ Yes → Backtracking is efficient                    │
│   ✗ No → Might be brute force                          │
│                                                         │
│ CHOOSE PATTERN:                                         │
│   • "All subsets" → Subsets pattern                    │
│   • "All arrangements" → Permutations pattern          │
│   • "Choose k" → Combinations pattern                  │
│   • "Split into groups" → Partition pattern            │
│   • "Word in grid" → Grid search pattern               │
│   • "N-Queens, Sudoku" → Constraint pattern            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## ✅ Mastery Checklist

- [ ] Understand backtracking fundamentals
- [ ] Master core template
- [ ] Can identify backtracking problems
- [ ] Know all 6 patterns
- [ ] Solved all 8 easy problems
- [ ] Solved all 10 medium problems
- [ ] Solved both hard problems
- [ ] Can handle duplicates
- [ ] Can optimize with pruning
- [ ] Complete problems in time limit

## 🎯 Key Principles

**Three Steps**:
1. **Choose**: Make a choice
2. **Explore**: Recurse with that choice
3. **Unchoose**: Backtrack and try next

**Always Remember**:
- Make copy when adding to result
- Backtrack after exploring
- Prune invalid branches early
- Handle duplicates by sorting + skipping

## 🔍 Optimization Techniques

1. **Sort for Pruning**
   ```java
   Arrays.sort(candidates);
   for (int i = start; i < candidates.length; i++) {
       if (candidates[i] > target) break; // Prune
   }
   ```

2. **Skip Duplicates**
   ```java
   if (i > start && nums[i] == nums[i-1]) continue;
   ```

3. **Early Termination**
   ```java
   if (sum > target) return; // No point continuing
   ```

4. **Bit Manipulation** (N-Queens)
   ```java
   int available = ((1 << n) - 1) & ~(cols | diag | antiDiag);
   ```

---

**Keep this cheat sheet handy during practice and interviews!**
