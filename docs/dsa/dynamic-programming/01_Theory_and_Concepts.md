# Dynamic Programming - Theory and Concepts

## 📖 What is Dynamic Programming?

**Dynamic Programming (DP)** is an algorithmic technique for solving optimization problems by breaking them down into simpler overlapping subproblems and storing their solutions to avoid redundant calculations.

### Core Idea

Instead of solving the same subproblem multiple times, we solve it once and store the result for future use.

```
Without DP: Solve same subproblem repeatedly → Exponential time
With DP: Solve each subproblem once → Polynomial time
```

## 🎯 Two Key Properties

### 1. Overlapping Subproblems

The problem can be broken down into subproblems which are reused several times.

**Example: Fibonacci**
```
fib(5) = fib(4) + fib(3)
fib(4) = fib(3) + fib(2)
fib(3) = fib(2) + fib(1)

Notice: fib(3) and fib(2) are calculated multiple times!
```

### 2. Optimal Substructure

An optimal solution to the problem contains optimal solutions to subproblems.

**Example: Shortest Path**
```
If shortest path from A to C goes through B:
  Shortest(A → C) = Shortest(A → B) + Shortest(B → C)

The path A → B must also be shortest, otherwise we could improve A → C.
```

## 🔍 DP vs Other Techniques

### DP vs Divide and Conquer

| Aspect | DP | Divide and Conquer |
|--------|----|--------------------|
| Subproblems | Overlapping | Independent |
| Reuse | Yes (memoization) | No |
| Example | Fibonacci | Merge Sort |
| Time | Polynomial | Often O(n log n) |

### DP vs Greedy

| Aspect | DP | Greedy |
|--------|----|--------------------|
| Approach | Consider all options | Make local optimal choice |
| Guarantee | Always optimal | Not always optimal |
| Time | Slower | Faster |
| Example | Coin Change | Activity Selection |

## 🎨 Two Main Approaches

### 1. Memoization (Top-Down)

Start with recursive solution and add caching.

**Process**:
1. Write recursive solution
2. Add cache (usually array or hash map)
3. Check cache before computing
4. Store result in cache

**Template**:
```java
int solve(int n, int[] memo) {
    // Base case
    if (n <= 1) return n;
    
    // Check cache
    if (memo[n] != -1) return memo[n];
    
    // Compute and store
    memo[n] = solve(n-1, memo) + solve(n-2, memo);
    
    return memo[n];
}
```

**Advantages**:
✅ Easy to write (start from recursion)  
✅ Only computes needed subproblems  
✅ Natural for problems with complex dependencies  

**Disadvantages**:
❌ Recursion overhead (stack space)  
❌ May hit stack overflow for large inputs  

### 2. Tabulation (Bottom-Up)

Build solution iteratively from base cases.

**Process**:
1. Identify base cases
2. Create DP table
3. Fill table in order
4. Return final answer

**Template**:
```java
int solve(int n) {
    // Base cases
    if (n <= 1) return n;
    
    // Create table
    int[] dp = new int[n + 1];
    dp[0] = 0;
    dp[1] = 1;
    
    // Fill table
    for (int i = 2; i <= n; i++) {
        dp[i] = dp[i-1] + dp[i-2];
    }
    
    return dp[n];
}
```

**Advantages**:
✅ No recursion overhead  
✅ Better space locality  
✅ Easier to optimize space  

**Disadvantages**:
❌ Must compute all subproblems  
❌ Harder to write initially  

## 📊 Common DP Patterns

### Pattern 1: Linear DP (1D)

**Characteristics**:
- State depends on previous states
- 1D array for DP table
- Usually O(n) time, O(n) or O(1) space

**Examples**: Climbing Stairs, House Robber, Fibonacci

**Template**:
```java
int[] dp = new int[n + 1];
dp[0] = base_case_0;
dp[1] = base_case_1;

for (int i = 2; i <= n; i++) {
    dp[i] = function(dp[i-1], dp[i-2], ...);
}

return dp[n];
```

**State Definition**: `dp[i]` = answer for input of size i

### Pattern 2: 2D DP (Grid)

**Characteristics**:
- Two dimensions of state
- 2D array for DP table
- Usually O(m × n) time and space

**Examples**: Unique Paths, Longest Common Subsequence, Edit Distance

