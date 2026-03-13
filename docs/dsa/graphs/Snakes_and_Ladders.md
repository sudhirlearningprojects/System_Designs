# Snakes and Ladders
**LeetCode 909** | Medium | BFS

## Problem
Given an `n×n` board where `-1` means no snake/ladder, and a positive value means a destination,
find the minimum number of dice rolls to reach square `n²` from square `1`.

Board is filled in Boustrophedon order (left-to-right on even rows from bottom, right-to-left on odd rows).

```
Input: board = [[-1,-1,-1,-1,-1,-1],[-1,-1,-1,-1,-1,-1],[-1,-1,-1,-1,-1,-1],
                [-1,35,-1,-1,13,-1],[-1,-1,-1,-1,-1,-1],[-1,15,-1,-1,-1,-1]]
Output: 4
```

## Approach: BFS (Shortest Path)
**Time:** O(n²) | **Space:** O(n²)

```java
public int snakesAndLadders(int[][] board) {
    int n = board.length;
    boolean[] visited = new boolean[n * n + 1];
    Queue<Integer> q = new LinkedList<>();
    q.offer(1); visited[1] = true;
    int moves = 0;

    while (!q.isEmpty()) {
        int size = q.size();
        while (size-- > 0) {
            int curr = q.poll();
            if (curr == n * n) return moves;
            for (int dice = 1; dice <= 6; dice++) {
                int next = curr + dice;
                if (next > n * n) break;
                int[] pos = getPosition(next, n);
                int dest = board[pos[0]][pos[1]];
                if (dest != -1) next = dest; // snake or ladder
                if (!visited[next]) { visited[next] = true; q.offer(next); }
            }
        }
        moves++;
    }
    return -1;
}

// Convert square number to board coordinates
private int[] getPosition(int square, int n) {
    int row = (square - 1) / n;
    int col = (square - 1) % n;
    // Bottom row is row 0 in board (board[n-1-row])
    // Even rows (from bottom) go left-to-right, odd rows go right-to-left
    if (row % 2 == 1) col = n - 1 - col;
    return new int[]{n - 1 - row, col};
}
```

## Key Insight
This is a shortest path problem on an implicit graph where each square connects to up to 6 others (dice rolls).
The tricky part is the coordinate conversion from square number to board position (Boustrophedon order).

```
For n=6, square numbering from bottom:
Row 0 (bottom): 1  2  3  4  5  6   → left to right  → board[5][0..5]
Row 1:          12 11 10  9  8  7   → right to left  → board[4][5..0]
Row 2:          13 14 15 16 17 18   → left to right  → board[3][0..5]
...
```

After landing on a square, immediately take the snake/ladder if present (before counting as a move).
