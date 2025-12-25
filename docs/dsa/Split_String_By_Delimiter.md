# Split String by Delimiter

## Problem Statement

Split a string based on a delimiter character and return a list of resulting substrings. The delimiter should not appear in the output substrings.

**Input:** String and delimiter character  
**Output:** List of substrings

**Examples:**
```
Input:  str = "apple,banana,cherry", delimiter = ','
Output: ["apple", "banana", "cherry"]

Input:  str = "one|two|three|four", delimiter = '|'
Output: ["one", "two", "three", "four"]

Input:  str = "hello", delimiter = ','
Output: ["hello"]
Explanation: No delimiter found, return entire string

Input:  str = "a,b,c,d,e", delimiter = ','
Output: ["a", "b", "c", "d", "e"]

Input:  str = "start,,end", delimiter = ','
Output: ["start", "", "end"]
Explanation: Empty string between consecutive delimiters

Input:  str = ",middle,", delimiter = ','
Output: ["", "middle", ""]
Explanation: Empty strings at start and end
```

---

## Solution Approaches

### Approach 1: Manual Iteration (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static List<String> splitString(String str, char delimiter) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    
    for (char c : str.toCharArray()) {
        if (c == delimiter) {
            result.add(current.toString());
            current = new StringBuilder();
        } else {
            current.append(c);
        }
    }
    
    result.add(current.toString());
    return result;
}
```

---

### Approach 2: Two Pointers

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static List<String> splitStringTwoPointers(String str, char delimiter) {
    List<String> result = new ArrayList<>();
    int start = 0;
    
    for (int i = 0; i < str.length(); i++) {
        if (str.charAt(i) == delimiter) {
            result.add(str.substring(start, i));
            start = i + 1;
        }
    }
    
    result.add(str.substring(start));
    return result;
}
```

---

### Approach 3: Built-in Split (Java)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static List<String> splitStringBuiltIn(String str, char delimiter) {
    return Arrays.asList(str.split(Pattern.quote(String.valueOf(delimiter))));
}
```

---

### Approach 4: Recursive

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static List<String> splitStringRecursive(String str, char delimiter) {
    List<String> result = new ArrayList<>();
    splitHelper(str, delimiter, 0, new StringBuilder(), result);
    return result;
}

private static void splitHelper(String str, char delimiter, int index, 
                                StringBuilder current, List<String> result) {
    if (index == str.length()) {
        result.add(current.toString());
        return;
    }
    
    if (str.charAt(index) == delimiter) {
        result.add(current.toString());
        splitHelper(str, delimiter, index + 1, new StringBuilder(), result);
    } else {
        current.append(str.charAt(index));
        splitHelper(str, delimiter, index + 1, current, result);
    }
}
```

---

## Algorithm Walkthrough

### Example: "apple,banana,cherry" with delimiter ','

**Step-by-Step Execution:**

```
Input: str = "apple,banana,cherry", delimiter = ','

Initial: result = [], current = ""

Step 1: Process 'a'
  Not delimiter → current = "a"

Step 2: Process 'p'
  Not delimiter → current = "ap"

Step 3: Process 'p'
  Not delimiter → current = "app"

Step 4: Process 'l'
  Not delimiter → current = "appl"

Step 5: Process 'e'
  Not delimiter → current = "apple"

Step 6: Process ','
  Is delimiter → result = ["apple"], current = ""

Step 7: Process 'b'
  Not delimiter → current = "b"

Step 8: Process 'a'
  Not delimiter → current = "ba"

Step 9: Process 'n'
  Not delimiter → current = "ban"

Step 10: Process 'a'
  Not delimiter → current = "bana"

Step 11: Process 'n'
  Not delimiter → current = "banan"

Step 12: Process 'a'
  Not delimiter → current = "banana"

Step 13: Process ','
  Is delimiter → result = ["apple", "banana"], current = ""

Step 14: Process 'c'
  Not delimiter → current = "c"

Step 15: Process 'h'
  Not delimiter → current = "ch"

Step 16: Process 'e'
  Not delimiter → current = "che"

Step 17: Process 'r'
  Not delimiter → current = "cher"

Step 18: Process 'r'
  Not delimiter → current = "cherr"

Step 19: Process 'y'
  Not delimiter → current = "cherry"

Step 20: End of string
  Add current → result = ["apple", "banana", "cherry"]

Result: ["apple", "banana", "cherry"]
```

### Example: "start,,end" with delimiter ','

