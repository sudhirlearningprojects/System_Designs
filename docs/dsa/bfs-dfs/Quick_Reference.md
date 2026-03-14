# BFS/DFS - Quick Reference Cheat Sheet

## 🎯 Core Templates

### BFS Template
```java
Queue<Node> queue = new LinkedList<>();
Set<Node> visited = new HashSet<>();

queue.offer(start);
visited.add(start);

while (!queue.isEmpty()) {
    Node node = queue.poll();
    // Process node
    
    for (Node neighbor : getNeighbors(node)) {
        if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.offer(neighbor);
        }
    }
}
```

### DFS Template (Recursive)
```java
void dfs(Node node, Set<Node> visited) {
    if (node == null || visited.contains(node)) {
        return;
    }
    
    visited.add(node);
    // Process node
    
    for (Node neighbor : getNeighbors(node)) {
        dfs(neighbor, visited);
    }
}
```

### DFS Template (Iterative)
```java
Stack<Node> stack = new Stack<>();
Set<Node> visited = new HashSet<>();

stack.push(start);

while (!stack.isEmpty()) {
    Node node = stack.pop();
    
    if (visited.contains(node)) continue;
    
    visited.add(node);
    // Process node
    
    for (Node neighbor : getNeighbors(node)) {
        if (!visited.contains(neighbor)) {
            stack.push(neighbor);
        }
    }
}
```

## 📋 When to Use What

| Goal | Algorithm | Why? |
|------|-----------|------|
| Shortest path (unweighted) | BFS | Level-by-level guarantees shortest |
| All paths | DFS | Backtracking natural |
| Level order traversal | BFS | Processes level by level |
| Cycle detection | DFS | Track recursion stack |
| Connected components | Either | DFS simpler (recursive) |
| Topological sort | DFS | Postorder traversal |
| Minimum steps | BFS | Finds minimum first |
| Tree traversal | DFS | Recursive is natural |

## 🔑 Common Patterns

### Pattern 1: Level-by-Level BFS
```java
Queue<Node> queue = new LinkedList<>();
queue.offer(root);
int level = 0;

while (!queue.isEmpty()) {
    int size = queue.size();
    
    for (int i = 0; i < size; i++) {
        Node node = queue.poll();
        // Process node at current level
        
        for (Node child : node.children) {
            queue.offer(child);
        }
    }
    
    level++;
}
```

### Pattern 2: DFS with Backtracking
```java
void dfs(Node node, List<Node> path, Set<Node> visited) {
    if (node == null || visited.contains(node)) return;
    
    // Add to path
    path.add(node);
    visited.add(node);
    
    if (isTarget(node)) {
        // Process solution
    }
    
    for (Node neighbor : node.neighbors) {
        dfs(neighbor, path, visited);
    }
    
    // Backtrack
    path.remove(path.size() - 1);
    visited.remove(node);
}
```

### Pattern 3: Cycle Detection (DFS)
```java
boolean hasCycle(Node node, Set<Node> visited, Set<Node> recStack) {
    visited.add(node);
    recStack.add(node);
    
    for (Node neighbor : node.neighbors) {
        if (!visited.contains(neighbor)) {
            if (hasCycle(neighbor, visited, recStack)) {
                return true;
            }
        } else if (recStack.contains(neighbor)) {
            return true; // Back edge = cycle
        }
    }
    
    recStack.remove(node);
    return false;
}
```

### Pattern 4: Multi-Source BFS
```java
Queue<Node> queue = new LinkedList<>();
Set<Node> visited = new HashSet<>();

// Add all sources
for (Node source : sources) {
    queue.offer(source);
    visited.add(source);
}

while (!queue.isEmpty()) {
    Node node = queue.poll();
    // Process node
    
    for (Node neighbor : getNeighbors(node)) {
        if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.offer(neighbor);
        }
    }
}
```

### Pattern 5: Matrix BFS/DFS
```java
// BFS on matrix
int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
Queue<int[]> queue = new LinkedList<>();
boolean[][] visited = new boolean[m][n];

queue.offer(new int[]{startRow, startCol});
visited[startRow][startCol] = true;

while (!queue.isEmpty()) {
    int[] pos = queue.poll();
    int r = pos[0], c = pos[1];
    
    for (int[] dir : dirs) {
        int nr = r + dir[0];
        int nc = c + dir[1];
        
        if (nr >= 0 && nr < m && nc >= 0 && nc < n && 
            !visited[nr][nc] && isValid(nr, nc)) {
            visited[nr][nc] = true;
            queue.offer(new int[]{nr, nc});
        }
    }
}

// DFS on matrix
void dfs(int[][] grid, int r, int c, boolean[][] visited) {
    if (r < 0 || r >= m || c < 0 || c >= n || 
        visited[r][c] || !isValid(r, c)) {
        return;
    }
    
    visited[r][c] = true;
    
    dfs(grid, r + 1, c, visited);
    dfs(grid, r - 1, c, visited);
    dfs(grid, r, c + 1, visited);
    dfs(grid, r, c - 1, visited);
}
```

## ⚡ Time & Space Complexity

| Structure | BFS Time | BFS Space | DFS Time | DFS Space |
|-----------|----------|-----------|----------|-----------|
| Tree | O(n) | O(w) | O(n) | O(h) |
| Graph | O(V+E) | O(V) | O(V+E) | O(V) |
| Matrix | O(m×n) | O(m×n) | O(m×n) | O(m×n) |

Where:
- n = nodes, w = max width, h = height
- V = vertices, E = edges
- m×n = matrix dimensions

## 🚫 Common Mistakes

1. **Not marking as visited**
   ```java
   // ❌ Wrong - infinite loop
   queue.offer(neighbor);
   
   // ✅ Correct
   visited.add(neighbor);
   queue.offer(neighbor);
   ```

