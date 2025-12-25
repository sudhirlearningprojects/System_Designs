# Shortest Path in Train Network (BFS in Unweighted Graph)

## Problem Statement

Given a train network represented as an undirected graph where stations are nodes and connections are edges, find the shortest path between two stations. All connections have equal distance (unweighted graph).

**Input:** 
- Train network (graph of stations)
- From station name
- To station name

**Output:** 
- List of stations representing shortest path
- Empty list if no path exists

**Example:**
```
Network:
King's Cross St Pancras --- Angel ---- Old Street
|                   \                            |
|                    \                           |
|                     \                          |
Russell Square         Farringdon --- Barbican --- Moorgate
|                                                  /
|                                                 /
|                                                /
Holborn --- Chancery Lane --- St Paul's --- Bank

Query: King's Cross St Pancras → St Paul's

Result: King's Cross St Pancras → Russell Square → Holborn 
        → Chancery Lane → St Paul's
Length: 5 stations (4 hops)
```

---

## Solution Approaches

### Approach 1: BFS (Optimal for Unweighted Graph)

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public List<Station> shortestPath(String from, String to) {
    Station start = stations.get(from);
    Station end = stations.get(to);
    
    if (start == null || end == null) return Collections.emptyList();
    if (start.equals(end)) return Arrays.asList(start);
    
    Queue<Station> queue = new LinkedList<>();
    Map<Station, Station> parent = new HashMap<>();
    Set<Station> visited = new HashSet<>();
    
    queue.offer(start);
    visited.add(start);
    parent.put(start, null);
    
    while (!queue.isEmpty()) {
        Station current = queue.poll();
        
        if (current.equals(end)) {
            return reconstructPath(parent, start, end);
        }
        
        for (Station neighbor : current.getNeighbours()) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                parent.put(neighbor, current);
                queue.offer(neighbor);
            }
        }
    }
    
    return Collections.emptyList();
}

private List<Station> reconstructPath(Map<Station, Station> parent, 
                                      Station start, Station end) {
    List<Station> path = new ArrayList<>();
    Station current = end;
    
    while (current != null) {
        path.add(current);
        current = parent.get(current);
    }
    
    Collections.reverse(path);
    return path;
}
```

---

### Approach 2: Bidirectional BFS (Optimized)

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public List<Station> shortestPathBidirectional(String from, String to) {
    Station start = stations.get(from);
    Station end = stations.get(to);
    
    if (start == null || end == null) return Collections.emptyList();
    if (start.equals(end)) return Arrays.asList(start);
    
    Map<Station, Station> parentFromStart = new HashMap<>();
    Map<Station, Station> parentFromEnd = new HashMap<>();
    
    Set<Station> visitedFromStart = new HashSet<>();
    Set<Station> visitedFromEnd = new HashSet<>();
    
    Queue<Station> queueFromStart = new LinkedList<>();
    Queue<Station> queueFromEnd = new LinkedList<>();
    
    queueFromStart.offer(start);
    queueFromEnd.offer(end);
    visitedFromStart.add(start);
    visitedFromEnd.add(end);
    parentFromStart.put(start, null);
    parentFromEnd.put(end, null);
    
    while (!queueFromStart.isEmpty() && !queueFromEnd.isEmpty()) {
        Station meeting = bfsStep(queueFromStart, visitedFromStart, 
                                   visitedFromEnd, parentFromStart);
        if (meeting != null) {
            return buildPath(parentFromStart, parentFromEnd, meeting);
        }
        
        meeting = bfsStep(queueFromEnd, visitedFromEnd, 
                         visitedFromStart, parentFromEnd);
        if (meeting != null) {
            return buildPath(parentFromStart, parentFromEnd, meeting);
        }
    }
    
    return Collections.emptyList();
}

private Station bfsStep(Queue<Station> queue, Set<Station> visited,
                       Set<Station> otherVisited, Map<Station, Station> parent) {
    Station current = queue.poll();
    
    for (Station neighbor : current.getNeighbours()) {
        if (otherVisited.contains(neighbor)) {
            return neighbor;
        }
        
        if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            parent.put(neighbor, current);
            queue.offer(neighbor);
        }
    }
    
    return null;
}

private List<Station> buildPath(Map<Station, Station> parentFromStart,
                                Map<Station, Station> parentFromEnd,
                                Station meeting) {
    List<Station> path = new ArrayList<>();
    
    Station current = meeting;
    while (current != null) {
        path.add(current);
        current = parentFromStart.get(current);
    }
    Collections.reverse(path);
    
    current = parentFromEnd.get(meeting);
    while (current != null) {
        path.add(current);
        current = parentFromEnd.get(current);
    }
    
    return path;
}
```

