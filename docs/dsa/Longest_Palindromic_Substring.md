# Longest Palindromic Substring

## Problem Statement

Find the longest palindromic substring in a given string.

**Input:** String  
**Output:** Longest palindromic substring

**Examples:**
```
Input: "babad"
Output: "bab" or "aba"

Input: "cbbd"
Output: "bb"

Input: "racecar"
Output: "racecar"

Input: "a"
Output: "a"

Input: "ac"
Output: "a" or "c"
```

---

## Solution Approaches

### Approach 1: Expand Around Center (Optimal for Interview)

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static String longestPalindrome(String s) {
    if (s == null || s.length() < 1) return "";
    
    int start = 0, maxLen = 0;
    
    for (int i = 0; i < s.length(); i++) {
        // Odd length palindrome (center is single char)
        int len1 = expandAroundCenter(s, i, i);
        // Even length palindrome (center is between two chars)
        int len2 = expandAroundCenter(s, i, i + 1);
        
        int len = Math.max(len1, len2);
        
        if (len > maxLen) {
            maxLen = len;
            start = i - (len - 1) / 2;
        }
    }
    
    return s.substring(start, start + maxLen);
}

private static int expandAroundCenter(String s, int left, int right) {
    while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
        left--;
        right++;
    }
    return right - left - 1;
}
```

---

### Approach 2: Dynamic Programming

**Time Complexity:** O(n²)  
**Space Complexity:** O(n²)

```java
public static String longestPalindromeDP(String s) {
    if (s == null || s.length() < 1) return "";
    
    int n = s.length();
    boolean[][] dp = new boolean[n][n];
    int start = 0, maxLen = 1;
    
    // Single characters are palindromes
    for (int i = 0; i < n; i++) {
        dp[i][i] = true;
    }
    
    // Check for length 2
    for (int i = 0; i < n - 1; i++) {
        if (s.charAt(i) == s.charAt(i + 1)) {
            dp[i][i + 1] = true;
            start = i;
            maxLen = 2;
        }
    }
    
    // Check for lengths > 2
    for (int len = 3; len <= n; len++) {
        for (int i = 0; i < n - len + 1; i++) {
            int j = i + len - 1;
            
            if (s.charAt(i) == s.charAt(j) && dp[i + 1][j - 1]) {
                dp[i][j] = true;
                start = i;
                maxLen = len;
            }
        }
    }
    
    return s.substring(start, start + maxLen);
}
```

---

### Approach 3: Manacher's Algorithm (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String longestPalindromeManacher(String s) {
    if (s == null || s.length() < 1) return "";
    
    // Transform string: "abc" -> "#a#b#c#"
    String t = "#" + String.join("#", s.split("")) + "#";
    int n = t.length();
    int[] p = new int[n]; // palindrome radius array
    int center = 0, right = 0;
    int maxLen = 0, maxCenter = 0;
    
    for (int i = 0; i < n; i++) {
        // Mirror of i
        int mirror = 2 * center - i;
        
        if (i < right) {
            p[i] = Math.min(right - i, p[mirror]);
        }
        
        // Expand around i
        while (i + p[i] + 1 < n && i - p[i] - 1 >= 0 && 
               t.charAt(i + p[i] + 1) == t.charAt(i - p[i] - 1)) {
            p[i]++;
        }
        
        // Update center and right boundary
        if (i + p[i] > right) {
            center = i;
            right = i + p[i];
        }
        
        // Track maximum
        if (p[i] > maxLen) {
            maxLen = p[i];
            maxCenter = i;
        }
    }
    
    // Extract palindrome from original string
    int start = (maxCenter - maxLen) / 2;
    return s.substring(start, start + maxLen);
}
```

---

### Approach 4: Brute Force

**Time Complexity:** O(n³)  
**Space Complexity:** O(1)

