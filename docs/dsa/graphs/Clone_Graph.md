# Clone Graph - Deep Copy Graph Structure

## Problem Statement

**Real-World Context**: Used in distributed systems to replicate data structures, version control systems (Git) to create branches, and game development to save/load game states.

Given a reference to a node in a connected undirected graph, return a deep copy (clone) of the graph. Each node contains a value and a list of neighbors.

**Input**: Reference to a node in the graph  
**Output**: Reference to the cloned node in the cloned graph

**Examples:**
```
Example 1:
Input: adjList = [[2,4],[1,3],[2,4],[1,3]]
Output: [[2,4],[1,3],[2,4],[1,3]]

Graph:
    1 --- 2
    |     |
    4 --- 3

Explanation: 4 nodes with edges 1-2, 1-4, 2-3, 3-4

Example 2:
Input: adjList = [[]]
Output: [[]]
Explanation: Single node with no neighbors

Example 3:
Input: adjList = []
Output: []
Explanation: Empty graph
```

---

## Solution Approaches

### Approach 1: DFS with HashMap (Optimal)

**Algorithm:**
1. Use HashMap to track original → cloned node mapping
2. DFS through graph, creating clones as we go
3. For each neighbor, recursively clone if not already cloned
4. Connect cloned nodes using the mapping

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V) for HashMap + O(V) for recursion stack

```java
class Node {
    public int val;
    public List<Node> neighbors;
    
    public Node(int val) {
        this.val = val;
        neighbors = new ArrayList<>();
    }
}

public Node cloneGraphDFS(Node node) {
    if (node == null) return null;
    
    Map<Node, Node> visited = new HashMap<>();
    return dfsClone(node, visited);
}

private Node dfsClone(Node node, Map<Node, Node> visited) {
    if (visited.containsKey(node)) {
        return visited.get(node);
    }
    
    // Create clone
    Node clone = new Node(node.val);
    visited.put(node, clone);
    
    // Clone neighbors
    for (Node neighbor : node.neighbors) {
        clone.neighbors.add(dfsClone(neighbor, visited));
    }
    
    return clone;
}
```

---

### Approach 2: BFS with HashMap

**Algorithm:**
1. Use HashMap for original → cloned mapping
2. BFS queue to process nodes level by level
3. Clone each node and its neighbors
4. Avoid revisiting using HashMap

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public Node cloneGraphBFS(Node node) {
    if (node == null) return null;
    
    Map<Node, Node> visited = new HashMap<>();
    Queue<Node> queue = new LinkedList<>();
    
    // Clone root
    Node clone = new Node(node.val);
    visited.put(node, clone);
    queue.offer(node);
    
    while (!queue.isEmpty()) {
        Node current = queue.poll();
        
        for (Node neighbor : current.neighbors) {
            if (!visited.containsKey(neighbor)) {
                // Clone neighbor
                visited.put(neighbor, new Node(neighbor.val));
                queue.offer(neighbor);
            }
            
            // Connect cloned nodes
            visited.get(current).neighbors.add(visited.get(neighbor));
        }
    }
    
    return clone;
}
```

---

## Complete Implementation

```java
import java.util.*;

public class CloneGraph {
    
    static class Node {
        public int val;
        public List<Node> neighbors;
        
        public Node(int val) {
            this.val = val;
            neighbors = new ArrayList<>();
        }
        
        public Node(int val, List<Node> neighbors) {
            this.val = val;
            this.neighbors = neighbors;
        }
        
        @Override
        public String toString() {
            return "Node " + val;
        }
    }
    
    // Approach 1: DFS with HashMap
    public static Node cloneGraphDFS(Node node) {
        if (node == null) return null;
        return dfsClone(node, new HashMap<>());
    }
    
    private static Node dfsClone(Node node, Map<Node, Node> visited) {
        if (visited.containsKey(node)) {
            return visited.get(node);
        }
        
        Node clone = new Node(node.val);
        visited.put(node, clone);
        
        for (Node neighbor : node.neighbors) {
            clone.neighbors.add(dfsClone(neighbor, visited));
        }
        
        return clone;
    }
    
    // Approach 2: BFS with HashMap
    public static Node cloneGraphBFS(Node node) {
        if (node == null) return null;
        
        Map<Node, Node> visited = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        
        Node clone = new Node(node.val);
        visited.put(node, clone);
        queue.offer(node);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            for (Node neighbor : current.neighbors) {
                if (!visited.containsKey(neighbor)) {
                    visited.put(neighbor, new Node(neighbor.val));
                    queue.offer(neighbor);
                }
                visited.get(current).neighbors.add(visited.get(neighbor));
            }
        }
        
