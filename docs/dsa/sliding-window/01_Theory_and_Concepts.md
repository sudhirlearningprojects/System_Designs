# Sliding Window Approach - Theory and Concepts

## 📖 What is Sliding Window?

The **Sliding Window** technique is an algorithmic pattern that maintains a window (a contiguous subarray or substring) that "slides" through the data structure to solve problems efficiently.

Instead of recalculating from scratch for each position, we maintain a window and update it incrementally as it moves.

## 🎯 Core Concept

```
Array: [1, 2, 3, 4, 5, 6, 7, 8]
Window size K = 3

Window 1: [1, 2, 3] 4, 5, 6, 7, 8
Window 2: 1, [2, 3, 4] 5, 6, 7, 8
Window 3: 1, 2, [3, 4, 5] 6, 7, 8
Window 4: 1, 2, 3, [4, 5, 6] 7, 8
...

Key Insight: When sliding from Window 1 to Window 2:
- Remove 1 (left element)
- Add 4 (right element)
- No need to recalculate entire sum!
```

## 🔍 Three Main Patterns

### 1. Fixed Window Size

**Pattern**: Window size is constant (K)

```
[1, 2, 3, 4, 5, 6, 7, 8]
 |--K--|
 
Slide by 1:
[1, 2, 3, 4, 5, 6, 7, 8]
    |--K--|
```

**Use Cases**:
- Maximum sum of subarray of size K
- Average of subarrays of size K
- Maximum of all subarrays of size K
- Contains duplicate within K distance

**Template**:
```java
int windowSum = 0;
int maxSum = Integer.MIN_VALUE;

// Build first window
for (int i = 0; i < k; i++) {
    windowSum += arr[i];
}
maxSum = windowSum;

// Slide the window
for (int i = k; i < arr.length; i++) {
    windowSum = windowSum - arr[i - k] + arr[i]; // Remove left, add right
    maxSum = Math.max(maxSum, windowSum);
}
```

### 2. Variable Window Size (Dynamic)

**Pattern**: Window size changes based on condition

```
[1, 2, 3, 4, 5, 6, 7, 8]
 |--|                      Window expands
 
[1, 2, 3, 4, 5, 6, 7, 8]
 |--------|                Window expands more
 
[1, 2, 3, 4, 5, 6, 7, 8]
    |-----|                Window contracts from left
```

**Use Cases**:
- Longest substring without repeating characters
- Minimum window substring
- Longest substring with at most K distinct characters
- Subarray with sum equals K

**Template**:
```java
int left = 0;
int maxLength = 0;

for (int right = 0; right < arr.length; right++) {
    // Expand window by including arr[right]
    addToWindow(arr[right]);
    
    // Contract window while condition is violated
    while (windowConditionViolated()) {
        removeFromWindow(arr[left]);
        left++;
    }
    
    // Update result with current window size
    maxLength = Math.max(maxLength, right - left + 1);
}
```

### 3. Dynamic Window with Auxiliary Data Structure

**Pattern**: Use hash map, set, or deque to track window state

```
Window: [a, b, c]
HashMap: {a: 1, b: 1, c: 1}

Slide to: [b, c, d]
HashMap: {b: 1, c: 1, d: 1}  // Remove 'a', add 'd'
```

**Use Cases**:
- Find all anagrams in string
- Minimum window containing all characters
- Longest substring with K distinct characters
- Sliding window maximum (using deque)

**Template**:
```java
Map<Character, Integer> windowMap = new HashMap<>();
int left = 0;

for (int right = 0; right < s.length(); right++) {
    char c = s.charAt(right);
    windowMap.put(c, windowMap.getOrDefault(c, 0) + 1);
    
    while (needsContraction()) {
        char leftChar = s.charAt(left);
        windowMap.put(leftChar, windowMap.get(leftChar) - 1);
        if (windowMap.get(leftChar) == 0) {
            windowMap.remove(leftChar);
        }
        left++;
    }
    
    // Update result
}
```

