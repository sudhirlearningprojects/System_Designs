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
