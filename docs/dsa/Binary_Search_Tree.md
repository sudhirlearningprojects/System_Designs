# Binary Search Tree (BST) - Complete Interview Guide

## Table of Contents
1. [Introduction](#introduction)
2. [BST Properties](#bst-properties)
3. [Basic Operations](#basic-operations)
4. [Tree Traversals](#tree-traversals)
5. [Advanced Operations](#advanced-operations)
6. [Interview Problems](#interview-problems)
7. [Time & Space Complexity](#time--space-complexity)
8. [Real-World Use Cases](#real-world-use-cases)

---

## Introduction

A **Binary Search Tree (BST)** is a binary tree where each node follows the ordering property:
- **Left subtree** contains only nodes with values **less than** the parent
- **Right subtree** contains only nodes with values **greater than** the parent
- Both left and right subtrees are also BSTs (recursive property)

### Visual Example
```
        8
       / \
      3   10
     / \    \
    1   6   14
       / \  /
      4  7 13

BST Property:
- All nodes in left subtree of 8: {1, 3, 4, 6, 7} < 8
- All nodes in right subtree of 8: {10, 13, 14} > 8
```

### Why BST?
- **Efficient Search**: O(log n) average case
- **Ordered Data**: In-order traversal gives sorted sequence
- **Dynamic**: Easy insertion and deletion
- **Range Queries**: Find elements in a range efficiently

---

## BST Properties

### 1. Ordering Property
```
For every node N:
  - All values in left subtree < N.value
  - All values in right subtree > N.value
```

### 2. In-order Traversal = Sorted Order
```
BST:        5
           / \
          3   7
         / \   \
        2   4   8

In-order: [2, 3, 4, 5, 7, 8] ✓ Sorted!
```

### 3. No Duplicate Values (Standard)
- Most BST implementations don't allow duplicates
- Variations: Store count or allow duplicates in left/right

### 4. Height Matters
```
Balanced BST:          Skewed BST:
      4                    1
     / \                    \
    2   6                    2
   / \ / \                    \
  1  3 5  7                    3
                                \
Height: log n                    4
Search: O(log n)            Height: n
                            Search: O(n)
```

---

## Basic Operations

### 1. Search

**Algorithm**:
1. Start at root
2. If target == current, found!
3. If target < current, go left
4. If target > current, go right
5. Repeat until found or null

**Implementation**:
```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}

// Recursive
public TreeNode search(TreeNode root, int target) {
    if (root == null || root.val == target) return root;
    
    if (target < root.val)
        return search(root.left, target);
    else
        return search(root.right, target);
}

// Iterative (preferred for interviews)
public TreeNode searchIterative(TreeNode root, int target) {
    while (root != null && root.val != target) {
        root = (target < root.val) ? root.left : root.right;
    }
    return root;
}
```

**Example**:
```
Search 6 in:
        8
       / \
      3   10
     / \    \
    1   6   14

Step 1: 6 < 8, go left → 3
Step 2: 6 > 3, go right → 6
Step 3: Found! ✓
```

**Time**: O(h) where h = height  
**Space**: O(1) iterative, O(h) recursive

---

### 2. Insert

**Algorithm**:
1. Start at root
2. If tree empty, create root
3. Compare with current node
4. Go left if smaller, right if larger
5. Insert at first null position

**Implementation**:
```java
// Recursive
public TreeNode insert(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    
    if (val < root.val)
        root.left = insert(root.left, val);
    else if (val > root.val)
        root.right = insert(root.right, val);
    
    return root;
}

// Iterative
public TreeNode insertIterative(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    
    TreeNode current = root, parent = null;
    
    while (current != null) {
        parent = current;
        if (val < current.val)
            current = current.left;
        else
            current = current.right;
    }
    
    if (val < parent.val)
        parent.left = new TreeNode(val);
    else
        parent.right = new TreeNode(val);
    
    return root;
}
```

**Example**:
```
Insert 5 into:
        8
       / \
      3   10

Step 1: 5 < 8, go left → 3
Step 2: 5 > 3, go right → null
Step 3: Insert 5 as right child of 3

Result:
        8
       / \
      3   10
       \
        5
```

**Time**: O(h)  
**Space**: O(1) iterative, O(h) recursive

---

### 3. Delete

**Three Cases**:

**Case 1: Node is Leaf (No Children)**
```
Delete 1:
    3           3
   / \    →      \
  1   4           4
```
Simply remove the node.

**Case 2: Node has One Child**
```
Delete 3:
    5           5
   /      →      \
  3               4
   \
    4
```
Replace node with its child.

**Case 3: Node has Two Children**
```
Delete 3:
    5           5
   / \    →    / \
  3   7       4   7
 / \           \
1   4           1

Steps:
1. Find in-order successor (smallest in right subtree) = 4
2. Replace 3's value with 4
3. Delete original 4 (becomes Case 1 or 2)
```

**Implementation**:
```java
public TreeNode delete(TreeNode root, int key) {
    if (root == null) return null;
    
    // Find the node
    if (key < root.val) {
        root.left = delete(root.left, key);
    } else if (key > root.val) {
        root.right = delete(root.right, key);
    } else {
        // Node found - handle 3 cases
        
        // Case 1 & 2: 0 or 1 child
        if (root.left == null) return root.right;
        if (root.right == null) return root.left;
        
        // Case 3: 2 children
        // Find in-order successor (min in right subtree)
        TreeNode successor = findMin(root.right);
        root.val = successor.val;
        root.right = delete(root.right, successor.val);
    }
    
    return root;
}

private TreeNode findMin(TreeNode node) {
    while (node.left != null) node = node.left;
    return node;
}
```

**Time**: O(h)  
**Space**: O(h) for recursion

---

### 4. Find Minimum

**Algorithm**: Keep going left until null

```java
public TreeNode findMin(TreeNode root) {
    if (root == null) return null;
    while (root.left != null) root = root.left;
    return root;
}
```

**Example**:
```
        8
       / \
      3   10
     / \
    1   6

Minimum: 1 (leftmost node)
```

**Time**: O(h)

---

### 5. Find Maximum

**Algorithm**: Keep going right until null

```java
public TreeNode findMax(TreeNode root) {
    if (root == null) return null;
    while (root.right != null) root = root.right;
    return root;
}
```

**Time**: O(h)

---

## Tree Traversals

### 1. In-Order (Left → Root → Right)

**Result**: Sorted order for BST ✓

```java
public void inOrder(TreeNode root) {
    if (root == null) return;
    inOrder(root.left);
    System.out.print(root.val + " ");
    inOrder(root.right);
}
```

**Example**:
```
        4
       / \
      2   6
     / \ / \
    1  3 5  7

In-order: 1 2 3 4 5 6 7 (sorted!)
```

---

### 2. Pre-Order (Root → Left → Right)

**Use**: Create copy of tree, prefix expression

```java
public void preOrder(TreeNode root) {
    if (root == null) return;
    System.out.print(root.val + " ");
    preOrder(root.left);
    preOrder(root.right);
}
```

**Example**: `4 2 1 3 6 5 7`

---

### 3. Post-Order (Left → Right → Root)

**Use**: Delete tree, postfix expression

```java
public void postOrder(TreeNode root) {
    if (root == null) return;
    postOrder(root.left);
    postOrder(root.right);
    System.out.print(root.val + " ");
}
```

**Example**: `1 3 2 5 7 6 4`

---

### 4. Level-Order (BFS)

**Use**: Level-by-level processing

```java
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    
    while (!queue.isEmpty()) {
        int size = queue.size();
        List<Integer> level = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
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

**Example**: `[[4], [2, 6], [1, 3, 5, 7]]`

---

## Advanced Operations

### 1. Validate BST

**Problem**: Check if a binary tree is a valid BST

**Wrong Approach**:
```java
// ❌ Only checks immediate children
public boolean isValidBST(TreeNode root) {
    if (root == null) return true;
    if (root.left != null && root.left.val >= root.val) return false;
    if (root.right != null && root.right.val <= root.val) return false;
    return isValidBST(root.left) && isValidBST(root.right);
}

// Fails for:
    5
   / \
  1   6
     / \
    4   7
// 4 < 5 but in right subtree!
```

**Correct Approach**: Use range validation

```java
public boolean isValidBST(TreeNode root) {
    return validate(root, null, null);
}

private boolean validate(TreeNode node, Integer min, Integer max) {
    if (node == null) return true;
    
    // Check range
    if (min != null && node.val <= min) return false;
    if (max != null && node.val >= max) return false;
    
    // Recursively validate subtrees with updated ranges
    return validate(node.left, min, node.val) &&
           validate(node.right, node.val, max);
}
```

**Time**: O(n), **Space**: O(h)

---

### 2. Lowest Common Ancestor (LCA)

**Problem**: Find LCA of two nodes in BST

**Key Insight**: Use BST property!

```java
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    // Both in left subtree
    if (p.val < root.val && q.val < root.val)
        return lowestCommonAncestor(root.left, p, q);
    
    // Both in right subtree
    if (p.val > root.val && q.val > root.val)
        return lowestCommonAncestor(root.right, p, q);
    
    // Split point - this is LCA
    return root;
}
```

**Example**:
```
        6
       / \
      2   8
     / \ / \
    0  4 7  9
      / \
     3   5

LCA(2, 8) = 6 (split point)
LCA(2, 4) = 2 (one is ancestor of other)
```

**Time**: O(h), **Space**: O(1) iterative

---

### 3. Kth Smallest Element

**Approach 1: In-order Traversal**

```java
public int kthSmallest(TreeNode root, int k) {
    List<Integer> sorted = new ArrayList<>();
    inOrder(root, sorted);
    return sorted.get(k - 1);
}

private void inOrder(TreeNode node, List<Integer> list) {
    if (node == null) return;
    inOrder(node.left, list);
    list.add(node.val);
    inOrder(node.right, list);
}
```

**Approach 2: Optimized (Stop at k)**

```java
private int count = 0;
private int result = 0;

public int kthSmallest(TreeNode root, int k) {
    inOrder(root, k);
    return result;
}

private void inOrder(TreeNode node, int k) {
    if (node == null) return;
    
    inOrder(node.left, k);
    
    count++;
    if (count == k) {
        result = node.val;
        return;
    }
    
    inOrder(node.right, k);
}
```

**Time**: O(k), **Space**: O(h)

---

### 4. Convert Sorted Array to BST

**Problem**: Build balanced BST from sorted array

**Key**: Use middle element as root

```java
public TreeNode sortedArrayToBST(int[] nums) {
    return build(nums, 0, nums.length - 1);
}

private TreeNode build(int[] nums, int left, int right) {
    if (left > right) return null;
    
    int mid = left + (right - left) / 2;
    TreeNode root = new TreeNode(nums[mid]);
    
    root.left = build(nums, left, mid - 1);
    root.right = build(nums, mid + 1, right);
    
    return root;
}
```

**Example**:
```
Array: [1, 2, 3, 4, 5, 6, 7]

Step 1: mid = 4 (root)
        4
       / \
      ?   ?

Step 2: Left [1,2,3], mid=2
        4
       / \
      2   ?
     / \
    ?   ?

Step 3: Continue recursively
        4
       / \
      2   6
     / \ / \
    1  3 5  7
```

**Time**: O(n), **Space**: O(log n)

---

### 5. Range Sum Query

**Problem**: Sum of all values in range [L, R]

```java
public int rangeSumBST(TreeNode root, int L, int R) {
    if (root == null) return 0;
    
    // Prune branches
    if (root.val < L) return rangeSumBST(root.right, L, R);
    if (root.val > R) return rangeSumBST(root.left, L, R);
    
    // In range - include current and both subtrees
    return root.val + 
           rangeSumBST(root.left, L, R) + 
           rangeSumBST(root.right, L, R);
}
```

**Time**: O(n) worst case, O(h + k) average (k = nodes in range)

---

## Interview Problems

### Problem 1: Invert Binary Tree

```java
public TreeNode invertTree(TreeNode root) {
    if (root == null) return null;
    
    TreeNode temp = root.left;
    root.left = invertTree(root.right);
    root.right = invertTree(temp);
    
    return root;
}
```

---

### Problem 2: Maximum Depth

```java
public int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```

---

### Problem 3: Symmetric Tree

```java
public boolean isSymmetric(TreeNode root) {
    return isMirror(root, root);
}

private boolean isMirror(TreeNode t1, TreeNode t2) {
    if (t1 == null && t2 == null) return true;
    if (t1 == null || t2 == null) return false;
    
    return t1.val == t2.val &&
           isMirror(t1.left, t2.right) &&
           isMirror(t1.right, t2.left);
}
```

---

### Problem 4: Path Sum

```java
public boolean hasPathSum(TreeNode root, int targetSum) {
    if (root == null) return false;
    
    if (root.left == null && root.right == null)
        return root.val == targetSum;
    
    return hasPathSum(root.left, targetSum - root.val) ||
           hasPathSum(root.right, targetSum - root.val);
}
```

---

### Problem 5: Serialize and Deserialize BST

```java
public String serialize(TreeNode root) {
    if (root == null) return "";
    
    StringBuilder sb = new StringBuilder();
    serializeHelper(root, sb);
    return sb.toString();
}

private void serializeHelper(TreeNode node, StringBuilder sb) {
    if (node == null) return;
    sb.append(node.val).append(",");
    serializeHelper(node.left, sb);
    serializeHelper(node.right, sb);
}

public TreeNode deserialize(String data) {
    if (data.isEmpty()) return null;
    
    String[] values = data.split(",");
    Queue<Integer> queue = new LinkedList<>();
    for (String val : values) queue.offer(Integer.parseInt(val));
    
    return deserializeHelper(queue, Integer.MIN_VALUE, Integer.MAX_VALUE);
}

private TreeNode deserializeHelper(Queue<Integer> queue, int min, int max) {
    if (queue.isEmpty()) return null;
    
    int val = queue.peek();
    if (val < min || val > max) return null;
    
    queue.poll();
    TreeNode root = new TreeNode(val);
    root.left = deserializeHelper(queue, min, val);
    root.right = deserializeHelper(queue, val, max);
    
    return root;
}
```

---

## Time & Space Complexity

| Operation | Average | Worst Case | Space |
|-----------|---------|------------|-------|
| **Search** | O(log n) | O(n) | O(1) iterative, O(h) recursive |
| **Insert** | O(log n) | O(n) | O(1) iterative, O(h) recursive |
| **Delete** | O(log n) | O(n) | O(h) |
| **Find Min/Max** | O(log n) | O(n) | O(1) |
| **In-order Traversal** | O(n) | O(n) | O(h) |
| **Level-order** | O(n) | O(n) | O(w) w=width |

**Note**: Worst case O(n) occurs when tree is skewed (like linked list)

---

## Real-World Use Cases

### 1. Database Indexing
```
B-Trees (variant of BST) used in:
- MySQL InnoDB indexes
- PostgreSQL indexes
- File systems (NTFS, ext4)

Why? O(log n) search for millions of records
```

### 2. Auto-Complete Systems
```
Trie (prefix tree) for:
- Google search suggestions
- IDE code completion
- Spell checkers

BST variant for sorted suggestions
```

### 3. Priority Queues
```
Heap (complete binary tree) for:
- Task scheduling
- Dijkstra's algorithm
- Huffman coding
```

### 4. Expression Parsing
```
Expression trees for:
- Compilers (AST)
- Calculators
- Query optimization

Example: (3 + 5) * 2
      *
     / \
    +   2
   / \
  3   5
```

### 5. File System Hierarchy
```
Directory structure:
- Folders as nodes
- Files as leaves
- Fast search/navigation
```

### 6. Network Routing
```
Routing tables using:
- Radix trees
- Patricia tries
- Fast IP lookup
```

### 7. Game Development
```
Decision trees for:
- AI behavior
- Game state management
- Collision detection (BSP trees)
```

---

## Key Takeaways

1. **BST Property**: Left < Root < Right (recursively)
2. **In-order = Sorted**: Always remember this!
3. **Height Matters**: Balanced O(log n) vs Skewed O(n)
4. **Range Validation**: Don't just check immediate children
5. **Iterative > Recursive**: For interviews (space efficiency)
6. **Use BST Property**: Prune branches in search/range queries
7. **Self-Balancing**: AVL, Red-Black trees maintain O(log n)

---

## Practice Problems

**Easy**:
- LeetCode 700: Search in BST
- LeetCode 701: Insert into BST
- LeetCode 938: Range Sum of BST

**Medium**:
- LeetCode 98: Validate BST
- LeetCode 230: Kth Smallest Element
- LeetCode 450: Delete Node in BST
- LeetCode 235: Lowest Common Ancestor

**Hard**:
- LeetCode 297: Serialize and Deserialize BST
- LeetCode 99: Recover BST
- LeetCode 272: Closest BST Value II