## 💡 Why Sliding Window Works

### Time Complexity Improvement

**Brute Force (Nested Loops)**:
```java
// O(n * k) or O(n²) - Recalculate for each window
for (int i = 0; i <= n - k; i++) {
    int sum = 0;
    for (int j = i; j < i + k; j++) {
        sum += arr[j];
    }
    maxSum = Math.max(maxSum, sum);
}
```

**Sliding Window**:
```java
// O(n) - Each element added and removed once
int sum = 0;
for (int i = 0; i < k; i++) sum += arr[i];
maxSum = sum;

for (int i = k; i < n; i++) {
    sum = sum - arr[i - k] + arr[i]; // O(1) update
    maxSum = Math.max(maxSum, sum);
}
```

### Space Complexity

- **Fixed Window**: O(1) - Only track window sum/state
- **Variable Window**: O(1) or O(k) - Depends on auxiliary structure
- **With Hash Map**: O(k) - Store at most k elements

## 🎨 Visual Example: Maximum Sum Subarray of Size K

**Problem**: Find maximum sum of any subarray of size K = 3

```
Array: [2, 1, 5, 1, 3, 2]
K = 3

Step 1: Initial window [2, 1, 5]
[2, 1, 5] 1, 3, 2
 |-----|
sum = 2 + 1 + 5 = 8
maxSum = 8

Step 2: Slide window right
2, [1, 5, 1] 3, 2
    |-----|
Remove 2, Add 1
sum = 8 - 2 + 1 = 7
maxSum = 8 (no change)

Step 3: Slide window right
2, 1, [5, 1, 3] 2
       |-----|
Remove 1, Add 3
sum = 7 - 1 + 3 = 9
maxSum = 9 ✓

Step 4: Slide window right
2, 1, 5, [1, 3, 2]
          |-----|
Remove 5, Add 2
sum = 9 - 5 + 2 = 6
maxSum = 9 (no change)

Final Answer: 9
```

## 🔑 Key Characteristics

### When Sliding Window is Applicable

1. **Contiguous Elements**
   - Problem asks about subarray or substring
   - Elements must be consecutive

2. **Optimization Opportunity**
   - Brute force involves nested loops
   - Recalculating same values repeatedly

3. **Window Definition**
   - Clear criteria for what's in the window
   - Can track window state efficiently

4. **Monotonic Operation**
   - Adding/removing elements has predictable effect
   - Can update state incrementally

### Advantages

✅ **Efficient**: O(n) time complexity  
✅ **Simple**: Easy to understand and implement  
✅ **Versatile**: Works for many problem types  
✅ **Optimal**: Often the best possible solution  

### Limitations

❌ **Contiguous only**: Doesn't work for non-contiguous elements  
❌ **State tracking**: May need auxiliary data structures  
❌ **Not always obvious**: Requires practice to recognize  

## 📊 Complexity Analysis

| Pattern | Time Complexity | Space Complexity | Notes |
|---------|----------------|------------------|-------|
| Fixed Window | O(n) | O(1) | Each element visited once |
| Variable Window | O(n) | O(1) or O(k) | Left and right pointers move at most n times |
| With Hash Map | O(n) | O(k) | k = number of distinct elements in window |
| With Deque | O(n) | O(k) | k = window size |

## 🎯 Problem-Solving Framework

### Step 1: Identify Window Type
- **Fixed size**: Problem mentions specific size K
- **Variable size**: Find longest/shortest/optimal window
- **With constraints**: Need to track specific conditions

### Step 2: Define Window State
- What do we need to track? (sum, count, frequency)
- What data structure? (variables, hash map, set, deque)

### Step 3: Expansion Logic
- When to expand window? (always for fixed, conditionally for variable)
- What to do when adding element?

### Step 4: Contraction Logic
- When to contract window? (when condition violated)
- What to do when removing element?

