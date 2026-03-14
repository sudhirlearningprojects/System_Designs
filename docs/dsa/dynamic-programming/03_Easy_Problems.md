# Dynamic Programming - Easy Problems (40%)

## 📚 8 Easy Problems with Complete Solutions

---

## Problem 1: Climbing Stairs

**Difficulty**: Easy  
**Pattern**: Linear DP (1D)  
**LeetCode**: #70

### Problem Statement

You are climbing a staircase with `n` steps. Each time you can climb 1 or 2 steps. In how many distinct ways can you climb to the top?

### Examples

```
Input: n = 2
Output: 2
Explanation: 1+1, 2

Input: n = 3
Output: 3
Explanation: 1+1+1, 1+2, 2+1
```

### Solution

```java
public class ClimbingStairs {
    // Approach 1: Tabulation
    public int climbStairs(int n) {
        if (n <= 2) return n;
        
        int[] dp = new int[n + 1];
        dp[1] = 1;
        dp[2] = 2;
        
        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i-1] + dp[i-2];
        }
        
        return dp[n];
    }
    
    // Approach 2: Space Optimized
    public int climbStairsOptimized(int n) {
        if (n <= 2) return n;
        
        int prev2 = 1, prev1 = 2;
        
        for (int i = 3; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### Dry Run

**Input**: `n = 5`

```
dp[1] = 1  (one way: 1)
dp[2] = 2  (two ways: 1+1, 2)

dp[3] = dp[2] + dp[1] = 2 + 1 = 3
  Ways: 1+1+1, 1+2, 2+1

dp[4] = dp[3] + dp[2] = 3 + 2 = 5
  Ways: 1+1+1+1, 1+1+2, 1+2+1, 2+1+1, 2+2

dp[5] = dp[4] + dp[3] = 5 + 3 = 8
  Ways: 1+1+1+1+1, 1+1+1+2, 1+1+2+1, 1+2+1+1, 2+1+1+1,
        1+2+2, 2+1+2, 2+2+1

Result: 8
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(n) for tabulation, O(1) for optimized

### Test Cases

```java
@Test
public void testClimbStairs() {
    ClimbingStairs solution = new ClimbingStairs();
    
    assertEquals(2, solution.climbStairs(2));
    assertEquals(3, solution.climbStairs(3));
    assertEquals(5, solution.climbStairs(4));
    assertEquals(8, solution.climbStairs(5));
}
```

---

## Problem 2: Fibonacci Number

**Difficulty**: Easy  
**Pattern**: Linear DP (1D)  
**LeetCode**: #509

### Problem Statement

The Fibonacci numbers form a sequence where each number is the sum of the two preceding ones, starting from 0 and 1.

### Solution

```java
public class FibonacciNumber {
    public int fib(int n) {
        if (n <= 1) return n;
        
        int prev2 = 0, prev1 = 1;
        
        for (int i = 2; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### Test Cases

```java
@Test
public void testFib() {
    FibonacciNumber solution = new FibonacciNumber();
    
    assertEquals(0, solution.fib(0));
    assertEquals(1, solution.fib(1));
    assertEquals(1, solution.fib(2));
    assertEquals(2, solution.fib(3));
    assertEquals(3, solution.fib(4));
    assertEquals(5, solution.fib(5));
}
```

---

## Problem 3: Min Cost Climbing Stairs

**Difficulty**: Easy  
**Pattern**: Linear DP (1D)  
**LeetCode**: #746

### Problem Statement

You are given an integer array `cost` where `cost[i]` is the cost of `i-th` step. Once you pay the cost, you can climb one or two steps. You can start from step 0 or step 1. Return the minimum cost to reach the top.

### Examples

```
Input: cost = [10,15,20]
Output: 15
Explanation: Start at index 1, pay 15, climb two steps to reach top

