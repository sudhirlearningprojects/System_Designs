# Word Ladder II
**LeetCode 126** | Hard | BFS + DFS Backtracking

## Problem
Find ALL shortest transformation sequences from `beginWord` to `endWord` using words from `wordList`.
Each step changes exactly one letter, and each intermediate word must be in `wordList`.

```
Input: beginWord = "hit", endWord = "cog",
       wordList = ["hot","dot","dog","lot","log","cog"]
Output: [["hit","hot","dot","dog","cog"],["hit","hot","lot","log","cog"]]

Input: beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log"]
Output: []
```

## Approach: BFS for Levels + DFS for Path Reconstruction
BFS builds a layer-by-layer parent map. DFS reconstructs all shortest paths.

**Time:** O(M²×N + K) where M=word length, N=wordList size, K=total chars in output | **Space:** O(M²×N)

```java
public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    List<List<String>> result = new ArrayList<>();
    if (!wordSet.contains(endWord)) return result;

    // BFS: build parent map (word → set of parents in shortest path)
    Map<String, List<String>> parents = new HashMap<>();
    Map<String, Integer> level = new HashMap<>();
    level.put(beginWord, 0);
    Queue<String> q = new LinkedList<>();
    q.offer(beginWord);
    boolean found = false;

    while (!q.isEmpty() && !found) {
        int size = q.size();
        Set<String> levelVisited = new HashSet<>();
        while (size-- > 0) {
            String word = q.poll();
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char orig = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    chars[i] = c;
                    String next = new String(chars);
                    if (!wordSet.contains(next)) { chars[i] = orig; continue; }
                    if (next.equals(endWord)) found = true;
                    // Only add if not visited at an earlier level
                    if (!level.containsKey(next) || level.get(next) == level.get(word) + 1) {
                        parents.computeIfAbsent(next, k -> new ArrayList<>()).add(word);
                        if (!level.containsKey(next)) {
                            level.put(next, level.get(word) + 1);
                            levelVisited.add(next);
                            q.offer(next);
                        }
                    }
                }
                chars[i] = orig;
            }
        }
        wordSet.removeAll(levelVisited); // prevent revisiting at deeper levels
    }

    // DFS: reconstruct all paths from endWord back to beginWord
    if (found) {
        List<String> path = new ArrayList<>();
        path.add(endWord);
        dfs(parents, beginWord, endWord, path, result);
    }
    return result;
}

private void dfs(Map<String, List<String>> parents, String beginWord, String word,
                 List<String> path, List<List<String>> result) {
    if (word.equals(beginWord)) {
        List<String> copy = new ArrayList<>(path);
        Collections.reverse(copy);
        result.add(copy);
        return;
    }
    for (String parent : parents.getOrDefault(word, new ArrayList<>())) {
        path.add(parent);
        dfs(parents, beginWord, parent, path, result);
        path.remove(path.size() - 1);
    }
}
```

## Key Insight
Two-phase approach:
1. **BFS phase**: Find the shortest path length and build a DAG of parent pointers (only shortest-path edges)
2. **DFS phase**: Traverse the DAG backwards from `endWord` to `beginWord` to collect all paths

The trick of removing `levelVisited` words from `wordSet` after each BFS level ensures we only record
shortest-path edges and don't revisit nodes at deeper levels (which would give non-shortest paths).

```
DAG built by BFS:
hit → hot → dot → dog → cog
              ↘         ↗
               lot → log
```

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `endWord` not in wordList | `[]` | No path possible |
| No transformation exists | `[]` | Graph disconnected |
| Only one shortest path | Single path in list | DFS finds one path |
| `beginWord == endWord` | Not in constraints | Problem guarantees they differ |
| Multiple paths of same length | All returned | DFS backtracks to find all |
| Very large wordList | TLE risk | Use pattern preprocessing |

---

## Dry Run

**Input:** `beginWord="hit"`, `endWord="cog"`, `wordList=["hot","dot","dog","lot","log","cog"]`

**BFS phase — building parent DAG:**
```
level: {hit:0}
Queue: [hit]

--- Level 0 (processing "hit") ---
Neighbors of "hit": "hot" (in wordSet)
  parents[hot]=[hit], level[hot]=1, levelVisited={hot}
wordSet after: {dot,dog,lot,log,cog}  (hot removed)

--- Level 1 (processing "hot") ---
Neighbors of "hot": "dot","lot" (in wordSet)
  parents[dot]=[hot], level[dot]=2
  parents[lot]=[hot], level[lot]=2
  levelVisited={dot,lot}
wordSet after: {dog,log,cog}

--- Level 2 (processing "dot","lot") ---
Neighbors of "dot": "dog" → parents[dog]=[dot], level[dog]=3
Neighbors of "lot": "log" → parents[log]=[lot], level[log]=3
levelVisited={dog,log}
wordSet after: {cog}

--- Level 3 (processing "dog","log") ---
Neighbors of "dog": "cog" == endWord! found=true
  parents[cog]=[dog]
Neighbors of "log": "cog" → level[cog]=4 == level[log]+1=4 → parents[cog]=[dog,log]
```

**DFS phase — reconstruct paths from "cog" back to "hit":**
```
path=[cog]
  parent=dog: path=[cog,dog]
    parent=dot: path=[cog,dog,dot]
      parent=hot: path=[cog,dog,dot,hot]
        parent=hit: → reverse → [hit,hot,dot,dog,cog] ✓
  parent=log: path=[cog,log]
    parent=lot: path=[cog,log,lot]
      parent=hot: path=[cog,log,lot,hot]
        parent=hit: → reverse → [hit,hot,lot,log,cog] ✓

Answer: [["hit","hot","dot","dog","cog"],["hit","hot","lot","log","cog"]]
```

---

## Follow-up Questions

**Q: Why is this problem harder than Word Ladder I (LC 127)?**
LC 127 only needs the length. LC 126 needs all paths, requiring the parent DAG and DFS reconstruction. The tricky part is correctly building the DAG without including non-shortest-path edges.

**Q: Why remove `levelVisited` from `wordSet` after each level?**
If a word is reachable at level k, we don't want to add it as a parent at level k+1 (that would be a longer path). Removing it from `wordSet` prevents this.

**Q: What's the time complexity of the DFS phase?**
O(K) where K = total characters in all output paths. In the worst case, exponentially many paths exist.

**Q: Can you use bidirectional BFS for LC 126?**
Yes, but it's significantly more complex to implement correctly for all-paths reconstruction.

**Related Problems:** LC 127 (Word Ladder), LC 433 (Minimum Genetic Mutation), LC 210 (Course Schedule II)
