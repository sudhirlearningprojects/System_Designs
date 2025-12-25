# Detect Loop in Linked List

## Problem Statement

Given a linked list, check if the linked list has a loop (cycle) or not. A loop exists if a node's next pointer points back to a previous node in the list.

**Input:** Head of a linked list  
**Output:** true if loop exists, false otherwise

**Examples:**
```
Input:  1 → 2 → 3 → 4 → 5 → NULL
Output: false
Explanation: No loop, list ends at NULL

Input:  1 → 2 → 3 → 4 → 5
            ↑           ↓
            └───────────┘
Output: true
Explanation: Node 5 points back to node 2

Input:  1 → 2
        ↑   ↓
        └───┘
Output: true
Explanation: Node 2 points back to node 1

Input:  1 ⟲
Output: true
Explanation: Node 1 points to itself

Input:  NULL
Output: false
Explanation: Empty list has no loop
```

---

## Solution Approaches

### Approach 1: Floyd's Cycle Detection (Optimal)

**Also known as:** Tortoise and Hare Algorithm

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean hasLoop(Node head) {
    if (head == null || head.next == null) return false;
    
    Node slow = head;
    Node fast = head;
    
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        
        if (slow == fast) {
            return true;
        }
    }
    
    return false;
}
```

---

### Approach 2: HashSet

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static boolean hasLoopHashSet(Node head) {
    Set<Node> visited = new HashSet<>();
    Node current = head;
    
    while (current != null) {
        if (visited.contains(current)) {
            return true;
        }
        visited.add(current);
        current = current.next;
    }
    
    return false;
}
```

---

### Approach 3: Modify Node (Not Recommended)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static boolean hasLoopModify(Node head) {
    Node current = head;
    Node temp = new Node(-1);
    
    while (current != null) {
        if (current.next == temp) {
            return true;
        }
        Node next = current.next;
        current.next = temp;
        current = next;
    }
    
    return false;
}
```

---

## Algorithm Walkthrough

### Example: Loop Detection with Floyd's Algorithm

```
List: 1 → 2 → 3 → 4 → 5
          ↑           ↓
          └───────────┘

Initial: slow = 1, fast = 1

Step 1:
  slow = slow.next = 2
  fast = fast.next.next = 3
  slow ≠ fast

Step 2:
  slow = slow.next = 3
  fast = fast.next.next = 5
  slow ≠ fast

Step 3:
  slow = slow.next = 4
  fast = fast.next.next = 3 (loop back)
  slow ≠ fast

Step 4:
  slow = slow.next = 5
  fast = fast.next.next = 5 (loop back)
  slow == fast ✓

Result: true (loop detected)
```

### Example: No Loop

```
List: 1 → 2 → 3 → 4 → NULL

Initial: slow = 1, fast = 1

Step 1:
  slow = 2, fast = 3
  slow ≠ fast

Step 2:
  slow = 3, fast = NULL (fast.next.next)
  Exit loop (fast == NULL)

Result: false (no loop)
```

---

## Complete Implementation

```java
class Node {
    int data;
    Node next;
    
    Node(int data) {
        this.data = data;
        this.next = null;
    }
}

public class Solution {
    
