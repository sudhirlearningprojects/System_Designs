# Shortest Path in Binary Matrix
**LeetCode 1091** | Medium | BFS

## Problem
Given an `n×n` binary matrix, find the length of the shortest clear path from `(0,0)` to `(n-1,n-1)`.
A clear path only goes through cells with value `0`, moving in 8 directions.
Return `-1` if no such path exists.

```
Input: grid = [[0,1],[1,0]]
Output: 2

Input: grid = [[0,0,0],[1,1,0],[1,1,0]]
Output: 4

Input: grid = [[1,0,0],[1,1,0],[1,1,0]]
Output: -1  (start is blocked)
```

## Approach 1: BFS (Optimal)
**Time:** O(n²) | **Space:** O(n²)

```java
public int shortestPathBinaryMatrix(int[][] grid) {
    int n = grid.length;
    if (grid[0][0] == 1 || grid[n-1][n-1] == 1) return -1;
    if (n == 1) return 1;

    Queue<int[]> q = new LinkedList<>();
    q.offer(new int[]{0, 0, 1}); // [row, col, path_length]
    grid[0][0] = 1; // mark visited

    int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};

    while (!q.isEmpty()) {
        int[] curr = q.poll();
        int r = curr[0], c = curr[1], len = curr[2];
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (nr < 0 || nr >= n || nc < 0 || nc >= n || grid[nr][nc] != 0) continue;
            if (nr == n-1 && nc == n-1) return len + 1;
            grid[nr][nc] = 1; // mark visited
            q.offer(new int[]{nr, nc, len + 1});
        }
    }
    return -1;
}
```

## Approach 2: BFS with Separate Visited Array (Non-destructive)
**Time:** O(n²) | **Space:** O(n²)

```java
public int shortestPathBinaryMatrix(int[][] grid) {
    int n = grid.length;
    if (grid[0][0] == 1 || grid[n-1][n-1] == 1) return -1;
    if (n == 1) return 1;

    boolean[][] visited = new boolean[n][n];
    Queue<int[]> q = new LinkedList<>();
    q.offer(new int[]{0, 0}); visited[0][0] = true;
    int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
    int pathLen = 1;

    while (!q.isEmpty()) {
        pathLen++;
        int size = q.size();
        while (size-- > 0) {
            int[] curr = q.poll();
            for (int[] d : dirs) {
                int nr = curr[0]+d[0], nc = curr[1]+d[1];
                if (nr < 0 || nr >= n || nc < 0 || nc >= n || grid[nr][nc] != 0 || visited[nr][nc]) continue;
                if (nr == n-1 && nc == n-1) return pathLen;
                visited[nr][nc] = true;
                q.offer(new int[]{nr, nc});
            }
        }
    }
    return -1;
}
```

## Approach 3: A* Search (Heuristic)
Use Manhattan/Chebyshev distance as heuristic for faster search in practice.

**Time:** O(n² log n) worst case | **Space:** O(n²)

```java
public int shortestPathBinaryMatrix(int[][] grid) {
    int n = grid.length;
    if (grid[0][0] == 1 || grid[n-1][n-1] == 1) return -1;

    // [f_score, g_score, row, col] where f = g + h
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    pq.offer(new int[]{1, 1, 0, 0});
    int[][] dist = new int[n][n];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
    dist[0][0] = 1;

    int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int g = curr[1], r = curr[2], c = curr[3];
        if (r == n-1 && c == n-1) return g;
        if (g > dist[r][c]) continue;
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (nr < 0 || nr >= n || nc < 0 || nc >= n || grid[nr][nc] != 0) continue;
            int ng = g + 1;
            if (ng < dist[nr][nc]) {
                dist[nr][nc] = ng;
                int h = Math.max(Math.abs(nr-(n-1)), Math.abs(nc-(n-1))); // Chebyshev distance
                pq.offer(new int[]{ng + h, ng, nr, nc});
            }
        }
    }
    return -1;
}
```

## Key Insight
8-directional BFS on a grid. Mark cells as visited by setting them to `1` to avoid extra space.
Edge case: check if start `(0,0)` or end `(n-1,n-1)` is blocked before BFS.
Path length starts at 1 (the starting cell itself counts).
