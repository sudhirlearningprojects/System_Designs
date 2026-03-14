# BFS/DFS - Medium Problems (50%)

## 📚 10 Medium Problems with Complete Solutions

---

## Problem 1: Binary Tree Right Side View

**Difficulty**: Medium  
**Pattern**: BFS  
**LeetCode**: #199

### Problem Statement

Given the root of a binary tree, imagine yourself standing on the right side of it. Return the values of the nodes you can see ordered from top to bottom.

### Examples

```
Input: root = [1,2,3,null,5,null,4]
      1
     / \
    2   3
     \   \
      5   4
Output: [1,3,4]
```

### Solution

```java
public class BinaryTreeRightSideView {
    public List<Integer> rightSideView(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                
                // Last node in level is rightmost
                if (i == levelSize - 1) {
                    result.add(node.val);
                }
                
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
        }
        
        return result;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(w) where w is max width

### Test Cases

```java
@Test
public void testRightSideView() {
    BinaryTreeRightSideView solution = new BinaryTreeRightSideView();
    
    TreeNode root = new TreeNode(1);
    root.left = new TreeNode(2);
    root.right = new TreeNode(3);
    root.left.right = new TreeNode(5);
    root.right.right = new TreeNode(4);
    
    assertEquals(Arrays.asList(1, 3, 4), solution.rightSideView(root));
}
```

---

## Problem 2: Number of Islands

**Difficulty**: Medium  
**Pattern**: DFS (Matrix)  
**LeetCode**: #200

### Problem Statement

Given an `m x n` 2D binary grid which represents a map of '1's (land) and '0's (water), return the number of islands. An island is surrounded by water and formed by connecting adjacent lands horizontally or vertically.

### Examples

```
Input: grid = [
  ["1","1","1","1","0"],
  ["1","1","0","1","0"],
  ["1","1","0","0","0"],
  ["0","0","0","0","0"]
]
Output: 1

Input: grid = [
  ["1","1","0","0","0"],
  ["1","1","0","0","0"],
  ["0","0","1","0","0"],
  ["0","0","0","1","1"]
]
Output: 3
```

### Solution

```java
public class NumberOfIslands {
    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int count = 0;
        int m = grid.length;
        int n = grid[0].length;
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;
                    dfs(grid, i, j);
                }
            }
        }
        
        return count;
    }
    
    private void dfs(char[][] grid, int i, int j) {
        int m = grid.length;
        int n = grid[0].length;
        
        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == '0') {
            return;
        }
        
        grid[i][j] = '0'; // Mark as visited
        
        dfs(grid, i + 1, j);
        dfs(grid, i - 1, j);
        dfs(grid, i, j + 1);
        dfs(grid, i, j - 1);
    }
}
```

### Dry Run

**Input**: 
```
1 1 0
1 0 0
0 0 1
```

```
Start: count = 0

(0,0) = '1':
  count = 1
  dfs(0,0):
    Mark (0,0) = '0'
    dfs(1,0): Mark (1,0) = '0'
      dfs(2,0): grid[2][0] = '0', return
      dfs(0,0): grid[0][0] = '0', return
      dfs(1,1): grid[1][1] = '0', return
      dfs(1,-1): out of bounds
    dfs(-1,0): out of bounds
    dfs(0,1): Mark (0,1) = '0'
      dfs(1,1): grid[1][1] = '0', return
      dfs(-1,1): out of bounds
      dfs(0,2): grid[0][2] = '0', return
      dfs(0,0): grid[0][0] = '0', return
    dfs(0,-1): out of bounds

Grid now:
0 0 0
0 0 0
0 0 1

(2,2) = '1':
  count = 2
  dfs(2,2): Mark (2,2) = '0'

