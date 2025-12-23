# Prime Factorization

## Problem Statement

Return an array containing prime numbers whose product equals the given number.

**Input:** Integer x  
**Output:** Array of prime factors

**Examples:**
```
primeFactorization(6) = [2, 3]        // 2 × 3 = 6
primeFactorization(5) = [5]           // 5 is prime
primeFactorization(12) = [2, 2, 3]    // 2 × 2 × 3 = 12
primeFactorization(100) = [2, 2, 5, 5] // 2² × 5² = 100
```

---

## Solution Approach

### Optimal: Trial Division

**Time Complexity:** O(√n)  
**Space Complexity:** O(log n) - for storing factors

```java
public List<Integer> primeFactorization(int n) {
    List<Integer> factors = new ArrayList<>();
    
    // Handle factor 2
    while (n % 2 == 0) {
        factors.add(2);
        n /= 2;
    }
    
    // Check odd factors from 3 onwards
    for (int i = 3; i * i <= n; i += 2) {
        while (n % i == 0) {
            factors.add(i);
            n /= i;
        }
    }
    
    // If n > 1, then it's a prime factor
    if (n > 1) {
        factors.add(n);
    }
    
    return factors;
}
```

---

## Algorithm Explanation

### Key Insights

1. **Divide by 2 first:** Handle all even factors
2. **Check only odd numbers:** After removing 2, only odd numbers can be factors
3. **Stop at √n:** If no factor found by √n, remaining n is prime
4. **Divide completely:** Extract all occurrences of each factor

### Why Stop at √n?

```
If n = a × b and a ≤ b, then a ≤ √n

Example: 36 = 6 × 6
- Factors: 1, 2, 3, 4, 6, 9, 12, 18, 36
- √36 = 6
- All factors > 6 are paired with factors < 6
- No need to check beyond √n
```

---

## Detailed Walkthrough

**Input:** n = 60

```
Step 1: Divide by 2
  60 % 2 == 0 → factors = [2], n = 30
  30 % 2 == 0 → factors = [2, 2], n = 15
  15 % 2 != 0 → Done with 2

Step 2: Check i = 3
  15 % 3 == 0 → factors = [2, 2, 3], n = 5
  5 % 3 != 0 → Move to next

Step 3: Check i = 5
  i * i = 25 > 5 → Stop loop

Step 4: n = 5 > 1
  factors = [2, 2, 3, 5]

Result: [2, 2, 3, 5]
Verify: 2 × 2 × 3 × 5 = 60 ✓
```

---

## Complete Implementation

```java
public class PrimeFactorization {
    
    // Optimal: Trial Division
    public List<Integer> primeFactorization(int n) {
        List<Integer> factors = new ArrayList<>();
        
        if (n <= 1) return factors;
        
        // Handle factor 2
        while (n % 2 == 0) {
            factors.add(2);
            n /= 2;
        }
        
        // Check odd factors from 3 onwards
        for (int i = 3; i * i <= n; i += 2) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        
        // If n > 1, then it's a prime factor
        if (n > 1) {
            factors.add(n);
        }
        
        return factors;
    }
    
    // Alternative: Return as array
    public int[] primeFactorizationArray(int n) {
        List<Integer> factors = primeFactorization(n);
        return factors.stream().mapToInt(Integer::intValue).toArray();
    }
    
    // Variation: Return unique prime factors only
    public List<Integer> uniquePrimeFactors(int n) {
        List<Integer> factors = new ArrayList<>();
        
        if (n % 2 == 0) {
            factors.add(2);
            while (n % 2 == 0) n /= 2;
        }
        
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                factors.add(i);
                while (n % i == 0) n /= i;
            }
        }
        
        if (n > 1) factors.add(n);
        
        return factors;
    }
    
    // Variation: Return with exponents (prime factorization form)
    public Map<Integer, Integer> primeFactorizationWithExponents(int n) {
        Map<Integer, Integer> factors = new LinkedHashMap<>();
        
        if (n % 2 == 0) {
            int count = 0;
            while (n % 2 == 0) {
                count++;
                n /= 2;
            }
            factors.put(2, count);
        }
        
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                int count = 0;
                while (n % i == 0) {
                    count++;
                    n /= i;
                }
                factors.put(i, count);
            }
        }
        
        if (n > 1) factors.put(n, 1);
        
        return factors;
    }
}
```

---

## Test Cases

