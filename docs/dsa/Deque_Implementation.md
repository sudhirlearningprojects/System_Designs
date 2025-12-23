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
