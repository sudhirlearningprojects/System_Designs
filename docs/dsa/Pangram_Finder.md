# Pangram Finder - Missing Letters

## Problem Statement

A pangram is a sentence that contains every letter of the alphabet at least once. The famous example is: "The quick brown fox jumps over the lazy dog".

Write a function that takes a string and returns all the letters missing from making it a pangram.

**Examples:**
```
Input:  "The quick brown fox jumps over the lazy dog"
Output: "" (empty - it's a complete pangram)

Input:  "Hello World"
Output: "abcfgijkmnpqstuvxyz"

Input:  "abcdefghijklm"
Output: "nopqrstuvwxyz"
```

---

## Solution Approaches

### Approach 1: HashSet (Most Readable)

**Time Complexity:** O(n)  
**Space Complexity:** O(1) - max 26 letters

```java
public String findMissingLetters(String sentence) {
    Set<Character> seen = new HashSet<>();
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            seen.add(c);
        }
    }
    
    StringBuilder missing = new StringBuilder();
    for (char c = 'a'; c <= 'z'; c++) {
        if (!seen.contains(c)) {
            missing.append(c);
        }
    }
    
    return missing.toString();
}
```

---

### Approach 2: Boolean Array (Most Efficient)

**Time Complexity:** O(n)  
**Space Complexity:** O(1) - fixed 26 booleans

```java
public String findMissingLetters(String sentence) {
    boolean[] present = new boolean[26];
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            present[c - 'a'] = true;
        }
    }
    
    StringBuilder missing = new StringBuilder();
    for (int i = 0; i < 26; i++) {
        if (!present[i]) {
            missing.append((char) ('a' + i));
        }
    }
    
    return missing.toString();
}
```

---

### Approach 3: Bit Manipulation (Most Compact)

**Time Complexity:** O(n)  
**Space Complexity:** O(1) - single integer

```java
public String findMissingLetters(String sentence) {
    int mask = 0;
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            mask |= (1 << (c - 'a'));
        }
    }
    
    StringBuilder missing = new StringBuilder();
    for (int i = 0; i < 26; i++) {
        if ((mask & (1 << i)) == 0) {
            missing.append((char) ('a' + i));
        }
    }
    
    return missing.toString();
}
```

---

## Algorithm Walkthrough

**Input:** "Hello World"

### Step 1: Mark Present Letters
```
Original: "Hello World"
Lowercase: "hello world"
Letters only: h, e, l, l, o, w, o, r, l, d

Present letters: d, e, h, l, o, r, w
```

### Step 2: Find Missing Letters
```
Alphabet: a b c d e f g h i j k l m n o p q r s t u v w x y z
Present:  . . . ✓ ✓ . . ✓ . . . ✓ . . ✓ . . ✓ . . . . ✓ . . .

Missing: a, b, c, f, g, i, j, k, m, n, p, q, s, t, u, v, x, y, z
```

**Output:** "abcfgijkmnpqstuvxyz"

---

## Complete Implementation

```java
public class PangramFinder {
    
    // Approach 1: HashSet (Most Readable)
    public String findMissingLetters(String sentence) {
        Set<Character> seen = new HashSet<>();
        
        for (char c : sentence.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                seen.add(c);
            }
        }
        
        StringBuilder missing = new StringBuilder();
        for (char c = 'a'; c <= 'z'; c++) {
            if (!seen.contains(c)) {
                missing.append(c);
            }
        }
        
        return missing.toString();
    }
    
    // Approach 2: Boolean Array (Most Efficient)
    public String findMissingLettersArray(String sentence) {
        boolean[] present = new boolean[26];
        
        for (char c : sentence.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                present[c - 'a'] = true;
            }
        }
        
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (!present[i]) {
                missing.append((char) ('a' + i));
            }
        }
        
        return missing.toString();
    }
    
    // Approach 3: Bit Manipulation (Most Compact)
    public String findMissingLettersBitwise(String sentence) {
        int mask = 0;
        
        for (char c : sentence.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                mask |= (1 << (c - 'a'));
            }
        }
        
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if ((mask & (1 << i)) == 0) {
                missing.append((char) ('a' + i));
            }
        }
        
        return missing.toString();
    }
    
    // Bonus: Check if pangram
    public boolean isPangram(String sentence) {
        return findMissingLetters(sentence).isEmpty();
    }
    
    // Bonus: Count missing letters
    public int countMissingLetters(String sentence) {
        return findMissingLetters(sentence).length();
    }
}
```

---

## Test Cases

```java
@Test
public void testPangramFinder() {
    PangramFinder finder = new PangramFinder();
    
    // Perfect pangram
    assertEquals("", finder.findMissingLetters("The quick brown fox jumps over the lazy dog"));
    
    // Missing letters
    assertEquals("abcfgijkmnpqstuvxyz", finder.findMissingLetters("Hello World"));
    
    // Only lowercase
    assertEquals("nopqrstuvwxyz", finder.findMissingLetters("abcdefghijklm"));
    
    // Mixed case
    assertEquals("", finder.findMissingLetters("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    
    // With numbers and special chars
    assertEquals("", finder.findMissingLetters("Pack my box with five dozen liquor jugs!"));
    
    // Empty string
    assertEquals("abcdefghijklmnopqrstuvwxyz", finder.findMissingLetters(""));
    
    // Only numbers
    assertEquals("abcdefghijklmnopqrstuvwxyz", finder.findMissingLetters("123456"));
    
    // Single letter
    assertEquals("bcdefghijklmnopqrstuvwxyz", finder.findMissingLetters("a"));
    
    // Check pangram
    assertTrue(finder.isPangram("The quick brown fox jumps over the lazy dog"));
    assertFalse(finder.isPangram("Hello World"));
}
```

