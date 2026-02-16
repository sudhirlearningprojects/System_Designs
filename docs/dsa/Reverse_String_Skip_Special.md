# Reverse String Skipping Special Characters

## Problem Statement

Reverse a string while keeping special characters at their original positions.

**Input:** String with alphanumeric and special characters  
**Output:** Reversed string with special characters at original positions

**Examples:**
```
Input:  "a,b$c"
Output: "c,b$a"

Input:  "Ab,c,de!$"
Output: "ed,c,bA!$"

Input:  "a!!!b.c.d,e'f,ghi"
Output: "i!!!h.g.f,e'd,cba"
```

---

## Solution Approach

### Two-Pointer Technique

**Algorithm:**
1. Convert string to character array
2. Use two pointers (left and right)
3. Skip special characters from both ends
4. Swap alphanumeric characters
5. Move pointers inward

**Time Complexity:** O(n)  
**Space Complexity:** O(n) for character array

---

## Implementation

### Java Implementation

```java
public class ReverseStringSkipSpecial {
    
    public static String reverseSkipSpecial(String str) {
        if (str == null || str.length() <= 1) return str;
        
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        
        while (left < right) {
            if (!Character.isLetterOrDigit(chars[left])) {
                left++;
            } else if (!Character.isLetterOrDigit(chars[right])) {
                right--;
            } else {
                char temp = chars[left];
                chars[left] = chars[right];
                chars[right] = temp;
                left++;
                right--;
            }
        }
        
        return new String(chars);
    }
}
```

### Python Implementation

```python
def reverse_skip_special(s):
    if not s or len(s) <= 1:
        return s
    
    chars = list(s)
    left, right = 0, len(chars) - 1
    
    while left < right:
        if not chars[left].isalnum():
            left += 1
        elif not chars[right].isalnum():
            right -= 1
        else:
            chars[left], chars[right] = chars[right], chars[left]
            left += 1
            right -= 1
    
    return ''.join(chars)
```

### C++ Implementation

```cpp
#include <string>
#include <cctype>
using namespace std;

string reverseSkipSpecial(string str) {
    int left = 0, right = str.length() - 1;
    
    while (left < right) {
        if (!isalnum(str[left])) {
            left++;
        } else if (!isalnum(str[right])) {
            right--;
        } else {
            swap(str[left], str[right]);
            left++;
            right--;
        }
    }
    
    return str;
}
```

---

## Algorithm Walkthrough

### Example 1: "a,b$c"

```
Initial: a , b $ c
         ↑       ↑
         L       R

Step 1: chars[L]='a' (alphanumeric), chars[R]='c' (alphanumeric)
        Swap: c , b $ a
              ↑   ↑
              L   R

Step 2: chars[L]=',' (special), skip
            c , b $ a
                ↑ ↑
                L R

Step 3: chars[L]='b' (alphanumeric), chars[R]='$' (special), skip
            c , b $ a
                ↑ ↑
                R L (left >= right, stop)

Result: "c,b$a"
```

### Example 2: "Ab,c,de!$"

```
Initial: A b , c , d e ! $
         ↑                 ↑
         L                 R

Step 1: chars[R]='$' (special), skip
        A b , c , d e ! $
        ↑             ↑
        L             R

Step 2: chars[R]='!' (special), skip
        A b , c , d e ! $
        ↑           ↑
        L           R

Step 3: chars[L]='A', chars[R]='e', swap
        e b , c , d A ! $
          ↑       ↑
          L       R

Step 4: chars[L]='b', chars[R]='d', swap
        e d , c , b A ! $
            ↑   ↑
            L   R

Step 5: chars[L]=',' (special), skip
        e d , c , b A ! $
              ↑ ↑
              L R

Step 6: chars[L]='c', chars[R]=',' (special), skip
        e d , c , b A ! $
              ↑ ↑
              R L (left >= right, stop)

Result: "ed,c,bA!$"
```

---

## Complete Implementation with Tests

