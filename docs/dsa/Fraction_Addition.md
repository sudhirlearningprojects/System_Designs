# Fraction Addition

## Problem Statement

Given two fractions as integer arrays, return the result of adding them as a simplified fraction.

**Input:** Two int arrays `[numerator, denominator]`  
**Output:** int array `[numerator, denominator]` in simplified form

**Examples:**
```
addFractions([1, 2], [1, 3]) = [5, 6]     // 1/2 + 1/3 = 3/6 + 2/6 = 5/6
addFractions([1, 4], [1, 4]) = [1, 2]     // 1/4 + 1/4 = 2/4 = 1/2
addFractions([2, 3], [1, 6]) = [5, 6]     // 2/3 + 1/6 = 4/6 + 1/6 = 5/6
addFractions([1, 2], [-1, 2]) = [0, 1]    // 1/2 + (-1/2) = 0
```

---

## Solution Approach

### Mathematical Formula

```
a/b + c/d = (a×d + b×c) / (b×d)

Then simplify by dividing by GCD(numerator, denominator)
```

### Algorithm Steps

1. Calculate new numerator: `a×d + b×c`
2. Calculate new denominator: `b×d`
3. Find GCD of numerator and denominator
4. Divide both by GCD to simplify

---

## Complete Solution

```java
public int[] addFractions(int[] frac1, int[] frac2) {
    int a = frac1[0], b = frac1[1];
    int c = frac2[0], d = frac2[1];
    
    // Calculate numerator and denominator
    int numerator = a * d + b * c;
    int denominator = b * d;
    
    // Simplify by GCD
    int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
    
    return new int[]{numerator / gcd, denominator / gcd};
}

private int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
}
```

---

## Detailed Walkthrough

### Example 1: 1/2 + 1/3

```
Step 1: Extract values
  a = 1, b = 2
  c = 1, d = 3

Step 2: Calculate numerator
  numerator = a×d + b×c
  numerator = 1×3 + 2×1 = 3 + 2 = 5

Step 3: Calculate denominator
  denominator = b×d
  denominator = 2×3 = 6

Step 4: Find GCD
  GCD(5, 6) = 1

Step 5: Simplify
  5/1 = 5
  6/1 = 6

Result: [5, 6] → 5/6
```

### Example 2: 1/4 + 1/4

```
Step 1: Extract values
  a = 1, b = 4
  c = 1, d = 4

Step 2: Calculate numerator
  numerator = 1×4 + 4×1 = 4 + 4 = 8

Step 3: Calculate denominator
  denominator = 4×4 = 16

Step 4: Find GCD
  GCD(8, 16) = 8

Step 5: Simplify
  8/8 = 1
  16/8 = 2

Result: [1, 2] → 1/2
```

### Example 3: 2/3 + 1/6

```
Step 1: Extract values
  a = 2, b = 3
  c = 1, d = 6

Step 2: Calculate numerator
  numerator = 2×6 + 3×1 = 12 + 3 = 15

Step 3: Calculate denominator
  denominator = 3×6 = 18

Step 4: Find GCD
  GCD(15, 18) = 3

Step 5: Simplify
  15/3 = 5
  18/3 = 6

Result: [5, 6] → 5/6
```

---

## Complete Implementation with Edge Cases

```java
public class FractionAddition {
    
    public int[] addFractions(int[] frac1, int[] frac2) {
        if (frac1 == null || frac2 == null) {
            throw new IllegalArgumentException("Fractions cannot be null");
        }
        if (frac1[1] == 0 || frac2[1] == 0) {
            throw new IllegalArgumentException("Denominator cannot be zero");
        }
        
        int a = frac1[0], b = frac1[1];
        int c = frac2[0], d = frac2[1];
        
        // Calculate result
        int numerator = a * d + b * c;
        int denominator = b * d;
        
        // Handle zero result
        if (numerator == 0) {
            return new int[]{0, 1};
        }
        
        // Simplify
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        numerator /= gcd;
        denominator /= gcd;
        
        // Keep denominator positive
        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }
        
        return new int[]{numerator, denominator};
    }
    
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
    
    // Alternative: Iterative GCD
    private int gcdIterative(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    
    // Bonus: Subtract fractions
    public int[] subtractFractions(int[] frac1, int[] frac2) {
        return addFractions(frac1, new int[]{-frac2[0], frac2[1]});
    }
    
    // Bonus: Multiply fractions
    public int[] multiplyFractions(int[] frac1, int[] frac2) {
        int numerator = frac1[0] * frac2[0];
        int denominator = frac1[1] * frac2[1];
        
        if (numerator == 0) return new int[]{0, 1};
        
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        return new int[]{numerator / gcd, denominator / gcd};
    }
    
    // Bonus: Divide fractions
    public int[] divideFractions(int[] frac1, int[] frac2) {
        if (frac2[0] == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        // Multiply by reciprocal
        return multiplyFractions(frac1, new int[]{frac2[1], frac2[0]});
    }
}
```

