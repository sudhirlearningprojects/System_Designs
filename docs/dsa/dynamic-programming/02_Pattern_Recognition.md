# Dynamic Programming - Pattern Recognition Guide

## 🎯 How to Identify DP Problems

This guide helps you recognize when to use Dynamic Programming by analyzing problem characteristics.

## 🔍 Recognition Checklist

### ✅ Strong Indicators

1. **Optimization keywords**
   - "Maximum", "Minimum", "Longest", "Shortest"
   - "Optimize", "Best", "Largest", "Smallest"
   
2. **Counting keywords**
   - "Count number of ways"
   - "How many ways"
   - "Number of paths"
   
3. **Decision keywords**
   - "Is it possible"
   - "Can you"
   - "Find if"
   
4. **Overlapping subproblems**
   - Same calculation repeated
   - Recursive structure with repetition
   
5. **Optimal substructure**
   - Optimal solution uses optimal subsolutions
   - Can break into smaller similar problems

### ⚠️ Moderate Indicators

1. **Choices at each step**
   - Take or skip
   - Multiple options
   
2. **Constraints**
   - Limited resources
   - Capacity limits
   
3. **Sequences/Arrays**
   - Process elements in order
   - Subsequences or subarrays

## 📋 Decision Tree

```
Does the problem ask for optimization or counting?
│
├─ YES ──→ Can you break it into subproblems?
│          │
│          ├─ YES ──→ Do subproblems overlap?
│          │          │
│          │          ├─ YES ──→ Does optimal solution use optimal subsolutions?
│          │          │          │
│          │          │          ├─ YES ──→ USE DYNAMIC PROGRAMMING ✓
│          │          │          │
│          │          │          └─ NO ──→ Try Greedy or other approaches
│          │          │
│          │          └─ NO ──→ Use Divide and Conquer
│          │
│          └─ NO ──→ Not DP
│
└─ NO ──→ Not DP (might be graph, sorting, etc.)
```

## 🎨 Pattern Matching Examples

### Pattern 1: Linear DP (1D)

**Keywords**: sequence, array, previous elements, fibonacci-like

**Example Problems**:
```
✓ "Climbing stairs with 1 or 2 steps"
✓ "House robber - can't rob adjacent houses"
✓ "Maximum sum non-adjacent elements"
✓ "Decode ways"
```

**Recognition**:
- State depends on previous few states
- 1D array sufficient
- Usually O(n) time

**Template Match**:
```java
int[] dp = new int[n + 1];
dp[0] = base_case_0;
dp[1] = base_case_1;

for (int i = 2; i <= n; i++) {
    dp[i] = function(dp[i-1], dp[i-2]);
}

return dp[n];
```

### Pattern 2: 2D DP (Grid/Matrix)

**Keywords**: grid, matrix, two sequences, paths

**Example Problems**:
```
✓ "Unique paths in grid"
✓ "Minimum path sum"
✓ "Longest common subsequence"
✓ "Edit distance"
```

**Recognition**:
- Two dimensions of input
- Grid traversal
- Compare two sequences

**Template Match**:
```java
int[][] dp = new int[m + 1][n + 1];

// Initialize base cases
for (int i = 0; i <= m; i++) dp[i][0] = base;
for (int j = 0; j <= n; j++) dp[0][j] = base;

for (int i = 1; i <= m; i++) {
    for (int j = 1; j <= n; j++) {
        dp[i][j] = function(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]);
    }
}

return dp[m][n];
```

### Pattern 3: Knapsack DP

**Keywords**: capacity, weight, value, choose items, subset

**Example Problems**:
```
✓ "0/1 Knapsack"
✓ "Coin change"
✓ "Partition equal subset sum"
✓ "Target sum"
```

**Recognition**:
- Choose items with constraints
- Maximize/minimize value
- Limited capacity

**Template Match**:
```java
int[][] dp = new int[n + 1][capacity + 1];

for (int i = 1; i <= n; i++) {
    for (int w = 1; w <= capacity; w++) {
        if (weight[i-1] <= w) {
            dp[i][w] = Math.max(
                dp[i-1][w],  // Don't take
                dp[i-1][w - weight[i-1]] + value[i-1]  // Take
            );
        } else {
            dp[i][w] = dp[i-1][w];
        }
    }
}
```

