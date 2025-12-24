# Rock Collection Path (Maximum Path Sum in Grid)

## Problem Statement

You are planning a cross-country road trip from Southern California to New York to collect rare rocks. You have a grid representing the number of rare rocks available in various cities. Find the optimal path that maximizes the total rocks collected.

**Constraints:**
- Start: Bottom-left corner (Southern California)
- End: Top-right corner (New York)
- Movement: Only **North (up)** or **East (right)**

**Input:** 2D grid of integers (rock counts)  
**Output:** Maximum rocks collectible

**Example:**
```
Grid:
{{0, 0, 0, 0, 5},  ← New York (finish)
 {0, 1, 1, 1, 0},
 {2, 0, 0, 0, 0}}  ← So_Cal (start)

Path: 2 → 0 → 1 → 1 → 1 → 0 → 5
Total: 10
```

---

## Solution Approaches

### Approach 1: Dynamic Programming (Bottom-Up)

**Time Complexity:** O(m × n)  
**Space Complexity:** O(m × n)

```java
public static Integer optimalPath(Integer[][] grid) {
    int rows = grid.length;
    int cols = grid[0].length;
    
    Integer[][] dp = new Integer[rows][cols];
    dp[rows - 1][0] = grid[rows - 1][0]; // Start position
    
    // Fill first column (can only move up)
    for (int i = rows - 2; i >= 0; i--) {
        dp[i][0] = dp[i + 1][0] + grid[i][0];
    }
    
    // Fill first row (can only move right)
    for (int j = 1; j < cols; j++) {
        dp[rows - 1][j] = dp[rows - 1][j - 1] + grid[rows - 1][j];
    }
    
    // Fill rest of grid
    for (int i = rows - 2; i >= 0; i--) {
        for (int j = 1; j < cols; j++) {
            dp[i][j] = grid[i][j] + Math.max(dp[i + 1][j], dp[i][j - 1]);
        }
    }
    
    return dp[0][cols - 1];
}
```

---

### Approach 2: Space Optimized DP

**Time Complexity:** O(m × n)  
**Space Complexity:** O(n)

```java
public static Integer optimalPathOptimized(Integer[][] grid) {
    int rows = grid.length;
    int cols = grid[0].length;
    
    Integer[] dp = new Integer[cols];
    dp[0] = grid[rows - 1][0];
    
    // Initialize first row
    for (int j = 1; j < cols; j++) {
        dp[j] = dp[j - 1] + grid[rows - 1][j];
    }
    
    // Process remaining rows
    for (int i = rows - 2; i >= 0; i--) {
        dp[0] += grid[i][0]; // First column
        
        for (int j = 1; j < cols; j++) {
            dp[j] = grid[i][j] + Math.max(dp[j], dp[j - 1]);
        }
    }
    
    return dp[cols - 1];
}
```

---

### Approach 3: In-Place Modification

**Time Complexity:** O(m × n)  
**Space Complexity:** O(1)

```java
public static Integer optimalPathInPlace(Integer[][] grid) {
    int rows = grid.length;
    int cols = grid[0].length;
    
    // Fill first column
    for (int i = rows - 2; i >= 0; i--) {
        grid[i][0] += grid[i + 1][0];
    }
    
    // Fill first row
    for (int j = 1; j < cols; j++) {
        grid[rows - 1][j] += grid[rows - 1][j - 1];
    }
    
    // Fill rest
    for (int i = rows - 2; i >= 0; i--) {
        for (int j = 1; j < cols; j++) {
            grid[i][j] += Math.max(grid[i + 1][j], grid[i][j - 1]);
        }
    }
    
    return grid[0][cols - 1];
}
```

---

## Algorithm Walkthrough

### Example: Rock Collection Grid

```
Initial Grid:
Row 0: [0, 0, 0, 0, 5]  ← Finish
Row 1: [0, 1, 1, 1, 0]
Row 2: [2, 0, 0, 0, 0]  ← Start

Step 1: Fill first column (moving up from start)
[0, 0, 0, 0, 5]
[0, 1, 1, 1, 0]
[2, 0, 0, 0, 0]
 ↓
[2, 0, 0, 0, 5]  (2 + 0 = 2)
[2, 1, 1, 1, 0]  (2 + 0 = 2)
[2, 0, 0, 0, 0]

Step 2: Fill first row (moving right from start)
[2, 0, 0, 0, 5]
[2, 1, 1, 1, 0]
[2, 2, 2, 2, 2]  (2→2→2→2→2)

Step 3: Fill remaining cells
For each cell: current + max(below, left)

Row 1, Col 1: 1 + max(2, 2) = 3
Row 1, Col 2: 1 + max(2, 3) = 4
Row 1, Col 3: 1 + max(2, 4) = 5
Row 1, Col 4: 0 + max(2, 5) = 5

Row 0, Col 1: 0 + max(3, 2) = 3
Row 0, Col 2: 0 + max(4, 3) = 4
Row 0, Col 3: 0 + max(5, 4) = 5
Row 0, Col 4: 5 + max(5, 5) = 10

Final DP Grid:
[2, 3, 4, 5, 10]
[2, 3, 4, 5, 5]
[2, 2, 2, 2, 2]

Result: 10
```