**Template**:
```java
int[][] dp = new int[m + 1][n + 1];

// Initialize base cases
for (int i = 0; i <= m; i++) dp[i][0] = base_case;
for (int j = 0; j <= n; j++) dp[0][j] = base_case;

// Fill table
for (int i = 1; i <= m; i++) {
    for (int j = 1; j <= n; j++) {
        dp[i][j] = function(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]);
    }
}

return dp[m][n];
```

**State Definition**: `dp[i][j]` = answer for inputs of size i and j

### Pattern 3: Subsequence DP

**Characteristics**:
- Find longest/shortest subsequence
- Often 2D DP
- Compare characters/elements

**Examples**: LCS, LIS, Edit Distance

**Template**:
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

return dp[m][n];
```

### Pattern 4: Knapsack DP

**Characteristics**:
- Choose items with constraints
- Maximize/minimize value
- 2D DP: items × capacity

**Examples**: 0/1 Knapsack, Coin Change, Partition

**Template**:
```java
int[][] dp = new int[n + 1][capacity + 1];

for (int i = 1; i <= n; i++) {
    for (int w = 1; w <= capacity; w++) {
        if (weight[i-1] <= w) {
            dp[i][w] = Math.max(
                dp[i-1][w],  // Don't take item
                dp[i-1][w - weight[i-1]] + value[i-1]  // Take item
            );
        } else {
            dp[i][w] = dp[i-1][w];
        }
    }
}

return dp[n][capacity];
```

### Pattern 5: Interval DP

**Characteristics**:
- Process intervals/ranges
- Often palindrome problems
- Fill diagonal first, then expand

**Examples**: Longest Palindromic Substring, Matrix Chain Multiplication

**Template**:
```java
boolean[][] dp = new boolean[n][n];

// Base case: single characters
for (int i = 0; i < n; i++) {
    dp[i][i] = true;
}

// Fill for increasing lengths
for (int len = 2; len <= n; len++) {
    for (int i = 0; i <= n - len; i++) {
        int j = i + len - 1;
        
        if (s[i] == s[j]) {
            dp[i][j] = (len == 2) || dp[i+1][j-1];
        }
    }
}
```

### Pattern 6: State Machine DP

**Characteristics**:
- Multiple states at each step
- Transition between states
- Track best value for each state

**Examples**: Stock Trading, Paint House

**Template**:
```java
int[][] dp = new int[n][num_states];

// Initialize first state
dp[0][state0] = initial_value;

for (int i = 1; i < n; i++) {
    for (int state = 0; state < num_states; state++) {
        for (int prev_state = 0; prev_state < num_states; prev_state++) {
            if (can_transition(prev_state, state)) {
                dp[i][state] = Math.max(
                    dp[i][state],
                    dp[i-1][prev_state] + transition_cost
                );
            }
        }
    }
}

return max(dp[n-1][all_states]);
```

## 💡 Step-by-Step DP Problem Solving

### Step 1: Identify if it's a DP Problem

**Ask**:
- Can I break it into subproblems?
- Do subproblems overlap?
- Does optimal solution use optimal subproblems?
- Is it asking for optimization or counting?

### Step 2: Define the State

**State** = What information do I need to solve the subproblem?

**Examples**:
- Fibonacci: `dp[i]` = fib(i)
- Climbing Stairs: `dp[i]` = ways to reach step i
- LCS: `dp[i][j]` = LCS of s1[0..i] and s2[0..j]
- Knapsack: `dp[i][w]` = max value using first i items with weight limit w

### Step 3: Write the Recurrence Relation

**Relation** = How to compute current state from previous states?

**Examples**:
- Fibonacci: `dp[i] = dp[i-1] + dp[i-2]`
- Climbing Stairs: `dp[i] = dp[i-1] + dp[i-2]`
- LCS: `dp[i][j] = dp[i-1][j-1] + 1` if match, else `max(dp[i-1][j], dp[i][j-1])`

### Step 4: Identify Base Cases

**Base cases** = Smallest subproblems with known answers

**Examples**:
- Fibonacci: `dp[0] = 0, dp[1] = 1`
- Climbing Stairs: `dp[0] = 1, dp[1] = 1`
- LCS: `dp[0][j] = 0, dp[i][0] = 0`

### Step 5: Determine Computation Order

**Order** = In what order to fill the DP table?

**Examples**:
- 1D: Left to right (i = 0 to n)
- 2D: Row by row, or diagonal
- Depends on dependencies

### Step 6: Implement

**Choose approach**:
- Memoization: If recursion is natural
- Tabulation: If iteration is clearer

### Step 7: Optimize Space

**Analyze dependencies**:
- If only need previous row: O(n) space
- If only need previous few values: O(1) space

## 🔍 Example: Climbing Stairs

**Problem**: You can climb 1 or 2 steps at a time. How many ways to reach step n?

### Step 1: Identify DP
- Subproblems: Ways to reach each step
- Overlapping: Ways to reach step i used multiple times
- Optimal substructure: Ways to reach n uses ways to reach n-1 and n-2

### Step 2: Define State
`dp[i]` = number of ways to reach step i

### Step 3: Recurrence
```
dp[i] = dp[i-1] + dp[i-2]

