# BFS/DFS - Pattern Recognition Guide

## 🎯 How to Identify BFS/DFS Problems

This guide helps you recognize when to use BFS or DFS by analyzing problem characteristics.

## 🔍 Recognition Checklist

### ✅ Strong Indicators for BFS/DFS

1. **Problem mentions graphs or trees**
   - "Given a binary tree..."
   - "Given a graph..."
   - "Given a grid/matrix..."
   
2. **Traversal keywords**
   - "Visit all nodes"
   - "Explore all paths"
   - "Find all connected components"
   
3. **Path-finding keywords**
   - "Shortest path"
   - "Minimum steps"
   - "Level order"
   - "All paths from X to Y"
   
4. **Connectivity keywords**
   - "Connected components"
   - "Islands"
   - "Regions"
   - "Reachable nodes"

## 📋 Decision Tree: BFS vs DFS

```
Does the problem involve graph/tree/matrix traversal?
│
├─ YES ──→ What are you looking for?
│          │
│          ├─ Shortest path (unweighted) ──→ Use BFS
│          │
│          ├─ Level-order traversal ──→ Use BFS
│          │
│          ├─ Minimum steps/moves ──→ Use BFS
│          │
│          ├─ All paths/solutions ──→ Use DFS
│          │
│          ├─ Backtracking needed ──→ Use DFS
│          │
│          ├─ Cycle detection ──→ Use DFS
│          │
│          ├─ Connected components ──→ Use either (DFS simpler)
│          │
│          └─ Tree traversal (in/pre/post) ──→ Use DFS
│
└─ NO ──→ Not a BFS/DFS problem
```

## 🎨 Pattern Matching Examples

### Pattern 1: Use BFS

**Keywords**: shortest, minimum, level, nearest, closest, fewest

**Example Problems**:
```
✓ "Find shortest path from A to B"
✓ "Minimum number of steps to reach target"
✓ "Level order traversal of binary tree"
✓ "Find nearest node with property X"
✓ "Minimum moves to solve puzzle"
```

**Recognition**:
- Need shortest/minimum in unweighted graph
- Process nodes level by level
- Find closest/nearest element

**Template Match**:
```java
Queue<Node> queue = new LinkedList<>();
Set<Node> visited = new HashSet<>();

queue.offer(start);
visited.add(start);
int steps = 0;

while (!queue.isEmpty()) {
    int size = queue.size();
    
    for (int i = 0; i < size; i++) {
        Node node = queue.poll();
        
        if (isTarget(node)) {
            return steps;
        }
        
        for (Node neighbor : getNeighbors(node)) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
    
    steps++;
}
```

### Pattern 2: Use DFS (Recursive)

**Keywords**: all paths, backtracking, permutations, combinations, subsets

**Example Problems**:
```
✓ "Find all paths from source to destination"
✓ "Generate all permutations"
✓ "Solve N-Queens problem"
✓ "Find all subsets"
✓ "Word search in grid"
```

**Recognition**:
- Need to explore all possibilities
- Backtracking required
- Generate all solutions

**Template Match**:
```java
void dfs(Node node, List<Node> path, Set<Node> visited) {
    if (node == null || visited.contains(node)) {
        return;
    }
    
    // Add to path
    path.add(node);
    visited.add(node);
    
    // Check if solution
    if (isTarget(node)) {
        processSolution(path);
    }
    
    // Explore neighbors
    for (Node neighbor : getNeighbors(node)) {
        dfs(neighbor, path, visited);
    }
    
    // Backtrack
    path.remove(path.size() - 1);
    visited.remove(node);
}
```

### Pattern 3: Use DFS (Cycle Detection)

**Keywords**: cycle, circular dependency, deadlock

**Example Problems**:
```
✓ "Detect cycle in directed graph"
✓ "Course schedule (circular dependencies)"
✓ "Detect deadlock"
```

**Recognition**:
- Need to detect cycles
- Track recursion stack
- Directed or undirected graph

