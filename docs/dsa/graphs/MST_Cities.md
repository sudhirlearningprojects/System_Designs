# Minimum Cost to Connect Cities - MST

## Problem Statement

**Real-World Context**: Telecom companies use MST to lay fiber optic cables connecting cities with minimum total cost. Power companies use it to design electrical grids. Network engineers use it to optimize network topology.

Given `n` cities and costs to build roads between them, find the minimum cost to connect all cities. Some cities may already be connected.

**Input**: 
- `n`: Number of cities
- `connections`: List of [city1, city2, cost] tuples
- `existingConnections`: Already built roads (optional)

**Output**: Minimum total cost to connect all cities, or -1 if impossible

**Examples:**
```
Example 1:
Input: n = 3, connections = [[1,2,5],[1,3,6],[2,3,1]]
Output: 6

Explanation:
  Cities: 1, 2, 3
  Possible roads:
    1-2: cost 5
    1-3: cost 6
    2-3: cost 1
  
  Optimal solution:
    Build 2-3 (cost 1)
    Build 1-2 (cost 5)
    Total: 6

Example 2:
Input: n = 4, connections = [[1,2,3],[3,4,4]]
Output: -1

Explanation: Cannot connect city 2 to cities 3 and 4
```

---

## Solution Approaches

### Approach 1: Kruskal's Algorithm (Edge-based)

**Algorithm:**
1. Sort all edges by cost (ascending)
2. Use Union-Find to detect cycles
3. Add edges that don't create cycles
4. Stop when all nodes connected (n-1 edges)

**Time Complexity:** O(E log E)  
**Space Complexity:** O(V)

```java
public int minimumCostKruskal(int n, int[][] connections) {
    // Sort edges by cost
    Arrays.sort(connections, (a, b) -> Integer.compare(a[2], b[2]));
    
    UnionFind uf = new UnionFind(n + 1);
    int totalCost = 0;
    int edgesUsed = 0;
    
    for (int[] conn : connections) {
        int city1 = conn[0];
        int city2 = conn[1];
        int cost = conn[2];
        
        // Add edge if it doesn't create cycle
        if (uf.union(city1, city2)) {
            totalCost += cost;
            edgesUsed++;
            
            if (edgesUsed == n - 1) {
                return totalCost;
            }
        }
    }
    
    return edgesUsed == n - 1 ? totalCost : -1;
}

class UnionFind {
    private int[] parent;
    private int[] rank;
    
    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }
    
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }
    
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        
        if (rootX == rootY) return false; // Already connected
        
        // Union by rank
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        
        return true;
    }
}
```

---

### Approach 2: Prim's Algorithm (Vertex-based)

**Algorithm:**
1. Start from any city
2. Use min-heap to track cheapest edge to unvisited cities
3. Add cheapest edge and mark city as visited
4. Repeat until all cities visited

**Time Complexity:** O((V + E) log V)  
**Space Complexity:** O(V + E)

```java
public int minimumCostPrim(int n, int[][] connections) {
    // Build adjacency list
    Map<Integer, List<int[]>> graph = new HashMap<>();
    for (int i = 1; i <= n; i++) {
        graph.put(i, new ArrayList<>());
    }
    
    for (int[] conn : connections) {
        graph.get(conn[0]).add(new int[]{conn[1], conn[2]});
        graph.get(conn[1]).add(new int[]{conn[0], conn[2]});
    }
    
    // Prim's algorithm
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
    Set<Integer> visited = new HashSet<>();
    
    pq.offer(new int[]{1, 0}); // Start from city 1
    int totalCost = 0;
    
    while (!pq.isEmpty() && visited.size() < n) {
        int[] current = pq.poll();
        int city = current[0];
        int cost = current[1];
        
        if (visited.contains(city)) continue;
        
        visited.add(city);
        totalCost += cost;
        
        for (int[] neighbor : graph.get(city)) {
            if (!visited.contains(neighbor[0])) {
                pq.offer(neighbor);
            }
        }
    }
    
    return visited.size() == n ? totalCost : -1;
}
```

---

## Complete Implementation

