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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| All `'1'`s | 1 | Entire grid is one island |
| All `'0'`s | 0 | No land at all |
| Single cell `[['1']]` | 1 | Minimal island |
| Single cell `[['0']]` | 0 | No island |
| Single row `[1,0,1,0,1]` | 3 | Isolated cells separated by water |
| Diagonal `'1'`s | n | Diagonal cells are NOT connected (only 4-directional) |
| 1×n all `'1'`s | 1 | One long horizontal island |

---

## Dry Run

**Input:**
```
1 1 0
0 1 0
0 0 1
```

**DFS trace (Approach 1):**
```
Scan (0,0): grid[0][0]='1' → count=1, call dfs(0,0)
  dfs(0,0): sink → '0', recurse right/down
    dfs(0,1): sink → '0', recurse right/down
      dfs(0,2): grid='0', return
      dfs(1,1): sink → '0', recurse
        dfs(2,1): grid='0', return
        dfs(1,2): grid='0', return
        dfs(1,0): grid='0', return
        dfs(0,1): already '0', return
    dfs(1,0): grid='0', return

Scan (0,1): already '0', skip
Scan (0,2): grid='0', skip
Scan (1,0): grid='0', skip
Scan (1,1): already '0', skip
...
Scan (2,2): grid[2][2]='1' → count=2, call dfs(2,2)
  dfs(2,2): sink → '0', all neighbors out of bounds or '0'

Final count = 2
```

---

## Follow-up Questions

**Q: What if diagonal connections also count as connected?**
Change 4-directional to 8-directional by adding `{1,1},{1,-1},{-1,1},{-1,-1}` to dirs.

**Q: Count the area of the largest island (LC 695)?**
Return the max DFS return value instead of incrementing a counter.

**Q: What if the grid is too large to fit in memory?**
Use Union-Find with streaming — process row by row, keeping only two rows in memory at a time.

**Q: Can you solve it without modifying the input?**
Yes — use a separate `boolean[][] visited` array instead of sinking cells.

**Q: What's the max recursion depth for DFS?**
O(M×N) in the worst case (one giant snake-shaped island). For very large grids, use iterative BFS to avoid stack overflow.

**Related Problems:** LC 695 (Max Area of Island), LC 463 (Island Perimeter), LC 827 (Making a Large Island), LC 1020 (Number of Enclaves)
