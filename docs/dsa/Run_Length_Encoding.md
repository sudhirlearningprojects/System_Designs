# Run Length Encoding (RLE)

## Problem Statement

Implement a run length encoding function that compresses consecutive repeated characters.

**Input:** String  
**Output:** Encoded string with character followed by count

**Examples:**
```
"a"                → "a1"
"aa"               → "a2"
"aabbb"            → "a2b3"
"aabbbaaabababab"  → "a2b3a3b1a1b1a1b1a1b1"
```

---

## Solution Approach

### Optimal: Single Pass with Counter

**Time Complexity:** O(n)  
**Space Complexity:** O(n) for result string

```java
public String encode(String input) {
    if (input == null || input.isEmpty()) return "";
    
    StringBuilder result = new StringBuilder();
    int count = 1;
    
    for (int i = 1; i < input.length(); i++) {
        if (input.charAt(i) == input.charAt(i - 1)) {
            count++;
        } else {
            result.append(input.charAt(i - 1)).append(count);
            count = 1;
        }
    }
    
    // Append last character and count
    result.append(input.charAt(input.length() - 1)).append(count);
    
    return result.toString();
}
```

---

## Algorithm Walkthrough

### Example: "aabbb"

```
Input: "aabbb"
Index:  01234

i=1: 'a' == 'a' → count = 2
i=2: 'b' != 'a' → append "a2", count = 1
i=3: 'b' == 'b' → count = 2
i=4: 'b' == 'b' → count = 3

After loop: append "b3"

Result: "a2b3"
```

### Example: "aabbbaaabababab"

```
Input: "aabbbaaabababab"

Step-by-step:
i=1: 'a'=='a' → count=2
i=2: 'b'!='a' → append "a2", count=1
i=3: 'b'=='b' → count=2
i=4: 'b'=='b' → count=3
i=5: 'a'!='b' → append "b3", count=1
i=6: 'a'=='a' → count=2
i=7: 'a'=='a' → count=3
i=8: 'b'!='a' → append "a3", count=1
i=9: 'a'!='b' → append "b1", count=1
i=10: 'b'!='a' → append "a1", count=1
i=11: 'a'!='b' → append "b1", count=1
i=12: 'b'!='a' → append "a1", count=1
i=13: 'a'!='b' → append "b1", count=1
i=14: 'b'!='a' → append "a1", count=1

After loop: append "b1"

Result: "a2b3a3b1a1b1a1b1a1b1"
```

---

## Complete Implementation

```java
public class RunLengthEncoding {
    
    // Standard encoding
    public String encode(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int count = 1;
        
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == input.charAt(i - 1)) {
                count++;
            } else {
                result.append(input.charAt(i - 1)).append(count);
                count = 1;
            }
        }
        
        // Append last character and count
        result.append(input.charAt(input.length() - 1)).append(count);
        
        return result.toString();
    }
    
    // Decoding function
    public String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < encoded.length(); i += 2) {
            char ch = encoded.charAt(i);
            int count = Character.getNumericValue(encoded.charAt(i + 1));
            
            for (int j = 0; j < count; j++) {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
    
    // Encoding with multi-digit counts
    public String encodeAdvanced(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int count = 1;
        
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == input.charAt(i - 1)) {
                count++;
            } else {
                result.append(input.charAt(i - 1)).append(count);
                count = 1;
            }
        }
        
        result.append(input.charAt(input.length() - 1)).append(count);
        
        return result.toString();
    }
    
    // Decoding with multi-digit counts
    public String decodeAdvanced(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < encoded.length()) {
            char ch = encoded.charAt(i++);
            
            // Read the count (may be multi-digit)
            int count = 0;
            while (i < encoded.length() && Character.isDigit(encoded.charAt(i))) {
                count = count * 10 + Character.getNumericValue(encoded.charAt(i++));
            }
            
            // Append character count times
            for (int j = 0; j < count; j++) {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
    
    // Optimized: Only encode if beneficial
    public String encodeOptimized(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        int count = 1;
        
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == input.charAt(i - 1)) {
                count++;
            } else {
                // Only encode if count > 1
                result.append(input.charAt(i - 1));
                if (count > 1) {
                    result.append(count);
                }
                count = 1;
            }
        }
        
        result.append(input.charAt(input.length() - 1));
        if (count > 1) {
            result.append(count);
        }
        
        return result.toString();
    }
}
```

