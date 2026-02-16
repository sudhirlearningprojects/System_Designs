# Deque (Double-Ended Queue) Implementation

## Problem Statement

Implement a double-ended queue (deque) that stores strings with operations at both ends.

**Operations:**
- `addFirst(String)` - Add element at front
- `addLast(String)` - Add element at rear
- `removeFirst()` - Remove and return front element
- `removeLast()` - Remove and return rear element
- `peekFirst()` - View front element without removing
- `peekLast()` - View rear element without removing
- `isEmpty()` - Check if deque is empty
- `size()` - Get number of elements

---

## How Each Operation Works (In-Depth)

### 1. addFirst(String item) - Add Element at Front

**Concept:** Insert a new element at the beginning of the deque, shifting the front pointer backward.

**Algorithm Steps:**
1. Check if deque is full → resize if needed
2. Move front pointer backward (circularly)
3. Place item at new front position
4. Increment size

**Circular Array Math:**
```java
front = (front - 1 + data.length) % data.length;
```
- Subtract 1 to move backward
- Add `data.length` to handle negative wraparound
- Modulo ensures circular behavior

**Example Walkthrough:**
```
Initial State: capacity=5, size=0
[_, _, _, _, _]
 ^front=0, rear=0

Step 1: addFirst("A")
- front = (0 - 1 + 5) % 5 = 4
- data[4] = "A"
- size = 1

[_, _, _, _, A]
             ^front=4
 ^rear=0

Step 2: addFirst("B")
- front = (4 - 1 + 5) % 5 = 3
- data[3] = "B"
- size = 2

[_, _, _, B, A]
          ^front=3
 ^rear=0

Step 3: addFirst("C")
- front = (3 - 1 + 5) % 5 = 2
- data[2] = "C"
- size = 3

[_, _, C, B, A]
       ^front=2
 ^rear=0
```

**Why Circular?** Without circular logic, we'd run out of space at index 0 even if indices 1-4 are empty.

---

### 2. addLast(String item) - Add Element at Rear

**Concept:** Insert a new element at the end of the deque, moving the rear pointer forward.

**Algorithm Steps:**
1. Check if deque is full → resize if needed
2. Place item at current rear position
3. Move rear pointer forward (circularly)
4. Increment size

**Circular Array Math:**
```java
rear = (rear + 1) % data.length;
```
- Add 1 to move forward
- Modulo wraps around to index 0 when reaching end

**Example Walkthrough:**
```
Initial State: capacity=5, size=0
[_, _, _, _, _]
 ^front=0, rear=0

Step 1: addLast("X")
- data[0] = "X"
- rear = (0 + 1) % 5 = 1
- size = 1

[X, _, _, _, _]
 ^front=0
    ^rear=1

Step 2: addLast("Y")
- data[1] = "Y"
- rear = (1 + 1) % 5 = 2
- size = 2

[X, Y, _, _, _]
 ^front=0
       ^rear=2

Step 3: addLast("Z")
- data[2] = "Z"
- rear = (2 + 1) % 5 = 3
- size = 3

[X, Y, Z, _, _]
 ^front=0
          ^rear=3
```

**Combined Example (addFirst + addLast):**
```
Start: [_, _, _, _, _] front=0, rear=0

addLast("A"):  [A, _, _, _, _] front=0, rear=1
addLast("B"):  [A, B, _, _, _] front=0, rear=2
addFirst("Z"): [A, B, _, _, Z] front=4, rear=2
addFirst("Y"): [A, B, _, Y, Z] front=3, rear=2
addLast("C"):  [A, B, C, Y, Z] front=3, rear=3

Logical Order: Y → Z → A → B → C
Array Indices: [3] [4] [0] [1] [2]
```

---

### 3. removeFirst() - Remove Element from Front

**Concept:** Remove and return the front element, moving the front pointer forward.

**Algorithm Steps:**
1. Check if deque is empty → throw exception
2. Retrieve item at front position
3. Set front position to null (garbage collection)
4. Move front pointer forward (circularly)
5. Decrement size
6. Return retrieved item

**Circular Array Math:**
```java
front = (front + 1) % data.length;
```

