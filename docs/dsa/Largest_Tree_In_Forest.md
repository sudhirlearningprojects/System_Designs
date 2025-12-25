# Largest Tree in Forest

## Problem Statement

Given a forest (one or more disconnected trees) represented as a child-to-parent mapping, find the root of the largest tree. If multiple trees have the same maximum size, return the smallest root ID.

**Input:** Map<Integer, Integer> where key = child, value = immediate parent

**Output:** Root ID of the largest tree (smallest ID if tie)

**Constraints:**
- Child cannot have more than one immediate parent
- Parent can have multiple children
- Well-formed forest (n nodes → n-1 edges per tree)

**Examples:**
```
Input: {1→2, 3→4}

Tree 1:    2        Tree 2:    4
           |                   |
           1                   3

Both trees have size 2
Result: 2 (smaller root ID)

Input: {2→3, 3→1, 7→8, 9→8, 12→15, 13→15, 11→15, 5→12}

Tree 1:    1         Tree 2:    8         Tree 3:    15
           |                   / \                   / | \
           3                  7   9                11 12 13
           |                                          |
           2                                          5

Sizes: Tree1=3, Tree2=3, Tree3=5
Result: 15 (largest tree)
```

---

## Solution Approaches

### Approach 1: Find Roots + DFS/BFS Size Calculation

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer largestTree(final Map<Integer, Integer> immediateParent) {
    if (immediateParent.isEmpty()) return 0;
    
    // Build parent-to-children map
    Map<Integer, List<Integer>> parentToChild = new HashMap<>();
    Set<Integer> allNodes = new HashSet<>();
    
    for (Map.Entry<Integer, Integer> entry : immediateParent.entrySet()) {
        int child = entry.getKey();
        int parent = entry.getValue();
        
        parentToChild.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
        allNodes.add(child);
        allNodes.add(parent);
    }
    
    // Find roots (nodes that are not children)
    Set<Integer> roots = new HashSet<>();
    for (int node : allNodes) {
        if (!immediateParent.containsKey(node)) {
            roots.add(node);
        }
    }
    
    // Find largest tree
    int maxSize = 0;
    int resultRoot = Integer.MAX_VALUE;
    
    for (int root : roots) {
        int size = getTreeSize(parentToChild, root) + 1; // +1 for root itself
        
        if (size > maxSize || (size == maxSize && root < resultRoot)) {
            maxSize = size;
            resultRoot = root;
        }
    }
    
    return resultRoot;
}

private static Integer getTreeSize(Map<Integer, List<Integer>> parentToChild, Integer rootIndex) {
    int result = 0;
    Stack<Integer> nodes = new Stack<>();
    nodes.push(rootIndex);
    
    while (!nodes.empty()) {
        Integer index = nodes.pop();
        for (Integer childIndex : parentToChild.getOrDefault(index, new ArrayList<>())) {
            nodes.push(childIndex);
            result++;
        }
    }
    
    return result;
}
```

---

### Approach 2: Union-Find (Optimized)

**Time Complexity:** O(n α(n)) ≈ O(n)  
**Space Complexity:** O(n)

```java
public static Integer largestTreeUnionFind(final Map<Integer, Integer> immediateParent) {
    if (immediateParent.isEmpty()) return 0;
    
    Map<Integer, Integer> parent = new HashMap<>();
    Map<Integer, Integer> size = new HashMap<>();
    
    // Initialize
    Set<Integer> allNodes = new HashSet<>();
    for (Map.Entry<Integer, Integer> entry : immediateParent.entrySet()) {
        allNodes.add(entry.getKey());
        allNodes.add(entry.getValue());
    }
    
    for (int node : allNodes) {
        parent.put(node, node);
        size.put(node, 1);
    }
    
    // Union operations
    for (Map.Entry<Integer, Integer> entry : immediateParent.entrySet()) {
        int child = entry.getKey();
        int par = entry.getValue();
        union(parent, size, child, par);
    }
    
    // Find largest tree root
    Map<Integer, Integer> rootToSize = new HashMap<>();
    for (int node : allNodes) {
        int root = find(parent, node);
        rootToSize.put(root, size.get(root));
    }
    
    int maxSize = 0;
    int resultRoot = Integer.MAX_VALUE;
    
    for (Map.Entry<Integer, Integer> entry : rootToSize.entrySet()) {
        int root = entry.getKey();
        int treeSize = entry.getValue();
        
        if (treeSize > maxSize || (treeSize == maxSize && root < resultRoot)) {
            maxSize = treeSize;
            resultRoot = root;
        }
    }
    
    return resultRoot;
}

private static int find(Map<Integer, Integer> parent, int x) {
    if (parent.get(x) != x) {
        parent.put(x, find(parent, parent.get(x))); // Path compression
    }
    return parent.get(x);
}