---

### Approach 3: DFS (Not Optimal for Shortest Path)

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public List<Station> shortestPathDFS(String from, String to) {
    Station start = stations.get(from);
    Station end = stations.get(to);
    
    if (start == null || end == null) return Collections.emptyList();
    
    List<Station> shortestPath = new ArrayList<>();
    List<Station> currentPath = new ArrayList<>();
    Set<Station> visited = new HashSet<>();
    
    dfs(start, end, visited, currentPath, shortestPath);
    
    return shortestPath;
}

private void dfs(Station current, Station end, Set<Station> visited,
                List<Station> currentPath, List<Station> shortestPath) {
    visited.add(current);
    currentPath.add(current);
    
    if (current.equals(end)) {
        if (shortestPath.isEmpty() || currentPath.size() < shortestPath.size()) {
            shortestPath.clear();
            shortestPath.addAll(currentPath);
        }
    } else {
        for (Station neighbor : current.getNeighbours()) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, end, visited, currentPath, shortestPath);
            }
        }
    }
    
    currentPath.remove(currentPath.size() - 1);
    visited.remove(current);
}
```

---

## Algorithm Walkthrough

### Example: King's Cross St Pancras → St Paul's

**Network:**
```
KX = King's Cross St Pancras
RS = Russell Square
H = Holborn
CL = Chancery Lane
SP = St Paul's
A = Angel
OS = Old Street
M = Moorgate
F = Farringdon
B = Barbican
BK = Bank

Connections:
KX: [A, F, RS]
RS: [KX, H]
H: [RS, CL]
CL: [H, SP]
SP: [CL, BK]
A: [KX, OS]
OS: [A, M]
M: [OS, B, BK]
F: [KX, B]
B: [F, M]
BK: [SP, M]
```

**BFS Execution:**

```
Initial:
  queue = [KX]
  visited = {KX}
  parent = {KX: null}

Step 1: Process KX
  queue = [A, F, RS]
  visited = {KX, A, F, RS}
  parent = {KX: null, A: KX, F: KX, RS: KX}

Step 2: Process A
  queue = [F, RS, OS]
  visited = {KX, A, F, RS, OS}
  parent = {KX: null, A: KX, F: KX, RS: KX, OS: A}

Step 3: Process F
  queue = [RS, OS, B]
  visited = {KX, A, F, RS, OS, B}
  parent = {KX: null, A: KX, F: KX, RS: KX, OS: A, B: F}

Step 4: Process RS
  queue = [OS, B, H]
  visited = {KX, A, F, RS, OS, B, H}
  parent = {KX: null, A: KX, F: KX, RS: KX, OS: A, B: F, H: RS}

Step 5: Process OS
  queue = [B, H, M]
  visited = {KX, A, F, RS, OS, B, H, M}
  parent = {..., M: OS}

Step 6: Process B
  queue = [H, M]
  (M already visited)

Step 7: Process H
  queue = [M, CL]
  visited = {..., CL}
  parent = {..., CL: H}

Step 8: Process M
  queue = [CL, BK]
  visited = {..., BK}
  parent = {..., BK: M}

Step 9: Process CL
  queue = [BK, SP]
  visited = {..., SP}
  parent = {..., SP: CL}

Step 10: Process BK
  queue = [SP]
  (SP already visited)

Step 11: Process SP
  Found target!

