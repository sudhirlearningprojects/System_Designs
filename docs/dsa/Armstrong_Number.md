# Armstrong Number (Narcissistic Number)

## Problem Statement

An Armstrong number (also called narcissistic number) is a number that is equal to the sum of its own digits each raised to the power of the number of digits.

**Definition:** For an n-digit number, if the sum of each digit raised to the power n equals the number itself, it's an Armstrong number.

**Input:** Integer  
**Output:** true if Armstrong number, false otherwise

**Examples:**
```
Input: 153
Output: true
Explanation: 1^3 + 5^3 + 3^3 = 1 + 125 + 27 = 153

Input: 371
Output: true
Explanation: 3^3 + 7^3 + 1^3 = 27 + 343 + 1 = 371

Input: 9474
Output: true
Explanation: 9^4 + 4^4 + 7^4 + 4^4 = 6561 + 256 + 2401 + 256 = 9474

Input: 123
Output: false
Explanation: 1^3 + 2^3 + 3^3 = 1 + 8 + 27 = 36 ≠ 123

Input: 0
Output: true
Explanation: 0^1 = 0

Input: 1
Output: true
Explanation: 1^1 = 1
```

---

## Solution Approaches

### Approach 1: Extract Digits (Optimal)

**Time Complexity:** O(d) where d = number of digits  
**Space Complexity:** O(1)

```java
public static boolean isArmstrong(int num) {
    if (num < 0) return false;
    
    int original = num;
    int digits = String.valueOf(num).length();
    int sum = 0;
    
    while (num > 0) {
        int digit = num % 10;
        sum += Math.pow(digit, digits);
        num /= 10;
    }
    
    return sum == original;
}
```

---

### Approach 2: Count Digits First

**Time Complexity:** O(d)  
**Space Complexity:** O(1)

```java
public static boolean isArmstrongCountFirst(int num) {
    if (num < 0) return false;
    
    int original = num;
    int digits = countDigits(num);
    int sum = 0;
    
    while (num > 0) {
        int digit = num % 10;
        sum += Math.pow(digit, digits);
        num /= 10;
    }
    
    return sum == original;
}

private static int countDigits(int num) {
    if (num == 0) return 1;
    int count = 0;
    while (num > 0) {
        count++;
        num /= 10;
    }
    return count;
}
```

---

### Approach 3: Using String

**Time Complexity:** O(d)  
**Space Complexity:** O(d)

```java
public static boolean isArmstrongString(int num) {
    if (num < 0) return false;
    
    String str = String.valueOf(num);
    int digits = str.length();
    int sum = 0;
    
    for (char c : str.toCharArray()) {
        int digit = c - '0';
        sum += Math.pow(digit, digits);
    }
    
    return sum == num;
}
```

---

### Approach 4: Without Math.pow

**Time Complexity:** O(d²)  
**Space Complexity:** O(1)

```java
public static boolean isArmstrongNoPow(int num) {
    if (num < 0) return false;
    
    int original = num;
    int digits = String.valueOf(num).length();
    int sum = 0;
    
    while (num > 0) {
        int digit = num % 10;
        sum += power(digit, digits);
        num /= 10;
    }
    
    return sum == original;
}

private static int power(int base, int exp) {
    int result = 1;
    for (int i = 0; i < exp; i++) {
        result *= base;
    }
    return result;
}
```

---

## Algorithm Walkthrough

### Example: 153

```
Number: 153
Digits: 3

Step 1: Extract last digit
  digit = 153 % 10 = 3
  sum = 0 + 3^3 = 0 + 27 = 27
  num = 153 / 10 = 15

Step 2: Extract next digit
  digit = 15 % 10 = 5
  sum = 27 + 5^3 = 27 + 125 = 152
  num = 15 / 10 = 1

Step 3: Extract next digit
  digit = 1 % 10 = 1
  sum = 152 + 1^3 = 152 + 1 = 153
  num = 1 / 10 = 0

Step 4: Check
  sum == original? 153 == 153? Yes

Result: true
```

### Example: 123