2. **Wrong timing for visited check**
   ```java
   // ❌ Wrong - may add duplicates
   Node node = queue.poll();
   if (!visited.contains(node)) {
       visited.add(node);
   }
   
   // ✅ Correct - mark before adding
   if (!visited.contains(neighbor)) {
       visited.add(neighbor);
       queue.offer(neighbor);
   }
   ```

3. **Forgetting to backtrack (DFS)**
   ```java
   // ❌ Wrong
   path.add(node);
   dfs(neighbor, path);
   // Missing: path.remove(path.size() - 1);
   
   // ✅ Correct
   path.add(node);
   dfs(neighbor, path);
   path.remove(path.size() - 1);
   ```

4. **Not handling null/empty**
   ```java
   // ❌ Wrong
   queue.offer(root);
   
   // ✅ Correct
   if (root == null) return;
   queue.offer(root);
   ```

## 📊 All 20 Problems Summary

### Easy (8 problems)
1. Level Order Traversal - BFS - O(n)
2. Maximum Depth - DFS - O(n)
3. Same Tree - DFS - O(n)
4. Invert Tree - DFS - O(n)
5. Symmetric Tree - DFS - O(n)
6. Path Sum - DFS - O(n)
7. Merge Trees - DFS - O(n)
8. Flood Fill - DFS - O(m×n)

### Medium (10 problems)
1. Right Side View - BFS - O(n)
2. Number of Islands - DFS - O(m×n)
3. Clone Graph - DFS - O(V+E)
4. Course Schedule - DFS - O(V+E)
5. Pacific Atlantic - DFS - O(m×n)
6. Word Search - DFS - O(m×n×4^L)
7. Surrounded Regions - DFS - O(m×n)
8. All Paths - DFS - O(2^V×V)
9. Rotting Oranges - BFS - O(m×n)
10. Shortest Path Matrix - BFS - O(n²)

### Hard (2 problems)
1. Word Ladder II - BFS+DFS - O(N×L²×26)
2. Serialize/Deserialize - DFS/BFS - O(n)

## 🎓 Study Plan

### Week 1: BFS Fundamentals
- Day 1-2: BFS theory and templates
- Day 3-4: Easy BFS problems (1, 8)
- Day 5-6: Medium BFS problems (1, 9, 10)
- Day 7: Review and practice

### Week 2: DFS Fundamentals
- Day 1-2: DFS theory and templates
- Day 3-4: Easy DFS problems (2-7)
- Day 5-6: Medium DFS problems (2-8)
- Day 7: Review and practice

### Week 3: Advanced
- Day 1-3: Hard problem 1 (Word Ladder II)
- Day 4-6: Hard problem 2 (Serialize/Deserialize)
- Day 7: Mixed practice

## 💡 Interview Tips

1. **Clarify the Problem**
   - Tree, graph, or matrix?
   - Directed or undirected?
   - Weighted or unweighted?

2. **Choose the Right Approach**
   - Shortest path? → BFS
   - All paths? → DFS
   - Level order? → BFS
   - Backtracking? → DFS

3. **Explain Your Approach**
   - "I'll use BFS because..."
   - "I'll use DFS because..."
   - Walk through example

4. **Handle Edge Cases**
   - Empty graph/tree
   - Single node
   - Disconnected components
   - Cycles

5. **Analyze Complexity**
   - Time: O(V+E) or O(n)
   - Space: O(V) or O(h)

## 🔗 Related Patterns

- **Topological Sort**: DFS postorder
- **Dijkstra's Algorithm**: BFS with priority queue
- **A* Search**: BFS with heuristic
- **Backtracking**: DFS with state restoration
- **Dynamic Programming on Trees**: DFS with memoization

## 📝 Quick Decision Guide

```
┌─────────────────────────────────────────────────────────┐
│ BFS/DFS DECISION GUIDE                                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ USE BFS WHEN:                                           │
│   ✓ Need shortest path (unweighted)                    │
│   ✓ Level-order traversal                              │
│   ✓ Minimum steps/moves                                │
│   ✓ Nearest/closest element                            │
│   → Queue, level by level                              │
│                                                         │
│ USE DFS WHEN:                                           │
│   ✓ Need all paths/solutions                           │
│   ✓ Backtracking problems                              │
│   ✓ Cycle detection                                    │
│   ✓ Tree traversal (in/pre/post)                       │
│   ✓ Topological sort                                   │
│   → Stack/Recursion, depth first                       │
│                                                         │
│ EITHER WORKS:                                           │
│   ✓ Connected components                               │
│   ✓ Reachable nodes                                    │
│   → DFS usually simpler (recursive)                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## ✅ Mastery Checklist

- [ ] Understand BFS and DFS concepts
- [ ] Memorize core templates
- [ ] Can identify when to use which
- [ ] Solved all 8 easy problems
- [ ] Solved all 10 medium problems
- [ ] Solved both hard problems
- [ ] Can handle matrix traversal
- [ ] Understand cycle detection
- [ ] Can implement backtracking
- [ ] Complete problems in time limit

## 🎯 Key Formulas

**BFS Queue Operations**:
```java
queue.offer(node);  // Add to queue
Node node = queue.poll();  // Remove from queue
int size = queue.size();  // Current level size
```

**DFS Recursion Pattern**:
```java
if (base_case) return;
// Process current
for (neighbor : neighbors) {
    dfs(neighbor);
}
// Backtrack if needed
```

**Matrix Directions**:
```java
int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};  // 4 directions
int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};  // 8 directions
```

**Visited Check**:
```java
// Before adding to queue/stack
if (!visited.contains(node)) {
    visited.add(node);
    queue.offer(node);
}
```

---

**Keep this cheat sheet handy during practice and interviews!**
