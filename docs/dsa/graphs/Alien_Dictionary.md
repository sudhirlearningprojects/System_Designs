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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `["z","z"]` | any valid order | Same word, no constraint derived |
| `["abc","ab"]` | `""` | Longer word before its prefix → invalid |
| `["a","b","a"]` | `""` | Cycle: a→b and b→a |
| Single word `["abc"]` | `"abc"` or any permutation | No ordering constraints |
| All same characters `["aa","bb"]` | `"ab"` | Only constraint: a before b |
| Words with unique chars only | valid topo order | No ambiguity |
| `["wrt","wrf"]` | `"tf"` or `"...t...f..."` | Only constraint: t before f |

---

## Dry Run

**Input:** `words = ["wrt","wrf","er","ett","rftt"]`

**Step 1: Extract edges from adjacent pairs**
```
"wrt" vs "wrf": first diff at index 2 → t < f  → edge t→f
"wrf" vs "er":  first diff at index 0 → w < e  → edge w→e
"er"  vs "ett": first diff at index 1 → r < t  → edge r→t
"ett" vs "rftt": first diff at index 0 → e < r  → edge e→r
```

**Step 2: Build graph**
```
Nodes: {w, r, t, f, e}
Edges: t→f, w→e, r→t, e→r
InDegree: {w:0, r:1, t:1, f:1, e:1}
```

**Step 3: Kahn’s BFS**
```
Queue (in-degree 0): [w]

Pop w: output="w", process w→e: inDegree[e]=0 → enqueue e
Queue: [e]

Pop e: output="we", process e→r: inDegree[r]=0 → enqueue r
Queue: [r]

Pop r: output="wer", process r→t: inDegree[t]=0 → enqueue t
Queue: [t]

Pop t: output="wert", process t→f: inDegree[f]=0 → enqueue f
Queue: [f]

Pop f: output="wertf"

output.length(5) == nodes(5) → valid → return "wertf"
```

---

## Follow-up Questions

**Q: Can there be multiple valid orderings?**
Yes. The problem says return any valid one. The BFS order depends on which zero-in-degree nodes are processed first (use a min-heap for lexicographically smallest).

**Q: What if a character appears in words but has no ordering constraint?**
It still gets initialized with in-degree 0 and will appear in the output — just at any valid position.

**Q: How to detect if the input is completely invalid (not just cyclic)?**
Two cases: (1) cycle detected → return `""`, (2) `w1.startsWith(w2)` with `w1` before `w2` → return `""`.

**Q: What’s the time complexity in terms of input size?**
O(C) where C = total number of characters across all words. Each character is processed once.

**Related Problems:** LC 207 (Course Schedule), LC 210 (Course Schedule II), LC 310 (Minimum Height Trees), LC 1203 (Sort Items by Groups)
