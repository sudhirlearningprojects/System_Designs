# Sliding Window Approach - Complete Guide

## 📚 Documentation Structure

This comprehensive guide covers the Sliding Window technique with theory, examples, and 20+ problems.

### Documents

1. **[01_Theory_and_Concepts.md](01_Theory_and_Concepts.md)** - Deep dive into Sliding Window theory
2. **[02_Pattern_Recognition.md](02_Pattern_Recognition.md)** - How to identify Sliding Window problems
3. **[03_Easy_Problems.md](03_Easy_Problems.md)** - 8 Easy problems with solutions (40%)
4. **[04_Medium_Problems.md](04_Medium_Problems.md)** - 10 Medium problems with solutions (50%)
5. **[05_Hard_Problems.md](05_Hard_Problems.md)** - 2 Hard problems with solutions (10%)
6. **[Quick_Reference.md](Quick_Reference.md)** - Cheat sheet for quick review

## 🎯 Quick Overview

**Sliding Window** is an algorithmic technique that maintains a window (subarray/substring) that slides through the data structure to solve problems efficiently.

### Key Benefits
- **Time Complexity**: Reduces O(n²) or O(n³) to O(n)
- **Space Complexity**: Usually O(1) or O(k) where k is window size
- **Efficiency**: Avoids redundant calculations

### Common Patterns
1. **Fixed Window** - Window size is constant
2. **Variable Window** - Window size changes dynamically
3. **Dynamic Window with Auxiliary Data Structure** - Uses hash map/set

## 📊 Problem Distribution

| Difficulty | Count | Percentage |
|------------|-------|------------|
| Easy       | 8     | 40%        |
| Medium     | 10    | 50%        |
| Hard       | 2     | 10%        |
| **Total**  | **20**| **100%**   |

## 🚀 Getting Started

Start with **01_Theory_and_Concepts.md** to understand the fundamentals, then move to **02_Pattern_Recognition.md** to learn how to identify these problems.

Practice problems in order: Easy → Medium → Hard

## 📝 Problem List

### Easy Problems (8)
1. Maximum Sum Subarray of Size K
2. Average of Subarrays of Size K
3. Contains Duplicate II
4. Maximum Average Subarray I
5. Minimum Size Subarray Sum
6. Find All Anagrams in String
7. Defanging an IP Address
8. Longest Nice Substring

### Medium Problems (10)
1. Longest Substring Without Repeating Characters
2. Longest Repeating Character Replacement
3. Permutation in String
4. Fruit Into Baskets
5. Longest Substring with At Most K Distinct Characters
6. Max Consecutive Ones III
7. Subarray Product Less Than K
8. Minimum Window Substring
9. Grumpy Bookstore Owner
10. Get Equal Substrings Within Budget

### Hard Problems (2)
1. Sliding Window Maximum
2. Minimum Window Subsequence

## 💡 Tips for Success

1. **Identify the window** - What defines the window boundaries?
2. **Expand and contract** - When to grow/shrink the window?
3. **Track state** - What data do you need to maintain?
4. **Update result** - When to update the answer?
5. **Handle edge cases** - Empty arrays, window larger than array

## 🎨 Visual Example

```
Array: [1, 3, 2, 6, -1, 4, 1, 8, 2]
Window size K = 3

Step 1: [1, 3, 2] 6, -1, 4, 1, 8, 2  → sum = 6
Step 2: 1, [3, 2, 6] -1, 4, 1, 8, 2  → sum = 11
Step 3: 1, 3, [2, 6, -1] 4, 1, 8, 2  → sum = 7
Step 4: 1, 3, 2, [6, -1, 4] 1, 8, 2  → sum = 9
...

Maximum sum = 13 (window: [4, 1, 8])
```

## 🔑 Key Differences from Two Pointers

| Aspect | Sliding Window | Two Pointers |
|--------|---------------|--------------|
| Focus | Contiguous subarray/substring | Any two elements |
| Window | Always contiguous | Can be non-contiguous |
| Movement | Window slides | Pointers converge/diverge |
| Use Case | Subarray problems | Pair/triplet problems |

## 📈 Complexity Patterns

| Pattern | Time | Space | Example |
|---------|------|-------|---------|
| Fixed Window | O(n) | O(1) | Max sum of size K |
| Variable Window | O(n) | O(1) or O(k) | Longest substring |
| With Hash Map | O(n) | O(k) | Anagrams |
| With Deque | O(n) | O(k) | Sliding window maximum |

---

**Total Problems**: 20 | **Estimated Study Time**: 15-20 hours
