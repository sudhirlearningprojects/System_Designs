# Dynamic Programming - Hard Problems (10%)

## 📚 2 Hard Problems with Complete Solutions

---

## Problem 1: Edit Distance

**Difficulty**: Hard  
**Pattern**: 2D Subsequence DP  
**LeetCode**: #72

### Problem Statement

Given two strings `word1` and `word2`, return the minimum number of operations required to convert `word1` to `word2`. You can perform three operations:
- Insert a character
- Delete a character
- Replace a character

### Examples

```
Input: word1 = "horse", word2 = "ros"
Output: 3
Explanation: 
horse -> rorse (replace 'h' with 'r')
rorse -> rose (remove 'r')
rose -> ros (remove 'e')

Input: word1 = "intention", word2 = "execution"
Output: 5
Explanation:
intention -> inention (remove 't')
inention -> enention (replace 'i' with 'e')
enention -> exention (replace 'n' with 'x')
exention -> exection (replace 'n' with 'c')
exection -> execution (insert 'u')
```

### Intuition

**State Definition**: `dp[i][j]` = minimum operations to convert `word1[0..i-1]` to `word2[0..j-1]`

**Recurrence**:
```
If word1[i-1] == word2[j-1]:
    dp[i][j] = dp[i-1][j-1]  // No operation needed

Else:
    dp[i][j] = 1 + min(
        dp[i-1][j],      // Delete from word1
        dp[i][j-1],      // Insert into word1
        dp[i-1][j-1]     // Replace
    )
```

**Base Cases**:
```
dp[0][j] = j  // Insert j characters
dp[i][0] = i  // Delete i characters
```

### Solution

```java
public class EditDistance {
    public int minDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // Base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i-1) == word2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i-1][j],      // Delete
                        Math.min(
                            dp[i][j-1],  // Insert
                            dp[i-1][j-1] // Replace
                        )
                    );
                }
            }
        }
        
        return dp[m][n];
    }
    
    // Space Optimized: O(n) space
    public int minDistanceOptimized(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i-1) == word2.charAt(j-1)) {
                    curr[j] = prev[j-1];
                } else {
                    curr[j] = 1 + Math.min(
                        prev[j],
                        Math.min(curr[j-1], prev[j-1])
                    );
                }
            }
            
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n];
    }
}
```

### Dry Run

**Input**: `word1 = "horse"`, `word2 = "ros"`

```
DP Table:
       ""  r  o  s
    "" 0   1  2  3
    h  1   1  2  3
    o  2   2  1  2
    r  3   2  2  2
    s  4   3  3  2
    e  5   4  4  3

Step-by-step:

dp[0][0] = 0 (both empty)
dp[1][0] = 1 (delete 'h')
dp[0][1] = 1 (insert 'r')

dp[1][1]: word1[0]='h', word2[0]='r'
  Not equal, so:
  dp[1][1] = 1 + min(dp[0][1]=1, dp[1][0]=1, dp[0][0]=0) = 1

dp[2][2]: word1[1]='o', word2[1]='o'
  Equal, so:
  dp[2][2] = dp[1][1] = 1

dp[3][1]: word1[2]='r', word2[0]='r'
  Equal, so:
  dp[3][1] = dp[2][0] = 2

... continue filling table

Final: dp[5][3] = 3
```

### Operations Trace

```
horse -> ros

1. Replace 'h' with 'r': rorse
2. Delete 'r': rose
3. Delete 'e': ros

Total: 3 operations
```

### Complexity Analysis

- **Time Complexity**: O(m × n)
- **Space Complexity**: O(m × n) or O(n) with optimization

### Test Cases

```java
@Test
public void testMinDistance() {
    EditDistance solution = new EditDistance();
    
    assertEquals(3, solution.minDistance("horse", "ros"));
    assertEquals(5, solution.minDistance("intention", "execution"));
    assertEquals(0, solution.minDistance("abc", "abc"));
    assertEquals(3, solution.minDistance("abc", ""));
    assertEquals(3, solution.minDistance("", "abc"));
}
```

