# Pascal's Triangle - Get Value at Position

## Problem Statement

Given a column and row position in Pascal's Triangle, return the value at that position.

**Pascal's Triangle:**
```
Row 0:              1
Row 1:            1   1
Row 2:          1   2   1
Row 3:        1   3   3   1
Row 4:      1   4   6   4   1
Row 5:    1   5  10  10   5   1
Row 6:  1   6  15  20  15   6   1
```

**Examples:**
```
pascal(0, 0) = 1
pascal(1, 2) = 2
pascal(5, 6) = 6
pascal(4, 8) = 70
pascal(6, 6) = 1
```

---

## Solution Approaches

### Approach 1: Combinatorial Formula (Optimal)

**Time Complexity:** O(min(col, row-col))  
**Space Complexity:** O(1)

```java
public static int pascal(int col, int row) {
    if (col == 0 || col == row) return 1;
    
    // C(row, col) = row! / (col! * (row-col)!)
    // Optimize: C(n, k) = C(n, n-k), use smaller k
    col = Math.min(col, row - col);
    
    long result = 1;
    for (int i = 0; i < col; i++) {
        result = result * (row - i) / (i + 1);
    }
    
    return (int) result;
}
```

**Formula:** `C(row, col) = row! / (col! × (row-col)!)`

---

### Approach 2: Recursive with Memoization

**Time Complexity:** O(row × col)  
**Space Complexity:** O(row × col)

```java
private static Map<String, Integer> memo = new HashMap<>();

public static int pascal(int col, int row) {
    if (col == 0 || col == row) return 1;
    
    String key = col + "," + row;
    if (memo.containsKey(key)) {
        return memo.get(key);
    }
    
    int result = pascal(col - 1, row - 1) + pascal(col, row - 1);
    memo.put(key, result);
    
    return result;
}
```

**Recurrence:** `pascal(col, row) = pascal(col-1, row-1) + pascal(col, row-1)`

---

### Approach 3: Dynamic Programming (Build Triangle)

**Time Complexity:** O(row²)  
**Space Complexity:** O(row²)

```java
public static int pascal(int col, int row) {
    int[][] triangle = new int[row + 1][];
    
    for (int i = 0; i <= row; i++) {
        triangle[i] = new int[i + 1];
        triangle[i][0] = 1;
        triangle[i][i] = 1;
        
        for (int j = 1; j < i; j++) {
            triangle[i][j] = triangle[i - 1][j - 1] + triangle[i - 1][j];
        }
    }
    
    return triangle[row][col];
}
```

---

### Approach 4: Space-Optimized DP

**Time Complexity:** O(row²)  
**Space Complexity:** O(row)

```java
public static int pascal(int col, int row) {
    int[] prev = new int[row + 1];
    prev[0] = 1;
    
    for (int i = 1; i <= row; i++) {
        int[] curr = new int[i + 1];
        curr[0] = 1;
        curr[i] = 1;
        
        for (int j = 1; j < i; j++) {
            curr[j] = prev[j - 1] + prev[j];
        }
        
        prev = curr;
    }
    
    return prev[col];
}
```

---

## Algorithm Explanation

### Pascal's Triangle Properties

1. **Edge values:** Always 1
2. **Interior values:** Sum of two values above
3. **Symmetry:** `pascal(col, row) = pascal(row-col, row)`
4. **Binomial coefficient:** `pascal(col, row) = C(row, col)`

### Visual Representation

```
         1                    C(0,0)
       1   1                C(1,0) C(1,1)
     1   2   1            C(2,0) C(2,1) C(2,2)
   1   3   3   1        C(3,0) C(3,1) C(3,2) C(3,3)
 1   4   6   4   1    C(4,0) C(4,1) C(4,2) C(4,3) C(4,4)

Each value = sum of two above:
  2 = 1 + 1
  3 = 1 + 2
  6 = 3 + 3
```

---

## Detailed Walkthrough

### Example: pascal(2, 4)

**Using Combinatorial Formula:**
```
C(4, 2) = 4! / (2! × 2!)
        = (4 × 3 × 2 × 1) / ((2 × 1) × (2 × 1))
        = 24 / 4
        = 6

Optimized calculation:
result = 1
i=0: result = 1 × (4-0) / (0+1) = 1 × 4 / 1 = 4
i=1: result = 4 × (4-1) / (1+1) = 4 × 3 / 2 = 6

Result: 6
```

