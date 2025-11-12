# Complete DSA Guide Part 2 - Trees, Graphs, Algorithms & Design Techniques

*Advanced data structures and algorithmic design techniques for freshers and MCQ exam preparation*

## Table of Contents
8. [Trees](#trees)
9. [Binary Search Trees](#bst)
10. [Heaps](#heaps)
11. [Graphs](#graphs)
12. [Graph Traversals](#graph-traversals)
13. [Connected Components](#connected-components)
14. [Spanning Trees](#spanning-trees)
15. [Shortest Paths](#shortest-paths)
16. [Hashing](#hashing)
17. [Sorting Algorithms](#sorting)
18. [Searching Algorithms](#searching)
19. [Greedy Algorithms](#greedy)
20. [Dynamic Programming](#dp)
21. [Divide and Conquer](#divide-conquer)
22. [MCQ Practice Questions](#mcq-questions)

---

## 8. Trees {#trees}

### Understanding Trees

**Definition**
A tree is a hierarchical data structure consisting of nodes connected by edges, with one node designated as the root. Each node can have zero or more child nodes, and there are no cycles.

**Tree Terminology**
- **Root**: Top node with no parent
- **Parent**: Node with children
- **Child**: Node with a parent
- **Leaf**: Node with no children
- **Sibling**: Nodes with same parent
- **Ancestor**: Node on path from root to current node
- **Descendant**: Node in subtree of current node
- **Height**: Maximum distance from node to leaf
- **Depth**: Distance from root to node
- **Level**: All nodes at same depth

**Tree Properties**
- **N nodes**: Exactly N-1 edges
- **Connected**: Path exists between any two nodes
- **Acyclic**: No cycles exist
- **Hierarchical**: Parent-child relationships

### Binary Trees

**Definition**
A binary tree is a tree where each node has at most two children, referred to as left child and right child.

**Binary Tree Node Structure**
```
class TreeNode:
    data: value stored in node
    left: reference to left child
    right: reference to right child
```

**Types of Binary Trees**

**Full Binary Tree**
- Every node has either 0 or 2 children
- No node has exactly 1 child

**Complete Binary Tree**
- All levels filled except possibly the last
- Last level filled from left to right
- Used in heap implementation

**Perfect Binary Tree**
- All internal nodes have 2 children
- All leaves at same level
- Total nodes = 2^h - 1 (h = height)

**Balanced Binary Tree**
- Height difference between left and right subtrees ≤ 1
- Ensures O(log n) operations

### Tree Traversals

**Depth-First Traversals**

**Inorder Traversal (Left-Root-Right)**
```
function inorder(root):
    if root != null:
        inorder(root.left)
        print root.data
        inorder(root.right)
```
- **Use**: Gets sorted order in BST
- **Time**: O(n), **Space**: O(h) where h = height

**Preorder Traversal (Root-Left-Right)**
```
function preorder(root):
    if root != null:
        print root.data
        inorder(root.left)
        inorder(root.right)
```
- **Use**: Tree copying, expression trees
- **Time**: O(n), **Space**: O(h)

**Postorder Traversal (Left-Right-Root)**
```
function postorder(root):
    if root != null:
        postorder(root.left)
        postorder(root.right)
        print root.data
```
- **Use**: Tree deletion, calculating size
- **Time**: O(n), **Space**: O(h)

**Breadth-First Traversal**

**Level Order Traversal**
```
function levelOrder(root):
    if root == null: return
    queue = new Queue()
    queue.enqueue(root)
    
    while not queue.isEmpty():
        node = queue.dequeue()
        print node.data
        
        if node.left != null:
            queue.enqueue(node.left)
        if node.right != null:
            queue.enqueue(node.right)
```
- **Use**: Level-wise processing
- **Time**: O(n), **Space**: O(w) where w = maximum width

**Iterative Traversals**

**Iterative Inorder**
```
function iterativeInorder(root):
    stack = new Stack()
    current = root
    
    while current != null or not stack.isEmpty():
        while current != null:
            stack.push(current)
            current = current.left
        
        current = stack.pop()
        print current.data
        current = current.right
```

**Morris Traversal (O(1) Space)**
```
function morrisInorder(root):
    current = root
    
    while current != null:
        if current.left == null:
            print current.data
            current = current.right
        else:
            predecessor = current.left
            while predecessor.right != null and predecessor.right != current:
                predecessor = predecessor.right
            
            if predecessor.right == null:
                predecessor.right = current
                current = current.left
            else:
                predecessor.right = null
                print current.data
                current = current.right
```

### Tree Construction

**From Inorder and Preorder**
```
function buildTree(preorder, inorder):
    if not preorder or not inorder:
        return null
    
    root = new TreeNode(preorder[0])
    rootIndex = inorder.indexOf(preorder[0])
    
    root.left = buildTree(preorder[1:rootIndex+1], inorder[0:rootIndex])
    root.right = buildTree(preorder[rootIndex+1:], inorder[rootIndex+1:])
    
    return root
```

**From Inorder and Postorder**
```
function buildTreeFromInPost(inorder, postorder):
    if not inorder or not postorder:
        return null
    
    root = new TreeNode(postorder[-1])
    rootIndex = inorder.indexOf(postorder[-1])
    
    root.left = buildTreeFromInPost(inorder[0:rootIndex], postorder[0:rootIndex])
    root.right = buildTreeFromInPost(inorder[rootIndex+1:], postorder[rootIndex:-1])
    
    return root
```

### Common Tree Problems

**Maximum Depth**
```
function maxDepth(root):
    if root == null: return 0
    return 1 + max(maxDepth(root.left), maxDepth(root.right))
```

**Diameter of Tree**
```
function diameter(root):
    if root == null: return 0
    
    leftHeight = height(root.left)
    rightHeight = height(root.right)
    
    leftDiameter = diameter(root.left)
    rightDiameter = diameter(root.right)
    
    return max(leftHeight + rightHeight + 1, max(leftDiameter, rightDiameter))
```

**Lowest Common Ancestor**
```
function LCA(root, p, q):
    if root == null or root == p or root == q:
        return root
    
    left = LCA(root.left, p, q)
    right = LCA(root.right, p, q)
    
    if left != null and right != null:
        return root
    
    return left != null ? left : right
```

---

## 9. Binary Search Trees {#bst}

### Understanding BST

**Definition**
A Binary Search Tree is a binary tree where for each node:
- All nodes in left subtree have values less than node's value
- All nodes in right subtree have values greater than node's value
- Both subtrees are also BSTs

**BST Property**
```
For any node N:
- Left subtree values < N.data
- Right subtree values > N.data
```

### BST Operations

**Search**
```
function search(root, key):
    if root == null or root.data == key:
        return root
    
    if key < root.data:
        return search(root.left, key)
    else:
        return search(root.right, key)
```
- **Time**: O(h) where h = height
- **Best Case**: O(log n) for balanced BST
- **Worst Case**: O(n) for skewed BST

**Insertion**
```
function insert(root, key):
    if root == null:
        return new TreeNode(key)
    
    if key < root.data:
        root.left = insert(root.left, key)
    elif key > root.data:
        root.right = insert(root.right, key)
    
    return root
```
- **Time**: O(h), **Space**: O(h) for recursion

**Deletion**
```
function delete(root, key):
    if root == null: return root
    
    if key < root.data:
        root.left = delete(root.left, key)
    elif key > root.data:
        root.right = delete(root.right, key)
    else:
        // Node to be deleted found
        if root.left == null:
            return root.right
        elif root.right == null:
            return root.left
        
        // Node has two children
        minNode = findMin(root.right)
        root.data = minNode.data
        root.right = delete(root.right, minNode.data)
    
    return root

function findMin(root):
    while root.left != null:
        root = root.left
    return root
```

**Find Minimum/Maximum**
```
function findMin(root):
    if root == null: return null
    while root.left != null:
        root = root.left
    return root

function findMax(root):
    if root == null: return null
    while root.right != null:
        root = root.right
    return root
```

### BST Validation

**Check if Valid BST**
```
function isValidBST(root):
    return validate(root, null, null)

function validate(node, minVal, maxVal):
    if node == null: return true
    
    if (minVal != null and node.data <= minVal) or 
       (maxVal != null and node.data >= maxVal):
        return false
    
    return validate(node.left, minVal, node.data) and 
           validate(node.right, node.data, maxVal)
```

### BST Applications

**Range Queries**
```
function rangeQuery(root, low, high):
    result = []
    rangeQueryHelper(root, low, high, result)
    return result

function rangeQueryHelper(root, low, high, result):
    if root == null: return
    
    if root.data > low:
        rangeQueryHelper(root.left, low, high, result)
    
    if low <= root.data <= high:
        result.append(root.data)
    
    if root.data < high:
        rangeQueryHelper(root.right, low, high, result)
```

**Kth Smallest Element**
```
function kthSmallest(root, k):
    count = [0]  // Use array to pass by reference
    return kthSmallestHelper(root, k, count)

function kthSmallestHelper(root, k, count):
    if root == null: return null
    
    left = kthSmallestHelper(root.left, k, count)
    if left != null: return left
    
    count[0]++
    if count[0] == k: return root.data
    
    return kthSmallestHelper(root.right, k, count)
```

---

## 10. Heaps {#heaps}

### Understanding Heaps

**Definition**
A heap is a complete binary tree that satisfies the heap property:
- **Max Heap**: Parent ≥ children
- **Min Heap**: Parent ≤ children

**Heap Properties**
- **Complete Binary Tree**: All levels filled except possibly last
- **Array Representation**: Efficient storage
- **Parent-Child Relationship**: 
  - Parent of i: (i-1)/2
  - Left child of i: 2*i + 1
  - Right child of i: 2*i + 2

### Heap Operations

**Heapify (Maintain Heap Property)**
```
function maxHeapify(arr, n, i):
    largest = i
    left = 2*i + 1
    right = 2*i + 2
    
    if left < n and arr[left] > arr[largest]:
        largest = left
    
    if right < n and arr[right] > arr[largest]:
        largest = right
    
    if largest != i:
        swap(arr[i], arr[largest])
        maxHeapify(arr, n, largest)
```
- **Time**: O(log n)

**Build Heap**
```
function buildMaxHeap(arr):
    n = length(arr)
    // Start from last non-leaf node
    for i = n//2 - 1 down to 0:
        maxHeapify(arr, n, i)
```
- **Time**: O(n) - not O(n log n)!

**Insert**
```
function insert(heap, key):
    heap.append(key)
    i = length(heap) - 1
    
    // Bubble up
    while i > 0 and heap[parent(i)] < heap[i]:
        swap(heap[i], heap[parent(i)])
        i = parent(i)
```
- **Time**: O(log n)

**Extract Max/Min**
```
function extractMax(heap):
    if length(heap) == 0: return null
    
    max = heap[0]
    heap[0] = heap[-1]
    heap.removeLast()
    
    if length(heap) > 0:
        maxHeapify(heap, length(heap), 0)
    
    return max
```
- **Time**: O(log n)

### Priority Queue Implementation

**Using Heap**
```
class PriorityQueue:
    heap: array to store elements
    
    insert(element, priority):
        heap.append((priority, element))
        bubbleUp(length(heap) - 1)
    
    extractMax():
        if isEmpty(): return null
        max = heap[0]
        heap[0] = heap[-1]
        heap.removeLast()
        bubbleDown(0)
        return max
    
    peek():
        return heap[0] if not isEmpty() else null
```

### Heap Applications

**Heap Sort**
```
function heapSort(arr):
    n = length(arr)
    
    // Build max heap
    buildMaxHeap(arr)
    
    // Extract elements one by one
    for i = n-1 down to 1:
        swap(arr[0], arr[i])
        maxHeapify(arr, i, 0)
```
- **Time**: O(n log n), **Space**: O(1)

**Top K Elements**
```
function findKLargest(arr, k):
    minHeap = new MinHeap()
    
    for element in arr:
        if minHeap.size() < k:
            minHeap.insert(element)
        elif element > minHeap.peek():
            minHeap.extractMin()
            minHeap.insert(element)
    
    return minHeap.toArray()
```

**Merge K Sorted Arrays**
```
function mergeKSortedArrays(arrays):
    minHeap = new MinHeap()
    result = []
    
    // Insert first element of each array
    for i = 0 to k-1:
        if arrays[i].length > 0:
            minHeap.insert((arrays[i][0], i, 0))
    
    while not minHeap.isEmpty():
        value, arrayIndex, elementIndex = minHeap.extractMin()
        result.append(value)
        
        if elementIndex + 1 < arrays[arrayIndex].length:
            nextElement = arrays[arrayIndex][elementIndex + 1]
            minHeap.insert((nextElement, arrayIndex, elementIndex + 1))
    
    return result
```

---

## 11. Graphs {#graphs}

### Understanding Graphs

**Definition**
A graph G = (V, E) consists of:
- **V**: Set of vertices (nodes)
- **E**: Set of edges connecting vertices

**Graph Types**

**Directed vs Undirected**
- **Directed**: Edges have direction (A → B)
- **Undirected**: Edges are bidirectional (A ↔ B)

**Weighted vs Unweighted**
- **Weighted**: Edges have associated weights/costs
- **Unweighted**: All edges have equal weight (usually 1)

**Connected vs Disconnected**
- **Connected**: Path exists between every pair of vertices
- **Disconnected**: Some vertices are unreachable from others

**Cyclic vs Acyclic**
- **Cyclic**: Contains at least one cycle
- **Acyclic**: No cycles (DAG - Directed Acyclic Graph)

### Graph Representation

**Adjacency Matrix**
```
// For graph with V vertices
matrix[V][V] where:
matrix[i][j] = 1 if edge exists from i to j
matrix[i][j] = 0 otherwise

For weighted graphs:
matrix[i][j] = weight of edge from i to j
matrix[i][j] = ∞ if no edge
```

**Advantages**: O(1) edge lookup, simple implementation
**Disadvantages**: O(V²) space, inefficient for sparse graphs

**Adjacency List**
```
// Array of lists
adjList[V] where:
adjList[i] = list of vertices adjacent to vertex i

For weighted graphs:
adjList[i] = list of (vertex, weight) pairs
```

**Advantages**: O(V + E) space, efficient for sparse graphs
**Disadvantages**: O(V) edge lookup in worst case

**Edge List**
```
// List of edges
edges = [(u1, v1), (u2, v2), ..., (uE, vE)]

For weighted graphs:
edges = [(u1, v1, w1), (u2, v2, w2), ...]
```

**Advantages**: Simple, good for algorithms that process all edges
**Disadvantages**: Inefficient for adjacency queries

---

## 12. Graph Traversals {#graph-traversals}

### Depth-First Search (DFS)

**Algorithm**
```
function DFS(graph, startVertex):
    visited = new Set()
    DFSUtil(graph, startVertex, visited)

function DFSUtil(graph, vertex, visited):
    visited.add(vertex)
    print vertex
    
    for each neighbor of vertex:
        if neighbor not in visited:
            DFSUtil(graph, neighbor, visited)
```

**Iterative DFS**
```
function DFSIterative(graph, startVertex):
    visited = new Set()
    stack = new Stack()
    
    stack.push(startVertex)
    
    while not stack.isEmpty():
        vertex = stack.pop()
        
        if vertex not in visited:
            visited.add(vertex)
            print vertex
            
            for each neighbor of vertex:
                if neighbor not in visited:
                    stack.push(neighbor)
```

**DFS Properties**
- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V) for visited set + O(V) for recursion stack
- **Applications**: Topological sorting, cycle detection, pathfinding

### Breadth-First Search (BFS)

**Algorithm**
```
function BFS(graph, startVertex):
    visited = new Set()
    queue = new Queue()
    
    visited.add(startVertex)
    queue.enqueue(startVertex)
    
    while not queue.isEmpty():
        vertex = queue.dequeue()
        print vertex
        
        for each neighbor of vertex:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.enqueue(neighbor)
```

**BFS Properties**
- **Time Complexity**: O(V + E)
- **Space Complexity**: O(V) for visited set + O(V) for queue
- **Applications**: Shortest path in unweighted graphs, level-order traversal

### DFS vs BFS Comparison

| Aspect | DFS | BFS |
|--------|-----|-----|
| Data Structure | Stack (recursion/explicit) | Queue |
| Memory Usage | O(h) where h = depth | O(w) where w = width |
| Path Found | May not be shortest | Shortest in unweighted graphs |
| Implementation | Simpler (recursive) | Requires queue |
| Applications | Topological sort, cycle detection | Shortest path, level traversal |

### Applications of Graph Traversals

**Cycle Detection in Undirected Graph**
```
function hasCycleUndirected(graph):
    visited = new Set()
    
    for each vertex in graph:
        if vertex not in visited:
            if DFSCycleCheck(graph, vertex, -1, visited):
                return true
    return false

function DFSCycleCheck(graph, vertex, parent, visited):
    visited.add(vertex)
    
    for each neighbor of vertex:
        if neighbor not in visited:
            if DFSCycleCheck(graph, neighbor, vertex, visited):
                return true
        elif neighbor != parent:
            return true  // Back edge found
    
    return false
```

**Cycle Detection in Directed Graph**
```
function hasCycleDirected(graph):
    visited = new Set()
    recStack = new Set()
    
    for each vertex in graph:
        if vertex not in visited:
            if DFSCycleCheckDirected(graph, vertex, visited, recStack):
                return true
    return false

function DFSCycleCheckDirected(graph, vertex, visited, recStack):
    visited.add(vertex)
    recStack.add(vertex)
    
    for each neighbor of vertex:
        if neighbor not in visited:
            if DFSCycleCheckDirected(graph, neighbor, visited, recStack):
                return true
        elif neighbor in recStack:
            return true  // Back edge in recursion stack
    
    recStack.remove(vertex)
    return false
```

**Topological Sorting**
```
function topologicalSort(graph):
    visited = new Set()
    stack = new Stack()
    
    for each vertex in graph:
        if vertex not in visited:
            topologicalSortUtil(graph, vertex, visited, stack)
    
    result = []
    while not stack.isEmpty():
        result.append(stack.pop())
    
    return result

function topologicalSortUtil(graph, vertex, visited, stack):
    visited.add(vertex)
    
    for each neighbor of vertex:
        if neighbor not in visited:
            topologicalSortUtil(graph, neighbor, visited, stack)
    
    stack.push(vertex)
```

---

## 13. Connected Components {#connected-components}

### Understanding Connected Components

**Definition**
A connected component is a maximal set of vertices such that there is a path between every pair of vertices in the set.

**Types**
- **Connected Components**: In undirected graphs
- **Strongly Connected Components**: In directed graphs

### Finding Connected Components

**Using DFS**
```
function findConnectedComponents(graph):
    visited = new Set()
    components = []
    
    for each vertex in graph:
        if vertex not in visited:
            component = []
            DFSComponent(graph, vertex, visited, component)
            components.append(component)
    
    return components

function DFSComponent(graph, vertex, visited, component):
    visited.add(vertex)
    component.append(vertex)
    
    for each neighbor of vertex:
        if neighbor not in visited:
            DFSComponent(graph, neighbor, visited, component)
```

**Using BFS**
```
function findConnectedComponentsBFS(graph):
    visited = new Set()
    components = []
    
    for each vertex in graph:
        if vertex not in visited:
            component = []
            queue = new Queue()
            
            visited.add(vertex)
            queue.enqueue(vertex)
            
            while not queue.isEmpty():
                current = queue.dequeue()
                component.append(current)
                
                for each neighbor of current:
                    if neighbor not in visited:
                        visited.add(neighbor)
                        queue.enqueue(neighbor)
            
            components.append(component)
    
    return components
```

### Strongly Connected Components

**Kosaraju's Algorithm**
```
function findSCCs(graph):
    // Step 1: Get finishing times using DFS
    visited = new Set()
    stack = new Stack()
    
    for each vertex in graph:
        if vertex not in visited:
            DFSFinishTime(graph, vertex, visited, stack)
    
    // Step 2: Create transpose graph
    transposeGraph = createTranspose(graph)
    
    // Step 3: DFS on transpose in reverse finishing time order
    visited.clear()
    sccs = []
    
    while not stack.isEmpty():
        vertex = stack.pop()
        if vertex not in visited:
            scc = []
            DFSComponent(transposeGraph, vertex, visited, scc)
            sccs.append(scc)
    
    return sccs

function DFSFinishTime(graph, vertex, visited, stack):
    visited.add(vertex)
    
    for each neighbor of vertex:
        if neighbor not in visited:
            DFSFinishTime(graph, neighbor, visited, stack)
    
    stack.push(vertex)  // Push after visiting all neighbors

function createTranspose(graph):
    transpose = new Graph()
    for each vertex u in graph:
        for each neighbor v of u:
            transpose.addEdge(v, u)  // Reverse edge direction
    return transpose
```

**Tarjan's Algorithm**
```
function tarjanSCC(graph):
    index = 0
    stack = new Stack()
    indices = new Map()
    lowlinks = new Map()
    onStack = new Set()
    sccs = []
    
    for each vertex in graph:
        if vertex not in indices:
            strongConnect(vertex, graph, index, stack, indices, lowlinks, onStack, sccs)
    
    return sccs

function strongConnect(v, graph, index, stack, indices, lowlinks, onStack, sccs):
    indices[v] = index
    lowlinks[v] = index
    index++
    stack.push(v)
    onStack.add(v)
    
    for each neighbor w of v:
        if w not in indices:
            strongConnect(w, graph, index, stack, indices, lowlinks, onStack, sccs)
            lowlinks[v] = min(lowlinks[v], lowlinks[w])
        elif w in onStack:
            lowlinks[v] = min(lowlinks[v], indices[w])
    
    // If v is root of SCC
    if lowlinks[v] == indices[v]:
        scc = []
        repeat:
            w = stack.pop()
            onStack.remove(w)
            scc.append(w)
        until w == v
        sccs.append(scc)
```

### Union-Find (Disjoint Set Union)

**Implementation**
```
class UnionFind:
    parent: array to store parent of each element
    rank: array to store rank of each element
    
    constructor(n):
        parent = [0, 1, 2, ..., n-1]  // Each element is its own parent
        rank = [0, 0, 0, ..., 0]      // All ranks start at 0
    
    find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])  // Path compression
        return parent[x]
    
    union(x, y):
        rootX = find(x)
        rootY = find(y)
        
        if rootX != rootY:
            // Union by rank
            if rank[rootX] < rank[rootY]:
                parent[rootX] = rootY
            elif rank[rootX] > rank[rootY]:
                parent[rootY] = rootX
            else:
                parent[rootY] = rootX
                rank[rootX]++
    
    connected(x, y):
        return find(x) == find(y)
```

**Applications**
- **Connected Components**: In dynamic graphs
- **Kruskal's MST Algorithm**: Cycle detection
- **Network Connectivity**: Check if nodes are connected

---

## 14. Spanning Trees {#spanning-trees}

### Understanding Spanning Trees

**Definition**
A spanning tree of a connected, undirected graph is a subgraph that:
- Includes all vertices
- Is connected
- Has no cycles
- Has exactly V-1 edges (where V = number of vertices)

**Minimum Spanning Tree (MST)**
A spanning tree with minimum total edge weight.

**MST Properties**
- **Unique**: If all edge weights are distinct
- **Cut Property**: Minimum weight edge crossing any cut is in some MST
- **Cycle Property**: Maximum weight edge in any cycle is not in MST

### Kruskal's Algorithm

**Algorithm**
```
function kruskalMST(graph):
    edges = getAllEdges(graph)
    sort(edges) by weight
    
    mst = []
    uf = new UnionFind(V)
    
    for each edge (u, v, weight) in edges:
        if not uf.connected(u, v):
            uf.union(u, v)
            mst.append((u, v, weight))
            
            if length(mst) == V - 1:
                break
    
    return mst
```

**Time Complexity**: O(E log E) for sorting edges
**Space Complexity**: O(V) for Union-Find

**Key Idea**: Greedily add minimum weight edges that don't create cycles

### Prim's Algorithm

**Algorithm**
```
function primMST(graph):
    visited = new Set()
    mst = []
    totalWeight = 0
    
    // Start from vertex 0
    visited.add(0)
    edges = new PriorityQueue()  // Min heap
    
    // Add all edges from vertex 0
    for each neighbor (v, weight) of 0:
        edges.insert((weight, 0, v))
    
    while not edges.isEmpty() and length(visited) < V:
        weight, u, v = edges.extractMin()
        
        if v not in visited:
            visited.add(v)
            mst.append((u, v, weight))
            totalWeight += weight
            
            // Add all edges from newly added vertex
            for each neighbor (w, edgeWeight) of v:
                if w not in visited:
                    edges.insert((edgeWeight, v, w))
    
    return mst, totalWeight
```

**Time Complexity**: O(E log V) with binary heap
**Space Complexity**: O(V + E)

**Key Idea**: Grow MST one vertex at a time, always adding minimum weight edge to unvisited vertex

### Kruskal vs Prim Comparison

| Aspect | Kruskal's | Prim's |
|--------|-----------|--------|
| Approach | Edge-based | Vertex-based |
| Data Structure | Union-Find | Priority Queue |
| Time Complexity | O(E log E) | O(E log V) |
| Space Complexity | O(V) | O(V + E) |
| Best for | Sparse graphs | Dense graphs |
| Implementation | Simpler | More complex |

### Applications of MST

**Network Design**
- **Computer Networks**: Minimum cost to connect all nodes
- **Transportation**: Minimum cost road/railway network
- **Utilities**: Minimum cost power/water distribution

**Clustering**
- **Single-linkage Clustering**: Remove heaviest edges from MST
- **Image Segmentation**: Group similar pixels

**Approximation Algorithms**
- **Traveling Salesman Problem**: MST provides 2-approximation
- **Steiner Tree**: MST is approximation for Steiner tree

---

## 15. Shortest Paths {#shortest-paths}

### Single Source Shortest Path

**Dijkstra's Algorithm**
```
function dijkstra(graph, source):
    dist = array of size V filled with ∞
    visited = new Set()
    pq = new PriorityQueue()  // Min heap
    
    dist[source] = 0
    pq.insert((0, source))
    
    while not pq.isEmpty():
        currentDist, u = pq.extractMin()
        
        if u in visited:
            continue
        
        visited.add(u)
        
        for each neighbor (v, weight) of u:
            if v not in visited:
                newDist = dist[u] + weight
                if newDist < dist[v]:
                    dist[v] = newDist
                    pq.insert((newDist, v))
    
    return dist
```

**Time Complexity**: O((V + E) log V) with binary heap
**Space Complexity**: O(V)
**Limitation**: Cannot handle negative edge weights

**Bellman-Ford Algorithm**
```
function bellmanFord(graph, source):
    dist = array of size V filled with ∞
    dist[source] = 0
    
    // Relax all edges V-1 times
    for i = 1 to V-1:
        for each edge (u, v, weight):
            if dist[u] != ∞ and dist[u] + weight < dist[v]:
                dist[v] = dist[u] + weight
    
    // Check for negative cycles
    for each edge (u, v, weight):
        if dist[u] != ∞ and dist[u] + weight < dist[v]:
            return "Negative cycle detected"
    
    return dist
```

**Time Complexity**: O(VE)
**Space Complexity**: O(V)
**Advantage**: Handles negative edge weights, detects negative cycles

### All Pairs Shortest Path

**Floyd-Warshall Algorithm**
```
function floydWarshall(graph):
    dist = 2D array of size V×V
    
    // Initialize distances
    for i = 0 to V-1:
        for j = 0 to V-1:
            if i == j:
                dist[i][j] = 0
            elif edge exists from i to j:
                dist[i][j] = weight(i, j)
            else:
                dist[i][j] = ∞
    
    // Try all intermediate vertices
    for k = 0 to V-1:
        for i = 0 to V-1:
            for j = 0 to V-1:
                if dist[i][k] + dist[k][j] < dist[i][j]:
                    dist[i][j] = dist[i][k] + dist[k][j]
    
    return dist
```

**Time Complexity**: O(V³)
**Space Complexity**: O(V²)
**Use Case**: Dense graphs, all pairs distances needed

### Shortest Path in Unweighted Graphs

**BFS for Unweighted Graphs**
```
function shortestPathBFS(graph, source):
    dist = array of size V filled with -1
    queue = new Queue()
    
    dist[source] = 0
    queue.enqueue(source)
    
    while not queue.isEmpty():
        u = queue.dequeue()
        
        for each neighbor v of u:
            if dist[v] == -1:
                dist[v] = dist[u] + 1
                queue.enqueue(v)
    
    return dist
```

**Time Complexity**: O(V + E)
**Space Complexity**: O(V)

### Path Reconstruction

**Storing Predecessors**
```
function dijkstraWithPath(graph, source):
    dist = array of size V filled with ∞
    parent = array of size V filled with -1
    visited = new Set()
    pq = new PriorityQueue()
    
    dist[source] = 0
    pq.insert((0, source))
    
    while not pq.isEmpty():
        currentDist, u = pq.extractMin()
        
        if u in visited:
            continue
        
        visited.add(u)
        
        for each neighbor (v, weight) of u:
            if v not in visited:
                newDist = dist[u] + weight
                if newDist < dist[v]:
                    dist[v] = newDist
                    parent[v] = u
                    pq.insert((newDist, v))
    
    return dist, parent

function reconstructPath(parent, source, target):
    path = []
    current = target
    
    while current != -1:
        path.append(current)
        current = parent[current]
    
    path.reverse()
    
    if path[0] == source:
        return path
    else:
        return []  // No path exists
```

### Applications of Shortest Path

**Navigation Systems**
- **GPS Navigation**: Find shortest route between locations
- **Route Planning**: Optimize delivery routes

**Network Routing**
- **Internet Routing**: Find optimal path for data packets
- **Telecommunication**: Route calls through network

**Game AI**
- **Pathfinding**: Move characters optimally
- **Strategy Games**: Plan optimal moves

---

## 16. Hashing {#hashing}

### Understanding Hashing

**Definition**
Hashing is a technique to map data of arbitrary size to fixed-size values using a hash function. The mapped values are stored in a hash table.

**Hash Function Properties**
- **Deterministic**: Same input always produces same output
- **Uniform Distribution**: Distributes keys evenly across hash table
- **Fast Computation**: O(1) time to compute hash value
- **Avalanche Effect**: Small input change causes large output change

### Hash Functions

**Division Method**
```
h(k) = k mod m
where m is table size (preferably prime)
```

**Multiplication Method**
```
h(k) = floor(m * (k * A mod 1))
where A is constant (0 < A < 1), often A = (√5 - 1)/2
```

**Universal Hashing**
```
h(k) = ((a * k + b) mod p) mod m
where p is prime > universe size, a and b are random
```

### Collision Resolution

**Chaining (Open Hashing)**
```
class HashTableChaining:
    table: array of linked lists
    size: number of slots
    
    constructor(size):
        table = array of size empty linked lists
        this.size = size
    
    hash(key):
        return key mod size
    
    insert(key, value):
        index = hash(key)
        table[index].append((key, value))
    
    search(key):
        index = hash(key)
        for (k, v) in table[index]:
            if k == key:
                return v
        return null
    
    delete(key):
        index = hash(key)
        table[index].remove((key, value))
```

**Open Addressing**

**Linear Probing**
```
class HashTableLinearProbing:
    table: array of (key, value) pairs
    size: number of slots
    deleted: array of boolean flags
    
    hash(key, i):
        return (key + i) mod size
    
    insert(key, value):
        i = 0
        while i < size:
            index = hash(key, i)
            if table[index] == null or deleted[index]:
                table[index] = (key, value)
                deleted[index] = false
                return
            i++
        throw "Hash table full"
    
    search(key):
        i = 0
        while i < size:
            index = hash(key, i)
            if table[index] == null:
                return null
            if table[index].key == key and not deleted[index]:
                return table[index].value
            i++
        return null
```

**Quadratic Probing**
```
hash(key, i) = (key + c1*i + c2*i²) mod size
where c1 and c2 are constants
```

**Double Hashing**
```
hash(key, i) = (h1(key) + i * h2(key)) mod size
where h1 and h2 are different hash functions
```

### Load Factor and Rehashing

**Load Factor**
```
α = n/m
where n = number of elements, m = table size
```

**Rehashing**
```
function rehash():
    oldTable = table
    size = size * 2
    table = new array of size null
    
    for each (key, value) in oldTable:
        if (key, value) != null:
            insert(key, value)  // Insert into new table
```

### Hash Table Performance

| Operation | Average Case | Worst Case |
|-----------|--------------|------------|
| Search | O(1) | O(n) |
| Insert | O(1) | O(n) |
| Delete | O(1) | O(n) |

**Factors Affecting Performance**
- **Hash Function Quality**: Good distribution reduces collisions
- **Load Factor**: Higher load factor increases collision probability
- **Collision Resolution**: Chaining vs open addressing trade-offs

### Applications of Hashing

**Database Indexing**
- **Hash Indexes**: Fast equality lookups
- **Join Operations**: Hash joins for database queries

**Caching**
- **Web Caching**: Store frequently accessed web pages
- **CPU Caches**: Map memory addresses to cache lines

**Cryptography**
- **Password Storage**: Store hashed passwords
- **Digital Signatures**: Verify data integrity

**Data Structures**
- **Hash Sets**: Fast membership testing
- **Hash Maps**: Key-value pair storage

### Consistent Hashing

**Problem**: Traditional hashing doesn't handle dynamic systems well

**Solution**: Consistent hashing maps both keys and nodes to points on a circle

```
function consistentHash(key, nodes):
    // Hash key to position on circle
    keyPosition = hash(key) mod CIRCLE_SIZE
    
    // Find first node clockwise from key position
    for each node in sorted(nodes):
        nodePosition = hash(node) mod CIRCLE_SIZE
        if nodePosition >= keyPosition:
            return node
    
    // Wrap around to first node
    return nodes[0]
```

**Applications**
- **Distributed Caching**: Memcached, Redis clusters
- **Load Balancing**: Distribute requests across servers
- **Distributed Databases**: Partition data across nodes

---

## 17. Sorting Algorithms {#sorting}

### Comparison-Based Sorting

**Bubble Sort**
```
function bubbleSort(arr):
    n = length(arr)
    for i = 0 to n-2:
        swapped = false
        for j = 0 to n-2-i:
            if arr[j] > arr[j+1]:
                swap(arr[j], arr[j+1])
                swapped = true
        if not swapped:
            break  // Array is sorted
```
- **Time**: O(n²) worst/average, O(n) best
- **Space**: O(1)
- **Stable**: Yes

**Selection Sort**
```
function selectionSort(arr):
    n = length(arr)
    for i = 0 to n-2:
        minIndex = i
        for j = i+1 to n-1:
            if arr[j] < arr[minIndex]:
                minIndex = j
        swap(arr[i], arr[minIndex])
```
- **Time**: O(n²) all cases
- **Space**: O(1)
- **Stable**: No

**Insertion Sort**
```
function insertionSort(arr):
    for i = 1 to n-1:
        key = arr[i]
        j = i - 1
        while j >= 0 and arr[j] > key:
            arr[j+1] = arr[j]
            j--
        arr[j+1] = key
```
- **Time**: O(n²) worst/average, O(n) best
- **Space**: O(1)
- **Stable**: Yes

**Merge Sort**
```
function mergeSort(arr, left, right):
    if left < right:
        mid = (left + right) / 2
        mergeSort(arr, left, mid)
        mergeSort(arr, mid+1, right)
        merge(arr, left, mid, right)

function merge(arr, left, mid, right):
    leftArr = arr[left...mid]
    rightArr = arr[mid+1...right]
    
    i = j = 0, k = left
    
    while i < length(leftArr) and j < length(rightArr):
        if leftArr[i] <= rightArr[j]:
            arr[k] = leftArr[i]
            i++
        else:
            arr[k] = rightArr[j]
            j++
        k++
    
    // Copy remaining elements
    while i < length(leftArr):
        arr[k++] = leftArr[i++]
    while j < length(rightArr):
        arr[k++] = rightArr[j++]
```
- **Time**: O(n log n) all cases
- **Space**: O(n)
- **Stable**: Yes

**Quick Sort**
```
function quickSort(arr, low, high):
    if low < high:
        pivotIndex = partition(arr, low, high)
        quickSort(arr, low, pivotIndex - 1)
        quickSort(arr, pivotIndex + 1, high)

function partition(arr, low, high):
    pivot = arr[high]
    i = low - 1
    
    for j = low to high-1:
        if arr[j] <= pivot:
            i++
            swap(arr[i], arr[j])
    
    swap(arr[i+1], arr[high])
    return i + 1
```
- **Time**: O(n log n) average, O(n²) worst
- **Space**: O(log n) average, O(n) worst
- **Stable**: No

**Heap Sort**
```
function heapSort(arr):
    n = length(arr)
    
    // Build max heap
    for i = n/2 - 1 down to 0:
        heapify(arr, n, i)
    
    // Extract elements one by one
    for i = n-1 down to 1:
        swap(arr[0], arr[i])
        heapify(arr, i, 0)

function heapify(arr, n, i):
    largest = i
    left = 2*i + 1
    right = 2*i + 2
    
    if left < n and arr[left] > arr[largest]:
        largest = left
    if right < n and arr[right] > arr[largest]:
        largest = right
    
    if largest != i:
        swap(arr[i], arr[largest])
        heapify(arr, n, largest)
```
- **Time**: O(n log n) all cases
- **Space**: O(1)
- **Stable**: No

### Non-Comparison Based Sorting

**Counting Sort**
```
function countingSort(arr, maxValue):
    count = array of size maxValue+1 filled with 0
    output = array of size length(arr)
    
    // Count occurrences
    for element in arr:
        count[element]++
    
    // Calculate cumulative count
    for i = 1 to maxValue:
        count[i] += count[i-1]
    
    // Build output array
    for i = length(arr)-1 down to 0:
        output[count[arr[i]] - 1] = arr[i]
        count[arr[i]]--
    
    return output
```
- **Time**: O(n + k) where k is range of input
- **Space**: O(k)
- **Stable**: Yes

**Radix Sort**
```
function radixSort(arr):
    maxValue = max(arr)
    
    exp = 1
    while maxValue / exp > 0:
        countingSortByDigit(arr, exp)
        exp *= 10

function countingSortByDigit(arr, exp):
    count = array of size 10 filled with 0
    output = array of size length(arr)
    
    // Count occurrences of digits
    for element in arr:
        digit = (element / exp) mod 10
        count[digit]++
    
    // Calculate cumulative count
    for i = 1 to 9:
        count[i] += count[i-1]
    
    // Build output array
    for i = length(arr)-1 down to 0:
        digit = (arr[i] / exp) mod 10
        output[count[digit] - 1] = arr[i]
        count[digit]--
    
    // Copy back to original array
    for i = 0 to length(arr)-1:
        arr[i] = output[i]
```
- **Time**: O(d × (n + k)) where d is number of digits
- **Space**: O(n + k)
- **Stable**: Yes

**Bucket Sort**
```
function bucketSort(arr):
    n = length(arr)
    buckets = array of n empty lists
    
    // Put elements into buckets
    for element in arr:
        bucketIndex = floor(n * element)  // Assuming elements in [0, 1)
        buckets[bucketIndex].append(element)
    
    // Sort individual buckets
    for bucket in buckets:
        insertionSort(bucket)
    
    // Concatenate buckets
    result = []
    for bucket in buckets:
        result.extend(bucket)
    
    return result
```
- **Time**: O(n + k) average, O(n²) worst
- **Space**: O(n + k)
- **Stable**: Yes

### Sorting Algorithm Comparison

| Algorithm | Best | Average | Worst | Space | Stable |
|-----------|------|---------|-------|-------|--------|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) | No |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) | No |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) | No |
| Counting Sort | O(n + k) | O(n + k) | O(n + k) | O(k) | Yes |
| Radix Sort | O(d(n + k)) | O(d(n + k)) | O(d(n + k)) | O(n + k) | Yes |
| Bucket Sort | O(n + k) | O(n + k) | O(n²) | O(n + k) | Yes |

### When to Use Which Sorting Algorithm

**Small Arrays (n < 50)**
- **Insertion Sort**: Simple, efficient for small datasets

**Nearly Sorted Arrays**
- **Insertion Sort**: O(n) for nearly sorted data
- **Bubble Sort**: With early termination

**Guaranteed O(n log n)**
- **Merge Sort**: Stable, predictable performance
- **Heap Sort**: In-place, guaranteed performance

**Average Case Performance**
- **Quick Sort**: Fastest average case, cache-friendly

**Stable Sorting Required**
- **Merge Sort**: Guaranteed stable
- **Counting Sort**: For integer data

**Memory Constrained**
- **Heap Sort**: O(1) extra space
- **Quick Sort**: O(log n) average space

**Integer Data with Small Range**
- **Counting Sort**: Linear time
- **Radix Sort**: For larger ranges

---

## 18. Searching Algorithms {#searching}

### Linear Search

**Algorithm**
```
function linearSearch(arr, target):
    for i = 0 to length(arr)-1:
        if arr[i] == target:
            return i
    return -1
```
- **Time**: O(n)
- **Space**: O(1)
- **Use**: Unsorted arrays, small datasets

### Binary Search

**Iterative Implementation**
```
function binarySearch(arr, target):
    left = 0
    right = length(arr) - 1
    
    while left <= right:
        mid = left + (right - left) / 2
        
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return -1
```

**Recursive Implementation**
```
function binarySearchRecursive(arr, target, left, right):
    if left > right:
        return -1
    
    mid = left + (right - left) / 2
    
    if arr[mid] == target:
        return mid
    elif arr[mid] < target:
        return binarySearchRecursive(arr, target, mid + 1, right)
    else:
        return binarySearchRecursive(arr, target, left, mid - 1)
```

- **Time**: O(log n)
- **Space**: O(1) iterative, O(log n) recursive
- **Prerequisite**: Array must be sorted

### Binary Search Variations

**Find First Occurrence**
```
function findFirst(arr, target):
    left = 0, right = length(arr) - 1
    result = -1
    
    while left <= right:
        mid = left + (right - left) / 2
        
        if arr[mid] == target:
            result = mid
            right = mid - 1  // Continue searching left
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return result
```

**Find Last Occurrence**
```
function findLast(arr, target):
    left = 0, right = length(arr) - 1
    result = -1
    
    while left <= right:
        mid = left + (right - left) / 2
        
        if arr[mid] == target:
            result = mid
            left = mid + 1  // Continue searching right
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return result
```

**Find Peak Element**
```
function findPeak(arr):
    left = 0, right = length(arr) - 1
    
    while left < right:
        mid = left + (right - left) / 2
        
        if arr[mid] < arr[mid + 1]:
            left = mid + 1
        else:
            right = mid
    
    return left
```

**Search in Rotated Sorted Array**
```
function searchRotated(arr, target):
    left = 0, right = length(arr) - 1
    
    while left <= right:
        mid = left + (right - left) / 2
        
        if arr[mid] == target:
            return mid
        
        // Left half is sorted
        if arr[left] <= arr[mid]:
            if arr[left] <= target < arr[mid]:
                right = mid - 1
            else:
                left = mid + 1
        // Right half is sorted
        else:
            if arr[mid] < target <= arr[right]:
                left = mid + 1
            else:
                right = mid - 1
    
    return -1
```

### Ternary Search

**Algorithm**
```
function ternarySearch(arr, target, left, right):
    if left > right:
        return -1
    
    mid1 = left + (right - left) / 3
    mid2 = right - (right - left) / 3
    
    if arr[mid1] == target:
        return mid1
    if arr[mid2] == target:
        return mid2
    
    if target < arr[mid1]:
        return ternarySearch(arr, target, left, mid1 - 1)
    elif target > arr[mid2]:
        return ternarySearch(arr, target, mid2 + 1, right)
    else:
        return ternarySearch(arr, target, mid1 + 1, mid2 - 1)
```

- **Time**: O(log₃ n) ≈ O(log n)
- **Comparisons**: More comparisons per iteration than binary search
- **Use**: Rarely used in practice

### Exponential Search

**Algorithm**
```
function exponentialSearch(arr, target):
    if arr[0] == target:
        return 0
    
    // Find range for binary search
    i = 1
    while i < length(arr) and arr[i] <= target:
        i *= 2
    
    // Binary search in found range
    return binarySearch(arr, target, i/2, min(i, length(arr)-1))
```

- **Time**: O(log n)
- **Use**: Unbounded/infinite arrays

### Interpolation Search

**Algorithm**
```
function interpolationSearch(arr, target):
    left = 0, right = length(arr) - 1
    
    while left <= right and target >= arr[left] and target <= arr[right]:
        if left == right:
            return arr[left] == target ? left : -1
        
        // Interpolation formula
        pos = left + ((target - arr[left]) * (right - left)) / (arr[right] - arr[left])
        
        if arr[pos] == target:
            return pos
        elif arr[pos] < target:
            left = pos + 1
        else:
            right = pos - 1
    
    return -1
```

- **Time**: O(log log n) for uniformly distributed data, O(n) worst case
- **Use**: Uniformly distributed sorted arrays

### Jump Search

**Algorithm**
```
function jumpSearch(arr, target):
    n = length(arr)
    step = sqrt(n)
    prev = 0
    
    // Find block containing target
    while arr[min(step, n) - 1] < target:
        prev = step
        step += sqrt(n)
        if prev >= n:
            return -1
    
    // Linear search in block
    while arr[prev] < target:
        prev++
        if prev == min(step, n):
            return -1
    
    if arr[prev] == target:
        return prev
    
    return -1
```

- **Time**: O(√n)
- **Use**: When binary search is not feasible

### Search Algorithm Comparison

| Algorithm | Time Complexity | Space | Prerequisites |
|-----------|----------------|-------|---------------|
| Linear Search | O(n) | O(1) | None |
| Binary Search | O(log n) | O(1) | Sorted array |
| Ternary Search | O(log n) | O(1) | Sorted array |
| Exponential Search | O(log n) | O(1) | Sorted array |
| Interpolation Search | O(log log n) avg | O(1) | Uniformly distributed |
| Jump Search | O(√n) | O(1) | Sorted array |

### Applications of Searching

**Database Systems**
- **Index Searching**: B-tree, hash indexes
- **Query Optimization**: Choose optimal search strategy

**Information Retrieval**
- **Web Search**: Search engines use sophisticated algorithms
- **Document Search**: Full-text search in documents

**Game Development**
- **Pathfinding**: Search for optimal paths
- **AI Decision Making**: Search game trees

**System Software**
- **Memory Management**: Search for free memory blocks
- **File Systems**: Search for files and directories

This completes Part 2 of the comprehensive DSA guide, covering advanced data structures, graph algorithms, and fundamental algorithmic techniques. The guide now provides complete coverage of essential DSA topics for freshers and MCQ exam preparation.