# Reconstruct Itinerary
**LeetCode 332** | Hard | Hierholzer's Algorithm (Eulerian Path)

## Problem
Given a list of airline tickets `[from, to]`, reconstruct the itinerary starting from "JFK".
Use all tickets exactly once. If multiple valid itineraries exist, return the lexicographically smallest one.

```
Input: tickets = [["MUC","LHR"],["JFK","MUC"],["SFO","SJC"],["LHR","SFO"]]
Output: ["JFK","MUC","LHR","SFO","SJC"]

Input: tickets = [["JFK","SFO"],["JFK","ATL"],["SFO","ATL"],["ATL","JFK"],["ATL","SFO"]]
Output: ["JFK","ATL","JFK","SFO","ATL","SFO"]
```

## Approach 1: Hierholzer's DFS (Optimal)
Use a sorted adjacency list (min-heap or sorted list). Post-order DFS builds the path in reverse.

**Time:** O(E log E) | **Space:** O(E)

```java
public List<String> findItinerary(List<List<String>> tickets) {
    Map<String, PriorityQueue<String>> adj = new HashMap<>();
    for (List<String> t : tickets)
        adj.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).offer(t.get(1));

    LinkedList<String> result = new LinkedList<>();
    dfs("JFK", adj, result);
    return result;
}

private void dfs(String airport, Map<String, PriorityQueue<String>> adj, LinkedList<String> result) {
    PriorityQueue<String> neighbors = adj.get(airport);
    while (neighbors != null && !neighbors.isEmpty())
        dfs(neighbors.poll(), adj, result);
    result.addFirst(airport); // post-order: add after all neighbors are visited
}
```

## Approach 2: Iterative Hierholzer's
**Time:** O(E log E) | **Space:** O(E)

```java
public List<String> findItinerary(List<List<String>> tickets) {
    Map<String, PriorityQueue<String>> adj = new HashMap<>();
    for (List<String> t : tickets)
        adj.computeIfAbsent(t.get(0), k -> new PriorityQueue<>()).offer(t.get(1));

    LinkedList<String> result = new LinkedList<>();
    Deque<String> stack = new ArrayDeque<>();
    stack.push("JFK");

    while (!stack.isEmpty()) {
        String airport = stack.peek();
        PriorityQueue<String> neighbors = adj.get(airport);
        if (neighbors == null || neighbors.isEmpty()) {
            result.addFirst(stack.pop());
        } else {
            stack.push(neighbors.poll());
        }
    }
    return result;
}
```

## Key Insight
This is finding an **Eulerian path** (visit every edge exactly once).
Hierholzer's algorithm: greedily follow the smallest neighbor; when stuck, backtrack and prepend to result.
The post-order insertion (`addFirst`) ensures the path is built correctly even when backtracking.

```
Why post-order?
If we hit a dead end, that airport must be the last in the itinerary (or a sub-path endpoint).
By adding it first to the result after exhausting all its edges, we naturally handle branching.
```

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| Single ticket `[["JFK","ATL"]]` | `["JFK","ATL"]` | Trivial path |
| All tickets from JFK | Sorted order | Lexicographically smallest |
| Tickets form a cycle | Cycle starting at JFK | Eulerian circuit |
| Multiple paths, pick lex smallest | Use min-heap | PriorityQueue ensures lex order |
| Dead-end branch before longer path | Post-order handles it | Hierholzer’s backtracks correctly |
| `[["JFK","KUL"],["JFK","NRT"],["NRT","JFK"]]` | `["JFK","NRT","JFK","KUL"]` | Must go NRT first, else KUL is dead end |

---

## Dry Run

**Input:** `tickets = [["JFK","SFO"],["JFK","ATL"],["SFO","ATL"],["ATL","JFK"],["ATL","SFO"]]`

**Build adjacency (min-heap):**
```
JFK: [ATL, SFO]
SFO: [ATL]
ATL: [JFK, SFO]
```

**Iterative Hierholzer’s trace:**
```
stack = [JFK], result = []

Peek JFK, neighbors=[ATL,SFO] → push ATL
stack = [JFK, ATL]

Peek ATL, neighbors=[JFK,SFO] → push JFK
stack = [JFK, ATL, JFK]

Peek JFK, neighbors=[SFO] → push SFO
stack = [JFK, ATL, JFK, SFO]

Peek SFO, neighbors=[ATL] → push ATL
stack = [JFK, ATL, JFK, SFO, ATL]

Peek ATL, neighbors=[SFO] → push SFO
stack = [JFK, ATL, JFK, SFO, ATL, SFO]

Peek SFO, neighbors=[] → pop SFO, addFirst → result=[SFO]
stack = [JFK, ATL, JFK, SFO, ATL]

Peek ATL, neighbors=[] → pop ATL, addFirst → result=[ATL,SFO]
stack = [JFK, ATL, JFK, SFO]

Peek SFO, neighbors=[] → pop SFO, addFirst → result=[SFO,ATL,SFO]
stack = [JFK, ATL, JFK]

Peek JFK, neighbors=[] → pop JFK, addFirst → result=[JFK,SFO,ATL,SFO]
stack = [JFK, ATL]

Peek ATL, neighbors=[] → pop ATL, addFirst → result=[ATL,JFK,SFO,ATL,SFO]
stack = [JFK]

Peek JFK, neighbors=[] → pop JFK, addFirst → result=[JFK,ATL,JFK,SFO,ATL,SFO]

Answer: ["JFK","ATL","JFK","SFO","ATL","SFO"]
```

---

## Follow-up Questions

**Q: When does an Eulerian path exist?**
For directed graphs: exactly one node has `out-degree - in-degree = 1` (start), one has `in-degree - out-degree = 1` (end), all others are balanced. The problem guarantees a valid itinerary exists.

**Q: What if no valid itinerary exists?**
The problem guarantees one exists. In general, check Eulerian path conditions first.

**Q: Why use `addFirst` instead of `add` + `reverse`?**
Both work. `addFirst` on a LinkedList is O(1) and avoids the final reverse step.

**Q: What if there are multiple airports with the same name?**
Not possible — airport codes are unique strings.

**Related Problems:** LC 753 (Cracking the Safe — Eulerian circuit on de Bruijn graph), LC 2097 (Valid Arrangement of Pairs)