private static void union(Map<Integer, Integer> parent, Map<Integer, Integer> size, 
                         int x, int y) {
    int rootX = find(parent, x);
    int rootY = find(parent, y);
    
    if (rootX != rootY) {
        // Union by size
        if (size.get(rootX) < size.get(rootY)) {
            parent.put(rootX, rootY);
            size.put(rootY, size.get(rootY) + size.get(rootX));
        } else {
            parent.put(rootY, rootX);
            size.put(rootX, size.get(rootX) + size.get(rootY));
        }
    }
}
```

---

### Approach 3: Single Pass with Root Tracking

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer largestTreeSinglePass(final Map<Integer, Integer> immediateParent) {
    if (immediateParent.isEmpty()) return 0;
    
    Map<Integer, List<Integer>> parentToChild = new HashMap<>();
    Set<Integer> children = new HashSet<>();
    Set<Integer> allNodes = new HashSet<>();
    
    for (Map.Entry<Integer, Integer> entry : immediateParent.entrySet()) {
        int child = entry.getKey();
        int parent = entry.getValue();
        
        parentToChild.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
        children.add(child);
        allNodes.add(child);
        allNodes.add(parent);
    }
    
    int maxSize = 0;
    int resultRoot = Integer.MAX_VALUE;
    
    for (int node : allNodes) {
        if (!children.contains(node)) { // This is a root
            int size = 1 + countDescendants(parentToChild, node);
            
            if (size > maxSize || (size == maxSize && node < resultRoot)) {
                maxSize = size;
                resultRoot = node;
            }
        }
    }
    
    return resultRoot;
}

private static int countDescendants(Map<Integer, List<Integer>> parentToChild, int node) {
    int count = 0;
    Queue<Integer> queue = new LinkedList<>();
    queue.offer(node);
    
    while (!queue.isEmpty()) {
        int current = queue.poll();
        List<Integer> children = parentToChild.get(current);
        
        if (children != null) {
            count += children.size();
            queue.addAll(children);
        }
    }
    
    return count;
}
```

---

## Algorithm Walkthrough

### Example: {2→3, 3→1, 7→8, 9→8, 12→15, 13→15, 11→15, 5→12}

**Step 1: Build parent-to-children map**
```
parentToChild:
  1 → [3]
  3 → [2]
  8 → [7, 9]
  15 → [11, 12, 13]
  12 → [5]

allNodes: {1, 2, 3, 7, 8, 9, 11, 12, 13, 15, 5}
```

**Step 2: Identify roots**
```
children: {2, 3, 7, 9, 11, 12, 13, 5}
roots: allNodes - children = {1, 8, 15}
```

**Step 3: Calculate tree sizes**
```
Root 1:
  DFS: 1 → 3 → 2
  Size: 3

Root 8:
  DFS: 8 → 7, 9
  Size: 3

Root 15:
  DFS: 15 → 11, 12, 13 → 5
  Size: 5
```

**Step 4: Find largest**
```
Sizes: {1: 3, 8: 3, 15: 5}
Max size: 5
Result: 15
```

### Example with Tie: {9→4, 1→4, 5→2, 8→4, 7→3, 2→3, 6→7, 10→4}

```
Tree 1 (root 4):        Tree 2 (root 3):
       4                       3
    / | | \                   / \
   9  1 8 10                 7   2
                             |   |
                             6   5

Size of tree 4: 5
Size of tree 3: 5

Both have size 5, return min(4, 3) = 3
```

---

## Complete Implementation

