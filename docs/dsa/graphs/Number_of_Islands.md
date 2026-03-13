# Number of Islands
**LeetCode 200** | Medium | BFS/DFS

## Problem
Given a 2D grid of `'1'` (land) and `'0'` (water), count the number of islands.
An island is surrounded by water and formed by connecting adjacent lands horizontally or vertically.

```
Input:
11110
11010
11000
00000
Output: 1

Input:
11000
11000
00100
00011
Output: 3
```

## Approach 1: DFS (Flood Fill)
Mark visited land cells by sinking them (`'1'` → `'0'`). Each DFS call sinks one full island.

**Time:** O(M×N) | **Space:** O(M×N) recursion stack

```java
public int numIslands(char[][] grid) {
    int count = 0;
    for (int i = 0; i < grid.length; i++)
        for (int j = 0; j < grid[0].length; j++)
            if (grid[i][j] == '1') { dfs(grid, i, j); count++; }
    return count;
}

private void dfs(char[][] grid, int i, int j) {
    if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] != '1') return;
    grid[i][j] = '0';
    dfs(grid, i+1, j); dfs(grid, i-1, j);
    dfs(grid, i, j+1); dfs(grid, i, j-1);
}
```

## Approach 2: BFS
**Time:** O(M×N) | **Space:** O(min(M,N))

```java
public int numIslands(char[][] grid) {
    int count = 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] != '1') continue;
            count++;
            Queue<int[]> q = new LinkedList<>();
            q.offer(new int[]{i, j});
            grid[i][j] = '0';
            while (!q.isEmpty()) {
                int[] cell = q.poll();
                for (int[] d : dirs) {
                    int r = cell[0]+d[0], c = cell[1]+d[1];
                    if (r >= 0 && r < grid.length && c >= 0 && c < grid[0].length && grid[r][c] == '1') {
                        grid[r][c] = '0';
                        q.offer(new int[]{r, c});
                    }
                }
            }
        }
    }
    return count;
}
```

## Approach 3: Union-Find
**Time:** O(M×N·α) | **Space:** O(M×N)

```java
public int numIslands(char[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[] parent = new int[m * n];
    int[] rank = new int[m * n];
    int count = 0;
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (grid[i][j] == '1') { parent[i*n+j] = i*n+j; count++; }

    int[][] dirs = {{1,0},{0,1}};
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] != '1') continue;
            for (int[] d : dirs) {
                int r = i+d[0], c = j+d[1];
                if (r < m && c < n && grid[r][c] == '1' && union(parent, rank, i*n+j, r*n+c)) count--;
            }
        }
    }
    return count;
}

private int find(int[] parent, int x) {
    if (parent[x] != x) parent[x] = find(parent, parent[x]);
    return parent[x];
}

private boolean union(int[] parent, int[] rank, int x, int y) {
    int px = find(parent, x), py = find(parent, y);
    if (px == py) return false;
    if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
    parent[py] = px;
    if (rank[px] == rank[py]) rank[px]++;
    return true;
}
```

## Key Insight
- DFS/BFS: sink visited land to avoid revisiting — no extra visited array needed
- Union-Find: count components by starting with all land cells and merging adjacent ones
