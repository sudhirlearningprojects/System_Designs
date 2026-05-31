# Agent Task Scheduler (Topological Sort)

## Problem

Given agent tasks with dependencies, find a valid execution order. If a cycle exists (impossible to schedule), return empty.

**Example:**
```
tasks = ["fetch_data", "process", "validate", "respond"]
dependencies = [("process", "fetch_data"), ("validate", "process"), ("respond", "validate")]
// "process" depends on "fetch_data" → fetch_data must run first
```
**Output:** `["fetch_data", "process", "validate", "respond"]`

---

## Solution: Kahn's Algorithm (BFS Topological Sort)

```java
List<String> scheduleAgentTasks(List<String> tasks, List<int[]> dependencies) {
    Map<String, List<String>> graph = new HashMap<>();
    Map<String, Integer> inDegree = new HashMap<>();

    for (String task : tasks) {
        graph.put(task, new ArrayList<>());
        inDegree.put(task, 0);
    }

    // dependencies[i] = [task, dependency] → dependency must run before task
    for (int[] dep : dependencies) {
        String task = tasks.get(dep[0]);
        String prerequisite = tasks.get(dep[1]);
        graph.get(prerequisite).add(task);
        inDegree.merge(task, 1, Integer::sum);
    }

    // Start with tasks that have no dependencies
    Queue<String> queue = new LinkedList<>();
    for (String task : tasks) {
        if (inDegree.get(task) == 0) queue.add(task);
    }

    List<String> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        String task = queue.poll();
        order.add(task);
        for (String next : graph.get(task)) {
            inDegree.merge(next, -1, Integer::sum);
            if (inDegree.get(next) == 0) queue.add(next);
        }
    }

    return order.size() == tasks.size() ? order : List.of(); // empty = cycle detected
}
```

---

## Cleaner Version (String-based dependencies)

```java
List<String> scheduleAgentTasks(String[] tasks, String[][] dependencies) {
    Map<String, List<String>> graph = new HashMap<>();
    Map<String, Integer> inDegree = new HashMap<>();

    for (String task : tasks) {
        graph.put(task, new ArrayList<>());
        inDegree.put(task, 0);
    }

    // dependencies[i] = [task, prerequisite]
    for (String[] dep : dependencies) {
        graph.get(dep[1]).add(dep[0]);
        inDegree.merge(dep[0], 1, Integer::sum);
    }

    Queue<String> queue = new LinkedList<>();
    for (String task : tasks) {
        if (inDegree.get(task) == 0) queue.add(task);
    }

    List<String> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        String task = queue.poll();
        order.add(task);
        for (String next : graph.get(task)) {
            if (inDegree.merge(next, -1, Integer::sum) == 0)
                queue.add(next);
        }
    }

    return order.size() == tasks.length ? order : List.of();
}
```

---

## Trace

**Input:**
```
tasks = ["fetch_data", "process", "validate", "respond"]
dependencies = [["process","fetch_data"], ["validate","process"], ["respond","validate"]]
```

**Build graph:**
```
fetch_data → [process]
process    → [validate]
validate   → [respond]
respond    → []
```

**In-degrees:** `fetch_data=0, process=1, validate=1, respond=1`

**BFS:**

| Step | Queue | Poll | Decrement | Order |
|------|-------|------|-----------|-------|
| 1 | [fetch_data] | fetch_data | process→0 | [fetch_data] |
| 2 | [process] | process | validate→0 | [fetch_data, process] |
| 3 | [validate] | validate | respond→0 | [fetch_data, process, validate] |
| 4 | [respond] | respond | — | [fetch_data, process, validate, respond] |

**Result:** `["fetch_data", "process", "validate", "respond"]` ✅

---

## Cycle Detection Example

**Input:**
```
tasks = ["A", "B", "C"]
dependencies = [["A","B"], ["B","C"], ["C","A"]]  // A→B→C→A (cycle!)
```

