# Longest Uniform Substring

## Problem Statement

Find the longest substring where all characters are the same (uniform).

**Input:** String  
**Output:** Starting index and length of the longest uniform substring

**Example:**
```
Input:  "abbbccda"
Output: index=1, length=3 (substring "bbb")
```

---

## Solution Approach

### Optimal: Single Pass with Sliding Window

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public int[] longestUniformSubstring(String s) {
    if (s == null || s.isEmpty()) return new int[]{-1, 0};
    
    int maxStart = 0, maxLen = 1;
    int currStart = 0, currLen = 1;
    
    for (int i = 1; i < s.length(); i++) {
        if (s.charAt(i) == s.charAt(i - 1)) {
            currLen++;
        } else {
            if (currLen > maxLen) {
                maxLen = currLen;
                maxStart = currStart;
            }
            currStart = i;
            currLen = 1;
        }
    }
    
    // Check last sequence
    if (currLen > maxLen) {
        maxLen = currLen;
        maxStart = currStart;
    }
    
    return new int[]{maxStart, maxLen};
}
```

---

## Algorithm Walkthrough

**Input:** "abbbccda"

```
Index:  0 1 2 3 4 5 6 7
Char:   a b b b c c d a

i=1: 'b' != 'a' → Reset: currStart=1, currLen=1
i=2: 'b' == 'b' → Extend: currLen=2
i=3: 'b' == 'b' → Extend: currLen=3
i=4: 'c' != 'b' → Update max: maxStart=1, maxLen=3
                  Reset: currStart=4, currLen=1
i=5: 'c' == 'c' → Extend: currLen=2
i=6: 'd' != 'c' → currLen=2 < maxLen=3, no update
                  Reset: currStart=6, currLen=1
i=7: 'a' != 'd' → currLen=1 < maxLen=3, no update

Final check: currLen=1 < maxLen=3

Result: [1, 3] → "bbb"
```

---

## Complete Implementation

```java
public class LongestUniformSubstring {
    
    public int[] longestUniformSubstring(String s) {
        if (s == null || s.isEmpty()) return new int[]{-1, 0};
        
        int maxStart = 0, maxLen = 1;
        int currStart = 0, currLen = 1;
        
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1)) {
                currLen++;
            } else {
                if (currLen > maxLen) {
                    maxLen = currLen;
                    maxStart = currStart;
                }
                currStart = i;
                currLen = 1;
            }
        }
        
        if (currLen > maxLen) {
            maxLen = currLen;
            maxStart = currStart;
        }
        
        return new int[]{maxStart, maxLen};
    }
    
    // Alternative: Return substring directly
    public String longestUniformSubstringStr(String s) {
        int[] result = longestUniformSubstring(s);
        if (result[1] == 0) return "";
        return s.substring(result[0], result[0] + result[1]);
    }
}
```

---

## Test Cases

```java
@Test
public void testLongestUniformSubstring() {
    LongestUniformSubstring solver = new LongestUniformSubstring();
    
    // Basic case
    assertArrayEquals(new int[]{1, 3}, solver.longestUniformSubstring("abbbccda"));
    
    // All same characters
    assertArrayEquals(new int[]{0, 5}, solver.longestUniformSubstring("aaaaa"));
    
    // No repeating characters
    assertArrayEquals(new int[]{0, 1}, solver.longestUniformSubstring("abcdef"));
    
    // Multiple sequences of same length
    assertArrayEquals(new int[]{0, 2}, solver.longestUniformSubstring("aabbcc"));
    
    // Longest at the end
    assertArrayEquals(new int[]{3, 4}, solver.longestUniformSubstring("abcddddd"));
    
    // Single character
    assertArrayEquals(new int[]{0, 1}, solver.longestUniformSubstring("a"));
    
    // Empty string
    assertArrayEquals(new int[]{-1, 0}, solver.longestUniformSubstring(""));
    
    // Two characters
    assertArrayEquals(new int[]{0, 2}, solver.longestUniformSubstring("aa"));
    assertArrayEquals(new int[]{0, 1}, solver.longestUniformSubstring("ab"));
}
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `[-1, 0]` | Empty string |
| `"a"` | `[0, 1]` | Single character |
| `"aaaaa"` | `[0, 5]` | All same |
| `"abcdef"` | `[0, 1]` | No repeats |
| `"aabbcc"` | `[0, 2]` | First occurrence wins |
| `"abcddddd"` | `[3, 4]` | Longest at end |