    // Approach 1: Floyd's Cycle Detection (Optimal)
    public static boolean hasLoop(Node head) {
        if (head == null || head.next == null) return false;
        
        Node slow = head;
        Node fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                return true;
            }
        }
        
        return false;
    }
    
    // Approach 2: HashSet
    public static boolean hasLoopHashSet(Node head) {
        Set<Node> visited = new HashSet<>();
        Node current = head;
        
        while (current != null) {
            if (visited.contains(current)) {
                return true;
            }
            visited.add(current);
            current = current.next;
        }
        
        return false;
    }
    
    // Approach 3: Modify Node (Not Recommended)
    public static boolean hasLoopModify(Node head) {
        Node current = head;
        Node temp = new Node(-1);
        
        while (current != null) {
            if (current.next == temp) {
                return true;
            }
            Node next = current.next;
            current.next = temp;
            current = next;
        }
        
        return false;
    }
    
    // Helper: Find loop start node (if loop exists)
    public static Node findLoopStart(Node head) {
        if (head == null || head.next == null) return null;
        
        Node slow = head;
        Node fast = head;
        
        // Detect loop
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                // Loop found, find start
                slow = head;
                while (slow != fast) {
                    slow = slow.next;
                    fast = fast.next;
                }
                return slow;
            }
        }
        
        return null;
    }
    
    // Helper: Count loop length
    public static int countLoopLength(Node head) {
        if (head == null || head.next == null) return 0;
        
        Node slow = head;
        Node fast = head;
        
        // Detect loop
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                // Count loop length
                int count = 1;
                Node temp = slow.next;
                while (temp != slow) {
                    count++;
                    temp = temp.next;
                }
                return count;
            }
        }
        
        return 0;
    }
    
    // Helper: Remove loop
    public static void removeLoop(Node head) {
        if (head == null || head.next == null) return;
        
        Node slow = head;
        Node fast = head;
        
        // Detect loop
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                // Find loop start
                slow = head;
                while (slow.next != fast.next) {
                    slow = slow.next;
                    fast = fast.next;
                }
                // Remove loop
                fast.next = null;
                return;
            }
        }
    }
    
    // Helper: Create list with loop
    public static Node createListWithLoop(int[] values, int loopPos) {
        if (values == null || values.length == 0) return null;
        
        Node head = new Node(values[0]);
        Node current = head;
        Node loopNode = null;
        
        if (loopPos == 0) loopNode = head;
        
        for (int i = 1; i < values.length; i++) {
            current.next = new Node(values[i]);
            current = current.next;
            if (i == loopPos) loopNode = current;
        }
        
        if (loopPos >= 0 && loopPos < values.length) {
            current.next = loopNode;
        }
        
        return head;
    }
    
    // Helper: Create list without loop
    public static Node createList(int[] values) {
        if (values == null || values.length == 0) return null;
        
        Node head = new Node(values[0]);
        Node current = head;
        
        for (int i = 1; i < values.length; i++) {
            current.next = new Node(values[i]);
            current = current.next;
        }
        
        return head;
    }
    
    public static boolean doTestsPass() {
        // Test 1: No loop
        Node list1 = createList(new int[]{1, 2, 3, 4, 5});
        if (hasLoop(list1)) return false;
        
        // Test 2: Loop at position 2
        Node list2 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 2);
        if (!hasLoop(list2)) return false;
        
        // Test 3: Self loop
        Node list3 = new Node(1);
        list3.next = list3;
        if (!hasLoop(list3)) return false;
        
        // Test 4: Empty list
        if (hasLoop(null)) return false;
        
        // Test 5: Single node
        Node list5 = new Node(1);
        if (hasLoop(list5)) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        System.out.println("Test 1: No loop");
        Node list1 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Has loop: " + hasLoop(list1));
        
        System.out.println("\nTest 2: Loop at position 2");
        Node list2 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 2);
        System.out.println("Has loop: " + hasLoop(list2));
        System.out.println("Loop length: " + countLoopLength(list2));
        
        System.out.println("\nTest 3: Self loop");
        Node list3 = new Node(1);
        list3.next = list3;
        System.out.println("Has loop: " + hasLoop(list3));
        
        System.out.println("\nTest 4: Remove loop");
        Node list4 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 2);
        System.out.println("Before: Has loop = " + hasLoop(list4));
        removeLoop(list4);
        System.out.println("After:  Has loop = " + hasLoop(list4));
    }
}
```

---

## Test Cases

```java
@Test
public void testHasLoop() {
    // Test 1: No loop
    Node list1 = createList(new int[]{1, 2, 3, 4, 5});
    assertFalse(hasLoop(list1));
    
    // Test 2: Loop at end
    Node list2 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 4);
    assertTrue(hasLoop(list2));
    
    // Test 3: Loop at middle
    Node list3 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 2);
    assertTrue(hasLoop(list3));
    
    // Test 4: Loop at start
    Node list4 = createListWithLoop(new int[]{1, 2, 3, 4, 5}, 0);
    assertTrue(hasLoop(list4));
    
    // Test 5: Self loop
    Node list5 = new Node(1);
    list5.next = list5;
    assertTrue(hasLoop(list5));
    
    // Test 6: Two nodes loop
    Node list6 = new Node(1);
    list6.next = new Node(2);
    list6.next.next = list6;
    assertTrue(hasLoop(list6));
    
    // Test 7: Empty list
    assertFalse(hasLoop(null));
    
    // Test 8: Single node no loop
    Node list8 = new Node(1);
    assertFalse(hasLoop(list8));
    
    // Test 9: Two nodes no loop
    Node list9 = new Node(1);
    list9.next = new Node(2);
    assertFalse(hasLoop(list9));
}
```

---

## Visual Representation

### Floyd's Cycle Detection

```
List with loop: 1 → 2 → 3 → 4 → 5
                    ↑           ↓
                    └───────────┘

