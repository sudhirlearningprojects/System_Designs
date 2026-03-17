# Data Structure Design Problems

Complete solutions with multiple approaches for fundamental data structure design problems.

## 📚 Problems List

### Cache Design
1. **[LRU Cache (LC 146)](./lru-cache.md)** ⭐⭐⭐
   - HashMap + Doubly Linked List
   - O(1) get and put operations
   - **Difficulty**: Medium
   - **Key Concepts**: Hash table, DLL, eviction policy

2. **[LFU Cache (LC 460)](./lfu-cache.md)** ⭐⭐⭐
   - HashMap + Frequency Map + DLL
   - O(1) get and put with frequency tracking
   - **Difficulty**: Hard
   - **Key Concepts**: Multi-level indexing, tie-breaking

### Stack Design
3. **[Min Stack (LC 155)](./min-stack.md)** ⭐⭐
   - Two stacks approach
   - O(1) getMin operation
   - **Difficulty**: Medium
   - **Key Concepts**: Auxiliary data structure

4. **Max Stack (LC 716)** ⭐⭐
   - Similar to Min Stack
   - O(1) getMax operation
   - **Difficulty**: Medium

### Hash-based Design
5. **Design HashMap (LC 706)** ⭐
   - Array + Linked List (chaining)
   - Handle collisions
   - **Difficulty**: Easy

6. **Design HashSet (LC 705)** ⭐
   - Similar to HashMap
   - Boolean presence tracking
   - **Difficulty**: Easy

### Queue Design
7. **Design Circular Queue (LC 622)** ⭐
   - Array-based circular buffer
   - Fixed size queue
   - **Difficulty**: Medium

8. **Design Circular Deque (LC 641)** ⭐⭐
   - Double-ended queue
   - Insert/delete from both ends
   - **Difficulty**: Medium

### Application-Specific Design
9. **Design Browser History (LC 1472)** ⭐
   - Two stacks or doubly linked list
   - Back/forward navigation
   - **Difficulty**: Easy

10. **Design Underground System (LC 1396)** ⭐⭐
    - HashMap for tracking trips
    - Average time calculation
    - **Difficulty**: Medium

11. **Design Parking System (LC 1603)** ⭐
    - Simple counter-based
    - Three vehicle types
    - **Difficulty**: Easy

12. **Design Authentication Manager (LC 1797)** ⭐
    - Token expiration tracking
    - HashMap + cleanup
    - **Difficulty**: Medium

## 🎯 Problem Patterns

### Pattern 1: Dual Data Structure
**Problems**: LRU Cache, LFU Cache, Min Stack
**Key Idea**: Use two complementary data structures
- One for fast lookup (HashMap)
- One for ordering/tracking (LinkedList, Stack)

### Pattern 2: Circular Buffer
**Problems**: Circular Queue, Circular Deque
**Key Idea**: Fixed-size array with wrap-around indexing
```java
index = (index + 1) % capacity
```

### Pattern 3: Lazy Cleanup
**Problems**: Authentication Manager, Hit Counter
**Key Idea**: Clean up expired data on access, not proactively

### Pattern 4: Auxiliary Tracking
**Problems**: Min Stack, Max Stack
**Key Idea**: Maintain additional structure to track aggregate info

## 📊 Complexity Quick Reference

| Problem | Insert/Add | Remove/Delete | Get/Access | Space |
|---------|-----------|---------------|------------|-------|
| LRU Cache | O(1) | O(1) | O(1) | O(n) |
| LFU Cache | O(1) | O(1) | O(1) | O(n) |
| Min Stack | O(1) | O(1) | O(1) | O(n) |
| HashMap | O(1) avg | O(1) avg | O(1) avg | O(n) |
| Circular Queue | O(1) | O(1) | O(1) | O(k) |

## 🔑 Key Takeaways

### 1. Trade-offs
- **Time vs Space**: Often sacrifice space for time (e.g., Min Stack uses 2x space)
- **Simplicity vs Optimality**: LinkedHashMap is simple but less educational

### 2. Common Techniques
- **Swap with last**: For O(1) removal from ArrayList
- **Dummy nodes**: Simplify edge cases in linked lists
- **Modulo arithmetic**: For circular buffers

### 3. Interview Tips
- Start with brute force, then optimize
- Discuss trade-offs explicitly
- Consider edge cases (empty, single element, capacity)
- Ask about concurrency requirements

## 🚀 Study Path

### Beginner (Start Here)
1. Min Stack
2. Design Parking System
3. Design Browser History
4. Design HashMap

### Intermediate
1. LRU Cache ⭐ Most Important
2. Design Circular Queue
3. Design Underground System
4. Design Authentication Manager

### Advanced
1. LFU Cache ⭐ Hardest
2. Max Stack
3. Design Circular Deque

## 📝 Common Interview Questions

### Q1: "Design a cache with eviction policy"
→ Start with LRU, discuss LFU as follow-up

### Q2: "Design a data structure with O(1) operations"
→ Usually requires HashMap + another structure

### Q3: "How to handle concurrency?"
→ Discuss locks, ConcurrentHashMap, lock-free algorithms

### Q4: "How to scale to distributed system?"
→ Discuss sharding, consistent hashing, Redis

## 🔗 Related Topics

- [Algorithm Design Problems](../02-algorithm-design/)
- [System Design Problems](../03-system-design/)
- [Rate Limiting](../05-rate-limiting/)

---

**Total Problems**: 12
**Estimated Study Time**: 2-3 days
**Difficulty Distribution**: Easy (4), Medium (7), Hard (1)
