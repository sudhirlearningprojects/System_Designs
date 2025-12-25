# Check if Number is Power of Another Number

## Problem Statement

Check if a given number `n` is a power of another number `base`. In other words, check if there exists an integer `k` such that `base^k = n`.

**Input:** 
- n: number to check
- base: base number

**Output:** true if n is a power of base, false otherwise

**Examples:**
```
Input: n = 8, base = 2
Output: true
Explanation: 2^3 = 8

Input: n = 16, base = 4
Output: true
Explanation: 4^2 = 16

Input: n = 27, base = 3
Output: true
Explanation: 3^3 = 27

Input: n = 10, base = 2
Output: false
Explanation: No integer k where 2^k = 10

Input: n = 1, base = 5
Output: true
Explanation: 5^0 = 1
```

---

## Solution Approaches

### Approach 1: Division (Optimal for General Case)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static boolean isPower(int n, int base) {
    if (n < 1 || base < 2) return false;
    if (n == 1) return true;
    
    while (n % base == 0) {
        n /= base;
    }
    
    return n == 1;
}
```

---

### Approach 2: Logarithm

**Time Complexity:** O(1)  
**Space Complexity:** O(1)

```java
public static boolean isPowerLog(int n, int base) {
    if (n < 1 || base < 2) return false;
    if (n == 1) return true;
    
    double result = Math.log(n) / Math.log(base);
    return Math.abs(result - Math.round(result)) < 1e-10;
}
```

---

### Approach 3: Multiplication (Avoid Overflow)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static boolean isPowerMult(int n, int base) {
    if (n < 1 || base < 2) return false;
    if (n == 1) return true;
    
    long power = base;
    while (power < n) {
        power *= base;
    }
    
    return power == n;
}
```

---

### Approach 4: Recursion

**Time Complexity:** O(log n)  
**Space Complexity:** O(log n)

```java
public static boolean isPowerRecursive(int n, int base) {
    if (n < 1 || base < 2) return false;
    if (n == 1) return true;
    if (n % base != 0) return false;
    
    return isPowerRecursive(n / base, base);
}
```

---

### Approach 5: Bit Manipulation (Power of 2 Only)

**Time Complexity:** O(1)  
**Space Complexity:** O(1)

```java
public static boolean isPowerOfTwo(int n) {
    return n > 0 && (n & (n - 1)) == 0;
}
```

---

## Algorithm Walkthrough

### Example: n = 8, base = 2

**Division Approach:**

```
Initial: n = 8, base = 2

Step 1: 8 % 2 == 0? Yes
  n = 8 / 2 = 4

Step 2: 4 % 2 == 0? Yes
  n = 4 / 2 = 2

Step 3: 2 % 2 == 0? Yes
  n = 2 / 2 = 1

Step 4: n == 1? Yes

Result: true (2^3 = 8)
```

### Example: n = 10, base = 2

```
Initial: n = 10, base = 2

Step 1: 10 % 2 == 0? Yes
  n = 10 / 2 = 5

Step 2: 5 % 2 == 0? No
  Exit loop

Step 3: n == 1? No (n = 5)

Result: false
```

### Example: n = 27, base = 3

