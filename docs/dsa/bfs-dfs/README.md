# BFS/DFS Approach - Complete Guide

## 📚 Documentation Structure

This comprehensive guide covers BFS (Breadth-First Search) and DFS (Depth-First Search) techniques with theory, examples, and 20+ problems.

### Documents

1. **[01_Theory_and_Concepts.md](01_Theory_and_Concepts.md)** - Deep dive into BFS/DFS theory
2. **[02_Pattern_Recognition.md](02_Pattern_Recognition.md)** - How to identify BFS/DFS problems
3. **[03_Easy_Problems.md](03_Easy_Problems.md)** - 8 Easy problems with solutions (40%)
4. **[04_Medium_Problems.md](04_Medium_Problems.md)** - 10 Medium problems with solutions (50%)
5. **[05_Hard_Problems.md](05_Hard_Problems.md)** - 2 Hard problems with solutions (10%)
6. **[Quick_Reference.md](Quick_Reference.md)** - Cheat sheet for quick review

## 🎯 Quick Overview

**BFS (Breadth-First Search)** explores level by level, visiting all neighbors before going deeper.  
**DFS (Depth-First Search)** explores as far as possible along each branch before backtracking.

### Key Benefits
- **Versatile**: Works on trees, graphs, matrices, and more
- **Optimal**: BFS finds shortest path in unweighted graphs
- **Complete**: DFS explores all possibilities
- **Efficient**: O(V + E) time complexity for graphs

### Common Patterns
1. **BFS with Queue** - Level-order traversal, shortest path
2. **DFS with Recursion** - Tree traversal, backtracking
3. **DFS with Stack** - Iterative DFS, cycle detection
4. **Bidirectional BFS** - Meet in the middle optimization

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
1. Binary Tree Level Order Traversal
2. Maximum Depth of Binary Tree
3. Same Tree
4. Invert Binary Tree
5. Symmetric Tree
6. Path Sum
7. Merge Two Binary Trees
8. Flood Fill

### Medium Problems (10)
1. Binary Tree Right Side View
2. Number of Islands
3. Clone Graph
4. Course Schedule
5. Pacific Atlantic Water Flow
6. Word Search
7. Surrounded Regions
8. All Paths From Source to Target
9. Rotting Oranges
10. Shortest Path in Binary Matrix

### Hard Problems (2)
1. Word Ladder II
2. Serialize and Deserialize Binary Tree

## 💡 Tips for Success

1. **Choose the right approach** - BFS for shortest path, DFS for all paths
2. **Track visited nodes** - Avoid infinite loops
3. **Handle edge cases** - Empty graphs, disconnected components
4. **Optimize space** - Use iterative DFS when possible
5. **Practice both** - Some problems work with either approach

## 🎨 Visual Comparison

### BFS (Level by Level)
```
        1
       / \
      2   3
     / \   \
    4   5   6

BFS Order: 1 → 2, 3 → 4, 5, 6
Uses: Queue
```

### DFS (Depth First)
```
        1
       / \
      2   3
     / \   \
    4   5   6

DFS Order: 1 → 2 → 4 → 5 → 3 → 6
Uses: Stack/Recursion
```

## 🔑 Key Differences

| Aspect | BFS | DFS |
|--------|-----|-----|
| Data Structure | Queue | Stack/Recursion |
| Memory | O(width) | O(height) |
| Shortest Path | ✅ Yes (unweighted) | ❌ No |
| All Paths | ❌ Complex | ✅ Natural |
| Implementation | Iterative | Recursive/Iterative |
| Use Case | Level-order, shortest path | Backtracking, cycle detection |

## 📈 Complexity Patterns

| Structure | Time | Space | Notes |
|-----------|------|-------|-------|
| Tree (BFS) | O(n) | O(w) | w = max width |
| Tree (DFS) | O(n) | O(h) | h = height |
| Graph (BFS) | O(V + E) | O(V) | V = vertices, E = edges |
| Graph (DFS) | O(V + E) | O(V) | Includes recursion stack |
| Matrix (BFS) | O(m × n) | O(m × n) | m × n grid |
| Matrix (DFS) | O(m × n) | O(m × n) | Includes recursion |

## 🎯 When to Use What

### Use BFS When:
- Finding shortest path (unweighted)
- Level-order traversal needed
- Exploring neighbors first
- Finding closest/nearest element
- Minimum steps/moves problems

### Use DFS When:
- Finding all paths/solutions
- Backtracking problems
- Cycle detection
- Topological sorting
- Connected components
- Tree traversal (in/pre/post order)

---

**Total Problems**: 20 | **Estimated Study Time**: 20-25 hours
