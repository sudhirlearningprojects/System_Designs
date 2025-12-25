# Find First Repeated Word in a String

## Problem Statement

Find the first word that appears more than once in a string. Return the first word that repeats (appears for the second time).

**Input:** String with words separated by spaces  
**Output:** First repeated word, or empty string if no repetition

**Examples:**
```
Input:  "Ravi had been saying that he had been there"
Output: "had"
Explanation: "had" appears at index 1 and repeats at index 6

Input:  "He had had quite enough of this"
Output: "had"
Explanation: "had" appears consecutively at index 1 and 2

Input:  "Hello world hello universe"
Output: "hello"
Explanation: Case-insensitive, "Hello" and "hello" are same

Input:  "One two three four five"
Output: ""
Explanation: No repeated words

Input:  "The the cat sat"
Output: "the"
Explanation: "the" repeats immediately (case-insensitive)
```

---

## Solution Approaches

### Approach 1: HashSet (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String firstRepeatedWord(String str) {
    if (str == null || str.isEmpty()) return "";
    
    Set<String> seen = new HashSet<>();
    String[] words = str.toLowerCase().split("\\s+");
    
    for (String word : words) {
        if (seen.contains(word)) {
            return word;
        }
        seen.add(word);
    }
    
    return "";
}
```

---

### Approach 2: HashMap with Index Tracking

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static String firstRepeatedWordWithIndex(String str) {
    if (str == null || str.isEmpty()) return "";
    
    Map<String, Integer> wordIndex = new HashMap<>();
    String[] words = str.toLowerCase().split("\\s+");
    
    for (int i = 0; i < words.length; i++) {
        if (wordIndex.containsKey(words[i])) {
            return words[i];
        }
        wordIndex.put(words[i], i);
    }
    
    return "";
}
```

---

### Approach 3: Two Pointers (Brute Force)

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static String firstRepeatedWordBruteForce(String str) {
    if (str == null || str.isEmpty()) return "";
    
    String[] words = str.toLowerCase().split("\\s+");
    
    for (int i = 0; i < words.length; i++) {
        for (int j = i + 1; j < words.length; j++) {
            if (words[i].equals(words[j])) {
                return words[i];
            }
        }
    }
    
    return "";
}
```

---

## Algorithm Walkthrough

### Example: "Ravi had been saying that he had been there"

**Step-by-Step Execution:**

```
Input: "Ravi had been saying that he had been there"
Split: ["ravi", "had", "been", "saying", "that", "he", "had", "been", "there"]

Initial: seen = {}

Step 1: Process "ravi"
  seen.contains("ravi")? No
  seen = {ravi}

Step 2: Process "had"
  seen.contains("had")? No
  seen = {ravi, had}

Step 3: Process "been"
  seen.contains("been")? No
  seen = {ravi, had, been}

Step 4: Process "saying"
  seen.contains("saying")? No
  seen = {ravi, had, been, saying}

Step 5: Process "that"
  seen.contains("that")? No
  seen = {ravi, had, been, saying, that}

Step 6: Process "he"
  seen.contains("he")? No
  seen = {ravi, had, been, saying, that, he}

Step 7: Process "had"
  seen.contains("had")? Yes ✓
  Return "had"

Result: "had"
```

### Example: "He had had quite enough of this"

```
Input: "He had had quite enough of this"
Split: ["he", "had", "had", "quite", "enough", "of", "this"]

Initial: seen = {}

Step 1: Process "he"
  seen = {he}

Step 2: Process "had"
  seen = {he, had}

Step 3: Process "had"
  seen.contains("had")? Yes ✓
  Return "had"

Result: "had"
```

### Example: "One two three four five"

```
Input: "One two three four five"
Split: ["one", "two", "three", "four", "five"]

Process all words:
  seen = {one, two, three, four, five}
  No repetitions found

