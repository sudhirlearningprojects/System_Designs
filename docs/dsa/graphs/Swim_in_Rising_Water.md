# Swim in Rising Water
**LeetCode 778** | Hard | Binary Search + BFS / Dijkstra / Union-Find

## Problem
Given an `n×n` grid where `grid[i][j]` is the elevation at cell `(i,j)`, find the minimum time `t`
such that you can travel from `(0,0)` to `(n-1,n-1)`. At time `t`, you can swim to any cell with elevation ≤ t.

```
Input: grid = [[0,2],[1,3]]
Output: 3  →  wait until t=3, path: (0,0)→(0,1)→(1,1)

Input: grid = [[0,1,2,3,4],[24,23,22,21,5],[12,13,14,15,16],[11,17,18,19,20],[10,9,8,7,6]]
Output: 16
```

## Approach 1: Dijkstra (Min-Heap on max elevation)
Treat the problem as finding the path that minimizes the maximum elevation encountered.

**Time:** O(n² log n) | **Space:** O(n²)

```java
public int swimInWater(int[][] grid) {
    int n = grid.length;
    int[][] dist = new int[n][n];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
    dist[0][0] = grid[0][0];

    // [max_elevation_so_far, row, col]
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{grid[0][0], 0, 0});
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int t = curr[0], r = curr[1], c = curr[2];
        if (r == n-1 && c == n-1) return t;
        if (t > dist[r][c]) continue;
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue;
            int newT = Math.max(t, grid[nr][nc]);
            if (newT < dist[nr][nc]) { dist[nr][nc] = newT; pq.offer(new int[]{newT, nr, nc}); }
        }
    }
    return -1;
}
```

## Approach 2: Binary Search + BFS
Binary search on the answer `t`, check if path exists using BFS.

**Time:** O(n² log n) | **Space:** O(n²)

```java
public int swimInWater(int[][] grid) {
    int n = grid.length, lo = grid[0][0], hi = n * n - 1;
    while (lo < hi) {
        int mid = (lo + hi) / 2;
        if (canReach(grid, mid)) hi = mid;
        else lo = mid + 1;
    }
    return lo;
}

private boolean canReach(int[][] grid, int t) {
    int n = grid.length;
    if (grid[0][0] > t) return false;
    boolean[][] visited = new boolean[n][n];
    Queue<int[]> q = new LinkedList<>();
    q.offer(new int[]{0, 0}); visited[0][0] = true;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    while (!q.isEmpty()) {
        int[] cell = q.poll();
        if (cell[0] == n-1 && cell[1] == n-1) return true;
        for (int[] d : dirs) {
            int r = cell[0]+d[0], c = cell[1]+d[1];
            if (r >= 0 && r < n && c >= 0 && c < n && !visited[r][c] && grid[r][c] <= t) {
                visited[r][c] = true; q.offer(new int[]{r, c});
            }
        }
    }
    return false;
}
```

## Approach 3: Union-Find (Sort cells by elevation)
Sort all cells by elevation, add them one by one, check when (0,0) and (n-1,n-1) connect.

**Time:** O(n² log n) | **Space:** O(n²)

```java
public int swimInWater(int[][] grid) {
    int n = grid.length;
    int[] parent = new int[n * n], rank = new int[n * n];
    for (int i = 0; i < n * n; i++) parent[i] = i;

    int[][] cells = new int[n * n][3]; // [elevation, row, col]
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++) cells[i*n+j] = new int[]{grid[i][j], i, j};
    Arrays.sort(cells, Comparator.comparingInt(a -> a[0]));

    boolean[][] added = new boolean[n][n];
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int[] cell : cells) {
        int t = cell[0], r = cell[1], c = cell[2];
        added[r][c] = true;
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (nr >= 0 && nr < n && nc >= 0 && nc < n && added[nr][nc])
                union(parent, rank, r*n+c, nr*n+nc);
        }
        if (find(parent, 0) == find(parent, (n-1)*n+(n-1))) return t;
    }
    return -1;
}

private int find(int[] p, int x) { return p[x] == x ? x : (p[x] = find(p, p[x])); }
private void union(int[] p, int[] r, int x, int y) {
    int px = find(p, x), py = find(p, y);
    if (px == py) return;
    if (r[px] < r[py]) { int t = px; px = py; py = t; }
    p[py] = px; if (r[px] == r[py]) r[px]++;
}
```

## Key Insight
This is a "minimax path" problem — minimize the maximum value along the path.
Dijkstra with `max(current_t, next_elevation)` as the edge weight solves it optimally.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `n=1` grid `[[0]]` | 0 | Already at destination |
| `n=2` grid `[[0,2],[1,3]]` | 3 | Must pass through elevation 3 |
| Monotonically increasing path | max elevation on path | No choice but to go through high cells |
| Multiple equal-cost paths | Any minimum | Dijkstra finds one |
| `grid[0][0]` is the max value | `grid[0][0]` | Must wait for start cell itself |
| Spiral grid (LC example) | 16 | Must traverse the spiral |

---

## Dry Run

**Input:** `grid = [[0,2],[1,3]]`

**Dijkstra trace (minimax):**
```
dist = [[0,INF],[INF,INF]]
pq = [(t=0, r=0, c=0)]

--- Pop (0, 0, 0) ---
t=0, not stale
Neighbors:
  (0,1): newT = max(0, grid[0][1]=2) = 2 < INF → dist[0][1]=2, push (2,0,1)
  (1,0): newT = max(0, grid[1][0]=1) = 1 < INF → dist[1][0]=1, push (1,1,0)
pq = [(1,1,0),(2,0,1)]

--- Pop (1, 1, 0) ---
t=1, not stale (dist[1][0]=1)
Neighbors:
  (0,0): newT = max(1, 0) = 1, dist[0][0]=0 → no update
  (1,1): newT = max(1, grid[1][1]=3) = 3 < INF → dist[1][1]=3, push (3,1,1)
pq = [(2,0,1),(3,1,1)]

--- Pop (2, 0, 1) ---
t=2, not stale (dist[0][1]=2)
Neighbors:
  (0,0): newT = max(2,0)=2, no update
  (1,1): newT = max(2, grid[1][1]=3) = 3, dist[1][1]=3 → no update (equal)
pq = [(3,1,1)]

--- Pop (3, 1, 1) ---
r=1, c=1 == n-1=1 → return t=3

Answer: 3
```

---

## Follow-up Questions

**Q: What’s the difference between this and standard Dijkstra?**
Standard Dijkstra minimizes the **sum** of edge weights. Here we minimize the **maximum** edge weight on the path. The relaxation changes from `dist[u] + w` to `max(dist[u], w)`.

**Q: Why does Binary Search + BFS also work?**
The answer is monotonic: if you can reach the destination at time `t`, you can also reach it at time `t+1`. Binary search on `t`, BFS checks feasibility.

**Q: When would Union-Find be preferred?**
When you need to process many queries offline — sort cells once, answer all queries by checking connectivity.

**Q: What if movement is 8-directional?**
Add diagonal directions to `dirs`. The algorithm remains the same.

**Related Problems:** LC 1631 (Path with Minimum Effort), LC 1514 (Path with Max Probability), LC 787 (Cheapest Flights K Stops)
