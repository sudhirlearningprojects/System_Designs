# Sum of Product of All Pairs

## Problem Statement

Given an array of integers, find the sum of products of all possible pairs of array elements.

**Input:** Array of integers  
**Output:** Sum of products of all pairs

**Examples:**
```
Input:  [1, 3, 4]
Output: 19
Explanation: Pairs: (1,3), (1,4), (3,4)
             Sum = 1*3 + 1*4 + 3*4 = 3 + 4 + 12 = 19

Input:  [2, 3]
Output: 6
Explanation: Pairs: (2,3)
             Sum = 2*3 = 6

Input:  [1, 2, 3, 4]
Output: 35
Explanation: Pairs: (1,2), (1,3), (1,4), (2,3), (2,4), (3,4)
             Sum = 1*2 + 1*3 + 1*4 + 2*3 + 2*4 + 3*4
                 = 2 + 3 + 4 + 6 + 8 + 12 = 35

Input:  [5]
Output: 0
Explanation: No pairs possible

Input:  [2, 4, 6]
Output: 68
Explanation: Pairs: (2,4), (2,6), (4,6)
             Sum = 2*4 + 2*6 + 4*6 = 8 + 12 + 24 = 44
```

---

## Solution Approaches

### Approach 1: Mathematical Formula (Optimal)

**Formula:** `Sum = ((Sum of all elements)² - Sum of squares) / 2`

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static long sumOfProductPairs(int[] arr) {
    if (arr == null || arr.length < 2) return 0;
    
    long sum = 0, sumOfSquares = 0;
    
    for (int num : arr) {
        sum += num;
        sumOfSquares += (long) num * num;
    }
    
    return (sum * sum - sumOfSquares) / 2;
}
```

---

### Approach 2: Nested Loops (Brute Force)

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static long sumOfProductPairsBruteForce(int[] arr) {
    if (arr == null || arr.length < 2) return 0;
    
    long sum = 0;
    
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            sum += (long) arr[i] * arr[j];
        }
    }
    
    return sum;
}
```

---

### Approach 3: Accumulator Pattern

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static long sumOfProductPairsAccumulator(int[] arr) {
    if (arr == null || arr.length < 2) return 0;
    
    long sum = 0, result = 0;
    
    for (int num : arr) {
        result += sum * num;
        sum += num;
    }
    
    return result;
}
```

---

## Mathematical Derivation

### Formula Proof

```
Given array: [a, b, c]

Sum of all products of pairs:
= a*b + a*c + b*c

Square of sum:
(a + b + c)² = a² + b² + c² + 2(a*b + a*c + b*c)

Rearranging:
2(a*b + a*c + b*c) = (a + b + c)² - (a² + b² + c²)

Therefore:
a*b + a*c + b*c = [(a + b + c)² - (a² + b² + c²)] / 2

General Formula:
Sum of products = [(Σaᵢ)² - Σ(aᵢ²)] / 2
```

---

## Algorithm Walkthrough

### Example: [1, 3, 4]

**Approach 1: Mathematical Formula**

```
Input: [1, 3, 4]

Step 1: Calculate sum of elements
  sum = 1 + 3 + 4 = 8

Step 2: Calculate sum of squares
  sumOfSquares = 1² + 3² + 4²
               = 1 + 9 + 16
               = 26

Step 3: Apply formula
  result = (sum² - sumOfSquares) / 2
         = (8² - 26) / 2
         = (64 - 26) / 2
         = 38 / 2
         = 19

Result: 19
```

**Approach 2: Brute Force**

```
Input: [1, 3, 4]

Pairs:
  i=0, j=1: arr[0] * arr[1] = 1 * 3 = 3
  i=0, j=2: arr[0] * arr[2] = 1 * 4 = 4
  i=1, j=2: arr[1] * arr[2] = 3 * 4 = 12

Sum = 3 + 4 + 12 = 19

Result: 19
```

**Approach 3: Accumulator Pattern**

```
Input: [1, 3, 4]

Initial: sum = 0, result = 0

Step 1: Process 1
  result += sum * 1 = 0 * 1 = 0
  sum += 1 = 1
  result = 0, sum = 1

Step 2: Process 3
  result += sum * 3 = 1 * 3 = 3
  sum += 3 = 4
  result = 3, sum = 4

Step 3: Process 4
  result += sum * 4 = 4 * 4 = 16
  sum += 4 = 8
  result = 3 + 16 = 19, sum = 8