```java
@Test
public void testPrimeFactorization() {
    PrimeFactorization solver = new PrimeFactorization();
    
    // Basic cases
    assertEquals(Arrays.asList(2, 3), solver.primeFactorization(6));
    assertEquals(Arrays.asList(5), solver.primeFactorization(5));
    assertEquals(Arrays.asList(2, 2, 3), solver.primeFactorization(12));
    
    // Powers of primes
    assertEquals(Arrays.asList(2, 2, 2), solver.primeFactorization(8));
    assertEquals(Arrays.asList(3, 3, 3), solver.primeFactorization(27));
    
    // Larger numbers
    assertEquals(Arrays.asList(2, 2, 5, 5), solver.primeFactorization(100));
    assertEquals(Arrays.asList(2, 2, 3, 5), solver.primeFactorization(60));
    
    // Prime numbers
    assertEquals(Arrays.asList(7), solver.primeFactorization(7));
    assertEquals(Arrays.asList(13), solver.primeFactorization(13));
    assertEquals(Arrays.asList(97), solver.primeFactorization(97));
    
    // Edge cases
    assertEquals(Arrays.asList(), solver.primeFactorization(1));
    assertEquals(Arrays.asList(2), solver.primeFactorization(2));
    
    // Large prime
    assertEquals(Arrays.asList(1009), solver.primeFactorization(1009));
    
    // Product of two large primes
    assertEquals(Arrays.asList(97, 101), solver.primeFactorization(9797));
}
```

---

## Step-by-Step Examples

### Example 1: n = 12

```
Initial: n = 12

Divide by 2:
  12 / 2 = 6  → factors = [2]
  6 / 2 = 3   → factors = [2, 2]
  3 % 2 != 0  → Done with 2

Check i = 3:
  3 * 3 = 9 > 3? No, continue
  3 % 3 == 0  → factors = [2, 2, 3], n = 1
  
n = 1, not > 1, done

Result: [2, 2, 3]
```

### Example 2: n = 100

```
Initial: n = 100

Divide by 2:
  100 / 2 = 50  → factors = [2]
  50 / 2 = 25   → factors = [2, 2]
  25 % 2 != 0   → Done with 2

Check i = 3:
  25 % 3 != 0   → Skip

Check i = 5:
  25 % 5 == 0   → factors = [2, 2, 5], n = 5
  5 % 5 == 0    → factors = [2, 2, 5, 5], n = 1

n = 1, not > 1, done

Result: [2, 2, 5, 5]
```

### Example 3: n = 97 (Prime)

```
Initial: n = 97

Divide by 2:
  97 % 2 != 0   → Skip

Check odd numbers:
  i = 3: 97 % 3 != 0
  i = 5: 97 % 5 != 0
  i = 7: 97 % 7 != 0
  i = 9: 97 % 9 != 0
  i = 11: 11 * 11 = 121 > 97 → Stop

n = 97 > 1 → factors = [97]

Result: [97]
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `1` | `[]` | 1 has no prime factors |
| `2` | `[2]` | 2 is prime |
| `4` | `[2, 2]` | 2² |
| `5` | `[5]` | 5 is prime |
| `100` | `[2, 2, 5, 5]` | 2² × 5² |
| `1009` | `[1009]` | Large prime |

---

## Optimization Techniques

### 1. Handle 2 Separately

```java
// Efficient: Check 2 once, then only odd numbers
while (n % 2 == 0) { ... }
for (int i = 3; i * i <= n; i += 2) { ... }

// Inefficient: Check all numbers
for (int i = 2; i * i <= n; i++) { ... }
```

### 2. Stop at √n

```java
// Efficient: O(√n)
for (int i = 3; i * i <= n; i += 2)

