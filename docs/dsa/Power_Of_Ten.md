# Check if Number is Power of 10

## Problem Statement

Determine if a given number is a power of 10.

**Input:** Integer or long  
**Output:** Boolean (true if power of 10, false otherwise)

**Examples:**
```
isPowerOfTen(1)      → true   (10^0)
isPowerOfTen(10)     → true   (10^1)
isPowerOfTen(100)    → true   (10^2)
isPowerOfTen(1000)   → true   (10^3)
isPowerOfTen(50)     → false
isPowerOfTen(0)      → false
isPowerOfTen(-10)    → false
```

---

## Solution Approaches

### Approach 1: Division by 10 (Iterative)

**Time Complexity:** O(log n)  
**Space Complexity:** O(1)

```java
public static boolean isPowerOfTen(int n) {
    if (n <= 0) return false;
    
    while (n % 10 == 0) {
        n /= 10;
    }
    
    return n == 1;
}
```

---

### Approach 2: Logarithm

**Time Complexity:** O(1)  
**Space Complexity:** O(1)

```java
public static boolean isPowerOfTen(int n) {
    if (n <= 0) return false;
    
    double log = Math.log10(n);
    return log == Math.floor(log);
}
```

---

### Approach 3: String Check (Simple)

**Time Complexity:** O(log n)  
**Space Complexity:** O(log n)

```java
public static boolean isPowerOfTen(int n) {
    if (n <= 0) return false;
    
    String str = String.valueOf(n);
    return str.charAt(0) == '1' && 
           str.substring(1).matches("0+");
}
```

---

### Approach 4: Mathematical Property

**Time Complexity:** O(1)  
**Space Complexity:** O(1)

```java
public static boolean isPowerOfTen(long n) {
    if (n <= 0) return false;
    
    // Power of 10 must be power of 2 AND power of 5
    return isPowerOfTwo(n) && isPowerOfFive(n);
}

private static boolean isPowerOfTwo(long n) {
    return n > 0 && (n & (n - 1)) == 0;
}

private static boolean isPowerOfFive(long n) {
    if (n <= 0) return false;
    while (n % 5 == 0) n /= 5;
    return n == 1;
}
```

---

## Algorithm Walkthrough

### Example 1: n = 1000

**Division Approach:**
```
n = 1000
1000 % 10 == 0 → n = 100
100 % 10 == 0  → n = 10
10 % 10 == 0   → n = 1
n == 1 → true
```

**Logarithm Approach:**
```
log10(1000) = 3.0
floor(3.0) = 3.0
3.0 == 3.0 → true
```

**String Approach:**
```
str = "1000"
First char = '1' ✓
Rest = "000" matches "0+" ✓
Result: true
```

### Example 2: n = 50

**Division Approach:**
```
n = 50
50 % 10 == 0 → n = 5
5 % 10 != 0  → exit loop
n == 1? No (n = 5)
Result: false
```

---

## Complete Implementation

