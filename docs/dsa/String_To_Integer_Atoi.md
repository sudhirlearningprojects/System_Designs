# String to Integer (atoi)

## Problem Statement

Convert a string to an integer, similar to C's `atoi` function.

**Rules:**
1. Skip leading whitespace
2. Handle optional '+' or '-' sign
3. Convert digits until non-digit character
4. Handle overflow (return INT_MAX or INT_MIN)

**Input:** String  
**Output:** Integer

**Examples:**
```
atoi("42")        → 42
atoi("   -42")    → -42
atoi("4193 with") → 4193
atoi("words 987") → 0
atoi("-91283472332") → -2147483648 (INT_MIN)
```

---

## Solution Approach

### Optimal: Single Pass

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static int atoi(String str) {
    int i = 0, sign = 1;
    long result = 0;
    
    // Skip whitespace
    while (i < str.length() && str.charAt(i) == ' ') {
        i++;
    }
    
    // Handle sign
    if (i < str.length() && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
        sign = str.charAt(i) == '-' ? -1 : 1;
        i++;
    }
    
    // Convert digits
    while (i < str.length() && Character.isDigit(str.charAt(i))) {
        result = result * 10 + (str.charAt(i) - '0');
        i++;
        
        // Check overflow
        if (result * sign > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (result * sign < Integer.MIN_VALUE) return Integer.MIN_VALUE;
    }
    
    return (int) (result * sign);
}
```

---

## Algorithm Walkthrough

### Example 1: "  -42"

```
Step 1: Skip whitespace
  i=0: ' ' → skip
  i=1: ' ' → skip
  i=2: '-' → stop

Step 2: Handle sign
  char='-' → sign = -1
  i=3

Step 3: Convert digits
  i=3: '4' → result = 0*10 + 4 = 4
  i=4: '2' → result = 4*10 + 2 = 42
  i=5: end of string

Step 4: Apply sign
  result = 42 * (-1) = -42

Result: -42
```

### Example 2: "4193 with words"

```
Step 1: No whitespace
  i=0

Step 2: No sign
  i=0

Step 3: Convert digits
  i=0: '4' → result = 4
  i=1: '1' → result = 41
  i=2: '9' → result = 419
  i=3: '3' → result = 4193
  i=4: ' ' → not a digit, stop

Result: 4193
```

### Example 3: "words 987"

```
Step 1: No whitespace
  i=0

Step 2: No sign
  i=0

Step 3: Convert digits
  i=0: 'w' → not a digit, stop
  result = 0

Result: 0
```

---

## Complete Implementation

```java
public class StringToInteger {
    
    // Approach 1: With overflow handling (Recommended)
    public static int atoi(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        
        int i = 0, sign = 1;
        long result = 0;
        
        // Skip leading whitespace
        while (i < str.length() && str.charAt(i) == ' ') {
            i++;
        }
        
        // Handle sign
        if (i < str.length() && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
            sign = str.charAt(i) == '-' ? -1 : 1;
            i++;
        }
        
        // Convert digits
        while (i < str.length() && Character.isDigit(str.charAt(i))) {
            result = result * 10 + (str.charAt(i) - '0');
            i++;
            
            // Check overflow
            if (result * sign > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (result * sign < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
        }
        
        return (int) (result * sign);
    }
    
    // Approach 2: Simple version (no overflow check)
    public static int atoiSimple(String str) {
        int result = 0;
        int sign = 1;
        int i = 0;
        
        while (i < str.length() && str.charAt(i) == ' ') i++;
        
        if (i < str.length() && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
            sign = str.charAt(i++) == '-' ? -1 : 1;
        }
        
        while (i < str.length() && Character.isDigit(str.charAt(i))) {
            result = result * 10 + (str.charAt(i++) - '0');
        }
        
        return result * sign;
    }
    
    // Approach 3: Using try-catch (not recommended)
    public static int atoiTryCatch(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // Bonus: Parse with base (binary, octal, hex)
    public static int atoiWithBase(String str, int base) {
        int result = 0;
        int sign = 1;
        int i = 0;
        
        if (str.charAt(0) == '-') {
            sign = -1;
            i = 1;
        }
        
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit;
            
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'z') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'Z') {
                digit = c - 'A' + 10;
            } else {
                break;
            }
            
            if (digit >= base) break;
            result = result * base + digit;
        }
        
        return result * sign;
    }
}
```

---

## Test Cases

```java
@Test
public void testAtoi() {
    // Basic cases
    assertEquals(42, atoi("42"));
    assertEquals(-42, atoi("-42"));
    assertEquals(42, atoi("+42"));
    
    // With whitespace
    assertEquals(42, atoi("   42"));
    assertEquals(-42, atoi("  -42"));
    
    // With trailing characters
    assertEquals(4193, atoi("4193 with words"));
    assertEquals(0, atoi("words and 987"));
    
    // Edge cases
    assertEquals(0, atoi(""));
    assertEquals(0, atoi("   "));
    assertEquals(0, atoi("abc"));
    
    // Overflow
    assertEquals(Integer.MAX_VALUE, atoi("2147483648"));
    assertEquals(Integer.MIN_VALUE, atoi("-2147483649"));
    assertEquals(Integer.MAX_VALUE, atoi("99999999999"));
    
    // Sign only
    assertEquals(0, atoi("+"));
    assertEquals(0, atoi("-"));
    
    // Multiple signs
    assertEquals(0, atoi("+-12"));
    assertEquals(0, atoi("-+12"));
    
    // Leading zeros
    assertEquals(123, atoi("00123"));
    assertEquals(-123, atoi("-00123"));
}
```

---

## Visual Representation

```
Input: "  -42abc"

Index:  0 1 2 3 4 5 6 7
Char:   ' ' ' ' '-' '4' '2' 'a' 'b' 'c'
        └─┬─┘ │   └─┬─┘ └─────┬─────┘
        skip  sign digits  ignore

Process:
1. Skip spaces (i=0,1)
2. Read sign '-' (i=2)
3. Read '4' → result = 4
4. Read '2' → result = 42
5. Read 'a' → stop (not digit)
6. Apply sign: -42
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `0` | Empty string |
| `"   "` | `0` | Only whitespace |
| `"abc"` | `0` | No digits |
| `"+1"` | `1` | Positive sign |
| `"-1"` | `-1` | Negative sign |
| `"00123"` | `123` | Leading zeros |
| `"2147483648"` | `2147483647` | Overflow to MAX |
| `"-2147483649"` | `-2147483648` | Overflow to MIN |
| `"+-12"` | `0` | Multiple signs |

---

## Common Mistakes

1. **Not Handling Overflow:**
   ```java
   // WRONG - can overflow
   result = result * 10 + digit;
   
   // CORRECT - use long and check
   long result = 0;
   if (result * sign > Integer.MAX_VALUE) return Integer.MAX_VALUE;
   ```

2. **Not Stopping at Non-Digit:**
   ```java
   // WRONG - continues past non-digits
   for (char c : str.toCharArray())
   
   // CORRECT - stop at first non-digit
   while (i < str.length() && Character.isDigit(str.charAt(i)))
   ```

3. **Multiple Signs:**
   ```java
   // Handle only first sign
   if (i < str.length() && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
       sign = str.charAt(i++) == '-' ? -1 : 1;
   }
   ```

4. **Not Trimming Whitespace:**
   ```java
   // Skip leading whitespace
   while (i < str.length() && str.charAt(i) == ' ') i++;
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Manual parsing | O(n) | O(1) | Optimal |
| Integer.parseInt | O(n) | O(1) | Built-in |
| Regex | O(n) | O(n) | Overkill |

---

## Character to Digit Conversion

```java
// ASCII values:
'0' = 48
'1' = 49
...
'9' = 57

// Convert char to digit:
int digit = '5' - '0';  // 53 - 48 = 5

// Build number:
result = result * 10 + digit;

Example: "123"
  '1' → result = 0*10 + 1 = 1
  '2' → result = 1*10 + 2 = 12
  '3' → result = 12*10 + 3 = 123
```

---

## Overflow Detection

### Method 1: Use Long
```java
long result = 0;
// ... build result
if (result * sign > Integer.MAX_VALUE) return Integer.MAX_VALUE;
if (result * sign < Integer.MIN_VALUE) return Integer.MIN_VALUE;
```

### Method 2: Check Before Multiply
```java
if (result > Integer.MAX_VALUE / 10 || 
    (result == Integer.MAX_VALUE / 10 && digit > 7)) {
    return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
}
```

---

## Related Problems

- **LeetCode 8:** String to Integer (atoi)
- **LeetCode 7:** Reverse Integer
- **LeetCode 65:** Valid Number
- **String parsing problems**

---

## Interview Tips

1. **Clarify Requirements:**
   - Handle overflow?
   - Leading zeros?
   - Multiple signs?
   - Whitespace handling?

2. **State the Algorithm:**
   - Skip whitespace
   - Check sign
   - Convert digits
   - Handle overflow

3. **Walk Through Example:**
   - Use "  -42"
   - Show each step

4. **Discuss Edge Cases:**
   - Empty string
   - No digits
   - Overflow
   - Multiple signs

5. **Mention Optimizations:**
   - Single pass O(n)
   - Constant space O(1)

---

## Real-World Applications

- **Input Validation:** User input parsing
- **Configuration Files:** Reading numeric values
- **Command Line Arguments:** Parsing parameters
- **Data Import:** CSV/text file processing
- **Protocol Parsing:** Network data conversion
- **Calculator Applications:** Expression evaluation

---

## Integer Limits

```java
Integer.MAX_VALUE =  2147483647  (2^31 - 1)
Integer.MIN_VALUE = -2147483648  (-2^31)

Overflow examples:
"2147483648"  → 2147483647 (MAX)
"-2147483649" → -2147483648 (MIN)
"99999999999" → 2147483647 (MAX)
```

---

## Key Takeaways

✅ Skip leading whitespace first  
✅ Handle optional +/- sign  
✅ Convert digits: `result = result * 10 + (c - '0')`  
✅ Stop at first non-digit character  
✅ Use `long` to detect overflow  
✅ Return INT_MAX/INT_MIN on overflow  
✅ O(n) time, O(1) space - optimal  
✅ Handle edge cases: empty, no digits, multiple signs
