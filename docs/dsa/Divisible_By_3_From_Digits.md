# Check if Digits Can Form Number Divisible by 3

## Problem Statement

Given an array of digits, determine if it's possible to form a number divisible by 3 using all the digits.

**Input:** Array of digits (0-9)  
**Output:** true if can form number divisible by 3, false otherwise

**Examples:**
```
Input: [1, 2, 3]
Output: true
Explanation: Sum = 1+2+3 = 6, divisible by 3
Can form: 123, 132, 213, 231, 312, 321 (all divisible by 3)

Input: [1, 2, 4]
Output: false
Explanation: Sum = 1+2+4 = 7, not divisible by 3

Input: [0, 0, 0]
Output: true
Explanation: Sum = 0, divisible by 3

Input: [3, 6, 9]
Output: true
Explanation: Sum = 18, divisible by 3

Input: [1]
Output: false
Explanation: Sum = 1, not divisible by 3
```

---

## Mathematical Background

### Divisibility Rule for 3

A number is divisible by 3 if and only if the sum of its digits is divisible by 3.

**Proof:**
```
Number: d₁d₂d₃...dₙ = d₁×10^(n-1) + d₂×10^(n-2) + ... + dₙ

Since 10 ≡ 1 (mod 3):
  10^k ≡ 1 (mod 3) for all k

Therefore:
  d₁×10^(n-1) + d₂×10^(n-2) + ... + dₙ ≡ d₁ + d₂ + ... + dₙ (mod 3)

The number is divisible by 3 ⟺ sum of digits is divisible by 3
```

**Key Insight:** Order doesn't matter! Only the sum of digits matters.

---

## Solution Approaches

### Approach 1: Sum of Digits (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean canFormDivisibleBy3(int[] digits) {
    int sum = 0;
    for (int digit : digits) {
        sum += digit;
    }
    return sum % 3 == 0;
}
```

---

### Approach 2: Modulo Arithmetic

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean canFormDivisibleBy3Mod(int[] digits) {
    int remainder = 0;
    for (int digit : digits) {
        remainder = (remainder + digit) % 3;
    }
    return remainder == 0;
}
```

---

### Approach 3: Using Streams

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean canFormDivisibleBy3Stream(int[] digits) {
    return Arrays.stream(digits).sum() % 3 == 0;
}
```

---

### Approach 4: Count Remainders

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean canFormDivisibleBy3Count(int[] digits) {
    int[] count = new int[3]; // count[i] = digits with remainder i
    
    for (int digit : digits) {
        count[digit % 3]++;
    }
    
    // Total remainder = (count[1] + 2*count[2]) % 3
    return (count[1] + 2 * count[2]) % 3 == 0;
}
```

---

## Algorithm Walkthrough

### Example: [1, 2, 3]

```
Sum approach:
  sum = 1 + 2 + 3 = 6
  6 % 3 = 0 ✓
  
Result: true

Possible numbers:
  123: 1+2+3 = 6, divisible by 3 ✓
  132: 1+3+2 = 6, divisible by 3 ✓
  213: 2+1+3 = 6, divisible by 3 ✓
  231: 2+3+1 = 6, divisible by 3 ✓
  312: 3+1+2 = 6, divisible by 3 ✓
  321: 3+2+1 = 6, divisible by 3 ✓
  
All permutations divisible by 3!
```

### Example: [1, 2, 4]

```
Sum approach:
  sum = 1 + 2 + 4 = 7
  7 % 3 = 1 ✗
  
Result: false

Check:
  124: 1+2+4 = 7, 7 % 3 = 1 ✗
  142: 1+4+2 = 7, 7 % 3 = 1 ✗
  214: 2+1+4 = 7, 7 % 3 = 1 ✗
  
No permutation divisible by 3!
```

### Example: [3, 6, 9]

