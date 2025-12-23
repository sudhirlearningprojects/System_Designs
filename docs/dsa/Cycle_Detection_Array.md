# Cycle Detection in Array - Find Cycle Length

## Problem Statement

Given an integer array where each element points to an index, start from a given index and follow the chain until you find a cycle. Return the length of the cycle, or -1 if no cycle exists.

**Input:** Integer array and start index  
**Output:** Length of cycle, or -1 if no cycle

**Examples:**
```
arr = [1, 0], startIndex = 0
Path: 0 → 1 → 0 (cycle found)
Cycle length: 2

arr = [1, 2, 0], startIndex = 0
Path: 0 → 1 → 2 → 0 (cycle found)
Cycle length: 3

arr = [1, 2, 3], startIndex = 0
Path: 0 → 1 → 2 → 3 → out of bounds
Cycle length: -1
```

---

## Solution Approach

### Optimal: HashMap to Track Visited Indices

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static int countLengthOfCycle(int[] arr, int startIndex) {
    Map<Integer, Integer> visited = new HashMap<>();
    int currentIndex = startIndex;
    int step = 0;
    
    while (currentIndex >= 0 && currentIndex < arr.length) {
        if (visited.containsKey(currentIndex)) {
            return step - visited.get(currentIndex);
        }
        
        visited.put(currentIndex, step);
        currentIndex = arr[currentIndex];
        step++;
    }
    
    return -1;
}
```

---

## Algorithm Explanation

### Key Insight

- Store each visited index with its step number
- When we revisit an index, cycle length = current step - previous step
- If we go out of bounds, no cycle exists

### Visual Walkthrough

**Example 1: [1, 0], startIndex = 0**
```
Step 0: currentIndex = 0
  visited = {0: 0}
  next = arr[0] = 1

Step 1: currentIndex = 1
  visited = {0: 0, 1: 1}
  next = arr[1] = 0

Step 2: currentIndex = 0
  0 is in visited!
  Cycle length = 2 - 0 = 2

Path: 0 → 1 → 0
      └─────┘
      cycle
```

**Example 2: [1, 2, 0], startIndex = 0**
```
Step 0: currentIndex = 0
  visited = {0: 0}
  next = arr[0] = 1

Step 1: currentIndex = 1
  visited = {0: 0, 1: 1}
  next = arr[1] = 2

Step 2: currentIndex = 2
  visited = {0: 0, 1: 1, 2: 2}
  next = arr[2] = 0

Step 3: currentIndex = 0
  0 is in visited!
  Cycle length = 3 - 0 = 3

Path: 0 → 1 → 2 → 0
      └──────────┘
         cycle
```

**Example 3: [1, 2, 3], startIndex = 0**
```
Step 0: currentIndex = 0
  visited = {0: 0}
  next = arr[0] = 1

Step 1: currentIndex = 1
  visited = {0: 0, 1: 1}
  next = arr[1] = 2

Step 2: currentIndex = 2
  visited = {0: 0, 1: 1, 2: 2}
  next = arr[2] = 3

Step 3: currentIndex = 3
  3 >= arr.length (out of bounds)
  Return -1 (no cycle)
```

---

## Complete Implementation

```java
public class CycleDetection {
    
    // Approach 1: HashMap (Recommended)
    public static int countLengthOfCycle(int[] arr, int startIndex) {
        if (arr == null || startIndex < 0 || startIndex >= arr.length) {
            return -1;
        }
        
        Map<Integer, Integer> visited = new HashMap<>();
        int currentIndex = startIndex;
        int step = 0;
        
        while (currentIndex >= 0 && currentIndex < arr.length) {
            if (visited.containsKey(currentIndex)) {
                return step - visited.get(currentIndex);
            }
            
            visited.put(currentIndex, step);
            currentIndex = arr[currentIndex];
            step++;
        }
        
        return -1;
    }
    
    // Approach 2: Floyd's Cycle Detection (Tortoise and Hare)
    public static int countLengthOfCycleFloyd(int[] arr, int startIndex) {
        if (arr == null || startIndex < 0 || startIndex >= arr.length) {
            return -1;
        }
        
        // Phase 1: Detect if cycle exists
        int slow = startIndex;
        int fast = startIndex;
        
        while (true) {
            // Move slow one step
            if (slow < 0 || slow >= arr.length) return -1;
            slow = arr[slow];
            
            // Move fast two steps
            if (fast < 0 || fast >= arr.length) return -1;
            fast = arr[fast];
            if (fast < 0 || fast >= arr.length) return -1;
            fast = arr[fast];
            
            if (slow == fast) break;
        }
        
        // Phase 2: Find cycle length
        int length = 1;
        fast = arr[slow];
        while (fast != slow) {
            if (fast < 0 || fast >= arr.length) return -1;
            fast = arr[fast];
            length++;
        }
        
        return length;
    }
    