```java
public class PowerOfTen {
    
    // Approach 1: Division (Recommended)
    public static boolean isPowerOfTen(int n) {
        if (n <= 0) return false;
        
        while (n % 10 == 0) {
            n /= 10;
        }
        
        return n == 1;
    }
    
    // Approach 2: Logarithm
    public static boolean isPowerOfTenLog(int n) {
        if (n <= 0) return false;
        
        double log = Math.log10(n);
        return Math.abs(log - Math.round(log)) < 1e-10;
    }
    
    // Approach 3: String check
    public static boolean isPowerOfTenString(int n) {
        if (n <= 0) return false;
        
        String str = String.valueOf(n);
        if (str.charAt(0) != '1') return false;
        
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != '0') return false;
        }
        
        return true;
    }
    
    // Approach 4: Recursive
    public static boolean isPowerOfTenRecursive(int n) {
        if (n == 1) return true;
        if (n <= 0 || n % 10 != 0) return false;
        return isPowerOfTenRecursive(n / 10);
    }
    
    // Approach 5: Pre-computed set (for int range)
    private static final Set<Integer> POWERS_OF_TEN = new HashSet<>(
        Arrays.asList(1, 10, 100, 1000, 10000, 100000, 1000000, 
                     10000000, 100000000, 1000000000)
    );
    
    public static boolean isPowerOfTenSet(int n) {
        return POWERS_OF_TEN.contains(n);
    }
    
    // For long values
    public static boolean isPowerOfTenLong(long n) {
        if (n <= 0) return false;
        
        while (n % 10 == 0) {
            n /= 10;
        }
        
        return n == 1;
    }
    
    // Bonus: Get the exponent if power of 10
    public static int getPowerOfTen(int n) {
        if (!isPowerOfTen(n)) return -1;
        
        int power = 0;
        while (n > 1) {
            n /= 10;
            power++;
        }
        return power;
    }
    
    // Bonus: Find nearest power of 10
    public static int nearestPowerOfTen(int n) {
        if (n <= 0) return 1;
        
        int lower = 1;
        while (lower * 10 <= n) {
            lower *= 10;
        }
        
        int upper = lower * 10;
        return (n - lower < upper - n) ? lower : upper;
    }
}
```

---

## Test Cases

```java
@Test
public void testPowerOfTen() {
    // Powers of 10
    assertTrue(isPowerOfTen(1));      // 10^0
    assertTrue(isPowerOfTen(10));     // 10^1
    assertTrue(isPowerOfTen(100));    // 10^2
    assertTrue(isPowerOfTen(1000));   // 10^3
    assertTrue(isPowerOfTen(10000));  // 10^4
    assertTrue(isPowerOfTen(1000000000)); // 10^9
    
    // Not powers of 10
    assertFalse(isPowerOfTen(0));
    assertFalse(isPowerOfTen(5));
    assertFalse(isPowerOfTen(50));
    assertFalse(isPowerOfTen(99));
    assertFalse(isPowerOfTen(101));
    assertFalse(isPowerOfTen(1001));
    
    // Negative numbers
    assertFalse(isPowerOfTen(-1));
    assertFalse(isPowerOfTen(-10));
    assertFalse(isPowerOfTen(-100));
    
    // Edge cases
    assertFalse(isPowerOfTen(Integer.MAX_VALUE));
    assertFalse(isPowerOfTen(Integer.MIN_VALUE));
}

@Test
public void testGetPowerOfTen() {
    assertEquals(0, getPowerOfTen(1));
    assertEquals(1, getPowerOfTen(10));
    assertEquals(2, getPowerOfTen(100));
    assertEquals(3, getPowerOfTen(1000));
    assertEquals(-1, getPowerOfTen(50));
}
```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Division | O(log n) | O(1) | Best general approach |
| Logarithm | O(1) | O(1) | Floating-point precision issues |
| String | O(log n) | O(log n) | Simple but inefficient |
| Pre-computed Set | O(1) | O(1) | Only 10 values for int |
| Recursive | O(log n) | O(log n) | Stack space |

---

## Visual Representation

```
Powers of 10:
10^0 = 1
10^1 = 10
10^2 = 100
10^3 = 1000
10^4 = 10000
...

Pattern: 1 followed by n zeros

Division process for 1000:
1000 → 100 → 10 → 1 ✓

Division process for 50:
50 → 5 (not divisible by 10) ✗
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `1` | `true` | 10^0 = 1 |
| `0` | `false` | Not positive |
| `-10` | `false` | Negative |
| `10` | `true` | 10^1 |
| `50` | `false` | 5 × 10 |
| `100` | `true` | 10^2 |
| `1000000000` | `true` | 10^9 (max for int) |

---

## Common Mistakes

1. **Not Handling Zero/Negative:**
   ```java
   // WRONG - doesn't check n <= 0
   while (n % 10 == 0) n /= 10;
   
   // CORRECT
   if (n <= 0) return false;
   ```

2. **Floating-Point Precision:**
   ```java
   // WRONG - exact equality fails
   return log == Math.floor(log);
   
   // CORRECT - use epsilon
   return Math.abs(log - Math.round(log)) < 1e-10;
   ```

3. **Not Checking n == 1 After Loop:**
   ```java
   // WRONG - doesn't verify final value
   while (n % 10 == 0) n /= 10;
   return true;
   
   // CORRECT
   return n == 1;
   ```

---

## Why Division is Best

```
Pros:
✅ Simple and intuitive
✅ No floating-point issues
✅ Works for all integer types
✅ O(log n) is acceptable

