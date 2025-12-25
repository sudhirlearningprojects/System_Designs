# Longest Substring with Unique Characters

## Problem Statement

Find the longest substring with all unique (non-repeating) characters.

**Input:** String  
**Output:** Longest substring with unique characters (or its length)

**Examples:**
```
Input: "aaabcbdeaf"
Output: "cbdeaf"
Length: 6

Input: "abcabcbb"
Output: "abc"
Length: 3

Input: "bbbbb"
Output: "b"
Length: 1

Input: "pwwkew"
Output: "wke" or "kew"
Length: 3

Input: ""
Output: ""
Length: 0
```

---

## Solution Approaches

### Approach 1: Sliding Window with HashMap (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(min(n, m)) where m = charset size

```java
public static String longestUniqueSubstring(String s) {
    if (s == null || s.length() == 0) return "";
    
    Map<Character, Integer> map = new HashMap<>();
    int maxLen = 0;
    int start = 0;
    int maxStart = 0;
    
    for (int end = 0; end < s.length(); end++) {
        char c = s.charAt(end);
        
        if (map.containsKey(c)) {
            start = Math.max(start, map.get(c) + 1);
        }
        
        map.put(c, end);
        
        if (end - start + 1 > maxLen) {
            maxLen = end - start + 1;
            maxStart = start;
        }
    }
    
    return s.substring(maxStart, maxStart + maxLen);
}
```

---

### Approach 2: Sliding Window with Set

**Time Complexity:** O(n)  
**Space Complexity:** O(min(n, m))

```java
public static String longestUniqueSubstringSet(String s) {
    if (s == null || s.length() == 0) return "";
    
    Set<Character> set = new HashSet<>();
    int maxLen = 0;
    int maxStart = 0;
    int left = 0;
    
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        
        while (set.contains(c)) {
            set.remove(s.charAt(left));
            left++;
        }
        
        set.add(c);
        
        if (right - left + 1 > maxLen) {
            maxLen = right - left + 1;
            maxStart = left;
        }
    }
    
    return s.substring(maxStart, maxStart + maxLen);
}
```

---

### Approach 3: Optimized with Array (ASCII)

**Time Complexity:** O(n)  
**Space Complexity:** O(1) - fixed size 128

```java
public static String longestUniqueSubstringArray(String s) {
    if (s == null || s.length() == 0) return "";
    
    int[] lastIndex = new int[128];
    Arrays.fill(lastIndex, -1);
    
    int maxLen = 0;
    int maxStart = 0;
    int start = 0;
    
    for (int end = 0; end < s.length(); end++) {
        char c = s.charAt(end);
        
        if (lastIndex[c] >= start) {
            start = lastIndex[c] + 1;
        }
        
        lastIndex[c] = end;
        
        if (end - start + 1 > maxLen) {
            maxLen = end - start + 1;
            maxStart = start;
        }
    }
    
    return s.substring(maxStart, maxStart + maxLen);
}
```

---

### Approach 4: Brute Force

**Time Complexity:** O(n³)  
**Space Complexity:** O(min(n, m))

```java
public static String longestUniqueSubstringBrute(String s) {
    if (s == null || s.length() == 0) return "";
    
    int maxLen = 0;
    String result = "";
    
    for (int i = 0; i < s.length(); i++) {
        for (int j = i + 1; j <= s.length(); j++) {
            String sub = s.substring(i, j);
            if (hasUniqueChars(sub) && sub.length() > maxLen) {
                maxLen = sub.length();
                result = sub;
            }
        }
    }
    
    return result;
}

private static boolean hasUniqueChars(String s) {
    Set<Character> set = new HashSet<>();
    for (char c : s.toCharArray()) {
        if (!set.add(c)) return false;
    }
    return true;
}
```

---

## Algorithm Walkthrough

### Example: "aaabcbdeaf"

**Sliding Window with HashMap:**

