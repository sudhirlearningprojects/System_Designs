# Path with Maximum Probability
**LeetCode 1514** | Medium | Dijkstra (Max Probability)

## Problem
Given `n` nodes, `edges[i] = [u, v]` with `succProb[i]` (probability of success), find the path
from `start` to `end` with the maximum probability of success.

```
Input: n=3, edges=[[0,1],[1,2],[0,2]], succProb=[0.5,0.5,0.2], start=0, end=2
Output: 0.25  →  0→1→2: 0.5×0.5=0.25 > 0→2: 0.2

Input: n=3, edges=[[0,1],[1,2],[0,2]], succProb=[0.5,0.5,0.3], start=0, end=2
Output: 0.3  →  0→2 directly
```

## Approach 1: Dijkstra (Max-Heap on Probability)
Maximize probability instead of minimizing distance. Use max-heap.

**Time:** O((V+E) log V) | **Space:** O(V+E)

```java
public double maxProbability(int n, int[][] edges, double[] succProb, int start, int end) {
    Map<Integer, List<double[]>> adj = new HashMap<>();
    for (int i = 0; i < edges.length; i++) {
        adj.computeIfAbsent(edges[i][0], k -> new ArrayList<>()).add(new double[]{edges[i][1], succProb[i]});
        adj.computeIfAbsent(edges[i][1], k -> new ArrayList<>()).add(new double[]{edges[i][0], succProb[i]});
    }

    double[] prob = new double[n];
    prob[start] = 1.0;

    // Max-heap: [probability, node]
    PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(b[0], a[0]));
    pq.offer(new double[]{1.0, start});

    while (!pq.isEmpty()) {
        double[] curr = pq.poll();
        double p = curr[0]; int node = (int) curr[1];
        if (node == end) return p;
        if (p < prob[node]) continue; // stale
        for (double[] next : adj.getOrDefault(node, new ArrayList<>())) {
            double newProb = p * next[1];
            if (newProb > prob[(int) next[0]]) {
                prob[(int) next[0]] = newProb;
                pq.offer(new double[]{newProb, next[0]});
            }
        }
    }
    return 0.0;
}
```

## Approach 2: Bellman-Ford
Relax all edges `n-1` times, maximizing probability.

**Time:** O(V×E) | **Space:** O(V)

```java
public double maxProbability(int n, int[][] edges, double[] succProb, int start, int end) {
    double[] prob = new double[n];
    prob[start] = 1.0;

    for (int i = 0; i < n - 1; i++) {
        boolean updated = false;
        for (int j = 0; j < edges.length; j++) {
            int u = edges[j][0], v = edges[j][1];
            double p = succProb[j];
            if (prob[u] * p > prob[v]) { prob[v] = prob[u] * p; updated = true; }
            if (prob[v] * p > prob[u]) { prob[u] = prob[v] * p; updated = true; }
        }
        if (!updated) break; // early termination
    }
    return prob[end];
}
```

## Approach 3: BFS (Relaxation-based)
**Time:** O(V×E) | **Space:** O(V+E)

```java
public double maxProbability(int n, int[][] edges, double[] succProb, int start, int end) {
    Map<Integer, List<double[]>> adj = new HashMap<>();
    for (int i = 0; i < edges.length; i++) {
        adj.computeIfAbsent(edges[i][0], k -> new ArrayList<>()).add(new double[]{edges[i][1], succProb[i]});
        adj.computeIfAbsent(edges[i][1], k -> new ArrayList<>()).add(new double[]{edges[i][0], succProb[i]});
    }

    double[] prob = new double[n];
    prob[start] = 1.0;
    Queue<Integer> q = new LinkedList<>();
    q.offer(start);

    while (!q.isEmpty()) {
        int node = q.poll();
        for (double[] next : adj.getOrDefault(node, new ArrayList<>())) {
            int nb = (int) next[0];
            double newProb = prob[node] * next[1];
            if (newProb > prob[nb]) { prob[nb] = newProb; q.offer(nb); }
        }
    }
    return prob[end];
}
```

## Key Insight
Same as Dijkstra for shortest path, but we maximize instead of minimize.
Use a max-heap and multiply probabilities (instead of adding distances).
Probabilities are always in [0,1], so no negative weight issues.
