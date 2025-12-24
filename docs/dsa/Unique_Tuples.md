# Unique Tuples (Substrings of Fixed Length)

## Problem Statement

Given a string and a length, return all unique substrings of that length.

**Input:** String and integer length  
**Output:** Set of unique substrings

**Examples:**
```
uniqueTuples("aab", 2) → {"aa", "ab"}
uniqueTuples("abc", 2) → {"ab", "bc"}
uniqueTuples("aaa", 2) → {"aa"}
uniqueTuples("hello", 3) → {"hel", "ell", "llo"}
```

---

## Solution Approach

### Optimal: Sliding Window

**Time Complexity:** O(n × len) where n = string length  
**Space Complexity:** O(k × len) where k = unique tuples

```java
public static HashSet<String> uniqueTuples(String input, int len) {
    HashSet<String> result = new HashSet<>();
    
    for (int i = 0; i <= input.length() - len; i++) {
        result.add(input.substring(i, i + len));
    }
    
    return result;
}
```

---

## Algorithm Walkthrough

### Example: "aab", len = 2

```
Input: "aab"
Length: 2

Step 1: i = 0
  substring(0, 2) = "aa"
  result = {"aa"}

Step 2: i = 1
  substring(1, 3) = "ab"
  result = {"aa", "ab"}

Step 3: i = 2
  i > input.length() - len (2 > 1)
  Stop

Result: {"aa", "ab"}
```

### Example: "hello", len = 3

```
Input: "hello"
Length: 3

i=0: substring(0, 3) = "hel" → {"hel"}
i=1: substring(1, 4) = "ell" → {"hel", "ell"}
i=2: substring(2, 5) = "llo" → {"hel", "ell", "llo"}

Result: {"hel", "ell", "llo"}
```

---

## Complete Implementation

```java
public class UniqueTuples {
    
    // Approach 1: Simple sliding window (Recommended)
    public static HashSet<String> uniqueTuples(String input, int len) {
        HashSet<String> result = new HashSet<>();
        
        if (input == null || len <= 0 || len > input.length()) {
            return result;
        }
        
        for (int i = 0; i <= input.length() - len; i++) {
            result.add(input.substring(i, i + len));
        }
        
        return result;
    }
    
    // Approach 2: With validation
    public static HashSet<String> uniqueTuplesValidated(String input, int len) {
        HashSet<String> result = new HashSet<>();
        
        if (input == null || input.isEmpty()) {
            return result;
        }
        
        if (len <= 0 || len > input.length()) {
            return result;
        }
        
        for (int i = 0; i <= input.length() - len; i++) {
            result.add(input.substring(i, i + len));
        }
        
        return result;
    }
    
    // Approach 3: Using StringBuilder (for large strings)
    public static HashSet<String> uniqueTuplesStringBuilder(String input, int len) {
        HashSet<String> result = new HashSet<>();
        
        if (input == null || len <= 0 || len > input.length()) {
            return result;
        }
        
        StringBuilder sb = new StringBuilder(input.substring(0, len));
        result.add(sb.toString());
        
        for (int i = len; i < input.length(); i++) {
            sb.deleteCharAt(0);
            sb.append(input.charAt(i));
            result.add(sb.toString());
        }
        
        return result;
    }
    
    // Bonus: Return with count
    public static Map<String, Integer> uniqueTuplesWithCount(String input, int len) {
        Map<String, Integer> result = new HashMap<>();
        
        for (int i = 0; i <= input.length() - len; i++) {
            String tuple = input.substring(i, i + len);
            result.put(tuple, result.getOrDefault(tuple, 0) + 1);
        }
        
        return result;
    }
    
    // Bonus: All unique tuples of any length
    public static Set<String> allUniqueTuples(String input) {
        Set<String> result = new HashSet<>();
        
        for (int len = 1; len <= input.length(); len++) {
            for (int i = 0; i <= input.length() - len; i++) {
                result.add(input.substring(i, i + len));
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        String input = "aab";
        HashSet<String> result = uniqueTuples(input, 2);
        
        if (result.contains("aa") && result.contains("ab")) {
            System.out.println("Test passed.");
        } else {
            System.out.println("Test failed.");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testUniqueTuples() {
    // Basic case
    assertEquals(Set.of("aa", "ab"), uniqueTuples("aab", 2));
    
    // No duplicates
    assertEquals(Set.of("ab", "bc"), uniqueTuples("abc", 2));
    
    // All same
    assertEquals(Set.of("aa"), uniqueTuples("aaa", 2));
    
    // Length 1
    assertEquals(Set.of("a", "b", "c"), uniqueTuples("abc", 1));
    
    // Full string
    assertEquals(Set.of("abc"), uniqueTuples("abc", 3));
    
    // Empty result
    assertEquals(Set.of(), uniqueTuples("ab", 3));
    
    // Empty string
    assertEquals(Set.of(), uniqueTuples("", 2));
    
    // Length 0
    assertEquals(Set.of(), uniqueTuples("abc", 0));
}
```