Result: 19
```

### Example: [1, 2, 3, 4]

```
Mathematical Formula:

sum = 1 + 2 + 3 + 4 = 10
sumOfSquares = 1 + 4 + 9 + 16 = 30

result = (10² - 30) / 2
       = (100 - 30) / 2
       = 70 / 2
       = 35

Verification (Brute Force):
(1,2)=2, (1,3)=3, (1,4)=4, (2,3)=6, (2,4)=8, (3,4)=12
Sum = 2 + 3 + 4 + 6 + 8 + 12 = 35 ✓

Result: 35
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Mathematical Formula (Optimal)
    public static long sumOfProductPairs(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        
        long sum = 0, sumOfSquares = 0;
        
        for (int num : arr) {
            sum += num;
            sumOfSquares += (long) num * num;
        }
        
        return (sum * sum - sumOfSquares) / 2;
    }
    
    // Approach 2: Nested Loops (Brute Force)
    public static long sumOfProductPairsBruteForce(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        
        long sum = 0;
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                sum += (long) arr[i] * arr[j];
            }
        }
        
        return sum;
    }
    
    // Approach 3: Accumulator Pattern
    public static long sumOfProductPairsAccumulator(int[] arr) {
        if (arr == null || arr.length < 2) return 0;
        
        long sum = 0, result = 0;
        
        for (int num : arr) {
            result += sum * num;
            sum += num;
        }
        
        return result;
    }
    
    // Helper: Get all pairs with their products
    public static List<String> getAllPairs(int[] arr) {
        List<String> pairs = new ArrayList<>();
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                int product = arr[i] * arr[j];
                pairs.add("(" + arr[i] + "," + arr[j] + ")=" + product);
            }
        }
        
        return pairs;
    }
    
    // Helper: Count number of pairs
    public static int countPairs(int n) {
        return n * (n - 1) / 2;  // nC2 = n!/(2!(n-2)!)
    }
    
    // Variation: Sum of product of triplets
    public static long sumOfProductTriplets(int[] arr) {
        if (arr == null || arr.length < 3) return 0;
        
        long sum = 0;
        
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                for (int k = j + 1; k < arr.length; k++) {
                    sum += (long) arr[i] * arr[j] * arr[k];
                }
            }
        }
        
        return sum;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Basic case
        if (sumOfProductPairs(new int[]{1, 3, 4}) != 19) return false;
        
        // Test 2: Two elements
        if (sumOfProductPairs(new int[]{2, 3}) != 6) return false;
        
        // Test 3: Four elements
        if (sumOfProductPairs(new int[]{1, 2, 3, 4}) != 35) return false;
        
        // Test 4: Single element
        if (sumOfProductPairs(new int[]{5}) != 0) return false;
        
        // Test 5: Empty array
        if (sumOfProductPairs(new int[]{}) != 0) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        int[][] testCases = {
            {1, 3, 4},
            {2, 3},
            {1, 2, 3, 4},
            {5},
            {2, 4, 6}
        };
        
        for (int[] arr : testCases) {
            long result = sumOfProductPairs(arr);
            List<String> pairs = getAllPairs(arr);
            
            System.out.println("Input:  " + Arrays.toString(arr));
            System.out.println("Pairs:  " + pairs);
            System.out.println("Result: " + result);
            System.out.println();
        }
        
        // Compare approaches
        int[] test = {1, 3, 4};
        System.out.println("Comparing approaches for: " + Arrays.toString(test));
        System.out.println("Formula:     " + sumOfProductPairs(test));
        System.out.println("Brute Force: " + sumOfProductPairsBruteForce(test));
        System.out.println("Accumulator: " + sumOfProductPairsAccumulator(test));
    }
}
```

---

## Test Cases

```java
@Test
public void testSumOfProductPairs() {
    // Test 1: Basic case
    assertEquals(19, sumOfProductPairs(new int[]{1, 3, 4}));
    
    // Test 2: Two elements
    assertEquals(6, sumOfProductPairs(new int[]{2, 3}));
    
    // Test 3: Four elements
    assertEquals(35, sumOfProductPairs(new int[]{1, 2, 3, 4}));
    
    // Test 4: Single element
    assertEquals(0, sumOfProductPairs(new int[]{5}));
    
    // Test 5: Empty array
    assertEquals(0, sumOfProductPairs(new int[]{}));
    
    // Test 6: All same elements
    assertEquals(12, sumOfProductPairs(new int[]{2, 2, 2}));
    // (2,2)=4, (2,2)=4, (2,2)=4 → 12
    
    // Test 7: Negative numbers
    assertEquals(-2, sumOfProductPairs(new int[]{-1, 2, 3}));
    // (-1,2)=-2, (-1,3)=-3, (2,3)=6 → -2-3+6=1
    
    // Test 8: Large numbers
    assertEquals(50, sumOfProductPairs(new int[]{5, 10}));
    
    // Test 9: Zero in array
    assertEquals(6, sumOfProductPairs(new int[]{0, 2, 3}));
    // (0,2)=0, (0,3)=0, (2,3)=6 → 6
    
    // Test 10: Five elements
    assertEquals(85, sumOfProductPairs(new int[]{1, 2, 3, 4, 5}));
}
```

---

## Visual Representation

### Pair Generation

```
Array: [1, 3, 4]

