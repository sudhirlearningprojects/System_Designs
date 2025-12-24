# Climbing Stairs (1, 2, or 3 Steps)

## Problem Statement

A child wants to climb a staircase with `n` steps. The child can take 1, 2, or 3 steps at a time. Find the number of unique combinations to reach the top.

**Input:** Integer n (number of steps)  
**Output:** Number of unique combinations

**Examples:**
```
n = 3:
  1+1+1
  1+2
  2+1
  3
  Result: 4

n = 4:
  1+1+1+1
  1+1+2
  1+2+1
  2+1+1
  2+2
  1+3
  3+1
  Result: 7
```

---

## Solution Approaches

### Approach 1: Dynamic Programming (Bottom-Up)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer countSteps(Integer n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;
    if (n == 2) return 2;
    if (n == 3) return 4;
    
    Integer[] dp = new Integer[n + 1];
    dp[0] = 1;
    dp[1] = 1;
    dp[2] = 2;
    dp[3] = 4;
    
    for (int i = 4; i <= n; i++) {
        dp[i] = dp[i - 1] + dp[i - 2] + dp[i - 3];
    }
    
    return dp[n];
}
```

---

### Approach 2: Space Optimized DP

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static Integer countStepsOptimized(Integer n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;
    if (n == 2) return 2;
    if (n == 3) return 4;
    
    int a = 1, b = 2, c = 4;
    
    for (int i = 4; i <= n; i++) {
        int temp = a + b + c;
        a = b;
        b = c;
        c = temp;
    }
    
    return c;
}
```

---

### Approach 3: Recursion with Memoization

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer countStepsMemo(Integer n) {
    Map<Integer, Integer> memo = new HashMap<>();
    return helper(n, memo);
}

private static Integer helper(Integer n, Map<Integer, Integer> memo) {
    if (n <= 0) return 0;
    if (n == 1) return 1;
    if (n == 2) return 2;
    if (n == 3) return 4;
    
    if (memo.containsKey(n)) return memo.get(n);
    
    int result = helper(n - 1, memo) + helper(n - 2, memo) + helper(n - 3, memo);
    memo.put(n, result);
    
    return result;
}
```

---

## Algorithm Walkthrough

### Example: n = 5

**Recurrence Relation:**
```
dp[i] = dp[i-1] + dp[i-2] + dp[i-3]
```

**Base Cases:**
```
dp[0] = 1  (one way to stay at ground)
dp[1] = 1  (1)
dp[2] = 2  (1+1, 2)
dp[3] = 4  (1+1+1, 1+2, 2+1, 3)
```

**Building DP Table:**
```
dp[4] = dp[3] + dp[2] + dp[1]
      = 4 + 2 + 1
      = 7

dp[5] = dp[4] + dp[3] + dp[2]
      = 7 + 4 + 2
      = 13

Result: 13
```

**All Combinations for n=5:**
```
1. 1+1+1+1+1
2. 1+1+1+2
3. 1+1+2+1
4. 1+2+1+1
5. 2+1+1+1
6. 1+2+2
7. 2+1+2
8. 2+2+1
9. 1+1+3
10. 1+3+1
11. 3+1+1
12. 2+3
13. 3+2

Total: 13 combinations
```

---

## Complete Implementation

```java
import java.io.*;
import java.util.*;

class Solution {
    
    // Approach 1: DP with array
    public static Integer countSteps(Integer n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        if (n == 2) return 2;
        if (n == 3) return 4;
        
        Integer[] dp = new Integer[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        dp[2] = 2;
        dp[3] = 4;
        
        for (int i = 4; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2] + dp[i - 3];
        }
        
        return dp[n];
    }
    
    // Approach 2: Space optimized
    public static Integer countStepsOptimized(Integer n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        if (n == 2) return 2;
        if (n == 3) return 4;
        
        int a = 1, b = 2, c = 4;
        
        for (int i = 4; i <= n; i++) {
            int temp = a + b + c;
            a = b;
            b = c;
            c = temp;
        }
        
        return c;
    }
    
