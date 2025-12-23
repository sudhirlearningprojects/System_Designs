# First Non-Repeating Character

## Problem Statement

Find the first character that does not repeat anywhere in the input string.

**Input:** String  
**Output:** First non-repeating character (or null/special value if none exists)

**Examples:**
```
"apple"     → 'a'
"racecars"  → 'e'
"aabbcc"    → null (no non-repeating character)
"abcabc"    → null
"abcd"      → 'a'
```

---

## Solution Approaches

### Approach 1: HashMap (Two Pass) - OPTIMAL

**Time Complexity:** O(n)  
**Space Complexity:** O(k) where k = unique characters (max 26 for lowercase)

```java
public Character firstNonRepeating(String s) {
    if (s == null || s.isEmpty()) return null;
    
    Map<Character, Integer> count = new HashMap<>();
    
    // Count occurrences
    for (char c : s.toCharArray()) {
        count.put(c, count.getOrDefault(c, 0) + 1);
    }
    
    // Find first with count = 1
    for (char c : s.toCharArray()) {
        if (count.get(c) == 1) {
            return c;
        }
    }
    
    return null;
}
```

---

### Approach 2: Array (For Lowercase Letters Only)

**Time Complexity:** O(n)  
**Space Complexity:** O(1) - fixed 26 size

```java
public Character firstNonRepeating(String s) {
    if (s == null || s.isEmpty()) return null;
    
    int[] count = new int[26];
    
    // Count occurrences
    for (char c : s.toCharArray()) {
        count[c - 'a']++;
    }
    
    // Find first with count = 1
    for (char c : s.toCharArray()) {
        if (count[c - 'a'] == 1) {
            return c;
        }
    }
    
    return null;
}
```

---

### Approach 3: LinkedHashMap (Preserves Order)

**Time Complexity:** O(n)  
**Space Complexity:** O(k)

```java
public Character firstNonRepeating(String s) {
    if (s == null || s.isEmpty()) return null;
    
    Map<Character, Integer> count = new LinkedHashMap<>();
    
    for (char c : s.toCharArray()) {
        count.put(c, count.getOrDefault(c, 0) + 1);
    }
    
    for (Map.Entry<Character, Integer> entry : count.entrySet()) {
        if (entry.getValue() == 1) {
            return entry.getKey();
        }
    }
    
    return null;
}
```

---

## Algorithm Walkthrough

### Example 1: "apple"

```
Step 1: Count occurrences
  a: 1
  p: 2
  l: 1
  e: 1

Step 2: Find first with count = 1
  Check 'a': count = 1 ✓

Result: 'a'
```

### Example 2: "racecars"

```
Step 1: Count occurrences
  r: 2
  a: 2
  c: 2
  e: 1
  s: 1

Step 2: Find first with count = 1
  Check 'r': count = 2 ✗
  Check 'a': count = 2 ✗
  Check 'c': count = 2 ✗
  Check 'e': count = 1 ✓

Result: 'e'
```

### Example 3: "aabbcc"

```
Step 1: Count occurrences
  a: 2
  b: 2
  c: 2

Step 2: Find first with count = 1
  Check 'a': count = 2 ✗
  Check 'b': count = 2 ✗
  Check 'c': count = 2 ✗

Result: null (no non-repeating character)
```

---

## Complete Implementation

