# Critical Connections in Network - Find Bridges

## Problem Statement

**Real-World Context**: Network engineers use this to identify single points of failure in computer networks, power grids, and transportation systems. AWS uses it to ensure no single server failure disconnects the network.

Given a network of servers connected by cables, find all "critical connections" (bridges) - connections whose removal would disconnect the network.

**Input**: 
- `n`: Number of servers
- `connections`: List of [server1, server2] pairs

**Output**: List of critical connections

**Examples:**
```
Example 1:
Input: n = 4, connections = [[0,1],[1,2],[2,0],[1,3]]
Output: [[1,3]]

Network:
    0 --- 1 --- 3
     \   /
      \ /
       2

Explanation: Removing [1,3] disconnects server 3 from the network.
The triangle 0-1-2 has redundant paths, so no edge there is critical.

Example 2:
Input: n = 6, connections = [[0,1],[1,2],[2,3],[3,4],[4,5],[5,3]]
Output: [[0,1],[1,2]]

Network:
    0 --- 1 --- 2 --- 3 --- 4
                       \   /
                        \ /
                         5

Explanation: Removing [0,1] or [1,2] disconnects parts of the network.
The cycle 3-4-5 has redundancy.
```

---

## Solution: Tarjan's Bridge-Finding Algorithm

**Algorithm:**
1. Perform DFS and assign discovery time to each node
2. Track the lowest discovery time reachable from each node
3. An edge (u, v) is a bridge if: `low[v] > discovery[u]`
4. This means v cannot reach any ancestor of u without using edge (u, v)

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

**Key Insight:**
- A bridge is an edge that's not part of any cycle
- If removing an edge increases the number of connected components, it's a bridge

---

## Complete Implementation

