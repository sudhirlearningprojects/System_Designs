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
