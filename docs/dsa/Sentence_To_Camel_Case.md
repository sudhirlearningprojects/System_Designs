# Convert Sentence to Camel Case

## Problem Statement

Convert a sentence to Camel Case by removing spaces and capitalizing the first letter of each word. Camel Case is a style of writing where we don't have spaces and all words begin with capital letters.

**Input:** String sentence with spaces  
**Output:** String in Camel Case (no spaces, each word capitalized)

**Examples:**
```
Input:  "I got intern at geeksforgeeks"
Output: "IGotInternAtGeeksforgeeks"

Input:  "here comes the sun"
Output: "HereComesTheSun"

Input:  "hello world"
Output: "HelloWorld"

Input:  "camel case conversion"
Output: "CamelCaseConversion"

Input:  "a b c"
Output: "ABC"

Input:  "  multiple   spaces  "
Output: "MultipleSpaces"
Explanation: Handle multiple spaces and trim
```

---

## Solution Approaches

### Approach 1: Split and Capitalize (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String toCamelCase(String sentence) {
    if (sentence == null || sentence.trim().isEmpty()) return "";
    
    StringBuilder result = new StringBuilder();
    String[] words = sentence.trim().split("\\s+");
    
    for (String word : words) {
        if (!word.isEmpty()) {
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase());
        }
    }
    
    return result.toString();
}
```

---

### Approach 2: Single Pass with Flag

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String toCamelCaseSinglePass(String sentence) {
    if (sentence == null || sentence.isEmpty()) return "";
    
    StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;
    
    for (char c : sentence.toCharArray()) {
        if (c == ' ') {
            capitalizeNext = true;
        } else {
            if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
    }
    
    return result.toString();
}
```

---

### Approach 3: Stream API (Functional)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String toCamelCaseStream(String sentence) {
    if (sentence == null || sentence.trim().isEmpty()) return "";
    
    return Arrays.stream(sentence.trim().split("\\s+"))
        .filter(word -> !word.isEmpty())
        .map(word -> Character.toUpperCase(word.charAt(0)) + 
                     word.substring(1).toLowerCase())
        .collect(Collectors.joining());
}
```

---

## Algorithm Walkthrough

### Example: "I got intern at geeksforgeeks"

**Step-by-Step Execution (Split Approach):**

```
Input: "I got intern at geeksforgeeks"

Step 1: Split by spaces
  words = ["I", "got", "intern", "at", "geeksforgeeks"]

Step 2: Process "I"
  Capitalize: 'I' → 'I'
  Lowercase rest: "" → ""
  result = "I"

Step 3: Process "got"
  Capitalize: 'g' → 'G'
  Lowercase rest: "ot" → "ot"
  result = "IGot"

Step 4: Process "intern"
  Capitalize: 'i' → 'I'
  Lowercase rest: "ntern" → "ntern"
  result = "IGotIntern"

Step 5: Process "at"
  Capitalize: 'a' → 'A'
  Lowercase rest: "t" → "t"
  result = "IGotInternAt"

Step 6: Process "geeksforgeeks"
  Capitalize: 'g' → 'G'
  Lowercase rest: "eeksforgeeks" → "eeksforgeeks"
  result = "IGotInternAtGeeksforgeeks"

Result: "IGotInternAtGeeksforgeeks"
```

### Example: "here comes the sun" (Single Pass)

```
Input: "here comes the sun"

Initial: result = "", capitalizeNext = true

Step 1: Process 'h'
  Not space, capitalizeNext = true
  Append 'H', capitalizeNext = false
  result = "H"

Step 2: Process 'e'
  Not space, capitalizeNext = false
  Append 'e'
  result = "He"

Step 3: Process 'r'
  Not space, capitalizeNext = false
  Append 'r'
  result = "Her"

Step 4: Process 'e'
  Not space, capitalizeNext = false
  Append 'e'
  result = "Here"

Step 5: Process ' '
  Is space, capitalizeNext = true
  result = "Here"

Step 6: Process 'c'
  Not space, capitalizeNext = true
  Append 'C', capitalizeNext = false
  result = "HereC"