**Template Match**:
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
            return true; // Back edge found
        }
    }
    
    recStack.remove(node);
    return false;
}
```

### Pattern 4: Use Either (Connected Components)

**Keywords**: islands, regions, connected, components

**Example Problems**:
```
✓ "Number of islands"
✓ "Number of connected components"
✓ "Flood fill"
```

**Recognition**:
- Count separate regions
- Mark connected nodes
- Either BFS or DFS works (DFS is simpler)

**Template Match (DFS)**:
```java
int countComponents(int[][] grid) {
    int count = 0;
    boolean[][] visited = new boolean[m][n];
    
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 1 && !visited[i][j]) {
                dfs(grid, i, j, visited);
                count++;
            }
        }
    }
    
    return count;
}

void dfs(int[][] grid, int i, int j, boolean[][] visited) {
    if (i < 0 || i >= m || j < 0 || j >= n || 
        visited[i][j] || grid[i][j] == 0) {
        return;
    }
    
    visited[i][j] = true;
    
    dfs(grid, i + 1, j, visited);
    dfs(grid, i - 1, j, visited);
    dfs(grid, i, j + 1, visited);
    dfs(grid, i, j - 1, visited);
}
```

## 🔑 Key Questions to Ask

### Question 1: What am I looking for?

**Shortest/Minimum** → BFS  
**All paths/solutions** → DFS  
**Any path** → Either (DFS simpler)  
**Cycle** → DFS  

### Question 2: Is it weighted or unweighted?

**Unweighted** → BFS for shortest path  
**Weighted** → Dijkstra/Bellman-Ford (not BFS/DFS)  

### Question 3: Do I need to backtrack?

**Yes** → DFS with backtracking  
**No** → BFS or simple DFS  

### Question 4: What's the structure?

**Tree** → DFS (recursive) is natural  
**Graph** → Both work, choose based on goal  
**Matrix/Grid** → Both work, BFS for shortest path  

## 📊 Problem Type Matrix

| Problem Type | Algorithm | Why? | Example |
|--------------|-----------|------|---------|
| Shortest path (unweighted) | BFS | Explores level by level | Shortest path in maze |
| Level order traversal | BFS | Natural level-by-level | Binary tree level order |
| Minimum steps | BFS | Finds minimum first | Minimum knight moves |
| All paths | DFS | Backtracking natural | All paths source to target |
| Permutations/Combinations | DFS | Backtracking needed | Generate permutations |
| Cycle detection | DFS | Track recursion stack | Course schedule |
| Topological sort | DFS | Postorder traversal | Task scheduling |
| Connected components | Either | Mark all connected | Number of islands |
| Tree traversal | DFS | Recursive is simple | Inorder/Preorder/Postorder |
| Flood fill | Either | Mark region | Paint fill |

## 🎯 Common Problem Phrases

### BFS Triggers

- "shortest path"
- "minimum number of"
- "fewest steps"
- "level order"
- "nearest"
- "closest"
- "minimum moves"
- "breadth first"

### DFS Triggers

- "all paths"
- "all solutions"
- "permutations"
- "combinations"
- "subsets"
- "backtracking"
- "cycle detection"
- "depth first"
- "in/pre/post order"

### Either Works

- "connected components"
- "number of islands"
- "flood fill"
- "reachable nodes"
- "traverse all nodes"

## 🚫 When NOT to Use BFS/DFS

### 1. Weighted Shortest Path
```
Problem: "Find shortest path in weighted graph"
Solution: Use Dijkstra's algorithm or Bellman-Ford
```

### 2. Dynamic Programming
```
Problem: "Maximum path sum in tree"
Solution: Use DP, not pure BFS/DFS
```

### 3. Greedy Algorithms
```
Problem: "Minimum spanning tree"
Solution: Use Kruskal's or Prim's algorithm
```

### 4. Sorting/Searching
```
Problem: "Find element in sorted array"
Solution: Use binary search
```

## 💡 Conversion Examples

### Example 1: Shortest Path

**Problem**: Find shortest path in unweighted graph

**Why BFS**:
- Unweighted graph
- Need shortest path
- BFS explores level by level, guarantees shortest

```java
int shortestPath(Node start, Node end) {
    Queue<Node> queue = new LinkedList<>();
    Map<Node, Integer> distance = new HashMap<>();
    
    queue.offer(start);
    distance.put(start, 0);
    
    while (!queue.isEmpty()) {
        Node node = queue.poll();
        
        if (node == end) {
            return distance.get(node);
        }
        
        for (Node neighbor : node.neighbors) {
            if (!distance.containsKey(neighbor)) {
                distance.put(neighbor, distance.get(node) + 1);
                queue.offer(neighbor);
            }
        }
    }
    
    return -1;
}
```

### Example 2: All Paths

**Problem**: Find all paths from source to destination

**Why DFS**:
- Need all paths, not just one
- Backtracking required
- DFS naturally explores all branches

```java
void findAllPaths(Node node, Node target, 
                  List<Node> path, List<List<Node>> result) {
    path.add(node);
    
    if (node == target) {
        result.add(new ArrayList<>(path));
    } else {
        for (Node neighbor : node.neighbors) {
            if (!path.contains(neighbor)) { // Avoid cycles
                findAllPaths(neighbor, target, path, result);
            }
        }
    }
    
    path.remove(path.size() - 1); // Backtrack
}
```

## 🎓 Practice Strategy

### Level 1: Master Templates
1. Memorize BFS template
2. Memorize DFS recursive template
3. Memorize DFS iterative template
4. Practice on simple trees

### Level 2: Recognize Patterns
1. Read problem statement
2. Identify keywords
3. Determine BFS vs DFS
4. Choose appropriate template

### Level 3: Solve Variations
1. Multi-source BFS
2. Bidirectional BFS
3. DFS with backtracking
4. Cycle detection

### Level 4: Optimize
1. Space optimization
2. Early termination
3. Pruning
4. Memoization

## 📝 Quick Recognition Card

```
┌─────────────────────────────────────────────────────────┐
│ BFS/DFS PATTERN RECOGNITION                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ USE BFS WHEN:                                           │
│   ✓ Shortest path (unweighted)                         │
│   ✓ Minimum steps/moves                                │
│   ✓ Level order traversal                              │
│   ✓ Nearest/closest element                            │
│   → Uses Queue, explores level by level                │
│                                                         │
│ USE DFS WHEN:                                           │
│   ✓ All paths/solutions                                │
│   ✓ Backtracking problems                              │
│   ✓ Cycle detection                                    │
│   ✓ Tree traversal (in/pre/post)                       │
│   → Uses Stack/Recursion, explores depth first         │
│                                                         │
│ EITHER WORKS:                                           │
│   ✓ Connected components                               │
│   ✓ Number of islands                                  │
│   ✓ Flood fill                                         │
│   → DFS is usually simpler (recursive)                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## 🔍 Problem Analysis Framework

