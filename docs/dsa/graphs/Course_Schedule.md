# Course Schedule - Dependency Graph

## Problem Statement

**Real-World Context**: Universities use this to validate course prerequisites. Build systems (Maven, Gradle) use it to determine compilation order.

Given a list of courses and their prerequisites, determine if it's possible to complete all courses. A course can only be taken after completing all its prerequisites.

**Input**: 
- `numCourses`: Total number of courses (labeled 0 to n-1)
- `prerequisites`: Array of [course, prerequisite] pairs

**Output**: 
- `true` if all courses can be completed
- `false` if there's a circular dependency

**Examples:**
```
Example 1:
Input: numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
Output: true
Explanation: 
  Course 0 → no prerequisites
  Course 1 → requires Course 0
  Course 2 → requires Course 0
  Course 3 → requires Courses 1 and 2
  Valid order: [0, 1, 2, 3] or [0, 2, 1, 3]

Example 2:
Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
Output: false
Explanation: Circular dependency (0 → 1 → 0)

Example 3:
Input: numCourses = 3, prerequisites = [[1,0],[2,1],[0,2]]
Output: false
Explanation: Circular dependency (0 → 2 → 1 → 0)
```

---

## Solution Approaches

### Approach 1: DFS with Cycle Detection (Optimal)

**Algorithm**: Use DFS with three states to detect cycles
- **WHITE (0)**: Unvisited
- **GRAY (1)**: Currently visiting (in recursion stack)
- **BLACK (2)**: Completely visited

If we encounter a GRAY node during DFS, there's a cycle.

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public boolean canFinish(int numCourses, int[][] prerequisites) {
    Map<Integer, List<Integer>> graph = new HashMap<>();
    
    // Build adjacency list
    for (int i = 0; i < numCourses; i++) {
        graph.put(i, new ArrayList<>());
    }
    for (int[] prereq : prerequisites) {
        graph.get(prereq[0]).add(prereq[1]);
    }
    
    int[] state = new int[numCourses]; // 0=WHITE, 1=GRAY, 2=BLACK
    
    // Check each course for cycles
    for (int course = 0; course < numCourses; course++) {
        if (hasCycle(course, graph, state)) {
            return false;
        }
    }
    
    return true;
}

private boolean hasCycle(int course, Map<Integer, List<Integer>> graph, int[] state) {
    if (state[course] == 1) return true;  // GRAY - cycle detected
    if (state[course] == 2) return false; // BLACK - already processed
    
    state[course] = 1; // Mark as GRAY (visiting)
    
    for (int prereq : graph.get(course)) {
        if (hasCycle(prereq, graph, state)) {
            return true;
        }
    }
    
    state[course] = 2; // Mark as BLACK (visited)
    return false;
}
```

---

### Approach 2: BFS with Kahn's Algorithm (Topological Sort)

**Algorithm**: Use in-degree counting
1. Calculate in-degree for each node
2. Start with nodes having in-degree 0
3. Process nodes and reduce in-degree of neighbors
4. If all nodes processed → valid, else cycle exists

**Time Complexity:** O(V + E)  
**Space Complexity:** O(V)

```java
public boolean canFinishBFS(int numCourses, int[][] prerequisites) {
    Map<Integer, List<Integer>> graph = new HashMap<>();
    int[] inDegree = new int[numCourses];
    
    // Build graph and calculate in-degrees
    for (int i = 0; i < numCourses; i++) {
        graph.put(i, new ArrayList<>());
    }
    for (int[] prereq : prerequisites) {
        graph.get(prereq[1]).add(prereq[0]);
        inDegree[prereq[0]]++;
    }
    
    // Start with courses having no prerequisites
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (inDegree[i] == 0) {
            queue.offer(i);
        }
    }
    
    int completed = 0;
    while (!queue.isEmpty()) {
        int course = queue.poll();
        completed++;
        
        for (int next : graph.get(course)) {
            inDegree[next]--;
            if (inDegree[next] == 0) {
                queue.offer(next);
            }
        }
    }
    
    return completed == numCourses;
}
```

---

## Complete Implementation with Course Order

```java
import java.util.*;

public class CourseSchedule {
    
    // Problem 1: Can finish all courses?
    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = buildGraph(numCourses, prerequisites);
        int[] state = new int[numCourses];
        