---

## Test Cases

```java
@Test
public void testRunLengthEncoding() {
    RunLengthEncoding rle = new RunLengthEncoding();
    
    // Basic cases
    assertEquals("a1", rle.encode("a"));
    assertEquals("a2", rle.encode("aa"));
    assertEquals("a2b3", rle.encode("aabbb"));
    
    // Complex case
    assertEquals("a2b3a3b1a1b1a1b1a1b1", 
                 rle.encode("aabbbaaabababab"));
    
    // All same characters
    assertEquals("a5", rle.encode("aaaaa"));
    
    // No repeats
    assertEquals("a1b1c1d1e1", rle.encode("abcde"));
    
    // Empty string
    assertEquals("", rle.encode(""));
    
    // Single character repeated
    assertEquals("z10", rle.encode("zzzzzzzzzz"));
    
    // Mixed
    assertEquals("a3b2c1d4", rle.encode("aaabbcddd"));
}

@Test
public void testDecoding() {
    RunLengthEncoding rle = new RunLengthEncoding();
    
    // Test encode-decode round trip
    String[] inputs = {"a", "aa", "aabbb", "aabbbaaabababab", "abcde"};
    
    for (String input : inputs) {
        String encoded = rle.encode(input);
        String decoded = rle.decode(encoded);
        assertEquals(input, decoded);
    }
}

@Test
public void testAdvancedEncoding() {
    RunLengthEncoding rle = new RunLengthEncoding();
    
    // Multi-digit counts
    String input = "a".repeat(100);
    assertEquals("a100", rle.encodeAdvanced(input));
    assertEquals(input, rle.decodeAdvanced("a100"));
}
```

---

## Visual Representation

```
Input:  a a b b b
        ↓ ↓ ↓ ↓ ↓
Count:  1 2 1 2 3
        └─┘ └───┘
         a2   b3

Output: "a2b3"
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `""` | Empty string |
| `"a"` | `"a1"` | Single character |
| `"aaaa"` | `"a4"` | All same |
| `"abcd"` | `"a1b1c1d1"` | No repeats |
| `"aabbccdd"` | `"a2b2c2d2"` | All pairs |
| `"aaaaaaaaaa"` (10 a's) | `"a10"` | Multi-digit count |

---

## Common Mistakes

1. **Forgetting Last Character:**
   ```java
   // WRONG - misses last character
   for (int i = 1; i < input.length(); i++) {
       // ... process
   }
   return result.toString();
   
   // CORRECT - append last character after loop
   result.append(input.charAt(input.length() - 1)).append(count);
   ```

2. **Off-by-One in Loop:**
   ```java
   // WRONG - starts at 0, compares with -1
   for (int i = 0; i < input.length(); i++)
   
   // CORRECT - starts at 1, compares with i-1
   for (int i = 1; i < input.length(); i++)
   ```

3. **Not Handling Empty String:**
   ```java
   if (input == null || input.isEmpty()) return "";
   ```

4. **StringBuilder vs String Concatenation:**
   ```java
   // WRONG - O(n²) due to string immutability
   String result = "";
   result += ch + count;
   
   // CORRECT - O(n) with StringBuilder
   StringBuilder result = new StringBuilder();
   result.append(ch).append(count);
   ```

---

## Variations

### Variation 1: Only Encode if Beneficial

```java
public String encodeOptimized(String input) {
    StringBuilder result = new StringBuilder();
    int count = 1;
    
    for (int i = 1; i < input.length(); i++) {
        if (input.charAt(i) == input.charAt(i - 1)) {
            count++;
        } else {
            result.append(input.charAt(i - 1));
            if (count > 1) result.append(count); // Only if > 1
            count = 1;
        }
    }
    
    result.append(input.charAt(input.length() - 1));
    if (count > 1) result.append(count);
    
    return result.toString();
}

// "abcde" → "abcde" (not "a1b1c1d1e1")
```

### Variation 2: Handle Multi-Digit Counts

```java
// For strings with 10+ consecutive characters
// "aaaaaaaaaa" (10 a's) → "a10"

