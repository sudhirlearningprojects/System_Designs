# Backtracking - Theory and Concepts

## 📖 What is Backtracking?

**Backtracking** is an algorithmic technique for solving problems recursively by trying to build a solution incrementally, one piece at a time, and removing solutions that fail to satisfy the constraints at any point.

### Core Idea

Try all possibilities systematically. When you hit a dead end, backtrack to the last decision point and try a different path.

```
Think of it like exploring a maze:
1. Try a path
2. If it leads to exit → Success!
3. If it's a dead end → Go back and try another path
4. Repeat until all paths explored
```

## 🎯 The Backtracking Framework

### Three Key Steps

1. **Choose**: Make a choice and add it to the current solution
2. **Explore**: Recursively explore with that choice
3. **Unchoose**: Remove the choice (backtrack) and try next option

### Template

```java
void backtrack(State current) {
    // Base case: solution is complete
    if (isComplete(current)) {
        processSolution(current);
        return;
    }
    
    // Try all possible choices
    for (Choice choice : getAllChoices(current)) {
        // Prune: skip invalid choices
        if (!isValid(current, choice)) {
            continue;
        }
        
        // Choose: make the choice
        makeChoice(current, choice);
        
        // Explore: recurse with the choice
        backtrack(current);
        
        // Unchoose: undo the choice (backtrack)
        undoChoice(current, choice);
    }
}
```

## 🔍 Detailed Example: Generate Subsets

**Problem**: Generate all subsets of [1, 2, 3]

### Approach 1: Include/Exclude Pattern

```java
public List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}

void backtrack(int[] nums, int start, List<Integer> current, 
               List<List<Integer>> result) {
    // Every state is a valid subset
    result.add(new ArrayList<>(current));
    
    for (int i = start; i < nums.length; i++) {
        // Choose: include nums[i]
        current.add(nums[i]);
        
        // Explore: recurse with nums[i] included
        backtrack(nums, i + 1, current, result);
        
        // Unchoose: remove nums[i] (backtrack)
        current.remove(current.size() - 1);
    }
}
```

### Execution Trace

```
Input: [1, 2, 3]

backtrack([], 0):
  Add [] to result
  
  i=0: Choose 1
    backtrack([1], 1):
      Add [1] to result
      
      i=1: Choose 2
        backtrack([1,2], 2):
          Add [1,2] to result
          
          i=2: Choose 3
            backtrack([1,2,3], 3):
              Add [1,2,3] to result
              return (no more elements)
          Unchoose 3
          return
      Unchoose 2
      
      i=2: Choose 3
        backtrack([1,3], 3):
          Add [1,3] to result
          return
      Unchoose 3
      return
  Unchoose 1
  
  i=1: Choose 2
    backtrack([2], 2):
      Add [2] to result
      
      i=2: Choose 3
        backtrack([2,3], 3):
          Add [2,3] to result
          return
      Unchoose 3
      return
  Unchoose 2
  
  i=2: Choose 3
    backtrack([3], 3):
      Add [3] to result
      return
  Unchoose 3

Result: [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]]
```

## 🎨 Common Backtracking Patterns

### Pattern 1: Subsets (Power Set)

**Characteristics**:
- Generate all possible subsets
- Each element: include or exclude
- No ordering constraint

