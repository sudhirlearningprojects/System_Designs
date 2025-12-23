# String Reversal

## Problem Statement

Reverse a given string.

**Input:** String  
**Output:** Reversed string

**Examples:**
```
reverseStr("abcd")    → "dcba"
reverseStr("hello")   → "olleh"
reverseStr("a")       → "a"
reverseStr("")        → ""
reverseStr("12345")   → "54321"
```

---

## Solution Approaches

### Approach 1: StringBuilder (Built-in) - SIMPLEST

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String reverseStr(String str) {
    return new StringBuilder(str).reverse().toString();
}
```

---

### Approach 2: Two Pointers (Character Array)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String reverseStr(String str) {
    char[] chars = str.toCharArray();
    int left = 0, right = chars.length - 1;
    
    while (left < right) {
        char temp = chars[left];
        chars[left] = chars[right];
        chars[right] = temp;
        left++;
        right--;
    }
    
    return new String(chars);
}
```

---

### Approach 3: Manual StringBuilder

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String reverseStr(String str) {
    StringBuilder result = new StringBuilder();
    
    for (int i = str.length() - 1; i >= 0; i--) {
        result.append(str.charAt(i));
    }
    
    return result.toString();
}
```

---

### Approach 4: Recursion

**Time Complexity:** O(n)  
**Space Complexity:** O(n) - call stack

```java
public static String reverseStr(String str) {
    if (str.isEmpty()) return str;
    return reverseStr(str.substring(1)) + str.charAt(0);
}
```

---

## Algorithm Walkthrough

### Example: "abcd"

**Two Pointers Approach:**
```
Initial: [a, b, c, d]
          L        R

Step 1: Swap a ↔ d
        [d, b, c, a]
             L  R

Step 2: Swap b ↔ c
        [d, c, b, a]
                L
                R

Step 3: left >= right, stop

Result: "dcba"
```

**Recursive Approach:**
```
reverseStr("abcd")
= reverseStr("bcd") + "a"
= (reverseStr("cd") + "b") + "a"
= ((reverseStr("d") + "c") + "b") + "a"
= (((reverseStr("") + "d") + "c") + "b") + "a"
= ((("" + "d") + "c") + "b") + "a"
= "dcba"
```

---

## Complete Implementation

```java
public class StringReversal {
    
    // Approach 1: StringBuilder (Recommended)
    public static String reverseStr(String str) {
        if (str == null) return null;
        return new StringBuilder(str).reverse().toString();
    }
    
    // Approach 2: Two Pointers
    public static String reverseStrTwoPointers(String str) {
        if (str == null) return null;
        
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        
        while (left < right) {
            char temp = chars[left];
            chars[left] = chars[right];
            chars[right] = temp;
            left++;
            right--;
        }
        
        return new String(chars);
    }
    
    // Approach 3: Manual iteration
    public static String reverseStrManual(String str) {
        if (str == null) return null;
        
        StringBuilder result = new StringBuilder();
        for (int i = str.length() - 1; i >= 0; i--) {
            result.append(str.charAt(i));
        }
        
        return result.toString();
    }
    
    // Approach 4: Recursion
    public static String reverseStrRecursive(String str) {
        if (str == null || str.isEmpty()) return str;
        return reverseStrRecursive(str.substring(1)) + str.charAt(0);
    }
    
    // Approach 5: Using Stack
    public static String reverseStrStack(String str) {
        if (str == null) return null;
        
        Stack<Character> stack = new Stack<>();
        for (char c : str.toCharArray()) {
            stack.push(c);
        }
        
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        
        return result.toString();
    }
    
    // Approach 6: XOR swap (in-place for char array)
    public static String reverseStrXOR(String str) {
        if (str == null) return null;
        
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        
        while (left < right) {
            chars[left] ^= chars[right];
            chars[right] ^= chars[left];
            chars[left] ^= chars[right];
            left++;
            right--;
        }
        
        return new String(chars);
    }
    
    // Bonus: Reverse words in a string
    public static String reverseWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = words.length - 1; i >= 0; i--) {
            result.append(words[i]);
            if (i > 0) result.append(" ");
        }
        
        return result.toString();
    }
    
    // Bonus: Reverse each word in place
    public static String reverseEachWord(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            result.append(reverseStr(words[i]));
            if (i < words.length - 1) result.append(" ");
        }
        
        return result.toString();
    }
}
```

---

## Test Cases

```java
@Test
public void testStringReversal() {
    // Basic cases
    assertEquals("dcba", reverseStr("abcd"));
    assertEquals("olleh", reverseStr("hello"));
    
    // Single character
    assertEquals("a", reverseStr("a"));
    
    // Empty string
    assertEquals("", reverseStr(""));
    
    // Numbers
    assertEquals("54321", reverseStr("12345"));
    
    // Palindrome
    assertEquals("racecar", reverseStr("racecar"));
    
    // Special characters
    assertEquals("!dlrow ,olleH", reverseStr("Hello, world!"));
    
    // Spaces
    assertEquals("  olleh", reverseStr("hello  "));
    
    // Null
    assertNull(reverseStr(null));
    
    // Unicode
    assertEquals("😊👋", reverseStr("👋😊"));
}
```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| StringBuilder | O(n) | O(n) | Simplest, recommended |
| Two Pointers | O(n) | O(n) | char array needed |
| Manual Loop | O(n) | O(n) | StringBuilder |
| Recursion | O(n) | O(n) | Stack space |
| Stack | O(n) | O(n) | Extra stack structure |
| XOR Swap | O(n) | O(n) | Clever but less readable |

**Note:** Strings are immutable in Java, so O(n) space is unavoidable.

---

## Visual Representation

```
Input: "hello"

