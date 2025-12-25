# Reverse a Doubly Linked List Using Recursion

## Problem Statement

Reverse a doubly linked list using recursion. Each node has three pointers: `data`, `next` (points to next node), and `prev` (points to previous node).

**Input:** Head of a doubly linked list  
**Output:** Head of the reversed doubly linked list

**Examples:**
```
Input:  1 ⇄ 2 ⇄ 3 ⇄ 4 ⇄ NULL
Output: 4 ⇄ 3 ⇄ 2 ⇄ 1 ⇄ NULL

Input:  5 ⇄ NULL
Output: 5 ⇄ NULL

Input:  NULL
Output: NULL

Input:  10 ⇄ 20 ⇄ NULL
Output: 20 ⇄ 10 ⇄ NULL
```

---

## Solution Approaches

### Approach 1: Recursion - Swap Pointers (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n) - recursion stack

```java
class Node {
    int data;
    Node next;
    Node prev;
    
    Node(int data) {
        this.data = data;
    }
}

public static Node reverseRecursive(Node head) {
    if (head == null || head.next == null) {
        return head;
    }
    
    Node temp = head.prev;
    head.prev = head.next;
    head.next = temp;
    
    if (head.prev == null) {
        return head;
    }
    
    return reverseRecursive(head.prev);
}
```

---

### Approach 2: Recursion - Reach End First

**Time Complexity:** O(n)  
**Space Complexity:** O(n) - recursion stack

```java
public static Node reverseReachEnd(Node head) {
    if (head == null) return null;
    
    if (head.next == null) {
        head.prev = null;
        return head;
    }
    
    Node newHead = reverseReachEnd(head.next);
    head.next.next = head;
    head.prev = head.next;
    head.next = null;
    
    return newHead;
}
```

---

### Approach 3: Iterative (For Comparison)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static Node reverseIterative(Node head) {
    Node current = head;
    Node temp = null;
    
    while (current != null) {
        temp = current.prev;
        current.prev = current.next;
        current.next = temp;
        current = current.prev;
    }
    
    if (temp != null) {
        head = temp.prev;
    }
    
    return head;
}
```

---

## Algorithm Walkthrough

### Example: 1 ⇄ 2 ⇄ 3 ⇄ 4 ⇄ NULL

**Recursive Approach (Swap Pointers):**

```
Initial: 1 ⇄ 2 ⇄ 3 ⇄ 4 ⇄ NULL

Call 1: reverseRecursive(1)
  Swap: 1.prev = 2, 1.next = NULL
  State: NULL ← 1 ⇄ 2 ⇄ 3 ⇄ 4 ⇄ NULL
  Recurse on head.prev (which is 2)

Call 2: reverseRecursive(2)
  Swap: 2.prev = 3, 2.next = 1
  State: NULL ← 1 ← 2 ⇄ 3 ⇄ 4 ⇄ NULL
  Recurse on head.prev (which is 3)

Call 3: reverseRecursive(3)
  Swap: 3.prev = 4, 3.next = 2
  State: NULL ← 1 ← 2 ← 3 ⇄ 4 ⇄ NULL
  Recurse on head.prev (which is 4)

Call 4: reverseRecursive(4)
  Swap: 4.prev = NULL, 4.next = 3
  State: NULL ← 1 ← 2 ← 3 ← 4 → NULL
  head.prev == null, return 4

Result: 4 ⇄ 3 ⇄ 2 ⇄ 1 ⇄ NULL
```

### Visual Step-by-Step

```
Original:
NULL ← 1 ⇄ 2 ⇄ 3 ⇄ 4 → NULL
       ↑
      head

After Call 1 (swap at node 1):
NULL ← 1   2 ⇄ 3 ⇄ 4 → NULL
       ↓   ↑
       ↓___↑

After Call 2 (swap at node 2):
NULL ← 1 ← 2   3 ⇄ 4 → NULL
           ↓   ↑
           ↓___↑

After Call 3 (swap at node 3):
NULL ← 1 ← 2 ← 3   4 → NULL
               ↓   ↑
               ↓___↑

After Call 4 (swap at node 4):
NULL ← 1 ← 2 ← 3 ← 4 → NULL
                   ↑
                 newHead

Final:
NULL ← 4 ⇄ 3 ⇄ 2 ⇄ 1 → NULL
       ↑
     newHead
```

---

## Complete Implementation

```java
class Node {
    int data;
    Node next;
    Node prev;
    
    Node(int data) {
        this.data = data;
        this.next = null;
        this.prev = null;
    }
}

public class Solution {
    
    // Approach 1: Recursion - Swap Pointers (Optimal)
    public static Node reverseRecursive(Node head) {
        if (head == null || head.next == null) {
            return head;
        }
        
        Node temp = head.prev;
        head.prev = head.next;
        head.next = temp;
        
        if (head.prev == null) {
            return head;
        }
        
        return reverseRecursive(head.prev);
    }
    
