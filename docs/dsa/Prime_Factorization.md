# Prime Factorization - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [What are Prime Factors?](#what-are-prime-factors)
3. [Algorithm](#algorithm)
4. [Implementation](#implementation)
5. [Time & Space Complexity](#time--space-complexity)
6. [Examples](#examples)
7. [Optimizations](#optimizations)
8. [Related Problems](#related-problems)

---

## Introduction

**Prime Factorization** is the process of breaking down a composite number into its prime factors. Every integer greater than 1 can be expressed as a product of prime numbers.

### Fundamental Theorem of Arithmetic
Every integer greater than 1 is either:
- A prime number itself, OR
- Can be uniquely represented as a product of prime numbers

---

## What are Prime Factors?

**Prime factors** are prime numbers that divide a given number exactly (with remainder 0).

### Examples
- **12** = 2 × 2 × 3 = 2² × 3
- **315** = 3 × 3 × 5 × 7 = 3² × 5 × 7
- **100** = 2 × 2 × 5 × 5 = 2² × 5²
- **17** = 17 (prime number itself)

---

## Algorithm

### Basic Approach

1. **Divide by 2**: Remove all factors of 2
2. **Check odd numbers**: Test divisibility from 3 to √n
3. **Remaining prime**: If n > 1 after loop, it's a prime factor

### Why only check up to √n?
If n has a factor greater than √n, it must also have a corresponding factor less than √n.

**Example**: For n = 36
- √36 = 6
- Factors: 1, 2, 3, 4, 6, 9, 12, 18, 36
- Pairs: (1,36), (2,18), (3,12), (4,9), (6,6)
- After 6, all factors are duplicates

---

## Implementation

### Java Implementation

```java
import java.util.ArrayList;
import java.util.List;

public class PrimeFactors {
    
    public static List<Integer> findPrimeFactors(int n) {
        List<Integer> factors = new ArrayList<>();
        
        // Handle 2 separately (only even prime)
        while (n % 2 == 0) {
            factors.add(2);
            n /= 2;
        }
        
        // Check odd numbers from 3 to √n
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
}
```

### Python Implementation

```python
def find_prime_factors(n):
    factors = []
    
    # Handle 2 separately
    while n % 2 == 0:
        factors.append(2)
        n //= 2
    
    # Check odd numbers from 3 to √n
    i = 3
    while i * i <= n:
        while n % i == 0:
            factors.append(i)
            n //= i
        i += 2
    
    # If n > 1, it's a prime factor
    if n > 1:
        factors.append(n)
    
    return factors
```

### C++ Implementation

```cpp
#include <vector>
using namespace std;

vector<int> findPrimeFactors(int n) {
    vector<int> factors;
    
    // Handle 2 separately
    while (n % 2 == 0) {
        factors.push_back(2);
        n /= 2;
    }
    
    // Check odd numbers from 3 to √n
    for (int i = 3; i * i <= n; i += 2) {
        while (n % i == 0) {
            factors.push_back(i);
            n /= i;
        }
    }
    
    // If n > 1, it's a prime factor
    if (n > 1) {
        factors.push_back(n);
    }
    
    return factors;
}
```

---

## Time & Space Complexity

### Time Complexity: **O(√n)**
- We iterate from 2 to √n
- Each division reduces n, making subsequent iterations faster
- Worst case: when n is prime (no factors found until the end)

### Space Complexity: **O(log n)**
- Maximum number of prime factors is O(log n)
- Example: 2^k has k factors (all 2s)
- For n = 1024 = 2^10, we have 10 factors

---

## Examples

### Example 1: n = 12
```
Step 1: n = 12, check 2
  12 % 2 == 0 → factors = [2], n = 6
  6 % 2 == 0 → factors = [2, 2], n = 3

Step 2: n = 3, check odd numbers
  i = 3, 3 * 3 = 9 > 3 (stop loop)

Step 3: n = 3 > 1
  factors = [2, 2, 3]

Result: 12 = 2 × 2 × 3
```

### Example 2: n = 315
```
Step 1: n = 315, check 2
  315 % 2 != 0 (skip)

Step 2: n = 315, check odd numbers
  i = 3: 315 % 3 == 0 → factors = [3], n = 105
  i = 3: 105 % 3 == 0 → factors = [3, 3], n = 35
  i = 3: 35 % 3 != 0
  
  i = 5: 35 % 5 == 0 → factors = [3, 3, 5], n = 7
  i = 5: 7 % 5 != 0
  
  i = 7: 7 * 7 = 49 > 7 (stop loop)

Step 3: n = 7 > 1
  factors = [3, 3, 5, 7]

Result: 315 = 3 × 3 × 5 × 7
```

### Example 3: n = 17 (Prime)
```
Step 1: n = 17, check 2
  17 % 2 != 0 (skip)

Step 2: n = 17, check odd numbers
  i = 3: 17 % 3 != 0
  i = 5: 5 * 5 = 25 > 17 (stop loop)

Step 3: n = 17 > 1
  factors = [17]

Result: 17 is prime
```

### Example 4: n = 100
```
Step 1: n = 100, check 2
  100 % 2 == 0 → factors = [2], n = 50
  50 % 2 == 0 → factors = [2, 2], n = 25

Step 2: n = 25, check odd numbers
  i = 3: 25 % 3 != 0
  i = 5: 25 % 5 == 0 → factors = [2, 2, 5], n = 5
  i = 5: 5 % 5 == 0 → factors = [2, 2, 5, 5], n = 1

Step 3: n = 1 (not > 1, skip)

Result: 100 = 2 × 2 × 5 × 5
```

---

## Optimizations

### 1. Return Unique Factors with Count

```java
public static Map<Integer, Integer> findPrimeFactorsWithCount(int n) {
    Map<Integer, Integer> factorCount = new HashMap<>();
    
    while (n % 2 == 0) {
        factorCount.put(2, factorCount.getOrDefault(2, 0) + 1);
        n /= 2;
    }
    
    for (int i = 3; i * i <= n; i += 2) {
        while (n % i == 0) {
            factorCount.put(i, factorCount.getOrDefault(i, 0) + 1);
            n /= i;
        }
    }
    
    if (n > 1) {
        factorCount.put(n, 1);
    }
    
    return factorCount;
}

// Example: 12 → {2: 2, 3: 1} → 2² × 3¹
```

### 2. Sieve of Eratosthenes (Multiple Queries)

For multiple factorization queries, precompute smallest prime factors:

```java
public static int[] computeSmallestPrimeFactor(int maxN) {
    int[] spf = new int[maxN + 1];
    for (int i = 0; i <= maxN; i++) spf[i] = i;
    
    for (int i = 2; i * i <= maxN; i++) {
        if (spf[i] == i) { // i is prime
            for (int j = i * i; j <= maxN; j += i) {
                if (spf[j] == j) spf[j] = i;
            }
        }
    }
    return spf;
}

public static List<Integer> factorizeUsingSPF(int n, int[] spf) {
    List<Integer> factors = new ArrayList<>();
    while (n > 1) {
        factors.add(spf[n]);
        n /= spf[n];
    }
    return factors;
}

// Precompute: O(n log log n)
// Each query: O(log n)
```

---

## Related Problems

### 1. Count Number of Prime Factors
```java
public static int countPrimeFactors(int n) {
    int count = 0;
    
    while (n % 2 == 0) {
        count++;
        n /= 2;
    }
    
    for (int i = 3; i * i <= n; i += 2) {
        while (n % i == 0) {
            count++;
            n /= i;
        }
    }
    
    if (n > 1) count++;
    
    return count;
}
```

### 2. Count Distinct Prime Factors
```java
public static int countDistinctPrimeFactors(int n) {
    int count = 0;
    
    if (n % 2 == 0) {
        count++;
        while (n % 2 == 0) n /= 2;
    }
    
    for (int i = 3; i * i <= n; i += 2) {
        if (n % i == 0) {
            count++;
            while (n % i == 0) n /= i;
        }
    }
    
    if (n > 1) count++;
    
    return count;
}
```

### 3. Largest Prime Factor
```java
public static int largestPrimeFactor(int n) {
    int largest = -1;
    
    while (n % 2 == 0) {
        largest = 2;
        n /= 2;
    }
    
    for (int i = 3; i * i <= n; i += 2) {
        while (n % i == 0) {
            largest = i;
            n /= i;
        }
    }
    
    if (n > 1) largest = n;
    
    return largest;
}
```

### 4. GCD using Prime Factorization
```java
public static int gcdUsingFactors(int a, int b) {
    Map<Integer, Integer> factorsA = findPrimeFactorsWithCount(a);
    Map<Integer, Integer> factorsB = findPrimeFactorsWithCount(b);
    
    int gcd = 1;
    for (int prime : factorsA.keySet()) {
        if (factorsB.containsKey(prime)) {
            int minPower = Math.min(factorsA.get(prime), factorsB.get(prime));
            gcd *= Math.pow(prime, minPower);
        }
    }
    return gcd;
}
```

### 5. LCM using Prime Factorization
```java
public static int lcmUsingFactors(int a, int b) {
    Map<Integer, Integer> factorsA = findPrimeFactorsWithCount(a);
    Map<Integer, Integer> factorsB = findPrimeFactorsWithCount(b);
    
    Set<Integer> allPrimes = new HashSet<>();
    allPrimes.addAll(factorsA.keySet());
    allPrimes.addAll(factorsB.keySet());
    
    int lcm = 1;
    for (int prime : allPrimes) {
        int maxPower = Math.max(
            factorsA.getOrDefault(prime, 0),
            factorsB.getOrDefault(prime, 0)
        );
        lcm *= Math.pow(prime, maxPower);
    }
    return lcm;
}
```

---

## Practice Problems

1. **LeetCode 204**: Count Primes
2. **LeetCode 650**: 2 Keys Keyboard
3. **LeetCode 952**: Largest Component Size by Common Factor
4. **Project Euler #3**: Largest Prime Factor
5. **SPOJ FACT0**: Integer Factorization
6. **Codeforces**: Prime Factorization problems

---

## Key Takeaways

✅ Prime factorization decomposes a number into prime factors  
✅ Only check divisors up to √n for efficiency  
✅ Handle 2 separately, then check odd numbers  
✅ Time complexity: O(√n)  
✅ Space complexity: O(log n)  
✅ Use SPF array for multiple queries  
✅ Applications: GCD, LCM, cryptography, number theory
