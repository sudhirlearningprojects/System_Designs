# Find Pairs Where a^b = b^a

## Problem Statement

Given a set of numbers, find all pairs (a, b) such that a^b = b^a where a ≠ b.

**Input:** Array of positive integers  
**Output:** Count or list of pairs satisfying a^b = b^a

**Examples:**
```
Input: [2, 3, 4]
Output: 1
Explanation: (2, 4) because 2^4 = 16 and 4^2 = 16

Input: [2, 3, 4, 16, 27]
Output: 3
Explanation: 
  (2, 4): 2^4 = 4^2 = 16
  (2, 16): 2^16 = 65536, 16^2 = 256 (not equal, but 2^16 = 16^2 in modular sense)
  (3, 27): 3^27 = 27^3 (both very large)

Input: [2, 2, 4]
Output: 2
Explanation: (2₁, 4) and (2₂, 4)
```

---

## Mathematical Analysis

### Key Property

For a^b = b^a to hold:
```
a^b = b^a
Taking log both sides:
b × log(a) = a × log(b)
b/a = log(b)/log(a)
```

### Special Cases

1. **a = b:** Always true (excluded as per problem)
2. **(2, 4):** 2^4 = 4^2 = 16 ✓
3. **(2^k, 2):** Always works for k > 1
4. **(3, 27):** 3^27 = 27^3 (both = 3^27)
5. **(n, n^k):** Works when n^(n^k) = (n^k)^n

---

## Solution Approaches

### Approach 1: Brute Force with Logarithm

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int countPairs(int[] arr) {
    int count = 0;
    
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (isPowerEqual(arr[i], arr[j])) {
                count++;
            }
        }
    }
    
    return count;
}

private static boolean isPowerEqual(int a, int b) {
    if (a == b) return false;
    
    // Check: b * log(a) == a * log(b)
    double left = b * Math.log(a);
    double right = a * Math.log(b);
    
    return Math.abs(left - right) < 1e-9;
}
```

---

### Approach 2: Mathematical Pattern Recognition

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int countPairsPattern(int[] arr) {
    int count = 0;
    
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            int a = arr[i];
            int b = arr[j];
            
            if (a == b) continue;
            
            // Known patterns
            if ((a == 2 && isPowerOf2(b)) || (b == 2 && isPowerOf2(a))) {
                count++;
            } else if (isPowerRelation(a, b)) {
                count++;
            }
        }
    }
    
    return count;
}

private static boolean isPowerOf2(int n) {
    return n > 0 && (n & (n - 1)) == 0;
}

private static boolean isPowerRelation(int a, int b) {
    // Check if b = a^k or a = b^k
    if (a > b) {
        int temp = a;
        a = b;
        b = temp;
    }
    
    long power = a;
    while (power < b) {
        power *= a;
    }
    
    return power == b;
}
```

---

### Approach 3: HashMap with Ratio

**Time Complexity:** O(n²)  
**Space Complexity:** O(n)

```java
public static int countPairsHashMap(int[] arr) {
    Map<Double, List<Integer>> ratioMap = new HashMap<>();
    
    for (int i = 0; i < arr.length; i++) {
        double ratio = Math.log(arr[i]) / arr[i];
        ratioMap.computeIfAbsent(ratio, k -> new ArrayList<>()).add(i);
    }
    
    int count = 0;
    for (List<Integer> indices : ratioMap.values()) {
        int n = indices.size();
        count += n * (n - 1) / 2;
    }
    
    return count;
}
```

---

### Approach 4: Direct Calculation (Small Numbers)

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int countPairsDirect(int[] arr) {
    int count = 0;
    
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (checkPowerEqual(arr[i], arr[j])) {
                count++;
            }
        }
    }
    
    return count;
}

private static boolean checkPowerEqual(int a, int b) {
    if (a == b) return false;
    
    // For small numbers, calculate directly
    if (a <= 10 && b <= 10) {
        long powAB = (long) Math.pow(a, b);
        long powBA = (long) Math.pow(b, a);
        return powAB == powBA;
    }
    
    // For larger numbers, use logarithm
    return Math.abs(b * Math.log(a) - a * Math.log(b)) < 1e-9;
}
```

---

## Algorithm Walkthrough

### Example: [2, 3, 4]

```
Check all pairs:

Pair (2, 3):
  2^3 = 8
  3^2 = 9
  8 ≠ 9 ✗

Pair (2, 4):
  2^4 = 16
  4^2 = 16
  16 = 16 ✓
  count = 1

Pair (3, 4):
  3^4 = 81
  4^3 = 64
  81 ≠ 64 ✗

Result: 1 pair
```

### Example: [2, 4, 16]

```
Pair (2, 4):
  2^4 = 16
  4^2 = 16 ✓
  count = 1

Pair (2, 16):
  2^16 = 65536
  16^2 = 256
  Using log: 16 × log(2) vs 2 × log(16)
  16 × 0.693 = 11.09
  2 × 2.773 = 5.55
  Not equal ✗

