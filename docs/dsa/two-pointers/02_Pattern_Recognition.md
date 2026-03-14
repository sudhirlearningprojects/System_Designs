# Two Pointers - Pattern Recognition Guide

## 🎯 How to Identify Two Pointers Problems

This guide helps you recognize when to use the Two Pointers technique by analyzing problem characteristics.

## 🔍 Recognition Checklist

### ✅ Strong Indicators

1. **Problem mentions "sorted array"**
   - "Given a sorted array..."
   - "Array is sorted in ascending order..."
   
2. **Looking for pairs/triplets**
   - "Find two numbers that sum to..."
   - "Find all triplets that sum to..."
   
3. **Palindrome-related**
   - "Check if string is palindrome"
   - "Find longest palindromic substring"
   
4. **In-place array manipulation**
   - "Remove duplicates in-place"
   - "Move all zeros to end"
   
5. **Subarray/substring with condition**
   - "Longest substring without repeating characters"
   - "Minimum window substring"
   
6. **Comparing elements from both ends**
   - "Container with most water"
   - "Trapping rain water"

### ⚠️ Moderate Indicators

1. **Can sort without breaking constraints**
   - Original order doesn't matter
   - Sorting is allowed
   
2. **Need to optimize O(n²) solution**
   - Brute force uses nested loops
   - Looking for linear solution
   
3. **Partitioning problems**
   - "Partition array into two parts"
   - "Dutch National Flag problem"

## 📋 Decision Tree

```
Is the array/string sorted?
│
├─ YES ──→ Is it about finding pairs/triplets?
│          │
│          ├─ YES ──→ Use OPPOSITE DIRECTION pattern
│          │
│          └─ NO ──→ Is it about removing/modifying elements?
│                    │
│                    └─ YES ──→ Use SAME DIRECTION pattern
│
└─ NO ──→ Can you sort it?
          │
          ├─ YES ──→ Sort first, then use TWO POINTERS
          │
          └─ NO ──→ Is it about subarray/substring?
                    │
                    ├─ YES ──→ Use SLIDING WINDOW (same direction)
                    │
                    └─ NO ──→ Is it about cycle detection?
                              │
                              └─ YES ──→ Use FAST & SLOW pointers
```

## 🎨 Pattern Matching Examples

### Pattern 1: Opposite Direction

**Keywords**: sorted, pair, two numbers, palindrome, reverse, from both ends

**Example Problems**:
```
✓ "Find two numbers in sorted array that sum to target"
✓ "Check if string is palindrome"
✓ "Container with most water"
✓ "Valid palindrome II (remove one character)"
```

**Recognition**:
- Array is sorted OR order doesn't matter
- Need to compare elements from different positions
- Can eliminate options by moving pointers

**Template Match**:
```java
int left = 0, right = n - 1;
while (left < right) {
    if (condition_met) return true;
    if (need_larger_value) left++;
    else right--;
}
```

### Pattern 2: Same Direction (Sliding Window)

**Keywords**: substring, subarray, consecutive, window, longest, shortest, contains

**Example Problems**:
```
✓ "Longest substring without repeating characters"
✓ "Minimum window substring containing all characters"
✓ "Find all anagrams in string"
✓ "Subarray with sum equals K"
```

**Recognition**:
- Looking for contiguous subarray/substring
- Need to maintain a "window" of elements
- Window expands and contracts based on condition

**Template Match**:
```java
int left = 0;
for (int right = 0; right < n; right++) {
    // Add arr[right] to window
    
    while (window_invalid) {
        // Remove arr[left] from window
        left++;
    }
    
    // Update result
}
```

### Pattern 3: Fast & Slow

**Keywords**: cycle, middle, linked list, duplicate, nth from end

**Example Problems**:
```
✓ "Detect cycle in linked list"
✓ "Find middle of linked list"
✓ "Find duplicate number (Floyd's algorithm)"
✓ "Happy number"
```

**Recognition**:
- Cycle detection needed
- Finding middle element
- Two pointers move at different speeds

**Template Match**:
```java
int slow = start, fast = start;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    
    if (slow == fast) {
        // Cycle detected
    }
}
```

## 🔑 Key Questions to Ask

### Question 1: Is the data sorted?

**YES** → Strong candidate for opposite direction two pointers

**NO** → Can I sort it?
- If YES and order doesn't matter → Sort then use two pointers
- If NO → Consider sliding window or other patterns

### Question 2: What am I looking for?

**Pairs/Triplets** → Opposite direction  
**Subarray/Substring** → Same direction (sliding window)  
**Cycle** → Fast & slow  
**In-place modification** → Same direction  

### Question 3: What's the brute force complexity?