Step-by-step:
┌──────┬──────┬──────┬─────────┐
│ Step │ Slow │ Fast │ Match?  │
├──────┼──────┼──────┼─────────┤
│  0   │  1   │  1   │   No    │
│  1   │  2   │  3   │   No    │
│  2   │  3   │  5   │   No    │
│  3   │  4   │  3   │   No    │
│  4   │  5   │  5   │  Yes ✓  │
└──────┴──────┴──────┴─────────┘

Loop detected at step 4!
```

### Why Floyd's Algorithm Works

```
Slow pointer moves 1 step: S
Fast pointer moves 2 steps: F

If loop exists:
- Both pointers enter loop
- Fast catches up to slow inside loop
- Distance closes by 1 each iteration
- They must meet!

Example:
Loop: 2 → 3 → 4 → 5 → 2
      ↑               ↓
      └───────────────┘

Slow enters at 2, Fast at 4
Step 1: S=3, F=2 (distance=1)
Step 2: S=4, F=4 (meet!)
```

### HashSet Approach

```
List: 1 → 2 → 3 → 4 → 5
          ↑           ↓
          └───────────┘

Visited Set:
Step 1: {1}
Step 2: {1, 2}
Step 3: {1, 2, 3}
Step 4: {1, 2, 3, 4}
Step 5: {1, 2, 3, 4, 5}
Step 6: Check 2 → Already in set! ✓

Loop detected!
```

---

## Edge Cases

1. **Empty list (NULL):** Return false
2. **Single node no loop:** Return false
3. **Single node self loop:** Return true
4. **Two nodes loop:** Return true
5. **Loop at start:** Detect correctly
6. **Loop at middle:** Detect correctly
7. **Loop at end:** Detect correctly
8. **Very long list:** O(n) handles efficiently
9. **Very small loop:** Detect correctly

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Floyd's Algorithm | O(n) | O(1) | **Optimal solution** |
| HashSet | O(n) | O(n) | Simple but uses extra space |
| Modify Node | O(n) | O(1) | Destroys original list |

**Where n = number of nodes**

**Floyd's Algorithm Analysis:**
- If no loop: Traverse entire list once: O(n)
- If loop exists: Fast pointer catches slow in at most n steps: O(n)
- Space: Only two pointers: O(1)

**Why Floyd's is Optimal:**
- Linear time complexity
- Constant space complexity
- Non-destructive (doesn't modify list)

---

## Related Problems

1. **Find Loop Start Node** - Return where loop begins
2. **Count Loop Length** - Number of nodes in loop
3. **Remove Loop** - Break the cycle
4. **Detect Cycle in Graph** - General cycle detection
5. **Happy Number** - Uses same algorithm
6. **Linked List Cycle II** - Find cycle start

---

## Interview Tips

### Clarification Questions
1. Can list be empty? (Yes, return false)
2. Can list have single node? (Yes, check self-loop)
3. Should we modify the list? (No, use Floyd's)
4. Need to find loop start? (Follow-up question)
5. Need to remove loop? (Follow-up question)

### Approach Explanation
1. "I'll use Floyd's Cycle Detection algorithm"
2. "Use two pointers: slow (1 step) and fast (2 steps)"
3. "If loop exists, fast will eventually catch slow"
4. "If fast reaches NULL, no loop exists"
5. "Time O(n), Space O(1) - optimal solution"

### Common Mistakes
1. **Not checking NULL** - Check fast and fast.next
2. **Wrong initialization** - Both start at head
3. **Infinite loop** - Ensure fast moves 2 steps
4. **Not handling single node** - Check head.next
5. **Modifying list** - Use Floyd's, not modification

### Follow-up Questions
1. "Find where loop starts?" - Reset slow to head, move both 1 step
2. "Count loop length?" - Count steps from meeting point
3. "Remove loop?" - Find start, set previous.next = null
4. "Why does Floyd's work?" - Explain relative speed
5. "Space optimization?" - Floyd's is already O(1)

---

## Real-World Applications

1. **Memory Leak Detection** - Circular references
2. **Deadlock Detection** - Circular wait in resources
3. **Graph Cycle Detection** - Dependency cycles
4. **Playlist Loop** - Detect repeating songs
5. **Network Routing** - Detect routing loops
6. **Compiler** - Detect circular dependencies
7. **Operating Systems** - Process scheduling cycles

---

## Key Takeaways

1. **Floyd's algorithm is optimal:** O(n) time, O(1) space
2. **Two pointers technique:** Slow (1 step), Fast (2 steps)
3. **Loop detection guarantee:** Fast catches slow if loop exists
4. **Check NULL carefully:** fast != null && fast.next != null
5. **Non-destructive:** Doesn't modify original list
6. **Versatile algorithm:** Works for many cycle detection problems
7. **Mathematical proof:** Relative speed ensures meeting

---

## Floyd's Algorithm Proof

### Why They Must Meet

```
Let:
- m = distance from head to loop start
- k = distance from loop start to meeting point
- L = loop length

