# BFS/DFS - Easy Problems (40%)

## 📚 8 Easy Problems with Complete Solutions

---

## Problem 1: Binary Tree Level Order Traversal

**Difficulty**: Easy  
**Pattern**: BFS  
**LeetCode**: #102

### Problem Statement

Given the root of a binary tree, return the level order traversal of its nodes' values (i.e., from left to right, level by level).

### Examples

```
Input: root = [3,9,20,null,null,15,7]
      3
     / \
    9  20
      /  \
     15   7
Output: [[3],[9,20],[15,7]]

Input: root = [1]
Output: [[1]]

Input: root = []
Output: []
```

### Solution

```java
public class BinaryTreeLevelOrder {
    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> currentLevel = new ArrayList<>();
            
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node.val);
                
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            
            result.add(currentLevel);
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `root = [3,9,20,null,null,15,7]`

```
Initial: queue = [3], result = []

Level 0:
  levelSize = 1
  Process node 3: currentLevel = [3]
  Add children: queue = [9, 20]
  result = [[3]]

Level 1:
  levelSize = 2
  Process node 9: currentLevel = [9]
  No children
  Process node 20: currentLevel = [9, 20]
  Add children: queue = [15, 7]
  result = [[3], [9, 20]]

Level 2:
  levelSize = 2
  Process node 15: currentLevel = [15]
  Process node 7: currentLevel = [15, 7]
  result = [[3], [9, 20], [15, 7]]

Final result = [[3], [9, 20], [15, 7]]
```

### Complexity Analysis

- **Time Complexity**: O(n) - Visit each node once
- **Space Complexity**: O(w) - w is maximum width of tree

### Test Cases

```java
@Test
public void testLevelOrder() {
    BinaryTreeLevelOrder solution = new BinaryTreeLevelOrder();
    
    TreeNode root1 = new TreeNode(3);
    root1.left = new TreeNode(9);
    root1.right = new TreeNode(20);
    root1.right.left = new TreeNode(15);
    root1.right.right = new TreeNode(7);
    
    List<List<Integer>> expected = Arrays.asList(
        Arrays.asList(3),
        Arrays.asList(9, 20),
        Arrays.asList(15, 7)
    );
    assertEquals(expected, solution.levelOrder(root1));
    
    assertEquals(new ArrayList<>(), solution.levelOrder(null));
}
```

---

## Problem 2: Maximum Depth of Binary Tree

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #104

### Problem Statement

Given the root of a binary tree, return its maximum depth. Maximum depth is the number of nodes along the longest path from the root to the farthest leaf node.

### Examples

```
Input: root = [3,9,20,null,null,15,7]
Output: 3

Input: root = [1,null,2]
Output: 2
```

### Solution

```java
public class MaximumDepth {
    // DFS Recursive
    public int maxDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        
        int leftDepth = maxDepth(root.left);
        int rightDepth = maxDepth(root.right);
        
        return Math.max(leftDepth, rightDepth) + 1;
    }
    
    // BFS Alternative
    public int maxDepthBFS(TreeNode root) {
        if (root == null) return 0;
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int depth = 0;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            
            depth++;
        }
        
        return depth;
    }
}
```

### Dry Run (DFS)

**Input**: `root = [3,9,20,null,null,15,7]`

```
maxDepth(3):
  maxDepth(9):
    maxDepth(null) = 0
    maxDepth(null) = 0
    return max(0, 0) + 1 = 1
  
  maxDepth(20):
    maxDepth(15):
      maxDepth(null) = 0
      maxDepth(null) = 0
      return max(0, 0) + 1 = 1
    
    maxDepth(7):
      maxDepth(null) = 0
      maxDepth(null) = 0
      return max(0, 0) + 1 = 1
    
    return max(1, 1) + 1 = 2
  
  return max(1, 2) + 1 = 3

Final result = 3
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(h) - h is height (recursion stack)

### Test Cases

```java
@Test
public void testMaxDepth() {
    MaximumDepth solution = new MaximumDepth();
    
    TreeNode root = new TreeNode(3);
    root.left = new TreeNode(9);
    root.right = new TreeNode(20);
    root.right.left = new TreeNode(15);
    root.right.right = new TreeNode(7);
    
    assertEquals(3, solution.maxDepth(root));
    assertEquals(0, solution.maxDepth(null));
}
```

---

## Problem 3: Same Tree

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #100

### Problem Statement

Given the roots of two binary trees `p` and `q`, check if they are the same (structurally identical and nodes have same values).

### Solution

