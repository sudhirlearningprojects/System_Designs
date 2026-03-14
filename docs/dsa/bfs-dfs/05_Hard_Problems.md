# BFS/DFS - Hard Problems (10%)

## 📚 2 Hard Problems with Complete Solutions

---

## Problem 1: Word Ladder II

**Difficulty**: Hard  
**Pattern**: BFS + DFS (Bidirectional BFS + Backtracking)  
**LeetCode**: #126

### Problem Statement

Given two words `beginWord` and `endWord`, and a dictionary `wordList`, find all shortest transformation sequences from `beginWord` to `endWord`. Each transformed word must exist in the word list, and only one letter can be changed at a time.

### Examples

```
Input: 
beginWord = "hit"
endWord = "cog"
wordList = ["hot","dot","dog","lot","log","cog"]

Output: [
  ["hit","hot","dot","dog","cog"],
  ["hit","hot","lot","log","cog"]
]

Input:
beginWord = "hit"
endWord = "cog"
wordList = ["hot","dot","dog","lot","log"]

Output: []
Explanation: endWord "cog" is not in wordList
```

### Intuition

**Challenge**: Find ALL shortest paths, not just one.

**Approach**:
1. **BFS**: Build graph of transformations and find shortest distance
2. **DFS**: Backtrack to find all paths of shortest length

**Why not just BFS?**
- BFS finds shortest distance but not all paths
- Need to track parent relationships
- Use DFS to reconstruct all paths

### Solution

```java
public class WordLadderII {
    public List<List<String>> findLadders(String beginWord, String endWord, 
                                          List<String> wordList) {
        List<List<String>> result = new ArrayList<>();
        Set<String> dict = new HashSet<>(wordList);
        
        if (!dict.contains(endWord)) {
            return result;
        }
        
        // BFS to build graph and find shortest distance
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> distance = new HashMap<>();
        
        bfs(beginWord, endWord, dict, graph, distance);
        
        // DFS to find all shortest paths
        List<String> path = new ArrayList<>();
        path.add(beginWord);
        dfs(beginWord, endWord, graph, distance, path, result);
        
        return result;
    }
    
    private void bfs(String beginWord, String endWord, Set<String> dict,
                     Map<String, List<String>> graph, 
                     Map<String, Integer> distance) {
        for (String word : dict) {
            graph.put(word, new ArrayList<>());
        }
        graph.put(beginWord, new ArrayList<>());
        
        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);
        distance.put(beginWord, 0);
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            boolean foundEnd = false;
            
            for (int i = 0; i < size; i++) {
                String word = queue.poll();
                int currDist = distance.get(word);
                List<String> neighbors = getNeighbors(word, dict);
                
                for (String neighbor : neighbors) {
                    graph.get(word).add(neighbor);
                    
                    if (!distance.containsKey(neighbor)) {
                        distance.put(neighbor, currDist + 1);
                        
                        if (neighbor.equals(endWord)) {
                            foundEnd = true;
                        } else {
                            queue.offer(neighbor);
                        }
                    }
                }
            }
            
            if (foundEnd) break;
        }
    }
    
    private void dfs(String word, String endWord, 
                     Map<String, List<String>> graph,
                     Map<String, Integer> distance,
                     List<String> path, List<List<String>> result) {
        if (word.equals(endWord)) {
            result.add(new ArrayList<>(path));
            return;
        }
        
        for (String neighbor : graph.get(word)) {
            if (distance.get(neighbor) == distance.get(word) + 1) {
                path.add(neighbor);
                dfs(neighbor, endWord, graph, distance, path, result);
                path.remove(path.size() - 1);
            }
        }
    }
    
    private List<String> getNeighbors(String word, Set<String> dict) {
        List<String> neighbors = new ArrayList<>();
        char[] chars = word.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            char old = chars[i];
            
            for (char c = 'a'; c <= 'z'; c++) {
                if (c == old) continue;
                
                chars[i] = c;
                String newWord = new String(chars);
                
                if (dict.contains(newWord)) {
                    neighbors.add(newWord);
                }
            }
            
            chars[i] = old;
        }
        
        return neighbors;
    }
}
```

