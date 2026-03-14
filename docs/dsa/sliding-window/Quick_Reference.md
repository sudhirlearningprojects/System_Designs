# Sliding Window - Quick Reference Cheat Sheet

## 🎯 Three Main Patterns

### 1. Fixed Window
```java
int windowSum = 0;
// Build first window
for (int i = 0; i < k; i++) {
    windowSum += arr[i];
}
int result = windowSum;

// Slide window
for (int i = k; i < n; i++) {
    windowSum = windowSum - arr[i - k] + arr[i];
    result = Math.max(result, windowSum);
}
```
**Use for**: Window size is given (K)

### 2. Variable Window
```java
int left = 0;
for (int right = 0; right < n; right++) {
    // Expand: add arr[right]
    
    while (windowInvalid) {
        // Contract: remove arr[left]
        left++;
    }
    
    // Update result
}
```
**Use for**: Find longest/shortest/optimal window

### 3. Window with Deque (Min/Max)
```java
Deque<Integer> deque = new LinkedList<>();

for (int i = 0; i < n; i++) {
    // Remove outside window
    while (!deque.isEmpty() && deque.peek() < i - k + 1) {
        deque.poll();
    }
    
    // Remove smaller elements (for max)
    while (!deque.isEmpty() && arr[deque.peekLast()] < arr[i]) {
        deque.pollLast();
    }
    
    deque.offer(i);
    
    if (i >= k - 1) {
        // arr[deque.peek()] is maximum
    }
}
```
**Use for**: Sliding window maximum/minimum

## 📋 Problem Recognition

| Keywords | Pattern | Example |
|----------|---------|---------|
| "of size K" | Fixed | Max sum of size K |
| "longest substring" | Variable | No repeating chars |
| "shortest subarray" | Variable | Sum >= K |
| "at most K" | Variable + Map | K distinct chars |
| "maximum in window" | Fixed + Deque | Sliding window max |
| "anagram" | Fixed + Map | Find all anagrams |
| "permutation" | Fixed + Map | Permutation in string |

## 🔑 Common Templates

### Fixed Window - Sum/Average
```java
int sum = 0;
for (int i = 0; i < k; i++) sum += arr[i];
int maxSum = sum;

for (int i = k; i < n; i++) {
    sum += arr[i] - arr[i - k];
    maxSum = Math.max(maxSum, sum);
}
```

### Variable Window - Longest
```java
int left = 0, maxLen = 0;
Set<Character> set = new HashSet<>();

for (int right = 0; right < s.length(); right++) {
    while (set.contains(s.charAt(right))) {
        set.remove(s.charAt(left++));
    }
    set.add(s.charAt(right));
    maxLen = Math.max(maxLen, right - left + 1);
}
```

### Variable Window - Shortest
```java
int left = 0, minLen = Integer.MAX_VALUE;
int sum = 0;

for (int right = 0; right < n; right++) {
    sum += arr[right];
    
    while (sum >= target) {
        minLen = Math.min(minLen, right - left + 1);
        sum -= arr[left++];
    }
}
```

### Window with Hash Map - Frequency
```java
Map<Character, Integer> map = new HashMap<>();
int left = 0;

for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    map.put(c, map.getOrDefault(c, 0) + 1);
    
    while (map.size() > k) {
        char leftChar = s.charAt(left);
        map.put(leftChar, map.get(leftChar) - 1);
        if (map.get(leftChar) == 0) map.remove(leftChar);
        left++;
    }
}
```

### Window with Deque - Maximum
```java
Deque<Integer> deque = new LinkedList<>();
int[] result = new int[n - k + 1];

for (int i = 0; i < n; i++) {
    while (!deque.isEmpty() && deque.peek() < i - k + 1) {
        deque.poll();
    }
    
    while (!deque.isEmpty() && arr[deque.peekLast()] < arr[i]) {
        deque.pollLast();
    }
    
    deque.offer(i);
    
    if (i >= k - 1) {
        result[i - k + 1] = arr[deque.peek()];
    }
}
```

## ⚡ Time Complexities

| Pattern | Time | Space | Notes |
|---------|------|-------|-------|
| Fixed Window | O(n) | O(1) | Each element processed once |
| Variable Window | O(n) | O(1) or O(k) | Amortized O(n) |
| With Hash Map | O(n) | O(k) | k = distinct elements |
| With Deque | O(n) | O(k) | k = window size |

## 🚫 Common Mistakes

1. **Off-by-one in window size**
   ```java
   // ❌ Wrong
   for (int i = k; i <= n; i++)
   
   // ✅ Correct
   for (int i = k; i < n; i++)
   ```

2. **Not building first window**
   ```java
   // ❌ Wrong - starts from index k
   for (int i = k; i < n; i++) {
       sum += arr[i] - arr[i - k];
   }
   
   // ✅ Correct - build first window
   for (int i = 0; i < k; i++) sum += arr[i];
   for (int i = k; i < n; i++) {
       sum += arr[i] - arr[i - k];
   }
   ```