**O(n²) with nested loops** → Likely can optimize with two pointers  
**O(n) already** → Two pointers might not help  

### Question 4: Do I need extra space?

**Must be O(1) space** → Two pointers is great  
**Can use O(n) space** → Consider hash map + two pointers  

## 📊 Problem Type Matrix

| Problem Type | Pattern | Sorted Required? | Example |
|--------------|---------|------------------|---------|
| Two Sum | Opposite | Yes | Find pair with sum = target |
| Three Sum | Opposite | Yes | Find triplets with sum = 0 |
| Palindrome | Opposite | No | Check if string is palindrome |
| Remove Duplicates | Same Direction | Yes | Remove duplicates in-place |
| Longest Substring | Same Direction | No | No repeating characters |
| Minimum Window | Same Direction | No | Contains all characters |
| Cycle Detection | Fast & Slow | No | Linked list cycle |
| Container Water | Opposite | No | Maximum area |

## 🎯 Common Problem Phrases

### Opposite Direction Triggers

- "sorted array"
- "find two numbers"
- "pair that sums to"
- "palindrome"
- "from both ends"
- "maximum area"
- "container"

### Same Direction Triggers

- "substring"
- "subarray"
- "consecutive elements"
- "window"
- "longest"
- "shortest"
- "contains all"
- "without repeating"

### Fast & Slow Triggers

- "cycle"
- "linked list"
- "middle element"
- "nth from end"
- "duplicate number"
- "happy number"

## 🚫 When NOT to Use Two Pointers

### 1. Unsorted + Can't Sort
```
Problem: "Find pair with sum = K in unsorted array, maintain order"
Solution: Use hash map instead
```

### 2. Need All Pairs
```
Problem: "Find all pairs (i, j) where i < j"
Solution: Nested loops might be necessary
```

### 3. Complex Dependencies
```
Problem: "Element at i depends on elements at i-2, i-1, i+1, i+2"
Solution: Dynamic programming might be better
```

### 4. Non-linear Data Structure
```
Problem: "Find path in binary tree"
Solution: Use tree traversal algorithms
```

## 💡 Conversion Examples

### Example 1: Brute Force → Two Pointers

**Problem**: Find pair with sum = target in sorted array

**Brute Force** (O(n²)):
```java
for (int i = 0; i < n; i++) {
    for (int j = i + 1; j < n; j++) {
        if (arr[i] + arr[j] == target) {
            return new int[]{i, j};
        }
    }
}
```

**Two Pointers** (O(n)):
```java
int left = 0, right = n - 1;
while (left < right) {
    int sum = arr[left] + arr[right];
    if (sum == target) return new int[]{left, right};
    if (sum < target) left++;
    else right--;
}
```

### Example 2: Hash Map → Two Pointers

**Problem**: Two Sum (when array is sorted)

**Hash Map** (O(n) time, O(n) space):
```java
Map<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < n; i++) {
    if (map.containsKey(target - arr[i])) {
        return new int[]{map.get(target - arr[i]), i};
    }
    map.put(arr[i], i);
}
```

**Two Pointers** (O(n) time, O(1) space):
```java
int left = 0, right = n - 1;
while (left < right) {
    int sum = arr[left] + arr[right];
    if (sum == target) return new int[]{left, right};
    if (sum < target) left++;
    else right--;
}
```

## 🎓 Practice Strategy

### Level 1: Recognition Practice
1. Read problem statement
2. Identify keywords
3. Determine which pattern applies
4. Don't code yet - just identify

### Level 2: Template Application
1. Choose the right template
2. Modify for specific problem
3. Handle edge cases
4. Test with examples

### Level 3: Optimization
1. Start with brute force
2. Identify bottleneck
3. Apply two pointers
4. Verify correctness

## 📝 Quick Reference Card

```
┌─────────────────────────────────────────────────────────┐
│ TWO POINTERS PATTERN RECOGNITION                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ OPPOSITE DIRECTION:                                     │
│   ✓ Sorted array                                       │
│   ✓ Find pairs/triplets                                │
│   ✓ Palindrome check                                   │
│   ✓ Compare from both ends                             │
│                                                         │
│ SAME DIRECTION (Sliding Window):                        │
│   ✓ Substring/subarray                                 │
│   ✓ Consecutive elements                               │
│   ✓ Longest/shortest with condition                    │
│   ✓ Window-based problems                              │
│                                                         │
│ FAST & SLOW:                                            │
│   ✓ Cycle detection                                    │
│   ✓ Middle element                                     │
│   ✓ Linked list problems                               │
│   ✓ Different speeds needed                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

**Next**: [Easy Problems](03_Easy_Problems.md)
