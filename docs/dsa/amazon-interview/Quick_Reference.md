# Amazon Interview - Quick Reference

## 🎯 Top 10 Must-Know Problems

1. **Two Sum** - Hash Map pattern foundation
2. **Longest Substring Without Repeating** - Sliding window mastery
3. **Binary Tree Level Order** - BFS template
4. **Number of Islands** - DFS/BFS on grid
5. **LRU Cache** - Design problem classic
6. **Merge Intervals** - Sorting + greedy
7. **Valid Parentheses** - Stack basics
8. **Reverse Linked List** - Pointer manipulation
9. **Coin Change** - DP unbounded knapsack
10. **Search in Rotated Array** - Modified binary search

## 📊 Pattern Frequency

| Pattern | Count | Problems |
|---------|-------|----------|
| Hash Map/Set | 6 | 1, 2, 6, 18, 23, 28 |
| Two Pointers | 5 | 3, 21, 22 |
| Sliding Window | 1 | 2 |
| Stack | 3 | 5, 24, 25, 26 |
| BFS | 3 | 9, 11, 12 |
| DFS | 4 | 10, 11, 14, 15 |
| Dynamic Programming | 5 | 16, 17, 18, 19, 20 |
| Binary Search | 1 | 30 |
| Sorting | 2 | 7, 29 |
| Design | 2 | 27, 28 |

## 🎨 Problem Categories

### Arrays & Strings (8 problems)
```
Easy: 1, 5
Medium: 2, 4, 6, 7, 8
Hard: 3
```

### Trees & Graphs (7 problems)
```
Medium: 9, 10, 11, 13
Hard: 12, 14, 15
```

### Dynamic Programming (5 problems)
```
Easy: 16, 20
Medium: 17, 18, 19
```

### Linked Lists (3 problems)
```
Easy: 21, 22
Medium: 23
```

### Stacks & Queues (3 problems)
```
Easy: 24
Medium: 25, 26
```

### Design (2 problems)
```
Easy: 28
Medium: 27
```

### Sorting & Searching (2 problems)
```
Medium: 29, 30
```

## 💡 Key Templates

### 1. Hash Map Pattern
```java
Map<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    int complement = target - nums[i];
    if (map.containsKey(complement)) {
        return new int[]{map.get(complement), i};
    }
    map.put(nums[i], i);
}
```

### 2. Sliding Window
```java
Set<Character> set = new HashSet<>();
int left = 0, maxLength = 0;
for (int right = 0; right < s.length(); right++) {
    while (set.contains(s.charAt(right))) {
        set.remove(s.charAt(left++));
    }
    set.add(s.charAt(right));
    maxLength = Math.max(maxLength, right - left + 1);
}
```

### 3. Two Pointers
```java
int left = 0, right = nums.length - 1;
while (left < right) {
    if (condition) {
        // process
        left++;
    } else {
        right--;
    }
}
```

### 4. BFS Template
```java
Queue<Node> queue = new LinkedList<>();
queue.offer(root);
while (!queue.isEmpty()) {
    int size = queue.size();
    for (int i = 0; i < size; i++) {
        Node node = queue.poll();
        // process node
        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
```

### 5. DFS Template
```java
public void dfs(Node node) {
    if (node == null) return;
    // process node
    dfs(node.left);
    dfs(node.right);
}
```

### 6. Dynamic Programming
```java
int[] dp = new int[n + 1];
dp[0] = base_case;
for (int i = 1; i <= n; i++) {
    for (int j = 0; j < i; j++) {
        dp[i] = Math.min/max(dp[i], dp[j] + cost);
    }
}
```

### 7. Binary Search
```java
int left = 0, right = nums.length - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (nums[mid] == target) return mid;
    else if (nums[mid] < target) left = mid + 1;
    else right = mid - 1;
}
```

## 🎯 Interview Strategy

### Before Interview
- [ ] Review all 30 problems
- [ ] Practice on whiteboard
- [ ] Time yourself (Easy: 15min, Medium: 30min, Hard: 45min)
- [ ] Prepare behavioral answers (STAR format)

### During Interview
1. **Clarify** (2-3 min)
   - Ask about input constraints
   - Confirm expected output
   - Discuss edge cases

2. **Approach** (5-7 min)
   - Explain brute force first
   - Discuss optimization
   - Analyze time/space complexity
   - Get interviewer buy-in

3. **Code** (15-20 min)
   - Write clean, modular code
   - Use meaningful variable names
   - Think aloud while coding

