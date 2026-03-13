# Alien Dictionary
**LeetCode 269** | Hard | Topological Sort

## Problem
Given a sorted list of words in an alien language, derive the order of characters in that alphabet.
Return any valid ordering, or `""` if invalid (cycle detected).

```
Input: words = ["wrt","wrf","er","ett","rftt"]
Output: "wertf"

Input: words = ["z","x"]
Output: "zx"

Input: words = ["z","x","z"]
Output: ""  (cycle: z → x → z)
```

## Approach 1: Topological Sort (BFS / Kahn's Algorithm)
**Time:** O(C) where C = total characters in all words | **Space:** O(1) (26 chars max)

```java
public String alienOrder(String[] words) {
    Map<Character, Set<Character>> adj = new HashMap<>();
    Map<Character, Integer> inDegree = new HashMap<>();

    // Initialize all characters
    for (String word : words)
        for (char c : word.toCharArray()) { adj.putIfAbsent(c, new HashSet<>()); inDegree.putIfAbsent(c, 0); }

    // Build edges from adjacent word pairs
    for (int i = 0; i < words.length - 1; i++) {
        String w1 = words[i], w2 = words[i+1];
        int minLen = Math.min(w1.length(), w2.length());
        if (w1.length() > w2.length() && w1.startsWith(w2)) return ""; // invalid: "abc" before "ab"
        for (int j = 0; j < minLen; j++) {
            if (w1.charAt(j) != w2.charAt(j)) {
                if (adj.get(w1.charAt(j)).add(w2.charAt(j)))
                    inDegree.merge(w2.charAt(j), 1, Integer::sum);
                break;
            }
        }
    }

    // Kahn's BFS topological sort
    Queue<Character> q = new LinkedList<>();
    inDegree.forEach((c, deg) -> { if (deg == 0) q.offer(c); });
    StringBuilder sb = new StringBuilder();
    while (!q.isEmpty()) {
        char c = q.poll();
        sb.append(c);
        for (char next : adj.get(c)) {
            inDegree.merge(next, -1, Integer::sum);
            if (inDegree.get(next) == 0) q.offer(next);
        }
    }
    return sb.length() == inDegree.size() ? sb.toString() : "";
}
```

## Approach 2: DFS Topological Sort
**Time:** O(C) | **Space:** O(1)

```java
public String alienOrder(String[] words) {
    Map<Character, List<Character>> adj = new HashMap<>();
    for (String word : words)
        for (char c : word.toCharArray()) adj.putIfAbsent(c, new ArrayList<>());

    for (int i = 0; i < words.length - 1; i++) {
        String w1 = words[i], w2 = words[i+1];
        if (w1.length() > w2.length() && w1.startsWith(w2)) return "";
        for (int j = 0; j < Math.min(w1.length(), w2.length()); j++) {
            if (w1.charAt(j) != w2.charAt(j)) { adj.get(w1.charAt(j)).add(w2.charAt(j)); break; }
        }
    }

    // 0=unvisited, 1=visiting, 2=visited
    Map<Character, Integer> state = new HashMap<>();
    StringBuilder sb = new StringBuilder();
    for (char c : adj.keySet())
        if (!state.containsKey(c) && !dfs(adj, state, sb, c)) return "";
    return sb.reverse().toString();
}

private boolean dfs(Map<Character, List<Character>> adj, Map<Character, Integer> state, StringBuilder sb, char c) {
    state.put(c, 1);
    for (char next : adj.get(c)) {
        if (state.getOrDefault(next, 0) == 1) return false; // cycle
        if (state.getOrDefault(next, 0) == 0 && !dfs(adj, state, sb, next)) return false;
    }
    state.put(c, 2);
    sb.append(c);
    return true;
}
```

## Key Insight
Compare adjacent words to extract ordering constraints (edges). Then topological sort gives the alphabet order.
Cycle = invalid ordering. If `w1.startsWith(w2)` but `w1` comes first, it's immediately invalid.