// Inefficient: O(n)
for (int i = 3; i <= n; i += 2)
```

### 3. Divide Completely

```java
// Extract all occurrences at once
while (n % i == 0) {
    factors.add(i);
    n /= i;
}
```

---

## Complexity Analysis

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Prime Factorization | O(√n) | O(log n) | Optimal for single number |
| With Sieve (preprocessing) | O(n log log n) + O(log n) | O(n) | Better for multiple queries |

**Why O(√n)?**
- Check divisors up to √n
- Each division reduces n significantly
- At most log n factors

---

## Common Mistakes

1. **Not Handling Remaining Prime:**
   ```java
   // WRONG - misses large prime factors
   for (int i = 2; i * i <= n; i++) { ... }
   return factors;
   
   // CORRECT
   if (n > 1) factors.add(n);
   ```

2. **Checking All Numbers:**
   ```java
   // WRONG - O(n) time
   for (int i = 2; i <= n; i++)
   
   // CORRECT - O(√n) time
   for (int i = 2; i * i <= n; i++)
   ```

3. **Not Extracting All Occurrences:**
   ```java
   // WRONG - only gets one occurrence
   if (n % i == 0) factors.add(i);
   
   // CORRECT - gets all occurrences
   while (n % i == 0) {
       factors.add(i);
       n /= i;
   }
   ```

---

## Variations

### Return Unique Factors Only

```java
public List<Integer> uniquePrimeFactors(int n) {
    Set<Integer> factors = new LinkedHashSet<>();
    
    if (n % 2 == 0) {
        factors.add(2);
        while (n % 2 == 0) n /= 2;
    }
    
    for (int i = 3; i * i <= n; i += 2) {
        if (n % i == 0) {
            factors.add(i);
            while (n % i == 0) n /= i;
        }
    }
    
    if (n > 1) factors.add(n);
    
    return new ArrayList<>(factors);
}
```

### Return as String (Mathematical Notation)

```java
public String primeFactorizationString(int n) {
    Map<Integer, Integer> factors = primeFactorizationWithExponents(n);
    
    return factors.entrySet().stream()
        .map(e -> e.getValue() == 1 ? 
             String.valueOf(e.getKey()) : 
             e.getKey() + "^" + e.getValue())
        .collect(Collectors.joining(" × "));
}

// Example: 100 → "2^2 × 5^2"
```

### Count Total Divisors

```java
public int countDivisors(int n) {
    Map<Integer, Integer> factors = primeFactorizationWithExponents(n);
    
    int count = 1;
    for (int exponent : factors.values()) {
        count *= (exponent + 1);
    }
    
    return count;
}

// Example: 12 = 2^2 × 3^1
// Divisors = (2+1) × (1+1) = 6
// [1, 2, 3, 4, 6, 12]
```

---

## Advanced: Sieve-Based Factorization

For multiple queries, precompute smallest prime factors:

```java
public class SieveFactorization {
    private int[] spf; // Smallest Prime Factor
    
    public SieveFactorization(int maxN) {
        spf = new int[maxN + 1];
        for (int i = 1; i <= maxN; i++) spf[i] = i;
        
        for (int i = 2; i * i <= maxN; i++) {
            if (spf[i] == i) { // i is prime
                for (int j = i * i; j <= maxN; j += i) {
                    if (spf[j] == j) spf[j] = i;
                }
            }
        }
    }
    
    public List<Integer> factorize(int n) {
        List<Integer> factors = new ArrayList<>();
        while (n > 1) {
            factors.add(spf[n]);
            n /= spf[n];
        }
        return factors;
    }
}

// Preprocessing: O(n log log n)
// Per query: O(log n)
```

---

## Related Problems

- **LeetCode 204:** Count Primes
- **LeetCode 263:** Ugly Number
- **LeetCode 264:** Ugly Number II
- **LeetCode 650:** 2 Keys Keyboard

---

## Interview Tips

1. **Clarify Requirements:**
   - Return all factors or unique factors?
   - Include 1 in output?
   - Handle negative numbers?

2. **Start with Approach:**
   - "Divide by smallest factors first"
   - "Only need to check up to √n"

3. **Optimize Incrementally:**
   - Start with checking all numbers
   - Then optimize to odd numbers only

4. **Walk Through Example:**
   - Use n = 60 or n = 100
   - Show each division step

5. **Discuss Complexity:**
   - O(√n) time
   - O(log n) space for factors

---

## Real-World Applications

- **Cryptography:** RSA encryption relies on difficulty of factorization
- **Number Theory:** GCD, LCM calculations
- **Mathematics:** Simplifying fractions
- **Computer Science:** Hash functions, random number generation
- **Competitive Programming:** Many problems require prime factorization

---

## Mathematical Properties

```
Fundamental Theorem of Arithmetic:
Every integer > 1 can be uniquely represented as a product of primes

Example: 60 = 2² × 3 × 5

Properties:
- Number of divisors: (e₁+1) × (e₂+1) × ... × (eₖ+1)
- Sum of divisors: Use formula with geometric series
- GCD/LCM: Use min/max of exponents
```

---

## Key Takeaways

✅ O(√n) time complexity using trial division  
✅ Handle 2 separately, then check only odd numbers  
✅ Stop at √n, remaining n (if > 1) is prime  
✅ Extract all occurrences of each factor  
✅ For multiple queries, use sieve preprocessing  
✅ Essential algorithm for number theory problems