### Dry Run

**Input**: `beginWord = "hit"`, `endWord = "cog"`, `wordList = ["hot","dot","dog","lot","log","cog"]`

```
=== BFS Phase ===

Level 0: ["hit"]
  distance = {hit: 0}
  
  Neighbors of "hit": ["hot"]
  graph = {hit: [hot]}
  distance = {hit: 0, hot: 1}
  queue = [hot]

Level 1: ["hot"]
  Neighbors of "hot": ["dot", "lot"]
  graph = {hit: [hot], hot: [dot, lot]}
  distance = {hit: 0, hot: 1, dot: 2, lot: 2}
  queue = [dot, lot]

Level 2: ["dot", "lot"]
  Neighbors of "dot": ["dog"]
  Neighbors of "lot": ["log"]
  graph = {hit: [hot], hot: [dot, lot], dot: [dog], lot: [log]}
  distance = {hit: 0, hot: 1, dot: 2, lot: 2, dog: 3, log: 3}
  queue = [dog, log]

Level 3: ["dog", "log"]
  Neighbors of "dog": ["cog"]
  Neighbors of "log": ["cog"]
  graph = {hit: [hot], hot: [dot, lot], dot: [dog], lot: [log], 
           dog: [cog], log: [cog]}
  distance = {hit: 0, hot: 1, dot: 2, lot: 2, dog: 3, log: 3, cog: 4}
  Found endWord! Stop BFS

=== DFS Phase ===

Start: path = [hit]

dfs(hit):
  neighbor = hot, distance[hot] = 1 = distance[hit] + 1 ✓
  path = [hit, hot]
  
  dfs(hot):
    neighbor = dot, distance[dot] = 2 = distance[hot] + 1 ✓
    path = [hit, hot, dot]
    
    dfs(dot):
      neighbor = dog, distance[dog] = 3 = distance[dot] + 1 ✓
      path = [hit, hot, dot, dog]
      
      dfs(dog):
        neighbor = cog, distance[cog] = 4 = distance[dog] + 1 ✓
        path = [hit, hot, dot, dog, cog]
        
        dfs(cog):
          word == endWord, add to result
          result = [[hit, hot, dot, dog, cog]]
        
        backtrack: path = [hit, hot, dot, dog]
      
      backtrack: path = [hit, hot, dot]
    
    backtrack: path = [hit, hot]
    
    neighbor = lot, distance[lot] = 2 = distance[hot] + 1 ✓
    path = [hit, hot, lot]
    
    dfs(lot):
      neighbor = log, distance[log] = 3 = distance[lot] + 1 ✓
      path = [hit, hot, lot, log]
      
      dfs(log):
        neighbor = cog, distance[cog] = 4 = distance[log] + 1 ✓
        path = [hit, hot, lot, log, cog]
        
        dfs(cog):
          word == endWord, add to result
          result = [[hit, hot, dot, dog, cog], [hit, hot, lot, log, cog]]

Final result = [
  [hit, hot, dot, dog, cog],
  [hit, hot, lot, log, cog]
]
```

### Algorithm Explanation

**Phase 1: BFS (Build Graph)**
1. Use BFS to explore all words level by level
2. Build adjacency list (graph) of valid transformations
3. Track distance from beginWord to each word
4. Stop when endWord is reached

**Phase 2: DFS (Find All Paths)**
1. Start from beginWord
2. Only follow edges where distance increases by 1
3. This ensures we only follow shortest paths
4. Backtrack to find all such paths

### Why This Works

- **BFS ensures shortest distance**: Level-by-level exploration
- **Graph stores all valid transformations**: No need to recompute
- **Distance map filters paths**: Only follow shortest paths in DFS
- **DFS finds all paths**: Backtracking explores all possibilities

### Complexity Analysis

- **Time Complexity**: O(N × L² × 26) where N = words, L = word length
  - BFS: O(N × L² × 26) to find neighbors
  - DFS: O(N) in worst case
  
- **Space Complexity**: O(N × L)
  - Graph: O(N × L)
  - Distance map: O(N)
  - Recursion: O(L)