```
Input: str = "start,,end", delimiter = ','

Step 1-5: Process "start"
  current = "start"

Step 6: Process ','
  result = ["start"], current = ""

Step 7: Process ','
  result = ["start", ""], current = ""

Step 8-10: Process "end"
  current = "end"

End: result = ["start", "", "end"]

Result: ["start", "", "end"]
```

---

## Complete Implementation

```java
import java.util.*;
import java.util.regex.Pattern;

public class Solution {
    
    // Approach 1: Manual Iteration (Optimal)
    public static List<String> splitString(String str, char delimiter) {
        if (str == null) return new ArrayList<>();
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (char c : str.toCharArray()) {
            if (c == delimiter) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        result.add(current.toString());
        return result;
    }
    
    // Approach 2: Two Pointers
    public static List<String> splitStringTwoPointers(String str, char delimiter) {
        if (str == null) return new ArrayList<>();
        
        List<String> result = new ArrayList<>();
        int start = 0;
        
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == delimiter) {
                result.add(str.substring(start, i));
                start = i + 1;
            }
        }
        
        result.add(str.substring(start));
        return result;
    }
    
    // Approach 3: Built-in Split
    public static List<String> splitStringBuiltIn(String str, char delimiter) {
        if (str == null) return new ArrayList<>();
        return Arrays.asList(str.split(Pattern.quote(String.valueOf(delimiter)), -1));
    }
    
    // Approach 4: Recursive
    public static List<String> splitStringRecursive(String str, char delimiter) {
        if (str == null) return new ArrayList<>();
        
        List<String> result = new ArrayList<>();
        splitHelper(str, delimiter, 0, new StringBuilder(), result);
        return result;
    }
    
    private static void splitHelper(String str, char delimiter, int index, 
                                    StringBuilder current, List<String> result) {
        if (index == str.length()) {
            result.add(current.toString());
            return;
        }
        
        if (str.charAt(index) == delimiter) {
            result.add(current.toString());
            splitHelper(str, delimiter, index + 1, new StringBuilder(), result);
        } else {
            current.append(str.charAt(index));
            splitHelper(str, delimiter, index + 1, current, result);
        }
    }
    
    // Helper: Split and remove empty strings
    public static List<String> splitStringNoEmpty(String str, char delimiter) {
        List<String> result = splitString(str, delimiter);
        result.removeIf(String::isEmpty);
        return result;
    }
    
    // Helper: Split with limit
    public static List<String> splitStringWithLimit(String str, char delimiter, int limit) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int count = 0;
        
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == delimiter && count < limit - 1) {
                result.add(current.toString());
                current = new StringBuilder();
                count++;
            } else {
                current.append(str.charAt(i));
            }
        }
        
        result.add(current.toString());
        return result;
    }
    
    // Helper: Split by multiple delimiters
    public static List<String> splitByMultipleDelimiters(String str, char[] delimiters) {
        Set<Character> delimSet = new HashSet<>();
        for (char d : delimiters) delimSet.add(d);
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (char c : str.toCharArray()) {
            if (delimSet.contains(c)) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Basic split
        List<String> result1 = splitString("apple,banana,cherry", ',');
        if (!result1.equals(Arrays.asList("apple", "banana", "cherry"))) return false;
        
        // Test 2: Different delimiter
        List<String> result2 = splitString("one|two|three", '|');
        if (!result2.equals(Arrays.asList("one", "two", "three"))) return false;
        
        // Test 3: No delimiter
        List<String> result3 = splitString("hello", ',');
        if (!result3.equals(Arrays.asList("hello"))) return false;
        
        // Test 4: Empty strings
        List<String> result4 = splitString("start,,end", ',');
        if (!result4.equals(Arrays.asList("start", "", "end"))) return false;
        
        // Test 5: Delimiter at edges
        List<String> result5 = splitString(",middle,", ',');
        if (!result5.equals(Arrays.asList("", "middle", ""))) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        String[][] testCases = {
            {"apple,banana,cherry", ","},
            {"one|two|three|four", "|"},
            {"hello", ","},
            {"a,b,c,d,e", ","},
            {"start,,end", ","},
            {",middle,", ","}
        };
        
        for (String[] test : testCases) {
            String str = test[0];
            char delimiter = test[1].charAt(0);
            List<String> result = splitString(str, delimiter);
            
            System.out.println("Input:  \"" + str + "\" with delimiter '" + delimiter + "'");
            System.out.println("Output: " + result);
            System.out.println();
        }
        
        // Demo: Multiple delimiters
        System.out.println("Multiple delimiters:");
        String str = "apple,banana;cherry|date";
        char[] delimiters = {',', ';', '|'};
        List<String> result = splitByMultipleDelimiters(str, delimiters);
        System.out.println("Input:  \"" + str + "\"");
        System.out.println("Output: " + result);
    }
}
```