```java
import java.util.*;

public class CriticalConnections {
    
    static class NetworkGraph {
        private Map<Integer, List<Integer>> graph;
        private int time;
        
        public NetworkGraph(int n) {
            graph = new HashMap<>();
            for (int i = 0; i < n; i++) {
                graph.put(i, new ArrayList<>());
            }
            time = 0;
        }
        
        public void addConnection(int server1, int server2) {
            graph.get(server1).add(server2);
            graph.get(server2).add(server1);
        }
        
        // Tarjan's Algorithm - Find all bridges
        public List<List<Integer>> findCriticalConnections(int n) {
            List<List<Integer>> bridges = new ArrayList<>();
            int[] discovery = new int[n];
            int[] low = new int[n];
            boolean[] visited = new boolean[n];
            
            Arrays.fill(discovery, -1);
            Arrays.fill(low, -1);
            
            // Handle disconnected components
            for (int i = 0; i < n; i++) {
                if (!visited[i]) {
                    dfs(i, -1, discovery, low, visited, bridges);
                }
            }
            
            return bridges;
        }
        
        private void dfs(int u, int parent, int[] discovery, int[] low, 
                        boolean[] visited, List<List<Integer>> bridges) {
            visited[u] = true;
            discovery[u] = low[u] = time++;
            
            for (int v : graph.get(u)) {
                if (v == parent) continue; // Skip parent edge
                
                if (!visited[v]) {
                    // Tree edge - explore child
                    dfs(v, u, discovery, low, visited, bridges);
                    
                    // Update low value after returning from child
                    low[u] = Math.min(low[u], low[v]);
                    
                    // Check if edge (u, v) is a bridge
                    if (low[v] > discovery[u]) {
                        bridges.add(Arrays.asList(u, v));
                    }
                } else {
                    // Back edge - update low value
                    low[u] = Math.min(low[u], discovery[v]);
                }
            }
        }
        
        // Find articulation points (critical servers)
        public List<Integer> findArticulationPoints(int n) {
            Set<Integer> articulationPoints = new HashSet<>();
            int[] discovery = new int[n];
            int[] low = new int[n];
            boolean[] visited = new boolean[n];
            int[] parent = new int[n];
            
            Arrays.fill(discovery, -1);
            Arrays.fill(low, -1);
            Arrays.fill(parent, -1);
            
            for (int i = 0; i < n; i++) {
                if (!visited[i]) {
                    findAP(i, discovery, low, visited, parent, articulationPoints);
                }
            }
            
            return new ArrayList<>(articulationPoints);
        }
        
        private void findAP(int u, int[] discovery, int[] low, boolean[] visited,
                           int[] parent, Set<Integer> articulationPoints) {
            visited[u] = true;
            discovery[u] = low[u] = time++;
            int children = 0;
            
            for (int v : graph.get(u)) {
                if (!visited[v]) {
                    children++;
                    parent[v] = u;
                    findAP(v, discovery, low, visited, parent, articulationPoints);
                    
                    low[u] = Math.min(low[u], low[v]);
                    
                    // Check if u is articulation point
                    if (parent[u] == -1 && children > 1) {
                        // Root with multiple children
                        articulationPoints.add(u);
                    }
                    if (parent[u] != -1 && low[v] >= discovery[u]) {
                        // Non-root with child that can't reach ancestor
                        articulationPoints.add(u);
                    }
                } else if (v != parent[u]) {
                    low[u] = Math.min(low[u], discovery[v]);
                }
            }
        }
        
        // Check if network remains connected after removing an edge
        public boolean isConnectedAfterRemoval(int n, int u, int v) {
            // Temporarily remove edge
            graph.get(u).remove(Integer.valueOf(v));
            graph.get(v).remove(Integer.valueOf(u));
            
            boolean connected = isConnected(n);
            
            // Restore edge
            graph.get(u).add(v);
            graph.get(v).add(u);
            
            return connected;
        }
        
        private boolean isConnected(int n) {
            boolean[] visited = new boolean[n];
            bfs(0, visited);
            
            for (boolean v : visited) {
                if (!v) return false;
            }
            return true;
        }
        
        private void bfs(int start, boolean[] visited) {
            Queue<Integer> queue = new LinkedList<>();
            queue.offer(start);
            visited[start] = true;
            
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : graph.get(u)) {
                    if (!visited[v]) {
                        visited[v] = true;
                        queue.offer(v);
                    }
                }
            }
        }
        
        // Find redundant connections (edges that can be removed)
        public List<List<Integer>> findRedundantConnections(int n) {
            List<List<Integer>> redundant = new ArrayList<>();
            List<List<Integer>> bridges = findCriticalConnections(n);
            Set<String> bridgeSet = new HashSet<>();
            
            for (List<Integer> bridge : bridges) {
                bridgeSet.add(bridge.get(0) + "-" + bridge.get(1));
                bridgeSet.add(bridge.get(1) + "-" + bridge.get(0));
            }
            
            for (int u : graph.keySet()) {
                for (int v : graph.get(u)) {
                    if (u < v) { // Avoid duplicates
                        String edge = u + "-" + v;
                        if (!bridgeSet.contains(edge)) {
                            redundant.add(Arrays.asList(u, v));
                        }
                    }
                }
            }
            
            return redundant;
        }
    }
    
    // Test cases
    public static boolean doTestsPass() {
        // Test 1: Simple bridge
        NetworkGraph net1 = new NetworkGraph(4);
        net1.addConnection(0, 1);
        net1.addConnection(1, 2);
        net1.addConnection(2, 0);
        net1.addConnection(1, 3);
        
        List<List<Integer>> bridges1 = net1.findCriticalConnections(4);
        if (bridges1.size() != 1) return false;
        
        // Test 2: No bridges (complete cycle)
        NetworkGraph net2 = new NetworkGraph(4);
        net2.addConnection(0, 1);
        net2.addConnection(1, 2);
        net2.addConnection(2, 3);
        net2.addConnection(3, 0);
        
        List<List<Integer>> bridges2 = net2.findCriticalConnections(4);
        if (bridges2.size() != 0) return false;
        
        // Test 3: Linear chain (all bridges)
        NetworkGraph net3 = new NetworkGraph(4);
        net3.addConnection(0, 1);
        net3.addConnection(1, 2);
        net3.addConnection(2, 3);
        
        List<List<Integer>> bridges3 = net3.findCriticalConnections(4);
        if (bridges3.size() != 3) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: Data center network analysis
        System.out.println("=== Data Center Network Analysis ===\n");
        
        int n = 7;
        NetworkGraph datacenter = new NetworkGraph(n);
        
        // Build network topology
        datacenter.addConnection(0, 1);  // Server 0 - Server 1
        datacenter.addConnection(1, 2);  // Server 1 - Server 2
        datacenter.addConnection(2, 3);  // Server 2 - Server 3
        datacenter.addConnection(3, 4);  // Server 3 - Server 4
        datacenter.addConnection(4, 5);  // Server 4 - Server 5
        datacenter.addConnection(5, 3);  // Server 5 - Server 3 (creates cycle)
        datacenter.addConnection(2, 6);  // Server 2 - Server 6
        
        System.out.println("Network topology:");
        System.out.println("  0 --- 1 --- 2 --- 3 --- 4");
        System.out.println("              |     \\   /");
        System.out.println("              6      \\ /");
        System.out.println("                      5");
        
        // Find critical connections
        List<List<Integer>> critical = datacenter.findCriticalConnections(n);
        System.out.println("\n🔴 Critical Connections (Single Points of Failure):");
        for (List<Integer> conn : critical) {
            System.out.println("  Server " + conn.get(0) + " ↔ Server " + conn.get(1));
        }
        
        // Find articulation points
        List<Integer> criticalServers = datacenter.findArticulationPoints(n);
        System.out.println("\n🔴 Critical Servers (Articulation Points):");
        for (int server : criticalServers) {
            System.out.println("  Server " + server);
        }
        
        // Find redundant connections
        List<List<Integer>> redundant = datacenter.findRedundantConnections(n);
        System.out.println("\n🟢 Redundant Connections (Can be removed safely):");
        for (List<Integer> conn : redundant) {
            System.out.println("  Server " + conn.get(0) + " ↔ Server " + conn.get(1));
        }
        
        // Recommendations
        System.out.println("\n📊 Network Resilience Report:");
        System.out.println("  Total connections: " + (critical.size() + redundant.size()));
        System.out.println("  Critical connections: " + critical.size());
        System.out.println("  Redundant connections: " + redundant.size());
        System.out.println("  Redundancy ratio: " + 
            String.format("%.1f%%", (redundant.size() * 100.0) / (critical.size() + redundant.size())));
        
        if (critical.size() > 0) {
            System.out.println("\n⚠️  Recommendation: Add backup connections for critical links");
            System.out.println("   to improve network fault tolerance.");
        }
    }
}
```

