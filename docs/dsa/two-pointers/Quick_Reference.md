# Two Pointers - Quick Reference Cheat Sheet

## 🎯 Three Main Patterns

### 1. Opposite Direction (Converging)
```java
int left = 0, right = arr.length - 1;
while (left < right) {
    if (condition) {
        left++;
    } else {
        right--;
    }
}
```
**Use for**: Sorted arrays, palindromes, pair finding

### 2. Same Direction (Sliding Window)
```java
int left = 0;
for (int right = 0; right < arr.length; right++) {
    // Expand window
    while (needsContraction) {
        left++; // Contract window
    }
}
```
**Use for**: Subarrays, substrings, consecutive elements

### 3. Fast & Slow
```java
int slow = 0, fast = 0;
while (fast < arr.length) {
    slow++;
    fast += 2;
}
```
**Use for**: Cycle detection, middle element, linked lists

## 📋 Problem Recognition

| Keywords | Pattern | Example |
|----------|---------|---------|
| "sorted array" | Opposite | Two Sum II |
| "pair/triplet" | Opposite | 3Sum |
| "palindrome" | Opposite | Valid Palindrome |
| "substring" | Sliding Window | Longest Substring |
| "subarray" | Sliding Window | Subarray Sum |
| "consecutive" | Sliding Window | Product Less Than K |
| "cycle" | Fast & Slow | Linked List Cycle |
| "middle" | Fast & Slow | Middle of List |

## 🔑 Common Templates

### Two Sum (Sorted)
```java
int left = 0, right = n - 1;
while (left < right) {
    int sum = arr[left] + arr[right];
    if (sum == target) return new int[]{left, right};
    if (sum < target) left++;
    else right--;
}
```

### Remove Duplicates
```java
int slow = 0;
for (int fast = 1; fast < n; fast++) {
    if (arr[fast] != arr[slow]) {
        arr[++slow] = arr[fast];
    }
}
return slow + 1;
```

### Sliding Window (Fixed Size)
```java
for (int i = 0; i < n; i++) {
    // Add arr[i] to window
    if (i >= k) {
        // Remove arr[i-k] from window
    }
    if (i >= k - 1) {
        // Process window
    }
}
```

### Sliding Window (Variable Size)
```java
int left = 0;
for (int right = 0; right < n; right++) {
    // Add arr[right]
    while (windowInvalid) {
        // Remove arr[left]
        left++;
    }
    // Update result
}
```

## ⚡ Time Complexities

| Pattern | Time | Space | Notes |
|---------|------|-------|-------|
| Opposite Direction | O(n) | O(1) | Single pass |
| Sliding Window | O(n) | O(1) or O(k) | Depends on tracking |
| Fast & Slow | O(n) | O(1) | Fast moves 2n |
| 3Sum | O(n²) | O(1) | Nested with 2-pointer |
| 4Sum | O(n³) | O(1) | Double nested |

## 🚫 Common Mistakes

1. **Off-by-one errors**
   ```java
   // ❌ Wrong
   while (left < right - 1)
   
   // ✅ Correct
   while (left < right)
   ```

2. **Forgetting to move pointers**
   ```java
   // ❌ Wrong - infinite loop
   while (left < right) {
       if (condition) {
           // Forgot left++ or right--
       }
   }
   ```

3. **Not handling duplicates**
   ```java
   // ✅ Skip duplicates in 3Sum
   while (left < right && arr[left] == arr[left+1]) left++;
   while (left < right && arr[right] == arr[right-1]) right--;
   ```

4. **Wrong initialization**
   ```java
   // ❌ Wrong
   int right = arr.length; // Out of bounds
   
   // ✅ Correct
   int right = arr.length - 1;
   ```

## 📊 All 20 Problems Summary

### Easy (8 problems)
1. Two Sum II - O(n)
2. Remove Duplicates - O(n)
3. Valid Palindrome - O(n)
4. Move Zeroes - O(n)
5. Reverse String - O(n)
6. Sorted Squares - O(n)
7. Merge Sorted Array - O(m+n)
8. Remove Element - O(n)

### Medium (10 problems)
1. 3Sum - O(n²)
2. Container With Most Water - O(n)
3. Sort Colors - O(n)
4. Trapping Rain Water - O(n)
5. Longest Substring Without Repeating - O(n)
6. Minimum Window Substring - O(n)
7. Find All Anagrams - O(n)
8. Subarray Product Less Than K - O(n)
9. Longest Repeating Character Replacement - O(n)
10. 4Sum - O(n³)

### Hard (2 problems)
1. Trapping Rain Water II - O(mn log(mn))
2. Minimum Window Subsequence - O(s×t)

## 🎓 Study Plan

### Week 1: Fundamentals
- Day 1-2: Theory and Pattern Recognition
- Day 3-4: Easy Problems 1-4
- Day 5-6: Easy Problems 5-8
- Day 7: Review and practice

### Week 2: Intermediate
- Day 1-2: Medium Problems 1-3
- Day 3-4: Medium Problems 4-6
- Day 5-6: Medium Problems 7-10
- Day 7: Review and practice

### Week 3: Advanced
- Day 1-3: Hard Problem 1
- Day 4-6: Hard Problem 2
- Day 7: Mixed practice

## 💡 Interview Tips

1. **Clarify Requirements**
   - Is array sorted?
   - Can I modify in-place?
   - What about duplicates?

2. **Start with Brute Force**
   - Explain O(n²) solution first
   - Then optimize with two pointers

3. **Draw Examples**
   - Visualize pointer movements
   - Show step-by-step execution

4. **Handle Edge Cases**
   - Empty array
   - Single element
   - All same elements
   - No solution exists

5. **Analyze Complexity**
   - Time: Usually O(n) or O(n²)
   - Space: Usually O(1)

## 🔗 Related Patterns

- **Binary Search**: Uses two pointers (low, high)
- **Merge Sort**: Merging uses two pointers
- **Quick Sort**: Partitioning uses two pointers
- **Kadane's Algorithm**: Similar to sliding window
- **Dutch National Flag**: Three pointers variant

## 📚 Practice Resources

- LeetCode: Filter by "Two Pointers" tag
- Practice order: Easy → Medium → Hard
- Time yourself: 20-30 min per problem
- Review solutions even if you solve it

## ✅ Mastery Checklist

- [ ] Understand all three patterns
- [ ] Can identify pattern from problem statement
- [ ] Solved all 8 easy problems
- [ ] Solved all 10 medium problems
- [ ] Solved both hard problems
- [ ] Can explain solutions clearly
- [ ] Handle edge cases consistently
- [ ] Optimize time/space complexity
- [ ] Code without syntax errors
- [ ] Complete problems in time limit

---

**Keep this cheat sheet handy during practice and interviews!**