        for (int course = 0; course < numCourses; course++) {
            if (hasCycle(course, graph, state)) {
                return false;
            }
        }
        return true;
    }
    
    // Problem 2: Find valid course order (Topological Sort)
    public static int[] findOrder(int numCourses, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        int[] inDegree = new int[numCourses];
        
        for (int i = 0; i < numCourses; i++) {
            graph.put(i, new ArrayList<>());
        }
        
        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
            inDegree[prereq[0]]++;
        }
        
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        int[] order = new int[numCourses];
        int index = 0;
        
        while (!queue.isEmpty()) {
            int course = queue.poll();
            order[index++] = course;
            
            for (int next : graph.get(course)) {
                inDegree[next]--;
                if (inDegree[next] == 0) {
                    queue.offer(next);
                }
            }
        }
        
        return index == numCourses ? order : new int[0];
    }
    
    // Problem 3: Find all possible orders
    public static List<List<Integer>> findAllOrders(int numCourses, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        int[] inDegree = new int[numCourses];
        
        for (int i = 0; i < numCourses; i++) {
            graph.put(i, new ArrayList<>());
        }
        
        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
            inDegree[prereq[0]]++;
        }
        
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        boolean[] visited = new boolean[numCourses];
        
        backtrack(graph, inDegree.clone(), visited, current, result, numCourses);
        return result;
    }
    
    private static void backtrack(Map<Integer, List<Integer>> graph, int[] inDegree,
                                   boolean[] visited, List<Integer> current,
                                   List<List<Integer>> result, int numCourses) {
        if (current.size() == numCourses) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int course = 0; course < numCourses; course++) {
            if (!visited[course] && inDegree[course] == 0) {
                visited[course] = true;
                current.add(course);
                
                // Reduce in-degree of neighbors
                for (int next : graph.get(course)) {
                    inDegree[next]--;
                }
                
                backtrack(graph, inDegree, visited, current, result, numCourses);
                
                // Backtrack
                for (int next : graph.get(course)) {
                    inDegree[next]++;
                }
                current.remove(current.size() - 1);
                visited[course] = false;
            }
        }
    }
    
    // Helper: Build adjacency list
    private static Map<Integer, List<Integer>> buildGraph(int numCourses, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        for (int i = 0; i < numCourses; i++) {
            graph.put(i, new ArrayList<>());
        }
        for (int[] prereq : prerequisites) {
            graph.get(prereq[0]).add(prereq[1]);
        }
        return graph;
    }
    
    // Helper: DFS cycle detection
    private static boolean hasCycle(int course, Map<Integer, List<Integer>> graph, int[] state) {
        if (state[course] == 1) return true;
        if (state[course] == 2) return false;
        
        state[course] = 1;
        for (int prereq : graph.get(course)) {
            if (hasCycle(prereq, graph, state)) {
                return true;
            }
        }
        state[course] = 2;
        return false;
    }
    
    // Test cases
    public static boolean doTestsPass() {
        // Test 1: Valid schedule
        int[][] prereqs1 = {{1,0},{2,0},{3,1},{3,2}};
        if (!canFinish(4, prereqs1)) return false;
        
        // Test 2: Circular dependency
        int[][] prereqs2 = {{1,0},{0,1}};
        if (canFinish(2, prereqs2)) return false;
        
        // Test 3: Find order
        int[] order = findOrder(4, prereqs1);
        if (order.length != 4) return false;
        
        // Test 4: Complex cycle
        int[][] prereqs3 = {{1,0},{2,1},{0,2}};
        if (canFinish(3, prereqs3)) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("✓ All tests pass\n");
        } else {
            System.out.println("✗ Tests fail\n");
        }
        
        // Demo: University course scheduling
        System.out.println("=== University Course Scheduler ===\n");
        
        int numCourses = 6;
        int[][] prerequisites = {
            {1, 0},  // Course 1 requires Course 0
            {2, 0},  // Course 2 requires Course 0
            {3, 1},  // Course 3 requires Course 1
            {3, 2},  // Course 3 requires Course 2
            {4, 3},  // Course 4 requires Course 3
            {5, 3}   // Course 5 requires Course 3
        };
        
        System.out.println("Courses: 6");
        System.out.println("Prerequisites:");
        for (int[] p : prerequisites) {
            System.out.println("  Course " + p[0] + " requires Course " + p[1]);
        }
        
        boolean canComplete = canFinish(numCourses, prerequisites);
        System.out.println("\nCan complete all courses? " + canComplete);
        
        if (canComplete) {
            int[] order = findOrder(numCourses, prerequisites);
            System.out.println("Valid course order: " + Arrays.toString(order));
            
            List<List<Integer>> allOrders = findAllOrders(numCourses, prerequisites);
            System.out.println("\nAll possible orders (" + allOrders.size() + " total):");
            for (int i = 0; i < Math.min(5, allOrders.size()); i++) {
                System.out.println("  " + allOrders.get(i));
            }
        }
        
        // Example with cycle
        System.out.println("\n=== Example with Circular Dependency ===\n");
        int[][] cyclicPrereqs = {{1,0},{2,1},{0,2}};
        System.out.println("Prerequisites: " + Arrays.deepToString(cyclicPrereqs));
        System.out.println("Can complete? " + canFinish(3, cyclicPrereqs));
        System.out.println("Reason: Circular dependency detected (0→2→1→0)");
    }
}
```

---

## Algorithm Walkthrough

### Example: Valid Schedule
```
Courses: 4
Prerequisites: [[1,0], [2,0], [3,1], [3,2]]