Step 7-9: Process 'o', 'm', 'e', 's'
  result = "HereComes"

Step 10: Process ' '
  capitalizeNext = true

Step 11-13: Process 't', 'h', 'e'
  result = "HereComesThe"

Step 14: Process ' '
  capitalizeNext = true

Step 15-17: Process 's', 'u', 'n'
  result = "HereComesTheSun"

Result: "HereComesTheSun"
```

---

## Complete Implementation

```java
import java.util.*;
import java.util.stream.*;

public class Solution {
    
    // Approach 1: Split and Capitalize (Optimal)
    public static String toCamelCase(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        String[] words = sentence.trim().split("\\s+");
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
    
    // Approach 2: Single Pass with Flag
    public static String toCamelCaseSinglePass(String sentence) {
        if (sentence == null || sentence.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : sentence.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }
        
        return result.toString();
    }
    
    // Approach 3: Stream API (Functional)
    public static String toCamelCaseStream(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) return "";
        
        return Arrays.stream(sentence.trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .map(word -> Character.toUpperCase(word.charAt(0)) + 
                         word.substring(1).toLowerCase())
            .collect(Collectors.joining());
    }
    
    // Variation: lowerCamelCase (first word lowercase)
    public static String toLowerCamelCase(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        String[] words = sentence.trim().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                if (i == 0) {
                    result.append(word.toLowerCase());
                } else {
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    // Variation: snake_case to CamelCase
    public static String snakeToCamelCase(String snakeCase) {
        return toCamelCase(snakeCase.replace('_', ' '));
    }
    
    // Variation: kebab-case to CamelCase
    public static String kebabToCamelCase(String kebabCase) {
        return toCamelCase(kebabCase.replace('-', ' '));
    }
    
    // Reverse: CamelCase to sentence
    public static String camelCaseToSentence(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append(' ');
            }
            result.append(Character.toLowerCase(c));
        }
        
        return result.toString();
    }
    
    public static boolean doTestsPass() {
        // Test 1: Basic conversion
        if (!toCamelCase("I got intern at geeksforgeeks")
            .equals("IGotInternAtGeeksforgeeks")) return false;
        
        // Test 2: Simple sentence
        if (!toCamelCase("here comes the sun")
            .equals("HereComesTheSun")) return false;
        
        // Test 3: Two words
        if (!toCamelCase("hello world")
            .equals("HelloWorld")) return false;
        
        // Test 4: Single letters
        if (!toCamelCase("a b c")
            .equals("ABC")) return false;
        
        // Test 5: Multiple spaces
        if (!toCamelCase("  multiple   spaces  ")
            .equals("MultipleSpaces")) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        String[] testCases = {
            "I got intern at geeksforgeeks",
            "here comes the sun",
            "hello world",
            "camel case conversion",
            "a b c",
            "  multiple   spaces  "
        };
        
        for (String test : testCases) {
            String result = toCamelCase(test);
            System.out.println("Input:  \"" + test + "\"");
            System.out.println("Output: \"" + result + "\"");
            System.out.println();
        }
        
        // Demo variations
        System.out.println("Variations:");
        System.out.println("lowerCamelCase: " + toLowerCamelCase("hello world"));
        System.out.println("snake_case: " + snakeToCamelCase("hello_world_test"));
        System.out.println("kebab-case: " + kebabToCamelCase("hello-world-test"));
        System.out.println("Reverse: " + camelCaseToSentence("HelloWorldTest"));
    }
}
```

---

## Test Cases

```java
@Test
public void testToCamelCase() {
    // Test 1: Basic conversion
    assertEquals("IGotInternAtGeeksforgeeks", 
        toCamelCase("I got intern at geeksforgeeks"));
    
    // Test 2: Simple sentence
    assertEquals("HereComesTheSun", 
        toCamelCase("here comes the sun"));
    
    // Test 3: Two words
    assertEquals("HelloWorld", 
        toCamelCase("hello world"));
    
    // Test 4: Three words
    assertEquals("CamelCaseConversion", 
        toCamelCase("camel case conversion"));
    
    // Test 5: Single letters
    assertEquals("ABC", 
        toCamelCase("a b c"));
    
    // Test 6: Multiple spaces
    assertEquals("MultipleSpaces", 
        toCamelCase("  multiple   spaces  "));
    
    // Test 7: Single word
    assertEquals("Hello", 
        toCamelCase("hello"));
    
    // Test 8: Empty string
    assertEquals("", 
        toCamelCase(""));
    
    // Test 9: Only spaces
    assertEquals("", 
        toCamelCase("   "));
    
    // Test 10: Mixed case input
    assertEquals("HelloWorld", 
        toCamelCase("HeLLo WoRLd"));
}
```

---

## Visual Representation

### Camel Case Transformation

```
Input: "hello world test"

┌───────────────────────────────────────┐
│ Word-by-Word Transformation           │
├──────────┬────────────┬──────────────┤
│ Original │ Capitalize │ Result       │
├──────────┼────────────┼──────────────┤
│ hello    │ H + ello   │ Hello        │
│ world    │ W + orld   │ World        │
│ test     │ T + est    │ Test         │
└──────────┴────────────┴──────────────┘

Final: "HelloWorldTest"
```

### Single Pass Flag Approach

```
Input: "hi there"

┌─────┬──────┬───────────────┬────────────┬──────────┐
│ Idx │ Char │ Is Space?     │ Capitalize │ Result   │
├─────┼──────┼───────────────┼────────────┼──────────┤
│  0  │  h   │ No            │ Yes (flag) │ H        │
│  1  │  i   │ No            │ No         │ Hi       │
│  2  │ ' '  │ Yes (set flag)│ -          │ Hi       │
│  3  │  t   │ No            │ Yes (flag) │ HiT      │
│  4  │  h   │ No            │ No         │ HiTh     │
│  5  │  e   │ No            │ No         │ HiThe    │
│  6  │  r   │ No            │ No         │ HiTher   │
│  7  │  e   │ No            │ No         │ HiThere  │
└─────┴──────┴───────────────┴────────────┴──────────┘

Final: "HiThere"
```

### Comparison: Different Case Styles

```
Original: "hello world test"

PascalCase/UpperCamelCase: HelloWorldTest
lowerCamelCase:            helloWorldTest
snake_case:                hello_world_test
kebab-case:                hello-world-test
SCREAMING_SNAKE_CASE:      HELLO_WORLD_TEST
```

---

## Edge Cases

1. **Empty string:** Return ""
2. **Null input:** Return ""
3. **Only spaces:** Return ""
4. **Single word:** Capitalize first letter
5. **Single letter words:** Handle correctly
6. **Multiple consecutive spaces:** Treat as single space
7. **Leading/trailing spaces:** Trim before processing
8. **Mixed case input:** Normalize to lowercase except first letter
9. **Special characters:** May need to handle

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Split & Capitalize | O(n) | O(n) | **Clean and readable** |
| Single Pass | O(n) | O(n) | **Most efficient** |
| Stream API | O(n) | O(n) | Functional style |

**Where n = length of string**

**Time Complexity Breakdown:**
- Split string: O(n)
- Process each word: O(n) total
- Build result: O(n)
- Total: O(n)

**Space Complexity:**
- Result StringBuilder: O(n)
- Words array: O(n)
- Total: O(n)

---

## Related Problems

1. **Snake Case Conversion** - Convert to snake_case
2. **Kebab Case Conversion** - Convert to kebab-case
3. **Title Case** - Capitalize first letter of each word with spaces
4. **Toggle Case** - Swap upper and lower case
5. **Reverse Words** - Reverse word order
6. **Remove Spaces** - Remove all spaces

---

## Interview Tips

### Clarification Questions
1. Should first letter be uppercase? (Yes, PascalCase)
2. What about lowerCamelCase? (First word lowercase)
3. Handle multiple spaces? (Yes, treat as single)
4. Preserve numbers/special chars? (Usually yes)
5. What if input is null/empty? (Return empty string)

### Approach Explanation
1. "I'll split the sentence by spaces into words"
2. "For each word, capitalize first letter and lowercase rest"
3. "Concatenate all words without spaces"
4. "Handle edge cases: empty string, multiple spaces"
5. "Time O(n), Space O(n) for result string"

### Common Mistakes
1. **Not handling multiple spaces** - Use split("\\s+")
2. **Not trimming input** - Leading/trailing spaces
3. **Case sensitivity** - Lowercase rest of word
4. **Empty words** - Check before processing
5. **Null pointer** - Check for null input

### Follow-up Questions
1. "What about lowerCamelCase?" - Keep first word lowercase
2. "Convert from CamelCase back?" - Insert space before capitals
3. "Handle snake_case input?" - Replace underscore with space
4. "Preserve acronyms?" - More complex logic needed
5. "Unicode characters?" - Use Character methods

---

## Real-World Applications

1. **Programming Conventions** - Variable/class naming
2. **API Design** - Endpoint naming conventions
3. **Database Schemas** - Table/column naming
4. **Code Generation** - Template to code conversion
5. **Text Processing** - Format standardization
6. **Configuration Files** - Key naming conventions
7. **Documentation** - Consistent naming across docs

---

## Key Takeaways

1. **Split approach is cleanest:** Easy to understand and maintain
2. **Single pass is most efficient:** No array allocation
3. **Handle multiple spaces:** Use split("\\s+") or flag
4. **Trim input:** Remove leading/trailing spaces
5. **Normalize case:** Lowercase all except first letter
6. **StringBuilder efficient:** For string concatenation
7. **Edge cases matter:** Empty, null, single word, spaces

---

## Optimization Notes

### String Concatenation vs StringBuilder
```java
// Inefficient: O(n²) due to string immutability
String result = "";
for (String word : words) {
    result += capitalize(word);  // Creates new string each time
}

// Efficient: O(n) with StringBuilder
StringBuilder result = new StringBuilder();
for (String word : words) {
    result.append(capitalize(word));  // Modifies in place
}
```

### Split vs Single Pass
```java
// Split: Creates array, more memory
String[] words = sentence.split("\\s+");

// Single Pass: No array, less memory
boolean capitalizeNext = true;
for (char c : sentence.toCharArray()) {
    // Process character by character
}
```

### Best Practice
```java
public static String toCamelCase(String sentence) {
    if (sentence == null || sentence.trim().isEmpty()) {
        return "";
    }
    
    StringBuilder result = new StringBuilder();
    String[] words = sentence.trim().split("\\s+");
    
    for (String word : words) {
        if (!word.isEmpty()) {
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }
    }
    
    return result.toString();
}
```

---

## Variations

### 1. lowerCamelCase (First Word Lowercase)
```java
public static String toLowerCamelCase(String sentence) {
    String[] words = sentence.trim().split("\\s+");
    StringBuilder result = new StringBuilder();
    
    for (int i = 0; i < words.length; i++) {
        if (i == 0) {
            result.append(words[i].toLowerCase());
        } else {
            result.append(Character.toUpperCase(words[i].charAt(0)))
                  .append(words[i].substring(1).toLowerCase());
        }
    }
    
    return result.toString();
}
```

### 2. CamelCase to Sentence
```java
public static String camelCaseToSentence(String camelCase) {
    StringBuilder result = new StringBuilder();
    
    for (int i = 0; i < camelCase.length(); i++) {
        char c = camelCase.charAt(i);
        if (Character.isUpperCase(c) && i > 0) {
            result.append(' ');
        }
        result.append(Character.toLowerCase(c));
    }
    
    return result.toString();
}
```

### 3. snake_case to CamelCase
```java
public static String snakeToCamelCase(String snakeCase) {
    return toCamelCase(snakeCase.replace('_', ' '));
}
```

### 4. Preserve Acronyms
```java
public static String toCamelCasePreserveAcronyms(String sentence) {
    StringBuilder result = new StringBuilder();
    String[] words = sentence.trim().split("\\s+");
    
    for (String word : words) {
        if (word.length() <= 2 || word.equals(word.toUpperCase())) {
            result.append(word.toUpperCase());  // Preserve acronym
        } else {
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase());
        }
    }
    
    return result.toString();
}
```
