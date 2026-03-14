# Bus Routes
**LeetCode 815** | Hard | BFS on Route Graph

## Problem
Given bus routes where `routes[i]` is the list of stops for bus `i`, find the minimum number of buses
needed to travel from `source` to `target`. Return `-1` if impossible.

```
Input: routes = [[1,2,7],[3,6,7]], source = 1, target = 6
Output: 2  →  Bus 0: 1→7, Bus 1: 7→6

Input: routes = [[7,12],[4,5,15],[6],[15,19],[9,12,13]], source = 15, target = 12
Output: -1
```

## Approach 1: BFS on Routes (Optimal)
Model routes as nodes. Two routes are connected if they share a stop.
BFS counts the minimum number of route changes (buses taken).

**Time:** O(N²×M) where N=routes, M=max stops per route | **Space:** O(N×M)

```java
public int numBusesToDestination(int[][] routes, int source, int target) {
    if (source == target) return 0;

    // Map each stop to the list of routes serving it
    Map<Integer, List<Integer>> stopToRoutes = new HashMap<>();
    for (int i = 0; i < routes.length; i++)
        for (int stop : routes[i])
            stopToRoutes.computeIfAbsent(stop, k -> new ArrayList<>()).add(i);

    // BFS: level = number of buses taken
    Queue<Integer> q = new LinkedList<>(); // stops
    Set<Integer> visitedStops = new HashSet<>();
    Set<Integer> visitedRoutes = new HashSet<>();
    q.offer(source); visitedStops.add(source);
    int buses = 0;

    while (!q.isEmpty()) {
        buses++;
        int size = q.size();
        while (size-- > 0) {
            int stop = q.poll();
            for (int route : stopToRoutes.getOrDefault(stop, new ArrayList<>())) {
                if (visitedRoutes.contains(route)) continue;
                visitedRoutes.add(route);
                for (int nextStop : routes[route]) {
                    if (nextStop == target) return buses;
                    if (!visitedStops.contains(nextStop)) {
                        visitedStops.add(nextStop); q.offer(nextStop);
                    }
                }
            }
        }
    }
    return -1;
}
```

## Approach 2: BFS on Stop Graph
Build a graph where stops are nodes, connected if they share a route.

**Time:** O(N×M²) | **Space:** O(N×M²) — worse for large routes

```java
public int numBusesToDestination(int[][] routes, int source, int target) {
    if (source == target) return 0;
    Map<Integer, Set<Integer>> adj = new HashMap<>();
    for (int[] route : routes) {
        for (int i = 0; i < route.length; i++)
            for (int j = i+1; j < route.length; j++) {
                adj.computeIfAbsent(route[i], k -> new HashSet<>()).add(route[j]);
                adj.computeIfAbsent(route[j], k -> new HashSet<>()).add(route[i]);
            }
    }
    // BFS on stops — but this doesn't count buses, counts stops
    // Need route-level BFS for correct answer
    return -1; // Use Approach 1 instead
}
```

## Key Insight
The key insight is to BFS at the **route level**, not the stop level.
When you board a route, you can reach ALL stops on that route for free (same bus).
So each BFS level = one bus taken. Mark routes as visited to avoid re-boarding.

```
Graph model:
- Nodes: bus routes
- Edges: two routes share at least one stop
- Source: all routes containing 'source' stop
- Target: any route containing 'target' stop
```

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `source == target` | 0 | Already there |
| Source and target on same route | 1 | One bus |
| No route contains target | -1 | Unreachable |
| Routes form a chain | Number of routes in chain | Must transfer at each shared stop |
| Single route containing both | 1 | Direct |
| `routes=[[1,2],[3,4]]`, `source=1, target=4` | -1 | No shared stop between routes |

---

## Dry Run

**Input:** `routes=[[1,2,7],[3,6,7]], source=1, target=6`

**Build stopToRoutes:**
```
stop 1 → [route 0]
stop 2 → [route 0]
stop 7 → [route 0, route 1]
stop 3 → [route 1]
stop 6 → [route 1]
```

**BFS trace:**
```
Queue: [1], visitedStops={1}, visitedRoutes={}, buses=0

--- buses=1 ---
Pop stop 1:
  Routes at stop 1: [route 0]
  Route 0 not visited → mark visited
  Stops in route 0: [1,2,7]
    stop 1: already visited
    stop 2: not target, not visited → enqueue, visitedStops={1,2,7}
    stop 7: not target, not visited → enqueue
Queue: [2,7]

--- buses=2 ---
Pop stop 2:
  Routes at stop 2: [route 0] → already visited, skip
Pop stop 7:
  Routes at stop 7: [route 0 (visited), route 1]
  Route 1 not visited → mark visited
  Stops in route 1: [3,6,7]
    stop 3: not target → enqueue
    stop 6: == target → return buses=2!

Answer: 2
```

---

## Follow-up Questions

**Q: Why mark routes as visited instead of just stops?**
If you only mark stops, you’d re-process the same route multiple times (once per stop). Marking routes avoids redundant work.

**Q: What if a stop is on 100 routes?**
All 100 routes get explored when that stop is first visited. This is fine — each route is processed at most once.

**Q: What if `source` is not on any route?**
The BFS queue starts with `source` but no routes are found → queue empties → return -1.

**Q: Can you model this as a bipartite graph (stops ↔ routes)?**
Yes — that’s exactly what `stopToRoutes` represents. BFS alternates between stop-nodes and route-nodes.

**Related Problems:** LC 127 (Word Ladder), LC 752 (Open the Lock), LC 1129 (Shortest Path with Alternating Colors)