Final count = 2
```

### Complexity Analysis

- **Time Complexity**: O(m × n)
- **Space Complexity**: O(m × n) for recursion stack

### Test Cases

```java
@Test
public void testNumIslands() {
    NumberOfIslands solution = new NumberOfIslands();
    
    char[][] grid1 = {
        {'1','1','1','1','0'},
        {'1','1','0','1','0'},
        {'1','1','0','0','0'},
        {'0','0','0','0','0'}
    };
    assertEquals(1, solution.numIslands(grid1));
    
    char[][] grid2 = {
        {'1','1','0','0','0'},
        {'1','1','0','0','0'},
        {'0','0','1','0','0'},
        {'0','0','0','1','1'}
    };
    assertEquals(3, solution.numIslands(grid2));
}
```

---

## Problem 3: Clone Graph

**Difficulty**: Medium  
**Pattern**: DFS/BFS  
**LeetCode**: #133

### Problem Statement

Given a reference of a node in a connected undirected graph, return a deep copy (clone) of the graph.

### Solution

```java
public class CloneGraph {
    public Node cloneGraph(Node node) {
        if (node == null) return null;
        
        Map<Node, Node> visited = new HashMap<>();
        return dfs(node, visited);
    }
    
    private Node dfs(Node node, Map<Node, Node> visited) {
        if (visited.containsKey(node)) {
            return visited.get(node);
        }
        
        Node clone = new Node(node.val);
        visited.put(node, clone);
        
        for (Node neighbor : node.neighbors) {
            clone.neighbors.add(dfs(neighbor, visited));
        }
        
        return clone;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V)

---

## Problem 4: Course Schedule

**Difficulty**: Medium  
**Pattern**: DFS (Cycle Detection)  
**LeetCode**: #207

### Problem Statement

There are `numCourses` courses labeled from `0` to `numCourses - 1`. You are given an array `prerequisites` where `prerequisites[i] = [ai, bi]` indicates you must take course `bi` before `ai`. Return `true` if you can finish all courses.

### Examples

```
Input: numCourses = 2, prerequisites = [[1,0]]
Output: true
Explanation: Take course 0, then course 1

Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
Output: false
Explanation: Circular dependency
```

### Solution

```java
public class CourseSchedule {
    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }
        
        for (int[] prereq : prerequisites) {
            graph.get(prereq[0]).add(prereq[1]);
        }
        
        // 0 = unvisited, 1 = visiting, 2 = visited
        int[] state = new int[numCourses];
        
        for (int i = 0; i < numCourses; i++) {
            if (hasCycle(graph, i, state)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean hasCycle(List<List<Integer>> graph, int course, int[] state) {
        if (state[course] == 1) return true;  // Cycle detected
        if (state[course] == 2) return false; // Already processed
        
        state[course] = 1; // Mark as visiting
        
        for (int prereq : graph.get(course)) {
            if (hasCycle(graph, prereq, state)) {
                return true;
            }
        }
        
        state[course] = 2; // Mark as visited
        return false;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V + E)

### Test Cases

```java
@Test
public void testCanFinish() {
    CourseSchedule solution = new CourseSchedule();
    
    assertTrue(solution.canFinish(2, new int[][]{{1,0}}));
    assertFalse(solution.canFinish(2, new int[][]{{1,0},{0,1}}));
}
```

---

## Problem 5: Pacific Atlantic Water Flow

**Difficulty**: Medium  
**Pattern**: DFS (Multi-source)  
**LeetCode**: #417

### Problem Statement

Given an `m x n` matrix of heights representing islands, where water can flow to adjacent cells with equal or lower height, find all cells where water can flow to both Pacific (top/left) and Atlantic (bottom/right) oceans.

### Solution

```java
public class PacificAtlantic {
    public List<List<Integer>> pacificAtlantic(int[][] heights) {
        List<List<Integer>> result = new ArrayList<>();
        if (heights == null || heights.length == 0) return result;
        
        int m = heights.length;
        int n = heights[0].length;
        
        boolean[][] pacific = new boolean[m][n];
        boolean[][] atlantic = new boolean[m][n];
        
        // DFS from Pacific borders
        for (int i = 0; i < m; i++) {
            dfs(heights, i, 0, pacific, Integer.MIN_VALUE);
        }
        for (int j = 0; j < n; j++) {
            dfs(heights, 0, j, pacific, Integer.MIN_VALUE);
        }
        
        // DFS from Atlantic borders
        for (int i = 0; i < m; i++) {
            dfs(heights, i, n - 1, atlantic, Integer.MIN_VALUE);
        }
        for (int j = 0; j < n; j++) {
            dfs(heights, m - 1, j, atlantic, Integer.MIN_VALUE);
        }
        
        // Find cells reachable by both
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (pacific[i][j] && atlantic[i][j]) {
                    result.add(Arrays.asList(i, j));
                }
            }
        }
        
