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
