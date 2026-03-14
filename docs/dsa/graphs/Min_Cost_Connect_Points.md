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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| Single point `[[0,0]]` | 0 | No edges needed |
| Two points | Manhattan distance between them | Only one edge |
| All points on same line | Sum of consecutive distances | MST = straight chain |
| All points at same location | 0 | All distances are 0 |
| Points with negative coordinates | Valid | Manhattan distance still works |
| Large coordinates | Watch for int overflow | Use long if needed |

---

## Dry Run

**Input:** `points = [[0,0],[2,2],[3,10]]`

**Prim’s O(n²) trace:**
```
n=3, minDist=[0,INF,INF], inMST=[F,F,F]

--- Iteration 1 ---
Pick u with min minDist not in MST:
  u=0 (minDist[0]=0)
inMST[0]=true, totalCost += 0 = 0

Update neighbors:
  v=1: dist = |0-2|+|0-2| = 4 < INF → minDist[1]=4
  v=2: dist = |0-3|+|0-10| = 13 < INF → minDist[2]=13
minDist=[0,4,13]

--- Iteration 2 ---
Pick u with min minDist not in MST:
  u=1 (minDist[1]=4)
inMST[1]=true, totalCost += 4 = 4

Update neighbors:
  v=2: dist = |2-3|+|2-10| = 9 < 13 → minDist[2]=9
minDist=[0,4,9]

--- Iteration 3 ---
Pick u with min minDist not in MST:
  u=2 (minDist[2]=9)
inMST[2]=true, totalCost += 9 = 13

All nodes in MST.
Answer: 13
```

---

## Follow-up Questions

**Q: Why is Prim’s O(n²) better than Kruskal’s O(n² log n) here?**
Kruskal’s requires generating and sorting all O(n²) edges upfront. Prim’s computes distances on-the-fly, avoiding the sort. For dense graphs (like this complete graph), Prim’s wins.

**Q: Can you use Prim’s with a priority queue?**
Yes (Approach 2), but it’s O(n² log n) due to pushing O(n²) edges into the heap. The O(n²) array-based Prim’s is better here.

**Q: What if the cost function is Euclidean distance instead of Manhattan?**
Replace `Math.abs(dx) + Math.abs(dy)` with `Math.sqrt(dx*dx + dy*dy)`. The algorithm is identical.

**Q: What if some points must be connected in a specific order?**
That’s a Steiner tree problem — NP-hard in general.

**Related Problems:** LC 1135 (Connecting Cities with Minimum Cost), LC 1168 (Optimize Water Distribution), LC 778 (Swim in Rising Water)
