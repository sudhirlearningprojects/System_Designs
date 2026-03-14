# Redundant Connection
**LeetCode 684** | Medium | Union-Find

## Problem
Given a tree with `n` nodes and one extra edge added (making it a graph with a cycle),
find the redundant edge. If multiple answers exist, return the last one in the input.

```
Input: edges = [[1,2],[1,3],[2,3]]
Output: [2,3]

Input: edges = [[1,2],[2,3],[3,4],[1,4],[1,5]]
Output: [1,4]
```

## Approach 1: Union-Find (Optimal)
Process edges in order. The first edge that connects two already-connected nodes is the redundant one.

**Time:** O(n·α(n)) | **Space:** O(n)

```java
public int[] findRedundantConnection(int[][] edges) {
    int n = edges.length;
    int[] parent = new int[n + 1], rank = new int[n + 1];
    for (int i = 1; i <= n; i++) parent[i] = i;

    for (int[] e : edges)
        if (!union(parent, rank, e[0], e[1])) return e;
    return new int[]{};
}

private int find(int[] parent, int x) {
    if (parent[x] != x) parent[x] = find(parent, parent[x]);
    return parent[x];
}

private boolean union(int[] parent, int[] rank, int x, int y) {
    int px = find(parent, x), py = find(parent, y);
    if (px == py) return false; // already connected → redundant edge
    if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
    parent[py] = px;
    if (rank[px] == rank[py]) rank[px]++;
    return true;
}
```

## Approach 2: DFS Cycle Detection
For each edge, check if a path already exists between its endpoints using DFS.

**Time:** O(n²) | **Space:** O(n)

```java
public int[] findRedundantConnection(int[][] edges) {
    Map<Integer, List<Integer>> adj = new HashMap<>();

    for (int[] e : edges) {
        Set<Integer> visited = new HashSet<>();
        if (adj.containsKey(e[0]) && adj.containsKey(e[1]) && dfs(adj, visited, e[0], e[1]))
            return e;
        adj.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
        adj.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
    }
    return new int[]{};
}

private boolean dfs(Map<Integer, List<Integer>> adj, Set<Integer> visited, int src, int dst) {
    if (src == dst) return true;
    visited.add(src);
    for (int nb : adj.getOrDefault(src, new ArrayList<>()))
        if (!visited.contains(nb) && dfs(adj, visited, nb, dst)) return true;
    return false;
}
```

## Key Insight
Union-Find is the cleanest solution: process edges sequentially, and the first edge where both endpoints
already share the same root is the cycle-forming (redundant) edge.
Since we process in order, the last such edge in the input is naturally returned.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `[[1,2],[2,3],[3,1]]` | `[3,1]` | Last edge forming the cycle |
| `[[1,2],[1,3],[1,4],[3,4]]` | `[3,4]` | Last redundant edge |
| Two redundant edges possible | Last one in input | Problem guarantees return last |
| Self-loop `[[1,1]]` | `[1,1]` | find(1)==find(1) immediately |
| Star graph + one extra edge | The extra edge | All spokes connect to center |
| `n=3, [[1,2],[1,3],[2,3]]` | `[2,3]` | Triangle, last edge is redundant |

---

## Dry Run

**Input:** `edges = [[1,2],[2,3],[3,4],[1,4],[1,5]]`

**Union-Find trace:**
```
Initial: parent=[_,1,2,3,4,5] (1-indexed)

Edge [1,2]: find(1)=1, find(2)=2 → different → union
  parent=[_,1,1,3,4,5]

Edge [2,3]: find(2): parent[2]=1 → root=1
            find(3)=3 → different → union
  parent=[_,1,1,1,4,5]

Edge [3,4]: find(3): parent[3]=1 → root=1
            find(4)=4 → different → union
  parent=[_,1,1,1,1,5]

Edge [1,4]: find(1)=1
            find(4): parent[4]=1 → root=1
            SAME ROOT! → return [1,4]

Answer: [1,4]
```

---

## Follow-up Questions

**Q: What about Redundant Connection II (LC 685) for directed graphs?**
More complex: a node can have in-degree 2 (two parents), or there’s a cycle. Handle both cases: find the node with in-degree 2 first, then check which of its two incoming edges causes the cycle.

**Q: What if there are multiple redundant edges?**
The problem guarantees exactly one extra edge. If there were multiple, you’d need to return the last one that forms a cycle.

**Q: Can you solve this with DFS instead of Union-Find?**
Yes (Approach 2), but it’s O(n²) vs O(nα(n)) for Union-Find. DFS checks if a path exists before adding each edge.

**Q: Why does Union-Find return the correct “last” redundant edge?**
Edges are processed in order. The first edge where `find(u) == find(v)` is the one that closes the cycle — which is the last redundant edge since there’s exactly one cycle.

**Related Problems:** LC 685 (Redundant Connection II), LC 261 (Graph Valid Tree), LC 323 (Number of Connected Components)
