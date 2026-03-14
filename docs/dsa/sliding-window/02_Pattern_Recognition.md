# Sliding Window - Pattern Recognition Guide

## 🎯 How to Identify Sliding Window Problems

This guide helps you recognize when to use the Sliding Window technique by analyzing problem characteristics.

## 🔍 Recognition Checklist

### ✅ Strong Indicators

1. **Problem mentions "subarray" or "substring"**
   - "Find the longest substring..."
   - "Maximum sum of subarray..."
   - "Minimum window substring..."
   
2. **Contiguous elements required**
   - Elements must be consecutive
   - No gaps allowed in the sequence
   
3. **Fixed or variable window size**
   - "Of size K" → Fixed window
   - "Longest/shortest" → Variable window
   
4. **Optimization keywords**
   - "Maximum", "Minimum", "Longest", "Shortest"
   - "Optimal", "Best"
   
5. **Constraint-based**
   - "At most K distinct characters"
   - "Without repeating characters"
   - "Sum equals K"

### ⚠️ Moderate Indicators

1. **Sequential processing**
   - Process elements in order
   - Can't skip elements
   
2. **Aggregate operations**
   - Sum, average, product
   - Count, frequency
   
3. **Pattern matching**
   - Anagrams, permutations
   - Character frequency matching

## 📋 Decision Tree

```
Does the problem involve contiguous elements (subarray/substring)?
│
├─ YES ──→ Is the window size fixed (given K)?
│          │
│          ├─ YES ──→ Use FIXED WINDOW pattern
│          │         Examples: Max sum of size K, Average of subarrays
│          │
│          └─ NO ──→ Is it about finding longest/shortest/optimal?
│                    │
│                    ├─ YES ──→ Use VARIABLE WINDOW pattern
│                    │         Examples: Longest substring, Minimum window
│                    │
│                    └─ NO ──→ Does it need frequency/count tracking?
│                              │
│                              └─ YES ──→ Use WINDOW + HASH MAP pattern
│
└─ NO ──→ Not a sliding window problem
          Consider: Two Pointers, DP, or other approaches
```

## 🎨 Pattern Matching Examples

### Pattern 1: Fixed Window

**Keywords**: "of size K", "exactly K elements", "window of length K"

**Example Problems**:
```
✓ "Maximum sum of subarray of size K"
✓ "Average of all subarrays of size K"
✓ "Maximum of all subarrays of size K"
✓ "Contains duplicate within K distance"
```

**Recognition**:
- Window size is explicitly given
- Need to process all windows of that size
- Simple sliding: remove left, add right

**Template Match**:
```java
// Build first window
for (int i = 0; i < k; i++) {
    // Add arr[i] to window
}

// Slide window
for (int i = k; i < n; i++) {
    // Remove arr[i-k], add arr[i]
    // Update result
}
```

### Pattern 2: Variable Window (Expand/Contract)

**Keywords**: longest, shortest, minimum, maximum, optimal, at most, at least

**Example Problems**:
```
✓ "Longest substring without repeating characters"
✓ "Minimum window substring containing all characters"
✓ "Longest substring with at most K distinct characters"
✓ "Shortest subarray with sum at least K"
```

**Recognition**:
- Window size changes dynamically
- Need to find optimal window
- Expand when valid, contract when invalid

**Template Match**:
```java
int left = 0;
for (int right = 0; right < n; right++) {
    // Expand: add arr[right]
    
    while (windowInvalid) {
        // Contract: remove arr[left]
        left++;
    }
    
    // Update result with current window
}
```

### Pattern 3: Window with Hash Map/Set

**Keywords**: distinct, unique, frequency, anagram, permutation, contains all

**Example Problems**:
```
✓ "Find all anagrams in string"
✓ "Permutation in string"
✓ "Longest substring with K distinct characters"
✓ "Minimum window containing all characters of another string"
```

**Recognition**:
- Need to track character/element frequencies
- Check if window satisfies frequency constraints
- Use hash map to maintain counts

