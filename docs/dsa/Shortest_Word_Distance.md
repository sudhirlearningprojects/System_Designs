# Shortest Distance Between Words in Document

## Problem Statement

Find the shortest distance between the midpoints of two words in a document. Words can appear multiple times and comparison is case-insensitive.

**Distance:** Number of characters between word midpoints

**Input:** Document string, two words  
**Output:** Shortest distance (double)

**Example:**
```
document = "Example we just made up"
shortestDistance(document, "we", "just") = 4

Explanation:
"we" starts at index 8, length 2, midpoint = 9
"just" starts at index 11, length 4, midpoint = 13
Distance = |13 - 9| = 4
```

---

## Solution Approach

### Optimal: Track All Positions

**Time Complexity:** O(n)  
**Space Complexity:** O(k) where k = word occurrences

```java
public static double shortestDistance(String document, String word1, String word2) {
    String docLower = document.toLowerCase();
    String w1 = word1.toLowerCase();
    String w2 = word2.toLowerCase();
    
    List<Double> pos1 = new ArrayList<>();
    List<Double> pos2 = new ArrayList<>();
    
    // Find all positions and midpoints
    int index = 0;
    while ((index = docLower.indexOf(w1, index)) != -1) {
        pos1.add(index + w1.length() / 2.0);
        index++;
    }
    
    index = 0;
    while ((index = docLower.indexOf(w2, index)) != -1) {
        pos2.add(index + w2.length() / 2.0);
        index++;
    }
    
    // Find minimum distance
    double minDist = Double.MAX_VALUE;
    for (double p1 : pos1) {
        for (double p2 : pos2) {
            minDist = Math.min(minDist, Math.abs(p1 - p2));
        }
    }
    
    return minDist;
}
```

---

## Algorithm Walkthrough

### Example: "Example we just made up"

```
Find: "we" and "just"

Step 1: Convert to lowercase
  document = "example we just made up"
  word1 = "we"
  word2 = "just"

Step 2: Find "we" positions
  Index 8: "we"
  Midpoint = 8 + 2/2.0 = 9.0
  pos1 = [9.0]

Step 3: Find "just" positions
  Index 11: "just"
  Midpoint = 11 + 4/2.0 = 13.0
  pos2 = [13.0]

Step 4: Calculate distances
  |9.0 - 13.0| = 4.0

Result: 4.0
```

---

## Complete Implementation