**Using Recursion:**
```
pascal(2, 4)
= pascal(1, 3) + pascal(2, 3)
= [pascal(0, 2) + pascal(1, 2)] + [pascal(1, 2) + pascal(2, 2)]
= [1 + 2] + [2 + 1]
= 3 + 3
= 6
```

**Using DP (Build Triangle):**
```
Row 0: [1]
Row 1: [1, 1]
Row 2: [1, 2, 1]
Row 3: [1, 3, 3, 1]
Row 4: [1, 4, 6, 4, 1]
              ^
         col=2, row=4
         
Result: 6
```

---

## Complete Implementation

```java
public class PascalTriangle {
    
    // Approach 1: Combinatorial Formula (RECOMMENDED)
    public static int pascal(int col, int row) {
        if (col < 0 || col > row) {
            throw new IllegalArgumentException("Invalid position");
        }
        
        if (col == 0 || col == row) return 1;
        
        // Optimize: use smaller of col or (row - col)
        col = Math.min(col, row - col);
        
        long result = 1;
        for (int i = 0; i < col; i++) {
            result = result * (row - i) / (i + 1);
        }
        
        return (int) result;
    }
    
    // Approach 2: Recursive with Memoization
    private static Map<String, Integer> memo = new HashMap<>();
    
    public static int pascalRecursive(int col, int row) {
        if (col == 0 || col == row) return 1;
        
        String key = col + "," + row;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }
        
        int result = pascalRecursive(col - 1, row - 1) + 
                     pascalRecursive(col, row - 1);
        memo.put(key, result);
        
        return result;
    }
    
    // Approach 3: Build entire triangle
    public static int pascalDP(int col, int row) {
        int[][] triangle = new int[row + 1][];
        
        for (int i = 0; i <= row; i++) {
            triangle[i] = new int[i + 1];
            triangle[i][0] = 1;
            triangle[i][i] = 1;
            
            for (int j = 1; j < i; j++) {
                triangle[i][j] = triangle[i - 1][j - 1] + triangle[i - 1][j];
            }
        }
        
        return triangle[row][col];
    }
    
    // Approach 4: Generate entire row
    public static int[] generateRow(int row) {
        int[] result = new int[row + 1];
        result[0] = 1;
        
        for (int i = 1; i <= row; i++) {
            for (int j = i; j > 0; j--) {
                result[j] = result[j] + result[j - 1];
            }
        }
        
        return result;
    }
    
    // Bonus: Generate entire triangle
    public static List<List<Integer>> generateTriangle(int numRows) {
        List<List<Integer>> triangle = new ArrayList<>();
        
        for (int i = 0; i < numRows; i++) {
            List<Integer> row = new ArrayList<>();
            row.add(1);
            
            for (int j = 1; j < i; j++) {
                row.add(triangle.get(i - 1).get(j - 1) + 
                       triangle.get(i - 1).get(j));
            }
            
            if (i > 0) row.add(1);
            triangle.add(row);
        }
        
        return triangle;
    }
}
```

---

## Test Cases

```java
@Test
public void testPascalTriangle() {
    // Edge cases
    assertEquals(1, pascal(0, 0));
    assertEquals(1, pascal(0, 5));
    assertEquals(1, pascal(5, 5));
    
    // Basic cases
    assertEquals(2, pascal(1, 2));
    assertEquals(3, pascal(1, 3));
    assertEquals(3, pascal(2, 3));
    
    // From problem
    assertEquals(1, pascal(0, 0));
    assertEquals(2, pascal(1, 2));
    assertEquals(6, pascal(5, 6));
    assertEquals(70, pascal(4, 8));
    assertEquals(1, pascal(6, 6));
    
    // Symmetry
    assertEquals(pascal(2, 5), pascal(3, 5)); // Both = 10
    
    // Larger values
    assertEquals(210, pascal(4, 10));
    assertEquals(252, pascal(5, 10));
}
```

---

## Complexity Comparison

| Approach | Time | Space | Best For |
|----------|------|-------|----------|
| Combinatorial | O(min(col, row-col)) | O(1) | Single query |
| Recursive + Memo | O(row × col) | O(row × col) | Multiple queries |
| DP (Full Triangle) | O(row²) | O(row²) | Need entire triangle |
| Space-Optimized DP | O(row²) | O(row) | Memory constrained |

---

## Why Combinatorial Formula is Optimal