```
Initial: n = 27, base = 3

Step 1: 27 % 3 == 0? Yes
  n = 27 / 3 = 9

Step 2: 9 % 3 == 0? Yes
  n = 9 / 3 = 3

Step 3: 3 % 3 == 0? Yes
  n = 3 / 3 = 1

Step 4: n == 1? Yes

Result: true (3^3 = 27)
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Division (Optimal)
    public static boolean isPower(int n, int base) {
        if (n < 1 || base < 2) return false;
        if (n == 1) return true;
        
        while (n % base == 0) {
            n /= base;
        }
        
        return n == 1;
    }
    
    // Approach 2: Logarithm
    public static boolean isPowerLog(int n, int base) {
        if (n < 1 || base < 2) return false;
        if (n == 1) return true;
        
        double result = Math.log(n) / Math.log(base);
        return Math.abs(result - Math.round(result)) < 1e-10;
    }
    
    // Approach 3: Multiplication
    public static boolean isPowerMult(int n, int base) {
        if (n < 1 || base < 2) return false;
        if (n == 1) return true;
        
        long power = base;
        while (power < n) {
            power *= base;
        }
        
        return power == n;
    }
    
    // Approach 4: Recursion
    public static boolean isPowerRecursive(int n, int base) {
        if (n < 1 || base < 2) return false;
        if (n == 1) return true;
        if (n % base != 0) return false;
        
        return isPowerRecursive(n / base, base);
    }
    
    // Approach 5: Power of 2 (Bit manipulation)
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    // Find the exponent k where base^k = n
    public static int findExponent(int n, int base) {
        if (n < 1 || base < 2) return -1;
        if (n == 1) return 0;
        
        int k = 0;
        while (n % base == 0) {
            n /= base;
            k++;
        }
        
        return n == 1 ? k : -1;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Power of 2
        if (!isPower(8, 2)) return false;
        
        // Test 2: Power of 4
        if (!isPower(16, 4)) return false;
        
        // Test 3: Power of 3
        if (!isPower(27, 3)) return false;
        
        // Test 4: Not a power
        if (isPower(10, 2)) return false;
        
        // Test 5: n = 1
        if (!isPower(1, 5)) return false;
        
        // Test 6: n = base
        if (!isPower(5, 5)) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[][] tests = {{8, 2}, {16, 4}, {27, 3}, {10, 2}, {1, 5}};
        
        for (int[] test : tests) {
            int n = test[0];
            int base = test[1];
            boolean result = isPower(n, base);
            int exp = findExponent(n, base);
            
            System.out.println("n=" + n + ", base=" + base + 
                             " → " + result + 
                             (exp >= 0 ? " (exponent=" + exp + ")" : ""));
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testIsPower() {
    // Test 1: Powers of 2
    assertTrue(isPower(1, 2));
    assertTrue(isPower(2, 2));
    assertTrue(isPower(4, 2));
    assertTrue(isPower(8, 2));
    assertTrue(isPower(16, 2));
    assertTrue(isPower(1024, 2));
    
    // Test 2: Powers of 3
    assertTrue(isPower(1, 3));
    assertTrue(isPower(3, 3));
    assertTrue(isPower(9, 3));
    assertTrue(isPower(27, 3));
    assertTrue(isPower(81, 3));
    
    // Test 3: Powers of 4
    assertTrue(isPower(1, 4));
    assertTrue(isPower(4, 4));
    assertTrue(isPower(16, 4));
    assertTrue(isPower(64, 4));
    
    // Test 4: Not powers
    assertFalse(isPower(10, 2));
    assertFalse(isPower(15, 3));
    assertFalse(isPower(20, 4));
    assertFalse(isPower(100, 7));
    
    // Test 5: Edge cases
    assertFalse(isPower(0, 2));
    assertFalse(isPower(-8, 2));
    assertFalse(isPower(8, 1));
    assertFalse(isPower(8, 0));
    
    // Test 6: n = base
    assertTrue(isPower(5, 5));
    assertTrue(isPower(10, 10));
}
```

---

## Visual Representation

### Division Process

```
n = 64, base = 4

64 ÷ 4 = 16  (64 = 4^1 × 16)
16 ÷ 4 = 4   (64 = 4^2 × 4)
4 ÷ 4 = 1    (64 = 4^3 × 1)

Result: 1 → true (4^3 = 64)

n = 10, base = 2

10 ÷ 2 = 5   (10 = 2^1 × 5)
5 % 2 ≠ 0    (cannot divide further)

Result: 5 ≠ 1 → false
```

### Power of 2 Bit Pattern

```
Powers of 2 have single bit set:
1   = 0001
2   = 0010
4   = 0100
8   = 1000
16  = 10000

n & (n-1) removes rightmost bit:
8   = 1000
7   = 0111
8&7 = 0000 → Power of 2 ✓

10  = 1010
9   = 1001
10&9= 1000 → Not power of 2 ✗
```

---

## Edge Cases

1. **n = 0:** false (0 is not a power)
2. **n = 1:** true (any base^0 = 1)
3. **n < 0:** false (negative numbers)
4. **base = 1:** false (1^k = 1 for all k)
5. **base < 2:** false (invalid base)
6. **n = base:** true (base^1 = base)
7. **Large n:** Handle overflow
8. **Floating point precision:** Logarithm approach

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Division | O(log n) | O(1) | **Best general solution** |
| Logarithm | O(1) | O(1) | Precision issues |
| Multiplication | O(log n) | O(1) | Avoids division |
| Recursion | O(log n) | O(log n) | Stack space |
| Bit (Power of 2) | O(1) | O(1) | **Optimal for base=2** |

**Why O(log n)?**
- Each iteration divides n by base
- Number of iterations = log_base(n)

---

## Related Problems

1. **Power of Two** - Check if power of 2
2. **Power of Three** - Check if power of 3
3. **Power of Four** - Check if power of 4
4. **Perfect Square** - Check if perfect square
5. **Perfect Cube** - Check if perfect cube
6. **Nth Root** - Find nth root

---

## Interview Tips

### Clarification Questions
1. Can n be negative? (Usually no)
2. Can n be zero? (Usually no)
3. What's the range of base? (Usually base ≥ 2)
4. What about n = 1? (Yes, any base^0 = 1)
5. Handle overflow? (Use long for multiplication)