```java
public static String longestPalindromeBrute(String s) {
    if (s == null || s.length() < 1) return "";
    
    String longest = "";
    
    for (int i = 0; i < s.length(); i++) {
        for (int j = i; j < s.length(); j++) {
            String sub = s.substring(i, j + 1);
            if (isPalindrome(sub) && sub.length() > longest.length()) {
                longest = sub;
            }
        }
    }
    
    return longest;
}

private static boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        if (s.charAt(left++) != s.charAt(right--)) {
            return false;
        }
    }
    return true;
}
```

---

## Algorithm Walkthrough

### Example: "babad"

**Expand Around Center:**

```
String: b a b a d
Index:  0 1 2 3 4

i=0 (center at 'b'):
  Odd: expand(0,0) → "b", len=1
  Even: expand(0,1) → "", len=0
  maxLen=1, start=0

i=1 (center at 'a'):
  Odd: expand(1,1) → "bab", len=3
    left=0, right=2: b==b ✓
    left=-1, stop
  Even: expand(1,2) → "", len=0
  maxLen=3, start=0

i=2 (center at 'b'):
  Odd: expand(2,2) → "aba", len=3
    left=1, right=3: a==a ✓
    left=0, right=4: b!=d ✗
  Even: expand(2,3) → "", len=0
  maxLen=3 (no change)

i=3 (center at 'a'):
  Odd: expand(3,3) → "a", len=1
  Even: expand(3,4) → "", len=0
  maxLen=3

i=4 (center at 'd'):
  Odd: expand(4,4) → "d", len=1
  Even: expand(4,5) → out of bounds
  maxLen=3

Result: s.substring(0, 3) = "bab"
```

### Example: "cbbd"

```
String: c b b d
Index:  0 1 2 3

i=0: max("c", "") = 1
i=1: max("b", "bb") = 2, start=1
i=2: max("b", "") = 2
i=3: max("d", "") = 2

Result: s.substring(1, 3) = "bb"
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Expand around center (Optimal for interview)
    public static String longestPalindrome(String s) {
        if (s == null || s.length() < 1) return "";
        
        int start = 0, maxLen = 0;
        
        for (int i = 0; i < s.length(); i++) {
            int len1 = expandAroundCenter(s, i, i);
            int len2 = expandAroundCenter(s, i, i + 1);
            int len = Math.max(len1, len2);
            
            if (len > maxLen) {
                maxLen = len;
                start = i - (len - 1) / 2;
            }
        }
        
        return s.substring(start, start + maxLen);
    }
    
    private static int expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && 
               s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }
    
    // Approach 2: Dynamic Programming
    public static String longestPalindromeDP(String s) {
        if (s == null || s.length() < 1) return "";
        
        int n = s.length();
        boolean[][] dp = new boolean[n][n];
        int start = 0, maxLen = 1;
        
        for (int i = 0; i < n; i++) {
            dp[i][i] = true;
        }
        
        for (int i = 0; i < n - 1; i++) {
            if (s.charAt(i) == s.charAt(i + 1)) {
                dp[i][i + 1] = true;
                start = i;
                maxLen = 2;
            }
        }
        
        for (int len = 3; len <= n; len++) {
            for (int i = 0; i < n - len + 1; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j) && dp[i + 1][j - 1]) {
                    dp[i][j] = true;
                    start = i;
                    maxLen = len;
                }
            }
        }
        
        return s.substring(start, start + maxLen);
    }
    
    // Return length only
    public static int longestPalindromeLength(String s) {
        return longestPalindrome(s).length();
    }
    
    public static boolean doTestsPass() {
        // Test 1
        String result1 = longestPalindrome("babad");
        if (!result1.equals("bab") && !result1.equals("aba")) return false;
        
        // Test 2
        if (!longestPalindrome("cbbd").equals("bb")) return false;
        
        // Test 3
        if (!longestPalindrome("racecar").equals("racecar")) return false;
        
        // Test 4
        if (!longestPalindrome("a").equals("a")) return false;
        
        // Test 5
        if (longestPalindrome("ac").length() != 1) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        String[] tests = {"babad", "cbbd", "racecar", "noon"};
        for (String s : tests) {
            System.out.println("Input: " + s);
            System.out.println("Longest palindrome: " + longestPalindrome(s));
            System.out.println();
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testLongestPalindrome() {
    // Test 1: Multiple valid answers
    String result1 = longestPalindrome("babad");
    assertTrue(result1.equals("bab") || result1.equals("aba"));
    
    // Test 2: Even length palindrome
    assertEquals("bb", longestPalindrome("cbbd"));
    
    // Test 3: Entire string is palindrome
    assertEquals("racecar", longestPalindrome("racecar"));
    
    // Test 4: Single character
    assertEquals("a", longestPalindrome("a"));
    
    // Test 5: No palindrome > 1
    assertEquals(1, longestPalindrome("ac").length());
    
    // Test 6: Empty string
    assertEquals("", longestPalindrome(""));
    
    // Test 7: All same characters
    assertEquals("aaaa", longestPalindrome("aaaa"));
    
    // Test 8: Palindrome at start
    assertEquals("aba", longestPalindrome("abacd"));
    
    // Test 9: Palindrome at end
    assertEquals("aba", longestPalindrome("cdaba"));
    
    // Test 10: Long string
    assertEquals("anana", longestPalindrome("banana"));
}
```