// Decoding needs to parse multi-digit numbers
while (i < encoded.length() && Character.isDigit(encoded.charAt(i))) {
    count = count * 10 + Character.getNumericValue(encoded.charAt(i++));
}
```

### Variation 3: Case-Sensitive

```java
// "AAAaaa" → "A3a3" (not "a6")
// Already handled by default implementation
```

---

## Compression Effectiveness

```
Best Case (high repetition):
  Input:  "aaaaaaaaaa" (10 chars)
  Output: "a10"        (3 chars)
  Ratio:  70% compression

Worst Case (no repetition):
  Input:  "abcde"      (5 chars)
  Output: "a1b1c1d1e1" (10 chars)
  Ratio:  -100% (expansion!)

Typical Case:
  Input:  "aabbbcccc"  (9 chars)
  Output: "a2b3c4"     (6 chars)
  Ratio:  33% compression
```

---

## Complexity Analysis

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Encoding | O(n) | O(n) | Single pass |
| Decoding | O(n) | O(n) | Single pass |
| Space | O(n) | O(n) | StringBuilder |

**Where n = input length**

---

## When to Use RLE

### Good Use Cases ✅
- **Images:** Bitmap compression (BMP, TIFF)
- **Fax Machines:** Black/white document transmission
- **Simple Graphics:** Icons, logos with solid colors
- **Data with Runs:** Scientific data, sensor readings

### Poor Use Cases ❌
- **Text:** Natural language has low repetition
- **Random Data:** No consecutive patterns
- **Already Compressed:** JPEG, MP3, ZIP files
- **Short Strings:** Overhead exceeds benefit

---

## Real-World Applications

1. **Image Compression:**
   - BMP file format
   - PCX graphics format
   - Fax transmission (CCITT Group 3/4)

2. **Video Encoding:**
   - Early video codecs
   - Animation compression

3. **Data Storage:**
   - Database column compression
   - Log file compression

4. **Network Protocols:**
   - Simple data transmission
   - Telemetry data

---

## Related Problems

- **LeetCode 443:** String Compression
- **LeetCode 38:** Count and Say
- **LeetCode 271:** Encode and Decode Strings
- **Image Processing:** Bitmap compression

---

## Interview Tips

1. **Clarify Requirements:**
   - Include count for single characters?
   - Handle multi-digit counts?
   - Case sensitive?

2. **Start with Simple Approach:**
   - Single pass with counter
   - StringBuilder for efficiency

3. **Walk Through Example:**
   - Use "aabbb"
   - Show count tracking

4. **Discuss Edge Cases:**
   - Empty string
   - Single character
   - No repeats

5. **Mention Optimization:**
   - Only encode if count > 1
   - When RLE is beneficial

---

## Advanced: Image RLE Example

```java
// Encode a row of pixels (0 = white, 1 = black)
public String encodePixels(int[] pixels) {
    if (pixels == null || pixels.length == 0) return "";
    
    StringBuilder result = new StringBuilder();
    int count = 1;
    
    for (int i = 1; i < pixels.length; i++) {
        if (pixels[i] == pixels[i - 1]) {
            count++;
        } else {
            result.append(pixels[i - 1]).append(count).append(" ");
            count = 1;
        }
    }
    
    result.append(pixels[pixels.length - 1]).append(count);
    
    return result.toString();
}

// Example:
// Input:  [0,0,0,1,1,0,0,0,0]
// Output: "0 3 1 2 0 4"
```

---

## Performance Optimization

```java
// Pre-allocate StringBuilder capacity
StringBuilder result = new StringBuilder(input.length() * 2);

// Avoid repeated charAt calls
char prev = input.charAt(0);
for (int i = 1; i < input.length(); i++) {
    char curr = input.charAt(i);
    if (curr == prev) {
        count++;
    } else {
        result.append(prev).append(count);
        prev = curr;
        count = 1;
    }
}
```

---

## Key Takeaways

✅ Single pass algorithm - O(n) time  
✅ Track count of consecutive characters  
✅ Don't forget to append last character  
✅ Use StringBuilder for efficiency  
✅ RLE works best with high repetition  
✅ Can expand data if no repetition  
✅ Simple but effective for specific use cases
