# Amazon Interview - Linked Lists Problems

## Problem 21: Reverse Linked List (LeetCode 206) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High  
**Pattern**: Two Pointers

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/reverse-linked-list/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/reverse-a-linked-list/)

### Solution - Iterative
```java
public ListNode reverseList(ListNode head) {
    ListNode prev = null;
    ListNode current = head;
    
    while (current != null) {
        ListNode next = current.next;
        current.next = prev;
        prev = current;
        current = next;
    }
    
    return prev;
}
```
**Time**: O(n), **Space**: O(1)

### Solution - Recursive
```java
public ListNode reverseList(ListNode head) {
    if (head == null || head.next == null) return head;
    
    ListNode newHead = reverseList(head.next);
    head.next.next = head;
    head.next = null;
    
    return newHead;
}
```
**Time**: O(n), **Space**: O(n)

---

## Problem 22: Merge Two Sorted Lists (LeetCode 21) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High  
**Pattern**: Two Pointers

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/merge-two-sorted-lists/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/merge-two-sorted-linked-lists/)

### Solution
```java
public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
    ListNode dummy = new ListNode(0);
    ListNode current = dummy;
    
    while (l1 != null && l2 != null) {
        if (l1.val <= l2.val) {
            current.next = l1;
            l1 = l1.next;
        } else {
            current.next = l2;
            l2 = l2.next;
        }
        current = current.next;
    }
    
    current.next = (l1 != null) ? l1 : l2;
    return dummy.next;
}
```
**Time**: O(n + m), **Space**: O(1)

---

## Problem 23: Copy List with Random Pointer (LeetCode 138) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: Hash Map

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/copy-list-with-random-pointer/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/clone-a-linked-list-with-next-and-random-pointer/)

### Solution
```java
public Node copyRandomList(Node head) {
    if (head == null) return null;
    
    Map<Node, Node> map = new HashMap<>();
    
    // First pass: create all nodes
    Node current = head;
    while (current != null) {
        map.put(current, new Node(current.val));
        current = current.next;
    }
    
    // Second pass: assign next and random pointers
    current = head;
    while (current != null) {
        map.get(current).next = map.get(current.next);
        map.get(current).random = map.get(current.random);
        current = current.next;
    }
    
    return map.get(head);
}
```
**Time**: O(n), **Space**: O(n)

### Optimized Solution - O(1) Space
```java
public Node copyRandomList(Node head) {
    if (head == null) return null;
    
    // Step 1: Create copy nodes interleaved with original
    Node current = head;
    while (current != null) {
        Node copy = new Node(current.val);
        copy.next = current.next;
        current.next = copy;
        current = copy.next;
    }
    
    // Step 2: Assign random pointers
    current = head;
    while (current != null) {
        if (current.random != null) {
            current.next.random = current.random.next;
        }
        current = current.next.next;
    }
    
    // Step 3: Separate the lists
    current = head;
    Node copyHead = head.next;
    Node copyCurrent = copyHead;
    
    while (current != null) {
        current.next = current.next.next;
        if (copyCurrent.next != null) {
            copyCurrent.next = copyCurrent.next.next;
        }
        current = current.next;
        copyCurrent = copyCurrent.next;
    }
    
    return copyHead;
}
```
**Time**: O(n), **Space**: O(1)

---

**Next**: [Stacks & Queues Problems](05_Stacks_and_Queues.md)
