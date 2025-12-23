# Group Anagrams Together

## Problem Statement

Print or return words grouped by anagrams from a given string.

**Input:** String containing space-separated words  
**Output:** Words grouped by anagrams

**Example:**
```
Input:  "cat dog tac sat tas god dog"
Output: "cat tac dog god dog sat tas"

Grouped: [cat, tac] [dog, god, dog] [sat, tas]
```

---

## Solution Approaches

### Approach 1: HashMap with Sorted Key (Optimal)

**Time Complexity:** O(n × k log k) where n = number of words, k = average word length  
**Space Complexity:** O(n × k)

```java
public String groupAnagrams(String input) {
    String[] words = input.split(" ");
    Map<String, List<String>> map = new LinkedHashMap<>();
    
    for (String word : words) {
        char[] chars = word.toCharArray();
        Arrays.sort(chars);
        String key = new String(chars);
        
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }
    
    StringBuilder result = new StringBuilder();
    for (List<String> group : map.values()) {
        for (String word : group) {
            result.append(word).append(" ");
        }
    }
    
    return result.toString().trim();
}
```

---

### Approach 2: HashMap with Character Count Key

**Time Complexity:** O(n × k) - better than sorting  
**Space Complexity:** O(n × k)

```java
public String groupAnagrams(String input) {
    String[] words = input.split(" ");
    Map<String, List<String>> map = new LinkedHashMap<>();
    
    for (String word : words) {
        String key = getCharCountKey(word);
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }
    
    StringBuilder result = new StringBuilder();
    for (List<String> group : map.values()) {
        for (String word : group) {
            result.append(word).append(" ");
        }
    }
    
    return result.toString().trim();
}

private String getCharCountKey(String word) {
    int[] count = new int[26];
    for (char c : word.toCharArray()) {
        count[c - 'a']++;
    }
    
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < 26; i++) {
        if (count[i] > 0) {
            key.append((char)('a' + i)).append(count[i]);
        }
    }
    return key.toString();
}
```

---

## Algorithm Explanation

### Key Insight

**Anagrams have the same characters when sorted:**
```
"cat" → sorted: "act"
"tac" → sorted: "act"
"dog" → sorted: "dgo"
"god" → sorted: "dgo"
```

Use sorted string as HashMap key to group anagrams together.

---

## Detailed Walkthrough

**Input:** "cat dog tac sat tas god dog"

### Step 1: Split into words
```
words = ["cat", "dog", "tac", "sat", "tas", "god", "dog"]
```

### Step 2: Group by sorted key
```
Word: "cat" → Key: "act" → map["act"] = ["cat"]
Word: "dog" → Key: "dgo" → map["dgo"] = ["dog"]
Word: "tac" → Key: "act" → map["act"] = ["cat", "tac"]
Word: "sat" → Key: "ast" → map["ast"] = ["sat"]
Word: "tas" → Key: "ast" → map["ast"] = ["sat", "tas"]
Word: "god" → Key: "dgo" → map["dgo"] = ["dog", "god"]
Word: "dog" → Key: "dgo" → map["dgo"] = ["dog", "god", "dog"]
```

### Step 3: Build result
```
map = {
    "act": ["cat", "tac"],
    "dgo": ["dog", "god", "dog"],
    "ast": ["sat", "tas"]
}

Result: "cat tac dog god dog sat tas"
```

---

## Complete Implementation

