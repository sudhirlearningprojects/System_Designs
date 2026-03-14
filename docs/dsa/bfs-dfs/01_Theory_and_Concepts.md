# BFS/DFS Approach - Theory and Concepts

## 📖 What are BFS and DFS?

**BFS (Breadth-First Search)** and **DFS (Depth-First Search)** are fundamental graph traversal algorithms used to explore nodes and edges of a graph or tree.

### Core Difference
- **BFS**: Explores neighbors first (level by level)
- **DFS**: Explores depth first (as far as possible before backtracking)

## 🎯 BFS (Breadth-First Search)

### Concept

BFS explores a graph level by level, visiting all neighbors of a node before moving to the next level.

```
        1
       /|\
      2 3 4
     /|   |\
    5 6   7 8

Level 0: 1
Level 1: 2, 3, 4
Level 2: 5, 6, 7, 8

BFS Order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8
```

### Algorithm

```java
void bfs(Node start) {
    Queue<Node> queue = new LinkedList<>();
    Set<Node> visited = new HashSet<>();
    
    queue.offer(start);
    visited.add(start);
    
    while (!queue.isEmpty()) {
        Node node = queue.poll();
        process(node);
        
        for (Node neighbor : node.neighbors) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
}
```

### Key Characteristics

✅ **Uses Queue** (FIFO - First In First Out)  
✅ **Level-by-level exploration**  
✅ **Finds shortest path** in unweighted graphs  
✅ **Iterative implementation**  
✅ **Space: O(width)** - stores one level at a time  

### When to Use BFS

1. **Shortest Path**: Find shortest path in unweighted graph
2. **Level Order**: Process nodes level by level
3. **Minimum Steps**: Find minimum moves/steps
4. **Nearest/Closest**: Find nearest node with property
5. **Connected Components**: Find all nodes in component

### BFS Variations

#### 1. Standard BFS
```java
Queue<Node> queue = new LinkedList<>();
queue.offer(start);
visited.add(start);

while (!queue.isEmpty()) {
    Node node = queue.poll();
    // Process node
    
    for (Node neighbor : node.neighbors) {
        if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.offer(neighbor);
        }
    }
}
```

#### 2. Level-by-Level BFS
```java
Queue<Node> queue = new LinkedList<>();
queue.offer(start);
int level = 0;

while (!queue.isEmpty()) {
    int size = queue.size();
    
    for (int i = 0; i < size; i++) {
        Node node = queue.poll();
        // Process node at current level
        
        for (Node neighbor : node.neighbors) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
    
    level++;
}
```

#### 3. Multi-Source BFS
```java
Queue<Node> queue = new LinkedList<>();

// Add all source nodes
for (Node source : sources) {
    queue.offer(source);
    visited.add(source);
}

while (!queue.isEmpty()) {
    Node node = queue.poll();
    // Process node
    
    for (Node neighbor : node.neighbors) {
        if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.offer(neighbor);
        }
    }
}
```

#### 4. Bidirectional BFS
```java
Set<Node> frontierStart = new HashSet<>();
Set<Node> frontierEnd = new HashSet<>();
Set<Node> visitedStart = new HashSet<>();
Set<Node> visitedEnd = new HashSet<>();

frontierStart.add(start);
frontierEnd.add(end);

while (!frontierStart.isEmpty() && !frontierEnd.isEmpty()) {
    // Expand smaller frontier
    if (frontierStart.size() > frontierEnd.size()) {
        swap(frontierStart, frontierEnd);
        swap(visitedStart, visitedEnd);
    }
    
    Set<Node> nextFrontier = new HashSet<>();
    
    for (Node node : frontierStart) {
        if (frontierEnd.contains(node)) {
            return true; // Path found
        }
        
        for (Node neighbor : node.neighbors) {
            if (!visitedStart.contains(neighbor)) {
                visitedStart.add(neighbor);
                nextFrontier.add(neighbor);
            }
        }
    }
    
    frontierStart = nextFrontier;
}
```

## 🎯 DFS (Depth-First Search)

### Concept

DFS explores as far as possible along each branch before backtracking.

```
        1
       /|\
      2 3 4
     /|   |\
    5 6   7 8

DFS Order (Preorder): 1 → 2 → 5 → 6 → 3 → 4 → 7 → 8
```

### Algorithm (Recursive)

