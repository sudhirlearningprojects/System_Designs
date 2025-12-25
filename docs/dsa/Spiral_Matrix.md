# Spiral Matrix Traversal

## Problem Statement

Print a matrix in spiral order (clockwise from outside to inside).

**Input:** 2D matrix (m × n)  
**Output:** List of elements in spiral order

**Examples:**
```
Input: [[1, 2, 3],
        [4, 5, 6],
        [7, 8, 9]]

Output: [1, 2, 3, 6, 9, 8, 7, 4, 5]

Visualization:
1 → 2 → 3
        ↓
4 → 5   6
↑       ↓
7 ← 8 ← 9

Input: [[1, 2, 3, 4],
        [5, 6, 7, 8],
        [9, 10, 11, 12]]

Output: [1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]

Input: [[1]]
Output: [1]
```

---

## Solution Approaches

### Approach 1: Layer by Layer (Optimal)

**Time Complexity:** O(m × n)  
**Space Complexity:** O(1) excluding output

```java
public static List<Integer> spiralOrder(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    if (matrix == null || matrix.length == 0) return result;
    
    int top = 0;
    int bottom = matrix.length - 1;
    int left = 0;
    int right = matrix[0].length - 1;
    
    while (top <= bottom && left <= right) {
        // Traverse right
        for (int i = left; i <= right; i++) {
            result.add(matrix[top][i]);
        }
        top++;
        
        // Traverse down
        for (int i = top; i <= bottom; i++) {
            result.add(matrix[i][right]);
        }
        right--;
        
        // Traverse left (if still have rows)
        if (top <= bottom) {
            for (int i = right; i >= left; i--) {
                result.add(matrix[bottom][i]);
            }
            bottom--;
        }
        
        // Traverse up (if still have columns)
        if (left <= right) {
            for (int i = bottom; i >= top; i--) {
                result.add(matrix[i][left]);
            }
            left++;
        }
    }
    
    return result;
}
```

---

### Approach 2: Direction Vectors

**Time Complexity:** O(m × n)  
**Space Complexity:** O(m × n) for visited array

```java
public static List<Integer> spiralOrderDirection(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    if (matrix == null || matrix.length == 0) return result;
    
    int m = matrix.length;
    int n = matrix[0].length;
    boolean[][] visited = new boolean[m][n];
    
    int[] dr = {0, 1, 0, -1}; // right, down, left, up
    int[] dc = {1, 0, -1, 0};
    
    int r = 0, c = 0, dir = 0;
    
    for (int i = 0; i < m * n; i++) {
        result.add(matrix[r][c]);
        visited[r][c] = true;
        
        int nr = r + dr[dir];
        int nc = c + dc[dir];
        
        if (nr < 0 || nr >= m || nc < 0 || nc >= n || visited[nr][nc]) {
            dir = (dir + 1) % 4; // Turn clockwise
            nr = r + dr[dir];
            nc = c + dc[dir];
        }
        
        r = nr;
        c = nc;
    }
    
    return result;
}
```

---

### Approach 3: Recursive

**Time Complexity:** O(m × n)  
**Space Complexity:** O(min(m, n)) for recursion stack

```java
public static List<Integer> spiralOrderRecursive(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    if (matrix == null || matrix.length == 0) return result;
    
    spiralHelper(matrix, 0, matrix.length - 1, 0, matrix[0].length - 1, result);
    return result;
}

private static void spiralHelper(int[][] matrix, int top, int bottom, 
                                 int left, int right, List<Integer> result) {
    if (top > bottom || left > right) return;
    
    // Right
    for (int i = left; i <= right; i++) {
        result.add(matrix[top][i]);
    }
    
    // Down
    for (int i = top + 1; i <= bottom; i++) {
        result.add(matrix[i][right]);
    }
    
    // Left (if multiple rows)
    if (top < bottom) {
        for (int i = right - 1; i >= left; i--) {
            result.add(matrix[bottom][i]);
        }
    }
    
    // Up (if multiple columns)
    if (left < right) {
        for (int i = bottom - 1; i > top; i--) {
            result.add(matrix[i][left]);
        }
    }
    
    spiralHelper(matrix, top + 1, bottom - 1, left + 1, right - 1, result);
}
```

---

## Algorithm Walkthrough

### Example: [[1, 2, 3], [4, 5, 6], [7, 8, 9]]

**Layer by Layer Approach:**

