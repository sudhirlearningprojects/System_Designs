# Implement Queue Using Stacks

## Problem Statement

Implement a Queue (FIFO - First In First Out) data structure using only two Stacks (LIFO - Last In First Out).

**Operations to Implement:**
- `enqueue(x)`: Add element to the rear of queue
- `dequeue()`: Remove element from the front of queue
- `peek()`: Get the front element without removing
- `isEmpty()`: Check if queue is empty

**Input/Output:**
```
Operations: enqueue(1), enqueue(2), enqueue(3), dequeue(), enqueue(4), dequeue()
Output: dequeue() returns 1, dequeue() returns 2
Queue state: [3, 4]

Operations: enqueue(5), peek(), dequeue()
Output: peek() returns 5, dequeue() returns 5
Queue state: []
```

---

## Solution Approaches

### Approach 1: Two Stacks (Amortized O(1))

**Strategy:** Use two stacks - one for enqueue, one for dequeue

**Time Complexity:** 
- Enqueue: O(1)
- Dequeue: O(1) amortized
- Peek: O(1) amortized

**Space Complexity:** O(n)

```java
class MyQueue {
    private Stack<Integer> stack1;  // For enqueue
    private Stack<Integer> stack2;  // For dequeue
    
    public MyQueue() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    public void enqueue(int x) {
        stack1.push(x);
    }
    
    public int dequeue() {
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        return stack2.pop();
    }
    
    public int peek() {
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        return stack2.peek();
    }
    
    public boolean isEmpty() {
        return stack1.isEmpty() && stack2.isEmpty();
    }
}
```

---

### Approach 2: Two Stacks (Enqueue Expensive)

**Strategy:** Make enqueue O(n), dequeue O(1)

**Time Complexity:**
- Enqueue: O(n)
- Dequeue: O(1)
- Peek: O(1)

**Space Complexity:** O(n)

```java
class MyQueueEnqueueExpensive {
    private Stack<Integer> stack1;
    private Stack<Integer> stack2;
    
    public MyQueueEnqueueExpensive() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    public void enqueue(int x) {
        while (!stack1.isEmpty()) {
            stack2.push(stack1.pop());
        }
        stack1.push(x);
        while (!stack2.isEmpty()) {
            stack1.push(stack2.pop());
        }
    }
    
    public int dequeue() {
        return stack1.pop();
    }
    
    public int peek() {
        return stack1.peek();
    }
    
    public boolean isEmpty() {
        return stack1.isEmpty();
    }
}
```

---

## Algorithm Walkthrough

### Example: Enqueue 1, 2, 3 then Dequeue

**Using Approach 1 (Amortized O(1)):**

```
Initial State:
  stack1: []
  stack2: []

Operation: enqueue(1)
  stack1.push(1)
  stack1: [1]
  stack2: []

Operation: enqueue(2)
  stack1.push(2)
  stack1: [1, 2]
  stack2: []

Operation: enqueue(3)
  stack1.push(3)
  stack1: [1, 2, 3]
  stack2: []

Operation: dequeue()
  stack2 is empty, transfer from stack1:
    stack1.pop() → 3 → stack2.push(3)
    stack1.pop() → 2 → stack2.push(2)
    stack1.pop() → 1 → stack2.push(1)
  
  stack1: []
  stack2: [3, 2, 1]
  
  stack2.pop() → return 1
  
  stack1: []
  stack2: [3, 2]

Operation: enqueue(4)
  stack1.push(4)
  stack1: [4]
  stack2: [3, 2]

Operation: dequeue()
  stack2 not empty
  stack2.pop() → return 2
  
  stack1: [4]
  stack2: [3]

Final State:
  Queue: [3, 4] (front to rear)
```

---

## Complete Implementation

