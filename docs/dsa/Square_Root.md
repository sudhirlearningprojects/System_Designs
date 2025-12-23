# Square Root of Double

## Problem Statement

Calculate the square root of a double number without using built-in `Math.sqrt()`.

**Input:** Double number  
**Output:** Square root as double

**Examples:**
```
sqrt(4.0) = 2.0
sqrt(2.0) = 1.414213562...
sqrt(0.25) = 0.5
sqrt(0.0) = 0.0
sqrt(100.0) = 10.0
```

---

## Solution Approaches

### Approach 1: Newton's Method (Newton-Raphson) - OPTIMAL

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public double sqrt(double x) {
    if (x < 0) throw new IllegalArgumentException("Negative number");
    if (x == 0) return 0;
    
    double guess = x;
    double epsilon = 1e-10;
    
    while (Math.abs(guess * guess - x) > epsilon) {
        guess = (guess + x / guess) / 2.0;
    }
    
    return guess;
}
```

**Formula:** `next = (current + x/current) / 2`

---

### Approach 2: Binary Search

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public double sqrt(double x) {
    if (x < 0) throw new IllegalArgumentException("Negative number");
    if (x == 0) return 0;
    if (x == 1) return 1;
    
    double left = 0, right = x;
    if (x < 1) right = 1; // For numbers < 1
    
    double epsilon = 1e-10;
    
    while (right - left > epsilon) {
        double mid = left + (right - left) / 2.0;
        double square = mid * mid;
        
        if (Math.abs(square - x) < epsilon) {
            return mid;
        } else if (square < x) {
            left = mid;
        } else {
            right = mid;
        }
    }
    
    return left;
}
```

---

### Approach 3: Babylonian Method (Ancient Algorithm)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public double sqrt(double x) {
    if (x < 0) throw new IllegalArgumentException("Negative number");
    if (x == 0) return 0;
    
    double guess = x / 2.0; // Initial guess
    double epsilon = 1e-10;
    
    while (true) {
        double nextGuess = (guess + x / guess) / 2.0;
        if (Math.abs(nextGuess - guess) < epsilon) {
            return nextGuess;
        }
        guess = nextGuess;
    }
}
```

---

## Newton's Method Explained

### Mathematical Derivation

To find √x, we solve: `f(y) = y² - x = 0`

Using Newton's formula:
```
y_next = y_current - f(y_current) / f'(y_current)
y_next = y_current - (y² - x) / (2y)
y_next = y_current - y/2 + x/(2y)
y_next = (y + x/y) / 2
```

### Visual Convergence

```
Finding √2:

Iteration 1: guess = 2.0
  next = (2.0 + 2.0/2.0) / 2 = 1.5

Iteration 2: guess = 1.5
  next = (1.5 + 2.0/1.5) / 2 = 1.41666...

Iteration 3: guess = 1.41666...
  next = (1.41666 + 2.0/1.41666) / 2 = 1.41421...

Iteration 4: guess = 1.41421...
  next = 1.41421356... (converged)

Result: 1.41421356 ≈ √2
```

---

## Detailed Walkthrough

### Example: sqrt(16)

**Newton's Method:**
```
Initial: guess = 16

Iteration 1:
  next = (16 + 16/16) / 2 = (16 + 1) / 2 = 8.5
  error = |8.5² - 16| = |72.25 - 16| = 56.25

Iteration 2:
  next = (8.5 + 16/8.5) / 2 = (8.5 + 1.882) / 2 = 5.191
  error = |5.191² - 16| = |26.95 - 16| = 10.95

Iteration 3:
  next = (5.191 + 16/5.191) / 2 = 4.136
  error = |4.136² - 16| = |17.11 - 16| = 1.11

Iteration 4:
  next = (4.136 + 16/4.136) / 2 = 4.002
  error = |4.002² - 16| = 0.016

Iteration 5:
  next = (4.002 + 16/4.002) / 2 = 4.0
  error ≈ 0 (converged)

Result: 4.0
```

---

## Complete Implementation

```java
public class SquareRoot {
    
    private static final double EPSILON = 1e-10;
    
    // Approach 1: Newton's Method (Recommended)
    public double sqrt(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number");
        }
        if (x == 0) return 0;
        
        double guess = x;
        
        while (Math.abs(guess * guess - x) > EPSILON) {
            guess = (guess + x / guess) / 2.0;
        }
        