### Edge Cases

1. **Empty strings**: Return length of non-empty string
2. **Same strings**: Return 0
3. **One character difference**: Return 1
4. **Completely different**: Return max(m, n)

### Common Mistakes

1. **Wrong base cases**: Not initializing first row/column
2. **Off-by-one errors**: Using wrong indices
3. **Not handling empty strings**: Edge case not covered

---

## Problem 2: Regular Expression Matching

**Difficulty**: Hard  
**Pattern**: 2D DP with Pattern Matching  
**LeetCode**: #10

### Problem Statement

Given an input string `s` and a pattern `p`, implement regular expression matching with support for '.' and '*' where:
- '.' matches any single character
- '*' matches zero or more of the preceding element

The matching should cover the entire input string (not partial).

### Examples

```
Input: s = "aa", p = "a"
Output: false
Explanation: "a" does not match the entire string "aa"

Input: s = "aa", p = "a*"
Output: true
Explanation: '*' means zero or more of the preceding element 'a'

Input: s = "ab", p = ".*"
Output: true
Explanation: ".*" means "zero or more (*) of any character (.)"

Input: s = "aab", p = "c*a*b"
Output: true
Explanation: c can be repeated 0 times, a can be repeated 2 times
```

### Intuition

**State Definition**: `dp[i][j]` = does `s[0..i-1]` match `p[0..j-1]`

**Recurrence**:
```
If p[j-1] == '*':
    // '*' can match zero or more of preceding element
    dp[i][j] = dp[i][j-2]  // Match zero occurrences
    
    If s[i-1] matches p[j-2] (either same char or p[j-2] == '.'):
        dp[i][j] |= dp[i-1][j]  // Match one or more occurrences

Else if s[i-1] == p[j-1] or p[j-1] == '.':
    dp[i][j] = dp[i-1][j-1]  // Characters match

Else:
    dp[i][j] = false  // No match
```

**Base Cases**:
```
dp[0][0] = true  // Empty string matches empty pattern
dp[i][0] = false for i > 0  // Non-empty string doesn't match empty pattern
dp[0][j] = depends on pattern (handle '*' that can match zero)
```

### Solution

```java
public class RegularExpressionMatching {
    public boolean isMatch(String s, String p) {
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        
        // Base case: empty string matches empty pattern
        dp[0][0] = true;
        
        // Handle patterns like a*, a*b*, a*b*c*
        for (int j = 2; j <= n; j++) {
            if (p.charAt(j-1) == '*') {
                dp[0][j] = dp[0][j-2];
            }
        }
        
        // Fill DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (p.charAt(j-1) == '*') {
                    // '*' matches zero occurrences
                    dp[i][j] = dp[i][j-2];
                    
                    // '*' matches one or more occurrences
                    if (matches(s, p, i, j-1)) {
                        dp[i][j] = dp[i][j] || dp[i-1][j];
                    }
                } else {
                    // Regular character or '.'
                    if (matches(s, p, i, j)) {
                        dp[i][j] = dp[i-1][j-1];
                    }
                }
            }
        }
        
        return dp[m][n];
    }
    
    private boolean matches(String s, String p, int i, int j) {
        return p.charAt(j-1) == '.' || s.charAt(i-1) == p.charAt(j-1);
    }
}
```

### Dry Run

**Input**: `s = "aab"`, `p = "c*a*b"`

