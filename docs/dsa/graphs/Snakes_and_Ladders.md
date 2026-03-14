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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| Already at `n²` | 0 | Start = end |
| Ladder from 1 to `n²` | 1 | One roll, instant win |
| Snake from near end back to start | More moves | Must reroute |
| Cycle of snakes/ladders | -1 | Infinite loop, never reach `n²` |
| `n=2` board | 1 or 2 | Small board |
| All `-1` (no snakes/ladders) | Minimum dice rolls | Pure BFS |
| Ladder destination has another ladder | Only one jump | Take only the first ladder |

---

## Dry Run

**Input (n=2):** `board = [[-1,-1],[-1,3]]`
```
Square layout (n=2):
  Row 1 (top):    3  4   → board[0][0..1]
  Row 0 (bottom): 1  2   → board[1][0..1]

board[0][0]=3 means square 3 has a ladder to 3 (no-op)
board[1][1]=-1 means square 2 has no snake/ladder

BFS:
Queue: [1], visited[1]=true, moves=0

--- moves=1 ---
Pop 1:
  dice=1: next=2, pos=getPosition(2,2)=(1,1), board[1][1]=-1 → next=2, enqueue
  dice=2: next=3, pos=getPosition(3,2)=(0,0), board[0][0]=3 → next=3
          next==n²=4? No. enqueue 3
  dice=3: next=4==n² → return moves+1=1? Wait, check: curr==n² check is at pop time
          Actually next=4, visited[4]=false, enqueue 4
  dice=4: next=5 > n²=4, break
Queue: [2,3,4]

--- moves=2 ---
Pop 2: curr=2 ≠ 4
Pop 3: curr=3 ≠ 4
Pop 4: curr=4 == n² → return moves=2

Answer: 2
```

---

## Follow-up Questions

**Q: Why mark visited BEFORE taking the snake/ladder?**
You mark the square number after applying the snake/ladder as visited. This prevents re-visiting the destination square via different paths.

**Q: What if a snake/ladder leads to another snake/ladder?**
The problem says you only follow one snake/ladder per landing. You don’t chain them.

**Q: Why BFS and not DFS?**
BFS guarantees the minimum number of moves. DFS would find a path but not necessarily the shortest.

**Q: How to handle the coordinate conversion for odd vs even n?**
The formula `row % 2 == 1 ? col = n-1-col : col` handles both cases. Row 0 from bottom is always left-to-right.

**Related Problems:** LC 752 (Open the Lock), LC 127 (Word Ladder), LC 1091 (Shortest Path in Binary Matrix)