All Pairs (i < j):
┌───────┬───────┬─────────┐
│ Pair  │ i,j   │ Product │
├───────┼───────┼─────────┤
│ (1,3) │ 0,1   │    3    │
│ (1,4) │ 0,2   │    4    │
│ (3,4) │ 1,2   │   12    │
└───────┴───────┴─────────┘

Sum = 3 + 4 + 12 = 19
```

### Mathematical Formula Visualization

```
Array: [1, 3, 4]

Step 1: Sum of elements
┌───┬───┬───┐
│ 1 │ 3 │ 4 │
└───┴───┴───┘
  ↓   ↓   ↓
Sum = 1 + 3 + 4 = 8

Step 2: Sum of squares
┌───┬───┬────┐
│ 1²│ 3²│ 4² │
└───┴───┴────┘
  ↓   ↓   ↓
  1 + 9 + 16 = 26

Step 3: Apply formula
(8² - 26) / 2 = (64 - 26) / 2 = 38 / 2 = 19
```

### Accumulator Pattern Flow

```
Array: [1, 3, 4]

┌──────┬─────┬────────────────┬─────┬────────┐
│ Step │ Num │ Operation      │ Sum │ Result │
├──────┼─────┼────────────────┼─────┼────────┤
│  0   │  -  │ Initialize     │  0  │   0    │
│  1   │  1  │ result+=0*1    │  1  │   0    │
│  2   │  3  │ result+=1*3    │  4  │   3    │
│  3   │  4  │ result+=4*4    │  8  │  19    │
└──────┴─────┴────────────────┴─────┴────────┘

Final Result: 19
```

---

## Edge Cases

1. **Empty array:** Return 0
2. **Null array:** Return 0
3. **Single element:** Return 0 (no pairs)
4. **Two elements:** Return their product
5. **All same elements:** Formula still works
6. **Negative numbers:** Handle correctly
7. **Zero in array:** Products with 0 are 0
8. **Large numbers:** Use long to avoid overflow
9. **Very large array:** O(n) solution efficient

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Mathematical Formula | O(n) | O(1) | **Optimal solution** |
| Brute Force | O(n²) | O(1) | Simple but slow |
| Accumulator | O(n) | O(1) | **Also optimal** |

**Where n = length of array**

**Time Complexity Breakdown:**
- Formula: Single pass to calculate sum and sum of squares: O(n)
- Brute Force: Nested loops generate all pairs: O(n²)
- Accumulator: Single pass with running sum: O(n)

**Space Complexity:**
- All approaches use constant extra space: O(1)

---

## Related Problems

1. **Sum of All Pairs** - Sum of all pair sums (not products)
2. **Product of All Pairs** - Product of all pair products
3. **Sum of Product of Triplets** - Extend to triplets
4. **Maximum Product Pair** - Find pair with max product
5. **Count Pairs with Sum K** - Count pairs summing to K
6. **Two Sum** - Find pair with given sum

---

## Interview Tips

### Clarification Questions
1. Can array have negative numbers? (Yes)
2. Can array have zeros? (Yes)
3. What if array is empty? (Return 0)
4. What if only one element? (Return 0, no pairs)
5. Handle overflow? (Use long)

### Approach Explanation
1. "I'll use the mathematical formula for optimal O(n) solution"
2. "Calculate sum of all elements and sum of squares"
3. "Apply formula: (sum² - sumOfSquares) / 2"
4. "This avoids O(n²) nested loops"
5. "Time O(n), Space O(1)"

### Common Mistakes
1. **Integer overflow** - Use long for products and sums
2. **Forgetting division by 2** - Formula requires /2
3. **Not handling edge cases** - Empty, single element
4. **Wrong pair counting** - Should be i < j, not i ≤ j
5. **Incorrect formula** - Must subtract sum of squares

### Follow-up Questions
1. "What about triplets?" - Extend to O(n³) or use formula
2. "Find actual pairs?" - Need O(n²) to enumerate
3. "Largest product pair?" - Sort and take last two
4. "Handle very large arrays?" - O(n) solution scales well
5. "Parallel processing?" - Can parallelize sum calculations

---

## Real-World Applications

1. **Statistics** - Covariance calculation
2. **Graph Theory** - Edge weight sums
3. **Machine Learning** - Feature interaction terms
4. **Finance** - Portfolio correlation analysis
5. **Physics** - Pairwise interaction energies
6. **Chemistry** - Molecular interaction calculations
7. **Network Analysis** - Connection strength metrics

---

## Key Takeaways

1. **Mathematical formula is optimal:** O(n) vs O(n²) brute force
2. **Formula: (sum² - sumOfSquares) / 2** - Elegant and efficient
3. **Accumulator pattern also O(n):** Alternative optimal approach
4. **Use long to avoid overflow:** Products can be large
5. **Edge cases matter:** Empty, single element, negatives
6. **Number of pairs: n(n-1)/2** - Combination formula nC2
7. **Mathematical insight saves time:** From O(n²) to O(n)

---

## Mathematical Insights

### Why the Formula Works

```
Expanding (a+b+c)²:
(a+b+c)² = a² + b² + c² + 2ab + 2ac + 2bc

