# Factorization - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Algorithm](#algorithm)
3. [Implementation](#implementation)
4. [Examples](#examples)
5. [Time Complexity](#time-complexity)

---

## Introduction

**Factorization** finds all divisors of a given number. Unlike prime factorization which only finds prime divisors, this finds ALL factors.

### Example
- **6** → Factors: [1, 2, 3, 6]
- **12** → Factors: [1, 2, 3, 4, 6, 12]
- **20** → Factors: [1, 2, 4, 5, 10, 20]

---

## Algorithm

### Efficient Approach (O(√n))

1. Iterate from 1 to √n
2. If i divides n, both i and n/i are factors
3. Avoid duplicates when i = √n

---

## Implementation

### Java Implementation

```java
import java.util.*;

public class Factorization {
    
    public static List<Integer> findFactors(int n) {
        List<Integer> factors = new ArrayList<>();
        
        for (int i = 1; i * i <= n; i++) {
            if (n % i == 0) {
                factors.add(i);
                if (i != n / i) {
                    factors.add(n / i);
                }
            }
        }
        
        Collections.sort(factors);
        return factors;
    }
}
```

### Python Implementation

```python
def find_factors(n):
    factors = []
    
    i = 1
    while i * i <= n:
        if n % i == 0:
            factors.append(i)
            if i != n // i:
                factors.append(n // i)
        i += 1
    
    return sorted(factors)
```

### C++ Implementation

```cpp
#include <vector>
#include <algorithm>
using namespace std;

vector<int> findFactors(int n) {
    vector<int> factors;
    
    for (int i = 1; i * i <= n; i++) {
        if (n % i == 0) {
            factors.push_back(i);
            if (i != n / i) {
                factors.push_back(n / i);
            }
        }
    }
    
    sort(factors.begin(), factors.end());
    return factors;
}
```

---

## Examples

### Example 1: n = 6
```
i = 1: 6 % 1 == 0 → factors = [1, 6]
i = 2: 6 % 2 == 0 → factors = [1, 6, 2, 3]
i = 3: 3 * 3 = 9 > 6 (stop)

Sorted: [1, 2, 3, 6]
```

### Example 2: n = 12
```
i = 1: 12 % 1 == 0 → factors = [1, 12]
i = 2: 12 % 2 == 0 → factors = [1, 12, 2, 6]
i = 3: 12 % 3 == 0 → factors = [1, 12, 2, 6, 3, 4]
i = 4: 4 * 4 = 16 > 12 (stop)

Sorted: [1, 2, 3, 4, 6, 12]
```

### Example 3: n = 20
```
i = 1: 20 % 1 == 0 → factors = [1, 20]
i = 2: 20 % 2 == 0 → factors = [1, 20, 2, 10]
i = 3: 20 % 3 != 0
i = 4: 20 % 4 == 0 → factors = [1, 20, 2, 10, 4, 5]
i = 5: 5 * 5 = 25 > 20 (stop)

Sorted: [1, 2, 4, 5, 10, 20]
```

### Example 4: n = 36 (Perfect Square)
```
i = 1: 36 % 1 == 0 → factors = [1, 36]
i = 2: 36 % 2 == 0 → factors = [1, 36, 2, 18]
i = 3: 36 % 3 == 0 → factors = [1, 36, 2, 18, 3, 12]
i = 4: 36 % 4 == 0 → factors = [1, 36, 2, 18, 3, 12, 4, 9]
i = 6: 36 % 6 == 0, but 6 == 36/6 → factors = [1, 36, 2, 18, 3, 12, 4, 9, 6]
i = 7: 7 * 7 = 49 > 36 (stop)

Sorted: [1, 2, 3, 4, 6, 9, 12, 18, 36]
```

---

## Time Complexity

**Time**: O(√n) - iterate up to square root  
**Space**: O(d) where d is number of divisors

### Number of Divisors
- Most numbers: O(√n) divisors
- Highly composite numbers: More divisors
- Example: 120 has 16 divisors

---

## Variations

### 1. Count Factors

```java
public static int countFactors(int n) {
    int count = 0;
    for (int i = 1; i * i <= n; i++) {
        if (n % i == 0) {
            count += (i == n / i) ? 1 : 2;
        }
    }
    return count;
}
```

### 2. Sum of Factors

```java
public static int sumOfFactors(int n) {
    int sum = 0;
    for (int i = 1; i * i <= n; i++) {
        if (n % i == 0) {
            sum += i;
            if (i != n / i) {
                sum += n / i;
            }
        }
    }
    return sum;
}
```

### 3. Proper Divisors (excluding n)

```java
public static List<Integer> properDivisors(int n) {
    List<Integer> factors = new ArrayList<>();
    for (int i = 1; i * i <= n; i++) {
        if (n % i == 0) {
            factors.add(i);
            if (i != n / i && n / i != n) {
                factors.add(n / i);
            }
        }
    }
    factors.remove(Integer.valueOf(n));
    Collections.sort(factors);
    return factors;
}
```

---

## Key Differences: Factorization vs Prime Factorization

| Feature | Factorization | Prime Factorization |
|---------|---------------|---------------------|
| **Output** | All divisors | Only prime divisors |
| **Example (6)** | [1, 2, 3, 6] | [2, 3] |
| **Example (12)** | [1, 2, 3, 4, 6, 12] | [2, 2, 3] |
| **Includes 1?** | Yes | No |
| **Includes n?** | Yes | Only if n is prime |
| **Duplicates?** | No | Yes (repeated primes) |

---

## Practice Problems

1. **LeetCode 507**: Perfect Number
2. **LeetCode 1492**: The kth Factor of n
3. **LeetCode 829**: Consecutive Numbers Sum
4. **Codeforces**: Divisor problems
5. **SPOJ**: Divisor counting problems

---

## Key Takeaways

✅ Factorization finds ALL divisors of a number  
✅ Efficient algorithm runs in O(√n) time  
✅ Check pairs: if i divides n, both i and n/i are factors  
✅ Handle perfect squares carefully to avoid duplicates  
✅ Different from prime factorization which only finds primes
