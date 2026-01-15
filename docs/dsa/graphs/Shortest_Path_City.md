# Shortest Path in City - GPS Navigation

## Problem Statement

**Real-World Context**: Google Maps, Uber, and delivery services use Dijkstra's algorithm to find the shortest route between locations considering traffic, distance, and road conditions.

Given a city represented as a weighted graph where:
- Nodes = Intersections/Locations
- Edges = Roads with travel time/distance
- Find the shortest path from source to destination

**Input**: 
- Graph with weighted edges (adjacency list)
- Source location
- Destination location

**Output**: 
- Shortest distance/time
- Path taken

**Examples:**
```
City Map:
        5
    A -----> B
    |      / |
  1 |    2/  | 3
    |  /     |
    C -----> D
        4

Shortest path A → D:
  Path 1: A → B → D = 5 + 3 = 8
  Path 2: A → C → D = 1 + 4 = 5 ✓ (shortest)
  Path 3: A → C → B → D = 1 + 2 + 3 = 6

Output: distance = 5, path = [A, C, D]
```

---

## Solution: Dijkstra's Algorithm

**Algorithm:**
1. Initialize distances to infinity, source to 0
2. Use min-heap (priority queue) to process nearest node first
3. For each node, update distances to neighbors
4. Track parent pointers to reconstruct path

**Time Complexity:** O((V + E) log V) with min-heap  
**Space Complexity:** O(V)

**Why Dijkstra?**
- Handles weighted graphs (unlike BFS)
- Optimal for non-negative weights
- Greedy approach guarantees shortest path

---

## Complete Implementation