```
String: a a a b c b d e a f
Index:  0 1 2 3 4 5 6 7 8 9

Initial: start=0, maxLen=0, maxStart=0, map={}

Step 1: end=0, c='a'
  'a' not in map
  map={'a':0}, window="a", len=1
  maxLen=1, maxStart=0

Step 2: end=1, c='a'
  'a' in map at index 0
  start = max(0, 0+1) = 1
  map={'a':1}, window="a", len=1
  maxLen=1

Step 3: end=2, c='a'
  'a' in map at index 1
  start = max(1, 1+1) = 2
  map={'a':2}, window="a", len=1
  maxLen=1

Step 4: end=3, c='b'
  'b' not in map
  map={'a':2, 'b':3}, window="ab", len=2
  maxLen=2, maxStart=2

Step 5: end=4, c='c'
  'c' not in map
  map={'a':2, 'b':3, 'c':4}, window="abc", len=3
  maxLen=3, maxStart=2

Step 6: end=5, c='b'
  'b' in map at index 3
  start = max(2, 3+1) = 4
  map={'a':2, 'b':5, 'c':4}, window="cb", len=2
  maxLen=3

Step 7: end=6, c='d'
  'd' not in map
  map={'a':2, 'b':5, 'c':4, 'd':6}, window="cbd", len=3
  maxLen=3

Step 8: end=7, c='e'
  'e' not in map
  map={'a':2, 'b':5, 'c':4, 'd':6, 'e':7}, window="cbde", len=4
  maxLen=4, maxStart=4

Step 9: end=8, c='a'
  'a' in map at index 2
  start = max(4, 2+1) = 4 (no change)
  map={'a':8, 'b':5, 'c':4, 'd':6, 'e':7}, window="cbdea", len=5
  maxLen=5, maxStart=4

Step 10: end=9, c='f'
  'f' not in map
  map={'a':8, 'b':5, 'c':4, 'd':6, 'e':7, 'f':9}
  window="cbdeaf", len=6
  maxLen=6, maxStart=4

Result: s.substring(4, 10) = "cbdeaf"
```

### Example: "abcabcbb"

```
String: a b c a b c b b
Index:  0 1 2 3 4 5 6 7

end=0: window="a", maxLen=1
end=1: window="ab", maxLen=2
end=2: window="abc", maxLen=3, maxStart=0
end=3: 'a' duplicate, start=1, window="bca", len=3
end=4: 'b' duplicate, start=2, window="cab", len=3
end=5: 'c' duplicate, start=3, window="abc", len=3
end=6: 'b' duplicate, start=5, window="cb", len=2
end=7: 'b' duplicate, start=7, window="b", len=1

Result: s.substring(0, 3) = "abc"
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: HashMap (Optimal)
    public static String longestUniqueSubstring(String s) {
        if (s == null || s.length() == 0) return "";
        
        Map<Character, Integer> map = new HashMap<>();
        int maxLen = 0;
        int start = 0;
        int maxStart = 0;
        
        for (int end = 0; end < s.length(); end++) {
            char c = s.charAt(end);
            
            if (map.containsKey(c)) {
                start = Math.max(start, map.get(c) + 1);
            }
            
            map.put(c, end);
            
            if (end - start + 1 > maxLen) {
                maxLen = end - start + 1;
                maxStart = start;
            }
        }
        
        return s.substring(maxStart, maxStart + maxLen);
    }
    
    // Return length only
    public static int lengthOfLongestSubstring(String s) {
        if (s == null || s.length() == 0) return 0;
        
        Map<Character, Integer> map = new HashMap<>();
        int maxLen = 0;
        int start = 0;
        
        for (int end = 0; end < s.length(); end++) {
            char c = s.charAt(end);
            
            if (map.containsKey(c)) {
                start = Math.max(start, map.get(c) + 1);
            }
            
            map.put(c, end);
            maxLen = Math.max(maxLen, end - start + 1);
        }
        
        return maxLen;
    }
    
    // Approach 2: Set
    public static String longestUniqueSubstringSet(String s) {
        if (s == null || s.length() == 0) return "";
        
        Set<Character> set = new HashSet<>();
        int maxLen = 0;
        int maxStart = 0;
        int left = 0;
        
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            
            while (set.contains(c)) {
                set.remove(s.charAt(left));
                left++;
            }
            
            set.add(c);
            
            if (right - left + 1 > maxLen) {
                maxLen = right - left + 1;
                maxStart = left;
            }
        }
        
        return s.substring(maxStart, maxStart + maxLen);
    }
    
    // Approach 3: Array (ASCII)
    public static String longestUniqueSubstringArray(String s) {
        if (s == null || s.length() == 0) return "";
        
        int[] lastIndex = new int[128];
        Arrays.fill(lastIndex, -1);
        
        int maxLen = 0;
        int maxStart = 0;
        int start = 0;
        
        for (int end = 0; end < s.length(); end++) {
            char c = s.charAt(end);
            
            if (lastIndex[c] >= start) {
                start = lastIndex[c] + 1;
            }
            
            lastIndex[c] = end;
            
            if (end - start + 1 > maxLen) {
                maxLen = end - start + 1;
                maxStart = start;
            }
        }
        
        return s.substring(maxStart, maxStart + maxLen);
    }
    
    public static boolean doTestsPass() {
        // Test 1
        if (!longestUniqueSubstring("aaabcbdeaf").equals("cbdeaf")) return false;
        
        // Test 2
        if (!longestUniqueSubstring("abcabcbb").equals("abc")) return false;
        
        // Test 3
        if (!longestUniqueSubstring("bbbbb").equals("b")) return false;
        
        // Test 4
        String result4 = longestUniqueSubstring("pwwkew");
        if (!result4.equals("wke") && !result4.equals("kew")) return false;
        
        // Test 5
        if (!longestUniqueSubstring("").equals("")) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        String s = "aaabcbdeaf";
        System.out.println("Input: " + s);
        System.out.println("Longest unique substring: " + longestUniqueSubstring(s));
        System.out.println("Length: " + lengthOfLongestSubstring(s));
    }
}
```