### Approach Explanation
1. "Repeatedly divide n by base"
2. "If we can divide until n becomes 1, it's a power"
3. "If remainder at any step, not a power"
4. "O(log n) time, O(1) space"

### Common Mistakes
- Not handling n = 1 case
- Not checking base < 2
- Integer overflow in multiplication approach
- Floating point precision in logarithm approach
- Not handling negative numbers

### Why Division is Better Than Multiplication?

```java
// Division: No overflow risk
while (n % base == 0) {
    n /= base; // n gets smaller
}

// Multiplication: Overflow risk
long power = base;
while (power < n) {
    power *= base; // power gets larger, can overflow
}
```

---

## Real-World Applications

1. **Computer Science** - Binary/hexadecimal conversions
2. **Mathematics** - Number theory problems
3. **Cryptography** - Prime power checks
4. **Data Structures** - Tree height calculations
5. **Algorithms** - Complexity analysis
6. **Physics** - Exponential growth/decay

---

## Key Takeaways

1. **Division Approach:** Most reliable for general case
2. **Base Cases:** n=1 is always true, base<2 is invalid
3. **Time Complexity:** O(log n) for iterative approaches
4. **Space Complexity:** O(1) for iterative, O(log n) for recursive
5. **Power of 2:** Use bit manipulation for O(1)
6. **Logarithm:** Fast but precision issues
7. **Edge Cases:** Handle n≤0, base<2, overflow

---

## Additional Notes

### Why n = 1 is Always True

```
For any base b ≥ 2:
  b^0 = 1

Therefore, 1 is a power of any valid base.
```

### Logarithm Precision Issues

```java
// Problem: Floating point precision
n = 243, base = 3
log(243) / log(3) = 5.0000000001 (due to precision)
round(5.0000000001) = 5
Correct!

n = 244, base = 3
log(244) / log(3) = 5.0062...
round(5.0062) = 5
Wrong! 3^5 = 243 ≠ 244

// Solution: Check tolerance
Math.abs(result - Math.round(result)) < 1e-10
```

### Power of 2 Bit Trick Explained

```
n & (n-1) removes rightmost set bit

Power of 2: Only one bit set
  8 = 1000
  7 = 0111
  8 & 7 = 0000 ✓

Not power of 2: Multiple bits set
  10 = 1010
  9  = 1001
  10 & 9 = 1000 ≠ 0 ✗

Check: n > 0 && (n & (n-1)) == 0
```

### Finding the Exponent

```java
public static int findExponent(int n, int base) {
    if (n < 1 || base < 2) return -1;
    if (n == 1) return 0;
    
    int k = 0;
    while (n % base == 0) {
        n /= base;
        k++;
    }
    
    return n == 1 ? k : -1;
}

// Example: n=64, base=4
// Returns: 3 (because 4^3 = 64)
```

### Power of 3 Without Loops

```java
// For 32-bit integer, max power of 3 is 3^19 = 1162261467
public static boolean isPowerOfThree(int n) {
    return n > 0 && 1162261467 % n == 0;
}

// Works because:
// If n is power of 3, then 3^19 % n == 0
// If n is not power of 3, then 3^19 % n != 0
```

### Power of 4 Check

```java
// Power of 4 is also power of 2
// AND has even number of trailing zeros
public static boolean isPowerOfFour(int n) {
    return n > 0 && 
           (n & (n - 1)) == 0 && 
           (n & 0x55555555) != 0;
}

// 0x55555555 = 01010101010101010101010101010101
// Checks if set bit is at even position
```

### Handling Negative Bases

```java
// If base can be negative
public static boolean isPowerNegBase(int n, int base) {
    if (base == 0 || base == 1 || base == -1) return false;
    if (n == 1) return true;
    
    if (base < 0) {
        // Negative base: alternating signs
        // More complex logic needed
    }
    
    // Standard logic for positive base
}
```

### Optimization: Early Exit

```java
// If n < base, can only be true if n == 1
if (n < base) {
    return n == 1;
}
```

### Comparison: Division vs Logarithm

```
Division:
  + No precision issues
  + Works for all bases
  + Reliable
  - O(log n) time

Logarithm:
  + O(1) time
  - Floating point precision
  - May give wrong results
  - Not recommended for production
```

### Perfect Square Check

```java
// Special case: base = any, exponent = 2
public static boolean isPerfectSquare(int n) {
    if (n < 0) return false;
    int sqrt = (int) Math.sqrt(n);
    return sqrt * sqrt == n;
}
```

### Perfect Cube Check

```java
// Special case: base = any, exponent = 3
public static boolean isPerfectCube(int n) {
    int cube = (int) Math.round(Math.cbrt(n));
    return cube * cube * cube == n;
}
```
