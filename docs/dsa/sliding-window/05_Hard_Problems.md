# Sliding Window - Hard Problems (10%)

## 📚 2 Hard Problems with Complete Solutions

---

## Problem 1: Sliding Window Maximum

**Difficulty**: Hard  
**Pattern**: Fixed Window with Deque  
**LeetCode**: #239

### Problem Statement

You are given an array of integers `nums`, and a sliding window of size `k` which is moving from the very left to the very right. You can only see the `k` numbers in the window. Return the max sliding window.

### Examples

```
Input: nums = [1,3,-1,-3,5,3,6,7], k = 3
Output: [3,3,5,5,6,7]
Explanation:
Window position                Max
---------------               -----
[1  3  -1] -3  5  3  6  7       3
 1 [3  -1  -3] 5  3  6  7       3
 1  3 [-1  -3  5] 3  6  7       5
 1  3  -1 [-3  5  3] 6  7       5
 1  3  -1  -3 [5  3  6] 7       6
 1  3  -1  -3  5 [3  6  7]      7

Input: nums = [1], k = 1
Output: [1]
```

### Intuition

**Naive Approach** (O(n × k)):
- For each window, scan all k elements to find maximum
- Time: O(n × k)

**Optimized with Deque** (O(n)):
- Use deque to maintain potential maximum candidates
- Keep deque in decreasing order
- Remove elements outside current window
- Front of deque is always the maximum

### Solution

```java
public class SlidingWindowMaximum {
    public int[] maxSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0) return new int[0];
        
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new LinkedList<>(); // Store indices
        
        for (int i = 0; i < n; i++) {
            // Remove indices outside current window
            while (!deque.isEmpty() && deque.peek() < i - k + 1) {
                deque.poll();
            }
            
            // Remove indices of elements smaller than current
            // (they can never be maximum)
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }
            
            deque.offer(i);
            
            // Add to result once we have a full window
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peek()];
            }
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `nums = [1, 3, -1, -3, 5, 3, 6, 7]`, `k = 3`

```
Initial: deque = [], result = []

Step 1: i = 0, nums[0] = 1
  deque is empty, add index 0
  deque = [0] (values: [1])
  i < k-1, don't add to result

Step 2: i = 1, nums[1] = 3
  nums[0] = 1 < nums[1] = 3, remove index 0
  deque = []
  Add index 1
  deque = [1] (values: [3])
  i < k-1, don't add to result

Step 3: i = 2, nums[2] = -1
  nums[1] = 3 > nums[2] = -1, keep index 1
  Add index 2
  deque = [1, 2] (values: [3, -1])
  i >= k-1, add nums[deque.peek()] = nums[1] = 3
  result = [3]

Step 4: i = 3, nums[3] = -3
  Check window: deque.peek() = 1, i - k + 1 = 1, valid
  nums[2] = -1 > nums[3] = -3, keep index 2
  Add index 3
  deque = [1, 2, 3] (values: [3, -1, -3])
  Add nums[1] = 3
  result = [3, 3]

Step 5: i = 4, nums[4] = 5
  Check window: deque.peek() = 1, i - k + 1 = 2
  1 < 2, remove index 1
  deque = [2, 3]
  
  nums[2] = -1 < nums[4] = 5, remove index 2
  nums[3] = -3 < nums[4] = 5, remove index 3
  deque = []
  
  Add index 4
  deque = [4] (values: [5])
  Add nums[4] = 5
  result = [3, 3, 5]

Step 6: i = 5, nums[5] = 3
  Check window: deque.peek() = 4, i - k + 1 = 3, valid
  nums[4] = 5 > nums[5] = 3, keep index 4
  Add index 5
  deque = [4, 5] (values: [5, 3])
  Add nums[4] = 5
  result = [3, 3, 5, 5]

Step 7: i = 6, nums[6] = 6
  Check window: deque.peek() = 4, i - k + 1 = 4, valid
  nums[4] = 5 < nums[6] = 6, remove index 4
  nums[5] = 3 < nums[6] = 6, remove index 5
  deque = []
  
  Add index 6
  deque = [6] (values: [6])
  Add nums[6] = 6
  result = [3, 3, 5, 5, 6]