---

## Test Cases

```java
@Test
public void testSplitString() {
    // Test 1: Basic split
    assertEquals(Arrays.asList("apple", "banana", "cherry"), 
        splitString("apple,banana,cherry", ','));
    
    // Test 2: Different delimiter
    assertEquals(Arrays.asList("one", "two", "three", "four"), 
        splitString("one|two|three|four", '|'));
    
    // Test 3: No delimiter
    assertEquals(Arrays.asList("hello"), 
        splitString("hello", ','));
    
    // Test 4: Single character
    assertEquals(Arrays.asList("a", "b", "c", "d", "e"), 
        splitString("a,b,c,d,e", ','));
    
    // Test 5: Empty strings between delimiters
    assertEquals(Arrays.asList("start", "", "end"), 
        splitString("start,,end", ','));
    
    // Test 6: Delimiter at start and end
    assertEquals(Arrays.asList("", "middle", ""), 
        splitString(",middle,", ','));
    
    // Test 7: Only delimiters
    assertEquals(Arrays.asList("", "", ""), 
        splitString(",,", ','));
    
    // Test 8: Empty string
    assertEquals(Arrays.asList(""), 
        splitString("", ','));
    
    // Test 9: Single delimiter
    assertEquals(Arrays.asList("", ""), 
        splitString(",", ','));
    
    // Test 10: Space delimiter
    assertEquals(Arrays.asList("hello", "world", "test"), 
        splitString("hello world test", ' '));
}
```

---

## Visual Representation

### Split Process Visualization

```
Input: "apple,banana,cherry" with delimiter ','

┌─────────────────────────────────────────┐
│ Character-by-Character Processing       │
├─────┬──────┬──────────────┬────────────┤
│ Idx │ Char │ Action       │ Result     │
├─────┼──────┼──────────────┼────────────┤
│ 0-4 │apple │ Build        │ current="apple" │
│  5  │  ,   │ Split        │ ["apple"]  │
│ 6-11│banana│ Build        │ current="banana"│
│ 12  │  ,   │ Split        │ ["apple","banana"]│
│13-18│cherry│ Build        │ current="cherry"│
│ END │      │ Add current  │ ["apple","banana","cherry"]│
└─────┴──────┴──────────────┴────────────┘
```

### Two Pointers Approach

```
Input: "one|two|three"

start = 0
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ o │ n │ e │ | │ t │ w │ o │ | │ t │ h │ r │ e │ e │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
  0   1   2   3   4   5   6   7   8   9  10  11  12
  ↑           ↑
start         i (delimiter found)

Extract: substring(0, 3) = "one"
start = 4

  ↑           ↑           ↑
  0          start=4      i=7 (delimiter found)

Extract: substring(4, 7) = "two"
start = 8

                          ↑
                       start=8

Extract: substring(8, 13) = "three"
```

### Empty String Handling

```
Input: "start,,end"

┌───────┬───┬───┬─────┐
│ start │ , │ , │ end │
└───────┴───┴───┴─────┘
    ↓     ↓   ↓    ↓
["start"][""]["end"]

Result: ["start", "", "end"]
```

---

## Edge Cases

1. **Empty string:** Return [""]
2. **Null input:** Return empty list or handle
3. **No delimiter:** Return [original string]
4. **Only delimiters:** Return empty strings
5. **Consecutive delimiters:** Create empty strings
6. **Delimiter at start:** First element is empty
7. **Delimiter at end:** Last element is empty
8. **Single character:** Handle correctly
9. **Special characters:** Escape if needed

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Manual Iteration | O(n) | O(n) | **Most efficient** |
| Two Pointers | O(n) | O(n) | Clean, uses substring |
| Built-in Split | O(n) | O(n) | Simplest code |
| Recursive | O(n) | O(n) | Stack overhead |

**Where n = length of string**

**Time Complexity Breakdown:**
- Iterate through string: O(n)
- Build substrings: O(n) total
- Total: O(n)

**Space Complexity:**
- Result list: O(n) worst case
- StringBuilder/temp: O(n)
- Total: O(n)

