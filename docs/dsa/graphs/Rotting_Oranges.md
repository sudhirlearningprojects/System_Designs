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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| No fresh oranges | 0 | Already done |
| No rotten oranges, fresh exist | -1 | Nothing to rot them |
| Fresh orange surrounded by empty cells | -1 | Rotten can’t reach it |
| All rotten | 0 | No fresh to rot |
| Single fresh adjacent to rotten | 1 | One minute to rot |
| Fresh orange in corner, rotten far away | Distance in minutes | BFS handles |
| `[[2,2],[1,1]]` | 1 | Both rotten spread simultaneously |

---

## Dry Run

**Input:** `grid = [[2,1,1],[1,1,0],[0,1,1]]`

**Multi-source BFS trace:**
```
Initial scan:
  Rotten: (0,0) → enqueue
  Fresh: (0,1),(0,2),(1,0),(1,1),(2,1),(2,2) → fresh=6

Queue: [(0,0)], minutes=0

--- Minute 1 ---
Process (0,0):
  Right (0,1): fresh → rot, fresh=5, enqueue
  Down  (1,0): fresh → rot, fresh=4, enqueue
Queue: [(0,1),(1,0)], minutes=1

--- Minute 2 ---
Process (0,1):
  Right (0,2): fresh → rot, fresh=3, enqueue
  Down  (1,1): fresh → rot, fresh=2, enqueue
Process (1,0):
  Right (1,1): already rotten, skip
  Down  (2,0): empty (0), skip
Queue: [(0,2),(1,1)], minutes=2

--- Minute 3 ---
Process (0,2):
  Down (1,2): empty (0), skip
Process (1,1):
  Down (2,1): fresh → rot, fresh=1, enqueue
Queue: [(2,1)], minutes=3

--- Minute 4 ---
Process (2,1):
  Right (2,2): fresh → rot, fresh=0, enqueue
Queue: [(2,2)], minutes=4

fresh=0 → return 4
Answer: 4
```

---

## Follow-up Questions

**Q: What if rotten oranges can spread diagonally?**
Add diagonal directions `{1,1},{1,-1},{-1,1},{-1,-1}` to dirs.

**Q: What if fresh oranges can also become rotten on their own after T minutes?**
Add a timer per fresh orange; if not rotted by BFS within T minutes, it self-rots. This changes the problem significantly.

**Q: Why multi-source BFS instead of running BFS from each rotten orange separately?**
Separate BFS would give the time for each rotten orange independently, but oranges rot simultaneously. Multi-source BFS models this correctly in one pass.

**Q: What’s the space complexity of the queue?**
O(M×N) in the worst case — all cells could be in the queue at once.

**Related Problems:** LC 200 (Number of Islands), LC 1162 (As Far from Land as Possible), LC 286 (Walls and Gates), LC 542 (01 Matrix)
