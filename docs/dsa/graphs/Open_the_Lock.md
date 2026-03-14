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

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `target = "0000"` | 0 | Already at target |
| `"0000"` in deadends | -1 | Can’t even start |
| `target` in deadends | -1 | Can never reach target |
| No deadends | Minimum turns | Pure BFS |
| Target surrounded by deadends | -1 | All neighbors of target are dead |
| `deadends = ["0001","0010","0100","1000","9999","9990","9909","9099","0999"]`, `target="9999"` | -1 | All paths to 9999 blocked |

---

## Dry Run

**Input:** `deadends=["0201","0101","0102","1212","2002"], target="0202"`

**BFS trace (abbreviated):**
```
dead = {0201, 0101, 0102, 1212, 2002}
Queue: ["0000"], visited={"0000"}, turns=0

--- turns=1 ---
Process "0000": 8 neighbors:
  "1000","9000","0100","0900","0010","0090","0001","0009"
  None are target, none in dead → all enqueued
Queue: ["1000","9000","0100","0900","0010","0090","0001","0009"]

--- turns=2 ---
Process "0100": neighbors include "0200","0000"(visited),"0110",...
  "0200" not dead, not target → enqueue
  ...
Process "0001": neighbors include "0002","0000"(visited),...
  "0002" not dead → enqueue
  ...

[After several levels, BFS reaches "0202" at turns=6]

Answer: 6
```

**Why 6 turns?** One valid path: `0000→0001→0002→0102`(dead!)→ must find alternate route around deadends.

---

## Follow-up Questions

**Q: What if the lock has more than 4 dials?**
The algorithm scales: state space = 10^k for k dials. BFS still works but may be slow for large k. Use A* with a heuristic.

**Q: What if dials don’t wrap around (0 doesn’t connect to 9)?**
Remove the modulo wrapping: `chars[i] = (char)(orig + 1)` only if `orig < '9'`, etc.

**Q: How does bidirectional BFS help here?**
Standard BFS explores O(8^d) states. Bidirectional explores O(8^(d/2)) from each end — much faster for large d.

**Q: Can you use A* here?**
Yes — heuristic = sum of min rotations per dial to match target. But BFS is simpler since all edges have weight 1.

**Related Problems:** LC 127 (Word Ladder), LC 909 (Snakes and Ladders), LC 433 (Minimum Genetic Mutation)