---

## Approach Comparison

| Approach | Time | Space | Pros | Cons |
|----------|------|-------|------|------|
| HashSet | O(n) | O(26) | Most readable | Slightly slower |
| Boolean Array | O(n) | O(26) | Fastest | Less intuitive |
| Bit Manipulation | O(n) | O(1) | Most compact | Hardest to understand |

**Recommendation:** Use Boolean Array for production (best balance of speed and clarity)

---

## Bit Manipulation Explained

```java
// Example: Mark letters 'a', 'c', 'e'
int mask = 0;

// 'a' = 0: mask |= (1 << 0) = 0000...0001
mask = 0b00000001

// 'c' = 2: mask |= (1 << 2) = 0000...0100
mask = 0b00000101

// 'e' = 4: mask |= (1 << 4) = 0001...0000
mask = 0b00010101

// Check if 'b' (1) is present:
(mask & (1 << 1)) == 0  // true (missing)

// Check if 'a' (0) is present:
(mask & (1 << 0)) != 0  // true (present)
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `"abcd...xyz"` | Empty string missing all |
| `"123!@#"` | `"abcd...xyz"` | No letters |
| `"aaa"` | `"bcde...xyz"` | Repeated letters |
| `"ABCDEFG..."` | `""` or missing | Case insensitive |
| `"The quick..."` | `""` | Perfect pangram |

---

## Common Mistakes

1. **Case Sensitivity:**
   ```java
   // WRONG - misses uppercase
   if (c >= 'a' && c <= 'z')
   
   // CORRECT
   for (char c : sentence.toLowerCase().toCharArray())
   ```

2. **Including Non-Letters:**
   ```java
   // WRONG - includes spaces, numbers
   seen.add(c);
   
   // CORRECT
   if (c >= 'a' && c <= 'z') {
       seen.add(c);
   }
   ```

3. **Not Checking All 26 Letters:**
   ```java
   // Must iterate through entire alphabet
   for (char c = 'a'; c <= 'z'; c++)
   ```

---

## Variations

### Return Missing Count Only

```java
public int countMissingLetters(String sentence) {
    boolean[] present = new boolean[26];
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            present[c - 'a'] = true;
        }
    }
    
    int count = 0;
    for (boolean p : present) {
        if (!p) count++;
    }
    
    return count;
}
```

### Check if Pangram (Boolean)

```java
public boolean isPangram(String sentence) {
    boolean[] present = new boolean[26];
    int uniqueCount = 0;
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            int index = c - 'a';
            if (!present[index]) {
                present[index] = true;
                uniqueCount++;
                if (uniqueCount == 26) return true;
            }
        }
    }
    
    return false;
}
```

### Find First Missing Letter

```java
public char findFirstMissingLetter(String sentence) {
    boolean[] present = new boolean[26];
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            present[c - 'a'] = true;
        }
    }
    
    for (int i = 0; i < 26; i++) {
        if (!present[i]) {
            return (char) ('a' + i);
        }
    }
    
    return '\0'; // All present
}
```

---

## Related Problems

- **LeetCode 1832:** Check if the Sentence Is Pangram
- **LeetCode 383:** Ransom Note
- **LeetCode 242:** Valid Anagram
- **LeetCode 387:** First Unique Character in a String

---

## Interview Tips

1. **Clarify Requirements:**
   - Case sensitive?
   - Include non-alphabetic characters?
   - Return format (string, list, count)?

2. **Start with HashSet:**
   - Easy to explain
   - Then optimize to boolean array

3. **Mention Optimizations:**
   - Early exit if all 26 found
   - Bit manipulation for space

4. **Walk Through Example:**
   - Use "Hello World"
   - Show letter marking

5. **Discuss Trade-offs:**
   - Readability vs performance
   - HashSet vs Array vs Bitwise

---

## Performance Optimization

### Early Exit Optimization

```java
public boolean isPangramOptimized(String sentence) {
    boolean[] present = new boolean[26];
    int count = 0;
    
    for (char c : sentence.toLowerCase().toCharArray()) {
        if (c >= 'a' && c <= 'z') {
            int index = c - 'a';
            if (!present[index]) {
                present[index] = true;
                if (++count == 26) return true; // Early exit
            }
        }
    }
    
    return false;
}
```

---

## Real-World Applications

- **Text Analysis:** Checking alphabet coverage
- **Cryptography:** Key validation
- **Typography:** Font testing (pangrams test all glyphs)
- **Education:** Typing practice sentences
- **Data Validation:** Ensuring complete character sets

---

## Famous Pangrams

```
"The quick brown fox jumps over the lazy dog" (35 letters)
"Pack my box with five dozen liquor jugs" (32 letters)
"How vexingly quick daft zebras jump!" (30 letters)
"Sphinx of black quartz, judge my vow" (29 letters)
```

---

## Key Takeaways

✅ O(n) time complexity - single pass  
✅ O(1) space - fixed 26 letters  
✅ Case-insensitive comparison required  
✅ Ignore non-alphabetic characters  
✅ Boolean array is optimal for production  
✅ Bit manipulation for space-constrained environments