---

## Visual Representation

### Expand Around Center

```
String: "babad"

Center at index 1 ('a'):
  b a b a d
  ← ↑ →
  
  Step 1: left=1, right=1, s[1]==s[1] ✓
  Step 2: left=0, right=2, s[0]==s[2] ✓ (b==b)
  Step 3: left=-1, stop
  
  Palindrome: "bab", length=3

Center between index 1 and 2:
  b a b a d
    ↑ ↑
  
  Step 1: left=1, right=2, s[1]!=s[2] ✗ (a!=b)
  
  Palindrome: "", length=0
```

### DP Table for "babad"

```
    b  a  b  a  d
b   T  F  T  F  F
a      T  F  T  F
b         T  F  F
a            T  F
d               T

T = palindrome
F = not palindrome

dp[0][2] = T because s[0]==s[2] and dp[1][1]==T
Result: "bab" (indices 0-2)
```

---

## Edge Cases

1. **Empty string:** "" → ""
2. **Single character:** "a" → "a"
3. **Two characters same:** "aa" → "aa"
4. **Two characters different:** "ab" → "a" or "b"
5. **All same:** "aaaa" → "aaaa"
6. **No palindrome > 1:** "abcd" → any single char
7. **Entire string:** "racecar" → "racecar"
8. **Multiple same length:** "babad" → "bab" or "aba"

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Expand Around Center | O(n²) | O(1) | **Best for interview** |
| Dynamic Programming | O(n²) | O(n²) | Clear logic |
| Manacher's Algorithm | O(n) | O(n) | **Optimal but complex** |
| Brute Force | O(n³) | O(1) | Not practical |

**Why Expand Around Center?**
- O(n²) time: n centers × O(n) expansion
- O(1) space: No extra data structures
- Simple to implement and explain
- Good balance of efficiency and clarity

---

## Related Problems

1. **Palindromic Substrings** - Count all palindromes
2. **Longest Palindromic Subsequence** - Non-contiguous
3. **Valid Palindrome** - Check if palindrome
4. **Palindrome Partitioning** - Split into palindromes
5. **Shortest Palindrome** - Add chars to make palindrome
6. **Palindrome Pairs** - Find palindrome word pairs

---

## Interview Tips

### Clarification Questions
1. Return substring or just length? (Usually substring)
2. Multiple answers with same length? (Return any)
3. Empty string input? (Return "")
4. Case sensitive? (Usually yes)
5. Special characters? (Usually included)

### Approach Explanation
1. "Expand around each possible center"
2. "Two cases: odd length (single center) and even length (between chars)"
3. "For each center, expand while characters match"
4. "Track maximum length and starting position"
5. "O(n²) time, O(1) space"

