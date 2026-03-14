# Surrounded Regions
**LeetCode 130** | Medium | DFS/BFS from Border

## Problem
Given an `m×n` board of `'X'` and `'O'`, capture all regions of `'O'` that are completely surrounded by `'X'`.
A region is NOT captured if any `'O'` in it is on the border or connected to a border `'O'`.

```
Input:
X X X X
X O O X
X X O X
X O X X

Output:
X X X X
X X X X
X X X X
X O X X
```

## Approach 1: DFS from Border (Optimal)
Mark all `'O'`s connected to the border as safe (`'S'`), then flip remaining `'O'`s to `'X'` and `'S'` back to `'O'`.

**Time:** O(M×N) | **Space:** O(M×N)

```java
public void solve(char[][] board) {
    int m = board.length, n = board[0].length;

    // Mark border-connected 'O's as safe
    for (int i = 0; i < m; i++) { dfs(board, i, 0); dfs(board, i, n-1); }
    for (int j = 0; j < n; j++) { dfs(board, 0, j); dfs(board, m-1, j); }

    // Flip: 'O' → 'X' (captured), 'S' → 'O' (safe)
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            board[i][j] = board[i][j] == 'S' ? 'O' : (board[i][j] == 'O' ? 'X' : board[i][j]);
}

private void dfs(char[][] board, int i, int j) {
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length || board[i][j] != 'O') return;
    board[i][j] = 'S';
    dfs(board, i+1, j); dfs(board, i-1, j);
    dfs(board, i, j+1); dfs(board, i, j-1);
}
```

## Approach 2: BFS from Border
**Time:** O(M×N) | **Space:** O(M×N)

```java
public void solve(char[][] board) {
    int m = board.length, n = board[0].length;
    Queue<int[]> q = new LinkedList<>();

    for (int i = 0; i < m; i++) { addIfO(board, q, i, 0); addIfO(board, q, i, n-1); }
    for (int j = 0; j < n; j++) { addIfO(board, q, 0, j); addIfO(board, q, m-1, j); }

    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    while (!q.isEmpty()) {
        int[] cell = q.poll();
        board[cell[0]][cell[1]] = 'S';
        for (int[] d : dirs) addIfO(board, q, cell[0]+d[0], cell[1]+d[1]);
    }

    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            board[i][j] = board[i][j] == 'S' ? 'O' : (board[i][j] == 'O' ? 'X' : board[i][j]);
}

private void addIfO(char[][] board, Queue<int[]> q, int i, int j) {
    if (i >= 0 && i < board.length && j >= 0 && j < board[0].length && board[i][j] == 'O') {
        board[i][j] = 'S'; q.offer(new int[]{i, j});
    }
}
```

## Approach 3: Union-Find
Connect all border `'O'`s to a virtual node. Any `'O'` connected to the virtual node is safe.

**Time:** O(M×N·α) | **Space:** O(M×N)

```java
public void solve(char[][] board) {
    int m = board.length, n = board[0].length;
    int[] parent = new int[m * n + 1], rank = new int[m * n + 1];
    int virtual = m * n;
    for (int i = 0; i <= m * n; i++) parent[i] = i;

    int[][] dirs = {{1,0},{0,1}};
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (board[i][j] != 'O') continue;
            if (i == 0 || i == m-1 || j == 0 || j == n-1) union(parent, rank, i*n+j, virtual);
            for (int[] d : dirs) {
                int r = i+d[0], c = j+d[1];
                if (r < m && c < n && board[r][c] == 'O') union(parent, rank, i*n+j, r*n+c);
            }
        }
    }

    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (board[i][j] == 'O' && find(parent, i*n+j) != find(parent, virtual))
                board[i][j] = 'X';
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
Think in reverse: instead of finding surrounded regions, find the SAFE regions (border-connected).
Everything else gets captured. The 3-pass approach (mark safe → flip captured → restore safe) is clean and O(M×N).

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| All `'X'` | No change | Nothing to capture |
| All `'O'` | No change | All border-connected |
| Single row/column | No change | All cells are on the border |
| `'O'` only in corners | No change | Corners are on the border |
| `'O'` completely surrounded | All flipped to `'X'` | No border connection |
| 1×1 board `[['O']]` | `[['O']]` | Single cell is on the border |
| 3×3 with center `'O'` | Center flipped | Center has no border connection |

---

## Dry Run

**Input:**
```
X X X X
X O O X
X X O X
X O X X
```

**Step 1: DFS from all border cells**
```
Border 'O' cells: (3,1) only

dfs(3,1): board[3][1]='O' → mark 'S'
  Up (2,1): board[2][1]='X' → stop
  Down (4,1): out of bounds → stop
  Left (3,0): board[3][0]='X' → stop
  Right (3,2): board[3][2]='X' → stop

After step 1:
X X X X
X O O X
X X O X
X S X X
```

**Step 2: Final pass**
```
'O' → 'X' (captured): (1,1),(1,2),(2,2)
'S' → 'O' (safe restored): (3,1)
'X' → 'X' (unchanged)

Result:
X X X X
X X X X
X X X X
X O X X
```

---

## Follow-up Questions

**Q: Why not just DFS from every `'O'` and check if it reaches the border?**
That’s O(M²×N²) in the worst case. The reverse approach (DFS from border) is O(M×N).

**Q: What if the board has only one row or one column?**
All cells are on the border, so nothing gets captured. The algorithm handles this correctly.

**Q: What does the virtual node in Union-Find represent?**
It’s a sentinel node that all border `'O'`s connect to. Any `'O'` in the same component as the virtual node is safe.

**Q: Can you solve this in-place without the `'S'` marker?**
Yes — use a separate `boolean[][] safe` array. But the in-place marker approach uses O(1) extra space.

**Related Problems:** LC 200 (Number of Islands), LC 417 (Pacific Atlantic Water Flow), LC 1020 (Number of Enclaves), LC 1254 (Number of Closed Islands)