    // Approach 3: Using Set (simpler but doesn't track steps)
    public static int countLengthOfCycleSet(int[] arr, int startIndex) {
        if (arr == null || startIndex < 0 || startIndex >= arr.length) {
            return -1;
        }
        
        Set<Integer> visited = new HashSet<>();
        int currentIndex = startIndex;
        
        // Find cycle start
        while (currentIndex >= 0 && currentIndex < arr.length) {
            if (visited.contains(currentIndex)) {
                break;
            }
            visited.add(currentIndex);
            currentIndex = arr[currentIndex];
        }
        
        if (currentIndex < 0 || currentIndex >= arr.length) {
            return -1;
        }
        
        // Count cycle length
        int cycleStart = currentIndex;
        int length = 1;
        currentIndex = arr[currentIndex];
        
        while (currentIndex != cycleStart) {
            if (currentIndex < 0 || currentIndex >= arr.length) {
                return -1;
            }
            currentIndex = arr[currentIndex];
            length++;
        }
        
        return length;
    }
    
    // Bonus: Find cycle start index
    public static int findCycleStart(int[] arr, int startIndex) {
        Map<Integer, Integer> visited = new HashMap<>();
        int currentIndex = startIndex;
        int step = 0;
        
        while (currentIndex >= 0 && currentIndex < arr.length) {
            if (visited.containsKey(currentIndex)) {
                return currentIndex;
            }
            visited.put(currentIndex, step);
            currentIndex = arr[currentIndex];
            step++;
        }
        
        return -1;
    }
    
    // Bonus: Get full cycle path
    public static List<Integer> getCyclePath(int[] arr, int startIndex) {
        Map<Integer, Integer> visited = new HashMap<>();
        List<Integer> path = new ArrayList<>();
        int currentIndex = startIndex;
        int step = 0;
        
        while (currentIndex >= 0 && currentIndex < arr.length) {
            if (visited.containsKey(currentIndex)) {
                // Extract cycle
                int cycleStart = visited.get(currentIndex);
                return path.subList(cycleStart, path.size());
            }
            
            visited.put(currentIndex, step);
            path.add(currentIndex);
            currentIndex = arr[currentIndex];
            step++;
        }
        
        return new ArrayList<>();
    }
}
```

---

## Test Cases

```java
@Test
public void testCycleDetection() {
    // Basic cycles
    assertEquals(2, countLengthOfCycle(new int[]{1, 0}, 0));
    assertEquals(3, countLengthOfCycle(new int[]{1, 2, 0}, 0));
    
    // Self-loop
    assertEquals(1, countLengthOfCycle(new int[]{0}, 0));
    
    // No cycle (out of bounds)
    assertEquals(-1, countLengthOfCycle(new int[]{1, 2, 3}, 0));
    assertEquals(-1, countLengthOfCycle(new int[]{2, 3, 4}, 0));
    
    // Negative index (out of bounds)
    assertEquals(-1, countLengthOfCycle(new int[]{-1}, 0));
    
    // Cycle not from start
    assertEquals(2, countLengthOfCycle(new int[]{1, 2, 1}, 0));
    
    // Larger cycle
    assertEquals(4, countLengthOfCycle(new int[]{1, 2, 3, 0}, 0));
    
    // Different start index
    assertEquals(2, countLengthOfCycle(new int[]{1, 2, 1}, 1));
    
    // Invalid start index
    assertEquals(-1, countLengthOfCycle(new int[]{1, 0}, -1));
    assertEquals(-1, countLengthOfCycle(new int[]{1, 0}, 5));
    
    // Empty array
    assertEquals(-1, countLengthOfCycle(new int[]{}, 0));
}
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[0], 0` | `1` | Self-loop |
| `[1, 0], 0` | `2` | Simple cycle |
| `[1, 2, 3], 0` | `-1` | Out of bounds |
| `[-1], 0` | `-1` | Negative index |
| `[], 0` | `-1` | Empty array |
| `[1, 0], 5` | `-1` | Invalid start |
| `[1, 2, 1], 0` | `2` | Cycle not including start |

---

## Visual Representation

```
Example: [1, 2, 3, 1, 4], startIndex = 0