```
Matrix:
1  2  3
4  5  6
7  8  9

Initial: top=0, bottom=2, left=0, right=2

Layer 1:
  Right (row 0): 1, 2, 3
  top=1
  
  Down (col 2): 6, 9
  right=1
  
  Left (row 2): 8, 7
  bottom=1
  
  Up (col 0): 4
  left=1

State: top=1, bottom=1, left=1, right=1

Layer 2:
  Right (row 1): 5
  top=2
  
  top > bottom, exit

Result: [1, 2, 3, 6, 9, 8, 7, 4, 5]
```

### Visual Step-by-Step

```
Step 1: Traverse Right (top row)
[1] [2] [3]
 4   5   6
 7   8   9
Result: [1, 2, 3]

Step 2: Traverse Down (right column)
 1   2   3
 4   5  [6]
 7   8  [9]
Result: [1, 2, 3, 6, 9]

Step 3: Traverse Left (bottom row)
 1   2   3
 4   5   6
 7  [8] [9]
Result: [1, 2, 3, 6, 9, 8, 7]

Step 4: Traverse Up (left column)
 1   2   3
[4]  5   6
[7]  8   9
Result: [1, 2, 3, 6, 9, 8, 7, 4]

Step 5: Inner layer (single element)
 1   2   3
 4  [5]  6
 7   8   9
Result: [1, 2, 3, 6, 9, 8, 7, 4, 5]
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Layer by layer (Optimal)
    public static List<Integer> spiralOrder(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0) return result;
        
        int top = 0;
        int bottom = matrix.length - 1;
        int left = 0;
        int right = matrix[0].length - 1;
        
        while (top <= bottom && left <= right) {
            // Right
            for (int i = left; i <= right; i++) {
                result.add(matrix[top][i]);
            }
            top++;
            
            // Down
            for (int i = top; i <= bottom; i++) {
                result.add(matrix[i][right]);
            }
            right--;
            
            // Left
            if (top <= bottom) {
                for (int i = right; i >= left; i--) {
                    result.add(matrix[bottom][i]);
                }
                bottom--;
            }
            
            // Up
            if (left <= right) {
                for (int i = bottom; i >= top; i--) {
                    result.add(matrix[i][left]);
                }
                left++;
            }
        }
        
        return result;
    }
    
    // Print as comma-separated string
    public static String spiralOrderString(int[][] matrix) {
        List<Integer> result = spiralOrder(matrix);
        return result.stream()
                     .map(String::valueOf)
                     .collect(Collectors.joining(","));
    }
    
    // Approach 2: Direction vectors
    public static List<Integer> spiralOrderDirection(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        if (matrix == null || matrix.length == 0) return result;
        
        int m = matrix.length;
        int n = matrix[0].length;
        boolean[][] visited = new boolean[m][n];
        
        int[] dr = {0, 1, 0, -1};
        int[] dc = {1, 0, -1, 0};
        
        int r = 0, c = 0, dir = 0;
        
        for (int i = 0; i < m * n; i++) {
            result.add(matrix[r][c]);
            visited[r][c] = true;
            
            int nr = r + dr[dir];
            int nc = c + dc[dir];
            
            if (nr < 0 || nr >= m || nc < 0 || nc >= n || visited[nr][nc]) {
                dir = (dir + 1) % 4;
                nr = r + dr[dir];
                nc = c + dc[dir];
            }
            
            r = nr;
            c = nc;
        }
        
        return result;
    }
    
    public static boolean doTestsPass() {
        // Test 1: 3x3 matrix
        int[][] test1 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        List<Integer> expected1 = Arrays.asList(1, 2, 3, 6, 9, 8, 7, 4, 5);
        if (!spiralOrder(test1).equals(expected1)) return false;
        
        // Test 2: 3x4 matrix
        int[][] test2 = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
        List<Integer> expected2 = Arrays.asList(1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7);
        if (!spiralOrder(test2).equals(expected2)) return false;
        
        // Test 3: Single element
        int[][] test3 = {{1}};
        List<Integer> expected3 = Arrays.asList(1);
        if (!spiralOrder(test3).equals(expected3)) return false;
        
        // Test 4: Single row
        int[][] test4 = {{1, 2, 3, 4}};
        List<Integer> expected4 = Arrays.asList(1, 2, 3, 4);
        if (!spiralOrder(test4).equals(expected4)) return false;
        
        // Test 5: Single column
        int[][] test5 = {{1}, {2}, {3}};
        List<Integer> expected5 = Arrays.asList(1, 2, 3);
        if (!spiralOrder(test5).equals(expected5)) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        System.out.println("Matrix:");
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
        System.out.println("\nSpiral order: " + spiralOrderString(matrix));
    }
}
```