**Example Walkthrough:**
```
Initial State: [A, B, C, Y, Z] front=3, rear=3, size=5
Logical Order: Y → Z → A → B → C

Step 1: removeFirst() → returns "Y"
- item = data[3] = "Y"
- data[3] = null
- front = (3 + 1) % 5 = 4
- size = 4

[A, B, C, _, Z] front=4, rear=3
Logical Order: Z → A → B → C

Step 2: removeFirst() → returns "Z"
- item = data[4] = "Z"
- data[4] = null
- front = (4 + 1) % 5 = 0
- size = 3

[A, B, C, _, _] front=0, rear=3
Logical Order: A → B → C

Step 3: removeFirst() → returns "A"
- item = data[0] = "A"
- data[0] = null
- front = (0 + 1) % 5 = 1
- size = 2

[_, B, C, _, _] front=1, rear=3
Logical Order: B → C
```

---

### 4. removeLast() - Remove Element from Rear

**Concept:** Remove and return the rear element, moving the rear pointer backward.

**Algorithm Steps:**
1. Check if deque is empty → throw exception
2. Move rear pointer backward (circularly)
3. Retrieve item at new rear position
4. Set rear position to null
5. Decrement size
6. Return retrieved item

**Circular Array Math:**
```java
rear = (rear - 1 + data.length) % data.length;
```

**Example Walkthrough:**
```
Initial State: [A, B, C, _, _] front=0, rear=3, size=3
Logical Order: A → B → C

Step 1: removeLast() → returns "C"
- rear = (3 - 1 + 5) % 5 = 2
- item = data[2] = "C"
- data[2] = null
- size = 2

[A, B, _, _, _] front=0, rear=2
Logical Order: A → B

Step 2: removeLast() → returns "B"
- rear = (2 - 1 + 5) % 5 = 1
- item = data[1] = "B"
- data[1] = null
- size = 1

[A, _, _, _, _] front=0, rear=1
Logical Order: A

Step 3: removeLast() → returns "A"
- rear = (1 - 1 + 5) % 5 = 0
- item = data[0] = "A"
- data[0] = null
- size = 0

[_, _, _, _, _] front=0, rear=0
Logical Order: (empty)
```

---

### 5. peekFirst() - View Front Element

**Concept:** Return the front element without removing it.

**Algorithm Steps:**
1. Check if deque is empty → throw exception
2. Return item at front position
3. No state changes (front, rear, size remain same)

**Example:**
```
State: [_, _, C, B, A] front=2, rear=0, size=3
Logical Order: C → B → A

peekFirst() → returns "C"
- Simply returns data[front] = data[2] = "C"
- No changes to array or pointers

State after: [_, _, C, B, A] front=2, rear=0, size=3 (unchanged)
```

---

### 6. peekLast() - View Rear Element

**Concept:** Return the rear element without removing it.

**Algorithm Steps:**
1. Check if deque is empty → throw exception
2. Calculate last valid index (rear - 1, circularly)
3. Return item at that position
4. No state changes

**Circular Array Math:**
```java
int lastIndex = (rear - 1 + data.length) % data.length;
```

**Why rear - 1?** The `rear` pointer always points to the NEXT empty position, not the last element.

**Example:**
```
State: [_, _, C, B, A] front=2, rear=0, size=3
Logical Order: C → B → A

peekLast() → returns "A"
- lastIndex = (0 - 1 + 5) % 5 = 4
- Returns data[4] = "A"
- No changes to array or pointers

State after: [_, _, C, B, A] front=2, rear=0, size=3 (unchanged)
```

---

### 7. isEmpty() - Check if Empty

**Concept:** Determine if deque contains any elements.

**Algorithm:**
```java
return size == 0;
```

**Example:**
```
Case 1: [_, _, _, _, _] front=0, rear=0, size=0
isEmpty() → true

Case 2: [A, _, _, _, _] front=0, rear=1, size=1
isEmpty() → false

Case 3: [A, B, C, D, E] front=0, rear=0, size=5 (full, wrapped)
isEmpty() → false
```

**Why not use front == rear?** Because front == rear can mean both empty AND full in a circular buffer.

---

### 8. size() - Get Element Count

**Concept:** Return the number of elements currently in the deque.

**Algorithm:**
```java
return size;
```

**Example:**
```
State: [A, B, C, Y, Z] front=3, rear=3, size=5
size() → 5

After removeFirst():
State: [A, B, C, _, Z] front=4, rear=3, size=4
size() → 4
```

---

### 9. resize() - Dynamic Array Expansion

**Concept:** When the array is full, create a larger array and copy elements in logical order.

