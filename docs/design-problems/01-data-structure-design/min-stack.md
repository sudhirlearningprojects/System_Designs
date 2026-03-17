# Min Stack (LeetCode 155)

## Problem Statement

Design a stack that supports push, pop, top, and retrieving the minimum element in constant time.

Implement the `MinStack` class:
- `MinStack()` initializes the stack object.
- `void push(int val)` pushes the element val onto the stack.
- `void pop()` removes the element on the top of the stack.
- `int top()` gets the top element of the stack.
- `int getMin()` retrieves the minimum element in the stack.

**Time Complexity Requirement**: O(1) for all operations

**Constraints:**
- -2^31 <= val <= 2^31 - 1
- Methods pop, top and getMin will always be called on non-empty stacks
- At most 3 * 10^4 calls will be made to push, pop, top, and getMin

## Approach 1: Two Stacks (Intuitive)

### Intuition
- Main stack: stores all elements
- Min stack: stores minimum at each level
- When push: update min stack with current minimum
- When pop: pop from both stacks

### Implementation

```java
class MinStack {
    private Stack<Integer> stack;
    private Stack<Integer> minStack;
    
    public MinStack() {
        stack = new Stack<>();
        minStack = new Stack<>();
    }
    
    public void push(int val) {
        stack.push(val);
        
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        } else {
            minStack.push(minStack.peek());
        }
    }
    
    public void pop() {
        stack.pop();
        minStack.pop();
    }
    
    public int top() {
        return stack.peek();
    }
    
    public int getMin() {
        return minStack.peek();
    }
}
```

**Time Complexity**: O(1) for all operations
**Space Complexity**: O(2n) = O(n)

### Pros
- Simple and intuitive
- O(1) for all operations
- Easy to understand

### Cons
- Uses 2x space (stores min for every element)
- Redundant storage when min doesn't change

---

## Approach 2: Optimized Two Stacks (Space Efficient)

### Intuition
- Only push to minStack when new minimum found
- Track how many times current min appears

### Implementation

```java
class MinStack {
    private Stack<Integer> stack;
    private Stack<Integer> minStack;
    
    public MinStack() {
        stack = new Stack<>();
        minStack = new Stack<>();
    }
    
    public void push(int val) {
        stack.push(val);
        
        // Only push to minStack if it's a new minimum
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        }
    }
    
    public void pop() {
        int val = stack.pop();
        
        // Only pop from minStack if we're removing the minimum
        if (val == minStack.peek()) {
            minStack.pop();
        }
    }
    
    public int top() {
        return stack.peek();
    }
    
    public int getMin() {
        return minStack.peek();
    }
}
```

**Time Complexity**: O(1) for all operations
**Space Complexity**: O(n) in worst case, O(1) in best case

### Pros
- Space efficient (only stores when min changes)
- Still O(1) for all operations
- Better than Approach 1 for most cases

### Cons
- Slightly more complex logic
- Worst case still O(n) space

---

## Approach 3: Single Stack with Pairs

### Intuition
- Store (value, currentMin) pairs in single stack
- Each element knows the minimum at its level

### Implementation

```java
class MinStack {
    class Node {
        int value;
        int min;
        
        Node(int value, int min) {
            this.value = value;
            this.min = min;
        }
    }
    
    private Stack<Node> stack;
    
    public MinStack() {
        stack = new Stack<>();
    }
    
    public void push(int val) {
        if (stack.isEmpty()) {
            stack.push(new Node(val, val));
        } else {
            int currentMin = Math.min(val, stack.peek().min);
            stack.push(new Node(val, currentMin));
        }
    }
    
    public void pop() {
        stack.pop();
    }
    
    public int top() {
        return stack.peek().value;
    }
    
    public int getMin() {
        return stack.peek().min;
    }
}
```

**Time Complexity**: O(1) for all operations
**Space Complexity**: O(2n) = O(n)