```java
void dfs(Node node, Set<Node> visited) {
    if (node == null || visited.contains(node)) {
        return;
    }
    
    visited.add(node);
    process(node);
    
    for (Node neighbor : node.neighbors) {
        dfs(neighbor, visited);
    }
}
```

### Algorithm (Iterative)

```java
void dfs(Node start) {
    Stack<Node> stack = new Stack<>();
    Set<Node> visited = new HashSet<>();
    
    stack.push(start);
    
    while (!stack.isEmpty()) {
        Node node = stack.pop();
        
        if (visited.contains(node)) continue;
        
        visited.add(node);
        process(node);
        
        for (Node neighbor : node.neighbors) {
            if (!visited.contains(neighbor)) {
                stack.push(neighbor);
            }
        }
    }
}
```

### Key Characteristics

✅ **Uses Stack** or **Recursion**  
✅ **Depth-first exploration**  
✅ **Natural for backtracking**  
✅ **Space: O(height)** - recursion stack  
✅ **Can be recursive or iterative**  

### When to Use DFS

1. **All Paths**: Find all paths from source to destination
2. **Backtracking**: Solve puzzles, permutations, combinations
3. **Cycle Detection**: Detect cycles in graphs
4. **Topological Sort**: Order tasks with dependencies
5. **Connected Components**: Find strongly connected components
6. **Tree Traversal**: Inorder, preorder, postorder

### DFS Variations

#### 1. Recursive DFS (Preorder)
```java
void dfs(Node node, Set<Node> visited) {
    if (node == null || visited.contains(node)) return;
    
    visited.add(node);
    process(node); // Process before children
    
    for (Node child : node.children) {
        dfs(child, visited);
    }
}
```

#### 2. Recursive DFS (Postorder)
```java
void dfs(Node node, Set<Node> visited) {
    if (node == null || visited.contains(node)) return;
    
    visited.add(node);
    
    for (Node child : node.children) {
        dfs(child, visited);
    }
    
    process(node); // Process after children
}
```

#### 3. Iterative DFS
```java
Stack<Node> stack = new Stack<>();
Set<Node> visited = new HashSet<>();

stack.push(start);

while (!stack.isEmpty()) {
    Node node = stack.pop();
    
    if (visited.contains(node)) continue;
    
    visited.add(node);
    process(node);
    
    for (Node neighbor : node.neighbors) {
        if (!visited.contains(neighbor)) {
            stack.push(neighbor);
        }
    }
}
```

#### 4. DFS with Backtracking
```java
void dfs(Node node, List<Node> path, Set<Node> visited) {
    if (node == null || visited.contains(node)) return;
    
    // Add to path
    path.add(node);
    visited.add(node);
    
    if (isTarget(node)) {
        // Found solution
        processSolution(path);
    }
    
    for (Node neighbor : node.neighbors) {
        dfs(neighbor, path, visited);
    }
    
    // Backtrack
    path.remove(path.size() - 1);
    visited.remove(node);
}
```

## 📊 Detailed Comparison

### Visual Example

```
Graph:
    A
   / \
  B   C
 / \   \
D   E   F

BFS from A:
Step 1: Visit A, Queue: []
Step 2: Add B, C, Queue: [B, C]
Step 3: Visit B, Queue: [C]
Step 4: Add D, E, Queue: [C, D, E]
Step 5: Visit C, Queue: [D, E]
Step 6: Add F, Queue: [D, E, F]
Step 7: Visit D, E, F

Order: A → B → C → D → E → F

DFS from A (Recursive):
Visit A
  Visit B
    Visit D
    Visit E
  Visit C
    Visit F

Order: A → B → D → E → C → F
```

### Comparison Table

| Aspect | BFS | DFS |
|--------|-----|-----|
| **Data Structure** | Queue | Stack/Recursion |
| **Order** | Level by level | Depth first |
| **Memory** | O(width) | O(height) |
| **Shortest Path** | ✅ Yes (unweighted) | ❌ No |
| **All Paths** | Complex | Natural |
| **Implementation** | Always iterative | Recursive or iterative |
| **Completeness** | Complete | Complete |
| **Optimality** | Optimal (unweighted) | Not optimal |
| **Space (Tree)** | O(2^h) worst case | O(h) |
| **Use Case** | Shortest path, level-order | Backtracking, all paths |

## 🔍 Common Applications

### BFS Applications

