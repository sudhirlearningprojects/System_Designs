# Trees & BST - Deep Dive Part 1 (Problems 1-20)

---

## 1. Binary Tree Level Order Traversal (LC 102) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/binary-tree-level-order-traversal/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/level-order-traversal/)

### Problem
Return level-by-level values of a binary tree.

**Example**:
```
    3
   / \
  9  20
    /  \
   15   7
→ [[3],[9,20],[15,7]]
```

### Solution
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
**Time**: O(n) | **Space**: O(n)

### Dry Run
```
queue=[3]
Level 1: poll 3, add 9,20 → level=[3], queue=[9,20]
Level 2: poll 9(no children), poll 20(add 15,7) → level=[9,20], queue=[15,7]
Level 3: poll 15, poll 7 → level=[15,7]
result=[[3],[9,20],[15,7]]
```

### Test Cases
```java
levelOrder(null)       → []
levelOrder([1])        → [[1]]
levelOrder([3,9,20,null,null,15,7]) → [[3],[9,20],[15,7]]
```

### Use Cases
- Printing tree level by level, finding shortest path in unweighted tree, web crawling BFS

---

## 2. Binary Tree Zigzag Level Order (LC 103) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/zigzag-tree-traversal/)

### Solution
```java
public List<List<Integer>> zigzagLevelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    boolean leftToRight = true;
    while (!queue.isEmpty()) {
        int size = queue.size();
        LinkedList<Integer> level = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            TreeNode node = queue.poll();
            if (leftToRight) level.addLast(node.val);
            else level.addFirst(node.val);
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        result.add(level);
        leftToRight = !leftToRight;
    }
    return result;
}
```
**Time**: O(n) | **Space**: O(n)

---

## 3. Maximum Depth of Binary Tree (LC 104) ⭐⭐⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/maximum-depth-of-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/height-of-binary-tree/)

### Solution
```java
public int maxDepth(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
}
```
**Time**: O(n) | **Space**: O(h)

### Test Cases
```java
maxDepth(null)  → 0
maxDepth([1])   → 1
maxDepth([3,9,20,null,null,15,7]) → 3
```

---

## 4. Invert Binary Tree (LC 226) ⭐⭐⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/invert-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/mirror-tree/)

### Solution
```java
public TreeNode invertTree(TreeNode root) {
    if (root == null) return null;
    TreeNode temp = root.left;
    root.left = invertTree(root.right);
    root.right = invertTree(temp);
    return root;
}
```
**Time**: O(n) | **Space**: O(h)

---

## 5. Diameter of Binary Tree (LC 543) ⭐⭐⭐⭐

**Difficulty**: Easy | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/diameter-of-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/diameter-of-binary-tree/)

### Solution
```java
private int diameter = 0;

public int diameterOfBinaryTree(TreeNode root) {
    depth(root);
    return diameter;
}

private int depth(TreeNode node) {
    if (node == null) return 0;
    int left = depth(node.left), right = depth(node.right);
    diameter = Math.max(diameter, left + right);
    return 1 + Math.max(left, right);
}
```
**Time**: O(n) | **Space**: O(h)

### Dry Run
```
    1
   / \
  2   3
 / \
4   5
depth(4)=1, depth(5)=1
depth(2): left=1,right=1, diameter=max(0,2)=2, return 2
depth(3)=1
depth(1): left=2,right=1, diameter=max(2,3)=3, return 3
→ diameter=3
```

---

## 6. Balanced Binary Tree (LC 110) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/balanced-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/check-for-balanced-tree/)

### Solution
```java
public boolean isBalanced(TreeNode root) {
    return checkHeight(root) != -1;
}

private int checkHeight(TreeNode node) {
    if (node == null) return 0;
    int left = checkHeight(node.left);
    if (left == -1) return -1;
    int right = checkHeight(node.right);
    if (right == -1) return -1;
    if (Math.abs(left - right) > 1) return -1;
    return 1 + Math.max(left, right);
}
```

---

## 7. Binary Tree Right Side View (LC 199) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/binary-tree-right-side-view/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/right-view-of-binary-tree/)

### Solution
```java
public List<Integer> rightSideView(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            TreeNode node = queue.poll();
            if (i == size - 1) result.add(node.val); // last node in level
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
    }
    return result;
}
```

---

## 8. Path Sum II (LC 113) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/path-sum-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/root-to-leaf-paths-sum/)

### Problem
Find all root-to-leaf paths where sum equals target.

