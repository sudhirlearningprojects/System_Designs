# Graph Data Structure - Real-Life Problems

A comprehensive collection of graph algorithms solving practical real-world problems.

## 📋 Problem Categories

### 1. Social Networks & Connections
- **Friend Recommendations** - Find mutual friends and suggest connections
- **Influence Propagation** - Track how information spreads through networks
- **Community Detection** - Identify clusters in social graphs

### 2. Navigation & Routing
- **Shortest Path Navigation** - GPS routing, delivery optimization
- **Flight Route Planning** - Multi-city travel optimization
- **Network Packet Routing** - Internet data transmission

### 3. Dependency Management
- **Build Order** - Compile dependencies in correct order
- **Course Prerequisites** - Academic course scheduling
- **Task Scheduling** - Project management dependencies

### 4. Network Infrastructure
- **Network Connectivity** - Detect network failures
- **Minimum Spanning Tree** - Optimize cable/pipeline networks
- **Critical Connections** - Find bridges in networks

### 5. Web & Search
- **Web Crawler** - Navigate and index web pages
- **PageRank Algorithm** - Rank web pages by importance
- **URL Shortener Graph** - Track URL redirections

## 🎯 Problems by Difficulty

### Easy
1. [Friend Recommendations](Friend_Recommendations.md) - BFS for mutual friends
2. [Course Schedule Validator](Course_Schedule.md) - Cycle detection
3. [Clone Graph](Clone_Graph.md) - Deep copy of graph

### Medium
4. [Shortest Path in City](Shortest_Path_City.md) - Dijkstra's algorithm
5. [Network Delay Time](Network_Delay_Time.md) - Signal propagation
6. [Word Ladder](Word_Ladder.md) - BFS transformation
7. [Critical Connections](Critical_Connections.md) - Find bridges
8. [Minimum Cost to Connect Cities](MST_Cities.md) - Kruskal's/Prim's

### Hard
9. [Flight Route Optimization](Flight_Routes.md) - Multi-criteria shortest path
10. [Social Network Influence](Influence_Propagation.md) - Advanced BFS/DFS
11. [Alien Dictionary](Alien_Dictionary.md) - Topological sort

## 🔧 Graph Representations

### Adjacency List (Most Common)
```java
Map<Integer, List<Integer>> graph = new HashMap<>();
// Space: O(V + E), Best for sparse graphs
```

### Adjacency Matrix
```java
int[][] graph = new int[n][n];
// Space: O(V²), Best for dense graphs
```

### Edge List
```java
List<int[]> edges = new ArrayList<>();
// Space: O(E), Best for MST algorithms
```

## 📚 Core Algorithms

| Algorithm | Use Case | Time Complexity | Space |
|-----------|----------|-----------------|-------|
| BFS | Shortest path (unweighted) | O(V + E) | O(V) |
| DFS | Cycle detection, connectivity | O(V + E) | O(V) |
| Dijkstra | Shortest path (weighted) | O((V + E) log V) | O(V) |
| Bellman-Ford | Negative weights | O(VE) | O(V) |
| Floyd-Warshall | All pairs shortest path | O(V³) | O(V²) |
| Kruskal's MST | Minimum spanning tree | O(E log E) | O(V) |
| Prim's MST | Minimum spanning tree | O((V + E) log V) | O(V) |
| Topological Sort | Dependency ordering | O(V + E) | O(V) |
| Tarjan's | Strongly connected components | O(V + E) | O(V) |
| Union-Find | Disjoint sets | O(α(n)) ≈ O(1) | O(V) |

## 🚀 Quick Start

```java
// Basic Graph Template
class Graph {
    private Map<Integer, List<Integer>> adjList;
    
    public Graph() {
        adjList = new HashMap<>();
    }
    
    public void addEdge(int u, int v) {
        adjList.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
    }
    
    public List<Integer> getNeighbors(int node) {
        return adjList.getOrDefault(node, new ArrayList<>());
    }
}
```

## 📖 Learning Path

1. **Fundamentals** → Start with BFS/DFS traversals
2. **Shortest Path** → Learn Dijkstra and Bellman-Ford
3. **Topological Sort** → Understand dependency graphs
4. **MST** → Master Kruskal's and Prim's
5. **Advanced** → Study strongly connected components

## 🎓 Real-World Applications

- **Google Maps** - Dijkstra's for navigation
- **Facebook** - BFS for friend suggestions
- **LinkedIn** - Graph traversal for connections
- **Uber/Lyft** - Shortest path for ride routing
- **Netflix** - Graph-based recommendations
- **Git** - DAG for version control
- **Package Managers** - Topological sort for dependencies
- **Network Routers** - Shortest path protocols (OSPF, BGP)

## 📝 Problem Index

1. [Friend Recommendations](Friend_Recommendations.md)
2. [Course Schedule](Course_Schedule.md)
3. [Shortest Path in City](Shortest_Path_City.md)
4. [Network Delay Time](Network_Delay_Time.md)
5. [Critical Connections](Critical_Connections.md)
6. [Minimum Cost to Connect Cities](MST_Cities.md)
7. [Word Ladder](Word_Ladder.md)
8. [Clone Graph](Clone_Graph.md)
9. [Flight Route Optimization](Flight_Routes.md)
10. [Social Network Influence](Influence_Propagation.md)

---

**Note**: Each problem includes:
- Problem statement with real-world context
- Multiple solution approaches
- Time/space complexity analysis
- Complete working code
- Test cases and examples
- Visual diagrams where applicable