### Step 5: Update Result
- When to update answer? (after each slide, when valid window found)
- What to track? (maximum, minimum, count)

## 🔍 Common Patterns and Tricks

### Pattern 1: Fixed Window with Sum
```java
// Track sum of window
int sum = 0;
for (int i = 0; i < k; i++) sum += arr[i];

for (int i = k; i < n; i++) {
    sum += arr[i] - arr[i - k];
    // Use sum
}
```

### Pattern 2: Variable Window with Hash Map
```java
// Track character frequency
Map<Character, Integer> map = new HashMap<>();
int left = 0;

for (int right = 0; right < s.length(); right++) {
    map.put(s.charAt(right), map.getOrDefault(s.charAt(right), 0) + 1);
    
    while (map.size() > k) { // More than k distinct
        char c = s.charAt(left);
        map.put(c, map.get(c) - 1);
        if (map.get(c) == 0) map.remove(c);
        left++;
    }
}
```

### Pattern 3: Window with Deque (for min/max)
```java
// Track maximum in window using deque
Deque<Integer> deque = new LinkedList<>();

for (int i = 0; i < arr.length; i++) {
    // Remove elements outside window
    while (!deque.isEmpty() && deque.peek() < i - k + 1) {
        deque.poll();
    }
    
    // Remove smaller elements (not useful)
    while (!deque.isEmpty() && arr[deque.peekLast()] < arr[i]) {
        deque.pollLast();
    }
    
    deque.offer(i);
    
    if (i >= k - 1) {
        // arr[deque.peek()] is maximum
    }
}
```

## 🚫 Common Mistakes to Avoid

1. **Off-by-one errors**
   ```java
   // Wrong: window size is k+1
   for (int i = k; i <= n; i++)
   
   // Correct: window size is k
   for (int i = k; i < n; i++)
   ```

2. **Not handling first window**
   ```java
   // Wrong: missing initial window
   for (int i = k; i < n; i++) {
       sum += arr[i] - arr[i - k];
   }
   
   // Correct: build first window
   for (int i = 0; i < k; i++) sum += arr[i];
   for (int i = k; i < n; i++) {
       sum += arr[i] - arr[i - k];
   }
   ```

3. **Forgetting to update window state**
   ```java
   // Wrong: forgot to remove left element
   for (int right = 0; right < n; right++) {
       add(arr[right]);
       while (invalid) {
           left++; // Forgot to remove arr[left]!
       }
   }
   ```

4. **Wrong window size calculation**
   ```java
   // Wrong
   int size = right - left; // Off by one
   
   // Correct
   int size = right - left + 1;
   ```

## 📚 Related Techniques

- **Two Pointers**: Sliding window is a special case
- **Prefix Sum**: Can optimize some window problems
- **Monotonic Queue**: For sliding window maximum/minimum
- **Hash Map**: For tracking frequencies in window

## 🎓 Learning Path

1. **Master fixed window**: Start with simple sum/average problems
2. **Learn variable window**: Practice expanding/contracting logic
3. **Add data structures**: Hash map for frequencies, deque for min/max
4. **Solve variations**: Different constraints and conditions
5. **Optimize solutions**: Convert O(n²) to O(n)

## 🔄 Sliding Window vs Two Pointers

| Aspect | Sliding Window | Two Pointers |
|--------|---------------|--------------|
| **Window** | Always contiguous | Can be non-contiguous |
| **Movement** | Slides together | Can move independently |
| **Use Case** | Subarray/substring | Pairs, sorted arrays |
| **Pattern** | Expand and contract | Converge or diverge |
| **Example** | Max sum of size K | Two sum in sorted array |

## 💡 Key Insights

1. **Amortized O(n)**: Even with nested while loop, each element added/removed once
2. **State maintenance**: Key is efficiently tracking window state
3. **Incremental updates**: Don't recalculate, just update
4. **Window validity**: Always check if window satisfies constraints

---

**Next**: [Pattern Recognition Guide](02_Pattern_Recognition.md)