```java
public class ReverseStringSkipSpecial {
    
    public static String reverseSkipSpecial(String str) {
        if (str == null || str.length() <= 1) {
            return str;
        }
        
        char[] chars = str.toCharArray();
        int left = 0, right = chars.length - 1;
        
        while (left < right) {
            if (!Character.isLetterOrDigit(chars[left])) {
                left++;
            } else if (!Character.isLetterOrDigit(chars[right])) {
                right--;
            } else {
                char temp = chars[left];
                chars[left] = chars[right];
                chars[right] = temp;
                left++;
                right--;
            }
        }
        
        return new String(chars);
    }
    
    public static void main(String[] args) {
        // Test cases
        System.out.println(reverseSkipSpecial("a,b$c"));           // "c,b$a"
        System.out.println(reverseSkipSpecial("Ab,c,de!$"));       // "ed,c,bA!$"
        System.out.println(reverseSkipSpecial("a!!!b.c.d,e'f,ghi")); // "i!!!h.g.f,e'd,cba"
        System.out.println(reverseSkipSpecial("abc"));             // "cba"
        System.out.println(reverseSkipSpecial("!!!"));             // "!!!"
        System.out.println(reverseSkipSpecial("a"));               // "a"
        System.out.println(reverseSkipSpecial(""));                // ""
    }
}
```

---

## Test Cases

```java
@Test
public void testReverseSkipSpecial() {
    // Basic case
    assertEquals("c,b$a", reverseSkipSpecial("a,b$c"));
    
    // Mixed case
    assertEquals("ed,c,bA!$", reverseSkipSpecial("Ab,c,de!$"));
    
    // Multiple special characters
    assertEquals("i!!!h.g.f,e'd,cba", reverseSkipSpecial("a!!!b.c.d,e'f,ghi"));
    
    // No special characters
    assertEquals("cba", reverseSkipSpecial("abc"));
    
    // Only special characters
    assertEquals("!!!", reverseSkipSpecial("!!!"));
    
    // Single character
    assertEquals("a", reverseSkipSpecial("a"));
    
    // Empty string
    assertEquals("", reverseSkipSpecial(""));
    
    // Null
    assertEquals(null, reverseSkipSpecial(null));
    
    // Numbers
    assertEquals("3,2$1", reverseSkipSpecial("1,2$3"));
    
    // Mixed alphanumeric
    assertEquals("3c,2b$1a", reverseSkipSpecial("a1,b2$c3"));
}
```

---

## Visual Representation

```
Input:  "a!!!b.c.d,e'f,ghi"

Step-by-step:
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ a │ ! │ ! │ ! │ b │ . │ c │ . │ d │ , │ e │ ' │ f │ , │ g │ h │ i │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
  ↑                                                                 ↑
  L                                                                 R

Swap 'a' ↔ 'i':
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ i │ ! │ ! │ ! │ b │ . │ c │ . │ d │ , │ e │ ' │ f │ , │ g │ h │ a │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
      ↑                                                         ↑
      L (skip !)                                                R

Continue swapping: b ↔ h, c ↔ g, d ↔ f, e stays

Final:
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ i │ ! │ ! │ ! │ h │ . │ g │ . │ f │ , │ e │ ' │ d │ , │ c │ b │ a │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘

Output: "i!!!h.g.f,e'd,cba"
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `""` | Empty string |
| `"a"` | `"a"` | Single character |
| `"!!!"` | `"!!!"` | Only special characters |
| `"abc"` | `"cba"` | No special characters |
| `"a!b"` | `"b!a"` | Special in middle |
| `"!a!"` | `"!a!"` | Special at ends |
| `null` | `null` | Null input |

---

## Complexity Analysis

**Time Complexity:** O(n)
- Single pass through the string
- Each character visited at most once

**Space Complexity:** O(n)
- Character array to store result
- Can be O(1) if modifying in-place (for mutable strings)

---

## Alternative Approach: Extract and Rebuild

```java
public static String reverseSkipSpecialAlternative(String str) {
    if (str == null || str.length() <= 1) return str;
    
    // Extract alphanumeric characters
    StringBuilder alphanumeric = new StringBuilder();
    for (char c : str.toCharArray()) {
        if (Character.isLetterOrDigit(c)) {
            alphanumeric.append(c);
        }
    }
    
    // Reverse alphanumeric
    alphanumeric.reverse();
    
    // Rebuild string
    StringBuilder result = new StringBuilder();
    int idx = 0;
    for (char c : str.toCharArray()) {
        if (Character.isLetterOrDigit(c)) {
            result.append(alphanumeric.charAt(idx++));
        } else {
            result.append(c);
        }
    }
    
    return result.toString();
}
```

**Time:** O(n), **Space:** O(n)

---

## Key Takeaways

✅ Use two-pointer technique for in-place reversal  
✅ Skip special characters from both ends  
✅ Only swap alphanumeric characters  
✅ Time complexity: O(n), Space: O(n)  
✅ Handle edge cases: empty, null, all special, no special
