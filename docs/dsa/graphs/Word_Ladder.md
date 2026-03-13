# Word Ladder
**LeetCode 127** | Hard | BFS

## Problem
Given `beginWord`, `endWord`, and a `wordList`, find the length of the shortest transformation sequence
where each step changes exactly one letter and each intermediate word must be in `wordList`.

```
Input: beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log","cog"]
Output: 5  →  "hit" → "hot" → "dot" → "dog" → "cog"

Input: beginWord = "hit", endWord = "cog", wordList = ["hot","dot","dog","lot","log"]
Output: 0  (endWord not in wordList)
```

## Approach 1: BFS (Standard)
**Time:** O(M²×N) where M=word length, N=wordList size | **Space:** O(M²×N)

```java
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;

    Queue<String> q = new LinkedList<>();
    q.offer(beginWord);
    wordSet.remove(beginWord);
    int steps = 1;

    while (!q.isEmpty()) {
        int size = q.size();
        while (size-- > 0) {
            String word = q.poll();
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char orig = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    chars[i] = c;
                    String next = new String(chars);
                    if (next.equals(endWord)) return steps + 1;
                    if (wordSet.contains(next)) { wordSet.remove(next); q.offer(next); }
                }
                chars[i] = orig;
            }
        }
        steps++;
    }
    return 0;
}
```

## Approach 2: Bidirectional BFS (Faster in Practice)
**Time:** O(M²×N) but explores far fewer nodes | **Space:** O(M²×N)

```java
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;

    Set<String> beginSet = new HashSet<>(), endSet = new HashSet<>();
    beginSet.add(beginWord); endSet.add(endWord);
    int steps = 1;

    while (!beginSet.isEmpty() && !endSet.isEmpty()) {
        if (beginSet.size() > endSet.size()) { Set<String> t = beginSet; beginSet = endSet; endSet = t; }
        Set<String> nextSet = new HashSet<>();
        for (String word : beginSet) {
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char orig = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    chars[i] = c;
                    String next = new String(chars);
                    if (endSet.contains(next)) return steps + 1;
                    if (wordSet.contains(next)) { wordSet.remove(next); nextSet.add(next); }
                }
                chars[i] = orig;
            }
        }
        beginSet = nextSet;
        steps++;
    }
    return 0;
}
```

## Approach 3: BFS with Pattern Preprocessing
Pre-build adjacency using wildcard patterns like `*ot`, `h*t`.

**Time:** O(M²×N) | **Space:** O(M²×N)

```java
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Map<String, List<String>> patternMap = new HashMap<>();
    wordList.forEach(word -> {
        for (int i = 0; i < word.length(); i++) {
            String pattern = word.substring(0, i) + "*" + word.substring(i+1);
            patternMap.computeIfAbsent(pattern, k -> new ArrayList<>()).add(word);
        }
    });

    Queue<String> q = new LinkedList<>();
    Set<String> visited = new HashSet<>();
    q.offer(beginWord); visited.add(beginWord);
    int steps = 1;

    while (!q.isEmpty()) {
        int size = q.size();
        while (size-- > 0) {
            String word = q.poll();
            for (int i = 0; i < word.length(); i++) {
                String pattern = word.substring(0, i) + "*" + word.substring(i+1);
                for (String next : patternMap.getOrDefault(pattern, new ArrayList<>())) {
                    if (next.equals(endWord)) return steps + 1;
                    if (!visited.contains(next)) { visited.add(next); q.offer(next); }
                }
            }
        }
        steps++;
    }
    return 0;
}
```

## Key Insight
BFS guarantees shortest path. Try all 26 letter substitutions at each position — this is the implicit graph edge.
Bidirectional BFS reduces search space from O(b^d) to O(b^(d/2)) where b=branching factor, d=depth.
