# Longest Word from Letters

## Problem Statement

Given a string of letters and a dictionary, find the longest word(s) in the dictionary that can be formed using the available letters.

**Rules:**
- Each letter can be used only once
- Return all words with maximum length
- Case sensitive

**Input:** String of letters, Dictionary  
**Output:** Set of longest words

**Examples:**
```
letters = "oet"
dictionary = {"to", "toe", "toes", "doe"}
Output: {"toe"}

letters = "dog"
dictionary = {"to", "dog", "god"}
Output: {"dog", "god"}  // Both length 3
```

---

## Solution Approaches

### Approach 1: Character Frequency Matching (Optimal)

**Time Complexity:** O(n × m) where n = dictionary size, m = avg word length  
**Space Complexity:** O(k) where k = unique characters

```java
public static Set<String> longestWord(String letters, Dictionary dict) {
    Set<String> result = new HashSet<>();
    int maxLen = 0;
    
    // Count available letters
    Map<Character, Integer> available = new HashMap<>();
    for (char c : letters.toCharArray()) {
        available.put(c, available.getOrDefault(c, 0) + 1);
    }
    
    // Check all dictionary words
    for (String word : dict.entries) {
        if (canForm(word, available)) {
            if (word.length() > maxLen) {
                result.clear();
                result.add(word);
                maxLen = word.length();
            } else if (word.length() == maxLen) {
                result.add(word);
            }
        }
    }
    
    return result;
}

private static boolean canForm(String word, Map<Character, Integer> available) {
    Map<Character, Integer> needed = new HashMap<>();
    for (char c : word.toCharArray()) {
        needed.put(c, needed.getOrDefault(c, 0) + 1);
    }
    
    for (Map.Entry<Character, Integer> entry : needed.entrySet()) {
        if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
            return false;
        }
    }
    
    return true;
}
```

---

### Approach 2: Generate Permutations (Less Efficient)

**Time Complexity:** O(n!) for permutations  
**Space Complexity:** O(n!)

```java
public static Set<String> longestWordPermutations(String letters, Dictionary dict) {
    Set<String> result = new HashSet<>();
    int maxLen = 0;
    
    Set<String> permutations = generateAllSubstrings(letters);
    
    for (String perm : permutations) {
        if (dict.contains(perm)) {
            if (perm.length() > maxLen) {
                result.clear();
                result.add(perm);
                maxLen = perm.length();
            } else if (perm.length() == maxLen) {
                result.add(perm);
            }
        }
    }
    
    return result;
}

private static Set<String> generateAllSubstrings(String str) {
    Set<String> result = new HashSet<>();
    generateHelper("", str, result);
    return result;
}

private static void generateHelper(String prefix, String remaining, Set<String> result) {
    if (!prefix.isEmpty()) {
        result.add(prefix);
    }
    
    for (int i = 0; i < remaining.length(); i++) {
        generateHelper(
            prefix + remaining.charAt(i),
            remaining.substring(0, i) + remaining.substring(i + 1),
            result
        );
    }
}
```

---

## Algorithm Walkthrough

### Example: letters = "oet", dictionary = {"to", "toe", "toes", "doe"}

```
Step 1: Count available letters
  available = {o: 1, e: 1, t: 1}

Step 2: Check each dictionary word

  Word "to":
    needed = {t: 1, o: 1}
    Can form? t:1≤1 ✓, o:1≤1 ✓
    Length = 2
    result = {"to"}, maxLen = 2

  Word "toe":
    needed = {t: 1, o: 1, e: 1}
    Can form? t:1≤1 ✓, o:1≤1 ✓, e:1≤1 ✓
    Length = 3 > maxLen
    result = {"toe"}, maxLen = 3

  Word "toes":
    needed = {t: 1, o: 1, e: 1, s: 1}
    Can form? s:1≤0 ✗
    Skip

  Word "doe":
    needed = {d: 1, o: 1, e: 1}
    Can form? d:1≤0 ✗
    Skip

Result: {"toe"}
```