        return guess;
    }
    
    // Approach 2: Binary Search
    public double sqrtBinarySearch(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number");
        }
        if (x == 0) return 0;
        if (x == 1) return 1;
        
        double left = 0, right = Math.max(1, x);
        
        while (right - left > EPSILON) {
            double mid = left + (right - left) / 2.0;
            double square = mid * mid;
            
            if (Math.abs(square - x) < EPSILON) {
                return mid;
            } else if (square < x) {
                left = mid;
            } else {
                right = mid;
            }
        }
        
        return left;
    }
    
    // Approach 3: With iteration limit
    public double sqrtWithLimit(double x, int maxIterations) {
        if (x < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number");
        }
        if (x == 0) return 0;
        
        double guess = x;
        
        for (int i = 0; i < maxIterations; i++) {
            double nextGuess = (guess + x / guess) / 2.0;
            if (Math.abs(nextGuess - guess) < EPSILON) {
                return nextGuess;
            }
            guess = nextGuess;
        }
        
        return guess;
    }
    
    // Approach 4: Integer square root (for comparison)
    public int sqrtInteger(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number");
        }
        if (x < 2) return x;
        
        int left = 1, right = x / 2;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            long square = (long) mid * mid;
            
            if (square == x) {
                return mid;
            } else if (square < x) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return right;
    }
    
    // Bonus: Nth root
    public double nthRoot(double x, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }
        if (n == 1) return x;
        if (x == 0) return 0;
        
        double guess = x;
        
        while (Math.abs(Math.pow(guess, n) - x) > EPSILON) {
            guess = ((n - 1) * guess + x / Math.pow(guess, n - 1)) / n;
        }
        
        return guess;
    }
}
```

---

## Test Cases

```java
@Test
public void testSquareRoot() {
    SquareRoot solver = new SquareRoot();
    double epsilon = 1e-9;
    
    // Perfect squares
    assertEquals(2.0, solver.sqrt(4.0), epsilon);
    assertEquals(3.0, solver.sqrt(9.0), epsilon);
    assertEquals(10.0, solver.sqrt(100.0), epsilon);
    
    // Non-perfect squares
    assertEquals(1.414213562, solver.sqrt(2.0), epsilon);
    assertEquals(1.732050808, solver.sqrt(3.0), epsilon);
    assertEquals(2.236067977, solver.sqrt(5.0), epsilon);
    
    // Edge cases
    assertEquals(0.0, solver.sqrt(0.0), epsilon);
    assertEquals(1.0, solver.sqrt(1.0), epsilon);
    
    // Decimals less than 1
    assertEquals(0.5, solver.sqrt(0.25), epsilon);
    assertEquals(0.1, solver.sqrt(0.01), epsilon);
    
    // Large numbers
    assertEquals(100.0, solver.sqrt(10000.0), epsilon);
    assertEquals(1000.0, solver.sqrt(1000000.0), epsilon);
    
    // Very small numbers
    assertEquals(0.001, solver.sqrt(0.000001), epsilon);
    
    // Negative number (should throw)
    assertThrows(IllegalArgumentException.class, () -> solver.sqrt(-1.0));
}
```

---

## Convergence Speed Comparison

```
Finding √2 (actual: 1.41421356...)

Newton's Method:
  Iteration 1: 1.5          (error: 0.08578...)
  Iteration 2: 1.41666...   (error: 0.00245...)
  Iteration 3: 1.41421...   (error: 0.00000...)
  → 3 iterations

Binary Search (with epsilon = 1e-10):
  Iteration 1: 1.0          (error: 0.41421...)
  Iteration 2: 1.5          (error: 0.08578...)
  Iteration 3: 1.25         (error: 0.16421...)
  ...
  → ~30 iterations

Winner: Newton's Method (quadratic convergence)
```

---

## Edge Cases

| Input | Output | Notes |
|-------|--------|-------|
| `0.0` | `0.0` | Base case |
| `1.0` | `1.0` | Identity |
| `0.25` | `0.5` | Fraction < 1 |
| `4.0` | `2.0` | Perfect square |
| `2.0` | `1.414...` | Irrational |
| `-1.0` | Exception | Negative |
| `1e-10` | `1e-5` | Very small |
| `1e10` | `1e5` | Very large |

---

## Common Mistakes

1. **Wrong Initial Guess for Small Numbers:**
   ```java
   // WRONG - for x < 1, x is too small as initial guess
   double guess = x;
   
   // BETTER - use max(x, 1) or x/2
   double guess = Math.max(x, 1.0);
   ```

2. **Binary Search Range for x < 1:**
   ```java
   // WRONG - right should be 1, not x
   double left = 0, right = x;
   
   // CORRECT
   double left = 0, right = Math.max(1, x);
   ```

3. **Precision Issues:**
   ```java
   // Use epsilon for comparison, not exact equality
   if (Math.abs(guess * guess - x) < EPSILON)
   ```

4. **Infinite Loop:**
   ```java
   // Add iteration limit as safety
   int maxIter = 100;
   while (condition && maxIter-- > 0)
   ```

---

## Why Newton's Method is Faster

### Convergence Rate

- **Binary Search:** Linear convergence (halves error each iteration)
- **Newton's Method:** Quadratic convergence (squares error each iteration)

```
Error reduction per iteration:

Binary Search:
  e₁ = 0.5
  e₂ = 0.25
  e₃ = 0.125
  e₄ = 0.0625

Newton's Method:
  e₁ = 0.5
  e₂ = 0.0625  (0.5²/4)
  e₃ = 0.00098 (0.0625²/4)
  e₄ ≈ 0       (converged)
```

---

## Handling Special Cases

### Numbers Less Than 1

```java
// For x < 1, sqrt(x) > x
// Example: sqrt(0.25) = 0.5

if (x < 1) {
    // Binary search: [0, 1]
    // Newton: initial guess = 1 or x/2
}
```

### Very Large Numbers

```java
// For very large x, start with x/2
double guess = x / 2.0;
```

### Precision Control

```java
// Adjust epsilon based on requirements
double epsilon = 1e-10; // High precision
double epsilon = 1e-6;  // Standard precision
double epsilon = 1e-3;  // Low precision (faster)
```

---

## Complexity Analysis

| Method | Time | Space | Iterations (√2) |
|--------|------|-------|-----------------|
| Newton's Method | O(log log n) | O(1) | ~3-5 |
| Binary Search | O(log n) | O(1) | ~30-40 |
| Babylonian | O(log log n) | O(1) | ~3-5 |

**Note:** Newton's method has quadratic convergence, making it significantly faster.

---

## Related Problems

- **LeetCode 69:** Sqrt(x) - Integer square root
- **LeetCode 367:** Valid Perfect Square
- **LeetCode 50:** Pow(x, n)
- **Newton's Method:** General root finding

---

## Interview Tips

1. **Start with Newton's Method:**
   - Most efficient
   - Show mathematical understanding

2. **Explain the Formula:**
   - `next = (current + x/current) / 2`
   - Geometric mean interpretation

3. **Handle Edge Cases:**
   - Zero, one
   - Numbers less than 1
   - Negative numbers

4. **Discuss Precision:**
   - Epsilon value choice
   - Iteration limit

5. **Compare Approaches:**
   - Newton vs Binary Search
   - Convergence speed

---

## Real-World Applications

- **Computer Graphics:** Distance calculations, normalization
- **Physics Simulations:** Velocity, acceleration computations
- **Financial Calculations:** Standard deviation, volatility
- **Machine Learning:** Euclidean distance, gradient descent
- **Game Development:** Collision detection, pathfinding

---

## Mathematical Properties

```
Properties of Square Root:
1. √(a × b) = √a × √b
2. √(a / b) = √a / √b
3. (√a)² = a
4. √a² = |a|

Newton's Method Convergence:
- Quadratic convergence near root
- Doubles correct digits each iteration
- Requires good initial guess
```

---

## Optimization Tips

1. **Better Initial Guess:**
   ```java
   // Use bit manipulation for power of 2
   double guess = x / 2.0;
   ```

2. **Early Termination:**
   ```java
   // Check if already close enough
   if (Math.abs(x - 1.0) < EPSILON) return 1.0;
   ```

3. **Iteration Limit:**
   ```java
   // Prevent infinite loops
   int maxIter = 50;
   while (condition && maxIter-- > 0)
   ```

---

## Bonus: Fast Inverse Square Root (Quake III)

```java
// Famous fast inverse square root algorithm
public float fastInverseSqrt(float x) {
    float xhalf = 0.5f * x;
    int i = Float.floatToIntBits(x);
    i = 0x5f3759df - (i >> 1); // Magic constant
    x = Float.intBitsToFloat(i);
    x = x * (1.5f - xhalf * x * x); // Newton iteration
    return x;
}
```

**Note:** Historical interest only; modern CPUs have fast hardware sqrt.

---

## Key Takeaways

✅ Newton's method is optimal - O(log log n) convergence  
✅ Formula: `next = (current + x/current) / 2`  
✅ Handle x < 1 carefully (sqrt > x)  
✅ Use epsilon for floating-point comparison  
✅ Add iteration limit for safety  
✅ Quadratic convergence doubles correct digits  
✅ Binary search is simpler but slower
