# Sort String in Descending Order

## Problem Statement

Given a string, sort its characters in descending order (highest to lowest based on ASCII/Unicode values).

**Input:** String  
**Output:** String sorted in descending order

**Examples:**
```
Input:  "mupursingh"
Output: "uusrpnmihg"
Explanation: u(117) > u(117) > s(115) > r(114) > p(112) > n(110) > m(109) > i(105) > h(104) > g(103)

Input:  "hello"
Output: "ollhe"
Explanation: o(111) > l(108) > l(108) > h(104) > e(101)

Input:  "programming"
Output: "rrogmmnipa"

Input:  "ABC123"
Output: "CBA321"
Explanation: C(67) > B(66) > A(65) > 3(51) > 2(50) > 1(49)

Input:  "aAbBcC"
Output: "cCbBaA"
Explanation: Lowercase > Uppercase in ASCII (c=99 > C=67)
```

---

## Solution Approaches

### Approach 1: Convert to Array and Sort (Optimal)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String sortDescending(String str) {
    if (str == null || str.isEmpty()) return str;
    
    char[] chars = str.toCharArray();
    Arrays.sort(chars);
    
    return new StringBuilder(new String(chars)).reverse().toString();
}
```

---

### Approach 2: Custom Comparator

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String sortDescendingComparator(String str) {
    if (str == null || str.isEmpty()) return str;
    
    Character[] chars = new Character[str.length()];
    for (int i = 0; i < str.length(); i++) {
        chars[i] = str.charAt(i);
    }
    
    Arrays.sort(chars, Collections.reverseOrder());
    
    StringBuilder result = new StringBuilder();
    for (char c : chars) {
        result.append(c);
    }
    
    return result.toString();
}
```

---

### Approach 3: Stream API (Functional)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String sortDescendingStream(String str) {
    if (str == null || str.isEmpty()) return str;
    
    return str.chars()
        .boxed()
        .sorted(Collections.reverseOrder())
        .map(i -> String.valueOf((char) i.intValue()))
        .collect(Collectors.joining());
}
```

---

### Approach 4: Counting Sort (For Limited Character Set)

**Time Complexity:** O(n + k) where k = 256 (ASCII)  
**Space Complexity:** O(k)

```java
public static String sortDescendingCounting(String str) {
    if (str == null || str.isEmpty()) return str;
    
    int[] count = new int[256];
    
    for (char c : str.toCharArray()) {
        count[c]++;
    }
    
    StringBuilder result = new StringBuilder();
    for (int i = 255; i >= 0; i--) {
        while (count[i]-- > 0) {
            result.append((char) i);
        }
    }
    
    return result.toString();
}
```

---

## Algorithm Walkthrough

### Example: "mupursingh"

**Step-by-Step Execution (Array Sort + Reverse):**

```
Input: "mupursingh"

Step 1: Convert to char array
  chars = ['m', 'u', 'p', 'u', 'r', 's', 'i', 'n', 'g', 'h']

Step 2: Sort array (ascending)
  chars = ['g', 'h', 'i', 'm', 'n', 'p', 'r', 's', 'u', 'u']
  
  ASCII values:
  g=103, h=104, i=105, m=109, n=110, p=112, r=114, s=115, u=117, u=117

Step 3: Reverse to get descending
  chars = ['u', 'u', 's', 'r', 'p', 'n', 'm', 'i', 'h', 'g']

Step 4: Convert to string
  result = "uusrpnmihg"

Result: "uusrpnmihg"
```

### Example: "hello" (Counting Sort)

```
Input: "hello"

Step 1: Count frequency
  count['e'] = 1  (ASCII 101)
  count['h'] = 1  (ASCII 104)
  count['l'] = 2  (ASCII 108)
  count['o'] = 1  (ASCII 111)

Step 2: Build result from highest to lowest
  i=255 down to 0:
    i=111 (o): count=1 → append 'o'
    i=108 (l): count=2 → append 'll'
    i=104 (h): count=1 → append 'h'
    i=101 (e): count=1 → append 'e'

Result: "ollhe"
```

### Example: "ABC123"

```
Input: "ABC123"

ASCII values:
  '1'=49, '2'=50, '3'=51
  'A'=65, 'B'=66, 'C'=67

Step 1: Sort ascending
  ['1', '2', '3', 'A', 'B', 'C']

Step 2: Reverse
  ['C', 'B', 'A', '3', '2', '1']