Two Pointers:
[h, e, l, l, o]
 L           R   → Swap h ↔ o

[o, e, l, l, h]
    L     R      → Swap e ↔ l

[o, l, l, e, h]
       L
       R         → Stop (left >= right)

Result: "olleh"
```

---

## Common Mistakes

1. **String Concatenation in Loop:**
   ```java
   // WRONG - O(n²) due to string immutability
   String result = "";
   for (int i = str.length() - 1; i >= 0; i--) {
       result += str.charAt(i);
   }
   
   // CORRECT - O(n) with StringBuilder
   StringBuilder result = new StringBuilder();
   for (int i = str.length() - 1; i >= 0; i--) {
       result.append(str.charAt(i));
   }
   ```

2. **Not Handling Null:**
   ```java
   if (str == null) return null;
   ```

3. **Off-by-One in Loop:**
   ```java
   // CORRECT
   for (int i = str.length() - 1; i >= 0; i--)
   ```

4. **Inefficient Recursion:**
   ```java
   // WRONG - O(n²) due to substring
   return reverseStr(str.substring(1)) + str.charAt(0);
   
   // Better to use iterative approach
   ```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `""` | Empty string |
| `"a"` | `"a"` | Single character |
| `null` | `null` | Null input |
| `"aa"` | `"aa"` | Palindrome |
| `" "` | `" "` | Single space |
| `"a b"` | `"b a"` | With space |

---

## Variations

### Reverse Words in String

```java
// "hello world" → "world hello"
public static String reverseWords(String str) {
    String[] words = str.split(" ");
    StringBuilder result = new StringBuilder();
    
    for (int i = words.length - 1; i >= 0; i--) {
        result.append(words[i]);
        if (i > 0) result.append(" ");
    }
    
    return result.toString();
}
```

### Reverse Each Word

```java
// "hello world" → "olleh dlrow"
public static String reverseEachWord(String str) {
    return Arrays.stream(str.split(" "))
        .map(word -> new StringBuilder(word).reverse().toString())
        .collect(Collectors.joining(" "));
}
```

### Reverse Vowels Only

```java
public static String reverseVowels(String str) {
    char[] chars = str.toCharArray();
    int left = 0, right = chars.length - 1;
    String vowels = "aeiouAEIOU";
    
    while (left < right) {
        while (left < right && vowels.indexOf(chars[left]) == -1) left++;
        while (left < right && vowels.indexOf(chars[right]) == -1) right--;
        
        if (left < right) {
            char temp = chars[left];
            chars[left] = chars[right];
            chars[right] = temp;
            left++;
            right--;
        }
    }
    
    return new String(chars);
}
```

---

## Performance Comparison

```
Input: "abcdefghij" (10 characters)

StringBuilder.reverse():
- Native implementation
- Highly optimized
- ~10 operations

Two Pointers:
- 5 swaps (n/2)
- ~15 operations

Manual Loop:
- 10 append operations
- ~20 operations

Recursion:
- 10 recursive calls
- 10 substring operations
- ~100+ operations (slowest)
```

---

## Related Problems

- **LeetCode 344:** Reverse String
- **LeetCode 541:** Reverse String II
- **LeetCode 151:** Reverse Words in a String
- **LeetCode 345:** Reverse Vowels of a String
- **LeetCode 186:** Reverse Words in a String II

---

## Interview Tips

1. **Clarify Requirements:**
   - In-place reversal needed?
   - Handle null/empty?
   - Preserve spaces?
   - Unicode characters?

2. **Start with StringBuilder:**
   - Simplest and most efficient
   - One-liner solution

3. **Mention Two Pointers:**
   - Classic algorithm
   - Shows understanding

4. **Avoid String Concatenation:**
   - Explain O(n²) problem
   - Use StringBuilder

5. **Discuss Trade-offs:**
   - Simplicity vs performance
   - Built-in vs manual

---

## Real-World Applications

- **Text Processing:** Formatting, display
- **Cryptography:** Simple encoding
- **Data Validation:** Palindrome checking
- **UI/UX:** RTL language support
- **Algorithms:** String manipulation problems
- **Debugging:** Log analysis

---

## XOR Swap Explained

```java
// Swap without temp variable
a ^= b;  // a = a XOR b
b ^= a;  // b = b XOR (a XOR b) = a
a ^= b;  // a = (a XOR b) XOR a = b

Example: a=5, b=3
Binary: a=101, b=011

a ^= b:  a = 101 XOR 011 = 110
b ^= a:  b = 011 XOR 110 = 101 (now b=5)
a ^= b:  a = 110 XOR 101 = 011 (now a=3)

Result: a=3, b=5 (swapped!)
```

---

## Why StringBuilder is Recommended

```java
// Pros:
✅ One-liner: new StringBuilder(str).reverse().toString()
✅ Highly optimized native implementation
✅ Most readable
✅ Handles all edge cases

// Cons:
❌ Hides algorithm (interview might want manual)
❌ Creates extra object

// When to use manual:
- Interview explicitly asks for algorithm
- Educational purposes
- Need to understand internals
```

---

## Key Takeaways

✅ StringBuilder.reverse() is simplest - O(n) time, O(n) space  
✅ Two pointers is classic algorithm approach  
✅ Avoid string concatenation in loops (O(n²))  
✅ Strings are immutable - O(n) space unavoidable  
✅ Handle null and empty string edge cases  
✅ Recursion is elegant but inefficient (O(n²))  
✅ For interviews, know both built-in and manual approaches