Why? To reach step i, you can:
- Come from step i-1 (take 1 step)
- Come from step i-2 (take 2 steps)
```

### Step 4: Base Cases
```
dp[0] = 1  (one way: don't climb)
dp[1] = 1  (one way: take 1 step)
```

### Step 5: Order
Fill from left to right: dp[0], dp[1], dp[2], ..., dp[n]

### Step 6: Implement (Tabulation)
```java
public int climbStairs(int n) {
    if (n <= 1) return 1;
    
    int[] dp = new int[n + 1];
    dp[0] = 1;
    dp[1] = 1;
    
    for (int i = 2; i <= n; i++) {
        dp[i] = dp[i-1] + dp[i-2];
    }
    
    return dp[n];
}
```

### Step 7: Optimize Space
```java
public int climbStairs(int n) {
    if (n <= 1) return 1;
    
    int prev2 = 1, prev1 = 1;
    
    for (int i = 2; i <= n; i++) {
        int curr = prev1 + prev2;
        prev2 = prev1;
        prev1 = curr;
    }
    
    return prev1;
}
```

## 🚫 Common Mistakes

### 1. Wrong State Definition
```java
// ❌ Wrong: Doesn't capture all needed information
dp[i] = max profit  // But from which state?

// ✅ Correct: Include all relevant information
dp[i][holding] = max profit at day i, holding stock or not
```

### 2. Wrong Base Cases
```java
// ❌ Wrong: Incorrect initialization
dp[0] = 0;  // Should be 1 for counting problems

// ✅ Correct
dp[0] = 1;  // One way to do nothing
```

### 3. Wrong Order of Computation
```java
// ❌ Wrong: Computing dp[i] before dp[i-1]
for (int i = n; i >= 0; i--) {
    dp[i] = dp[i-1] + dp[i-2];  // dp[i-1] not computed yet!
}

// ✅ Correct
for (int i = 2; i <= n; i++) {
    dp[i] = dp[i-1] + dp[i-2];
}
```

### 4. Not Handling Edge Cases
```java
// ❌ Wrong: Array index out of bounds
dp[i] = dp[i-1] + dp[i-2];  // What if i = 0 or i = 1?

// ✅ Correct
if (i <= 1) return base_case;
dp[i] = dp[i-1] + dp[i-2];
```

## 📈 Complexity Analysis

### Time Complexity

**Memoization**:
- Number of unique subproblems × Time per subproblem
- Example: Fibonacci → O(n) subproblems × O(1) time = O(n)

**Tabulation**:
- Size of DP table × Time to fill each cell
- Example: LCS → O(m × n) cells × O(1) time = O(m × n)

### Space Complexity

**Memoization**:
- Cache size + Recursion stack
- Example: Fibonacci → O(n) cache + O(n) stack = O(n)

**Tabulation**:
- DP table size
- Example: LCS → O(m × n) table

**Space Optimized**:
- Only keep necessary states
- Example: Fibonacci → O(1) (only prev2, prev1)

## 🎓 Learning Path

1. **Understand Basics**: Learn what DP is and when to use it
2. **Master 1D DP**: Fibonacci, Climbing Stairs, House Robber
3. **Learn 2D DP**: Unique Paths, LCS, Edit Distance
4. **Practice Patterns**: Knapsack, Subsequence, Interval
5. **Optimize**: Space optimization techniques
6. **Advanced**: State machine, bitmask DP

---

**Next**: [Pattern Recognition Guide](02_Pattern_Recognition.md)