**Template Match**:
```java
Map<Character, Integer> map = new HashMap<>();
int left = 0;

for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    map.put(c, map.getOrDefault(c, 0) + 1);
    
    while (needsContraction) {
        char leftChar = s.charAt(left);
        map.put(leftChar, map.get(leftChar) - 1);
        if (map.get(leftChar) == 0) map.remove(leftChar);
        left++;
    }
    
    // Check if window is valid
}
```

### Pattern 4: Window with Deque (Min/Max)

**Keywords**: maximum/minimum in window, sliding window maximum

**Example Problems**:
```
✓ "Sliding window maximum"
✓ "Sliding window minimum"
✓ "Maximum of all subarrays of size K"
```

**Recognition**:
- Need to find max/min in each window
- Window slides continuously
- Use deque to maintain candidates

**Template Match**:
```java
Deque<Integer> deque = new LinkedList<>();

for (int i = 0; i < n; i++) {
    // Remove elements outside window
    while (!deque.isEmpty() && deque.peek() < i - k + 1) {
        deque.poll();
    }
    
    // Remove elements smaller than current (for max)
    while (!deque.isEmpty() && arr[deque.peekLast()] < arr[i]) {
        deque.pollLast();
    }
    
    deque.offer(i);
    
    if (i >= k - 1) {
        // arr[deque.peek()] is the maximum
    }
}
```

## 🔑 Key Questions to Ask

### Question 1: Are elements contiguous?

**YES** → Strong candidate for sliding window

**NO** → Consider two pointers or other approaches

### Question 2: Is window size fixed or variable?

**Fixed (given K)** → Fixed window pattern  
**Variable (find optimal)** → Variable window pattern  

### Question 3: What needs to be tracked?

**Simple aggregate (sum, count)** → Use variables  
**Frequencies** → Use hash map  
**Min/Max in window** → Use deque  

### Question 4: What's the optimization goal?

**Maximum/Minimum value** → Track best result  
**Longest/Shortest window** → Track window size  
**Count of valid windows** → Increment counter  

## 📊 Problem Type Matrix

| Problem Type | Pattern | Data Structure | Example |
|--------------|---------|----------------|---------|
| Max sum of size K | Fixed | Variables | Sum of K elements |
| Longest substring | Variable | Hash Set | No repeating chars |
| Anagram search | Fixed + Map | Hash Map | Find all anagrams |
| Min window | Variable + Map | Hash Map | Contains all chars |
| Sliding max | Fixed + Deque | Deque | Max in each window |
| K distinct chars | Variable + Map | Hash Map | At most K distinct |
| Subarray sum = K | Variable | Variables | Exact sum |

## 🎯 Common Problem Phrases

### Fixed Window Triggers

- "of size K"
- "exactly K elements"
- "window of length K"
- "every K consecutive elements"
- "all subarrays of size K"

### Variable Window Triggers

- "longest substring"
- "shortest subarray"
- "minimum window"
- "maximum length"
- "optimal window"
- "at most K"
- "at least K"

### Hash Map Triggers

- "distinct characters"
- "unique elements"
- "frequency"
- "anagram"
- "permutation"
- "contains all"
- "character count"

### Deque Triggers

- "maximum in window"
- "minimum in window"
- "sliding window maximum"
- "max of all subarrays"

## 🚫 When NOT to Use Sliding Window

### 1. Non-contiguous Elements
```
Problem: "Find two numbers that sum to target" (can be anywhere)
Solution: Use hash map or two pointers
```

### 2. Need All Subarrays
```
Problem: "Count all subarrays with sum divisible by K"
Solution: Use prefix sum + hash map
```

### 3. Complex Dependencies
```
Problem: "Longest increasing subsequence" (not contiguous)
Solution: Use dynamic programming
```

### 4. Global Optimization
```
Problem: "Maximum subarray sum" (Kadane's algorithm is better)
Solution: Use dynamic programming
```

## 💡 Conversion Examples

### Example 1: Brute Force → Fixed Window

**Problem**: Maximum sum of subarray of size K

**Brute Force** (O(n × k)):
```java
int maxSum = Integer.MIN_VALUE;
for (int i = 0; i <= n - k; i++) {
    int sum = 0;
    for (int j = i; j < i + k; j++) {
        sum += arr[j];
    }
    maxSum = Math.max(maxSum, sum);
}
```

