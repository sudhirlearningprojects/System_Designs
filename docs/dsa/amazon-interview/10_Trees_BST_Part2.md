# Trees & BST - Deep Dive Part 2 (Problems 21-40)

---

## 21. Symmetric Tree (LC 101) ⭐⭐⭐⭐

**Difficulty**: Easy | **Frequency**: High

### Solution
```java
public boolean isSymmetric(TreeNode root) {
    return isMirror(root.left, root.right);
}

private boolean isMirror(TreeNode l, TreeNode r) {
    if (l == null && r == null) return true;
    if (l == null || r == null) return false;
    return l.val == r.val && isMirror(l.left, r.right) && isMirror(l.right, r.left);
}
```

---

## 22. Same Tree (LC 100) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public boolean isSameTree(TreeNode p, TreeNode q) {
    if (p == null && q == null) return true;
    if (p == null || q == null || p.val != q.val) return false;
    return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
}
```

---

## 23. Subtree of Another Tree (LC 572) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public boolean isSubtree(TreeNode root, TreeNode subRoot) {
    if (root == null) return false;
    if (isSameTree(root, subRoot)) return true;
    return isSubtree(root.left, subRoot) || isSubtree(root.right, subRoot);
}

private boolean isSameTree(TreeNode p, TreeNode q) {
    if (p == null && q == null) return true;
    if (p == null || q == null || p.val != q.val) return false;
    return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
}
```

---

## 24. Path Sum III (LC 437) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Problem
Count paths (not necessarily root-to-leaf) that sum to target.

### Solution (Prefix Sum)
```java
public int pathSum(TreeNode root, int targetSum) {
    Map<Long, Integer> prefixCount = new HashMap<>();
    prefixCount.put(0L, 1);
    return dfs(root, 0, targetSum, prefixCount);
}

private int dfs(TreeNode node, long currSum, int target, Map<Long, Integer> map) {
    if (node == null) return 0;
    currSum += node.val;
    int count = map.getOrDefault(currSum - target, 0);
    map.put(currSum, map.getOrDefault(currSum, 0) + 1);
    count += dfs(node.left, currSum, target, map) + dfs(node.right, currSum, target, map);
    map.put(currSum, map.get(currSum) - 1);
    return count;
}
```
**Time**: O(n) | **Space**: O(n)

---

## 25. Binary Tree Paths (LC 257) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public List<String> binaryTreePaths(TreeNode root) {
    List<String> result = new ArrayList<>();
    dfs(root, "", result);
    return result;
}

private void dfs(TreeNode node, String path, List<String> result) {
    if (node == null) return;
    path += node.val;
    if (node.left == null && node.right == null) { result.add(path); return; }
    dfs(node.left, path + "->", result);
    dfs(node.right, path + "->", result);
}
```

---

## 26. Sum Root to Leaf Numbers (LC 129) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int sumNumbers(TreeNode root) {
    return dfs(root, 0);
}

private int dfs(TreeNode node, int curr) {
    if (node == null) return 0;
    curr = curr * 10 + node.val;
    if (node.left == null && node.right == null) return curr;
    return dfs(node.left, curr) + dfs(node.right, curr);
}
```

---

## 27. Vertical Order Traversal (LC 987) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: High

### Solution
```java
public List<List<Integer>> verticalTraversal(TreeNode root) {
    TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> map = new TreeMap<>();
    dfs(root, 0, 0, map);
    List<List<Integer>> result = new ArrayList<>();
    for (TreeMap<Integer, PriorityQueue<Integer>> colMap : map.values()) {
        List<Integer> col = new ArrayList<>();
        for (PriorityQueue<Integer> pq : colMap.values())
            while (!pq.isEmpty()) col.add(pq.poll());
        result.add(col);
    }
    return result;
}

private void dfs(TreeNode node, int col, int row, TreeMap<Integer, TreeMap<Integer, PriorityQueue<Integer>>> map) {
    if (node == null) return;
    map.computeIfAbsent(col, k -> new TreeMap<>())
       .computeIfAbsent(row, k -> new PriorityQueue<>())
       .offer(node.val);
    dfs(node.left, col - 1, row + 1, map);
    dfs(node.right, col + 1, row + 1, map);
}
```

