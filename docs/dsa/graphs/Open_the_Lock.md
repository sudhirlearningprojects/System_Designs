# Open the Lock
**LeetCode 752** | Medium | BFS

## Problem
A lock has 4 circular dials (0-9). Starting from `"0000"`, find the minimum number of turns
to reach `target`, avoiding `deadends`. Each turn rotates one dial by ±1.

```
Input: deadends = ["0201","0101","0102","1212","2002"], target = "0202"
Output: 6

Input: deadends = ["8888"], target = "0009"
Output: 1

Input: deadends = ["8887","8889","8878","8898","8788","8988","7888","9888"], target = "8888"
Output: -1
```

## Approach 1: BFS (Standard)
**Time:** O(10⁴ × 4 × 2) = O(1) bounded | **Space:** O(10⁴)

```java
public int openLock(String[] deadends, String target) {
    Set<String> dead = new HashSet<>(Arrays.asList(deadends));
    if (dead.contains("0000")) return -1;
    if (target.equals("0000")) return 0;

    Queue<String> q = new LinkedList<>();
    Set<String> visited = new HashSet<>();
    q.offer("0000"); visited.add("0000");
    int turns = 0;

    while (!q.isEmpty()) {
        turns++;
        int size = q.size();
        while (size-- > 0) {
            String curr = q.poll();
            for (String next : getNeighbors(curr)) {
                if (next.equals(target)) return turns;
                if (!visited.contains(next) && !dead.contains(next)) {
                    visited.add(next); q.offer(next);
                }
            }
        }
    }
    return -1;
}

private List<String> getNeighbors(String s) {
    List<String> neighbors = new ArrayList<>();
    char[] chars = s.toCharArray();
    for (int i = 0; i < 4; i++) {
        char orig = chars[i];
        chars[i] = (char)((orig - '0' + 1) % 10 + '0');
        neighbors.add(new String(chars));
        chars[i] = (char)((orig - '0' + 9) % 10 + '0');
        neighbors.add(new String(chars));
        chars[i] = orig;
    }
    return neighbors;
}
```

## Approach 2: Bidirectional BFS (Faster)
**Time:** O(10⁴) | **Space:** O(10⁴)

```java
public int openLock(String[] deadends, String target) {
    Set<String> dead = new HashSet<>(Arrays.asList(deadends));
    if (dead.contains("0000") || dead.contains(target)) return -1;
    if (target.equals("0000")) return 0;

    Set<String> beginSet = new HashSet<>(), endSet = new HashSet<>(), visited = new HashSet<>();
    beginSet.add("0000"); endSet.add(target);
    visited.add("0000"); visited.add(target);
    int turns = 0;

    while (!beginSet.isEmpty() && !endSet.isEmpty()) {
        if (beginSet.size() > endSet.size()) { Set<String> t = beginSet; beginSet = endSet; endSet = t; }
        Set<String> nextSet = new HashSet<>();
        for (String curr : beginSet) {
            for (String next : getNeighbors(curr)) {
                if (endSet.contains(next)) return turns + 1;
                if (!visited.contains(next) && !dead.contains(next)) { visited.add(next); nextSet.add(next); }
            }
        }
        beginSet = nextSet;
        turns++;
    }
    return -1;
}

private List<String> getNeighbors(String s) {
    List<String> neighbors = new ArrayList<>();
    char[] chars = s.toCharArray();
    for (int i = 0; i < 4; i++) {
        char orig = chars[i];
        chars[i] = (char)((orig - '0' + 1) % 10 + '0'); neighbors.add(new String(chars));
        chars[i] = (char)((orig - '0' + 9) % 10 + '0'); neighbors.add(new String(chars));
        chars[i] = orig;
    }
    return neighbors;
}
```

## Key Insight
The lock combinations form an implicit graph with 10⁴ = 10,000 nodes.
Each node has exactly 8 neighbors (4 dials × 2 directions).
BFS finds the shortest path. Bidirectional BFS cuts the search space roughly in half.