**Optimal Path:**
```
Start (2,0): 2
→ (2,1): 0
↑ (1,1): 1
→ (1,2): 1
→ (1,3): 1
→ (1,4): 0
↑ (0,4): 5

Total: 2 + 0 + 1 + 1 + 1 + 0 + 5 = 10
```

---

## Complete Implementation

```java
import java.io.*;
import java.util.*;

class Solution {
    
    // Approach 1: Standard DP
    public static Integer optimalPath(Integer[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        
        Integer[][] dp = new Integer[rows][cols];
        dp[rows - 1][0] = grid[rows - 1][0];
        
        // Fill first column
        for (int i = rows - 2; i >= 0; i--) {
            dp[i][0] = dp[i + 1][0] + grid[i][0];
        }
        
        // Fill first row
        for (int j = 1; j < cols; j++) {
            dp[rows - 1][j] = dp[rows - 1][j - 1] + grid[rows - 1][j];
        }
        
        // Fill rest
        for (int i = rows - 2; i >= 0; i--) {
            for (int j = 1; j < cols; j++) {
                dp[i][j] = grid[i][j] + Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }
        
        return dp[0][cols - 1];
    }
    
    // Approach 2: Space optimized
    public static Integer optimalPathOptimized(Integer[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        
        Integer[] dp = new Integer[cols];
        dp[0] = grid[rows - 1][0];
        
        for (int j = 1; j < cols; j++) {
            dp[j] = dp[j - 1] + grid[rows - 1][j];
        }
        
        for (int i = rows - 2; i >= 0; i--) {
            dp[0] += grid[i][0];
            
            for (int j = 1; j < cols; j++) {
                dp[j] = grid[i][j] + Math.max(dp[j], dp[j - 1]);
            }
        }
        
        return dp[cols - 1];
    }
    
    // Approach 3: With path reconstruction
    public static class PathResult {
        int maxRocks;
        List<String> path;
        
        PathResult(int maxRocks, List<String> path) {
            this.maxRocks = maxRocks;
            this.path = path;
        }
    }
    
    public static PathResult optimalPathWithRoute(Integer[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        
        Integer[][] dp = new Integer[rows][cols];
        dp[rows - 1][0] = grid[rows - 1][0];
        
        // Fill DP table
        for (int i = rows - 2; i >= 0; i--) {
            dp[i][0] = dp[i + 1][0] + grid[i][0];
        }
        
        for (int j = 1; j < cols; j++) {
            dp[rows - 1][j] = dp[rows - 1][j - 1] + grid[rows - 1][j];
        }
        
        for (int i = rows - 2; i >= 0; i--) {
            for (int j = 1; j < cols; j++) {
                dp[i][j] = grid[i][j] + Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }
        
        // Reconstruct path
        List<String> path = new ArrayList<>();
        int i = rows - 1, j = 0;
        path.add("(" + i + "," + j + "):" + grid[i][j]);
        
        while (i > 0 || j < cols - 1) {
            if (i == 0) {
                j++;
            } else if (j == cols - 1) {
                i--;
            } else if (dp[i - 1][j] > dp[i][j + 1]) {
                i--;
            } else {
                j++;
            }
            path.add("(" + i + "," + j + "):" + grid[i][j]);
        }
        
        return new PathResult(dp[0][cols - 1], path);
    }
    
    public static boolean doTestsPass() {
        boolean result = true;
        
        result &= optimalPath(new Integer[][]{
            {0, 0, 0, 0, 5},
            {0, 1, 1, 1, 0},
            {2, 0, 0, 0, 0}
        }) == 10;
        
        result &= optimalPath(new Integer[][]{
            {1, 2, 3},
            {4, 5, 6}
        }) == 18;
        
        result &= optimalPath(new Integer[][]{
            {5}
        }) == 5;
        
        return result;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail.");
        }
        
        // Demo with path
        Integer[][] grid = {
            {0, 0, 0, 0, 5},
            {0, 1, 1, 1, 0},
            {2, 0, 0, 0, 0}
        };
        
        PathResult result = optimalPathWithRoute(grid);
        System.out.println("Max rocks: " + result.maxRocks);
        System.out.println("Path: " + result.path);
    }
}
```

---

## Test Cases

