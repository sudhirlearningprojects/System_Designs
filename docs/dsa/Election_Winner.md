# Find Winner of an Election

## Problem Statement

Find the winner of an election where votes are represented as candidate names in an array. Each candidate name in the array represents one vote. Return the candidate with the maximum votes. If there's a tie, return the lexicographically smaller name.

**Input:** Array of strings (candidate names)  
**Output:** String (winner's name)

**Examples:**
```
Input:  ["Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"]
Output: "Alice"
Explanation: Alice=3, Bob=2, Charlie=1. Alice wins.

Input:  ["John", "Mary", "John", "Mary"]
Output: "John"
Explanation: John=2, Mary=2. Tie, so "John" < "Mary" lexicographically.

Input:  ["Zara", "Alice", "Bob", "Alice", "Bob", "Zara"]
Output: "Alice"
Explanation: All have 2 votes. "Alice" < "Bob" < "Zara" lexicographically.

Input:  ["David"]
Output: "David"
Explanation: Single candidate wins.

Input:  ["Eve", "Eve", "Eve"]
Output: "Eve"
Explanation: Eve gets all votes.
```

---

## Solution Approaches

### Approach 1: HashMap + Single Pass (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(k) where k = unique candidates

```java
public static String findWinner(String[] votes) {
    Map<String, Integer> voteCount = new HashMap<>();
    
    for (String vote : votes) {
        voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
    }
    
    String winner = "";
    int maxVotes = 0;
    
    for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
        String candidate = entry.getKey();
        int count = entry.getValue();
        
        if (count > maxVotes || (count == maxVotes && candidate.compareTo(winner) < 0)) {
            winner = candidate;
            maxVotes = count;
        }
    }
    
    return winner;
}
```

---

### Approach 2: TreeMap (Auto-sorted)

**Time Complexity:** O(n log k)  
**Space Complexity:** O(k)

```java
public static String findWinnerTreeMap(String[] votes) {
    Map<String, Integer> voteCount = new TreeMap<>();
    
    for (String vote : votes) {
        voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
    }
    
    String winner = "";
    int maxVotes = 0;
    
    for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
        if (entry.getValue() > maxVotes) {
            winner = entry.getKey();
            maxVotes = entry.getValue();
        }
    }
    
    return winner;
}
```

---

### Approach 3: Stream API (Functional)

**Time Complexity:** O(n)  
**Space Complexity:** O(k)

```java
public static String findWinnerStream(String[] votes) {
    return Arrays.stream(votes)
        .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
        .entrySet().stream()
        .max(Comparator.comparing(Map.Entry<String, Long>::getValue)
            .thenComparing(Comparator.comparing(Map.Entry<String, Long>::getKey).reversed()))
        .map(Map.Entry::getKey)
        .orElse("");
}
```

---

## Algorithm Walkthrough

### Example: ["Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"]

**Step-by-Step Execution:**

```
Initial: votes = ["Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"]
         voteCount = {}
         winner = ""
         maxVotes = 0

Step 1: Process "Alice"
  voteCount = {Alice: 1}

Step 2: Process "Bob"
  voteCount = {Alice: 1, Bob: 1}

Step 3: Process "Alice"
  voteCount = {Alice: 2, Bob: 1}

Step 4: Process "Charlie"
  voteCount = {Alice: 2, Bob: 1, Charlie: 1}

Step 5: Process "Bob"
  voteCount = {Alice: 2, Bob: 2, Charlie: 1}

Step 6: Process "Alice"
  voteCount = {Alice: 3, Bob: 2, Charlie: 1}

Finding Winner:
  Check Alice: count=3 > maxVotes=0 → winner="Alice", maxVotes=3
  Check Bob: count=2 < maxVotes=3 → skip
  Check Charlie: count=1 < maxVotes=3 → skip

Result: "Alice"
```

### Example with Tie: ["John", "Mary", "John", "Mary"]

```
Initial: votes = ["John", "Mary", "John", "Mary"]
         voteCount = {}

After counting:
  voteCount = {John: 2, Mary: 2}

Finding Winner:
  Check John: count=2 > maxVotes=0 → winner="John", maxVotes=2
  Check Mary: count=2 == maxVotes=2
              "Mary".compareTo("John") > 0 → skip (John is smaller)

Result: "John" (lexicographically smaller)
```

### Example with Multiple Ties: ["Zara", "Alice", "Bob", "Alice", "Bob", "Zara"]

```
After counting:
  voteCount = {Zara: 2, Alice: 2, Bob: 2}

Finding Winner:
  Check Zara: count=2 > maxVotes=0 → winner="Zara", maxVotes=2
  Check Alice: count=2 == maxVotes=2
               "Alice".compareTo("Zara") < 0 → winner="Alice"
  Check Bob: count=2 == maxVotes=2
             "Bob".compareTo("Alice") > 0 → skip

Result: "Alice" (lexicographically smallest among tied)
```

---

## Complete Implementation

```java
import java.util.*;
import java.util.stream.*;

public class Solution {
    
    // Approach 1: HashMap + Single Pass (Optimal)
    public static String findWinner(String[] votes) {
        if (votes == null || votes.length == 0) return "";
        
        Map<String, Integer> voteCount = new HashMap<>();
        
        for (String vote : votes) {
            voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
        }
        
        String winner = "";
        int maxVotes = 0;
        
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            String candidate = entry.getKey();
            int count = entry.getValue();
            
            if (count > maxVotes || (count == maxVotes && candidate.compareTo(winner) < 0)) {
                winner = candidate;
                maxVotes = count;
            }
        }
        
        return winner;
    }
    
    // Approach 2: TreeMap (Auto-sorted)
    public static String findWinnerTreeMap(String[] votes) {
        if (votes == null || votes.length == 0) return "";
        
        Map<String, Integer> voteCount = new TreeMap<>();
        
        for (String vote : votes) {
            voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
        }
        
        String winner = "";
        int maxVotes = 0;
        
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                winner = entry.getKey();
                maxVotes = entry.getValue();
            }
        }
        
        return winner;
    }
    
    // Approach 3: Stream API (Functional)
    public static String findWinnerStream(String[] votes) {
        if (votes == null || votes.length == 0) return "";
        
        return Arrays.stream(votes)
            .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
            .entrySet().stream()
            .max(Comparator.comparing(Map.Entry<String, Long>::getValue)
                .thenComparing(Comparator.comparing(Map.Entry<String, Long>::getKey).reversed()))
            .map(Map.Entry::getKey)
            .orElse("");
    }
    
    // Helper: Get all candidates with vote counts
    public static Map<String, Integer> getVoteCounts(String[] votes) {
        Map<String, Integer> voteCount = new HashMap<>();
        for (String vote : votes) {
            voteCount.put(vote, voteCount.getOrDefault(vote, 0) + 1);
        }
        return voteCount;
    }
    
    // Helper: Get top N candidates
    public static List<String> getTopNCandidates(String[] votes, int n) {
        Map<String, Integer> voteCount = getVoteCounts(votes);
        
        return voteCount.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .limit(n)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    public static boolean doTestsPass() {
        // Test 1: Clear winner
        String[] votes1 = {"Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"};
        if (!findWinner(votes1).equals("Alice")) return false;
        
        // Test 2: Tie - lexicographic
        String[] votes2 = {"John", "Mary", "John", "Mary"};
        if (!findWinner(votes2).equals("John")) return false;
        
        // Test 3: Multiple ties
        String[] votes3 = {"Zara", "Alice", "Bob", "Alice", "Bob", "Zara"};
        if (!findWinner(votes3).equals("Alice")) return false;
        
        // Test 4: Single candidate
        String[] votes4 = {"David"};
        if (!findWinner(votes4).equals("David")) return false;
        
        // Test 5: All same
        String[] votes5 = {"Eve", "Eve", "Eve"};
        if (!findWinner(votes5).equals("Eve")) return false;
        
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
            {"Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"},
            {"John", "Mary", "John", "Mary"},
            {"Zara", "Alice", "Bob", "Alice", "Bob", "Zara"},
            {"David"},
            {"Eve", "Eve", "Eve"}
        };
        
        for (String[] votes : testCases) {
            String winner = findWinner(votes);
            Map<String, Integer> counts = getVoteCounts(votes);
            
            System.out.println("Votes: " + Arrays.toString(votes));
            System.out.println("Vote counts: " + counts);
            System.out.println("Winner: " + winner);
            System.out.println();
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testFindWinner() {
    // Test 1: Clear winner
    String[] votes1 = {"Alice", "Bob", "Alice", "Charlie", "Bob", "Alice"};
    assertEquals("Alice", findWinner(votes1));
    
    // Test 2: Tie - lexicographic order
    String[] votes2 = {"John", "Mary", "John", "Mary"};
    assertEquals("John", findWinner(votes2));
    
    // Test 3: Multiple candidates with same votes
    String[] votes3 = {"Zara", "Alice", "Bob", "Alice", "Bob", "Zara"};
    assertEquals("Alice", findWinner(votes3));
    
    // Test 4: Single candidate
    String[] votes4 = {"David"};
    assertEquals("David", findWinner(votes4));
    
    // Test 5: All votes for same candidate
    String[] votes5 = {"Eve", "Eve", "Eve", "Eve"};
    assertEquals("Eve", findWinner(votes5));
    
    // Test 6: Two candidates
    String[] votes6 = {"A", "B"};
    assertEquals("A", findWinner(votes6));
    
    // Test 7: Reverse alphabetical with tie
    String[] votes7 = {"Zoe", "Amy", "Zoe", "Amy"};
    assertEquals("Amy", findWinner(votes7));
    
    // Test 8: Case sensitivity
    String[] votes8 = {"alice", "Alice", "alice"};
    assertEquals("alice", findWinner(votes8));
    
    // Test 9: Empty array
    String[] votes9 = {};
    assertEquals("", findWinner(votes9));
    
    // Test 10: Large dataset
    String[] votes10 = new String[1000];
    Arrays.fill(votes10, 0, 400, "Candidate1");
    Arrays.fill(votes10, 400, 800, "Candidate2");
    Arrays.fill(votes10, 800, 1000, "Candidate3");
    assertEquals("Candidate1", findWinner(votes10));
}
```

---

## Visual Representation

### Vote Counting Process

```
Votes: [Alice, Bob, Alice, Charlie, Bob, Alice]

HashMap Building:
┌─────────┬───────┐
│ Alice   │   3   │
├─────────┼───────┤
│ Bob     │   2   │
├─────────┼───────┤
│ Charlie │   1   │
└─────────┴───────┘

Winner: Alice (max votes = 3)
```

### Tie-Breaking with Lexicographic Order

```
Votes: [John, Mary, John, Mary]

Vote Count:
┌──────┬───────┐
│ John │   2   │
├──────┼───────┤
│ Mary │   2   │
└──────┴───────┘

Lexicographic Comparison:
"John".compareTo("Mary") = -3 (negative)
"John" < "Mary" ✓

Winner: John
```

### Multiple Ties

```
Votes: [Zara, Alice, Bob, Alice, Bob, Zara]

Vote Count:
┌───────┬───────┐
│ Alice │   2   │
├───────┼───────┤
│ Bob   │   2   │
├───────┼───────┤
│ Zara  │   2   │
└───────┴───────┘

Lexicographic Order:
Alice < Bob < Zara

Winner: Alice (smallest lexicographically)
```

---

## Edge Cases

1. **Empty array:** Return empty string
2. **Single vote:** Return that candidate
3. **All votes same:** Return that candidate
4. **Two-way tie:** Return lexicographically smaller
5. **Multi-way tie:** Return lexicographically smallest
6. **Case sensitivity:** "Alice" ≠ "alice"
7. **Null input:** Handle gracefully
8. **Large dataset:** Efficient O(n) solution

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(k) | **Optimal solution** |
| TreeMap | O(n log k) | O(k) | Auto-sorted keys |
| Stream API | O(n) | O(k) | Functional style |

**Where:**
- n = number of votes
- k = number of unique candidates

**Time Complexity Breakdown:**
- Count votes: O(n)
- Find winner: O(k)
- Total: O(n + k) = O(n) since k ≤ n

**Space Complexity:**
- HashMap: O(k) for storing vote counts
- Best case: O(1) if all votes for one candidate
- Worst case: O(n) if all votes for different candidates

---

## Related Problems

1. **Top K Frequent Elements** - Find k most voted candidates
2. **Majority Element** - Find candidate with >n/2 votes
3. **Group Anagrams** - Similar HashMap grouping
4. **First Unique Character** - Find first non-repeated vote
5. **Sort Characters by Frequency** - Sort candidates by votes
6. **Kth Largest Element** - Find kth most voted candidate

---

## Interview Tips

### Clarification Questions
1. Can the array be empty? (Yes, return empty string)
2. Are names case-sensitive? (Yes, "Alice" ≠ "alice")
3. What if there's a tie? (Return lexicographically smaller)
4. Can there be null values? (Assume no nulls)
5. What's the expected size? (Optimize for large datasets)

### Approach Explanation
1. "I'll use a HashMap to count votes for each candidate"
2. "Iterate through votes array once to build counts"
3. "Find candidate with maximum votes"
4. "If tie, compare lexicographically and choose smaller"
5. "Time O(n), Space O(k) where k is unique candidates"

### Common Mistakes
1. **Forgetting tie-breaking rule** - Must handle lexicographic order
2. **Wrong comparison** - Use compareTo() < 0, not <=
3. **Not handling empty array** - Check for null/empty input
4. **Case sensitivity** - "Alice" and "alice" are different
5. **Inefficient sorting** - Don't sort entire array, use HashMap

### Follow-up Questions
1. "What if we need top K candidates?" - Use PriorityQueue or sort by votes
2. "What about streaming votes?" - Update HashMap incrementally
3. "Memory constraints?" - External sorting for huge datasets
4. "Real-time results?" - Maintain max heap with current leader
5. "Distributed system?" - MapReduce pattern for vote counting

---

## Real-World Applications

1. **Elections** - Political voting systems
2. **Polls** - Online surveys and polls
3. **Ratings** - Product/movie rating aggregation
4. **Social Media** - Trending topics, hashtags
5. **E-commerce** - Most popular products
6. **Gaming** - Leaderboards and rankings
7. **Analytics** - User behavior tracking

---

## Key Takeaways

1. **HashMap is optimal:** O(n) time for counting and finding winner
2. **Tie-breaking crucial:** Use compareTo() for lexicographic comparison
3. **Single pass counting:** Build vote count map in one iteration
4. **Edge cases matter:** Empty array, single vote, all same votes
5. **Space-time tradeoff:** O(k) space for O(n) time efficiency
6. **TreeMap alternative:** Auto-sorted but O(n log k) time
7. **Stream API elegant:** Functional approach for modern Java

---

## Optimization Notes

### Why HashMap Over Array?
- **Dynamic candidates:** Don't know candidates beforehand
- **String keys:** Candidate names are strings, not indices
- **Sparse data:** Not all candidates may receive votes
- **O(1) lookup:** Fast vote counting

### TreeMap vs HashMap
```java
// HashMap: O(n) but needs manual comparison
Map<String, Integer> map = new HashMap<>();

// TreeMap: O(n log k) but auto-sorted
Map<String, Integer> map = new TreeMap<>();
```

### Best Practice
```java
// Use HashMap for optimal performance
public static String findWinner(String[] votes) {
    Map<String, Integer> voteCount = new HashMap<>();
    
    // Count votes: O(n)
    for (String vote : votes) {
        voteCount.merge(vote, 1, Integer::sum);
    }
    
    // Find winner: O(k)
    return voteCount.entrySet().stream()
        .max(Comparator.comparing(Map.Entry<String, Integer>::getValue)
            .thenComparing(Comparator.comparing(Map.Entry<String, Integer>::getKey).reversed()))
        .map(Map.Entry::getKey)
        .orElse("");
}
```

---

## Comparison: Different Approaches

```
Input: ["Alice", "Bob", "Alice", "Bob"]

Approach 1: HashMap (Optimal)
- Count: {Alice: 2, Bob: 2}
- Compare: "Alice" < "Bob"
- Winner: Alice
- Time: O(n), Space: O(k)

Approach 2: TreeMap
- Count: {Alice: 2, Bob: 2} (auto-sorted)
- First with max: Alice
- Winner: Alice
- Time: O(n log k), Space: O(k)

Approach 3: Sorting (Inefficient)
- Sort: ["Alice", "Alice", "Bob", "Bob"]
- Count consecutive: Alice=2, Bob=2
- Winner: Alice
- Time: O(n log n), Space: O(1)
```
