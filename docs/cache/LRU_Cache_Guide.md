# LRU Cache - Complete Coding Guide

## System Design Overview

**Problem**: Implement Least Recently Used (LRU) cache with O(1) get/put

**Requirements**:
- get(key): Return value if exists, -1 otherwise
- put(key, value): Insert/update, evict LRU if full
- Both operations in O(1) time

## Data Structure

**HashMap + Doubly Linked List**
- HashMap: O(1) lookup
- DLL: O(1) add/remove, track LRU order

## Complete Implementation

```java
import java.util.*;

class LRUCache {
    class Node {
        int key, value;
        Node prev, next;
        
        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private int capacity;
    private Map<Integer, Node> cache;
    private Node head, tail;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        
        head = new Node(0, 0);
        tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }
    
    public int get(int key) {
        if (!cache.containsKey(key)) {
            System.out.println("GET " + key + " -> MISS");
            return -1;
        }
        
        Node node = cache.get(key);
        remove(node);
        addToHead(node);
        System.out.println("GET " + key + " -> " + node.value);
        return node.value;
    }
    
    public void put(int key, int value) {
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            remove(node);
            addToHead(node);
            System.out.println("PUT " + key + "=" + value + " (updated)");
        } else {
            if (cache.size() >= capacity) {
                Node lru = tail.prev;
                remove(lru);
                cache.remove(lru.key);
                System.out.println("EVICTED " + lru.key);
            }
            
            Node node = new Node(key, value);
            cache.put(key, node);
            addToHead(node);
            System.out.println("PUT " + key + "=" + value);
        }
    }
    
    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    private void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    public void printCache() {
        System.out.print("Cache: [");
        Node curr = head.next;
        while (curr != tail) {
            System.out.print(curr.key + "=" + curr.value);
            if (curr.next != tail) System.out.print(", ");
            curr = curr.next;
        }
        System.out.println("]");
    }
}

public class LRUCacheDemo {
    public static void main(String[] args) {
        System.out.println("=== LRU Cache (capacity=3) ===\n");
        
        LRUCache cache = new LRUCache(3);
        
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);
        cache.printCache();
        
        System.out.println();
        cache.get(1);
        cache.printCache();
        
        System.out.println();
        cache.put(4, 40);
        cache.printCache();
        
        System.out.println();
        cache.get(2);
        
        System.out.println();
        cache.put(5, 50);
        cache.printCache();
    }
}
```

## Time Complexity

- **get()**: O(1) - HashMap lookup + DLL move
- **put()**: O(1) - HashMap insert + DLL add/remove
- **Space**: O(capacity)

## Interview Questions

**Q: Why HashMap + DLL?**
A: HashMap for O(1) lookup, DLL for O(1) add/remove and LRU tracking

**Q: Thread-safe LRU?**
A: Use ConcurrentHashMap + synchronized methods or ReadWriteLock

**Q: Distributed LRU?**
A: Redis with ZADD (score=timestamp) + ZREMRANGEBYRANK

**Q: LFU vs LRU?**
A: LFU evicts least frequently used, better for hot data

Run: https://www.jdoodle.com/online-java-compiler