```java
public class GroupAnagrams {
    
    // Approach 1: Sorted key (Most common)
    public String groupAnagrams(String input) {
        if (input == null || input.isEmpty()) return "";
        
        String[] words = input.split(" ");
        Map<String, List<String>> map = new LinkedHashMap<>();
        
        for (String word : words) {
            String key = sortString(word);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }
        
        return buildResult(map);
    }
    
    private String sortString(String s) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
    
    // Approach 2: Character count key (Faster)
    public String groupAnagramsOptimized(String input) {
        if (input == null || input.isEmpty()) return "";
        
        String[] words = input.split(" ");
        Map<String, List<String>> map = new LinkedHashMap<>();
        
        for (String word : words) {
            String key = getCharCountKey(word);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }
        
        return buildResult(map);
    }
    
    private String getCharCountKey(String word) {
        int[] count = new int[26];
        for (char c : word.toLowerCase().toCharArray()) {
            count[c - 'a']++;
        }
        
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (count[i] > 0) {
                key.append((char)('a' + i)).append(count[i]);
            }
        }
        return key.toString();
    }
    
    private String buildResult(Map<String, List<String>> map) {
        StringBuilder result = new StringBuilder();
        for (List<String> group : map.values()) {
            for (String word : group) {
                if (result.length() > 0) result.append(" ");
                result.append(word);
            }
        }
        return result.toString();
    }
    
    // Return as List of Lists (LeetCode style)
    public List<List<String>> groupAnagramsList(String[] words) {
        Map<String, List<String>> map = new HashMap<>();
        
        for (String word : words) {
            String key = sortString(word);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }
        
        return new ArrayList<>(map.values());
    }
    
    // Count anagram groups
    public int countAnagramGroups(String input) {
        String[] words = input.split(" ");
        Set<String> keys = new HashSet<>();
        
        for (String word : words) {
            keys.add(sortString(word));
        }
        
        return keys.size();
    }
}
```

---

## Test Cases

```java
@Test
public void testGroupAnagrams() {
    GroupAnagrams solver = new GroupAnagrams();
    
    // Basic case
    assertEquals("cat tac dog god dog sat tas", 
                 solver.groupAnagrams("cat dog tac sat tas god dog"));
    
    // No anagrams
    assertEquals("hello world java", 
                 solver.groupAnagrams("hello world java"));
    
    // All anagrams
    assertEquals("abc bca cab", 
                 solver.groupAnagrams("abc bca cab"));
    
    // Single word
    assertEquals("test", solver.groupAnagrams("test"));
    
    // Empty string
    assertEquals("", solver.groupAnagrams(""));
    
    // Duplicates
    assertEquals("cat cat tac", 
                 solver.groupAnagrams("cat cat tac"));
    
    // Mixed case (if case-insensitive)
    assertEquals("Cat tac", 
                 solver.groupAnagrams("Cat tac"));
}
```

---

## Visual Representation

```
Input: "cat dog tac sat tas god dog"

HashMap Structure:
┌─────────────────────────────────┐
│ Key: "act" → ["cat", "tac"]     │
│ Key: "dgo" → ["dog", "god", "dog"]│
│ Key: "ast" → ["sat", "tas"]     │
└─────────────────────────────────┘

Output: "cat tac dog god dog sat tas"
         └─┬─┘ └────┬────┘ └──┬──┘
        Group1   Group2    Group3
```

---

## Approach Comparison

| Approach | Time | Space | Key Generation | Best For |
|----------|------|-------|----------------|----------|
| Sorted String | O(n×k log k) | O(n×k) | Sort characters | General use |
| Character Count | O(n×k) | O(n×k) | Count array | Lowercase only |
| Prime Product | O(n×k) | O(n×k) | Prime multiplication | Small words |

**Recommendation:** Use sorted string for simplicity, character count for performance.

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `""` | `""` | Empty string |
| `"a"` | `"a"` | Single word |
| `"abc abc"` | `"abc abc"` | Duplicate words |
| `"a b c"` | `"a b c"` | No anagrams |
| `"Cat cat"` | Depends | Case sensitivity |

---

## Common Mistakes

1. **Not Preserving Order:**
   ```java
   // WRONG - HashMap doesn't preserve insertion order
   Map<String, List<String>> map = new HashMap<>();
   
   // CORRECT - LinkedHashMap preserves order
   Map<String, List<String>> map = new LinkedHashMap<>();
   ```

2. **Case Sensitivity:**
   ```java
   // Handle case if needed
   String key = sortString(word.toLowerCase());
   ```

