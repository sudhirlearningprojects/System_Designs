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