---

## Test Cases

```java
@Test
public void testSpiralOrder() {
    // Test 1: 3x3 square
    int[][] test1 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    assertEquals(Arrays.asList(1, 2, 3, 6, 9, 8, 7, 4, 5), spiralOrder(test1));
    
    // Test 2: 3x4 rectangle
    int[][] test2 = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
    assertEquals(Arrays.asList(1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7), 
                 spiralOrder(test2));
    
    // Test 3: 4x3 rectangle
    int[][] test3 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}};
    assertEquals(Arrays.asList(1, 2, 3, 6, 9, 12, 11, 10, 7, 4, 5, 8), 
                 spiralOrder(test3));
    
    // Test 4: Single element
    int[][] test4 = {{1}};
    assertEquals(Arrays.asList(1), spiralOrder(test4));
    
    // Test 5: Single row
    int[][] test5 = {{1, 2, 3, 4}};
    assertEquals(Arrays.asList(1, 2, 3, 4), spiralOrder(test5));
    
    // Test 6: Single column
    int[][] test6 = {{1}, {2}, {3}, {4}};
    assertEquals(Arrays.asList(1, 2, 3, 4), spiralOrder(test6));
    
    // Test 7: 2x2
    int[][] test7 = {{1, 2}, {3, 4}};
    assertEquals(Arrays.asList(1, 2, 4, 3), spiralOrder(test7));
    
    // Test 8: Empty matrix
    int[][] test8 = {};
    assertEquals(Arrays.asList(), spiralOrder(test8));
}
```

---

## Visual Representation

### 3x3 Matrix Layers

```
Layer 1 (outer):
→ → →
↓   ↓
← ← ←
↑

Layer 2 (inner):
    5

Matrix:
1 → 2 → 3
        ↓
4   5   6
↑       ↓
7 ← 8 ← 9

Order: 1,2,3,6,9,8,7,4,5
```

### 4x5 Matrix

```
Matrix:
 1   2   3   4   5
 6   7   8   9  10
11  12  13  14  15
16  17  18  19  20

Spiral:
 1 → 2 → 3 → 4 → 5
                 ↓
 6 → 7 → 8 → 9  10
 ↑           ↓   ↓
11  12 ← 13  14  15
 ↑           ↓   ↓
16 ← 17 ← 18 ← 19  20

Order: 1,2,3,4,5,10,15,20,19,18,17,16,11,6,7,8,9,14,13,12
```

### Boundary Movement

```
Initial boundaries:
top = 0
bottom = 2
left = 0
right = 2

After layer 1:
top = 1
bottom = 1
left = 1
right = 1

After layer 2:
top = 2 (top > bottom, stop)
```

---

## Edge Cases

1. **Empty matrix:** [[]] → []
2. **Single element:** [[1]] → [1]
3. **Single row:** [[1, 2, 3]] → [1, 2, 3]
4. **Single column:** [[1], [2], [3]] → [1, 2, 3]
5. **2x2 matrix:** [[1, 2], [3, 4]] → [1, 2, 4, 3]
6. **Tall matrix:** More rows than columns
7. **Wide matrix:** More columns than rows

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Layer by Layer | O(m×n) | O(1) | **Optimal**, excluding output |
| Direction Vectors | O(m×n) | O(m×n) | Visited array needed |
| Recursive | O(m×n) | O(min(m,n)) | Recursion stack |

**Why O(m×n)?**
- Must visit every element exactly once
- Each element added to result once

**Space Optimization:**
- Layer by layer: O(1) extra space
- Direction vectors: O(m×n) for visited array
- Output space: O(m×n) always required

---

## Related Problems

1. **Spiral Matrix II** - Generate spiral matrix
2. **Spiral Matrix III** - Start from center
3. **Diagonal Traverse** - Diagonal order
4. **Rotate Image** - 90-degree rotation
5. **Set Matrix Zeroes** - Matrix modification
6. **Game of Life** - Matrix simulation

---

## Interview Tips

### Clarification Questions
1. Can matrix be empty? (Handle edge case)
2. Can matrix be non-square? (Yes, m×n)
3. Return list or print? (Usually return list)
4. What about single row/column? (Handle specially)
5. Can we modify input? (Usually no need)

