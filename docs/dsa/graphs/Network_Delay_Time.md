# Network Delay Time
**LeetCode 743** | Medium | Dijkstra

## Problem
Given `n` nodes, `times[i] = [u, v, w]` (directed edge with weight), and a source `k`,
find the minimum time for all nodes to receive the signal. Return `-1` if not all nodes are reachable.

```
Input: times = [[2,1,1],[2,3,1],[3,4,1]], n=4, k=2
Output: 2

Input: times = [[1,2,1]], n=2, k=1
Output: 1

Input: times = [[1,2,1]], n=2, k=2
Output: -1
```

## Approach 1: Dijkstra (Optimal)
**Time:** O((V+E) log V) | **Space:** O(V+E)

```java
public int networkDelayTime(int[][] times, int n, int k) {
    Map<Integer, List<int[]>> adj = new HashMap<>();
    for (int[] t : times) adj.computeIfAbsent(t[0], x -> new ArrayList<>()).add(new int[]{t[1], t[2]});

    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;

    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{0, k});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int d = curr[0], node = curr[1];
        if (d > dist[node]) continue; // stale entry
        for (int[] next : adj.getOrDefault(node, new ArrayList<>())) {
            int newDist = dist[node] + next[1];
            if (newDist < dist[next[0]]) {
                dist[next[0]] = newDist;
                pq.offer(new int[]{newDist, next[0]});
            }
        }
    }

    int maxDist = 0;
    for (int i = 1; i <= n; i++) {
        if (dist[i] == Integer.MAX_VALUE) return -1;
        maxDist = Math.max(maxDist, dist[i]);
    }
    return maxDist;
}
```

## Approach 2: Bellman-Ford
**Time:** O(V×E) | **Space:** O(V)

```java
public int networkDelayTime(int[][] times, int n, int k) {
    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;

    for (int i = 0; i < n - 1; i++)
        for (int[] t : times)
            if (dist[t[0]] != Integer.MAX_VALUE && dist[t[0]] + t[2] < dist[t[1]])
                dist[t[1]] = dist[t[0]] + t[2];

    int maxDist = 0;
    for (int i = 1; i <= n; i++) {
        if (dist[i] == Integer.MAX_VALUE) return -1;
        maxDist = Math.max(maxDist, dist[i]);
    }
    return maxDist;
}
```

## Approach 3: Floyd-Warshall (All-Pairs)
**Time:** O(V³) | **Space:** O(V²) — overkill for single source but shows the pattern

```java
public int networkDelayTime(int[][] times, int n, int k) {
    int[][] dist = new int[n+1][n+1];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE / 2);
    for (int i = 1; i <= n; i++) dist[i][i] = 0;
    for (int[] t : times) dist[t[0]][t[1]] = t[2];

    for (int mid = 1; mid <= n; mid++)
        for (int i = 1; i <= n; i++)
            for (int j = 1; j <= n; j++)
                dist[i][j] = Math.min(dist[i][j], dist[i][mid] + dist[mid][j]);

    int maxDist = 0;
    for (int i = 1; i <= n; i++) {
        if (dist[k][i] >= Integer.MAX_VALUE / 2) return -1;
        maxDist = Math.max(maxDist, dist[k][i]);
    }
    return maxDist;
}
```

## Key Insight
The answer is the maximum shortest-path distance from `k` to any node.
If any node is unreachable (dist = ∞), return -1.
Dijkstra is optimal for non-negative weights — use it here.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `n=1, times=[], k=1` | 0 | Only source node, already reached |
| Source can’t reach all nodes | -1 | Disconnected directed graph |
| Multiple edges between same nodes | Use shortest | Dijkstra handles naturally |
| Self-loop `[k,k,5]` | Valid | Ignored since dist[k]=0 already |
| All edges point away from k | max weight | All reachable, answer = max dist |
| `k` not in 1..n | Invalid | Problem guarantees valid k |

---

## Dry Run

**Input:** `times=[[2,1,1],[2,3,1],[3,4,1]], n=4, k=2`

**Dijkstra trace:**
```
Adj list: {2: [(1,1),(3,1)], 3: [(4,1)]}
dist = [INF, INF, 0, INF, INF]  (1-indexed, dist[2]=0)
pq = [(0,2)]

--- Pop (0,2) ---
d=0, node=2, not stale
Neighbors: (1,w=1), (3,w=1)
  dist[1] = min(INF, 0+1) = 1 → push (1,1)
  dist[3] = min(INF, 0+1) = 1 → push (1,3)
pq = [(1,1),(1,3)]

--- Pop (1,1) ---
d=1, node=1, not stale
No outgoing edges from node 1
pq = [(1,3)]

--- Pop (1,3) ---
d=1, node=3, not stale
Neighbors: (4,w=1)
  dist[4] = min(INF, 1+1) = 2 → push (2,4)
pq = [(2,4)]

--- Pop (2,4) ---
d=2, node=4, not stale
No outgoing edges
pq = []

dist = [INF, 1, 0, 1, 2]
max(dist[1..4]) = max(1,0,1,2) = 2
Answer: 2
```

---

## Follow-up Questions

**Q: What if the graph has negative weights?**
Use Bellman-Ford instead. Dijkstra is incorrect with negative weights.

**Q: What if you only need to reach a specific target node?**
Stop Dijkstra as soon as the target is popped from the priority queue — that’s its shortest distance.

**Q: Why does Dijkstra skip stale entries (`d > dist[node]`)?**
A node can be pushed multiple times with different distances. When popped, if the stored distance is outdated (a shorter path was already found), skip it.

**Q: Can you use BFS here?**
Only if all edge weights are equal. For weighted graphs, BFS gives incorrect shortest paths.

**Related Problems:** LC 787 (Cheapest Flights K Stops), LC 1514 (Path with Max Probability), LC 1334 (Find the City), LC 1631 (Path with Minimum Effort)