Cons:
❌ Not O(1) like logarithm

Logarithm issues:
❌ Floating-point precision errors
❌ log10(1000) might be 2.9999999999
```

---

## Pre-computed Set for Int Range

```java
// Only 10 possible values for 32-bit int
private static final Set<Integer> POWERS = Set.of(
    1, 10, 100, 1000, 10000, 100000, 
    1000000, 10000000, 100000000, 1000000000
);

public static boolean isPowerOfTen(int n) {
    return POWERS.contains(n);
}

// O(1) lookup, perfect for frequent queries
```

---

## Mathematical Property Approach

```
10 = 2 × 5

Therefore:
10^n = 2^n × 5^n

A number is power of 10 if and only if:
1. It's a power of 2, AND
2. It's a power of 5

Example: 100 = 10^2
  100 = 4 × 25 = 2^2 × 5^2 ✓

Example: 50 = 2 × 25
  50 = 2^1 × 5^2 (different exponents) ✗
```

---

## Related Problems

- **LeetCode 231:** Power of Two
- **LeetCode 326:** Power of Three
- **LeetCode 342:** Power of Four
- **Check if number is perfect power**

---

## Interview Tips

1. **Clarify Requirements:**
   - Handle negative numbers?
   - Handle zero?
   - Int or long range?
   - Frequency of queries?

2. **Start with Division:**
   - Most straightforward
   - Easy to explain

3. **Mention Optimizations:**
   - Pre-computed set for int
   - Logarithm for O(1)

4. **Walk Through Example:**
   - Use 1000
   - Show division steps

5. **Discuss Trade-offs:**
   - Simplicity vs performance
   - Precision issues with log

---

## Real-World Applications

- **Number Formatting:** Determining decimal places
- **Scientific Notation:** Converting numbers
- **Data Validation:** Checking scale factors
- **Unit Conversion:** Metric system (powers of 10)
- **Rounding:** To nearest power of 10
- **Logarithmic Scales:** Chart axes

---

## Bonus: All Powers of 10 in Int Range

```java
public static List<Integer> getAllPowersOfTen() {
    List<Integer> powers = new ArrayList<>();
    int power = 1;
    
    while (power > 0) { // Check for overflow
        powers.add(power);
        if (power > Integer.MAX_VALUE / 10) break;
        power *= 10;
    }
    
    return powers;
}

// Result: [1, 10, 100, 1000, ..., 1000000000]
// Only 10 values!
```

---

## Pattern Recognition

```
Powers of 10 in binary:
1     = 0b1
10    = 0b1010
100   = 0b1100100
1000  = 0b1111101000

No simple binary pattern (unlike powers of 2)

Powers of 10 in decimal:
1, 10, 100, 1000, 10000, ...
Pattern: 1 followed by n zeros
```

---

## Key Takeaways

✅ Division approach is simplest and most reliable  
✅ Check `n <= 0` first (powers of 10 are positive)  
✅ Divide by 10 until not divisible, check if result is 1  
✅ O(log n) time, O(1) space  
✅ Pre-computed set is O(1) for int range (only 10 values)  
✅ Avoid logarithm due to floating-point precision issues  
✅ String approach works but is inefficient