Array indices: 0  1  2  3  4
Array values: [1, 2, 3, 1, 4]

Path:
0 → arr[0]=1 → arr[1]=2 → arr[2]=3 → arr[3]=1
                          ↑                 ↓
                          └─────────────────┘
                               cycle (length 3)

Visited map:
{0: 0, 1: 1, 2: 2, 3: 3}

When we reach index 1 again at step 4:
Cycle length = 4 - 1 = 3
```

---

## Common Mistakes

1. **Not Checking Bounds:**
   ```java
   // WRONG - can cause ArrayIndexOutOfBoundsException
   currentIndex = arr[currentIndex];
   
   // CORRECT
   while (currentIndex >= 0 && currentIndex < arr.length) {
       // ...
   }
   ```

2. **Using Set Instead of Map:**
   ```java
   // WRONG - can't calculate cycle length
   Set<Integer> visited = new HashSet<>();
   
   // CORRECT - need step numbers
   Map<Integer, Integer> visited = new HashMap<>();
   ```

3. **Not Handling Invalid Start:**
   ```java
   if (startIndex < 0 || startIndex >= arr.length) {
       return -1;
   }
   ```

4. **Infinite Loop Without Bounds Check:**
   ```java
   // Add bounds check in while condition
   while (currentIndex >= 0 && currentIndex < arr.length)
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap | O(n) | O(n) | Best for single query |
| Floyd's | O(n) | O(1) | Space-efficient |
| Set + Count | O(n) | O(n) | Two passes |

**Where n = array length**

---

## Floyd's Cycle Detection Explained

```
Phase 1: Detect cycle
  slow moves 1 step
  fast moves 2 steps
  If they meet, cycle exists

Phase 2: Find cycle length
  Keep one pointer at meeting point
  Move other pointer until they meet again
  Count steps = cycle length

Example: [1, 2, 3, 1]
  slow: 0 → 1 → 2 → 3 → 1 → 2
  fast: 0 → 2 → 1 → 3 → 2 → 1
                    ↑
                  meet at 1
  
  Count from 1: 1 → 2 → 3 → 1 (3 steps)
```

---

## Related Problems

- **LeetCode 141:** Linked List Cycle
- **LeetCode 142:** Linked List Cycle II
- **LeetCode 287:** Find the Duplicate Number
- **LeetCode 202:** Happy Number

---

## Interview Tips

1. **Clarify Requirements:**
   - What if array has negative values?
   - What if start index is invalid?
   - Can array be empty?
   - What defines a cycle?

2. **Start with HashMap:**
   - Track visited indices with steps
   - Easy to understand and implement

3. **Mention Floyd's Algorithm:**
   - O(1) space optimization
   - Classic cycle detection

4. **Walk Through Example:**
   - Use [1, 2, 0]
   - Show visited map

5. **Handle Edge Cases:**
   - Out of bounds
   - Self-loop
   - Invalid start

---

## Real-World Applications

- **Linked List Cycle Detection:** Same concept
- **Dependency Resolution:** Circular dependencies
- **Graph Algorithms:** Cycle detection in directed graphs
- **Memory Management:** Detecting circular references
- **State Machines:** Loop detection
- **Network Routing:** Routing loop detection

---

## Why HashMap Over Set?

```
Set approach:
1. Find where cycle starts (first revisit)
2. Count cycle length from that point
→ Two passes needed

HashMap approach:
1. Store index with step number
2. When revisit: cycle length = current step - stored step
→ Single pass, more efficient
```

---

## Optimization: Early Termination

```java
public static int countLengthOfCycle(int[] arr, int startIndex) {
    Map<Integer, Integer> visited = new HashMap<>();
    int currentIndex = startIndex;
    int step = 0;
    
    // Early termination if we've visited more than array length
    while (currentIndex >= 0 && currentIndex < arr.length && step <= arr.length) {
        if (visited.containsKey(currentIndex)) {
            return step - visited.get(currentIndex);
        }
        
        visited.put(currentIndex, step);
        currentIndex = arr[currentIndex];
        step++;
    }
    
    return -1;
}
```

---

## Key Takeaways

✅ Use HashMap to track visited indices with step numbers  
✅ Cycle length = current step - previous step at revisited index  
✅ Check bounds: `currentIndex >= 0 && currentIndex < arr.length`  
✅ Return -1 if out of bounds (no cycle)  
✅ O(n) time, O(n) space - optimal for single query  
✅ Floyd's algorithm for O(1) space (more complex)  
✅ Handle edge cases: empty array, invalid start, self-loop