---

## Test Cases

```java
@Test
public void testLongestUniqueSubstring() {
    // Test 1: Given example
    assertEquals("cbdeaf", longestUniqueSubstring("aaabcbdeaf"));
    
    // Test 2: Multiple same length
    assertEquals("abc", longestUniqueSubstring("abcabcbb"));
    
    // Test 3: All same characters
    assertEquals("b", longestUniqueSubstring("bbbbb"));
    
    // Test 4: Multiple valid answers
    String result = longestUniqueSubstring("pwwkew");
    assertTrue(result.equals("wke") || result.equals("kew"));
    
    // Test 5: Empty string
    assertEquals("", longestUniqueSubstring(""));
    
    // Test 6: Single character
    assertEquals("a", longestUniqueSubstring("a"));
    
    // Test 7: All unique
    assertEquals("abcdef", longestUniqueSubstring("abcdef"));
    
    // Test 8: Unique at end
    assertEquals("abc", longestUniqueSubstring("aabc"));
    
    // Test 9: Unique at start
    assertEquals("abc", longestUniqueSubstring("abcaa"));
    
    // Test 10: Special characters
    assertEquals("a b!", longestUniqueSubstring("a b!a"));
}
```

---

## Visual Representation

### Sliding Window Movement

```
String: a a a b c b d e a f

Window 1: [a]aa bcbdeaf        → len=1
Window 2: a[a]a bcbdeaf        → len=1
Window 3: aa[a] bcbdeaf        → len=1
Window 4: aaa[b] cbdeaf        → len=1
Window 5: aaa[bc] bdeaf        → len=2
Window 6: aaa[abc] deaf        → len=3
Window 7: aaab[cb] deaf        → len=2
Window 8: aaab[cbd] eaf        → len=3
Window 9: aaab[cbde] af        → len=4
Window 10: aaab[cbdea] f       → len=5
Window 11: aaab[cbdeaf]        → len=6 ✓

Maximum: "cbdeaf" with length 6
```

### HashMap State Changes

```
"abcabcbb"

i=0: map={a:0}, window="a"
i=1: map={a:0,b:1}, window="ab"
i=2: map={a:0,b:1,c:2}, window="abc" ← max
i=3: map={a:3,b:1,c:2}, start=1, window="bca"
i=4: map={a:3,b:4,c:2}, start=2, window="cab"
i=5: map={a:3,b:4,c:5}, start=3, window="abc"
i=6: map={a:3,b:6,c:5}, start=5, window="cb"
i=7: map={a:3,b:7,c:5}, start=7, window="b"

Result: "abc" (first occurrence)
```

---

## Edge Cases

1. **Empty string:** "" → ""
2. **Single character:** "a" → "a"
3. **All same:** "aaaa" → "a"
4. **All unique:** "abcdef" → "abcdef"
5. **Null string:** null → ""
6. **Two characters:** "ab" → "ab"
7. **Repeating pattern:** "abcabc" → "abc"
8. **Special characters:** "a b!" → "a b!"

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(min(n,m)) | **Optimal**, m=charset |
| Set | O(n) | O(min(n,m)) | Slightly slower |
| Array | O(n) | O(1) | Best for ASCII |
| Brute Force | O(n³) | O(min(n,m)) | Not practical |

**Why O(n)?**
- Each character visited at most twice (once by end, once by start)
- HashMap/Set operations: O(1)

**Space Optimization:**
- HashMap/Set: O(min(n, m)) where m = charset size
- Array: O(1) for fixed charset (128 for ASCII)

---

## Related Problems