### Pattern 4: Subsequence DP

**Keywords**: subsequence, substring, longest, common

**Example Problems**:
```
✓ "Longest increasing subsequence"
✓ "Longest common subsequence"
✓ "Longest palindromic subsequence"
✓ "Edit distance"
```

**Recognition**:
- Find longest/shortest subsequence
- Compare characters
- Often 2D DP

**Template Match**:
```java
int[][] dp = new int[m + 1][n + 1];

for (int i = 1; i <= m; i++) {
    for (int j = 1; j <= n; j++) {
        if (s1[i-1] == s2[j-1]) {
            dp[i][j] = dp[i-1][j-1] + 1;
        } else {
            dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
        }
    }
}
```

### Pattern 5: Interval DP

**Keywords**: palindrome, interval, range, substring

**Example Problems**:
```
✓ "Longest palindromic substring"
✓ "Palindrome partitioning"
✓ "Matrix chain multiplication"
✓ "Burst balloons"
```

**Recognition**:
- Process intervals/ranges
- Often palindrome problems
- Fill diagonal first

**Template Match**:
```java
boolean[][] dp = new boolean[n][n];

// Base case: single elements
for (int i = 0; i < n; i++) {
    dp[i][i] = true;
}

// Fill for increasing lengths
for (int len = 2; len <= n; len++) {
    for (int i = 0; i <= n - len; i++) {
        int j = i + len - 1;
        dp[i][j] = check(i, j, dp);
    }
}
```

### Pattern 6: State Machine DP

**Keywords**: states, transitions, buy/sell, multiple stages

**Example Problems**:
```
✓ "Best time to buy and sell stock"
✓ "Paint house"
✓ "Maximum profit with cooldown"
✓ "House robber with states"
```

**Recognition**:
- Multiple states at each step
- Transitions between states
- Track best for each state

**Template Match**:
```java
int[][] dp = new int[n][num_states];

dp[0][state0] = initial;

for (int i = 1; i < n; i++) {
    for (int state = 0; state < num_states; state++) {
        dp[i][state] = best_transition(dp[i-1], state);
    }
}
```

## 🔑 Key Questions to Ask

### Question 1: Is it optimization or counting?

**Optimization** (max/min) → DP likely  
**Counting** (number of ways) → DP likely  
**Search** (find element) → Not DP  

### Question 2: Can I break it into subproblems?

**YES** → Continue checking  
**NO** → Not DP  

### Question 3: Do subproblems overlap?

**YES** → DP is good  
**NO** → Use Divide and Conquer  

### Question 4: Does optimal solution use optimal subsolutions?

**YES** → DP works  
**NO** → Try Greedy  

## 📊 Problem Type Matrix

| Problem Type | Pattern | State | Example |
|--------------|---------|-------|---------|
| Fibonacci-like | Linear 1D | dp[i] | Climbing Stairs |
| Grid paths | 2D Grid | dp[i][j] | Unique Paths |
| Two sequences | 2D Subsequence | dp[i][j] | LCS |
| Choose items | Knapsack | dp[i][w] | Coin Change |
| Palindrome | Interval | dp[i][j] | Longest Palindrome |
| Multiple states | State Machine | dp[i][state] | Stock Trading |
| Partition | Knapsack | dp[i][sum] | Partition Sum |

## 🎯 Common Problem Phrases

### Optimization Triggers

- "maximum sum"
- "minimum cost"
- "longest subsequence"
- "shortest path"
- "largest value"
- "smallest number"
- "best strategy"
- "optimize"

### Counting Triggers

- "count number of ways"
- "how many ways"
- "number of paths"
- "total ways"
- "count possibilities"

### Decision Triggers

- "is it possible"
- "can you"
- "find if exists"
- "determine whether"

## 🚫 When NOT to Use DP

### 1. No Overlapping Subproblems
```
Problem: "Merge sort an array"
Solution: Divide and Conquer (subproblems don't overlap)
```

### 2. Greedy Works
```
Problem: "Activity selection"
Solution: Greedy (local optimal = global optimal)
```