```
Sum approach:
  sum = 3 + 6 + 9 = 18
  18 % 3 = 0 ✓
  
Result: true

Note: Each digit already divisible by 3
  3 % 3 = 0
  6 % 3 = 0
  9 % 3 = 0
  Sum of multiples of 3 is also multiple of 3
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Sum of digits (Optimal)
    public static boolean canFormDivisibleBy3(int[] digits) {
        int sum = 0;
        for (int digit : digits) {
            sum += digit;
        }
        return sum % 3 == 0;
    }
    
    // Approach 2: Modulo arithmetic (Avoid overflow)
    public static boolean canFormDivisibleBy3Mod(int[] digits) {
        int remainder = 0;
        for (int digit : digits) {
            remainder = (remainder + digit) % 3;
        }
        return remainder == 0;
    }
    
    // Approach 3: Using streams
    public static boolean canFormDivisibleBy3Stream(int[] digits) {
        return Arrays.stream(digits).sum() % 3 == 0;
    }
    
    // Approach 4: Count remainders
    public static boolean canFormDivisibleBy3Count(int[] digits) {
        int[] count = new int[3];
        
        for (int digit : digits) {
            count[digit % 3]++;
        }
        
        return (count[1] + 2 * count[2]) % 3 == 0;
    }
    
    // Return one valid number (if possible)
    public static String formNumber(int[] digits) {
        if (!canFormDivisibleBy3(digits)) {
            return "";
        }
        
        // Sort in descending order for largest number
        Integer[] boxed = Arrays.stream(digits).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, Collections.reverseOrder());
        
        StringBuilder sb = new StringBuilder();
        for (int digit : boxed) {
            sb.append(digit);
        }
        
        // Handle leading zeros
        String result = sb.toString();
        if (result.matches("0+")) {
            return "0";
        }
        
        return result;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Sum divisible by 3
        if (!canFormDivisibleBy3(new int[]{1, 2, 3})) return false;
        
        // Test 2: Sum not divisible by 3
        if (canFormDivisibleBy3(new int[]{1, 2, 4})) return false;
        
        // Test 3: All zeros
        if (!canFormDivisibleBy3(new int[]{0, 0, 0})) return false;
        
        // Test 4: All divisible by 3
        if (!canFormDivisibleBy3(new int[]{3, 6, 9})) return false;
        
        // Test 5: Single digit
        if (canFormDivisibleBy3(new int[]{1})) return false;
        if (!canFormDivisibleBy3(new int[]{3})) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[][] tests = {
            {1, 2, 3},
            {1, 2, 4},
            {0, 0, 0},
            {3, 6, 9},
            {1, 5, 7}
        };
        
        for (int[] digits : tests) {
            boolean result = canFormDivisibleBy3(digits);
            String number = formNumber(digits);
            
            System.out.println("Digits: " + Arrays.toString(digits));
            System.out.println("Can form divisible by 3: " + result);
            if (result) {
                System.out.println("Example number: " + number);
            }
            System.out.println();
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testCanFormDivisibleBy3() {
    // Test 1: Sum divisible by 3
    assertTrue(canFormDivisibleBy3(new int[]{1, 2, 3}));
    assertTrue(canFormDivisibleBy3(new int[]{3, 6, 9}));
    assertTrue(canFormDivisibleBy3(new int[]{0, 0, 0}));
    
    // Test 2: Sum not divisible by 3
    assertFalse(canFormDivisibleBy3(new int[]{1, 2, 4}));
    assertFalse(canFormDivisibleBy3(new int[]{1, 5, 7}));
    
    // Test 3: Single digit
    assertTrue(canFormDivisibleBy3(new int[]{0}));
    assertFalse(canFormDivisibleBy3(new int[]{1}));
    assertFalse(canFormDivisibleBy3(new int[]{2}));
    assertTrue(canFormDivisibleBy3(new int[]{3}));
    assertTrue(canFormDivisibleBy3(new int[]{6}));
    assertTrue(canFormDivisibleBy3(new int[]{9}));
    
    // Test 4: Two digits
    assertTrue(canFormDivisibleBy3(new int[]{1, 2})); // sum=3
    assertFalse(canFormDivisibleBy3(new int[]{1, 3})); // sum=4
    assertTrue(canFormDivisibleBy3(new int[]{2, 4})); // sum=6
    
    // Test 5: Large array
    assertTrue(canFormDivisibleBy3(new int[]{1, 1, 1})); // sum=3
    assertTrue(canFormDivisibleBy3(new int[]{9, 9, 9})); // sum=27
    
    // Test 6: Empty array
    assertTrue(canFormDivisibleBy3(new int[]{})); // sum=0
}
```