Result: "CBA321"
```

---

## Complete Implementation

```java
import java.util.*;
import java.util.stream.*;

public class Solution {
    
    // Approach 1: Convert to Array and Sort (Optimal)
    public static String sortDescending(String str) {
        if (str == null || str.isEmpty()) return str;
        
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        
        return new StringBuilder(new String(chars)).reverse().toString();
    }
    
    // Approach 2: Custom Comparator
    public static String sortDescendingComparator(String str) {
        if (str == null || str.isEmpty()) return str;
        
        Character[] chars = new Character[str.length()];
        for (int i = 0; i < str.length(); i++) {
            chars[i] = str.charAt(i);
        }
        
        Arrays.sort(chars, Collections.reverseOrder());
        
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            result.append(c);
        }
        
        return result.toString();
    }
    
    // Approach 3: Stream API (Functional)
    public static String sortDescendingStream(String str) {
        if (str == null || str.isEmpty()) return str;
        
        return str.chars()
            .boxed()
            .sorted(Collections.reverseOrder())
            .map(i -> String.valueOf((char) i.intValue()))
            .collect(Collectors.joining());
    }
    
    // Approach 4: Counting Sort (For Limited Character Set)
    public static String sortDescendingCounting(String str) {
        if (str == null || str.isEmpty()) return str;
        
        int[] count = new int[256];
        
        for (char c : str.toCharArray()) {
            count[c]++;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 255; i >= 0; i--) {
            while (count[i]-- > 0) {
                result.append((char) i);
            }
        }
        
        return result.toString();
    }
    
    // Variation: Sort ascending
    public static String sortAscending(String str) {
        if (str == null || str.isEmpty()) return str;
        
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        
        return new String(chars);
    }
    
    // Variation: Sort case-insensitive
    public static String sortDescendingCaseInsensitive(String str) {
        if (str == null || str.isEmpty()) return str;
        
        Character[] chars = new Character[str.length()];
        for (int i = 0; i < str.length(); i++) {
            chars[i] = str.charAt(i);
        }
        
        Arrays.sort(chars, (a, b) -> 
            Character.compare(Character.toLowerCase(b), Character.toLowerCase(a)));
        
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            result.append(c);
        }
        
        return result.toString();
    }
    
    // Helper: Get character frequency
    public static Map<Character, Integer> getCharFrequency(String str) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : str.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        return freq;
    }
    
    // Helper: Sort by frequency (descending)
    public static String sortByFrequency(String str) {
        Map<Character, Integer> freq = getCharFrequency(str);
        
        return str.chars()
            .mapToObj(c -> (char) c)
            .sorted((a, b) -> {
                int freqCompare = freq.get(b).compareTo(freq.get(a));
                return freqCompare != 0 ? freqCompare : Character.compare(b, a);
            })
            .map(String::valueOf)
            .collect(Collectors.joining());
    }
    
    public static boolean doTestsPass() {
        // Test 1: Basic string
        if (!sortDescending("mupursingh").equals("uusrpnmihg")) return false;
        
        // Test 2: Simple string
        if (!sortDescending("hello").equals("ollhe")) return false;
        
        // Test 3: Mixed case and digits
        if (!sortDescending("ABC123").equals("CBA321")) return false;
        
        // Test 4: Empty string
        if (!sortDescending("").equals("")) return false;
        
        // Test 5: Single character
        if (!sortDescending("a").equals("a")) return false;
        
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
            "mupursingh",
            "hello",
            "programming",
            "ABC123",
            "aAbBcC"
        };
        
        for (String test : testCases) {
            String result = sortDescending(test);
            System.out.println("Input:  \"" + test + "\"");
            System.out.println("Output: \"" + result + "\"");
            System.out.println();
        }
        
        // Compare approaches
        String test = "mupursingh";
        System.out.println("Comparing approaches for: \"" + test + "\"");
        System.out.println("Array Sort:    " + sortDescending(test));
        System.out.println("Comparator:    " + sortDescendingComparator(test));
        System.out.println("Stream API:    " + sortDescendingStream(test));
        System.out.println("Counting Sort: " + sortDescendingCounting(test));
    }
}
```

---

## Test Cases

```java
@Test
public void testSortDescending() {
    // Test 1: Basic string
    assertEquals("uusrpnmihg", sortDescending("mupursingh"));
    
    // Test 2: Simple string
    assertEquals("ollhe", sortDescending("hello"));
    
    // Test 3: Programming
    assertEquals("rrogmmnipa", sortDescending("programming"));
    
    // Test 4: Mixed case and digits
    assertEquals("CBA321", sortDescending("ABC123"));
    
    // Test 5: Mixed case
    assertEquals("cCbBaA", sortDescending("aAbBcC"));
    
    // Test 6: All same characters
    assertEquals("aaaa", sortDescending("aaaa"));
    
    // Test 7: Already sorted descending
    assertEquals("dcba", sortDescending("dcba"));
    
    // Test 8: Already sorted ascending
    assertEquals("dcba", sortDescending("abcd"));
    
    // Test 9: Empty string
    assertEquals("", sortDescending(""));
    
    // Test 10: Single character
    assertEquals("a", sortDescending("a"));
    
    // Test 11: Special characters
    assertEquals("zyx!@#", sortDescending("xyz!@#"));
}
```

---

## Visual Representation

### Sorting Process

```
Input: "hello"