Pair (4, 16):
  4^16 = 2^32 = 4294967296
  16^4 = 2^16 = 65536
  Not equal ✗

Result: 1 pair
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Logarithm comparison
    public static int countPairs(int[] arr) {
        int count = 0;
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (isPowerEqual(arr[i], arr[j])) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private static boolean isPowerEqual(int a, int b) {
        if (a == b) return false;
        
        double left = b * Math.log(a);
        double right = a * Math.log(b);
        
        return Math.abs(left - right) < 1e-9;
    }
    
    // Return all pairs
    public static List<int[]> findAllPairs(int[] arr) {
        List<int[]> pairs = new ArrayList<>();
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (isPowerEqual(arr[i], arr[j])) {
                    pairs.add(new int[]{arr[i], arr[j]});
                }
            }
        }
        
        return pairs;
    }
    
    // Check specific pair
    public static boolean checkPair(int a, int b) {
        if (a == b) return false;
        
        // For small numbers, calculate directly
        if (a <= 20 && b <= 20) {
            try {
                long powAB = (long) Math.pow(a, b);
                long powBA = (long) Math.pow(b, a);
                return powAB == powBA;
            } catch (Exception e) {
                // Overflow, use logarithm
            }
        }
        
        // Use logarithm for larger numbers
        return Math.abs(b * Math.log(a) - a * Math.log(b)) < 1e-9;
    }
    
    public static boolean doTestsPass() {
        // Test 1: (2, 4)
        if (countPairs(new int[]{2, 3, 4}) != 1) return false;
        
        // Test 2: Multiple pairs
        if (countPairs(new int[]{2, 4, 16}) != 1) return false;
        
        // Test 3: No pairs
        if (countPairs(new int[]{3, 5, 7}) != 0) return false;
        
        // Test 4: Duplicates
        if (countPairs(new int[]{2, 2, 4}) != 2) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {2, 3, 4, 16, 27};
        System.out.println("Array: " + Arrays.toString(arr));
        System.out.println("Number of pairs: " + countPairs(arr));
        
        List<int[]> pairs = findAllPairs(arr);
        System.out.println("\nPairs where a^b = b^a:");
        for (int[] pair : pairs) {
            System.out.println("(" + pair[0] + ", " + pair[1] + ")");
        }
        
        // Verify specific pairs
        System.out.println("\nVerification:");
        System.out.println("2^4 = " + Math.pow(2, 4));
        System.out.println("4^2 = " + Math.pow(4, 2));
    }
}
```

---

## Test Cases

```java
@Test
public void testCountPairs() {
    // Test 1: Known pair (2, 4)
    assertEquals(1, countPairs(new int[]{2, 3, 4}));
    
    // Test 2: No pairs
    assertEquals(0, countPairs(new int[]{3, 5, 7}));
    
    // Test 3: Multiple same numbers
    assertEquals(2, countPairs(new int[]{2, 2, 4}));
    
    // Test 4: Single element
    assertEquals(0, countPairs(new int[]{5}));
    
    // Test 5: Two elements (pair)
    assertEquals(1, countPairs(new int[]{2, 4}));
    
    // Test 6: Two elements (no pair)
    assertEquals(0, countPairs(new int[]{3, 5}));
    
    // Test 7: Powers of 2
    assertEquals(1, countPairs(new int[]{2, 4, 8}));
    
    // Test 8: Verify specific pairs
    assertTrue(checkPair(2, 4));
    assertFalse(checkPair(2, 3));
    assertFalse(checkPair(3, 4));
}
```

---

## Visual Representation

### Known Pairs

```
(2, 4):
  2^4 = 2 × 2 × 2 × 2 = 16
  4^2 = 4 × 4 = 16
  Equal ✓

(2, 3):
  2^3 = 8
  3^2 = 9
  Not equal ✗

(3, 27):
  3^27 = 7625597484987
  27^3 = 19683
  Not equal ✗
  (But 27 = 3^3, so special case)
```

### Logarithm Check

```
For a^b = b^a:
  b × log(a) = a × log(b)

Example: (2, 4)
  4 × log(2) = 4 × 0.693 = 2.772
  2 × log(4) = 2 × 1.386 = 2.772
  Equal ✓

Example: (2, 3)
  3 × log(2) = 3 × 0.693 = 2.079
  2 × log(3) = 2 × 1.099 = 2.198
  Not equal ✗
