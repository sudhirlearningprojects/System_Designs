# Dynamic Programming - Quick Reference Cheat Sheet

## 🎯 Core Templates

### Memoization (Top-Down)
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

### Tabulation (Bottom-Up)
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

### Space Optimized
```java
int solve(int n) {
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

## 📋 Pattern Templates

### Pattern 1: Linear DP (1D)
```java
int[] dp = new int[n + 1];
dp[0] = base_case_0;
dp[1] = base_case_1;

for (int i = 2; i <= n; i++) {
    dp[i] = function(dp[i-1], dp[i-2]);
}

return dp[n];
```
**Use for**: Fibonacci, Climbing Stairs, House Robber

### Pattern 2: 2D DP (Grid)
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
**Use for**: Unique Paths, LCS, Edit Distance

### Pattern 3: Knapsack
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

return dp[n][capacity];
```
**Use for**: 0/1 Knapsack, Coin Change, Partition Sum

### Pattern 4: Subsequence
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
**Use for**: LCS, LIS, Edit Distance

### Pattern 5: Interval DP
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
**Use for**: Palindrome problems, Matrix Chain Multiplication

### Pattern 6: State Machine
```java
int[][] dp = new int[n][num_states];

dp[0][state0] = initial;

for (int i = 1; i < n; i++) {
    for (int state = 0; state < num_states; state++) {
        dp[i][state] = best_transition(dp[i-1], state);
    }
}

return max(dp[n-1][all_states]);
```
**Use for**: Stock Trading, Paint House

## 🔑 When to Use DP

| Characteristic | Indicator |
|----------------|-----------|
| **Optimization** | Maximum, Minimum, Longest, Shortest |
| **Counting** | Number of ways, Count paths |
| **Decision** | Is it possible, Can you |
| **Overlapping** | Same subproblems solved multiple times |
| **Optimal Substructure** | Optimal solution uses optimal subsolutions |

## ⚡ Time & Space Complexity

| Pattern | Time | Space | Space Optimized |
|---------|------|-------|-----------------|
| Linear 1D | O(n) | O(n) | O(1) |
| 2D Grid | O(m×n) | O(m×n) | O(n) |
| Knapsack | O(n×W) | O(n×W) | O(W) |
| Subsequence | O(m×n) | O(m×n) | O(n) |
| Interval | O(n²) or O(n³) | O(n²) | Hard to optimize |

## 🚫 Common Mistakes

1. **Wrong state definition**
   ```java
   // ❌ Wrong - incomplete state
   dp[i] = max profit
   
   // ✅ Correct - complete state
   dp[i][holding] = max profit at day i, holding stock or not
   ```

2. **Wrong base cases**
   ```java
   // ❌ Wrong
   dp[0] = 0;  // Should be 1 for counting
   
   // ✅ Correct
   dp[0] = 1;  // One way to do nothing
   ```

3. **Wrong computation order**
   ```java
   // ❌ Wrong - computing before dependencies
   for (int i = n; i >= 0; i--) {
       dp[i] = dp[i-1] + dp[i-2];
   }
   
   // ✅ Correct
   for (int i = 2; i <= n; i++) {
       dp[i] = dp[i-1] + dp[i-2];
   }
   ```

4. **Not handling edge cases**
   ```java
   // ❌ Wrong - array out of bounds
   dp[i] = dp[i-1] + dp[i-2];
   
   // ✅ Correct
   if (i <= 1) return base_case;
   dp[i] = dp[i-1] + dp[i-2];
   ```

## 📊 All 20 Problems Summary

### Easy (8 problems)
1. Climbing Stairs - O(n) - Linear 1D
2. Fibonacci - O(n) - Linear 1D
3. Min Cost Stairs - O(n) - Linear 1D
4. House Robber - O(n) - Linear 1D
5. Maximum Subarray - O(n) - Kadane's
6. Buy Sell Stock - O(n) - State Machine
7. Divisor Game - O(n²) - Game Theory
8. Tribonacci - O(n) - Linear 1D

### Medium (10 problems)
1. LIS - O(n²) - Subsequence
2. Coin Change - O(n×m) - Knapsack
3. LCS - O(m×n) - 2D Subsequence
4. Unique Paths - O(m×n) - 2D Grid
5. House Robber II - O(n) - Linear + Constraint
6. Decode Ways - O(n) - Linear
7. Word Break - O(n²) - Linear + Dict
8. Partition Sum - O(n×sum) - Knapsack
9. Longest Palindrome - O(n²) - Interval
10. Max Product - O(n) - Linear