### Pros
- Single data structure
- Clean encapsulation
- O(1) for all operations

### Cons
- Uses 2x space (stores min with every element)
- Object overhead for Node class

---

## Approach 4: Difference Encoding (Most Space Efficient)

### Intuition
- Store difference from current minimum
- When difference is negative, it's a new minimum
- Decode on pop to restore previous minimum

### Implementation

```java
class MinStack {
    private Stack<Long> stack;
    private long min;
    
    public MinStack() {
        stack = new Stack<>();
    }
    
    public void push(int val) {
        if (stack.isEmpty()) {
            stack.push(0L);
            min = val;
        } else {
            long diff = (long) val - min;
            stack.push(diff);
            
            if (diff < 0) {
                min = val; // New minimum
            }
        }
    }
    
    public void pop() {
        long diff = stack.pop();
        
        if (diff < 0) {
            // Restore previous minimum
            min = min - diff;
        }
    }
    
    public int top() {
        long diff = stack.peek();
        
        if (diff < 0) {
            return (int) min;
        } else {
            return (int) (min + diff);
        }
    }
    
    public int getMin() {
        return (int) min;
    }
}
```

**Time Complexity**: O(1) for all operations
**Space Complexity**: O(n)

### Pros
- Most space efficient (single stack)
- O(1) for all operations
- Clever encoding technique

### Cons
- Complex to understand
- Uses long to avoid overflow
- Not intuitive in interviews

---

## Approach 5: Linked List Implementation

### Intuition
- Custom linked list node with min field
- Each node tracks minimum up to that point

### Implementation

```java
class MinStack {
    class Node {
        int value;
        int min;
        Node next;
        
        Node(int value, int min, Node next) {
            this.value = value;
            this.min = min;
            this.next = next;
        }
    }
    
    private Node head;
    
    public MinStack() {
        head = null;
    }
    
    public void push(int val) {
        if (head == null) {
            head = new Node(val, val, null);
        } else {
            int currentMin = Math.min(val, head.min);
            head = new Node(val, currentMin, head);
        }
    }
    
    public void pop() {
        head = head.next;
    }
    
    public int top() {
        return head.value;
    }
    
    public int getMin() {
        return head.min;
    }
}
```

**Time Complexity**: O(1) for all operations
**Space Complexity**: O(2n) = O(n)

### Pros
- No Stack class dependency
- Clean implementation
- Good for understanding internals

### Cons
- Uses 2x space
- More code than Stack-based solutions

---

## Comparison Table

| Approach | Time | Space | Complexity | Interview Suitable |
|----------|------|-------|------------|-------------------|
| Two Stacks | O(1) | O(2n) | Easy | ✅ Best for beginners |
| Optimized Two Stacks | O(1) | O(n) avg | Medium | ✅ Best overall |
| Single Stack + Pairs | O(1) | O(2n) | Easy | ✅ Good |
| Difference Encoding | O(1) | O(n) | Hard | ⚠️ Too clever |
| Linked List | O(1) | O(2n) | Medium | ✅ Good |

---

## Test Cases

```java
public class MinStackTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        MinStack stack = new MinStack();
        stack.push(-2);
        stack.push(0);
        stack.push(-3);
        assert stack.getMin() == -3;
        stack.pop();
        assert stack.top() == 0;
        assert stack.getMin() == -2;
        
        // Test Case 2: Duplicate minimums
        MinStack stack2 = new MinStack();
        stack2.push(1);
        stack2.push(2);
        stack2.push(1);
        assert stack2.getMin() == 1;
        stack2.pop();
        assert stack2.getMin() == 1; // Still 1
        stack2.pop();
        assert stack2.getMin() == 1;
        
        // Test Case 3: Decreasing sequence
        MinStack stack3 = new MinStack();
        stack3.push(5);
        stack3.push(4);
        stack3.push(3);
        stack3.push(2);
        stack3.push(1);
        assert stack3.getMin() == 1;
        stack3.pop();
        assert stack3.getMin() == 2;
        stack3.pop();
        assert stack3.getMin() == 3;
        
        // Test Case 4: Integer overflow edge case
        MinStack stack4 = new MinStack();
        stack4.push(Integer.MIN_VALUE);
        stack4.push(Integer.MAX_VALUE);
        assert stack4.getMin() == Integer.MIN_VALUE;
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

### 1. How to support getMax() as well?

```java
class MinMaxStack {
    private Stack<Integer> stack;
    private Stack<Integer> minStack;
    private Stack<Integer> maxStack;
    