```java
import java.util.*;

public class ShortestPathCity {
    
    static class Edge {
        int destination;
        int weight;
        
        public Edge(int destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }
    
    static class Node implements Comparable<Node> {
        int id;
        int distance;
        
        public Node(int id, int distance) {
            this.id = id;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }
    
    static class PathResult {
        int distance;
        List<Integer> path;
        
        public PathResult(int distance, List<Integer> path) {
            this.distance = distance;
            this.path = path;
        }
        
        @Override
        public String toString() {
            return String.format("Distance: %d, Path: %s", distance, path);
        }
    }
    
    static class CityGraph {
        private Map<Integer, List<Edge>> graph;
        private Map<Integer, String> locationNames;
        
        public CityGraph() {
            graph = new HashMap<>();
            locationNames = new HashMap<>();
        }
        
        public void addLocation(int id, String name) {
            locationNames.put(id, name);
            graph.putIfAbsent(id, new ArrayList<>());
        }
        
        public void addRoad(int from, int to, int time) {
            graph.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, time));
        }
        
        public void addBidirectionalRoad(int loc1, int loc2, int time) {
            addRoad(loc1, loc2, time);
            addRoad(loc2, loc1, time);
        }
        
        // Dijkstra's Algorithm - Main Implementation
        public PathResult findShortestPath(int source, int destination) {
            int n = graph.size();
            int[] distances = new int[n];
            int[] parent = new int[n];
            boolean[] visited = new boolean[n];
            
            Arrays.fill(distances, Integer.MAX_VALUE);
            Arrays.fill(parent, -1);
            distances[source] = 0;
            
            PriorityQueue<Node> pq = new PriorityQueue<>();
            pq.offer(new Node(source, 0));
            
            while (!pq.isEmpty()) {
                Node current = pq.poll();
                int u = current.id;
                
                if (visited[u]) continue;
                visited[u] = true;
                
                // Early termination if destination reached
                if (u == destination) break;
                
                // Relax edges
                for (Edge edge : graph.getOrDefault(u, new ArrayList<>())) {
                    int v = edge.destination;
                    int weight = edge.weight;
                    
                    if (!visited[v] && distances[u] + weight < distances[v]) {
                        distances[v] = distances[u] + weight;
                        parent[v] = u;
                        pq.offer(new Node(v, distances[v]));
                    }
                }
            }
            
            // Reconstruct path
            List<Integer> path = reconstructPath(parent, source, destination);
            
            return new PathResult(
                distances[destination] == Integer.MAX_VALUE ? -1 : distances[destination],
                path
            );
        }
        
        // Find shortest paths from source to all destinations
        public Map<Integer, PathResult> findAllShortestPaths(int source) {
            int n = graph.size();
            int[] distances = new int[n];
            int[] parent = new int[n];
            boolean[] visited = new boolean[n];
            
            Arrays.fill(distances, Integer.MAX_VALUE);
            Arrays.fill(parent, -1);
            distances[source] = 0;
            
            PriorityQueue<Node> pq = new PriorityQueue<>();
            pq.offer(new Node(source, 0));
            
            while (!pq.isEmpty()) {
                Node current = pq.poll();
                int u = current.id;
                
                if (visited[u]) continue;
                visited[u] = true;
                
                for (Edge edge : graph.getOrDefault(u, new ArrayList<>())) {
                    int v = edge.destination;
                    int weight = edge.weight;
                    
                    if (!visited[v] && distances[u] + weight < distances[v]) {
                        distances[v] = distances[u] + weight;
                        parent[v] = u;
                        pq.offer(new Node(v, distances[v]));
                    }
                }
            }
            
            Map<Integer, PathResult> results = new HashMap<>();
            for (int dest = 0; dest < n; dest++) {
                if (dest != source && distances[dest] != Integer.MAX_VALUE) {
                    List<Integer> path = reconstructPath(parent, source, dest);
                    results.put(dest, new PathResult(distances[dest], path));
                }
            }
            
            return results;
        }
        
        // K shortest paths (Yen's algorithm simplified)
        public List<PathResult> findKShortestPaths(int source, int destination, int k) {
            List<PathResult> kPaths = new ArrayList<>();
            PriorityQueue<PathResult> candidates = new PriorityQueue<>(
                Comparator.comparingInt(p -> p.distance)
            );
            
            PathResult shortest = findShortestPath(source, destination);
            if (shortest.distance == -1) return kPaths;
            
            kPaths.add(shortest);
            
            // Simplified: Find alternative paths by temporarily removing edges
            // Full Yen's algorithm is more complex
            
            return kPaths;
        }
        
        private List<Integer> reconstructPath(int[] parent, int source, int destination) {
            List<Integer> path = new ArrayList<>();
            
            if (parent[destination] == -1 && source != destination) {
                return path; // No path exists
            }
            
            int current = destination;
            while (current != -1) {
                path.add(current);
                current = parent[current];
            }
            
            Collections.reverse(path);
            return path;
        }
        
        public String getLocationName(int id) {
            return locationNames.getOrDefault(id, "Location " + id);
        }
        
        public void printPath(PathResult result) {
            if (result.distance == -1) {
                System.out.println("No path exists");
                return;
            }
            
            System.out.print("Path: ");
            for (int i = 0; i < result.path.size(); i++) {
                System.out.print(getLocationName(result.path.get(i)));
                if (i < result.path.size() - 1) {
                    System.out.print(" → ");
                }
            }
            System.out.println("\nTotal time: " + result.distance + " minutes");
        }
    }
    
    // Test cases
    public static boolean doTestsPass() {
        CityGraph city = new CityGraph();
        
        // Build test graph
        city.addBidirectionalRoad(0, 1, 5);
        city.addBidirectionalRoad(0, 2, 1);
        city.addBidirectionalRoad(1, 2, 2);
        city.addBidirectionalRoad(1, 3, 3);
        city.addBidirectionalRoad(2, 3, 4);
        
        // Test 1: Shortest path 0 → 3
        PathResult result1 = city.findShortestPath(0, 3);
        if (result1.distance != 5) return false; // 0→2→3
        
        // Test 2: Direct path
        PathResult result2 = city.findShortestPath(0, 1);
        if (result2.distance != 3) return false; // 0→2→1
        
        // Test 3: No path
        city.addLocation(4, "Isolated");
        PathResult result3 = city.findShortestPath(0, 4);
        if (result3.distance != -1) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: San Francisco navigation
        System.out.println("=== San Francisco GPS Navigation ===\n");
        
        CityGraph sf = new CityGraph();
        
        // Add locations
        sf.addLocation(0, "Union Square");
        sf.addLocation(1, "Fisherman's Wharf");
        sf.addLocation(2, "Chinatown");
        sf.addLocation(3, "Golden Gate Bridge");
        sf.addLocation(4, "Mission District");
        sf.addLocation(5, "Castro");
        
        // Add roads (travel time in minutes)
        sf.addBidirectionalRoad(0, 1, 15);  // Union Square ↔ Fisherman's Wharf
        sf.addBidirectionalRoad(0, 2, 5);   // Union Square ↔ Chinatown
        sf.addBidirectionalRoad(0, 4, 10);  // Union Square ↔ Mission
        sf.addBidirectionalRoad(1, 2, 8);   // Fisherman's Wharf ↔ Chinatown
        sf.addBidirectionalRoad(1, 3, 20);  // Fisherman's Wharf ↔ Golden Gate
        sf.addBidirectionalRoad(2, 3, 25);  // Chinatown ↔ Golden Gate
        sf.addBidirectionalRoad(4, 5, 7);   // Mission ↔ Castro
        sf.addBidirectionalRoad(0, 5, 12);  // Union Square ↔ Castro
        
        // Find shortest path
        System.out.println("Route: Union Square → Golden Gate Bridge\n");
        PathResult route = sf.findShortestPath(0, 3);
        sf.printPath(route);
        
        // Find all shortest paths from Union Square
        System.out.println("\n=== All Routes from Union Square ===\n");
        Map<Integer, PathResult> allRoutes = sf.findAllShortestPaths(0);
        for (Map.Entry<Integer, PathResult> entry : allRoutes.entrySet()) {
            System.out.println("To " + sf.getLocationName(entry.getKey()) + ":");
            System.out.println("  " + entry.getValue());
        }
    }
}
```