Step 8: i = 7, nums[7] = 7
  Check window: deque.peek() = 6, i - k + 1 = 5, valid
  nums[6] = 6 < nums[7] = 7, remove index 6
  deque = []
  
  Add index 7
  deque = [7] (values: [7])
  Add nums[7] = 7
  result = [3, 3, 5, 5, 6, 7]

Final result = [3, 3, 5, 5, 6, 7]
```

### Why Deque Works

**Key Insights**:
1. **Monotonic Decreasing**: Keep deque in decreasing order
2. **Remove Smaller Elements**: If current element is larger, previous smaller elements can never be maximum
3. **Remove Outside Window**: Elements outside current window are irrelevant
4. **Front is Maximum**: Front of deque always contains index of maximum element

**Deque State Example**:
```
nums = [1, 3, -1, -3, 5]
Window [1, 3, -1]:
  - Add 1: deque = [1]
  - Add 3: 3 > 1, remove 1, deque = [3]
  - Add -1: -1 < 3, deque = [3, -1]
  - Maximum = 3 (front of deque)
```

### Complexity Analysis

- **Time Complexity**: O(n)
  - Each element added to deque once: O(n)
  - Each element removed from deque once: O(n)
  - Total: O(n)

- **Space Complexity**: O(k)
  - Deque stores at most k elements

### Test Cases

```java
@Test
public void testMaxSlidingWindow() {
    SlidingWindowMaximum solution = new SlidingWindowMaximum();
    
    // Test case 1: Basic case
    assertArrayEquals(new int[]{3, 3, 5, 5, 6, 7}, 
        solution.maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3));
    
    // Test case 2: Single element
    assertArrayEquals(new int[]{1}, 
        solution.maxSlidingWindow(new int[]{1}, 1));
    
    // Test case 3: Decreasing array
    assertArrayEquals(new int[]{3, 2, 1}, 
        solution.maxSlidingWindow(new int[]{3, 2, 1}, 1));
    
    // Test case 4: Increasing array
    assertArrayEquals(new int[]{3}, 
        solution.maxSlidingWindow(new int[]{1, 2, 3}, 3));
    
    // Test case 5: All same
    assertArrayEquals(new int[]{5, 5, 5}, 
        solution.maxSlidingWindow(new int[]{5, 5, 5, 5, 5}, 3));
    
    // Test case 6: Window size = array length
    assertArrayEquals(new int[]{5}, 
        solution.maxSlidingWindow(new int[]{1, 3, 5, 2, 4}, 5));
}
```

### Edge Cases

1. **Empty array**: Return empty array
2. **k = 1**: Each element is its own maximum
3. **k = n**: Single window, return max of entire array
4. **Decreasing array**: Each window max is leftmost element
5. **Increasing array**: Each window max is rightmost element

### Common Mistakes

1. **Using wrong data structure**: Priority queue is O(n log k), deque is O(n)
2. **Storing values instead of indices**: Need indices to check window boundaries
3. **Not maintaining monotonic property**: Deque must be decreasing
4. **Off-by-one in window check**: Use `i - k + 1` for window start

---

## Problem 2: Minimum Window Subsequence

**Difficulty**: Hard  
**Pattern**: Two Pointers with Sliding Window Concept  
**LeetCode**: #727

### Problem Statement

Given strings `s` and `t`, return the minimum window substring of `s` such that every character in `t` (including duplicates) is included in the window **in order** (subsequence).

### Examples

```
Input: s = "abcdebdde", t = "bde"
Output: "bcde"
Explanation: "bcde" is the shortest window containing "bde" as subsequence