        return clone;
    }
    
    // Helper: Build graph from adjacency list
    public static Node buildGraph(int[][] adjList) {
        if (adjList == null || adjList.length == 0) return null;
        
        int n = adjList.length;
        Node[] nodes = new Node[n + 1];
        
        // Create all nodes
        for (int i = 1; i <= n; i++) {
            nodes[i] = new Node(i);
        }
        
        // Connect neighbors
        for (int i = 0; i < n; i++) {
            for (int neighbor : adjList[i]) {
                nodes[i + 1].neighbors.add(nodes[neighbor]);
            }
        }
        
        return nodes[1];
    }
    
    // Helper: Convert graph to adjacency list
    public static List<List<Integer>> graphToAdjList(Node node) {
        if (node == null) return new ArrayList<>();
        
        Map<Node, Integer> nodeToIndex = new HashMap<>();
        List<Node> nodes = new ArrayList<>();
        
        // BFS to collect all nodes
        Queue<Node> queue = new LinkedList<>();
        Set<Node> visited = new HashSet<>();
        queue.offer(node);
        visited.add(node);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            nodeToIndex.put(current, nodes.size());
            nodes.add(current);
            
            for (Node neighbor : current.neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        
        // Build adjacency list
        List<List<Integer>> adjList = new ArrayList<>();
        for (Node n : nodes) {
            List<Integer> neighbors = new ArrayList<>();
            for (Node neighbor : n.neighbors) {
                neighbors.add(nodeToIndex.get(neighbor) + 1);
            }
            adjList.add(neighbors);
        }
        
        return adjList;
    }
    
    // Helper: Verify if two graphs are identical
    public static boolean areGraphsIdentical(Node node1, Node node2) {
        if (node1 == null && node2 == null) return true;
        if (node1 == null || node2 == null) return false;
        
        Map<Node, Node> mapping = new HashMap<>();
        Queue<Node> queue1 = new LinkedList<>();
        Queue<Node> queue2 = new LinkedList<>();
        
        queue1.offer(node1);
        queue2.offer(node2);
        mapping.put(node1, node2);
        
        while (!queue1.isEmpty()) {
            Node current1 = queue1.poll();
            Node current2 = queue2.poll();
            
            if (current1.val != current2.val) return false;
            if (current1.neighbors.size() != current2.neighbors.size()) return false;
            
            for (int i = 0; i < current1.neighbors.size(); i++) {
                Node neighbor1 = current1.neighbors.get(i);
                Node neighbor2 = current2.neighbors.get(i);
                
                if (!mapping.containsKey(neighbor1)) {
                    mapping.put(neighbor1, neighbor2);
                    queue1.offer(neighbor1);
                    queue2.offer(neighbor2);
                } else if (mapping.get(neighbor1) != neighbor2) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Helper: Check if clone is deep copy (not shallow)
    public static boolean isDeepCopy(Node original, Node clone) {
        if (original == null && clone == null) return true;
        if (original == null || clone == null) return false;
        if (original == clone) return false; // Same reference = shallow copy
        
        Set<Node> visitedOriginal = new HashSet<>();
        Set<Node> visitedClone = new HashSet<>();
        
        Queue<Node> queueOriginal = new LinkedList<>();
        Queue<Node> queueClone = new LinkedList<>();
        
        queueOriginal.offer(original);
        queueClone.offer(clone);
        visitedOriginal.add(original);
        visitedClone.add(clone);
        
        while (!queueOriginal.isEmpty()) {
            Node currOriginal = queueOriginal.poll();
            Node currClone = queueClone.poll();
            
            if (currOriginal == currClone) return false; // Same reference
            
            for (int i = 0; i < currOriginal.neighbors.size(); i++) {
                Node neighborOriginal = currOriginal.neighbors.get(i);
                Node neighborClone = currClone.neighbors.get(i);
                
                if (neighborOriginal == neighborClone) return false;
                
                if (!visitedOriginal.contains(neighborOriginal)) {
                    visitedOriginal.add(neighborOriginal);
                    visitedClone.add(neighborClone);
                    queueOriginal.offer(neighborOriginal);
                    queueClone.offer(neighborClone);
                }
            }
        }
        
        return true;
    }
    
    // Test cases
    public static boolean doTestsPass() {
        // Test 1: Simple graph
        int[][] adjList1 = {{2, 4}, {1, 3}, {2, 4}, {1, 3}};
        Node graph1 = buildGraph(adjList1);
        Node clone1 = cloneGraphDFS(graph1);
        if (!areGraphsIdentical(graph1, clone1)) return false;
        if (!isDeepCopy(graph1, clone1)) return false;
        
        // Test 2: Single node
        int[][] adjList2 = {{}};
        Node graph2 = buildGraph(adjList2);
        Node clone2 = cloneGraphBFS(graph2);
        if (clone2 == null || clone2.val != 1) return false;
        
        // Test 3: Null graph
        Node clone3 = cloneGraphDFS(null);
        if (clone3 != null) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: Clone social network graph
        System.out.println("=== Clone Social Network Graph ===\n");
        
        // Build original graph
        Node user1 = new Node(1);
        Node user2 = new Node(2);
        Node user3 = new Node(3);
        Node user4 = new Node(4);
        
        user1.neighbors.addAll(Arrays.asList(user2, user4));
        user2.neighbors.addAll(Arrays.asList(user1, user3));
        user3.neighbors.addAll(Arrays.asList(user2, user4));
        user4.neighbors.addAll(Arrays.asList(user1, user3));
        
        System.out.println("Original Graph:");
        System.out.println("  User 1 → Friends: " + user1.neighbors);
        System.out.println("  User 2 → Friends: " + user2.neighbors);
        System.out.println("  User 3 → Friends: " + user3.neighbors);
        System.out.println("  User 4 → Friends: " + user4.neighbors);
        
        // Clone using DFS
        System.out.println("\n--- Cloning with DFS ---");
        Node clonedDFS = cloneGraphDFS(user1);
        System.out.println("✓ Graph cloned successfully");
        System.out.println("Is deep copy? " + isDeepCopy(user1, clonedDFS));
        System.out.println("Are graphs identical? " + areGraphsIdentical(user1, clonedDFS));
        
        // Clone using BFS
        System.out.println("\n--- Cloning with BFS ---");
        Node clonedBFS = cloneGraphBFS(user1);
        System.out.println("✓ Graph cloned successfully");
        System.out.println("Is deep copy? " + isDeepCopy(user1, clonedBFS));
        System.out.println("Are graphs identical? " + areGraphsIdentical(user1, clonedBFS));
        
        // Verify independence
        System.out.println("\n--- Verify Independence ---");
        System.out.println("Original user1 reference: " + System.identityHashCode(user1));
        System.out.println("Cloned user1 reference: " + System.identityHashCode(clonedDFS));
        System.out.println("References are different: " + (user1 != clonedDFS));
        
        // Modify original
        user1.neighbors.clear();
        System.out.println("\nAfter clearing original user1's friends:");
        System.out.println("  Original user1 friends: " + user1.neighbors.size());
        System.out.println("  Cloned user1 friends: " + clonedDFS.neighbors.size());
        System.out.println("✓ Clone is independent of original");
    }
}
```

---

## Algorithm Walkthrough

### Example: Clone graph with 4 nodes

```
Original Graph:
    1 --- 2
    |     |
    4 --- 3

DFS Cloning Process:

Initial call: cloneGraphDFS(node1)
  visited = {}

Step 1: Clone node 1
  clone1 = new Node(1)
  visited = {node1 → clone1}
  Process neighbors: [node2, node4]

Step 2: Clone node 2 (recursive call)
  clone2 = new Node(2)
  visited = {node1 → clone1, node2 → clone2}
  Process neighbors: [node1, node3]
    - node1 already visited → return clone1
    
Step 3: Clone node 3 (recursive call)
  clone3 = new Node(3)
  visited = {node1 → clone1, node2 → clone2, node3 → clone3}
  Process neighbors: [node2, node4]
    - node2 already visited → return clone2
    
Step 4: Clone node 4 (recursive call)
  clone4 = new Node(4)
  visited = {node1 → clone1, node2 → clone2, node3 → clone3, node4 → clone4}
  Process neighbors: [node1, node3]
    - Both already visited → return clones

Final cloned graph:
    clone1 --- clone2
    |          |
    clone4 --- clone3

All connections preserved, but new object references
```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| DFS | O(V + E) | O(V) | Recursion stack + HashMap |
| BFS | O(V + E) | O(V) | Queue + HashMap |
| Naive (no HashMap) | O(V²) | O(V) | Revisits nodes |

---

## Real-World Applications

1. **Version Control** - Git branch creation
2. **Game Development** - Save/load game state
3. **Distributed Systems** - Data replication
4. **Testing** - Create test fixtures
5. **Undo/Redo** - State snapshots
6. **Caching** - Clone data structures

---

## Common Pitfalls

### 1. Shallow Copy (Wrong)
```java
// This creates shallow copy - shares references!
Node clone = new Node(node.val);
clone.neighbors = node.neighbors; // ✗ Wrong
```

### 2. Missing HashMap (Infinite Loop)
```java
// Without HashMap, cycles cause infinite recursion
Node clone(Node node) {
    Node copy = new Node(node.val);
    for (Node n : node.neighbors) {
        copy.neighbors.add(clone(n)); // ✗ Infinite loop on cycles
    }
    return copy;
}
```

### 3. Not Handling Null
```java
// Always check for null input
if (node == null) return null;
```

---

## Extensions

### 1. Clone Directed Graph
```java
// Same algorithm works for directed graphs
// Just don't add reverse edges
```

### 2. Clone Weighted Graph
```java
class WeightedNode {
    int val;
    List<Edge> neighbors;
}

class Edge {
    WeightedNode node;
    int weight;
}
```

### 3. Clone with Metadata
```java
// Preserve additional node properties
class Node {
    int val;
    List<Node> neighbors;
    Map<String, Object> metadata;
}
```