```
Number: 123
Digits: 3

Step 1: digit = 3, sum = 3^3 = 27, num = 12
Step 2: digit = 2, sum = 27 + 2^3 = 27 + 8 = 35, num = 1
Step 3: digit = 1, sum = 35 + 1^3 = 35 + 1 = 36, num = 0

Check: 36 == 123? No

Result: false
```

### Example: 9474 (4-digit)

```
Number: 9474
Digits: 4

Step 1: digit = 4, sum = 4^4 = 256, num = 947
Step 2: digit = 7, sum = 256 + 7^4 = 256 + 2401 = 2657, num = 94
Step 3: digit = 4, sum = 2657 + 4^4 = 2657 + 256 = 2913, num = 9
Step 4: digit = 9, sum = 2913 + 9^4 = 2913 + 6561 = 9474, num = 0

Check: 9474 == 9474? Yes

Result: true
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Extract digits (Optimal)
    public static boolean isArmstrong(int num) {
        if (num < 0) return false;
        
        int original = num;
        int digits = String.valueOf(num).length();
        int sum = 0;
        
        while (num > 0) {
            int digit = num % 10;
            sum += Math.pow(digit, digits);
            num /= 10;
        }
        
        return sum == original;
    }
    
    // Approach 2: Count digits first
    public static boolean isArmstrongCountFirst(int num) {
        if (num < 0) return false;
        
        int original = num;
        int digits = countDigits(num);
        int sum = 0;
        
        while (num > 0) {
            int digit = num % 10;
            sum += Math.pow(digit, digits);
            num /= 10;
        }
        
        return sum == original;
    }
    
    private static int countDigits(int num) {
        if (num == 0) return 1;
        int count = 0;
        while (num > 0) {
            count++;
            num /= 10;
        }
        return count;
    }
    
    // Approach 3: Using string
    public static boolean isArmstrongString(int num) {
        if (num < 0) return false;
        
        String str = String.valueOf(num);
        int digits = str.length();
        int sum = 0;
        
        for (char c : str.toCharArray()) {
            int digit = c - '0';
            sum += Math.pow(digit, digits);
        }
        
        return sum == num;
    }
    
    // Find all Armstrong numbers up to n
    public static List<Integer> findArmstrongNumbers(int n) {
        List<Integer> result = new ArrayList<>();
        
        for (int i = 0; i <= n; i++) {
            if (isArmstrong(i)) {
                result.add(i);
            }
        }
        
        return result;
    }
    
    public static boolean doTestsPass() {
        // Test 1: 3-digit Armstrong numbers
        if (!isArmstrong(153)) return false;
        if (!isArmstrong(370)) return false;
        if (!isArmstrong(371)) return false;
        if (!isArmstrong(407)) return false;
        
        // Test 2: Not Armstrong
        if (isArmstrong(123)) return false;
        if (isArmstrong(100)) return false;
        
        // Test 3: Single digit (all are Armstrong)
        if (!isArmstrong(0)) return false;
        if (!isArmstrong(1)) return false;
        if (!isArmstrong(9)) return false;
        
        // Test 4: 4-digit Armstrong
        if (!isArmstrong(9474)) return false;
        
        // Test 5: Negative
        if (isArmstrong(-153)) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo: Find all Armstrong numbers up to 10000
        System.out.println("\nArmstrong numbers up to 10000:");
        List<Integer> armstrong = findArmstrongNumbers(10000);
        System.out.println(armstrong);
        
        // Test specific numbers
        int[] tests = {0, 1, 153, 370, 371, 407, 123, 9474};
        System.out.println("\nTest results:");
        for (int num : tests) {
            System.out.println(num + " → " + isArmstrong(num));
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testIsArmstrong() {
    // Test 1: Single digit (all are Armstrong)
    assertTrue(isArmstrong(0));
    assertTrue(isArmstrong(1));
    assertTrue(isArmstrong(2));
    assertTrue(isArmstrong(9));
    
    // Test 2: 3-digit Armstrong numbers
    assertTrue(isArmstrong(153));
    assertTrue(isArmstrong(370));
    assertTrue(isArmstrong(371));
    assertTrue(isArmstrong(407));
    
    // Test 3: Not Armstrong
    assertFalse(isArmstrong(10));
    assertFalse(isArmstrong(123));
    assertFalse(isArmstrong(100));
    assertFalse(isArmstrong(999));
    
    // Test 4: 4-digit Armstrong numbers
    assertTrue(isArmstrong(1634));
    assertTrue(isArmstrong(8208));
    assertTrue(isArmstrong(9474));
    
    // Test 5: 5-digit Armstrong numbers
    assertTrue(isArmstrong(54748));
    assertTrue(isArmstrong(92727));
    
    // Test 6: Negative numbers
    assertFalse(isArmstrong(-1));
    assertFalse(isArmstrong(-153));
}
```

