# Rotting Oranges
**LeetCode 994** | Medium | Multi-source BFS

## Problem
Given a grid where `0`=empty, `1`=fresh orange, `2`=rotten orange.
Each minute, rotten oranges spread to adjacent fresh oranges.
Return the minimum minutes until no fresh oranges remain, or `-1` if impossible.

```
Input: grid = [[2,1,1],[1,1,0],[0,1,1]]
Output: 4

Input: grid = [[2,1,1],[0,1,1],[1,0,1]]
Output: -1  (bottom-left fresh orange is isolated)

Input: grid = [[0,2]]
Output: 0  (no fresh oranges)
```

## Approach 1: Multi-source BFS (Optimal)
Start BFS from all rotten oranges simultaneously. Count levels = minutes elapsed.

**Time:** O(M×N) | **Space:** O(M×N)

```java
public int orangesRotting(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    Queue<int[]> q = new LinkedList<>();
    int fresh = 0;

    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 2) q.offer(new int[]{i, j});
            else if (grid[i][j] == 1) fresh++;
        }

    if (fresh == 0) return 0;

    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    int minutes = 0;

    while (!q.isEmpty() && fresh > 0) {
        minutes++;
        int size = q.size();
        while (size-- > 0) {
            int[] cell = q.poll();
            for (int[] d : dirs) {
                int r = cell[0]+d[0], c = cell[1]+d[1];
                if (r >= 0 && r < m && c >= 0 && c < n && grid[r][c] == 1) {
                    grid[r][c] = 2;
                    fresh--;
                    q.offer(new int[]{r, c});
                }
            }
        }
    }
    return fresh == 0 ? minutes : -1;
}
```

## Approach 2: BFS with Time Tracking (Alternative)
Store time in the queue instead of counting levels.

**Time:** O(M×N) | **Space:** O(M×N)

```java
public int orangesRotting(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    Queue<int[]> q = new LinkedList<>();
    int fresh = 0;

    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 2) q.offer(new int[]{i, j, 0});
            else if (grid[i][j] == 1) fresh++;
        }

    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    int maxTime = 0;

    while (!q.isEmpty()) {
        int[] curr = q.poll();
        int r = curr[0], c = curr[1], t = curr[2];
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            if (nr >= 0 && nr < m && nc >= 0 && nc < n && grid[nr][nc] == 1) {
                grid[nr][nc] = 2;
                fresh--;
                maxTime = Math.max(maxTime, t + 1);
                q.offer(new int[]{nr, nc, t + 1});
            }
        }
    }
    return fresh == 0 ? maxTime : -1;
}
```

## Key Insight
Multi-source BFS: enqueue ALL rotten oranges at time 0, then spread level by level.
This naturally computes the minimum time since BFS explores in order of distance.
The key check: if `fresh > 0` after BFS, some oranges are unreachable → return -1.
