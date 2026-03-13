# Min Cost to Connect All Points
**LeetCode 1584** | Medium | Prim's / Kruskal's MST

## Problem
Given `points[i] = [xi, yi]`, find the minimum cost to connect all points where the cost between
two points is their Manhattan distance `|xi - xj| + |yi - yj|`.

```
Input: points = [[0,0],[2,2],[3,10],[5,2],[7,0]]
Output: 20

Input: points = [[3,12],[-2,5],[-4,1]]
Output: 18
```

## Approach 1: Prim's Algorithm (Optimal for Dense Graphs)
**Time:** O(n²) | **Space:** O(n)

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;
    int[] minDist = new int[n];
    boolean[] inMST = new boolean[n];
    Arrays.fill(minDist, Integer.MAX_VALUE);
    minDist[0] = 0;
    int totalCost = 0;

    for (int i = 0; i < n; i++) {
        // Pick the node with minimum distance not yet in MST
        int u = -1;
        for (int j = 0; j < n; j++)
            if (!inMST[j] && (u == -1 || minDist[j] < minDist[u])) u = j;

        inMST[u] = true;
        totalCost += minDist[u];

        // Update distances for remaining nodes
        for (int v = 0; v < n; v++) {
            if (!inMST[v]) {
                int dist = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
                minDist[v] = Math.min(minDist[v], dist);
            }
        }
    }
    return totalCost;
}
```

## Approach 2: Prim's with Priority Queue
**Time:** O(n² log n) | **Space:** O(n²)

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;
    boolean[] inMST = new boolean[n];
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{0, 0}); // [cost, node]
    int totalCost = 0, edgesUsed = 0;

    while (edgesUsed < n) {
        int[] curr = pq.poll();
        int cost = curr[0], u = curr[1];
        if (inMST[u]) continue;
        inMST[u] = true;
        totalCost += cost;
        edgesUsed++;
        for (int v = 0; v < n; v++) {
            if (!inMST[v]) {
                int dist = Math.abs(points[u][0] - points[v][0]) + Math.abs(points[u][1] - points[v][1]);
                pq.offer(new int[]{dist, v});
            }
        }
    }
    return totalCost;
}
```

## Approach 3: Kruskal's Algorithm
**Time:** O(n² log n) | **Space:** O(n²)

```java
public int minCostConnectPoints(int[][] points) {
    int n = points.length;
    List<int[]> edges = new ArrayList<>(); // [cost, u, v]
    for (int i = 0; i < n; i++)
        for (int j = i+1; j < n; j++)
            edges.add(new int[]{Math.abs(points[i][0]-points[j][0]) + Math.abs(points[i][1]-points[j][1]), i, j});
    edges.sort(Comparator.comparingInt(a -> a[0]));

    int[] parent = new int[n], rank = new int[n];
    for (int i = 0; i < n; i++) parent[i] = i;
    int totalCost = 0, edgesUsed = 0;

    for (int[] e : edges) {
        if (edgesUsed == n - 1) break;
        if (union(parent, rank, e[1], e[2])) { totalCost += e[0]; edgesUsed++; }
    }
    return totalCost;
}

private int find(int[] p, int x) { return p[x] == x ? x : (p[x] = find(p, p[x])); }
private boolean union(int[] p, int[] r, int x, int y) {
    int px = find(p, x), py = find(p, y);
    if (px == py) return false;
    if (r[px] < r[py]) { int t = px; px = py; py = t; }
    p[py] = px; if (r[px] == r[py]) r[px]++;
    return true;
}
```

## Key Insight
This is a complete graph (every pair of points is connected). Prim's O(n²) beats Kruskal's O(n² log n)
for dense graphs since we avoid sorting all O(n²) edges. The O(n²) Prim's is the optimal approach here.