---

## Complete Implementation

```java
class Dictionary {
    String[] entries;
    
    public Dictionary(String[] entries) {
        this.entries = entries;
    }
    
    public boolean contains(String word) {
        return Arrays.asList(entries).contains(word);
    }
}

public class LongestWordFromLetters {
    
    // Approach 1: Character frequency (Recommended)
    public static Set<String> longestWord(String letters, Dictionary dict) {
        Set<String> result = new HashSet<>();
        int maxLen = 0;
        
        Map<Character, Integer> available = new HashMap<>();
        for (char c : letters.toCharArray()) {
            available.put(c, available.getOrDefault(c, 0) + 1);
        }
        
        for (String word : dict.entries) {
            if (canForm(word, available)) {
                if (word.length() > maxLen) {
                    result.clear();
                    result.add(word);
                    maxLen = word.length();
                } else if (word.length() == maxLen) {
                    result.add(word);
                }
            }
        }
        
        return result;
    }
    
    private static boolean canForm(String word, Map<Character, Integer> available) {
        Map<Character, Integer> needed = new HashMap<>();
        for (char c : word.toCharArray()) {
            needed.put(c, needed.getOrDefault(c, 0) + 1);
        }
        
        for (Map.Entry<Character, Integer> entry : needed.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        
        return true;
    }
    
    // Optimized canForm using array
    private static boolean canFormArray(String word, String letters) {
        int[] available = new int[26];
        for (char c : letters.toCharArray()) {
            available[c - 'a']++;
        }
        
        for (char c : word.toCharArray()) {
            if (--available[c - 'a'] < 0) {
                return false;
            }
        }
        
        return true;
    }
    
    // Bonus: Return with word length
    public static Map<String, Integer> longestWordWithLength(String letters, Dictionary dict) {
        Map<String, Integer> result = new HashMap<>();
        int maxLen = 0;
        
        Map<Character, Integer> available = new HashMap<>();
        for (char c : letters.toCharArray()) {
            available.put(c, available.getOrDefault(c, 0) + 1);
        }
        
        for (String word : dict.entries) {
            if (canForm(word, available)) {
                if (word.length() >= maxLen) {
                    if (word.length() > maxLen) {
                        result.clear();
                        maxLen = word.length();
                    }
                    result.put(word, word.length());
                }
            }
        }
        
        return result;
    }
    
    public static boolean pass() {
        Dictionary dict = new Dictionary(new String[]{
            "to", "toe", "toes", "doe", "dog", "god", "dogs", "banana"
        });
        
        Set<String> expected = new HashSet<>(Arrays.asList("toe"));
        Set<String> actual = longestWord("toe", dict);
        
        return expected.equals(actual);
    }
    
    public static void main(String[] args) {
        if (pass()) {
            System.out.println("Pass");
        } else {
            System.err.println("Fails");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testLongestWord() {
    Dictionary dict = new Dictionary(new String[]{
        "to", "toe", "toes", "doe", "dog", "god", "dogs", "banana"
    });
    
    // Basic case
    assertEquals(Set.of("toe"), longestWord("toe", dict));
    
    // Multiple longest words
    assertEquals(Set.of("dog", "god"), longestWord("dog", dict));
    
    // No valid words
    assertEquals(Set.of(), longestWord("xyz", dict));
    
    // Single letter
    assertEquals(Set.of(), longestWord("t", dict));
    
    // All letters available
    assertEquals(Set.of("dogs"), longestWord("dogs", dict));
    
    // Repeated letters
    Dictionary dict2 = new Dictionary(new String[]{"aa", "aaa", "a"});
    assertEquals(Set.of("aa"), longestWord("aa", dict2));
}
```

---

## Visual Representation