3. **Wrong window size calculation**
   ```java
   // ❌ Wrong
   int size = right - left;
   
   // ✅ Correct
   int size = right - left + 1;
   ```

4. **Not removing from hash map**
   ```java
   // ❌ Wrong - leaves 0 counts
   map.put(c, map.get(c) - 1);
   
   // ✅ Correct - remove when 0
   map.put(c, map.get(c) - 1);
   if (map.get(c) == 0) map.remove(c);
   ```

## 📊 All 20 Problems Summary

### Easy (8 problems)
1. Max Sum Subarray Size K - O(n)
2. Average of Subarrays - O(n)
3. Contains Duplicate II - O(n)
4. Max Average Subarray - O(n)
5. Min Size Subarray Sum - O(n)
6. Find All Anagrams - O(n)
7. Longest Nice Substring - O(n³)
8. Defang IP Address - O(n)

### Medium (10 problems)
1. Longest Substring No Repeat - O(n)
2. Character Replacement - O(n)
3. Permutation in String - O(n)
4. Fruit Into Baskets - O(n)
5. K Distinct Characters - O(n)
6. Max Consecutive Ones III - O(n)
7. Subarray Product Less K - O(n)
8. Minimum Window Substring - O(n)
9. Grumpy Bookstore - O(n)
10. Equal Substrings Budget - O(n)

### Hard (2 problems)
1. Sliding Window Maximum - O(n)
2. Minimum Window Subsequence - O(s×t)

## 🎓 Study Plan

### Week 1: Fixed Window
- Day 1-2: Theory and templates
- Day 3-4: Easy problems 1-4
- Day 5-6: Practice and variations
- Day 7: Review

### Week 2: Variable Window
- Day 1-2: Variable window concept
- Day 3-4: Medium problems 1-5
- Day 5-6: Medium problems 6-10
- Day 7: Review

### Week 3: Advanced
- Day 1-3: Deque pattern (Hard problem 1)
- Day 4-6: Subsequence pattern (Hard problem 2)
- Day 7: Mixed practice

## 💡 Interview Tips

1. **Clarify Window Type**
   - Is window size fixed or variable?
   - What defines a valid window?

2. **Choose Right Data Structure**
   - Simple aggregate? → Variables
   - Frequency? → Hash Map
   - Min/Max? → Deque

3. **Explain Approach**
   - "I'll use sliding window because..."
   - "Window expands when..."
   - "Window contracts when..."

4. **Handle Edge Cases**
   - Empty array
   - Window larger than array
   - No valid window
   - All elements same

5. **Analyze Complexity**
   - Time: Usually O(n)
   - Space: O(1) or O(k)
   - Explain amortized analysis

## 🔗 Related Patterns

- **Two Pointers**: Sliding window is specialized two pointers
- **Monotonic Stack/Queue**: Used in deque pattern
- **Prefix Sum**: Alternative for some fixed window problems
- **Binary Search**: Can combine for optimization

## 📚 Pattern Decision Tree

```
Is it about contiguous elements?
│
├─ YES ──→ Is window size fixed?
│          │
│          ├─ YES ──→ FIXED WINDOW
│          │         - Build first window
│          │         - Slide by removing left, adding right
│          │
│          └─ NO ──→ Need to find optimal?
│                    │
│                    ├─ Longest ──→ VARIABLE WINDOW (expand/contract)
│                    │              - Expand right
│                    │              - Contract left when invalid
│                    │
│                    └─ Shortest ──→ VARIABLE WINDOW (contract when valid)
│                                   - Expand right
│                                   - Contract left when valid
│
└─ NO ──→ Not sliding window
          Try: Two Pointers, DP, etc.
```

## ✅ Mastery Checklist

- [ ] Understand all three patterns
- [ ] Can identify pattern from problem
- [ ] Solved all 8 easy problems
- [ ] Solved all 10 medium problems
- [ ] Solved both hard problems
- [ ] Can explain deque pattern
- [ ] Handle edge cases consistently
- [ ] Optimize time/space complexity
- [ ] Code without syntax errors
- [ ] Complete in time limit

## 🎯 Key Formulas

**Window Size**: `right - left + 1`

**Fixed Window Slide**: `sum = sum - arr[left] + arr[right]`

**Amortized Analysis**: Each element added once, removed once = O(n)

**Deque Invariant**: Maintain monotonic (increasing/decreasing) order

**Hash Map Update**: 
```java
map.put(c, map.get(c) - 1);
if (map.get(c) == 0) map.remove(c);
```

## 🔍 Quick Problem Solver

**Given**: Array/String, need contiguous subarray/substring

**Ask**:
1. Fixed or variable window?
2. What to track? (sum, count, frequency)
3. When to expand/contract?
4. What's the result? (max, min, count)

**Choose**:
- Fixed size K → Fixed window template
- Longest/shortest → Variable window template
- Frequency → Add hash map
- Min/Max in window → Add deque

---

**Keep this cheat sheet handy during practice and interviews!**
