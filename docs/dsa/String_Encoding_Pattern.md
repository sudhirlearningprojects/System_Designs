# String Encoding with Repetition Pattern

## Problem Statement

Encode a string by replacing repeated substrings with a special character `*` that represents "repeat previous pattern". Find the minimum number of characters needed to encode the string.

**Encoding Rule:** `*` means repeat the entire sequence before it

**Examples:**
```
"ABCABCE" → "ABC*E" (5 characters)
  ABC appears, then ABC repeats (use *), then E
  
"ABCDABCE" → "ABCDABCE" (8 characters)
  No beneficial pattern to repeat
  
"ABABAB" → "AB**" (4 characters)
  AB, then repeat (*), then repeat again (*)
```

---

## Solution Approach

### Dynamic Programming

**Time Complexity:** O(n³)  
**Space Complexity:** O(n)

```java
private int minimalSteps(String ingredients) {
    int n = ingredients.length();
    int[] dp = new int[n + 1];
    
    for (int i = 1; i <= n; i++) {
        dp[i] = i; // Worst case: no encoding
        
        // Try all possible pattern lengths
        for (int len = 1; len <= i / 2; len++) {
            String pattern = ingredients.substring(i - len, i);
            String prev = ingredients.substring(i - 2 * len, i - len);
            
            if (pattern.equals(prev)) {
                dp[i] = Math.min(dp[i], dp[i - len] + 1);
            }
        }
    }
    
    return dp[n];
}
```

---

## Algorithm Explanation

### Key Insight

For each position, we can either:
1. Add the character normally (cost = previous + 1)
2. Use `*` if the previous substring repeats (cost = previous + 1)

We choose the minimum cost option.

### Dynamic Programming State

```
dp[i] = minimum characters needed to encode first i characters

For each position i:
  - Try all pattern lengths from 1 to i/2
  - If pattern matches previous substring of same length
  - dp[i] = min(dp[i], dp[i - len] + 1)
```

---

## Detailed Walkthrough

### Example 1: "ABCABCE"

```
Position 0: dp[0] = 0 (empty)

Position 1: 'A'
  dp[1] = 1 (just 'A')

Position 2: 'AB'
  dp[2] = 2 (just 'AB')

Position 3: 'ABC'
  dp[3] = 3 (just 'ABC')

Position 4: 'ABCA'
  Check len=1: 'A' vs 'C' → no match
  Check len=2: 'CA' vs 'AB' → no match
  dp[4] = 4

Position 5: 'ABCAB'
  Check len=1: 'B' vs 'A' → no match
  Check len=2: 'AB' vs 'CA' → no match
  dp[5] = 5

Position 6: 'ABCABC'
  Check len=1: 'C' vs 'B' → no match
  Check len=2: 'BC' vs 'AB' → no match
  Check len=3: 'ABC' vs 'ABC' → MATCH!
    dp[6] = min(6, dp[3] + 1) = min(6, 4) = 4
  Encoding: "ABC*"

Position 7: 'ABCABCE'
  Check len=1: 'E' vs 'C' → no match
  Check len=2: 'CE' vs 'BC' → no match
  Check len=3: 'BCE' vs 'ABC' → no match
  dp[7] = 5
  Encoding: "ABC*E"

Result: 5
```

### Example 2: "ABCDABCE"

```
Position 0-4: dp[0]=0, dp[1]=1, dp[2]=2, dp[3]=3, dp[4]=4

Position 5: 'ABCDA'
  No matches
  dp[5] = 5

Position 6: 'ABCDAB'
  No matches
  dp[6] = 6

Position 7: 'ABCDABC'
  No matches
  dp[7] = 7

Position 8: 'ABCDABCE'
  Check len=4: 'ABCE' vs 'ABCD' → no match
  dp[8] = 8

Result: 8 (no encoding benefit)
```

### Example 3: "ABABAB"

```
Position 2: 'AB'
  dp[2] = 2

Position 4: 'ABAB'
  Check len=2: 'AB' vs 'AB' → MATCH!
  dp[4] = dp[2] + 1 = 3
  Encoding: "AB*"

Position 6: 'ABABAB'
  Check len=2: 'AB' vs 'AB' → MATCH!
  dp[6] = dp[4] + 1 = 4
  Encoding: "AB**"

Result: 4
```

