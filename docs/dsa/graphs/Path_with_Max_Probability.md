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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `start == end` | 1.0 | Already at destination, probability = 1 |
| No path from start to end | 0.0 | Unreachable |
| Single edge `start→end` | `succProb[0]` | Direct path |
| Multiple paths, pick max product | Dijkstra finds it | Greedy max-heap |
| All probabilities = 1.0 | 1.0 | Any path has probability 1 |
| All probabilities = 0.0 | 0.0 | No path has positive probability |
| Disconnected graph | 0.0 | end unreachable from start |

---

## Dry Run

**Input:** `n=3, edges=[[0,1],[1,2],[0,2]], succProb=[0.5,0.5,0.2], start=0, end=2`

**Dijkstra (max-heap) trace:**
```
Adj: {0:[(1,0.5),(2,0.2)], 1:[(0,0.5),(2,0.5)], 2:[(1,0.5),(0,0.2)]}
prob = [1.0, 0.0, 0.0]
pq (max-heap) = [(1.0, 0)]

--- Pop (1.0, 0) ---
p=1.0, node=0, not stale
Neighbors:
  (1, 0.5): newProb = 1.0*0.5 = 0.5 > prob[1]=0.0 → prob[1]=0.5, push (0.5,1)
  (2, 0.2): newProb = 1.0*0.2 = 0.2 > prob[2]=0.0 → prob[2]=0.2, push (0.2,2)
pq = [(0.5,1),(0.2,2)]

--- Pop (0.5, 1) ---
p=0.5, node=1, not stale
Neighbors:
  (0, 0.5): newProb = 0.5*0.5 = 0.25, prob[0]=1.0 → no update
  (2, 0.5): newProb = 0.5*0.5 = 0.25 > prob[2]=0.2 → prob[2]=0.25, push (0.25,2)
pq = [(0.25,2),(0.2,2)]

--- Pop (0.25, 2) ---
node=2 == end → return 0.25

Answer: 0.25
```

---

## Follow-up Questions

**Q: Why multiply probabilities instead of adding?**
Probabilities of independent events multiply. The probability of a path is the product of all edge probabilities along it.

**Q: Can you take the log to convert multiplication to addition?**
Yes! `log(p1 * p2) = log(p1) + log(p2)`. Maximize sum of `log(prob)` using standard Dijkstra (negate for min-heap). Careful with `log(0)`.

**Q: What if probabilities can be > 1?**
Not valid for probabilities. If weights represent something else (e.g., reliability scores), the same algorithm applies as long as maximizing product makes sense.

**Q: Does Bellman-Ford work here?**
Yes — relax edges n-1 times, maximizing probability. Useful if you need to handle all edge cases without a priority queue.

**Related Problems:** LC 743 (Network Delay Time), LC 787 (Cheapest Flights K Stops), LC 1631 (Path with Minimum Effort)