---

## Visual Representation

### Calculation Breakdown

```
153 (3 digits):
  1^3 = 1 × 1 × 1 = 1
  5^3 = 5 × 5 × 5 = 125
  3^3 = 3 × 3 × 3 = 27
  Sum = 1 + 125 + 27 = 153 ✓

371 (3 digits):
  3^3 = 27
  7^3 = 343
  1^3 = 1
  Sum = 27 + 343 + 1 = 371 ✓

9474 (4 digits):
  9^4 = 6561
  4^4 = 256
  7^4 = 2401
  4^4 = 256
  Sum = 6561 + 256 + 2401 + 256 = 9474 ✓

123 (3 digits):
  1^3 = 1
  2^3 = 8
  3^3 = 27
  Sum = 1 + 8 + 27 = 36 ≠ 123 ✗
```

### All Armstrong Numbers by Digit Count

```
1-digit: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
  (All single digits are Armstrong)

2-digit: None

3-digit: 153, 370, 371, 407

4-digit: 1634, 8208, 9474

5-digit: 54748, 92727, 93084

6-digit: 548834

7-digit: 1741725, 4210818, 9800817, 9926315

8-digit: 24678050, 24678051, 88593477

9-digit: 146511208, 472335975, 534494836, 912985153
```

---

## Edge Cases

1. **Zero:** 0 → true (0^1 = 0)
2. **Single digit:** 1-9 → all true
3. **Negative:** -153 → false
4. **Two digits:** None are Armstrong
5. **Large numbers:** Check overflow
6. **Leading zeros:** Not applicable (integers)

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Extract Digits | O(d) | O(1) | **Optimal** |
| Count First | O(d) | O(1) | Two passes |
| String | O(d) | O(d) | String allocation |
| No Math.pow | O(d²) | O(1) | Manual power |

**Where d = number of digits**

**Why O(d)?**
- Must process each digit once
- Math.pow is O(1) for small exponents

---

## Related Problems

1. **Happy Number** - Sum of squares of digits
2. **Perfect Number** - Sum of divisors equals number
3. **Harshad Number** - Divisible by sum of digits
4. **Disarium Number** - Sum of digits raised to position
5. **Automorphic Number** - Square ends with number
6. **Palindrome Number** - Reads same forwards/backwards

---

## Interview Tips

### Clarification Questions
1. Can number be negative? (Usually no)
2. What about zero? (Yes, 0^1 = 0)
3. Range of input? (Check for overflow)
4. Return boolean or print? (Usually boolean)
5. Need to find all Armstrong numbers? (Clarify)

### Approach Explanation
1. "Count number of digits"
2. "Extract each digit using modulo and division"
3. "Raise each digit to power of digit count"
4. "Sum all powered digits"
5. "Compare sum with original number"

### Common Mistakes
- Using wrong power (always use digit count, not 3)
- Not handling single digits
- Integer overflow for large numbers
- Modifying original number before comparison
- Not handling zero correctly

### Why Save Original Number?

```java
// ✓ Correct: Save original
int original = num;
while (num > 0) {
    // Process num
}
return sum == original;

// ✗ Wrong: Lose original value
while (num > 0) {
    // Process num
}
return sum == num; // num is now 0!
```

---

## Real-World Applications

1. **Number Theory** - Mathematical curiosity
2. **Cryptography** - Special number properties
3. **Puzzles** - Mathematical games
4. **Education** - Teaching digit manipulation
5. **Code Challenges** - Interview problems
6. **Pattern Recognition** - Number patterns

---

