# Cheapest Flights Within K Stops
**LeetCode 787** | Medium | Bellman-Ford / BFS / Dijkstra

## Problem
Given `n` cities, `flights[i] = [from, to, price]`, find the cheapest price from `src` to `dst`
with at most `k` stops. Return `-1` if no such route exists.

```
Input: n=4, flights=[[0,1,100],[1,2,100],[2,0,100],[1,3,600],[2,3,200]], src=0, dst=3, k=1
Output: 700  →  0→1→3

Input: n=3, flights=[[0,1,100],[1,2,100],[0,2,500]], src=0, dst=2, k=1
Output: 200  →  0→1→2
```

## Approach 1: Bellman-Ford (K+1 Relaxations)
Relax edges exactly `k+1` times (k stops = k+1 edges).
Use a copy of prices to avoid using edges from the same iteration.

**Time:** O(K×E) | **Space:** O(n)

```java
public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    int[] prices = new int[n];
    Arrays.fill(prices, Integer.MAX_VALUE);
    prices[src] = 0;

    for (int i = 0; i <= k; i++) {
        int[] temp = Arrays.copyOf(prices, n);
        for (int[] f : flights) {
            if (prices[f[0]] == Integer.MAX_VALUE) continue;
            temp[f[1]] = Math.min(temp[f[1]], prices[f[0]] + f[2]);
        }
        prices = temp;
    }
    return prices[dst] == Integer.MAX_VALUE ? -1 : prices[dst];
}
```

## Approach 2: BFS Level-by-Level (K+1 levels)
**Time:** O(K×E) | **Space:** O(n)

```java
public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;

    for (int i = 0; i <= k; i++) {
        int[] temp = Arrays.copyOf(dist, n);
        for (int[] f : flights) {
            if (dist[f[0]] != Integer.MAX_VALUE)
                temp[f[1]] = Math.min(temp[f[1]], dist[f[0]] + f[2]);
        }
        dist = temp;
    }
    return dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
}
```

## Approach 3: Dijkstra with Stop Count
**Time:** O(E log(V×K)) | **Space:** O(V×K)

```java
public int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    Map<Integer, List<int[]>> adj = new HashMap<>();
    for (int[] f : flights) adj.computeIfAbsent(f[0], x -> new ArrayList<>()).add(new int[]{f[1], f[2]});

    // [cost, node, stops]
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{0, src, 0});
    int[] stops = new int[n];
    Arrays.fill(stops, Integer.MAX_VALUE);

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int cost = curr[0], node = curr[1], usedStops = curr[2];
        if (node == dst) return cost;
        if (usedStops > k || usedStops >= stops[node]) continue;
        stops[node] = usedStops;
        for (int[] next : adj.getOrDefault(node, new ArrayList<>()))
            pq.offer(new int[]{cost + next[1], next[0], usedStops + 1});
    }
    return -1;
}
```

## Key Insight
Bellman-Ford naturally limits path length by controlling the number of relaxation rounds.
The `temp` copy is critical — without it, a single iteration could chain multiple edges (violating the k-stop constraint).

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `src == dst` | 0 | Already at destination |
| `k=0` | direct flight price or -1 | Only direct flights allowed (0 stops) |
| No path within k stops | -1 | All paths require more stops |
| Cheaper path exists but needs k+1 stops | Use more expensive k-stop path | Constraint must be respected |
| Multiple edges between same cities | Use cheapest | Bellman-Ford handles naturally |
| Negative prices | Not in constraints | Problem guarantees non-negative prices |

---

## Dry Run

**Input:** `n=3, flights=[[0,1,100],[1,2,100],[0,2,500]], src=0, dst=2, k=1`

**Bellman-Ford trace (k+1=2 rounds):**
```
Initial: prices=[0, INF, INF]  (src=0)

--- Round 1 (i=0, using 1 edge) ---
temp = copy of prices = [0, INF, INF]

Flight [0,1,100]: prices[0]=0 != INF
  temp[1] = min(INF, 0+100) = 100
Flight [1,2,100]: prices[1]=INF → skip
Flight [0,2,500]: prices[0]=0 != INF
  temp[2] = min(INF, 0+500) = 500

prices = [0, 100, 500]

--- Round 2 (i=1, using 2 edges = 1 stop) ---
temp = copy of prices = [0, 100, 500]

Flight [0,1,100]: temp[1] = min(100, 0+100) = 100 (no change)
Flight [1,2,100]: prices[1]=100 != INF
  temp[2] = min(500, 100+100) = 200
Flight [0,2,500]: temp[2] = min(200, 0+500) = 200 (no change)

prices = [0, 100, 200]

Answer: prices[2] = 200
```

**Why temp copy matters:** Without it, in Round 1, after updating `prices[1]=100`, the flight `[1,2,100]` would immediately use it, giving `prices[2]=200` in round 1 — that’s a 2-edge path computed in 1 round, violating the k=0 constraint.

---

## Follow-up Questions

**Q: Why not use standard Dijkstra?**
Dijkstra doesn’t track the number of edges used. A node could be reached via a cheap but long path that exceeds k stops. The modified Dijkstra (Approach 3) tracks stops per state.

**Q: What if k is very large (effectively unlimited)?**
Run standard Dijkstra — the stop constraint becomes irrelevant.

**Q: Can prices be 0?**
Yes. The algorithm handles 0-weight edges correctly.

**Q: What’s the state space for the Dijkstra approach?**
O(V×K) states: each node can be visited with 0 to k stops used.

**Related Problems:** LC 743 (Network Delay Time), LC 1514 (Path with Max Probability), LC 1631 (Path with Minimum Effort)