Step 1: Convert to array
┌───┬───┬───┬───┬───┐
│ h │ e │ l │ l │ o │
└───┴───┴───┴───┴───┘

Step 2: Sort ascending
┌───┬───┬───┬───┬───┐
│ e │ h │ l │ l │ o │
└───┴───┴───┴───┴───┘
 101 104 108 108 111

Step 3: Reverse
┌───┬───┬───┬───┬───┐
│ o │ l │ l │ h │ e │
└───┴───┴───┴───┴───┘
 111 108 108 104 101

Result: "ollhe"
```

### ASCII Value Comparison

```
Character ASCII Values:

Digits:    '0'=48 ... '9'=57
Uppercase: 'A'=65 ... 'Z'=90
Lowercase: 'a'=97 ... 'z'=122

Example: "aAbBcC"
┌──────┬───────┬────────────┐
│ Char │ ASCII │ Order      │
├──────┼───────┼────────────┤
│  c   │  99   │ 1st (max)  │
│  C   │  67   │ 2nd        │
│  b   │  98   │ 3rd        │
│  B   │  66   │ 4th        │
│  a   │  97   │ 5th        │
│  A   │  65   │ 6th (min)  │
└──────┴───────┴────────────┘

Result: "cCbBaA"
```

### Counting Sort Visualization

```
Input: "hello"

Frequency Count:
┌─────┬─────┬─────┬─────┬─────┐
│ 101 │ 104 │ 108 │ 111 │     │
│  e  │  h  │  l  │  o  │     │
│  1  │  1  │  2  │  1  │     │
└─────┴─────┴─────┴─────┴─────┘

Build from highest (255) to lowest (0):
  111 (o): 1 time  → "o"
  108 (l): 2 times → "oll"
  104 (h): 1 time  → "ollh"
  101 (e): 1 time  → "ollhe"