---

## Visual Representation

```
Input: "aab", len = 2

String: a a b
Index:  0 1 2

Window positions:
[a a] b  → "aa"
  a [a b] → "ab"

Result: {"aa", "ab"}
```

---

## Edge Cases

| Input | Length | Output | Explanation |
|-------|--------|--------|-------------|
| `"aab"` | `2` | `{"aa","ab"}` | Normal case |
| `"abc"` | `1` | `{"a","b","c"}` | Single chars |
| `"aaa"` | `2` | `{"aa"}` | All same |
| `"ab"` | `3` | `{}` | len > string |
| `""` | `2` | `{}` | Empty string |
| `"abc"` | `0` | `{}` | Invalid length |

---

## Common Mistakes

1. **Off-by-One Error:**
   ```java
   // WRONG - goes out of bounds
   for (int i = 0; i < input.length(); i++)
   
   // CORRECT
   for (int i = 0; i <= input.length() - len; i++)
   ```

2. **Not Using HashSet:**
   ```java
   // WRONG - allows duplicates
   List<String> result = new ArrayList<>();
   
   // CORRECT - automatic deduplication
   HashSet<String> result = new HashSet<>();
   ```

3. **Wrong Substring Indices:**
   ```java
   // WRONG
   input.substring(i, len)
   
   // CORRECT
   input.substring(i, i + len)
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Sliding window | O(n × len) | O(k × len) | k = unique tuples |
| StringBuilder | O(n × len) | O(k × len) | Slightly faster |
| All lengths | O(n³) | O(n²) | All substrings |

---

## Sliding Window Visualization

```
"hello", len = 3

Position 0: [hel]lo → "hel"
Position 1: h[ell]o → "ell"
Position 2: he[llo] → "llo"

Each position: O(len) to create substring
Total: O(n × len)
```

---

## Related Problems

- **LeetCode 187:** Repeated DNA Sequences
- **LeetCode 1044:** Longest Duplicate Substring
- **LeetCode 3:** Longest Substring Without Repeating Characters
- **Rabin-Karp algorithm:** String matching

---

## Interview Tips

1. **Clarify Requirements:**
   - Case sensitive?
   - Empty string handling?
   - Invalid length?
   - Return format?

2. **Explain Approach:**
   - Sliding window of size len
   - HashSet for uniqueness
   - O(n × len) complexity

3. **Walk Through Example:**
   - Use "aab" with len=2
   - Show each window position

4. **Discuss Optimization:**
   - StringBuilder for large strings
   - Rolling hash for very long strings

5. **Handle Edge Cases:**
   - len > string length
   - Empty string
   - len = 0

---

## Real-World Applications

- **DNA Sequencing:** Finding k-mers
- **Text Analysis:** N-gram generation
- **Pattern Recognition:** Substring patterns
- **Data Compression:** Finding repeated patterns
- **Plagiarism Detection:** Text similarity
- **Natural Language Processing:** Language models

---

## Optimization: Rolling Hash

```java
// For very long strings, use rolling hash
public static Set<String> uniqueTuplesRollingHash(String input, int len) {
    Set<Long> hashes = new HashSet<>();
    Set<String> result = new HashSet<>();
    
    long hash = 0;
    long pow = 1;
    int base = 31;
    
    // Calculate initial hash
    for (int i = 0; i < len; i++) {
        hash = hash * base + input.charAt(i);
        if (i < len - 1) pow *= base;
    }
    hashes.add(hash);
    result.add(input.substring(0, len));
    
    // Rolling hash
    for (int i = len; i < input.length(); i++) {
        hash = (hash - input.charAt(i - len) * pow) * base + input.charAt(i);
        if (hashes.add(hash)) {
            result.add(input.substring(i - len + 1, i + 1));
        }
    }
    
    return result;
}
```

---

## Key Takeaways

✅ Use sliding window to extract substrings  
✅ HashSet automatically handles duplicates  
✅ Loop condition: `i <= input.length() - len`  
✅ Substring: `input.substring(i, i + len)`  
✅ O(n × len) time complexity  
✅ Handle edge cases: empty string, invalid length  
✅ StringBuilder optimization for large strings  
✅ Rolling hash for very long strings