```java
public class ShortestWordDistance {
    
    // Approach 1: Find all positions (Recommended)
    public static double shortestDistance(String document, String word1, String word2) {
        String docLower = document.toLowerCase();
        String w1 = word1.toLowerCase();
        String w2 = word2.toLowerCase();
        
        List<Double> pos1 = findMidpoints(docLower, w1);
        List<Double> pos2 = findMidpoints(docLower, w2);
        
        if (pos1.isEmpty() || pos2.isEmpty()) {
            return -1; // Word not found
        }
        
        double minDist = Double.MAX_VALUE;
        for (double p1 : pos1) {
            for (double p2 : pos2) {
                minDist = Math.min(minDist, Math.abs(p1 - p2));
            }
        }
        
        return minDist;
    }
    
    private static List<Double> findMidpoints(String text, String word) {
        List<Double> midpoints = new ArrayList<>();
        int index = 0;
        
        while ((index = text.indexOf(word, index)) != -1) {
            double midpoint = index + word.length() / 2.0;
            midpoints.add(midpoint);
            index++;
        }
        
        return midpoints;
    }
    
    // Approach 2: Single pass with word boundary check
    public static double shortestDistanceWordBoundary(String document, String word1, String word2) {
        String[] words = document.toLowerCase().split("\\s+");
        String w1 = word1.toLowerCase();
        String w2 = word2.toLowerCase();
        
        List<Integer> pos1 = new ArrayList<>();
        List<Integer> pos2 = new ArrayList<>();
        
        int charPos = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(w1)) {
                pos1.add(charPos + words[i].length() / 2);
            } else if (words[i].equals(w2)) {
                pos2.add(charPos + words[i].length() / 2);
            }
            charPos += words[i].length() + 1; // +1 for space
        }
        
        double minDist = Double.MAX_VALUE;
        for (int p1 : pos1) {
            for (int p2 : pos2) {
                minDist = Math.min(minDist, Math.abs(p1 - p2));
            }
        }
        
        return minDist;
    }
    
    // Approach 3: Optimized with sorted positions
    public static double shortestDistanceOptimized(String document, String word1, String word2) {
        List<Double> pos1 = findMidpoints(document.toLowerCase(), word1.toLowerCase());
        List<Double> pos2 = findMidpoints(document.toLowerCase(), word2.toLowerCase());
        
        if (pos1.isEmpty() || pos2.isEmpty()) return -1;
        
        // Two pointers on sorted lists
        int i = 0, j = 0;
        double minDist = Double.MAX_VALUE;
        
        while (i < pos1.size() && j < pos2.size()) {
            minDist = Math.min(minDist, Math.abs(pos1.get(i) - pos2.get(j)));
            
            if (pos1.get(i) < pos2.get(j)) {
                i++;
            } else {
                j++;
            }
        }
        
        return minDist;
    }
    
    // Bonus: Return all distances
    public static List<Double> allDistances(String document, String word1, String word2) {
        List<Double> pos1 = findMidpoints(document.toLowerCase(), word1.toLowerCase());
        List<Double> pos2 = findMidpoints(document.toLowerCase(), word2.toLowerCase());
        
        List<Double> distances = new ArrayList<>();
        for (double p1 : pos1) {
            for (double p2 : pos2) {
                distances.add(Math.abs(p1 - p2));
            }
        }
        
        Collections.sort(distances);
        return distances;
    }
    
    public static boolean pass() {
        String document = "In publishing and graphic design, lorem ipsum is a filler text commonly " +
                         "used to demonstrate the graphic elements lorem ipsum text has been used " +
                         "in typesetting since the 1960s or earlier, when it was popularized by " +
                         "advertisements for Letraset transfer sheets. It was introduced to the " +
                         "Information Age in the mid-1980s by Aldus Corporation, which";
        
        return shortestDistance(document, "and", "graphic") == 6d &&
               shortestDistance(document, "transfer", "it") == 14d &&
               shortestDistance(document, "Design", "filler") == 25d;
    }
    
    public static void main(String[] args) {
        if (pass()) {
            System.out.println("Pass");
        } else {
            System.out.println("Some Fail");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testShortestDistance() {
    String doc = "Example we just made up";
    
    // Basic case
    assertEquals(4.0, shortestDistance(doc, "we", "just"), 0.01);
    
    // Case insensitive
    assertEquals(4.0, shortestDistance(doc, "WE", "JUST"), 0.01);
    assertEquals(4.0, shortestDistance(doc, "We", "Just"), 0.01);
    
    // Multiple occurrences
    String doc2 = "the cat and the dog and the bird";
    double dist = shortestDistance(doc2, "the", "and");
    assertTrue(dist > 0);
    
    // Same word
    assertEquals(0.0, shortestDistance(doc, "we", "we"), 0.01);
    
    // Word not found
    assertEquals(-1.0, shortestDistance(doc, "xyz", "abc"), 0.01);
    
    // Adjacent words
    String doc3 = "hello world";
    assertTrue(shortestDistance(doc3, "hello", "world") < 10);
}
```

---

## Visual Representation