    // Approach 3: Memoization
    public static Integer countStepsMemo(Integer n) {
        Map<Integer, Integer> memo = new HashMap<>();
        return helper(n, memo);
    }
    
    private static Integer helper(Integer n, Map<Integer, Integer> memo) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        if (n == 2) return 2;
        if (n == 3) return 4;
        
        if (memo.containsKey(n)) return memo.get(n);
        
        int result = helper(n - 1, memo) + helper(n - 2, memo) + helper(n - 3, memo);
        memo.put(n, result);
        
        return result;
    }
    
    // Approach 4: Pure recursion (for comparison - exponential time)
    public static Integer countStepsRecursive(Integer n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        if (n == 2) return 2;
        if (n == 3) return 4;
        
        return countStepsRecursive(n - 1) + 
               countStepsRecursive(n - 2) + 
               countStepsRecursive(n - 3);
    }
    
    public static boolean doTestsPass() {
        return countSteps(3) == 4
            && countSteps(4) == 7
            && countSteps(5) == 13
            && countSteps(1) == 1
            && countSteps(2) == 2;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail.");
        }
        
        for (Integer n = 1; n <= 10; n++) {
            Integer numberOfCombinations = countSteps(n);
            System.out.println(n + " steps => " + numberOfCombinations);
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testCountSteps() {
    // Base cases
    assertEquals(1, countSteps(1));
    assertEquals(2, countSteps(2));
    assertEquals(4, countSteps(3));
    
    // Given test cases
    assertEquals(7, countSteps(4));
    assertEquals(13, countSteps(5));
    
    // Additional cases
    assertEquals(24, countSteps(6));
    assertEquals(44, countSteps(7));
    assertEquals(81, countSteps(8));
    
    // Edge cases
    assertEquals(0, countSteps(0));
    assertEquals(0, countSteps(-1));
    
    // Large value
    assertEquals(1389537, countSteps(20));
}
```

---

## Visual Representation

### Decision Tree for n=4

```
                    Start (4 steps remaining)
                   /        |         \
                  /         |          \
            Take 1      Take 2      Take 3
               |           |            |
            (3 left)    (2 left)    (1 left)
           /   |   \      /  \          |
          1    2   3     1    2         1
         /|\  /|   |     |    |         |
        1 2 3 1 2  1     1    -        Done
       /|\ |  | |  |     |
      1 2 3 1 1 1  -    Done
      | | | | | |
      - - - - - -

Paths:
1. 1+1+1+1
2. 1+1+2
3. 1+2+1
4. 2+1+1
5. 1+3
6. 2+2
7. 3+1

Total: 7 ways
```

### DP Table Growth

```
n:    0  1  2  3  4   5   6   7   8   9   10
dp:   1  1  2  4  7  13  24  44  81 149  274

Pattern: Each value = sum of previous 3 values
```

---

## Edge Cases

1. **Zero steps:** n=0 → 0 (or 1 if considering "stay at ground")
2. **Negative steps:** n<0 → 0
3. **Single step:** n=1 → 1
4. **Two steps:** n=2 → 2
5. **Three steps:** n=3 → 4
6. **Large n:** n=20 → 1,389,537
7. **Overflow:** For very large n, use BigInteger

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Pure Recursion | O(3^n) | O(n) | Exponential, not practical |
| Memoization | O(n) | O(n) | Top-down DP |
| DP Array | O(n) | O(n) | Bottom-up, clear |
| Space Optimized | O(n) | O(1) | Optimal solution |

**Why O(n)?**
- Must compute values for all steps from 1 to n
- Each computation is O(1)

**Recurrence:**
```
T(n) = T(n-1) + T(n-2) + T(n-3) + O(1)
```

---

## Related Problems

1. **Climbing Stairs (1 or 2 steps)** - Classic Fibonacci
2. **Min Cost Climbing Stairs** - With cost array
3. **Climbing Stairs with Variable Steps** - Array of allowed steps
4. **Decode Ways** - Similar DP pattern
5. **House Robber** - Similar recurrence relation
6. **Tribonacci Number** - Exact same recurrence

---

## Interview Tips

### Clarification Questions
1. Can n be zero or negative? (Return 0)
2. What's the maximum value of n? (Affects overflow)
3. Can we take 0 steps? (No, must take 1, 2, or 3)
4. Do we count order? (Yes, [1,2] ≠ [2,1])
5. Any constraints on time/space? (Prefer O(1) space)

### Approach Explanation
1. "This is a DP problem with recurrence relation"
2. "Each step depends on previous 3 steps"
3. "Base cases: n=1→1, n=2→2, n=3→4"
4. "Can optimize space from O(n) to O(1)"

### Common Mistakes
- Wrong base cases (especially n=3)
- Not handling n≤0
- Forgetting that order matters
- Integer overflow for large n
- Off-by-one errors in loop

### Optimization Path
1. Start with recursion to understand pattern
2. Add memoization to avoid recomputation
3. Convert to bottom-up DP
4. Optimize space to O(1)

---

## Real-World Applications

1. **Pathfinding** - Number of ways to reach destination
2. **Game Development** - Character movement combinations
3. **Network Routing** - Path diversity calculation
4. **Combinatorics** - Counting sequences
5. **Resource Allocation** - Distribution strategies

---

## Key Takeaways

1. **Recurrence:** `dp[i] = dp[i-1] + dp[i-2] + dp[i-3]`
2. **Base Cases:** dp[1]=1, dp[2]=2, dp[3]=4
3. **Pattern:** Tribonacci-like sequence
4. **Optimization:** Can reduce space to O(1) with 3 variables
5. **Time Complexity:** Always O(n) for DP solutions
6. **Similar to Fibonacci:** But sum of 3 previous values
7. **Order Matters:** [1,2] and [2,1] are different combinations

---

## Additional Notes

### Generalization to K Steps

For climbing with 1 to k steps:

```java
public static Integer countStepsK(Integer n, Integer k) {
    if (n <= 0) return 0;
    
    Integer[] dp = new Integer[n + 1];
    dp[0] = 1;
    
    for (int i = 1; i <= n; i++) {
        dp[i] = 0;
        for (int j = 1; j <= k && j <= i; j++) {
            dp[i] += dp[i - j];
        }
    }
    
    return dp[n];
}
```

### Sequence Pattern

```
n:  1  2  3   4   5   6   7   8    9    10
f:  1  2  4   7  13  24  44  81  149   274

Ratio: f(n)/f(n-1) approaches ~1.839...
(Tribonacci constant)
```

### Matrix Exponentiation (Advanced)

For very large n, can use matrix exponentiation in O(log n):

```
[f(n)  ]   [1 1 1]^n   [f(3)]
[f(n-1)] = [1 0 0]   * [f(2)]
[f(n-2)]   [0 1 0]     [f(1)]
```

### Comparison with Fibonacci

```
Fibonacci (1 or 2 steps):
  f(n) = f(n-1) + f(n-2)
  1, 1, 2, 3, 5, 8, 13, 21...

Tribonacci (1, 2, or 3 steps):
  f(n) = f(n-1) + f(n-2) + f(n-3)
  1, 1, 2, 4, 7, 13, 24, 44...
```

### Why Base Cases Matter

```
Wrong: dp[0]=0, dp[1]=1, dp[2]=2, dp[3]=3
  dp[4] = 3+2+1 = 6 ✗ (should be 7)

Correct: dp[0]=1, dp[1]=1, dp[2]=2, dp[3]=4
  dp[4] = 4+2+1 = 7 ✓
```

The key insight: dp[0]=1 represents "one way to take zero steps" (do nothing), which is needed for the recurrence to work correctly.