---

## 28. Boundary of Binary Tree (LC 545) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public List<Integer> boundaryOfBinaryTree(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;
    if (!isLeaf(root)) result.add(root.val);
    addLeftBoundary(root.left, result);
    addLeaves(root, result);
    addRightBoundary(root.right, result);
    return result;
}

private void addLeftBoundary(TreeNode node, List<Integer> res) {
    if (node == null || isLeaf(node)) return;
    res.add(node.val);
    if (node.left != null) addLeftBoundary(node.left, res);
    else addLeftBoundary(node.right, res);
}

private void addRightBoundary(TreeNode node, List<Integer> res) {
    if (node == null || isLeaf(node)) return;
    if (node.right != null) addRightBoundary(node.right, res);
    else addRightBoundary(node.left, res);
    res.add(node.val);
}

private void addLeaves(TreeNode node, List<Integer> res) {
    if (node == null) return;
    if (isLeaf(node)) { res.add(node.val); return; }
    addLeaves(node.left, res);
    addLeaves(node.right, res);
}

private boolean isLeaf(TreeNode node) { return node.left == null && node.right == null; }
```

---

## 29. Delete Node in BST (LC 450) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution
```java
public TreeNode deleteNode(TreeNode root, int key) {
    if (root == null) return null;
    if (key < root.val) root.left = deleteNode(root.left, key);
    else if (key > root.val) root.right = deleteNode(root.right, key);
    else {
        if (root.left == null) return root.right;
        if (root.right == null) return root.left;
        // find inorder successor (min of right subtree)
        TreeNode minNode = root.right;
        while (minNode.left != null) minNode = minNode.left;
        root.val = minNode.val;
        root.right = deleteNode(root.right, minNode.val);
    }
    return root;
}
```

---

## 30. Insert into BST (LC 701) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    if (val < root.val) root.left = insertIntoBST(root.left, val);
    else root.right = insertIntoBST(root.right, val);
    return root;
}
```

---

## 31. Trim a BST (LC 669) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public TreeNode trimBST(TreeNode root, int low, int high) {
    if (root == null) return null;
    if (root.val < low) return trimBST(root.right, low, high);
    if (root.val > high) return trimBST(root.left, low, high);
    root.left = trimBST(root.left, low, high);
    root.right = trimBST(root.right, low, high);
    return root;
}
```

---

## 32. Convert Sorted Array to BST (LC 108) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public TreeNode sortedArrayToBST(int[] nums) {
    return build(nums, 0, nums.length - 1);
}

private TreeNode build(int[] nums, int l, int r) {
    if (l > r) return null;
    int mid = l + (r - l) / 2;
    TreeNode node = new TreeNode(nums[mid]);
    node.left = build(nums, l, mid - 1);
    node.right = build(nums, mid + 1, r);
    return node;
}
```

---

## 33. Minimum Absolute Difference in BST (LC 530) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
private int minDiff = Integer.MAX_VALUE, prev = -1;

public int getMinimumDifference(TreeNode root) {
    inorder(root);
    return minDiff;
}

private void inorder(TreeNode node) {
    if (node == null) return;
    inorder(node.left);
    if (prev != -1) minDiff = Math.min(minDiff, node.val - prev);
    prev = node.val;
    inorder(node.right);
}
```

---

## 34. Find Mode in BST (LC 501) ⭐⭐

**Difficulty**: Easy | **Frequency**: Low

### Solution
```java
private int currVal, currCount, maxCount;
private List<Integer> modes = new ArrayList<>();

public int[] findMode(TreeNode root) {
    inorder(root);
    return modes.stream().mapToInt(i -> i).toArray();
}

private void inorder(TreeNode node) {
    if (node == null) return;
    inorder(node.left);
    if (node.val != currVal) { currVal = node.val; currCount = 0; }
    currCount++;
    if (currCount > maxCount) { maxCount = currCount; modes.clear(); modes.add(currVal); }
    else if (currCount == maxCount) modes.add(currVal);
    inorder(node.right);
}
```

---

## 35. Two Sum IV - BST (LC 653) ⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public boolean findTarget(TreeNode root, int k) {
    Set<Integer> set = new HashSet<>();
    return dfs(root, k, set);
}

private boolean dfs(TreeNode node, int k, Set<Integer> set) {
    if (node == null) return false;
    if (set.contains(k - node.val)) return true;
    set.add(node.val);
    return dfs(node.left, k, set) || dfs(node.right, k, set);
}
```