**Algorithm Steps:**
1. Create new array with double capacity
2. Copy elements from old array in logical order (not physical order)
3. Reset front to 0, rear to size
4. Replace old array with new array

**Example Walkthrough:**
```
Before Resize: capacity=5, size=5 (FULL)
[A, B, C, Y, Z] front=3, rear=3
Logical Order: Y → Z → A → B → C

Resize Process:
1. Create newData[10]
2. Copy in logical order:
   - i=0: newData[0] = data[(3+0)%5] = data[3] = "Y"
   - i=1: newData[1] = data[(3+1)%5] = data[4] = "Z"
   - i=2: newData[2] = data[(3+2)%5] = data[0] = "A"
   - i=3: newData[3] = data[(3+3)%5] = data[1] = "B"
   - i=4: newData[4] = data[(3+4)%5] = data[2] = "C"
3. front = 0, rear = 5

After Resize: capacity=10, size=5
[Y, Z, A, B, C, _, _, _, _, _] front=0, rear=5
Logical Order: Y → Z → A → B → C
```

**Why Copy in Logical Order?** To eliminate wraparound and simplify the new array layout.

---

## Complete Visual Example: All Operations

```
Initial: capacity=5
[_, _, _, _, _] front=0, rear=0, size=0

1. addLast("A"):
   [A, _, _, _, _] front=0, rear=1, size=1
   Order: A

2. addLast("B"):
   [A, B, _, _, _] front=0, rear=2, size=2
   Order: A → B

3. addFirst("Z"):
   [A, B, _, _, Z] front=4, rear=2, size=3
   Order: Z → A → B

4. addFirst("Y"):
   [A, B, _, Y, Z] front=3, rear=2, size=4
   Order: Y → Z → A → B

5. addLast("C"):
   [A, B, C, Y, Z] front=3, rear=3, size=5 (FULL)
   Order: Y → Z → A → B → C

6. peekFirst() → "Y" (no change)
   [A, B, C, Y, Z] front=3, rear=3, size=5

7. peekLast() → "C" (no change)
   [A, B, C, Y, Z] front=3, rear=3, size=5

8. removeFirst() → "Y":
   [A, B, C, _, Z] front=4, rear=3, size=4
   Order: Z → A → B → C

9. removeLast() → "C":
   [A, B, _, _, Z] front=4, rear=2, size=3
   Order: Z → A → B

10. size() → 3
    isEmpty() → false
```

---

## Solution Approaches

### Approach 1: Array-Based (Circular Buffer)

**Time Complexity:** O(1) for all operations  
**Space Complexity:** O(n)

```java
public class ArrayDeque {
    private String[] data;
    private int front;
    private int rear;
    private int size;
    
    public ArrayDeque(int capacity) {
        data = new String[capacity];
        front = 0;
        rear = 0;
        size = 0;
    }
    
    public void addFirst(String item) {
        if (size == data.length) resize();
        front = (front - 1 + data.length) % data.length;
        data[front] = item;
        size++;
    }
    
    public void addLast(String item) {
        if (size == data.length) resize();
        data[rear] = item;
        rear = (rear + 1) % data.length;
        size++;
    }
    
    public String removeFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        String item = data[front];
        data[front] = null;
        front = (front + 1) % data.length;
        size--;
        return item;
    }
    
    public String removeLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        rear = (rear - 1 + data.length) % data.length;
        String item = data[rear];
        data[rear] = null;
        size--;
        return item;
    }
    
    public String peekFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        return data[front];
    }
    
    public String peekLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        int lastIndex = (rear - 1 + data.length) % data.length;
        return data[lastIndex];
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    private void resize() {
        String[] newData = new String[data.length * 2];
        for (int i = 0; i < size; i++) {
            newData[i] = data[(front + i) % data.length];
        }
        data = newData;
        front = 0;
        rear = size;
    }
}
```

---

### Approach 2: Doubly Linked List

**Time Complexity:** O(1) for all operations  
**Space Complexity:** O(n)

```java
public class LinkedDeque {
    private Node head;
    private Node tail;
    private int size;
    
    private static class Node {
        String data;
        Node prev;
        Node next;
        
        Node(String data) {
            this.data = data;
        }
    }
    
    public void addFirst(String item) {
        Node newNode = new Node(item);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        size++;
    }
    
    public void addLast(String item) {
        Node newNode = new Node(item);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
    }
    
    public String removeFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        String item = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return item;
    }
    
    public String removeLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        String item = tail.data;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        size--;
        return item;
    }
    
    public String peekFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        return head.data;
    }
    
    public String peekLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        return tail.data;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
}
```