    public void push(int val) {
        stack.push(val);
        
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        }
        
        if (maxStack.isEmpty() || val >= maxStack.peek()) {
            maxStack.push(val);
        }
    }
    
    public int getMax() {
        return maxStack.peek();
    }
}
```

### 2. How to make it thread-safe?

```java
class ThreadSafeMinStack {
    private final Stack<Integer> stack = new Stack<>();
    private final Stack<Integer> minStack = new Stack<>();
    private final Object lock = new Object();
    
    public void push(int val) {
        synchronized (lock) {
            stack.push(val);
            if (minStack.isEmpty() || val <= minStack.peek()) {
                minStack.push(val);
            }
        }
    }
    
    public int getMin() {
        synchronized (lock) {
            return minStack.peek();
        }
    }
}
```

### 3. How to support getMedian()?

```java
class MedianStack {
    private Stack<Integer> stack;
    private PriorityQueue<Integer> minHeap; // larger half
    private PriorityQueue<Integer> maxHeap; // smaller half (max heap)
    
    public double getMedian() {
        if (maxHeap.size() == minHeap.size()) {
            return (maxHeap.peek() + minHeap.peek()) / 2.0;
        }
        return maxHeap.peek();
    }
}
```

### 4. How to implement with O(1) space?

Not possible while maintaining O(1) time for all operations. Must store minimum information somewhere.

### 5. How to support getKthMin()?

```java
class KthMinStack {
    private Stack<Integer> stack;
    private TreeMap<Integer, Integer> freqMap; // value -> frequency
    
    public int getKthMin(int k) {
        int count = 0;
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            count += entry.getValue();
            if (count >= k) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
```

---

## Common Mistakes

1. ❌ Forgetting to handle empty stack case
2. ❌ Using `val < minStack.peek()` instead of `val <= minStack.peek()` (misses duplicates)
3. ❌ Not popping from minStack when removing minimum
4. ❌ Integer overflow in difference encoding approach
5. ❌ Comparing objects with `==` instead of `.equals()`

---

## Key Insights

### Why Two Stacks Work?

```
Example: push(-2), push(0), push(-3)

Main Stack:  [-2, 0, -3]
Min Stack:   [-2, -2, -3]

At each level, minStack stores the minimum up to that point.
When we pop -3, we restore min to -2 (previous minimum).
```

### Space Optimization

```
Unoptimized: Always push to minStack
[-2, 0, -3] -> minStack: [-2, -2, -3]  (3 elements)

Optimized: Only push when new min
[-2, 0, -3] -> minStack: [-2, -3]  (2 elements)

Savings: 33% in this example
```

---

## Related Problems

- [Max Stack (LC 716)](./max-stack.md) - Similar but with getMax()
- [Min Stack with getMedian()](./min-stack-median.md)
- [Sliding Window Maximum (LC 239)](./sliding-window-maximum.md)
- [Design a Stack With Increment Operation (LC 1381)](./stack-with-increment.md)

---

## Real-world Applications

1. **Expression Evaluation**: Track minimum value during parsing
2. **Stock Trading**: Track minimum price in sliding window
3. **Game Development**: Track minimum health/score
4. **Monitoring Systems**: Track minimum metrics
5. **Undo/Redo**: Stack-based operations with state tracking