### Step 1: Identify Structure
- Is it a tree, graph, or matrix?
- Directed or undirected?
- Weighted or unweighted?

### Step 2: Determine Goal
- Shortest path? → BFS
- All paths? → DFS
- Any path? → Either
- Cycle? → DFS

### Step 3: Choose Implementation
- BFS: Always iterative with queue
- DFS: Recursive (simpler) or iterative with stack

### Step 4: Handle Edge Cases
- Empty graph/tree
- Disconnected components
- Cycles
- Invalid inputs

## 🎯 Common Variations

### Variation 1: Multi-Source BFS
```
"Find distance from any source to all nodes"
→ Add all sources to queue initially
```

### Variation 2: Bidirectional BFS
```
"Find shortest path between two nodes"
→ BFS from both ends, meet in middle
```

### Variation 3: DFS with Memoization
```
"Count paths with memoization"
→ DFS + cache results
```

### Variation 4: Iterative DFS
```
"DFS without recursion (avoid stack overflow)"
→ Use explicit stack
```

## 📈 Complexity Patterns

| Structure | BFS Time | BFS Space | DFS Time | DFS Space |
|-----------|----------|-----------|----------|-----------|
| Tree | O(n) | O(w) | O(n) | O(h) |
| Graph | O(V+E) | O(V) | O(V+E) | O(V) |
| Matrix | O(m×n) | O(m×n) | O(m×n) | O(m×n) |

Where:
- n = nodes, w = max width, h = height
- V = vertices, E = edges
- m×n = matrix dimensions

---

**Next**: [Easy Problems](03_Easy_Problems.md)