Reconstruct Path:
  SP → CL → H → RS → KX
  Reverse: KX → RS → H → CL → SP

Result: [King's Cross St Pancras, Russell Square, Holborn, 
         Chancery Lane, St Paul's]
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    private static class Station {
        private String name;
        private List<Station> neighbours;
        
        public Station(String name) {
            this.name = name;
            this.neighbours = new ArrayList<>(3);
        }
        
        String getName() {
            return name;
        }
        
        void addNeighbour(Station v) {
            this.neighbours.add(v);
        }
        
        List<Station> getNeighbours() {
            return this.neighbours;
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Station && this.name.equals(((Station) obj).getName());
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }
    }
    
    private static class TrainMap {
        private HashMap<String, Station> stations;
        
        public TrainMap() {
            this.stations = new HashMap<>();
        }
        
        public TrainMap addStation(String name) {
            Station s = new Station(name);
            this.stations.putIfAbsent(name, s);
            return this;
        }
        
        public Station getStation(String name) {
            return this.stations.get(name);
        }
        
        public TrainMap connectStations(Station fromStation, Station toStation) {
            if (fromStation == null || toStation == null) {
                throw new IllegalArgumentException("Station is null");
            }
            fromStation.addNeighbour(toStation);
            toStation.addNeighbour(fromStation);
            return this;
        }
        
        // BFS Solution
        public List<Station> shortestPath(String from, String to) {
            Station start = stations.get(from);
            Station end = stations.get(to);
            
            if (start == null || end == null) return Collections.emptyList();
            if (start.equals(end)) return Arrays.asList(start);
            
            Queue<Station> queue = new LinkedList<>();
            Map<Station, Station> parent = new HashMap<>();
            Set<Station> visited = new HashSet<>();
            
            queue.offer(start);
            visited.add(start);
            parent.put(start, null);
            
            while (!queue.isEmpty()) {
                Station current = queue.poll();
                
                if (current.equals(end)) {
                    return reconstructPath(parent, end);
                }
                
                for (Station neighbor : current.getNeighbours()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parent.put(neighbor, current);
                        queue.offer(neighbor);
                    }
                }
            }
            
            return Collections.emptyList();
        }
        
        private List<Station> reconstructPath(Map<Station, Station> parent, Station end) {
            List<Station> path = new ArrayList<>();
            Station current = end;
            
            while (current != null) {
                path.add(current);
                current = parent.get(current);
            }
            
            Collections.reverse(path);
            return path;
        }
        
        public static String convertPathToStringRepresentation(List<Station> path) {
            if (path.isEmpty()) return "";
            return path.stream()
                      .map(Station::getName)
                      .reduce((s1, s2) -> s1 + "->" + s2)
                      .get();
        }
    }
    
    public static boolean doTestsPass() {
        TrainMap trainMap = new TrainMap();
        
        trainMap.addStation("King's Cross St Pancras")
                .addStation("Angel")
                .addStation("Old Street")
                .addStation("Moorgate")
                .addStation("Farringdon")
                .addStation("Barbican")
                .addStation("Russel Square")
                .addStation("Holborn")
                .addStation("Chancery Lane")
                .addStation("St Paul's")
                .addStation("Bank");
        
        trainMap.connectStations(trainMap.getStation("King's Cross St Pancras"), 
                                trainMap.getStation("Angel"))
                .connectStations(trainMap.getStation("King's Cross St Pancras"), 
                                trainMap.getStation("Farringdon"))
                .connectStations(trainMap.getStation("King's Cross St Pancras"), 
                                trainMap.getStation("Russel Square"))
                .connectStations(trainMap.getStation("Russel Square"), 
                                trainMap.getStation("Holborn"))
                .connectStations(trainMap.getStation("Holborn"), 
                                trainMap.getStation("Chancery Lane"))
                .connectStations(trainMap.getStation("Chancery Lane"), 
                                trainMap.getStation("St Paul's"))
                .connectStations(trainMap.getStation("St Paul's"), 
                                trainMap.getStation("Bank"))
                .connectStations(trainMap.getStation("Angel"), 
                                trainMap.getStation("Old Street"))
                .connectStations(trainMap.getStation("Old Street"), 
                                trainMap.getStation("Moorgate"))
                .connectStations(trainMap.getStation("Moorgate"), 
                                trainMap.getStation("Bank"))
                .connectStations(trainMap.getStation("Farringdon"), 
                                trainMap.getStation("Barbican"))
                .connectStations(trainMap.getStation("Barbican"), 
                                trainMap.getStation("Moorgate"));
        
        String solution = "King's Cross St Pancras->Russel Square->Holborn->Chancery Lane->St Paul's";
        
        return solution.equals(TrainMap.convertPathToStringRepresentation(
            trainMap.shortestPath("King's Cross St Pancras", "St Paul's")));
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail.");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testShortestPath() {
    TrainMap map = buildTestMap();
    
    // Test 1: Given example
    List<Station> path = map.shortestPath("King's Cross St Pancras", "St Paul's");
    assertEquals(5, path.size());
    assertEquals("King's Cross St Pancras", path.get(0).getName());
    assertEquals("St Paul's", path.get(4).getName());
    
    // Test 2: Same station
    path = map.shortestPath("Bank", "Bank");
    assertEquals(1, path.size());
    
    // Test 3: Adjacent stations
    path = map.shortestPath("Angel", "Old Street");
    assertEquals(2, path.size());
    
    // Test 4: Non-existent station
    path = map.shortestPath("King's Cross St Pancras", "NonExistent");
    assertTrue(path.isEmpty());
    
    // Test 5: Reverse path
    path = map.shortestPath("St Paul's", "King's Cross St Pancras");
    assertEquals(5, path.size());
    
    // Test 6: Multiple shortest paths (any valid)
    path = map.shortestPath("King's Cross St Pancras", "Moorgate");
    assertEquals(4, path.size());
    
    // Test 7: Disconnected graph
    TrainMap disconnected = new TrainMap();
    disconnected.addStation("A").addStation("B");
    path = disconnected.shortestPath("A", "B");
    assertTrue(path.isEmpty());
}
```

---

## Visual Representation

### BFS Level-by-Level Exploration

```
Start: King's Cross St Pancras (KX)
Target: St Paul's (SP)