```java
public class SameTree {
    public boolean isSameTree(TreeNode p, TreeNode q) {
        // Both null
        if (p == null && q == null) {
            return true;
        }
        
        // One null, other not
        if (p == null || q == null) {
            return false;
        }
        
        // Values different
        if (p.val != q.val) {
            return false;
        }
        
        // Check left and right subtrees
        return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }
}
```

### Test Cases

```java
@Test
public void testIsSameTree() {
    SameTree solution = new SameTree();
    
    TreeNode p = new TreeNode(1);
    p.left = new TreeNode(2);
    p.right = new TreeNode(3);
    
    TreeNode q = new TreeNode(1);
    q.left = new TreeNode(2);
    q.right = new TreeNode(3);
    
    assertTrue(solution.isSameTree(p, q));
    assertFalse(solution.isSameTree(p, null));
}
```

---

## Problem 4: Invert Binary Tree

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #226

### Problem Statement

Given the root of a binary tree, invert the tree and return its root.

### Examples

```
Input:       4
           /   \
          2     7
         / \   / \
        1   3 6   9

Output:      4
           /   \
          7     2
         / \   / \
        9   6 3   1
```

### Solution

```java
public class InvertBinaryTree {
    public TreeNode invertTree(TreeNode root) {
        if (root == null) {
            return null;
        }
        
        // Swap left and right
        TreeNode temp = root.left;
        root.left = root.right;
        root.right = temp;
        
        // Recursively invert subtrees
        invertTree(root.left);
        invertTree(root.right);
        
        return root;
    }
}
```

### Test Cases

```java
@Test
public void testInvertTree() {
    InvertBinaryTree solution = new InvertBinaryTree();
    
    TreeNode root = new TreeNode(4);
    root.left = new TreeNode(2);
    root.right = new TreeNode(7);
    root.left.left = new TreeNode(1);
    root.left.right = new TreeNode(3);
    
    TreeNode inverted = solution.invertTree(root);
    assertEquals(7, inverted.left.val);
    assertEquals(2, inverted.right.val);
}
```

---

## Problem 5: Symmetric Tree

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #101

### Problem Statement

Given the root of a binary tree, check whether it is a mirror of itself (symmetric around its center).

### Solution

```java
public class SymmetricTree {
    public boolean isSymmetric(TreeNode root) {
        if (root == null) return true;
        return isMirror(root.left, root.right);
    }
    
    private boolean isMirror(TreeNode left, TreeNode right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        
        return (left.val == right.val) &&
               isMirror(left.left, right.right) &&
               isMirror(left.right, right.left);
    }
}
```

### Test Cases

```java
@Test
public void testIsSymmetric() {
    SymmetricTree solution = new SymmetricTree();
    
    TreeNode root = new TreeNode(1);
    root.left = new TreeNode(2);
    root.right = new TreeNode(2);
    root.left.left = new TreeNode(3);
    root.right.right = new TreeNode(3);
    
    assertTrue(solution.isSymmetric(root));
}
```

---

## Problem 6: Path Sum

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #112

### Problem Statement

Given the root of a binary tree and an integer `targetSum`, return `true` if the tree has a root-to-leaf path such that adding up all values equals `targetSum`.

### Solution

```java
public class PathSum {
    public boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }
        
        // Leaf node
        if (root.left == null && root.right == null) {
            return root.val == targetSum;
        }
        
        int remaining = targetSum - root.val;
        return hasPathSum(root.left, remaining) || 
               hasPathSum(root.right, remaining);
    }
}
```

### Test Cases

```java
@Test
public void testHasPathSum() {
    PathSum solution = new PathSum();
    
    TreeNode root = new TreeNode(5);
    root.left = new TreeNode(4);
    root.right = new TreeNode(8);
    root.left.left = new TreeNode(11);
    root.left.left.left = new TreeNode(7);
    root.left.left.right = new TreeNode(2);
    
    assertTrue(solution.hasPathSum(root, 22));
    assertFalse(solution.hasPathSum(root, 100));
}
```

---

## Problem 7: Merge Two Binary Trees

**Difficulty**: Easy  
**Pattern**: DFS (Recursive)  
**LeetCode**: #617

### Problem Statement

Given two binary trees, merge them by overlapping nodes. If two nodes overlap, sum their values. Otherwise, use the non-null node.

### Solution

```java
public class MergeTrees {
    public TreeNode mergeTrees(TreeNode t1, TreeNode t2) {
        if (t1 == null) return t2;
        if (t2 == null) return t1;
        
        TreeNode merged = new TreeNode(t1.val + t2.val);
        merged.left = mergeTrees(t1.left, t2.left);
        merged.right = mergeTrees(t1.right, t2.right);
        
        return merged;
    }
}
```