---

## Visual Representation

### Array-Based Deque (Circular Buffer)

```
Initial: capacity = 5
[_, _, _, _, _]
 ^front/rear

addLast("A"):
[A, _, _, _, _]
 ^front    ^rear

addLast("B"):
[A, B, _, _, _]
 ^front       ^rear

addFirst("Z"):
[A, B, _, _, Z]
             ^front
          ^rear

removeFirst():
[A, B, _, _, _]
    ^front    ^rear
```

### Linked List Deque

```
addFirst("A"):
head/tail → [A] ← head/tail

addLast("B"):
head → [A] ⇄ [B] ← tail

addFirst("Z"):
head → [Z] ⇄ [A] ⇄ [B] ← tail

removeFirst():
head → [A] ⇄ [B] ← tail
```

---

## Complete Implementation with All Features

```java
public class Deque {
    private String[] data;
    private int front;
    private int rear;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;
    
    public Deque() {
        this(DEFAULT_CAPACITY);
    }
    
    public Deque(int capacity) {
        data = new String[capacity];
        front = 0;
        rear = 0;
        size = 0;
    }
    
    public void addFirst(String item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (size == data.length) resize();
        
        front = (front - 1 + data.length) % data.length;
        data[front] = item;
        size++;
    }
    
    public void addLast(String item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (size == data.length) resize();
        
        data[rear] = item;
        rear = (rear + 1) % data.length;
        size++;
    }
    
    public String removeFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        
        String item = data[front];
        data[front] = null;
        front = (front + 1) % data.length;
        size--;
        
        return item;
    }
    
    public String removeLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        
        rear = (rear - 1 + data.length) % data.length;
        String item = data[rear];
        data[rear] = null;
        size--;
        
        return item;
    }
    
    public String peekFirst() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        return data[front];
    }
    
    public String peekLast() {
        if (isEmpty()) throw new IllegalStateException("Deque is empty");
        int lastIndex = (rear - 1 + data.length) % data.length;
        return data[lastIndex];
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    public int size() {
        return size;
    }
    
    public void clear() {
        for (int i = 0; i < size; i++) {
            data[(front + i) % data.length] = null;
        }
        front = 0;
        rear = 0;
        size = 0;
    }
    
    private void resize() {
        String[] newData = new String[data.length * 2];
        for (int i = 0; i < size; i++) {
            newData[i] = data[(front + i) % data.length];
        }
        data = newData;
        front = 0;
        rear = size;
    }
    
    @Override
    public String toString() {
        if (isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(data[(front + i) % data.length]);
            if (i < size - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
```

---

## Test Cases

```java
@Test
public void testDeque() {
    Deque deque = new Deque(5);
    
    // Test empty
    assertTrue(deque.isEmpty());
    assertEquals(0, deque.size());
    
    // Test addLast
    deque.addLast("A");
    deque.addLast("B");
    assertEquals(2, deque.size());
    assertEquals("A", deque.peekFirst());
    assertEquals("B", deque.peekLast());
    
    // Test addFirst
    deque.addFirst("Z");
    assertEquals(3, deque.size());
    assertEquals("Z", deque.peekFirst());
    assertEquals("B", deque.peekLast());
    
    // Test removeFirst
    assertEquals("Z", deque.removeFirst());
    assertEquals(2, deque.size());
    assertEquals("A", deque.peekFirst());
    
    // Test removeLast
    assertEquals("B", deque.removeLast());
    assertEquals(1, deque.size());
    assertEquals("A", deque.peekFirst());
    assertEquals("A", deque.peekLast());
    
    // Test clear
    deque.clear();
    assertTrue(deque.isEmpty());
    
    // Test resize
    for (int i = 0; i < 10; i++) {
        deque.addLast("Item" + i);
    }
    assertEquals(10, deque.size());
    
    // Test exceptions
    deque.clear();
    assertThrows(IllegalStateException.class, () -> deque.removeFirst());
    assertThrows(IllegalStateException.class, () -> deque.removeLast());
    assertThrows(IllegalStateException.class, () -> deque.peekFirst());
    assertThrows(IllegalStateException.class, () -> deque.peekLast());
}
```