```java
public class FirstNonRepeatingChar {
    
    // Approach 1: HashMap (works for all characters)
    public Character firstNonRepeating(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        
        Map<Character, Integer> count = new HashMap<>();
        
        // First pass: count occurrences
        for (char c : s.toCharArray()) {
            count.put(c, count.getOrDefault(c, 0) + 1);
        }
        
        // Second pass: find first with count = 1
        for (char c : s.toCharArray()) {
            if (count.get(c) == 1) {
                return c;
            }
        }
        
        return null;
    }
    
    // Approach 2: Array (lowercase letters only)
    public Character firstNonRepeatingLowercase(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        
        int[] count = new int[26];
        
        for (char c : s.toLowerCase().toCharArray()) {
            count[c - 'a']++;
        }
        
        for (char c : s.toLowerCase().toCharArray()) {
            if (count[c - 'a'] == 1) {
                return c;
            }
        }
        
        return null;
    }
    
    // Approach 3: Return index instead of character
    public int firstNonRepeatingIndex(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        
        Map<Character, Integer> count = new HashMap<>();
        
        for (char c : s.toCharArray()) {
            count.put(c, count.getOrDefault(c, 0) + 1);
        }
        
        for (int i = 0; i < s.length(); i++) {
            if (count.get(s.charAt(i)) == 1) {
                return i;
            }
        }
        
        return -1;
    }
    
    // Approach 4: Return all non-repeating characters
    public List<Character> allNonRepeating(String s) {
        List<Character> result = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return result;
        }
        
        Map<Character, Integer> count = new HashMap<>();
        
        for (char c : s.toCharArray()) {
            count.put(c, count.getOrDefault(c, 0) + 1);
        }
        
        for (char c : s.toCharArray()) {
            if (count.get(c) == 1) {
                result.add(c);
            }
        }
        
        return result;
    }
    
    // Approach 5: Using frequency map with first occurrence index
    public Character firstNonRepeatingOptimized(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        
        Map<Character, Integer> firstIndex = new LinkedHashMap<>();
        Map<Character, Integer> count = new HashMap<>();
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            count.put(c, count.getOrDefault(c, 0) + 1);
            if (!firstIndex.containsKey(c)) {
                firstIndex.put(c, i);
            }
        }
        
        int minIndex = Integer.MAX_VALUE;
        Character result = null;
        
        for (Map.Entry<Character, Integer> entry : count.entrySet()) {
            if (entry.getValue() == 1) {
                int index = firstIndex.get(entry.getKey());
                if (index < minIndex) {
                    minIndex = index;
                    result = entry.getKey();
                }
            }
        }
        
        return result;
    }
}
```

---

## Test Cases

```java
@Test
public void testFirstNonRepeating() {
    FirstNonRepeatingChar solver = new FirstNonRepeatingChar();
    
    // Basic cases
    assertEquals('a', (char) solver.firstNonRepeating("apple"));
    assertEquals('e', (char) solver.firstNonRepeating("racecars"));
    
    // No non-repeating character
    assertNull(solver.firstNonRepeating("aabbcc"));
    assertNull(solver.firstNonRepeating("abcabc"));
    
    // All unique
    assertEquals('a', (char) solver.firstNonRepeating("abcd"));
    
    // Single character
    assertEquals('a', (char) solver.firstNonRepeating("a"));
    
    // Empty string
    assertNull(solver.firstNonRepeating(""));
    
    // All same
    assertNull(solver.firstNonRepeating("aaaa"));
    
    // Non-repeating at end
    assertEquals('d', (char) solver.firstNonRepeating("aabbccd"));
    
    // Case sensitive
    assertEquals('A', (char) solver.firstNonRepeating("AaBbCc"));
    
    // With spaces
    assertEquals(' ', (char) solver.firstNonRepeating("aabbcc "));
    
    // Index version
    assertEquals(0, solver.firstNonRepeatingIndex("apple"));
    assertEquals(3, solver.firstNonRepeatingIndex("racecars"));
    assertEquals(-1, solver.firstNonRepeatingIndex("aabbcc"));
}
```

---

## Visual Representation

```
Input: "racecars"

Step 1: Build frequency map
┌───┬───┬───┬───┬───┐
│ r │ a │ c │ e │ s │
├───┼───┼───┼───┼───┤
│ 2 │ 2 │ 2 │ 1 │ 1 │
└───┴───┴───┴───┴───┘

Step 2: Scan original string
r → count=2 ✗
a → count=2 ✗
c → count=2 ✗
e → count=1 ✓ FOUND

Result: 'e'
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `null` | Empty string |
| `"a"` | `'a'` | Single character |
| `"aa"` | `null` | All repeating |
| `"aabbcc"` | `null` | No unique |
| `"abcd"` | `'a'` | All unique, return first |
| `"aabbccd"` | `'d'` | Unique at end |
| `"AaBbCc"` | `'A'` | Case sensitive |

---

## Common Mistakes

1. **Not Preserving Order:**
   ```java
   // WRONG - HashMap doesn't preserve insertion order
   for (Character c : count.keySet()) {
       if (count.get(c) == 1) return c;
   }
   
   // CORRECT - Iterate through original string
   for (char c : s.toCharArray()) {
       if (count.get(c) == 1) return c;
   }
   ```

2. **Single Pass Attempt (Incorrect):**
   ```java
   // WRONG - can't determine if character repeats later
   if (!seen.contains(c)) return c;
   
   // CORRECT - need two passes
   ```

3. **Not Handling Empty String:**
   ```java
   if (s == null || s.isEmpty()) return null;
   ```

4. **Case Sensitivity:**
   ```java
   // Clarify if 'A' and 'a' are different
   s.toLowerCase() // if case-insensitive
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(k) | k = unique chars |
| Array (26) | O(n) | O(1) | Lowercase only |
| LinkedHashMap | O(n) | O(k) | Preserves order |
| Brute Force | O(n²) | O(1) | Not recommended |