### Test Cases

```java
@Test
public void testFindLadders() {
    WordLadderII solution = new WordLadderII();
    
    // Test case 1: Multiple shortest paths
    List<String> wordList1 = Arrays.asList("hot","dot","dog","lot","log","cog");
    List<List<String>> result1 = solution.findLadders("hit", "cog", wordList1);
    assertEquals(2, result1.size());
    
    // Test case 2: No path exists
    List<String> wordList2 = Arrays.asList("hot","dot","dog","lot","log");
    List<List<String>> result2 = solution.findLadders("hit", "cog", wordList2);
    assertEquals(0, result2.size());
    
    // Test case 3: Single path
    List<String> wordList3 = Arrays.asList("hot","dot","dog","cog");
    List<List<String>> result3 = solution.findLadders("hit", "cog", wordList3);
    assertEquals(1, result3.size());
}
```

### Edge Cases

1. **endWord not in dictionary**: Return empty list
2. **No transformation possible**: Return empty list
3. **beginWord == endWord**: Return [[beginWord]]
4. **Single word in dictionary**: Check if it's the endWord

### Common Mistakes

1. **Using only BFS**: Won't find all paths
2. **Using only DFS**: Won't guarantee shortest paths
3. **Not tracking distance**: May include longer paths
4. **Not stopping BFS early**: Wastes time exploring beyond shortest distance

---

## Problem 2: Serialize and Deserialize Binary Tree

**Difficulty**: Hard  
**Pattern**: BFS/DFS  
**LeetCode**: #297

### Problem Statement

Design an algorithm to serialize and deserialize a binary tree. Serialization is converting a tree to a string, and deserialization is reconstructing the tree from the string.

### Examples

```
Input: root = [1,2,3,null,null,4,5]
      1
     / \
    2   3
       / \
      4   5

Serialized: "1,2,null,null,3,4,null,null,5,null,null"
Deserialized: Same tree structure
```

### Solution (DFS - Preorder)

```java
public class Codec {
    // Encodes a tree to a single string
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
    
    // Decodes your encoded data to tree
    public TreeNode deserialize(String data) {
        Queue<String> queue = new LinkedList<>(Arrays.asList(data.split(",")));
        return deserializeHelper(queue);
    }
    
    private TreeNode deserializeHelper(Queue<String> queue) {
        String val = queue.poll();
        
        if (val.equals("null")) {
            return null;
        }
        
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper(queue);
        node.right = deserializeHelper(queue);
        
        return node;
    }
}
```

### Solution (BFS - Level Order)

```java
public class CodecBFS {
    // Encodes a tree to a single string
    public String serialize(TreeNode root) {
        if (root == null) return "";
        
        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            
            if (node == null) {
                sb.append("null,");
            } else {
                sb.append(node.val).append(",");
                queue.offer(node.left);
                queue.offer(node.right);
            }
        }
        
        return sb.toString();
    }
    
    // Decodes your encoded data to tree
    public TreeNode deserialize(String data) {
        if (data.isEmpty()) return null;
        
        String[] values = data.split(",");
        TreeNode root = new TreeNode(Integer.parseInt(values[0]));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            
            if (!values[i].equals("null")) {
                node.left = new TreeNode(Integer.parseInt(values[i]));
                queue.offer(node.left);
            }
            i++;
            
            if (!values[i].equals("null")) {
                node.right = new TreeNode(Integer.parseInt(values[i]));
                queue.offer(node.right);
            }
            i++;
        }
        
        return root;
    }
}
```

### Dry Run (DFS Preorder)

**Input Tree**:
```
    1
   / \
  2   3
     / \
    4   5
```

**Serialization**:
```
serialize(1):
  sb = "1,"
  serialize(2):
    sb = "1,2,"
    serialize(null): sb = "1,2,null,"
    serialize(null): sb = "1,2,null,null,"
  serialize(3):
    sb = "1,2,null,null,3,"
    serialize(4):
      sb = "1,2,null,null,3,4,"
      serialize(null): sb = "1,2,null,null,3,4,null,"
      serialize(null): sb = "1,2,null,null,3,4,null,null,"
    serialize(5):
      sb = "1,2,null,null,3,4,null,null,5,"
      serialize(null): sb = "1,2,null,null,3,4,null,null,5,null,"
      serialize(null): sb = "1,2,null,null,3,4,null,null,5,null,null,"

Result: "1,2,null,null,3,4,null,null,5,null,null,"
```