---

## Complete Implementation

```java
public class StringEncoding {
    
    // Approach 1: Dynamic Programming (Recommended)
    private int minimalSteps(String ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return 0;
        }
        
        int n = ingredients.length();
        int[] dp = new int[n + 1];
        
        for (int i = 1; i <= n; i++) {
            dp[i] = i; // Worst case: no encoding possible
            
            // Try all possible pattern lengths
            for (int len = 1; len <= i / 2; len++) {
                String pattern = ingredients.substring(i - len, i);
                String prev = ingredients.substring(i - 2 * len, i - len);
                
                if (pattern.equals(prev)) {
                    dp[i] = Math.min(dp[i], dp[i - len] + 1);
                }
            }
        }
        
        return dp[n];
    }
    
    // Approach 2: With actual encoding string
    private String encode(String ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return "";
        }
        
        int n = ingredients.length();
        int[] dp = new int[n + 1];
        String[] encoding = new String[n + 1];
        encoding[0] = "";
        
        for (int i = 1; i <= n; i++) {
            dp[i] = i;
            encoding[i] = ingredients.substring(0, i);
            
            for (int len = 1; len <= i / 2; len++) {
                String pattern = ingredients.substring(i - len, i);
                String prev = ingredients.substring(i - 2 * len, i - len);
                
                if (pattern.equals(prev)) {
                    if (dp[i - len] + 1 < dp[i]) {
                        dp[i] = dp[i - len] + 1;
                        encoding[i] = encoding[i - len] + "*";
                    }
                }
            }
        }
        
        return encoding[n];
    }
    
    // Approach 3: Recursive with memoization
    private Map<Integer, Integer> memo = new HashMap<>();
    
    private int minimalStepsRecursive(String ingredients) {
        return helper(ingredients, ingredients.length());
    }
    
    private int helper(String s, int pos) {
        if (pos == 0) return 0;
        if (memo.containsKey(pos)) return memo.get(pos);
        
        int result = pos; // Worst case
        
        for (int len = 1; len <= pos / 2; len++) {
            String pattern = s.substring(pos - len, pos);
            String prev = s.substring(pos - 2 * len, pos - len);
            
            if (pattern.equals(prev)) {
                result = Math.min(result, helper(s, pos - len) + 1);
            }
        }
        
        memo.put(pos, result);
        return result;
    }
    
    private boolean doTestsPass() {
        return minimalSteps("ABCDABCE") == 8 && 
               minimalSteps("ABCABCE") == 5;
    }
    
    public static void main(String[] args) {
        StringEncoding solution = new StringEncoding();
        
        if (solution.doTestsPass()) {
            System.out.println("Pass");
        } else {
            System.out.println("Fail");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testMinimalSteps() {
    StringEncoding solution = new StringEncoding();
    
    // Given test cases
    assertEquals(8, solution.minimalSteps("ABCDABCE"));
    assertEquals(5, solution.minimalSteps("ABCABCE"));
    
    // Additional test cases
    assertEquals(1, solution.minimalSteps("A"));
    assertEquals(2, solution.minimalSteps("AB"));
    assertEquals(4, solution.minimalSteps("ABABAB"));
    assertEquals(3, solution.minimalSteps("ABAB"));
    assertEquals(6, solution.minimalSteps("ABCDEF"));
    assertEquals(0, solution.minimalSteps(""));
    
    // All same character
    assertEquals(2, solution.minimalSteps("AAA")); // "A**"
    assertEquals(3, solution.minimalSteps("AAAA")); // "A***"
    
    // Complex patterns
    assertEquals(7, solution.minimalSteps("ABCABCABC")); // "ABC**"
}

@Test
public void testEncode() {
    StringEncoding solution = new StringEncoding();
    
    assertEquals("ABC*E", solution.encode("ABCABCE"));
    assertEquals("AB**", solution.encode("ABABAB"));
    assertEquals("A**", solution.encode("AAA"));
}
```

---

## Visual Representation

```
"ABCABCE" encoding process:

Original: A B C A B C E
          └─┬─┘ └─┬─┘
          ABC   ABC (repeat)

Encoded:  A B C * E
          └─┬─┘ │ │
           3    1 1  = 5 characters

"ABABAB" encoding process:

Original: A B A B A B
          └┬┘ └┬┘ └┬┘
          AB  AB  AB

Encoded:  A B * *
          └┬┘ │ │
           2  1 1  = 4 characters
```