    // Approach 2: Recursion - Reach End First
    public static Node reverseReachEnd(Node head) {
        if (head == null) return null;
        
        if (head.next == null) {
            head.prev = null;
            return head;
        }
        
        Node newHead = reverseReachEnd(head.next);
        head.next.next = head;
        head.prev = head.next;
        head.next = null;
        
        return newHead;
    }
    
    // Approach 3: Iterative (For Comparison)
    public static Node reverseIterative(Node head) {
        Node current = head;
        Node temp = null;
        
        while (current != null) {
            temp = current.prev;
            current.prev = current.next;
            current.next = temp;
            current = current.prev;
        }
        
        if (temp != null) {
            head = temp.prev;
        }
        
        return head;
    }
    
    // Helper: Create doubly linked list from array
    public static Node createList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        
        Node head = new Node(arr[0]);
        Node current = head;
        
        for (int i = 1; i < arr.length; i++) {
            Node newNode = new Node(arr[i]);
            current.next = newNode;
            newNode.prev = current;
            current = newNode;
        }
        
        return head;
    }
    
    // Helper: Convert list to array
    public static int[] toArray(Node head) {
        int size = 0;
        Node temp = head;
        while (temp != null) {
            size++;
            temp = temp.next;
        }
        
        int[] arr = new int[size];
        temp = head;
        for (int i = 0; i < size; i++) {
            arr[i] = temp.data;
            temp = temp.next;
        }
        
        return arr;
    }
    
    // Helper: Print list
    public static void printList(Node head) {
        Node temp = head;
        while (temp != null) {
            System.out.print(temp.data);
            if (temp.next != null) System.out.print(" ⇄ ");
            temp = temp.next;
        }
        System.out.println(" ⇄ NULL");
    }
    
    // Helper: Verify doubly linked list integrity
    public static boolean verifyList(Node head) {
        if (head == null) return true;
        if (head.prev != null) return false;
        
        Node current = head;
        while (current.next != null) {
            if (current.next.prev != current) return false;
            current = current.next;
        }
        
        return true;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Multiple elements
        Node head1 = createList(new int[]{1, 2, 3, 4});
        head1 = reverseRecursive(head1);
        int[] result1 = toArray(head1);
        if (!java.util.Arrays.equals(result1, new int[]{4, 3, 2, 1})) return false;
        if (!verifyList(head1)) return false;
        
        // Test 2: Single element
        Node head2 = createList(new int[]{5});
        head2 = reverseRecursive(head2);
        int[] result2 = toArray(head2);
        if (!java.util.Arrays.equals(result2, new int[]{5})) return false;
        
        // Test 3: Two elements
        Node head3 = createList(new int[]{10, 20});
        head3 = reverseRecursive(head3);
        int[] result3 = toArray(head3);
        if (!java.util.Arrays.equals(result3, new int[]{20, 10})) return false;
        
        // Test 4: Empty list
        Node head4 = reverseRecursive(null);
        if (head4 != null) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        System.out.println("Original list:");
        Node head = createList(new int[]{1, 2, 3, 4, 5});
        printList(head);
        
        System.out.println("\nReversed list (Recursive):");
        head = reverseRecursive(head);
        printList(head);
        
        System.out.println("\nList integrity: " + verifyList(head));
    }
}
```

---

## Test Cases

```java
@Test
public void testReverseDoublyLinkedList() {
    // Test 1: Multiple elements
    Node head1 = createList(new int[]{1, 2, 3, 4, 5});
    head1 = reverseRecursive(head1);
    assertArrayEquals(new int[]{5, 4, 3, 2, 1}, toArray(head1));
    assertTrue(verifyList(head1));
    
    // Test 2: Single element
    Node head2 = createList(new int[]{42});
    head2 = reverseRecursive(head2);
    assertArrayEquals(new int[]{42}, toArray(head2));
    assertTrue(verifyList(head2));
    
    // Test 3: Two elements
    Node head3 = createList(new int[]{10, 20});
    head3 = reverseRecursive(head3);
    assertArrayEquals(new int[]{20, 10}, toArray(head3));
    assertTrue(verifyList(head3));
    
    // Test 4: Empty list
    Node head4 = reverseRecursive(null);
    assertNull(head4);
    
    // Test 5: Large list
    int[] large = new int[100];
    for (int i = 0; i < 100; i++) large[i] = i;
    Node head5 = createList(large);
    head5 = reverseRecursive(head5);
    int[] expected = new int[100];
    for (int i = 0; i < 100; i++) expected[i] = 99 - i;
    assertArrayEquals(expected, toArray(head5));
    assertTrue(verifyList(head5));
    
    // Test 6: Verify prev pointers
    Node head6 = createList(new int[]{1, 2, 3});
    head6 = reverseRecursive(head6);
    assertEquals(3, head6.data);
    assertNull(head6.prev);
    assertEquals(2, head6.next.data);
    assertEquals(head6, head6.next.prev);
}
```

---

## Visual Representation

### Pointer Swapping Process

```
Original Node Structure:
┌─────────────────┐
│  prev │ data │ next │
└─────────────────┘