---

## Algorithm Walkthrough

### Example: Find shortest path from A(0) to D(3)

```
Graph:
    5
  0 → 1
  |   |↘
1 |   2  3
  ↓ ↗   ↓
  2 → → 3
    4

Step-by-step execution:

Initial state:
  distances = [0, ∞, ∞, ∞]
  parent = [-1, -1, -1, -1]
  pq = [(0, 0)]  // (node, distance)

Iteration 1: Process node 0
  visited[0] = true
  Relax edges:
    0→1: distances[1] = min(∞, 0+5) = 5, parent[1] = 0
    0→2: distances[2] = min(∞, 0+1) = 1, parent[2] = 0
  pq = [(2, 1), (1, 5)]

Iteration 2: Process node 2 (smallest distance)
  visited[2] = true
  Relax edges:
    2→1: distances[1] = min(5, 1+2) = 3, parent[1] = 2
    2→3: distances[3] = min(∞, 1+4) = 5, parent[3] = 2
  pq = [(1, 3), (1, 5), (3, 5)]

Iteration 3: Process node 1
  visited[1] = true
  Relax edges:
    1→3: distances[3] = min(5, 3+3) = 5 (no update)
  pq = [(3, 5)]

Iteration 4: Process node 3
  visited[3] = true
  Destination reached!

Final state:
  distances = [0, 3, 1, 5]
  parent = [-1, 2, 0, 2]

Reconstruct path from parent array:
  3 ← 2 ← 0
  Path: [0, 2, 3]
  Distance: 5
```

---

## Complexity Analysis

| Operation | Time | Space |
|-----------|------|-------|
| Dijkstra (binary heap) | O((V + E) log V) | O(V) |
| Dijkstra (Fibonacci heap) | O(E + V log V) | O(V) |
| Build graph | O(E) | O(V + E) |
| Path reconstruction | O(V) | O(V) |

**Why Priority Queue?**
- Always process nearest unvisited node
- Ensures optimal substructure property
- Avoids reprocessing nodes

---

## Optimizations for Production

### 1. A* Algorithm (Heuristic-based)
```java
// Add heuristic for faster pathfinding
public PathResult aStarSearch(int source, int destination) {
    // distance[v] = g(v) + h(v)
    // g(v) = actual distance from source
    // h(v) = heuristic (e.g., Euclidean distance)
}
```

### 2. Bidirectional Dijkstra
```java
// Search from both source and destination
public PathResult bidirectionalDijkstra(int source, int destination) {
    // Meet in the middle for 2x speedup
}
```

### 3. Contraction Hierarchies
```java
// Preprocess graph for ultra-fast queries
// Used by Google Maps for real-time navigation
```

---

## Real-World Applications

1. **Google Maps** - Turn-by-turn navigation
2. **Uber/Lyft** - Driver-rider matching and routing
3. **Delivery Services** - Amazon, FedEx route optimization
4. **Network Routing** - OSPF protocol in routers
5. **Game Development** - NPC pathfinding
6. **Flight Planning** - Airline route optimization

---

## Variations

### 1. With Traffic Conditions
```java
// Edge weights change based on time of day
public PathResult findPathWithTraffic(int source, int dest, int currentTime) {
    // Adjust edge weights based on traffic data
}
```

### 2. Multi-Stop Route
```java
// Visit multiple locations (Traveling Salesman variant)
public PathResult findMultiStopRoute(int source, List<Integer> stops, int dest) {
    // Optimize order of stops
}
```

### 3. Alternative Routes
```java
// Find top-K shortest paths
public List<PathResult> findAlternativeRoutes(int source, int dest, int k) {
    // Yen's algorithm for K-shortest paths
}
```