Result: ""
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: HashSet (Optimal)
    public static String firstRepeatedWord(String str) {
        if (str == null || str.isEmpty()) return "";
        
        Set<String> seen = new HashSet<>();
        String[] words = str.toLowerCase().split("\\s+");
        
        for (String word : words) {
            if (seen.contains(word)) {
                return word;
            }
            seen.add(word);
        }
        
        return "";
    }
    
    // Approach 2: HashMap with Index Tracking
    public static String firstRepeatedWordWithIndex(String str) {
        if (str == null || str.isEmpty()) return "";
        
        Map<String, Integer> wordIndex = new HashMap<>();
        String[] words = str.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            if (wordIndex.containsKey(words[i])) {
                return words[i];
            }
            wordIndex.put(words[i], i);
        }
        
        return "";
    }
    
    // Approach 3: Two Pointers (Brute Force)
    public static String firstRepeatedWordBruteForce(String str) {
        if (str == null || str.isEmpty()) return "";
        
        String[] words = str.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            for (int j = i + 1; j < words.length; j++) {
                if (words[i].equals(words[j])) {
                    return words[i];
                }
            }
        }
        
        return "";
    }
    
    // Helper: Find all repeated words with their positions
    public static Map<String, List<Integer>> findAllRepeatedWords(String str) {
        Map<String, List<Integer>> wordPositions = new HashMap<>();
        String[] words = str.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            wordPositions.computeIfAbsent(words[i], k -> new ArrayList<>()).add(i);
        }
        
        wordPositions.entrySet().removeIf(e -> e.getValue().size() < 2);
        return wordPositions;
    }
    
    // Helper: Find first repeated word (case-sensitive)
    public static String firstRepeatedWordCaseSensitive(String str) {
        if (str == null || str.isEmpty()) return "";
        
        Set<String> seen = new HashSet<>();
        String[] words = str.split("\\s+");
        
        for (String word : words) {
            if (seen.contains(word)) {
                return word;
            }
            seen.add(word);
        }
        
        return "";
    }
    
    // Helper: Count word occurrences
    public static Map<String, Integer> countWords(String str) {
        Map<String, Integer> count = new HashMap<>();
        String[] words = str.toLowerCase().split("\\s+");
        
        for (String word : words) {
            count.put(word, count.getOrDefault(word, 0) + 1);
        }
        
        return count;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Basic repetition
        String test1 = "Ravi had been saying that he had been there";
        if (!firstRepeatedWord(test1).equals("had")) return false;
        
        // Test 2: Consecutive repetition
        String test2 = "He had had quite enough of this";
        if (!firstRepeatedWord(test2).equals("had")) return false;
        
        // Test 3: Case insensitive
        String test3 = "Hello world hello universe";
        if (!firstRepeatedWord(test3).equals("hello")) return false;
        
        // Test 4: No repetition
        String test4 = "One two three four five";
        if (!firstRepeatedWord(test4).equals("")) return false;
        
        // Test 5: Immediate repetition
        String test5 = "The the cat sat";
        if (!firstRepeatedWord(test5).equals("the")) return false;
        
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
            "Ravi had been saying that he had been there",
            "He had had quite enough of this",
            "Hello world hello universe",
            "One two three four five",
            "The the cat sat"
        };
        
        for (String test : testCases) {
            String result = firstRepeatedWord(test);
            System.out.println("Input:  \"" + test + "\"");
            System.out.println("Output: \"" + result + "\"");
            
            Map<String, List<Integer>> allRepeated = findAllRepeatedWords(test);
            if (!allRepeated.isEmpty()) {
                System.out.println("All repeated words: " + allRepeated);
            }
            System.out.println();
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testFirstRepeatedWord() {
    // Test 1: Basic repetition
    assertEquals("had", 
        firstRepeatedWord("Ravi had been saying that he had been there"));
    
    // Test 2: Consecutive repetition
    assertEquals("had", 
        firstRepeatedWord("He had had quite enough of this"));
    
    // Test 3: Case insensitive
    assertEquals("hello", 
        firstRepeatedWord("Hello world hello universe"));
    
    // Test 4: No repetition
    assertEquals("", 
        firstRepeatedWord("One two three four five"));
    
    // Test 5: Immediate repetition
    assertEquals("the", 
        firstRepeatedWord("The the cat sat"));
    
    // Test 6: Multiple repetitions
    assertEquals("is", 
        firstRepeatedWord("This is a test is a good test"));
    
    // Test 7: Single word
    assertEquals("", 
        firstRepeatedWord("Hello"));
    
    // Test 8: Empty string
    assertEquals("", 
        firstRepeatedWord(""));
    
    // Test 9: All same words
    assertEquals("same", 
        firstRepeatedWord("same same same same"));
    
    // Test 10: Punctuation
    assertEquals("word", 
        firstRepeatedWord("word, another word here"));
}
```

---

## Visual Representation

### HashSet Tracking Process

```
Input: "Ravi had been saying that he had been there"

┌─────────────────────────────────────────┐
│ Word Processing with HashSet           │
├─────────┬───────────┬──────────────────┤
│ Index   │ Word      │ Seen Set         │
├─────────┼───────────┼──────────────────┤
│   0     │ ravi      │ {ravi}           │
│   1     │ had       │ {ravi, had}      │
│   2     │ been      │ {ravi, had, been}│
│   3     │ saying    │ {..., saying}    │
│   4     │ that      │ {..., that}      │
│   5     │ he        │ {..., he}        │
│   6     │ had       │ FOUND! ✓         │
└─────────┴───────────┴──────────────────┘

Result: "had" (first repetition at index 6)
```

### Comparison: Different Approaches

```
Input: "He had had quite enough"

Approach 1: HashSet
┌──────┬──────┬─────────────┐
│ Step │ Word │ Action      │
├──────┼──────┼─────────────┤
│  1   │ he   │ Add to set  │
│  2   │ had  │ Add to set  │
│  3   │ had  │ Found! ✓    │
└──────┴──────┴─────────────┘
Time: O(n), Space: O(n)

Approach 2: Brute Force
Compare each word with all following words
he vs [had, had, quite, enough]
had vs [had, quite, enough] → Match! ✓
Time: O(n²), Space: O(1)
```

### Word Frequency Visualization

```
Input: "Ravi had been saying that he had been there"

Word Frequency:
┌─────────┬───────────┐
│ Word    │ Count     │
├─────────┼───────────┤
│ ravi    │ 1         │
│ had     │ 2 ← First │
│ been    │ 2         │
│ saying  │ 1         │
│ that    │ 1         │
│ he      │ 1         │
│ there   │ 1         │
└─────────┴───────────┘

First word with count > 1: "had"
```

---

## Edge Cases

1. **Empty string:** Return ""
2. **Null input:** Return ""
3. **Single word:** Return ""
4. **No repetition:** Return ""
5. **All same words:** Return first word
6. **Consecutive repetition:** Handle correctly
7. **Case sensitivity:** Convert to lowercase
8. **Multiple spaces:** Use split("\\s+")
9. **Punctuation:** May need to clean words
10. **Special characters:** Handle appropriately

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashSet | O(n) | O(n) | **Optimal solution** |
| HashMap | O(n) | O(n) | Tracks indices |
| Brute Force | O(n²) | O(1) | No extra space |

**Where n = number of words**

**Time Complexity Breakdown:**
- Split string: O(n)
- Iterate words: O(n)
- HashSet lookup: O(1) average
- Total: O(n)

**Space Complexity:**
- HashSet: O(k) where k = unique words
- Worst case: O(n) if all words unique
- Best case: O(1) if immediate repetition

---

## Related Problems

1. **First Non-Repeating Character** - Find first unique character
2. **Find Duplicate in Array** - Similar concept with numbers
3. **Two Sum** - Use HashSet for lookup
4. **Longest Substring Without Repeating** - Sliding window
5. **Group Anagrams** - HashMap grouping
6. **Word Pattern** - Pattern matching with words

---

## Interview Tips

### Clarification Questions
1. Is comparison case-sensitive? (Usually no)
2. How to handle punctuation? (Usually ignore or clean)
3. What about empty string? (Return empty string)
4. Multiple spaces between words? (Use split("\\s+"))
5. What if no repetition? (Return empty string)

### Approach Explanation
1. "I'll use a HashSet to track seen words"
2. "Split string into words and convert to lowercase"
3. "Iterate through words, check if already seen"
4. "If seen, return immediately (first repetition)"
5. "Otherwise, add to set and continue"
6. "Time O(n), Space O(n) for HashSet"

### Common Mistakes
1. **Case sensitivity** - Forgetting to convert to lowercase
2. **Multiple spaces** - Use split("\\s+") not split(" ")
3. **Not returning first** - Must return on first match
4. **Null check** - Handle null input
5. **Empty result** - Return "" not null when no repetition

### Follow-up Questions
1. "What if case-sensitive?" - Don't convert to lowercase
2. "Find all repeated words?" - Use HashMap with counts
3. "Find last repeated word?" - Continue iteration, update result
4. "Handle punctuation?" - Use regex to clean words
5. "Stream of words?" - Process incrementally with HashSet

---

## Real-World Applications

1. **Plagiarism Detection** - Find repeated phrases
2. **Text Analysis** - Identify common words
3. **Spell Checkers** - Detect duplicate words
4. **Search Engines** - Query optimization
5. **Natural Language Processing** - Text preprocessing
6. **Code Analysis** - Find duplicate identifiers
7. **Data Deduplication** - Remove redundant entries

---

## Key Takeaways

1. **HashSet is optimal:** O(n) time with O(1) lookup
2. **Case-insensitive:** Convert to lowercase before comparison
3. **Early return:** Stop at first repetition
4. **Split carefully:** Use "\\s+" for multiple spaces
5. **Handle edge cases:** Empty, null, single word, no repetition
6. **Space-time tradeoff:** O(n) space for O(n) time
7. **Clean input:** Consider punctuation and special characters

---

## Optimization Notes

### Why HashSet Over HashMap?
```java
// HashSet: Only need to track existence
Set<String> seen = new HashSet<>();
if (seen.contains(word)) return word;

// HashMap: Overkill if only checking existence
Map<String, Integer> count = new HashMap<>();
if (count.containsKey(word)) return word;
```

### Regex for Word Splitting
```java
// Basic split (fails with multiple spaces)
String[] words = str.split(" ");

// Better split (handles multiple spaces)
String[] words = str.split("\\s+");

// With punctuation removal
String[] words = str.replaceAll("[^a-zA-Z ]", "").split("\\s+");
```

### Best Practice
```java
public static String firstRepeatedWord(String str) {
    if (str == null || str.trim().isEmpty()) return "";
    
    Set<String> seen = new HashSet<>();
    
    // Clean and split
    String[] words = str.toLowerCase()
                        .replaceAll("[^a-zA-Z ]", "")
                        .split("\\s+");
    
    for (String word : words) {
        if (!word.isEmpty() && seen.contains(word)) {
            return word;
        }
        seen.add(word);
    }
    
    return "";
}
```

---

## Variations

### 1. Find All Repeated Words
```java
public static List<String> findAllRepeated(String str) {
    Map<String, Integer> count = new HashMap<>();
    String[] words = str.toLowerCase().split("\\s+");
    
    for (String word : words) {
        count.put(word, count.getOrDefault(word, 0) + 1);
    }
    
    return count.entrySet().stream()
        .filter(e -> e.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

### 2. Find Most Repeated Word
```java
public static String mostRepeatedWord(String str) {
    Map<String, Integer> count = new HashMap<>();
    String[] words = str.toLowerCase().split("\\s+");
    
    for (String word : words) {
        count.put(word, count.getOrDefault(word, 0) + 1);
    }
    
    return count.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("");
}
```
