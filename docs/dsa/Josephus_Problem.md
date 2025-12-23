# Josephus Problem - Class President Election

## Problem Statement

A group of students are sitting in a circle. The teacher is electing a new class president by singing a song while walking around the circle. After the song is finished, the student at which the teacher stopped is removed from the circle.

Starting at the student next to the one that was just removed, the teacher resumes singing and walking around the circle. After the teacher is done singing, the next student is removed. The teacher repeats this until only one student is left.

**Parameters:**
- `n`: Number of students (numbered 1 to n)
- `k`: Song length (number of students the teacher walks past)
- Teacher starts at student 1

**Example:**
- Students: [1, 2, 3, 4]
- Song length: k = 2
- Elimination order: 2 → 4 → 3
- Winner: Student 1

## Solution Approaches

### Approach 1: Simulation with Queue (Intuitive)

**Time Complexity:** O(n × k)  
**Space Complexity:** O(n)

```java
public int findWinner(int n, int k) {
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 1; i <= n; i++) {
        queue.offer(i);
    }
    
    while (queue.size() > 1) {
        // Skip k-1 students
        for (int i = 0; i < k - 1; i++) {
            queue.offer(queue.poll());
        }
        // Remove the kth student
        queue.poll();
    }
    
    return queue.poll();
}
```

**How it works:**
1. Add all students to a queue
2. For each round:
   - Move k-1 students from front to back
   - Remove the kth student
3. Return the last remaining student

---

### Approach 2: Simulation with ArrayList

**Time Complexity:** O(n × k)  
**Space Complexity:** O(n)

```java
public int findWinner(int n, int k) {
    List<Integer> students = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
        students.add(i);
    }
    
    int index = 0;
    while (students.size() > 1) {
        index = (index + k - 1) % students.size();
        students.remove(index);
    }
    
    return students.get(0);
}
```

**How it works:**
1. Create list of students
2. Calculate next elimination index: `(currentIndex + k - 1) % size`
3. Remove student at that index
4. Continue until one student remains

---

### Approach 3: Mathematical Formula (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public int findWinner(int n, int k) {
    int position = 0;
    for (int i = 2; i <= n; i++) {
        position = (position + k) % i;
    }
    return position + 1; // Convert 0-indexed to 1-indexed
}
```

**Mathematical Insight:**

The Josephus problem has a recursive formula:
```
J(n, k) = (J(n-1, k) + k) % n
J(1, k) = 0  (base case, 0-indexed)
```

**Why this works:**
- When we eliminate one person, the problem reduces to n-1 people
- The position shifts by k in the new circle
- We use modulo to wrap around the circle

**Derivation:**
1. For n=1: Winner is at position 0 (0-indexed)
2. For n=2: Position = (0 + k) % 2
3. For n=3: Position = (previous + k) % 3
4. Continue building up to n

---

## Detailed Example Walkthrough

**Input:** n=4, k=2

### Simulation Approach:
```
Initial: [1, 2, 3, 4]
Index: 0

Round 1:
- Start at index 0 (student 1)
- Count k=2: 1 → 2
- Remove student 2
- Circle: [1, 3, 4]
- Next start: index 1 (student 3)

Round 2:
- Start at index 1 (student 3)
- Count k=2: 3 → 4
- Remove student 4
- Circle: [1, 3]
- Next start: index 0 (student 1)

Round 3:
- Start at index 0 (student 1)
- Count k=2: 1 → 3
- Remove student 3
- Circle: [1]

Winner: Student 1
```

### Mathematical Approach:
```
J(1, 2) = 0
J(2, 2) = (0 + 2) % 2 = 0
J(3, 2) = (0 + 2) % 3 = 2
J(4, 2) = (2 + 2) % 4 = 0

Result: 0 (0-indexed) → 1 (1-indexed)
Winner: Student 1 ✓
```

---

## Complete Implementation

```java
public class JosephusProblem {
    
    // Optimal: Mathematical formula
    public int findWinner(int n, int k) {
        int position = 0;
        for (int i = 2; i <= n; i++) {
            position = (position + k) % i;
        }
        return position + 1;
    }
    
    // Alternative: Queue simulation
    public int findWinnerQueue(int n, int k) {
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 1; i <= n; i++) {
            queue.offer(i);
        }
        
        while (queue.size() > 1) {
            for (int i = 0; i < k - 1; i++) {
                queue.offer(queue.poll());
            }
            queue.poll();
        }
        
        return queue.poll();
    }
    
    // Alternative: ArrayList simulation
    public int findWinnerList(int n, int k) {
        List<Integer> students = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            students.add(i);
        }
        
        int index = 0;
        while (students.size() > 1) {
            index = (index + k - 1) % students.size();
            students.remove(index);
        }
        
        return students.get(0);
    }
    
    // Recursive approach
    public int findWinnerRecursive(int n, int k) {
        return josephusRecursive(n, k) + 1;
    }
    
    private int josephusRecursive(int n, int k) {
        if (n == 1) return 0;
        return (josephusRecursive(n - 1, k) + k) % n;
    }
}
```

---

## Test Cases

```java
@Test
public void testJosephusProblem() {
    JosephusProblem solver = new JosephusProblem();
    
    // Example from problem
    assertEquals(1, solver.findWinner(4, 2));
    
    // Edge cases
    assertEquals(1, solver.findWinner(1, 1));  // Single student
    assertEquals(3, solver.findWinner(5, 2));  // Classic case
    assertEquals(4, solver.findWinner(7, 3));  // Larger circle
    
    // All approaches should give same result
    assertEquals(solver.findWinner(10, 3), 
                 solver.findWinnerQueue(10, 3));
    assertEquals(solver.findWinner(10, 3), 
                 solver.findWinnerList(10, 3));
    assertEquals(solver.findWinner(10, 3), 
                 solver.findWinnerRecursive(10, 3));
}
```

---

## Complexity Comparison

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Queue Simulation | O(n × k) | O(n) | Intuitive, easy to understand |
| ArrayList Simulation | O(n²) | O(n) | Remove operation is O(n) |
| Mathematical Formula | O(n) | O(1) | Optimal, requires understanding |
| Recursive | O(n) | O(n) | Stack space for recursion |

---

## Key Insights

1. **Pattern Recognition:** This is the classic Josephus Problem
2. **Optimization:** Mathematical formula eliminates need for simulation
3. **Index Management:** Careful with 0-indexed vs 1-indexed positions
4. **Modulo Arithmetic:** Essential for circular array operations

---

## Related Problems

- **LeetCode 1823:** Find the Winner of the Circular Game
- **Classic Josephus:** Historical problem with sword elimination
- **Circular Array Problems:** Any problem involving circular traversal

---

## Interview Tips

1. **Start with simulation** to show understanding
2. **Optimize to mathematical formula** if time permits
3. **Explain the recurrence relation** clearly
4. **Handle edge cases:** n=1, k=1, k>n
5. **Clarify indexing:** 0-based or 1-based

---

## Real-World Applications

- **Load Balancing:** Round-robin server selection
- **Game Theory:** Elimination games
- **Resource Allocation:** Fair distribution algorithms
- **Network Protocols:** Token ring networks

---

## References

- Original problem: Flavius Josephus (1st century AD)
- Mathematical analysis: Donald Knuth, "The Art of Computer Programming"
- Modern variant: LeetCode Problem 1823