1. **Shortest Path in Unweighted Graph**
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
       
       return -1; // No path
   }
   ```

2. **Level Order Traversal**
   ```java
   List<List<Integer>> levelOrder(TreeNode root) {
       List<List<Integer>> result = new ArrayList<>();
       if (root == null) return result;
       
       Queue<TreeNode> queue = new LinkedList<>();
       queue.offer(root);
       
       while (!queue.isEmpty()) {
           int size = queue.size();
           List<Integer> level = new ArrayList<>();
           
           for (int i = 0; i < size; i++) {
               TreeNode node = queue.poll();
               level.add(node.val);
               
               if (node.left != null) queue.offer(node.left);
               if (node.right != null) queue.offer(node.right);
           }
           
           result.add(level);
       }
       
       return result;
   }
   ```

### DFS Applications

1. **Find All Paths**
   ```java
   void findAllPaths(Node node, Node target, 
                     List<Node> path, List<List<Node>> result) {
       path.add(node);
       
       if (node == target) {
           result.add(new ArrayList<>(path));
       } else {
           for (Node neighbor : node.neighbors) {
               findAllPaths(neighbor, target, path, result);
           }
       }
       
       path.remove(path.size() - 1); // Backtrack
   }
   ```

2. **Cycle Detection**
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
               return true; // Cycle found
           }
       }
       
       recStack.remove(node);
       return false;
   }
   ```

## 💡 Complexity Analysis

### Time Complexity

**Graph**: O(V + E)
- V = number of vertices
- E = number of edges
- Visit each vertex once: O(V)
- Explore each edge once: O(E)

**Tree**: O(n)
- n = number of nodes
- Visit each node exactly once

**Matrix**: O(m × n)
- m × n = dimensions
- Visit each cell once

### Space Complexity

**BFS**:
- Queue: O(V) in worst case
- Visited set: O(V)
- Total: O(V)
- For trees: O(width) = O(2^h) in worst case

**DFS**:
- Stack/Recursion: O(V) in worst case
- Visited set: O(V)
- Total: O(V)
- For trees: O(height) = O(log n) for balanced, O(n) for skewed

## 🚫 Common Mistakes

### 1. Not Marking as Visited
```java
// ❌ Wrong - infinite loop
void bfs(Node start) {
    Queue<Node> queue = new LinkedList<>();
    queue.offer(start);
    
    while (!queue.isEmpty()) {
        Node node = queue.poll();
        for (Node neighbor : node.neighbors) {
            queue.offer(neighbor); // Will revisit!
        }
    }
}

// ✅ Correct
void bfs(Node start) {
    Queue<Node> queue = new LinkedList<>();
    Set<Node> visited = new HashSet<>();
    
    queue.offer(start);
    visited.add(start);
    
    while (!queue.isEmpty()) {
        Node node = queue.poll();
        for (Node neighbor : node.neighbors) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
}
```

### 2. Wrong Timing for Visited Check
```java
// ❌ Wrong - may add duplicates to queue
queue.offer(neighbor);
visited.add(neighbor); // Too late!

// ✅ Correct - mark before adding
visited.add(neighbor);
queue.offer(neighbor);
```

### 3. Forgetting to Backtrack (DFS)
```java
// ❌ Wrong - doesn't backtrack
void dfs(Node node, List<Node> path) {
    path.add(node);
    
    for (Node neighbor : node.neighbors) {
        dfs(neighbor, path);
    }
    // Missing: path.remove(path.size() - 1);
}

// ✅ Correct
void dfs(Node node, List<Node> path) {
    path.add(node);
    
    for (Node neighbor : node.neighbors) {
        dfs(neighbor, path);
    }
    
    path.remove(path.size() - 1); // Backtrack
}
```

### 4. Not Handling Null/Empty
```java
// ❌ Wrong - NullPointerException
void bfs(Node start) {
    Queue<Node> queue = new LinkedList<>();
    queue.offer(start); // What if start is null?
}

// ✅ Correct
void bfs(Node start) {
    if (start == null) return;
    
    Queue<Node> queue = new LinkedList<>();
    queue.offer(start);
}
```

## 🎓 Learning Path

1. **Understand Basics**: Learn how BFS and DFS work
2. **Master Templates**: Memorize standard implementations
3. **Practice Trees**: Start with binary tree problems
4. **Move to Graphs**: Practice graph traversal
5. **Learn Variations**: Multi-source, bidirectional, backtracking
6. **Solve Problems**: Easy → Medium → Hard

---

**Next**: [Pattern Recognition Guide](02_Pattern_Recognition.md)