---

## Test Cases

```java
@Test
public void testAddFractions() {
    FractionAddition solver = new FractionAddition();
    
    // Basic addition
    assertArrayEquals(new int[]{5, 6}, 
        solver.addFractions(new int[]{1, 2}, new int[]{1, 3}));
    
    // Same denominator
    assertArrayEquals(new int[]{1, 2}, 
        solver.addFractions(new int[]{1, 4}, new int[]{1, 4}));
    
    // Needs simplification
    assertArrayEquals(new int[]{5, 6}, 
        solver.addFractions(new int[]{2, 3}, new int[]{1, 6}));
    
    // Result is zero
    assertArrayEquals(new int[]{0, 1}, 
        solver.addFractions(new int[]{1, 2}, new int[]{-1, 2}));
    
    // Negative fractions
    assertArrayEquals(new int[]{-1, 6}, 
        solver.addFractions(new int[]{1, 3}, new int[]{-1, 2}));
    
    // Whole numbers (denominator = 1)
    assertArrayEquals(new int[]{5, 1}, 
        solver.addFractions(new int[]{2, 1}, new int[]{3, 1}));
    
    // Zero + fraction
    assertArrayEquals(new int[]{1, 2}, 
        solver.addFractions(new int[]{0, 1}, new int[]{1, 2}));
    
    // Large numbers
    assertArrayEquals(new int[]{1, 1}, 
        solver.addFractions(new int[]{1, 2}, new int[]{1, 2}));
}
```

---

## GCD Algorithm Explanation

### Euclidean Algorithm

```
GCD(48, 18):
  48 = 18 × 2 + 12
  18 = 12 × 1 + 6
  12 = 6 × 2 + 0
  
  GCD = 6
```

### Recursive Implementation

```java
int gcd(int a, int b) {
    if (b == 0) return a;
    return gcd(b, a % b);
}
```

### Iterative Implementation

```java
int gcd(int a, int b) {
    while (b != 0) {
        int temp = b;
        b = a % b;
        a = temp;
    }
    return a;
}
```

---

## Edge Cases

| Case | Input | Output | Notes |
|------|-------|--------|-------|
| Zero result | `[1,2], [-1,2]` | `[0,1]` | Simplify to 0/1 |
| Negative | `[1,2], [-1,3]` | `[1,6]` | Handle signs |
| Whole numbers | `[2,1], [3,1]` | `[5,1]` | Denominator = 1 |
| Same fraction | `[1,2], [1,2]` | `[1,1]` | Simplifies to 1 |
| Already simplified | `[1,2], [1,3]` | `[5,6]` | GCD = 1 |

---

## Common Mistakes

1. **Forgetting to Simplify:**
   ```java
   // WRONG - returns [10, 12] instead of [5, 6]
   return new int[]{numerator, denominator};
   
   // CORRECT
   int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
   return new int[]{numerator / gcd, denominator / gcd};
   ```

2. **Not Handling Negative Denominators:**
   ```java
   // Keep denominator positive
   if (denominator < 0) {
       numerator = -numerator;
       denominator = -denominator;
   }
   ```

3. **Integer Overflow:**
   ```java
   // For large numbers, use long
   long numerator = (long)a * d + (long)b * c;
   long denominator = (long)b * d;
   ```

4. **Not Using Absolute Value in GCD:**
   ```java
   // WRONG - GCD fails with negative numbers
   int gcd = gcd(numerator, denominator);
   
   // CORRECT
   int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
   ```

---

## Visual Representation

