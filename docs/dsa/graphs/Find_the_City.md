# Find the City With the Smallest Number of Neighbors at a Threshold Distance
**LeetCode 1334** | Medium | Floyd-Warshall

## Problem
Given `n` cities, `edges[i] = [from, to, weight]`, and a `distanceThreshold`,
find the city with the fewest number of cities reachable within the threshold.
If tie, return the city with the greatest index.

```
Input: n=4, edges=[[0,1,3],[1,2,1],[1,3,4],[2,3,1]], distanceThreshold=4
Output: 3
Explanation: City 0→{1,2,3}=3, City 1→{0,2,3}=3, City 2→{0,1,3}=3, City 3→{1,2}=2 ← winner

Input: n=5, edges=[[0,1,2],[0,4,8],[1,2,3],[1,4,2],[2,3,1],[3,4,1]], distanceThreshold=2
Output: 0
```

## Approach 1: Floyd-Warshall (Optimal)
Compute all-pairs shortest paths, then count reachable cities per node.

**Time:** O(n³) | **Space:** O(n²)

```java
public int findTheCity(int n, int[][] edges, int distanceThreshold) {
    int[][] dist = new int[n][n];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE / 2);
    for (int i = 0; i < n; i++) dist[i][i] = 0;
    for (int[] e : edges) { dist[e[0]][e[1]] = e[2]; dist[e[1]][e[0]] = e[2]; }

    // Floyd-Warshall
    for (int k = 0; k < n; k++)
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);

    int result = -1, minNeighbors = n;
    for (int i = 0; i < n; i++) {
        int count = 0;
        for (int j = 0; j < n; j++)
            if (i != j && dist[i][j] <= distanceThreshold) count++;
        if (count <= minNeighbors) { minNeighbors = count; result = i; } // >= to prefer larger index
    }
    return result;
}
```

## Approach 2: Dijkstra from Each Node
**Time:** O(n × (V+E) log V) | **Space:** O(V+E)

```java
public int findTheCity(int n, int[][] edges, int distanceThreshold) {
    Map<Integer, List<int[]>> adj = new HashMap<>();
    for (int[] e : edges) {
        adj.computeIfAbsent(e[0], k -> new ArrayList<>()).add(new int[]{e[1], e[2]});
        adj.computeIfAbsent(e[1], k -> new ArrayList<>()).add(new int[]{e[0], e[2]});
    }

    int result = -1, minNeighbors = n;
    for (int src = 0; src < n; src++) {
        int[] dist = dijkstra(adj, n, src);
        int count = 0;
        for (int j = 0; j < n; j++)
            if (j != src && dist[j] <= distanceThreshold) count++;
        if (count <= minNeighbors) { minNeighbors = count; result = src; }
    }
    return result;
}

private int[] dijkstra(Map<Integer, List<int[]>> adj, int n, int src) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{0, src});
    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        if (curr[0] > dist[curr[1]]) continue;
        for (int[] next : adj.getOrDefault(curr[1], new ArrayList<>())) {
            int newDist = dist[curr[1]] + next[1];
            if (newDist < dist[next[0]]) { dist[next[0]] = newDist; pq.offer(new int[]{newDist, next[0]}); }
        }
    }
    return dist;
}
```

## Key Insight
Floyd-Warshall is ideal here since n ≤ 100 (n³ = 10⁶ operations).
Iterate cities in order 0..n-1 and use `<=` for tie-breaking — the last city with minimum count wins (largest index).

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `n=2, threshold=0` | 1 (larger index) | No city reachable within 0 distance (self excluded) |
| All cities connected with weight 1, threshold=1 | City n-1 | All have same count, pick largest index |
| Disconnected graph | City with fewest reachable | Unreachable cities don’t count |
| `threshold` very large | City with fewest total neighbors | All paths within threshold |
| Single city `n=1` | 0 | Only one city, no neighbors |
| Tie between two cities | Larger index wins | Problem constraint |

---

## Dry Run

**Input:** `n=4, edges=[[0,1,3],[1,2,1],[1,3,4],[2,3,1]], distanceThreshold=4`

**Floyd-Warshall:**
```
Initial dist (INF/2 for unreachable, 0 diagonal):
     0    1    2    3
0  [ 0,   3, INF, INF]
1  [ 3,   0,   1,   4]
2  [INF,  1,   0,   1]
3  [INF,  4,   1,   0]

After Floyd-Warshall (k=0,1,2,3):
     0    1    2    3
0  [ 0,   3,   4,   5]  ← 0→3 via 0→1→2→3 = 3+1+1=5
1  [ 3,   0,   1,   2]  ← 1→3 via 1→2→3 = 1+1=2
2  [ 4,   1,   0,   1]
3  [ 5,   2,   1,   0]
```

**Count reachable within threshold=4:**
```
City 0: dist[0][1]=3≤4✓, dist[0][2]=4≤4✓, dist[0][3]=5>4✗ → count=2
City 1: dist[1][0]=3≤4✓, dist[1][2]=1≤4✓, dist[1][3]=2≤4✓ → count=3
City 2: dist[2][0]=4≤4✓, dist[2][1]=1≤4✓, dist[2][3]=1≤4✓ → count=3
City 3: dist[3][0]=5>4✗, dist[3][1]=2≤4✓, dist[3][2]=1≤4✓ → count=2

Min count=2, cities with count=2: {0, 3} → pick largest index = 3
Answer: 3
```

---

## Follow-up Questions

**Q: Why Floyd-Warshall instead of running Dijkstra from each node?**
For n≤100, Floyd-Warshall is simpler to implement and O(n³) = 10⁶ is fast enough. Dijkstra from each node is O(n² log n) — asymptotically better but more code.

**Q: What if the graph is directed?**
Floyd-Warshall handles directed graphs naturally — just don’t add the reverse edge when initializing.

**Q: What if `distanceThreshold = 0`?**
No city is reachable from any other (self-loops excluded). All cities have count=0, return city n-1.

**Q: How to handle the tie-breaking correctly in code?**
Iterate `i` from 0 to n-1 and use `count <= minNeighbors` (not `<`). The last city satisfying this condition has the largest index.

**Related Problems:** LC 743 (Network Delay Time), LC 1514 (Path with Max Probability), LC 787 (Cheapest Flights K Stops)