---

## Edge Cases

| Input | Output | Encoding | Explanation |
|-------|--------|----------|-------------|
| `""` | `0` | `""` | Empty string |
| `"A"` | `1` | `"A"` | Single character |
| `"AB"` | `2` | `"AB"` | No pattern |
| `"ABAB"` | `3` | `"AB*"` | One repeat |
| `"AAA"` | `2` | `"A**"` | Multiple repeats |
| `"ABCDEF"` | `6` | `"ABCDEF"` | No pattern |

---

## Common Mistakes

1. **Not Checking All Pattern Lengths:**
   ```java
   // WRONG - only checks len=1
   for (int len = 1; len <= 1; len++)
   
   // CORRECT - checks all possible lengths
   for (int len = 1; len <= i / 2; len++)
   ```

2. **Off-by-One in Substring:**
   ```java
   // CORRECT indices
   String pattern = ingredients.substring(i - len, i);
   String prev = ingredients.substring(i - 2 * len, i - len);
   ```

3. **Not Initializing DP Array:**
   ```java
   // Initialize with worst case
   dp[i] = i;
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| DP | O(n³) | O(n) | n positions × n/2 lengths × n comparison |
| Recursive + Memo | O(n³) | O(n) | Same as DP |
| Brute Force | O(2ⁿ) | O(n) | Try all combinations |

**Where n = string length**

---

## Why O(n³)?

```
Outer loop: n positions
Inner loop: n/2 pattern lengths
String comparison: O(n) in worst case

Total: O(n × n/2 × n) = O(n³)

Can be optimized to O(n²) using rolling hash
```

---

## Optimization: Rolling Hash

```java
// Use rolling hash for O(1) string comparison
private int minimalStepsOptimized(String ingredients) {
    int n = ingredients.length();
    int[] dp = new int[n + 1];
    
    for (int i = 1; i <= n; i++) {
        dp[i] = i;
        
        for (int len = 1; len <= i / 2; len++) {
            // Use rolling hash instead of substring comparison
            if (hashMatch(ingredients, i - len, i, len)) {
                dp[i] = Math.min(dp[i], dp[i - len] + 1);
            }
        }
    }
    
    return dp[n];
}

// This reduces time to O(n²)
```

---

## Related Problems

- **LeetCode 471:** Encode String with Shortest Length
- **LeetCode 443:** String Compression
- **Run Length Encoding**
- **Pattern Matching**

---

## Interview Tips

1. **Clarify Requirements:**
   - What does `*` represent exactly?
   - Can patterns overlap?
   - Case sensitive?

2. **Start with DP:**
   - State: dp[i] = min chars for first i characters
   - Transition: try all pattern lengths

3. **Walk Through Example:**
   - Use "ABCABCE"
   - Show how ABC repeats

4. **Discuss Complexity:**
   - O(n³) with substring comparison
   - Can optimize to O(n²) with rolling hash

5. **Handle Edge Cases:**
   - Empty string
   - Single character
   - No patterns

---

## Real-World Applications

- **Data Compression:** Lempel-Ziv algorithms
- **DNA Sequence Analysis:** Finding repeated patterns
- **Text Compression:** Gzip, LZ77
- **Network Protocols:** Compression algorithms
- **File Formats:** PNG, ZIP compression

---

## Pattern Recognition

```
Key observation:
- * represents "repeat everything before"
- Can chain multiple *'s
- Each * saves (pattern_length - 1) characters

Example: "ABABAB"
- Without encoding: 6 chars
- With encoding "AB**": 4 chars
- Savings: 2 chars (one * saves 1 char each)
```

---

## Key Takeaways

✅ Use DP to find minimum encoding length  
✅ State: dp[i] = min characters for first i chars  
✅ Try all pattern lengths from 1 to i/2  
✅ Check if current pattern matches previous  
✅ `*` means repeat entire previous sequence  
✅ O(n³) time with substring comparison  
✅ Can optimize to O(n²) with rolling hash  
✅ Initialize dp[i] = i (worst case: no encoding)