---

## Visual Representation

### Divisibility Rule Visualization

```
Number: 123
Digits: 1, 2, 3
Sum: 1 + 2 + 3 = 6
6 % 3 = 0 ✓

All permutations:
123 = 1×100 + 2×10 + 3×1 = 123, 123 % 3 = 0 ✓
132 = 1×100 + 3×10 + 2×1 = 132, 132 % 3 = 0 ✓
213 = 2×100 + 1×10 + 3×1 = 213, 213 % 3 = 0 ✓
231 = 2×100 + 3×10 + 1×1 = 231, 231 % 3 = 0 ✓
312 = 3×100 + 1×10 + 2×1 = 312, 312 % 3 = 0 ✓
321 = 3×100 + 2×10 + 1×1 = 321, 321 % 3 = 0 ✓

All divisible by 3!
```

### Remainder Analysis

```
Digits: [1, 2, 4]

Remainders when divided by 3:
  1 % 3 = 1
  2 % 3 = 2
  4 % 3 = 1

Sum of remainders: 1 + 2 + 1 = 4
4 % 3 = 1 ≠ 0 ✗

Therefore, sum = 7, 7 % 3 = 1 ✗
Cannot form number divisible by 3
```

---

## Edge Cases

1. **Empty array:** [] → true (sum = 0)
2. **Single zero:** [0] → true
3. **All zeros:** [0, 0, 0] → true
4. **Single digit divisible:** [3], [6], [9] → true
5. **Single digit not divisible:** [1], [2], [4] → false
6. **All same digit:** [1, 1, 1] → true (sum = 3)
7. **Large array:** Works for any size

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Sum | O(n) | O(1) | **Optimal** |
| Modulo | O(n) | O(1) | Avoids overflow |
| Streams | O(n) | O(1) | Functional style |
| Count Remainders | O(n) | O(1) | Alternative |

**Why O(n)?**
- Must examine each digit once
- Sum/modulo operation is O(1)

---

## Related Problems

1. **Divisible by 9** - Sum of digits divisible by 9
2. **Divisible by 11** - Alternating sum rule
3. **Largest Number** - Arrange digits for max
4. **Smallest Number** - Arrange digits for min
5. **Remove K Digits** - Form smallest after removal
6. **Valid Number** - Check if string is valid number

---

## Interview Tips

### Clarification Questions
1. Can array be empty? (Yes, sum = 0, divisible by 3)
2. Are digits 0-9? (Yes, single digits)
3. Can we rearrange? (Yes, any order works)
4. Return boolean or actual number? (Usually boolean)
5. Handle leading zeros? (If forming number)

### Approach Explanation
1. "Use divisibility rule for 3"
2. "A number is divisible by 3 iff sum of digits is"
3. "Order doesn't matter, only sum matters"
4. "Calculate sum and check if divisible by 3"
5. "O(n) time, O(1) space"

### Common Mistakes
- Trying to generate all permutations (unnecessary)
- Not knowing divisibility rule
- Forgetting empty array case
- Integer overflow for large sums
- Not handling leading zeros when forming number

### Why Order Doesn't Matter

```
123: 1×10² + 2×10¹ + 3×10⁰
321: 3×10² + 2×10¹ + 1×10⁰

Since 10 ≡ 1 (mod 3):
  10² ≡ 1 (mod 3)
  10¹ ≡ 1 (mod 3)
  10⁰ ≡ 1 (mod 3)

Both reduce to: (1 + 2 + 3) mod 3
Order irrelevant!
```

---

## Real-World Applications

1. **Data Validation** - Check number properties
2. **Checksum Algorithms** - Digit sum validation
3. **Barcode Verification** - Check digit validation
4. **Credit Card Validation** - Luhn algorithm variant
5. **ISBN Validation** - Book number verification
6. **Error Detection** - Data integrity checks

---

## Key Takeaways

