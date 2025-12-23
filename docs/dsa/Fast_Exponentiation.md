# Power Function - Fast Exponentiation

## Problem Statement

Given a base and an integer exponent, compute the value of base raised to the power of exponent.

**Challenge:** Implement a solution faster than O(exp)

**Examples:**
```
pow(2, 10) = 1024
pow(3, 4) = 81
pow(2, -2) = 0.25
pow(5, 0) = 1
```

---

## Solution Approaches

### Approach 1: Naive Iteration (Baseline)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public double pow(double base, int exp) {
    if (exp == 0) return 1;
    
    double result = 1;
    int absExp = Math.abs(exp);
    
    for (int i = 0; i < absExp; i++) {
        result *= base;
    }
    
    return exp < 0 ? 1 / result : result;
}
```

**Problem:** Too slow for large exponents (e.g., 2^1000)

---

### Approach 2: Fast Exponentiation (Binary Exponentiation) - OPTIMAL

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public double pow(double base, int exp) {
    if (exp == 0) return 1;
    
    long absExp = Math.abs((long) exp);
    double result = 1;
    double current = base;
    
    while (absExp > 0) {
        if ((absExp & 1) == 1) {
            result *= current;
        }
        current *= current;
        absExp >>= 1;
    }
    
    return exp < 0 ? 1 / result : result;
}
```

**Key Insight:**
```
2^10 = (2^5)^2
2^5 = 2 × (2^2)^2
2^2 = (2^1)^2
2^1 = 2 × 2^0
```

---

### Approach 3: Recursive Binary Exponentiation

**Time Complexity:** O(log n)  
**Space Complexity:** O(log n) - recursion stack

```java
public double pow(double base, int exp) {
    if (exp == 0) return 1;
    
    long absExp = Math.abs((long) exp);
    double result = powHelper(base, absExp);
    
    return exp < 0 ? 1 / result : result;
}

private double powHelper(double base, long exp) {
    if (exp == 0) return 1;
    if (exp == 1) return base;
    
    double half = powHelper(base, exp / 2);
    
    if (exp % 2 == 0) {
        return half * half;
    } else {
        return half * half * base;
    }
}
```

---

## Algorithm Explanation: Binary Exponentiation

### Mathematical Foundation

Any exponent can be represented in binary:
```
10 = 1010₂ = 8 + 2 = 2³ + 2¹

Therefore:
2^10 = 2^(8+2) = 2^8 × 2^2
```

### Step-by-Step Example: 2^10

```
exp = 10 (binary: 1010)

Iteration 1: exp=10 (1010)
  - Bit 0 = 0 → skip
  - current = 2
  - result = 1
  - current = 2² = 4
  - exp = 5 (101)

Iteration 2: exp=5 (101)
  - Bit 0 = 1 → result *= current
  - result = 1 × 4 = 4
  - current = 4² = 16
  - exp = 2 (10)

Iteration 3: exp=2 (10)
  - Bit 0 = 0 → skip
  - result = 4
  - current = 16² = 256
  - exp = 1 (1)

Iteration 4: exp=1 (1)
  - Bit 0 = 1 → result *= current
  - result = 4 × 256 = 1024
  - current = 256² = 65536
  - exp = 0

Final: 1024 ✓
```

### Why It's Fast

```
Naive:    2 × 2 × 2 × 2 × 2 × 2 × 2 × 2 × 2 × 2 = 10 multiplications
Optimized: 2² → 4² → 16² → 256 × 4 = 4 multiplications
```

---

## Complete Implementation with Edge Cases

```java
public class FastPower {
    
    // Iterative - OPTIMAL
    public double pow(double base, int exp) {
        if (exp == 0) return 1;
        if (base == 0) return 0;
        if (base == 1) return 1;
        if (base == -1) return exp % 2 == 0 ? 1 : -1;
        
        long absExp = Math.abs((long) exp);
        double result = 1;
        double current = base;
        
        while (absExp > 0) {
            if ((absExp & 1) == 1) {
                result *= current;
            }
            current *= current;
            absExp >>= 1;
        }
        
        return exp < 0 ? 1 / result : result;
    }
    
    // Recursive
    public double powRecursive(double base, int exp) {
        if (exp == 0) return 1;
        if (base == 0) return 0;
        
        long absExp = Math.abs((long) exp);
        double result = powHelper(base, absExp);
        
        return exp < 0 ? 1 / result : result;
    }
    
    private double powHelper(double base, long exp) {
        if (exp == 0) return 1;
        if (exp == 1) return base;
        
        double half = powHelper(base, exp / 2);
        return (exp % 2 == 0) ? half * half : half * half * base;
    }
}
```

---

## Handling Integer Overflow (LeetCode 50)