```
DP Table:
       ""  c  c* a  a* b
    "" T   F  T  F  T  F
    a  F   F  F  T  T  F
    a  F   F  F  F  T  F
    b  F   F  F  F  F  T

Step-by-step:

dp[0][0] = true (empty matches empty)

dp[0][2] = true (c* can match zero 'c')
dp[0][4] = true (c*a* can match zero 'c' and zero 'a')

dp[1][3]: s[0]='a', p[2]='a'
  Match, so dp[1][3] = dp[0][2] = true

dp[1][4]: p[3]='*'
  Zero occurrences: dp[1][4] = dp[1][2] = false
  One or more: s[0]='a' matches p[2]='a'
    dp[1][4] = dp[0][4] = true

dp[2][4]: p[3]='*'
  s[1]='a' matches p[2]='a'
    dp[2][4] = dp[1][4] = true

dp[3][5]: s[2]='b', p[4]='b'
  Match, so dp[3][5] = dp[2][4] = true

Final: dp[3][5] = true
```

### Pattern Matching Examples

```
s = "aa", p = "a*"
  'a*' matches "aa" (two 'a's)
  Result: true

s = "ab", p = ".*"
  '.' matches 'a', '*' allows multiple
  '.*' matches "ab"
  Result: true

s = "mississippi", p = "mis*is*p*."
  mis matches mis
  s* matches s (one 's')
  is matches is
  s* matches s (one 's')
  ip matches ip
  p* matches p (one 'p')
  . matches i
  Result: false (extra 'i' at end)
```

### Complexity Analysis

- **Time Complexity**: O(m × n)
- **Space Complexity**: O(m × n)

### Test Cases

```java
@Test
public void testIsMatch() {
    RegularExpressionMatching solution = new RegularExpressionMatching();
    
    assertFalse(solution.isMatch("aa", "a"));
    assertTrue(solution.isMatch("aa", "a*"));
    assertTrue(solution.isMatch("ab", ".*"));
    assertTrue(solution.isMatch("aab", "c*a*b"));
    assertFalse(solution.isMatch("mississippi", "mis*is*p*."));
    assertTrue(solution.isMatch("", "a*"));
    assertTrue(solution.isMatch("", ".*"));
}
```

### Edge Cases

1. **Empty string and pattern**: Return true
2. **Empty string, non-empty pattern**: Check if pattern can match empty
3. **Pattern with only '*'**: Invalid (must have preceding character)
4. **Multiple consecutive '*'**: Handle correctly

### Common Mistakes

1. **Not handling '*' correctly**: Must check both zero and one+ occurrences
2. **Wrong base case for pattern**: Not handling patterns like "a*b*"
3. **Off-by-one errors**: Using wrong indices for s and p
4. **Not checking if preceding char matches**: For '*' case

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Edit Distance | 2D Subsequence | O(m×n) | O(m×n) or O(n) | Three operations: insert, delete, replace |
| Regex Matching | 2D Pattern | O(m×n) | O(m×n) | Handle '.' and '*' with careful logic |

---

## 🎓 Key Takeaways

### Hard Problem Characteristics

1. **Complex State Transitions**: Multiple cases to handle
2. **Pattern Matching**: Special characters with specific rules
3. **Optimization Required**: Space optimization often needed
4. **Edge Cases**: Many corner cases to consider

### Problem-Solving Strategies

1. **Define State Clearly**: What does dp[i][j] represent?
2. **Handle Base Cases**: Empty strings, special patterns
3. **Break Down Recurrence**: Consider all possible transitions
4. **Test Thoroughly**: Many edge cases in hard problems

### Edit Distance Applications

- Spell checking
- DNA sequence alignment
- Plagiarism detection
- Version control (diff)

### Regex Matching Applications

- Text search and validation
- Compiler design (lexical analysis)
- Data validation
- Pattern recognition

---

**Congratulations!** You've completed all 20 Dynamic Programming problems! 🎉

**Next Steps**:
1. Review problems you found difficult
2. Practice space optimization techniques
3. Solve variations (2D → 3D, add constraints)
4. Time yourself on random problems

**Related Topics**:
- Greedy Algorithms
- Backtracking
- Divide and Conquer
- Graph Algorithms with DP
- Bitmask DP
- Digit DP