**Deserialization**:
```
queue = [1, 2, null, null, 3, 4, null, null, 5, null, null]

deserialize():
  val = "1", create node(1)
  node.left = deserialize():
    val = "2", create node(2)
    node.left = deserialize():
      val = "null", return null
    node.right = deserialize():
      val = "null", return null
    return node(2)
  node.right = deserialize():
    val = "3", create node(3)
    node.left = deserialize():
      val = "4", create node(4)
      node.left = deserialize():
        val = "null", return null
      node.right = deserialize():
        val = "null", return null
      return node(4)
    node.right = deserialize():
      val = "5", create node(5)
      node.left = deserialize():
        val = "null", return null
      node.right = deserialize():
        val = "null", return null
      return node(5)
    return node(3)
  return node(1)

Reconstructed tree:
    1
   / \
  2   3
     / \
    4   5
```

### Complexity Analysis

**DFS (Preorder)**:
- **Time Complexity**: O(n) for both serialize and deserialize
- **Space Complexity**: O(n) for string + O(h) for recursion

**BFS (Level Order)**:
- **Time Complexity**: O(n) for both serialize and deserialize
- **Space Complexity**: O(n) for string + O(w) for queue

### Test Cases

```java
@Test
public void testCodec() {
    Codec codec = new Codec();
    
    // Test case 1: Normal tree
    TreeNode root1 = new TreeNode(1);
    root1.left = new TreeNode(2);
    root1.right = new TreeNode(3);
    root1.right.left = new TreeNode(4);
    root1.right.right = new TreeNode(5);
    
    String serialized1 = codec.serialize(root1);
    TreeNode deserialized1 = codec.deserialize(serialized1);
    assertEquals(serialized1, codec.serialize(deserialized1));
    
    // Test case 2: Empty tree
    assertNull(codec.deserialize(codec.serialize(null)));
    
    // Test case 3: Single node
    TreeNode root3 = new TreeNode(1);
    String serialized3 = codec.serialize(root3);
    TreeNode deserialized3 = codec.deserialize(serialized3);
    assertEquals(1, deserialized3.val);
}
```

### Edge Cases

1. **Empty tree**: Return empty string or "null"
2. **Single node**: Handle correctly
3. **Skewed tree**: Left or right only
4. **Large values**: Handle negative numbers

### Common Mistakes

1. **Not handling null nodes**: Tree structure lost
2. **Wrong delimiter**: Parsing errors
3. **Not using queue in deserialization**: Order mismatch
4. **Forgetting to handle empty string**: NullPointerException

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Word Ladder II | BFS + DFS | O(N×L²×26) | O(N×L) | Build graph with BFS, find paths with DFS |
| Serialize/Deserialize Tree | DFS/BFS | O(n) | O(n) | Preorder or level-order traversal |

---

## 🎓 Key Takeaways

### Hard Problem Characteristics

1. **Combination of Techniques**: BFS + DFS, multiple passes
2. **Complex State Management**: Track multiple data structures
3. **Optimization Required**: Bidirectional search, early termination
4. **Edge Cases**: Many corner cases to handle

### Problem-Solving Strategies

1. **Break Down**: Solve in phases (BFS then DFS)
2. **Choose Right Tool**: BFS for shortest, DFS for all paths
3. **Track State**: Use maps to store relationships
4. **Optimize**: Stop early when possible

---

**Congratulations!** You've completed all 20 BFS/DFS problems! 🎉

**Next Steps**:
1. Review problems you found difficult
2. Practice variations (bidirectional BFS, iterative DFS)
3. Combine with other techniques (DP, Binary Search)
4. Time yourself on random problems

**Related Topics**:
- Topological Sort
- Dijkstra's Algorithm
- A* Search
- Backtracking
- Dynamic Programming on Trees/Graphs
