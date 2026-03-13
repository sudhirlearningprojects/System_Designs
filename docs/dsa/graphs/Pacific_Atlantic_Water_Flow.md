# Pacific Atlantic Water Flow
**LeetCode 417** | Medium | Multi-source BFS/DFS

## Problem
Given an `m×n` matrix of heights, water flows to adjacent cells with height ≤ current.
Find all cells from which water can flow to **both** the Pacific (top/left) and Atlantic (bottom/right) oceans.

```
Input: heights = [[1,2,2,3,5],[3,2,3,4,4],[2,4,5,3,1],[6,7,1,4,5],[5,1,1,2,4]]
Output: [[0,4],[1,3],[1,4],[2,2],[3,0],[3,1],[4,0]]
```

## Approach 1: Reverse BFS from Oceans (Optimal)
Instead of flowing down from each cell, flow **uphill** from ocean borders.
A cell reachable from both oceans is an answer.

**Time:** O(M×N) | **Space:** O(M×N)

```java
public List<List<Integer>> pacificAtlantic(int[][] heights) {
    int m = heights.length, n = heights[0].length;
    boolean[][] pac = new boolean[m][n], atl = new boolean[m][n];
    Queue<int[]> pq = new LinkedList<>(), aq = new LinkedList<>();

    for (int i = 0; i < m; i++) {
        pq.offer(new int[]{i, 0});   pac[i][0] = true;
        aq.offer(new int[]{i, n-1}); atl[i][n-1] = true;
    }
    for (int j = 0; j < n; j++) {
        pq.offer(new int[]{0, j});   pac[0][j] = true;
        aq.offer(new int[]{m-1, j}); atl[m-1][j] = true;
    }

    bfs(heights, pq, pac);
    bfs(heights, aq, atl);

    List<List<Integer>> res = new ArrayList<>();
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (pac[i][j] && atl[i][j]) res.add(Arrays.asList(i, j));
    return res;
}

private void bfs(int[][] h, Queue<int[]> q, boolean[][] visited) {
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    while (!q.isEmpty()) {
        int[] cell = q.poll();
        for (int[] d : dirs) {
            int r = cell[0]+d[0], c = cell[1]+d[1];
            if (r < 0 || r >= h.length || c < 0 || c >= h[0].length
                || visited[r][c] || h[r][c] < h[cell[0]][cell[1]]) continue;
            visited[r][c] = true;
            q.offer(new int[]{r, c});
        }
    }
}
```

## Approach 2: DFS from Oceans

**Time:** O(M×N) | **Space:** O(M×N)

```java
public List<List<Integer>> pacificAtlantic(int[][] heights) {
    int m = heights.length, n = heights[0].length;
    boolean[][] pac = new boolean[m][n], atl = new boolean[m][n];

    for (int i = 0; i < m; i++) { dfs(heights, pac, i, 0); dfs(heights, atl, i, n-1); }
    for (int j = 0; j < n; j++) { dfs(heights, pac, 0, j); dfs(heights, atl, m-1, j); }

    List<List<Integer>> res = new ArrayList<>();
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (pac[i][j] && atl[i][j]) res.add(Arrays.asList(i, j));
    return res;
}

private void dfs(int[][] h, boolean[][] visited, int i, int j) {
    visited[i][j] = true;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int[] d : dirs) {
        int r = i+d[0], c = j+d[1];
        if (r >= 0 && r < h.length && c >= 0 && c < h[0].length
            && !visited[r][c] && h[r][c] >= h[i][j])
            dfs(h, visited, r, c);
    }
}
```

## Key Insight
Reverse the flow direction: instead of checking if water from cell (i,j) reaches the ocean,
start from ocean borders and find all cells that can "send" water to that ocean (height ≥ neighbor).
The intersection of both reachable sets is the answer.