```java
public double myPow(double x, int n) {
    // Handle Integer.MIN_VALUE overflow
    long exp = n;
    if (exp < 0) {
        x = 1 / x;
        exp = -exp;
    }
    
    double result = 1;
    double current = x;
    
    while (exp > 0) {
        if ((exp & 1) == 1) {
            result *= current;
        }
        current *= current;
        exp >>= 1;
    }
    
    return result;
}
```

**Critical Edge Case:**
```java
// Integer.MIN_VALUE = -2147483648
// Math.abs(Integer.MIN_VALUE) = -2147483648 (overflow!)
// Solution: Use long type
```

---

## Test Cases

```java
@Test
public void testFastPower() {
    FastPower solver = new FastPower();
    
    // Basic cases
    assertEquals(1024.0, solver.pow(2, 10), 0.00001);
    assertEquals(81.0, solver.pow(3, 4), 0.00001);
    
    // Edge cases
    assertEquals(1.0, solver.pow(5, 0), 0.00001);      // Any^0 = 1
    assertEquals(0.0, solver.pow(0, 5), 0.00001);      // 0^n = 0
    assertEquals(1.0, solver.pow(1, 1000), 0.00001);   // 1^n = 1
    
    // Negative exponents
    assertEquals(0.25, solver.pow(2, -2), 0.00001);    // 2^-2 = 1/4
    assertEquals(0.125, solver.pow(2, -3), 0.00001);   // 2^-3 = 1/8
    
    // Negative base
    assertEquals(1.0, solver.pow(-1, 100), 0.00001);   // (-1)^even = 1
    assertEquals(-1.0, solver.pow(-1, 101), 0.00001);  // (-1)^odd = -1
    
    // Large exponents
    assertTrue(solver.pow(2, 30) > 1_000_000_000);
    
    // Integer.MIN_VALUE edge case
    assertEquals(1.0, solver.pow(1, Integer.MIN_VALUE), 0.00001);
}
```

---

## Complexity Analysis

| Approach | Time | Space | Multiplications for 2^1000 |
|----------|------|-------|----------------------------|
| Naive | O(n) | O(1) | 1000 |
| Binary Exponentiation (Iterative) | O(log n) | O(1) | 10 |
| Binary Exponentiation (Recursive) | O(log n) | O(log n) | 10 |

**Speed Improvement:**
- For exp = 1000: 1000 → 10 operations (100x faster)
- For exp = 1,000,000: 1,000,000 → 20 operations (50,000x faster)

---

## Bit Manipulation Tricks

```java
// Check if odd
(exp & 1) == 1    // Instead of exp % 2 == 1

// Divide by 2
exp >>= 1         // Instead of exp /= 2

// Why? Bit operations are faster than arithmetic
```

---

## Visual Representation

```
Computing 2^13 (binary: 1101)

13 = 8 + 4 + 1 = 2³ + 2² + 2⁰

2^13 = 2^8 × 2^4 × 2^1

Steps:
1. 2^1 = 2
2. 2^2 = 4
3. 2^4 = 16      ← use (bit 0 set)
4. 2^8 = 256     ← use (bit 3 set)

Result = 2 × 16 × 256 = 8192
```

---

## Common Mistakes

1. **Integer Overflow:**
   ```java
   // WRONG
   int absExp = Math.abs(exp);  // Fails for Integer.MIN_VALUE
   
   // CORRECT
   long absExp = Math.abs((long) exp);
   ```

2. **Negative Exponent Handling:**
   ```java
   // WRONG
   return 1 / pow(base, -exp);  // Infinite recursion
   
   // CORRECT
   return 1 / pow(base, Math.abs(exp));
   ```

3. **Precision Issues:**
   ```java
   // Use delta for floating point comparison
   assertEquals(expected, actual, 0.00001);
   ```

---

## Related Problems

- **LeetCode 50:** Pow(x, n)
- **LeetCode 372:** Super Pow
- **Matrix Exponentiation:** Computing Fibonacci in O(log n)
- **Modular Exponentiation:** (base^exp) % mod

---

## Interview Tips

1. **Start with naive O(n) solution** to show understanding
2. **Mention binary exponentiation** as optimization
3. **Handle edge cases:** exp=0, base=0, negative exponents
4. **Discuss Integer.MIN_VALUE overflow** issue
5. **Explain bit manipulation** for bonus points

---

## Real-World Applications

- **Cryptography:** RSA encryption (modular exponentiation)
- **Computer Graphics:** Matrix transformations
- **Scientific Computing:** Large number calculations
- **Competitive Programming:** Fast computation under time limits

---

## Key Takeaways

✅ Binary exponentiation reduces O(n) to O(log n)  
✅ Works by representing exponent in binary  
✅ Iterative approach is more space-efficient  
✅ Handle Integer.MIN_VALUE with long type  
✅ Essential algorithm for competitive programming