Level 0: [KX]

Level 1: [Angel, Farringdon, Russell Square]
         (neighbors of KX)

Level 2: [Old Street, Barbican, Holborn]
         (new neighbors from Level 1)

Level 3: [Moorgate, Chancery Lane]
         (new neighbors from Level 2)

Level 4: [Bank, St Paul's] ← Found!
         (new neighbors from Level 3)

Shortest path length: 4 hops (5 stations)
```

### Path Comparison

```
Path 1 (via Russell Square): 
  KX → RS → H → CL → SP
  Length: 4 hops ✓ (Shortest)

Path 2 (via Farringdon):
  KX → F → B → M → BK → SP
  Length: 5 hops

Path 3 (via Angel):
  KX → A → OS → M → BK → SP
  Length: 5 hops
```

---

## Edge Cases

1. **Same start and end:** Return single station
2. **Non-existent station:** Return empty list
3. **Disconnected graph:** Return empty list
4. **Single station network:** Return that station
5. **Multiple shortest paths:** Return any valid one
6. **Null station names:** Return empty list
7. **Cyclic graph:** BFS handles correctly

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| BFS | O(V + E) | O(V) | **Optimal for unweighted** |
| Bidirectional BFS | O(V + E) | O(V) | Faster in practice |
| DFS | O(V + E) | O(V) | Not guaranteed shortest |
| Dijkstra | O((V+E) log V) | O(V) | Overkill for unweighted |

**Where:**
- V = number of stations (vertices)
- E = number of connections (edges)

**Why BFS is Optimal:**
- Explores level by level
- First time reaching target = shortest path
- Unweighted graph → all edges equal

---

## Related Problems

1. **Word Ladder** - BFS on word transformations
2. **Shortest Path in Binary Matrix** - 2D grid BFS
3. **Minimum Knight Moves** - Chess board BFS
4. **Open the Lock** - State space BFS
5. **Snakes and Ladders** - Game board BFS
6. **Shortest Bridge** - Multi-source BFS

---

## Interview Tips

### Clarification Questions
1. Is the graph directed or undirected? (Undirected)
2. Are all edges equal weight? (Yes - unweighted)
3. Can there be cycles? (Yes, but BFS handles it)
4. What if no path exists? (Return empty list)
5. Multiple shortest paths? (Return any one)
6. Can station names be null? (Handle gracefully)

### Approach Explanation
1. "Unweighted graph → BFS is optimal"
2. "Use queue for level-by-level exploration"
3. "Track parent pointers to reconstruct path"
4. "Visited set prevents cycles"
5. "First time reaching target = shortest path"

### Common Mistakes
- Using DFS instead of BFS (won't guarantee shortest)
- Not tracking parent pointers (can't reconstruct path)
- Forgetting visited set (infinite loops)
- Not handling edge cases (null, same station)
- Marking visited when dequeuing instead of enqueuing

### Why Not DFS?
```
DFS might find: KX → A → OS → M → BK → SP (5 hops)
BFS finds: KX → RS → H → CL → SP (4 hops) ✓
```

---

## Real-World Applications

1. **Navigation Systems** - GPS shortest route
2. **Social Networks** - Degrees of separation
3. **Network Routing** - Packet routing
4. **Game AI** - Pathfinding in games
5. **Recommendation Systems** - Connection discovery
6. **Logistics** - Delivery route optimization

---

## Key Takeaways

1. **BFS for Unweighted:** Always use BFS for shortest path in unweighted graphs
2. **Queue Data Structure:** FIFO ensures level-by-level exploration
3. **Parent Tracking:** Essential for path reconstruction
4. **Visited Set:** Prevents revisiting and infinite loops
5. **Time Complexity:** O(V + E) - visit each vertex and edge once
6. **Space Complexity:** O(V) - queue, visited, parent map
7. **First Found = Shortest:** BFS guarantees shortest path on first discovery

---

## Additional Notes

### BFS vs Dijkstra

```
Unweighted Graph:
  BFS: O(V + E) ✓
  Dijkstra: O((V + E) log V) - unnecessary overhead

Weighted Graph:
  BFS: Incorrect results
  Dijkstra: O((V + E) log V) ✓
```

### Bidirectional BFS Optimization

Reduces search space from O(b^d) to O(b^(d/2)):
```
Regular BFS: Explore all nodes up to depth d
Bidirectional: Meet in middle at depth d/2

Example: b=3, d=6
  Regular: 3^6 = 729 nodes
  Bidirectional: 2 * 3^3 = 54 nodes
```

### When to Mark Visited

```
❌ Wrong: Mark when dequeuing
  while (!queue.isEmpty()) {
      Station s = queue.poll();
      visited.add(s); // Too late!
  }
  Problem: Same node added multiple times

✓ Correct: Mark when enqueuing
  if (!visited.contains(neighbor)) {
      visited.add(neighbor); // Immediately!
      queue.offer(neighbor);
  }
```

### Path Reconstruction Alternatives

**Method 1: Parent Map (Used above)**
```java
Map<Station, Station> parent;
// Reconstruct by following parent pointers
```

**Method 2: Store Full Path**
```java
Queue<List<Station>> queue;
// Each queue entry contains full path
// More space: O(V²) worst case
```

**Method 3: Distance + Backtrack**
```java
Map<Station, Integer> distance;
// Reconstruct by finding neighbors with distance-1
```

### Graph Representation Trade-offs

**Adjacency List (Used here):**
- Space: O(V + E)
- Get neighbors: O(1)
- Check edge: O(degree)

**Adjacency Matrix:**
- Space: O(V²)
- Get neighbors: O(V)
- Check edge: O(1)

For sparse graphs (like train networks), adjacency list is better.