```java
import java.util.*;

public class MinimumCostConnectCities {
    
    static class Edge {
        int city1, city2, cost;
        
        public Edge(int city1, int city2, int cost) {
            this.city1 = city1;
            this.city2 = city2;
            this.cost = cost;
        }
        
        @Override
        public String toString() {
            return String.format("City %d ↔ City %d (cost: %d)", city1, city2, cost);
        }
    }
    
    static class MSTResult {
        int totalCost;
        List<Edge> edges;
        boolean possible;
        
        public MSTResult(int totalCost, List<Edge> edges, boolean possible) {
            this.totalCost = totalCost;
            this.edges = edges;
            this.possible = possible;
        }
    }
    
    // Kruskal's Algorithm with detailed result
    public static MSTResult minimumCostKruskal(int n, int[][] connections) {
        List<Edge> allEdges = new ArrayList<>();
        for (int[] conn : connections) {
            allEdges.add(new Edge(conn[0], conn[1], conn[2]));
        }
        
        // Sort by cost
        allEdges.sort(Comparator.comparingInt(e -> e.cost));
        
        UnionFind uf = new UnionFind(n + 1);
        List<Edge> mstEdges = new ArrayList<>();
        int totalCost = 0;
        
        for (Edge edge : allEdges) {
            if (uf.union(edge.city1, edge.city2)) {
                mstEdges.add(edge);
                totalCost += edge.cost;
                
                if (mstEdges.size() == n - 1) {
                    break;
                }
            }
        }
        
        boolean possible = mstEdges.size() == n - 1;
        return new MSTResult(totalCost, mstEdges, possible);
    }
    
    // Prim's Algorithm with detailed result
    public static MSTResult minimumCostPrim(int n, int[][] connections) {
        Map<Integer, List<int[]>> graph = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            graph.put(i, new ArrayList<>());
        }
        
        for (int[] conn : connections) {
            graph.get(conn[0]).add(new int[]{conn[1], conn[2]});
            graph.get(conn[1]).add(new int[]{conn[0], conn[2]});
        }
        
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]);
        Set<Integer> visited = new HashSet<>();
        List<Edge> mstEdges = new ArrayList<>();
        int totalCost = 0;
        
        // Start from city 1
        visited.add(1);
        for (int[] neighbor : graph.get(1)) {
            pq.offer(new int[]{1, neighbor[0], neighbor[1]});
        }
        
        while (!pq.isEmpty() && visited.size() < n) {
            int[] current = pq.poll();
            int from = current[0];
            int to = current[1];
            int cost = current[2];
            
            if (visited.contains(to)) continue;
            
            visited.add(to);
            mstEdges.add(new Edge(from, to, cost));
            totalCost += cost;
            
            for (int[] neighbor : graph.get(to)) {
                if (!visited.contains(neighbor[0])) {
                    pq.offer(new int[]{to, neighbor[0], neighbor[1]});
                }
            }
        }
        
        boolean possible = visited.size() == n;
        return new MSTResult(totalCost, mstEdges, possible);
    }
    
    // Find second best MST (for redundancy planning)
    public static MSTResult findSecondBestMST(int n, int[][] connections) {
        MSTResult bestMST = minimumCostKruskal(n, connections);
        if (!bestMST.possible) return bestMST;
        
        int secondBestCost = Integer.MAX_VALUE;
        List<Edge> secondBestEdges = null;
        
        // Try removing each MST edge and finding alternative
        for (Edge removedEdge : bestMST.edges) {
            List<int[]> modifiedConnections = new ArrayList<>();
            for (int[] conn : connections) {
                if (!((conn[0] == removedEdge.city1 && conn[1] == removedEdge.city2) ||
                      (conn[0] == removedEdge.city2 && conn[1] == removedEdge.city1))) {
                    modifiedConnections.add(conn);
                }
            }
            
            MSTResult alternative = minimumCostKruskal(n, 
                modifiedConnections.toArray(new int[0][]));
            
            if (alternative.possible && alternative.totalCost < secondBestCost) {
                secondBestCost = alternative.totalCost;
                secondBestEdges = alternative.edges;
            }
        }
        
        return new MSTResult(secondBestCost, secondBestEdges, secondBestEdges != null);
    }
    
    static class UnionFind {
        private int[] parent;
        private int[] rank;
        
        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }
        
        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }
        
        public boolean union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            if (rootX == rootY) return false;
            
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
            
            return true;
        }
        
        public boolean isConnected(int x, int y) {
            return find(x) == find(y);
        }
    }
    
    // Test cases
    public static boolean doTestsPass() {
        // Test 1: Simple case
        int[][] conn1 = {{1,2,5},{1,3,6},{2,3,1}};
        MSTResult result1 = minimumCostKruskal(3, conn1);
        if (result1.totalCost != 6) return false;
        
        // Test 2: Disconnected graph
        int[][] conn2 = {{1,2,3},{3,4,4}};
        MSTResult result2 = minimumCostKruskal(4, conn2);
        if (result2.possible) return false;
        
        // Test 3: Already optimal
        int[][] conn3 = {{1,2,1},{2,3,1},{3,4,1}};
        MSTResult result3 = minimumCostKruskal(4, conn3);
        if (result3.totalCost != 3) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: Fiber optic network planning
        System.out.println("=== Fiber Optic Network Planning ===\n");
        
        int n = 6;
        int[][] connections = {
            {1, 2, 10},  // City 1 - City 2: $10M
            {1, 3, 15},  // City 1 - City 3: $15M
            {2, 3, 7},   // City 2 - City 3: $7M
            {2, 4, 12},  // City 2 - City 4: $12M
            {3, 4, 8},   // City 3 - City 4: $8M
            {3, 5, 9},   // City 3 - City 5: $9M
            {4, 5, 5},   // City 4 - City 5: $5M
            {4, 6, 11},  // City 4 - City 6: $11M
            {5, 6, 6}    // City 5 - City 6: $6M
        };
        
        System.out.println("Cities to connect: " + n);
        System.out.println("Available connections: " + connections.length);
        
        // Kruskal's algorithm
        System.out.println("\n--- Kruskal's Algorithm (Edge-based) ---");
        MSTResult kruskalResult = minimumCostKruskal(n, connections);
        
        if (kruskalResult.possible) {
            System.out.println("✓ All cities can be connected");
            System.out.println("Total cost: $" + kruskalResult.totalCost + "M");
            System.out.println("\nCables to install:");
            for (Edge edge : kruskalResult.edges) {
                System.out.println("  " + edge);
            }
        } else {
            System.out.println("✗ Cannot connect all cities");
        }
        
        // Prim's algorithm
        System.out.println("\n--- Prim's Algorithm (Vertex-based) ---");
        MSTResult primResult = minimumCostPrim(n, connections);
        
        if (primResult.possible) {
            System.out.println("✓ All cities can be connected");
            System.out.println("Total cost: $" + primResult.totalCost + "M");
            System.out.println("\nCables to install:");
            for (Edge edge : primResult.edges) {
                System.out.println("  " + edge);
            }
        }
        
        // Second best MST (for redundancy)
        System.out.println("\n--- Backup Plan (Second Best MST) ---");
        MSTResult secondBest = findSecondBestMST(n, connections);
        
        if (secondBest.possible) {
            System.out.println("Backup plan cost: $" + secondBest.totalCost + "M");
            System.out.println("Additional cost: $" + (secondBest.totalCost - kruskalResult.totalCost) + "M");
        }
        
        // Cost analysis
        int totalPossibleCost = Arrays.stream(connections)
            .mapToInt(c -> c[2])
            .sum();
        
        System.out.println("\n📊 Cost Analysis:");
        System.out.println("  Total if all cables installed: $" + totalPossibleCost + "M");
        System.out.println("  Optimal MST cost: $" + kruskalResult.totalCost + "M");
        System.out.println("  Savings: $" + (totalPossibleCost - kruskalResult.totalCost) + "M");
        System.out.println("  Efficiency: " + 
            String.format("%.1f%%", (1 - kruskalResult.totalCost / (double) totalPossibleCost) * 100));
    }
}
```