        return result;
    }
    
    private void dfs(int[][] heights, int i, int j, boolean[][] visited, int prevHeight) {
        int m = heights.length;
        int n = heights[0].length;
        
        if (i < 0 || i >= m || j < 0 || j >= n || visited[i][j] || 
            heights[i][j] < prevHeight) {
            return;
        }
        
        visited[i][j] = true;
        
        dfs(heights, i + 1, j, visited, heights[i][j]);
        dfs(heights, i - 1, j, visited, heights[i][j]);
        dfs(heights, i, j + 1, visited, heights[i][j]);
        dfs(heights, i, j - 1, visited, heights[i][j]);
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(m × n)
- **Space Complexity**: O(m × n)

---

## Problem 6: Word Search

**Difficulty**: Medium  
**Pattern**: DFS (Backtracking)  
**LeetCode**: #79

### Problem Statement

Given an `m x n` grid of characters and a string `word`, return `true` if `word` exists in the grid. The word can be constructed from letters of sequentially adjacent cells (horizontally or vertically). The same cell cannot be used more than once.

### Solution

```java
public class WordSearch {
    public boolean exist(char[][] board, String word) {
        int m = board.length;
        int n = board[0].length;
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (dfs(board, word, i, j, 0)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean dfs(char[][] board, String word, int i, int j, int index) {
        if (index == word.length()) {
            return true;
        }
        
        int m = board.length;
        int n = board[0].length;
        
        if (i < 0 || i >= m || j < 0 || j >= n || 
            board[i][j] != word.charAt(index)) {
            return false;
        }
        
        char temp = board[i][j];
        board[i][j] = '#'; // Mark as visited
        
        boolean found = dfs(board, word, i + 1, j, index + 1) ||
                       dfs(board, word, i - 1, j, index + 1) ||
                       dfs(board, word, i, j + 1, index + 1) ||
                       dfs(board, word, i, j - 1, index + 1);
        
        board[i][j] = temp; // Backtrack
        
        return found;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(m × n × 4^L) where L is word length
- **Space Complexity**: O(L) for recursion

---

## Problem 7: Surrounded Regions

**Difficulty**: Medium  
**Pattern**: DFS  
**LeetCode**: #130

### Problem Statement

Given an `m x n` matrix filled with 'X' and 'O', capture all regions that are surrounded by 'X'. A region is captured by flipping all 'O's into 'X's.

### Solution

```java
public class SurroundedRegions {
    public void solve(char[][] board) {
        if (board == null || board.length == 0) return;
        
        int m = board.length;
        int n = board[0].length;
        
        // Mark border-connected 'O's
        for (int i = 0; i < m; i++) {
            if (board[i][0] == 'O') dfs(board, i, 0);
            if (board[i][n - 1] == 'O') dfs(board, i, n - 1);
        }
        
        for (int j = 0; j < n; j++) {
            if (board[0][j] == 'O') dfs(board, 0, j);
            if (board[m - 1][j] == 'O') dfs(board, m - 1, j);
        }
        
        // Flip remaining 'O's to 'X' and restore marked ones
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == 'O') {
                    board[i][j] = 'X';
                } else if (board[i][j] == '#') {
                    board[i][j] = 'O';
                }
            }
        }
    }
    
    private void dfs(char[][] board, int i, int j) {
        int m = board.length;
        int n = board[0].length;
        
        if (i < 0 || i >= m || j < 0 || j >= n || board[i][j] != 'O') {
            return;
        }
        
        board[i][j] = '#'; // Mark as border-connected
        
        dfs(board, i + 1, j);
        dfs(board, i - 1, j);
        dfs(board, i, j + 1);
        dfs(board, i, j - 1);
    }
}
```

---

## Problem 8: All Paths From Source to Target

**Difficulty**: Medium  
**Pattern**: DFS (Backtracking)  
**LeetCode**: #797

### Problem Statement

Given a directed acyclic graph (DAG) of `n` nodes labeled from `0` to `n - 1`, find all possible paths from node `0` to node `n - 1`.

### Solution

```java
public class AllPathsSourceTarget {
    public List<List<Integer>> allPathsSourceTarget(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        path.add(0);
        
        dfs(graph, 0, path, result);
        
        return result;
    }
    