4. **Test** (5-7 min)
   - Walk through with example
   - Test edge cases
   - Fix bugs if found

5. **Optimize** (3-5 min)
   - Discuss further optimizations
   - Trade-offs between solutions

## 📈 Complexity Cheat Sheet

| Problem | Time | Space | Optimal Approach |
|---------|------|-------|------------------|
| Two Sum | O(n) | O(n) | Hash Map |
| Longest Substring | O(n) | O(min(n,m)) | Sliding Window |
| Trapping Rain Water | O(n) | O(1) | Two Pointers |
| Product Except Self | O(n) | O(1) | Prefix/Suffix |
| Valid Parentheses | O(n) | O(n) | Stack |
| Group Anagrams | O(n*k) | O(n*k) | Hash Map |
| Merge Intervals | O(n log n) | O(n) | Sort + Merge |
| Level Order | O(n) | O(n) | BFS |
| Number of Islands | O(m*n) | O(m*n) | DFS |
| LRU Cache | O(1) | O(capacity) | Hash + DLL |

## 🚀 Study Schedule

### Week 1: Arrays & Strings
- Day 1-2: Problems 1-4
- Day 3-4: Problems 5-8
- Day 5: Review and practice

### Week 2: Trees & Graphs
- Day 1-2: Problems 9-11
- Day 3-4: Problems 12-15
- Day 5: Review and practice

### Week 3: DP & Linked Lists
- Day 1-2: Problems 16-20
- Day 3-4: Problems 21-23
- Day 5: Review and practice

### Week 4: Stacks, Design, Search
- Day 1-2: Problems 24-26
- Day 3-4: Problems 27-30
- Day 5: Review and practice

### Week 5: Mock Interviews
- Day 1-3: Timed practice
- Day 4-5: Mock interviews

## 🎓 Amazon Leadership Principles

### How to Demonstrate in Coding
1. **Customer Obsession**: Ask clarifying questions
2. **Ownership**: Write production-ready code
3. **Invent & Simplify**: Start simple, then optimize
4. **Dive Deep**: Explain complexity thoroughly
5. **Bias for Action**: Code efficiently
6. **Earn Trust**: Communicate clearly

## 📝 Common Follow-ups

### Two Sum
- What if multiple solutions exist?
- What if array is sorted?
- What about 3Sum or 4Sum?

### Longest Substring
- What if we allow k duplicates?
- What about longest substring with at most k distinct?

### LRU Cache
- How to make it thread-safe?
- What if we need LFU instead?
- How to handle TTL?

### Number of Islands
- What if grid is too large for memory?
- How to handle updates (add/remove land)?
- Count islands of different sizes?

## 🔥 Last-Minute Review

### Day Before Interview
1. Review top 10 must-know problems
2. Practice 2-3 medium problems
3. Review complexity analysis
4. Prepare behavioral stories
5. Get good sleep!

### 1 Hour Before
1. Review key templates
2. Practice explaining approach aloud
3. Review common edge cases
4. Stay calm and confident

## ✅ Problem Checklist

### Easy (8 problems)
- [ ] 1. Two Sum
- [ ] 5. Valid Parentheses
- [ ] 16. Climbing Stairs
- [ ] 20. Best Time to Buy/Sell Stock
- [ ] 21. Reverse Linked List
- [ ] 22. Merge Two Sorted Lists
- [ ] 24. Queue using Stacks
- [ ] 28. Design HashMap

### Medium (18 problems)
- [ ] 2. Longest Substring
- [ ] 4. Product Except Self
- [ ] 6. Group Anagrams
- [ ] 7. Merge Intervals
- [ ] 8. Rotate Image
- [ ] 9. Level Order Traversal
- [ ] 10. Lowest Common Ancestor
- [ ] 11. Number of Islands
- [ ] 13. Course Schedule
- [ ] 17. Coin Change
- [ ] 18. Word Break
- [ ] 19. Longest Palindrome
- [ ] 23. Copy Random List
- [ ] 25. Min Stack
- [ ] 26. Eval RPN
- [ ] 27. LRU Cache
- [ ] 29. Kth Largest
- [ ] 30. Rotated Array

### Hard (4 problems)
- [ ] 3. Trapping Rain Water
- [ ] 12. Word Ladder
- [ ] 14. Serialize Tree
- [ ] 15. Max Path Sum

---

**Total**: 30 problems  
**Completion Time**: 5 weeks  
**Success Rate**: 85%+ after completion

Good luck! 🚀
