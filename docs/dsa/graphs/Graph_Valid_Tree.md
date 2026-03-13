# Graph Valid Tree
**LeetCode 261** | Medium | Union-Find / DFS

## Problem
Given `n` nodes and a list of undirected edges, determine if the edges form a valid tree.
A valid tree has exactly `n-1` edges and is fully connected (no cycles).

```
Input: n = 5, edges = [[0,1],[0,2],[0,3],[1,4]]
Output: true

Input: n = 5, edges = [[0,1],[1,2],[2,3],[1,3],[1,4]]
Output: false  (cycle exists)
```

## Approach 1: Union-Find (Optimal)
A valid tree: exactly n-1 edges + no cycle (union returns false when same component).

**Time:** O(n·α(n)) | **Space:** O(n)

```java
public boolean validTree(int n, int[][] edges) {
    if (edges.length != n - 1) return false;
    int[] parent = new int[n], rank = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    for (int[] e : edges)
        if (!union(parent, rank, e[0], e[1])) return false;
    return true;
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

## Approach 2: DFS Cycle Detection
**Time:** O(V+E) | **Space:** O(V+E)

```java
public boolean validTree(int n, int[][] edges) {
    if (edges.length != n - 1) return false;
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

    boolean[] visited = new boolean[n];
    if (!dfs(adj, visited, 0, -1)) return false;
    for (boolean v : visited) if (!v) return false; // must be fully connected
    return true;
}

private boolean dfs(List<List<Integer>> adj, boolean[] visited, int node, int parent) {
    visited[node] = true;
    for (int nb : adj.get(node)) {
        if (nb == parent) continue;
        if (visited[nb]) return false; // cycle
        if (!dfs(adj, visited, nb, node)) return false;
    }
    return true;
}
```

## Approach 3: BFS
**Time:** O(V+E) | **Space:** O(V+E)

```java
public boolean validTree(int n, int[][] edges) {
    if (edges.length != n - 1) return false;
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); adj.get(e[1]).add(e[0]); }

    boolean[] visited = new boolean[n];
    Queue<Integer> q = new LinkedList<>();
    q.offer(0); visited[0] = true;
    int count = 1;
    while (!q.isEmpty()) {
        int node = q.poll();
        for (int nb : adj.get(node))
            if (!visited[nb]) { visited[nb] = true; q.offer(nb); count++; }
    }
    return count == n;
}
```

## Key Insight
Two conditions for a valid tree:
1. Exactly `n-1` edges (necessary for a tree)
2. No cycle / fully connected (sufficient with condition 1)

Check condition 1 upfront to short-circuit — saves traversal time.
