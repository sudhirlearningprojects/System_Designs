# Graph Problems - LeetCode Top 25

Solutions for the [25 Graph Problems to Revise Before Interviews](https://leetcode.com/discuss/post/7368340/25-graph-problems-to-revise-before-inter-3pa2/).

## 🎯 Problems by Difficulty

### Easy
| # | Problem | Algorithm | LC |
|---|---------|-----------|-----|
| 1 | [Clone Graph](Clone_Graph.md) | DFS/BFS + HashMap | 133 |
| 2 | [Number of Islands](Number_of_Islands.md) | DFS/BFS/Union-Find | 200 |
| 3 | [Course Schedule](Course_Schedule.md) | Topological Sort | 207 |
| 4 | [Rotting Oranges](Rotting_Oranges.md) | Multi-source BFS | 994 |
| 5 | [Jump Game III](Jump_Game_III.md) | BFS/DFS | 1306 |

### Medium
| # | Problem | Algorithm | LC |
|---|---------|-----------|-----|
| 6 | [Number of Connected Components](Number_of_Connected_Components.md) | Union-Find/DFS | 323 |
| 7 | [Graph Valid Tree](Graph_Valid_Tree.md) | Union-Find/DFS | 261 |
| 8 | [Pacific Atlantic Water Flow](Pacific_Atlantic_Water_Flow.md) | Multi-source BFS/DFS | 417 |
| 9 | [Surrounded Regions](Surrounded_Regions.md) | DFS/BFS from border | 130 |
| 10 | [Word Ladder](Word_Ladder.md) | BFS / Bidirectional BFS | 127 |
| 11 | [Redundant Connection](Redundant_Connection.md) | Union-Find | 684 |
| 12 | [Accounts Merge](Accounts_Merge.md) | Union-Find/DFS | 721 |
| 13 | [Cheapest Flights Within K Stops](Cheapest_Flights_K_Stops.md) | Bellman-Ford/Dijkstra | 787 |
| 14 | [Network Delay Time](Network_Delay_Time.md) | Dijkstra/Bellman-Ford | 743 |
| 15 | [Path with Max Probability](Path_with_Max_Probability.md) | Dijkstra (max-heap) | 1514 |
| 16 | [Find the City](Find_the_City.md) | Floyd-Warshall | 1334 |
| 17 | [Min Cost to Connect All Points](Min_Cost_Connect_Points.md) | Prim's/Kruskal's MST | 1584 |
| 18 | [Shortest Path in Binary Matrix](Shortest_Path_Binary_Matrix.md) | BFS / A* | 1091 |
| 19 | [Snakes and Ladders](Snakes_and_Ladders.md) | BFS | 909 |
| 20 | [Open the Lock](Open_the_Lock.md) | BFS / Bidirectional BFS | 752 |
| 21 | [Reconstruct Itinerary](Reconstruct_Itinerary.md) | Hierholzer's (Eulerian Path) | 332 |

### Hard
| # | Problem | Algorithm | LC |
|---|---------|-----------|-----|
| 22 | [Alien Dictionary](Alien_Dictionary.md) | Topological Sort (Kahn's/DFS) | 269 |
| 23 | [Swim in Rising Water](Swim_in_Rising_Water.md) | Dijkstra/Binary Search+BFS/Union-Find | 778 |
| 24 | [Bus Routes](Bus_Routes.md) | BFS on Route Graph | 815 |
| 25 | [Word Ladder II](Word_Ladder_II.md) | BFS + DFS Backtracking | 126 |

---

## 📚 Algorithm Cheat Sheet

| Algorithm | When to Use | Time | Space |
|-----------|-------------|------|-------|
| BFS | Shortest path (unweighted), level-order | O(V+E) | O(V) |
| DFS | Cycle detection, connectivity, topological sort | O(V+E) | O(V) |
| Dijkstra | Shortest path (non-negative weights) | O((V+E) log V) | O(V) |
| Bellman-Ford | Shortest path with edge count limit | O(V·E) | O(V) |
| Floyd-Warshall | All-pairs shortest path (small n) | O(V³) | O(V²) |
| Kruskal's | MST (sparse graphs) | O(E log E) | O(V) |
| Prim's | MST (dense graphs) | O(V²) or O((V+E) log V) | O(V) |
| Topological Sort | DAG ordering, dependency resolution | O(V+E) | O(V) |
| Union-Find | Connected components, cycle detection | O(α(n)) ≈ O(1) | O(V) |
| Hierholzer's | Eulerian path/circuit | O(E log E) | O(E) |
| Bidirectional BFS | Shortest path (large search space) | O(b^(d/2)) | O(b^(d/2)) |

---

## 🔑 Pattern Recognition Guide

```
Grid problems (islands, regions, matrix paths)
  → BFS/DFS with 4 or 8 directional movement
  → Multi-source BFS when multiple starting points

Shortest path
  → Unweighted graph: BFS
  → Weighted, non-negative: Dijkstra
  → Weighted with step limit: Bellman-Ford
  → All pairs, small n: Floyd-Warshall
  → Minimax path: Dijkstra with max() instead of sum

Connected components / cycle detection
  → Union-Find (simplest, most efficient)
  → DFS with visited/visiting/done states

Dependency ordering
  → Topological Sort (Kahn's BFS or DFS post-order)
  → Cycle = invalid ordering

Minimum spanning tree
  → Kruskal's: sort edges, union-find
  → Prim's: greedy from any node, min-heap

Eulerian path (use every edge once)
  → Hierholzer's algorithm

Implicit graphs (word ladder, lock, board games)
  → BFS on state space
  → Bidirectional BFS for large state spaces
```

---

## 🔧 Graph Representations

```java
// Adjacency List — O(V+E) space, best for sparse graphs
Map<Integer, List<Integer>> adj = new HashMap<>();

// Adjacency Matrix — O(V²) space, best for dense graphs / Floyd-Warshall
int[][] dist = new int[n][n];

// Edge List — O(E) space, best for Kruskal's MST
List<int[]> edges = new ArrayList<>(); // [weight, u, v]

// Union-Find — O(V) space, best for connectivity
int[] parent = new int[n], rank = new int[n];
```

---

## 📝 Full Problem Index

1. [Clone Graph](Clone_Graph.md)
2. [Number of Islands](Number_of_Islands.md)
3. [Course Schedule](Course_Schedule.md)
4. [Rotting Oranges](Rotting_Oranges.md)
5. [Jump Game III](Jump_Game_III.md)
6. [Number of Connected Components](Number_of_Connected_Components.md)
7. [Graph Valid Tree](Graph_Valid_Tree.md)
8. [Pacific Atlantic Water Flow](Pacific_Atlantic_Water_Flow.md)
9. [Surrounded Regions](Surrounded_Regions.md)
10. [Word Ladder](Word_Ladder.md)
11. [Redundant Connection](Redundant_Connection.md)
12. [Accounts Merge](Accounts_Merge.md)
13. [Cheapest Flights Within K Stops](Cheapest_Flights_K_Stops.md)
14. [Network Delay Time](Network_Delay_Time.md)
15. [Path with Max Probability](Path_with_Max_Probability.md)
16. [Find the City](Find_the_City.md)
17. [Min Cost to Connect All Points](Min_Cost_Connect_Points.md)
18. [Shortest Path in Binary Matrix](Shortest_Path_Binary_Matrix.md)
19. [Snakes and Ladders](Snakes_and_Ladders.md)
20. [Open the Lock](Open_the_Lock.md)
21. [Reconstruct Itinerary](Reconstruct_Itinerary.md)
22. [Alien Dictionary](Alien_Dictionary.md)
23. [Swim in Rising Water](Swim_in_Rising_Water.md)
24. [Bus Routes](Bus_Routes.md)
25. [Word Ladder II](Word_Ladder_II.md)

---

**Also included** (real-world graph problems):
- [Friend Recommendations](Friend_Recommendations.md) — BFS social graph
- [Shortest Path in City](Shortest_Path_City.md) — Dijkstra navigation
- [Critical Connections](Critical_Connections.md) — Tarjan's bridge finding
- [Minimum Cost to Connect Cities](MST_Cities.md) — Kruskal's/Prim's MST