---

## Algorithm Walkthrough

### Example: Find bridges in network

```
Network:
    0 --- 1 --- 3
     \   /
      \ /
       2

Connections: [[0,1], [1,2], [2,0], [1,3]]

DFS Traversal with Tarjan's Algorithm:

Initial state:
  time = 0
  discovery = [-1, -1, -1, -1]
  low = [-1, -1, -1, -1]
  visited = [false, false, false, false]

Start DFS from node 0:

Visit 0:
  discovery[0] = low[0] = 0, time = 1
  Explore neighbor 1:
  
  Visit 1:
    discovery[1] = low[1] = 1, time = 2
    Explore neighbor 2:
    
    Visit 2:
      discovery[2] = low[2] = 2, time = 3
      Explore neighbor 0:
        Already visited, back edge
        low[2] = min(low[2], discovery[0]) = min(2, 0) = 0
      
      Return to 1:
        low[1] = min(low[1], low[2]) = min(1, 0) = 0
        Check: low[2] > discovery[1]? → 0 > 1? NO (not a bridge)
    
    Explore neighbor 3:
    
    Visit 3:
      discovery[3] = low[3] = 3, time = 4
      No unvisited neighbors
      
      Return to 1:
        low[1] = min(low[1], low[3]) = min(0, 3) = 0
        Check: low[3] > discovery[1]? → 3 > 1? YES ✓
        Bridge found: [1, 3]

Final state:
  discovery = [0, 1, 2, 3]
  low = [0, 0, 0, 3]
  bridges = [[1, 3]]

Explanation:
  - Nodes 0, 1, 2 form a cycle (low values all 0)
  - Node 3 can only reach the cycle through node 1
  - Edge [1, 3] is a bridge
```

---

## Complexity Analysis

| Operation | Time | Space |
|-----------|------|-------|
| Tarjan's algorithm | O(V + E) | O(V) |
| Find articulation points | O(V + E) | O(V) |
| Brute force (remove each edge) | O(E × (V + E)) | O(V) |

**Why Tarjan's is optimal:**
- Single DFS traversal
- No need to test each edge individually
- Linear time complexity

---

## Real-World Applications

1. **Network Infrastructure** - Identify single points of failure
2. **AWS/Cloud** - Ensure redundant connections between data centers
3. **Power Grids** - Find critical transmission lines
4. **Transportation** - Identify critical roads/bridges
5. **Social Networks** - Find influential connectors
6. **Circuit Design** - Detect critical components

---

## Related Problems

### 1. Articulation Points (Critical Nodes)
```java
// Nodes whose removal disconnects the graph
// Similar to bridges but for vertices
```

### 2. Strongly Connected Components
```java
// Find groups of nodes with paths in both directions
// Uses Tarjan's or Kosaraju's algorithm
```

### 3. Network Redundancy Score
```java
public double calculateRedundancy(int n) {
    List<List<Integer>> bridges = findCriticalConnections(n);
    int totalEdges = countEdges();
    return 1.0 - (bridges.size() / (double) totalEdges);
}
```

---

## Optimizations

### 1. Parallel Bridge Detection
```java
// Divide graph into components and process in parallel
public List<List<Integer>> parallelBridgeFinding(int n) {
    // Use fork-join framework for large graphs
}
```

### 2. Dynamic Bridge Detection
```java
// Update bridges incrementally as edges are added/removed
// Useful for real-time network monitoring
```

### 3. Weighted Bridges
```java
// Prioritize bridges by their importance (traffic, capacity)
public List<WeightedBridge> findCriticalByImportance(int n) {
    // Consider edge weights in criticality calculation
}
```