### Solution
```java
public List<List<Integer>> pathSum(TreeNode root, int target) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, target, new ArrayList<>(), result);
    return result;
}

private void dfs(TreeNode node, int remaining, List<Integer> path, List<List<Integer>> result) {
    if (node == null) return;
    path.add(node.val);
    if (node.left == null && node.right == null && remaining == node.val)
        result.add(new ArrayList<>(path));
    dfs(node.left, remaining - node.val, path, result);
    dfs(node.right, remaining - node.val, path, result);
    path.remove(path.size() - 1);
}
```

---

## 9. Lowest Common Ancestor (LC 236) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/lowest-common-ancestor-in-a-binary-tree/)

### Solution
```java
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root;
    return left != null ? left : right;
}
```
**Time**: O(n) | **Space**: O(h)

### Dry Run
```
Tree: [3,5,1,6,2,0,8], p=5, q=1
LCA(3): LCA(5)=5, LCA(1)=1 → both non-null → return 3
```

---

## 10. Validate Binary Search Tree (LC 98) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/validate-binary-search-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/check-for-bst/)

### Solution
```java
public boolean isValidBST(TreeNode root) {
    return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

private boolean validate(TreeNode node, long min, long max) {
    if (node == null) return true;
    if (node.val <= min || node.val >= max) return false;
    return validate(node.left, min, node.val) && validate(node.right, node.val, max);
}
```
**Time**: O(n) | **Space**: O(h)

### Test Cases
```java
isValidBST([2,1,3])     → true
isValidBST([5,1,4,null,null,3,6]) → false (4 in right but 3<5)
isValidBST([2,2,2])     → false
```

---

## 11. Kth Smallest Element in BST (LC 230) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/kth-smallest-element-in-a-bst/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/find-k-th-smallest-element-in-bst/)

### Solution (Inorder)
```java
private int count = 0, result = 0;

public int kthSmallest(TreeNode root, int k) {
    inorder(root, k);
    return result;
}

private void inorder(TreeNode node, int k) {
    if (node == null) return;
    inorder(node.left, k);
    if (++count == k) { result = node.val; return; }
    inorder(node.right, k);
}
```
**Time**: O(h + k) | **Space**: O(h)

---

## 12. Binary Tree Maximum Path Sum (LC 124) ⭐⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/binary-tree-maximum-path-sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/maximum-path-sum/)

### Solution
```java
private int maxSum = Integer.MIN_VALUE;

public int maxPathSum(TreeNode root) {
    gain(root);
    return maxSum;
}

private int gain(TreeNode node) {
    if (node == null) return 0;
    int left = Math.max(gain(node.left), 0);
    int right = Math.max(gain(node.right), 0);
    maxSum = Math.max(maxSum, node.val + left + right);
    return node.val + Math.max(left, right);
}
```
**Time**: O(n) | **Space**: O(h)

### Dry Run
```
    -10
    /  \
   9   20
      /  \
     15   7
gain(9)=9, gain(15)=15, gain(7)=7
gain(20): left=15,right=7, maxSum=max(MIN,20+15+7)=42, return 20+15=35
gain(-10): left=9,right=35, maxSum=max(42,-10+9+35)=42, return -10+35=25
→ 42
```

---

## 13. Serialize and Deserialize Binary Tree (LC 297) ⭐⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Very High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/serialize-and-deserialize-a-binary-tree/)

### Solution
```java
public String serialize(TreeNode root) {
    if (root == null) return "null";
    return root.val + "," + serialize(root.left) + "," + serialize(root.right);
}

public TreeNode deserialize(String data) {
    Queue<String> q = new LinkedList<>(Arrays.asList(data.split(",")));
    return build(q);
}

private TreeNode build(Queue<String> q) {
    String val = q.poll();
    if (val.equals("null")) return null;
    TreeNode node = new TreeNode(Integer.parseInt(val));
    node.left = build(q);
    node.right = build(q);
    return node;
}
```

---

## 14. Construct Binary Tree from Preorder and Inorder (LC 105) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/construct-tree-1/)

### Solution
```java
public TreeNode buildTree(int[] preorder, int[] inorder) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < inorder.length; i++) map.put(inorder[i], i);
    return build(preorder, 0, preorder.length - 1, 0, inorder.length - 1, map);
}

private TreeNode build(int[] pre, int preL, int preR, int inL, int inR, Map<Integer, Integer> map) {
    if (preL > preR) return null;
    TreeNode root = new TreeNode(pre[preL]);
    int mid = map.get(pre[preL]);
    int leftSize = mid - inL;
    root.left = build(pre, preL + 1, preL + leftSize, inL, mid - 1, map);
    root.right = build(pre, preL + leftSize + 1, preR, mid + 1, inR, map);
    return root;
}
```

---

