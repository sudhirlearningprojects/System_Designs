# Graphs Deep Dive (40 Problems)

## 1. Number of Islands (LC 200) ⭐⭐⭐⭐⭐
```java
public int numIslands(char[][] grid) {
    int count = 0;
    for (int i = 0; i < grid.length; i++)
        for (int j = 0; j < grid[0].length; j++)
            if (grid[i][j] == '1') { count++; dfs(grid, i, j); }
    return count;
}
void dfs(char[][] g, int i, int j) {
    if (i < 0 || i >= g.length || j < 0 || j >= g[0].length || g[i][j] == '0') return;
    g[i][j] = '0';
    dfs(g, i+1, j); dfs(g, i-1, j); dfs(g, i, j+1); dfs(g, i, j-1);
}
```

## 2. Clone Graph (LC 133) ⭐⭐⭐⭐
```java
public Node cloneGraph(Node node) {
    if (node == null) return null;
    Map<Node, Node> map = new HashMap<>();
    return dfs(node, map);
}
Node dfs(Node node, Map<Node, Node> map) {
    if (map.containsKey(node)) return map.get(node);
    Node copy = new Node(node.val);
    map.put(node, copy);
    for (Node n : node.neighbors) copy.neighbors.add(dfs(n, map));
    return copy;
}
```

## 3. Course Schedule (LC 207) ⭐⭐⭐⭐⭐
```java
public boolean canFinish(int n, int[][] pre) {
    List<List<Integer>> g = new ArrayList<>();
    int[] indegree = new int[n];
    for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    for (int[] p : pre) { g.get(p[1]).add(p[0]); indegree[p[0]]++; }
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < n; i++) if (indegree[i] == 0) q.offer(i);
    int count = 0;
    while (!q.isEmpty()) {
        int c = q.poll(); count++;
        for (int next : g.get(c)) if (--indegree[next] == 0) q.offer(next);
    }
    return count == n;
}
```

## 4. Course Schedule II (LC 210) ⭐⭐⭐⭐
```java
public int[] findOrder(int n, int[][] pre) {
    List<List<Integer>> g = new ArrayList<>();
    int[] indegree = new int[n];
    for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    for (int[] p : pre) { g.get(p[1]).add(p[0]); indegree[p[0]]++; }
    Queue<Integer> q = new LinkedList<>();
    for (int i = 0; i < n; i++) if (indegree[i] == 0) q.offer(i);
    int[] result = new int[n]; int idx = 0;
    while (!q.isEmpty()) {
        int c = q.poll(); result[idx++] = c;
        for (int next : g.get(c)) if (--indegree[next] == 0) q.offer(next);
    }
    return idx == n ? result : new int[0];
}
```

## 5. Word Ladder (LC 127) ⭐⭐⭐⭐⭐
```java
public int ladderLength(String begin, String end, List<String> wordList) {
    Set<String> set = new HashSet<>(wordList);
    if (!set.contains(end)) return 0;
    Queue<String> q = new LinkedList<>(); q.offer(begin);
    int level = 1;
    while (!q.isEmpty()) {
        int size = q.size();
        for (int i = 0; i < size; i++) {
            String word = q.poll();
            if (word.equals(end)) return level;
            char[] chars = word.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char orig = chars[j];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == orig) continue;
                    chars[j] = c;
                    String newWord = new String(chars);
                    if (set.contains(newWord)) { q.offer(newWord); set.remove(newWord); }
                }
                chars[j] = orig;
            }
        }
        level++;
    }
    return 0;
}
```

## 6. Pacific Atlantic Water Flow (LC 417) ⭐⭐⭐⭐
```java
public List<List<Integer>> pacificAtlantic(int[][] heights) {
    int m = heights.length, n = heights[0].length;
    boolean[][] pac = new boolean[m][n], atl = new boolean[m][n];
    for (int i = 0; i < m; i++) { dfs(heights, pac, i, 0); dfs(heights, atl, i, n-1); }
    for (int j = 0; j < n; j++) { dfs(heights, pac, 0, j); dfs(heights, atl, m-1, j); }
    List<List<Integer>> res = new ArrayList<>();
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++)
            if (pac[i][j] && atl[i][j]) res.add(Arrays.asList(i, j));
    return res;
}
void dfs(int[][] h, boolean[][] vis, int i, int j) {
    vis[i][j] = true;
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    for (int[] d : dirs) {
        int x = i+d[0], y = j+d[1];
        if (x >= 0 && x < h.length && y >= 0 && y < h[0].length && !vis[x][y] && h[x][y] >= h[i][j])
            dfs(h, vis, x, y);
    }
}
```

