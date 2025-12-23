# Dot Product of Two Arrays

## Problem Statement

Calculate the dot product of two integer arrays.

**Dot Product Formula:** `a·b = a[0]*b[0] + a[1]*b[1] + ... + a[n-1]*b[n-1]`

**Input:** Two integer arrays  
**Output:** Integer (dot product)

**Examples:**
```
dotProduct([1, 2, 3], [4, 5, 6]) = 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
dotProduct([1, 0, 1], [1, 1, 1]) = 1*1 + 0*1 + 1*1 = 1 + 0 + 1 = 2
dotProduct([2, 3], [4, 5]) = 2*4 + 3*5 = 8 + 15 = 23
dotProduct([0, 0], [5, 5]) = 0*5 + 0*5 = 0
```

---

## Solution Approach

### Optimal: Single Pass

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int dotProduct(int[] a, int[] b) {
    int result = 0;
    
    for (int i = 0; i < a.length; i++) {
        result += a[i] * b[i];
    }
    
    return result;
}
```

---

## Algorithm Walkthrough

### Example: [1, 2, 3] · [4, 5, 6]

```
Initial: result = 0

i=0: result = 0 + (1 * 4) = 0 + 4 = 4
i=1: result = 4 + (2 * 5) = 4 + 10 = 14
i=2: result = 14 + (3 * 6) = 14 + 18 = 32

Result: 32
```

---

## Complete Implementation

```java
public class DotProduct {
    
    // Approach 1: Basic loop (Recommended)
    public static int dotProduct(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        
        return result;
    }
    
    // Approach 2: Enhanced for loop
    public static int dotProductEnhanced(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        int result = 0;
        int i = 0;
        for (int val : a) {
            result += val * b[i++];
        }
        
        return result;
    }
    
    // Approach 3: Stream API
    public static int dotProductStream(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        return IntStream.range(0, a.length)
            .map(i -> a[i] * b[i])
            .sum();
    }
    
    // Approach 4: Recursive
    public static int dotProductRecursive(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        return dotProductHelper(a, b, 0);
    }
    
    private static int dotProductHelper(int[] a, int[] b, int index) {
        if (index == a.length) return 0;
        return a[index] * b[index] + dotProductHelper(a, b, index + 1);
    }
    
    // For long to avoid overflow
    public static long dotProductLong(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        long result = 0;
        for (int i = 0; i < a.length; i++) {
            result += (long) a[i] * b[i];
        }
        
        return result;
    }
    
    // For double arrays
    public static double dotProduct(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        
        return result;
    }
    
    // Bonus: Sparse dot product (for arrays with many zeros)
    public static int sparseDotProduct(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be non-null and same length");
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != 0 && b[i] != 0) {
                result += a[i] * b[i];
            }
        }
        
        return result;
    }
}
```

---

## Test Cases

```java
@Test
public void testDotProduct() {
    // Basic cases
    assertEquals(32, dotProduct(new int[]{1, 2, 3}, new int[]{4, 5, 6}));
    assertEquals(2, dotProduct(new int[]{1, 0, 1}, new int[]{1, 1, 1}));
    assertEquals(23, dotProduct(new int[]{2, 3}, new int[]{4, 5}));
    
    // Zero result
    assertEquals(0, dotProduct(new int[]{0, 0}, new int[]{5, 5}));
    assertEquals(0, dotProduct(new int[]{1, -1}, new int[]{1, 1}));
    
    // Single element
    assertEquals(6, dotProduct(new int[]{2}, new int[]{3}));
    
    // Negative numbers
    assertEquals(-14, dotProduct(new int[]{1, 2, 3}, new int[]{-1, -2, -3}));
    assertEquals(14, dotProduct(new int[]{-1, -2, -3}, new int[]{1, 2, 3}));
    
    // Large numbers (check overflow)
    assertEquals(2000000000, dotProduct(new int[]{1000000}, new int[]{2000}));
    
    // Empty arrays
    assertEquals(0, dotProduct(new int[]{}, new int[]{}));
    
    // Exceptions
    assertThrows(IllegalArgumentException.class, 
        () -> dotProduct(new int[]{1, 2}, new int[]{1}));
    assertThrows(IllegalArgumentException.class, 
        () -> dotProduct(null, new int[]{1}));
}
```

---

## Visual Representation

```
Array a: [1, 2, 3]
Array b: [4, 5, 6]

Dot Product:
  1 × 4 = 4
  2 × 5 = 10
  3 × 6 = 18
  ─────────
  Sum = 32

