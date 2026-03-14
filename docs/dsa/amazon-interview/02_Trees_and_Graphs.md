# Amazon Interview - Trees & Graphs Problems

## Problem 9: Binary Tree Level Order Traversal (LeetCode 102) ⭐⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Very High  
**Pattern**: BFS

### Problem Statement
Given the root of a binary tree, return the level order traversal of its nodes' values.

### Solution
```java
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    
    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();
        
        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            level.add(node.val);
            
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        
        result.add(level);
    }
    
    return result;
}
```
**Time**: O(n), **Space**: O(n)

---

## Problem 10: Lowest Common Ancestor (LeetCode 236) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: DFS

### Solution
```java
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) {
        return root;
    }
    
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    
    if (left != null && right != null) return root;
    return left != null ? left : right;
}
```
**Time**: O(n), **Space**: O(h)

---

## Problem 11: Number of Islands (LeetCode 200) ⭐⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Very High  
**Pattern**: DFS/BFS

### Solution
```java
public int numIslands(char[][] grid) {
    if (grid == null || grid.length == 0) return 0;
    
    int count = 0;
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] == '1') {
                count++;
                dfs(grid, i, j);
            }
        }
    }
    return count;
}

private void dfs(char[][] grid, int i, int j) {
    if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length || grid[i][j] == '0') {
        return;
    }
    
    grid[i][j] = '0';
    dfs(grid, i + 1, j);
    dfs(grid, i - 1, j);
    dfs(grid, i, j + 1);
    dfs(grid, i, j - 1);
}
```
**Time**: O(m*n), **Space**: O(m*n)

---

## Problem 12: Word Ladder (LeetCode 127) ⭐⭐⭐⭐

**Difficulty**: Hard  
**Frequency**: High  
**Pattern**: BFS

### Solution
```java
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;
    
    Queue<String> queue = new LinkedList<>();
    queue.offer(beginWord);
    int level = 1;
    
    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            String word = queue.poll();
            if (word.equals(endWord)) return level;
            
            char[] chars = word.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char original = chars[j];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == original) continue;
                    chars[j] = c;
                    String newWord = new String(chars);
                    if (wordSet.contains(newWord)) {
                        queue.offer(newWord);
                        wordSet.remove(newWord);
                    }
                }
                chars[j] = original;
            }
        }
        level++;
    }
    return 0;
}
```
**Time**: O(n * m² * 26), **Space**: O(n)

---

## Problem 13: Course Schedule (LeetCode 207) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: Topological Sort

### Solution
```java
public boolean canFinish(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    int[] indegree = new int[numCourses];
    
    for (int i = 0; i < numCourses; i++) {
        graph.add(new ArrayList<>());
    }
    
    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);
        indegree[pre[0]]++;
    }
    
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }
    
    int count = 0;
    while (!queue.isEmpty()) {
        int course = queue.poll();
        count++;
        for (int next : graph.get(course)) {
            indegree[next]--;
            if (indegree[next] == 0) queue.offer(next);
        }
    }
    
    return count == numCourses;
}
```
**Time**: O(V + E), **Space**: O(V + E)

---

## Problem 14: Serialize and Deserialize Binary Tree (LeetCode 297) ⭐⭐⭐⭐

**Difficulty**: Hard  
**Pattern**: DFS

### Solution
```java
public class Codec {
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null,");
            return;
        }
        sb.append(node.val).append(",");
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }
    
    public TreeNode deserialize(String data) {
        Queue<String> queue = new LinkedList<>(Arrays.asList(data.split(",")));
        return deserializeHelper(queue);
    }
    
    private TreeNode deserializeHelper(Queue<String> queue) {
        String val = queue.poll();
        if (val.equals("null")) return null;
        
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper(queue);
        node.right = deserializeHelper(queue);
        return node;
    }
}
```
**Time**: O(n), **Space**: O(n)

---

## Problem 15: Binary Tree Maximum Path Sum (LeetCode 124) ⭐⭐⭐⭐

**Difficulty**: Hard  
**Pattern**: DFS

### Solution
```java
private int maxSum = Integer.MIN_VALUE;

public int maxPathSum(TreeNode root) {
    maxGain(root);
    return maxSum;
}

private int maxGain(TreeNode node) {
    if (node == null) return 0;
    
    int leftGain = Math.max(maxGain(node.left), 0);
    int rightGain = Math.max(maxGain(node.right), 0);
    
    int currentPathSum = node.val + leftGain + rightGain;
    maxSum = Math.max(maxSum, currentPathSum);
    
    return node.val + Math.max(leftGain, rightGain);
}
```
**Time**: O(n), **Space**: O(h)

---

**Next**: [Dynamic Programming Problems](03_Dynamic_Programming.md)