    private void dfs(int[][] graph, int node, List<Integer> path, 
                     List<List<Integer>> result) {
        if (node == graph.length - 1) {
            result.add(new ArrayList<>(path));
            return;
        }
        
        for (int neighbor : graph[node]) {
            path.add(neighbor);
            dfs(graph, neighbor, path, result);
            path.remove(path.size() - 1); // Backtrack
        }
    }
}
```

---

## Problem 9: Rotting Oranges

**Difficulty**: Medium  
**Pattern**: BFS (Multi-source)  
**LeetCode**: #994

### Problem Statement

Given an `m x n` grid where each cell can be empty (0), fresh orange (1), or rotten orange (2). Every minute, fresh oranges adjacent to rotten ones become rotten. Return the minimum minutes until no fresh oranges remain, or -1 if impossible.

### Solution

```java
public class RottingOranges {
    public int orangesRotting(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();
        int fresh = 0;
        
        // Add all rotten oranges to queue
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 2) {
                    queue.offer(new int[]{i, j});
                } else if (grid[i][j] == 1) {
                    fresh++;
                }
            }
        }
        
        if (fresh == 0) return 0;
        
        int minutes = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            
            for (int i = 0; i < size; i++) {
                int[] pos = queue.poll();
                
                for (int[] dir : dirs) {
                    int ni = pos[0] + dir[0];
                    int nj = pos[1] + dir[1];
                    
                    if (ni >= 0 && ni < m && nj >= 0 && nj < n && 
                        grid[ni][nj] == 1) {
                        grid[ni][nj] = 2;
                        fresh--;
                        queue.offer(new int[]{ni, nj});
                    }
                }
            }
            
            minutes++;
        }
        
        return fresh == 0 ? minutes - 1 : -1;
    }
}
```

---

## Problem 10: Shortest Path in Binary Matrix

**Difficulty**: Medium  
**Pattern**: BFS  
**LeetCode**: #1091

### Problem Statement

Given an `n x n` binary matrix, return the length of the shortest clear path from top-left to bottom-right. A clear path has all visited cells with value 0. You can move in 8 directions.

### Solution

```java
public class ShortestPathBinaryMatrix {
    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;
        
        if (grid[0][0] == 1 || grid[n-1][n-1] == 1) {
            return -1;
        }
        
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{0, 0, 1}); // row, col, distance
        grid[0][0] = 1; // Mark as visited
        
        int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1], dist = curr[2];
            
            if (r == n - 1 && c == n - 1) {
                return dist;
            }
            
            for (int[] dir : dirs) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                
                if (nr >= 0 && nr < n && nc >= 0 && nc < n && grid[nr][nc] == 0) {
                    grid[nr][nc] = 1;
                    queue.offer(new int[]{nr, nc, dist + 1});
                }
            }
        }
        
        return -1;
    }
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Right Side View | BFS | O(n) | O(w) | Last node per level |
| Number of Islands | DFS | O(m×n) | O(m×n) | Connected components |
| Clone Graph | DFS | O(V+E) | O(V) | Deep copy with map |
| Course Schedule | DFS | O(V+E) | O(V) | Cycle detection |
| Pacific Atlantic | DFS | O(m×n) | O(m×n) | Multi-source DFS |
| Word Search | DFS | O(m×n×4^L) | O(L) | Backtracking |
| Surrounded Regions | DFS | O(m×n) | O(m×n) | Border marking |
| All Paths | DFS | O(2^V×V) | O(V) | Backtracking |
| Rotting Oranges | BFS | O(m×n) | O(m×n) | Multi-source BFS |
| Shortest Path Matrix | BFS | O(n²) | O(n²) | 8-directional BFS |

---

**Next**: [Hard Problems](05_Hard_Problems.md)