Before Swap (Node 2):
NULL ← 1 ⇄ 2 ⇄ 3 → NULL
           ↑
         current

Swap Operation:
temp = current.prev  (temp = 1)
current.prev = current.next  (prev = 3)
current.next = temp  (next = 1)

After Swap (Node 2):
NULL ← 1 ← 2 → 3 → NULL
           ↑
         current
```

### Recursion Tree

```
reverseRecursive(1)
│
├─ Swap pointers at node 1
│  (1.prev = 2, 1.next = NULL)
│
└─ reverseRecursive(2)
   │
   ├─ Swap pointers at node 2
   │  (2.prev = 3, 2.next = 1)
   │
   └─ reverseRecursive(3)
      │
      ├─ Swap pointers at node 3
      │  (3.prev = 4, 3.next = 2)
      │
      └─ reverseRecursive(4)
         │
         ├─ Swap pointers at node 4
         │  (4.prev = NULL, 4.next = 3)
         │
         └─ Return 4 (new head)
```

---

## Edge Cases

1. **Empty list (NULL):** Return NULL
2. **Single node:** Return same node
3. **Two nodes:** Swap and return second node
4. **Large list:** Stack overflow risk (use iterative for very large lists)
5. **Verify prev pointers:** Ensure bidirectional integrity
6. **Head.prev should be NULL:** After reversal

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Recursive (Swap) | O(n) | O(n) | **Clean recursive solution** |
| Recursive (Reach End) | O(n) | O(n) | Alternative recursive approach |
| Iterative | O(n) | O(1) | **Best space complexity** |

**Space Complexity Breakdown:**
- Recursive: O(n) stack space for n recursive calls
- Iterative: O(1) only uses temp variable

**When to Use:**
- **Recursion:** Clean, elegant code; small to medium lists
- **Iterative:** Large lists (avoid stack overflow); production code

---

## Related Problems

1. **Reverse Singly Linked List** - Only next pointer
2. **Reverse in Groups** - Reverse k nodes at a time
3. **Palindrome Linked List** - Check if list is palindrome
4. **Rotate Doubly Linked List** - Rotate by k positions
5. **Clone Doubly Linked List** - Deep copy with random pointer
6. **Flatten Multilevel Doubly Linked List** - Flatten nested structure

---

## Interview Tips

### Clarification Questions
1. Can the list be empty? (Yes, return NULL)
2. Should I use recursion or iteration? (Recursion required)
3. Do I need to verify list integrity? (Good practice)
4. What about very large lists? (Mention stack overflow risk)
5. Should prev pointer of head be NULL? (Yes)

### Approach Explanation
1. "I'll use recursion to swap next and prev pointers at each node"
2. "Base case: empty list or single node"
3. "Recursive case: swap pointers, recurse on new next (old prev)"
4. "Return new head when prev becomes NULL"
5. "Time O(n), Space O(n) due to recursion stack"

### Common Mistakes
1. **Forgetting to swap both pointers** - Must swap both next and prev
2. **Not handling base cases** - NULL and single node
3. **Losing reference to new head** - Track when prev becomes NULL
4. **Not verifying prev pointers** - Ensure bidirectional integrity
5. **Stack overflow** - Mention iterative alternative for large lists

### Follow-up Questions
1. "Can you do it iteratively?" - Yes, O(1) space
2. "How to reverse in groups of k?" - Track k nodes, reverse each group
3. "What if list is circular?" - Detect cycle, handle differently
4. "Space optimization?" - Iterative approach uses O(1) space

---

## Real-World Applications

1. **Undo/Redo Operations** - Browser history, text editors
2. **Music Playlist** - Navigate forward/backward
3. **LRU Cache** - Doubly linked list for O(1) operations
4. **Memory Management** - Free block lists
5. **Navigation Systems** - Route history
6. **Database Transactions** - Rollback operations

---

## Key Takeaways

1. **Recursion elegantly reverses doubly linked list** by swapping pointers
2. **Base case crucial:** Empty list or single node
3. **Swap both pointers:** next and prev at each node
4. **Track new head:** When prev becomes NULL after swap
5. **Space trade-off:** O(n) recursion stack vs O(1) iterative
6. **Verify integrity:** Check bidirectional links after reversal
7. **Production consideration:** Use iterative for very large lists to avoid stack overflow

---

## Optimization Notes

### Why Recursion Here?
- **Cleaner code:** More readable than iterative
- **Natural fit:** Linked list structure suits recursion
- **Interview preference:** Shows understanding of recursion

### When to Avoid Recursion?
- **Very large lists:** Risk of stack overflow (>10,000 nodes)
- **Production systems:** Iterative more reliable
- **Memory constraints:** O(1) space preferred

### Best Practice
```java
// Add stack depth check for safety
public static Node reverseRecursiveSafe(Node head, int depth) {
    if (depth > 10000) {
        throw new StackOverflowError("List too large for recursion");
    }
    // ... rest of recursive logic
}
```