### Test Cases

```java
@Test
public void testMergeTrees() {
    MergeTrees solution = new MergeTrees();
    
    TreeNode t1 = new TreeNode(1);
    t1.left = new TreeNode(3);
    t1.right = new TreeNode(2);
    
    TreeNode t2 = new TreeNode(2);
    t2.left = new TreeNode(1);
    t2.right = new TreeNode(3);
    
    TreeNode merged = solution.mergeTrees(t1, t2);
    assertEquals(3, merged.val);
    assertEquals(4, merged.left.val);
}
```

---

## Problem 8: Flood Fill

**Difficulty**: Easy  
**Pattern**: DFS (Matrix)  
**LeetCode**: #733

### Problem Statement

Given an image represented by a 2D array, a starting pixel `(sr, sc)`, and a color, perform a flood fill. Change the color of the starting pixel and all connected pixels of the same color to the new color.

### Examples

```
Input: image = [[1,1,1],[1,1,0],[1,0,1]], sr = 1, sc = 1, color = 2
Output: [[2,2,2],[2,2,0],[2,0,1]]
```

### Solution

```java
public class FloodFill {
    public int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int originalColor = image[sr][sc];
        
        // Already the target color
        if (originalColor == color) {
            return image;
        }
        
        dfs(image, sr, sc, originalColor, color);
        return image;
    }
    
    private void dfs(int[][] image, int r, int c, int originalColor, int newColor) {
        // Boundary check
        if (r < 0 || r >= image.length || c < 0 || c >= image[0].length) {
            return;
        }
        
        // Not the original color
        if (image[r][c] != originalColor) {
            return;
        }
        
        // Change color
        image[r][c] = newColor;
        
        // Explore 4 directions
        dfs(image, r + 1, c, originalColor, newColor);
        dfs(image, r - 1, c, originalColor, newColor);
        dfs(image, r, c + 1, originalColor, newColor);
        dfs(image, r, c - 1, originalColor, newColor);
    }
}
```

### Dry Run

**Input**: `image = [[1,1,1],[1,1,0],[1,0,1]]`, `sr = 1`, `sc = 1`, `color = 2`

```
Original:
1 1 1
1 1 0
1 0 1

Start at (1,1), originalColor = 1

dfs(1, 1):
  image[1][1] = 2
  2 1 1
  1 2 0
  1 0 1
  
  dfs(2, 1): image[2][1] = 0, return
  dfs(0, 1): image[0][1] = 1
    image[0][1] = 2
    2 2 1
    1 2 0
    1 0 1
    
    dfs(1, 1): already 2, return
    dfs(-1, 1): out of bounds
    dfs(0, 2): image[0][2] = 1
      image[0][2] = 2
      2 2 2
      1 2 0
      1 0 1
      
      ... continue
  
  dfs(1, 2): image[1][2] = 0, return
  dfs(1, 0): image[1][0] = 1
    image[1][0] = 2
    2 2 2
    2 2 0
    1 0 1
    
    dfs(2, 0): image[2][0] = 1
      image[2][0] = 2
      2 2 2
      2 2 0
      2 0 1

Final:
2 2 2
2 2 0
2 0 1
```

### Complexity Analysis

- **Time Complexity**: O(m × n) - Visit each cell once
- **Space Complexity**: O(m × n) - Recursion stack in worst case

### Test Cases

```java
@Test
public void testFloodFill() {
    FloodFill solution = new FloodFill();
    
    int[][] image = {{1,1,1},{1,1,0},{1,0,1}};
    int[][] expected = {{2,2,2},{2,2,0},{2,0,1}};
    
    assertArrayEquals(expected, solution.floodFill(image, 1, 1, 2));
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Level Order Traversal | BFS | O(n) | O(w) | Queue, level by level |
| Maximum Depth | DFS | O(n) | O(h) | Recursive depth |
| Same Tree | DFS | O(n) | O(h) | Compare recursively |
| Invert Tree | DFS | O(n) | O(h) | Swap children |
| Symmetric Tree | DFS | O(n) | O(h) | Mirror comparison |
| Path Sum | DFS | O(n) | O(h) | Root to leaf |
| Merge Trees | DFS | O(n) | O(h) | Combine nodes |
| Flood Fill | DFS | O(m×n) | O(m×n) | Matrix traversal |

---

**Next**: [Medium Problems](04_Medium_Problems.md)