**In-degrees:** `A=1, B=1, C=1` — no task has in-degree 0.

Queue starts empty → order is empty → `order.size() != tasks.length` → return `[]`

---

## LeetCode Variant: Course Schedule II (Problem 210)

Same problem with integer tasks:

```java
int[] findOrder(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    int[] inDegree = new int[numCourses];

    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());

    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);
        inDegree[pre[0]]++;
    }

    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++)
        if (inDegree[i] == 0) queue.add(i);

    int[] order = new int[numCourses];
    int idx = 0;
    while (!queue.isEmpty()) {
        int course = queue.poll();
        order[idx++] = course;
        for (int next : graph.get(course))
            if (--inDegree[next] == 0) queue.add(next);
    }

    return idx == numCourses ? order : new int[0];
}
```

---

## Complexity

| | Time | Space |
|--|------|-------|
| Build graph | O(V + E) | O(V + E) |
| BFS | O(V + E) | O(V) |
| **Total** | **O(V + E)** | **O(V + E)** |

Where V = number of tasks, E = number of dependencies.

---

## When to Use

- Task scheduling with dependencies (CI/CD pipelines, build systems)
- AI agent workflow execution (multi-step plans)
- Course prerequisite ordering
- Package dependency resolution
- Compilation order in build tools (Make, Gradle)

---

## Interview Template (Give to Candidate)

```java
import java.util.*;

/**
 * Problem: Agent Task Scheduler
 * 
 * You are building an AI agent orchestration system. The agent has multiple tasks
 * to execute, but some tasks depend on others being completed first.
 * 
 * Given:
 *   - tasks: list of task names
 *   - dependencies: list of [task, prerequisite] pairs
 *     meaning "task" cannot start until "prerequisite" is done
 * 
 * Return: A valid execution order. If no valid order exists (cycle), return empty list.
 * 
 * Example:
 *   tasks = ["fetch_data", "process", "validate", "respond"]
 *   dependencies = [["process","fetch_data"], ["validate","process"], ["respond","validate"]]
 *   Output: ["fetch_data", "process", "validate", "respond"]
 * 
 * Example (cycle):
 *   tasks = ["A", "B", "C"]
 *   dependencies = [["A","B"], ["B","C"], ["C","A"]]
 *   Output: []
 * 
 * Constraints:
 *   - 1 <= tasks.length <= 10^4
 *   - 0 <= dependencies.length <= 10^4
 *   - All task names are unique
 */
public class AgentTaskScheduler {

    public List<String> schedule(String[] tasks, String[][] dependencies) {
        // Step 1: Build adjacency list and in-degree map
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (String task : tasks) {
            graph.put(task, new ArrayList<>());
            inDegree.put(task, 0);
        }

        for (String[] dep : dependencies) {
            String task = dep[0];         // this task...
            String prerequisite = dep[1]; // ...depends on this
            graph.get(prerequisite).add(task);
            inDegree.merge(task, 1, Integer::sum);
        }

        // ============================================================
        // TODO: Implement the scheduling logic
        // 
        // Hints:
        //   - Which tasks can start immediately? (no dependencies)
        //   - Once a task completes, what happens to tasks depending on it?
        //   - How do you detect if scheduling is impossible?
        //
        // You have:
        //   - graph: adjacency list (prerequisite → list of dependent tasks)
        //   - inDegree: map of task → number of unmet dependencies
        //
        // Return: ordered list of tasks, or empty list if cycle exists
        // ============================================================

        List<String> order = new ArrayList<>();

        // YOUR CODE HERE

        return order;
    }

    // ==================== DO NOT MODIFY BELOW ====================

    public static void main(String[] args) {
        AgentTaskScheduler scheduler = new AgentTaskScheduler();

        // Test 1: Reverse input order — output must be completely reversed
        String[] tasks1 = {"respond", "validate", "process", "fetch_data"};
        String[][] deps1 = {{"process","fetch_data"}, {"validate","process"}, {"respond","validate"}};
        System.out.println("Test 1: " + scheduler.schedule(tasks1, deps1));
        // Expected: [fetch_data, process, validate, respond] (reversed from input!)

        // Test 2: Complex DAG — last task in input must come first
        String[] tasks2 = {"deploy", "test", "build", "lint", "install"};
        String[][] deps2 = {{"lint","install"}, {"build","lint"}, {"test","build"}, {"deploy","test"}, {"deploy","lint"}};
        System.out.println("Test 2: " + scheduler.schedule(tasks2, deps2));
        // Expected: [install, lint, build, test, deploy]

        // Test 3: Partial cycle — some tasks schedulable, some not
        String[] tasks3 = {"A", "B", "C", "D", "E"};
        String[][] deps3 = {{"B","A"}, {"C","B"}, {"B","C"}, {"D","A"}, {"E","D"}};
        System.out.println("Test 3: " + scheduler.schedule(tasks3, deps3));
        // Expected: [] (B↔C cycle makes full schedule impossible)

        // Test 4: Wide fan-in — many tasks feed into one
        String[] tasks4 = {"final", "X", "Y", "Z", "W"};
        String[][] deps4 = {{"final","X"}, {"final","Y"}, {"final","Z"}, {"final","W"}};
        System.out.println("Test 4: " + scheduler.schedule(tasks4, deps4));
        // Expected: X,Y,Z,W in any order, then final (first element in input comes LAST)

        // Test 5: Interleaved dependencies — non-obvious ordering
        String[] tasks5 = {"E", "D", "C", "B", "A", "F"};
        String[][] deps5 = {{"E","B"}, {"E","D"}, {"D","A"}, {"C","A"}, {"B","A"}, {"F","E"}, {"F","C"}};
        System.out.println("Test 5: " + scheduler.schedule(tasks5, deps5));
        // Expected: A first, then B/C/D (any order), then E, then F
        // Input starts with E,D,C but they all depend on A which is near the end of input

        // Test 6: Single task with self-dependency (edge case)
        String[] tasks6 = {"loop"};
        String[][] deps6 = {{"loop","loop"}};
        System.out.println("Test 6: " + scheduler.schedule(tasks6, deps6));
        // Expected: [] (self-cycle)
    }
}
```