1. **Divisibility Rule:** Number divisible by 3 ⟺ sum of digits divisible by 3
2. **Order Independent:** Any arrangement has same divisibility
3. **Time Complexity:** O(n) to sum digits
4. **Space Complexity:** O(1) - only need sum
5. **No Permutations:** Don't need to generate all arrangements
6. **Modulo Arithmetic:** Can use (sum % 3 == 0)
7. **Edge Cases:** Empty array (sum=0) is divisible by 3

---

## Additional Notes

### Divisibility Rules for Other Numbers

```
Divisible by 2: Last digit even
Divisible by 3: Sum of digits divisible by 3 ✓
Divisible by 4: Last 2 digits divisible by 4
Divisible by 5: Last digit 0 or 5
Divisible by 6: Divisible by both 2 and 3
Divisible by 9: Sum of digits divisible by 9
Divisible by 10: Last digit 0
Divisible by 11: Alternating sum divisible by 11
```

### Divisible by 9 (Similar Problem)

```java
public static boolean canFormDivisibleBy9(int[] digits) {
    int sum = 0;
    for (int digit : digits) {
        sum += digit;
    }
    return sum % 9 == 0;
}

// Same logic! 9 has similar property as 3
```

### Divisible by 11 (Different Rule)

```java
public static boolean canFormDivisibleBy11(int[] digits) {
    // For 11: alternating sum must be divisible by 11
    // But order matters for 11!
    // Cannot determine without trying arrangements
    
    // This problem is harder - need to check permutations
    return false; // Cannot determine easily
}
```

### Form Largest Number Divisible by 3

```java
public static String largestDivisibleBy3(int[] digits) {
    if (!canFormDivisibleBy3(digits)) {
        return "";
    }
    
    // Sort in descending order
    Integer[] boxed = Arrays.stream(digits).boxed().toArray(Integer[]::new);
    Arrays.sort(boxed, Collections.reverseOrder());
    
    StringBuilder sb = new StringBuilder();
    for (int digit : boxed) {
        sb.append(digit);
    }
    
    String result = sb.toString();
    return result.matches("0+") ? "0" : result;
}

// Example: [3, 1, 4, 1, 5, 9] → "954311" (if sum divisible by 3)
```

### Avoid Integer Overflow

```java
// For very large arrays, sum might overflow
public static boolean canFormDivisibleBy3Safe(int[] digits) {
    int remainder = 0;
    for (int digit : digits) {
        remainder = (remainder + digit) % 3;
    }
    return remainder == 0;
}

// Taking modulo at each step prevents overflow
```

### Mathematical Proof

```
Claim: n ≡ sum_of_digits(n) (mod 3)

Proof:
  Let n = dₖ×10^k + ... + d₁×10 + d₀
  
  Since 10 ≡ 1 (mod 3):
    10^i ≡ 1^i ≡ 1 (mod 3) for all i
  
  Therefore:
    n ≡ dₖ×1 + ... + d₁×1 + d₀ (mod 3)
    n ≡ dₖ + ... + d₁ + d₀ (mod 3)
    n ≡ sum_of_digits(n) (mod 3)
  
  QED
```

### Why This Works for Any Base

```
In base b, if b ≡ 1 (mod m):
  Then number ≡ sum_of_digits (mod m)

For base 10:
  10 ≡ 1 (mod 3) ✓ → Works for 3
  10 ≡ 1 (mod 9) ✓ → Works for 9
  10 ≡ -1 (mod 11) → Alternating sum for 11
```

### Extension: Remove Minimum Digits

```java
// Remove minimum digits to make sum divisible by 3
public static int minRemovalsForDivisibleBy3(int[] digits) {
    int sum = Arrays.stream(digits).sum();
    int remainder = sum % 3;
    
    if (remainder == 0) return 0;
    
    // Need to remove digits with total remainder = remainder
    int[] count = new int[3];
    for (int digit : digits) {
        count[digit % 3]++;
    }
    
    if (remainder == 1) {
        // Remove one digit with remainder 1, or two with remainder 2
        if (count[1] > 0) return 1;
        if (count[2] >= 2) return 2;
    } else { // remainder == 2
        // Remove one digit with remainder 2, or two with remainder 1
        if (count[2] > 0) return 1;
        if (count[1] >= 2) return 2;
    }
    
    return -1; // Cannot make divisible by 3
}
```