### Common Mistakes
- Forgetting even-length palindromes
- Wrong calculation of start index
- Not handling empty string
- Off-by-one errors in expansion
- Not considering single character case

### Why Two Expansion Cases?

```
Odd length: "aba"
  Center: single character 'b'
  expand(1, 1)

Even length: "abba"
  Center: between 'b' and 'b'
  expand(1, 2)

Must check both for each position!
```

---

## Real-World Applications

1. **DNA Sequencing** - Finding palindromic sequences
2. **Text Processing** - Pattern recognition
3. **Compression** - Identifying repeating patterns
4. **Bioinformatics** - Gene analysis
5. **String Matching** - Pattern detection
6. **Data Validation** - Palindrome checks

---

## Key Takeaways

1. **Two Cases:** Odd-length and even-length palindromes
2. **Expand Around Center:** Check both cases for each position
3. **Start Index:** `i - (len - 1) / 2` for center at i
4. **Time Complexity:** O(n²) for expand around center
5. **Space Complexity:** O(1) - no extra data structures
6. **Optimal:** Manacher's O(n) but complex for interviews
7. **Best Choice:** Expand around center for interviews

---

## Additional Notes

### Start Index Calculation

```java
start = i - (len - 1) / 2;

Why?
  Odd length (len=3, center at i=2):
    start = 2 - (3-1)/2 = 2 - 1 = 1 ✓
    
  Even length (len=4, center between i=1 and i=2):
    start = 1 - (4-1)/2 = 1 - 1 = 0 ✓
    (i is left center)

Formula works for both cases!
```

### Manacher's Algorithm Intuition

```
Transform: "aba" → "#a#b#a#"
  - All palindromes become odd length
  - Simplifies logic

Use symmetry:
  If palindrome centered at C extends to R,
  then position i has mirror at 2*C - i
  
  Can reuse information from mirror!
```

### DP Recurrence

```java
dp[i][j] = true if:
  1. s[i] == s[j] AND
  2. dp[i+1][j-1] == true (inner substring is palindrome)

Base cases:
  dp[i][i] = true (single char)
  dp[i][i+1] = (s[i] == s[i+1]) (two chars)
```

### Count All Palindromic Substrings

```java
public static int countPalindromes(String s) {
    int count = 0;
    
    for (int i = 0; i < s.length(); i++) {
        // Odd length
        count += countExpand(s, i, i);
        // Even length
        count += countExpand(s, i, i + 1);
    }
    
    return count;
}

private static int countExpand(String s, int left, int right) {
    int count = 0;
    while (left >= 0 && right < s.length() && 
           s.charAt(left) == s.charAt(right)) {
        count++;
        left--;
        right++;
    }
    return count;
}
```

### Longest Palindromic Subsequence (LCS variant)

```java
// Non-contiguous - different problem
public static int longestPalindromeSubseq(String s) {
    int n = s.length();
    int[][] dp = new int[n][n];
    
    for (int i = 0; i < n; i++) {
        dp[i][i] = 1;
    }
    
    for (int len = 2; len <= n; len++) {
        for (int i = 0; i < n - len + 1; i++) {
            int j = i + len - 1;
            
            if (s.charAt(i) == s.charAt(j)) {
                dp[i][j] = dp[i + 1][j - 1] + 2;
            } else {
                dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }
    }
    
    return dp[0][n - 1];
}
```

### Optimization: Early Exit

```java
// If remaining string < current max, can't improve
if (s.length() - i < maxLen / 2) {
    break;
}
```

### Why Not Binary Search?

```
Binary search doesn't apply:
  - No sorted property
  - Can't eliminate half of search space
  - Palindrome property is local, not global

Must check all possible centers
```

### Comparison: Substring vs Subsequence

```
Substring (contiguous):
  "babad" → "bab" or "aba"
  Must be consecutive characters

Subsequence (non-contiguous):
  "babad" → "babab" (take b,a,b,a,b)
  Can skip characters

Different problems, different algorithms!
```