```
1/2 + 1/3 = ?

Step 1: Find common denominator
  1/2 = 3/6
  1/3 = 2/6

Step 2: Add numerators
  3/6 + 2/6 = 5/6

Step 3: Simplify (if needed)
  GCD(5, 6) = 1
  Already simplified: 5/6
```

---

## Optimization: Using LCM

Instead of `b×d`, use LCM for smaller intermediate values:

```java
public int[] addFractionsOptimized(int[] frac1, int[] frac2) {
    int a = frac1[0], b = frac1[1];
    int c = frac2[0], d = frac2[1];
    
    // Find LCM of denominators
    int lcm = (b * d) / gcd(b, d);
    
    // Adjust numerators
    int numerator = a * (lcm / b) + c * (lcm / d);
    int denominator = lcm;
    
    // Simplify
    if (numerator == 0) return new int[]{0, 1};
    
    int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
    return new int[]{numerator / gcd, denominator / gcd};
}
```

**Benefit:** Reduces risk of integer overflow

---

## Complexity Analysis

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Addition | O(log(min(a,b))) | O(1) | GCD dominates |
| GCD (Euclidean) | O(log(min(a,b))) | O(1) | Iterative |
| GCD (Recursive) | O(log(min(a,b))) | O(log(min(a,b))) | Stack space |

---

## Related Problems

- **LeetCode 592:** Fraction Addition and Subtraction
- **LeetCode 166:** Fraction to Recurring Decimal
- **LeetCode 972:** Equal Rational Numbers

---

## Interview Tips

1. **Clarify Requirements:**
   - Handle negative fractions?
   - Simplify result?
   - Input validation needed?

2. **Explain Formula:**
   - a/b + c/d = (a×d + b×c)/(b×d)
   - Mention common denominator

3. **Discuss GCD:**
   - Euclidean algorithm
   - O(log n) complexity

4. **Walk Through Example:**
   - Use 1/2 + 1/3
   - Show simplification

5. **Handle Edge Cases:**
   - Zero result
   - Negative fractions
   - Integer overflow

---

## Real-World Applications

- **Financial Calculations:** Stock splits, interest rates
- **Cooking/Recipes:** Ingredient measurements
- **Engineering:** Gear ratios, scaling
- **Music Theory:** Note durations, time signatures
- **Computer Graphics:** Aspect ratios

---

## Mathematical Properties

```
Properties of Fraction Addition:
1. Commutative: a/b + c/d = c/d + a/b
2. Associative: (a/b + c/d) + e/f = a/b + (c/d + e/f)
3. Identity: a/b + 0/1 = a/b
4. Inverse: a/b + (-a/b) = 0/1

GCD Properties:
1. GCD(a, 0) = a
2. GCD(a, b) = GCD(b, a % b)
3. GCD(a, b) × LCM(a, b) = a × b
```

---

## Bonus: Fraction Class

```java
public class Fraction {
    private int numerator;
    private int denominator;
    
    public Fraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator cannot be zero");
        }
        
        int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
        this.numerator = numerator / gcd;
        this.denominator = denominator / gcd;
        
        // Keep denominator positive
        if (this.denominator < 0) {
            this.numerator = -this.numerator;
            this.denominator = -this.denominator;
        }
    }
    
    public Fraction add(Fraction other) {
        int num = this.numerator * other.denominator + 
                  this.denominator * other.numerator;
        int den = this.denominator * other.denominator;
        return new Fraction(num, den);
    }
    
    public Fraction subtract(Fraction other) {
        return add(new Fraction(-other.numerator, other.denominator));
    }
    
    public Fraction multiply(Fraction other) {
        return new Fraction(
            this.numerator * other.numerator,
            this.denominator * other.denominator
        );
    }
    
    public Fraction divide(Fraction other) {
        return multiply(new Fraction(other.denominator, other.numerator));
    }
    
    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
    
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
```

---

## Key Takeaways

✅ Formula: (a×d + b×c) / (b×d)  
✅ Always simplify using GCD  
✅ GCD uses Euclidean algorithm - O(log n)  
✅ Handle zero result as [0, 1]  
✅ Keep denominator positive  
✅ Use Math.abs() for GCD with negatives  
✅ Consider LCM to reduce overflow risk
