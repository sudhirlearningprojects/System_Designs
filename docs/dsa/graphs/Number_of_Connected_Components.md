# Number of Connected Components in Undirected Graph
**LeetCode 323** | Medium | Union-Find / DFS

## Problem
Given `n` nodes (0 to n-1) and a list of undirected edges, return the number of connected components.

```
Input: n = 5, edges = [[0,1],[1,2],[3,4]]
Output: 2

Input: n = 5, edges = [[0,1],[1,2],[2,3],[3,4]]
Output: 1
```

## Approach 1: Union-Find (Optimal)
**Time:** O(n·α(n)) | **Space:** O(n)

```java
public int countComponents(int n, int[][] edges) {
    int[] parent = new int[n], rank = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int components = n;
    for (int[] e : edges)
        if (union(parent, rank, e[0], e[1])) components--;
    return components;
}

private int find(int[] parent, int x) {
    if (parent[x] != x) parent[x] = find(parent, parent[x]);
    return parent[x];
}

private boolean union(int[] parent, int[] rank, int x, int y) {
    int px = find(parent, x), py = find(parent, y);
    if (px == py) return false;
    if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
    parent[py] = px;
    if (rank[px] == rank[py]) rank[px]++;
    return true;
}
```

## Approach 2: DFS
**Time:** O(V+E) | **Space:** O(V+E)

```java
public int countComponents(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

    boolean[] visited = new boolean[n];
    int count = 0;
    for (int i = 0; i < n; i++)
        if (!visited[i]) { dfs(adj, visited, i); count++; }
    return count;
}

private void dfs(List<List<Integer>> adj, boolean[] visited, int node) {
    visited[node] = true;
    for (int neighbor : adj.get(node))
        if (!visited[neighbor]) dfs(adj, visited, neighbor);
}
```

## Approach 3: BFS
**Time:** O(V+E) | **Space:** O(V+E)

```java
public int countComponents(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

    boolean[] visited = new boolean[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
        if (visited[i]) continue;
        count++;
        Queue<Integer> q = new LinkedList<>();
        q.offer(i); visited[i] = true;
        while (!q.isEmpty()) {
            int node = q.poll();
            for (int nb : adj.get(node))
                if (!visited[nb]) { visited[nb] = true; q.offer(nb); }
        }
    }
    return count;
}
```

## Key Insight
Union-Find is the most elegant: start with `n` components, decrement each time two different components merge.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `n=1, edges=[]` | 1 | Single isolated node |
| `n=5, edges=[]` | 5 | No edges → all isolated |
| `n=3, edges=[[0,1],[1,2],[0,2]]` | 1 | Triangle → one component |
| Duplicate edges `[[0,1],[0,1]]` | n-1 | Second edge is redundant, same component |
| Self-loop `[[0,0]]` | n | Self-loop doesn’t connect two different nodes |
| `n=2, edges=[[0,1],[1,0]]` | 1 | Undirected duplicate → still one component |

---

## Dry Run

**Input:** `n=5, edges=[[0,1],[1,2],[3,4]]`

**Union-Find trace:**
```
Initial: parent=[0,1,2,3,4], components=5

Edge [0,1]:
  find(0)=0, find(1)=1 → different roots → union
  parent=[0,0,2,3,4], components=4

Edge [1,2]:
  find(1): parent[1]=0 → root=0
  find(2)=2 → different roots → union
  parent=[0,0,0,3,4], components=3

Edge [3,4]:
  find(3)=3, find(4)=4 → different roots → union
  parent=[0,0,0,3,3], components=2

Final: 2 components → {0,1,2} and {3,4}
```

---

## Follow-up Questions

**Q: What if edges are directed?**
Use DFS/BFS with a directed adjacency list. Union-Find only works for undirected graphs.

**Q: How to also return which nodes are in each component?**
After all unions, group nodes by their root: `Map<Integer, List<Integer>> groups`.

**Q: What if new edges are added dynamically?**
Union-Find handles dynamic edge additions in O(α(n)) per operation — ideal for online algorithms.

**Q: What’s the difference between this and Graph Valid Tree (LC 261)?**
This counts components. Valid Tree additionally requires exactly n-1 edges and no cycle (i.e., exactly 1 component).

**Related Problems:** LC 261 (Graph Valid Tree), LC 684 (Redundant Connection), LC 547 (Number of Provinces), LC 721 (Accounts Merge)