```
letters = "oet"
Available: {o:1, e:1, t:1}

Dictionary Check:
┌──────┬─────────────┬──────────┬────────┐
│ Word │ Needed      │ Can Form │ Length │
├──────┼─────────────┼──────────┼────────┤
│ to   │ {t:1, o:1}  │    ✓     │   2    │
│ toe  │ {t:1,o:1,e:1}│   ✓     │   3    │ ← Longest
│ toes │ {t:1,o:1,e:1,s:1}│ ✗   │   4    │
│ doe  │ {d:1,o:1,e:1}│   ✗     │   3    │
└──────┴─────────────┴──────────┴────────┘

Result: {"toe"}
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `"", dict` | `{}` | No letters |
| `"xyz", dict` | `{}` | No valid words |
| `"dog", {"dog","god"}` | `{"dog","god"}` | Multiple max |
| `"a", {"a","aa"}` | `{"a"}` | Exact match |
| Repeated letters | Check count | "aa" needs 2 'a's |

---

## Common Mistakes

1. **Not Counting Letter Frequency:**
   ```java
   // WRONG - "aa" would match "a"
   if (letters.contains(word))
   
   // CORRECT - count each letter
   Map<Character, Integer> available
   ```

2. **Not Clearing Result on New Max:**
   ```java
   // WRONG - keeps shorter words
   if (word.length() >= maxLen) {
       result.add(word);
   }
   
   // CORRECT - clear when new max found
   if (word.length() > maxLen) {
       result.clear();
       result.add(word);
       maxLen = word.length();
   }
   ```

3. **Modifying Available Map:**
   ```java
   // WRONG - modifies original
   for (char c : word.toCharArray()) {
       available.put(c, available.get(c) - 1);
   }
   
   // CORRECT - create separate needed map
   Map<Character, Integer> needed = new HashMap<>();
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Frequency matching | O(n × m) | O(k) | n=dict size, m=word length |
| Permutations | O(n!) | O(n!) | Exponential, not practical |
| Array frequency | O(n × m) | O(1) | Fixed 26 letters |

---

## Optimization: Array Instead of HashMap

```java
// For lowercase letters only
private static boolean canFormArray(String word, String letters) {
    int[] available = new int[26];
    for (char c : letters.toCharArray()) {
        available[c - 'a']++;
    }
    
    for (char c : word.toCharArray()) {
        if (--available[c - 'a'] < 0) {
            return false;
        }
    }
    
    return true;
}

// Faster: O(1) space, better cache locality
```

---

## Related Problems

- **LeetCode 720:** Longest Word in Dictionary
- **LeetCode 524:** Longest Word in Dictionary through Deleting
- **Anagram problems**
- **Scrabble word finder**

---

## Interview Tips

1. **Clarify Requirements:**
   - Case sensitive?
   - Return all or just one?
   - Empty input handling?
   - Letter reuse allowed?

2. **Explain Approach:**
   - Count available letters
   - Check each dictionary word
   - Track maximum length

3. **Walk Through Example:**
   - Use "oet" with small dictionary
   - Show frequency matching

4. **Discuss Optimization:**
   - Array vs HashMap
   - Early termination possible?

5. **Handle Edge Cases:**
   - Empty letters
   - No valid words
   - Multiple longest words

---

## Real-World Applications

- **Word Games:** Scrabble, Words with Friends
- **Anagram Solvers:** Puzzle helpers
- **Spell Checkers:** Suggestion generation
- **Crossword Puzzles:** Word finding
- **Text Analysis:** Vocabulary matching
- **Educational Apps:** Word learning games

---

## Key Takeaways

✅ Use character frequency map to track available letters  
✅ Check each dictionary word if it can be formed  
✅ Track maximum length and collect all words with that length  
✅ O(n × m) time where n = dictionary size, m = word length  
✅ Array optimization for lowercase: O(1) space  
✅ Clear result set when new maximum found  
✅ Handle multiple words with same maximum length  
✅ Don't modify original available map during checking