1. **Longest Substring with At Most K Distinct** - Variant with constraint
2. **Longest Repeating Character Replacement** - With replacements
3. **Minimum Window Substring** - Contains all characters
4. **Permutation in String** - Substring permutation
5. **Find All Anagrams** - All anagram positions
6. **Longest Substring with At Most 2 Distinct** - Specific k=2

---

## Interview Tips

### Clarification Questions
1. What characters are allowed? (ASCII, Unicode, etc.)
2. Case sensitive? (Usually yes)
3. Return substring or length? (Clarify)
4. Multiple answers with same length? (Return any)
5. Empty string input? (Return "")

### Approach Explanation
1. "Use sliding window technique"
2. "Expand window by moving right pointer"
3. "When duplicate found, shrink from left"
4. "Track maximum window size and position"
5. "O(n) time with HashMap"

### Common Mistakes
- Not using Math.max when updating start pointer
- Forgetting to update map with current index
- Wrong substring indices
- Not handling empty string
- Infinite loop in Set approach

### Why HashMap Over Set?
```java
// HashMap: Jump directly to position after duplicate
if (map.containsKey(c)) {
    start = Math.max(start, map.get(c) + 1);
}

// Set: Must remove characters one by one
while (set.contains(c)) {
    set.remove(s.charAt(left));
    left++;
}

HashMap is more efficient (fewer operations)
```

---

## Real-World Applications

1. **Text Processing** - Finding unique character sequences
2. **Data Compression** - Identifying repeating patterns
3. **Password Validation** - Checking character diversity
4. **DNA Sequencing** - Finding unique gene sequences
5. **Network Packets** - Detecting unique data patterns
6. **Cache Systems** - LRU-like behavior

---

## Key Takeaways

1. **Sliding Window:** Optimal pattern for substring problems
2. **HashMap:** Stores last index of each character
3. **Two Pointers:** Start and end define current window
4. **Update Start:** Use Math.max to avoid going backwards
5. **Time Complexity:** O(n) - each character visited at most twice
6. **Space Complexity:** O(min(n, m)) where m = charset size
7. **Return Value:** Can return substring or just length

---

## Additional Notes

### Why Math.max for Start?

```java
start = Math.max(start, map.get(c) + 1);

Example: "abba"
i=0: map={a:0}, start=0, window="a"
i=1: map={a:0,b:1}, start=0, window="ab"
i=2: map={a:0,b:2}, start=2, window="b"
i=3: map={a:3,b:2}, start=max(2, 0+1)=2, window="ba"

Without Math.max: start would go back to 1 ✗
With Math.max: start stays at 2 ✓
```

### HashMap vs Array Performance

```
HashMap:
  - Works for any character set
  - O(1) average operations
  - More memory overhead

Array (ASCII):
  - Only for limited charset
  - O(1) guaranteed
  - Less memory, faster access
  - Best for ASCII strings
```

### Returning All Maximum Substrings

```java
public static List<String> allLongestUniqueSubstrings(String s) {
    List<String> result = new ArrayList<>();
    Map<Character, Integer> map = new HashMap<>();
    int maxLen = 0;
    int start = 0;
    
    for (int end = 0; end < s.length(); end++) {
        char c = s.charAt(end);
        
        if (map.containsKey(c)) {
            start = Math.max(start, map.get(c) + 1);
        }
        
        map.put(c, end);
        int len = end - start + 1;
        
        if (len > maxLen) {
            maxLen = len;
            result.clear();
            result.add(s.substring(start, end + 1));
        } else if (len == maxLen) {
            result.add(s.substring(start, end + 1));
        }
    }
    
    return result;
}
```

### Optimization: Early Exit

```java
// If remaining characters < current max, can't improve
if (s.length() - start <= maxLen) {
    break;
}
```

### Character Frequency Variant

```java
// Find longest substring with at most k distinct characters
public static String longestKDistinct(String s, int k) {
    Map<Character, Integer> map = new HashMap<>();
    int maxLen = 0;
    int maxStart = 0;
    int left = 0;
    
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        map.put(c, map.getOrDefault(c, 0) + 1);
        
        while (map.size() > k) {
            char leftChar = s.charAt(left);
            map.put(leftChar, map.get(leftChar) - 1);
            if (map.get(leftChar) == 0) {
                map.remove(leftChar);
            }
            left++;
        }
        
        if (right - left + 1 > maxLen) {
            maxLen = right - left + 1;
            maxStart = left;
        }
    }
    
    return s.substring(maxStart, maxStart + maxLen);
}
```