**Template**:
```java
void backtrack(int[] nums, int start, List<Integer> current) {
    result.add(new ArrayList<>(current));
    
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);
        backtrack(nums, i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

**Time**: O(2^n) - Each element has 2 choices

### Pattern 2: Permutations

**Characteristics**:
- Use all elements
- Different orderings
- Track which elements used

**Template**:
```java
void backtrack(int[] nums, List<Integer> current, boolean[] used) {
    if (current.size() == nums.length) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        
        current.add(nums[i]);
        used[i] = true;
        
        backtrack(nums, current, used);
        
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

**Time**: O(n!) - n choices for first, n-1 for second, etc.

### Pattern 3: Combinations

**Characteristics**:
- Choose k elements from n
- Order doesn't matter
- No duplicates

**Template**:
```java
void backtrack(int n, int k, int start, List<Integer> current) {
    if (current.size() == k) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = start; i <= n; i++) {
        current.add(i);
        backtrack(n, k, i + 1, current);
        current.remove(current.size() - 1);
    }
}
```

**Time**: O(C(n,k)) - Binomial coefficient

### Pattern 4: Partition

**Characteristics**:
- Split into groups
- Each element in exactly one group
- Groups satisfy constraints

**Template**:
```java
void backtrack(int[] nums, int index, List<List<Integer>> groups) {
    if (index == nums.length) {
        if (isValidPartition(groups)) {
            result.add(deepCopy(groups));
        }
        return;
    }
    
    // Try adding to existing groups
    for (List<Integer> group : groups) {
        if (canAddToGroup(group, nums[index])) {
            group.add(nums[index]);
            backtrack(nums, index + 1, groups);
            group.remove(group.size() - 1);
        }
    }
    
    // Try creating new group
    List<Integer> newGroup = new ArrayList<>();
    newGroup.add(nums[index]);
    groups.add(newGroup);
    backtrack(nums, index + 1, groups);
    groups.remove(groups.size() - 1);
}
```

### Pattern 5: Grid/Board Search

**Characteristics**:
- Explore grid in multiple directions
- Mark visited cells
- Backtrack to try other paths

**Template**:
```java
boolean backtrack(char[][] board, int row, int col, String word, int index) {
    if (index == word.length()) {
        return true;
    }
    
    if (row < 0 || row >= m || col < 0 || col >= n || 
        board[row][col] != word.charAt(index)) {
        return false;
    }
    
    char temp = board[row][col];
    board[row][col] = '#'; // Mark as visited
    
    boolean found = backtrack(board, row+1, col, word, index+1) ||
                   backtrack(board, row-1, col, word, index+1) ||
                   backtrack(board, row, col+1, word, index+1) ||
                   backtrack(board, row, col-1, word, index+1);
    
    board[row][col] = temp; // Backtrack
    
    return found;
}
```

### Pattern 6: Constraint Satisfaction

**Characteristics**:
- Place items with constraints
- Check validity at each step
- Prune invalid branches early

**Template**:
```java
boolean backtrack(int row) {
    if (row == n) {
        return true; // All rows filled
    }
    
    for (int col = 0; col < n; col++) {
        if (isValid(row, col)) {
            // Choose
            placeQueen(row, col);
            
            // Explore
            if (backtrack(row + 1)) {
                return true;
            }
            
            // Unchoose
            removeQueen(row, col);
        }
    }
    
    return false;
}
```

## 💡 Key Concepts

### 1. State Space Tree

Backtracking explores a state space tree where:
- **Nodes**: Partial solutions
- **Edges**: Choices/decisions
- **Leaves**: Complete solutions or dead ends

```
Example: Permutations of [1,2,3]

                    []
          /         |         \
        [1]        [2]        [3]
       /   \      /   \      /   \
    [1,2] [1,3] [2,1] [2,3] [3,1] [3,2]
      |     |     |     |     |     |
   [1,2,3][1,3,2][2,1,3][2,3,1][3,1,2][3,2,1]
```

### 2. Pruning

**Pruning** = Cutting off branches that cannot lead to valid solutions

**Benefits**:
- Reduces time complexity
- Avoids exploring impossible paths
- Critical for performance

**Example: N-Queens**
```java
boolean isValid(int row, int col) {
    // Check column
    for (int i = 0; i < row; i++) {
        if (board[i][col] == 'Q') return false;
    }
    
    // Check diagonal
    for (int i = row-1, j = col-1; i >= 0 && j >= 0; i--, j--) {
        if (board[i][j] == 'Q') return false;
    }
    
    // Check anti-diagonal
    for (int i = row-1, j = col+1; i >= 0 && j < n; i--, j++) {
        if (board[i][j] == 'Q') return false;
    }
    
    return true;
}
```

### 3. Backtracking vs Brute Force

**Brute Force**: Generate all possibilities, then filter
```java
// Generate all permutations, then check validity
List<List<Integer>> all = generateAll();
for (List<Integer> perm : all) {
    if (isValid(perm)) {
        result.add(perm);
    }
}
```

**Backtracking**: Build solutions incrementally, prune early
```java
// Build only valid solutions
void backtrack(List<Integer> current) {
    if (isComplete(current)) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int choice : choices) {
        if (isValid(current, choice)) { // Prune here!
            current.add(choice);
            backtrack(current);
            current.remove(current.size() - 1);
        }
    }
}
```

## 🚫 Common Mistakes

### 1. Not Making a Copy

```java
// ❌ Wrong: All results point to same list
result.add(current);

// ✅ Correct: Create new copy
result.add(new ArrayList<>(current));
```

### 2. Forgetting to Backtrack

```java
// ❌ Wrong: Doesn't undo choice
current.add(choice);
backtrack(current);
// Missing: current.remove(current.size() - 1);

// ✅ Correct: Always backtrack
current.add(choice);
backtrack(current);
current.remove(current.size() - 1);
```

### 3. Wrong Base Case

```java
// ❌ Wrong: Checks after adding
for (int i = start; i < n; i++) {
    current.add(i);
    if (current.size() == k) {
        result.add(new ArrayList<>(current));
    }
    backtrack(i + 1, current);
    current.remove(current.size() - 1);
}

// ✅ Correct: Check before loop
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

### 4. Not Handling Duplicates

```java
// ❌ Wrong: Generates duplicate subsets
for (int i = start; i < nums.length; i++) {
    current.add(nums[i]);
    backtrack(i + 1, current);
    current.remove(current.size() - 1);
}

// ✅ Correct: Skip duplicates
Arrays.sort(nums); // Sort first
for (int i = start; i < nums.length; i++) {
    if (i > start && nums[i] == nums[i-1]) continue; // Skip duplicates
    current.add(nums[i]);
    backtrack(i + 1, current);
    current.remove(current.size() - 1);
}
```

## 📈 Complexity Analysis

### Time Complexity

**General Formula**: O(b^d)
- b = branching factor (choices at each level)
- d = depth of recursion tree

**Specific Cases**:
- Subsets: O(2^n) - 2 choices per element
- Permutations: O(n!) - n choices, then n-1, etc.
- Combinations: O(C(n,k)) - Binomial coefficient
- N-Queens: O(n!) with pruning

### Space Complexity

**Recursion Stack**: O(d) where d is max depth
**Auxiliary Space**: Depends on what you track
- Visited array: O(n)
- Current solution: O(k) where k is solution size

## 🎓 When to Use Backtracking

### Use Backtracking When:

✅ Need to find **all** solutions  
✅ Problem has **constraints** to satisfy  
✅ Can **prune** invalid branches early  
✅ Solution built **incrementally**  
✅ Need to explore **all possibilities**  

### Don't Use Backtracking When:

❌ Only need **one** solution (use greedy/DP)  
❌ No way to **prune** (pure brute force better)  
❌ Problem has **optimal substructure** (use DP)  
❌ Can solve with **greedy** approach  

## 🔍 Optimization Techniques

### 1. Early Termination

```java
void backtrack(List<Integer> current, int sum, int target) {
    if (sum > target) return; // Prune: exceeded target
    if (sum == target) {
        result.add(new ArrayList<>(current));
        return;
    }
    // Continue exploring...
}
```

### 2. Sorting for Pruning

```java
Arrays.sort(candidates); // Sort first
void backtrack(int start, int target) {
    if (target == 0) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = start; i < candidates.length; i++) {
        if (candidates[i] > target) break; // Prune: rest are larger
        current.add(candidates[i]);
        backtrack(i, target - candidates[i]);
        current.remove(current.size() - 1);
    }
}
```

### 3. Memoization (Backtracking + DP)

```java
Map<String, Boolean> memo = new HashMap<>();

boolean backtrack(String state) {
    if (memo.containsKey(state)) {
        return memo.get(state);
    }
    
    // Backtracking logic...
    boolean result = /* compute */;
    
    memo.put(state, result);
    return result;
}
```

## 🎯 Learning Path

1. **Understand Template**: Master the basic backtracking template
2. **Practice Subsets**: Start with subset generation
3. **Learn Permutations**: Move to permutation problems
4. **Try Combinations**: Practice combination problems
5. **Grid Problems**: Word search, path finding
6. **Constraint Problems**: N-Queens, Sudoku
7. **Optimize**: Learn pruning techniques

---

**Next**: [Pattern Recognition Guide](02_Pattern_Recognition.md)