---

## Complexity Analysis

| Operation | Array-Based | Linked List | Notes |
|-----------|-------------|-------------|-------|
| addFirst | O(1) | O(1) | Amortized for array |
| addLast | O(1) | O(1) | Amortized for array |
| removeFirst | O(1) | O(1) | - |
| removeLast | O(1) | O(1) | - |
| peekFirst | O(1) | O(1) | - |
| peekLast | O(1) | O(1) | - |
| Space | O(n) | O(n) | Array may waste space |

---

## Circular Buffer Explanation

### Modulo Arithmetic for Wrapping

```java
// Move front backward (wrap around)
front = (front - 1 + data.length) % data.length;

// Move rear forward (wrap around)
rear = (rear + 1) % data.length;

// Example with length = 5:
// front = 0, move back: (0 - 1 + 5) % 5 = 4
// rear = 4, move forward: (4 + 1) % 5 = 0
```

### Why Add `data.length` Before Modulo?

```java
// WRONG - negative result
front = (front - 1) % data.length;  // -1 % 5 = -1

// CORRECT - always positive
front = (front - 1 + data.length) % data.length;  // 4 % 5 = 4
```

---

## Common Mistakes

1. **Not Handling Wrap-Around:**
   ```java
   // WRONG
   front = front - 1;
   
   // CORRECT
   front = (front - 1 + data.length) % data.length;
   ```

2. **Incorrect peekLast Index:**
   ```java
   // WRONG - rear points to next empty slot
   return data[rear];
   
   // CORRECT - rear - 1 is last element
   return data[(rear - 1 + data.length) % data.length];
   ```

3. **Not Resizing:**
   ```java
   // Check before adding
   if (size == data.length) resize();
   ```

4. **Memory Leak (Not Nullifying):**
   ```java
   // Set to null to allow garbage collection
   data[front] = null;
   ```

---

## Use Cases

### Stack (LIFO)
```java
// Use addFirst + removeFirst
deque.addFirst("A");
deque.addFirst("B");
deque.removeFirst(); // Returns "B"
```

### Queue (FIFO)
```java
// Use addLast + removeFirst
deque.addLast("A");
deque.addLast("B");
deque.removeFirst(); // Returns "A"
```

### Palindrome Checker
```java
public boolean isPalindrome(String str) {
    Deque deque = new Deque();
    for (char c : str.toCharArray()) {
        deque.addLast(String.valueOf(c));
    }
    
    while (deque.size() > 1) {
        if (!deque.removeFirst().equals(deque.removeLast())) {
            return false;
        }
    }
    return true;
}
```

### Sliding Window Maximum
```java
// Store indices in deque, maintain decreasing order
```

---

## Array vs Linked List Trade-offs

### Array-Based ✅
- Better cache locality
- Less memory overhead per element
- Predictable memory usage
- Faster for small to medium sizes

### Linked List ✅
- No resizing needed
- True O(1) for all operations
- No wasted space
- Better for frequent size changes

---

## Related Problems

- **LeetCode 641:** Design Circular Deque
- **LeetCode 239:** Sliding Window Maximum
- **LeetCode 346:** Moving Average from Data Stream
- **LeetCode 622:** Design Circular Queue

---

## Interview Tips

1. **Clarify Requirements:**
   - Fixed or dynamic size?
   - Thread-safe needed?
   - Null elements allowed?

2. **Choose Implementation:**
   - Array for cache efficiency
   - Linked list for simplicity

3. **Explain Circular Buffer:**
   - Modulo arithmetic
   - Wrap-around logic

4. **Handle Edge Cases:**
   - Empty deque
   - Single element
   - Full capacity

5. **Discuss Complexity:**
   - All operations O(1)
   - Amortized for array resize

---

## Real-World Applications

- **Browser History:** Back/forward navigation
- **Undo/Redo:** Text editors
- **Task Scheduling:** Priority at both ends
- **Sliding Window:** Algorithm problems
- **LRU Cache:** Eviction policy
- **Work Stealing:** Thread pool queues

---

## Key Takeaways

✅ Deque supports operations at both ends  
✅ Array-based uses circular buffer with modulo  
✅ Linked list uses doubly-linked nodes  
✅ All operations are O(1)  
✅ Can implement stack or queue  
✅ Add `data.length` before modulo for negative indices  
✅ Array-based is generally faster due to cache locality
