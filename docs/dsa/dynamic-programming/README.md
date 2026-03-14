# Dynamic Programming - Complete Guide

## 📚 Documentation Structure

This comprehensive guide covers Dynamic Programming (DP) techniques with theory, examples, and 20+ problems.

### Documents

1. **[01_Theory_and_Concepts.md](01_Theory_and_Concepts.md)** - Deep dive into DP theory
2. **[02_Pattern_Recognition.md](02_Pattern_Recognition.md)** - How to identify DP problems
3. **[03_Easy_Problems.md](03_Easy_Problems.md)** - 8 Easy problems with solutions (40%)
4. **[04_Medium_Problems.md](04_Medium_Problems.md)** - 10 Medium problems with solutions (50%)
5. **[05_Hard_Problems.md](05_Hard_Problems.md)** - 2 Hard problems with solutions (10%)
6. **[Quick_Reference.md](Quick_Reference.md)** - Cheat sheet for quick review

## 🎯 Quick Overview

**Dynamic Programming (DP)** is an algorithmic technique that solves complex problems by breaking them down into simpler subproblems and storing their solutions to avoid redundant calculations.

### Key Benefits
- **Optimization**: Reduces exponential time to polynomial
- **Efficiency**: Avoids redundant calculations through memoization
- **Versatile**: Solves optimization, counting, and decision problems
- **Powerful**: Handles overlapping subproblems elegantly

### Core Principles
1. **Overlapping Subproblems** - Same subproblems solved multiple times
2. **Optimal Substructure** - Optimal solution contains optimal solutions to subproblems
3. **Memoization** - Top-down approach with caching
4. **Tabulation** - Bottom-up approach with table

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
1. Climbing Stairs
2. Fibonacci Number
3. Min Cost Climbing Stairs
4. House Robber
5. Maximum Subarray (Kadane's Algorithm)
6. Best Time to Buy and Sell Stock
7. Divisor Game
8. N-th Tribonacci Number

### Medium Problems (10)
1. Longest Increasing Subsequence
2. Coin Change
3. Longest Common Subsequence
4. Unique Paths
5. House Robber II
6. Decode Ways
7. Word Break
8. Partition Equal Subset Sum
9. Longest Palindromic Substring
10. Maximum Product Subarray

### Hard Problems (2)
1. Edit Distance
2. Regular Expression Matching

## 💡 Tips for Success

1. **Identify DP characteristics** - Overlapping subproblems + optimal substructure
2. **Start with recursion** - Write recursive solution first
3. **Add memoization** - Cache results to avoid recomputation
4. **Convert to tabulation** - Bottom-up for better space efficiency
5. **Optimize space** - Often can reduce from O(n²) to O(n) or O(1)

## 🎨 Visual Example: Fibonacci

### Recursive (Exponential Time)
```
fib(5)
├── fib(4)
│   ├── fib(3)
│   │   ├── fib(2)
│   │   │   ├── fib(1) = 1
│   │   │   └── fib(0) = 0
│   │   └── fib(1) = 1
│   └── fib(2)
│       ├── fib(1) = 1
│       └── fib(0) = 0
└── fib(3)
    ├── fib(2)
    │   ├── fib(1) = 1
    │   └── fib(0) = 0
    └── fib(1) = 1

Notice: fib(3), fib(2), fib(1) calculated multiple times!
```

### DP (Linear Time)
```
Memoization: Store results in cache
fib(5) → cache[5] = 5
fib(4) → cache[4] = 3
fib(3) → cache[3] = 2
fib(2) → cache[2] = 1
fib(1) → cache[1] = 1
fib(0) → cache[0] = 0

Each subproblem solved only once!
```

## 🔑 Key Approaches

### 1. Memoization (Top-Down)
- Start with recursive solution
- Add cache to store results
- Check cache before computing

```java
int fib(int n, int[] memo) {
    if (n <= 1) return n;
    if (memo[n] != 0) return memo[n];
    memo[n] = fib(n-1, memo) + fib(n-2, memo);
    return memo[n];
}
```

### 2. Tabulation (Bottom-Up)
- Build table from base cases
- Fill table iteratively
- Return final result

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

### 3. Space Optimization
- Identify dependencies
- Keep only necessary states
- Reduce space complexity

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

## 📈 Common DP Patterns

| Pattern | Example | State Definition |
|---------|---------|------------------|
| Linear DP | Climbing Stairs | dp[i] = ways to reach step i |
| 2D DP | Unique Paths | dp[i][j] = paths to cell (i,j) |
| Subsequence | LCS | dp[i][j] = LCS of s1[0..i] and s2[0..j] |
| Knapsack | Coin Change | dp[i][j] = min coins for amount j using first i coins |
| Interval DP | Palindrome | dp[i][j] = is s[i..j] palindrome |
| State Machine | Stock Trading | dp[i][state] = max profit at day i in state |

## 🎯 When to Use DP

### Strong Indicators
- Problem asks for "maximum", "minimum", "longest", "shortest"
- Count number of ways to do something
- Optimization problem with choices
- Can break into overlapping subproblems
- Optimal solution uses optimal solutions of subproblems

### Problem Keywords
- "Maximum/Minimum"
- "Longest/Shortest"
- "Count ways"
- "Is it possible"
- "Optimize"
- "Best strategy"

## 📊 Complexity Patterns

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Recursion | O(2^n) | O(n) | Exponential, stack space |
| Memoization | O(n) or O(n²) | O(n) or O(n²) | Cache + stack |
| Tabulation | O(n) or O(n²) | O(n) or O(n²) | Table only |
| Space Optimized | O(n) or O(n²) | O(1) or O(n) | Reduced space |

---

**Total Problems**: 20 | **Estimated Study Time**: 25-30 hours
