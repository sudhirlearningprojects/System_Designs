# Amazon Interview - Design Problems

## Problem 27: LRU Cache (LeetCode 146) ⭐⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: Very High (Asked in 50%+ Amazon interviews)  
**Pattern**: Hash Map + Doubly Linked List

### Problem Statement
Design a data structure that follows the constraints of a Least Recently Used (LRU) cache.

### Solution
```java
class LRUCache {
    class Node {
        int key, value;
        Node prev, next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private Map<Integer, Node> cache;
    private int capacity;
    private Node head, tail;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        cache = new HashMap<>();
        
        head = new Node(0, 0);
        tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) {
            return -1;
        }
        
        Node node = cache.get(key);
        remove(node);
        addToHead(node);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            remove(node);
            addToHead(node);
        } else {
            if (cache.size() == capacity) {
                Node lru = tail.prev;
                remove(lru);
                cache.remove(lru.key);
            }
            
            Node newNode = new Node(key, value);
            cache.put(key, newNode);
            addToHead(newNode);
        }
    }
    
    private void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
}
```
**Time**: O(1) for get and put, **Space**: O(capacity)

### Key Points
- Use HashMap for O(1) lookup
- Use Doubly Linked List for O(1) insertion/deletion
- Most recently used at head, least recently used at tail
- On get/put, move node to head
- On capacity full, remove tail node

---

## Problem 28: Design HashMap (LeetCode 706) ⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: Medium  
**Pattern**: Array + Linked List (Chaining)

### Solution
```java
class MyHashMap {
    class Node {
        int key, value;
        Node next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private static final int SIZE = 10000;
    private Node[] buckets;
    
    public MyHashMap() {
        buckets = new Node[SIZE];
    }
    
    private int hash(int key) {
        return key % SIZE;
    }
    
    public void put(int key, int value) {
        int index = hash(key);
        
        if (buckets[index] == null) {
            buckets[index] = new Node(key, value);
            return;
        }
        
        Node current = buckets[index];
        while (true) {
            if (current.key == key) {
                current.value = value;
                return;
            }
            if (current.next == null) break;
            current = current.next;
        }
        current.next = new Node(key, value);
    }
    
    public int get(int key) {
        int index = hash(key);
        Node current = buckets[index];
        
        while (current != null) {
            if (current.key == key) {
                return current.value;
            }
            current = current.next;
        }
        
        return -1;
    }
    
    public void remove(int key) {
        int index = hash(key);
        Node current = buckets[index];
        
        if (current == null) return;
        
        if (current.key == key) {
            buckets[index] = current.next;
            return;
        }
        
        while (current.next != null) {
            if (current.next.key == key) {
                current.next = current.next.next;
                return;
            }
            current = current.next;
        }
    }
}
```
**Time**: O(1) average, O(n) worst case, **Space**: O(n)

### Key Points
- Use array of linked lists (chaining for collision handling)
- Hash function: key % SIZE
- Handle collisions with linked list
- Update value if key exists

---

**Next**: [Sorting & Searching Problems](07_Sorting_and_Searching.md)