Graph representation:
  0 → (no prerequisites)
  1 → 0
  2 → 0
  3 → 1, 2

Step-by-step (Kahn's Algorithm):

Initial state:
  inDegree = [0, 1, 1, 2]
  queue = [0]  (courses with inDegree 0)

Iteration 1:
  Process course 0
  Reduce inDegree of neighbors: 1, 2
  inDegree = [0, 0, 0, 2]
  queue = [1, 2]
  order = [0]

Iteration 2:
  Process course 1
  Reduce inDegree of neighbor: 3
  inDegree = [0, 0, 0, 1]
  queue = [2]
  order = [0, 1]

Iteration 3:
  Process course 2
  Reduce inDegree of neighbor: 3
  inDegree = [0, 0, 0, 0]
  queue = [3]
  order = [0, 1, 2]

Iteration 4:
  Process course 3
  queue = []
  order = [0, 1, 2, 3]

Result: All 4 courses processed → Valid schedule
```

### Example: Circular Dependency
```
Courses: 3
Prerequisites: [[1,0], [2,1], [0,2]]

Graph:
  0 → 2
  1 → 0
  2 → 1

DFS Cycle Detection:

Start DFS from course 0:
  state[0] = GRAY
  Visit prerequisite 2:
    state[2] = GRAY
    Visit prerequisite 1:
      state[1] = GRAY
      Visit prerequisite 0:
        state[0] == GRAY → CYCLE DETECTED!

Result: Cannot complete courses
```

---

## Complexity Analysis

| Operation | Time | Space |
|-----------|------|-------|
| Build graph | O(E) | O(V + E) |
| DFS cycle detection | O(V + E) | O(V) |
| BFS topological sort | O(V + E) | O(V) |
| Find all orders | O(V! × E) | O(V) |

---

## Real-World Applications

1. **University Systems**: Course prerequisite validation
2. **Build Tools**: Maven, Gradle dependency resolution
3. **Package Managers**: npm, pip dependency ordering
4. **Task Scheduling**: Project management (PERT/CPM)
5. **Compiler**: Symbol resolution order
6. **Database**: Foreign key constraint validation

---

## Extensions

### 1. Minimum Semesters
```java
public int minimumSemesters(int numCourses, int[][] prerequisites) {
    // Use BFS level-order traversal
    // Each level = one semester
}
```

### 2. Course with Maximum Prerequisites
```java
public int courseWithMaxPrereqs(int numCourses, int[][] prerequisites) {
    int[] inDegree = new int[numCourses];
    for (int[] p : prerequisites) {
        inDegree[p[0]]++;
    }
    return IntStream.range(0, numCourses)
        .boxed()
        .max(Comparator.comparingInt(i -> inDegree[i]))
        .orElse(-1);
}
```

### 3. Parallel Course Scheduling
```java
// Courses that can be taken in parallel (same level in topological sort)
public List<List<Integer>> parallelSchedule(int numCourses, int[][] prerequisites) {
    // BFS with level tracking
}
```