### Approach Explanation
1. "Use four boundaries: top, bottom, left, right"
2. "Process one layer at a time"
3. "Move boundaries inward after each direction"
4. "Check boundaries before left and up traversal"
5. "O(m×n) time, O(1) extra space"

### Common Mistakes
- Not checking `top <= bottom` before left traversal
- Not checking `left <= right` before up traversal
- Wrong boundary updates
- Off-by-one errors in loops
- Not handling single row/column cases

### Why Boundary Checks?

```java
// Without checks:
// Single row: [[1, 2, 3]]
// Would traverse: 1,2,3 (right), then 3,2,1 (left) ✗
// Duplicates!

// With check:
if (top <= bottom) {
    // Only traverse left if still have rows
}

// Correct: 1,2,3 ✓
```

---

## Real-World Applications

1. **Image Processing** - Spiral scanning
2. **Game Development** - Spiral movement patterns
3. **Data Visualization** - Spiral layouts
4. **Printer Patterns** - Spiral printing
5. **Maze Generation** - Spiral maze creation
6. **UI Animations** - Spiral reveal effects

---

## Key Takeaways

1. **Four Boundaries:** Track top, bottom, left, right
2. **Four Directions:** Right, Down, Left, Up
3. **Boundary Updates:** Move inward after each direction
4. **Conditional Traversal:** Check boundaries before left/up
5. **Time Complexity:** O(m×n) - visit each element once
6. **Space Complexity:** O(1) extra space (optimal)
7. **Edge Cases:** Single row, single column, empty matrix

---

## Additional Notes

### Why Check Before Left and Up?

```
Single row: [[1, 2, 3]]

Without check:
  Right: 1, 2, 3 (top=0)
  top++ → top=1
  Down: nothing (top > bottom)
  right-- → right=1
  Left: 2, 1 ✗ (duplicates!)

With check (top <= bottom):
  Right: 1, 2, 3
  top++ → top=1
  Down: nothing
  right-- → right=1
  Left: skipped (top > bottom) ✓
```

### Spiral Matrix II (Generate)

```java
// Generate n×n matrix in spiral order
public static int[][] generateMatrix(int n) {
    int[][] matrix = new int[n][n];
    int top = 0, bottom = n - 1, left = 0, right = n - 1;
    int num = 1;
    
    while (top <= bottom && left <= right) {
        for (int i = left; i <= right; i++) matrix[top][i] = num++;
        top++;
        
        for (int i = top; i <= bottom; i++) matrix[i][right] = num++;
        right--;
        
        for (int i = right; i >= left; i--) matrix[bottom][i] = num++;
        bottom--;
        
        for (int i = bottom; i >= top; i--) matrix[i][left] = num++;
        left++;
    }
    
    return matrix;
}

// Example: n=3
// [[1, 2, 3],
//  [8, 9, 4],
//  [7, 6, 5]]
```

### Counter-Clockwise Spiral

```java
// Reverse direction: Right, Up, Left, Down
public static List<Integer> spiralCounterClockwise(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    if (matrix == null || matrix.length == 0) return result;
    
    int top = 0, bottom = matrix.length - 1;
    int left = 0, right = matrix[0].length - 1;
    
    while (top <= bottom && left <= right) {
        // Right
        for (int i = left; i <= right; i++) {
            result.add(matrix[bottom][i]);
        }
        bottom--;
        
        // Up
        for (int i = bottom; i >= top; i--) {
            result.add(matrix[i][right]);
        }
        right--;
        
        // Left
        if (left <= right) {
            for (int i = right; i >= left; i--) {
                result.add(matrix[top][i]);
            }
            top++;
        }
        
        // Down
        if (top <= bottom) {
            for (int i = top; i <= bottom; i++) {
                result.add(matrix[i][left]);
            }
            left++;
        }
    }
    
    return result;
}
```

### Diagonal Spiral

```java
// Start from center and spiral outward
public static List<Integer> spiralFromCenter(int[][] matrix) {
    // More complex, requires different approach
    // Start from center, expand outward
}
```

### Memory Optimization

```java
// If allowed to modify input, mark visited with special value
public static List<Integer> spiralOrderInPlace(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    // Use Integer.MIN_VALUE or similar to mark visited
    // Saves O(m×n) space for visited array
}
```

### Comparison: Layer vs Direction

```
Layer by Layer:
  + O(1) extra space
  + Clearer logic
  + Easier to debug
  - More code

Direction Vectors:
  + More elegant
  + Easier to modify direction
  - O(m×n) space for visited
  - Slightly harder to understand
```