Input: s = "jmeqksfrsdcmsiwvaovztaqenprpvnbstl", t = "u"
Output: ""
```

### Intuition

**Difference from Minimum Window Substring**:
- Substring: Characters can be in any order
- Subsequence: Characters must be in order

**Strategy**:
1. **Forward scan**: Find window containing all characters of t in order
2. **Backward scan**: Shrink window from left to minimize length
3. **Repeat**: Find all possible windows, return shortest

### Solution

```java
public class MinimumWindowSubsequence {
    public String minWindow(String s, String t) {
        int sLen = s.length();
        int tLen = t.length();
        int minLen = Integer.MAX_VALUE;
        String result = "";
        
        int sIndex = 0;
        
        while (sIndex < sLen) {
            // Step 1: Forward scan - find window with all chars of t
            int tIndex = 0;
            
            while (sIndex < sLen) {
                if (s.charAt(sIndex) == t.charAt(tIndex)) {
                    tIndex++;
                    if (tIndex == tLen) break;
                }
                sIndex++;
            }
            
            // No more windows possible
            if (sIndex == sLen) break;
            
            // Step 2: Backward scan - shrink window from left
            int end = sIndex;
            tIndex = tLen - 1;
            
            while (tIndex >= 0) {
                if (s.charAt(sIndex) == t.charAt(tIndex)) {
                    tIndex--;
                }
                sIndex--;
            }
            
            sIndex++; // Move back to start of window
            
            // Step 3: Update result if shorter
            if (end - sIndex + 1 < minLen) {
                minLen = end - sIndex + 1;
                result = s.substring(sIndex, end + 1);
            }
            
            sIndex++; // Move to next position
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `s = "abcdebdde"`, `t = "bde"`

```
s = a b c d e b d d e
    0 1 2 3 4 5 6 7 8

t = b d e
    0 1 2

=== Finding First Window ===

Forward Scan (sIndex starts at 0):
  sIndex = 0, s[0] = 'a' != t[0] = 'b', continue
  sIndex = 1, s[1] = 'b' == t[0] = 'b', tIndex = 1
  sIndex = 2, s[2] = 'c' != t[1] = 'd', continue
  sIndex = 3, s[3] = 'd' == t[1] = 'd', tIndex = 2
  sIndex = 4, s[4] = 'e' == t[2] = 'e', tIndex = 3
  Found all characters! end = 4

Backward Scan (from sIndex = 4):
  sIndex = 4, s[4] = 'e' == t[2] = 'e', tIndex = 1
  sIndex = 3, s[3] = 'd' == t[1] = 'd', tIndex = 0
  sIndex = 2, s[2] = 'c' != t[0] = 'b', continue
  sIndex = 1, s[1] = 'b' == t[0] = 'b', tIndex = -1
  Done! sIndex = 1 (after increment)

Window: s[1..4] = "bcde" (length 4)
result = "bcde", minLen = 4

=== Finding Second Window ===

Forward Scan (sIndex starts at 2):
  sIndex = 2, s[2] = 'c' != t[0] = 'b', continue
  sIndex = 3, s[3] = 'd' != t[0] = 'b', continue
  sIndex = 4, s[4] = 'e' != t[0] = 'b', continue
  sIndex = 5, s[5] = 'b' == t[0] = 'b', tIndex = 1
  sIndex = 6, s[6] = 'd' == t[1] = 'd', tIndex = 2
  sIndex = 7, s[7] = 'd' != t[2] = 'e', continue
  sIndex = 8, s[8] = 'e' == t[2] = 'e', tIndex = 3
  Found all characters! end = 8

Backward Scan (from sIndex = 8):
  sIndex = 8, s[8] = 'e' == t[2] = 'e', tIndex = 1
  sIndex = 7, s[7] = 'd' == t[1] = 'd', tIndex = 0
  sIndex = 6, s[6] = 'd' != t[0] = 'b', continue
  sIndex = 5, s[5] = 'b' == t[0] = 'b', tIndex = -1
  Done! sIndex = 5 (after increment)

Window: s[5..8] = "bdde" (length 4)
Not shorter than "bcde", don't update

No more characters, done.

Final result = "bcde"
```

### Algorithm Steps

1. **Forward Scan**:
   - Move right pointer to find all characters of t in order
   - Track position where last character found

2. **Backward Scan**:
   - Move left pointer backwards to find start of window
   - Shrink window to minimum size while maintaining subsequence

3. **Update Result**:
   - If current window shorter, update result
   - Move to next position and repeat

4. **Optimization**:
   - Start next search from position after current window start
   - Avoids redundant searches

### Complexity Analysis

- **Time Complexity**: O(s × t)
  - Worst case: scan entire s for each character in t
  - Each position visited at most twice (forward + backward)

- **Space Complexity**: O(1)
  - Only using pointers

### Test Cases

```java
@Test
public void testMinWindow() {
    MinimumWindowSubsequence solution = new MinimumWindowSubsequence();
    
    // Test case 1: Basic case
    assertEquals("bcde", solution.minWindow("abcdebdde", "bde"));
    
    // Test case 2: No such window
    assertEquals("", solution.minWindow("jmeqksfrsdcmsiwvaovztaqenprpvnbstl", "u"));
    
    // Test case 3: Multiple windows
    assertEquals("abc", solution.minWindow("abcabc", "abc"));
    
    // Test case 4: Single character
    assertEquals("a", solution.minWindow("abcde", "a"));
    
    // Test case 5: t equals s
    assertEquals("abc", solution.minWindow("abc", "abc"));
    
    // Test case 6: Window at end
    assertEquals("bde", solution.minWindow("abcbde", "bde"));
    
    // Test case 7: Overlapping windows
    assertEquals("abbc", solution.minWindow("aabbbccc", "abc"));
    
    // Test case 8: Long string
    assertEquals("fksf", solution.minWindow("jmeqksfrsdcmsiwvaovztaqenprpvnbstl", "ksf"));
}
```

### Edge Cases

1. **Empty strings**: Return ""
2. **t longer than s**: Return ""
3. **No valid window**: Return ""
4. **Multiple valid windows**: Return shortest
5. **Window at start/end**: Handle correctly
6. **Single character in t**: Find first occurrence

### Comparison Table

| Aspect | Minimum Window Substring | Minimum Window Subsequence |
|--------|-------------------------|---------------------------|
| **Order** | Not required | Required (subsequence) |
| **Characters** | All in window | Must appear in order |
| **Approach** | Sliding window + hash map | Two pointers + greedy |
| **Complexity** | O(s + t) | O(s × t) |
| **Difficulty** | Medium/Hard | Hard |

### Common Mistakes

1. **Confusing with substring**: Order matters in subsequence
2. **Not shrinking window**: Missing optimization opportunity
3. **Wrong pointer movement**: Must track both forward and backward
4. **Not handling edge cases**: Empty strings, no solution
5. **Starting next search wrong**: Should start after current window start

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Sliding Window Maximum | Fixed + Deque | O(n) | O(k) | Monotonic deque |
| Minimum Window Subsequence | Two Pointers + Greedy | O(s×t) | O(1) | Forward + backward scan |

---

## 🎓 Key Takeaways

### Hard Problem Characteristics

1. **Advanced Data Structures**: Deque for maintaining order
2. **Multiple Passes**: Forward + backward scans
3. **Order Constraints**: Subsequence vs substring
4. **Optimization**: Greedy approaches for efficiency

### Problem-Solving Strategies

1. **Identify Pattern**: Fixed vs variable window
2. **Choose Data Structure**: Deque for min/max, hash map for frequency
3. **Optimize Incrementally**: Start simple, then optimize
4. **Handle Edge Cases**: Empty inputs, no solution, extreme values

### Deque Pattern (Sliding Window Maximum)

**When to Use**:
- Need min/max in sliding window
- Window slides continuously
- O(n) time required

**How it Works**:
1. Maintain monotonic deque (increasing or decreasing)
2. Remove elements outside window
3. Remove elements that can't be answer
4. Front of deque is always the answer

### Two Pointers Pattern (Minimum Window Subsequence)

**When to Use**:
- Order matters (subsequence)
- Need to find optimal window
- Can use greedy approach

**How it Works**:
1. Forward scan to find valid window
2. Backward scan to minimize window
3. Repeat to find all windows
4. Return optimal result

---

## 💡 Practice Tips

1. **Master Deque**: Understand monotonic deque pattern
2. **Visualize**: Draw deque state at each step
3. **Edge Cases**: Test with empty, single element, all same
4. **Time Yourself**: Practice under interview conditions
5. **Explain Out Loud**: Articulate your thought process

---

**Congratulations!** You've completed all 20 Sliding Window problems! 🎉

**Next Steps**:
1. Review problems you found difficult
2. Solve variations and similar problems
3. Practice explaining solutions
4. Combine with other techniques (DP, Binary Search)

**Related Topics**:
- Two Pointers (closely related)
- Monotonic Stack/Queue
- Binary Search with Sliding Window
- Dynamic Programming with Sliding Window