When slow enters loop:
- Slow traveled: m steps
- Fast traveled: 2m steps
- Fast is (2m - m) = m steps ahead in loop

Distance between them in loop: L - (m mod L)

Each iteration:
- Distance closes by 1 (fast gains 1 step)
- After (L - (m mod L)) iterations, they meet

Therefore: They MUST meet if loop exists!
```

### Finding Loop Start

```
After meeting at point M:
- Slow traveled: m + k
- Fast traveled: 2(m + k) = m + k + nL (n loops)
- Therefore: m + k = nL
- So: m = nL - k

Reset slow to head, move both 1 step:
- Slow travels m to reach loop start
- Fast travels m = nL - k from M
- Both reach loop start simultaneously!
```

---

## Optimization Notes

### Why Floyd's Over HashSet?

```java
// HashSet: O(n) space
Set<Node> visited = new HashSet<>();  // Extra memory
while (current != null) {
    if (visited.contains(current)) return true;
    visited.add(current);
}

// Floyd's: O(1) space
Node slow = head, fast = head;  // Only 2 pointers
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    if (slow == fast) return true;
}
```

### Best Practice

```java
public static boolean hasLoop(Node head) {
    // Handle edge cases
    if (head == null || head.next == null) {
        return false;
    }
    
    Node slow = head;
    Node fast = head;
    
    // Floyd's cycle detection
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        
        if (slow == fast) {
            return true;  // Loop detected
        }
    }
    
    return false;  // No loop
}
```

---

## Advanced: Find Loop Start

```java
public static Node findLoopStart(Node head) {
    if (head == null || head.next == null) return null;
    
    Node slow = head;
    Node fast = head;
    
    // Phase 1: Detect loop
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        
        if (slow == fast) {
            // Phase 2: Find start
            slow = head;
            while (slow != fast) {
                slow = slow.next;
                fast = fast.next;
            }
            return slow;  // Loop start node
        }
    }
    
    return null;  // No loop
}
```

---

## Variations

### 1. Count Loop Length

```java
public static int countLoopLength(Node head) {
    Node slow = head, fast = head;
    
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        
        if (slow == fast) {
            int count = 1;
            Node temp = slow.next;
            while (temp != slow) {
                count++;
                temp = temp.next;
            }
            return count;
        }
    }
    
    return 0;
}
```

### 2. Remove Loop

```java
public static void removeLoop(Node head) {
    Node slow = head, fast = head;
    
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        
        if (slow == fast) {
            slow = head;
            while (slow.next != fast.next) {
                slow = slow.next;
                fast = fast.next;
            }
            fast.next = null;
            return;
        }
    }
}
```