---

## Related Problems

1. **Split by Multiple Delimiters** - Handle multiple delimiter characters
2. **Split with Limit** - Limit number of splits
3. **Tokenize String** - Similar to split
4. **Parse CSV** - Handle quoted strings
5. **String to Array** - Convert string to character array
6. **Join Strings** - Reverse operation

---

## Interview Tips

### Clarification Questions
1. Should empty strings be included? (Usually yes)
2. What if delimiter not found? (Return original string)
3. Handle null input? (Return empty list)
4. Case-sensitive delimiter? (Usually yes)
5. Multiple consecutive delimiters? (Create empty strings)

### Approach Explanation
1. "I'll iterate through the string character by character"
2. "Build current substring until delimiter is found"
3. "When delimiter found, add current to result and reset"
4. "After loop, add remaining substring"
5. "Time O(n), Space O(n) for result list"

### Common Mistakes
1. **Forgetting last substring** - Must add after loop
2. **Not handling empty strings** - Between consecutive delimiters
3. **Off-by-one errors** - Substring indices
4. **Null pointer** - Check for null input
5. **Using split() incorrectly** - May remove trailing empty strings

### Follow-up Questions
1. "What about multiple delimiters?" - Use Set to check
2. "Split with limit?" - Stop after n-1 splits
3. "Remove empty strings?" - Filter result list
4. "Handle escape characters?" - Check for backslash before delimiter
5. "Memory optimization?" - Use iterator instead of list

---

## Real-World Applications

1. **CSV Parsing** - Parse comma-separated values
2. **Log Processing** - Split log entries by delimiter
3. **URL Parsing** - Split by '/' or '?'
4. **Command Line Arguments** - Split by space
5. **Configuration Files** - Parse key=value pairs
6. **Data Import/Export** - Process delimited files
7. **Text Processing** - Tokenization for NLP

---

## Key Takeaways

1. **Manual iteration is optimal:** O(n) time with full control
2. **Handle empty strings:** Between consecutive delimiters
3. **Don't forget last substring:** Add after loop ends
4. **StringBuilder efficient:** For building substrings
5. **Two pointers clean:** Uses substring() method
6. **Built-in split() simplest:** But less control
7. **Edge cases matter:** Empty string, no delimiter, null input

---

## Optimization Notes

### StringBuilder vs String Concatenation
```java
// Inefficient: O(n²) due to string immutability
String current = "";
for (char c : str.toCharArray()) {
    current += c;  // Creates new string each time
}

// Efficient: O(n) with StringBuilder
StringBuilder current = new StringBuilder();
for (char c : str.toCharArray()) {
    current.append(c);  // Modifies in place
}
```

### Substring vs StringBuilder
```java
// Two Pointers: Uses substring (creates new string)
result.add(str.substring(start, i));

// StringBuilder: Builds incrementally
result.add(current.toString());
```

### Best Practice
```java
public static List<String> splitString(String str, char delimiter) {
    if (str == null) return new ArrayList<>();
    if (str.isEmpty()) return Arrays.asList("");
    
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    
    for (char c : str.toCharArray()) {
        if (c == delimiter) {
            result.add(current.toString());
            current.setLength(0);  // Clear StringBuilder
        } else {
            current.append(c);
        }
    }
    
    result.add(current.toString());
    return result;
}
```

---

## Variations

### 1. Split Without Empty Strings
```java
public static List<String> splitNoEmpty(String str, char delimiter) {
    List<String> result = splitString(str, delimiter);
    result.removeIf(String::isEmpty);
    return result;
}
```

### 2. Split with Limit
```java
public static List<String> splitWithLimit(String str, char delimiter, int limit) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int count = 0;
    
    for (int i = 0; i < str.length(); i++) {
        if (str.charAt(i) == delimiter && count < limit - 1) {
            result.add(current.toString());
            current = new StringBuilder();
            count++;
        } else {
            current.append(str.charAt(i));
        }
    }
    
    result.add(current.toString());
    return result;
}
```

### 3. Split by Multiple Delimiters
```java
public static List<String> splitMultiple(String str, String delimiters) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    
    for (char c : str.toCharArray()) {
        if (delimiters.indexOf(c) >= 0) {
            if (current.length() > 0) {
                result.add(current.toString());
                current = new StringBuilder();
            }
        } else {
            current.append(c);
        }
    }
    
    if (current.length() > 0) {
        result.add(current.toString());
    }
    
    return result;
}
```