---

## 36. Unique Binary Search Trees (LC 96) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution (Catalan Number)
```java
public int numTrees(int n) {
    int[] dp = new int[n + 1];
    dp[0] = dp[1] = 1;
    for (int i = 2; i <= n; i++)
        for (int j = 1; j <= i; j++)
            dp[i] += dp[j - 1] * dp[i - j];
    return dp[n];
}
```
**Time**: O(n²) | **Space**: O(n)

---

## 37. House Robber III (LC 337) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution
```java
public int rob(TreeNode root) {
    int[] res = dfs(root);
    return Math.max(res[0], res[1]);
}

// returns [rob_root, skip_root]
private int[] dfs(TreeNode node) {
    if (node == null) return new int[]{0, 0};
    int[] left = dfs(node.left), right = dfs(node.right);
    int robRoot = node.val + left[1] + right[1];
    int skipRoot = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);
    return new int[]{robRoot, skipRoot};
}
```

---

## 38. Binary Tree Cameras (LC 968) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Solution
```java
private int cameras = 0;

public int minCameraCover(TreeNode root) {
    return dfs(root) == 0 ? cameras + 1 : cameras;
}

// 0=not covered, 1=covered no camera, 2=has camera
private int dfs(TreeNode node) {
    if (node == null) return 1;
    int left = dfs(node.left), right = dfs(node.right);
    if (left == 0 || right == 0) { cameras++; return 2; }
    return (left == 2 || right == 2) ? 1 : 0;
}
```

---

## 39. Maximum Width of Binary Tree (LC 662) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int widthOfBinaryTree(TreeNode root) {
    if (root == null) return 0;
    int maxWidth = 0;
    Queue<long[]> queue = new LinkedList<>(); // [node_index]
    queue.offer(new long[]{0});
    List<TreeNode> nodes = new ArrayList<>();
    nodes.add(root);
    // BFS with index tracking
    Queue<TreeNode> nodeQ = new LinkedList<>();
    nodeQ.offer(root);
    Queue<Long> idxQ = new LinkedList<>();
    idxQ.offer(0L);
    while (!nodeQ.isEmpty()) {
        int size = nodeQ.size();
        long first = 0, last = 0;
        for (int i = 0; i < size; i++) {
            TreeNode node = nodeQ.poll();
            long idx = idxQ.poll();
            if (i == 0) first = idx;
            if (i == size - 1) last = idx;
            if (node.left != null) { nodeQ.offer(node.left); idxQ.offer(2 * idx); }
            if (node.right != null) { nodeQ.offer(node.right); idxQ.offer(2 * idx + 1); }
        }
        maxWidth = (int) Math.max(maxWidth, last - first + 1);
    }
    return maxWidth;
}
```

---

## 40. Lowest Common Ancestor of BST (LC 235) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution
```java
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    while (root != null) {
        if (p.val < root.val && q.val < root.val) root = root.left;
        else if (p.val > root.val && q.val > root.val) root = root.right;
        else return root;
    }
    return null;
}
```
**Time**: O(h) | **Space**: O(1)

### Dry Run
```
BST: [6,2,8,0,4,7,9], p=2, q=4
root=6: both 2,4 < 6 → go left
root=2: 2<=2 and 4>=2 → return 2
```

---

## Pattern Summary

| Pattern | Problems |
|---------|----------|
| BFS Level Order | 1,2,7,17 |
| DFS Recursion | 3,4,5,8,9,12,13 |
| BST Property | 10,11,29,30,40 |
| Prefix Sum on Tree | 24 |
| DP on Tree | 37,38 |
| Inorder = Sorted | 11,19,33,34 |

**Next**: [Graphs Deep Dive](11_Graphs_Deep_Dive.md)