## 7. Graph Valid Tree (LC 261) ⭐⭐⭐⭐
```java
public boolean validTree(int n, int[][] edges) {
    if (edges.length != n - 1) return false;
    List<List<Integer>> g = new ArrayList<>();
    for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    for (int[] e : edges) { g.get(e[0]).add(e[1]); g.get(e[1]).add(e[0]); }
    boolean[] vis = new boolean[n];
    return dfs(g, vis, 0, -1) && Arrays.stream(vis).allMatch(v -> v);
}
boolean dfs(List<List<Integer>> g, boolean[] vis, int node, int parent) {
    vis[node] = true;
    for (int next : g.get(node)) {
        if (next == parent) continue;
        if (vis[next] || !dfs(g, vis, next, node)) return false;
    }
    return true;
}
```

## 8. Alien Dictionary (LC 269) ⭐⭐⭐⭐
```java
public String alienOrder(String[] words) {
    Map<Character, Set<Character>> g = new HashMap<>();
    Map<Character, Integer> indegree = new HashMap<>();
    for (String w : words) for (char c : w.toCharArray()) { g.putIfAbsent(c, new HashSet<>()); indegree.putIfAbsent(c, 0); }
    for (int i = 0; i < words.length - 1; i++) {
        String w1 = words[i], w2 = words[i+1];
        int len = Math.min(w1.length(), w2.length());
        if (w1.length() > w2.length() && w1.startsWith(w2)) return "";
        for (int j = 0; j < len; j++) {
            if (w1.charAt(j) != w2.charAt(j)) {
                if (!g.get(w1.charAt(j)).contains(w2.charAt(j))) {
                    g.get(w1.charAt(j)).add(w2.charAt(j));
                    indegree.put(w2.charAt(j), indegree.get(w2.charAt(j)) + 1);
                }
                break;
            }
        }
    }
    Queue<Character> q = new LinkedList<>();
    for (char c : indegree.keySet()) if (indegree.get(c) == 0) q.offer(c);
    StringBuilder sb = new StringBuilder();
    while (!q.isEmpty()) {
        char c = q.poll(); sb.append(c);
        for (char next : g.get(c)) if (--indegree.put(next, indegree.get(next) - 1) == 0) q.offer(next);
    }
    return sb.length() == indegree.size() ? sb.toString() : "";
}
```

## 9. Network Delay Time (LC 743) ⭐⭐⭐⭐
```java
public int networkDelayTime(int[][] times, int n, int k) {
    Map<Integer, List<int[]>> g = new HashMap<>();
    for (int[] t : times) g.computeIfAbsent(t[0], x -> new ArrayList<>()).add(new int[]{t[1], t[2]});
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
    pq.offer(new int[]{k, 0});
    int[] dist = new int[n + 1]; Arrays.fill(dist, Integer.MAX_VALUE);
    dist[k] = 0;
    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int node = curr[0], time = curr[1];
        if (time > dist[node]) continue;
        if (!g.containsKey(node)) continue;
        for (int[] next : g.get(node)) {
            int newTime = time + next[1];
            if (newTime < dist[next[0]]) { dist[next[0]] = newTime; pq.offer(new int[]{next[0], newTime}); }
        }
    }
    int max = 0;
    for (int i = 1; i <= n; i++) { if (dist[i] == Integer.MAX_VALUE) return -1; max = Math.max(max, dist[i]); }
    return max;
}
```

## 10. Accounts Merge (LC 721) ⭐⭐⭐⭐
```java
public List<List<String>> accountsMerge(List<List<String>> accounts) {
    Map<String, String> parent = new HashMap<>();
    Map<String, String> owner = new HashMap<>();
    for (List<String> acc : accounts) {
        for (int i = 1; i < acc.size(); i++) {
            parent.put(acc.get(i), acc.get(i));
            owner.put(acc.get(i), acc.get(0));
        }
    }
    for (List<String> acc : accounts)
        for (int i = 2; i < acc.size(); i++)
            union(parent, acc.get(1), acc.get(i));
    Map<String, TreeSet<String>> unions = new HashMap<>();
    for (List<String> acc : accounts)
        for (int i = 1; i < acc.size(); i++)
            unions.computeIfAbsent(find(parent, acc.get(i)), x -> new TreeSet<>()).add(acc.get(i));
    List<List<String>> res = new ArrayList<>();
    for (String p : unions.keySet()) {
        List<String> emails = new ArrayList<>(unions.get(p));
        emails.add(0, owner.get(p));
        res.add(emails);
    }
    return res;
}
String find(Map<String, String> p, String s) {
    return p.get(s).equals(s) ? s : find(p, p.get(s));
}
void union(Map<String, String> p, String a, String b) {
    p.put(find(p, a), find(p, b));
}
```

## Pattern Summary
- **BFS**: Shortest path (Word Ladder, Network Delay)
- **DFS**: Connected components (Islands, Clone)
- **Topological Sort**: Course Schedule, Alien Dictionary
- **Union-Find**: Accounts Merge, Graph Valid Tree
- **Dijkstra**: Network Delay Time

**Next**: [Dynamic Programming Deep Dive](12_DP_Deep_Dive.md)