```java
@Test
public void testOptimalPath() {
    // Test case 1: Given example
    assertEquals(10, optimalPath(new Integer[][]{
        {0, 0, 0, 0, 5},
        {0, 1, 1, 1, 0},
        {2, 0, 0, 0, 0}
    }));
    
    // Test case 2: Simple grid
    assertEquals(18, optimalPath(new Integer[][]{
        {1, 2, 3},
        {4, 5, 6}
    }));
    
    // Test case 3: Single cell
    assertEquals(5, optimalPath(new Integer[][]{{5}}));
    
    // Test case 4: All zeros
    assertEquals(0, optimalPath(new Integer[][]{
        {0, 0, 0},
        {0, 0, 0}
    }));
    
    // Test case 5: Single row
    assertEquals(15, optimalPath(new Integer[][]{{1, 2, 3, 4, 5}}));
    
    // Test case 6: Single column
    assertEquals(15, optimalPath(new Integer[][]{{1}, {2}, {3}, {4}, {5}}));
    
    // Test case 7: Large values
    assertEquals(1000, optimalPath(new Integer[][]{
        {100, 200, 300},
        {400, 0, 0}
    }));
}
```

---

## Visual Representation

### DP Table Evolution

```
Initial Grid:
[0, 0, 0, 0, 5]
[0, 1, 1, 1, 0]
[2, 0, 0, 0, 0]

After filling first column:
[2, ?, ?, ?, ?]
[2, ?, ?, ?, ?]
[2, ?, ?, ?, ?]

After filling first row:
[2, ?, ?, ?, ?]
[2, ?, ?, ?, ?]
[2, 2, 2, 2, 2]

After filling all cells:
[2, 3, 4, 5, 10]
[2, 3, 4, 5, 5]
[2, 2, 2, 2, 2]

Path visualization:
S = Start, E = End, * = Path

[*, *, *, *, E]
[0, *, *, *, *]
[S, *, 0, 0, 0]
```

---

## Edge Cases

1. **Single cell grid:** `[[5]]` → 5
2. **Single row:** `[[1, 2, 3]]` → 6
3. **Single column:** `[[1], [2], [3]]` → 6
4. **All zeros:** `[[0, 0], [0, 0]]` → 0
5. **Large grid:** 100×100 grid
6. **Negative values:** Not applicable (rocks ≥ 0)
7. **Maximum path on edge:** Path along border

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Standard DP | O(m×n) | O(m×n) | Clear, easy to debug |
| Space Optimized | O(m×n) | O(n) | Better space efficiency |
| In-Place | O(m×n) | O(1) | Modifies input |

**Where:**
- m = number of rows
- n = number of columns

**Why O(m×n)?**
- Visit each cell exactly once
- Constant work per cell

---

## Related Problems

1. **Minimum Path Sum** - Find path with minimum sum
2. **Unique Paths** - Count all possible paths
3. **Unique Paths II** - With obstacles
4. **Dungeon Game** - Minimum health needed
5. **Cherry Pickup** - Two-pass path optimization
6. **Triangle** - Path sum in triangular grid

---

## Interview Tips

### Clarification Questions
1. Can we move diagonally? (No, only up/right)
2. Can rock counts be negative? (No, ≥ 0)
3. Is the grid always rectangular? (Yes)
4. Do we need the actual path or just the sum? (Just sum)
5. Can we modify the input grid? (Depends on requirements)

### Approach Explanation
1. "This is a classic DP problem - optimal substructure"
2. "Each cell depends on max of (below, left)"
3. "Start from bottom-left, end at top-right"
4. "Can optimize space from O(m×n) to O(n)"

### Common Mistakes
- Wrong iteration direction (should be bottom-up, left-right)
- Forgetting to initialize first row/column
- Off-by-one errors in indices
- Not handling single row/column cases

---

## Real-World Applications

1. **Route Optimization** - Delivery trucks maximizing pickups
2. **Game Development** - Pathfinding with resource collection
3. **Network Routing** - Maximizing bandwidth utilization
4. **Financial Planning** - Investment path optimization
5. **Supply Chain** - Warehouse to destination with pickups

---

## Key Takeaways

1. **DP Pattern:** Grid DP with restricted movement
2. **Recurrence:** `dp[i][j] = grid[i][j] + max(dp[i+1][j], dp[i][j-1])`
3. **Initialization:** Handle first row and column separately
4. **Space Optimization:** Can reduce from O(m×n) to O(n)
5. **Path Reconstruction:** Backtrack from end to start using DP table
6. **Time Complexity:** Always O(m×n) - must visit all cells
7. **Similar Pattern:** Minimum path sum, unique paths, triangle

---

## Additional Notes

**Why Bottom-Up?**
- Start position is bottom-left
- Natural to build solution from start to end
- Easier to reconstruct path

**Alternative: Top-Down (Memoization)**
```java
public static Integer optimalPathMemo(Integer[][] grid, int i, int j, Integer[][] memo) {
    if (i == grid.length - 1 && j == 0) return grid[i][j];
    if (i >= grid.length || j < 0) return Integer.MIN_VALUE;
    if (memo[i][j] != null) return memo[i][j];
    
    int down = optimalPathMemo(grid, i + 1, j, memo);
    int left = optimalPathMemo(grid, i, j - 1, memo);
    
    memo[i][j] = grid[i][j] + Math.max(down, left);
    return memo[i][j];
}
```

**Optimization Tips:**
- Use 1D array for space optimization
- In-place modification if input can be changed
- Early termination if all remaining cells are 0