Result: "ollhe"
```

---

## Edge Cases

1. **Empty string:** Return empty string
2. **Null input:** Return null or empty
3. **Single character:** Return same character
4. **All same characters:** Return same string
5. **Already sorted descending:** Return same string
6. **Already sorted ascending:** Reverse it
7. **Mixed case:** Lowercase > Uppercase in ASCII
8. **Special characters:** Sort by ASCII value
9. **Unicode characters:** May need special handling
10. **Very long string:** Memory considerations

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Array Sort + Reverse | O(n log n) | O(n) | **Most practical** |
| Custom Comparator | O(n log n) | O(n) | Boxing overhead |
| Stream API | O(n log n) | O(n) | Functional style |
| Counting Sort | O(n + k) | O(k) | **Fastest for ASCII** |

**Where:**
- n = length of string
- k = character set size (256 for ASCII, 65536 for Unicode)

**Time Complexity Breakdown:**
- Array Sort: O(n log n) using dual-pivot quicksort
- Reverse: O(n)
- Total: O(n log n)

**Space Complexity:**
- Char array: O(n)
- Result string: O(n)
- Total: O(n)

---

## Related Problems

1. **Sort String Ascending** - Sort in ascending order
2. **Sort by Frequency** - Sort by character frequency
3. **Custom Sort String** - Sort based on custom order
4. **Sort Characters by Case** - Uppercase first or lowercase first
5. **Anagram Sorting** - Group anagrams together
6. **Lexicographic Sorting** - Dictionary order

---

## Interview Tips

### Clarification Questions
1. Should sorting be case-sensitive? (Yes, by ASCII)
2. How to handle special characters? (Include in sort)
3. What about Unicode characters? (Usually ASCII only)
4. Empty string handling? (Return empty)
5. In-place sorting required? (Usually no)

### Approach Explanation
1. "I'll convert string to character array"
2. "Sort the array in ascending order using Arrays.sort()"
3. "Reverse the sorted array to get descending order"
4. "Convert back to string and return"
5. "Time O(n log n), Space O(n) for array"

### Common Mistakes
1. **Forgetting to reverse** - Arrays.sort() is ascending
2. **Not handling empty string** - Check for null/empty
3. **Case sensitivity** - Lowercase > Uppercase in ASCII
4. **Immutability** - Strings are immutable, need new string
5. **Boxing overhead** - Using Character[] instead of char[]

### Follow-up Questions
1. "Can you do it in O(n) time?" - Yes, counting sort for limited charset
2. "Sort case-insensitive?" - Use custom comparator
3. "Sort by frequency?" - Count frequency, then sort
4. "Stable sort needed?" - Arrays.sort() is stable for objects
5. "Very large strings?" - Consider external sorting

---

## Real-World Applications

1. **Text Processing** - Normalize text data
2. **Data Analysis** - Sort characters for analysis
3. **Anagram Detection** - Sort to check anagrams
4. **Password Strength** - Analyze character distribution
5. **Compression** - Prepare data for compression
6. **Cryptography** - Character frequency analysis
7. **Search Optimization** - Preprocess search terms

---

## Key Takeaways

1. **Arrays.sort() + reverse is simplest:** O(n log n) time
2. **Counting sort is fastest:** O(n) for limited character set
3. **Strings are immutable:** Need to create new string
4. **ASCII ordering:** Lowercase > Uppercase > Digits
5. **char[] more efficient:** Than Character[] (no boxing)
6. **StringBuilder for building:** More efficient than concatenation
7. **Edge cases matter:** Empty, null, single character

---

## Optimization Notes

### char[] vs Character[]
```java
// Efficient: Primitive array (no boxing)
char[] chars = str.toCharArray();
Arrays.sort(chars);

// Less efficient: Object array (boxing overhead)
Character[] chars = new Character[str.length()];
Arrays.sort(chars, Collections.reverseOrder());
```

### Counting Sort for ASCII
```java
// O(n + 256) = O(n) for ASCII
int[] count = new int[256];
for (char c : str.toCharArray()) {
    count[c]++;
}
// Build result from count array
```

### Best Practice
```java
public static String sortDescending(String str) {
    if (str == null || str.isEmpty()) {
        return str;
    }
    
    // For ASCII strings, counting sort is fastest
    if (str.length() > 100) {
        return sortDescendingCounting(str);
    }
    
    // For short strings, simple sort + reverse
    char[] chars = str.toCharArray();
    Arrays.sort(chars);
    return new StringBuilder(new String(chars)).reverse().toString();
}
```

---

## Variations

### 1. Sort Ascending
```java
public static String sortAscending(String str) {
    char[] chars = str.toCharArray();
    Arrays.sort(chars);
    return new String(chars);
}
```

### 2. Sort Case-Insensitive
```java
public static String sortCaseInsensitive(String str) {
    return str.chars()
        .boxed()
        .sorted((a, b) -> Character.compare(
            Character.toLowerCase(a), 
            Character.toLowerCase(b)))
        .map(i -> String.valueOf((char) i.intValue()))
        .collect(Collectors.joining());
}
```

### 3. Sort by Frequency
```java
public static String sortByFrequency(String str) {
    Map<Character, Long> freq = str.chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
    
    return str.chars()
        .mapToObj(c -> (char) c)
        .sorted((a, b) -> {
            int freqCompare = freq.get(b).compareTo(freq.get(a));
            return freqCompare != 0 ? freqCompare : Character.compare(b, a);
        })
        .map(String::valueOf)
        .collect(Collectors.joining());
}
```

### 4. Custom Order Sort
```java
public static String sortCustomOrder(String str, String order) {
    Map<Character, Integer> orderMap = new HashMap<>();
    for (int i = 0; i < order.length(); i++) {
        orderMap.put(order.charAt(i), i);
    }
    
    return str.chars()
        .mapToObj(c -> (char) c)
        .sorted((a, b) -> orderMap.getOrDefault(a, 999)
                        - orderMap.getOrDefault(b, 999))
        .map(String::valueOf)
        .collect(Collectors.joining());
}
```
