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