```java
import java.util.*;

class Solution {
    
    // Main solution
    public static Integer largestTree(final Map<Integer, Integer> immediateParent) {
        if (immediateParent.isEmpty()) return 0;
        
        // Build parent-to-children map
        Map<Integer, List<Integer>> parentToChild = new HashMap<>();
        Set<Integer> allNodes = new HashSet<>();
        
        for (Map.Entry<Integer, Integer> entry : immediateParent.entrySet()) {
            int child = entry.getKey();
            int parent = entry.getValue();
            
            parentToChild.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
            allNodes.add(child);
            allNodes.add(parent);
        }
        
        // Find roots (nodes that are not children)
        Set<Integer> roots = new HashSet<>();
        for (int node : allNodes) {
            if (!immediateParent.containsKey(node)) {
                roots.add(node);
            }
        }
        
        // Find largest tree
        int maxSize = 0;
        int resultRoot = Integer.MAX_VALUE;
        
        for (int root : roots) {
            int size = getTreeSize(parentToChild, root) + 1;
            
            if (size > maxSize || (size == maxSize && root < resultRoot)) {
                maxSize = size;
                resultRoot = root;
            }
        }
        
        return resultRoot;
    }
    
    public static Integer getTreeSize(final Map<Integer, List<Integer>> parentToChild, 
                                      final Integer rootIndex) {
        Integer result = 0;
        final Stack<Integer> nodes = new Stack<>();
        nodes.push(rootIndex);
        
        while (!nodes.empty()) {
            final Integer index = nodes.pop();
            for (final Integer childIndex : parentToChild.getOrDefault(index, new ArrayList<>())) {
                nodes.push(childIndex);
                result++;
            }
        }
        
        return result;
    }
    
    public static boolean doTestsPass() {
        Map<Map<Integer, Integer>, Integer> testCases = new HashMap<>();
        
        // Test 1: Example
        Map<Integer, Integer> test1 = new HashMap<>();
        test1.put(1, 2);
        test1.put(3, 4);
        testCases.put(test1, 2);
        
        // Test 2: More than two trees
        Map<Integer, Integer> test2 = new HashMap<>();
        test2.put(2, 3);
        test2.put(7, 8);
        test2.put(12, 15);
        test2.put(3, 1);
        test2.put(13, 15);
        test2.put(11, 15);
        test2.put(9, 8);
        test2.put(5, 12);
        testCases.put(test2, 15);
        
        // Test 3: Large index values
        Map<Integer, Integer> test3 = new HashMap<>();
        test3.put(200000000, 300000000);
        test3.put(500000000, 200000000);
        test3.put(700000000, 300000000);
        test3.put(600000000, 700000000);
        test3.put(900000000, 400000000);
        test3.put(100000000, 400000000);
        test3.put(800000000, 400000000);
        test3.put(1000000000, 400000000);
        testCases.put(test3, 300000000);
        
        // Test 4: Two trees of same size
        Map<Integer, Integer> test4 = new HashMap<>();
        test4.put(9, 4);
        test4.put(1, 4);
        test4.put(5, 2);
        test4.put(8, 4);
        test4.put(7, 3);
        test4.put(2, 3);
        test4.put(6, 7);
        test4.put(10, 4);
        testCases.put(test4, 3);
        
        // Test 5: Tree sizes differ by one
        Map<Integer, Integer> test5 = new HashMap<>();
        test5.put(35, 33);
        test5.put(33, 28);
        test5.put(31, 22);
        test5.put(28, 25);
        test5.put(34, 31);
        test5.put(29, 27);
        test5.put(21, 23);
        test5.put(25, 21);
        test5.put(22, 29);
        testCases.put(test5, 23);
        
        boolean passed = true;
        for (Map.Entry<Map<Integer, Integer>, Integer> entry : testCases.entrySet()) {
            final Integer actual = largestTree(entry.getKey());
            if (!actual.equals(entry.getValue())) {
                passed = false;
                System.out.println("Failed for " + entry.getKey().toString() + 
                                 "\n  expected " + entry.getValue() + ", actual " + actual);
            }
        }
        
        return passed;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail.");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testLargestTree() {
    // Test 1: Two trees, same size
    Map<Integer, Integer> test1 = new HashMap<>();
    test1.put(1, 2);
    test1.put(3, 4);
    assertEquals(2, largestTree(test1));
    
    // Test 2: Multiple trees, different sizes
    Map<Integer, Integer> test2 = new HashMap<>();
    test2.put(2, 3);
    test2.put(3, 1);
    test2.put(7, 8);
    test2.put(9, 8);
    test2.put(12, 15);
    test2.put(13, 15);
    test2.put(11, 15);
    test2.put(5, 12);
    assertEquals(15, largestTree(test2));
    
    // Test 3: Single tree
    Map<Integer, Integer> test3 = new HashMap<>();
    test3.put(1, 2);
    test3.put(2, 3);
    assertEquals(3, largestTree(test3));
    
    // Test 4: Empty map
    assertEquals(0, largestTree(new HashMap<>()));
    
    // Test 5: Single edge
    Map<Integer, Integer> test5 = new HashMap<>();
    test5.put(1, 2);
    assertEquals(2, largestTree(test5));
}
```

---

## Visual Representation

### Example Forest Structure

```
Input: {2→3, 3→1, 7→8, 9→8, 12→15, 13→15, 11→15, 5→12}

Forest Visualization:

Tree 1 (root=1, size=3):
    1
    |
    3
    |
    2

Tree 2 (root=8, size=3):
    8
   / \
  7   9

Tree 3 (root=15, size=5):
      15
    / | \
   11 12 13
      |
      5

Result: 15 (largest tree with 5 nodes)
```

### Tie-Breaking Example

```
Input: {9→4, 1→4, 8→4, 10→4, 7→3, 2→3, 6→7, 5→2}

Tree 1 (root=4, size=5):        Tree 2 (root=3, size=5):
       4                               3
    / | | \                           / \
   9  1 8 10                         7   2
                                     |   |
                                     6   5

Both size 5, return min(4, 3) = 3
```

---

## Edge Cases