```

---

## Edge Cases

1. **Empty array:** [] → 0
2. **Single element:** [5] → 0
3. **All same:** [2, 2, 2] → 0 (a ≠ b required)
4. **Duplicates:** [2, 2, 4] → 2 pairs
5. **No pairs:** [3, 5, 7] → 0
6. **Large numbers:** Use logarithm
7. **Overflow:** Handle with long or logarithm

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Brute Force | O(n²) | O(1) | Check all pairs |
| HashMap | O(n²) | O(n) | Group by ratio |
| Pattern | O(n²) | O(1) | Known patterns |

**Why O(n²)?**
- Must check all pairs
- n × (n-1) / 2 comparisons

---

## Related Problems

1. **Power of Number** - Check if n = a^b
2. **Perfect Power** - Find if number is perfect power
3. **Exponent Pairs** - Find pairs with specific exponent
4. **Logarithm Equations** - Solve log equations
5. **Number Theory** - Diophantine equations

---

## Interview Tips

### Clarification Questions
1. Can array have duplicates? (Yes, count separately)
2. Are numbers positive? (Usually yes)
3. Is a = b allowed? (Usually no)
4. Return count or pairs? (Clarify)
5. Range of numbers? (Affects overflow)

### Approach Explanation
1. "Check all pairs using nested loops"
2. "For each pair, verify a^b = b^a"
3. "Use logarithm to avoid overflow"
4. "Check: b × log(a) = a × log(b)"
5. "O(n²) time, O(1) space"

### Common Mistakes
- Integer overflow for large powers
- Floating point precision errors
- Not handling duplicates correctly
- Forgetting a ≠ b condition
- Not using logarithm for large numbers

### Why Use Logarithm?

```java
// Direct calculation: Overflow risk
long powAB = (long) Math.pow(2, 100); // Overflow!

// Logarithm: No overflow
double left = 100 * Math.log(2);
double right = 2 * Math.log(100);
// Compare safely
```

---

## Real-World Applications

1. **Mathematics** - Number theory research
2. **Cryptography** - Modular exponentiation
3. **Puzzles** - Mathematical games
4. **Education** - Teaching exponents
5. **Algorithm Design** - Complexity analysis

---

## Key Takeaways

1. **Logarithm Property:** a^b = b^a ⟺ b×log(a) = a×log(b)
2. **Known Pair:** (2, 4) is the most common example
3. **Time Complexity:** O(n²) to check all pairs
4. **Overflow:** Use logarithm for large numbers
5. **Precision:** Use tolerance (1e-9) for floating point
6. **Duplicates:** Count each occurrence separately
7. **Pattern:** Powers of 2 often satisfy the condition

---

## Additional Notes

### Mathematical Proof

```
Given: a^b = b^a

Taking natural log:
  ln(a^b) = ln(b^a)
  b × ln(a) = a × ln(b)
  b/a = ln(b)/ln(a)
  b/a = log_a(b)

This means: b = a × log_a(b)
```

### Known Pairs List

```
Small number pairs where a^b = b^a:
  (2, 4): 2^4 = 4^2 = 16
  (2^k, 2) for k > 1
  (e^(1/e), e^(1/e)): Special mathematical case
  
No other small integer pairs exist!
```

### Why (2, 4) Works

```
2^4 = 16
4^2 = 16

Because: 4 = 2^2
So: 4^2 = (2^2)^2 = 2^4 ✓

General: (a, a^k) works when:
  a^(a^k) = (a^k)^a
  a^(a^k) = a^(k×a)
  a^k = k×a
  Only works for specific values
```

### Floating Point Precision

```java
// Problem: Direct comparison
double left = 4 * Math.log(2);
double right = 2 * Math.log(4);
if (left == right) // May fail due to precision

// Solution: Use tolerance
if (Math.abs(left - right) < 1e-9) // Better
```

### Optimization: Early Exit

```java
// If a and b are both > 2 and not powers of each other
// They likely don't satisfy a^b = b^a
if (a > 2 && b > 2 && !isPowerRelation(a, b)) {
    return false;
}
```

### Extension: Find All Valid Pairs

```java
// Generate all pairs up to n where a^b = b^a
public static List<int[]> generateValidPairs(int n) {
    List<int[]> pairs = new ArrayList<>();
    
    for (int a = 2; a <= n; a++) {
        for (int b = a + 1; b <= n; b++) {
            if (isPowerEqual(a, b)) {
                pairs.add(new int[]{a, b});
            }
        }
    }
    
    return pairs;
}

// Up to 100: Only (2, 4) and powers of 2
```

### Why So Few Pairs?

```
For a^b = b^a:
  b/a = log(b)/log(a)

As a and b grow, this ratio becomes harder to satisfy.

Example:
  (2, 4): 4/2 = 2, log(4)/log(2) = 2 ✓
  (3, 9): 9/3 = 3, log(9)/log(3) = 2 ✗
  (3, 27): 27/3 = 9, log(27)/log(3) = 3 ✗

Only very specific combinations work!
```

### Modular Arithmetic Variant

```java
// Check if a^b ≡ b^a (mod m)
public static boolean isPowerEqualMod(int a, int b, int m) {
    long powAB = modPow(a, b, m);
    long powBA = modPow(b, a, m);
    return powAB == powBA;
}

private static long modPow(long base, long exp, long mod) {
    long result = 1;
    base %= mod;
    
    while (exp > 0) {
        if (exp % 2 == 1) {
            result = (result * base) % mod;
        }
        base = (base * base) % mod;
        exp /= 2;
    }
    
    return result;
}
```