### Hard (2 problems)
1. Edit Distance - O(m×n) - 2D Subsequence
2. Regex Matching - O(m×n) - 2D Pattern

## 🎓 Study Plan

### Week 1: Fundamentals
- Day 1-2: Theory and memoization
- Day 3-4: Easy problems 1-4
- Day 5-6: Easy problems 5-8
- Day 7: Review

### Week 2: 2D DP
- Day 1-2: 2D DP theory
- Day 3-4: Medium problems 1-5
- Day 5-6: Medium problems 6-10
- Day 7: Review

### Week 3: Advanced
- Day 1-3: Hard problem 1 (Edit Distance)
- Day 4-6: Hard problem 2 (Regex Matching)
- Day 7: Mixed practice

## 💡 Interview Tips

1. **Identify DP**
   - Optimization or counting?
   - Overlapping subproblems?
   - Optimal substructure?

2. **Define State**
   - What information needed?
   - 1D or 2D?
   - What does dp[i] represent?

3. **Write Recurrence**
   - How to compute current from previous?
   - What are the choices?

4. **Handle Base Cases**
   - Empty input
   - Single element
   - Edge cases

5. **Optimize**
   - Can reduce space?
   - Only need previous row/values?

## 🔗 Related Patterns

- **Greedy**: When local optimal = global optimal
- **Backtracking**: When need all solutions (not just optimal)
- **Divide and Conquer**: When subproblems don't overlap
- **Graph DP**: DP on trees and DAGs

## 📝 Quick Decision Guide

```
┌─────────────────────────────────────────────────────────┐
│ DYNAMIC PROGRAMMING DECISION GUIDE                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ STEP 1: Is it optimization or counting?                │
│   ✓ Maximum/Minimum → Likely DP                        │
│   ✓ Count ways → Likely DP                             │
│   ✗ Search/Find → Not DP                               │
│                                                         │
│ STEP 2: Can break into subproblems?                    │
│   ✓ Yes → Continue                                     │
│   ✗ No → Not DP                                        │
│                                                         │
│ STEP 3: Do subproblems overlap?                        │
│   ✓ Yes → DP is good                                   │
│   ✗ No → Divide and Conquer                            │
│                                                         │
│ STEP 4: Optimal substructure?                          │
│   ✓ Yes → Use DP                                       │
│   ✗ No → Try Greedy                                    │
│                                                         │
│ CHOOSE PATTERN:                                         │
│   • Fibonacci-like → Linear 1D                         │
│   • Grid paths → 2D Grid                               │
│   • Two sequences → 2D Subsequence                     │
│   • Choose items → Knapsack                            │
│   • Palindrome → Interval                              │
│   • Multiple states → State Machine                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## ✅ Mastery Checklist

- [ ] Understand DP fundamentals
- [ ] Can identify DP problems
- [ ] Master memoization
- [ ] Master tabulation
- [ ] Can optimize space
- [ ] Solved all 8 easy problems
- [ ] Solved all 10 medium problems
- [ ] Solved both hard problems
- [ ] Can write recurrence relations
- [ ] Complete problems in time limit

## 🎯 Key Formulas

**Fibonacci Pattern**:
```java
dp[i] = dp[i-1] + dp[i-2]
```

**Knapsack Pattern**:
```java
dp[i][w] = max(dp[i-1][w], dp[i-1][w-weight[i]] + value[i])
```

**LCS Pattern**:
```java
if (s1[i] == s2[j]):
    dp[i][j] = dp[i-1][j-1] + 1
else:
    dp[i][j] = max(dp[i-1][j], dp[i][j-1])
```

**Space Optimization**:
```java
// From O(n) to O(1)
int prev2 = base1, prev1 = base2;
for (int i = 2; i <= n; i++) {
    int curr = prev1 + prev2;
    prev2 = prev1;
    prev1 = curr;
}
```

## 🔍 Problem-Solving Framework

1. **Identify**: Is it DP?
2. **Define**: What is the state?
3. **Recurrence**: How to compute current state?
4. **Base Cases**: What are the smallest subproblems?
5. **Order**: In what order to fill table?
6. **Implement**: Memoization or tabulation?
7. **Optimize**: Can reduce space?

---

**Keep this cheat sheet handy during practice and interviews!**