```java
import java.util.*;

class MyQueue {
    private Stack<Integer> stack1;  // For enqueue
    private Stack<Integer> stack2;  // For dequeue
    
    public MyQueue() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    // O(1)
    public void enqueue(int x) {
        stack1.push(x);
    }
    
    // O(1) amortized
    public int dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.pop();
    }
    
    // O(1) amortized
    public int peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.peek();
    }
    
    // O(1)
    public boolean isEmpty() {
        return stack1.isEmpty() && stack2.isEmpty();
    }
    
    // O(1)
    public int size() {
        return stack1.size() + stack2.size();
    }
}

// Alternative: Enqueue Expensive
class MyQueueEnqueueExpensive {
    private Stack<Integer> stack1;
    private Stack<Integer> stack2;
    
    public MyQueueEnqueueExpensive() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    // O(n)
    public void enqueue(int x) {
        while (!stack1.isEmpty()) {
            stack2.push(stack1.pop());
        }
        stack1.push(x);
        while (!stack2.isEmpty()) {
            stack1.push(stack2.pop());
        }
    }
    
    // O(1)
    public int dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return stack1.pop();
    }
    
    // O(1)
    public int peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return stack1.peek();
    }
    
    // O(1)
    public boolean isEmpty() {
        return stack1.isEmpty();
    }
}

public class Solution {
    
    public static boolean doTestsPass() {
        MyQueue queue = new MyQueue();
        
        // Test enqueue
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        
        // Test dequeue
        if (queue.dequeue() != 1) return false;
        if (queue.dequeue() != 2) return false;
        
        // Test enqueue after dequeue
        queue.enqueue(4);
        
        // Test peek
        if (queue.peek() != 3) return false;
        
        // Test dequeue
        if (queue.dequeue() != 3) return false;
        if (queue.dequeue() != 4) return false;
        
        // Test isEmpty
        if (!queue.isEmpty()) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        MyQueue queue = new MyQueue();
        
        System.out.println("Enqueue: 1, 2, 3");
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        
        System.out.println("Dequeue: " + queue.dequeue());  // 1
        System.out.println("Peek: " + queue.peek());        // 2
        
        System.out.println("Enqueue: 4");
        queue.enqueue(4);
        
        System.out.println("Dequeue: " + queue.dequeue());  // 2
        System.out.println("Dequeue: " + queue.dequeue());  // 3
        System.out.println("Dequeue: " + queue.dequeue());  // 4
        
        System.out.println("Is empty: " + queue.isEmpty()); // true
    }
}
```

---

## Test Cases

```java
@Test
public void testMyQueue() {
    MyQueue queue = new MyQueue();
    
    // Test 1: Basic enqueue and dequeue
    queue.enqueue(1);
    queue.enqueue(2);
    assertEquals(1, queue.dequeue());
    assertEquals(2, queue.dequeue());
    
    // Test 2: Enqueue after dequeue
    queue.enqueue(3);
    queue.enqueue(4);
    assertEquals(3, queue.dequeue());
    queue.enqueue(5);
    assertEquals(4, queue.dequeue());
    assertEquals(5, queue.dequeue());
    
    // Test 3: Peek
    queue.enqueue(6);
    assertEquals(6, queue.peek());
    assertEquals(6, queue.peek());  // Peek doesn't remove
    assertEquals(6, queue.dequeue());
    
    // Test 4: isEmpty
    assertTrue(queue.isEmpty());
    queue.enqueue(7);
    assertFalse(queue.isEmpty());
    
    // Test 5: Multiple operations
    queue.enqueue(8);
    queue.enqueue(9);
    assertEquals(7, queue.dequeue());
    assertEquals(8, queue.dequeue());
    assertEquals(9, queue.dequeue());
    assertTrue(queue.isEmpty());
}
```

---

## Visual Representation

### Two Stacks Approach