---

## Expected Solution (What Candidate Should Write)

```java
// Fill in the "YOUR CODE HERE" section:

Queue<String> queue = new LinkedList<>();
for (String task : tasks) {
    if (inDegree.get(task) == 0) queue.add(task);
}

while (!queue.isEmpty()) {
    String task = queue.poll();
    order.add(task);
    for (String next : graph.get(task)) {
        if (inDegree.merge(next, -1, Integer::sum) == 0)
            queue.add(next);
    }
}

return order.size() == tasks.length ? order : List.of();
```

---

## Evaluation Rubric

| Criteria | Strong | Acceptable | Weak |
|----------|--------|------------|------|
| Identifies BFS/topo-sort | Immediately | With hints | Cannot identify |
| Starts from 0 in-degree | Without prompting | After "which tasks can start?" | Needs explicit hint |
| Decrements in-degree | Correct | Off-by-one but self-corrects | Incorrect |
| Cycle detection | Checks order size vs task count | Mentions but doesn't implement | Misses entirely |
| Time complexity | States O(V+E) unprompted | Correct when asked | Incorrect |

---

## Follow-up Questions

1. **"What if we want tasks at the same level to run in parallel? Return grouped by levels."**
   - Use level-order BFS (process entire queue per level)

2. **"What if we want lexicographically smallest order?"**
   - Replace `LinkedList` queue with `PriorityQueue`

3. **"What's the minimum time to complete all tasks if each takes 1 unit and parallel execution is allowed?"**
   - Number of BFS levels = critical path length

4. **"How would you handle dynamic dependency additions at runtime?"**
   - Incremental topo-sort or re-run with updated graph
