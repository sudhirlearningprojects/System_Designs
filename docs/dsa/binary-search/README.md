# Binary Search - Complete Guide

## 📚 Overview

Binary Search is a fundamental divide-and-conquer algorithm that efficiently searches sorted data by repeatedly dividing the search space in half. It's one of the most important algorithmic techniques with applications far beyond simple array searching.

**Time Complexity**: O(log n) - Exponentially faster than linear search  
**Space Complexity**: O(1) iterative, O(log n) recursive  
**Key Requirement**: Data must be monotonic (sorted or have a searchable property)

## 🎯 When to Use Binary Search

Binary Search is applicable when:
- ✅ Searching in sorted arrays
- ✅ Finding boundaries (first/last occurrence)
- ✅ Searching in rotated sorted arrays
- ✅ Finding peak elements
- ✅ Searching in 2D matrices
- ✅ Optimization problems (minimize/maximize)
- ✅ Answer space is monotonic

## 📊 Problem Distribution

This guide contains **20 carefully selected problems**:

| Difficulty | Count | Percentage |
|------------|-------|------------|
| Easy       | 8     | 40%        |
| Medium     | 10    | 50%        |
| Hard       | 2     | 10%        |

## 🗂️ Documentation Structure

### 1. [Theory & Concepts](01_Theory_and_Concepts.md)
- Binary Search fundamentals
- 5 core patterns with templates
- Common pitfalls and edge cases
- Complexity analysis

### 2. [Pattern Recognition Guide](02_Pattern_Recognition.md)
- How to identify Binary Search problems
- Pattern templates and variations
- Decision framework
- Problem-solving strategies

### 3. [Easy Problems](03_Easy_Problems.md) (8 problems)
- Binary Search (Basic)
- Search Insert Position
- First Bad Version
- Valid Perfect Square
- Sqrt(x)
- Guess Number Higher or Lower
- Peak Index in Mountain Array
- Count Negative Numbers in Sorted Matrix

### 4. [Medium Problems](04_Medium_Problems.md) (10 problems)
- Find First and Last Position
- Search in Rotated Sorted Array
- Find Peak Element
- Find Minimum in Rotated Sorted Array
- Search a 2D Matrix
- Koko Eating Bananas
- Capacity To Ship Packages
- Minimum Number of Days to Make m Bouquets
- Split Array Largest Sum
- Find K Closest Elements

### 5. [Hard Problems](05_Hard_Problems.md) (2 problems)
- Median of Two Sorted Arrays
- Find Minimum in Rotated Sorted Array II

### 6. [Quick Reference](Quick_Reference.md)
- All templates at a glance
- Common patterns cheat sheet
- Complexity reference
- Study plan

## 🎓 Learning Path

### Week 1: Foundations (Days 1-7)
- **Day 1-2**: Study Theory & Concepts, understand basic template
- **Day 3-4**: Complete Easy Problems 1-4
- **Day 4-5**: Complete Easy Problems 5-8
- **Day 6-7**: Review and practice variations

### Week 2: Advanced Patterns (Days 8-14)
- **Day 8-9**: Study Pattern Recognition Guide
- **Day 10-11**: Complete Medium Problems 1-5
- **Day 12-13**: Complete Medium Problems 6-10
- **Day 14**: Review boundary conditions and edge cases

### Week 3: Mastery (Days 15-21)
- **Day 15-17**: Complete Hard Problems
- **Day 18-19**: Revisit challenging problems
- **Day 20-21**: Timed practice and mock interviews

## 🔑 Core Patterns Covered

### 1. **Standard Binary Search**
Finding exact target in sorted array

### 2. **Finding Boundaries**
First/last occurrence, lower/upper bound

### 3. **Rotated/Modified Arrays**
Search in rotated arrays, find minimum/peak

### 4. **2D Matrix Search**
Row-wise and column-wise sorted matrices

### 5. **Binary Search on Answer Space**
Optimization problems (minimize maximum, maximize minimum)

## 💡 Key Insights

### Template Selection
```java
// Use this for exact match
while (left <= right)

// Use this for finding boundaries
while (left < right)
```

### Common Pitfalls
1. **Integer Overflow**: Use `left + (right - left) / 2` instead of `(left + right) / 2`
2. **Infinite Loops**: Ensure search space reduces each iteration
3. **Off-by-One Errors**: Carefully handle `left`, `right`, and `mid` updates
4. **Edge Cases**: Empty arrays, single elements, duplicates

### Optimization Techniques
- Use iterative over recursive (saves stack space)
- Cache frequently accessed values
- Consider early termination conditions
- Handle edge cases before main loop

## 📈 Complexity Patterns

| Pattern | Time | Space | Notes |
|---------|------|-------|-------|
| Standard Search | O(log n) | O(1) | Iterative |
| Recursive Search | O(log n) | O(log n) | Stack space |
| 2D Matrix | O(log(m*n)) | O(1) | Treat as 1D |
| Rotated Array | O(log n) | O(1) | Two-pass possible |
| Answer Space | O(n log k) | O(1) | k = answer range |

## 🎯 Success Metrics

After completing this guide, you should be able to:
- ✅ Recognize Binary Search opportunities in O(n) problems
- ✅ Choose the correct template for different scenarios
- ✅ Handle edge cases and boundary conditions
- ✅ Avoid common pitfalls (overflow, infinite loops)
- ✅ Apply Binary Search to optimization problems
- ✅ Solve 80%+ of Binary Search problems in interviews

## 🔗 Navigation

- **Next**: [Theory & Concepts](01_Theory_and_Concepts.md)
- **Jump to**: [Easy Problems](03_Easy_Problems.md) | [Medium Problems](04_Medium_Problems.md) | [Hard Problems](05_Hard_Problems.md)
- **Quick Reference**: [Cheat Sheet](Quick_Reference.md)

## 📝 Additional Resources

### Practice Platforms
- LeetCode Binary Search Tag
- HackerRank Search Challenges
- Codeforces Binary Search Problems

### Related Topics
- Divide and Conquer
- Two Pointers (for sorted arrays)
- Ternary Search (for unimodal functions)
- Exponential Search (for unbounded arrays)

---

**Total Problems**: 20  
**Estimated Completion Time**: 3 weeks  
**Difficulty Distribution**: 40% Easy, 50% Medium, 10% Hard

Happy Learning! 🚀