Geometric Interpretation:
a · b = |a| × |b| × cos(θ)
where θ is angle between vectors
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[], []` | `0` | Empty arrays |
| `[0], [5]` | `0` | Zero element |
| `[2], [3]` | `6` | Single element |
| `[1, -1], [1, 1]` | `0` | Cancellation |
| `[-1, -2], [-3, -4]` | `11` | All negative |
| Different lengths | Exception | Invalid input |

---

## Common Mistakes

1. **Not Checking Array Lengths:**
   ```java
   // WRONG - can cause ArrayIndexOutOfBoundsException
   for (int i = 0; i < a.length; i++) {
       result += a[i] * b[i];
   }
   
   // CORRECT
   if (a.length != b.length) {
       throw new IllegalArgumentException("Arrays must be same length");
   }
   ```

2. **Integer Overflow:**
   ```java
   // WRONG - can overflow for large values
   int result = 0;
   
   // CORRECT - use long for large values
   long result = 0;
   result += (long) a[i] * b[i];
   ```

3. **Not Handling Null:**
   ```java
   if (a == null || b == null) {
       throw new IllegalArgumentException("Arrays cannot be null");
   }
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Basic Loop | O(n) | O(1) | Optimal |
| Stream API | O(n) | O(1) | Functional style |
| Recursive | O(n) | O(n) | Stack space |
| Sparse | O(n) | O(1) | Better for sparse arrays |

---

## Mathematical Properties

### Dot Product Properties

```
1. Commutative: a · b = b · a
2. Distributive: a · (b + c) = a · b + a · c
3. Scalar multiplication: (ka) · b = k(a · b)
4. Zero vector: a · 0 = 0
5. Self dot product: a · a = |a|²

Geometric meaning:
a · b = |a| × |b| × cos(θ)

Special cases:
- a · b = 0 → vectors are perpendicular
- a · b > 0 → angle < 90°
- a · b < 0 → angle > 90°
```

---

## Optimization for Sparse Arrays

```java
// If arrays have many zeros, skip zero multiplications
public static int sparseDotProduct(int[] a, int[] b) {
    int result = 0;
    
    for (int i = 0; i < a.length; i++) {
        if (a[i] != 0 && b[i] != 0) {
            result += a[i] * b[i];
        }
    }
    
    return result;
}

// Example:
// a = [1, 0, 0, 0, 2, 0, 0, 3]
// b = [0, 5, 0, 0, 6, 0, 0, 7]
// Only compute: 2*6 + 3*7 = 12 + 21 = 33
// Skip 6 zero multiplications
```

---

## Related Problems

- **LeetCode 1570:** Dot Product of Two Sparse Vectors
- **LeetCode 311:** Sparse Matrix Multiplication
- **Vector operations:** Cross product, magnitude
- **Matrix multiplication:** Uses dot products

---

## Interview Tips

1. **Clarify Requirements:**
   - Same length guaranteed?
   - Handle null arrays?
   - Integer overflow concern?
   - Sparse arrays?

2. **Start with Simple Loop:**
   - Most straightforward
   - O(n) time, O(1) space

3. **Mention Optimizations:**
   - Sparse array optimization
   - Use long for overflow

4. **Walk Through Example:**
   - Use [1, 2, 3] · [4, 5, 6]
   - Show step-by-step calculation

5. **Discuss Properties:**
   - Commutative
   - Geometric interpretation

---

## Real-World Applications

- **Machine Learning:** Feature similarity, neural networks
- **Computer Graphics:** Lighting calculations, projections
- **Physics:** Work calculation (force · displacement)
- **Signal Processing:** Correlation, convolution
- **Recommendation Systems:** User similarity
- **Natural Language Processing:** Document similarity (TF-IDF)

---

## Geometric Interpretation

```
2D Example:
a = [3, 4]  (vector from origin)
b = [1, 2]

Dot product: 3*1 + 4*2 = 3 + 8 = 11

Magnitude:
|a| = √(3² + 4²) = √25 = 5
|b| = √(1² + 2²) = √5 ≈ 2.236

Angle:
cos(θ) = (a·b) / (|a|×|b|)
cos(θ) = 11 / (5 × 2.236) ≈ 0.984
θ ≈ 10.3°
```

---

## Stream API vs Loop Performance

```java
// Traditional loop (faster)
int result = 0;
for (int i = 0; i < a.length; i++) {
    result += a[i] * b[i];
}

// Stream API (more functional, slightly slower)
int result = IntStream.range(0, a.length)
    .map(i -> a[i] * b[i])
    .sum();

// For small arrays: negligible difference
// For large arrays: loop is ~2x faster
// Use loop for performance-critical code
```

---

## Handling Overflow

```java
// Problem: Large values can overflow
int[] a = {1000000, 1000000};
int[] b = {1000000, 1000000};
// Result: 2,000,000,000,000 > Integer.MAX_VALUE

// Solution 1: Use long
public static long dotProductLong(int[] a, int[] b) {
    long result = 0;
    for (int i = 0; i < a.length; i++) {
        result += (long) a[i] * b[i];
    }
    return result;
}

// Solution 2: Check for overflow
public static int dotProductSafe(int[] a, int[] b) {
    long result = 0;
    for (int i = 0; i < a.length; i++) {
        result += (long) a[i] * b[i];
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow");
        }
    }
    return (int) result;
}
```

---

## Key Takeaways

✅ Simple loop is optimal - O(n) time, O(1) space  
✅ Formula: sum of element-wise products  
✅ Check arrays are same length before processing  
✅ Handle null arrays appropriately  
✅ Use long to avoid integer overflow  
✅ Sparse optimization: skip zero multiplications  
✅ Commutative property: a·b = b·a  
✅ Geometric meaning: projection and angle between vectors