1. **Empty map:** Return 0
2. **Single edge:** Return parent node
3. **Single tree:** Return its root
4. **All trees same size:** Return smallest root ID
5. **Large node IDs:** Handle up to Integer.MAX_VALUE
6. **Linear tree:** Chain of nodes
7. **Star tree:** One root, many children

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| DFS/BFS Size | O(n) | O(n) | Simple, clear |
| Union-Find | O(n α(n)) | O(n) | Near-linear with path compression |
| Single Pass | O(n) | O(n) | Optimal |

**Where:**
- n = total number of nodes in forest

**Why O(n)?**
- Build parent-to-child map: O(n)
- Find roots: O(n)
- Calculate sizes: O(n) total (each node visited once)
- Find max: O(number of roots) ≤ O(n)

---

## Related Problems

1. **Number of Connected Components** - Count trees in forest
2. **Diameter of Binary Tree** - Longest path in tree
3. **Lowest Common Ancestor** - Find LCA in tree
4. **Tree Height** - Maximum depth
5. **Serialize and Deserialize Tree** - Tree encoding
6. **Clone Graph** - Deep copy of graph

---

## Interview Tips

### Clarification Questions
1. Can the map be empty? (Yes, return 0)
2. Are node IDs always positive? (Yes, but can be large)
3. What if multiple trees have same max size? (Return smallest root ID)
4. Is the input guaranteed to be a valid forest? (Yes)
5. Can there be cycles? (No, it's a forest)

### Approach Explanation
1. "Build parent-to-children adjacency list"
2. "Identify roots: nodes not appearing as children"
3. "Calculate size of each tree using DFS/BFS"
4. "Track maximum size and corresponding root"
5. "Handle ties by choosing smallest root ID"

### Common Mistakes
- Forgetting to add 1 for root node in size calculation
- Not handling tie-breaking correctly (smallest ID)
- Confusing child-to-parent with parent-to-child mapping
- Not considering empty input
- Off-by-one errors in tree size counting

### Optimization Insights
- DFS with stack vs recursion (avoid stack overflow)
- Can use BFS instead of DFS (same complexity)
- Union-Find is overkill but shows advanced knowledge

---

## Real-World Applications

1. **Organization Hierarchy** - Find largest department
2. **File System** - Largest directory tree
3. **Social Networks** - Largest connected component
4. **Dependency Analysis** - Largest dependency tree
5. **Version Control** - Largest branch
6. **Network Topology** - Largest subnet

---

## Key Takeaways

1. **Root Identification:** Nodes not appearing as children are roots
2. **Tree Size:** DFS/BFS from root counts all descendants
3. **Tie-Breaking:** When sizes equal, choose smallest root ID
4. **Time Complexity:** O(n) - visit each node once
5. **Space Complexity:** O(n) - store adjacency list and visited nodes
6. **Data Structure:** Parent-to-children map for efficient traversal
7. **Edge Cases:** Empty map, single tree, all same size

---

## Additional Notes

### Why Build Parent-to-Children Map?

```
Given: child → parent
Need: parent → children (for tree traversal)

Example: {1→2, 3→2}
  Input map: {1: 2, 3: 2}
  Need: {2: [1, 3]} to traverse from root 2
```

### DFS vs BFS for Size Calculation

Both work equally well:

**DFS (Stack):**
```java
Stack<Integer> stack = new Stack<>();
stack.push(root);
while (!stack.isEmpty()) {
    int node = stack.pop();
    for (int child : children) {
        stack.push(child);
        count++;
    }
}
```

**BFS (Queue):**
```java
Queue<Integer> queue = new LinkedList<>();
queue.offer(root);
while (!queue.isEmpty()) {
    int node = queue.poll();
    for (int child : children) {
        queue.offer(child);
        count++;
    }
}
```

### Handling Tie-Breaking

```java
if (size > maxSize || (size == maxSize && root < resultRoot)) {
    maxSize = size;
    resultRoot = root;
}

// Equivalent to:
if (size > maxSize) {
    maxSize = size;
    resultRoot = root;
} else if (size == maxSize && root < resultRoot) {
    resultRoot = root;
}
```

### Why Not Use Recursion?

Recursion works but risks stack overflow for deep trees:

```java
private int getTreeSizeRecursive(Map<Integer, List<Integer>> graph, int node) {
    List<Integer> children = graph.getOrDefault(node, new ArrayList<>());
    int size = 1; // Count current node
    
    for (int child : children) {
        size += getTreeSizeRecursive(graph, child);
    }
    
    return size;
}
```

Iterative approach (with Stack) is safer for large trees.

### Union-Find Advantage

Union-Find is useful when:
- Building forest incrementally
- Need to query connectivity dynamically
- Multiple operations on the forest

For this problem, simple DFS/BFS is clearer and sufficient.