Input: cost = [1,100,1,1,1,100,1,1,100,1]
Output: 6
Explanation: Start at index 0, pay 1, climb two steps to index 2, pay 1, 
climb two steps to index 4, pay 1, climb two steps to index 6, pay 1, 
climb one step to index 7, pay 1, climb two steps to index 9, pay 1, 
climb one step to reach top
```

### Solution

```java
public class MinCostClimbingStairs {
    public int minCostClimbingStairs(int[] cost) {
        int n = cost.length;
        
        int prev2 = 0, prev1 = 0;
        
        for (int i = 2; i <= n; i++) {
            int curr = Math.min(
                prev1 + cost[i-1],
                prev2 + cost[i-2]
            );
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

---

## Problem 4: House Robber

**Difficulty**: Easy/Medium  
**Pattern**: Linear DP (1D)  
**LeetCode**: #198

### Problem Statement

You are a robber planning to rob houses along a street. Each house has a certain amount of money. Adjacent houses have security systems connected, so you cannot rob two adjacent houses. Return the maximum amount you can rob.

### Examples

```
Input: nums = [1,2,3,1]
Output: 4
Explanation: Rob house 1 (money = 1) and house 3 (money = 3). Total = 4

Input: nums = [2,7,9,3,1]
Output: 12
Explanation: Rob house 1 (money = 2), house 3 (money = 9), house 5 (money = 1). Total = 12
```

### Solution

```java
public class HouseRobber {
    public int rob(int[] nums) {
        if (nums.length == 0) return 0;
        if (nums.length == 1) return nums[0];
        
        int prev2 = 0, prev1 = 0;
        
        for (int num : nums) {
            int curr = Math.max(prev1, prev2 + num);
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### Dry Run

**Input**: `nums = [2, 7, 9, 3, 1]`

```
Initial: prev2 = 0, prev1 = 0

i = 0, num = 2:
  curr = max(0, 0 + 2) = 2
  prev2 = 0, prev1 = 2

i = 1, num = 7:
  curr = max(2, 0 + 7) = 7
  prev2 = 2, prev1 = 7

i = 2, num = 9:
  curr = max(7, 2 + 9) = 11
  prev2 = 7, prev1 = 11

i = 3, num = 3:
  curr = max(11, 7 + 3) = 11
  prev2 = 11, prev1 = 11

i = 4, num = 1:
  curr = max(11, 11 + 1) = 12
  prev2 = 11, prev1 = 12

Result: 12
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

---

## Problem 5: Maximum Subarray (Kadane's Algorithm)

**Difficulty**: Easy/Medium  
**Pattern**: Linear DP (1D)  
**LeetCode**: #53

### Problem Statement

Given an integer array `nums`, find the contiguous subarray with the largest sum and return its sum.

### Examples

```
Input: nums = [-2,1,-3,4,-1,2,1,-5,4]
Output: 6
Explanation: [4,-1,2,1] has the largest sum = 6

Input: nums = [1]
Output: 1

Input: nums = [5,4,-1,7,8]
Output: 23
```

### Solution

```java
public class MaximumSubarray {
    public int maxSubArray(int[] nums) {
        int maxSoFar = nums[0];
        int maxEndingHere = nums[0];
        
        for (int i = 1; i < nums.length; i++) {
            maxEndingHere = Math.max(nums[i], maxEndingHere + nums[i]);
            maxSoFar = Math.max(maxSoFar, maxEndingHere);
        }
        
        return maxSoFar;
    }
}
```

### Dry Run

**Input**: `nums = [-2, 1, -3, 4, -1, 2, 1, -5, 4]`

```
i = 0: maxEndingHere = -2, maxSoFar = -2

i = 1: maxEndingHere = max(1, -2+1) = 1, maxSoFar = 1

i = 2: maxEndingHere = max(-3, 1-3) = -2, maxSoFar = 1

i = 3: maxEndingHere = max(4, -2+4) = 4, maxSoFar = 4

i = 4: maxEndingHere = max(-1, 4-1) = 3, maxSoFar = 4

i = 5: maxEndingHere = max(2, 3+2) = 5, maxSoFar = 5

i = 6: maxEndingHere = max(1, 5+1) = 6, maxSoFar = 6

i = 7: maxEndingHere = max(-5, 6-5) = 1, maxSoFar = 6

i = 8: maxEndingHere = max(4, 1+4) = 5, maxSoFar = 6

Result: 6
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

---

## Problem 6: Best Time to Buy and Sell Stock

**Difficulty**: Easy  
**Pattern**: State Machine DP  
**LeetCode**: #121

### Problem Statement

You are given an array `prices` where `prices[i]` is the price of a stock on the `i-th` day. You want to maximize profit by choosing a single day to buy and a different day in the future to sell. Return the maximum profit.

### Solution

```java
public class BestTimeToBuyAndSellStock {
    public int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
        
        return maxProfit;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

---

## Problem 7: Divisor Game

**Difficulty**: Easy  
**Pattern**: Linear DP (1D)  
**LeetCode**: #1025

### Problem Statement

Alice and Bob play a game with a number `n`. On each turn, a player chooses any `x` with `0 < x < n` and `n % x == 0`, then replaces `n` with `n - x`. The player who cannot make a move loses. Return `true` if Alice wins assuming both play optimally.

### Solution

```java
public class DivisorGame {
    // Mathematical solution
    public boolean divisorGame(int n) {
        return n % 2 == 0;
    }
    
    // DP solution
    public boolean divisorGameDP(int n) {
        boolean[] dp = new boolean[n + 1];
        
        for (int i = 2; i <= n; i++) {
            for (int x = 1; x < i; x++) {
                if (i % x == 0 && !dp[i - x]) {
                    dp[i] = true;
                    break;
                }
            }
        }
        
        return dp[n];
    }
}
```

---

## Problem 8: N-th Tribonacci Number

**Difficulty**: Easy  
**Pattern**: Linear DP (1D)  
**LeetCode**: #1137

### Problem Statement

The Tribonacci sequence is defined as: T₀ = 0, T₁ = 1, T₂ = 1, and Tₙ₊₃ = Tₙ + Tₙ₊₁ + Tₙ₊₂ for n >= 0. Given `n`, return the value of Tₙ.

### Solution

```java
public class Tribonacci {
    public int tribonacci(int n) {
        if (n == 0) return 0;
        if (n <= 2) return 1;
        
        int t0 = 0, t1 = 1, t2 = 1;
        
        for (int i = 3; i <= n; i++) {
            int curr = t0 + t1 + t2;
            t0 = t1;
            t1 = t2;
            t2 = curr;
        }
        
        return t2;
    }
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Climbing Stairs | Linear 1D | O(n) | O(1) | Fibonacci-like |
| Fibonacci | Linear 1D | O(n) | O(1) | Classic DP |
| Min Cost Stairs | Linear 1D | O(n) | O(1) | Choose min path |
| House Robber | Linear 1D | O(n) | O(1) | Skip adjacent |
| Maximum Subarray | Linear 1D | O(n) | O(1) | Kadane's algorithm |
| Buy Sell Stock | State Machine | O(n) | O(1) | Track min price |
| Divisor Game | Linear 1D | O(n²) | O(n) | Game theory |
| Tribonacci | Linear 1D | O(n) | O(1) | Three previous |

---

**Next**: [Medium Problems](04_Medium_Problems.md)