---

## Key Insights

1. **Single Pass:** Only need one iteration through the string
2. **Two Pointers:** Track current sequence and maximum sequence
3. **Final Check:** Don't forget to check the last sequence
4. **First Occurrence:** When multiple sequences have same length, return first

---

## Common Mistakes

1. **Forgetting Final Check:**
   ```java
   // WRONG - misses last sequence
   for (int i = 1; i < s.length(); i++) {
       // ... update logic
   }
   return new int[]{maxStart, maxLen};
   
   // CORRECT
   if (currLen > maxLen) {
       maxLen = currLen;
       maxStart = currStart;
   }
   ```

2. **Off-by-One Errors:**
   ```java
   // Start loop at i=1, not i=0
   for (int i = 1; i < s.length(); i++)
   ```

3. **Null/Empty Handling:**
   ```java
   if (s == null || s.isEmpty()) return new int[]{-1, 0};
   ```

---

## Variations

### Return All Longest Uniform Substrings

```java
public List<int[]> allLongestUniformSubstrings(String s) {
    if (s == null || s.isEmpty()) return new ArrayList<>();
    
    List<int[]> result = new ArrayList<>();
    int maxLen = 1;
    int currStart = 0, currLen = 1;
    
    for (int i = 1; i < s.length(); i++) {
        if (s.charAt(i) == s.charAt(i - 1)) {
            currLen++;
        } else {
            if (currLen > maxLen) {
                result.clear();
                result.add(new int[]{currStart, currLen});
                maxLen = currLen;
            } else if (currLen == maxLen) {
                result.add(new int[]{currStart, currLen});
            }
            currStart = i;
            currLen = 1;
        }
    }
    
    if (currLen > maxLen) {
        result.clear();
        result.add(new int[]{currStart, currLen});
    } else if (currLen == maxLen) {
        result.add(new int[]{currStart, currLen});
    }
    
    return result;
}
```

### Count All Uniform Substrings

```java
public int countUniformSubstrings(String s) {
    if (s == null || s.isEmpty()) return 0;
    
    int count = 0;
    int currLen = 1;
    
    for (int i = 1; i < s.length(); i++) {
        if (s.charAt(i) == s.charAt(i - 1)) {
            currLen++;
        } else {
            count += currLen * (currLen + 1) / 2;
            currLen = 1;
        }
    }
    count += currLen * (currLen + 1) / 2;
    
    return count;
}
```

**Example:** "aaa" has 6 uniform substrings: "a", "a", "a", "aa", "aa", "aaa"

---

## Related Problems

- **LeetCode 424:** Longest Repeating Character Replacement
- **LeetCode 3:** Longest Substring Without Repeating Characters
- **LeetCode 1446:** Consecutive Characters
- **LeetCode 485:** Max Consecutive Ones

---

## Interview Tips

1. **Clarify Requirements:**
   - Return index or substring?
   - First occurrence or any?
   - Handle empty/null input?

2. **Start Simple:**
   - Explain single-pass approach
   - Mention O(n) time, O(1) space

3. **Walk Through Example:**
   - Use "abbbccda" to demonstrate

4. **Discuss Edge Cases:**
   - Empty string
   - Single character
   - All same characters

5. **Optimize:**
   - Already optimal at O(n)
   - No additional space needed

---

## Complexity Analysis

| Metric | Value | Explanation |
|--------|-------|-------------|
| Time | O(n) | Single pass through string |
| Space | O(1) | Only tracking indices and lengths |
| Best Case | O(n) | Must check all characters |
| Worst Case | O(n) | Must check all characters |

---

## Real-World Applications

- **Data Compression:** Run-length encoding (RLE)
- **Pattern Recognition:** Detecting repeated sequences
- **Log Analysis:** Finding consecutive error patterns
- **Bioinformatics:** DNA sequence analysis
- **Text Processing:** Identifying repeated characters

---

## Key Takeaways

✅ Single pass solution with O(n) time  
✅ Track current and maximum sequences simultaneously  
✅ Don't forget to check the last sequence  
✅ Handle edge cases (empty, single character)  
✅ Simple sliding window pattern