```
Queue Operations: enqueue(1), enqueue(2), enqueue(3)

After enqueue(1):
  stack1: [1]
  stack2: []
  Queue view: [1]

After enqueue(2):
  stack1: [1, 2]
  stack2: []
  Queue view: [1, 2]

After enqueue(3):
  stack1: [1, 2, 3]
  stack2: []
  Queue view: [1, 2, 3]

Operation: dequeue()
  Transfer stack1 to stack2:
  
  stack1: []
  stack2: [3, 2, 1]
           ↑     ↑
         rear  front
  
  Pop from stack2: return 1
  
  stack1: []
  stack2: [3, 2]
  Queue view: [2, 3]
```

### Stack Transfer Visualization

```
Before Transfer:
┌─────────┐  ┌─────────┐
│ stack1  │  │ stack2  │
├─────────┤  ├─────────┤
│    3    │  │         │
│    2    │  │         │
│    1    │  │         │
└─────────┘  └─────────┘

Transfer Process:
  pop 3 from stack1 → push to stack2
  pop 2 from stack1 → push to stack2
  pop 1 from stack1 → push to stack2

After Transfer:
┌─────────┐  ┌─────────┐
│ stack1  │  │ stack2  │
├─────────┤  ├─────────┤
│         │  │    3    │
│         │  │    2    │
│         │  │    1    │ ← front
└─────────┘  └─────────┘
```

---

## Edge Cases

1. **Empty queue dequeue:** Throw exception
2. **Empty queue peek:** Throw exception
3. **Single element:** Enqueue and dequeue correctly
4. **Alternating operations:** Enqueue, dequeue, enqueue, dequeue
5. **Multiple dequeues:** Transfer happens once
6. **Large number of elements:** Handle efficiently
7. **All enqueues then all dequeues:** Amortized O(1)

---

## Complexity Analysis

| Operation | Approach 1 | Approach 2 | Notes |
|-----------|------------|------------|-------|
| Enqueue | O(1) | O(n) | **Approach 1 better** |
| Dequeue | O(1) amortized | O(1) | Approach 1 amortized |
| Peek | O(1) amortized | O(1) | Approach 1 amortized |
| Space | O(n) | O(n) | Both use two stacks |

**Amortized Analysis (Approach 1):**
- Each element pushed to stack1 once: O(1)
- Each element transferred to stack2 once: O(1)
- Each element popped from stack2 once: O(1)
- Total per element: O(1) + O(1) + O(1) = O(3) = O(1) amortized

**Why Approach 1 is Better:**
- Enqueue is always O(1)
- Dequeue is O(1) amortized (transfer happens rarely)
- Most practical use cases have mixed operations

---

## Related Problems

1. **Implement Stack Using Queues** - Reverse problem
2. **Min Stack** - Stack with O(1) min operation
3. **Max Queue** - Queue with O(1) max operation
4. **Circular Queue** - Fixed-size queue
5. **Deque** - Double-ended queue
6. **Priority Queue** - Heap-based queue

---

## Interview Tips

### Clarification Questions
1. Can we use built-in Stack? (Yes, usually)
2. What to return on empty dequeue? (Throw exception or return -1)
3. Need to support peek? (Usually yes)
4. Expected number of operations? (Helps choose approach)
5. Memory constraints? (Both approaches use O(n))

### Approach Explanation
1. "I'll use two stacks: stack1 for enqueue, stack2 for dequeue"
2. "Enqueue pushes to stack1 in O(1)"
3. "Dequeue pops from stack2, transferring from stack1 if needed"
4. "Transfer reverses order, making oldest element accessible"
5. "Amortized O(1) for all operations"

### Common Mistakes
1. **Not checking empty** - Check before dequeue/peek
2. **Transferring every time** - Only when stack2 is empty
3. **Wrong transfer direction** - stack1 to stack2, not reverse
4. **Not understanding amortized** - Each element transferred once
5. **Choosing wrong approach** - Approach 1 is usually better