```
For pascal(4, 8):

Combinatorial: 4 iterations
  result = 1
  result = 1 × 8 / 1 = 8
  result = 8 × 7 / 2 = 28
  result = 28 × 6 / 3 = 56
  result = 56 × 5 / 4 = 70

DP: Build 8 rows = 1+2+3+4+5+6+7+8 = 36 operations

Recursive: Exponential without memoization
```

---

## Common Mistakes

1. **Integer Overflow:**
   ```java
   // WRONG - can overflow for large values
   int result = 1;
   
   // CORRECT - use long
   long result = 1;
   return (int) result;
   ```

2. **Division Order:**
   ```java
   // WRONG - loses precision
   result = result * (row - i) / (i + 1);
   
   // CORRECT - multiply first, then divide
   result = result * (row - i) / (i + 1);
   ```

3. **Not Using Symmetry:**
   ```java
   // Optimize using C(n, k) = C(n, n-k)
   col = Math.min(col, row - col);
   ```

4. **Boundary Conditions:**
   ```java
   if (col == 0 || col == row) return 1;
   ```

---

## Mathematical Properties

### Binomial Coefficient

```
C(n, k) = n! / (k! × (n-k)!)

Properties:
1. C(n, 0) = C(n, n) = 1
2. C(n, k) = C(n, n-k)  (symmetry)
3. C(n, k) = C(n-1, k-1) + C(n-1, k)  (Pascal's identity)
4. Sum of row n = 2^n
```

### Row Sums

```
Row 0: 1                    = 2^0 = 1
Row 1: 1 + 1                = 2^1 = 2
Row 2: 1 + 2 + 1            = 2^2 = 4
Row 3: 1 + 3 + 3 + 1        = 2^3 = 8
Row 4: 1 + 4 + 6 + 4 + 1    = 2^4 = 16
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `(0, 0)` | `1` | Top of triangle |
| `(0, n)` | `1` | Left edge |
| `(n, n)` | `1` | Right edge |
| `(1, 2)` | `2` | Interior value |
| `(5, 10)` | `252` | Large value |

---

## Related Problems

- **LeetCode 118:** Pascal's Triangle
- **LeetCode 119:** Pascal's Triangle II
- **Binomial Coefficients:** Combinatorics
- **Combinations:** C(n, k) calculations

---

## Interview Tips

1. **Clarify Requirements:**
   - 0-indexed or 1-indexed?
   - Return single value or entire triangle?
   - Handle invalid inputs?

2. **Start with Formula:**
   - Mention binomial coefficient
   - C(row, col) = row! / (col! × (row-col)!)

3. **Optimize:**
   - Use symmetry: min(col, row-col)
   - Avoid factorial calculation

4. **Walk Through Example:**
   - Use pascal(2, 4) = 6
   - Show calculation steps

5. **Discuss Alternatives:**
   - DP for multiple queries
   - Recursion with memoization

---

## Real-World Applications

- **Probability Theory:** Binomial distribution
- **Combinatorics:** Counting combinations
- **Algebra:** Binomial expansion (a+b)^n
- **Computer Science:** Dynamic programming examples
- **Statistics:** Probability calculations
- **Game Theory:** Outcome probabilities

---

## Binomial Expansion

```
(a + b)^n = Σ C(n, k) × a^(n-k) × b^k

Example: (a + b)^3
= C(3,0)a³ + C(3,1)a²b + C(3,2)ab² + C(3,3)b³
= 1a³ + 3a²b + 3ab² + 1b³

Coefficients: 1, 3, 3, 1 (Row 3 of Pascal's Triangle)
```

---

## Performance Optimization

```java
// Avoid repeated calculations
public static int pascalOptimized(int col, int row) {
    if (col == 0 || col == row) return 1;
    
    // Use symmetry
    if (col > row - col) {
        col = row - col;
    }
    
    long result = 1;
    for (int i = 0; i < col; i++) {
        // Multiply first to avoid precision loss
        result = result * (row - i) / (i + 1);
    }
    
    return (int) result;
}
```

---

## Key Takeaways

✅ Combinatorial formula is optimal for single query - O(k)  
✅ Formula: C(row, col) = row! / (col! × (row-col)!)  
✅ Use symmetry: C(n, k) = C(n, n-k)  
✅ Edge values always equal 1  
✅ Interior: sum of two values above  
✅ Use long to avoid integer overflow  
✅ DP approach better for multiple queries or full triangle