**Where:**
- n = string length
- k = unique characters (max 26 for lowercase, 52 for both cases, 128 for ASCII)

---

## Optimization for Lowercase Only

```java
public Character firstNonRepeatingFast(String s) {
    int[] count = new int[26];
    
    // Single loop to count
    for (char c : s.toCharArray()) {
        count[c - 'a']++;
    }
    
    // Find first with count = 1
    for (char c : s.toCharArray()) {
        if (count[c - 'a'] == 1) {
            return c;
        }
    }
    
    return null;
}
```

**Benefits:**
- O(1) space (fixed 26 size)
- Faster than HashMap (no hashing overhead)
- Cache-friendly (contiguous array)

---

## Variations

### Return Index Instead of Character

```java
public int firstNonRepeatingIndex(String s) {
    Map<Character, Integer> count = new HashMap<>();
    
    for (char c : s.toCharArray()) {
        count.put(c, count.getOrDefault(c, 0) + 1);
    }
    
    for (int i = 0; i < s.length(); i++) {
        if (count.get(s.charAt(i)) == 1) {
            return i;
        }
    }
    
    return -1;
}
```

### Find All Non-Repeating Characters

```java
public List<Character> allNonRepeating(String s) {
    Map<Character, Integer> count = new HashMap<>();
    
    for (char c : s.toCharArray()) {
        count.put(c, count.getOrDefault(c, 0) + 1);
    }
    
    List<Character> result = new ArrayList<>();
    for (char c : s.toCharArray()) {
        if (count.get(c) == 1 && !result.contains(c)) {
            result.add(c);
        }
    }
    
    return result;
}
```

### Stream API Solution

```java
public Character firstNonRepeatingStream(String s) {
    return s.chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue() == 1)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
}
```

---

## Related Problems

- **LeetCode 387:** First Unique Character in a String
- **LeetCode 451:** Sort Characters By Frequency
- **LeetCode 1941:** Check if All Characters Have Equal Number of Occurrences
- **LeetCode 383:** Ransom Note

---

## Interview Tips

1. **Clarify Requirements:**
   - Return character or index?
   - Case sensitive?
   - What if no non-repeating character?
   - Character set (ASCII, Unicode)?

2. **Start with HashMap:**
   - Two-pass approach
   - Count then find

3. **Optimize if Needed:**
   - Array for lowercase only
   - O(1) space

4. **Walk Through Example:**
   - Use "racecars"
   - Show frequency map

5. **Discuss Trade-offs:**
   - HashMap vs Array
   - Space vs constraints

---

## Real-World Applications

- **Text Processing:** Finding unique identifiers
- **Data Validation:** Checking for duplicate entries
- **Compression:** Identifying compressible patterns
- **Cryptography:** Frequency analysis
- **Natural Language Processing:** Character distribution analysis

---

## Why Two Passes?

```
Single pass doesn't work:

"abcabc"
 ^
First 'a' looks unique, but repeats later!

Need to:
1. Count all occurrences first
2. Then find first with count = 1
```

---

## Performance Comparison

```
Input: "racecars" (8 characters)

HashMap Approach:
- Pass 1: 8 operations (count)
- Pass 2: 4 operations (find 'e')
- Total: 12 operations

Array Approach (lowercase):
- Pass 1: 8 operations (count)
- Pass 2: 4 operations (find 'e')
- Total: 12 operations
- But faster due to array access vs hashing

Brute Force (nested loop):
- For each char, scan entire string
- Total: 8 × 8 = 64 operations
```

---

## Key Takeaways

✅ Two-pass algorithm: count then find  
✅ HashMap for general case - O(n) time, O(k) space  
✅ Array for lowercase only - O(n) time, O(1) space  
✅ Must iterate original string to preserve order  
✅ Cannot solve in single pass (need full count first)  
✅ Return null/special value if no non-repeating char  
✅ Array approach is faster for constrained character sets