### Follow-up Questions
1. "What if enqueue is more frequent?" - Approach 1 is better
2. "What if dequeue is more frequent?" - Still Approach 1
3. "Can you do better than O(1) amortized?" - No, this is optimal
4. "Implement with one stack?" - Possible with recursion (not practical)
5. "Thread-safe implementation?" - Add synchronization

---

## Real-World Applications

1. **Task Scheduling** - FIFO job queue
2. **Message Queues** - RabbitMQ, Kafka
3. **Print Queue** - Printer spooler
4. **Breadth-First Search** - Graph traversal
5. **Request Handling** - Web server request queue
6. **Buffer Management** - I/O buffering
7. **Event Processing** - Event-driven systems

---

## Key Takeaways

1. **Two stacks simulate queue:** One for enqueue, one for dequeue
2. **Amortized O(1):** Each element transferred at most once
3. **Lazy transfer:** Only transfer when stack2 is empty
4. **FIFO from LIFO:** Double reversal achieves FIFO
5. **Approach 1 is optimal:** O(1) enqueue, O(1) amortized dequeue
6. **Space trade-off:** O(n) space for O(1) operations
7. **Practical solution:** Used in real systems

---

## Amortized Analysis Proof

### Why O(1) Amortized?

```
Consider n operations:

Enqueue operations:
- Each element pushed to stack1 once: n × O(1) = O(n)

Dequeue operations:
- Each element transferred to stack2 once: n × O(1) = O(n)
- Each element popped from stack2 once: n × O(1) = O(n)

Total cost for n operations: O(n) + O(n) + O(n) = O(3n) = O(n)

Amortized cost per operation: O(n) / n = O(1)

Therefore: Each operation is O(1) amortized!
```

---

## Optimization Notes

### Why Not Transfer Every Time?

```java
// Inefficient: Transfer every dequeue
public int dequeue() {
    while (!stack1.isEmpty()) {
        stack2.push(stack1.pop());  // O(n) every time
    }
    int result = stack2.pop();
    while (!stack2.isEmpty()) {
        stack1.push(stack2.pop());  // O(n) again
    }
    return result;  // Total: O(n) per dequeue
}

// Efficient: Transfer only when needed
public int dequeue() {
    if (stack2.isEmpty()) {
        while (!stack1.isEmpty()) {
            stack2.push(stack1.pop());  // O(n) once
        }
    }
    return stack2.pop();  // O(1) amortized
}
```

### Best Practice

```java
class MyQueue {
    private Stack<Integer> stack1;
    private Stack<Integer> stack2;
    
    public MyQueue() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    public void enqueue(int x) {
        stack1.push(x);
    }
    
    public int dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        // Lazy transfer: only when stack2 is empty
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.pop();
    }
    
    public int peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        
        if (stack2.isEmpty()) {
            while (!stack1.isEmpty()) {
                stack2.push(stack1.pop());
            }
        }
        
        return stack2.peek();
    }
    
    public boolean isEmpty() {
        return stack1.isEmpty() && stack2.isEmpty();
    }
}
```

---

## Advanced: One Stack Implementation (Not Practical)

```java
// Using recursion (not recommended - O(n) per operation)
class MyQueueOneStack {
    private Stack<Integer> stack;
    
    public MyQueueOneStack() {
        stack = new Stack<>();
    }
    
    public void enqueue(int x) {
        stack.push(x);
    }
    
    public int dequeue() {
        if (stack.size() == 1) {
            return stack.pop();
        }
        
        int temp = stack.pop();
        int result = dequeue();  // Recursive
        stack.push(temp);
        
        return result;
    }
}
```

---

## Comparison with Array-Based Queue

```
Array-Based Queue:
✓ O(1) enqueue and dequeue
✓ Better cache locality
✗ Fixed size or need resizing
✗ Wasted space with circular buffer

Stack-Based Queue:
✓ Dynamic size
✓ No wasted space
✓ O(1) amortized operations
✗ Slightly more complex
✗ Two data structures
```