## Key Takeaways

1. **Definition:** Sum of digits^(digit_count) equals number
2. **All Single Digits:** Are Armstrong numbers
3. **No 2-Digit:** Armstrong numbers exist
4. **Time Complexity:** O(d) where d = digits
5. **Space Complexity:** O(1) for optimal solution
6. **Save Original:** Before modifying number
7. **Power:** Use digit count, not fixed value

---

## Additional Notes

### Why No 2-Digit Armstrong Numbers?

```
For 2-digit number ab (10a + b):
  a^2 + b^2 = 10a + b

Maximum: 9^2 + 9^2 = 81 + 81 = 162
Minimum 2-digit: 10

But: 1^2 + 0^2 = 1 ≠ 10
     9^2 + 9^2 = 162 > 99

No 2-digit number satisfies the condition.
```

### Optimization: Early Exit

```java
// If sum exceeds original, can exit early
if (sum > original) {
    return false;
}
```

### Finding All Armstrong Numbers

```java
public static List<Integer> findAllArmstrong(int limit) {
    List<Integer> result = new ArrayList<>();
    
    for (int i = 0; i <= limit; i++) {
        if (isArmstrong(i)) {
            result.add(i);
        }
    }
    
    return result;
}

// Up to 10000: [0,1,2,3,4,5,6,7,8,9,153,370,371,407,1634,8208,9474]
```

### Disarium Number (Variant)

```java
// Sum of digits raised to their position
// Example: 135 = 1^1 + 3^2 + 5^3 = 1 + 9 + 125 = 135
public static boolean isDisarium(int num) {
    String str = String.valueOf(num);
    int sum = 0;
    
    for (int i = 0; i < str.length(); i++) {
        int digit = str.charAt(i) - '0';
        sum += Math.pow(digit, i + 1);
    }
    
    return sum == num;
}
```

### Perfect Digital Invariant

```java
// Generalization: sum of f(digits) = number
// Armstrong: f(d) = d^n
// Happy: f(d) = d^2
// Factorial: f(d) = d!
```

### Largest Armstrong Numbers

```
By digit count:
  1-digit: 9
  3-digit: 407
  4-digit: 9474
  5-digit: 93084
  6-digit: 548834
  7-digit: 9926315
  8-digit: 88593477
  9-digit: 912985153
  10-digit: 4679307774
  
Beyond 10 digits: Very rare
```

### Integer Overflow Check

```java
// For large numbers, use long
public static boolean isArmstrongLong(long num) {
    if (num < 0) return false;
    
    long original = num;
    int digits = String.valueOf(num).length();
    long sum = 0;
    
    while (num > 0) {
        long digit = num % 10;
        sum += (long) Math.pow(digit, digits);
        num /= 10;
    }
    
    return sum == original;
}
```

### Performance Comparison

```
Math.pow vs Manual:
  Math.pow: O(1) but slower (floating point)
  Manual: O(d) but faster for small d
  
For d ≤ 10: Manual is faster
For d > 10: Math.pow is acceptable
```

### Why Armstrong Numbers are Rare

```
As digits increase, number grows exponentially
But sum of powers grows linearly

Example: 10-digit number
  Max number: 9,999,999,999
  Max sum: 10 × 9^10 = 34,867,844,010
  
Only 88 Armstrong numbers exist in total!
```

### Complete List (All 88 Armstrong Numbers)

```
1-digit: 0,1,2,3,4,5,6,7,8,9 (10 numbers)
3-digit: 153,370,371,407 (4 numbers)
4-digit: 1634,8208,9474 (3 numbers)
5-digit: 54748,92727,93084 (3 numbers)
6-digit: 548834 (1 number)
7-digit: 1741725,4210818,9800817,9926315 (4 numbers)
8-digit: 24678050,24678051,88593477 (3 numbers)
9-digit: 146511208,472335975,534494836,912985153 (4 numbers)
10-digit: 4679307774 (1 number)
11-digit: 32164049650,32164049651,40028394225,42678290603,44708635679,49388550606,82693916578,94204591914 (8 numbers)
...
39-digit: Last known Armstrong number

Total: 88 Armstrong numbers exist
```