```
Document: "Example we just made up"
Indices:   0123456789012345678901234

Word "we":
  Position: 8-9
  Length: 2
  Midpoint: 8 + 2/2 = 9.0
           ↓
  "Example we just made up"
           ^^

Word "just":
  Position: 11-14
  Length: 4
  Midpoint: 11 + 4/2 = 13.0
               ↓
  "Example we just made up"
              ^^^^

Distance: |13.0 - 9.0| = 4.0
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| Same word | `0.0` | Distance to itself |
| Word not found | `-1.0` | Invalid input |
| Adjacent words | Small | Close together |
| Multiple occurrences | Minimum | Closest pair |
| Case mismatch | Works | Case insensitive |

---

## Common Mistakes

1. **Not Handling Case:**
   ```java
   // WRONG - case sensitive
   document.indexOf(word1)
   
   // CORRECT - case insensitive
   document.toLowerCase().indexOf(word1.toLowerCase())
   ```

2. **Wrong Midpoint Calculation:**
   ```java
   // WRONG - integer division
   midpoint = index + word.length() / 2;
   
   // CORRECT - floating point
   midpoint = index + word.length() / 2.0;
   ```

3. **Not Finding All Occurrences:**
   ```java
   // WRONG - finds only first
   int index = text.indexOf(word);
   
   // CORRECT - finds all
   while ((index = text.indexOf(word, index)) != -1) {
       // process
       index++;
   }
   ```

4. **Not Checking All Pairs:**
   ```java
   // Must check all combinations
   for (double p1 : pos1) {
       for (double p2 : pos2) {
           minDist = Math.min(minDist, Math.abs(p1 - p2));
       }
   }
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Find all positions | O(n × m) | O(k) | m = word length, k = occurrences |
| Two pointers | O(n + k log k) | O(k) | If positions sorted |
| Single pass | O(n) | O(k) | Best for single query |

**Where:**
- n = document length
- k = total word occurrences
- m = average word length

---

## Midpoint Calculation

```
Word at index i with length L:
Midpoint = i + L / 2.0

Examples:
"we" at index 8, length 2:
  Midpoint = 8 + 2/2.0 = 9.0

"just" at index 11, length 4:
  Midpoint = 11 + 4/2.0 = 13.0

"a" at index 0, length 1:
  Midpoint = 0 + 1/2.0 = 0.5
```

---

## Optimization: Two Pointers

```java
// If positions are sorted (they are from indexOf)
public static double shortestDistanceOptimized(List<Double> pos1, List<Double> pos2) {
    int i = 0, j = 0;
    double minDist = Double.MAX_VALUE;
    
    while (i < pos1.size() && j < pos2.size()) {
        minDist = Math.min(minDist, Math.abs(pos1.get(i) - pos2.get(j)));
        
        if (pos1.get(i) < pos2.get(j)) {
            i++;
        } else {
            j++;
        }
    }
    
    return minDist;
}

// Reduces from O(k1 × k2) to O(k1 + k2)
```

---

## Related Problems

- **LeetCode 243:** Shortest Word Distance
- **LeetCode 244:** Shortest Word Distance II
- **LeetCode 245:** Shortest Word Distance III
- **String matching problems**

---

## Interview Tips

1. **Clarify Requirements:**
   - Case sensitive?
   - Word boundaries?
   - What if word not found?
   - Multiple occurrences?

2. **Explain Approach:**
   - Find all positions
   - Calculate midpoints
   - Find minimum distance

3. **Walk Through Example:**
   - Use "Example we just made up"
   - Show midpoint calculation

4. **Discuss Optimization:**
   - Two pointers if sorted
   - Single pass possible

5. **Handle Edge Cases:**
   - Same word
   - Word not found
   - Case insensitivity

---

## Real-World Applications

- **Text Analysis:** Finding related terms
- **Search Engines:** Proximity ranking
- **Document Similarity:** Measuring closeness
- **Natural Language Processing:** Co-occurrence analysis
- **Information Retrieval:** Relevance scoring
- **Plagiarism Detection:** Pattern matching

---

## Word Boundary Consideration

```java
// Problem: "the" matches "there", "other"
// Solution: Use word boundaries

public static List<Double> findMidpointsWithBoundary(String text, String word) {
    List<Double> midpoints = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(text);
    
    while (matcher.find()) {
        double midpoint = matcher.start() + word.length() / 2.0;
        midpoints.add(midpoint);
    }
    
    return midpoints;
}
```

---

## Key Takeaways

✅ Find all occurrences of both words  
✅ Calculate midpoint: `index + length / 2.0`  
✅ Check all pairs for minimum distance  
✅ Case insensitive comparison  
✅ O(n) to find positions, O(k1 × k2) to compare  
✅ Two pointers optimization: O(k1 + k2)  
✅ Handle edge cases: word not found, same word  
✅ Consider word boundaries for exact matches