## 15. Flatten Binary Tree to Linked List (LC 114) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/flatten-binary-tree-to-linked-list/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/flatten-binary-tree-to-linked-list/)

### Solution (Morris-like)
```java
public void flatten(TreeNode root) {
    TreeNode curr = root;
    while (curr != null) {
        if (curr.left != null) {
            TreeNode rightmost = curr.left;
            while (rightmost.right != null) rightmost = rightmost.right;
            rightmost.right = curr.right;
            curr.right = curr.left;
            curr.left = null;
        }
        curr = curr.right;
    }
}
```
**Time**: O(n) | **Space**: O(1)

---

## 16. Count Complete Tree Nodes (LC 222) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/count-complete-tree-nodes/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/count-number-of-nodes-in-a-complete-binary-tree/)

### Solution
```java
public int countNodes(TreeNode root) {
    if (root == null) return 0;
    int leftH = 0, rightH = 0;
    TreeNode l = root, r = root;
    while (l != null) { leftH++; l = l.left; }
    while (r != null) { rightH++; r = r.right; }
    if (leftH == rightH) return (1 << leftH) - 1; // perfect tree
    return 1 + countNodes(root.left) + countNodes(root.right);
}
```
**Time**: O(log²n) | **Space**: O(log n)

---

## 17. All Nodes Distance K (LC 863) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/all-nodes-distance-k-in-binary-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/nodes-at-given-distance-in-binary-tree/)

### Solution
```java
public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
    Map<TreeNode, TreeNode> parent = new HashMap<>();
    buildParent(root, null, parent);

    Queue<TreeNode> queue = new LinkedList<>();
    Set<TreeNode> visited = new HashSet<>();
    queue.offer(target);
    visited.add(target);
    int dist = 0;

    while (!queue.isEmpty()) {
        if (dist == k) return new ArrayList<>(queue.stream().map(n -> n.val).collect(java.util.stream.Collectors.toList()));
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            TreeNode node = queue.poll();
            for (TreeNode next : new TreeNode[]{node.left, node.right, parent.get(node)}) {
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        dist++;
    }
    return new ArrayList<>();
}

private void buildParent(TreeNode node, TreeNode par, Map<TreeNode, TreeNode> parent) {
    if (node == null) return;
    parent.put(node, par);
    buildParent(node.left, node, parent);
    buildParent(node.right, node, parent);
}
```

---

## 18. Binary Search Tree Iterator (LC 173) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/binary-search-tree-iterator/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/binary-search-tree-iterator/)

### Solution
```java
class BSTIterator {
    Stack<TreeNode> stack = new Stack<>();

    public BSTIterator(TreeNode root) { pushLeft(root); }

    public int next() {
        TreeNode node = stack.pop();
        pushLeft(node.right);
        return node.val;
    }

    public boolean hasNext() { return !stack.isEmpty(); }

    private void pushLeft(TreeNode node) {
        while (node != null) { stack.push(node); node = node.left; }
    }
}
```
**Time**: O(1) amortized | **Space**: O(h)

---

## 19. Recover Binary Search Tree (LC 99) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/recover-binary-search-tree/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/fixed-two-nodes-of-a-bst/)

### Solution (Morris Inorder)
```java
public void recoverTree(TreeNode root) {
    TreeNode first = null, second = null, prev = null, curr = root;
    while (curr != null) {
        if (curr.left == null) {
            if (prev != null && prev.val > curr.val) {
                if (first == null) first = prev;
                second = curr;
            }
            prev = curr;
            curr = curr.right;
        } else {
            TreeNode pred = curr.left;
            while (pred.right != null && pred.right != curr) pred = pred.right;
            if (pred.right == null) { pred.right = curr; curr = curr.left; }
            else {
                pred.right = null;
                if (prev != null && prev.val > curr.val) {
                    if (first == null) first = prev;
                    second = curr;
                }
                prev = curr;
                curr = curr.right;
            }
        }
    }
    int temp = first.val; first.val = second.val; second.val = temp;
}
```

---

## 20. Populating Next Right Pointers (LC 116) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/populating-next-right-pointers-in-each-node/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/connect-nodes-at-same-level/)

### Solution (O(1) space)
```java
public Node connect(Node root) {
    if (root == null) return null;
    Node leftmost = root;
    while (leftmost.left != null) {
        Node curr = leftmost;
        while (curr != null) {
            curr.left.next = curr.right;
            if (curr.next != null) curr.right.next = curr.next.left;
            curr = curr.next;
        }
        leftmost = leftmost.left;
    }
    return root;
}
```
**Time**: O(n) | **Space**: O(1)