Rearranging:
2(ab + ac + bc) = (a+b+c)² - (a² + b² + c²)

Therefore:
ab + ac + bc = [(a+b+c)² - (a² + b² + c²)] / 2

This generalizes to n elements:
Σᵢ<ⱼ aᵢ*aⱼ = [(Σaᵢ)² - Σ(aᵢ²)] / 2
```

### Accumulator Pattern Explanation

```
For array [a, b, c]:

Step 1: Process a
  result = 0, sum = a

Step 2: Process b
  result = a*b, sum = a+b

Step 3: Process c
  result = a*b + (a+b)*c
        = a*b + a*c + b*c ✓

This builds the sum incrementally!
```

---

## Optimization Notes

### Why O(n) is Better

```
Brute Force: O(n²)
for i = 0 to n-1:
    for j = i+1 to n-1:
        sum += arr[i] * arr[j]

Number of operations: n(n-1)/2 ≈ n²/2

Mathematical Formula: O(n)
sum = Σaᵢ
sumOfSquares = Σ(aᵢ²)
result = (sum² - sumOfSquares) / 2

Number of operations: 2n + 3 ≈ n
```

### Best Practice

```java
public static long sumOfProductPairs(int[] arr) {
    if (arr == null || arr.length < 2) {
        return 0;
    }
    
    long sum = 0;
    long sumOfSquares = 0;
    
    for (int num : arr) {
        sum += num;
        sumOfSquares += (long) num * num;  // Cast to avoid overflow
    }
    
    return (sum * sum - sumOfSquares) / 2;
}
```

---

## Variations

### 1. Sum of Product of Triplets

```java
// O(n³) brute force
public static long sumOfProductTriplets(int[] arr) {
    long sum = 0;
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            for (int k = j + 1; k < arr.length; k++) {
                sum += (long) arr[i] * arr[j] * arr[k];
            }
        }
    }
    return sum;
}
```

### 2. Maximum Product Pair

```java
public static long maxProductPair(int[] arr) {
    Arrays.sort(arr);
    int n = arr.length;
    
    // Either two largest or two smallest (if negative)
    long max1 = (long) arr[n-1] * arr[n-2];
    long max2 = (long) arr[0] * arr[1];
    
    return Math.max(max1, max2);
}
```

### 3. Count Pairs with Product K

```java
public static int countPairsWithProductK(int[] arr, int k) {
    int count = 0;
    for (int i = 0; i < arr.length; i++) {
        for (int j = i + 1; j < arr.length; j++) {
            if (arr[i] * arr[j] == k) {
                count++;
            }
        }
    }
    return count;
}
```