### 3. Simple Iteration
```
Problem: "Find maximum in array"
Solution: Simple loop (no subproblems)
```

### 4. Graph Algorithms
```
Problem: "Shortest path in weighted graph"
Solution: Dijkstra's algorithm
```

## 💡 Conversion Examples

### Example 1: Recursion → DP

**Problem**: Fibonacci

**Recursion** (O(2^n)):
```java
int fib(int n) {
    if (n <= 1) return n;
    return fib(n-1) + fib(n-2);
}
```

**Memoization** (O(n)):
```java
int fib(int n, int[] memo) {
    if (n <= 1) return n;
    if (memo[n] != 0) return memo[n];
    memo[n] = fib(n-1, memo) + fib(n-2, memo);
    return memo[n];
}
```

**Tabulation** (O(n)):
```java
int fib(int n) {
    if (n <= 1) return n;
    int[] dp = new int[n + 1];
    dp[0] = 0;
    dp[1] = 1;
    for (int i = 2; i <= n; i++) {
        dp[i] = dp[i-1] + dp[i-2];
    }
    return dp[n];
}
```

**Space Optimized** (O(1)):
```java
int fib(int n) {
    if (n <= 1) return n;
    int prev2 = 0, prev1 = 1;
    for (int i = 2; i <= n; i++) {
        int curr = prev1 + prev2;
        prev2 = prev1;
        prev1 = curr;
    }
    return prev1;
}
```

## 🎓 Practice Strategy

### Level 1: Recognize Pattern
1. Read problem statement
2. Identify keywords
3. Check for DP characteristics
4. Determine which pattern

### Level 2: Define State
1. What information needed?
2. 1D or 2D?
3. What does dp[i] or dp[i][j] represent?

### Level 3: Write Recurrence
1. How to compute current from previous?
2. What are the choices?
3. Base cases?

### Level 4: Implement
1. Start with memoization
2. Convert to tabulation
3. Optimize space

## 📝 Quick Recognition Card

```
┌─────────────────────────────────────────────────────────┐
│ DYNAMIC PROGRAMMING PATTERN RECOGNITION                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ LINEAR DP (1D):                                         │
│   ✓ Fibonacci-like                                     │
│   ✓ Previous few states                                │
│   ✓ Sequence problems                                  │
│   → dp[i] = f(dp[i-1], dp[i-2])                        │
│                                                         │
│ 2D DP (GRID):                                           │
│   ✓ Grid paths                                         │
│   ✓ Two sequences                                      │
│   ✓ Matrix problems                                    │
│   → dp[i][j] = f(dp[i-1][j], dp[i][j-1])              │
│                                                         │
│ KNAPSACK:                                               │
│   ✓ Choose items                                       │
│   ✓ Capacity constraint                                │
│   ✓ Maximize/minimize value                            │
│   → dp[i][w] = max(take, skip)                         │
│                                                         │
│ SUBSEQUENCE:                                            │
│   ✓ Longest/shortest subsequence                       │
│   ✓ Compare characters                                 │
│   → dp[i][j] based on match                            │
│                                                         │
│ INTERVAL:                                               │
│   ✓ Palindrome problems                                │
│   ✓ Range queries                                      │
│   → dp[i][j] for substring [i..j]                      │
│                                                         │
│ STATE MACHINE:                                          │
│   ✓ Multiple states                                    │
│   ✓ Transitions                                        │
│   → dp[i][state]                                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 🔍 Problem Analysis Framework

### Step 1: Identify Type
- Optimization? Counting? Decision?
- What are we optimizing/counting?

### Step 2: Check DP Characteristics
- Overlapping subproblems?
- Optimal substructure?

### Step 3: Choose Pattern
- 1D, 2D, Knapsack, Subsequence, Interval, State Machine?

### Step 4: Define State
- What information needed?
- What does dp[...] represent?

### Step 5: Write Recurrence
- How to compute current state?
- What are the transitions?

### Step 6: Implement
- Memoization or Tabulation?
- Handle base cases
- Fill in correct order

### Step 7: Optimize
- Can reduce space?
- Only need previous row/values?

---

**Next**: [Easy Problems](03_Easy_Problems.md)