**Sliding Window** (O(n)):
```java
int sum = 0;
for (int i = 0; i < k; i++) sum += arr[i];
int maxSum = sum;

for (int i = k; i < n; i++) {
    sum += arr[i] - arr[i - k];
    maxSum = Math.max(maxSum, sum);
}
```

### Example 2: Nested Loops → Variable Window

**Problem**: Longest substring without repeating characters

**Brute Force** (O(n³)):
```java
int maxLen = 0;
for (int i = 0; i < n; i++) {
    for (int j = i; j < n; j++) {
        if (allUnique(s, i, j)) {
            maxLen = Math.max(maxLen, j - i + 1);
        }
    }
}
```

**Sliding Window** (O(n)):
```java
Set<Character> set = new HashSet<>();
int left = 0, maxLen = 0;

for (int right = 0; right < n; right++) {
    while (set.contains(s.charAt(right))) {
        set.remove(s.charAt(left++));
    }
    set.add(s.charAt(right));
    maxLen = Math.max(maxLen, right - left + 1);
}
```

## 🎓 Practice Strategy

### Level 1: Fixed Window Mastery
1. Start with simple sum/average problems
2. Understand the sliding mechanism
3. Practice with different aggregates (max, min, product)

### Level 2: Variable Window
1. Learn expand/contract logic
2. Practice with different conditions
3. Handle edge cases (empty window, no valid window)

### Level 3: Advanced Patterns
1. Add hash map for frequency tracking
2. Use deque for min/max in window
3. Combine multiple techniques

## 📝 Quick Recognition Card

```
┌─────────────────────────────────────────────────────────┐
│ SLIDING WINDOW PATTERN RECOGNITION                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ FIXED WINDOW:                                           │
│   ✓ "of size K"                                        │
│   ✓ "exactly K elements"                               │
│   ✓ "all subarrays of size K"                          │
│   → Use: Build first window, then slide                │
│                                                         │
│ VARIABLE WINDOW:                                        │
│   ✓ "longest/shortest"                                 │
│   ✓ "minimum/maximum"                                  │
│   ✓ "at most/at least K"                               │
│   → Use: Expand right, contract left                   │
│                                                         │
│ WITH HASH MAP:                                          │
│   ✓ "distinct/unique"                                  │
│   ✓ "frequency/count"                                  │
│   ✓ "anagram/permutation"                              │
│   → Use: Track frequencies in window                   │
│                                                         │
│ WITH DEQUE:                                             │
│   ✓ "maximum/minimum in window"                        │
│   ✓ "sliding window maximum"                           │
│   → Use: Maintain monotonic deque                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 🔍 Problem Analysis Framework

### Step 1: Identify Contiguity
- Does the problem require consecutive elements?
- Is it about subarray or substring?

### Step 2: Determine Window Type
- Is window size given? → Fixed
- Need to find optimal? → Variable

### Step 3: Choose Data Structure
- Simple aggregate? → Variables
- Frequency tracking? → Hash Map
- Min/Max in window? → Deque

### Step 4: Define Window Operations
- What to do when expanding?
- What to do when contracting?
- When to update result?

## 🎯 Common Variations

### Variation 1: Exact Match
```
"Subarray with sum exactly K"
→ Variable window with sum tracking
```

### Variation 2: At Most K
```
"Longest substring with at most K distinct characters"
→ Variable window with hash map, contract when > K
```

### Variation 3: At Least K
```
"Shortest subarray with sum at least K"
→ Variable window, contract when sum >= K
```

### Variation 4: Count Windows
```
"Count subarrays with sum less than K"
→ Variable window, count valid windows
```

## 📈 Complexity Patterns

| Pattern | Time | Space | Why? |
|---------|------|-------|------|
| Fixed Window | O(n) | O(1) | Each element added/removed once |
| Variable Window | O(n) | O(1) | Left and right move at most n times |
| With Hash Map | O(n) | O(k) | k distinct elements in window |
| With Deque | O(n) | O(k) | Deque size ≤ window size |

**Key Insight**: Even with nested while loop, time is O(n) because each element is added once and removed once (amortized analysis).

---

**Next**: [Easy Problems](03_Easy_Problems.md)
