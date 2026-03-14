# Two Pointers - Hard Problems (10%)

## 📚 2 Hard Problems with Complete Solutions

---

## Problem 1: Trapping Rain Water II (2D)

**Difficulty**: Hard  
**Pattern**: Priority Queue + BFS (Advanced Two Pointers Concept)  
**LeetCode**: #407

### Problem Statement

Given an `m x n` integer matrix `heightMap` representing the height of each unit cell in a 2D elevation map, return the volume of water it can trap after raining.

### Examples

```
Input: heightMap = [[1,4,3,1,3,2],[3,2,1,3,2,4],[2,3,3,2,3,1]]
Output: 4

Input: heightMap = [[3,3,3,3,3],[3,2,2,2,3],[3,2,1,2,3],[3,2,2,2,3],[3,3,3,3,3]]
Output: 10
```

### Intuition

Unlike 1D trapping rain water where we can use two pointers from both ends, in 2D we need to process cells from outside to inside, always processing the lowest boundary first (similar to Dijkstra's algorithm).

**Key Insight**: Water level at any cell is determined by the minimum height of the boundary surrounding it.

### Solution

```java
public class TrappingRainWaterII {
    private static final int[][] DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
    
    static class Cell {
        int row, col, height;
        
        Cell(int row, int col, int height) {
            this.row = row;
            this.col = col;
            this.height = height;
        }
    }
    
    public int trapRainWater(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0) return 0;
        
        int m = heightMap.length;
        int n = heightMap[0].length;
        
        // Priority queue to process cells from lowest to highest
        PriorityQueue<Cell> pq = new PriorityQueue<>((a, b) -> a.height - b.height);
        boolean[][] visited = new boolean[m][n];
        
        // Add all boundary cells to priority queue
        for (int i = 0; i < m; i++) {
            pq.offer(new Cell(i, 0, heightMap[i][0]));
            pq.offer(new Cell(i, n - 1, heightMap[i][n - 1]));
            visited[i][0] = true;
            visited[i][n - 1] = true;
        }
        
        for (int j = 1; j < n - 1; j++) {
            pq.offer(new Cell(0, j, heightMap[0][j]));
            pq.offer(new Cell(m - 1, j, heightMap[m - 1][j]));
            visited[0][j] = true;
            visited[m - 1][j] = true;
        }
        
        int waterTrapped = 0;
        int maxHeight = 0;
        
        // Process cells from outside to inside
        while (!pq.isEmpty()) {
            Cell cell = pq.poll();
            maxHeight = Math.max(maxHeight, cell.height);
            
            // Check all 4 neighbors
            for (int[] dir : DIRECTIONS) {
                int newRow = cell.row + dir[0];
                int newCol = cell.col + dir[1];
                
                if (newRow >= 0 && newRow < m && newCol >= 0 && newCol < n 
                    && !visited[newRow][newCol]) {
                    
                    visited[newRow][newCol] = true;
                    
                    // If neighbor is lower than current max height, water can be trapped
                    if (heightMap[newRow][newCol] < maxHeight) {
                        waterTrapped += maxHeight - heightMap[newRow][newCol];
                    }
                    
                    pq.offer(new Cell(newRow, newCol, heightMap[newRow][newCol]));
                }
            }
        }
        
        return waterTrapped;
    }
}
```

### Dry Run

**Input**: 
```
heightMap = [
  [1, 4, 3, 1, 3, 2],
  [3, 2, 1, 3, 2, 4],
  [2, 3, 3, 2, 3, 1]
]
```

**Visualization**:
```
Initial boundary (marked with *):
1* 4* 3* 1* 3* 2*
3*  2  1  3  2  4*
2* 3* 3* 2* 3* 1*

Step 1: Process cell with height 1 (top-left corner)
  maxHeight = 1
  Check neighbors: (1,0) with height 3
  3 > 1, no water trapped
  Add (1,0) to queue

Step 2: Process cell with height 1 (position [0,3])
  maxHeight = 1
  Check neighbor: (1,3) with height 3
  3 > 1, no water trapped

Step 3: Process cell with height 1 (bottom-right)
  maxHeight = 1
  Check neighbor: (2,4) with height 3
  3 > 1, no water trapped

Step 4: Process cell with height 2 (multiple positions)
  maxHeight = 2
  Check interior cells...

Step 5: Process cell (1,1) with height 2
  Current maxHeight = 2
  2 >= 2, no water trapped

Step 6: Process cell (1,2) with height 1
  Current maxHeight = 2
  1 < 2, water trapped = 2 - 1 = 1 unit
  waterTrapped = 1

... continue processing

Final waterTrapped = 4
```

**Detailed Water Calculation**:
```
Original:          Water Level:       Water Trapped:
1  4  3  1  3  2   1  4  3  2  3  2   0  0  0  1  0  0
3  2  1  3  2  4   3  2  2  3  2  4   0  0  1  0  0  0
2  3  3  2  3  1   2  3  3  2  3  2   0  0  0  0  0  1

Total water = 1 + 1 + 1 + 1 = 4 units
```

### Algorithm Explanation

1. **Initialize**: Add all boundary cells to priority queue
2. **Process**: Always process the cell with minimum height
3. **Track Maximum**: Keep track of maximum height seen so far (water level)
4. **Calculate Water**: For each unvisited neighbor:
   - If neighbor height < max height → water trapped
   - Update max height if needed
   - Add neighbor to queue
5. **Repeat**: Until all cells processed

### Why This Works

- Water flows from high to low
- Boundary determines water level for interior cells
- Processing from lowest boundary ensures correct water level
- Similar to Dijkstra's algorithm for shortest path

### Complexity Analysis

- **Time Complexity**: O(m × n × log(m × n))
  - Each cell added to priority queue once: O(m × n)
  - Each operation on priority queue: O(log(m × n))
  
- **Space Complexity**: O(m × n)
  - Priority queue: O(m × n)
  - Visited array: O(m × n)

### Test Cases

```java
@Test
public void testTrapRainWaterII() {
    TrappingRainWaterII solution = new TrappingRainWaterII();
    
    // Test case 1: Basic 2D trapping
    int[][] heightMap1 = {
        {1, 4, 3, 1, 3, 2},
        {3, 2, 1, 3, 2, 4},
        {2, 3, 3, 2, 3, 1}
    };
    assertEquals(4, solution.trapRainWater(heightMap1));
    
    // Test case 2: Square with depression in center
    int[][] heightMap2 = {
        {3, 3, 3, 3, 3},
        {3, 2, 2, 2, 3},
        {3, 2, 1, 2, 3},
        {3, 2, 2, 2, 3},
        {3, 3, 3, 3, 3}
    };
    assertEquals(10, solution.trapRainWater(heightMap2));
    
    // Test case 3: No water can be trapped
    int[][] heightMap3 = {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}
    };
    assertEquals(0, solution.trapRainWater(heightMap3));
    
    // Test case 4: Single row/column
    int[][] heightMap4 = {{1, 2, 3}};
    assertEquals(0, solution.trapRainWater(heightMap4));
    
    // Test case 5: Complex terrain
    int[][] heightMap5 = {
        {12, 13, 1, 12},
        {13, 4, 13, 12},
        {13, 8, 10, 12},
        {12, 13, 12, 12}
    };
    assertEquals(14, solution.trapRainWater(heightMap5));
}
```

### Edge Cases

1. **Empty or null matrix**: Return 0
2. **Single row or column**: No water can be trapped
3. **All same height**: No water trapped
4. **Monotonically increasing/decreasing**: No water trapped
5. **Multiple depressions**: Handle each independently

### Common Mistakes

1. **Using 1D approach**: Won't work for 2D
2. **Not using priority queue**: May process cells in wrong order
3. **Forgetting to mark visited**: May process cells multiple times
4. **Not tracking max height**: Incorrect water level calculation

---

## Problem 2: Minimum Window Subsequence

**Difficulty**: Hard  
**Pattern**: Two Pointers (Greedy)  
**LeetCode**: #727

### Problem Statement

Given strings `s` and `t`, return the minimum window substring of `s` such that every character in `t` (including duplicates) is included in the window **in order** (subsequence, not substring).

If there is no such window, return empty string.

### Examples

```
Input: s = "abcdebdde", t = "bde"
Output: "bcde"
Explanation: "bcde" is the shortest window that contains "bde" as subsequence

Input: s = "jmeqksfrsdcmsiwvaovztaqenprpvnbstl", t = "u"
Output: ""
Explanation: No 'u' in s
```

### Intuition

This is different from "Minimum Window Substring" because:
- Characters must appear **in order** (subsequence)
- We need to find the **shortest** window containing the subsequence

**Strategy**:
1. Find a window that contains all characters of `t` in order
2. Shrink the window from left to minimize length
3. Repeat to find all possible windows
4. Return the shortest one

### Solution

```java
public class MinimumWindowSubsequence {
    public String minWindow(String s, String t) {
        int sLen = s.length();
        int tLen = t.length();
        int minLen = Integer.MAX_VALUE;
        String result = "";
        
        int sIndex = 0;
        
        while (sIndex < sLen) {
            // Step 1: Find window that contains all characters of t
            int tIndex = 0;
            
            while (sIndex < sLen) {
                if (s.charAt(sIndex) == t.charAt(tIndex)) {
                    tIndex++;
                    if (tIndex == tLen) break;
                }
                sIndex++;
            }
            
            // If we didn't find all characters, no more windows possible
            if (sIndex == sLen) break;
            
            // Step 2: Shrink window from left
            int end = sIndex;
            tIndex = tLen - 1;
            
            while (tIndex >= 0) {
                if (s.charAt(sIndex) == t.charAt(tIndex)) {
                    tIndex--;
                }
                sIndex--;
            }
            
            sIndex++; // Move back to start of window
            
            // Step 3: Update result if this window is smaller
            if (end - sIndex + 1 < minLen) {
                minLen = end - sIndex + 1;
                result = s.substring(sIndex, end + 1);
            }
            
            sIndex++; // Move to next position to find next window
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `s = "abcdebdde"`, `t = "bde"`

```
s = a b c d e b d d e
    0 1 2 3 4 5 6 7 8

t = b d e
    0 1 2

=== Finding First Window ===

Step 1: Forward scan to find all characters of t
  sIndex = 0, tIndex = 0
  s[0] = 'a' != t[0] = 'b', sIndex++
  
  sIndex = 1, tIndex = 0
  s[1] = 'b' == t[0] = 'b', tIndex++, sIndex++
  
  sIndex = 2, tIndex = 1
  s[2] = 'c' != t[1] = 'd', sIndex++
  
  sIndex = 3, tIndex = 1
  s[3] = 'd' == t[1] = 'd', tIndex++, sIndex++
  
  sIndex = 4, tIndex = 2
  s[4] = 'e' == t[2] = 'e', tIndex++
  Found all characters! end = 4

Step 2: Backward scan to shrink window
  sIndex = 4, tIndex = 2
  s[4] = 'e' == t[2] = 'e', tIndex--, sIndex--
  
  sIndex = 3, tIndex = 1
  s[3] = 'd' == t[1] = 'd', tIndex--, sIndex--
  
  sIndex = 2, tIndex = 0
  s[2] = 'c' != t[0] = 'b', sIndex--
  
  sIndex = 1, tIndex = 0
  s[1] = 'b' == t[0] = 'b', tIndex--
  
  tIndex = -1, done shrinking
  sIndex++ → sIndex = 2 (start of window)

Step 3: Window found
  Window: s[2..4] = "cde" (length 3)
  But wait, we need "bde", so window is s[1..4] = "bcde" (length 4)
  result = "bcde", minLen = 4

=== Finding Second Window ===

Step 4: Continue from sIndex = 2
  Forward scan starting from sIndex = 2
  
  sIndex = 2, tIndex = 0
  s[2] = 'c' != t[0] = 'b', sIndex++
  
  ... continue scanning ...
  
  sIndex = 5, tIndex = 0
  s[5] = 'b' == t[0] = 'b', tIndex++
  
  sIndex = 6, tIndex = 1
  s[6] = 'd' == t[1] = 'd', tIndex++
  
  sIndex = 7, tIndex = 2
  s[7] = 'd' != t[2] = 'e', sIndex++
  
  sIndex = 8, tIndex = 2
  s[8] = 'e' == t[2] = 'e', tIndex++
  Found all characters! end = 8

Step 5: Backward scan
  Shrink from position 8 backwards
  Window: s[5..8] = "bdde" (length 4)
  Not shorter than "bcde", don't update

Step 6: Continue searching
  No more characters left
  
Final result: "bcde"
```

### Algorithm Steps

1. **Forward Scan**: 
   - Move right pointer to find all characters of `t` in order
   - Track position where last character of `t` is found

2. **Backward Scan**:
   - Move left pointer backwards to find start of window
   - Shrink window to minimum size

3. **Update Result**:
   - If current window is smaller, update result
   - Move to next position and repeat

4. **Optimization**:
   - Start next search from position after current window start
   - Avoids redundant searches

### Complexity Analysis

- **Time Complexity**: O(s × t)
  - In worst case, we scan entire string `s` for each character in `t`
  - Each position in `s` visited at most twice (forward + backward)
  
- **Space Complexity**: O(1)
  - Only using pointers, no extra data structures

### Test Cases

```java
@Test
public void testMinWindow() {
    MinimumWindowSubsequence solution = new MinimumWindowSubsequence();
    
    // Test case 1: Basic case
    assertEquals("bcde", solution.minWindow("abcdebdde", "bde"));
    
    // Test case 2: No such window
    assertEquals("", solution.minWindow("jmeqksfrsdcmsiwvaovztaqenprpvnbstl", "u"));
    
    // Test case 3: Multiple windows, return shortest
    assertEquals("abc", solution.minWindow("abcabc", "abc"));
    
    // Test case 4: t is single character
    assertEquals("a", solution.minWindow("abcde", "a"));
    
    // Test case 5: t equals s
    assertEquals("abc", solution.minWindow("abc", "abc"));
    
    // Test case 6: Window at the end
    assertEquals("bde", solution.minWindow("abcbde", "bde"));
    
    // Test case 7: Overlapping windows
    assertEquals("abbc", solution.minWindow("aabbbccc", "abc"));
}
```

### Edge Cases

1. **Empty strings**: Return ""
2. **t longer than s**: Return ""
3. **No valid window**: Return ""
4. **Multiple valid windows**: Return shortest
5. **Window at start/end**: Handle correctly

### Comparison with Minimum Window Substring

| Aspect | Substring (Medium) | Subsequence (Hard) |
|--------|-------------------|-------------------|
| Order | Not required | Required |
| Characters | All must be in window | Must appear in order |
| Approach | Sliding window + hash map | Two pointers + greedy |
| Complexity | O(s + t) | O(s × t) |

### Common Mistakes

1. **Confusing with substring problem**: Order matters here
2. **Not shrinking window**: Missing optimization
3. **Wrong pointer movement**: Must track both forward and backward
4. **Not handling edge cases**: Empty strings, no solution

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Trapping Rain Water II | Priority Queue + BFS | O(mn log(mn)) | O(mn) | Process from boundary inward |
| Minimum Window Subsequence | Two Pointers + Greedy | O(s × t) | O(1) | Forward + backward scan |

---

## 🎓 Key Takeaways

### Hard Problem Characteristics

1. **Multi-dimensional**: Extend 1D concepts to 2D (Trapping Rain Water II)
2. **Order Constraints**: Subsequence vs substring (Minimum Window Subsequence)
3. **Advanced Data Structures**: Priority queue, complex state tracking
4. **Multiple Passes**: Forward + backward scans, iterative refinement

### Problem-Solving Strategies

1. **Break Down**: Decompose into simpler subproblems
2. **Visualize**: Draw examples, trace algorithm
3. **Start Simple**: Solve 1D version first, then extend
4. **Optimize Incrementally**: Brute force → optimize → refine

### Practice Tips

1. **Master Medium First**: Hard problems build on medium concepts
2. **Understand Why**: Don't just memorize solutions
3. **Handle Edge Cases**: Empty inputs, no solution, extreme values
4. **Time Yourself**: Practice under interview conditions

---

**Congratulations!** You've completed all 20 Two Pointers problems! 🎉

**Next Steps**:
1. Review problems you found difficult
2. Solve variations and similar problems
3. Practice explaining solutions out loud
4. Time yourself on random problems

**Related Topics to Explore**:
- Sliding Window (advanced)
- Binary Search with Two Pointers
- Fast & Slow Pointers (Linked Lists)
- Three Pointers / Multi-pointer techniques