3. **Null/Empty Input:**
   ```java
   if (input == null || input.isEmpty()) return "";
   ```

4. **Extra Spaces in Output:**
   ```java
   // Use trim() to remove trailing space
   return result.toString().trim();
   ```

---

## Variations

### Return as List of Lists (LeetCode 49)

```java
public List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    
    for (String str : strs) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        String key = new String(chars);
        
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
    }
    
    return new ArrayList<>(map.values());
}
```

### Find Largest Anagram Group

```java
public List<String> largestAnagramGroup(String input) {
    String[] words = input.split(" ");
    Map<String, List<String>> map = new HashMap<>();
    
    for (String word : words) {
        String key = sortString(word);
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
    }
    
    return map.values().stream()
        .max(Comparator.comparingInt(List::size))
        .orElse(new ArrayList<>());
}
```

### Count Anagram Pairs

```java
public int countAnagramPairs(String[] words) {
    Map<String, Integer> map = new HashMap<>();
    int count = 0;
    
    for (String word : words) {
        String key = sortString(word);
        int prev = map.getOrDefault(key, 0);
        count += prev; // Each existing word forms a pair with current
        map.put(key, prev + 1);
    }
    
    return count;
}
```

---

## Alternative: Prime Number Encoding

```java
private static final int[] PRIMES = {
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 
    43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101
};

private long getPrimeKey(String word) {
    long key = 1;
    for (char c : word.toLowerCase().toCharArray()) {
        key *= PRIMES[c - 'a'];
    }
    return key;
}

// Use Long as key instead of String
Map<Long, List<String>> map = new HashMap<>();
```

**Pros:** Unique key for anagrams  
**Cons:** Risk of overflow for long words

---

## Complexity Analysis

### Sorted String Approach
```
n = number of words
k = average word length

Time: O(n × k log k)
  - Split: O(n)
  - For each word: O(k log k) for sorting
  - HashMap operations: O(1) average
  - Build result: O(n × k)

Space: O(n × k)
  - HashMap storage
  - Result string
```

### Character Count Approach
```
Time: O(n × k)
  - For each word: O(k) for counting
  - Key generation: O(26) = O(1)
  
Space: O(n × k)
  - Same as sorted approach
```

---

## Related Problems

- **LeetCode 49:** Group Anagrams
- **LeetCode 242:** Valid Anagram
- **LeetCode 438:** Find All Anagrams in a String
- **LeetCode 567:** Permutation in String

---

## Interview Tips

1. **Clarify Requirements:**
   - Preserve original order?
   - Case sensitive?
   - Handle duplicates?
   - Return format (string vs list)?

2. **Start with HashMap:**
   - Explain sorted string as key
   - Mention O(n × k log k) complexity

3. **Optimize if Asked:**
   - Character count for O(n × k)
   - Prime encoding (mention overflow risk)

4. **Walk Through Example:**
   - Use "cat dog tac"
   - Show key generation

5. **Discuss Trade-offs:**
   - Simplicity vs performance
   - Memory usage

---

## Real-World Applications

- **Spell Checkers:** Finding word variations
- **Word Games:** Scrabble, anagram puzzles
- **Data Deduplication:** Identifying similar entries
- **Search Engines:** Query expansion
- **Plagiarism Detection:** Finding rearranged text

---

## Performance Tips

1. **Use LinkedHashMap** to preserve insertion order
2. **StringBuilder** for efficient string concatenation
3. **computeIfAbsent** for cleaner code
4. **Character count** for lowercase-only inputs
5. **Early validation** for null/empty inputs

---

## Key Takeaways

✅ Use sorted string as HashMap key for grouping  
✅ LinkedHashMap preserves insertion order  
✅ O(n × k log k) with sorting, O(n × k) with counting  
✅ Handle case sensitivity based on requirements  
✅ Character count approach is faster for lowercase  
✅ Essential pattern for anagram-related problems