---

## Algorithm Comparison

| Aspect | Kruskal's | Prim's |
|--------|-----------|--------|
| Approach | Edge-based | Vertex-based |
| Data Structure | Union-Find | Min-Heap |
| Time Complexity | O(E log E) | O((V + E) log V) |
| Best for | Sparse graphs | Dense graphs |
| Implementation | Simpler | More complex |
| Parallelizable | Yes (sort edges) | Harder |

---

## Real-World Applications

1. **Telecom Networks** - Fiber optic cable layout
2. **Power Grids** - Electrical transmission lines
3. **Water Supply** - Pipeline networks
4. **Transportation** - Road/railway networks
5. **Computer Networks** - LAN topology design
6. **Circuit Design** - PCB trace routing

---

## Extensions

### 1. Degree-Constrained MST
```java
// Each city can have at most k connections
public MSTResult degreeConstrainedMST(int n, int[][] connections, int maxDegree) {
    // Modified Kruskal's with degree tracking
}
```

### 2. Capacitated MST
```java
// Each edge has a capacity limit
public MSTResult capacitatedMST(int n, int[][] connections, int[] demands) {
    // Consider both cost and capacity
}
```

### 3. Dynamic MST
```java
// Update MST as edges are added/removed
public void addEdge(int city1, int city2, int cost) {
    // Incrementally update MST
}
```

### 4. Steiner Tree
```java
// Connect subset of cities (not all)
public MSTResult steinerTree(int n, int[][] connections, Set<Integer> requiredCities) {
    // NP-hard problem, use approximation
}
```
